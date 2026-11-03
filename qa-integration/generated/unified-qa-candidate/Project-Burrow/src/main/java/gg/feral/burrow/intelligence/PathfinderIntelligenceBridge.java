package gg.feral.burrow.intelligence;

import gg.feral.burrow.pathfinder.SuggestionEngine;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * W06 additive Pathfinder gate. Uses only source-approved activity records and
 * positive same-login observations. No automatic routing, moves, withdrawals,
 * bank-item absence conclusions, diary-state guesses or cooldown inference.
 * Existing suggestions keep their rank and wording when research is CHECK.
 */
public final class PathfinderIntelligenceBridge {
    private PathfinderIntelligenceBridge() {}

    private static final ActivityKnowledge.Pack BUNDLED = load();

    private static ActivityKnowledge.Pack load() {
        try { return ActivityKnowledge.bundled(); }
        catch (IOException | RuntimeException missingOrInvalidResearch) { return null; }
    }

    public static List<SuggestionEngine.Suggestion> enrich(SuggestionEngine.Snapshot account,
                                                             List<SuggestionEngine.Suggestion> suggestions) {
        if (BUNDLED == null || BUNDLED.approvedCount() == 0) return suggestions;
        return enrich(account, suggestions, BUNDLED);
    }

    /** Pure injection point: no RuneLite, bank widget or UI reads. */
    public static List<SuggestionEngine.Suggestion> enrich(SuggestionEngine.Snapshot account,
                                                             List<SuggestionEngine.Suggestion> suggestions,
                                                             ActivityKnowledge.Pack pack) {
        Objects.requireNonNull(suggestions);
        Objects.requireNonNull(pack);
        if (account == null || !account.verifiedIdentity() || pack.approvedCount() == 0)
            return suggestions;
        Map<String,ActivityKnowledge.Entry> exact = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        for (ActivityKnowledge.Entry entry : pack.entries()) {
            if (!entry.approved() || entry.kind() != ActivityKnowledge.Kind.ACTIVITY) continue;
            String key = normalize(entry.name());
            if (exact.containsKey(key)) ambiguous.add(key);
            else exact.put(key, entry);
        }
        ambiguous.forEach(exact::remove);
        if (exact.isEmpty()) return suggestions;
        ActivityDecisions.AccountEvidence evidence = observed(account);
        List<SuggestionEngine.Suggestion> out = new ArrayList<>(suggestions.size());
        boolean changed = false;
        for (SuggestionEngine.Suggestion suggestion : suggestions) {
            ActivityKnowledge.Entry entry = exact.get(normalize(suggestion.title()));
            if (entry == null) { out.add(suggestion); continue; }
            ActivityDecisions.Result check = ActivityDecisions.evaluate(entry, evidence);
            // Intelligence may lower a positive readiness assertion, never manufacture READY.
            SuggestionEngine.State readiness = suggestion.readiness();
            if (check.outcome() == ActivityDecisions.Outcome.BLOCKED)
                readiness = SuggestionEngine.State.UNMET;
            else if (check.outcome() == ActivityDecisions.Outcome.VERIFY &&
                     readiness == SuggestionEngine.State.MET)
                readiness = SuggestionEngine.State.UNKNOWN;
            if (readiness == suggestion.readiness()) { out.add(suggestion); continue; }
            String detail = check.outcome() == ActivityDecisions.Outcome.BLOCKED
                ? " Source-reviewed activity prerequisite unmet."
                : " Source-reviewed activity prerequisite not observed.";
            List<String> missing = new ArrayList<>(suggestion.missing());
            missing.addAll(check.blockers());
            List<String> unknown = new ArrayList<>(suggestion.unverified());
            unknown.addAll(check.checks());
            out.add(new SuggestionEngine.Suggestion(suggestion.id(), suggestion.title(),
                suggestion.why()+detail, readiness, suggestion.requirement(), suggestion.source(),
                suggestion.focus(), suggestion.attention(), suggestion.minimumMinutes(),
                suggestion.category(), suggestion.section(), missing, suggestion.nextStep(),
                suggestion.outfitterBoss(), suggestion.score(), suggestion.goal(), suggestion.optional(),
                unknown, suggestion.afkSeconds(), suggestion.start()));
            changed = true;
        }
        return changed ? List.copyOf(out) : suggestions;
    }

    /** Quest NOT_STARTED is definite; bank absence remains unknown even after a positive-only scan. */
    static ActivityDecisions.AccountEvidence observed(SuggestionEngine.Snapshot account) {
        Map<String,Boolean> flags = new HashMap<>();
        for (var quest : account.quests().entrySet()) {
            switch (quest.getValue()) {
                case FINISHED -> flags.put("QUEST:"+quest.getKey(),true);
                case NOT_STARTED, IN_PROGRESS -> flags.put("QUEST:"+quest.getKey(),false);
                case UNKNOWN -> { /* no inference */ }
            }
        }
        Map<String,Integer> quantities = new HashMap<>();
        if (account.bankStatus() == SuggestionEngine.BankStatus.VERIFIED_SAME_ACCOUNT) {
            for (Integer id : account.observedItemIds()) quantities.put(Integer.toString(id),1);
        }
        // Skills observed as real levels only; no boosted effective levels are invented.
        return new ActivityDecisions.AccountEvidence(account.skills(), Map.of(), null,
            flags, quantities, false, Map.of(), Map.of());
    }

    private static String normalize(String s) {
        return Objects.requireNonNull(s).trim().toLowerCase(Locale.ROOT);
    }
}
