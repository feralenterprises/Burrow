package gg.feral.burrow.runelite;

import java.lang.reflect.Proxy;
import java.util.Map;
import net.runelite.api.*;
import net.runelite.api.widgets.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class IronLedgerManualBankReaderJUnitTest {
    @SuppressWarnings("unchecked")
    static <T> T make(Class<T> type, java.util.function.Function<java.lang.reflect.Method,Object> value) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
            (obj,method,args)->value.apply(method));
    }
    static Client client(boolean open, long account, Item[] items) {
        Widget widget=make(Widget.class,m->m.getName().equals("isHidden")?!open:null);
        ItemContainer bank=make(ItemContainer.class,m->m.getName().equals("getItems")?items:null);
        ItemComposition def=make(ItemComposition.class,m->switch(m.getName()) {
            case "getId"->6737;
            case "getNote","getPlaceholderTemplateId"->-1;
            default->null;
        });
        return (Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},
            (obj,m,args)->switch(m.getName()) {
                case "getGameState"->GameState.LOGGED_IN;
                case "getAccountHash"->account;
                case "getWidget"->args[0]==WidgetInfo.BANK_PIN_CONTAINER?null:widget;
                case "getItemContainer"->bank;
                case "getItemDefinition"->def;
                default->null;
            });
    }
    @Test void positiveExactIdsAndQuantitiesFromOpenBank() {
        var found=IronLedgerManualBankReader.scan(client(true,17,new Item[]{new Item(6737,3),new Item(-1,0),new Item(20594,1)}));
        assertTrue(found.observed());
        assertEquals(Map.of(6737,3),found.quantities());
    }
    @Test void closedBankCannotClaimOwnership() {
        var found=IronLedgerManualBankReader.scan(client(false,17,new Item[]{new Item(6737,1)}));
        assertFalse(found.observed());
        assertEquals(Map.of(),found.quantities());
    }
    @Test void noAccountCannotClaimOwnership() {
        var found=IronLedgerManualBankReader.scan(client(true,-1,new Item[]{new Item(6737,1)}));
        assertFalse(found.observed());
        assertEquals(Map.of(),found.quantities());
    }
    @Test void nullAndUnloadedBankAreUnknownNotEmpty() {
        for(Item[] items:new Item[][]{null,new Item[0]}) {
            var found=IronLedgerManualBankReader.scan(client(true,17,items));
            assertFalse(found.observed());
        }
    }
}
