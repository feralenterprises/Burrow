package com.feralenterprises.hoverlite;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Exact, owner-approved target-name cleanup. Purely visual: never edits menu entries. */
final class TargetShorten
{
    private static final Pattern PLAIN_OR_COLORED = Pattern.compile(
        "^(<col=[0-9a-fA-F]{6,8}>)?([^<>]+?)(</col>)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TREE = Pattern.compile(
        "^(Oak|Willow|Maple|Yew|Magic|Teak|Mahogany|Redwood) tree$", Pattern.CASE_INSENSITIVE);
    private TargetShorten() {}

    static String shorten(HoverCategory category, String action, String target)
    {
        if (category == null || action == null || target == null || target.isEmpty()
            || target.indexOf('\n') >= 0 || target.indexOf('\r') >= 0)
        {
            return target;
        }
        Matcher markup = PLAIN_OR_COLORED.matcher(target);
        if (!markup.matches())
        {
            return target;
        }
        String name = markup.group(2);
        String shorter = null;
        if (category == HoverCategory.GAME_OBJECT)
        {
            if (action.equalsIgnoreCase("Chop down") && TREE.matcher(name).matches())
            {
                shorter = name.substring(0, name.length() - " tree".length());
            }
            else if (action.equalsIgnoreCase("Pick") && name.equalsIgnoreCase("Limpwurt plant"))
            {
                shorter = name.substring(0, name.length() - " plant".length());
            }
            else if (action.equalsIgnoreCase("Open") && name.equalsIgnoreCase("Seed vault"))
            {
                shorter = "Vault";
            }
        }
        else if (category == HoverCategory.NPC && action.equalsIgnoreCase("Exchange")
            && name.equalsIgnoreCase("Tool Leprechaun"))
        {
            shorter = "Leprechaun";
        }
        if (shorter == null)
        {
            return target;
        }
        return (markup.group(1) == null ? "" : markup.group(1))
            + shorter + (markup.group(3) == null ? "" : markup.group(3));
    }
}
