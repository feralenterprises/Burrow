package gg.feral.burrow.core;

import gg.feral.burrow.core.BestBankPlanner.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Profile;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Build 19 Best in Bank reproduction fixtures (synthetic item maps, not live accounts). */
final class BestBankPlannerJUnitTest {
    static final Map<String,Integer> MAXED=new HashMap<>();
    static { for(String s:List.of("Attack","Strength","Defence","Ranged","Prayer","Magic","Runecraft","Construction",
        "Hitpoints","Agility","Herblore","Thieving","Crafting","Fletching","Slayer","Hunter","Mining","Smithing",
        "Fishing","Cooking","Firemaking","Woodcutting","Farming","Sailing")) MAXED.put(s,99); }
    static final AccountContext MAX=new AccountContext(MAXED,Map.of("Dragon Slayer I","FINISHED","Dragon Slayer II","FINISHED"));

    static Profile profile(String boss,String method,Tier t){return TieredOutfitterCatalog.find(boss,method,t).profile();}
    /** Every gear + inventory item of a profile in the exact planned quantity. */
    static Map<Integer,Integer> ownAll(Profile p) {
        Map<Integer,Integer> m=new HashMap<>();
        for(Item i:p.gear()) if(i!=null && i.id()>0) m.merge(i.id(),Math.max(1,i.amount()),Integer::sum);
        for(Item i:p.inventory()) if(i!=null && i.id()>0) m.merge(i.id(),Math.max(1,i.amount()),Integer::sum);
        return m;
    }
    static BankEvidence bank(Map<Integer,Integer> m){
        return new BankEvidence(m,Map.of(),Map.of(),BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT,1L,true,false);
    }
    static Plan plan(String b,String m,Tier t,Map<Integer,Integer> own,AccountContext ctx){
        return BestBankPlanner.plan(b,m,t,bank(own),ctx,ItemFacts.catalogDerived());
    }

    @Test void noSnapshotIsUnknownAndShowsUnchangedWiki() {
        var wiki=profile("Scurrius","Standard melee",Tier.OPTIMAL);
        var p=BestBankPlanner.plan("Scurrius","Standard melee",Tier.OPTIMAL,null,MAX,null);
        assertEquals(Verdict.UNKNOWN,p.verdict());
        for(int i=0;i<11;i++) assertEquals(wiki.gear().get(i),p.gear().get(i).shown());
        for(int i=0;i<28;i++) assertEquals(wiki.inventory().get(i),p.inventory().get(i).shown());
        assertFalse(p.playable());
        // Equipment/inventory present but bank container empty: still UNKNOWN, not "missing".
        var noBank=new BankEvidence(Map.of(),Map.of(385,5),Map.of(4151,1),BankEvidence.Population.SINGLE_READ,1L,true,false);
        assertEquals(Verdict.UNKNOWN,BestBankPlanner.plan("Scurrius","Standard melee",Tier.OPTIMAL,noBank,MAX,null).verdict());
    }

    @Test void fullyOwnedProfileIsTripReadyWithFull28SlotManifest() {
        var wiki=profile("Brutus","Standard free-to-play",Tier.RECOMMENDED);
        var p=plan("Brutus","Standard free-to-play",Tier.RECOMMENDED,ownAll(wiki),MAX);
        // Cowbell amulet (-1 id) is an explicitly unspecified variant in the source: VERIFY, not READY.
        assertEquals(Verdict.VERIFY,p.verdict(),String.valueOf(p.checks()));
        assertTrue(p.blockers().isEmpty(),String.valueOf(p.blockers()));
        assertEquals(Tier.RECOMMENDED,p.shownTier());
        assertEquals(28,p.inventory().size());
        assertTrue(p.inventory().stream().filter(c->c.planned()!=null && c.planned().id()==333)
            .allMatch(c->c.status()==SupplyStatus.STOCKED),"every trout cell bank-checked");
        assertEquals(p.usedSlots()+p.freeSlots(),28);
    }

