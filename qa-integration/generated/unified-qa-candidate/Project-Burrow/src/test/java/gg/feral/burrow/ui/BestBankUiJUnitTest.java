package gg.feral.burrow.ui;

import gg.feral.burrow.core.BurrowModel;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BestBankUiJUnitTest {
    @Test void autoBankSelectionNeverStartsManualScanAndWikiProfileNeverMutates() {
        var model=new BurrowModel();
        var panel=new BurrowPanel(model);
        var clicks=new AtomicInteger();
        panel.setBankScanAction(clicks::incrementAndGet);
        model.show(BurrowModel.View.OUTFITTER);
        var wikiGear=model.profile().gear();
        panel.clickAt(160,-8+384);
        assertEquals(0,clicks.get());
        panel.automaticBankResult(Map.of(11665,1),"Auto observed one gear type");
        assertEquals(wikiGear,model.profile().gear());
        panel.clickAt(52,-8+384);
        assertEquals(0,clicks.get());
        assertEquals(wikiGear,model.profile().gear());
        panel.clearBankScan();
        assertEquals(wikiGear,model.profile().gear());
    }
}
