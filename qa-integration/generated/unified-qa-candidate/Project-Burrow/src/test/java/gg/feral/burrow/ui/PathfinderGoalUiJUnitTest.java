package gg.feral.burrow.ui;

import gg.feral.burrow.core.AccountModePreference;
import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.pathfinder.SuggestionEngine;
import gg.feral.burrow.pathfinder.SuggestionEngine.*;
import java.util.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Build 19: Pathfinder detail, goal buttons and list rows through actual 225px hitboxes (synthetic data). */
final class PathfinderGoalUiJUnitTest {
    record Call(long hash,String id,GoalMark mark){}
    @Test void selectExplainSaveSkipAndListRows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var m=new BurrowModel(); var p=new BurrowPanel(m); p.setSize(225,2000);
            List<Call> calls=new ArrayList<>();
            p.setPathfinderGoalAction((h,i,k)->calls.add(new Call(h,i,k)));
            p.setAccountModeState(42L,AccountModePreference.Mode.IRONMAN);
            p.clickAt(90,94);
            assertEquals(BurrowModel.View.PATHFINDER,m.view());
            var snap=new Snapshot(true,Map.of("Slayer",75),Map.of("Song of the Elves",Quest.FINISHED,"Dragon Slayer II",Quest.NOT_STARTED),
                BankStatus.UNAVAILABLE,Set.of());
            p.setPathfinderSnapshot(snap);
            assertTrue(p.pathfinderSuggestions().size()>10);
            assertNull(p.pathfinderActionRectForTest("save"),"no buttons until a step is selected");
            p.clickPathfinderActionForTest("inspect:0"); // card body, not checkbox
            String first=p.pathfinderSelectedIdForTest();
            assertNotNull(first);
            String detail=p.pathfinderDetailTextForTest();
            assertTrue(detail.contains("Why:") && detail.contains("Source:"),detail);
            int by=p.pathfinderActionRectForTest("save").y;
            assertTrue(by>850);
            p.clickPathfinderActionForTest("save"); // explicit alternate button
            assertEquals(new Call(42L,first,GoalMark.SAVED),calls.get(0));
            p.clickPathfinderActionForTest("skip"); // SKIP
            assertEquals(GoalMark.SKIPPED,calls.get(1).mark());
            p.clickPathfinderActionForTest("done"); // DONE (owner override)
            assertEquals(GoalMark.DONE,calls.get(2).mark());
            // A list row below the top three becomes the selected step.
            p.clickPathfinderActionForTest("inspect:3");
            assertEquals(p.pathfinderSuggestions().get(3).id(),p.pathfinderSelectedIdForTest());
            // Account B logged out: suggestions cleared; goal clicks without an authenticated account do nothing.
            p.setPathfinderSnapshot(null);
            assertTrue(p.pathfinderSuggestions().isEmpty());
            p.setAccountModeState(-1L,null);
            int n=calls.size();
            p.clickAt(30,by+10-8);
            assertEquals(n,calls.size());
            assertTrue(p.getPreferredSize().height>=880);
        });
    }
}
