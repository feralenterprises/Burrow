package gg.feral.burrow.bib;

import gg.feral.burrow.bib.AmmoRules.Status;
import gg.feral.burrow.bib.HeadAdvisor.Reason;
import gg.feral.burrow.bib.RangedCapeAdvisor.QuiverUpgrade;
import gg.feral.burrow.bib.RangedCapeAdvisor.Saving;
import gg.feral.burrow.bib.RangedCapeAdvisor.Torso;
import gg.feral.burrow.bib.RangedCapeAdvisor.Tri;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Plain main-method checks, so they run offline with no test framework on the classpath. */
public final class BibRulesJUnitTest
{
	private static int pass;
	private static int fail;

	private static final int ASSEMBLER = 22109;
	private static final int ACCUMULATOR = 10499;
	private static final int ATTRACTOR = 10498;
	private static final int QUIVER = 28947;
	private static final int HELM = 11864;
	private static final int HELM_I = 11865;
	private static final int SALVE_EI = 12018;
	private static final int FACEGUARD = 24271;
	private static final int NEITIZNOT = 10828;
	private static final int TORVA = 26382;

	@Test
	void allRules()
	{
		pass = 0;
		fail = 0;
		ammo();
		capes();
		slayer();
		head();
		assertEquals(0, fail, "failed rule checks");
		assertTrue(pass > 100, "rule checks ran");
	}

