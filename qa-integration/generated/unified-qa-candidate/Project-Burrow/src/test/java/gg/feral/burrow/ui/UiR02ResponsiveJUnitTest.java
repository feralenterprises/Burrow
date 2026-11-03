package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Map;

/** UI-R02: synthetic rendering and UI routing only; not native RuneLite QA. */
final class UiR02ResponsiveJUnitTest {
    @SuppressWarnings("unchecked")
    private static Rectangle action(BurrowPanel p,String property,String name) {
        try {
            Field f=BurrowPanel.class.getDeclaredField(property);
            f.setAccessible(true);
            return ((Map<String,Rectangle>)f.get(p)).get(name);
        } catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
    }
    private static void render(BurrowPanel p) {
        Dimension size=p.getPreferredSize();
        assertEquals(225,size.width);
        assertTrue(size.height>=880);
        assertTrue(size.height<40000);
        p.setSize(size);
        BufferedImage image=new BufferedImage(225,size.height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();
        try {p.paint(g);} finally {g.dispose();}
    }
    @Test void largeAndExtraLargeFlowAllViewsAndNavigateByMeasuredBounds() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            for(String size:new String[]{"LARGE","EXTRA_LARGE"}) for(String weight:new String[]{"REGULAR","BOLD"}) {
                var model=new BurrowModel();var p=new BurrowPanel(model);
                p.setAppearance(size,"BRONZE");p.setTextWeight(weight);
                model.show(BurrowModel.View.HOME);
                render(p);
                Rectangle homeButton=action(p,"homeFlowActions","outfitter");
                assertNotNull(homeButton);
                p.clickAt(homeButton.x+10,homeButton.y+10);
                assertEquals(BurrowModel.View.OUTFITTER,model.view());
                model.show(BurrowModel.View.PATHFINDER);
                render(p);
                Rectangle focus=action(p,"pathfinderFlowActions","focus");
                assertNotNull(focus);
                var before=p.pathfinderFocus();
                p.clickAt(focus.x+8,focus.y+8);
                assertTrue(p.pathfinderFocus()!=before);
                render(p);
                model.show(BurrowModel.View.SESSIONS);
                render(p);
            }
        });
    }
    @Test void noDataHomeAndSessionStatusRemainTruthfulOnLargeLayouts() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel();var p=new BurrowPanel(m);
            p.setAppearance("EXTRA_LARGE","BRONZE");p.setTextWeight("BOLD");
            render(p);
            assertTrue(p.homeSignalsForTest().get(0).equals("—"));
            assertTrue(p.homeSignalsForTest().get(1).equals("—"));
            m.show(BurrowModel.View.SESSIONS);render(p);
            assertTrue(p.getPreferredSize().height>=880);
        });
    }
}
