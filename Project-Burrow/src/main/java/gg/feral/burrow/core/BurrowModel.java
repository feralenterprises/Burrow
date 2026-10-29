package gg.feral.burrow.core;

import java.util.*;
import gg.feral.burrow.core.Outfitter.*;

/** All user-facing surfaces consume the same immutable plan object. */
public final class BurrowModel {
    public enum View { HOME, PATHFINDER, OUTFITTER, SESSIONS }
    private View view = View.HOME;
    private Scope scope = new Scope("DEMO", 0);
    private Snapshot bank;
    // Session-local revision high-water survives clearBank(); no bank contents are retained.
    private long highestBankRevision = -1;
    // A retired profile+epoch cannot be resurrected by A -> B -> A navigation.
    // The real RuneLite bridge still MUST authenticate a fresh login epoch.
    private final Set<Scope> retiredScopes = new HashSet<>();
    private String mode = "Recommended";
    private String boss = "Scurrius";
    private long nextPlanId = 1;
    private Plan plan;
    private int demoKills;
    private final List<String> journal = new ArrayList<>();
    public BurrowModel() { refresh(); }
    public View view() { return view; }
    public void show(View next) { view = Objects.requireNonNull(next); }
    public Scope scope() { return scope; }
    public Snapshot bank() { return bank; }
    /** Null when the selected boss has no reviewed sample template; never reuse another boss's plan. */
    public Plan plan() { return plan; }
    /** Both the Outfitter heading and recommendation projection derive from the same selected boss. */
    public String outfitterSubtitle() {
        return plan == null ? boss + " • no vetted loadout" : plan.template().boss() + " • sample melee loadout";
    }
    public int demoKills() { return demoKills; }
    public List<String> journal() { return List.copyOf(journal); }
    public String boss() { return boss; }
    public void chooseBoss(String value) {
        if (!Set.of("Scurrius","Vorkath","Barrows").contains(value)) throw new IllegalArgumentException("boss");
        if (!boss.equals(value)) { boss = value; refresh(); }
    }
    public void setMode(String next) {
        if (!Set.of("Recommended","Best in Bank").contains(next)) throw new IllegalArgumentException("mode");
        if (!next.equals(mode)) { mode = next; refresh(); }
    }
    public String mode() { return mode; }
    /** No automatic scanning, this method deliberately injects a labeled fixture. */
    public void loadDemoBank() {
        // Explicitly user-triggered DEMO fixture, never a live scan. Give repeated
        // fixture loads fresh revisions so they cannot reset the replay watermark.
        if (highestBankRevision == Long.MAX_VALUE)
            throw new IllegalStateException("revision exhausted");
        Snapshot fixture = Outfitter.sampleBank(scope);
        observeBank(new Snapshot(scope, Math.max(1, highestBankRevision + 1), fixture.items()));
    }
    public void clearBank() { bank = null; refresh(); }
    public void enterAccount(Scope next) {
        Objects.requireNonNull(next, "scope");
        if (scope.equals(next)) return; // Idempotent, cannot rewind revisions.
        if (retiredScopes.contains(next))
            throw new IllegalArgumentException("reused profile/login epoch");
        // Retire the old scope before switching. Keep only the identity tombstone,
        // not its inventory; a genuinely new epoch starts its own revision sequence.
        retiredScopes.add(scope);
        scope = next;
        bank = null;
        highestBankRevision = -1;
        refresh();
    }
    public void observeBank(Snapshot observation) {
        Objects.requireNonNull(observation, "observation");
        if (!scope.equals(observation.scope())) throw new IllegalArgumentException("foreign bank");
        // Compare against the accepted high-water even when no active bank remains.
        if (observation.revision() <= highestBankRevision)
            throw new IllegalArgumentException("non-increasing bank revision");
        bank = observation;
        highestBankRevision = observation.revision();
        refresh();
    }
    public void recordDemoKill() {
        demoKills++; journal.add(0,"DEMO kill #" + demoKills + " • no game event");
        if (journal.size() > 6) journal.remove(journal.size()-1);
    }
    public void resetDemo() { demoKills = 0; journal.clear(); }
    private void refresh() {
        // Scurrius is the only available *demo* template. Unsupported bosses have
        // no equipment claims, even when a demo bank fixture or mode is selected.
        plan = "Scurrius".equals(boss)
            ? Outfitter.resolve(nextPlanId++, scope, Outfitter.scurriusSample(), mode, bank)
            : null;
    }
    public static double cumulativeChance(double probability, int independentKills) {
        if (probability < 0 || probability > 1 || independentKills < 0) throw new IllegalArgumentException("odds");
        return 1 - Math.pow(1-probability, independentKills);
    }
}
