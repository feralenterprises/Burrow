package gg.feral.burrow.bib;

import java.util.Set;

/**
 * Owner's Ranged cape order: Dizana's quiver, assembler, accumulator, attractor. Applies to any Ranged weapon.
 */
public final class RangedCapeAdvisor
{
	public enum Tri
	{
		YES, NO, UNKNOWN
	}

	/** Which Ava device was brought to Ava with the quiver. */
	public enum QuiverUpgrade
	{
		NONE, ACCUMULATOR, ASSEMBLER, UNKNOWN
	}

	public enum Torso
	{
		SAFE, METAL, UNKNOWN
	}

	public enum Saving
	{
		SAVES_80, SAVES_72, SAVES_60, NOTHING_TO_SAVE, OFF_METAL_TORSO, NO_AVA_UPGRADE, UNCONFIRMED, NO_DEVICE
	}

	public static final class Pick
	{
		public final int itemId;
		public final Saving saving;

		Pick(int itemId, Saving saving)
		{
			this.itemId = itemId;
			this.saving = saving;
		}
	}

	private RangedCapeAdvisor()
	{
	}

	private static final Set<String> METAL_TORSOS = Set.of("ahrim's robetop", "dharok's platebody", "guthan's platebody",
		"justiciar chestguard", "rune platebody", "rune platebody (g)", "rune platebody (t)", "steel platebody");
	private static final Set<String> SAFE_METAL_LOOKING = Set.of("3rd age platebody", "ancient platebody", "armadyl chestplate",
		"bandos platebody", "void knight top");

	/** Only names listed on the Wiki are decided; every other torso stays unknown. */
	public static Torso torso(String bodyName)
	{
		String n = bodyName == null ? "" : bodyName.trim().toLowerCase(java.util.Locale.ROOT);
		if (METAL_TORSOS.contains(n))
		{
			return Torso.METAL;
		}
		return SAFE_METAL_LOOKING.contains(n) ? Torso.SAFE : Torso.UNKNOWN;
	}

	/** Owner's order: higher is better, 0 means not an ammo-saving Ranged cape. */
	public static int rank(int itemId)
	{
		if (GearIds.DIZANA_ITEMS.contains(itemId))
		{
			return 6;
		}
		if (GearIds.ASSEMBLER_BASE.contains(itemId) || GearIds.ASSEMBLER_VARIANTS.contains(itemId))
		{
			return 5;
		}
		if (GearIds.ACCUMULATOR_BASE.contains(itemId) || GearIds.ACCUMULATOR_VARIANTS.contains(itemId))
		{
			return 4;
		}
		if (GearIds.ATTRACTOR.contains(itemId))
		{
			return 3;
		}
		return GearIds.RANGING_CAPE.contains(itemId) ? 2 : 0;
	}

	public static String savingNote(Saving saving)
	{
		switch (saving)
		{
			case SAVES_80:
				return "saves 80% of ammo";
			case SAVES_72:
				return "saves 72% of ammo";
			case SAVES_60:
				return "saves 60% of ammo";
			case NOTHING_TO_SAVE:
				return "this weapon has no recoverable ammo; kept for its stats";
			case OFF_METAL_TORSO:
				return "metal torso stops ammo saving";
			case NO_AVA_UPGRADE:
				return "no Ava upgrade, so no ammo saving";
			default:
				return "ammo saving unconfirmed";
		}
	}

	public static Pick pick(Set<Integer> owned, int rangedLevel, String weaponName, Torso torso, QuiverUpgrade quiverUpgrade)
	{
		AmmoRules.Recovery recovery = AmmoRules.recovery(weaponName);
		int id;
		if (rangedLevel >= 75 && (id = first(owned, GearIds.DIZANA_ITEMS)) > 0)
		{
			return new Pick(id, quiverSaving(recovery, quiverUpgrade));
		}
		if (rangedLevel >= 70 && (id = first(owned, GearIds.ASSEMBLER_BASE)) > 0)
		{
			return new Pick(id, deviceSaving(recovery, torso, Saving.SAVES_80));
		}
		if (rangedLevel >= 70 && (id = first(owned, GearIds.ASSEMBLER_VARIANTS)) > 0)
		{
			return new Pick(id, variantSaving(recovery));
		}
		if (rangedLevel >= 50 && (id = first(owned, GearIds.ACCUMULATOR_BASE)) > 0)
		{
			return new Pick(id, deviceSaving(recovery, torso, Saving.SAVES_72));
		}
		if (rangedLevel >= 50 && (id = first(owned, GearIds.ACCUMULATOR_VARIANTS)) > 0)
		{
			return new Pick(id, variantSaving(recovery));
		}
		if (rangedLevel >= 30 && (id = first(owned, GearIds.ATTRACTOR)) > 0)
		{
			return new Pick(id, deviceSaving(recovery, torso, Saving.SAVES_60));
		}
		if (rangedLevel >= 99 && (id = first(owned, GearIds.RANGING_CAPE)) > 0)
		{
			return new Pick(id, Saving.UNCONFIRMED);
		}
		return new Pick(-1, Saving.NO_DEVICE);
	}

	private static Saving quiverSaving(AmmoRules.Recovery recovery, QuiverUpgrade upgrade)
	{
		Saving weapon = weaponLimit(recovery);
		if (weapon != null)
		{
			return weapon;
		}
		// The Wiki gives a saving rate only after the Ava upgrade, and metal torsos do not affect the quiver.
		switch (upgrade)
		{
			case ASSEMBLER:
				return Saving.SAVES_80;
			case ACCUMULATOR:
				return Saving.SAVES_72;
			case NONE:
				return Saving.NO_AVA_UPGRADE;
			default:
				return Saving.UNCONFIRMED;
		}
	}

	private static Saving deviceSaving(AmmoRules.Recovery recovery, Torso torso, Saving rate)
	{
		Saving weapon = weaponLimit(recovery);
		if (weapon != null)
		{
			return weapon;
		}
		if (torso == Torso.METAL)
		{
			return Saving.OFF_METAL_TORSO;
		}
		return torso == Torso.SAFE ? rate : Saving.UNCONFIRMED;
	}

	private static Saving variantSaving(AmmoRules.Recovery recovery)
	{
		Saving weapon = weaponLimit(recovery);
		return weapon != null ? weapon : Saving.UNCONFIRMED;
	}

	private static Saving weaponLimit(AmmoRules.Recovery recovery)
	{
		switch (recovery)
		{
			case NOTHING_TO_SAVE:
			case NOT_RECOVERED:
				return Saving.NOTHING_TO_SAVE;
			case UNKNOWN:
				return Saving.UNCONFIRMED;
			default:
				return null;
		}
	}

	private static int first(Set<Integer> owned, Set<Integer> group)
	{
		return owned == null ? -1 : owned.stream().filter(group::contains).sorted().findFirst().orElse(-1);
	}
}
