package gg.feral.burrow.preview;
import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.ui.BurrowPanel;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;
import java.nio.file.*;
/** Deterministic off-screen render for real previews. */
public final class RenderPreview {
    public static void main(String[] args) throws Exception {
        Path directory=Paths.get(args.length==0?"build/previews":args[0]);Files.createDirectories(directory);
        BurrowModel model=new BurrowModel();BurrowPanel p=new BurrowPanel(model);
        p.setSize(BurrowPanel.WIDTH,BurrowPanel.HEIGHT);
        for(BurrowModel.View v:BurrowModel.View.values()) {
            model.show(v);
            if(v==BurrowModel.View.OUTFITTER) model.loadDemoBank();
            if(v==BurrowModel.View.SESSIONS) {model.recordDemoKill();model.recordDemoKill();}
            BufferedImage img=new BufferedImage(BurrowPanel.WIDTH,BurrowPanel.HEIGHT,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=img.createGraphics();p.paint(g);g.dispose();
            ImageIO.write(img,"png",directory.resolve(v.name().toLowerCase()+".png").toFile());
        }
        System.out.println("Rendered 4 actual Swing views to " + directory);
    }
}
