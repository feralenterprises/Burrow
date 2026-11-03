package gg.feral.burrow.runelite;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.Experience;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** R06 restart attack: source-level JVM fixture, NOT live RuneLite evidence. */
public final class BurrowXpRestartSafetyTest {
    static class World {
        long account = 2002L;
        long overall = 1154;
        GameState state = GameState.LOGGED_IN;
        final EnumMap<Skill,Integer> xp = new EnumMap<>(Skill.class);
        World() {
            for (Skill skill : Skill.values()) if (skill != Skill.OVERALL)
                xp.put(skill, skill == Skill.HITPOINTS ? 1154 : 0);
        }
        final Client client = (Client)Proxy.newProxyInstance(Client.class.getClassLoader(),
                new Class<?>[]{Client.class}, (p,m,args) -> switch (m.getName()) {
                    case "getAccountHash" -> account;
                    case "getGameState" -> state;
                    case "getSkillExperience" -> xp.get((Skill)args[0]);
                    case "getRealSkillLevel" -> Math.min(99,Experience.getLevelForXp(xp.get((Skill)args[0])));
                    case "getOverallExperience" -> overall;
                    case "toString" -> "R06 synthetic cache";
                    default -> throw new AssertionError("Unexpected API: "+m.getName());
                });
        void set(Skill skill, int value) { overall += value - xp.get(skill); xp.put(skill,value); }
    }
    private static Object field(BurrowPlugin p, String name) throws Exception {
        Field f = BurrowPlugin.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(p);
    }
    private static BurrowPlugin instance(World w) throws Exception {
        BurrowPlugin p = new BurrowPlugin();
        Field client = BurrowPlugin.class.getDeclaredField("client");
        client.setAccessible(true);
        client.set(p,w.client);
        ((AtomicBoolean)field(p,"running")).set(true);
        return p;
    }
    private static void tick(BurrowPlugin p, int n) { for(int i=0;i<n;i++) p.onGameTick(new GameTick()); }
    private static void transition(BurrowPlugin p, GameState state) throws Exception {
        Method m = BurrowPlugin.class.getDeclaredMethod("observeXpLoginTransition",GameState.class);
        m.setAccessible(true);
        m.invoke(p,state);
    }
    @Test void pluginReenabledMidSessionMustNotBelieveOldAccountCache() throws Exception {
        World w = new World();
        w.set(Skill.MINING,41); // previous account's plausible XP cached under B's identity
        BurrowPlugin p = instance(w); // plugin has just been enabled, already LOGGED_IN
        tick(p,3);
        assertFalse((boolean)field(p,"xpSynchronized"));
        assertFalse((boolean)field(p,"xpObservedFullLogin"));
        w.set(Skill.MINING,1000); // genuine B cache arrives, NOT an earned gain
        p.onStatChanged(new StatChanged(Skill.MINING,1000,9,9));
        tick(p,3);
        assertFalse((boolean)field(p,"xpSynchronized"));
        assertEquals(0L,((Number)field(p,"sessionEventSequence")).longValue()); // No panel: no startup event IDs
    }
    @Test void witnessedLoginCannotBaselineWhileXpDisabled() throws Exception {
        World w = new World();
        BurrowPlugin p = instance(w);
        transition(p,GameState.LOGIN_SCREEN);
        transition(p,GameState.LOGGING_IN);
        transition(p,GameState.LOGGED_IN);
        tick(p,2);
        assertFalse((boolean)field(p,"xpSynchronized"));
        long before=(long)field(p,"sessionEventSequence");
        w.set(Skill.MINING,5);
        p.onStatChanged(new StatChanged(Skill.MINING,5,1,1));
        assertEquals(before,(long)field(p,"sessionEventSequence"));
    }
    @Test void hopOrStandaloneLoggedInEventNeverGrantsFreshStart() throws Exception {
        World w = new World(); BurrowPlugin p = instance(w);
        transition(p,GameState.HOPPING);
        transition(p,GameState.LOGGED_IN);
        tick(p,3);
        assertFalse((boolean)field(p,"xpSynchronized"));
    }
    @Test void accountSwitchRemainsQuarantinedEvenAfterWitnessedLogin() throws Exception {
        World w = new World(); BurrowPlugin p = instance(w);
        transition(p,GameState.LOGIN_SCREEN);
        transition(p,GameState.LOGGING_IN);
        transition(p,GameState.LOGGED_IN);
        tick(p,2);
        assertFalse((boolean)field(p,"xpSynchronized"));
        w.account=3003L;
        tick(p,1);
        assertTrue((boolean)field(p,"accountXpQuarantined"));
        transition(p,GameState.LOGIN_SCREEN);
        transition(p,GameState.LOGGING_IN);
        transition(p,GameState.LOGGED_IN);
        tick(p,3);
        assertFalse((boolean)field(p,"xpSynchronized"));
    }
}
