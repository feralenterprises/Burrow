package gg.feral.burrow.pathfinder;

import java.util.*;

/** Conservative per-action budget for a small, Wiki-verified subset of methods.
 *  No action rate, success rate, spellbook, equipment rune-substitution or game action is inferred.
 */
public final class PathfinderActionBudget {
    private PathfinderActionBudget() {}
    public enum Verdict { ENOUGH_FOR_ACTION, VERIFIED_SHORT, UNKNOWN, NOT_MODELLED }
    public record Result(String id, Verdict verdict, Integer maximumCasts, String variant,
                         List<String> notes, boolean guaranteesStartOrCompletion) {
        public Result {notes=List.copyOf(notes);}
    }
    private record Recipe(String name, int logId, int coinCost) {}
    // Exact wiki item IDs and 70%-of-sawmill coin fees, verified 2026-11-03.
    // The budget excludes unlisted future variants rather than guessing their IDs or costs.
    private static final List<Recipe> PLANKS=List.of(
        new Recipe("normal",1511,70),new Recipe("oak",1521,175),
        new Recipe("teak",6333,350),new Recipe("mahogany",6332,1050),
        new Recipe("camphor",32904,1750),new Recipe("ironwood",32907,3500),
        new Recipe("rosewood",32910,5250));
    public static Result evaluate(String id,SuggestionEngine.ResourceEvidence r) {
        if(!id.equals("afk_plank_make"))
            return new Result(id,Verdict.NOT_MODELLED,null,"",List.of("No verified finite-action recipe"),false);
        if(r==null||!r.reliable())
            return new Result(id,Verdict.UNKNOWN,null,"",List.of("Same-account fresh quantities unavailable"),false);
        Integer gp=r.quantity(995), astral=r.quantity(9075), nature=r.quantity(561), earth=r.quantity(557);
        // All 4 input groups are needed per cast. No rune substitutions or charged items are assumed.
        List<Result> candidates=new ArrayList<>();
        for(Recipe recipe:PLANKS) {
            Integer logs=r.quantity(recipe.logId());
            boolean fixedShort=(logs!=null&&logs==0)||(gp!=null&&gp<recipe.coinCost());
            // Rune-short is not a certified 'impossible' claim: staves/other rune providers
            // are outside this budget. Exact runes are used only for positive sufficiency.
            if(fixedShort) {
                candidates.add(new Result(id,Verdict.VERIFIED_SHORT,0,recipe.name(),List.of("No physical "+recipe.name()+" log or insufficient coins for one cast"),false));
            } else if(logs==null||gp==null||astral==null||nature==null||earth==null ||
                astral<2 || nature<1 || earth<15) {
                candidates.add(new Result(id,Verdict.UNKNOWN,null,recipe.name(),
                    List.of("Incomplete exact rune/coin/log proof; staff substitutions and Lunar spellbook not observed"),false));
            } else {
                int casts=Math.min(logs,Math.min(gp/recipe.coinCost(),Math.min(astral/2,Math.min(nature,earth/15))));
                if(casts>=1) candidates.add(new Result(id,Verdict.ENOUGH_FOR_ACTION,casts,recipe.name(),
                    List.of("Verified exact items for up to "+casts+" "+recipe.name()+" casts", "Current Lunar spellbook, future stock and actual casts are unverified"),false));
                else candidates.add(new Result(id,Verdict.UNKNOWN,null,recipe.name(),
                    List.of("Rune supply may require a worn staff or other substitution not certified"),false));
            }
        }
        Optional<Result> best=candidates.stream().filter(x->x.verdict()==Verdict.ENOUGH_FOR_ACTION)
            .max(Comparator.comparingInt(x->x.maximumCasts()));
        if(best.isPresent())return best.get();
        if(candidates.stream().anyMatch(x->x.verdict()==Verdict.UNKNOWN))
            return new Result(id,Verdict.UNKNOWN,null,"",List.of("No fully verified log recipe; unknown inputs may still work"),false);
        return new Result(id,Verdict.VERIFIED_SHORT,0,"",List.of("No eligible log/coin combination among seven Wiki-listed log variants for one cast; future variants are not covered"),false);
    }
}
