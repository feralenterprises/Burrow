package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import java.util.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** CP-4 (BQA-R12-010): multi-copy recipe goals are visible, ownership-reconciled and never invented. */
public final class Cp4RecipeGoalUiJUnitTest {
    private static String text(BurrowPanel p) {
        StringBuilder b = new StringBuilder();
        for (var r : p.rareDropRows()) b.append(r.text()).append(" ");
        return b.toString();
    }
    @Test void tormentedDemonSynapseGoalCountsLooseAndCraftedAndShowsExactOdds() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var m = new BurrowModel(); m.show(BurrowModel.View.OUTFITTER); m.chooseBoss("Tormented Demons");
            var p = new BurrowPanel(m); p.setSize(225, 1200); p.setRareDropExpandedForTest(true);
            String none = text(p);
            assertTrue(none.contains("owned not checked"), none);
            assertFalse(none.contains("90% within"), "no odds for copies we cannot count");
            var ev = new BankEvidence(Map.of(29580, 1, 29589, 1), Map.of(), Map.of(),
                BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT, 1L, true, false);
            p.setRecipeEvidence(ev);
            String with = text(p);
            assertTrue(with.contains("have 2 (1 loose, 1 in crafted) · need 1"), with);
            assertTrue(with.contains("All 1 more: 90% within"), with);
            p.setRecipeEvidence(ev.closed().markStale());
            assertTrue(text(p).contains("owned not checked"), "stale evidence is never counted");
        });
    }
}
