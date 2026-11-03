package gg.feral.burrow.runelite;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;

import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * BUR-0049: current-login-only StatChanged XP observation. An isolated client-thread
 * component, NOT a login authenticator, session store, UI presenter, or event-bus subscriber.
 *
 * Spoons creates one authenticated, immutable BurrowPanel.SessionScope INSTANCE per
 * confirmed logical login and passes that exact same object to every entry point.
 * Reference identity is intentional: a value-equal scope created for an old queued
 * callback must not be accepted. Never pass a name, username, display name, or the
 * ephemeral LiveWeaponGate paint epoch as a session scope.
 *
 * Spoons also owns source EventBus provenance, trusted synchronization readiness,
 * monotonically unique SessionEvent IDs, the Swing-EDT bridge, and logout detection.
 * This adapter must be invoked on RuneLite's client thread. Tests use synthetic
 * StatChanged fixtures, which are not evidence of live RuneLite operation.
 */
public final class RuneLiteXpObservationAdapter {
    /** No names, account IDs, or scope details appear in a delta's printable fields. */
    public record XpDelta(Skill skill, long delta, long previousXp, long currentXp,
                          long observationFence, String sourceEvidence) {
        public XpDelta {
            Objects.requireNonNull(skill, "skill");
            if (skill == Skill.OVERALL || delta <= 0 || previousXp < 0 ||
                currentXp <= previousXp || currentXp - previousXp != delta ||
                observationFence <= 0 || sourceEvidence == null || sourceEvidence.isBlank()) {
                throw new IllegalArgumentException("invalid sourced XP delta");
            }
        }
    }

    /** Name intentionally says exactly what is known, not that the host authenticated it. */
    public static final String EVIDENCE =
        "RuneLite StatChanged.getXp cross-checked with Client.getSkillExperience";

    private final Set<Object> retiredScopes =
        Collections.newSetFromMap(new IdentityHashMap<>());
    private final EnumMap<Skill, Integer> baselines = new EnumMap<>(Skill.class);
    private Object activeScope;
    private boolean paused;
    private long fenceCounter;
    private long activeFence;

    /**
     * Begin a CONFIRMED authenticated login. This is NOT called on LOADING/HOPPING.
     * Same-object re-entry is idempotent, preserving session state. An object used
     * in a retired session cannot be recycled, including an A -> B -> A transition.
     * Returns 0 when a retired token is supplied; otherwise a fresh observation
     * fence. The fence guards transient callbacks, not account/login identity.
     */
    public long beginVerifiedLogin(Object trustedScope) {
        Objects.requireNonNull(trustedScope, "trustedScope");
        if (retiredScopes.contains(trustedScope)) return 0;
        if (activeScope == trustedScope) return activeFence;
        retireActive();
        activeScope = trustedScope;
        paused = false;
        baselines.clear();
        activeFence = nextFence();
        return activeFence;
    }

    /** Pause collection on LOADING, HOPPING or temporary disconnect. No UI wipe. */
    public long pauseTransient(Object trustedScope) {
        if (activeScope != trustedScope || activeScope == null) return 0;
        if (!paused) {
            paused = true;
            baselines.clear(); // hop/resume stat flood must not produce fake gains
            activeFence = nextFence();
        }
        return activeFence;
    }

    /** Resume the SAME proven login; first reliable samples rebaseline conservatively. */
    public long resumeVerifiedSameLogin(Object trustedScope) {
        if (activeScope != trustedScope || activeScope == null) return 0;
        if (paused) {
            paused = false;
            baselines.clear();
            activeFence = nextFence();
        }
        return activeFence;
    }

    /** Confirmed final logout or account change only. Hopping is NEVER a logout. */
    public void confirmedLogout(Object trustedScope) {
        if (activeScope != null && activeScope == trustedScope) retireActive();
    }

    /** Spoons calls this at real plugin shutdown; no session data persists. */
    public void pluginStop() {
        retireActive();
    }