	private static void ammo()
	{
		eq("ADD-01 bolts in a bow", Status.WRONG_TYPE, AmmoRules.check("Magic shortbow", "Runite bolts"));
		eq("ADD-02 Castle Wars bolts", Status.RESTRICTED, AmmoRules.check("Dragon crossbow", "Castle wars bolts"));
		eq("barbarian assault arrow", Status.RESTRICTED, AmmoRules.check("Twisted bow", "Bullet arrow"));
		eq("ADD-03 blowpipe empty slot", Status.NOT_REQUIRED, AmmoRules.check("Toxic blowpipe", null));
		eq("charged blowpipe", Status.NOT_REQUIRED, AmmoRules.check("Toxic blowpipe (charged)", null));
		eq("Bow of Faerdhinen variant", Status.NOT_REQUIRED, AmmoRules.check("Bow of Faerdhinen (c) (Cadarn)", null));
		eq("chinchompa free slot", Status.NOT_REQUIRED, AmmoRules.check("Red chinchompa", null));
		eq("missing ammo", Status.MISSING, AmmoRules.check("Twisted bow", null));
		eq("dragon arrow in Twisted bow", Status.OK, AmmoRules.check("Twisted bow", "Dragon arrow"));
		eq("dragon arrow plural", Status.OK, AmmoRules.check("Twisted bow", "Dragon arrows"));
		eq("poisoned arrow", Status.OK, AmmoRules.check("Yew shortbow", "Rune arrow(p++)"));
		eq("dragon arrow over tier", Status.OVER_TIER, AmmoRules.check("Magic shortbow", "Dragon arrow"));
		eq("broad bolts over tier", Status.OVER_TIER, AmmoRules.check("Adamant crossbow", "Broad bolts"));
		eq("broad bolts rune crossbow", Status.OK, AmmoRules.check("Rune crossbow", "Broad bolts"));
		eq("gem bolts", Status.OK, AmmoRules.check("Zaryte crossbow", "Ruby dragon bolts (e)"));
		eq("gem dragon bolts over tier", Status.OVER_TIER, AmmoRules.check("Rune crossbow", "Ruby dragon bolts (e)"));
		eq("Karil bolt rack", Status.OK, AmmoRules.check("Karil's crossbow", "Bolt rack"));
		eq("Karil wrong ammo", Status.WRONG_TYPE, AmmoRules.check("Karil's crossbow", "Dragon bolts"));
		eq("Hunters' needs kebbit", Status.WRONG_TYPE, AmmoRules.check("Hunters' crossbow", "Dragon bolts"));
		eq("ballista javelin", Status.OK, AmmoRules.check("Heavy ballista", "Dragon javelin"));
		eq("ballista with arrow", Status.WRONG_TYPE, AmmoRules.check("Heavy ballista", "Dragon arrow"));
		eq("quest javelin", Status.RESTRICTED, AmmoRules.check("Heavy ballista", "Silvthrill javelin"));
		eq("Scorching bow arrows", Status.OK, AmmoRules.check("Scorching bow", "Dragon arrow"));
		eq("ogre bow mithril brutal", Status.OK, AmmoRules.check("Ogre bow", "Mithril brutal"));
		eq("ogre bow rune brutal", Status.WRONG_TYPE, AmmoRules.check("Ogre bow", "Rune brutal"));
		eq("comp ogre rune brutal", Status.OK, AmmoRules.check("Comp ogre bow", "Rune brutal"));
		eq("unknown dart stays unknown", Status.UNKNOWN_WEAPON, AmmoRules.check("Mystery dart", null));
		eq("unknown knife stays unknown", Status.UNKNOWN_WEAPON, AmmoRules.check("Mystery knife", null));
		eq("known dart free slot", Status.NOT_REQUIRED, AmmoRules.check("Dragon dart", null));
		eq("poisoned knife free slot", Status.NOT_REQUIRED, AmmoRules.check("Rune knife (p++)", null));
		eq("known thrownaxe free slot", Status.NOT_REQUIRED, AmmoRules.check("Dragon thrownaxe", null));
		eq("blowpipe needs darts and scales", AmmoRules.InternalResource.DARTS_AND_SCALES,
			AmmoRules.internalResource("Toxic blowpipe"));
		eq("Bowfa needs charges", AmmoRules.InternalResource.WEAPON_CHARGES,
			AmmoRules.internalResource("Bow of Faerdhinen (c)"));
		eq("bow has no internal resource", AmmoRules.InternalResource.NONE, AmmoRules.internalResource("Twisted bow"));
		eq("unknown weapon resource", AmmoRules.InternalResource.UNKNOWN, AmmoRules.internalResource("Mystery bow"));
		eq("Dorgeshuun bone bolts", Status.OK, AmmoRules.check("Dorgeshuun crossbow", "Bone bolts"));
		eq("Dorgeshuun iron bolts", Status.OK, AmmoRules.check("Dorgeshuun crossbow", "Iron bolts"));
		eq("Dorgeshuun no tipped bolts", Status.WRONG_TYPE, AmmoRules.check("Dorgeshuun crossbow", "Opal bolts"));
		eq("Dorgeshuun no jade bolts", Status.WRONG_TYPE, AmmoRules.check("Dorgeshuun crossbow", "Jade bolts"));
		eq("Dorgeshuun no silver bolts", Status.WRONG_TYPE, AmmoRules.check("Dorgeshuun crossbow", "Silver bolts"));
		eq("Ascension enchanted bolts", Status.OK, AmmoRules.check("Ascension crossbows", "Onyx ascension bolts (e)"));
		eq("salamander tar", Status.OK, AmmoRules.check("Red salamander", "Tarromin tar"));
		eq("salamander wrong tar", Status.WRONG_TYPE, AmmoRules.check("Red salamander", "Guam tar"));
		eq("seeking dragon arrow", Status.OK, AmmoRules.check("Twisted bow", "Seeking dragon arrow"));
		eq("atlatl dart", Status.OK, AmmoRules.check("Eclipse atlatl", "Atlatl dart"));
		eq("unknown weapon", Status.UNKNOWN_WEAPON, AmmoRules.check("Mystery bow", "Dragon arrow"));
		eq("unknown ammo", Status.UNKNOWN_AMMO, AmmoRules.check("Twisted bow", "Mystery arrow"));
		eq("Karil recovery", AmmoRules.Recovery.NOT_RECOVERED, AmmoRules.recovery("Karil's crossbow"));
		eq("blowpipe recovery", AmmoRules.Recovery.RECOVERED, AmmoRules.recovery("Toxic blowpipe"));
	}

