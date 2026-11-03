package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.pathfinder.SuggestionEngine;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Build 20 panel controls at 225 px: dropdown menus, segmented attention, minutes. Synthetic snapshot. */
final class Build20PathfinderPanelJUnitTest {
    @Test void controlsAreWiredAndDropdownsOfferEveryChoice() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel(); var p=new BurrowPanel(m); p.setSize(225,880);
            p.clickAt(90,94);
            assertEquals(BurrowModel.View.PATHFINDER,m.view());
            p.setPathfinderSnapshot(Build20PathfinderControlsJUnitTestAccess.account());
            p.clickPathfinderActionForTest("attention");p.clickPathfinderActionForTest("attention"); // ACTIVE -> LOW -> AFK
            assertEquals(SuggestionEngine.Attention.AFK,p.pathfinderAttention());
            assertTrue(p.pathfinderSuggestions().stream().allMatch(s->s.afkSeconds()>=60));
            p.clickPathfinderActionForTest("attention"); // AFK -> ACTIVE
            assertEquals(SuggestionEngine.Attention.ACTIVE,p.pathfinderAttention());
            var focus=p.pathfinderFocusMenu();
            assertEquals(6,focus.getComponentCount(),"ALL/Quests/PvM/GP/Supplies/AFK");
            ((JMenuItem)focus.getComponent(5)).doClick();
            assertEquals(SuggestionEngine.Focus.AFK,p.pathfinderFocus());
            var mins=p.pathfinderMinutesMenu();
            assertEquals(6,mins.getComponentCount());
            ((JMenuItem)mins.getComponent(5)).doClick();
            assertEquals(120,p.pathfinderMinutes());
            var img=new BufferedImage(225,880,BufferedImage.TYPE_INT_ARGB);
            p.paint(img.getGraphics());
            assertNotEquals(0,img.getRGB(20,440),"controls painted");
        });
    }
}
