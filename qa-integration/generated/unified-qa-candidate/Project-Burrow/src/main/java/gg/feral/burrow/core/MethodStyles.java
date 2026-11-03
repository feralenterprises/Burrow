package gg.feral.burrow.core;

import gg.feral.burrow.core.CombatStyle.Styles;
import java.util.*;

/**
 * Build 20 structured method style metadata (primary + every required style), replacing the
 * single "selectedStyle" text for the combat-triangle icons. Explicit reviewed entries for every
 * multi-style method in the catalog; single-style methods use the catalog profile/research style.
 * Method identities are unchanged.
 */
public final class MethodStyles {
    private MethodStyles() {}
    private static final CombatStyle ME = CombatStyle.MELEE, RA = CombatStyle.RANGED, MA = CombatStyle.MAGIC;
    private static final Map<String,Styles> EXPLICIT = new HashMap<>();
    private static void put(String boss, String method, CombatStyle primary, CombatStyle... others) {
        List<CombatStyle> all = new ArrayList<>(); all.add(primary);
        for (CombatStyle o : others) if (!all.contains(o)) all.add(o);
        EXPLICIT.put(boss + "\u0000" + method, new Styles(primary, all));
    }
    static {
        put("Tormented Demons", DemonEncounterLoadouts.MELEE, ME, RA);
        put("Tormented Demons", DemonEncounterLoadouts.RANGED, RA, ME);
        put("Tormented Demons", DemonEncounterLoadouts.MAGIC, MA, ME);
        put("Tormented Demons", DemonEncounterLoadouts.TRIBRID, ME, RA, MA);
        put("Demonic Gorillas", DemonEncounterLoadouts.GORILLA, ME, RA);
        put("Demonic Gorillas", DemonEncounterLoadouts.GORILLA_RANGE, RA, ME);
        put("Zulrah", "Hybrid · magic + ranged", MA, RA);
        put("Dagannoth Kings", "Tribrid · Rex Prime Supreme", RA, ME, MA);
        put("Dagannoth Kings", "Rex safespot with Guthan healing switch", MA, ME);
        put("Phantom Muspah", "Hybrid · ranged + magic", RA, MA);
        put("Grotesque Guardians", "Mid-game melee with Eclipse atlatl ranged switch", ME, RA);
        put("Hueycoatl", "Body melee plus tail switches", ME, RA);
        put("Barrows", "Standard lower-level hybrid, Magic primary", MA, ME);
    }
    /** Styles for a registered method; NONE when unknown. */
    public static Styles of(String boss, String method) {
        if (boss == null || method == null || method.isBlank()) return Styles.NONE;
        Styles s = EXPLICIT.get(boss + "\u0000" + method);
        if (s != null) return s;
        String raw = TieredOutfitterCatalog.style(boss, method);
        var parsed = CombatStyle.parse(raw);
        if (parsed.isPresent()) {
            // R05: a Wiki draft with a switch table (e.g. Nightmare totem mage gear) also uses the switch's style.
            var sw = WikiStrategyLoadouts.switchStyle(boss, method);
            if (sw.isPresent() && sw.get() != parsed.get()) return new Styles(parsed.get(), List.of(parsed.get(), sw.get()));
            return new Styles(parsed.get(), List.of(parsed.get()));
        }
        // Fall back to the method name only when it starts with a style word.
        var byName = CombatStyle.parse(method);
        return byName.map(c -> new Styles(c, List.of(c))).orElse(Styles.NONE);
    }
}