    /**
     * Optional FIRST-GAIN-SAFE baseline seed. Spoons may call this before the
     * user's first gameplay XP event ONLY after independently confirming the
     * logged-in XP table is synchronized. Merely LOGGED_IN does not prove this.
     *
     * Reads all skills atomically on the RuneLite client thread. A legitimate
     * zero is valid after verified sync. Any unavailable/negative read rejects
     * the entire seed; no partial or fabricated initialization. Never reseed
     * an already-active observation set, as that would swallow real gains.
     * Re-run only after pause/resume has deliberately cleared baselines.
     */
    public boolean seedVerifiedClientSnapshot(Object trustedScope, long observationFence,
                                              Client client, boolean skillXpSynchronized) {
        if (!accepting(trustedScope, observationFence) || !skillXpSynchronized ||
            client == null || !baselines.isEmpty()) return false;
        EnumMap<Skill, Integer> snapshot = new EnumMap<>(Skill.class);
        try {
            if (client.getGameState() != GameState.LOGGED_IN) return false;
            for (Skill skill : Skill.values()) {
                if (skill == Skill.OVERALL) continue;
                int xp = client.getSkillExperience(skill);
                if (xp < 0) return false;
                snapshot.put(skill, xp);
            }
        } catch (RuntimeException unavailable) {
            return false;
        }
        if (snapshot.isEmpty()) return false;
        baselines.putAll(snapshot);
        return true;
    }

    /**
     * Consume only the actual RuneLite EventBus StatChanged event on the client
     * thread. Spoons must set skillXpSynchronized only on independently confirmed
     * post-login stat synchronization, not merely because GameState is LOGGED_IN.
     * A reliable zero is permitted. If synchronization cannot be proved, stay
     * unavailable rather than interpreting a 0 -> cached-XP flood as a gain.
     *
     * The current Client XP must equal event XP; a mismatched/delayed callback
     * is rejected without modifying the baseline. getLevel()/getBoostedLevel()
     * are deliberately not used. OVERALL is excluded.
     *
     * Spoons wraps the returned delta on this same client-thread callback into:
     * SessionXpGain(theExactScope, uniqueHostEventId, VERIFIED_BRIDGE,
     *    delta.skill().getName(), delta.delta(), delta.sourceEvidence()),
     * then re-checks the exact session scope and event ID on Swing EDT.
     */
    public Optional<XpDelta> onStatChanged(Object trustedScope, long observationFence,
                                           StatChanged event, Client client,
                                           boolean skillXpSynchronized) {
        if (activeScope == null || trustedScope != activeScope || paused ||
            observationFence != activeFence || observationFence <= 0 ||
            !skillXpSynchronized || event == null || client == null) {
            return Optional.empty();
        }
        final Skill skill = event.getSkill();
        if (skill == null || skill == Skill.OVERALL) return Optional.empty();
        final int reportedXp = event.getXp();
        if (reportedXp < 0) return Optional.empty();
        try {
            if (client.getGameState() != GameState.LOGGED_IN ||
                client.getSkillExperience(skill) != reportedXp) {
                return Optional.empty();
            }
        } catch (RuntimeException unavailable) {
            return Optional.empty(); // no guesses on stale/off-thread client reads
        }
        Integer previous = baselines.get(skill);
        if (previous == null) {
            baselines.put(skill, reportedXp); // first reliable observation, NOT a gain
            return Optional.empty();
        }
        if (reportedXp < previous) {
            baselines.remove(skill); // regression: quarantine; next good sample rebaselines
            return Optional.empty();
        }
        if (reportedXp == previous) return Optional.empty();
        baselines.put(skill, reportedXp);
        return Optional.of(new XpDelta(skill, (long) reportedXp - previous,
                previous, reportedXp, observationFence, EVIDENCE));
    }

    /** Diagnostic only: observation validity, not a replacement for host auth. */
    public boolean accepting(Object trustedScope, long observationFence) {
        return activeScope != null && activeScope == trustedScope && !paused &&
               activeFence == observationFence && observationFence > 0;
    }

    private long nextFence() {
        if (fenceCounter == Long.MAX_VALUE) {
            // Never reuse a stale fence after overflow, even hypothetically.
            throw new IllegalStateException("observation fence exhausted");
        }
        return ++fenceCounter;
    }

    private void retireActive() {
        if (activeScope != null) retiredScopes.add(activeScope);
        activeScope = null;
        paused = false;
        baselines.clear();
        activeFence = nextFence(); // invalidate pending observation callbacks
    }
}
