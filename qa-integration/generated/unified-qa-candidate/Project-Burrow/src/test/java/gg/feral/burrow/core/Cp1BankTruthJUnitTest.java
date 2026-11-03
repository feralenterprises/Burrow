package gg.feral.burrow.core;

import gg.feral.burrow.core.BestBankPlanner.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Profile;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CP-1 bank truth + Best in Bank acceptance fixtures (BANK-X01, TRIP-X02, EQUIP-X02, variant rules).
 * Synthetic, pure-Java fixtures: they prove the rules, NOT live RuneLite ownership.
 */
public final class Cp1BankTruthJUnitTest {
    static final AccountContext MAX = BestBankPlannerJUnitTest.MAX;

    // ---------- Named variant rules (BQA-R12-003) ----------
    @Test void lockedOrnamentImbuedAndDegradedFormsAreUsableBrokenAndChargelessAreNot() {
        Item cape = new Item(6570, "Fire cape", 1);
        assertEquals(NamedVariantRules.Rule.LOCKED,
            NamedVariantRules.usable(cape, Map.of(90001, 1), Map.of(90001, "Fire cape (l)")).orElseThrow().rule());
        Item helm = new Item(4716, "Dharok's helm", 1);
        var names = Map.of(90100, "Dharok's helm 25", 90101, "Dharok's helm 100", 90102, "Dharok's helm 0");
        var owned = Map.of(90100, 1, 90101, 1, 90102, 1);
        var best = NamedVariantRules.usable(helm, owned, names).orElseThrow();
        assertEquals(90101, best.itemId(), "highest remaining durability first");
        assertTrue(NamedVariantRules.classify(helm, owned, names).stream()
            .anyMatch(h -> h.itemId() == 90102 && h.rule() == NamedVariantRules.Rule.BROKEN && !h.usable()));
        assertTrue(NamedVariantRules.usable(helm, Map.of(90102, 1), Map.of(90102, "Dharok's helm 0")).isEmpty(), "broken is never usable");
        Item crystal = new Item(23971, "Crystal helm", 1);
        assertTrue(NamedVariantRules.usable(crystal, Map.of(90200, 1), Map.of(90200, "Crystal helm (inactive)")).isEmpty());
        Item ring = new Item(500001, "Test ring", 1);
        assertEquals(NamedVariantRules.Rule.IMBUED_UPGRADE,
            NamedVariantRules.usable(ring, Map.of(90300, 1), Map.of(90300, "Test ring (i)")).orElseThrow().rule());
        // Not observed in stock: a name alone never creates ownership.
        assertTrue(NamedVariantRules.usable(cape, Map.of(), Map.of(90001, "Fire cape (l)")).isEmpty());
    }

    @Test void sameNameUnreviewedAndLmsCopiesAreReportedButNeverCounted() {
        Item ring = new Item(11773, "Berserker ring (i)", 1);
        var lms = NamedVariantRules.classify(ring, Map.of(23595, 1), Map.of(23595, "Berserker ring (i)"));
        assertEquals(1, lms.size());
        assertFalse(lms.get(0).usable(), "LMS copy withheld by the explicit ID deny-list");
        var unknown = NamedVariantRules.classify(ring, Map.of(28770, 1), Map.of(28770, "Berserker ring (i)"));
        assertEquals(NamedVariantRules.Rule.SAME_NAME_UNREVIEWED, unknown.get(0).rule());
        assertFalse(unknown.get(0).usable(), "owner-observed 28770 stays unreviewed until identified");
    }

    @Test void plannerCountsATrouverLockedCapeAsOwnedWhenNamesAreSupplied() {
        Profile rec = BestBankPlannerJUnitTest.profile("Scurrius", "Standard melee", Tier.RECOMMENDED);
        var own = BestBankPlannerJUnitTest.ownAll(rec);
        own.remove(6570); own.put(90001, 1);
        var ev = BestBankPlannerJUnitTest.bank(own);
        var without = BestBankPlanner.plan("Scurrius", "Standard melee", Tier.RECOMMENDED, ev, MAX, ItemFacts.catalogDerived());
        assertEquals(GearStatus.OPTIONAL_MISSING, without.gear().get(1).status(), "no names: not counted");
        var with = BestBankPlanner.plan("Scurrius", "Standard melee", Tier.RECOMMENDED, ev, MAX, ItemFacts.catalogDerived(),
            Map.of(90001, "Fire cape (l)"));
        assertEquals(90001, with.gear().get(1).shown().id());
        assertTrue(with.gear().get(1).status().physicallyOwned(), String.valueOf(with.gear().get(1)));
        assertTrue(with.gear().get(1).note().contains("Fire cape (l)"), with.gear().get(1).note());
    }

