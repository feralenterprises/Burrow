package gg.feral.burrow.bib;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Typed weapon-to-ammo rules. A weapon or ammo this table does not know is reported as unknown, never as valid.
 */
public final class AmmoRules
{
	public enum Status
	{
		NOT_REQUIRED, OK, MISSING, WRONG_TYPE, OVER_TIER, RESTRICTED, UNKNOWN_WEAPON, UNKNOWN_AMMO
	}

	/** What an Ava-style device can do for this weapon's ammunition. */
	public enum Recovery
	{
		RECOVERED, NOTHING_TO_SAVE, NOT_RECOVERED, UNKNOWN
	}

	/** What a weapon needs internally. NOT_REQUIRED for the ammo slot never means the weapon is ready to attack. */
	public enum InternalResource
	{
		NONE, DARTS_AND_SCALES, WEAPON_CHARGES, UNKNOWN
	}

	private enum Kind
	{
		ARROW, BOLT, JAVELIN, EXACT, NONE
	}

	private static final class Spec
	{
		final Kind kind;
		final int maxTier;
		final Set<String> exact;
		final Recovery recovery;

		Spec(Kind kind, int maxTier, Set<String> exact, Recovery recovery)
		{
			this.kind = kind;
			this.maxTier = maxTier;
			this.exact = exact;
			this.recovery = recovery;
		}
	}

	private static final Map<String, Spec> WEAPONS = new HashMap<>();
	private static final Set<String> THROWN = new HashSet<>();
	private static final Map<String, Integer> ARROW_TIERS = new HashMap<>();
	private static final Map<String, Integer> BOLT_TIERS = new HashMap<>();

	static
	{
		arrows(1, "shortbow", "longbow", "rain bow");
		arrows(5, "oak shortbow", "oak longbow", "signed oak bow");
		arrows(20, "willow shortbow", "willow longbow", "willow comp bow");
		arrows(30, "maple shortbow", "maple longbow");
		arrows(40, "yew shortbow", "yew longbow", "yew comp bow");
		arrows(50, "magic shortbow", "magic shortbow (i)", "magic longbow", "magic comp bow", "seercull", "bone shortbow");
		arrows(60, "dark bow", "3rd age bow", "venator bow", "twisted bow", "nature's recurve", "scorching bow");

		bolts(1, "crossbow", "phoenix crossbow", "bronze crossbow");
		bolts(16, "blurite crossbow");
		bolts(26, "iron crossbow");
		bolts(31, "steel crossbow");
		bolts(36, "mithril crossbow");
		bolts(46, "adamant crossbow");
		bolts(61, "rune crossbow");
		bolts(64, "dragon crossbow", "dragon hunter crossbow", "armadyl crossbow", "zaryte crossbow");

		WEAPONS.put("light ballista", new Spec(Kind.JAVELIN, 0, null, Recovery.RECOVERED));
		WEAPONS.put("heavy ballista", new Spec(Kind.JAVELIN, 0, null, Recovery.RECOVERED));

		exact("ogre bow", Recovery.UNKNOWN, "ogre arrow", "bronze brutal", "iron brutal", "steel brutal", "black brutal",
			"mithril brutal");
		exact("comp ogre bow", Recovery.UNKNOWN, "ogre arrow", "bronze brutal", "iron brutal", "steel brutal", "black brutal",
			"mithril brutal", "adamant brutal", "rune brutal");
		exact("training bow", Recovery.UNKNOWN, "training arrows");
		exact("dorgeshuun crossbow", Recovery.UNKNOWN, "bone bolts", "bronze bolts", "blurite bolts", "iron bolts");
		exact("ascension crossbows", Recovery.UNKNOWN, "ascension bolts", "diamond ascension bolts", "onyx ascension bolts");
		exact("swamp lizard", Recovery.NOT_RECOVERED, "guam tar");
		exact("orange salamander", Recovery.NOT_RECOVERED, "marrentill tar");
		exact("red salamander", Recovery.NOT_RECOVERED, "tarromin tar");
		exact("black salamander", Recovery.NOT_RECOVERED, "harralander tar");
		exact("tecu salamander", Recovery.NOT_RECOVERED, "irit tar");
		exact("hunters' crossbow", Recovery.UNKNOWN, "kebbit bolts", "long kebbit bolts");
		exact("hunters' sunlight crossbow", Recovery.UNKNOWN, "sunlight antler bolts", "moonlight antler bolts");
		exact("karil's crossbow", Recovery.NOT_RECOVERED, "bolt rack");
		exact("eclipse atlatl", Recovery.RECOVERED, "atlatl dart");

		none(Recovery.NOTHING_TO_SAVE, "crystal bow", "bow of faerdhinen", "craw's bow", "webweaver bow");
		none(Recovery.RECOVERED, "toxic blowpipe");
		none(Recovery.NOT_RECOVERED, "chinchompa", "red chinchompa", "black chinchompa", "holy water");
		none(Recovery.RECOVERED, "toktz-xil-ul");

		for (String metal : new String[]{"bronze", "iron", "steel", "mithril", "adamant", "rune", "dragon"})
		{
			THROWN.add(metal + " dart");
			THROWN.add(metal + " knife");
			THROWN.add(metal + " thrownaxe");
		}
		THROWN.add("black dart");
		THROWN.add("black knife");
		THROWN.add("amethyst dart");

		tier(ARROW_TIERS, 1, "bronze", "iron");
		tier(ARROW_TIERS, 5, "steel");
		tier(ARROW_TIERS, 20, "mithril");
		tier(ARROW_TIERS, 30, "adamant");
		tier(ARROW_TIERS, 40, "rune", "ice");
		tier(ARROW_TIERS, 50, "amethyst", "broad");
		tier(ARROW_TIERS, 60, "dragon", "seeking dragon");

		tier(BOLT_TIERS, 1, "bronze", "opal");
		tier(BOLT_TIERS, 16, "blurite", "jade");
		tier(BOLT_TIERS, 26, "iron", "pearl", "silver");
		tier(BOLT_TIERS, 31, "steel", "topaz");
		tier(BOLT_TIERS, 36, "mithril", "sapphire", "emerald");
		tier(BOLT_TIERS, 46, "adamant", "ruby", "diamond");
		tier(BOLT_TIERS, 61, "runite", "broad", "amethyst broad", "dragonstone", "onyx");
		tier(BOLT_TIERS, 64, "dragon");
	}

