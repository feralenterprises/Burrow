package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

/** BUR-0019: dispatch actual ordinary mouse moves, never call getter as a shortcut. */
public final class ItemHoverNoModifierTest {
    private static int checks;
    private static void require(boolean valid,String label) {
        checks++;
        if(!valid)throw new AssertionError(label);
    }
    private static void move(BurrowPanel p,int x,int y) {
        p.dispatchEvent(new MouseEvent(p,MouseEvent.MOUSE_MOVED,
            System.currentTimeMillis(),0,x,y,0,false));
    }
    private static void exit(BurrowPanel p) {
        p.dispatchEvent(new MouseEvent(p,MouseEvent.MOUSE_EXITED,
            System.currentTimeMillis(),0,-1,-1,0,false));
    }
    private static void run() {
        BurrowModel m=new BurrowModel();
        BurrowPanel p=new BurrowPanel(m);
        p.setSize(BurrowPanel.WIDTH,BurrowPanel.HEIGHT);
        m.show(BurrowModel.View.OUTFITTER);
        int[] cols={1,0,1,2,0,1,2,1,0,1,2};
        int[] rows={0,1,1,1,2,2,2,3,4,4,4};
        int profiles=0,empty=0,occupied=0;
        for(String boss:VerifiedOutfitterCatalog.BOSSES) {
            m.chooseBoss(boss);
            for(String method:m.methods()) {
                m.chooseMethod(method);
                for(Tier tier:Tier.values()) {
                    m.chooseTier(tier);
                    var profile=m.profile();
                    if(profile==null)continue;
                    profiles++;
                    int shift=-8;
                    for(int i=0;i<11;i++) {
                        var item=profile.gear().get(i);
                        int x=29+cols[i]*62+15, y=shift+p.equipmentYForR13Test()+43+rows[i]*46+15;
                        move(p,x,y);
                        String actual=p.hoveredItemTextForTest();
                        if(item==null) {
                            require(actual==null,"Empty gear slot must be silent: "+boss+" "+i);empty++;
                            require(p.getToolTipText(new MouseEvent(p,MouseEvent.MOUSE_MOVED,0,0,x,y,0,false))==null,
                                "Empty gear native tooltip must be silent");
                        } else {
                            require(actual!=null && actual.contains(item.name()),"Gear missing hover: "+boss+" "+tier+" "+i+" "+actual);occupied++;
                        }
                    }
                    for(int i=0;i<28;i++) {
                        var item=profile.inventory().get(i);
                        int x=16+(i%4)*49+15, y=shift+p.equipmentYForR13Test()+281+31+(i/4)*42+15;
                        move(p,x,y);
                        String actual=p.hoveredItemTextForTest();
                        if(item==null) {
                            require(actual==null,"Empty inventory slot must be silent: "+boss+" "+i);empty++;
                            require(p.getToolTipText(new MouseEvent(p,MouseEvent.MOUSE_MOVED,0,0,x,y,0,false))==null,
                                "Empty inventory native tooltip must be silent");
                        } else {
                            require(actual!=null && actual.contains(item.name()),"Inventory missing hover: "+boss+" "+tier+" "+i);occupied++;
                            if(item.amount()>1)require(actual.contains("× "+item.amount()),"Stack count missing");
                        }
                    }
                }
            }
        }
        require(gg.feral.burrow.core.TieredOutfitterCatalog.size()==51,"Original 51 baseline unchanged");
        require(profiles==63,"All original, Brutus, Wave 7.5 research and demon profiles checked: "+profiles);
        m.chooseBoss("King Black Dragon");m.chooseTier(Tier.RECOMMENDED);
        move(p,4,390);
        require(p.hoveredItemTextForTest()==null,"Moving off slot must dismiss hover");
        BufferedImage before=new BufferedImage(225,1390,BufferedImage.TYPE_INT_ARGB);
        var noHoverGraphics=before.createGraphics();p.paint(noHoverGraphics);noHoverGraphics.dispose();
        move(p,106,-8+p.equipmentYForR13Test()+43+15);
        require(p.hoveredItemTextForTest()!=null,"KBD offset hover must show");
        BufferedImage after=new BufferedImage(225,1390,BufferedImage.TYPE_INT_ARGB);
        var hoverGraphics=after.createGraphics();p.paint(hoverGraphics);hoverGraphics.dispose();
        int differences=0;
        for(int y=p.equipmentYForR13Test()+25;y<p.equipmentYForR13Test()+85;y++)for(int x=5;x<220;x++) if(before.getRGB(x,y)!=after.getRGB(x,y))differences++;
        require(differences>80,"Normal mouse movement must paint a visible item-name label");
        exit(p);require(p.hoveredItemTextForTest()==null,"Mouse exit clears hover");
        p.dispatchEvent(new MouseEvent(p,MouseEvent.MOUSE_ENTERED,
            System.currentTimeMillis(),0,106,-8+p.equipmentYForR13Test()+43+15,0,false));
        require(p.hoveredItemTextForTest()!=null,"Normal mouse entry into an item shows name");
        p.dispatchEvent(new MouseEvent(p,MouseEvent.MOUSE_DRAGGED,
            System.currentTimeMillis(),0,107,-8+p.equipmentYForR13Test()+43+16,0,false));
        require(p.hoveredItemTextForTest()==null,"Dragging dismisses passive item label");
        move(p,106,-8+p.equipmentYForR13Test()+43+15);m.show(BurrowModel.View.HOME);
        require(p.hoveredItemTextForTest()==null,"Changing view must hide Outfitter hover");
        System.out.println("BUR-0019 NO-MODIFIER ITEM HOVER PASS: "+checks+
            " assertions, "+profiles+" loadouts, "+occupied+" filled slots, "+empty+" empty slots");
    }
    public static void main(String[] args) throws Exception {
        if(SwingUtilities.isEventDispatchThread())run();
        else SwingUtilities.invokeAndWait(ItemHoverNoModifierTest::run);
    }
}
