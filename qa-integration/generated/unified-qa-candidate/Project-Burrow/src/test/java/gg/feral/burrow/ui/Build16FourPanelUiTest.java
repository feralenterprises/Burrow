package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.IronLedgerQuestSurvey;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.swing.SwingUtilities;

/** Actual Swing four-panel R01 UI + routing, preserves genuine source contracts. */
public final class Build16FourPanelUiTest {
    private static void check(boolean ok,String why) { if(!ok)throw new AssertionError(why); }
    private static BufferedImage render(BurrowPanel panel) {
        BufferedImage out=new BufferedImage(225,880,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=out.createGraphics(); panel.paint(g); g.dispose(); return out;
    }
    private static boolean regionDiff(BufferedImage a,BufferedImage b,int x,int y,int w,int h) {
        for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)
            if(a.getRGB(xx,yy)!=b.getRGB(xx,yy))return true;
        return false;
    }
    public static void main(String[] ignored) throws Exception {
        SwingUtilities.invokeAndWait(()->{
            BurrowModel model=new BurrowModel();
            BurrowPanel panel=new BurrowPanel(model);panel.setSize(225,880);
            BufferedImage home=render(panel);
            check(home.getRGB(22,90)!=home.getRGB(79,90),"selected Home tab visibly distinguished");
            panel.clickAt(85,94); check(model.view()==BurrowModel.View.PATHFINDER,"Pathfinder tab");
            BufferedImage before=render(panel);
            panel.setQuestSurvey(new IronLedgerQuestSurvey.Snapshot(Map.of("Cook's Assistant","FINISHED"),true,0,1,1));
            check(regionDiff(before,render(panel),18,318,192,74),"quest count reflects observed survey");
            panel.clickAt(36,777);check(model.view()==BurrowModel.View.OUTFITTER,"route CTA opens Outfitter");
            check(model.profile()!=null,"boss profile preserved");
            panel.clickAt(190,94);check(model.view()==BurrowModel.View.SESSIONS,"Sessions tab");
            check(panel.sessionReadout().totalXp()==0,"no fictional XP on UI navigation");
            panel.clickAt(34,94);check(model.view()==BurrowModel.View.HOME,"Home tab");
            panel.clickAt(30,419);check(model.view()==BurrowModel.View.OUTFITTER,"Home quick dispatch");
            panel.clickAt(34,94);panel.clickAt(30,455);
            check(model.view()==BurrowModel.View.SESSIONS,"Home session shortcut");
            System.out.println("Build16FourPanelUiTest PASS: four tabs, real data, routes, no fake XP");
        });
    }
}
