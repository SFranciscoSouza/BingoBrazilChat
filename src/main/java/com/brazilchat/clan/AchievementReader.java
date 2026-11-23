package com.brazilchat.clan;

import com.brazilchat.clan.api.ClanDtos;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;

/**
 * Reads the local player's own progress for the clan's rank ladder: total level, max cape, the
 * achievement diary tier done in every area, and the Combat Achievement tier. Only ever the
 * player's own account, from their own client -- never anyone else's.
 */
@Singleton
public class AchievementReader
{
	/** Per area: easy, medium, hard, elite completion varbits. Karamja's first three differ. */
	private static final int[][] DIARIES = {
		{VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE, VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE, VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE, VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE},
		{VarbitID.DESERT_DIARY_EASY_COMPLETE, VarbitID.DESERT_DIARY_MEDIUM_COMPLETE, VarbitID.DESERT_DIARY_HARD_COMPLETE, VarbitID.DESERT_DIARY_ELITE_COMPLETE},
		{VarbitID.FALADOR_DIARY_EASY_COMPLETE, VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE, VarbitID.FALADOR_DIARY_HARD_COMPLETE, VarbitID.FALADOR_DIARY_ELITE_COMPLETE},
		{VarbitID.FREMENNIK_DIARY_EASY_COMPLETE, VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE, VarbitID.FREMENNIK_DIARY_HARD_COMPLETE, VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE},
		{VarbitID.KANDARIN_DIARY_EASY_COMPLETE, VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE, VarbitID.KANDARIN_DIARY_HARD_COMPLETE, VarbitID.KANDARIN_DIARY_ELITE_COMPLETE},
		{VarbitID.ATJUN_EASY_DONE, VarbitID.ATJUN_MED_DONE, VarbitID.ATJUN_HARD_DONE, VarbitID.KARAMJA_DIARY_ELITE_COMPLETE},
		{VarbitID.KOUREND_DIARY_EASY_COMPLETE, VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE, VarbitID.KOUREND_DIARY_HARD_COMPLETE, VarbitID.KOUREND_DIARY_ELITE_COMPLETE},
		{VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE, VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE, VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE, VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE},
		{VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE, VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE},
		{VarbitID.VARROCK_DIARY_EASY_COMPLETE, VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE, VarbitID.VARROCK_DIARY_HARD_COMPLETE, VarbitID.VARROCK_DIARY_ELITE_COMPLETE},
		{VarbitID.WESTERN_DIARY_EASY_COMPLETE, VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE, VarbitID.WESTERN_DIARY_HARD_COMPLETE, VarbitID.WESTERN_DIARY_ELITE_COMPLETE},
		{VarbitID.WILDERNESS_DIARY_EASY_COMPLETE, VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE, VarbitID.WILDERNESS_DIARY_HARD_COMPLETE, VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE},
	};

	private static final int[] KARAMJA_LEGACY = {VarbitID.ATJUN_EASY_DONE, VarbitID.ATJUN_MED_DONE, VarbitID.ATJUN_HARD_DONE};

	private static final int[] CA_TIERS = {
		VarbitID.CA_TIER_STATUS_EASY, VarbitID.CA_TIER_STATUS_MEDIUM, VarbitID.CA_TIER_STATUS_HARD,
		VarbitID.CA_TIER_STATUS_ELITE, VarbitID.CA_TIER_STATUS_MASTER, VarbitID.CA_TIER_STATUS_GRANDMASTER,
	};

	private static final String[] DIARY_LEVELS = {"none", "easy", "medium", "hard", "elite"};

	/** True for the diary and Combat Achievement varbits this class reads. */
	public static boolean isTracked(int varbitId)
	{
		for (int[] area : DIARIES)
		{
			for (int v : area)
			{
				if (v == varbitId)
				{
					return true;
				}
			}
		}
		for (int v : CA_TIERS)
		{
			if (v == varbitId)
			{
				return true;
			}
		}
		return false;
	}

	private final Client client;

	@Inject
	AchievementReader(Client client)
	{
		this.client = client;
	}

	/** Client thread only. Null when not logged in. */
	@Nullable
	public ClanDtos.Achievements read()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}

		// Right after login skills and varbits may not be loaded yet; a total of 0 means "not
		// yet", not "level 3". Report nothing rather than a wrong zero.
		int total = client.getTotalLevel();
		if (total < Skill.values().length)
		{
			return null;
		}

		boolean maxed = true;
		for (Skill skill : Skill.values())
		{
			if (client.getRealSkillLevel(skill) < 99)
			{
				maxed = false;
				break;
			}
		}

		boolean[] tierDone = new boolean[4];
		for (int tier = 0; tier < 4; tier++)
		{
			boolean all = true;
			for (int[] area : DIARIES)
			{
				if (!isDiaryDone(area[tier]))
				{
					all = false;
					break;
				}
			}
			tierDone[tier] = all;
		}

		int caTier = 0;
		for (int i = 0; i < CA_TIERS.length; i++)
		{
			if (client.getVarbitValue(CA_TIERS[i]) > 0)
			{
				caTier = i + 1;
			}
			else
			{
				break;
			}
		}

		return new ClanDtos.Achievements(total, maxed,
			DIARY_LEVELS[diaryLevel(tierDone)], caTier);
	}

	/** The highest tier completed in every area, counting up from easy: 0 none .. 4 elite. */
	static int diaryLevel(boolean[] tierDone)
	{
		int level = 0;
		for (boolean done : tierDone)
		{
			if (!done)
			{
				break;
			}
			level++;
		}
		return level;
	}

	private boolean isDiaryDone(int varbit)
	{
		int value = client.getVarbitValue(varbit);
		// Karamja's easy/medium/hard predate the other diaries and use 2 for "complete".
		for (int legacy : KARAMJA_LEGACY)
		{
			if (legacy == varbit)
			{
				return value >= 2;
			}
		}
		return value >= 1;
	}
}
