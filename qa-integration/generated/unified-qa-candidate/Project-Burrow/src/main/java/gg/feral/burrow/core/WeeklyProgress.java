package gg.feral.burrow.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * BQA-IMPL-R08: persistent weekly progress, independent of the last-10 completed-login history.
 *
 * <p><b>Reporting boundary.</b> A week is the ISO week in the player's local time zone: Monday 00:00:00 (inclusive) to
 * the next Monday 00:00:00 (exclusive). Every observation is attributed to the week that contains its own timestamp,
 * so a login that crosses midnight Sunday contributes to two weeks, each with only its own events.
 *
 * <p><b>No duplicates, restart-safe.</b> The file stores, per week, one {@link Contribution} per login id
 * (the opaque per-login UUID). A save REPLACES that login's contribution with its current cumulative totals, so saving
 * twice, or saving after every tick, never double counts. After a restart, saved logins keep their totals; a crash can
 * lose at most the events since the last save of the running login.
 *
 * <p><b>Isolation.</b> One file per account, named by the same salted pseudonym as {@link CompletedSessionHistory}
 * (raw account hashes are never written); integrity digest, temp + fsync + atomic move, .bak and quarantine on damage.
 *
 * <p><b>Honesty.</b> Counters other than XP / verified KC / observed drops / loot estimates are reported only when the
 * login declares the counter's source as tracked; otherwise the week shows them as "not tracked", never 0.
 */
public final class WeeklyProgress {
    public static final int SCHEMA = 1;
    public static final int KEEP_WEEKS = 8;

