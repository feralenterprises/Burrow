package com.feralenterprises.hoverlite;

import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.InterfaceID;
import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Proxy;

public class HoverCategoryTest
{
    private MenuEntry entry(MenuAction type, int widgetGroup) {
        return (MenuEntry) Proxy.newProxyInstance(MenuEntry.class.getClassLoader(), new Class<?>[]{MenuEntry.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getType")) return type;
                if (method.getName().equals("getParam1")) return widgetGroup << 16;
                throw new UnsupportedOperationException(method.getName());
            });
    }
    @Test public void recognizesDuke() {
        assertEquals(HoverCategory.NPC, HoverCategory.from(entry(MenuAction.NPC_FIRST_OPTION, 0)));
    }
    @Test public void recognizesInventoryWidgetAction() {
        assertEquals(HoverCategory.INVENTORY_ITEM, HoverCategory.from(entry(MenuAction.CC_OP, InterfaceID.INVENTORY)));
    }
    @Test public void recognizesStandardInventoryOption() {
        assertEquals(HoverCategory.INVENTORY_ITEM, HoverCategory.from(entry(MenuAction.ITEM_FIRST_OPTION, InterfaceID.INVENTORY)));
    }
    @Test public void recognizesObjectAndGround() {
        assertEquals(HoverCategory.GAME_OBJECT, HoverCategory.from(entry(MenuAction.GAME_OBJECT_FIRST_OPTION, 0)));
        assertEquals(HoverCategory.GROUND_ITEM, HoverCategory.from(entry(MenuAction.GROUND_ITEM_FIRST_OPTION, 0)));
    }
    @Test public void nonItemUi() {
        assertEquals(HoverCategory.INTERFACE, HoverCategory.from(entry(MenuAction.CC_OP, InterfaceID.PRAYERBOOK)));
    }
}
