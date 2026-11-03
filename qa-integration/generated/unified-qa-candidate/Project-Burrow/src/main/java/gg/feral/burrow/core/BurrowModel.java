package gg.feral.burrow.core;

import java.util.*;
import gg.feral.burrow.core.Outfitter.*;
import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;

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
    private Tier tier = Tier.RECOMMENDED;
    private String boss = "Scurrius";
    private String method = VerifiedOutfitterCatalog.profiles("Scurrius").get(0).method();
    private long nextPlanId = 1;
    private Plan plan;
    public record Selection(String boss, Plan plan) {}
    private volatile Selection bankSelection;
    public Selection bankSelection() { return bankSelection; }
    public BurrowModel() { refresh(); }
    public View view() { return view; }
    public void show(View next) { view = Objects.requireNonNull(next); }
    public Scope scope() { return scope; }
    public Snapshot bank() { return bank; }
    /** Null when the selected boss has no reviewed sample template; never reuse another boss's plan. */
    public Plan plan() { return plan; }
    /** Both the Outfitter heading and recommendation projection derive from the same selected boss. */
    public String outfitterSubtitle() { return outfitterSubtitle(false); }
    /** Build 19: label reflects the actual display mode instead of a hard-coded "Wiki". */
    public String outfitterSubtitle(boolean bestBank) {
        return plan == null ? boss + " • " + tier.label() + " unavailable" :
            boss + " • " + (bestBank ? "Best in Bank" : "Wiki") + " / " + tier.label() + " / " + selectedStyle();
    }
    /**
     * Legacy, read-only compatibility check for the existing BUR-0031 UI proof.
     * No fake kills are recorded or retained by this model. Not a session data source.
     * Remove only when Spoons authorizes updating that separately owned UI test.
     */
    @Deprecated
    public int demoKills() { return 0; }
    public String boss() { return boss; }
    public String method() { return method; }
    public Tier tier() { return tier; }
    public void chooseTier(Tier next) {
        Objects.requireNonNull(next,"tier");
        if(tier!=next) { tier=next; refresh(); }
    }
    public String selectedStyle() {
        return TieredOutfitterCatalog.style(boss,method);
    }
    public TieredOutfitterCatalog.Selection tierSelection() { return TieredOutfitterCatalog.find(boss,method,tier); }
    public VerifiedOutfitterCatalog.Profile profile() {
        var selected=tierSelection();return selected==null?null:selected.profile();
    }
    public List<String> methods() {
        return TieredOutfitterCatalog.methods(boss);
    }
    /** Baseline 15 boss records are immutable; two explicit additional encounters are registered separately. */
    public static List<String> availableBosses() {
        // Build 20: every manifest boss (Regular / Wilderness / Moons / Demi-bosses); excluded
        // raid, minigame, skilling and quest-only bosses are never present.
        return BossRegistry.selectableIds();
    }
    /** Build 19 encounter list (15 original + 2 demons), kept for regression comparisons. */
    public static List<String> build19Bosses() {
        var all=new ArrayList<String>(VerifiedOutfitterCatalog.BOSSES);
        all.addAll(DemonEncounterLoadouts.bosses());
        return List.copyOf(all);
    }
    public void chooseBoss(String value) {
        if (!availableBosses().contains(value)) throw new IllegalArgumentException("boss");
        if (!boss.equals(value)) {
            boss = value;
            method=methods().isEmpty()?"":methods().get(0);
            nearestAvailableTier();
            refresh();
        }
    }
    public void chooseMethod(String value) {
        if (!methods().contains(value)) throw new IllegalArgumentException("unavailable method");
        if (!method.equals(value)) { method=value; nearestAvailableTier(); refresh(); }
    }
    /** Build 20: a one-tier method (e.g. Recommended-only demons, Entry-only shaman drafts) opens on a
     *  tier that exists instead of an empty "unavailable" screen. Never invents a tier. */
    private void nearestAvailableTier() {
        if (method.isBlank() || TieredOutfitterCatalog.find(boss,method,tier)!=null) return;
        Tier[] order=switch(tier){
            case OPTIMAL -> new Tier[]{Tier.RECOMMENDED,Tier.ENTRY};
            case RECOMMENDED -> new Tier[]{Tier.ENTRY,Tier.OPTIMAL};
            case ENTRY -> new Tier[]{Tier.RECOMMENDED,Tier.OPTIMAL};
        };
        for (Tier t:order) if (TieredOutfitterCatalog.find(boss,method,t)!=null) { tier=t; return; }
    }
    public CombatStyle.Styles selectedStyles() { return MethodStyles.of(boss,method); }
    public BossRegistry.Entry bossEntry() { return BossRegistry.find(boss).orElse(null); }
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
    private void refresh() {
        var selected=profile();
        // Research plans never claim actual bank ownership. Demo bank fixtures remain
        // separate and do not turn a Wiki plan into Best in Bank optimization.
        plan = selected==null ? null : Outfitter.resolve(nextPlanId++,scope,selected.template(),mode,bank);
        bankSelection = new Selection(boss, plan);
    }
    public static double cumulativeChance(double probability, int independentKills) {
        if (probability < 0 || probability > 1 || independentKills < 0) throw new IllegalArgumentException("odds");
        return 1 - Math.pow(1-probability, independentKills);
    }
}
