package com.bingobrazilchat.api;

import java.util.List;
import java.util.Map;

/**
 * Wire shapes for {@code /api/plugin/*}. Plain fields, deserialised by the injected Gson.
 *
 * <p>Field names match the JSON exactly, so there are no {@code @SerializedName} annotations to
 * fall out of step with the server.
 */
public final class ApiDtos
{
	private ApiDtos()
	{
	}

	public static class ConfigResponse
	{
		public boolean ok;
		public String error;
		public String serverTime;
		public EventDto event;
		public List<ItemDto> items;
		/** Normalised in-game item name -&gt; drop_item_id. Authoritative over the baked-in table. */
		public Map<String, String> aliases;
		public List<String> ignored;
		public List<String> roster;
		public PolicyDto policy;
	}

	public static class EventDto
	{
		public String id;
		public String name;
		/** ISO-8601 UTC, or null for an unbounded event. */
		public String startDate;
		public String endDate;
	}

	public static class ItemDto
	{
		public String id;
		public String name;
		public int points;
		public String raid;
	}

	public static class PolicyDto
	{
		public long observerDelayMs = 20_000L;
		public long observerJitterMs = 15_000L;
		public long herdWindowSeconds = 120L;
		public long maxProofBytes = 3_145_728L;
		public long configPollSeconds = 300L;
		public long leaderboardPollSeconds = 60L;
	}

	/** Phase 1: what we tell the server happened. Carries no screenshot. */
	public static class ClaimRequest
	{
		public String eventId;
		public String rsn;
		public String dropItemId;
		public String itemNameRaw;
		public String broadcastType;
		public String rawMessage;
		public String occurredAt;
		public boolean observer;
		public String reporterRsn;
		public String dedupeKey;
		public boolean hasProof;
		public String clientVersion;
	}

	public static class ClaimResponse
	{
		public boolean ok;
		public String error;
		/** {@code created} | {@code duplicate} | {@code rejected}. */
		public String status;
		public String reason;
		public String submissionId;
		public boolean proofUploadRequired;
		public String proofUploadToken;
		public String itemName;
		public int points;
		public String playerRsn;

		public boolean isCreated()
		{
			return "created".equals(status);
		}

		public boolean isDuplicate()
		{
			return "duplicate".equals(status);
		}

		public boolean isRejected()
		{
			return "rejected".equals(status);
		}
	}

	public static class LeaderboardResponse
	{
		public boolean ok;
		public String generatedAt;
		public EventDto event;
		public List<TeamRow> teams;
		public List<PlayerRow> players;
	}

	public static class TeamRow
	{
		public int rank;
		public String teamId;
		public String name;
		public String color;
		public int points;
		public int uniqueItems;
		public int memberCount;
	}

	public static class PlayerRow
	{
		public int rank;
		public String rsn;
		public String teamName;
		public int points;
		public int uniqueItems;
	}
}
