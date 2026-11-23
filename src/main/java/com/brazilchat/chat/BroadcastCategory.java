package com.brazilchat.chat;

import com.brazilchat.util.Sanitizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * What a clan broadcast is about. Used to hide the noisy kinds and highlight the rare ones.
 *
 * <p>Classification is substring-based on purpose: Jagex tweaks broadcast wording now and then, and
 * a loose match that still recognises "has completed a quest" after a punctuation change is worth
 * more than a strict regex that silently stops matching.
 */
public enum BroadcastCategory
{
	RAID_LOOT,
	DROP,
	PET,
	COLLECTION_LOG,
	MAX_TOTAL,
	LEVEL,
	QUEST,
	DIARY,
	COMBAT_ACHIEVEMENT,
	PERSONAL_BEST,
	PVP,
	MEMBERSHIP,
	OTHER;

	private static final Pattern MEMBERSHIP_PATTERN = Pattern.compile(
		"(invited into|joined|left|expelled from|been kicked from|been removed from) the clan");

	/** Rare, celebratory broadcasts: never hidden, optionally highlighted. */
	public boolean isRare()
	{
		return this == RAID_LOOT || this == PET || this == COLLECTION_LOG || this == MAX_TOTAL;
	}

	public static BroadcastCategory classify(String rawMessage)
	{
		String line = Sanitizer.clean(rawMessage);
		if (line.isEmpty())
		{
			return OTHER;
		}

		Optional<ParsedBroadcast> parsed = BroadcastParser.parse(line);
		if (parsed.isPresent())
		{
			return parsed.get().getKind() == BroadcastKind.RAID_SPECIAL_LOOT ? RAID_LOOT : DROP;
		}

		String lower = line.toLowerCase(Locale.ROOT);
		if (lower.contains("funny feeling like") || lower.contains("would have been followed"))
		{
			return PET;
		}
		if (lower.contains("new collection log item"))
		{
			return COLLECTION_LOG;
		}
		if (lower.contains("highest possible total level"))
		{
			return MAX_TOTAL;
		}
		if (lower.contains("personal best"))
		{
			return PERSONAL_BEST;
		}
		if (lower.contains("combat achievement") || lower.contains("combat task"))
		{
			return COMBAT_ACHIEVEMENT;
		}
		if (lower.contains(" diary"))
		{
			return DIARY;
		}
		if (lower.contains("completed a quest") || lower.contains("completed the quest"))
		{
			return QUEST;
		}
		if (lower.contains(" has reached "))
		{
			return LEVEL;
		}
		if (lower.contains(" has defeated ") || lower.contains(" has been defeated by ")
			|| lower.contains("hardcore"))
		{
			return PVP;
		}
		if (MEMBERSHIP_PATTERN.matcher(lower).find())
		{
			return MEMBERSHIP;
		}
		return OTHER;
	}
}
