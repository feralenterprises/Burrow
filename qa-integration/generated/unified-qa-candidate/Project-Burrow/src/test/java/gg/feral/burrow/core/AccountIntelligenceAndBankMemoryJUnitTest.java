package gg.feral.burrow.core;

import gg.feral.burrow.pathfinder.SuggestionEngine;
import gg.feral.burrow.ui.BankMemoryViewModel;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Offline regression for first-run account intelligence + Bank Memory fences.
 * Not native RuneLite proof. Does not authorize Best in Bank from historical memory.
 */
public final class AccountIntelligenceAndBankMemoryJUnitTest {
    private static final long ACCOUNT_A = 0xA11CEL;
    private static final long ACCOUNT_B = 0xB0BCA7L;
    private static final long DAY = PathfinderAccountArchive.REMINDER_AFTER_MS;

    @Test
    void accountArchiveIsolatesAFromBAcrossRestart() {
        MapStore store = new MapStore();
        PathfinderAccountArchive archive = new PathfinderAccountArchive(store::get, store::put);
        long t0 = 1_700_000_000_000L;
        assertTrue(archive.saveInitialOnce(ACCOUNT_A, t0, snapshot("Attack", 60, "Cook's Assistant", SuggestionEngine.Quest.FINISHED), true, true));
        assertTrue(archive.rememberBank(ACCOUNT_A, t0 + 1_000, openStableBank(t0 + 500, Map.of(995, 1000)), true));

        // Simulate restart with same ConfigManager-backed map.
        PathfinderAccountArchive reloaded = new PathfinderAccountArchive(store::get, store::put);
        assertTrue(reloaded.readInitial(ACCOUNT_A).isPresent());
        assertTrue(reloaded.readBank(ACCOUNT_A).isPresent());
        assertTrue(reloaded.readInitial(ACCOUNT_B).isEmpty());
        assertTrue(reloaded.readBank(ACCOUNT_B).isEmpty());

        assertTrue(reloaded.saveInitialOnce(ACCOUNT_B, t0 + 2_000, snapshot("Attack", 40, "Cook's Assistant", SuggestionEngine.Quest.NOT_STARTED), true, true));
        assertNotEquals(
            reloaded.readInitial(ACCOUNT_A).orElseThrow().levels().get("Attack"),
            reloaded.readInitial(ACCOUNT_B).orElseThrow().levels().get("Attack"));
        assertEquals(60, reloaded.readInitial(ACCOUNT_A).orElseThrow().levels().get("Attack").intValue());
        assertEquals(40, reloaded.readInitial(ACCOUNT_B).orElseThrow().levels().get("Attack").intValue());
        // A→B→A: A's bank memory survives B's login.
        assertEquals(1000, reloaded.readBank(ACCOUNT_A).orElseThrow().bankItems().get(995).intValue());
        assertTrue(reloaded.readBank(ACCOUNT_B).isEmpty());
    }

    @Test
    void historicalBankMemoryNeverProvesCurrentStockOrAuthorizesBib() {
        MapStore store = new MapStore();
        PathfinderAccountArchive archive = new PathfinderAccountArchive(store::get, store::put);
        long now = 1_700_000_000_000L;
        assertTrue(archive.rememberBank(ACCOUNT_A, now, openStableBank(now - 1_000, Map.of(4151, 1)), true));
        var memory = archive.readBank(ACCOUNT_A).orElseThrow();
        assertFalse(memory.provesCurrentStock());
        assertTrue(memory.fullTabWitness());
        var display = BankMemoryViewModel.renderForAccount(ACCOUNT_A, ACCOUNT_A, Optional.of(memory), now, id -> null);
        assertFalse(display.currentStockProven());
        assertTrue(display.notice().contains("Best in Bank"));
        assertTrue(display.notice().toLowerCase().contains("historical"));
        // Wrong account must not see A's memory.
        var foreign = BankMemoryViewModel.renderForAccount(ACCOUNT_B, ACCOUNT_A, Optional.of(memory), now, id -> null);
        assertEquals(BankMemoryViewModel.Freshness.NOT_SCANNED, foreign.freshness());
        assertTrue(foreign.rows().isEmpty());
    }

