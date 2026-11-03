package gg.feral.burrow.core;

import gg.feral.burrow.core.CompletedSessionHistory.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** BQA-902 SESS-04 / SESS-05 durability, isolation and fences (temp directories, synthetic summaries). */
final class BqaCompletedHistoryJUnitTest {
    static Path tmp() throws Exception { return Files.createTempDirectory("tpt-history"); }
    static Summary s(String id, long end) {
        return new Summary(CompletedSessionHistory.SCHEMA, id, end - 60_000, end, CompletedSessionHistory.CONFIRMED_LOGOUT,
            FeedState.OBSERVED, Map.of("Ranged", 100L), FeedState.OBSERVED, Map.of("Scurrius", 1L),
            Map.of(), List.of(new Drop(25766, "Infernal \"ashes\"", "Tormented Demon", 2)), FeedState.UNAVAILABLE, List.of(), List.of());
    }

    @Test void t20_elevenCompletedKeepNewestTenAndSurviveRestart() throws Exception {
        Path d = tmp();
        var h = CompletedSessionHistory.open(d, "account-A");
        assertEquals(LoadState.EMPTY_NEW, h.loadState());
        for (int i = 1; i <= 11; i++) assertTrue(h.commit(s(String.format("L%02d", i), i * 1000_000L)));
        var ids = h.newestFirst().stream().map(Summary::loginId).toList();
        assertEquals(List.of("L11", "L10", "L09", "L08", "L07", "L06", "L05", "L04", "L03", "L02"), ids);
        var reopened = CompletedSessionHistory.open(d, "account-A");
        assertEquals(LoadState.LOADED, reopened.loadState());
        assertEquals(ids, reopened.newestFirst().stream().map(Summary::loginId).toList());
        assertEquals(h.newestFirst(), reopened.newestFirst(), "exact round trip incl. quoted item name");
    }

    @Test void t18_duplicateTerminalAndNonTerminalNeverAdd() throws Exception {
        var h = CompletedSessionHistory.open(tmp(), "account-A");
        assertTrue(h.commit(s("L1", 5_000_000)));
        assertFalse(h.commit(s("L1", 6_000_000)), "same login twice");
        var interrupted = new Summary(1, "L2", 0, 10, "INTERRUPTED", FeedState.UNAVAILABLE, Map.of(), FeedState.UNAVAILABLE,
            Map.of(), Map.of(), List.of(), FeedState.UNAVAILABLE, List.of(), List.of());
        assertFalse(h.commit(interrupted), "account switch / stop / crash cannot create a completed login");
        assertEquals(1, h.newestFirst().size());
    }

    @Test void t15_accountsAreIsolatedAndFileNamesArePseudonyms() throws Exception {
        Path d = tmp();
        var a = CompletedSessionHistory.open(d, "-4242424242");
        var b = CompletedSessionHistory.open(d, "777");
        a.commit(s("A1", 2_000_000));
        b.commit(s("B1", 3_000_000));
        assertEquals(List.of("A1"), CompletedSessionHistory.open(d, "-4242424242").newestFirst().stream().map(Summary::loginId).toList());
        assertEquals(List.of("B1"), CompletedSessionHistory.open(d, "777").newestFirst().stream().map(Summary::loginId).toList());
        try (var files = Files.list(d)) {
            for (Path p : files.toList()) {
                assertFalse(p.getFileName().toString().contains("4242424242"));
                String body = Files.readString(p, StandardCharsets.UTF_8);
                assertFalse(body.contains("4242424242"), "raw account key never written");
            }
        }
    }

    @Test void t21_corruptFileFallsBackToBackupAndIsQuarantined() throws Exception {
        Path d = tmp();
        var h = CompletedSessionHistory.open(d, "acct");
        h.commit(s("L1", 1_000_000));
        h.commit(s("L2", 2_000_000)); // creates .bak holding [L1]
        Files.writeString(h.file(), "{\"schema\":1,\"sha256\":\"x\",\"sessions\":[", StandardCharsets.UTF_8); // power-loss truncation
        var r = CompletedSessionHistory.open(d, "acct");
        assertEquals(LoadState.RECOVERED_FROM_BACKUP, r.loadState());
        assertEquals(List.of("L1"), r.newestFirst().stream().map(Summary::loginId).toList(), "last valid state, never false zero");
        try (var files = Files.list(d)) { assertTrue(files.anyMatch(p -> p.toString().contains(".corrupt-1"))); }
    }

