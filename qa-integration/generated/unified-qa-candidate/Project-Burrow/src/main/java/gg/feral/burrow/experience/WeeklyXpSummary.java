package gg.feral.burrow.experience;

import java.time.LocalDate;
import java.util.OptionalLong;

/** @param xp empty unless a real total exists. Counts only XP seen while Burrow ran */
public record WeeklyXpSummary(LocalDate weekStart, OptionalLong xp, int logins, EvidenceStatus status, String note) {}
