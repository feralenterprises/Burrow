package gg.feral.burrow.experience;

import gg.feral.burrow.experience.AccountEvidenceContext.Check;
import gg.feral.burrow.experience.AccountEvidenceContext.Spellbook;
import java.util.*;
import java.util.function.Function;

/**
 * Sourced Spoons tips and their account gates. A tip is eligible only when every gate is MET, so unknown account state
 * never produces a claim about the account. Levels and quests were read from the linked OSRS Wiki pages on 2026-11-02.
 * Item IDs are RuneLite ItemID constants. No durations are sourced, so every tip reads "Time varies".
 */
public final class SpoonsTipCatalog {
    private static final String W = "https://oldschool.runescape.wiki/w/";
    /** Grimy herb ID to the Herblore level needed to clean it. */
    private static final Map<Integer,Integer> GRIMY = Map.ofEntries(Map.entry(199, 3), Map.entry(201, 5), Map.entry(203, 11),
        Map.entry(205, 20), Map.entry(207, 25), Map.entry(3049, 30), Map.entry(209, 40), Map.entry(211, 48), Map.entry(213, 54),
        Map.entry(3051, 59), Map.entry(215, 65), Map.entry(2485, 67), Map.entry(217, 70), Map.entry(219, 75));
    /** Uncut gem ID to the Crafting level needed to cut it. */
    private static final Map<Integer,Integer> UNCUT = Map.of(1623, 20, 1621, 27, 1619, 34, 1617, 43, 1631, 55);
    private static final int CHISEL = 1755, RAW_KARAMBWAN = 3142, NATURE_RUNE = 561, FIRE_RUNE = 554, ASTRAL_RUNE = 9075, EARTH_RUNE = 557, COINS = 995;
    private static final int LOGS = 1511, OAK_LOGS = 1521, TEAK_LOGS = 6333, MAHOGANY_LOGS = 6332;
    private static final int BONES = 526, BIG_BONES = 532, DRAGON_BONES = 536;
    // Sourced Plank Make coin cost per cast: 70/175/350/1050 GP respectively.
    private static final Map<Integer,Integer> PLANK_COSTS = Map.of(LOGS,70, OAK_LOGS,175, TEAK_LOGS,350, MAHOGANY_LOGS,1050);
    // Limited to explicitly sourced, ordinary fletched bows; do not suggest alching prized gear.
    private static final Set<Integer> REVIEWED_ALCHABLES = Set.of(851, 855, 859);

    /** One gate: result plus a short reason. */
    record Gate(Function<AccountEvidenceContext,Check> check, String reason) {}
    public record Tip(String id, String text, String sourceUrl, boolean generic, List<Gate> gates) {
        public Tip { gates = List.copyOf(gates); }
        public boolean eligible(AccountEvidenceContext account) {
            return gates.stream().allMatch(g -> g.check().apply(account) == Check.MET);
        }
        public List<String> reasons() { return gates.stream().map(Gate::reason).toList(); }
    }
    private static final List<Tip> TIPS = List.of(
        new Tip("cut_gems", "Cut the uncut gems in your bank.", W + "Uncut_sapphire", false,
            List.of(owns(CHISEL, "Chisel seen"), new Gate(a -> anyWithLevel(a, UNCUT, "Crafting"), "Uncut gems within your Crafting level"))),
        new Tip("clean_herbs", "Clean the grimy herbs in your bank.", W + "Herb", false,
            List.of(new Gate(a -> anyWithLevel(a, GRIMY, "Herblore"), "Grimy herbs within your Herblore level"))),
        new Tip("plank_make", "Cast Plank Make on your logs.", W + "Plank_Make", false,
            List.of(skill("Magic", 86), quest("Dream Mentor"), spellbook(Spellbook.LUNAR),
                new Gate(SpoonsTipCatalog::plankMakeMaterials, "One log, 2 astral, 1 nature, 15 earth runes and correct coins"))),
        new Tip("zaff_battlestaves", "Check whether Zaff has battlestaves available today.", W + "Zaff", false,
            List.of(new Gate(a -> a.diary("Varrock Easy"), "Varrock Easy Diary complete"),
                quantity(COINS, 7000, "7,000 coins for one staff"),
                // Diary completion is distinct from claiming the reward from Toby and from daily stock.
                // CP9 provides neither witnessed event, so this is disabled until those feeds exist.
                new Gate(a -> Check.UNKNOWN, "Toby reward claim and today's stock not observed"))),
        new Tip("cook_karambwan", "Cook your raw karambwan.", W + "Raw_karambwan", false,
            List.of(skill("Cooking", 30), quest("Tai Bwo Wannai Trio"), owns(RAW_KARAMBWAN, "Raw karambwan seen"))),
        new Tip("high_alch", "Consider High Alchemy for spare maple, yew or magic longbows in your bank.", W + "High_Level_Alchemy", false,
            List.of(skill("Magic", 55), spellbook(Spellbook.STANDARD),
                quantity(NATURE_RUNE, 1, "One nature rune per cast"),
                quantity(FIRE_RUNE, 5, "Five fire runes per cast"),
                ownsAny(REVIEWED_ALCHABLES, "Sourced longbow available to alch"))),
        new Tip("use_bones", "Bury or offer the bones in your bank for Prayer XP.", W + "Bones", false,
            List.of(ownsAny(Set.of(BONES, BIG_BONES, DRAGON_BONES), "Bones seen"))),
        new Tip("check_farms", "Have you checked your farming patches?", W + "Farming", true, List.of()));