	private AmmoRules()
	{
	}

	/**
	 * @param weaponName worn or planned weapon name
	 * @param ammoName   ammo slot item name, or null when the slot is empty
	 */
	public static Status check(String weaponName, String ammoName)
	{
		String weapon = canonicalWeapon(weaponName);
		Spec spec = WEAPONS.get(weapon);
		if (spec == null)
		{
			return isOwnAmmoWeapon(weapon) ? Status.NOT_REQUIRED : Status.UNKNOWN_WEAPON;
		}
		if (spec.kind == Kind.NONE)
		{
			return Status.NOT_REQUIRED;
		}
		if (ammoName == null || ammoName.trim().isEmpty())
		{
			return Status.MISSING;
		}
		String ammo = canonicalAmmo(ammoName);
		if (isRestricted(ammo))
		{
			return Status.RESTRICTED;
		}
		switch (spec.kind)
		{
			case ARROW:
				return tiered(ammo, " arrow", ARROW_TIERS, spec.maxTier);
			case BOLT:
				return ammo.endsWith(" dragon bolts")
					? (64 <= spec.maxTier ? Status.OK : Status.OVER_TIER)
					: tiered(ammo, " bolts", BOLT_TIERS, spec.maxTier);
			case JAVELIN:
				if (ammo.equals("silvthrill javelin"))
				{
					return Status.RESTRICTED;
				}
				return ammo.endsWith(" javelin") ? Status.OK : Status.WRONG_TYPE;
			default:
				return spec.exact.contains(ammo) ? Status.OK : Status.WRONG_TYPE;
		}
	}

	/** True when the ammo slot may stay empty or hold a blessing with this weapon. */
	public static boolean slotIsFree(String weaponName)
	{
		return check(weaponName, null) == Status.NOT_REQUIRED;
	}

