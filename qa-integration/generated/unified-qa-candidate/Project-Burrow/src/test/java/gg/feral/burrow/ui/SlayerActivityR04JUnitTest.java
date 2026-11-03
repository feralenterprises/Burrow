package gg.feral.burrow.ui;
import gg.feral.burrow.core.BurrowModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.SwingUtilities;
/** SLAY-R04: reference-only boundaries and owner Wilderness lock. */
final class SlayerActivityR04JUnitTest {
 @Test void wildernessNeverPermitsBestBankAndOffersNoAutomaticSafeClaim() {
     var w=ActivityOutfitterCatalog.find("slayer-ranged-wilderness");
     assertEquals("WILDERNESS",w.category());
     assertTrue(ActivityOutfitterCatalog.isWilderness(w.id()));
     assertFalse(ActivityOutfitterCatalog.bestBankPermitted(w.id()));
     assertFalse(ActivityOutfitterCatalog.bestBankPermitted("future-wilderness-slayer"));
     assertTrue(ActivityOutfitterCatalog.bestBankPermitted("slayer-melee"));
     assertTrue(w.notes().stream().anyMatch(n->n.contains("BEST IN BANK PERMANENTLY DISABLED")));
     assertTrue(w.notes().stream().anyMatch(n->n.contains("not a universal safe")));
 }
 @Test void distinctSourceVariantsAndSafeTwoHandedSlots() {
     assertEquals(20,ActivityOutfitterCatalog.all().size());
     var h=ActivityOutfitterCatalog.find("slayer-melee-hallowfell");
     assertTrue(h.equipment().get(4).label().contains("Hallowfell"));
     assertNull(h.equipment().get(6),"Two-handed Hallowfell must leave shield empty");
     assertEquals(34027,ActivityItemRefs.find(h.id(),true,4).itemId());
     var barrage=ActivityOutfitterCatalog.find("slayer-magic-barrage");
     assertTrue(barrage.notes().stream().anyMatch(n->n.contains("Ancient Magicks")));
     assertEquals(21006,ActivityItemRefs.find(barrage.id(),true,4).itemId());
     assertNull(ActivityItemRefs.find("trees",false,0),"No invented sapling item ID");
     assertEquals(952,ActivityItemRefs.find("trees",false,1).itemId());
     assertEquals(7409,ActivityItemRefs.find("herb",true,4).itemId());
     assertEquals(5305,ActivityItemRefs.find("birdhouse",false,4).itemId());
 }
 @Test void actualItemImageResolverAndWildernessBossIsolation() throws Exception {
     SwingUtilities.invokeAndWait(()->{
        var m=new BurrowModel();m.show(BurrowModel.View.OUTFITTER);
        var observed=new HashSet<Integer>();
        var panel=new BurrowPanel(m,id->{observed.add(id);return new BufferedImage(28,28,BufferedImage.TYPE_INT_ARGB);});
        String boss=m.boss(),method=m.method();var tier=m.tier();
        for(String kit:List.of("herb","birdhouse","slayer-melee-hallowfell","slayer-magic-barrage","slayer-ranged-wilderness")){
          panel.selectActivityForTest(kit);panel.setAppearance("EXTRA_LARGE","BRONZE");panel.setTextWeight("BOLD");
          int h=panel.getPreferredSize().height;assertTrue(h>880 && h<6000);
          panel.setSize(225,h);
          var bmp=new BufferedImage(225,h,BufferedImage.TYPE_INT_ARGB);var g=bmp.createGraphics();
          try{panel.paint(g);} finally{g.dispose();}
        }
        assertTrue(observed.contains(34027));assertTrue(observed.contains(2503));
        assertTrue(observed.contains(7409));assertTrue(observed.contains(5305));
        assertEquals(boss,m.boss());assertEquals(method,m.method());assertEquals(tier,m.tier());
     });
 }
}
