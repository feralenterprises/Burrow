package gg.feral.burrow.intelligence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure W04 activity and route reasoning. Does NOT claim to read quests, bank, cooldowns or diary completion. */
public final class ActivityDecisions {
    private ActivityDecisions() {}

    public enum Outcome { READY, BLOCKED, VERIFY, RESEARCH_ONLY }
    public enum Observation { MET, UNMET, UNKNOWN }

    /**
     * All observations are supplied by a separately validated account adapter. Missing
     * keys are UNKNOWN, never "false". An incomplete bank read must NOT provide a
     * fabricated zero quantity. Daily uses/cooldowns can be absent or explicitly known.
     * Flags use namespaced keys, e.g. "QUEST:Sins_of_the_Father".
     */
    public record AccountEvidence(Map<String,Integer> realLevels,
                                  Map<String,Integer> effectiveLevels,
                                  Integer combatLevel,
                                  Map<String,Boolean> unlockFlags,
                                  Map<String,Integer> itemQuantities,
                                  boolean completeItemSnapshot,
                                  Map<String,Integer> dailyRemaining,
                                  Map<String,Boolean> cooldownReady) {
        public AccountEvidence {
            realLevels = Map.copyOf(Objects.requireNonNull(realLevels));
            effectiveLevels = Map.copyOf(Objects.requireNonNull(effectiveLevels));
            unlockFlags = Map.copyOf(Objects.requireNonNull(unlockFlags));
            itemQuantities = Map.copyOf(Objects.requireNonNull(itemQuantities));
            dailyRemaining = Map.copyOf(Objects.requireNonNull(dailyRemaining));
            cooldownReady = Map.copyOf(Objects.requireNonNull(cooldownReady));
        }
        public static AccountEvidence unknown() {
            return new AccountEvidence(Map.of(),Map.of(),null,Map.of(),Map.of(),false,Map.of(),Map.of());
        }
    }

    public record Result(String activityId, Outcome outcome, List<String> blockers,
                         List<String> checks, List<String> optionalMissing,
                         boolean forbidBestInBank) {
        public Result {
            blockers = List.copyOf(blockers);
            checks = List.copyOf(checks);
            optionalMissing = List.copyOf(optionalMissing);
        }
    }

    public static Observation observe(ActivityKnowledge.Requirement requirement, AccountEvidence account) {
        Objects.requireNonNull(requirement);
        Objects.requireNonNull(account);
        switch (requirement.type()) {
            case SKILL_REAL, SKILL_EFFECTIVE -> {
                Map<String,Integer> levels = requirement.type() == ActivityKnowledge.RequirementType.SKILL_REAL
                    ? account.realLevels() : account.effectiveLevels();
                Integer level = levels.get(requirement.key());
                if (level == null || level < 0) return Observation.UNKNOWN;
                return level >= requirement.minimum() ? Observation.MET : Observation.UNMET;
            }
            case COMBAT_LEVEL -> {
                Integer level = account.combatLevel();
                return level == null ? Observation.UNKNOWN : level >= requirement.minimum() ? Observation.MET : Observation.UNMET;
            }
            case QUEST, DIARY, UNLOCK -> {
                Boolean value = account.unlockFlags().get(requirement.type().name() + ":" + requirement.key());
                return value == null ? Observation.UNKNOWN : value ? Observation.MET : Observation.UNMET;
            }
            case ITEM -> {
                // Item requirement keys are string forms of exact item IDs. No loose name matching.
                Integer amount = account.itemQuantities().get(requirement.key());
                if (amount == null) return account.completeItemSnapshot() ? Observation.UNMET : Observation.UNKNOWN;
                return amount >= requirement.minimum() ? Observation.MET : Observation.UNMET;
            }
            default -> throw new IllegalArgumentException("Unhandled requirement type");
        }
    }

