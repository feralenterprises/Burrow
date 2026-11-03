package gg.feral.burrow.core;

import java.util.*;

/**
 * BQA-901 Batch A (BF-02, BF-03, BF-04, BF-07): pure, RuneLite-free decision logic for one
 * client-thread BANK + INVENTORY + EQUIPMENT read.
 *
 * <p>The RuneLite adapter only copies raw facts into a {@link RawRead}; every accept/reject rule
 * lives here so it can be unit-tested without a client. Every rejection carries a typed
 * {@link Outcome} instead of a bare {@code Optional.empty()}.
 *
 * <p>Rules (all fail closed, nothing is ever turned into absence evidence):
 * <ul>
 *   <li>Only RuneLite's documented sentinel {@code -1} means "account unavailable". Any other signed
 *       {@code long}, including negative values, is a legitimate opaque account hash (BF-03).</li>
 *   <li>Account identity must be identical before and after the container reads.</li>
 *   <li>The BANK container must be at least as long as the sum of the nine tab-count varbits
 *       (consistency check only, NOT proof of server-side completeness: BF-06).</li>
 *   <li>Any bank entry whose item definition is unreadable or mismatched blocks the whole read.</li>
 *   <li>Placeholders, noted entries, filler (20594), zero/negative quantities are filtered.</li>
 *   <li>A bank with zero usable entries is a valid read (BF-04) but is flagged
 *       {@link Read#bankUsableEmpty()}; {@link BankScanTracker} requires a longer stability witness.</li>
 * </ul>
 */
public final class BankCaptureGate {
    private BankCaptureGate() {}

    /** RuneLite documents {@code -1} as "not logged in / unavailable"; nothing else is a sentinel. */
    public static final long ACCOUNT_UNAVAILABLE = -1L;
    public static final int BANK_FILLER_ID = 20594;
    public static final int MAX_BANK_ENTRIES = 4096;
    public static final int MAX_CARRIED_ENTRIES = 40;

    public static boolean accountAvailable(long hash) { return hash != ACCOUNT_UNAVAILABLE; }

    /** Typed result of a single read attempt. */
    public enum Outcome {
        READY_SOURCE_QUALIFIED(false, "bank read"),
        NOT_LOGGED_IN(true, "not logged in"),
        WIDGET_NOT_READY(true, "bank interface still loading"),
        PIN_OPEN(true, "bank PIN open"),
        IDENTITY_UNAVAILABLE(true, "account identity unavailable"),
        IDENTITY_CHANGED(true, "account changed during read"),
        SESSION_NOT_READY(true, "login not verified yet"),
        BANK_CONTAINER_UNAVAILABLE(true, "bank items not received yet"),
        INVENTORY_UNAVAILABLE(true, "inventory not received yet"),
        EQUIPMENT_UNAVAILABLE(true, "worn equipment not received yet"),
        BANK_POPULATING(true, "bank still populating (tab counts ahead of items)"),
        BAD_BANK_ITEM_DEFINITION(true, "an item definition was unreadable"),
        CONTAINER_TOO_LARGE(false, "container size outside safe bounds"),
        CHANGING_SAMPLE(true, "bank contents still changing"),
        CLIENT_READ_EXCEPTION(true, "client read error");
        private final boolean waitState;
        private final String label;
        Outcome(boolean waitState, String label) { this.waitState = waitState; this.label = label; }
        /** True for transient conditions that a later tick may resolve. */
        public boolean waitState() { return waitState; }
        public String label() { return label; }
    }

    /** One raw ItemContainer slot as copied from the client. */
    public record RawItem(int id, int quantity, boolean definitionOk, boolean placeholder, boolean noted, String name) {
        public RawItem(int id, int quantity, boolean definitionOk, boolean placeholder, boolean noted) {
            this(id, quantity, definitionOk, placeholder, noted, null);
        }
        public static RawItem carried(int id, int quantity) { return new RawItem(id, quantity, true, false, false); }
        public static RawItem bank(int id, int quantity) { return new RawItem(id, quantity, true, false, false); }
    }

