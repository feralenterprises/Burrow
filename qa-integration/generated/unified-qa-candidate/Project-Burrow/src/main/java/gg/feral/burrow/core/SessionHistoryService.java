package gg.feral.burrow.core;

import java.nio.file.Path;
import java.util.*;

/**
 * BQA-902 SESS-04: host-side owner of the per-account {@link CompletedSessionHistory}.
 * Binds to exactly one account key at a time; a different key closes the previous binding first, so account B
 * can never see or write account A's cards. IO failures never throw into the game thread: they become a
 * user-visible note and the live session is unaffected.
 */
public final class SessionHistoryService {
    private final Path dir;
    private String boundKey;
    private CompletedSessionHistory history;
    private WeeklyProgress weekly; // R08
    private String weeklyNote = "Sign in to load this account's weekly progress";
    private String note = "Sign in to load this account's history";

    public SessionHistoryService(Path dir) { this.dir = dir; }

    /** Bind to the authenticated account (opaque in-memory key) at the start of a verified login. */
    public synchronized void bind(String accountKey) {
        if (accountKey == null || accountKey.isBlank()) { unbind(); return; }
        if (accountKey.equals(boundKey) && history != null) return;
        unbind();
        boundKey = accountKey;
        if (dir == null) { note = "History storage unavailable"; return; }
        try { weekly = WeeklyProgress.open(dir, accountKey); weeklyNote = "Mon–Sun, local time · " + weekly.loadNote(); }
        catch (Exception e) { weekly = null; weeklyNote = "Weekly storage unavailable (" + e.getClass().getSimpleName() + ")"; }
        try {
            history = CompletedSessionHistory.open(dir, accountKey);
            note = switch (history.loadState()) {
                case EMPTY_NEW, LOADED -> "This account · newest first";
                case RECOVERED_FROM_BACKUP -> "Recovered from backup after a damaged file";
                case RECOVERED_EMPTY -> "History file damaged and set aside; new history started";
            };
        } catch (Exception e) {
            history = null; note = "History storage unavailable (" + e.getClass().getSimpleName() + ")";
        }
    }
    public synchronized void unbind() { boundKey = null; history = null; weekly = null; note = "Sign in to load this account's history";
        weeklyNote = "Sign in to load this account's weekly progress"; }
    /** R08: persist the running login's per-week contributions (replace, idempotent). Never throws. */
    public synchronized boolean saveWeekly(String accountKey, String loginId, Map<java.time.LocalDate,WeeklyProgress.Contribution> perWeek) {
        if (weekly == null || accountKey == null || !accountKey.equals(boundKey) || loginId == null) return false;
        try { weekly.save(loginId, perWeek); return true; }
        catch (Exception e) { weeklyNote = "Could not save weekly progress (" + e.getClass().getSimpleName() + ")"; return false; }
    }
    /** R08: totals for {@code week} with the live login's contribution substituted (or empty if not bound). */
    public synchronized Optional<WeeklyProgress.Totals> weeklyTotals(java.time.LocalDate week, String liveLoginId, WeeklyProgress.Contribution live) {
        return weekly == null ? Optional.empty() : Optional.of(weekly.totals(week, liveLoginId, live));
    }
    public synchronized String weeklyNote() { return weeklyNote; }

    /** Commit a completed login for the bound account only. */
    public synchronized boolean commit(String accountKey, CompletedSessionHistory.Summary summary) {
        if (history == null || accountKey == null || !accountKey.equals(boundKey)) return false;
        try { return history.commit(summary); }
        catch (Exception e) { note = "Could not save last login (" + e.getClass().getSimpleName() + "); history unchanged"; return false; }
    }
    public synchronized List<CompletedSessionHistory.Summary> sessions() { return history == null ? List.of() : history.newestFirst(); }
    public synchronized String note() { return note; }
    public synchronized boolean boundTo(String accountKey) { return accountKey != null && accountKey.equals(boundKey); }
}
