package gg.feral.burrow.host;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.ui.BurrowPanel;
import gg.feral.burrow.ui.SidebarWheelRouter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import net.runelite.client.ui.PluginPanel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Build 19 mouse-wheel host test against the REAL RuneLite 1.13.4 {@link PluginPanel} class.
 * The component mounted is {@code getWrappedPanel()}, which is exactly what RuneLite's ClientUI
 * adds to its sidebar (verified from ClientUI bytecode).
 *
 * <p>Headless Swing has no native peer, so AWT's LightweightDispatcher step "deliver the wheel to
 * the deepest component under the cursor that has wheel listeners, else its nearest ancestor that
 * does" is performed explicitly by {@link #routeWheel}. The event starts at sidebar-root pixel
 * coordinates, exactly as a real mouse would.
 */
final class SidebarHostWheelJUnitTest {
    static void layoutTree(Component c) {
        if (c instanceof Container k) {
            k.doLayout();
            for (Component child : k.getComponents()) layoutTree(child);
        }
    }
    static List<JScrollPane> scrollPanes(Component c, List<JScrollPane> out) {
        if (c instanceof JScrollPane s) out.add(s);
        if (c instanceof Container k) for (Component child : k.getComponents()) scrollPanes(child, out);
        return out;
    }
    /** Mount at the real RuneLite sidebar width (PluginPanel's own preferred width). */
    static Container mountSidebar(PluginPanel host, int height) {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.add(host.getWrappedPanel(), BorderLayout.CENTER);
        sidebar.setSize(host.getWrappedPanel().getPreferredSize().width, height);
        layoutTree(sidebar);
        return sidebar;
    }
    /** AWT-equivalent wheel routing from a point in sidebar coordinates. Returns the receiver. */
    static Component routeWheel(Container root, int x, int y, int notches) {
        Component deepest = SwingUtilities.getDeepestComponentAt(root, x, y);
        assertNotNull(deepest, "something under the cursor at " + x + "," + y);
        Component target = deepest;
        while (target != null && target.getMouseWheelListeners().length == 0) target = target.getParent();
        assertNotNull(target, "a wheel owner exists above " + deepest.getClass().getSimpleName());
        Point p = SwingUtilities.convertPoint(root, x, y, target);
        MouseWheelEvent e = new MouseWheelEvent(target, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0,
            p.x, p.y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, notches);
        target.dispatchEvent(e);
        return target;
    }

