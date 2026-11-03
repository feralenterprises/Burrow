package gg.feral.burrow.core;

import gg.feral.burrow.bib.AmmoRules;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.BestBankPlanner.*;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import java.util.*;

/** W07 source-only offline contract: synthetic bank data, not a native RuneLite QA claim. */
public final class Wave7OutfitterRegressionTest {
    private Wave7OutfitterRegressionTest() {}
    static final AccountContext MAX;
    static {
        Map<String,Integer> m=new HashMap<>();
        for(String s:List.of("Attack","Strength","Defence","Ranged","Prayer","Magic","Runecraft",
            "Construction","Hitpoints","Agility","Herblore","Thieving","Crafting","Fletching",
            "Slayer","Hunter","Mining","Smithing","Fishing","Cooking","Firemaking","Woodcutting","Farming","Sailing")) m.put(s,99);
        MAX=new AccountContext(m,Map.of("Dragon Slayer I","FINISHED","Dragon Slayer II","FINISHED"));
    }
    static void ok(boolean b,String msg) {if(!b)throw new AssertionError(msg);}
    static BankEvidence evidence(Map<Integer,Integer> items) {
        return new BankEvidence(items,Map.of(),Map.of(),BankEvidence.Population.STABLE_READ,1L,true,false);
    }
    static Map<Integer,Integer> owned(VerifiedOutfitterCatalog.Profile p,boolean inventory) {
        Map<Integer,Integer> b=new HashMap<>();
        for(Item it:p.gear())if(it!=null&&it.id()>0)b.merge(it.id(),Math.max(1,it.amount()),Integer::sum);
        if(inventory)for(Item it:p.inventory())if(it!=null&&it.id()>0)b.merge(it.id(),Math.max(1,it.amount()),Integer::sum);
        return b;
    }
    static String findMethod(String boss,String fragment) {
        return TieredOutfitterCatalog.methods(boss).stream().filter(m->m.contains(fragment)).findFirst().orElseThrow();
    }
    public static void main(String[] args) throws Exception {
        String kree="Kree'arra", tank=findMethod(kree,"Ranged (tank)");
        var kp=TieredOutfitterCatalog.find(kree,tank,Tier.ENTRY).profile();
        ok(kp.gear().get(4).name().equals("Twisted bow"),"Kree source weapon changed");
        ok(kp.gear().get(3).id()==33595,"Kree's Twisted bow is not paired with exact seeking dragon arrow");
        var k=BestBankPlanner.plan(kree,tank,Tier.ENTRY,evidence(owned(kp,true)),MAX,ItemFacts.catalogDerived());
        ok(k.verdict()!=Verdict.READY,"One-arrow Kree trip must not claim READY");
        ok(k.checks().stream().anyMatch(s->s.contains("trip arrow quantity")),"Kree unknown quantity must be visible");

        String levi="The Leviathan", ranged=findMethod(levi,"Ranged");
        var lp=TieredOutfitterCatalog.find(levi,ranged,Tier.ENTRY).profile();
        ok(lp.gear().get(4).name().equals("Rune crossbow"),"Levi source weapon changed");
        ok(lp.gear().get(3).name().equals("Ruby bolts (e)"),"Levi source ammo not corrected");
        ok(AmmoRules.check(lp.gear().get(4).name(),lp.gear().get(3).name())==AmmoRules.Status.OK,
            "Levi bolts cannot be fired with rune crossbow");
        var l=BestBankPlanner.plan(levi,ranged,Tier.ENTRY,evidence(owned(lp,true)),MAX,ItemFacts.catalogDerived());
        ok(l.verdict()!=Verdict.NOT_READY,"Fully-owned Levi source has blocker: "+l.blockers());

        String sc="Scurrius", method=findMethod(sc,"Standard melee");
        var sp=TieredOutfitterCatalog.find(sc,method,Tier.OPTIMAL).profile();
        var s=BestBankPlanner.plan(sc,method,Tier.OPTIMAL,evidence(owned(sp,false)),MAX,ItemFacts.catalogDerived());
        ok(s.inventory().stream().anyMatch(c->c.status()==SupplyStatus.SWITCH_NOT_OBSERVED),"No missing switch in fixture");
        ok(s.switches().isEmpty(),"Absent switch received simulated fit approval");
        ok(s.usedSlots()+s.freeSlots()==28,"Inventory geometry changed");

        var low=ItemVariantPolicy.resolve(11978,Set.of(1706));
        ok(low.layoutId()==-1 && low.kind()==ItemVariantPolicy.Kind.BLOCKED_UNSAFE_VARIANT,
            "A one-charge glory satisfied six-charge plan");
        ok(ItemVariantPolicy.resolveForTravel(1712,Set.of(11964),6).layoutId()==11964,
            "Six-charge trimmed glory was not accepted");
        ok(ItemVariantPolicy.resolveForCombatStats(1712,Set.of(1706)).layoutId()==1706,
            "Combat-only stat equivalence was lost");

        Item weapon=new Item(9185,"Rune crossbow",1);
        int[] layout=new int[56];Arrays.fill(layout,-1);
        layout[gg.feral.burrow.core.Outfitter.GEAR_POSITIONS.get(4)]=BankLayoutEligibility.wornId(
            new GearCell(weapon,weapon,GearStatus.LEVEL_UNMET,"61 Ranged needed"));
        ok(WithdrawableBankLayout.withdrawableSlots(
            WithdrawableBankLayout.fromBestPlan(layout,Map.of(9185,1)))==0,
            "An owned but unwearable item reached Bank O withdrawable slots");
        int unapproved= (int) gg.feral.burrow.intelligence.GearKnowledge.bundled().approvedItemCount();
        ok(unapproved==0,"Research was activated without independent gate");
        System.out.println("Wave7OutfitterRegressionTest PASS: 7 targeted source/evidence contracts");
    }
}
