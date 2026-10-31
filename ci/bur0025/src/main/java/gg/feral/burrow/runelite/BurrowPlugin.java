package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.LiveWeapon;
import gg.feral.burrow.core.LiveWeaponGate;
import gg.feral.burrow.core.LiveCombat;
import gg.feral.burrow.core.QuestRecommendation;
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
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private PluginManager pluginManager;
    @Inject private OverlayManager overlayManager;
    @Inject private ItemManager itemManager;
    private final ConcurrentHashMap<Integer,BufferedImage> itemImages = new ConcurrentHashMap<>();
    private BankChestOverlay chestOverlay;
    // Client-thread-only controller. The published selection snapshot is immutable.
    private OutfitterBank outfitterBank;
    private volatile BurrowModel activeModel;
    private final LiveWeaponGate weaponGate = new LiveWeaponGate();
    // BUR-0049: separate from weaponGate! A world hop retires the drawing epoch,
    // never the logical Sessions login. All fields except sessionScope are client-thread-only.
    // BUR-0049/Tongs L2: OFF for every production build until independently
    // proven account-bound XP cache readiness and Wrench release approval.
    // No user configuration or runtime override may enable this unsafe feed.
    private static final boolean SESSION_XP_ENABLED = false;
    private final RuneLiteXpObservationAdapter xpObserver = new RuneLiteXpObservationAdapter();
    private volatile SessionScope sessionScope;
    private long sessionAccountHash = -1L; // ephemeral authenticated hash, never logged or exported
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
        running.set(true);
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
            outfitterBank.refresh();
        });
    }

    @Override protected void shutDown() {
        running.set(false);
        itemImages.clear();
        weaponGate.stop(); // invalidates pending client reads and Swing paints
        // stop is genuine: revoke scope and erase Session state, including old EDT work.
        SessionScope ending = sessionScope;
        sessionScope = null;
        xpObserver.pluginStop();
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
    @Subscribe public void onGameTick(GameTick event) {
        if (!running.get()) return;
        OutfitterBank current = outfitterBank;
        if (current != null) current.refresh();
        // State reads run on RuneLite's client thread and only in the current login epoch.
        if (client.getGameState() != GameState.LOGGED_IN) return;
        // Client-thread tick after RuneLite's stat callbacks; login sync must be
        // independently evidenced before a baseline may be initialized.
        ensureVerifiedSession();
        trySynchronizeXp();
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
        observeXpLoginTransition(event.getGameState());
        // Any non-LOGGED_IN state (hopping, loading, reconnect, logout, etc.)
        // retires the previous epoch immediately without storing player identity.
        if (event.getGameState() == GameState.LOGGED_IN) {
            // Returning from HOPPING/LOADING with the same authenticated account
            // reuses the same immutable session. A real account change opens new.
            ensureVerifiedSession();
            long epoch = weaponGate.enterLoggedIn();
            showLive(epoch, LiveWeapon.waiting());
            showCombat(epoch, LiveCombat.waiting());
            showQuest(epoch, QuestRecommendation.waiting());
            // Give quest varps a few client ticks to settle after the login transition.
            questTickCountdown = 3;
            clientThread.invoke(() -> { pollWeapon(epoch); pollCombat(epoch); });
        } else {
            // LOGIN_SCREEN is a confirmed end of this login. Other transitions
            // are suspended, never converted into false logout/reset events.
            if (event.getGameState() == GameState.LOGIN_SCREEN ||
                event.getGameState() == GameState.LOGIN_SCREEN_AUTHENTICATOR) {
                finishVerifiedSession();
            } else if (event.getGameState() != GameState.LOADING) {
                // Ordinary scene-loading (teleport/region) must NOT discard a
                // synchronized skill baseline. Hops/disconnects do rebaseline.
                pauseVerifiedSession();
            }
            OutfitterBank current = outfitterBank;
            if (current != null) current.clear();
            long epoch = weaponGate.leaveLoggedIn();
            questTickCountdown = 0;
            showLive(epoch, LiveWeapon.offline());
            showCombat(epoch, LiveCombat.offline());
            showQuest(epoch, QuestRecommendation.offline());
        }
    }

    @Subscribe public void onItemContainerChanged(ItemContainerChanged event) {
        if (!running.get() || event.getContainerId() != InventoryID.EQUIPMENT.getId()) return;
        long epoch = weaponGate.epoch();
        if (weaponGate.canObserve(epoch)) clientThread.invoke(() -> pollWeapon(epoch));
    }

    @Subscribe public void onStatChanged(StatChanged event) {
        if (!running.get()) return;
        // Genuine RuneLite EventBus callback, not an XP-estimate from level changes.
        observeSessionXp(event);
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
        if (!running.get()) return;
        long hash = authenticatedAccountHash();
        if (hash == -1L) return; // unproven login: fail closed
        if (sessionScope != null && sessionAccountHash != hash) finishVerifiedSession();
        if (sessionScope == null) {
            // Account identity is checked privately; exported tokens are random and opaque.
            SessionScope created = new SessionScope(UUID.randomUUID().toString(),
                                                     UUID.randomUUID().toString());
            sessionAccountHash = hash;
            sessionScope = created;
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
            sessionFence = xpObserver.beginVerifiedLogin(created);
            publishSessionToPanel(created, panel);
        } else if (sessionPaused) {
            sessionPaused = false;
            sessionFence = xpObserver.resumeVerifiedSameLogin(sessionScope);
            resetXpReadiness();
            emitSessionEvent(new BurrowPanel.SessionResume(sessionScope, nextSessionEventId(),
                                                           SessionOrigin.VERIFIED_BRIDGE));
            emitXpAvailability(FeedStatus.UNAVAILABLE);
        }
    }

    private void resetXpReadiness() {
        xpSynchronized = false;
        xpReadiness.clear();
    }

    private synchronized String nextSessionEventId() {
        return "verified-xp-" + (++sessionEventSequence);
    }

    private void emitSessionEvent(SessionEvent event) {
        // Defense-in-depth against accidental future bypass of either entry gate.
        if (!SESSION_XP_ENABLED && event instanceof BurrowPanel.SessionXpGain) return;
        SessionScope expected = event.scope();
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
        String startId = nextSessionEventId();
        String statusId = nextSessionEventId();
        SwingUtilities.invokeLater(() -> {
            if (!running.get() || sessionScope != scope || panel != destination) return;
            destination.ingestSessionEvent(new BurrowPanel.SessionStart(
                    scope, startId, SessionOrigin.VERIFIED_BRIDGE));
            destination.ingestSessionEvent(new BurrowPanel.SessionFeedStatus(
                    scope, statusId, SessionOrigin.VERIFIED_BRIDGE,
                    SessionFeed.XP, SESSION_XP_ENABLED && xpSynchronized
                        ? FeedStatus.AVAILABLE : FeedStatus.UNAVAILABLE));
        });
    }

    private void emitXpAvailability(FeedStatus status) {
        if (sessionScope == null || sessionPaused) return;
        // Defense-in-depth: never claim observed XP when the host feed is off.
        if (!SESSION_XP_ENABLED) status = FeedStatus.UNAVAILABLE;
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
        if (old == null) return;
        xpObserver.confirmedLogout(old);
        priorSessionAccountHash = sessionAccountHash;
        sessionScope = null; // invalidate all queued XP deliveries before posting stop
        sessionAccountHash = -1L;
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
        sessionFence = xpObserver.pauseTransient(current);
        resetXpReadiness();
        emitSessionEvent(new BurrowPanel.SessionPause(current, nextSessionEventId(),
                                                       SessionOrigin.VERIFIED_BRIDGE));
    }

    private void observeSessionXp(StatChanged event) {
        // StatChanged may reflect another account's stale synchronized cache.
        // Keep the adapter for future review; emit no XP in this release candidate.
        if (!SESSION_XP_ENABLED) return;
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
        xpObserver.onStatChanged(scope, sessionFence, event, client, true).ifPresent(delta -> {
            // Client-thread provenance checked above; EDT revalidates the login.
            emitSessionEvent(new BurrowPanel.SessionXpGain(scope, nextSessionEventId(),
                    SessionOrigin.VERIFIED_BRIDGE, delta.skill().getName(),
                    delta.delta(), delta.sourceEvidence()));
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
        if (!SESSION_XP_ENABLED) return;
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
        activeModel = nextModel;
        PluginPanel nextHostPanel = new PluginPanel() { };
        nextHostPanel.setLayout(new BorderLayout());
        // Explicit real scroll container: PluginPanel's internal wrapper is not enough
        // once this host is assigned its own BorderLayout. Small RuneLite heights must work.
        javax.swing.JScrollPane journalScroll = new javax.swing.JScrollPane(nextPanel,
                javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        journalScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        journalScroll.getVerticalScrollBar().setUnitIncrement(20);
        nextHostPanel.add(journalScroll, BorderLayout.CENTER);
        BufferedImage logo = TrashPandaToolbarIcon.create();
        NavigationButton next = NavigationButton.builder()
                .tooltip("TrashPanda Tracker • experimental").icon(logo).priority(8).panel(nextHostPanel).build();
        toolbar.addNavigation(next);
        panel = nextPanel;
        hostPanel = nextHostPanel;
        // A sidebar created after login still receives a current-login state.
        // The observer never invents XP that happened before panel registration.
        SessionScope uiScope = sessionScope;
        if (uiScope != null) publishSessionToPanel(uiScope, nextPanel);

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
