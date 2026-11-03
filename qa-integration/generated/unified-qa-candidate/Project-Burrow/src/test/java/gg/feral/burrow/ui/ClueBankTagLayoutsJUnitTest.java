package gg.feral.burrow.ui;

import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import gg.feral.burrow.core.BurrowModel;
import java.util.*;

/** Exact user-provided Wiki Bank Tag Layout exports: these are 8x7 bank, never player inventory. */
final class ClueBankTagLayoutsJUnitTest {
    @Test void allNineValidAndNoInventedQuantities() {
        var all=ClueBankTagLayouts.all();assertEquals(9,all.size());
        assertEquals(9,all.stream().map(x->x.id()).distinct().count());
        for(var x:all){
            assertEquals(56,x.slots().length);
            assertTrue(x.count()>15);
            assertTrue(x.raw().startsWith("banktags,1,"));
            assertEquals(x.count(),Arrays.stream(x.slots()).filter(i->i>0).count());
            assertEquals(x.raw().split(",").length,5+x.count()*2);
            assertTrue(ActivityOutfitterCatalog.all().stream().anyMatch(k->k.id().equals(x.id())));
        }
        var medium=ClueBankTagLayouts.find("clue-medium-hunting");
        assertEquals(11260,medium.slots()[38]);
        assertEquals(11260,medium.slots()[46]);
        assertEquals(11260,medium.slots()[37]);
        assertNotNull(ClueBankTagLayouts.find("clue-hard-other"));
        assertNull(ClueBankTagLayouts.find("clue-fictional"));
    }
    @Test void rendererUsesActualSourceItemIdsAndKeepsBossState() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var model=new BurrowModel();
            var seen=new HashSet<Integer>();
            var panel=new BurrowPanel(model,id->{seen.add(id);return new BufferedImage(24,24,BufferedImage.TYPE_INT_ARGB);});
            model.show(BurrowModel.View.OUTFITTER);
            var boss=model.boss();var tier=model.tier();var method=model.method();
            panel.selectActivityForTest("clue-master");
            panel.setAppearance("EXTRA_LARGE","BRONZE");panel.setTextWeight("BOLD");
            int h=panel.getPreferredSize().height;assertTrue(h<4000);
            panel.setSize(225,h);
            var img=new BufferedImage(225,h,BufferedImage.TYPE_INT_ARGB);
            var g=img.createGraphics();try{panel.paint(g);}finally{g.dispose();}
            assertEquals(Arrays.stream(ClueBankTagLayouts.find("clue-master").slots()).filter(id->id>0).distinct().count(),seen.size());
            assertEquals(boss,model.boss());assertEquals(tier,model.tier());assertEquals(method,model.method());
        });
    }
}
