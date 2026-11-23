package com.brazilchat.clan.api;

import java.util.List;

/**
 * Wire shapes for /api/plugin/clan/* and /api/plugin/staff/*. Public fields, filled by Gson;
 * mirrors the TypeScript route handlers in the BrazilChat site.
 */
public final class ClanDtos
{
	private ClanDtos()
	{
	}

	public static class OkResponse
	{
		public boolean ok;
		public String error;
	}

	// ------------------------------------------------------------------------------- bootstrap

	public static class Bootstrap extends OkResponse
	{
		public String serverTime;
		public Me me;
		public List<Announcement> announcements;
		public List<EventDto> events;
		public List<Competition> competitions;
		public Links links;
	}

	public static class Me
	{
		public String rsn;
		public Integer rank;
		public String rankTitle;
		public String joinedAt;
		public boolean staff;
	}

	public static class Announcement
	{
		public String id;
		public String title;
		public String body;
		public boolean pinned;
		public String expiresAt;
		public String createdAt;
		public String updatedAt;
		public String createdBy;
	}

	public static class Announcements extends OkResponse
	{
		public List<Announcement> announcements;
	}

	/** What an admin types when creating or editing an announcement. */
	public static class AnnouncementInput
	{
		public String title;
		public String body;
		public boolean pinned;

		public AnnouncementInput(String title, String body, boolean pinned)
		{
			this.title = title;
			this.body = body;
			this.pinned = pinned;
		}
	}

	public static class EventDto
	{
		public String id;
		public String name;
		public String description;
		public String startDate;
		public String endDate;
		public boolean usesSignups;
		public boolean signupsOpen;
		public String signupDeadline;
		public Long entryFee;
		public Integer maxSlots;
		public int signedUpCount;
		public String password;
		public boolean isBingo;
		public MySignup mySignup;
	}

	public static class MySignup
	{
		/** signed_up | cancelled | removed */
		public String status;
		public boolean feePaid;
		public String removedReason;

		public boolean isActive()
		{
			return "signed_up".equals(status);
		}
	}

	public static class Competition
	{
		public long id;
		public String title;
		public String metric;
		public String startsAt;
		public String endsAt;
		public int participants;
		public List<Standing> top;
		public Standing me;
	}

	public static class Standing
	{
		public int rank;
		public String rsn;
		public long gained;
	}

	public static class Links
	{
		public String site;
		public String bingo;
		public String wall;
		public String wom;
		public String discord;
	}

	/** The player's own achievements, read from their game client. Feeds the rank-up worklist. */
	@lombok.EqualsAndHashCode
	public static class Achievements
	{
		public int totalLevel;
		public boolean maxed;
		/** Highest diary tier done in every area: none | easy | medium | hard | elite */
		public String diaryLevel;
		/** 0 none .. 6 grandmaster */
		public int caTier;

		public Achievements(int totalLevel, boolean maxed, String diaryLevel, int caTier)
		{
			this.totalLevel = totalLevel;
			this.maxed = maxed;
			this.diaryLevel = diaryLevel;
			this.caTier = caTier;
		}
	}

	// ----------------------------------------------------------------------------------- staff

	public static class StaffEvents extends OkResponse
	{
		public List<StaffEvent> events;
	}

	public static class StaffEvent
	{
		public String id;
		public String name;
		public String startDate;
		public boolean signupsOpen;
		public Long entryFee;
		public Integer maxSlots;
		public Totals totals;
		public List<StaffSignup> signups;
	}

	public static class Totals
	{
		public int signedUp;
		public int paid;
		public int pending;
		public long collected;
	}

	public static class StaffSignup
	{
		public String id;
		public String rsn;
		public String status;
		public String note;
		public boolean feePaid;
		public String feeMarkedBy;
		public String removedReason;
	}

	public static class Overview extends OkResponse
	{
		public List<Sync> syncs;
		public int memberCount;
		public List<Inactive> inactive;
		public List<Promotion> recentPromotions;
		public List<WorkItem> worklist;
		public Mvp mvp;
		public List<Announcement> announcements;
	}

	public static class WorkItem
	{
		public String rsn;
		/** promote | possible */
		public String kind;
		public String from;
		public String to;
		public List<String> unknown;
		public String maybeHigher;
		public boolean usesPlugin;
	}

	public static class Mvp
	{
		public Top pvm;
		public Top skiller;
	}

	public static class Top
	{
		public String rsn;
		public double gained;
	}

	public static class Sync
	{
		public String source;
		public String synced_at;
		public int member_count;
		public String synced_by;
	}

	public static class Inactive
	{
		public String rsn;
		public String title;
		public int daysInactive;
	}

	public static class Promotion
	{
		public String rsn;
		public String title;
		public String at;
	}

	public static class RosterMember
	{
		public String rsn;
		public int rank;
		public String title;
		public String joinDate;

		public RosterMember(String rsn, int rank, String title, String joinDate)
		{
			this.rsn = rsn;
			this.rank = rank;
			this.title = title;
			this.joinDate = joinDate;
		}
	}

	public static class RosterUpload
	{
		public String clan;
		public List<RosterMember> members;

		public RosterUpload(String clan, List<RosterMember> members)
		{
			this.clan = clan;
			this.members = members;
		}
	}
}
