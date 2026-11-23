package com.brazilchat.chat;

import lombok.Value;

/**
 * The facts extracted from one broadcast line. Immutable and free of any RuneLite type, so the
 * parser that produces it can be unit-tested without a running client.
 */
@Value
public class ParsedBroadcast
{
	BroadcastKind kind;

	/** The dropper's name exactly as broadcast, tags and NBSP already removed but not lowercased. */
	String rsn;

	/** The item name exactly as broadcast, e.g. {@code "Kodai insignia"}. Not yet catalog-resolved. */
	String itemName;

	/** GE value in coins when the line carried one, else {@code -1}. Informational only. */
	long valueGp;

	/** Stack size when the line carried one (e.g. "587 x Cannonball"), else {@code 1}. */
	int quantity;

	/** The NPC/source the drop came from when the line named one, else {@code null}. */
	String source;

	/** The full sanitized line, forwarded to the server for audit. */
	String rawMessage;
}
