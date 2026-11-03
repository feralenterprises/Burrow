package gg.feral.burrow.ui;

import gg.feral.burrow.core.CompletedSessionHistory;
import gg.feral.burrow.ui.BurrowPanel.*;
import java.util.*;
import java.util.function.Supplier;

/**
 * BQA-902 F04/F12 (SESS-03): the current login's session truth, held OUTSIDE Swing.
 *
 * <p>The plugin records every session event it actually emits (after its own gates). A new or recreated
 * {@link BurrowPanel} is then rebuilt from {@link #replay(Supplier)}: one start, the feed states, one row per
 * skill / boss / drop (already-aggregated totals), the loot-estimate count, and a trailing pause when the login
 * is suspended. Replay never mutates the ledger, so rendering or re-opening the sidebar can never add totals.
 *
 * <p>At a confirmed logout {@link #completedSummary(long)} produces the immutable card for
 * {@link CompletedSessionHistory}. Feeds that never became available are recorded as UNAVAILABLE, not zero.
 * Thread-safe (all methods synchronized); never stores account identifiers.
 */
public final class LiveSessionLedger {
    private SessionScope scope;
    private SessionOrigin origin;
    private long startMillis;
    private boolean paused;
    private final Set<String> seenIds = new HashSet<>();
    private final EnumMap<SessionFeed,FeedStatus> feeds = new EnumMap<>(SessionFeed.class);
    private final EnumSet<SessionFeed> everAvailable = EnumSet.noneOf(SessionFeed.class);
    private final Map<String,Long> xp = new LinkedHashMap<>();
    private final Map<String,Long> kc = new LinkedHashMap<>();
    private final Map<String,Long> loot = new LinkedHashMap<>();
    private final Map<String,SessionItem> drops = new LinkedHashMap<>();
    private final Map<String,SessionItem> supplies = new LinkedHashMap<>();
    private boolean pausedEver;
    // ---- R07: "recent activity" for Home (same accepted events as Sessions; never re-derived from replay) ----
    public enum ActivityKind { XP, BOSS_KC, DROP, LOOT_ESTIMATE, LEVEL_UP, COLLECTION }
    public record Activity(ActivityKind kind, String text, String detail, int itemId, long atMillis, String key, long amount) {}
    public static final int RECENT_CAP = 20;
    /** XP events for one skill within this window are merged into one row (StatChanged fires per action). */
    public static final long XP_MERGE_MILLIS = 60_000L;
    private final ArrayDeque<Activity> recent = new ArrayDeque<>();
    public synchronized List<Activity> recent() { return List.copyOf(recent); }
    public synchronized long totalXp() { return xp.values().stream().mapToLong(Long::longValue).sum(); }
    public synchronized long totalVerifiedKc() { return kc.values().stream().mapToLong(Long::longValue).sum(); }
    public synchronized long dropItemCount() { return drops.values().stream().mapToLong(SessionItem::quantity).sum(); }
    public synchronized long lootEstimates() { return loot.values().stream().mapToLong(Long::longValue).sum(); }
    // ---- R08: per-week contributions of THIS login (attributed by each event's own timestamp) ----
    private java.time.ZoneId zone = java.time.ZoneId.systemDefault();
    private static final class WeekAcc {
        final Map<String,Long> xp = new TreeMap<>(), kc = new TreeMap<>(), counters = new TreeMap<>();
        long drops, est;
    }
    private final Map<java.time.LocalDate,WeekAcc> weekly = new TreeMap<>();
    private final Set<String> trackedCounters = new TreeSet<>();
    public synchronized void setZone(java.time.ZoneId z) { zone = Objects.requireNonNull(z); }
    public synchronized java.time.ZoneId zone() { return zone; }
    private WeekAcc week(long at) { return weekly.computeIfAbsent(gg.feral.burrow.core.WeeklyProgress.weekStart(at, zone), k -> new WeekAcc()); }
    /** R10 hook: declare that this login observes counter {@code name} from a verified source (else "not tracked"). */
    public synchronized void trackCounter(String name) { if (scope != null) trackedCounters.add(name); }
    /** R10 hook: a verified counter observation (level-up, collection unlock, confirmed chest). */
    public synchronized boolean recordCounter(String name, long amount, long nowMillis) {
        if (scope == null || paused || amount <= 0 || !trackedCounters.contains(name)) return false;
        week(nowMillis).counters.merge(name, amount, Math::addExact);
        return true;
    }
    /** R10: verified level-up(s) for one skill, derived by the host from accepted XP. */
    public synchronized boolean recordLevelUp(String skill, int levels, int newLevel, long nowMillis) {
        if (skill == null || levels <= 0 || newLevel < 2) return false;
        if (!recordCounter(gg.feral.burrow.core.VerifiedCounters.LEVEL_UPS, levels, nowMillis)) return false;
        pushActivity(new Activity(ActivityKind.LEVEL_UP, skill + " level " + newLevel + (levels > 1 ? " (+" + levels + ")" : ""),
            "From verified XP", -1, nowMillis, "lvl:" + skill, levels));
        return true;
    }
    /** R10: the game's "New item added to your collection log" message, only while that source is tracked. */
    public synchronized boolean recordCollectionUnlock(String itemName, long nowMillis) {
        if (itemName == null || itemName.isBlank()) return false;
        if (!recordCounter(gg.feral.burrow.core.VerifiedCounters.COLLECTION, 1, nowMillis)) return false;
        pushActivity(new Activity(ActivityKind.COLLECTION, "Collection log: " + itemName, "Game message", -1, nowMillis, "clog:" + itemName, 1));
        return true;
    }
    public synchronized boolean tracks(String counter) { return trackedCounters.contains(counter); }
    public synchronized String loginId() { return scope == null ? null : scope.loginEpoch(); }
    public synchronized Map<java.time.LocalDate,gg.feral.burrow.core.WeeklyProgress.Contribution> weeklyContributions() {
        Map<java.time.LocalDate,gg.feral.burrow.core.WeeklyProgress.Contribution> out = new TreeMap<>();
        weekly.forEach((w, a) -> out.put(w, new gg.feral.burrow.core.WeeklyProgress.Contribution(a.xp, a.kc, a.drops, a.est, a.counters, trackedCounters)));
        return out;
    }
    /** Live contribution for one week (empty but with this login's tracked counters if nothing happened that week). */
    public synchronized gg.feral.burrow.core.WeeklyProgress.Contribution weeklyContribution(java.time.LocalDate w) {
        WeekAcc a = weekly.get(w);
        return a == null ? new gg.feral.burrow.core.WeeklyProgress.Contribution(Map.of(), Map.of(), 0, 0, Map.of(), trackedCounters)
            : new gg.feral.burrow.core.WeeklyProgress.Contribution(a.xp, a.kc, a.drops, a.est, a.counters, trackedCounters);
    }
    private void pushActivity(Activity a) {
        Activity head = recent.peekFirst();
        if (a.kind() == ActivityKind.XP && head != null && head.kind() == ActivityKind.XP && head.key().equals(a.key())
                && a.atMillis() - head.atMillis() <= XP_MERGE_MILLIS) {
            recent.pollFirst();
            long sum = Math.addExact(head.amount(), a.amount());
            a = new Activity(ActivityKind.XP, "+" + String.format(Locale.US, "%,d", sum) + " " + a.key() + " XP", a.detail(), -1, a.atMillis(), a.key(), sum);
        }
        recent.addFirst(a);
        while (recent.size() > RECENT_CAP) recent.pollLast();
    }

