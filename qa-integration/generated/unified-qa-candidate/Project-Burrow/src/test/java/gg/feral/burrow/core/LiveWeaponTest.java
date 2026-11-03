package gg.feral.burrow.core;

import gg.feral.burrow.ui.BurrowPanel;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

/** Dependency-free source regression; synthetic transitions, NOT a native client test. */
public final class LiveWeaponTest {
    private static int checks;
    static void check(boolean expected, String message) {
        checks++;
        if (!expected) throw new AssertionError("LiveWeapon: " + message);
    }
    public static void main(String[] args) {
        LiveWeaponGate gate = new LiveWeaponGate();
        check(gate.current().state()==LiveWeapon.State.OFFLINE,"initial state offline");
        long original = gate.start();
        check(gate.isCurrent(original),"active startup epoch");
        check(!gate.accept(original,LiveWeapon.equipped(1,"Old")),"no weapon before login");
        long loginA=gate.enterLoggedIn();
        check(gate.current().state()==LiveWeapon.State.WAITING,"login awaiting equipment");
        check(gate.accept(loginA,LiveWeapon.equipped(4587,"Dragon scimitar")),"weapon A");
        check(gate.current().displayName().equals("Dragon scimitar"),"real name A displayed");
        check(gate.accept(loginA,LiveWeapon.equipped(4151,"Abyssal whip")),"weapon B");
        check(gate.current().itemId()==4151,"B exact item ID");
        check(gate.accept(loginA,LiveWeapon.empty()),"unequip to empty");
        check(gate.current().state()==LiveWeapon.State.EMPTY,"empty not fabricated weapon");
        check(gate.accept(loginA,LiveWeapon.waiting()),"missing container is unknown");
        check(!gate.current().displayName().contains("No weapon"),"unknown not misread as empty");
        check(gate.accept(loginA,LiveWeapon.equipped(90001, "")),"unresolved real item ID");
        check(gate.current().displayName().contains("#90001"),"unknown name retained as ID");
        check(gate.accept(loginA,LiveWeapon.equipped(5, " null ")),"null-like item name");
        check(gate.current().displayName().contains("#5"),"unresolved names never guessed");
        long logout=gate.leaveLoggedIn();
        check(gate.current().state()==LiveWeapon.State.OFFLINE,"logout clears");
        check(!gate.accept(loginA,LiveWeapon.equipped(4587,"Stale")),"stale callback rejected");
        check(gate.isCurrent(logout),"logout fence active");
        long loginB=gate.enterLoggedIn();
        check(!gate.accept(loginA,LiveWeapon.equipped(1,"Old")),"old account cannot resurrect");
        check(gate.accept(loginB,LiveWeapon.equipped(12954,"Dragon defender")),"new login receives fresh result");
        long hop=gate.leaveLoggedIn();
        check(!gate.accept(loginB,LiveWeapon.equipped(12954,"Stale hop")),"hop event cannot resurrect");
        check(gate.isCurrent(hop),"world hop retired old epoch");
        long relog=gate.enterLoggedIn();
        check(gate.current().state()==LiveWeapon.State.WAITING,"relogin doesn't reuse weapon");
        check(gate.accept(relog,LiveWeapon.empty()),"new account can be unarmed");
        gate.stop();
        check(!gate.isCurrent(relog),"shutdown invalidates queued UI paints");
        check(gate.current().state()==LiveWeapon.State.OFFLINE,"shutdown clears state");
        boolean bad=false;
        try { LiveWeapon.equipped(-1,"fake"); } catch(IllegalArgumentException expected) {bad=true;}
        check(bad,"reject invalid equipped ID");
        BurrowModel model=new BurrowModel();
        model.chooseBoss("Vorkath");
        check(model.plan()!=null && model.plan().template().boss().equals("Vorkath"),"Vorkath uses its own verified plan");
        model.chooseBoss("Barrows");
        check(model.plan()!=null && model.plan().template().boss().equals("Barrows"),"Barrows uses its own verified plan");
        model.chooseBoss("Scurrius");
        Outfitter.Plan before = model.plan();
        AtomicReference<Throwable> problem = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    BurrowPanel panel=new BurrowPanel(model);
                    panel.setSize(225,730);
                    model.show(BurrowModel.View.OUTFITTER);
                    panel.setObservedWeapon(LiveWeapon.equipped(4587,"Dragon scimitar"));
                    check(panel.observedWeapon().itemId()==4587,"UI receives observed item");
                    check(before == model.plan(),"observation cannot replace plan");
                    check(model.bank()==null,"observation cannot mark bank verified");
                    BufferedImage img=new BufferedImage(225,730,BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g=img.createGraphics();panel.paint(g);g.dispose();
                    check(img.getRGB(20,290)!=0,"UI rendering available");
                    panel.setObservedWeapon(LiveWeapon.offline());
                    check(panel.observedWeapon().state()==LiveWeapon.State.OFFLINE,"UI cleared after logout");
                } catch(Throwable x) { problem.set(x); }
            });
        } catch(Exception x) { throw new AssertionError(x); }
        if (problem.get()!=null) throw new AssertionError(problem.get());
        System.out.println("PASS: BUR-0012 LiveWeaponTest " + checks + " direct checks (synthetic; not native RuneLite)");
    }
}