    public static Result evaluate(ActivityKnowledge.Entry entry, AccountEvidence account) {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(account);
        if (!entry.approved()) return new Result(entry.id(),Outcome.RESEARCH_ONLY,
            List.of(),List.of("Evidence not approved; staging only"),List.of(),entry.wilderness());

        List<String> blockers = new ArrayList<>();
        List<String> checks = new ArrayList<>();
        List<String> optional = new ArrayList<>();
        Map<String,List<ActivityKnowledge.Requirement>> alternatives = new java.util.LinkedHashMap<>();
        for (ActivityKnowledge.Requirement r : entry.requirements()) {
            if (!r.anyOfGroup().isBlank()) {
                alternatives.computeIfAbsent(r.anyOfGroup(), k -> new ArrayList<>()).add(r);
                continue;
            }
            Observation observation = observe(r,account);
            if (observation == Observation.MET) continue;
            if (r.role() == ActivityKnowledge.Role.REQUIRED) {
                if (observation == Observation.UNMET) blockers.add("Requirement not met: " + r.id());
                else checks.add("Verify required: " + r.id());
            } else if (observation == Observation.UNMET) {
                optional.add("Optional/recommended unavailable: " + r.id());
            } else {
                // Unknown optional inventory or gear must not hold a ready activity hostage.
                optional.add("Optional/recommended unobserved: " + r.id());
            }
        }
        // Alternative gates are OR, rather than AND. Unknown never defeats a known
        // met alternative, but one unknown prevents an incorrect hard failure.
        for (var group : alternatives.entrySet()) {
            boolean met = false;
            boolean unknown = false;
            for (ActivityKnowledge.Requirement r : group.getValue()) {
                Observation state = observe(r,account);
                met |= state == Observation.MET;
                unknown |= state == Observation.UNKNOWN;
            }
            if (met) continue;
            boolean required = group.getValue().get(0).role() == ActivityKnowledge.Role.REQUIRED;
            if (!required) optional.add("Optional alternatives unavailable/unobserved: " + group.getKey());
            else if (unknown) checks.add("Verify one alternative for: " + group.getKey());
            else blockers.add("No valid alternative for: " + group.getKey());
        }
        // A required named supply with no reviewed exact item ID must never imply ready.
        // Recommendations remain non-blocking; a bank snapshot is only complete when
        // an independent observer explicitly asserts completeness.
        for (ActivityKnowledge.Supply supply : entry.supplies()) {
            if (supply.role() != ActivityKnowledge.Role.REQUIRED) continue;
            if (supply.itemId() <= 0) {
                checks.add("Resolve required exact item ID and stock: " + supply.name());
                continue;
            }
            Integer amount = account.itemQuantities().get(Integer.toString(supply.itemId()));
            if (amount == null && !account.completeItemSnapshot())
                checks.add("Verify required supply: " + supply.name());
            else if (amount == null || amount < supply.minQuantity())
                blockers.add("Required supply missing: " + supply.name());
        }
        ActivityKnowledge.Travel travel = entry.travel();
        if (travel != null) {
            if (!travel.cooldownGroup().isBlank() && travel.cooldownSeconds() > 0) {
                Boolean ready = account.cooldownReady().get(travel.cooldownGroup());
                if (ready == null) checks.add("Verify shared cooldown: " + travel.cooldownGroup());
                else if (!ready) blockers.add("Shared cooldown active: " + travel.cooldownGroup());
            }
            if (travel.dailyLimit() >= 0) {
                Integer remaining = account.dailyRemaining().get(travel.dailyGroup());
                if (remaining == null) checks.add("Verify remaining daily teleports: " + travel.dailyGroup());
                else if (remaining <= 0) blockers.add("Daily teleports depleted: " + travel.dailyGroup());
            }
        }
        Outcome outcome = !blockers.isEmpty() ? Outcome.BLOCKED :
            !checks.isEmpty() ? Outcome.VERIFY : Outcome.READY;
        return new Result(entry.id(),outcome,blockers,checks,optional,entry.wilderness());
    }

    /** No travel-time claims or ordering optimization. Keeps all 14 researched patch rows visible. */
    public static List<Result> patchReadiness(ActivityKnowledge.Pack pack,
                                               ActivityKnowledge.PatchType type,
                                               AccountEvidence account) {
        Objects.requireNonNull(pack);
        Objects.requireNonNull(type);
        List<Result> results = new ArrayList<>();
        for (ActivityKnowledge.Entry entry : pack.patches(type))
            results.add(evaluate(entry,account));
        return List.copyOf(results);
    }

    /** A Wilderness clue must never authorize the Bank O / automated Best in Bank workflow. */
    public static boolean canUseBestInBank(ActivityKnowledge.Entry clueProfile) {
        Objects.requireNonNull(clueProfile);
        return clueProfile.kind() == ActivityKnowledge.Kind.CLUE_PROFILE &&
               clueProfile.approved() && !clueProfile.wilderness();
    }
}
