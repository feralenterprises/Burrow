package gg.feral.burrow.core;

import java.util.*;

/**
 * CP-7 (owner QA 2026-11-01 17:58): "Best in Bank" fills slots the Wiki plan leaves empty with the player's OWN best
 * item for that slot, scored from RuneLite's item stats ({@code ItemManager.getItemStats}) captured on the client
 * thread for observed items only. The player chooses what "best" means: {@link Focus#DAMAGE} (strength for the
 * plan's style), {@link Focus#DEFENCE} or {@link Focus#PRAYER}.
 *
 * <p>Never applied to the weapon, to projectile ammunition, or to mechanic-protection slots (those keep the reviewed
 * Wiki/tier rules). Broken, inactive, uncharged and minigame copies are never picked. Wear requirements of arbitrary
 * items are not known here, so every pick carries a "check requirements" note. Pure Java, no RuneLite types.
 */
public final class OwnedGearStats {
    private OwnedGearStats() {}

    public enum Focus {
        /** Owner rule (2026-11-01 18:03): observe the Wiki item, find the owned item with the closest stats in every
         *  category, choose the closest match. Default. */
        MATCH("≈ Wiki"), DAMAGE("Damage"), DEFENCE("Defence"), PRAYER("Prayer");
        public final String label;
        Focus(String label) { this.label = label; }
        public Focus next() { return values()[(ordinal() + 1) % values().length]; }
        public static Focus parse(String s) {
            try { return s == null ? MATCH : valueOf(s); } catch (IllegalArgumentException e) { return MATCH; }
        }
    }

    /** Equipment bonuses of one item, as RuneLite reports them. {@code slot} uses RuneLite equipment slot indices. */
    public record Stats(int slot, boolean twoHanded, double astab, double aslash, double acrush, double amagic, double arange,
                        double dstab, double dslash, double dcrush, double dmagic, double drange,
                        double str, double rstr, double mdmg, double prayer) {
        public double defenceTotal() { return dstab + dslash + dcrush + dmagic + drange; }
        public double meleeAttack() { return Math.max(astab, Math.max(aslash, acrush)); }
    }

    public record Pick(int itemId, String name, double score, String why, boolean twoHanded) {
        public Pick(int itemId, String name, double score, String why) { this(itemId, name, score, why, false); }
    }

    enum Style { MELEE, RANGED, MAGIC }
    static Style style(String profileStyle) {
        String s = profileStyle == null ? "" : profileStyle.toLowerCase(Locale.ROOT);
        if (s.contains("ranged") || s.contains("range")) return Style.RANGED;
        if (s.contains("magic") || s.contains("mage")) return Style.MAGIC;
        return Style.MELEE;
    }

    /** Higher is better. Small tie-breakers keep the choice stable and sensible. MATCH uses {@link #distance}. */
    static double score(Stats st, Style style, Focus focus) {
        return switch (focus) {
            case MATCH -> score(st, style, Focus.DAMAGE);
            case DAMAGE -> switch (style) {
                case MELEE -> st.str() * 3 + st.meleeAttack() + st.prayer() * 0.5 + st.defenceTotal() * 0.02;
                case RANGED -> st.rstr() * 3 + st.arange() + st.prayer() * 0.5 + st.defenceTotal() * 0.02;
                case MAGIC -> st.mdmg() * 6 + st.amagic() + st.prayer() * 0.5 + st.defenceTotal() * 0.02;
            };
            case DEFENCE -> st.defenceTotal() + st.prayer() * 0.5 + (style == Style.MAGIC ? st.amagic() * 0.1 : 0);
            case PRAYER -> st.prayer() * 10 + st.defenceTotal() * 0.05
                + (style == Style.MELEE ? st.str() : style == Style.RANGED ? st.rstr() : st.mdmg()) * 0.5;
        };
    }

