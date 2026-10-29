package gg.feral.burrow.core;

import gg.feral.burrow.core.Outfitter.Plan;
import gg.feral.burrow.ui.BurrowPanel;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** BUR-0009 F01: one selected boss, one truthful demo plan or no plan. */
public final class BurrowBossPlanTest {
    private static int assertions;
    private static void check(boolean actual, String label) {
        assertions++;
        if (!actual) throw new AssertionError(label);
    }
    private static void checkPlan(BurrowModel model, String expectedBoss) {
        Plan p = model.plan();
        check(expectedBoss.equals(model.boss()), "selected boss " + expectedBoss);
        check(p != null, "Scurrius plan must exist");
        check(expectedBoss.equals(p.template().boss()), "plan boss provenance");
        check(p.gear().size() == 11 && p.inventory().size() == 28 && p.cells().size() == 56,
              "unchanged 11/28/17 geometry");
        check(model.outfitterSubtitle().contains("Scurrius") &&
              model.outfitterSubtitle().contains("sample"), "demo subtitle");
    }
    private static void checkUnsupported(BurrowModel model, String expectedBoss) {
        check(expectedBoss.equals(model.boss()), "selected unsupported boss " + expectedBoss);
        check(model.plan() == null, "borrowed plan or equipment from another boss");
        check(model.outfitterSubtitle().contains(expectedBoss), "Outfitter title boss mismatch");
        check(model.outfitterSubtitle().contains("no vetted loadout"), "unsupported status missing");
    }
    private static int gearTilePixel(BurrowPanel panel) {
        panel.setSize(BurrowPanel.WIDTH, BurrowPanel.HEIGHT);
        BufferedImage image = new BufferedImage(BurrowPanel.WIDTH, BurrowPanel.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try { panel.paint(g); } finally { g.dispose(); }
        // A real gear tile in the 8x7 sample grid, away from displayed headings.
        return image.getRGB(70, 354);
    }
    public static void main(String[] args) {
        BurrowModel m = new BurrowModel();
        checkPlan(m, "Scurrius");
        for (String mode : new String[]{"Recommended", "Best in Bank"}) {
            m.setMode(mode);
            check(mode.equals(m.mode()), "mode switched");
            checkPlan(m, "Scurrius");
            Plan scurrius = m.plan();
            m.chooseBoss("Vorkath");
            checkUnsupported(m, "Vorkath");
            m.chooseBoss("Barrows");
            checkUnsupported(m, "Barrows");
            check(m.plan() != scurrius, "Scurrius plan reused");
            m.chooseBoss("Scurrius");
            checkPlan(m, "Scurrius");
            check(m.plan() != scurrius, "stale Scurrius plan reused on return");
        }
        m.chooseBoss("Vorkath");
        m.loadDemoBank();
        check(m.bank() != null, "manual fixture still available");
        checkUnsupported(m, "Vorkath");
        m.setMode("Recommended");
        checkUnsupported(m, "Vorkath");
        m.chooseBoss("Barrows");
        checkUnsupported(m, "Barrows");
        m.clearBank();
        checkUnsupported(m, "Barrows");
        m.chooseBoss("Scurrius");
        checkPlan(m, "Scurrius");
        check(!m.plan().bankVerified(), "cleared demo bank resurrected");

        // Real painted-button routes, not just direct model assignments.
        BurrowPanel panel = new BurrowPanel(m);
        panel.clickAt(114, 83); // GEAR with Scurrius selected
        int sampleGearPixel = gearTilePixel(panel);
        check(sampleGearPixel == new java.awt.Color(91, 80, 59).getRGB(), "Scurrius demo gear cell must render");
        panel.clickAt(58, 83); // PATH
        panel.clickAt(25, 220); // Vorkath
        checkUnsupported(m, "Vorkath");
        panel.clickAt(114, 83); // GEAR
        check(m.view() == BurrowModel.View.OUTFITTER, "Outfitter tab");
        check(gearTilePixel(panel) != sampleGearPixel, "Vorkath must not render a Scurrius equipment tile");
        panel.clickAt(22, 238); // Best in Bank, still unsupported
        check("Best in Bank".equals(m.mode()), "Best in Bank clicked");
        checkUnsupported(m, "Vorkath");
        panel.clickAt(58, 83); // PATH
        panel.clickAt(25, 278); // Barrows
        checkUnsupported(m, "Barrows");
        panel.clickAt(114, 83); // GEAR
        panel.clickAt(22, 210); // Recommended
        check(gearTilePixel(panel) != sampleGearPixel, "Barrows must not render a Scurrius equipment tile");
        check("Recommended".equals(m.mode()), "Recommended clicked");
        checkUnsupported(m, "Barrows");
        panel.clickAt(58, 83); // PATH
        panel.clickAt(25, 249); // Scurrius
        checkPlan(m, "Scurrius");
        System.out.println("BUR-0009 F01 BOSS PLAN PASS • " + assertions + " direct assertions");
    }
}
