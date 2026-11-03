package gg.feral.burrow.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * BUR-0032. Immutable, in-memory, account-bound Pathfinder goal state.
 *
 * This is NOT a persistence provider, RuneLite observer, UI/controller, quest guide,
 * or Sessions event producer. Only explicit player actions mutate goal state.
 * A host must supply its own VERIFIED, stable account identity; when verification
 * fails, it must not show or mutate any account-bound state.
 */
public final class PathfinderGoalState {
    private PathfinderGoalState(Snapshot snapshot) { this.snapshot = snapshot; }

    /** Host bridge contract, not an implementation of account authentication. */
    public interface AccountIdentity {
        /** Empty until a real account is verified. Never fall back to a display name. */
        Optional<String> verifiedStableAccountId();
    }

    /** Opaque stable identifier; unlike a login epoch, stays stable between sessions. */
    public record AccountKey(String stableId) {
        public AccountKey {
            if (stableId == null || stableId.isBlank()) throw new IllegalArgumentException("account id");
        }
    }

    public static AccountKey requireVerified(AccountIdentity host) {
        Objects.requireNonNull(host, "account identity host");
        return new AccountKey(Objects.requireNonNull(host.verifiedStableAccountId(), "identity result")
            .filter(s -> !s.isBlank())
            .orElseThrow(() -> new IllegalStateException("Account data couldn't be verified. Please restart RuneLite.")));
    }

    /** A hard prerequisite only when a reviewed source reference is supplied. */
    public enum PrerequisiteKind { QUEST, SKILL, ACCESS, ITEM, OTHER }
    public record Prerequisite(String id, String label, PrerequisiteKind kind, String verifiedSource) {
        public Prerequisite {
            requireNonblank(id, "prerequisite id");
            requireNonblank(label, "prerequisite label");
            Objects.requireNonNull(kind, "prerequisite kind");
            requireNonblank(verifiedSource, "reviewed prerequisite source");
        }
    }

    /** Rank is an owner/player preference, NOT a claim about mandatory progression. */
    public record GoalSpec(String id, String title, int rank, List<Prerequisite> prerequisites) {
        public GoalSpec {
            requireNonblank(id, "goal id");
            requireNonblank(title, "goal title");
            if (rank < 1) throw new IllegalArgumentException("rank must be >= 1");
            prerequisites = List.copyOf(Objects.requireNonNull(prerequisites, "prerequisites"));
            Set<String> ids = new HashSet<>();
            for (Prerequisite p : prerequisites) {
                if (!ids.add(p.id())) throw new IllegalArgumentException("duplicate prerequisite: " + p.id());
            }
        }
    }

    /** Player-entered progress; never automatically inferred from a partial bank. */
    public record Progress(long amount, long target, String unit) {
        public Progress {
            if (amount < 0 || target < 0) throw new IllegalArgumentException("negative progress");
            Objects.requireNonNull(unit, "progress unit");
            if (target != 0 && unit.isBlank()) throw new IllegalArgumentException("progress unit");
        }
        public static Progress unspecified() { return new Progress(0, 0, ""); }
    }

    public record SavedGoal(GoalSpec spec, Progress progress, boolean ignored, boolean completed) {
        public SavedGoal {
            Objects.requireNonNull(spec, "goal spec");
            Objects.requireNonNull(progress, "progress");
        }
    }

    /** Observation is advisory and cannot complete, ignore, skip, or activate a goal. */
    public enum PrerequisiteStatus { VERIFIED_MET, VERIFIED_UNMET, UNKNOWN }
    public record PrerequisiteObservation(PrerequisiteStatus status, String evidence) {
        public PrerequisiteObservation {
            Objects.requireNonNull(status, "status");
            if (status != PrerequisiteStatus.UNKNOWN) requireNonblank(evidence, "observation evidence");
            evidence = evidence == null ? "" : evidence;
        }
        public static PrerequisiteObservation unknown() {
            return new PrerequisiteObservation(PrerequisiteStatus.UNKNOWN, "");
        }
    }
    public record PrerequisiteView(Prerequisite requirement, PrerequisiteStatus status,
                                   boolean onIgnoreList) {
        public String label() {
            return requirement.label() + (onIgnoreList ? " required (on your Ignore list)" : " required");
        }
    }
    public record GoalView(SavedGoal goal, boolean active, List<PrerequisiteView> prerequisites,
                           boolean someDataUnavailable) {
        public GoalView { prerequisites = List.copyOf(prerequisites); }
        public String dataNotice() { return someDataUnavailable ? "Some data unavailable" : ""; }
    }

