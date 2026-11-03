package gg.feral.burrow.experience;

/** @param pinnedGoalId the player's own goal, null when none. Noodles only reads it */
public record NoodlesOpportunity(String pinnedGoalId, String title, String nextStep, String why, EvidenceStatus status, DestinationIntent destination) {
    public static NoodlesOpportunity none(String goal, EvidenceStatus status, String why) { return new NoodlesOpportunity(goal, null, null, why, status, null); }
}
