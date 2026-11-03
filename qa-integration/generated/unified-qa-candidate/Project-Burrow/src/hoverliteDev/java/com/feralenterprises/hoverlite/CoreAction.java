package com.feralenterprises.hoverlite;

import java.util.Locale;
import java.util.regex.Pattern;

/** Shorten only established compound actions when the target remains identifiable. */
final class CoreAction
{
    private static final Pattern TREE = Pattern.compile("^(Oak|Willow|Maple|Yew|Magic|Teak|Mahogany|Redwood) tree$|^Tree$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUSH = Pattern.compile("^[\\p{L}][\\p{L} '-]* bush$", Pattern.CASE_INSENSITIVE);
    private static final Pattern COLOR_TAG = Pattern.compile("(?i)</col>|<col=[0-9a-f]{6,8}>");

    private CoreAction() {}

    static String shorten(HoverCategory category, String action, String target)
    {
        if (category == null || action == null || action.isEmpty() || target == null || target.trim().isEmpty()
            || action.indexOf('<') >= 0 || action.indexOf('>') >= 0
            || target.indexOf('\n') >= 0 || target.indexOf('\r') >= 0)
        {
            return action;
        }
        // Only strip recognized RuneLite color tags for *matching*. Actual tooltip target is unchanged.
        final String name = COLOR_TAG.matcher(target).replaceAll("");
        if (name.indexOf('<') >= 0 || name.indexOf('>') >= 0)
        {
            return action;
        }

        String replacement = null;
        if (category == HoverCategory.NPC && action.equalsIgnoreCase("Talk-to"))
        {
            replacement = "Talk";
        }
        else if (category == HoverCategory.GAME_OBJECT)
        {
            if (action.equalsIgnoreCase("Chop down") && TREE.matcher(name).matches())
            {
                replacement = "Chop";
            }
            else if (action.equalsIgnoreCase("Pray-at") && name.equalsIgnoreCase("Altar"))
            {
                replacement = "Pray";
            }
            else if (action.equalsIgnoreCase("Pick-from") && BUSH.matcher(name).matches())
            {
                replacement = "Pick";
            }
            else if (action.equalsIgnoreCase("Pick-spine") && name.equalsIgnoreCase("Cactus"))
            {
                replacement = "Pick";
            }
        }
        if (replacement == null)
        {
            return action; // Direction, amounts, route, targets, states are preserved.
        }
        return Character.isLowerCase(action.charAt(0)) ? replacement.toLowerCase(Locale.ROOT) : replacement;
    }
}
