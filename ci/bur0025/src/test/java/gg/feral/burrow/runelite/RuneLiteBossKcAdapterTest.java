package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BossKcDeltaTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Offline event fixtures only: NOT RuneLite native or host-registration proof. */
public final class RuneLiteBossKcAdapterTest {
    private static final class Sink implements RuneLiteBossKcAdapter.SessionsSink {
        int resets;
        final List<String> gains = new ArrayList<>();
        @Override public void onCurrentLoginReset() { resets++; gains.clear(); }
        @Override public void onBossKcGain(String boss, long delta, long total) {
            gains.add(boss + ":" + delta + ":" + total);
        }
    }
    private static GameStateChanged state(GameState s) {
        GameStateChanged e = new GameStateChanged(); e.setGameState(s); return e;
    }
    private static ChatMessage chat(ChatMessageType t, String message) {
        ChatMessage e = new ChatMessage(); e.setType(t); e.setMessage(message); return e;
    }
    private static ChatMessage game(String message) { return chat(ChatMessageType.GAMEMESSAGE, message); }
    private static BossKcDeltaTracker.Status status(Optional<BossKcDeltaTracker.Outcome> outcome) {
        return outcome.orElseThrow().status();
    }
    private static void enter(RuneLiteBossKcAdapter a, long accountHash) {
        a.onGameStateChanged(state(GameState.LOGGED_IN));
        assertTrue(a.onAuthenticatedLogin(accountHash));
    }

