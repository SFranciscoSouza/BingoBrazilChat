package com.brazilchat.clan;

import com.brazilchat.clan.api.ClanDtos;
import javax.annotation.Nullable;
import lombok.Value;
import lombok.With;

/**
 * What the site told us about the clan, as one immutable snapshot for the panel. Built on the
 * clan service's executor, rendered on the EDT.
 */
@Value
@With
public class ClanData
{
	public enum Status
	{
		/** "Conectar ao Brazil Chat" is off: nothing is sent, nothing shown. */
		DISABLED,
		/** On, but not logged in to the Brazil Chat clan. */
		NOT_IN_CLAN,
		CONNECTING,
		ONLINE,
		ERROR
	}

	Status status;

	/** Human-readable (PT-BR) problem for ERROR, or a transient notice after an action. */
	@Nullable
	String message;

	@Nullable
	ClanDtos.Bootstrap bootstrap;

	/** Staff data; null unless the player is staff with a working key. */
	@Nullable
	ClanDtos.StaffEvents staffEvents;

	@Nullable
	ClanDtos.Overview overview;

	/** Last staff error (e.g. key rejected), shown on the Staff tab only. */
	@Nullable
	String staffMessage;

	/** Ids of events with an action in flight, so buttons can be disabled. */
	boolean busy;

	/**
	 * Clan announcements for the Início tab. Loaded for any confirmed clan member, independently
	 * of the clan connection (see ClanService).
	 */
	java.util.List<ClanDtos.Announcement> announcements;

	public static ClanData disabled()
	{
		return new ClanData(Status.DISABLED, null, null, null, null, null, false, java.util.Collections.emptyList());
	}
}
