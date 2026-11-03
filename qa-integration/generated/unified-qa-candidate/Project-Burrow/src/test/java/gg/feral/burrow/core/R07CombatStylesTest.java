package gg.feral.burrow.core;
import java.util.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.*;

/** No native RuneLite assumptions. Preserves all original 51, tests new 17 selectors and five item plans. */
public final class R07CombatStylesTest {
  private static int assertions=0;
  private static void ok(boolean yes,String m){assertions++;if(!yes)throw new AssertionError(m);}
  public static void main(String[] args){
    ok(TieredOutfitterCatalog.size()==51,"51 original exact-count invariant");
    ok(Final06ProfileMetadata.size()==51,"51 original metadata unchanged");
    ok(TieredOutfitterCatalog.totalSize()==69,"original 51 + two Brutus + ten R07/W075 + six R08 plans");
    ok(ResearchCombatMethods.size()==17,"all 17 newly researched styles registered");
    ok(ResearchCombatLoadouts.size()==10,"seven original drafts plus three W075 research examples");
    ok(VerifiedOutfitterCatalog.BOSSES.size()==15,"original 15 bosses unchanged");
    int methodCount=0,researchCount=0,ready=0;
    var model=new BurrowModel();
    for(String boss:VerifiedOutfitterCatalog.BOSSES){
      var names=TieredOutfitterCatalog.methods(boss);
      methodCount+=names.size();
      ok(new HashSet<>(names).size()==names.size(),"unique method names: "+boss);
      for(String method:names){
        model.chooseBoss(boss);model.chooseTier(Tier.RECOMMENDED);model.chooseMethod(method);
        ok(model.method().equals(method),"method selected: "+boss+" / "+method);
        var research=ResearchCombatMethods.find(boss,method);
        if(research!=null){
          researchCount++;
          ok(research.url().startsWith("https://oldschool.runescape.wiki/"),"source reference");
          ok(!research.tactics().isBlank()&&!research.safeguards().isBlank(),"encounter guidance");
          ok(research.style().equals(model.selectedStyle()),"research style matches model");
          ok(TieredOutfitterCatalog.find(boss,method,Tier.ENTRY)==null && TieredOutfitterCatalog.find(boss,method,Tier.OPTIMAL)==null,"no invented extra tiers");
        }
        if(model.profile()!=null){
          ready++;
          ok(model.profile().gear().size()==11&&model.profile().inventory().size()==28,"full 11+28 geometry");
          ok(model.profile().boss().equals(boss)&&model.profile().method().equals(method),"no boss mixing");
        }else if(research!=null){
          ok(model.plan()==null,"research-only has no invented gear plan");
        }
      }
    }
    ok(methodCount==36 && researchCount==17,"36 total methods and 17 new research strategies");
    ok(ready==29,"17 existing + 2 Brutus + ten research item plans");
    var bryo=ResearchCombatLoadouts.find("Bryophyta","Fire magic",Tier.RECOMMENDED).profile();
    ok(bryo.gear().get(4).id()==1387,"fire staff");
    ok(bryo.inventory().stream().anyMatch(x->x!=null&&x.id()==1359),"axe for growthlings");
    ok(bryo.inventory().stream().anyMatch(x->x!=null&&x.id()==562&&x.amount()==100),"chaos budget");
    var mole=ResearchCombatLoadouts.find("Giant Mole","Ranged · Bowfa / crossbow",Tier.RECOMMENDED).profile();
    ok(mole.gear().get(4).name().contains("Faerdhinen"),"Bowfa as explicitly named in research");
    ok(mole.gear().get(6)==null,"Bowfa two-handed with no shield");
    ok(mole.inventory().stream().anyMatch(x->x!=null&&x.id()==952),"mole spade");
    var obor=ResearchCombatLoadouts.find("Obor","Magic · earth / snare",Tier.RECOMMENDED).profile();
    ok(obor.inventory().stream().anyMatch(x->x!=null&&x.id()==561&&x.amount()==150),"Snare nature budget");
    ok(obor.inventory().stream().noneMatch(x->x!=null&&x.id()==20754),"giant key must not be treated as recurring combat supply");
    ok(obor.notes().stream().anyMatch(x->x.contains("FIRST ACCESS")&&x.contains("chest")),"first-time key unlock and optional chest consumption must remain explained");
    System.out.println("R07 COMBAT STYLES PASS "+assertions+" assertions: 36 methods / 17 research / ten research full-slot drafts / original 51");
  }
}
