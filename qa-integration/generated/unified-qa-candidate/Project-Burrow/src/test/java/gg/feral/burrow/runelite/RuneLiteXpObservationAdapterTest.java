package gg.feral.burrow.runelite;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Synthetic offline event-fixture replay; NOT a native RuneLite client verification. */
public final class RuneLiteXpObservationAdapterTest {
    private static final class Feed {
        private final EnumMap<Skill, Integer> latest = new EnumMap<>(Skill.class);
        private GameState game = GameState.LOGGED_IN;
        private boolean synchronizedXp = true;
        private final Client client = (Client) Proxy.newProxyInstance(
            Client.class.getClassLoader(), new Class<?>[]{Client.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getGameState")) return game;
                if (method.getName().equals("getSkillExperience"))
                    return latest.getOrDefault((Skill) args[0], -1);
                if (method.getName().equals("toString")) return "synthetic-client";
                throw new AssertionError("unexpected Client method " + method.getName());
            });

        Optional<RuneLiteXpObservationAdapter.XpDelta> event(
                RuneLiteXpObservationAdapter adapter, Object scope, long fence,
                Skill skill, int eventXp, int cachedXp, int level, int boosted) {
            latest.put(skill, cachedXp);
            return adapter.onStatChanged(scope, fence,
                new StatChanged(skill, eventXp, level, boosted), client, synchronizedXp);
        }
        Optional<RuneLiteXpObservationAdapter.XpDelta> event(
                RuneLiteXpObservationAdapter adapter, Object scope, long fence,
                Skill skill, int xp) {
            return event(adapter, scope, fence, skill, xp, xp, 1, 1);
        }
    }

    @Test void seededAuthoritativeSnapshotCountsFirstGameplayGainAndZeroSkill() {
        var adapter = new RuneLiteXpObservationAdapter();
        var feed = new Feed();
        Object scope = new Object();
        long fence = adapter.beginVerifiedLogin(scope);
        for (Skill skill : Skill.values()) {
            if (skill != Skill.OVERALL) feed.latest.put(skill, skill == Skill.PRAYER ? 0 : 100);
        }
        feed.synchronizedXp = false;
        assertFalse(adapter.seedVerifiedClientSnapshot(scope,fence,feed.client,feed.synchronizedXp));
        feed.synchronizedXp = true;
        assertTrue(adapter.seedVerifiedClientSnapshot(scope,fence,feed.client,feed.synchronizedXp));
        assertFalse(adapter.seedVerifiedClientSnapshot(scope,fence,feed.client,true));
        assertEquals(12L,feed.event(adapter,scope,fence,Skill.ATTACK,112).orElseThrow().delta());
        assertEquals(2L,feed.event(adapter,scope,fence,Skill.PRAYER,2).orElseThrow().delta());
        long pause = adapter.pauseTransient(scope);
        long resumed = adapter.resumeVerifiedSameLogin(scope);
        assertTrue(resumed > pause);
        assertTrue(adapter.seedVerifiedClientSnapshot(scope,resumed,feed.client,true));
        assertEquals(5L,feed.event(adapter,scope,resumed,Skill.ATTACK,117).orElseThrow().delta());
    }

    @Test void snapshotMustBeCompleteAndCannotUseUnsupportedXp() {
        var adapter = new RuneLiteXpObservationAdapter();
        var feed = new Feed();
        Object scope = new Object();
        long fence = adapter.beginVerifiedLogin(scope);
        for (Skill skill : Skill.values()) if (skill != Skill.OVERALL) feed.latest.put(skill, 50);
        feed.latest.put(Skill.MAGIC, -1);
        assertFalse(adapter.seedVerifiedClientSnapshot(scope,fence,feed.client,true));
        feed.latest.put(Skill.MAGIC, 50);
        assertTrue(adapter.seedVerifiedClientSnapshot(scope,fence,feed.client,true));
        assertEquals(1L,feed.event(adapter,scope,fence,Skill.MAGIC,51).orElseThrow().delta());
    }