	public static Recovery recovery(String weaponName)
	{
		String weapon = canonicalWeapon(weaponName);
		Spec spec = WEAPONS.get(weapon);
		if (spec != null)
		{
			return spec.recovery;
		}
		return isOwnAmmoWeapon(weapon) ? Recovery.RECOVERED : Recovery.UNKNOWN;
	}

	public static InternalResource internalResource(String weaponName)
	{
		String weapon = canonicalWeapon(weaponName);
		switch (weapon)
		{
			case "toxic blowpipe":
				return InternalResource.DARTS_AND_SCALES;
			case "crystal bow":
			case "bow of faerdhinen":
			case "craw's bow":
			case "webweaver bow":
				return InternalResource.WEAPON_CHARGES;
			default:
				return WEAPONS.containsKey(weapon) || THROWN.contains(weapon) ? InternalResource.NONE : InternalResource.UNKNOWN;
		}
	}

	private static Status tiered(String ammo, String suffix, Map<String, Integer> tiers, int maxTier)
	{
		String singular = suffix.trim();
		String stem;
		if (ammo.endsWith(suffix))
		{
			stem = ammo.substring(0, ammo.length() - suffix.length());
		}
		else if (ammo.endsWith(" " + singular + "s"))
		{
			stem = ammo.substring(0, ammo.length() - singular.length() - 2);
		}
		else
		{
			boolean otherFamily = ammo.endsWith(" arrow") || ammo.endsWith(" arrows") || ammo.endsWith(" bolts")
				|| ammo.endsWith(" javelin") || ammo.endsWith(" dart");
			return otherFamily ? Status.WRONG_TYPE : Status.UNKNOWN_AMMO;
		}
		Integer tier = tiers.get(stem);
		if (tier == null)
		{
			return Status.UNKNOWN_AMMO;
		}
		return tier <= maxTier ? Status.OK : Status.OVER_TIER;
	}

	private static boolean isRestricted(String ammo)
	{
		return ammo.startsWith("castle wars ")
			|| ammo.equals("barbed arrow") || ammo.equals("blunt arrow")
			|| ammo.equals("bullet arrow") || ammo.equals("field arrow");
	}

	/** Known thrown weapons sit in the weapon slot. An unlisted name stays unknown. */
	private static boolean isOwnAmmoWeapon(String weapon)
	{
		return THROWN.contains(weapon);
	}

	static String canonicalWeapon(String name)
	{
		String s = clean(name);
		if (s.startsWith("crystal bow"))
		{
			return "crystal bow";
		}
		if (s.startsWith("bow of faerdhinen"))
		{
			return "bow of faerdhinen";
		}
		return s.replaceAll(" \\((or|t|b|cr|bh|charged|uncharged|empty|p\\+*)\\)$", "");
	}

	private static String canonicalAmmo(String name)
	{
		String s = clean(name).replaceAll("\\s*\\((p\\+*|e|unf)\\)$", "");
		if (s.endsWith("s") && (s.endsWith(" arrows") || s.endsWith(" javelins")))
		{
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	private static String clean(String name)
	{
		return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
	}

	private static void arrows(int tier, String... names)
	{
		Arrays.stream(names).forEach(n -> WEAPONS.put(n, new Spec(Kind.ARROW, tier, null, Recovery.RECOVERED)));
	}

	private static void bolts(int tier, String... names)
	{
		Arrays.stream(names).forEach(n -> WEAPONS.put(n, new Spec(Kind.BOLT, tier, null, Recovery.RECOVERED)));
	}

	private static void exact(String weapon, Recovery recovery, String... ammo)
	{
		Set<String> set = new HashSet<>(Arrays.asList(ammo));
		WEAPONS.put(weapon, new Spec(Kind.EXACT, 0, Collections.unmodifiableSet(set), recovery));
	}

	private static void none(Recovery recovery, String... names)
	{
		Arrays.stream(names).forEach(n -> WEAPONS.put(n, new Spec(Kind.NONE, 0, null, recovery)));
	}

	private static void tier(Map<String, Integer> tiers, int tier, String... stems)
	{
		Arrays.stream(stems).forEach(s -> tiers.put(s, tier));
	}
}
