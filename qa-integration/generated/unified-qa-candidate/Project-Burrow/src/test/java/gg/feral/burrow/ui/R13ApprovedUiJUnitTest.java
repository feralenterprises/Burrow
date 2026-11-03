package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.CompletedSessionHistory;
import gg.feral.burrow.core.TieredOutfitterCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

/** R13 visual-interaction contract. Pure Swing fixtures, NOT native RuneLite evidence. */
final class R13ApprovedUiJUnitTest {
    @Test void outFitterChronicleKeepsBossAndTierAndMovesRareDropsAfterInventory() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var model=new BurrowModel();model.show(BurrowModel.View.OUTFITTER);model.chooseBoss("Vorkath");
            var panel=new BurrowPanel(model);panel.setSize(225,1500);
            String boss=model.boss();var tier=model.tier();var method=model.method();
            int oldGearY=panel.equipmentYForR13Test();
            int chipY=panel.rareDropChipClickYForTest();
            assertTrue(chipY>oldGearY+281+290,"rare drop section follows 28 inventory positions");
            panel.clickAt(160,chipY);assertTrue(panel.rareDropExpanded());
            assertEquals(oldGearY,panel.equipmentYForR13Test(),"expanded drop data never shifts gear or its hitboxes");
            panel.clickAt(196,panel.rareDropClickYForTest(10));assertFalse(panel.rareDropExpanded());
            panel.clickAt(174,133-8+15);assertTrue(panel.isChronicleOpenForR13Test());
            panel.clickAt(45,130-8+10);assertFalse(panel.isChronicleOpenForR13Test());
            assertEquals(boss,model.boss());assertEquals(tier,model.tier());assertEquals(method,model.method());
            assertEquals(BurrowModel.View.OUTFITTER,model.view(),"Chronicle is never a fifth tab");
            paint(panel,225);paint(panel,242);
        });
    }
    @Test void singleStyleDifferencesDoNotImplySpecialMethodButDharokAndHybridDo() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var model=new BurrowModel();model.show(BurrowModel.View.OUTFITTER);
            var panel=new BurrowPanel(model);
            model.chooseBoss("Sarachnis");assertFalse(panel.hasSpecialMethodsForR13Test());
            model.chooseBoss("Giant Mole");assertTrue(panel.hasSpecialMethodsForR13Test());
            String dharok=model.methods().stream().filter(m->m.toLowerCase().contains("dharok")).findFirst().orElseThrow();
            model.chooseMethod(dharok);
            int y=panel.equipmentYForR13Test()+6-8;
            panel.clickAt(103,y+10);assertEquals(dharok,model.method(),"active style must preserve special method");
            model.chooseBoss("Dagannoth Kings");assertTrue(panel.hasSpecialMethodsForR13Test(),"hybrid has separate selector");
            model.chooseBoss("Vorkath");assertFalse(panel.hasSpecialMethodsForR13Test());
            var method=model.method();
            int styleY=panel.equipmentYForR13Test()+6-8+10;
            panel.clickAt(189,styleY);assertEquals(method,model.method(),"unsupported magic style cannot change method");
            assertTrue(panel.styleFeedbackForTest()!=null);
        });
    }
    @Test void sessionsOnlyPaintActuallySuppliedCompletedRecords() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var model=new BurrowModel();model.show(BurrowModel.View.SESSIONS);
            var panel=new BurrowPanel(model);panel.setSize(225,1250);
            assertTrue(panel.completedSessionsForTest().isEmpty());
            var realFixture=new CompletedSessionHistory.Summary(1,"fixture-login",1000000L,1060000L,
                CompletedSessionHistory.CONFIRMED_LOGOUT,CompletedSessionHistory.FeedState.OBSERVED,
                Map.of("Cooking",1200L),CompletedSessionHistory.FeedState.UNAVAILABLE,Map.of(),
                Map.of(),List.of(),CompletedSessionHistory.FeedState.UNAVAILABLE,List.of(),List.of("No KC provenance"));
            panel.setCompletedSessions(List.of(realFixture),"Fixture: exactly one completed login");
            assertEquals(1,panel.completedSessionsForTest().size());
            int top=panel.historyTopForR13Test();
            int before=panel.historyRowHeightForR13Test(0);
            panel.clickAt(83,top-8+75);
            assertTrue(panel.historyRowHeightForR13Test(0)>before,"click expands actual history row details");
            panel.clickAt(83,top-8+75);
            assertEquals(before,panel.historyRowHeightForR13Test(0),"click collapses same actual row");
            paint(panel,225);paint(panel,242);
        });
    }
    private static void paint(BurrowPanel panel,int width) {
        panel.setSize(width,Math.max(900,panel.getPreferredSize().height));
        var img=new BufferedImage(width,panel.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=img.createGraphics();try {panel.paint(g);}finally {g.dispose();}
    }
}
