package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import java.time.LocalDate;
import java.util.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** R08: Home weekly card text (what the player is shown). */
final class BqaR08HomeWeeklyJUnitTest {
    @Test void weeklyCardShowsObservedValuesAndNotTracked() throws Exception {
        var t = new WeeklyProgress.Totals(LocalDate.parse("2026-10-26"), 44398, 15, 88, 9, Map.of(), 4);
        SwingUtilities.invokeAndWait(() -> {
            var p = new BurrowPanel(new BurrowModel());
            assertEquals(List.of("Sign in to load this account's weekly progress"), p.weeklyLinesForTest());
            p.setWeeklyProgress(t, "Mon–Sun, local time · loaded");
            assertEquals(List.of("Week of 2026-10-26 (Mon–Sun)", "Observed XP: +44,398", "Verified boss KC increases: +15",
                "Boss drops: 88 items · ~9 loot kills (est.)", "Level-ups: not tracked", "Confirmed chests (Barrows): not tracked",
                "Collection unlocks: not tracked", "4 logins · first KC readings are baselines"), p.weeklyLinesForTest());
            p.setSize(225, p.getPreferredSize().height);
            var img = new java.awt.image.BufferedImage(225, p.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g = img.createGraphics(); p.paint(g); g.dispose();
        });
    }
}
