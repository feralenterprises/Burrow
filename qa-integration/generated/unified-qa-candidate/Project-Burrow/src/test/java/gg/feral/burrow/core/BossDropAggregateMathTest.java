package gg.feral.burrow.core;

import gg.feral.burrow.core.BossDropAggregateMath.Outcome;
import gg.feral.burrow.core.BossDropAggregateMath.Roll;
import java.util.List;

/** BUR-0029 offline assertions on synthetic rates (no boss data is asserted here). */
public final class BossDropAggregateMathTest {
    public static void main(String[] args) {
        double p = 1.0 / 100;
        // Single item reduces to BUR-0028's fixed-rate formula.
        var single = BossDropAggregateMath.anyOf(List.of(new Roll("main", List.of(new Outcome("A", p)))), 50L);
        double ref = BossDropOddsMath.fixedRate(p, 50L).probabilityAtLeastOne().getAsDouble();
        near(single.atLeastOne().getAsDouble(), ref, 1e-12, "single item equals BUR-0028");
        // Two exclusive outcomes on one roll: per kill 2/100 exactly.
        var same = BossDropAggregateMath.anyOf(List.of(new Roll("main", List.of(new Outcome("A", p), new Outcome("B", p)))), 1L);
        near(same.perKillAny().getAsDouble(), 0.02, 1e-15, "same-roll outcomes add");
        // Two independent rolls: per kill 1-(0.99*0.99) = 0.0199, NOT 0.02.
        var indep = BossDropAggregateMath.anyOf(List.of(new Roll("main", List.of(new Outcome("A", p))),
                new Roll("tertiary", List.of(new Outcome("B", p)))), 1L);
        near(indep.perKillAny().getAsDouble(), 0.0199, 1e-15, "independent rolls multiply");
        // Bounds over n kills: max single <= any <= min(1, sum of singles).
        var many = BossDropAggregateMath.anyOf(List.of(new Roll("main", List.of(new Outcome("A", 1.0 / 5000))),
                new Roll("tertiary", List.of(new Outcome("Pet", 1.0 / 3000)))), 1000L);
        double a = BossDropOddsMath.fixedRate(1.0 / 5000, 1000L).probabilityAtLeastOne().getAsDouble();
        double b = BossDropOddsMath.fixedRate(1.0 / 3000, 1000L).probabilityAtLeastOne().getAsDouble();
        double any = many.atLeastOne().getAsDouble();
        yes(any >= Math.max(a, b) && any <= Math.min(1.0, a + b), "bounds hold");
        near(any, 1 - (1 - a) * (1 - b), 1e-12, "independent combination");
        // Zero kills is a known zero, unknowns stay unknown.
        near(BossDropAggregateMath.anyOf(List.of(new Roll("main", List.of(new Outcome("A", p)))), 0L).atLeastOne().getAsDouble(), 0.0, 0, "n=0");
        yes(!BossDropAggregateMath.anyOf(List.of(new Roll("main", List.of(new Outcome("A", null)))), 10L).isKnown(), "unknown rate");
        var noKc = BossDropAggregateMath.anyOf(List.of(new Roll("main", List.of(new Outcome("A", p)))), null);
        yes(!noKc.isKnown() && noKc.perKillAny().isPresent() && noKc.atLeastOne().isEmpty(), "unknown KC keeps per-kill only");
        // Certain outcome.
        near(BossDropAggregateMath.anyOf(List.of(new Roll("main", List.of(new Outcome("A", 1.0)))), 3L).atLeastOne().getAsDouble(), 1.0, 0, "p=1");
        // Invalid models are rejected rather than silently computed.
        bad(() -> BossDropAggregateMath.anyOf(List.of(new Roll("main", List.of(new Outcome("A", 0.7), new Outcome("B", 0.6)))), 1L), "same roll > 1");
        bad(() -> BossDropAggregateMath.anyOf(List.of(new Roll("main", List.of(new Outcome("A", p))), new Roll("t", List.of(new Outcome("A", p)))), 1L), "duplicate item");
        bad(() -> BossDropAggregateMath.anyOf(List.of(new Roll("main", List.of(new Outcome("A", -0.1)))), 1L), "negative rate");
        bad(() -> BossDropAggregateMath.anyOf(List.of(new Roll("main", List.of(new Outcome("A", p)))), -1L), "negative kills");
        bad(() -> BossDropAggregateMath.anyOf(List.of(), 1L), "no rolls");
        System.out.println("BossDropAggregateMathTest PASS (16 assertions)");
    }
    private static void near(double x, double y, double tol, String m) { if (Math.abs(x - y) > tol) throw new AssertionError(m + ": " + x + " vs " + y); }
    private static void yes(boolean b, String m) { if (!b) throw new AssertionError(m); }
    private static void bad(Runnable r, String m) { try { r.run(); throw new AssertionError("accepted " + m); } catch (IllegalArgumentException expected) { } }
}
