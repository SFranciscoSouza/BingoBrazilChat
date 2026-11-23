package com.brazilchat.clan;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RankLadderTest
{
	@Test
	public void inGameTitlesMapToTheClanNames()
	{
		assertEquals("Recruta", RankLadder.label("Recruit"));
		assertEquals("Soldado", RankLadder.label("Corporal"));
		assertEquals("Sargento", RankLadder.label("Sergeant"));
		assertEquals("Cadete", RankLadder.label("Cadet"));
		assertEquals("Tenente", RankLadder.label("Lieutenant"));
		assertEquals("Capitão", RankLadder.label("Captain"));
		assertEquals("General", RankLadder.label("General"));
		assertEquals("Endgame", RankLadder.label("Colonel"));
		assertEquals("Maxed", RankLadder.label("Brigadier"));
		assertEquals("GM", RankLadder.label("Marshal"));
		assertEquals("MVP PvM", RankLadder.label("Infantry"));
		assertEquals("MVP Skiller", RankLadder.label("Skiller"));
		assertEquals("Deputy Owner", RankLadder.label("Deputy Owner"));
	}

	@Test
	public void promotionFollowsTheLadderNotTheRankNumber()
	{
		assertTrue(RankLadder.isPromotion("Recruit", 30, "Corporal", 10));
		assertFalse(RankLadder.isPromotion("Corporal", 10, "Recruit", 30));
		assertTrue(RankLadder.isPromotion("Captain", 40, "Infantry", 20));
		assertFalse(RankLadder.isPromotion("Fire", 50, "Captain", 40));
		assertTrue(RankLadder.isPromotion("Marshal", 90, "Administrator", 100));
		assertFalse(RankLadder.isPromotion("Captain", 40, "Captain", 40));
	}

	@Test
	public void nextRankChecklistUsesTheLocalPlayersData()
	{
		RankLadder.Rank next = RankLadder.next("Cadet");
		assertEquals("Tenente", next.getLabel());
		List<RankLadder.Check> checks = RankLadder.checks(next,
			new RankLadder.Facts(1950, false, 2, 1, 20));
		assertEquals(4, checks.size());
		assertTrue(checks.get(0).getOk());   // 1900 total
		assertFalse(checks.get(1).getOk());  // all hard diaries (has medium)
		assertTrue(checks.get(2).getOk());   // easy CA
		assertFalse(checks.get(3).getOk());  // 30 days (has 20)
	}

	@Test
	public void finishingTheLastDiaryMakesThePlayerReady()
	{
		java.time.LocalDate today = java.time.LocalDate.of(2026, 11, 23);
		java.time.LocalDate joined = java.time.LocalDate.of(2026, 9, 1);
		RankLadder.Rank tenente = RankLadder.next("Cadet");

		com.brazilchat.clan.api.ClanDtos.Achievements before =
			new com.brazilchat.clan.api.ClanDtos.Achievements(1950, false, "medium", 1);
		com.brazilchat.clan.api.ClanDtos.Achievements after =
			new com.brazilchat.clan.api.ClanDtos.Achievements(1950, false, "hard", 1);

		assertFalse(RankLadder.meetsAll(tenente, RankLadder.facts(before, joined, today)));
		assertTrue(RankLadder.meetsAll(tenente, RankLadder.facts(after, joined, today)));
		// Without a join date the 30-day rule is unknown, so no "ready" notice yet.
		assertFalse(RankLadder.meetsAll(tenente, RankLadder.facts(after, null, today)));
		assertNull(RankLadder.facts(null, joined, today));
	}

	@Test
	public void hellcatIsAManualTitle()
	{
		assertTrue(RankLadder.isSpecial("Hellcat"));
		assertNull(RankLadder.next("Hellcat"));
	}

	@Test
	public void unknownJoinDateIsNotAFailureAndTheTopHasNoNext()
	{
		List<RankLadder.Check> checks = RankLadder.checks(RankLadder.next("Captain"),
			new RankLadder.Facts(2376, true, 4, 6, null));
		assertNull(checks.get(checks.size() - 1).getOk());
		assertNull(RankLadder.next("Marshal"));
		assertNull(RankLadder.next("Beast"));
	}
}
