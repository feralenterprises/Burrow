package gg.feral.burrow.core;

/**
 * Source-independent lease gate for a Bank Tags O withdrawal view.
 * Every input is current, source-qualified host state. The filtered bank VIEW
 * must not be reread as evidence while an approved, selected withdrawal tab is
 * active. The bank MUST be rescanned when the view is left, including after
 * no container events: a withdrawal may not always produce an observed event.
 */
public final class BankWithdrawalFreezeGate {
    private BankWithdrawalFreezeGate() {}

    public static boolean authorized(boolean ownSelectedBestBankTab,
            boolean bankRootOpen, boolean sameLoginScope,
            boolean sameAuthenticatedAccount, boolean sameAcceptedBankOpenEpoch,
            boolean acceptedAndUncontestedBankEvidence) {
        return ownSelectedBestBankTab && bankRootOpen && sameLoginScope
            && sameAuthenticatedAccount && sameAcceptedBankOpenEpoch
            && acceptedAndUncontestedBankEvidence;
    }
}
