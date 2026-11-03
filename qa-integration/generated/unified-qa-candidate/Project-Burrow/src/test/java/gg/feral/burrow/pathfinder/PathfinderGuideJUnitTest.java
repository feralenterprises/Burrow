package gg.feral.burrow.pathfinder;

import gg.feral.burrow.core.PathfinderGoalStore;
import gg.feral.burrow.pathfinder.SuggestionEngine.*;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Build 19 Pathfinder acceptance fixtures (synthetic accounts; never live data). */
final class PathfinderGuideJUnitTest {
    static final Preferences ALL60=new Preferences(Focus.ANY,Attention.ACTIVE,60);
    static Optional<Suggestion> find(List<Suggestion> s,String id){return s.stream().filter(x->x.id().equals(id)).findFirst();}
    static Map<String,Quest> finished(String... names){Map<String,Quest> m=new HashMap<>();for(String n:names)m.put(n,Quest.FINISHED);return m;}
    static Map<String,Quest> allQuests(Quest state){
        Map<String,Quest> m=new HashMap<>();
        for(var q:net.runelite.api.Quest.values()) m.put(q.getName(),state);
        return m;
    }

    @Test void registryIsLargeSourcedAcyclicAndPreservesLegacyIds() {
        var rules=GuideData.rules();
        assertTrue(rules.size()>350,"rules "+rules.size());
        for(String legacy:List.of("quest_cooks_assistant","quest_sote","pvm_gauntlet","pvm_vorkath","gp_gargoyles","gp_silk","supplies_fishing","pvm_demons"))
            assertTrue(rules.stream().anyMatch(r->r.id().equals(legacy)),legacy);
        for(var r:rules) {
            assertTrue(r.source().startsWith("http") || r.source().startsWith("Burrow"),"source "+r.id());
            assertFalse(r.why().isBlank(),"why "+r.id());
        }
        GuideCoverage.requireAcyclic();
        var names=Arrays.stream(net.runelite.api.Quest.values()).map(net.runelite.api.Quest::getName).toList();
        var rep=GuideCoverage.report(names);
        assertEquals(215,rep.questEntries());
        assertEquals(213,rep.questsMatchedToRuneLite(),String.valueOf(rep.questsNotInRuneLite()));
        assertEquals(List.of("Natural History Quiz","Knight Waves Training Ground"),rep.questsNotInRuneLite());
        assertEquals(91,rep.afkMethods());
        assertEquals(35,rep.moneyMethods(),"33 ironman.guide rows + gargoyles + silk legacy");
        assertEquals(17,rep.outfitterEncounters(),"all 17 Outfitter encounters linked");
        assertTrue(rep.gaps().stream().anyMatch(g->g.contains("Mootrius")),"gap register is explicit");
        // Every Outfitter boss link names a real Outfitter boss.
        var bosses=gg.feral.burrow.core.BurrowModel.availableBosses();
        rules.stream().filter(r->r.outfitterBoss()!=null).forEach(r->assertTrue(bosses.contains(r.outfitterBoss()),r.outfitterBoss()));
    }

    @Test void questStatesFinishedInProgressNotStartedUnknown() {
        var s=new Snapshot(true,Map.of("Cooking",10,"Crafting",10),
            Map.of("Cook's Assistant",Quest.FINISHED,"Sheep Shearer",Quest.IN_PROGRESS,"The Restless Ghost",Quest.NOT_STARTED),
            BankStatus.UNAVAILABLE,Set.of());
        var out=SuggestionEngine.suggest(s,new Preferences(Focus.QUESTS,Attention.ACTIVE,60));
        assertTrue(find(out,"quest_cooks_assistant").isEmpty(),"finished never suggested");
        assertEquals("quest_sheep_shearer",out.get(0).id(),"in-progress quest ranks first");
        assertTrue(out.get(0).title().startsWith("Continue"));
        var ghost=find(out,"quest_the_restless_ghost").orElseThrow();
        // GuideData NO_HARD_SKILL_GATE: annotated Prayer 9 is a route waypoint, not a hard quest gate.
        assertEquals(State.MET,ghost.readiness(),"Restless Ghost start is not gated on unobserved Prayer");
        assertTrue(ghost.why().toLowerCase(java.util.Locale.ROOT).contains("waypoint")
            || ghost.why().contains("NOT a quest requirement")
            || ghost.requirement().contains("No observable prerequisite")
            || ghost.requirement().contains("Observed requirements met"), ghost.why()+" / "+ghost.requirement());
        assertTrue(find(out,"quest_x_marks_the_spot").isEmpty(),"UNKNOWN quest state is never claimed unfinished");
    }

