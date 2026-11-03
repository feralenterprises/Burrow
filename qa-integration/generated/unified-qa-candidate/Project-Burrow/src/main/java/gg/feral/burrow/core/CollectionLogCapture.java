package gg.feral.burrow.core;

import java.util.*;

/**
 * R11: read-only capture of collection-log unlocks from client script 4100, the per-item callback the game runs while it
 * builds the collection-log search list. WikiSync (BSD-2, github.com/weirdgloop/WikiSync) reads the same signal: item id
 * in script argument 1, list considered complete two ticks after the last callback.
 *
 * <p>TrashPanda NEVER triggers the search, clicks, or runs scripts: it only listens when the game (the player's own search,
 * or another plugin such as WikiSync) runs it, so it cannot interfere with WikiSync. Positive-only: an item absent from a
 * capture is not "not obtained". Ids are canonicalised through the cache's replacement enum (WikiSync uses enum 3721),
 * supplied by the host. Script 4100's meaning is inferred from WikiSync, not documented by RuneLite: CHECK natively.
 */
public final class CollectionLogCapture {
    public static final int SCRIPT_ID = 4100;
    public static final int QUIET_TICKS = 2;
    private final Set<Integer> pending = new LinkedHashSet<>();
    private int lastTick = Integer.MIN_VALUE;
    private Map<Integer,Integer> replacements = Map.of();

    public void setReplacements(Map<Integer,Integer> r) { replacements = r == null ? Map.of() : Map.copyOf(r); }

    /** @return true when a valid item id was taken from the callback arguments. */
    public boolean offer(int scriptId, Object[] args, int tick) {
        if (scriptId != SCRIPT_ID || args == null || args.length < 2 || !(args[1] instanceof Integer id) || id <= 0) return false;
        pending.add(id);
        lastTick = tick;
        return true;
    }
    public boolean ready(int tick) { return !pending.isEmpty() && tick >= lastTick + QUIET_TICKS; }
    /** Canonical ids (raw id plus its replacement when the cache maps it), then clears. */
    public Set<Integer> drain() {
        Set<Integer> out = new LinkedHashSet<>();
        for (int id : pending) { out.add(id); Integer r = replacements.get(id); if (r != null && r > 0) out.add(r); }
        pending.clear(); lastTick = Integer.MIN_VALUE;
        return out;
    }
    /** Same canonicalisation for a drawn page's slot ids (raw id kept, replacement added). */
    public Set<Integer> canonical(Set<Integer> ids) {
        Set<Integer> out = new LinkedHashSet<>();
        for (int id : ids) { out.add(id); Integer r = replacements.get(id); if (r != null && r > 0) out.add(r); }
        return out;
    }
    public CollectionLogParser.Observation canonical(CollectionLogParser.Observation o) {
        return new CollectionLogParser.Observation(o.page(), o.killCounts(), canonical(o.obtained()), canonical(o.listed()));
    }
    /** Hop, loading, logout or account change: drop an unfinished capture (WikiSync does the same). */
    public void reset() { pending.clear(); lastTick = Integer.MIN_VALUE; }
    public int pendingCount() { return pending.size(); }
}
