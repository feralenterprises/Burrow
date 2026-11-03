package gg.feral.burrow.ui;
import gg.feral.burrow.core.*;
import gg.feral.burrow.core.AccountModePreference.Mode;
import gg.feral.burrow.pathfinder.SuggestionEngine;
import java.awt.*;
import java.util.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** PF-R04 isolated native Swing interaction geometry, using synthetic account data only. */
public final class PathfinderR04GoalUiJUnitTest {
    @Test void checkboxAndInspectAreIndependentAt225ExtraLargeBold() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel();m.show(BurrowModel.View.PATHFINDER);
            var p=new BurrowPanel(m);p.setAppearance("EXTRA_LARGE","BRONZE");p.setTextWeight("BOLD");
            p.setAccountModeState(19L,Mode.IRONMAN);
            var snap=new SuggestionEngine.Snapshot(true,Map.of("Slayer",85),
                Map.of("Dragon Slayer II",SuggestionEngine.Quest.FINISHED,"Fairytale II - Cure a Queen",SuggestionEngine.Quest.IN_PROGRESS),
                SuggestionEngine.BankStatus.UNAVAILABLE,Set.of());
            p.setPathfinderSnapshot(snap);
            assertTrue(!p.pathfinderSuggestions().isEmpty());
            var calls=new java.util.ArrayList<String>();
            p.setPathfinderGoalAction((hash,id,mark)->calls.add(hash+":"+id+":"+mark));
            p.setSize(p.getPreferredSize()); // renders on graphics for coordinate boxes
            java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(225,p.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
            var g=image.createGraphics();try{p.paint(g);}finally{g.dispose();}
            var box=p.pathfinderCheckboxRectForTest(0);var card=p.pathfinderInspectRectForTest(0);
            assertNotNull(box);assertNotNull(card);assertTrue(card.contains(box));
            assertEquals(null,p.pathfinderSelectedIdForTest());
            p.clickAt(box.x+8,box.y+8-8); // CONTENT_SHIFT is -8, sourceY = clickY+8
            assertEquals(1,calls.size());
            assertTrue(calls.get(0).contains(p.pathfinderSuggestions().get(0).id()+":SAVED"));
            assertEquals(null,p.pathfinderSelectedIdForTest(),"checkbox must never open details");
            p.clickAt(100,card.y+12-8);
            assertEquals(1,calls.size(),"inspecting must not alter saved goals");
            assertEquals(p.pathfinderSuggestions().get(0).id(),p.pathfinderSelectedIdForTest());
            p.setPathfinderSnapshot(null);
            assertTrue(p.pathfinderSuggestions().isEmpty());
        });
    }
}
