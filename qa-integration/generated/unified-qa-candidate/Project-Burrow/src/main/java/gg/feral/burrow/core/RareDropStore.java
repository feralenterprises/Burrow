package gg.feral.burrow.core;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Build 20: per-account Rare Drop Tracker state (KC by source, Collection Log observations, owner's
 * selected drop goals). Keyed by the authenticated account hash so accounts never share data.
 *
 * <p>Every KC is an absolute value from its source and REPLACES the previous value of that source,
 * so revisiting a page or re-reading a chat message never double counts. An unvisited page has no
 * entry and reads as UNKNOWN, never zero KC or missing uniques. Manual KC is stored separately and
 * is only used when no observed source exists.
 */
public final class RareDropStore {
    public enum Source { COLLECTION_LOG, CHAT, HISCORES, MANUAL }
    public record Kc(String encounter, Source source, long value, long epochMillis) {}
    public record PageObservation(String page, long epochMillis, Set<Integer> obtained, Set<Integer> listed) {
        public PageObservation { obtained=Set.copyOf(obtained); listed=Set.copyOf(listed); }
    }
    public enum Ownership { OBSERVED_OBTAINED, OBSERVED_NOT_OBTAINED, UNKNOWN }
    public record Effective(OptionalLong value, Source source, long epochMillis) {
        public boolean known(){return value.isPresent();}
    }

    private static final String PREFIX="rare_drops_";
    private final Function<String,String> reader; private final BiConsumer<String,String> writer;
    public RareDropStore(Function<String,String> reader, BiConsumer<String,String> writer) {
        this.reader=Objects.requireNonNull(reader); this.writer=Objects.requireNonNull(writer);
    }
    public static String key(long hash) {
        if(hash==-1L) throw new IllegalArgumentException("Authenticated account required");
        return PREFIX+Long.toUnsignedString(hash,16);
    }

