package gg.feral.burrow.pathfinder;

import java.util.*;

/**
 * CP-2 (BQA-R12-005, PATH-X01): how a suggestion relates to the player's TOTAL available time, kept separate
 * from the AFK look-away interval.
 *
 * <p>Burrow does not know where the player is standing or how long travel, boat departure, banking or set-up
 * takes. It therefore never invents a travel estimate. Instead each rule is either
 * <ul>
 *   <li>{@link Access#BANKSTANDING}: can be started at any bank with the inputs in hand (e.g. glassblowing,
 *       Plank Make, bolt tips), so its time-to-start is negligible and a short window is honest; or</li>
 *   <li>{@link Access#TRAVEL_UNKNOWN}: travel/set-up time is not modelled. In a short window it is not called
 *       ready and ranks below startable options, with the reason shown.</li>
 * </ul>
 * For bank-standing methods the inputs are checked against the last same-account bank read: positively observed
 * inputs mean "ready from bank", absent inputs mean "inputs not observed" (never "you have none").
 */
public final class PathfinderTiming {
    private PathfinderTiming() {}
    public enum Access { BANKSTANDING, TRAVEL_UNKNOWN }
    /** Start-time verdict for the chosen window, independent of requirement readiness. */
    public enum Start { STARTABLE_AT_BANK, UNKNOWN_FOR_WINDOW, NOT_TIME_BOUND }
    /** Windows at or below this many minutes cannot absorb unknown travel/set-up honestly. */
    public static final int SHORT_WINDOW_MINUTES = 30;

    /** inputs: every group must have at least one positively observed item. */
    record Inputs(List<Set<Integer>> groups, String label) {}

    private static final Set<Integer> CUT_GEMS = Set.of(1607, 1605, 1603, 1601, 1615, 6573, 1609, 1611, 1613);
    private static final Set<Integer> ORBS = Set.of(571, 575, 569, 573);
    private static final Set<Integer> PLANK_LOGS = Set.of(1511,1521,6333,6332,32904,32907,32910);
    private static final Map<String, Inputs> BANKSTANDING = Map.ofEntries(
        Map.entry("afk_glassblowing", new Inputs(List.of(Set.of(1775)), "molten glass")),
        Map.entry("afk_battlestaves", new Inputs(List.of(Set.of(1391), ORBS), "battlestaves and elemental orbs")),
        Map.entry("gp_battlestaff_crafting", new Inputs(List.of(Set.of(1391), ORBS), "battlestaves and elemental orbs")),
        Map.entry("afk_enchant_jewellery", new Inputs(List.of(Set.of(564)), "cosmic runes")),
        Map.entry("afk_plank_make", new Inputs(List.of(Set.of(9075), Set.of(561), PLANK_LOGS), "astral and nature runes plus logs")),
        Map.entry("afk_bolt_tips", new Inputs(List.of(CUT_GEMS), "cut gems")),
        Map.entry("afk_string_jewellery", new Inputs(List.of(Set.of(9075)), "astral runes")),
        Map.entry("afk_cleaning_herbs", new Inputs(List.of(), "grimy herbs")),
        Map.entry("afk_stackable_secondaries", new Inputs(List.of(), "potion secondaries")),
        Map.entry("afk_herb_tar", new Inputs(List.of(), "herbs and swamp tar")),
        Map.entry("gp_dhide_crafting", new Inputs(List.of(), "tanned dragon leather and thread")),
        Map.entry("afk_splashing", new Inputs(List.of(), "runes for the chosen spell")));

    public static Access access(GuideRule r) {
        return r != null && BANKSTANDING.containsKey(r.id()) ? Access.BANKSTANDING : Access.TRAVEL_UNKNOWN;
    }
    /** Categories whose value is producing something during the window (quests are progression, not output). */
    static boolean timeSensitive(GuideRule.Category c) {
        return c == GuideRule.Category.AFK || c == GuideRule.Category.MONEY || c == GuideRule.Category.SKILL
            || c == GuideRule.Category.BOSS || c == GuideRule.Category.LEGACY;
    }

    public enum InputState { OBSERVED, NOT_OBSERVED, NOT_CHECKABLE, BANK_UNKNOWN }
    public record InputCheck(InputState state, String label) {}

    public static InputCheck inputs(GuideRule r, SuggestionEngine.Snapshot s) {
        Inputs in = r == null ? null : BANKSTANDING.get(r.id());
        if (in == null) return new InputCheck(InputState.NOT_CHECKABLE, "");
        // W05 safety audit: one reagent is not a complete jewellery spell recipe.
        // String Jewellery also needs an unstrung item, 2 astral, 5 water and 10 earth;
        // enchantments vary by spell and target. Until enumerated, never assert ready.
        if(r.id().equals("afk_string_jewellery") || r.id().equals("afk_enchant_jewellery"))
            return new InputCheck(InputState.NOT_CHECKABLE,
                "Unverified full jewellery item/spell/rune recipe (not just astral/cosmic runes)");
        if (in.groups().isEmpty()) return new InputCheck(InputState.NOT_CHECKABLE, in.label());
        if (s.bankStatus() != SuggestionEngine.BankStatus.VERIFIED_SAME_ACCOUNT) return new InputCheck(InputState.BANK_UNKNOWN, in.label());
        SuggestionEngine.ResourceEvidence res=s.resources();
        if (res==null || !res.reliable())return new InputCheck(InputState.BANK_UNKNOWN, in.label());
        for (Set<Integer> group : in.groups()) {
            boolean positive=false, unknown=false;
            for(int id:group) {
                Integer count=res.quantity(id);
                if(count==null)unknown=true;
                else if(count>0)positive=true;
            }
            if(!positive) {
                if(unknown)return new InputCheck(InputState.BANK_UNKNOWN, in.label());
                return new InputCheck(InputState.NOT_OBSERVED, in.label());
            }
        }
        return new InputCheck(InputState.OBSERVED, in.label());
    }
}
