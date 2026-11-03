package gg.feral.burrow.experience;

import java.util.*;

/**
 * Chooses at most one tip per confirmed login. The choice is deterministic for an account and login, so redraws,
 * navigation and world hops keep it. Done and Dismiss only close the tip. This class never touches the game.
 */
public final class SpoonsTipService {
    /** Tips chosen on recent logins are skipped when another is eligible. */
    public static final int COOLDOWN_LOGINS = 3;
    private SpoonsTipService() {}

    public enum Closed { OPEN, MANUAL_DONE, DISMISSED }
    /** Per-account state for a later host to hold. It stores tip IDs and nothing else. */
    public record State(String accountKey, String loginId, String tipId, Closed closed, List<String> recent) {
        public State { recent = List.copyOf(recent); closed = closed == null ? Closed.OPEN : closed; }
    }
    public record Selection(Optional<SpoonsTip> tip, State state, EvidenceStatus status) {}

    public static Selection select(AccountEvidenceContext account, State prior) {
        if (account == null || !account.identified()) return new Selection(Optional.empty(), null, EvidenceStatus.UNAVAILABLE);
        State state = prior != null && account.accountKey().equals(prior.accountKey()) ? prior : null;
        if (state != null && account.loginId().equals(state.loginId())) return show(state, account);
        List<String> recent = state == null ? List.of() : state.recent();
        var tip = choose(account, recent);
        List<String> nextRecent = new ArrayList<>();
        nextRecent.add(tip.id());
        for (String id : recent) if (nextRecent.size() < COOLDOWN_LOGINS && !nextRecent.contains(id)) nextRecent.add(id);
        return show(new State(account.accountKey(), account.loginId(), tip.id(), Closed.OPEN, nextRecent), account);
    }

    public static State close(State state, Closed how) {
        return state == null ? null : new State(state.accountKey(), state.loginId(), state.tipId(), how, state.recent());
    }

    private static Selection show(State state, AccountEvidenceContext account) {
        if (state.closed() != Closed.OPEN) return new Selection(Optional.empty(), state, EvidenceStatus.OBSERVED);
        var tip = SpoonsTipCatalog.find(state.tipId()).orElseThrow();
        var status = tip.generic() ? EvidenceStatus.GENERIC_REMINDER : EvidenceStatus.OBSERVED;
        return new Selection(Optional.of(new SpoonsTip(tip.id(), tip.text(), null, null, tip.reasons(), tip.sourceUrl(), null, status)), state, status);
    }

    /** Account-specific tips win over the generic reminder. Cooldown is dropped when nothing else is eligible. */
    private static SpoonsTipCatalog.Tip choose(AccountEvidenceContext account, List<String> recent) {
        List<SpoonsTipCatalog.Tip> specific = new ArrayList<>();
        SpoonsTipCatalog.Tip generic = null;
        for (var tip : SpoonsTipCatalog.tips()) {
            if (tip.generic()) generic = tip;
            else if (tip.eligible(account)) specific.add(tip);
        }
        if (specific.isEmpty()) return generic;
        List<SpoonsTipCatalog.Tip> fresh = specific.stream().filter(t -> !recent.contains(t.id())).toList();
        // When every eligible tip is on cooldown, still avoid repeating the last login's tip.
        List<SpoonsTipCatalog.Tip> notLast = specific.stream().filter(t -> recent.isEmpty() || !recent.get(0).equals(t.id())).toList();
        List<SpoonsTipCatalog.Tip> pool = !fresh.isEmpty() ? fresh : notLast.isEmpty() ? specific : notLast;
        return pool.get(Math.floorMod(seed(account.accountKey() + "|" + account.loginId()), pool.size()));
    }

    /** FNV-1a, so the choice does not depend on the JVM. */
    private static int seed(String text) {
        int h = 0x811c9dc5;
        for (byte b : text.getBytes(java.nio.charset.StandardCharsets.UTF_8)) { h ^= b; h *= 16777619; }
        return h;
    }
}
