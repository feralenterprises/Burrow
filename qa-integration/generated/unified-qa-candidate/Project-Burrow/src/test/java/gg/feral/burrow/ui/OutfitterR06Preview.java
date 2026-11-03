package gg.feral.burrow.ui;
import gg.feral.burrow.core.BurrowModel;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;
import javax.imageio.ImageIO;import javax.swing.SwingUtilities;
/** Headless visual evidence only, not installed in RuneLite. */
public final class OutfitterR06Preview{
 public static void main(String[]args)throws Exception{
  String target=args.length>0?args[0]:"/mnt/data/BURROW-OUT-R06-STAGING/PREVIEWS";
  SwingUtilities.invokeAndWait(()->{
   try{
    for(String mode:new String[]{"boss","herb","birdhouse","slayer-ranged-wilderness"}){
     var m=new BurrowModel();m.show(BurrowModel.View.OUTFITTER);
     var p=new BurrowPanel(m);p.setAppearance("EXTRA_LARGE","BRONZE");p.setTextWeight("BOLD");
     if(!mode.equals("boss"))p.selectActivityForTest(mode);
     Dimension sz=p.getPreferredSize();p.setSize(sz);
     BufferedImage img=new BufferedImage(225,Math.min(1050,sz.height),BufferedImage.TYPE_INT_RGB);
     Graphics2D g=img.createGraphics();g.setColor(new Color(13,17,18));g.fillRect(0,0,225,img.getHeight());p.paint(g);g.dispose();
     var file=new File(target,mode+"-225-XL-BOLD.png");file.getParentFile().mkdirs();ImageIO.write(img,"png",file);
     System.out.println(mode+"="+file+" preferred "+sz.height);
    }
   }catch(Exception ex){throw new RuntimeException(ex);}
  });
 }
}
