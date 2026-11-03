package gg.feral.burrow.core;

import gg.feral.burrow.core.BankCaptureGate.Outcome;
import gg.feral.burrow.core.BankCaptureGate.Read;
import gg.feral.burrow.core.BankCaptureGate.Result;
import java.util.*;

/**
 * BQA-901 Batch A (BF-02, BF-04, BF-05, BF-08): client-thread state machine for one login's
 * automatic bank reads. Pure Java, no RuneLite types, no clocks of its own (time is passed in).
 *
 * <ul>
 *   <li>Every attempt records a typed {@link Outcome}, an attempt count and a timestamp, so the UI can
 *       say <em>why</em> Best in Bank is unavailable instead of a bare UNKNOWN.</li>
 *   <li>Stability witness: {@value #STABLE_READS} identical consecutive ready reads; a bank with zero
 *       usable entries needs {@value #EMPTY_BANK_STABLE_READS} identical reads AND a readable tab-count
 *       witness, so a container that is still loading is not mistaken for an empty bank.</li>
 *   <li>The last accepted read survives a failed or invalidated rescan only as display provenance
 *       ({@link LastGood}); it is never handed back as current stock.</li>
 *   <li>Everything is fenced by an opaque login scope token and the authenticated account; a new scope
 *       or account wipes pending samples and the last-good record (no cross-account replay).</li>
 * </ul>
 * Never logs or exposes the account hash, item lists or names.
 */
public final class BankScanTracker {
    public static final int STABLE_READS = 2;
    public static final int EMPTY_BANK_STABLE_READS = 3;
    /** Ticks to wait before the next attempt after a transient failure. */
    public static final int RETRY_TICKS = 3;
    /** Same-sample retry after a changing read. */
    public static final int RESAMPLE_TICKS = 1;

    public enum Phase { IDLE, WAITING, ACCEPTED, VERIFYING_CHANGE, CLOSED_AS_OF, INVALIDATED }

    /** Provenance of the last accepted read: for labels only, never current stock. */
    public record LastGood(long observedAtMillis, int bankTypes, int inventoryTypes, int equipmentTypes, boolean bankEmpty) {}

    public record Status(Phase phase, Outcome lastOutcome, int attempts, long lastAttemptMillis, LastGood lastGood) {
        /** Short headline for the Outfitter Best in Bank card. */
        public String headline() {
            return switch (phase) {
                case IDLE -> "BEST IN BANK UNAVAILABLE · OPEN BANK";
                case WAITING -> "READING BANK · " + label(lastOutcome);
                case ACCEPTED -> "BANK READ";
                case VERIFYING_CHANGE -> "VERIFYING BANK CHANGE";
                case CLOSED_AS_OF -> "BANK READ · AS OF BANK CLOSE";
                case INVALIDATED -> "BANK CHANGED · RESCANNING";
            };
        }
        /** One explanatory line, including last-good provenance when the current read is missing. */
        public String detail(long nowMillis) {
            StringBuilder b = new StringBuilder();
            if ((phase == Phase.WAITING || phase == Phase.VERIFYING_CHANGE) && lastOutcome != null) {
                b.append("Waiting: ").append(lastOutcome.label());
                if (attempts > 1) b.append(" (").append(attempts).append(" tries)");
            } else if (phase == Phase.IDLE) {
                b.append("No bank read this login.");
            }
            if (lastGood != null && phase != Phase.ACCEPTED && phase != Phase.CLOSED_AS_OF) {
                if (b.length() > 0) b.append(' ');
                b.append("Last good read ").append(age(nowMillis - lastGood.observedAtMillis()))
                 .append(" ago is history only, not current stock.");
            }
            return b.toString();
        }
        private static String label(Outcome o) { return o == null ? "waiting" : o.name().replace('_', ' '); }
    }

    public record Decision(Kind kind, Read accepted, int nextDelayTicks) {
        public enum Kind { ACCEPT, WAIT, REJECT_FOREIGN }
    }

    private Object scope;
    private long account = BankCaptureGate.ACCOUNT_UNAVAILABLE;
    private volatile Phase phase = Phase.IDLE;
    private Read pending;
    private int identical;
    private Outcome lastOutcome;
    private int attempts;
    private long lastAttemptMillis;
    private LastGood lastGood;

    /** Bind to a login scope + authenticated account; a change wipes everything (BF-11/T11). */
    public void bind(Object scopeToken, long accountHash) {
        if (scopeToken == scope && accountHash == account) return;
        scope = scopeToken; account = accountHash;
        phase = Phase.IDLE; pending = null; identical = 0; lastOutcome = null; attempts = 0; lastAttemptMillis = 0; lastGood = null;
    }
    public void unbind() { bind(null, BankCaptureGate.ACCOUNT_UNAVAILABLE); }
    public boolean boundTo(Object scopeToken, long accountHash) {
        return scope != null && scope == scopeToken && account == accountHash && BankCaptureGate.accountAvailable(accountHash);
    }

