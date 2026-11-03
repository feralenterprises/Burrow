package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.TieredOutfitterCatalog;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

/** BUR-0046 test-only, no invented extra loadouts or production changes. */
public final class BUR0046CombatStyleCoverageTest {
    private static int assertions;
    private static void ok(boolean value, String why) {
        assertions++;
        if (!value) throw new AssertionError(why);
    }
    // Control rectangles are painted at relative y=345 and translated by 65 pixels.
    private static void clickStyle(BurrowPanel p, String style) {
        int x = switch (style) {
            case "Melee" -> 39;
            case "Ranged" -> 105;
            case "Magic" -> 171;
            default -> throw new IllegalArgumentException(style);
        };
        MouseEvent e = new MouseEvent(p, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
            0, x, 65 + 358, 1, false, MouseEvent.BUTTON1);
        p.dispatchEvent(e); // Actual Swing listener, not direct model.chooseMethod().
    }
    private static void audit() {
        BurrowModel model = new BurrowModel();
        BurrowPanel panel = new BurrowPanel(model);
        model.show(BurrowModel.View.OUTFITTER);
        int count = 0;
        for (String boss : VerifiedOutfitterCatalog.BOSSES) {
            model.chooseBoss(boss);
            int methods = model.methods().size();
            ok(methods==TieredOutfitterCatalog.baselineMethods(boss).size()
                + gg.feral.burrow.core.ResearchOutfitterAdditions.methods(boss).size()
                + gg.feral.burrow.core.ResearchCombatMethods.methods(boss).size(),boss+" exact baseline + researched method count");
            count += methods;
            for (Tier tier : Tier.values()) {
                model.chooseTier(tier);
                for (String style : new String[]{"Melee", "Ranged", "Magic"}) {
                    String previous = model.method();
                    boolean exists = model.methods().stream().anyMatch(m ->
                        TieredOutfitterCatalog.style(boss,m).equals(style)
                            && (TieredOutfitterCatalog.find(boss,m,tier)!=null ||
                                gg.feral.burrow.core.ResearchCombatMethods.find(boss,m)!=null));
                    clickStyle(panel,style);
                    ok(model.boss().equals(boss), boss + " stable on " + style);
                    ok(model.tier()==tier, boss + " tier stable on " + style);
                    if (exists) {
                        ok(style.equals(model.selectedStyle()), boss + " should switch to " + style);
                        ok(model.profile()!=null || gg.feral.burrow.core.ResearchCombatMethods.find(boss,model.method())!=null,
                            boss + " source-backed strategy may leave gear intentionally unavailable");
                    } else {
                        ok(model.method().equals(previous), boss + " must not invent " + style);
                    }
                }
            }
        }
        ok(count==36 && gg.feral.burrow.core.ResearchCombatMethods.size()==17,
            "17 baseline + 2 Brutus + 17 documented strategies");
        ok(TieredOutfitterCatalog.size()==51,"all 51 tiers");
        System.out.println("BUR0046 PASS JDK21/--release17 OFFLINE Swing dispatch: " + assertions + " assertions; not RuneLite native/JDK17");
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(BUR0046CombatStyleCoverageTest::audit);
    }
}
