package gg.feral.burrow.pathfinder;

import java.util.*;

/**
 * Pathfinder suggestion engine. Inputs must be authenticated by the host (RuneLite plugin);
 * this class never calls RuneLite and never controls the game.
 *
 * <p>Build 19 expands the original eight curated activities (IDs, titles and semantics preserved)
 * into a source-linked rule registry ({@link GuideData}): the ironman.guide quest order with a
 * prerequisite graph, the 17 Outfitter encounters, ironman.guide AFK and money-making tables, Wiki
 * Ironman gear goals and route milestones. Each suggestion explains why, what is missing, the next
 * concrete step and its source. Unknown is never treated as unfinished or missing.
 */
public final class SuggestionEngine {
    private SuggestionEngine() {}
    public enum State { MET, UNMET, UNKNOWN }
    public enum Quest { FINISHED, IN_PROGRESS, NOT_STARTED, UNKNOWN }
    /** Build 20 appends AFK (category filter: ironman.guide AFK methods). Ordinals of earlier values unchanged. */
    public enum Focus { ANY, QUESTS, PVM, GP, SUPPLIES, AFK }
    /** Build 20 appends AFK: only activities with a sourced look-away interval of at least {@link #AFK_MIN_SECONDS}. */
    public enum Attention { LOW, ACTIVE, AFK }
    public static final int AFK_MIN_SECONDS = 60;
    /** Minutes dropdown values (Build 20). */
    public static final List<Integer> MINUTE_CHOICES = List.of(15,30,45,60,90,120);
    public enum BankStatus { VERIFIED_SAME_ACCOUNT, UNAVAILABLE }
    public enum Mode { IRONMAN, MAIN }
    /** Explicit owner action recorded per account; never inferred. */
    public enum GoalMark { SAVED, SKIPPED, DONE }

    /** Optional, never implicitly populated from item IDs. The host MUST authenticate account and freshness.
     *  A partial scan cannot establish absence; absent counts are UNKNOWN, never zero. */
    public record ResourceEvidence(boolean sameAccount, boolean fresh, boolean completeCoverage,
                                   Map<Integer,Integer> itemQuantities, Integer gp, Integer food, Integer runes) {
        public ResourceEvidence {
            itemQuantities=Map.copyOf(itemQuantities);
            if(itemQuantities.values().stream().anyMatch(v->v==null||v<0) ||
                (gp!=null&&gp<0)||(food!=null&&food<0)||(runes!=null&&runes<0))
                throw new IllegalArgumentException("invalid negative supplies");
        }
        public boolean reliable(){return sameAccount&&fresh;}
        /** Null means UNKNOWN: no trusted scan or omitted item in a partial scan. */
        public Integer quantity(int id) {
            if(!reliable())return null;
            if(itemQuantities.containsKey(id))return itemQuantities.get(id);
            return completeCoverage?0:null;
        }
        /** Any known positive alternative suffices, zero requires all alternatives observed. */
        public Integer anyQuantity(int... ids) {
            boolean unknown=false;
            long sum=0;
            for(int id:ids) {Integer q=quantity(id);if(q==null)unknown=true;else sum+=q;}
            if(sum>0)return (int)Math.min(sum,Integer.MAX_VALUE);
            if(unknown)return null;
            return 0;
        }
        /** Three-way check for alternative item IDs. A partial bank read must never prove shortage
         *  from an incomplete sum (e.g. 12 rune essence, pure essence UNKNOWN, need 50).
         *  Saturates before overflow. This doesn't authorize consuming or moving items. */
        public State alternativeRequirement(int required,int... ids) {
            if(required<=0)throw new IllegalArgumentException("required");
            long observed=0; boolean unknown=false;
            for(int id:ids) {
                Integer q=quantity(id);
                if(q==null)unknown=true;
                else observed+=q;
                if(observed>=required)return State.MET;
            }
            return unknown?State.UNKNOWN:State.UNMET;
        }
    }

