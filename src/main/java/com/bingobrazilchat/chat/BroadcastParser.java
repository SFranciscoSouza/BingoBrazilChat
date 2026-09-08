package com.bingobrazilchat.chat;

import com.bingobrazilchat.util.RsnUtil;
import com.bingobrazilchat.util.Sanitizer;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * Turns a raw clan chat line into a {@link ParsedBroadcast}, or nothing.
 *
 * <p>Pure: no RuneLite types, no state, no I/O. That is deliberate -- broadcast wording is the part
 * of this plugin most likely to be wrong, and keeping the parser dependency-free means it can be
 * exercised against real strings in a plain unit test instead of a running game client.
 */
public final class BroadcastParser
{
	private BroadcastParser()
	{
	}

	public static Optional<ParsedBroadcast> parse(String rawMessage)
	{
		final String line = Sanitizer.clean(rawMessage);
		if (line.isEmpty())
		{
			return Optional.empty();
		}

		Matcher raid = BroadcastPatterns.RAID_SPECIAL_LOOT.matcher(line);
		if (raid.matches())
		{
			return build(BroadcastKind.RAID_SPECIAL_LOOT, raid.group("rsn"), raid.group("item"),
				raid.group("value"), null, null, line);
		}

		Matcher drop = BroadcastPatterns.VALUE_DROP.matcher(line);
		if (drop.matches())
		{
			return build(BroadcastKind.VALUE_DROP, drop.group("rsn"), drop.group("item"),
				drop.group("value"), drop.group("qty"), drop.group("source"), line);
		}

		return Optional.empty();
	}

	/**
	 * True when a line looks like a drop announcement we failed to parse. Used only to decide what
	 * to write to the debug log, so that a wording mismatch shows up as a log line during testing
	 * rather than as a silent no-op during the event.
	 */
	public static boolean looksLikeUnparsedDrop(String rawMessage)
	{
		String line = Sanitizer.clean(rawMessage);
		return line.contains(BroadcastPatterns.RECEIVED_HINT) && !parse(rawMessage).isPresent();
	}

	private static Optional<ParsedBroadcast> build(BroadcastKind kind, String rsn, String item,
		String value, String qty, String source, String line)
	{
		String cleanRsn = rsn == null ? "" : rsn.trim();
		String cleanItem = item == null ? "" : item.trim();

		// A name the server would reject anyway is dropped here, before it can reach the queue.
		// This also filters out lines that structurally matched but captured nonsense, which is the
		// usual failure mode of a lazy leading group.
		if (cleanItem.isEmpty() || !RsnUtil.isValid(cleanRsn))
		{
			return Optional.empty();
		}

		return Optional.of(new ParsedBroadcast(
			kind, cleanRsn, cleanItem, parseNumber(value, -1L), (int) parseNumber(qty, 1L),
			source == null ? null : source.trim(), line));
	}

	private static long parseNumber(String withCommas, long fallback)
	{
		if (withCommas == null || withCommas.isEmpty())
		{
			return fallback;
		}
		try
		{
			return Long.parseLong(withCommas.replace(",", ""));
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}
}
