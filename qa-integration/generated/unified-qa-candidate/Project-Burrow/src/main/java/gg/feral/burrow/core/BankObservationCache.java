package gg.feral.burrow.core;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * BUR-0023/0042: isolated, in-memory, read-only bank observation/cache core.
 * No RuneLite event subscriptions, bank actions, UI, persistence or Best in Bank.
 *
 * This class validates attestations but CANNOT verify RuneLite's full-bank readiness
 * signal itself. A future client-thread adapter must prove each attestation, and
 * MUST NOT publish a native snapshot until the full-load gate is resolved.
 */
public final class BankObservationCache {
    public enum Source { RUNELITE_BANK_OPEN, DEV_MANUAL_SCAN, SYNTHETIC_TEST }
    public enum Status { NO_IDENTITY, UNKNOWN, VERIFIED_AS_OF, HISTORICAL_AS_OF,
                         SCAN_FAILED_LAST_VERIFIED }
    public enum Freshness { NO_DATA, SAME_LOGIN_AS_OF, PRIOR_LOGIN_AS_OF,
                            WORLD_HOP_STALE, AGE_STALE, FAILED_RESCAN }

    // Standard RuneLite inventory and equipment ItemContainer physical slot counts.
    // Bank capacity varies by account and client state; its authoritative bound
    // must be established by the future native adapter, not guessed here.
    private static final int INVENTORY_SLOTS = 28;
    private static final int EQUIPMENT_SLOTS = 14;

    /** Exact raw RuneLite item ID. Do not normalize charged or imbued variants here. */
    public record Slot(int slot, int itemId, long quantity) {
        public Slot {
            if (slot < 0 || itemId <= 0 || quantity <= 0)
                throw new IllegalArgumentException("filled slot, raw item ID and positive quantity required");
        }
    }

    /** Three disjoint complete containers. Empty lists are legitimate only with full-load proof. */
    public record Containers(List<Slot> bank, List<Slot> inventory, List<Slot> equipment) {
        public Containers {
            bank = checkedSlots(bank, 0); // No static maximum for a variable-size bank.
            inventory = checkedSlots(inventory, INVENTORY_SLOTS);
            equipment = checkedSlots(equipment, EQUIPMENT_SLOTS);
        }
    }

    /**
     * Caller-supplied external proof: no boolean can independently prove native
     * readiness. Host adapter must establish these conditions on the client thread.
     */
    public record Attestation(Source source, boolean accountStableBeforeAndAfter,
                              boolean verifiedFullBankLoaded, boolean bankOpenAndReady,
                              boolean completeInventory, boolean completeEquipment,
                              boolean loggedInAndIdentified, boolean noPinOrLoadingUi) {
        public Attestation {
            Objects.requireNonNull(source, "source");
        }
        public boolean complete() {
            return accountStableBeforeAndAfter && verifiedFullBankLoaded && bankOpenAndReady
                && completeInventory && completeEquipment && loggedInAndIdentified
                && noPinOrLoadingUi;
        }
    }

    /** One detached observation attempt. The slot lists are never mutated or merged in place. */
    public record Observation(Instant observedAt, Attestation attestation, Containers containers) {
        public Observation {
            Objects.requireNonNull(observedAt, "observedAt");
            Objects.requireNonNull(attestation, "attestation");
            Objects.requireNonNull(containers, "containers");
        }
    }

    /** Unforgeable-by-construction generation handle issued only by the cache. */
    public static final class Token {
        private final String accountKey;
        private final String epoch;
        private final long generation;
        private final Instant bankOpenedAt;
        private Token(String accountKey, String epoch, long generation, Instant bankOpenedAt) {
            this.accountKey = accountKey;
            this.epoch = epoch;
            this.generation = generation;
            this.bankOpenedAt = bankOpenedAt;
        }
        public String accountKey() { return accountKey; }
        public String epoch() { return epoch; }
        public long generation() { return generation; }
        public Instant bankOpenedAt() { return bankOpenedAt; }
    }

    /** The three raw source counts remain separate; total counts physical items, not doses. */
    public record Counts(long bank, long inventory, long equipment) {
        public Counts {
            if (bank < 0 || inventory < 0 || equipment < 0)
                throw new IllegalArgumentException("negative count");
            Math.addExact(Math.addExact(bank, inventory), equipment);
        }
        public long total() { return Math.addExact(Math.addExact(bank, inventory), equipment); }
    }

