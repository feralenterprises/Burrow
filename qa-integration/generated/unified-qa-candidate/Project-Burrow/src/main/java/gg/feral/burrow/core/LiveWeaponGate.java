package gg.feral.burrow.core;

import java.util.Objects;

/**
 * Session epoch fence for asynchronous client-thread reads and later EDT paints.
 * A snapshot prepared for a retired epoch can never be shown for a new login.
 * Does not retain player names, identifiers, bank data, or equipment history.
 */
public final class LiveWeaponGate {
    private long epoch;
    private boolean active;
    private boolean loggedIn;
    private LiveWeapon current = LiveWeapon.offline();

    public synchronized long start() {
        epoch++;
        active = true;
        loggedIn = false;
        current = LiveWeapon.offline();
        return epoch;
    }

    public synchronized long enterLoggedIn() {
        epoch++;
        loggedIn = true;
        current = active ? LiveWeapon.waiting() : LiveWeapon.offline();
        return epoch;
    }

    public synchronized long leaveLoggedIn() {
        epoch++;
        loggedIn = false;
        current = LiveWeapon.offline();
        return epoch;
    }

    public synchronized long stop() {
        epoch++;
        loggedIn = false;
        active = false;
        current = LiveWeapon.offline();
        return epoch;
    }

    public synchronized long epoch() { return epoch; }
    public synchronized boolean isCurrent(long token) { return active && token == epoch; }
    public synchronized boolean canObserve(long token) { return isCurrent(token) && loggedIn; }
    public synchronized LiveWeapon current() { return current; }

    public synchronized boolean accept(long token, LiveWeapon next) {
        if (!canObserve(token)) return false;
        current = Objects.requireNonNull(next, "next");
        return true;
    }
}
