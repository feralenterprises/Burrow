package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.ui.BurrowPanel;
import gg.feral.burrow.ui.BurrowPanel.FeedStatus;
import gg.feral.burrow.ui.BurrowPanel.SessionFeed;
import gg.feral.burrow.ui.BurrowPanel.SessionState;
import gg.feral.burrow.ui.BurrowPanel.XpTrackingState;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import org.junit.jupiter.api.Test;

import static gg.feral.burrow.runelite.IronLedgerXpIntegrationJUnitTest.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Build 20 P0: XP tracking defaults ON (production startUp path, simulated by setting the same
 * field), survives teleports / map-chunk LOADING and same-account hops without re-clicking, never
 * counts XP from an unverified interval, and resets on genuine logout. Synthetic client proxy only;
 * NOT native RuneLite evidence.
 */
final class Build20XpDefaultOnJUnitTest {
    static BurrowPlugin defaultOnHost(World w, BurrowPanel panel, boolean witnessedLogin) throws Exception {
        BurrowPlugin p = new BurrowPlugin();
        set(p, "client", w.client); set(p, "panel", panel);
        set(p, "xpTrackingDefaultOn", true); // exactly what startUp() sets in RuneLite
        ((java.util.concurrent.atomic.AtomicBoolean) get(p, "running")).set(true);
        if (witnessedLogin) simulateFreshLogin(p);
        ticks(p, 1); flush();
        return p;
    }
    static void teleport(BurrowPlugin p, World w) throws Exception {
        w.state = GameState.LOADING;
        GameStateChanged e = new GameStateChanged(); e.setGameState(GameState.LOADING);
        p.onGameStateChanged(e); // real event path: LOADING keeps bank/pathfinder, pauses XP
        ticks(p, 1); flush();
    }
    static XpTrackingState state(BurrowPlugin p) throws Exception {
        var m = BurrowPlugin.class.getDeclaredMethod("xpTrackingState"); m.setAccessible(true);
        return (XpTrackingState) m.invoke(p);
    }

    @Test void freshLoginTracksWithoutAnyClickButOnlyAfterBaseline() throws Exception {
        World w = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin p = defaultOnHost(w, panel, true);
        assertTrue((boolean) get(p, "sessionXpUserArmed"), "armed automatically from default-ON intent");
        w.earn(Skill.MINING, 40); w.notify(p, Skill.MINING); // pre-baseline: must not count
        ticks(p, 3); flush();
        assertTrue((boolean) get(p, "xpSynchronized"));
        assertEquals(XpTrackingState.ACTIVE, state(p));
        assertEquals(XpTrackingState.ACTIVE, panel.xpTrackingState(), "panel shows TRACKING ACTIVE");
        assertEquals(0L, panel.sessionReadout().totalXp(), "no fabricated pre-baseline XP");
        w.earn(Skill.MINING, 35); w.notify(p, Skill.MINING); flush();
        assertEquals(35L, panel.sessionReadout().totalXp());
    }

    @Test void twoTeleportsAndChunkLoadsKeepTotalsAndAutoRearmWithoutCountingGaps() throws Exception {
        World w = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin p = defaultOnHost(w, panel, true);
        ticks(p, 3);
        w.earn(Skill.ATTACK, 100); w.notify(p, Skill.ATTACK); flush();
        assertEquals(100L, panel.sessionReadout().totalXp());
        for (int hop = 0; hop < 3; hop++) {
            teleport(p, w);
            assertEquals(XpTrackingState.PAUSED, state(p), "pause, not 'turned off'");
            assertEquals(SessionState.SUSPENDED, panel.sessionReadout().state());
            assertEquals(100L + hop * 10L, panel.sessionReadout().totalXp(), "totals kept across LOADING");
            w.earn(Skill.ATTACK, 3); w.notify(p, Skill.ATTACK); // XP observed during LOADING: ignored
            w.state = GameState.LOGGED_IN; ticks(p, 1); flush();
            w.notify(p, Skill.ATTACK); flush(); // first return event: never a gain
            assertEquals(100L + hop * 10L, panel.sessionReadout().totalXp());
            assertNotEquals(XpTrackingState.OFF, state(p), "no START XP TRACKING after a teleport");
            ticks(p, 3); flush();
            assertEquals(XpTrackingState.ACTIVE, state(p), "re-armed and re-baselined automatically");
            w.earn(Skill.ATTACK, 10); w.notify(p, Skill.ATTACK); flush();
            assertEquals(110L + hop * 10L, panel.sessionReadout().totalXp(), "later real XP counted");
        }
        assertEquals(130L, panel.sessionReadout().xpBySkill().get("Attack"));
    }

