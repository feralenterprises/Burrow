package gg.feral.burrow.core;

import gg.feral.burrow.bib.SlayerTask;
import gg.feral.burrow.core.BestBankPlanner.GearStatus;
import gg.feral.burrow.core.BestBankPlanner.Plan;
import gg.feral.burrow.core.BestBankPlanner.Verdict;
import gg.feral.burrow.core.OwnedGearStats.Focus;
import gg.feral.burrow.core.OwnedGearStats.Stats;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Step 2 regressions: ammo compatibility, Ranged cape order and live Slayer eligibility inside the existing planner.
 * Kits come from the real catalogue and are never edited. Bank fixtures own every planned item unless a test says so.
 */
public final class Step2SafetyRulesJUnitTest {
    static final AccountContext MAX = BestBankPlannerJUnitTest.MAX;
    static final int ASSEMBLER = 22109, ACCUMULATOR = 10499, QUIVER = 28947, NEITIZNOT = 10828, HELM_I = 11865;
    static final int DRAGON_ARROW = 11212, RUBY_E = 9242, SALVE_EI = 12018;
    static final int HEAD = 0, CAPE = 1, NECK = 2, AMMO = 3, WEAPON = 4;

    static Stats ammoStats(double rangedStrength) {
        return new Stats(ItemFacts.AMMO, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, rangedStrength, 0, 0);
    }
    static Stats gearStats(int slot, double strength) {
        return new Stats(slot, false, 10, 10, 10, 0, 0, 5, 5, 5, 5, 5, strength, 0, 0, 0);
    }
    static Map<Integer,Integer> planned(String boss, String method, Tier tier) {
        Map<Integer,Integer> own = new HashMap<>();
        var profile = TieredOutfitterCatalog.find(boss, method, tier).profile();
        for (Item it : profile.gear()) if (it != null && it.id() > 0) own.put(it.id(), Math.max(1000, it.amount()));
        for (Item it : profile.inventory()) if (it != null && it.id() > 0) own.merge(it.id(), Math.max(1000, it.amount()), Integer::sum);
        return own;
    }
    static Plan plan(String boss, String method, Tier tier, Map<Integer,Integer> own, Map<Integer,String> names,
                     Map<Integer,Stats> stats, AccountContext ctx, SlayerTask task) {
        return BestBankPlanner.plan(boss, method, tier, BestBankPlannerJUnitTest.bank(own), ctx, ItemFacts.catalogDerived(),
            names, stats, Focus.MATCH, task);
    }
    static Plan plan(String boss, String method, Tier tier, Map<Integer,Integer> own, SlayerTask task) {
        return plan(boss, method, tier, own, Map.of(ASSEMBLER, "Ava's assembler", ACCUMULATOR, "Ava's accumulator", QUIVER, "Dizana's quiver",
            NEITIZNOT, "Helm of Neitiznot", HELM_I, "Slayer helmet (i)"), Map.of(), MAX, task);
    }
    static boolean has(List<String> lines, String text) { return lines.stream().anyMatch(l -> l.contains(text)); }

