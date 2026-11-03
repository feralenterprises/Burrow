package gg.feral.burrow.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * BUR-0025: in-memory, read-only boss KC delta accounting, independent of RuneLite.
 *
 * <p>This class does not poll KC, infer boss identity, store account details or touch
 * the UI. The caller supplies authenticated session/epoch transitions and monotonically
 * sequenced samples. Every new boss/epoch starts with a ZERO-delta baseline.
 *
 * <p>Not thread-affine: synchronized methods make replay protection atomic. Do not
 * reuse this instance after host-side sequence counters are restarted without creating
 * a fresh bridge/tracker lifecycle and establishing a new baseline.
 */
public final class BossKcDeltaTracker {
    public enum Reliability { RELIABLE, UNKNOWN, UNRELIABLE }

    public enum Status {
        NO_SESSION, WRONG_IDENTITY, NO_EPOCH, WRONG_EPOCH, OLD_OR_REPLAYED,
        BASELINED, GAIN, UNCHANGED, UNKNOWN_KC, UNRELIABLE_KC,
        COUNTER_REGRESSION, QUARANTINED
    }

    /** IDs are opaque and exact: the tracker deliberately never renames or normalizes bosses. */
    public record Observation(
            String accountId, String sessionId, long sessionGeneration,
            String epochId, long epochGeneration,
            String bossId, long sampleSequence, Reliability reliability, Long killCount) {
        public Observation {
            requireId(accountId, "accountId");
            requireId(sessionId, "sessionId");
            requireId(epochId, "epochId");
            requireId(bossId, "bossId");
            if (sessionGeneration < 1 || epochGeneration < 1 || sampleSequence < 1) {
                throw new IllegalArgumentException("generations and sampleSequence must be positive");
            }
            Objects.requireNonNull(reliability, "reliability");
            if (reliability == Reliability.RELIABLE && (killCount == null || killCount < 0)) {
                throw new IllegalArgumentException("RELIABLE KC must be a nonnegative integer");
            }
        }
    }

    /** delta is always >= 0; sessionTotal counts only accepted gains in the current login. */
    public record Outcome(Status status, String bossId, long delta, long sessionTotal) {
        public Outcome {
            Objects.requireNonNull(status, "status");
            requireId(bossId, "bossId");
            if (delta < 0 || sessionTotal < 0) throw new IllegalArgumentException("negative total");
        }
    }

    private static final class BossState {
        long lastSequence;
        Long lastReliableKc;
        boolean quarantined;
    }

    private long highestSessionGeneration;
    private boolean sessionOpen;
    private String accountId;
    private String sessionId;
    private long sessionGeneration;
    private String epochId;
    private long epochGeneration;
    private final Set<String> openedEpochIds = new HashSet<>();
    private final Map<String, BossState> byBoss = new HashMap<>();
    private final Map<String, Long> sessionTotals = new HashMap<>();

    /**
     * Explicit login or account switch. Generation must increase across this tracker
     * instance, including after logout. Replayed/older login events cannot resurrect state.
     * Returns false without mutation for stale generations.
     */
    public synchronized boolean openSession(String newAccountId, String newSessionId, long generation) {
        requireId(newAccountId, "accountId");
        requireId(newSessionId, "sessionId");
        if (generation < 1) throw new IllegalArgumentException("session generation must be positive");
        if (generation <= highestSessionGeneration) return false;
        highestSessionGeneration = generation;
        clearCurrentSession();
        accountId = newAccountId;
        sessionId = newSessionId;
        sessionGeneration = generation;
        sessionOpen = true;
        return true;
    }

    /**
     * Called BEFORE KC samples after login, hop, server reset, or any uncertain scope
     * change. Epoch generation is strictly increasing within a login. An epoch ID can
     * never be reopened within that login, even using a higher generation.
     */
    public synchronized boolean openEpoch(
            String expectedAccountId, String expectedSessionId, long expectedSessionGeneration,
            String newEpochId, long newEpochGeneration) {
        requireId(expectedAccountId, "accountId");
        requireId(expectedSessionId, "sessionId");
        requireId(newEpochId, "epochId");
        if (newEpochGeneration < 1) throw new IllegalArgumentException("epoch generation must be positive");
        if (!matchesSession(expectedAccountId, expectedSessionId, expectedSessionGeneration)) return false;
        if (newEpochGeneration <= epochGeneration || openedEpochIds.contains(newEpochId)) return false;
        epochId = newEpochId;
        epochGeneration = newEpochGeneration;
        openedEpochIds.add(newEpochId);
        byBoss.clear(); // Do not diff a KC from a different world/server epoch.
        return true;    // Session totals persist over a legitimate world hop.
    }

