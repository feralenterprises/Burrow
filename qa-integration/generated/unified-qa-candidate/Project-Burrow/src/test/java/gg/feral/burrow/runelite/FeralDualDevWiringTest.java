package gg.feral.burrow.runelite;

import com.feralenterprises.hoverlite.HoverLitePlugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FeralDualDevWiringTest
{
    @Test void bothIndependentPluginsAreOnDevelopmentClasspath()
    {
        assertNotNull(BurrowPlugin.class.getAnnotation(PluginDescriptor.class));
        assertNotNull(HoverLitePlugin.class.getAnnotation(PluginDescriptor.class));
        assertNotEquals(BurrowPlugin.class.getName(), HoverLitePlugin.class.getName());
    }
}
