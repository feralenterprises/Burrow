package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Build 19: Best Bank presentation through the real 225px panel and its hitboxes (synthetic bank). */
final class BestBankTripUiJUnitTest {
    static MouseEvent at(BurrowPanel p,int x,int y){return new MouseEvent(p,MouseEvent.MOUSE_MOVED,0,0,x,y,0,false);}
    static BestBankPreloader.Prepared prepared(BurrowModel m,Map<Integer,Integer> own,AccountContext ctx) {
        var key=new BestBankPreloader.Key(m.boss(),m.method(),m.tier());
        var ev=new BankEvidence(own,Map.of(),Map.of(),BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT,1L,true,false);
        var plan=BestBankPlanner.plan(m.boss(),m.method(),m.tier(),ev,ctx,ItemFacts.catalogDerived());
        return new BestBankPreloader.Prepared(key,1,BestBankMatches.gear(m.profile(),ev.combined()),ev.combined(),
            TierAwareBankResolver.resolve(m.boss(),m.method(),m.tier(),ev.combined()),plan);
    }
    @Test void bestBankShowsTopStatusBlockersAndBankCheckedInventoryWhileWikiStaysExact() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var m=new BurrowModel(); m.show(BurrowModel.View.OUTFITTER);
            m.chooseBoss("Scurrius"); m.chooseTier(Tier.OPTIMAL);
            var p=new BurrowPanel(m); p.setSize(225,p.getPreferredSize().height);
            var wikiGear=new ArrayList<>(m.profile().gear()); var wikiInv=new ArrayList<>(m.profile().inventory());
            assertTrue(p.equipmentYForR13Test()>p.sourceYForR13Test(),"Wiki source stays above equipment");
            p.clickAt(150,p.sourceYForR13Test()+8-8); // BEST BANK toggle
            assertTrue(p.isBankMatchView());
            // BQA-901 BF-09 (R01): reasoned headline + explicit "not ownership-checked" Wiki label.
            assertTrue(p.bankStatusTextForTest().startsWith("BEST IN BANK UNAVAILABLE"),p.bankStatusTextForTest());
            assertTrue(p.bankStatusTextForTest().replace('\n',' ').contains("NOT ownership-checked"),p.bankStatusTextForTest());
            p.showBankCaptureState("READING BANK · BANK POPULATING","Waiting: bank still populating (2 tries)");
            assertTrue(p.bankStatusTextForTest().startsWith("READING BANK"),p.bankStatusTextForTest());
            assertTrue(p.bankStatusTextForTest().replace('\n',' ').contains("NOT ownership-checked"),p.bankStatusTextForTest());
            p.showBankCaptureState(null,null);
            int shift=p.contentShiftForTest();
            assertTrue(p.equipmentYForR13Test()>p.sourceYForR13Test()+35,"status card sits above equipment");
            // Inventory hover in Best mode without a plan is explicitly labelled Wiki.
            String inv=p.getToolTipText(at(p,30,p.equipmentYForR13Test()+281+31+5-8));
            assertTrue(inv.startsWith("WIKI SUPPLY (not bank-checked)"),inv);
            // Own everything except the Scythe.
            Map<Integer,Integer> own=new HashMap<>();
            for(Item i:m.profile().gear()) if(i!=null && i.id()>0) own.merge(i.id(),Math.max(1,i.amount()),Integer::sum);
            for(Item i:m.profile().inventory()) if(i!=null && i.id()>0) own.merge(i.id(),Math.max(1,i.amount()),Integer::sum);
            own.remove(22325);
            own.remove(28792); // CP-1: no owned lower-tier weapon either, so the Scythe stays a genuine core blocker
            var max=new HashMap<String,Integer>(); for(String s:List.of("Attack","Strength","Defence","Ranged","Prayer","Magic","Hitpoints","Agility")) max.put(s,99);
            p.automaticBankResult(prepared(m,own,new AccountContext(max,Map.of())),"test");
            String status=p.bankStatusTextForTest();
            assertTrue(status.startsWith("NOT RECOMMENDED"),status);
            assertTrue(status.replace("\n"," ").contains("Scythe of Vitur not observed"),status);
            shift=p.contentShiftForTest();
            String weapon=p.getToolTipText(at(p,35,p.equipmentYForR13Test()+43+2*46+5-8));
            assertTrue(weapon.startsWith("NOT_OBSERVED") && weapon.contains("Scythe"),weapon);
            String firstInv=p.getToolTipText(at(p,30,p.equipmentYForR13Test()+281+31+5-8));
            assertFalse(firstInv.startsWith("WIKI SUPPLY"),"plan inventory is bank-checked: "+firstInv);
            assertTrue(firstInv.startsWith("STOCKED")||firstInv.startsWith("SWITCH_OWNED"),firstInv);
            var img=new BufferedImage(225,p.getPreferredSize().height,BufferedImage.TYPE_INT_ARGB);
            p.setSize(225,p.getPreferredSize().height); p.paint(img.getGraphics());
            // Back to Wiki: exact Wiki equipment and inventory, original geometry.
            p.clickAt(40,p.sourceYForR13Test()+8-8);
            assertFalse(p.isBankMatchView());
            assertTrue(p.equipmentYForR13Test()>p.sourceYForR13Test());
            assertEquals(wikiGear,new ArrayList<>(m.profile().gear())); assertEquals(wikiInv,new ArrayList<>(m.profile().inventory()));
            String wikiWeapon=p.getToolTipText(at(p,35,p.equipmentYForR13Test()+43+2*46+5-8));
            assertTrue(wikiWeapon.startsWith("Scythe of Vitur"),wikiWeapon);
        });
    }
    @Test void staleResultForAnotherSelectionIsIgnored() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var m=new BurrowModel(); m.show(BurrowModel.View.OUTFITTER);
            var p=new BurrowPanel(m); p.clickAt(150,p.sourceYForR13Test()+8-8);
            var old=prepared(m,Map.of(1163,1),AccountContext.UNKNOWN);
            m.chooseBoss("Giant Mole");
            p.automaticBankResult(old,"late callback");
            assertNull(p.currentTripPlan(),"late result for previous boss is rejected");
        });
    }
}