    @Test void sotEFinishedDs2UnknownSlayer75() {
        var s=new Snapshot(true,Map.of("Slayer",75),finished("Song of the Elves"),BankStatus.UNAVAILABLE,Set.of());
        var out=SuggestionEngine.suggest(s,ALL60);
        assertTrue(find(out,"quest_sote").isEmpty(),"no pointless SotE grind");
        assertEquals(State.MET,find(out,"pvm_gauntlet").orElseThrow().readiness(),"post-quest unlock suggested");
        // Vorkath is Category.BOSS and requires authenticated trip-food evidence when DS2 is unknown.
        assertEquals(State.UNKNOWN,find(out,"pvm_vorkath").orElseThrow().readiness(),"DS2 not observed: unknown, never unlocked");
        assertEquals(State.MET,find(SuggestionEngine.suggest(s,new Preferences(Focus.GP,Attention.LOW,60)),"gp_gargoyles").orElseThrow().readiness());
        var food=new ResourceEvidence(true,true,true,Map.of(),null,10,null);
        var notStarted=new Snapshot(true,Map.of("Slayer",75),Map.of("Song of the Elves",Quest.FINISHED,"Dragon Slayer II",Quest.NOT_STARTED),
            BankStatus.UNAVAILABLE,Set.of(),Mode.IRONMAN,Map.of(),Map.of(),food);
        var v=find(SuggestionEngine.suggest(notStarted,ALL60),"pvm_vorkath").orElseThrow();
        assertEquals(State.UNMET,v.readiness());
        assertTrue(v.missing().contains("Dragon Slayer II (not started)"));
        assertTrue(v.nextStep().contains("Dragon Slayer II"));
    }

    @Test void prerequisiteGraphPointsAtTheEarliestMissingQuest() {
        var quests=allQuests(Quest.FINISHED);
        quests.put("Song of the Elves",Quest.NOT_STARTED);
        quests.put("Regicide",Quest.NOT_STARTED);
        quests.put("Underground Pass",Quest.NOT_STARTED);
        var s=new Snapshot(true,Map.of("Agility",70,"Construction",70,"Farming",70,"Herblore",70,"Hunter",70,"Mining",70,"Smithing",70,"Woodcutting",70),
            quests,BankStatus.UNAVAILABLE,Set.of());
        var sote=find(SuggestionEngine.suggest(s,ALL60),"quest_sote").orElseThrow();
        assertEquals(State.UNMET,sote.readiness());
        assertEquals("Next: complete Underground Pass.",sote.nextStep(),"guide-order earliest missing prerequisite");
        var up=find(SuggestionEngine.suggest(s,ALL60),"quest_underground_pass").orElseThrow();
        assertTrue(up.score()>0);
    }

    @Test void filtersProduceDistinctListsAndLowAttentionHasNoActiveBosses() {
        var s=new Snapshot(true,Map.of("Slayer",80,"Fishing",80,"Woodcutting",80,"Attack",80,"Strength",80),
            finished("Dragon Slayer II","Song of the Elves"),BankStatus.UNAVAILABLE,Set.of());
        var active=SuggestionEngine.suggest(s,new Preferences(Focus.ANY,Attention.ACTIVE,60));
        var low15=SuggestionEngine.suggest(s,new Preferences(Focus.ANY,Attention.LOW,15));
        assertNotEquals(active.stream().map(Suggestion::id).toList(),low15.stream().map(Suggestion::id).toList());
        assertTrue(low15.stream().allMatch(x->x.attention()==Attention.LOW && x.minimumMinutes()<=15));
        assertTrue(low15.stream().noneMatch(x->x.category()==GuideRule.Category.BOSS));
        var gp=SuggestionEngine.suggest(s,new Preferences(Focus.GP,Attention.ACTIVE,60));
        assertTrue(gp.stream().allMatch(x->x.focus()==Focus.GP));
        assertTrue(find(gp,"gp_vorkath_gp").isPresent());
    }

    @Test void afkRequiresLevelAndQuestAndMoneyIsIronmanCoins() {
        var s=new Snapshot(true,Map.of("Fishing",70),Map.of("Swan Song",Quest.NOT_STARTED),BankStatus.UNAVAILABLE,Set.of());
        var low=SuggestionEngine.suggest(s,new Preferences(Focus.ANY,Attention.LOW,60));
        var monk=find(low,"afk_monkfish_sea_max").orElseThrow();
        assertEquals(State.UNMET,monk.readiness(),"Swan Song gate");
        var shark=find(low,"afk_sharks_sea_max").orElseThrow();
        assertEquals(State.UNMET,shark.readiness(),"76 Fishing gate with 70 observed");
        assertTrue(shark.missing().get(0).startsWith("Fishing 76 (have 70)"));
        var gp=find(SuggestionEngine.suggest(s,new Preferences(Focus.GP,Attention.ACTIVE,60)),"gp_rune_dragons").orElseThrow();
        assertTrue(gp.why().contains("coins/hr") && !gp.why().contains("Grand Exchange price"));
        assertTrue(gp.nextStep().contains("not the Grand Exchange"));
    }

