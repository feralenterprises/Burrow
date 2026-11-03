package gg.feral.burrow.core;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static gg.feral.burrow.core.BankQuantityReconciler.*;

/** Self-contained Java 17 test harness. Run with: java -ea gg.feral.burrow.core.BankQuantityReconcilerTest */
public final class BankQuantityReconcilerTest {
    private static final Instant NOW = Instant.parse("2026-10-30T19:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(5);
    private static int checks;

    private static Identity id(int item, String variant) { return new Identity(item, variant); }
    private static AcceptedVariant accept(Identity i, long units) { return new AcceptedVariant(i, units); }
    private static Need need(String key, String name, String unit, long amount, AcceptedVariant... accepted) {
        return new Need(key, name, unit, amount, List.of(accepted));
    }
    private static ObservedStack stack(String slot, Identity identity, long count) {
        return new ObservedStack(slot, identity, count);
    }
    private static Observation observed(Location location, Coverage c, String account, Instant at,
                                        ObservedStack... stacks) {
        return new Observation(location, account, c, at, List.of(stacks));
    }
    private static Observation complete(Location location, ObservedStack... stacks) {
        return observed(location, Coverage.COMPLETE, "fixture-account-A", NOW, stacks);
    }
    private static Result run(List<Need> needs, Observation... sources) {
        return reconcile("fixture-account-A", needs, List.of(sources), NOW, TTL);
    }
    private static void eq(Object actual, Object expected, String label) {
        checks++;
        if (!java.util.Objects.equals(actual, expected))
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
    }
    private static void yes(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    private static void rejects(Runnable block, String label) {
        checks++;
        try { block.run(); }
        catch (IllegalArgumentException | ArithmeticException ok) { return; }
        throw new AssertionError("expected rejection: " + label);
    }
    private static Row first(Result r) { return r.rows().get(0); }

    private static void duplicateApprovedSlotsSumWithoutBorrowingStock() {
        Identity pot = id(2434, "dose-4");
        Need perSlot = need("prayer-potion-4", "Prayer potion (4)", "vials", 1, accept(pot, 1));
        Result r = run(List.of(perSlot, perSlot, perSlot, perSlot),
            complete(Location.BANK, stack("bank:6", pot, 2)),
            complete(Location.INVENTORY, stack("inv:1", pot, 1)),
            complete(Location.EQUIPMENT));
        eq(r.rows().size(), 1, "duplicate needs consolidated");
        Row row = first(r);
        eq(row.requiredUnits(), 4L, "requirements independent from observed ownership");
        eq(row.confirmedTotal(), 3L, "bank and inventory independently counted");
        eq(row.confirmedByLocation().get(Location.BANK), 2L, "bank visible separately");
        eq(row.confirmedByLocation().get(Location.INVENTORY), 1L, "inventory visible separately");
        eq(row.confirmedByLocation().get(Location.EQUIPMENT), 0L, "worn gear visible separately");
        eq(row.uncoveredUnits(), 1L, "actual verified deficit");
        eq(row.verdict(), Verdict.VERIFIED_SHORT, "verified missing not unknown");
    }

    private static void stackablesAndActualDuplicateSlots() {
        Identity arrow = id(892, "base");
        Need need = need("rune-arrows", "Rune arrow", "arrows", 240, accept(arrow, 1));
        Result r = run(List.of(need),
            complete(Location.BANK, stack("bank:1", arrow, 200)),
            complete(Location.INVENTORY, stack("inv:1", arrow, 50)),
            complete(Location.EQUIPMENT));
        eq(first(r).confirmedTotal(), 250L, "stack count represents actual quantity");
        eq(first(r).verdict(), Verdict.SUFFICIENT, "stackable supply sufficient");
        rejects(() -> complete(Location.BANK, stack("same", arrow, 200), stack("same", arrow, 200)),
                "replayed same slot must not inflate observed stock");
        Result twoRealStacks = run(List.of(need),
            complete(Location.BANK, stack("b1", arrow, 100), stack("b2", arrow, 150)),
            complete(Location.INVENTORY), complete(Location.EQUIPMENT));
        eq(first(twoRealStacks).confirmedTotal(), 250L, "distinct slots sum");
    }

    private static void chargesOnlyWithExplicitApprovedConversions() {
        Identity p4 = id(2434, "dose-4"), p3 = id(139, "dose-3"), p2 = id(141, "dose-2");
        Need doses = need("prayer-doses", "Prayer potion", "doses", 8,
            accept(p4, 4), accept(p3, 3));
        Result r = run(List.of(doses),
            complete(Location.BANK, stack("b4", p4, 1), stack("b3", p3, 1), stack("b2", p2, 9)),
            complete(Location.INVENTORY), complete(Location.EQUIPMENT));
        eq(first(r).confirmedTotal(), 7L, "4+3 explicit doses, unapproved variant excluded");
        eq(first(r).uncoveredUnits(), 1L, "actual dose shortfall");
        eq(first(r).verdict(), Verdict.VERIFIED_SHORT, "short 1 dose");
        Need onlyFourDoseVials = need("pot4", "Prayer potion (4)", "vials", 2, accept(p4, 1));
        Result strict = run(List.of(onlyFourDoseVials),
            complete(Location.BANK, stack("b1", p4, 1), stack("b2", p3, 20)),
            complete(Location.INVENTORY), complete(Location.EQUIPMENT));
        eq(first(strict).confirmedTotal(), 1L, "lower-dose vials cannot fulfill exact 4-dose requirement");
        eq(first(strict).verdict(), Verdict.VERIFIED_SHORT, "no automatic potion alias");
    }

    private static void unknownAccountNeverCreditsOrDeniesStock() {
        Identity shark = id(385, "base");
        Need n = need("sharks", "Shark", "fish", 4, accept(shark, 1));
        Observation bank = complete(Location.BANK, stack("b1", shark, 999));
        Result r = reconcile(null, List.of(n), List.of(bank), NOW, TTL);
        eq(first(r).confirmedTotal(), 0L, "unknown account must credit no stock");
        eq(first(r).verdict(), Verdict.UNKNOWN, "unknown account not verified zero");
        eq(r.sourceStates().get(Location.BANK), SourceState.ACCOUNT_UNKNOWN, "no identity state");
        Result foreign = run(List.of(n), observed(Location.BANK, Coverage.COMPLETE, "somebody-else", NOW,
            stack("b1", shark, 999)), complete(Location.INVENTORY), complete(Location.EQUIPMENT));
        eq(first(foreign).confirmedTotal(), 0L, "foreign stock ignored");
        eq(first(foreign).verdict(), Verdict.UNKNOWN, "foreign bank is not a zero scan");
        eq(foreign.sourceStates().get(Location.BANK), SourceState.FOREIGN_ACCOUNT, "foreign state");
    }

    private static void missingSuppliesVerifiedZeroVsNeverScanned() {
        Identity food = id(3144, "base");
        Need n = need("karambwan", "Cooked karambwan", "items", 12, accept(food, 1));
        Result zeros = run(List.of(n), complete(Location.BANK), complete(Location.INVENTORY),
            complete(Location.EQUIPMENT));
        eq(first(zeros).confirmedTotal(), 0L, "empty complete snapshots are verified zero");
        eq(first(zeros).verdict(), Verdict.VERIFIED_SHORT, "verified zero means missing 12");
        eq(first(zeros).uncoveredUnits(), 12L, "correct required shortfall");
        Result unknown = run(List.of(n), complete(Location.INVENTORY), complete(Location.EQUIPMENT));
        eq(first(unknown).verdict(), Verdict.UNKNOWN, "unscanned bank must NOT be missing");
        eq(unknown.sourceStates().get(Location.BANK), SourceState.NOT_SCANNED, "bank not scanned");
        eq(first(unknown).requiredUnits(), 12L, "loadout demand unchanged by missing scan");
    }

    private static void staleAndPartialCannotProveShortfall() {
        Identity food = id(385, "base");
        Need n = need("sharks", "Shark", "items", 3, accept(food, 1));
        Result stale = run(List.of(n), observed(Location.BANK, Coverage.COMPLETE, "fixture-account-A",
            NOW.minus(Duration.ofMinutes(6)), stack("b", food, 400)),
            complete(Location.INVENTORY), complete(Location.EQUIPMENT));
        eq(stale.sourceStates().get(Location.BANK), SourceState.STALE, "old snapshot flagged stale");
        eq(first(stale).confirmedTotal(), 0L, "stale positive stock NOT credited");
        eq(first(stale).verdict(), Verdict.UNKNOWN, "stale positive NOT verified zero");
        Result partial = run(List.of(n), observed(Location.BANK, Coverage.PARTIAL, "fixture-account-A", NOW,
            stack("b", food, 2)), complete(Location.INVENTORY), complete(Location.EQUIPMENT));
        eq(first(partial).confirmedTotal(), 2L, "partial known positives credited");
        eq(first(partial).verdict(), Verdict.UNKNOWN, "partial cannot prove missing third");
        Result enough = run(List.of(n), observed(Location.BANK, Coverage.PARTIAL, "fixture-account-A", NOW,
            stack("b", food, 3)), complete(Location.INVENTORY), complete(Location.EQUIPMENT));
        eq(first(enough).verdict(), Verdict.SUFFICIENT, "partial known positives can prove enough");
        Result future = run(List.of(n), observed(Location.BANK, Coverage.COMPLETE, "fixture-account-A", NOW.plusSeconds(1),
            stack("b", food, 400)), complete(Location.INVENTORY), complete(Location.EQUIPMENT));
        eq(future.sourceStates().get(Location.BANK), SourceState.FUTURE_TIMESTAMP, "future rejected");
        eq(first(future).confirmedTotal(), 0L, "future cannot create stock");
    }

    private static void unresolvedItemCannotBeInvented() {
        Need unknownCharge = need("unknown-cowbell", "Cowbell amulet (charge unspecified)",
            "items", 1);
        Result r = run(List.of(unknownCharge), complete(Location.BANK), complete(Location.INVENTORY),
            complete(Location.EQUIPMENT));
        eq(first(r).verdict(), Verdict.UNRESOLVED_IDENTITY, "unknown charge stays unresolved");
        eq(first(r).requiredUnits(), 1L, "approved count retained");
        eq(first(r).confirmedTotal(), 0L, "no inferred ownership");
    }

    private static void cannotDoubleSpendOrInjectStock() {
        Identity id = id(385, "base");
        Need a = need("food-a", "Shark", "items", 1, accept(id, 1));
        Need b = need("food-b", "Shark", "items", 1, accept(id, 1));
        rejects(() -> run(List.of(a,b), complete(Location.BANK, stack("b1", id, 1))),
                "same item identity across distinct needs not allocated twice");
        rejects(() -> run(List.of(a, need("food-a", "Shark", "items", 1, accept(id, 2)))),
                "inconsistent loadout duplicates");
        rejects(() -> run(List.of(a), complete(Location.BANK), complete(Location.BANK)),
                "same source cannot be replayed");
        rejects(() -> stack("inv:1", id, -1), "negative stock must reject");
        rejects(() -> new Observation(Location.BANK, "fixture-account-A", Coverage.UNKNOWN, null,
                     List.of(stack("b1", id, 100))), "unknown cannot carry stock");
        rejects(() -> new Need("bad", "bad", "items", 0, List.of()), "invalid loadout zero");
        rejects(() -> run(List.of(a), observed(Location.BANK, Coverage.COMPLETE, "fixture-account-A", NOW,
            stack("b1", id, Long.MAX_VALUE), stack("b2", id, 1)), complete(Location.INVENTORY),
            complete(Location.EQUIPMENT)), "overflow must reject not wrap");
        // Defensive copying: subsequent caller mutation does not alter already captured observation.
        List<ObservedStack> mutable = new ArrayList<>(List.of(stack("b1", id, 1)));
        Observation stable = new Observation(Location.BANK, "fixture-account-A", Coverage.COMPLETE, NOW, mutable);
        mutable.clear();
        eq(stable.stacks().size(), 1, "snapshot immune to list mutation");
    }

    public static void main(String[] args) {
        duplicateApprovedSlotsSumWithoutBorrowingStock();
        stackablesAndActualDuplicateSlots();
        chargesOnlyWithExplicitApprovedConversions();
        unknownAccountNeverCreditsOrDeniesStock();
        missingSuppliesVerifiedZeroVsNeverScanned();
        staleAndPartialCannotProveShortfall();
        unresolvedItemCannotBeInvented();
        cannotDoubleSpendOrInjectStock();
        System.out.println("BUR-0020 OFFLINE PURE-JAVA TESTS PASS: 8 scenarios, " + checks + " assertions");
        System.out.println("No RuneLite client, live scan, native Java17 runtime, or Gradle gate exercised.");
    }
}
