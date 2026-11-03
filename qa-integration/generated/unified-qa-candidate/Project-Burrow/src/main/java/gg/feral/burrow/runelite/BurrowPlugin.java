package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.pathfinder.SuggestionEngine;
import gg.feral.burrow.core.AccountModePreference;
import net.runelite.client.config.ConfigManager;
import gg.feral.burrow.core.LiveWeapon;
import gg.feral.burrow.core.LiveWeaponGate;
import gg.feral.burrow.core.LiveCombat;
import gg.feral.burrow.core.BestBankPreloader;
import gg.feral.burrow.core.QuestRecommendation;
import gg.feral.burrow.core.IronLedgerLootIntake;
import gg.feral.burrow.core.IronLedgerQuestSurvey;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.api.ChatMessageType;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.Experience;
import net.runelite.api.events.StatChanged;
import java.util.concurrent.atomic.AtomicBoolean;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ChatMessage;
import java.util.concurrent.CopyOnWriteArrayList;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import gg.feral.burrow.host.NavigationLifecycle;
import gg.feral.burrow.ui.BurrowPanel;
import gg.feral.burrow.ui.TrashPandaToolbarIcon;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.PluginPanel;
import java.awt.BorderLayout;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.UUID;
import gg.feral.burrow.ui.BurrowPanel.SessionScope;
import gg.feral.burrow.ui.BurrowPanel.SessionOrigin;
import gg.feral.burrow.ui.BurrowPanel.SessionEvent;
import gg.feral.burrow.ui.BurrowPanel.SessionFeed;
import gg.feral.burrow.ui.BurrowPanel.FeedStatus;


/** Minimal host adapter. BUR-0012 reads only the equipped weapon slot; never bank or inventory. */
@PluginDescriptor(name="TrashPanda Tracker [Experimental]", description="Feral Enterprises independent account companion; experimental")
public final class BurrowPlugin extends Plugin {
    @Inject private ClientToolbar toolbar;
    @Inject private ConfigManager configManager;
    private long accountModeHash = -1L;
    private long accountModeRevision;
    private AccountModePreference accountModes() {
        // Reflectively created legacy host fixtures have no injected ConfigManager.
        // In that restricted state, keep the product default (Ironman) for reads,
        // but never pretend a setting can be persisted. In RuneLite the normal
        // injected manager remains the sole storage source.
        ConfigManager manager = configManager;
        if (manager == null) {
            return new AccountModePreference(key -> null, (key, value) -> {
                throw new IllegalStateException("Account settings storage unavailable");
            });
        }
        return new AccountModePreference(
            key -> manager.getConfiguration(AccountModePreference.GROUP, key),
            (key, value) -> manager.setConfiguration(AccountModePreference.GROUP, key, value));
    }
    /** Build 19: per-account Pathfinder goal marks, same ConfigManager group as account mode. */
    private gg.feral.burrow.core.PathfinderGoalStore pathfinderGoals() {
        ConfigManager manager = configManager;
        if (manager == null) return new gg.feral.burrow.core.PathfinderGoalStore(key -> null, (key, value) -> {
            throw new IllegalStateException("Goal storage unavailable");
        });
        return new gg.feral.burrow.core.PathfinderGoalStore(
            key -> manager.getConfiguration(AccountModePreference.GROUP, key),
            (key, value) -> manager.setConfiguration(AccountModePreference.GROUP, key, value));
    }
    /** PQA-W06 isolated candidate: durable historical baseline and last accepted bank scan.
     *  This data is never fed into current/verified Pathfinder stock decisions. */
    private gg.feral.burrow.core.PathfinderAccountArchive pathfinderArchive() {
        ConfigManager manager = configManager;
        if (manager == null) return null; // no fake in-memory persistence in tests
        return new gg.feral.burrow.core.PathfinderAccountArchive(
            key -> manager.getConfiguration(AccountModePreference.GROUP, key),
            (key, value) -> manager.setConfiguration(AccountModePreference.GROUP, key, value));
    }
    /** PQA-W07 isolated capture journal. Does not imply complete log coverage. */
    private gg.feral.burrow.core.PathfinderGuidedCapture pathfinderGuidedCapture() {
        ConfigManager manager=configManager;
        if(manager==null)return null;
        return new gg.feral.burrow.core.PathfinderGuidedCapture(
            key -> manager.getConfiguration(AccountModePreference.GROUP,key),
            (key,value) -> manager.setConfiguration(AccountModePreference.GROUP,key,value));
    }
    // PQA-W08 candidate: publish historical account records only to the current
    // sidebar account. Never promote historical bank counts into live Pathfinder stock.
    private volatile long accountMemoryPublicationRevision;
    /** Numeric login fence for Pathfinder capture journals. SessionScope.loginEpoch is a String token (W09 type mismatch fix). */
    private volatile long sessionLoginFence;
    private void publishAccountMemory() {
        var current=hostPanel instanceof gg.feral.burrow.host.SidebarHost
            ? (gg.feral.burrow.host.SidebarHost)hostPanel : null;
        if(current==null)return;
        SessionScope scope=sessionScope;
        long hash=authenticatedAccountHash();
        boolean trusted=running.get() && scope!=null && !sessionPaused &&
            hash!=-1L && hash==sessionAccountHash;
        if(!trusted)hash=-1L;
        long stamped=hash;
        long revision=++accountMemoryPublicationRevision;
        long now=System.currentTimeMillis();
        gg.feral.burrow.ui.BankMemoryViewModel.Display memory;
        java.util.List<gg.feral.burrow.core.PathfinderGuidedCapture.ChecklistStep> steps=java.util.List.of();
        boolean initial=false,latest=false;
        try {
            var archive=pathfinderArchive();
            if(stamped!=-1L && archive!=null) {
                memory=gg.feral.burrow.ui.BankMemoryViewModel.renderForAccount(
                    stamped,stamped,archive.readBank(stamped),now,id->null);
                initial=archive.readInitial(stamped).isPresent();
                latest=archive.readLatest(stamped).isPresent();
                var journal=pathfinderGuidedCapture();
                if(journal!=null)steps=journal.checklist(stamped);
            } else {
                memory=gg.feral.burrow.ui.BankMemoryViewModel.renderForAccount(
                    -1L,-1L,java.util.Optional.empty(),now,id->null);
            }
        } catch(RuntimeException unavailable) {
            memory=gg.feral.burrow.ui.BankMemoryViewModel.renderForAccount(
                -1L,-1L,java.util.Optional.empty(),now,id->null);
            steps=java.util.List.of();initial=false;latest=false;
        }
        final var result=memory;
        final var checklist=java.util.List.copyOf(steps);
        final boolean first=initial,recent=latest;
        SwingUtilities.invokeLater(()-> {
            if(hostPanel==current && accountMemoryPublicationRevision==revision &&
               (stamped==-1L || (sessionScope==scope && sessionAccountHash==stamped &&
                    !sessionPaused)))current.publish(stamped,result,checklist,first,recent);
        });
    }
    private boolean mayCaptureFor(long expected) {
        return running.get() && sessionScope!=null && !sessionPaused &&
            expected!=-1L && authenticatedAccountHash()==expected && sessionAccountHash==expected;
    }
    /** Convert the active SessionScope string login token into the numeric fence Pathfinder journals require. */
    private long captureLoginEpoch(SessionScope scope) {
        if (scope == null || sessionScope != scope) return 0L;
        long fence = sessionLoginFence;
        return fence > 0L ? fence : 0L;
    }

    /** Explicit user confirmation is only a review receipt, not KC or diary truth. */
    private void markCaptureScreenReviewed(long expected,
        gg.feral.burrow.core.PathfinderGuidedCapture.Section section) {
        if(!mayCaptureFor(expected) || section==null)return;
        try {
            var journal=pathfinderGuidedCapture();
            long now=System.currentTimeMillis();
            if(journal!=null && journal.markPlayerReviewed(
                new gg.feral.burrow.core.PathfinderGuidedCapture.Context(
                    expected,sessionAccountHash,authenticatedAccountHash(),
                    captureLoginEpoch(sessionScope),running.get(),sessionPaused,now),
                section,"manual-review:"+section.name(),now))publishAccountMemory();
        } catch(RuntimeException unavailable) { /* recording failure is not completion */ }
    }
    /** Request new bounded quest observations; no game UI clicks or guessed unlocks. */
    private void requestQuestRefresh(long expected) {
        if(!mayCaptureFor(expected))return;
        beginQuestSurvey(expected);questTickCountdown=0;
        publishPathfinderSuggestions();
    }
    /** The existing bank widget reader automatically saves the next accepted scan.
     * This action only explains that process; it never opens the bank or forces
     * a new scan (especially during an active Best-in-Bank/O withdrawal lease). */
    private void requestBankMemoryRefresh(long expected) {
        if(!mayCaptureFor(expected))return;
        var current=hostPanel instanceof gg.feral.burrow.host.SidebarHost
            ? (gg.feral.burrow.host.SidebarHost)hostPanel : null;
        publishAccountMemory();
        // Queue the instruction AFTER the fresh history publication, so a
        // redisplay cannot immediately erase the requested next-step guidance.
        SwingUtilities.invokeLater(()-> {
            if(hostPanel==current && current!=null)current.showBankRefreshInstruction(expected);
        });
    }
    /** Build 20: per-account Rare Drop Tracker store (same ConfigManager group). */
    private gg.feral.burrow.core.RareDropStore rareDrops() {
        ConfigManager manager = configManager;
        if (manager == null) return new gg.feral.burrow.core.RareDropStore(key -> null, (key, value) -> {
            throw new IllegalStateException("Rare drop storage unavailable");
        });
        return new gg.feral.burrow.core.RareDropStore(
            key -> manager.getConfiguration(AccountModePreference.GROUP, key),
            (key, value) -> manager.setConfiguration(AccountModePreference.GROUP, key, value));
    }
    /** Client thread. True only for the live, verified, unpaused session account. */
    private boolean rareDropAccountOk(long hash) {
        return running.get() && hash != -1L && sessionScope != null && !sessionPaused &&
            authenticatedAccountHash() == hash && sessionAccountHash == hash;
    }
    private volatile long rareDropRevision;
    /** Client thread. Publishes this account's tracker state, or null (signed out) otherwise. */
    private void publishRareDrops() {
        BurrowPanel target = panel;
        if (target == null) return;
        long hash = authenticatedAccountHash();
        long revision = ++rareDropRevision;
        gg.feral.burrow.core.RareDropStore.State state = null;
        if (rareDropAccountOk(hash)) {
            try { state = rareDrops().read(hash); } catch (RuntimeException unavailable) { state = null; }
        }
        final gg.feral.burrow.core.RareDropStore.State published = state;
        final long publishedHash = state == null ? -1L : hash;
        SwingUtilities.invokeLater(() -> {
            if (panel == target && rareDropRevision == revision) target.setRareDropState(publishedHash, published);
        });
    }
    private void rareDropWrite(long hash, java.util.function.Consumer<gg.feral.burrow.core.RareDropStore> write) {
        if (!rareDropAccountOk(hash)) return;
        try { write.accept(rareDrops()); } catch (RuntimeException unavailable) { return; }
        publishPathfinderSuggestions(); // also republishes the tracker state
    }
    // ---- R11: read-only full collection-log capture (script 4100 callbacks; never triggered by TrashPanda) ----
    private final gg.feral.burrow.core.CollectionLogCapture clogCapture = new gg.feral.burrow.core.CollectionLogCapture();
    private long clogCaptureAccount = -1L;
    private boolean clogReplacementsLoaded;
    /** CP-4 (BQA-R12-015): RuneLite binds the native settings page (plugin-list cog) to this interface. */
    @com.google.inject.Provides
    TrashPandaConfig provideConfig(ConfigManager manager) {
        return manager.getConfig(TrashPandaConfig.class);
    }
    /** CP-4: a change made from the native settings page is applied to the open sidebar on the EDT. */
    @Subscribe public void onConfigChanged(net.runelite.client.events.ConfigChanged event) {
        if (event == null || !TrashPandaConfig.GROUP.equals(event.getGroup()) || configManager == null) return;
        if (TrashPandaConfig.FILL_FOCUS.equals(event.getKey())) {
            applyBestBankFocus(gg.feral.burrow.core.OwnedGearStats.Focus.parse(event.getNewValue()));
            return;
        }
        BurrowPanel target = panel;
        if (target == null) return;
        String size = configManager.getConfiguration(TrashPandaConfig.GROUP, TrashPandaConfig.FONT_SIZE);
        String accent = configManager.getConfiguration(TrashPandaConfig.GROUP, TrashPandaConfig.ACCENT);
        String weight = configManager.getConfiguration(TrashPandaConfig.GROUP, TrashPandaConfig.TEXT_WEIGHT);
        SwingUtilities.invokeLater(() -> {
            if (panel == target) { target.setAppearance(size, accent); target.setTextWeight(weight); }
        });
    }

    /** CP-7: new focus re-plans the same evidence and refreshes the visible plan. */
    private void applyBestBankFocus(gg.feral.burrow.core.OwnedGearStats.Focus focus) {
        if (bankPreloader == null || focus == null) return;
        long before = bankPreloader.generation();
        BurrowPanel target = panel;
        if (target != null) SwingUtilities.invokeLater(() -> { if (panel == target) target.setBestBankFocus(focus); });
        if (bankPreloader.setFocus(focus) != before) refreshBankPlanOnPanel();
    }

    @Subscribe public void onScriptPreFired(net.runelite.api.events.ScriptPreFired event) {
        if (!running.get() || event == null || event.getScriptId() != gg.feral.burrow.core.CollectionLogCapture.SCRIPT_ID) return;
        long hash = authenticatedAccountHash();
        if (!rareDropAccountOk(hash)) { clogCapture.reset(); return; }
        if (clogCaptureAccount != hash) { clogCapture.reset(); clogCaptureAccount = hash; }
        if (!clogReplacementsLoaded) loadClogReplacements();
        Object[] args;
        try { args = event.getScriptEvent() == null ? null : event.getScriptEvent().getArguments(); } catch (RuntimeException e) { args = null; }
        clogCapture.offer(event.getScriptId(), args, client.getTickCount());
    }
    /** Cache enum 3721 (item id -> replacement id), read-only, as WikiSync does; failure leaves ids unmapped. Client thread. */
    private void loadClogReplacements() {
        clogReplacementsLoaded = true;
        try {
            var e = client.getEnum(3721);
            if (e == null) return;
            int[] keys = e.getKeys(), vals = e.getIntVals();
            var map = new java.util.HashMap<Integer,Integer>();
            for (int i = 0; i < Math.min(keys.length, vals.length); i++) map.put(keys[i], vals[i]);
            clogCapture.setReplacements(map);
        } catch (RuntimeException unavailable) { /* unmapped ids still match through reviewed equivalents */ }
    }
    /** Client tick: commit a finished capture for the SAME authenticated account only. */
    private void flushClogCapture() {
        // R11 fix (native Gradle run): never read the client on a tick with nothing captured.
        if (clogCapture.pendingCount() == 0 || !clogCapture.ready(client.getTickCount())) return;
        long hash = authenticatedAccountHash();
        var ids = clogCapture.drain();
        if (hash != clogCaptureAccount || !rareDropAccountOk(hash)) return;
        rareDropWrite(hash, store -> store.observeSearchUnlocks(hash, ids, System.currentTimeMillis()));
    }
    /** Read-only Collection Log observer: runs after the game draws an entry page. */
    @Subscribe public void onScriptPostFired(net.runelite.api.events.ScriptPostFired event) {
        if (!running.get() || event == null || event.getScriptId() != net.runelite.api.ScriptID.COLLECTION_DRAW_LIST) return;
        long hash = authenticatedAccountHash();
        if (!rareDropAccountOk(hash)) return;
        var observation = CollectionLogObserver.read(client);
        if (observation.isEmpty()) return;
        if (!clogReplacementsLoaded) loadClogReplacements();
        var canonical = clogCapture.canonical(observation.get()); // R11: cache replacement ids, raw ids kept
        rareDropWrite(hash, store -> store.observe(hash, canonical, System.currentTimeMillis()));
        // PQA-W07: reuse the EXISTING Collection Log widget read. Preserve
        // only exact witnessed page rows and positive obtained IDs; no absent
        // page/slot is interpreted as zero, and this never marks the whole log
        // complete. A later login/account cannot reuse this event.
        try {
            SessionScope scope=sessionScope;
            var journal=pathfinderGuidedCapture();
            if(journal!=null && scope!=null && rareDropAccountOk(hash)) {
                long now=System.currentTimeMillis();
                journal.observeCollectionPage(
                    new gg.feral.burrow.core.PathfinderGuidedCapture.Context(
                        hash,sessionAccountHash,authenticatedAccountHash(),captureLoginEpoch(scope),
                        running.get(),sessionPaused,now),
                    canonical.page(),canonical.killCounts(),canonical.obtained(),now);
                publishAccountMemory();
            }
        }catch(RuntimeException journalUnavailable) {
            // Historical QA recording must never disrupt gameplay or KC tracking.
        }
    }
    // PQA-W09: one screen-opening witness, fenced to account AND login epoch.
    // No automatic game actions, bank reads, KC completion inference, or telemetry.
    private gg.feral.burrow.core.KillLogCaptureFence.Pending pendingKillLog;

