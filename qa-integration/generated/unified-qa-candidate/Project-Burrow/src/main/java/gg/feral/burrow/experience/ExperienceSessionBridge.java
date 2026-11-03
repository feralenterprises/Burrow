package gg.feral.burrow.experience;

import gg.feral.burrow.core.AccountContext;
import gg.feral.burrow.core.BankEvidence;
import gg.feral.burrow.pathfinder.SuggestionEngine;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * W03: client-thread event bridge for real RuneLite account and login scope. The host supplies
 * already-observed immutable data, not client APIs, and this bridge has no UI or game actions.
 * A transient hop suspends publication but retains the logical login. A confirmed logout or
 * changed account retires its generation. No snapshot is shared without an exact scope match.
 */
public final class ExperienceSessionBridge {
    private final ExperienceSnapshotGate<AccountEvidenceContext> gate = new ExperienceSnapshotGate<>();
    private String accountKey;
    private String loginId;
    private ExperienceSnapshotGate.Ticket ticket;
    private boolean paused = true;

    /** A duplicate LOGGED_IN event in one login is idempotent. */
    public synchronized boolean begin(String key, String login) {
        if (key == null || key.isBlank() || login == null || login.isBlank()) {
            end(); return false;
        }
        if (Objects.equals(accountKey, key) && Objects.equals(loginId, login)) return false;
        accountKey = key;
        loginId = login;
        paused = false;
        ticket = gate.begin(key);
        return true;
    }

    /** LOADING/HOPPING must not retain a claim that the bank is live. */
    public synchronized void pause() {
        if (ticket == null || paused) return;
        paused = true;
        ticket = gate.begin(accountKey);
    }

    /** A witnessed same-login resume requires new evidence, never cached pre-hop truth. */
    public synchronized boolean resume(String key, String login) {
        if (!Objects.equals(accountKey,key) || !Objects.equals(loginId,login)) return begin(key,login);
        if (!paused) return false;
        paused = false;
        ticket = gate.begin(key);
        return true;
    }

    /** Bank mutations and incomplete read cycles immediately retract any published owned claim. */
    public synchronized void invalidateEvidence() {
        if (ticket != null) ticket = gate.begin(accountKey);
    }

    public synchronized boolean observe(String key, String login, SuggestionEngine.Snapshot snapshot,
                                        BankEvidence observedBank, AccountContext gearContext) {
        if (paused || ticket == null || !Objects.equals(accountKey,key) || !Objects.equals(loginId,login)
            || snapshot == null || !snapshot.verifiedIdentity()) return false;
        // W04: Never publish owned-bank evidence if Pathfinder's observation is NOT authenticated
        // to this account. The host can briefly hold a prior/preload bank snapshot while its
        // newer skill/quest snapshot is already marked UNAVAILABLE. Both must agree before
        // suggesting ownership; there is no safe way to infer provenance from a bare item map.
        BankEvidence sameAccountBank = snapshot.bankStatus() == SuggestionEngine.BankStatus.VERIFIED_SAME_ACCOUNT
            ? observedBank : null;
        // CP9 has no trusted diary tier / spellbook witness. UNKNOWN is intentional.
        var account = new AccountEvidenceContext(key,login,ticket.generation(),sameAccountBank,snapshot,
            gearContext, Map.of(),AccountEvidenceContext.Spellbook.UNKNOWN);
        return gate.publish(ticket,account);
    }

    /** Both account and login must match. A-only checks can accidentally reveal an old login. */
    public synchronized Optional<AccountEvidenceContext> current(String key, String login) {
        if (paused || ticket == null || !Objects.equals(accountKey,key) || !Objects.equals(loginId,login))
            return Optional.empty();
        return gate.current(key).filter(a -> a.generation() == ticket.generation()
            && Objects.equals(a.loginId(),login));
    }

    public synchronized void end() {
        paused = true; accountKey = null; loginId = null; ticket = null; gate.clear();
    }

    /** Text diagnostics never disclose a key or a player's bank. */
    @Override public synchronized String toString() {
        return "ExperienceSessionBridge[bound=" + (ticket != null) + ",paused=" + paused + "]";
    }
}
