package gg.feral.burrow.experience;

import gg.feral.burrow.core.SessionHistoryService;
import gg.feral.burrow.core.WeeklyProgress;
import java.time.LocalDate;
import java.util.Optional;

/** Composes Home. It does no I/O of its own and persists nothing. A host holds the returned Spoons state. */
public final class HomeFeedProvider {
    public static final int BANDIT_ROWS = 3;
    private HomeFeedProvider() {}

    /** Host-safe read model: uses ONLY scoped, already-prepared plans. Missing cache entries stay UNKNOWN. */
    public static HomeFeed buildScoped(AccountEvidenceContext account, SessionHistoryService history, LocalDate week,
                                       String liveLoginId, WeeklyProgress.Contribution live,
                                       SpoonsTipService.State spoons, ProgressProjections.Pin pin,
                                       BanditAssessor.ScopedPlanSource plans) {
        var a = account == null ? AccountEvidenceContext.signedOut() : account;
        var key = a.identified() ? a.accountKey() : null;
        var tip = SpoonsTipService.select(a, spoons);
        var hunts = a.identified() && plans != null ? BanditAssessor.withScopedPlans(a, plans).hunts(BANDIT_ROWS)
            : java.util.List.<BanditBossCandidate>of();
        return new HomeFeed(a.generation(), ProgressProjections.weeklyXp(history, key, week, liveLoginId, live),
            ProgressProjections.biggestWin(history, key, pin), tip.tip(), tip.status(), tip.state(),
            hunts, NoodlesAdvisor.opportunity(a));
    }

    public static HomeFeed build(AccountEvidenceContext account, SessionHistoryService history, LocalDate week, String liveLoginId,
                                 WeeklyProgress.Contribution live, SpoonsTipService.State spoons, ProgressProjections.Pin pin,
                                 BanditAssessor.PlanSource plans) {
        var a = account == null ? AccountEvidenceContext.signedOut() : account;
        var key = a.identified() ? a.accountKey() : null;
        var tip = SpoonsTipService.select(a, spoons);
        var bandit = plans == null ? BanditAssessor.withPlanner(a) : new BanditAssessor(a, plans);
        return new HomeFeed(a.generation(), ProgressProjections.weeklyXp(history, key, week, liveLoginId, live),
            ProgressProjections.biggestWin(history, key, pin), tip.tip(), tip.status(), tip.state(),
            a.identified() ? bandit.hunts(BANDIT_ROWS) : java.util.List.of(), NoodlesAdvisor.opportunity(a));
    }
}
