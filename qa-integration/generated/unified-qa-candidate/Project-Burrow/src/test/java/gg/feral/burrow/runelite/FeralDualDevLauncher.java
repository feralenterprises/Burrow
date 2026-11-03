package gg.feral.burrow.runelite;

import com.feralenterprises.hoverlite.HoverLitePlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Local owner-only launcher; no auth handling or Plugin Hub changes. */
public final class FeralDualDevLauncher
{
    private FeralDualDevLauncher() { }
    public static void main(String[] args) throws Exception
    {
        System.out.println("[FERAL DUAL DEV] Registering both local plugins in one client.");
        ExternalPluginManager.loadBuiltin(BurrowPlugin.class, HoverLitePlugin.class);
        RuneLite.main(args);
    }
}
