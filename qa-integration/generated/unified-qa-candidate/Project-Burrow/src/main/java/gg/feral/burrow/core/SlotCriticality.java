package gg.feral.burrow.core;

import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import java.util.*;

/**
 * CP-1 (BQA-R12-001, TRIP-X02): which planned gear slots are real combat BLOCKERS when not owned, and which are
 * optional UPGRADES whose absence leaves the slot empty but the trip still playable.
 *
 * <p>Blocking: the weapon; projectile ammunition for a weapon that fires it; and mechanic-protection items
 * without which the encounter itself is unsafe (anti-dragon shields, face protection in smoke dungeons,
 * earmuffs, nose pegs, insulated boots, mirror shields...). Everything else (helm, cape, neck, body, legs,
 * gloves, boots, ring, a non-mechanic offhand or a blessing in the ammo slot) is an upgrade: missing it lowers
 * damage or defence but does not make the trip impossible.
 *
 * <p>Set effects (Void, Barrows, Obsidian, Inquisitor, Justiciar, Moons) are judged separately by
 * {@link #setEffects}: an incomplete set loses its effect. It is a VERIFY check, or a blocker when the chosen
 * method is named after the set (e.g. "Dharok's").
 */
public final class SlotCriticality {
    private SlotCriticality() {}
    public static final int AMMO = 3, WEAPON = 4, SHIELD = 6, HEAD = 0;

    private static final List<String> MECHANIC_KEYWORDS = List.of(
        "mirror shield", "v's shield",
        "facemask", "face mask", "gas mask", "nose peg", "earmuffs", "spiny helmet",
        "insulated boots", "witchwood icon",
        // Full Shayzien (5) protects against Lizardman shaman acid: every piece is mandatory protection there.
        "shayzien");
    /** Step 2: dragonfire shields are protection only where dragonfire exists; elsewhere they are stat shields. */
    private static final List<String> DRAGONFIRE_KEYWORDS = List.of("anti-dragon", "dragonfire", "wyvern shield");
    private static final Set<String> DRAGONFIRE_BOSSES = Set.of("vorkath", "king black dragon");
    /** Step 2: never swapped out for stats, but the Wiki only recommends it, so losing it is a warning. */
    private static final String RECOMMENDED_PROTECTION = "tortugan shield";
    private static final List<String> PROJECTILE_KEYWORDS = List.of(
        "arrow", "bolts", "bolt rack", "javelin", "brutal", "ogre arrow");

    /** True when a missing item in this slot makes the whole trip invalid rather than weaker. */
    public static boolean blocking(String boss, int catalogIndex, Item planned) {
        if (planned == null || planned.id() <= 0) return false;
        String n = planned.name() == null ? "" : planned.name().toLowerCase(Locale.ROOT);
        if (catalogIndex == WEAPON) return true;
        if (catalogIndex == AMMO) return PROJECTILE_KEYWORDS.stream().anyMatch(n::contains);
        if (DRAGONFIRE_KEYWORDS.stream().anyMatch(n::contains))
            return boss == null || DRAGONFIRE_BOSSES.contains(boss.toLowerCase(Locale.ROOT));
        if (MECHANIC_KEYWORDS.stream().anyMatch(n::contains)) return true;
        // Smoke dungeon: some face protection is mandatory; the planned head item is that protection.
        if (catalogIndex == HEAD && boss != null && boss.toLowerCase(Locale.ROOT).contains("smoke devil")) return true;
        return false;
    }

    /**
     * W02: an encounter may demand protection even when a source template uses an optional
     * offhand. Do not mistake a Dragon defender or an unverified potion for witnessed protection.
     * Extra anti-fire/potion-only setups are deliberately not assessed here. Their existence does
     * not authorize this planner to show READY without proof of the full mechanic.
     */
    public static boolean demandsDragonfireWitness(String boss) {
        return boss != null && "vorkath".equalsIgnoreCase(boss.trim());
    }

