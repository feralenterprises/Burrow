package gg.feral.burrow.experience;

import gg.feral.burrow.core.RareDropStore;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalLong;

/** A selected boss. Account fields are UNKNOWN unless a signed-in account's evidence was supplied. */
public record ChronicleDetail(BossPreview preview, List<EncounterDrops> encounters, List<String> notModelled,
                              List<TaskReference> tasks, OutfitterNavigationIntent outfitter) {
    public ChronicleDetail { encounters = List.copyOf(encounters); notModelled = List.copyOf(notModelled); tasks = List.copyOf(tasks); }

    public record EncounterDrops(String encounter, String kcUnit, OptionalLong eligibleKc, RareDropStore.Source kcSource,
                                 EvidenceStatus kcStatus, List<PersonalDropRow> rows) {
        public EncounterDrops { rows = List.copyOf(rows); }
    }
    /** {@code atLeastOne} is a historical cumulative figure at this KC, not a forecast. Empty means unknown. */
    public record PersonalDropRow(PublishedDrop published, OptionalDouble atLeastOne, EvidenceStatus oddsStatus, String reason,
                                  String unlockSource) {}
    public enum Completion { UNKNOWN, COMPLETE }
    public record TaskReference(String name, String tier, String type, String requirement, String tip, String tipSourceUrl,
                                Completion completion, String completionSource) {
        public boolean hasTip() { return !tip.isEmpty(); }
    }
}
