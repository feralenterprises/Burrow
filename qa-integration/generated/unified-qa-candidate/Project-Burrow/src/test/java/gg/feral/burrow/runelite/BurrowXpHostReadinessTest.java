package gg.feral.burrow.runelite;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/** Actual BUR-0049 host observer/lifecycle fixture; no RuneLite player was used. */
public final class BurrowXpHostReadinessTest {
    private static final class World {
        private final EnumMap<Skill,Integer> xp = new EnumMap<>(Skill.class);
        private final EnumMap<Skill,Integer> levels = new EnumMap<>(Skill.class);
        long account = 2001L;
        GameState state = GameState.LOGGED_IN;
        long overall;
        final Client client = (Client) Proxy.newProxyInstance(
            Client.class.getClassLoader(), new Class<?>[]{Client.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getGameState" -> state;
                case "getAccountHash" -> account;
                case "getSkillExperience" -> xp.getOrDefault((Skill) args[0], -1);
                case "getRealSkillLevel" -> levels.getOrDefault((Skill) args[0], 0);
                case "getOverallExperience" -> overall;
                case "toString" -> "offline-world";
                default -> throw new AssertionError("unexpected client API " + method.getName());
            });
        World() {
            for (Skill skill : Skill.values()) {
                if (skill == Skill.OVERALL) continue;
                xp.put(skill, skill == Skill.HITPOINTS ? 1154 : 0);
                levels.put(skill, skill == Skill.HITPOINTS ? 10 : 1);
            }
            overall = 1154;
        }
        void earn(Skill skill, int amount) {
            xp.put(skill, xp.get(skill) + amount);
            overall += amount;
        }
    }
    private static BurrowPlugin host(World world) throws Exception {
        return host(world, true);
    }
    private static BurrowPlugin host(World world, boolean witnessedLogin) throws Exception {
        BurrowPlugin plugin = new BurrowPlugin();
        Field client = BurrowPlugin.class.getDeclaredField("client");
        client.setAccessible(true);
        client.set(plugin, world.client);
        Field running = BurrowPlugin.class.getDeclaredField("running");
        running.setAccessible(true);
        ((AtomicBoolean) running.get(plugin)).set(true);
        if (witnessedLogin) witnessedLogin(plugin);
        return plugin;
    }
    private static Object field(BurrowPlugin plugin, String name) throws Exception {
        Field f = BurrowPlugin.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(plugin);
    }
    private static void invoke(BurrowPlugin plugin, String name) throws Exception {
        Method m = BurrowPlugin.class.getDeclaredMethod(name);
        m.setAccessible(true);
        m.invoke(plugin);
    }
    private static void witnessedLogin(BurrowPlugin plugin) throws Exception {
        Method transition = BurrowPlugin.class.getDeclaredMethod("observeXpLoginTransition", GameState.class);
        transition.setAccessible(true);
        transition.invoke(plugin, GameState.LOGIN_SCREEN);
        transition.invoke(plugin, GameState.LOGGING_IN);
        transition.invoke(plugin, GameState.LOGGED_IN);
    }
    private static void ticks(BurrowPlugin plugin, int count) {
        for (int i = 0; i < count; i++) plugin.onGameTick(new GameTick());
    }

    @Test void witnessedFreshLoginCannotSeedOrCountWhileDisabled() throws Exception {
        World world = new World();
        BurrowPlugin plugin = host(world);
        ticks(plugin, 1);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
        ticks(plugin, 1);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
        long before = (long) field(plugin, "sessionEventSequence");
        world.earn(Skill.WOODCUTTING, 5);
        plugin.onStatChanged(new StatChanged(Skill.WOODCUTTING, 5, 1, 1));
        assertEquals(before, (long) field(plugin, "sessionEventSequence"));
        plugin.onStatChanged(new StatChanged(Skill.WOODCUTTING, 5, 1, 20));
        assertEquals(before, (long) field(plugin, "sessionEventSequence"));
    }

