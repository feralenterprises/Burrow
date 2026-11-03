package gg.feral.burrow.core;

import gg.feral.burrow.core.RecipeGoalMath.State;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** BQA Batch D: multi-copy / recipe goal maths checked against closed forms (synthetic, not native). */
final class BqaRecipeGoalJUnitTest {
    static RareDropTable.Encounter enc(String n) { return RareDropTable.encounter(n).orElseThrow(() -> new AssertionError(n)); }
    static double binomAtLeast(int k, long n, double p) {
        double below = 0, term = Math.pow(1 - p, n);
        for (int i = 0; i < k; i++) { below += term; term = term * (n - i) / (i + 1) * p / (1 - p); }
        return 1 - below;
    }

    @Test void singleCopyMatchesOneMinusQPowN() {
        var r = RecipeGoalMath.evaluate(enc("Phantom Muspah"), Map.of("Venator shard", 1), 100, 0.5);
        assertEquals(State.COMPUTED, r.state());
        assertEquals(1 - Math.pow(0.99, 100), r.probability(), 1e-12);
        assertEquals(100.0, r.expectedKills().getAsDouble(), 1e-6, "geometric mean 1/p");
        assertEquals(69L, r.killsForTarget().getAsLong(), "smallest n with 1-0.99^n >= 0.5");
    }

    @Test void fiveVenatorShardsIsNegativeBinomialNotNaive() {
        var r = RecipeGoalMath.evaluate(enc("Phantom Muspah"), Map.of("Venator shard", 5), 500, 0.9);
        assertEquals(binomAtLeast(5, 500, 0.01), r.probability(), 1e-10);
        assertEquals(500.0, r.expectedKills().getAsDouble(), 1e-4, "k/p");
        double naive = Math.pow(1 - Math.pow(0.99, 500), 5);
        assertTrue(Math.abs(naive - r.probability()) > 1e-3, "naive per-item product differs");
    }

    @Test void zulrahTwoRollsPerKill() {
        var r = RecipeGoalMath.evaluate(enc("Zulrah"), Map.of("Magic fang", 1), 1, Double.NaN);
        assertEquals(1 - Math.pow(1 - 1.0 / 1024, 2), r.probability(), 1e-15);
        var two = RecipeGoalMath.evaluate(enc("Zulrah"), Map.of("Magic fang", 2), 1, Double.NaN);
        assertEquals(Math.pow(1.0 / 1024, 2), two.probability(), 1e-15, "two fangs from one kill needs both rolls");
    }

    @Test void sameGroupDropsAreMutuallyExclusivePerKill() {
        var td = enc("Tormented Demons");
        var one = RecipeGoalMath.evaluate(td, Map.of("Tormented synapse", 1, "Burning claw", 1), 1, Double.NaN);
        assertEquals(0.0, one.probability(), 0.0, "never both on one kill");
        var two = RecipeGoalMath.evaluate(td, Map.of("Tormented synapse", 1, "Burning claw", 1), 2, Double.NaN);
        double ps = 1.0 / 500, pc = 499.0 / 250000;
        assertEquals(2 * ps * pc, two.probability(), 1e-15);
        var goal = RecipeGoalMath.evaluate(td, Map.of("Tormented synapse", 3, "Burning claw", 2), 2000, 0.5);
        assertEquals(State.COMPUTED, goal.state());
        assertTrue(goal.probability() > 0 && goal.probability() < 1);
        assertTrue(goal.expectedKills().getAsDouble() > 1500, "needs ~max of two neg-binomials, > 3/ps");
    }

    @Test void blockedForUnverifiedHiddenOrUnknownMechanics() {
        assertEquals(State.BLOCKED, RecipeGoalMath.evaluate(enc("Hueycoatl"), Map.of("Hueycoatl hide", 8), 100, 0.9).state());
        assertTrue(RecipeGoalMath.evaluate(enc("Hueycoatl"), Map.of("Hueycoatl hide", 8), 100, 0.9).reason().startsWith("CHECK"));
        assertEquals(State.BLOCKED, RecipeGoalMath.evaluate(enc("Phantom Muspah"), Map.of("Dragon axe", 1), 100, 0.9).state());
        assertEquals(State.BLOCKED, RecipeGoalMath.evaluate(null, Map.of("x", 1), 1, 0.5).state());
        var dt2 = RareDropTable.all().stream().filter(e -> e.notModelled().stream().anyMatch(n -> n.contains("vestige"))).findFirst();
        assertTrue(dt2.isPresent());
        String vestige = dt2.get().notModelled().stream().filter(n -> n.contains("vestige")).findFirst().get().split(":")[0];
        assertEquals(State.BLOCKED, RecipeGoalMath.evaluate(dt2.get(), Map.of(vestige, 1), 100, 0.5).state(), "no naive Bernoulli for hidden progress");
        assertEquals(State.ALREADY_COMPLETE, RecipeGoalMath.evaluate(enc("Zulrah"), Map.of("Magic fang", 0), 10, 0.5).state());
    }

    static BankEvidence ev(Map<Integer,Integer> bank, Map<Integer,Integer> worn) {
        return new BankEvidence(bank, Map.of(), worn, BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT, 1L, true, false);
    }

    @Test void goalIsNotOwnershipAndCraftedDescendantsCountOnce() {
        var syn = RecipeGoals.find("synapse").orElseThrow();
        var none = RecipeGoals.progress(syn, 3, 29580, null, 2);
        assertEquals(RecipeGoals.OwnershipSource.NOT_OBSERVED, none.source());
        assertEquals(3, none.missing(), "a ticked goal and 2 historical drops never count as owned");
        assertFalse(none.complete());
        assertTrue(none.line().contains("owned not checked"));
        // Owner wields Emberlight + Scorching bow, holds 1 loose synapse, and 2 historical drops were observed.
        var p = RecipeGoals.progress(syn, 3, 29580, ev(Map.of(29580, 1, 29591, 1), Map.of(29589, 1)), 2);
        assertEquals(1, p.ownedComponents());
        assertEquals(2, p.inDescendants());
        assertEquals(0, p.missing());
        assertTrue(p.complete());
        assertTrue(p.notes().stream().anyMatch(n -> n.contains("history only")));
        var claws = RecipeGoals.progress(RecipeGoals.find("burning-claws").orElseThrow(), 2, 29574, ev(Map.of(29577, 1), Map.of()), 0);
        assertEquals(2, claws.inDescendants(), "Burning claws consumed 2 components");
        assertEquals(0, claws.missing());
        var stale = RecipeGoals.progress(syn, 3, 29580, ev(Map.of(29580, 3), Map.of()).markStale(), 0);
        assertEquals(RecipeGoals.OwnershipSource.NOT_OBSERVED, stale.source(), "stale bank never counts");
    }

    @Test void configurableQuantityFeedsExactMath() {
        var zen = RecipeGoals.find("zenyte").orElseThrow();
        var prog = RecipeGoals.progress(zen, 4, 19529, ev(Map.of(19529, 1, 19547, 1), Map.of()), 0);
        assertEquals(2, prog.missing());
        var r = RecipeGoalMath.evaluate(enc(zen.encounter()), Map.of(zen.component(), prog.missing()), 600, 0.9);
        assertEquals(binomAtLeast(2, 600, 1.0 / 300), r.probability(), 1e-10);
        assertFalse(zen.unreconciled().isEmpty(), "unverified descendant IDs are surfaced, not guessed");
        assertTrue(RecipeGoals.find("venator").orElseThrow().descendants().isEmpty());
    }
}
