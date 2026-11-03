package gg.feral.burrow.runelite;

import java.util.Map;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** All synthetic item snapshots; proves only the safety policy, not native bank readiness. */
class BankStockObservationJUnitTest {
    private static ItemComposition def(int id, int note, int placeholder) {
        return BankHeaderFixture.proxy(ItemComposition.class, (method, args) -> switch(method) {
            case "getId" -> id;
            case "getNote" -> note;
            case "getPlaceholderTemplateId" -> placeholder;
            default -> throw new AssertionError("Unexpected composition request: " + method);
        });
    }
    private static ItemContainer bank(Item... items) {
        return BankHeaderFixture.proxy(ItemContainer.class, (method, args) -> switch(method) {
            case "getItems" -> items;
            default -> throw new AssertionError("Unexpected bank request: " + method);
        });
    }
    @Test void normalChargedGloryAndImbuedRingArePositiveEvidence() {
        var observed = BankStockObservation.capture(bank(new Item(11978,1),new Item(25264,2)),
                id -> def(id,-1,-1));
        assertTrue(observed.available());
        assertEquals(java.util.Set.of(11978,25264),observed.usableIds());
        assertTrue(observed.reason().contains("completeness not independently verified"));
    }
    @Test void placeholdersNotedFillerAndZeroQuantityNeverProveUsableGear() {
        var observed = BankStockObservation.capture(bank(
                new Item(25264,0), new Item(6737,1), new Item(11978,1),
                new Item(20594,200), new Item(-1,1), new Item(1704,1)),
                id -> id==6737 ? def(id,-1,14401) :
                        id==11978 ? def(id,799,-1) : def(id,-1,-1));
        assertTrue(observed.available());
        assertEquals(java.util.Set.of(1704),observed.usableIds());
        var ring = gg.feral.burrow.core.ItemVariantPolicy.resolve(11773,observed.usableIds());
        assertFalse(ring.kind()==gg.feral.burrow.core.ItemVariantPolicy.Kind.EXACT);
        assertFalse(ring.kind()==gg.feral.burrow.core.ItemVariantPolicy.Kind.EQUIVALENT_VARIANT);
        var glory = gg.feral.burrow.core.ItemVariantPolicy.resolve(1712,observed.usableIds());
        assertEquals(-1, glory.layoutId());
    }
    @Test void missingBankNullItemsAndFailedDefinitionAllFailClosed() {
        assertFalse(BankStockObservation.capture(null,id->def(id,-1,-1)).available());
        assertFalse(BankStockObservation.capture(bank((Item[])null),id->def(id,-1,-1)).available());
        assertFalse(BankStockObservation.capture(bank(),id->def(id,-1,-1)).available());
        assertFalse(BankStockObservation.capture(bank(new Item(25264,0), new Item(6737,1)),
                id->def(id,-1,14401)).available());
        assertFalse(BankStockObservation.capture(bank(new Item(25264,1)),id->null).available());
        assertFalse(BankStockObservation.capture(bank(new Item(25264,1)),id->{throw new IllegalStateException();}).available());
        assertFalse(BankStockObservation.capture(bank(new Item(25264,1)),id->def(999,-1,-1)).available());
    }
    @Test void plainAndImbuedRingAndChargedAndUnchargedGloryHaveDifferentSemantics() {
        var seen=BankStockObservation.capture(bank(new Item(6737,1),new Item(25264,1),
                new Item(1704,1),new Item(11978,1)),id->def(id,-1,-1)).usableIds();
        assertEquals(25264,gg.feral.burrow.core.ItemVariantPolicy.resolve(11773,seen).layoutId());
        assertEquals(11978,gg.feral.burrow.core.ItemVariantPolicy.resolve(1712,seen).layoutId());
        var unsafe=BankStockObservation.capture(bank(new Item(6737,1),new Item(1704,1)),
                id->def(id,-1,-1)).usableIds();
        assertEquals(-1,gg.feral.burrow.core.ItemVariantPolicy.resolve(11773,unsafe).layoutId());
        assertEquals(-1,gg.feral.burrow.core.ItemVariantPolicy.resolve(1712,unsafe).layoutId());
    }
}
