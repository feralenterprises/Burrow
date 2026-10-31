package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BossKcDeltaTracker;
import gg.feral.burrow.core.VerifiedOutfitterCatalog;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;

/**
 * BUR-0025: an unwired, client-thread-only RuneLite chat-to-KC observer.
 *
 * <p>ONLY RuneLite game-message events carrying a parseable, absolute
 * KC for an explicitly supported boss reach the existing BossKcDeltaTracker.
 * NPC deaths, loot notifications, chat from players, demo clicks, and arbitrary
 * UI selections are not KC. This adapter NEVER edits the host plugin or UI.
 *
 * <p>The host MUST call onGameStateChanged(event) and then, after each verified
 * LOGGED_IN transition, onAuthenticatedLogin(client.getAccountHash()) on the
 * SAME RuneLite client thread. -1 is not authenticated: it retires any active
 * login scope (R02) and KC stays suspended until a valid hash. The host then forwards
 * onChatMessage(event) directly, without queueing old-account messages. The
 * returned Optional is diagnostic only; Sessions changes ONLY via SessionsSink.
 *
 * <p>Account identifiers are ephemeral, never published to Sessions or stored.
 * The existing core's rejected Outcome.sessionTotal=0 privacy repair is kept;
 * this bridge never uses the unscoped core sessionTotal(bossId) accessor.
 */
public final class RuneLiteBossKcAdapter {
    /** Synchronous, client-thread-bound callbacks. The host owns the UI thread handoff. */
    public interface SessionsSink {
        /** Called once on logout, new login, account change, explicit stop, or loss of verified identity (-1). */
        void onCurrentLoginReset();
        /** Accepted positive KC increment only; IDs match the existing catalog. */
        void onBossKcGain(String exactBossId, long delta, long currentLoginBossTotal);
    }

