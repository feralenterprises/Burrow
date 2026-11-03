package gg.feral.burrow.bib;

import java.util.Objects;

/** Immutable task snapshot. UNKNOWN is distinct from NONE, and both plan as off task. */
public final class SlayerTask
{
	public enum State
	{
		UNKNOWN, NONE, ACTIVE
	}

	/** Values of the Slayer master variable that restrict where kills count. */
	public static final int MASTER_KRYSTILIA = 7;
	public static final int MASTER_KONAR = 8;

	public static final SlayerTask UNKNOWN = new SlayerTask(State.UNKNOWN, null, 0, null);
	public static final SlayerTask NONE = new SlayerTask(State.NONE, null, 0, null);

	public final State state;
	public final String taskName;
	public final int master;
	public final String area;

	private SlayerTask(State state, String taskName, int master, String area)
	{
		this.state = state;
		this.taskName = taskName;
		this.master = master;
		this.area = area;
	}

	/**
	 * @param remaining value of the Slayer count variable, or null if it has not been read
	 * @param taskName  resolved task name, or null if it could not be resolved
	 */
	public static SlayerTask of(Integer remaining, String taskName)
	{
		return of(remaining, taskName, 0, null);
	}

	/**
	 * @param master value of the Slayer master variable; it is only meaningful while a task is active
	 * @param area   assigned area name, or null when none could be resolved
	 */
	public static SlayerTask of(Integer remaining, String taskName, int master, String area)
	{
		if (remaining == null)
		{
			return UNKNOWN;
		}
		if (remaining <= 0)
		{
			return NONE;
		}
		return taskName == null || taskName.trim().isEmpty() ? UNKNOWN : new SlayerTask(State.ACTIVE, taskName.trim(), master, area);
	}

	/** Include this in plan identity so a finished or changed task, or another account, invalidates the plan. */
	public String identity(String accountScope)
	{
		return Objects.toString(accountScope, "") + ":" + state + ":" + Objects.toString(taskName, "") + ":" + master + ":"
			+ Objects.toString(area, "");
	}
}
