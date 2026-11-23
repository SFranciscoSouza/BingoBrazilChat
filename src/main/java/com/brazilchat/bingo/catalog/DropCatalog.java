package com.brazilchat.bingo.catalog;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves an in-game item name to the catalog entry it scores as.
 *
 * <p>Three layers, in precedence order:
 * <ol>
 *   <li>the server's alias map (normalized in-game name -&gt; {@code drop_item_id}), which is
 *       authoritative and can be corrected with a Vercel deploy rather than a plugin release;</li>
 *   <li>a direct match on the catalog's own name, which covers the majority of items;</li>
 *   <li>the baked-in alias table, a safety net for the handful of names where the in-game item and
 *       the catalog entry genuinely differ (see {@code aliases.json}).</li>
 * </ol>
 *
 * <p>Anything unresolved is either a known non-scoring raid drop (see {@link #isKnownIgnored}) or a
 * genuine surprise worth logging.
 */
public final class DropCatalog
{
	private final Map<String, CatalogItem> byName;
	private final Set<String> ignored;

	private DropCatalog(Map<String, CatalogItem> byName, Set<String> ignored)
	{
		this.byName = Collections.unmodifiableMap(byName);
		this.ignored = Collections.unmodifiableSet(ignored);
	}

	/** An empty catalog: what the plugin runs on before it has ever reached the server. */
	public static DropCatalog empty()
	{
		return new DropCatalog(new HashMap<>(), new HashSet<>());
	}

	/**
	 * Builds the live catalog.
	 *
	 * @param items        the catalog rows from {@code /api/plugin/config}
	 * @param serverAlias  normalized in-game name -&gt; {@code drop_item_id}, from the same response
	 * @param bakedAlias   the shipped fallback table, normalized in-game name -&gt; (name, raid)
	 * @param ignoredNames normalized names that broadcast as raid loot but deliberately score zero
	 */
	public static DropCatalog build(
		Iterable<CatalogItem> items,
		Map<String, String> serverAlias,
		Map<String, AliasTarget> bakedAlias,
		Iterable<String> ignoredNames)
	{
		Map<String, CatalogItem> byId = new HashMap<>();
		Map<String, CatalogItem> resolved = new HashMap<>();

		for (CatalogItem item : items)
		{
			byId.put(item.getDropItemId(), item);
			// Layer 2: the catalog's own name. Note this intentionally lets a later raid overwrite
			// an earlier one for an ambiguous name like "Kit/Dust" -- such names are never resolved
			// this way in practice, because every real in-game name for them goes through an alias.
			resolved.put(ItemNameNormalizer.normalize(item.getCatalogName()), item);
		}

		// Layer 3 first, so layer 1 can overwrite it below.
		if (bakedAlias != null)
		{
			for (Map.Entry<String, AliasTarget> e : bakedAlias.entrySet())
			{
				CatalogItem target = findByNameAndRaid(items, e.getValue());
				if (target != null)
				{
					resolved.put(ItemNameNormalizer.normalize(e.getKey()), target);
				}
			}
		}

		// Layer 1: the server always wins.
		if (serverAlias != null)
		{
			for (Map.Entry<String, String> e : serverAlias.entrySet())
			{
				CatalogItem target = byId.get(e.getValue());
				if (target != null)
				{
					resolved.put(ItemNameNormalizer.normalize(e.getKey()), target);
				}
			}
		}

		Set<String> ignoredSet = new HashSet<>();
		if (ignoredNames != null)
		{
			for (String n : ignoredNames)
			{
				ignoredSet.add(ItemNameNormalizer.normalize(n));
			}
		}

		return new DropCatalog(resolved, ignoredSet);
	}

	public Optional<CatalogItem> lookup(String rawItemName)
	{
		return Optional.ofNullable(byName.get(ItemNameNormalizer.normalize(rawItemName)));
	}

	/**
	 * True for raid loot we know about and have decided not to score -- pets, dark relics, the ToA
	 * upgrade items. Distinguishing these from genuine surprises keeps the debug log signal-heavy.
	 */
	public boolean isKnownIgnored(String rawItemName)
	{
		return ignored.contains(ItemNameNormalizer.normalize(rawItemName));
	}

	public boolean isEmpty()
	{
		return byName.isEmpty();
	}

	public int size()
	{
		return byName.size();
	}

	private static CatalogItem findByNameAndRaid(Iterable<CatalogItem> items, AliasTarget target)
	{
		String wantName = ItemNameNormalizer.normalize(target.getName());
		for (CatalogItem item : items)
		{
			if (ItemNameNormalizer.normalize(item.getCatalogName()).equals(wantName)
				&& item.getRaid().equalsIgnoreCase(target.getRaid()))
			{
				return item;
			}
		}
		return null;
	}

	/**
	 * A baked-in alias points at a (catalog name, raid) pair rather than an id, because the shipped
	 * resource cannot know the server-generated UUIDs. The raid half is what disambiguates the two
	 * "Kit/Dust" rows.
	 */
	@lombok.Value
	public static class AliasTarget
	{
		String name;
		String raid;
	}
}
