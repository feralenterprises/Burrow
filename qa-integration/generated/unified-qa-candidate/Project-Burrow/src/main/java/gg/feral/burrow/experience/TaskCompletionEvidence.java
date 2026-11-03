package gg.feral.burrow.experience;

import java.util.Map;

/**
 * Game-confirmed Combat Achievement completion, task name to source. CP9 has no such feed, so {@link #none()} is the
 * only real value today, and every task reads UNKNOWN. A later host adapter can fill this from game state.
 */
public record TaskCompletionEvidence(Map<String,String> sourceByTask) {
    public TaskCompletionEvidence { sourceByTask = Map.copyOf(sourceByTask); }
    public static TaskCompletionEvidence none() { return new TaskCompletionEvidence(Map.of()); }
}
