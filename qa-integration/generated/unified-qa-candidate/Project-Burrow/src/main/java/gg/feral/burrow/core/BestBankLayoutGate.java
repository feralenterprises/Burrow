package gg.feral.burrow.core;

/**
 * Pure safety gate for publishing owned Best in Bank items to a temporary bank tag.
 * A bank-as-of-close read may inform the sidebar, but can never authorize an O tag.
 */
public final class BestBankLayoutGate {
    private BestBankLayoutGate() {}

    public static boolean permitted(long displayedGeneration, long currentGeneration,
                                    boolean sameAccount, boolean sameSession,
                                    boolean selectedBestBank, boolean bossAllowsBestBank,
                                    BankEvidence evidence) {
        return displayedGeneration == currentGeneration
            && sameAccount && sameSession && selectedBestBank && bossAllowsBestBank
            && evidence != null && evidence.planningInput() && !evidence.stale()
            && evidence.bankStillOpen();
    }

    /** Additional live-bank epoch and scan-state fences required by the bank O button. */
    public static boolean permitted(long displayedGeneration, long currentGeneration,
                                    boolean sameAccount, boolean sameSession,
                                    boolean selectedBestBank, boolean bossAllowsBestBank,
                                    BankEvidence evidence,
                                    boolean bankOpen, boolean acceptedCurrentOpenEpoch,
                                    boolean scanAccepted, boolean pendingVerification) {
        return bankOpen && acceptedCurrentOpenEpoch && scanAccepted && !pendingVerification
            && permitted(displayedGeneration, currentGeneration, sameAccount, sameSession,
                         selectedBestBank, bossAllowsBestBank, evidence);
    }
}
