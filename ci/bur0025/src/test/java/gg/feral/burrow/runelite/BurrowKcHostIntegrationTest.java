package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.ui.BurrowPanel;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

/** Synthetic host fixtures, not evidence of actual RuneLite player events. */
final class BurrowKcHostIntegrationTest {
    static final class World {
        long hash = 101;
        GameState state = GameState.LOGGED_IN;
        Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[]{Client.class},
                (p,m,a) -> switch(m.getName()) {
                    case "getGameState" -> state;
                    case "getAccountHash" -> hash;
                    default -> throw new AssertionError("Unexpected client call: " + m.getName());
                });
    }
    static Field field(String name) throws Exception { Field f=BurrowPlugin.class.getDeclaredField(name); f.setAccessible(true); return f; }
    static void flush() throws Exception { SwingUtilities.invokeAndWait(() -> {}); }
    static BurrowPlugin host(World w, BurrowPanel p) throws Exception {
        BurrowPlugin h=new BurrowPlugin(); field("client").set(h,w.client); field("panel").set(h,p);
        ((AtomicBoolean)field("running").get(h)).set(true); return h;
    }
    static void chat(BurrowPlugin h, ChatMessageType type, int count) {
        ChatMessage e=new ChatMessage(); e.setType(type); e.setMessage("Your Vorkath kill count is: " + count + "."); h.onChatMessage(e);
    }
    static void republish(BurrowPlugin h, BurrowPanel p) throws Exception {
        Object scope=field("sessionScope").get(h); field("panel").set(h,p);
        Method m=BurrowPlugin.class.getDeclaredMethod("publishSessionToPanel", BurrowPanel.SessionScope.class, BurrowPanel.class);
        m.setAccessible(true); m.invoke(h,scope,p); flush();
    }
    @Test void midLoginIdentityLossReauthenticationAndSidebarReplay() throws Exception {
        World w=new World(); BurrowPanel p=new BurrowPanel(new BurrowModel()); BurrowPlugin h=host(w,p);
        chat(h,ChatMessageType.GAMEMESSAGE,100); chat(h,ChatMessageType.GAMEMESSAGE,101); flush();
        assertEquals(1L,p.sessionReadout().kcByBoss().get("Vorkath"));
        BurrowPanel reopened=new BurrowPanel(new BurrowModel()); republish(h,reopened);
        assertEquals(1L,reopened.sessionReadout().kcByBoss().get("Vorkath"));
        w.hash=-1; chat(h,ChatMessageType.GAMEMESSAGE,102); flush();
        assertTrue(reopened.sessionReadout().kcByBoss().isEmpty());
        assertNull(field("sessionScope").get(h));
        w.hash=101; chat(h,ChatMessageType.GAMEMESSAGE,103); flush();
        assertTrue(reopened.sessionReadout().kcByBoss().isEmpty());
        chat(h,ChatMessageType.PUBLICCHAT,104); chat(h,ChatMessageType.GAMEMESSAGE,104); flush();
        assertEquals(1L,reopened.sessionReadout().kcByBoss().get("Vorkath"));
    }
    @Test void accountSwitchCannotCarryGains() throws Exception {
        World w=new World(); BurrowPanel p=new BurrowPanel(new BurrowModel()); BurrowPlugin h=host(w,p);
        chat(h,ChatMessageType.GAMEMESSAGE,1); chat(h,ChatMessageType.GAMEMESSAGE,2); flush();
        w.hash=202; chat(h,ChatMessageType.GAMEMESSAGE,50); flush();
        assertTrue(p.sessionReadout().kcByBoss().isEmpty());
        chat(h,ChatMessageType.GAMEMESSAGE,51); flush(); assertEquals(1L,p.sessionReadout().kcByBoss().get("Vorkath"));
        w.hash=101; chat(h,ChatMessageType.GAMEMESSAGE,3); flush(); assertTrue(p.sessionReadout().kcByBoss().isEmpty());
    }
}
