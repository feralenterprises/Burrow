package gg.feral.burrow.runelite;

import net.runelite.api.*;
import net.runelite.api.widgets.WidgetInfo;
import java.util.*;

/** Client-thread-only, event-driven immutable BANK + INVENTORY + EQUIPMENT read.
 * Never claims negative ownership from a partial or unreadable bank. No widget mutation.
 *
 * <p>Build 19 population witness: the BANK container's raw slot count (placeholders included) must
 * be at least the sum of RuneLite's nine bank-tab count varbits. A container shorter than its own
 * tab counts is still populating and is rejected; the caller additionally requires two identical
 * consecutive tick reads before accepting a snapshot. Bank search, tag tabs and widget scrolling
 * filter only the rendered widgets, not the BANK ItemContainer read here. */
final class AutomaticBankSnapshotReader {
    record Snapshot(long accountHash, Map<Integer,Integer> bank,
                    Map<Integer,Integer> inventory,Map<Integer,Integer> equipment,
                    int rawBankSlots, int tabCountSum) {
        Snapshot {bank=Map.copyOf(bank);inventory=Map.copyOf(inventory);equipment=Map.copyOf(equipment);}
        Snapshot(long accountHash, Map<Integer,Integer> bank, Map<Integer,Integer> inventory, Map<Integer,Integer> equipment) {
            this(accountHash,bank,inventory,equipment,-1,-1);
        }
        Map<Integer,Integer> combined() {
            Map<Integer,Integer> result=new HashMap<>(bank);
            for(var e:inventory.entrySet())result.merge(e.getKey(),e.getValue(),Math::addExact);
            for(var e:equipment.entrySet())result.merge(e.getKey(),e.getValue(),Math::addExact);
            return Map.copyOf(result);
        }
        boolean tabWitnessed() { return tabCountSum>=0 && rawBankSlots>=tabCountSum; }
        /** Same observable content on two ticks (used as the stability witness). */
        boolean sameContent(Snapshot other) {
            return other!=null && other.accountHash==accountHash && other.rawBankSlots==rawBankSlots &&
                other.bank.equals(bank) && other.inventory.equals(inventory) && other.equipment.equals(equipment);
        }
    }
    @SuppressWarnings("deprecation")
    private static final int[] TAB_COUNT_VARBITS = {
        Varbits.BANK_TAB_ONE_COUNT, Varbits.BANK_TAB_TWO_COUNT, Varbits.BANK_TAB_THREE_COUNT,
        Varbits.BANK_TAB_FOUR_COUNT, Varbits.BANK_TAB_FIVE_COUNT, Varbits.BANK_TAB_SIX_COUNT,
        Varbits.BANK_TAB_SEVEN_COUNT, Varbits.BANK_TAB_EIGHT_COUNT, Varbits.BANK_TAB_NINE_COUNT };
    private AutomaticBankSnapshotReader() {}
    /** Bank OPEN is distinct from bank ITEMS READY. A Bank Tags tab can temporarily
     * hide/rebuild the item widget while the authenticated bank root remains open.
     * evaluate() still rejects hidden/unavailable items (WIDGET_NOT_READY) and PINs;
     * this method only chooses whether we should keep trying while the bank is open. */
    static boolean visible(Client client) {
        if(client==null || client.getGameState()!=GameState.LOGGED_IN)return false;
        try {
            var root=client.getWidget(WidgetInfo.BANK_CONTAINER);
            var pin=client.getWidget(WidgetInfo.BANK_PIN_CONTAINER);
            return root!=null && !root.isHidden() && (pin==null || pin.isHidden());
        } catch(RuntimeException | AbstractMethodError | AssertionError e) {return false;}
    }
    /** Legacy entry point retained for existing callers/tests: READY reads only. */
    static Optional<Snapshot> capture(Client client) {
        var r=evaluate(client);
        if(!r.ready())return Optional.empty();
        var x=r.read();
        return Optional.of(new Snapshot(x.accountHash(),x.bank(),x.inventory(),x.equipment(),x.rawBankSlots(),x.tabCountSum()));
    }
    /** BQA-901: copy raw client facts, then let the pure core gate decide with a typed outcome.
     *  Only {@code -1} is the unavailable account sentinel (BF-03). Client thread only. */
    static gg.feral.burrow.core.BankCaptureGate.Result evaluate(Client client) {
        return gg.feral.burrow.core.BankCaptureGate.evaluate(raw(client));
    }
    static gg.feral.burrow.core.BankCaptureGate.RawRead raw(Client client) {
        if(client==null)return null;
        try {
            boolean loggedIn=client.getGameState()==GameState.LOGGED_IN;
            var root=client.getWidget(WidgetInfo.BANK_CONTAINER);
            var items=client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
            var pin=client.getWidget(WidgetInfo.BANK_PIN_CONTAINER);
            boolean rootVisible=root!=null && !root.isHidden();
            boolean itemsVisible=items!=null && !items.isHidden();
            boolean pinVisible=pin!=null && !pin.isHidden();
            long before=client.getAccountHash();
            var bank=rawItems(client,InventoryID.BANK,true);
            var inv=rawItems(client,InventoryID.INVENTORY,false);
            var equip=rawItems(client,InventoryID.EQUIPMENT,false);
            // R05: a container that does not exist yet (nothing worn) differs from one whose items are unreadable.
            boolean invExists=client.getItemContainer(InventoryID.INVENTORY)!=null;
            boolean equipExists=client.getItemContainer(InventoryID.EQUIPMENT)!=null;
            int tabs=tabCountSum(client);
            boolean stillLoggedIn=client.getGameState()==GameState.LOGGED_IN;
            long after=client.getAccountHash();
            return new gg.feral.burrow.core.BankCaptureGate.RawRead(loggedIn && stillLoggedIn,rootVisible,itemsVisible,
                pinVisible,before,after,bank,inv,equip,tabs,false,invExists,equipExists);
        } catch(RuntimeException | AbstractMethodError | AssertionError e) {
            return new gg.feral.burrow.core.BankCaptureGate.RawRead(false,false,false,false,-1L,-1L,null,null,null,-1,true);
        }
    }
    private static List<gg.feral.burrow.core.BankCaptureGate.RawItem> rawItems(Client client,InventoryID id,boolean bank) {
        ItemContainer c=client.getItemContainer(id);
        if(c==null || c.getItems()==null)return null;
        var out=new ArrayList<gg.feral.burrow.core.BankCaptureGate.RawItem>();
        for(Item item:c.getItems()) {
            // Keep one entry per raw slot so the tab-count consistency check sees the true container length.
            if(item==null){ out.add(new gg.feral.burrow.core.BankCaptureGate.RawItem(-1,0,true,false,false)); continue; }
            int itemId=item.getId(), qty=item.getQuantity();
            if(itemId<=0 || qty<=0) { out.add(new gg.feral.burrow.core.BankCaptureGate.RawItem(itemId,qty,true,false,false)); continue; }
            ItemComposition def=client.getItemDefinition(itemId);
            boolean ok=def!=null && def.getId()==itemId;
            String name=null;
            try { name=ok?def.getName():null; } catch(RuntimeException | AbstractMethodError unnamed) { name=null; }
            if(!bank) { out.add(new gg.feral.burrow.core.BankCaptureGate.RawItem(itemId,qty,true,false,false,name)); continue; }
            out.add(new gg.feral.burrow.core.BankCaptureGate.RawItem(itemId,qty,ok,
                ok && def.getPlaceholderTemplateId()!=-1,ok && def.getNote()!=-1,name));
        }
        return out;
    }
    private static int tabCountSum(Client client) {
        try {
            int sum=0;
            for(int varbit:TAB_COUNT_VARBITS) {
                int v=client.getVarbitValue(varbit);
                if(v<0)return -1;
                sum+=v;
            }
            return sum;
        } catch(RuntimeException | AbstractMethodError e) { return -1; }
    }

}
