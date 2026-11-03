package gg.feral.burrow.core;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * BUR-0020: pure Java 17 quantity reconciliation, with no RuneLite or persistent-state dependency.
 * The caller MUST obtain Needs from the selected, approved loadout, NEVER from ownership data.
 * This class neither scans items nor guesses which charged items are interchangeable.
 */
public final class BankQuantityReconciler {
    private BankQuantityReconciler() { }

    /** BANK, INVENTORY, and EQUIPMENT are disjoint physical storage locations. */
    public enum Location { BANK, INVENTORY, EQUIPMENT }
    public enum Coverage { COMPLETE, PARTIAL, UNKNOWN }
    public enum SourceState { FRESH_COMPLETE, FRESH_PARTIAL, NOT_SCANNED, STALE,
                              FUTURE_TIMESTAMP, ACCOUNT_UNKNOWN, FOREIGN_ACCOUNT }
    public enum Verdict { SUFFICIENT, VERIFIED_SHORT, UNKNOWN, UNRESOLVED_IDENTITY }

    /** Exact numeric item ID AND variant token. Never merge by display name alone. */
    public record Identity(int itemId, String variant) {
        public Identity {
            if (itemId <= 0 || variant == null || variant.isBlank())
                throw new IllegalArgumentException("exact item ID and variant required");
        }
    }

    /** Units contributed by each item of this exact variant, e.g. 4 doses or 1 item. */
    public record AcceptedVariant(Identity identity, long unitsPerItem) {
        public AcceptedVariant {
            Objects.requireNonNull(identity, "identity");
            if (unitsPerItem <= 0) throw new IllegalArgumentException("unitsPerItem");
        }
    }

    /** An approved-loadout requirement. Repeated loadout slots use the SAME key. */
    public record Need(String key, String label, String unit, long requiredUnits,
                       List<AcceptedVariant> acceptedVariants) {
        public Need {
            if (key == null || key.isBlank() || label == null || label.isBlank() ||
                unit == null || unit.isBlank() || requiredUnits <= 0)
                throw new IllegalArgumentException("approved loadout need");
            acceptedVariants = List.copyOf(Objects.requireNonNull(acceptedVariants, "acceptedVariants"));
            Set<Identity> unique = new HashSet<>();
            for (AcceptedVariant variant : acceptedVariants)
                if (!unique.add(variant.identity())) throw new IllegalArgumentException("duplicate accepted variant");
            // Empty acceptedVariants is allowed: unresolved Wiki ID/charge must stay UNKNOWN.
        }
    }

    /** One physical slot in one source. Stack counts are item counts, NOT dose counts. */
    public record ObservedStack(String slotKey, Identity identity, long items) {
        public ObservedStack {
            if (slotKey == null || slotKey.isBlank() || items < 0)
                throw new IllegalArgumentException("slot and nonnegative observed count required");
            Objects.requireNonNull(identity, "identity");
        }
    }

    /**
     * One source observation. COMPLETE proves unlisted identities have count zero;
     * PARTIAL proves only listed stacks; UNKNOWN proves nothing. The timestamp
     * is required for COMPLETE or PARTIAL. A missing account identity
     * is accepted but makes the snapshot untrusted; it cannot prove presence or absence.
     */
    public record Observation(Location location, String accountKey, Coverage coverage,
                              Instant observedAt, List<ObservedStack> stacks) {
        public Observation {
            Objects.requireNonNull(location, "location");
            Objects.requireNonNull(coverage, "coverage");
            stacks = List.copyOf(Objects.requireNonNull(stacks, "stacks"));
            if (coverage == Coverage.UNKNOWN && !stacks.isEmpty())
                throw new IllegalArgumentException("UNKNOWN cannot carry stock");
            if (coverage != Coverage.UNKNOWN && observedAt == null)
                throw new IllegalArgumentException("observedAt required for observed stock");
            Set<String> seenSlots = new HashSet<>();
            for (ObservedStack stack : stacks)
                if (!seenSlots.add(stack.slotKey()))
                    throw new IllegalArgumentException("replayed or duplicate physical slot: " + stack.slotKey());
        }
    }

    /** One row is an approved need, not a list inferred from a player's belongings. */
    public record Row(String key, String label, String unit, long requiredUnits,
                      Map<Location, Long> confirmedByLocation, long confirmedTotal,
                      long uncoveredUnits, Verdict verdict) {
        public Row {
            confirmedByLocation = Map.copyOf(confirmedByLocation);
        }
        /** A true shortfall only when all three sources have complete fresh coverage. */
        public boolean isVerifiedMissing() { return verdict == Verdict.VERIFIED_SHORT; }
    }

    public record Result(String accountKey, Instant evaluatedAt,
                         Map<Location, SourceState> sourceStates, List<Row> rows) {
        public Result {
            sourceStates = Map.copyOf(sourceStates);
            rows = List.copyOf(rows);
        }
    }