    private SpoonsTipCatalog() {}
    public static List<Tip> tips() { return TIPS; }
    public static Optional<Tip> find(String id) { return TIPS.stream().filter(t -> t.id().equals(id)).findFirst(); }

    private static Gate owns(int id, String reason) { return new Gate(a -> a.owns(id), reason); }
    /** UNKNOWN on absent/stale bank evidence, not a claim of zero stock. */
    private static Check quantityCheck(AccountEvidenceContext a, int id, int minimum) {
        Check seen = a == null ? Check.UNKNOWN : a.owns(id);
        if (seen != Check.MET) return seen;
        try { return a.bank().combined().getOrDefault(id, 0) >= minimum ? Check.MET : Check.UNMET; }
        catch (RuntimeException e) { return Check.UNKNOWN; }
    }
    private static Gate quantity(int id, int minimum, String reason) {
        return new Gate(a -> quantityCheck(a, id, minimum), reason);
    }
    private static Check plankMakeMaterials(AccountEvidenceContext a) {
        // Conservative: no inference of elemental-staff runes from unverified charges/equip state.
        for (int id : List.of(ASTRAL_RUNE, NATURE_RUNE, EARTH_RUNE)) {
            int need = id == ASTRAL_RUNE ? 2 : id == EARTH_RUNE ? 15 : 1;
            Check runes = quantityCheck(a, id, need);
            if (runes != Check.MET) return runes;
        }
        Check coins = a == null ? Check.UNKNOWN : a.owns(COINS);
        if (coins != Check.MET) return coins;
        boolean unknown = false;
        for (var e : PLANK_COSTS.entrySet()) {
            Check logs = a.owns(e.getKey());
            if (logs == Check.MET && quantityCheck(a, COINS, e.getValue()) == Check.MET) return Check.MET;
            if (logs == Check.UNKNOWN) unknown = true;
        }
        return unknown ? Check.UNKNOWN : Check.UNMET;
    }
    private static Gate ownsAny(Set<Integer> ids, String reason) {
        return new Gate(a -> {
            boolean unknown = false;
            for (int id : ids) {
                Check c = a.owns(id);
                if (c == Check.MET) return Check.MET;
                unknown |= c == Check.UNKNOWN;
            }
            return unknown ? Check.UNKNOWN : Check.UNMET;
        }, reason);
    }
    private static Gate skill(String name, int level) { return new Gate(a -> a.skill(name, level), level + " " + name); }
    private static Gate quest(String name) { return new Gate(a -> a.questFinished(name), name + " complete"); }
    private static Gate spellbook(Spellbook book) { return new Gate(a -> a.onSpellbook(book), "On the " + book.name().toLowerCase(Locale.ROOT) + " spellbook"); }
    private static Check anyWithLevel(AccountEvidenceContext a, Map<Integer,Integer> items, String skill) {
        boolean unknown = false;
        for (var e : items.entrySet()) {
            if (a.owns(e.getKey()) == Check.UNKNOWN || a.skill(skill, e.getValue()) == Check.UNKNOWN) { unknown = true; continue; }
            if (a.owns(e.getKey()) == Check.MET && a.skill(skill, e.getValue()) == Check.MET) return Check.MET;
        }
        return unknown ? Check.UNKNOWN : Check.UNMET;
    }
}
