package gg.feral.burrow.runelite;

import gg.feral.burrow.bib.SlayerTask;
import gg.feral.burrow.bib.SlayerTaskResolver;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * RuneLite side of {@link SlayerTaskResolver}. Call on the client thread.
 * Compiled only against a stub of Client with RuneLite's real constants; never run in the client.
 */
public final class SlayerTaskReader
{
	private SlayerTaskReader()
	{
	}

	/** Cheap value that changes when the task, master or area changes, or when a task starts or ends. */
	public static long changeKey(Client client)
	{
		long key = client.getVarpValue(VarPlayerID.SLAYER_COUNT) > 0 ? 1 : 0;
		key = key * 1009 + client.getVarpValue(VarPlayerID.SLAYER_TARGET);
		key = key * 1009 + client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID);
		key = key * 1009 + client.getVarbitValue(VarbitID.SLAYER_MASTER);
		return key * 1009 + client.getVarpValue(VarPlayerID.SLAYER_AREA);
	}

	public static SlayerTask read(Client client)
	{
		return SlayerTaskResolver.resolve(new SlayerTaskResolver.Source()
		{
			@Override
			public int count()
			{
				return client.getVarpValue(VarPlayerID.SLAYER_COUNT);
			}

			@Override
			public int target()
			{
				return client.getVarpValue(VarPlayerID.SLAYER_TARGET);
			}

			@Override
			public int bossId()
			{
				return client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID);
			}

			@Override
			public int master()
			{
				return client.getVarbitValue(VarbitID.SLAYER_MASTER);
			}

			@Override
			public int areaId()
			{
				return client.getVarpValue(VarPlayerID.SLAYER_AREA);
			}

			@Override
			public List<Integer> taskRows(int target)
			{
				return client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, target);
			}

			@Override
			public List<Integer> bossRows(int bossId)
			{
				return client.getDBRowsByValue(DBTableID.SlayerTaskSublist.ID, DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID, 0, bossId);
			}

			@Override
			public Object[] bossTaskField(int row)
			{
				return client.getDBTableField(row, DBTableID.SlayerTaskSublist.COL_TASK, 0);
			}

			@Override
			public Object[] taskNameField(int row)
			{
				return client.getDBTableField(row, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0);
			}

			@Override
			public List<Integer> areaRows(int areaId)
			{
				return client.getDBRowsByValue(DBTableID.SlayerArea.ID, DBTableID.SlayerArea.COL_AREA_ID, 0, areaId);
			}

			@Override
			public Object[] areaNameField(int row)
			{
				return client.getDBTableField(row, DBTableID.SlayerArea.COL_AREA_NAME_IN_HELPER, 0);
			}
		});
	}
}
