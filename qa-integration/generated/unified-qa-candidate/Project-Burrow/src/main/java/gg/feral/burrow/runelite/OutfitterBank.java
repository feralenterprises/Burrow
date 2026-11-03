package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.ui.SharedOutfitterSetup;
import java.util.function.Supplier;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;

/** Client-thread-only ownership. O toggles one temporary Bank Tags layout. */
final class OutfitterBank {
    private final Client client;
    private final PluginManager plugins;
    private final Supplier<BurrowModel.Selection> selection;
    private final BankWidgetAdapter button;
    private final Supplier<L01BankLayout.Provider> providers;
    private BankWidgetAdapter.Entry bound;
    private L01BankLayout layout;
    private SharedOutfitterSetup shown;
    private Supplier<SharedOutfitterSetup> setupSelection;
    private java.util.function.LongSupplier selectionEpoch=()->0L;
    private long shownSelectionEpoch;
    void setSelectionEpoch(java.util.function.LongSupplier source) {
        selectionEpoch=source==null?()->0L:source;
    }
    private boolean active=true;
    /** CP-8: Best in Bank positions for the CURRENT selection, or null (Wiki plan). Client thread reads it. */
    private java.util.function.Supplier<int[]> bestBankOverride=()->null;
    private java.util.function.BooleanSupplier bestBankRequested=()->false;
    /** Uncertain Bank Tags event is not a confirmed owned-item change. */
    private java.util.function.BooleanSupplier bestBankVerifying=()->false;
    void setBestBankVerifying(java.util.function.BooleanSupplier source) {
        bestBankVerifying=source==null?()->false:source;
    }
    private boolean shownAsBestBank;
    /** Covers synchronous Bank Tags redraw events during openTag before activeTag is set. */
    private boolean openingBestBankTab;
    private java.util.function.LongSupplier bestBankGeneration=()->-1L;
    private long shownBestBankGeneration=-1L;
    void setBestBankGeneration(java.util.function.LongSupplier source) {
        bestBankGeneration=source==null?()->-1L:source;
    }
    private Supplier<gg.feral.burrow.core.BankEvidence> verifiedBankEvidence=()->null;
    void setVerifiedBankEvidence(Supplier<gg.feral.burrow.core.BankEvidence> source) {
        verifiedBankEvidence=source==null?()->null:source;
    }
    /** Owner R07: the active category supplies the same plan object to bank O.
     * Legacy boss supplier remains as a test seam and fallback only. */
    void setSetupSelection(Supplier<SharedOutfitterSetup> source) {
        setupSelection=source;
    }
    private SharedOutfitterSetup selectedSetup() {
        var current=setupSelection==null?null:setupSelection.get();
        return current!=null?current:SharedOutfitterSetup.boss(selection.get());
    }
    void setBestBankOverride(java.util.function.Supplier<int[]> supplier){bestBankOverride=supplier==null?()->null:supplier;}
    void setBestBankRequested(java.util.function.BooleanSupplier source){bestBankRequested=source==null?()->false:source;}

