package gg.feral.burrow.runelite;

import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.ui.SharedOutfitterSetup;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Native Runelite test-source seam: shared setup is used by the SAME O controller. */
class SharedOutfitterBankR07JUnitTest {
    @Test void scurriusToSlayerAndBackRetiresLayoutAndUsesActualSelectedSetup() {
        var h=new OutfitterBankJUnitTest.HeaderHarness();
        var model=new BurrowModel();
        var provider=new OutfitterBankJUnitTest.Provider();
        var selected=new AtomicReference<>(SharedOutfitterSetup.boss(model.bankSelection()));
        var epoch=new AtomicLong();
        var bank=new OutfitterBank(h.client,model::bankSelection,()->provider);
        bank.setSetupSelection(selected::get);
        bank.setSelectionEpoch(epoch::get);
        bank.refresh();
        Runnable click=()->{
            h.press++;
            var control=h.header.all.stream().filter(e->e.text.equals("O") && !e.hidden).findFirst().orElseThrow();
            h.header.op(control.widget,1);
        };
        click.run();
        assertTrue(provider.tag.startsWith("trashpanda scur"),provider.tag);
        assertTrue(provider.tag.length()<=30);
        assertEquals(10828,provider.layout.getLayout()[1]);
        selected.set(SharedOutfitterSetup.activity("slayer-melee"));epoch.incrementAndGet();
        bank.refresh();
        assertNull(provider.layout,"Old boss view must close before the activity O action");
        click.run();
        assertTrue(provider.tag.startsWith("trashpanda slay"),provider.tag);
        assertTrue(provider.tag.length()<=30);
        assertEquals(11865,provider.layout.getLayout()[1]);
        assertEquals(4151,provider.layout.getLayout()[16]);
        selected.set(SharedOutfitterSetup.boss(model.bankSelection()));epoch.incrementAndGet();
        bank.refresh();assertNull(provider.layout);
        click.run();
        assertTrue(provider.tag.startsWith("trashpanda scur"),provider.tag);
        assertTrue(provider.tag.length()<=30);
        assertEquals(10828,provider.layout.getLayout()[1]);
    }
    @Test void quickAtoBtoARetiresOldLayoutOnEpochEvenWhenSetupIsEqualAgain() {
        var h=new OutfitterBankJUnitTest.HeaderHarness();var model=new BurrowModel();
        var provider=new OutfitterBankJUnitTest.Provider();var selected=new AtomicReference<>(SharedOutfitterSetup.boss(model.bankSelection()));
        var epoch=new AtomicLong();var bank=new OutfitterBank(h.client,model::bankSelection,()->provider);
        bank.setSetupSelection(selected::get);bank.setSelectionEpoch(epoch::get);bank.refresh();
        h.press++;var control=h.header.all.stream().filter(e->e.text.equals("O") && !e.hidden).findFirst().orElseThrow();
        h.header.op(control.widget,1);assertNotNull(provider.layout);
        selected.set(SharedOutfitterSetup.activity("birdhouse"));epoch.incrementAndGet();
        selected.set(SharedOutfitterSetup.boss(model.bankSelection()));epoch.incrementAndGet();
        bank.refresh();assertNull(provider.layout);assertEquals(1,provider.closes);
    }
    @Test void wildernessNeverCreatesABankTagEvenIfStaleBestBankFlagWasTrue() {
        var h=new OutfitterBankJUnitTest.HeaderHarness();var m=new BurrowModel();
        var provider=new OutfitterBankJUnitTest.Provider();var bank=new OutfitterBank(h.client,m::bankSelection,()->provider);
        bank.setSetupSelection(()->SharedOutfitterSetup.activity("slayer-ranged-wilderness"));
        bank.setBestBankRequested(()->true);bank.refresh();
        assertFalse(bank.chestHasPlan());
        h.press++;var control=h.header.all.stream().filter(e->e.text.equals("O") && !e.hidden).findFirst().orElseThrow();
        h.header.op(control.widget,1);
        assertEquals(0,provider.opens);assertNull(provider.layout);
    }
    @Test void ownerClueBankTagPositionsRemainExactAfterSelection() {
        var h=new OutfitterBankJUnitTest.HeaderHarness();var m=new BurrowModel();
        var provider=new OutfitterBankJUnitTest.Provider();var bank=new OutfitterBank(h.client,m::bankSelection,()->provider);
        var selected=SharedOutfitterSetup.activity("clue-master");
        bank.setSetupSelection(()->selected);bank.refresh();
        h.press++;var control=h.header.all.stream().filter(e->e.text.equals("O") && !e.hidden).findFirst().orElseThrow();
        h.header.op(control.widget,1);
        assertArrayEquals(selected.sourcedBankLayout(),provider.layout.getLayout());
    }
}
