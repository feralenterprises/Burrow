package gg.feral.burrow.experience;

import java.util.List;
import java.util.Optional;

/** Everything Home needs for one account. Each part carries its own evidence status. */
public record HomeFeed(long generation, WeeklyXpSummary weeklyXp, ProgressProjections.WinResult biggestWin,
                       Optional<SpoonsTip> spoons, EvidenceStatus spoonsStatus, SpoonsTipService.State spoonsState,
                       List<BanditBossCandidate> banditHunts, NoodlesOpportunity noodles) {
    public HomeFeed { banditHunts = List.copyOf(banditHunts); }
}
