package gg.feral.burrow.ui;

import gg.feral.burrow.core.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Actual panel selection/result handoff; verifies that tier resolver changes
 * the displayed item, not merely a detached test object. */
final class TierAwareBankPanelIntegrationJUnitTest {
    @Test void livePreparedOwnedTierFallbackAppearsInBestBankButNotWiki() throws Exception {
        var model=new BurrowModel();
        model.chooseBoss("Brutus");model.chooseMethod("Standard free-to-play");
        model.chooseTier(Tier.RECOMMENDED);model.show(BurrowModel.View.OUTFITTER);
        var panel=new BurrowPanel(model);
        var key=new BestBankPreloader.Key(model.boss(),model.method(),model.tier());
        var prepared=new AtomicReference<BestBankPreloader.Prepared>();
        try(var preloader=new BestBankPreloader(List.of(key))) {
            preloader.pauseForOutfitter(true);
            preloader.observe(Map.of(1161,1,1111,1,1333,1,1201,1));
            var done=new CountDownLatch(1);
            preloader.request(key,p->{prepared.set(p);done.countDown();});
            assertTrue(done.await(3,TimeUnit.SECONDS));
            Method gear=BurrowPanel.class.getDeclaredMethod("ownedGear",int.class);
            gear.setAccessible(true);
            SwingUtilities.invokeAndWait(()->{
                try {
                    assertEquals(1163,((Item)gear.invoke(panel,0)).id());
                    panel.clickAt(160,panel.sourceYForR13Test()+12-8); // user selects BEST BANK
                    panel.automaticBankResult(prepared.get(),"Auto prepared 1/1");
                    assertEquals(1161,((Item)gear.invoke(panel,0)).id(),
                        "Actual UI must display owned adamant helm from Entry tier");
                    assertEquals(1111,((Item)gear.invoke(panel,5)).id());
                    assertEquals(1163,model.profile().gear().get(0).id(),
                        "Wiki data is never mutated by Best Bank");
                    panel.clickAt(50,panel.sourceYForR13Test()+12-8); // user selects WIKI
                    assertEquals(1163,((Item)gear.invoke(panel,0)).id());
                } catch(Exception e) {throw new RuntimeException(e);}
            });
        }
    }
}
