package gg.feral.burrow.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Narrow reuse of Iron Ledger's NpcLootReceived intake for current-login
 * *boss-encounter drops only*. No generic NPC drops, persistence, or KC inference.
 * These canonical names come from the supported encounter lineup; neither
 * Outfitter data nor UI is modified by this independent event filter.
 */
public final class IronLedgerLootIntake {
    public record ItemObserved(int id, int quantity, String itemName) {}
    public record Drop(int itemId, String itemName, int amount, String boss) {}
    private IronLedgerLootIntake() {}

    // Exact observed NPC names only. Do not use substring matching: e.g. a
    // Dagannoth is NOT Dagannoth Rex, and a regular gorilla is NOT demonic.
    // Barrows chest rewards have no boss NPC drop callback and are intentionally
    // unsupported here; other multi-entity rewards need independent proof.
    private static final Map<String, String> BOSSES = Map.ofEntries(
        Map.entry("scurrius", "Scurrius"),
        Map.entry("brutus", "Brutus"),
        Map.entry("giant mole", "Giant Mole"),
        Map.entry("vorkath", "Vorkath"),
        Map.entry("phantom muspah", "Phantom Muspah"),
        Map.entry("sarachnis", "Sarachnis"),
        Map.entry("the hueycoatl", "Hueycoatl"),
        Map.entry("hueycoatl", "Hueycoatl"),
        Map.entry("amoxliatl", "Amoxliatl"),
        Map.entry("king black dragon", "King Black Dragon"),
        Map.entry("zulrah", "Zulrah"),
        Map.entry("obor", "Obor"),
        Map.entry("bryophyta", "Bryophyta"),
        Map.entry("dagannoth rex", "Dagannoth Rex"),
        Map.entry("dagannoth prime", "Dagannoth Prime"),
        Map.entry("dagannoth supreme", "Dagannoth Supreme"),
        Map.entry("dusk", "Grotesque Guardians (Dusk)"),
        Map.entry("dawn", "Grotesque Guardians (Dawn)"),
        Map.entry("tormented demon", "Tormented Demons"),
        Map.entry("demonic gorilla", "Demonic Gorillas")
    );

    public static Optional<String> recognizeEncounter(String npc) {
        if (npc == null) return Optional.empty();
        return Optional.ofNullable(BOSSES.get(npc.trim().toLowerCase(Locale.ROOT)));
    }

    /** NPC is the event's actual source, never a reconstructed KC message. */
    public static List<Drop> accept(String npc, List<ItemObserved> items) {
        Optional<String> boss = recognizeEncounter(npc);
        if (boss.isEmpty() || items == null) return List.of();
        List<Drop> result = new ArrayList<>();
        for (ItemObserved item : items) {
            if (item == null || item.id() <= 0 || item.quantity() <= 0) continue;
            String name = item.itemName() == null || item.itemName().isBlank()
                ? "Item #" + item.id() : item.itemName();
            result.add(new Drop(item.id(), name, item.quantity(), boss.get()));
        }
        return List.copyOf(result);
    }
}
