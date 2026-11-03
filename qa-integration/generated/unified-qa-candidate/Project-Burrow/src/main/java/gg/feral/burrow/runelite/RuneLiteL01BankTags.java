package gg.feral.burrow.runelite;

import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTag;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.Layout;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;

/** Public upstream APIs only. Optional dependency discovered from the actual enabled plugin instance. */
final class RuneLiteL01BankTags implements L01BankLayout.Provider {
    interface Store {
        Object profile();
        String tab();
        void tab(String value);
        Layout load(String name);
        void remove(String name);
        boolean hasTaggedItems(String name);
        void registerTemporaryTag(String name, BankTag filter);
        void unregisterTemporaryTag(String name);
    }
    private final java.util.function.BooleanSupplier enabled;
    private final Store store;
    private final BankTagsPlugin plugin;
    private Layout owned;
    private String registeredTemporaryTag;
    private Object originalProfile;
    private String previousTab;
    RuneLiteL01BankTags(PluginManager manager, BankTagsPlugin plugin) {
        this(plugin,new Store() {
            private <T> T api(Class<T> type){return plugin.getInjector().getInstance(type);}
            public Object profile(){return api(net.runelite.client.config.ConfigManager.class).getProfile();}
            public String tab(){return api(net.runelite.client.plugins.banktags.BankTagsConfig.class).tab();}
            public void tab(String value){api(net.runelite.client.plugins.banktags.BankTagsConfig.class).tab(value);}
            public Layout load(String name){return api(LayoutManager.class).loadLayout(name);}
            public void remove(String name){api(LayoutManager.class).removeLayout(name);}
            public boolean hasTaggedItems(String name){return !api(TagManager.class).getItemsForTag(name).isEmpty();}
            public void registerTemporaryTag(String name, BankTag filter){api(TagManager.class).registerTag(name,filter);}
            public void unregisterTemporaryTag(String name){api(TagManager.class).unregisterTag(name);}
        },()->manager.isPluginEnabled(plugin));
    }
    RuneLiteL01BankTags(BankTagsPlugin plugin, Store store, java.util.function.BooleanSupplier enabled) {
        this.plugin=plugin;this.store=store;this.enabled=enabled;
    }
    public boolean available() { return enabled.getAsBoolean(); }
    public String activeTag() { return plugin.getActiveTag(); }
    public Layout activeLayout() { return plugin.getActiveLayout(); }
    public boolean nameInUse(String name) {
        return store.load(name)!=null || store.hasTaggedItems(name);
    }
    public void open(String name,Layout layout) {
        // openBankTag loads a persisted LayoutManager layout. The public concrete overload accepts
        // an explicit Layout and reaches the identical TabInterface + bank script lifecycle without saving it.
        // Exact nonpersistent membership is REQUIRED before rendering. Bank Tags applies
        // the tag filter to real bank items BEFORE LayoutManager maps them into slots.
        if(owned!=null) throw new IllegalStateException("Temporary bank tab already owned");
        // RuneLite BankTagsPlugin builds its search filter from TagManager, independently
        // of the Layout. Without a registered filter, the native bank draws placeholders
        // instead of real items, so Withdraw cannot work. This is a NON-PERSISTED tag:
        // no item tags, user tabs, or permanent layouts are created.
        java.util.Set<Integer> exactIds=new java.util.HashSet<>();
        for(int id:layout.getLayout())if(id>0)exactIds.add(id);
        java.util.Set<Integer> membership=java.util.Set.copyOf(exactIds);
        Object profile=store.profile();
        String prior=store.tab();
        store.registerTemporaryTag(name,membership::contains);
        registeredTemporaryTag=name;
        originalProfile=profile;
        previousTab=prior;
        owned=layout;
        try {
            plugin.openTag(name,layout,net.runelite.client.plugins.banktags.BankTagsService.OPTION_HIDE_TAG_NAME);
        } catch(RuntimeException | Error failure) {
            owned=null;originalProfile=null;previousTab=null;
            unregisterOwnedFilter();
            throw failure;
        }
    }
    public void maintain() {
        if(owned==null || store.profile()!=originalProfile)return;
        var saved=store.load(owned.getTag());
        if(saved!=null && java.util.Arrays.equals(saved.getLayout(),owned.getLayout()))
            store.remove(owned.getTag()); // Native dragging can save even with OPTION_ALLOW_MODIFICATIONS off.
        // An independently replaced saved layout is left untouched.
    }
    public void discard() { close(); }
    public void close() {
        if(!available() || owned==null || plugin.getActiveLayout()!=owned || !owned.getTag().equals(plugin.getActiveTag())) {
            // CP-6 (BQA-R12-016): the user already switched to another tag. Never close THEIR view, but still drop a
            // persisted copy of OUR temporary layout (native dragging can save it) so no stray layout is left behind.
            try { if(owned!=null) maintain(); }
            finally {
                unregisterOwnedFilter();
                owned=null;previousTab=null;originalProfile=null;
            }
            return;
        }
        try {
            maintain();
            String current=store.tab();
            String restore=owned!=null && owned.getTag().equals(current)
                ? (store.profile()==originalProfile?previousTab:"") : current;
            plugin.closeBankTag();
            // closeBankTag normally clears remembered tab; preserve the current profile's prior value.
            if(store.tab().isEmpty() && restore!=null && !restore.isEmpty())store.tab(restore);
        } finally {
            unregisterOwnedFilter();
            owned=null;previousTab=null;originalProfile=null;
        }
    }
    private void unregisterOwnedFilter() {
        String old=registeredTemporaryTag;
        registeredTemporaryTag=null;
        if(old!=null)store.unregisterTemporaryTag(old);
    }
}
