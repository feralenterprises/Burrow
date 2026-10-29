package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;

/** Java 17 regression main. No RuneLite classes, no JUnit, no account data. */
public final class BurrowHitboxTest {
    private static int checks;
    private static void yes(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        BurrowModel m = new BurrowModel();
        BurrowPanel p = new BurrowPanel(m);
        p.clickAt(-1, 83);
        p.clickAt(0, 83);
        p.clickAt(56, 83); // gap between HOME/PATH buttons
        yes(m.view() == BurrowModel.View.HOME, "invisible tabs at left edge or gap");
        p.clickAt(58, 83);
        yes(m.view() == BurrowModel.View.PATHFINDER, "PATH tab interior");
        p.clickAt(0, 220);
        yes(m.boss().equals("Scurrius"), "outside Pathfinder selector was clickable");
        p.clickAt(207, 220);
        yes(m.boss().equals("Scurrius"), "right edge of Pathfinder selector was clickable");
        p.clickAt(17, 220);
        yes(m.boss().equals("Vorkath"), "Vorkath selector interior");
        p.clickAt(0, 249);
        yes(m.boss().equals("Vorkath"), "outside second Pathfinder selector");
        p.clickAt(206, 249);
        yes(m.boss().equals("Scurrius"), "Scurrius selector rightmost interior");
        p.clickAt(50, 267);
        yes(m.boss().equals("Scurrius"), "gap between Pathfinder selectors");
        p.clickAt(30, 278);
        yes(m.boss().equals("Barrows") && m.plan() == null, "Barrows selector must not borrow Scurrius plan");
        p.clickAt(30, 249); // Restore the only implemented demo template before bank-geometry checks.
        yes(m.boss().equals("Scurrius") && m.plan() != null, "Scurrius demo plan restored");

        p.clickAt(114, 83);
        yes(m.view() == BurrowModel.View.OUTFITTER, "GEAR tab interior");
        p.clickAt(0, 236);
        p.clickAt(212, 236);
        yes(m.mode().equals("Recommended"), "Best in Bank invisible horizontal extension");
        p.clickAt(13, 236);
        yes(m.mode().equals("Best in Bank"), "Best in Bank leftmost interior");
        p.clickAt(20, 211);
        yes(m.mode().equals("Recommended"), "Recommended selector interior");
        p.clickAt(0, 584);
        yes(!m.plan().bankVerified(), "demo fixture invisible left extension");
        p.clickAt(20, 584);
        yes(m.plan().bankVerified(), "demo fixture interior");
        long rev = m.bank().revision();
        p.clickAt(214, 584);
        yes(m.bank().revision() == rev, "demo fixture invisible right extension");
        p.clickAt(0, 620);
        yes(m.bank() != null, "clear bank invisible left extension");
        p.clickAt(20, 620);
        yes(m.bank() == null, "clear bank button interior");

        p.clickAt(170, 83);
        yes(m.view() == BurrowModel.View.SESSIONS, "LOG tab interior");
        p.clickAt(0, 246);
        p.clickAt(212, 246);
        yes(m.demoKills() == 0, "session kill invisible horizontal extension");
        p.clickAt(13, 246);
        yes(m.demoKills() == 1, "session kill button interior");
        p.clickAt(0, 288);
        yes(m.demoKills() == 1, "session reset invisible extension");
        p.clickAt(211, 288);
        yes(m.demoKills() == 0, "session reset last interior pixel");
        p.clickAt(223, 83);
        yes(m.view() == BurrowModel.View.SESSIONS, "right-side navigation gap");
        p.clickAt(20, 73);
        yes(m.view() == BurrowModel.View.HOME, "HOME tab interior");
        p.clickAt(20, BurrowPanel.HEIGHT);
        yes(m.view() == BurrowModel.View.HOME, "out of bounds bottom click");
        System.out.println("BURROW HITBOX PASS • " + checks + " direct assertions");
    }
}
