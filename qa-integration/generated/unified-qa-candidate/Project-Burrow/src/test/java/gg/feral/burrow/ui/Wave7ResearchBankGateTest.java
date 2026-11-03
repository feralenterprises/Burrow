package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import java.awt.event.*;
import javax.swing.*;

/** W07: provisional alternate-style gear may be viewed, never withdrawn through Bank O. */
public final class Wave7ResearchBankGateTest {
    static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
    static void test(){
        BurrowModel m=new BurrowModel();
        BurrowPanel panel=new BurrowPanel(m);
        panel.setSize(BurrowPanel.WIDTH,BurrowPanel.HEIGHT);
        m.show(BurrowModel.View.OUTFITTER);
        m.chooseBoss("Dagannoth Kings");
        m.chooseTier(TieredOutfitterCatalog.Tier.RECOMMENDED);
        panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_RELEASED,1,
            InputEvent.BUTTON1_DOWN_MASK,155,panel.sourceYForR13Test()+12-8,1,false,MouseEvent.BUTTON1));
        check(panel.bestBankToggleOnForHost(),"catalog gear should allow Best Bank host");
        m.chooseBoss("Brutus");
        m.chooseMethod("Ranged (free-to-play)");
        check(m.profile()!=null,"research Ranged preview gear must be selectable");
        check(!panel.bestBankToggleOnForHost() && !panel.isBankMatchView(),
            "provisional Ranged gear must not unlock Best Bank");
        m.chooseMethod("Magic (free-to-play)");
        check(!panel.bestBankToggleOnForHost(),"provisional Magic gear must not unlock Bank O");
        m.chooseMethod("Standard free-to-play");
        check(panel.bestBankToggleOnForHost(),"returning to catalog method restores deliberate bank toggle");
        m.chooseBoss("Scurrius");
        m.chooseMethod("Magic · rat-bone option");
        check(m.profile()!=null && StyleMethodPicker.isProvisional("Scurrius",m.method(),m.tier())
            && !panel.bestBankToggleOnForHost() && !panel.isBankMatchView(),
            "Wave 7.5 Scurrius research Magic gear must remain excluded from Best in Bank");
        System.out.println("Wave7ResearchBankGateTest PASS: catalog and Wave 7.5 research-gear bank boundaries");
    }
    public static void main(String[] args) throws Exception {
        if(SwingUtilities.isEventDispatchThread())test();else SwingUtilities.invokeAndWait(Wave7ResearchBankGateTest::test);
    }
}
