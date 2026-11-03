package gg.feral.burrow.core;

import java.util.Arrays;
import java.util.Set;

/**
 * BUR-0016: reviewed owned-variant resolution for the Outfitter bank layout.
 *
 * <p>RuneLite 1.13.4 Bank Tags layouts fall back to ItemVariationMapping groups when the exact
 * planned ID is absent (LayoutManager.matchesVariant, tag runelite-parent-1.13.4). Those groups
 * are broader than Burrow may accept: the "berserker ring" group is [6737, 11773, 23595, 25264, 26770]
 * and the matcher returns the FIRST group member found, so a plan for Berserker ring (i) can be
 * rendered with the plain ring even when an imbued ring is owned. This policy resolves the plan ID
 * to an explicitly reviewed equivalent the player actually has, and blocks unsafe stand-ins.
 *
 * <p>Identities come from RuneLite 1.13.4 api ItemID / gameval ItemID names and item_variations.json:
 * glory 1704 AMULET_OF_GLORY (uncharged), 1706-1712 (1)-(4), 11976/11978 (5)/(6),
 * 10362 TRAIL_AMULET_OF_GLORY (t, uncharged), 10360-10354 (t1)-(t4), 11966/11964 (t5)/(t6),
 * 8283 POH_TROPHY_AMULETOFGLORY, 20586 BR_ (Last Man Standing);
 * ring 6737 BERZERKER_RING (not imbued), 11773 NZONE_ (NMZ imbue), 25264 SW_ (Soul Wars imbue),
 * 23595 BR_ (Last Man Standing), 26770 PVPA_ (PvP Arena / Emir's Arena imbue).
 *
 * <p>CP-1 correction (2026-11-01): the OSRS Wiki "Berserker ring (i)" version table lists exactly three
 * versions, Nightmare Zone 11773, Soul Wars 25264 and Emir's Arena 26770. The Emir's Arena (formerly PvP
 * Arena) imbue is a permanent, wearable imbued ring, so 26770 is an equivalent, not an unsafe stand-in.
 * Only the Last Man Standing copy 23595 remains withheld. ID 28770 (reported in owner QA) is NOT a listed
 * version and stays unreviewed until its identity is confirmed from the RuneLite 1.13.4 ItemID table.
 * Pure Java: no RuneLite types, no ownership inference beyond the supplied bank ID set.
 */
public final class ItemVariantPolicy {
    private ItemVariantPolicy() {}

    public enum Kind {
        /** The planned ID itself is in the bank. */
        EXACT,
        /** A reviewed equivalent the player owns is shown instead (its real icon and charges). */
        EQUIVALENT_VARIANT,
        /** No positively observed matching variant; absence cannot be proved, so do not substitute. */
        NOT_OBSERVED,
        /** No matching variant observed but an unsafe stand-in is present; never substitute it. */
        BLOCKED_UNSAFE_VARIANT,
        /** No reviewed policy for this item: unchanged behaviour. */
        NO_POLICY
    }

    public record Resolution(int planId, int layoutId, Kind kind, String note) {}

    // Preference order: most charges first; trimmed variants are cosmetic equivalents.
    static final int[] GLORY_CHARGED = {11978, 11964, 11976, 11966, 1712, 10354, 1710, 10356, 1708, 10358, 1706, 10360};
    static final int[] GLORY_UNCHARGED = {1704, 10362};
    static final int[] GLORY_NEVER = {8283, 20586};
    static final int[] RING_IMBUED = {11773, 25264, 26770};
    static final int[] RING_NEVER = {6737, 23595};

    /** Variants whose automatic Bank Tags substitution needs verified positive observations. */
    public static boolean isGovernedVariant(int planId) {
        return in(GLORY_UNCHARGED, planId) || in(GLORY_CHARGED, planId)
                || in(RING_IMBUED, planId) || planId == 6737;
    }

