package gg.feral.burrow.core;

import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;
import java.util.*;

/** W07: select a boss-specific, gear-backed combat method before strategy-only research.
 * Research gear is intentionally still provisional, never an approved bank recommendation.
 * An alternate style is NOT manufactured from a different boss's equipment. */
public final class StyleMethodPicker {
    private StyleMethodPicker() {}
    public enum Coverage { CATALOG_GEAR, RESEARCH_GEAR, STRATEGY_ONLY }
    public record Choice(String method, Tier gearTier, Coverage coverage, boolean hybrid) {
        public boolean hasGear(){return gearTier!=null;}
        public String displayStatus() {return switch(coverage) {
            case CATALOG_GEAR -> "Catalog gear plan";
            case RESEARCH_GEAR -> "Research gear example · CHECK (not bank-approved)";
            case STRATEGY_ONLY -> "Strategy research only · no approved gear plan";
        };}
    }
    /** Existing source gear, existing provisional gear examples and explicitly researched strategy notes. */
    public static boolean isProvisional(String boss,String method,Tier tier) {
        return ResearchCombatLoadouts.find(boss,method,tier)!=null ||
            ResearchOutfitterAdditions.find(boss,method,tier)!=null;
    }
    public static Choice choose(String boss,CombatStyle style,String current,Tier preferred) {
        Objects.requireNonNull(boss);Objects.requireNonNull(style);Objects.requireNonNull(preferred);
        var candidates=new ArrayList<Choice>();
        for(String method:TieredOutfitterCatalog.methods(boss)) {
            var styles=MethodStyles.of(boss,method);
            if (!styles.uses(style)) continue;
            Tier gearTier=null;
            // Keep the current equipment tier where available. Never invent a missing tier.
            for(Tier t:new Tier[]{preferred,Tier.RECOMMENDED,Tier.ENTRY,Tier.OPTIMAL}) {
                if(TieredOutfitterCatalog.find(boss,method,t)!=null) {gearTier=t;break;}
            }
            Coverage coverage=gearTier==null?Coverage.STRATEGY_ONLY:
                isProvisional(boss,method,gearTier)?Coverage.RESEARCH_GEAR:Coverage.CATALOG_GEAR;
            candidates.add(new Choice(method,gearTier,coverage,styles.primary()!=style));
        }
        if(candidates.isEmpty())return null;
        // An explicit alternative (Dharok, phase method etc) survives a click on its own primary style.
        for(var c:candidates)if(c.method().equals(current) && !c.hybrid())return c;
        // Prefer a genuine standalone gear plan over a hybrid and strategy-only entry.
        candidates.sort(Comparator.comparingInt((Choice c)->c.hybrid()?1:0)
            .thenComparingInt(c->c.coverage().ordinal())
            .thenComparingInt(c->c.gearTier()==preferred?0:1));
        return candidates.get(0);
    }
}
