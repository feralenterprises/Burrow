package gg.feral.burrow.ui;

import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

/** UI-only typography helpers. No RuneLite event, account, item or gameplay dependencies. */
public final class FeralTypography {
    private FeralTypography() { }

    public enum TextWeight {
        REGULAR("Regular"), BOLD("Bold");
        private final String label;
        TextWeight(String label) { this.label = label; }
        public String label() { return label; }
    }

    public static TextWeight parseWeight(String raw) {
        if (raw == null) return TextWeight.REGULAR;
        try { return TextWeight.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return TextWeight.REGULAR; }
    }

    public static int size(int nominalSize, int presetDelta) {
        return Math.max(7, nominalSize + presetDelta);
    }

    public static Font font(int nominalSize, int presetDelta, boolean emphasized, TextWeight weight) {
        boolean bold = emphasized || weight == TextWeight.BOLD;
        return new Font("Dialog", bold ? Font.BOLD : Font.PLAIN, size(nominalSize,presetDelta));
    }

    /** Word-wrap within a fixed width. Never fabricate extra text or hide a clipped tail without an ellipsis. */
    public static List<String> wrap(FontMetrics metrics, String input, int width, int maxLines) {
        if (metrics == null || width <= 0 || maxLines <= 0) return List.of();
        String value = input == null ? "" : input.trim().replaceAll("\\s+", " ");
        if (value.isEmpty()) return List.of("");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : value.split(" ")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (metrics.stringWidth(candidate) <= width) { current.setLength(0); current.append(candidate); continue; }
            if (current.length() > 0) { lines.add(current.toString()); current.setLength(0); }
            // A single unbreakable word may itself exceed width: split safely by characters.
            while (!word.isEmpty() && metrics.stringWidth(word) > width) {
                int end=1;
                while (end < word.length() && metrics.stringWidth(word.substring(0,end+1)) <= width) end++;
                lines.add(word.substring(0,end)); word=word.substring(end);
            }
            current.append(word);
        }
        if(current.length()>0)lines.add(current.toString());
        if(lines.size()<=maxLines)return List.copyOf(lines);
        List<String> visible=new ArrayList<>(lines.subList(0,maxLines));
        StringBuilder finalLine=new StringBuilder(visible.get(maxLines-1));
        while(finalLine.length()>0 && metrics.stringWidth(finalLine+"…")>width)
            finalLine.deleteCharAt(finalLine.length()-1);
        visible.set(maxLines-1,finalLine+"…");
        return List.copyOf(visible);
    }
}
