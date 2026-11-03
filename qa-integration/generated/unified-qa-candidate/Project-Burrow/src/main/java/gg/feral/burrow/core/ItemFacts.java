package gg.feral.burrow.core;

import java.util.*;

/**
 * Build 19: equipment slot and hand-count facts used for 2H/shield coherence and switch capacity.
 *
 * <p>In RuneLite these are captured once on the client thread from
 * {@code ItemManager.getItemStats(id).getEquipment()} ({@code getSlot()}, {@code isTwoHanded()}),
 * i.e. RuneLite's bundled item stats. Offline (tests/preview) the catalog's own slot placement is
 * used for slots and hand count is UNKNOWN except for a small reviewed list.
 */
public final class ItemFacts {
    /** OSRS equipment slot index as used by RuneLite EquipmentInventorySlot. */
    public static final int HEAD=0, CAPE=1, AMULET=2, WEAPON=3, BODY=4, SHIELD=5, LEGS=7, GLOVES=9, BOOTS=10, RING=12, AMMO=13;
    public enum Hands { ONE, TWO, UNKNOWN }
    public record Fact(int equipmentSlot, Hands hands, String source) {}
    private final Map<Integer,Fact> facts;
    private ItemFacts(Map<Integer,Fact> facts) { this.facts=Map.copyOf(facts); }
    public static ItemFacts of(Map<Integer,Fact> facts) { return new ItemFacts(facts); }

    /** Catalog layout index (Burrow 11-cell order) -> RuneLite equipment slot. */
    public static final int[] CATALOG_TO_SLOT = {HEAD,CAPE,AMULET,AMMO,WEAPON,BODY,SHIELD,LEGS,GLOVES,BOOTS,RING};

    /** Reviewed offline hand-count facts. Scorching bow two-handed and Emberlight one-handed are
     *  stated by the TD/Gorilla research notes in this catalog (DemonEncounterLoadouts). */
    private static final Map<Integer,Hands> REVIEWED_HANDS = Map.of(29591,Hands.TWO, 29589,Hands.ONE);

    /** Slot facts derived from where the immutable catalog places each item. */
    public static ItemFacts catalogDerived() {
        Map<Integer,Fact> out=new HashMap<>();
        for(var boss:BurrowModel.availableBosses())
            for(var method:TieredOutfitterCatalog.methods(boss))
                for(var tier:TieredOutfitterCatalog.Tier.values()) {
                    var sel=TieredOutfitterCatalog.find(boss,method,tier);
                    if(sel==null)continue;
                    var gear=sel.profile().gear();
                    for(int i=0;i<11;i++) {
                        var item=gear.get(i);
                        if(item==null || item.id()<=0)continue;
                        Hands h=REVIEWED_HANDS.getOrDefault(item.id(),Hands.UNKNOWN);
                        out.putIfAbsent(item.id(),new Fact(CATALOG_TO_SLOT[i],h,
                            h==Hands.UNKNOWN?"catalog slot":"catalog slot + reviewed research note"));
                    }
                }
        return new ItemFacts(out);
    }
    public Optional<Fact> find(int id) { return Optional.ofNullable(facts.get(id)); }
    public int size() { return facts.size(); }
    /** Prefer host (RuneLite ItemManager) facts, fall back to catalog. */
    public ItemFacts overlay(ItemFacts host) {
        Map<Integer,Fact> m=new HashMap<>(facts);
        m.putAll(host.facts);
        return new ItemFacts(m);
    }
}