    @Test void scurriusOptimalWithoutScytheIsNotRecommendedWithItemizedBlocker() {
        var opt=profile("Scurrius","Standard melee",Tier.OPTIMAL);
        var own=ownAll(opt); own.remove(22325);
        own.remove(28792); // CP-1: and no owned lower-tier weapon (the Optimal pack's Bone mace switch)
        var p=plan("Scurrius","Standard melee",Tier.OPTIMAL,own,MAX);
        assertEquals(Verdict.NOT_READY,p.verdict());
        assertEquals(Tier.OPTIMAL,p.shownTier(),"no lower tier is coherent here, so selected tier and its blockers are shown");
        assertTrue(p.blockers().stream().anyMatch(b->b.startsWith("CORE Weapon") && b.contains("Scythe")),String.valueOf(p.blockers()));
        assertFalse(p.playable(),"never present a six-swap mix as playable");
    }

    /** CP-1: the owned Torva set is kept and the owned Bone mace (from the Optimal pack) becomes the weapon. */
    @Test void scurriusOptimalWithoutScytheKeepsOwnedArmourAndUsesOwnedBoneMace() {
        var opt=profile("Scurrius","Standard melee",Tier.OPTIMAL);
        var own=ownAll(opt); own.remove(22325);
        var p=plan("Scurrius","Standard melee",Tier.OPTIMAL,own,MAX);
        assertTrue(p.playable() || p.verdict()==Verdict.VERIFY,p.headline()+" "+p.blockers());
        assertEquals(Tier.OPTIMAL,p.shownTier(),String.valueOf(p.candidateSummaries()));
        assertEquals(28792,p.gear().get(4).shown().id(),"owned Bone mace is the weapon");
        assertEquals(26384,p.gear().get(5).shown().id(),"owned Torva body kept, not a bare lower tier");
        assertTrue(p.inventory().stream().noneMatch(c->c.status()==SupplyStatus.SWITCH_NOT_OBSERVED),"worn Bone mace is not demanded again as a switch");
        assertTrue(p.blockers().isEmpty(),String.valueOf(p.blockers()));
    }

    /** CP-1 (TRIP-X02): an empty optional ring with every combat-required item owned is not a rejection. */
    @Test void missingRingAloneNeverRejectsAnOtherwiseOwnedTrip() {
        var rec=profile("Scurrius","Standard melee",Tier.RECOMMENDED);
        var own=ownAll(rec); own.remove(11773);
        var p=plan("Scurrius","Standard melee",Tier.RECOMMENDED,own,MAX);
        assertNotEquals(Verdict.NOT_READY,p.verdict(),String.valueOf(p.blockers()));
        assertEquals(GearStatus.OPTIONAL_MISSING,p.gear().get(10).status());
        assertTrue(p.upgrades().stream().anyMatch(u->u.startsWith("Ring")),String.valueOf(p.upgrades()));
        assertTrue(p.headline().contains("optional slot"),p.headline());
    }

    @Test void scurriusOptimalFallsBackToWholeOwnedRecommendedTier() {
        var opt=profile("Scurrius","Standard melee",Tier.OPTIMAL);
        var rec=profile("Scurrius","Standard melee",Tier.RECOMMENDED);
        var own=ownAll(rec); // everything for Recommended, nothing Optimal-only
        var p=plan("Scurrius","Standard melee",Tier.OPTIMAL,own,MAX);
        assertEquals(Tier.RECOMMENDED,p.shownTier(),String.valueOf(p.candidateSummaries()));
        assertTrue(p.fallback());
        assertNotEquals(Verdict.NOT_READY,p.verdict());
        for(int i=0;i<11;i++) {
            Item shown=p.gear().get(i).shown(), planned=rec.gear().get(i);
            if(planned!=null) assertEquals(planned.id(),shown.id(),"fallback is the WHOLE Recommended set, slot "+i);
        }
        assertEquals(opt,profile("Scurrius","Standard melee",Tier.OPTIMAL),"Wiki Optimal unchanged");
        assertTrue(p.candidateSummaries().get(0).startsWith("Optimal: NOT_READY"));
    }

