package gg.feral.burrow.core;

import java.util.Objects;

/**
 * One explicitly bounded recommendation, derived from an observed RuneLite quest state.
 * The quest has no prerequisites. Unknown / loading is NEVER assumed incomplete.
 * No account identifier or quest history is stored.
 */
public final class QuestRecommendation {
    public enum State { OFFLINE, WAITING, UNKNOWN, NOT_STARTED, IN_PROGRESS, FINISHED }
    public static final String QUEST_NAME = "Cook's Assistant";
    private final State state;

    private QuestRecommendation(State state) { this.state = Objects.requireNonNull(state); }
    public static QuestRecommendation of(State state) { return new QuestRecommendation(state); }
    public static QuestRecommendation offline() { return of(State.OFFLINE); }
    public static QuestRecommendation waiting() { return of(State.WAITING); }
    public static QuestRecommendation unknown() { return of(State.UNKNOWN); }
    public State state() { return state; }

    /** The only two states where this quest can be recommended. */
    public boolean eligible() { return state == State.NOT_STARTED || state == State.IN_PROGRESS; }
    public String headline() {
        return switch (state) {
            case OFFLINE -> "Log in to check quests";
            case WAITING -> "Checking quest journal...";
            case UNKNOWN -> "Quest status unavailable";
            case NOT_STARTED -> "Start Cook's Assistant";
            case IN_PROGRESS -> "Finish Cook's Assistant";
            case FINISHED -> "Cook's Assistant complete";
        };
    }
    public String explanation() {
        return switch (state) {
            case OFFLINE -> "No account observations retained";
            case WAITING -> "Waiting for a RuneLite quest read";
            case UNKNOWN -> "No unfinished-quest claim made";
            case NOT_STARTED -> "Eligible: no prerequisites";
            case IN_PROGRESS -> "Observed unfinished in quest log";
            case FINISHED -> "Not recommended again";
        };
    }
}