    @Test void unstableCacheAfterReturnNeverBaselinesUntilStable() throws Exception {
        World w = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin p = defaultOnHost(w, panel, true);
        ticks(p, 3);
        teleport(p, w);
        w.state = GameState.LOGGED_IN;
        for (int i = 0; i < 4; i++) { w.earn(Skill.FISHING, 50); ticks(p, 1); } // changing every tick
        assertFalse((boolean) get(p, "xpSynchronized"), "changing XP table is not a baseline");
        assertEquals(XpTrackingState.SYNCING, state(p));
        ticks(p, 3); flush();
        assertTrue((boolean) get(p, "xpSynchronized"));
        assertEquals(0L, panel.sessionReadout().totalXp(), "the unstable 200 XP was never counted");
    }

    @Test void midSessionPluginStartStaysUnavailableAndNeverCounts() throws Exception {
        World w = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin p = defaultOnHost(w, panel, false); // enabled while already logged in
        ticks(p, 4);
        assertFalse((boolean) get(p, "sessionXpUserArmed"));
        assertEquals(XpTrackingState.UNAVAILABLE, state(p), "intent ON but provenance unproven");
        w.earn(Skill.MINING, 500); w.notify(p, Skill.MINING); ticks(p, 3); flush();
        assertEquals(0L, panel.sessionReadout().totalXp());
        assertEquals(FeedStatus.UNAVAILABLE, panel.sessionReadout().feeds().get(SessionFeed.XP));
    }

    @Test void logoutResetsImmediatelyAndAccountSwitchNeverLeaks() throws Exception {
        World w = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin p = defaultOnHost(w, panel, true);
        ticks(p, 3);
        w.earn(Skill.SLAYER, 250); w.notify(p, Skill.SLAYER); flush();
        assertEquals(250L, panel.sessionReadout().totalXp());
        w.state = GameState.LOGIN_SCREEN; ticks(p, 1); flush();
        assertEquals(SessionState.OFFLINE, panel.sessionReadout().state(), "genuine logout retires immediately");
        assertEquals(0L, panel.sessionReadout().totalXp());
        assertFalse((boolean) get(p, "xpIntent"), "intent retired with the login");
        assertEquals(XpTrackingState.SIGNED_OUT, state(p));
        // Account B logs in with A's stale cache values plus a jump.
        w.account = 222L; simulateFreshLogin(p); w.state = GameState.LOGGED_IN;
        w.earn(Skill.SLAYER, 999);
        ticks(p, 4); w.notify(p, Skill.SLAYER); flush();
        assertTrue((boolean) get(p, "accountXpQuarantined"), "existing R05 quarantine preserved");
        assertEquals(XpTrackingState.UNAVAILABLE, state(p));
        assertEquals(0L, panel.sessionReadout().totalXp(), "no A XP and no fabricated B XP");
        // Back to A in the same plugin instance: still quarantined, nothing leaks.
        w.state = GameState.LOGIN_SCREEN; ticks(p, 1); w.account = 111L; simulateFreshLogin(p);
        w.state = GameState.LOGGED_IN; ticks(p, 4); w.earn(Skill.SLAYER, 5); w.notify(p, Skill.SLAYER); flush();
        assertEquals(0L, panel.sessionReadout().totalXp());
    }

    @Test void sameAccountReloginDefaultsOnAgain() throws Exception {
        World w = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin p = defaultOnHost(w, panel, true);
        ticks(p, 3);
        w.state = GameState.LOGIN_SCREEN; ticks(p, 1); flush();
        simulateFreshLogin(p); w.state = GameState.LOGGED_IN; ticks(p, 4); flush();
        assertTrue((boolean) get(p, "xpIntent"));
        assertEquals(XpTrackingState.ACTIVE, state(p));
        w.earn(Skill.COOKING, 12); w.notify(p, Skill.COOKING); flush();
        assertEquals(12L, panel.sessionReadout().totalXp(), "new login starts at zero, then tracks");
    }

    @Test void legacyHostWithoutStartUpKeepsExplicitlyDisabledContract() throws Exception {
        World w = new World(); BurrowPanel panel = new BurrowPanel(new BurrowModel());
        BurrowPlugin p = host(w, panel, true); // Build 19 fixture: no startUp()
        ticks(p, 4); flush();
        assertFalse((boolean) get(p, "sessionXpUserArmed"));
        assertEquals(XpTrackingState.OFF, state(p));
    }
}
