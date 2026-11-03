package gg.feral.burrow.core;
import java.util.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.*;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.*;
/** Independent offline method, equipment and switch-geometry test, not a RuneLite/Windows claim. */
public final class R08DemonEncounterTest {
  private static int checks;
  private static void ok(boolean good,String why){checks++;if(!good)throw new AssertionError(why);}
  private static boolean item(List<Item> items,int id){return items.stream().anyMatch(x->x!=null&&x.id()==id);}
  private static int count(List<Item> items,int id){return (int)items.stream().filter(x->x!=null&&x.id()==id).count();}
  private static void trip(String boss,String method,String style){
    var p=TieredOutfitterCatalog.find(boss,method,Tier.RECOMMENDED);
    ok(p!=null,boss+" "+method+" has recommended research profile");
    var profile=p.profile();ok(profile.boss().equals(boss)&&profile.method().equals(method),"no boss bleed");
    ok(profile.style().equals(style),"style match");
    ok(profile.gear().size()==11&&profile.inventory().size()==28,"OSRS geometry");
    ok(profile.notes().stream().anyMatch(s->s.contains("RESEARCH DRAFT")),"no wiki certification");
    ok(profile.inventory().stream().filter(Objects::nonNull).count()<=28,"supply fit");
    ok(TieredOutfitterCatalog.find(boss,method,Tier.ENTRY)==null,"entry unresearched");
    ok(TieredOutfitterCatalog.find(boss,method,Tier.OPTIMAL)==null,"optimal unresearched");
    for(Item i:profile.gear())if(i!=null)ok(i.id()>0&&i.amount()>0,"equip id and count");
    for(Item i:profile.inventory())if(i!=null)ok(i.id()>0&&i.amount()>0,"inventory id and count");
    ok(item(profile.gear(),29589)||item(profile.inventory(),29589),"Emberlight switch present");
    ok(item(profile.gear(),29591)||item(profile.inventory(),29591)||method.contains("Magic"),"Scorching switch or magic alternative");
    ok(profile.notes().stream().anyMatch(s->s.toLowerCase(Locale.ROOT).contains("switch")||s.contains("two-handed")||s.contains("protection prayers")),"switch constraints noted: "+boss+" "+method);
    if(item(profile.gear(),29591)){
      ok(profile.gear().get(6)==null,"bow cannot hold defender");
      ok(item(profile.inventory(),12954),"defender parked in inventory with bow");
    }
    if(item(profile.gear(),29589))ok(item(profile.gear(),12954),"melee + defender equipped");
    if(boss.equals(DemonEncounterLoadouts.TD))ok(item(profile.inventory(),29684),"temple travel item");
    if(boss.equals(DemonEncounterLoadouts.DG))ok(item(profile.inventory(),19564),"royal seed pod route");
  }
  public static void main(String... args){
    ok(VerifiedOutfitterCatalog.BOSSES.size()==15,"original baseline fifteen unchanged");
    ok(TieredOutfitterCatalog.size()==51,"exact original 51 untouched");
    ok(Final06ProfileMetadata.size()==51,"original metadata untouched");
    ok(ResearchCombatLoadouts.size()==7&&ResearchOutfitterAdditions.size()==2,"R07/R06 overlays remain intact");
    ok(DemonEncounterLoadouts.size()==6,"six new demon hybrid research proposals");
    ok(TieredOutfitterCatalog.totalSize()==66,"51+2+7+6 gear templates");
    ok(BurrowModel.availableBosses().size()==17,"17 selectable bosses");
    var model=new BurrowModel();
    for(String boss:DemonEncounterLoadouts.bosses()){
      ok(BurrowModel.availableBosses().contains(boss),"boss registered: "+boss);
      model.chooseBoss(boss);
      ok(model.boss().equals(boss),"model opens "+boss);
      ok(new HashSet<>(model.methods()).size()==model.methods().size(),"no duplicate methods");
      for(String method:model.methods()){
        model.chooseMethod(method);model.chooseTier(Tier.RECOMMENDED);
        ok(model.profile()!=null&&model.plan()!=null,"UI has real proposal for method");
        ok(model.outfitterSubtitle().contains(boss),"correct heading");
      }
    }
    trip(DemonEncounterLoadouts.TD,DemonEncounterLoadouts.MELEE,"Melee");
    trip(DemonEncounterLoadouts.TD,DemonEncounterLoadouts.RANGED,"Ranged");
    trip(DemonEncounterLoadouts.TD,DemonEncounterLoadouts.MAGIC,"Magic");
    trip(DemonEncounterLoadouts.TD,DemonEncounterLoadouts.TRIBRID,"Hybrid");
    trip(DemonEncounterLoadouts.DG,DemonEncounterLoadouts.GORILLA,"Melee");
    trip(DemonEncounterLoadouts.DG,DemonEncounterLoadouts.GORILLA_RANGE,"Ranged");
    var r=TieredOutfitterCatalog.find(DemonEncounterLoadouts.TD,DemonEncounterLoadouts.RANGED,Tier.RECOMMENDED).profile();
    ok(r.gear().get(4).id()==29591&&r.gear().get(6)==null,"two-handed Scorching bow and no defender");
    ok(r.inventory().get(0).id()==29589&&r.inventory().get(1).id()==12954,"Emberlight + defender stored for switching");
    var mage=TieredOutfitterCatalog.find(DemonEncounterLoadouts.TD,DemonEncounterLoadouts.MAGIC,Tier.RECOMMENDED).profile();
    ok(mage.gear().get(4).id()==29594,"Purging staff");
    ok(item(mage.inventory(),556)&&item(mage.inventory(),554)&&item(mage.inventory(),565),"real rune budget");
    var tri=TieredOutfitterCatalog.find(DemonEncounterLoadouts.TD,DemonEncounterLoadouts.TRIBRID,Tier.RECOMMENDED).profile();
    ok(item(tri.inventory(),29591)&&item(tri.inventory(),29594),"three-style switches equipped/packed");
    ok(count(tri.inventory(),556)==1,"no duplicate air rune stack");
    model.chooseBoss("Scurrius");ok(model.profile()!=null,"original 15 boss paths work");
    System.out.println("R08 DEMON ENCOUNTERS PASS: "+checks+" checks, 17 bosses / six new recommended hybrid drafts; old 51 untouched");
  }
}
