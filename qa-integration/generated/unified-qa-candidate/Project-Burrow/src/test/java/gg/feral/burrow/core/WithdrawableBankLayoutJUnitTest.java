package gg.feral.burrow.core;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WithdrawableBankLayoutJUnitTest {
    @Test void onlyExactPositiveVerifiedBankIdsAppearAsWithdrawable() {
        int[] plan=new int[56];
        plan[1]=10828;plan[2]=0;plan[3]=-1;plan[4]=4151;plan[5]=25264;plan[6]=999;
        int[] result=WithdrawableBankLayout.fromBestPlan(plan,Map.of(10828,1,4151,3,999,0));
        assertEquals(10828,result[1]);assertEquals(4151,result[4]);
        assertEquals(-1,result[2]);assertEquals(-1,result[3]);
        assertEquals(-1,result[5],"equipped or inventory-only equipment is not in the bank");
        assertEquals(-1,result[6],"zero stock cannot be withdrawn");
        assertEquals(2,WithdrawableBankLayout.withdrawableSlots(result));
        assertEquals(0,plan[2],"input cannot be mutated");
    }
    @Test void rejectsUnknownOrMalformedBankEvidence() {
        assertThrows(IllegalArgumentException.class,()->WithdrawableBankLayout.fromBestPlan(null,Map.of()));
        assertThrows(IllegalArgumentException.class,()->WithdrawableBankLayout.fromBestPlan(new int[28],Map.of()));
        assertThrows(IllegalArgumentException.class,()->WithdrawableBankLayout.fromBestPlan(new int[56],null));
        assertThrows(IllegalArgumentException.class,()->WithdrawableBankLayout.withdrawableSlots(new int[5]));
    }
    @Test void repeatedSuppliesRemainAtTheirSeparatePositionsAndNeverInventStock() {
        int[] plan=new int[56];plan[8]=385;plan[9]=385;plan[10]=385;
        int[] out=WithdrawableBankLayout.fromBestPlan(plan,Map.of(385,2));
        assertEquals(385,out[8]);assertEquals(385,out[9]);assertEquals(385,out[10]);
        assertEquals(3,WithdrawableBankLayout.withdrawableSlots(out));
        // Each position is a presentation slot, NOT a claimed independent bank quantity.
    }
    @Test void temporaryTitlesAreDistinctReadableAndBounded() {
        var names=new HashSet<String>();
        for(int i=0;i<10;i++){
            String name=BankTabTitle.temporary("Commander Zilyana",names::contains).orElseThrow();
            assertTrue(name.startsWith("trashpanda "),name);
            assertTrue(name.length()<=30,name);
            assertTrue(names.add(name),name);
        }
        assertTrue(BankTabTitle.temporary("Zilyana",name->true).isEmpty());
    }
}
