package gg.feral.burrow.core;

/** Standalone, dependency-free test runner for BUR-0028; JUnit wrapper below. */
public final class BossDropOddsMathTest {
    private static int assertions;
    private BossDropOddsMathTest() { }

    public static void main(String[] args) {
        assertOdds(0.0, 0L, 1.0, 0.0, 0.0);
        assertOdds(0.0, 2500L, 1.0, 0.0, 0.0);
        assertOdds(1.0, 0L, 1.0, 0.0, 0.0);
        assertOdds(1.0, 1L, 0.0, 1.0, 0.0);
        assertOdds(1.0, Long.MAX_VALUE, 0.0, 1.0, 0.0);
        assertOdds(0.5, 1L, 0.5, 0.5, 0.0);
        assertOdds(0.5, 3L, 0.125, 0.875, 2e-16);
        assertOdds(0.01, 100L, Math.pow(0.99, 100.0),
                -Math.expm1(100.0 * Math.log(0.99)), 2e-14);
        assertOdds(0.001, 1000L, 0.36769542477096373,
                0.6323045752290363, 2e-15);
        assertOdds(0.5, Long.MAX_VALUE, 0.0, 1.0, 0.0);

        // Values so small that 1 - Math.pow(1 - p, 1) incorrectly yields 0.
        assertOdds(1e-16, 1L, 1.0 - 1e-16, 1e-16, 1e-30);
        assertOdds(Double.MIN_VALUE, 1L, 1.0, Double.MIN_VALUE, 0.0);
        assertOdds(Double.MIN_VALUE, Long.MAX_VALUE, 1.0,
                Double.MIN_VALUE * (double) Long.MAX_VALUE, 1e-318);

        // Stable tiny surviving probability even for large kill counts.
        BossDropOddsMath.Result many = BossDropOddsMath.fixedRate(1e-19, Long.MAX_VALUE);
        double expected = Math.exp(-0.9223372036854775807);
        near(many.probabilityNoDrop().orElseThrow(), expected, 1e-15, "huge n");
        near(many.probabilityAtLeastOne().orElseThrow(), 1.0 - expected, 1e-15,
                "huge n complementary");

        BossDropOddsMath.Result zero = BossDropOddsMath.fixedRate(null, 0L);
        unknown(zero, BossDropOddsMath.Missing.RATE);
        unknown(BossDropOddsMath.fixedRate(0.01, null), BossDropOddsMath.Missing.KILL_COUNT);
        unknown(BossDropOddsMath.fixedRate(null, null),
                BossDropOddsMath.Missing.RATE_AND_KILL_COUNT);
        unknown(BossDropOddsMath.fixedRate(null, Long.MAX_VALUE), BossDropOddsMath.Missing.RATE);

        invalid(Double.NaN, 1L);
        invalid(Double.POSITIVE_INFINITY, 1L);
        invalid(Double.NEGATIVE_INFINITY, 1L);
        invalid(-0.1, 1L);
        invalid(1.00000001, 1L);
        invalid(0.5, -1L);
        invalid(Double.NaN, null);
        invalid(null, -1L);

        for (int n = 0; n <= 1000; n += 5) {
            var result = BossDropOddsMath.fixedRate(0.003, (long) n);
            double yes = result.probabilityAtLeastOne().orElseThrow();
            double no = result.probabilityNoDrop().orElseThrow();
            yes(yes >= 0.0 && yes <= 1.0, "bounded success");
            yes(no >= 0.0 && no <= 1.0, "bounded failure");
            near(yes + no, 1.0, 2e-15, "complementary probabilities");
            if (n > 0) {
                double previous = BossDropOddsMath.fixedRate(0.003, (long) n - 5L)
                        .probabilityAtLeastOne().orElseThrow();
                yes(yes >= previous, "monotone across kills");
            }
        }

        // Expected KC for 1/1000 is 1000 but is NOT a guaranteed drop.
        yes(BossDropOddsMath.fixedRate(0.001, 1000L)
                .probabilityNoDrop().orElseThrow() > 0.36,
                "expected kill count is not a guarantee");
        System.out.println("PASS BossDropOddsMathTest (" + assertions + " assertions)");
    }

    private static void assertOdds(double p, long n, double expectedNo,
                                   double expectedYes, double tol) {
        var actual = BossDropOddsMath.fixedRate(p, n);
        yes(actual.isKnown(), "known result");
        yes(actual.state() == BossDropOddsMath.State.KNOWN, "known enum");
        yes(actual.missing() == BossDropOddsMath.Missing.NONE, "no missing inputs");
        near(actual.probabilityNoDrop().orElseThrow(), expectedNo, tol, "no drop");
        near(actual.probabilityAtLeastOne().orElseThrow(), expectedYes, tol, "at least one");
    }

    private static void unknown(BossDropOddsMath.Result value,
                                BossDropOddsMath.Missing reason) {
        yes(value.state() == BossDropOddsMath.State.UNKNOWN, "unknown state");
        yes(!value.isKnown(), "not known");
        yes(value.missing() == reason, "unknown reason");
        yes(value.probabilityNoDrop().isEmpty(), "no false zero failure chance");
        yes(value.probabilityAtLeastOne().isEmpty(), "no false zero success chance");
    }

    private static void invalid(Double p, Long n) {
        try {
            BossDropOddsMath.fixedRate(p, n);
            throw new AssertionError("Invalid argument not rejected: " + p + "," + n);
        } catch (IllegalArgumentException expected) {
            assertions++;
        }
    }

    private static void near(double actual, double expected, double tolerance,
                             String label) {
        yes(Double.isFinite(actual) && Math.abs(actual - expected) <= tolerance,
                label + ": actual=" + actual + ", expected=" + expected);
    }

    private static void yes(boolean test, String message) {
        assertions++;
        if (!test) throw new AssertionError(message);
    }
}