    public synchronized void start(SessionScope s, SessionOrigin o, long nowMillis) {
        scope = Objects.requireNonNull(s); origin = Objects.requireNonNull(o); startMillis = nowMillis; recent.clear(); weekly.clear(); trackedCounters.clear();
        trackedCounters.add(gg.feral.burrow.core.VerifiedCounters.CHESTS); // R10: verified Barrows chest-count deltas
        paused = false; pausedEver = false; seenIds.clear(); feeds.clear(); everAvailable.clear();
        xp.clear(); kc.clear(); loot.clear(); drops.clear(); supplies.clear();
    }
    public synchronized void clear() { scope = null; origin = null; start0(); recent.clear(); weekly.clear(); trackedCounters.clear(); }
    private void start0() { paused = false; pausedEver = false; seenIds.clear(); feeds.clear(); everAvailable.clear(); xp.clear(); kc.clear(); loot.clear(); drops.clear(); supplies.clear(); }
    public synchronized SessionScope scope() { return scope; }
    public synchronized boolean paused() { return paused; }
    public synchronized boolean hasFeed(SessionFeed f) { return feeds.containsKey(f); }

    /** Record an event the plugin emitted for the active scope. Same acceptance rules as the panel. */
    public synchronized boolean record(SessionEvent e) { return record(e, System.currentTimeMillis()); }
    /** Same as {@link #record(SessionEvent)} with an explicit event time (tests, deterministic replays). */
    public synchronized boolean record(SessionEvent e, long nowMillis) {
        if (e == null || scope == null || !scope.equals(e.scope()) || origin != e.origin()) return false;
        if (e instanceof SessionStart || e instanceof SessionStop) return false; // lifecycle owned by start()/clear()
        if (e.eventId() == null || !seenIds.add(e.eventId())) return false;
        if (e instanceof SessionPause) { if (paused) return false; paused = true; pausedEver = true; return true; }
        if (e instanceof SessionResume) { if (!paused) return false; paused = false; return true; }
        if (paused) return false;
        if (e instanceof SessionFeedStatus f) { feeds.put(f.feed(), f.status()); if (f.status() == FeedStatus.AVAILABLE) everAvailable.add(f.feed());
            // R10: level-ups are counted only from the verified XP feed, so they become "tracked" when it is available.
            if (f.feed() == SessionFeed.XP && f.status() == FeedStatus.AVAILABLE) trackedCounters.add(gg.feral.burrow.core.VerifiedCounters.LEVEL_UPS); }
        else if (e instanceof SessionXpGain g) { if (g.delta() <= 0) return false; xp.merge(g.skill(), g.delta(), Math::addExact); feed(SessionFeed.XP); week(nowMillis).xp.merge(g.skill(), g.delta(), Math::addExact);
            pushActivity(new Activity(ActivityKind.XP, "+" + String.format(Locale.US, "%,d", g.delta()) + " " + g.skill() + " XP", "Verified XP feed", -1, nowMillis, g.skill(), g.delta())); }
        else if (e instanceof SessionBossKcGain g) { if (g.delta() <= 0) return false; kc.merge(g.boss(), g.delta(), Math::addExact); feed(SessionFeed.BOSS_KC); week(nowMillis).kc.merge(g.boss(), g.delta(), Math::addExact);
            if (gg.feral.burrow.core.VerifiedCounters.CHEST_ENCOUNTER.equals(g.boss())) recordCounter(gg.feral.burrow.core.VerifiedCounters.CHESTS, g.delta(), nowMillis);
            pushActivity(new Activity(ActivityKind.BOSS_KC, g.boss() + " KC +" + g.delta(), "Verified game-message KC", -1, nowMillis, g.boss(), g.delta())); }
        else if (e instanceof SessionLootEncounter g) { loot.merge(g.encounter(), 1L, Math::addExact); week(nowMillis).est++;
            pushActivity(new Activity(ActivityKind.LOOT_ESTIMATE, g.encounter() + " kill (estimate)", "Loot Tracker · not KC", -1, nowMillis, g.encounter(), 1)); }
        else if (e instanceof SessionDrop d) { add(drops, d.itemId(), d.itemName(), d.boss() == null || d.boss().isBlank() ? "Unassigned" : d.boss(), d.amount()); feed(SessionFeed.DROPS); week(nowMillis).drops += d.amount();
            pushActivity(new Activity(ActivityKind.DROP, d.itemName() + " ×" + String.format(Locale.US, "%,d", d.amount()),
                d.boss() == null || d.boss().isBlank() ? "Unassigned" : d.boss(), d.itemId(), nowMillis, d.itemId() + "", d.amount())); }
        else if (e instanceof SessionSupplyUsed u) { add(supplies, u.itemId(), u.itemName(), "Confirmed use", u.amount()); feed(SessionFeed.SUPPLIES); }
        return true;
    }
    private void feed(SessionFeed f) { feeds.put(f, FeedStatus.AVAILABLE); everAvailable.add(f); }
    private static void add(Map<String,SessionItem> m, int id, String name, String attribution, long amount) {
        if (id <= 0 || amount <= 0) return;
        String key = id + "/" + attribution;
        SessionItem prior = m.get(key);
        m.put(key, new SessionItem(id, name, attribution, prior == null ? amount : Math.addExact(prior.quantity(), amount)));
    }

