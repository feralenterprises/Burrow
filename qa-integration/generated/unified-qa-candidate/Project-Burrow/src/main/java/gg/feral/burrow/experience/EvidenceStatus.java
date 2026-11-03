package gg.feral.burrow.experience;

/** How far a value can be trusted. A UI must never present anything but VERIFIED or OBSERVED as fact. */
public enum EvidenceStatus {
    /** Confirmed from a reviewed source or a game-confirmed record. */
    VERIFIED,
    /** Seen on this account by an existing tracker. */
    OBSERVED,
    /** Generic advice that claims nothing about the account. */
    GENERIC_REMINDER,
    /** A heuristic built on real evidence, with known limits. Never a promise. */
    INFERRED_WITH_LIMITS,
    /** Burrow cannot tell. Never means zero, false or not started. */
    UNKNOWN,
    /** The feed behind this value is not available right now. */
    UNAVAILABLE
}
