package gg.feral.burrow.core;

import gg.feral.burrow.core.VerifiedOutfitterCatalog.*;
import java.util.*;
import gg.feral.burrow.ui.BurrowPanel;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;

/** Independent sample oracles and selection fencing for the R04 catalog. */
public final class BurrowBossPlanTest {
    private static int checks;
    private static void check(boolean yes,String why) { checks++; if(!yes)throw new AssertionError(why); }
    public static void main(String[] args) {
        BurrowModel model=new BurrowModel();
        check(VerifiedOutfitterCatalog.BOSSES.size()==15,"fifteen boss selectors");
        int verified=0;
        for(String boss:VerifiedOutfitterCatalog.BOSSES) {
            model.chooseBoss(boss);
            List<Profile> profiles=VerifiedOutfitterCatalog.profiles(boss);
            if(profiles.isEmpty()) {
                check(model.plan()!=null && model.plan().template().boss().equals(boss),"new curated boss plan: "+boss);
                continue;
            }
            verified++;
            Profile p=profiles.get(0);
            check(model.plan()!=null && model.plan().template().boss().equals(boss),"boss provenance: "+boss);
            check(model.plan().gear().size()==11 && model.plan().inventory().size()==28,"geometry: "+boss);
            check(p.gear().size()==11 && p.inventory().size()==28,"literal slots: "+boss);
            check(p.source().contains("oldschool.runescape.wiki") && p.source().contains("oldid="),"pinned source: "+boss);
            for(int i=0;i<28;i++) {
                Item item=model.profile().inventory().get(i);
                var cell=model.plan().cells().get(Outfitter.inventoryPosition(i));
                check(item==null || item.id()<1 || item.amount()<1 || cell.requirement()!=null,
                      "known item retained at "+boss+":"+(i+1));
                if(item==null || item.id()<1 || item.amount()<1)check(cell.requirement()==null,"unresolved must not project to bank");
            }
        }
        check(verified==13,"thirteen complete official methods");
        check(VerifiedOutfitterCatalog.profiles("Obor").isEmpty(),"Obor incomplete");
        check(VerifiedOutfitterCatalog.profiles("Bryophyta").isEmpty(),"Bryophyta incomplete");
        model.chooseBoss("Dagannoth Kings");
        var rex=model.profile();
        check(rex.style().equals("Magic"),"Rex method");
        var originalRex=VerifiedOutfitterCatalog.find("Dagannoth Kings","Rex safespot with Guthan healing switch");
        for(int position:new int[]{4,8,11,12,16,20})
            check(originalRex.inventory().get(position-1).id()==5952,"R04 Rex Antidote++(4) preserved: "+position);
        check(rex.inventory().stream().filter(i->i!=null && i.id()==5952).count()==2,"Q07 short-trip Rex antidotes");
        check(rex.inventory().stream().noneMatch(i->i!=null && i.id()==5943),"wrong Antidote+ absent");
        try { model.chooseMethod("Prime"); throw new AssertionError("Prime borrowed Rex"); }
        catch(IllegalArgumentException expected) { checks++; }
        model.chooseBoss("Scurrius");
        var first=model.plan();
        model.chooseBoss("Vorkath");
        check(model.plan()!=first && model.plan().template().boss().equals("Vorkath"),"Vorkath own plan");
        check(model.profile().inventory().get(2).id()==12913,"F06 recommended four-dose anti-venom variant");
        check(VerifiedOutfitterCatalog.find("Vorkath","Ranged crossbow").inventory().get(3).id()<1,
            "original R04 unspecified variant remains preserved");
        model.chooseBoss("Obor");
        check(model.plan()!=null && model.plan().template().boss().equals("Obor"),"Obor own curated equipment");
        model.chooseBoss("Scurrius");
        check(model.plan()!=first,"return refreshes plan");
        model.chooseBoss("Phantom Muspah");
        check(model.profile().gear().get(4).name().equals("Bow of Faerdhinen (c)") &&
            model.profile().gear().get(6)==null,"two-handed bow leaves offhand empty");
        BufferedImage sprite=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
        sprite.setRGB(5,5,0xff23a676);
        BurrowPanel panel=new BurrowPanel(model,id->id==23971?sprite:null);
        model.show(BurrowModel.View.OUTFITTER);
        panel.setSize(BurrowPanel.WIDTH,BurrowPanel.HEIGHT);
        BufferedImage frame=new BufferedImage(BurrowPanel.WIDTH,BurrowPanel.HEIGHT,BufferedImage.TYPE_INT_ARGB);
        var graphics=frame.createGraphics();panel.paint(graphics);graphics.dispose();
        check(frame.getRGB(101,panel.equipmentYForR13Test()+42)==0xff23a676,"native sprite provider paints exact equipment cell");
        model.chooseBoss("Obor");
        frame=new BufferedImage(BurrowPanel.WIDTH,BurrowPanel.HEIGHT,BufferedImage.TYPE_INT_ARGB);
        graphics=frame.createGraphics();panel.paint(graphics);graphics.dispose();
        check(frame.getRGB(101,panel.equipmentYForR13Test()+42)!=0xff23a676,"boss switch clears old sprite");
        model.chooseBoss("Scurrius");
        String tip=panel.getToolTipText(new MouseEvent(panel,MouseEvent.MOUSE_MOVED,0,0,97,panel.equipmentYForR13Test()+50,0,false));
        check(tip.contains("Helm of Neitiznot") && tip.contains("10828"),"exact item hover identity");
        System.out.println("BUR-OUTFITTER R04 CATALOG PASS • "+checks+" assertions");
    }
}
