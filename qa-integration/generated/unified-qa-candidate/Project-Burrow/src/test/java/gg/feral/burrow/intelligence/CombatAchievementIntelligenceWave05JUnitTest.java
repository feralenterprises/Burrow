package gg.feral.burrow.intelligence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Wave 05 fixtures kept for the owner's consolidated final QA, not an intermediate RuneLite QA run. */
class CombatAchievementIntelligenceWave05JUnitTest {
    private static IntelligenceCatalog.Source syntheticSource() {
        return new IntelligenceCatalog.Source("https://oldschool.runescape.wiki/w/Combat_Achievements",
            "oldid=123456", "2026-11-03T04:00:00Z", "SYNTHETIC TEST FIXTURE, not reviewed gameplay evidence");
    }
    private static CombatAchievementKnowledge.Task synthetic() {
        return new CombatAchievementKnowledge.Task("W05-SYNTHETIC-ANIMAL-WHISPERER",
            "Commander Zilyana","Synthetic private instance CA",CombatAchievementKnowledge.Tier.MASTER,
            "Restriction","Complete the synthetic test in a private instance", "", "Try accurate prayers",
            CombatAchievementKnowledge.Review.VERIFIED_SOURCE,syntheticSource(),
            List.of(new CombatAchievementKnowledge.Gate("INSTANCE-PRIVATE", CombatAchievementKnowledge.GateType.INSTANCE,
                "private-instance",0,false)));
    }

    @Test void bundledLegacyCAsNotPromotedAndNoPlaceholders() throws Exception {
        var pack=CombatAchievementKnowledge.bundled();
        assertEquals(290,pack.tasks().size());
        assertEquals(655,pack.referenceTotal());
        assertEquals(0,pack.approvedCount());
        assertEquals(0,pack.tasks().stream().filter(t->!t.canonicalCondition().isEmpty()).count());
        assertEquals(77,pack.tasks().stream().filter(t->!t.strategyTip().isEmpty()).count());
        assertNotNull(pack.matchLegacy("Barrows","Defence? What Defence?"));
        assertNull(pack.matchLegacy("Barrows","Easy Combat Task #1"));
    }
    @Test void unapprovedTaskNeverDeclaresReadyOrComplete() throws Exception {
        var task=CombatAchievementKnowledge.bundled().tasks().get(0);
        var result=CombatAchievementDecisions.evaluate(task, CombatAchievementDecisions.Observation.none(),true,
            Map.of(task.id(),"game:task-list"));
        assertEquals(CombatAchievementDecisions.Prep.RESEARCH_ONLY,result.preparation());
        assertEquals(CombatAchievementDecisions.Completion.UNKNOWN,result.completion());
    }
    @Test void gameTaskCompletionNotInferredFromReadiness() {
        var task=synthetic();
        var yes=new CombatAchievementDecisions.Observation(Map.of("INSTANCE-PRIVATE",CombatAchievementDecisions.Signal.YES));
        assertEquals(CombatAchievementDecisions.Prep.ELIGIBLE_TO_ATTEMPT,
            CombatAchievementDecisions.preparation(task,yes));
        assertEquals(CombatAchievementDecisions.Completion.UNKNOWN,
            CombatAchievementDecisions.completion(task,true,Map.of()));
        assertEquals(CombatAchievementDecisions.Completion.COMPLETE,
            CombatAchievementDecisions.completion(task,true,Map.of(task.id(),"game-confirmed")));
        assertEquals(CombatAchievementDecisions.Completion.UNKNOWN,
            CombatAchievementDecisions.completion(task,false,Map.of(task.id(),"game-confirmed")));
    }
    @Test void missingAndUnknownGateDoNotBecomeReady() {
        var task=synthetic();
        assertEquals(CombatAchievementDecisions.Prep.VERIFY,
            CombatAchievementDecisions.preparation(task,CombatAchievementDecisions.Observation.none()));
        assertEquals(CombatAchievementDecisions.Prep.BLOCKED,
            CombatAchievementDecisions.preparation(task,
                new CombatAchievementDecisions.Observation(Map.of("INSTANCE-PRIVATE",CombatAchievementDecisions.Signal.NO))));
    }
    @Test void rejectUnpinnedPromotionAndFakeCanonicalConditions() {
        var src=new IntelligenceCatalog.Source("https://oldschool.runescape.wiki/w/Combat_Achievements",
            "UNKNOWN", "2026-11-03T04:00:00Z", "unverified");
        assertThrows(IllegalArgumentException.class,()->new CombatAchievementKnowledge.Task("W05-TEST-SOURCE-FAIL",
            "Vorkath","The Walk", CombatAchievementKnowledge.Tier.ELITE,"Restriction",
            "Do the exact task", "","",CombatAchievementKnowledge.Review.VERIFIED_SOURCE,src,List.of()));
        assertThrows(IllegalArgumentException.class,()->new CombatAchievementKnowledge.Task("W05-TEST-PLACEHOLDER",
            "Various","Easy Combat Task #1",CombatAchievementKnowledge.Tier.EASY,"Kill Count",
            "Canonical game-checked condition for Easy Combat Task #1.","","",
            CombatAchievementKnowledge.Review.VERIFIED_SOURCE,syntheticSource(),List.of()));
    }
}