    @Test
    void sevenDayStaleRemindersFireForBankSkillsAndCapture() {
        MapStore store = new MapStore();
        PathfinderAccountArchive archive = new PathfinderAccountArchive(store::get, store::put);
        PathfinderGuidedCapture capture = new PathfinderGuidedCapture(store::get, store::put);
        long observed = 1_700_000_000_000L;
        assertTrue(archive.saveInitialOnce(ACCOUNT_A, observed, snapshot("Attack", 50, "Cook's Assistant", SuggestionEngine.Quest.FINISHED), true, true));
        assertTrue(archive.rememberLatest(ACCOUNT_A, observed, snapshot("Attack", 50, "Cook's Assistant", SuggestionEngine.Quest.FINISHED), true, true));
        assertTrue(archive.rememberBank(ACCOUNT_A, observed + 10, openStableBank(observed + 5, Map.of(995, 5)), true));
        assertTrue(capture.markPlayerReviewed(
            new PathfinderGuidedCapture.Context(ACCOUNT_A, ACCOUNT_A, ACCOUNT_A, 7L, true, false, observed + 20),
            PathfinderGuidedCapture.Section.ACHIEVEMENT_DIARIES, "manual-review:diaries", observed + 20));
        // Past the newest of skills/bank/capture timestamps by a full seven days.
        long later = observed + 20 + DAY + 1;

        assertTrue(archive.readLatest(ACCOUNT_A).orElseThrow().reminderDue(later));
        assertTrue(archive.readBank(ACCOUNT_A).orElseThrow().reminderDue(later));
        var step = capture.checklist(ACCOUNT_A).stream()
            .filter(s -> s.section() == PathfinderGuidedCapture.Section.ACHIEVEMENT_DIARIES)
            .findFirst().orElseThrow();
        assertTrue(step.staleReminderDue(later));

        var intel = AccountIntelligenceReport.build(
            ACCOUNT_A, later,
            archive.readInitial(ACCOUNT_A),
            archive.readLatest(ACCOUNT_A),
            archive.readBank(ACCOUNT_A),
            capture.checklist(ACCOUNT_A));
        assertTrue(intel.skillsQuestsStaleReminderDue());
        assertTrue(intel.anyCaptureStaleReminderDue());
        assertTrue(intel.bankStaleReminderDue());
        assertNotEquals(AccountIntelligenceReport.Overall.SIGNED_OUT, intel.overall());
        assertFalse(intel.headline().endsWith(" COMPLETE"));
        assertTrue(intel.headline().contains("NOT COMPLETE") || intel.headline().contains("OPEN")
            || intel.headline().contains("PARTIAL"));
    }

    @Test
    void missingEvidenceIsNeverZeroAndPartialNeverComplete() {
        MapStore store = new MapStore();
        PathfinderGuidedCapture capture = new PathfinderGuidedCapture(store::get, store::put);
        long now = 1_700_000_000_000L;
        var ctx = new PathfinderGuidedCapture.Context(ACCOUNT_A, ACCOUNT_A, ACCOUNT_A, 9L, true, false, now);
        assertTrue(capture.observeCollectionPage(ctx, "bosses", Map.of(), java.util.Set.of(4151), now));
        var snap = capture.read(ACCOUNT_A, PathfinderGuidedCapture.Section.COLLECTION_LOG).orElseThrow();
        assertEquals(PathfinderGuidedCapture.Status.PARTIALLY_WITNESSED, snap.status());
        assertFalse(snap.provesCompleteLog());
        // Unobserved boss kill log must stay UNOBSERVED — not zero KC.
        assertEquals(PathfinderGuidedCapture.Status.UNOBSERVED,
            capture.checklist(ACCOUNT_A).stream()
                .filter(s -> s.section() == PathfinderGuidedCapture.Section.BOSS_KILL_LOG)
                .findFirst().orElseThrow().status());
        var intel = AccountIntelligenceReport.build(
            ACCOUNT_A, now, Optional.empty(), Optional.empty(), Optional.empty(), capture.checklist(ACCOUNT_A));
        assertEquals(AccountIntelligenceReport.Overall.FIRST_RUN_INCOMPLETE, intel.overall());
        assertTrue(intel.nextUnobserved().isPresent());
    }

    @Test
    void freezeGateStillRequiresLiveBoundaries() {
        assertTrue(BankWithdrawalFreezeGate.authorized(true, true, true, true, true, true));
        assertFalse(BankWithdrawalFreezeGate.authorized(true, false, true, true, true, true),
            "closed bank must release O withdrawal lease");
        assertFalse(BankWithdrawalFreezeGate.authorized(true, true, true, true, true, false),
            "uncontested evidence required");
    }

