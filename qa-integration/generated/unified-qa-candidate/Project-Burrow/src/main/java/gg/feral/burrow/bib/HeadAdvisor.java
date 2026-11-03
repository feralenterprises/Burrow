package gg.feral.burrow.bib;

import java.util.Set;

/** Decides whether the Slayer helm applies, and orders the melee head fallback by focus. */
public final class HeadAdvisor
{
	public enum Style
	{
		MELEE, RANGED, MAGIC
	}

	public enum Focus
	{
		DAMAGE, PRAYER
	}

	public enum Reason
	{
		HELM_ON_TASK, OFF_TASK, TASK_UNCONFIRMED, NO_USABLE_HELM, SALVE_TAKES_PRIORITY, SET_NEEDS_HEAD_SLOT,
		EFFECT_DISABLED_IN_TOA, EFFECT_DISABLED_AWAKENED, MELEE_FALLBACK, DEFER_TO_PLAN
	}

	public static final class Input
	{
		public Style style = Style.MELEE;
		public Focus focus = Focus.DAMAGE;
		public Set<Integer> owned = Set.of();
		public String boss;
		public SlayerTask task = SlayerTask.UNKNOWN;
		public RangedCapeAdvisor.Tri undead = RangedCapeAdvisor.Tri.UNKNOWN;
		/** Neck item chosen for this plan, or -1. Owning Salve is not enough; it must be worn. */
		public int plannedNeck = -1;
		public boolean insideToa;
		/** Awakened Desert Treasure II fight; the Wiki says the on-task boost does not work there. CP8 has no such method yet. */
		public boolean awakened;
		public boolean setNeedsHeadSlot;
		/** The Eclipse atlatl uses melee bonuses, so an unimbued helm counts for it. */
		public boolean atlatl;
	}

	public static final class Pick
	{
		public final int itemId;
		public final Reason reason;

		Pick(int itemId, Reason reason)
		{
			this.itemId = itemId;
			this.reason = reason;
		}
	}

	private HeadAdvisor()
	{
	}

	public static boolean isSlayerHead(int itemId)
	{
		return GearIds.SLAYER_HELM_IMBUED.contains(itemId) || GearIds.SLAYER_HELM_PLAIN.contains(itemId)
			|| GearIds.BLACK_MASK_IMBUED.contains(itemId) || GearIds.BLACK_MASK_PLAIN.contains(itemId);
	}

	public static Pick pick(Input in)
	{
		Reason blocked = helmBlock(in);
		int helm = usableHelm(in);
		if (blocked == null && helm > 0)
		{
			return new Pick(helm, Reason.HELM_ON_TASK);
		}
		Reason reason = blocked != null ? blocked : Reason.NO_USABLE_HELM;
		if (in.style != Style.MELEE)
		{
			return new Pick(-1, reason == Reason.OFF_TASK ? Reason.DEFER_TO_PLAN : reason);
		}
		int fallback = meleeFallback(in);
		return fallback > 0 ? new Pick(fallback, reason == Reason.OFF_TASK ? Reason.MELEE_FALLBACK : reason)
			: new Pick(-1, reason);
	}

	private static Reason helmBlock(Input in)
	{
		switch (BossTaskMatrix.coverage(in.boss, in.task))
		{
			case NOT_COVERED:
				return Reason.OFF_TASK;
			case UNCONFIRMED:
				return Reason.TASK_UNCONFIRMED;
			default:
				break;
		}
		if (in.insideToa)
		{
			return Reason.EFFECT_DISABLED_IN_TOA;
		}
		if (in.awakened)
		{
			return Reason.EFFECT_DISABLED_AWAKENED;
		}
		if (in.undead == RangedCapeAdvisor.Tri.YES && salveInPlan(in))
		{
			return Reason.SALVE_TAKES_PRIORITY;
		}
		return in.setNeedsHeadSlot ? Reason.SET_NEEDS_HEAD_SLOT : null;
	}

	private static boolean salveInPlan(Input in)
	{
		boolean melee = in.style == Style.MELEE || in.atlatl;
		return GearIds.SALVE_IMBUED.contains(in.plannedNeck) || (melee && GearIds.SALVE_MELEE_ONLY.contains(in.plannedNeck));
	}

	private static int usableHelm(Input in)
	{
		int imbued = first(in.owned, GearIds.SLAYER_HELM_IMBUED, GearIds.BLACK_MASK_IMBUED);
		if (imbued > 0)
		{
			return imbued;
		}
		boolean melee = in.style == Style.MELEE || in.atlatl;
		return melee ? first(in.owned, GearIds.SLAYER_HELM_PLAIN, GearIds.BLACK_MASK_PLAIN) : -1;
	}

	private static int meleeFallback(Input in)
	{
		return in.focus == Focus.PRAYER
			? first(in.owned, GearIds.NEITIZNOT_FACEGUARD, GearIds.HELM_OF_NEITIZNOT, GearIds.TORVA_HELM)
			: first(in.owned, GearIds.TORVA_HELM, GearIds.NEITIZNOT_FACEGUARD, GearIds.HELM_OF_NEITIZNOT);
	}

	@SafeVarargs
	private static int first(Set<Integer> owned, Set<Integer>... groups)
	{
		for (Set<Integer> group : groups)
		{
			int id = owned.stream().filter(group::contains).sorted().findFirst().orElse(-1);
			if (id > 0)
			{
				return id;
			}
		}
		return -1;
	}
}
