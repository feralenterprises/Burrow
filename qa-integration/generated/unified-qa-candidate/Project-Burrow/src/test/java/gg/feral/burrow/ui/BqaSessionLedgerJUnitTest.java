package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.CompletedSessionHistory;
import gg.feral.burrow.ui.BurrowPanel.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** BQA-902 Batch B: panel reconstruction from the current-login ledger (synthetic events, not native). */
final class BqaSessionLedgerJUnitTest {
    static final SessionOrigin O = SessionOrigin.VERIFIED_BRIDGE;
    static final AtomicInteger IDS = new AtomicInteger();
    static String id() { return "t-" + IDS.incrementAndGet(); }

    /** Emits a fixed login: +500 XP, hop pause/resume, +60 XP, 2 KC, 1 loot estimate (2 stacks), 1 drop. */
    static List<SessionEvent> login(SessionScope s, boolean endPaused) {
        List<SessionEvent> e = new ArrayList<>();
        e.add(new SessionFeedStatus(s, id(), O, SessionFeed.XP, FeedStatus.AVAILABLE));
        e.add(new SessionXpGain(s, id(), O, "Ranged", 500, "StatChanged"));
        e.add(new SessionPause(s, id(), O));
        e.add(new SessionXpGain(s, id(), O, "Ranged", 100, "gap XP must be ignored while paused"));
        e.add(new SessionResume(s, id(), O));
        e.add(new SessionFeedStatus(s, id(), O, SessionFeed.XP, FeedStatus.AVAILABLE));
        e.add(new SessionXpGain(s, id(), O, "Ranged", 60, "StatChanged"));
        e.add(new SessionBossKcGain(s, id(), O, "Scurrius", 1, "kc delta"));
        e.add(new SessionBossKcGain(s, id(), O, "Scurrius", 1, "kc delta"));
        e.add(new SessionLootEncounter(s, id(), O, "Tormented Demon", "LootReceived estimate"));
        e.add(new SessionDrop(s, id(), O, 25766, "Infernal ashes", 1, "Tormented Demon", "NpcLootReceived"));
        e.add(new SessionDrop(s, id(), O, 25766, "Infernal ashes", 2, "Tormented Demon", "NpcLootReceived"));
        if (endPaused) e.add(new SessionPause(s, id(), O));
        return e;
    }