    /** Immutable successful snapshot. Counts must match the detached containers exactly. */
    public record Snapshot(String accountKey, String epoch, long generation, long revision,
                           Instant bankOpenedAt, Instant observedAt, Source source,
                           Containers containers, Map<Integer, Counts> exactRawCounts) {
        public Snapshot {
            checkedIdentity(accountKey, epoch);
            if (generation < 1 || revision < 1 || bankOpenedAt == null || observedAt == null
                || source == null || observedAt.isBefore(bankOpenedAt))
                throw new IllegalArgumentException("invalid successful observation metadata");
            Objects.requireNonNull(containers, "containers");
            exactRawCounts = Map.copyOf(Objects.requireNonNull(exactRawCounts, "exactRawCounts"));
            // Public record construction must not permit invented or contradictory
            // stock counts; this does not authenticate a caller's claimed source.
            if (!exactRawCounts.equals(aggregate(containers)))
                throw new IllegalArgumentException("snapshot counts do not match containers");
        }
        /** A zero is verified only AS OF observedAt, never proof of current bank state. */
        public Counts rawCountAsOf(int itemId) {
            if (itemId <= 0) throw new IllegalArgumentException("itemId");
            return exactRawCounts.getOrDefault(itemId, new Counts(0, 0, 0));
        }
    }

    /** A caller MUST inspect status/freshness/source before showing any AS-OF counts. */
    public record View(Status status, Freshness freshness, Snapshot snapshot) {
        public View {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(freshness, "freshness");
        }
        public Optional<Counts> rawCountAsOf(int itemId) {
            if (itemId <= 0) throw new IllegalArgumentException("itemId");
            return snapshot == null ? Optional.empty() : Optional.of(snapshot.rawCountAsOf(itemId));
        }
    }

    private final Map<String, Snapshot> lastSuccessfulByAccount = new HashMap<>();
    private final boolean allowSyntheticTests;
    private final boolean allowDeveloperManual;
    private String accountKey;
    private String epoch;
    private long nextGeneration;
    private Token activeToken;
    private Snapshot currentLoginSuccess;
    private boolean failedScan;
    private boolean hoppedSinceSuccess;

    /** Production-default: synthetic and dev-manual observations rejected. */
    public BankObservationCache() { this(false, false); }
    private BankObservationCache(boolean synthetic, boolean developerManual) {
        allowSyntheticTests = synthetic;
        allowDeveloperManual = developerManual;
    }
    /** Explicit fixture-only instance. It MUST NOT be installed in a live plugin. */
    public static BankObservationCache forSyntheticTests() {
        return new BankObservationCache(true, false);
    }

    /** An opaque pre-verified per-install keyed/HMAC identity, never a username or raw hash. */
    public synchronized void beginLogin(String verifiedAccountKey, String freshEpoch) {
        checkedIdentity(verifiedAccountKey, freshEpoch);
        accountKey = verifiedAccountKey;
        epoch = freshEpoch;
        nextGeneration = 0;
        activeToken = null;
        currentLoginSuccess = null;
        failedScan = false;
        hoppedSinceSuccess = false;
    }

    /** Call on logout, unknown identity, or switching away from authenticated character. */
    public synchronized void identityUnknown() {
        accountKey = null;
        epoch = null;
        activeToken = null;
        currentLoginSuccess = null;
        failedScan = false;
        hoppedSinceSuccess = false;
    }

    /** Invalidates any unfinished observation; successful prior data stays explicitly AS OF. */
    public synchronized void worldHop() {
        activeToken = null;
        if (currentLoginSuccess != null) hoppedSinceSuccess = true;
        failedScan = false;
    }

    /** Called ONLY for a real bank-open generation in a future client-thread adapter. */
    public synchronized Token beginBankOpen(Instant bankOpenedAt) {
        Objects.requireNonNull(bankOpenedAt, "bankOpenedAt");
        if (accountKey == null) throw new IllegalStateException("identity unavailable");
        nextGeneration = Math.addExact(nextGeneration, 1);
        activeToken = new Token(accountKey, epoch, nextGeneration, bankOpenedAt);
        failedScan = false;
        return activeToken;
    }

    /** Closing the standard bank invalidates pending observations. Does not fabricate zero. */
    public synchronized void bankClosed() { activeToken = null; }

    /**
     * Transactional commit. No partial updates, stale generations, or repeated
     * commits. Invalid data marks this generation failed and retains ONLY the
     * same account's last verified snapshot (if one exists).
     */
    public synchronized boolean observe(Token token, Observation observation, Instant now) {
        Objects.requireNonNull(now, "now");
        if (!isActive(token)) return false;
        // A failed capture consumes this bank-open generation, avoiding replay.
        activeToken = null;
        try {
            if (observation == null || !observation.attestation().complete()
                || observation.observedAt().isBefore(token.bankOpenedAt())
                || observation.observedAt().isAfter(now)
                || token.bankOpenedAt().isAfter(now)) {
                failedScan = true;
                return false;
            }
            Source source = observation.attestation().source();
            if ((source == Source.SYNTHETIC_TEST && !allowSyntheticTests)
                || (source == Source.DEV_MANUAL_SCAN && !allowDeveloperManual)) {
                failedScan = true;
                return false;
            }
            Map<Integer, Counts> counts = aggregate(observation.containers());
            Snapshot previous = lastSuccessfulByAccount.get(accountKey);
            long revision = previous == null ? 1 : Math.addExact(previous.revision(), 1);
            Snapshot next = new Snapshot(accountKey, epoch, token.generation(), revision,
                token.bankOpenedAt(), observation.observedAt(), source,
                observation.containers(), counts);
            // Commit only after ALL validation, aggregation and allocation succeed.
            lastSuccessfulByAccount.put(accountKey, next);
            currentLoginSuccess = next;
            failedScan = false;
            hoppedSinceSuccess = false;
            return true;
        } catch (IllegalArgumentException | ArithmeticException ex) {
            failedScan = true;
            return false;
        }
    }

