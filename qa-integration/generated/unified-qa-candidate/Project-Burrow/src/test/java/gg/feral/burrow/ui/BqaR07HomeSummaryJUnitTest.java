package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.ui.BurrowPanel.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** R07: Home shows the current login from the same accepted events as Sessions. Synthetic events. */
final class BqaR07HomeSummaryJUnitTest {
    static final SessionOrigin O = SessionOrigin.VERIFIED_BRIDGE;
    static final AtomicInteger N = new AtomicInteger();
    static String id() { return "r7-" + N.incrementAndGet(); }

    static List<SessionEvent> login(SessionScope s) {
        return List.of(
            new SessionFeedStatus(s, id(), O, SessionFeed.XP, FeedStatus.AVAILABLE),
            new SessionXpGain(s, id(), O, "Ranged", 300, "x"),
            new SessionXpGain(s, id(), O, "Ranged", 200, "x"),
            new SessionBossKcGain(s, id(), O, "Vorkath", 1, "kc"),
            new SessionLootEncounter(s, id(), O, "Tormented Demon", "est"),
            new SessionDrop(s, id(), O, 25766, "Infernal ashes", 2, "Tormented Demon", "npc"),
            new SessionXpGain(s, id(), O, "Prayer", 40, "x"));
    }

    @Test void homeSignalsEqualSessionsAndSurviveSidebarRebuild() throws Exception {
        var scope = new SessionScope("acct", "login");
        var ledger = new LiveSessionLedger(); ledger.start(scope, O, 0);
        long t = 1_000_000;
        for (var e : login(scope)) ledger.record(e, t += 10_000);
        AtomicReference<List<String>> live = new AtomicReference<>(), rebuilt = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            var p = new BurrowPanel(new BurrowModel());
            p.ingestSessionEvent(new SessionStart(scope, id(), O));
            for (var e : login(scope)) p.ingestSessionEvent(e);
            live.set(p.homeSignalsForTest());
            var r = p.sessionReadout();
            assertEquals("+" + String.format(Locale.US, "%,d", r.totalXp()), p.homeSignalsForTest().get(0), "Home XP = Sessions XP");
            assertEquals("+1", p.homeSignalsForTest().get(1));
            var q = new BurrowPanel(new BurrowModel());
            q.restoreSessionSnapshot(ledger.replay(BqaR07HomeSummaryJUnitTest::id));
            q.setRecentActivity(ledger.recent());
            rebuilt.set(q.homeSignalsForTest());
            assertEquals(5, q.recentActivityForTest().size(), "Prayer, ashes, estimate, KC, merged Ranged");
        });
        assertEquals(List.of("+540", "+1", "2 drop items · ~1 loot kill"), live.get());
        assertEquals(live.get(), rebuilt.get(), "sidebar rebuild shows identical Home numbers");
        assertEquals(540, ledger.totalXp()); assertEquals(1, ledger.totalVerifiedKc());
        assertEquals(2, ledger.dropItemCount()); assertEquals(1, ledger.lootEstimates());
    }

    @Test void recentActivityIsNewestFirstMergesXpAndLabelsEstimates() {
        var scope = new SessionScope("acct", "login");
        var ledger = new LiveSessionLedger(); ledger.start(scope, O, 0);
        long t = 1_000_000;
        for (var e : login(scope)) ledger.record(e, t += 10_000);
        var r = ledger.recent();
        assertEquals("+40 Prayer XP", r.get(0).text());
        assertEquals("Infernal ashes ×2", r.get(1).text());
        assertEquals(25766, r.get(1).itemId(), "drop row uses the real item id for its sprite");
        assertEquals("Tormented Demon", r.get(1).detail());
        assertEquals("Tormented Demon kill (estimate)", r.get(2).text());
        assertEquals("Loot Tracker · not KC", r.get(2).detail());
        assertEquals("Vorkath KC +1", r.get(3).text());
        assertEquals("+500 Ranged XP", r.get(4).text(), "two Ranged gains 10 s apart merged");
        // Gains far apart are separate rows; cap is enforced.
        ledger.record(new SessionXpGain(scope, id(), O, "Prayer", 10, "x"), t + LiveSessionLedger.XP_MERGE_MILLIS + 1);
        assertEquals("+10 Prayer XP", ledger.recent().get(0).text());
        for (int i = 0; i < 40; i++) ledger.record(new SessionBossKcGain(scope, id(), O, "Vorkath", 1, "kc"), t + 1_000_000 + i);
        assertEquals(LiveSessionLedger.RECENT_CAP, ledger.recent().size());
    }

    @Test void pausedOrForeignEventsNeverReachRecentActivity() {
        SessionScope scope = new SessionScope("acct", "login"), other = new SessionScope("b", "l2");
        var ledger = new LiveSessionLedger(); ledger.start(scope, O, 0);
        ledger.record(new SessionPause(scope, id(), O), 1);
        ledger.record(new SessionXpGain(scope, id(), O, "Ranged", 100, "gap"), 2);
        ledger.record(new SessionXpGain(other, id(), O, "Ranged", 100, "other account"), 3);
        assertTrue(ledger.recent().isEmpty());
        ledger.start(new SessionScope("acct", "login-2"), O, 4);
        assertTrue(ledger.recent().isEmpty(), "new login starts empty");
    }

    @Test void homeGrowsForRecentCardAndAgoLabels() throws Exception {
        assertEquals("now", BurrowPanel.ago(60_000, 30_000));
        assertEquals("2m ago", BurrowPanel.ago(200_000, 60_000));
        assertEquals("1h ago", BurrowPanel.ago(4_000_000, 0));
        SwingUtilities.invokeAndWait(() -> {
            var p = new BurrowPanel(new BurrowModel());
            int empty = p.homeBottom();
            var scope = new SessionScope("acct", "login");
            var ledger = new LiveSessionLedger(); ledger.start(scope, O, 0);
            for (var e : login(scope)) ledger.record(e, 5);
            p.setRecentActivity(ledger.recent());
            assertTrue(p.homeBottom() > empty);
            p.setSize(225, p.getPreferredSize().height);
            var img = new java.awt.image.BufferedImage(225, p.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g = img.createGraphics(); p.paint(g); g.dispose();
        });
    }
}