    /** Schema-neutral, immutable snapshot for a FUTURE persistence adapter. No IO here. */
    public record Snapshot(AccountKey account, long revision, List<SavedGoal> savedGoals,
                           List<String> suggestionOrder, String activeGoalId) {
        public Snapshot {
            Objects.requireNonNull(account, "account");
            if (revision < 0) throw new IllegalArgumentException("revision");
            savedGoals = List.copyOf(Objects.requireNonNull(savedGoals, "saved goals"));
            suggestionOrder = List.copyOf(Objects.requireNonNull(suggestionOrder, "suggestion order"));
            Set<String> ids = new HashSet<>();
            for (SavedGoal g : savedGoals) {
                if (!ids.add(g.spec().id())) throw new IllegalArgumentException("duplicate saved goal");
            }
            if (suggestionOrder.size() != ids.size() ||
                !new HashSet<>(suggestionOrder).equals(ids))
                throw new IllegalArgumentException("order must contain every saved goal exactly once");
            if (new HashSet<>(suggestionOrder).size() != suggestionOrder.size())
                throw new IllegalArgumentException("duplicate queue entry");
            if (activeGoalId != null) {
                SavedGoal active = null;
                for (SavedGoal g : savedGoals) if (g.spec().id().equals(activeGoalId)) active = g;
                if (active == null || active.ignored() || active.completed())
                    throw new IllegalArgumentException("invalid active goal");
            }
        }
    }

    private final Snapshot snapshot;

    public static PathfinderGoalState empty(AccountKey account) {
        return new PathfinderGoalState(new Snapshot(account, 0, List.of(), List.of(), null));
    }

    /** Starts with data-provider supplied rank; ties retain input order. */
    public static PathfinderGoalState start(AccountKey account, List<GoalSpec> goals) {
        Objects.requireNonNull(goals, "goals");
        List<GoalSpec> ranked = new ArrayList<>(goals);
        ranked.sort(Comparator.comparingInt(GoalSpec::rank));
        List<SavedGoal> entries = new ArrayList<>();
        List<String> order = new ArrayList<>();
        for (GoalSpec g : ranked) {
            entries.add(new SavedGoal(g, Progress.unspecified(), false, false));
            order.add(g.id());
        }
        return new PathfinderGoalState(new Snapshot(account, 0, entries, order, null));
    }

    /** Rehydrate only after trusted host identity matches. Does NOT read any storage. */
    public static PathfinderGoalState fromProposedSnapshot(AccountKey verifiedAccount, Snapshot saved) {
        Objects.requireNonNull(verifiedAccount, "verified account");
        Objects.requireNonNull(saved, "saved snapshot");
        if (!verifiedAccount.equals(saved.account())) throw new SecurityException("foreign account goal snapshot");
        return new PathfinderGoalState(saved);
    }

    public Snapshot snapshotFor(AccountKey verifiedAccount) {
        guard(verifiedAccount);
        return snapshot;
    }

    public Optional<SavedGoal> findFor(AccountKey verifiedAccount, String goalId) {
        guard(verifiedAccount);
        return snapshot.savedGoals().stream().filter(g -> g.spec().id().equals(goalId)).findFirst();
    }

