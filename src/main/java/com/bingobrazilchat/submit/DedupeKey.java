package com.bingobrazilchat.submit;

import com.bingobrazilchat.util.RsnUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The key that decides whether two reports are the same drop.
 *
 * <p>A clan broadcast is seen by every member running the plugin, so one drop produces one report
 * per online member. Those clients share nothing except what the broadcast said -- their clocks
 * routinely differ by minutes -- so the key is built purely from broadcast content and contains
 * <strong>no timestamp at all</strong>. A client-computed time bucket would split the herd whenever
 * skew straddled a boundary, and the drop would score twice.
 *
 * <p>How long two identical keys count as the same drop is decided server-side, under a lock, in
 * {@code claim_plugin_drop}. That keeps the "is this a repeat?" policy in one place.
 */
public final class DedupeKey
{
	private DedupeKey()
	{
	}

	/**
	 * @param eventId    the active event's id
	 * @param rsn        the dropper's name as broadcast; normalised here, not by the caller
	 * @param dropItemId the resolved catalog item id
	 */
	public static String of(String eventId, String rsn, String dropItemId)
	{
		String material = eventId + "|" + RsnUtil.normalizeForCompare(rsn) + "|" + dropItemId;
		return sha256Hex(material);
	}

	private static String sha256Hex(String input)
	{
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(hash.length * 2);
			for (byte b : hash)
			{
				sb.append(Character.forDigit((b >> 4) & 0xF, 16));
				sb.append(Character.forDigit(b & 0xF, 16));
			}
			return sb.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			// SHA-256 is required of every JVM; if it is genuinely absent nothing here can work.
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
	}
}
