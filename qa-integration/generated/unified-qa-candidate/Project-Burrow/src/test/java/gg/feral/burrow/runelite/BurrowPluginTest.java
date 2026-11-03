package gg.feral.burrow.runelite;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Local RuneLite development entrypoint; never handles credentials. */
public final class BurrowPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(BurrowPlugin.class);
        RuneLite.main(args);
    }
}
