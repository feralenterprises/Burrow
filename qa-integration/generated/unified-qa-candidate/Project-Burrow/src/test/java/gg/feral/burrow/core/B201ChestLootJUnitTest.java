package gg.feral.burrow.core;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** B20.1 R02: Moons of Peril via the Lunar Chest, Barrows via the Barrows chest (Wiki, read 2026-11-01). */
final class B201ChestLootJUnitTest {
    static RareDropTable.Drop d(String enc,String name){
        return RareDropTable.encounter(enc).orElseThrow().drops().stream().filter(x->x.name().equals(name)).findFirst().orElseThrow();
    }
    @Test void eachMoonRollsItsOwnSetFromTheLunarChest() {
        for(String moon:List.of("Eclipse Moon","Blood Moon","Blue Moon")) {
            var e=RareDropTable.encounter(moon).orElseThrow();
            assertEquals(4,e.drops().size(),moon);
            assertEquals(1/56.0,e.setTableChance(),1e-15);
            for(var x:e.drops()) { assertEquals("1/224",x.rateText()); assertEquals("lunar-chest",x.group()); }
            assertEquals(1/56.0,1-RareDropMath.none(e.drops(),1),1e-12,"any piece per kill = Wiki 1/56");
            assertTrue(e.wikiUrl().endsWith("Lunar_Chest"));
        }
        var helm=d("Blood Moon","Blood moon helm"); var mac=d("Blood Moon","Dual macuahuitl");
        assertEquals(0,RareDropMath.all(List.of(helm,mac),1),1e-15,"one piece per Moon kill");
        assertEquals(1-Math.pow(1-1/224.0,100),RareDropMath.any(List.of(helm),100),1e-12);
    }
    @Test void duplicateProtectionUsesOnlyObservedLog() {
        Map<String,String> cfg=new HashMap<>(); var store=new RareDropStore(cfg::get,cfg::put); long h=3L;
        store.toggle(h,"blood_moon:28997");
        var e=RareDropTable.encounter("Blood Moon").orElseThrow();
        var sum=RareDropSummary.of(e,store.read(h)).orElseThrow();
        assertTrue(RareDropSummary.duplicateProtectedPerKill(sum,store.read(h)).isEmpty(),"page not visited: no assumption");
        // Log page observed: helm + chestplate owned, macuahuitl + tassets missing.
        store.observe(h,CollectionLogParser.parse(List.of("Moons of Peril","Blood Moon kills: 40"),
            List.of(new CollectionLogParser.Slot(29028,0,1),new CollectionLogParser.Slot(29022,0,1),
                    new CollectionLogParser.Slot(28997,175,0),new CollectionLogParser.Slot(29025,175,0))).orElseThrow(),1);
        var st=store.read(h);
        sum=RareDropSummary.of(e,st).orElseThrow();
        assertEquals((1/56.0)*1/2,RareDropSummary.duplicateProtectedPerKill(sum,st).getAsDouble(),1e-15,"1 of 2 missing selected");
        assertEquals(40,st.effective("Blood Moon").value().getAsLong(),"per-Moon KC from the log page");
    }
    @Test void barrowsIsPerChestWithAllSixBrothers() {
        var e=RareDropTable.encounter("Barrows").orElseThrow();
        assertEquals(24,e.drops().size());
        assertEquals("Barrows chest",e.chestUnit());
        for(var x:e.drops()) { assertEquals("1/350.14",x.rateText(),"Wiki text as shown"); assertEquals("All six brothers killed",x.condition()); }
        assertTrue(e.rateNote().contains("approximation"),"combining pieces is labelled");
        assertEquals("Barrows",ChatKcParser.parse("Your Barrows chest count is: 102.").orElseThrow().encounter());
        assertEquals(102,ChatKcParser.parse("Your Barrows chest count is: 102.").orElseThrow().value());
        assertEquals(Map.of("Barrows",55L),CollectionLogParser.parse(List.of("Barrows Chests","Barrows Chests opened: 55"),List.of()).orElseThrow().killCounts());
        assertTrue(LootEncounterLedger.vettedEncounter("Barrows").isEmpty(),"chest is never a loot-confirmed kill");
        assertTrue(LootEncounterLedger.vettedEncounter("Lunar Chest").isEmpty());
    }
}
