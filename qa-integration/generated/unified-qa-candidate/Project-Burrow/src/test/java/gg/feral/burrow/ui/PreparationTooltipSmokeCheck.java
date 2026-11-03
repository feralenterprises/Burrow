package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.VerifiedOutfitterCatalog;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JToolTip;

/** Offline, dependency-free BUR-0036 regression check; Java 17 source-compatible. */
public final class PreparationTooltipSmokeCheck {
    private static void require(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        String large = "Safe but extremely long preparation notes about special phases and equipment switches ".repeat(30);
        String html = PreparationTooltip.render(
            List.of(large, large, large), List.of(large), List.of(large), List.of(large));
        require(html.startsWith("<html>"), "HTML renderer");
        JToolTip tip = new JToolTip();
        tip.setTipText(html);
        Dimension d = tip.getPreferredSize();
        require(d.width <= 270 && d.height <= 340, "Tooltip bounds: " + d);
        String escaped = PreparationTooltip.render(List.of("<img> & all"), List.of(), List.of(), List.of());
        require(escaped.contains("&lt;img&gt;") && escaped.contains("&amp;"), "HTML encoding");
        BurrowModel model = new BurrowModel();
        model.show(BurrowModel.View.OUTFITTER);
        BurrowPanel panel = new BurrowPanel(model);
        panel.clickAt(175,133+15-8); // Open read-only Chronicle: preparation no longer occupies Outfitter.
        MouseEvent preparationHover = new MouseEvent(panel, MouseEvent.MOUSE_MOVED, 0, 0, 40,
            panel.chronicleNotesYForR13Test()+65-8, 0, false);
        String preparation = panel.getToolTipText(preparationHover);
        require(preparation != null && preparation.startsWith("<html>"), "Preparation hover should be bounded HTML");
        panel.clickAt(44,130+12-8); // Gear is in the main Outfitter subview.
        MouseEvent gearHover = new MouseEvent(panel, MouseEvent.MOUSE_MOVED, 0, 0, 100,
            panel.equipmentYForR13Test()+43+15-8, 0, false);
        String gear = panel.getToolTipText(gearHover);
        require(gear != null && !gear.startsWith("<html>"), "Gear naming must remain plain-text");
        panel.clickAt(175,133+15-8); // Chronicle keeps current boss and reads its existing sourced notes.
        int profiles = 0;
        int largestWidth = 0, largestHeight = 0;
        for (String boss : BurrowModel.availableBosses()) {
            model.chooseBoss(boss);
            for (String method : model.methods()) {
                model.chooseMethod(method);
                for (Tier tier : Tier.values()) {
                    model.chooseTier(tier);
                    if (model.profile() == null) continue;
                    // A visible Wilderness banner (KBD and, since R03, every Wilderness Wiki draft)
                    // moves the preparation card down; follow the actual drawn shift, not a fixed y.
                    int shiftedY = panel.chronicleNotesYForR13Test()+65-8;
                    MouseEvent actualPrepHover = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,
                        0, 0, 40, shiftedY, 0, false);
                    String hover = panel.getToolTipText(actualPrepHover);
                    require(hover != null && hover.startsWith("<html>"), boss + " / " + method + " / " + tier);
                    tip.setTipText(hover);
                    Dimension actual = tip.getPreferredSize();
                    largestWidth = Math.max(largestWidth, actual.width);
                    largestHeight = Math.max(largestHeight, actual.height);
                    require(actual.width <= 270 && actual.height <= 340,
                        "Overflow: " + boss + " / " + method + " / " + tier + " => " + actual);
                    profiles++;
                }
            }
        }
        require(gg.feral.burrow.core.TieredOutfitterCatalog.size()==51,"Original 51 profile count unchanged");
        require(gg.feral.burrow.core.DemonEncounterLoadouts.size()==6,
            "R08 demon encounter proposals must include all six switch configurations");
        require(gg.feral.burrow.core.TieredOutfitterCatalog.totalSize()==69,
            "Expected 51 original + 2 Brutus + 10 research + 6 demon proposals");
        // Build 20: every available cumulative source profile must have a bounded tooltip.
        int expected = gg.feral.burrow.core.TieredOutfitterCatalog.totalSize() + gg.feral.burrow.core.Build20EncounterLoadouts.size();
        require(profiles == expected,
            "Every available boss/method/tier with a real profile must have bounded preparation tooltips ("+expected+" expected); got " + profiles);
        System.out.println("BUR-0036 SMOKE PASS: " + profiles + " profiles, max tooltip "
            + largestWidth + "x" + largestHeight + ", HTML escaping, panel routing, gear hover preserved");
    }
}
