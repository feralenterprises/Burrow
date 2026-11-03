package gg.feral.burrow.runelite;

import java.util.*;
import java.util.function.BooleanSupplier;
import net.runelite.client.plugins.banktags.tabs.Layout;

/** Owns one ephemeral Bank Tags view, never a widget, persistent item tag, or user's saved layout. */
final class L01BankLayout {
    interface Provider {
        boolean available();
        String activeTag();
        Layout activeLayout();
        boolean nameInUse(String name);
        void open(String name, Layout layout);
        void close();
        default void maintain() {}
        /** Retire our ephemeral registration even if Bank Tags is disabled or user replaced the tab. */
        default void discard() {}
    }
    private final Provider provider;
    private final String name;
    private Layout layout;
    private L01LayoutSpecification specification;
    L01BankLayout(Provider provider) { this(provider,"burrow-outfitter-"+UUID.randomUUID()); }
    L01BankLayout(Provider provider,String name) { this.provider=provider;this.name=name; }
    boolean selected() { return provider.available() && layout!=null && name.equals(provider.activeTag()) && provider.activeLayout()==layout; }
    L01LayoutSpecification specification() { return selected()?specification:null; }
    void open(L01LayoutSpecification spec, BooleanSupplier current) {
        if(!current.getAsBoolean()) throw new IllegalStateException("Stale plan discarded");
        if(!provider.available()) throw new IllegalStateException("Enable Bank Tags before using the experimental bank layout");
        if(!selected() && provider.activeTag()!=null) throw new IllegalStateException("Close the active bank tag / Inventory Setup first");
        if(layout==null && provider.nameInUse(name)) throw new IllegalStateException("Outfitter layout name conflict; no content changed");
        Layout next=new Layout(name,spec.itemIds());
        if(!current.getAsBoolean()) throw new IllegalStateException("Stale plan discarded");
        // Public explicit-layout API. registerAutoLayout alone does NOT attach a layout to a tag.
        layout=next;specification=spec;
        try {
            provider.open(name,next);
            if(!name.equals(provider.activeTag()) || provider.activeLayout()!=next)
                throw new IllegalStateException("Bank Tags did not activate the requested layout");
        } catch(RuntimeException failed) {
            close();throw failed;
        }
    }
    void maintain() { if(selected())provider.maintain(); }
    void close() {
        boolean ours=selected();
        layout=null;specification=null;
        if(ours && provider.available()) provider.close();
        else provider.discard(); // Never close a replacement view; still unregister temporary membership.
    }
}
