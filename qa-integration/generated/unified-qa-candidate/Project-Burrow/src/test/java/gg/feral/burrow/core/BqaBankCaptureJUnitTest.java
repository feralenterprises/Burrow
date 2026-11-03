package gg.feral.burrow.core;

import gg.feral.burrow.core.BankCaptureGate.*;
import gg.feral.burrow.core.BestBankPlanner.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * BQA-901 Batch A focused regressions (synthetic raw reads, NOT a native RuneLite capture).
 * Test ids map to BQA-901 section 5 where noted.
 */
final class BqaBankCaptureJUnitTest {
    static final long ACCT = 0x1234_5678_9ABCL;
    static List<RawItem> items(int... idQty) {
        List<RawItem> out = new ArrayList<>();
        for (int i = 0; i < idQty.length; i += 2) out.add(RawItem.bank(idQty[i], idQty[i + 1]));
        return out;
    }
    static RawRead read(long acct, List<RawItem> bank, List<RawItem> inv, List<RawItem> eq, int tabs) {
        return new RawRead(true, true, true, false, acct, acct, bank, inv, eq, tabs, false);
    }
    static RawRead ok(List<RawItem> bank) { return read(ACCT, bank, items(), items(), bank.size()); }

    // ---- gate ----
    @Test void t01_coherentReadIsReadyWithPositiveCounts() {
        var r = BankCaptureGate.evaluate(read(ACCT, items(4151, 1, 385, 20), items(2434, 4), items(11832, 1), 2));
        assertEquals(Outcome.READY_SOURCE_QUALIFIED, r.outcome());
        assertEquals(Map.of(4151, 1, 385, 20), r.read().bank());
        assertEquals(Map.of(4151, 1, 385, 20, 2434, 4, 11832, 1), r.read().combined());
        assertTrue(r.read().tabWitnessed());
    }

    @Test void t10_negativeButNotMinusOneAccountHashIsValidIdentity() {
        for (long h : new long[]{-2L, Long.MIN_VALUE, -987654321L}) {
            var r = BankCaptureGate.evaluate(read(h, items(4151, 1), items(), items(), 1));
            assertEquals(Outcome.READY_SOURCE_QUALIFIED, r.outcome(), "hash class " + Long.signum(h));
            assertEquals(h, r.read().accountHash());
        }
        assertEquals(Outcome.IDENTITY_UNAVAILABLE,
            BankCaptureGate.evaluate(read(-1L, items(4151, 1), items(), items(), 1)).outcome());
        assertTrue(BankCaptureGate.accountAvailable(-2L));
        assertFalse(BankCaptureGate.accountAvailable(-1L));
    }

    @Test void t02_t03_widgetAndPinStatesAreTypedWaitStates() {
        var hidden = new RawRead(true, true, false, false, ACCT, ACCT, items(4151, 1), items(), items(), 1, false);
        assertEquals(Outcome.WIDGET_NOT_READY, BankCaptureGate.evaluate(hidden).outcome());
        var pin = new RawRead(true, true, true, true, ACCT, ACCT, items(4151, 1), items(), items(), 1, false);
        assertEquals(Outcome.PIN_OPEN, BankCaptureGate.evaluate(pin).outcome());
        var out = new RawRead(false, true, true, false, ACCT, ACCT, items(4151, 1), items(), items(), 1, false);
        assertEquals(Outcome.NOT_LOGGED_IN, BankCaptureGate.evaluate(out).outcome());
        assertEquals(Outcome.CLIENT_READ_EXCEPTION, BankCaptureGate.evaluate(null).outcome());
        for (var o : List.of(Outcome.WIDGET_NOT_READY, Outcome.PIN_OPEN, Outcome.BANK_POPULATING)) assertTrue(o.waitState());
    }

    @Test void missingContainersAreDistinguishedNotZero() {
        assertEquals(Outcome.BANK_CONTAINER_UNAVAILABLE, BankCaptureGate.evaluate(read(ACCT, null, items(), items(), -1)).outcome());
        assertEquals(Outcome.INVENTORY_UNAVAILABLE, BankCaptureGate.evaluate(read(ACCT, items(1, 1), null, items(), 1)).outcome());
        assertEquals(Outcome.EQUIPMENT_UNAVAILABLE, BankCaptureGate.evaluate(read(ACCT, items(1, 1), items(), null, 1)).outcome());
    }

    @Test void identityChangeDuringReadIsRejected() {
        var r = new RawRead(true, true, true, false, ACCT, ACCT + 1, items(4151, 1), items(), items(), 1, false);
        assertEquals(Outcome.IDENTITY_CHANGED, BankCaptureGate.evaluate(r).outcome());
    }

