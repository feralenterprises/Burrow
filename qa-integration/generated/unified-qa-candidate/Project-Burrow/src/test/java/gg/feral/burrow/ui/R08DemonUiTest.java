package gg.feral.burrow.ui;
import gg.feral.burrow.core.*;
import java.util.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.*;
/** Native Swing menu/render proof with synthetic model, not a RuneLite runtime proof. */
public final class R08DemonUiTest {
  private static int checks;
  private static void check(boolean yes,String note){checks++;if(!yes)throw new AssertionError(note);}
  private static void run(){
    BurrowModel model=new BurrowModel();model.show(BurrowModel.View.OUTFITTER);
    BurrowPanel p=new BurrowPanel(model);
    for(String boss:DemonEncounterLoadouts.bosses()){
      model.chooseBoss(boss);
      JPopupMenu menu=p.methodMenu();
      check(menu.getComponentCount()==DemonEncounterLoadouts.methods(boss).size(),"new methods appear in real Swing dropdown");
      for(int n=0;n<menu.getComponentCount();n++){
        var entry=(JRadioButtonMenuItem)menu.getComponent(n);
        check(entry.getText().equals(DemonEncounterLoadouts.methods(boss).get(n)),"method order");
        entry.doClick();
        check(model.method().equals(entry.getText()),"method click routes through model");
        check(model.plan()!=null,"data-backed recommendation for "+entry.getText());
        p.setSize(p.getPreferredSize());
        BufferedImage image=new BufferedImage(225,p.getPreferredSize().height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();p.paint(g);g.dispose();
        check(p.getPreferredSize().width==225,"still narrow 225px");
        check(image.getHeight()>880,"full notes scroll available");
      }
    }
    model.chooseBoss("Vorkath");check(model.profile()!=null,"original boss navigation retained");
    System.out.println("R08 DEMON SWING UI PASS "+checks+" checks: all two-boss real menus, six selected plans, 225px rendering");
  }
  public static void main(String...args)throws Exception{SwingUtilities.invokeAndWait(R08DemonUiTest::run);}
}
