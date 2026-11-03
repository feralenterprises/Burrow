package gg.feral.burrow.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BankTagRetentionJUnitTest {
    private BankTagRetention.Decision decision(boolean selected,boolean epoch,boolean setup,
            boolean best, boolean requested,boolean verifying,boolean override,boolean sameGeneration) {
        return BankTagRetention.decide(selected,epoch,setup,best,requested,verifying,override,sameGeneration);
    }
    @Test void hiddenBankWidgetShouldNotClosePreviousTagDuringQuarantine() {
        for(int i=0;i<20;i++)assertEquals(BankTagRetention.Decision.HOLD_UNVERIFIED,
                decision(true,true,true,true,true,true,false,true));
        assertEquals(BankTagRetention.Decision.MAINTAIN,
                decision(true,true,true,true,true,false,true,true));
    }
    @Test void actualMutationLoginChangeSelectionOrWikiSwitchMustCloseTag() {
        assertEquals(BankTagRetention.Decision.CLOSE,
                decision(true,true,true,true,true,false,false,false));
        assertEquals(BankTagRetention.Decision.CLOSE,
                decision(true,true,true,true,true,true,false,false),
                "a changed bank generation cannot be held by an unrelated verification event");
        assertEquals(BankTagRetention.Decision.CLOSE,
                decision(true,false,true,true,true,true,false,true));
        assertEquals(BankTagRetention.Decision.CLOSE,
                decision(true,true,false,true,true,true,false,true));
        assertEquals(BankTagRetention.Decision.CLOSE,
                decision(true,true,true,true,false,true,false,true));
        assertEquals(BankTagRetention.Decision.CLOSE,
                decision(false,true,true,true,true,true,false,true));
    }
    @Test void wikiLayoutsRemainIndependentOfBestBankTransient() {
        assertEquals(BankTagRetention.Decision.MAINTAIN,
                decision(true,true,true,false,false,true,false,false));
    }
}