    // ---------- closest-stat match (Focus.MATCH) ----------
    static double[] vector(Stats s) {
        return new double[]{s.astab(), s.aslash(), s.acrush(), s.amagic(), s.arange(), s.dstab(), s.dslash(), s.dcrush(),
            s.dmagic(), s.drange(), s.str(), s.rstr(), s.mdmg(), s.prayer()};
    }
    static final String[] CATEGORY = {"stab atk","slash atk","crush atk","magic atk","ranged atk","stab def","slash def",
        "crush def","magic def","ranged def","str","ranged str","magic dmg","prayer"};
    // Category scales so strength/prayer points weigh like attack/defence points (str and prayer ranges are smaller).
    static final double[] SCALE = {1,1,1,1,1,1,1,1,1,1,1.5,1.5,3,3};
    /** A surplus over the Wiki item (more of a stat than it has) costs only this fraction of a shortfall. */
    static final double SURPLUS_COST = 0.1;
    /**
     * Weighted distance from the Wiki item: every category counts, and categories the Wiki item leans on count more
     * (weight = 1 + |target_k| / max|target|), so a strength/crush weapon is matched by a strength/crush weapon.
     * Asymmetric: falling SHORT of the Wiki item costs fully; having MORE of a stat costs only {@link #SURPLUS_COST}.
     */
    public static double distance(Stats target, Stats candidate) {
        double[] t = vector(target), c = vector(candidate);
        double max = 1;
        for (int k = 0; k < t.length; k++) max = Math.max(max, Math.abs(t[k] * SCALE[k]));
        double d = 0;
        for (int k = 0; k < t.length; k++) {
            double w = 1 + Math.abs(t[k] * SCALE[k]) / max;
            double diff = (c[k] - t[k]) * SCALE[k];
            d += w * (diff < 0 ? -diff : diff * SURPLUS_COST);
        }
        return d;
    }
    static final Stats NOTHING = new Stats(-1, false, 0,0,0,0,0, 0,0,0,0,0, 0,0,0,0);

