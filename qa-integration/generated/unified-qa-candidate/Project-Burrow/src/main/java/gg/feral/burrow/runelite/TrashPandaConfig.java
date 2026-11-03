package gg.feral.burrow.runelite;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * CP-4 (BQA-R12-015): RuneLite NATIVE configuration, reachable from the plugin list's settings cog.
 *
 * <p>Only owner-approved, working presentation preferences. The group and key names are exactly the ones the
 * R13-R05 in-panel control already wrote ({@code trashpanda-appearance} / {@code fontSize} / {@code accentColor}),
 * so existing saved choices carry over unchanged. Account data (mode, goals, rare drops, history) lives in the
 * separate {@code trashpanda} group and is never exposed or reset from here.
 */
@ConfigGroup(TrashPandaConfig.GROUP)
public interface TrashPandaConfig extends Config {
    String GROUP = "trashpanda-appearance";
    String FONT_SIZE = "fontSize";
    String ACCENT = "accentColor";
    String TEXT_WEIGHT = "textWeight";
    String FILL_FOCUS = "bestBankFill";

    enum TextSize { COMPACT, DEFAULT, LARGE, EXTRA_LARGE }
    enum Accent { BRONZE, BLUE, GREEN, CRIMSON }
    enum TextWeight { REGULAR, BOLD }

    @ConfigItem(keyName = FONT_SIZE, name = "Text size", position = 1,
        description = "Sidebar text size, including Extra Large. Normal is saved as DEFAULT for compatibility.")
    default TextSize textSize() { return TextSize.DEFAULT; }

    @ConfigItem(keyName = TEXT_WEIGHT, name = "Text weight", position = 2,
        description = "Use Bold labels and body text throughout the Burrow sidebar.")
    default TextWeight textWeight() { return TextWeight.REGULAR; }

    @ConfigItem(keyName = ACCENT, name = "Accent colour", position = 3,
        description = "Highlight colour for headings and selected controls. Bronze is the Feral default.")
    default Accent accent() { return Accent.BRONZE; }

    /** CP-7: what Best in Bank optimises when it fills a slot the Wiki plan leaves empty with your own gear. */
    enum FillFocus { MATCH, DAMAGE, DEFENCE, PRAYER }

    @ConfigItem(keyName = FILL_FOCUS, name = "Best in Bank fill", position = 4,
        description = "When you don't own the Wiki item: MATCH picks your owned item with the closest stats to it; or pick your best item for Damage, Defence or Prayer.")
    default FillFocus fillFocus() { return FillFocus.MATCH; }
}
