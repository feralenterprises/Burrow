package gg.feral.burrow.core;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BestBankPreloaderJUnitTest {
    @Test void pausesWhileOutfitterVisibleButSelectedProfileIsPreparedImmediately() throws Exception {
        var key=new BestBankPreloader.Key("Scurrius","Standard melee",TieredOutfitterCatalog.Tier.RECOMMENDED);
        try(var loader=new BestBankPreloader(List.of(key))) {
            loader.pauseForOutfitter(true);
            loader.observe(Map.of(11773,1));
            Thread.sleep(400);
            assertEquals(0,loader.preparedCount());
            CountDownLatch done=new CountDownLatch(1);
            AtomicReference<BestBankPreloader.Prepared> found=new AtomicReference<>();
            loader.request(key,p->{found.set(p);done.countDown();});
            assertTrue(done.await(3,TimeUnit.SECONDS));
            assertEquals(key,found.get().key());
            assertEquals(BestBankMatches.Status.EXACT,found.get().matches().get(10).status());
            assertEquals(1,loader.preparedCount());
        }
    }
    @Test void changeOfSnapshotInvalidatesPreviousOwnership() throws Exception {
        var key=new BestBankPreloader.Key("Scurrius","Standard melee",TieredOutfitterCatalog.Tier.RECOMMENDED);
        try(var loader=new BestBankPreloader(List.of(key))) {
            loader.pauseForOutfitter(true);
            long old=loader.observe(Map.of(11773,1));
            CountDownLatch first=new CountDownLatch(1);
            loader.request(key,p->first.countDown());
            assertTrue(first.await(3,TimeUnit.SECONDS));
            long next=loader.observe(Map.of(6737,1));
            assertTrue(next>old);
            assertTrue(loader.cached(key).isEmpty());
            CountDownLatch second=new CountDownLatch(1);
            AtomicReference<BestBankPreloader.Prepared> found=new AtomicReference<>();
            loader.request(key,p->{found.set(p);second.countDown();});
            assertTrue(second.await(3,TimeUnit.SECONDS));
            assertEquals(BestBankMatches.Status.NOT_OBSERVED,found.get().matches().get(10).status());
            loader.clear();
            assertEquals(0,loader.preparedCount());
            assertTrue(loader.cached(key).isEmpty());
        }
    }
    @Test void movesSlowlyAndDoesNotProcessAllProfilesInOneTick() throws Exception {
        var keys=BestBankPreloader.knownProfiles();
        // Build 20: 66 Build 19 profiles minus KBD's Wilderness profiles (never preloaded) plus new drafts.
        assertTrue(keys.size()>=60,"preloadable profiles "+keys.size());
        assertTrue(keys.stream().noneMatch(k->BossRegistry.wilderness(k.boss())),"no Wilderness profile is ever preloaded");
        try(var loader=new BestBankPreloader(keys)) {
            loader.observe(Map.of(11773,1));
            Thread.sleep(700);
            assertTrue(loader.preparedCount()>0);
            assertTrue(loader.preparedCount()<loader.profileCount());
        }
    }
}
