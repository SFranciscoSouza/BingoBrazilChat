package com.brazilchat.chat;

import java.util.regex.Pattern;

/**
 * The broadcast templates, kept in one place so a wording change is a single-file edit.
 *
 * <p>RuneLite itself does not parse clan drop broadcasts -- the client source has no pattern for
 * "special loot" or "received a drop" anywhere (its {@code ScreenshotPlugin} only handles the local
 * {@code Valuable drop:} / {@code Untradeable drop:} game messages). So these are ours, written
 * from observed broadcast shapes.
 *
 * <p>Both patterns run <em>after</em> {@link com.brazilchat.util.Sanitizer#clean(String)}.
 */
public final class BroadcastPatterns
{
	/**
	 * "{@code Zezinho received special loot from a raid: Kodai insignia.}"
	 *
	 * <p>The trailing full stop is optional -- both forms occur in the wild. The optional trailing
	 * {@code (1,234,567)} group is defensive: sources disagree on whether this line carries a GE
	 * value. It is safe to include because it accepts only digits and commas, so it can never eat a
	 * legitimate {@code (uncharged)} suffix off the item name.
	 */
	public static final Pattern RAID_SPECIAL_LOOT = Pattern.compile(
		"^(?<rsn>.+?) received special loot from a raid: (?<item>.+?)"
			+ "(?: \\((?<value>[\\d,]+)\\))?\\.?$");

	/**
	 * "{@code Zezinho received a drop: Voidwaker blade (39,648,370 coins) from Vet'ion.}"
	 *
	 * <p>The {@code from <Source>} clause is not optional to <em>handle</em>: without it the lazy
	 * item group swallows "{@code (39,648,370 coins) from Vet'ion}" whole and the catalog lookup
	 * fails silently.
	 */
	public static final Pattern VALUE_DROP = Pattern.compile(
		"^(?<rsn>.+?) received a drop: "
			+ "(?:(?<qty>[\\d,]+) x )?"
			+ "(?<item>.+?)"
			+ "(?: \\((?<value>[\\d,]+) coins\\))?"
			+ "(?: from (?<source>.+?))?\\.?$");

	/**
	 * Cheap pre-filter for the debug log: any clan line mentioning a receipt that matched neither
	 * pattern above is worth surfacing, because it is probably a template we got wrong.
	 */
	public static final String RECEIVED_HINT = " received ";

	private BroadcastPatterns()
	{
	}
}
