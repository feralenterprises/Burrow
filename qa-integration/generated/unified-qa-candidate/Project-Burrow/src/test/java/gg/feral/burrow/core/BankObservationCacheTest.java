package gg.feral.burrow.core;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/** Pure Java 17 fixture replay; NO RuneLite client and NO native PASS assertions. */
public final class BankObservationCacheTest {
    private static final String A = "23a34388091eb04504104d596821cbf7af2362febd14a5940009217901d7961a";
    private static final String B = "983256c5a06f9a598012e28deda7a113748d2c105ded333772e0faf636bab19c";
    private static final Instant OPEN = Instant.parse("2026-10-30T19:13:00Z");
    private static final Instant READ = OPEN.plusSeconds(1);
    private static final Duration FRESH = Duration.ofHours(2);
    private static int tests;

    public static void main(String[] ignored) {
        run("no identity + unknown is not zero", BankObservationCacheTest::testUnknown);
        run("full three-way count + source segregation", BankObservationCacheTest::testThreeWay);
        run("verified empty differs from unknown", BankObservationCacheTest::testEmpty);
        run("raw jewelry/imbued variants remain separate", BankObservationCacheTest::testVariants);
        run("duplicate physical slot rejected", BankObservationCacheTest::testDuplicateSlot);
        run("invalid inventory/equipment slots rejected", BankObservationCacheTest::testPhysicalSlotBounds);
        run("public snapshot cannot contradict source containers", BankObservationCacheTest::testSnapshotConsistency);
        run("stack aggregation and overflow atomic failure", BankObservationCacheTest::testCounts);
        run("cross-account A->B->A and logout", BankObservationCacheTest::testAccountSwitch);
        run("login epoch and pending token invalidation", BankObservationCacheTest::testEpoch);
        run("world hop and bank close invalidate captures", BankObservationCacheTest::testHopAndClose);
        run("failed reads retain same-account prior success", BankObservationCacheTest::testFailedScan);
        run("failed read without prior success remains unknown", BankObservationCacheTest::testFailureWithoutHistory);
        run("partial/readiness/identity proof rejects commits", BankObservationCacheTest::testAttestation);
        run("historical and time staleness are labeled", BankObservationCacheTest::testStaleness);
        run("defensive deep copies and immutable outputs", BankObservationCacheTest::testImmutability);
        run("bad input, future time and generation replay", BankObservationCacheTest::testBadInput);
        run("synthetic + dev sources blocked in production", BankObservationCacheTest::testProductionGate);
        run("clearAll destroys all cached stock", BankObservationCacheTest::testClearAll);
        System.out.println("BANK OBSERVATION OFFLINE PASS: " + tests + " independent synthetic scenarios");
    }

    private static BankObservationCache cache() { return BankObservationCache.forSyntheticTests(); }
    private static BankObservationCache.Attestation attest() {
        return new BankObservationCache.Attestation(BankObservationCache.Source.SYNTHETIC_TEST,
                true, true, true, true, true, true, true);
    }
    private static BankObservationCache.Slot slot(int index, int id, long qty) {
        return new BankObservationCache.Slot(index, id, qty);
    }
    private static BankObservationCache.Containers containers(List<BankObservationCache.Slot> bank,
             List<BankObservationCache.Slot> inventory, List<BankObservationCache.Slot> equipment) {
        return new BankObservationCache.Containers(bank, inventory, equipment);
    }
    private static BankObservationCache.Containers full() {
        // Mirrors bank-contract R01/fixtures/verified_full.json verbatim in raw counts.
        return containers(List.of(slot(0, 385, 3), slot(1, 2434, 4)),
                          List.of(slot(0, 385, 2)), List.of(slot(12, 6737, 1)));
    }
    private static BankObservationCache.Observation observation(BankObservationCache.Containers stacks) {
        return new BankObservationCache.Observation(READ, attest(), stacks);
    }
    private static boolean add(BankObservationCache cache, BankObservationCache.Containers stacks) {
        return cache.observe(cache.beginBankOpen(OPEN), observation(stacks), READ);
    }
    private static BankObservationCache.View view(BankObservationCache c) { return c.view(READ, FRESH); }
    private static void check(boolean condition, String error) {
        if (!condition) throw new AssertionError(error);
    }
    private static void equal(Object actual, Object expected) {
        check(java.util.Objects.equals(actual, expected), "expected " + expected + ", got " + actual);
    }
    private static void throwsIA(Runnable r) {
        try { r.run(); } catch (IllegalArgumentException ex) { return; }
        throw new AssertionError("expected IllegalArgumentException");
    }
    private static void run(String name, Runnable r) {
        r.run(); tests++; System.out.println("PASS " + name);
    }

