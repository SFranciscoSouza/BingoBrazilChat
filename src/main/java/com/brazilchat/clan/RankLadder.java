package com.brazilchat.clan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * The Brazil Chat rank table, keyed by the <em>in-game title name</em> (what
 * {@code ClanSettings.titleForRank(...).getName()} returns, and what Wise Old Man stores), with the
 * clan's own Portuguese name next to it. Must match {@code src/lib/clan/rank-rules.ts} on the site.
 *
 * <pre>
 *   in game      clan name   requirements
 *   Recruit      Recruta     apply via Discord
 *   Corporal     Soldado     1300 total
 *   Sergeant     Sargento    1500 total, all easy diaries
 *   Cadet        Cadete      1700 total, all medium diaries
 *   Lieutenant   Tenente     1900 total, all hard diaries, easy CA, 30 days
 *   Captain      Capitão     2000 total, all hard diaries, medium CA, 60 days
 *   General      General     2100 total, diary cape, hard CA, 90 days
 *   Colonel      Endgame     2200 total, diary cape, elite CA, 90 days
 *   Brigadier    Maxed       max cape, diary cape, master CA, 90 days
 *   Marshal      GM          max cape, diary cape, grandmaster CA, 90 days
 * </pre>
 *
 * Special titles (given by hand): Infantry = MVP PvM, Skiller = MVP Skiller, Fire, Commander,
 * Beast, Hellcat.
 *
 * <p>Order matters and comes from this table, never from Jagex's numeric rank: the number is the
 * slot a title was assigned in the clan settings, which says nothing about the clan's ladder.
 */
public final class RankLadder
{
	/** Diary tiers, in order: 0 none .. 4 elite (elite in every area = diary cape). */
	public static final String[] DIARY_NAMES = {"none", "easy", "medium", "hard", "elite"};

	@Value
	public static class Rank
	{
		String key;
		String label;
		int totalLevel;
		boolean maxed;
		int diaryLevel;
		int caTier;
		int days;
	}

	public static final List<Rank> LADDER = Collections.unmodifiableList(Arrays.asList(
		new Rank("recruit", "Recruta", 0, false, 0, 0, 0),
		new Rank("corporal", "Soldado", 1300, false, 0, 0, 0),
		new Rank("sergeant", "Sargento", 1500, false, 1, 0, 0),
		new Rank("cadet", "Cadete", 1700, false, 2, 0, 0),
		new Rank("lieutenant", "Tenente", 1900, false, 3, 1, 30),
		new Rank("captain", "Capitão", 2000, false, 3, 2, 60),
		new Rank("general", "General", 2100, false, 4, 3, 90),
		new Rank("colonel", "Endgame", 2200, false, 4, 4, 90),
		new Rank("brigadier", "Maxed", 0, true, 4, 5, 90),
		new Rank("marshal", "GM", 0, true, 4, 6, 90)
	));

	private static final String[][] SPECIAL = {
		{"infantry", "MVP PvM"},
		{"skiller", "MVP Skiller"},
		{"fire", "Fire"},
		{"commander", "Commander"},
		{"beast", "Beast"},
		{"hellcat", "Hellcat"},
	};

	private static final String[] CA_NAMES = {"", "Easy", "Medium", "Hard", "Elite", "Master", "Grandmaster"};
	private static final String[] DIARY_LABELS = {"", "fáceis", "médios", "difíceis", "elite"};

	private RankLadder()
	{
	}

	static String key(@Nullable String title)
	{
		return title == null ? "" : title.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
	}

	/** Position on the ladder, or -1 for staff, special and unknown titles. */
	public static int index(@Nullable String title)
	{
		String key = key(title);
		for (int i = 0; i < LADDER.size(); i++)
		{
			if (LADDER.get(i).getKey().equals(key))
			{
				return i;
			}
		}
		return -1;
	}

	public static boolean isSpecial(@Nullable String title)
	{
		String key = key(title);
		for (String[] s : SPECIAL)
		{
			if (s[0].equals(key))
			{
				return true;
			}
		}
		return false;
	}

	/** The clan's name for an in-game title; titles not in the table are shown as they are. */
	public static String label(@Nullable String title)
	{
		if (title == null)
		{
			return "-";
		}
		int i = index(title);
		if (i >= 0)
		{
			return LADDER.get(i).getLabel();
		}
		String key = key(title);
		for (String[] s : SPECIAL)
		{
			if (s[0].equals(key))
			{
				return s[1];
			}
		}
		return title;
	}

