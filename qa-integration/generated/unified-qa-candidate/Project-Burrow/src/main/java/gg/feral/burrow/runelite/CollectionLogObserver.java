package gg.feral.burrow.runelite;

import gg.feral.burrow.core.CollectionLogParser;
import java.util.*;
import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;

/**
 * Build 20: READ-ONLY Collection Log page reader. Called on the client thread after the game's
 * collection-draw script ran. Copies header text and item slots; never clicks, scrolls, or changes
 * a widget. A missing or hidden widget yields empty (unknown), never zero.
 */
final class CollectionLogObserver {
    private CollectionLogObserver() {}
    static Optional<CollectionLogParser.Observation> read(Client client) {
        try {
            Widget header=client.getWidget(ComponentID.COLLECTION_LOG_ENTRY_HEADER);
            Widget items=client.getWidget(ComponentID.COLLECTION_LOG_ENTRY_ITEMS);
            if(header==null || header.isHidden()) return Optional.empty();
            List<String> texts=new ArrayList<>();
            Widget[] hc=header.getDynamicChildren();
            if(hc==null) return Optional.empty();
            for(Widget w:hc) if(w!=null && w.getText()!=null && !w.getText().isBlank()) texts.add(w.getText());
            List<CollectionLogParser.Slot> slots=new ArrayList<>();
            if(items!=null && !items.isHidden() && items.getDynamicChildren()!=null)
                for(Widget w:items.getDynamicChildren())
                    if(w!=null && w.getItemId()>0) slots.add(new CollectionLogParser.Slot(w.getItemId(),w.getOpacity(),w.getItemQuantity()));
            return CollectionLogParser.parse(texts,slots);
        } catch(RuntimeException unavailable) {
            return Optional.empty();
        }
    }
}