    @Subscribe public void onWidgetLoaded(net.runelite.api.events.WidgetLoaded event) {
        if (!running.get() || event == null ||
            event.getGroupId() != net.runelite.api.gameval.InterfaceID.KILL_LOG) return;
        SessionScope scope = sessionScope;
        long hash = authenticatedAccountHash();
        if (scope == null || captureLoginEpoch(scope) <= 0 || !mayCaptureFor(hash)) return;
        pendingKillLog = new gg.feral.burrow.core.KillLogCaptureFence.Pending(
            hash, captureLoginEpoch(scope), client.getTickCount());
    }
    /** Defer until after widget children populate, then capture only exact visible rows. */
    private void pollPendingKillLog() {
        var pending = pendingKillLog;
        if (pending == null) return;
        SessionScope scope = sessionScope;
        long tick = client.getTickCount();
        if (tick == pending.openingTick()) return; // may still be rendering
        if (scope == null || !gg.feral.burrow.core.KillLogCaptureFence.mayRead(
                pending, sessionAccountHash, authenticatedAccountHash(), captureLoginEpoch(scope),
                running.get() && !sessionPaused && client.getGameState()==GameState.LOGGED_IN,
                sessionPaused, tick)) {
            pendingKillLog = null; return;
        }
        var parsed = KillLogWidgetReader.read(client);
        if (parsed.isEmpty()) return; // leave pending briefly until the widgets settle
        pendingKillLog = null; // at most one accepted page per load event
        try {
            var journal = pathfinderGuidedCapture();
            if (journal == null) return;
            long now = System.currentTimeMillis();
            boolean stored = journal.observeKillLogPage(
                new gg.feral.burrow.core.PathfinderGuidedCapture.Context(
                    pending.accountHash(), sessionAccountHash, authenticatedAccountHash(),
                    captureLoginEpoch(scope), running.get(), sessionPaused, now),
                parsed.get(), now);
            if (stored) publishAccountMemory();
        } catch (RuntimeException unavailable) {
            // A journal failure must not interrupt the client or its bank/Slayer state.
        }
    }

    /** Explicit owner click only. Unranked/unavailable results stay UNKNOWN, never zero. */
    private void lookupHiscoreKc(long hash, String encounter) {
        if (!rareDropAccountOk(hash) || hiscoreClient == null) { rareDropFeedback("HiScores unavailable"); return; }
        var skill = HiscoreKc.skillFor(encounter);
        if (skill.isEmpty()) { rareDropFeedback("No official HiScores entry for " + encounter); return; }
        String name;
        try { name = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getName(); } catch (RuntimeException e) { name = null; }
        if (name == null || name.isBlank()) { rareDropFeedback("HiScores: player name not available yet"); return; }
        net.runelite.client.hiscore.HiscoreEndpoint endpoint;
        try { endpoint = HiscoreKc.endpointFor(client.getVarbitValue(net.runelite.api.Varbits.ACCOUNT_TYPE)); }
        catch (RuntimeException e) { endpoint = net.runelite.client.hiscore.HiscoreEndpoint.NORMAL; }
        final String player = name;
        // BQA-902 F08: fence by login scope + account + generation, not by hash alone (A -> B -> A safe).
        final gg.feral.burrow.core.AsyncRequestFence.Ticket ticket = hiscoreFence.issue(sessionScope, hash);
        hiscoreClient.lookupAsync(player, endpoint).whenComplete((result, error) -> clientThread.invoke(() -> {
            if (!rareDropAccountOk(hash) || !hiscoreFence.admits(ticket, sessionScope, sessionAccountHash)) return;
            var kc = error == null ? HiscoreKc.kc(result, skill.get()) : java.util.OptionalLong.empty();
            if (kc.isEmpty()) { rareDropFeedback("HiScores: not ranked or unavailable · KC stays unknown"); return; }
            rareDropFeedback(null);
            rareDropWrite(hash, store -> store.recordKc(hash, encounter, gg.feral.burrow.core.RareDropStore.Source.HISCORES,
                kc.getAsLong(), System.currentTimeMillis()));
        }));
    }
    private void rareDropFeedback(String text) {
        BurrowPanel target = panel;
        if (target != null) SwingUtilities.invokeLater(() -> { if (panel == target) target.setRareDropFeedback(text); });
    }
    @Inject private net.runelite.client.hiscore.HiscoreClient hiscoreClient;
    private final gg.feral.burrow.core.AsyncRequestFence hiscoreFence = new gg.feral.burrow.core.AsyncRequestFence();
    /** Client thread. Explicit owner click only; ignored for any other/old account. */
    private void savePathfinderGoal(long requestedHash, String ruleId, SuggestionEngine.GoalMark mark) {
        if (!running.get() || requestedHash == -1L || ruleId == null || sessionScope == null || sessionPaused ||
            authenticatedAccountHash() != requestedHash || sessionAccountHash != requestedHash) return;
        try {
            if (mark == SuggestionEngine.GoalMark.SAVED)
                pathfinderGoals().setSingleActive(requestedHash,ruleId,true);
            else pathfinderGoals().set(requestedHash,ruleId,mark);
        }
        catch (RuntimeException unavailable) { return; }
        publishPathfinderSuggestions();
    }
    /** RuneLite client-thread only. A settings choice must never cross an account boundary. */
    private void saveAccountMode(long requestedHash, AccountModePreference.Mode mode) {
        if (!running.get() || requestedHash == -1L || mode == null ||
            sessionScope == null || sessionPaused || authenticatedAccountHash() != requestedHash ||
            sessionAccountHash != requestedHash) return;
        accountModes().save(requestedHash,mode);
        publishAccountMode(requestedHash);
        publishPathfinderSuggestions(); // Ironman/Main changes recommendation logic
    }
    private void publishAccountMode(long observedHash) {
        if (!running.get()) return;
        long current = authenticatedAccountHash();
        long hash = observedHash != -1L && observedHash == current &&
            sessionScope != null && sessionAccountHash == current ? current : -1L;
        accountModeHash = hash;
        long revision = ++accountModeRevision;
        AccountModePreference.Mode choice = hash == -1L ? null : accountModes().resolve(hash);
        BurrowPanel destination = panel;
        if (destination == null) return;
        SwingUtilities.invokeLater(() -> {
            if (running.get() && panel == destination && accountModeRevision == revision &&
                accountModeHash == hash) destination.setAccountModeState(hash, choice);
        });
    }
    private void publishKnownAccountMode() {
        if (!running.get()) return;
        refreshAccountModeIfChanged();
        if (authenticatedAccountHash() == -1L) publishAccountMode(-1L);
    }
    private void refreshAccountModeIfChanged() {
        long hash = authenticatedAccountHash();
        if (hash == -1L || sessionScope == null || sessionAccountHash != hash) hash = -1L;
        if (hash != accountModeHash) publishAccountMode(hash);
    }

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private PluginManager pluginManager;
    @Inject private OverlayManager overlayManager;
    @Inject private ItemManager itemManager;
    @Inject private SkillIconManager skillIconManager;
    private final ConcurrentHashMap<Integer,BufferedImage> itemImages = new ConcurrentHashMap<>();
    private BankChestOverlay chestOverlay;
    // Client-thread-only controller. The published selection snapshot is immutable.
    private OutfitterBank outfitterBank;
    private volatile BurrowModel activeModel;
    private final LiveWeaponGate weaponGate = new LiveWeaponGate();
    // R12: client-thread bank-open snapshots; all catalog preparation happens on a
    // low-priority daemon worker with immutable values and session revision fencing.
    private BestBankPreloader bankPreloader = new BestBankPreloader(BestBankPreloader.knownProfiles());
    private volatile boolean bankWasOpen;
    private volatile boolean bankNeedsSnapshot;
    private int bankReadCooldown;
    private volatile long bankSnapshotAccount=-1L;
    private volatile SessionScope bankSnapshotScope;
    private volatile long bankSnapshotGeneration;
    /** Last accepted complete source-qualified read; never exposed to UI or logs. */
    private gg.feral.burrow.core.BankCaptureGate.Read acceptedBankRead;
    /** ItemContainerChanged can report unchanged bank contents during Bank Tags navigation.
     * Fence the O layout until the next tick independently verifies the SAME content. */
    private volatile boolean pendingBankEventVerification;
    /** R05: a verified temporary O withdrawal tab is a frozen-layout lease.
     * Do not reread a Bank Tags-filtered widget or invalidate on its own redraw events.
     * Once the user leaves O, force a new source-qualified bank scan before any new plan.
     */
    private boolean bankOWithdrawalLeaseActive;

    private boolean verifiedBestBankWithdrawalLease() {
        OutfitterBank current=outfitterBank;
        long account=authenticatedAccountHash();
        return gg.feral.burrow.core.BankWithdrawalFreezeGate.authorized(
            current!=null && current.selectedBestBankWithdrawalTab(),
            AutomaticBankSnapshotReader.visible(client),
            sessionScope!=null && !sessionPaused && bankSnapshotScope==sessionScope,
            gg.feral.burrow.core.BankCaptureGate.accountAvailable(account)
                && account==bankSnapshotAccount && account==sessionAccountHash,
            bankWasOpen && acceptedBankRead!=null && bankOpenEpoch==acceptedBankOpenEpoch,
            !bankNeedsSnapshot && !pendingBankEventVerification
                && bankScan.phase()==gg.feral.burrow.core.BankScanTracker.Phase.ACCEPTED);
    }

