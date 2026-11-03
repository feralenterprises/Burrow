package gg.feral.burrow.core;

import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import java.util.List;

/** BUR-0022: First Scurrius kill cannot require a spine dropped by that same boss. */
public final class ScurriusEntryProgressionTest {
    private static int assertions;
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        BurrowModel model = new BurrowModel();
        check(model.boss().equals("Scurrius") && model.plan()!=null,
            "default Scurrius plan resolves using aligned base catalog method");
        check(model.methods().get(0).equals("Standard melee") &&
              model.methods().contains("Ranged · rat-bone option") &&
              model.methods().contains("Magic · rat-bone option"),
            "default first-kill melee untouched; researched styles separately available");
        for (Tier tier : Tier.values()) {
            model.chooseTier(tier);
            check(model.profile()!=null && model.plan()!=null,
                "Scurrius tier " + tier + " still projects a plan");
            check(model.profile().method().equals("Standard melee"),
                "same method title in all three tiers");
            check(model.profile().gear().size()==11 && model.profile().inventory().size()==28,
                "equipment and supplies geometry unchanged");
        }
        model.chooseTier(Tier.ENTRY);
        check(model.profile().gear().get(4).id()==1333,
            "Entry continues to use attainable Rune scimitar");
        var entry=Final06ProfileMetadata.find("Scurrius", "Standard melee", Tier.ENTRY);
        check(entry!=null,"Entry preparation metadata exists");
        check(entry.questRequirements().stream().noneMatch(s -> s.contains("Bone mace") || s.contains("Scurrius spine")),
            "Entry quest/equipment gate must not demand ratbane weapon or boss spine");
        check(entry.questRequirements().contains("Dragon Slayer I") &&
              entry.questRequirements().contains("Death Plateau"),
            "unrelated requirements for selected platebody and boots retained");
        model.chooseTier(Tier.RECOMMENDED);
        check(model.profile().gear().get(4).id()==28792,
            "Recommended retains bone mace as progression upgrade");
        var recommended=Final06ProfileMetadata.find("Scurrius", "Standard melee", Tier.RECOMMENDED);
        check(recommended.questRequirements().stream().anyMatch(s -> s.contains("Scurrius spine")),
            "bone mace requirement retained where weapon is actually equipped");
        model.chooseTier(Tier.OPTIMAL);
        check(model.profile().gear().get(4).id()==22325 && model.profile().gear().get(6)==null,
            "Optimal retains two-handed Scythe with empty offhand");
        check(model.profile().inventory().stream().anyMatch(i -> i!=null && i.id()==28792),
            "Optimal keeps the bone mace switch for summoned rats");
        check(TieredOutfitterCatalog.size()==51 && Final06ProfileMetadata.size()==51,
            "all pre-existing 51 loadout registrations preserved");
        System.out.println("BUR-0022 SCURRIUS FIRST-KILL PROGRESSION PASS: " + assertions + " assertions");
    }
}
