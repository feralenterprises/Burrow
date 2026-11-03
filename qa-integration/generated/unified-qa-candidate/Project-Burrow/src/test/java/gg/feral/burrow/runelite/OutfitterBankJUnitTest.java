package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BurrowModel;
import net.runelite.api.*;
import net.runelite.api.widgets.*;
import net.runelite.client.plugins.banktags.tabs.Layout;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OutfitterBankJUnitTest {
    static class Provider implements L01BankLayout.Provider {
        boolean enabled=true,conflict; String tag; Layout layout; int opens,closes,discards;
        Runnable duringOpen;
        public boolean available(){return enabled;} public String activeTag(){return tag;}
        public Layout activeLayout(){return layout;} public boolean nameInUse(String n){return conflict;}
        public void open(String n,Layout l){if(duringOpen!=null)duringOpen.run();tag=n;layout=l;opens++;}
        public void close(){tag=null;layout=null;closes++;}
        public void discard(){discards++;}
    }
    static L01LayoutSpecification spec() { return L01LayoutSpecification.from(new BurrowModel().plan()); }
    @Test void exactEquipmentAndSuppliesGeometryUsesBurrowPlan() {
        var model=new BurrowModel();var ids=L01LayoutSpecification.from(model.plan()).itemIds();
        assertEquals(56,ids.length);assertEquals(10828,ids[1]);assertEquals(28792,ids[16]);
        assertEquals(12954,ids[18]);assertEquals(11773,ids[34]);
        for(int i=0;i<11;i++) {
            var item=model.profile().gear().get(i);
            assertEquals(item==null?-1:item.id(),ids[gg.feral.burrow.core.Outfitter.GEAR_POSITIONS.get(i)]);
        }
        for(int i=0;i<28;i++) {
            var item=model.profile().inventory().get(i);
            assertEquals(item==null?-1:item.id(),ids[gg.feral.burrow.core.Outfitter.inventoryPosition(i)]);
        }
        for(int row=0;row<7;row++)assertEquals(-1,ids[row*8+3]);
        ids[1]=999;assertEquals(10828,spec().itemIds()[1]);
    }
    @Test void selectedBossNeverReusesScurriusAndSnapshotIsImmutable() {
        var m=new BurrowModel();var old=m.bankSelection();m.chooseBoss("Vorkath");
        assertEquals("Vorkath",m.bankSelection().plan().template().boss());assertEquals("Scurrius",old.boss());
        m.chooseBoss("Obor");assertEquals("Obor",m.bankSelection().plan().template().boss());
        m.chooseBoss("Bryophyta");assertEquals("Bryophyta",m.bankSelection().plan().template().boss());
        m.chooseBoss("Scurrius");assertNotSame(old,m.bankSelection());
    }
    @Test void demoStockNeverChangesNativeLayoutIdsOrClaimsOwnership() {
        var m=new BurrowModel();int[] before=L01LayoutSpecification.from(m.plan()).itemIds();
        m.loadDemoBank();assertArrayEquals(before,L01LayoutSpecification.from(m.plan()).itemIds());
    }
    @Test void opensClosesReopensAndCloseIsIdempotent() {
        var p=new Provider();var tab=new L01BankLayout(p,"burrow-test");
        tab.open(spec(),()->true);assertTrue(tab.selected());assertEquals(56,p.layout.size());
        tab.close();tab.close();assertEquals(1,p.closes);assertFalse(tab.selected());
        tab.open(spec(),()->true);assertEquals(2,p.opens);
    }
    @Test void foreignSameNameReplacementIsNeverClosed() {
        var p=new Provider();var tab=new L01BankLayout(p,"burrow-test");tab.open(spec(),()->true);
        p.layout=new Layout("burrow-test");var replacement=p.layout;tab.close();
        assertSame(replacement,p.layout);assertEquals(0,p.closes);assertEquals(1,p.discards);
    }
    @Test void bankTagsDisabledWhileOOpenStillReleasesEphemeralRegistration() {
        var p=new Provider();var tab=new L01BankLayout(p,"burrow-test");
        tab.open(spec(),()->true);p.enabled=false;
        tab.close();assertEquals(1,p.discards);assertEquals(0,p.closes);
    }
    @Test void disabledForeignConflictingAndStaleRequestsDoNotOpen() {
        var p=new Provider();var tab=new L01BankLayout(p,"burrow-test");
        p.enabled=false;assertThrows(IllegalStateException.class,()->tab.open(spec(),()->true));
        p.enabled=true;p.tag="foreign";assertThrows(IllegalStateException.class,()->tab.open(spec(),()->true));
        p.tag=null;p.conflict=true;assertThrows(IllegalStateException.class,()->tab.open(spec(),()->true));
        p.conflict=false;assertThrows(IllegalStateException.class,()->tab.open(spec(),()->false));
        int[] calls={0};assertThrows(IllegalStateException.class,()->tab.open(spec(),()->++calls[0]==1));
        assertEquals(0,p.opens);assertEquals(0,p.closes);
    }
    static class HeaderHarness {
        boolean open=true,logged=true;long press=1;
        net.runelite.api.ItemContainer bank; // BUR-0016: null = bank contents unknown
        BankHeaderFixture.Header header=new BankHeaderFixture.Header(()->true,()->open);
        Client client=BankHeaderFixture.proxy(Client.class,(m,a)->switch(m) {
            case "getGameState" -> logged?GameState.LOGGED_IN:GameState.LOGIN_SCREEN;
            case "getWidget" -> header.lookup((WidgetInfo)a[0]);
            case "getCanvasWidth" -> 1000; case "getCanvasHeight" -> 800;
            case "getMouseLastPressedMillis" -> press;
            case "addChatMessage" -> null;
            case "getItemContainer" -> bank;
            case "getItemDefinition" -> BankHeaderFixture.proxy(net.runelite.api.ItemComposition.class,
                    (key,args) -> switch(key) {
                        case "getId" -> (Integer)a[0];
                        case "getPlaceholderTemplateId", "getNote" -> -1;
                        default -> throw new AssertionError("Unexpected item definition: " + key);
                    });
            default -> throw new AssertionError("Unpermitted client operation: "+m);
        });
        BankWidgetAdapter adapter=new BankWidgetAdapter(client);
    }
    @Test void buttonInstallsOnceWithoutReplacingForeignSibling() {
        var h=new HeaderHarness();var neighbor=h.header.extra(new java.awt.Rectangle(700,9,20,20),"Foreign");
        var entry=h.adapter.refresh();assertNotNull(entry);
        for(int i=0;i<50;i++)assertSame(entry,h.adapter.refresh());
        assertEquals(1,h.header.attachedEntries());assertEquals("O",h.header.get(entry.child).text);
        assertEquals(0,h.header.get(entry.child).clickMask);
        h.adapter.clear();assertSame(neighbor.widget,h.header.root.children[neighbor.index]);
        assertEquals(0,h.header.attachedEntries());
    }
    @Test void onePhysicalPressOpAndClickDispatchOnceAndStaleCallbackIsRevoked() {
        var h=new HeaderHarness();var e=h.adapter.refresh();int[] count={0};h.adapter.bind(e,()->count[0]++);
        var callback=h.header.get(e.child).op;h.press++;
        var box=h.header.get(e.child).bounds();h.header.pointer(box.x+2,box.y+2,false);
        assertEquals(1,count[0]);h.adapter.clear();h.press++;
        ((JavaScriptCallback)callback[0]).run(BankHeaderFixture.proxy(ScriptEvent.class,(m,a)->m.equals("getSource")?e.child:1));
        assertEquals(1,count[0]);
    }
    @Test void bankCloseLogoutPinAndReplacedRootRevokeButton() {
        var h=new HeaderHarness();var old=h.adapter.refresh();h.header.rebuild();
        var replacement=h.adapter.refresh();assertNotSame(old,replacement);
        assertTrue(h.header.get(old.child).hidden);
        h.open=false;assertNull(h.adapter.refresh());h.open=true;assertNotNull(h.adapter.refresh());
        h.logged=false;assertNull(h.adapter.refresh());h.logged=true;assertNotNull(h.adapter.refresh());
        h.header.pin=true;assertNull(h.adapter.refresh());
    }
    @Test void actualControllerOOpensAndSecondOClosesWithBankLifecycleCleanup() {
        var h=new HeaderHarness();var model=new BurrowModel();var provider=new Provider();
        var controller=new OutfitterBank(h.client,model::bankSelection,()->provider);
        controller.refresh();
        Runnable click=()->{h.press++;var w=h.header.all.stream().filter(n->n.text.equals("O") && !n.hidden).findFirst().orElseThrow();h.header.op(w.widget,1);};
        click.run();assertEquals(1,provider.opens);assertNotNull(provider.layout);
        click.run();assertEquals(1,provider.closes);
        click.run();h.open=false;controller.refresh();assertEquals(2,provider.closes);assertEquals(0,h.header.attachedEntries());
        h.open=true;controller.refresh();click.run();h.logged=false;controller.refresh();assertEquals(3,provider.closes);
        h.logged=true;controller.refresh();click.run();controller.stop();assertEquals(4,provider.closes);
        controller.refresh();assertEquals(0,h.header.attachedEntries());
    }
    @Test void transientBankTagsEventDoesNotCloseOLayoutButConfirmedMutationDoes() {
        var h=new HeaderHarness();var model=new BurrowModel();var provider=new Provider();
        var controller=new OutfitterBank(h.client,model::bankSelection,()->provider);
        var verifying=new java.util.concurrent.atomic.AtomicBoolean();
        var overrideAvailable=new java.util.concurrent.atomic.AtomicBoolean(true);
        var generation=new java.util.concurrent.atomic.AtomicLong(9);
        var recommended=L01LayoutSpecification.from(model.plan()).itemIds();
        controller.setBestBankRequested(()->true);
        controller.setBestBankVerifying(verifying::get);
        controller.setBestBankGeneration(generation::get);
        controller.setBestBankOverride(()->overrideAvailable.get()?recommended:null);
        controller.setVerifiedBankEvidence(()->new gg.feral.burrow.core.BankEvidence(
                java.util.Map.of(10828,1),java.util.Map.of(),java.util.Map.of(),
                gg.feral.burrow.core.BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT,
                System.currentTimeMillis(),true,false));
        controller.refresh();
        java.util.concurrent.atomic.AtomicBoolean freezeDuringOpen=new java.util.concurrent.atomic.AtomicBoolean();
        provider.duringOpen=()->freezeDuringOpen.set(controller.selectedBestBankWithdrawalTab());
        h.press++;
        var o=h.header.all.stream().filter(e->e.text.equals("O") && !e.hidden).findFirst().orElseThrow();
        h.header.op(o.widget,1);
        assertTrue(freezeDuringOpen.get(),"early Bank Tags redraw must be covered before tab activation");
        assertTrue(controller.selectedBestBankWithdrawalTab(),"selected verified O must permit a withdrawal freeze");
        assertEquals(1,provider.opens);
        assertEquals(10828,provider.layout.getLayout()[1]);
        assertEquals(-1,provider.layout.getLayout()[16], "Wiki fallback may not masquerade as withdrawable stock");
        assertEquals(0,provider.closes);
        // Actual recording: bank tab switch makes the override transiently null.
        verifying.set(true);overrideAvailable.set(false);
        for(int i=0;i<8;i++)controller.refresh();
        assertEquals(0,provider.closes,"uncertain Bank Tags widget must not close our selected tag");
        assertNotNull(provider.layout);
        overrideAvailable.set(true);verifying.set(false);
        controller.refresh();assertEquals(0,provider.closes);
        // A confirmed mutation genuinely retires the old owned layout.
        overrideAvailable.set(false);generation.incrementAndGet();
        assertFalse(controller.selectedBestBankWithdrawalTab(),"new generation revokes withdrawal freeze");
        controller.refresh();
        assertEquals(1,provider.closes);
        assertNull(provider.layout);
    }
    // ---- BUR-0016: owned-variant resolution without unsafe substitution ----
    static final int RING_POS=34; // Berserker ring (i) 11773 in the default Scurrius plan
    @Test void variantSpecPrefersOwnedImbuedRingOverPlainRing() {
        var plan=new BurrowModel().plan();
        assertEquals(11773,L01LayoutSpecification.from(plan).itemIds()[RING_POS]);
        java.util.List<String> notes=new java.util.ArrayList<>();
        // RuneLite's variant group order would pick plain 6737 first; Burrow must show the owned imbue.
        assertEquals(25264,L01LayoutSpecification.from(plan,java.util.Set.of(6737,25264),notes).itemIds()[RING_POS]);
        assertTrue(notes.isEmpty());
        assertEquals(-1,L01LayoutSpecification.from(plan,java.util.Set.of(6737),notes).itemIds()[RING_POS]);
        assertEquals(1,notes.size());
        assertArrayEquals(L01LayoutSpecification.from(plan).itemIds(),
                L01LayoutSpecification.from(plan,null,null).itemIds(), "static plan is not an ownership claim");
        java.util.List<String> missing = new java.util.ArrayList<>();
        assertEquals(-1, L01LayoutSpecification.from(plan, null, missing).itemIds()[RING_POS],
                "a live layout without bank evidence must not let Bank Tags substitute a plain ring");
        assertFalse(missing.isEmpty());
    }
    @Test void controllerUsesOwnedVariantFromTheOpenBank() {
        var h=new HeaderHarness();var model=new BurrowModel();var provider=new Provider();
        net.runelite.api.Item[] items={new net.runelite.api.Item(6737,1),new net.runelite.api.Item(25264,1)};
        h.bank=BankHeaderFixture.proxy(net.runelite.api.ItemContainer.class,(m,a)->switch(m) {
            case "getItems" -> items;
            default -> throw new AssertionError("Unpermitted container operation: "+m);
        });
        var controller=new OutfitterBank(h.client,model::bankSelection,()->provider);
        controller.refresh();
        var w=h.header.all.stream().filter(n->n.text.equals("O") && !n.hidden).findFirst().orElseThrow();
        h.press++;h.header.op(w.widget,1);
        assertEquals(1,provider.opens);
        assertEquals(25264,provider.layout.getLayout()[RING_POS]);
    }
    @Test void controllerSelectionChangeAndForeignNavigationDoNotKeepOldLayout() {
        var h=new HeaderHarness();var m=new BurrowModel();var p=new Provider();
        var c=new OutfitterBank(h.client,m::bankSelection,()->p);c.refresh();
        Runnable click=()->{h.press++;var w=h.header.all.stream().filter(n->n.text.equals("O") && !n.hidden).findFirst().orElseThrow();h.header.op(w.widget,1);};
        click.run();m.chooseBoss("Vorkath");c.refresh();assertNull(p.layout);click.run();assertEquals(2,p.opens);
        m.chooseBoss("Scurrius");click.run();p.tag="foreign";p.layout=new Layout("foreign");var foreign=p.layout;
        c.refresh();c.clear();assertSame(foreign,p.layout);assertEquals(2,p.closes);
    }

}
