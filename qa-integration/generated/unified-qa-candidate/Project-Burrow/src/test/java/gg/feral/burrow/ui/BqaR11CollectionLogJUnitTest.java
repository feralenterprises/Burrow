package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import java.util.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * R11 owner acceptance: Brutus Collection Log 3/4 = Beef, Mooleta, Bottomless milk bucket; Cow slippers NOT collected.
 * Item ids verified on the OSRS Wiki 2026-11-01: Beef 33124, Mooleta 33101, Bottomless milk bucket (empty) 33091,
 * Cow slippers 33093. Synthetic page reads / script-4100 callbacks; NOT a native capture.
 */
final class BqaR11CollectionLogJUnitTest {
    static final int BEEF = 33124, MOOLETA = 33101, BUCKET = 33091, SLIPPERS = 33093;
    static final long A = 42L, B = 77L;
    static RareDropTable.Encounter brutus() { return RareDropTable.encounter("Brutus").orElseThrow(); }
    static CollectionLogParser.Observation brutusPage(int... unlocked) {
        Set<Integer> got = new HashSet<>(); for (int i : unlocked) got.add(i);
        List<CollectionLogParser.Slot> slots = new ArrayList<>();
        for (int id : new int[]{BUCKET, SLIPPERS, MOOLETA, BEEF}) slots.add(new CollectionLogParser.Slot(id, got.contains(id) ? 0 : 175, got.contains(id) ? 1 : 0));
        return CollectionLogParser.parse(List.of("Brutus", "Brutus kills: 42"), slots).orElseThrow();
    }
    static Map<Integer,RareDropStore.Ownership> states(RareDropStore.State st) {
        Map<Integer,RareDropStore.Ownership> m = new TreeMap<>();
        for (var d : brutus().drops()) m.put(d.itemId(), st.ownership(d.itemId()));
        return m;
    }

    @Test void acceptancePageReadReproducesExactUnlockStates() {
        var mem = new HashMap<String,String>(); var store = new RareDropStore(mem::get, mem::put);
        var st = store.observe(A, brutusPage(BEEF, MOOLETA, BUCKET), 1_000L);
        assertEquals(Map.of(BEEF, RareDropStore.Ownership.OBSERVED_OBTAINED, MOOLETA, RareDropStore.Ownership.OBSERVED_OBTAINED,
            BUCKET, RareDropStore.Ownership.OBSERVED_OBTAINED, SLIPPERS, RareDropStore.Ownership.OBSERVED_NOT_OBTAINED), states(st));
        assertEquals("page", st.unlockEvidence(BEEF).orElseThrow().source());
        assertTrue(st.unlockEvidence(SLIPPERS).isEmpty());
    }

    @Test void searchCaptureFindsHistoricalUnlocksWithoutOpeningThePage() {
        var cap = new CollectionLogCapture();
        // The game's search list fires script 4100 once per obtained item (item id in argument 1), across all bosses.
        int t = 100;
        for (int id : new int[]{BEEF, 4151, MOOLETA, BUCKET}) assertTrue(cap.offer(4100, new Object[]{0, id}, t++));
        assertFalse(cap.offer(4099, new Object[]{0, SLIPPERS}, t), "other scripts ignored");
        assertFalse(cap.offer(4100, new Object[]{0, "x"}, t), "bad argument ignored");
        assertFalse(cap.ready(t), "not complete until 2 quiet ticks");
        assertTrue(cap.ready(t + 1));
        var ids = cap.drain();
        var mem = new HashMap<String,String>(); var store = new RareDropStore(mem::get, mem::put);
        var st = store.observeSearchUnlocks(A, ids, 2_000L);
        assertEquals(RareDropStore.Ownership.OBSERVED_OBTAINED, st.ownership(BEEF));
        assertEquals(RareDropStore.Ownership.OBSERVED_OBTAINED, st.ownership(MOOLETA));
        assertEquals(RareDropStore.Ownership.OBSERVED_OBTAINED, st.ownership(BUCKET));
        assertEquals(RareDropStore.Ownership.UNKNOWN, st.ownership(SLIPPERS), "search is positive-only: absence is not 'not obtained'");
        assertEquals("search", st.unlockEvidence(MOOLETA).orElseThrow().source());
        // Reading the page later turns slippers into an observed NOT-obtained, keeping the three unlocks.
        st = store.observe(A, brutusPage(BEEF, MOOLETA, BUCKET), 3_000L);
        assertEquals(RareDropStore.Ownership.OBSERVED_NOT_OBTAINED, st.ownership(SLIPPERS));
        assertEquals(3, brutus().drops().stream().filter(d -> RareDropStore.Ownership.OBSERVED_OBTAINED == states(store.read(A)).get(d.itemId())).count());
    }

