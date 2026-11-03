package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import gg.feral.burrow.pathfinder.SuggestionEngine;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Build 20 Rare Drop Tracker panel at 225 px. Synthetic account state (TEST FIXTURE). */
final class Build20RareDropPanelJUnitTest {
    static final long HASH=0xABCDL;
    static final class Rec implements BurrowPanel.RareDropAction {
        final List<String> calls=new ArrayList<>();
        public void toggle(long h,String k){calls.add("toggle "+h+" "+k);}
        public void mode(long h,String e,RareDropMath.Mode m){calls.add("mode "+e+" "+m);}
        public void manualKc(long h,String e,Long v){calls.add("manual "+e+" "+v);}
        public void hiscores(long h,String e){calls.add("hiscores "+e);}
    }
    static BurrowPanel outfitter(BurrowModel m,String boss){
        var p=new BurrowPanel(m); p.setSize(225,880);
        m.show(BurrowModel.View.OUTFITTER); m.chooseBoss(boss); p.setSize(p.getPreferredSize()); return p;
    }
    static int rowY(BurrowPanel p,char kind,String contains){
        for(var r:p.rareDropRows()) if(r.kind()==kind && (contains==null || r.text().contains(contains))) return r.y();
        throw new AssertionError("row "+kind+" "+contains+" in "+p.rareDropRows());
    }
    static void paint(BurrowPanel p){var img=new BufferedImage(225,p.getPreferredSize().height,BufferedImage.TYPE_INT_ARGB);p.paint(img.getGraphics());}

    @Test void signedOutShowsRatesButNeverSaves() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel(); var p=outfitter(m,"Vorkath"); var rec=new Rec(); p.setRareDropAction(rec);
            assertFalse(p.rareDropExpanded());
            p.clickAt(150,p.rareDropChipClickYForTest()); // B20.1: chip beside the Outfitter title
            assertTrue(p.rareDropExpanded(),"chip opens the Rare drops / KC panel");
            p.clickAt(40,p.rareDropClickYForTest(rowY(p,'d',"Draconic visage")+8));
            assertTrue(rec.calls.isEmpty(),"nothing saved while signed out");
            assertTrue(p.rareDropRows().stream().anyMatch(r->r.text().startsWith("Sign in")));
            assertTrue(p.rareDropRows().stream().anyMatch(r->r.text().contains("1/5,000")),"Wiki rate visible");
            paint(p);
        });
    }
    @Test void ownerActionsAreExplicitAndAccountScoped() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel(); var p=outfitter(m,"Vorkath"); var rec=new Rec(); p.setRareDropAction(rec);
            Map<String,String> cfg=new HashMap<>(); var store=new RareDropStore(cfg::get,cfg::put);
            var key=RareDropTable.encounter("Vorkath").orElseThrow().drops().stream().filter(d->d.name().equals("Draconic visage")).findFirst().orElseThrow().key();
            store.toggle(HASH,key);
            store.observe(HASH,CollectionLogParser.parse(List.of("Vorkath","Vorkath kills: 250"),List.of()).orElseThrow(),1);
            p.setRareDropState(HASH,store.read(HASH));
            p.setRareDropExpandedForTest(true);
            assertTrue(p.rareDropRows().stream().anyMatch(r->r.text().startsWith("KC 250 · COLLECTION LOG")),"observed KC labelled");
            assertTrue(p.rareDropRows().stream().anyMatch(r->r.text().startsWith("ANY of 1 selected · by KC 250: 4.9%")),
                "chance at genuine KC: "+p.rareDropRows());
            p.clickAt(40,p.rareDropClickYForTest(rowY(p,'d',"Skeletal visage")+8));
            assertEquals("toggle "+HASH+" vorkath:22006",rec.calls.get(0));
            // Feedback lines can push the button row down, so locate it before each click.
            p.setManualKcPromptForTest(e->"1,200"); p.clickAt(30,p.rareDropClickYForTest(rowY(p,'b',null)+10));
            p.setManualKcPromptForTest(e->"lots"); p.clickAt(30,p.rareDropClickYForTest(rowY(p,'b',null)+10));
            assertTrue(p.rareDropRows().stream().anyMatch(r->r.text().startsWith("Manual KC must be")),"bad input rejected, not saved");
            p.setManualKcPromptForTest(e->""); p.clickAt(30,p.rareDropClickYForTest(rowY(p,'b',null)+10));
            p.clickAt(110,p.rareDropClickYForTest(rowY(p,'b',null)+10));
            p.clickAt(170,p.rareDropClickYForTest(rowY(p,'b',null)+10));
            assertEquals(List.of("toggle "+HASH+" vorkath:22006","manual Vorkath 1200","manual Vorkath null","hiscores Vorkath","mode Vorkath ALL"),rec.calls);
            paint(p);
            p.setRareDropState(HASH,null);
            assertNull(p.rareDropStateForTest(),"logout clears the account view");
        });
    }
    @Test void wildernessAndUnresearchedBossesAreHonest() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var p=outfitter(new BurrowModel(),"King Black Dragon");
            p.setRareDropExpandedForTest(true);
            assertTrue(p.rareDropRows().stream().anyMatch(r->r.text().contains("WILDERNESS")));
            assertTrue(p.rareDropRows().stream().anyMatch(r->r.kind()=='d' && r.text().startsWith("Prince black dragon")),"odds only");
            assertFalse(BossRegistry.bestBankAllowed("King Black Dragon"),"tracker never re-enables Best in Bank");
            paint(p);
            var moon=outfitter(new BurrowModel(),"Blood Moon");
            moon.setRareDropExpandedForTest(true);
            // B20.1 R02 (owner): Moons are modelled by their Lunar Chest roll, one set of 4 per Moon.
            assertEquals(4,moon.rareDropRows().stream().filter(r->r.kind()=='d').count(),"Blood Moon set from the Lunar Chest");
            assertTrue(moon.rareDropRows().stream().anyMatch(r->r.text().contains("Lunar Chest")));
            var nightmare=outfitter(new BurrowModel(),"The Nightmare");
            nightmare.setRareDropExpandedForTest(true);
            assertTrue(nightmare.rareDropRows().stream().noneMatch(r->r.kind()=='d'),"no invented rates");
            var g=outfitter(new BurrowModel(),"General Graardor");
            g.setRareDropExpandedForTest(true);
            assertEquals(8,g.rareDropRows().stream().filter(r->r.kind()=='d').count(),"GWD uniques + pet on a discovery-only boss");
            paint(g);
            var dk=outfitter(new BurrowModel(),"Dagannoth Kings");
            dk.setRareDropExpandedForTest(true);
            assertEquals(3,dk.rareDropRows().stream().filter(r->r.kind()=='n').count(),"per-king KC sections");
        });
    }
    @Test void pathfinderShowsGoalsAndOpensOutfitter() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel(); var p=new BurrowPanel(m); p.setSize(225,880);
            p.clickAt(90,94);
            Map<String,String> cfg=new HashMap<>(); var store=new RareDropStore(cfg::get,cfg::put);
            store.toggle(HASH,"zulrah:12922");
            p.setRareDropState(HASH,store.read(HASH));
            p.setPathfinderSnapshot(Build20PathfinderControlsJUnitTestAccess.account());
            p.setSize(p.getPreferredSize());
            paint(p);
            p.clickPathfinderActionForTest("goal:0");
            assertEquals(BurrowModel.View.OUTFITTER,m.view());
            assertEquals("Zulrah",m.boss());
            assertTrue(p.rareDropExpanded());
        });
    }
}
