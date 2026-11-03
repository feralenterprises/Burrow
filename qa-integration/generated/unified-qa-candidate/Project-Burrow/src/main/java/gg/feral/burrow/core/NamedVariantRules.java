package gg.feral.burrow.core;

import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import java.util.*;

/**
 * CP-1 (BQA-R12-003): reviewed NAME-SUFFIX variant rules, applied only to items positively observed in the
 * current BANK + INVENTORY + EQUIPMENT read, using the item names RuneLite reported for those exact IDs.
 *
 * <p>Each rule compares the planned Wiki item name P with an observed item name O, exactly (case-insensitive):
 * <ul>
 *   <li>{@code O == P + " (l)"}: Trouver-parchment locked copy. Same item, same stats, wearable. USABLE.</li>
 *   <li>{@code O == P + " (or)" | " (g)" | " (t)"}: ornament/trimmed cosmetic copy, same stats. USABLE.</li>
 *   <li>{@code O == P + " (i)"} where P is not itself imbued: imbued upgrade of the same item. USABLE.</li>
 *   <li>{@code O == P + " 100|75|50|25"} for a Barrows piece: degraded but wearable. USABLE, with a check note.</li>
 *   <li>{@code O == P + " 0"} or {@code P + " (broken)"}: broken. EXCLUDED (repair first).</li>
 *   <li>{@code O == P + " (inactive)" | " (uncharged)" | " (empty)"}: needs activation/charges. EXCLUDED.</li>
 *   <li>{@code O == P} but a different item ID that is not a reviewed equivalent: EXCLUDED, reported as
 *       "unreviewed same-name variant" (e.g. minigame copies). Never silently counted.</li>
 * </ul>
 * Explicit ID deny-lists in {@link ItemVariantPolicy} (Last Man Standing / trophy copies) always win.
 * Pure Java; no RuneLite types. Absence of a name never proves absence of an item.
 */
public final class NamedVariantRules {
    private NamedVariantRules() {}

    public enum Rule { LOCKED, ORNAMENT, IMBUED_UPGRADE, DEGRADED_BARROWS, BROKEN, NEEDS_CHARGE, SAME_NAME_UNREVIEWED }

    public record Hit(int itemId, String ownedName, Rule rule, String note) {
        public boolean usable() {
            return rule == Rule.LOCKED || rule == Rule.ORNAMENT || rule == Rule.IMBUED_UPGRADE || rule == Rule.DEGRADED_BARROWS;
        }
    }

    private static final List<String> BARROWS = List.of("dharok", "verac", "guthan", "torag", "karil", "ahrim");
    private static final Set<Integer> DENY = deny();
    private static Set<Integer> deny() {
        Set<Integer> s = new HashSet<>();
        for (int id : ItemVariantPolicy.RING_NEVER) s.add(id);
        for (int id : ItemVariantPolicy.GLORY_NEVER) s.add(id);
        return Set.copyOf(s);
    }

    /** Strips Burrow's own display decoration so rules compare against the Wiki name. */
    static String plannedName(Item planned) {
        String n = planned == null || planned.name() == null ? "" : planned.name();
        n = n.replace(" (owned variant)", "").replace(" (reviewed owned variant)", "");
        return n.trim().toLowerCase(Locale.ROOT);
    }

    /** Every observed item that a rule recognises as a form of {@code planned}, best usable first. */
    public static List<Hit> classify(Item planned, Map<Integer,Integer> observed, Map<Integer,String> names) {
        if (planned == null || planned.id() <= 0 || observed == null || names == null || names.isEmpty()) return List.of();
        String p = plannedName(planned);
        if (p.isEmpty()) return List.of();
        boolean plannedImbued = p.endsWith(" (i)");
        boolean barrows = BARROWS.stream().anyMatch(p::contains);
        List<Hit> out = new ArrayList<>();
        for (var e : names.entrySet()) {
            int id = e.getKey();
            if (id == planned.id() || observed.getOrDefault(id, 0) <= 0 || e.getValue() == null) continue;
            String o = e.getValue().trim().toLowerCase(Locale.ROOT);
            String owned = e.getValue().trim();
            Rule rule = null; String note = null;
            if (DENY.contains(id)) {
                if (o.startsWith(p.replace(" (i)", ""))) { rule = Rule.SAME_NAME_UNREVIEWED; note = "minigame or trophy copy (id " + id + "), never usable here"; }
            } else if (o.equals(p + " (l)")) { rule = Rule.LOCKED; note = "Trouver-locked copy, same stats"; }
            else if (o.equals(p + " (or)") || o.equals(p + " (g)") || o.equals(p + " (t)")) { rule = Rule.ORNAMENT; note = "ornament/trimmed copy, same stats"; }
            else if (!plannedImbued && o.equals(p + " (i)")) { rule = Rule.IMBUED_UPGRADE; note = "imbued version of the planned item"; }
            else if (barrows && o.matches(java.util.regex.Pattern.quote(p) + " (100|75|50|25)")) {
                rule = Rule.DEGRADED_BARROWS; note = "degraded Barrows piece (" + o.substring(p.length() + 1) + "); repair cost applies";
            }
            else if (o.equals(p + " 0") || o.equals(p + " (broken)")) { rule = Rule.BROKEN; note = "broken; repair before use"; }
            else if (o.equals(p + " (inactive)") || o.equals(p + " (uncharged)") || o.equals(p + " (empty)")) {
                rule = Rule.NEEDS_CHARGE; note = "needs activation or charges before use";
            }
            else if (o.equals(p)) { rule = Rule.SAME_NAME_UNREVIEWED; note = "same name, different item id " + id + " (not a reviewed equivalent)"; }
            if (rule != null) out.add(new Hit(id, owned, rule, note));
        }
        out.sort(Comparator.comparingInt((Hit h) -> h.usable() ? 0 : 1)
            .thenComparingInt(h -> h.rule().ordinal())
            .thenComparing(h -> degradeRank(h.ownedName()))
            .thenComparingInt(Hit::itemId));
        return List.copyOf(out);
    }

    /** First usable reviewed form, if any. */
    public static Optional<Hit> usable(Item planned, Map<Integer,Integer> observed, Map<Integer,String> names) {
        return classify(planned, observed, names).stream().filter(Hit::usable).findFirst();
    }

    // Higher remaining durability first: "100" before "25".
    private static int degradeRank(String name) {
        String n = name == null ? "" : name.trim();
        for (String d : new String[]{" 100", " 75", " 50", " 25"}) if (n.endsWith(d)) return -Integer.parseInt(d.trim());
        return 0;
    }
}
