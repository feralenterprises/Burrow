package gg.feral.burrow.core;
import java.util.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.*;
import gg.feral.burrow.core.VerifiedOutfitterCatalog.Profile;
/** BUR-0039: precise mechanical and tier integrity checks. Standalone main is runnable offline. */
public final class MuspahGearRegressionTest {
  private static int assertions;
  private static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
  private static Profile muspah(Tier tier){return Objects.requireNonNull(TieredOutfitterCatalog.find("Phantom Muspah","Ranged-only, Twisted bow with Zaryte crossbow shield-phase switch",tier)).profile();}
  public static void main(String[] args) {
    Profile entry=muspah(Tier.ENTRY),recommended=muspah(Tier.RECOMMENDED),optimal=muspah(Tier.OPTIMAL);
    check(entry.gear().size()==11 && recommended.gear().size()==11 && optimal.gear().size()==11,"11 fixed gear slots");
    check(entry.gear().get(0).id()==3749,"Fremennik archer helm, not coif");
    check(entry.gear().get(2).id()==1704,"Entry glory, not endgame fury");
    check(entry.gear().get(4).id()==9185,"Entry one-handed rune crossbow");
    check(entry.gear().get(6).id()==22284,"Entry shield is black d'hide, not blank");
    check(entry.gear().get(8).id()==2491,"Entry Ranged vambraces, not Barrows gloves");
    check(entry.inventory().stream().anyMatch(i->i!=null && i.id()==9240),"Entry sapphire shield-phase bolts preserved");
    check(recommended.gear().get(4).id()==25867,"Recommended Bowfa unchanged");
    check(recommended.gear().get(6)==null,"Two-handed Bowfa must have EMPTY offhand");
    check(recommended.gear().get(9).id()==19933,"Ranged blessed d'hide boots, not melee dragon boots");
    check(recommended.inventory().stream().anyMatch(i->i!=null && i.id()==9185),"Bowfa crossbow switch preserved");
    check(recommended.inventory().stream().anyMatch(i->i!=null && i.id()==9240),"Bowfa sapphire bolts switch preserved");
    check(optimal.gear().get(4).id()==20997 && optimal.gear().get(6)==null,"Optimal Tbow two-handed empty offhand");
    check(optimal.inventory().stream().anyMatch(i->i!=null && i.id()==26374),"Optimal ZCB switch retained");
    check(optimal.inventory().stream().anyMatch(i->i!=null && i.id()==21940),"Optimal sapphire dragon bolts retained");
    check(Final06ProfileMetadata.find("Phantom Muspah","Ranged-only, Twisted bow with Zaryte crossbow shield-phase switch",Tier.ENTRY).questRequirements().stream().anyMatch(x->x.contains("Fremennik")),"Entry quest metadata follows helm");
    check(Final06ProfileMetadata.find("Phantom Muspah","Ranged-only, Twisted bow with Zaryte crossbow shield-phase switch",Tier.RECOMMENDED).itemAcquisitionConditions().stream().anyMatch(x->x.contains("19933")),"Recommended boot metadata updated");
    check(TieredOutfitterCatalog.size()==51,"Unrelated method/tier coverage unchanged");
    System.out.println("BUR-0039 MUSPAH GEAR REGRESSION PASS: "+assertions+" assertions");
  }
}
