package com.brazilchat.bingo.event;

import com.brazilchat.bingo.api.ApiDtos;
import com.brazilchat.bingo.catalog.DropCatalog;
import com.brazilchat.util.RsnUtil;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Value;

/**
 * An immutable snapshot of everything the last successful config fetch told us about the event.
 *
 * <p>Replaced wholesale on each refresh rather than mutated, so a broadcast handler that grabs a
 * reference mid-refresh always sees one internally consistent view.
 */
@Value
public class EventState
{
	EventWindow window;
	DropCatalog catalog;

	/** Normalised RSNs, so a name written with a hyphen still matches the roster. */
	Set<String> roster;

	ApiDtos.PolicyDto policy;

	public static EventState from(ApiDtos.ConfigResponse response, DropCatalog catalog)
	{
		Set<String> roster = new HashSet<>();
		List<String> raw = response.roster;
		if (raw != null)
		{
			for (String rsn : raw)
			{
				roster.add(RsnUtil.normalizeForCompare(rsn));
			}
		}

		EventWindow window = new EventWindow(
			response.event.id,
			response.event.name,
			parseInstant(response.event.startDate),
			parseInstant(response.event.endDate));

		return new EventState(window, catalog, Collections.unmodifiableSet(roster),
			response.policy == null ? new ApiDtos.PolicyDto() : response.policy);
	}

	public boolean isOnRoster(String rsn)
	{
		return roster.contains(RsnUtil.normalizeForCompare(rsn));
	}

	private static java.time.Instant parseInstant(String iso)
	{
		if (iso == null || iso.isEmpty())
		{
			return null;
		}
		try
		{
			return java.time.Instant.parse(iso);
		}
		catch (java.time.format.DateTimeParseException e)
		{
			// A bound we cannot read is treated as absent rather than as "closed", so a server-side
			// formatting slip cannot silently stop the whole clan from scoring.
			return null;
		}
	}
}
