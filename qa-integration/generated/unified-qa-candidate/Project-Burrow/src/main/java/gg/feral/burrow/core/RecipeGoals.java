package gg.feral.burrow.core;

import java.util.*;

/**
 * BQA Batch D (GOAL-01/02/04): quantity- and recipe-aware drop goals with ownership reconciliation.
 *
 * <p>Separates four things the owner saw conflated: the <b>selected goal</b> (a wish, never ownership), copies
 * <b>verified owned now</b> (current source-qualified bank/inventory/worn evidence only), copies already
 * <b>consumed into owned crafted items</b> (a descendant counts its component copies), and <b>historical drops</b>
 * (displayed only; never added on top of owned stock, because a dropped item that is still held is already in the
 * bank evidence: no double credit).
 *
 * <p>Recipe component counts below are the owner's stated requirements (Project Burrow QA 2026-11-01). Descendant
 * item IDs are reused from IDs already present and verified in this codebase; descendants whose IDs are not yet
 * verified (e.g. Venator bow, Amulet of torture, imbued/charged ring variants) are deliberately omitted and listed in
 * {@link Recipe#unreconciled()} so the UI can say "crafted items not checked".
 */
public final class RecipeGoals {
    private RecipeGoals() {}

    public record Descendant(int itemId, String name, int componentsEach) {}
    public record Recipe(String key, String title, String encounter, String component, int defaultCopies,
                         List<Descendant> descendants, List<String> unreconciled, String source) {
        public Recipe { descendants = List.copyOf(descendants); unreconciled = List.copyOf(unreconciled); }
    }

    public static final String OWNER = "Owner requirement R01 (2026-11-01); component counts CHECK against OSRS Wiki before release";
    public static final List<Recipe> ALL = List.of(
        new Recipe("synapse", "Tormented synapse weapons", "Tormented Demons", "Tormented synapse", 3,
            List.of(new Descendant(29589, "Emberlight", 1), new Descendant(29591, "Scorching bow", 1), new Descendant(29594, "Purging staff", 1)),
            List.of(), OWNER),
        new Recipe("burning-claws", "Burning claws", "Tormented Demons", "Burning claw", 2,
            List.of(new Descendant(29577, "Burning claws", 2)), List.of(), OWNER),
        new Recipe("venator", "Venator bow", "Phantom Muspah", "Venator shard", 5,
            List.of(), List.of("Venator bow (item ID not yet verified here)"), OWNER),
        new Recipe("zenyte", "Zenyte jewellery", "Demonic Gorillas", "Zenyte shard", 4,
            List.of(new Descendant(19544, "Tormented bracelet", 1), new Descendant(19547, "Necklace of anguish", 1),
                    new Descendant(20657, "Ring of suffering", 1)),
            List.of("Amulet of torture, imbued/recoil ring variants and uncut/cut zenyte (IDs not yet verified here)"), OWNER),
        new Recipe("magic-fang", "Magic fang (optional x2)", "Zulrah", "Magic fang", 2,
            List.of(new Descendant(12899, "Trident of the Swamp", 1)),
            List.of("Toxic staff of the dead / charged variants (IDs not yet verified here)"), OWNER),
        new Recipe("huey-hides", "Hueycoatl armour hides", "Hueycoatl", "Hueycoatl hide", 8,
            List.of(), List.of("Hueycoatl armour pieces (3/2/2/1 split per owner; IDs and per-drop quantity CHECK)"), OWNER));

    public static Optional<Recipe> find(String key) { return ALL.stream().filter(r -> r.key().equals(key)).findFirst(); }

    /** Where ownership numbers came from; anything but CURRENT_EVIDENCE means "not checked". */
    public enum OwnershipSource { CURRENT_EVIDENCE, NOT_OBSERVED }

    public record Progress(Recipe recipe, int goalCopies, int ownedComponents, int inDescendants, int historicalDrops,
                           OwnershipSource source, int missing, List<String> notes) {
        public boolean complete() { return source == OwnershipSource.CURRENT_EVIDENCE && missing == 0; }
        public String line() {
            if (source != OwnershipSource.CURRENT_EVIDENCE)
                return "Goal " + goalCopies + " × " + recipe.component() + " · owned not checked (open bank)";
            return "Goal " + goalCopies + " · have " + (ownedComponents + inDescendants) + " (" + ownedComponents + " loose, "
                + inDescendants + " in crafted) · need " + missing;
        }
    }

    /**
     * @param goalCopies     owner-configurable target (defaults to {@link Recipe#defaultCopies()})
     * @param componentId    verified item ID of the component (from {@link RareDropTable})
     * @param evidence       current source-qualified bank evidence, or null when none this login
     * @param historicalDrops observed drops of the component (sessions/collection log), display only
     */
    public static Progress progress(Recipe r, int goalCopies, int componentId, BankEvidence evidence, int historicalDrops) {
        if (goalCopies < 0) throw new IllegalArgumentException("goal");
        List<String> notes = new ArrayList<>(r.unreconciled());
        if (historicalDrops > 0) notes.add(historicalDrops + " observed drop(s) shown for history only; not added to owned");
        if (evidence == null || !evidence.planningInput() || evidence.stale()) {
            if (evidence != null && evidence.stale()) notes.add("Bank read is stale; ownership not counted");
            return new Progress(r, goalCopies, 0, 0, historicalDrops, OwnershipSource.NOT_OBSERVED, goalCopies, notes);
        }
        Map<Integer,Integer> all = evidence.combined();
        int loose = all.getOrDefault(componentId, 0);
        int crafted = 0;
        for (Descendant d : r.descendants()) crafted = Math.addExact(crafted, Math.multiplyExact(all.getOrDefault(d.itemId(), 0), d.componentsEach()));
        int missing = Math.max(0, goalCopies - loose - crafted);
        return new Progress(r, goalCopies, loose, crafted, historicalDrops, OwnershipSource.CURRENT_EVIDENCE, missing, notes);
    }
}