    private static void testUnknown() {
        BankObservationCache c=cache();
        equal(view(c).status(), BankObservationCache.Status.NO_IDENTITY);
        check(view(c).rawCountAsOf(385).isEmpty(), "no identity must not imply zero");
        c.beginLogin(A, "EPOCH-0001");
        equal(view(c).status(), BankObservationCache.Status.UNKNOWN);
        check(view(c).rawCountAsOf(385).isEmpty(), "not yet scanned != verified zero");
        throwsIA(() -> c.beginLogin("raw-account-hash", "EPOCH-0001"));
        throwsIA(() -> c.beginLogin(A, "short"));
    }
    private static void testThreeWay() {
        BankObservationCache c=cache(); c.beginLogin(A, "EPOCH-0001");
        check(add(c, full()), "full observed");
        BankObservationCache.View v=view(c);
        equal(v.status(), BankObservationCache.Status.VERIFIED_AS_OF);
        equal(v.freshness(), BankObservationCache.Freshness.SAME_LOGIN_AS_OF);
        equal(v.snapshot().source(), BankObservationCache.Source.SYNTHETIC_TEST);
        equal(v.rawCountAsOf(385).orElseThrow(), new BankObservationCache.Counts(3, 2, 0));
        equal(v.rawCountAsOf(385).orElseThrow().total(), 5L);
        equal(v.rawCountAsOf(2434).orElseThrow().bank(), 4L);
        equal(v.rawCountAsOf(6737).orElseThrow().equipment(), 1L);
        equal(v.snapshot().revision(), 1L);
    }
    private static void testEmpty() {
        BankObservationCache c=cache(); c.beginLogin(A, "EPOCH-0001");
        check(add(c, containers(List.of(),List.of(),List.of())), "verified empty must commit");
        equal(view(c).rawCountAsOf(385).orElseThrow().total(), 0L);
        equal(view(c).snapshot().containers().bank().size(), 0);
        equal(view(c).snapshot().source(), BankObservationCache.Source.SYNTHETIC_TEST);
    }
    private static void testVariants() {
        BankObservationCache c=cache(); c.beginLogin(A, "EPOCH-0001");
        check(add(c, containers(List.of(slot(2, 1704, 1), slot(3, 11978, 1)),
                 List.of(), List.of(slot(12, 11770, 1)))), "variants commit");
        equal(view(c).rawCountAsOf(1704).orElseThrow().total(), 1L);
        equal(view(c).rawCountAsOf(11978).orElseThrow().total(), 1L);
        equal(view(c).rawCountAsOf(11770).orElseThrow().total(), 1L);
        equal(view(c).rawCountAsOf(6737).orElseThrow().total(), 0L);
        check(view(c).snapshot().exactRawCounts().size() == 3, "no variant folding");
    }
    private static void testDuplicateSlot() {
        throwsIA(() -> containers(List.of(slot(0, 385, 3), slot(0, 385, 2)), List.of(), List.of()));
        throwsIA(() -> slot(-1, 385, 1));
        throwsIA(() -> slot(0, 0, 1));
        throwsIA(() -> slot(0, 385, 0));
        throwsIA(() -> containers(null, List.of(), List.of()));
        // Same physical index in DIFFERENT containers is legal.
        containers(List.of(slot(0,385,1)), List.of(slot(0,385,1)),List.of(slot(0,385,1)));
    }
    private static void testPhysicalSlotBounds() {
        // All legal edges: inventory slots 0..27, equipment slots 0..13.
        containers(List.of(slot(999, 385, 1)), List.of(slot(0, 385, 1), slot(27, 385, 1)),
                   List.of(slot(0, 6737, 1), slot(13, 6737, 1)));
        for (int invalid : new int[] {28, 29, 999, Integer.MAX_VALUE}) {
            throwsIA(() -> containers(List.of(), List.of(slot(invalid, 385, 7)), List.of()));
        }
        for (int invalid : new int[] {14, 15, 999, Integer.MAX_VALUE}) {
            throwsIA(() -> containers(List.of(), List.of(), List.of(slot(invalid, 6737, 1))));
        }
        // Malformed containers can never be submitted as an accepted observation.
        BankObservationCache c = cache(); c.beginLogin(A, "EPOCH-0001");
        check(add(c, full()), "valid initial observation");
        throwsIA(() -> containers(List.of(), List.of(slot(999, 385, 7)), List.of()));
        equal(view(c).rawCountAsOf(385).orElseThrow().total(), 5L);
    }
    private static void testSnapshotConsistency() {
        var source = full();
        var correct = Map.of(385, new BankObservationCache.Counts(3, 2, 0),
                             2434, new BankObservationCache.Counts(4, 0, 0),
                             6737, new BankObservationCache.Counts(0, 0, 1));
        var valid = new BankObservationCache.Snapshot(A, "EPOCH-0001", 1, 1,
                OPEN, READ, BankObservationCache.Source.SYNTHETIC_TEST, source, correct);
        equal(valid.rawCountAsOf(385).total(), 5L);
        var empty = containers(List.of(), List.of(), List.of());
        throwsIA(() -> new BankObservationCache.Snapshot(A, "EPOCH-0001", 1, 1,
                OPEN, READ, BankObservationCache.Source.RUNELITE_BANK_OPEN, empty,
                Map.of(385, new BankObservationCache.Counts(999, 0, 0))));
        throwsIA(() -> new BankObservationCache.Snapshot(A, "EPOCH-0001", 1, 1,
                OPEN, READ, BankObservationCache.Source.RUNELITE_BANK_OPEN, source, Map.of()));
        throwsIA(() -> new BankObservationCache.Snapshot(A, "EPOCH-0001", 1, 1,
                OPEN, READ, BankObservationCache.Source.RUNELITE_BANK_OPEN, source,
                Map.of(385, new BankObservationCache.Counts(3, 1, 0),
                       2434, new BankObservationCache.Counts(4, 0, 0),
                       6737, new BankObservationCache.Counts(0, 0, 1))));
        throwsIA(() -> new BankObservationCache.Snapshot(A, "EPOCH-0001", 1, 1,
                OPEN, READ, BankObservationCache.Source.RUNELITE_BANK_OPEN, empty,
                Map.of(385, new BankObservationCache.Counts(0, 0, 0))));
        // Successful map construction copies external mutable state.
        Map<Integer, BankObservationCache.Counts> mutable = new HashMap<>(correct);
        var detached = new BankObservationCache.Snapshot(A, "EPOCH-0001", 1, 1,
                OPEN, READ, BankObservationCache.Source.SYNTHETIC_TEST, source, mutable);
        mutable.clear();
        equal(detached.exactRawCounts(), correct);
        try { detached.exactRawCounts().clear(); throw new AssertionError("mutable snapshot map"); }
        catch (UnsupportedOperationException expected) { }
        // Snapshot counts remain separated by exact raw item ID.
        var variants = containers(List.of(slot(0, 1704, 1), slot(1, 11978, 2)),
                List.of(), List.of());
        var variantCounts = Map.of(1704, new BankObservationCache.Counts(1, 0, 0),
                11978, new BankObservationCache.Counts(2, 0, 0));
        new BankObservationCache.Snapshot(A, "EPOCH-0001", 1, 1,
                OPEN, READ, BankObservationCache.Source.SYNTHETIC_TEST, variants, variantCounts);
    }
    private static void testCounts() {
        BankObservationCache c=cache(); c.beginLogin(A,"EPOCH-0001");
        check(add(c, full()), "initial");
        var overflow = containers(List.of(slot(0, 385, Long.MAX_VALUE), slot(1, 385, 1)),
                                  List.of(), List.of());
        check(!add(c, overflow), "long overflow rejected before partial commit");
        equal(view(c).status(), BankObservationCache.Status.SCAN_FAILED_LAST_VERIFIED);
        equal(view(c).rawCountAsOf(385).orElseThrow().total(), 5L);
        equal(view(c).snapshot().revision(), 1L);
        // Per-container and cross-container aggregation (no double count).
        check(add(c, containers(List.of(slot(0,385,4),slot(1,385,6)),
                                  List.of(slot(0,385,3)), List.of(slot(12,385,1)))), "valid duplicate ID slots");
        equal(view(c).rawCountAsOf(385).orElseThrow(), new BankObservationCache.Counts(10,3,1));
        equal(view(c).snapshot().revision(), 2L);
    }
    private static void testAccountSwitch() {
        BankObservationCache c=cache(); c.beginLogin(A,"EPOCH-A001"); check(add(c,full()), "A");
        c.identityUnknown();
        check(view(c).rawCountAsOf(385).isEmpty(), "A logout must be opaque");
        c.beginLogin(B,"EPOCH-B001");
        equal(view(c).status(), BankObservationCache.Status.UNKNOWN);
        check(view(c).rawCountAsOf(385).isEmpty(), "B must not see A");
        check(add(c, containers(List.of(slot(0,385,50)),List.of(),List.of())), "B");
        equal(view(c).rawCountAsOf(385).orElseThrow().bank(),50L);
        c.beginLogin(A,"EPOCH-A002");
        equal(view(c).status(), BankObservationCache.Status.HISTORICAL_AS_OF);
        equal(view(c).freshness(), BankObservationCache.Freshness.PRIOR_LOGIN_AS_OF);
        equal(view(c).rawCountAsOf(385).orElseThrow().bank(),3L);
        check(add(c,full()), "A refreshed");
        equal(view(c).snapshot().revision(),2L);
        c.beginLogin(B,"EPOCH-B002");
        equal(view(c).rawCountAsOf(385).orElseThrow().bank(),50L);
    }
    private static void testEpoch() {
        BankObservationCache c=cache(); c.beginLogin(A,"EPOCH-0001");
        var old=c.beginBankOpen(OPEN);
        c.beginLogin(A,"EPOCH-0002");
        check(!c.observe(old, observation(full()), READ), "late previous epoch must be rejected");
        equal(view(c).status(),BankObservationCache.Status.UNKNOWN);
        var current=c.beginBankOpen(OPEN);
        c.beginLogin(B,"EPOCH-0003");
        check(!c.scanFailed(current), "late A failure cannot contaminate B");
        equal(view(c).status(),BankObservationCache.Status.UNKNOWN);
    }
    private static void testHopAndClose() {
        BankObservationCache c=cache(); c.beginLogin(A,"EPOCH-0001");
        var pending=c.beginBankOpen(OPEN);
        c.worldHop();
        check(!c.observe(pending,observation(full()),READ), "pending old world discarded");
        equal(view(c).status(),BankObservationCache.Status.UNKNOWN);
        check(add(c, full()), "new world capture");
        var stale=c.beginBankOpen(READ);
        c.bankClosed();
        check(!c.observe(stale,observation(full()),READ), "closed bank blocks late capture");
        c.worldHop();
        equal(view(c).status(),BankObservationCache.Status.VERIFIED_AS_OF);
        equal(view(c).freshness(),BankObservationCache.Freshness.WORLD_HOP_STALE);
        equal(view(c).rawCountAsOf(385).orElseThrow().total(),5L);
    }
    private static void testFailedScan() {
        BankObservationCache c=cache(); c.beginLogin(A,"EPOCH-0001"); check(add(c,full()), "prior");
        var token=c.beginBankOpen(OPEN);
        check(c.scanFailed(token),"read failure");
        equal(view(c).status(),BankObservationCache.Status.SCAN_FAILED_LAST_VERIFIED);
        equal(view(c).freshness(),BankObservationCache.Freshness.FAILED_RESCAN);
        equal(view(c).rawCountAsOf(385).orElseThrow().bank(),3L);
        check(!c.scanFailed(token),"cannot double fail/replay");
    }
    private static void testFailureWithoutHistory() {
        BankObservationCache c=cache(); c.beginLogin(A,"EPOCH-0001");
        check(c.scanFailed(c.beginBankOpen(OPEN)),"failed");
        equal(view(c).status(),BankObservationCache.Status.UNKNOWN);
        check(view(c).rawCountAsOf(385).isEmpty(),"unknown must not become zero");
    }
    private static void testAttestation() {
        for (int bad = 0; bad < 7; ++bad) {
            BankObservationCache c=cache(); c.beginLogin(A,"EPOCH-0001");
            boolean[] flags = {true,true,true,true,true,true,true}; flags[bad]=false;
            var a = new BankObservationCache.Attestation(BankObservationCache.Source.SYNTHETIC_TEST,
                flags[0],flags[1],flags[2],flags[3],flags[4],flags[5],flags[6]);
            check(!c.observe(c.beginBankOpen(OPEN),new BankObservationCache.Observation(READ,a,full()),READ),
                  "incomplete gate " + bad + " must fail");
            equal(view(c).status(), BankObservationCache.Status.UNKNOWN);
        }
    }
    private static void testStaleness() {
        BankObservationCache c=cache(); c.beginLogin(A,"EPOCH-0001"); check(add(c,full()),"initial");
        var aged=c.view(READ.plus(Duration.ofHours(3)),FRESH);
        equal(aged.status(),BankObservationCache.Status.VERIFIED_AS_OF);
        equal(aged.freshness(),BankObservationCache.Freshness.AGE_STALE);
        equal(aged.rawCountAsOf(385).orElseThrow().total(),5L);
        check(c.view(OPEN,FRESH).rawCountAsOf(385).isEmpty(),"clock backwards must fail closed");
        c.beginLogin(A,"EPOCH-0002");
        equal(c.view(READ.plus(Duration.ofHours(3)),FRESH).status(),BankObservationCache.Status.HISTORICAL_AS_OF);
    }
    private static void testImmutability() {
        List<BankObservationCache.Slot> mutable = new ArrayList<>(); mutable.add(slot(0,385,3));
        var raw=containers(mutable,List.of(),List.of());
        mutable.clear();
        check(raw.bank().size()==1,"copy must detach mutable input");
        BankObservationCache c=cache(); c.beginLogin(A,"EPOCH-0001"); check(add(c,raw),"copy");
        try {view(c).snapshot().containers().bank().clear();throw new AssertionError("mutable list");}
        catch(UnsupportedOperationException correct) { }
        try {view(c).snapshot().exactRawCounts().clear();throw new AssertionError("mutable counts map");}
        catch(UnsupportedOperationException correct) { }
        equal(view(c).rawCountAsOf(385).orElseThrow().total(),3L);
    }
    private static void testBadInput() {
        BankObservationCache c=cache(); c.beginLogin(A,"EPOCH-0001");
        var t=c.beginBankOpen(OPEN);
        check(!c.observe(t,null,READ),"null observation rejected");
        check(!c.observe(t,observation(full()),READ),"same generation replay rejected");
        var t2=c.beginBankOpen(OPEN);
        check(!c.observe(t2,observation(full()),OPEN),"future capture rejected");
        equal(view(c).status(),BankObservationCache.Status.UNKNOWN);
        check(add(c,full()),"healthy future generation");
    }
    private static void testProductionGate() {
        BankObservationCache c=new BankObservationCache(); c.beginLogin(A,"EPOCH-0001");
        check(!add(c,full()), "fixture must not enter production cache");
        equal(view(c).status(),BankObservationCache.Status.UNKNOWN);
        var dev=new BankObservationCache.Attestation(BankObservationCache.Source.DEV_MANUAL_SCAN,
                  true,true,true,true,true,true,true);
        check(!c.observe(c.beginBankOpen(OPEN),new BankObservationCache.Observation(READ,dev,full()),READ),
              "manual mode must be disabled by default");
    }
    private static void testClearAll() {
        BankObservationCache c=cache(); c.beginLogin(A,"EPOCH-0001"); check(add(c,full()),"initial");
        c.clearAll(); equal(view(c).status(),BankObservationCache.Status.NO_IDENTITY);
        c.beginLogin(A,"EPOCH-0002");
        check(view(c).rawCountAsOf(385).isEmpty(),"shutdown clears memory, no persistence");
    }
}
