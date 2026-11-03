package gg.feral.burrow.experience;

import gg.feral.burrow.pathfinder.SuggestionEngine;
import gg.feral.burrow.pathfinder.SuggestionEngine.*;
import java.util.Optional;

/** One goal-aware nudge from the existing Pathfinder rules. It reads goals and has no way to change them. */
public final class NoodlesAdvisor {
    private NoodlesAdvisor() {}

    public static NoodlesOpportunity opportunity(AccountEvidenceContext account) {
        if (account == null || !account.identified()) return NoodlesOpportunity.none(null, EvidenceStatus.UNAVAILABLE, "Sign in to see your next move");
        var snapshot = account.snapshot();
        if (snapshot.quests().isEmpty() || snapshot.skills().isEmpty()) return NoodlesOpportunity.none(null, EvidenceStatus.UNAVAILABLE, "Quest and skill data not available yet");
        String pinned = snapshot.goals().entrySet().stream().filter(e -> e.getValue() == GoalMark.SAVED).map(java.util.Map.Entry::getKey).findFirst().orElse(null);
        return pinned == null ? unpinned(snapshot) : pinned(snapshot, pinned);
    }

    private static NoodlesOpportunity pinned(Snapshot snapshot, String goal) {
        Optional<Suggestion> found = SuggestionEngine.evaluateAll(snapshot).stream().filter(s -> s.id().equals(goal)).findFirst();
        if (found.isEmpty()) return NoodlesOpportunity.none(goal, EvidenceStatus.UNKNOWN, "No next step can be confirmed for your goal");
        var s = found.get();
        if (s.readiness() == State.UNKNOWN) return NoodlesOpportunity.none(goal, EvidenceStatus.UNKNOWN, s.requirement());
        return new NoodlesOpportunity(goal, s.title(), s.nextStep(), s.readiness() == State.MET ? "You meet the observed requirements for your goal." : s.requirement(),
            EvidenceStatus.OBSERVED, new DestinationIntent(DestinationIntent.Kind.OPEN_PATHFINDER, goal));
    }

    private static NoodlesOpportunity unpinned(Snapshot snapshot) {
        for (var s : SuggestionEngine.suggest(snapshot, new Preferences(Focus.ANY, Attention.ACTIVE, 60)))
            if (s.readiness() == State.MET && !s.optional())
                return new NoodlesOpportunity(null, s.title(), s.nextStep(), s.why(), EvidenceStatus.INFERRED_WITH_LIMITS,
                    new DestinationIntent(DestinationIntent.Kind.OPEN_PATHFINDER, s.id()));
        return NoodlesOpportunity.none(null, EvidenceStatus.UNKNOWN, "No confirmed opportunity right now");
    }
}