    @Test void mainModeHasDistinctLogic() {
        var base=new Snapshot(true,Map.of(),finished("Throne of Miscellania"),BankStatus.UNAVAILABLE,Set.of());
        var iron=SuggestionEngine.suggest(base,new Preferences(Focus.GP,Attention.LOW,60));
        var main=SuggestionEngine.suggest(base.withMode(Mode.MAIN),new Preferences(Focus.GP,Attention.LOW,60));
        assertTrue(find(iron,"gp_miscellania").isPresent());
        assertTrue(find(main,"gp_miscellania").isEmpty(),"Ironman-only workaround hidden for Main");
        assertTrue(find(main,"gp_gargoyles").orElseThrow().why().startsWith("Main account"));
    }

    @Test void bankUnknownStaleAbsentAndPresentAreDistinct() {
        var unknown=new Snapshot(true,Map.of(),Map.of(),BankStatus.UNAVAILABLE,Set.of(7462));
        var observedOwned=new Snapshot(true,Map.of(),Map.of(),BankStatus.VERIFIED_SAME_ACCOUNT,Set.of(7462,29589));
        var observedNot=new Snapshot(true,Map.of(),Map.of(),BankStatus.VERIFIED_SAME_ACCOUNT,Set.of());
        var gearPrefs=new Preferences(Focus.PVM,Attention.ACTIVE,60);
        var u=find(SuggestionEngine.suggest(unknown,gearPrefs),"gear_barrows_gloves").orElseThrow();
        assertTrue(u.why().contains("ownership is unknown"));
        assertTrue(find(SuggestionEngine.suggest(observedOwned,gearPrefs),"gear_barrows_gloves").isEmpty(),"observed owned: hidden");
        var n=find(SuggestionEngine.suggest(observedNot,gearPrefs),"gear_barrows_gloves").orElseThrow();
        assertFalse(n.why().contains("unknown"));
        // Emberlight ownership alone is not TD readiness.
        var td=find(SuggestionEngine.suggest(observedOwned,new Preferences(Focus.PVM,Attention.ACTIVE,30)),"pvm_demons").orElseThrow();
        assertNotEquals(State.MET,td.readiness(),"WGS not observed");
        assertTrue(find(SuggestionEngine.suggest(unknown,gearPrefs),"pvm_demons").isEmpty(),"unavailable bank proves nothing");
        // Best Bank readiness flows in by boss name, as text, never as a certification.
        var withPlan=observedOwned.withBossReadiness(Map.of("Tormented Demons","NOT RECOMMENDED · 2 blockers"));
        assertTrue(find(SuggestionEngine.suggest(withPlan,new Preferences(Focus.PVM,Attention.ACTIVE,30)),"pvm_demons").orElseThrow()
            .why().contains("Best in Bank: NOT RECOMMENDED"));
    }

    @Test void everySuggestionIsExplainedWithSourceAndNextStep() {
        var s=new Snapshot(true,Map.of("Slayer",60,"Attack",60),finished("Cook's Assistant"),BankStatus.UNAVAILABLE,Set.of());
        for(var x:SuggestionEngine.evaluateAll(s)) {
            assertFalse(x.why().isBlank(),x.id()); assertFalse(x.source().isBlank(),x.id());
            assertNotNull(x.requirement(),x.id()); assertNotNull(x.nextStep(),x.id());
        }
    }

    @Test void savedGoalBoostsGoalAndItsMissingPrerequisitesSkipAndDoneHide() {
        var quests=allQuests(Quest.NOT_STARTED);
        var s=new Snapshot(true,Map.of(),quests,BankStatus.UNAVAILABLE,Set.of());
        int before=find(SuggestionEngine.suggest(s,ALL60),"quest_legends_quest").orElseThrow().score();
        var goal=s.withGoals(Map.of("pvm_vorkath",GoalMark.SAVED,"quest_cooks_assistant",GoalMark.SKIPPED,"quest_sheep_shearer",GoalMark.DONE));
        var out=SuggestionEngine.suggest(goal,ALL60);
        assertTrue(find(out,"quest_legends_quest").orElseThrow().score()>before,"DS2 prerequisite chain boosted by saved Vorkath goal");
        assertEquals(GoalMark.SAVED,find(out,"pvm_vorkath").orElseThrow().goal());
        assertTrue(find(out,"quest_cooks_assistant").isEmpty(),"skipped by owner");
        assertTrue(find(out,"quest_sheep_shearer").isEmpty(),"owner-marked done");
    }

