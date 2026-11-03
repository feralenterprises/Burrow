package gg.feral.burrow.runelite;

import gg.feral.burrow.core.KillLogPageParser;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PQA-W09 read-only candidate. The only widget IDs used here match RuneLite's
 * own ChatCommandsPlugin boss-log reader (KillLog.INTERFACE_TITLE, NAME, KILL).
 * It never clicks a game menu, retrieves a bank, or interprets an unseen row.
 * Slayer requires an exact title match and remains pending native UI validation.
 */
public final class KillLogWidgetReader {
    private KillLogWidgetReader() {}
    public static Optional<KillLogPageParser.Observation> read(Client client) {
        if (client == null) return Optional.empty();
        try {
            Widget title = client.getWidget(InterfaceID.KillLog.INTERFACE_TITLE);
            Widget names = client.getWidget(InterfaceID.KillLog.NAME);
            Widget kills = client.getWidget(InterfaceID.KillLog.KILL);
            if (title == null || names == null || kills == null ||
                title.isHidden() || names.isHidden() || kills.isHidden()) return Optional.empty();
            Widget[] nameChildren = names.getChildren();
            Widget[] killChildren = kills.getChildren();
            if (nameChildren == null || killChildren == null || nameChildren.length < 1 ||
                nameChildren.length != killChildren.length) return Optional.empty();
            List<String> rawNames = new ArrayList<>(), rawKills = new ArrayList<>();
            for (int i = 0; i < nameChildren.length; i++) {
                if (nameChildren[i] == null || killChildren[i] == null) return Optional.empty();
                rawNames.add(nameChildren[i].getText());
                rawKills.add(killChildren[i].getText());
            }
            return KillLogPageParser.parse(title.getText(), rawNames, rawKills);
        } catch (RuntimeException incompleteOrChangedWidget) { return Optional.empty(); }
    }
}