    public record Snapshot(boolean verifiedIdentity, Map<String,Integer> skills,
                           Map<String,Quest> quests, BankStatus bankStatus,
                           Set<Integer> observedItemIds, Mode mode,
                           Map<String,String> bossReadiness, Map<String,GoalMark> goals,
                           ResourceEvidence resources, Set<String> witnessedUnlocks) {
        public Snapshot {
            skills=Map.copyOf(skills); quests=Map.copyOf(quests);
            Objects.requireNonNull(bankStatus); observedItemIds=Set.copyOf(observedItemIds);
            mode=mode==null?Mode.IRONMAN:mode;
            bossReadiness=Map.copyOf(bossReadiness); goals=Map.copyOf(goals);
            witnessedUnlocks=Set.copyOf(witnessedUnlocks);
            if (skills.values().stream().anyMatch(x -> x == null || x < 1 || x > 99))
                throw new IllegalArgumentException("invalid level");
        }
        /** Build 17/18 constructor: Ironman default, no boss readiness or saved goals. */
        public Snapshot(boolean verifiedIdentity, Map<String,Integer> skills, Map<String,Quest> quests,
                        BankStatus bankStatus, Set<Integer> observedItemIds) {
            this(verifiedIdentity,skills,quests,bankStatus,observedItemIds,Mode.IRONMAN,Map.of(),Map.of(),null,Set.of());
        }
        /** Backward-compatible Wave 7.5.1 eight-field constructor. */
        public Snapshot(boolean verifiedIdentity, Map<String,Integer> skills, Map<String,Quest> quests,
                        BankStatus bankStatus, Set<Integer> observedItemIds, Mode mode,
                        Map<String,String> bossReadiness, Map<String,GoalMark> goals) {
            this(verifiedIdentity,skills,quests,bankStatus,observedItemIds,mode,bossReadiness,goals,null,Set.of());
        }
        /** W04-compatible nine-field constructor; unlocking stages are NOT inferred from IN_PROGRESS. */
        public Snapshot(boolean verifiedIdentity, Map<String,Integer> skills, Map<String,Quest> quests,
                        BankStatus bankStatus, Set<Integer> observedItemIds, Mode mode,
                        Map<String,String> bossReadiness, Map<String,GoalMark> goals, ResourceEvidence resources) {
            this(verifiedIdentity,skills,quests,bankStatus,observedItemIds,mode,bossReadiness,goals,resources,Set.of());
        }
        public Snapshot withMode(Mode m){return new Snapshot(verifiedIdentity,skills,quests,bankStatus,observedItemIds,m,bossReadiness,goals,resources,witnessedUnlocks);}
        public Snapshot withGoals(Map<String,GoalMark> g){return new Snapshot(verifiedIdentity,skills,quests,bankStatus,observedItemIds,mode,bossReadiness,g,resources,witnessedUnlocks);}
        public Snapshot withBossReadiness(Map<String,String> b){return new Snapshot(verifiedIdentity,skills,quests,bankStatus,observedItemIds,mode,b,goals,resources,witnessedUnlocks);}
        public Snapshot withResources(ResourceEvidence v){return new Snapshot(verifiedIdentity,skills,quests,bankStatus,observedItemIds,mode,bossReadiness,goals,v,witnessedUnlocks);}
        /** For independent, authenticated, witnessed unlock events ONLY. Host must verify provenance. */
        public Snapshot withWitnessedUnlocks(Set<String> w){return new Snapshot(verifiedIdentity,skills,quests,bankStatus,observedItemIds,mode,bossReadiness,goals,resources,w);}
        Quest quest(String name) {return quests.getOrDefault(name, Quest.UNKNOWN);}
        State completed(String name) {return switch(quest(name)) {
            case FINISHED -> State.MET; case NOT_STARTED, IN_PROGRESS -> State.UNMET; case UNKNOWN -> State.UNKNOWN;
        };}
        State level(String skill,int minimum) {
            Integer value=skills.get(skill); return value==null ? State.UNKNOWN : value>=minimum ? State.MET : State.UNMET;
        }
        State item(int id) {
            if(resources!=null) {
                Integer q=resources.quantity(id);
                if(q!=null)return q>0?State.MET:State.UNMET;
                // A stale, partial or different-account quantity read cannot establish ownership.
                return State.UNKNOWN;
            }
            if (bankStatus != BankStatus.VERIFIED_SAME_ACCOUNT) return State.UNKNOWN;
            // Legacy item IDs are positive evidence only; missing from an incomplete read is unknown.
            return observedItemIds.contains(id) ? State.MET : State.UNKNOWN;
        }
    }
    public record Preferences(Focus focus, Attention attention, int minutes) {
        public Preferences { Objects.requireNonNull(focus); Objects.requireNonNull(attention); if(minutes<=0) throw new IllegalArgumentException("minutes"); }
    }
    /** First nine components are the Build 17/18 contract; the rest are Build 19 explanation fields. */
    public record Suggestion(String id, String title, String why, State readiness,
                             String requirement, String source, Focus focus,
                             Attention attention, int minimumMinutes,
                             GuideRule.Category category, String section, List<String> missing,
                             String nextStep, String outfitterBoss, int score, GoalMark goal,
                             boolean optional, List<String> unverified, int afkSeconds,
                             PathfinderTiming.Start start) {
        public Suggestion {
            missing=List.copyOf(missing); unverified=List.copyOf(unverified);
            start=start==null?PathfinderTiming.Start.NOT_TIME_BOUND:start;
        }
        /** Pre-CP-2 shape (no start-time verdict). */
        public Suggestion(String id, String title, String why, State readiness, String requirement, String source,
                          Focus focus, Attention attention, int minimumMinutes, GuideRule.Category category, String section,
                          List<String> missing, String nextStep, String outfitterBoss, int score, GoalMark goal,
                          boolean optional, List<String> unverified, int afkSeconds) {
            this(id,title,why,readiness,requirement,source,focus,attention,minimumMinutes,category,section,missing,nextStep,
                outfitterBoss,score,goal,optional,unverified,afkSeconds,PathfinderTiming.Start.NOT_TIME_BOUND);
        }
        /** Requirements observed AND the window can honestly start it (CP-2). */
        public boolean readyNow() { return readiness==State.MET && start!=PathfinderTiming.Start.UNKNOWN_FOR_WINDOW; }
    }