    /** R03: consecutive, source-qualified post-event reads, reset on EVERY new event. */
    private final gg.feral.burrow.core.BankRevalidationWitness bankRevalidationWitness =
        new gg.feral.burrow.core.BankRevalidationWitness();
    /** Each visible opening starts a new ownership epoch; closed-bank evidence never authorizes O. */
    private volatile long bankOpenEpoch;
    private volatile long acceptedBankOpenEpoch=-1L;
    private void scheduleAutomaticBankObservation() {
        if(!bankNeedsSnapshot)bankReadCooldown=2;
        bankNeedsSnapshot=true;
    }
    // Build 19: previous same-account capture; a snapshot is accepted only when two consecutive
    // tick reads are identical (stability witness). Client-thread only.
    private AutomaticBankSnapshotReader.Snapshot pendingBankCapture;
    private volatile gg.feral.burrow.core.ItemFacts bankItemFacts;
    // BQA-901 Batch A: typed, reasoned bank-read state for this login (client thread only).
    private final gg.feral.burrow.core.BankScanTracker bankScan=new gg.feral.burrow.core.BankScanTracker();
    private gg.feral.burrow.core.BankScanTracker.Status publishedBankScanStatus;
    /** CP-1: inventory/worn changed while the bank was closed; re-read both on the next tick (client thread). */
    private boolean carriedDirty;
    private void checkAutomaticBankOnTick() {
        boolean open=AutomaticBankSnapshotReader.visible(client);
        if(!open) {
            // A queued event cannot be verified after the bank becomes unavailable.
            if(pendingBankEventVerification) {
                pendingBankEventVerification=false;
                bankRevalidationWitness.reset();
                retireBankAfterMutation(false);
            }
            if(bankWasOpen) {
                // Keep the accepted observation, but label it AS OF BANK CLOSE (new generation).
                long before=bankPreloader.generation();
                if(bankPreloader.markBankClosed()!=before)refreshBankPlanOnPanel();
                bankScan.bankClosed();
                experienceBridge.invalidateEvidence();
                experienceEvidenceDirty = true;
                publishBankScanStatus();
            }
            boolean hadOpenBank=bankWasOpen;
            bankWasOpen=false;bankNeedsSnapshot=false;pendingBankCapture=null;bankRevalidationWitness.reset();
            if(carriedDirty){carriedDirty=false;refreshCarriedFromClient();}
            if(hadOpenBank)publishPathfinderSuggestions(); // retire live quantities when bank closes
            return;
        }
        carriedDirty=false; // bank open: the full snapshot path below re-reads everything
        SessionScope scope=sessionScope;
        long account=authenticatedAccountHash();
        // BF-03: only RuneLite's documented -1 sentinel means "unavailable"; other negative hashes are valid.
        if(scope!=null && gg.feral.burrow.core.BankCaptureGate.accountAvailable(account) && account==sessionAccountHash)
            bankScan.bind(scope,account);
        if(!bankWasOpen) {
            bankWasOpen=true; bankOpenEpoch++; acceptedBankOpenEpoch=-1L;
            pendingBankCapture=null;bankRevalidationWitness.reset();bankScan.bankOpened();scheduleAutomaticBankObservation();
            publishPathfinderSuggestions(); // never carry old bank quantities into a new opening
            // The last bank close is informational only. Never show it as fresh owned gear.
            BurrowPanel target=panel;
            if(target!=null)SwingUtilities.invokeLater(()->{ if(panel==target)target.clearBankScan(); });
            publishBankScanStatus();
        }
        if(pendingBankEventVerification) {
            var fresh=AutomaticBankSnapshotReader.evaluate(client);
            var verdict=gg.feral.burrow.core.BankEventRevalidation.classify(
                acceptedBankRead,fresh,bankSnapshotScope,scope,account);
            var revalidated=bankRevalidationWitness.observe(verdict,
                fresh!=null && fresh.ready()?fresh.read():null);
            switch(revalidated) {
                case RECOVER -> {
                    pendingBankEventVerification=false;
                    bankRevalidationWitness.reset();
                    bankScan.verificationMatched();
                    publishBankScanStatus();
                    // Do not publish ownership until consecutive ready reads match.
                    if(panel!=null && panel.isBankMatchView())requestSelectedBestBank();
                    return;
                }
                case WAIT -> {
                    bankScan.verificationIndeterminate(fresh==null?null:
                        verdict==gg.feral.burrow.core.BankEventRevalidation.Verdict.MATCH
                            ?gg.feral.burrow.core.BankCaptureGate.Outcome.CHANGING_SAMPLE
                            :fresh.outcome(),System.currentTimeMillis());
                    publishBankScanStatus();
                    // Keep the visible tag shell, but quarantine all ownership evidence.
                    return;
                }
                case RETIRE -> retireBankAfterMutation(true);
            }
        }
        if(!bankNeedsSnapshot || --bankReadCooldown>0)return;
        long now=System.currentTimeMillis();
        if(!gg.feral.burrow.core.BankCaptureGate.accountAvailable(account)) {
            bankReadCooldown=bankScan.failedBeforeRead(gg.feral.burrow.core.BankCaptureGate.Outcome.IDENTITY_UNAVAILABLE,now).nextDelayTicks();
            publishBankScanStatus();return;
        }
        if(scope==null || sessionPaused || account!=sessionAccountHash) {
            // Previously a silent return: now visible as "login not verified yet" and retried.
            bankReadCooldown=bankScan.failedBeforeRead(gg.feral.burrow.core.BankCaptureGate.Outcome.SESSION_NOT_READY,now).nextDelayTicks();
            publishBankScanStatus();return;
        }
        var decision=bankScan.offer(AutomaticBankSnapshotReader.evaluate(client),now);
        if(decision.kind()!=gg.feral.burrow.core.BankScanTracker.Decision.Kind.ACCEPT) {
            // Bank containers can lag behind the widget. Retry on a later tick; no empty, changing or
            // incomplete read is ever treated as absence evidence.
            bankReadCooldown=Math.max(1,decision.nextDelayTicks());
            publishBankScanStatus();return;
        }
        var shot=decision.accepted();
        acceptedBankRead=shot;
        pendingBankCapture=null;
        bankNeedsSnapshot=false;
        var evidence=new gg.feral.burrow.core.BankEvidence(shot.bank(),shot.inventory(),shot.equipment(),
            shot.tabWitnessed()?gg.feral.burrow.core.BankEvidence.Population.STABLE_TAB_COUNTS_CONSISTENT:
                gg.feral.burrow.core.BankEvidence.Population.STABLE_READ,
            now,true,false);
        bankSnapshotAccount=account;
        bankSnapshotScope=scope;
        var gearStats=new java.util.HashMap<>(catalogEquipmentStats());
        gearStats.putAll(ownedEquipmentStats(shot.combined().keySet()));
        bankSnapshotGeneration=bankPreloader.observe(evidence,currentAccountContext(account,scope),hostItemFacts(),shot.names(),gearStats);
        acceptedBankOpenEpoch=bankOpenEpoch;
        publishRecipeEvidence();
        pathfinderBankScope=scope;pathfinderBankAccount=account;
        pathfinderPositiveBankItems=Set.copyOf(shot.combined().keySet());
        publishPathfinderSuggestions();
        publishBankScanStatus();
        final long observedGeneration=bankSnapshotGeneration;
        final String carriedNote=shot.carriedNote();
        BurrowPanel target=panel;
        if(target!=null)SwingUtilities.invokeLater(()-> {
            if(panel!=target || scope!=sessionScope || bankPreloader.generation()!=observedGeneration)return;
            target.clearBankScan();
            target.showAutomaticBankStatus("Auto-read: "+evidence.trustLine()+(carriedNote.isEmpty()?"":" · "+carriedNote));
            if(target.isBankMatchView())requestSelectedBestBank();
        });
    }
    /** BQA-901: push the typed bank-read status to the Outfitter card (no hashes, no item lists). */
    private void publishBankScanStatus() {
        var status=bankScan.status();
        if(status.equals(publishedBankScanStatus))return;
        publishedBankScanStatus=status;
        BurrowPanel target=panel;
        SessionScope scope=sessionScope;
        if(target==null)return;
        long now=System.currentTimeMillis();
        String head=status.headline(), detail=status.detail(now);
        SwingUtilities.invokeLater(()-> {
            if(panel==target && scope==sessionScope)target.showBankCaptureState(head,detail);
        });
    }
    /** CP-4: Chronicle recipe goals read the SAME evidence (and stale/closed state) as Best in Bank; null clears. */
    private void publishRecipeEvidence() {
        BurrowPanel target=panel;
        if(target==null || bankPreloader==null)return;
        var ev=bankPreloader.evidence();
        SwingUtilities.invokeLater(()-> { if(panel==target) target.setRecipeEvidence(ev); });
    }
    /** Re-request the selected plan after a generation change (close/stale/context). */
    private void refreshBankPlanOnPanel() {
        publishRecipeEvidence();
        BurrowPanel target=panel;
        var ev=bankPreloader.evidence();
        if(target==null || ev==null)return;
        String status="Auto-read: "+ev.trustLine();
        SwingUtilities.invokeLater(()-> {
            if(panel!=target)return;
            target.showAutomaticBankStatus(status);
            if(target.isBankMatchView())requestSelectedBestBank();
        });
    }
    /** Observed same-login levels and quest states; unknown entries stay absent. Client thread. */
    private gg.feral.burrow.core.AccountContext currentAccountContext(long hash, SessionScope scope) {
        var levels=new java.util.LinkedHashMap<String,Integer>();
        try {
            for(Skill skill:Skill.values()) {
                if(skill==Skill.OVERALL)continue;
                int level=client.getRealSkillLevel(skill);
                if(level>=1 && level<=99)levels.put(skill.getName(),level);
            }
        } catch(RuntimeException unavailable) {levels.clear();}
        var quests=new java.util.LinkedHashMap<String,String>();
        if(questSurveyAccount==hash && questSurveyStarted && scope==sessionScope)
            quests.putAll(questSurvey.snapshot().quests());
        return new gg.feral.burrow.core.AccountContext(levels,quests);
    }
    /** RuneLite's bundled item stats (slot, two-handed) for catalog items, captured once. Client thread. */
    private gg.feral.burrow.core.ItemFacts hostItemFacts() {
        var cached=bankItemFacts;
        if(cached!=null)return cached;
        var base=gg.feral.burrow.core.ItemFacts.catalogDerived();
        var host=new java.util.HashMap<Integer,gg.feral.burrow.core.ItemFacts.Fact>();
        java.util.Set<Integer> ids=new java.util.HashSet<>();
        for(var key:BestBankPreloader.knownProfiles()) {
            var sel=gg.feral.burrow.core.TieredOutfitterCatalog.find(key.boss(),key.method(),key.tier());
            if(sel==null)continue;
            for(var it:sel.profile().gear()) if(it!=null && it.id()>0) ids.add(it.id());
            for(var it:sel.profile().inventory()) if(it!=null && it.id()>0) ids.add(it.id());
        }
        for(int id:ids) {
            try {
                var stats=itemManager==null?null:itemManager.getItemStats(id);
                if(stats==null || !stats.isEquipable() || stats.getEquipment()==null)continue;
                var eq=stats.getEquipment();
                var hands=eq.getSlot()==gg.feral.burrow.core.ItemFacts.WEAPON?
                    (eq.isTwoHanded()?gg.feral.burrow.core.ItemFacts.Hands.TWO:gg.feral.burrow.core.ItemFacts.Hands.ONE):
                    gg.feral.burrow.core.ItemFacts.Hands.ONE;
                host.put(id,new gg.feral.burrow.core.ItemFacts.Fact(eq.getSlot(),hands,"RuneLite ItemManager item stats"));
            } catch(RuntimeException unavailable) { /* catalog fact remains */ }
        }
        var merged=base.overlay(gg.feral.burrow.core.ItemFacts.of(host));
        if(!host.isEmpty())bankItemFacts=merged;
        return merged;
    }
    // ---- CP-8: the bank tag tab (O) shows the Best in Bank plan the sidebar shows ----
    private record BestBankLayout(BestBankPreloader.Key key, long generation, int[] ids) {}
    private volatile BestBankLayout bestBankLayout;
    /** EDT: remember the displayed plan as 56 L01 positions (owned item id, -1 = leave empty). */
    private void publishBestBankLayout(BestBankPreloader.Prepared prepared) {
        if(prepared==null || prepared.plan()==null){bestBankLayout=null;return;}
        var plan=prepared.plan();
        int[] ids=new int[56];
        for(int i=0;i<11;i++) {
            ids[gg.feral.burrow.core.Outfitter.GEAR_POSITIONS.get(i)]=
                gg.feral.burrow.core.BankLayoutEligibility.wornId(plan.gear().get(i));
        }
        for(int j=0;j<28;j++) {
            ids[gg.feral.burrow.core.Outfitter.inventoryPosition(j)]=
                gg.feral.burrow.core.BankLayoutEligibility.inventoryId(plan.inventory().get(j));
        }
        bestBankLayout=new BestBankLayout(prepared.key(),prepared.generation(),ids);
    }
    /** The only evidence allowed to feed a live owned-item O layout. */
    private gg.feral.burrow.core.BankEvidence verifiedLiveBankEvidence() {
        long account=authenticatedAccountHash();
        var evidence=bankPreloader.evidence();
        return bankWasOpen && !bankNeedsSnapshot && !pendingBankEventVerification
            && bankOpenEpoch==acceptedBankOpenEpoch
            && bankScan.phase()==gg.feral.burrow.core.BankScanTracker.Phase.ACCEPTED
            && bankSnapshotScope!=null && bankSnapshotScope==sessionScope
            && gg.feral.burrow.core.BankCaptureGate.accountAvailable(account)
            && bankSnapshotAccount==account && sessionAccountHash==account
            && evidence!=null && evidence.bankStillOpen() && !evidence.stale()
            ?evidence:null;
    }
    /** Client thread: the override only while Best Bank is on and the same boss/method/tier is selected. */
    private int[] currentBestBankLayout() {
        var layout=bestBankLayout; var model=activeModel; var target=panel;
        if(layout==null || model==null || target==null)return null;
        if(!layout.key().equals(new BestBankPreloader.Key(model.boss(),model.method(),model.tier())))return null;
        long currentAccount=authenticatedAccountHash();
        if(!gg.feral.burrow.core.BestBankLayoutGate.permitted(
            layout.generation(),bankPreloader.generation(),
            bankSnapshotAccount!=-1L && bankSnapshotAccount==currentAccount && sessionAccountHash==currentAccount,
            bankSnapshotScope!=null && bankSnapshotScope==sessionScope,
            target.bestBankToggleOnForHost(),
            gg.feral.burrow.core.BossRegistry.bestBankAllowed(model.boss()),bankPreloader.evidence(),
            bankWasOpen,bankOpenEpoch==acceptedBankOpenEpoch,
            bankScan.phase()==gg.feral.burrow.core.BankScanTracker.Phase.ACCEPTED && !bankNeedsSnapshot,
            pendingBankEventVerification))return null;
        return layout.ids().clone();
    }
    private void requestSelectedBestBank() {
        BurrowPanel target=panel;
        if(target==null || !target.isBankMatchView())return;
        var model=activeModel;
        if(model==null || model.view()!=BurrowModel.View.OUTFITTER)return;
        // No queued planner result may republish bank-as-of-close or quarantined owned gear.
        if(verifiedLiveBankEvidence()==null) {
            SwingUtilities.invokeLater(()->{
                if(panel==target && target.isBankMatchView()) {
                    target.clearBankScan();
                    target.showAutomaticBankStatus("Open bank for a fresh verified owned layout; previous bank read is history only.");
                }
            });
            return;
        }
        long account=bankSnapshotAccount;
        if(bankSnapshotScope==null || bankSnapshotScope!=sessionScope) {
            SwingUtilities.invokeLater(()->{ if(panel==target)target.clearBankScan(); });
            return;
        }
        long stamp=bankPreloader.generation();
        var key=new BestBankPreloader.Key(model.boss(),model.method(),model.tier());
        // A new focus, task, item observation or selection invalidates the displayed
        // plan. All Swing state writes are fenced on the EDT, before result delivery.
        SwingUtilities.invokeLater(()->{
            if(panel!=target || bankPreloader.generation()!=stamp || activeModel!=model ||
                !target.isBankMatchView() || model.view()!=BurrowModel.View.OUTFITTER ||
                !key.equals(new BestBankPreloader.Key(model.boss(),model.method(),model.tier())))return;
            target.clearBankScan();
            target.showAutomaticBankStatus(account==-1L?"Open bank to scan all owned gear":"Preparing selected owned matches...");
        });
        if(account==-1L)return; // BF-03: -1 is the only unavailable sentinel
        bankPreloader.request(key,result-> SwingUtilities.invokeLater(()-> {
            if(!running.get() || panel!=target || activeModel!=model ||
                verifiedLiveBankEvidence()==null ||
                bankPreloader.generation()!=stamp || bankSnapshotAccount!=account ||
                !target.isBankMatchView() || model.view()!=BurrowModel.View.OUTFITTER ||
                !key.equals(new BestBankPreloader.Key(model.boss(),model.method(),model.tier())))return;
            var ev=bankPreloader.evidence();
            publishBestBankLayout(result);
            target.automaticBankResult(result,(ev==null?"":ev.trustLine()+" · ")+
                "prepared "+bankPreloader.preparedCount()+" / "+bankPreloader.profileCount());
        }));
    }
    private void clearObservedGearBank() {
        bankOWithdrawalLeaseActive=false;
        bankWasOpen=false;bankNeedsSnapshot=false;bankSnapshotAccount=-1L;bankSnapshotScope=null;
        pendingBankCapture=null;acceptedBankRead=null;pendingBankEventVerification=false;bankRevalidationWitness.reset();acceptedBankOpenEpoch=-1L;
        bankScan.invalidated(false);publishBankScanStatus();
        bankPreloader.clear();bestBankLayout=null;clearPathfinderBankEvidence();publishRecipeEvidence();
        BurrowPanel target=panel;
        if(target!=null)SwingUtilities.invokeLater(()-> {
            if(panel==target)target.clearBankScan();
        });
    }
    // BUR-0049: separate from weaponGate! A world hop retires the drawing epoch,
    // never the logical Sessions login. All fields except sessionScope are client-thread-only.
    // IL XP port: explicitly armed, per-login experimental collection. OFF by default,
    // never armed by plugin startup, account switching, relogin, or a world hop.
    // This is not an approved release feed: RuneLite 1.13.4 does not expose an
    // authoritative account-bound XP-cache synchronization witness.
    private boolean sessionXpUserArmed;
    // Build 20: XP tracking INTENT (owner wants tracking this login) is separate from the
    // safety-gated ARM/baseline state above. Production startUp() sets the default ON; a
    // reflectively created legacy host fixture never runs startUp() and therefore keeps the
    // explicitly-disabled contract those regression tests assert. Intent survives transient
    // LOADING / teleport / hop pauses; it is cleared only by confirmed logout/account change.
    private boolean xpTrackingDefaultOn;
    private boolean xpIntent;
    private BurrowPanel.XpTrackingState publishedXpState;
    private final gg.feral.burrow.core.IronLedgerXpBaselines ironXpBaselines =
        new gg.feral.burrow.core.IronLedgerXpBaselines();
    private final RuneLiteXpObservationAdapter xpObserver = new RuneLiteXpObservationAdapter();
    private volatile SessionScope sessionScope;
    private final CopyOnWriteArrayList<BurrowPanel.SessionBossKcGain> kcPresentation = new CopyOnWriteArrayList<>();
    private RuneLiteBossKcAdapter kcAdapter;
    private boolean kcLoggedIn;
    // Iron Ledger live drop callback, now scoped to Burrow's current-login ledger.
    // Event identity avoids double-delivery; reset only on confirmed session retirement.
    private final Set<NpcLootReceived> seenLootEvents =
        Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<BurrowPanel.SessionDrop> lootPresentation = new ArrayList<>();
    // Iron Ledger quest discovery, 12 quest scripts per tick on RuneLite client thread.
    private final IronLedgerQuestSurvey questSurvey = new IronLedgerQuestSurvey();
    private int questSurveyIndex;
    private volatile long questSurveyAccount = -1L;
    private boolean questSurveyStarted;
    // R17 guide-based Pathfinder. Only positive, same-session bank observations are retained.
    private volatile long pathfinderPublicationRevision;
    private int pathfinderBankRefreshTick;
    private boolean pathfinderExpiryPublished;
    private int pathfinderLastPrepared=-1;
    private Set<Integer> pathfinderPositiveBankItems=Set.of();
    private SessionScope pathfinderBankScope;
    private long pathfinderBankAccount=-1L;
    private void clearPathfinderBankEvidence() {
        pathfinderPositiveBankItems=Set.of();pathfinderBankScope=null;pathfinderBankAccount=-1L;
    }
    /** Called on RuneLite client thread. Never claim full encounter readiness. */
    private void publishPathfinderSuggestions() {
        publishRareDrops();
        long hash=authenticatedAccountHash();
        SessionScope scope=sessionScope;
        BurrowPanel target=panel;
        long publication=++pathfinderPublicationRevision;
        if(!running.get() || target==null || hash==-1L || scope==null ||
           sessionPaused || hash!=sessionAccountHash) {
            if(target!=null)SwingUtilities.invokeLater(()-> {
                if(panel==target && pathfinderPublicationRevision==publication) {
                    target.setPathfinderSnapshot(null);
                    target.setExperienceHomeFeed(null);
                }
            });
            publishAccountMemory();
            return;
        }
        var observedSkills=new java.util.LinkedHashMap<String,Integer>();
        // Real RuneLite levels only. A failed/invalid reading is UNKNOWN, never 1.
        try {
            for(Skill skill : Skill.values()) {
                if(skill==Skill.OVERALL)continue;
                int level=client.getRealSkillLevel(skill);
                if(level>=1 && level<=99)observedSkills.put(skill.getName(),level);
            }
        }catch(RuntimeException unavailable) {observedSkills.clear();}
        var questStates=new java.util.LinkedHashMap<String,SuggestionEngine.Quest>();
        if(questSurveyAccount==hash && questSurveyStarted) {
            // Partial scans contain only positively observed states; others remain UNKNOWN.
            for(var entry:questSurvey.snapshot().quests().entrySet()) {
                try {questStates.put(entry.getKey(),SuggestionEngine.Quest.valueOf(entry.getValue()));}
                catch(IllegalArgumentException ignored) {/* unknown remains unknown */}
            }
        }
        // PQA-W04 isolated integration candidate: the existing accepted bank reader is
        // the ONLY source of stock counts. Never re-read a widget from Pathfinder.
        boolean trustedPositive=pathfinderBankScope==scope && pathfinderBankAccount==hash;
        if(trustedPositive && bankSnapshotScope==scope && bankSnapshotAccount==hash) {
            long before=bankPreloader.generation();
            if(bankPreloader.updateContext(currentAccountContext(hash,scope))!=before)refreshBankPlanOnPanel();
        }
        gg.feral.burrow.core.BankEvidence bankForPathfinder =
            bankSnapshotScope==scope && bankSnapshotAccount==hash && bankPreloader!=null
                ? bankPreloader.evidence() : null;
        var truth=gg.feral.burrow.pathfinder.PathfinderAccountTruth.capture(bankForPathfinder,
            new gg.feral.burrow.pathfinder.PathfinderAccountTruth.Context(
                hash,bankSnapshotAccount,scope,bankSnapshotScope,
                running.get() && !sessionPaused && hash==sessionAccountHash,
                bankWasOpen && AutomaticBankSnapshotReader.visible(client),
                bankNeedsSnapshot || bankOWithdrawalLeaseActive,
                pendingBankEventVerification,carriedDirty,
                acceptedBankOpenEpoch==bankOpenEpoch,System.currentTimeMillis()));
        var verifiedResources=truth.resources();
        if(truth.accepted())pathfinderExpiryPublished=false;
        AccountModePreference.Mode savedMode=accountModes().resolve(hash);
        java.util.Map<String,SuggestionEngine.GoalMark> goals;
        try {goals=pathfinderGoals().read(hash);} catch(RuntimeException unavailable) {goals=java.util.Map.of();}
        var snapshot=new SuggestionEngine.Snapshot(true,observedSkills,questStates,
            truth.accepted()?SuggestionEngine.BankStatus.VERIFIED_SAME_ACCOUNT:SuggestionEngine.BankStatus.UNAVAILABLE,
            truth.accepted()?verifiedResources.itemQuantities().keySet():Set.of(),
            savedMode==AccountModePreference.Mode.MAIN?SuggestionEngine.Mode.MAIN:SuggestionEngine.Mode.IRONMAN,
            truth.accepted()?bankPreloader.bestHeadlineByBoss():java.util.Map.of(),goals,verifiedResources);
        // PQA-W06 isolated historical capture. A baseline requires the completed
        // SAME-login quest survey and a full client skill vector, not a partial scan.
        // Saved bank memory is as-of-only and does NOT become live ownership input.
        // The accepted W04 bank and current-open-epoch witnesses are mandatory.
        if (configManager != null && sessionScope == scope && sessionAccountHash == hash &&
            !sessionPaused && authenticatedAccountHash() == hash) {
            try {
                var archive=pathfinderArchive();
                boolean questsComplete=questSurveyAccount==hash && questSurveyStarted &&
                    questSurvey.snapshot().complete();
                boolean fullSkills=observedSkills.size()==Skill.values().length-1;
                archive.saveInitialOnce(hash,System.currentTimeMillis(),snapshot,fullSkills,questsComplete);
                archive.rememberLatest(hash,System.currentTimeMillis(),snapshot,fullSkills,questsComplete);
                if (truth.accepted() && bankForPathfinder != null)
                    archive.rememberBank(hash,System.currentTimeMillis(),bankForPathfinder,
                        acceptedBankOpenEpoch==bankOpenEpoch &&
                        !bankOWithdrawalLeaseActive && !bankNeedsSnapshot &&
                        !pendingBankEventVerification && !carriedDirty);
            } catch (RuntimeException archiveUnavailable) {
                // ConfigManager write/parse error must not break the game or turn
                // an untrusted historical observation into a current recommendation.
            }
        }
        // W03: publish the SAME Pathfinder observations as a client-thread account snapshot.
        // Do not infer diary completion, spellbook, or ownership from an incomplete bank read.
        gg.feral.burrow.core.BankEvidence verifiedBank =
            bankSnapshotScope==scope && bankSnapshotAccount==hash && bankPreloader!=null
                ? bankPreloader.evidence() : null;
        // While a carried container is changing the previous bank/pack sum is not trustworthy.
        if (carriedDirty || bankNeedsSnapshot) verifiedBank = null;
        var safeExperienceSnapshot = (carriedDirty || bankNeedsSnapshot)
            ? new SuggestionEngine.Snapshot(true, observedSkills, questStates,
                SuggestionEngine.BankStatus.UNAVAILABLE, Set.of(),
                snapshot.mode(), java.util.Map.of(), goals) : snapshot;
        experienceBridge.observe(Long.toString(hash), scope.loginEpoch(), safeExperienceSnapshot, verifiedBank,
            new gg.feral.burrow.core.AccountContext(observedSkills,
                questStates.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                    java.util.Map.Entry::getKey, e -> e.getValue().name()))));
        var home=experienceHomeData();
        SwingUtilities.invokeLater(()-> {
            if(running.get() && panel==target && pathfinderPublicationRevision==publication &&
                sessionScope==scope && sessionAccountHash==hash && !sessionPaused) {
                target.setPathfinderSnapshot(snapshot);
                target.setExperienceHomeFeed(home.orElse(null));
            }
        });
        publishAccountMemory();
    }


    private volatile long kcGeneration;
    private RuneLiteBossKcAdapter newKcAdapter() {
        final long generation = ++kcGeneration;
        return new RuneLiteBossKcAdapter(new RuneLiteBossKcAdapter.SessionsSink() {
            public void onCurrentLoginReset() { if (generation == kcGeneration) { finishVerifiedSession(); kcPresentation.clear(); } }
            public void onBossKcGain(String boss, long delta, long total) {
                SessionScope scope = sessionScope;
                if (generation != kcGeneration || !running.get() || scope == null || sessionPaused || delta <= 0) return;
                BurrowPanel.SessionBossKcGain gain = new BurrowPanel.SessionBossKcGain(scope,
                    nextSessionEventId(), SessionOrigin.VERIFIED_BRIDGE, boss, delta,
                    "Observed RuneLite game-message KC delta");
                kcPresentation.add(gain);
                emitSessionEvent(gain);
            }
        });
    }
    private void reconcileKcLogin() {
        if (!running.get() || client.getGameState() != GameState.LOGGED_IN) return;
        if (kcAdapter == null) kcAdapter = newKcAdapter();
        if (!kcLoggedIn) {
            GameStateChanged observed = new GameStateChanged();
            observed.setGameState(client.getGameState());
            if (observed.getGameState() != GameState.LOGGED_IN) return;
            kcAdapter.onGameStateChanged(observed);
            kcLoggedIn = true;
        }
        long hash = authenticatedAccountHash();
        kcAdapter.onAuthenticatedLogin(hash);
        ensureVerifiedSession(hash);
    }
    /**
     * Safety fallback for a live client state that changes before its corresponding
     * GameStateChanged callback reaches this plugin. Shared with the normal event
     * path so a late state callback is idempotent, not a second session reset.
     * Called only from RuneLite's client thread.
     */
    private void reconcileKcUnavailableState(GameState state) {
        if (state == GameState.LOGGED_IN) return;
        boolean terminal = state == GameState.LOGIN_SCREEN ||
            state == GameState.LOGIN_SCREEN_AUTHENTICATOR ||
            state == GameState.STARTING;
        // BUR-0025 R03: a terminal state must ALWAYS reach the adapter, even when an
        // earlier transient (HOPPING/LOADING/CONNECTION_LOST) already cleared kcLoggedIn.
        // Otherwise the adapter keeps the old login open across a confirmed logout and a
        // same-account re-login continues the previous login's KC totals. Repeated
        // terminal delivery is idempotent in the adapter (retireLogin on no active login).
        terminalLogoutInProgress = terminal; // BQA-902 F13: only a login-screen state may complete a login
        try {
        if (kcAdapter != null && (kcLoggedIn || terminal)) {
            GameStateChanged observed = new GameStateChanged();
            observed.setGameState(state);
            kcAdapter.onGameStateChanged(observed);
        }
        kcLoggedIn = false;
        if (terminal) {
            finishVerifiedSession();
            kcPresentation.clear();
        } else {
            // HOPPING, LOADING, CONNECTION_LOST and any other nonterminal state
            // suspend, never retire, a same-login scope. A fresh KC baseline is
            // required when the authenticated account resumes LOGGED_IN.
            pauseVerifiedSession();
        }
        } finally { terminalLogoutInProgress = false; }
    }

    /** Exact Iron Ledger RuneLite NpcLootReceived source, no guessed bank withdrawals. */
    // Build 20.1: read-only Loot Tracker observation -> "loot-confirmed kills" ESTIMATE (never KC).
    private final gg.feral.burrow.core.LootEncounterLedger lootLedger = new gg.feral.burrow.core.LootEncounterLedger();
    @Subscribe public void onLootReceived(net.runelite.client.plugins.loottracker.LootReceived event) {
        if (!running.get() || event == null || client.getGameState() != GameState.LOGGED_IN) return;
        ensureVerifiedSession();
        SessionScope scope = sessionScope;
        if (scope == null || sessionPaused || sessionAccountHash != authenticatedAccountHash()) return;
        lootLedger.bind(scope);
        gg.feral.burrow.core.LootEncounterLedger.RecordType type;
        try { type = gg.feral.burrow.core.LootEncounterLedger.RecordType.valueOf(String.valueOf(event.getType())); }
        catch (IllegalArgumentException unknown) { type = gg.feral.burrow.core.LootEncounterLedger.RecordType.UNKNOWN; }
        List<int[]> stacks = new ArrayList<>();
        if (event.getItems() != null) for (ItemStack stack : event.getItems())
            if (stack != null) stacks.add(new int[]{stack.getId(), stack.getQuantity()});
        int tick;
        try { tick = client.getTickCount(); } catch (RuntimeException unavailable) { tick = -1; }
        var outcome = lootLedger.accept(scope, new gg.feral.burrow.core.LootEncounterLedger.Observation(
            event, event.getName(), type, event.getAmount(), tick, stacks));
        if (outcome != gg.feral.burrow.core.LootEncounterLedger.Outcome.COUNTED) return;
        String encounter = gg.feral.burrow.core.LootEncounterLedger.vettedEncounter(event.getName()).orElseThrow();
        emitSessionEvent(new BurrowPanel.SessionLootEncounter(scope, nextSessionEventId(), SessionOrigin.VERIFIED_BRIDGE,
            encounter, "RuneLite Loot Tracker LootReceived NPC record; one event = one encounter; estimate, not KC"));
    }
    @Subscribe public void onNpcLootReceived(NpcLootReceived event) {
        if (!running.get() || event==null || client.getGameState()!=GameState.LOGGED_IN) return;
        ensureVerifiedSession();
        SessionScope scope=sessionScope;
        if (scope==null || sessionPaused || sessionAccountHash!=authenticatedAccountHash()) return;
        String npc=event.getNpc()==null?null:event.getNpc().getName();
        // Boss/encounter loot ONLY. Normal NPCs, loot from unknown NPC names,
        // bank transfers and unproven chest rewards must not enter Sessions.
        if (IronLedgerLootIntake.recognizeEncounter(npc).isEmpty() || event.getItems()==null) return;
        // Per-current-login exact event identity protects against replaying the
        // same callback. A new login gets an independent ledger and dedupe set.
        if (!seenLootEvents.add(event)) return;
        List<IronLedgerLootIntake.ItemObserved> observed=new ArrayList<>();
        for (ItemStack stack:event.getItems()) {
            if (stack==null || stack.getId()<=0 || stack.getQuantity()<=0) continue;
            String name="Item #"+stack.getId();
            try {
                ItemComposition item=client.getItemDefinition(stack.getId());
                if(item!=null && item.getName()!=null && !item.getName().isBlank())
                    name=item.getName();
            } catch (RuntimeException unavailable) { /* observed ID remains valid */ }
            observed.add(new IronLedgerLootIntake.ItemObserved(stack.getId(),stack.getQuantity(),name));
        }
        for (IronLedgerLootIntake.Drop drop:IronLedgerLootIntake.accept(npc,observed)) {
            BurrowPanel.SessionDrop gain=new BurrowPanel.SessionDrop(scope,
                nextSessionEventId(),SessionOrigin.VERIFIED_BRIDGE,drop.itemId(),
                drop.itemName(),drop.amount(),drop.boss(),
                "RuneLite NpcLootReceived from exact matched boss NPC; not KC confirmation");
            lootPresentation.add(gain);
            emitSessionEvent(gain);
        }
    }

    @Subscribe public void onChatMessage(ChatMessage event) {
        if (!running.get()) return;
        GameState state = client.getGameState();
        if (state != GameState.LOGGED_IN) {
            reconcileKcUnavailableState(state);
            return;
        }
        reconcileKcLogin();
        if (kcAdapter != null && sessionScope != null && !sessionPaused) kcAdapter.onChatMessage(event);
        // R10: collection-log unlocks from the game's own message, only while the in-game chat notification is on.
        if (event != null && event.getType() == ChatMessageType.GAMEMESSAGE) {
            var item = gg.feral.burrow.core.VerifiedCounters.collectionItem(event.getMessage());
            SessionScope scope = sessionScope;
            if (item.isPresent() && scope != null && !sessionPaused && authenticatedAccountHash() == sessionAccountHash
                    && collectionChatEnabled() && liveLedger.scope() == scope) {
                liveLedger.trackCounter(gg.feral.burrow.core.VerifiedCounters.COLLECTION);
                if (liveLedger.recordCollectionUnlock(item.get(), System.currentTimeMillis())) publishRecentActivity(scope);
            }
        }
        // Build 20: the game's own absolute KC message replaces (never adds to) the CHAT source.
        if (event != null && (event.getType() == ChatMessageType.GAMEMESSAGE || event.getType() == ChatMessageType.SPAM)) {
            var kc = gg.feral.burrow.core.ChatKcParser.parse(event.getMessage());
            long hash = authenticatedAccountHash();
            if (kc.isPresent() && rareDropAccountOk(hash))
                rareDropWrite(hash, store -> store.recordKc(hash, kc.get().encounter(),
                    gg.feral.burrow.core.RareDropStore.Source.CHAT, kc.get().value(), System.currentTimeMillis()));
        }
    }
    // W03: account-bound source bridge for future Home/Chronicle UI. No UI is changed in this wave.
    private final gg.feral.burrow.experience.ExperienceSessionBridge experienceBridge =
        new gg.feral.burrow.experience.ExperienceSessionBridge();
    // After carried containers change, re-observe only after a coherent later tick.
    private boolean experienceEvidenceDirty;
    // W05: ephemeral per-login tip state. IDs only; never player names or bank items.
    private gg.feral.burrow.experience.SpoonsTipService.State homeTipState;
    private void closeCrewTip(gg.feral.burrow.experience.SpoonsTipService.State expected,
                              gg.feral.burrow.experience.SpoonsTipService.Closed how) {
        // Called from the RuneLite client thread. A late UI click from a previous login is inert.
        SessionScope scope=sessionScope;
        long hash=authenticatedAccountHash();
        var state=homeTipState;
        if(scope==null || sessionPaused || hash==-1L || state==null || expected==null ||
            !state.equals(expected) || !Long.toString(hash).equals(state.accountKey()) ||
            !scope.loginEpoch().equals(state.loginId()))return;
        homeTipState=gg.feral.burrow.experience.SpoonsTipService.close(state,how);
        publishPathfinderSuggestions();
    }
    /**
     * W03 backend-only read model for the future Home panel. Returns nothing during a hop,
     * unverified identity or account transition. No UI calls or player actions here.
     * Client-thread call only: it reads the existing session ledger and preloader caches.
     */
    java.util.Optional<gg.feral.burrow.experience.HomeFeed> experienceHomeData() {
        SessionScope scope = sessionScope;
        long hash = authenticatedAccountHash();
        if (!running.get() || scope == null || sessionPaused || hash == -1L || hash != sessionAccountHash)
            return java.util.Optional.empty();
        String key = Long.toString(hash);
        var current = experienceBridge.current(key,scope.loginEpoch());
        if (current.isEmpty()) return java.util.Optional.empty();
        var account = current.orElseThrow();
        // Existing WeeklyProgress stores MONDAY week-start keys, not today's LocalDate.
        var now = gg.feral.burrow.core.WeeklyProgress.weekStart(System.currentTimeMillis(), liveLedger.zone());
        var provider = gg.feral.burrow.experience.ScopedPreparedPlanSource.bind(bankPreloader, account);
        var feed = gg.feral.burrow.experience.HomeFeedProvider.buildScoped(account, sessionHistory,
            now, liveLedger.loginId(), liveLedger.weeklyContribution(now), homeTipState, null, provider);
        homeTipState=feed.spoonsState();
        // Late result cannot cross a login/generation or bank mutation.
        var fresh = experienceBridge.current(key, scope.loginEpoch());
        return fresh.isPresent() && fresh.get().generation() == account.generation()
            && sessionScope == scope && !sessionPaused ? java.util.Optional.of(feed) : java.util.Optional.empty();
    }
    /** Account-scoped personal Chronicle data. Unobserved task completion remains UNKNOWN. */
    java.util.Optional<gg.feral.burrow.experience.ChronicleDetail> experienceChronicleData(String boss) {
        SessionScope scope = sessionScope;
        long hash = authenticatedAccountHash();
        if (!rareDropAccountOk(hash) || scope == null) return java.util.Optional.empty();
        String key = Long.toString(hash);
        var current = experienceBridge.current(key,scope.loginEpoch());
        if (current.isEmpty()) return java.util.Optional.empty();
        gg.feral.burrow.experience.PersonalDropEvidence personal = null;
        try {
            var sourceState = rareDrops().read(hash);
            if (sourceState != null)
                personal = gg.feral.burrow.experience.PersonalDropEvidence.of(sourceState);
        }
        catch (RuntimeException unavailable) { /* no personal evidence is safer than stale evidence */ }
        var result = gg.feral.burrow.experience.ChronicleProvider.detail(boss,current.orElseThrow(),personal,
            gg.feral.burrow.experience.TaskCompletionEvidence.none());
        var fresh = experienceBridge.current(key,scope.loginEpoch());
        return fresh.isPresent() && fresh.get().generation() == current.orElseThrow().generation()
            && sessionScope == scope && !sessionPaused ? result : java.util.Optional.empty();
    }
    private volatile long sessionAccountHash = -1L; // ephemeral authenticated hash, never logged or exported
    private long sessionFence;
    private long sessionEventSequence;
    private boolean sessionPaused;
    private boolean xpSynchronized;
    // Readiness is established from the live client XP table and independent
    // base-level/overall consistency, not an assumed all-skill event flood.
    private final XpReadinessEvidence xpReadiness = new XpReadinessEvidence();
    // After switching accounts in the same plugin instance, never baseline from
    // a potentially old account's coherent cached XP table alone.
    private boolean previousSessionEstablished;
    private boolean requireFreshLoginStat;
    private boolean freshLoginStatObserved;
    // R05/R06: account identity never proves cached XP belongs to that account.
    // RuneLite 1.13.4 does not expose an account-bound completion witness for
    // skill-cache synchronization. Once this plugin observes a *different*
    // authenticated account, quarantine XP for every subsequent login in this
    // plugin instance. No XP-cache change or StatChanged event can release it.
    // A restart clears the in-memory quarantine, so R06 also requires a newly
    // witnessed login sequence; mid-session plugin enablement fails closed.
    private long priorSessionAccountHash = -1L;
    private boolean accountXpQuarantined;
    // R06: a plugin started while already LOGGED_IN has no evidence the XP
    // cache belongs to the current account. Only a fresh LOGIN_SCREEN ->
    // LOGGING_IN -> LOGGED_IN sequence witnessed by THIS plugin may enter the
    // existing cautious baseline path. This witness proves event order only,
    // not absolute cache ownership; real RuneLite validation is outstanding.
    private boolean xpSawLoginScreen;
    private boolean xpSawLoginAttempt;
    private boolean xpObservedFullLogin;

    // Shares the login epoch fence with weapon observation; neither persists account data.
    private static final Skill[] COMBAT_SKILLS = { Skill.ATTACK, Skill.STRENGTH,
        Skill.DEFENCE, Skill.HITPOINTS, Skill.RANGED, Skill.MAGIC, Skill.PRAYER };
    private final AtomicBoolean running = new AtomicBoolean();
    // Poll the single quest every five game ticks; it is a read-only quest query.
    private int questTickCountdown;
    private NavigationButton navigation;
    private volatile BurrowPanel panel;
    private PluginPanel hostPanel;
    private final NavigationLifecycle lifecycle = new NavigationLifecycle(
            SwingUtilities::invokeLater, this::registerButton, this::unregisterButton);

    @Override protected void startUp() {
        pendingKillLog = null; // never reuse a screen from the prior host lifecycle
        xpTrackingDefaultOn = true; // Build 20 owner decision: XP tracking defaults ON
        if(bankPreloader==null || bankPreloader.isClosed())
            bankPreloader=new BestBankPreloader(BestBankPreloader.knownProfiles());
        running.set(true);
        accountModeHash = -1L;
        ++accountModeRevision;
        weaponGate.start();
        // No fabricated login at startup. Complete coherent client XP snapshots
        // are required. A cross-account cache has no proven ownership witness.
        resetXpReadiness();
        xpSawLoginScreen = false;
        xpSawLoginAttempt = false;
        xpObservedFullLogin = false;
        // Recognize an initial login screen only on the client thread. Starting
        // after login, during LOGGING_IN, or while hopping is NOT proof.
        clientThread.invoke(() -> {
            if (running.get() && client.getGameState() == GameState.LOGIN_SCREEN)
                observeXpLoginTransition(GameState.LOGIN_SCREEN);
        });
        // The original clickable 'O' widget and its event handlers remain unchanged.
        // This separately removable image layer paints the four approved chest states over it.
        chestOverlay = new BankChestOverlay(client, () -> outfitterBank);
        overlayManager.add(chestOverlay);
        lifecycle.start();
        clientThread.invoke(() -> {
            if (!running.get()) return;
            outfitterBank = new OutfitterBank(client, pluginManager, () -> {
                BurrowModel current = activeModel;
                return current == null ? null : current.bankSelection();
            });
            // R07: the displayed category is the single source for gear, inventory and bank O.
            outfitterBank.setSetupSelection(() -> {
                BurrowPanel visible=panel;
                return visible==null?null:visible.selectedOutfitterSetupForHost();
            });
            outfitterBank.setSelectionEpoch(() -> {
                BurrowPanel visible=panel;
                return visible==null?0L:visible.outfitterSelectionEpochForHost();
            });
            outfitterBank.setBestBankOverride(this::currentBestBankLayout);
            outfitterBank.setVerifiedBankEvidence(this::verifiedLiveBankEvidence);
            outfitterBank.setBestBankVerifying(()->pendingBankEventVerification && bankWasOpen
                && bankSnapshotScope!=null && bankSnapshotScope==sessionScope
                && acceptedBankOpenEpoch==bankOpenEpoch
                && gg.feral.burrow.core.BankCaptureGate.accountAvailable(authenticatedAccountHash())
                && bankSnapshotAccount==authenticatedAccountHash());
            outfitterBank.setBestBankGeneration(()->bankPreloader.generation());
            outfitterBank.setBestBankRequested(() -> {
                BurrowPanel target=panel;
                return target!=null && target.bestBankToggleOnForHost();
            });
            outfitterBank.refresh();
        });
    }

    @Override protected void shutDown() {
        running.set(false);
        accountModeHash = -1L;
        ++accountModeRevision;
        bankPreloader.close();
        itemImages.clear();
        weaponGate.stop(); // invalidates pending client reads and Swing paints
        // stop is genuine: revoke scope and erase Session state, including old EDT work.
        SessionScope ending = sessionScope;
        // R08: keep this login's weekly observations across a plugin stop / client restart.
        if (ending != null && liveLedger.scope() == ending && sessionAccountHash != -1L && liveLedger.loginId() != null)
            sessionHistory.saveWeekly(Long.toString(sessionAccountHash), liveLedger.loginId(), liveLedger.weeklyContributions());
        sessionScope = null;
        experienceBridge.end();
        homeTipState=null;
        experienceEvidenceDirty = false;
        liveLedger.clear();          // BQA-902 F13: plugin stop is never a completed login
        hiscoreFence.revokeAll();
        sessionHistory.unbind();
        ++kcGeneration;
        RuneLiteBossKcAdapter endingKc = kcAdapter;
        kcAdapter = null;
        kcLoggedIn = false;
        kcPresentation.clear();
        lootLedger.bind(null);
        if (endingKc != null) clientThread.invoke(endingKc::stop);
        xpObserver.pluginStop();
        sessionXpUserArmed = false;
        xpIntent = false;
        xpTrackingDefaultOn = false;
        publishedXpState = null;
        ironXpBaselines.transition(null);
        sessionAccountHash = -1L;
        requireFreshLoginStat = false;
        freshLoginStatObserved = false;
        previousSessionEstablished = false;
        priorSessionAccountHash = -1L;
        accountXpQuarantined = false;
        xpSawLoginScreen = false;
        xpSawLoginAttempt = false;
        xpObservedFullLogin = false;
        resetXpReadiness();
        stopUiSession(ending);
        if(chestOverlay!=null) { overlayManager.remove(chestOverlay); chestOverlay=null; }
        lifecycle.stop();
        clientThread.invoke(() -> {
            OutfitterBank previous = outfitterBank;
            outfitterBank = null;
            if (previous != null) previous.stop();
        });
    }

    // RuneLite emits these events on the client thread. Never mutate bank widgets on Swing EDT.
    /** Step 2: re-reads the task only when its identifying values change, so kills on one task never re-plan. */
    private long slayerTaskKey=Long.MIN_VALUE;
    private void refreshSlayerTask() {
        // The task only affects bank plans, so nothing is read from the client until a bank snapshot exists.
        if(bankPreloader==null || bankPreloader.isClosed() || bankPreloader.evidence()==null)return;
        long before=bankPreloader.generation();
        gg.feral.burrow.bib.SlayerTask task;
        try {
            long key=SlayerTaskReader.changeKey(client);
            // After a logout or account change the preloader is back to UNKNOWN, so the same key must read again.
            if(key==slayerTaskKey && bankPreloader.slayerTask().state!=gg.feral.burrow.bib.SlayerTask.State.UNKNOWN)return;
            slayerTaskKey=key;
            task=SlayerTaskReader.read(client);
        } catch(RuntimeException transientRead) {
            slayerTaskKey=Long.MIN_VALUE;
            task=gg.feral.burrow.bib.SlayerTask.UNKNOWN;
        }
        if(bankPreloader.updateSlayerTask(task)!=before)refreshBankPlanOnPanel();
    }
    @Subscribe public void onGameTick(GameTick event) {
        if (!running.get()) return;
        OutfitterBank current = outfitterBank;
        // State reads run on RuneLite's client thread and only in the current login epoch.
        GameState liveState = client.getGameState();
        if (liveState != GameState.LOGGED_IN) {
            if (current != null) current.refresh();
            reconcileKcUnavailableState(liveState);
            return;
        }
        // R05: Bank Tags temporarily changes its rendered bank view to show the O layout.
        // That view is NOT a fresh bank-stock observation. Freeze the already verified
        // bank evidence while our current-account, current-epoch O tab stays selected.
        // A genuine withdrawal can change bank contents while the tab is open: never
        // re-claim the old snapshot after O closes; obtain a new verified read instead.
        boolean withdrawalLease=verifiedBestBankWithdrawalLease();
        if(withdrawalLease) {
            if(!bankOWithdrawalLeaseActive) {
                bankOWithdrawalLeaseActive=true;
                publishPathfinderSuggestions(); // O is frozen for withdrawal; not a live quantity witness
            }
        } else {
            if(bankOWithdrawalLeaseActive) {
                bankOWithdrawalLeaseActive=false;
                retireBankAfterMutation(AutomaticBankSnapshotReader.visible(client));
            }
            checkAutomaticBankOnTick();
        }
        if (current != null) current.refresh();
        if (!withdrawalLease && experienceEvidenceDirty && !carriedDirty && !bankNeedsSnapshot &&
            sessionScope != null && !sessionPaused && sessionAccountHash == authenticatedAccountHash()) {
            experienceEvidenceDirty = false;
            publishPathfinderSuggestions(); // repopulate only after a settled client tick
        }
        // A mid-withdrawal Slayer-task refresh could bump the planner generation and
        // retire O even though no bank stock was verified to have changed. Defer it.
        if(!withdrawalLease) refreshSlayerTask();
        flushClogCapture(); // R11
        // R08: persist weekly contributions about every 30 s (50 ticks); a crash loses at most that window.
        if (++weeklySaveTick >= 50) {
            weeklySaveTick = 0;
            saveAndPublishWeekly(sessionScope);
        }
        // BQA owner-native (Home "Waiting for equipment data..." while XP was live): the weapon was polled
        // once at LOGGED_IN and then only on an EQUIPMENT change event. If the container arrived later without
        // a change event (e.g. after a hop) the card stayed WAITING. Re-poll on ticks while still WAITING.
        long weaponEpoch = weaponGate.epoch();
        if (weaponGate.canObserve(weaponEpoch) && weaponGate.current().state() == LiveWeapon.State.WAITING) pollWeapon(weaponEpoch);
        // Build 19: as background Best Bank plans finish (one per 250 ms), refresh Pathfinder's
        // per-boss readiness at most every 10 ticks; cheap and only when the count changed.
        if(++pathfinderBankRefreshTick>=10) {
            pathfinderBankRefreshTick=0;
            // Expire already-rendered stock evidence even when no container event arrives.
            // This is display invalidation only, NOT a bank rescan or a widget action.
            var currentEvidence=bankPreloader==null?null:bankPreloader.evidence();
            if(!pathfinderExpiryPublished && currentEvidence!=null && bankWasOpen &&
                currentEvidence.observedAtMillis()>0 &&
                System.currentTimeMillis()-currentEvidence.observedAtMillis() >
                    gg.feral.burrow.pathfinder.PathfinderAccountTruth.MAX_OBSERVATION_AGE_MS) {
                pathfinderExpiryPublished=true;
                publishPathfinderSuggestions();
            }
            publishUpgrades(); // R09: evidence-gated Home upgrades, current generation only
            int prepared=bankPreloader.preparedCount();
            if(prepared!=pathfinderLastPrepared){pathfinderLastPrepared=prepared;publishPathfinderSuggestions();}
        }
        // Client-thread tick after RuneLite's stat callbacks; login sync must be
        // independently evidenced before a baseline may be initialized.
        reconcileKcLogin();
        refreshAccountModeIfChanged();
        autoArmXpFromIntent();
        trySynchronizeXp();
        publishXpTrackingState();
        pollPendingKillLog(); // W09: only read one witnessed Kill Log page, never bank/O
        pollQuestSurvey(weaponGate.epoch());
        if (--questTickCountdown <= 0) {
            questTickCountdown = 5;
            long epoch = weaponGate.epoch();
            if (weaponGate.canObserve(epoch)) pollQuest(epoch);
        }
    }

    @Subscribe public void onMenuEntryAdded(MenuEntryAdded event) {
        if (!running.get()) return;
        OutfitterBank current = outfitterBank;
        if (current != null) current.menuAdded(event);
    }

    @Subscribe public void onMenuOptionClicked(MenuOptionClicked event) {
        if (!running.get()) return;
        OutfitterBank current = outfitterBank;
        if (current != null) current.menuClicked(event);
    }

    @Subscribe public void onGameStateChanged(GameStateChanged event) {
        if (!running.get()) return;
        GameState observedState = event.getGameState();
        if (observedState != GameState.LOGGED_IN) pendingKillLog = null;
        if (observedState == GameState.LOGGED_IN) {
            if (kcAdapter == null) kcAdapter = newKcAdapter();
            kcAdapter.onGameStateChanged(event);
            kcLoggedIn = true;
        } else {
            // Bank scan is an as-of manual read; never carry it through hopping
            // or reconnecting even when the XP Sessions scope is suspended.
            // Build 20: LOADING (teleport / map chunk) cannot change the account, so the accepted
            // bank evidence, Pathfinder and account-mode views are kept; XP/KC still pause below.
            boolean chunkLoad = observedState == GameState.LOADING;
            clogCapture.reset(); // R11: an unfinished collection-log capture never crosses a hop / loading / logout
            if (!chunkLoad) clearObservedGearBank();
            reconcileKcUnavailableState(observedState);
            if (!chunkLoad) {
                publishPathfinderSuggestions(); // hide guide data while RuneLite is not logged in
                publishAccountMode(-1L);
            }
        }
        observeXpLoginTransition(observedState);
        publishXpTrackingState();
        // Any non-LOGGED_IN state (hopping, loading, reconnect, logout, etc.)
        // retires the previous epoch immediately without storing player identity.
        if (event.getGameState() == GameState.LOGGED_IN) {
            // Returning from HOPPING/LOADING with the same authenticated account
            // reuses the same immutable session. A real account change opens new.
            reconcileKcLogin();
            refreshAccountModeIfChanged();
            publishPathfinderSuggestions();
            long epoch = weaponGate.enterLoggedIn();
            showLive(epoch, LiveWeapon.waiting());
            showCombat(epoch, LiveCombat.waiting());
            showQuest(epoch, QuestRecommendation.waiting());
            // Give quest varps a few client ticks to settle after the login transition.
            questTickCountdown = 3;
            clientThread.invoke(() -> { pollWeapon(epoch); pollCombat(epoch); });
        } else {
            // The same KC state reconciliation runs for both observed state
            // events and fallback chat/tick reads. No second retirement here.
            OutfitterBank current = outfitterBank;
            if (current != null) current.clear();
            bankOWithdrawalLeaseActive=false;
            long epoch = weaponGate.leaveLoggedIn();
            questTickCountdown = 0;
            showLive(epoch, LiveWeapon.offline());
            showCombat(epoch, LiveCombat.offline());
            showQuest(epoch, QuestRecommendation.offline());
        }
    }

    /** One source-qualified invalidation path for BANK and carried mutations. */
    private void retireBankAfterMutation(boolean bankOpen) {
        pendingBankEventVerification=false;
        bankRevalidationWitness.reset();
        acceptedBankOpenEpoch=-1L;
        acceptedBankRead=null;
        bankPreloader.clear();bestBankLayout=null;bankSnapshotAccount=-1L;bankSnapshotScope=null;
        publishRecipeEvidence();
        clearPathfinderBankEvidence();publishPathfinderSuggestions();
        BurrowPanel target=panel;
        if(target!=null)SwingUtilities.invokeLater(()-> {
            if(panel==target)target.clearBankScan();
        });
        bankScan.invalidated(bankOpen);publishBankScanStatus();
        if(bankOpen) scheduleAutomaticBankObservation();
    }

    @Subscribe public void onItemContainerChanged(ItemContainerChanged event) {
        if (!running.get()) return;
        boolean bank=event.getContainerId()==InventoryID.BANK.getId();
        boolean inv=event.getContainerId()==InventoryID.INVENTORY.getId();
        boolean worn=event.getContainerId()==InventoryID.EQUIPMENT.getId();
        if(bank || inv || worn) {
            experienceBridge.invalidateEvidence();
            experienceEvidenceDirty=true;
            boolean open=AutomaticBankSnapshotReader.visible(client);
            if(verifiedBestBankWithdrawalLease()) {
                // Bank Tags redraws and real withdrawals both fire container events.
                // Do not discard the active withdrawal tab or reinterpret filtered
                // widgets as changed bank ownership. Scan again AFTER O is closed.
                bankOWithdrawalLeaseActive=true;
                publishPathfinderSuggestions(); // no scan: O withdrawal window is not a fresh quantity witness
            } else if(open) {
                if(acceptedBankRead!=null && bankWasOpen &&
                   (bankScan.phase()==gg.feral.burrow.core.BankScanTracker.Phase.ACCEPTED ||
                    bankScan.phase()==gg.feral.burrow.core.BankScanTracker.Phase.VERIFYING_CHANGE) &&
                   bankOpenEpoch==acceptedBankOpenEpoch && bankSnapshotScope==sessionScope && bankSnapshotAccount==authenticatedAccountHash()) {
                    // Defer one verified re-read until the next tick. Do not repeatedly clear
                    // the entire planner for duplicate Bank Tags/ItemContainerChanged events.
                    boolean firstEvent=!pendingBankEventVerification;
                    // Even when already verifying, another container event invalidates
                    // the matching-frame streak. Never accept an interleaved old read.
                    pendingBankEventVerification=true;
                    bankRevalidationWitness.reset();
                    bankScan.beginVerification();publishBankScanStatus();
                    if(firstEvent)publishPathfinderSuggestions(); // quarantine prior sidebar counts now
                    if(firstEvent) {
                        // Quarantine already-rendered owned suggestions immediately.
                        BurrowPanel target=panel;
                        if(target!=null)SwingUtilities.invokeLater(()->{
                            if(panel==target && pendingBankEventVerification)target.clearBankScan();
                        });
                    }
                } else if(!bankNeedsSnapshot) {
                    retireBankAfterMutation(true);
                }
                // If a scan is already pending, KEEP its stability samples. The next read
                // compares full contents and will naturally restart on changed data.
            } else if(bank) {
                retireBankAfterMutation(false);
            } else {
                // CP-1: bank closed; the next tick reconciles carried gear without
                // pretending the saved bank contents have changed.
                carriedDirty=true;
                publishPathfinderSuggestions();
            }
        }
        if (!worn) return;
        long epoch = weaponGate.epoch();
        if (weaponGate.canObserve(epoch)) clientThread.invoke(() -> pollWeapon(epoch));
    }

    @Subscribe public void onStatChanged(StatChanged event) {
        if (!running.get()) return;
        // Genuine RuneLite EventBus callback, not an XP-estimate from level changes.
        observeSessionXp(event);
        if(event!=null && event.getSkill()==Skill.SLAYER)publishPathfinderSuggestions();
        // Only the seven requested combat skills trigger a new combat snapshot.
        boolean relevant = false;
        for (Skill skill : COMBAT_SKILLS) if (skill == event.getSkill()) { relevant = true; break; }
        if (!relevant) return;
        long epoch = weaponGate.epoch();
        if (weaponGate.canObserve(epoch)) clientThread.invoke(() -> pollCombat(epoch));
    }

    /** Real base-level reads on RuneLite's client thread. No player names, XP history or network. */
    private void pollCombat(long epoch) {
        if (!running.get() || !weaponGate.canObserve(epoch) || client.getGameState() != GameState.LOGGED_IN)
            return;
        int[] levels = new int[COMBAT_SKILLS.length];
        try {
            for (int i=0;i<COMBAT_SKILLS.length;i++) levels[i]=client.getRealSkillLevel(COMBAT_SKILLS[i]);
        } catch (RuntimeException unavailable) {
            showCombat(epoch, LiveCombat.waiting());
            return;
        }
        showCombat(epoch, LiveCombat.fromRealLevels(levels));
    }

    /**
     * RuneLite 1.13.4 Quest#getState(Client) queries the quest-status script.
     * Never read outside LOGGED_IN, never assume a failed read means unfinished.
     * This does not take actions or change quest state in-game.
     */
    private void beginQuestSurvey(long accountHash) {
        questSurvey.reset(Quest.values().length);
        questSurveyIndex=0; questSurveyAccount=accountHash;
        questSurveyStarted=true;
        publishQuestSurvey(weaponGate.epoch());
    }
    private void resetQuestSurvey() {
        questSurveyStarted=false;questSurveyAccount=-1L;questSurveyIndex=0;
        questSurvey.reset(0);
        BurrowPanel destination=panel;
        if(destination!=null)SwingUtilities.invokeLater(()-> {
            if(panel==destination && questSurveyAccount==-1L)
                destination.setQuestSurvey(new IronLedgerQuestSurvey.Snapshot(java.util.Map.of(),false,0,0,0));
        });
    }
    private void pollQuestSurvey(long epoch) {
        if (!questSurveyStarted || questSurvey.scannedAll() ||
            sessionScope==null || sessionPaused || !running.get() ||
            client.getGameState()!=GameState.LOGGED_IN ||
            questSurveyAccount!=authenticatedAccountHash() || !weaponGate.canObserve(epoch)) return;
        Quest[] quests=Quest.values();
        // Same bounded 12-quest game-tick window as Iron Ledger.
        for(int i=0;i<12 && questSurveyIndex<quests.length;i++) {
            Quest q=quests[questSurveyIndex++];
            String status=null;
            try { QuestState state=q.getState(client);if(state!=null) status=state.name(); }
            catch (RuntimeException failed) { /* record unknown, never NOT_STARTED */ }
            questSurvey.observe(q.getName(),status);
        }
        if(questSurveyIndex==quests.length) { questSurvey.finish();publishQuestSurvey(epoch);publishPathfinderSuggestions(); }
    }
    private void publishQuestSurvey(long epoch) {
        IronLedgerQuestSurvey.Snapshot snapshot=questSurvey.snapshot();
        BurrowPanel destination=panel;
        SessionScope scope=sessionScope;
        long account=questSurveyAccount;
        SwingUtilities.invokeLater(() -> {
            if (!running.get() || !weaponGate.isCurrent(epoch) || panel!=destination || destination==null ||
                sessionScope!=scope || sessionAccountHash!=account || questSurveyAccount!=account) return;
            destination.setQuestSurvey(snapshot);
        });
    }

    private void pollQuest(long epoch) {
        if (!running.get() || !weaponGate.canObserve(epoch) || client.getGameState() != GameState.LOGGED_IN)
            return;
        QuestRecommendation result;
        try {
            QuestState state = Quest.COOKS_ASSISTANT.getState(client);
            if (state == null) {
                result = QuestRecommendation.unknown();
            } else {
                result = QuestRecommendation.of(switch (state) {
                    case FINISHED -> QuestRecommendation.State.FINISHED;
                    case IN_PROGRESS -> QuestRecommendation.State.IN_PROGRESS;
                    case NOT_STARTED -> QuestRecommendation.State.NOT_STARTED;
                });
            }
        } catch (RuntimeException unavailable) {
            result = QuestRecommendation.unknown();
        }
        showQuest(epoch, result);
    }

    /** Old-account quest paints are rejected on EDT by the same epoch fence as combat. */
    private void showQuest(long epoch, QuestRecommendation state) {
        SwingUtilities.invokeLater(() -> {
            if (!running.get() || !weaponGate.isCurrent(epoch)) return;
            BurrowPanel p = panel;
            if (p != null) p.setObservedQuest(state);
        });
    }

    private void showCombat(long epoch, LiveCombat state) {
        SwingUtilities.invokeLater(() -> {
            if (!running.get() || !weaponGate.isCurrent(epoch)) return;
            BurrowPanel p=panel;
            if (p!=null) p.setObservedCombat(state);
        });
    }

    /** Only called on RuneLite's client thread. No inventory, bank or player-name reads. */
    private void pollWeapon(long epoch) {
        if (!running.get() || !weaponGate.canObserve(epoch) || client.getGameState() != GameState.LOGGED_IN)
            return;
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        LiveWeapon observed;
        if (equipment == null) {
            observed = LiveWeapon.waiting(); // missing container is NOT an empty slot
        } else {
            Item item = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
            if (item == null || item.getId() <= 0) {
                observed = LiveWeapon.empty();
            } else {
                int id = item.getId();
                String name = "";
                try {
                    ItemComposition definition = client.getItemDefinition(id);
                    if (definition != null) name = definition.getName();
                } catch (RuntimeException missingDefinition) {
                    // Item ID is a real observation. Do not invent an item name.
                }
                observed = LiveWeapon.equipped(id, name);
            }
        }
        if (weaponGate.accept(epoch, observed)) showLive(epoch, observed);
    }

    /** A queued old-account paint cannot pass the epoch fence on the Swing thread. */
    private void showLive(long epoch, LiveWeapon state) {
        SwingUtilities.invokeLater(() -> {
            if (!running.get() || !weaponGate.isCurrent(epoch)) return;
            BurrowPanel p = panel;
            if (p != null) p.setObservedWeapon(state);
        });
    }


    /**
     * R06 trust boundary. Do not count a plugin restart as a fresh account
     * login. A game state alone or client XP table alone is not account-bound
     * synchronization evidence. A witnessed ordinary login is only permission
     * to attempt the strict coherent-XP baseline checks, not native proof.
     * Account switches still use the stricter permanent R05 quarantine.
     */
    private void observeXpLoginTransition(GameState state) {
        if (state == GameState.LOGIN_SCREEN) {
            xpSawLoginScreen = true;
            xpSawLoginAttempt = false;
            xpObservedFullLogin = false;
        } else if (state == GameState.LOGGING_IN) {
            if (xpSawLoginScreen) xpSawLoginAttempt = true;
        } else if (state == GameState.LOGGED_IN) {
            if (xpSawLoginScreen && xpSawLoginAttempt)
                xpObservedFullLogin = true;
            xpSawLoginScreen = false;
            xpSawLoginAttempt = false;
        } else if (state == GameState.LOGIN_SCREEN_AUTHENTICATOR) {
            // Authentication is not yet an accepted game login.
            xpSawLoginAttempt = false;
            xpObservedFullLogin = false;
        }
        // HOPPING, LOADING and temporary disconnects preserve an already
        // witnessed same-login session, never create a fresh witness.
    }

    // ----- BUR-0049: XP-only trusted session bridge (client-thread source) -----

    /**
     * CP-1 (BQA-R12-002): bank closed and pack/worn changed. Read BOTH containers in one client-thread pass; when
     * either is unavailable, or the account/login no longer matches the bank snapshot, fall back to STALE (never
     * a guessed or mixed-epoch CURRENT claim).
     */
    private void refreshCarriedFromClient() {
        if(bankPreloader==null || bankPreloader.evidence()==null)return;
        long before=bankPreloader.generation();
        long account=authenticatedAccountHash();
        java.util.Map<Integer,Integer> inv=liveContainer(InventoryID.INVENTORY);
        java.util.Map<Integer,Integer> eq=liveContainer(InventoryID.EQUIPMENT);
        long after;
        if(inv!=null && eq!=null && account!=-1L && account==bankSnapshotAccount && sessionScope!=null && sessionScope==bankSnapshotScope) {
            java.util.Map<Integer,String> names=new java.util.HashMap<>();
            for(var m:java.util.List.of(inv,eq)) for(int id:m.keySet()) {
                // Same definition lookup as AutomaticBankSnapshotReader (client thread, id-checked).
                try { ItemComposition def=client.getItemDefinition(id); if(def!=null && def.getId()==id && def.getName()!=null) names.put(id,def.getName()); }
                catch(RuntimeException | AbstractMethodError ignored) { /* name is optional evidence */ }
            }
            java.util.Set<Integer> carriedIds=new java.util.HashSet<>(inv.keySet()); carriedIds.addAll(eq.keySet());
            after=bankPreloader.refreshCarried(inv,eq,System.currentTimeMillis(),names,ownedEquipmentStats(carriedIds));
        } else after=bankPreloader.markStale();
        if(after!=before)refreshBankPlanOnPanel();
    }
    /**
     * CP-7: RuneLite's own equipment bonuses for observed items (client thread). Items without stats, or not
     * equipable, are simply absent; nothing is guessed. Used only to fill slots the Wiki plan leaves empty.
     */
    private java.util.Map<Integer,gg.feral.burrow.core.OwnedGearStats.Stats> ownedEquipmentStats(java.util.Collection<Integer> ids) {
        java.util.Map<Integer,gg.feral.burrow.core.OwnedGearStats.Stats> out=new java.util.HashMap<>();
        if(itemManager==null || ids==null)return out;
        for(int id:ids) {
            try {
                var st=itemManager.getItemStats(id);
                if(st==null || !st.isEquipable() || st.getEquipment()==null)continue;
                var e=st.getEquipment();
                out.put(id,new gg.feral.burrow.core.OwnedGearStats.Stats(e.getSlot(),e.isTwoHanded(),
                    e.getAstab(),e.getAslash(),e.getAcrush(),e.getAmagic(),e.getArange(),
                    e.getDstab(),e.getDslash(),e.getDcrush(),e.getDmagic(),e.getDrange(),
                    e.getStr(),e.getRstr(),e.getMdmg(),e.getPrayer()));
            } catch(RuntimeException | AbstractMethodError | NoSuchMethodError unavailable) { /* no stats: not a candidate */ }
        }
        return out;
    }
    /** CP-7: stats of every Wiki/tier gear item (the match TARGETS), read once from RuneLite and cached. */
    private volatile java.util.Map<Integer,gg.feral.burrow.core.OwnedGearStats.Stats> catalogGearStats;
    private java.util.Map<Integer,gg.feral.burrow.core.OwnedGearStats.Stats> catalogEquipmentStats() {
        var cached=catalogGearStats;
        if(cached!=null)return cached;
        java.util.Set<Integer> ids=new java.util.HashSet<>();
        for(var key:BestBankPreloader.knownProfiles()) {
            var sel=gg.feral.burrow.core.TieredOutfitterCatalog.find(key.boss(),key.method(),key.tier());
            if(sel==null)continue;
            for(var it:sel.profile().gear()) if(it!=null && it.id()>0) ids.add(it.id());
        }
        var out=java.util.Map.copyOf(ownedEquipmentStats(ids));
        if(!out.isEmpty())catalogGearStats=out;
        return out;
    }
    /** Positive item observations of one container, or null when the client has not provided it (unknown, not empty). */
    private java.util.Map<Integer,Integer> liveContainer(InventoryID id) {
        ItemContainer c=client.getItemContainer(id);
        if(c==null || c.getItems()==null)return null;
        java.util.Map<Integer,Integer> out=new java.util.HashMap<>();
        for(Item it:c.getItems()) if(it!=null && it.getId()>0 && it.getQuantity()>0) out.merge(it.getId(),it.getQuantity(),Math::addExact);
        return java.util.Map.copyOf(out);
    }

    /** Never use a name or the paint epoch for account authentication. */
    private long authenticatedAccountHash() {
        if (client.getGameState() != GameState.LOGGED_IN) return -1L;
        try {
            long hash = client.getAccountHash();
            return hash == -1L ? -1L : hash;
        } catch (RuntimeException unavailable) {
            return -1L;
        }
    }

    private void ensureVerifiedSession() {
        ensureVerifiedSession(authenticatedAccountHash());
    }

    private void ensureVerifiedSession(long hash) {
        if (!running.get()) return;
        if (hash == -1L) { finishVerifiedSession(); kcPresentation.clear(); return; } // fail closed
        if (sessionScope != null && sessionAccountHash != hash) finishVerifiedSession();
        if (sessionScope == null) {
            // Account identity is checked privately; exported tokens are random and opaque.
            SessionScope created = new SessionScope(UUID.randomUUID().toString(),
                                                     UUID.randomUUID().toString());
            sessionAccountHash = hash;
            sessionLoginFence = Math.max(1L, System.currentTimeMillis());
            sessionScope = created;
            experienceBridge.begin(Long.toString(hash),created.loginEpoch());
            liveLedger.start(created, SessionOrigin.VERIFIED_BRIDGE, System.currentTimeMillis());
            sessionHistory.bind(Long.toString(hash)); // opaque in-memory key; file name is a salted pseudonym
            refreshAccountModeIfChanged();
            lootPresentation.clear();seenLootEvents.clear();
            beginQuestSurvey(hash);
            publishPathfinderSuggestions();
            // A new authenticated account does not attest XP-cache ownership.
            // Latch across B->A and later logins; never allow an XP value or
            // callback from an old account to unlock a new account's feed.
            if (previousSessionEstablished && priorSessionAccountHash != hash) {
                accountXpQuarantined = true;
            }
            // A full witnessed login is stronger than a lone StatChanged and
            // prevents R02's repeated-login event-flood dead end. If no full
            // login was witnessed, trySynchronizeXp stays unavailable.
            requireFreshLoginStat = previousSessionEstablished && !accountXpQuarantined && !xpObservedFullLogin;
            previousSessionEstablished = true;
            freshLoginStatObserved = false;
            sessionPaused = false;
            sessionEventSequence = 0;
            resetXpReadiness();
            sessionXpUserArmed = false; // never carry opt-in into a different login
            xpIntent = xpTrackingDefaultOn; // Build 20: default ON for each new verified login (still gated)
            ironXpBaselines.transition(null);
            sessionFence = xpObserver.beginVerifiedLogin(created);
            publishSessionToPanel(created, panel);
        } else if (sessionPaused) {
            experienceBridge.resume(Long.toString(hash),sessionScope.loginEpoch());
            sessionPaused = false;
            sessionFence = xpObserver.resumeVerifiedSameLogin(sessionScope);
            resetXpReadiness();
            emitSessionEvent(new BurrowPanel.SessionResume(sessionScope, nextSessionEventId(),
                                                           SessionOrigin.VERIFIED_BRIDGE));
            emitXpAvailability(FeedStatus.UNAVAILABLE);
        }
    }

    /**
     * Build 20: re-arm from same-login intent (default ON) through the SAME gated path as the
     * explicit button. requestExperimentalXpForCurrentLogin() still refuses unless this plugin
     * witnessed a full login, the account is not quarantined, the session is not paused and the
     * authenticated hash matches. A fresh baseline (two stable coherent XP frames) is required
     * again after every re-arm, so XP earned during a pause is never counted. Client thread.
     */
    private void autoArmXpFromIntent() {
        if (!xpIntent || sessionXpUserArmed) return;
        requestExperimentalXpForCurrentLogin();
    }
    /** Pure classification for the Sessions card; no account data leaves the client thread. */
    BurrowPanel.XpTrackingState xpTrackingState() {
        if (sessionScope == null) return BurrowPanel.XpTrackingState.SIGNED_OUT;
        if (sessionPaused) return BurrowPanel.XpTrackingState.PAUSED;
        if (!xpIntent) return BurrowPanel.XpTrackingState.OFF;
        if (accountXpQuarantined || !xpObservedFullLogin) return BurrowPanel.XpTrackingState.UNAVAILABLE;
        if (sessionXpUserArmed && xpSynchronized) return BurrowPanel.XpTrackingState.ACTIVE;
        return BurrowPanel.XpTrackingState.SYNCING;
    }
    private void publishXpTrackingState() {
        BurrowPanel.XpTrackingState state = xpTrackingState();
        SessionScope scope = sessionScope;
        BurrowPanel destination = panel;
        if (state == publishedXpState || destination == null || !running.get()) return;
        publishedXpState = state;
        SwingUtilities.invokeLater(() -> {
            if (running.get() && panel == destination && sessionScope == scope) destination.setXpTrackingState(state);
        });
    }

    /** Explicit user action in Sessions; client-thread-only and fail-closed.
     *  The owner must log in AFTER starting the plugin. Never silently trust an
     *  already-logged-in cache or rearm after hopping / account switching.
     */
    private void requestExperimentalXpForCurrentLogin() {
        if (!running.get() || sessionXpUserArmed || accountXpQuarantined ||
            !xpObservedFullLogin || sessionScope == null || sessionPaused ||
            client.getGameState() != GameState.LOGGED_IN ||
            authenticatedAccountHash() != sessionAccountHash) return;
        sessionXpUserArmed = true;
        xpIntent = true; // an explicit click also records same-login intent
        resetXpReadiness();
        ironXpBaselines.transition(null);
        emitXpAvailability(FeedStatus.UNAVAILABLE); // pending verified baseline
    }

    private void resetXpReadiness() {
        xpSynchronized = false;
        xpReadiness.clear();
    }

    // ---- BQA-902 Batch B: current-login truth outside Swing + durable last-10 completed logins ----
    private final gg.feral.burrow.ui.LiveSessionLedger liveLedger = new gg.feral.burrow.ui.LiveSessionLedger();
    private final gg.feral.burrow.core.SessionHistoryService sessionHistory =
        new gg.feral.burrow.core.SessionHistoryService(historyDirectory());
    /** Set only while a TERMINAL game state (login screen) is being reconciled; never by hop, -1 or stop. */
    private boolean terminalLogoutInProgress;
    private static java.nio.file.Path historyDirectory() {
        try {
            // R05: tests (and only tests) redirect history away from the player's real RuneLite folder.
            String override = System.getProperty("trashpanda.historyDir");
            if (override != null && !override.isBlank()) return java.nio.file.Paths.get(override);
            return net.runelite.client.RuneLite.RUNELITE_DIR.toPath().resolve("trashpanda-tracker").resolve("completed-sessions");
        } catch (RuntimeException | LinkageError unavailable) { return null; }
    }
    /**
     * R10: RuneLite's Screenshot plugin reads VarbitID.OPTION_COLLECTION_NEW_ITEM == 1 before trusting the chat message.
     * The constant is resolved reflectively so a RuneLite build without it leaves collection unlocks "not tracked"
     * instead of failing to compile or guessing a varbit number. Client thread.
     */
    private Integer collectionVarbit; private boolean collectionVarbitResolved;
    private boolean collectionChatEnabled() {
        if (!collectionVarbitResolved) {
            collectionVarbitResolved = true;
            try { collectionVarbit = Class.forName("net.runelite.api.gameval.VarbitID").getField("OPTION_COLLECTION_NEW_ITEM").getInt(null); }
            catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { collectionVarbit = null; }
        }
        if (collectionVarbit == null) return false;
        try { return client.getVarbitValue(collectionVarbit) == 1; } catch (RuntimeException unavailable) { return false; }
    }
    /** R08: save this login's weekly contributions (replace, idempotent) and refresh the Home weekly card. */
    private int weeklySaveTick;
    private void saveAndPublishWeekly(SessionScope scope) {
        long hash = sessionAccountHash;
        String login = liveLedger.loginId();
        if (scope != null && hash != -1L && login != null && liveLedger.scope() == scope)
            sessionHistory.saveWeekly(Long.toString(hash), login, liveLedger.weeklyContributions());
        publishWeekly(scope);
    }
    private void publishWeekly(SessionScope scope) {
        BurrowPanel destination = panel;
        if (destination == null) return;
        java.time.LocalDate week = gg.feral.burrow.core.WeeklyProgress.weekStart(System.currentTimeMillis(), liveLedger.zone());
        var totals = scope == null || liveLedger.scope() != scope ? java.util.Optional.<gg.feral.burrow.core.WeeklyProgress.Totals>empty()
            : sessionHistory.weeklyTotals(week, liveLedger.loginId(), liveLedger.weeklyContribution(week));
        String note = sessionHistory.weeklyNote();
        SwingUtilities.invokeLater(() -> {
            if (panel == destination && sessionScope == scope) destination.setWeeklyProgress(totals.orElse(null), note);
        });
    }
    /** R09: Home upgrades from the preloader's CURRENT-generation plans; NONE/STALE evidence is passed through as such. */
    private Object publishedUpgradeSignature;
    private void publishUpgrades() {
        BurrowPanel destination = panel;
        SessionScope scope = sessionScope;
        if (destination == null) return;
        var evidence = scope == null || bankSnapshotScope != scope ? BestBankPreloader.UpgradeEvidence.NONE : bankPreloader.upgradeEvidence();
        var list = evidence == BestBankPreloader.UpgradeEvidence.CURRENT ? bankPreloader.upgradeOpportunities(6) : java.util.List.<BestBankPreloader.Upgrade>of();
        int prepared = bankPreloader.preparedCount(), total = bankPreloader.profileCount();
        Object signature = java.util.List.of(evidence, list, bankPreloader.generation(), prepared);
        if (signature.equals(publishedUpgradeSignature)) return;
        publishedUpgradeSignature = signature;
        SwingUtilities.invokeLater(() -> { if (panel == destination && sessionScope == scope) destination.setUpgrades(evidence, list, prepared, total); });
    }
    /** R07: Home "recent activity" comes from the same ledger as Sessions; scope-fenced on the EDT. */
    private void publishRecentActivity(SessionScope scope) {
        BurrowPanel destination = panel;
        if (destination == null) return;
        var list = scope == null ? java.util.List.<gg.feral.burrow.ui.LiveSessionLedger.Activity>of() : liveLedger.recent();
        SwingUtilities.invokeLater(() -> {
            if (panel == destination && sessionScope == scope) destination.setRecentActivity(list);
        });
    }
    private void publishHistoryToPanel(BurrowPanel destination, SessionScope scope) {
        if (destination == null) return;
        var list = scope == null ? java.util.List.<gg.feral.burrow.core.CompletedSessionHistory.Summary>of() : sessionHistory.sessions();
        String note = scope == null ? "Sign in to load this account's history" : sessionHistory.note();
        SwingUtilities.invokeLater(() -> {
            if (panel == destination && sessionScope == scope) destination.setCompletedSessions(list, note);
        });
    }

    private synchronized String nextSessionEventId() {
        return "verified-xp-" + (++sessionEventSequence);
    }

    private void emitSessionEvent(SessionEvent event) {
        // Defense-in-depth against accidental future bypass of either entry gate.
        if (!sessionXpUserArmed && event instanceof BurrowPanel.SessionXpGain) return;
        // BUR-0049: a directly constructed XP AVAILABLE status must not escape either
        // while the feed is disabled (previously only emitXpAvailability coerced it).
        if (!sessionXpUserArmed && event instanceof BurrowPanel.SessionFeedStatus status
                && status.feed() == SessionFeed.XP && status.status() == FeedStatus.AVAILABLE) return;
        SessionScope expected = event.scope();
        // BQA-902 F04: record what was actually emitted, even when no panel exists yet.
        if (expected == sessionScope && liveLedger.record(event)) publishRecentActivity(expected);
        BurrowPanel destination = panel;
        if (destination == null || !running.get()) return;
        // Queued old-account/offline events cannot arrive in a new UI/login.
        SwingUtilities.invokeLater(() -> {
            if (!running.get() || sessionScope != expected || panel != destination) return;
            destination.ingestSessionEvent(event);
        });
    }

    private void publishSessionToPanel(SessionScope scope, BurrowPanel destination) {
        if (scope == null || destination == null || !running.get()) return;
        // BQA-902 F04/F12: rebuild the new panel from the ledger (XP, KC, estimates, drops, feeds, pause state).
        // Replay events carry fresh ids and the panel is new, so nothing is double-counted; the ledger is not mutated.
        if (liveLedger.scope() != scope) liveLedger.start(scope, SessionOrigin.VERIFIED_BRIDGE, System.currentTimeMillis());
        if (!liveLedger.hasFeed(SessionFeed.XP)) {
            liveLedger.record(new BurrowPanel.SessionFeedStatus(scope, nextSessionEventId(), SessionOrigin.VERIFIED_BRIDGE,
                SessionFeed.XP, sessionXpUserArmed && xpSynchronized ? FeedStatus.AVAILABLE : FeedStatus.UNAVAILABLE));
        }
        java.util.List<SessionEvent> replay = liveLedger.replay(this::nextSessionEventId);
        SwingUtilities.invokeLater(() -> {
            if (!running.get() || sessionScope != scope || panel != destination) return;
            destination.restoreSessionSnapshot(replay); // full snapshot: idempotent per panel (R05)
        });
        publishHistoryToPanel(destination, scope);
        publishRecentActivity(scope);
        publishWeekly(scope);
        publishedUpgradeSignature = null; // new panel: always republish
        publishUpgrades();
    }

    private void emitXpAvailability(FeedStatus status) {
        if (sessionScope == null || sessionPaused) return;
        // Defense-in-depth: never claim observed XP when the host feed is off.
        if (!sessionXpUserArmed) status = FeedStatus.UNAVAILABLE;
        emitSessionEvent(new BurrowPanel.SessionFeedStatus(sessionScope, nextSessionEventId(),
                SessionOrigin.VERIFIED_BRIDGE, SessionFeed.XP, status));
    }

    private void stopUiSession(SessionScope old) {
        if (old == null) return;
        BurrowPanel destination = panel;
        if (destination == null) return;
        String stopId = "verified-stop-" + UUID.randomUUID();
        SwingUtilities.invokeLater(() -> {
            if (panel == destination) destination.ingestSessionEvent(
                    new BurrowPanel.SessionStop(old, stopId, SessionOrigin.VERIFIED_BRIDGE));
        });
    }

    private void finishVerifiedSession() {
        SessionScope old = sessionScope;
        if (old == null) { experienceBridge.end(); return; }
        experienceBridge.end();
        experienceEvidenceDirty = false;
        // BQA-902 SESS-04: exactly one completed card per CONFIRMED logout. Account switch, identity -1,
        // KC-adapter resets outside a terminal state and plugin stop never create a completed login.
        if (terminalLogoutInProgress && liveLedger.scope() == old && sessionAccountHash != -1L) {
            try {
                sessionHistory.commit(Long.toString(sessionAccountHash),
                    liveLedger.completedSummary(System.currentTimeMillis(), gg.feral.burrow.core.CompletedSessionHistory.CONFIRMED_LOGOUT));
            } catch (RuntimeException invalid) { /* never block logout; history stays unchanged */ }
        }
        // R08: every observed event counts toward its week, whatever ended the login (logout, switch, identity loss).
        if (liveLedger.scope() == old && sessionAccountHash != -1L && liveLedger.loginId() != null)
            sessionHistory.saveWeekly(Long.toString(sessionAccountHash), liveLedger.loginId(), liveLedger.weeklyContributions());
        liveLedger.clear();
        sessionHistory.unbind();
        hiscoreFence.revokeAll();
        BurrowPanel historyTarget = panel;
        if (historyTarget != null) SwingUtilities.invokeLater(() -> {
            if (panel == historyTarget) { historyTarget.setCompletedSessions(java.util.List.of(), "Sign in to load this account's history");
                historyTarget.setRecentActivity(java.util.List.of());
                historyTarget.setWeeklyProgress(null, "Sign in to load this account's weekly progress");
                historyTarget.setExperienceHomeFeed(null);
                historyTarget.setUpgrades(BestBankPreloader.UpgradeEvidence.NONE, java.util.List.of()); }
        });
        clearObservedGearBank();
        xpObserver.confirmedLogout(old);
        sessionXpUserArmed = false;
        xpIntent = false; // confirmed logout retires intent with the login
        ironXpBaselines.transition(null);
        lootPresentation.clear();seenLootEvents.clear();
        lootLedger.bind(null);
        resetQuestSurvey();
        priorSessionAccountHash = sessionAccountHash;
        sessionScope = null; // invalidate all queued XP deliveries before posting stop
        sessionAccountHash = -1L;
        bankScan.unbind(); // BQA-901 T11: no last-good provenance survives into another login
        publishedBankScanStatus = null;
        publishedUpgradeSignature = null;
        BurrowPanel bankTarget = panel;
        if (bankTarget != null) SwingUtilities.invokeLater(() -> { if (panel == bankTarget) bankTarget.showBankCaptureState(null, null); });
        publishPathfinderSuggestions(); // clear stale suggestions at confirmed logout
        publishAccountMode(-1L);
        sessionFence = 0;
        sessionPaused = false;
        requireFreshLoginStat = false;
        freshLoginStatObserved = false;
        resetXpReadiness();
        stopUiSession(old);
    }

    private void pauseVerifiedSession() {
        SessionScope current = sessionScope;
        if (current == null || sessionPaused) return;
        sessionPaused = true;
        experienceBridge.pause();
        BurrowPanel crewTarget=panel;
        if(crewTarget!=null)SwingUtilities.invokeLater(() -> {
            if(panel==crewTarget)crewTarget.setExperienceHomeFeed(null);
        });
        // Fail closed DURING the pause (no events, no baseline). Same-login intent is kept, so
        // autoArmXpFromIntent() re-arms after LOGGED_IN with a brand-new baseline (Build 20).
        sessionXpUserArmed = false;
        ironXpBaselines.transition(null);
        sessionFence = xpObserver.pauseTransient(current);
        resetXpReadiness();
        emitSessionEvent(new BurrowPanel.SessionPause(current, nextSessionEventId(),
                                                       SessionOrigin.VERIFIED_BRIDGE));
    }

    private void observeSessionXp(StatChanged event) {
        // StatChanged may reflect another account's stale synchronized cache.
        // Keep the adapter for future review; emit no XP in this release candidate.
        if (!sessionXpUserArmed) return;
        if (!running.get() || event == null || client.getGameState() != GameState.LOGGED_IN) return;
        ensureVerifiedSession();
        SessionScope scope = sessionScope;
        if (scope == null || sessionPaused || accountXpQuarantined ||
            !xpObservedFullLogin || authenticatedAccountHash() != sessionAccountHash) return;
        Skill skill = event.getSkill();
        if (skill == null || skill == Skill.OVERALL) return;
        int reportedXp = event.getXp();
        if (reportedXp < 0) return;
        try {
            if (client.getSkillExperience(skill) != reportedXp) return;
        } catch (RuntimeException unavailable) {
            return;
        }
        if (!xpSynchronized) {
            // A same-account return may need an initial stat event. A changed
            // account cannot reach this branch: it is quarantined above.
            freshLoginStatObserved = true;
            // StatChanged can be a login-sync event OR a real first gain. Do not
            // guess which; the client-thread snapshot is seeded only when its
            // independent consistency witnesses are ready. No pre-seed gains.
            return;
        }
        // Require agreement with Iron Ledger's independently seeded, no-regression
        // baseline. A stale/lower callback must never ratchet the source backward.
        int ironDelta = ironXpBaselines.observe(scope.loginEpoch(), skill.getName(), reportedXp);
        xpObserver.onStatChanged(scope, sessionFence, event, client, true).ifPresent(delta -> {
            if (ironDelta <= 0 || ironDelta != delta.delta()) return;
            emitSessionEvent(new BurrowPanel.SessionXpGain(scope, nextSessionEventId(),
                    SessionOrigin.VERIFIED_BRIDGE, delta.skill().getName(),
                    delta.delta(), "Iron Ledger baseline + " + delta.sourceEvidence()));
            // R10: real level-ups derived from the SAME accepted XP (standard XP table), never from StatChanged.level.
            int levels = gg.feral.burrow.core.VerifiedCounters.levelsGained((long) reportedXp - delta.delta(), reportedXp);
            if (levels > 0 && liveLedger.scope() == scope && liveLedger.recordLevelUp(delta.skill().getName(), levels,
                    gg.feral.burrow.core.VerifiedCounters.levelForXp(reportedXp), System.currentTimeMillis()))
                publishRecentActivity(scope);
        });
    }

    /**
     * RuneLite's XP Tracker initializes cached skill XP on the client-thread
     * GameTick after login (it uses a two-tick synchronization guard). We add
     * independent witnesses so the mere passage of ticks is NEVER enough:
     * - verified account identity and LOGGED_IN
     * - complete nonnegative XP vector including genuine level-1 zero XP
     * - total equals RuneLite's overall XP
     * - real base skill levels agree with the XP table
     * - default character's minimum HP XP has loaded
     * - two identical, internally coherent game-tick snapshots for this account
     * This supports a witnessed fresh login without a full StatChanged flood.
     * Mid-session enablement remains UNAVAILABLE because no account-bound XP
     * sync witness has been established. Do not market it as live-ready.
     *
     * Important: these are client-cache synchronization witnesses, not a
     * native proof of event ordering. If any witness fails: UNAVAILABLE.
     */
    private void trySynchronizeXp() {
        // A witnessed login and two stable frames do NOT prove cache ownership.
        // Never admit a baseline until authoritative provenance is approved.
        if (!sessionXpUserArmed) return;
        SessionScope scope = sessionScope;
        if (scope == null || sessionPaused || xpSynchronized || accountXpQuarantined ||
            !xpObservedFullLogin || client.getGameState() != GameState.LOGGED_IN ||
            authenticatedAccountHash() != sessionAccountHash ||
            (requireFreshLoginStat && !freshLoginStatObserved)) return;
        final Skill[] skills = Skill.values();
        final int[] xp = new int[skills.length]; // All 24 real skills; Skill.OVERALL is a null compatibility field
        int count = 0;
        int hpIndex = -1;
        long overall;
        try {
            for (Skill skill : skills) {
                if (skill == Skill.OVERALL) continue;
                int observedXp = client.getSkillExperience(skill);
                int realLevel = client.getRealSkillLevel(skill);
                // All skills are real 1..99 levels, even when XP is capped at 200m.
                // Boosted level changes are deliberately NOT an XP witness.
                if (observedXp < 0 || observedXp > Experience.MAX_SKILL_XP ||
                    realLevel < 1 || realLevel > Experience.MAX_REAL_LEVEL ||
                    realLevel != Math.min(Experience.MAX_REAL_LEVEL,
                                          Experience.getLevelForXp(observedXp))) {
                    xpReadiness.clear();
                    return;
                }
                if (skill == Skill.HITPOINTS) hpIndex = count;
                xp[count++] = observedXp;
            }
            overall = client.getOverallExperience();
        } catch (RuntimeException unavailable) {
            xpReadiness.clear();
            return;
        }
        if (count != xp.length || hpIndex < 0) return;
        if (!xpReadiness.observe(sessionAccountHash, xp, overall, hpIndex)) return;

        // Guard against a change between validation and seeding. The adapter
        // reads all skills again, refusing negative/unknown values. Check that
        // the proven frame is still exact before accepting its baselines.
        if (authenticatedAccountHash() != sessionAccountHash ||
            client.getGameState() != GameState.LOGGED_IN) {
            xpReadiness.clear();
            return;
        }
        if (xpObserver.seedVerifiedClientSnapshot(scope, sessionFence, client, true)) {
            // Reject a changed cache across seed operations instead of
            // accidentally treating a login flood as earned XP.
            if (!isSameXpSnapshot(skills, xp, overall)) {
                xpObserver.pauseTransient(scope);
                sessionFence = xpObserver.resumeVerifiedSameLogin(scope);
                xpReadiness.clear();
                return;
            }
            ironXpBaselines.transition(scope.loginEpoch());
            int i = 0;
            for (Skill skill : skills) {
                if (skill != Skill.OVERALL) ironXpBaselines.seed(scope.loginEpoch(), skill.getName(), xp[i++]);
            }
            xpSynchronized = true;
            emitXpAvailability(FeedStatus.AVAILABLE);
        } else {
            xpReadiness.clear();
        }
    }


    private boolean isSameXpSnapshot(Skill[] skills, int[] acceptedXp, long acceptedOverall) {
        try {
            if (client.getOverallExperience() != acceptedOverall) return false;
            int cursor = 0;
            for (Skill skill : skills) {
                if (skill != Skill.OVERALL &&
                    (cursor >= acceptedXp.length ||
                     client.getSkillExperience(skill) != acceptedXp[cursor++])) return false;
            }
            return cursor == acceptedXp.length;
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    /**
     * Pure, testable client-cache evidence gate. No identity, XP or session data
     * leave this process. Two STABLE VALID snapshots are required, not a timer.
     * It is intentionally reset on account change, bad data and world hops.
     */
    static final class XpReadinessEvidence {
        private long account = -1L;
        private long overall = -1L;
        private int[] previous;
        private int matchingFrames;

        boolean observe(long verifiedAccountHash, int[] skillXp, long totalXp,
                        int hpIndex) {
            if (verifiedAccountHash == -1L || skillXp == null || skillXp.length == 0 ||
                hpIndex < 0 || hpIndex >= skillXp.length || skillXp[hpIndex] < 1154 ||
                totalXp < 1154) {
                clear();
                return false;
            }
            long sum = 0;
            for (int current : skillXp) {
                if (current < 0 || current > 200_000_000) {
                    clear();
                    return false;
                }
                sum += current;
            }
            if (sum != totalXp) {
                clear();
                return false;
            }
            if (account != verifiedAccountHash || overall != totalXp ||
                !Arrays.equals(previous, skillXp)) {
                previous = skillXp.clone(); // defensive: caller's arrays may be reused
                account = verifiedAccountHash;
                overall = totalXp;
                matchingFrames = 1;
                return false;
            }
            if (matchingFrames < 2) matchingFrames++;
            return matchingFrames >= 2;
        }

        void clear() {
            account = -1L;
            overall = -1L;
            previous = null;
            matchingFrames = 0;
        }
    }

    private void registerButton() {
        if (navigation != null) return;
        BurrowModel nextModel = new BurrowModel();
        BurrowPanel nextPanel = new BurrowPanel(nextModel, id -> itemImages.computeIfAbsent(id, key -> {
            var image=itemManager.getImage(key);
            if(image!=null) image.onLoaded(() -> {
                BurrowPanel current=panel;
                if(current!=null) SwingUtilities.invokeLater(current::repaint);
            });
            return image;
        }));
        // Authentic RuneLite skill art, no image CDN or hardcoded foreign item assets.
        // Quest, clue and generic categories keep compact fallback pictograms.
        nextPanel.setActivityIconImages(category -> {
            if(skillIconManager==null)return null;
            String skillName=switch(category){
                case MELEE,COMBAT -> "ATTACK";
                case MONEY,GEAR,GENERIC,QUEST,CLUE -> null;
                default -> category.name();
            };
            if(skillName==null)return null;
            try {return skillIconManager.getSkillImage(Skill.valueOf(skillName));}
            catch(IllegalArgumentException unsupported) {return null;}
        });
        nextPanel.setAccountModeAction((hash,mode) -> clientThread.invoke(() -> saveAccountMode(hash,mode)));
        // Appearance is a RuneLite profile UI preference, independent of boss or account evidence.
        // No user/gameplay data is written by these two keys.
        if (configManager != null) {
            // CP-4: same keys as the native TrashPandaConfig cog, so both controls always agree.
            nextPanel.setAppearance(configManager.getConfiguration(TrashPandaConfig.GROUP, TrashPandaConfig.FONT_SIZE),
                configManager.getConfiguration(TrashPandaConfig.GROUP, TrashPandaConfig.ACCENT));
            nextPanel.setTextWeight(configManager.getConfiguration(TrashPandaConfig.GROUP, TrashPandaConfig.TEXT_WEIGHT));
            nextPanel.setAppearanceAction((size,color) -> {
                configManager.setConfiguration(TrashPandaConfig.GROUP, TrashPandaConfig.FONT_SIZE, size);
                configManager.setConfiguration(TrashPandaConfig.GROUP, TrashPandaConfig.ACCENT, color);
            });
            nextPanel.setTextWeightAction(weight ->
                configManager.setConfiguration(TrashPandaConfig.GROUP, TrashPandaConfig.TEXT_WEIGHT, weight));
        }
        // CP-7: Best in Bank fill focus (Damage / Defence / Prayer) is a native setting shared with the panel chip.
        if (configManager != null && bankPreloader != null)
            bankPreloader.setFocus(gg.feral.burrow.core.OwnedGearStats.Focus.parse(
                configManager.getConfiguration(TrashPandaConfig.GROUP, TrashPandaConfig.FILL_FOCUS)));
        nextPanel.setBestBankFocus(bankPreloader == null ? gg.feral.burrow.core.OwnedGearStats.Focus.MATCH : bankPreloader.focus());
        nextPanel.setBestBankFocusAction(f -> {
            if (configManager != null) configManager.setConfiguration(TrashPandaConfig.GROUP, TrashPandaConfig.FILL_FOCUS, f.name());
            else applyBestBankFocus(f);
        });
        // CP-4: a recreated sidebar starts with the current evidence (same thread contract as setAppearance above).
        nextPanel.setRecipeEvidence(bankPreloader == null ? null : bankPreloader.evidence());
        nextPanel.setPathfinderGoalAction((hash,id,mark) -> clientThread.invoke(() -> savePathfinderGoal(hash,id,mark)));
        nextPanel.setRareDropAction(new BurrowPanel.RareDropAction() {
            public void toggle(long hash, String key) { clientThread.invoke(() -> rareDropWrite(hash, st -> st.toggle(hash, key))); }
            public void mode(long hash, String enc, gg.feral.burrow.core.RareDropMath.Mode m) {
                clientThread.invoke(() -> rareDropWrite(hash, st -> st.setMode(hash, enc, m))); }
            public void manualKc(long hash, String enc, Long v) {
                clientThread.invoke(() -> rareDropWrite(hash, st -> { if (v == null) st.clearManual(hash, enc);
                    else st.recordKc(hash, enc, gg.feral.burrow.core.RareDropStore.Source.MANUAL, v, System.currentTimeMillis()); })); }
            public void hiscores(long hash, String enc) { clientThread.invoke(() -> lookupHiscoreKc(hash, enc)); }
        });
        nextPanel.setAccountModeState(-1L,null); // first-run UI waits for authenticated account
        nextPanel.setXpStartAction(() -> clientThread.invoke(this::requestExperimentalXpForCurrentLogin));
        nextPanel.setCrewTipAction((state,how) -> clientThread.invoke(() -> closeCrewTip(state,how)));
        nextPanel.setBankScanAction(() -> {}); // no manual scan required
        nextPanel.setOutfitterVisibilityAction(visible -> {
            bankPreloader.pauseForOutfitter(visible);
            if(visible)requestSelectedBestBank();
        });
        nextPanel.setLoadoutSelectedAction(this::requestSelectedBestBank);
        activeModel = nextModel;
        // Build 19: exactly ONE scroll owner. PluginPanel(false) disables RuneLite's own outer
        // wrapper scroll pane (which made the inner viewport full-height and swallowed the wheel).
        gg.feral.burrow.host.SidebarHost nextHostPanel=gg.feral.burrow.host.SidebarHost.mount(nextPanel);
        nextHostPanel.setRequestPublication(()->clientThread.invoke(this::publishAccountMemory));
        nextHostPanel.setRequestBankRefresh(hash->clientThread.invoke(()->requestBankMemoryRefresh(hash)));
        nextHostPanel.setManualReviewAction((hash,section)->
            clientThread.invoke(()->markCaptureScreenReviewed(hash,section)));
        nextHostPanel.setQuestSurveyAction(hash->clientThread.invoke(()->requestQuestRefresh(hash)));
        BufferedImage logo = TrashPandaToolbarIcon.create();
        NavigationButton next = NavigationButton.builder()
                .tooltip("TrashPanda Tracker • experimental").icon(logo).priority(8).panel(nextHostPanel).build();
        toolbar.addNavigation(next);
        panel = nextPanel;
        hostPanel = nextHostPanel;
        bankPreloader.pauseForOutfitter(nextModel.view()==BurrowModel.View.OUTFITTER);
        // A sidebar created after login still receives a current-login state.
        // The observer never invents XP that happened before panel registration.
        SessionScope uiScope = sessionScope;
        if (uiScope != null) publishSessionToPanel(uiScope, nextPanel);
        clientThread.invoke(this::publishKnownAccountMode);
        clientThread.invoke(this::publishAccountMemory);
        if (questSurveyStarted) {
            publishQuestSurvey(weaponGate.epoch());
            clientThread.invoke(this::publishPathfinderSuggestions);
        }

        navigation = next;
        // UI can be created after the client is already logged in. Bootstrap a
        // fresh, fenced observation only once the new panel actually exists.
        clientThread.invoke(() -> {
            if (!running.get()) return;
            long epoch = weaponGate.epoch();
            if (client.getGameState() == GameState.LOGGED_IN) {
                if (!weaponGate.canObserve(epoch)) epoch = weaponGate.enterLoggedIn();
                final long current = epoch;
                showLive(current, LiveWeapon.waiting());
                showCombat(current, LiveCombat.waiting());
                showQuest(current, QuestRecommendation.waiting());
                questTickCountdown = 3;
                pollWeapon(current);
                pollCombat(current);
            } else {
                long current = weaponGate.leaveLoggedIn();
                showLive(current, LiveWeapon.offline());
                showCombat(current, LiveCombat.offline());
                showQuest(current, QuestRecommendation.offline());
            }
        });
    }

    private void unregisterButton() {
        NavigationButton previous = navigation;
        navigation = null;
        activeModel = null;
        bankPreloader.pauseForOutfitter(false);
        ++accountModeRevision;
        // Immediately clear the temporary layout even if the client stays logged in.
        clientThread.invoke(() -> {
            OutfitterBank current = outfitterBank;
            if (current != null) current.clear();
        });
        panel = null;
        hostPanel = null;
        if (previous != null) toolbar.removeNavigation(previous);
    }
}
