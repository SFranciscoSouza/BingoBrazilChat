package com.bingobrazilchat.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * These cases are deliberately the same ones asserted against {@code normalizeRsn} in the website's
 * {@code plugin-api.test.ts}. The two implementations must agree exactly: if they drift, clients
 * and server compute different dedupe keys and the broadcast herd stops collapsing.
 */
public class RsnUtilTest
{
	@Test
	public void lowercases()
	{
		assertEquals("zezinho", RsnUtil.normalizeForCompare("Zezinho"));
	}

	@Test
	public void treatsHyphenUnderscoreAndSpaceAsTheSameSeparator()
	{
		assertEquals("fn junior", RsnUtil.normalizeForCompare("Fn-Junior"));
		assertEquals("fn junior", RsnUtil.normalizeForCompare("Fn_Junior"));
		assertEquals("fn junior", RsnUtil.normalizeForCompare("Fn Junior"));
	}

	@Test
	public void collapsesRepeatedSeparatorsAndTrims()
	{
		assertEquals("fn junior", RsnUtil.normalizeForCompare("  Fn__Junior  "));
	}

	@Test
	public void handlesNullAndEmpty()
	{
		assertEquals("", RsnUtil.normalizeForCompare(null));
		assertEquals("", RsnUtil.normalizeForCompare("   "));
	}

	@Test
	public void acceptsRealNames()
	{
		assertTrue(RsnUtil.isValid("Zezinho"));
		assertTrue(RsnUtil.isValid("Fn Junior"));
		assertTrue(RsnUtil.isValid("A-B_c 123"));
		assertTrue(RsnUtil.isValid("123456789012"));
	}

	@Test
	public void rejectsNamesTheServerWouldReject()
	{
		// OSRS display names are at most 12 characters, and the server enforces the same rule.
		assertFalse(RsnUtil.isValid("1234567890123"));
		assertFalse(RsnUtil.isValid(""));
		assertFalse(RsnUtil.isValid(null));
		assertFalse(RsnUtil.isValid("bad<name>"));
	}
}