    // ---------- audit findings ----------
    static final int FULL_SEAS = 11905, SCEPTRE = 28585, UNCHARGED_SEAS = 11908, SWAMP = 12899, TOXIC_E = 22292;
    static final String MAGIC = "Wiki · Magic";
    static Plan kraken(Tier tier, AccountContext ctx, int... weapons) {
        Map<Integer,Integer> own = new HashMap<>();
        for (int id : weapons) own.put(id, 1);
        return plan("Kraken", MAGIC, tier, own, Map.of(FULL_SEAS, "Trident of the seas (full)", SCEPTRE, "Warped sceptre"), Map.of(), ctx, SlayerTask.UNKNOWN);
    }
    static AccountContext magicLevel(int level) {
        Map<String,Integer> levels = new HashMap<>(MAX.realLevels());
        levels.put("Magic", level);
        return new AccountContext(levels, MAX.questStates());
    }
    @Test void ownedFullTridentBeatsTheSceptreAboveEntry() {
        for (Tier tier : List.of(Tier.RECOMMENDED, Tier.OPTIMAL)) {
            var plan = kraken(tier, magicLevel(95), FULL_SEAS, SCEPTRE);
            assertEquals(FULL_SEAS, plan.gear().get(WEAPON).shown().id(), tier.name());
            assertEquals(GearStatus.OWNED_SUBSTITUTE, plan.gear().get(WEAPON).status());
            assertTrue(has(plan.checks(), "charges cannot be read"), "charges stay a visible check");
        }
    }
    @Test void ownedTridentClearsTheMissingWeaponBlocker() {
        var plan = kraken(Tier.OPTIMAL, magicLevel(95), FULL_SEAS);
        assertEquals(FULL_SEAS, plan.gear().get(WEAPON).shown().id());
        assertFalse(has(plan.blockers(), "Weapon:"), plan.blockers().toString());
    }
    @Test void entryNeverGainsAWeaponAboveItsTier() {
        assertEquals(SCEPTRE, kraken(Tier.ENTRY, magicLevel(95), FULL_SEAS, SCEPTRE).gear().get(WEAPON).shown().id());
    }
    @Test void strongerPlannedWeaponIsNotReplaced() {
        assertEquals(SWAMP, kraken(Tier.OPTIMAL, magicLevel(95), SWAMP, FULL_SEAS).gear().get(WEAPON).shown().id());
    }
    @Test void strongerTridentWinsWhenBothAreOwned() {
        assertEquals(TOXIC_E, kraken(Tier.OPTIMAL, magicLevel(95), TOXIC_E, FULL_SEAS, SCEPTRE).gear().get(WEAPON).shown().id());
    }
    @Test void unchargedTridentAndLowMagicAreIgnored() {
        assertEquals(SCEPTRE, kraken(Tier.OPTIMAL, magicLevel(95), UNCHARGED_SEAS, SCEPTRE).gear().get(WEAPON).shown().id());
        assertEquals(SCEPTRE, kraken(Tier.OPTIMAL, magicLevel(70), FULL_SEAS, SCEPTRE).gear().get(WEAPON).shown().id());
    }
    @Test void swampTridentLevelIsCheckedOnItsOwn() {
        assertEquals(FULL_SEAS, kraken(Tier.OPTIMAL, magicLevel(76), TOXIC_E, FULL_SEAS, SCEPTRE).gear().get(WEAPON).shown().id());
    }
    @Test void curatedMethodTakesTheTridentOverItsEntryStaff() {
        String rex = "Rex safespot with Guthan healing switch";
        var own = new HashMap<>(Map.of(FULL_SEAS, 1, 12658, 1));
        var names = Map.of(FULL_SEAS, "Trident of the seas (full)", 12658, "Iban's staff (u)");
        for (Tier tier : List.of(Tier.RECOMMENDED, Tier.OPTIMAL))
            assertEquals(FULL_SEAS, plan("Dagannoth Kings", rex, tier, own, names, Map.of(), magicLevel(95), SlayerTask.UNKNOWN).gear().get(WEAPON).shown().id(), tier.name());
        assertEquals(12658, plan("Dagannoth Kings", rex, Tier.ENTRY, own, names, Map.of(), magicLevel(95), SlayerTask.UNKNOWN).gear().get(WEAPON).shown().id());
    }
    @Test void bankMatchTreatsTheFullCopyAsAReviewedVariant() {
        var profile = TieredOutfitterCatalog.find("The Mimic", MAGIC, Tier.ENTRY).profile();
        var match = BestBankMatches.gear(profile, Map.of(FULL_SEAS, 1), Map.of()).get(WEAPON);
        assertEquals(BestBankMatches.Status.REVIEWED_VARIANT, match.status());
        assertEquals(FULL_SEAS, match.observedId());
    }
    @Test void rangedBootsFollowTheFillMode() {
        int dragon = 11840, brimstone = 22951;
        var own = new HashMap<>(planned("Dagannoth Kings", "Prime, ranged", Tier.RECOMMENDED));
        own.put(brimstone, 1);
        var stats = Map.of(dragon, new Stats(ItemFacts.BOOTS, false, 0, 0, 0, -3, -1, 16, 17, 18, 0, 0, 4, 0, 0, 0),
            brimstone, new Stats(ItemFacts.BOOTS, false, 3, 0, 0, 3, 5, 10, 10, 10, 5, 5, 0, 0, 0, 0));
        var names = Map.of(dragon, "Dragon boots", brimstone, "Boots of brimstone");
        var bank = BestBankPlannerJUnitTest.bank(own);
        int matchPick = BestBankPlanner.plan("Dagannoth Kings", "Prime, ranged", Tier.RECOMMENDED, bank, MAX, ItemFacts.catalogDerived(), names, stats, Focus.MATCH, SlayerTask.UNKNOWN).gear().get(9).shown().id();
        int defencePick = BestBankPlanner.plan("Dagannoth Kings", "Prime, ranged", Tier.RECOMMENDED, bank, MAX, ItemFacts.catalogDerived(), names, stats, Focus.DEFENCE, SlayerTask.UNKNOWN).gear().get(9).shown().id();
        assertEquals(brimstone, matchPick, "default mode favours the plan's style");
        assertEquals(dragon, defencePick, "Fill Defence favours total defence");
    }
    @Test void plannedSeasTridentAcceptsTheFullCopy() {
        var own = planned("The Mimic", MAGIC, Tier.ENTRY);
        assertNotNull(own.remove(11907), "kit must plan the plain trident");
        own.put(FULL_SEAS, 1);
        var cell = plan("The Mimic", MAGIC, Tier.ENTRY, own, SlayerTask.UNKNOWN).gear().get(WEAPON);
        assertEquals(FULL_SEAS, cell.shown().id());
        assertTrue(cell.status().physicallyOwned());
    }
    @Test void aSeasTridentNeverCountsAsASwampTrident() {
        assertEquals(-1, gg.feral.burrow.bib.MagicWeaponRules.ownedVariant(SWAMP, Map.of(FULL_SEAS, 1)));
    }

