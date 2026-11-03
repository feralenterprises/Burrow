package gg.feral.burrow.core;

/**
 * Client-thread-only witness for recovering an accepted bank generation after a
 * RuneLite bank/inventory/equipment event. A single readable frame is not a
 * stable read, even if it matches the last accepted contents. A new event or
 * unreadable frame resets the consecutive-match count. No bank items escape.
 */
public final class BankRevalidationWitness {
    public enum Decision { WAIT, RECOVER, RETIRE }
    private BankCaptureGate.Read previous;
    private int matches;

    public void reset() { previous = null; matches = 0; }

    public Decision observe(BankEventRevalidation.Verdict verdict, BankCaptureGate.Read current) {
        if (verdict == BankEventRevalidation.Verdict.CHANGED_OR_FOREIGN) {
            reset();
            return Decision.RETIRE;
        }
        if (verdict != BankEventRevalidation.Verdict.MATCH || current == null) {
            reset();
            return Decision.WAIT;
        }
        if (previous != null && previous.sameContent(current)) matches++;
        else { previous = current; matches = 1; }
        // A zero-usable-item bank requires the same three-read stability witness
        // as initial capture, plus a readable bank tab-count witness.
        int required = current.bankUsableEmpty() ? BankScanTracker.EMPTY_BANK_STABLE_READS : BankScanTracker.STABLE_READS;
        if (current.bankUsableEmpty() && !current.tabWitnessed()) return Decision.WAIT;
        return matches >= required ? Decision.RECOVER : Decision.WAIT;
    }
}