    @Test void pluginEnabledAlreadyLoggedInDoesNotAcceptOldCache() throws Exception {
        World world = new World();
        BurrowPlugin plugin = host(world, false);
        world.account = 3002L;
        world.earn(Skill.MINING, 41); // hypothetically A's old cache
        ticks(plugin, 5);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
        world.earn(Skill.MINING, 959); // B's actual cache, not gameplay
        plugin.onStatChanged(new StatChanged(Skill.MINING, 1000, 9, 9));
        ticks(plugin, 3);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
        assertEquals(0L, (long) field(plugin, "sessionEventSequence")); // No panel: no startup event IDs
        witnessedLogin(plugin);
        ticks(plugin, 2);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
        long before = (long) field(plugin, "sessionEventSequence");
        world.earn(Skill.FISHING, 5);
        plugin.onStatChanged(new StatChanged(Skill.FISHING, 5, 1, 1));
        assertEquals(before, (long) field(plugin, "sessionEventSequence"));
    }

    @Test void absentOrPartialXpCannotBaseline() throws Exception {
        World world = new World();
        BurrowPlugin plugin = host(world);
        world.xp.put(Skill.MINING, -1);
        ticks(plugin, 3);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
        world.xp.put(Skill.MINING, 0);
        world.overall = 5; // a partial/invalid overall total
        ticks(plugin, 2);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
        world.overall = 1154;
        ticks(plugin, 2);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
    }

    @Test void validLevelAndHpEvidenceCannotEnableDisabledFeed() throws Exception {
        World world = new World();
        BurrowPlugin plugin = host(world);
        world.levels.put(Skill.HITPOINTS, 1);
        ticks(plugin, 3);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
        world.levels.put(Skill.HITPOINTS, 10);
        world.xp.put(Skill.HITPOINTS, 0);
        world.overall = 0;
        ticks(plugin, 2);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
        world.xp.put(Skill.HITPOINTS, 1154);
        world.overall = 1154;
        ticks(plugin, 2);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
    }

    @Test void changingXpSnapshotsNeverOverrideDisabledFeed() throws Exception {
        World world = new World();
        BurrowPlugin plugin = host(world);
        ticks(plugin, 1);
        world.earn(Skill.MINING, 1);
        ticks(plugin, 1);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
        ticks(plugin, 1);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
    }

    @Test void hopAndSameLoginKeepScopeWithoutEnablingXp() throws Exception {
        World world = new World();
        BurrowPlugin plugin = host(world);
        ticks(plugin, 2);
        Object scope = field(plugin, "sessionScope");
        invoke(plugin, "pauseVerifiedSession");
        assertSame(scope, field(plugin, "sessionScope"));
        assertFalse((boolean) field(plugin, "xpSynchronized"));
        world.state = GameState.HOPPING;
        ticks(plugin, 2);
        assertSame(scope, field(plugin, "sessionScope"));
        world.state = GameState.LOGGED_IN;
        ticks(plugin, 2);
        assertSame(scope, field(plugin, "sessionScope"));
        assertFalse((boolean) field(plugin, "xpSynchronized"));
        invoke(plugin, "finishVerifiedSession");
        assertNull(field(plugin, "sessionScope"));
        ticks(plugin, 2);
        assertNotSame(scope, field(plugin, "sessionScope"));
    }

    @Test void accountSwapMustCreateNewScopeAndBlockStaleXp() throws Exception {
        World world = new World();
        BurrowPlugin plugin = host(world);
        ticks(plugin, 2);
        Object scopeA = field(plugin, "sessionScope");
        world.account = 3002L;
        ticks(plugin, 1);
        Object scopeB = field(plugin, "sessionScope");
        assertNotSame(scopeA, scopeB);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
        ticks(plugin, 3);
        assertFalse((boolean) field(plugin, "xpSynchronized")); // old-account cache alone is NOT trustworthy
        plugin.onStatChanged(new StatChanged(Skill.MINING, 0, 1, 1));
        ticks(plugin, 2);
        assertFalse((boolean) field(plugin, "xpSynchronized"));
        assertTrue((boolean) field(plugin, "accountXpQuarantined"));
        world.account = 2001L;
        ticks(plugin, 1);
        assertNotSame(scopeA, field(plugin, "sessionScope"));
        assertNotSame(scopeB, field(plugin, "sessionScope"));
        assertFalse((boolean) field(plugin, "xpSynchronized"));
    }
}
