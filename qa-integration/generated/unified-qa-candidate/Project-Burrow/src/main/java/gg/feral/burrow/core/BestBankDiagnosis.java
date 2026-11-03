package gg.feral.burrow.core;

import gg.feral.burrow.core.BestBankPlanner.*;
import java.util.*;

/**
 * BQA-IMPL-R05 P0: explains, per unmet gear slot, WHY Best in Bank did not use owned gear, so capture failure,
 * missing recognition, unreviewed variants, withheld substitutions and planner rejection are no longer one
 * undifferentiated "not observed".
 *
 * <p>Name evidence comes from RuneLite item definitions of the items actually observed this read. A same-name
 * owned item with a different ID (e.g. "(i)", "(u)", charged/uncharged forms) is reported as
 * {@link Cause#OWNED_VARIANT_NOT_REVIEWED}: it is NOT counted as owned until the variant is reviewed into
 * {@link ItemVariantPolicy}. Nothing here changes the plan or its verdict.
 */
public final class BestBankDiagnosis {
    private BestBankDiagnosis() {}
    public enum Cause { OWNED_VARIANT_NOT_REVIEWED, CORE_SUBSTITUTE_WITHHELD, NO_REVIEWED_ALTERNATIVE_OWNED, QUANTITY_SHORT, REQUIREMENT }
    public record Finding(int slot, String slotName, Cause cause, String text) {}
    private static final String[] SLOT = {"Head","Cape","Neck","Ammo","Weapon","Body","Shield","Legs","Gloves","Boots","Ring"};

    /** Lower-case, parenthesised suffixes / dose markers / punctuation removed: "Ring of suffering (i)" -> "ring of suffering". */
    public static String baseName(String name) {
        if (name == null) return "";
        String s = name.toLowerCase(Locale.ROOT).replaceAll("\\([^)]*\\)", " ").replaceAll("#.*$", " ")
            .replaceAll("[^a-z0-9' ]", " ").replaceAll(" +", " ").trim();
        return s;
    }

    public static List<Finding> diagnose(Plan plan, Map<Integer,Integer> combined, Map<Integer,String> observedNames) {
        List<Finding> out = new ArrayList<>();
        if (plan == null || plan.verdict() == Verdict.UNKNOWN || plan.verdict() == Verdict.NOT_AVAILABLE_WILDERNESS) return out;
        Map<String,List<Integer>> ownedByBase = new HashMap<>();
        if (observedNames != null)
            for (var e : observedNames.entrySet())
                if (combined.getOrDefault(e.getKey(), 0) > 0)
                    ownedByBase.computeIfAbsent(baseName(e.getValue()), k -> new ArrayList<>()).add(e.getKey());
        for (int i = 0; i < plan.gear().size() && i < SLOT.length; i++) {
            GearCell c = plan.gear().get(i);
            if (c.planned() == null) continue;
            switch (c.status()) {
                case NOT_OBSERVED, OPTIONAL_MISSING -> {
                    var same = ownedByBase.getOrDefault(baseName(c.planned().name()), List.of()).stream()
                        .filter(id -> id != c.planned().id()).sorted().toList();
                    var unusable = NamedVariantRules.classify(c.planned(), combined, observedNames == null ? Map.of() : observedNames)
                        .stream().filter(h -> !h.usable()).findFirst();
                    if (unusable.isPresent()) {
                        out.add(new Finding(i, SLOT[i], Cause.OWNED_VARIANT_NOT_REVIEWED,
                            SLOT[i] + ": you own " + unusable.get().ownedName() + " (id " + unusable.get().itemId() + "), excluded: " + unusable.get().note()));
                    } else if (!same.isEmpty()) {
                        String owned = observedNames.get(same.get(0));
                        out.add(new Finding(i, SLOT[i], Cause.OWNED_VARIANT_NOT_REVIEWED,
                            SLOT[i] + ": you own " + owned + " (id " + same.get(0) + "); this variant is not reviewed yet, so it is not counted"));
                    } else if (c.status() == GearStatus.OPTIONAL_MISSING) {
                        out.add(new Finding(i, SLOT[i], Cause.NO_REVIEWED_ALTERNATIVE_OWNED,
                            SLOT[i] + ": " + c.planned().name() + " not owned; optional slot left empty (not a blocker)"));
                    } else if (i == 3 || i == 4 || i == 6) {
                        out.add(new Finding(i, SLOT[i], Cause.CORE_SUBSTITUTE_WITHHELD,
                            SLOT[i] + ": " + c.planned().name() + " not owned; weapon/ammo/shield swaps need a complete owned lower-tier set"));
                    } else {
                        out.add(new Finding(i, SLOT[i], Cause.NO_REVIEWED_ALTERNATIVE_OWNED,
                            SLOT[i] + ": " + c.planned().name() + " not owned; no reviewed lower-tier item owned"));
                    }
                }
                case PARTIAL_STACK -> out.add(new Finding(i, SLOT[i], Cause.QUANTITY_SHORT, SLOT[i] + ": " + c.planned().name() + " quantity short"));
                case LEVEL_UNMET, QUEST_UNMET -> out.add(new Finding(i, SLOT[i], Cause.REQUIREMENT, SLOT[i] + ": " + c.note()));
                default -> {}
            }
        }
        return out;
    }
}
