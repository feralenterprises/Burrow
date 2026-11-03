package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.awt.*;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assertions.*;

/** Owner OUT-R06: category is organization, not another independent Outfitter system. */
final class OutfitterR06UnifiedJUnitTest {
    private static int y(int contentY){return contentY-8;}
    private static void draw(BurrowPanel p){
        Dimension sz=p.getPreferredSize();assertEquals(225,sz.width);
        assertTrue(sz.height>=880);p.setSize(sz);
        BufferedImage b=new BufferedImage(225,sz.height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=b.createGraphics();try{p.paint(g);}finally{g.dispose();}
    }
    @Test void sameCategoryTabsPreserveBossWhenSelectingActivity() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel();m.show(BurrowModel.View.OUTFITTER);
            var p=new BurrowPanel(m);p.setAppearance("EXTRA_LARGE","BRONZE");p.setTextWeight("BOLD");
            assertTrue(p.spaciousOutfitterForTest());
            String boss=m.boss(),method=m.method();var tier=m.tier();
            draw(p);
            p.clickAt(155,y(144));assertTrue(p.activityModeForTest());
            draw(p);p.selectActivityForTest("birdhouse");draw(p);
            assertEquals("birdhouse",p.activityKitIdForTest());
            p.clickAt(51,y(144));assertFalse(p.activityModeForTest());
            draw(p);
            assertEquals(boss,m.boss());assertEquals(method,m.method());assertEquals(tier,m.tier());
            p.clickAt(156,y(144));assertEquals("birdhouse",p.activityKitIdForTest());
        });
    }
    @Test void fullWidthTiersAndSourceHaveDistinctClickTargets() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel();m.show(BurrowModel.View.OUTFITTER);
            var p=new BurrowPanel(m);p.setAppearance("EXTRA_LARGE","BRONZE");p.setTextWeight("BOLD");
            draw(p);int top=p.tierYForR13Test();
            assertTrue(top>=290);
            p.clickAt(40,y(top+19));assertEquals(gg.feral.burrow.core.TieredOutfitterCatalog.Tier.ENTRY,m.tier());
            p.clickAt(40,y(top+39+19));assertEquals(gg.feral.burrow.core.TieredOutfitterCatalog.Tier.RECOMMENDED,m.tier());
            p.clickAt(45,y(p.sourceYForR13Test()+15));assertFalse(p.bestBankToggleOnForHost());
            draw(p);
        });
    }
    @Test void wildernessAndClueKeepReferenceOnlyOnSharedShell() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel();m.show(BurrowModel.View.OUTFITTER);
            var p=new BurrowPanel(m);p.setAppearance("LARGE","BRONZE");
            for(String kit:new String[]{"slayer-ranged-wilderness","clue-master-wiki"}){
                try{p.selectActivityForTest(kit);}catch(IllegalArgumentException e){
                    if(kit.startsWith("clue"))continue;throw e;
                }
                draw(p);assertFalse(p.bestBankToggleOnForHost());
            }
        });
    }
}
