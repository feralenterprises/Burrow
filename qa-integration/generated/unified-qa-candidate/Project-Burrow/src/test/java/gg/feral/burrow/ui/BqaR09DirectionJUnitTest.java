package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import gg.feral.burrow.pathfinder.SuggestionEngine;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** R09: Home direction (objective, drop goals, upgrades) is evidence-gated. Synthetic inputs over the real catalog. */
final class BqaR09DirectionJUnitTest {
    static final AccountContext MAX;
    static { Map<String,Integer> m = new HashMap<>();
        for (String s : List.of("Attack","Strength","Defence","Ranged","Prayer","Magic","Runecraft","Construction","Hitpoints","Agility",
            "Herblore","Thieving","Crafting","Fletching","Slayer","Hunter","Mining","Smithing","Fishing","Cooking","Firemaking",
            "Woodcutting","Farming","Sailing")) m.put(s, 99);
        MAX = new AccountContext(m, Map.of("Dragon Slayer I", "FINISHED", "Dragon Slayer II", "FINISHED")); }
    static BankEvidence ev(Map<Integer,Integer> m) {
        return new BankEvidence(m, Map.of(), Map.of(), BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT, 1L, true, false);
    }

    @Test void signedOutObjectiveAndGoalsSaySoNeverInvent() {
        var o = HomeDirection.objective(null, List.of());
        assertEquals("Sign in for account-based direction", o.get(0).text());
        assertEquals("Sign in to see your drop goals", HomeDirection.dropGoals(null).get(0).text());
    }

    @Test void upgradesAreUnknownWithoutCurrentBankRead() {
        assertTrue(HomeDirection.upgrades(BestBankPreloader.UpgradeEvidence.NONE, List.of()).get(0).text().startsWith("UNKNOWN"));
        assertTrue(HomeDirection.upgrades(BestBankPreloader.UpgradeEvidence.STALE, List.of()).get(0).text().contains("stale"));
        try (var pre = new BestBankPreloader(List.of())) {
            assertEquals(BestBankPreloader.UpgradeEvidence.NONE, pre.upgradeEvidence());
            assertTrue(pre.upgradeOpportunities(5).isEmpty());
            pre.observe(ev(Map.of(4151, 1)), MAX, null);
            assertEquals(BestBankPreloader.UpgradeEvidence.CURRENT, pre.upgradeEvidence());
            pre.markBankClosed(); pre.markStale();
            assertEquals(BestBankPreloader.UpgradeEvidence.STALE, pre.upgradeEvidence());
            assertTrue(pre.upgradeOpportunities(5).isEmpty(), "stale evidence never lists upgrades");
        }
    }

    @Test void upgradesListMissingAndLowerTierTargetsFromPreparedPlans() throws Exception {
        var key = new BestBankPreloader.Key("Scurrius", "Standard melee", Tier.OPTIMAL);
        var opt = TieredOutfitterCatalog.find("Scurrius", "Standard melee", Tier.OPTIMAL).profile();
        Map<Integer,Integer> owned = new HashMap<>();
        for (int i = 0; i < 11; i++) { Item it = opt.gear().get(i); if (it != null && it.id() > 0 && i != 4) owned.merge(it.id(), Math.max(1, it.amount()), Integer::sum); }
        try (var pre = new BestBankPreloader(List.of(key))) {
            pre.observe(ev(owned), MAX, null);
            var got = new CompletableFuture<BestBankPreloader.Prepared>();
            pre.request(key, got::complete);
            got.get(5, TimeUnit.SECONDS);
            var ups = pre.upgradeOpportunities(6);
            var weapon = ups.stream().filter(u -> u.itemId() == opt.gear().get(4).id()).findFirst().orElseThrow(() -> new AssertionError(String.valueOf(ups)));
            assertEquals(BestBankPreloader.Upgrade.Kind.MISSING, weapon.kind());
            assertEquals(List.of("Scurrius"), weapon.bosses());
            assertTrue(ups.stream().noneMatch(u -> owned.containsKey(u.itemId())), "owned target items are never upgrades");
            var lines = HomeDirection.upgrades(pre.upgradeEvidence(), ups);
            int at = -1; for (int i = 0; i < lines.size(); i++) if (lines.get(i).text().equals(opt.gear().get(4).name())) at = i;
            assertTrue(at >= 0, String.valueOf(lines));
            assertEquals("Weapon · not in bank read · Scurrius", lines.get(at + 1).text());
        }
    }

