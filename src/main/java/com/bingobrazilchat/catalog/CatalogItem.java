package com.bingobrazilchat.catalog;

import lombok.Value;

/**
 * One scoreable item, as the website defines it.
 *
 * <p>{@code dropItemId} is the identity that matters. Resolving to a UUID rather than a name is
 * required because "Kit/Dust" exists twice in {@code drop_items} -- once under CoX and once under
 * ToB -- so a name alone is ambiguous and only the alias entry knows which raid it came from.
 */
@Value
public class CatalogItem
{
	/** {@code drop_items.id} on the server. */
	String dropItemId;

	/** The catalog's own display name, e.g. "Kodai Wand". */
	String catalogName;

	/** {@code cox} | {@code tob} | {@code toa}. */
	String raid;

	int points;
}
