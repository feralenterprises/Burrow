package gg.feral.burrow.runelite;

import gg.feral.burrow.core.Outfitter;
import java.util.Arrays;

/** Burrow's existing resolved positions; no stock reads, ranking or ownership inference. */
final class L01LayoutSpecification {
    private final int[] ids;
    private L01LayoutSpecification(int[] ids) { this.ids=ids.clone(); }
    int[] itemIds() { return ids.clone(); }
    /** Sourced 8x7 bank-tag layouts bypass the physical 4x7 pack projection.
     * No quantities or ownership claims are inferred from repeated positions. */
    static L01LayoutSpecification fromExactBankPositions(int[] exact) {
        if(exact==null || exact.length!=56)throw new IllegalArgumentException("8x7 bank layout required");
        int[] ids=exact.clone();
        for(int i=0;i<ids.length;i++)if(ids[i]<=0)ids[i]=-1;
        return new L01LayoutSpecification(ids);
    }
    static L01LayoutSpecification from(Outfitter.Plan plan) {
        return from(plan, null, null);
    }
    /**
     * BUR-0016: when the open bank's item IDs are known, resolve reviewed owned variants
     * (charged glory, imbued Berserker ring) and block unsafe stand-ins. A null bank set means
     * UNKNOWN, never empty. When rendering a live bank layout with notes, omit these
     * ownership-sensitive slots until trustworthy metadata is available. The standalone
     * static plan factory is intentionally unchanged (not an ownership claim).
     */
    static L01LayoutSpecification from(Outfitter.Plan plan, java.util.Set<Integer> bankIds,
                                       java.util.List<String> notes) {
        if (plan == null) throw new IllegalArgumentException("No vetted loadout for selected boss");
        int[] ids=new int[56]; Arrays.fill(ids,-1);
        for (var cell:plan.cells()) if(cell.requirement()!=null) {
            int planned=cell.requirement().itemId();
            if (bankIds == null) {
                if (notes != null && gg.feral.burrow.core.ItemVariantPolicy.isGovernedVariant(planned)) {
                    ids[cell.position()] = -1;
                    notes.add("bank data unavailable for reviewed charged glory / ring variant");
                } else {
                    ids[cell.position()] = planned;
                }
                continue;
            }
            var r=gg.feral.burrow.core.ItemVariantPolicy.resolve(planned, bankIds);
            ids[cell.position()]=r.layoutId();
            if (notes != null && (r.kind()==gg.feral.burrow.core.ItemVariantPolicy.Kind.BLOCKED_UNSAFE_VARIANT
                    || r.kind()==gg.feral.burrow.core.ItemVariantPolicy.Kind.NOT_OBSERVED))
                notes.add(r.note());
        }
        return new L01LayoutSpecification(ids);
    }
    /**
     * CP-8: apply the Best in Bank plan the player is looking at. {@code override[p]} &gt; 0 places that owned item,
     * -1 clears the position (slot left empty in the owned plan), 0 keeps the Wiki item. Length must be 56.
     */
    L01LayoutSpecification withOverride(int[] override) {
        if (override == null || override.length != 56) return this;
        int[] out = ids.clone();
        for (int p = 0; p < 56; p++) {
            if (override[p] > 0) out[p] = override[p];
            else if (override[p] < 0) out[p] = -1;
        }
        return new L01LayoutSpecification(out);
    }
}
