package com.brazilchat.clan;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * What the game currently says about the player's clan, read on the client thread and handed to
 * the panel as one immutable snapshot. Nothing here comes from the network.
 */
@Value
public class ClanSnapshot
{
	/** ClanRank values at or above this are staff (Administrator, Deputy Owner, Owner). */
	public static final int STAFF_RANK = 100;

	@Value
	public static class OnlineMember
	{
		String name;
		String title;
		/** In-game title id; with the name it identifies the game's rank icon. -1 if unknown. */
		int titleId;
		int rank;
		int world;

		public boolean isStaff()
		{
			return rank >= STAFF_RANK;
		}
	}

	boolean loggedIn;

	/** RuneLite account hash, or -1 when not logged in. Identifies the account to the site. */
	long accountHash;

	@Nullable
	String rsn;

	/** Null when the player is not in a clan (or the clan data has not loaded yet). */
	@Nullable
	String clanName;

	@Nullable
	String rankTitle;

	int rank;

	@Nullable
	LocalDate joinDate;

	int totalMembers;

	List<OnlineMember> online;

	/** The local player's own diaries, CA tier, total level and max status; null when unknown. */
	@Nullable
	com.brazilchat.clan.api.ClanDtos.Achievements achievements;

	public static ClanSnapshot loggedOut()
	{
		return new ClanSnapshot(false, -1L, null, null, null, 0, null, 0, Collections.emptyList(), null);
	}

	public boolean inClan()
	{
		return clanName != null;
	}

	public boolean isStaff()
	{
		return rank >= STAFF_RANK;
	}

	/**
	 * A confirmed member: logged in, in the Brazil Chat clan, and listed in its member list with a
	 * rank title (so not a guest in the channel).
	 */
	public boolean isConfirmedMember()
	{
		return inBrazilChat() && rankTitle != null;
	}

	/** True when logged in to the Brazil Chat clan specifically -- the only clan the site serves. */
	public boolean inBrazilChat()
	{
		return loggedIn && accountHash != -1L && rsn != null
			&& clanName != null && com.brazilchat.ui.Strings.CLAN_NAME.equalsIgnoreCase(clanName.trim());
	}
}
