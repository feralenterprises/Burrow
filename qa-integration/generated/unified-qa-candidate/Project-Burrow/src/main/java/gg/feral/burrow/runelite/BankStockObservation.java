package gg.feral.burrow.runelite;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;

/**
 * BUR-0016: positive bank observations only, never a complete-bank ownership claim.
 * Called synchronously while the native bank is open, on RuneLite's client thread.
 * A missing container/items or failed item definition makes the entire snapshot
 * unavailable, NOT empty. Filters out placeholders, noted forms and zero quantities.
 * This is NOT the production-ready complete/per-account cache required by BUR-0023/0042.
 */
final class BankStockObservation {
    private static final int FILLER = 20594;

    record Snapshot(Set<Integer> usableIds, boolean available, String reason) {
        Snapshot {
            usableIds = Set.copyOf(usableIds);
        }
        static Snapshot unknown(String reason) {
            return new Snapshot(Collections.emptySet(), false, reason);
        }
    }

    static Snapshot capture(ItemContainer bank, IntFunction<ItemComposition> definitions) {
        if (bank == null) return Snapshot.unknown("bank item container unavailable");
        if (definitions == null) return Snapshot.unknown("item definitions unavailable");
        try {
            Item[] items = bank.getItems();
            if (items == null) return Snapshot.unknown("bank item list unavailable");
            if (items.length == 0) return Snapshot.unknown("empty or not-yet-loaded bank not independently confirmed");
            Set<Integer> seen = new HashSet<>();
            for (Item item : items) {
                if (item == null || item.getId() <= 0 || item.getQuantity() <= 0
                        || item.getId() == FILLER) continue;
                ItemComposition composition = definitions.apply(item.getId());
                if (composition == null) return Snapshot.unknown("one or more item definitions unavailable");
                if (composition.getPlaceholderTemplateId() != -1 || composition.getNote() != -1)
                    continue;
                // A definition for a different ID is not usable evidence for this slot.
                if (composition.getId() != item.getId())
                    return Snapshot.unknown("item definition identity mismatch");
                seen.add(item.getId());
            }
            if (seen.isEmpty()) return Snapshot.unknown("no usable entries observed; bank completeness unproven");
            return new Snapshot(seen, true, "observed open-bank items; completeness not independently verified");
        } catch (RuntimeException unresolved) {
            return Snapshot.unknown("bank item metadata unreadable");
        }
    }

    private BankStockObservation() {}
}
