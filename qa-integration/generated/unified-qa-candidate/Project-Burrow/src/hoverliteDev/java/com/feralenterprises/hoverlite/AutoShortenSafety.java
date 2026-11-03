package com.feralenterprises.hoverlite;

import net.runelite.api.MenuAction;

/** No abbreviations for item-on-target, selected spell, or unknown menu actions. */
final class AutoShortenSafety
{
    private AutoShortenSafety() {}

    static boolean allows(MenuAction type)
    {
        if (type == null) return false;
        switch (type)
        {
            case NPC_FIRST_OPTION:
            case NPC_SECOND_OPTION:
            case NPC_THIRD_OPTION:
            case NPC_FOURTH_OPTION:
            case NPC_FIFTH_OPTION:
            case EXAMINE_NPC:
            case GAME_OBJECT_FIRST_OPTION:
            case GAME_OBJECT_SECOND_OPTION:
            case GAME_OBJECT_THIRD_OPTION:
            case GAME_OBJECT_FOURTH_OPTION:
            case GAME_OBJECT_FIFTH_OPTION:
            case EXAMINE_OBJECT:
                return true;
            default:
                return false;
        }
    }
}
