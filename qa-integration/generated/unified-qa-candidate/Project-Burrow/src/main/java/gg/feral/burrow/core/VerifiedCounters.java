package gg.feral.burrow.core;

import java.util.*;

/**
 * BQA-IMPL-R10: pure helpers for the three Home/weekly counters, each tied to one verified source.
 *
 * <ul>
 *   <li><b>Level-ups</b>: from XP already accepted by the verified XP feed (fresh baseline, account/epoch fences). Levels are
 *       derived from experience with the standard OSRS table (levels 1–99), not from {@code StatChanged.level}, whose
 *       real-vs-boosted meaning is not documented in the RuneLite javadoc.</li>
 *   <li><b>Collection unlocks</b>: the game's own message {@value #COLLECTION_PREFIX}…, the same text RuneLite's Screenshot
 *       plugin keys on, accepted only from GAMEMESSAGE and only while the game's "collection log new item" chat option is on
 *       (checked by the host when such a message arrives). The counter becomes "tracked" for a login at its first verified
 *       message; a week with none shows "not tracked" rather than an unprovable 0.</li>
 *   <li><b>Confirmed chests</b>: verified Barrows chest-count deltas from the existing KC adapter (the only chest grammar it
 *       accepts); shown as "(Barrows)".</li>
 * </ul>
 */
public final class VerifiedCounters {
    private VerifiedCounters() {}
    public static final String LEVEL_UPS = "level-ups", COLLECTION = "collection unlocks", CHESTS = "confirmed chests";
    public static final String COLLECTION_PREFIX = "New item added to your collection log: ";
    public static final String CHEST_ENCOUNTER = "Barrows";
    public static final int MAX_LEVEL = 99;
    private static final int[] XP_FOR_LEVEL = new int[MAX_LEVEL + 1];
    static {
        double points = 0;
        XP_FOR_LEVEL[1] = 0;
        for (int lvl = 1; lvl < MAX_LEVEL; lvl++) {
            points += Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
            XP_FOR_LEVEL[lvl + 1] = (int) Math.floor(points / 4);
        }
    }
    public static int xpForLevel(int level) {
        if (level < 1 || level > MAX_LEVEL) throw new IllegalArgumentException("level");
        return XP_FOR_LEVEL[level];
    }
    public static int levelForXp(long xp) {
        if (xp < 0) throw new IllegalArgumentException("xp");
        int lvl = 1;
        while (lvl < MAX_LEVEL && xp >= XP_FOR_LEVEL[lvl + 1]) lvl++;
        return lvl;
    }
    /** Real levels gained between two verified XP readings of one skill (0 when none or on bad input). */
    public static int levelsGained(long xpBefore, long xpAfter) {
        if (xpBefore < 0 || xpAfter <= xpBefore) return 0;
        return levelForXp(xpAfter) - levelForXp(xpBefore);
    }
    /** Item name from the game's collection-log chat message; empty for anything else. Strips colour tags. */
    public static Optional<String> collectionItem(String message) {
        if (message == null) return Optional.empty();
        String m = message.replaceAll("<[^>]*>", "").trim();
        if (!m.startsWith(COLLECTION_PREFIX)) return Optional.empty();
        String item = m.substring(COLLECTION_PREFIX.length()).trim();
        return item.isEmpty() || item.length() > 80 ? Optional.empty() : Optional.of(item);
    }
}
