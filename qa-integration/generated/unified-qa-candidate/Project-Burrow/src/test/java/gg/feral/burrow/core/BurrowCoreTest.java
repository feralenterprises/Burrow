package gg.feral.burrow.core;

import gg.feral.burrow.core.Outfitter.*;
import gg.feral.burrow.ui.BurrowPanel;
import java.util.*;
import javax.swing.*;

/** No JUnit or network required. Runs with javac --release 17 + java -ea. */
public final class BurrowCoreTest {
    private static int checks;
    static void yes(boolean value,String msg) {checks++;if(!value)throw new AssertionError(msg);}
    public static void main(String[] args) throws Exception {
        var scope=new Scope("account-test-A",1);
        var geometry=new HashSet<Integer>();
        for(int p:Outfitter.GEAR_POSITIONS) yes(geometry.add(p),"duplicate equipment");
        for(int i=0;i<28;i++) yes(geometry.add(Outfitter.inventoryPosition(i)),"overlap inventory");
        yes(geometry.size()==39,"39 used physical positions");
        yes(56-geometry.size()==17,"17 spacers");
        var noBank=Outfitter.resolve(1,scope,Outfitter.scurriusSample(),"Recommended",null);
        yes(!noBank.bankVerified(),"unverified without observation");
        yes(noBank.cells().size()==56,"56 tiles");
        yes(noBank.inventory().size()==28,"28 physical inventory cells");
        yes(noBank.gear().size()==11,"11 equipment cells");
        yes(noBank.cells().stream().filter(c->c.kind()==Kind.SPACE).count()==17,"17 physical spacer cells");
        yes(noBank.cells().stream().filter(c->c.stock()==Stock.UNVERIFIED).count()==19,"10 required gear + 8 supplies + 1 unresolved gear");
        var bank=Outfitter.sampleBank(scope);
        var verified=Outfitter.resolve(2,scope,Outfitter.scurriusSample(),"Best in Bank",bank);
        yes(verified.bankVerified(),"bank-bound");
        yes(verified.missing().size()==3,"one potion and two sharks missing");
        yes(verified.inventory().stream().filter(c->c.requirement()!=null && c.requirement().itemId()==2434 && c.allocated()==1).count()==3,"three actual potion allocations");
        yes(verified.inventory().stream().filter(c->c.requirement()!=null && c.requirement().itemId()==2434 && c.allocated()==0).count()==1,"fourth missing potion");
        var foreign=new Scope("account-test-B",1);
        try {Outfitter.resolve(3,foreign,Outfitter.scurriusSample(),"Recommended",bank);throw new AssertionError("foreign bank allowed");}
        catch(IllegalArgumentException expected){checks++;}
        var m=new BurrowModel();
        var first=m.plan();yes(first==m.plan(),"single plan object");
        m.loadDemoBank();yes(m.plan()!=first,"new resolution on bank change");
        yes(m.plan().cells()==m.plan().cells(),"one projection object");
        m.enterAccount(new Scope("other",7));yes(m.bank()==null,"wipe snapshot on account change");
        yes(!m.plan().bankVerified(),"no old stock on changed account");
        m.observeBank(new Snapshot(m.scope(),3,Map.of(2434,2)));
        try {m.observeBank(new Snapshot(m.scope(),3,Map.of(2434,1)));throw new AssertionError("old revision accepted");}
        catch(IllegalArgumentException expected){checks++;}
        m.chooseBoss("Vorkath");yes(m.boss().equals("Vorkath") && m.plan()!=null && "Vorkath".equals(m.plan().template().boss()),"verified Vorkath plan selected");
        m.chooseBoss("Scurrius");yes(m.plan()!=null && "Scurrius".equals(m.plan().template().boss()),"verified plan restored");
        yes(Math.abs(BurrowModel.cumulativeChance(0.02,20)-0.3323920282449)<1e-6,"drop odds");
        var panel=new BurrowPanel(m);
        panel.clickAt(137,94);yes(m.view()==BurrowModel.View.OUTFITTER,"third horizontal tab");
        panel.clickAt(180,panel.tierYForR13Test()+10-8);yes(m.tier()==TieredOutfitterCatalog.Tier.OPTIMAL && m.plan()!=null,"optimal tier selects own plan");
        panel.clickAt(82,panel.tierYForR13Test()+10-8);yes(m.tier()==TieredOutfitterCatalog.Tier.RECOMMENDED && m.plan()!=null,"recommended restores plan");
        yes(m.plan().bankVerified(),"existing injected bank observation remains scoped");
        panel.clickAt(191,94);yes(m.view()==BurrowModel.View.SESSIONS,"fourth horizontal tab");
        // Typed synthetic evidence is explicitly labelled; no Sessions screen tap
        // may create a kill, reset real session history, or mutate XP totals.
        var fixture=new BurrowPanel.SessionScope("synthetic_core_test","login_1");
        SwingUtilities.invokeAndWait(()->{
            yes(panel.ingestSessionEvent(new BurrowPanel.SessionStart(fixture,"core-start",
                BurrowPanel.SessionOrigin.SYNTHETIC_FIXTURE)),"fixture session start");
            yes(panel.ingestSessionEvent(new BurrowPanel.SessionXpGain(fixture,"core-xp",
                BurrowPanel.SessionOrigin.SYNTHETIC_FIXTURE,"Attack",42,"explicit synthetic fixture")),
                "fixture XP accepted only through typed ingestion");
        });
        var readOnly=panel.sessionReadout();
        yes(readOnly.synthetic() && readOnly.totalXp()==42 &&
            readOnly.kcByBoss().isEmpty(),"typed fixture visible, no fabricated boss KC");
        panel.clickAt(60,313);
        yes(m.view()==BurrowModel.View.SESSIONS &&
            panel.sessionReadout().equals(readOnly),"old kill hitbox cannot inject or reset activity");
        panel.clickAt(60,355);
        yes(m.view()==BurrowModel.View.SESSIONS &&
            panel.sessionReadout().equals(readOnly),"old reset hitbox cannot clear Sessions");
        yes(m.demoKills()==0,"legacy demo sentinel is permanently inactive");
        System.out.println("BURROW OFFLINE CORE PASS • " + checks + " direct assertions");
    }
}
