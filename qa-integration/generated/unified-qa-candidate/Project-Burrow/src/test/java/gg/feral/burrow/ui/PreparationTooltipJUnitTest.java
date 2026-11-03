package gg.feral.burrow.ui;

import java.awt.Dimension;
import java.util.List;
import javax.swing.JToolTip;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Focused Swing tooltip formatting regression tests for BUR-0036. */
final class PreparationTooltipJUnitTest {
    @Test void longPreparationNotesFitNormalTooltipSize() {
        String longLine = "At the Wilderness entrance watch for dangerous players and teleblock ".repeat(20);
        String html = PreparationTooltip.render(
            List.of(longLine, longLine, longLine),
            List.of(longLine, "Other unlock"),
            List.of(longLine),
            List.of(longLine));
        assertTrue(html.startsWith("<html>"));
        assertTrue(html.endsWith("</html>"));
        assertFalse(html.contains(longLine)); // never put an entire long source string into one line
        JToolTip tip = new JToolTip();
        tip.setTipText(html);
        Dimension size = tip.getPreferredSize();
        assertTrue(size.width <= 270, "tooltip width was " + size.width);
        assertTrue(size.height <= 340, "tooltip height was " + size.height);
    }

    @Test void maliciousMarkupIsEscapedAndNotesAreBounded() {
        String html = PreparationTooltip.render(
            List.of("A <img src=x> & B", "Valid second note"),
            List.of("A <script>danger</script>"), List.of(), List.of());
        assertFalse(html.contains("<img"));
        assertFalse(html.contains("<script"));
        assertTrue(html.contains("&lt;img"));
        assertTrue(html.contains("&amp;"));
    }

    @Test void all66OutfitterProfilesAreBoundedThroughActualPanelMouseRoute() {
        PreparationTooltipSmokeCheck.main(new String[0]);
    }

    @Test void emptyListsYieldShortPreview() {
        String html = PreparationTooltip.render(List.of(),List.of(),List.of(),List.of());
        assertTrue(html.contains("Preparation preview"));
        assertTrue(html.contains("See preparation panel"));
        assertFalse(html.contains("Required unlocks:"));
    }
}
