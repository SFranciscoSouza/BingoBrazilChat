package com.brazilchat.chat;

/**
 * Which broadcast template a line matched. Sent to the server as {@code broadcastType} so an admin
 * reviewing Drop History can tell a high-confidence raid purple from a value-drop line that merely
 * happened to name a raid item.
 */
public enum BroadcastKind
{
	/**
	 * "{@code <RSN> received special loot from a raid: <Item>.}" -- the authoritative signal. Only
	 * fires for genuine raid uniques, so a match here is as close to proof as chat gets.
	 */
	RAID_SPECIAL_LOOT("raid_special_loot"),

	/**
	 * "{@code <RSN> received a drop: <Item> (<value> coins) from <Source>.}" -- the value-threshold
	 * broadcast. Lower confidence: a raid unique can appear here via a wilderness loot key or a PK,
	 * so these are flagged for audit rather than trusted outright.
	 */
	VALUE_DROP("value_drop");

	private final String wireName;

	BroadcastKind(String wireName)
	{
		this.wireName = wireName;
	}

	public String getWireName()
	{
		return wireName;
	}
}
