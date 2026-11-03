package gg.feral.burrow.bib;

import java.util.List;
import java.util.Map;

/**
 * Owned variants of the two trident families. A full, partly charged, enhanced or ornamented trident is the same
 * one-handed weapon, so the plan may use whichever copy the bank holds. Uncharged copies are deliberately absent.
 * IDs are RuneLite ItemID constants; preference is most charges first. Pure Java.
 */
public final class MagicWeaponRules
{
	public static final int SEAS = 11907, SWAMP = 12899;
	private static final List<Integer> SEAS_IDS = List.of(22288, 33326, 11905, 33323, 11907, 33322);
	private static final List<Integer> SWAMP_IDS = List.of(22292, 33318, 12899, 33314);

	private MagicWeaponRules()
	{
	}

	private static List<Integer> family(int id)
	{
		return SEAS_IDS.contains(id) ? SEAS_IDS : SWAMP_IDS.contains(id) ? SWAMP_IDS : List.of();
	}

	public static boolean isTrident(int id)
	{
		return !family(id).isEmpty();
	}

	/** OSRS Wiki: 75 Magic to wield the seas trident, 78 for the swamp trident. */
	public static int magicLevel(int id)
	{
		return SWAMP_IDS.contains(id) ? 78 : 75;
	}

	/** The plain catalogue ID for a trident variant; other items are returned unchanged. */
	public static int canonical(int id)
	{
		return SEAS_IDS.contains(id) ? SEAS : SWAMP_IDS.contains(id) ? SWAMP : id;
	}

	/** The owned copy of this trident, or -1. Never crosses families: a seas trident is not a swamp trident. */
	public static int ownedVariant(int plannedId, Map<Integer, Integer> stock)
	{
		for (int id : family(plannedId))
		{
			if (stock.getOrDefault(id, 0) > 0)
			{
				return id;
			}
		}
		return -1;
	}
}
