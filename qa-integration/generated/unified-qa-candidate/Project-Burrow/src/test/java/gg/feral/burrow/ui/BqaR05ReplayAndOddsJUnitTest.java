package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import gg.feral.burrow.ui.BurrowPanel.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** BQA-IMPL-R05: idempotent panel replay (the R04 native JUnit failure) and per-drop odds rows. Synthetic. */
final class BqaR05ReplayAndOddsJUnitTest {
    static final SessionOrigin O = SessionOrigin.VERIFIED_BRIDGE;
    static final AtomicInteger N = new AtomicInteger();
    static String id() { return "r5-" + N.incrementAndGet(); }

    @Test void replayingTheSameSnapshotTwiceIntoOnePanelNeverDoubleCounts() throws Exception {
        // Mirrors BurrowKcHostSafetyRegressionTest.panelReplacementReplaysOnlyCurrentScopeOnce at panel level.
        var scope = new SessionScope("acct", "login");
        var ledger = new LiveSessionLedger(); ledger.start(scope, O, 0);
        ledger.record(new SessionBossKcGain(scope, id(), O, "Vorkath", 1, "kc"));
        ledger.record(new SessionXpGain(scope, id(), O, "Ranged", 300, "xp"));
        AtomicReference<BurrowPanel> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            var p = new BurrowPanel(new BurrowModel());
            assertTrue(p.restoreSessionSnapshot(ledger.replay(BqaR05ReplayAndOddsJUnitTest::id)));
            assertTrue(p.restoreSessionSnapshot(ledger.replay(BqaR05ReplayAndOddsJUnitTest::id)));
            ref.set(p);
        });
        assertEquals(1L, ref.get().sessionReadout().kcByBoss().get("Vorkath").longValue());
        assertEquals(300L, ref.get().sessionReadout().totalXp());
        // A live gain applied to the panel and the ledger, then a third replay: still exactly the ledger total.
        var live = new SessionBossKcGain(scope, id(), O, "Vorkath", 1, "kc");
        ledger.record(live);
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(ref.get().ingestSessionEvent(live));
            assertTrue(ref.get().restoreSessionSnapshot(ledger.replay(BqaR05ReplayAndOddsJUnitTest::id)));
        });
        assertEquals(2L, ref.get().sessionReadout().kcByBoss().get("Vorkath").longValue());
    }

    @Test void retiredOrForeignScopesAreIsolated() throws Exception {
        SessionScope a = new SessionScope("acctA", "l1"), b = new SessionScope("acctB", "l2");
        var la = new LiveSessionLedger(); la.start(a, O, 0); la.record(new SessionBossKcGain(a, id(), O, "Vorkath", 3, "kc"));
        var lb = new LiveSessionLedger(); lb.start(b, O, 0);
        SwingUtilities.invokeAndWait(() -> {
            var p = new BurrowPanel(new BurrowModel());
            p.restoreSessionSnapshot(la.replay(BqaR05ReplayAndOddsJUnitTest::id));
            assertTrue(p.restoreSessionSnapshot(lb.replay(BqaR05ReplayAndOddsJUnitTest::id)), "B replaces A");
            assertTrue(p.sessionReadout().kcByBoss().isEmpty(), "A's KC never shown under B");
            assertFalse(p.restoreSessionSnapshot(la.replay(BqaR05ReplayAndOddsJUnitTest::id)), "retired A cannot come back");
            assertFalse(p.restoreSessionSnapshot(List.of()));
        });
    }

    static RareDropTable.Encounter enc(String n) { return RareDropTable.encounter(n).orElseThrow(); }
    static RareDropStore.Effective kc(long n, RareDropStore.Source s) { return new RareDropStore.Effective(OptionalLong.of(n), s, 1L); }

    @Test void perDropOddsUseOneMinusQPowNAndSeparateNextKill() {
        var e = enc("Phantom Muspah");
        var shard = e.drops().stream().filter(d -> d.name().equals("Venator shard")).findFirst().orElseThrow();
        var r = RareDropRowOdds.row(e, shard, kc(60, RareDropStore.Source.HISCORES));
        assertEquals(RareDropRowOdds.Kind.CUMULATIVE, r.kind());
        assertEquals(1 - Math.pow(0.99, 60), r.byKc().getAsDouble(), 1e-12);
        assertEquals(0.01, r.perKill(), 1e-15, "next kill is independent of history");
        assertTrue(r.detail().startsWith("by KC 60: "), r.detail());
        assertFalse(r.detail().contains("manual"));
        var manual = RareDropRowOdds.row(e, shard, kc(60, RareDropStore.Source.MANUAL));
        assertTrue(manual.detail().contains("(manual KC)"));
    }

    @Test void unknownKcIsUnknownNeverZeroAndMultiRollIsPerKill() {
        var z = enc("Zulrah");
        var fang = z.drops().stream().filter(d -> d.name().equals("Magic fang")).findFirst().orElseThrow();
        var unk = RareDropRowOdds.row(z, fang, new RareDropStore.Effective(OptionalLong.empty(), null, 0));
        assertEquals(RareDropRowOdds.Kind.KC_UNKNOWN, unk.kind());
        assertTrue(unk.byKc().isEmpty());
        assertTrue(unk.detail().startsWith("KC UNKNOWN"));
        assertFalse(unk.detail().matches(".*(^|[^0-9.])0(\\.0+)?%.*"), "never a 0% chance: " + unk.detail());
        assertEquals(1 - Math.pow(1 - 1.0 / 1024, 2), unk.perKill(), 1e-15, "2 rolls per kill");
        assertEquals(RareDropRowOdds.Kind.KC_UNKNOWN, RareDropRowOdds.row(z, fang, null).kind());
        var known = RareDropRowOdds.row(z, fang, kc(512, RareDropStore.Source.COLLECTION_LOG));
        assertEquals(1 - Math.pow(1 - 1.0 / 1024, 1024), known.byKc().getAsDouble(), 1e-12);
    }

    @Test void conditionalAndDuplicateProtectedDropsGetNoCumulativeFigure() {
        for (var e : RareDropTable.all()) for (var d : e.drops()) {
            var r = RareDropRowOdds.row(e, d, kc(100, RareDropStore.Source.CHAT));
            if (d.condition() != null || e.setTableChance() > 0) {
                assertEquals(RareDropRowOdds.Kind.NOT_FIXED_RATE, r.kind(), e.encounter() + " " + d.name());
                assertTrue(r.byKc().isEmpty());
            } else assertEquals(RareDropRowOdds.Kind.CUMULATIVE, r.kind());
        }
    }

    @Test void everyRareDropRowCarriesItemIdRateAndOddsLine() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Set<Integer> requested = new HashSet<>();
            var m = new BurrowModel();
            var p = new BurrowPanel(m, id -> { requested.add(id); return null; });
            for (String boss : BurrowModel.availableBosses()) {
                m.chooseBoss(boss); p.setRareDropExpandedForTest(true);
                for (var r : p.rareDropRows()) if (r.kind() == 'd') {
                    String[] parts = r.text().split("\\|", 7);
                    assertEquals(7, parts.length, boss);
                    assertFalse(parts[4].isBlank(), "card label");
                    assertTrue(parts[5].isEmpty(), "signed out: no highlighted % without a KC");
                    assertTrue(parts[6].contains("next kill"), "rate line");
                    assertFalse(parts[1].isBlank(), "rate");
                    assertTrue(parts[2].startsWith("KC UNKNOWN") || parts[2].startsWith("conditional") || parts[2].startsWith("duplicate-protected"),
                        "signed out: never a % from an invented KC: " + parts[2]);
                    assertTrue(Integer.parseInt(parts[3]) > 0);
                }
            }
            m.show(BurrowModel.View.OUTFITTER);
            p.setSize(225, 2000);
            m.chooseBoss("Phantom Muspah"); p.setRareDropExpandedForTest(true);
            var img = new java.awt.image.BufferedImage(225, 2000, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g = img.createGraphics(); p.paint(g); g.dispose();
            assertTrue(requested.contains(27614), "Venator shard sprite requested from the host item-image provider");
        });
    }
}
