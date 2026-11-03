package gg.feral.burrow.core;

import gg.feral.burrow.core.BestBankPlanner.*;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;

/** W07 offline Bank O safety contract, not live RuneLite QA. */
public final class BankLayoutEligibilityTest {
    private BankLayoutEligibilityTest() {}
    private static void eq(int got,int want) { if(got!=want) throw new AssertionError("Expected "+want+" got "+got); }
    public static void main(String[] args) {
        Item item=new Item(9185,"Rune crossbow",1);
        for(GearStatus s:new GearStatus[]{GearStatus.LEVEL_UNMET,GearStatus.QUEST_UNMET,
            GearStatus.REQUIREMENT_UNKNOWN,GearStatus.NOT_OBSERVED,GearStatus.PARTIAL_STACK})
            eq(BankLayoutEligibility.wornId(new GearCell(item,item,s,"blocked")),0);
        for(GearStatus s:new GearStatus[]{GearStatus.OWNED,GearStatus.OWNED_VARIANT,
            GearStatus.OWNED_SUBSTITUTE,GearStatus.LOW_STOCK})
            eq(BankLayoutEligibility.wornId(new GearCell(item,item,s,"confirmed")),9185);
        eq(BankLayoutEligibility.wornId(new GearCell(item,null,GearStatus.OPTIONAL_MISSING,"")),-1);
        for(SupplyStatus s:new SupplyStatus[]{SupplyStatus.SWITCH_REQUIREMENT_UNMET,
            SupplyStatus.SWITCH_NOT_OBSERVED,SupplyStatus.NOT_OBSERVED,
            SupplyStatus.VARIANT_UNSPECIFIED})
            eq(BankLayoutEligibility.inventoryId(new SupplyCell(item,item,s,"blocked")),0);
        for(SupplyStatus s:new SupplyStatus[]{SupplyStatus.STOCKED,SupplyStatus.SWITCH_OWNED,
            SupplyStatus.SHORT,SupplyStatus.QUANTITY_UNSPECIFIED})
            eq(BankLayoutEligibility.inventoryId(new SupplyCell(item,item,s,"present")),9185);
        eq(BankLayoutEligibility.inventoryId(new SupplyCell(null,null,SupplyStatus.EMPTY,"")),-1);
        System.out.println("BankLayoutEligibilityTest PASS");
    }
}