    @Test void unknownDefenceKeepsAVerifyOnASwappedHelm() {
        String boss = "Cerberus", method = "Wiki · Melee";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(NEITIZNOT, 1);
        var p = plan(boss, method, Tier.ENTRY, own, Map.of(NEITIZNOT, "Helm of Neitiznot"), Map.of(), AccountContext.UNKNOWN, SlayerTask.NONE);
        assertEquals(NEITIZNOT, p.gear().get(HEAD).shown().id());
        assertTrue(p.checks().stream().anyMatch(c -> c.startsWith("Head: ") && !c.contains("Slayer task")), String.valueOf(p.checks()));
        assertNotEquals(Verdict.READY, p.verdict());
    }
    @Test void knownLowDefenceNeverGetsTheHelm() {
        String boss = "Cerberus", method = "Wiki · Melee";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(NEITIZNOT, 1);
        var low = new AccountContext(Map.of("Defence", 40), MAX.questStates());
        var p = plan(boss, method, Tier.ENTRY, own, Map.of(NEITIZNOT, "Helm of Neitiznot"), Map.of(), low, SlayerTask.NONE);
        assertNotEquals(NEITIZNOT, p.gear().get(HEAD).shown().id());
    }
    @Test void swappedCapeWithoutRegistryEntryKeepsAVerify() {
        String boss = "Dagannoth Kings", method = "Prime, ranged";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(QUIVER, 1);
        var p = plan(boss, method, Tier.ENTRY, own, SlayerTask.UNKNOWN);
        assertEquals(QUIVER, p.gear().get(CAPE).shown().id());
        if (ItemRequirementRegistry.find(QUIVER).isEmpty())
            assertTrue(has(p.checks(), "Cape:"), String.valueOf(p.checks()));
    }
    @Test void chargedBowsCanNeverBeReady() {
        String boss = "K'ril Tsutsaroth", method = "Wiki · Ranged";
        var p = plan(boss, method, Tier.ENTRY, planned(boss, method, Tier.ENTRY), SlayerTask.UNKNOWN);
        assertTrue(has(p.checks(), "charges cannot be read"), String.valueOf(p.checks()));
        assertNotEquals(Verdict.READY, p.verdict());
    }
    @Test void optionalShieldMayGiveWayToATwoHandedFallback() {
        String boss = "Cerberus", method = "Wiki · Melee";
        var profile = TieredOutfitterCatalog.find(boss, method, Tier.ENTRY).profile();
        var own = planned(boss, method, Tier.ENTRY);
        own.remove(profile.gear().get(WEAPON).id());
        for (Tier t : Tier.values()) own.remove(TieredOutfitterCatalog.find(boss, method, t).profile().gear().get(WEAPON).id());
        int greatsword = 900100;
        own.put(greatsword, 1);
        var p = BestBankPlanner.plan(boss, method, Tier.ENTRY, BestBankPlannerJUnitTest.bank(own), MAX, ItemFacts.catalogDerived(),
            Map.of(greatsword, "Test greatsword"), Map.of(greatsword, new Stats(ItemFacts.WEAPON, true, 90, 90, 90, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0, 0)),
            Focus.DAMAGE, SlayerTask.UNKNOWN);
        assertEquals(greatsword, p.gear().get(WEAPON).shown().id(), String.valueOf(p.gear().get(WEAPON)));
        assertNull(p.gear().get(6).shown(), "an optional stat shield comes off a two-handed weapon");
    }

