package gg.feral.burrow.core;

import gg.feral.burrow.pathfinder.SuggestionEngine.GoalMark;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Build 19: per-account Pathfinder goal marks (SAVED / SKIPPED / DONE), persisted through RuneLite's
 * ConfigManager under a key derived from the authenticated account hash. Written ONLY on an explicit
 * owner action (never per tick). A DONE mark is a manual override with provenance "owner marked",
 * never an observed completion. SAVED order is the owner's priority order (reorder by moving up).
 */
public final class PathfinderGoalStore {
    private static final String PREFIX = "pathfinder_goals_";
    private final Function<String,String> reader;
    private final BiConsumer<String,String> writer;
    public PathfinderGoalStore(Function<String,String> reader, BiConsumer<String,String> writer) {
        this.reader=Objects.requireNonNull(reader); this.writer=Objects.requireNonNull(writer);
    }
    public static String key(long accountHash) {
        if(accountHash==-1L)throw new IllegalArgumentException("Authenticated account required");
        return PREFIX+Long.toUnsignedString(accountHash,16);
    }
    /** Insertion-ordered marks for exactly this account. Malformed entries are ignored. */
    public LinkedHashMap<String,GoalMark> read(long accountHash) {
        LinkedHashMap<String,GoalMark> out=new LinkedHashMap<>();
        if(accountHash==-1L)return out;
        String raw=reader.apply(key(accountHash));
        if(raw==null || raw.isBlank())return out;
        for(String part:raw.split("\\|")) {
            int c=part.lastIndexOf(':');
            if(c<=0)continue;
            String id=part.substring(0,c);
            if(!id.matches("[a-z0-9_]+"))continue;
            try { out.put(id,GoalMark.valueOf(part.substring(c+1))); } catch(IllegalArgumentException ignored) {}
        }
        return out;
    }
    /** mark==null restores (removes) the entry. Returns the new map. */
    public LinkedHashMap<String,GoalMark> set(long accountHash, String ruleId, GoalMark mark) {
        if(ruleId==null || !ruleId.matches("[a-z0-9_]+"))throw new IllegalArgumentException("rule id");
        var m=read(accountHash);
        m.remove(ruleId);
        if(mark!=null)m.put(ruleId,mark);
        write(accountHash,m);
        return m;
    }
    /** Explicit owner checkbox: at most one SAVED goal; preserve other DONE/SKIPPED marks. */
    public LinkedHashMap<String,GoalMark> setSingleActive(long accountHash,String ruleId,boolean checked) {
        if(ruleId==null||!ruleId.matches("[a-z0-9_]+"))throw new IllegalArgumentException("rule id");
        var marks=read(accountHash);
        if(checked)marks.entrySet().removeIf(e->e.getValue()==GoalMark.SAVED);
        marks.remove(ruleId);
        if(checked)marks.put(ruleId,GoalMark.SAVED);
        write(accountHash,marks); // one atomic config write for the user's explicit choice
        return marks;
    }
    /** Moves a SAVED goal one position earlier among saved goals. */
    public LinkedHashMap<String,GoalMark> moveUp(long accountHash, String ruleId) {
        var m=read(accountHash);
        List<String> keys=new ArrayList<>(m.keySet());
        int i=keys.indexOf(ruleId);
        if(i>0 && m.get(ruleId)==GoalMark.SAVED){Collections.swap(keys,i,i-1);}
        LinkedHashMap<String,GoalMark> out=new LinkedHashMap<>();
        for(String k:keys)out.put(k,m.get(k));
        write(accountHash,out);
        return out;
    }
    private void write(long accountHash, Map<String,GoalMark> m) {
        StringBuilder b=new StringBuilder();
        for(var e:m.entrySet()){ if(b.length()>0)b.append('|'); b.append(e.getKey()).append(':').append(e.getValue().name()); }
        writer.accept(key(accountHash),b.toString());
    }
}
