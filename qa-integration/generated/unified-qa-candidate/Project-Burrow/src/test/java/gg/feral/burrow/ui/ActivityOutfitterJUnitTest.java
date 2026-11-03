package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** ACT-R01: tests generic reference kits, navigation isolation and 225px render sizes. */
final class ActivityOutfitterJUnitTest {
    @Test void eightSourceGroundedGenericKitsNeverClaimBankOwnership() {
        var kits=ActivityOutfitterCatalog.all();
        assertEquals(20,kits.size());
        assertEquals(20,kits.stream().map(ActivityOutfitterCatalog.Kit::id).distinct().count());
        assertEquals(List.of("herb","trees","fruit-trees","birdhouse","clue","slayer-melee","slayer-magic","slayer-ranged"),
            kits.subList(0,8).stream().map(ActivityOutfitterCatalog.Kit::id).toList());
        for(var kit:kits){
            assertEquals(11,kit.equipment().size());
            assertTrue(kit.inventory().size()<=28 && kit.inventory().size()>0);
            assertTrue(kit.sourceUrl().startsWith("https://oldschool.runescape.wiki/w/"));
            assertFalse(kit.notes().isEmpty());
        }
        assertTrue(kits.get(6).notes().stream().anyMatch(x->x.contains("spellbook")));
        assertTrue(kits.get(7).equipment().get(1).label().contains("Ava"));
    }
    @Test void treeFruitAndBirdhouseKitsAreSeparateAndRouteConditional() {
        var tree=ActivityOutfitterCatalog.find("trees");
        var fruit=ActivityOutfitterCatalog.find("fruit-trees");
        var birdhouse=ActivityOutfitterCatalog.find("birdhouse");
        assertFalse(tree.inventory().stream().anyMatch(x->x.label().contains("Fruit tree")));
        assertTrue(fruit.inventory().stream().anyMatch(x->x.label().contains("Calquat") && x.conditional()));
        assertEquals(4,birdhouse.inventory().stream().filter(x->x.label().startsWith("Replacement birdhouse #")).count());
        assertEquals("HUNTER",birdhouse.category());
        assertEquals("https://oldschool.runescape.wiki/w/Treasure_Trails/Guide", ActivityOutfitterCatalog.find("clue").sourceUrl());
        for(var kit:List.of(tree,fruit,birdhouse))
            assertTrue(kit.notes().stream().anyMatch(x->x.contains("not")||x.contains("depend")||x.contains("alternate")));
    }
    @Test void switchingActivitiesNeverChangesBossSelectionAndKeepsRealSlots() throws Exception {
        SwingUtilities.invokeAndWait(()-> {
            var model=new BurrowModel();var panel=new BurrowPanel(model);
            model.show(BurrowModel.View.OUTFITTER);
            var before=model.boss();var method=model.method();var tier=model.tier();
            assertFalse(panel.activityModeForTest());
            panel.clickAt(166,panel.tierYForR13Test()-13-8);
            assertTrue(panel.activityModeForTest());
            for(var kit:ActivityOutfitterCatalog.all()){
                panel.selectActivityForTest(kit.id());
                assertEquals(kit.id(),panel.activityKitIdForTest());
                int height=panel.getPreferredSize().height;
                assertTrue(height>880 && height<4000);
                var bitmap=new BufferedImage(225,height,BufferedImage.TYPE_INT_ARGB);
                panel.setSize(225,height);
                var g=bitmap.createGraphics(); try{panel.paint(g);}finally{g.dispose();}
                assertEquals(225,panel.getPreferredSize().width);
            }
            panel.setAppearance("EXTRA_LARGE","BRONZE");panel.setTextWeight("BOLD");
            var height=panel.getPreferredSize().height;
            assertTrue(height>880 && height<6000);
            var bitmap=new BufferedImage(225,height,BufferedImage.TYPE_INT_ARGB);
            panel.setSize(225,height);
            var g=bitmap.createGraphics();try{panel.paint(g);}finally{g.dispose();}
            assertEquals(before,model.boss());assertEquals(method,model.method());assertEquals(tier,model.tier());
            panel.clickAt(35,138-8);
            assertFalse(panel.activityModeForTest());
            assertEquals(before,model.boss());
        });
    }
}