    @Test void levelAndQuestGatesBlockOwnedGear() {
        var wiki=profile("Brutus","Standard free-to-play",Tier.RECOMMENDED);
        var low=new AccountContext(Map.of("Defence",30,"Attack",60,"Strength",60),Map.of());
        var p=plan("Brutus","Standard free-to-play",Tier.RECOMMENDED,ownAll(wiki),low);
        assertEquals(GearStatus.LEVEL_UNMET,p.gear().get(0).status(),"Rune full helm needs 40 Defence");
        assertTrue(p.blockers().stream().anyMatch(b->b.contains("40 Defence (observed 30)")));
        assertNotEquals(Verdict.READY,p.verdict());
        var unknown=plan("Brutus","Standard free-to-play",Tier.RECOMMENDED,ownAll(wiki),AccountContext.UNKNOWN);
        assertEquals(GearStatus.REQUIREMENT_UNKNOWN,unknown.gear().get(0).status(),"unobserved level is UNKNOWN, not met");
        assertEquals(Verdict.VERIFY,unknown.verdict());
    }

    @Test void brutusRangedPrefersStockedCompatiblePairAndAllocatesArrowsOnce() {
        Map<Integer,Integer> own=new HashMap<>(Map.of(853,1,890,1,849,1,888,120,1133,1,1097,1,1063,1));
        var p=plan("Brutus","Ranged (free-to-play)",Tier.RECOMMENDED,own,MAX);
        assertEquals(853,p.gear().get(4).shown().id(),"maple bow");
        assertEquals(888,p.gear().get(3).shown().id(),"mithril arrows, not 1 adamant arrow");
        assertTrue(p.blockers().stream().noneMatch(b->b.startsWith("CORE")),"compatible stocked pair: no core blocker "+p.blockers());
        // CP-1 (TRIP-X02): unowned armour is an optional empty slot, still itemized as an upgrade, never a blocker.
        assertTrue(p.upgrades().stream().anyMatch(b->b.startsWith("Head")),"unowned armour is still itemized, never hidden "+p.upgrades());
        assertTrue(p.blockers().stream().noneMatch(b->b.startsWith("Head")),String.valueOf(p.blockers()));
        assertEquals(GearStatus.OPTIONAL_MISSING,p.gear().get(0).status());
        var one=plan("Brutus","Ranged (free-to-play)",Tier.RECOMMENDED,Map.of(853,1,890,44),MAX);
        assertEquals(GearStatus.LOW_STOCK,one.gear().get(3).status());
        assertTrue(one.shortages().stream().anyMatch(s->s.contains("Low stock")));
        var none=plan("Brutus","Ranged (free-to-play)",Tier.RECOMMENDED,Map.of(849,1,890,44),MAX);
        assertEquals(Verdict.NOT_READY,none.verdict(),"willow cannot fire adamant arrows: no compatible ammo");
    }

    @Test void vorkathMissingShieldIsCoreBlocker() {
        var rec=profile("Vorkath","Ranged crossbow",Tier.RECOMMENDED);
        Item shield=rec.gear().get(6);
        assertNotNull(shield,"Vorkath plan uses a dragonfire shield");
        var own=ownAll(rec); own.remove(shield.id());
        // Remove lower-tier shields too so no fallback can be coherent.
        for(Tier t:Tier.values()){var s=TieredOutfitterCatalog.find("Vorkath","Ranged crossbow",t);
            if(s!=null && s.profile().gear().get(6)!=null) own.remove(s.profile().gear().get(6).id());}
        var p=plan("Vorkath","Ranged crossbow",Tier.RECOMMENDED,own,MAX);
        assertEquals(Verdict.NOT_READY,p.verdict());
        assertTrue(p.blockers().stream().anyMatch(b->b.startsWith("CORE Shield")),String.valueOf(p.blockers()));
    }

