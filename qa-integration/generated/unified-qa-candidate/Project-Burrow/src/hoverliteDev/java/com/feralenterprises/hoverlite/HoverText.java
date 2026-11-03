package com.feralenterprises.hoverlite;

/** Pure formatting logic: no game actions, menu modification, or state writes. */
final class HoverText
{
    private HoverText() {}

    // Kept for compatibility with the original pure formatting tests/callers.
    static String format(HoverMode mode, String action, String target)
    {
        return format(mode, HoverCategory.OTHER, action, target);
    }

    static String format(HoverMode mode, HoverCategory category, String action, String target)
    {
        if (mode == null || mode == HoverMode.HIDDEN || action == null || action.trim().isEmpty())
        {
            return null;
        }
        if (mode == HoverMode.INDICATOR)
        {
            return "<col=65eeb0>●</col>";
        }
        if (mode == HoverMode.ACTION_ONLY)
        {
            return action;
        }
        if (mode == HoverMode.SMART_SHORTEN)
        {
            // Keep original RuneLite menu entries intact; only presentation changes.
            // Normalize the core action independently of the target's name.
            final String originalAction = action;
            action = CoreAction.shorten(category, originalAction, target);
            target = TargetShorten.shorten(category, originalAction,
                AutoShorten.shorten(category, target));
        }
        return target == null || target.isEmpty() ? action : action + " " + target;
    }
}