    // ---------- Slot criticality (TRIP-X02) ----------
    @Test void onlyWeaponsProjectileAmmoAndMechanicProtectionBlock() {
        assertTrue(SlotCriticality.blocking("Scurrius", 4, new Item(1, "Bone mace", 1)));
        assertTrue(SlotCriticality.blocking("Vorkath", 3, new Item(2, "Ruby dragon bolts (e)", 50)));
        assertFalse(SlotCriticality.blocking("Scurrius", 3, new Item(22943, "Rada's blessing 2", 1)), "blessing is optional");
        assertTrue(SlotCriticality.blocking("Vorkath", 6, new Item(3, "Dragonfire ward", 1)));
        assertTrue(SlotCriticality.blocking("King Black Dragon", 6, new Item(1540, "Anti-dragon shield", 1)));
        assertFalse(SlotCriticality.blocking("Scurrius", 6, new Item(12954, "Dragon defender", 1)));
        assertFalse(SlotCriticality.blocking("Scurrius", 10, new Item(11773, "Berserker ring (i)", 1)));
        assertTrue(SlotCriticality.blocking("Thermonuclear smoke devil", 0, new Item(4, "Slayer helmet (i)", 1)), "smoke: face protection");
        assertFalse(SlotCriticality.blocking("Scurrius", 0, new Item(4, "Slayer helmet (i)", 1)));
        assertTrue(SlotCriticality.blocking("Lizardman Shamans", 5, new Item(5, "Shayzien body (5)", 1)));
    }

    // ---------- Set effects (EQUIP-X02) ----------
    @Test void incompleteSetLosesItsEffectAndBlocksOnlyWhenTheMethodDependsOnIt() {
        List<Item> voidGear = Arrays.asList(new Item(11665, "Void melee helm", 1), null, null, null, new Item(1, "Abyssal whip", 1),
            new Item(8839, "Void knight top", 1), null, new Item(8840, "Void knight robe", 1), new Item(8842, "Void knight gloves", 1), null, null);
        boolean[] owned = new boolean[11]; owned[0] = owned[5] = owned[7] = true; // gloves missing
        var checks = SlotCriticality.setEffects("Standard melee", voidGear, owned);
        assertEquals(1, checks.size());
        assertEquals(3, checks.get(0).owned()); assertEquals(4, checks.get(0).planned());
        assertFalse(checks.get(0).complete()); assertFalse(checks.get(0).methodDepends());
        assertTrue(checks.get(0).text().contains("3/4"), checks.get(0).text());
        List<Item> dh = Arrays.asList(new Item(4716, "Dharok's helm", 1), null, null, null, new Item(4718, "Dharok's greataxe", 1),
            new Item(4720, "Dharok's platebody", 1), null, new Item(4722, "Dharok's platelegs", 1), null, null, null);
        boolean[] dhOwned = new boolean[11]; dhOwned[0] = dhOwned[7] = true;
        var d = SlotCriticality.setEffects("Dharok's set (special method)", dh, dhOwned);
        assertTrue(d.get(0).methodDepends(), "Dharok method depends on the full set");
        assertEquals(3, d.get(0).planned(), "weapon is not counted as an armour set piece");
    }

    // ---------- Live carried evidence (BANK-X01) ----------
    @Test void packToWornMoveAfterBankCloseNeverDuplicatesOwnership() {
        var open = new BankEvidence(Map.of(1, 5), Map.of(4151, 1), Map.of(), BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT, 10L, true, false);
        var closed = open.closed();
        assertThrows(IllegalStateException.class, () -> open.withLiveCarried(Map.of(), Map.of(4151, 1), 20L));
        var equipped = closed.withLiveCarried(Map.of(), Map.of(4151, 1), 20L);
        assertEquals(Integer.valueOf(1), equipped.combined().get(4151), "moved from pack to worn: counted once");
        assertEquals(Integer.valueOf(5), equipped.combined().get(1), "bank as of close unchanged");
        assertFalse(equipped.stale());
        assertTrue(equipped.trustLine().contains("BANK AS OF CLOSE") && equipped.trustLine().contains("PACK+WORN LIVE"), equipped.trustLine());
        var dropped = equipped.withLiveCarried(Map.of(), Map.of(), 30L);
        assertNull(dropped.combined().get(4151), "dropped/consumed item is gone, not a ghost");
    }

