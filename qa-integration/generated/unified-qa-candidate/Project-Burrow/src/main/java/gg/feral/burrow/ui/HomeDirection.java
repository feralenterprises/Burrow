package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import gg.feral.burrow.pathfinder.SuggestionEngine;
import java.util.*;

/**
 * BQA-IMPL-R09: the "direction" text the Home tab shows. Pure, evidence-gated:
 * <ul>
 *   <li>Next objective comes from the owner's SAVED Pathfinder goals first, else the top current suggestion; readiness is
 *       the engine's observed MET / UNMET / UNKNOWN, never "completed". "Done" only ever means the owner marked it done.</li>
 *   <li>Long-term goals are the selected rare-drop goals with chance by verified KC (UNKNOWN without KC); a drop is shown
 *       as obtained only when the Collection Log page was observed with it unlocked.</li>
 *   <li>Upgrades come only from a current, non-stale bank read; otherwise the text says why they are unknown.</li>
 * </ul>
 */
public final class HomeDirection {
    private HomeDirection() {}
    public record Line(String text, Kind kind, int itemId) {
        public enum Kind { HEAD, NORMAL, GOOD, WARN, MUTED }
    }

    public static List<Line> objective(SuggestionEngine.Snapshot snapshot, List<SuggestionEngine.Suggestion> current) {
        List<Line> out = new ArrayList<>();
        if (snapshot == null) { out.add(new Line("Sign in for account-based direction", Line.Kind.MUTED, -1)); return out; }
        List<SuggestionEngine.Suggestion> all = SuggestionEngine.evaluateAll(snapshot);
        long saved = snapshot.goals().values().stream().filter(m -> m == SuggestionEngine.GoalMark.SAVED).count();
        long done = snapshot.goals().values().stream().filter(m -> m == SuggestionEngine.GoalMark.DONE).count();
        SuggestionEngine.Suggestion pick = all.stream().filter(s -> s.goal() == SuggestionEngine.GoalMark.SAVED).findFirst()
            .orElse(current == null || current.isEmpty() ? null : current.get(0));
        if (pick == null) { out.add(new Line("No open suggestion matches your filters", Line.Kind.MUTED, -1)); }
        else {
            out.add(new Line(pick.title(), Line.Kind.HEAD, -1));
            String step = pick.nextStep() != null && !pick.nextStep().isBlank() ? pick.nextStep() : pick.why();
            if (step != null && !step.isBlank()) out.add(new Line(step, Line.Kind.NORMAL, -1));
            out.add(switch (pick.readiness()) {
                case MET -> pick.readyNow() ? new Line("Requirements observed", Line.Kind.GOOD, -1)
                    : new Line("Requirements observed · travel time unknown", Line.Kind.WARN, -1);
                case UNMET -> new Line("Missing: " + (pick.missing().isEmpty() ? "see Pathfinder" : String.join(", ", pick.missing())), Line.Kind.WARN, -1);
                case UNKNOWN -> new Line("Requirements UNKNOWN (not observed yet)", Line.Kind.MUTED, -1);
            });
            out.add(new Line(pick.goal() == SuggestionEngine.GoalMark.SAVED ? "Your saved goal" : "Top Pathfinder suggestion", Line.Kind.MUTED, -1));
        }
        out.add(new Line(saved + " saved goal" + (saved == 1 ? "" : "s") + " · " + done + " marked done by you", Line.Kind.MUTED, -1));
        return out;
    }

    public static List<Line> dropGoals(RareDropStore.State state) {
        List<Line> out = new ArrayList<>();
        if (state == null) { out.add(new Line("Sign in to see your drop goals", Line.Kind.MUTED, -1)); return out; }
        var goals = RareDropSummary.all(state);
        if (goals.isEmpty()) { out.add(new Line("No drop goals selected (Outfitter → Rare drops)", Line.Kind.MUTED, -1)); return out; }
        for (var sum : goals) {
            for (var d : sum.selected()) {
                boolean got = state.ownership(d.itemId()) == RareDropStore.Ownership.OBSERVED_OBTAINED;
                var odds = RareDropRowOdds.row(sum.encounter(), d, state.effective(sum.encounter().encounter()));
                String v = got ? "in your Collection Log" : odds.byKc().isPresent()
                    ? RareDropRowOdds.label(odds) + ": " + RareDropRowOdds.value(odds) : RareDropRowOdds.label(odds);
                out.add(new Line(d.name() + " · " + sum.encounter().encounter(), got ? Line.Kind.GOOD : Line.Kind.NORMAL, d.itemId()));
                out.add(new Line(v, got ? Line.Kind.GOOD : odds.byKc().isPresent() ? Line.Kind.WARN : Line.Kind.MUTED, -1));
            }
        }
        return out;
    }

    public static List<Line> upgrades(BestBankPreloader.UpgradeEvidence evidence, List<BestBankPreloader.Upgrade> list) {
        return upgrades(evidence, list, -1, -1);
    }
    /** CP-1 (BQA-R12-017): states how many prepared plans the list covers; it is never a global best-upgrade ranking. */
    public static List<Line> upgrades(BestBankPreloader.UpgradeEvidence evidence, List<BestBankPreloader.Upgrade> list,
                                      int prepared, int total) {
        List<Line> out = new ArrayList<>();
        if (evidence == BestBankPreloader.UpgradeEvidence.CURRENT && prepared >= 0 && total > 0)
            out.add(new Line("From " + prepared + " of " + total + " prepared plans · not a global ranking", Line.Kind.MUTED, -1));
        switch (evidence) {
            case NONE -> { out.add(new Line("UNKNOWN · open your bank to check upgrades", Line.Kind.MUTED, -1)); return out; }
            case STALE -> { out.add(new Line("Bank read is stale · open your bank again", Line.Kind.MUTED, -1)); return out; }
            default -> {}
        }
        if (list.isEmpty()) { out.add(new Line(prepared == 0 ? "Plans still being prepared" : "No gaps in the plans prepared so far", prepared == 0 ? Line.Kind.MUTED : Line.Kind.GOOD, -1)); return out; }
        for (var u : list) {
            String why = u.kind() == BestBankPreloader.Upgrade.Kind.MISSING ? "not in bank read" : "lower tier in use";
            String bosses = u.bosses().size() == 1 ? u.bosses().get(0) : u.bosses().size() + " bosses";
            out.add(new Line(u.itemName(), Line.Kind.NORMAL, u.itemId()));
            out.add(new Line(u.slot() + " · " + why + " · " + bosses, u.kind() == BestBankPreloader.Upgrade.Kind.MISSING ? Line.Kind.MUTED : Line.Kind.WARN, -1));
        }
        return out;
    }
}
