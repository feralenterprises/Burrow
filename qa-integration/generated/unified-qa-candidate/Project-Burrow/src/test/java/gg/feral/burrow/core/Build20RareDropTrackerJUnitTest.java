package gg.feral.burrow.core;

import gg.feral.burrow.core.RareDropTable.Drop;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Build 20 Rare Drop Tracker: Wiki-sourced data integrity, shared-roll maths, KC sources, isolation. */
final class Build20RareDropTrackerJUnitTest {
    static Drop d(String enc,String name){
        return RareDropTable.encounter(enc).orElseThrow().drops().stream().filter(x->x.name().equals(name)).findFirst().orElseThrow();
    }
    @Test void everyEncounterBelongsToARegisteredBossAndRatesAreSane() {
        for(var e:RareDropTable.all()) {
            assertTrue(BossRegistry.find(e.boss()).isPresent(),e.boss());
            assertTrue(e.wikiUrl().startsWith("https://oldschool.runescape.wiki/w/"));
            Map<String,Double> sum=new HashMap<>();
            for(var x:e.drops()) { assertTrue(x.itemId()>0); sum.merge(x.group(),x.perRoll(),Double::sum); }
            sum.values().forEach(v->assertTrue(v<1,"group under 1"));
        }
        assertEquals("2 × 1/1,024",d("Zulrah","Tanzanite fang").rateText());
        assertEquals("1/5,012.5",d("Giant Mole","Curved bone").rateText());
        assertEquals("8/512",d("Corporeal Beast","Spirit shield").rateText());
        assertEquals(3,RareDropTable.encounters("Dagannoth Kings").size(),"KC is per king");
        assertTrue(RareDropTable.encounter("Vorkath").orElseThrow().drops().stream().noneMatch(x->x.name().startsWith("Vorkath's head")),
            "guaranteed-at-KC item not modelled");
    }
    @Test void coverageIsHonest() {
        assertEquals(RareDropTable.Coverage.VERIFIED,RareDropTable.coverage("Vorkath"));
        assertEquals(RareDropTable.Coverage.PARTIAL,RareDropTable.coverage("Phantom Muspah"));
        assertEquals(RareDropTable.Coverage.VERIFIED,RareDropTable.coverage("Obor"),"B20.1 R02: giant-key chest");
        assertEquals("1/118",RareDropTable.encounter("Bryophyta").orElseThrow().drops().get(0).rateText(),"mossy-key chest");
        assertEquals(RareDropTable.Coverage.VERIFIED,RareDropTable.coverage("Barrows"),"B20.1 R02: modelled by the Barrows chest");
        assertEquals(RareDropTable.Coverage.MECHANIC_NOT_MODELLED,RareDropTable.coverage("The Nightmare"));
        assertEquals(RareDropTable.Coverage.PARTIAL,RareDropTable.coverage("Alchemical Hydra"),"missing pet reported");
        assertEquals(RareDropTable.Coverage.NOT_YET_RESEARCHED,RareDropTable.coverage("Some Future Boss"));
        for(var e:BossRegistry.all()) {
            var c=RareDropTable.coverage(e.id());
            assertNotEquals(RareDropTable.Coverage.NOT_YET_RESEARCHED,c,"every eligible boss reviewed: "+e.id());
            if(c!=RareDropTable.Coverage.VERIFIED) assertFalse(RareDropTable.coverageNote(e.id()).isBlank(),"gap explained: "+e.id());
        }
        assertEquals(RareDropTable.Coverage.VERIFIED,RareDropTable.coverage("Blood Moon"),"B20.1 R02: modelled by the Lunar Chest roll per Moon");
    }
    @Test void sharedRollsAreMutuallyExclusiveAndRollsMultiply() {
        double a=1/1024.0;
        var fang=d("Zulrah","Tanzanite fang"); var visage=d("Zulrah","Serpentine visage");
        assertEquals(1-Math.pow(1-a,2),RareDropMath.any(List.of(fang),1),1e-15);
        assertEquals(1-Math.pow(1-2*a,2),RareDropMath.any(List.of(fang,visage),1),1e-15);
        assertEquals(2*a*a,RareDropMath.all(List.of(fang,visage),1),1e-15,"both only via two rolls");
        var chest=d("General Graardor","Bandos chestplate"); var tassets=d("General Graardor","Bandos tassets");
        assertEquals(0,RareDropMath.all(List.of(chest,tassets),1),1e-15,"one roll can't give both");
        var pet=d("General Graardor","Pet general graardor");
        assertEquals((1/381.0)*(1/5000.0),RareDropMath.all(List.of(chest,pet),1),1e-15,"tertiary independent");
        assertEquals(0,RareDropMath.any(List.of(chest),0));
    }
    @Test void milestonesAreMinimalAndNeverHundredPercent() {
        var sel=List.of(d("Vorkath","Draconic visage"),d("Vorkath","Skeletal visage"));
        long prev=0;
        for(double m:RareDropMath.MILESTONES) for(var mode:RareDropMath.Mode.values()) {
            long n=RareDropMath.killsFor(mode,sel,m);
            assertTrue(RareDropMath.chance(mode,sel,n)>=m);
            assertTrue(RareDropMath.chance(mode,sel,n-1)<m,"minimal");
        }
        long any50=RareDropMath.killsFor(RareDropMath.Mode.ANY,sel,0.5);
        assertEquals((long)Math.ceil(Math.log(0.5)/(2*Math.log1p(-1/5000.0))),any50);
        assertEquals(">99.9%",RareDropMath.percent(RareDropMath.any(sel,1_000_000)));
        assertThrows(IllegalArgumentException.class,()->RareDropMath.killsFor(RareDropMath.Mode.ANY,sel,1.0));
        assertThrows(IllegalArgumentException.class,()->RareDropMath.any(List.of(),10));
    }
    @Test void collectionLogParsingNeverInventsZero() {
        var o=CollectionLogParser.parse(List.of("<col=ff981f>Dagannoth Kings</col>","Obtained: <col=ffffff>3/12</col>",
            "Dagannoth Rex kills: <col=ffffff>1,204</col>","Dagannoth Prime kills: <col=ffffff>988</col>"),
            List.of(new CollectionLogParser.Slot(6737,0,1),new CollectionLogParser.Slot(6735,175,0))).orElseThrow();
        assertEquals(Map.of("Dagannoth Rex",1204L,"Dagannoth Prime",988L),o.killCounts());
        assertFalse(o.killCounts().containsKey("Dagannoth Supreme"),"missing line stays unknown");
        assertEquals(Set.of(6737),o.obtained());
        assertEquals(Set.of(6737,6735),o.listed());
        var v=CollectionLogParser.parse(List.of("Vorkath","Vorkath kills: 77"),List.of()).orElseThrow();
        assertEquals(Map.of("Vorkath",77L),v.killCounts());
        var unknown=CollectionLogParser.parse(List.of("Vorkath","Obtained: 0/7"),List.of()).orElseThrow();
        assertTrue(unknown.killCounts().isEmpty());
        assertTrue(CollectionLogParser.parse(List.of(),List.of()).isEmpty());
    }
    @Test void storeIsPerAccountIdempotentAndLabelsSources() {
        Map<String,String> cfg=new HashMap<>();
        var store=new RareDropStore(cfg::get,cfg::put);
        long a=0x1234L, b=0x9999L;
        assertFalse(store.read(a).effective("Vorkath").known(),"unvisited is unknown");
        var obs=CollectionLogParser.parse(List.of("Vorkath","Vorkath kills: 77"),
            List.of(new CollectionLogParser.Slot(22006,0,1),new CollectionLogParser.Slot(11286,175,0))).orElseThrow();
        store.observe(a,obs,1000); store.observe(a,obs,2000);
        var s=store.read(a);
        assertEquals(77,s.effective("Vorkath").value().getAsLong(),"revisit does not double count");
        assertEquals(RareDropStore.Source.COLLECTION_LOG,s.effective("Vorkath").source());
        assertEquals(RareDropStore.Ownership.OBSERVED_OBTAINED,s.ownership(22006));
        assertEquals(RareDropStore.Ownership.OBSERVED_NOT_OBTAINED,s.ownership(11286));
        assertEquals(RareDropStore.Ownership.UNKNOWN,s.ownership(12922),"Zulrah page not visited");
        store.recordKc(a,"Vorkath",RareDropStore.Source.MANUAL,500,3000);
        assertEquals(RareDropStore.Source.COLLECTION_LOG,store.read(a).effective("Vorkath").source(),"observed beats manual");
        store.recordKc(a,"Zulrah",RareDropStore.Source.MANUAL,40,3000);
        assertEquals(RareDropStore.Source.MANUAL,store.read(a).effective("Zulrah").source());
        store.recordKc(a,"Vorkath",RareDropStore.Source.CHAT,80,4000);
        assertEquals(80,store.read(a).effective("Vorkath").value().getAsLong());
        var key=d("Vorkath","Draconic visage").key();
        store.toggle(a,key);
        assertEquals(List.of(key),store.read(a).selected());
        assertTrue(store.read(b).selected().isEmpty(),"account isolation");
        assertFalse(store.read(b).effective("Vorkath").known());
        assertEquals(RareDropStore.Ownership.UNKNOWN,store.read(b).ownership(22006));
        store.toggle(a,key);
        assertTrue(store.read(a).selected().isEmpty());
        assertThrows(IllegalArgumentException.class,()->store.toggle(-1L,key),"signed-out never writes");
        cfg.put(RareDropStore.key(a),"garbage\nK\tVorkath\tNOPE\t1\t1\nS\tmade_up:1\n");
        assertTrue(store.read(a).selected().isEmpty(),"malformed ignored");
        assertFalse(store.read(a).effective("Vorkath").known());
    }
}
