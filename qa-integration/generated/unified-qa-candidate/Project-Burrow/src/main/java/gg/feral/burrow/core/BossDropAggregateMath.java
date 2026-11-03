package gg.feral.burrow.core;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * BUR-0029: probability of obtaining AT LEAST ONE of a selected set of drops over n eligible kills.
 *
 * <p>The caller describes the drop table structure explicitly as independent per-kill {@link Roll}s.
 * Outcomes on the SAME roll are mutually exclusive, so their chances add within that roll; separate
 * rolls (for example a main table and a tertiary roll) are independent, so their "nothing selected"
 * chances multiply. Per kill: P(none) = PRODUCT over rolls of (1 - SUM selected chances on that roll);
 * over n independent kills: P(none)^n. This avoids simply adding overlapping rates across rolls.
 *
 * <p>Not a drop-rate registry: no boss data lives here. Unknown rates or kill counts yield UNKNOWN,
 * never zero. Not valid for bad-luck protection, guaranteed milestones, or rates that change with
 * kill count; those must be modelled elsewhere (as BUR-0028 already documents for single items).
 */
public final class BossDropAggregateMath {
    private BossDropAggregateMath() {}

    /** One selected outcome: a stable item name and its verified chance on its roll (null = unknown). */
    public record Outcome(String item, Double chance) {
        public Outcome { Objects.requireNonNull(item, "item"); }
    }

    /** One independent per-kill roll and the selected, mutually exclusive outcomes on it. */
    public record Roll(String name, List<Outcome> selected) {
        public Roll {
            Objects.requireNonNull(name, "name");
            selected = List.copyOf(Objects.requireNonNull(selected, "selected"));
        }
    }

    public enum State { KNOWN, UNKNOWN }

    public record Result(State state, OptionalDouble perKillAny, OptionalDouble atLeastOne, String reason) {
        public boolean isKnown() { return state == State.KNOWN; }
    }

    public static Result anyOf(List<Roll> rolls, Long eligibleKills) {
        Objects.requireNonNull(rolls, "rolls");
        if (rolls.isEmpty()) throw new IllegalArgumentException("select at least one roll");
        Set<String> seen = new HashSet<>();
        double perKillNone = 1.0;
        boolean unknownRate = false;
        for (Roll roll : rolls) {
            if (roll.selected().isEmpty()) throw new IllegalArgumentException("roll " + roll.name() + " has no selected outcome");
            double sum = 0.0;
            for (Outcome o : roll.selected()) {
                if (!seen.add(o.item())) throw new IllegalArgumentException("item selected twice: " + o.item());
                if (o.chance() == null) { unknownRate = true; continue; }
                double p = o.chance();
                if (!Double.isFinite(p) || p < 0.0 || p > 1.0) throw new IllegalArgumentException("invalid chance for " + o.item());
                sum += p;
            }
            if (sum > 1.0 + 1e-12) throw new IllegalArgumentException("exclusive outcomes on roll " + roll.name() + " exceed 1");
            perKillNone *= Math.max(0.0, 1.0 - sum);
        }
        if (unknownRate && eligibleKills == null)
            return new Result(State.UNKNOWN, OptionalDouble.empty(), OptionalDouble.empty(), "rate and kill count unknown");
        if (unknownRate)
            return new Result(State.UNKNOWN, OptionalDouble.empty(), OptionalDouble.empty(), "a selected rate is unknown");
        double perKillAny = 1.0 - perKillNone;
        if (eligibleKills == null)
            return new Result(State.UNKNOWN, OptionalDouble.of(perKillAny), OptionalDouble.empty(), "kill count unknown");
        long n = eligibleKills;
        if (n < 0) throw new IllegalArgumentException("kill count must be >= 0");
        double none = perKillNone == 0.0 ? (n == 0 ? 1.0 : 0.0) : Math.exp(((double) n) * Math.log(perKillNone));
        return new Result(State.KNOWN, OptionalDouble.of(perKillAny), OptionalDouble.of(1.0 - none), null);
    }
}
