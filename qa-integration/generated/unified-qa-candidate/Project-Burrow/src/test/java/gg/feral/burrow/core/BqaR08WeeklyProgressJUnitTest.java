package gg.feral.burrow.core;

import gg.feral.burrow.core.WeeklyProgress.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** R08: weekly boundary, idempotent saves, restart, isolation, cross-week attribution, damage recovery. Synthetic. */
final class BqaR08WeeklyProgressJUnitTest {
    static final ZoneId LA = ZoneId.of("America/Los_Angeles");
    static long at(String local) { return LocalDateTime.parse(local).atZone(LA).toInstant().toEpochMilli(); }
    static Contribution xp(long n) { return new Contribution(Map.of("Ranged", n), Map.of(), 0, 0, Map.of(), Set.of()); }

    @Test void boundaryIsMondayMidnightLocalInclusive() {
        assertEquals(LocalDate.parse("2026-10-26"), WeeklyProgress.weekStart(at("2026-11-01T23:59:59"), LA), "Sunday night = previous Monday's week");
        assertEquals(LocalDate.parse("2026-11-02"), WeeklyProgress.weekStart(at("2026-11-02T00:00:00"), LA), "Monday 00:00 starts the new week");
        assertEquals(LocalDate.parse("2026-10-26"), WeeklyProgress.weekStart(at("2026-10-26T00:00:00"), LA));
        // The same instant can be a different week in another zone: the player's zone is the boundary.
        long instant = at("2026-11-01T20:00:00");
        assertEquals(LocalDate.parse("2026-11-02"), WeeklyProgress.weekStart(instant, ZoneId.of("Asia/Tokyo")));
    }

    @Test void repeatedSavesReplaceNeverAdd() throws Exception {
        var w = WeeklyProgress.open(Files.createTempDirectory("tpt-week"), "acct");
        var wk = LocalDate.parse("2026-10-26");
        w.save("login-1", Map.of(wk, xp(100)));
        w.save("login-1", Map.of(wk, xp(100)));
        w.save("login-1", Map.of(wk, xp(250)));
        assertEquals(250, w.totals(wk, null, null).xp());
        w.save("login-2", Map.of(wk, xp(50)));
        assertEquals(300, w.totals(wk, null, null).xp());
        assertEquals(2, w.totals(wk, null, null).logins());
        // Live contribution substitutes the saved entry of the running login (no double count).
        assertEquals(350, w.totals(wk, "login-2", xp(100)).xp());
    }

    @Test void restartKeepsSavedLoginsAndAccountsStayIsolated() throws Exception {
        Path d = Files.createTempDirectory("tpt-week");
        var wk = LocalDate.parse("2026-10-26");
        WeeklyProgress.open(d, "-555").save("a1", Map.of(wk, xp(1000)));
        WeeklyProgress.open(d, "777").save("b1", Map.of(wk, xp(7)));
        assertEquals(1000, WeeklyProgress.open(d, "-555").totals(wk, null, null).xp());
        assertEquals(7, WeeklyProgress.open(d, "777").totals(wk, null, null).xp());
        try (var files = Files.list(d)) {
            for (Path p : files.toList()) assertFalse(Files.readString(p, StandardCharsets.UTF_8).contains("555"), p.toString());
        }
    }

