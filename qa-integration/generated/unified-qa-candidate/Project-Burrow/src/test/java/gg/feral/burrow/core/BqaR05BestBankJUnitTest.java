package gg.feral.burrow.core;

import gg.feral.burrow.core.BankCaptureGate.*;
import gg.feral.burrow.core.BestBankPlanner.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Profile;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * BQA-IMPL-R05 P0 Best in Bank regressions: realistic bank + inventory + EMPTY equipment, owned alternatives,
 * and reasons. Synthetic item maps over the real catalog; NOT a native RuneLite capture.
 */
final class BqaR05BestBankJUnitTest {
    static final long ACCT = 77L;
    static Profile p(String b, String m, Tier t) { return TieredOutfitterCatalog.find(b, m, t).profile(); }
    static Map<Integer,Integer> own(Profile p, int... skipSlots) {
        Set<Integer> skip = new HashSet<>(); for (int s : skipSlots) skip.add(s);
        Map<Integer,Integer> m = new HashMap<>();
        for (int i = 0; i < 11; i++) { Item it = p.gear().get(i); if (it != null && it.id() > 0 && !skip.contains(i)) m.merge(it.id(), Math.max(1, it.amount()), Integer::sum); }
        for (Item it : p.inventory()) if (it != null && it.id() > 0) m.merge(it.id(), Math.max(1, it.amount()), Integer::sum);
        return m;
    }
    static List<RawItem> raw(Map<Integer,Integer> m) {
        List<RawItem> out = new ArrayList<>();
        m.forEach((k, v) -> out.add(RawItem.bank(k, v)));
        return out;
    }
    static BankEvidence ev(Read r) {
        return new BankEvidence(r.bank(), r.inventory(), r.equipment(), BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT, 1L, true, false);
    }

    @Test void nothingWornNoEquipmentContainerStillProducesATripPlan() {
        // Owner: "recommendations only appeared after I equipped something". Equipment container absent = nothing reported.
        Profile wiki = p("Scurrius", "Standard melee", Tier.OPTIMAL);
        var bank = raw(own(wiki));
        var noEquip = new RawRead(true, true, true, false, ACCT, ACCT, bank, List.of(), null, bank.size(), false, true, false);
        var r = BankCaptureGate.evaluate(noEquip);
        assertEquals(Outcome.READY_SOURCE_QUALIFIED, r.outcome());
        assertTrue(r.read().equipment().isEmpty());
        assertEquals("worn: none reported by client", r.read().carriedNote());
        var tracker = new BankScanTracker(); Object scope = new Object();
        tracker.bind(scope, ACCT); tracker.bankOpened();
        tracker.offer(r, 1);
        var d = tracker.offer(BankCaptureGate.evaluate(noEquip), 2);
        assertEquals(BankScanTracker.Decision.Kind.ACCEPT, d.kind());
        var plan = BestBankPlanner.plan("Scurrius", "Standard melee", Tier.OPTIMAL, ev(d.accepted()), BestBankPlannerJUnitTest.MAX, null);
        assertNotEquals(Verdict.UNKNOWN, plan.verdict());
        assertEquals(GearStatus.OWNED, plan.gear().get(4).status(), "weapon recognised from the BANK without equipping it");
        // An existing container whose items are unreadable still fails closed.
        var unreadable = new RawRead(true, true, true, false, ACCT, ACCT, bank, List.of(), null, bank.size(), false, true, true);
        assertEquals(Outcome.EQUIPMENT_UNAVAILABLE, BankCaptureGate.evaluate(unreadable).outcome());
    }

