package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import java.util.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** R06: owner reference layout (Vorkath @ KC 1,000): header card, per-drop cards, Guaranteed / Unlocks. Synthetic. */
final class BqaR06RareDropCardsJUnitTest {
    @Test void vorkathAtKc1000MatchesOwnerReferenceNumbers() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var m = new BurrowModel(); m.show(BurrowModel.View.OUTFITTER); m.chooseBoss("Vorkath");
            var p = new BurrowPanel(m);
            var mem = new HashMap<String,String>();
            var st = new RareDropStore(mem::get, mem::put).recordKc(42L, "Vorkath", RareDropStore.Source.HISCORES, 1000, 1L);
            p.setRareDropState(42L, st); p.setRareDropExpandedForTest(true);
            var rows = p.rareDropRows();
            var header = rows.stream().filter(r -> r.kind() == 'k').findFirst().orElseThrow();
            assertEquals("Vorkath", header.action());
            assertTrue(header.text().startsWith("KC 1,000 · OFFICIAL HISCORES"), header.text());
            Map<String,String> pct = new HashMap<>();
            for (var r : rows) if (r.kind() == 'd') { String[] x = r.text().split("\\|", 7); pct.put(x[0], x[4] + ": " + x[5]); }
            assertEquals("Chance by your KC: 18.1%", pct.get("Draconic visage"), String.valueOf(pct));
            assertEquals("Chance by your KC: 18.1%", pct.get("Skeletal visage"));
            assertEquals("Chance by your KC: 28.4%", pct.get("Vorki"), "1-(2999/3000)^1000 = 28.35%; the reference mockup truncated to 28.3%");
            var g = rows.stream().filter(r -> r.kind() == 'g').findFirst().orElseThrow();
            assertEquals("Vorkath's head|Guaranteed at 50 KC|Reached", g.text());
            assertTrue(rows.stream().noneMatch(r -> r.text().startsWith("Not modelled: Vorkath's head")), "guarantee not duplicated");
            var img = new java.awt.image.BufferedImage(225, 2400, java.awt.image.BufferedImage.TYPE_INT_RGB);
            p.setSize(225, 2400); var gr = img.createGraphics(); p.paint(gr); gr.dispose();
        });
    }
    @Test void nextKillShowsRealDigitsNotLessThanPointOne() {
        assertEquals("0.033%", RareDropRowOdds.finePercent(1.0 / 3000));
        assertEquals("0.020%", RareDropRowOdds.finePercent(1.0 / 5000));
        assertEquals("0.10%", RareDropRowOdds.finePercent(1.0 / 1000));
        assertEquals("1.0%", RareDropRowOdds.finePercent(0.01));
        assertEquals("0.0040%", RareDropRowOdds.finePercent(1.0 / 25000));
    }
    @Test void guaranteeParserOnlyAcceptsFixedKillGuarantees() {
        assertEquals(new RareDropRowOdds.Guarantee("Kq head", 256), RareDropRowOdds.guarantee("Kq head (tattered): always on the 256th kill, once").orElseThrow());
        assertTrue(RareDropRowOdds.guarantee("Tattered pages 1/5: you receive the page you own fewest of").isEmpty());
        assertTrue(RareDropRowOdds.guarantee("Hydra's eye/fang/heart 1/181.1: always received in a fixed order, so a specific piece is not a constant rate").isEmpty());
    }
}