    @Test void dropGoalsShowChanceByVerifiedKcOrUnknownAndObtainedOnlyFromLog() {
        var mem = new HashMap<String,String>();
        var store = new RareDropStore(mem::get, mem::put);
        var shard = RareDropTable.encounter("Phantom Muspah").orElseThrow().drops().stream().filter(d -> d.name().equals("Venator shard")).findFirst().orElseThrow();
        var st = store.toggle(42L, shard.key());
        var unknown = HomeDirection.dropGoals(st);
        assertEquals("Venator shard · Phantom Muspah", unknown.get(0).text());
        assertEquals("KC UNKNOWN", unknown.get(1).text());
        st = store.recordKc(42L, "Phantom Muspah", RareDropStore.Source.HISCORES, 60, 1L);
        assertEquals("Chance by your KC: 45.3%", HomeDirection.dropGoals(st).get(1).text());
        assertEquals(27614, HomeDirection.dropGoals(st).get(0).itemId(), "sprite id for the goal item");
    }

    @Test void objectiveUsesSavedGoalAndObservedReadinessOnly() throws Exception {
        Map<String,Integer> skills = new HashMap<>();
        for (var s : List.of("Attack","Strength","Defence","Ranged","Prayer","Magic","Hitpoints","Slayer")) skills.put(s, 70);
        var snap = new SuggestionEngine.Snapshot(true, skills, Map.of(), SuggestionEngine.BankStatus.UNAVAILABLE, Set.of());
        var any = SuggestionEngine.evaluateAll(snap);
        assertFalse(any.isEmpty());
        var chosen = any.get(any.size() - 1);
        var withGoal = snap.withGoals(Map.of(chosen.id(), SuggestionEngine.GoalMark.SAVED));
        var lines = HomeDirection.objective(withGoal, List.of());
        String title = SuggestionEngine.evaluateAll(withGoal).stream().filter(x -> x.goal() == SuggestionEngine.GoalMark.SAVED).findFirst().orElseThrow().title();
        assertEquals(title, lines.get(0).text());
        assertTrue(lines.stream().anyMatch(l -> l.text().equals("Your saved goal")));
        assertTrue(lines.stream().noneMatch(l -> l.text().toLowerCase(Locale.ROOT).contains("complete")), "never claims completion");
        assertTrue(lines.get(lines.size() - 1).text().startsWith("1 saved goal · 0 marked done by you"));
        SwingUtilities.invokeAndWait(() -> {
            var p = new BurrowPanel(new BurrowModel());
            p.setPathfinderSnapshot(withGoal);
            assertEquals(title, p.homeObjectiveLines().get(0).text());
            int before = p.homeBottom();
            p.setUpgrades(BestBankPreloader.UpgradeEvidence.CURRENT, List.of(
                new BestBankPreloader.Upgrade(26219, "Osmumten's fang", "Weapon", BestBankPreloader.Upgrade.Kind.MISSING, List.of("Vorkath"))));
            assertEquals("Osmumten's fang", p.homeUpgradeLines().get(0).text());
            assertEquals(26219, p.homeUpgradeLines().get(0).itemId());
            assertEquals("Weapon · not in bank read · Vorkath", p.homeUpgradeLines().get(1).text());
            assertTrue(p.homeBottom() >= before);
            p.setSize(225, p.getPreferredSize().height);
            var img = new java.awt.image.BufferedImage(225, p.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g = img.createGraphics(); p.paint(g); g.dispose();
        });
    }
}
