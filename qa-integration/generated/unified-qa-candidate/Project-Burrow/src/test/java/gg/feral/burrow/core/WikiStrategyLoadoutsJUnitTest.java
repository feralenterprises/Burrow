package gg.feral.burrow.core;

import gg.feral.burrow.core.TieredOutfitterCatalog.Authority;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Build 20.1 R03: safety contract for the OSRS Wiki strategy DRAFT loadouts. */
class WikiStrategyLoadoutsJUnitTest {
    /** Restated from the OSRS Wiki item pages / Two-handed slot table (2026-11-01), not from production. */
    private static final Set<String> KNOWN_TWO_HANDED = Set.of(
        "twisted bow", "scythe of vitur", "bow of faerdhinen", "elder maul", "tumeken's shadow",
        "soulreaper axe", "magic shortbow (i)", "crystal bow", "webweaver bow", "craw's bow",
        "eclipse atlatl", "ascension crossbows", "abyssal bludgeon", "toxic blowpipe", "zorya's tome");
    /** Wiki says one-handed: these must never be treated as two-handed. */
    private static final Set<String> KNOWN_ONE_HANDED = Set.of(
        "keris partisan of breaching", "ursine chainmace", "viggora's chainmace", "zombie axe",
        "sanguinesti staff", "iban's staff", "hunters' sunlight crossbow", "tonalztics of ralos",
        "osmumten's fang", "trident of the swamp", "harmonised nightmare staff");

    @Test void twoHandedListMatchesTheWikiSlotTable() {
        for (String w : KNOWN_TWO_HANDED) assertTrue(WikiStrategyLoadouts.TWO_HANDED.contains(w), w);
        for (String w : KNOWN_ONE_HANDED) assertFalse(WikiStrategyLoadouts.TWO_HANDED.contains(w), w);
    }

    @Test void transcriptionLoadsCompletelyWithoutGuessing() {
        assertEquals(List.of(), WikiStrategyLoadouts.unresolved(), "every Wiki name maps to a verified item ID");
        assertEquals(List.of(), WikiStrategyLoadouts.overflow(), "no transcribed inventory exceeds 28 slots");
        assertTrue(WikiStrategyLoadouts.setups().size() >= 39, "setups: " + WikiStrategyLoadouts.setups().size());
        assertTrue(WikiStrategyLoadouts.bosses().size() >= 38, "bosses: " + WikiStrategyLoadouts.bosses().size());
        assertFalse(WikiStrategyLoadouts.bosses().contains("Gemstone Crab"));
    }

    @Test void everyDraftIsSlotBoundedLabelledAndSourced() {
        int n = 0;
        for (String boss : WikiStrategyLoadouts.bosses()) {
            assertTrue(BossRegistry.find(boss).isPresent(), "draft boss is a registry boss: " + boss);
            for (String m : WikiStrategyLoadouts.methods(boss)) {
                assertTrue(m.startsWith("Wiki · "), m);
                for (Tier t : Tier.values()) {
                    var s = WikiStrategyLoadouts.find(boss, m, t);
                    assertNotNull(s, boss + "/" + m + "/" + t);
                    var p = s.profile();
                    String id = boss + "/" + m + "/" + t;
                    assertEquals(11, p.gear().size(), id);
                    assertEquals(28, p.inventory().size(), id);
                    assertNotNull(p.gear().get(4), "weapon present: " + id);
                    assertEquals(Authority.TPT_CURATED_WIKI_STRATEGY, s.authority(), id);
                    assertTrue(s.provenance().startsWith("https://oldschool.runescape.wiki/w/"), id);
                    assertTrue(s.provenance().contains("(read 2026-11-01)"), id);
                    assertTrue(p.notes().get(0).contains("DRAFT"), "DRAFT label first: " + id);
                    assertTrue(p.notes().get(0).contains("not a bank or readiness check"), id);
                    for (Item i : p.gear()) if (i != null) assertTrue(i.id() > 0 && i.amount() > 0, id + " " + i);
                    for (Item i : p.inventory()) if (i != null) assertTrue(i.id() > 0 && (i.amount() > 0 || (i.amount() == -1
                        && WikiStrategyLoadouts.countsUnstated(p.notes()))), id + " " + i);
                    String w = p.gear().get(4).name().toLowerCase(Locale.ROOT);
                    if (KNOWN_TWO_HANDED.contains(w)) assertNull(p.gear().get(6), "2H weapon has no shield: " + id);
                    if (BossRegistry.wilderness(boss)) {
                        assertTrue(p.notes().stream().anyMatch(x -> x.startsWith("WILDERNESS")), "warning: " + id);
                        assertFalse(BossRegistry.bestBankAllowed(boss), "Best in Bank stays off: " + id);
                    }
                    n++;
                }
            }
        }
        assertEquals(WikiStrategyLoadouts.size(), n);
    }

