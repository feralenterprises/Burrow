package gg.feral.burrow.core;

import gg.feral.burrow.core.ItemVariantPolicy.Kind;
import gg.feral.burrow.core.ItemVariantPolicy.Resolution;
import java.util.Set;

/** BUR-0016 offline assertions (synthetic bank ID sets; not native RuneLite observations). */
public final class ItemVariantPolicyTest {
    public static void main(String[] args) {
        // Glory: uncharged plan, owner has glory(6) -> show the owned glory(6) (the reported case).
        is(ItemVariantPolicy.resolve(1704, Set.of(11978)), 11978, Kind.EQUIVALENT_VARIANT);
        is(ItemVariantPolicy.resolve(1704, Set.of(1704, 11978)), 1704, Kind.EXACT);
        is(ItemVariantPolicy.resolve(1704, Set.of(10362)), 10362, Kind.EQUIVALENT_VARIANT);
        is(ItemVariantPolicy.resolve(1704, Set.of()), -1, Kind.NOT_OBSERVED);
        // Charged plan never shows an uncharged glory.
        is(ItemVariantPolicy.resolve(1712, Set.of(1704)), -1, Kind.BLOCKED_UNSAFE_VARIANT);
        is(ItemVariantPolicy.resolve(1712, Set.of(1706, 1704)), -1, Kind.BLOCKED_UNSAFE_VARIANT);
        is(ItemVariantPolicy.resolve(1712, Set.of(11976, 1706)), 11976, Kind.EQUIVALENT_VARIANT); // most charges first
        is(ItemVariantPolicy.resolve(11978, Set.of(1706)), -1, Kind.BLOCKED_UNSAFE_VARIANT);
        is(ItemVariantPolicy.resolve(11978, Set.of(11964)), 11964, Kind.EQUIVALENT_VARIANT);
        is(ItemVariantPolicy.resolveForTravel(1712, Set.of(1712), 6), -1, Kind.BLOCKED_UNSAFE_VARIANT);
        is(ItemVariantPolicy.resolveForTravel(1704, Set.of(11978), 5), 11978, Kind.EQUIVALENT_VARIANT);
        is(ItemVariantPolicy.resolveForCombatStats(1712,Set.of(1706)),1706,Kind.EQUIVALENT_VARIANT);
        // Berserker ring (i): NMZ/Soul Wars/Emir's Arena imbues count; plain and LMS rings never stand in.
        is(ItemVariantPolicy.resolve(11773, Set.of(25264)), 25264, Kind.EQUIVALENT_VARIANT);
        is(ItemVariantPolicy.resolve(11773, Set.of(6737, 25264)), 25264, Kind.EQUIVALENT_VARIANT); // RuneLite group order would pick 6737
        is(ItemVariantPolicy.resolve(11773, Set.of(6737)), -1, Kind.BLOCKED_UNSAFE_VARIANT);
        is(ItemVariantPolicy.resolve(11773, Set.of(26770)), 26770, Kind.EQUIVALENT_VARIANT); // CP-1: Emir's Arena imbue is a real (i)
        is(ItemVariantPolicy.resolve(11773, Set.of(23595)), -1, Kind.BLOCKED_UNSAFE_VARIANT); // LMS copy still withheld
        is(ItemVariantPolicy.resolve(11773, Set.of(28770)), -1, Kind.NOT_OBSERVED);            // unreviewed id never counted
        is(ItemVariantPolicy.resolve(11773, Set.of()), -1, Kind.NOT_OBSERVED);
        // Plain-ring plan may use an owned imbued ring; minigame rings never stand in.
        is(ItemVariantPolicy.resolve(6737, Set.of(11773)), 11773, Kind.EQUIVALENT_VARIANT);
        is(ItemVariantPolicy.resolve(6737, Set.of(23595)), -1, Kind.BLOCKED_UNSAFE_VARIANT);
        // Everything else is untouched.
        is(ItemVariantPolicy.resolve(4151, Set.of(12773)), 4151, Kind.NO_POLICY);
        try { ItemVariantPolicy.resolve(1704, null); throw new AssertionError("unknown bank accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("ItemVariantPolicyTest PASS (18 assertions)");
    }

    private static void is(Resolution r, int layoutId, Kind kind) {
        if (r.layoutId() != layoutId || r.kind() != kind)
            throw new AssertionError("plan " + r.planId() + ": expected " + layoutId + "/" + kind + " got " + r.layoutId() + "/" + r.kind());
    }
}