    public static List<Suggestion> suggest(Snapshot s, Preferences p) {
        Objects.requireNonNull(s); Objects.requireNonNull(p);
        if(!s.verifiedIdentity()) return List.of();
        List<Suggestion> out=new ArrayList<>();
        Set<String> goalQuests=goalPrerequisiteQuests(s);
        for(GuideRule r:GuideData.rules()) {
            Suggestion x=evaluate(r,s,goalQuests);
            if(x==null)continue;
            if(!passes(p,x,goalQuests))continue;
            boolean onGoalRoute=x.goal()==GoalMark.SAVED ||
                (r.category()==GuideRule.Category.QUEST && goalQuests.contains(r.completesQuest()));
            out.add(timed(x,r,s,p,onGoalRoute));
        }
        out.sort(Comparator.comparingInt(Suggestion::score).reversed()
            .thenComparing(x->x.readiness()==State.MET?0:x.readiness()==State.UNKNOWN?1:2)
            .thenComparing(Suggestion::title));
        return diversify(out);
    }
    /** CP-2: start-time penalty for unknown travel in a short window; bank inputs for bank-standing methods. */
    static final int UNKNOWN_TRAVEL_PENALTY = 200, INPUTS_OBSERVED_BONUS = 60, INPUTS_MISSING_PENALTY = 80;
    static Suggestion timed(Suggestion x,GuideRule r,Snapshot s,Preferences p) {return timed(x,r,s,p,false);}
    static Suggestion timed(Suggestion x,GuideRule r,Snapshot s,Preferences p,boolean onGoalRoute) {
        String why=x.why(), requirement=x.requirement(); State readiness=x.readiness(); int score=x.score();
        var access=PathfinderTiming.access(r);
        PathfinderTiming.Start start=PathfinderTiming.Start.NOT_TIME_BOUND;
        if(access==PathfinderTiming.Access.BANKSTANDING) {
            start=PathfinderTiming.Start.STARTABLE_AT_BANK;
            var in=PathfinderTiming.inputs(r,s);
            why=why+" Can be started at a bank.";
            switch(in.state()) {
                case OBSERVED -> {
                    score+=INPUTS_OBSERVED_BONUS;
                    why=why+" First inputs ("+in.label()+") verified in fresh same-account quantities; sustained budget not established.";
                }
                case NOT_OBSERVED -> {
                    score-=INPUTS_MISSING_PENALTY;
                    if(readiness==State.MET)readiness=State.UNMET;
                    why=why+" Required inputs ("+in.label()+") absent in verified same-account stock.";
                    requirement=requirement+" · Missing input stock: "+in.label();
                }
                case BANK_UNKNOWN -> {
                    if(readiness==State.MET)readiness=State.UNKNOWN;
                    why=why+" Quantity/completeness/freshness unknown for bank inputs ("+in.label()+").";
                    requirement=requirement+" · Unknown input quantities: "+in.label();
                }
                default -> {
                    if(readiness==State.MET)readiness=State.UNKNOWN;
                    if(!in.label().isEmpty()) why=why+" Unverified input list: "+in.label()+".";
                }
            }
        } else if(PathfinderTiming.timeSensitive(x.category()) && p.minutes()<=PathfinderTiming.SHORT_WINDOW_MINUTES) {
            score-=UNKNOWN_TRAVEL_PENALTY;
            start=PathfinderTiming.Start.UNKNOWN_FOR_WINDOW;
            why=why+" Travel and set-up time are not modelled, so this may not fit "+p.minutes()+" minutes.";
            requirement=requirement+" · start time unknown for "+p.minutes()+" min";
        }
        boolean attentionFit=switch(p.attention()) {
            case ACTIVE -> true;
            case LOW -> x.attention()==Attention.LOW;
            case AFK -> x.afkSeconds()>=AFK_MIN_SECONDS;
        };
        if(onGoalRoute && (!attentionFit || p.minutes()<x.minimumMinutes())) {
            start=PathfinderTiming.Start.UNKNOWN_FOR_WINDOW;
            score-=UNKNOWN_TRAVEL_PENALTY;
            String reason="Goal prerequisite stays visible but cannot start within selected "
                +p.minutes()+"m / "+p.attention()+" preferences.";
            why=why+" "+reason;
            requirement=requirement+" · "+reason;
        }
        return new Suggestion(x.id(),x.title(),why,readiness,requirement,x.source(),x.focus(),x.attention(),x.minimumMinutes(),
            x.category(),x.section(),x.missing(),x.nextStep(),x.outfitterBoss(),score,x.goal(),x.optional(),x.unverified(),x.afkSeconds(),start);
    }
    /** Greedy re-rank: each further pick from an already-used category costs DIVERSITY points, so a
     *  plan mixes quests, bosses, AFK, money and gear instead of one category repeated. Stable. */
    static final int DIVERSITY = 45;
    static List<Suggestion> diversify(List<Suggestion> sorted) {
        List<Suggestion> pool=new ArrayList<>(sorted), out=new ArrayList<>(sorted.size());
        Map<GuideRule.Category,Integer> used=new EnumMap<>(GuideRule.Category.class);
        while(!pool.isEmpty()) {
            int bestIdx=0; int best=Integer.MIN_VALUE; int bestBand=Integer.MAX_VALUE;
            for(int i=0;i<pool.size();i++) {
                Suggestion x=pool.get(i);
                int eff=x.score()-DIVERSITY*used.getOrDefault(x.category(),0);
                // Variety reorders within a band. It never lifts something you cannot start over something you can.
                int band=band(x);
                if(band<bestBand || (band==bestBand && eff>best)){best=eff;bestIdx=i;bestBand=band;}
            }
            Suggestion pick=pool.remove(bestIdx);
            used.merge(pick.category(),1,Integer::sum);
            out.add(pick);
        }
        return List.copyOf(out);
    }
    private static int band(Suggestion x) {
        // Saved does not mean feasible; an impossible saved goal cannot leapfrog a startable step.
        if(x.readyNow()) return 0;
        if(x.readiness()==State.MET) return 1; // time window cannot be established
        if(x.readiness()==State.UNKNOWN)return 2;
        return 3;
    }
    /** Everything evaluable regardless of focus/attention/time (for coverage and goal views). */
    public static List<Suggestion> evaluateAll(Snapshot s) {
        if(!s.verifiedIdentity())return List.of();
        Set<String> goalQuests=goalPrerequisiteQuests(s);
        List<Suggestion> out=new ArrayList<>();
        for(GuideRule r:GuideData.rules()) { var x=evaluate(r,s,goalQuests); if(x!=null)out.add(x); }
        return out;
    }
    /** Sourced AFK methods that are fights against monsters, so a PvM filter must find them. */
    static final Set<String> COMBAT_AFK = Set.of("afk_brutus_afk","afk_nmz_melee","afk_nmz_ranged","afk_vyrewatch",
        "afk_monkeys_barrage","afk_monkeys_chinning","afk_gemstone_crabs","afk_sand_crabs","afk_ammonite_crabs");
    private static final Set<String> FARMING_SUPPLY_PRODUCERS = Set.of("gp_herb_runs", "gp_farming_contracts");
    static boolean passes(Preferences p,Suggestion s) {return passes(p,s,Set.of());}
    static boolean passes(Preferences p,Suggestion s,Set<String> goalQuests) {
        boolean focus=switch(p.focus()) {
            case ANY -> true;
            case AFK -> s.category()==GuideRule.Category.AFK;
            case PVM -> s.focus()==Focus.PVM || COMBAT_AFK.contains(s.id());
            case GP, SUPPLIES -> p.focus()==s.focus() || FARMING_SUPPLY_PRODUCERS.contains(s.id());
            default -> p.focus()==s.focus();
        };
        boolean attention=switch(p.attention()) {
            case ACTIVE -> true;
            case LOW -> s.attention()==Attention.LOW;
            // Never inferred: only a sourced look-away interval qualifies as AFK.
            case AFK -> s.afkSeconds()>=AFK_MIN_SECONDS;
        };
        // Explicit saved goals and their unfinished quest chain remain visible across category filters.
        // Attention and time limitations still apply: visibility is not proof of startability.
        boolean goalRoute=s.goal()==GoalMark.SAVED ||
            (s.category()==GuideRule.Category.QUEST && goalQuests.contains(questForId(s.id())));
        return (focus && attention && s.minimumMinutes()<=p.minutes()) || goalRoute;
    }

