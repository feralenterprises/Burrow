package gg.feral.burrow.core;
import java.util.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Reproduces owner's R13 Brutus ranged Recommended no-lower-tier failure. */
public final class BrutusRangedBestBankJUnitTest {
 @Test public void f2pRangedActuallyUsesWikiAlternativesAndCompatibleAmmo(){
  var r=TierAwareBankResolver.resolve("Brutus","Ranged (free-to-play)",Tier.RECOMMENDED,
    Map.of(849,1,890,44,888,65,1133,1,1097,1,1063,1));
  assertEquals(849,r.slots().get(4).displayed().id());
  assertEquals(888,r.slots().get(3).displayed().id(),"Willow cannot use Adamant arrows");
  assertEquals(65,r.slots().get(3).displayed().amount());
  assertEquals(1133,r.slots().get(5).displayed().id());
  assertEquals(1097,r.slots().get(7).displayed().id());
  assertTrue(r.coreWeaponObserved()&&r.coreAmmoObserved());
  assertFalse(r.notRecommended());
  assertEquals(TierAwareBankResolver.Kind.LOW_STOCK_OWNED,r.slots().get(3).kind());
  assertTrue(r.substitutions()>=4);
 }
 @Test public void withoutCompatibleArrowsCannotRecommendBow(){
  var r=TierAwareBankResolver.resolve("Brutus","Ranged (free-to-play)",Tier.RECOMMENDED,
    Map.of(849,1,890,44));
  assertFalse(r.coreAmmoObserved());assertTrue(r.notRecommended());
  assertEquals(TierAwareBankResolver.Kind.NOT_OBSERVED,r.slots().get(4).kind());
 }
 @Test public void exactMapleWithPartialAdamantArrowsDisplaysOwnedQuantity(){
  var r=TierAwareBankResolver.resolve("Brutus","Ranged (free-to-play)",Tier.RECOMMENDED,
    Map.of(853,1,890,44));
  assertEquals(853,r.slots().get(4).displayed().id());
  assertEquals(890,r.slots().get(3).displayed().id());
  assertEquals(44,r.slots().get(3).displayed().amount());
  assertEquals(TierAwareBankResolver.Kind.LOW_STOCK_OWNED,r.slots().get(3).kind());
 }
 @Test public void prefersTripReadyPairOverHigherBowWithOneArrow(){
  var r=TierAwareBankResolver.resolve("Brutus","Ranged (free-to-play)",Tier.RECOMMENDED,
    Map.of(853,1,890,1,849,1,888,120));
  assertEquals(853,r.slots().get(4).displayed().id(),"Keep owned maple bow with plentiful compatible mithril arrows; do not insist on 1 adamant arrow");
  assertEquals(888,r.slots().get(3).displayed().id());
  assertEquals(100,r.slots().get(3).displayed().amount(),"Proposed trip amount, not falsely reported bank ownership");
  assertEquals(TierAwareBankResolver.Kind.WIKI_ALTERNATIVE_OWNED,r.slots().get(3).kind());
  assertFalse(r.notRecommended());
 }
 @Test public void whenAllPairsAreShortPreferEnoughForMoreAttacks(){
  var r=TierAwareBankResolver.resolve("Brutus","Ranged (free-to-play)",Tier.RECOMMENDED,
    Map.of(853,1,890,2,849,1,888,65));
  assertEquals(853,r.slots().get(4).displayed().id(),"Maple can use plentiful mithril arrows");
  assertEquals(888,r.slots().get(3).displayed().id());
  assertEquals(65,r.slots().get(3).displayed().amount());
  assertEquals(TierAwareBankResolver.Kind.LOW_STOCK_OWNED,r.slots().get(3).kind());
 }
 @Test public void doesNotApplyRangedFallbackToOtherBossOrCombatStyle(){
  var r=TierAwareBankResolver.resolve("Brutus","Magic (free-to-play)",Tier.RECOMMENDED,
    Map.of(849,1,888,100,1133,1));
  assertEquals(0,r.substitutions());assertTrue(r.notRecommended());
 }
}
