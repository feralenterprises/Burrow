package gg.feral.burrow.core;

import gg.feral.burrow.core.RareDropTable.Drop;
import gg.feral.burrow.core.RareDropTable.Encounter;
import java.util.*;

/**
 * BQA Batch D (GOAL-02/GOAL-03): exact multi-copy, multi-item drop-goal probabilities for ONE encounter.
 *
 * <p>Model (only what {@link RareDropTable} verifies): each roll group is rolled {@code rolls} times per kill; one roll
 * yields at most one outcome of its group (mutually exclusive); different groups and tertiaries are independent.
 * The state is the vector of copies still missing (each capped at the need), propagated kill by kill with an exact
 * dynamic programme, so "4 zenyte shards", "3 synapses + 2 burning claws" (same group, at most one per kill) and
 * "5 Venator shards" are computed without a naive per-item binomial or an independence assumption.
 *
 * <p>Fail closed: a requested item that is not a verified table drop, that the table lists as not modelled
 * (hidden/invisible progression such as DT2 vestiges, kill-count guarantees, fewest-owned rules) or whose per-drop
 * quantity is unverified, makes the whole result {@link State#BLOCKED} with a CHECK reason. Nothing is approximated.
 */
public final class RecipeGoalMath {
    private RecipeGoalMath() {}
    public static final long MAX_KILLS = 2_000_000L;
    public static final int MAX_STATES = 4096;
    /** Drops whose per-drop quantity is not verified as exactly one (owner asked to verify batch drops). */
    public static final Set<String> QUANTITY_UNVERIFIED_ITEMS = Set.of("Hueycoatl hide");

    public enum State { COMPUTED, ALREADY_COMPLETE, BLOCKED }
    public record Result(State state, double probability, OptionalLong killsForTarget, OptionalDouble expectedKills, String reason) {
        static Result blocked(String why) { return new Result(State.BLOCKED, Double.NaN, OptionalLong.empty(), OptionalDouble.empty(), "CHECK: " + why); }
    }

    /** Validated, ordered requirement list: drop + copies still missing. */
    private record Need(Drop drop, int copies) {}

    /**
     * @param missing item name -> copies still missing (after ownership reconciliation); zero entries ignored
     * @param kills   kills from now for {@link Result#probability()}
     * @param target  e.g. 0.90 for the "kills for 90%" figure; NaN to skip
     */
    public static Result evaluate(Encounter enc, Map<String,Integer> missing, long kills, double target) {
        if (enc == null) return Result.blocked("encounter not in the verified table");
        if (kills < 0) throw new IllegalArgumentException("kills");
        List<Need> needs = new ArrayList<>();
        for (var e : new TreeMap<>(missing).entrySet()) {
            int c = e.getValue() == null ? 0 : e.getValue();
            if (c < 0) throw new IllegalArgumentException("negative need");
            if (c == 0) continue;
            String item = e.getKey();
            for (String nm : enc.notModelled())
                if (nm.toLowerCase(Locale.ROOT).startsWith(item.toLowerCase(Locale.ROOT)))
                    return Result.blocked(item + " uses a mechanic not modelled (" + nm + ")");
            Optional<Drop> d = enc.drops().stream().filter(x -> x.name().equalsIgnoreCase(item)).findFirst();
            if (d.isEmpty()) return Result.blocked(item + " has no verified rate for " + enc.encounter());
            if (d.get().condition() != null) return Result.blocked(item + " rate is conditional (" + d.get().condition() + ")");
            if (QUANTITY_UNVERIFIED_ITEMS.contains(d.get().name())) return Result.blocked(item + " quantity per drop not verified");
            needs.add(new Need(d.get(), c));
        }
        if (needs.isEmpty()) return new Result(State.ALREADY_COMPLETE, 1.0, OptionalLong.of(0), OptionalDouble.of(0), "Goal already met by owned items");
        int states = 1;
        for (Need n : needs) { states = Math.multiplyExact(states, n.copies() + 1); if (states > MAX_STATES) return Result.blocked("goal too large to compute exactly"); }

        // Per-roll transition data, grouped: group -> (rolls, [needIndex, p])
        Map<String,List<double[]>> groupOutcomes = new LinkedHashMap<>();
        Map<String,Integer> groupRolls = new LinkedHashMap<>();
        for (int i = 0; i < needs.size(); i++) {
            Drop d = needs.get(i).drop();
            groupOutcomes.computeIfAbsent(d.group(), g -> new ArrayList<>()).add(new double[]{i, d.perRoll()});
            Integer prior = groupRolls.putIfAbsent(d.group(), d.rolls());
            if (prior != null && prior != d.rolls()) return Result.blocked("inconsistent rolls in group " + d.group());
        }
        int[] radix = new int[needs.size()];
        for (int i = 0; i < radix.length; i++) radix[i] = needs.get(i).copies() + 1;
        final int full = states - 1; // every digit at its cap = goal complete
        int[] step = new int[radix.length]; int mul = 1;
        for (int i = 0; i < radix.length; i++) { step[i] = mul; mul *= radix[i]; }

        double[] dist = new double[states];
        dist[0] = 1.0;
        double probAtKills = kills == 0 ? 0 : Double.NaN;
        OptionalLong killsFor = OptionalLong.empty();
        double expected = 0;          // E[T] = sum_{n>=0} P(T > n)
        boolean expectedDone = false;
        boolean wantTarget = !Double.isNaN(target);
        double[] next = new double[states];
        for (long n = 0; n < MAX_KILLS; n++) {
            double notDone = 1.0 - dist[full];
            if (!expectedDone) { expected += notDone; if (notDone < 1e-12) expectedDone = true; }
            if (n == kills) probAtKills = dist[full];
            if (wantTarget && killsFor.isEmpty() && dist[full] >= target) killsFor = OptionalLong.of(n);
            if (n >= kills && expectedDone && (!wantTarget || killsFor.isPresent())) break;
            // one kill: apply every roll of every group
            for (var g : groupOutcomes.entrySet()) {
                int rolls = groupRolls.get(g.getKey());
                for (int r = 0; r < rolls; r++) {
                    Arrays.fill(next, 0);
                    for (int s = 0; s < states; s++) {
                        double ps = dist[s]; if (ps == 0) continue;
                        double rest = 1.0;
                        for (double[] o : g.getValue()) {
                            int idx = (int) o[0]; double p = o[1]; rest -= p;
                            int digit = (s / step[idx]) % radix[idx];
                            int t = digit + 1 < radix[idx] ? s + step[idx] : s;
                            next[t] += ps * p;
                        }
                        next[s] += ps * rest;
                    }
                    double[] tmp = dist; dist = next; next = tmp;
                }
            }
        }
        if (Double.isNaN(probAtKills)) return Result.blocked("kill horizon beyond " + MAX_KILLS);
        return new Result(State.COMPUTED, clamp(probAtKills), killsFor,
            expectedDone ? OptionalDouble.of(expected) : OptionalDouble.empty(),
            "Exact per-kill model from verified Wiki rates; same-group drops are mutually exclusive");
    }
    private static double clamp(double x) { return Math.max(0, Math.min(1, x)); }
}
