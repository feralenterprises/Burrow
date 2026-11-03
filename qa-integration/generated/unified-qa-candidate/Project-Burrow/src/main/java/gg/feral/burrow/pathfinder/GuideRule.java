package gg.feral.burrow.pathfinder;

import java.util.*;

/**
 * Build 19: one structured, source-linked guide rule. Original compact summary, never copied prose.
 *
 * @param skills        real-level minimums (RuneLite Skill#getName keys)
 * @param quests        quests that must be FINISHED (RuneLite Quest#getName keys)
 * @param unverified    requirements Burrow cannot observe (diaries, QP, kudos, partial quest steps)
 * @param completesQuest when this RuneLite quest is FINISHED the rule is done and never suggested
 * @param skipIfOwned   positive same-account bank observation of any of these means already obtained
 * @param needsOwned    a positive observation of one of these is required (e.g. Emberlight for TD)
 * @param order         guide order (lower = earlier in the source route); 0 when not applicable
 * @param doneAt        milestone is complete once every listed real level is reached
 * @param doneWhenStarted quest-unlock milestone complete once completesQuest is IN_PROGRESS or FINISHED
 */
public record GuideRule(String id, String title, Category category, String section,
                        SuggestionEngine.Focus focus, SuggestionEngine.Attention attention, int minimumMinutes,
                        Map<String,Integer> skills, List<String> quests, List<String> unverified,
                        String completesQuest, Set<Integer> skipIfOwned, Set<Integer> needsOwned, int order,
                        String why, String nextStep, String source, String outfitterBoss, ModeScope mode,
                        int afkSeconds, int gpLowK, int gpHighK, boolean optional,
                        Map<String,Integer> doneAt, boolean doneWhenStarted) {
    public enum Category { QUEST, BOSS, AFK, MONEY, GEAR, SKILL, LEGACY }
    public enum ModeScope { BOTH, IRONMAN, MAIN }
    public GuideRule {
        Objects.requireNonNull(id); Objects.requireNonNull(title); Objects.requireNonNull(category);
        Objects.requireNonNull(focus); Objects.requireNonNull(attention); Objects.requireNonNull(source);
        skills=Map.copyOf(skills); quests=List.copyOf(quests); unverified=List.copyOf(unverified);
        skipIfOwned=Set.copyOf(skipIfOwned); needsOwned=Set.copyOf(needsOwned); doneAt=Map.copyOf(doneAt);
        if(minimumMinutes<=0)throw new IllegalArgumentException("minutes "+id);
        if(id.isBlank())throw new IllegalArgumentException("id");
        for(var v:skills.values()) if(v<1 || v>99) throw new IllegalArgumentException("level "+id);
    }
    /** Fluent builder keeps GuideData readable. */
    static Builder of(String id,String title,Category category) { return new Builder(id,title,category); }
    static final class Builder {
        private final String id,title; private final Category category;
        private String section="", why="", next="", source=GuideSources.IRONMAN_GUIDE, boss=null, completes=null;
        private SuggestionEngine.Focus focus=SuggestionEngine.Focus.ANY;
        private SuggestionEngine.Attention attention=SuggestionEngine.Attention.ACTIVE;
        private int minutes=20, order=0, afk=0, gpLow=0, gpHigh=0; private boolean optional;
        private final Map<String,Integer> skills=new LinkedHashMap<>();
        private final List<String> quests=new ArrayList<>(), unverified=new ArrayList<>();
        private final Set<Integer> skip=new LinkedHashSet<>(), needs=new LinkedHashSet<>();
        private ModeScope mode=ModeScope.BOTH;
        private final Map<String,Integer> doneAt=new LinkedHashMap<>(); private boolean doneWhenStarted;
        Builder doneAt(String s,int l){doneAt.put(s,l);return this;}
        Builder doneWhenStarted(){doneWhenStarted=true;return this;}
        Builder(String id,String title,Category category){this.id=id;this.title=title;this.category=category;}
        Builder section(String s){section=s;return this;}
        Builder focus(SuggestionEngine.Focus f){focus=f;return this;}
        Builder low(){attention=SuggestionEngine.Attention.LOW;return this;}
        Builder minutes(int m){minutes=m;return this;}
        Builder skill(String s,int l){skills.merge(s,l,Math::max);return this;}
        Builder quest(String q){if(!quests.contains(q))quests.add(q);return this;}
        Builder quests(Collection<String> q){q.forEach(this::quest);return this;}
        Builder unverified(String u){unverified.add(u);return this;}
        Builder completes(String q){completes=q;return this;}
        Builder skipIfOwned(int... ids){for(int i:ids)skip.add(i);return this;}
        Builder needsOwned(int... ids){for(int i:ids)needs.add(i);return this;}
        Builder order(int o){order=o;return this;}
        Builder why(String w){why=w;return this;}
        Builder next(String n){next=n;return this;}
        Builder source(String s){source=s;return this;}
        Builder boss(String b){boss=b;return this;}
        Builder mode(ModeScope m){mode=m;return this;}
        Builder afk(int seconds){afk=seconds;return this;}
        Builder gp(int lowK,int highK){gpLow=lowK;gpHigh=highK;return this;}
        Builder optional(){optional=true;return this;}
        GuideRule build(){return new GuideRule(id,title,category,section,focus,attention,minutes,skills,quests,unverified,
            completes,skip,needs,order,why,next,source,boss,mode,afk,gpLow,gpHigh,optional,doneAt,doneWhenStarted);}
    }
}
