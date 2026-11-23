package com.brazilchat.bingo.catalog;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The catalog is where a wrong assumption costs points silently: an unresolved item simply never
 * scores. Every alias in the shipped table is asserted here.
 */
public class DropCatalogTest
{
	private DropCatalog catalog;

	/** Mirrors the website's seed catalog, with stable fake ids. */
	private static List<CatalogItem> seedItems()
	{
		List<CatalogItem> items = new ArrayList<>();
		items.add(new CatalogItem("cox-tbow", "Twisted Bow", "cox", 20));
		items.add(new CatalogItem("cox-kodai", "Kodai Wand", "cox", 6));
		items.add(new CatalogItem("cox-maul", "Elder Maul", "cox", 6));
		items.add(new CatalogItem("cox-dex", "Dexterous Prayer Scroll", "cox", 2));
		items.add(new CatalogItem("cox-claws", "Dragon Claws", "cox", 4));
		items.add(new CatalogItem("cox-anc-top", "Ancestral Robe Top", "cox", 5));
		items.add(new CatalogItem("cox-kit", "Kit/Dust", "cox", 1));

		items.add(new CatalogItem("tob-scythe", "Scythe of Vitur", "tob", 20));
		items.add(new CatalogItem("tob-sang", "Sanguinesti Staff", "tob", 6));
		items.add(new CatalogItem("tob-rapier", "Ghrazi Rapier", "tob", 6));
		items.add(new CatalogItem("tob-kit", "Kit/Dust", "tob", 1));

		items.add(new CatalogItem("toa-shadow", "Tumeken's Shadow", "toa", 20));
		items.add(new CatalogItem("toa-fang", "Osmumten's Fang", "toa", 2));
		items.add(new CatalogItem("toa-ward", "Elidinis' Ward", "toa", 1));
		items.add(new CatalogItem("toa-masori-body", "Masori Body", "toa", 5));
		return items;
	}

	@Before
	public void setUp() throws IOException
	{
		// new Gson() is fine in src/test; the Plugin Hub packager only scans src/main.
		AliasResource.Loaded baked = AliasResource.load(new Gson());
		catalog = DropCatalog.build(seedItems(), Collections.emptyMap(), baked.getAliases(),
			baked.getIgnored());
	}

	private void assertResolves(String inGameName, String expectedId)
	{
		assertTrue("no catalog match for: " + inGameName, catalog.lookup(inGameName).isPresent());
		assertEquals(inGameName, expectedId, catalog.lookup(inGameName).get().getDropItemId());
	}

	@Test
	public void resolvesPlainSentenceCaseNames()
	{
		assertResolves("Twisted bow", "cox-tbow");
		assertResolves("Elder maul", "cox-maul");
		assertResolves("Dragon claws", "cox-claws");
		assertResolves("Ancestral robe top", "cox-anc-top");
		assertResolves("Ghrazi rapier", "tob-rapier");
		assertResolves("Masori body", "toa-masori-body");
	}

	@Test
	public void resolvesKodaiInsignia()
	{
		// The single most important alias: CoX drops the insignia, never the wand. Without this,
		// every Kodai in the event scores zero.
		assertResolves("Kodai insignia", "cox-kodai");
	}

	@Test
	public void stripsChargeSuffixes()
	{
		assertResolves("Scythe of Vitur (uncharged)", "tob-scythe");
		assertResolves("Sanguinesti staff (uncharged)", "tob-sang");
		assertResolves("Tumeken's shadow (uncharged)", "toa-shadow");
	}

	@Test
	public void resolvesElidinisWardWithoutAnyBrokenSuffix()
	{
		// There is no "Elidinis' ward (broken)" item; the ToA drop broadcasts as the plain name.
		assertResolves("Elidinis' ward", "toa-ward");
	}

	@Test
	public void kitAndDustResolveToTheCorrectRaid()
	{
		// "Kit/Dust" exists once per raid, so name alone is ambiguous. Only the alias knows.
		assertResolves("Metamorphic dust", "cox-kit");
		assertResolves("Twisted ancestral colour kit", "cox-kit");
		assertResolves("Twisted ancestral color kit", "cox-kit");

		assertResolves("Sanguine dust", "tob-kit");
		assertResolves("Holy ornament kit", "tob-kit");
		assertResolves("Sanguine ornament kit", "tob-kit");
	}

	@Test
	public void resolvesBothSpellingsOfDexterousScroll()
	{
		assertResolves("Dexterous prayer scroll", "cox-dex");
		assertResolves("Dextrous prayer scroll", "cox-dex");
	}

	@Test
	public void doesNotStripBuiltUpgradeSuffixes()
	{
		// These are upgrades the player made, never raid drops. Scoring them would award points
		// for a cosmetic recolour.
		assertFalse(catalog.lookup("Osmumten's fang (or)").isPresent());
		assertFalse(catalog.lookup("Masori body (f)").isPresent());
		assertFalse(catalog.lookup("Elidinis' ward (f)").isPresent());
	}

	@Test
	public void knownNonScoringRaidLootIsRecognisedAsIgnored()
	{
		assertTrue(catalog.isKnownIgnored("Olmlet"));
		assertTrue(catalog.isKnownIgnored("Dark relic"));
		assertTrue(catalog.isKnownIgnored("Lil' Zik"));
		// ToA upgrade items: deliberately not scored, because the catalog has no ToA Kit/Dust row.
		assertTrue(catalog.isKnownIgnored("Thread of elidinis"));
		assertTrue(catalog.isKnownIgnored("Breach of the scarab"));
		assertTrue(catalog.isKnownIgnored("Eye of the corruptor"));
		assertTrue(catalog.isKnownIgnored("Jewel of the sun"));

		assertFalse(catalog.isKnownIgnored("Twisted bow"));
	}

	@Test
	public void toaUpgradeItemsDoNotScore()
	{
		assertFalse(catalog.lookup("Thread of elidinis").isPresent());
		assertFalse(catalog.lookup("Breach of the scarab").isPresent());
	}

	@Test
	public void serverAliasOverridesTheBakedInTable()
	{
		// The server map is authoritative so a rename can be fixed with a deploy, not a release.
		Map<String, String> serverAlias = new HashMap<>();
		serverAlias.put("kodai insignia", "cox-maul");

		DropCatalog overridden = DropCatalog.build(seedItems(), serverAlias,
			Collections.emptyMap(), Collections.emptyList());

		assertEquals("cox-maul", overridden.lookup("Kodai insignia").get().getDropItemId());
	}

	@Test
	public void unknownItemsDoNotResolve()
	{
		assertFalse(catalog.lookup("Bandos chestplate").isPresent());
		assertFalse(catalog.lookup("").isPresent());
		assertFalse(catalog.lookup(null).isPresent());
	}
}
