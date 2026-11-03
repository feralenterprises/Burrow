package gg.feral.burrow.experience;

import java.util.Objects;
import java.util.Optional;

/**
 * Generation fence for values computed off the client thread. A worker takes a ticket, computes, then publishes. A
 * ticket from an older generation or another account is refused, so late work never overwrites the current login.
 */
public final class ExperienceSnapshotGate<T> {
    public record Ticket(String accountKey, long generation) {}
    private String accountKey;
    private long generation;
    private T value;

    /** Starts a new generation for this account and drops the previous account's value. */
    public synchronized Ticket begin(String accountKey) {
        // Every generation change, including a relog to the same account, retires old data.
        value = null;
        this.accountKey = accountKey;
        return new Ticket(accountKey, ++generation);
    }

    public synchronized boolean publish(Ticket ticket, T computed) {
        if (ticket == null || computed == null || ticket.generation() != generation
            || !Objects.equals(ticket.accountKey(), accountKey)) return false;
        value = computed;
        return true;
    }

    /** Only the bound account can read the value. */
    public synchronized Optional<T> current(String accountKey) {
        return accountKey != null && accountKey.equals(this.accountKey) ? Optional.ofNullable(value) : Optional.empty();
    }

    public synchronized void clear() { accountKey = null; value = null; generation++; }
}
