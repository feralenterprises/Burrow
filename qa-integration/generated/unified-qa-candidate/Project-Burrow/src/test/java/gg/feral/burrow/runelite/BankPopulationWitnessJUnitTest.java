package gg.feral.burrow.runelite;

import java.lang.reflect.Proxy;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.widgets.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Build 19: bank container shorter than RuneLite's own tab-count varbits is still populating. */
final class BankPopulationWitnessJUnitTest {
    static Client client(int rawItems,int tabCount) {
        Widget widget=(Widget)Proxy.newProxyInstance(Widget.class.getClassLoader(),new Class<?>[]{Widget.class},
            (o,m,a)->m.getName().equals("isHidden")?false:null);
        Item[] bankItems=new Item[rawItems];
        for(int i=0;i<rawItems;i++)bankItems[i]=new Item(1000+i,1);
        ItemContainer bank=(ItemContainer)Proxy.newProxyInstance(ItemContainer.class.getClassLoader(),new Class<?>[]{ItemContainer.class},
            (o,m,a)->m.getName().equals("getItems")?bankItems:null);
        ItemContainer empty=(ItemContainer)Proxy.newProxyInstance(ItemContainer.class.getClassLoader(),new Class<?>[]{ItemContainer.class},
            (o,m,a)->m.getName().equals("getItems")?new Item[0]:null);
        return (Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},(o,m,a)->switch(m.getName()){
            case "getGameState"->GameState.LOGGED_IN;
            case "getAccountHash"->7L;
            case "getWidget"->a[0]==WidgetInfo.BANK_PIN_CONTAINER?null:widget;
            case "getItemContainer"->a[0]==InventoryID.BANK?bank:empty;
            case "getVarbitValue"->((Integer)a[0])==Varbits.BANK_TAB_ONE_COUNT?tabCount:0;
            case "getItemDefinition"->{int id=(Integer)a[0];
                yield Proxy.newProxyInstance(ItemComposition.class.getClassLoader(),new Class<?>[]{ItemComposition.class},
                (o2,m2,a2)->switch(m2.getName()){case "getId"->id;case "getPlaceholderTemplateId","getNote"->-1;default->null;});}
            default->null;});
    }
    @SuppressWarnings("deprecation")
    @Test void populatingBankRejectedAndConsistentBankWitnessed() {
        assertTrue(AutomaticBankSnapshotReader.capture(client(5,9)).isEmpty(),"5 slots but tab 1 claims 9");
        var ok=AutomaticBankSnapshotReader.capture(client(12,9));
        assertTrue(ok.isPresent());
        assertTrue(ok.get().tabWitnessed());
        assertEquals(12,ok.get().bank().size());
        var again=AutomaticBankSnapshotReader.capture(client(12,9)).orElseThrow();
        assertTrue(again.sameContent(ok.get()),"identical consecutive reads satisfy stability");
        var changed=AutomaticBankSnapshotReader.capture(client(13,9)).orElseThrow();
        assertFalse(changed.sameContent(ok.get()));
    }
}
