package gg.feral.burrow.ui;
import gg.feral.burrow.core.BurrowModel;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import javax.swing.SwingUtilities;
/** Existing navigation plus scoped Outfitter controls. */
public final class BurrowHitboxTest {
 static int checks;
 static void ok(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
 public static void main(String[] ignored) throws Exception {
  BurrowModel m=new BurrowModel();BurrowPanel p=new BurrowPanel(m);
  p.clickAt(12,90);ok(m.view()==BurrowModel.View.HOME,"Home");
  p.clickAt(83,94);ok(m.view()==BurrowModel.View.PATHFINDER,"Pathfinder");
  p.clickAt(138,94);ok(m.view()==BurrowModel.View.OUTFITTER,"Outfitter");
  p.clickAt(26,133+10-8);ok(m.boss().equals("Brutus")&&m.plan()!=null,"boss selector advances to Brutus");
  p.clickAt(26,133+10-8);ok(m.boss().equals("Giant Mole")&&m.plan()!=null,"boss selector advances to Mole");
  p.clickAt(180,p.tierYForR13Test()+12-8);ok(m.tier()==Tier.OPTIMAL && m.plan()!=null,"optimal tier selection");
  p.clickAt(80,p.tierYForR13Test()+12-8);ok(m.tier()==Tier.RECOMMENDED && m.plan()!=null,"default tier return");
  p.clickAt(188,p.equipmentYForR13Test()+16-8);ok(m.selectedStyle().equals("Magic")&&m.profile()!=null &&
    m.profile().method().equals("Earth magic") && m.profile().gear().get(4).id()==1385,
    "Mole Earth Magic must route to its new research gear example, not silently borrow another style");
  p.clickAt(83,94);ok(m.view()==BurrowModel.View.PATHFINDER,"Pathfinder return");
  p.clickAt(30,400);ok(m.view()==BurrowModel.View.PATHFINDER,
    "Quest status is read-only and cannot accidentally change combat selection");
  p.clickAt(138,94);ok(m.view()==BurrowModel.View.OUTFITTER&&
    m.boss().equals("Giant Mole")&&m.selectedStyle().equals("Magic"),
    "Outfitter selection remains preserved after Pathfinder navigation");
  p.clickAt(191,94);ok(m.view()==BurrowModel.View.SESSIONS,"Sessions");
  var fixture=new BurrowPanel.SessionScope("synthetic_hitbox_test","login_1");
  SwingUtilities.invokeAndWait(()->{
   ok(p.ingestSessionEvent(new BurrowPanel.SessionStart(fixture,"hitbox-start",
     BurrowPanel.SessionOrigin.SYNTHETIC_FIXTURE)),"synthetic session accepted");
   ok(p.ingestSessionEvent(new BurrowPanel.SessionXpGain(fixture,"hitbox-xp",
     BurrowPanel.SessionOrigin.SYNTHETIC_FIXTURE,"Strength",25,
     "explicit synthetic fixture")),"only typed XP event adds progress");
  });
  var before=p.sessionReadout();
  ok(before.synthetic()&&before.totalXp()==25&&before.kcByBoss().isEmpty(),"fixture has no fake kills");
  p.clickAt(30,p.tierYForR13Test()+12-8);
  ok(m.view()==BurrowModel.View.SESSIONS&&p.sessionReadout().equals(before),
    "old demo hitbox read only");
  p.clickAt(105,p.equipmentYForR13Test()+16-8);
  ok(m.view()==BurrowModel.View.SESSIONS&&p.sessionReadout().equals(before),
    "old reset hitbox read only");
  ok(m.demoKills()==0,"no model demo counter remains");
  p.clickAt(35,94);ok(m.view()==BurrowModel.View.HOME,"Home return");
  p.clickAt(30,BurrowPanel.HEIGHT);ok(m.view()==BurrowModel.View.HOME,"outside bottom");
  System.out.println("OUTFITTER HITBOX PASS: "+checks+" assertions");
 }
}