    @Test void t07_badDefinitionBlocksPlaceholdersNotesFillerZeroFiltered() {
        List<RawItem> bank = new ArrayList<>(items(4151, 1));
        bank.add(new RawItem(9999, 1, false, false, false));
        assertEquals(Outcome.BAD_BANK_ITEM_DEFINITION, BankCaptureGate.evaluate(ok(bank)).outcome());
        List<RawItem> filtered = List.of(RawItem.bank(4151, 1), new RawItem(4152, 1, true, true, false),
            new RawItem(4153, 50, true, false, true), RawItem.bank(BankCaptureGate.BANK_FILLER_ID, 1),
            RawItem.bank(385, 0), RawItem.bank(-1, 0));
        var r = BankCaptureGate.evaluate(ok(filtered));
        assertEquals(Map.of(4151, 1), r.read().bank());
        assertEquals(6, r.read().rawBankSlots(), "raw container length kept for the tab witness");
    }

    @Test void t08_t09_containerShorterThanTabCountsIsPopulating() {
        assertEquals(Outcome.BANK_POPULATING, BankCaptureGate.evaluate(read(ACCT, items(4151, 1), items(), items(), 5)).outcome());
        var unreadableTabs = BankCaptureGate.evaluate(read(ACCT, items(4151, 1), items(), items(), -1));
        assertTrue(unreadableTabs.ready());
        assertFalse(unreadableTabs.read().tabWitnessed(), "unreadable tab counts never claim consistency");
    }

    // ---- tracker ----
    @Test void t01_trackerNeedsTwoIdenticalReadsAndRecordsLastGood() {
        var t = new BankScanTracker(); Object scope = new Object();
        t.bind(scope, ACCT); t.bankOpened();
        var a = BankCaptureGate.evaluate(ok(items(4151, 1)));
        assertEquals(BankScanTracker.Decision.Kind.WAIT, t.offer(a, 1000).kind());
        var d = t.offer(a, 1600);
        assertEquals(BankScanTracker.Decision.Kind.ACCEPT, d.kind());
        assertEquals(BankScanTracker.Phase.ACCEPTED, t.phase());
        assertEquals(1600, t.status().lastGood().observedAtMillis());
    }

    @Test void t05_changingSamplesNeverCommitMixedReads() {
        var t = new BankScanTracker(); Object scope = new Object();
        t.bind(scope, ACCT); t.bankOpened();
        assertEquals(BankScanTracker.Decision.Kind.WAIT, t.offer(BankCaptureGate.evaluate(ok(items(4151, 1))), 1).kind());
        assertEquals(BankScanTracker.Decision.Kind.WAIT, t.offer(BankCaptureGate.evaluate(ok(items(4151, 2))), 2).kind());
        assertEquals(Outcome.CHANGING_SAMPLE, t.status().lastOutcome());
        var d = t.offer(BankCaptureGate.evaluate(ok(items(4151, 2))), 3);
        assertEquals(BankScanTracker.Decision.Kind.ACCEPT, d.kind());
        assertEquals(Map.of(4151, 2), d.accepted().bank());
    }

    @Test void t02_failuresAreReasonedAndCountedThenRecover() {
        var t = new BankScanTracker(); Object scope = new Object();
        t.bind(scope, ACCT); t.bankOpened();
        var hidden = new RawRead(true, true, false, false, ACCT, ACCT, items(4151, 1), items(), items(), 1, false);
        for (int i = 0; i < 3; i++) t.offer(BankCaptureGate.evaluate(hidden), i);
        var st = t.status();
        assertEquals(Outcome.WIDGET_NOT_READY, st.lastOutcome());
        assertEquals(3, st.attempts());
        assertTrue(st.headline().contains("WIDGET NOT READY"), st.headline());
        assertTrue(st.detail(10).contains("bank interface still loading"), st.detail(10));
        t.offer(BankCaptureGate.evaluate(ok(items(4151, 1))), 10);
        assertEquals(BankScanTracker.Decision.Kind.ACCEPT, t.offer(BankCaptureGate.evaluate(ok(items(4151, 1))), 11).kind());
    }

    @Test void t06_emptyBankNeedsLongerWitnessAndReadableTabs() {
        var t = new BankScanTracker(); Object scope = new Object();
        t.bind(scope, ACCT); t.bankOpened();
        var unwitnessed = BankCaptureGate.evaluate(read(ACCT, items(), items(4151, 1), items(), -1));
        for (int i = 0; i < 5; i++) assertEquals(BankScanTracker.Decision.Kind.WAIT, t.offer(unwitnessed, i).kind());
        assertEquals(Outcome.BANK_POPULATING, t.status().lastOutcome());
        var witnessed = BankCaptureGate.evaluate(read(ACCT, items(), items(4151, 1), items(), 0));
        assertEquals(BankScanTracker.Decision.Kind.WAIT, t.offer(witnessed, 10).kind());
        assertEquals(BankScanTracker.Decision.Kind.WAIT, t.offer(witnessed, 11).kind());
        var d = t.offer(witnessed, 12);
        assertEquals(BankScanTracker.Decision.Kind.ACCEPT, d.kind());
        assertTrue(d.accepted().bankUsableEmpty());
        assertTrue(t.status().lastGood().bankEmpty());
    }

