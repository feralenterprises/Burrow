package gg.feral.burrow.ui;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;

public final class BankChestPixelsTest {
 public static void main(String[] args) {
  if(!BankChestPixels.ready())throw new AssertionError("Approved chest sprites absent");
  HashSet<Integer> distinct=new HashSet<>();
  for(BankChestPixels.State state:BankChestPixels.State.values()) {
   BufferedImage src=BankChestPixels.image(state);
   int hash=1;
   for(int y=0;y<src.getHeight();y++)for(int x=0;x<src.getWidth();x++)hash=31*hash+src.getRGB(x,y);
   distinct.add(hash);
   BufferedImage out=new BufferedImage(28,28,BufferedImage.TYPE_INT_ARGB);
   Graphics2D g=out.createGraphics();BankChestPixels.draw(g,new Rectangle(2,2,24,24),state);g.dispose();
   if(out.getRGB(12,12)==0)throw new AssertionError("Bank chest empty at "+state);
  }
  if(distinct.size()!=4)throw new AssertionError("All four owner states must be distinct");
  System.out.println("BANK CHEST PIXELS PASS: 4 owner states, 24px painting (offline only)");
 }
}
