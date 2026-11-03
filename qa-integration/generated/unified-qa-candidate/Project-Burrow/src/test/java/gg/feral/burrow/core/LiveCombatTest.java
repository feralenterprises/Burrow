package gg.feral.burrow.core;

import gg.feral.burrow.ui.BurrowPanel;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

/** Direct offline state/EDT test; not genuine RuneLite host proof. */
public final class LiveCombatTest {
    private static int checks;
    private static void ok(boolean x, String what) { checks++; if (!x) throw new AssertionError(what); }
    public static void main(String[] args) throws Exception {
        ok(LiveCombat.offline().state()==LiveCombat.State.OFFLINE,"offline");
        ok(LiveCombat.offline().level(0)==-1,"offline redacted");
        ok(LiveCombat.waiting().state()==LiveCombat.State.WAITING,"waiting");
        ok(LiveCombat.fromRealLevels(null).state()==LiveCombat.State.WAITING,"null unknown");
        ok(LiveCombat.fromRealLevels(new int[6]).state()==LiveCombat.State.WAITING,"incomplete unknown");
        int[] data={75,82,76,88,90,79,70};
        LiveCombat snapshot=LiveCombat.fromRealLevels(data);
        ok(snapshot.state()==LiveCombat.State.OBSERVED,"observed");
        ok(snapshot.level(0)==75,"real attack");
        ok(snapshot.level(3)==88,"real HP");
        ok(snapshot.level(6)==70,"real prayer");
        data[0]=1;ok(snapshot.level(0)==75,"defensive immutable copy");
        ok(LiveCombat.fromRealLevels(new int[]{75,82,0,88,90,79,70}).state()==LiveCombat.State.WAITING,"zero invalid not fabricated");
        ok(LiveCombat.fromRealLevels(new int[]{75,82,-1,88,90,79,70}).state()==LiveCombat.State.WAITING,"negative invalid");
        ok(LiveCombat.fromRealLevels(new int[]{75,82,127,88,90,79,70}).state()==LiveCombat.State.WAITING,"invalid range");
        ok(snapshot.label(4).equals("90"),"live display");
        ok(LiveCombat.offline().label(4).equals("?"),"offline redaction");
        LiveWeaponGate gate=new LiveWeaponGate();gate.start();
        long a=gate.enterLoggedIn();ok(gate.canObserve(a),"login A current");
        long logout=gate.leaveLoggedIn();ok(!gate.canObserve(a),"logout retires old read");
        ok(gate.isCurrent(logout),"logout can paint cleared state");
        long b=gate.enterLoggedIn();ok(!gate.isCurrent(a),"account A cannot paint after B login");
        ok(gate.isCurrent(b),"account B active");
        gate.stop();ok(!gate.isCurrent(b),"plugin shutdown invalidates paint");
        AtomicReference<Throwable> problem=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->{
            try {
                BurrowModel model=new BurrowModel();
                BurrowPanel p=new BurrowPanel(model);p.setSize(225,730);
                p.setObservedCombat(snapshot);
                ok(p.observedCombat().level(0)==75,"home received real level");
                BufferedImage img=new BufferedImage(225,730,BufferedImage.TYPE_INT_ARGB);
                var g=img.createGraphics();p.paint(g);g.dispose();
                ok(img.getRGB(20,199)!=0,"home paints combat card");
                model.chooseBoss("Vorkath");ok(model.plan()!=null && model.plan().template().boss().equals("Vorkath"),"Vorkath plan provenance");
                model.chooseBoss("Barrows");ok(model.plan()!=null && model.plan().template().boss().equals("Barrows"),"Barrows plan provenance");
                p.setObservedCombat(LiveCombat.offline());
                ok(p.observedCombat().state()==LiveCombat.State.OFFLINE,"logout clears home");
                ok(p.observedCombat().label(0).equals("?"),"no stale level after logout");
                ok(model.bank()==null,"live levels never verify bank");
            } catch(Throwable t){problem.set(t);}
        });
        if (problem.get()!=null) throw new AssertionError(problem.get());
        System.out.println("PASS: BUR-0013 LiveCombatTest "+checks+" assertions (offline synthetic)");
    }
}
