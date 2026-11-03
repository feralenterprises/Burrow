package gg.feral.burrow.core;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Synthetic plan-lifecycle tests. Does not claim live RuneLite coverage. */
final class BestBankLayoutGateJUnitTest {
    private static final BankEvidence LIVE = new BankEvidence(Map.of(4151,1),Map.of(),Map.of(),
        BankEvidence.Population.STABLE_READ,123L,true,false);
    private static boolean allowed(long old,long now,boolean account,boolean session,boolean toggle,
                                   boolean boss,BankEvidence evidence){
        return BestBankLayoutGate.permitted(old,now,account,session,toggle,boss,evidence);
    }
    @Test void acceptedCurrentPlanMayShowWithValidSelectionAndIdentity(){
        assertTrue(allowed(8,8,true,true,true,true,LIVE));
        assertFalse(allowed(8,8,true,true,true,true,LIVE.closed()), "as-of-close is never a live O tag source");
        assertTrue(allowed(8,8,true,true,true,true,new BankEvidence(Map.of(),Map.of(),Map.of(),
            BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT,123L,true,false)),
            "a witnessed genuinely empty bank is valid observation (not a ready trip)");
    }
    @Test void oldPlanMustNotSurviveBankRescanOrPlannerSettingsChange(){
        assertFalse(allowed(8,9,true,true,true,true,LIVE));
        assertFalse(allowed(8,8,true,true,true,true,LIVE.markStale()));
        assertFalse(allowed(8,8,true,true,true,true,null));
        assertFalse(allowed(8,8,false,true,true,true,LIVE));
        assertFalse(allowed(8,8,true,false,true,true,LIVE));
    }
    @Test void currentOpeningAndPendingMutationMustBothBeVerified() {
        assertTrue(BestBankLayoutGate.permitted(8,8,true,true,true,true,LIVE,true,true,true,false));
        assertFalse(BestBankLayoutGate.permitted(8,8,true,true,true,true,LIVE,false,true,true,false));
        assertFalse(BestBankLayoutGate.permitted(8,8,true,true,true,true,LIVE,true,false,true,false));
        assertFalse(BestBankLayoutGate.permitted(8,8,true,true,true,true,LIVE,true,true,false,false));
        assertFalse(BestBankLayoutGate.permitted(8,8,true,true,true,true,LIVE,true,true,true,true));
        assertFalse(BestBankLayoutGate.permitted(8,8,true,true,true,true,LIVE.closed(),true,true,true,false));
    }
    @Test void neverPublishInWildernessActivityOrWikiMode(){
        assertFalse(allowed(8,8,true,true,true,false,LIVE));
        assertFalse(allowed(8,8,true,true,false,true,LIVE));
        assertFalse(allowed(8,8,true,true,true,true, BankEvidence.fixture(Map.of())));
    }
}
