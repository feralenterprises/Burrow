package gg.feral.burrow.ui;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

/** BUR-0037: lossless pixel-width-wrapped preparation and switch text.
 * The RuneLite host's existing outer scroll pane provides vertical scrolling. */
final class PreparationNotesLayout {
    static final int LINE_HEIGHT = 16;
    private static final int MIN_HEIGHT = 225;

    record Line(String text, boolean heading) { }
    record Layout(List<Line> lines, int height) {
        Layout { lines = List.copyOf(lines); }
    }

    private PreparationNotesLayout() { }

    static Layout empty() { return new Layout(List.of(), MIN_HEIGHT); }

    static Layout layout(List<String> notes, List<String> switches, FontMetrics font, int width) {
        if (font == null || width < 40) throw new IllegalArgumentException("Invalid font or width");
        List<Line> rows = new ArrayList<>();
        if (notes.isEmpty()) rows.add(new Line("Follow the linked Wiki strategy.",false));
        else {
            for (String note : notes) {
                addWrapped(rows,"• " + note,font,width);
                rows.add(new Line("",false));
            }
        }
        if (!switches.isEmpty()) {
            rows.add(new Line("SWITCHES",true));
            for (String detail : switches) {
                addWrapped(rows,"• " + detail,font,width);
                rows.add(new Line("",false));
            }
        }
        // Card starts at 1014, header baseline +20, notes +41.
        // Reserve 52px after the last line for attribution and readiness footer.
        int height=Math.max(MIN_HEIGHT,41+rows.size()*LINE_HEIGHT+52);
        return new Layout(rows,height);
    }

    private static void addWrapped(List<Line> lines, String input,FontMetrics fm,int width) {
        // No ellipsis or fixed line limit. Prefer word boundaries; break very
        // long item names/JSON tokens at their actual rendered pixel width.
        for(String paragraph:input.split("\\R",-1)) {
            StringBuilder row=new StringBuilder();
            for(String word:paragraph.trim().split("\\s+")) {
                if(word.isEmpty())continue;
                String candidate=row.length()==0?word:row+" "+word;
                if(fm.stringWidth(candidate)<=width) {
                    row.setLength(0);row.append(candidate);
                    continue;
                }
                if(row.length()>0) {
                    lines.add(new Line(row.toString(),false));row.setLength(0);
                }
                for(int i=0;i<word.length();i++) {
                    char c=word.charAt(i);
                    if(row.length()>0 && fm.stringWidth(row.toString()+c)>width) {
                        lines.add(new Line(row.toString(),false));row.setLength(0);
                    }
                    row.append(c);
                }
            }
            lines.add(new Line(row.toString(),false));
        }
    }
}
