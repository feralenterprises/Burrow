package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.ui.BurrowPanel;
import gg.feral.burrow.ui.BurrowPanel.FeedStatus;
import gg.feral.burrow.ui.BurrowPanel.SessionFeed;
import gg.feral.burrow.ui.BurrowPanel.SessionState;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/** BUR-0049 R12: synthetic HOST contract for an explicitly disabled XP feed.
 *  These are NOT native RuneLite observations or evidence of XP cache ownership.
 */
final class BurrowXpDisabledHostTest {
    private static final class World {
        GameState state = GameState.LOGGED_IN;
        long account = 101L;
        long overall = 1154L;
        final EnumMap<Skill, Integer> skills = new EnumMap<>(Skill.class);
        World() {
            for (Skill skill : Skill.values())
                skills.put(skill, skill == Skill.HITPOINTS ? 1154 : 0);
        }
        final Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
                new Class<?>[]{ Client.class }, (p, method, args) -> switch (method.getName()) {
                    case "getGameState" -> state;
                    case "getAccountHash" -> account;
                    case "getSkillExperience" -> skills.get((Skill) args[0]);
                    case "getRealSkillLevel" -> Math.min(99, Experience.getLevelForXp(skills.get((Skill) args[0])));
                    case "getOverallExperience" -> overall;
                    case "toString" -> "synthetic-explicitly-disabled-XP";
                    default -> throw new AssertionError("Unexpected mocked API: " + method.getName());
                });
        void set(Skill skill, int absolute) {
            overall += absolute - skills.get(skill);
            skills.put(skill, absolute);
        }
        void event(BurrowPlugin plugin, Skill skill) {
            int xp = skills.get(skill);
            int level = Math.min(99, Experience.getLevelForXp(xp));
            plugin.onStatChanged(new StatChanged(skill, xp, level, level));
        }
    }

    private static Field field(String name) throws Exception {
        Field f = BurrowPlugin.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
    private static Object get(BurrowPlugin plugin, String name) throws Exception {
        return field(name).get(plugin);
    }
    private static void call(BurrowPlugin plugin, String method) throws Exception {
        Method m = BurrowPlugin.class.getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(plugin);
    }
    private static void observedLogin(BurrowPlugin plugin) throws Exception {
        Method m = BurrowPlugin.class.getDeclaredMethod("observeXpLoginTransition", GameState.class);
        m.setAccessible(true);
        for (GameState state : new GameState[]{GameState.LOGIN_SCREEN, GameState.LOGGING_IN, GameState.LOGGED_IN})
            m.invoke(plugin, state);
    }
    private static void tick(BurrowPlugin plugin, int count) {
        for (int i = 0; i < count; i++) plugin.onGameTick(new GameTick());
    }
    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
    private static BurrowPlugin host(World world, BurrowPanel panel, boolean witnessedLogin) throws Exception {
        BurrowPlugin plugin = new BurrowPlugin();
        field("client").set(plugin, world.client);
        field("panel").set(plugin, panel);
        ((AtomicBoolean) get(plugin, "running")).set(true);
        if (witnessedLogin) observedLogin(plugin);
        return plugin;
    }
    private static void unavailable(BurrowPlugin plugin, BurrowPanel panel) throws Exception {
        flushEdt();
        assertFalse((boolean) get(plugin, "xpSynchronized"), "disabled host must never admit XP baseline");
        BurrowPanel.SessionReadout readout = panel.sessionReadout();
        assertEquals(SessionState.ACTIVE, readout.state());
        assertEquals(FeedStatus.UNAVAILABLE, readout.feeds().get(SessionFeed.XP));
        assertEquals(0L, readout.totalXp(), "disabled host must never fabricate earned XP");
        assertTrue(readout.xpBySkill().isEmpty(), "disabled host must never publish a skill gain");
    }

    @Test void all24SkillsIncludingRealZeroNeverEarnXpOrBecomeAvailable() throws Exception {
        World world = new World();
        BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin plugin = host(world, panel, true);
        assertEquals(24, Skill.values().length, "RuneLite 1.13.4 has 24 real enum skills");
        assertEquals(0, world.skills.get(Skill.FISHING).intValue()); // legitimate true zero XP
        tick(plugin, 3);
        unavailable(plugin, panel);
        long before = (long) get(plugin, "sessionEventSequence");
        for (Skill skill : Skill.values()) {
            world.event(plugin, skill); // initial sync flood and duplicates
            world.set(skill, world.skills.get(skill) + 1);
            world.event(plugin, skill); // apparent gameplay gain
            world.event(plugin, skill); // duplicate
            world.set(skill, world.skills.get(skill) - 1);
            world.event(plugin, skill); // backward/stale
        }
        tick(plugin, 3);
        unavailable(plugin, panel);
        assertEquals(before, (long) get(plugin, "sessionEventSequence"));
    }

    @Test void witnessedLoginStaleCachePlus959CannotLeakToSessions() throws Exception {
        World world = new World();
        world.set(Skill.MINING, 41); // coherent but hypothetically prior-account cache
        BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin plugin = host(world, panel, true);
        tick(plugin, 4);
        unavailable(plugin, panel);
        long before = (long) get(plugin, "sessionEventSequence");
        world.set(Skill.MINING, 1000); // account B's cache arrives; zero gameplay
        world.event(plugin, Skill.MINING); // source-level counterexample event
        tick(plugin, 3);
        unavailable(plugin, panel);
        assertEquals(before, (long) get(plugin, "sessionEventSequence"),
                "synthetic +959 must not generate an XP event");
        // Even a manually corrupted ready flag cannot turn a later StatChanged into XP.
        field("xpSynchronized").setBoolean(plugin, true);
        world.set(Skill.MINING, 1025);
        world.event(plugin, Skill.MINING);
        assertEquals(before, (long) get(plugin, "sessionEventSequence"));
        field("xpSynchronized").setBoolean(plugin, false);
        unavailable(plugin, panel);
    }

    @Test void evenDirectHostDeliveryCannotPublishXpOrAvailableState() throws Exception {
        World world = new World();
        BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin plugin = host(world, panel, true);
        tick(plugin, 3);
        unavailable(plugin, panel);
        Object scope = get(plugin, "sessionScope");
        Method availability = BurrowPlugin.class.getDeclaredMethod("emitXpAvailability", FeedStatus.class);
        availability.setAccessible(true);
        availability.invoke(plugin, FeedStatus.AVAILABLE); // should coerce to UNAVAILABLE
        unavailable(plugin, panel);
        long before = (long) get(plugin, "sessionEventSequence");
        Method direct = BurrowPlugin.class.getDeclaredMethod("emitSessionEvent", BurrowPanel.SessionEvent.class);
        direct.setAccessible(true);
        direct.invoke(plugin, new BurrowPanel.SessionXpGain((BurrowPanel.SessionScope) scope,
                "explicit-bypass-attempt", BurrowPanel.SessionOrigin.VERIFIED_BRIDGE,
                "Mining", 959L, "synthetic invalidated provenance"));
        assertEquals(before, (long) get(plugin, "sessionEventSequence"));
        unavailable(plugin, panel);
    }

    /** BUR-0049 defense-in-depth: a hand-built XP AVAILABLE status cannot escape the disabled host. */
    @Test void directFeedStatusAvailableCannotEscapeDisabledHost() throws Exception {
        World world = new World();
        BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin plugin = host(world, panel, true);
        tick(plugin, 3);
        unavailable(plugin, panel);
        Object scope = get(plugin, "sessionScope");
        Method direct = BurrowPlugin.class.getDeclaredMethod("emitSessionEvent", BurrowPanel.SessionEvent.class);
        direct.setAccessible(true);
        direct.invoke(plugin, new BurrowPanel.SessionFeedStatus((BurrowPanel.SessionScope) scope,
                "explicit-available-bypass", BurrowPanel.SessionOrigin.VERIFIED_BRIDGE,
                SessionFeed.XP, FeedStatus.AVAILABLE));
        unavailable(plugin, panel);
        // An UNAVAILABLE status is still allowed through (it is the truthful state).
        direct.invoke(plugin, new BurrowPanel.SessionFeedStatus((BurrowPanel.SessionScope) scope,
                "explicit-unavailable", BurrowPanel.SessionOrigin.VERIFIED_BRIDGE,
                SessionFeed.XP, FeedStatus.UNAVAILABLE));
        unavailable(plugin, panel);
    }

    @Test void accountAtoBtoAHopLogoutAndRestartKeepXpUnavailable() throws Exception {
        World world = new World();
        BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin plugin = host(world, panel, true);
        tick(plugin, 2);
        unavailable(plugin, panel);
        Object scopeA = get(plugin, "sessionScope");

        world.account = 202L; tick(plugin, 1);
        Object scopeB = get(plugin, "sessionScope");
        assertNotSame(scopeA, scopeB);
        unavailable(plugin, panel);
        world.set(Skill.MINING, 1000); world.event(plugin, Skill.MINING);
        unavailable(plugin, panel);

        world.account = 101L; tick(plugin, 1);
        assertNotSame(scopeA, get(plugin, "sessionScope"));
        assertNotSame(scopeB, get(plugin, "sessionScope"));
        unavailable(plugin, panel);

        call(plugin, "pauseVerifiedSession");
        world.state = GameState.HOPPING;
        tick(plugin, 2);
        world.state = GameState.LOGGED_IN;
        tick(plugin, 2);
        unavailable(plugin, panel); // same-login scope stays but XP stays off

        call(plugin, "finishVerifiedSession");
        flushEdt();
        assertEquals(SessionState.OFFLINE, panel.sessionReadout().state());
        assertEquals(0L, panel.sessionReadout().totalXp());
        // Restart while already logged in has no fresh event ordering witness.
        BurrowPanel otherPanel = new BurrowPanel(new BurrowModel());
        BurrowPlugin restarted = host(world, otherPanel, false);
        tick(restarted, 5);
        unavailable(restarted, otherPanel);
        world.set(Skill.FISHING, 1); world.event(restarted, Skill.FISHING);
        unavailable(restarted, otherPanel);
    }
}
