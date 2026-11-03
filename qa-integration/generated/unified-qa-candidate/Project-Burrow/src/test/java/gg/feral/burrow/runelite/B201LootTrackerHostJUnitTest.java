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
public final class B201LootTrackerHostJUnitTest {
    private static class World {
        long account = 42L;
        int tick = 1;
        GameState state = GameState.LOGGED_IN;
        final Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
            new Class<?>[]{Client.class}, (proxy, method, args) -> switch(method.getName()) {
                case "getGameState" -> state;
                case "getAccountHash" -> account;
                case "getItemDefinition" -> null;
                case "getTickCount" -> tick;
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
    static net.runelite.client.plugins.loottracker.LootReceived loot(String name,net.runelite.http.api.loottracker.LootRecordType type,ItemStack... items){
        return new net.runelite.client.plugins.loottracker.LootReceived(name,0,type,List.of(items),1,null);
    }
    @Test void lootTrackerEventsBecomeLabelledEstimateNeverKc() throws Exception {
        World world=new World();BurrowPanel panel=new BurrowPanel(new BurrowModel());
        BurrowPlugin host=host(world,panel);
        var npc=net.runelite.http.api.loottracker.LootRecordType.NPC;
        var td=loot("Tormented Demon",npc,new ItemStack(20718,1),new ItemStack(211,1));
        host.onLootReceived(td);flush();
        assertEquals(java.util.Map.of("Tormented Demons",1L),panel.lootConfirmedEncounters(),"one event, two stacks = 1");
        host.onLootReceived(td);flush();
        assertEquals(1L,panel.lootConfirmedEncounters().get("Tormented Demons"),"duplicate subscriber");
        assertTrue(panel.sessionReadout().kcByBoss().isEmpty(),"never written as boss KC");
        host.onNpcLootReceived(event("Tormented demon",20718,1));flush();
        assertEquals(1L,panel.lootConfirmedEncounters().get("Tormented Demons"),"NpcLootReceived adds drops, not encounters");
        assertEquals(1,panel.sessionReadout().drops().size());
        host.onLootReceived(loot("Barrows",net.runelite.http.api.loottracker.LootRecordType.EVENT,new ItemStack(995,100)));flush();
        host.onLootReceived(loot("Goblin",npc,new ItemStack(526,1)));flush();
        host.onLootReceived(loot("Vorkath",npc));flush();
        assertEquals(java.util.Map.of("Tormented Demons",1L),panel.lootConfirmedEncounters());
        world.tick=9;host.onLootReceived(loot("Tormented Demon",npc,new ItemStack(20718,1),new ItemStack(211,1)));flush();
        assertEquals(2L,panel.lootConfirmedEncounters().get("Tormented Demons"),"a later kill counts");
    }
    @Test void accountSwitchAndLogoutResetEstimate() throws Exception {
        World world=new World();BurrowPanel panel=new BurrowPanel(new BurrowModel());
        BurrowPlugin host=host(world,panel);
        var npc=net.runelite.http.api.loottracker.LootRecordType.NPC;
        host.onLootReceived(loot("Vorkath",npc,new ItemStack(536,2)));flush();
        assertEquals(1L,panel.lootConfirmedEncounters().get("Vorkath"));
        world.account=99L;world.tick=5;
        host.onLootReceived(loot("Vorkath",npc,new ItemStack(536,2)));flush();
        assertEquals(java.util.Map.of("Vorkath",1L),panel.lootConfirmedEncounters(),"B starts from zero then counts its own kill");
        world.account=42L;world.tick=6;
        host.onLootReceived(loot("Zulrah",npc,new ItemStack(12934,100)));flush();
        assertEquals(java.util.Map.of("Zulrah",1L),panel.lootConfirmedEncounters(),"A's earlier login is not resurrected");
        world.state=GameState.LOGIN_SCREEN;
        GameStateChanged state=new GameStateChanged();state.setGameState(GameState.LOGIN_SCREEN);
        host.onGameStateChanged(state);flush();
        assertTrue(panel.lootConfirmedEncounters().isEmpty(),"logout clears");
        host.onLootReceived(loot("Zulrah",npc,new ItemStack(12934,100)));flush();
        assertTrue(panel.lootConfirmedEncounters().isEmpty(),"logged out: ignored");
    }
}
