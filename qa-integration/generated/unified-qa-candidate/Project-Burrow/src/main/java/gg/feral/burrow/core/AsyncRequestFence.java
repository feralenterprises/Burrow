package gg.feral.burrow.core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * BQA-902 F08 (SESS-05): admission fence for asynchronous callbacks (HiScores lookups).
 * A ticket remembers the exact login scope object, the authenticated account and a fence generation.
 * A callback is admitted only if all three still match; A -> B -> A therefore rejects A's old request
 * even though the account hash matches again, because the login scope is a different object.
 */
public final class AsyncRequestFence {
    public record Ticket(Object scope, long account, long generation) {}
    private final AtomicLong generation = new AtomicLong();

    public Ticket issue(Object scope, long account) { return new Ticket(scope, account, generation.get()); }
    /** Revoke every outstanding ticket (call on logout, account change, plugin stop). */
    public void revokeAll() { generation.incrementAndGet(); }
    public boolean admits(Ticket t, Object currentScope, long currentAccount) {
        return t != null && t.scope() != null && t.scope() == currentScope && t.account() == currentAccount
            && currentAccount != BankCaptureGate.ACCOUNT_UNAVAILABLE && t.generation() == generation.get();
    }
}
