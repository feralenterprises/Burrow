package gg.feral.burrow.ui;

import gg.feral.burrow.core.AccountModePreference.Mode;
import gg.feral.burrow.core.BurrowModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/** UI-only checks; no RuneLite host, account or bank events are synthesized. */
final class UiR01TypographyJUnitTest {
    @Test void compactLegacyAndNewSizesAndWeightPersistViaCallbacks() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var p=new BurrowPanel(new BurrowModel()); p.setSize(225,900);
            p.setAccountModeState(42L,Mode.IRONMAN);
            Map<String,String> saved=new HashMap<>();
            p.setAppearanceAction((s,c)->{saved.put("size",s);saved.put("accent",c);});
            p.setTextWeightAction(w->saved.put("weight",w));
            p.clickAt(204,56);assertTrue(p.isSettingsOpen());
            p.clickAt(155,568);assertEquals("Extra Large",p.uiScaleLabel());
            p.clickAt(144,614);assertEquals("Bold",p.textWeightLabel());
            assertEquals("EXTRA_LARGE",saved.get("size"));
            assertEquals("BOLD",saved.get("weight"));
            p.clickAt(52,613);assertEquals("Regular",p.textWeightLabel());
            p.clickAt(54,536);assertEquals("Compact",p.uiScaleLabel());
            var restart=new BurrowPanel(new BurrowModel());
            restart.setAppearance("EXTRA_LARGE","BRONZE");restart.setTextWeight("BOLD");
            assertEquals("Extra Large",restart.uiScaleLabel());
            assertEquals("Bold",restart.textWeightLabel());
            restart.setTextWeight("UNKNOWN");assertEquals("Regular",restart.textWeightLabel());
            restart.setAppearance("DEFAULT","BRONZE");assertEquals("Normal",restart.uiScaleLabel());
        });
    }

    @Test void allFourViewsPaintAt225ForEveryNewCombination() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            for(String size:new String[]{"DEFAULT","LARGE","EXTRA_LARGE"}) {
                for(String weight:new String[]{"REGULAR","BOLD"}) {
                    BurrowModel m=new BurrowModel();var p=new BurrowPanel(m);
                    p.setAppearance(size,"BRONZE");p.setTextWeight(weight);
                    for(BurrowModel.View v:BurrowModel.View.values()) {
                        m.show(v);Dimension d=p.getPreferredSize();
                        assertEquals(225,d.width);
                        p.setSize(d);
                        BufferedImage image=new BufferedImage(225,d.height,BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g=image.createGraphics();try{p.paint(g);}finally{g.dispose();}
                    }
                }
            }
        });
    }

    @Test void wrapsWithoutLosingWordsOnOneOrTwoLinesAndUsesEllipsisWhenNecessary() {
        var image=new BufferedImage(225,100,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();
        try {
            Font font=FeralTypography.font(10,3,false,FeralTypography.TextWeight.BOLD);
            var fm=g.getFontMetrics(font);
            String message="Review your next Pathfinder requirement before the fight";
            var lines=FeralTypography.wrap(fm,message,140,3);
            assertTrue(lines.size()>=2);assertTrue(lines.size()<=3);
            for(String line:lines)assertTrue(fm.stringWidth(line)<=140);
            var shortList=FeralTypography.wrap(fm,message,140,1);
            assertEquals(1,shortList.size());assertTrue(shortList.get(0).endsWith("…"));
        } finally {g.dispose();}
    }
}
