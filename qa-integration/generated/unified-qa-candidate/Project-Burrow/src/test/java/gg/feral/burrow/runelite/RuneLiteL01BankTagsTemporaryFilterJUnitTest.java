package gg.feral.burrow.runelite;

import net.runelite.client.plugins.banktags.BankTag;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.tabs.Layout;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The provider must register an ephemeral BankTag BEFORE the native tab filter redraw. */
class RuneLiteL01BankTagsTemporaryFilterJUnitTest {
    static class Store implements RuneLiteL01BankTags.Store {
        final Object profile=new Object();
        final Map<String,BankTag> ephemeral=new HashMap<>();
        final Map<String,Layout> persisted=new HashMap<>();
        String tab="all";int registered,unregistered;
        public Object profile(){return profile;}
        public String tab(){return tab;}
        public void tab(String value){tab=value;}
        public Layout load(String name){return persisted.get(name);}
        public void remove(String name){persisted.remove(name);}
        public boolean hasTaggedItems(String name){return false;}
        public void registerTemporaryTag(String name,BankTag filter){
            if(ephemeral.putIfAbsent(name,filter)!=null)throw new IllegalStateException("foreign collision");
            registered++;
        }
        public void unregisterTemporaryTag(String name){ephemeral.remove(name);unregistered++;}
    }
    static class NativeBankTags extends BankTagsPlugin {
        String tag; Layout layout; int opens,closes; boolean fail;
        Store store;
        public String getActiveTag(){return tag;}
        public Layout getActiveLayout(){return layout;}
        public void openTag(String name,Layout next,int options) {
            assertNotNull(store.ephemeral.get(name),"Native filter MUST exist before drawing layout");
            assertTrue((options & OPTION_HIDE_TAG_NAME)!=0);
            if(fail)throw new IllegalStateException("simulated native redraw failure");
            tag=name;layout=next;store.tab=name;opens++;
        }
        public void closeBankTag(){tag=null;layout=null;store.tab="";closes++;}
    }
    static Layout stockLayout(){
        int[] ids=new int[56];Arrays.fill(ids,-1);
        ids[1]=10828;ids[16]=4151;ids[34]=4151;
        return new Layout("trashpanda zilyana a1b2c3d4",ids);
    }
    @Test void exactFilterEnablesActualWithdrawableBankItemsAndIsDeletedOnClose(){
        Store store=new Store();NativeBankTags plugin=new NativeBankTags();plugin.store=store;
        var p=new RuneLiteL01BankTags(plugin,store,()->true);
        Layout l=stockLayout();p.open(l.getTag(),l);
        assertSame(l,p.activeLayout());
        BankTag filter=store.ephemeral.get(l.getTag());assertNotNull(filter);
        assertTrue(filter.contains(10828));assertTrue(filter.contains(4151));
        assertFalse(filter.contains(6737));assertFalse(filter.contains(-1));
        assertTrue(store.persisted.isEmpty(),"no saved layouts touched on open");
        p.close();assertEquals(1,plugin.closes);assertTrue(store.ephemeral.isEmpty());
        assertEquals("all",store.tab);assertEquals(1,store.registered);assertEquals(1,store.unregistered);
    }
    @Test void nativeRedrawFailureDoesNotLeakTemporaryMembership(){
        Store store=new Store();NativeBankTags plugin=new NativeBankTags();plugin.store=store;plugin.fail=true;
        var p=new RuneLiteL01BankTags(plugin,store,()->true);Layout l=stockLayout();
        assertThrows(IllegalStateException.class,()->p.open(l.getTag(),l));
        assertTrue(store.ephemeral.isEmpty());assertEquals(1,store.unregistered);
    }
    @Test void disabledBankTagsUnregistersWithoutInvokingItsCloseAction(){
        Store store=new Store();NativeBankTags plugin=new NativeBankTags();plugin.store=store;
        final boolean[] enabled={true};
        var p=new RuneLiteL01BankTags(plugin,store,()->enabled[0]);Layout l=stockLayout();p.open(l.getTag(),l);
        enabled[0]=false;
        p.discard();assertEquals(0,plugin.closes);assertTrue(store.ephemeral.isEmpty());
    }
    @Test void userNavigatingToForeignTabIsNeverClosedButOurFilterIsCleaned(){
        Store store=new Store();NativeBankTags plugin=new NativeBankTags();plugin.store=store;
        var p=new RuneLiteL01BankTags(plugin,store,()->true);Layout l=stockLayout();p.open(l.getTag(),l);
        Layout foreign=new Layout("foreign");plugin.tag="foreign";plugin.layout=foreign;store.tab="foreign";
        p.close();assertEquals(0,plugin.closes);assertSame(foreign,plugin.layout);
        assertEquals("foreign",store.tab);assertTrue(store.ephemeral.isEmpty());
    }
}