    /** In-memory snapshot for one account. */
    public static final class State {
        final Map<String,EnumMap<Source,Kc>> kc=new LinkedHashMap<>();
        final Map<String,PageObservation> pages=new LinkedHashMap<>();
        final LinkedHashSet<String> selected=new LinkedHashSet<>();
        final Map<String,RareDropMath.Mode> modes=new LinkedHashMap<>();
        public Optional<Kc> kc(String enc,Source s){var m=kc.get(enc);return m==null?Optional.empty():Optional.ofNullable(m.get(s));}
        public Map<String,PageObservation> pages(){return Collections.unmodifiableMap(pages);}
        public List<String> selected(){return List.copyOf(selected);}
        public RareDropMath.Mode mode(String enc){return modes.getOrDefault(enc,RareDropMath.Mode.ANY);}
        /** Highest genuine observed KC (KC only rises); manual only when nothing was observed. */
        public Effective effective(String enc) {
            var m=kc.get(enc);
            if(m==null) return new Effective(OptionalLong.empty(),null,0);
            Kc best=null;
            for(Source s:List.of(Source.COLLECTION_LOG,Source.CHAT,Source.HISCORES)) {
                Kc k=m.get(s); if(k!=null && (best==null || k.value()>best.value())) best=k;
            }
            if(best==null) best=m.get(Source.MANUAL);
            return best==null?new Effective(OptionalLong.empty(),null,0):new Effective(OptionalLong.of(best.value()),best.source(),best.epochMillis());
        }
        /** R11: where an unlock was seen: "page" (encounter page read) or "search" (full-log search), with the read time. */
        public record UnlockEvidence(String source, String page, long epochMillis) {}
        public Optional<UnlockEvidence> unlockEvidence(int itemId) {
            Set<Integer> ids=RareDropTable.equivalentIds(itemId);
            UnlockEvidence best=null;
            for(var p:pages.values()) for(int id:ids) if(p.obtained().contains(id)) {
                var e=new UnlockEvidence(SEARCH_PAGE.equals(p.page())?"search":"page",p.page(),p.epochMillis());
                if(best==null || e.epochMillis()>best.epochMillis()) best=e;
            }
            return Optional.ofNullable(best);
        }
        /** Latest read time per source for this encounter's log data ("page" = its own page; "search" = full-log search). */
        public OptionalLong lastPageRead(RareDropTable.Encounter e) {
            long t=-1;
            for(var p:pages.values()) if(p.page().equalsIgnoreCase(e.encounter())||p.page().equalsIgnoreCase(e.boss())) t=Math.max(t,p.epochMillis());
            return t<0?OptionalLong.empty():OptionalLong.of(t);
        }
        public OptionalLong lastSearchRead() { var p=pages.get(SEARCH_PAGE); return p==null?OptionalLong.empty():OptionalLong.of(p.epochMillis()); }
        /** R09 hotfix: matches the drop id AND its reviewed same-item variants (RareDropTable.equivalentIds). */
        public Ownership ownership(int itemId) {
            boolean listed=false;
            Set<Integer> ids=RareDropTable.equivalentIds(itemId);
            for(var p:pages.values()) for(int id:ids) {
                if(p.obtained().contains(id)) return Ownership.OBSERVED_OBTAINED;
                if(p.listed().contains(id)) listed=true;
            }
            return listed?Ownership.OBSERVED_NOT_OBTAINED:Ownership.UNKNOWN;
        }
        /**
         * R09 hotfix: items the Collection Log page for this encounter shows UNLOCKED that no table drop (or reviewed variant)
         * accounts for. Shown to the player as a CHECK line instead of being silently ignored.
         */
        public Set<Integer> unmatchedObtained(RareDropTable.Encounter e) {
            Set<Integer> known=new HashSet<>();
            for(var d:e.drops()) known.addAll(RareDropTable.equivalentIds(d.itemId()));
            Set<Integer> out=new TreeSet<>();
            for(var p:pages.values()) {
                if(!p.page().equalsIgnoreCase(e.encounter()) && !p.page().equalsIgnoreCase(e.boss())) continue;
                for(int id:p.obtained()) if(!known.contains(id)) out.add(id);
            }
            return out;
        }
    }

    public State read(long hash) {
        State s=new State();
        if(hash==-1L) return s;
        String raw=reader.apply(key(hash));
        if(raw==null || raw.isBlank()) return s;
        for(String line:raw.split("\n")) {
            String[] f=line.split("\t",-1);
            try {
                switch(f[0]) {
                    case "K" -> { var k=new Kc(f[1],Source.valueOf(f[2]),Long.parseLong(f[3]),Long.parseLong(f[4]));
                        if(k.value()>=0) s.kc.computeIfAbsent(k.encounter(),x->new EnumMap<>(Source.class)).put(k.source(),k); }
                    case "P" -> s.pages.put(f[1],new PageObservation(f[1],Long.parseLong(f[2]),ids(f[3]),ids(f[4])));
                    case "S" -> { String k=RareDropTable.canonicalKey(f[1]); if(RareDropTable.drop(k).isPresent()) s.selected.add(k); }
                    case "G" -> s.modes.put(f[1],RareDropMath.Mode.valueOf(f[2]));
                    default -> {}
                }
            } catch(RuntimeException malformed) { /* ignore malformed line, never invent data */ }
        }
        return s;
    }
    private static Set<Integer> ids(String csv){
        Set<Integer> out=new LinkedHashSet<>();
        if(csv!=null && !csv.isBlank()) for(String x:csv.split(",")) out.add(Integer.parseInt(x));
        return out;
    }
    private static String clean(String s){return s.replace('\t',' ').replace('\n',' ');}
    private void write(long hash,State s) {
        StringBuilder b=new StringBuilder();
        for(var m:s.kc.values()) for(var k:m.values())
            b.append("K\t").append(clean(k.encounter())).append('\t').append(k.source()).append('\t').append(k.value()).append('\t').append(k.epochMillis()).append('\n');
        for(var p:s.pages.values()) b.append("P\t").append(clean(p.page())).append('\t').append(p.epochMillis()).append('\t')
            .append(join(p.obtained())).append('\t').append(join(p.listed())).append('\n');
        for(String sel:s.selected) b.append("S\t").append(sel).append('\n');
        for(var g:s.modes.entrySet()) b.append("G\t").append(clean(g.getKey())).append('\t').append(g.getValue()).append('\n');
        writer.accept(key(hash),b.toString());
    }
    private static String join(Set<Integer> s){StringJoiner j=new StringJoiner(",");for(int i:s)j.add(Integer.toString(i));return j.toString();}