    private static String questForId(String id) {
        for (GuideRule rule:GuideData.rules()) if(rule.id().equals(id))return rule.completesQuest();
        return null;
    }

    /** Unfinished quests required by the owner's saved goals (goal-directed prioritisation). */
    private static Set<String> goalPrerequisiteQuests(Snapshot s) {
        Set<String> out=new HashSet<>();
        for(var e:s.goals().entrySet()) {
            if(e.getValue()!=GoalMark.SAVED)continue;
            for(GuideRule r:GuideData.rules()) if(r.id().equals(e.getKey())) {
                Deque<String> todo=new ArrayDeque<>(r.quests());
                if(r.completesQuest()!=null) todo.add(r.completesQuest());
                while(!todo.isEmpty()) {
                    String q=todo.pop();
                    if(!out.add(q))continue;
                    todo.addAll(GuideData.questPrerequisites().getOrDefault(q,List.of()));
                }
            }
        }
        return out;
    }

    static Suggestion evaluate(GuideRule r,Snapshot s,Set<String> goalQuests) {
        GoalMark mark=s.goals().get(r.id());
        if(mark==GoalMark.SKIPPED || mark==GoalMark.DONE)return null; // explicit owner action, provenance kept by host
        if(r.mode()==GuideRule.ModeScope.IRONMAN && s.mode()!=Mode.IRONMAN)return null;
        if(r.mode()==GuideRule.ModeScope.MAIN && s.mode()!=Mode.MAIN)return null;
        // Done / not applicable.
        Quest own=r.completesQuest()==null?null:s.quest(r.completesQuest());
        if(r.category()==GuideRule.Category.QUEST) {
            if(own==Quest.FINISHED || own==Quest.UNKNOWN) return null; // UNKNOWN is never claimed unfinished
        } else if(own!=null) {
            if(own==Quest.FINISHED) return null;
            if(r.doneWhenStarted() && own==Quest.IN_PROGRESS) return null;
        }
        if(!r.doneAt().isEmpty() && r.doneAt().entrySet().stream().allMatch(e->s.level(e.getKey(),e.getValue())==State.MET))return null;
        for(int id:r.skipIfOwned()) if(s.item(id)==State.MET) return null; // positive observation only
        if(!r.needsOwned().isEmpty() && r.needsOwned().stream().noneMatch(id->s.item(id)==State.MET)) return null;
        // Fairy-ring permission is an intermediate quest stage. IN_PROGRESS is NOT evidence
        // of permission; the named unlock needs an independent, witnessed host signal.
        if(r.id().equals("unlock_fairy_rings") &&
            (own==Quest.FINISHED || (own==Quest.IN_PROGRESS &&
                s.witnessedUnlocks().contains("fairy_rings_permission"))))return null;
        // A permission witness contradicting NOT_STARTED or UNKNOWN must not be accepted
        // as an unlock: the host must reconcile quest-stage observations first.
        // Requirements.
        List<String> missing=new ArrayList<>(), unknown=new ArrayList<>();
        for(var e:r.skills().entrySet()) {
            switch(s.level(e.getKey(),e.getValue())) {
                case UNMET -> missing.add(e.getKey()+" "+e.getValue()+" (have "+s.skills().get(e.getKey())+")");
                case UNKNOWN -> unknown.add(e.getKey()+" "+e.getValue());
                default -> {}
            }
        }
        String firstMissingQuest=null;
        for(String q:r.quests()) {
            Quest st=s.quest(q);
            if(st==Quest.FINISHED)continue;
            if(st==Quest.UNKNOWN){unknown.add(q);continue;}
            missing.add(q+(st==Quest.IN_PROGRESS?" (in progress)":" (not started)"));
            if(firstMissingQuest==null || order(q)<order(firstMissingQuest)) firstMissingQuest=q;
        }
        if(r.id().equals("unlock_fairy_rings") && (own==Quest.UNKNOWN ||
            (own==Quest.IN_PROGRESS && !s.witnessedUnlocks().contains("fairy_rings_permission"))))
            unknown.add("Fairy Godfather's ring permission or underlying quest stage not independently witnessed");
        // Resource truth. Missing inventory quantities are UNKNOWN, not zero; absent bank item-ID
        // sets are not enough to demonstrate a viable trip or sustained food production.
        if(r.id().equals("afk_karambwan")) {
            ResourceEvidence res=s.resources();
            Integer vessel=res==null?null:res.anyQuantity(3157,3159); // OSRS Wiki: empty/baited variants
            Integer bait=res==null?null:res.quantity(3150);           // raw karambwanji, not cooked
            checkQuantity("karambwan vessel",vessel,missing,unknown);
            checkQuantity("raw karambwanji bait",bait,missing,unknown);
        }
        // W04: finite quest kit check is a PLANNING annotation, not a hard quest
        // requirement. Quests can often start before collecting their supplies.
        PathfinderSupplyKit.Result questKit =
            r.id().equals("quest_cooks_assistant") || r.id().equals("quest_priest_in_peril")
            ? PathfinderSupplyKit.evaluate(r.id(),s.resources()) : null;
        // Trip-food provenance applies to external boss trips. The Gauntlet (LEGACY) is
        // self-supplied inside the instance; do not demand a bank food reserve for it.
        if(r.category()==GuideRule.Category.BOSS) {
            ResourceEvidence res=s.resources();
            if(res==null||!res.reliable()||res.food()==null)
                unknown.add("Trip food reserve not verified; zero-food methods require a separately proven setup");
            else if(res.food()==0)
                unknown.add("Food reserve empty; no-food encounter strategy not established");
        }
        State readiness=!missing.isEmpty()?State.UNMET:!unknown.isEmpty()?State.UNKNOWN:State.MET;
        // Explanation fields.
        String title=r.title(), why=r.why(), next=r.nextStep();
        if(questKit!=null)
            why=why+" Finite kit " + questKit.verdict()+": "+String.join("; ",questKit.details())+".";
        if(r.category()==GuideRule.Category.QUEST) {
            title=(own==Quest.IN_PROGRESS?"Continue ":"Start ")+r.title();
            if(r.id().equals("quest_sote")) title="Work toward Song of the Elves";
            if(own==Quest.IN_PROGRESS) why="In progress in your quest journal. "+why;
            next=readiness==State.MET?(own==Quest.IN_PROGRESS?"Resume where you left off.":"Start the quest."):next;
        }
        if(r.id().equals("unlock_fairy_rings") && own==Quest.IN_PROGRESS)
            next="Obtain and verify permission from the Fairy Godfather; quest IN_PROGRESS alone is insufficient.";
        if(firstMissingQuest!=null) {
            String first=firstStep(firstMissingQuest,s,new HashSet<>());
            next=first.equals(firstMissingQuest)?"Next: complete "+first+".":"Next: complete "+first+" (needed for "+firstMissingQuest+").";
        }
        else if(!missing.isEmpty()) next="Next: train "+missing.get(0).replaceAll(" \\(have.*","")+".";
        else if(questKit!=null && questKit.verdict()!=PathfinderSupplyKit.Verdict.KIT_PRESENT)
            next="Gather or verify the listed quest kit; starting the quest does not require having it all already.";
        // A finite input budget changes the recommendation, but proves no spellbook, rune
        // substitution, future banking state, catch count or completion.
        if(r.id().equals("afk_plank_make")) {
            PathfinderActionBudget.Result budget=PathfinderActionBudget.evaluate(r.id(),s.resources());
            why=why+" Input budget: "+budget.verdict()+"; "+String.join("; ",budget.notes())+".";
            if(budget.verdict()==PathfinderActionBudget.Verdict.VERIFIED_SHORT) {
                if(readiness==State.MET)readiness=State.UNMET;
            } else if(readiness==State.MET)readiness=State.UNKNOWN;
            if(budget.verdict()!=PathfinderActionBudget.Verdict.ENOUGH_FOR_ACTION)
                next="Verify or gather the exact Plank Make log, coin and rune inputs; inspect Lunar spellbook before casting.";
        }
        if(r.category()==GuideRule.Category.MONEY && s.mode()==Mode.MAIN)
            why="Main account: ironman.guide coin estimate shown; market value is not modelled. "+why;
        if(r.category()==GuideRule.Category.GEAR && s.bankStatus()!=BankStatus.VERIFIED_SAME_ACCOUNT)
            why=why+" Bank not observed this login, so ownership is unknown.";
        String boss=r.outfitterBoss();
        if(boss!=null) {
            String bank=s.bossReadiness().get(boss);
            if(bank!=null) why=why+" Best in Bank: "+bank+".";
        }
        String requirement=requirementText(r,missing,unknown);
        int score=score(r,s,readiness,own,mark,goalQuests);
        return new Suggestion(r.id(),title,why,readiness,requirement,r.source(),r.focus(),r.attention(),r.minimumMinutes(),
            r.category(),r.section(),missing,next,boss,score,mark,r.optional(),r.unverified(),r.afkSeconds());
    }
    /** Follows unfinished prerequisites down to a quest that can be worked on now. Unknown quests are not assumed unfinished. */
    private static String firstStep(String quest,Snapshot s,Set<String> seen) {
        if(!seen.add(quest))return quest;
        String best=null;
        for(String q:GuideData.questPrerequisites().getOrDefault(quest,List.of())) {
            Quest st=s.quest(q);
            if(st==Quest.FINISHED || st==Quest.UNKNOWN)continue;
            if(best==null || order(q)<order(best))best=q;
        }
        return best==null?quest:firstStep(best,s,seen);
    }
    private static void checkQuantity(String label,Integer qty,List<String> missing,List<String> unknown) {
        if(qty==null)unknown.add(label+" quantity unavailable or stale");
        else if(qty<=0)missing.add(label+" missing from verified same-account stock");
    }