    // ---------- Slayer reader bad data ----------
    static class FakeSource implements gg.feral.burrow.bib.SlayerTaskResolver.Source {
        int count = 50, target = 7, bossId = 3, master = 5, areaId = 0;
        List<Integer> taskRows = List.of(1), bossRows = List.of(2), areaRows = List.of(3);
        Object[] bossTask = {1}, taskName = {"HYDRAS"}, areaName = {"the Lighthouse"};
        RuntimeException thrown;
        void maybeThrow() { if (thrown != null) throw thrown; }
        @Override public int count() { return count; }
        @Override public int target() { return target; }
        @Override public int bossId() { return bossId; }
        @Override public int master() { return master; }
        @Override public int areaId() { return areaId; }
        @Override public List<Integer> taskRows(int t) { maybeThrow(); return taskRows; }
        @Override public List<Integer> bossRows(int b) { maybeThrow(); return bossRows; }
        @Override public Object[] bossTaskField(int r) { return bossTask; }
        @Override public Object[] taskNameField(int r) { return taskName; }
        @Override public List<Integer> areaRows(int a) { return areaRows; }
        @Override public Object[] areaNameField(int r) { return areaName; }
    }
    static SlayerTask.State state(FakeSource s) { return gg.feral.burrow.bib.SlayerTaskResolver.resolve(s).state; }
    @Test void readerResolvesNormalAndBossTasks() {
        var s = new FakeSource();
        assertEquals(SlayerTask.State.ACTIVE, state(s));
        s.target = gg.feral.burrow.bib.SlayerTaskResolver.BOSS_TASK_TARGET;
        assertEquals(SlayerTask.State.ACTIVE, state(s));
        var none = new FakeSource();
        none.count = 0;
        none.taskRows = null;
        assertEquals(SlayerTask.State.NONE, state(none));
    }
    @Test void readerFailsClosedOnBadData() {
        var empty = new FakeSource(); empty.taskRows = List.of();
        var nullRows = new FakeSource(); nullRows.taskRows = null;
        var nullField = new FakeSource(); nullField.taskName = null;
        var emptyField = new FakeSource(); emptyField.taskName = new Object[0];
        var wrongType = new FakeSource(); wrongType.taskName = new Object[]{5};
        var bossBad = new FakeSource(); bossBad.target = 98; bossBad.bossTask = new Object[]{"x"};
        var bossNone = new FakeSource(); bossNone.target = 98; bossNone.bossRows = List.of();
        var threw = new FakeSource(); threw.thrown = new IllegalStateException("login transition");
        for (var s : List.of(empty, nullRows, nullField, emptyField, wrongType, bossBad, bossNone, threw))
            assertEquals(SlayerTask.State.UNKNOWN, state(s));
    }
    @Test void readerKeepsKonarAreaUnknownWhenItCannotBeRead() {
        var s = new FakeSource();
        s.master = SlayerTask.MASTER_KONAR;
        s.areaId = 9;
        assertEquals("the Lighthouse", gg.feral.burrow.bib.SlayerTaskResolver.resolve(s).area);
        s.areaName = new Object[]{7};
        var t = gg.feral.burrow.bib.SlayerTaskResolver.resolve(s);
        assertEquals(SlayerTask.State.ACTIVE, t.state);
        assertNull(t.area);
        assertEquals(gg.feral.burrow.bib.BossTaskMatrix.Coverage.UNCONFIRMED, gg.feral.burrow.bib.BossTaskMatrix.coverage("Abyssal Sire",
            SlayerTask.of(5, "Abyssal demons", SlayerTask.MASTER_KONAR, t.area)));
    }

