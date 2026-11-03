package gg.feral.burrow.core;

import gg.feral.burrow.core.RareDropTable.Drop;
import java.util.*;

/**
 * Build 20 multi-drop probability that respects shared loot rolls.
 *
 * <p>Per roll of a group, the selected items in that group are mutually exclusive, so
 * P(none of T from that group on one roll) = 1 - sum(p_i). A group rolled r times per kill over n
 * kills gives (1 - sum p_i)^(r*n). Groups (and tertiaries) are independent, so the per-group terms
 * multiply. P(ANY) = 1 - P(none of S). P(ALL) uses inclusion-exclusion over subsets of S:
 * P(all) = sum over T subset of S of (-1)^|T| * P(none of T). Exact for this model; no
 * independence is assumed between items that share a roll.
 *
 * <p>Never reports 100%; a probabilistic drop is never guaranteed.
 */
public final class RareDropMath {
    private RareDropMath() {}
    public static final int MAX_SELECTED = 12;
    public static final double[] MILESTONES = {0.50, 0.75, 0.90, 0.95};

    /** P(none of the given drops in n kills). */
    public static double none(Collection<Drop> drops, long kills) {
        if(kills<0) throw new IllegalArgumentException("kills");
        Map<String,double[]> groups=new LinkedHashMap<>(); // group -> {sumP, rolls}
        for(Drop d:drops) {
            double[] g=groups.computeIfAbsent(d.group(),k->new double[]{0,d.rolls()});
            if(g[1]!=d.rolls()) throw new IllegalArgumentException("inconsistent rolls in group "+d.group());
            g[0]+=d.perRoll();
        }
        double logNone=0;
        for(double[] g:groups.values()) {
            if(g[0]>1+1e-12) throw new IllegalArgumentException("group probability exceeds 1");
            if(g[0]>=1) return kills==0?1:0;
            logNone+=g[1]*kills*Math.log1p(-g[0]);
        }
        return Math.exp(logNone);
    }
    public static double any(List<Drop> selected, long kills) {
        check(selected);
        double r=1-none(selected,kills);
        return r<=0?0:r;
    }
    public static double all(List<Drop> selected, long kills) {
        check(selected);
        int k=selected.size(); double sum=0;
        for(int mask=0;mask<(1<<k);mask++) {
            List<Drop> t=new ArrayList<>();
            for(int i=0;i<k;i++) if((mask&(1<<i))!=0) t.add(selected.get(i));
            double term=t.isEmpty()?1:none(t,kills);
            sum+=(Integer.bitCount(mask)%2==0?1:-1)*term;
        }
        return Math.max(0,Math.min(1,sum));
    }
    public enum Mode { ANY, ALL }
    public static double chance(Mode mode, List<Drop> selected, long kills) {
        return mode==Mode.ANY?any(selected,kills):all(selected,kills);
    }
    /** Smallest total kill count n with chance >= target. Monotone in n, found by doubling + bisection. */
    public static long killsFor(Mode mode, List<Drop> selected, double target) {
        check(selected);
        if(!(target>0 && target<1)) throw new IllegalArgumentException("target must be in (0,1): never 100%");
        long hi=1;
        while(chance(mode,selected,hi)<target) { hi*=2; if(hi>(1L<<40)) throw new IllegalStateException("unreachable"); }
        long lo=0;
        while(hi-lo>1) { long mid=lo+(hi-lo)/2; if(chance(mode,selected,mid)>=target) hi=mid; else lo=mid; }
        return hi;
    }
    public static String percent(double x) {
        if(x>=0.9995) return ">99.9%";
        if(x>0 && x<0.0005) return "<0.1%";
        return String.format(Locale.ROOT,"%.1f%%",x*100);
    }
    private static void check(List<Drop> s) {
        if(s==null || s.isEmpty()) throw new IllegalArgumentException("select at least one drop");
        if(s.size()>MAX_SELECTED) throw new IllegalArgumentException("too many drops");
        if(new HashSet<>(s.stream().map(Drop::key).toList()).size()!=s.size()) throw new IllegalArgumentException("duplicate drop");
    }
}