    OutfitterBank(Client client, PluginManager plugins, Supplier<BurrowModel.Selection> selection) {
        this.client=client;this.plugins=plugins;this.selection=selection;
        button=new BankWidgetAdapter(client);
        providers=this::findProvider;
    }
    // Test seam supplies a provider, while using real RuneLite widget/API types.
    OutfitterBank(Client client, Supplier<BurrowModel.Selection> selection,
                  Supplier<L01BankLayout.Provider> providers) {
        this.client=client;this.plugins=null;this.selection=selection;this.providers=providers;
        button=new BankWidgetAdapter(client);
    }
    private boolean bankOpen() {
        var root=client.getWidget(WidgetInfo.BANK_CONTAINER);
        var pin=client.getWidget(WidgetInfo.BANK_PIN_CONTAINER);
        // Bank Tags may hide/rebuild the item widget without closing the actual bank.
        // The source-qualified reader, not this open-state test, controls ownership claims.
        return active && client.getGameState()==GameState.LOGGED_IN && root!=null && !root.isHidden()
            && (pin==null || pin.isHidden());
    }
    void refresh() {
        if(!bankOpen()) { clear();return; }
        if(layout!=null) {
            var retention=gg.feral.burrow.core.BankTagRetention.decide(
                layout.selected(),shownSelectionEpoch==selectionEpoch.getAsLong(),
                shown!=null && shown.equals(selectedSetup()),shownAsBestBank,
                !shownAsBestBank || bestBankRequested.getAsBoolean(),
                shownAsBestBank && bestBankVerifying.getAsBoolean(),
                !shownAsBestBank || bestBankOverride.get()!=null,
                shownBestBankGeneration==bestBankGeneration.getAsLong());
            switch(retention) {
                case HOLD_UNVERIFIED -> {
                    // Preserve the existing temporary tag without claiming ownership.
                    // Best in Bank / O cannot authorize a new owned layout in this phase.
                    // Never mutate the Bank Tags view while it is transitioning.
                }
                case CLOSE -> {
                    boolean missingOwnedPlan=shownAsBestBank && bestBankRequested.getAsBoolean();
                    closeLayout();
                    if(missingOwnedPlan)status("Best in Bank evidence changed; the old bank tag was closed for safety.");
                }
                case MAINTAIN -> layout.maintain();
            }
        }
        var next=button.refresh();
        if(next!=null && next!=bound) {
            bound=next;
            button.bind(next,()->{if(active && bankOpen() && button.current(next))toggle();});
        } else if(next==null) bound=null;
    }
    /** True only for our explicitly selected, same-setup Best in Bank withdrawal tab.
     * This is a tab lease, not proof that the bank still contains every displayed item.
     * The host also validates authenticated account, session and bank-open epoch.
     */
    boolean selectedBestBankWithdrawalTab() {
        return active && bankOpen() && layout!=null && (layout.selected() || openingBestBankTab) && shownAsBestBank
            && shown!=null && shown.equals(selectedSetup())
            && shownSelectionEpoch==selectionEpoch.getAsLong()
            && bestBankRequested.getAsBoolean()
            && shownBestBankGeneration==bestBankGeneration.getAsLong();
    }