    /** An ignored goal is still searchable; no automatic restore or assignment. */
    public GoalView viewFor(AccountKey verifiedAccount, String goalId,
                            Map<String, PrerequisiteObservation> observations,
                            boolean catalogComplete) {
        guard(verifiedAccount);
        Objects.requireNonNull(observations, "observations");
        SavedGoal goal = existing(goalId);
        List<PrerequisiteView> shown = new ArrayList<>();
        boolean unknown = !catalogComplete;
        Map<String, SavedGoal> lookup = byId(snapshot.savedGoals());
        for (Prerequisite p : goal.spec().prerequisites()) {
            PrerequisiteObservation observed = observations.getOrDefault(p.id(), PrerequisiteObservation.unknown());
            if (observed == null) observed = PrerequisiteObservation.unknown();
            if (observed.status() == PrerequisiteStatus.UNKNOWN) unknown = true;
            SavedGoal prerequisiteGoal = lookup.get(p.id());
            shown.add(new PrerequisiteView(p, observed.status(),
                prerequisiteGoal != null && prerequisiteGoal.ignored()));
        }
        return new GoalView(goal, goalId.equals(snapshot.activeGoalId()), shown, unknown);
    }

    /** Ranked/rotated advisory suggestions. No automatic goal-selection side effect. */
    public List<SavedGoal> suggestionsFor(AccountKey verifiedAccount) {
        guard(verifiedAccount);
        Map<String, SavedGoal> byId = byId(snapshot.savedGoals());
        List<SavedGoal> result = new ArrayList<>();
        for (String id : snapshot.suggestionOrder()) {
            SavedGoal g = byId.get(id);
            if (!g.ignored() && !g.completed() && !id.equals(snapshot.activeGoalId())) result.add(g);
        }
        return List.copyOf(result);
    }

    /** Adds a newly user-requested goal at the end without reordering saved preferences. */
    public PathfinderGoalState saveGoal(AccountKey verifiedAccount, GoalSpec goal) {
        guard(verifiedAccount);
        Objects.requireNonNull(goal, "goal");
        if (findFor(verifiedAccount, goal.id()).isPresent())
            throw new IllegalArgumentException("saved goal exists; refusing silent overwrite");
        List<SavedGoal> entries = new ArrayList<>(snapshot.savedGoals());
        entries.add(new SavedGoal(goal, Progress.unspecified(), false, false));
        List<String> order = new ArrayList<>(snapshot.suggestionOrder());
        order.add(goal.id());
        return next(entries, order, snapshot.activeGoalId());
    }

    /** Explicit player choice; previous goal stays saved with progress intact. */
    public PathfinderGoalState choose(AccountKey verifiedAccount, String goalId) {
        guard(verifiedAccount);
        SavedGoal goal = existing(goalId);
        if (goal.ignored() || goal.completed())
            throw new IllegalStateException("Restore or reopen this goal explicitly first");
        if (goalId.equals(snapshot.activeGoalId())) return this;
        return next(snapshot.savedGoals(), snapshot.suggestionOrder(), goalId);
    }

    /** Skip moves goal to queue tail, leaves progress unchanged; never auto-selects next. */
    public PathfinderGoalState skip(AccountKey verifiedAccount, String goalId) {
        guard(verifiedAccount);
        SavedGoal goal = existing(goalId);
        if (goal.ignored() || goal.completed()) throw new IllegalStateException("goal not skippable");
        List<String> order = new ArrayList<>(snapshot.suggestionOrder());
        order.remove(goalId);
        order.add(goalId);
        String active = goalId.equals(snapshot.activeGoalId()) ? null : snapshot.activeGoalId();
        if (order.equals(snapshot.suggestionOrder()) && Objects.equals(active, snapshot.activeGoalId())) return this;
        return next(snapshot.savedGoals(), order, active);
    }

    /** Ignore removes goal from AUTOMATIC suggestions only; never deletes it. */
    public PathfinderGoalState ignore(AccountKey verifiedAccount, String goalId) {
        guard(verifiedAccount);
        SavedGoal goal = existing(goalId);
        if (goal.ignored()) return this;
        return replace(goalId, new SavedGoal(goal.spec(), goal.progress(), true, goal.completed()),
            goalId.equals(snapshot.activeGoalId()) ? null : snapshot.activeGoalId());
    }

    /** Manual restore. An ignored goal is NEVER automatically restored. */
    public PathfinderGoalState restore(AccountKey verifiedAccount, String goalId) {
        guard(verifiedAccount);
        SavedGoal goal = existing(goalId);
        if (!goal.ignored()) return this;
        return replace(goalId, new SavedGoal(goal.spec(), goal.progress(), false, goal.completed()),
            snapshot.activeGoalId());
    }

