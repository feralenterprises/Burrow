package gg.feral.burrow.preview;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.LiveCombat;
import gg.feral.burrow.core.LiveWeapon;
import gg.feral.burrow.ui.BurrowPanel;
import gg.feral.burrow.ui.BankChestPixels;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** Actual Swing raster under headless JDK; NOT the RuneLite client or owner-native proof. */
public final class RenderOwnerReview {
    private static void save(JComponent c, int w, int h, Path file) throws Exception {
        BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();c.paint(g);g.dispose();
        ImageIO.write(image,"png",file.toFile());
    }
    public static void main(String[] args) throws Exception {
        Path target=Paths.get(args.length==0?"build/owner-review":args[0]);
        Files.createDirectories(target);
        SwingUtilities.invokeAndWait(()->{
            try {
                BurrowPanel home=new BurrowPanel(new BurrowModel());home.setSize(225,880);
                save(home,225,880,target.resolve("HOME-ACTUAL-SWING-225x880.png"));
                BurrowPanel example=new BurrowPanel(new BurrowModel(), true);
                example.setSize(225,880);
                save(example,225,880,target.resolve("HOME-LAYOUT-ILLUSTRATION-SAMPLE-NOT-LIVE.png"));
                // RuneLite PluginPanel itself wraps the content in a vertical JScrollPane.
                JScrollPane scroll=new JScrollPane(home,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                scroll.setBorder(BorderFactory.createEmptyBorder());scroll.setSize(242,440);scroll.doLayout();
                save(scroll,242,440,target.resolve("HOME-SHORT-242x440-TOP.png"));
                scroll.getVerticalScrollBar().setValue(250);scroll.doLayout();
                save(scroll,242,440,target.resolve("HOME-SHORT-242x440-SCROLLED.png"));
                home.setSize(225,880);
                home.setObservedWeapon(LiveWeapon.equipped(4151,"Abyssal whip"));
                home.setObservedCombat(LiveCombat.fromRealLevels(new int[]{75,80,75,82,81,70,70}));
                save(home,225,880,target.resolve("HOME-SYNTHETIC-OBSERVATION-FIXTURE.png"));
                int x=2;
                BufferedImage bank=new BufferedImage(150,46,BufferedImage.TYPE_INT_ARGB);
                Graphics2D bankG=bank.createGraphics();
                for (BankChestPixels.State state:BankChestPixels.State.values()) {
                    BankChestPixels.draw(bankG,new Rectangle(x,6,30,30),state);
                    x+=37;
                }
                bankG.dispose();ImageIO.write(bank,"png",target.resolve("BANK-FOUR-STATES-SWING-FIXTURE.png").toFile());
            } catch(Exception e) { throw new IllegalStateException(e); }
        });
        System.out.println("Saved actual Java/Swing screenshots; no RuneLite was started.");
    }
}