    private void toggle() {
        if(layout!=null && layout.selected()) { closeLayout();return; }
        closeLayout();
        var selected=selectedSetup();
        if(selected==null || selected.plan()==null) { status("No vetted loadout for the selected setup.");return; }
        if(!selected.bankTagPermitted()) {
            status(selected.category()==SharedOutfitterSetup.Category.ACTIVITY && selected.id().contains("wilderness")
                ?"Wilderness activity is reference-only; bank optimization and O layouts are locked."
                :"This setup has no verified item-ID bank reference yet. No previous boss layout was reused.");
            return;
        }
        // Never silently show a Wiki plan as if it were a selected Best in Bank layout.
        // The user can explicitly switch the sidebar to Wiki Recommended instead.
        boolean requestingBest=bestBankRequested.getAsBoolean();
        if(requestingBest && !selected.bestBankPermitted()) {
            status("Best in Bank is unavailable for this selected setup.");return;
        }
        int[] best=requestingBest?bestBankOverride.get():null;
        if(requestingBest && best==null) {
            status("Best in Bank is waiting for the automatic same-login bank read. Keep the bank open; no manual restart needed. See Outfitter scan status.");
            return;
        }
        var provider=providers.get();
        if(provider==null)return;
        // BQA UI-01: readable tab name instead of a raw "burrow-outfitter-<UUID>".
        var name=gg.feral.burrow.core.BankTabTitle.temporary(selected.title(),provider::nameInUse);
        if(name.isEmpty()) { status("Temporary bank tag names for this setup are all in use; no saved tags were changed.");return; }
        var owned=new L01BankLayout(provider,name.get());
        layout=owned;shown=selected;shownSelectionEpoch=selectionEpoch.getAsLong();
        shownBestBankGeneration=best!=null?bestBankGeneration.getAsLong():-1L;
        try {
            java.util.List<String> blocked=new java.util.ArrayList<>();
            BankStockObservation.Snapshot bank = best!=null?trustedBankObservation():bankObservation();
            if(best!=null && !bank.available()) {
                closeLayout();status("Verified bank evidence changed before the O layout could open; waiting for automatic scan.");
                return;
            }
            // A selected owned item in inventory/equipment is NOT a withdrawable bank item.
            // Do not render the Wiki fallback (override=0) as a ghost in Best in Bank.
            // Only positive exact IDs in the verified bank itself go into the native view.
            L01LayoutSpecification spec;
            if(best!=null) {
                var evidence=verifiedBankEvidence.get();
                if(evidence==null || evidence.stale() || !evidence.planningInput() || !evidence.bankStillOpen()) {
                    closeLayout();status("Verified bank stock changed before the temporary withdrawal tab opened.");return;
                }
                int[] bankOnly=gg.feral.burrow.core.WithdrawableBankLayout.fromBestPlan(best,evidence.bank());
                if(gg.feral.burrow.core.WithdrawableBankLayout.withdrawableSlots(bankOnly)==0) {
                    closeLayout();status("No selected Best in Bank items remain to withdraw from the bank. Already-carried items are not shown.");return;
                }
                spec=L01LayoutSpecification.fromExactBankPositions(bankOnly);
            } else {
                spec=selected.sourcedBankLayout()!=null
                    ?L01LayoutSpecification.fromExactBankPositions(selected.sourcedBankLayout())
                    :L01LayoutSpecification.from(selected.plan(),bank.available()?bank.usableIds():null,blocked);
            }
            shownAsBestBank=best!=null;
            openingBestBankTab=shownAsBestBank;
            try {
                owned.open(spec, ()->bankOpen() && layout==owned && shownSelectionEpoch==selectionEpoch.getAsLong() && selected.equals(selectedSetup()) &&
                    (!shownAsBestBank || (bestBankOverride.get()!=null &&
                        shownBestBankGeneration==bestBankGeneration.getAsLong())));
            } finally { openingBestBankTab=false; }
            status(selected.title()+(best!=null?" Best in Bank withdrawal tab (bank-stock items only)":
                selected.category()==SharedOutfitterSetup.Category.ACTIVITY?" suggested Wiki reference (NOT bank-verified)":" Wiki plan")
                +": equipment left, supplies right. Unspecified variants are omitted. O or All items returns to bank. Highlighting OFF.");
            if (!bank.available()) status("Owned variants unavailable: " + bank.reason() + ".");
            for (String note : blocked) status("Variant: " + note + ".");
        } catch(RuntimeException e) { closeLayout();status("Layout unavailable: "+e.getMessage()); }
    }
    /** Best-in-Bank bank tags must use the SAME authenticated evidence as the visible plan. */
    private BankStockObservation.Snapshot trustedBankObservation() {
        var ev=verifiedBankEvidence.get();
        return ev==null || ev.stale() || !ev.planningInput()
            ?BankStockObservation.Snapshot.unknown("verified bank evidence unavailable")
            :new BankStockObservation.Snapshot(ev.bank().keySet(),true,"verified same-login bank read");
    }
    /** Never confuse an unavailable/partial read with an empty owned set. */
    private BankStockObservation.Snapshot bankObservation() {
        return BankStockObservation.capture(client.getItemContainer(InventoryID.BANK),
                client::getItemDefinition);
    }
    private L01BankLayout.Provider findProvider() {
        BankTagsPlugin provider=null;
        for(var plugin:plugins.getPlugins()) if(plugins.isPluginEnabled(plugin)) {
            if(plugin instanceof BankTagsService tags && tags.getActiveTag()!=null) {
                status("Close the active Bank Tags / Inventory Setups view first.");return null;
            }
            if(plugin.getClass().getName().equals("com.questhelper.QuestHelperPlugin")) {
                try { if(Boolean.TRUE.equals(plugin.getClass().getMethod("isBankTabOpen").invoke(plugin))) {
                    status("Close the Quest Helper bank view first.");return null;
                }} catch(ReflectiveOperationException e) { status("Quest Helper bank-view state unavailable.");return null; }
            }
            if(plugin instanceof BankTagsPlugin tags) provider=tags;
        }
        if(provider==null) { status("Enable RuneLite's Bank Tags plugin, then click O again.");return null; }
        return new RuneLiteL01BankTags(plugins,provider);
    }
    /** Immutable published rectangle; overlay never dereferences live RuneLite widgets. */
    java.awt.Rectangle chestBounds() { return active?button.pressBounds():null; }
    boolean chestHasPlan() {
        var current=selectedSetup();
        return active && current!=null && current.plan()!=null && current.bankTagPermitted();
    }
    void menuAdded(MenuEntryAdded e) { if(active)button.routeMenu(e.getMenuEntry()); }
    void menuClicked(MenuOptionClicked e) { if(active)button.selectMenu(e); }
    void clear() { closeLayout();bound=null;button.clear(); }
    void stop() { active=false;clear(); }
    private void closeLayout() {
        var old=layout;layout=null;shown=null;shownAsBestBank=false;openingBestBankTab=false;shownBestBankGeneration=-1L;
        if(old!=null)old.close();
    }
    private void status(String message) {
        client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","TrashPanda Outfitter: "+message,null);
    }
}