    /** Full, idempotent rebuild for a fresh panel. {@code ids} must yield unique event ids. */
    public synchronized List<SessionEvent> replay(Supplier<String> ids) {
        if (scope == null) return List.of();
        List<SessionEvent> out = new ArrayList<>();
        out.add(new SessionStart(scope, ids.get(), origin));
        String proof = "Replayed from current-login ledger (no new observation)";
        xp.forEach((k, v) -> out.add(new SessionXpGain(scope, ids.get(), origin, k, v, proof)));
        kc.forEach((k, v) -> out.add(new SessionBossKcGain(scope, ids.get(), origin, k, v, proof)));
        loot.forEach((k, v) -> { for (long i = 0; i < v; i++) out.add(new SessionLootEncounter(scope, ids.get(), origin, k, proof)); });
        drops.values().forEach(d -> out.add(new SessionDrop(scope, ids.get(), origin, d.itemId(), d.itemName(), d.quantity(),
            "Unassigned".equals(d.attribution()) ? null : d.attribution(), proof)));
        supplies.values().forEach(d -> out.add(new SessionSupplyUsed(scope, ids.get(), origin, d.itemId(), d.itemName(), d.quantity(), proof)));
        // Current feed statuses go AFTER the rows: a row implies AVAILABLE in the panel, but the live state may
        // since have become UNAVAILABLE (e.g. XP re-syncing after a hop). Last write wins.
        feeds.forEach((f, st) -> out.add(new SessionFeedStatus(scope, ids.get(), origin, f, st)));
        if (paused) out.add(new SessionPause(scope, ids.get(), origin));
        return out;
    }

