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
        m.chooseBoss("Vorkath");yes(m.boss().equals("Vorkath"),"target selection");
        yes(Math.abs(BurrowModel.cumulativeChance(0.02,20)-0.3323920282449)<1e-6,"drop odds");
        var panel=new BurrowPanel(m);
        panel.clickAt(117,83);yes(m.view()==BurrowModel.View.OUTFITTER,"third top tab");
        panel.clickAt(35,239);yes(m.mode().equals("Best in Bank"),"mode action");
        panel.clickAt(35,585);yes(m.plan().bankVerified(),"manual fixture demo scan");
        panel.clickAt(171,83);yes(m.view()==BurrowModel.View.SESSIONS,"fourth top tab");
        panel.clickAt(60,248);yes(m.demoKills()==1,"session click route");
        panel.clickAt(60,290);yes(m.demoKills()==0,"session reset route");
        System.out.println("BURROW OFFLINE CORE PASS • " + checks + " direct assertions");
    }
}