    /**
     * @param expectedAccountKey currently authenticated account identity, or null if unknown
     * @param approvedLoadoutNeeds ONLY selected approved-loadout counts, including repeated slots
     * @param observations zero or one observation for each physical location (not inferred scans)
     * @param now trusted comparison instant; future observations are never counted
     * @param maxAge oldest acceptable age; old sources become unknown, not verified zero
     */
    public static Result reconcile(String expectedAccountKey, List<Need> approvedLoadoutNeeds,
                                   List<Observation> observations, Instant now, Duration maxAge) {
        Objects.requireNonNull(approvedLoadoutNeeds, "approvedLoadoutNeeds");
        Objects.requireNonNull(observations, "observations");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(maxAge, "maxAge");
        if (maxAge.isNegative()) throw new IllegalArgumentException("maxAge");

        // Join DUPLICATE requirement slots before considering any observed stock.
        Map<String, Need> consolidated = new LinkedHashMap<>();
        for (Need need : approvedLoadoutNeeds) {
            Objects.requireNonNull(need, "need");
            Need prior = consolidated.get(need.key());
            if (prior == null) consolidated.put(need.key(), need);
            else {
                if (!prior.label().equals(need.label()) || !prior.unit().equals(need.unit()) ||
                    !prior.acceptedVariants().equals(need.acceptedVariants()))
                    throw new IllegalArgumentException("conflicting approved requirement key: " + need.key());
                consolidated.put(need.key(), new Need(need.key(), need.label(), need.unit(),
                    Math.addExact(prior.requiredUnits(), need.requiredUnits()), need.acceptedVariants()));
            }
        }
        // Never silently double-spend an observed item across distinct requirement keys.
        Map<Identity, String> ownerOfVariant = new HashMap<>();
        for (Need need : consolidated.values()) for (AcceptedVariant variant : need.acceptedVariants()) {
            String prior = ownerOfVariant.putIfAbsent(variant.identity(), need.key());
            if (prior != null && !prior.equals(need.key()))
                throw new IllegalArgumentException("shared item identity across distinct needs: " + prior + ", " + need.key());
        }

        EnumMap<Location, Observation> byLocation = new EnumMap<>(Location.class);
        for (Observation observation : observations) {
            Objects.requireNonNull(observation, "observation");
            if (byLocation.putIfAbsent(observation.location(), observation) != null)
                throw new IllegalArgumentException("duplicate source observation " + observation.location());
        }
        EnumMap<Location, SourceState> states = new EnumMap<>(Location.class);
        EnumMap<Location, Map<Identity, Long>> observed = new EnumMap<>(Location.class);
        for (Location location : Location.values()) {
            Observation snapshot = byLocation.get(location);
            SourceState state = determineState(expectedAccountKey, snapshot, now, maxAge);
            states.put(location, state);
            Map<Identity, Long> counts = new HashMap<>();
            if (state == SourceState.FRESH_COMPLETE || state == SourceState.FRESH_PARTIAL) {
                for (ObservedStack stack : snapshot.stacks())
                    counts.merge(stack.identity(), stack.items(), Math::addExact);
            }
            observed.put(location, counts);
        }
        boolean allComplete = states.values().stream().allMatch(s -> s == SourceState.FRESH_COMPLETE);
        List<Row> rows = new ArrayList<>();
        for (Need need : consolidated.values()) {
            EnumMap<Location, Long> counts = new EnumMap<>(Location.class);
            long total = 0;
            for (Location location : Location.values()) {
                long fromLocation = 0;
                for (AcceptedVariant variant : need.acceptedVariants()) {
                    long amount = observed.get(location).getOrDefault(variant.identity(), 0L);
                    long units = Math.multiplyExact(amount, variant.unitsPerItem());
                    fromLocation = Math.addExact(fromLocation, units);
                }
                counts.put(location, fromLocation);
                total = Math.addExact(total, fromLocation);
            }
            long uncovered = total >= need.requiredUnits() ? 0 : need.requiredUnits() - total;
            Verdict verdict = need.acceptedVariants().isEmpty() ? Verdict.UNRESOLVED_IDENTITY
                : total >= need.requiredUnits() ? Verdict.SUFFICIENT
                : allComplete ? Verdict.VERIFIED_SHORT : Verdict.UNKNOWN;
            rows.add(new Row(need.key(), need.label(), need.unit(), need.requiredUnits(),
                             counts, total, uncovered, verdict));
        }
        return new Result(expectedAccountKey, now, states, rows);
    }

    private static SourceState determineState(String account, Observation source, Instant now, Duration maxAge) {
        if (account == null || account.isBlank()) return SourceState.ACCOUNT_UNKNOWN;
        if (source == null || source.coverage() == Coverage.UNKNOWN) return SourceState.NOT_SCANNED;
        if (source.accountKey() == null || source.accountKey().isBlank()) return SourceState.ACCOUNT_UNKNOWN;
        if (!account.equals(source.accountKey())) return SourceState.FOREIGN_ACCOUNT;
        if (source.observedAt().isAfter(now)) return SourceState.FUTURE_TIMESTAMP;
        if (Duration.between(source.observedAt(), now).compareTo(maxAge) > 0) return SourceState.STALE;
        return source.coverage() == Coverage.COMPLETE ? SourceState.FRESH_COMPLETE : SourceState.FRESH_PARTIAL;
    }
}