    // ---------- remaining P0 gaps ----------
    @Test void dragonfireShieldsAreOnlyMandatoryWhereDragonfireExists() {
        var shield = new Item(1, "Dragonfire shield", 1);
        assertTrue(SlotCriticality.blocking("Vorkath", 6, shield));
        assertTrue(SlotCriticality.blocking("King Black Dragon", 6, shield));
        assertFalse(SlotCriticality.blocking("Cerberus", 6, shield));
        assertFalse(SlotCriticality.blocking("Kraken", 6, new Item(2, "Dragonfire ward", 1)));
        assertFalse(SlotCriticality.blocking("Commander Zilyana", 6, new Item(3, "Ancient wyvern shield", 1)));
    }
    @Test void cerberusNoLongerBlocksOnAStatShield() {
        String boss = "Cerberus", method = "Wiki · Melee";
        var own = planned(boss, method, Tier.ENTRY);
        own.remove(TieredOutfitterCatalog.find(boss, method, Tier.ENTRY).profile().gear().get(6).id());
        var p = plan(boss, method, Tier.ENTRY, own, SlayerTask.UNKNOWN);
        assertFalse(has(p.blockers(), "Shield"), String.valueOf(p.blockers()));
    }
    @Test void vorkathStillBlocksWithoutDragonfireProtection() {
        String boss = "Vorkath", method = "Ranged crossbow";
        var own = planned(boss, method, Tier.ENTRY);
        own.remove(TieredOutfitterCatalog.find(boss, method, Tier.ENTRY).profile().gear().get(6).id());
        var p = plan(boss, method, Tier.ENTRY, own, SlayerTask.UNKNOWN);
        assertTrue(has(p.blockers(), "Shield"), String.valueOf(p.blockers()));
    }
    @Test void twoHandedGuardSeesRequiredShields() {
        Item adfs = new Item(1, "Anti-dragon shield", 1);
        var cell = new BestBankPlanner.GearCell(adfs, adfs, GearStatus.OWNED, "");
        List<BestBankPlanner.GearCell> gear = new ArrayList<>(Collections.nCopies(11, new BestBankPlanner.GearCell(null, null, GearStatus.EMPTY, "")));
        gear.set(6, cell);
        assertTrue(BestBankPlanner.shieldRequired("Vorkath", gear));
        assertFalse(BestBankPlanner.shieldRequired("Scurrius", gear));
    }
    @Test void occupiedSlotIsNotSetMembership() {
        assertFalse(SlotCriticality.sameSet(new Item(1, "Guthan's platebody", 1), new Item(2, "Rune platebody", 1)));
        assertTrue(SlotCriticality.sameSet(new Item(1, "Guthan's platebody", 1), new Item(3, "Guthan's platebody 100", 1)));
        assertTrue(SlotCriticality.sameSet(new Item(1, "Rune platebody", 1), new Item(2, "Fighter torso", 1)));
    }
    @Test void statFilledSubstituteDoesNotCompleteASet() {
        String boss = "Dagannoth Kings", method = "Rex safespot with Guthan healing switch";
        var profile = TieredOutfitterCatalog.find(boss, method, Tier.RECOMMENDED).profile();
        Item guthanBody = profile.gear().get(5);
        var own = planned(boss, method, Tier.RECOMMENDED);
        own.remove(guthanBody.id());
        int runeBody = 1127;
        own.put(runeBody, 1);
        var p = BestBankPlanner.plan(boss, method, Tier.RECOMMENDED, BestBankPlannerJUnitTest.bank(own), MAX, ItemFacts.catalogDerived(),
            Map.of(runeBody, "Rune platebody"), Map.of(runeBody, gearStats(ItemFacts.BODY, 5)), Focus.DAMAGE, SlayerTask.UNKNOWN);
        assertEquals(runeBody, p.gear().get(5).shown().id(), "the substitute must really be worn: " + p.gear().get(5));
        assertTrue(has(p.blockers(), "set incomplete"), "method depends on the set: " + p.blockers());
    }
    @Test void missingTortuganWarnsButDoesNotBlock() {
        String boss = "Shellbane gryphon", method = "Wiki · Melee";
        var own = planned(boss, method, Tier.ENTRY);
        var withShield = plan(boss, method, Tier.ENTRY, own, SlayerTask.UNKNOWN);
        assertFalse(has(withShield.checks(), "Tortugan shield is recommended"), String.valueOf(withShield.checks()));
        own.remove(TieredOutfitterCatalog.find(boss, method, Tier.ENTRY).profile().gear().get(CAPE).id());
        var without = plan(boss, method, Tier.ENTRY, own, SlayerTask.UNKNOWN);
        assertTrue(has(without.checks(), "Tortugan shield is recommended"), String.valueOf(without.checks()));
        assertFalse(has(without.blockers(), "Tortugan"), String.valueOf(without.blockers()));
    }
    @Test void tortuganIsNotUpgradedAwayForStats() {
        String boss = "Shellbane gryphon", method = "Wiki · Melee";
        var own = planned(boss, method, Tier.ENTRY);
        int tortugan = TieredOutfitterCatalog.find(boss, method, Tier.ENTRY).profile().gear().get(CAPE).id();
        int strongCape = 21295;
        own.put(strongCape, 1);
        var p = BestBankPlanner.plan(boss, method, Tier.ENTRY, BestBankPlannerJUnitTest.bank(own), MAX, ItemFacts.catalogDerived(),
            Map.of(tortugan, "Tortugan shield", strongCape, "Infernal cape"),
            Map.of(tortugan, gearStats(ItemFacts.CAPE, 0), strongCape, gearStats(ItemFacts.CAPE, 40)), Focus.DAMAGE, SlayerTask.UNKNOWN);
        assertEquals(tortugan, p.gear().get(CAPE).shown().id(), String.valueOf(p.gear().get(CAPE)));
        assertFalse(SlotCriticality.blocking(boss, 1, new Item(1, "Tortugan shield", 1)), "recommended, not mandatory");
    }
    @Test void hydraBootsWithoutHeatProtectionNeedVerifying() {
        var entry = plan(HYDRA, HYDRA_M, Tier.ENTRY, planned(HYDRA, HYDRA_M, Tier.ENTRY), SlayerTask.UNKNOWN);
        assertFalse(has(entry.checks(), "Karuulm heat"), "Boots of brimstone protect: " + entry.checks());
        var rec = plan(HYDRA, HYDRA_M, Tier.RECOMMENDED, planned(HYDRA, HYDRA_M, Tier.RECOMMENDED), SlayerTask.UNKNOWN);
        assertTrue(has(rec.checks(), "Karuulm heat") || has(rec.candidateSummaries(), "Karuulm"), String.valueOf(rec.checks()));
    }