    @Test void inventoryAndBankOnlyRealisticSplitCountsEachItemOnce() {
        Profile wiki = p("Scurrius", "Standard melee", Tier.OPTIMAL);
        Map<Integer,Integer> all = own(wiki);
        Map<Integer,Integer> bank = new HashMap<>(all), inv = new HashMap<>();
        Item weapon = wiki.gear().get(4);
        bank.remove(weapon.id()); inv.put(weapon.id(), 1); // weapon carried in inventory, nothing worn
        var rr = new RawRead(true, true, true, false, ACCT, ACCT, raw(bank), raw(inv), null, bank.size(), false, true, false);
        var read = BankCaptureGate.evaluate(rr).read();
        assertEquals(1, read.combined().get(weapon.id()).intValue());
        var plan = BestBankPlanner.plan("Scurrius", "Standard melee", Tier.OPTIMAL, ev(read), BestBankPlannerJUnitTest.MAX, null);
        assertEquals(GearStatus.OWNED, plan.gear().get(4).status());
    }

    @Test void missingTargetWeaponUsesOwnedLowerTierWeaponSetAsAWhole() {
        Profile opt = p("Scurrius", "Standard melee", Tier.OPTIMAL), rec = p("Scurrius", "Standard melee", Tier.RECOMMENDED);
        Map<Integer,Integer> owned = own(opt, 4);                       // everything but the Scythe
        for (int idx : new int[]{3, 4, 6}) { Item it = rec.gear().get(idx); if (it != null && it.id() > 0) owned.merge(it.id(), Math.max(1, it.amount()), Math::max); }
        var res = TierAwareBankResolver.resolve("Scurrius", "Standard melee", Tier.OPTIMAL, owned);
        assertEquals(TierAwareBankResolver.Kind.LOWER_TIER_OWNED, res.slots().get(4).kind(), res.summary());
        assertEquals(rec.gear().get(4).id(), res.slots().get(4).displayed().id());
        assertTrue(res.coreWeaponObserved());
        var plan = BestBankPlanner.plan("Scurrius", "Standard melee", Tier.OPTIMAL,
            new BankEvidence(owned, Map.of(), Map.of(), BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT, 1L, true, false), BestBankPlannerJUnitTest.MAX, null);
        assertEquals(GearStatus.OWNED_SUBSTITUTE, plan.gear().get(4).status());
        assertFalse(plan.blockers().stream().anyMatch(b -> b.startsWith("CORE Weapon")), String.valueOf(plan.blockers()));
    }

    @Test void weaponIsNeverSwappedAloneWithoutItsLowerTierPartners() {
        Profile opt = p("Scurrius", "Standard melee", Tier.OPTIMAL), rec = p("Scurrius", "Standard melee", Tier.RECOMMENDED);
        Map<Integer,Integer> owned = own(opt, 4, 6);                    // no Scythe and no target offhand
        owned.put(rec.gear().get(4).id(), 1);                           // lower-tier weapon owned...
        Item recShield = rec.gear().get(6);
        if (recShield != null) owned.remove(recShield.id());           // ...but its offhand is not
        var res = TierAwareBankResolver.resolve("Scurrius", "Standard melee", Tier.OPTIMAL, owned);
        // CP-1: the Dragon defender is an OPTIONAL offhand (not mechanic protection). Its absence no longer vetoes the
        // owned lower-tier weapon; the offhand is reported as not owned and left empty. Mechanic offhands and
        // projectile ammunition still veto the swap (see the Vorkath and F2P bow tests).
        assertEquals(TierAwareBankResolver.Kind.LOWER_TIER_OWNED, res.slots().get(4).kind(), res.summary());
        if (recShield != null && recShield.id() > 0)
            assertEquals(TierAwareBankResolver.Kind.NOT_OBSERVED, res.slots().get(6).kind(), "offhand shown as not owned, not invented");
        assertTrue(res.coreWeaponObserved());
    }

