package gg.feral.burrow.experience;

import gg.feral.burrow.core.RareDropRowOdds;
import gg.feral.burrow.core.RareDropTable;
import java.util.OptionalDouble;

/** One published drop rate. It is never the player's accumulated odds. An unrated row has no probability. */
public record PublishedDrop(String key, int itemId, String name, RateType rateType, OptionalDouble perKillChance,
                            String displayRate, String caveat, String sourceUrl, String sourceRead, EvidenceStatus status) {
    /** INDEPENDENT is its own roll or table, CONDITIONAL needs a stated condition, UNKNOWN has no sourced rate. */
    public enum RateType { INDEPENDENT, CONDITIONAL, UNKNOWN }

    public static PublishedDrop of(RareDropTable.Encounter encounter, RareDropTable.Drop drop) {
        boolean conditional = drop.condition() != null;
        return new PublishedDrop(drop.key(), drop.itemId(), drop.name(), conditional ? RateType.CONDITIONAL : RateType.INDEPENDENT,
            OptionalDouble.of(RareDropRowOdds.perKill(drop)), drop.rateText(), conditional ? drop.condition() : "",
            encounter.wikiUrl(), RareDropTable.READ_DATE, EvidenceStatus.VERIFIED);
    }

    /** A drop with no sourced rate. It stays unknown, never 0%. */
    public static PublishedDrop unknown(String key, int itemId, String name, String reason, String sourceUrl) {
        return new PublishedDrop(key, itemId, name, RateType.UNKNOWN, OptionalDouble.empty(), "Unknown", reason, sourceUrl, "",
            EvidenceStatus.UNKNOWN);
    }
}
