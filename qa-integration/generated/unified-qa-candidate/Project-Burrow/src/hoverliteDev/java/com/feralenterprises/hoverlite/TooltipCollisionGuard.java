package com.feralenterprises.hoverlite;

import java.util.regex.Pattern;

/**
 * Prevent the auto mode from giving two distinct actions the same visible label
 * on the current interaction menu. Identical originals are not a collision.
 */
final class TooltipCollisionGuard
{
    private static final Pattern TAGS = Pattern.compile("<[^<>]*>");

    private TooltipCollisionGuard() {}

    static boolean isAmbiguous(String original, String shortened, String competingOriginal,
                               String competingShortened)
    {
        if (shortened == null || competingShortened == null || original == null || competingOriginal == null)
        {
            return false;
        }
        final String displayed = visible(shortened);
        return displayed.equalsIgnoreCase(visible(competingShortened))
            && !visible(original).equalsIgnoreCase(visible(competingOriginal));
    }

    /**
     * NPC combat levels are optional display-only metadata in Auto Shorten.
     * Two menu entries whose only distinguishing text is a verified level suffix
     * must not cause the overlay to put the level back on screen.
     *
     * This is NOT a general collision bypass. Different actions or names still
     * require a fallback, and malformed level layouts remain untouched.
     */
    static boolean onlyNpcCombatLevelsDiffer(HoverCategory firstCategory, String firstAction,
            String firstTarget, HoverCategory secondCategory, String secondAction, String secondTarget)
    {
        if (firstCategory != HoverCategory.NPC || secondCategory != HoverCategory.NPC
            || firstAction == null || secondAction == null || !firstAction.equals(secondAction)
            || firstTarget == null || secondTarget == null)
        {
            return false;
        }
        final String firstShort = AutoShorten.shorten(HoverCategory.NPC, firstTarget);
        final String secondShort = AutoShorten.shorten(HoverCategory.NPC, secondTarget);
        // Both inputs must actually contain a well-formed removable level.
        return !visible(firstTarget).equalsIgnoreCase(visible(secondTarget))
            && !visible(firstTarget).equalsIgnoreCase(visible(firstShort))
            && !visible(secondTarget).equalsIgnoreCase(visible(secondShort))
            && visible(firstShort).equalsIgnoreCase(visible(secondShort));
    }

    private static String visible(String input)
    {
        return TAGS.matcher(input).replaceAll("").trim();
    }
}
