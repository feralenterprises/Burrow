package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Combat Achievement reference in the Boss Chronicle. No kill-count tasks and no raids. */
public final class CombatTaskChronicleJUnitTest {
    @Test void guideLoadsAndKeepsBossNamesSelectable() {
        assertTrue(CombatTaskGuide.taskCount() >= 280, "task list must not shrink silently");
        Set<String> chronicle = new HashSet<>();
        for (var e : BossRegistry.all()) chronicle.add(e.id());
        for (String boss : CombatTaskGuide.bosses()) assertTrue(chronicle.contains(boss), "unknown boss " + boss);
    }
    @Test void noKillCountOrRaidTasksAreListed() {
        for (String boss : CombatTaskGuide.bosses()) for (var task : CombatTaskGuide.forBoss(boss)) {
            assertNotEquals("Kill Count", task.type(), task.name());
            assertFalse(task.requirement().isBlank(), task.name());
            assertTrue(task.tip().isEmpty() || task.source().startsWith("https://oldschool.runescape.wiki/"), "a tip needs its Wiki page: " + task.name());
        }
        assertTrue(CombatTaskGuide.forBoss("Verzik Vitur").isEmpty());
        assertTrue(CombatTaskGuide.forBoss(null).isEmpty());
    }
    @Test void everyMoonSharesTheMoonsTasks() {
        var blue = CombatTaskGuide.forBoss("Blue Moon").stream().map(CombatTaskGuide.Task::name).toList();
        var eclipse = CombatTaskGuide.forBoss("Eclipse Moon").stream().map(CombatTaskGuide.Task::name).toList();
        assertTrue(blue.contains("Betrayal"));
        assertFalse(blue.contains("The Clone Zone"));
        assertTrue(eclipse.contains("Betrayal"));
        assertTrue(eclipse.contains("The Clone Zone"));
    }
    @Test void tasksAreEasiestFirst() {
        var tiers = CombatTaskGuide.forBoss("Vorkath").stream().map(CombatTaskGuide.Task::tier).toList();
        assertEquals("Elite", tiers.get(0));
        assertEquals("Grandmaster", tiers.get(tiers.size() - 1));
    }
    @Test void cardStartsCollapsedAndExpandsWithoutCoveringNotes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var m = new BurrowModel(); m.show(BurrowModel.View.OUTFITTER); m.chooseBoss("Kraken");
            var p = new BurrowPanel(m); p.setSize(225, 1200);
            p.openChronicleForTest();
            p.selectChronicleForTest("Kraken");
            int collapsed = p.combatTasksHeightForTest();
            int notes = p.chronicleNotesYForR13Test();
            int prefer = p.getPreferredSize().height;
            assertTrue(collapsed > 0);
            p.toggleCombatTasksForTest();
            assertTrue(p.combatTasksHeightForTest() > collapsed);
            assertEquals(notes + p.combatTasksHeightForTest() - collapsed, p.chronicleNotesYForR13Test(), "notes move down");
            assertEquals(prefer + p.combatTasksHeightForTest() - collapsed, p.getPreferredSize().height, "scroll height grows");
            assertTrue(p.combatTaskLinesForTest().stream().anyMatch(l -> l.contains("Unnecessary Optimisation")));
            var img = new BufferedImage(225, p.getPreferredSize().height, BufferedImage.TYPE_INT_ARGB);
            p.setSize(225, p.getPreferredSize().height); p.paint(img.getGraphics());
        });
    }
    @Test void raidsShowNoTaskCard() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var m = new BurrowModel(); m.show(BurrowModel.View.OUTFITTER); m.chooseBoss("Scurrius");
            var p = new BurrowPanel(m); p.setSize(225, 1200);
            p.openChronicleForTest();
            p.selectChronicleForTest("Verzik Vitur");
            assertEquals(0, p.combatTasksHeightForTest());
        });
    }
}