    public static LocalDate weekStart(long epochMillis, ZoneId zone) {
        return Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** One login's cumulative observations inside one week. Immutable. */
    public record Contribution(Map<String,Long> xpBySkill, Map<String,Long> verifiedKcByBoss, long dropItems,
                               long lootEstimates, Map<String,Long> counters, Set<String> trackedCounters) {
        public Contribution {
            xpBySkill = pos(xpBySkill); verifiedKcByBoss = pos(verifiedKcByBoss); counters = pos(counters);
            trackedCounters = Set.copyOf(trackedCounters == null ? Set.of() : trackedCounters);
            if (dropItems < 0 || lootEstimates < 0) throw new IllegalArgumentException("negative");
            for (String c : counters.keySet()) if (!trackedCounters.contains(c)) throw new IllegalArgumentException("untracked counter " + c);
        }
        public static Contribution empty() { return new Contribution(Map.of(), Map.of(), 0, 0, Map.of(), Set.of()); }
        private static Map<String,Long> pos(Map<String,Long> m) {
            Map<String,Long> out = new TreeMap<>();
            if (m != null) for (var e : m.entrySet()) {
                if (e.getValue() == null || e.getValue() < 0 || e.getKey() == null || e.getKey().isBlank()) throw new IllegalArgumentException("row");
                if (e.getValue() > 0) out.put(e.getKey(), e.getValue());
            }
            return Collections.unmodifiableMap(out);
        }
    }

    /** Totals for one week. A counter is present in {@link #counters()} only if at least one login tracked it. */
    public record Totals(LocalDate weekStart, long xp, long verifiedKc, long dropItems, long lootEstimates,
                         Map<String,Long> counters, int logins) {
        public OptionalLong counter(String name) { Long v = counters.get(name); return v == null ? OptionalLong.empty() : OptionalLong.of(v); }
    }

    private final Path dir;
    private final String accountFile;
    private final Map<LocalDate,Map<String,Contribution>> weeks = new TreeMap<>();
    private String loadNote = "new";

    private WeeklyProgress(Path dir, String file) { this.dir = dir; this.accountFile = file; }

    public static WeeklyProgress open(Path dir, String accountKey) throws IOException {
        Objects.requireNonNull(dir);
        if (accountKey == null || accountKey.isBlank()) throw new IllegalArgumentException("account");
        Files.createDirectories(dir);
        String salt = CompletedSessionHistory.installSaltFor(dir);
        var w = new WeeklyProgress(dir, CompletedSessionHistory.pseudonym(salt, accountKey) + ".weekly.json");
        w.load();
        return w;
    }
    public String loadNote() { return loadNote; }
    Path file() { return dir.resolve(accountFile); }

    /** Replace this login's contributions (all weeks it touched) and persist atomically. Idempotent. */
    public synchronized void save(String loginId, Map<LocalDate,Contribution> perWeek) throws IOException {
        if (loginId == null || loginId.isBlank()) throw new IllegalArgumentException("login");
        Map<LocalDate,Map<String,Contribution>> next = deepCopy();
        for (var e : perWeek.entrySet()) next.computeIfAbsent(e.getKey(), k -> new TreeMap<>()).put(loginId, e.getValue());
        while (next.size() > KEEP_WEEKS) next.remove(((TreeMap<LocalDate,?>) next).firstKey());
        write(next);
        weeks.clear(); weeks.putAll(next);
    }

    /** Week totals; the running login's saved entry is replaced by its live contribution (no double count). */
    public synchronized Totals totals(LocalDate week, String liveLoginId, Contribution liveForThisWeek) {
        Map<String,Contribution> logins = new TreeMap<>(weeks.getOrDefault(week, Map.of()));
        if (liveLoginId != null && liveForThisWeek != null) logins.put(liveLoginId, liveForThisWeek);
        long xp = 0, kc = 0, drops = 0, est = 0;
        Map<String,Long> counters = new TreeMap<>();
        for (Contribution c : logins.values()) {
            xp += c.xpBySkill().values().stream().mapToLong(Long::longValue).sum();
            kc += c.verifiedKcByBoss().values().stream().mapToLong(Long::longValue).sum();
            drops += c.dropItems(); est += c.lootEstimates();
            for (String t : c.trackedCounters()) counters.merge(t, c.counters().getOrDefault(t, 0L), Long::sum);
        }
        return new Totals(week, xp, kc, drops, est, Collections.unmodifiableMap(counters), logins.size());
    }
    public synchronized Set<LocalDate> weeks() { return Set.copyOf(weeks.keySet()); }

    private Map<LocalDate,Map<String,Contribution>> deepCopy() {
        Map<LocalDate,Map<String,Contribution>> m = new TreeMap<>();
        weeks.forEach((k, v) -> m.put(k, new TreeMap<>(v)));
        return m;
    }

    // ---------------- persistence ----------------
    private void load() {
        Path f = file(), bak = dir.resolve(accountFile + ".bak");
        if (!Files.exists(f) && !Files.exists(bak)) return;
        try { weeks.putAll(read(f)); loadNote = "loaded"; return; } catch (IOException | RuntimeException e) { quarantine(f); }
        try { weeks.putAll(read(bak)); loadNote = "recovered from backup"; }
        catch (IOException | RuntimeException e) { weeks.clear(); quarantine(bak); loadNote = "damaged file set aside; new weekly record started"; }
    }
    private static void quarantine(Path p) {
        if (!Files.exists(p)) return;
        for (int i = 1; i < 1000; i++) {
            Path q = p.resolveSibling(p.getFileName() + ".corrupt-" + i);
            if (Files.exists(q)) continue;
            try { Files.move(p, q); } catch (IOException ignored) { }
            return;
        }
    }
    private static Map<LocalDate,Map<String,Contribution>> read(Path f) throws IOException {
        Object root = MiniJson.parse(Files.readString(f, StandardCharsets.UTF_8));
        if (!(root instanceof Map<?,?> m) || !(m.get("schema") instanceof Number n) || n.intValue() != SCHEMA) throw new IOException("schema");
        Object body = m.get("weeks");
        if (!(body instanceof Map<?,?> wm)) throw new IOException("weeks");
        Map<LocalDate,Map<String,Contribution>> out = new TreeMap<>();
        for (var we : wm.entrySet()) {
            Map<String,Contribution> logins = new TreeMap<>();
            for (var le : ((Map<?,?>) we.getValue()).entrySet()) logins.put((String) le.getKey(), contribution((Map<?,?>) le.getValue()));
            out.put(LocalDate.parse((String) we.getKey()), logins);
        }
        if (!CompletedSessionHistory.sha256(json(out)).equals(String.valueOf(m.get("sha256")))) throw new IOException("integrity");
        return out;
    }
    private void write(Map<LocalDate,Map<String,Contribution>> data) throws IOException {
        String body = json(data);
        String text = "{\"schema\":" + SCHEMA + ",\"sha256\":\"" + CompletedSessionHistory.sha256(body) + "\",\"weeks\":" + body + "}\n";
        Path f = file(), tmp = dir.resolve(accountFile + ".tmp"), bak = dir.resolve(accountFile + ".bak");
        try (var ch = java.nio.channels.FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ch.write(java.nio.ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8))); ch.force(true);
        }
        if (Files.exists(f)) Files.copy(f, bak, StandardCopyOption.REPLACE_EXISTING);
        try { Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING); }
    }
    static String json(Map<LocalDate,Map<String,Contribution>> data) {
        StringBuilder b = new StringBuilder("{");
        int i = 0;
        for (var we : data.entrySet()) {
            if (i++ > 0) b.append(',');
            str(b, we.getKey().toString()); b.append(":{");
            int j = 0;
            for (var le : we.getValue().entrySet()) {
                if (j++ > 0) b.append(',');
                str(b, le.getKey()); b.append(':');
                Contribution c = le.getValue();
                b.append("{\"xp\":"); map(b, c.xpBySkill()); b.append(",\"kc\":"); map(b, c.verifiedKcByBoss());
                b.append(",\"drops\":").append(c.dropItems()).append(",\"est\":").append(c.lootEstimates());
                b.append(",\"counters\":"); map(b, c.counters()); b.append(",\"tracked\":[");
                int k = 0; for (String t : new TreeSet<>(c.trackedCounters())) { if (k++ > 0) b.append(','); str(b, t); }
                b.append("]}");
            }
            b.append('}');
        }
        return b.append('}').toString();
    }
    private static void map(StringBuilder b, Map<String,Long> m) {
        b.append('{'); int i = 0;
        for (var e : new TreeMap<>(m).entrySet()) { if (i++ > 0) b.append(','); str(b, e.getKey()); b.append(':').append(e.getValue()); }
        b.append('}');
    }
    private static void str(StringBuilder b, String s) {
        b.append('"');
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
            else b.append(c);
        }
        b.append('"');
    }
    private static Contribution contribution(Map<?,?> m) {
        Set<String> tracked = new HashSet<>(); for (Object o : (List<?>) m.get("tracked")) tracked.add((String) o);
        return new Contribution(longs(m.get("xp")), longs(m.get("kc")), ((Number) m.get("drops")).longValue(),
            ((Number) m.get("est")).longValue(), longs(m.get("counters")), tracked);
    }
    private static Map<String,Long> longs(Object o) {
        Map<String,Long> out = new TreeMap<>();
        for (var e : ((Map<?,?>) o).entrySet()) out.put((String) e.getKey(), ((Number) e.getValue()).longValue());
        return out;
    }
}
