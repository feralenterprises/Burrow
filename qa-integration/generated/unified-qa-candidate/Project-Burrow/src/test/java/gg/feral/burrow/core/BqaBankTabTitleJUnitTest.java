package gg.feral.burrow.core;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** BQA UI-01: O-button Bank Tags view gets a readable name, never a raw UUID. */
final class BqaBankTabTitleJUnitTest {
    @Test void readableNoUuidAndBounded() {
        assertEquals("trashpanda vorkath", BankTabTitle.base("Vorkath"));
        assertEquals("trashpanda tormented demons", BankTabTitle.base("Tormented Demons"));
        assertEquals("trashpanda k ril tsutsaroth", BankTabTitle.base("K'ril Tsutsaroth"));
        assertEquals("trashpanda gear", BankTabTitle.base(null));
        for (String boss : BurrowModel.availableBosses()) {
            String n = BankTabTitle.base(boss);
            assertTrue(n.length() <= BankTabTitle.MAX, n);
            assertTrue(n.matches("[a-z0-9 ]+"), n);
            assertFalse(n.matches(".*[0-9a-f]{8}-[0-9a-f]{4}.*"), n);
        }
    }
    @Test void avoidsPlayersSavedTags() {
        Set<String> taken = new HashSet<>(List.of("trashpanda vorkath", "trashpanda vorkath 2"));
        assertEquals(Optional.of("trashpanda vorkath 3"), BankTabTitle.available("Vorkath", taken::contains));
        assertEquals(Optional.empty(), BankTabTitle.available("Vorkath", n -> true));
    }
}