    // ---------- ammo ----------
    @Test void catalogRuneCrossbowUsesCompatibleBolts() {
        String boss = "The Leviathan", method = "Wiki · Ranged";
        var profile = TieredOutfitterCatalog.find(boss, method, Tier.ENTRY).profile();
        assertEquals(9185, profile.gear().get(WEAPON).id(), "Entry weapon remains Rune crossbow");
        assertEquals(RUBY_E, profile.gear().get(AMMO).id(), "Source fix must keep standard Ruby bolts (e)");
        var p = plan(boss, method, Tier.ENTRY, planned(boss, method, Tier.ENTRY), SlayerTask.UNKNOWN);
        assertFalse(has(p.blockers(), "cannot be used with Rune crossbow"), String.valueOf(p.blockers()));
        assertFalse(has(p.blockers(), "CORE Ammo"), String.valueOf(p.blockers()));
    }
    @Test void compatibleOwnedBoltsWinOverIncompatibleDragonBolts() {
        String boss = "The Leviathan", method = "Wiki · Ranged";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(RUBY_E, 500);
        own.put(21944, 500); // Ruby dragon bolts (e) are owned, but not compatible with Rune crossbow.
        var p = plan(boss, method, Tier.ENTRY, own,
            Map.of(RUBY_E, "Ruby bolts (e)", 21944, "Ruby dragon bolts (e)"),
            Map.of(RUBY_E, ammoStats(103), 21944, ammoStats(122)), MAX, SlayerTask.UNKNOWN);
        assertEquals(RUBY_E, p.gear().get(AMMO).shown().id(), String.valueOf(p.gear().get(AMMO)));
        assertFalse(has(p.blockers(), "CORE Ammo"), String.valueOf(p.blockers()));
    }
    @Test void twistedBowWithNoPlannedAmmoBlocksUntilArrowsAreOwned() {
        String boss = "Kree'arra", method = "Wiki · Ranged (tank)";
        var own = planned(boss, method, Tier.ENTRY);
        int plannedArrow = TieredOutfitterCatalog.find(boss, method, Tier.ENTRY).profile().gear().get(AMMO).id();
        own.remove(plannedArrow); // W07 source now includes an arrow; the absence case must remove it explicitly.
        var empty = plan(boss, method, Tier.ENTRY, own, SlayerTask.UNKNOWN);
        assertTrue(has(empty.blockers(), "needs ammo") || has(empty.blockers(), "CORE Ammo"), String.valueOf(empty.blockers()));
        own.put(DRAGON_ARROW, 500);
        var armed = plan(boss, method, Tier.ENTRY, own, Map.of(DRAGON_ARROW, "Dragon arrow"), Map.of(DRAGON_ARROW, ammoStats(60)), MAX, SlayerTask.UNKNOWN);
        assertEquals(DRAGON_ARROW, armed.gear().get(AMMO).shown().id());
        assertFalse(has(armed.blockers(), "needs ammo"), String.valueOf(armed.blockers()));
    }
    @Test void selfContainedWeaponsDoNotNeedAmmo() {
        String boss = "K'ril Tsutsaroth", method = "Wiki · Ranged";
        var p = plan(boss, method, Tier.ENTRY, planned(boss, method, Tier.ENTRY), SlayerTask.UNKNOWN);
        assertFalse(has(p.blockers(), "CORE Ammo"), String.valueOf(p.blockers()));
    }
    @Test void meleePlansAreNotAmmoChecked() {
        String boss = "Scurrius", method = "Standard melee";
        var p = plan(boss, method, Tier.ENTRY, planned(boss, method, Tier.ENTRY), SlayerTask.UNKNOWN);
        assertFalse(has(p.blockers(), "Ammo"), String.valueOf(p.blockers()));
        assertFalse(has(p.checks(), "Ammo:"), String.valueOf(p.checks()));
    }
    @Test void boltPicksObeyNamedCrossbowsAndBroadBoltLimits() {
        Map<Integer,Stats> st = Map.of(1, ammoStats(10), 2, ammoStats(100));
        Map<Integer,String> nm = Map.of(1, "Bronze bolts", 2, "Broad bolts");
        Map<Integer,Integer> have = Map.of(1, 100, 2, 100);
        assertTrue(OwnedGearStats.bestBolts("Karil's crossbow", 10, have, st, nm, Set.of()).isEmpty());
        assertTrue(OwnedGearStats.bestBolts("Hunters' crossbow", 10, have, st, nm, Set.of()).isEmpty());
        assertEquals(1, OwnedGearStats.bestBolts("Adamant crossbow", 10, have, st, nm, Set.of()).get().itemId(), "broad needs rune tier");
        assertEquals(2, OwnedGearStats.bestBolts("Rune crossbow", 10, have, st, nm, Set.of()).get().itemId());
    }
    @Test void compatibleAmmoIgnoresWrongFamilyEvenWithHigherStrength() {
        Map<Integer,Stats> st = Map.of(1, ammoStats(122), 2, ammoStats(60));
        var pick = OwnedGearStats.bestCompatibleAmmo("Twisted bow", 10, Map.of(1, 100, 2, 100), st, Map.of(1, "Dragon bolts", 2, "Dragon arrow"));
        assertEquals(2, pick.get().itemId());
    }

