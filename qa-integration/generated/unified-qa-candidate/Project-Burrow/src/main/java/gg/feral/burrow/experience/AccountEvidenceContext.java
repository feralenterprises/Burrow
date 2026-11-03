package gg.feral.burrow.experience;

import gg.feral.burrow.core.AccountContext;
import gg.feral.burrow.core.BankEvidence;
import gg.feral.burrow.pathfinder.SuggestionEngine;
import java.util.*;

/**
 * One immutable account view for every experience provider. It only wraps evidence existing CP9 systems already hold,
 * and it separates "confirmed empty" from "stale" from "never seen". Missing data stays UNKNOWN.
 *
 * @param accountKey opaque account binding, null when identity is not confirmed
 * @param loginId    current login, unchanged by world hops
 * @param generation incremented by the host on account or login change, see {@link ExperienceSnapshotGate}
 * @param diaries    diary tier name to completion. Absent means unknown. CP9 has no diary feed
 * @param spellbook  active spellbook. UNKNOWN until a host feed exists
 */
public record AccountEvidenceContext(String accountKey, String loginId, long generation, BankEvidence bank,
                                     SuggestionEngine.Snapshot snapshot, AccountContext gearContext,
                                     Map<String,Boolean> diaries, Spellbook spellbook) {
    public enum BankState { LIVE_OBSERVED, SNAPSHOT_KNOWN, CONFIRMED_EMPTY, STALE, UNKNOWN }
    public enum Spellbook { STANDARD, ANCIENT, LUNAR, ARCEUUS, UNKNOWN }
    public enum Check { MET, UNMET, UNKNOWN }

    public AccountEvidenceContext {
        gearContext = gearContext == null ? AccountContext.UNKNOWN : gearContext;
        diaries = diaries == null ? Map.of() : Map.copyOf(diaries);
        spellbook = spellbook == null ? Spellbook.UNKNOWN : spellbook;
    }

    /** Safe value for a logged-out or unconfirmed client. Holds no account data. */
    public static AccountEvidenceContext signedOut() {
        return new AccountEvidenceContext(null, null, 0, null, null, AccountContext.UNKNOWN, Map.of(), Spellbook.UNKNOWN);
    }

    public boolean identified() {
        return accountKey != null && !accountKey.isBlank() && loginId != null && !loginId.isBlank()
            && snapshot != null && snapshot.verifiedIdentity();
    }

    public BankState bankState() { return classify(bank); }

    public static BankState classify(BankEvidence evidence) {
        if (evidence == null || !evidence.planningInput()) return BankState.UNKNOWN;
        if (evidence.stale()) return BankState.STALE;
        if (evidence.bank().isEmpty()) return BankState.CONFIRMED_EMPTY;
        return evidence.bankStillOpen() ? BankState.LIVE_OBSERVED : BankState.SNAPSHOT_KNOWN;
    }

    /** Stale or missing evidence proves neither ownership nor absence. */
    public Check owns(int itemId) {
        if (!identified()) return Check.UNKNOWN;
        return switch (bankState()) {
            case UNKNOWN, STALE -> Check.UNKNOWN;
            default -> bank.combined().containsKey(itemId) ? Check.MET : Check.UNMET;
        };
    }

    public Check skill(String name, int minimum) {
        if (!identified()) return Check.UNKNOWN;
        Integer level = snapshot.skills().get(name);
        return level == null ? Check.UNKNOWN : level >= minimum ? Check.MET : Check.UNMET;
    }

    public Check questFinished(String name) {
        if (!identified()) return Check.UNKNOWN;
        var state = snapshot.quests().get(name);
        if (state == null || state == SuggestionEngine.Quest.UNKNOWN) return Check.UNKNOWN;
        return state == SuggestionEngine.Quest.FINISHED ? Check.MET : Check.UNMET;
    }

    public Check diary(String tier) {
        if (!identified()) return Check.UNKNOWN;
        Boolean done = diaries.get(tier);
        return done == null ? Check.UNKNOWN : done ? Check.MET : Check.UNMET;
    }

    public Check onSpellbook(Spellbook wanted) {
        if (!identified() || spellbook == Spellbook.UNKNOWN) return Check.UNKNOWN;
        return spellbook == wanted ? Check.MET : Check.UNMET;
    }

    /** Never prints account data. */
    @Override public String toString() { return "AccountEvidenceContext[identified=" + identified() + ", generation=" + generation + "]"; }
}
