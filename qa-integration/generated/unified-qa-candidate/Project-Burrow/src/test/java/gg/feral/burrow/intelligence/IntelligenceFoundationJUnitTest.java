package gg.feral.burrow.intelligence;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Development contract checks only; not independent QA or live RuneLite proof. */
class IntelligenceFoundationJUnitTest {
    private static final IntelligenceCatalog.Source SOURCE =
        new IntelligenceCatalog.Source("https://oldschool.runescape.wiki/w/Test", "oldid=12345",
            "2026-11-03T03:00:00Z", "section/requirement");
    private static IntelligenceCatalog.Fact f(IntelligenceCatalog.Role role, IntelligenceCatalog.Review review) {
        return new IntelligenceCatalog.Fact("CASE-001", IntelligenceCatalog.Domain.GEAR,
            "Example activity", "Example gear", "Example mode only", role, review,
            review == IntelligenceCatalog.Review.VERIFIED_SOURCE ? SOURCE : null);
    }
    @Test void bundledPackIsEmptyUntilEvidenceCleared() throws Exception {
        var pack = IntelligenceCatalog.bundled();
        assertEquals(1, pack.schemaVersion());
        assertTrue(pack.facts().isEmpty());
    }
    @Test void unverifiedDataIsNotUsedForDecisions() {
        assertEquals(IntelligenceDecisions.Outcome.RESEARCH_ONLY,
            IntelligenceDecisions.evaluate(f(IntelligenceCatalog.Role.REQUIRED, IntelligenceCatalog.Review.STAGING),
                IntelligenceDecisions.Evidence.OBSERVED_USABLE));
    }
    @Test void unknownStockNeverBecomesMissing() {
        assertEquals(IntelligenceDecisions.Outcome.VERIFY,
            IntelligenceDecisions.evaluate(f(IntelligenceCatalog.Role.REQUIRED, IntelligenceCatalog.Review.VERIFIED_SOURCE),
                IntelligenceDecisions.Evidence.UNOBSERVED));
    }
    @Test void requiredMissingBlocks() {
        assertEquals(IntelligenceDecisions.Outcome.BLOCKED,
            IntelligenceDecisions.evaluate(f(IntelligenceCatalog.Role.REQUIRED, IntelligenceCatalog.Review.VERIFIED_SOURCE),
                IntelligenceDecisions.Evidence.OBSERVED_MISSING));
    }
    @Test void optionalMissingDoesNotBlock() {
        assertEquals(IntelligenceDecisions.Outcome.OPTIONAL_MISSING,
            IntelligenceDecisions.evaluate(f(IntelligenceCatalog.Role.OPTIONAL_UPGRADE, IntelligenceCatalog.Review.VERIFIED_SOURCE),
                IntelligenceDecisions.Evidence.OBSERVED_MISSING));
    }
    @Test void unusableOwnedRequiredStillBlocks() {
        assertEquals(IntelligenceDecisions.Outcome.BLOCKED,
            IntelligenceDecisions.evaluate(f(IntelligenceCatalog.Role.REQUIRED, IntelligenceCatalog.Review.VERIFIED_SOURCE),
                IntelligenceDecisions.Evidence.OBSERVED_UNUSABLE));
    }
    @Test void verifiedReadyNeedsObservedUsable() {
        assertEquals(IntelligenceDecisions.Outcome.READY,
            IntelligenceDecisions.evaluate(f(IntelligenceCatalog.Role.REQUIRED, IntelligenceCatalog.Review.VERIFIED_SOURCE),
                IntelligenceDecisions.Evidence.OBSERVED_USABLE));
    }
    @Test void noFakePinnedSource() {
        assertThrows(IllegalArgumentException.class, () -> new IntelligenceCatalog.Fact(
            "CASE-999", IntelligenceCatalog.Domain.FARMING, "Tree runs", "Travel method", "example",
            IntelligenceCatalog.Role.RECOMMENDED, IntelligenceCatalog.Review.VERIFIED_SOURCE,
            new IntelligenceCatalog.Source("https://oldschool.runescape.wiki/w/Farming", "", "2026-11-03T03:00:00Z", "")));
    }
    @Test void duplicateIdsRejected() {
        var one=f(IntelligenceCatalog.Role.REQUIRED, IntelligenceCatalog.Review.STAGING);
        assertThrows(IllegalArgumentException.class, () -> new IntelligenceCatalog.Snapshot(1,"sample",java.util.List.of(one,one)));
    }
    @Test void malformedPayloadRejected() {
        assertThrows(IllegalArgumentException.class, () -> IntelligenceCatalog.parse("{\"schema_version\":1,\"pack_id\":\"x\",\"facts\":[{}]}"));
    }
}
