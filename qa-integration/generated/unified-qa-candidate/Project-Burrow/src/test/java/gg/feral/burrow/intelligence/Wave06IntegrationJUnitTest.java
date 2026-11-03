package gg.feral.burrow.intelligence;

import gg.feral.burrow.core.CombatTaskGuide;
import gg.feral.burrow.pathfinder.GuideRule;
import gg.feral.burrow.pathfinder.SuggestionEngine;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** W06 integration fixtures for the owner's single combined QA cycle. */
public final class Wave06IntegrationJUnitTest {
    private static final IntelligenceCatalog.Source SOURCE = new IntelligenceCatalog.Source(
        "https://oldschool.runescape.wiki/w/Synthetic_test_only","oldid=123456",
        "2026-11-03T00:00:00Z","test fixture only; not real game evidence");

    @Test void noResearchIsSilentlyPromotedIntoLiveDecisions() throws Exception {
        assertEquals(0,ChronicleIntelligenceBridge.liveApprovedCount());
        assertEquals(0,ActivityKnowledge.bundled().approvedCount());
        assertEquals(0,CombatAchievementKnowledge.bundled().approvedCount());
        assertEquals(290,CombatAchievementKnowledge.bundled().tasks().size());
        assertEquals(44,ActivityKnowledge.bundled().entries().size());
        var task=CombatTaskGuide.forBoss("Vorkath").get(0);
        var view=ChronicleIntelligenceBridge.display(task);
        assertFalse(view.reviewed());
        assertEquals(task.requirement(),view.condition());
        assertEquals(task.tip(),view.tip());
    }

    @Test void approvedConditionMayReplaceExactLegacyRowButNotUnapproved() {
        CombatTaskGuide.Task legacy=CombatTaskGuide.forBoss("Vorkath").get(0);
        var reviewed = new CombatAchievementKnowledge.Task("W06-TEST-CA01",legacy.boss(),legacy.name(),
            CombatAchievementKnowledge.Tier.valueOf(legacy.tier().toUpperCase()),legacy.type(),
            "Defeat the synthetic practice dummy without taking damage.",legacy.requirement(),
            "Synthetic optional strategy",CombatAchievementKnowledge.Review.VERIFIED_SOURCE,
            SOURCE,List.of());
        var pack=new CombatAchievementKnowledge.Pack(5,"W06-TEST-PACK",655,"AGGREGATE_ONLY",List.of(reviewed));
        var approved=ChronicleIntelligenceBridge.approvedIndex(pack);
        var view=ChronicleIntelligenceBridge.display(legacy,approved);
        assertTrue(view.reviewed());
        assertEquals(reviewed.canonicalCondition(),view.condition());
        assertEquals(reviewed.strategyTip(),view.tip());
        assertFalse(ChronicleIntelligenceBridge.display(legacy,Map.of()).reviewed());
        var badTier=new CombatTaskGuide.Task(legacy.boss(),"MEDIUM",legacy.type(),legacy.name(),
            legacy.requirement(),legacy.tip(),legacy.source());
        assertFalse(ChronicleIntelligenceBridge.display(badTier,approved).reviewed());
    }

    @Test void approvedActivityMayDowngradeButCannotPromoteOrGuessBankAbsence() throws Exception {
        var realGate=new ActivityKnowledge.Requirement("W06-SKILL",
            ActivityKnowledge.RequirementType.SKILL_REAL,"Agility",87,false,
            ActivityKnowledge.Role.REQUIRED,"");
        var questGate=new ActivityKnowledge.Requirement("W06-QUEST",
            ActivityKnowledge.RequirementType.QUEST,"Sins of the Father",0,false,
            ActivityKnowledge.Role.REQUIRED,"");
        var entry=new ActivityKnowledge.Entry("W06-SYNTHETIC-SEPULCHRE",ActivityKnowledge.Kind.ACTIVITY,
            IntelligenceCatalog.Domain.SKILLING,"Hallowed Sepulchre","synthetic test",
            ActivityKnowledge.PatchType.NONE,false,IntelligenceCatalog.Review.VERIFIED_SOURCE,SOURCE,
            List.of(realGate,questGate),List.of(),null);
        var pack=new ActivityKnowledge.Pack(4,"W06-TEST-PACK",List.of(entry));
        var suggestion=new SuggestionEngine.Suggestion("test","Hallowed Sepulchre","Guide text",
            SuggestionEngine.State.MET,"Req","Existing source",SuggestionEngine.Focus.ANY,
            SuggestionEngine.Attention.ACTIVE,30,GuideRule.Category.SKILL,"Skilling",
            List.of(),"Next",null,25,null,false,List.of(),0);
        var low=new SuggestionEngine.Snapshot(true,Map.of("Agility",80),
            Map.of("Sins of the Father",SuggestionEngine.Quest.FINISHED),
            SuggestionEngine.BankStatus.UNAVAILABLE,Set.of());
        var altered=PathfinderIntelligenceBridge.enrich(low,List.of(suggestion),pack);
        assertEquals(SuggestionEngine.State.UNMET,altered.get(0).readiness());
        var unknown=new SuggestionEngine.Snapshot(true,Map.of("Agility",88),
            Map.of("Sins of the Father",SuggestionEngine.Quest.UNKNOWN),
            SuggestionEngine.BankStatus.UNAVAILABLE,Set.of());
        assertEquals(SuggestionEngine.State.UNKNOWN,PathfinderIntelligenceBridge.enrich(
            unknown,List.of(suggestion),pack).get(0).readiness());
        var ready=new SuggestionEngine.Snapshot(true,Map.of("Agility",88),
            Map.of("Sins of the Father",SuggestionEngine.Quest.FINISHED),
            SuggestionEngine.BankStatus.UNAVAILABLE,Set.of());
        var notYetReady=new SuggestionEngine.Suggestion("test","Hallowed Sepulchre","Original",
            SuggestionEngine.State.UNKNOWN,"Req","Existing",SuggestionEngine.Focus.ANY,
            SuggestionEngine.Attention.ACTIVE,30,GuideRule.Category.SKILL,"Skilling",
            List.of(),"Next",null,25,null,false,List.of(),0);
        assertSame(notYetReady,PathfinderIntelligenceBridge.enrich(ready,List.of(notYetReady),pack).get(0),
            "source-approved gates cannot promote unknown suggestion readiness");
        assertSame(suggestion,PathfinderIntelligenceBridge.enrich(ready,List.of(suggestion),
            ActivityKnowledge.bundled()).get(0));
    }
}
