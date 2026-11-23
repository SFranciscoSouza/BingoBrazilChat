package com.brazilchat.bingo.submit;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * The dedupe key is what stops one clan broadcast becoming N scored drops. Because
 * leaderboard_teams sums points, a key that differs between two clients is not a cosmetic bug --
 * it is a duplicated Twisted Bow worth 20 phantom points.
 */
public class DedupeKeyTest
{
	private static final String EVENT = "11111111-2222-3333-4444-555555555555";
	private static final String ITEM = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

	@Test
	public void isStableForTheSameDrop()
	{
		assertEquals(DedupeKey.of(EVENT, "Zezinho", ITEM), DedupeKey.of(EVENT, "Zezinho", ITEM));
	}

	@Test
	public void ignoresClockDifferencesBetweenClients()
	{
		// There is no timestamp in the key at all, precisely so that clients whose clocks disagree
		// by minutes still agree on the key. This test exists to stop anyone adding one.
		String a = DedupeKey.of(EVENT, "Zezinho", ITEM);
		Thread.yield();
		String b = DedupeKey.of(EVENT, "Zezinho", ITEM);
		assertEquals(a, b);
	}

	@Test
	public void agreesAcrossEverySpellingOfOneName()
	{
		// Twenty clients may render the dropper's name differently -- NBSP, hyphen, case. They must
		// still land on one key, or the herd never collapses.
		String canonical = DedupeKey.of(EVENT, "Fn Junior", ITEM);
		assertEquals(canonical, DedupeKey.of(EVENT, "fn junior", ITEM));
		assertEquals(canonical, DedupeKey.of(EVENT, "FN-JUNIOR", ITEM));
		assertEquals(canonical, DedupeKey.of(EVENT, "Fn_Junior", ITEM));
		assertEquals(canonical, DedupeKey.of(EVENT, " Fn  Junior ", ITEM));
	}

	@Test
	public void differsByPlayer()
	{
		assertNotEquals(DedupeKey.of(EVENT, "Zezinho", ITEM), DedupeKey.of(EVENT, "Someone", ITEM));
	}

	@Test
	public void differsByItem()
	{
		assertNotEquals(DedupeKey.of(EVENT, "Zezinho", ITEM),
			DedupeKey.of(EVENT, "Zezinho", "ffffffff-bbbb-cccc-dddd-eeeeeeeeeeee"));
	}

	@Test
	public void differsByEvent()
	{
		// Otherwise a drop in one event would suppress the same drop in the next one.
		assertNotEquals(DedupeKey.of(EVENT, "Zezinho", ITEM),
			DedupeKey.of("99999999-2222-3333-4444-555555555555", "Zezinho", ITEM));
	}

	@Test
	public void isLowercaseHexOfTheRightLength()
	{
		// The server validates this shape, so a change to the digest would be rejected there.
		String key = DedupeKey.of(EVENT, "Zezinho", ITEM);
		assertEquals(64, key.length());
		assertTrue(key, key.matches("^[0-9a-f]{64}$"));
	}
}
