package gg.feral.burrow.pathfinder;

import java.util.*;

/**
 * Build 19 Pathfinder coverage validator and gap register. Reports what was actually ingested, by
 * source and section, and lists exactly what was NOT ingested. Used by tests and the Pathfinder UI.
 */
public final class GuideCoverage {
    private GuideCoverage() {}
    public record Report(Map<String,Integer> byCategory, Map<String,Integer> bySection,
                         int questEntries, int questsMatchedToRuneLite, List<String> questsNotInRuneLite,
                         int afkMethods, int moneyMethods, int outfitterEncounters, List<String> gaps) {}

    /** @param runeLiteQuestNames names from RuneLite 1.13.4 Quest.values() (host supplied; tests pass the dump) */
    public static Report report(Collection<String> runeLiteQuestNames) {
        Map<String,Integer> byCategory=new TreeMap<>(), bySection=new TreeMap<>();
        int afk=0,money=0,bosses=0;
        Set<String> bossNames=new TreeSet<>();
        for(GuideRule r:GuideData.rules()) {
            byCategory.merge(r.category().name(),1,Integer::sum);
            bySection.merge(r.section().isBlank()?"(unsectioned)":r.section(),1,Integer::sum);
            if(r.category()==GuideRule.Category.AFK)afk++;
            if(r.category()==GuideRule.Category.MONEY)money++;
            if(r.outfitterBoss()!=null)bossNames.add(r.outfitterBoss());
        }
        bosses=bossNames.size();
        Set<String> rl=new HashSet<>(runeLiteQuestNames);
        List<String> notIn=new ArrayList<>();
        int matched=0;
        for(var e:GuideQuestOrder.entries()) { if(rl.contains(e.name()))matched++; else notIn.add(e.name()); }
        List<String> gaps=new ArrayList<>(List.of(
            "ironman.guide main route: the 576 individual steps are an interactive checklist that was not ingested step by step; Burrow models its 6 sections + Sailing track through quest order, milestones and unlocks. Use the site for the full checklist.",
            "ironman.guide Sailing track (12 steps): not modelled beyond Sailing quests and Sailing AFK/money methods.",
            "Guide:Mootrius_Ironman_Guide: not fetched or compared in Build 19; no rules derived from it.",
            "Quest prerequisites: full recursive Wiki requirement lists only for Song of the Elves, Dragon Slayer II, While Guthix Sleeps, Monkey Madness II and Desert Treasure II, plus RFD structure and named sequels; other quests use ironman.guide's listed skill levels and order only.",
            "Quest points, kudos, combat level, diaries, combat achievements and minigame unlocks are not observed by Burrow; shown as 'check' notes, never as met.",
            "Drop rates: no boss drop table is bundled; the Pathfinder drop card shows only the sourced ironman.guide preset (1/75) at hypothetical KC milestones. Owner-entered rates and real KC totals are not wired into the UI; no KC is invented.",
            "Money-making GP/hr are ironman.guide estimates; not verified, not live prices; Main-account market value is not modelled.",
            "AFK durations are ironman.guide figures; equipment that changes them (fish barrel, log basket) is listed as a check, not detected.",
            "Gear-goal wear/acquisition levels for Slayer drops were authored from Wiki item/monster knowledge without a per-page fetch in Build 19; labelled 'verify'."));
        if(!notIn.isEmpty()) gaps.add("Guide quest entries without a RuneLite Quest id (never suggested): "+String.join(", ",notIn));
        return new Report(byCategory,bySection,GuideQuestOrder.entries().size(),matched,notIn,afk,money,bosses,gaps);
    }

    /** Throws if the quest prerequisite graph has a cycle or self-reference. */
    public static void requireAcyclic() {
        Map<String,List<String>> g=GuideData.questPrerequisites();
        Map<String,Integer> state=new HashMap<>();
        for(String q:g.keySet()) visit(q,g,state,new ArrayDeque<>());
    }
    private static void visit(String q,Map<String,List<String>> g,Map<String,Integer> state,Deque<String> path) {
        Integer st=state.get(q);
        if(st!=null && st==2)return;
        if(st!=null && st==1)throw new IllegalStateException("quest prerequisite cycle: "+path+" -> "+q);
        state.put(q,1); path.push(q);
        for(String p:g.getOrDefault(q,List.of())) {
            if(p.equals(q))throw new IllegalStateException("self prerequisite "+q);
            visit(p,g,state,path);
        }
        path.pop(); state.put(q,2);
    }
}
