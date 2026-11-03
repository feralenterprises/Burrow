package gg.feral.burrow.host;

import org.junit.jupiter.api.Test;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** These tests require Java 17 JUnit/Gradle and do not simulate a RuneLite host. */
public final class NavigationLifecycleTest {
    static final class Rig {
        final Queue<Runnable> edt = new ArrayDeque<>();
        final AtomicInteger attached = new AtomicInteger();
        final AtomicInteger maxAttached = new AtomicInteger();
        final AtomicInteger adds = new AtomicInteger();
        final AtomicInteger removes = new AtomicInteger();
        final NavigationLifecycle controller = new NavigationLifecycle(edt::add,
            () -> { adds.incrementAndGet(); int n = attached.incrementAndGet(); maxAttached.accumulateAndGet(n, Math::max); },
            () -> { if (attached.getAndSet(0) > 0) removes.incrementAndGet(); });
        void flush() { while (!edt.isEmpty()) edt.remove().run(); }
    }

    @Test void startStopBeforeEdtCannotReattach() {
        Rig r=new Rig(); r.controller.start(); r.controller.stop(); r.flush();
        assertEquals(0, r.attached.get()); assertEquals(0,r.adds.get());
    }
    @Test void rapidStartStopStartOnlyLatestStarts() {
        Rig r=new Rig(); r.controller.start(); r.controller.stop(); r.controller.start(); r.flush();
        assertEquals(1,r.adds.get()); assertEquals(1,r.attached.get()); assertEquals(1,r.maxAttached.get());
        r.controller.stop(); r.flush(); assertEquals(0,r.attached.get()); assertEquals(1,r.removes.get());
    }
    @Test void repeatedStartupNeverDuplicates() {
        Rig r=new Rig(); r.controller.start(); r.flush();
        for(int i=0;i<100;i++) { r.controller.start(); r.flush(); assertEquals(1,r.attached.get()); }
        assertEquals(1,r.maxAttached.get()); assertEquals(101,r.adds.get()); assertEquals(100,r.removes.get());
        r.controller.stop(); r.flush(); assertEquals(0,r.attached.get());
    }
    @Test void queuedStaleStopsCannotRemoveFreshStart() {
        Rig r=new Rig(); r.controller.start(); r.flush();
        r.controller.stop(); r.controller.start(); r.flush();
        assertEquals(1,r.attached.get()); assertEquals(2,r.adds.get());
        r.controller.stop(); r.flush(); assertEquals(0,r.attached.get());
    }
}
