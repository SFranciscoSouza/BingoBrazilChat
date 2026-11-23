package com.brazilchat.clan;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AchievementReaderTest
{
	@Test
	public void diaryLevelCountsUpFromEasyAndStopsAtTheFirstGap()
	{
		assertEquals(0, AchievementReader.diaryLevel(new boolean[]{false, true, true, true}));
		assertEquals(2, AchievementReader.diaryLevel(new boolean[]{true, true, false, true}));
		assertEquals(4, AchievementReader.diaryLevel(new boolean[]{true, true, true, true}));
	}
}
