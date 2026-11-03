package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.experience.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** W05 synthetic UI fixtures. No fictitious account facts are shipped with the plugin. */
final class W05CrewHomeJUnitTest {
    private static HomeFeed fixture(boolean hunt) {
        var tip=new SpoonsTip("synthetic-gems","Cut two inventories of gems",null,null,
            List.of("synthetic fixture"),null,null,EvidenceStatus.OBSERVED);
        List<BanditBossCandidate> hunts = hunt ? List.of(new BanditBossCandidate(
            "Scurrius",BanditBossCandidate.Readiness.COMFORTABLE,
            BanditBossCandidate.Reason.GEAR_READY,"synthetic plan","Ranged",List.of(),
            EvidenceStatus.INFERRED_WITH_LIMITS,"",new DestinationIntent(DestinationIntent.Kind.OPEN_OUTFITTER_BOSS,"Scurrius")))
            : List.of();
        return new HomeFeed(18,null,null,Optional.of(tip),EvidenceStatus.OBSERVED,null,
            hunts,new NoodlesOpportunity(null,"Goal","Review your route","synthetic",EvidenceStatus.OBSERVED,
                new DestinationIntent(DestinationIntent.Kind.OPEN_PATHFINDER,"goal")));
    }
    @Test void signedOutKeepsLegacyHomeAndNoCrewActions() throws Exception {
        SwingUtilities.invokeAndWait(()-> {
            var m=new BurrowModel();var p=new BurrowPanel(m);
            int before=p.getPreferredSize().height;
            assertEquals(0,p.crewShiftForTest());
            assertTrue(p.crewLinesForTest().isEmpty());
            p.clickAt(51,419);
            assertEquals(BurrowModel.View.OUTFITTER,m.view());
            m.show(BurrowModel.View.HOME);
            p.setExperienceHomeFeed(null);
            assertEquals(before,p.getPreferredSize().height);
        });
    }
    @Test void crewNeverInventsHuntWhenPlanUnknown() throws Exception {
        SwingUtilities.invokeAndWait(()-> {
            var m=new BurrowModel();var p=new BurrowPanel(m);
            p.setExperienceHomeFeed(fixture(false));
            assertEquals(152,p.crewShiftForTest());
            assertTrue(p.crewLinesForTest().get(1).contains("unknown"));
            p.clickAt(70,209);
            assertEquals(BurrowModel.View.HOME,m.view(),"No hunt means no hidden boss navigation");
            p.setSize(225,p.getPreferredSize().height);
            var img=new BufferedImage(225,p.getHeight(),BufferedImage.TYPE_INT_ARGB);
            var g=img.createGraphics(); try {p.paint(g);} finally {g.dispose();}
            assertEquals(225,p.getPreferredSize().width);
        });
    }
    @Test void crewLinksUseRealViewsAndPreserveExistingHomeHitboxes() throws Exception {
        SwingUtilities.invokeAndWait(()-> {
            var m=new BurrowModel();var p=new BurrowPanel(m);
            p.setExperienceHomeFeed(fixture(true));
            p.clickAt(55,208);
            assertEquals(BurrowModel.View.OUTFITTER,m.view());
            assertEquals("Scurrius",m.boss());
            m.show(BurrowModel.View.HOME);
            p.clickAt(65,244);
            assertEquals(BurrowModel.View.PATHFINDER,m.view());
            m.show(BurrowModel.View.HOME);
            p.clickAt(45,419+152);
            assertEquals(BurrowModel.View.OUTFITTER,m.view(),"legacy Home button must shift with visible crew card");
        });
    }
    @Test void doneAndDismissHaveSeparateSpoonsActionsAndClearVisibleTip() throws Exception {
        SwingUtilities.invokeAndWait(()-> {
            var p=new BurrowPanel(new BurrowModel());
            var seen=new ArrayList<SpoonsTipService.Closed>();
            p.setCrewTipAction((state,how)->seen.add(how));
            p.setExperienceHomeFeed(fixture(false));
            p.clickAt(190,168);
            assertEquals(SpoonsTipService.Closed.MANUAL_DONE,seen.get(0));
            assertEquals("No current tip",p.crewLinesForTest().get(0));
            p.setExperienceHomeFeed(fixture(false));
            p.clickAt(204,168);
            assertEquals(SpoonsTipService.Closed.DISMISSED,seen.get(1));
            assertEquals(2,seen.size());
        });
    }
    @Test void accountRetirementHidesCrewAndRestoresOriginalHomeGeometry() throws Exception {
        SwingUtilities.invokeAndWait(()-> {
            var p=new BurrowPanel(new BurrowModel());
            int old=p.getPreferredSize().height;
            p.setExperienceHomeFeed(fixture(true));
            assertEquals(old+152,p.getPreferredSize().height);
            p.setExperienceHomeFeed(null);
            assertEquals(old,p.getPreferredSize().height);
            assertTrue(p.crewLinesForTest().isEmpty());
        });
    }
}