    @Test void firstAbsoluteCountIsBaselineAndDeltasAreNotDemoKills() {
        Sink sink = new Sink(); RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink);
        assertTrue(a.onChatMessage(game("Your Vorkath kill count is: 165.")).isEmpty());
        enter(a, 101L);
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 165."))));
        assertEquals(BossKcDeltaTracker.Status.UNCHANGED, status(a.onChatMessage(game("Your Vorkath kill count is: 165."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Vorkath kill count is: <col=ff0000>170</col>."))));
        assertEquals(List.of("Vorkath:5:5"), sink.gains);
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Scurrius kill count is: 12."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Scurrius kill count is: 13."))));
        assertEquals(List.of("Vorkath:5:5", "Scurrius:1:1"), sink.gains);
    }

    @Test void userChatSpoofsLootDeathAndUnknownBossesAreNotObservations() {
        Sink sink = new Sink(); RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink);
        enter(a, 101);
        assertTrue(a.onChatMessage(chat(ChatMessageType.PUBLICCHAT, "Your Vorkath kill count is: 999.")).isEmpty());
        assertTrue(a.onChatMessage(chat(ChatMessageType.CLAN_CHAT, "Your Vorkath kill count is: 999.")).isEmpty());
        assertTrue(a.onChatMessage(game("You have killed Vorkath.")).isEmpty());
        assertTrue(a.onChatMessage(game("Valuable drop: Dragon bones")).isEmpty());
        assertTrue(a.onChatMessage(game("Your Fake Boss kill count is: 99.")).isEmpty());
        assertTrue(a.onChatMessage(game("Your Vorkath chest count is: 999.")).isEmpty());
        assertTrue(a.onChatMessage(game("Your Dagannoth Rex kill count is: 777.")).isEmpty());
        assertTrue(a.onChatMessage(game("Your Dagannoth Kings kill count is: 777.")).isEmpty());
        assertTrue(a.onChatMessage(game("Your Vorkath kill count is: 12,34.")).isEmpty());
        assertTrue(a.onChatMessage(game("Your Vorkath kill count is: -4.")).isEmpty());
        assertTrue(a.onChatMessage(game("Your Vorkath kill count is123.")).isEmpty());
        assertTrue(a.onChatMessage(game("Your Vorkath kill count is: 999999999999999999999999999.")).isEmpty());
        assertTrue(sink.gains.isEmpty());
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 100."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(chat(ChatMessageType.SPAM, "Your Vorkath kill count is: 101."))));
        assertEquals(List.of("Vorkath:1:1"), sink.gains);
    }

    @Test void worldHopKeepsSessionTotalsButNeverDiffsAcrossEpoch() {
        Sink sink = new Sink(); RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink);
        enter(a, 1001);
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 100."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Vorkath kill count is: 101."))));
        a.onGameStateChanged(state(GameState.HOPPING));
        assertTrue(a.onChatMessage(game("Your Vorkath kill count is: 5000.")).isEmpty());
        enter(a, 1001);
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 5000."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Vorkath kill count is: 5001."))));
        assertEquals(List.of("Vorkath:1:1", "Vorkath:1:2"), sink.gains);
        assertEquals(1, sink.resets);
        // A duplicate authenticated callback must not erase the baseline.
        assertTrue(a.onAuthenticatedLogin(1001));
        assertEquals(BossKcDeltaTracker.Status.UNCHANGED, status(a.onChatMessage(game("Your Vorkath kill count is: 5001."))));
    }

    @Test void logoutAndAccountSwitchResetAndDoNotLeakTotals() {
        Sink sink = new Sink(); RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink);
        enter(a, 1);
        a.onChatMessage(game("Your Vorkath kill count is: 1."));
        a.onChatMessage(game("Your Vorkath kill count is: 6."));
        a.onGameStateChanged(state(GameState.LOGIN_SCREEN));
        assertEquals(2, sink.resets);
        assertTrue(a.onChatMessage(game("Your Vorkath kill count is: 7.")).isEmpty());
        enter(a, 2);
        assertEquals(3, sink.resets);
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 100."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Vorkath kill count is: 101."))));
        assertEquals(List.of("Vorkath:1:1"), sink.gains);
        // Even if LOGIN_SCREEN were skipped, a new authenticated hash fences A from B.
        assertTrue(a.onAuthenticatedLogin(3));
        assertEquals(4, sink.resets);
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 999."))));
        assertTrue(sink.gains.isEmpty());
        a.stop();
        assertEquals(5, sink.resets);
    }

    @Test void accountAtoBtoARequiresFreshBaselineEachTime() {
        Sink sink = new Sink(); RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink);
        enter(a, 11);
        a.onChatMessage(game("Your Vorkath kill count is: 100."));
        a.onChatMessage(game("Your Vorkath kill count is: 101."));
        assertTrue(a.onAuthenticatedLogin(22));
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 900."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Vorkath kill count is: 901."))));
        assertTrue(a.onAuthenticatedLogin(11));
        assertTrue(sink.gains.isEmpty());
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 101."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Vorkath kill count is: 102."))));
        assertEquals(List.of("Vorkath:1:1"), sink.gains);
        assertEquals(3, sink.resets);
    }

    @Test void missingIdentityAndLoadingAlwaysFailClosed() {
        Sink sink = new Sink(); RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink);
        assertFalse(a.onAuthenticatedLogin(12));
        a.onGameStateChanged(state(GameState.LOGGED_IN));
        assertFalse(a.onAuthenticatedLogin(-1));
        assertTrue(a.onChatMessage(game("Your Giant Mole kill count is: 1.")).isEmpty());
        assertTrue(a.onAuthenticatedLogin(12));
        a.onChatMessage(game("Your Giant Mole kill count is: 1."));
        a.onGameStateChanged(state(GameState.LOADING));
        assertTrue(a.onChatMessage(game("Your Giant Mole kill count is: 50.")).isEmpty());
        enter(a, 12);
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Giant Mole kill count is: 50."))));
        assertTrue(sink.gains.isEmpty());
    }

    @Test void regressedCountsAreQuarantinedUntilFreshEpoch() {
        Sink sink = new Sink(); RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink);
        enter(a, 99);
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 200."))));
        assertEquals(BossKcDeltaTracker.Status.COUNTER_REGRESSION, status(a.onChatMessage(game("Your Vorkath kill count is: 199."))));
        assertEquals(BossKcDeltaTracker.Status.QUARANTINED, status(a.onChatMessage(game("Your Vorkath kill count is: 300."))));
        assertTrue(sink.gains.isEmpty());
        a.onGameStateChanged(state(GameState.HOPPING));
        enter(a, 99);
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 300."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Vorkath kill count is: 301."))));
        assertEquals(List.of("Vorkath:1:1"), sink.gains);
    }

    @Test void exactBossAliasesAndBarrowsChestsOnly() {
        Sink sink = new Sink(); RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink);
        enter(a, 55);
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Barrows chest count is 96."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Barrows chest count is: 97."))));
        assertTrue(a.onChatMessage(game("Your Barrows kill count is: 97.")).isEmpty());
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your The Hueycoatl kill count is: 1."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your The Hueycoatl kill count is: 2."))));
        assertEquals(List.of("Barrows:1:1", "Hueycoatl:1:1"), sink.gains);
    }

    @Test void r06CoreRejectedOutcomeCannotExposeOtherSessionsTotals() {
        Sink sink = new Sink(); BossKcDeltaTracker core = new BossKcDeltaTracker();
        RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink, core);
        enter(a, 55);
        a.onChatMessage(game("Your Vorkath kill count is: 3."));
        a.onChatMessage(game("Your Vorkath kill count is: 4."));
        BossKcDeltaTracker.Outcome rejected = core.observe(new BossKcDeltaTracker.Observation(
                "another-account", "login-1", 1, "epoch-1", 1, "Vorkath", 3,
                BossKcDeltaTracker.Reliability.RELIABLE, 900L));
        assertEquals(BossKcDeltaTracker.Status.WRONG_IDENTITY, rejected.status());
        assertEquals(0, rejected.sessionTotal());
        assertEquals(0, rejected.delta());
        assertEquals(List.of("Vorkath:1:1"), sink.gains);
    }

    // ---- BUR-0025 R02: unknown account hash (-1) after a verified, ready login ----

    @Test void r02ReadyAccountThenUnknownHashRejectsSpoofShapedGameMessage() {
        Sink sink = new Sink(); BossKcDeltaTracker core = new BossKcDeltaTracker();
        RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink, core);
        enter(a, 101);
        assertEquals(1, sink.resets);
        assertEquals(BossKcDeltaTracker.Status.BASELINED,
                status(a.onChatMessage(game("Your Vorkath kill count is: <col=ff0000>100</col>."))));
        // Tongs R01 counterexample: this used to leave the old epoch ready.
        assertFalse(a.onAuthenticatedLogin(-1));
        assertEquals(2, sink.resets);           // Sessions told once that identity was lost
        assertFalse(core.isSessionOpen());      // core scope closed, not merely hidden
        assertTrue(a.onChatMessage(game("Your Vorkath kill count is: <col=ff0000>101</col>.")).isEmpty());
        assertTrue(a.onChatMessage(chat(ChatMessageType.SPAM, "Your Vorkath kill count is: 101.")).isEmpty());
        assertTrue(a.onChatMessage(game("Your Scurrius kill count is: 5.")).isEmpty());
        assertTrue(sink.gains.isEmpty());
        assertEquals(0, core.sessionTotal("Vorkath"));
    }

    @Test void r02UnknownThenValidReauthenticationStartsFreshScopeAndZeroBaseline() {
        Sink sink = new Sink(); RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink);
        enter(a, 101);
        a.onChatMessage(game("Your Vorkath kill count is: 100."));
        a.onChatMessage(game("Your Vorkath kill count is: 101."));
        assertEquals(List.of("Vorkath:1:1"), sink.gains);
        assertFalse(a.onAuthenticatedLogin(-1));
        assertTrue(sink.gains.isEmpty());
        // Same hash returns: a NEW scope, never a resumed one, and never a guessed kill.
        assertTrue(a.onAuthenticatedLogin(101));
        assertEquals(3, sink.resets);
        BossKcDeltaTracker.Outcome first = a.onChatMessage(game("Your Vorkath kill count is: 103.")).orElseThrow();
        assertEquals(BossKcDeltaTracker.Status.BASELINED, first.status());
        assertEquals(0L, first.delta());
        assertEquals(0L, first.sessionTotal());
        assertTrue(sink.gains.isEmpty());       // 101 -> 103 across the gap is NOT counted
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Vorkath kill count is: 104."))));
        assertEquals(List.of("Vorkath:1:1"), sink.gains);  // total 1: old scope not carried over
        // Also via a full state transition rather than an in-place hash recovery.
        assertFalse(a.onAuthenticatedLogin(-1));
        a.onGameStateChanged(state(GameState.LOADING));
        enter(a, 101);
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 104."))));
        assertTrue(sink.gains.isEmpty());
    }

    @Test void r02UnknownHashBetweenAccountsAtoBtoAKeepsIsolation() {
        Sink sink = new Sink(); RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink);
        enter(a, 11);
        a.onChatMessage(game("Your Vorkath kill count is: 100."));
        assertFalse(a.onAuthenticatedLogin(-1));
        assertTrue(a.onAuthenticatedLogin(22));
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 900."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Vorkath kill count is: 901."))));
        assertEquals(List.of("Vorkath:1:1"), sink.gains);
        assertFalse(a.onAuthenticatedLogin(-1));
        assertTrue(a.onChatMessage(game("Your Vorkath kill count is: 902.")).isEmpty());
        assertTrue(a.onAuthenticatedLogin(11));
        assertTrue(sink.gains.isEmpty());
        // A's earlier 100 must not be diffed against anything; B's 901 must not leak.
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 101."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Vorkath kill count is: 102."))));
        assertEquals(List.of("Vorkath:1:1"), sink.gains);
        // resets: A login, -1, B login, -1, A login
        assertEquals(5, sink.resets);
    }

    @Test void r02UnknownHashIsIdempotentAndLogoutOrStopStayClean() {
        Sink sink = new Sink(); RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink);
        assertFalse(a.onAuthenticatedLogin(-1));        // before any state: no-op
        a.onGameStateChanged(state(GameState.LOGGED_IN));
        assertFalse(a.onAuthenticatedLogin(-1));        // LOGGED_IN but hash not yet known
        assertFalse(a.onAuthenticatedLogin(-1));
        assertEquals(0, sink.resets);
        assertTrue(a.onAuthenticatedLogin(5));
        a.onChatMessage(game("Your Zulrah kill count is: 10."));
        assertFalse(a.onAuthenticatedLogin(-1));
        assertFalse(a.onAuthenticatedLogin(-1));        // duplicate unknown: no second reset
        assertEquals(2, sink.resets);
        a.onGameStateChanged(state(GameState.LOGIN_SCREEN));
        a.stop();
        assertEquals(2, sink.resets);                   // nothing active, nothing to reset
        // Unknown hash while not in game (contract violation) still fails closed.
        enter(a, 5);
        a.onGameStateChanged(state(GameState.HOPPING));
        assertFalse(a.onAuthenticatedLogin(-1));
        assertEquals(4, sink.resets);
        enter(a, 5);
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Zulrah kill count is: 11."))));
        a.stop();
        assertEquals(6, sink.resets);
        assertTrue(a.onChatMessage(game("Your Zulrah kill count is: 12.")).isEmpty());
        assertTrue(sink.gains.isEmpty());
    }

    @Test void r02DuplicateRegressionAndSameLoginHopStillWorkAfterReauthentication() {
        Sink sink = new Sink(); RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink);
        enter(a, 77);
        assertFalse(a.onAuthenticatedLogin(-1));
        assertTrue(a.onAuthenticatedLogin(77));
        int resetsAfterReauth = sink.resets;
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 50."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Vorkath kill count is: 51."))));
        assertEquals(BossKcDeltaTracker.Status.UNCHANGED, status(a.onChatMessage(game("Your Vorkath kill count is: 51."))));
        assertTrue(a.onAuthenticatedLogin(77));         // duplicate valid callback keeps baseline
        assertEquals(BossKcDeltaTracker.Status.UNCHANGED, status(a.onChatMessage(game("Your Vorkath kill count is: 51."))));
        a.onGameStateChanged(state(GameState.HOPPING));
        assertTrue(a.onChatMessage(game("Your Vorkath kill count is: 900.")).isEmpty());
        a.onGameStateChanged(state(GameState.LOADING));
        enter(a, 77);
        assertEquals(resetsAfterReauth, sink.resets);   // verified same-login hop: no reset
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Vorkath kill count is: 51."))));
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Vorkath kill count is: 52."))));
        assertEquals(List.of("Vorkath:1:1", "Vorkath:1:2"), sink.gains);  // hop total preserved
        assertEquals(BossKcDeltaTracker.Status.COUNTER_REGRESSION, status(a.onChatMessage(game("Your Vorkath kill count is: 40."))));
        assertEquals(BossKcDeltaTracker.Status.QUARANTINED, status(a.onChatMessage(game("Your Vorkath kill count is: 60."))));
        assertEquals(List.of("Vorkath:1:1", "Vorkath:1:2"), sink.gains);
    }

    @Test void r02OnlyGameAndSpamMessagesCountAfterReauthentication() {
        Sink sink = new Sink(); RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(sink);
        enter(a, 31);
        assertFalse(a.onAuthenticatedLogin(-1));
        assertTrue(a.onAuthenticatedLogin(31));
        assertEquals(BossKcDeltaTracker.Status.BASELINED, status(a.onChatMessage(game("Your Obor kill count is: 7."))));
        for (ChatMessageType t : ChatMessageType.values()) {
            if (t == ChatMessageType.GAMEMESSAGE || t == ChatMessageType.SPAM) continue;
            assertTrue(a.onChatMessage(chat(t, "Your Obor kill count is: 999.")).isEmpty());
        }
        assertTrue(a.onChatMessage(null).isEmpty());
        assertTrue(sink.gains.isEmpty());
        assertEquals(BossKcDeltaTracker.Status.GAIN, status(a.onChatMessage(game("Your Obor kill count is: 8."))));
        assertEquals(List.of("Obor:1:1"), sink.gains);
    }

    @Test void r02NoAccountIdentifierLeaksAndStaleScopeReadsNoTotals() {
        final long hash = 8675309123L;
        final String digits = Long.toString(hash);
        List<String> seen = new ArrayList<>();
        RuneLiteBossKcAdapter.SessionsSink recording = new RuneLiteBossKcAdapter.SessionsSink() {
            @Override public void onCurrentLoginReset() { seen.add("reset"); }
            @Override public void onBossKcGain(String boss, long delta, long total) {
                seen.add(boss + ":" + delta + ":" + total);
            }
        };
        BossKcDeltaTracker core = new BossKcDeltaTracker();
        RuneLiteBossKcAdapter a = new RuneLiteBossKcAdapter(recording, core);
        enter(a, hash);
        seen.add(a.onChatMessage(game("Your Vorkath kill count is: 10.")).orElseThrow().toString());
        seen.add(a.onChatMessage(game("Your Vorkath kill count is: 12.")).orElseThrow().toString());
        assertFalse(a.onAuthenticatedLogin(-1));
        seen.add(a.onChatMessage(game("Your Vorkath kill count is: 13.")).toString());
        // A late sample stamped with the retired scope gets no totals back (R06 rule).
        BossKcDeltaTracker.Outcome stale = core.observe(new BossKcDeltaTracker.Observation(
                digits, "login-1", 1, "epoch-1", 1, "Vorkath", 99,
                BossKcDeltaTracker.Reliability.RELIABLE, 50L));
        assertEquals(BossKcDeltaTracker.Status.NO_SESSION, stale.status());
        assertEquals(0L, stale.sessionTotal());
        assertEquals(0L, stale.delta());
        assertTrue(a.onAuthenticatedLogin(hash));
        seen.add(a.onChatMessage(game("Your Vorkath kill count is: 13.")).orElseThrow().toString());
        assertEquals(List.of("reset", "reset", "reset"),
                seen.stream().filter(s -> s.equals("reset")).toList());
        assertTrue(seen.contains("Vorkath:2:2"));
        for (String s : seen) assertFalse(s.contains(digits));
    }
}