    @Test void preloaderRefreshesCarriedOnlyWhenBankClosedAndAdvancesGeneration() throws Exception {
        var key = new BestBankPreloader.Key("Scurrius", "Standard melee", Tier.RECOMMENDED);
        try (var pre = new BestBankPreloader(List.of(key))) {
            pre.pauseForOutfitter(true);
            var ev = new BankEvidence(Map.of(28792, 1), Map.of(), Map.of(), BankEvidence.Population.STABLE_READ, 1L, true, false);
            long g0 = pre.observe(ev, MAX, null, Map.of());
            assertEquals(g0, pre.refreshCarried(Map.of(), Map.of(11773, 1), 5L, Map.of()), "bank open: no carried refresh");
            long g1 = pre.markBankClosed();
            long g2 = pre.refreshCarried(Map.of(), Map.of(11773, 1), 5L, Map.of(11773, "Berserker ring (i)"));
            assertTrue(g2 > g1);
            assertEquals(Integer.valueOf(1), pre.evidence().combined().get(11773));
            assertEquals(g2, pre.refreshCarried(Map.of(), Map.of(11773, 1), 6L, Map.of()), "identical read: no churn");
            assertEquals(BestBankPreloader.UpgradeEvidence.CURRENT, pre.upgradeEvidence());
        }
    }

    // ---------- Set effect inside the real planner ----------
    @Test void guthanHealingMethodWithIncompleteSetIsBlockedAtThatTier() {
        String boss = "Dagannoth Kings", method = "Rex safespot with Guthan healing switch";
        Profile rec = BestBankPlannerJUnitTest.profile(boss, method, Tier.RECOMMENDED);
        int body = -1;
        for (int i = 0; i < 11; i++) { Item it = rec.gear().get(i); if (it != null && it.name().contains("Guthan") && i == 5) body = i; }
        if (body < 0) for (int i = 0; i < 11; i++) { Item it = rec.gear().get(i); if (it != null && i != 4 && it.name().contains("Guthan")) { body = i; break; } }
        assertTrue(body >= 0, "fixture: Recommended DK Guthan plan wears Guthan armour " + rec.gear());
        var own = BestBankPlannerJUnitTest.ownAll(rec); own.remove(rec.gear().get(body).id());
        var plan = BestBankPlanner.plan(boss, method, Tier.RECOMMENDED, BestBankPlannerJUnitTest.bank(own), MAX, ItemFacts.catalogDerived());
        String recSummary = plan.candidateSummaries().get(0);
        assertTrue(recSummary.startsWith("Recommended: NOT_READY"), String.valueOf(plan.candidateSummaries()));
        if (plan.shownTier() == Tier.RECOMMENDED)
            assertTrue(plan.blockers().stream().anyMatch(b -> b.contains("set incomplete")), String.valueOf(plan.blockers()));
    }

    @Test void twoKarilPiecesAsPlainArmourGetNoSetEffectWarning() {
        List<Item> mixed = Arrays.asList(null, null, null, null, new Item(1, "Bow", 1), new Item(4736, "Karil's leathertop", 1),
            null, new Item(4738, "Karil's leatherskirt", 1), null, null, null);
        assertTrue(SlotCriticality.setEffects("Wiki · Ranged (tank)", mixed, new boolean[11]).isEmpty());
    }
    /** CP-1: a weapon-only trip is playable but never silently READY. */
    @Test void weaponWithMostArmourMissingIsVerifyNotReady() {
        var p = BestBankPlannerJUnitTest.plan("Scurrius", "Standard melee", Tier.RECOMMENDED, new HashMap<>(Map.of(28792, 1)), MAX);
        assertNotEquals(Verdict.NOT_READY, p.verdict(), String.valueOf(p.blockers()));
        assertNotEquals(Verdict.READY, p.verdict());
        assertTrue(p.checks().stream().anyMatch(c -> c.contains("noticeably weaker than the Wiki plan")), String.valueOf(p.checks()));
    }
}
