package gg.feral.burrow.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Native JUnit tests for BANK-R01's no-change / changed / cross-login gates. */
final class BankEventRevalidationJUnitTest {
    private static BankCaptureGate.Result read(long account, int bankQty, int invQty, int tabSum) {
        var bank = List.of(BankCaptureGate.RawItem.bank(4151, bankQty));
        var inv = List.of(BankCaptureGate.RawItem.carried(385, invQty));
        var eq = List.of(BankCaptureGate.RawItem.carried(11832, 1));
        return BankCaptureGate.evaluate(new BankCaptureGate.RawRead(true,true,true,false,
            account,account,bank,inv,eq,tabSum,false));
    }
    @Test void duplicateBankTagNavigationPreservesSameGenerationOnlyAfterFullMatchingRead() {
        var first=read(1234L,1,10,1);
        var same=read(1234L,1,10,1);
        Object session=new Object();
        assertTrue(BankEventRevalidation.unchanged(first.read(),same,session,session,1234L));
    }
    @Test void bankQuantityInventoryQuantityAndTabWitnessChangesCannotKeepOldTag() {
        var first=read(1234L,1,10,1);
        Object session=new Object();
        assertFalse(BankEventRevalidation.unchanged(first.read(),read(1234L,2,10,1),session,session,1234L));
        assertFalse(BankEventRevalidation.unchanged(first.read(),read(1234L,1,9,1),session,session,1234L));
        assertFalse(BankEventRevalidation.unchanged(first.read(),read(1234L,1,10,0),session,session,1234L));
    }
    @Test void foreignAccountOrNewLoginEpochCanNeverReuseOldStock() {
        var first=read(-7L,1,10,1);
        Object session=new Object();
        assertTrue(BankEventRevalidation.unchanged(first.read(),read(-7L,1,10,1),session,session,-7L));
        assertFalse(BankEventRevalidation.unchanged(first.read(),read(-7L,1,10,1),session,new Object(),-7L));
        assertFalse(BankEventRevalidation.unchanged(first.read(),read(-7L,1,10,1),session,session,1234L));
        assertFalse(BankEventRevalidation.unchanged(first.read(),read(-1L,1,10,1),session,session,-1L));
    }
    @Test void transientsAreQuarantinedThenSameGenerationCanResume() {
        var first=read(1234L,1,10,1);
        Object session=new Object();
        var waiting=BankCaptureGate.evaluate(new BankCaptureGate.RawRead(true,true,false,false,
            1234L,1234L,null,null,null,-1,false));
        var bank=new BankScanTracker();bank.bind(session,1234L);bank.bankOpened();
        bank.offer(first,100L);bank.offer(first,200L);
        assertEquals(BankScanTracker.Phase.ACCEPTED,bank.phase());
        bank.beginVerification();
        assertEquals(BankScanTracker.Phase.VERIFYING_CHANGE,bank.phase());
        for (int i=0;i<20;i++) {
            assertEquals(BankEventRevalidation.Verdict.INDETERMINATE,
                BankEventRevalidation.classify(first.read(),waiting,session,session,1234L));
            bank.verificationIndeterminate(waiting.outcome(),300L+i);
            assertEquals("VERIFYING BANK CHANGE",bank.status().headline());
            assertNotEquals(BankScanTracker.Phase.ACCEPTED,bank.phase());
        }
        assertEquals(BankEventRevalidation.Verdict.MATCH,
            BankEventRevalidation.classify(first.read(),read(1234L,1,10,1),session,session,1234L));
        bank.verificationMatched();
        assertEquals(BankScanTracker.Phase.ACCEPTED,bank.phase());
    }
    @Test void actualMutationsAndForeignScopesRetire() {
        Object session=new Object();
        var first=read(1234L,1,10,1);
        assertEquals(BankEventRevalidation.Verdict.CHANGED_OR_FOREIGN,
            BankEventRevalidation.classify(first.read(),read(1234L,2,10,1),session,session,1234L));
        assertEquals(BankEventRevalidation.Verdict.CHANGED_OR_FOREIGN,
            BankEventRevalidation.classify(first.read(),read(1234L,1,9,1),session,session,1234L));
        assertEquals(BankEventRevalidation.Verdict.CHANGED_OR_FOREIGN,
            BankEventRevalidation.classify(first.read(),read(1234L,1,10,1),session,new Object(),1234L));
        assertEquals(BankEventRevalidation.Verdict.INDETERMINATE,
            BankEventRevalidation.classify(first.read(),null,session,session,-1L));
        var bank=new BankScanTracker();bank.bind(session,1234L);bank.bankOpened();
        bank.offer(first,100L);bank.offer(first,200L);bank.beginVerification();bank.bankClosed();
        assertNotEquals(BankScanTracker.Phase.ACCEPTED,bank.phase(),"closing while pending cannot approve old stock");
    }
    @Test void missingBankPopulationOrUnresolvedWidgetNeverPreservesOldStock() {
        var first=read(1234L,1,10,1);
        Object session=new Object();
        var waiting=BankCaptureGate.evaluate(new BankCaptureGate.RawRead(true,true,false,false,
            1234L,1234L,null,null,null,-1,false));
        assertFalse(BankEventRevalidation.unchanged(first.read(),waiting,session,session,1234L));
        assertFalse(BankEventRevalidation.unchanged(null,first,session,session,1234L));
        assertFalse(BankEventRevalidation.unchanged(first.read(),first,null,session,1234L));
    }
}
