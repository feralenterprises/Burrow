package gg.feral.burrow.core;

/**
 * Classifies a bank event against the last source-qualified read. A temporarily hidden
 * Bank Tags widget is not evidence of changed stock. The caller must quarantine owned
 * layouts until MATCH; CHANGED_OR_FOREIGN must retire old evidence. Pure Java.
 */
public final class BankEventRevalidation {
    private BankEventRevalidation() {}
    public enum Verdict { MATCH, CHANGED_OR_FOREIGN, INDETERMINATE }

    public static Verdict classify(BankCaptureGate.Read accepted,
                                   BankCaptureGate.Result fresh,
                                   Object acceptedScope, Object currentScope,
                                   long currentAccount) {
        if (accepted == null || acceptedScope == null || acceptedScope != currentScope)
            return Verdict.CHANGED_OR_FOREIGN;
        // An unavailable account identity is not a verified different account.
        if (!BankCaptureGate.accountAvailable(currentAccount)) return Verdict.INDETERMINATE;
        if (accepted.accountHash() != currentAccount) return Verdict.CHANGED_OR_FOREIGN;
        if (fresh == null) return Verdict.INDETERMINATE;
        if (!fresh.ready()) {
            return fresh.outcome() == BankCaptureGate.Outcome.IDENTITY_CHANGED
                    ? Verdict.CHANGED_OR_FOREIGN : Verdict.INDETERMINATE;
        }
        return accepted.sameContent(fresh.read()) ? Verdict.MATCH : Verdict.CHANGED_OR_FOREIGN;
    }

    /** Compatibility helper: a non-ready result never returns true. */
    public static boolean unchanged(BankCaptureGate.Read accepted,
                                    BankCaptureGate.Result fresh,
                                    Object acceptedScope, Object currentScope,
                                    long currentAccount) {
        return classify(accepted, fresh, acceptedScope, currentScope, currentAccount) == Verdict.MATCH;
    }
}
