package gg.feral.burrow.core;

import gg.feral.burrow.core.BestBankPlanner.*;
import gg.feral.burrow.core.OwnedGearStats.Focus;
import gg.feral.burrow.core.OwnedGearStats.Stats;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CP-7 (owner native QA 2026-11-01 17:58 / 18:01): Best in Bank uses the player's OWN gear when the Wiki plan's
 * items are not owned. Stats below mirror the owner's in-game tooltips (Zombie axe, Dragon defender, Mixed hide top);
 * IDs 9000xx are synthetic fixtures, not claims about real item IDs.
 */
public final class Cp7OwnedBestInBankJUnitTest {
    static final AccountContext MAX = BestBankPlannerJUnitTest.MAX;
    static final int ZOMBIE_AXE = 900001, MIXED_TOP = 900002, PLATEBODY = 900003, PRAYER_TOP = 900004, DEFENDER = 12954;
    // slot ids: RuneLite equipment slots (weapon 3, shield 5, body 4)
    static Stats s(int slot, boolean twoH, double atkStab, double atkSlash, double atkCrush, double amagic, double arange,
                   double def, double str, double rstr, double prayer) {
        return new Stats(slot, twoH, atkStab, atkSlash, atkCrush, amagic, arange, def, def, def, def, def, str, rstr, 0, prayer);
    }
    static final Map<Integer,Stats> STATS = Map.of(
        ZOMBIE_AXE, s(3, false, -3, 99, 92, 0, 0, 0, 102, 0, 0),
        DEFENDER, s(5, false, 25, 24, 23, -3, -2, 24, 6, 0, 0),
        MIXED_TOP, s(4, false, 4, -2, -3, -15, 27, 35, 2, 0, 0),
        PLATEBODY, s(4, false, 0, 0, 0, -30, -10, 80, 0, 0, 0),
        PRAYER_TOP, s(4, false, 0, 0, 0, 0, 0, 0, 0, 0, 6));
    static final Map<Integer,String> NAMES = Map.of(ZOMBIE_AXE, "Zombie axe", DEFENDER, "Dragon defender",
        MIXED_TOP, "Mixed hide top", PLATEBODY, "Rune platebody", PRAYER_TOP, "Monk's robe top");
    static Map<Integer,Integer> bank() {
        Map<Integer,Integer> m = new HashMap<>();
        for (int id : STATS.keySet()) m.put(id, 1);
        return m;
    }
    static Plan plan(String boss, String method, Tier t, Map<Integer,Integer> own, Focus f) {
        return BestBankPlanner.plan(boss, method, t, BestBankPlannerJUnitTest.bank(own), MAX, ItemFacts.catalogDerived(), NAMES, STATS, f);
    }

    @Test void amoxliatlWithoutMacuahuitlUsesOwnedZombieAxeAndDefender() {
        String boss = "Amoxliatl", method = "Melee, full Inquisitor with mace";
        var p = plan(boss, method, Tier.RECOMMENDED, bank(), Focus.DAMAGE);
        assertTrue(p.blockers().stream().noneMatch(b -> b.contains("CORE Weapon")), String.valueOf(p.blockers()));
        assertNotEquals(Verdict.NOT_READY, p.verdict(), p.headline() + " " + p.blockers());
        assertEquals(ZOMBIE_AXE, p.gear().get(4).shown().id(), "strongest owned melee weapon");
        assertTrue(p.gear().get(4).note().contains("attack style"), p.gear().get(4).note());
        assertEquals(DEFENDER, p.gear().get(6).shown().id(), "1H weapon: owned defender fills the offhand");
        assertTrue(p.checks().stream().noneMatch(c -> c.contains("set incomplete (0/")), "no warning for a set you own none of: " + p.checks());
    }

    @Test void focusChoosesStrengthDefenceOrPrayerForAnEmptyBodySlot() {
        String boss = "Amoxliatl", method = "Melee, full Inquisitor with mace";
        assertEquals(MIXED_TOP, plan(boss, method, Tier.RECOMMENDED, bank(), Focus.DAMAGE).gear().get(5).shown().id());
        assertEquals(PLATEBODY, plan(boss, method, Tier.RECOMMENDED, bank(), Focus.DEFENCE).gear().get(5).shown().id());
        assertEquals(PRAYER_TOP, plan(boss, method, Tier.RECOMMENDED, bank(), Focus.PRAYER).gear().get(5).shown().id());
        var body = plan(boss, method, Tier.RECOMMENDED, bank(), Focus.DAMAGE).gear().get(5);
        assertEquals(GearStatus.OWNED_SUBSTITUTE, body.status());
        assertTrue(body.note().startsWith("Best owned for damage") && body.note().contains("wear requirements not checked"), body.note());
    }

