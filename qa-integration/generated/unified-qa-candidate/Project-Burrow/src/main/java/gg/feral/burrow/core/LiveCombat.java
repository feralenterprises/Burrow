package gg.feral.burrow.core;

import java.util.Arrays;

/** A read-only, unsaved snapshot of seven authentic base combat levels. No account identity. */
public final class LiveCombat {
    public enum State { OFFLINE, WAITING, OBSERVED }
    public static final String[] LABELS = {"Attack", "Strength", "Defence", "Hitpoints", "Ranged", "Magic", "Prayer"};
    private final State state;
    private final int[] levels;
    private LiveCombat(State state, int[] levels) {
        this.state = state;
        this.levels = levels == null ? null : levels.clone();
    }
    public static LiveCombat offline() { return new LiveCombat(State.OFFLINE, null); }
    public static LiveCombat waiting() { return new LiveCombat(State.WAITING, null); }
    public static LiveCombat fromRealLevels(int[] values) {
        if (values == null || values.length != LABELS.length) return waiting();
        for (int level : values) if (level < 1 || level > 126) return waiting();
        return new LiveCombat(State.OBSERVED, values);
    }
    public State state() { return state; }
    public int level(int index) {
        if (index < 0 || index >= LABELS.length) throw new IndexOutOfBoundsException();
        return state == State.OBSERVED ? levels[index] : -1;
    }
    public String label(int index) { return level(index) < 0 ? "?" : Integer.toString(level(index)); }
    public String status() {
        return switch(state) {
            case OFFLINE -> "Log in to see combat levels";
            case WAITING -> "Waiting for skill data...";
            case OBSERVED -> "Live base levels, read-only";
        };
    }
    @Override public String toString() { return "LiveCombat{" + state + "," + Arrays.toString(levels) + "}"; }
}
