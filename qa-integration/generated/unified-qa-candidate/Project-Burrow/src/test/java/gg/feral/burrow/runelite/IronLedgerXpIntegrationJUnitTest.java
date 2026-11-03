package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.IronLedgerXpBaselines;
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

/** Isolated offline provenance and functional checks for the opt-in Iron Ledger XP import.
 * These fixtures do not attest RuneLite account-bound cache provenance in native gameplay.
 */
public final class IronLedgerXpIntegrationJUnitTest {
    static final class World {
        final EnumMap<Skill,Integer> skills = new EnumMap<>(Skill.class);
        GameState state = GameState.LOGGED_IN;
        long account = 111L;
        long overall = 1154L;
        World() {
            for (Skill s : Skill.values()) skills.put(s, s == Skill.HITPOINTS ? 1154 : 0);
        }
        final Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
                new Class<?>[]{Client.class}, (proxy, method, args) -> switch(method.getName()) {
                    case "getGameState" -> state;
                    case "getAccountHash" -> account;
                    case "getSkillExperience" -> skills.get((Skill)args[0]);
                    case "getRealSkillLevel" -> Experience.getLevelForXp(skills.get((Skill)args[0]));
                    case "getOverallExperience" -> overall;
                    case "toString" -> "synthetic-world";
                    default -> throw new AssertionError("Unexpected client API: " + method.getName());
                });
        void earn(Skill s, int delta) { skills.put(s, skills.get(s)+delta); overall+=delta; }
        void notify(BurrowPlugin p, Skill s) {
            int xp=skills.get(s), level=Experience.getLevelForXp(xp);
            p.onStatChanged(new StatChanged(s, xp, level, level));
        }
    }
    static Object get(BurrowPlugin p, String field) throws Exception {
        Field f=BurrowPlugin.class.getDeclaredField(field); f.setAccessible(true); return f.get(p);
    }
    static void set(BurrowPlugin p, String field, Object value) throws Exception {
        Field f=BurrowPlugin.class.getDeclaredField(field); f.setAccessible(true); f.set(p,value);
    }
    static void invoke(BurrowPlugin p, String method) throws Exception {
        Method m=BurrowPlugin.class.getDeclaredMethod(method); m.setAccessible(true); m.invoke(p);
    }
    static void simulateFreshLogin(BurrowPlugin p) throws Exception {
        Method m=BurrowPlugin.class.getDeclaredMethod("observeXpLoginTransition", GameState.class);
        m.setAccessible(true);
        m.invoke(p,GameState.LOGIN_SCREEN);
        m.invoke(p,GameState.LOGGING_IN);
        m.invoke(p,GameState.LOGGED_IN);
    }
    static void ticks(BurrowPlugin p,int n) { for(int i=0;i<n;i++)p.onGameTick(new GameTick()); }
    static void flush() throws Exception { SwingUtilities.invokeAndWait(()->{}); }
    static BurrowPlugin host(World w, BurrowPanel panel, boolean fresh) throws Exception {
        BurrowPlugin p=new BurrowPlugin();
        set(p,"client",w.client);set(p,"panel",panel);
        ((AtomicBoolean)get(p,"running")).set(true);
        if(fresh) simulateFreshLogin(p);
        ticks(p,1);flush();
        return p;
    }
    @Test void freshUserOptInTracksOnlyPostBaselineGainsAcrossSkillsAndIgnoresDuplicates() throws Exception {
        World w=new World(); BurrowPanel panel=new BurrowPanel(new BurrowModel());
        BurrowPlugin p=host(w,panel,true);
        assertFalse((boolean)get(p,"sessionXpUserArmed"));
        invoke(p,"requestExperimentalXpForCurrentLogin");
        assertTrue((boolean)get(p,"sessionXpUserArmed"));
        // This pre-baseline event is ignored; it must not count as earned XP.
        w.earn(Skill.MINING, 40); w.notify(p,Skill.MINING);
        ticks(p,3);flush();
        assertTrue((boolean)get(p,"xpSynchronized"));
        assertEquals(0L,panel.sessionReadout().totalXp());
        assertEquals(FeedStatus.AVAILABLE,panel.sessionReadout().feeds().get(SessionFeed.XP));
        w.earn(Skill.MINING, 35);w.notify(p,Skill.MINING);
        w.notify(p,Skill.MINING);
        w.earn(Skill.FISHING, 25);w.notify(p,Skill.FISHING);
        flush();
        assertEquals(60L,panel.sessionReadout().totalXp());
        assertEquals(35L,panel.sessionReadout().xpBySkill().get("Mining"));
        assertEquals(25L,panel.sessionReadout().xpBySkill().get("Fishing"));
        // A stale lower reading, even if the client table temporarily regresses,
        // never manufactures a later positive gain.
        w.earn(Skill.MINING,-20);w.notify(p,Skill.MINING);
        w.earn(Skill.MINING,20);w.notify(p,Skill.MINING);
        flush();
        assertEquals(60L,panel.sessionReadout().totalXp());
    }
    @Test void midSessionPluginStartAndAccountSwapCannotArmOrForgeXp() throws Exception {
        World w=new World(); BurrowPanel panel=new BurrowPanel(new BurrowModel());
        BurrowPlugin p=host(w,panel,false);
        invoke(p,"requestExperimentalXpForCurrentLogin");
        assertFalse((boolean)get(p,"sessionXpUserArmed"));
        simulateFreshLogin(p);invoke(p,"requestExperimentalXpForCurrentLogin");
        ticks(p,2);flush();
        assertTrue((boolean)get(p,"xpSynchronized"));
        w.account=222L;ticks(p,1);flush();
        assertFalse((boolean)get(p,"sessionXpUserArmed"));
        assertTrue((boolean)get(p,"accountXpQuarantined"));
        invoke(p,"requestExperimentalXpForCurrentLogin");
        assertFalse((boolean)get(p,"sessionXpUserArmed"));
        w.earn(Skill.MINING,1000);w.notify(p,Skill.MINING);ticks(p,3);flush();
        assertEquals(0L,panel.sessionReadout().totalXp());
        assertEquals(FeedStatus.UNAVAILABLE,panel.sessionReadout().feeds().get(SessionFeed.XP));
    }
    // Build 20 policy change (owner): a same-login hop pauses XP fail-closed, keeps the login's
    // totals and intent, then re-baselines automatically. Strengthened: XP earned during the
    // unverified hop interval is NEVER counted; only post-new-baseline gains are.
    @Test void terminalLogoutResetsAndHopPausesThenRebaselinesWithoutCountingTheGap() throws Exception {
        World w=new World(); BurrowPanel panel=new BurrowPanel(new BurrowModel());
        BurrowPlugin p=host(w,panel,true);
        invoke(p,"requestExperimentalXpForCurrentLogin");ticks(p,2);
        w.earn(Skill.WOODCUTTING,10);w.notify(p,Skill.WOODCUTTING);flush();
        assertEquals(10L,panel.sessionReadout().totalXp());
        w.state=GameState.HOPPING;ticks(p,1);flush();
        assertFalse((boolean)get(p,"sessionXpUserArmed"),"fail closed during the hop");
        w.earn(Skill.WOODCUTTING,7);w.notify(p,Skill.WOODCUTTING);flush();
        w.state=GameState.LOGGED_IN;ticks(p,3);
        w.notify(p,Skill.WOODCUTTING);flush();
        assertEquals(10L,panel.sessionReadout().totalXp(),"gap XP and the first return event are not gains");
        w.earn(Skill.WOODCUTTING,5);w.notify(p,Skill.WOODCUTTING);flush();
        assertEquals(15L,panel.sessionReadout().totalXp(),"real post-baseline gain counted, prior total kept");
        w.state=GameState.LOGIN_SCREEN;ticks(p,1);flush();
        assertEquals(SessionState.OFFLINE,panel.sessionReadout().state());
        assertEquals(0L,panel.sessionReadout().totalXp());
    }
    @Test void actualSessionsButtonRoutesToRegisteredHostActionWithoutFakeData() throws Exception {
        BurrowModel model=new BurrowModel();
        BurrowPanel panel=new BurrowPanel(model);
        java.util.concurrent.atomic.AtomicInteger clicks=new java.util.concurrent.atomic.AtomicInteger();
        panel.setXpStartAction(clicks::incrementAndGet);
        // Actual per-login Sessions start and an offline plugin panel render are separate.
        BurrowPanel.SessionScope scope=new BurrowPanel.SessionScope("synthetic-account","login-epoch");
        SwingUtilities.invokeAndWait(() -> {
            panel.ingestSessionEvent(new BurrowPanel.SessionStart(
                scope,"start",BurrowPanel.SessionOrigin.SYNTHETIC_FIXTURE));
            // The live host explicitly publishes OFF before the Sessions card
            // presents its START action; null means the host has not reported a state.
            panel.setXpTrackingState(BurrowPanel.XpTrackingState.OFF);
        });
        model.show(BurrowModel.View.SESSIONS);
        // Old pre-R13 position now falls in the Bossing Summary, not XP.
        panel.clickAt(25,-8+262);
        assertEquals(0,clicks.get(),"the old Sessions coordinate cannot arm XP");
        int paintedXpActionY=-8+128+38; // R13 card: CONTENT_SHIFT + SESSION_XP_TOP + caption
        panel.clickAt(25,paintedXpActionY);
        assertEquals(1,clicks.get(),"the painted Sessions XP button must invoke the action");
        assertEquals(0L,panel.sessionReadout().totalXp(),"button never fabricates XP");
        SwingUtilities.invokeAndWait(() -> panel.setXpTrackingState(
            BurrowPanel.XpTrackingState.UNAVAILABLE));
        panel.clickAt(25,paintedXpActionY);
        assertEquals(1,clicks.get(),"unavailable state cannot arm XP");
        SwingUtilities.invokeAndWait(() -> {
            panel.ingestSessionEvent(new BurrowPanel.SessionStop(
                scope,"logout",BurrowPanel.SessionOrigin.SYNTHETIC_FIXTURE));
            panel.setXpTrackingState(BurrowPanel.XpTrackingState.OFF);
        });
        panel.clickAt(25,paintedXpActionY);
        assertEquals(1,clicks.get(),"offline button cannot arm another session");
    }
    @Test void originalIronLedgerBaselineNeverCountsARegressiveOrStaleSample() {
        IronLedgerXpBaselines b=new IronLedgerXpBaselines();
        b.transition("one");b.seed("one","Mining",500);
        assertEquals(0,b.observe("one","Mining",480));
        assertEquals(0,b.observe("two","Mining",1000));
        assertEquals(0,b.observe("one","Mining",500));
        assertEquals(7,b.observe("one","Mining",507));
        b.transition("two");
        assertEquals(0,b.observe("two","Mining",1000));
        assertEquals(3,b.observe("two","Mining",1003));
    }
}