    /** Player-confirmed completion, NEVER derived from inventory/quest observations. */
    public PathfinderGoalState confirmCompleted(AccountKey verifiedAccount, String goalId) {
        guard(verifiedAccount);
        SavedGoal goal = existing(goalId);
        if (goal.completed()) return this;
        return replace(goalId, new SavedGoal(goal.spec(), goal.progress(), goal.ignored(), true),
            goalId.equals(snapshot.activeGoalId()) ? null : snapshot.activeGoalId());
    }

    public PathfinderGoalState reopen(AccountKey verifiedAccount, String goalId) {
        guard(verifiedAccount);
        SavedGoal goal = existing(goalId);
        if (!goal.completed()) return this;
        return replace(goalId, new SavedGoal(goal.spec(), goal.progress(), goal.ignored(), false),
            snapshot.activeGoalId());
    }

    /** Explicit progress input never completes a goal, even when target is reached. */
    public PathfinderGoalState updateProgress(AccountKey verifiedAccount, String goalId, Progress progress) {
        guard(verifiedAccount);
        SavedGoal goal = existing(goalId);
        Objects.requireNonNull(progress, "progress");
        if (goal.progress().equals(progress)) return this;
        return replace(goalId, new SavedGoal(goal.spec(), progress, goal.ignored(), goal.completed()),
            snapshot.activeGoalId());
    }

    /** Explicit re-rank; this action intentionally resets the rotation to updated rank order. */
    public PathfinderGoalState changeRank(AccountKey verifiedAccount, String goalId, int newRank) {
        guard(verifiedAccount);
        SavedGoal g = existing(goalId);
        GoalSpec replacement = new GoalSpec(g.spec().id(), g.spec().title(), newRank, g.spec().prerequisites());
        if (g.spec().equals(replacement)) return this;
        List<SavedGoal> entries = new ArrayList<>(snapshot.savedGoals());
        int index = indexOf(goalId, entries);
        entries.set(index, new SavedGoal(replacement, g.progress(), g.ignored(), g.completed()));
        Map<String, Integer> earlier = new HashMap<>();
        for (int i = 0; i < snapshot.suggestionOrder().size(); i++) earlier.put(snapshot.suggestionOrder().get(i), i);
        List<String> order = new ArrayList<>(snapshot.suggestionOrder());
        Map<String, SavedGoal> byId = byId(entries);
        order.sort(Comparator.comparingInt((String id) -> byId.get(id).spec().rank())
            .thenComparingInt(earlier::get));
        return next(entries, order, snapshot.activeGoalId());
    }

    private PathfinderGoalState replace(String id, SavedGoal replacement, String active) {
        List<SavedGoal> entries = new ArrayList<>(snapshot.savedGoals());
        entries.set(indexOf(id, entries), replacement);
        return next(entries, snapshot.suggestionOrder(), active);
    }

    private PathfinderGoalState next(List<SavedGoal> entries, List<String> order, String active) {
        if (snapshot.revision() == Long.MAX_VALUE) throw new IllegalStateException("revision overflow");
        return new PathfinderGoalState(new Snapshot(snapshot.account(), snapshot.revision() + 1,
            entries, order, active));
    }

    private SavedGoal existing(String goalId) {
        if (goalId == null || goalId.isBlank()) throw new IllegalArgumentException("goal id");
        return snapshot.savedGoals().stream().filter(g -> g.spec().id().equals(goalId))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("unknown saved goal: " + goalId));
    }
    private void guard(AccountKey verifiedAccount) {
        if (!snapshot.account().equals(verifiedAccount)) throw new SecurityException("foreign account goal access");
    }
    private static int indexOf(String id, List<SavedGoal> list) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).spec().id().equals(id)) return i;
        throw new IllegalArgumentException("unknown saved goal: " + id);
    }
    private static Map<String, SavedGoal> byId(List<SavedGoal> entries) {
        Map<String, SavedGoal> result = new LinkedHashMap<>();
        for (SavedGoal g : entries) result.put(g.spec().id(), g);
        return result;
    }
    private static void requireNonblank(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label);
    }
}
