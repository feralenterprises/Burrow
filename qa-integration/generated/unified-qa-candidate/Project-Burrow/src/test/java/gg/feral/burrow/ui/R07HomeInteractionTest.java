package gg.feral.burrow.ui;
import gg.feral.burrow.core.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
/** Headless Swing source test: Home quick-prep and Sessions links do something visible. */
public final class R07HomeInteractionTest {
  private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
  public static void main(String[] args)throws Exception{
    SwingUtilities.invokeAndWait(()->{
      var m=new BurrowModel();var panel=new BurrowPanel(m);panel.setSize(225,880);
      var pixels=new BufferedImage(225,880,BufferedImage.TYPE_INT_ARGB);
      Graphics2D g=pixels.createGraphics();panel.paint(g);g.dispose();
      check(m.view()==BurrowModel.View.HOME,"Home is default");
      panel.clickAt(27,419);check(m.view()==BurrowModel.View.OUTFITTER,"quick prep routes to real Outfitter");
      panel.clickAt(35,94);check(m.view()==BurrowModel.View.HOME,"Home navigation returns");
      panel.clickAt(26,455);check(m.view()==BurrowModel.View.SESSIONS,"activity shortcut routes to real Sessions");
      check(panel.sessionReadout().totalXp()==0,"No fake XP created by dashboard");
      System.out.println("R07 HOME ACTIONS PASS: real routing and no fake session activity");
    });
  }
}
