package gg.feral.burrow.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BankWithdrawalFreezeGateJUnitTest {
    @Test void selectedVerifiedTabMayFreezeRedrawsAndWithdrawals() {
        assertTrue(BankWithdrawalFreezeGate.authorized(true,true,true,true,true,true));
    }
    @Test void anyExpiredBoundaryMustReleaseTheWithdrawalLease() {
        for (int missing=0;missing<6;missing++) {
            boolean[] valid={true,true,true,true,true,true};
            valid[missing]=false;
            assertFalse(BankWithdrawalFreezeGate.authorized(
                valid[0],valid[1],valid[2],valid[3],valid[4],valid[5]),
                "missing boundary index "+missing);
        }
    }
    @Test void otherBankTabsAndUnverifiedPlansNeverFreezeEvidence() {
        assertFalse(BankWithdrawalFreezeGate.authorized(false,true,true,true,true,true));
        assertFalse(BankWithdrawalFreezeGate.authorized(true,true,true,true,true,false));
        assertFalse(BankWithdrawalFreezeGate.authorized(true,true,true,false,true,true));
    }
}
