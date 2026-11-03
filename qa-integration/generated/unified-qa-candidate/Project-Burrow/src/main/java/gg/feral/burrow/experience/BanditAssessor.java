package gg.feral.burrow.experience;

import gg.feral.burrow.core.BestBankPlanner;
import gg.feral.burrow.core.BestBankPlanner.Plan;
import gg.feral.burrow.core.BossRegistry;
import gg.feral.burrow.core.ItemFacts;
import gg.feral.burrow.core.OwnedGearStats;
import gg.feral.burrow.core.TieredOutfitterCatalog;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.experience.AccountEvidenceContext.BankState;
import gg.feral.burrow.experience.AccountEvidenceContext.Check;
import gg.feral.burrow.experience.BanditBossCandidate.Readiness;
import gg.feral.burrow.experience.BanditBossCandidate.Reason;
import java.util.*;

/**
 * Turns the existing Best in Bank verdict into a conservative readiness band. All gear truth comes from the planner.
 * A missing dream item is not a blocker, because the planner already steps down a tier. Wilderness bosses never reach
 * the planner. Plans are CPU work, so a host should call this off the client thread, or pass a cached {@link PlanSource}.
 */
public final class BanditAssessor {
    /** Supplies a plan for one method. A host can back this with the Best in Bank preloader cache. */
    /** Legacy offline-only entry. A host cache MUST use withScopedPlans instead. */
    public interface PlanSource { Plan plan(String boss, String method, Tier tier); }

    /** W02-02: immutable proof of who, which login, which bank generation, and which plan input. */
    public record PlanTicket(String accountKey, String loginId, long generation,
                             long bankObservedAtMillis, long carriedAtMillis, boolean bankStillOpen) {}
    public record ScopedPlan(PlanTicket ticket, Plan plan) {}
    public interface ScopedPlanSource { ScopedPlan resolve(String boss, String method, Tier tier, PlanTicket ticket); }

    /** Client-thread evidence takes precedence; unconfirmed login/bank data cannot mint a ticket. */
    public static Optional<PlanTicket> ticket(AccountEvidenceContext account) {
        if(account==null || !account.identified() || account.bank()==null
            || account.bankState()==BankState.UNKNOWN || account.bankState()==BankState.STALE)
            return Optional.empty();
        var bank=account.bank();
        return Optional.of(new PlanTicket(account.accountKey(),account.loginId(),account.generation(),
            bank.observedAtMillis(),bank.carriedAtMillis(),bank.bankStillOpen()));
    }
    /** Owner: these fights are not green on gear alone. */
    private static final Set<String> NOT_COMFORTABLE = Set.of("General Graardor", "K'ril Tsutsaroth");
    private static final int MAX_METHODS = 6;
    private final AccountEvidenceContext account;
    private final PlanSource plans;
    private final ScopedPlanSource scopedPlans;

    /** Legacy offline tests/direct plans only. Never wire unbound cached results to the live host. */
    public BanditAssessor(AccountEvidenceContext account, PlanSource plans) {
        this(account,plans,null);
    }
    private BanditAssessor(AccountEvidenceContext account, PlanSource plans, ScopedPlanSource scopedPlans) {
        this.account = account == null ? AccountEvidenceContext.signedOut() : account;
        this.plans = plans;
        this.scopedPlans = scopedPlans;
    }
    /** Host-safe interface: stale/mismatched results are rejected before any readiness classification. */
    public static BanditAssessor withScopedPlans(AccountEvidenceContext account, ScopedPlanSource scopedPlans) {
        return new BanditAssessor(account,null,scopedPlans);
    }

    public static BanditAssessor withPlanner(AccountEvidenceContext account) {
        var a = account == null ? AccountEvidenceContext.signedOut() : account;
        return new BanditAssessor(a, (boss, method, tier) -> BestBankPlanner.plan(boss, method, tier, a.bank(), a.gearContext(),
            ItemFacts.catalogDerived(), Map.of(), Map.of(), OwnedGearStats.Focus.MATCH));
    }

    /** Best bands first, ties A to Z. Unknown and blocked bosses are left out, so nothing unproven is suggested. */
    public List<BanditBossCandidate> hunts(int limit) {
        List<BanditBossCandidate> out = new ArrayList<>();
        for (var entry : BossRegistry.all()) {
            if (entry.wilderness()) continue;
            var c = assess(entry.id());
            if (c.readiness().compareTo(Readiness.HARD_BUT_DOABLE) <= 0) out.add(c);
        }
        out.sort(Comparator.comparing(BanditBossCandidate::readiness).thenComparing(BanditBossCandidate::bossId));
        return List.copyOf(out.subList(0, Math.min(Math.max(0, limit), out.size())));
    }

