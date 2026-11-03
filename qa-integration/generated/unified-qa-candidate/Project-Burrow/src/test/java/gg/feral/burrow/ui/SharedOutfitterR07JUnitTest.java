package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.Outfitter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** R07 shared setup contract: the category filters the ONE Outfitter. */
class SharedOutfitterR07JUnitTest {
    @Test void bossAndActivityPublishSamePhysicalPlanGeometry() {
        var boss=SharedOutfitterSetup.boss(new BurrowModel().bankSelection());
        var activity=SharedOutfitterSetup.activity("slayer-melee");
        assertEquals(SharedOutfitterSetup.Category.BOSS,boss.category());
        assertEquals(SharedOutfitterSetup.Category.ACTIVITY,activity.category());
        assertEquals(56,boss.plan().cells().size());
        assertEquals(56,activity.plan().cells().size());
        assertEquals(11,activity.plan().gear().size());
        assertEquals(28,activity.plan().inventory().size());
        for(int pos:Outfitter.GEAR_POSITIONS)assertEquals(Outfitter.Kind.GEAR,activity.plan().cells().get(pos).kind());
        assertEquals(Outfitter.Kind.INVENTORY,activity.plan().cells().get(Outfitter.inventoryPosition(0)).kind());
    }
    @Test void slayerReferenceIsSelectedInsteadOfHiddenScurriusPlan() {
        var boss=SharedOutfitterSetup.boss(new BurrowModel().bankSelection());
        var melee=SharedOutfitterSetup.activity("slayer-melee");
        assertEquals("Scurrius",boss.title());
        assertEquals("Slayer · Melee",melee.title());
        assertEquals(10828,boss.plan().cells().get(1).requirement().itemId());
        assertEquals(11865,melee.plan().cells().get(1).requirement().itemId());
        assertEquals(4151,melee.plan().cells().get(16).requirement().itemId());
        assertEquals(Outfitter.Stock.UNVERIFIED,melee.plan().cells().get(1).stock());
        assertFalse(melee.plan().bankVerified());
        assertTrue(melee.bankTagPermitted());
        assertFalse(melee.bestBankPermitted());
    }
    @Test void sourceExamplesMatchSharedPlanOnActivityAndNeverInferBank() {
        for(var kit:ActivityOutfitterCatalog.all()){
            var selected=SharedOutfitterSetup.activity(kit.id());
            assertFalse(selected.plan().bankVerified());
            for(int i=0;i<11;i++){
                var ref=ActivityItemRefs.find(kit.id(),true,i);
                var c=selected.plan().cells().get(Outfitter.GEAR_POSITIONS.get(i));
                assertEquals(ref==null?null:ref.itemId(),c.requirement()==null?null:c.requirement().itemId());
            }
            for(int i=0;i<28;i++){
                var ref=ActivityItemRefs.find(kit.id(),false,i);
                var c=selected.plan().cells().get(Outfitter.inventoryPosition(i));
                assertEquals(ref==null?null:ref.itemId(),c.requirement()==null?null:c.requirement().itemId());
            }
        }
    }
    @Test void wildernessCantOpenBankTagOrBestBank() {
        var wild=SharedOutfitterSetup.activity("slayer-ranged-wilderness");
        assertFalse(wild.bestBankPermitted());
        assertFalse(wild.bankTagPermitted());
        assertEquals(Outfitter.Stock.UNVERIFIED,wild.plan().cells().get(1).stock());
    }
    @Test void originalClueBankGeometryIsPreservedAndNotCarriedInventory() {
        for(var clue:ClueBankTagLayouts.all()){
            var activity=SharedOutfitterSetup.activity(clue.id());
            int[] source=clue.slots(),planned=activity.sourcedBankLayout();
            assertEquals(56,planned.length);
            for(int i=0;i<56;i++)assertEquals(source[i]<=0?-1:source[i],planned[i]);
            assertEquals(28,activity.plan().inventory().size());
            assertTrue(activity.bankTagPermitted());
            assertFalse(activity.bestBankPermitted());
        }
    }
    @Test void panelCategoryChangeChangesPublishedPlanAndReturnRestoresBoss() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel();m.show(BurrowModel.View.OUTFITTER);
            var p=new BurrowPanel(m);
            String bossMethod=m.method();var bossTier=m.tier();
            assertEquals("Scurrius",p.selectedOutfitterSetupForHost().title());
            p.selectActivityForTest("birdhouse");
            assertEquals("Birdhouse Run",p.selectedOutfitterSetupForHost().title());
            assertEquals(21515,p.selectedOutfitterSetupForHost().plan().cells().get(Outfitter.inventoryPosition(0)).requirement().itemId());
            assertEquals(bossMethod,m.method());assertEquals(bossTier,m.tier());
            p.selectActivityForTest("slayer-melee");
            assertEquals("Slayer · Melee",p.selectedOutfitterSetupForHost().title());
            assertEquals(11865,p.selectedOutfitterSetupForHost().plan().cells().get(1).requirement().itemId());
            p.setAppearance("EXTRA_LARGE","BRONZE");
            p.clickAt(50,144-8);
            assertEquals("Scurrius",p.selectedOutfitterSetupForHost().title());
        });
    }
}