    @Test void build19HasExactlyOneScrollOwnerAndWheelMovesBothWaysOverEveryRegion() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BurrowModel model = new BurrowModel();
            model.show(BurrowModel.View.OUTFITTER);
            BurrowPanel panel = new BurrowPanel(model);
            SidebarHost host = SidebarHost.mount(panel);
            assertSame(host, host.getWrappedPanel(), "PluginPanel(false): RuneLite adds no outer wrapper");
            assertEquals(PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH, host.getPreferredSize().width);
            Container sidebar = mountSidebar(host, 420); // short RuneLite window
            List<JScrollPane> owners = scrollPanes(sidebar, new ArrayList<>());
            assertEquals(1, owners.size(), "exactly one JScrollPane in the mounted sidebar");
            JScrollPane scroll = owners.get(0);
            assertSame(host.scrollOwner(), scroll);
            assertEquals(0, panel.getMouseWheelListeners().length, "content has no swallowing wheel listener");
            JViewport viewport = scroll.getViewport();
            assertTrue(viewport.getHeight() <= 420, "viewport bounded by sidebar, was " + viewport.getHeight());
            // R13 intentionally moved the long preparation notes to Boss Chronicle; the
            // default Outfitter is shorter, but must still require genuine host scrolling.
            assertTrue(panel.getPreferredSize().height > viewport.getHeight() + 450,
                "R13 Outfitter must scroll well beyond the short sidebar");
            assertTrue(viewport.getWidth() >= BurrowPanel.WIDTH,
                "225px content fits beside scrollbar, viewport=" + viewport.getWidth());
            String boss = model.boss(), method = model.method();
            // R13 cursor regions: equipment, inventory, and compact Rare Drops near
            // the new end of the page. The original 1100px notes target was moved to
            // Chronicle and falls outside the compact default Outfitter.
            int[] contentY = {
                panel.equipmentYForR13Test() + 25,
                panel.equipmentYForR13Test() + 281 + 50,
                panel.getPreferredSize().height - 90
            };
            int maxScroll = panel.getPreferredSize().height - viewport.getExtentSize().height;
            assertTrue(maxScroll > SidebarWheelRouter.PIXELS_PER_NOTCH,
                "R13 page has room to test one more wheel step");
            for (int target : contentY) {
                // Keep the region visible AND leave room for one downward wheel step.
                // The bottom Rare Drops target is only 90px from the page end;
                // target-200 was beyond maxScroll and Swing silently clamped it,
                // making the old > before assertion impossible even with working scrolling.
                int start = Math.min(Math.max(0, target - 200),
                    maxScroll - SidebarWheelRouter.PIXELS_PER_NOTCH);
                viewport.setViewPosition(new Point(0, start));
                int before = viewport.getViewPosition().y;
                int screenY = target - before; // cursor coordinate within the visible sidebar
                assertTrue(screenY >= 0 && screenY < viewport.getExtentSize().height,
                    "target " + target + " is visible at scroll " + before +
                    " (screen y=" + screenY + ")");
                Component receiver = routeWheel(sidebar, 100, screenY, 2);
                assertSame(scroll, receiver, "the single scroll owner receives the wheel");
                int after = viewport.getViewPosition().y;
                assertTrue(after > before, "wheel down over content y=" + target + " moved " + before + "->" + after);
                routeWheel(sidebar, 100, screenY, -1);
                assertTrue(viewport.getViewPosition().y < after, "wheel up over content y=" + target);
            }
            for (int i = 0; i < 200; i++) routeWheel(sidebar, 100, 100, 3);
            int max = panel.getPreferredSize().height - viewport.getExtentSize().height;
            assertEquals(max, viewport.getViewPosition().y, "every card reachable by wheel");
            JScrollBar bar = scroll.getVerticalScrollBar();
            assertEquals(bar.getMaximum() - bar.getVisibleAmount(), bar.getValue(), "scrollbar tracks same viewport");
            for (int i = 0; i < 200; i++) routeWheel(sidebar, 100, 100, -3);
            assertEquals(0, viewport.getViewPosition().y, "back to top");
            assertEquals(boss, model.boss(), "wheel never changes boss");
            assertEquals(method, model.method(), "wheel never changes method");
            bar.setValue(300);
            assertEquals(300, viewport.getViewPosition().y, "scrollbar drag moves the same viewport");
        });
    }

    @Test void everyViewScrollsAtShortHeight() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (BurrowModel.View view : BurrowModel.View.values()) {
                BurrowModel model = new BurrowModel();
                model.show(view);
                BurrowPanel panel = new BurrowPanel(model);
                SidebarHost host = SidebarHost.mount(panel);
                Container sidebar = mountSidebar(host, 360);
                routeWheel(sidebar, 100, 200, 3);
                assertTrue(host.scrollOwner().getViewport().getViewPosition().y > 0, view + " scrolls");
            }
        });
    }

    @Test void clicksStillHitTheSameControlsAfterScrolling() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BurrowModel model = new BurrowModel();
            BurrowPanel panel = new BurrowPanel(model);
            SidebarHost host = SidebarHost.mount(panel);
            mountSidebar(host, 400);
            // Navigation tab hitbox lives in content coordinates; scrolling must not shift it.
            host.scrollOwner().getViewport().setViewPosition(new Point(0, 40));
            panel.clickAt(140, 94); // Outfitter tab, content coordinates
            assertEquals(BurrowModel.View.OUTFITTER, model.view());
        });
    }

    /** Reproduces the Build 12-18 defect with the same real PluginPanel class, so this suite
     *  would have caught it: nested inner scroll gets full height => wheel cannot move anything. */
    @Test void build18NestedArrangementIsProvablyBroken() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BurrowModel model = new BurrowModel();
            model.show(BurrowModel.View.OUTFITTER);
            BurrowPanel panel = new BurrowPanel(model);
            PluginPanel legacy = new PluginPanel() { };
            legacy.setLayout(new BorderLayout());
            JScrollPane inner = new JScrollPane(panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            legacy.add(inner, BorderLayout.CENTER);
            // Build 18 router behaviour: listener on the content that clamps to the inner max and consumes.
            inner.setWheelScrollingEnabled(false);
            panel.addMouseWheelListener(e -> {
                JViewport v = inner.getViewport();
                int max = Math.max(0, Math.max(panel.getHeight(), panel.getPreferredSize().height) - v.getExtentSize().height);
                v.setViewPosition(new Point(0, Math.max(0, Math.min(max, v.getViewPosition().y + 60))));
                e.consume();
            });
            Container sidebar = mountSidebar(legacy, 420);
            List<JScrollPane> panes = scrollPanes(sidebar, new ArrayList<>());
            assertEquals(2, panes.size(), "two nested scroll panes (RuneLite wrapper + Burrow inner)");
            assertTrue(inner.getViewport().getHeight() >= panel.getPreferredSize().height - 2,
                "inner viewport is full content height, so inner max scroll is 0");
            JScrollPane outer = panes.get(0) == inner ? panes.get(1) : panes.get(0);
            int outerBefore = outer.getViewport().getViewPosition().y;
            Component receiver = routeWheel(sidebar, 100, 300, 3);
            assertSame(panel, receiver, "content listener intercepts first");
            assertEquals(0, inner.getViewport().getViewPosition().y, "inner did not move");
            assertEquals(outerBefore, outer.getViewport().getViewPosition().y, "outer never saw it: owner's Windows symptom");
        });
    }

    @Test void routerStepIsVisibleButBounded() {
        assertTrue(SidebarWheelRouter.PIXELS_PER_NOTCH >= 24 && SidebarWheelRouter.PIXELS_PER_NOTCH <= 96);
    }
}
