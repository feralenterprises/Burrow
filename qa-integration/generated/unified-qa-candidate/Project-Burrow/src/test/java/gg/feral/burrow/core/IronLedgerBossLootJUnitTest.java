package gg.feral.burrow.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Boss-only event filter regression: never promote ordinary NPC loot into Sessions. */
public final class IronLedgerBossLootJUnitTest {
    private static IronLedgerLootIntake.ItemObserved item(int id,int qty,String name) {
        return new IronLedgerLootIntake.ItemObserved(id,qty,name);
    }
    @Test public void onlySupportedBossesPass() {
        List<IronLedgerLootIntake.ItemObserved> items=List.of(item(385,2,"Shark"));
        for(String npc:List.of("Vorkath","SCURRIUS","Tormented demon","Demonic gorilla",
                "Dagannoth Rex","Dagannoth Prime","Dagannoth Supreme","The Hueycoatl","Dusk")) {
            var got=IronLedgerLootIntake.accept(npc,items);
            assertEquals(1,got.size());
            assertEquals(385,got.get(0).itemId());
            assertEquals(2,got.get(0).amount());
        }
        for(String npc:List.of("Goblin","Dagannoth","Gorilla","Abyssal demon",
                 "Barrows chest","Vorkath minion","Tormented spirit","Calvar'ion"))
            assertTrue(IronLedgerLootIntake.accept(npc,items).isEmpty());
    }
    @Test public void bossNamesAreCanonicalAndNotKcProof() {
        var drops=IronLedgerLootIntake.accept("  tormented DEMON  ",
            List.of(item(26219,1,"Scorching bow"),item(1,2,"Item")));
        assertEquals(2,drops.size());
        assertEquals("Tormented Demons",drops.get(0).boss());
        assertEquals("Demonic Gorillas",IronLedgerLootIntake.recognizeEncounter("demonic gorilla").orElseThrow());
        assertFalse(IronLedgerLootIntake.recognizeEncounter("dagannoth").isPresent());
    }
    @Test public void missingAndNonpositiveItemsDoNotBecomeDrops() {
        var got=IronLedgerLootIntake.accept("Vorkath",List.of(
            item(-1,2,"invalid"),item(385,0,"Shark"),item(385,-3,"Shark"),item(565,4,"")));
        assertEquals(1,got.size());
        assertEquals(565,got.get(0).itemId());
        assertEquals("Item #565",got.get(0).itemName());
        assertTrue(IronLedgerLootIntake.accept("Vorkath",null).isEmpty());
        assertTrue(IronLedgerLootIntake.accept(null,List.of(item(565,4,"Rune"))).isEmpty());
    }
    @Test public void sameBossDropIsNotInferredFromOrdinaryNpc() {
        assertEquals(0,IronLedgerLootIntake.accept("Skeleton",List.of(item(11840,1,"Dragon boots"))).size());
        assertEquals("King Black Dragon",IronLedgerLootIntake.accept("King Black Dragon",List.of(item(11840,1,"Dragon boots"))).get(0).boss());
    }
}
