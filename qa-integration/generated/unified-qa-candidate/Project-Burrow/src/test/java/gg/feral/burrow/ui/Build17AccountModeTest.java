package gg.feral.burrow.ui;

import gg.feral.burrow.core.AccountModePreference;
import gg.feral.burrow.core.AccountModePreference.Mode;
import gg.feral.burrow.core.BurrowModel;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** No RuneLite account spoofing. Uses a memory-backed config adapter and real Swing actions. */
public final class Build17AccountModeTest {
    private static int checks;
    private static void ok(boolean expected, String reason) {
        checks++;
        if (!expected) throw new AssertionError(reason);
    }
    private static BufferedImage draw(BurrowPanel p) {
        BufferedImage image = new BufferedImage(225,880,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics(); p.paint(g); g.dispose();return image;
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Map<String,String> persisted=new HashMap<>();
            AccountModePreference prefs=new AccountModePreference(persisted::get,persisted::put);
            long a=123456789123L,b=912348889991L;
            ok(prefs.resolve(a)==Mode.IRONMAN,"A defaults to Ironman");
            ok(prefs.resolve(b)==Mode.IRONMAN,"B defaults to Ironman");
            ok(prefs.read(-1L).isEmpty(),"no anonymous account selection");
            try {prefs.save(-1L,Mode.MAIN);throw new AssertionError("anonymous write accepted");}
            catch (IllegalArgumentException expected) {checks++;}
            BurrowModel model=new BurrowModel();
            BurrowPanel panel=new BurrowPanel(model); panel.setSize(225,880);
            panel.setAccountModeAction((hash,mode)-> {
                prefs.save(hash,mode);
                panel.setAccountModeState(hash,prefs.read(hash).orElse(null));
            });
            panel.setAccountModeState(-1L,null);
            ok(panel.isAccountSetupRequired(),"logged out setup shown");
            panel.clickAt(42,310);
            ok(persisted.isEmpty(),"no mode saved before login");
            panel.setAccountModeState(a,prefs.resolve(a));
            ok(!panel.isAccountSetupRequired(),"Ironman default no setup prompt");
            ok(panel.currentAccountMode()==Mode.IRONMAN,"A selected Ironman");
            ok(prefs.read(a).isEmpty(),"default does not require persistence");
            ok(model.view()==BurrowModel.View.HOME,"setup does not navigate app");
            panel.clickAt(204,56);
            ok(panel.isSettingsOpen(),"header settings opens");
            panel.clickAt(42,359);
            ok(panel.currentAccountMode()==Mode.MAIN,"settings changed to Main");
            ok(prefs.read(a).orElse(null)==Mode.MAIN,"Main persisted for A");
            panel.clickAt(46,443);
            ok(!panel.isSettingsOpen(),"back returns to tracker");
            panel.setAccountModeState(-1L,null);
            panel.setAccountModeState(b,prefs.resolve(b));
            ok(!panel.isAccountSetupRequired(),"B uses Ironman default");
            ok(prefs.read(b).isEmpty(),"B not contaminated by A override");
            panel.setAccountModeState(-1L,null);
            panel.setAccountModeState(a,prefs.resolve(a));
            ok(panel.currentAccountMode()==Mode.MAIN,"A restored independently after B");
            ok(!panel.isAccountSetupRequired(),"A no second onboarding");
            ok(!panel.isSettingsOpen(),"account change closes settings");
            persisted.put(AccountModePreference.key(111L),"NOT_A_MODE");
            ok(prefs.resolve(111L)==Mode.IRONMAN,"malformed setting uses Ironman default");
            panel.clickAt(90,94);
            ok(model.view()==BurrowModel.View.PATHFINDER,"normal nav still works after setup");
            panel.clickAt(140,94);
            ok(model.view()==BurrowModel.View.OUTFITTER,"Outfitter nav preserved");
            panel.clickAt(188,94);
            ok(model.view()==BurrowModel.View.SESSIONS,"Sessions nav preserved");
            // optional screenshot output for human visual inspection, never used as a functional assertion.
            if(args.length>0) {
                try {
                    panel.setAccountModeState(-1L,null);
                    panel.setAccountModeState(24680L,Mode.IRONMAN);
                    ImageIO.write(draw(panel),"png",new File(args[0]+"/FIRST-RUN-225.png"));
                    panel.setAccountModeState(a,Mode.MAIN);
                    panel.clickAt(205,56);
                    ImageIO.write(draw(panel),"png",new File(args[0]+"/SETTINGS-225.png"));
                } catch(Exception e) {throw new RuntimeException(e);}
            }
            System.out.println("Build17AccountModeTest PASS " + checks + " checks");
        });
    }
}
