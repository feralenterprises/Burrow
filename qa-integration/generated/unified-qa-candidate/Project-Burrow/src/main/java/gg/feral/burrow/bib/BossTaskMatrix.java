package gg.feral.burrow.bib;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Which Slayer tasks cover which boss. A direct boss task always counts. WIKI entries were read on the OSRS Wiki.
 * LEAD entries come only from RuneLite's kill-matching list and must not auto-select the helm until confirmed.
 */
public final class BossTaskMatrix
{
	public enum Coverage
	{
		COVERED, UNCONFIRMED, NOT_COVERED
	}

	private static final Map<String, Set<String>> WIKI = new HashMap<>();
	private static final Map<String, Set<String>> LEAD = new HashMap<>();
	private static final Set<String> WILDERNESS = new HashSet<>();
	/** Konar area text that confirms a boss, from the Wiki's Konar page. Matched as a substring. */
	private static final Map<String, String> KONAR_AREA = new HashMap<>();

	static
	{
		wiki("kraken", "cave kraken", "cave kraken boss");
		wiki("dagannoth kings", "dagannoth");
		wiki("abyssal sire", "abyssal demons");
		wiki("araxxor", "araxytes");
		wiki("kree'arra", "aviansie");
		wiki("callisto", "bears");
		wiki("artio", "bears", "callisto");
		wiki("skotizo", "black demons");
		wiki("vorkath", "blue dragons", "zombies");
		wiki("king black dragon", "black dragons");
		wiki("cerberus", "hellhounds");
		wiki("alchemical hydra", "hydras");
		wiki("grotesque guardians", "gargoyles");
		wiki("thermonuclear smoke devil", "smoke devils");
		wiki("scurrius", "rats");
		wiki("brutus", "cows");
		wiki("obor", "hill giants");
		wiki("bryophyta", "moss giants");
		wiki("eldric the ice king", "ice giants");
		wiki("branda the fire queen", "fire giants");
		wiki("amoxliatl", "lesser naguas");
		wiki("demonic gorillas", "black demons");

		wiki("spindel", "venenatis");
		wiki("calvar'ion", "vet'ion");
		wiki("deranged archaeologist", "crazy archaeologist");

		for (String boss : new String[]{"callisto", "artio", "venenatis", "spindel", "vet'ion", "calvar'ion",
			"chaos elemental", "chaos fanatic", "crazy archaeologist", "scorpia", "king black dragon"})
		{
			WILDERNESS.add(norm(boss));
		}
		KONAR_AREA.put(norm("abyssal sire"), "abyss");
		KONAR_AREA.put(norm("dagannoth kings"), "waterbirth");
		KONAR_AREA.put(norm("kraken"), "kraken");
		KONAR_AREA.put(norm("alchemical hydra"), "karuulm");

		lead("tormented demons", "greater demons");
		lead("araxxor", "spiders");
		lead("skotizo", "greater demons");
		lead("k'ril tsutsaroth", "greater demons");
		lead("scorpia", "scorpions");
		lead("vet'ion", "skeletons");
		lead("calvar'ion", "skeletons");
		lead("sarachnis", "spiders");
		lead("venenatis", "spiders");
		lead("spindel", "spiders");
		lead("tztok-jad", "tzhaar");
		lead("tzkal-zuk", "tzhaar");
	}

	private BossTaskMatrix()
	{
	}

	/** Only bosses the Wiki pages read here state either way; everything else stays unknown. */
	public static RangedCapeAdvisor.Tri undead(String boss)
	{
		String b = norm(boss);
		if (b.equals(norm("vorkath")) || b.equals(norm("calvar'ion")))
		{
			return RangedCapeAdvisor.Tri.YES;
		}
		if (b.equals(norm("the nightmare")) || b.equals(norm("phosani's nightmare")) || b.equals(norm("vardorvis")))
		{
			return RangedCapeAdvisor.Tri.NO;
		}
		return RangedCapeAdvisor.Tri.UNKNOWN;
	}

	public static Coverage coverage(String boss, SlayerTask task)
	{
		if (task == null || task.state != SlayerTask.State.ACTIVE)
		{
			return Coverage.NOT_COVERED;
		}
		String b = norm(boss);
		String t = norm(task.taskName);
		boolean direct = b.equals(t);
		boolean wiki = WIKI.getOrDefault(b, Set.of()).contains(t);
		if (!direct && !wiki)
		{
			return LEAD.getOrDefault(b, Set.of()).contains(t) ? Coverage.UNCONFIRMED : Coverage.NOT_COVERED;
		}
		if (task.master == SlayerTask.MASTER_KRYSTILIA && !WILDERNESS.contains(b))
		{
			return Coverage.NOT_COVERED;
		}
		if (task.master == SlayerTask.MASTER_KONAR && !direct)
		{
			return konarArea(b, task.area);
		}
		return Coverage.COVERED;
	}

	/** Konar parent tasks only cover a boss inside the assigned area; an unknown area is never assumed. */
	private static Coverage konarArea(String boss, String area)
	{
		String wanted = KONAR_AREA.get(boss);
		if (wanted == null || area == null)
		{
			return Coverage.UNCONFIRMED;
		}
		return area.toLowerCase(Locale.ROOT).contains(wanted) ? Coverage.COVERED : Coverage.NOT_COVERED;
	}

	private static String norm(String s)
	{
		String n = s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
		n = n.startsWith("the ") ? n.substring(4) : n;
		return n.endsWith("s") ? n.substring(0, n.length() - 1) : n;
	}

	private static void wiki(String boss, String... tasks)
	{
		WIKI.put(norm(boss), normalised(tasks));
	}

	private static void lead(String boss, String... tasks)
	{
		LEAD.put(norm(boss), normalised(tasks));
	}

	private static Set<String> normalised(String... tasks)
	{
		Set<String> set = new HashSet<>();
		Arrays.stream(tasks).forEach(t -> set.add(norm(t)));
		return set;
	}
}