    /** Recognized worn shielding, not merely a bank item, and never an explicit uncharged variant. */
    public static boolean witnessedDragonfireShield(Item item) {
        if (item == null || item.name() == null) return false;
        String n = item.name().toLowerCase(Locale.ROOT);
        if (n.contains("uncharged") || n.contains("broken") || n.contains("inactive")) return false;
        return n.contains("anti-dragon shield") || n.contains("dragonfire shield") || n.contains("dragonfire ward");
    }

    /** Step 2: mechanic protection or recommended protection; owned-stat upgrades must leave this item alone. */
    public static boolean protectedItem(String boss, int catalogIndex, Item item) {
        if (blocking(boss, catalogIndex, item)) return true;
        return item != null && item.name() != null && item.name().toLowerCase(Locale.ROOT).contains(RECOMMENDED_PROTECTION);
    }
    /** Step 2: a shown item only counts toward a planned set piece when it really belongs to that set. */
    public static boolean sameSet(Item planned, Item shown) {
        if (planned == null || planned.name() == null) return true;
        String want = planned.name().toLowerCase(Locale.ROOT);
        String have = shown == null || shown.name() == null ? "" : shown.name().toLowerCase(Locale.ROOT);
        for (SetEffect se : SetEffect.values()) if (want.contains(se.keyword)) return have.contains(se.keyword);
        return true;
    }

    /**
     * Named sets with a set effect, and how many ARMOUR pieces the effect needs (weapon excluded). A Wiki plan that
     * lists fewer pieces than that is using them as ordinary armour, so no set-effect check applies.
     */
    public enum SetEffect {
        VOID("void", 4), DHAROK("dharok", 3), VERAC("verac", 3), GUTHAN("guthan", 3), TORAG("torag", 3), KARIL("karil", 3),
        AHRIM("ahrim", 3), OBSIDIAN("obsidian", 3), INQUISITOR("inquisitor", 3), JUSTICIAR("justiciar", 3),
        BLOOD_MOON("blood moon", 3), BLUE_MOON("blue moon", 3), ECLIPSE_MOON("eclipse moon", 3);
        public final String keyword; public final int armourPieces;
        SetEffect(String keyword, int armourPieces) { this.keyword = keyword; this.armourPieces = armourPieces; }
    }
    public record SetCheck(SetEffect set, int planned, int owned, boolean methodDepends) {
        public boolean complete() { return owned >= planned; }
        /** CP-7: owning none of the set is not an "incomplete set"; the pieces are simply optional slots. */
        public boolean partial() { return owned > 0 && owned < planned; }
        public String text() {
            String label = set.keyword.substring(0, 1).toUpperCase(Locale.ROOT) + set.keyword.substring(1);
            return label + " set incomplete (" + owned + "/" + planned + " pieces owned): set effect lost"
                + (methodDepends ? "; this method depends on it" : "; damage will be lower than the Wiki plan");
        }
    }

    /**
     * Sets the plan wears in full (so their effect is intended), and how many of those pieces the plan owns.
     * {@code ownedPerSlot[i]} is true when slot i resolved to owned gear of the SAME named set.
     */
    public static List<SetCheck> setEffects(String method, List<Item> plannedGear, boolean[] ownedPerSlot) {
        List<SetCheck> out = new ArrayList<>();
        String m = method == null ? "" : method.toLowerCase(Locale.ROOT);
        for (SetEffect set : SetEffect.values()) {
            int planned = 0, owned = 0;
            for (int i = 0; i < plannedGear.size() && i < 11; i++) {
                if (i == WEAPON || i == AMMO) continue; // set effects here are armour sets
                Item it = plannedGear.get(i);
                if (it == null || it.name() == null) continue;
                if (!it.name().toLowerCase(Locale.ROOT).contains(set.keyword)) continue;
                planned++;
                if (ownedPerSlot != null && i < ownedPerSlot.length && ownedPerSlot[i]) owned++;
            }
            if (planned >= set.armourPieces) out.add(new SetCheck(set, planned, owned, m.contains(set.keyword)));
        }
        return out;
    }
}