    @Test void t06_emptyBankPlusCarriedGearIsPlannedNotUnknownAndCountedOnce() {
        var wiki = TieredOutfitterCatalog.find("Scurrius", "Standard melee", Tier.OPTIMAL).profile();
        var weapon = wiki.gear().get(4);
        assertNotNull(weapon);
        var ev = new BankEvidence(Map.of(), Map.of(), Map.of(weapon.id(), 1),
            BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT, 1L, true, false);
        assertTrue(ev.planningInput());
        var p = BestBankPlanner.plan("Scurrius", "Standard melee", Tier.OPTIMAL, ev, BestBankPlannerJUnitTest.MAX, null);
        assertNotEquals(Verdict.UNKNOWN, p.verdict());
        assertTrue(p.gear().get(4).status() == GearStatus.OWNED || p.gear().get(4).status() == GearStatus.OWNED_VARIANT,
            String.valueOf(p.gear().get(4)));
        assertTrue(ev.trustLine().contains("bank empty (confirmed)"), ev.trustLine());
        try (var pre = new BestBankPreloader(List.of())) {
            pre.observe(ev, BestBankPlannerJUnitTest.MAX, null);
            assertTrue(pre.evidence() != null);
        }
        // An unattested single-read fixture with an empty bank stays UNKNOWN (existing contract).
        var fixture = new BankEvidence(Map.of(), Map.of(), Map.of(weapon.id(), 1), BankEvidence.Population.SINGLE_READ, 1L, true, false);
        assertFalse(fixture.planningInput());
        assertEquals(Verdict.UNKNOWN, BestBankPlanner.plan("Scurrius", "Standard melee", Tier.OPTIMAL, fixture, BestBankPlannerJUnitTest.MAX, null).verdict());
    }

    @Test void t13_failedRescanKeepsLastGoodAsHistoryOnly() {
        var t = new BankScanTracker(); Object scope = new Object();
        t.bind(scope, ACCT); t.bankOpened();
        var a = BankCaptureGate.evaluate(ok(items(4151, 1)));
        t.offer(a, 1000); t.offer(a, 1600);
        t.invalidated(true);
        var hidden = new RawRead(true, true, false, false, ACCT, ACCT, null, items(), items(), -1, false);
        var d = t.offer(BankCaptureGate.evaluate(hidden), 61_600);
        assertNull(d.accepted(), "last good is never handed back as current stock");
        var st = t.status();
        assertEquals(BankScanTracker.Phase.WAITING, st.phase());
        assertNotNull(st.lastGood());
        assertTrue(st.detail(61_600).contains("Last good read 1m ago is history only"), st.detail(61_600));
        t.bankClosed();
        assertEquals(BankScanTracker.Phase.INVALIDATED, t.phase());
    }

    @Test void t11_t21_accountOrScopeChangeWipesPendingAndLastGood() {
        var t = new BankScanTracker(); Object a = new Object(), b = new Object();
        t.bind(a, ACCT); t.bankOpened();
        var r = BankCaptureGate.evaluate(ok(items(4151, 1)));
        t.offer(r, 1); t.offer(r, 2);
        assertNotNull(t.status().lastGood());
        t.bind(b, ACCT + 7);
        assertNull(t.status().lastGood());
        assertEquals(BankScanTracker.Phase.IDLE, t.phase());
        t.bankOpened();
        // A late read carrying the previous account cannot be accepted under the new binding.
        assertEquals(BankScanTracker.Decision.Kind.REJECT_FOREIGN, t.offer(r, 3).kind());
        assertEquals(BankScanTracker.Decision.Kind.REJECT_FOREIGN, t.offer(r, 4).kind());
        t.unbind();
        assertFalse(t.boundTo(null, -1L));
    }

    @Test void t12_closeAfterAcceptIsAsOfAndSessionNotReadyIsVisible() {
        var t = new BankScanTracker(); Object scope = new Object();
        t.bind(scope, ACCT); t.bankOpened();
        t.failedBeforeRead(Outcome.SESSION_NOT_READY, 5);
        assertTrue(t.status().headline().contains("SESSION NOT READY"));
        var r = BankCaptureGate.evaluate(ok(items(4151, 1)));
        t.offer(r, 6); t.offer(r, 7);
        t.bankClosed();
        assertEquals(BankScanTracker.Phase.CLOSED_AS_OF, t.phase());
        assertEquals("BANK READ · AS OF BANK CLOSE", t.status().headline());
        assertThrows(IllegalArgumentException.class, () -> t.failedBeforeRead(Outcome.READY_SOURCE_QUALIFIED, 1));
    }

    @Test void statusTextNeverContainsAccountHashOrItemIds() {
        var t = new BankScanTracker(); Object scope = new Object();
        t.bind(scope, ACCT); t.bankOpened();
        var r = BankCaptureGate.evaluate(ok(items(4151, 1)));
        t.offer(r, 1); t.offer(r, 2); t.invalidated(false);
        String all = t.status().headline() + " " + t.status().detail(100_000);
        assertFalse(all.contains(Long.toString(ACCT)));
        assertFalse(all.contains("4151"));
    }
}
