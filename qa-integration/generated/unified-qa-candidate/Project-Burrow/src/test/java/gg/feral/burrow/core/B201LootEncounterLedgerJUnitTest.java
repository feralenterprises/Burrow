package gg.feral.burrow.core;

import gg.feral.burrow.core.LootEncounterLedger.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** B20.1: loot-confirmed encounter estimate. Synthetic events only. */
final class B201LootEncounterLedgerJUnitTest {
    static Observation npc(Object id,String name,int tick,int[]... stacks){return new Observation(id,name,RecordType.NPC,1,tick,List.of(stacks));}
    @Test void oneEventWithTwoStacksIsOneEncounter() {
        var l=new LootEncounterLedger(); Object login="A1"; l.bind(login);
        assertEquals(Outcome.COUNTED,l.accept(login,npc(new Object(),"Tormented Demon",100,new int[]{20718,1},new int[]{211,1})));
        assertEquals(Map.of("Tormented Demons",1),l.counts(),"never +2 for two item stacks");
    }
    @Test void duplicateSubscribersAndRepostsCountOnce() {
        var l=new LootEncounterLedger(); Object login="A1"; l.bind(login);
        Object ev=new Object();
        assertEquals(Outcome.COUNTED,l.accept(login,npc(ev,"Vorkath",50,new int[]{536,2})));
        assertEquals(Outcome.DUPLICATE,l.accept(login,npc(ev,"Vorkath",50,new int[]{536,2})),"same object twice");
        assertEquals(Outcome.DUPLICATE,l.accept(login,npc(new Object(),"Vorkath",50,new int[]{536,2})),"equal re-post same tick");
        assertEquals(Outcome.COUNTED,l.accept(login,npc(new Object(),"Vorkath",80,new int[]{536,2})),"next kill counts");
        assertEquals(2,l.count("Vorkath"));
    }
    @Test void onlyVettedNpcRecordsCount() {
        var l=new LootEncounterLedger(); Object login="A1"; l.bind(login);
        assertEquals(Outcome.NOT_NPC,l.accept(login,new Observation(new Object(),"Barrows",RecordType.EVENT,1,1,List.<int[]>of(new int[]{995,500}))));
        assertEquals(Outcome.NOT_NPC,l.accept(login,new Observation(new Object(),"Lunar Chest",RecordType.EVENT,1,1,List.<int[]>of(new int[]{995,1}))));
        assertEquals(Outcome.NOT_VETTED,l.accept(login,npc(new Object(),"Dusk",2,new int[]{995,1})),"GG pair excluded");
        assertEquals(Outcome.NOT_VETTED,l.accept(login,npc(new Object(),"Goblin",3,new int[]{526,1})));
        assertEquals(Outcome.NOT_VETTED,l.accept(login,npc(new Object(),"Araxxor",4,new int[]{995,1})),"destroy option breaks 1:1");
        assertEquals(Outcome.AMBIGUOUS_AMOUNT,l.accept(login,new Observation(new Object(),"Vorkath",RecordType.NPC,2,5,List.<int[]>of(new int[]{536,2}))));
        assertEquals(Outcome.NO_ITEMS,l.accept(login,npc(new Object(),"Vorkath",6)),"empty event");
        assertEquals(Outcome.NO_ITEMS,l.accept(login,npc(new Object(),"Vorkath",7,new int[]{-1,1},new int[]{536,0})),"invalid stacks");
        assertTrue(l.counts().isEmpty());
        assertEquals(Outcome.COUNTED,l.accept(login,npc(new Object(),"<col=ffff00>Dagannoth Rex</col>",8,new int[]{6737,1})));
        assertEquals(1,l.count("Dagannoth Rex"),"per-king KC unit");
    }
    @Test void loginScopeIsolationAndHopContinuity() {
        var l=new LootEncounterLedger();
        assertEquals(Outcome.NO_LOGIN,l.accept("A1",npc(new Object(),"Zulrah",1,new int[]{12934,100})),"before any login");
        l.bind("A1");
        l.accept("A1",npc(new Object(),"Zulrah",1,new int[]{12934,100}));
        l.bind("A1"); // LOADING / world hop keeps the same login scope
        l.accept("A1",npc(new Object(),"Zulrah",40,new int[]{12934,90}));
        assertEquals(2,l.count("Zulrah"));
        assertEquals(Outcome.NO_LOGIN,l.accept("B1",npc(new Object(),"Zulrah",41,new int[]{12934,1})),"late event from another login");
        l.bind("B1"); assertTrue(l.counts().isEmpty(),"A->B resets");
        l.accept("B1",npc(new Object(),"Zulrah",50,new int[]{12934,1}));
        l.bind("A2"); assertTrue(l.counts().isEmpty(),"B->A is a new login, not A's old numbers");
        l.bind(null); assertEquals(Outcome.NO_LOGIN,l.accept("A2",npc(new Object(),"Zulrah",60,new int[]{12934,1})),"logged out");
    }
    @Test void ledgerNeverTouchesAuthoritativeKc() {
        Map<String,String> cfg=new HashMap<>(); var store=new RareDropStore(cfg::get,cfg::put);
        long hash=7L;
        store.recordKc(hash,"Vorkath",RareDropStore.Source.CHAT,120,1);
        var l=new LootEncounterLedger(); l.bind("A1");
        for(int t=0;t<5;t++) l.accept("A1",npc(new Object(),"Vorkath",t,new int[]{536,2}));
        assertEquals(5,l.count("Vorkath"));
        assertEquals(120,store.read(hash).effective("Vorkath").value().getAsLong(),"estimate never added to KC");
        assertEquals(RareDropStore.Source.CHAT,store.read(hash).effective("Vorkath").source());
        for(String enc:LootEncounterLedger.vettedEncounters())
            assertTrue(BossRegistry.find(enc).isPresent() || RareDropTable.encounter(enc).isPresent(),"maps to a real encounter: "+enc);
    }
}
