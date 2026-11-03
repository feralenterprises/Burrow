package gg.feral.burrow.core;

import java.util.*;

/**
 * Build 19: potion dose families for supply allocation. Every ID verified against RuneLite 1.13.4
 * {@code net.runelite.api.ItemID} constants (e.g. PRAYER_POTION4=2434, PRAYER_POTION3=139 ...).
 * A planned (4)-dose slot may be filled by a lower dose of the SAME potion and is then labelled
 * LOWER DOSE; a different potion is never substituted.
 */
public final class DoseFamilies {
    private DoseFamilies() {}
    private static final List<int[]> FAMILIES = List.of(
        new int[]{2434,139,141,143},          // Prayer potion
        new int[]{3024,3026,3028,3030},       // Super restore
        new int[]{12695,12697,12699,12701},   // Super combat potion
        new int[]{23685,23688,23691,23694},   // Divine super combat potion
        new int[]{2444,169,171,173},          // Ranging potion
        new int[]{12625,12627,12629,12631},   // Stamina potion
        new int[]{2436,145,147,149},          // Super attack
        new int[]{2440,157,159,161},          // Super strength
        new int[]{2442,163,165,167},          // Super defence
        new int[]{113,115,117,119},           // Strength potion
        new int[]{6685,6687,6689,6691},       // Saradomin brew
        new int[]{22209,22212,22215,22218});  // Extended super antifire
    /** Highest dose first, starting with the planned ID itself. Unknown item: just itself. */
    public static List<Integer> candidates(int plannedId) {
        for(int[] f:FAMILIES) for(int i=0;i<f.length;i++) if(f[i]==plannedId) {
            List<Integer> out=new ArrayList<>();
            out.add(plannedId);
            for(int j=i+1;j<f.length;j++)out.add(f[j]);
            return out;
        }
        return List.of(plannedId);
    }
    public static int doses(int id) {
        for(int[] f:FAMILIES) for(int i=0;i<f.length;i++) if(f[i]==id) return 4-i;
        return -1;
    }
}
