package gg.feral.burrow.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** BANK-R03: event-to-tick lifecycle probes, not self-referential classifier checks. */
class BankRevalidationWitnessJUnitTest {
    static BankCaptureGate.Result sample(long account, int qty, int bankCount) {
        var bank = qty == 0 ? List.<BankCaptureGate.RawItem>of()
                : List.of(BankCaptureGate.RawItem.bank(4151,qty));
        return BankCaptureGate.evaluate(new BankCaptureGate.RawRead(true,true,true,false,
                account, account, bank,
                List.of(BankCaptureGate.RawItem.carried(385,2)),
                List.of(BankCaptureGate.RawItem.carried(11832,1)),bankCount,false));
    }
    static BankRevalidationWitness.Decision observe(BankRevalidationWitness witness,
            BankCaptureGate.Read accepted,BankCaptureGate.Result fresh,Object oldScope,Object currentScope,long account) {
        var verdict=BankEventRevalidation.classify(accepted,fresh,oldScope,currentScope,account);
        return witness.observe(verdict,fresh==null || !fresh.ready()?null:fresh.read());
    }
    @Test void twoConsecutiveMatchingTicksNeededToRestoreAnUnchangedTag() {
        var accepted=sample(124,1,1).read();var w=new BankRevalidationWitness();Object login=new Object();
        assertEquals(BankRevalidationWitness.Decision.WAIT,observe(w,accepted,sample(124,1,1),login,login,124));
        assertEquals(BankRevalidationWitness.Decision.RECOVER,observe(w,accepted,sample(124,1,1),login,login,124));
    }
    @Test void hiddenWidgetResetsStreakWithoutRetiringAValidGeneration() {
        var accepted=sample(124,1,1).read();var w=new BankRevalidationWitness();Object login=new Object();
        var hidden=BankCaptureGate.evaluate(new BankCaptureGate.RawRead(true,true,false,false,
                124,124,null,null,null,-1,false));
        for(int n=0;n<10;n++) assertEquals(BankRevalidationWitness.Decision.WAIT,
                observe(w,accepted,hidden,login,login,124));
        assertEquals(BankRevalidationWitness.Decision.WAIT,observe(w,accepted,sample(124,1,1),login,login,124));
        assertEquals(BankRevalidationWitness.Decision.WAIT,observe(w,accepted,hidden,login,login,124));
        assertEquals(BankRevalidationWitness.Decision.WAIT,observe(w,accepted,sample(124,1,1),login,login,124));
        assertEquals(BankRevalidationWitness.Decision.RECOVER,observe(w,accepted,sample(124,1,1),login,login,124));
    }
    @Test void repeatedContainerEventMustRestartConfirmation() {
        var accepted=sample(124,1,1).read();var w=new BankRevalidationWitness();Object login=new Object();
        assertEquals(BankRevalidationWitness.Decision.WAIT,observe(w,accepted,sample(124,1,1),login,login,124));
        w.reset(); // triggered by another ItemContainerChanged event, even while pending
        assertEquals(BankRevalidationWitness.Decision.WAIT,observe(w,accepted,sample(124,1,1),login,login,124));
        assertEquals(BankRevalidationWitness.Decision.RECOVER,observe(w,accepted,sample(124,1,1),login,login,124));
    }
    @Test void changedStockOrChangedAccountMustRetireImmediately() {
        var accepted=sample(124,1,1).read();var w=new BankRevalidationWitness();Object login=new Object();
        assertEquals(BankRevalidationWitness.Decision.RETIRE,
                observe(w,accepted,sample(124,2,1),login,login,124));
        assertEquals(BankRevalidationWitness.Decision.RETIRE,
                observe(w,accepted,sample(124,1,1),login,new Object(),124));
        assertEquals(BankRevalidationWitness.Decision.RETIRE,
                observe(w,accepted,sample(222,1,1),login,login,222));
    }
    @Test void emptyBankRequiresThreeReadyFramesAndPopulationWitness() {
        var accepted=sample(124,0,0).read();var w=new BankRevalidationWitness();Object login=new Object();
        assertEquals(BankRevalidationWitness.Decision.WAIT,observe(w,accepted,sample(124,0,0),login,login,124));
        assertEquals(BankRevalidationWitness.Decision.WAIT,observe(w,accepted,sample(124,0,0),login,login,124));
        assertEquals(BankRevalidationWitness.Decision.RECOVER,observe(w,accepted,sample(124,0,0),login,login,124));
    }
}
