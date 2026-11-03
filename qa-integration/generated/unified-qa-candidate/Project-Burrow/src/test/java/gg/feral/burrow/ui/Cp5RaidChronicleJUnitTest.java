package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** CP-5 (BQA-R12-013): raid encounters are visible in Chronicle only, clearly unsupported, never in Outfitter. */
public final class Cp5RaidChronicleJUnitTest {
    @Test void raidEncounterIsChronicleOnlyAndLeavesOutfitterUntouched() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var m = new BurrowModel(); m.show(BurrowModel.View.OUTFITTER); m.chooseBoss("Scurrius");
            var p = new BurrowPanel(m); p.setSize(225, 1200);
            assertFalse(BurrowModel.availableBosses().contains("Verzik Vitur"), "never an Outfitter boss");
            assertThrows(IllegalArgumentException.class, () -> m.chooseBoss("Verzik Vitur"));
            p.openChronicleForTest();
            p.selectChronicleForTest("Verzik Vitur");
            assertEquals("Verzik Vitur", p.chronicleRaidForTest());
            assertEquals("Scurrius", m.boss(), "Outfitter selection unchanged");
            var img = new BufferedImage(225, p.getPreferredSize().height, BufferedImage.TYPE_INT_ARGB);
            p.setSize(225, p.getPreferredSize().height); p.paint(img.getGraphics());
            assertTrue(RaidChronicle.UNSUPPORTED.contains("No raid loadouts planned in the near term"));
            p.selectChronicleForTest("Brutus");
            assertNull(p.chronicleRaidForTest(), "picking a standalone boss leaves raid view");
            assertEquals("Brutus", p.chronicleBossForUiTest());
            assertEquals("Scurrius", m.boss());
        });
    }
    @Test void everyRaidNameIsOnTheOutfitterExclusionList() {
        for (var e : RaidChronicle.all()) assertTrue(BossRegistry.excluded(e.name()), e.name());
    }
    /** CP-5 (BQA-R12-018): TD switch weapons carry a visual hint; food never does; nothing claims slot locking. */
    @Test void tormentedDemonSwitchesAreHintedVisuallyOnly() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var m = new BurrowModel(); m.show(BurrowModel.View.OUTFITTER); m.chooseBoss("Tormented Demons");
            var p = new BurrowPanel(m);
            int hinted = 0, food = 0;
            for (int i = 0; i < 28; i++) {
                var it = m.profile().inventory().get(i);
                String h = p.switchHint(i);
                if (h != null) { hinted++; assertTrue(h.contains("visual guide only") && h.contains("not locked"), h); }
                if (it != null && it.name().toLowerCase().contains("shark") || it != null && it.name().toLowerCase().contains("brew")) {
                    food++; assertNull(h, "supplies are never switch-hinted: " + it.name());
                }
            }
            assertTrue(hinted >= 1, "the TD plan carries at least one weapon/offhand switch: " + m.profile().inventory());
        });
    }
}