    /** Immutable completed-login card; caller decides whether the end is a CONFIRMED logout. */
    public synchronized CompletedSessionHistory.Summary completedSummary(long endMillis, String terminal) {
        if (scope == null) throw new IllegalStateException("no active login");
        List<String> flags = new ArrayList<>();
        if (pausedEver) flags.add("Paused at least once (hop/loading); gains during pauses were not counted");
        boolean xpObserved = everAvailable.contains(SessionFeed.XP);
        if (!xpObserved) flags.add("XP feed unavailable this login");
        if (!loot.isEmpty()) flags.add("Loot-confirmed encounters are Loot Tracker estimates, not KC");
        List<CompletedSessionHistory.Drop> d = new ArrayList<>();
        for (var i : drops.values()) d.add(new CompletedSessionHistory.Drop(i.itemId(), i.itemName(), "Unassigned".equals(i.attribution()) ? null : i.attribution(), i.quantity()));
        List<CompletedSessionHistory.Drop> s = new ArrayList<>();
        for (var i : supplies.values()) s.add(new CompletedSessionHistory.Drop(i.itemId(), i.itemName(), null, i.quantity()));
        boolean suppliesObserved = everAvailable.contains(SessionFeed.SUPPLIES);
        return new CompletedSessionHistory.Summary(CompletedSessionHistory.SCHEMA, scope.loginEpoch(), startMillis,
            Math.max(startMillis, endMillis), terminal,
            xpObserved ? CompletedSessionHistory.FeedState.OBSERVED : CompletedSessionHistory.FeedState.UNAVAILABLE,
            xpObserved ? xp : Map.of(),
            everAvailable.contains(SessionFeed.BOSS_KC) ? CompletedSessionHistory.FeedState.OBSERVED : CompletedSessionHistory.FeedState.UNAVAILABLE,
            kc, loot, d,
            suppliesObserved ? CompletedSessionHistory.FeedState.OBSERVED : CompletedSessionHistory.FeedState.UNAVAILABLE,
            suppliesObserved ? s : List.of(), flags);
    }
}
