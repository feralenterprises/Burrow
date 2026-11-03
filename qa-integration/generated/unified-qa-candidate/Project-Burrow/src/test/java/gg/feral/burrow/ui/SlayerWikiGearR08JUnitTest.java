package gg.feral.burrow.ui;
import gg.feral.burrow.core.Outfitter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
/** R08: Five owner-supplied OSRS Wiki source tables; advisory example versus ranked columns. */
final class SlayerWikiGearR08JUnitTest {
 @Test void exactlyFiveSourceSectionsAndElevenGearSlots(){
  assertEquals(Set.of("slayer-melee","slayer-melee-hallowfell","slayer-magic-barrage", "slayer-ranged","slayer-ranged-wilderness"),SlayerWikiGear.ids());
  for(String kit:SlayerWikiGear.ids()){
   var table=SlayerWikiGear.find(kit);
   assertEquals(11,table.gear().size());
   assertTrue(table.sourceUrl().startsWith("https://oldschool.runescape.wiki/w/Slayer_training#"));
   assertEquals(11,ActivityOutfitterCatalog.find(kit).equipment().size());
  }
 }
 @Test void wikiRanksAreStillOrderedColumnsAndWithinCellAltsStayTogether(){
  var melee=SlayerWikiGear.find("slayer-melee");
  assertEquals("Soulreaper axe + Scythe of Vitur",melee.slot(4).rankedColumns().get(0));
  assertEquals("Ghrazi rapier / Noxious halberd",melee.slot(4).rankedColumns().get(1));
  assertEquals("Expeditious bracelet / Bracelet of slaughter",melee.slot(8).rankedColumns().get(0));
  var h=SlayerWikiGear.find("slayer-melee-hallowfell");
  assertEquals("Hallowfell",h.slot(4).rankedColumns().get(0));
  assertEquals("Lightbearer",h.slot(10).rankedColumns().get(0));
  assertNull(h.slot(6));
  var barrage=SlayerWikiGear.find("slayer-magic-barrage");
  assertEquals("Kodai wand / Nightmare staff",barrage.slot(4).rankedColumns().get(0));
  assertTrue(barrage.slot(4).rankedColumns().get(2).contains("two-handed"));
  var ranged=SlayerWikiGear.find("slayer-ranged");
  assertEquals("Venator bow",ranged.slot(4).rankedColumns().get(0));
  assertNull(ranged.slot(6));
  assertEquals("Amethyst arrow [bow only] / Moonlight antler bolts [sunlight crossbow only]",ranged.slot(3).rankedColumns().get(0));
  var wild=SlayerWikiGear.find("slayer-ranged-wilderness");
  assertEquals("Webweaver bow",wild.slot(4).rankedColumns().get(0));
  assertEquals("Craw's bow",wild.slot(4).rankedColumns().get(1));
  assertNull(wild.slot(6));
  assertEquals(-1,wild.slot(0).exampleItemId()); // no automatic risky helmet suggestion
 }
 @Test void bothGearPreviewAndSharedBankPlanUseTheSameVerifiedItemIds(){
  for(String kit:SlayerWikiGear.ids()){
   var table=SlayerWikiGear.find(kit);
   var shared=SharedOutfitterSetup.activity(kit);
   assertFalse(shared.bestBankPermitted());
   for(int i=0;i<11;i++){
    var slot=table.slot(i);
    var icon=ActivityItemRefs.find(kit,true,i);
    var planned=shared.plan().cells().get(Outfitter.GEAR_POSITIONS.get(i)).requirement();
    if(slot==null || slot.exampleItemId()<1){assertNull(icon); assertNull(planned);}
    else{
     assertNotNull(icon); assertNotNull(planned);
     assertEquals(slot.exampleItemId(),icon.itemId());
     assertEquals(slot.exampleItemId(),planned.itemId());
     assertEquals(slot.exampleName(),icon.itemName());
     assertTrue(icon.qualification().contains("Wiki example"));
    }
   }
  }
 }
 @Test void wildernessNeverTriggersBankTagOrBestInBank(){
  assertTrue(ActivityOutfitterCatalog.isWilderness("slayer-ranged-wilderness"));
  assertFalse(ActivityOutfitterCatalog.bestBankPermitted("slayer-ranged-wilderness"));
  var wild=SharedOutfitterSetup.activity("slayer-ranged-wilderness");
  assertFalse(wild.bestBankPermitted());assertFalse(wild.bankTagPermitted());
  assertTrue(SharedOutfitterSetup.activity("slayer-melee").bankTagPermitted());
 }
 @Test void defaultWikiExamplesAreNotOwnershipAssertions(){
  for(String kit:SlayerWikiGear.ids()){
   var setup=SharedOutfitterSetup.activity(kit);
   assertEquals("ACTIVITY-REFERENCE",setup.plan().scope().profile());
  }
 }
}