    // ---------- cape ----------
    @Test void ownedQuiverBeatsPlannedCapeAndStaysUnconfirmedForSaving() {
        String boss = "Kalphite Queen", method = "Wiki · Ranged";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(QUIVER, 1);
        var cell = plan(boss, method, Tier.ENTRY, own, SlayerTask.UNKNOWN).gear().get(CAPE);
        assertEquals(QUIVER, cell.shown().id());
        assertEquals(GearStatus.OWNED_SUBSTITUTE, cell.status());
        assertTrue(cell.note().contains("ammo saving unconfirmed"), cell.note());
    }
    @Test void assemblerBeatsAccumulator() {
        String boss = "Dagannoth Kings", method = "Prime, ranged";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(ASSEMBLER, 1);
        assertEquals(ASSEMBLER, plan(boss, method, Tier.ENTRY, own, SlayerTask.UNKNOWN).gear().get(CAPE).shown().id());
    }
    @Test void noRecoverableAmmoIsSaidOutLoud() {
        String boss = "K'ril Tsutsaroth", method = "Wiki · Ranged";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(ASSEMBLER, 1);
        var cell = plan(boss, method, Tier.ENTRY, own, SlayerTask.UNKNOWN).gear().get(CAPE);
        assertEquals(ASSEMBLER, cell.shown().id());
        assertTrue(cell.note().contains("no recoverable ammo"), cell.note());
    }
    @Test void unknownRangedLevelNeverSwapsTheCape() {
        String boss = "Dagannoth Kings", method = "Prime, ranged";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(QUIVER, 1);
        var cell = plan(boss, method, Tier.ENTRY, own, Map.of(QUIVER, "Dizana's quiver"), Map.of(), AccountContext.UNKNOWN, SlayerTask.UNKNOWN).gear().get(CAPE);
        assertEquals(ACCUMULATOR, cell.shown().id(), String.valueOf(cell));
    }
    @Test void lowRangedLevelSkipsTheQuiver() {
        String boss = "Dagannoth Kings", method = "Prime, ranged";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(QUIVER, 1);
        var low = new AccountContext(Map.of("Ranged", 60), MAX.questStates());
        var cell = plan(boss, method, Tier.ENTRY, own, Map.of(QUIVER, "Dizana's quiver"), Map.of(), low, SlayerTask.UNKNOWN).gear().get(CAPE);
        assertEquals(ACCUMULATOR, cell.shown().id(), String.valueOf(cell));
    }
    @Test void meleePlansKeepTheirCape() {
        String boss = "Scurrius", method = "Standard melee";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(QUIVER, 1);
        assertNotEquals(QUIVER, plan(boss, method, Tier.ENTRY, own, SlayerTask.UNKNOWN).gear().get(CAPE).shown().id());
    }

