package gg.feral.burrow.core;

import java.util.*;

/** Build 20: computed view of one encounter's selected rare-drop goal (shared by Outfitter and Pathfinder). */
public record RareDropSummary(RareDropTable.Encounter encounter, List<RareDropTable.Drop> selected,
                              RareDropMath.Mode mode, RareDropStore.Effective kc,
                              OptionalDouble chanceNow, long[] milestoneKills) {
    public static Optional<RareDropSummary> of(RareDropTable.Encounter e, RareDropStore.State s) {
        List<RareDropTable.Drop> sel=new ArrayList<>();
        for(String k:s.selected()) e.drop(k).ifPresent(sel::add);
        if(sel.isEmpty()) return Optional.empty();
        var mode=s.mode(e.encounter());
        var kc=s.effective(e.encounter());
        OptionalDouble now=kc.known()?OptionalDouble.of(RareDropMath.chance(mode,sel,kc.value().getAsLong())):OptionalDouble.empty();
        long[] ms=new long[RareDropMath.MILESTONES.length];
        for(int i=0;i<ms.length;i++) ms[i]=RareDropMath.killsFor(mode,sel,RareDropMath.MILESTONES[i]);
        return Optional.of(new RareDropSummary(e,List.copyOf(sel),mode,kc,now,ms));
    }
    public static List<RareDropSummary> all(RareDropStore.State s) {
        List<RareDropSummary> out=new ArrayList<>();
        for(var e:RareDropTable.all()) of(e,s).ifPresent(out::add);
        return out;
    }
    /**
     * Moons of Peril duplicate protection, using ONLY observed Collection Log ownership: with r set
     * pieces still missing and s of them selected, the next roll's chance of a selected piece is
     * setTableChance * s / r per kill. It is a lower bound for "ANY selected" (r only shrinks).
     * Empty when the rule does not apply, the mode is ALL, or the log page was not observed.
     */
    public static OptionalDouble duplicateProtectedPerKill(RareDropSummary sum, RareDropStore.State st) {
        var e=sum.encounter();
        if(e.setTableChance()<=0 || sum.mode()!=RareDropMath.Mode.ANY || st==null) return OptionalDouble.empty();
        boolean observed=e.drops().stream().anyMatch(d->st.ownership(d.itemId())!=RareDropStore.Ownership.UNKNOWN);
        if(!observed) return OptionalDouble.empty();
        long missing=e.drops().stream().filter(d->st.ownership(d.itemId())!=RareDropStore.Ownership.OBSERVED_OBTAINED).count();
        long sel=sum.selected().stream().filter(d->st.ownership(d.itemId())!=RareDropStore.Ownership.OBSERVED_OBTAINED).count();
        if(missing==0 || sel==0) return OptionalDouble.empty();
        return OptionalDouble.of(e.setTableChance()*sel/missing);
    }
    public static String sourceLabel(RareDropStore.Source s) {
        return switch(s) {
            case COLLECTION_LOG -> "COLLECTION LOG";
            case CHAT -> "GAME MESSAGE";
            case HISCORES -> "OFFICIAL HISCORES";
            case MANUAL -> "MANUAL (you entered)";
        };
    }
    /** Milestone text in total kill count, plus kills remaining when the KC is genuinely known. */
    public String milestoneText(int i) {
        long n=milestoneKills[i];
        String base=Math.round(RareDropMath.MILESTONES[i]*100)+"% by "+String.format(Locale.ROOT,"%,d",n)+" KC";
        if(kc.known()) { long left=n-kc.value().getAsLong(); return base+(left<=0?" ✓":" (+"+String.format(Locale.ROOT,"%,d",left)+")"); }
        return base;
    }
}