	/**
	 * Whether going from one title to another is a promotion worth congratulating.
	 *
	 * <ul>
	 *   <li>Both on the ladder: higher on the ladder.</li>
	 *   <li>Into a special title (MVP, Fire, ...): yes, it is an honour.</li>
	 *   <li>Into staff (Administrator and up, rank 100+): yes, if the rank number went up.</li>
	 *   <li>Anything else, including leaving a special title: no.</li>
	 * </ul>
	 */
	public static boolean isPromotion(@Nullable String oldTitle, int oldRank, @Nullable String newTitle, int newRank)
	{
		if (key(oldTitle).equals(key(newTitle)) && oldRank == newRank)
		{
			return false;
		}
		int from = index(oldTitle);
		int to = index(newTitle);
		if (from >= 0 && to >= 0)
		{
			return to > from;
		}
		if (isSpecial(newTitle) && !key(oldTitle).equals(key(newTitle)))
		{
			return true;
		}
		return newRank >= ClanSnapshot.STAFF_RANK && newRank > oldRank;
	}

	/** What the local player has, for checking their own next rank. */
	@Value
	public static class Facts
	{
		int totalLevel;
		boolean maxed;
		int diaryLevel;
		int caTier;
		/** Null when the join date is unknown. */
		@Nullable
		Integer daysInClan;
	}

	@Value
	public static class Check
	{
		String label;
		/** What the player has now, e.g. "1856" or "Medium". */
		String have;
		/** True, false, or null when it cannot be known locally. */
		@Nullable
		Boolean ok;
	}

	/**
	 * Facts for the local player, from their own client. Null when their achievements have not
	 * been read yet (e.g. still loading after login).
	 */
	@Nullable
	public static Facts facts(@Nullable com.brazilchat.clan.api.ClanDtos.Achievements a,
		@Nullable java.time.LocalDate joinDate, java.time.LocalDate today)
	{
		if (a == null)
		{
			return null;
		}
		Integer days = joinDate == null ? null : (int) java.time.temporal.ChronoUnit.DAYS.between(joinDate, today);
		int diary = Math.max(0, Arrays.asList(DIARY_NAMES).indexOf(a.diaryLevel));
		return new Facts(a.totalLevel, a.maxed, diary, a.caTier, days);
	}

	/** True when the rank has checkable requirements and every one of them is met. */
	public static boolean meetsAll(Rank rank, Facts facts)
	{
		List<Check> checks = checks(rank, facts);
		return !checks.isEmpty() && checks.stream().allMatch(c -> Boolean.TRUE.equals(c.getOk()));
	}

	/** The next ladder rank above the given title, or null at the top / off the ladder. */
	@Nullable
	public static Rank next(@Nullable String title)
	{
		int i = index(title);
		return i < 0 || i + 1 >= LADDER.size() ? null : LADDER.get(i + 1);
	}

	public static List<Check> checks(Rank rank, Facts f)
	{
		List<Check> out = new ArrayList<>();
		if (rank.getTotalLevel() > 0)
		{
			out.add(new Check("Nível total " + rank.getTotalLevel(), String.valueOf(f.getTotalLevel()),
				f.isMaxed() || f.getTotalLevel() >= rank.getTotalLevel()));
		}
		if (rank.isMaxed())
		{
			out.add(new Check("Max cape", f.isMaxed() ? "Sim" : "Não", f.isMaxed()));
		}
		if (rank.getDiaryLevel() > 0)
		{
			String label = rank.getDiaryLevel() == 4 ? "Diary cape" : "Todos os diários " + DIARY_LABELS[rank.getDiaryLevel()];
			String have = f.getDiaryLevel() == 0 ? "Nenhum" : "Até " + DIARY_LABELS[f.getDiaryLevel()];
			out.add(new Check(label, have, f.getDiaryLevel() >= rank.getDiaryLevel()));
		}
		if (rank.getCaTier() > 0)
		{
			out.add(new Check("Combat Achievements " + CA_NAMES[rank.getCaTier()],
				f.getCaTier() == 0 ? "Nenhum" : CA_NAMES[f.getCaTier()], f.getCaTier() >= rank.getCaTier()));
		}
		if (rank.getDays() > 0)
		{
			Integer days = f.getDaysInClan();
			out.add(new Check(rank.getDays() + " dias de clã", days == null ? "?" : days + " dias",
				days == null ? null : days >= rank.getDays()));
		}
		return out;
	}
}
