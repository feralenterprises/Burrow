package gg.feral.burrow.core;

import java.util.HashMap;
import java.util.Map;

/** Ported from Iron Ledger T063: only post-baseline positive XP is a gain. A session transition invalidates baselines. */
public final class IronLedgerXpBaselines {
    private final Map<String, Integer> previous = new HashMap<>();
    private String accountKey;

    public void transition(String nextAccountKey) {
        accountKey = nextAccountKey;
        previous.clear();
    }
    public void seed(String key, String skill, int xp) {
        if (key != null && key.equals(accountKey) && xp >= 0) previous.put(skill, xp);
    }
    public int observe(String key, String skill, int xp) {
        if (key == null || !key.equals(accountKey) || xp < 0) return 0;
        Integer before = previous.get(skill);
        if (before == null) { previous.put(skill, xp); return 0; }
        // RuneLite may briefly expose a stale or lower reading during transitions.
        // Never let it lower the baseline and manufacture XP on the next update.
        if (xp <= before) return 0;
        previous.put(skill, xp);
        return xp - before;
    }
    public int last(String skill) { return previous.getOrDefault(skill, -1); }
}
