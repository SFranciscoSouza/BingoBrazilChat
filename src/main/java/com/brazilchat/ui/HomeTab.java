package com.brazilchat.ui;

import com.brazilchat.bingo.ui.BingoState;
import com.brazilchat.bingo.ui.BingoTab;
import com.brazilchat.clan.ClanData;
import com.brazilchat.clan.ClanSnapshot;
import com.brazilchat.clan.RankLadder;
import com.brazilchat.clan.api.ClanDtos;
import java.awt.Color;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Início: who you are in the clan, the clan at a glance, the current event, and links.
 * EDT only.
 */
public class HomeTab extends JPanel
{

	private final JPanel announcementsSection = Ui.column();
	private final JPanel linksCard = Ui.card();
	private final JPanel youSection = Ui.column();
	private final JPanel nextRankSection = Ui.column();
	private final JPanel clanSection = Ui.column();
	private final JPanel eventCard = Ui.card();
	private final JLabel eventName = Ui.small(" ", Palette.TEXT);
	private final JLabel eventCountdown = Ui.small(" ", Palette.MUTED);
	private final JLabel eventStatus = Ui.small(" ", Palette.MUTED);

	private ClanSnapshot clan = ClanSnapshot.loggedOut();
	private BingoState bingo = BingoState.initial();
	private ClanData clanData = ClanData.disabled();

	public HomeTab(Runnable openEvents)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(Palette.BACKGROUND);

		add(announcementsSection);

		add(Ui.sectionHeader(Strings.SECTION_YOU));
		add(youSection);
		add(Ui.vgap(Ui.SECTION_GAP));

		add(nextRankSection);

		add(Ui.sectionHeader(Strings.SECTION_CLAN));
		add(clanSection);
		add(Ui.vgap(Ui.SECTION_GAP));

		add(Ui.sectionHeader(Strings.SECTION_CURRENT_EVENT));
		eventCard.add(eventName);
		eventCard.add(eventCountdown);
		eventCard.add(eventStatus);
		eventCard.setToolTipText(Strings.OPEN_EVENTS);
		Ui.clickable(eventCard, openEvents);
		add(eventCard);
		add(Ui.vgap(Ui.SECTION_GAP));

		add(Ui.sectionHeader(Strings.SECTION_LINKS));
		add(linksCard);

