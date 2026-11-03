package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.ui.BurrowPanel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Synthetic host fixture tests only; genuine RuneLite integration still unverified. */
public final class IronLedgerBossLootHostJUnitTest {
    private static class World {
        long account = 42L;
        GameState state = GameState.LOGGED_IN;
        final Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
            new Class<?>[]{Client.class}, (proxy, method, args) -> switch(method.getName()) {
                case "getGameState" -> state;
                case "getAccountHash" -> account;
                case "getItemDefinition" -> null;
                    case "getRealSkillLevel" -> 1; // Optional Pathfinder snapshot; no XP/KC entitlement.
                default -> throw new AssertionError("Unexpected call: " + method.getName());
            });
    }
    private static Field field(String name) throws Exception {
        Field f=BurrowPlugin.class.getDeclaredField(name);f.setAccessible(true);return f;
    }
    private static BurrowPlugin host(World world,BurrowPanel panel) throws Exception {
        BurrowPlugin h=new BurrowPlugin();
        field("client").set(h,world.client);
        field("panel").set(h,panel);
        ((AtomicBoolean)field("running").get(h)).set(true);
        return h;
    }
    private static NpcLootReceived event(String npc,int itemId,int qty) {
        NPC source=(NPC)Proxy.newProxyInstance(NPC.class.getClassLoader(),new Class<?>[]{NPC.class},
            (proxy, method, args)->method.getName().equals("getName")?npc:null);
        // RuneLite 1.13.4 exposes an immutable event with a required constructor.
        // There is no zero-argument constructor and no setNpc/setItems API.
        return new NpcLootReceived(source, List.of(new ItemStack(itemId,qty)));
    }
    private static void flush() throws Exception {SwingUtilities.invokeAndWait(()->{});}
    @Test void onlyBossNpcEventsBecomeSessionDropsAndNeverIncreaseKc() throws Exception {
        World world=new World();BurrowPanel panel=new BurrowPanel(new BurrowModel());
        BurrowPlugin host=host(world,panel);
        host.onNpcLootReceived(event("Goblin",385,3));flush();
        assertTrue(panel.sessionReadout().drops().isEmpty());
        var drop=event("Tormented demon",565,2);
        host.onNpcLootReceived(drop);flush();
        assertEquals(1,panel.sessionReadout().drops().size());
        var row=panel.sessionReadout().drops().get(0);
        assertEquals("Tormented Demons",row.attribution());
        assertEquals(2L,row.quantity());
        assertTrue(panel.sessionReadout().kcByBoss().isEmpty());
        host.onNpcLootReceived(drop);flush();
        assertEquals(2L,panel.sessionReadout().drops().get(0).quantity());
        host.onNpcLootReceived(event("Demonic gorilla",565,3));flush();
        assertEquals(2,panel.sessionReadout().drops().size());
        assertTrue(panel.sessionReadout().kcByBoss().isEmpty());
    }
    @Test void logoutAndAccountChangeCannotLeakDrops() throws Exception {
        World world=new World();BurrowPanel panel=new BurrowPanel(new BurrowModel());
        BurrowPlugin host=host(world,panel);
        host.onNpcLootReceived(event("Vorkath",385,2));flush();
        assertEquals(1,panel.sessionReadout().drops().size());
        world.account=99L;
        host.onNpcLootReceived(event("Goblin",385,10));flush();
        assertTrue(panel.sessionReadout().drops().isEmpty());
        host.onNpcLootReceived(event("Vorkath",385,1));flush();
        assertEquals(1,panel.sessionReadout().drops().size());
        world.state=GameState.LOGIN_SCREEN;
        GameStateChanged state=new GameStateChanged();state.setGameState(GameState.LOGIN_SCREEN);
        host.onGameStateChanged(state);flush();
        assertTrue(panel.sessionReadout().drops().isEmpty());
    }
}