    @Test void coordinatedSetNoLongerFreezesUnrelatedSlots() {
        String b = "Amoxliatl", m = "Melee, full Inquisitor with mace";
        Profile opt = p(b, m, Tier.OPTIMAL), rec = p(b, m, Tier.RECOMMENDED);
        Map<Integer,Integer> owned = own(opt, 1);                       // no Infernal cape
        owned.put(rec.gear().get(1).id(), 1);                           // Fire cape owned
        var res = TierAwareBankResolver.resolve(b, m, Tier.OPTIMAL, owned);
        assertEquals(TierAwareBankResolver.Kind.LOWER_TIER_OWNED, res.slots().get(1).kind());
        assertEquals("Fire cape", res.slots().get(1).displayed().name());
        // The Inquisitor pieces themselves are still never swapped.
        for (int i = 0; i < 11; i++) {
            Item it = opt.gear().get(i);
            if (it != null && it.name().toLowerCase(Locale.ROOT).contains("inquisitor")) {
                var without = new HashMap<>(owned); without.remove(it.id());
                assertNotEquals(TierAwareBankResolver.Kind.LOWER_TIER_OWNED,
                    TierAwareBankResolver.resolve(b, m, Tier.OPTIMAL, without).slots().get(i).kind());
            }
        }
    }

    @Test void diagnosisSeparatesUnreviewedVariantFromNotOwned() {
        Profile opt = p("Scurrius", "Standard melee", Tier.OPTIMAL);
        Item scythe = opt.gear().get(4);
        Map<Integer,Integer> owned = own(opt, 4, 1);
        owned.remove(28792);                                            // CP-1: no owned lower-tier weapon either
        owned.put(990001, 1);                                           // a same-name variant ID that is not reviewed
        var ev = new BankEvidence(owned, Map.of(), Map.of(), BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT, 1L, true, false);
        var plan = BestBankPlanner.plan("Scurrius", "Standard melee", Tier.OPTIMAL, ev, BestBankPlannerJUnitTest.MAX, null);
        assertNotEquals(GearStatus.OWNED, plan.gear().get(4).status(), "unreviewed variant is never counted as owned");
        var f = BestBankDiagnosis.diagnose(plan, ev.combined(), Map.of(990001, scythe.name() + " (uncharged)"));
        var weapon = f.stream().filter(x -> x.slot() == 4).findFirst().orElseThrow();
        assertEquals(BestBankDiagnosis.Cause.OWNED_VARIANT_NOT_REVIEWED, weapon.cause());
        assertTrue(weapon.text().contains("(uncharged)"));
        assertEquals("ring of suffering", BestBankDiagnosis.baseName("Ring of suffering (i)"));
        // No names (e.g. offline fixture): only generic reasons, never a guessed variant.
        assertTrue(BestBankDiagnosis.diagnose(plan, ev.combined(), Map.of()).stream()
            .noneMatch(x -> x.cause() == BestBankDiagnosis.Cause.OWNED_VARIANT_NOT_REVIEWED));
        assertTrue(BestBankDiagnosis.diagnose(BestBankPlanner.unknown("Scurrius", "Standard melee", Tier.OPTIMAL), Map.of(), Map.of()).isEmpty());
    }

    @Test void preparedResultCarriesDiagnosisToThePanel() throws Exception {
        Profile opt = p("Scurrius", "Standard melee", Tier.OPTIMAL);
        Map<Integer,Integer> owned = own(opt, 4);
        owned.remove(28792);                                            // CP-1: no owned lower-tier weapon either
        var key = new BestBankPreloader.Key("Scurrius", "Standard melee", Tier.OPTIMAL);
        try (var pre = new BestBankPreloader(List.of(key))) {
            pre.observe(new BankEvidence(owned, Map.of(), Map.of(), BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT, 1L, true, false),
                BestBankPlannerJUnitTest.MAX, null, Map.of());
            var got = new java.util.concurrent.CompletableFuture<BestBankPreloader.Prepared>();
            pre.request(key, got::complete);
            var prepared = got.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(prepared.diagnosis().stream().anyMatch(x -> x.slot() == 4), String.valueOf(prepared.diagnosis()));
        }
    }
}