    @Test void goalStoreIsPerAccountAndExplicit() {
        Map<String,String> cfg=new HashMap<>();
        var store=new PathfinderGoalStore(cfg::get,cfg::put);
        assertTrue(store.read(1L).isEmpty());
        store.set(1L,"pvm_vorkath",GoalMark.SAVED);
        store.set(1L,"gp_silk",GoalMark.SAVED);
        assertEquals(List.of("pvm_vorkath","gp_silk"),new ArrayList<>(store.read(1L).keySet()));
        store.moveUp(1L,"gp_silk");
        assertEquals(List.of("gp_silk","pvm_vorkath"),new ArrayList<>(store.read(1L).keySet()),"reorder");
        assertTrue(store.read(2L).isEmpty(),"account B never sees A's goals");
        store.set(2L,"gp_silk",GoalMark.SKIPPED);
        assertEquals(GoalMark.SAVED,store.read(1L).get("gp_silk"),"A unaffected by B");
        store.set(1L,"gp_silk",null);
        assertFalse(store.read(1L).containsKey("gp_silk"),"restore");
        assertThrows(IllegalArgumentException.class,()->store.set(-1L,"x",GoalMark.SAVED));
        cfg.put(PathfinderGoalStore.key(3L),"bad|ok_id:SAVED|evil id:SAVED|x:NOPE");
        assertEquals(Map.of("ok_id",GoalMark.SAVED),store.read(3L),"malformed entries ignored");
    }

    @Test void dropCalculatorMath() {
        double p=1.0/75;
        assertEquals(1-Math.pow(74.0/75,75),DropChance.atLeastOne(p,75),1e-12);
        assertEquals("63.5%",DropChance.percent(DropChance.atLeastOne(p,75)));
        assertEquals(0.0,DropChance.atLeastOne(p,0));
        assertTrue(DropChance.noDrop(p,5000)>0,"never a 100% guarantee: a dry streak stays possible");
        assertEquals(">99.9%",DropChance.percent(DropChance.atLeastOne(p,5000)),"display never claims 100%");
        assertEquals(52,DropChance.killsForConfidence(p,0.5));
        assertEquals(172,DropChance.killsForConfidence(p,0.9));
        assertThrows(IllegalArgumentException.class,()->DropChance.killsForConfidence(p,1.0));
        assertEquals(75.0,DropChance.expectedKills(p),1e-9);
        // Independence: all-of is the product, any-of the complement of joint misses.
        double a=DropChance.atLeastOne(1.0/100,100), b=DropChance.atLeastOne(1.0/50,100);
        assertEquals(a*b,DropChance.allOf(new double[]{1.0/100,1.0/50},100),1e-12);
        assertEquals(1-(1-a)*(1-b),DropChance.anyOf(new double[]{1.0/100,1.0/50},100),1e-12);
    }

    @Test void realisticMidGameAccountGetsPersonalizedPrioritizedPlan() {
        var quests=allQuests(Quest.NOT_STARTED);
        // Finish the full MM2 prerequisite chain (Awowogei is order 170), then resume MM2.
        for(var e:GuideQuestOrder.entries()) if(e.order()<=200 && quests.containsKey(e.name())) quests.put(e.name(),Quest.FINISHED);
        quests.put("Song of the Elves",Quest.FINISHED);
        quests.put("Monkey Madness II",Quest.IN_PROGRESS);
        Map<String,Integer> skills=new HashMap<>();
        for(var sk:net.runelite.api.Skill.values()) if(sk!=net.runelite.api.Skill.OVERALL) skills.put(sk.getName(),70);
        skills.put("Slayer",85); skills.put("Magic",80);
        var food=new ResourceEvidence(true,true,true,Map.of(7462,1,6570,1,29589,1),null,20,null);
        var s=new Snapshot(true,skills,quests,BankStatus.VERIFIED_SAME_ACCOUNT,Set.of(7462,6570,29589),Mode.IRONMAN,
            Map.of("Vorkath","TRIP READY · Recommended (Ranged crossbow)"),Map.of(),food);
        var out=SuggestionEngine.suggest(s,ALL60);
        assertTrue(out.size()>40,"far more than the old eight: "+out.size());
        assertEquals("quest_monkey_madness_ii",out.get(0).id(),"in-progress quest resumes first");
        var cats=out.stream().limit(25).map(Suggestion::category).collect(Collectors.toSet());
        assertTrue(cats.size()>=3,"varied plan "+cats);
        assertTrue(find(out,"gear_barrows_gloves").isEmpty() && find(out,"gear_fire_cape").isEmpty(),"owned gear goals hidden");
        assertTrue(find(out,"quest_sote").isEmpty());
        assertEquals(State.MET,find(out,"pvm_gauntlet").orElseThrow().readiness());
        var vork=find(out,"pvm_vorkath").orElseThrow();
        assertTrue(vork.why().contains("TRIP READY"));
        long distinct=out.stream().map(Suggestion::id).distinct().count();
        assertEquals(out.size(),distinct,"no duplicated goals");
    }
}
