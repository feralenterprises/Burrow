package gg.feral.burrow.experience;

import gg.feral.burrow.core.BankEvidence;
import gg.feral.burrow.core.BestBankPreloader;
import gg.feral.burrow.core.BestBankPreloader.Key;
import gg.feral.burrow.experience.BanditAssessor.PlanTicket;
import gg.feral.burrow.experience.BanditAssessor.ScopedPlan;
import gg.feral.burrow.experience.BanditAssessor.ScopedPlanSource;

/**
 * W03 read-only adapter to CP9's prepared planner cache. No computation is launched,
 * no RuneLite API is accessed, and no plan without the same observed bank object and
 * generation is returned. Caller MUST bind this adapter to a verified host session.
 */
public final class ScopedPreparedPlanSource {
    private ScopedPreparedPlanSource() {}
    public static ScopedPlanSource bind(BestBankPreloader preloader, AccountEvidenceContext account) {
        if (preloader == null || account == null) return (boss,method,tier,ticket) -> null;
        var expected = BanditAssessor.ticket(account);
        BankEvidence source = preloader.evidence();
        long generation = preloader.generation();
        if (expected.isEmpty() || source == null || source != account.bank())
            return (boss,method,tier,ticket) -> null;
        PlanTicket bound = expected.orElseThrow();
        return (boss,method,tier,ticket) -> {
            if (ticket == null || !ticket.equals(bound) || preloader.generation() != generation
                || preloader.evidence() != source) return null;
            var cached = preloader.cached(new Key(boss,method,tier));
            if (cached.isEmpty() || cached.get().generation() != generation || cached.get().plan() == null) return null;
            // A generation can change during an asynchronous lookup. Recheck after reading.
            if (preloader.generation() != generation || preloader.evidence() != source) return null;
            return new ScopedPlan(bound,cached.get().plan());
        };
    }
}
