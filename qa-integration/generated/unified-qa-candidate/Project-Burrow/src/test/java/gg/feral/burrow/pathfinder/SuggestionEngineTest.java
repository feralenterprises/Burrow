package gg.feral.burrow.pathfinder;
import java.util.*;
import static gg.feral.burrow.pathfinder.SuggestionEngine.*;
public class SuggestionEngineTest {
 static void check(boolean b,String s){if(!b) throw new AssertionError(s);}
 static boolean has(List<Suggestion> s,String id){return s.stream().anyMatch(x->x.id().equals(id));}
 static Suggestion byId(List<Suggestion> s,String id){return s.stream().filter(x->x.id().equals(id)).findFirst().orElseThrow(()->new AssertionError("missing "+id));}
 public static void main(String[] a){
  var prefs=new Preferences(Focus.ANY,Attention.ACTIVE,60);
  var unknown=new Snapshot(true,Map.of(),Map.of(),BankStatus.UNAVAILABLE,Set.of(29589));
  var u=suggest(unknown,prefs);
  check(!has(u,"quest_cooks_assistant"),"unknown quest is not unfinished");
  check(!has(u,"pvm_demons"),"unavailable bank cannot prove item");
  check(u.stream().filter(x->x.id().equals("pvm_gauntlet")).findFirst().orElseThrow().readiness()==State.UNKNOWN,"unknown sote");
  var ready=new Snapshot(true,Map.of("Ranged",80),Map.of("Song of the Elves",Quest.FINISHED,"Dragon Slayer II",Quest.FINISHED,"Cook's Assistant",Quest.FINISHED,"While Guthix Sleeps",Quest.FINISHED),BankStatus.VERIFIED_SAME_ACCOUNT,Set.of(29589));
  var x=suggest(ready,prefs);
  check(has(x,"pvm_demons"),"positive emberlight evidence");
  check(!has(x,"quest_cooks_assistant") && !has(x,"quest_sote"),"finished quests not re-suggested");
  check(x.stream().filter(y->y.id().equals("pvm_gauntlet")).findFirst().orElseThrow().readiness()==State.MET,"sote prerequisite only");
  check(!has(suggest(new Snapshot(false,Map.of(),Map.of(),BankStatus.UNAVAILABLE,Set.of()),prefs),"pvm_gauntlet"),"unverified account");
  var low=suggest(ready,new Preferences(Focus.ANY,Attention.LOW,15));
  check(has(low,"supplies_fishing") && !has(low,"pvm_vorkath"),"time and attention filters");
  var quest=new Snapshot(true,Map.of(),Map.of("Cook's Assistant",Quest.IN_PROGRESS),BankStatus.UNAVAILABLE,Set.of());
  check(suggest(quest,new Preferences(Focus.QUESTS,Attention.ACTIVE,20)).get(0).title().startsWith("Continue"),"continue quest");
  var none=new Snapshot(true,Map.of(),Map.of(),BankStatus.VERIFIED_SAME_ACCOUNT,Set.of());
  check(!has(suggest(none,prefs),"pvm_demons"),"verified absence does not invent item");
  var slayer=new Snapshot(true,Map.of("Slayer",74),Map.of(),BankStatus.UNAVAILABLE,Set.of());
  // Build 19: the GP list now contains many methods, so look up gargoyles by stable ID instead of list position.
  check(byId(suggest(slayer,new Preferences(Focus.GP,Attention.LOW,30)),"gp_gargoyles").readiness()==State.UNMET,"slayer threshold");
  var slayer2=new Snapshot(true,Map.of("Slayer",75),Map.of(),BankStatus.UNAVAILABLE,Set.of());
  check(byId(suggest(slayer2,new Preferences(Focus.GP,Attention.LOW,30)),"gp_gargoyles").readiness()==State.MET,"slayer threshold met only");
  System.out.println("PASS 12 targeted checks: identity, quests, bank, skill, time, attention");
 }
}