    /** Bank became visible: start a new read cycle (last-good kept as provenance). */
    public void bankOpened() { phase = Phase.WAITING; pending = null; identical = 0; attempts = 0; lastOutcome = null; }

    /** Start quarantining previously accepted stock after any relevant container event. */
    public void beginVerification() {
        if (phase != Phase.ACCEPTED && phase != Phase.VERIFYING_CHANGE) return;
        phase = Phase.VERIFYING_CHANGE;
        lastOutcome = Outcome.CHANGING_SAMPLE;
        attempts = 0;
    }
    /** A partial widget read is not a mutation. Stay quarantined and try again next tick. */
    public void verificationIndeterminate(Outcome why, long nowMillis) {
        if (phase != Phase.VERIFYING_CHANGE) return;
        attempts++; lastAttemptMillis = nowMillis;
        lastOutcome = why == null ? Outcome.CHANGING_SAMPLE : why;
    }
    /** Only the unchanged, source-qualified read restores the old accepted generation. */
    public void verificationMatched() {
        if (phase == Phase.VERIFYING_CHANGE) {
            phase = Phase.ACCEPTED;
            lastOutcome = Outcome.READY_SOURCE_QUALIFIED;
            attempts = 0;
        }
    }

    /** Bank closed: an accepted read becomes "as of bank close"; an unfinished read is abandoned. */
    public void bankClosed() {
        pending = null; identical = 0;
        phase = (phase == Phase.ACCEPTED || phase == Phase.CLOSED_AS_OF) ? Phase.CLOSED_AS_OF
              : (lastGood != null ? Phase.INVALIDATED : Phase.IDLE);
    }

    /** A bank/inventory/equipment change while open: current read is no longer current. */
    public void invalidated(boolean bankStillOpen) {
        pending = null; identical = 0; attempts = 0;
        phase = bankStillOpen ? Phase.WAITING : (lastGood != null ? Phase.INVALIDATED : Phase.IDLE);
        if (bankStillOpen) lastOutcome = Outcome.CHANGING_SAMPLE;
    }

    /** Record a typed failure that happened before a container read (e.g. session not verified). */
    public Decision failedBeforeRead(Outcome why, long nowMillis) {
        if (why == Outcome.READY_SOURCE_QUALIFIED) throw new IllegalArgumentException();
        attempts++; lastAttemptMillis = nowMillis; lastOutcome = why;
        pending = null; identical = 0;
        if (phase != Phase.ACCEPTED) phase = Phase.WAITING;
        return new Decision(Decision.Kind.WAIT, null, RETRY_TICKS);
    }

    /** Offer one evaluated client read. */
    public Decision offer(Result r, long nowMillis) {
        Objects.requireNonNull(r);
        attempts++; lastAttemptMillis = nowMillis;
        if (!r.ready()) {
            lastOutcome = r.outcome(); pending = null; identical = 0;
            if (phase != Phase.ACCEPTED) phase = Phase.WAITING;
            return new Decision(Decision.Kind.WAIT, null, RETRY_TICKS);
        }
        Read read = r.read();
        if (!boundTo(scope, read.accountHash())) {
            lastOutcome = Outcome.IDENTITY_CHANGED; pending = null; identical = 0;
            return new Decision(Decision.Kind.REJECT_FOREIGN, null, RETRY_TICKS);
        }
        if (read.sameContent(pending)) identical++; else { pending = read; identical = 1; }
        int needed = read.bankUsableEmpty() ? EMPTY_BANK_STABLE_READS : STABLE_READS;
        boolean emptyNeedsWitness = read.bankUsableEmpty() && read.tabCountSum() < 0;
        if (identical < needed || emptyNeedsWitness) {
            lastOutcome = emptyNeedsWitness ? Outcome.BANK_POPULATING : Outcome.CHANGING_SAMPLE;
            phase = Phase.WAITING;
            return new Decision(Decision.Kind.WAIT, null, emptyNeedsWitness ? RETRY_TICKS : RESAMPLE_TICKS);
        }
        pending = null; identical = 0; phase = Phase.ACCEPTED; lastOutcome = Outcome.READY_SOURCE_QUALIFIED;
        lastGood = new LastGood(nowMillis, read.bank().size(), read.inventory().size(), read.equipment().size(), read.bankUsableEmpty());
        return new Decision(Decision.Kind.ACCEPT, read, 0);
    }

    public Status status() { return new Status(phase, lastOutcome, attempts, lastAttemptMillis, lastGood); }
    public Phase phase() { return phase; }

    static String age(long ms) {
        long s = Math.max(0, ms) / 1000;
        if (s < 60) return s + "s";
        long m = s / 60;
        if (m < 60) return m + "m";
        return (m / 60) + "h" + (m % 60 == 0 ? "" : " " + (m % 60) + "m");
    }
}
