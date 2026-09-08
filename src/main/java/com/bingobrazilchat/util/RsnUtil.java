package com.bingobrazilchat.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * RSN handling, split deliberately into two operations that must never be confused.
 *
 * <p>{@link #normalizeForCompare(String)} produces the key used for roster lookups and for the
 * dedupe hash. It has to match the server's rule exactly, or every client computes a different
 * dedupe key and the herd never collapses into one scored drop.
 *
 * <p>The RSN itself is sent to the server <em>unmodified</em>. Notably we do not use RuneLite's
 * {@code Text.toJagexName}, which replaces {@code -} and {@code _} with spaces: the
 * {@code players.rsn} column allows both characters, so that would corrupt real names.
 */
public final class RsnUtil
{
	private static final Pattern SEPARATORS = Pattern.compile("[-_ ]");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	/** Mirrors the RSN rule the server validates with, so junk is rejected before any network call. */
	private static final Pattern VALID_RSN = Pattern.compile("^[a-zA-Z0-9 _-]{1,12}$");

	private RsnUtil()
	{
	}

	public static String normalizeForCompare(String rsn)
	{
		if (rsn == null)
		{
			return "";
		}
		String s = SEPARATORS.matcher(rsn).replaceAll(" ");
		s = WHITESPACE.matcher(s).replaceAll(" ").trim();
		// Locale.ROOT, not the default locale: on a Turkish-locale machine the default maps 'I' to
		// the dotless 'i' and every roster lookup silently misses.
		return s.toLowerCase(Locale.ROOT);
	}

	public static boolean isValid(String rsn)
	{
		return rsn != null && VALID_RSN.matcher(rsn).matches();
	}
}
