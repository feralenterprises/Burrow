package gg.feral.burrow.experience;

import java.util.List;

/**
 * An explanatory readiness heuristic, never a DPS or kill-time claim. HARD_BUT_DOABLE is not impossible, and BLOCKED is
 * a real missing requirement, not a harder colour. UNKNOWN must not be drawn as ready.
 */
public record BanditBossCandidate(String bossId, Readiness readiness, Reason reasonCode, String reason, String method,
                                  List<String> mechanicRequirements, EvidenceStatus status, String warning,
                                  DestinationIntent nextAction) {
    public enum Readiness { COMFORTABLE, STRETCH, HARD_BUT_DOABLE, UNKNOWN, BLOCKED }
    public enum Reason { GEAR_READY, GEAR_NEEDS_CHECKS, ENTRY_GEAR_ONLY, MISSING_SWITCHES, OWNER_DIFFICULTY_CAP, NOT_SIGNED_IN,
        NO_BANK_SNAPSHOT, BANK_STALE, CONFIRMED_EMPTY_BANK, NO_REVIEWED_WEAPON_OWNED, NO_VERIFIED_LOADOUT, ACCESS_UNKNOWN,
        ACCESS_NOT_MET, PROTECTION_MISSING, WILDERNESS_NOT_ASSESSED, UNKNOWN_BOSS }
    public BanditBossCandidate { mechanicRequirements = List.copyOf(mechanicRequirements); }
}
