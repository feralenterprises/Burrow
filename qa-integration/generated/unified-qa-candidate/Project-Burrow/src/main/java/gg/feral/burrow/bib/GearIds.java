package gg.feral.burrow.bib;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Item ID groups. Generated once; game item IDs only, no stats. Verify against the RuneLite ItemID constants. */
final class GearIds
{
	static final Set<Integer> SLAYER_HELM_PLAIN = set(11864, 19639, 19643, 19647, 21264, 21888, 23073, 24370, 25898, 25904, 25910, 29816, 33066, 33338, 33340);
	static final Set<Integer> SLAYER_HELM_IMBUED = set(11865, 19641, 19645, 19649, 21266, 21890, 23075, 24444, 25177, 25179, 25181, 25183, 25185, 25187, 25189, 25191, 25900, 25902, 25906, 25908, 25912, 25914, 26674, 26675, 26676, 26677, 26678, 26679, 26680, 26681, 26682, 26683, 26684, 29818, 29820, 29822, 33068, 33070, 33072, 33439, 33441, 33443, 33445, 33447, 33449);
	static final Set<Integer> BLACK_MASK_PLAIN = set(8901, 8903, 8905, 8907, 8909, 8911, 8913, 8915, 8917, 8919, 8921);
	static final Set<Integer> BLACK_MASK_IMBUED = set(11774, 11775, 11776, 11777, 11778, 11779, 11780, 11781, 11782, 11783, 11784, 25266, 25267, 25268, 25269, 25270, 25271, 25272, 25273, 25274, 25275, 25276, 26771, 26772, 26773, 26774, 26775, 26776, 26777, 26778, 26779, 26780, 26781);
	static final Set<Integer> SALVE_MELEE_ONLY = set(4081, 10588);
	static final Set<Integer> SALVE_IMBUED = set(12017, 12018, 25250, 25278, 26763, 26782);
	static final Set<Integer> DIZANA_ITEMS = set(28902, 28906, 28947, 28949, 28951, 28953, 28955, 28957);
	static final Set<Integer> ASSEMBLER_BASE = set(22109, 24222);
	static final Set<Integer> ASSEMBLER_VARIANTS = set(21898, 24135, 27363, 27365, 27374, 27376);
	static final Set<Integer> ACCUMULATOR_BASE = set(10499);
	static final Set<Integer> ACCUMULATOR_VARIANTS = set(13337);
	static final Set<Integer> ATTRACTOR = set(10498);
	static final Set<Integer> RANGING_CAPE = set(9756, 9757);
	static final Set<Integer> NEITIZNOT_FACEGUARD = set(24271);
	static final Set<Integer> HELM_OF_NEITIZNOT = set(10828, 28070);
	static final Set<Integer> TORVA_HELM = set(26382, 28254);

	private GearIds()
	{
	}

	private static Set<Integer> set(Integer... ids)
	{
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ids)));
	}
}
