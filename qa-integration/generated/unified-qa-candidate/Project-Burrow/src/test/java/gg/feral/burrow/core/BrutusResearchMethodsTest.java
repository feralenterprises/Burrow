package gg.feral.burrow.core;
import java.util.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.*;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.*;
/** Pure-Java 17 assertions; no fake bank ownership or RuneLite host. */
public final class BrutusResearchMethodsTest {
    private static int checked;
    private static void check(boolean c,String m){checked++;if(!c)throw new AssertionError(m);}
    public static void main(String[] args){
        check(TieredOutfitterCatalog.size()==51 && Final06ProfileMetadata.size()==51,"original baseline unchanged");
        check(TieredOutfitterCatalog.totalSize()==69 && ResearchCombatLoadouts.size()==10,"51 original + 2 Brutus + 10 research drafts + 6 demon encounter drafts (Wave 7.5)");
        check(TieredOutfitterCatalog.baselineMethods("Brutus").equals(List.of("Standard free-to-play")),"existing Brutus method untouched");
        check(TieredOutfitterCatalog.methods("Brutus").size()==3,"Brutus UI gets 3 methods");
        var model=new BurrowModel();model.chooseBoss("Brutus");model.chooseTier(Tier.RECOMMENDED);
        for(String method:List.of("Ranged (free-to-play)","Magic (free-to-play)")){
            model.chooseMethod(method);
            var profile=model.profile();check(profile!=null && model.plan()!=null,method+" provides an actual plan");
            check(profile.gear().size()==11 && profile.inventory().size()==28,"full slot geometry");
            check(profile.boss().equals("Brutus") && profile.method().equals(method),"metadata doesn't borrow other boss");
            check(profile.notes().stream().anyMatch(x->x.contains("RESEARCH")),"research labelled");
            check(model.tierSelection().provenance().contains("oldschool.runescape.wiki"),"source retained");
            check(profile.inventory().get(0).id()==33104,"charged cowbell first slot");
            check(profile.inventory().stream().filter(Objects::nonNull).count()==(method.startsWith("Ranged")?13:15),"inventory stack capacity");
            for(var item:profile.gear()) if(item!=null)check(item.id()>0 && item.amount()>0,"positive gear id");
            for(var item:profile.inventory()) if(item!=null)check(item.id()>0 && item.amount()>0,"positive inventory id");
            if(method.startsWith("Ranged")){
                check(profile.style().equals("Ranged"),"correct style");
                check(profile.gear().get(4).id()==853 && profile.gear().get(3).id()==890,"bow and compatible arrows");
                check(profile.gear().get(6)==null,"two-handed bow no shield");
            } else {
                check(profile.style().equals("Magic"),"correct style");
                check(profile.gear().get(4).id()==1385,"earth staff");
                check(profile.inventory().get(1).id()==556 && profile.inventory().get(1).amount()==100,"air rune budget");
                check(profile.inventory().get(2).id()==558 && profile.inventory().get(2).amount()==100,"mind rune budget");
            }
            model.chooseTier(Tier.ENTRY);
            check(model.profile()==null && model.plan()==null,"entry must remain unavailable for "+method);
            model.chooseTier(Tier.OPTIMAL);
            check(model.profile()==null && model.plan()==null,"optimal must remain unavailable for "+method);
            model.chooseTier(Tier.RECOMMENDED);
            check(model.profile()!=null,"recommended reselect recovers");
        }
        model.chooseMethod("Standard free-to-play");
        for(Tier t:Tier.values()){model.chooseTier(t);check(model.profile()!=null,"original Brutus "+t+" preserved");}
        check(TieredOutfitterCatalog.find("Brutus","Ranged (free-to-play)",Tier.ENTRY)==null,"no Entry fallback");
        check(TieredOutfitterCatalog.find("Scurrius","Ranged (free-to-play)",Tier.RECOMMENDED)==null,"no cross boss borrowing");
        System.out.println("BRUTUS RESEARCH ADDITIONS PASS: "+checked+" assertions, original 51 retained, 2 new recommended plans");
    }
}
