package gg.feral.burrow.core;

import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

final class TierAwareBankResolverJUnitTest {
    @Test void recommendedBrutusCanActuallyDisplayOwnedEntryArmourWithoutMutatingWiki() {
        var wiki=TieredOutfitterCatalog.find("Brutus","Standard free-to-play",Tier.RECOMMENDED).profile();
        var before=List.copyOf(wiki.gear().stream().map(x->x==null?"EMPTY":x.name()).toList());
        var result=TierAwareBankResolver.resolve("Brutus","Standard free-to-play",Tier.RECOMMENDED,
            Map.of(1161,1,1111,1,1091,1,1333,1,1201,1));
        assertEquals(TierAwareBankResolver.Kind.LOWER_TIER_OWNED,result.slots().get(0).kind());
        assertEquals(1161,result.slots().get(0).displayed().id());
        assertEquals(Tier.ENTRY,result.slots().get(0).fromTier());
        assertEquals(1111,result.slots().get(5).displayed().id());
        assertEquals(1091,result.slots().get(7).displayed().id());
        assertEquals(1333,result.slots().get(4).displayed().id());
        assertTrue(result.coreWeaponObserved());
        assertTrue(result.coreShieldObserved());
        assertEquals(3,result.substitutions());
        assertEquals(before,wiki.gear().stream().map(x->x==null?"EMPTY":x.name()).toList());
    }
    @Test void entryTierNeverPullsHigherTierItems() {
        var result=TierAwareBankResolver.resolve("Brutus","Standard free-to-play",Tier.ENTRY,
            Map.of(1163,1,1333,1,1113,1));
        assertEquals(0,result.substitutions());
        assertEquals(1161,result.slots().get(0).displayed().id());
        assertEquals(TierAwareBankResolver.Kind.NOT_OBSERVED,result.slots().get(0).kind());
    }
    @Test void noSnapshotNeverClaimsOwnershipAndNoMagicMissingItemUpgrade() {
        var result=TierAwareBankResolver.resolve("Brutus","Standard free-to-play",Tier.RECOMMENDED,Map.of());
        assertEquals(0,result.confirmed());
        assertEquals(0,result.substitutions());
        assertTrue(result.notRecommended());
    }
    @Test void noWeaponOrRequiredShieldMeansNotRecommendedEvenWhenArmourOwned() {
        var result=TierAwareBankResolver.resolve("Brutus","Standard free-to-play",Tier.RECOMMENDED,
            Map.of(1161,1,1111,1,1091,1));
        assertTrue(result.notRecommended());
        assertFalse(result.coreWeaponObserved());
        // CP-1: a Rune kiteshield is not mechanic protection, so it is not a core blocker; the weapon still is.
        assertTrue(result.coreShieldObserved());
        assertEquals(1333,result.slots().get(4).displayed().id());
        assertEquals(1201,result.slots().get(6).displayed().id());
    }
    @Test void unsafeVariantPlainBerserkerRingCannotPretendToBeImbued() {
        var result=TierAwareBankResolver.resolve("Scurrius","Standard melee",Tier.RECOMMENDED,
            Map.of(6737,1,28792,1,12954,1));
        assertEquals(TierAwareBankResolver.Kind.NOT_OBSERVED,result.slots().get(10).kind());
        assertEquals(11773,result.slots().get(10).displayed().id());
        var reviewed=TierAwareBankResolver.resolve("Scurrius","Standard melee",Tier.RECOMMENDED,
            Map.of(25264,1,28792,1,12954,1));
        assertEquals(TierAwareBankResolver.Kind.REVIEWED_VARIANT,reviewed.slots().get(10).kind());
        assertEquals(25264,reviewed.slots().get(10).displayed().id());
    }
    @Test void cannotSwitchScytheOrShieldUsingLowerTierWithoutCombatCompatibilityProof() {
        // CP-1: the owned Recommended weapon set (Bone mace + Dragon defender) is adopted as a WHOLE in place of
        // the unowned 2H Scythe; the unowned Rada's blessing is optional and no longer vetoes it.
        var result=TierAwareBankResolver.resolve("Scurrius","Standard melee",Tier.OPTIMAL,
            Map.of(28792,1,12954,1));
        assertEquals(28792,result.slots().get(4).displayed().id());
        assertEquals(TierAwareBankResolver.Kind.LOWER_TIER_OWNED,result.slots().get(4).kind());
        assertEquals(12954,result.slots().get(6).displayed().id());
        assertFalse(result.notRecommended());
        // Without the Bone mace there is no owned coherent weapon: still not recommended, Scythe still shown.
        var none=TierAwareBankResolver.resolve("Scurrius","Standard melee",Tier.OPTIMAL,Map.of(12954,1));
        assertEquals(22325,none.slots().get(4).displayed().id());
        assertEquals(TierAwareBankResolver.Kind.NOT_OBSERVED,none.slots().get(4).kind());
        assertTrue(none.notRecommended());
    }
    @Test void stalePreloaderResultIsRetiredOnNextBankSnapshot() throws Exception {
        var key=new BestBankPreloader.Key("Brutus","Standard free-to-play",Tier.RECOMMENDED);
        try(var preload=new BestBankPreloader(List.of(key))) {
            preload.pauseForOutfitter(true);
            preload.observe(Map.of(1161,1,1333,1,1201,1));
            CountDownLatch latch=new CountDownLatch(1);
            AtomicReference<BestBankPreloader.Prepared> a=new AtomicReference<>();
            preload.request(key,p->{a.set(p);latch.countDown();});
            assertTrue(latch.await(3,TimeUnit.SECONDS));
            assertEquals(1,a.get().tierResult().substitutions());
            preload.observe(Map.of(1163,1,1333,1,1201,1));
            assertTrue(preload.cached(key).isEmpty());
            CountDownLatch again=new CountDownLatch(1);
            AtomicReference<BestBankPreloader.Prepared> b=new AtomicReference<>();
            preload.request(key,p->{b.set(p);again.countDown();});
            assertTrue(again.await(3,TimeUnit.SECONDS));
            assertEquals(0,b.get().tierResult().substitutions());
            assertTrue(b.get().generation()>a.get().generation());
        }
    }
}
