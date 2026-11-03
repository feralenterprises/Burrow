package gg.feral.burrow.experience;

import gg.feral.burrow.core.CompletedSessionHistory;
import gg.feral.burrow.core.CompletedSessionHistory.Summary;
import gg.feral.burrow.core.RareDropRowOdds;
import gg.feral.burrow.core.RareDropTable;
import gg.feral.burrow.core.SessionHistoryService;
import gg.feral.burrow.core.WeeklyProgress;
import java.time.LocalDate;
import java.util.*;

/** Read-only views over the existing weekly and completed-session stores. Stored records are never changed. */
public final class ProgressProjections {
    private ProgressProjections() {}

    /** A pin refers to a persisted event for one account. It is a preference and changes no source record. */
    public record Pin(String accountKey, String eventId) {}
    public record WinResult(Optional<BiggestWin> win, boolean pinCleared, EvidenceStatus status) {}

    /** Reads through the account-bound service. An unbound service gives UNAVAILABLE, never another account's data. */
    public static WeeklyXpSummary weeklyXp(SessionHistoryService service, String accountKey, LocalDate week, String liveLoginId, WeeklyProgress.Contribution live) {
        if (service == null || !service.boundTo(accountKey)) return new WeeklyXpSummary(week, OptionalLong.empty(), 0, EvidenceStatus.UNAVAILABLE, "Weekly data is not available");
        return weeklyXp(week, service.weeklyTotals(week, liveLoginId, live));
    }

    public static WeeklyXpSummary weeklyXp(LocalDate week, Optional<WeeklyProgress.Totals> totals) {
        if (totals == null || totals.isEmpty()) return new WeeklyXpSummary(week, OptionalLong.empty(), 0, EvidenceStatus.UNAVAILABLE, "Weekly data is not available");
        var t = totals.get();
        if (t.logins() == 0 || t.xp() == 0) return new WeeklyXpSummary(t.weekStart(), OptionalLong.empty(), t.logins(), EvidenceStatus.UNKNOWN, "No XP recorded this week");
        return new WeeklyXpSummary(t.weekStart(), OptionalLong.of(t.xp()), t.logins(), EvidenceStatus.OBSERVED, "XP seen while Burrow was running");
    }

    public static WinResult biggestWin(SessionHistoryService service, String accountKey, Pin pin) {
        if (service == null || !service.boundTo(accountKey)) return new WinResult(Optional.empty(), false, EvidenceStatus.UNAVAILABLE);
        return biggestWin(service.sessions(), accountKey, pin);
    }

    /** Best real event from the newest ten confirmed sessions, or a still-retained pin. Order is in {@link #rank()}. */
    public static WinResult biggestWin(List<Summary> sessions, String accountKey, Pin pin) {
        List<BiggestWin> events = events(sessions);
        boolean pinCleared = false;
        if (pin != null && Objects.equals(pin.accountKey(), accountKey)) {
            for (var e : events) if (e.eventId().equals(pin.eventId()))
                return new WinResult(Optional.of(pinned(e)), false, e.status());
            pinCleared = true;
        }
        var best = events.stream().min(rank());
        return new WinResult(best, pinCleared, best.map(BiggestWin::status).orElse(EvidenceStatus.UNKNOWN));
    }

    private static BiggestWin pinned(BiggestWin e) {
        return new BiggestWin(e.eventId(), e.kind(), e.title(), e.boss(), e.amount(), e.rate(), e.endedAtMillis(), true, e.status());
    }

    /** Every candidate event for a future pin picker. */
    public static List<BiggestWin> events(List<Summary> sessions) {
        List<BiggestWin> out = new ArrayList<>();
        if (sessions == null) return out;
        var recent = sessions.stream().filter(s -> CompletedSessionHistory.CONFIRMED_LOGOUT.equals(s.terminal()))
            .sorted(Comparator.comparingLong(Summary::endMillis).reversed()).limit(CompletedSessionHistory.CAPACITY).toList();
        for (var s : recent) {
            for (var drop : s.drops()) {
                rate(drop).ifPresent(r -> out.add(new BiggestWin(id(s, "drop", drop.boss() + ":" + drop.itemId()), BiggestWin.Kind.RARE_DROP, drop.itemName(), drop.boss(),
                    drop.quantity(), r.rateText(), s.endMillis(), false, EvidenceStatus.OBSERVED)));
            }
            if (s.kcFeed() == CompletedSessionHistory.FeedState.OBSERVED) for (var kc : s.verifiedKcByBoss().entrySet())
                out.add(new BiggestWin(id(s, "kills", kc.getKey()), BiggestWin.Kind.VERIFIED_KILLS, kc.getValue() + " " + kc.getKey() + " kills", kc.getKey(), kc.getValue(), "", s.endMillis(), false, EvidenceStatus.VERIFIED));
            if (s.xpFeed() == CompletedSessionHistory.FeedState.OBSERVED && s.totalXp() > 0)
                out.add(new BiggestWin(id(s, "xp", "total"), BiggestWin.Kind.XP_GAIN, "XP session", null, s.totalXp(), "", s.endMillis(), false, EvidenceStatus.OBSERVED));
        }
        return out;
    }

    private static String id(Summary s, String kind, String key) { return s.loginId() + ":" + kind + ":" + key; }

    /** Only a drop with a published rate on this boss can rank. Anything else has unknown rarity. */
    private static Optional<RareDropTable.Drop> rate(CompletedSessionHistory.Drop drop) {
        if (drop.boss() == null) return Optional.empty();
        for (var encounter : RareDropTable.all()) {
            if (!drop.boss().equals(encounter.boss()) && !drop.boss().equals(encounter.encounter())) continue;
            for (var d : encounter.drops()) if (RareDropTable.equivalentIds(d.itemId()).contains(drop.itemId())) return Optional.of(d);
        }
        return Optional.empty();
    }

    /** Drops first, rarest first. Then most verified kills, then most XP. Ties go to the newest, then the event ID. */
    private static Comparator<BiggestWin> rank() {
        return Comparator.comparingInt((BiggestWin w) -> w.kind().ordinal())
            .thenComparingDouble(ProgressProjections::rarity)
            .thenComparing(Comparator.comparingLong(BiggestWin::amount).reversed())
            .thenComparing(Comparator.comparingLong(BiggestWin::endedAtMillis).reversed())
            .thenComparing(BiggestWin::eventId);
    }

    private static double rarity(BiggestWin w) {
        if (w.kind() != BiggestWin.Kind.RARE_DROP) return 0;
        return RareDropTable.all().stream().flatMap(e -> e.drops().stream()).filter(d -> d.name().equals(w.title()) && d.rateText().equals(w.rate()))
            .mapToDouble(RareDropRowOdds::perKill).min().orElse(1);
    }
}
