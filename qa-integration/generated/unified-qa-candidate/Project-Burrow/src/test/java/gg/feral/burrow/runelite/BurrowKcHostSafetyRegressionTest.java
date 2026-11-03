package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.ui.BurrowPanel;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

/** Tests intentionally synthesize RuneLite callbacks. They do not attest real server kills. */
final class BurrowKcHostSafetyRegressionTest {
    static final class World {
        volatile GameState state = GameState.LOGGED_IN;
        volatile long hash = 101L;
        volatile boolean throwHash;
        final Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
                new Class<?>[]{Client.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getGameState": return state;
                    case "getRealSkillLevel": return 1; // Optional Pathfinder observation; not XP evidence.
                        case "getAccountHash":
                            if (throwHash) throw new IllegalStateException("identity unavailable");
                            return hash;
                        default: throw new AssertionError("Unexpected client read: " + method.getName());
                    }
                });
    }

    private static Field field(String name) throws Exception {
        Field result = BurrowPlugin.class.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }
    private static void flush() throws Exception { SwingUtilities.invokeAndWait(() -> {}); }
    private static BurrowPlugin host(World w, BurrowPanel p) throws Exception {
        BurrowPlugin result = new BurrowPlugin();
        field("client").set(result, w.client);
        field("panel").set(result, p);
        ((AtomicBoolean)field("running").get(result)).set(true);
        return result;
    }
    private static void chat(BurrowPlugin host, ChatMessageType type, int absoluteCount) {
        ChatMessage event = new ChatMessage();
        event.setType(type);
        event.setMessage("Your Vorkath kill count is: " + absoluteCount + ".");
        host.onChatMessage(event);
    }
    private static void tick(BurrowPlugin host) { host.onGameTick(new GameTick()); }
    private static void stateEvent(BurrowPlugin host, GameState state) {
        GameStateChanged event = new GameStateChanged();
        event.setGameState(state);
        host.onGameStateChanged(event);
    }
    private static void gain(BurrowPlugin host, BurrowPanel panel) throws Exception {
        chat(host, ChatMessageType.GAMEMESSAGE, 100); // per-account/epoch baseline only
        chat(host, ChatMessageType.GAMEMESSAGE, 101); // accepted +1
        flush();
        assertEquals(1L, panel.sessionReadout().kcByBoss().getOrDefault("Vorkath", 0L));
        assertEquals(0L, panel.sessionReadout().totalXp());
        assertEquals(BurrowPanel.FeedStatus.UNAVAILABLE,
                panel.sessionReadout().feeds().get(BurrowPanel.SessionFeed.XP));
    }
    private static void assertRetired(BurrowPlugin host, BurrowPanel panel) throws Exception {
        flush();
        assertNull(field("sessionScope").get(host));
        assertTrue(panel.sessionReadout().kcByBoss().isEmpty());
        assertEquals(0L, panel.sessionReadout().totalXp());
        assertTrue(((java.util.List<?>)field("kcPresentation").get(host)).isEmpty());
    }

    @Test void terminalStatesWithoutStateEventMustRetireFromTick() throws Exception {
        for (GameState terminal : new GameState[]{GameState.LOGIN_SCREEN,
                GameState.LOGIN_SCREEN_AUTHENTICATOR, GameState.STARTING}) {
            World world = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
            BurrowPlugin host = host(world, panel); gain(host, panel);
            world.state = terminal; // deliberately OMIT GameStateChanged
            tick(host);
            assertRetired(host, panel);
            world.state = GameState.LOGGED_IN;
            chat(host, ChatMessageType.GAMEMESSAGE, 105); // no old +4 leak
            flush(); assertTrue(panel.sessionReadout().kcByBoss().isEmpty());
            chat(host, ChatMessageType.GAMEMESSAGE, 106);
            flush(); assertEquals(1L, panel.sessionReadout().kcByBoss().getOrDefault("Vorkath", 0L));
        }
    }

    @Test void terminalStatesWithoutStateEventMustRetireFromChat() throws Exception {
        for (GameState terminal : new GameState[]{GameState.LOGIN_SCREEN,
                GameState.LOGIN_SCREEN_AUTHENTICATOR, GameState.STARTING}) {
            World world = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
            BurrowPlugin host = host(world, panel); gain(host, panel);
            world.state = terminal; // deliberately OMIT GameStateChanged
            chat(host, ChatMessageType.GAMEMESSAGE, 500);
            assertRetired(host, panel);
            chat(host, ChatMessageType.GAMEMESSAGE, 501);
            assertRetired(host, panel); // idempotent
        }
    }

    @Test void transientStatesPreserveSessionAndRebaselineOnTickAndChat() throws Exception {
        for (GameState transientState : new GameState[]{GameState.HOPPING,
                GameState.LOADING, GameState.CONNECTION_LOST}) {
            World world = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
            BurrowPlugin host = host(world, panel); gain(host, panel);
            Object originalScope = field("sessionScope").get(host);
            world.state = transientState;
            chat(host, ChatMessageType.GAMEMESSAGE, 900); tick(host);
            flush();
            assertSame(originalScope, field("sessionScope").get(host));
            assertEquals(1L, panel.sessionReadout().kcByBoss().getOrDefault("Vorkath", 0L));
            world.state = GameState.LOGGED_IN;
            tick(host); // same-account return opens new *baseline*, not gain
            chat(host, ChatMessageType.GAMEMESSAGE, 300);
            flush(); assertEquals(1L, panel.sessionReadout().kcByBoss().getOrDefault("Vorkath", 0L));
            chat(host, ChatMessageType.GAMEMESSAGE, 301);
            flush(); assertEquals(2L, panel.sessionReadout().kcByBoss().getOrDefault("Vorkath", 0L));
        }
    }

    @Test void stateCallbackTerminalAndTransientUseTheSameDisposition() throws Exception {
        for (GameState transientState : new GameState[]{GameState.HOPPING,
                GameState.LOADING, GameState.CONNECTION_LOST}) {
            World world = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
            BurrowPlugin host = host(world, panel); gain(host, panel);
            Object scope = field("sessionScope").get(host);
            world.state = transientState;
            stateEvent(host, transientState);
            flush(); assertSame(scope, field("sessionScope").get(host));
            assertEquals(1L, panel.sessionReadout().kcByBoss().getOrDefault("Vorkath",0L));
            world.state = GameState.LOGGED_IN;
            tick(host);
            chat(host, ChatMessageType.GAMEMESSAGE, 500); // fresh observation baseline
            flush(); assertEquals(1L, panel.sessionReadout().kcByBoss().getOrDefault("Vorkath",0L));
        }
        for (GameState terminal : new GameState[]{GameState.LOGIN_SCREEN,
                GameState.LOGIN_SCREEN_AUTHENTICATOR, GameState.STARTING}) {
            World world = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
            BurrowPlugin host = host(world, panel); gain(host, panel);
            world.state = terminal;
            stateEvent(host, terminal);
            assertRetired(host, panel);
            stateEvent(host, terminal); // idempotent duplicate state callback
            assertRetired(host, panel);
        }
    }

    /** BUR-0025 R03: transient first, terminal second, delivered by state callback, tick or chat. */
    @Test void transientThenTerminalRetiresTheAdapterLogin() throws Exception {
        Field activeAccount = RuneLiteBossKcAdapter.class.getDeclaredField("activeAccount");
        activeAccount.setAccessible(true);
        for (GameState transientState : new GameState[]{GameState.HOPPING,
                GameState.LOADING, GameState.CONNECTION_LOST}) {
            for (GameState terminal : new GameState[]{GameState.LOGIN_SCREEN,
                    GameState.LOGIN_SCREEN_AUTHENTICATOR, GameState.STARTING}) {
                for (int path = 0; path < 3; path++) { // 0 state callback, 1 tick, 2 chat
                    World world = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
                    BurrowPlugin host = host(world, panel); gain(host, panel);
                    world.state = transientState;
                    stateEvent(host, transientState);
                    world.state = terminal;
                    if (path == 0) stateEvent(host, terminal);
                    else if (path == 1) tick(host);
                    else chat(host, ChatMessageType.GAMEMESSAGE, 777);
                    assertRetired(host, panel);
                    Object adapter = field("kcAdapter").get(host);
                    assertNotNull(adapter);
                    assertNull(activeAccount.get(adapter),
                        transientState + "->" + terminal + " path " + path + " left the adapter login open");
                    // Same account logs in again: a fresh login, not a continuation.
                    world.state = GameState.LOGGED_IN;
                    tick(host); // use the same login path as existing host tests; no clientThread injected here
                    chat(host, ChatMessageType.GAMEMESSAGE, 102); // new baseline only
                    flush(); assertTrue(panel.sessionReadout().kcByBoss().isEmpty());
                    chat(host, ChatMessageType.GAMEMESSAGE, 103);
                    flush(); assertEquals(1L, panel.sessionReadout().kcByBoss().getOrDefault("Vorkath", 0L));
                }
            }
        }
    }

    @Test void unknownAndThrowingIdentityAtTickOrChatRetiresAndRebaselines() throws Exception {
        for (boolean fromTick : new boolean[]{false,true}) {
            for (boolean thrown : new boolean[]{false,true}) {
                World world = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
                BurrowPlugin host = host(world, panel); gain(host, panel);
                world.hash = -1L; world.throwHash = thrown;
                if (fromTick) tick(host); else chat(host, ChatMessageType.GAMEMESSAGE, 102);
                assertRetired(host, panel);
                world.hash = 101L; world.throwHash = false;
                chat(host, ChatMessageType.GAMEMESSAGE, 103); // fresh baseline
                flush(); assertTrue(panel.sessionReadout().kcByBoss().isEmpty());
                chat(host, ChatMessageType.GAMEMESSAGE, 104);
                flush(); assertEquals(1L, panel.sessionReadout().kcByBoss().getOrDefault("Vorkath", 0L));
            }
        }
    }

    @Test void accountAtoBtoAAndDuplicateSamplesCannotCarryTotals() throws Exception {
        World world = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin host = host(world, panel); gain(host, panel);
        world.hash = 202L; chat(host, ChatMessageType.GAMEMESSAGE, 30);
        flush(); assertTrue(panel.sessionReadout().kcByBoss().isEmpty());
        chat(host, ChatMessageType.GAMEMESSAGE, 30);
        chat(host, ChatMessageType.PUBLICCHAT, 31);
        flush(); assertTrue(panel.sessionReadout().kcByBoss().isEmpty());
        chat(host, ChatMessageType.GAMEMESSAGE, 31);
        flush(); assertEquals(1L, panel.sessionReadout().kcByBoss().getOrDefault("Vorkath",0L));
        world.hash = 101L; chat(host, ChatMessageType.GAMEMESSAGE, 105);
        flush(); assertTrue(panel.sessionReadout().kcByBoss().isEmpty());
        chat(host, ChatMessageType.GAMEMESSAGE, 106);
        flush(); assertEquals(1L, panel.sessionReadout().kcByBoss().getOrDefault("Vorkath",0L));
    }

    @Test void pendingOldEdtGainDoesNotLeakIntoDifferentAccount() throws Exception {
        World world = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin host = host(world, panel);
        CountDownLatch blockerEntered = new CountDownLatch(1), unblock = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            blockerEntered.countDown();
            try { unblock.await(4, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        try {
            assertTrue(blockerEntered.await(2, TimeUnit.SECONDS));
            chat(host, ChatMessageType.GAMEMESSAGE, 1);
            chat(host, ChatMessageType.GAMEMESSAGE, 2); // queued under A
            world.hash = 202L;
            chat(host, ChatMessageType.GAMEMESSAGE, 40); // retires A before EDT accepts it
        } finally { unblock.countDown(); }
        flush();
        assertTrue(panel.sessionReadout().kcByBoss().isEmpty());
        chat(host, ChatMessageType.GAMEMESSAGE, 41); flush();
        assertEquals(1L, panel.sessionReadout().kcByBoss().getOrDefault("Vorkath", 0L));
    }

    @Test void panelReplacementReplaysOnlyCurrentScopeOnce() throws Exception {
        World world = new World(); BurrowPanel first = new BurrowPanel(new BurrowModel());
        BurrowPlugin host = host(world, first); gain(host, first);
        BurrowPanel second = new BurrowPanel(new BurrowModel());
        field("panel").set(host, second);
        Method replay = BurrowPlugin.class.getDeclaredMethod("publishSessionToPanel",
                BurrowPanel.SessionScope.class, BurrowPanel.class);
        replay.setAccessible(true);
        replay.invoke(host, field("sessionScope").get(host), second);
        flush(); assertEquals(1L, second.sessionReadout().kcByBoss().getOrDefault("Vorkath", 0L));
        replay.invoke(host, field("sessionScope").get(host), second);
        flush(); assertEquals(1L, second.sessionReadout().kcByBoss().getOrDefault("Vorkath", 0L));
        world.state = GameState.LOGIN_SCREEN;
        tick(host); assertRetired(host, second);
    }

    @Test void firstTickWhileAlreadyLoggedInBootstrapsOnlyABaseline() throws Exception {
        World world = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin host = host(world, panel);
        tick(host); flush(); assertNotNull(field("sessionScope").get(host));
        chat(host, ChatMessageType.GAMEMESSAGE, 600);
        flush(); assertTrue(panel.sessionReadout().kcByBoss().isEmpty());
        chat(host, ChatMessageType.GAMEMESSAGE, 601);
        flush(); assertEquals(1L, panel.sessionReadout().kcByBoss().getOrDefault("Vorkath",0L));
        assertEquals(0L, panel.sessionReadout().totalXp());
    }

    @Test void queuedEdtGainIsFencedByStopGenerationAndScope() throws Exception {
        World world = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin host = host(world, panel);
        CountDownLatch blockerEntered = new CountDownLatch(1), unblock = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            blockerEntered.countDown();
            try { unblock.await(4, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        try {
            assertTrue(blockerEntered.await(2, TimeUnit.SECONDS));
            chat(host, ChatMessageType.GAMEMESSAGE, 1);
            chat(host, ChatMessageType.GAMEMESSAGE, 2);
            // Simulates the shutdown's *early invalidation envelope*. Full
            // shutDown() requires RuneLite-managed injected dependencies.
            ((AtomicBoolean)field("running").get(host)).set(false);
            field("sessionScope").set(host, null);
            Field generation = field("kcGeneration");
            generation.setLong(host, generation.getLong(host) + 1);
        } finally { unblock.countDown(); }
        flush(); assertTrue(panel.sessionReadout().kcByBoss().isEmpty());
    }

    @Test void staleOriginTextCannotBeAuthenticatedByMessageOnly() throws Exception {
        World world = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin host = host(world, panel);
        chat(host, ChatMessageType.GAMEMESSAGE, 5); // account A baseline
        world.hash = 202L;
        chat(host, ChatMessageType.GAMEMESSAGE, 10); // account B baseline
        // Labeled A-origin by this fixture, but RuneLite ChatMessage carries no
        // authenticated origin. Current observer CANNOT verify our assertion.
        chat(host, ChatMessageType.GAMEMESSAGE, 200);
        flush();
        assertEquals(190L, panel.sessionReadout().kcByBoss().getOrDefault("Vorkath", 0L));
        // This is a documented limitation, NOT an assertion that 190 real kills occurred.
    }
}
