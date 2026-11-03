package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.host.SidebarHost;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.EnumSet;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Offline contracts for the unified QA candidate UI shell wiring. Not native RuneLite proof. */
public final class R10cUnifiedUiContractJUnitTest {
    @Test
    void sidebarWidthIsApproved225() {
        assertEquals(225, BurrowPanel.WIDTH);
    }

    @Test
    void exactlyFourRootViews() {
        assertEquals(EnumSet.of(
            BurrowModel.View.HOME,
            BurrowModel.View.PATHFINDER,
            BurrowModel.View.OUTFITTER,
            BurrowModel.View.SESSIONS), EnumSet.allOf(BurrowModel.View.class));
    }

    @Test
    void approvedArmoredRaccoonResourcePresent() {
        BufferedImage approved = Sprites.load("approved-armored-raccoon.png");
        assertNotNull(approved, "R10C approved-armored-raccoon.png must ship in plugin resources");
        assertTrue(approved.getWidth() > 0 && approved.getHeight() > 0);
    }

    @Test
    void outfitterInventoryCoversTwentyEightSlots() throws Exception {
        BurrowPanel panel = new BurrowPanel(new BurrowModel());
        Method inventoryItem = BurrowPanel.class.getDeclaredMethod("inventoryItem", int.class);
        inventoryItem.setAccessible(true);
        for (int i = 0; i < 28; i++) {
            final int index = i;
            assertDoesNotThrow(() -> inventoryItem.invoke(panel, index),
                "OSRS 4x7 inventory requires slots 0..27 inclusive");
        }
    }

    @Test
    void sidebarHostKeepsBankMemorySubordinate() throws Exception {
        final javax.swing.JComponent[] bank = new javax.swing.JComponent[1];
        final javax.swing.JComponent[] capture = new javax.swing.JComponent[1];
        final javax.swing.JComponent[] back = new javax.swing.JComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            BurrowPanel panel = new BurrowPanel(new BurrowModel());
            SidebarHost host = SidebarHost.mount(panel);
            bank[0] = findNamed(host, "OutfitterBankMemory");
            capture[0] = findNamed(host, "FirstRunCapture");
            back[0] = findNamed(host, "BackToFourTabs");
        });
        assertNotNull(bank[0]);
        assertNotNull(capture[0]);
        assertNotNull(back[0]);
    }

    private static javax.swing.JComponent findNamed(java.awt.Container root, String name) {
        java.util.ArrayDeque<java.awt.Container> q = new java.util.ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            java.awt.Container c = q.removeFirst();
            for (java.awt.Component child : c.getComponents()) {
                if (name.equals(child.getName()) && child instanceof javax.swing.JComponent jc) {
                    return jc;
                }
                if (child instanceof java.awt.Container nested) q.add(nested);
            }
        }
        fail("Missing named control: " + name);
        return null;
    }
}
