package gg.feral.burrow.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import gg.feral.burrow.core.PathfinderGoalState.*;

/** Standalone Java-only offline checks; NO real bank, account, RuneLite or UI proof. */
public final class PathfinderGoalStateTest {
    private static int assertions;
    private static void ok(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static void throwsType(Class<? extends Throwable> expected, Runnable command, String message) {
        assertions++;
        try { command.run(); }
        catch (Throwable t) {
            if (expected.isInstance(t)) return;
            throw new AssertionError(message + ": wrong exception " + t, t);
        }
        throw new AssertionError(message + ": should reject");
    }
    private static GoalSpec g(String id, String name, int rank, Prerequisite... prerequisites) {
        return new GoalSpec(id, name, rank, List.of(prerequisites));
    }
    private static List<String> ids(List<SavedGoal> suggestions) {
        return suggestions.stream().map(s -> s.spec().id()).toList();
    }
    public static void main(String[] args) {
        AccountKey a = PathfinderGoalState.requireVerified(() -> Optional.of("verified-account-a"));
        AccountKey sameOnNextLogin = PathfinderGoalState.requireVerified(() -> Optional.of("verified-account-a"));
        AccountKey b = PathfinderGoalState.requireVerified(() -> Optional.of("verified-account-b"));
        ok(a.equals(sameOnNextLogin), "stable account identity across logins");
        throwsType(IllegalStateException.class,
            () -> PathfinderGoalState.requireVerified(Optional::empty), "unverified account must fail closed");
        throwsType(IllegalArgumentException.class, () -> new AccountKey(" "), "blank account id rejected");

        // These three source names are TEST FIXTURES, not approved production prerequisites.
        Prerequisite sote = new Prerequisite("sote", "Song of the Elves", PrerequisiteKind.QUEST,
            "FIXTURE_ONLY: reviewed-source-bridge-to-be-implemented");
        GoalSpec bowfa = g("bowfa", "Bowfa", 1, sote);
        GoalSpec vorkath = g("vorkath", "Vorkath", 2);
        GoalSpec song = g("sote", "Song of the Elves", 3);
        PathfinderGoalState state = PathfinderGoalState.start(a, List.of(song, vorkath, bowfa));
        ok(ids(state.suggestionsFor(a)).equals(List.of("bowfa", "vorkath", "sote")), "initial suggestions ranked");
        ok(state.snapshotFor(a).activeGoalId() == null, "no automatic active goal");
        ok(state.snapshotFor(a).revision() == 0, "initial revision zero");
        throwsType(SecurityException.class, () -> state.snapshotFor(b), "foreign snapshot not exposed");
        throwsType(SecurityException.class, () -> state.suggestionsFor(b), "foreign suggestions not exposed");
        throwsType(SecurityException.class, () -> state.findFor(b, "bowfa"), "foreign search not exposed");
        throwsType(SecurityException.class, () -> PathfinderGoalState.fromProposedSnapshot(b, state.snapshotFor(a)),
            "foreign persisted snapshot refused");

        PathfinderGoalState selected = state.choose(a, "bowfa");
        ok("bowfa".equals(selected.snapshotFor(a).activeGoalId()), "player selects active goal");
        ok(ids(selected.suggestionsFor(a)).equals(List.of("vorkath", "sote")), "active goal excluded from automatic suggestions");
        PathfinderGoalState progress = selected.updateProgress(a, "bowfa", new Progress(250, 250, "CG kills"));
        ok(!progress.findFor(a, "bowfa").orElseThrow().completed(), "progress at target cannot auto-complete");
        ok("bowfa".equals(progress.snapshotFor(a).activeGoalId()), "progress cannot switch goal");
        PathfinderGoalState skipped = progress.skip(a, "bowfa");
        ok(skipped.snapshotFor(a).activeGoalId() == null, "Skip clears focus without assigning another goal");
        ok(ids(skipped.suggestionsFor(a)).equals(List.of("vorkath", "sote", "bowfa")), "Skip rotates to tail");
        ok(skipped.findFor(a, "bowfa").orElseThrow().progress().amount() == 250,
            "Skip preserves existing progress");
        ok(!skipped.findFor(a, "bowfa").orElseThrow().completed(), "Skip does not complete goal");
        PathfinderGoalState finishedOther = skipped.choose(a, "vorkath").confirmCompleted(a, "vorkath");
        ok(finishedOther.snapshotFor(a).activeGoalId() == null,
            "completion does not automatically choose another goal");
        ok(ids(finishedOther.suggestionsFor(a)).equals(List.of("sote", "bowfa")),
            "skipped Bowfa remains available after completing other goal");
        ok(finishedOther.findFor(a, "vorkath").orElseThrow().completed(), "explicit completion recorded");

        PathfinderGoalState ignored = finishedOther.ignore(a, "sote");
        ok(ids(ignored.suggestionsFor(a)).equals(List.of("bowfa")), "Ignore filters auto recommendations");
        ok(ignored.findFor(a, "sote").orElseThrow().ignored(), "Ignore retains searchable saved goal");
        var ignoredPre = ignored.viewFor(a, "bowfa", Map.of(), true);
        ok(ignoredPre.prerequisites().size() == 1, "known prerequisite displayed");
        ok(ignoredPre.prerequisites().get(0).onIgnoreList(), "ignored prerequisite stays visible");
        ok(ignoredPre.prerequisites().get(0).label().contains("on your Ignore list"),
            "ignored prerequisite explicitly labeled");
        ok(ignoredPre.someDataUnavailable(), "missing account observation must be unknown");
        ok(ignoredPre.dataNotice().equals("Some data unavailable"), "data warning text");
        ok(ignored.viewFor(a, "sote", Map.of(), true).goal().ignored(),
            "direct goal search does not auto-restore");
        ok(ignored.findFor(a, "sote").orElseThrow().ignored(), "search kept Ignore state");
        PathfinderGoalState known = ignored;
        var observed = known.viewFor(a, "bowfa", Map.of("sote",
            new PrerequisiteObservation(PrerequisiteStatus.VERIFIED_MET, "fixture:quest-state-read")), true);
        ok(!observed.someDataUnavailable(), "supported known observation shown as known");
        ok(!known.findFor(a, "bowfa").orElseThrow().completed(),
            "verified prerequisite does not complete goal");
        ok(known.findFor(a, "sote").orElseThrow().ignored(),
            "verified observation never unignores prerequisite");
        ok(known.viewFor(a, "bowfa", Map.of("sote", PrerequisiteObservation.unknown()), false)
            .someDataUnavailable(), "incomplete catalog flags uncertainty");
        throwsType(IllegalArgumentException.class,
            () -> new PrerequisiteObservation(PrerequisiteStatus.VERIFIED_MET, ""),
            "confirmed observation must have provenance");
        throwsType(IllegalArgumentException.class,
            () -> new Prerequisite("missing", "Undocumented", PrerequisiteKind.OTHER, " "),
            "hard prerequisites require reviewed-source handle");

        PathfinderGoalState restored = ignored.restore(a, "sote");
        ok(!restored.findFor(a, "sote").orElseThrow().ignored(), "manual restore only");
        ok(ids(restored.suggestionsFor(a)).equals(List.of("sote", "bowfa")),
            "manually restored goal reappears at prior position");
        throwsType(IllegalStateException.class, () -> ignored.choose(a, "sote"),
            "cannot choose ignored goal without restore");
        throwsType(IllegalStateException.class, () -> restored.choose(a, "vorkath"),
            "completed goal requires explicit reopen");
        PathfinderGoalState reopened = restored.reopen(a, "vorkath");
        ok(!reopened.findFor(a, "vorkath").orElseThrow().completed(), "manual reopen successful");
        ok(reopened.findFor(a, "bowfa").orElseThrow().progress().amount() == 250,
            "reopening other goal does not erase saved progress");
        ok(reopened.snapshotFor(a).revision() > state.snapshotFor(a).revision(), "state revisions advance");
        ok(reopened == reopened.restore(a, "sote"), "no-op restoration keeps same instance");
        ok(reopened == reopened.reopen(a, "vorkath"), "no-op reopen keeps same instance");

        PathfinderGoalState ranked = reopened.changeRank(a, "bowfa", 4);
        ok(ids(ranked.suggestionsFor(a)).equals(List.of("vorkath", "sote", "bowfa")),
            "explicit ranking resets rotated recommendation order");
        ok(ranked.findFor(a, "bowfa").orElseThrow().progress().amount() == 250,
            "rank change retains progress");
        PathfinderGoalState imported = PathfinderGoalState.fromProposedSnapshot(sameOnNextLogin, ranked.snapshotFor(a));
        ok(imported.snapshotFor(a).equals(ranked.snapshotFor(a)), "snapshot roundtrip for same account");
        PathfinderGoalState newAccount = PathfinderGoalState.empty(b);
        ok(newAccount.suggestionsFor(b).isEmpty(), "new account never inherits old goals");

        PathfinderGoalState extra = ranked.saveGoal(a, g("defender", "Dragon defender", 5));
        ok(extra.findFor(a, "defender").isPresent(), "new goal saves explicitly");
        ok(extra.findFor(a, "bowfa").orElseThrow().progress().amount() == 250,
            "saving new goal cannot silently change old goal");
        throwsType(IllegalArgumentException.class, () -> extra.saveGoal(a, bowfa),
            "existing goal refuses silent overwrite");
        throwsType(IllegalArgumentException.class,
            () -> new Snapshot(a, 0, List.of(new SavedGoal(bowfa, Progress.unspecified(), false, false)),
                List.of("other"), null), "corrupt suggestion order rejected");
        throwsType(IllegalArgumentException.class,
            () -> new Snapshot(a, 0, List.of(new SavedGoal(bowfa, Progress.unspecified(), true, false)),
                List.of("bowfa"), "bowfa"), "ignored active goal rejected");
        throwsType(SecurityException.class, () -> extra.choose(b, "bowfa"), "foreign actions rejected");
        throwsType(IllegalArgumentException.class, () -> extra.skip(a, "missing"), "unknown goal not created");
        throwsType(IllegalArgumentException.class, () -> new Progress(-1, 250, "kills"),
            "negative progress rejected");
        ok(extra.findFor(a, "bowfa").orElseThrow().spec().prerequisites().equals(List.of(sote)),
            "known prerequisite preserved verbatim");
        System.out.println("BUR-0032 OFFLINE FOCUSED PASS: " + assertions +
            " assertions; synthetic pure-Java fixture only; not JDK17 runtime/native/Gradle verification");
    }
}