    @Test void tierColumnsAreDistinctWhereTheWikiHasDistinctColumns() {
        // Kalphite Queen melee: Optimal and Entry must not silently collapse into the same weapon.
        var o = WikiStrategyLoadouts.find("Kalphite Queen", "Wiki · Melee", Tier.OPTIMAL).profile();
        var e = WikiStrategyLoadouts.find("Kalphite Queen", "Wiki · Melee", Tier.ENTRY).profile();
        assertNotEquals(o.gear().get(4).id(), e.gear().get(4).id());
    }

    @Test void genericWikiCellsStayEmptyWithAReason() {
        var p = WikiStrategyLoadouts.find("General Graardor", "Wiki · Melee (solo)", Tier.ENTRY).profile();
        assertNull(p.gear().get(5), "Barrows tank top is not one item");
        assertTrue(p.notes().stream().anyMatch(n -> n.contains("Barrows tank top") && n.contains("left empty")));
    }

    @Test void venenatisSpecSlotIsSwappedNotOverflowed() {
        for (Tier t : Tier.values()) {
            var p = WikiStrategyLoadouts.find("Venenatis", "Wiki · Melee", t).profile();
            assertEquals(28, p.inventory().stream().filter(Objects::nonNull).count(), t.name());
        }
    }

    @Test void originalCatalogStillWinsAndDraftsOnlyFillGaps() {
        // A protected original profile is still served from its own catalog, not a Wiki draft.
        for (String m : TieredOutfitterCatalog.methods("Scurrius"))
            assertFalse(m.startsWith("Wiki · "), "Scurrius keeps its original methods: " + m);
        for (String b : BossRegistry.selectableIds())
            if (BossRegistry.evidence(b) == BossRegistry.Evidence.GUIDE_NOT_YET_VERIFIED)
                assertTrue(TieredOutfitterCatalog.methods(b).isEmpty(), "no invented gear: " + b);
    }

    @Test void gemstoneCrabIsNotSelectable() {
        assertTrue(BossRegistry.excluded("Gemstone Crab"));
        assertFalse(BossRegistry.selectableIds().contains("Gemstone Crab"));
        assertFalse(BurrowModel.availableBosses().contains("Gemstone Crab"));
    }

    private static VerifiedOutfitterCatalog.Profile p(String boss, String method, Tier t) {
        var s = WikiStrategyLoadouts.find(boss, method, t);
        assertNotNull(s, boss + "/" + method + "/" + t);
        return s.profile();
    }
    private static boolean carries(VerifiedOutfitterCatalog.Profile p, String name) {
        return p.inventory().stream().anyMatch(i -> i != null && i.name().equalsIgnoreCase(name));
    }

    @Test void r04AmmoAlwaysFitsTheTiersWeapon() {
        for (String boss : WikiStrategyLoadouts.bosses()) for (String m : WikiStrategyLoadouts.methods(boss)) for (Tier t : Tier.values()) {
            var pr = p(boss, m, t);
            var need = WikiStrategyLoadouts.needs(pr.gear().get(4).name());
            if (need == WikiStrategyLoadouts.AmmoKind.OTHER) continue;
            var ammo = pr.gear().get(3);
            String id = boss + "/" + m + "/" + t + " " + pr.gear().get(4).name() + " + " + (ammo == null ? "none" : ammo.name());
            if (need == WikiStrategyLoadouts.AmmoKind.NONE_NEEDED) {
                assertTrue(ammo == null || WikiStrategyLoadouts.kind(ammo.name()) == WikiStrategyLoadouts.AmmoKind.NONE_NEEDED, id);
            } else {
                if (ammo != null) assertEquals(need, WikiStrategyLoadouts.kind(ammo.name()), id);
                else assertTrue(pr.notes().stream().anyMatch(n -> n.startsWith("Ammo:")), "missing ammo is explained: " + id);
            }
        }
        var zil = p("Commander Zilyana", "Wiki · Ranged", Tier.RECOMMENDED);
        assertEquals("Zaryte crossbow", zil.gear().get(4).name());
        assertEquals(WikiStrategyLoadouts.AmmoKind.BOLT, WikiStrategyLoadouts.kind(zil.gear().get(3).name()), "crossbow gets bolts, not N/A");
        var liz = p("Lizardman Shamans", "Wiki · Ranged (Lizardman Settlement)", Tier.RECOMMENDED);
        assertEquals("Twisted bow", liz.gear().get(4).name());
        assertEquals("Rune arrow", liz.gear().get(3).name(), "bolt rack is not shot from a twisted bow");
    }

