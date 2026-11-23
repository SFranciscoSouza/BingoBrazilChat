package com.brazilchat.bingo.catalog;

import com.brazilchat.util.Sanitizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Produces the lookup key for an item name.
 *
 * <p>The catalog on the website is Title Case ("Twisted Bow"); the game broadcasts sentence case
 * ("Twisted bow"). Both sides go through this, so the two meet in the middle.
 */
public final class ItemNameNormalizer
{
	/**
	 * Suffixes stripped before lookup. These denote a charge state of the item the raid actually
	 * dropped -- "Scythe of Vitur (uncharged)" is the drop, "Scythe of Vitur" is the catalog entry.
	 *
	 * <p>Deliberately NOT stripped: {@code (or)}, {@code (f)}, {@code (t)}, {@code (b)}. Those mark
	 * upgrades the player <em>built</em> ("Masori body (f)", "Osmumten's fang (or)") and never drop
	 * from a raid. Accepting them would score points for a cosmetic recolour.
	 */
	private static final List<String> CHARGE_SUFFIXES =
		Arrays.asList(" (uncharged)", " (broken)", " (empty)");

	private ItemNameNormalizer()
	{
	}

	public static String normalize(String rawItemName)
	{
		if (rawItemName == null)
		{
			return "";
		}

		// Locale.ROOT is mandatory. RuneLite's Text.standardize() lowercases with the default
		// locale, which on a Turkish-locale machine turns "TWISTED BOW" into "twisted bow" with a
		// dotless i -- and every catalog lookup silently misses.
		String s = Sanitizer.clean(rawItemName).toLowerCase(Locale.ROOT).trim();

		for (String suffix : CHARGE_SUFFIXES)
		{
			if (s.endsWith(suffix))
			{
				// One suffix only: a name never carries two charge states.
				return s.substring(0, s.length() - suffix.length()).trim();
			}
		}

		return s;
	}
}