	private static void capes()
	{
		eq("ADD-05 quiver beats assembler", QUIVER,
			RangedCapeAdvisor.pick(Set.of(ASSEMBLER, QUIVER), 90, "Twisted bow", Torso.SAFE, QuiverUpgrade.UNKNOWN).itemId);
		eq("quiver upgrade unknown", Saving.UNCONFIRMED,
			RangedCapeAdvisor.pick(Set.of(QUIVER), 90, "Twisted bow", Torso.SAFE, QuiverUpgrade.UNKNOWN).saving);
		eq("quiver without upgrade gets no saving credit", Saving.NO_AVA_UPGRADE,
			RangedCapeAdvisor.pick(Set.of(QUIVER), 90, "Twisted bow", Torso.SAFE, QuiverUpgrade.NONE).saving);
		eq("quiver without upgrade still ranks first", QUIVER,
			RangedCapeAdvisor.pick(Set.of(ASSEMBLER, QUIVER), 90, "Twisted bow", Torso.SAFE, QuiverUpgrade.NONE).itemId);
		eq("accumulator-upgraded quiver saves 72", Saving.SAVES_72,
			RangedCapeAdvisor.pick(Set.of(QUIVER), 90, "Twisted bow", Torso.METAL, QuiverUpgrade.ACCUMULATOR).saving);
		eq("upgraded quiver ignores metal", Saving.SAVES_80,
			RangedCapeAdvisor.pick(Set.of(QUIVER), 90, "Twisted bow", Torso.METAL, QuiverUpgrade.ASSEMBLER).saving);
		eq("quiver level gate", ASSEMBLER,
			RangedCapeAdvisor.pick(Set.of(QUIVER, ASSEMBLER), 74, "Twisted bow", Torso.SAFE, QuiverUpgrade.ASSEMBLER).itemId);
		eq("order assembler over accumulator", ASSEMBLER,
			RangedCapeAdvisor.pick(Set.of(ATTRACTOR, ACCUMULATOR, ASSEMBLER), 90, "Twisted bow", Torso.SAFE, QuiverUpgrade.NONE).itemId);
		eq("order accumulator over attractor", ACCUMULATOR,
			RangedCapeAdvisor.pick(Set.of(ATTRACTOR, ACCUMULATOR), 90, "Twisted bow", Torso.SAFE, QuiverUpgrade.NONE).itemId);
		eq("attractor last", ATTRACTOR,
			RangedCapeAdvisor.pick(Set.of(ATTRACTOR), 40, "Maple shortbow", Torso.SAFE, QuiverUpgrade.NONE).itemId);
		RangedCapeAdvisor.Pick bofa = RangedCapeAdvisor.pick(Set.of(ASSEMBLER), 90, "Bow of Faerdhinen", Torso.SAFE, QuiverUpgrade.NONE);
		eq("ADD-04 BoFa keeps assembler", ASSEMBLER, bofa.itemId);
		eq("ADD-04 BoFa saves nothing", Saving.NOTHING_TO_SAVE, bofa.saving);
		eq("ADD-06 metal torso", Saving.OFF_METAL_TORSO,
			RangedCapeAdvisor.pick(Set.of(ASSEMBLER), 90, "Twisted bow", Torso.METAL, QuiverUpgrade.NONE).saving);
		eq("assembler 80", Saving.SAVES_80,
			RangedCapeAdvisor.pick(Set.of(ASSEMBLER), 90, "Toxic blowpipe", Torso.SAFE, QuiverUpgrade.NONE).saving);
		eq("Karil saves nothing", Saving.NOTHING_TO_SAVE,
			RangedCapeAdvisor.pick(Set.of(ASSEMBLER), 90, "Karil's crossbow", Torso.SAFE, QuiverUpgrade.NONE).saving);
		eq("no device", Saving.NO_DEVICE,
			RangedCapeAdvisor.pick(Set.of(), 90, "Twisted bow", Torso.SAFE, QuiverUpgrade.NONE).saving);
	}