    @Test void cacheReplacementIdsAndVariantsResolveToTheTableItem() {
        var cap = new CollectionLogCapture();
        cap.setReplacements(Map.of(90001, BUCKET));           // e.g. cache enum 3721 maps a display id to the drop id
        cap.offer(4100, new Object[]{0, 90001}, 1);
        var ids = cap.drain();
        assertTrue(ids.contains(90001) && ids.contains(BUCKET));
        var mem = new HashMap<String,String>(); var store = new RareDropStore(mem::get, mem::put);
        assertEquals(RareDropStore.Ownership.OBSERVED_OBTAINED, store.observeSearchUnlocks(A, ids, 1L).ownership(BUCKET));
        // Cow slippers variants (Wiki: 33096-33098) count as the same item; the full milk bucket 33089 too.
        var st = store.observeSearchUnlocks(B, Set.of(33097, 33089), 1L);
        assertEquals(RareDropStore.Ownership.OBSERVED_OBTAINED, st.ownership(SLIPPERS));
        assertEquals(RareDropStore.Ownership.OBSERVED_OBTAINED, st.ownership(BUCKET));
        var page = cap.canonical(brutusPage(MOOLETA));
        assertEquals(brutusPage(MOOLETA).obtained(), page.obtained(), "no replacement for page ids here: unchanged");
    }

    @Test void unlocksAreMonotonicPersistentAndAccountIsolated() {
        var mem = new HashMap<String,String>(); var store = new RareDropStore(mem::get, mem::put);
        store.observe(A, brutusPage(BEEF, MOOLETA, BUCKET), 1L);
        // A mid-draw / partial read showing fewer unlocks never removes an unlock.
        var st = store.observe(A, brutusPage(MOOLETA), 2L);
        assertEquals(RareDropStore.Ownership.OBSERVED_OBTAINED, st.ownership(BEEF));
        assertEquals(RareDropStore.Ownership.OBSERVED_OBTAINED, st.ownership(BUCKET));
        // Restart / sidebar reopen = a fresh read from the same persisted text.
        var reopened = new RareDropStore(mem::get, mem::put).read(A);
        assertEquals(states(st), states(reopened));
        assertEquals(RareDropStore.Ownership.UNKNOWN, store.read(B).ownership(BEEF), "account B never sees A's log");
        store.observeSearchUnlocks(B, Set.of(SLIPPERS), 3L);
        assertEquals(RareDropStore.Ownership.OBSERVED_NOT_OBTAINED, store.read(A).ownership(SLIPPERS), "B's capture never reaches A");
    }

    @Test void unlockIsNotOwnershipNotDropsAndNotKc() {
        var mem = new HashMap<String,String>(); var store = new RareDropStore(mem::get, mem::put);
        var st = store.observe(A, brutusPage(BEEF, MOOLETA, BUCKET), 1L);
        // Historical unlock: Collection Log. KC: only what the page header stated, labelled COLLECTION_LOG.
        assertEquals(42L, st.effective("Brutus").value().getAsLong());
        assertEquals(RareDropStore.Source.COLLECTION_LOG, st.effective("Brutus").source());
        var noKc = store.observeSearchUnlocks(B, Set.of(BEEF), 1L);
        assertTrue(noKc.effective("Brutus").value().isEmpty(), "a search capture carries no KC");
        // Currently owned: only from a bank read. A log unlock with no bank evidence is NOT owned.
        var recipe = new RecipeGoals.Recipe("t", "t", "Brutus", "Mooleta", 1, List.of(), List.of(), "test");
        assertEquals(RecipeGoals.OwnershipSource.NOT_OBSERVED, RecipeGoals.progress(recipe, 1, MOOLETA, null, 0).source());
        // Observed drop quantities: a log unlock adds no session drop.
        var ledger = new LiveSessionLedger();
        ledger.start(new BurrowPanel.SessionScope("acct", "l"), BurrowPanel.SessionOrigin.VERIFIED_BRIDGE, 0);
        assertEquals(0, ledger.dropItemCount());
    }

    @Test void panelShowsThreeOfFourSlippersUnchecked() throws Exception {
        var mem = new HashMap<String,String>(); var store = new RareDropStore(mem::get, mem::put);
        store.observeSearchUnlocks(A, Set.of(BEEF, MOOLETA, BUCKET), 1_000L);
        var st = store.observe(A, brutusPage(BEEF, MOOLETA, BUCKET), 2_000L);
        SwingUtilities.invokeAndWait(() -> {
            var m = new BurrowModel(); m.show(BurrowModel.View.OUTFITTER); m.chooseBoss("Brutus");
            var p = new BurrowPanel(m); p.setHomeClockForTest(() -> 2_000L + 3 * 86_400_000L);
            p.setRareDropState(A, st); p.setRareDropExpandedForTest(true);
            var rows = p.rareDropRows();
            var names = new ArrayList<String>();
            for (var r : rows) if (r.kind() == 'd') names.add(r.text().split("\\|")[0]);
            assertTrue(names.contains("✓ Beef"), String.valueOf(names));
            assertTrue(names.contains("✓ Mooleta"));
            assertTrue(names.contains("✓ Bottomless milk bucket (empty)"));
            assertTrue(names.contains("Cow slippers"), "slippers stay uncollected");
            assertTrue(rows.stream().anyMatch(r -> r.text().startsWith("Collection Log: 3 / 4 unlocked")));
            assertTrue(rows.stream().anyMatch(r -> r.text().equals("Read: page 3d ago · search 3d ago")), String.valueOf(rows));
        });
    }
}
