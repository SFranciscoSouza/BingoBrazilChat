package com.brazilchat.chat;

import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Broadcast wording is the part of this plugin most likely to be wrong, and a mismatch fails
 * silently (a drop simply never scores). These cases pin down every shape observed in the wild.
 */
public class BroadcastParserTest
{
	private static ParsedBroadcast parsed(String line)
	{
		Optional<ParsedBroadcast> p = BroadcastParser.parse(line);
		assertTrue("expected a match for: " + line, p.isPresent());
		return p.get();
	}

	@Test
	public void parsesRaidSpecialLoot()
	{
		ParsedBroadcast p = parsed("Zezinho received special loot from a raid: Twisted bow.");
		assertEquals(BroadcastKind.RAID_SPECIAL_LOOT, p.getKind());
		assertEquals("Zezinho", p.getRsn());
		assertEquals("Twisted bow", p.getItemName());
	}

	@Test
	public void parsesRaidSpecialLootWithoutTrailingPeriod()
	{
		// Both forms occur; the corpus has "Tumeken's shadow (uncharged)" with no full stop.
		ParsedBroadcast p = parsed(
			"Zezinho Two received special loot from a raid: Tumeken's shadow (uncharged)");
		assertEquals("Zezinho Two", p.getRsn());
		assertEquals("Tumeken's shadow (uncharged)", p.getItemName());
	}

	@Test
	public void chargeSuffixSurvivesParsingAndIsNotEatenByTheValueGroup()
	{
		// The optional (value) group accepts digits and commas only, precisely so it can never
		// swallow a legitimate parenthetical like "(uncharged)".
		ParsedBroadcast p = parsed(
			"Zezinho received special loot from a raid: Scythe of Vitur (uncharged).");
		assertEquals("Scythe of Vitur (uncharged)", p.getItemName());
		assertEquals(-1L, p.getValueGp());
	}

	@Test
	public void parsesRaidSpecialLootCarryingAValue()
	{
		// Sources disagree on whether this line carries a GE value, so we accept both.
		ParsedBroadcast p = parsed(
			"Karambtwo received special loot from a raid: Twisted bow (1,680,988,483)");
		assertEquals("Twisted bow", p.getItemName());
		assertEquals(1_680_988_483L, p.getValueGp());
	}

	@Test
	public void stripsColourTags()
	{
		ParsedBroadcast p = parsed(
			"<col=ef20ff>Zezinho received special loot from a raid: Kodai insignia.</col>");
		assertEquals("Zezinho", p.getRsn());
		assertEquals("Kodai insignia", p.getItemName());
	}

	@Test
	public void handlesNonBreakingSpaceInTwoWordName()
	{
		// OSRS renders display names with U+00A0. Without the fold in Sanitizer, this never matches.
		ParsedBroadcast p = parsed(
			"Fn Junior received special loot from a raid: Elidinis' ward.");
		assertEquals("Fn Junior", p.getRsn());
		assertEquals("Elidinis' ward", p.getItemName());
	}

	@Test
	public void handlesTypographicApostrophe()
	{
		ParsedBroadcast p = parsed(
			"Zezinho received special loot from a raid: Osmumten’s fang.");
		assertEquals("Osmumten's fang", p.getItemName());
	}

	@Test
	public void stripsMetadataPrefixHiddenBehindAColourSpan()
	{
		ParsedBroadcast p = parsed(
			"<col=ef20ff>CA_ID:112|Zezinho received special loot from a raid: Dragon claws.</col>");
		assertEquals("Zezinho", p.getRsn());
		assertEquals("Dragon claws", p.getItemName());
	}

	@Test
	public void parsesValueDropWithCoinsAndSource()
	{
		// Without handling the "from <Source>" clause, the lazy item group swallows the whole tail.
		ParsedBroadcast p = parsed(
			"Zezinho received a drop: Voidwaker blade (39,648,370 coins) from Vet'ion.");
		assertEquals(BroadcastKind.VALUE_DROP, p.getKind());
		assertEquals("Voidwaker blade", p.getItemName());
		assertEquals(39_648_370L, p.getValueGp());
		assertEquals("Vet'ion", p.getSource());
	}

	@Test
	public void parsesValueDropWithQuantity()
	{
		ParsedBroadcast p = parsed("Zezinho received a drop: 587 x Cannonball (111,530 coins).");
		assertEquals("Cannonball", p.getItemName());
		assertEquals(587, p.getQuantity());
	}

	@Test
	public void parsesValueDropWithoutSource()
	{
		ParsedBroadcast p = parsed("Zezinho received a drop: Abyssal whip (1,456,814 coins).");
		assertEquals("Abyssal whip", p.getItemName());
		assertEquals(null, p.getSource());
		assertEquals(1, p.getQuantity());
	}

	@Test
	public void rejectsUnrelatedLines()
	{
		assertFalse(BroadcastParser.parse("Zezinho has logged in.").isPresent());
		assertFalse(BroadcastParser.parse("").isPresent());
		assertFalse(BroadcastParser.parse(null).isPresent());
	}

	@Test
	public void rejectsAnImpossiblyLongName()
	{
		// A greedy leading group would happily match rubbish here; the RSN validity check is what
		// stops a malformed line becoming a submission.
		assertFalse(BroadcastParser.parse(
			"This is far too long to be a name received a drop: Twisted bow.").isPresent());
	}

	@Test
	public void flagsUnparsedDropLookalikesForTheDebugLog()
	{
		assertTrue(BroadcastParser.looksLikeUnparsedDrop(
			"Zezinho received something entirely new: Twisted bow."));
		assertFalse(BroadcastParser.looksLikeUnparsedDrop(
			"Zezinho received special loot from a raid: Twisted bow."));
		assertFalse(BroadcastParser.looksLikeUnparsedDrop("Zezinho has logged in."));
	}
}
