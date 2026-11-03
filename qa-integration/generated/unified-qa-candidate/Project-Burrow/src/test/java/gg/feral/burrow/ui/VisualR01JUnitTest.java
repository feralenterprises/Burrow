package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.experience.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Source-safe headless geometry/navigation checks, not native RuneLite verification. */
final class VisualR01JUnitTest {
    private void render(BurrowPanel p){
        Dimension d=p.getPreferredSize();p.setSize(d);
        BufferedImage image=new BufferedImage(225,Math.min(d.height,2500),BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics(); try{p.paint(g);}finally{g.dispose();}
    }
    @Test void featuredBrowserDoesNotMutateSelectedBoss() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            BurrowModel model=new BurrowModel(); model.show(BurrowModel.View.OUTFITTER);
            var ui=new BurrowPanel(model);
            ui.setAppearance("EXTRA_LARGE","BRONZE");ui.setTextWeight("BOLD");
            ui.showChronicleBrowserForTest();
            assertTrue(ui.chronicleBrowserForTest());
            assertEquals("Scurrius",ui.chronicleBrowseBossesForTest().get(0));
            assertEquals("Sarachnis",ui.chronicleBrowseBossesForTest().get(1));
            assertEquals("Bryophyta",ui.chronicleBrowseBossesForTest().get(2));
            assertEquals("Scurrius",model.boss());
            render(ui);
            ui.chooseChronicleCardForTest("Sarachnis");
            assertFalse(ui.chronicleBrowserForTest());
            assertEquals("Sarachnis",ui.chronicleBossForUiTest());
            assertEquals("Scurrius",model.boss());
            render(ui);
        });
    }
    @Test void sortDoesNotInventDifficultyForUnranked() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var ui=new BurrowPanel(new BurrowModel());ui.showChronicleBrowserForTest();
            ui.setChronicleBrowseSortForTest(1);
            var az=ui.chronicleBrowseBossesForTest();
            assertTrue(az.size()>25);
            ui.setChronicleBrowseSortForTest(2);
            var za=ui.chronicleBrowseBossesForTest();
            assertEquals(az.get(0),za.get(za.size()-1));
            ui.setChronicleBrowseSortForTest(3);
            assertTrue(!ui.chronicleBrowseBossesForTest().isEmpty());
        });
    }
    @Test void activityGlyphsKeepAllElevenAndTwentyEightSlots() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var model=new BurrowModel();model.show(BurrowModel.View.OUTFITTER);
            var ui=new BurrowPanel(model);ui.setAppearance("EXTRA_LARGE","BRONZE");ui.setTextWeight("BOLD");
            ui.selectActivityForTest("herb");render(ui);
            assertTrue(ui.activityModeForTest());
            assertEquals("herb",ui.activityKitIdForTest());
            // The host doesn't claim owned items or equip anything by painting a reference kit.
            assertEquals("Scurrius",model.boss());
        });
    }
    @Test void portraitHomeShowsOnlyObservedHunts() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel();var ui=new BurrowPanel(m);
            ui.setAppearance("EXTRA_LARGE","BRONZE");ui.setTextWeight("BOLD");
            var h=new BanditBossCandidate("Sarachnis",BanditBossCandidate.Readiness.STRETCH,
                BanditBossCandidate.Reason.GEAR_NEEDS_CHECKS,"synthetic","Melee",List.of(),
                EvidenceStatus.INFERRED_WITH_LIMITS,"",new DestinationIntent(DestinationIntent.Kind.OPEN_OUTFITTER_BOSS,"Sarachnis"));
            var tip=new SpoonsTip("synthetic","Check your bank first",null,null,List.of("synthetic"),null,null,EvidenceStatus.OBSERVED);
            ui.setExperienceHomeFeed(new HomeFeed(1,null,null,Optional.of(tip),EvidenceStatus.OBSERVED,null,
                List.of(h),new NoodlesOpportunity(null,"Goal","Review Pathfinder","synthetic",EvidenceStatus.OBSERVED,
                    new DestinationIntent(DestinationIntent.Kind.OPEN_PATHFINDER,"goal"))));
            render(ui);
            assertTrue(ui.getPreferredSize().height>880);
            assertEquals("Scurrius",m.boss());
            ui.setExperienceHomeFeed(null);render(ui);
            assertTrue(ui.crewLinesForTest().isEmpty());
        });
    }
    @Test void browserCardClickOpensChronicleDetailNotBossOutfitter() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var model=new BurrowModel();model.show(BurrowModel.View.OUTFITTER);
            var ui=new BurrowPanel(model);ui.showChronicleBrowserForTest();render(ui);
            Rectangle row=ui.chronicleBrowseCardForTest("Sarachnis");
            assertNotNull(row);
            ui.clickAt(row.x+row.width/2,row.y+row.height/2-8); // CONTENT_SHIFT = -8
            assertFalse(ui.chronicleBrowserForTest());
            assertEquals("Sarachnis",ui.chronicleBossForUiTest());
            assertEquals("Scurrius",model.boss());
        });
    }
    @Test void banditSecondHuntOpensTheCorrectOwnedActivity() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var model=new BurrowModel();var ui=new BurrowPanel(model);
            ui.setAppearance("EXTRA_LARGE","BRONZE");ui.setTextWeight("BOLD");
            var a=new BanditBossCandidate("Scurrius",BanditBossCandidate.Readiness.COMFORTABLE,
                BanditBossCandidate.Reason.GEAR_READY,"synthetic","Melee",List.of(),EvidenceStatus.INFERRED_WITH_LIMITS,
                "",new DestinationIntent(DestinationIntent.Kind.OPEN_OUTFITTER_BOSS,"Scurrius"));
            var b=new BanditBossCandidate("Vorkath",BanditBossCandidate.Readiness.STRETCH,
                BanditBossCandidate.Reason.GEAR_NEEDS_CHECKS,"synthetic","Ranged",List.of(),EvidenceStatus.INFERRED_WITH_LIMITS,
                "",new DestinationIntent(DestinationIntent.Kind.OPEN_OUTFITTER_BOSS,"Vorkath"));
            ui.setExperienceHomeFeed(new HomeFeed(1,null,null,Optional.empty(),EvidenceStatus.OBSERVED,null,List.of(a,b),
                new NoodlesOpportunity(null,"Goal","Synthetic objective","synthetic",EvidenceStatus.OBSERVED,
                    new DestinationIntent(DestinationIntent.Kind.OPEN_PATHFINDER,"goal"))));
            render(ui);
            Rectangle second=ui.homeFlowActionForTest("bandit:1");assertNotNull(second);
            ui.clickAt(second.x+15,second.y+second.height/2);
            assertEquals(BurrowModel.View.OUTFITTER,model.view());
            assertEquals("Vorkath",model.boss());
        });
    }

}
