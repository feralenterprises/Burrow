package gg.feral.burrow.experience;

/** Where a tip wants to take the player. Data only: a later UI decides whether to offer it. */
public record DestinationIntent(Kind kind, String target) {
    public enum Kind { OPEN_PATHFINDER, OPEN_CHRONICLE_BOSS, OPEN_OUTFITTER_BOSS }
}
