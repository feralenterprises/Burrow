package gg.feral.burrow.pathfinder;

import java.util.*;
/** Source-scoped finite input kits. KIT_PRESENT is NOT quest-completion, combat viability or time proof. */
public final class PathfinderSupplyKit {
    private PathfinderSupplyKit() {}
    public enum Verdict { KIT_PRESENT, VERIFIED_SHORT, UNKNOWN, NOT_MODELLED }
    public record ItemNeed(String label, int required, Set<Integer> alternatives) {
        public ItemNeed { if(required<=0 || alternatives.isEmpty())throw new IllegalArgumentException(); alternatives=Set.copyOf(alternatives); }
    }
    public record Result(String activity, Verdict verdict, List<String> details, boolean guaranteesCompletion) {
        public Result { details=List.copyOf(details); }
    }
    private static final Map<String,List<ItemNeed>> KITS=Map.of(
        "quest_cooks_assistant",List.of(new ItemNeed("egg",1,Set.of(1944)),
            new ItemNeed("bucket of milk",1,Set.of(1927)),new ItemNeed("pot of flour",1,Set.of(1933))),
        "quest_priest_in_peril",List.of(new ItemNeed("unnoted rune/pure essence",50,Set.of(1436,7936)),
            new ItemNeed("bucket",1,Set.of(1925))),
        "afk_karambwan",List.of(new ItemNeed("raw karambwanji",1,Set.of(3150)),
            new ItemNeed("karambwan vessel",1,Set.of(3157,3159))));

    public static Result evaluate(String activity, SuggestionEngine.ResourceEvidence resources) {
        List<ItemNeed> needs=KITS.get(activity);
        if(needs==null)return new Result(activity,Verdict.NOT_MODELLED,List.of("No source-approved finite kit"),false);
        List<String> report=new ArrayList<>();boolean unknown=false,shortage=false;
        for(ItemNeed need:needs) {
            int[] ids=need.alternatives().stream().sorted().mapToInt(Integer::intValue).toArray();
            var evidence=resources==null?SuggestionEngine.State.UNKNOWN:resources.alternativeRequirement(need.required(),ids);
            // A nonzero known subtotal cannot prove a shortfall if another alternative is unobserved.
            Integer count=resources==null?null:resources.anyQuantity(ids);
            if(evidence==SuggestionEngine.State.UNKNOWN) {unknown=true;report.add("UNKNOWN "+need.label()+" need "+need.required()+" (partial quantities are not a shortage proof)");}
            else if(evidence==SuggestionEngine.State.UNMET) {shortage=true;report.add("SHORT "+need.label()+" "+count+"/"+need.required());}
            else report.add("PRESENT "+need.label()+" at least "+need.required());
        }
        // A positively verified shortage is still true even if another independent input is unknown.
        Verdict v=shortage?Verdict.VERIFIED_SHORT:unknown?Verdict.UNKNOWN:Verdict.KIT_PRESENT;
        if(activity.equals("afk_karambwan"))report.add("Fishing yields, bait loss, route time and trip duration NOT GUARANTEED");
        else report.add("Kit does NOT verify combat, travel, quest steps, or successful completion");
        return new Result(activity,v,report,false);
    }
}
