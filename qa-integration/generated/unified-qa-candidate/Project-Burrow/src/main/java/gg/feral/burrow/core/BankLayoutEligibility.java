package gg.feral.burrow.core;

import gg.feral.burrow.core.BestBankPlanner.GearCell;
import gg.feral.burrow.core.BestBankPlanner.GearStatus;
import gg.feral.burrow.core.BestBankPlanner.SupplyCell;
import gg.feral.burrow.core.BestBankPlanner.SupplyStatus;

/** W07. Read-only positional Bank O eligibility; never moves, withdraws or searches the bank.
 * A positive item observation does not establish wearability. Zero is the Bank Tags
 * nonempty-source placeholder, negative one is an intentional empty slot.
 */
public final class BankLayoutEligibility {
    private BankLayoutEligibility() {}

    public static int wornId(GearCell cell) {
        if(cell==null) return 0;
        GearStatus status=cell.status();
        if(status==GearStatus.EMPTY || status==GearStatus.OPTIONAL_MISSING) return -1;
        if(status==GearStatus.OWNED || status==GearStatus.OWNED_VARIANT ||
           status==GearStatus.OWNED_SUBSTITUTE || status==GearStatus.LOW_STOCK) {
            return cell.shown()!=null && cell.shown().id()>0?cell.shown().id():0;
        }
        // LEVEL_UNMET, QUEST_UNMET, REQUIREMENT_UNKNOWN, PARTIAL_STACK, NOT_OBSERVED
        return 0;
    }

    public static int inventoryId(SupplyCell cell) {
        if(cell==null || cell.status()==SupplyStatus.EMPTY) return -1;
        SupplyStatus status=cell.status();
        if(status==SupplyStatus.STOCKED || status==SupplyStatus.LOWER_DOSE ||
           status==SupplyStatus.SHORT || status==SupplyStatus.QUANTITY_UNSPECIFIED ||
           status==SupplyStatus.SWITCH_OWNED) {
            return cell.shown()!=null && cell.shown().id()>0?cell.shown().id():0;
        }
        // SWITCH_REQUIREMENT_UNMET and unresolved or missing planned items get no ready ID.
        return 0;
    }
}