    /** Closest owned item to the Wiki item's stats; only if wearing it is closer than leaving the slot empty. */
    public static Optional<Pick> closest(int runeliteSlot, String profileStyle, Stats target, String targetName,
                                         Map<Integer,Integer> remaining, Map<Integer,Stats> stats, Map<Integer,String> names,
                                         Set<Integer> exclude) {
        if (target == null || stats == null || remaining == null) return Optional.empty();
        Style style = style(profileStyle);
        double empty = distance(target, NOTHING);
        Pick best = null; double bestD = Double.MAX_VALUE;
        for (var e : stats.entrySet()) {
            int id = e.getKey(); Stats st = e.getValue();
            if (!eligible(id, st, runeliteSlot, style, remaining, names, exclude)) continue;
            double d = distance(target, st);
            if (d >= empty) continue;
            if (d < bestD || (d == bestD && best != null && id < best.itemId())) {
                bestD = d;
                String name = names == null ? null : names.get(id);
                best = new Pick(id, name == null ? "Item " + id : name, -d,
                    "Closest owned match to " + (targetName == null ? "the Wiki item" : targetName) + ": " + summary(st, target), st.twoHanded());
            }
        }
        return Optional.ofNullable(best);
    }
    /** Minimum gain for an owned item to replace an OWNED Wiki item: 10% and 3 points on the focus score. */
    static final double UPGRADE_RATIO = 1.10, UPGRADE_MIN_GAIN = 3;
    /**
     * Owned item clearly better than the Wiki item the player already wears in this slot (e.g. Antler guard over a
     * god book on a ranged plan). MATCH scores by the plan's style damage (+ prayer, small defence term), as the owner
     * judged in native QA; the Defence/Prayer focuses score by that focus instead.
     */
    static boolean dominates(Stats a, Stats b) {
        double gain = a.astab() - b.astab() + a.aslash() - b.aslash() + a.acrush() - b.acrush() + a.amagic() - b.amagic() + a.arange() - b.arange()
            + a.dstab() - b.dstab() + a.dslash() - b.dslash() + a.dcrush() - b.dcrush() + a.dmagic() - b.dmagic() + a.drange() - b.drange()
            + a.str() - b.str() + a.rstr() - b.rstr() + a.mdmg() - b.mdmg() + a.prayer() - b.prayer();
        return gain >= UPGRADE_MIN_GAIN && a.astab() >= b.astab() && a.aslash() >= b.aslash() && a.acrush() >= b.acrush() && a.amagic() >= b.amagic() && a.arange() >= b.arange()
            && a.dstab() >= b.dstab() && a.dslash() >= b.dslash() && a.dcrush() >= b.dcrush() && a.dmagic() >= b.dmagic() && a.drange() >= b.drange()
            && a.str() >= b.str() && a.rstr() >= b.rstr() && a.mdmg() >= b.mdmg() && a.prayer() >= b.prayer();
    }
    /** Best owned Ranged or Magic weapon by stats. Ranged weapons must be known to the ammo rules and able to fire. */
    public static Optional<Pick> bestRangedOrMagic(String profileStyle, Map<Integer,Integer> remaining, Map<Integer,Stats> stats,
                                                   Map<Integer,String> names, Set<Integer> exclude) {
        Style style = style(profileStyle);
        if (style == Style.MELEE || stats == null || remaining == null || names == null) return Optional.empty();
        Pick best = null;
        for (var e : stats.entrySet()) {
            int id = e.getKey(); Stats st = e.getValue(); String name = names.get(id);
            if (st == null || st.slot() != ItemFacts.WEAPON || name == null || unusableName(name) || remaining.getOrDefault(id, 0) <= 0 || exclude.contains(id)) continue;
            double sc;
            if (style == Style.RANGED) {
                if (st.arange() <= st.meleeAttack() || gg.feral.burrow.bib.AmmoRules.check(name, null) == gg.feral.burrow.bib.AmmoRules.Status.UNKNOWN_WEAPON) continue;
                if (!gg.feral.burrow.bib.AmmoRules.slotIsFree(name) && bestCompatibleAmmo(name, 1, remaining, stats, names).isEmpty()) continue;
                sc = st.arange() + st.rstr() * 3;
            } else {
                if (st.amagic() <= st.meleeAttack() || st.rstr() > 0) continue;
                sc = st.amagic() + st.mdmg() * 6;
            }
            if (sc > 0 && (best == null || sc > best.score() || (sc == best.score() && id < best.itemId())))
                best = new Pick(id, name, sc, "Best owned " + style.name().toLowerCase(Locale.ROOT) + " weapon by stats", st.twoHanded());
        }
        return Optional.ofNullable(best);
    }
    public static Optional<Pick> upgradeOver(int runeliteSlot, String profileStyle, Focus focus, Stats current, String currentName,
                                             Map<Integer,Integer> remaining, Map<Integer,Stats> stats, Map<Integer,String> names,
                                             Set<Integer> exclude) {
        if (current == null || stats == null || remaining == null) return Optional.empty();
        if (runeliteSlot == ItemFacts.WEAPON) return Optional.empty(); // the weapon defines the method
        Style style = style(profileStyle);
        Focus scoring = focus == Focus.MATCH ? Focus.DAMAGE : focus;
        double base = score(current, style, scoring);
        Pick best = null;
        for (var e : stats.entrySet()) {
            int id = e.getKey(); Stats st = e.getValue();
            if (!eligible(id, st, runeliteSlot, style, remaining, names, exclude)) continue;
            double sc = score(st, style, scoring);
            // Owner QA: an item equal or better in every stat wins even when the style score barely moves, as Hueycoatl
            // hide does over Black d'hide (65 more defence). A one-point difference still does not churn the plan.
            if (!(sc > base && dominates(st, current)) && (sc < base * UPGRADE_RATIO || sc < base + UPGRADE_MIN_GAIN)) continue;
            if (best == null || sc > best.score()) {
                String name = names == null ? null : names.get(id);
                best = new Pick(id, name == null ? "Item " + id : name, sc,
                    "Owned upgrade over " + currentName + " for " + scoring.label.toLowerCase(Locale.ROOT) + ": "
                        + summary(st, current), st.twoHanded());
            }
        }
        return Optional.ofNullable(best);
    }
    /** "+102 str (Wiki +80), +92 crush atk (Wiki +95)": the item's three most important categories vs the Wiki item. */
    static String summary(Stats st, Stats target) {
        double[] t = vector(target), c = vector(st);
        Integer[] order = new Integer[t.length];
        for (int k = 0; k < t.length; k++) order[k] = k;
        Arrays.sort(order, (a, b) -> Double.compare(Math.abs(t[b] * SCALE[b]), Math.abs(t[a] * SCALE[a])));
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < 3 && i < order.length; i++) {
            int k = order[i];
            if (t[k] == 0 && c[k] == 0) continue;
            parts.add(fmt(c[k]) + " " + CATEGORY[k] + " (Wiki " + fmt(t[k]) + ")");
        }
        return String.join(", ", parts);
    }
    private static boolean eligible(int id, Stats st, int runeliteSlot, Style style, Map<Integer,Integer> remaining,
                                    Map<Integer,String> names, Set<Integer> exclude) {
        if (st == null || st.slot() != runeliteSlot || remaining.getOrDefault(id, 0) <= 0) return false;
        if (exclude != null && exclude.contains(id)) return false;
        if (in(ItemVariantPolicy.RING_NEVER, id) && id != 6737 || in(ItemVariantPolicy.GLORY_NEVER, id)) return false;
        if (unusableName(names == null ? null : names.get(id))) return false;
        if (runeliteSlot == ItemFacts.WEAPON && (style != Style.MELEE || st.rstr() > 0 || st.arange() > st.meleeAttack()
                || st.amagic() > st.meleeAttack() || (st.str() <= 0 && st.meleeAttack() <= 0))) return false;
        if (runeliteSlot == ItemFacts.AMMO && (style == Style.RANGED || st.rstr() > 0)) return false;
        return true;
    }

    private static final List<String> NEVER = List.of("(broken)", "(inactive)", "(uncharged)", "(empty)", "(deadman)", "(bh)",
        "(nz)", "(last man standing)");
    static boolean unusableName(String name) {
        if (name == null) return false;
        String n = name.toLowerCase(Locale.ROOT).trim();
        if (n.matches(".* 0$")) return true; // broken Barrows
        return NEVER.stream().anyMatch(n::contains);
    }

    /**
     * Best owned item for one RuneLite equipment slot. Only items with remaining (unallocated) stock, known stats for
     * exactly that slot, a usable name and a strictly positive score for the focus are considered.
     */
    public static Optional<Pick> best(int runeliteSlot, String profileStyle, Focus focus, Map<Integer,Integer> remaining,
                                      Map<Integer,Stats> stats, Map<Integer,String> names, Set<Integer> exclude) {
        if (stats == null || stats.isEmpty() || remaining == null) return Optional.empty();
        Style style = style(profileStyle);
        Pick best = null;
        for (var e : stats.entrySet()) {
            int id = e.getKey(); Stats st = e.getValue();
            // Weapon: melee plans and genuine melee weapons only. Ammo: blessings only, never for ranged plans.
            if (!eligible(id, st, runeliteSlot, style, remaining, names, exclude)) continue;
            String name = names == null ? null : names.get(id);
            double sc = score(st, style, focus);
            if (sc <= 0) continue;
            if (best == null || sc > best.score() || (sc == best.score() && id < best.itemId()))
                best = new Pick(id, name == null ? "Item " + id : name, sc, describe(st, style, focus), st.twoHanded());
        }
        return Optional.ofNullable(best);
    }

    static String describe(Stats st, Style style, Focus focus) {
        List<String> parts = new ArrayList<>();
        switch (style) {
            case MELEE -> { if (st.str() != 0) parts.add(fmt(st.str()) + " str"); if (st.meleeAttack() != 0) parts.add(fmt(st.meleeAttack()) + " best melee atk"); }
            case RANGED -> { if (st.rstr() != 0) parts.add(fmt(st.rstr()) + " ranged str"); if (st.arange() != 0) parts.add(fmt(st.arange()) + " ranged atk"); }
            case MAGIC -> { if (st.mdmg() != 0) parts.add(fmt(st.mdmg()) + "% magic dmg"); if (st.amagic() != 0) parts.add(fmt(st.amagic()) + " magic atk"); }
        }
        if (st.defenceTotal() != 0) parts.add(fmt(st.defenceTotal()) + " total def");
        if (st.prayer() != 0) parts.add(fmt(st.prayer()) + " prayer");
        return "Best owned for " + focus.label.toLowerCase(Locale.ROOT) + (parts.isEmpty() ? "" : ": " + String.join(", ", parts));
    }
    private static String fmt(double v) {
        String s = v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
        return v > 0 ? "+" + s : s;
    }
    private static boolean in(int[] xs, int v) { for (int x : xs) if (x == v) return true; return false; }

    // ---------- Void / Elite Void option ----------
    public record VoidOption(String label, List<Integer> itemIds) {}
    /**
     * A full owned Void set for the plan's style (style helm + top + robe + gloves; elite pieces preferred; Trouver
     * "(l)" copies accepted by name). Shown as an OPTION to compare, never auto-swapped: the set effect only pays off
     * against the Wiki plan's own armour in some fights.
     */
    public static Optional<VoidOption> voidOption(String profileStyle, Map<Integer,Integer> owned, Map<Integer,String> names) {
        if (owned == null || names == null) return Optional.empty();
        Style style = style(profileStyle);
        String helm = switch (style) { case MELEE -> "void melee helm"; case RANGED -> "void ranger helm"; case MAGIC -> "void mage helm"; };
        Integer h = byName(owned, names, helm), g = byName(owned, names, "void knight gloves");
        Integer eTop = byName(owned, names, "elite void top"), eRobe = byName(owned, names, "elite void robe");
        Integer top = eTop != null ? eTop : byName(owned, names, "void knight top");
        Integer robe = eRobe != null ? eRobe : byName(owned, names, "void knight robe");
        if (h == null || g == null || top == null || robe == null) return Optional.empty();
        boolean elite = eTop != null && eRobe != null;
        String styleLabel = style == Style.MELEE ? "melee" : style == Style.RANGED ? "ranged" : "magic";
        return Optional.of(new VoidOption((elite ? "Elite Void" : "Void") + " (" + styleLabel + ")", List.of(h, top, robe, g)));
    }
    private static Integer byName(Map<Integer,Integer> owned, Map<Integer,String> names, String base) {
        for (var e : names.entrySet()) {
            if (owned.getOrDefault(e.getKey(), 0) <= 0 || e.getValue() == null) continue;
            String n = e.getValue().toLowerCase(Locale.ROOT).trim();
            if (n.equals(base) || n.equals(base + " (l)") || n.equals(base + " (or)")) return e.getKey();
        }
        return null;
    }

    // ---------- CP-8: crossbow bolt review (owner QA 18:21: "I don't use broad bolts when I have diamond (e) bolts") ----------
    /** Crossbow tier from its name: bronze 1 ... rune 6, dragon/Armadyl/Zaryte/dragon hunter 7; 0 = not a crossbow. */
    static int crossbowTier(String weapon) {
        if (weapon == null) return 0;
        String w = weapon.toLowerCase(Locale.ROOT);
        if (!w.contains("crossbow")) return 0;
        if (w.contains("dragon") || w.contains("armadyl") || w.contains("zaryte")) return 7;
        if (w.contains("rune")) return 6;
        if (w.contains("adamant")) return 5;
        if (w.contains("mithril")) return 4;
        if (w.contains("steel")) return 3;
        if (w.contains("iron") || w.contains("blurite")) return 2;
        return 1;
    }
    /** Bolt tier by its name (gem-tipped bolts take their metal's tier); -1 = unrecognised, never picked. */
    static int boltTier(String name) {
        if (name == null) return -1;
        String n = name.toLowerCase(Locale.ROOT);
        if (!n.contains("bolts") || n.contains("bolt rack")) return -1;
        if (n.contains("dragon bolts")) return 7;                   // "Diamond dragon bolts (e)", "Dragon bolts"
        // Step 2: the Wiki limits broad bolts to rune tier and above (rune, dragon, dragon hunter, Armadyl, Zaryte).
        if (n.contains("broad") ) return 6;
        if (n.startsWith("runite") || n.startsWith("dragonstone") || n.startsWith("onyx")) return 6;
        if (n.startsWith("adamant") || n.startsWith("ruby") || n.startsWith("diamond")) return 5;
        if (n.startsWith("mithril") || n.startsWith("sapphire") || n.startsWith("emerald")) return 4;
        if (n.startsWith("steel") || n.startsWith("topaz")) return 3;
        if (n.startsWith("iron") || n.startsWith("pearl") || n.startsWith("blurite") || n.startsWith("jade")) return 2;
        if (n.startsWith("bronze") || n.startsWith("opal")) return 1;
        return -1;
    }
    /**
     * Highest ranged-strength owned bolts the crossbow can fire, preferring stacks that cover the trip budget
     * ({@code need}). Returns the pick with the quantity to bring (min(owned, need)).
     */
    /** Step 2: best owned ammo the typed weapon rules accept, for any weapon that fires ammo. */
    public static Optional<Pick> bestCompatibleAmmo(String weaponName, int need, Map<Integer,Integer> remaining,
                                                    Map<Integer,Stats> stats, Map<Integer,String> names) {
        return bestCompatibleAmmo(weaponName, need, remaining, stats, names, Set.of());
    }
    public static Optional<Pick> bestCompatibleAmmo(String weaponName, int need, Map<Integer,Integer> remaining,
                                                    Map<Integer,Stats> stats, Map<Integer,String> names, Set<Integer> exclude) {
        if (weaponName == null || stats == null || names == null || remaining == null) return Optional.empty();
        Pick best = null; boolean bestEnough = false; double bestR = -1;
        for (var e : stats.entrySet()) {
            int id = e.getKey(); Stats st = e.getValue();
            int have = remaining.getOrDefault(id, 0);
            if (st == null || st.slot() != ItemFacts.AMMO || have <= 0 || exclude.contains(id)) continue;
            String name = names.get(id);
            if (name == null || unusableName(name)) continue;
            if (gg.feral.burrow.bib.AmmoRules.check(weaponName, name) != gg.feral.burrow.bib.AmmoRules.Status.OK) continue;
            boolean enough = have >= Math.max(1, need);
            if (best == null || (enough && !bestEnough) || (enough == bestEnough && st.rstr() > bestR)) {
                best = new Pick(id, name, Math.min(have, Math.max(1, need)),
                    "Best owned ammo " + weaponName + " can fire: " + fmt(st.rstr()) + " ranged str"
                        + (enough ? "" : " (only " + have + " owned)"), false);
                bestEnough = enough; bestR = st.rstr();
            }
        }
        return Optional.ofNullable(best);
    }
    public static Optional<Pick> bestBolts(String weaponName, int need, Map<Integer,Integer> remaining,
                                           Map<Integer,Stats> stats, Map<Integer,String> names, Set<Integer> exclude) {
        int ct = crossbowTier(weaponName);
        if (ct == 0 || stats == null || names == null || remaining == null) return Optional.empty();
        Pick best = null; boolean bestEnough = false; double bestR = -1;
        for (var e : stats.entrySet()) {
            int id = e.getKey(); Stats st = e.getValue();
            int have = remaining.getOrDefault(id, 0);
            if (st == null || st.slot() != ItemFacts.AMMO || have <= 0 || (exclude != null && exclude.contains(id))) continue;
            String name = names.get(id);
            int bt = boltTier(name);
            if (bt < 0 || bt > ct || unusableName(name)) continue;
            // Step 2: typed weapon rules reject bolts a named crossbow cannot fire (Karil's, Hunters', Dorgeshuun...).
            if (gg.feral.burrow.bib.AmmoRules.check(weaponName, name) != gg.feral.burrow.bib.AmmoRules.Status.OK) continue;
            boolean enough = have >= Math.max(1, need);
            if (best == null || (enough && !bestEnough) || (enough == bestEnough && st.rstr() > bestR)) {
                best = new Pick(id, name, Math.min(have, Math.max(1, need)),
                    "Best owned bolts for " + weaponName + ": " + fmt(st.rstr()) + " ranged str"
                        + (enough ? "" : " (only " + have + " owned)")
                        + (name.toLowerCase(Locale.ROOT).contains("(e)") ? "; enchanted effect differs, check it suits the boss" : ""));
                bestEnough = enough; bestR = st.rstr();
            }
        }
        return Optional.ofNullable(best);
    }
}
