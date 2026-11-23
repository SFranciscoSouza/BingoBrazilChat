package com.brazilchat.chat;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BroadcastCategoryTest
{
	private static void expect(BroadcastCategory expected, String line)
	{
		assertEquals(line, expected, BroadcastCategory.classify(line));
	}

	@Test
	public void raidLootAndDrops()
	{
		expect(BroadcastCategory.RAID_LOOT, "Zezinho received special loot from a raid: Twisted bow.");
		expect(BroadcastCategory.DROP,
			"Zezinho received a drop: Voidwaker blade (39,648,370 coins) from Vet'ion.");
	}

	@Test
	public void rareBroadcasts()
	{
		expect(BroadcastCategory.PET,
			"Zezinho has a funny feeling like he's being followed: Pet snakeling at 1,234 kill count.");
		expect(BroadcastCategory.PET,
			"Zezinho has a funny feeling like she would have been followed: Heron at 99 Fishing.");
		expect(BroadcastCategory.COLLECTION_LOG,
			"Zezinho received a new collection log item: Dragon pickaxe (123/1477)");
		expect(BroadcastCategory.MAX_TOTAL,
			"Zezinho has reached the highest possible total level of 2277.");
		assertTrue(BroadcastCategory.PET.isRare());
		assertTrue(BroadcastCategory.RAID_LOOT.isRare());
		assertFalse(BroadcastCategory.DROP.isRare());
	}

	@Test
	public void noisyBroadcasts()
	{
		expect(BroadcastCategory.LEVEL, "Zezinho has reached Attack level 99.");
		expect(BroadcastCategory.LEVEL, "Zezinho has reached a total level of 2000.");
		expect(BroadcastCategory.QUEST, "Zezinho has completed a quest: Dragon Slayer II");
		expect(BroadcastCategory.DIARY, "Zezinho has completed the Hard Ardougne diary.");
		expect(BroadcastCategory.COMBAT_ACHIEVEMENT,
			"Zezinho has unlocked the Elite tier of rewards from Combat Achievements!");
		expect(BroadcastCategory.PERSONAL_BEST,
			"Zezinho has achieved a new Zulrah personal best: 0:58");
		expect(BroadcastCategory.PVP,
			"Zezinho has defeated Fulano and received (12,345 coins) worth of loot!");
		expect(BroadcastCategory.PVP,
			"Zezinho has died and lost their Hardcore Ironman status.");
		expect(BroadcastCategory.MEMBERSHIP, "Zezinho has been invited into the clan by Fulano.");
		expect(BroadcastCategory.MEMBERSHIP, "Zezinho has left the clan.");
	}

	@Test
	public void tagsAreIgnoredAndUnknownIsOther()
	{
		expect(BroadcastCategory.QUEST,
			"<col=ef1020>Zezinho</col> has completed a quest: <col=ef1020>Monkey Madness II</col>");
		expect(BroadcastCategory.OTHER, "Welcome to the clan chat!");
		expect(BroadcastCategory.OTHER, "");
	}
}
