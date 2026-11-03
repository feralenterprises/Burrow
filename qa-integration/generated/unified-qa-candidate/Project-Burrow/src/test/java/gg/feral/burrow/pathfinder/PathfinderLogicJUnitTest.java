package gg.feral.burrow.pathfinder;

import gg.feral.burrow.pathfinder.SuggestionEngine.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Regressions from owner tester reports: filters must find real matches, and order must stay honest. */
public final class PathfinderLogicJUnitTest {
    static Snapshot account(int level) {
        Map<String,Integer> skills = new HashMap<>();
        for (String s : List.of("Attack","Strength","Defence","Ranged","Magic","Hitpoints","Prayer","Slayer","Agility","Herblore","Mining","Smithing","Crafting","Fletching","Thieving","Firemaking","Woodcutting","Fishing","Cooking","Farming","Runecraft","Hunter","Construction")) skills.put(s, level);
        Map<String,Quest> quests = new HashMap<>();
        for (var e : GuideQuestOrder.entries()) quests.put(e.name(), Quest.NOT_STARTED);
        for (var e : GuideData.questPrerequisites().entrySet()) { quests.putIfAbsent(e.getKey(), Quest.NOT_STARTED); for (String q : e.getValue()) quests.putIfAbsent(q, Quest.NOT_STARTED); }
        return new Snapshot(true, skills, quests, BankStatus.VERIFIED_SAME_ACCOUNT, Set.of());
    }
    @Test void pvmLowFindsCombatAfkMethods() {
        for (int level : List.of(30, 70, 99)) {
            var rows = SuggestionEngine.suggest(account(level), new Preferences(Focus.PVM, Attention.LOW, 120));
            assertFalse(rows.isEmpty(), "PvM Low must not be empty at level " + level);
            assertTrue(rows.stream().allMatch(r -> r.attention() == Attention.LOW));
        }
    }
    @Test void pvmFilterStaysPvmOnly() {
        for (var r : SuggestionEngine.suggest(account(99), new Preferences(Focus.PVM, Attention.ACTIVE, 120)))
            assertTrue(r.focus() == Focus.PVM || SuggestionEngine.COMBAT_AFK.contains(r.id()), r.id());
    }
    @Test void everyCombatAfkIdIsARealRule() {
        Set<String> ids = new HashSet<>();
        for (GuideRule r : GuideData.rules()) ids.add(r.id());
        assertTrue(ids.containsAll(SuggestionEngine.COMBAT_AFK));
    }
    @Test void nothingStartableIsListedAfterSomethingUnstartable() {
        var rows = SuggestionEngine.suggest(account(60), new Preferences(Focus.ANY, Attention.ACTIVE, 120));
        boolean sawUnmet = false;
        for (var r : rows) {
            if (r.readiness() == State.UNMET && r.score() < 300 && r.goal() != GoalMark.SAVED) sawUnmet = true;
            else if (r.readiness() == State.MET) assertFalse(sawUnmet, "ready item buried: " + r.title());
        }
        assertTrue(sawUnmet, "fixture must contain unstartable rows");
    }
    @Test void nextStepWalksToTheFirstUnfinishedPrerequisite() {
        var deep = GuideData.questPrerequisites().entrySet().stream()
            .filter(e -> !e.getValue().isEmpty()).findFirst().orElseThrow();
        var snapshot = account(99);
        boolean checked = false;
        for (var r : SuggestionEngine.evaluateAll(snapshot)) {
            if (r.nextStep().contains("(needed for ")) {
                String first = r.nextStep().substring("Next: complete ".length(), r.nextStep().indexOf(" (needed"));
                for (String q : GuideData.questPrerequisites().getOrDefault(first, List.of()))
                    assertNotEquals(Quest.NOT_STARTED, snapshot.quests().getOrDefault(q, Quest.UNKNOWN), first + " still needs " + q);
                checked = true;
            }
        }
        assertTrue(checked, "fixture must exercise a prerequisite chain; first entry " + deep.getKey());
    }
    @Test void unknownPrerequisiteIsNotClaimedUnfinished() {
        var base = account(99);
        Map<String,Quest> quests = new HashMap<>(base.quests());
        for (var e : GuideData.questPrerequisites().entrySet()) for (String q : e.getValue()) quests.put(q, Quest.UNKNOWN);
        var snapshot = new Snapshot(true, base.skills(), quests, BankStatus.VERIFIED_SAME_ACCOUNT, Set.of());
        for (var r : SuggestionEngine.evaluateAll(snapshot))
            for (var u : quests.entrySet()) if (u.getValue() == Quest.UNKNOWN && r.nextStep().contains("Next: complete " + u.getKey() + " (needed"))
                fail("claimed unfinished: " + u.getKey());
    }
}