    @Test void tormentedDemonSwitchCapacityCountsPhysicalCells() {
        String td=DemonEncounterLoadouts.TD;
        var ranged=profile(td,DemonEncounterLoadouts.RANGED,Tier.RECOMMENDED);
        var p=plan(td,DemonEncounterLoadouts.RANGED,Tier.RECOMMENDED,ownAll(ranged),MAX);
        var sw=p.switches().stream().filter(s->s.label().contains("Emberlight")).findFirst().orElseThrow();
        assertTrue(sw.verified(),"Emberlight one-handed is a reviewed fact");
        assertTrue(sw.fits(),"bow -> Emberlight + defender fits with the plan's free cells");
        // Melee primary wearing Emberlight + defender, switching to the 2H bow: needs one free cell.
        var melee=profile(td,DemonEncounterLoadouts.MELEE,Tier.RECOMMENDED);
        var pm=plan(td,DemonEncounterLoadouts.MELEE,Tier.RECOMMENDED,ownAll(melee),MAX);
        List<GearCell> worn=pm.gear();
        List<Item> full=new ArrayList<>(melee.inventory());
        for(int i=0;i<28;i++) if(full.get(i)==null) full.set(i,new Item(385,"Shark",1));
        var cramped=BestBankPlanner.simulateSwitches(worn,full,0,ItemFacts.catalogDerived());
        var bow=cramped.stream().filter(s->s.label().contains("Scorching bow")).findFirst().orElseThrow();
        assertFalse(bow.fits(),"0 free cells: the displaced defender has nowhere to go");
        assertEquals(1,bow.freeSlotsNeeded());
        var roomy=BestBankPlanner.simulateSwitches(worn,melee.inventory(),pm.freeSlots(),ItemFacts.catalogDerived());
        assertTrue(roomy.stream().allMatch(SwitchCheck::fits));
        // Missing switch is a blocker even when worn gear is owned.
        var own=ownAll(melee); own.remove(29591);
        var noBow=plan(td,DemonEncounterLoadouts.MELEE,Tier.RECOMMENDED,own,MAX);
        assertTrue(noBow.blockers().stream().anyMatch(b->b.contains("Switch Scorching bow not observed")));
        // Owning Emberlight is not TD-ready.
        var only=plan(td,DemonEncounterLoadouts.MELEE,Tier.RECOMMENDED,Map.of(29589,1),MAX);
        assertEquals(Verdict.NOT_READY,only.verdict());
    }

    @Test void twoHandedFactsDriveShieldDisplacement() {
        var facts=ItemFacts.catalogDerived();
        assertEquals(ItemFacts.Hands.TWO,facts.find(29591).orElseThrow().hands(),"Scorching bow");
        assertEquals(ItemFacts.SHIELD,facts.find(12954).orElseThrow().equipmentSlot(),"Dragon defender");
        // Host (RuneLite ItemManager) facts override catalog-derived facts.
        var host=ItemFacts.of(Map.of(29591,new ItemFacts.Fact(ItemFacts.WEAPON,ItemFacts.Hands.ONE,"test host")));
        assertEquals(ItemFacts.Hands.ONE,facts.overlay(host).find(29591).orElseThrow().hands());
        // Unknown hand count -> the switch is reported unverified instead of claiming it fits.
        var melee=profile(DemonEncounterLoadouts.TD,DemonEncounterLoadouts.MELEE,Tier.RECOMMENDED);
        List<GearCell> worn=new ArrayList<>();
        for(int i=0;i<11;i++){Item it=melee.gear().get(i); worn.add(new GearCell(it,it,it==null?GearStatus.EMPTY:GearStatus.OWNED,""));}
        var unknownHands=ItemFacts.of(Map.of(29591,new ItemFacts.Fact(ItemFacts.WEAPON,ItemFacts.Hands.UNKNOWN,"test"),
            12954,new ItemFacts.Fact(ItemFacts.SHIELD,ItemFacts.Hands.UNKNOWN,"test")));
        var checks=BestBankPlanner.simulateSwitches(worn,melee.inventory(),10,unknownHands);
        assertFalse(checks.get(0).verified());
    }

