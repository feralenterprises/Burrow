package com.feralenterprises.hoverlite;

/** What HoverLite shows for an interaction category. */
public enum HoverMode
{
    FULL("Full action + target"),
    SMART_SHORTEN("Auto Shorten"),
    ACTION_ONLY("Action only"),
    INDICATOR("Indicator only"),
    HIDDEN("Hidden");

    private final String label;

    HoverMode(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