    public BanditBossCandidate assess(String boss) {
        var found = BossRegistry.find(boss);
        if (found.isEmpty()) return unknown(boss, Reason.UNKNOWN_BOSS, "Unknown boss", "");
        var entry = found.get();
        if (entry.wilderness()) return unknown(entry.id(), Reason.WILDERNESS_NOT_ASSESSED, "Best in Bank is off in the Wilderness", ChronicleProvider.PVP_WARNING);
        if (!account.identified()) return unknown(entry.id(), Reason.NOT_SIGNED_IN, "Sign in to check this boss", "");
        var access = access(entry);
        if (access.isPresent()) return access.get();
        BankState bank = account.bankState();
        if (bank == BankState.UNKNOWN) return unknown(entry.id(), Reason.NO_BANK_SNAPSHOT, "Open your bank so Burrow can check your gear", "");
        if (bank == BankState.STALE) return unknown(entry.id(), Reason.BANK_STALE, "Your bank snapshot is out of date", "");
        var methods = TieredOutfitterCatalog.methods(entry.id());
        if (methods.isEmpty()) return unknown(entry.id(), Reason.NO_VERIFIED_LOADOUT, "No verified gear plan yet", "");
        // Missing cache entries and planner failures are UNKNOWN, never an implicit READY or a crash.
        // A single independently usable method may still be offered if other methods are unavailable.
        if (plans == null && scopedPlans == null) return unknown(entry.id(), Reason.NO_VERIFIED_LOADOUT, "No verified plan provider", "");
        var currentTicket = scopedPlans == null ? Optional.<PlanTicket>empty() : ticket(account);
        if(scopedPlans != null && currentTicket.isEmpty())
            return unknown(entry.id(), Reason.NO_VERIFIED_LOADOUT, "Account or bank generation cannot be verified", "");
        BanditBossCandidate best = null;
        for (String method : methods.subList(0, Math.min(MAX_METHODS, methods.size()))) {
            Tier top = highestTier(entry.id(), method);
            if (top == null) continue;
            Plan plan;
            try {
                if(scopedPlans != null) {
                    ScopedPlan scoped = scopedPlans.resolve(entry.id(),method,top,currentTicket.orElseThrow());
                    plan = scoped != null && currentTicket.orElseThrow().equals(scoped.ticket())
                        ? scoped.plan() : null;
                } else {
                    plan = plans.plan(entry.id(), method, top);
                }
            } catch (RuntimeException ex) {
                // Providers may be backed by an asynchronously invalidated account-bound cache.
                // Do not expose account data or exception text to a visible recommendation.
                plan = null;
            }
            var c = fromPlan(entry, method, top, plan);
            if (best == null || c.readiness().compareTo(best.readiness()) < 0) best = c;
        }
        return best != null ? best : unknown(entry.id(), Reason.NO_VERIFIED_LOADOUT, "No verified gear plan yet", "");
    }

    /** Some methods stop below Optimal. The planner steps down from here on its own. */
    private static Tier highestTier(String boss, String method) {
        for (int t = Tier.values().length - 1; t >= 0; t--)
            if (TieredOutfitterCatalog.find(boss, method, Tier.values()[t]) != null) return Tier.values()[t];
        return null;
    }

    private Optional<BanditBossCandidate> access(BossRegistry.Entry entry) {
        boolean unknown = false;
        for (String quest : entry.quests()) {
            Check c = account.questFinished(quest);
            if (c == Check.UNMET) return Optional.of(blocked(entry.id(), Reason.ACCESS_NOT_MET, "Needs " + quest, List.of(quest), null));
            unknown |= c == Check.UNKNOWN;
        }
        for (var skill : entry.skills().entrySet()) {
            Check c = account.skill(skill.getKey(), skill.getValue());
            if (c == Check.UNMET) return Optional.of(blocked(entry.id(), Reason.ACCESS_NOT_MET, "Needs " + skill.getValue() + " " + skill.getKey(), List.of(), null));
            unknown |= c == Check.UNKNOWN;
        }
        return unknown ? Optional.of(unknown(entry.id(), Reason.ACCESS_UNKNOWN, "Burrow cannot confirm your access to this boss", "")) : Optional.empty();
    }

