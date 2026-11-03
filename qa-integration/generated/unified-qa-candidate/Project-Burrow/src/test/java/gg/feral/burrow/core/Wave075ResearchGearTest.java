package gg.feral.burrow.core;

import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Item;
import java.util.List;

/** Independent, executable W07.5 development regressions. No player account or live bank access. */
public final class Wave075ResearchGearTest {
  private static void check(boolean ok,String msg) { if(!ok) throw new AssertionError(msg); }
  private static int id(Item item){return item == null ? -1 : item.id();}
  private static void profile(String boss,String method,int weapon,int ammo) {
    var choice=StyleMethodPicker.choose(boss,
        method.startsWith("Ranged")?CombatStyle.RANGED:CombatStyle.MAGIC,"",Tier.RECOMMENDED);
    check(choice!=null && choice.method().equals(method),boss+" method routing");
    check(choice.coverage()==StyleMethodPicker.Coverage.RESEARCH_GEAR,boss+" must remain research-only");
    check(StyleMethodPicker.isProvisional(boss,method,Tier.RECOMMENDED),boss+" may not feed Bank O");
    var selection=TieredOutfitterCatalog.find(boss,method,Tier.RECOMMENDED);
    check(selection!=null,boss+" must have a gear example");
    var p=selection.profile();
    check(p.gear().size()==11 && p.inventory().size()==28,boss+" 11+28 geometry");
    check(id(p.gear().get(4))==weapon,boss+" wrong weapon ID");
    check(ammo < 0 || id(p.gear().get(3))==ammo,boss+" incompatible ammo ID");
    check(p.notes().stream().anyMatch(n->n.contains("RESEARCH DRAFT")),boss+" missing source gate");
    check(TieredOutfitterCatalog.find(boss,method,Tier.ENTRY)==null,boss+" invented Entry gear");
    check(TieredOutfitterCatalog.find(boss,method,Tier.OPTIMAL)==null,boss+" invented Optimal gear");
  }
  private static boolean inventoryHasId(List<Item> items,int wanted) {
    return items.stream().anyMatch(i -> i!=null && i.id()==wanted);
  }
  public static void main(String[]args) {
    profile("Scurrius","Ranged · rat-bone option",28794,892);
    profile("Scurrius","Magic · rat-bone option",28796,-1);
    profile("Giant Mole","Earth magic",1385,-1);
    var scurR=TieredOutfitterCatalog.find("Scurrius","Ranged · rat-bone option",Tier.RECOMMENDED).profile();
    var scurM=TieredOutfitterCatalog.find("Scurrius","Magic · rat-bone option",Tier.RECOMMENDED).profile();
    var moleM=TieredOutfitterCatalog.find("Giant Mole","Earth magic",Tier.RECOMMENDED).profile();
    check(scurR.gear().get(6)==null,"Bone bow must not equip shield");
    check(scurM.gear().get(3)==null,"Powered bone staff must not require external equipped ammo");
    check(scurM.notes().stream().anyMatch(n->n.contains("PRECHARGED")),"Must verify staff stored charges");
    check(inventoryHasId(moleM.inventory(),556)&&inventoryHasId(moleM.inventory(),562),"Earth Bolt rune budget");
    check(inventoryHasId(moleM.inventory(),4550),"Mole light must be LIT lantern ID 4550");
    var preloaded=BestBankPreloader.knownProfiles();
    for(var boss:List.of("Scurrius","Giant Mole","Obor","Bryophyta"))
      for(var method:TieredOutfitterCatalog.methods(boss))
        if(StyleMethodPicker.isProvisional(boss,method,Tier.RECOMMENDED))
          check(preloaded.stream().noneMatch(k->k.boss().equals(boss)&&k.method().equals(method)),
              "provisional research leaked into Best in Bank preloader: "+boss+"/"+method);
    for(var boss:List.of("Obor","Bryophyta")) {
      int key=boss.equals("Obor")?20754:22374;
      for(var style:List.of(CombatStyle.RANGED,CombatStyle.MAGIC)) {
        var choice=StyleMethodPicker.choose(boss,style,"",Tier.RECOMMENDED);
        check(choice!=null && choice.coverage()==StyleMethodPicker.Coverage.RESEARCH_GEAR,boss+" route changed");
        var p=TieredOutfitterCatalog.find(boss,choice.method(),Tier.RECOMMENDED).profile();
        check(!inventoryHasId(p.inventory(),key),boss+" cannot demand chest key every fight");
        check(p.notes().stream().anyMatch(n->n.contains("FIRST ACCESS")),boss+" missing permanent first-access caveat");
      }
    }
    System.out.println("Wave075ResearchGearTest PASS: 3 styles, 4 keyless follow-up examples, provisional bank gates, lit light");
  }
}
