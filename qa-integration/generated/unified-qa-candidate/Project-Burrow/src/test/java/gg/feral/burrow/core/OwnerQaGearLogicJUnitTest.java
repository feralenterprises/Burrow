package gg.feral.burrow.core;

import gg.feral.burrow.core.BestBankPlanner.Plan;
import gg.feral.burrow.core.OwnedGearStats.Focus;
import gg.feral.burrow.core.OwnedGearStats.Stats;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Owner live QA, 2026-11-02: Zilyana said no weapon suited the fight and kept Black d'hide over better armour. */
public final class OwnerQaGearLogicJUnitTest {
    static final AccountContext MAX = BestBankPlannerJUnitTest.MAX;
    static final String RANGED = "Wiki · Ranged", MAGIC = "Wiki · Magic", ZILYANA = "Commander Zilyana";
    static final int WEAPON = 4, AMMO = 3, BODY = 5;
    static final int RUNE_CROSSBOW = 9185, DRAGON_CROSSBOW = 21902, ZARYTE = 26374, RUBY_E = 9242, BLOWPIPE = 12926;
    static final int BLACK_BODY = 2503, HUEY_BODY = 30076, SARA_BODY = 10386, AHRIM_STAFF = 4710, SCEPTRE = 28585;

    static Stats weapon(double range, double magic, boolean twoHanded) {
        return new Stats(ItemFacts.WEAPON, twoHanded, 0, 0, 0, magic, range, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    static Stats body(double range, double stab, double slash, double crush, double magic, double ranged, double prayer) {
        return new Stats(ItemFacts.BODY, false, 0, 0, 0, -15, range, stab, slash, crush, magic, ranged, 0, 0, 0, prayer);
    }
    static Stats bolts() { return new Stats(ItemFacts.AMMO, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 85, 0, 0); }

    /** Every item of the kit except its weapon, ammo and extras, like a mid-game bank. */
    static Map<Integer,Integer> armourOnly(String boss, String method, Tier tier) {
        Map<Integer,Integer> own = new HashMap<>();
        var p = TieredOutfitterCatalog.find(boss, method, tier).profile();
        for (int i = 0; i < 11; i++) { Item it = p.gear().get(i); if (i != WEAPON && i != AMMO && it != null && it.id() > 0) own.put(it.id(), 1); }
        for (Item it : p.inventory()) if (it != null && it.id() > 0 && !ItemRequirementRegistry.isEquipment(it.id())) own.merge(it.id(), Math.max(1, it.amount()), Integer::sum);
        return own;
    }
    static Plan plan(String boss, String method, Tier tier, Map<Integer,Integer> own, Map<Integer,String> names, Map<Integer,Stats> stats) {
        return BestBankPlanner.plan(boss, method, tier, BestBankPlannerJUnitTest.bank(own), MAX, ItemFacts.catalogDerived(), names, stats, Focus.MATCH);
    }
    static boolean has(List<String> lines, String text) { return lines.stream().anyMatch(l -> l.contains(text)); }

    @Test void zilyanaUsesAnOwnedLowerWikiCrossbow() {
        var own = armourOnly(ZILYANA, RANGED, Tier.ENTRY); own.put(RUNE_CROSSBOW, 1); own.put(RUBY_E, 500);
        var names = Map.of(RUNE_CROSSBOW, "Rune crossbow", RUBY_E, "Ruby bolts (e)");
        var plan = plan(ZILYANA, RANGED, Tier.ENTRY, own, names, Map.of(RUBY_E, bolts()));
        assertEquals(RUNE_CROSSBOW, plan.gear().get(WEAPON).shown().id());
        assertEquals(RUBY_E, plan.gear().get(AMMO).shown().id(), "bolts are chosen for the weapon actually used");
        assertFalse(has(plan.blockers(), "Weapon"), plan.blockers().toString());
        assertFalse(has(plan.blockers(), "Ammo"), plan.blockers().toString());
    }
    @Test void aWeaponWithAmmoBeatsABetterWeaponWithout() {
        var own = armourOnly(ZILYANA, RANGED, Tier.ENTRY); own.put(RUNE_CROSSBOW, 1); own.put(DRAGON_CROSSBOW, 1); own.put(RUBY_E, 500);
        var names = Map.of(RUNE_CROSSBOW, "Rune crossbow", DRAGON_CROSSBOW, "Dragon crossbow", RUBY_E, "Ruby bolts (e)");
        assertEquals(DRAGON_CROSSBOW, plan(ZILYANA, RANGED, Tier.ENTRY, own, names, Map.of(RUBY_E, bolts())).gear().get(WEAPON).shown().id(), "both can fire, the higher one wins");
    }
    @Test void aTierNeverGainsAWeaponListedAboveIt() {
        var own = armourOnly(ZILYANA, RANGED, Tier.ENTRY); own.put(ZARYTE, 1); own.put(RUNE_CROSSBOW, 1); own.put(RUBY_E, 500);
        var names = Map.of(ZARYTE, "Zaryte crossbow", RUNE_CROSSBOW, "Rune crossbow", RUBY_E, "Ruby bolts (e)");
        var stats = Map.of(RUBY_E, bolts(), ZARYTE, weapon(110, 0, false), RUNE_CROSSBOW, weapon(90, 0, false));
        assertEquals(RUNE_CROSSBOW, plan(ZILYANA, RANGED, Tier.ENTRY, own, names, stats).gear().get(WEAPON).shown().id());
    }
    @Test void statsFallbackCannotReachAWeaponAboveTheTier() {
        var own = armourOnly(ZILYANA, RANGED, Tier.ENTRY); own.put(ZARYTE, 1); own.put(RUBY_E, 500);
        var names = Map.of(ZARYTE, "Zaryte crossbow", RUBY_E, "Ruby bolts (e)");
        var stats = Map.of(RUBY_E, bolts(), ZARYTE, weapon(110, 0, false));
        var plan = plan(ZILYANA, RANGED, Tier.ENTRY, own, names, stats);
        assertTrue(has(plan.blockers(), "Weapon"), "Entry keeps its ceiling: " + plan.gear().get(WEAPON).shown());
    }
    @Test void anOwnedRangedWeaponOutsideTheRowIsUsedByStats() {
        var own = armourOnly(ZILYANA, RANGED, Tier.ENTRY); own.put(861, 1); own.put(892, 500);
        var names = Map.of(861, "Magic shortbow", 892, "Rune arrow");
        var stats = Map.of(861, weapon(69, 0, true), 892, new Stats(ItemFacts.AMMO, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 49, 0, 0));
        var plan = plan(ZILYANA, RANGED, Tier.ENTRY, own, names, stats);
        assertEquals(861, plan.gear().get(WEAPON).shown().id());
        assertTrue(has(plan.checks(), "picked from your bank"), plan.checks().toString());
        assertNotEquals(BestBankPlanner.Verdict.READY, plan.verdict(), "a stats pick must be checked by the player");
    }
    @Test void aRangedWeaponWithNoUsableAmmoIsNotPickedByStats() {
        var own = armourOnly(ZILYANA, RANGED, Tier.ENTRY); own.put(861, 1);
        var plan = plan(ZILYANA, RANGED, Tier.ENTRY, own, Map.of(861, "Magic shortbow"), Map.of(861, weapon(69, 0, true)));
        assertTrue(has(plan.blockers(), "Weapon"), "nothing can fire, so it stays blocked");
    }
    @Test void magicPlansFallBackToAnOwnedStaffAndAskForRunes() {
        var plan = plan("Kraken", MAGIC, Tier.OPTIMAL, new HashMap<>(Map.of(AHRIM_STAFF, 1)), Map.of(AHRIM_STAFF, "Ahrim's staff"), Map.of(AHRIM_STAFF, weapon(0, 15, true)));
        assertEquals(AHRIM_STAFF, plan.gear().get(WEAPON).shown().id());
        assertTrue(has(plan.checks(), "does not plan spells"));
    }
    @Test void armourEqualOrBetterInEveryStatAlwaysWins() {
        var own = new HashMap<>(BestBankPlannerJUnitTest.ownAll(TieredOutfitterCatalog.find(ZILYANA, RANGED, Tier.ENTRY).profile())); own.put(HUEY_BODY, 1);
        var names = Map.of(BLACK_BODY, "Black d'hide body", HUEY_BODY, "Hueycoatl hide body");
        var stats = Map.of(BLACK_BODY, body(30, 30, 38, 45, 45, 50, 0), HUEY_BODY, body(30, 55, 47, 60, 56, 55, 3));
        assertEquals(HUEY_BODY, plan(ZILYANA, RANGED, Tier.ENTRY, own, names, stats).gear().get(BODY).shown().id());
    }
    @Test void aSmallGainWithALossStillNeedsTheThreshold() {
        var own = new HashMap<>(BestBankPlannerJUnitTest.ownAll(TieredOutfitterCatalog.find(ZILYANA, RANGED, Tier.ENTRY).profile())); own.put(HUEY_BODY, 1);
        var names = Map.of(BLACK_BODY, "Black d'hide body", HUEY_BODY, "Hueycoatl hide body");
        var stats = Map.of(BLACK_BODY, body(30, 30, 38, 45, 45, 50, 0), HUEY_BODY, body(30, 55, 47, 60, 56, 40, 3));
        assertEquals(BLACK_BODY, plan(ZILYANA, RANGED, Tier.ENTRY, own, names, stats).gear().get(BODY).shown().id());
    }
    @Test void aMissingExtraIsAnUpgradeNotABlocker() {
        var own = new HashMap<>(BestBankPlannerJUnitTest.ownAll(TieredOutfitterCatalog.find(ZILYANA, RANGED, Tier.ENTRY).profile()));
        own.remove(BLOWPIPE); own.remove(13128);
        var plan = plan(ZILYANA, RANGED, Tier.ENTRY, own, Map.of(), Map.of());
        assertFalse(has(plan.blockers(), "Switch Toxic blowpipe"), plan.blockers().toString());
        assertTrue(has(plan.upgrades(), "Toxic blowpipe"), plan.upgrades().toString());
    }
    @Test void curatedAndSwitchRowItemsStillBlock() {
        assertTrue(WikiStrategyLoadouts.bosses().stream().anyMatch(b -> WikiStrategyLoadouts.methods(b).stream().anyMatch(m -> anySwitch(b, m))), "fixture needs a Switch row");
    }
    private static boolean anySwitch(String boss, String method) {
        var profile = WikiStrategyLoadouts.find(boss, method, Tier.RECOMMENDED);
        if (profile == null) return false;
        for (Item it : profile.profile().inventory()) if (it != null && WikiStrategyLoadouts.requiredSwitch(boss, method, it.name())) return true;
        return false;
    }
}
