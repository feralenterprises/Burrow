package gg.feral.burrow.experience;

import gg.feral.burrow.core.TieredOutfitterCatalog.Tier;

/** Immutable navigation state. Chronicle is a subview, never a fifth tab. Chronicle and Outfitter selections are separate. */
public record NavigationState(TopTab topTab, ChronicleRoute chronicleRoute, BrowseMemory browse, String chronicleBossId,
                              OutfitterSelection outfitter) {
    public enum TopTab { HOME, PATHFINDER, OUTFITTER, SESSIONS }
    public enum ChronicleRoute { BROWSE, DETAIL }
    /** @param scrollKey opaque value owned by the UI, restored on Back */
    public record BrowseMemory(String query, gg.feral.burrow.core.BossRegistry.Group group, ChronicleBrowseRequest.Sort sort, String scrollKey) {
        public static final BrowseMemory DEFAULT = new BrowseMemory("", null, ChronicleBrowseRequest.Sort.A_TO_Z, "");
    }
    /** @param bestBank false for any boss where Best in Bank is denied */
    public record OutfitterSelection(String bossId, String method, Tier tier, String fill, boolean bestBank) {
        public static final OutfitterSelection NONE = new OutfitterSelection(null, null, Tier.RECOMMENDED, "MATCH", false);
    }
    public static NavigationState initial() {
        return new NavigationState(TopTab.HOME, null, BrowseMemory.DEFAULT, null, OutfitterSelection.NONE);
    }
    public boolean chronicleOpen() { return chronicleRoute != null; }
}
