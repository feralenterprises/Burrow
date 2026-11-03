package gg.feral.burrow.host;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Coordinates toolbar attachment on the Swing event thread. No RuneLite classes and no game reads.
 * A later stop/start invalidates already queued EDT work, avoiding resurrected sidebar buttons.
 */
public final class NavigationLifecycle {
    private final AtomicLong generation = new AtomicLong();
    private final Consumer<Runnable> edtQueue;
    private final Runnable attach;
    private final Runnable detach;

    public NavigationLifecycle(Consumer<Runnable> edtQueue, Runnable attach, Runnable detach) {
        this.edtQueue = Objects.requireNonNull(edtQueue);
        this.attach = Objects.requireNonNull(attach);
        this.detach = Objects.requireNonNull(detach);
    }

    public void start() {
        long token = generation.incrementAndGet();
        edtQueue.accept(() -> {
            if (generation.get() != token) return;
            detach.run();  // drop the previous registration before reattaching
            if (generation.get() == token) attach.run();
        });
    }

    public void stop() {
        long token = generation.incrementAndGet();
        edtQueue.accept(() -> {
            if (generation.get() == token) detach.run();
        });
    }
}