    /** Logout only closes the exact active session; late logout from an old account is ignored. */
    public synchronized boolean logout(String expectedAccountId, String expectedSessionId, long expectedGeneration) {
        if (!matchesSession(expectedAccountId, expectedSessionId, expectedGeneration)) return false;
        clearCurrentSession();
        return true;
    }

    /** Pure accounting: never calls RuneLite and never mutates the observed account. */
    public synchronized Outcome observe(Observation snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        final String boss = snapshot.bossId();
        if (!sessionOpen) return rejected(Status.NO_SESSION, boss);
        if (!matchesSession(snapshot.accountId(), snapshot.sessionId(), snapshot.sessionGeneration())) {
            return rejected(Status.WRONG_IDENTITY, boss);
        }
        if (epochId == null) return rejected(Status.NO_EPOCH, boss);
        if (snapshot.epochGeneration() != epochGeneration || !epochId.equals(snapshot.epochId())) {
            return rejected(Status.WRONG_EPOCH, boss);
        }

        BossState state = byBoss.computeIfAbsent(boss, ignored -> new BossState());
        if (snapshot.sampleSequence() <= state.lastSequence) return rejected(Status.OLD_OR_REPLAYED, boss);
        state.lastSequence = snapshot.sampleSequence();

        if (snapshot.reliability() == Reliability.UNKNOWN) {
            state.lastReliableKc = null; // uncertain gap: never bridge an unverifiable period
            return outcome(Status.UNKNOWN_KC, boss, 0);
        }
        if (snapshot.reliability() == Reliability.UNRELIABLE) {
            state.lastReliableKc = null;
            return outcome(Status.UNRELIABLE_KC, boss, 0);
        }
        if (state.quarantined) return outcome(Status.QUARANTINED, boss, 0);
        final long kc = snapshot.killCount(); // guaranteed non-null and nonnegative
        if (state.lastReliableKc == null) {
            state.lastReliableKc = kc;
            return outcome(Status.BASELINED, boss, 0);
        }
        if (kc < state.lastReliableKc) {
            // A counter regression may be a reset, wrong world, or invalid reading.
            // Freeze until the host explicitly starts a fresh epoch. Do NOT count 0->1.
            state.lastReliableKc = null;
            state.quarantined = true;
            return outcome(Status.COUNTER_REGRESSION, boss, 0);
        }
        long increase = kc - state.lastReliableKc;
        if (increase == 0) {
            state.lastReliableKc = kc;
            return outcome(Status.UNCHANGED, boss, 0);
        }
        // Compute before mutating any state; overflow cannot half-commit a sample.
        long updatedTotal = Math.addExact(sessionTotals.getOrDefault(boss, 0L), increase);
        state.lastReliableKc = kc;
        sessionTotals.put(boss, updatedTotal);
        return outcome(Status.GAIN, boss, increase);
    }

    public synchronized long sessionTotal(String bossId) {
        requireId(bossId, "bossId");
        return sessionTotals.getOrDefault(bossId, 0L);
    }

    public synchronized boolean isSessionOpen() { return sessionOpen; }

    /**
     * A rejected sample is not authorized to read active-session totals. Even when
     * the same boss exists in the active session, stale identity, epoch, or
     * sequence samples return no cross-context information.
     */
    private Outcome rejected(Status status, String boss) {
        return new Outcome(status, boss, 0, 0);
    }

    private Outcome outcome(Status status, String boss, long delta) {
        return new Outcome(status, boss, delta, sessionTotals.getOrDefault(boss, 0L));
    }

    private boolean matchesSession(String account, String session, long generation) {
        return sessionOpen && Objects.equals(accountId, account) && Objects.equals(sessionId, session)
                && sessionGeneration == generation;
    }

    private void clearCurrentSession() {
        sessionOpen = false;
        accountId = null;
        sessionId = null;
        sessionGeneration = 0;
        epochId = null;
        epochGeneration = 0;
        openedEpochIds.clear();
        byBoss.clear();
        sessionTotals.clear();
    }

    private static void requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must be nonblank");
    }
}