    /** Pseudo-page holding unlocks seen through the game's collection-log search (script 4100); positive-only. */
    public static final String SEARCH_PAGE="__collection_log_search__";
    /**
     * Records a Collection Log page observation. R11: historical unlocks never revert in OSRS, so a page's obtained set is the
     * UNION of every read of that page (a partial or mid-draw read can add, never remove); listed items are unioned too.
     */
    public State observe(long hash, CollectionLogParser.Observation o, long now) {
        State s=read(hash);
        var prior=s.pages.get(o.page());
        Set<Integer> obtained=new LinkedHashSet<>(o.obtained()), listed=new LinkedHashSet<>(o.listed());
        if(prior!=null){obtained.addAll(prior.obtained());listed.addAll(prior.listed());}
        s.pages.put(o.page(),new PageObservation(o.page(),now,obtained,listed));
        for(var e:o.killCounts().entrySet()) put(s,new Kc(e.getKey(),Source.COLLECTION_LOG,e.getValue(),now));
        write(hash,s); return s;
    }
    /**
     * R11: unlocks captured read-only from the game's collection-log search (script 4100 item ids, the same signal WikiSync
     * uses). Positive-only and monotonic: merged into {@link #SEARCH_PAGE}; never marks anything as not obtained.
     */
    public State observeSearchUnlocks(long hash, Set<Integer> itemIds, long now) {
        State s=read(hash);
        if(itemIds==null || itemIds.isEmpty()) return s;
        var prior=s.pages.get(SEARCH_PAGE);
        Set<Integer> obtained=new LinkedHashSet<>(prior==null?Set.of():prior.obtained());
        for(int id:itemIds) if(id>0) obtained.add(id);
        s.pages.put(SEARCH_PAGE,new PageObservation(SEARCH_PAGE,now,obtained,Set.of()));
        write(hash,s); return s;
    }
    public State recordKc(long hash, String encounter, Source source, long value, long now) {
        if(value<0) throw new IllegalArgumentException("kc");
        State s=read(hash); put(s,new Kc(encounter,source,value,now)); write(hash,s); return s;
    }
    public State clearManual(long hash, String encounter) {
        State s=read(hash); var m=s.kc.get(encounter); if(m!=null) m.remove(Source.MANUAL); write(hash,s); return s;
    }
    public State toggle(long hash, String dropKey) {
        if(RareDropTable.drop(dropKey).isEmpty()) throw new IllegalArgumentException("unknown drop");
        State s=read(hash);
        if(!s.selected.remove(dropKey)) {
            String enc=RareDropTable.encounterOf(dropKey).orElseThrow().encounter();
            long inEnc=s.selected.stream().filter(k->RareDropTable.encounterOf(k).map(e->e.encounter().equals(enc)).orElse(false)).count();
            if(inEnc>=RareDropMath.MAX_SELECTED) return s;
            s.selected.add(dropKey);
        }
        write(hash,s); return s;
    }
    public State setMode(long hash, String encounter, RareDropMath.Mode mode) {
        State s=read(hash); s.modes.put(encounter,mode); write(hash,s); return s;
    }
    private static void put(State s,Kc k){s.kc.computeIfAbsent(k.encounter(),x->new EnumMap<>(Source.class)).put(k.source(),k);}
}
