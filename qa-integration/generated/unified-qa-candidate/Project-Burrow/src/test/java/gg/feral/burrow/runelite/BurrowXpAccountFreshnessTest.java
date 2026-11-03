package gg.feral.burrow.runelite;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** R04: source-level account XP cache crossing; never interprets synthetic data as live. */
public final class BurrowXpAccountFreshnessTest {
    private static final class FakeWorld {
        final EnumMap<Skill,Integer> xp = new EnumMap<>(Skill.class);
        long accountHash = 11L;
        GameState state = GameState.LOGGED_IN;
        long overall = 1154L;
        final Client client = (Client)Proxy.newProxyInstance(Client.class.getClassLoader(),
                new Class<?>[]{Client.class}, (p,m,a) -> switch (m.getName()) {
                    case "getAccountHash" -> accountHash;
                    case "getGameState" -> state;
                    case "getSkillExperience" -> xp.get((Skill)a[0]);
                    case "getRealSkillLevel" -> Math.min(99, Experience.getLevelForXp(xp.get((Skill)a[0])));
                    case "getOverallExperience" -> overall;
                    case "toString" -> "synthetic-test-client";
                    default -> throw new AssertionError("Unexpected API: " + m.getName());
                });
        FakeWorld() {
            for (Skill skill : Skill.values()) if (skill != Skill.OVERALL)
                xp.put(skill, skill == Skill.HITPOINTS ? 1154 : 0);
        }
        void set(Skill skill,int next) {
            overall += next - xp.get(skill);
            xp.put(skill,next);
        }
        void event(BurrowPlugin plugin,Skill skill) {
            int x=xp.get(skill);
            plugin.onStatChanged(new StatChanged(skill,x,Experience.getLevelForXp(x),Experience.getLevelForXp(x)));
        }
        void tick(BurrowPlugin plugin,int times) {
            for(int i=0;i<times;i++) plugin.onGameTick(new GameTick());
        }
    }
    private static Object field(BurrowPlugin plugin, String name) throws Exception {
        Field f=BurrowPlugin.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(plugin);
    }
    private static BurrowPlugin started(FakeWorld world) throws Exception {
        BurrowPlugin p=new BurrowPlugin();
        Field f=BurrowPlugin.class.getDeclaredField("client");
        f.setAccessible(true);
        f.set(p,world.client);
        f=BurrowPlugin.class.getDeclaredField("running");
        f.setAccessible(true);
        ((AtomicBoolean)f.get(p)).set(true);
        java.lang.reflect.Method transition = BurrowPlugin.class.getDeclaredMethod(
            "observeXpLoginTransition", GameState.class);
        transition.setAccessible(true);
        transition.invoke(p, GameState.LOGIN_SCREEN);
        transition.invoke(p, GameState.LOGGING_IN);
        transition.invoke(p, GameState.LOGGED_IN);
        return p;
    }
    @Test void staleMatchingEventFromPreviousAccountDoesNotBaselineOrEmitXp() throws Exception {
        FakeWorld w=new FakeWorld(); BurrowPlugin p=started(w);
        w.tick(p,2); assertFalse((boolean)field(p,"xpSynchronized"));
        Object scopeA=field(p,"sessionScope");
        w.accountHash=22L;
        w.tick(p,1);
        assertNotSame(scopeA,field(p,"sessionScope"));
        assertFalse((boolean)field(p,"xpSynchronized"));
        long start=(long)field(p,"sessionEventSequence");
        w.event(p,Skill.MINING); // stale previous-account callback and cache
        w.tick(p,4);
        assertFalse((boolean)field(p,"xpSynchronized"));
        assertEquals(start,(long)field(p,"sessionEventSequence"));
        assertFalse((boolean)field(p,"freshLoginStatObserved"));
        w.set(Skill.MINING,1000); // no authenticated cache-ownership witness
        w.event(p,Skill.MINING);
        w.tick(p,2);
        assertFalse((boolean)field(p,"xpSynchronized"));
        long ready=(long)field(p,"sessionEventSequence");
        w.set(Skill.MINING,1025); // even apparently real XP is unavailable
        w.event(p,Skill.MINING);
        assertEquals(ready,(long)field(p,"sessionEventSequence"));
        assertTrue((boolean)field(p,"accountXpQuarantined"));
    }
    @Test void aToBToARejectsPreviousCacheAndPreservesOpaqueScopeIsolation() throws Exception {
        FakeWorld w=new FakeWorld(); BurrowPlugin p=started(w);
        w.tick(p,2); Object scopeA=field(p,"sessionScope");
        w.accountHash=22L; w.tick(p,1);
        w.event(p,Skill.MINING); w.tick(p,3);
        assertFalse((boolean)field(p,"xpSynchronized"));
        w.set(Skill.MINING,2000); w.event(p,Skill.MINING); w.tick(p,2);
        assertFalse((boolean)field(p,"xpSynchronized"));
        Object scopeB=field(p,"sessionScope");
        w.accountHash=11L; w.tick(p,1);
        Object scopeA2=field(p,"sessionScope");
        assertNotSame(scopeA,scopeA2);
        assertNotSame(scopeB,scopeA2);
        w.event(p,Skill.MINING); w.tick(p,3); // B's stale XP still in the cache
        assertFalse((boolean)field(p,"xpSynchronized"));
        w.set(Skill.MINING,0); w.event(p,Skill.MINING); w.tick(p,2);
        assertFalse((boolean)field(p,"xpSynchronized"));
        assertTrue((boolean)field(p,"accountXpQuarantined"));
        assertNotSame(scopeB,field(p,"sessionScope"));
    }
    @Test void witnessedFreshLoginDoesNotEnableDisabledFeed() throws Exception {
        FakeWorld w=new FakeWorld(); BurrowPlugin p=started(w);
        w.tick(p,2);
        assertFalse((boolean)field(p,"xpSynchronized"));
        long before=(long)field(p,"sessionEventSequence");
        w.set(Skill.FISHING,8);w.event(p,Skill.FISHING);
        assertEquals(before,(long)field(p,"sessionEventSequence"));
    }
    @Test void noPreviousAuthenticatedXpMustFailClosedOnNewAccount() throws Exception {
        FakeWorld w=new FakeWorld(); BurrowPlugin p=started(w);
        w.tick(p,1); // create A but don't establish baseline
        w.accountHash=22L; w.tick(p,1);
        w.set(Skill.MINING,900);
        w.event(p,Skill.MINING);w.tick(p,5);
        assertFalse((boolean)field(p,"xpSynchronized"));
    }
    @Test void changedOldAccountCacheCannotClaimNewLoginAndQuarantinePersists() throws Exception {
        FakeWorld w=new FakeWorld(); BurrowPlugin p=started(w);
        w.tick(p,2);
        Object scopeA=field(p,"sessionScope");
        w.set(Skill.MINING,40); w.event(p,Skill.MINING);
        w.accountHash=22L; w.tick(p,1);
        Object scopeB=field(p,"sessionScope");
        assertNotSame(scopeA,scopeB);
        assertTrue((boolean)field(p,"accountXpQuarantined"));
        long count=(long)field(p,"sessionEventSequence");
        w.set(Skill.MINING,41); w.event(p,Skill.MINING); w.tick(p,4);
        assertFalse((boolean)field(p,"xpSynchronized"));
        w.set(Skill.MINING,1000); w.event(p,Skill.MINING); w.tick(p,4);
        assertFalse((boolean)field(p,"xpSynchronized"));
        assertEquals(count,(long)field(p,"sessionEventSequence"));
        w.accountHash=11L; w.tick(p,1); // B->A must not bypass quarantine
        assertTrue((boolean)field(p,"accountXpQuarantined"));
        w.event(p,Skill.MINING); w.tick(p,4);
        assertFalse((boolean)field(p,"xpSynchronized"));
        assertNotSame(scopeA,field(p,"sessionScope"));
        assertNotSame(scopeB,field(p,"sessionScope"));
    }

    @Test void identicalAccountXpVectorsRemainUnavailableWithoutFabricatingGain() throws Exception {
        FakeWorld w=new FakeWorld(); BurrowPlugin p=started(w);
        w.tick(p,2);
        w.accountHash=22L; w.tick(p,1);
        assertTrue((boolean)field(p,"accountXpQuarantined"));
        w.event(p,Skill.MINING); w.tick(p,4);
        long count=(long)field(p,"sessionEventSequence");
        assertFalse((boolean)field(p,"xpSynchronized"));
        w.set(Skill.MINING,5); w.event(p,Skill.MINING); w.tick(p,3);
        assertFalse((boolean)field(p,"xpSynchronized"));
        assertEquals(count,(long)field(p,"sessionEventSequence"));
    }

}
