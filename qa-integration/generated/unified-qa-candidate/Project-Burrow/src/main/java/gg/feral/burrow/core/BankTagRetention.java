package gg.feral.burrow.core;

/** Pure layout-lifecycle decision for an existing temporary Bank Tags O view. */
public final class BankTagRetention {
    private BankTagRetention() {}
    public enum Decision { MAINTAIN, HOLD_UNVERIFIED, CLOSE }

    public static Decision decide(boolean stillSelected, boolean sameSelectionEpoch,
                                  boolean sameSetup, boolean shownAsBestBank,
                                  boolean stillRequestingBestBank, boolean verifyingCurrentBank,
                                  boolean overridePresent, boolean sameBankGeneration) {
        if (!stillSelected || !sameSelectionEpoch || !sameSetup) return Decision.CLOSE;
        if (!shownAsBestBank) return Decision.MAINTAIN;
        // An explicit switch back to Wiki must not keep a previous owned layout.
        if (!stillRequestingBestBank) return Decision.CLOSE;
        // A new ownership generation is a real revocation, even if a separate
        // container event is still verifying. Never hold a cross-generation tag.
        if (!sameBankGeneration) return Decision.CLOSE;
        // Only the SAME current account, bank-open epoch and selection may hold
        // the temporary tag; the caller enforces those boundaries in its supplier.
        if (verifyingCurrentBank) return Decision.HOLD_UNVERIFIED;
        return overridePresent ? Decision.MAINTAIN : Decision.CLOSE;
    }
}