    private static final Pattern KC_LINE = Pattern.compile(
            "^Your (?<boss>[A-Za-z][A-Za-z0-9 '\\-’]+?) (?<kind>kill count|chest count) is(?::\\s*|\\s+)" +
            "(?<count>(?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+))\\.?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COLOR_TAG = Pattern.compile("(?i)</?col(?:=[0-9a-f]{6})?>");
    private static final Map<String, String> CHAT_TO_BOSS = createBossAliases();

    private final BossKcDeltaTracker tracker;
    private final SessionsSink sink;
    private boolean inGame;
    private boolean needsEpoch = true;
    private boolean epochReady;
    private String activeAccount;
    private String activeSession;
    private long sessionGeneration;
    private long epochGeneration;
    private long nextSequence;

    public RuneLiteBossKcAdapter(SessionsSink sink) {
        this(sink, new BossKcDeltaTracker());
    }

    // Package-private for isolated replay and R06 rejected-outcome checks.
    RuneLiteBossKcAdapter(SessionsSink sink, BossKcDeltaTracker tracker) {
        this.sink = Objects.requireNonNull(sink, "sink");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
    }

    /**
     * Forward the REAL RuneLite GameStateChanged event, in client-thread order.
     * HOPPING / LOADING / CONNECTION_LOST preserve current-login totals but
     * suspend samples until a new epoch is opened after LOGGED_IN. A region
     * LOADING may unnecessarily rebaseline, but it cannot manufacture kills.
     */
    public void onGameStateChanged(GameStateChanged event) {
        Objects.requireNonNull(event, "event");
        GameState state = event.getGameState();
        if (state == GameState.LOGGED_IN) {
            inGame = true;
        } else if (state == GameState.LOGIN_SCREEN || state == GameState.LOGIN_SCREEN_AUTHENTICATOR
                || state == GameState.STARTING) {
            inGame = false;
            retireLogin();
        } else {
            inGame = false;
            epochReady = false;
            needsEpoch = true;
        }
    }

    /**
     * Identity gate: call ONLY after confirmed LOGGED_IN, passing RuneLite's
     * account hash, never a display name or a cached last-account guess.
     * A changed hash resets the prior session even if LOGIN_SCREEN was missed.
     * Same account after a hop gets a new baseline without erasing login gains.
     * Returns false on unknown (-1) account or absent LOGGED_IN transition.
     *
     * <p>BUR-0025 R02: an unknown (-1) hash FAILS CLOSED. If a login scope was
     * active, it is retired exactly like a logout (core session closed, Sessions
     * reset once, epoch invalidated), so no later chat line can be counted under
     * the previously verified account. Identity is no longer verified, so the
     * old current-login totals are not carried over; a later valid hash, even
     * the same one, opens a NEW login scope whose first count is a zero baseline.
     * Calling -1 when nothing is active, or repeatedly, is a harmless no-op.
     */
    public boolean onAuthenticatedLogin(long accountHash) {
        if (accountHash == -1) {
            retireLogin();
            return false;
        }
        if (!inGame) return false;
        String nextAccount = Long.toString(accountHash);
        if (!nextAccount.equals(activeAccount)) {
            if (sessionGeneration == Long.MAX_VALUE) throw new IllegalStateException("KC session generation exhausted");
            sessionGeneration++;
            activeAccount = nextAccount;
            activeSession = "login-" + sessionGeneration;
            epochGeneration = 0;
            nextSequence = 0;
            needsEpoch = true;
            epochReady = false;
            if (!tracker.openSession(activeAccount, activeSession, sessionGeneration)) {
                throw new IllegalStateException("KC core rejected a fresh login generation");
            }
            sink.onCurrentLoginReset();
        }
        if (needsEpoch) {
            if (epochGeneration == Long.MAX_VALUE) throw new IllegalStateException("KC epoch generation exhausted");
            epochGeneration++;
            // No account identifiers are present in the epoch ID.
            String epochId = "epoch-" + epochGeneration;
            if (!tracker.openEpoch(activeAccount, activeSession, sessionGeneration, epochId, epochGeneration)) {
                epochReady = false;
                return false;
            }
            nextSequence = 0;
            needsEpoch = false;
            epochReady = true;
        }
        return epochReady;
    }

    /**
     * Parse and observe a game/spam KC line after identity gating.
     * First observed absolute count is a baseline with ZERO gain. No
     * guessed events, no public-chat spoofing, and no unverified boss aliases. RuneLite chat events are not cryptographic
     * proof of server origin; the host must not synthesize these events.
     * Optional.empty() means no accepted KC observation was attempted.
     */
    public Optional<BossKcDeltaTracker.Outcome> onChatMessage(ChatMessage event) {
        if (event == null || !inGame || !epochReady || activeAccount == null) return Optional.empty();
        ChatMessageType type = event.getType();
        // RuneLite SPAM contains filtered game messages; never accept user chat.
        if (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM) return Optional.empty();
        Optional<KcLine> parsed = parseKcLine(event.getMessage());
        if (parsed.isEmpty()) return Optional.empty();
        KcLine kcLine = parsed.get();
        if (nextSequence == Long.MAX_VALUE) throw new IllegalStateException("KC observation sequence exhausted");
        BossKcDeltaTracker.Outcome outcome = tracker.observe(new BossKcDeltaTracker.Observation(
                activeAccount, activeSession, sessionGeneration,
                "epoch-" + epochGeneration, epochGeneration,
                kcLine.bossId(), ++nextSequence, BossKcDeltaTracker.Reliability.RELIABLE, kcLine.count()));
        // On rejection, never publish sessionTotal (the R06 privacy rule).
        if (outcome.status() == BossKcDeltaTracker.Status.GAIN) {
            sink.onBossKcGain(outcome.bossId(), outcome.delta(), outcome.sessionTotal());
        }
        return Optional.of(outcome);
    }

    /** Explicit plugin shutdown, including when RuneLite emits no logout event. */
    public void stop() {
        inGame = false;
        retireLogin();
    }

    private void retireLogin() {
        if (activeAccount != null) {
            tracker.logout(activeAccount, activeSession, sessionGeneration);
            sink.onCurrentLoginReset();
        }
        activeAccount = null;
        activeSession = null;
        epochReady = false;
        needsEpoch = true;
        epochGeneration = 0;
        nextSequence = 0;
    }

    private record KcLine(String bossId, long count) {}

    private static Optional<KcLine> parseKcLine(String raw) {
        if (raw == null || raw.length() > 256) return Optional.empty();
        // RuneLite's color markup surrounds some server counts. Other markup
        // is not recognized, rather than being stripped speculatively.
        String text = COLOR_TAG.matcher(raw).replaceAll("").trim();
        Matcher m = KC_LINE.matcher(text);
        if (!m.matches()) return Optional.empty();
        String boss = CHAT_TO_BOSS.get(m.group("boss").toLowerCase(Locale.ROOT));
        if (boss == null) return Optional.empty();
        if ("chest count".equalsIgnoreCase(m.group("kind")) && !"Barrows".equals(boss)) {
            return Optional.empty();
        }
        if ("kill count".equalsIgnoreCase(m.group("kind")) && "Barrows".equals(boss)) {
            return Optional.empty(); // Barrows tracks rewarded chests, not brothers' deaths.
        }
        try {
            long count = Long.parseLong(m.group("count").replace(",", ""));
            return Optional.of(new KcLine(boss, count));
        } catch (NumberFormatException overflowOrBadNumber) {
            return Optional.empty();
        }
    }

    private static Map<String, String> createBossAliases() {
        Map<String, String> result = new HashMap<>();
        for (String id : VerifiedOutfitterCatalog.BOSSES) {
            // Individual Rex/Prime/Supreme KC must not masquerade as combined DK KC.
            if (!"Dagannoth Kings".equals(id)) result.put(id.toLowerCase(Locale.ROOT), id);
        }
        // Exact game-spelling aliases, not generic substring or NPC-name guessing.
        if (result.containsValue("Hueycoatl")) result.put("the hueycoatl", "Hueycoatl");
        if (result.containsValue("Grotesque Guardians")) result.put("the grotesque guardians", "Grotesque Guardians");
        return Map.copyOf(result);
    }
}