		renderClan();
		renderEvent();
		renderClanData();
	}

	public void setClan(ClanSnapshot snapshot)
	{
		this.clan = snapshot;
		renderClan();
		renderClanData();
	}

	public void setBingo(BingoState state)
	{
		this.bingo = state;
		renderEvent();
	}

	public void setClanData(ClanData data)
	{
		this.clanData = data;
		renderClanData();
		renderEvent();
	}

	public void tick()
	{
		Instant now = Instant.now();
		if (bingo.getWindow() != null)
		{
			eventCountdown.setText(BingoTab.countdownText(bingo.getWindow(), now));
			return;
		}
		ClanDtos.EventDto next = nextClanEvent(now);
		Instant start = next == null ? null : parse(next.startDate);
		if (start == null)
		{
			eventCountdown.setText(" ");
		}
		else if (start.isAfter(now))
		{
			eventCountdown.setText(Strings.EVENT_STARTS_IN + Strings.duration(java.time.Duration.between(now, start)));
		}
		else
		{
			eventCountdown.setText(Strings.EVENT_RUNNING);
		}
	}

	/** The next calendar event that has not finished, when the clan connection is on. */
	private ClanDtos.EventDto nextClanEvent(Instant now)
	{
		ClanDtos.Bootstrap b = clanData.getBootstrap();
		if (b == null || b.events == null)
		{
			return null;
		}
		for (ClanDtos.EventDto e : b.events)
		{
			Instant start = parse(e.startDate);
			Instant end = parse(e.endDate);
			if (start != null && (end == null ? start.plusSeconds(6 * 3600).isAfter(now) : end.isAfter(now)))
			{
				return e;
			}
		}
		return null;
	}

	private static Instant parse(String iso)
	{
		if (iso == null)
		{
			return null;
		}
		try
		{
			return Instant.parse(iso);
		}
		catch (java.time.format.DateTimeParseException e)
		{
			return null;
		}
	}

	// ------------------------------------------------------------------------------------------

	private void renderClan()
	{
		youSection.removeAll();
		clanSection.removeAll();
		renderNextRank();

		if (!clan.isLoggedIn())
		{
			youSection.add(Ui.empty(Strings.NOT_LOGGED_IN));
			clanSection.add(Ui.empty(Strings.NOT_LOGGED_IN));
		}
		else if (!clan.inClan())
		{
			youSection.add(card(Ui.statRow(Strings.LABEL_NAME, nz(clan.getRsn()))));
			clanSection.add(Ui.empty(Strings.NOT_IN_CLAN));
		}
		else
		{
			JPanel you = Ui.accentCard(clan.isStaff() ? Palette.STAFF : Palette.BRAND_GREEN);
			you.add(Ui.statRow(Strings.LABEL_NAME, nz(clan.getRsn())));
			you.add(Ui.statRow(Strings.LABEL_RANK, Strings.rankLabel(clan.getRankTitle()),
				clan.isStaff() ? Palette.STAFF : Palette.TEXT));
			LocalDate joined = clan.getJoinDate();
			if (joined != null)
			{
				you.add(Ui.statRow(Strings.LABEL_MEMBER_SINCE, joined.format(Strings.DATE)));
				long days = ChronoUnit.DAYS.between(joined, LocalDate.now(Strings.BRASILIA));
				you.add(Ui.statRow(Strings.LABEL_TIME_IN_CLAN, Strings.timeInClan(days)));
			}
			youSection.add(you);

			JPanel clanCard = Ui.card();
			clanCard.add(Ui.title(clan.getClanName(), Palette.BRAND_GOLD));
			clanCard.add(Ui.statRow(Strings.LABEL_ONLINE, String.valueOf(clan.getOnline().size())));
			clanCard.add(Ui.statRow(Strings.LABEL_MEMBERS, String.valueOf(clan.getTotalMembers())));
			if (!Strings.CLAN_NAME.equalsIgnoreCase(clan.getClanName()))
			{
				clanCard.add(Ui.vgap(4));
				clanCard.add(Ui.wrapped(Strings.OTHER_CLAN_WARNING, Palette.WARNING));
			}
			clanSection.add(clanCard);
		}

		revalidate();
		repaint();
	}

	private void renderEvent()
	{
		ClanDtos.EventDto next = bingo.getWindow() == null ? nextClanEvent(Instant.now()) : null;
		if (bingo.getWindow() == null && next != null)
		{
			eventName.setText(next.name);
			eventName.setForeground(Palette.TEXT);
		}
		else if (bingo.getWindow() == null)
		{
			eventName.setText(Strings.NO_EVENT);
			eventName.setForeground(Palette.MUTED);
		}
		else
		{
			eventName.setText(bingo.getWindow().getName());
			eventName.setForeground(Palette.TEXT);
		}
		Color colour = BingoTab.colourFor(bingo.getConnection());
		eventStatus.setText(Strings.SECTION_BINGO + ": " + bingo.getConnection().getLabel());
		eventStatus.setForeground(colour);
		tick();
	}

	private void renderClanData()
	{
		announcementsSection.removeAll();
		ClanDtos.Bootstrap b = clanData.getBootstrap();
		java.util.List<ClanDtos.Announcement> announcements = clanData.getAnnouncements();
		if (announcements != null && !announcements.isEmpty())
		{
			announcementsSection.add(Ui.sectionHeader(Strings.SECTION_ANNOUNCEMENTS));
			for (ClanDtos.Announcement a : announcements)
			{
				JPanel card = Ui.accentCard(a.pinned ? Palette.BRAND_GOLD : Palette.BRAND_GREEN);
				card.add(Ui.title(a.title, Palette.TEXT));
				if (a.body != null && !a.body.isEmpty())
				{
					card.add(Ui.wrapped(a.body, Palette.MUTED));
				}
				announcementsSection.add(card);
				announcementsSection.add(Ui.vgap(4));
			}
			announcementsSection.add(Ui.vgap(Ui.SECTION_GAP - 4));
		}
		if (clanData.getStatus() == ClanData.Status.DISABLED && clan.isConfirmedMember())
		{
			JPanel hint = Ui.accentCard(Palette.MUTED);
			hint.add(Ui.wrapped(Strings.CONNECT_HINT, Palette.TEXT));
			announcementsSection.add(hint);
			announcementsSection.add(Ui.vgap(Ui.SECTION_GAP));
		}

		linksCard.removeAll();
		ClanDtos.Links l = b == null ? null : b.links;
		linksCard.add(Ui.link(Strings.LINK_SITE, or(l == null ? null : l.site, Strings.CLAN_SITE_URL)));
		linksCard.add(Ui.vgap(4));
		linksCard.add(Ui.link(Strings.LINK_DISCORD, or(l == null ? null : l.discord, Strings.DISCORD_URL)));
		linksCard.add(Ui.vgap(4));
		linksCard.add(Ui.link(Strings.LINK_BINGO, or(l == null ? null : l.bingo, Strings.BINGO_SITE_URL)));
		linksCard.add(Ui.vgap(4));
		linksCard.add(Ui.link(Strings.LINK_WOM, or(l == null ? null : l.wom, Strings.WOM_URL)));

		revalidate();
		repaint();
	}

	/**
	 * "Próximo cargo": the next rank on the clan ladder and which requirements the logged-in player
	 * already meets, all from their own client (skills, diary and CA varbits, join date).
	 */
	private void renderNextRank()
	{
		nextRankSection.removeAll();
		RankLadder.Rank next = clan.inClan() && Strings.CLAN_NAME.equalsIgnoreCase(nz(clan.getClanName()).trim())
			? RankLadder.next(clan.getRankTitle())
			: null;
		ClanDtos.Achievements a = clan.getAchievements();
		if (next == null || a == null)
		{
			return;
		}

		RankLadder.Facts facts = RankLadder.facts(a, clan.getJoinDate(), LocalDate.now(Strings.BRASILIA));
		java.util.List<RankLadder.Check> checks = RankLadder.checks(next, facts);
		boolean ready = RankLadder.meetsAll(next, facts);

		nextRankSection.add(Ui.sectionHeader(Strings.SECTION_NEXT_RANK));
		JPanel card = Ui.accentCard(ready ? Palette.POSITIVE : Palette.BRAND_GOLD);
		card.add(Ui.title(next.getLabel(), Palette.TEXT));
		if (checks.isEmpty())
		{
			card.add(Ui.wrapped(Strings.NEXT_RANK_MANUAL, Palette.MUTED));
		}
		for (RankLadder.Check c : checks)
		{
			Color colour = c.getOk() == null ? Palette.MUTED : c.getOk() ? Palette.POSITIVE : Palette.WARNING;
			card.add(Ui.statRow(c.getLabel(), c.getHave(), colour));
		}
		if (ready)
		{
			card.add(Ui.vgap(4));
			card.add(Ui.wrapped(Strings.NEXT_RANK_READY, Palette.POSITIVE));
		}
		nextRankSection.add(card);
		nextRankSection.add(Ui.vgap(Ui.SECTION_GAP));
	}

	private static String or(String value, String fallback)
	{
		return value == null || value.isEmpty() ? fallback : value;
	}

	private static JPanel card(JPanel row)
	{
		JPanel card = Ui.card();
		card.add(row);
		return card;
	}

	private static String nz(String value)
	{
		return value == null ? "-" : value;
	}
}
