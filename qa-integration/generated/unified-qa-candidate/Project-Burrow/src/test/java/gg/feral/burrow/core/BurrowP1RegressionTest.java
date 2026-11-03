package gg.feral.burrow.core;

import gg.feral.burrow.core.Outfitter.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/** BUR-0004 independent-minded executable red/green regression, no JUnit/network. */
public final class BurrowP1RegressionTest {
    private static int checks;
    private static void check(boolean b, String message) {
        checks++;
        if (!b) throw new AssertionError(message);
    }
    private static void rejects(Runnable f, String message) {
        boolean rejected = false;
        try { f.run(); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, message);
    }
    private static void testClearReplay() {
        var a = new Scope("A", 11);
        var m = new BurrowModel();
        m.enterAccount(a);
        m.observeBank(new Snapshot(a, 99, Map.of(2434, 3)));
        check(m.plan().bankVerified(), "99 verified");
        m.clearBank();
        check(!m.plan().bankVerified() && m.bank() == null, "clear wipes stock");
        rejects(() -> m.observeBank(new Snapshot(a, 98, Map.of(2434, 99))), "lower revision after clear accepted");
        rejects(() -> m.observeBank(new Snapshot(a, 99, Map.of(2434, 99))), "equal revision after clear accepted");
        check(m.bank() == null && !m.plan().bankVerified(), "reject must not resurrect bank");
        m.observeBank(new Snapshot(a, 100, Map.of(2434, 2)));
        check(m.bank().revision() == 100 && m.bank().items().get(2434) == 2, "higher revision accepted");
        rejects(() -> m.observeBank(new Snapshot(a, 100, Map.of())), "equal active revision accepted");
        m.clearBank();
        rejects(() -> m.observeBank(new Snapshot(a, 100, Map.of())), "second clear reset high-water");
        m.observeBank(new Snapshot(a, 101, Map.of(2434, 1)));
        check(m.bank().revision() == 101, "successive higher accepted");
        m.enterAccount(a);
        rejects(() -> m.observeBank(new Snapshot(a, 100, Map.of())), "no-op enterAccount resets high-water");
    }
    private static void testEpochAndABA() {
        var a11 = new Scope("A", 11);
        var b11 = new Scope("B", 11);
        var a12 = new Scope("A", 12);
        var m = new BurrowModel();
        m.enterAccount(a11);
        m.observeBank(new Snapshot(a11, 88, Map.of(385, 4)));
        m.enterAccount(b11);
        check(m.bank() == null && !m.plan().bankVerified(), "A bank crossed to B");
        m.observeBank(new Snapshot(b11, 1, Map.of(385, 1)));
        rejects(() -> m.enterAccount(a11), "re-entering previously used A epoch accepted");
        check(m.scope().equals(b11) && m.bank().revision() == 1, "failed reentry changed live B state");
        rejects(() -> m.observeBank(new Snapshot(a11, 89, Map.of(385, 77))), "foreign scope accepted");
        m.enterAccount(a12);
        check(m.bank() == null && m.scope().equals(a12), "fresh A epoch still bound to B stock");
        m.observeBank(new Snapshot(a12, 0, Map.of(2434, 1)));
        check(m.bank().revision() == 0 && m.plan().bankVerified(), "new epoch cannot start fresh rev zero");
        m.clearBank();
        rejects(() -> m.observeBank(new Snapshot(a12, 0, Map.of())), "rev zero replay after clear");
        m.observeBank(new Snapshot(a12, 1, Map.of(2434, 2)));
        check(m.plan().bankRevision() == 1, "new epoch advancing normally");
    }
    private static void testEmptyAndFixture() {
        var demo = new BurrowModel();
        demo.clearBank();
        check(!demo.plan().bankVerified(), "cleared empty fixture should be unknown");
        demo.loadDemoBank();
        long first = demo.bank().revision();
        check(first == 1, "first explicitly labeled demo fixture revision must preserve R01");
        demo.clearBank();
        demo.loadDemoBank();
        check(demo.bank().revision() > first, "reloading explicit demo fixture reuses stale revision");
        rejects(() -> demo.observeBank(new Snapshot(demo.scope(), first, Map.of(2434, 500))), "demo fixture stale replay allowed");
        demo.clearBank();
        check(!demo.plan().bankVerified(), "fixture clear didn't blank verified status");
        var model = new BurrowModel();
        var s = new Scope("EMPTY", 20);
        model.enterAccount(s);
        model.clearBank();
        model.observeBank(new Snapshot(s, 0, Map.of()));
        check(model.plan().bankVerified() && model.bank().items().isEmpty(), "new empty snapshot rejected");
    }
    private static void testGeometry() throws Exception {
        var expected = List.of(1,8,9,10,16,17,18,25,32,33,34);
        Field f = Outfitter.class.getField("GEAR_POSITIONS");
        check(Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()), "gear roles not static final");
        Object exposed = f.get(null);
        if (exposed instanceof int[] a) {
            int prior = a[0];
            try {
                a[0] = 4;
                var corrupted = Outfitter.resolve(1, new Scope("GEOMETRY", 1), Outfitter.scurriusSample(), "Recommended", null);
                check(corrupted.gear().size() == 11 && corrupted.inventory().size() == 28, "external mutation corrupted roles");
            } finally { a[0] = prior; }
            // Even if a defensive copy were exposed, a public mutable array is unacceptable.
            throw new AssertionError("GEAR_POSITIONS exposes mutable array instead of read-only authority");
        }
        check(exposed instanceof List<?>, "gear API should expose an immutable list");
        @SuppressWarnings("unchecked") List<Integer> gear = (List<Integer>)exposed;
        check(gear.equals(expected), "historical 11 slot order changed");
        boolean immutable = false;
        try { gear.set(0, 4); } catch (UnsupportedOperationException e) { immutable = true; }
        check(immutable && gear.equals(expected), "external list mutation accepted");
        Set<Integer> spots = new HashSet<>();
        for (int p : gear) check(p >= 0 && p < 56 && spots.add(p), "duplicate/invalid gear position " + p);
        for (int i = 0; i < 28; i++) check(spots.add(Outfitter.inventoryPosition(i)), "inventory overlap " + i);
        check(spots.size() == 39 && 56 - spots.size() == 17, "11/28/17 partition invalid");
        var noBank = Outfitter.resolve(1, new Scope("G",2), Outfitter.scurriusSample(), "Recommended", null);
        check(noBank.cells().size() == 56 && noBank.gear().size() == 11 && noBank.inventory().size() == 28, "bad resolved geometry");
        check(noBank.cells().stream().filter(c -> c.kind()==Kind.SPACE).count() == 17, "spacer count wrong");
        for(int i=0;i<56;i++)check(noBank.cells().get(i).position()==i, "cell index mismatch " + i);
        check(noBank.gear().stream().filter(c -> c.requirement() == null && c.stock()==Stock.UNVERIFIED).count()==1,"unknown gear lost");
        var bank=Outfitter.sampleBank(noBank.scope());
        var stocked=Outfitter.resolve(2,noBank.scope(),Outfitter.scurriusSample(),"Recommended",bank);
        check(stocked.inventory().stream().filter(c -> c.requirement()!=null && c.requirement().itemId()==2434 && c.stock()==Stock.OWNED).count()==3, "duplicate potions overallocated");
        check(stocked.inventory().stream().filter(c -> c.requirement()!=null && c.requirement().itemId()==2434 && c.stock()==Stock.MISSING).count()==1, "missing fourth potion not shown");
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("F01")) testClearReplay();
        if (args.length == 0 || args[0].equals("ABA")) testEpochAndABA();
        if (args.length == 0 || args[0].equals("FIXTURE")) testEmptyAndFixture();
        if (args.length == 0 || args[0].equals("F02")) testGeometry();
        System.out.println("BUR-0004 P1 REGRESSION PASS • " + checks + " checks");
    }
}
