package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.LiveWeapon;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** A simulated visual reference only. This does not read actual game data. */
public final class LiveWeaponPreview {
    public static void main(String[] args) throws Exception {
        if (args.length!=1) throw new IllegalArgumentException("output png path required");
        SwingUtilities.invokeAndWait(() -> {
            try {
                BurrowModel model=new BurrowModel();
                model.show(BurrowModel.View.OUTFITTER);
                BurrowPanel panel=new BurrowPanel(model);
                panel.setSize(225,730);
                panel.setObservedWeapon(LiveWeapon.equipped(4587,"Dragon scimitar"));
                BufferedImage img=new BufferedImage(225,730,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=img.createGraphics();panel.paint(g);g.dispose();
                ImageIO.write(img,"png",new File(args[0]));
            } catch(Exception x) { throw new RuntimeException(x); }
        });
        System.out.println("Wrote simulated UI preview: " + args[0]);
    }
}