    @Test void loginCrossingSundayMidnightSplitsByEventTime() {
        var scope = new gg.feral.burrow.ui.BurrowPanel.SessionScope("acct", "login-x");
        var o = gg.feral.burrow.ui.BurrowPanel.SessionOrigin.VERIFIED_BRIDGE;
        var l = new gg.feral.burrow.ui.LiveSessionLedger(); l.setZone(LA); l.start(scope, o, at("2026-11-01T23:00:00"));
        l.record(new gg.feral.burrow.ui.BurrowPanel.SessionXpGain(scope, "e1", o, "Ranged", 400, "x"), at("2026-11-01T23:59:00"));
        l.record(new gg.feral.burrow.ui.BurrowPanel.SessionXpGain(scope, "e2", o, "Ranged", 60, "x"), at("2026-11-02T00:01:00"));
        l.record(new gg.feral.burrow.ui.BurrowPanel.SessionBossKcGain(scope, "e3", o, "Vorkath", 1, "kc"), at("2026-11-02T00:05:00"));
        var c = l.weeklyContributions();
        assertEquals(400L, c.get(LocalDate.parse("2026-10-26")).xpBySkill().get("Ranged").longValue());
        assertEquals(60L, c.get(LocalDate.parse("2026-11-02")).xpBySkill().get("Ranged").longValue());
        assertTrue(c.get(LocalDate.parse("2026-10-26")).verifiedKcByBoss().isEmpty());
        assertEquals(1L, c.get(LocalDate.parse("2026-11-02")).verifiedKcByBoss().get("Vorkath").longValue());
    }

    @Test void pausedForeignAndDuplicateEventsDoNotCount() {
        var scope = new gg.feral.burrow.ui.BurrowPanel.SessionScope("acct", "login-y");
        var o = gg.feral.burrow.ui.BurrowPanel.SessionOrigin.VERIFIED_BRIDGE;
        var l = new gg.feral.burrow.ui.LiveSessionLedger(); l.setZone(LA); l.start(scope, o, 0);
        long t = at("2026-10-28T12:00:00");
        var e = new gg.feral.burrow.ui.BurrowPanel.SessionXpGain(scope, "same", o, "Magic", 10, "x");
        l.record(e, t); l.record(e, t);
        l.record(new gg.feral.burrow.ui.BurrowPanel.SessionPause(scope, "p", o), t);
        l.record(new gg.feral.burrow.ui.BurrowPanel.SessionXpGain(scope, "gap", o, "Magic", 999, "x"), t);
        l.record(new gg.feral.burrow.ui.BurrowPanel.SessionXpGain(new gg.feral.burrow.ui.BurrowPanel.SessionScope("b", "z"), "f", o, "Magic", 5, "x"), t);
        assertEquals(10L, l.weeklyContributions().get(LocalDate.parse("2026-10-26")).xpBySkill().get("Magic").longValue());
    }

    @Test void untrackedCountersAreNotTrackedNeverZero() throws Exception {
        var w = WeeklyProgress.open(Files.createTempDirectory("tpt-week"), "acct");
        var wk = LocalDate.parse("2026-10-26");
        w.save("l1", Map.of(wk, xp(5)));
        var t = w.totals(wk, null, null);
        assertTrue(t.counter("level-ups").isEmpty(), "no login tracked it: not tracked, not 0");
        w.save("l2", Map.of(wk, new Contribution(Map.of(), Map.of(), 0, 0, Map.of("level-ups", 2L), Set.of("level-ups"))));
        assertEquals(2, w.totals(wk, null, null).counter("level-ups").getAsLong());
        assertThrows(IllegalArgumentException.class, () -> new Contribution(Map.of(), Map.of(), 0, 0, Map.of("chests", 1L), Set.of()));
    }

    @Test void damagedFileRecoversFromBackupAndOldWeeksAreTrimmed() throws Exception {
        Path d = Files.createTempDirectory("tpt-week");
        var w = WeeklyProgress.open(d, "acct");
        var wk = LocalDate.parse("2026-10-26");
        w.save("l1", Map.of(wk, xp(10)));
        w.save("l2", Map.of(wk, xp(20)));          // .bak holds only l1
        Files.writeString(w.file(), "{\"schema\":1,", StandardCharsets.UTF_8);
        var r = WeeklyProgress.open(d, "acct");
        assertEquals("recovered from backup", r.loadNote());
        assertEquals(10, r.totals(wk, null, null).xp());
        for (int i = 0; i < 12; i++) r.save("l" + i, Map.of(wk.plusWeeks(i), xp(1)));
        assertEquals(WeeklyProgress.KEEP_WEEKS, r.weeks().size());
    }
}
