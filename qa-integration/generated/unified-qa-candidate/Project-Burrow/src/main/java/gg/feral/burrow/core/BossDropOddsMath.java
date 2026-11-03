package gg.feral.burrow.core;

import java.util.OptionalDouble;

/**
 * BUR-0028: pure probability calculator for a single, independently verified,
 * fixed per-kill drop chance over statistically independent kills.
 *
 * <p>Not a drop-rate registry, kill-count observer, bad-luck protection model,
 * multi-drop calculator, or prediction of a future drop. Callers must verify
 * the rate, eligible kill population and independence assumption themselves.
 */
public final class BossDropOddsMath {
    private BossDropOddsMath() { }

    public enum State { KNOWN, UNKNOWN }
    public enum Missing { NONE, RATE, KILL_COUNT, RATE_AND_KILL_COUNT }

    /** Explicitly carries unknownness rather than substituting a zero chance. */
    public static final class Result {
        private final State state;
        private final Missing missing;
        private final OptionalDouble noDrop;
        private final OptionalDouble atLeastOne;

        private Result(State state, Missing missing, OptionalDouble noDrop,
                       OptionalDouble atLeastOne) {
            this.state = state;
            this.missing = missing;
            this.noDrop = noDrop;
            this.atLeastOne = atLeastOne;
        }

        public State state() { return state; }
        public Missing missing() { return missing; }
        public boolean isKnown() { return state == State.KNOWN; }
        public OptionalDouble probabilityNoDrop() { return noDrop; }
        public OptionalDouble probabilityAtLeastOne() { return atLeastOne; }
    }

    /**
     * For a constant p independently rolled once per eligible kill, returns
     * P(no drop after n)=(1-p)^n and P(at least one by n)=1-(1-p)^n.
     *
     * <p>Null rate or kill count produces UNKNOWN, including when n=0; a
     * verified zero is different from an unobserved count. Non-null inputs
     * must be finite, 0 <= p <= 1 and 0 <= n <= Long.MAX_VALUE.
     * The caller must not apply this model to conditional, changing, or
     * multi-roll mechanics without establishing a valid per-kill p.
     *
     * @param perKillChance verified chance of the particular drop per kill;
     *                      null when unavailable
     * @param eligibleKills verified count of comparable eligible kills;
     *                      null when unavailable
     */
    public static Result fixedRate(Double perKillChance, Long eligibleKills) {
        if (perKillChance != null
                && (!Double.isFinite(perKillChance)
                    || perKillChance < 0.0 || perKillChance > 1.0)) {
            throw new IllegalArgumentException("perKillChance must be finite and in [0,1]");
        }
        if (eligibleKills != null && eligibleKills < 0L) {
            throw new IllegalArgumentException("eligibleKills must be nonnegative");
        }

        if (perKillChance == null || eligibleKills == null) {
            Missing reason = perKillChance == null
                    ? (eligibleKills == null ? Missing.RATE_AND_KILL_COUNT : Missing.RATE)
                    : Missing.KILL_COUNT;
            return new Result(State.UNKNOWN, reason, OptionalDouble.empty(),
                    OptionalDouble.empty());
        }

        double p = perKillChance;
        long n = eligibleKills;
        if (n == 0 || p == 0.0) {
            return known(1.0, 0.0);
        }
        if (p == 1.0) {
            return known(0.0, 1.0);
        }
        if (n == 1) {
            return known(1.0 - p, p);
        }

        // Accurate for tiny p (log1p), tiny success probability (expm1),
        // and very large n. Evaluating 1 - Math.pow(1-p,n) loses precision.
        double exponent = ((double) n) * Math.log1p(-p);
        double noDrop = Math.exp(exponent);
        double atLeastOne = -Math.expm1(exponent);
        return known(noDrop, atLeastOne);
    }

    private static Result known(double noDrop, double atLeastOne) {
        return new Result(State.KNOWN, Missing.NONE,
                OptionalDouble.of(noDrop), OptionalDouble.of(atLeastOne));
    }
}
