package gg.feral.burrow.intelligence;

import java.util.Map;
import java.util.Objects;

/**
 * W05: CA prerequisite reasoning, deliberately separate from task completion.
 * A player can have the right equipment and still fail or never attempt a task.
 * Only an authenticated, task-specific game confirmation can report COMPLETE.
 */
public final class CombatAchievementDecisions {
    private CombatAchievementDecisions() {}
    public enum Signal { YES, NO, UNKNOWN }
    public enum Prep { RESEARCH_ONLY, VERIFY, BLOCKED, ELIGIBLE_TO_ATTEMPT }
    public enum Completion { COMPLETE, UNKNOWN }
    public record Observation(Map<String,Signal> gates) {
        public Observation {gates=Map.copyOf(Objects.requireNonNull(gates));}
        public Signal signal(CombatAchievementKnowledge.Gate gate) {
            return gates.getOrDefault(gate.id(),Signal.UNKNOWN);
        }
        public static Observation none() {return new Observation(Map.of());}
    }
    public record Result(Prep preparation,Completion completion,String explanation) {
        public Result { Objects.requireNonNull(preparation); Objects.requireNonNull(completion); Objects.requireNonNull(explanation); }
    }

    /** Distinguishes preconditions from execution strategy; never evaluates tip text. */
    public static Prep preparation(CombatAchievementKnowledge.Task task,Observation observation) {
        Objects.requireNonNull(task);
        if (!task.approved()) return Prep.RESEARCH_ONLY;
        if (observation==null || task.gates().isEmpty()) return Prep.VERIFY;
        boolean unknown=false;
        for (CombatAchievementKnowledge.Gate gate:task.gates()) {
            Signal observed=observation.signal(gate);
            if (observed==Signal.NO) return Prep.BLOCKED;
            if (observed==Signal.UNKNOWN) unknown=true;
        }
        return unknown?Prep.VERIFY:Prep.ELIGIBLE_TO_ATTEMPT;
    }

    /** Completion must be confirmed via the game's task-level feed, never from KC, gear or combat tips. */
    public static Completion completion(CombatAchievementKnowledge.Task task,
                                        boolean authenticatedAccount,
                                        Map<String,String> gameConfirmedTaskSources) {
        Objects.requireNonNull(task);
        if (!task.approved() || !authenticatedAccount || gameConfirmedTaskSources==null) return Completion.UNKNOWN;
        String source=gameConfirmedTaskSources.get(task.id());
        return source!=null && !source.isBlank() ? Completion.COMPLETE : Completion.UNKNOWN;
    }

    public static Result evaluate(CombatAchievementKnowledge.Task task,Observation observation,
                                  boolean authenticatedAccount,Map<String,String> gameConfirmedTaskSources) {
        Prep prep=preparation(task,observation);
        Completion complete=completion(task,authenticatedAccount,gameConfirmedTaskSources);
        String explanation=switch(prep) {
            case RESEARCH_ONLY -> "Task research not approved; no readiness or completion inference";
            case VERIFY -> "Prerequisites are incomplete or unobserved; verify before attempting";
            case BLOCKED -> "A verified mandatory prerequisite is observed unmet";
            case ELIGIBLE_TO_ATTEMPT -> "Verified prerequisites observed; task execution is still required";
        };
        return new Result(prep,complete,explanation);
    }
}
