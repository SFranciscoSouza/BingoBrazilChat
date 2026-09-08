package com.bingobrazilchat.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Scrubs a raw chat line down to the plain text a regex can anchor on.
 *
 * <p>Every broadcast pattern in this plugin anchors on {@code ^}, so anything the client renders as
 * nothing but still leaves in the string will silently break matching. Three such things exist:
 * markup tags ({@code <col=ef20ff>}, {@code </col>}, {@code <img=2>}), chat colour tokens
 * ({@code @red@}), and the machine-readable metadata Jagex prefixes some broadcasts with
 * ({@code CA_ID:112|...}). All three are removed here, in that order -- the metadata prefix goes
 * last because it can sit *behind* a colour span, so it only reaches the start of the string once
 * the tags are gone.
 */
public final class Sanitizer
{
	private static final Pattern TAGS_AND_TOKENS = Pattern.compile("(<[^>]*>)|(@[^@\\s]+@)");
	private static final Pattern METADATA_PREFIX = Pattern.compile("^(?:[A-Z][A-Z0-9_]*:\\d+\\|)+");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private static final char NBSP = ' ';
	private static final char RIGHT_SINGLE_QUOTE = '’';
	private static final char LEFT_SINGLE_QUOTE = '‘';

	private Sanitizer()
	{
	}

	public static String clean(String raw)
	{
		if (raw == null)
		{
			return "";
		}

		String s = raw.replace("<br>", " ");
		s = TAGS_AND_TOKENS.matcher(s).replaceAll("");
		// OSRS renders display names with non-breaking spaces; without this fold, any RSN with a
		// space in it fails to match a pattern written with ordinary spaces.
		s = s.replace(NBSP, ' ');
		s = Normalizer.normalize(s, Normalizer.Form.NFKC);
		// Typographic apostrophes would break "Osmumten's fang" / "Elidinis' ward" lookups.
		s = s.replace(RIGHT_SINGLE_QUOTE, '\'').replace(LEFT_SINGLE_QUOTE, '\'');
		s = WHITESPACE.matcher(s).replaceAll(" ").trim();

		return METADATA_PREFIX.matcher(s).replaceAll("").trim();
	}
}
