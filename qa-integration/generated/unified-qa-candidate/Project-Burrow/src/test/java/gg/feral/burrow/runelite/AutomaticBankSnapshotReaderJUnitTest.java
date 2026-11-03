package gg.feral.burrow.runelite;

import java.lang.reflect.Proxy;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.widgets.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AutomaticBankSnapshotReaderJUnitTest {
    @SuppressWarnings("unchecked")
    static <T> T mock(Class<T> kind,java.util.function.Function<java.lang.reflect.Method,Object> handler) {
        return (T)Proxy.newProxyInstance(kind.getClassLoader(),new Class<?>[]{kind},
            (obj,m,args)->handler.apply(m));
    }
    static Client setup(boolean opened,boolean invReady) {
        Widget widget=mock(Widget.class,m->m.getName().equals("isHidden")?!opened:null);
        ItemContainer bank=mock(ItemContainer.class,m->m.getName().equals("getItems")?
            new Item[]{new Item(6737,1),new Item(20594,1),new Item(-1,0)}:null);
        ItemContainer inventory=mock(ItemContainer.class,m->m.getName().equals("getItems")?
            (invReady?new Item[]{new Item(6737,1),new Item(385,2)}:null):null);
        ItemContainer equipment=mock(ItemContainer.class,m->m.getName().equals("getItems")?
            new Item[]{new Item(6737,1)}:null);
        ItemComposition def=mock(ItemComposition.class,m->switch(m.getName()) {
            case "getId"->6737;
            case "getPlaceholderTemplateId","getNote"->-1;
            default->null;
        });
        return (Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},
            (obj,m,args)->switch(m.getName()) {
            case "getGameState"->GameState.LOGGED_IN;
            case "getAccountHash"->14L;
            case "getWidget"->args[0]==WidgetInfo.BANK_PIN_CONTAINER?null:widget;
            case "getItemContainer"->args[0]==InventoryID.BANK?bank:args[0]==InventoryID.INVENTORY?inventory:equipment;
            case "getItemDefinition"->def;
            default->null;
        });
    }
    @Test void readsAllThreeLocationsAndCombinesOnlyPositiveObservedItems() {
        var found=AutomaticBankSnapshotReader.capture(setup(true,true));
        assertTrue(found.isPresent());
        assertEquals(14L,found.get().accountHash());
        assertEquals(1,found.get().bank().get(6737));
        assertEquals(1,found.get().inventory().get(6737));
        assertEquals(1,found.get().equipment().get(6737));
        assertEquals(3,found.get().combined().get(6737));
        assertEquals(2,found.get().combined().get(385));
        assertFalse(found.get().combined().containsKey(20594));
    }
    @Test void closedBankCannotGenerateOwnedEvidence() {
        assertTrue(AutomaticBankSnapshotReader.capture(setup(false,true)).isEmpty());
    }
    @Test void aPartiallyLoadedBankCannotGenerateOwnedEvidence() {
        assertTrue(AutomaticBankSnapshotReader.capture(setup(true,false)).isEmpty());
    }
    @Test void hiddenBankItemsAreOpenForRetryButNotTrustedAsOwnedEvidence() {
        Client c=setup(true,true);
        Widget hidden=mock(Widget.class,m->m.getName().equals("isHidden")?true:null);
        Client withHiddenItems=(Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},
            (obj,m,args)->m.getName().equals("getWidget") && args[0]==WidgetInfo.BANK_ITEM_CONTAINER
                ?hidden:m.invoke(c,args));
        assertTrue(AutomaticBankSnapshotReader.visible(withHiddenItems),"bank root still open; don't close O tab");
        assertEquals(gg.feral.burrow.core.BankCaptureGate.Outcome.WIDGET_NOT_READY,
            AutomaticBankSnapshotReader.evaluate(withHiddenItems).outcome(),"no stock should be trusted");
        assertTrue(AutomaticBankSnapshotReader.capture(withHiddenItems).isEmpty());
    }
}