    /** Raw facts gathered on the client thread. {@code null} list = container not available. */
    public record RawRead(boolean loggedIn, boolean bankRootVisible, boolean bankItemsVisible, boolean pinVisible,
                          long accountBefore, long accountAfter,
                          List<RawItem> bank, List<RawItem> inventory, List<RawItem> equipment,
                          int tabCountSum, boolean readException,
                          boolean inventoryContainerExists, boolean equipmentContainerExists) {
        /** Legacy form: a null carried list means "container exists but items unreadable" (fail closed). */
        public RawRead(boolean loggedIn, boolean bankRootVisible, boolean bankItemsVisible, boolean pinVisible,
                       long accountBefore, long accountAfter, List<RawItem> bank, List<RawItem> inventory,
                       List<RawItem> equipment, int tabCountSum, boolean readException) {
            this(loggedIn, bankRootVisible, bankItemsVisible, pinVisible, accountBefore, accountAfter, bank, inventory,
                 equipment, tabCountSum, readException, true, true);
        }
        public RawRead {
            bank = bank == null ? null : List.copyOf(bank);
            inventory = inventory == null ? null : List.copyOf(inventory);
            equipment = equipment == null ? null : List.copyOf(equipment);
        }
        public boolean bankVisible() { return loggedIn && bankRootVisible && bankItemsVisible && !pinVisible; }
    }

    /** A source-qualified read: positive quantities only, opaque account key kept in memory only. */
    public record Read(long accountHash, Map<Integer,Integer> bank, Map<Integer,Integer> inventory,
                       Map<Integer,Integer> equipment, int rawBankSlots, int tabCountSum,
                       boolean inventoryReported, boolean equipmentReported, Map<Integer,String> names) {
        public Read {
            bank = Map.copyOf(bank); inventory = Map.copyOf(inventory); equipment = Map.copyOf(equipment);
            names = names == null ? Map.of() : Map.copyOf(names);
        }
        public Read(long accountHash, Map<Integer,Integer> bank, Map<Integer,Integer> inventory,
                    Map<Integer,Integer> equipment, int rawBankSlots, int tabCountSum) {
            this(accountHash, bank, inventory, equipment, rawBankSlots, tabCountSum, true, true, Map.of());
        }
        /** e.g. "worn: none reported by client" when the EQUIPMENT container did not exist (nothing worn yet). */
        public String carriedNote() {
            if (inventoryReported && equipmentReported) return "";
            if (!inventoryReported && !equipmentReported) return "inventory and worn: none reported by client";
            return (inventoryReported ? "worn" : "inventory") + ": none reported by client";
        }
        public boolean tabWitnessed() { return tabCountSum >= 0 && rawBankSlots >= tabCountSum; }
        public boolean bankUsableEmpty() { return bank.isEmpty(); }
        public Map<Integer,Integer> combined() {
            Map<Integer,Integer> r = new HashMap<>(bank);
            inventory.forEach((k, v) -> r.merge(k, v, Math::addExact));
            equipment.forEach((k, v) -> r.merge(k, v, Math::addExact));
            return Map.copyOf(r);
        }
        /** Stability witness: identical account, raw size, tab sum and all three maps. */
        public boolean sameContent(Read other) {
            return other != null && other.accountHash == accountHash && other.rawBankSlots == rawBankSlots
                && other.tabCountSum == tabCountSum
                && other.inventoryReported == inventoryReported && other.equipmentReported == equipmentReported
                && other.bank.equals(bank) && other.inventory.equals(inventory) && other.equipment.equals(equipment);
        }
    }

    public record Result(Outcome outcome, Read read) {
        public Result {
            Objects.requireNonNull(outcome);
            if ((outcome == Outcome.READY_SOURCE_QUALIFIED) != (read != null))
                throw new IllegalArgumentException("read present iff READY");
        }
        static Result fail(Outcome o) { return new Result(o, null); }
        public boolean ready() { return read != null; }
    }

