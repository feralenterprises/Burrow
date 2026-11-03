package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.TieredOutfitterCatalog;
import gg.feral.burrow.core.VerifiedOutfitterCatalog;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

/** BUR-0040: actual mouse-release routing, sourced method coverage and visible rejection. */
public final class OutfitterStyleInteractionTest {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static void leftClick(BurrowPanel panel, int x, int y) {
        panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,
            System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK,
            x, y, 1, false, MouseEvent.BUTTON1));
    }
    private static String tip(BurrowPanel panel, int x, int y) {
        return panel.getToolTipText(new MouseEvent(panel, MouseEvent.MOUSE_MOVED,
            System.currentTimeMillis(), 0, x, y, 0, false));
    }
    private static void test() {
        BurrowModel model=new BurrowModel();
        BurrowPanel panel=new BurrowPanel(model);
        panel.setSize(BurrowPanel.WIDTH, BurrowPanel.HEIGHT);
        model.show(BurrowModel.View.OUTFITTER);
        model.chooseBoss("Dagannoth Kings");
        model.chooseTier(Tier.OPTIMAL);
        String[] styles={"Melee","Ranged","Magic"};
        String[] methods={"Supreme, melee","Prime, ranged","Rex safespot with Guthan healing switch"};
        for(int i=0;i<3;i++) {
            leftClick(panel,105+42*i,panel.equipmentYForR13Test()+16-8);
            check(model.method().equals(methods[i]),"DK " + styles[i] + " click chooses matching method");
            check(model.selectedStyle().equals(styles[i]),"DK " + styles[i] + " style active");
            check(model.tier()==Tier.OPTIMAL && model.boss().equals("Dagannoth Kings"),"preserves DK boss/tier");
            check(model.profile()!=null && model.plan()!=null,"DK backed by verified profile");
            check(panel.styleFeedbackForTest()==null,"successful selection clears warning");
        }
        model.chooseBoss("Scurrius");
        model.chooseTier(Tier.ENTRY);
        var before=model.plan();
        var methodBefore=model.method();
        leftClick(panel,147,panel.equipmentYForR13Test()+16-8); // W07.5: source-backed, provisional Ranged gear
        check(model.profile()!=null && model.plan()!=null && model.selectedStyle().equals("Ranged"),
            "W07.5 Ranged shows an actual research gear example");
        check(model.tier()==Tier.RECOMMENDED && model.boss().equals("Scurrius"),
            "Ranged moves to the only documented gear tier instead of inventing Entry");
        check(panel.styleFeedbackForTest()!=null && panel.styleFeedbackForTest().contains("RESEARCH GEAR"),
            "Ranged preview must disclose provisional bank gate");
        check(tip(panel,147,panel.equipmentYForR13Test()+16-8).contains("Ranged"),"research gear tooltip identifies style");
        leftClick(panel,189,panel.equipmentYForR13Test()+16-8);
        check(model.selectedStyle().equals("Magic") && model.profile()!=null && model.plan()!=null,
            "W07.5 Magic shows an actual research gear example");
        check(panel.styleFeedbackForTest()!=null && panel.styleFeedbackForTest().contains("RESEARCH GEAR"),
            "Magic preview must disclose provisional bank gate");
        leftClick(panel,105,panel.equipmentYForR13Test()+16-8);
        check(panel.styleFeedbackForTest()==null,"original supported method selected");
        check(model.profile()!=null,"original gear returns after provisional research style");
        leftClick(panel,147,panel.equipmentYForR13Test()+16-8);
        leftClick(panel,101,panel.tierYForR13Test()+12-8);
        check(model.tier()==Tier.RECOMMENDED && panel.styleFeedbackForTest()==null,"tier changes clear stale warning");
        leftClick(panel,189,panel.equipmentYForR13Test()+16-8);
        leftClick(panel,35,133+14-8); // next boss Brutus (existing boss hitbox)
        check(model.boss().equals("Brutus") && panel.styleFeedbackForTest()==null,"boss change clears stale warning");
        model.chooseBoss("Dagannoth Kings");
        leftClick(panel,189,panel.equipmentYForR13Test()+16-8);
        check(tip(panel,189,panel.equipmentYForR13Test()+16-8).contains("Magic"),"active style tooltip identifies selected method");
        check(tip(panel,105,panel.equipmentYForR13Test()+16-8).contains("Switch to Melee"),"supported alternative tooltip offers switch");
        String selected=model.method();
        panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_RELEASED,
            System.currentTimeMillis(), InputEvent.BUTTON3_DOWN_MASK,105,panel.equipmentYForR13Test()+16-8,1,false,MouseEvent.BUTTON3));
        check(model.method().equals(selected),"right click cannot select style");
        // Confirm entire catalog's actual method/style coverage is respected with real mouse events.
        int supported=0, unsupported=0;
        for(String boss:VerifiedOutfitterCatalog.BOSSES) {
            model.chooseBoss(boss);
            model.chooseTier(Tier.RECOMMENDED);
            for(int i=0;i<3;i++) {
                var choice=gg.feral.burrow.core.StyleMethodPicker.choose(boss,
                    gg.feral.burrow.core.CombatStyle.values()[i],model.method(),model.tier());
                String expected=choice==null?null:choice.method();
                var previous=model.plan();
                var previousMethod=model.method();
                leftClick(panel,105+42*i,panel.equipmentYForR13Test()+16-8);
                if(expected!=null) {
                    supported++;
                    check(model.method().equals(expected),boss+" "+styles[i]+" selects researched method");
                    check(model.selectedStyles().uses(gg.feral.burrow.core.CombatStyle.values()[i]),
                        "selected researched method genuinely uses requested combat style");
                    check(model.profile()!=null || gg.feral.burrow.core.ResearchCombatMethods.find(boss,model.method())!=null,
                          "either real profile or explicit research-only strategy");
                    if(model.profile()==null)
                        check(panel.styleFeedbackForTest()!=null && panel.styleFeedbackForTest().contains("STRATEGY ONLY"),
                            "strategy-only style must disclose missing full gear");
                    else if(gg.feral.burrow.core.StyleMethodPicker.isProvisional(boss,model.method(),model.tier()))
                        check(panel.styleFeedbackForTest()!=null && panel.styleFeedbackForTest().contains("RESEARCH GEAR"),
                            "provisional gear cannot masquerade as a bank-approved recommendation");
                    else check(panel.styleFeedbackForTest()==null,"catalog gear selection clears warning");
                } else {
                    unsupported++;
                    check(model.method().equals(previousMethod)&&model.plan()==previous,"no manufactured method for " + boss + " " + styles[i]);
                    check(panel.styleFeedbackForTest()!=null&&panel.styleFeedbackForTest().contains(styles[i]),"clear rejected style");
                }
                check(model.boss().equals(boss)&&model.tier()==Tier.RECOMMENDED,"boss and tier preserved");
            }
        }
        check(supported>=19 && supported+unsupported==45,"existing method/style combinations preserved, research adds coverage");
        // Headless full paint check exercises both the disabled and selected control labels.
        BufferedImage image=new BufferedImage(BurrowPanel.WIDTH,BurrowPanel.HEIGHT,BufferedImage.TYPE_INT_ARGB);
        panel.paint(image.createGraphics());
        check(image.getRGB(35,350)!=0,"style buttons render");
        System.out.println("BUR-0040 STYLE INTERACTION PASS: " + checks +
            " assertions; existing and new researched style choices verified");
    }
    public static void main(String[] args) throws Exception {
        if(SwingUtilities.isEventDispatchThread())test();
        else SwingUtilities.invokeAndWait(OutfitterStyleInteractionTest::test);
    }
}
