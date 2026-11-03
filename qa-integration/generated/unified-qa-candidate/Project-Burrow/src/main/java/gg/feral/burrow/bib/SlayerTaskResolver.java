package gg.feral.burrow.bib;

import java.util.List;

/**
 * Turns raw Slayer values into a task. Pure Java so bad data can be tested offline. Anything missing, malformed or
 * thrown during a login or task transition gives UNKNOWN, which never grants an on-task bonus.
 */
public final class SlayerTaskResolver
{
	/** "Bosses" target value, from RuneLite's SlayerPlugin. */
	public static final int BOSS_TASK_TARGET = 98;

	/** The client reads the resolver needs. Implemented by the RuneLite host. */
	public interface Source
	{
		int count();

		int target();

		int bossId();

		int master();

		int areaId();

		List<Integer> taskRows(int target);

		List<Integer> bossRows(int bossId);

		Object[] bossTaskField(int row);

		Object[] taskNameField(int row);

		List<Integer> areaRows(int areaId);

		Object[] areaNameField(int row);
	}

	private SlayerTaskResolver()
	{
	}

	public static SlayerTask resolve(Source source)
	{
		try
		{
			int remaining = source.count();
			if (remaining <= 0)
			{
				return SlayerTask.NONE;
			}
			int target = source.target();
			Integer row = target == BOSS_TASK_TARGET ? bossTaskRow(source) : first(source.taskRows(target));
			if (row == null)
			{
				return SlayerTask.UNKNOWN;
			}
			String name = text(source.taskNameField(row));
			if (name == null)
			{
				return SlayerTask.UNKNOWN;
			}
			int master = source.master();
			return SlayerTask.of(remaining, name, master, master == SlayerTask.MASTER_KONAR ? area(source) : null);
		}
		catch (RuntimeException transientRead)
		{
			return SlayerTask.UNKNOWN;
		}
	}

	private static Integer bossTaskRow(Source source)
	{
		Integer row = first(source.bossRows(source.bossId()));
		return row == null ? null : number(source.bossTaskField(row));
	}

	private static String area(Source source)
	{
		int areaId = source.areaId();
		if (areaId <= 0)
		{
			return null;
		}
		Integer row = first(source.areaRows(areaId));
		return row == null ? null : text(source.areaNameField(row));
	}

	private static Integer first(List<Integer> rows)
	{
		return rows == null || rows.isEmpty() ? null : rows.get(0);
	}

	private static Integer number(Object[] field)
	{
		return field != null && field.length > 0 && field[0] instanceof Integer ? (Integer) field[0] : null;
	}

	private static String text(Object[] field)
	{
		return field != null && field.length > 0 && field[0] instanceof String ? (String) field[0] : null;
	}
}