	private static void slayer()
	{
		eq("unread count", SlayerTask.State.UNKNOWN, SlayerTask.of(null, null).state);
		eq("zero count", SlayerTask.State.NONE, SlayerTask.of(0, "Hydras").state);
		eq("no name", SlayerTask.State.UNKNOWN, SlayerTask.of(5, null).state);
		eq("ADD-08 direct boss task", BossTaskMatrix.Coverage.COVERED,
			BossTaskMatrix.coverage("Zulrah", SlayerTask.of(5, "Zulrah")));
		eq("ADD-08 other boss", BossTaskMatrix.Coverage.NOT_COVERED,
			BossTaskMatrix.coverage("Vorkath", SlayerTask.of(5, "Zulrah")));
		eq("The prefix", BossTaskMatrix.Coverage.COVERED,
			BossTaskMatrix.coverage("Alchemical Hydra", SlayerTask.of(5, "The Alchemical Hydra")));
		eq("parent task", BossTaskMatrix.Coverage.COVERED,
			BossTaskMatrix.coverage("Cerberus", SlayerTask.of(5, "Hellhounds")));
		eq("second parent task", BossTaskMatrix.Coverage.COVERED,
			BossTaskMatrix.coverage("Vorkath", SlayerTask.of(5, "Zombies")));
		eq("uppercase plural task", BossTaskMatrix.Coverage.COVERED,
			BossTaskMatrix.coverage("Kree'arra", SlayerTask.of(5, "AVIANSIES")));
		eq("uppercase Hellhounds", BossTaskMatrix.Coverage.COVERED,
			BossTaskMatrix.coverage("Cerberus", SlayerTask.of(5, "HELLHOUNDS")));
		eq("Kraken boss task", BossTaskMatrix.Coverage.COVERED,
			BossTaskMatrix.coverage("Kraken", SlayerTask.of(5, "THE CAVE KRAKEN BOSS")));
		int kon = SlayerTask.MASTER_KONAR;
		int kry = SlayerTask.MASTER_KRYSTILIA;
		eq("Konar Sire in Abyss", BossTaskMatrix.Coverage.COVERED,
			BossTaskMatrix.coverage("Abyssal Sire", SlayerTask.of(5, "Abyssal demons", kon, "the Abyss")));
		eq("Konar Sire at Catacombs", BossTaskMatrix.Coverage.NOT_COVERED,
			BossTaskMatrix.coverage("Abyssal Sire", SlayerTask.of(5, "Abyssal demons", kon, "the Catacombs of Kourend")));
		eq("Konar Kings at Lighthouse", BossTaskMatrix.Coverage.NOT_COVERED,
			BossTaskMatrix.coverage("Dagannoth Kings", SlayerTask.of(5, "Dagannoth", kon, "the Lighthouse")));
		eq("Konar unknown area", BossTaskMatrix.Coverage.UNCONFIRMED,
			BossTaskMatrix.coverage("Abyssal Sire", SlayerTask.of(5, "Abyssal demons", kon, null)));
		eq("Konar unmapped boss", BossTaskMatrix.Coverage.UNCONFIRMED,
			BossTaskMatrix.coverage("Cerberus", SlayerTask.of(5, "Hellhounds", kon, "Taverley Dungeon")));
		eq("Konar direct boss task", BossTaskMatrix.Coverage.COVERED,
			BossTaskMatrix.coverage("Alchemical Hydra", SlayerTask.of(5, "The Alchemical Hydra", kon, null)));
		eq("Krystilia Wilderness boss", BossTaskMatrix.Coverage.COVERED,
			BossTaskMatrix.coverage("King Black Dragon", SlayerTask.of(5, "Black dragons", kry, null)));
		eq("Krystilia non-Wilderness boss", BossTaskMatrix.Coverage.NOT_COVERED,
			BossTaskMatrix.coverage("Abyssal Sire", SlayerTask.of(5, "Abyssal demons", kry, null)));
		eq("Artio on Callisto task", BossTaskMatrix.Coverage.COVERED,
			BossTaskMatrix.coverage("Artio", SlayerTask.of(5, "Callisto")));
		eq("Spindel on Venenatis task", BossTaskMatrix.Coverage.COVERED,
			BossTaskMatrix.coverage("Spindel", SlayerTask.of(5, "Venenatis")));
		eq("Deranged on Crazy task", BossTaskMatrix.Coverage.COVERED,
			BossTaskMatrix.coverage("Deranged archaeologist", SlayerTask.of(5, "Crazy archaeologist")));
		eq("area changes identity", false, SlayerTask.of(5, "Dagannoth", kon, "the Lighthouse").identity("a")
			.equals(SlayerTask.of(5, "Dagannoth", kon, "Waterbirth Island").identity("a")));
		eq("lead only", BossTaskMatrix.Coverage.UNCONFIRMED,
			BossTaskMatrix.coverage("K'ril Tsutsaroth", SlayerTask.of(5, "Greater demons")));
		eq("ADD-10 identity changes", false,
			SlayerTask.of(5, "Hydras").identity("a").equals(SlayerTask.of(0, null).identity("a")));
		eq("account changes identity", false,
			SlayerTask.of(5, "Hydras").identity("a").equals(SlayerTask.of(5, "Hydras").identity("b")));
	}

