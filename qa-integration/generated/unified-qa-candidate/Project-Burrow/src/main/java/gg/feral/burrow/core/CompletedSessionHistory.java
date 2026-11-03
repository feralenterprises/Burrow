package gg.feral.burrow.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * BQA-902 F05/F11/F13 (SESS-04): durable, account-isolated rolling history of the last
 * {@value #CAPACITY} COMPLETED logins.
 *
 * <ul>
 *   <li>Only a summary explicitly committed with {@link Summary#terminal()} = {@code CONFIRMED_LOGOUT} enters
 *       the history. Account switch, identity loss, plugin stop and crash never manufacture a completed login.</li>
 *   <li>Exactly once: a summary's {@code loginId} (the opaque per-login UUID) is unique per account; duplicate
 *       terminal callbacks are no-ops.</li>
 *   <li>Atomic: written to a temp file, fsync'd, then moved over the old file (ATOMIC_MOVE where supported).
 *       The previous good file is kept as {@code .bak}.</li>
 *   <li>Integrity: the file carries a SHA-256 of its body. A corrupt or tampered file is quarantined (renamed
 *       {@code .corrupt-<n>}), the {@code .bak} is tried, and on total failure the history reports
 *       {@link LoadState#RECOVERED_EMPTY} rather than pretending there were zero sessions.</li>
 *   <li>Isolation: the file name is a salted SHA-256 pseudonym of the account key; raw hashes, names and bank
 *       contents are never written.</li>
 * </ul>
 * Not thread-safe by itself; the plugin calls it from one thread.
 */
public final class CompletedSessionHistory {
    public static final int CAPACITY = 10;
    public static final int SCHEMA = 1;
    public static final String CONFIRMED_LOGOUT = "CONFIRMED_LOGOUT";

    public enum FeedState { OBSERVED, UNAVAILABLE }
    public enum LoadState { EMPTY_NEW, LOADED, RECOVERED_FROM_BACKUP, RECOVERED_EMPTY }

    public record Drop(int itemId, String itemName, String boss, long quantity) {
        public Drop { if (itemId <= 0 || quantity <= 0 || itemName == null || itemName.isBlank()) throw new IllegalArgumentException("drop"); }
    }

    /** Immutable completed-login card. Maps are skill/boss name -> positive amount. */
    public record Summary(int schema, String loginId, long startMillis, long endMillis, String terminal,
                          FeedState xpFeed, Map<String,Long> xpBySkill,
                          FeedState kcFeed, Map<String,Long> verifiedKcByBoss,
                          Map<String,Long> estimatedLootEncounters,
                          List<Drop> drops, FeedState suppliesFeed, List<Drop> supplies,
                          List<String> uncertainty) {
        public Summary {
            if (loginId == null || loginId.isBlank()) throw new IllegalArgumentException("login id");
            if (endMillis < startMillis) throw new IllegalArgumentException("end before start");
            Objects.requireNonNull(terminal); Objects.requireNonNull(xpFeed); Objects.requireNonNull(kcFeed); Objects.requireNonNull(suppliesFeed);
            xpBySkill = positive(xpBySkill); verifiedKcByBoss = positive(verifiedKcByBoss);
            estimatedLootEncounters = positive(estimatedLootEncounters);
            drops = List.copyOf(drops == null ? List.of() : drops);
            supplies = List.copyOf(supplies == null ? List.of() : supplies);
            uncertainty = List.copyOf(uncertainty == null ? List.of() : uncertainty);
            if (xpFeed == FeedState.UNAVAILABLE && !xpBySkill.isEmpty()) throw new IllegalArgumentException("xp rows without feed");
            if (suppliesFeed == FeedState.UNAVAILABLE && !supplies.isEmpty()) throw new IllegalArgumentException("supplies without feed");
        }
        private static Map<String,Long> positive(Map<String,Long> m) {
            Map<String,Long> out = new LinkedHashMap<>();
            if (m != null) for (var e : m.entrySet()) {
                if (e.getKey() == null || e.getKey().isBlank() || e.getValue() == null || e.getValue() <= 0)
                    throw new IllegalArgumentException("positive named rows only");
                out.put(e.getKey(), e.getValue());
            }
            return Collections.unmodifiableMap(out);
        }
        public long totalXp() { return xpBySkill.values().stream().mapToLong(Long::longValue).sum(); }
        public long totalVerifiedKc() { return verifiedKcByBoss.values().stream().mapToLong(Long::longValue).sum(); }
        public long durationMillis() { return endMillis - startMillis; }
    }

    private final Path dir;
    private final String salt;
    private final String accountFile;
    private final List<Summary> newestFirst = new ArrayList<>();
    private LoadState loadState = LoadState.EMPTY_NEW;
    private String lastError;

    private CompletedSessionHistory(Path dir, String salt, String accountKey) {
        this.dir = dir; this.salt = salt; this.accountFile = pseudonym(salt, accountKey) + ".json";
    }

    /** Open (and load) one account's history. {@code accountKey} is an opaque in-memory key, never written. */
    public static CompletedSessionHistory open(Path dir, String accountKey) throws IOException {
        Objects.requireNonNull(dir); if (accountKey == null || accountKey.isBlank()) throw new IllegalArgumentException("account");
        Files.createDirectories(dir);
        var h = new CompletedSessionHistory(dir, installSalt(dir), accountKey);
        h.load();
        return h;
    }

    public List<Summary> newestFirst() { return List.copyOf(newestFirst); }
    public LoadState loadState() { return loadState; }
    public String lastError() { return lastError; }
    Path file() { return dir.resolve(accountFile); }

    /** Commit one completed login. Returns false (and writes nothing) for duplicates or non-terminal summaries. */
    public boolean commit(Summary s) throws IOException {
        Objects.requireNonNull(s);
        if (!CONFIRMED_LOGOUT.equals(s.terminal())) return false;
        for (Summary x : newestFirst) if (x.loginId().equals(s.loginId())) return false;
        List<Summary> next = new ArrayList<>(newestFirst);
        next.add(0, s);
        next.sort(Comparator.comparingLong(Summary::endMillis).reversed().thenComparing(Summary::loginId));
        while (next.size() > CAPACITY) next.remove(next.size() - 1);
        write(next); // durable before the in-memory view changes
        newestFirst.clear(); newestFirst.addAll(next);
        return true;
    }

    // ---------------- persistence ----------------
    private void load() {
        Path f = file(), bak = dir.resolve(accountFile + ".bak");
        if (!Files.exists(f) && !Files.exists(bak)) { loadState = LoadState.EMPTY_NEW; return; }
        try { newestFirst.addAll(read(f)); loadState = LoadState.LOADED; return; }
        catch (IOException | RuntimeException e) { lastError = e.getClass().getSimpleName(); quarantine(f); }
        try { newestFirst.addAll(read(bak)); loadState = LoadState.RECOVERED_FROM_BACKUP; }
        catch (IOException | RuntimeException e) { newestFirst.clear(); loadState = LoadState.RECOVERED_EMPTY; quarantine(bak); }
    }
    private void quarantine(Path p) {
        if (!Files.exists(p)) return;
        for (int i = 1; i < 1000; i++) {
            Path q = p.resolveSibling(p.getFileName() + ".corrupt-" + i);
            if (Files.exists(q)) continue;
            try { Files.move(p, q); } catch (IOException ignored) { }
            return;
        }
    }
    private static List<Summary> read(Path f) throws IOException {
        String text = Files.readString(f, StandardCharsets.UTF_8);
        Object root = MiniJson.parse(text);
        if (!(root instanceof Map<?,?> m)) throw new IOException("root");
        if (!(m.get("schema") instanceof Number n) || n.intValue() != SCHEMA) throw new IOException("schema");
        Object body = m.get("sessions");
        String digest = String.valueOf(m.get("sha256"));
        if (!(body instanceof List<?> list)) throw new IOException("sessions");
        List<Summary> out = new ArrayList<>();
        for (Object o : list) out.add(fromJson((Map<?,?>) o));
        if (!sha256(sessionsJson(out)).equals(digest)) throw new IOException("integrity");
        if (out.size() > CAPACITY) throw new IOException("capacity");
        Set<String> ids = new HashSet<>();
        for (Summary s : out) if (!ids.add(s.loginId())) throw new IOException("duplicate login");
        return out;
    }
    private void write(List<Summary> sessions) throws IOException {
        String body = sessionsJson(sessions);
        String text = "{\"schema\":" + SCHEMA + ",\"sha256\":\"" + sha256(body) + "\",\"sessions\":" + body + "}\n";
        Path f = file(), tmp = dir.resolve(accountFile + ".tmp"), bak = dir.resolve(accountFile + ".bak");
        try (var ch = java.nio.channels.FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ch.write(java.nio.ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
            ch.force(true);
        }
        if (Files.exists(f)) Files.copy(f, bak, StandardCopyOption.REPLACE_EXISTING);
        try { Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING); }
    }

    // ---------------- canonical JSON ----------------
    static String sessionsJson(List<Summary> list) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) { if (i > 0) b.append(','); toJson(b, list.get(i)); }
        return b.append(']').toString();
    }
    private static void toJson(StringBuilder b, Summary s) {
        b.append('{');
        kv(b, "schema", s.schema()).append(',');
        kv(b, "loginId", s.loginId()).append(',');
        kv(b, "start", s.startMillis()).append(',');
        kv(b, "end", s.endMillis()).append(',');
        kv(b, "terminal", s.terminal()).append(',');
        kv(b, "xpFeed", s.xpFeed().name()).append(',');
        map(b, "xp", s.xpBySkill()).append(',');
        kv(b, "kcFeed", s.kcFeed().name()).append(',');
        map(b, "kc", s.verifiedKcByBoss()).append(',');
        map(b, "lootEstimates", s.estimatedLootEncounters()).append(',');
        drops(b, "drops", s.drops()).append(',');
        kv(b, "suppliesFeed", s.suppliesFeed().name()).append(',');
        drops(b, "supplies", s.supplies()).append(',');
        str(b, "uncertainty"); b.append(":[");
        for (int i = 0; i < s.uncertainty().size(); i++) { if (i > 0) b.append(','); str(b, s.uncertainty().get(i)); }
        b.append("]}");
    }
    private static StringBuilder kv(StringBuilder b, String k, Object v) {
        str(b, k); b.append(':');
        if (v instanceof Number) b.append(v); else str(b, String.valueOf(v));
        return b;
    }
    private static StringBuilder map(StringBuilder b, String k, Map<String,Long> m) {
        str(b, k); b.append(":{");
        int i = 0;
        for (var e : m.entrySet()) { if (i++ > 0) b.append(','); str(b, e.getKey()); b.append(':').append(e.getValue()); }
        return b.append('}');
    }
    private static StringBuilder drops(StringBuilder b, String k, List<Drop> d) {
        str(b, k); b.append(":[");
        for (int i = 0; i < d.size(); i++) {
            if (i > 0) b.append(',');
            Drop x = d.get(i);
            b.append('{'); kv(b, "id", x.itemId()).append(','); kv(b, "name", x.itemName()).append(',');
            kv(b, "boss", x.boss() == null ? "" : x.boss()).append(','); kv(b, "qty", x.quantity()); b.append('}');
        }
        return b.append(']');
    }
    private static void str(StringBuilder b, String s) {
        b.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\""); case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n"); case '\r' -> b.append("\\r"); case '\t' -> b.append("\\t");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        b.append('"');
    }
    private static Summary fromJson(Map<?,?> m) {
        return new Summary(num(m, "schema").intValue(), (String) m.get("loginId"), num(m, "start").longValue(), num(m, "end").longValue(),
            (String) m.get("terminal"), FeedState.valueOf((String) m.get("xpFeed")), longs(m.get("xp")),
            FeedState.valueOf((String) m.get("kcFeed")), longs(m.get("kc")), longs(m.get("lootEstimates")),
            dropsOf(m.get("drops")), FeedState.valueOf((String) m.get("suppliesFeed")), dropsOf(m.get("supplies")),
            strings(m.get("uncertainty")));
    }
    private static Number num(Map<?,?> m, String k) { Object v = m.get(k); if (!(v instanceof Number n)) throw new IllegalArgumentException(k); return n; }
    private static Map<String,Long> longs(Object o) {
        Map<String,Long> out = new LinkedHashMap<>();
        for (var e : ((Map<?,?>) o).entrySet()) out.put((String) e.getKey(), ((Number) e.getValue()).longValue());
        return out;
    }
    private static List<Drop> dropsOf(Object o) {
        List<Drop> out = new ArrayList<>();
        for (Object x : (List<?>) o) {
            Map<?,?> d = (Map<?,?>) x; String boss = (String) d.get("boss");
            out.add(new Drop(num(d, "id").intValue(), (String) d.get("name"), boss == null || boss.isEmpty() ? null : boss, num(d, "qty").longValue()));
        }
        return out;
    }
    private static List<String> strings(Object o) { List<String> out = new ArrayList<>(); for (Object x : (List<?>) o) out.add((String) x); return out; }

    // ---------------- privacy ----------------
    /** Shared per-install salt for every per-account TrashPanda file in {@code dir}. */
    static String installSaltFor(Path dir) throws IOException { return installSalt(dir); }
    private static String installSalt(Path dir) throws IOException {
        Path p = dir.resolve("install-salt.txt");
        if (Files.exists(p)) {
            String s = Files.readString(p, StandardCharsets.UTF_8).trim();
            if (s.matches("[0-9a-f]{32}")) return s;
        }
        String s = UUID.randomUUID().toString().replace("-", "");
        Files.writeString(p, s, StandardCharsets.UTF_8);
        return s;
    }
    static String pseudonym(String salt, String accountKey) { return "acct-" + sha256(salt + ":" + accountKey).substring(0, 32); }
    static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder();
            for (byte x : d) b.append(String.format("%02x", x));
            return b.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
