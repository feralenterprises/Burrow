package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

/** BUR-0035: visible PvP warning and the KBD-specific content shift. */
public final class KbdWildernessWarningTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static String tip(BurrowPanel panel, int x, int y) {
        return panel.getToolTipText(new MouseEvent(panel,MouseEvent.MOUSE_MOVED,
            System.currentTimeMillis(),0,x,y,0,false));
    }
    private static void run() {
        BurrowModel model=new BurrowModel();
        BurrowPanel panel=new BurrowPanel(model);
        panel.setSize(BurrowPanel.WIDTH,BurrowPanel.HEIGHT);
        model.show(BurrowModel.View.OUTFITTER);
        check(BurrowPanel.showsWildernessTravelWarning("King Black Dragon"),"KBD route flagged");
        check(!BurrowPanel.showsWildernessTravelWarning("Vorkath"),"non-Wilderness boss not flagged");
        for(Tier tier:Tier.values()) {
            model.chooseBoss("King Black Dragon");
            model.chooseTier(tier);
            BufferedImage rendered=new BufferedImage(BurrowPanel.WIDTH,BurrowPanel.HEIGHT,BufferedImage.TYPE_INT_ARGB);
            var graphics=rendered.createGraphics();
            panel.paint(graphics); graphics.dispose();
            int warningPixel=rendered.getRGB(20,panel.sourceYForR13Test()+29-8+8)&0xffffff;
            check(warningPixel==0x46201a,"warning has high-contrast background on "+tier);
            String warning=tip(panel,25,panel.sourceYForR13Test()+29-8+19);
            check(warning!=null && warning.contains("PvP Wilderness") && warning.contains("outside the Wilderness"),
                "KBD warning explains travel risk vs safe lair on "+tier);
            check(model.profile()!=null,"KBD tier is still available on "+tier);
            // The warning must not steal the equipment or inventory tooltip positions.
            var head=model.profile().gear().get(0);
            check(tip(panel,110,-8+panel.equipmentYForR13Test()+43+20).contains(head.name()),"head item still hoverable on "+tier);
            var firstItem=model.profile().inventory().get(0);
            check(firstItem!=null && tip(panel,30,-8+panel.equipmentYForR13Test()+281+31+10).contains(firstItem.name()),
                "inventory still hoverable on "+tier);
            check(tip(panel,105,-8+panel.equipmentYForR13Test()+6+15)!=null,"style control remains hoverable on "+tier);
        }
        model.chooseBoss("Vorkath");
        BufferedImage other=new BufferedImage(BurrowPanel.WIDTH,BurrowPanel.HEIGHT,BufferedImage.TYPE_INT_ARGB);
        var graphics=other.createGraphics(); panel.paint(graphics); graphics.dispose();
        check((other.getRGB(20,panel.sourceYForR13Test()+29-8+8)&0xffffff)!=0x46201a,"non-KBD layout has no warning banner");
        String otherTip=tip(panel,25,panel.sourceYForR13Test()+29-8+19);
        check(otherTip==null || !otherTip.contains("PvP Wilderness"),"non-KBD tooltip has no KBD warning");
        check(model.boss().equals("Vorkath"),"boss selection remains intact");
        System.out.println("BUR-0035 KBD WILDERNESS WARNING PASS: "+checks+" assertions");
    }
    public static void main(String[] args) throws Exception {
        if(SwingUtilities.isEventDispatchThread()) run();
        else SwingUtilities.invokeAndWait(KbdWildernessWarningTest::run);
    }
}
