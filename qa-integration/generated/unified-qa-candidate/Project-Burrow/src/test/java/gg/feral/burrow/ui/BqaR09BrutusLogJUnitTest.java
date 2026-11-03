package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import java.util.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * R09 hotfix, owner native QA: Brutus Collection Log 3/4 unlocked, TrashPanda showed 2/4.
 * Root cause: table held 33089 (FULL bucket); Brutus drops / the log draws "Bottomless milk bucket (empty)" 33091.
 * Synthetic page observation built from the Wiki's 4 Brutus log items.
 */
final class BqaR09BrutusLogJUnitTest {
    static final int MOOLETA = 33101, BUCKET_EMPTY = 33091, BUCKET_FULL = 33089, SLIPPERS = 33093, BEEF = 33124;
    static RareDropStore.State observe(RareDropStore store, int... unlocked) {
        Set<Integer> got = new LinkedHashSet<>(); for (int i : unlocked) got.add(i);
        List<CollectionLogParser.Slot> slots = new ArrayList<>();
        for (int id : new int[]{BUCKET_EMPTY, SLIPPERS, MOOLETA, BEEF}) slots.add(new CollectionLogParser.Slot(id, got.contains(id) ? 0 : 175, 1));
        var obs = CollectionLogParser.parse(List.of("Brutus", "Brutus kills: 42"), slots).orElseThrow();
        return store.observe(42L, obs, 1L);
    }
    static RareDropTable.Encounter brutus() { return RareDropTable.encounter("Brutus").orElseThrow(); }

    @Test void threeOfFourUnlockedIsShownAsThreeOfFour() {
        var mem = new HashMap<String,String>();
        var store = new RareDropStore(mem::get, mem::put);
        var st = observe(store, MOOLETA, BUCKET_EMPTY, SLIPPERS);
        long got = brutus().drops().stream().filter(d -> st.ownership(d.itemId()) == RareDropStore.Ownership.OBSERVED_OBTAINED).count();
        assertEquals(3, got, "was 2 in R08/R09-pre (bucket id mismatch)");
        assertEquals(4, brutus().drops().size());
        var bucket = brutus().drops().stream().filter(d -> d.name().startsWith("Bottomless milk bucket")).findFirst().orElseThrow();
        assertEquals(BUCKET_EMPTY, bucket.itemId());
        assertEquals("Bottomless milk bucket (empty)", bucket.name());
        assertEquals(RareDropStore.Ownership.OBSERVED_NOT_OBTAINED, st.ownership(BEEF));
        assertTrue(st.unmatchedObtained(brutus()).isEmpty());
        // Persistence: a fresh read of the same account shows the same 3/4.
        var again = store.read(42L);
        assertEquals(RareDropStore.Ownership.OBSERVED_OBTAINED, again.ownership(BUCKET_EMPTY));
        assertEquals(RareDropStore.Ownership.UNKNOWN, store.read(43L).ownership(BUCKET_EMPTY), "other account unaffected");
    }

    @Test void fullBucketVariantAlsoCountsAndUnknownUnlocksAreFlagged() {
        var mem = new HashMap<String,String>();
        var store = new RareDropStore(mem::get, mem::put);
        var st = observe(store);
        // A log that drew the FULL form (or an older save) still matches the same drop.
        var withFull = store.observe(42L, CollectionLogParser.parse(List.of("Brutus"),
            List.of(new CollectionLogParser.Slot(BUCKET_FULL, 0, 1), new CollectionLogParser.Slot(99999, 0, 1))).orElseThrow(), 2L);
        assertEquals(RareDropStore.Ownership.OBSERVED_OBTAINED, withFull.ownership(BUCKET_EMPTY));
        assertEquals(Set.of(99999), withFull.unmatchedObtained(brutus()), "unrecognised unlocked id is surfaced, not ignored");
        assertEquals(Set.of(BUCKET_EMPTY, BUCKET_FULL), RareDropTable.equivalentIds(BUCKET_EMPTY));
        assertEquals(Set.of(MOOLETA), RareDropTable.equivalentIds(MOOLETA));
    }

    @Test void savedGoalOnTheOldBucketKeyIsMigrated() {
        var mem = new HashMap<String,String>();
        var store = new RareDropStore(mem::get, mem::put);
        store.toggle(42L, "brutus:33101");
        String key = mem.keySet().iterator().next();
        mem.put(key, mem.get(key) + "S\tbrutus:33089\n"); // as saved by R08 and earlier
        var st = store.read(42L);
        assertTrue(st.selected().contains("brutus:33091"), String.valueOf(st.selected()));
        assertFalse(st.selected().contains("brutus:33089"));
        assertEquals("brutus:33091", RareDropTable.canonicalKey("brutus:33089"));
    }

    @Test void panelShowsCountCheckmarksAndCheckLine() throws Exception {
        var mem = new HashMap<String,String>();
        var store = new RareDropStore(mem::get, mem::put);
        var st = observe(store, MOOLETA, BUCKET_EMPTY, SLIPPERS);
        SwingUtilities.invokeAndWait(() -> {
            var m = new BurrowModel(); m.show(BurrowModel.View.OUTFITTER); m.chooseBoss("Brutus");
            var p = new BurrowPanel(m); p.setRareDropState(42L, st); p.setRareDropExpandedForTest(true);
            var rows = p.rareDropRows();
            assertTrue(rows.stream().anyMatch(r -> r.text().startsWith("Collection Log: 3 / 4 unlocked")), String.valueOf(rows));
            assertTrue(rows.stream().anyMatch(r -> r.kind() == 'd' && r.text().startsWith("✓ Bottomless milk bucket (empty)")));
            assertTrue(rows.stream().anyMatch(r -> r.kind() == 'd' && r.text().startsWith("Beef")), "Beef not ticked");
            assertTrue(rows.stream().noneMatch(r -> r.text().startsWith("CHECK: your log")));
        });
    }
}