    @Test void t21_tamperedDigestAndOldSchemaAreRejectedNotTrusted() throws Exception {
        Path d = tmp();
        var h = CompletedSessionHistory.open(d, "acct");
        h.commit(s("L1", 1_000_000));
        String text = Files.readString(h.file(), StandardCharsets.UTF_8);
        Files.writeString(h.file(), text.replace("\"Ranged\":100", "\"Ranged\":999999"), StandardCharsets.UTF_8);
        var r = CompletedSessionHistory.open(d, "acct");
        assertEquals(LoadState.RECOVERED_EMPTY, r.loadState(), "no backup existed yet: reported as recovered-empty, not silently empty");
        Path d2 = tmp();
        var h2 = CompletedSessionHistory.open(d2, "acct");
        h2.commit(s("L1", 1_000_000));
        Files.writeString(h2.file(), Files.readString(h2.file()).replace("\"schema\":1,\"sha256\"", "\"schema\":0,\"sha256\""));
        assertNotEquals(LoadState.LOADED, CompletedSessionHistory.open(d2, "acct").loadState());
    }

    @Test void summaryRejectsInventedRows() {
        assertThrows(IllegalArgumentException.class, () -> new Summary(1, "x", 0, 1, CompletedSessionHistory.CONFIRMED_LOGOUT,
            FeedState.UNAVAILABLE, Map.of("Ranged", 5L), FeedState.OBSERVED, Map.of(), Map.of(), List.of(), FeedState.UNAVAILABLE, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Summary(1, "x", 0, 1, CompletedSessionHistory.CONFIRMED_LOGOUT,
            FeedState.OBSERVED, Map.of("Ranged", 0L), FeedState.OBSERVED, Map.of(), Map.of(), List.of(), FeedState.UNAVAILABLE, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Summary(1, "x", 10, 1, CompletedSessionHistory.CONFIRMED_LOGOUT,
            FeedState.OBSERVED, Map.of(), FeedState.OBSERVED, Map.of(), Map.of(), List.of(), FeedState.UNAVAILABLE, List.of(), List.of()));
    }

    @Test void serviceBindsOneAccountAtATime() throws Exception {
        Path d = tmp();
        var svc = new SessionHistoryService(d);
        svc.bind("A");
        assertTrue(svc.commit("A", s("A1", 1_000_000)));
        assertFalse(svc.commit("B", s("B1", 2_000_000)), "cannot write to an account that is not bound");
        svc.bind("B");
        assertTrue(svc.sessions().isEmpty(), "B never sees A");
        assertTrue(svc.commit("B", s("B1", 2_000_000)));
        svc.bind("A");
        assertEquals(List.of("A1"), svc.sessions().stream().map(Summary::loginId).toList());
        svc.unbind();
        assertTrue(svc.sessions().isEmpty());
        var none = new SessionHistoryService(null);
        none.bind("A");
        assertEquals("History storage unavailable", none.note());
        assertFalse(none.commit("A", s("A1", 1)));
    }

    @Test void t11_asyncFenceRejectsAtoBtoAStaleCallback() {
        var fence = new AsyncRequestFence();
        Object loginA1 = new Object(), loginB = new Object(), loginA2 = new Object();
        var ticket = fence.issue(loginA1, 42L);
        assertTrue(fence.admits(ticket, loginA1, 42L));
        assertFalse(fence.admits(ticket, loginB, 99L));
        assertFalse(fence.admits(ticket, loginA2, 42L), "same hash, different login scope");
        var t2 = fence.issue(loginA2, 42L);
        fence.revokeAll();
        assertFalse(fence.admits(t2, loginA2, 42L), "revoked at logout");
        assertFalse(fence.admits(fence.issue(null, 42L), null, 42L));
        assertFalse(fence.admits(fence.issue(loginA2, -1L), loginA2, -1L));
    }
}
