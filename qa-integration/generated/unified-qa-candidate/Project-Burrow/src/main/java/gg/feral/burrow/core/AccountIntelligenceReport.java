package gg.feral.burrow.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Honest first-run / account-intelligence projection.
 * Missing evidence stays missing — never coerced to zero/complete/owned.
 * Partial native pages never prove a full log.
 */
public final class AccountIntelligenceReport {
    public enum Overall {
        SIGNED_OUT,
        UNAUTHENTICATED,
        FIRST_RUN_INCOMPLETE,
        PARTIALLY_CAPTURED,
        BASELINE_SAVED_CAPTURE_OPEN
    }

    public record SectionRow(
        PathfinderGuidedCapture.Section section,
        PathfinderGuidedCapture.Status status,
        int witnessedPages,
        int playerReviewedPages,
        long lastSeenAtMillis,
        boolean staleReminderDue,
        int witnessedEvidenceRows,
        boolean provesComplete,
        String coverageLabel,
        String instruction
    ) {}

    public record Snapshot(
        long accountHash,
        Overall overall,
        boolean initialSkillsQuestsSaved,
        boolean latestSkillsQuestsSaved,
        long initialObservedAtMillis,
        long latestObservedAtMillis,
        boolean skillsQuestsStaleReminderDue,
        boolean anyCaptureStaleReminderDue,
        boolean bankStaleReminderDue,
        boolean bankNeverScanned,
        List<SectionRow> sections,
        Optional<PathfinderGuidedCapture.Section> nextUnobserved,
        String headline,
        String detail
    ) {
        public Snapshot {
            sections = List.copyOf(sections);
            Objects.requireNonNull(overall);
            Objects.requireNonNull(headline);
            Objects.requireNonNull(detail);
            Objects.requireNonNull(nextUnobserved);
        }
    }

    private AccountIntelligenceReport() {}

    public static Snapshot build(
        long accountHash,
        long nowMillis,
        Optional<PathfinderAccountArchive.Baseline> initial,
        Optional<PathfinderAccountArchive.Baseline> latest,
        Optional<PathfinderAccountArchive.BankMemory> bank,
        List<PathfinderGuidedCapture.ChecklistStep> checklist
    ) {
        if (accountHash == -1L) {
            return new Snapshot(
                -1L, Overall.SIGNED_OUT,
                false, false, 0L, 0L, false, false, false, true,
                List.of(), Optional.empty(),
                "ACCOUNT CAPTURE · SIGN IN",
                "Sign in so TPT can gather per-account evidence. Unknown values stay unknown."
            );
        }
        List<PathfinderGuidedCapture.ChecklistStep> steps =
            checklist == null ? List.of() : checklist;
        List<SectionRow> rows = new ArrayList<>();
        boolean anyNative = false;
        boolean anyReview = false;
        boolean anyCaptureStale = false;
        Optional<PathfinderGuidedCapture.Section> next = Optional.empty();
        for (PathfinderGuidedCapture.Section section : PathfinderGuidedCapture.Section.values()) {
            PathfinderGuidedCapture.ChecklistStep step = steps.stream()
                .filter(s -> s.section() == section).findFirst().orElse(null);
            PathfinderGuidedCapture.Status status = step == null
                ? PathfinderGuidedCapture.Status.UNOBSERVED : step.status();
            int nativePages = step == null ? 0 : step.witnessedPages();
            int reviews = step == null ? 0 : step.playerReviewedPages();
            long lastSeen = step == null ? 0L : step.lastSeenAtMillis();
            boolean stale = step != null && step.staleReminderDue(nowMillis);
            int evidenceRows = step == null ? 0 : step.witnessedEvidenceRows();
            boolean complete = step != null && step.provesComplete();
            if (status == PathfinderGuidedCapture.Status.PARTIALLY_WITNESSED) anyNative = true;
            if (status == PathfinderGuidedCapture.Status.REVIEWED_BY_PLAYER) anyReview = true;
            if (stale) anyCaptureStale = true;
            if (status == PathfinderGuidedCapture.Status.UNOBSERVED && next.isEmpty()) {
                next = Optional.of(section);
            }
            String coverage = switch (status) {
                case UNOBSERVED -> "NO EVIDENCE (unknown, not zero)";
                case REVIEWED_BY_PLAYER -> "PLAYER REVIEW ONLY · " + reviews
                    + " receipt(s) · never machine-complete";
                case PARTIALLY_WITNESSED -> "PARTIAL NATIVE · " + nativePages + " page(s), "
                    + evidenceRows + " witnessed row(s)/item(s) · complete=" + complete;
            };
            rows.add(new SectionRow(
                section, status, nativePages, reviews, lastSeen, stale,
                evidenceRows, complete, coverage, section.guidance()));
        }
        boolean initialSaved = initial != null && initial.isPresent();
        boolean latestSaved = latest != null && latest.isPresent();
        long initialAt = initialSaved ? initial.get().observedAtMillis() : 0L;
        long latestAt = latestSaved ? latest.get().observedAtMillis() : 0L;
        boolean skillsStale = (latestSaved && latest.get().reminderDue(nowMillis))
            || (initialSaved && !latestSaved && initial.get().reminderDue(nowMillis));
        boolean bankNever = bank == null || bank.isEmpty();
        boolean bankStale = !bankNever && bank.get().reminderDue(nowMillis);

        Overall overall;
        if (!initialSaved) {
            overall = Overall.FIRST_RUN_INCOMPLETE;
        } else if (anyNative || anyReview) {
            overall = next.isEmpty() && initialSaved
                ? Overall.BASELINE_SAVED_CAPTURE_OPEN
                : Overall.PARTIALLY_CAPTURED;
        } else {
            overall = Overall.FIRST_RUN_INCOMPLETE;
        }

        String headline = switch (overall) {
            case SIGNED_OUT, UNAUTHENTICATED -> "ACCOUNT CAPTURE · SIGN IN";
            case FIRST_RUN_INCOMPLETE -> "ACCOUNT CAPTURE · FIRST RUN OPEN";
            case PARTIALLY_CAPTURED -> "ACCOUNT CAPTURE · PARTIAL (NOT COMPLETE)";
            case BASELINE_SAVED_CAPTURE_OPEN -> "ACCOUNT CAPTURE · BASELINE SAVED · LOGS OPEN";
        };
        StringBuilder detail = new StringBuilder();
        detail.append("Initial skills/quests: ").append(initialSaved ? "SAVED" : "NOT VERIFIED");
        if (initialSaved) detail.append(" @ ").append(initialAt).append(" ms");
        detail.append(". Latest skills/quests: ").append(latestSaved ? "SAVED" : "NOT VERIFIED");
        if (latestSaved) detail.append(" @ ").append(latestAt).append(" ms");
        detail.append(". Missing screens stay unknown — never treated as zero.");
        if (skillsStale) detail.append(" Skills/quests evidence is 7+ days old; refresh the quest survey.");
        if (anyCaptureStale) detail.append(" One or more capture sections are 7+ days old.");
        if (next.isPresent()) {
            detail.append(" Next unobserved: ").append(next.get().name().replace('_', ' ')).append('.');
        }
        return new Snapshot(
            accountHash, overall,
            initialSaved, latestSaved, initialAt, latestAt,
            skillsStale, anyCaptureStale, bankStale, bankNever,
            rows, next, headline, detail.toString()
        );
    }
}
