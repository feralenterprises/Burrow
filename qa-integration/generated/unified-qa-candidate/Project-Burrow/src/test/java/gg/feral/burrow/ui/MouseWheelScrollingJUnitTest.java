package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Actual wheel-event delivery through the 225px Swing canvas, not merely
 * programmatically setting JScrollBar values. */
public class MouseWheelScrollingJUnitTest {
    private static MouseWheelEvent wheel(BurrowPanel panel,int rotation) {
        return new MouseWheelEvent(panel, MouseEvent.MOUSE_WHEEL,
            System.currentTimeMillis(), 0, 95, 290, 0, false,
            MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, rotation);
    }
    /** Build 19: BurrowPanel deliberately has no wheel listener. Real AWT delivers a wheel over
     *  the content to the nearest ancestor that listens (the single JScrollPane owner); headless
     *  Swing has no peer to do that, so perform the same routing explicitly. */
    private static void deliver(java.awt.Component content, MouseWheelEvent e) {
        java.awt.Component target=content;
        while(target!=null && target.getMouseWheelListeners().length==0)target=target.getParent();
        assertNotNull(target,"wheel owner above content");
        java.awt.Point p=javax.swing.SwingUtilities.convertPoint(content,e.getX(),e.getY(),target);
        MouseWheelEvent routed=new MouseWheelEvent(target,e.getID(),e.getWhen(),e.getModifiersEx(),p.x,p.y,
            0,false,e.getScrollType(),e.getScrollAmount(),e.getWheelRotation());
        target.dispatchEvent(routed);
        if(routed.isConsumed())e.consume();
    }
    @Test public void wheelScrollsOutfitterAndHomeWithoutChangingSelections() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            BurrowModel model=new BurrowModel();
            model.show(BurrowModel.View.OUTFITTER);
            BurrowPanel panel=new BurrowPanel(model);
            JScrollPane scroll=new JScrollPane(panel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getVerticalScrollBar().setUnitIncrement(20);
            SidebarWheelRouter.install(scroll,panel);
            scroll.setSize(225,400);
            scroll.doLayout();
            panel.setSize(panel.getPreferredSize());
            assertTrue(scroll.getVerticalScrollBar().isVisible(),"Outfitter scrollbar present");
            String boss=model.boss(), method=model.method();
            MouseWheelEvent down=wheel(panel,3);
            deliver(panel,down);
            assertTrue(down.isConsumed(),"Wheel input handled inside sidebar");
            int lower=scroll.getViewport().getViewPosition().y;
            assertTrue(lower>0,"Wheel must scroll content down, not only drag scrollbar");
            assertEquals(boss,model.boss(),"Wheel cannot change selected boss");
            assertEquals(method,model.method(),"Wheel cannot change combat method");
            MouseWheelEvent up=wheel(panel,-2);
            deliver(panel,up);
            assertTrue(up.isConsumed());
            assertTrue(scroll.getViewport().getViewPosition().y<lower,"Wheel scrolls back up");
            // RuneLite frequently dispatches the wheel to the viewport or
            // enclosing scroll pane rather than directly to painted content.
            scroll.getViewport().setViewPosition(new java.awt.Point(0,0));
            MouseWheelEvent hostWheel=new MouseWheelEvent(scroll,MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),0,95,290,0,false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,3,2);
            scroll.dispatchEvent(hostWheel);
            assertTrue(hostWheel.isConsumed(),"Host container receives wheel");
            assertTrue(scroll.getViewport().getViewPosition().y>0,"Host wheel moves actual viewport");
            model.show(BurrowModel.View.HOME);
            panel.revalidate();scroll.doLayout();
            scroll.getVerticalScrollBar().setValue(0);
            deliver(panel,wheel(panel,1));
            assertTrue(scroll.getViewport().getViewPosition().y>0,"Home must also wheel-scroll");
        });
    }
}
