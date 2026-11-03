package gg.feral.burrow.core;

import java.util.Arrays;
import java.util.Map;

/**
 * The O bank tab is a WITHDRAWAL view, not an account-wide owned-gear preview.
 * The Best in Bank planner considers BANK + INVENTORY + EQUIPMENT; only the
 * exact positive item IDs actually in BANK can be clicked to withdraw.
 * Wiki fallback (0), absent optional (-1), and already-carried items are omitted.
 */
public final class WithdrawableBankLayout {
    private WithdrawableBankLayout() {}

    public static int[] fromBestPlan(int[] bestPlanPositions, Map<Integer, Integer> bankStock) {
        if (bestPlanPositions == null || bestPlanPositions.length != 56 || bankStock == null)
            throw new IllegalArgumentException("Verified 56-slot Best in Bank plan and bank stock required");
        int[] result = new int[56];
        Arrays.fill(result, -1);
        for (int i = 0; i < bestPlanPositions.length; i++) {
            int id = bestPlanPositions[i];
            if (id > 0 && bankStock.getOrDefault(id, 0) > 0) result[i] = id;
        }
        return result;
    }
    public static int withdrawableSlots(int[] positions) {
        if(positions == null || positions.length != 56)throw new IllegalArgumentException("56-slot bank layout required");
        int count = 0;
        for (int id : positions) if (id > 0) count++;
        return count;
    }
}
