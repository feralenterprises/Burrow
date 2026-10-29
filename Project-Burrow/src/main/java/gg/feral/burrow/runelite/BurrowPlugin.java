package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.host.NavigationLifecycle;
import gg.feral.burrow.ui.BurrowPanel;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.PluginPanel;
import java.awt.BorderLayout;
import net.runelite.client.ui.NavigationButton;

/** Minimal, isolated host adapter. All OSRS observations and bank writes are intentionally absent. */
@PluginDescriptor(name="Project Burrow [Experimental]", description="Feral Enterprises independent rewrite; offline-only prototype")
public final class BurrowPlugin extends Plugin {
    @Inject private ClientToolbar toolbar;
    private NavigationButton navigation;
    private BurrowPanel panel;
    private PluginPanel hostPanel;
    private final NavigationLifecycle lifecycle = new NavigationLifecycle(
            SwingUtilities::invokeLater, this::registerButton, this::unregisterButton);

    @Override protected void startUp() { lifecycle.start(); }
    @Override protected void shutDown() { lifecycle.stop(); }

    private void registerButton() {
        if (navigation != null) return;
        BurrowPanel nextPanel = new BurrowPanel(new BurrowModel());
        PluginPanel nextHostPanel = new PluginPanel() { };
        nextHostPanel.setLayout(new BorderLayout());
        nextHostPanel.add(nextPanel, BorderLayout.CENTER);
        BufferedImage logo = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = logo.createGraphics();
        try {
            g.setColor(new Color(231,179,100)); g.fillRoundRect(1,1,14,14,6,6);
            g.setColor(new Color(30,34,36)); g.fillOval(4,5,8,6);
            g.setColor(Color.WHITE); g.fillOval(5,6,2,2); g.fillOval(9,6,2,2);
        } finally { g.dispose(); }
        NavigationButton next = NavigationButton.builder()
                .tooltip("Burrow • experimental").icon(logo).priority(8).panel(nextHostPanel).build();
        toolbar.addNavigation(next);
        panel = nextPanel;
        hostPanel = nextHostPanel;
        navigation = next;
    }

    private void unregisterButton() {
        NavigationButton previous = navigation;
        navigation = null;
        panel = null;
        hostPanel = null;
        if (previous != null) toolbar.removeNavigation(previous);
    }
}
