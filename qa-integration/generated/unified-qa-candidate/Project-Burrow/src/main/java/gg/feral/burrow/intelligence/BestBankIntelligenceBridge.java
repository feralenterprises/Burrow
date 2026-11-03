package gg.feral.burrow.intelligence;

import gg.feral.burrow.core.AccountContext;
import gg.feral.burrow.core.BankEvidence;
import gg.feral.burrow.core.BestBankPlanner;
import gg.feral.burrow.core.BestBankPlanner.GearCell;
import gg.feral.burrow.core.BestBankPlanner.GearStatus;
import gg.feral.burrow.core.BestBankPlanner.Plan;
import gg.feral.burrow.core.BestBankPlanner.Verdict;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * W03: verified-intelligence guard on each existing Best in Bank candidate.
 *
 * Does not choose gear, allocate stock, create bank tabs, perform withdrawals or
 * read RuneLite. A candidate's 11 worn + 28 inventory cells are never rewritten.
 * Rules must have immutable source receipts and source review approval. W02
 * research is CHECK-only, so its current 80 item IDs have NO live effect.
 *
 * This is an additive safety check: confirmed gear incompatibility can make an
 * otherwise-selected candidate NOT_READY; missing state or skill observations
 * can make it VERIFY. Unknown relations do not veto established legacy methods.
 */
public final class BestBankIntelligenceBridge {
    private BestBankIntelligenceBridge() {}

    private static final GearKnowledge.Pack BUNDLED = readBundled();

    private static GearKnowledge.Pack readBundled() {
        try { return GearKnowledge.bundled(); }
        catch (IOException | RuntimeException e) {
            // No new knowledge is authorized when its resource cannot be read;
            // preserve original legacy planning instead of inventing a rule.
            return null;
        }
    }

    /** Production hook: bundled, provenance-gated facts only. */
    public static Plan enrich(Plan plan, BankEvidence evidence, AccountContext account) {
        if (BUNDLED == null) return plan;
        return enrich(plan, evidence, account, BUNDLED);
    }