    @Test void ownedWikiItemsAreNeverReplacedByStatPicks() {
        var rec = BestBankPlannerJUnitTest.profile("Scurrius", "Standard melee", Tier.RECOMMENDED);
        var own = BestBankPlannerJUnitTest.ownAll(rec);
        own.putAll(bank());
        var p = plan("Scurrius", "Standard melee", Tier.RECOMMENDED, own, Focus.DAMAGE);
        for (int i = 0; i < 11; i++) {
            var planned = rec.gear().get(i);
            if (planned != null && planned.id() > 0 && i != 10) // ring: governed variant, owned exactly here
                assertEquals(planned.id(), p.gear().get(i).shown().id(), "slot " + i + " keeps the owned Wiki item");
        }
    }

    @Test void rangedPlansNeverGetAStatPickedWeapon() {
        var p = plan("Brutus", "Ranged (free-to-play)", Tier.RECOMMENDED, bank(), Focus.DAMAGE);
        assertNotEquals(ZOMBIE_AXE, p.gear().get(4).shown() == null ? -1 : p.gear().get(4).shown().id());
        assertEquals(Verdict.NOT_READY, p.verdict(), "no owned bow: still a real blocker");
    }

    @Test void voidOptionIsOfferedOnlyForAFullOwnedSet() {
        Map<Integer,Integer> own = Map.of(1, 1, 2, 1, 3, 1, 4, 1);
        Map<Integer,String> names = Map.of(1, "Void melee helm", 2, "Elite void top (l)", 3, "Elite void robe", 4, "Void knight gloves");
        var opt = OwnedGearStats.voidOption("Melee", own, names);
        assertEquals("Elite Void (melee)", opt.orElseThrow().label());
        assertTrue(OwnedGearStats.voidOption("Ranged", own, names).isEmpty(), "no ranger helm owned");
        assertTrue(OwnedGearStats.voidOption("Melee", Map.of(1, 1, 2, 1, 3, 1), names).isEmpty(), "gloves missing");
    }

    // ---------- Owner rule 18:03: observe the Wiki item, find the closest-statted owned item, choose it ----------
    static final int DAGGER = 900010;
    @Test void closestMatchKeepsTheWikiItemsPurpose() {
        Stats crushWeapon = s(3, true, 0, 0, 95, 0, 0, 0, 85, 0, 0);          // a crush/strength Wiki weapon
        Map<Integer,Stats> st = new HashMap<>(STATS);
        st.put(DAGGER, s(3, false, 40, 25, -4, 1, 0, 1, 40, 0, 0));          // stab dagger
        Map<Integer,Integer> own = bank(); own.put(DAGGER, 1);
        var w = OwnedGearStats.closest(3, "Melee", crushWeapon, "Wiki mace", own, st, NAMES, Set.of()).orElseThrow();
        assertEquals(ZOMBIE_AXE, w.itemId(), "crush/strength profile matched");
        assertTrue(w.why().startsWith("Closest owned match to Wiki mace") && w.why().contains("crush atk (Wiki +95)"), w.why());

        Stats prayerBody = s(4, false, 0, 0, 0, 0, 0, 2, 0, 0, 5);              // a prayer Wiki body
        assertEquals(PRAYER_TOP, OwnedGearStats.closest(4, "Melee", prayerBody, "Wiki robe", own, st, NAMES, Set.of()).orElseThrow().itemId());
        Stats tankBody = s(4, false, 0, 0, 0, -20, -5, 85, 0, 0, 0);
        assertEquals(PLATEBODY, OwnedGearStats.closest(4, "Melee", tankBody, "Wiki platebody", own, st, NAMES, Set.of()).orElseThrow().itemId());
    }

    @Test void nothingCloserThanAnEmptySlotLeavesTheSlotEmpty() {
        Stats magicHat = s(0, false, 0, 0, 0, 20, 0, 5, 0, 0, 0);             // head slot: nothing owned for head
        assertTrue(OwnedGearStats.closest(0, "Magic", magicHat, "Wiki hat", bank(), STATS, NAMES, Set.of()).isEmpty());
        Stats rangedBody = s(4, false, 0, 0, 0, 0, 30, 0, 0, 0, 0);
        // A ranged-attack Wiki body is closest to the owned Mixed hide top (+27 ranged), not the platebody.
        assertEquals(MIXED_TOP, OwnedGearStats.closest(4, "Ranged", rangedBody, "Wiki body", bank(), STATS, NAMES, Set.of()).orElseThrow().itemId());
    }

