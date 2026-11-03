package gg.feral.burrow.ui;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
/** Original owner-approved mascot crop in the real RuneLite toolbar. */
public final class TrashPandaToolbarIcon {
    public static BufferedImage create() {
        BufferedImage result=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        BufferedImage mascot=Sprites.load("feral-avatar.png");
        if(mascot!=null) {
            Graphics2D g=result.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(mascot,0,0,16,16,null);g.dispose();
        }
        return result;
    }
    private TrashPandaToolbarIcon() {}
}