    @Test void r04NaMeansNoLesserOptionNotNakedSlot() {
        var e = p("Lizardman Shamans", "Wiki · Ranged (Lizardman Temple)", Tier.ENTRY);
        for (int slot : new int[]{0, 5, 7, 8, 9}) assertTrue(e.gear().get(slot).name().startsWith("Shayzien"), "Shayzien (5) kept at Entry");
        assertTrue(e.notes().stream().anyMatch(n -> n.contains("no lower option")));
    }

    @Test void r04ZilyanaKeepsMinionBlowpipeAndExactGrid() {
        for (Tier t : Tier.values()) {
            var z = p("Commander Zilyana", "Wiki · Ranged", t);
            assertTrue(carries(z, "Toxic blowpipe"), "blowpipe kept for minions at " + t);
            assertEquals(28, z.inventory().stream().filter(Objects::nonNull).count(), t.name());
        }
        assertTrue(p("Commander Zilyana", "Wiki · Ranged", Tier.RECOMMENDED).notes().stream().anyMatch(n -> n.contains("not carried")),
            "a lower-tier spec weapon that does not fit is explained, not squeezed in");
    }

    @Test void r04NightmareSwitchesFollowTheTierAndColumnsAreFaithful() {
        var o = p("The Nightmare", "Wiki · Melee (crush, group)", Tier.OPTIMAL);
        var r = p("The Nightmare", "Wiki · Melee (crush, group)", Tier.RECOMMENDED);
        var e = p("The Nightmare", "Wiki · Melee (crush, group)", Tier.ENTRY);
        assertEquals("Soulreaper axe", r.gear().get(4).name(), "5-column Wiki weapon row, middle column");
        assertTrue(carries(o, "Ancestral hat") && carries(o, "Tumeken's shadow"));
        assertFalse(carries(o, "Tome of fire"), "no shield switch with a two-handed switch staff");
        assertTrue(carries(r, "Ahrim's hood") || carries(r, "Virtus mask"));
        assertTrue(carries(e, "Ahrim's hood"), "Entry mage switch is the Wiki's last listed piece");
        assertFalse(carries(o, "Avernic treads (max)"), "worn items are not duplicated as switches");
    }

    @Test void r04ProtectedOriginalBossesOnlyGetHeldDrafts() {
        for (String b : VerifiedOutfitterCatalog.BOSSES) assertTrue(WikiStrategyLoadouts.methods(b).isEmpty(), "no Wiki method in the picker for " + b);
        assertTrue(WikiStrategyLoadouts.heldBosses().contains("Brutus"));
        assertEquals(9, WikiStrategyLoadouts.heldSize(), "Brutus F2P melee/ranged/magic x 3 tiers held for owner review");
        var melee = WikiStrategyLoadouts.findHeld("Brutus", "Wiki · Melee (free-to-play)", Tier.OPTIMAL).profile();
        assertNull(melee.gear().get(9), "unconfirmed decorative boots ID is left empty");
        assertTrue(melee.notes().stream().anyMatch(n -> n.contains("not confirmed")));
        var ranged = WikiStrategyLoadouts.findHeld("Brutus", "Wiki · Ranged (free-to-play)", Tier.ENTRY).profile();
        assertEquals("Oak shortbow", ranged.gear().get(4).name());
        assertEquals("Steel arrow", ranged.gear().get(3).name());
    }

    @Test void r05SwitchTablesShowAsSecondaryStyles() {
        var n = MethodStyles.of("The Nightmare", "Wiki · Melee (crush, group)");
        assertEquals(CombatStyle.MELEE, n.primary());
        assertTrue(n.uses(CombatStyle.MAGIC), "totem mage switch is shown as a switch style");
        var t = MethodStyles.of("Royal Titans", "Wiki · Melee + ranged switch (high level)");
        assertTrue(t.uses(CombatStyle.RANGED));
        var z = MethodStyles.of("Commander Zilyana", "Wiki · Ranged");
        assertEquals(List.of(CombatStyle.RANGED), z.required(), "no switch table, no invented secondary style");
    }
}
