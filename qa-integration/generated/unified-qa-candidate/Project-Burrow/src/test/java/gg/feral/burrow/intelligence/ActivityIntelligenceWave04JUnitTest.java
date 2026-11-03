package gg.feral.burrow.intelligence;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Wave 04 fixtures: intentionally staged for owner's single final six-wave QA. */
class ActivityIntelligenceWave04JUnitTest {
    @Test void bundledResearchRemainsQuarantined() throws Exception {
        ActivityKnowledge.Pack pack = ActivityKnowledge.bundled();
        assertEquals(44, pack.entries().size());
        assertEquals(0, pack.approvedCount());
        assertEquals(7, pack.patches(ActivityKnowledge.PatchType.REGULAR_TREE).size());
        assertEquals(7, pack.patches(ActivityKnowledge.PatchType.FRUIT_TREE).size());
        for (ActivityKnowledge.Entry e : pack.entries())
            assertEquals(ActivityDecisions.Outcome.RESEARCH_ONLY,
                ActivityDecisions.evaluate(e,ActivityDecisions.AccountEvidence.unknown()).outcome());
    }

    @Test void rejectSourcePromotionWithoutImmutableRevision() throws Exception {
        ActivityKnowledge.Entry e=ActivityKnowledge.bundled().find("W04-ACT-SEPULCHRE-F4");
        assertThrows(IllegalArgumentException.class, () -> new ActivityKnowledge.Entry(
            e.id(),e.kind(),e.domain(),e.name(),e.context(),e.patchType(),e.wilderness(),
            IntelligenceCatalog.Review.VERIFIED_SOURCE,e.source(),e.requirements(),e.supplies(),e.travel()));
    }

    @Test void wildernessClueNeverAuthorizesBestInBank() throws Exception {
        var pack=ActivityKnowledge.bundled();
        assertFalse(ActivityDecisions.canUseBestInBank(pack.find("W04-CLUE-WILDERNESS")));
        assertFalse(ActivityDecisions.canUseBestInBank(pack.find("W04-CLUE-GENERAL")));
    }

    @Test void reviewedSyntheticRequirementsStayConditional() {
        var source = new IntelligenceCatalog.Source("https://oldschool.runescape.wiki/w/SYNTHETIC-TEST",
            "oldid=12345","2026-11-03T00:00:00Z","synthetic-fixture-only");
        var gate = new ActivityKnowledge.Requirement("SKILL-AGILITY",
            ActivityKnowledge.RequirementType.SKILL_EFFECTIVE,"Agility",77,true,
            ActivityKnowledge.Role.REQUIRED,"");
        var optional = new ActivityKnowledge.Requirement("OPTIONAL-BOOTS",
            ActivityKnowledge.RequirementType.UNLOCK,"PegasianBoots",0,false,
            ActivityKnowledge.Role.OPTIONAL_UPGRADE,"");
        var entry = new ActivityKnowledge.Entry("W04-TEST-ACTIVITY",ActivityKnowledge.Kind.ACTIVITY,
            IntelligenceCatalog.Domain.SKILLING,"Synthetic activity","Test only",
            ActivityKnowledge.PatchType.NONE,false,IntelligenceCatalog.Review.VERIFIED_SOURCE,
            source,List.of(gate,optional),List.of(),null);
        var missing = new ActivityDecisions.AccountEvidence(Map.of(),Map.of("Agility",76),null,
            Map.of("UNLOCK:PegasianBoots",false),Map.of(),false,Map.of(),Map.of());
        assertEquals(ActivityDecisions.Outcome.BLOCKED,ActivityDecisions.evaluate(entry,missing).outcome());
        var boosted = new ActivityDecisions.AccountEvidence(Map.of("Agility",72),Map.of("Agility",77),null,
            Map.of("UNLOCK:PegasianBoots",false),Map.of(),false,Map.of(),Map.of());
        var result=ActivityDecisions.evaluate(entry,boosted);
        assertEquals(ActivityDecisions.Outcome.READY,result.outcome());
        assertFalse(result.optionalMissing().isEmpty());
    }
}
