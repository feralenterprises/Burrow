package gg.feral.burrow.ui;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Four EXACT owner-supplied chest appearance crops; no repainting or invented icons. */
public final class BankChestPixels {
    public enum State { NORMAL, HOVER, PRESSED, DISABLED }
    private static final String ROOT="/gg/feral/burrow/ui/bank-approved-";
    private static final BufferedImage[] IMAGES={load("normal"),load("hover"),load("pressed"),load("disabled")};
    private static BufferedImage load(String state) {
        try(InputStream in=BankChestPixels.class.getResourceAsStream(ROOT+state+".png")) {
            return in==null?null:ImageIO.read(in);
        } catch(java.io.IOException ex) { return null; }
    }
    public static boolean ready() {
        for(BufferedImage img:IMAGES)if(img==null||img.getWidth()<12||img.getHeight()<12)return false;
        return true;
    }
    public static BufferedImage image(State state) { return IMAGES[state.ordinal()]; }
    public static void draw(Graphics2D g, Rectangle bounds, State state) {
        if(!ready()||bounds==null||bounds.width<=0||bounds.height<=0)return;
        BufferedImage art=image(state);
        Object prev=g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        // Overlay covers the existing underlying literal 'O' without changing its tested widget listeners.
        // No border is added because discovery has already reserved collision-free native header geometry.
        g.setColor(new java.awt.Color(31,27,23));
        g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(art,bounds.x,bounds.y,bounds.width,bounds.height,null);
        if(prev!=null)g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,prev);
        else g.getRenderingHints().remove(RenderingHints.KEY_INTERPOLATION);
    }
    private BankChestPixels() {}
}