    private static String requirementText(GuideRule r,List<String> missing,List<String> unknown) {
        if(!missing.isEmpty()) return "Missing: "+String.join(", ",missing);
        if(!unknown.isEmpty()) return "Not observed: "+String.join(", ",unknown);
        if(!r.unverified().isEmpty()) return "Observed requirements met; check: "+r.unverified().get(0);
        return r.skills().isEmpty() && r.quests().isEmpty()?"No observable prerequisite":"Observed requirements met";
    }
    private static int order(String quest) {
        for(var e:GuideQuestOrder.entries()) if(e.name().equals(quest)) return e.order();
        return Integer.MAX_VALUE;
    }
    private static final Map<String,Integer> DEPENDENTS = dependents();
    private static Map<String,Integer> dependents() {
        Map<String,Integer> m=new HashMap<>();
        for(var e:GuideData.questPrerequisites().entrySet()) for(String q:e.getValue()) m.merge(q,1,Integer::sum);
        for(GuideRule r:GuideData.rules()) if(r.category()!=GuideRule.Category.QUEST) for(String q:r.quests()) m.merge(q,1,Integer::sum);
        return m;
    }
    static int score(GuideRule r,Snapshot s,State readiness,Quest own,GoalMark mark,Set<String> goalQuests) {
        int score=switch(readiness){case MET->300;case UNKNOWN->150;case UNMET->0;};
        if(mark==GoalMark.SAVED) score+=250;
        if(r.completesQuest()!=null && goalQuests.contains(r.completesQuest())) score+=150;
        switch(r.category()) {
            case QUEST -> {
                if(own==Quest.IN_PROGRESS) score+=400;
                score+=Math.max(0,100-r.order()/3);                       // follow the guide route
                score+=Math.min(60,DEPENDENTS.getOrDefault(r.completesQuest(),0)*6); // unlock value
            }
            case BOSS, LEGACY -> {
                String bank=r.outfitterBoss()==null?null:s.bossReadiness().get(r.outfitterBoss());
                if(bank!=null) score+=bank.startsWith("READY")?120:bank.startsWith("GEAR READY")?90:bank.startsWith("OWNED")?40:0;
                if(!r.needsOwned().isEmpty()) score+=100; // positively observed key item
                if(!r.quests().isEmpty() && readiness==State.MET) score+=40; // an unlock you have earned
            }
            // CP-2 (BQA-R12-006): look-away length is a filter (Attention.AFK), never a reward by itself.
            case AFK -> {}
            case MONEY -> score+=s.mode()==Mode.IRONMAN?Math.min(80,(r.gpLowK()+r.gpHighK())/30):0;
            case GEAR -> score+=r.optional()?0:20;
            case SKILL -> score+=30;
        }
        if(r.optional()) score-=40;
        boolean nothingObservable=r.skills().isEmpty() && r.quests().isEmpty() && r.doneAt().isEmpty()
            && r.needsOwned().isEmpty() && r.category()!=GuideRule.Category.QUEST && r.category()!=GuideRule.Category.LEGACY;
        if(nothingObservable) score-=90; // "ready" only because Burrow can verify nothing: rank below verified items
        // CP-2 (BQA-R12-006): no blanket reward for a higher requirement. A small bonus only when the method's
        // requirement is close to the player's own level in that skill (it is the account's current tier of method).
        if(readiness==State.MET && (r.category()==GuideRule.Category.AFK || r.category()==GuideRule.Category.MONEY)) {
            for(var e:r.skills().entrySet()) {
                Integer have=s.skills().get(e.getKey());
                if(have!=null && e.getValue()>1 && have-e.getValue()<=15) { score+=25; break; }
            }
        }
        return score;
    }
}
