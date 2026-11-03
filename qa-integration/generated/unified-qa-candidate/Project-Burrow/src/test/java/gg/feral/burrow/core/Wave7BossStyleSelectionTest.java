package gg.feral.burrow.core;

import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import java.util.List;

/** W07 research-backed multi-style check. Does not manufacture gear or activate staged research. */
public final class Wave7BossStyleSelectionTest {
    static void ok(boolean condition,String detail){if(!condition)throw new AssertionError(detail);}
    static StyleMethodPicker.Choice pick(String boss,CombatStyle style){return StyleMethodPicker.choose(boss,style,"",Tier.RECOMMENDED);}
    static void gear(String boss,CombatStyle style,String expected) {
        var c=pick(boss,style);
        ok(c!=null && c.hasGear(),boss+" "+style+" must have a gear profile");
        ok(c.method().contains(expected),boss+" "+style+" method was "+c.method());
        var p=TieredOutfitterCatalog.find(boss,c.method(),c.gearTier()).profile();
        ok(p.gear().size()==11 && p.inventory().size()==28,boss+" invalid gear geometry");
        ok(p.boss().equals(boss),"cross-boss recommendation");
    }
    public static void main(String[]args){
        gear("Dagannoth Kings",CombatStyle.MAGIC,"Rex");
        gear("Dagannoth Kings",CombatStyle.RANGED,"Prime");
        gear("Dagannoth Kings",CombatStyle.MELEE,"Supreme");
        gear("Brutus",CombatStyle.MELEE,"Standard");
        gear("Brutus",CombatStyle.RANGED,"Ranged");
        gear("Brutus",CombatStyle.MAGIC,"Magic");
        gear("Obor",CombatStyle.MELEE,"Melee");
        gear("Obor",CombatStyle.RANGED,"Ranged");
        gear("Obor",CombatStyle.MAGIC,"Magic");
        gear("Bryophyta",CombatStyle.MELEE,"Melee");
        gear("Bryophyta",CombatStyle.RANGED,"Ranged");
        gear("Bryophyta",CombatStyle.MAGIC,"Fire magic");
        gear("Giant Mole",CombatStyle.MELEE,"Melee");
        gear("Giant Mole",CombatStyle.RANGED,"Ranged");
        // W07.5 replaces these strategy-only routes with visibly provisional gear examples.
        gear("Giant Mole",CombatStyle.MAGIC,"Earth magic");
        gear("Scurrius",CombatStyle.MAGIC,"Magic");
        gear("Scurrius",CombatStyle.RANGED,"Ranged");
        for(var boss:List.of("Giant Mole","Scurrius"))
            for(var style:List.of(CombatStyle.MAGIC,CombatStyle.RANGED)) {
                var selected=pick(boss,style);
                ok(selected!=null && selected.coverage()==StyleMethodPicker.Coverage.RESEARCH_GEAR,
                    boss+" "+style+" must remain provisional");
            }
        var kbdM=pick("King Black Dragon",CombatStyle.MELEE);
        ok(kbdM!=null && kbdM.coverage()==StyleMethodPicker.Coverage.RESEARCH_GEAR,
            "KBD melee must remain provisional");
        ok(StyleMethodPicker.isProvisional("Obor",pick("Obor",CombatStyle.MAGIC).method(),Tier.RECOMMENDED),
            "Obor magic may not enter bank-approved profile set");
        ok(!StyleMethodPicker.isProvisional("Dagannoth Kings",pick("Dagannoth Kings",CombatStyle.MAGIC).method(),Tier.RECOMMENDED),
            "Existing DK source profile must remain usable");
        var b1=TieredOutfitterCatalog.find("Brutus",pick("Brutus",CombatStyle.MELEE).method(),Tier.RECOMMENDED).profile();
        var b2=TieredOutfitterCatalog.find("Brutus",pick("Brutus",CombatStyle.RANGED).method(),Tier.RECOMMENDED).profile();
        var b3=TieredOutfitterCatalog.find("Brutus",pick("Brutus",CombatStyle.MAGIC).method(),Tier.RECOMMENDED).profile();
        ok(b1.gear().get(4).id()!=b2.gear().get(4).id()&&b2.gear().get(4).id()!=b3.gear().get(4).id(),
            "Combat styles must change actual equipment not just label");
        for(var boss:BurrowModel.availableBosses())for(var style:CombatStyle.values()){
            var candidate=pick(boss,style);
            if(candidate==null)continue;
            ok(TieredOutfitterCatalog.methods(boss).contains(candidate.method()),"invented boss method");
        }
        System.out.println("Wave7BossStyleSelectionTest PASS: multi-style gear and research gates");
    }
}
