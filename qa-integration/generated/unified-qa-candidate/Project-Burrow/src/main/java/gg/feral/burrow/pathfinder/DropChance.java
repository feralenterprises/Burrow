package gg.feral.burrow.pathfinder;

import java.util.*;

/**
 * Build 19 drop-chance planner (ironman.guide drop-calculator semantics, independently implemented).
 *
 * <p>Model: each eligible kill rolls the item independently with constant probability p.
 * P(at least one in n) = 1 - (1-p)^n, computed with log1p/expm1 for precision. This is a
 * probability, never a guarantee: for any p &lt; 1 the result is &lt; 1 for every finite n.
 * Multiple items are modelled ONLY under explicit independence between items; correlated rewards
 * (shared loot rolls, pity/bad-luck mechanics, team splits) are out of scope and must not use this.
 */
public final class DropChance {
    private DropChance() {}
    /** A sourced example rate (ironman.guide drop-calculator preset). */
    public record Preset(String label, long denominator, String source) {
        public double p() { return 1.0/denominator; }
    }
    public static final List<Preset> PRESETS = List.of(
        new Preset("Osmumten's fang (ToA 300)",75,GuideSources.IRONMAN_GUIDE_DROPS));

    public static double atLeastOne(double p,long kills) {
        check(p,kills);
        if(kills==0 || p==0)return 0;
        if(p==1)return 1;
        return -Math.expm1(kills*Math.log1p(-p));
    }
    /** Smallest n with P(at least one) >= confidence. Throws for unreachable targets (p==0 or confidence>=1). */
    public static long killsForConfidence(double p,double confidence) {
        if(!(p>0 && p<=1))throw new IllegalArgumentException("p must be in (0,1]");
        if(!(confidence>0 && confidence<1))throw new IllegalArgumentException("confidence must be in (0,1): never 100%");
        if(p==1)return 1;
        long n=(long)Math.ceil(Math.log1p(-confidence)/Math.log1p(-p));
        while(n>1 && atLeastOne(p,n-1)>=confidence)n--;
        while(atLeastOne(p,n)<confidence)n++;
        return n;
    }
    /** P(every item at least once) for independent items, each rolled once per kill. */
    public static double allOf(double[] ps,long kills) {
        double r=1; for(double p:ps) r*=atLeastOne(p,kills); return r;
    }
    /** P(at least one of the items) for independent items. */
    public static double anyOf(double[] ps,long kills) {
        double none=1; for(double p:ps) none*=1-atLeastOne(p,kills); return 1-none;
    }
    public static double expectedKills(double p) {
        if(!(p>0 && p<=1))throw new IllegalArgumentException("p");
        return 1/p;
    }
    /** P(no drop in n) = (1-p)^n; stays &gt; 0 for p &lt; 1 even where 1-P rounds to 1.0 in doubles. */
    public static double noDrop(double p,long kills) {
        check(p,kills);
        if(p==1)return kills==0?1:0;
        return Math.exp(kills*Math.log1p(-p));
    }
    /** Display never shows 100% for a probabilistic drop. */
    public static String percent(double x) {
        if(x>=0.9995 && x<=1.0) return ">99.9%";
        return String.format(Locale.ROOT,"%.1f%%",x*100);
    }
    private static void check(double p,long kills) {
        if(!(p>=0 && p<=1) || Double.isNaN(p))throw new IllegalArgumentException("p");
        if(kills<0)throw new IllegalArgumentException("kills");
    }
}