    @Test void firstBaselineDuplicateBoostAndTwoSkills() {
        var adapter = new RuneLiteXpObservationAdapter();
        var feed = new Feed();
        Object scope = new Object();
        long fence = adapter.beginVerifiedLogin(scope);
        assertTrue(feed.event(adapter,scope,fence,Skill.ATTACK,4500).isEmpty());
        assertTrue(feed.event(adapter,scope,fence,Skill.PRAYER,0).isEmpty());
        assertTrue(feed.event(adapter,scope,fence,Skill.ATTACK,4500,4500,21,35).isEmpty());
        var attack = feed.event(adapter,scope,fence,Skill.ATTACK,4530).orElseThrow();
        assertEquals(Skill.ATTACK,attack.skill());
        assertEquals(30L,attack.delta());
        assertEquals(4500L,attack.previousXp());
        assertEquals(4530L,attack.currentXp());
        assertEquals(fence,attack.observationFence());
        assertFalse(attack.sourceEvidence().isBlank());
        assertTrue(feed.event(adapter,scope,fence,Skill.ATTACK,4530).isEmpty());
        assertEquals(10L,feed.event(adapter,scope,fence,Skill.PRAYER,10).orElseThrow().delta());
    }

    @Test void initialLoginFloodNeverCountsAndOverallIgnored() {
        var adapter = new RuneLiteXpObservationAdapter();
        var feed = new Feed();
        Object scope = new Object();
        long fence = adapter.beginVerifiedLogin(scope);
        int eligible = 0;
        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL) {
                assertTrue(feed.event(adapter,scope,fence,skill,999_999).isEmpty());
            } else {
                eligible++;
                assertTrue(feed.event(adapter,scope,fence,skill,1000+eligible).isEmpty());
            }
        }
        assertTrue(eligible >= 20);
        assertEquals(3L,feed.event(adapter,scope,fence,Skill.ATTACK,1004).orElseThrow().delta());
    }

    @Test void unreadinessOutOfDateEventAndOfflineAreFailClosed() {
        var adapter = new RuneLiteXpObservationAdapter();
        var feed = new Feed();
        Object scope = new Object();
        long fence = adapter.beginVerifiedLogin(scope);
        feed.synchronizedXp=false;
        assertTrue(feed.event(adapter,scope,fence,Skill.MAGIC,0).isEmpty());
        feed.synchronizedXp=true;
        assertTrue(feed.event(adapter,scope,fence,Skill.MAGIC,1500).isEmpty());
        assertTrue(feed.event(adapter,scope,fence,Skill.MAGIC,1520,1530,1,1).isEmpty());
        feed.game = GameState.LOGIN_SCREEN;
        assertTrue(feed.event(adapter,scope,fence,Skill.MAGIC,1530).isEmpty());
        feed.game = GameState.LOGGED_IN;
        assertEquals(30L,feed.event(adapter,scope,fence,Skill.MAGIC,1530).orElseThrow().delta());
        assertTrue(feed.event(adapter,scope,fence,Skill.MAGIC,-1).isEmpty());
    }

    @Test void backwardReadQuarantinesUntilAnotherBaseline() {
        var adapter = new RuneLiteXpObservationAdapter();
        var feed = new Feed();
        Object scope = new Object();
        long fence = adapter.beginVerifiedLogin(scope);
        assertTrue(feed.event(adapter,scope,fence,Skill.WOODCUTTING,200).isEmpty());
        assertEquals(20L,feed.event(adapter,scope,fence,Skill.WOODCUTTING,220).orElseThrow().delta());
        assertTrue(feed.event(adapter,scope,fence,Skill.WOODCUTTING,90).isEmpty());
        assertTrue(feed.event(adapter,scope,fence,Skill.WOODCUTTING,230).isEmpty());
        assertEquals(5L,feed.event(adapter,scope,fence,Skill.WOODCUTTING,235).orElseThrow().delta());
    }

    @Test void transientHopLoadingAndReconnectKeepSessionButRebaseline() {
        var adapter = new RuneLiteXpObservationAdapter();
        var feed = new Feed();
        Object scope = new Object();
        long first = adapter.beginVerifiedLogin(scope);
        assertTrue(feed.event(adapter,scope,first,Skill.MINING,100).isEmpty());
        assertEquals(10L,feed.event(adapter,scope,first,Skill.MINING,110).orElseThrow().delta());
        long loading = adapter.pauseTransient(scope);
        assertTrue(adapter.beginVerifiedLogin(scope)==loading); // repeated login event does not wipe
        assertTrue(feed.event(adapter,scope,first,Skill.MINING,115).isEmpty());
        assertTrue(feed.event(adapter,scope,loading,Skill.MINING,115).isEmpty());
        long returned = adapter.resumeVerifiedSameLogin(scope);
        assertTrue(returned > loading);
        assertTrue(feed.event(adapter,scope,first,Skill.MINING,115).isEmpty());
        assertTrue(feed.event(adapter,scope,returned,Skill.MINING,115).isEmpty());
        assertEquals(5L,feed.event(adapter,scope,returned,Skill.MINING,120).orElseThrow().delta());
        long hopping = adapter.pauseTransient(scope);
        assertEquals(hopping,adapter.pauseTransient(scope));
        long resumed = adapter.resumeVerifiedSameLogin(scope);
        assertTrue(feed.event(adapter,scope,resumed,Skill.MINING,180).isEmpty());
        assertEquals(1L,feed.event(adapter,scope,resumed,Skill.MINING,181).orElseThrow().delta());
    }

    @Test void accountAtoBtoAStaleFenceAndConfirmedLogout() {
        var adapter = new RuneLiteXpObservationAdapter();
        var feed = new Feed();
        Object a1 = new Object(), b = new Object(), a2 = new Object();
        long aFence = adapter.beginVerifiedLogin(a1);
        assertTrue(feed.event(adapter,a1,aFence,Skill.FISHING,100).isEmpty());
        long bFence = adapter.beginVerifiedLogin(b);
        assertTrue(bFence>aFence);
        assertTrue(feed.event(adapter,a1,aFence,Skill.FISHING,500).isEmpty());
        assertTrue(feed.event(adapter,b,bFence,Skill.FISHING,500).isEmpty());
        assertEquals(7L,feed.event(adapter,b,bFence,Skill.FISHING,507).orElseThrow().delta());
        assertEquals(0L,adapter.beginVerifiedLogin(a1)); // retired token cannot be resurrected
        adapter.confirmedLogout(b);
        assertFalse(adapter.accepting(b,bFence));
        long reentryFence = adapter.beginVerifiedLogin(a2);
        assertTrue(feed.event(adapter,a2,reentryFence,Skill.FISHING,507).isEmpty());
        assertTrue(feed.event(adapter,b,bFence,Skill.FISHING,600).isEmpty());
        assertEquals(3L,feed.event(adapter,a2,reentryFence,Skill.FISHING,510).orElseThrow().delta());
        adapter.pluginStop();
        assertTrue(feed.event(adapter,a2,reentryFence,Skill.FISHING,510).isEmpty());
        assertEquals(0L,adapter.beginVerifiedLogin(a2));
    }

    @Test void sameScopeIdempotencyAndUnavailableSkillDeltaNotInvented() {
        var adapter = new RuneLiteXpObservationAdapter();
        var feed = new Feed();
        Object scope = new Object();
        long fence = adapter.beginVerifiedLogin(scope);
        assertTrue(feed.event(adapter,scope,fence,Skill.AGILITY,10).isEmpty());
        assertEquals(fence,adapter.beginVerifiedLogin(scope));
        assertEquals(4L,feed.event(adapter,scope,fence,Skill.AGILITY,14).orElseThrow().delta());
        assertTrue(feed.event(adapter,new Object(),fence,Skill.AGILITY,100).isEmpty());
        assertEquals(0L,adapter.pauseTransient(new Object()));
        assertEquals(0L,adapter.resumeVerifiedSameLogin(new Object()));
        assertTrue(adapter.accepting(scope,fence));
    }
}
