package gg.feral.burrow.ui;

import gg.feral.burrow.core.AccountModePreference.Mode;
import gg.feral.burrow.core.BurrowModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javax.swing.SwingUtilities;
import java.util.HashMap;
import java.util.Map;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

/** Isolated R04 UI routing and presentation tests; no fabricated account events. */
final class R04HomeChronicleAppearanceJUnitTest {
    @Test void homeChronicleBackRestoresHomeAndSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var model=new BurrowModel();
            var panel=new BurrowPanel(model);panel.setSize(225,1500);
            String boss=model.boss();var method=model.method();var tier=model.tier();
            panel.clickAt(50,490);
            assertEquals(BurrowModel.View.OUTFITTER,model.view());
            assertTrue(panel.isChronicleOpenForR13Test());
            assertEquals(boss,model.boss());
            paint(panel);
            panel.clickAt(50,130-8+12);
            assertFalse(panel.isChronicleOpenForR13Test());
            assertEquals(BurrowModel.View.HOME,model.view());
            assertEquals(boss,model.boss());assertEquals(method,model.method());assertEquals(tier,model.tier());
            panel.clickAt(40,419);
            assertEquals(BurrowModel.View.OUTFITTER,model.view());
            panel.clickAt(170,133-8+15);
            assertTrue(panel.isChronicleOpenForR13Test());
            panel.clickAt(50,130-8+12);
            assertEquals(BurrowModel.View.OUTFITTER,model.view());
            panel.clickAt(35,94);
            assertEquals(BurrowModel.View.HOME,model.view());
            panel.clickAt(35,455);
            assertEquals(BurrowModel.View.SESSIONS,model.view());
        });
    }
    @Test void settingsClicksStayInsideBoxesAndConfigRoundtrip() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            Map<String,String> saved=new HashMap<>();
            var p=new BurrowPanel(new BurrowModel());p.setSize(225,900);
            p.setAccountModeState(123L,Mode.IRONMAN);
            p.setAppearanceAction((font,color)->{saved.put("font",font);saved.put("color",color);});
            p.clickAt(204,56);assertTrue(p.isSettingsOpen());
            p.clickAt(55,568);assertEquals("Large",p.uiScaleLabel());
            p.clickAt(153,716);assertEquals("Blue",p.accentThemeLabel());
            assertEquals("LARGE",saved.get("font")); assertEquals("BLUE",saved.get("color"));
            p.clickAt(110,550);assertEquals("Large",p.uiScaleLabel(),"gap cannot alter selected font");
            p.clickAt(110,721);assertEquals("Blue",p.accentThemeLabel(),"gap cannot alter selected accent");
            paint(p);
            p.clickAt(45,442);assertFalse(p.isSettingsOpen());
            var another=new BurrowPanel(new BurrowModel());
            another.setAppearance(saved.get("font"),saved.get("color"));
            assertEquals("Large",another.uiScaleLabel());assertEquals("Blue",another.accentThemeLabel());
            another.setAppearance("INVALID", "INVALID");
            assertEquals("Normal",another.uiScaleLabel());assertEquals("Bronze",another.accentThemeLabel());
        });
    }
    @Test void homeChronicleRestoresScrollInNativeSidebarViewport() throws Exception {
        final javax.swing.JScrollPane[] holder=new javax.swing.JScrollPane[1];
        final BurrowPanel[] panels=new BurrowPanel[1];
        SwingUtilities.invokeAndWait(()->{
            var model=new BurrowModel();var panel=new BurrowPanel(model);
            var scroll=new javax.swing.JScrollPane(panel,
                javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setSize(242,380);scroll.doLayout();
            scroll.getViewport().setViewPosition(new java.awt.Point(0,180));
            assertEquals(180,scroll.getViewport().getViewPosition().y);
            panel.clickAt(40,490);
            holder[0]=scroll;panels[0]=panel;
        });
        SwingUtilities.invokeAndWait(()->{
            assertEquals(0,holder[0].getViewport().getViewPosition().y,
                "opening Chronicle resets the visible scroll position");
            panels[0].clickAt(50,130-8+12);
        });
        SwingUtilities.invokeAndWait(()->{
            assertEquals(180,holder[0].getViewport().getViewPosition().y,
                "Chronicle back returns to original Home scroll position");
        });
    }
    @Test void allFourViewsRenderAtNativeWidthWithLargeFont() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var model=new BurrowModel();var p=new BurrowPanel(model);
            p.setAppearance("LARGE","CRIMSON");
            for(var v:BurrowModel.View.values()){
                model.show(v);
                p.setSize(225,p.getPreferredSize().height);
                paint(p);
                assertEquals(225,p.getPreferredSize().width);
            }
        });
    }
    private static void paint(BurrowPanel p){
        p.setSize(225,Math.max(900,p.getPreferredSize().height));
        BufferedImage b=new BufferedImage(225,p.getHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=b.createGraphics();try{p.paint(g);}finally{g.dispose();}
    }
}
