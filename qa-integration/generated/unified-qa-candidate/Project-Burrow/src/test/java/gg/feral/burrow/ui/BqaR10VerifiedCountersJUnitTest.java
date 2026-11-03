package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import gg.feral.burrow.ui.BurrowPanel.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** R10: level-ups, collection unlocks, confirmed chests — counted only from their verified sources. Synthetic events. */
final class BqaR10VerifiedCountersJUnitTest {
    static final SessionOrigin O = SessionOrigin.VERIFIED_BRIDGE;
    static final AtomicInteger N = new AtomicInteger();
    static String id() { return "r10-" + N.incrementAndGet(); }
    static final ZoneId LA = ZoneId.of("America/Los_Angeles");
    static long at(String s) { return LocalDateTime.parse(s).atZone(LA).toInstant().toEpochMilli(); }

    @Test void xpTableMatchesKnownOsrsValues() {
        assertEquals(0, VerifiedCounters.xpForLevel(1));
        assertEquals(83, VerifiedCounters.xpForLevel(2));
        assertEquals(174, VerifiedCounters.xpForLevel(3));
        assertEquals(1_154, VerifiedCounters.xpForLevel(10));
        assertEquals(101_333, VerifiedCounters.xpForLevel(50));
        assertEquals(6_517_253, VerifiedCounters.xpForLevel(92));
        assertEquals(13_034_431, VerifiedCounters.xpForLevel(99));
        assertEquals(98, VerifiedCounters.levelForXp(13_034_430));
        assertEquals(99, VerifiedCounters.levelForXp(200_000_000));
        assertEquals(1, VerifiedCounters.levelsGained(13_034_000, 13_034_431));
        assertEquals(0, VerifiedCounters.levelsGained(13_034_431, 14_000_000), "no levels beyond 99");
        assertEquals(2, VerifiedCounters.levelsGained(80, 200), "1 -> 3 in one gain");
        assertEquals(0, VerifiedCounters.levelsGained(500, 400));
    }

    @Test void collectionMessageParserIsExact() {
        assertEquals(Optional.of("Bottomless milk bucket (empty)"),
            VerifiedCounters.collectionItem("New item added to your collection log: <col=ef1020>Bottomless milk bucket (empty)</col>"));
        assertTrue(VerifiedCounters.collectionItem("Player: New item added to your collection log: Mooleta").isEmpty());
        assertTrue(VerifiedCounters.collectionItem("New item added to your collection log: ").isEmpty());
        assertTrue(VerifiedCounters.collectionItem(null).isEmpty());
    }

    @Test void countersAreNotTrackedUntilTheirSourceIsVerified() {
        var scope = new SessionScope("acct", "login");
        var l = new LiveSessionLedger(); l.setZone(LA); l.start(scope, O, 0);
        long t = at("2026-10-28T12:00:00");
        assertTrue(l.tracks(VerifiedCounters.CHESTS), "verified KC adapter covers every verified login");
        assertFalse(l.tracks(VerifiedCounters.LEVEL_UPS), "XP feed not available yet");
        assertFalse(l.recordLevelUp("Ranged", 1, 98, t), "no level-up without the verified XP feed");
        assertFalse(l.recordCollectionUnlock("Mooleta", t), "no unlock without the chat-notification source");
        l.record(new SessionFeedStatus(scope, id(), O, SessionFeed.XP, FeedStatus.AVAILABLE), t);
        assertTrue(l.tracks(VerifiedCounters.LEVEL_UPS));
        assertTrue(l.recordLevelUp("Ranged", 1, 98, t));
        l.trackCounter(VerifiedCounters.COLLECTION);
        assertTrue(l.recordCollectionUnlock("Mooleta", t + 1));
        l.record(new SessionBossKcGain(scope, id(), O, "Barrows", 1, "chest count"), t + 2);
        l.record(new SessionBossKcGain(scope, id(), O, "Vorkath", 1, "kc"), t + 3);
        var c = l.weeklyContribution(WeeklyProgress.weekStart(t, LA));
        assertEquals(1L, c.counters().get(VerifiedCounters.LEVEL_UPS).longValue());
        assertEquals(1L, c.counters().get(VerifiedCounters.COLLECTION).longValue());
        assertEquals(1L, c.counters().get(VerifiedCounters.CHESTS).longValue(), "Barrows chest only; Vorkath KC is not a chest");
        assertEquals("Collection log: Mooleta", l.recent().get(2).text());
        assertEquals("Ranged level 98", l.recent().get(3).text());
    }

    @Test void pausedLoginCountsNothingAndNewLoginStartsClean() {
        var scope = new SessionScope("acct", "login");
        var l = new LiveSessionLedger(); l.start(scope, O, 0);
        l.record(new SessionFeedStatus(scope, id(), O, SessionFeed.XP, FeedStatus.AVAILABLE), 1);
        l.trackCounter(VerifiedCounters.COLLECTION);
        l.record(new SessionPause(scope, id(), O), 2);
        assertFalse(l.recordLevelUp("Magic", 1, 90, 3));
        assertFalse(l.recordCollectionUnlock("Vorki", 4));
        l.start(new SessionScope("acct", "login-2"), O, 5);
        assertFalse(l.tracks(VerifiedCounters.LEVEL_UPS));
        assertFalse(l.tracks(VerifiedCounters.COLLECTION));
    }

    @Test void weeklyCardShowsCountsOnlyForTrackedSources() throws Exception {
        var t = new WeeklyProgress.Totals(LocalDate.parse("2026-10-26"), 1000, 2, 3, 1,
            Map.of(VerifiedCounters.LEVEL_UPS, 2L, VerifiedCounters.CHESTS, 0L), 1);
        SwingUtilities.invokeAndWait(() -> {
            var p = new BurrowPanel(new BurrowModel());
            p.setWeeklyProgress(t, "x");
            var lines = p.weeklyLinesForTest();
            assertTrue(lines.contains("Level-ups: 2"));
            assertTrue(lines.contains("Confirmed chests (Barrows): 0"), "tracked source, genuinely none");
            assertTrue(lines.contains("Collection unlocks: not tracked"));
        });
    }
}