    @Test void matchIsTheDefaultAndUsesTheWikiWeaponsStats() {
        String boss = "Amoxliatl", method = "Melee, full Inquisitor with mace";
        var rec = BestBankPlannerJUnitTest.profile(boss, method, Tier.RECOMMENDED);
        Map<Integer,Stats> st = new HashMap<>(STATS);
        for (Tier t : Tier.values()) { var sel = TieredOutfitterCatalog.find(boss, method, t);   // every tier's Wiki weapon target
            if (sel != null && sel.profile().gear().get(4) != null) st.put(sel.profile().gear().get(4).id(), s(3, true, 0, 0, 95, 0, 0, 0, 85, 0, 0)); }
        st.put(DAGGER, s(3, false, 40, 25, -4, 1, 0, 1, 40, 0, 0));
        Map<Integer,Integer> own = bank(); own.put(DAGGER, 1);
        Map<Integer,String> names = new HashMap<>(NAMES); names.put(DAGGER, "Dragon dagger");
        var p = BestBankPlanner.plan(boss, method, Tier.RECOMMENDED, BestBankPlannerJUnitTest.bank(own), MAX,
            ItemFacts.catalogDerived(), names, st, null); // null focus = default MATCH
        assertEquals(ZOMBIE_AXE, p.gear().get(4).shown().id(), p.gear().get(4).note());
        assertTrue(p.gear().get(4).note().startsWith("Closest owned match to "), p.gear().get(4).note());
        assertEquals(OwnedGearStats.Focus.MATCH, OwnedGearStats.Focus.parse(null));
    }

    /** Owner QA 18:05 (Lizardman ranged): an owned Antler guard is better than the owned Wiki book, so it is shown. */
    @Test void ownedAntlerGuardReplacesAnOwnedBookOnARangedPlan() {
        Stats book = s(5, false, 0, 0, 0, 6, 6, 6, 0, 0, 5);                  // god-book-like offhand
        Stats antler = new Stats(5, false, 15, 15, 15, 9, 9, -15, -15, -15, -15, -15, 5, 2, 0, 5); // owner tooltip
        Map<Integer,Stats> st = Map.of(1, book, 2, antler);
        var up = OwnedGearStats.upgradeOver(5, "Ranged", OwnedGearStats.Focus.MATCH, book, "Book of law",
            Map.of(2, 1), st, Map.of(2, "Antler guard"), Set.of());
        assertEquals(2, up.orElseThrow().itemId());
        assertTrue(up.get().why().startsWith("Owned upgrade over Book of law"), up.get().why());
        // A marginal item is not an upgrade (needs +10% and +3 on the plan's style score).
        Stats marginal = s(5, false, 0, 0, 0, 6, 7, 6, 0, 0, 5);
        assertTrue(OwnedGearStats.upgradeOver(5, "Ranged", OwnedGearStats.Focus.MATCH, book, "Book of law",
            Map.of(3, 1), Map.of(3, marginal), Map.of(3, "Other book"), Set.of()).isEmpty());
        // Never for the weapon slot.
        assertTrue(OwnedGearStats.upgradeOver(3, "Melee", OwnedGearStats.Focus.MATCH, book, "x", Map.of(2, 1), st, Map.of(), Set.of()).isEmpty());
    }

    @Test void plannerSwapsAnOwnedWikiPieceForAClearlyBetterOwnedOneAndReturnsItToTheBank() {
        var rec = BestBankPlannerJUnitTest.profile("Scurrius", "Standard melee", Tier.RECOMMENDED);
        var own = BestBankPlannerJUnitTest.ownAll(rec);
        int capeId = rec.gear().get(1).id();
        own.put(900020, 1);
        Map<Integer,Stats> st = new HashMap<>();
        st.put(capeId, s(1, false, 1, 1, 1, 1, 1, 11, 4, 0, 2));               // fire-cape-like
        st.put(900020, s(1, false, 4, 4, 4, 1, 1, 12, 8, 0, 2));               // a clearly stronger owned melee cape
        var p = BestBankPlanner.plan("Scurrius", "Standard melee", Tier.RECOMMENDED, BestBankPlannerJUnitTest.bank(own), MAX,
            ItemFacts.catalogDerived(), Map.of(900020, "Infernal-like cape"), st, null);
        assertEquals(900020, p.gear().get(1).shown().id(), p.gear().get(1).note());
        assertTrue(p.gear().get(1).note().startsWith("Owned upgrade over " + rec.gear().get(1).name()), p.gear().get(1).note());
        assertEquals(rec.gear().get(10).id(), p.gear().get(10).shown().id(), "other owned Wiki items untouched");
    }