    public static Resolution resolve(int planId, Set<Integer> bankIds) {
        if (bankIds == null) throw new IllegalArgumentException("bank IDs required; unknown bank is not empty");
        if (bankIds.contains(planId)) return new Resolution(planId, planId, Kind.EXACT, null);
        if (in(GLORY_UNCHARGED, planId)) {
            // Uncharged glory requested: any wearable glory is the same item with equal or more capability.
            int owned = first(concat(GLORY_CHARGED, GLORY_UNCHARGED), bankIds);
            if (owned != -1) return new Resolution(planId, owned, Kind.EQUIVALENT_VARIANT, "Amulet of glory variant owned");
            return notOwned(planId, GLORY_NEVER, bankIds, "Amulet of glory");
        }
        if (in(GLORY_CHARGED, planId)) {
            // W07: when the plan names a charge count, a lower-charge variant
            // is combat-equivalent but NOT a safe teleport-itinerary substitute.
            int minimum=charges(planId), owned=-1;
            for(int candidate:GLORY_CHARGED) if(charges(candidate)>=minimum && bankIds.contains(candidate)) {
                owned=candidate;break;
            }
            if (owned != -1) return new Resolution(planId, owned, Kind.EQUIVALENT_VARIANT,
                "Amulet of glory with at least "+minimum+" teleport charge(s) observed");
            return notOwned(planId, concat(GLORY_CHARGED, concat(GLORY_UNCHARGED,GLORY_NEVER)), bankIds,
                "Amulet of glory requires "+minimum+" charges; observed lower-charge items withheld");
        }
        if (in(RING_IMBUED, planId)) {
            int owned = first(RING_IMBUED, bankIds);
            if (owned != -1) return new Resolution(planId, owned, Kind.EQUIVALENT_VARIANT, "imbued Berserker ring owned");
            return notOwned(planId, RING_NEVER, bankIds, "Berserker ring (i) (plain or minigame ring not substituted)");
        }
        if (in(RING_NEVER, planId) && planId == 6737) {
            // A plain-ring plan may use an imbued ring the player owns (strictly better, same slot).
            int owned = first(RING_IMBUED, bankIds);
            if (owned != -1) return new Resolution(planId, owned, Kind.EQUIVALENT_VARIANT, "imbued Berserker ring owned");
            return notOwned(planId, new int[]{23595}, bankIds, "Berserker ring");
        }
        return new Resolution(planId, planId, Kind.NO_POLICY, null);
    }

    /** Source-backed exact item-ID charge counts; cosmetics retain the same charge count. */
    public static int charges(int id) {
        return switch(id) {
            case 11978,11964 -> 6;
            case 11976,11966 -> 5;
            case 1712,10354 -> 4;
            case 1710,10356 -> 3;
            case 1708,10358 -> 2;
            case 1706,10360 -> 1;
            case 1704,10362 -> 0;
            default -> -1;
        };
    }

    /** Trip planners can require multiple remaining teleports, beyond a generic charged item. */
    public static Resolution resolveForTravel(int planId, Set<Integer> bankIds, int minimumCharges) {
        if(bankIds==null) throw new IllegalArgumentException("bank IDs required");
        if(minimumCharges<1 || minimumCharges>6) throw new IllegalArgumentException("Invalid trip charge requirement");
        if(!isGlory(planId)) return resolve(planId,bankIds);
        int needed=Math.max(charges(planId),minimumCharges);
        for(int candidate:GLORY_CHARGED) if(charges(candidate)>=needed && bankIds.contains(candidate))
            return new Resolution(planId,candidate,candidate==planId?Kind.EXACT:Kind.EQUIVALENT_VARIANT,
                "Amulet of glory has "+charges(candidate)+" charges for "+needed+" required teleports");
        return notOwned(planId,concat(GLORY_CHARGED,concat(GLORY_UNCHARGED,GLORY_NEVER)),bankIds,
            "Need at least "+needed+" remaining glory teleports");
    }
    /** A combat-only stat comparison may disregard teleport charges; never use for route layout. */
    public static Resolution resolveForCombatStats(int planId, Set<Integer> bankIds) {
        if(bankIds==null) throw new IllegalArgumentException("bank IDs required");
        if(in(GLORY_CHARGED,planId) && !bankIds.contains(planId)) {
            int owned=first(GLORY_CHARGED,bankIds);
            if(owned!=-1) return new Resolution(planId,owned,Kind.EQUIVALENT_VARIANT,
                "Combat stats equivalent; teleport charges may differ");
        }
        return resolve(planId,bankIds);
    }
    private static boolean isGlory(int id) { return in(GLORY_CHARGED,id)||in(GLORY_UNCHARGED,id); }

    private static Resolution notOwned(int planId, int[] unsafe, Set<Integer> bank, String what) {
        if (first(unsafe, bank) != -1) return new Resolution(planId, -1, Kind.BLOCKED_UNSAFE_VARIANT, what + " not safely observed; unsuitable alternate withheld");
        return new Resolution(planId, -1, Kind.NOT_OBSERVED, what + " not confirmed in current observed bank entries");
    }

    private static boolean in(int[] xs, int v) { for (int x : xs) if (x == v) return true; return false; }
    private static int first(int[] xs, Set<Integer> bank) { for (int x : xs) if (bank.contains(x)) return x; return -1; }
    private static int[] concat(int[] a, int[] b) {
        int[] r = Arrays.copyOf(a, a.length + b.length); System.arraycopy(b, 0, r, a.length, b.length); return r;
    }
}