    @Test void suppliesAllocatedOnceAndLowerDoseLabelled() {
        var wiki=profile("Giant Mole","Melee and prayer budget",Tier.RECOMMENDED);
        long prayerSlots=wiki.inventory().stream().filter(i->i!=null && i.id()==2434).count();
        assertTrue(prayerSlots>=4);
        var own=ownAll(wiki);
        own.put(2434,2); own.put(139,50); // two (4)-dose, plenty of (3)-dose
        var p=plan("Giant Mole","Melee and prayer budget",Tier.RECOMMENDED,own,MAX);
        long stocked=p.inventory().stream().filter(c->c.planned()!=null && c.planned().id()==2434 && c.status()==SupplyStatus.STOCKED).count();
        long lower=p.inventory().stream().filter(c->c.planned()!=null && c.planned().id()==2434 && c.status()==SupplyStatus.LOWER_DOSE).count();
        assertEquals(2,stocked,"physical (4)-dose stock used once");
        assertEquals(prayerSlots-2,lower);
        assertTrue(p.inventory().stream().filter(c->c.status()==SupplyStatus.LOWER_DOSE).allMatch(c->c.shown().id()==139));
        own.put(139,0); own.remove(139); own.put(2434,1);
        var short_=plan("Giant Mole","Melee and prayer budget",Tier.RECOMMENDED,own,MAX);
        assertTrue(short_.inventory().stream().anyMatch(c->c.status()==SupplyStatus.NOT_OBSERVED && c.planned().id()==2434));
        assertNotEquals(Verdict.READY,short_.verdict());
    }

    @Test void staleEvidenceCannotBeReady() {
        var wiki=profile("Giant Mole","Melee and prayer budget",Tier.RECOMMENDED);
        var stale=bank(ownAll(wiki)).markStale();
        var p=BestBankPlanner.plan("Giant Mole","Melee and prayer budget",Tier.RECOMMENDED,stale,MAX,null);
        assertTrue(p.checks().stream().anyMatch(c->c.contains("reopen bank")));
        assertNotEquals(Verdict.READY,p.verdict());
        assertTrue(p.trust().startsWith("STALE"));
    }

    @Test void chargedItemsAndPlaceholderSafety() {
        // Plain berserker ring cannot satisfy an imbued plan (existing variant policy), so not owned.
        var opt=profile("Scurrius","Standard melee",Tier.RECOMMENDED);
        var own=ownAll(opt); own.remove(11773); own.put(6737,1);
        var p=plan("Scurrius","Standard melee",Tier.RECOMMENDED,own,MAX);
        assertNotEquals(GearStatus.OWNED,p.gear().get(10).status());
        assertThrows(IllegalArgumentException.class,()->new BankEvidence(Map.of(4151,0),Map.of(),Map.of(),
            BankEvidence.Population.SINGLE_READ,0,true,false),"zero/placeholder quantities rejected");
    }

    @Test void preloaderGenerationsPacingPauseAndOnDemand() throws Exception {
        var key=new BestBankPreloader.Key("Giant Mole","Melee and prayer budget",Tier.RECOMMENDED);
        var keys=BestBankPreloader.knownProfiles();
        try(var pre=new BestBankPreloader(keys)) {
            pre.pauseForOutfitter(true);
            long g1=pre.observe(bank(ownAll(profile("Giant Mole","Melee and prayer budget",Tier.RECOMMENDED))),MAX,ItemFacts.catalogDerived());
            Thread.sleep(700);
            assertEquals(0,pre.computations(),"no background work while Outfitter is open");
            CountDownLatch l=new CountDownLatch(1); AtomicReference<BestBankPreloader.Prepared> got=new AtomicReference<>();
            pre.request(key,r->{got.set(r);l.countDown();});
            assertTrue(l.await(3,TimeUnit.SECONDS));
            assertEquals(1,pre.computations(),"selected loadout computed on demand only");
            assertNotNull(got.get().plan());
            assertEquals(g1,got.get().generation());
            long g2=pre.updateContext(new AccountContext(Map.of("Defence",1),Map.of()));
            assertTrue(g2>g1,"context change retires old plans");
            assertTrue(pre.cached(key).isEmpty());
            pre.pauseForOutfitter(false);
            Thread.sleep(1100);
            int done=pre.computations();
            assertTrue(done>=2 && done<=7,"~one profile per 250ms, was "+done);
            pre.clear();
            assertTrue(pre.cached(key).isEmpty());
            CountDownLatch never=new CountDownLatch(1);
            pre.request(key,r->never.countDown());
            assertFalse(never.await(400,TimeUnit.MILLISECONDS),"no result after account/logout clear");
        }
    }
}
