package com.brazilchat.clan;

import com.brazilchat.clan.api.ClanDtos;
import java.time.Instant;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventOverlayTest
{
	private static ClanData online(ClanDtos.EventDto event)
	{
		ClanDtos.Bootstrap b = new ClanDtos.Bootstrap();
		b.events = Collections.singletonList(event);
		return ClanData.disabled().withStatus(ClanData.Status.ONLINE).withBootstrap(b);
	}

	private static ClanDtos.EventDto event(String start, String end, String password)
	{
		ClanDtos.EventDto e = new ClanDtos.EventDto();
		e.id = "e1";
		e.name = "Mass de Nex";
		e.startDate = start;
		e.endDate = end;
		e.password = password;
		return e;
	}

	@Test
	public void showsFromHalfAnHourBeforeUntilTheEnd()
	{
		ClanData data = online(event("2026-11-20T20:00:00Z", "2026-11-20T22:00:00Z", "jabuti"));
		assertNull(EventOverlay.current(data, Instant.parse("2026-11-20T19:29:00Z")));
		assertEquals("e1", EventOverlay.current(data, Instant.parse("2026-11-20T19:31:00Z")).id);
		assertEquals("e1", EventOverlay.current(data, Instant.parse("2026-11-20T21:59:00Z")).id);
		assertNull(EventOverlay.current(data, Instant.parse("2026-11-20T22:00:00Z")));
	}

	@Test
	public void needsAPasswordAndAssumesSixHoursWithoutAnEnd()
	{
		assertNull(EventOverlay.current(online(event("2026-11-20T20:00:00Z", null, null)),
			Instant.parse("2026-11-20T20:10:00Z")));
		ClanData open = online(event("2026-11-20T20:00:00Z", null, "jabuti"));
		assertEquals("e1", EventOverlay.current(open, Instant.parse("2026-11-21T01:59:00Z")).id);
		assertNull(EventOverlay.current(open, Instant.parse("2026-11-21T02:00:00Z")));
	}

	@Test
	public void hiddenWhenNotOnline()
	{
		ClanData data = online(event("2026-11-20T20:00:00Z", null, "jabuti")).withStatus(ClanData.Status.ERROR);
		assertNull(EventOverlay.current(data, Instant.parse("2026-11-20T20:10:00Z")));
	}
}
