package gg.feral.burrow.pathfinder;

import gg.feral.burrow.pathfinder.SuggestionEngine.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** CP-2 Pathfinder acceptance fixtures (PATH-X01, T16-T18). Synthetic snapshots; not live RuneLite state. */
public final class Cp2PathfinderJUnitTest {
    private static Optional<Suggestion> find(List<Suggestion> list, String id) {
        return list.stream().filter(x -> x.id().equals(id)).findFirst();
    }
    private static int rank(List<Suggestion> list, String id) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).id().equals(id)) return i;
        return Integer.MAX_VALUE;
    }

    /** Trusted same-account bank quantities for PathfinderTiming input checks. */
    private static ResourceEvidence bankQty(int itemId, int qty) {
        return new ResourceEvidence(true, true, true, Map.of(itemId, qty), null, null, null);
    }
    private static ResourceEvidence emptyBank() {
        return new ResourceEvidence(true, true, true, Map.of(), null, null, null);
    }

    /** PATH-X01 / T16: 15 minutes, AFK; at-sea sharks have unknown travel; bank-ready glassblowing ranks ahead. */
    @Test void fifteenMinutesNeverCallsAtSeaFishingReadyAndPrefersBankReadyWork() {
        var s = new Snapshot(true, Map.of("Fishing", 80, "Crafting", 60), Map.of(), BankStatus.VERIFIED_SAME_ACCOUNT,
            Set.of(1775), Mode.IRONMAN, Map.of(), Map.of(), bankQty(1775, 27));
        var list = SuggestionEngine.suggest(s, new Preferences(Focus.ANY, Attention.LOW, 15));
        var sharks = find(list, "afk_sharks_sea_fast");
        var glass = find(list, "afk_glassblowing").orElseThrow(() -> new AssertionError("glassblowing missing " + list));
        assertTrue(glass.readyNow(), "bank-standing with observed molten glass is startable");
        assertEquals(PathfinderTiming.Start.STARTABLE_AT_BANK, glass.start());
        assertTrue(glass.why().contains("molten glass") &&
            (glass.why().contains("verified in fresh same-account") || glass.why().contains("seen in your last bank read")),
            glass.why());
        assertTrue(sharks.isPresent(), "fixture: sharks at sea pass the 15-minute LOW filter " + list);
        {
            assertFalse(sharks.get().readyNow(), "travel unknown: not ready for 15 minutes");
            assertEquals(State.MET, sharks.get().readiness(), "requirement readiness itself is unchanged");
            assertTrue(sharks.get().why().contains("may not fit 15 minutes"), sharks.get().why());
            assertTrue(rank(list, "afk_glassblowing") < rank(list, "afk_sharks_sea_fast"), "local work ranks first");
        }
        // A longer window does not penalise travel.
        var long60 = SuggestionEngine.suggest(s, new Preferences(Focus.ANY, Attention.LOW, 60));
        assertNotEquals(PathfinderTiming.Start.UNKNOWN_FOR_WINDOW, find(long60, "afk_sharks_sea_fast").orElseThrow().start());
    }

    @Test void bankStandingInputsAbsentAreReportedNotInvented() {
        var s = new Snapshot(true, Map.of("Crafting", 60), Map.of(), BankStatus.VERIFIED_SAME_ACCOUNT,
            Set.of(), Mode.IRONMAN, Map.of(), Map.of(), emptyBank());
        var glass = find(SuggestionEngine.suggest(s, new Preferences(Focus.ANY, Attention.LOW, 15)), "afk_glassblowing").orElseThrow();
        assertTrue(glass.why().contains("absent in verified same-account stock") ||
            glass.why().contains("not seen in your last bank read"), glass.why());
        var unknownBank = new Snapshot(true, Map.of("Crafting", 60), Map.of(), BankStatus.UNAVAILABLE, Set.of(), Mode.IRONMAN, Map.of(), Map.of());
        var g2 = find(SuggestionEngine.suggest(unknownBank, new Preferences(Focus.ANY, Attention.LOW, 15)), "afk_glassblowing").orElseThrow();
        assertTrue(g2.why().contains("Quantity/completeness/freshness unknown") ||
            g2.why().contains("Open your bank to check inputs"), g2.why());
    }

    /** T17: AFK length and a higher requirement are no longer score rewards on their own. */
    @Test void stranglewoodGetsNoAfkOrSkillLevelBonus() {
        var s = new Snapshot(true, Map.of("Fishing", 99, "Woodcutting", 60), Map.of(), BankStatus.UNAVAILABLE, Set.of(),
            Mode.IRONMAN, Map.of(), Map.of());
        var all = SuggestionEngine.evaluateAll(s);
        var strangle = find(all, "afk_stranglewood_fishing").orElseThrow();
        var fly = find(all, "afk_fly_fishing").orElseThrow();
        // 30:00 look-away vs 1:57 used to be worth +80 more points, and Fishing 63 vs 1 another +42.
        assertTrue(strangle.score() - fly.score() <= 0, "no blanket AFK/level reward: " + strangle.score() + " vs " + fly.score());
    }

    @Test void inProgressQuestAndSavedGoalOutrankStranglewood() {
        var goals = Map.of("afk_infected_root", GoalMark.SAVED);
        var s = new Snapshot(true, Map.of("Fishing", 70, "Woodcutting", 80), Map.of("Cook's Assistant", Quest.IN_PROGRESS),
            BankStatus.UNAVAILABLE, Set.of(), Mode.IRONMAN, Map.of(), goals);
        var list = SuggestionEngine.suggest(s, new Preferences(Focus.ANY, Attention.ACTIVE, 120));
        int strangle = rank(list, "afk_stranglewood_fishing");
        assertTrue(rank(list, "quest_cooks_assistant") < strangle, "in-progress quest first");
        assertTrue(rank(list, "afk_infected_root") < strangle, "saved goal first");
    }
}