	private static void head()
	{
		HeadAdvisor.Input i = base("Cerberus", "Hellhounds", HeadAdvisor.Style.MELEE, HELM, NEITIZNOT);
		eq("melee on task", HELM, HeadAdvisor.pick(i).itemId);

		i = base("Cerberus", null, HeadAdvisor.Style.MELEE, HELM, NEITIZNOT);
		i.task = SlayerTask.NONE;
		eq("ADD-07 off task", Reason.MELEE_FALLBACK, HeadAdvisor.pick(i).reason);
		eq("ADD-11 Neitiznot off task", NEITIZNOT, HeadAdvisor.pick(i).itemId);

		i = base("Cerberus", null, HeadAdvisor.Style.MELEE, HELM, NEITIZNOT, FACEGUARD);
		i.task = SlayerTask.NONE;
		eq("ADD-11 faceguard if owned", FACEGUARD, HeadAdvisor.pick(i).itemId);

		i = base("Cerberus", null, HeadAdvisor.Style.MELEE, FACEGUARD, TORVA);
		i.task = SlayerTask.NONE;
		eq("damage focus Torva", TORVA, HeadAdvisor.pick(i).itemId);
		i.focus = HeadAdvisor.Focus.PRAYER;
		eq("prayer focus faceguard", FACEGUARD, HeadAdvisor.pick(i).itemId);

		i = base("Vorkath", "Blue dragons", HeadAdvisor.Style.RANGED, HELM_I, SALVE_EI);
		i.undead = Tri.YES;
		eq("Salve owned but not worn keeps helm", HELM_I, HeadAdvisor.pick(i).itemId);
		i.plannedNeck = SALVE_EI;
		eq("ADD-09 Salve priority", Reason.SALVE_TAKES_PRIORITY, HeadAdvisor.pick(i).reason);

		i = base("Vorkath", "Blue dragons", HeadAdvisor.Style.RANGED, HELM_I);
		i.undead = Tri.YES;
		eq("no Salve keeps helm", HELM_I, HeadAdvisor.pick(i).itemId);

		i = base("Cerberus", "Hellhounds", HeadAdvisor.Style.RANGED, HELM);
		eq("Ranged needs imbue", Reason.NO_USABLE_HELM, HeadAdvisor.pick(i).reason);
		i.atlatl = true;
		eq("atlatl uses plain helm", HELM, HeadAdvisor.pick(i).itemId);

		i = base("Cerberus", "Hellhounds", HeadAdvisor.Style.MELEE, HELM);
		i.insideToa = true;
		eq("Tombs disables helm", Reason.EFFECT_DISABLED_IN_TOA, HeadAdvisor.pick(i).reason);

		i = base("Cerberus", "Hellhounds", HeadAdvisor.Style.RANGED, HELM_I);
		i.setNeedsHeadSlot = true;
		eq("set needs head", Reason.SET_NEEDS_HEAD_SLOT, HeadAdvisor.pick(i).reason);

		i = base("Duke Sucellus", "Duke Sucellus", HeadAdvisor.Style.MELEE, HELM);
		eq("normal Duke on task", HELM, HeadAdvisor.pick(i).itemId);
		i.awakened = true;
		eq("Awakened disables helm", Reason.EFFECT_DISABLED_AWAKENED, HeadAdvisor.pick(i).reason);

		i = base("K'ril Tsutsaroth", "Greater demons", HeadAdvisor.Style.MELEE, HELM);
		eq("unconfirmed task no auto helm", Reason.TASK_UNCONFIRMED, HeadAdvisor.pick(i).reason);
	}

	private static HeadAdvisor.Input base(String boss, String task, HeadAdvisor.Style style, Integer... owned)
	{
		HeadAdvisor.Input in = new HeadAdvisor.Input();
		in.boss = boss;
		in.task = SlayerTask.of(5, task);
		in.style = style;
		in.owned = Set.of(owned);
		return in;
	}

	private static void eq(String name, Object expected, Object actual)
	{
		if (expected.equals(actual))
		{
			pass++;
		}
		else
		{
			fail++;
			System.out.println("FAIL " + name + ": expected " + expected + ", got " + actual);
		}
	}
}