    /** Pure injection point for final QA and synthetic development contracts. */
    public static Plan enrich(Plan plan, BankEvidence evidence, AccountContext account,
                              GearKnowledge.Pack knowledge) {
        Objects.requireNonNull(plan);
        Objects.requireNonNull(knowledge);
        if (plan.verdict() == Verdict.NOT_AVAILABLE_WILDERNESS ||
            plan.verdict() == Verdict.UNKNOWN || evidence == null || !evidence.planningInput() ||
            (knowledge.approvedItemCount() == 0 && knowledge.approvedRelationCount() == 0))
            return plan;
        if (account == null) account = AccountContext.UNKNOWN;

        List<String> blockers = new ArrayList<>(plan.blockers());
        List<String> checks = new ArrayList<>(plan.checks());
        Set<String> newBlockers = new HashSet<>();
        Set<String> newChecks = new HashSet<>();
        Map<Integer,Integer> stock = evidence.combined();

        for (int index = 0; index < plan.gear().size(); index++) {
            GearCell cell = plan.gear().get(index);
            if (cell.shown() == null || !ownedGearCell(cell.status())) continue;
            GearKnowledge.Item known = knowledge.find(cell.shown().id());
            if (known == null || !known.approved() || known.identityConflict()) continue;
            // W02 slot enumeration has the same 11 canonical worn positions as the planner.
            if (known.slot() != slotAt(index)) {
                newChecks.add("Intelligence: verify slot for " + known.name());
                continue;
            }
            Integer quantity = evidence.stale() ? null : stock.get(known.itemId());
            GearDecisions.Observation obs = new GearDecisions.Observation(quantity, Map.of(), account.realLevels());
            GearDecisions.Verdict verdict = GearDecisions.usability(known, obs);
            if (verdict == GearDecisions.Verdict.BLOCKED) {
                // Only approved exact-ID state and skill requirements can block.
                newBlockers.add("Intelligence: " + known.name() + " fails verified wear/state requirements");
            } else if (verdict == GearDecisions.Verdict.UNKNOWN) {
                // Charge/activation flags cannot be read from a bank item ID.
                newChecks.add("Intelligence: verify " + known.name() + " skill, charge, or active state");
            } else if (verdict == GearDecisions.Verdict.NOT_OWNED) {
                // An inconsistency between planned ownership and observed stock is not proof
                // of zero stock. Never undo R05's positive-only evidence policy.
                newChecks.add("Intelligence: recheck observed stock of " + known.name());
            }
        }

        GearCell weapon = plan.gear().get(4), ammo = plan.gear().get(3), shield = plan.gear().get(6);
        if (owned(weapon) && owned(ammo)) {
            GearKnowledge.Item w = knowledge.find(weapon.shown().id());
            GearKnowledge.Item a = knowledge.find(ammo.shown().id());
            if (w != null && a != null && w.approved() && a.approved() &&
                !w.identityConflict() && !a.identityConflict()) {
                GearKnowledge.Relation incompatible = knowledge.relation(w.itemId(), a.itemId(),
                    GearKnowledge.RelationKind.AMMO_INCOMPATIBLE);
                GearKnowledge.Relation compatible = knowledge.relation(w.itemId(), a.itemId(),
                    GearKnowledge.RelationKind.AMMO_COMPATIBLE);
                boolean bad = incompatible != null && incompatible.approved();
                boolean good = compatible != null && compatible.approved();
                if (bad && good) {
                    newChecks.add("Intelligence: contradictory approved ammunition relations for " + w.name());
                } else if (bad && !incompatible.requiredContext().isBlank()) {
                    newChecks.add("Intelligence: verify ammunition restriction context for " + w.name());
                } else if (bad) {
                    newBlockers.add("Intelligence: confirmed incompatible weapon/ammo pair " + w.name() + " / " + a.name());
                }
                // An absent directed relation is UNKNOWN; preserve legacy ammo knowledge.
            }
        }
        if (owned(weapon) && owned(shield)) {
            GearKnowledge.Item w = knowledge.find(weapon.shown().id());
            GearKnowledge.Item s = knowledge.find(shield.shown().id());
            if (w != null && s != null && w.approved() && s.approved() &&
                !w.identityConflict() && !s.identityConflict() && GearDecisions.hands(w,s) == GearDecisions.Verdict.BLOCKED)
                newBlockers.add("Intelligence: verified two-handed weapon conflicts with shield");
        }
        if (newChecks.isEmpty() && newBlockers.isEmpty()) return plan;
        for (String reason : newChecks) if (!checks.contains(reason)) checks.add(reason);
        for (String reason : newBlockers) if (!blockers.contains(reason)) blockers.add(reason);
        Verdict resulting = plan.verdict();
        if (!newBlockers.isEmpty()) resulting = Verdict.NOT_READY;
        else if (resulting == Verdict.READY || resulting == Verdict.READY_SHORT_SUPPLIES)
            resulting = Verdict.VERIFY;

        return new Plan(plan.boss(), plan.method(), plan.selectedTier(), plan.shownTier(), resulting,
            plan.gear(), plan.inventory(), blockers, checks, plan.shortages(),
            plan.usedSlots(), plan.freeSlots(), plan.switches(), plan.trust(),
            plan.tierResult(), plan.candidateSummaries(), plan.upgrades(), plan.options());
    }

    private static boolean ownedGearCell(GearStatus status) {
        return status == GearStatus.OWNED || status == GearStatus.OWNED_VARIANT ||
            status == GearStatus.OWNED_SUBSTITUTE || status == GearStatus.LOW_STOCK;
    }
    private static boolean owned(GearCell cell) {
        return cell != null && cell.shown() != null && ownedGearCell(cell.status());
    }
    private static GearKnowledge.Slot slotAt(int i) {
        return switch (i) {
            case 0 -> GearKnowledge.Slot.HEAD;
            case 1 -> GearKnowledge.Slot.CAPE;
            case 2 -> GearKnowledge.Slot.NECK;
            case 3 -> GearKnowledge.Slot.AMMO;
            case 4 -> GearKnowledge.Slot.WEAPON;
            case 5 -> GearKnowledge.Slot.BODY;
            case 6 -> GearKnowledge.Slot.SHIELD;
            case 7 -> GearKnowledge.Slot.LEGS;
            case 8 -> GearKnowledge.Slot.HANDS;
            case 9 -> GearKnowledge.Slot.FEET;
            case 10 -> GearKnowledge.Slot.RING;
            default -> throw new IllegalArgumentException("Invalid worn slot " + i);
        };
    }
}
