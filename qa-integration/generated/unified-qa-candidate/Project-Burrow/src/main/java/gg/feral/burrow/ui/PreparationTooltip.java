package gg.feral.burrow.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A bounded tooltip preview, not a second rendering of the complete boss guide.
 * The full notes remain the concern of the visible preparation panel (BUR-0037).
 * Explicit line breaks avoid relying on platform-specific Swing HTML word wrapping.
 */
final class PreparationTooltip {
    private static final int CHARACTERS_PER_LINE = 27;
    private static final int MAX_LINES = 12;
    private static final int MAX_NOTES = 2;

    private PreparationTooltip() {}

    static String render(List<String> notes, List<String> unlocks,
                         List<String> chargedStates, List<String> switches) {
        Objects.requireNonNull(notes, "notes");
        Objects.requireNonNull(unlocks, "unlocks");
        Objects.requireNonNull(chargedStates, "chargedStates");
        Objects.requireNonNull(switches, "switches");
        List<String> lines = new ArrayList<>();
        lines.add("Preparation preview");
        append(lines, "Notes", notes, MAX_NOTES);
        append(lines, "Required unlocks", unlocks, 1);
        append(lines, "Charges / item states", chargedStates, 1);
        append(lines, "Switches", switches, 1);
        // Reserve the last row for the pointer to the existing on-panel notes.
        boolean clipped = lines.size() > MAX_LINES - 1;
        if (clipped) lines = new ArrayList<>(lines.subList(0, MAX_LINES - 1));
        lines.add(clipped ? "More in preparation panel" : "See preparation panel");

        StringBuilder html = new StringBuilder("<html><table width='205' cellpadding='3' cellspacing='0'><tr><td>");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) html.append("<br>");
            html.append(escapeHtml(lines.get(i)));
        }
        return html.append("</td></tr></table></html>").toString();
    }

    private static void append(List<String> lines, String heading, List<String> items, int count) {
        if (items.isEmpty()) return;
        lines.add(heading + ":");
        int actual = 0;
        for (String item : items) {
            if (item == null || item.isBlank()) continue;
            if (actual == count) break;
            actual++;
            lines.addAll(wrap(item, 2));
        }
        if (items.size() > actual) lines.add("+" + (items.size() - actual) + " more");
    }

    /** Split at word boundaries where possible; never emit a line wider than the cap. */
    private static List<String> wrap(String value, int maxLines) {
        String remaining = value.strip().replaceAll("\\s+", " ");
        List<String> lines = new ArrayList<>();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            if (remaining.length() <= CHARACTERS_PER_LINE) {
                lines.add(remaining);
                remaining = "";
                break;
            }
            int split = remaining.lastIndexOf(' ', CHARACTERS_PER_LINE);
            if (split <= 0) split = CHARACTERS_PER_LINE;
            lines.add(remaining.substring(0, split).strip());
            remaining = remaining.substring(split).strip();
        }
        if (!remaining.isEmpty() && !lines.isEmpty()) {
            int last = lines.size() - 1;
            String shortened = lines.get(last);
            if (shortened.length() >= CHARACTERS_PER_LINE) shortened = shortened.substring(0, CHARACTERS_PER_LINE - 3);
            lines.set(last, shortened + "...");
        }
        return lines;
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