    private BanditBossCandidate fromPlan(BossRegistry.Entry entry, String method, Tier requestedTier, Plan plan) {
        String boss = entry.id();
        // Cache results are not interchangeable across bosses, methods or selected tiers.
        // This is an additional identity check, NOT a replacement for the host's account/login/generation gate.
        if (plan == null || !boss.equals(plan.boss()) || !method.equals(plan.method())
                || requestedTier != plan.selectedTier() || plan.shownTier() == null
                || plan.gear() == null || plan.gear().size() != 11) {
            return unknown(boss, Reason.NO_VERIFIED_LOADOUT, "No matching current gear plan", "");
        }
        if (plan.verdict() == BestBankPlanner.Verdict.UNKNOWN
                || plan.verdict() == BestBankPlanner.Verdict.NOT_AVAILABLE_WILDERNESS) {
            return unknown(boss, Reason.NO_VERIFIED_LOADOUT, "Gear plan has not been verified", "");
        }
        List<String> weaponOrAmmo = new ArrayList<>(), switches = new ArrayList<>(), mechanics = new ArrayList<>();
        for (String b : plan.blockers()) {
            if (b.startsWith("CORE Weapon") || b.startsWith("Weapon:") || b.startsWith("CORE Ammo") || b.startsWith("Ammo:")) weaponOrAmmo.add(b);
            else if (b.startsWith("Switch ")) switches.add(b);
            else mechanics.add(b);
        }
        if (!mechanics.isEmpty()) return blocked(boss, Reason.PROTECTION_MISSING, "Missing something this fight requires", mechanics, method);
        // Vorkath must never be presented as safely huntable without proof of dragonfire protection.
        // A melee/2H method whose template does not require a shield is NOT proof of safety;
        // antifire-only alternatives require a separate, sourced mechanics and charge gate.
        if ("Vorkath".equals(boss) && !observedVorkathShield(plan)) {
            return unknown(boss, Reason.PROTECTION_MISSING, "Cannot confirm dragonfire protection for this setup", "");
        }
        // A non-ready verdict without an explained missing item is an unresolved solver state.
        if (plan.verdict() == BestBankPlanner.Verdict.NOT_READY && plan.blockers().isEmpty())
            return unknown(boss, Reason.NO_VERIFIED_LOADOUT, "Planner cannot verify readiness", "");
        if (plan.verdict() == BestBankPlanner.Verdict.VERIFY)
            return unknown(boss, Reason.NO_VERIFIED_LOADOUT, "Gear checks are still unverified", "");
        if (!weaponOrAmmo.isEmpty()) {
            if (account.bankState() == BankState.CONFIRMED_EMPTY && account.bank().inventory().isEmpty() && account.bank().equipment().isEmpty())
                return blocked(boss, Reason.CONFIRMED_EMPTY_BANK, "Your bank and gear are empty", weaponOrAmmo, method);
            return unknown(boss, Reason.NO_REVIEWED_WEAPON_OWNED, "No weapon from the reviewed list was seen. You may still own a suitable one", "");
        }
        boolean aboveEntry = plan.shownTier() != Tier.ENTRY;
        Readiness band;
        Reason reason;
        String text;
        if (!switches.isEmpty()) return unknown(boss, Reason.MISSING_SWITCHES, "Required switches have not been verified", "");
        else if (plan.verdict() == BestBankPlanner.Verdict.READY && aboveEntry) { band = Readiness.COMFORTABLE; reason = Reason.GEAR_READY; text = "Your gear covers the " + plan.shownTier().label() + " plan"; }
        else if (plan.verdict() == BestBankPlanner.Verdict.READY || plan.verdict() == BestBankPlanner.Verdict.READY_SHORT_SUPPLIES) {
            band = Readiness.STRETCH; reason = Reason.ENTRY_GEAR_ONLY; text = "Entry gear or short supplies: expect limited trips"; }
        else if (plan.verdict() == BestBankPlanner.Verdict.VERIFY) {
            band = aboveEntry ? Readiness.STRETCH : Readiness.HARD_BUT_DOABLE; reason = Reason.GEAR_NEEDS_CHECKS; text = "Gear looks workable, with checks to do first"; }
        else { band = Readiness.HARD_BUT_DOABLE; reason = Reason.ENTRY_GEAR_ONLY; text = "Doable with care on the gear you own"; }
        if (NOT_COMFORTABLE.contains(boss) && band == Readiness.COMFORTABLE) { band = Readiness.STRETCH; reason = Reason.OWNER_DIFFICULTY_CAP; text = "A demanding fight even with this gear"; }
        return new BanditBossCandidate(boss, band, reason, text, method, plan.checks().stream().limit(3).toList(), EvidenceStatus.INFERRED_WITH_LIMITS, "",
            new DestinationIntent(DestinationIntent.Kind.OPEN_CHRONICLE_BOSS, boss));
    }

    /** Exact slot evidence, not just a shield-like plan label or the presence of a potion. */
    private static boolean observedVorkathShield(Plan plan) {
        var shield = plan.gear().get(6);
        if (shield == null || shield.shown() == null || shield.status() == null
                || !shield.status().physicallyOwned()) return false;
        String name = shield.shown().name();
        if (name == null) return false;
        String n = name.toLowerCase(Locale.ROOT);
        // Charged ward variants are only accepted if the planner has a physical owned status;
        // the existing planner remains responsible for separate variant/charge requirements.
        return n.contains("anti-dragon shield") || n.contains("dragonfire shield")
                || n.contains("dragonfire ward");
    }

    private static BanditBossCandidate unknown(String boss, Reason code, String text, String warning) {
        return new BanditBossCandidate(boss, Readiness.UNKNOWN, code, text, null, List.of(), EvidenceStatus.UNKNOWN, warning, null);
    }
    private static BanditBossCandidate blocked(String boss, Reason code, String text, List<String> needs, String method) {
        return new BanditBossCandidate(boss, Readiness.BLOCKED, code, text, method, needs, EvidenceStatus.OBSERVED, "",
            new DestinationIntent(DestinationIntent.Kind.OPEN_CHRONICLE_BOSS, boss));
    }
}
