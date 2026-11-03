package gg.feral.burrow.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Build 19: wheel handling for the ONE scroll owner of the TrashPanda sidebar.
 *
 * <p>Root cause of the R12-R18 Windows defect (verified against the real RuneLite 1.13.4
 * {@code PluginPanel} bytecode): the default {@code new PluginPanel(){}} wraps itself in
 * RuneLite's own outer JScrollPane (BorderLayout.NORTH, full preferred height). Burrow's inner
 * JScrollPane therefore always received its full content height, so its maximum scroll offset was
 * 0. The old router was attached to {@link BurrowPanel} itself, computed max=0, moved nothing and
 * then CONSUMED the event, so the outer RuneLite scroll pane never received the wheel either.
 *
 * <p>Build 19 mounts Burrow in {@code PluginPanel(false)} (no RuneLite wrapper; see
 * {@link gg.feral.burrow.host.SidebarHost}) and installs this handler only on the single
 * JScrollPane. {@link BurrowPanel} has no wheel listener, so Swing routes wheel events from any
 * painted card, item cell or note to this scroll pane. No game input APIs are used.
 */
public final class SidebarWheelRouter {
    private SidebarWheelRouter() {}
    public static final int PIXELS_PER_NOTCH = 48;

    public static void install(JScrollPane scroll, BurrowPanel panel) {
        if (scroll==null || panel==null)throw new IllegalArgumentException("host required");
        for (MouseWheelListener existing : panel.getMouseWheelListeners()) panel.removeMouseWheelListener(existing);
        // Exactly one wheel owner: this listener, not Swing's default handler as well.
        scroll.setWheelScrollingEnabled(false);
        scroll.addMouseWheelListener(e -> scroll(scroll,panel,e));
    }

    static void scroll(JScrollPane host,BurrowPanel panel,MouseWheelEvent event) {
        if(event.isConsumed())return;
        JViewport viewport=host.getViewport();
        Component view=viewport.getView();
        if(view==null)return;
        int viewportHeight=viewport.getExtentSize().height;
        if(viewportHeight<=0)viewportHeight=viewport.getHeight();
        if(viewportHeight<=0)return;
        int contentHeight=Math.max(view.getHeight(),view.getPreferredSize().height);
        int max=Math.max(0,contentHeight-viewportHeight);
        int current=viewport.getViewPosition().y;
        double rotation=event.getPreciseWheelRotation();
        int pixels;
        if(event.getScrollType()==MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
            pixels=(int)Math.round(rotation*Math.max(40,viewportHeight-40));
        } else {
            // Windows reports getScrollAmount()==3 lines per notch by default; normalise to a
            // fixed pixel step so a single notch always moves visibly but never skips a card.
            pixels=(int)Math.round(rotation*PIXELS_PER_NOTCH);
        }
        if(pixels==0 && rotation!=0)pixels=rotation>0?1:-1;
        int next=(int)Math.max(0,Math.min((long)max,(long)current+pixels));
        if(next!=current) {
            viewport.setViewPosition(new Point(viewport.getViewPosition().x,next));
            panel.onViewportWheelScrolled();
        }
        // The sidebar is not the game canvas; consuming here only stops Swing's own default
        // scroll handler from also moving the viewport (double scroll).
        event.consume();
    }
}