    /** An explicit failed scan/read; stale tokens cannot alter the active account's state. */
    public synchronized boolean scanFailed(Token token) {
        if (!isActive(token)) return false;
        activeToken = null;
        failedScan = true;
        return true;
    }

    /** Read-only view. Historical/stale snapshots are usable ONLY as labeled AS-OF data. */
    public synchronized View view(Instant now, Duration maximumAge) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(maximumAge, "maximumAge");
        if (maximumAge.isNegative()) throw new IllegalArgumentException("maximumAge");
        if (accountKey == null) return new View(Status.NO_IDENTITY, Freshness.NO_DATA, null);
        Snapshot snapshot = currentLoginSuccess != null
            ? currentLoginSuccess : lastSuccessfulByAccount.get(accountKey);
        if (snapshot == null) return new View(Status.UNKNOWN, Freshness.NO_DATA, null);
        // Never display time-travel observations. E.g. wall-clock rolled backward.
        if (snapshot.observedAt().isAfter(now)) return new View(Status.UNKNOWN, Freshness.NO_DATA, null);
        if (failedScan) return new View(Status.SCAN_FAILED_LAST_VERIFIED,
                                        Freshness.FAILED_RESCAN, snapshot);
        if (Duration.between(snapshot.observedAt(), now).compareTo(maximumAge) > 0)
            return new View(currentLoginSuccess == null ? Status.HISTORICAL_AS_OF : Status.VERIFIED_AS_OF,
                            Freshness.AGE_STALE, snapshot);
        if (currentLoginSuccess == null)
            return new View(Status.HISTORICAL_AS_OF, Freshness.PRIOR_LOGIN_AS_OF, snapshot);
        return new View(Status.VERIFIED_AS_OF,
            hoppedSinceSuccess ? Freshness.WORLD_HOP_STALE : Freshness.SAME_LOGIN_AS_OF,
            snapshot);
    }

    /** Explicit plugin shutdown cleanup. No disk writes or cross-restart persistence. */
    public synchronized void clearAll() {
        identityUnknown();
        lastSuccessfulByAccount.clear();
        nextGeneration = 0;
    }

    private boolean isActive(Token token) {
        return token != null && token == activeToken && accountKey != null
            && token.accountKey().equals(accountKey) && token.epoch().equals(epoch);
    }

    private static void checkedIdentity(String accountKey, String epoch) {
        if (accountKey == null || !accountKey.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("opaque HMAC-SHA256 account key required");
        if (epoch == null || epoch.length() < 8 || epoch.length() > 128 || epoch.isBlank())
            throw new IllegalArgumentException("fresh login epoch required");
    }

    private static List<Slot> checkedSlots(List<Slot> raw, int upperExclusive) {
        if (raw == null) throw new IllegalArgumentException("complete container must not be null");
        List<Slot> copied = List.copyOf(raw);
        Set<Integer> seen = new HashSet<>();
        for (Slot slot : copied) {
            if (upperExclusive > 0 && slot.slot() >= upperExclusive)
                throw new IllegalArgumentException("physical slot index out of range: " + slot.slot());
            if (!seen.add(slot.slot()))
                throw new IllegalArgumentException("duplicate physical slot " + slot.slot());
        }
        return copied;
    }

    private static Map<Integer, Counts> aggregate(Containers containers) {
        Map<Integer, long[]> scratch = new HashMap<>();
        putStacks(scratch, containers.bank(), 0);
        putStacks(scratch, containers.inventory(), 1);
        putStacks(scratch, containers.equipment(), 2);
        Map<Integer, Counts> result = new HashMap<>();
        for (Map.Entry<Integer, long[]> e : scratch.entrySet()) {
            long[] c = e.getValue();
            result.put(e.getKey(), new Counts(c[0], c[1], c[2]));
        }
        return Map.copyOf(result);
    }

    private static void putStacks(Map<Integer, long[]> values, List<Slot> items, int source) {
        for (Slot slot : items) {
            long[] c = values.computeIfAbsent(slot.itemId(), key -> new long[3]);
            c[source] = Math.addExact(c[source], slot.quantity());
        }
    }
}
