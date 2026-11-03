package gg.feral.burrow.experience;

import java.util.List;

/**
 * One optional housekeeping suggestion. Null minutes means "Time varies". A manual Done is the player's own statement
 * and is never verified activity, XP or bank change.
 *
 * @param reasons plain account checks that passed
 */
public record SpoonsTip(String tipId, String text, Integer minMinutes, Integer maxMinutes, List<String> reasons,
                        String sourceUrl, DestinationIntent destination, EvidenceStatus status) {
    public SpoonsTip { reasons = List.copyOf(reasons); }
}
