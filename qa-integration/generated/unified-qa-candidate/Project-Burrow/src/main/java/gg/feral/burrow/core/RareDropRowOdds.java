package gg.feral.burrow.core;

import gg.feral.burrow.core.RareDropTable.Drop;
import gg.feral.burrow.core.RareDropTable.Encounter;
import java.util.*;

/**
 * BQA-IMPL-R05 P1: per-drop odds shown on every Rare Drops row.
 *
 * <ul>
 *   <li><b>Next kill</b>: the item's per-kill chance. For a group rolled {@code r} times per kill the per-kill chance is
 *       {@code 1-(1-p)^r} (each roll gives at most one item of the group, so the rolls are independent trials for
 *       this item). Independent of history.</li>
 *   <li><b>By your KC</b>: {@code P(at least one by n kills) = 1-(1-pKill)^n} via {@link BossDropOddsMath#fixedRate},
 *       evaluated at the player's current eligible KC. This is a historical cumulative figure, not a forecast.</li>
 *   <li>Unknown KC is {@code UNKNOWN}, never 0%. Manual KC is labelled manual. Conditional rates and duplicate-protected
 *       set tables (Moons) get no cumulative figure (not a fixed independent rate). Loot Tracker estimates are never KC.</li>
 * </ul>
 */
public final class RareDropRowOdds {
    private RareDropRowOdds() {}

    public enum Kind { CUMULATIVE, KC_UNKNOWN, NOT_FIXED_RATE }
    public record Row(Drop drop, double perKill, Kind kind, OptionalDouble byKc, OptionalLong kc,
                      RareDropStore.Source kcSource, String detail) {}

    /** Card label, e.g. "Chance by your KC" / "Chance by manual KC" / "KC UNKNOWN" / "Conditional rate". */
    public static String label(Row r) {
        return switch (r.kind()) {
            case CUMULATIVE -> r.kcSource() == RareDropStore.Source.MANUAL ? "Chance by manual KC" : "Chance by your KC";
            case KC_UNKNOWN -> "KC UNKNOWN";
            case NOT_FIXED_RATE -> r.drop().condition() != null ? "Conditional rate" : "Duplicate-protected set";
        };
    }
    /** Highlighted value: the cumulative % or "" when there is none. */
    public static String value(Row r) { return r.byKc().isPresent() ? RareDropMath.percent(r.byKc().getAsDouble()) : ""; }
    /** Secondary muted line: verified rate and the independent next-kill chance. */
    public static String rateLine(Row r) { return r.drop().rateText() + " · next kill " + finePercent(r.perKill()); }
    /** Next-kill chances are often below 0.1%; show them with enough digits instead of "<0.1%". */
    public static String finePercent(double x) {
        if (x >= 0.01) return RareDropMath.percent(x);
        if (x <= 0) return "0%";
        double pct = x * 100;
        int decimals = Math.max(2, 1 - (int) Math.floor(Math.log10(pct)));
        return String.format(Locale.ROOT, "%." + Math.min(decimals, 6) + "f%%", pct);
    }

    /** "Vorkath's head 1/50: always received on the 50th kill" -> ("Vorkath's head", 50). Empty if not a fixed-KC guarantee. */
    public record Guarantee(String item, long atKc) {}
    private static final java.util.regex.Pattern ALWAYS =
        java.util.regex.Pattern.compile("^(.*?)(?:\\s+\\d+/\\d+)?(?:\\s*\\([^)]*\\))?\\s*:\\s*always (?:received )?on the (\\d+)(?:st|nd|rd|th) kill.*$");
    public static Optional<Guarantee> guarantee(String notModelled) {
        if (notModelled == null) return Optional.empty();
        var m = ALWAYS.matcher(notModelled.trim());
        if (!m.matches()) return Optional.empty();
        return Optional.of(new Guarantee(m.group(1).trim(), Long.parseLong(m.group(2))));
    }

    public static double perKill(Drop d) {
        return -Math.expm1(d.rolls() * Math.log1p(-d.perRoll()));
    }

    public static Row row(Encounter e, Drop d, RareDropStore.Effective kc) {
        double pk = perKill(d);
        String next = "next kill " + finePercent(pk);
        if (d.condition() != null)
            return new Row(d, pk, Kind.NOT_FIXED_RATE, OptionalDouble.empty(), OptionalLong.empty(), null,
                "conditional rate · no cumulative %");
        if (e.setTableChance() > 0)
            return new Row(d, pk, Kind.NOT_FIXED_RATE, OptionalDouble.empty(), OptionalLong.empty(), null,
                "duplicate-protected set · no per-item cumulative %");
        if (kc == null || !kc.known())
            return new Row(d, pk, Kind.KC_UNKNOWN, OptionalDouble.empty(), OptionalLong.empty(), null,
                "KC UNKNOWN · " + next);
        long n = kc.value().getAsLong();
        var r = BossDropOddsMath.fixedRate(pk, n);
        double by = r.probabilityAtLeastOne().orElseThrow();
        String manual = kc.source() == RareDropStore.Source.MANUAL ? " (manual KC)" : "";
        return new Row(d, pk, Kind.CUMULATIVE, OptionalDouble.of(by), OptionalLong.of(n), kc.source(),
            "by KC " + String.format(Locale.ROOT, "%,d", n) + manual + ": " + RareDropMath.percent(by) + " · " + next);
    }
}
