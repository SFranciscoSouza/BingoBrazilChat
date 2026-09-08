package com.bingobrazilchat.event;

import java.time.Instant;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * The event's start and end, as the website defines them.
 *
 * <p>Both bounds are optional on the server ({@code events.start_date} and {@code end_date} are
 * nullable), and a null bound means "unbounded in that direction" rather than "closed".
 *
 * <p>The plugin's window check is only a courtesy that avoids pointless network calls and lets the
 * panel show a countdown. The server re-checks and is authoritative.
 */
@Value
public class EventWindow
{
	String eventId;
	String name;

	@Nullable
	Instant start;

	@Nullable
	Instant end;

	public boolean isOpen(Instant now)
	{
		if (start != null && now.isBefore(start))
		{
			return false;
		}
		return end == null || !now.isAfter(end);
	}

	public boolean isBeforeStart(Instant now)
	{
		return start != null && now.isBefore(start);
	}

	public boolean isAfterEnd(Instant now)
	{
		return end != null && now.isAfter(end);
	}
}