    @Test
    void historicalBankMemoryPlansReadOnlyButNeverAuthorizesOTag() {
        MapStore store = new MapStore();
        PathfinderAccountArchive archive = new PathfinderAccountArchive(store::get, store::put);
        long now = 1_700_000_000_000L;
        assertTrue(archive.rememberBank(ACCOUNT_A, now, openStableBank(now - 500, Map.of(4151, 1, 995, 100)), true));
        var memory = archive.readBank(ACCOUNT_A).orElseThrow();
        var historical = BankEvidence.fromHistoricalBankMemory(memory, now);
        assertTrue(historical.historicalArchive());
        assertTrue(historical.planningInput());
        assertFalse(historical.bankStillOpen());
        assertFalse(memory.provesCurrentStock());
        assertTrue(historical.trustLine().contains("HISTORICAL"));
        assertTrue(historical.trustLine().contains("NOT currently withdrawable"));
        // O / layout gate must refuse closed historical evidence.
        assertFalse(BestBankLayoutGate.permitted(1, 1, true, true, true, true, historical));
        assertFalse(BankWithdrawalFreezeGate.authorized(true, historical.bankStillOpen(), true, true, true, true));
        // Planner may still compute a read-only plan from historical stock.
        try (BestBankPreloader pre = new BestBankPreloader(BestBankPreloader.knownProfiles())) {
            pre.observe(historical, AccountContext.UNKNOWN, null);
            assertNotNull(pre.evidence());
            assertTrue(pre.evidence().historicalArchive());
        }
    }

    @Test
    void firstRunCoverageLabelsDiariesCombatAchievementsAndCollectionLog() {
        MapStore store = new MapStore();
        PathfinderGuidedCapture capture = new PathfinderGuidedCapture(store::get, store::put);
        long now = 1_700_000_000_000L;
        var ctx = new PathfinderGuidedCapture.Context(ACCOUNT_A, ACCOUNT_A, ACCOUNT_A, 3L, true, false, now);
        assertTrue(capture.observeCollectionPage(ctx, "bosses", Map.of("Kree'arra", 12L), java.util.Set.of(11828), now));
        assertTrue(capture.markPlayerReviewed(ctx, PathfinderGuidedCapture.Section.ACHIEVEMENT_DIARIES, "manual-review:diaries", now));
        assertTrue(capture.markPlayerReviewed(ctx, PathfinderGuidedCapture.Section.COMBAT_ACHIEVEMENTS, "manual-review:ca", now));
        var intel = AccountIntelligenceReport.build(
            ACCOUNT_A, now, Optional.empty(), Optional.empty(), Optional.empty(), capture.checklist(ACCOUNT_A));
        var clog = intel.sections().stream()
            .filter(s -> s.section() == PathfinderGuidedCapture.Section.COLLECTION_LOG).findFirst().orElseThrow();
        var diaries = intel.sections().stream()
            .filter(s -> s.section() == PathfinderGuidedCapture.Section.ACHIEVEMENT_DIARIES).findFirst().orElseThrow();
        var ca = intel.sections().stream()
            .filter(s -> s.section() == PathfinderGuidedCapture.Section.COMBAT_ACHIEVEMENTS).findFirst().orElseThrow();
        assertTrue(clog.coverageLabel().contains("PARTIAL NATIVE"));
        assertFalse(clog.provesComplete());
        assertTrue(clog.witnessedEvidenceRows() >= 1);
        assertTrue(diaries.coverageLabel().contains("PLAYER REVIEW ONLY"));
        assertTrue(ca.coverageLabel().contains("PLAYER REVIEW ONLY"));
        assertFalse(diaries.provesComplete());
        assertFalse(ca.provesComplete());
    }

    @Test
    void legacyBankMemoryWithoutWitnessFlagDecodesAsNotFullTab() {
        MapStore store = new MapStore();
        // Craft a sealed legacy body: time|items  (no witness bit)
        long time = 1_700_000_000_000L;
        String body = time + "|995=3";
        String envelope = "PQA6|M|" + body;
        String sealed = envelope + "|" + sha(envelope);
        store.put(PathfinderAccountArchive.bankKey(ACCOUNT_A), sealed);
        PathfinderAccountArchive archive = new PathfinderAccountArchive(store::get, store::put);
        var memory = archive.readBank(ACCOUNT_A).orElseThrow();
        assertFalse(memory.fullTabWitness());
        assertEquals(3, memory.bankItems().get(995).intValue());
    }

    private static SuggestionEngine.Snapshot snapshot(
        String skill, int level, String quest, SuggestionEngine.Quest status) {
        return new SuggestionEngine.Snapshot(
            true,
            Map.of(skill, level),
            Map.of(quest, status),
            SuggestionEngine.BankStatus.UNAVAILABLE,
            java.util.Set.of());
    }

    private static BankEvidence openStableBank(long observedAt, Map<Integer, Integer> bank) {
        return new BankEvidence(
            bank, Map.of(), Map.of(),
            BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT,
            observedAt, true, false);
    }

    private static String sha(String s) {
        try {
            return java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static final class MapStore {
        private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        String get(String key) { return map.get(key); }
        void put(String key, String value) { map.put(key, value); }
    }
}
