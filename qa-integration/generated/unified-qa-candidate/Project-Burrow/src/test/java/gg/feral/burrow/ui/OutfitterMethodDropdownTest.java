package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

/** BUR-0038: dropdown presents full registered methods without changing tiers or inventing methods. */
public final class OutfitterMethodDropdownTest {
    private static int checks;
    private static void check(boolean pass,String explanation) {
        checks++;if(!pass)throw new AssertionError(explanation);
    }
    private static void run() {
        BurrowModel model=new BurrowModel();
        model.show(BurrowModel.View.OUTFITTER);
        model.chooseBoss("Dagannoth Kings");
        model.chooseTier(Tier.OPTIMAL);
        BurrowPanel panel=new BurrowPanel(model);
        panel.setSize(225,1390);
        List<String> names=List.copyOf(model.methods());
        check(names.size()==4 && names.get(3).contains("Tribrid"),"DK three original methods plus researched tribrid");
        String first=model.method();
        JPopupMenu menu=panel.methodMenu();
        check(menu.getComponentCount()==names.size(),"menu contains only registered methods");
        check(menu.getPreferredSize().width>180,"long method displayed at native full width, not cropped to selector");
        for(int i=0;i<names.size();i++) {
            check(menu.getComponent(i) instanceof JRadioButtonMenuItem,"radio menu item");
            JRadioButtonMenuItem entry=(JRadioButtonMenuItem)menu.getComponent(i);
            check(names.get(i).equals(entry.getText()),"exact full method name, no truncation");
            check(entry.isSelected()==(i==0),"currently selected method indicated");
        }
        var initialProfile=model.profile();
        // Direct click routing no longer cycles the method behind a hidden button.
        panel.clickAt(90,133+39+12-8);
        check(model.method().equals(first)&&model.profile()==initialProfile,"no click-to-cycle");
        // Real event listener receives the mouse release; a non-displayed test panel
        // deliberately cannot show a Swing popup or change a method implicitly.
        panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(),InputEvent.BUTTON1_DOWN_MASK,90,133+39+12-8,1,false,MouseEvent.BUTTON1));
        check(model.method().equals(first),"mouse click never silently cycles method");
        // Selecting menu items executes the same handlers the player uses.
        for(int i=0;i<names.size();i++) {
            menu=panel.methodMenu();
            ((JRadioButtonMenuItem)menu.getComponent(i)).doClick();
            check(model.method().equals(names.get(i)),"menu picks exact strategy "+names.get(i));
            check(model.boss().equals("Dagannoth Kings")&&model.tier()==Tier.OPTIMAL,
                    "boss/tier unchanged after menu selection");
            if(i<3) check(model.profile()!=null&&model.plan()!=null&&model.profile().method().equals(names.get(i)),
                    "original full method has matching profile and plan");
            else check(model.profile()==null && model.plan()==null &&
                    gg.feral.burrow.core.ResearchCombatMethods.find(model.boss(),model.method())!=null,
                    "researched tribrid displays a strategy but does not invent gear");
            check(((JRadioButtonMenuItem)panel.methodMenu().getComponent(i)).isSelected(),
                    "active method radio check persists");
        }
        // A different boss with just one registered method has only one menu item.
        for(String boss:VerifiedOutfitterCatalog.BOSSES) {
            model.chooseBoss(boss);
            check(panel.methodMenu().getComponentCount()==model.methods().size(),"source backed menu "+boss);
            for(int i=0;i<model.methods().size();i++) {
                String method=model.methods().get(i);
                JRadioButtonMenuItem entry=(JRadioButtonMenuItem)panel.methodMenu().getComponent(i);
                check(method.equals(entry.getText()),"full exact boss method "+boss);
            }
        }
        model.chooseBoss("Dagannoth Kings");
        model.chooseTier(Tier.ENTRY);
        JPopupMenu rex=panel.methodMenu();
        ((JRadioButtonMenuItem)rex.getComponent(1)).doClick();
        check(model.tier()==Tier.ENTRY&&model.method().equals(names.get(1)),"Entry tier preserved");
        model.chooseTier(Tier.RECOMMENDED);
        ((JRadioButtonMenuItem)panel.methodMenu().getComponent(2)).doClick();
        check(model.tier()==Tier.RECOMMENDED&&model.method().equals(names.get(2)),"Recommended tier preserved");
        String help=panel.getToolTipText(new MouseEvent(panel,MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),0,90,133+39+12-8,0,false));
        check(help.contains(model.method())&&help.contains("Choose method"),"full method in accessible tooltip");
        System.out.println("BUR-0038 METHOD DROPDOWN PASS: "+checks+" assertions; full registered method names");
    }
    public static void main(String[] args) throws Exception {
        if(SwingUtilities.isEventDispatchThread())run();else SwingUtilities.invokeAndWait(OutfitterMethodDropdownTest::run);
    }
}
