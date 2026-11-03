package com.feralenterprises.hoverlite;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Display-only NPC combat level removal for RuneLite's actual menu target markup.
 * RuneLite often emits e.g. <col=ffff00>Ram<col=00ff00> (level-2), with no closing tags.
 * Unsupported/malformed markup always falls back to original text.
 */
final class AutoShorten
{
    private static final String COLOR = "<col=[0-9a-fA-F]{6,8}>";
    private static final Pattern SINGLE = Pattern.compile(
        "^(" + COLOR + ")?([^<>]*?)\\s+\\(level-\\d+\\)(</col>)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DUAL = Pattern.compile(
        "^(" + COLOR + ")?([^<>]*?)(</col>)?\\s*(" + COLOR + ")\\s*\\(level-\\d+\\)(</col>)?$",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern CLOSED_NAME = Pattern.compile(
        "^(" + COLOR + ")([^<>]+)</col>\\s+\\(level-\\d+\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXISTING_LEVEL = Pattern.compile("\\(level-\\d+\\)", Pattern.CASE_INSENSITIVE);

    private AutoShorten() {}

    static String shorten(HoverCategory category, String target)
    {
        if (category != HoverCategory.NPC || target == null || target.isEmpty()
            || target.indexOf('\n') >= 0 || target.indexOf('\r') >= 0 || target.indexOf('\t') >= 0)
        {
            return target;
        }

        final Matcher coloredLevel = DUAL.matcher(target);
        if (coloredLevel.matches())
        {
            String nameColor = coloredLevel.group(1);
            String name = coloredLevel.group(2);
            String nameClose = coloredLevel.group(3);
            // Never infer that a floating level-color tag is a valid NPC
            // color layout. A missing name-color opener is ambiguous.
            if (nameColor == null)
            {
                return target;
            }
            if (badName(name))
            {
                return target;
            }
            return (nameColor == null ? "" : nameColor) + name
                + (nameClose == null ? "" : nameClose);
        }

        final Matcher closedName = CLOSED_NAME.matcher(target);
        if (closedName.matches() && !badName(closedName.group(2)))
        {
            return closedName.group(1) + closedName.group(2) + "</col>";
        }

        final Matcher level = SINGLE.matcher(target);
        if (level.matches())
        {
            String nameColor = level.group(1);
            String name = level.group(2);
            String nameClose = level.group(3);
            if (nameClose != null && nameColor == null || badName(name))
            {
                return target;
            }
            return (nameColor == null ? "" : nameColor) + name
                + (nameClose == null ? "" : nameClose);
        }
        return target;
    }

    private static boolean badName(String name)
    {
        return name == null || name.trim().isEmpty()
            || EXISTING_LEVEL.matcher(name).find(); // doubled levels: preserve unmodified
    }
}
