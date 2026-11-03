package gg.feral.burrow.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * PQA-W07 ISOLATED CANDIDATE: an account-fenced first-run evidence journal and
 * guided capture checklist. It NEVER opens widgets, injects game scripts, performs
 * game actions or converts an unobserved row into zero/completed/owned.
 *
 * A received widget PAGE is not a verified COMPLETE LOG. No supported RuneLite
 * signal in this candidate establishes exhaustive page coverage. Thus this
 * class deliberately cannot mark an entire section COMPLETE.
 */
public final class PathfinderGuidedCapture {
    public enum Section {
        BOSS_KILL_LOG("Open the boss kill log via your ring of wealth, then inspect each page"),
        SLAYER_KILL_LOG("Open the Slayer kill log via an enchanted gem or Slayer ring"),
        COLLECTION_LOG("Open Collection Log and inspect its categories or search results"),
        ACHIEVEMENT_DIARIES("Open the Achievement Diary interface and inspect regional tiers"),
        COMBAT_ACHIEVEMENTS("Open Combat Achievements and inspect completed tasks");
        private final String guidance;
        Section(String guidance) { this.guidance = guidance; }
        public String guidance() { return guidance; }
    }
    public enum Status { UNOBSERVED, REVIEWED_BY_PLAYER, PARTIALLY_WITNESSED }
    public enum Origin { NATIVE_WIDGET, PLAYER_REVIEW }

    /** Every live observation must be bound to ONE currently authenticated login. */
    public record Context(long targetHash, long sessionHash, long authenticatedHash,
                          long loginEpoch, boolean loggedIn, boolean sessionPaused, long nowMillis) {
        public boolean trusted() {
            return targetHash != -1L && sessionHash == targetHash &&
                authenticatedHash == targetHash && loginEpoch > 0 && loggedIn &&
                !sessionPaused && nowMillis > 0;
        }
    }
    /** Only exact displayed/count rows may contain 0. An absent row remains UNKNOWN. */
    public record Page(String id, Origin origin, long observedAtMillis, long loginEpoch,
                       Map<String,Long> witnessedCounts, Set<Integer> obtainedItemIds) {
        public Page {
            if(id==null || id.isBlank() || id.length()>96 || origin==null || observedAtMillis<=0 || loginEpoch<=0)
                throw new IllegalArgumentException("Invalid page provenance");
            witnessedCounts=Map.copyOf(Objects.requireNonNull(witnessedCounts));
            obtainedItemIds=Set.copyOf(Objects.requireNonNull(obtainedItemIds));
            if(witnessedCounts.size()>500 || obtainedItemIds.size()>500)
                throw new IllegalArgumentException("Page evidence too large");
            for(var e:witnessedCounts.entrySet())
                if(e.getKey()==null || e.getKey().isBlank() || e.getKey().length()>120 || e.getValue()==null || e.getValue()<0)
                    throw new IllegalArgumentException("Invalid witnessed row");
            for(int itemId:obtainedItemIds) if(itemId<=0) throw new IllegalArgumentException("Invalid item ID");
            if(origin==Origin.PLAYER_REVIEW && (!witnessedCounts.isEmpty() || !obtainedItemIds.isEmpty()))
                throw new IllegalArgumentException("Player review cannot assert machine-witnessed counts");
        }
    }
    public record SectionSnapshot(long firstSeenAtMillis, Map<String,Page> pages) {
        public SectionSnapshot { pages=Map.copyOf(pages); }
        public long lastSeenAtMillis() { return pages.values().stream().mapToLong(Page::observedAtMillis).max().orElse(0L); }
        public int nativePages() { return (int) pages.values().stream().filter(p -> p.origin()==Origin.NATIVE_WIDGET).count(); }
        public int playerReviewPages() { return (int) pages.values().stream().filter(p -> p.origin()==Origin.PLAYER_REVIEW).count(); }
        public int witnessedRows() { return pages.values().stream().filter(p -> p.origin()==Origin.NATIVE_WIDGET).mapToInt(p -> p.witnessedCounts().size()+p.obtainedItemIds().size()).sum(); }
        public Status status() {
            if(nativePages()>0) return Status.PARTIALLY_WITNESSED;
            return playerReviewPages()>0?Status.REVIEWED_BY_PLAYER:Status.UNOBSERVED;
        }
        /** Full menu coverage is unproved. No positive-only widget page proves completion. */
        public boolean provesCompleteLog() { return false; }
    }
    public record ChecklistStep(Section section, Status status, int witnessedPages,
                                int playerReviewedPages, String instruction,
                                long lastSeenAtMillis, int witnessedEvidenceRows,
                                boolean provesComplete) {
        public ChecklistStep(Section section, Status status, int witnessedPages,
                             int playerReviewedPages, String instruction) {
            this(section, status, witnessedPages, playerReviewedPages, instruction, 0L, 0, false);
        }
        public ChecklistStep(Section section, Status status, int witnessedPages,
                             int playerReviewedPages, String instruction, long lastSeenAtMillis) {
            this(section, status, witnessedPages, playerReviewedPages, instruction, lastSeenAtMillis, 0, false);
        }
        /** Seven-day stale reminder for a previously witnessed/reviewed section. */
        public boolean staleReminderDue(long nowMillis) {
            return lastSeenAtMillis > 0
                && (nowMillis < lastSeenAtMillis
                    || nowMillis - lastSeenAtMillis >= PathfinderAccountArchive.REMINDER_AFTER_MS);
        }
    }
    private static final String VERSION="PQA7";
    private static final String PREFIX="pathfinder_capture_";
    private static final int MAX_PAGES=60;
    private static final int MAX_STORED_CHARS=65_000;
    private static final long MAX_OBSERVATION_AGE=120_000;
    private final Function<String,String> reader;
    private final BiConsumer<String,String> writer;

