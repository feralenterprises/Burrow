package gg.feral.burrow.experience;

import gg.feral.burrow.core.*;
import gg.feral.burrow.experience.ChronicleDetail.*;
import java.util.*;

/** Read-only Chronicle data. Composes the registry, drop table, odds and task guide. It writes nothing. */
public final class ChronicleProvider {
    private static final int NOTABLE_DROPS = 3;
    public static final String PVP_WARNING = "Wilderness: other players can attack you. Best in Bank is off here.";
    private ChronicleProvider() {}

    /** The canonical Chronicle ID for a name or alias, or empty for anything unknown. */
    public static Optional<String> resolve(String nameOrAlias) {
        if (nameOrAlias == null) return Optional.empty();
        var boss = BossRegistry.find(nameOrAlias);
        if (boss.isPresent()) return boss.map(BossRegistry.Entry::id);
        return RaidChronicle.find(nameOrAlias).map(RaidChronicle.Entry::name);
    }

    public static ChronicleBrowseResult browse(ChronicleBrowseRequest request) {
        String q = request.query().toLowerCase(Locale.ROOT);
        List<BossPreview> matches = new ArrayList<>();
        for (var entry : BossRegistry.all())
            if ((request.group() == null || request.group() == entry.group()) && matches(entry, q)) matches.add(preview(entry));
        if (request.includeRaids() && request.group() == null)
            for (var raid : RaidChronicle.all())
                if (raid.name().toLowerCase(Locale.ROOT).contains(q) || raid.raid().toLowerCase(Locale.ROOT).contains(q)) matches.add(raidPreview(raid));
        Comparator<BossPreview> byName = Comparator.comparing((BossPreview b) -> b.displayName().toLowerCase(Locale.ROOT)).thenComparing(BossPreview::bossId);
        var applied = request.sort() == ChronicleBrowseRequest.Sort.Z_TO_A ? ChronicleBrowseRequest.Sort.Z_TO_A : ChronicleBrowseRequest.Sort.A_TO_Z;
        matches.sort(applied == ChronicleBrowseRequest.Sort.Z_TO_A ? byName.reversed() : byName);
        String note = request.sort() == ChronicleBrowseRequest.Sort.DIFFICULTY ? "No sourced difficulty ranking exists yet, so this list is A to Z." : "";
        int from = (int) Math.min(matches.size(), (long) request.page() * request.limit());
        int to = Math.min(matches.size(), from + request.limit());
        return new ChronicleBrowseResult(matches.subList(from, to), matches.size(), request.page(), request.limit(), applied, note);
    }

    private static boolean matches(BossRegistry.Entry e, String q) {
        if (q.isEmpty() || e.id().toLowerCase(Locale.ROOT).contains(q) || e.manifestName().toLowerCase(Locale.ROOT).contains(q)) return true;
        return e.aliases().stream().anyMatch(a -> a.toLowerCase(Locale.ROOT).contains(q));
    }

    public static Optional<BossPreview> preview(String nameOrAlias) {
        if (nameOrAlias == null) return Optional.empty();
        var boss = BossRegistry.find(nameOrAlias);
        if (boss.isPresent()) return boss.map(ChronicleProvider::preview);
        return RaidChronicle.find(nameOrAlias).map(ChronicleProvider::raidPreview);
    }

    private static BossPreview preview(BossRegistry.Entry e) {
        List<PublishedDrop> drops = new ArrayList<>();
        for (var encounter : RareDropTable.encounters(e.id()))
            for (var drop : encounter.drops()) drops.add(PublishedDrop.of(encounter, drop));
        drops.sort(Comparator.comparingDouble((PublishedDrop d) -> d.perKillChance().orElse(1)).thenComparing(PublishedDrop::key));
        var styles = e.suggestedStyles();
        return new BossPreview(e.id(), e.id(), e.wikiUrl(), null, e.group(), false, styles,
            styles.isEmpty() ? EvidenceStatus.UNKNOWN : EvidenceStatus.INFERRED_WITH_LIMITS, e.mechanic(),
            drops.subList(0, Math.min(NOTABLE_DROPS, drops.size())), RareDropTable.coverage(e.id()), RareDropTable.coverageNote(e.id()),
            e.wilderness(), e.wilderness() ? PVP_WARNING : "", outfitter(e));
    }

