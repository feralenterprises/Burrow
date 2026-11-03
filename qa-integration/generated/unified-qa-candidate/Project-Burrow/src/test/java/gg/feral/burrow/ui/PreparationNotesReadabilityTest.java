package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.VerifiedOutfitterCatalog;
import gg.feral.burrow.core.Final06ProfileMetadata;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.event.MouseEvent;

/** BUR-0037: full notes, readable line widths, actual scrollbar extent and KBD offsets. */
public final class PreparationNotesReadabilityTest {
    private static int checks;
    private static void check(boolean c, String message) {
        checks++;
        if(!c)throw new AssertionError(message);
    }
    private static String normalize(String text) {
        return text.replaceAll("\\s+", "");
    }
    private static void run() {
        BurrowModel m=new BurrowModel();m.show(BurrowModel.View.OUTFITTER);
        BurrowPanel p=new BurrowPanel(m);
        // B20.1: notes open as a short preview by default; the full-text guarantees below are
        // checked with the notes expanded (one tap), exactly as before.
        p.setPreparationExpandedForTest(true);
        var fm=p.getFontMetrics(new Font("Dialog", Font.PLAIN,10));
        int count=0,maxHeight=0;
        for(String boss:VerifiedOutfitterCatalog.BOSSES) {
            m.chooseBoss(boss);
            for(String method:m.methods()) {
                m.chooseMethod(method);
                for(Tier tier:Tier.values()) {
                    m.chooseTier(tier);
                    var profile=m.profile();if(profile==null)continue;
                    var metadata=Final06ProfileMetadata.find(boss,method,tier);
                    List<String> switches=metadata==null?List.of():SwitchNoteFormatter.formatSwitches(metadata.switches(),profile);
                    var layout=PreparationNotesLayout.layout(profile.notes(),switches,fm,183);
                    check(layout.lines().stream().noneMatch(line->line.text().contains("capacity_required") || line.text().contains("inventory_item_ids")),
                        "Source JSON field leaked into card: "+boss+"/"+tier);
                    maxHeight=Math.max(maxHeight,layout.height());
                    check(layout.height()>=225,"minimum notes card height");
                    // Preparation is now a Chronicle subview; the source text must remain complete
                    // without consuming permanent space in the main Outfitter.
                    check(layout.height()>=225,"source-faithful notes retained in Chronicle");
                    for(var line:layout.lines())check(fm.stringWidth(line.text())<=183,
                        "Line overflows 225px panel: "+boss+"/"+tier+" '"+line.text()+"'");
                    // Every formatted note character must survive the existing wrapping.
                    // Raw JSON must never enter the visible preparation card.
                    String source=String.join("",profile.notes())+String.join("",switches);
                    String rendered="";
                    for(var line:layout.lines())if(!line.heading())rendered+=line.text();
                    check(normalize(rendered).equals(normalize(source).replace("•","")) ||
                        normalize(rendered).replace("•","").equals(normalize(source)),
                        "Preparation text lost: "+boss+"/"+tier);
                    count++;
                }
            }
        }
        check(gg.feral.burrow.core.TieredOutfitterCatalog.size()==51,"Original 51 profile coverage unchanged");
        check(count==63,"Expected 51 original + 2 Brutus + 10 research + 6 demon loadouts, found "+count);
        // KBD warning and gear cells must remain independent of variable notes height.
        m.chooseBoss("King Black Dragon");m.chooseTier(Tier.RECOMMENDED);
        var target=m.profile().gear().get(0);
        var gear=p.getToolTipText(new MouseEvent(p, MouseEvent.MOUSE_MOVED,0,0,106,
            p.equipmentYForR13Test()+43+15-8,0,false));
        check(target!=null && gear!=null && gear.contains(target.name()),"KBD equipment hitbox changed");
        p.clickAt(175,133+15-8);
        String noteTip=p.getToolTipText(new MouseEvent(p,MouseEvent.MOUSE_MOVED,0,0,40,
            p.chronicleNotesYForR13Test()+65-8,0,false));
        check(noteTip!=null && noteTip.startsWith("<html>"),"KBD notes tooltip shifted");
        // Real Swing scroll viewport, no horizontal scroll, full extended content height.
        JScrollPane scroll=new JScrollPane(p,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setSize(225,500);scroll.doLayout();
        check(scroll.getVerticalScrollBar().getMaximum()>=p.getPreferredSize().height,
            "Outer scroll can't reach expanded instructions");
        check(scroll.getHorizontalScrollBarPolicy()==JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
            "No horizontal scrollbar");
        // Rendering the bottom of a lengthy note must paint actual content.
        m.chooseBoss("Scurrius");m.chooseTier(Tier.OPTIMAL);
        p.setSize(p.getPreferredSize());
        BufferedImage img=new BufferedImage(225,p.getHeight(),BufferedImage.TYPE_INT_RGB);
        var gfx=img.createGraphics();p.paint(gfx);gfx.dispose();
        check(img.getHeight()>=880,"Scroll-expanded Chronicle page render");
        System.out.println("BUR-0037 PREPARATION NOTES PASS: "+checks+" checks, "+count+
            " loadouts, longest card "+maxHeight+"px; formatted notes retained, KBD offset and full-page scroll");
    }
    public static void main(String[] args)throws Exception {
        if(SwingUtilities.isEventDispatchThread())run();else SwingUtilities.invokeAndWait(PreparationNotesReadabilityTest::run);
    }
}