    public PathfinderGuidedCapture(Function<String,String> reader, BiConsumer<String,String> writer) {
        this.reader=Objects.requireNonNull(reader);
        this.writer=Objects.requireNonNull(writer);
    }
    public static String key(long hash, Section section) {
        if(hash==-1L || section==null) throw new IllegalArgumentException("Account and section required");
        return PREFIX+Long.toUnsignedString(hash,16)+"_"+section.name().toLowerCase(Locale.ROOT);
    }
    /** Invalid, mismatched, truncated, or corrupted records return ABSENT, never empty/zero. */
    public Optional<SectionSnapshot> read(long hash, Section section) {
        if(hash==-1L || section==null) return Optional.empty();
        try {
            String raw=reader.apply(key(hash,section));
            if(raw==null || raw.isBlank() || raw.length()>MAX_STORED_CHARS+80)return Optional.empty();
            int finalSep=raw.lastIndexOf('|');
            if(finalSep<0 || !checksum(raw.substring(0,finalSep)).equals(raw.substring(finalSep+1)))return Optional.empty();
            String[] head=raw.substring(0,finalSep).split("\\|",5);
            if(head.length!=5 || !VERSION.equals(head[0]) || !head[1].equals(Long.toUnsignedString(hash,16)) ||
               !head[2].equals(section.name()))return Optional.empty();
            long first=Long.parseLong(head[3]);
            if(first<=0)return Optional.empty();
            Map<String,Page> pages=new LinkedHashMap<>();
            if(!head[4].isEmpty())for(String packed:head[4].split(";",-1)) {
                String[] v=packed.split("~",6);
                if(v.length!=6)return Optional.empty();
                String pageId=decode(v[0]);
                Origin origin=Origin.valueOf(v[1]);
                long at=Long.parseLong(v[2]),epoch=Long.parseLong(v[3]);
                Map<String,Long> counts=new TreeMap<>();
                if(!v[4].isEmpty())for(String c:v[4].split(",",-1)) {
                    String[] parts=c.split("=",2);if(parts.length!=2)return Optional.empty();
                    if(counts.putIfAbsent(decode(parts[0]),Long.parseLong(parts[1]))!=null)return Optional.empty();
                }
                Set<Integer> items=new TreeSet<>();
                if(!v[5].isEmpty())for(String item:v[5].split(",",-1))
                    if(!items.add(Integer.parseInt(item)))return Optional.empty();
                Page page=new Page(pageId,origin,at,epoch,counts,items);
                if(page.observedAtMillis()<first || pages.putIfAbsent(pageId,page)!=null)return Optional.empty();
            }
            if(pages.isEmpty() || pages.size()>MAX_PAGES)return Optional.empty();
            return Optional.of(new SectionSnapshot(first,pages));
        } catch(RuntimeException ignored) { return Optional.empty(); }
    }
    /** Only authentic, same-epoch, recent evidence is persisted. One page NEVER seals a section. */
    public boolean observe(Context context, Section section, Page page) {
        if(context==null || !context.trusted() || section==null || page==null ||
           page.loginEpoch()!=context.loginEpoch() || page.observedAtMillis()>context.nowMillis() ||
           context.nowMillis()-page.observedAtMillis()>MAX_OBSERVATION_AGE)return false;
        if(page.origin()==Origin.NATIVE_WIDGET && !allowedNativeSource(section))return false;
        // Native source integration is currently reviewed only for Collection Log pages.
        Map<String,Page> pages=new TreeMap<>();
        var saved=read(context.targetHash(),section);
        String existing=reader.apply(key(context.targetHash(),section));
        if(existing!=null && !existing.isBlank() && saved.isEmpty())return false;
        saved.ifPresent(s -> pages.putAll(s.pages()));
        Page previous=pages.get(page.id());
        if(previous!=null && previous.observedAtMillis()>=page.observedAtMillis())return false;
        // A later *manual* review never erases previously machine-witnessed rows.
        if(previous!=null && previous.origin()==Origin.NATIVE_WIDGET && page.origin()==Origin.PLAYER_REVIEW)
            return false;
        pages.put(page.id(),page);
        if(pages.size()>MAX_PAGES)return false;
        long first=saved.map(v -> Math.min(v.firstSeenAtMillis(),page.observedAtMillis())).orElse(page.observedAtMillis());
        String packed=encodeRecord(context.targetHash(),section,first,pages);
        if(packed.length()>MAX_STORED_CHARS)return false; // no silent truncation
        try { writer.accept(key(context.targetHash(),section),packed); return true; }
        catch(RuntimeException unavailable){return false;}
    }
    /** Human confirmation is audit trail only, never item/KC/diary proof. */
    public boolean markPlayerReviewed(Context c, Section s, String page, long observedAt) {
        try {return observe(c,s,new Page(page,Origin.PLAYER_REVIEW,observedAt,c.loginEpoch(),Map.of(),Set.of()));}
        catch(RuntimeException invalid){return false;}
    }
    /** Source-traced: collection-log page is already read by CollectionLogObserver. */
    public boolean observeCollectionPage(Context c, String page, Map<String,Long> visibleKillCounts,
                                         Set<Integer> positivelyObtainedIds, long observedAt) {
        try { return observe(c,Section.COLLECTION_LOG,new Page(page,Origin.NATIVE_WIDGET,observedAt,
            c.loginEpoch(),visibleKillCounts,positivelyObtainedIds)); }
        catch(RuntimeException invalid){return false;}
    }
    /** PQA-W09: caller must first parse an exact Kill Log widget page through
     * KillLogWidgetReader. A capped Lots! row is deliberately omitted, never zero.
     * As with Collection Log, this is one partial page, not full log coverage. */
    public boolean observeKillLogPage(Context context, gg.feral.burrow.core.KillLogPageParser.Observation page,
                                      long observedAtMillis) {
        if (page == null || context == null) return false;
        Section section = page.kind() == gg.feral.burrow.core.KillLogPageParser.Kind.BOSS
            ? Section.BOSS_KILL_LOG : Section.SLAYER_KILL_LOG;
        try {
            return observe(context, section, new Page(page.pageId(), Origin.NATIVE_WIDGET,
                observedAtMillis, context.loginEpoch(), page.exactCounts(), Set.of()));
        } catch (RuntimeException invalid) { return false; }
    }
    private static boolean allowedNativeSource(Section s) {
        return s==Section.COLLECTION_LOG || s==Section.BOSS_KILL_LOG || s==Section.SLAYER_KILL_LOG;
    }
    public List<ChecklistStep> checklist(long hash) {
        List<ChecklistStep> items=new ArrayList<>();
        for(Section s:Section.values()) {
            var v=read(hash,s);
            Status status=v.map(SectionSnapshot::status).orElse(Status.UNOBSERVED);
            items.add(new ChecklistStep(s,status,v.map(SectionSnapshot::nativePages).orElse(0),
                v.map(SectionSnapshot::playerReviewPages).orElse(0),s.guidance(),
                v.map(SectionSnapshot::lastSeenAtMillis).orElse(0L),
                v.map(SectionSnapshot::witnessedRows).orElse(0),
                v.map(SectionSnapshot::provesCompleteLog).orElse(false)));
        }
        return List.copyOf(items);
    }
    public Optional<ChecklistStep> nextUnobserved(long hash) {
        return checklist(hash).stream().filter(s -> s.status()==Status.UNOBSERVED).findFirst();
    }
    private static String encodeRecord(long hash,Section section,long first,Map<String,Page> pages) {
        StringJoiner rows=new StringJoiner(";");
        for(Page p:pages.values()) {
            StringJoiner counts=new StringJoiner(","),items=new StringJoiner(",");
            p.witnessedCounts().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->counts.add(token(e.getKey())+"="+e.getValue()));
            p.obtainedItemIds().stream().sorted().forEach(id->items.add(Integer.toString(id)));
            rows.add(token(p.id())+"~"+p.origin()+"~"+p.observedAtMillis()+"~"+p.loginEpoch()+"~"+counts+"~"+items);
        }
        String data=VERSION+"|"+Long.toUnsignedString(hash,16)+"|"+section+"|"+first+"|"+rows;
        return data+"|"+checksum(data);
    }
    private static String token(String s) {return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    private static String decode(String s) {return new String(Base64.getUrlDecoder().decode(s),StandardCharsets.UTF_8);}
    private static String checksum(String data) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new AssertionError(e);}
    }
}
