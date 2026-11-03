package gg.feral.burrow.intelligence;

import gg.feral.burrow.core.CombatTaskGuide;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * W06 read-only Chronicle integration. Exact existing boss/name crosswalk required.
 * Never writes Combat Task completion, player state, or the legacy TSV.
 * CHECK and missing evidence cannot replace the existing reference wording.
 */
public final class ChronicleIntelligenceBridge {
    private ChronicleIntelligenceBridge() {}

    public record Display(String condition, String tip, boolean reviewed, String note) {
        public Display {
            Objects.requireNonNull(condition);
            Objects.requireNonNull(tip);
            Objects.requireNonNull(note);
        }
    }

    private static final Map<String,CombatAchievementKnowledge.Task> APPROVED = loadApproved();

    private static Map<String,CombatAchievementKnowledge.Task> loadApproved() {
        try {
            return approvedIndex(CombatAchievementKnowledge.bundled());
        } catch (IOException | RuntimeException missingOrInvalidResearch) {
            // Research must fail closed. The legacy card still works offline.
            return Map.of();
        }
    }

    /** Pure injection point for independent review and tests. */
    public static Map<String,CombatAchievementKnowledge.Task> approvedIndex(CombatAchievementKnowledge.Pack pack) {
        Objects.requireNonNull(pack);
        Map<String,CombatAchievementKnowledge.Task> approved = new HashMap<>();
        for (CombatAchievementKnowledge.Task task : pack.tasks()) {
            if (!task.approved()) continue;
            if (approved.putIfAbsent(task.crosswalkKey(), task) != null)
                throw new IllegalArgumentException("Duplicate approved CA crosswalk: " + task.crosswalkKey());
        }
        return Map.copyOf(approved);
    }

    public static Display display(CombatTaskGuide.Task legacy) {
        return display(legacy, APPROVED);
    }

    public static Display display(CombatTaskGuide.Task legacy,
                                  Map<String,CombatAchievementKnowledge.Task> approved) {
        Objects.requireNonNull(legacy);
        Objects.requireNonNull(approved);
        CombatAchievementKnowledge.Task task = approved.get(CombatAchievementKnowledge.key(legacy.boss(),legacy.name()));
        if (task == null || !task.approved() ||
            !task.tier().name().equalsIgnoreCase(legacy.tier()) ||
            task.canonicalCondition().isBlank()) {
            return new Display(legacy.requirement(),legacy.tip(),false,
                "Research reference only; canonical task condition not independently verified");
        }
        return new Display(task.canonicalCondition(),task.strategyTip(),true,
            "Pinned, approved source; task completion remains unobserved");
    }

    /** Currently zero reviewed CA task rows, so all Chronicle legacy tasks remain CHECK. */
    public static int liveApprovedCount() { return APPROVED.size(); }
}
