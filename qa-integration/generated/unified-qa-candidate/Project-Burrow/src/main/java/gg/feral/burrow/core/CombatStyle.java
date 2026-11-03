package gg.feral.burrow.core;

import java.util.*;

/** Build 20 combat triangle: Melee red, Ranged green, Magic blue (owner-approved accents). */
public enum CombatStyle {
    MELEE("Melee", 0xD85A50), RANGED("Ranged", 0x53B87A), MAGIC("Magic", 0x5B9EE5);
    private final String label; private final int rgb;
    CombatStyle(String label, int rgb) { this.label = label; this.rgb = rgb; }
    public String label() { return label; }
    public int rgb() { return rgb; }
    public static Optional<CombatStyle> parse(String s) {
        if (s == null) return Optional.empty();
        String t = s.trim().toLowerCase(Locale.ROOT);
        if (t.startsWith("melee")) return Optional.of(MELEE);
        if (t.startsWith("ranged") || t.startsWith("range")) return Optional.of(RANGED);
        if (t.startsWith("magic") || t.startsWith("mage")) return Optional.of(MAGIC);
        return Optional.empty();
    }
    /** Primary style plus the ordered set of every style the method requires. */
    public record Styles(CombatStyle primary, List<CombatStyle> required) {
        public Styles {
            required = List.copyOf(required);
            if (primary != null && !required.contains(primary)) throw new IllegalArgumentException("primary not required");
        }
        public boolean uses(CombatStyle s) { return required.contains(s); }
        public boolean hybrid() { return required.size() > 1; }
        public static final Styles NONE = new Styles(null, List.of());
    }
}
