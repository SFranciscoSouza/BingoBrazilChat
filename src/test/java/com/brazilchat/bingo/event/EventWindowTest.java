package com.brazilchat.bingo.event;

import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventWindowTest
{
	private static final Instant START = Instant.parse("2026-09-15T00:00:00Z");
	private static final Instant END = Instant.parse("2026-09-30T00:00:00Z");

	private static EventWindow window(Instant start, Instant end)
	{
		return new EventWindow("event-1", "Test Event", start, end);
	}

	@Test
	public void closedBeforeStart()
	{
		EventWindow w = window(START, END);
		assertFalse(w.isOpen(START.minusSeconds(1)));
		assertTrue(w.isBeforeStart(START.minusSeconds(1)));
	}

	@Test
	public void openOnTheBoundaries()
	{
		EventWindow w = window(START, END);
		assertTrue(w.isOpen(START));
		assertTrue(w.isOpen(END));
	}

	@Test
	public void closedAfterEnd()
	{
		EventWindow w = window(START, END);
		assertFalse(w.isOpen(END.plusSeconds(1)));
		assertTrue(w.isAfterEnd(END.plusSeconds(1)));
	}

	@Test
	public void nullBoundsMeanUnbounded()
	{
		// events.start_date and end_date are both nullable. A missing bound must mean "no limit in
		// that direction", not "closed" -- otherwise an event with no end date would never score.
		assertTrue(window(null, END).isOpen(START.minusSeconds(100_000)));
		assertTrue(window(START, null).isOpen(END.plusSeconds(100_000)));
		assertTrue(window(null, null).isOpen(Instant.now()));
	}

	@Test
	public void keepsItsIdentity()
	{
		assertEquals("event-1", window(START, END).getEventId());
		assertEquals("Test Event", window(START, END).getName());
	}
}
