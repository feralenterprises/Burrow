package gg.feral.burrow.preview;
import gg.feral.burrow.core.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.ui.BurrowPanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.nio.file.*;
/** Actual application Swing raster; not a screenshot of RuneLite or actual player inventory. */
public final class RenderR07Snapshots {
  public static void main(String[] args)throws Exception{
    Path target=Paths.get(args.length==0?"build/r07-screenshots":args[0]);Files.createDirectories(target);
    SwingUtilities.invokeAndWait(()->{
      try{
        String[][] cases={{"Scurrius","Ranged · rat-bone option","SCURRIUS-RANGED-RESEARCH-ONLY.png"},
          {"Bryophyta","Fire magic","BRYOPHYTA-FIRE-MAGIC-DRAFT.png"},
          {"Vorkath","Melee · lance / fang","VORKATH-MELEE-DRAFT.png"}};
        for(String[] c:cases){
          BurrowModel m=new BurrowModel();m.chooseBoss(c[0]);m.chooseTier(Tier.RECOMMENDED);m.chooseMethod(c[1]);m.show(BurrowModel.View.OUTFITTER);
          BurrowPanel p=new BurrowPanel(m);p.setSize(225,1250);
          BufferedImage image=new BufferedImage(225,1250,BufferedImage.TYPE_INT_ARGB);
          Graphics2D g=image.createGraphics();p.paint(g);g.dispose();ImageIO.write(image,"png",target.resolve(c[2]).toFile());
        }
      }catch(Exception e){throw new IllegalStateException(e);}
    });
    System.out.println("R07 Swing renders generated, not native RuneLite footage");
  }
}