    // ---------- CP-8: crossbow bolts (owner QA 18:21) ----------
    @Test void boltTiersFollowTheCrossbow() {
        assertEquals(6, OwnedGearStats.crossbowTier("Rune crossbow"));
        assertEquals(7, OwnedGearStats.crossbowTier("Armadyl crossbow"));
        assertEquals(5, OwnedGearStats.boltTier("Diamond bolts (e)"));
        assertEquals(6, OwnedGearStats.boltTier("Dragonstone bolts (e)"));
        assertEquals(7, OwnedGearStats.boltTier("Diamond dragon bolts (e)"));
        assertEquals(6, OwnedGearStats.boltTier("Broad bolts")); // Step 2: Wiki limits broad bolts to rune tier and above; CP-8 asserted 1
        assertEquals(-1, OwnedGearStats.boltTier("Bolt rack"));
        Map<Integer,Stats> st = Map.of(1, new Stats(13,false,0,0,0,0,0,0,0,0,0,0,0,100,0,0),
            2, new Stats(13,false,0,0,0,0,0,0,0,0,0,0,0,105,0,0), 3, new Stats(13,false,0,0,0,0,0,0,0,0,0,0,0,122,0,0));
        Map<Integer,String> nm = Map.of(1, "Broad bolts", 2, "Diamond bolts (e)", 3, "Diamond dragon bolts (e)");
        var pick = OwnedGearStats.bestBolts("Rune crossbow", 100, Map.of(1, 500, 2, 300, 3, 900), st, nm, Set.of()).orElseThrow();
        assertEquals(2, pick.itemId(), "diamond (e) over broad; dragon bolts can't be fired from a rune crossbow");
        assertEquals(100, (int) pick.score(), "bring the trip budget, not the whole stack");
        assertEquals(3, OwnedGearStats.bestBolts("Armadyl crossbow", 100, Map.of(1, 500, 2, 300, 3, 900), st, nm, Set.of()).orElseThrow().itemId());
        // A stack that covers the trip beats a stronger stack that does not.
        assertEquals(1, OwnedGearStats.bestBolts("Rune crossbow", 100, Map.of(1, 500, 2, 20), st, nm, Set.of()).orElseThrow().itemId());
    }

    @Test void lizardmanCrossbowPlanSwapsBroadForDiamondBolts() {
        for (String m : TieredOutfitterCatalog.methods("Lizardman Shamans")) for (Tier t : Tier.values()) {
            var sel = TieredOutfitterCatalog.find("Lizardman Shamans", m, t);
            if (sel == null) continue;
            var p = sel.profile();
            if (p.gear().get(4) == null || OwnedGearStats.crossbowTier(p.gear().get(4).name()) == 0 || p.gear().get(3) == null) continue;
            var own = BestBankPlannerJUnitTest.ownAll(p);
            own.put(900030, 1000);
            Map<Integer,Stats> st = new HashMap<>();
            st.put(p.gear().get(3).id(), new Stats(13,false,0,0,0,0,0,0,0,0,0,0,0,100,0,0));
            st.put(900030, new Stats(13,false,0,0,0,0,0,0,0,0,0,0,0,105,0,0));
            Map<Integer,String> nm = new HashMap<>(); nm.put(900030, "Diamond bolts (e)");
            nm.put(p.gear().get(4).id(), p.gear().get(4).name());
            var plan = BestBankPlanner.plan("Lizardman Shamans", m, t, BestBankPlannerJUnitTest.bank(own), MAX, ItemFacts.catalogDerived(), nm, st, null);
            var ammo = plan.gear().get(3);
            assertEquals(900030, ammo.shown().id(), ammo.note());
            assertEquals(Math.max(1, p.gear().get(3).amount()), ammo.shown().amount());
            return;
        }
        fail("fixture: a Lizardman crossbow plan with ammo exists");
    }
}
