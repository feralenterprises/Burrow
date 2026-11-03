package com.feralenterprises.hoverlite;

import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;

/** Categories are inferred only from RuneLite's menu entries. Never modify an entry. */
public enum HoverCategory
{
    NPC, INVENTORY_ITEM, GAME_OBJECT, GROUND_ITEM, PLAYER, INTERFACE, OTHER;

    static HoverCategory from(MenuEntry entry)
    {
        MenuAction type = entry.getType();
        if (type == null)
        {
            return OTHER;
        }
        switch (type)
        {
            case ITEM_USE_ON_NPC:
            case WIDGET_TARGET_ON_NPC:
            case NPC_FIRST_OPTION:
            case NPC_SECOND_OPTION:
            case NPC_THIRD_OPTION:
            case NPC_FOURTH_OPTION:
            case NPC_FIFTH_OPTION:
            case EXAMINE_NPC:
                return NPC;

            case ITEM_USE_ON_GAME_OBJECT:
            case WIDGET_TARGET_ON_GAME_OBJECT:
            case GAME_OBJECT_FIRST_OPTION:
            case GAME_OBJECT_SECOND_OPTION:
            case GAME_OBJECT_THIRD_OPTION:
            case GAME_OBJECT_FOURTH_OPTION:
            case GAME_OBJECT_FIFTH_OPTION:
            case EXAMINE_OBJECT:
                return GAME_OBJECT;

            case ITEM_USE_ON_GROUND_ITEM:
            case WIDGET_TARGET_ON_GROUND_ITEM:
            case GROUND_ITEM_FIRST_OPTION:
            case GROUND_ITEM_SECOND_OPTION:
            case GROUND_ITEM_THIRD_OPTION:
            case GROUND_ITEM_FOURTH_OPTION:
            case GROUND_ITEM_FIFTH_OPTION:
            case EXAMINE_ITEM_GROUND:
                return GROUND_ITEM;

            case ITEM_USE_ON_PLAYER:
            case WIDGET_TARGET_ON_PLAYER:
            case PLAYER_FIRST_OPTION:
            case PLAYER_SECOND_OPTION:
            case PLAYER_THIRD_OPTION:
            case PLAYER_FOURTH_OPTION:
            case PLAYER_FIFTH_OPTION:
            case PLAYER_SIXTH_OPTION:
            case PLAYER_SEVENTH_OPTION:
            case PLAYER_EIGHTH_OPTION:
            case RUNELITE_PLAYER:
                return PLAYER;

            case ITEM_USE_ON_ITEM:
            case WIDGET_USE_ON_ITEM:
            case ITEM_FIRST_OPTION:
            case ITEM_SECOND_OPTION:
            case ITEM_THIRD_OPTION:
            case ITEM_FOURTH_OPTION:
            case ITEM_FIFTH_OPTION:
            case ITEM_USE:
            case EXAMINE_ITEM:
                return INVENTORY_ITEM;

            case WIDGET_TYPE_1:
            case WIDGET_TARGET:
            case WIDGET_CLOSE:
            case WIDGET_TYPE_4:
            case WIDGET_TYPE_5:
            case WIDGET_CONTINUE:
            case WIDGET_FIRST_OPTION:
            case WIDGET_SECOND_OPTION:
            case WIDGET_THIRD_OPTION:
            case WIDGET_FOURTH_OPTION:
            case WIDGET_FIFTH_OPTION:
            case WIDGET_TARGET_ON_WIDGET:
            case CC_OP:
            case RUNELITE_WIDGET:
            {
                // Modern RuneLite uses CC_OP on items too. Preserve inventory behavior
                // when the clicked widget is in the inventory or side-inventory group.
                final int group = WidgetUtil.componentToInterface(entry.getParam1());
                if (group == InterfaceID.INVENTORY || group == InterfaceID.BANKSIDE
                    || group == InterfaceID.GE_OFFERS_SIDE || group == InterfaceID.SHOPSIDE
                    || group == InterfaceID.TRADESIDE || group == InterfaceID.BANK_DEPOSITBOX
                    || group == InterfaceID.EQUIPMENT_SIDE)
                {
                    return INVENTORY_ITEM;
                }
                return INTERFACE;
            }

            default:
                return OTHER;
        }
    }
}
