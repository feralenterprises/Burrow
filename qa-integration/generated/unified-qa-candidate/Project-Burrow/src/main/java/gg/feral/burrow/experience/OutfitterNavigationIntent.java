package gg.feral.burrow.experience;

/** What "Open in Outfitter" may do for a boss. Nothing happens until the player issues the explicit command. */
public record OutfitterNavigationIntent(String bossId, String method, Availability availability, boolean bestBankAllowed, String reason) {
    public enum Availability { AVAILABLE, WIKI_GEAR_ONLY_WILDERNESS, NO_VERIFIED_LOADOUT, UNSUPPORTED_RAID, UNKNOWN_BOSS }
    public boolean canOpen() { return availability == Availability.AVAILABLE || availability == Availability.WIKI_GEAR_ONLY_WILDERNESS; }
}
