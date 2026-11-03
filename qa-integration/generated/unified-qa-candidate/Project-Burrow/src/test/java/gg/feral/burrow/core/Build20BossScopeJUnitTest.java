package gg.feral.burrow.core;

import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Build 20: Outfitter scope vs the owner's machine-readable manifest, exclusions and Wilderness rule. */
final class Build20BossScopeJUnitTest {
    static String resource(String name) throws Exception {
        try (InputStream in = Build20BossScopeJUnitTest.class.getResourceAsStream("/build20/" + name)) {
            assertNotNull(in, name); return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    @SuppressWarnings("unchecked")
    static List<Map<String,Object>> manifest() throws Exception {
        return (List<Map<String,Object>>) ((Map<String,Object>) MiniJson.parse(resource("BOSS-SCOPE-MANIFEST.json"))).get("bosses");
    }
    static BossRegistry.Group group(String manifestGroup) {
        return switch (manifestGroup) {
            case "Regular bosses" -> BossRegistry.Group.REGULAR;
            case "Wilderness bosses", "Wilderness-access bosses" -> BossRegistry.Group.WILDERNESS;
            case "Moons of Peril" -> BossRegistry.Group.MOONS;
            case "Demi-bosses" -> BossRegistry.Group.DEMI;
            default -> throw new AssertionError(manifestGroup);
        };
    }

    /** Owner removals after the Build 20 manifest (2026-11-01: Gemstone Crab needs no gear). */
    static final Set<String> OWNER_REMOVED = Set.of("Gemstone Crab");
    @Test void everyManifestBossIsSelectableWithMatchingGroupRiskAndBestBankFlag() throws Exception {
        var rows = manifest();
        assertEquals(61, rows.size());
        Set<String> seen = new HashSet<>();
        for (var r : rows) {
            String name = (String) r.get("name");
            if (OWNER_REMOVED.contains(name)) {
                // Owner decision after the manifest: removed from Outfitter, never selectable.
                assertTrue(BossRegistry.excluded(name) && BossRegistry.find(name).isEmpty(), name);
                assertFalse(BurrowModel.availableBosses().contains(name), name);
                continue;
            }
            var e = BossRegistry.find(name).orElseThrow(() -> new AssertionError("missing " + name));
            assertTrue(seen.add(e.id()), "no duplicate mapping " + name);
            assertTrue(BurrowModel.availableBosses().contains(e.id()), "selectable " + name);
            assertEquals(group((String) r.get("group")), e.group(), name);
            assertEquals(r.get("risk"), e.risk().name(), name);
            assertEquals(r.get("best_in_bank_enabled"), e.bestBankAllowed(), name);
            assertTrue(e.wikiUrl().startsWith("https://oldschool.runescape.wiki/w/"), name);
        }
        assertEquals(61 - OWNER_REMOVED.size(), BurrowModel.availableBosses().size(), "no extra unreviewed bosses");
        assertEquals(3, BossRegistry.group(BossRegistry.Group.MOONS).size());
        assertEquals(List.of("Tormented Demons", "Demonic Gorillas", "Lizardman Shamans"),
            BossRegistry.group(BossRegistry.Group.DEMI).stream().map(BossRegistry.Entry::id).toList());
        assertEquals(11, BossRegistry.group(BossRegistry.Group.WILDERNESS).size(), "10 Wilderness + KBD");
    }

    @Test void aliasesNormaliseWithoutDuplicates() {
        assertEquals("Hueycoatl", BossRegistry.find("The Hueycoatl").orElseThrow().id());
        assertEquals("Barrows", BossRegistry.find("Dharok the Wretched").orElseThrow().id());
        assertEquals("Dagannoth Kings", BossRegistry.find("dagannoth rex").orElseThrow().id());
        assertEquals("Royal Titans", BossRegistry.find("Branda the Fire Queen").orElseThrow().id());
        assertEquals("Lizardman Shamans", BossRegistry.find("Lizardman shaman").orElseThrow().id());
        assertNotEquals(BossRegistry.find("The Nightmare").orElseThrow(), BossRegistry.find("Phosani's Nightmare").orElseThrow(),
            "Phosani's is a separate encounter");
    }

    @Test void everyExcludedBossIsAbsentFromThePicker() throws Exception {
        List<String> csv = new ArrayList<>();
        for (String line : resource("HARD-EXCLUSION-MATRIX.csv").split("\\R")) {
            if (line.isBlank() || line.startsWith("excluded_from_outfitter")) continue;
            csv.add(line.split(",")[0].trim());
        }
        assertTrue(csv.size() >= 45);
        for (String name : csv) {
            assertTrue(BossRegistry.excluded(name), "registered as excluded: " + name);
            assertTrue(BossRegistry.find(name).isEmpty(), "never resolvable: " + name);
            assertFalse(BurrowModel.availableBosses().contains(name), "never in picker: " + name);
            assertThrows(IllegalArgumentException.class, () -> new BurrowModel().chooseBoss(name), name);
        }
    }

    @Test void wildernessNeverGetsBestInBankAtAnyLayer() throws Exception {
        var max = new AccountContext(Map.of("Ranged",99,"Defence",99,"Attack",99,"Strength",99), Map.of());
        for (var e : BossRegistry.group(BossRegistry.Group.WILDERNESS)) {
            assertFalse(BossRegistry.bestBankAllowed(e.id()), e.id());
            for (String method : TieredOutfitterCatalog.methods(e.id())) for (Tier t : Tier.values()) {
                var sel = TieredOutfitterCatalog.find(e.id(), method, t);
                if (sel == null) continue;
                Map<Integer,Integer> own = new HashMap<>();
                for (Item i : sel.profile().gear()) if (i != null && i.id() > 0) own.put(i.id(), Math.max(1, i.amount()));
                var plan = BestBankPlanner.plan(e.id(), method, t, BankEvidence.fixture(own), max, ItemFacts.catalogDerived());
                assertEquals(BestBankPlanner.Verdict.NOT_AVAILABLE_WILDERNESS, plan.verdict(), e.id());
                assertFalse(plan.playable());
                assertTrue(plan.gear().stream().noneMatch(c -> c.status() == BestBankPlanner.GearStatus.OWNED));
            }
        }
        assertTrue(BestBankPreloader.knownProfiles().stream().noneMatch(k -> BossRegistry.wilderness(k.boss())));
        // A crafted request for KBD must never answer, even with an active snapshot.
        try (var pre = new BestBankPreloader(List.of(new BestBankPreloader.Key("Scurrius","Standard melee",Tier.RECOMMENDED)))) {
            pre.observe(Map.of(9185, 1, 1540, 1));
            var kbd = new BestBankPreloader.Key("King Black Dragon","Crossbow, standard route",Tier.RECOMMENDED);
            CountDownLatch never = new CountDownLatch(1);
            pre.request(kbd, p -> never.countDown());
            assertFalse(never.await(500, TimeUnit.MILLISECONDS), "no Wilderness plan is computed");
            assertTrue(pre.cached(kbd).isEmpty());
            assertFalse(pre.bestHeadlineByBoss().containsKey("King Black Dragon"));
        }
        // Default deny: an unregistered name is never bank-optimized.
        assertFalse(BossRegistry.bestBankAllowed("Some Future Boss"));
        // No overbroad disable: ordinary bosses keep Best in Bank.
        assertTrue(BossRegistry.bestBankAllowed("Vorkath"));
        assertTrue(BossRegistry.bestBankAllowed("Lizardman Shamans"));
        assertTrue(BossRegistry.bestBankAllowed("Corporeal Beast"), "lair is not Wilderness per Wiki");
    }

    @Test void newBossesWithoutVerifiedLoadoutsAreDiscoveryOnly() {
        var unverified = new java.util.TreeSet<String>();
        for (var e : BossRegistry.all()) {
            var ev = BossRegistry.evidence(e.id());
            if (ev == BossRegistry.Evidence.GUIDE_NOT_YET_VERIFIED) {
                unverified.add(e.id());
                assertTrue(TieredOutfitterCatalog.methods(e.id()).isEmpty(), "no invented methods for " + e.id());
                var m = new BurrowModel(); m.chooseBoss(e.id());
                assertNull(m.profile(), "no gear for " + e.id());
            }
            if (!e.existing()) assertFalse(e.wikiUrl().isBlank());
        }
        // R03/R04: bosses with OSRS Wiki strategy DRAFT profiles. Maggot King stays discovery-only
        // (no gear invented) until its melee/ranged tables and inventory are supplied.
        assertEquals(new java.util.TreeSet<>(Set.of("Maggot King")),
            unverified, "exact discovery-only set (no invented gear)");
        for (String b : WikiStrategyLoadouts.bosses())
            assertEquals(BossRegistry.Evidence.DRAFT_PROFILES, BossRegistry.evidence(b), "Wiki draft evidence for " + b);
        assertEquals(BossRegistry.Evidence.DRAFT_PROFILES, BossRegistry.evidence("Lizardman Shamans"));
        assertEquals(BossRegistry.Evidence.CATALOG_PROFILES, BossRegistry.evidence("Scurrius"));
    }

    @Test void moonsAreThreeDistinctFightsWithDistinctWeaknessesAndNoSharedInventedLoadout() {
        var eclipse = BossRegistry.find("Eclipse Moon").orElseThrow();
        var blood = BossRegistry.find("Blood Moon").orElseThrow();
        var blue = BossRegistry.find("Blue Moon").orElseThrow();
        assertTrue(eclipse.mechanic().startsWith("Stab weakness"));
        assertTrue(blood.mechanic().startsWith("Slash weakness"));
        assertTrue(blue.mechanic().startsWith("Crush weakness"));
        var weaponRows = new HashSet<String>();
        for (var m : List.of(eclipse, blood, blue)) {
            assertEquals(List.of("Perilous Moons"), m.quests());
            // R04: each Moon now has its own Wiki weapon table (owner-supplied screenshots), never a shared invented one.
            var methods = TieredOutfitterCatalog.methods(m.id());
            assertEquals(1, methods.size(), m.id());
            assertTrue(methods.get(0).startsWith("Wiki · Melee ("), m.id());
            var p = TieredOutfitterCatalog.find(m.id(), methods.get(0), Tier.RECOMMENDED).profile();
            assertTrue(p.notes().stream().anyMatch(n -> n.contains("weak to")), "weakness stated for " + m.id());
            for (var setup : WikiStrategyLoadouts.setups()) if (setup.boss().equals(m.id())) weaponRows.add(setup.slots().get("Weapon"));
        }
        // The Wiki can rank the same weapon for two Moons (e.g. Blade of Saeldor on stab vs slash); the weapon tables differ.
        assertEquals(3, weaponRows.size(), "three Moons, three different Wiki weapon tables");
        assertTrue(eclipse.mechanic().contains("Protection prayers do nothing"), "no generic prayer-heavy advice");
    }

    @Test void lizardmanShamanDraftsAreCoherentAndSafetyNoted() {
        var model = new BurrowModel();
        model.chooseBoss("Lizardman Shamans");
        assertEquals(Tier.ENTRY, model.tier(), "opens on the tier that exists");
        // R04: the two original Entry drafts stay first and unchanged; Wiki location setups are added after them.
        assertEquals(List.of(Build20EncounterLoadouts.LIZARD_RANGED, Build20EncounterLoadouts.LIZARD_MELEE), model.methods().subList(0, 2));
        assertTrue(model.methods().subList(2, model.methods().size()).stream().allMatch(m -> m.startsWith("Wiki · ")));
        assertEquals(CombatStyle.RANGED, model.selectedStyles().primary(), "ranged preferred");
        for (String method : model.methods().subList(0, 2)) {
            var p = TieredOutfitterCatalog.find("Lizardman Shamans", method, Tier.ENTRY).profile();
            for (int slot : new int[]{0, 5, 7, 8, 9}) assertTrue(p.gear().get(slot).name().startsWith("Shayzien"), "full Shayzien (5)");
            assertEquals(28, p.inventory().stream().filter(Objects::nonNull).count(), "full 28-slot plan");
            assertTrue(p.inventory().stream().anyMatch(i -> i != null && i.id() == 5952), "antipoison carried");
            String notes = String.join(" ", p.notes());
            assertTrue(notes.contains("DRAFT"));
            assertTrue(notes.contains("Captain Cleive") && notes.contains("hard Kourend & Kebos diary"));
            assertTrue(notes.contains("lizardman Slayer task"));
        }
        // Owning the Shayzien helm without full set is not "ready".
        var plan = BestBankPlanner.plan("Lizardman Shamans", Build20EncounterLoadouts.LIZARD_RANGED, Tier.ENTRY,
            BankEvidence.fixture(Map.of(13379, 1, 9185, 1)), AccountContext.UNKNOWN, ItemFacts.catalogDerived());
        assertEquals(BestBankPlanner.Verdict.NOT_READY, plan.verdict());
        assertTrue(plan.blockers().stream().anyMatch(b -> b.contains("Shayzien body (5)")));
    }

    @Test void build19CatalogPreserved() {
        assertEquals(51, TieredOutfitterCatalog.size());
        assertEquals(69, TieredOutfitterCatalog.totalSize(), "51 original + 2 Brutus + 10 research + 6 demon; Build20 separate");
        assertEquals(10, ResearchCombatLoadouts.size(), "Wave 7.5 added three gear-backed alternatives");
        assertEquals(17, BurrowModel.build19Bosses().size());
        for (String b : BurrowModel.build19Bosses()) assertTrue(BurrowModel.availableBosses().contains(b), b);
    }
}
