package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.util.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** B20.1 Outfitter fixes at 225 px: tier-true method label, rare-drop chip/panel, wrapped rows, notes preview. */
final class B201OutfitterLayoutJUnitTest {
    static BurrowPanel panel(BurrowModel m){var p=new BurrowPanel(m);m.show(BurrowModel.View.OUTFITTER);p.setSize(225,p.getPreferredSize().height);return p;}
    static void paint(BurrowPanel p){p.setSize(225,p.getPreferredSize().height);var img=new BufferedImage(225,p.getPreferredSize().height,BufferedImage.TYPE_INT_ARGB);p.paint(img.getGraphics());}
    static String tip(BurrowPanel p,int x,int y){return p.getToolTipText(new MouseEvent(p,MouseEvent.MOUSE_MOVED,0,0,x,y,0,false));}

    @Test void methodRowDescribesTheTierActuallyShown() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel(); var p=panel(m);
            m.chooseBoss("Amoxliatl"); m.chooseTier(Tier.ENTRY);
            assertEquals("Melee, full Inquisitor with mace",m.method(),"Wiki method name itself is unchanged");
            assertFalse(p.methodRowLabel().contains("Inquisitor"),"no top-tier title over Entry gear: "+p.methodRowLabel());
            assertEquals("Melee · "+m.profile().gear().get(4).name(),p.methodRowLabel());
            assertTrue(p.methodCaption().startsWith("Entry gear shown"));
            // With no distinct special method, the full method is preserved in the catalog/menu
            // but not painted redundantly above the equipment.
            assertFalse(p.hasSpecialMethodsForR13Test());
            assertTrue(p.methodMenu().getComponentCount()>0);
            for(String boss:BurrowModel.availableBosses()) {
                m.chooseBoss(boss);
                for(String method:m.methods()) { m.chooseMethod(method);
                    for(Tier t:Tier.values()) { if(TieredOutfitterCatalog.find(boss,method,t)==null) continue; m.chooseTier(t);
                        var w=m.profile().gear().get(4);
                        if(w!=null) assertTrue(p.methodRowLabel().endsWith(w.name()),boss+"/"+method+"/"+t);
                        assertTrue(p.methodCaption().startsWith(t.label()),boss+"/"+t);
                    } }
            }
        });
    }
    @Test void rareDropChipOpensPanelAboveEquipmentAndClosesAgain() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel(); var p=panel(m); m.chooseBoss("Vorkath");
            int gearTop=p.equipmentYForR13Test();
            String weaponTipBefore=tip(p,35,gearTop+43+2*46+15-8);
            assertNotNull(weaponTipBefore);
            p.clickAt(150,p.rareDropChipClickYForTest());
            assertTrue(p.rareDropExpanded());
            assertEquals(gearTop,p.equipmentYForR13Test(),"Rare Drops follow inventory, never shift gear");
            assertEquals(weaponTipBefore,tip(p,35,gearTop+43+2*46+15-8),"equipment hitbox stays aligned");
            assertEquals(11,m.profile().gear().size()); assertEquals(28,m.profile().inventory().size());
            paint(p);
            p.clickAt(195,p.rareDropClickYForTest(12)); // ✕ in the panel header
            assertFalse(p.rareDropExpanded());
            assertEquals(gearTop,p.equipmentYForR13Test());
            m.chooseBoss("Callisto"); p.clickAt(150,p.rareDropChipClickYForTest());
            assertTrue(p.rareDropExpanded());
            assertTrue(p.rareDropRows().stream().anyMatch(r->r.text().contains("WILDERNESS")));
            assertFalse(BossRegistry.bestBankAllowed("Callisto"));
            paint(p);
        });
    }
    // R06: rows are cards (36px sprite box, bold 11pt name up to 124px, chance line, rate line).
    @Test void longDropNamesWrapInsteadOfClipping() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel(); var p=panel(m); m.chooseBoss("Phosani's Nightmare");
            p.setRareDropExpandedForTest(true);
            var rows=p.rareDropRows().stream().filter(r->r.kind()=='d').toList();
            assertFalse(rows.isEmpty());
            var fm=p.getFontMetrics(BurrowPanel.RD_NAME_FONT);
            for(var r:rows) {
                String name=r.text().split("\\|")[0];
                assertEquals(BurrowPanel.rdCardHeight(fm.stringWidth(name)>124?2:1),r.h(),"wide names get a second line: "+name);
            }
            // Every boss: rows follow the same rule (wider Windows fonts get two lines, never a cut).
            for(String boss:BurrowModel.availableBosses()) { m.chooseBoss(boss); p.setRareDropExpandedForTest(true);
                for(var r:p.rareDropRows()) if(r.kind()=='d') {
                    String name=r.text().split("\\|")[0];
                    assertEquals(BurrowPanel.rdCardHeight(fm.stringWidth(name)>124?2:1),r.h(),boss+": "+name);
                } }
            for(var r:rows) assertTrue(r.text().split("\\|")[1].length()>0,"rate always present");
            // Rows never overlap.
            for(int i=1;i<rows.size();i++) assertTrue(rows.get(i).y()>=rows.get(i-1).y()+rows.get(i-1).h());
            paint(p);
        });
    }
    @Test void preparationNotesPreviewWithOneTapFullText() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel(); var p=panel(m); m.chooseBoss("Amoxliatl"); m.chooseTier(Tier.ENTRY);
            assertFalse(p.preparationExpanded(),"default is the short preview");
            p.clickAt(175,133-8+15);
            assertTrue(p.isChronicleOpenForR13Test());
            int collapsed=p.getPreferredSize().height;
            p.setPreparationExpandedForTest(true);
            int expanded=p.getPreferredSize().height;
            assertTrue(expanded>collapsed,"full notes still reachable in Chronicle when expanded");
            p.setPreparationExpandedForTest(false);
            paint(p);
        });
    }
    @Test void lootEstimateAppearsInTrackerAsProvisionalNeverAsKc() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var m=new BurrowModel(); var p=panel(m); m.chooseBoss("Tormented Demons");
            var scope=new BurrowPanel.SessionScope("acct","login1");
            p.ingestSessionEvent(new BurrowPanel.SessionStart(scope,"s1",BurrowPanel.SessionOrigin.VERIFIED_BRIDGE));
            p.ingestSessionEvent(new BurrowPanel.SessionLootEncounter(scope,"e1",BurrowPanel.SessionOrigin.VERIFIED_BRIDGE,"Tormented Demons","test fixture"));
            p.ingestSessionEvent(new BurrowPanel.SessionLootEncounter(scope,"e1",BurrowPanel.SessionOrigin.VERIFIED_BRIDGE,"Tormented Demons","test fixture"));
            assertEquals(Map.of("Tormented Demons",1L),p.lootConfirmedEncounters(),"same event id once");
            Map<String,String> cfg=new HashMap<>(); var store=new RareDropStore(cfg::get,cfg::put);
            store.toggle(1L,"tormented_demons:29580");
            p.setRareDropState(1L,store.read(1L)); p.setRareDropExpandedForTest(true);
            var texts=p.rareDropRows().stream().map(BurrowPanel.RdRow::text).toList();
            assertTrue(texts.stream().anyMatch(t->t.startsWith("KC UNKNOWN")),"KC stays unknown: "+texts);
            assertTrue(texts.stream().anyMatch(t->t.startsWith("This login: 1 loot-confirmed kill")));
            assertTrue(texts.stream().anyMatch(t->t.startsWith("Provisional, this login only: 0.2%")),texts.toString());
            assertFalse(store.read(1L).effective("Tormented Demons").known(),"never written into KC store");
            p.ingestSessionEvent(new BurrowPanel.SessionStop(scope,"s2",BurrowPanel.SessionOrigin.VERIFIED_BRIDGE));
            assertTrue(p.lootConfirmedEncounters().isEmpty(),"logout clears the estimate");
        });
    }
}