    // ---------- Slayer ----------
    static final String HYDRA = "Alchemical Hydra", HYDRA_M = "Wiki · Ranged";
    @Test void unknownTaskKeepsThePlanAndTheReminder() {
        var p = plan(HYDRA, HYDRA_M, Tier.ENTRY, planned(HYDRA, HYDRA_M, Tier.ENTRY), SlayerTask.UNKNOWN);
        assertTrue(has(p.checks(), "matching Slayer task"), String.valueOf(p.checks()));
    }
    @Test void confirmedTaskClearsTheReminder() {
        var p = plan(HYDRA, HYDRA_M, Tier.ENTRY, planned(HYDRA, HYDRA_M, Tier.ENTRY), SlayerTask.of(50, "HYDRAS"));
        assertFalse(has(p.checks(), "matching Slayer task"), String.valueOf(p.checks()));
    }
    @Test void noTaskSaysTheHelmGivesNoBonus() {
        var p = plan(HYDRA, HYDRA_M, Tier.ENTRY, planned(HYDRA, HYDRA_M, Tier.ENTRY), SlayerTask.NONE);
        assertTrue(has(p.checks(), "matching Slayer task"), String.valueOf(p.checks()));
        assertTrue(has(p.options(), "No active Slayer task"), String.valueOf(p.options()));
    }
    @Test void offTaskMeleeMovesToNeitiznot() {
        String boss = "Cerberus", method = "Wiki · Melee";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(NEITIZNOT, 1);
        var cell = plan(boss, method, Tier.ENTRY, own, SlayerTask.NONE).gear().get(HEAD);
        assertEquals(NEITIZNOT, cell.shown().id(), String.valueOf(cell));
        assertTrue(cell.note().contains("Not on task"), cell.note());
    }
    @Test void onTaskMeleeKeepsTheHelm() {
        String boss = "Cerberus", method = "Wiki · Melee";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(NEITIZNOT, 1);
        var cell = plan(boss, method, Tier.ENTRY, own, SlayerTask.of(50, "HELLHOUNDS")).gear().get(HEAD);
        assertNotEquals(NEITIZNOT, cell.shown().id());
    }
    @Test void onTaskAddsAnOwnedHelmToAPlanWithoutOne() {
        String boss = "Dagannoth Kings", method = "Prime, ranged";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(HELM_I, 1);
        var cell = plan(boss, method, Tier.ENTRY, own, SlayerTask.of(50, "DAGANNOTH")).gear().get(HEAD);
        assertEquals(HELM_I, cell.shown().id());
        assertEquals(GearStatus.OWNED_SUBSTITUTE, cell.status());
    }
    @Test void otherTaskNeverAddsTheHelm() {
        String boss = "Dagannoth Kings", method = "Prime, ranged";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(HELM_I, 1);
        assertNotEquals(HELM_I, plan(boss, method, Tier.ENTRY, own, SlayerTask.of(50, "HYDRAS")).gear().get(HEAD).shown().id());
        assertNotEquals(HELM_I, plan(boss, method, Tier.ENTRY, own, SlayerTask.UNKNOWN).gear().get(HEAD).shown().id());
    }
    @Test void konarAreaDecidesCoverage() {
        String boss = "Dagannoth Kings", method = "Prime, ranged";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(HELM_I, 1);
        var light = SlayerTask.of(50, "DAGANNOTH", SlayerTask.MASTER_KONAR, "the Lighthouse");
        var water = SlayerTask.of(50, "DAGANNOTH", SlayerTask.MASTER_KONAR, "Waterbirth Island");
        var noArea = SlayerTask.of(50, "DAGANNOTH", SlayerTask.MASTER_KONAR, null);
        assertNotEquals(HELM_I, plan(boss, method, Tier.ENTRY, own, light).gear().get(HEAD).shown().id());
        assertEquals(HELM_I, plan(boss, method, Tier.ENTRY, own, water).gear().get(HEAD).shown().id());
        assertNotEquals(HELM_I, plan(boss, method, Tier.ENTRY, own, noArea).gear().get(HEAD).shown().id());
    }
    @Test void plannedSalveIsNotOverriddenAtVorkathButOwnedAloneIsNotEnough() {
        String boss = "Vorkath", method = "Ranged crossbow";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(HELM_I, 1);
        assertNotEquals(HELM_I, plan(boss, method, Tier.ENTRY, own, SlayerTask.of(50, "BLUE DRAGONS")).gear().get(HEAD).shown().id(), "Salve is worn");
        own.remove(SALVE_EI);
        assertEquals(HELM_I, plan(boss, method, Tier.ENTRY, own, SlayerTask.of(50, "BLUE DRAGONS")).gear().get(HEAD).shown().id());
    }
    @Test void setHeadPieceIsNeverRemoved() {
        String boss = "K'ril Tsutsaroth", method = "Wiki · Ranged";
        var own = planned(boss, method, Tier.ENTRY);
        own.put(HELM_I, 1);
        assertNotEquals(HELM_I, plan(boss, method, Tier.ENTRY, own, SlayerTask.of(50, "K'ril Tsutsaroth")).gear().get(HEAD).shown().id());
    }
    @Test void wildernessStaysDeniedEvenOnTask() {
        var p = plan("Callisto", "Wiki · Ranged", Tier.ENTRY, Map.of(1, 1), SlayerTask.of(5, "Callisto"));
        assertEquals(Verdict.NOT_AVAILABLE_WILDERNESS, p.verdict());
    }
    @Test void preloaderReplansOnTaskChangeOnly() {
        try (var loader = new BestBankPreloader(List.of())) {
            loader.observe(BestBankPlannerJUnitTest.bank(Map.of(1, 1)), MAX, ItemFacts.catalogDerived());
            long g0 = loader.generation();
            long g1 = loader.updateSlayerTask(SlayerTask.of(50, "HYDRAS"));
            assertTrue(g1 > g0);
            assertEquals(g1, loader.updateSlayerTask(SlayerTask.of(49, "HYDRAS")), "a kill on the same task must not re-plan");
            assertTrue(loader.updateSlayerTask(SlayerTask.NONE) > g1);
            loader.updateSlayerTask(SlayerTask.of(50, "HYDRAS"));
            loader.clear();
            assertEquals(SlayerTask.State.UNKNOWN, loader.slayerTask().state, "another account must not inherit a task");
        }
    }
}
