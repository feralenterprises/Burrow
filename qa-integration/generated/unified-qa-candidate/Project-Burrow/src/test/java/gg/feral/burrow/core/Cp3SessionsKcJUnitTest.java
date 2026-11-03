package gg.feral.burrow.core;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CP-3 acceptance fixtures against the EXISTING session/KC core (KC-X01, SESSION-X01, TD Collection Log KC).
 * Pure Java; the RuneLite event wiring (hop, LOADING, logout) stays covered by the host JUnit tests and native QA.
 */
public final class Cp3SessionsKcJUnitTest {
    private static BossKcDeltaTracker.Observation obs(long seq, long kc) {
        return new BossKcDeltaTracker.Observation("acct", "login-1", 1, "epoch-1", 1, "Tormented Demons", seq,
            BossKcDeltaTracker.Reliability.RELIABLE, kc);
    }

    /** KC-X01: first absolute KC is a baseline (0 gained); a real +1 counts once; replays never count. */
    @Test void firstKcBaselinesThenOnlyProvenIncreasesCountOnce() {
        var t = new BossKcDeltaTracker();
        assertTrue(t.openSession("acct", "login-1", 1));
        assertTrue(t.openEpoch("acct", "login-1", 1, "epoch-1", 1));
        assertEquals(BossKcDeltaTracker.Status.BASELINED, t.observe(obs(1, 621)).status());
        assertEquals(0, t.sessionTotal("Tormented Demons"), "lifetime 621 is never session kills");
        var gain = t.observe(obs(2, 622));
        assertEquals(BossKcDeltaTracker.Status.GAIN, gain.status());
        assertEquals(1, gain.delta());
        assertEquals(BossKcDeltaTracker.Status.OLD_OR_REPLAYED, t.observe(obs(2, 622)).status(), "same sample twice");
        assertEquals(BossKcDeltaTracker.Status.UNCHANGED, t.observe(obs(3, 622)).status());
        assertEquals(1, t.sessionTotal("Tormented Demons"));
        var unknown = new BossKcDeltaTracker.Observation("acct", "login-1", 1, "epoch-1", 1, "Tormented Demons", 4,
            BossKcDeltaTracker.Reliability.UNKNOWN, null);
        assertEquals(BossKcDeltaTracker.Status.UNKNOWN_KC, t.observe(unknown).status());
        assertEquals(BossKcDeltaTracker.Status.BASELINED, t.observe(obs(5, 640)).status(), "an unverifiable gap is never bridged");
        assertEquals(1, t.sessionTotal("Tormented Demons"));
    }

    @Test void otherAccountOrLoginNeverFeedsThisSession() {
        var t = new BossKcDeltaTracker();
        t.openSession("acct", "login-1", 1); t.openEpoch("acct", "login-1", 1, "epoch-1", 1);
        var other = new BossKcDeltaTracker.Observation("other", "login-1", 1, "epoch-1", 1, "Tormented Demons", 1,
            BossKcDeltaTracker.Reliability.RELIABLE, 5L);
        assertEquals(BossKcDeltaTracker.Status.WRONG_IDENTITY, t.observe(other).status());
    }

    private static CompletedSessionHistory.Summary login(String id, long end) {
        return new CompletedSessionHistory.Summary(CompletedSessionHistory.SCHEMA, id, end - 1000, end,
            CompletedSessionHistory.CONFIRMED_LOGOUT,
            CompletedSessionHistory.FeedState.OBSERVED, Map.of("Cooking", 100L),
            CompletedSessionHistory.FeedState.OBSERVED, Map.of(),
            Map.of(), List.of(), CompletedSessionHistory.FeedState.UNAVAILABLE, List.of(), List.of());
    }

    /** SESSION-X01: 11 genuine logins keep the newest 10; duplicates and restarts never add rows; accounts isolated. */
    @Test void elevenCompletedLoginsKeepNewestTenAcrossRestartPerAccount() throws Exception {
        Path dir = Files.createTempDirectory("cp3-history");
        var a = CompletedSessionHistory.open(dir, "account-a");
        for (int i = 1; i <= 11; i++) assertTrue(a.commit(login("login-" + i, 1_000_000L * i)));
        assertFalse(a.commit(login("login-11", 11_000_000L)), "same login archived once");
        var notTerminal = new CompletedSessionHistory.Summary(CompletedSessionHistory.SCHEMA, "hop-1", 1, 2, "HOPPING",
            CompletedSessionHistory.FeedState.OBSERVED, Map.of(), CompletedSessionHistory.FeedState.OBSERVED, Map.of(),
            Map.of(), List.of(), CompletedSessionHistory.FeedState.UNAVAILABLE, List.of(), List.of());
        assertFalse(a.commit(notTerminal), "a world hop is not a completed login");
        assertEquals(10, a.newestFirst().size());
        assertEquals("login-11", a.newestFirst().get(0).loginId());
        assertEquals("login-2", a.newestFirst().get(9).loginId());
        var restarted = CompletedSessionHistory.open(dir, "account-a");
        assertEquals(a.newestFirst(), restarted.newestFirst(), "restart reloads exactly the same ten");
        var b = CompletedSessionHistory.open(dir, "account-b");
        assertTrue(b.newestFirst().isEmpty(), "no cross-account bleed");
    }

    /** Owner note (2026-11-01): the TD Collection Log page gives lifetime KC; singular/plural headers both map. */
    @Test void tormentedDemonCollectionLogKcMapsToTheTrackerEncounter() {
        for (String page : List.of("Tormented Demons", "Tormented Demon"))
            for (String header : List.of("Tormented Demon kills: 621", "Tormented Demons kills: 621", "Kills: 621")) {
                var o = CollectionLogParser.parse(List.of(page, header), List.of()).orElseThrow();
                assertEquals(Map.of("Tormented Demons", 621L), o.killCounts(), page + " / " + header);
            }
        assertTrue(CollectionLogParser.parse(List.of("Tormented Demons", "Kills: ?"), List.of()).orElseThrow().killCounts().isEmpty(),
            "unparseable KC is unknown, never zero");
    }
}