    private static BossPreview raidPreview(RaidChronicle.Entry raid) {
        return new BossPreview(raid.name(), raid.name(), raid.wikiUrl(), null, null, true, List.of(), EvidenceStatus.UNKNOWN, "",
            List.of(), RareDropTable.Coverage.NOT_YET_RESEARCHED, RaidChronicle.UNSUPPORTED, false, "",
            new OutfitterNavigationIntent(raid.name(), null, OutfitterNavigationIntent.Availability.UNSUPPORTED_RAID, false, RaidChronicle.UNSUPPORTED));
    }

    private static OutfitterNavigationIntent outfitter(BossRegistry.Entry e) {
        var methods = TieredOutfitterCatalog.methods(e.id());
        if (methods.isEmpty())
            return new OutfitterNavigationIntent(e.id(), null, OutfitterNavigationIntent.Availability.NO_VERIFIED_LOADOUT, false, "Guide not yet verified");
        if (!e.bestBankAllowed())
            return new OutfitterNavigationIntent(e.id(), methods.get(0), OutfitterNavigationIntent.Availability.WIKI_GEAR_ONLY_WILDERNESS, false, PVP_WARNING);
        return new OutfitterNavigationIntent(e.id(), methods.get(0), OutfitterNavigationIntent.Availability.AVAILABLE, true, "");
    }

    public static OutfitterNavigationIntent outfitterIntent(String nameOrAlias) {
        return preview(nameOrAlias).map(BossPreview::outfitter)
            .orElse(new OutfitterNavigationIntent(nameOrAlias, null, OutfitterNavigationIntent.Availability.UNKNOWN_BOSS, false, "Unknown boss"));
    }

    /** Account-free detail. Personal fields are all UNKNOWN. */
    public static Optional<ChronicleDetail> detail(String nameOrAlias) {
        return detail(nameOrAlias, AccountEvidenceContext.signedOut(), null, TaskCompletionEvidence.none());
    }

    /** {@code personal} is only used for a signed-in account, so no player figure can appear without one. */
    public static Optional<ChronicleDetail> detail(String nameOrAlias, AccountEvidenceContext account, PersonalDropEvidence personal, TaskCompletionEvidence completion) {
        var preview = preview(nameOrAlias);
        if (preview.isEmpty()) return Optional.empty();
        var boss = preview.get();
        PersonalDropEvidence evidence = account != null && account.identified() ? personal : null;
        TaskCompletionEvidence done = account != null && account.identified() && completion != null ? completion : TaskCompletionEvidence.none();
        List<EncounterDrops> encounters = new ArrayList<>();
        List<String> notModelled = new ArrayList<>();
        for (var encounter : RareDropTable.encounters(boss.bossId())) {
            encounters.add(encounterDrops(encounter, evidence));
            notModelled.addAll(encounter.notModelled());
        }
        List<TaskReference> tasks = new ArrayList<>();
        for (var task : CombatTaskGuide.forBoss(boss.bossId())) {
            String source = done.sourceByTask().get(task.name());
            boolean complete = source != null && !source.isBlank();
            tasks.add(new TaskReference(task.name(), task.tier(), task.type(), task.requirement(), task.tip(), task.source(),
                complete ? Completion.COMPLETE : Completion.UNKNOWN, complete ? source : ""));
        }
        return Optional.of(new ChronicleDetail(boss, encounters, notModelled, tasks, boss.outfitter()));
    }

    static EncounterDrops encounterDrops(RareDropTable.Encounter encounter, PersonalDropEvidence evidence) {
        RareDropStore.Effective kc = evidence == null ? null : evidence.kc(encounter.encounter());
        boolean known = kc != null && kc.known();
        List<PersonalDropRow> rows = new ArrayList<>();
        for (var drop : encounter.drops()) {
            var odds = RareDropRowOdds.row(encounter, drop, kc);
            EvidenceStatus status = odds.byKc().isPresent()
                ? (odds.kcSource() == RareDropStore.Source.MANUAL ? EvidenceStatus.INFERRED_WITH_LIMITS : EvidenceStatus.OBSERVED)
                : EvidenceStatus.UNKNOWN;
            rows.add(new PersonalDropRow(PublishedDrop.of(encounter, drop), odds.byKc(), status, odds.detail(),
                evidence == null ? "" : evidence.unlockSource(drop.itemId()).orElse("")));
        }
        return new EncounterDrops(encounter.encounter(), encounter.chestUnit(), known ? kc.value() : OptionalLong.empty(),
            known ? kc.source() : null, known ? EvidenceStatus.OBSERVED : EvidenceStatus.UNKNOWN, rows);
    }
}