    public static Result evaluate(RawRead raw) {
        if (raw == null || raw.readException()) return Result.fail(Outcome.CLIENT_READ_EXCEPTION);
        if (!raw.loggedIn()) return Result.fail(Outcome.NOT_LOGGED_IN);
        if (raw.pinVisible()) return Result.fail(Outcome.PIN_OPEN);
        if (!raw.bankRootVisible() || !raw.bankItemsVisible()) return Result.fail(Outcome.WIDGET_NOT_READY);
        if (!accountAvailable(raw.accountBefore())) return Result.fail(Outcome.IDENTITY_UNAVAILABLE);
        if (raw.accountAfter() != raw.accountBefore()) return Result.fail(Outcome.IDENTITY_CHANGED);
        if (raw.bank() == null) return Result.fail(Outcome.BANK_CONTAINER_UNAVAILABLE);
        // R05 (owner native QA: "recommendations only appear after I equip something"): RuneLite has no EQUIPMENT
        // ItemContainer until the server sends one, which may not happen while nothing is worn. A missing CARRIED
        // container is therefore read as "no carried items reported" (positive-only: it never asserts that an item is
        // missing, it just adds nothing) and is labelled in the trust line. The BANK container stays mandatory.
        if (raw.inventory() == null && raw.inventoryContainerExists()) return Result.fail(Outcome.INVENTORY_UNAVAILABLE);
        if (raw.equipment() == null && raw.equipmentContainerExists()) return Result.fail(Outcome.EQUIPMENT_UNAVAILABLE);
        boolean invReported = raw.inventory() != null, eqReported = raw.equipment() != null;
        List<RawItem> invRaw = invReported ? raw.inventory() : List.of(), eqRaw = eqReported ? raw.equipment() : List.of();
        if (raw.bank().size() > MAX_BANK_ENTRIES || invRaw.size() > MAX_CARRIED_ENTRIES
            || eqRaw.size() > MAX_CARRIED_ENTRIES) return Result.fail(Outcome.CONTAINER_TOO_LARGE);
        int rawSlots = raw.bank().size();
        if (raw.tabCountSum() >= 0 && rawSlots < raw.tabCountSum()) return Result.fail(Outcome.BANK_POPULATING);
        Map<Integer,Integer> bank = new HashMap<>();
        Map<Integer,String> names = new HashMap<>();
        for (RawItem it : raw.bank()) {
            if (it == null || it.id() <= 0 || it.quantity() <= 0 || it.id() == BANK_FILLER_ID) continue;
            if (!it.definitionOk()) return Result.fail(Outcome.BAD_BANK_ITEM_DEFINITION);
            if (it.placeholder() || it.noted()) continue; // never an equippable / withdrawable trip item
            bank.merge(it.id(), it.quantity(), Math::addExact);
            if (it.name() != null && !it.name().isBlank()) names.putIfAbsent(it.id(), it.name());
        }
        Map<Integer,Integer> inv = carried(invRaw, names), equip = carried(eqRaw, names);
        return new Result(Outcome.READY_SOURCE_QUALIFIED,
            new Read(raw.accountBefore(), bank, inv, equip, rawSlots, raw.tabCountSum(), invReported, eqReported, names));
    }

    private static Map<Integer,Integer> carried(List<RawItem> items, Map<Integer,String> names) {
        Map<Integer,Integer> out = new HashMap<>();
        for (RawItem it : items) {
            if (it == null || it.id() <= 0 || it.quantity() <= 0 || it.id() == BANK_FILLER_ID) continue;
            out.merge(it.id(), it.quantity(), Math::addExact);
            if (it.name() != null && !it.name().isBlank()) names.putIfAbsent(it.id(), it.name());
        }
        return out;
    }
}
