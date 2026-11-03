package gg.feral.burrow.experience;

import gg.feral.burrow.core.BossRegistry;
import gg.feral.burrow.core.CombatStyle;
import gg.feral.burrow.core.RareDropTable;
import java.util.List;

/**
 * One browse row. It has no player data. Styles describe viable approaches from the registry, not account readiness.
 * {@code imageRef} is null until an approved asset exists, and is never generated.
 */
public record BossPreview(String bossId, String displayName, String wikiUrl, String imageRef, BossRegistry.Group group,
                          boolean raidReference, List<CombatStyle> recommendedStyles, EvidenceStatus styleEvidence,
                          String mechanicNote, List<PublishedDrop> notableDrops, RareDropTable.Coverage dropCoverage,
                          String dropCoverageNote, boolean wilderness, String pvpWarning,
                          OutfitterNavigationIntent outfitter) {
    public BossPreview { recommendedStyles = List.copyOf(recommendedStyles); notableDrops = List.copyOf(notableDrops); }
}
