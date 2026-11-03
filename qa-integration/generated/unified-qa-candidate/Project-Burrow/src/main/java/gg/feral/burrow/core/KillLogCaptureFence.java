package gg.feral.burrow.core;

/** PQA-W09: source-event identity fence for a deferred read-only log widget read. */
public final class KillLogCaptureFence {
    private KillLogCaptureFence() {}
    public record Pending(long accountHash, long loginEpoch, long openingTick) {
        public Pending {
            if (accountHash == -1L || loginEpoch <= 0 || openingTick < 0)
                throw new IllegalArgumentException("No authenticated widget-opening event");
        }
    }
    /** Only the same authenticated login, one to eight game ticks after the event. */
    public static boolean mayRead(Pending p, long sessionAccount, long authenticatedAccount,
                                  long loginEpoch, boolean loggedIn, boolean paused, long tick) {
        if (p == null || !loggedIn || paused || loginEpoch <= 0 || tick < p.openingTick()) return false;
        long delta = tick - p.openingTick();
        return delta >= 1 && delta <= 8 && sessionAccount == p.accountHash() &&
            authenticatedAccount == p.accountHash() && loginEpoch == p.loginEpoch();
    }
}
