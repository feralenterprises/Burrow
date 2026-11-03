package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.LiveCombat;
import gg.feral.burrow.core.LiveWeapon;
import gg.feral.burrow.core.QuestRecommendation;
import gg.feral.burrow.core.IronLedgerQuestSurvey;
import java.util.Map;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

/** BUR-0017/0018: narrow visual regression; no RuneLite/native claims. */
public final class FeralModernHomePathfinderUiTest {
    private static BufferedImage render(BurrowPanel panel) {
        BufferedImage image = new BufferedImage(225, 880, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        panel.paint(g);
        g.dispose();
        return image;
    }
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static boolean different(BufferedImage a, BufferedImage b, int left, int top, int right, int bottom) {
        for(int y=top;y<bottom;y++) for(int x=left;x<right;x++) {
            if(a.getRGB(x,y)!=b.getRGB(x,y)) return true;
        }
        return false;
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            require(Sprites.loaded(), "Missing approved original artwork");
            BurrowModel model = new BurrowModel();
            BurrowPanel panel = new BurrowPanel(model);
            panel.setSize(225,880);
            require(panel.getPreferredSize().width==225, "Native sidebar width must remain 225");
            BufferedImage home = render(panel);
            // Owner-approved compact brand badge must render, with no large raccoon panel.
            require(different(home, new BufferedImage(225,880,BufferedImage.TYPE_INT_RGB),12,20,46,55),
                "Owner R01 compact brand badge missing");
            panel.clickAt(85,94);
            require(model.view()==BurrowModel.View.PATHFINDER, "Four-tab navigation changed");
            require(panel.observedQuest().state()==QuestRecommendation.State.OFFLINE,
                "Visual lane must not assume a verified quest");
            BufferedImage before = render(panel);
            String selected = model.boss();
            // Dependency entries are evidence-only and cannot silently mutate boss state.
            panel.clickAt(82,600);
            require(selected.equals(model.boss()), "Route card gained phantom combat action");
            panel.setQuestSurvey(new IronLedgerQuestSurvey.Snapshot(
                Map.of("Cook's Assistant","FINISHED"),true,0,1,1));
            BufferedImage finished = render(panel);
            require(different(before,finished,18,312,210,388), "Observed quest status not displayed");
            panel.clickAt(35,94);
            require(model.view()==BurrowModel.View.HOME, "Home tab no longer works");
            panel.setObservedWeapon(LiveWeapon.equipped(4151,"Abyssal whip"));
            panel.setObservedCombat(LiveCombat.fromRealLevels(new int[]{75,80,75,82,81,70,70}));
            BufferedImage live = render(panel);
            require(different(home,live,18,351,204,404), "Home live observations did not update");
            require(model.view()==BurrowModel.View.HOME, "Paint changed selected tab");
            // Current Home and Pathfinder painter never manipulates profile or bank inputs.
            panel.clickAt(138,94);
            require(model.view()==BurrowModel.View.OUTFITTER, "Outfitter tab changed");
            panel.clickAt(191,94);
            require(model.view()==BurrowModel.View.SESSIONS, "Sessions tab changed");
            System.out.println("BUR-0017/0018 Feral Modern focused offline UI PASS");
        });
    }
}
