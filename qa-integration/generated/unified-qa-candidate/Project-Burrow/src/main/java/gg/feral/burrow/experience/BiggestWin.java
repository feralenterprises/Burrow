package gg.feral.burrow.experience;

/** One real event from a completed login. Today's feeds give drops, verified kills and XP only. */
public record BiggestWin(String eventId, Kind kind, String title, String boss, long amount, String rate, long endedAtMillis,
                         boolean pinned, EvidenceStatus status) {
    public enum Kind { RARE_DROP, VERIFIED_KILLS, XP_GAIN }
}
