package gg.feral.burrow.core;

import java.util.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.*;
import gg.feral.burrow.ui.BurrowPanel;
import java.awt.event.MouseEvent;

/** Independent 51-plan coverage, slot and switching checks. */
public final class TieredOutfitterTest {
    private static int assertions;
    private static void check(boolean condition,String message) {
        assertions++;if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        BurrowModel model=new BurrowModel();
        check(model.tier()==Tier.RECOMMENDED,"recommended default");
        int[] available=new int[3];int methods=0;
        for(String boss:VerifiedOutfitterCatalog.BOSSES) {
            model.chooseBoss(boss);
            check(!model.methods().isEmpty(),boss+" methods");
            for(String method:List.copyOf(TieredOutfitterCatalog.baselineMethods(boss))) {
                model.chooseMethod(method);methods++;
                for(Tier tier:Tier.values()) {
                    model.chooseTier(tier);
                    var selection=model.tierSelection();
                    check(selection!=null && model.plan()!=null,boss+" "+method+" "+tier+" available");
                    available[tier.ordinal()]++;
                    var profile=selection.profile();
                    check(profile.gear().size()==11 && profile.inventory().size()==28,"geometry");
                    check(profile.boss().equals(boss) && profile.method().equals(method) && profile.style().equals(model.selectedStyle()),"provenance");
                    check(model.bankSelection().boss().equals(boss) && model.bankSelection().plan()==model.plan(),"bank projection selection");
                    check(selection.provenance().contains("oldschool.runescape.wiki"),"source URL");
                    for(int i=0;i<11;i++) {
                        var item=profile.gear().get(i);var cell=model.plan().gear().get(i);
                        check((item!=null && item.id()>0 && item.amount()>0)==(cell.requirement()!=null),"gear cell "+i);
                        if(cell.requirement()!=null)check(cell.requirement().itemId()==item.id() && cell.requirement().amount()==item.amount(),"gear ID/quantity "+i);
                    }
                    for(int i=0;i<28;i++) {
                        var item=profile.inventory().get(i);
                        var cell=model.plan().cells().get(Outfitter.inventoryPosition(i));
                        check((item!=null && item.id()>0 && item.amount()>0)==(cell.requirement()!=null),"inventory cell "+i);
                        if(cell.requirement()!=null)
                            check(cell.requirement().itemId()==item.id() && cell.requirement().amount()==item.amount(),"inventory ID/quantity "+i);
                    }
                }
            }
        }
        check(methods==17 && Arrays.equals(available,new int[]{17,17,17}),"all 51 method/tier plans");
        model.chooseBoss("Scurrius");model.chooseTier(Tier.ENTRY);
        check(model.profile().gear().get(4).id()==1333,"Scurrius pre-spine weapon");
        var entry=model.plan();model.chooseTier(Tier.RECOMMENDED);
        check(model.profile().gear().get(4).id()==28792 && model.plan()!=entry,"R04 bone mace restored");
        model.chooseTier(Tier.OPTIMAL);
        check(model.profile().gear().get(4).id()==22325 && model.profile().gear().get(6)==null,"F06 optimal Scythe two-handed setup");
        model.chooseBoss("Brutus");model.chooseTier(Tier.ENTRY);
        check(model.profile().gear().get(4).id()==1331 && model.profile().gear().get(6).id()==1199,"Brutus accessible adamant gear");
        model.chooseBoss("Dagannoth Kings");model.chooseTier(Tier.RECOMMENDED);
        BurrowPanel panel=new BurrowPanel(model);model.show(BurrowModel.View.OUTFITTER);
        String rex=model.method();
        panel.clickAt(80,316);
        check(model.method().equals(rex),"method hitbox no longer silently cycles strategies");
        // Real method-menu actions are covered by OutfitterMethodDropdownTest.
        model.chooseMethod("Prime, ranged");
        check(model.method().equals("Prime, ranged"),"model selects registered Prime method");
        model.chooseMethod("Supreme, melee");
        check(model.method().equals("Supreme, melee"),"model selects registered Supreme method");
        model.chooseMethod(rex);
        check(model.method().equals(rex),"model restores registered Rex method");
        model.chooseMethod("Prime, ranged");
        check(model.profile().gear().get(4).id()==25867 && model.profile().gear().get(6)==null,"Prime Bowfa crystal, no offhand");
        model.chooseTier(Tier.OPTIMAL);
        check(model.profile().gear().get(4).id()==20997 && model.profile().gear().get(6)==null,"Prime Tbow no Elysian pairing");
        model.chooseMethod("Supreme, melee");
        check(model.profile().style().equals("Melee") && model.plan().template().boss().equals("Dagannoth Kings"),"Supreme own plan");
        model.chooseMethod("Rex safespot with Guthan healing switch");model.chooseTier(Tier.RECOMMENDED);
        check(model.profile().inventory().stream().filter(i->i!=null && i.id()==5952).count()==2,"Rex short-trip antidote identity");
        model.chooseBoss("Obor");check(model.profile()!=null && model.profile().inventory().size()==28,"Obor full plan");
        model.chooseBoss("Bryophyta");
        check(model.profile().inventory().stream().anyMatch(i->i!=null && i.id()==1359),"Bryophyta growthling axe");
        model.chooseBoss("Vorkath");model.chooseTier(Tier.ENTRY);
        check(model.profile().gear().get(4).id()==9185 && model.profile().gear().get(3).id()==9242,"rune crossbow regular bolts");
        check(model.profile().inventory().stream().anyMatch(i->i!=null && i.id()==9243 && i.amount()==150),"regular Diamond bolt switch");
        model.chooseTier(Tier.RECOMMENDED);
        check(model.profile().gear().get(4).id()==21902 && model.profile().gear().get(6).id()==1540,"dragon crossbow and anti-dragon shield progression");
        check(model.profile().inventory().get(2).id()==12913,"F06 recommended anti-venom(4) variant");
        model.chooseTier(Tier.OPTIMAL);
        check(model.profile().gear().get(6).id()==22002,"charged ward ID");
        var sourceTip=panel.getToolTipText(new MouseEvent(panel,MouseEvent.MOUSE_MOVED,0,0,18,1288,0,false));
        check(sourceTip.contains("oldschool.runescape.wiki"),"source provenance tooltip");
        check(VerifiedOutfitterCatalog.find("Vorkath","Ranged crossbow").inventory().get(3).id()<1,"R04 unknown dose preserved");
        model.chooseBoss("Zulrah");model.chooseTier(Tier.OPTIMAL);
        check(model.profile().gear().get(4).id()==27275 && model.profile().gear().get(6)==null,"Shadow two-handed");
        model.chooseBoss("Scurrius");model.chooseTier(Tier.RECOMMENDED);
        check(model.plan()!=null && model.profile().gear().get(4).id()==28792,"switching never borrows prior boss/tier");
        System.out.println("TIERED OUTFITTER PASS: "+assertions+" assertions; 17 methods × 3 tiers = 51 plans");
    }
}
