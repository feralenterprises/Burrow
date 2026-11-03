package gg.feral.burrow.runelite;

import java.util.*;
import net.runelite.api.*;
import net.runelite.api.widgets.WidgetInfo;

/** Explicit, client-thread-only bank read adapted from the working Iron Ledger
 * manual-scan pattern. This captures POSITIVE observations, not a completeness
 * certificate. Never reads the bank on ticks or changes game widgets. */
final class IronLedgerManualBankReader {
    record Result(boolean observed, Map<Integer,Integer> quantities, String message) {
        Result { quantities=Map.copyOf(quantities); }
        static Result unavailable(String message){return new Result(false,Map.of(),message);}
    }
    private IronLedgerManualBankReader() {}
    static Result scan(Client client) {
        if(client==null) return Result.unavailable("Bank client unavailable");
        if(client.getGameState()!=GameState.LOGGED_IN || client.getAccountHash()==-1L)
            return Result.unavailable("Log in before scanning");
        try {
            var root=client.getWidget(WidgetInfo.BANK_CONTAINER);
            var visible=client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
            var pin=client.getWidget(WidgetInfo.BANK_PIN_CONTAINER);
            if(root==null || root.isHidden() || visible==null || visible.isHidden() || (pin!=null && !pin.isHidden()))
                return Result.unavailable("Open your bank, then select SCAN BANK");
            long accountBefore=client.getAccountHash();
            ItemContainer bank=client.getItemContainer(InventoryID.BANK);
            if(bank==null)return Result.unavailable("Bank container unavailable; try again when loaded");
            Item[] items=bank.getItems();
            if(items==null || items.length==0 || items.length>4096)
                return Result.unavailable("Bank entries are not ready; reopen and retry");
            Map<Integer,Integer> counts=new HashMap<>();
            for(Item item:items) {
                if(item==null || item.getId()<=0 || item.getQuantity()<=0 || item.getId()==20594)continue;
                ItemComposition def=client.getItemDefinition(item.getId());
                if(def==null || def.getId()!=item.getId())return Result.unavailable("Item definitions not ready; retry");
                if(def.getPlaceholderTemplateId()!=-1 || def.getNote()!=-1)continue;
                counts.merge(item.getId(),item.getQuantity(),Math::addExact);
            }
            if(client.getGameState()!=GameState.LOGGED_IN || client.getAccountHash()!=accountBefore)
                return Result.unavailable("Account changed during scan; discarded");
            if(counts.isEmpty())return Result.unavailable("No usable bank entries observed; retry when loaded");
            return new Result(true,counts,"Observed "+counts.size()+" exact item types in the open bank (as-of scan)");
        } catch (RuntimeException | AbstractMethodError unexpected) {
            return Result.unavailable("Bank observation unavailable; no ownership was assumed");
        }
    }
}
