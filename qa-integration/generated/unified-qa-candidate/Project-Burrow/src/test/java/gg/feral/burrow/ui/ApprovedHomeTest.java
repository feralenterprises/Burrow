package gg.feral.burrow.ui;
import gg.feral.burrow.core.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
public final class ApprovedHomeTest {
 public static void main(String[] args) throws Exception {
  SwingUtilities.invokeAndWait(()->{
   if(!Sprites.loaded())throw new AssertionError("Approved artwork missing");
   BurrowModel model=new BurrowModel();BurrowPanel p=new BurrowPanel(model);
   p.setSize(225,BurrowPanel.HEIGHT);
   if(!p.observedWeapon().displayName().contains("Log in"))throw new AssertionError("startup offline state");
   p.clickAt(83,94);if(model.view()!=BurrowModel.View.PATHFINDER)throw new AssertionError("Pathfinder navigation");
   p.clickAt(138,94);if(model.view()!=BurrowModel.View.OUTFITTER)throw new AssertionError("Outfitter navigation");
   p.clickAt(191,94);if(model.view()!=BurrowModel.View.SESSIONS)throw new AssertionError("Sessions navigation");
   p.clickAt(35,94);if(model.view()!=BurrowModel.View.HOME)throw new AssertionError("Home navigation");
   p.clickAt(30,640);if(model.view()!=BurrowModel.View.HOME)throw new AssertionError("No phantom demo Home action");
   p.setObservedWeapon(LiveWeapon.equipped(4151,"Abyssal whip"));
   p.setObservedCombat(LiveCombat.fromRealLevels(new int[]{75,80,75,82,81,70,70}));
   if(!p.observedWeapon().displayName().equals("Abyssal whip"))throw new AssertionError("weapon data");
   if(p.observedCombat().state()!=LiveCombat.State.OBSERVED)throw new AssertionError("combat data");
   BufferedImage out=new BufferedImage(225,BurrowPanel.HEIGHT,BufferedImage.TYPE_INT_ARGB);
   Graphics2D g=out.createGraphics();p.paint(g);g.dispose();
   JScrollPane scroll=new JScrollPane(p,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
   scroll.setSize(242,440);scroll.doLayout();
   if(scroll.getVerticalScrollBar().getMaximum()<=440)throw new AssertionError("No short-window scrolling");
   System.out.println("C07 HOME PASS: 225px real Java/Swing, four tabs, live observations, short-window scroll");
  });
 }
}
