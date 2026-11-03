package com.feralenterprises.hoverlite;

/** Global Auto Shorten defaults ON for NPCs/objects, even with persisted v0.1.x display modes. */
final class HoverModePolicy
{
    private HoverModePolicy() {}

    static HoverMode resolve(HoverCategory category, HoverMode configured, boolean autoEnabled)
    {
        if (autoEnabled && (category == HoverCategory.NPC || category == HoverCategory.GAME_OBJECT))
        {
            return HoverMode.SMART_SHORTEN;
        }
        return configured;
    }
}
