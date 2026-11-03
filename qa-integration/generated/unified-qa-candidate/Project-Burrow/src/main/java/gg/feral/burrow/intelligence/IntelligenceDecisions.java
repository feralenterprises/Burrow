package gg.feral.burrow.intelligence;

import java.util.Objects;

/** W01: pure decision semantics; no bank reads, no plan changes or player actions. */
public final class IntelligenceDecisions {
    private IntelligenceDecisions() {}

    public enum Evidence { OBSERVED_USABLE, OBSERVED_UNUSABLE, OBSERVED_MISSING, UNOBSERVED }
    public enum Outcome { READY, BLOCKED, OPTIONAL_MISSING, VERIFY, RESEARCH_ONLY }

    /**
     * Only a pinned, reviewed requirement can authorize readiness.
     * An absent/unobserved bank read always remains unknown, never zero stock.
     * OPTIONAL_UPGRADE and RECOMMENDED items never block trip readiness on missing stock.
     */
    public static Outcome evaluate(IntelligenceCatalog.Fact fact, Evidence evidence) {
        Objects.requireNonNull(fact);
        Objects.requireNonNull(evidence);
        if (!fact.eligibleForEvaluation()) return Outcome.RESEARCH_ONLY;
        if (evidence == Evidence.UNOBSERVED) return Outcome.VERIFY;
        if (evidence == Evidence.OBSERVED_USABLE) return Outcome.READY;
        if (fact.role() == IntelligenceCatalog.Role.REQUIRED) return Outcome.BLOCKED;
        return Outcome.OPTIONAL_MISSING;
    }
}
