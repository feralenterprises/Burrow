package gg.feral.burrow.core;

import java.util.*;

/**
 * Build 19: one coherent BANK + INVENTORY + EQUIPMENT observation taken on the RuneLite client
 * thread in a single tick. Holds item IDs and quantities only; no account identity or names.
 *
 * <p>Positive observations only: an item absent from this evidence is "not observed", never
 * "confirmed missing". {@link Population} records how strongly the bank container's population was
 * witnessed; {@code stale} is set when inventory/worn gear changed after the bank closed.
 */
public record BankEvidence(Map<Integer,Integer> bank, Map<Integer,Integer> inventory,
                           Map<Integer,Integer> equipment, Population population,
                           long observedAtMillis, boolean bankStillOpen, boolean stale,
                           /** CP-1: when inventory+worn were last re-read live after bank close (0 = with the bank). */
                           long carriedAtMillis) {
    /** Pre-CP-1 shape: inventory and worn gear were read together with the bank. */
    public BankEvidence(Map<Integer,Integer> bank, Map<Integer,Integer> inventory,
                        Map<Integer,Integer> equipment, Population population,
                        long observedAtMillis, boolean bankStillOpen, boolean stale) {
        this(bank,inventory,equipment,population,observedAtMillis,bankStillOpen,stale,0L);
    }
    public enum Population {
        /** Two identical consecutive tick reads AND container length >= RuneLite tab-count varbits. */
        STABLE_TAB_COUNTS_CONSISTENT,
        /** Two identical consecutive tick reads; tab-count witness unavailable. */
        STABLE_READ,
        /** A single read (offline fixtures). */
        SINGLE_READ
    }
    public BankEvidence {
        bank=Map.copyOf(bank); inventory=Map.copyOf(inventory); equipment=Map.copyOf(equipment);
        Objects.requireNonNull(population);
        for(var m:List.of(bank,inventory,equipment))
            for(var e:m.entrySet()) if(e.getKey()<=0 || e.getValue()<=0)
                throw new IllegalArgumentException("positive item observations only");
    }
    public static BankEvidence fixture(Map<Integer,Integer> combined) {
        return new BankEvidence(combined,Map.of(),Map.of(),Population.SINGLE_READ,0L,true,false);
    }
    public Map<Integer,Integer> combined() {
        Map<Integer,Integer> r=new HashMap<>(bank);
        inventory.forEach((k,v)->r.merge(k,v,Math::addExact));
        equipment.forEach((k,v)->r.merge(k,v,Math::addExact));
        return Map.copyOf(r);
    }
    /** BQA-901 BF-04: usable as planning input. A stable live read (even of an empty bank) is; an
     *  unattested single-read fixture with no bank entries is not. */
    public boolean planningInput() { return !(bank.isEmpty() && population==Population.SINGLE_READ); }
    public boolean isEmpty() { return bank.isEmpty() && inventory.isEmpty() && equipment.isEmpty(); }
    public BankEvidence closed() { return new BankEvidence(bank,inventory,equipment,population,observedAtMillis,false,stale,carriedAtMillis); }
    public BankEvidence markStale() { return new BankEvidence(bank,inventory,equipment,population,observedAtMillis,bankStillOpen,true,carriedAtMillis); }
    /**
     * CP-1 (BQA-R12-002, BANK-X01): the bank is closed, so its contents cannot have changed; inventory and worn gear
     * are replaced by a complete LIVE client read. Items moved between pack and worn are therefore never counted
     * twice, and equipping/unequipping is reflected without pretending the bank itself is current.
     */
    public BankEvidence withLiveCarried(Map<Integer,Integer> liveInventory, Map<Integer,Integer> liveEquipment, long now) {
        if (bankStillOpen) throw new IllegalStateException("bank open: take a full snapshot instead");
        return new BankEvidence(bank, liveInventory, liveEquipment, population, observedAtMillis, false, false, Math.max(1L, now));
    }
    public boolean carriedLive() { return carriedAtMillis > 0L && !bankStillOpen; }
    public String trustLine() {
        String p=switch(population){
            case STABLE_TAB_COUNTS_CONSISTENT -> "stable read, tab counts consistent";
            case STABLE_READ -> "stable read";
            case SINGLE_READ -> "single read";
        };
        // BQA-901 BF-04: a source-qualified empty bank is stated as such, never as "unknown".
        String bankPart=bank.isEmpty()&&population!=Population.SINGLE_READ?"bank empty (confirmed), ":bank.size()+" bank types, ";
        return (stale?"STALE · ":bankStillOpen?"LIVE · ":carriedLive()?"BANK AS OF CLOSE · PACK+WORN LIVE · ":"AS OF BANK CLOSE · ")+bankPart
            +inventory.size()+" inv, "+equipment.size()+" worn · "+p;
    }
}
