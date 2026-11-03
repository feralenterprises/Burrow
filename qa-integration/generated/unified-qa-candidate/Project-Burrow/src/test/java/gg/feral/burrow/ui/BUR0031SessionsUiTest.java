package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import static gg.feral.burrow.ui.BurrowPanel.*;

/** BUR-0031 isolated EDT/Swing UI proof. The populated capture is SYNTHETIC FIXTURE only. */
public final class BUR0031SessionsUiTest {
    private static int checks;
    private static void ok(boolean test,String message) {
        checks++;if(!test)throw new AssertionError(message);
    }
    private static BufferedImage screenshot(BurrowPanel panel, int height) {
        panel.setSize(225,height);
        BufferedImage image=new BufferedImage(225,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();return image;
    }
    private static void save(Path path, BufferedImage image) throws Exception {
        ImageIO.write(image,"png",path.toFile());
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("screenshots output directory required");
        Path folder=Path.of(args[0]);Files.createDirectories(folder);
        SwingUtilities.invokeAndWait(()->{
            try {
                BurrowModel model=new BurrowModel();BurrowPanel panel=new BurrowPanel(model);
                panel.clickAt(191,94);
                ok(model.view()==BurrowModel.View.SESSIONS,"Sessions fourth tab must remain");
                SessionReadout empty=panel.sessionReadout();
                ok(empty.state()==SessionState.OFFLINE&&empty.totalXp()==0&&empty.kcByBoss().isEmpty(),"disconnected starts empty");
                ok(empty.feeds().get(SessionFeed.XP)==FeedStatus.NOT_CONNECTED,"unknown not coerced to zero availability");
                ok(panel.getPreferredSize().width==225,"225px sidebar remains");
                save(folder.resolve("BUR0031-225-EMPTY-NOT-CONNECTED.png"),screenshot(panel,880));
                SessionScope a=new SessionScope("fixture_A","login_A_1");
                SessionOrigin synth=SessionOrigin.SYNTHETIC_FIXTURE;
                ok(panel.ingestSessionEvent(new SessionStart(a,"start-a",synth)),"synthetic start");
                ok(panel.ingestSessionEvent(new SessionFeedStatus(a,"xp-ready",synth,SessionFeed.XP,FeedStatus.AVAILABLE)),"xp feed ready");
                ok(panel.ingestSessionEvent(new SessionXpGain(a,"xp-1",synth,"Attack",1300,"synthetic delta fixture")),"attack XP");
                ok(!panel.ingestSessionEvent(new SessionXpGain(a,"xp-1",synth,"Attack",1300,"synthetic delta fixture")),"event replay suppressed");
                ok(panel.ingestSessionEvent(new SessionXpGain(a,"xp-2",synth,"Strength",800,"synthetic delta fixture")),"strength XP");
                ok(panel.ingestSessionEvent(new SessionBossKcGain(a,"kc-1",synth,"Scurrius",3,"synthetic verified delta")),"boss KC");
                ok(panel.ingestSessionEvent(new SessionDrop(a,"drop-1",synth,995,"Coins",15000,"Scurrius","synthetic kill-link fixture")),"boss drop");
                ok(panel.ingestSessionEvent(new SessionDrop(a,"drop-2",synth,11230,"Dragon dart",12,null,"synthetic no-boss-link fixture")),"unassigned drop");
                ok(panel.ingestSessionEvent(new SessionSupplyUsed(a,"supply-1",synth,385,"Shark",2,"synthetic consumed item")),"consumable");
                SessionReadout populated=panel.sessionReadout();
                ok(populated.synthetic()&&populated.totalXp()==2100&&populated.xpBySkill().get("Attack")==1300,"total and by-skill XP");
                ok(populated.kcByBoss().get("Scurrius")==3,"delta-only boss KC");
                ok(populated.drops().size()==2&&populated.drops().get(1).attribution().equals("Unassigned"),"no fabricated boss attribution");
                ok(populated.supplies().get(0).quantity()==2,"consumption amounts");
                ok(panel.getPreferredSize().width==225&&panel.getPreferredSize().height>=880,"dynamic panel layout");
                save(folder.resolve("BUR0031-225-SYNTHETIC-FIXTURE-TOP.png"),screenshot(panel,880));
                save(folder.resolve("BUR0031-225-SYNTHETIC-FIXTURE-FULL.png"),screenshot(panel,panel.getPreferredSize().height));
                panel.clickAt(45,409);
                ok(model.demoKills()==0,"Sessions must not have hidden demo click actions");
                ok(panel.ingestSessionEvent(new SessionPause(a,"hop",synth)),"temporary pause");
                ok(panel.sessionReadout().state()==SessionState.SUSPENDED,"paused UI");
                ok(!panel.ingestSessionEvent(new SessionBossKcGain(a,"kc-stale",synth,"Scurrius",999,"stale")),"no events while paused");
                ok(panel.ingestSessionEvent(new SessionResume(a,"resume",synth)),"same-login resume");
                ok(panel.sessionReadout().kcByBoss().get("Scurrius")==3,"hop preserves existing count");
                SessionScope b=new SessionScope("fixture_B","login_B_1");
                ok(panel.ingestSessionEvent(new SessionStart(b,"start-b",synth)),"account switch");
                ok(panel.sessionReadout().totalXp()==0&&panel.sessionReadout().drops().isEmpty(),"account B isolated");
                ok(!panel.ingestSessionEvent(new SessionXpGain(a,"old-a",synth,"Attack",900,"stale")),"late account A callback blocked");
                ok(!panel.ingestSessionEvent(new SessionStart(a,"replay-a",synth)),"retired epoch cannot reactivate");
                ok(panel.ingestSessionEvent(new SessionStop(b,"logout-b",synth)),"verified logout");
                ok(panel.sessionReadout().state()==SessionState.OFFLINE&&panel.sessionReadout().feeds().get(SessionFeed.DROPS)==FeedStatus.NOT_CONNECTED,"logout clears and marks unknown");
                ok(!panel.ingestSessionEvent(new SessionDrop(b,"post-logout",synth,995,"Coins",1,null,"late")),"post-logout event rejected");
                ok(!panel.ingestSessionEvent(new SessionStart(b,"reuse-b",synth)),"old epoch reuse rejected");
                SessionScope freshA=new SessionScope("fixture_A","login_A_2");
                ok(panel.ingestSessionEvent(new SessionStart(freshA,"fresh-a",synth)),"new authenticated epoch after A-B-A accepted");
                ok(panel.sessionReadout().totalXp()==0&&panel.sessionReadout().kcByBoss().isEmpty(),"fresh A cannot see old A data");
                ok(!panel.ingestSessionEvent(new SessionXpGain(freshA,"wrong-origin",SessionOrigin.VERIFIED_BRIDGE,"Attack",5,"stale")),"cannot mix synthetic and purported live events");
                ok(panel.ingestSessionEvent(new SessionFeedStatus(freshA,"sup-unavailable",synth,SessionFeed.SUPPLIES,FeedStatus.UNAVAILABLE)),"unavailable feed distinct from empty");
                ok(panel.sessionReadout().feeds().get(SessionFeed.SUPPLIES)==FeedStatus.UNAVAILABLE,"unavailable retained");
                save(folder.resolve("BUR0031-225-SOURCE-UNAVAILABLE-FIXTURE.png"),screenshot(panel,880));
                panel.clickAt(35,94);ok(model.view()==BurrowModel.View.HOME,"Home navigation preserved");
                panel.clickAt(85,94);ok(model.view()==BurrowModel.View.PATHFINDER,"Pathfinder navigation preserved");
                panel.clickAt(137,94);ok(model.view()==BurrowModel.View.OUTFITTER,"Outfitter navigation preserved");
                ok(!model.methods().isEmpty()&&panel.methodMenu().getComponentCount()==model.methods().size(),"existing method dropdown preserved");
                System.out.println("BUR-0031 focused Sessions UI: "+checks+" assertions PASS; synthetic screenshots saved");
            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
}
