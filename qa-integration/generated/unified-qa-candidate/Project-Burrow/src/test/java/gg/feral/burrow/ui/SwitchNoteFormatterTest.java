package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.Final06ProfileMetadata;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** Focused, dependency-free Java17-compatible evidence for BUR-0037. */
public final class SwitchNoteFormatterTest {
    private static int checks;
    private static void require(boolean ok, String reason) { checks++;if(!ok)throw new AssertionError(reason); }
    private static String rendered(List<String> items){return String.join(" | ",items);}
    private static String sample(String boss, Tier tier) {
        var profile=VerifiedOutfitterCatalog.profiles(boss).get(0);
        for(var p:VerifiedOutfitterCatalog.profiles(boss)) {
            var meta=Final06ProfileMetadata.find(boss,p.method(),tier);
            if(meta!=null && !meta.switches().isEmpty())
                return rendered(SwitchNoteFormatter.formatSwitches(meta.switches(),p));
        }
        throw new AssertionError("No source switch for "+boss+" "+tier);
    }
    private static void run(Path evidence) throws Exception {
        System.out.println("SCURRIUS OPTIMAL: "+sample("Scurrius", Tier.OPTIMAL));
        String scurrius=sample("Scurrius", Tier.OPTIMAL);
        require(scurrius.contains("Summoned rats"),"Scurrius role");
        require(scurrius.contains("Bone mace (ID 28792)"),"Scurrius verified item name");
        require(scurrius.contains("Free inventory spaces required by plan: 1"),"Scurrius capacity");
        require(scurrius.contains("rat attack cooldown")&&scurrius.contains("displaced items"),"Scurrius warnings and steps");
        require(!scurrius.contains("{\"")&&!scurrius.contains("capacity_required"),"Scurrius raw JSON leak");
        String muspah=sample("Phantom Muspah",Tier.RECOMMENDED);
        System.out.println("MUSPAH RECOMMENDED: "+muspah);
        require(muspah.contains("Prayer shield")&&muspah.contains("Equipment slots to vacate: weapon, ammo"),"Muspah shield slots");
        require(muspah.contains("2"),"Muspah capacity");
        // KBD source has no structured switch in the current 51-profile dataset:
        // preserve its on-panel risk notes; do not manufacture a switch.
        for(var p:VerifiedOutfitterCatalog.profiles("King Black Dragon")) {
            var meta=Final06ProfileMetadata.find("King Black Dragon",p.method(),Tier.RECOMMENDED);
            require(meta!=null,"KBD metadata present");
            require(SwitchNoteFormatter.formatSwitches(meta.switches(),p).isEmpty(),"KBD cannot gain made-up switches");
        }
        String kbd=String.join(" | ",VerifiedOutfitterCatalog.profiles("King Black Dragon").get(0).notes());
        require(!kbd.isBlank(),"KBD notes remain intact");
        System.out.println("KBD: no structured switch entry in current source; normal warning/notes unchanged");
        var active=VerifiedOutfitterCatalog.profiles("Scurrius").get(0);
        String unknown=rendered(SwitchNoteFormatter.formatSwitches(List.of(
            "{\"role\":\"Unreviewed phase\",\"new_warning\":\"Do not proceed before checking attacks\",\"custom\":{\"unsafe\":true},\"condition\":\"Stay clear of hazards\"}"),active));
        require(unknown.contains("new_warning")&&unknown.contains("Do not proceed before checking attacks"),"Unknown warning preserved");
        require(unknown.contains("unsafe: true")&&unknown.contains("Stay clear of hazards"),"Nested and known warnings preserved");
        System.out.println("UNKNOWN FIELDS: "+unknown);
        String malformed=rendered(SwitchNoteFormatter.formatSwitches(List.of("{\"role\":\"Bad\",\"condition\":\"Do not enter\""),active));
        require(malformed.contains("Unverified switch data")&&malformed.contains("Do not enter")&&malformed.contains("Do not rely"),"Malformed risk warning retained");
        System.out.println("MALFORMED: "+malformed);
        require(rendered(SwitchNoteFormatter.formatSwitches(List.of("Switch at boss HP threshold"),active))
            .equals("Switch at boss HP threshold"),"Plain note unchanged");
        require(rendered(SwitchNoteFormatter.formatSwitches(List.of("{\"role\":\"A\",\"role\":\"B\"}"),active))
            .contains("Unverified switch data"),"Duplicate JSON keys rejected");
        require(rendered(SwitchNoteFormatter.formatSwitches(List.of("{\"role\":\"A\",\"condition\":\"Use \\\"quoted\\\" warning\"}"),active))
            .contains("Use \"quoted\" warning"),"Escaped warning preserved");
        BurrowModel model=new BurrowModel();model.show(BurrowModel.View.OUTFITTER);
        BurrowPanel panel=new BurrowPanel(model);
        var fm=panel.getFontMetrics(new Font("Dialog",Font.PLAIN,10));
        int profiles=0,structured=0;
        for(String boss:VerifiedOutfitterCatalog.BOSSES) {
            model.chooseBoss(boss);
            for(String method:model.methods()) {
                model.chooseMethod(method);
                for(Tier tier:Tier.values()) {
                    model.chooseTier(tier);
                    var p=model.profile();if(p==null)continue;
                    var meta=Final06ProfileMetadata.find(boss,method,tier);
                    List<String> switches=meta==null?List.of():SwitchNoteFormatter.formatSwitches(meta.switches(),p);
                    var layout=PreparationNotesLayout.layout(p.notes(),switches,fm,183);
                    for(var line:layout.lines()) {
                        require(fm.stringWidth(line.text())<=183,"225px panel text overflow: "+boss+" "+tier);
                        require(!line.text().contains("capacity_required")&&!line.text().contains("inventory_item_ids"),"JSON key displayed: "+boss+" "+tier);
                    }
                    if(meta!=null&&!meta.switches().isEmpty())structured++;
                    require(panel.getPreferredSize().width==225,"Panel width 225");
                    require(panel.getPreferredSize().height>=65+(boss.equals("King Black Dragon")?72:0)+1014+layout.height()+12,"scroll extent "+boss+" "+tier);
                    profiles++;
                }
            }
        }
        require(profiles==60,"all 51 fixed baseline plus nine research drafts");
        require(structured>0,"Real structured switches inspected");
        model.chooseBoss("Scurrius");model.chooseTier(Tier.OPTIMAL);
        panel.setPreferredSize(panel.getPreferredSize());
        JScrollPane scroll=new JScrollPane(panel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setSize(225,560);scroll.doLayout();
        scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
        scroll.doLayout();
        require(scroll.getVerticalScrollBar().getMaximum()>scroll.getViewport().getHeight(),"Real scrollbar required");
        require(scroll.getViewport().getViewPosition().y>0,"Viewport scrolled vertically");
        require(scroll.getHorizontalScrollBarPolicy()==JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,"No horizontal scrollbar");
        // Headless JScrollPane has no native peer; paint the *real panel* at
        // the measured JScrollPane viewport offset instead of capturing an
        // unrealized/blank Swing container. The scroll assertions above are
        // against the actual JScrollPane component.
        panel.setSize(panel.getPreferredSize());
        BufferedImage page=new BufferedImage(225,panel.getHeight(),BufferedImage.TYPE_INT_RGB);
        var pageGraphics=page.createGraphics();panel.paint(pageGraphics);pageGraphics.dispose();
        int scrollY=scroll.getViewport().getViewPosition().y;
        BufferedImage screenshot=new BufferedImage(225,560,BufferedImage.TYPE_INT_RGB);
        var g=screenshot.createGraphics();
        g.drawImage(page,0,0,225,560,0,scrollY,225,scrollY+560,null);
        g.dispose();
        require(scrollY+560>=panel.getHeight()-20,"Screenshot captures final instructions/footer");
        Files.createDirectories(evidence);ImageIO.write(screenshot,"png",evidence.resolve("BUR-0037-SCURRIUS-225PX-SCROLL.png").toFile());
        System.out.println("BUR-0037 FOCUSED OFFLINE PASS: "+checks+" checks; profiles="+profiles+
            "; structured profiles="+structured+"; 225px Swing scroll capture saved");
    }
    public static void main(String[] args) throws Exception {
        Path target=Path.of(args.length>0?args[0]:"/tmp/bur0037-evidence");
        SwingUtilities.invokeAndWait(()->{
            try{run(target);}catch(Exception e){throw new RuntimeException(e);}
        });
    }
}