    static BurrowPanel panelWith(List<SessionEvent> events) throws Exception {
        AtomicReference<BurrowPanel> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            var p = new BurrowPanel(new BurrowModel());
            for (var ev : events) p.ingestSessionEvent(ev);
            ref.set(p);
        });
        return ref.get();
    }

    @Test void t16_recreatedPanelShowsIdenticalTotalsWithoutReplayIncrement() throws Exception {
        var scope = new SessionScope("acct-opaque", "login-1");
        var ledger = new LiveSessionLedger();
        ledger.start(scope, O, 1000);
        var events = login(scope, false);
        List<SessionEvent> withStart = new ArrayList<>();
        withStart.add(new SessionStart(scope, id(), O));
        withStart.addAll(events);
        for (var e : events) ledger.record(e);
        var original = panelWith(withStart);
        var rebuilt = panelWith(ledger.replay(BqaSessionLedgerJUnitTest::id));
        var a = original.sessionReadout(); var b = rebuilt.sessionReadout();
        assertEquals(560L, a.totalXp(), "hop gap uncredited: 500 + 60");
        assertEquals(a.totalXp(), b.totalXp());
        assertEquals(a.xpBySkill(), b.xpBySkill());
        assertEquals(Map.of("Scurrius", 2L), b.kcByBoss());
        assertEquals(a.kcByBoss(), b.kcByBoss());
        assertEquals(Map.of("Tormented Demon", 1L), rebuilt.lootConfirmedEncounters(), "two stacks = one estimate, never KC");
        assertEquals(original.lootConfirmedEncounters(), rebuilt.lootConfirmedEncounters());
        assertEquals(a.drops(), b.drops());
        assertEquals(3L, b.drops().get(0).quantity());
        assertEquals(SessionState.ACTIVE, b.state());
        // Replaying again (sidebar reopened twice) never mutates the ledger.
        var again = panelWith(ledger.replay(BqaSessionLedgerJUnitTest::id));
        assertEquals(560L, again.sessionReadout().totalXp());
        assertFalse(b.kcByBoss().containsKey("Tormented Demon"), "TD loot never becomes verified KC");
    }

    @Test void t17_recreateWhilePausedShowsSuspendedThenResumes() throws Exception {
        var scope = new SessionScope("acct-opaque", "login-2");
        var ledger = new LiveSessionLedger();
        ledger.start(scope, O, 0);
        for (var e : login(scope, true)) ledger.record(e);
        assertTrue(ledger.paused());
        var p = panelWith(ledger.replay(BqaSessionLedgerJUnitTest::id));
        assertEquals(SessionState.SUSPENDED, p.sessionReadout().state());
        assertEquals(560L, p.sessionReadout().totalXp());
        SwingUtilities.invokeAndWait(() -> assertTrue(p.ingestSessionEvent(new SessionResume(scope, id(), O))));
        assertEquals(SessionState.ACTIVE, p.sessionReadout().state());
    }

    @Test void wrongScopeDuplicateIdsAndLifecycleEventsAreNotRecorded() {
        var scope = new SessionScope("acct-opaque", "login-3");
        var other = new SessionScope("acct-other", "login-x");
        var ledger = new LiveSessionLedger();
        ledger.start(scope, O, 0);
        var xp = new SessionXpGain(scope, "dup", O, "Magic", 10, "x");
        assertTrue(ledger.record(xp));
        assertFalse(ledger.record(xp), "same id twice");
        assertFalse(ledger.record(new SessionXpGain(other, id(), O, "Magic", 10, "x")), "other account/login");
        assertFalse(ledger.record(new SessionXpGain(scope, id(), SessionOrigin.SYNTHETIC_FIXTURE, "Magic", 10, "x")));
        assertFalse(ledger.record(new SessionStop(scope, id(), O)));
        var s = ledger.completedSummary(60_000, CompletedSessionHistory.CONFIRMED_LOGOUT);
        assertEquals(CompletedSessionHistory.FeedState.OBSERVED, s.xpFeed());
        assertEquals(10L, s.totalXp());
        ledger.clear();
        assertNull(ledger.scope());
        assertTrue(ledger.replay(BqaSessionLedgerJUnitTest::id).isEmpty());
        assertThrows(IllegalStateException.class, () -> ledger.completedSummary(1, CompletedSessionHistory.CONFIRMED_LOGOUT));
    }

    @Test void summaryMarksUnavailableFeedsAndEstimatesNotZeroOrKc() {
        var scope = new SessionScope("acct-opaque", "login-4");
        var ledger = new LiveSessionLedger();
        ledger.start(scope, O, 1_000);
        ledger.record(new SessionFeedStatus(scope, id(), O, SessionFeed.XP, FeedStatus.UNAVAILABLE));
        ledger.record(new SessionLootEncounter(scope, id(), O, "Tormented Demon", "estimate"));
        var s = ledger.completedSummary(5_000, CompletedSessionHistory.CONFIRMED_LOGOUT);
        assertEquals(CompletedSessionHistory.FeedState.UNAVAILABLE, s.xpFeed());
        assertTrue(s.xpBySkill().isEmpty());
        assertEquals(CompletedSessionHistory.FeedState.UNAVAILABLE, s.kcFeed());
        assertTrue(s.verifiedKcByBoss().isEmpty());
        assertEquals(Map.of("Tormented Demon", 1L), s.estimatedLootEncounters());
        assertEquals(CompletedSessionHistory.FeedState.UNAVAILABLE, s.suppliesFeed());
        assertTrue(s.uncertainty().stream().anyMatch(x -> x.contains("not KC")));
        assertEquals("login-4", s.loginId());
        assertEquals("XP n/a · no verified KC · 0 drops", BurrowPanel.historyLine(s));
    }

    @Test void historyCardReplacesNotBuiltYetAndGrowsPanel() throws Exception {
        var scope = new SessionScope("acct-opaque", "login-5");
        var ledger = new LiveSessionLedger();
        ledger.start(scope, O, 0);
        ledger.record(new SessionXpGain(scope, id(), O, "Ranged", 1234, "x"));
        var s = ledger.completedSummary(3_600_000, CompletedSessionHistory.CONFIRMED_LOGOUT);
        AtomicReference<int[]> h = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            var p = new BurrowPanel(new BurrowModel());
            p.setSize(225, 800);
            int before = p.getPreferredSize().height;
            p.setCompletedSessions(List.of(s), "This account · newest first");
            int after = p.getPreferredSize().height;
            assertEquals(1, p.completedSessionsForTest().size());
            h.set(new int[]{before, after});
        });
        assertEquals("1,234 XP · no verified KC · 0 drops", BurrowPanel.historyLine(s));
        assertTrue(BurrowPanel.historyWhen(s.endMillis(), s.durationMillis()).endsWith("· 1h 0m"));
    }
}
