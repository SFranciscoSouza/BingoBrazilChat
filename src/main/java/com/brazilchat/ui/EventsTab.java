package com.brazilchat.ui;

import com.brazilchat.bingo.ui.BingoTab;
import com.brazilchat.clan.ClanData;
import com.brazilchat.clan.api.ClanDtos;
import java.awt.Color;
import java.awt.FlowLayout;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Eventos: the clan calendar with sign-up buttons, the clan's Wise Old Man competitions, and the
 * bingo. EDT only.
 */
public class EventsTab extends JPanel
{
	private final ClanActions actions;
	private final JPanel calendar = Ui.column();
	private final JPanel competitions = Ui.column();

	private ClanData data = ClanData.disabled();

	public EventsTab(ClanActions actions, BingoTab bingoTab)
	{
		this.actions = actions;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(Palette.BACKGROUND);

		add(Ui.sectionHeader(Strings.SECTION_CALENDAR));
		add(calendar);
		add(Ui.vgap(Ui.SECTION_GAP));
		add(Ui.sectionHeader(Strings.SECTION_COMPETITIONS));
		add(competitions);
		add(Ui.vgap(Ui.SECTION_GAP));
		add(bingoTab);

		render();
	}

	public void setClanData(ClanData next)
	{
		this.data = next;
		render();
	}

	private void render()
	{
		calendar.removeAll();
		competitions.removeAll();

		String gate = gateMessage(data);
		if (gate != null)
		{
			calendar.add(Ui.empty(gate));
			competitions.add(Ui.empty(gate));
		}
		else
		{
			if (data.getMessage() != null)
			{
				calendar.add(Ui.wrapped(data.getMessage(),
					data.getStatus() == ClanData.Status.ERROR ? Palette.NEGATIVE : Palette.POSITIVE));
				calendar.add(Ui.vgap(4));
			}
			ClanDtos.Bootstrap b = data.getBootstrap();
			List<ClanDtos.EventDto> events = b == null ? null : b.events;
			if (events == null || events.isEmpty())
			{
				calendar.add(Ui.empty(Strings.NO_EVENTS));
			}
			else
			{
				for (ClanDtos.EventDto e : events)
				{
					calendar.add(eventCard(e));
					calendar.add(Ui.vgap(4));
				}
			}

			List<ClanDtos.Competition> comps = b == null ? null : b.competitions;
			if (comps == null || comps.isEmpty())
			{
				competitions.add(Ui.empty(Strings.NO_COMPETITIONS));
			}
			else
			{
				for (ClanDtos.Competition c : comps)
				{
					competitions.add(competitionCard(c));
					competitions.add(Ui.vgap(4));
				}
			}
		}

		revalidate();
		repaint();
	}

	/** Null when the clan data can be shown; otherwise why not. Shared with the Home tab. */
	@Nullable
	static String gateMessage(ClanData data)
	{
		switch (data.getStatus())
		{
			case DISABLED:
				return Strings.CONNECT_HINT;
			case NOT_IN_CLAN:
				return Strings.NOT_IN_BRAZIL_CHAT;
			case CONNECTING:
				return Strings.CONNECTING;
			case ERROR:
				return data.getBootstrap() == null ? data.getMessage() : null;
			default:
				return null;
		}
	}

	private JPanel eventCard(ClanDtos.EventDto e)
	{
		ClanDtos.MySignup mine = e.mySignup;
		boolean active = mine != null && mine.isActive();
		boolean removed = mine != null && "removed".equals(mine.status);
		boolean hasFee = e.entryFee != null && e.entryFee > 0;

		Color accent = removed ? Palette.NEGATIVE : active ? Palette.BRAND_GREEN : Palette.MUTED;
		JPanel card = Ui.accentCard(accent);
		card.add(Ui.title(e.name, Palette.TEXT));

		String when = when(e.startDate);
		if (when != null)
		{
			card.add(Ui.small(when, Palette.MUTED));
		}
		if (e.description != null && !e.description.isEmpty())
		{
			card.add(Ui.vgap(2));
			card.add(Ui.wrapped(e.description, Palette.TEXT));
		}
		card.add(Ui.vgap(4));

		if (e.usesSignups)
		{
			card.add(Ui.statRow(Strings.LABEL_FEE, hasFee ? Strings.gp(e.entryFee) : Strings.FREE));
			card.add(Ui.statRow(e.maxSlots != null ? Strings.LABEL_SLOTS : Strings.LABEL_SIGNED_UP,
				e.maxSlots != null ? e.signedUpCount + "/" + e.maxSlots : String.valueOf(e.signedUpCount)));
			String deadline = when(e.signupDeadline);
			if (deadline != null && e.signupsOpen)
			{
				card.add(Ui.statRow(Strings.LABEL_DEADLINE, deadline));
			}
		}
		if (e.password != null)
		{
			card.add(Ui.statRow(Strings.LABEL_PASSWORD, e.password, Palette.BRAND_GOLD));
		}

		if (mine != null && !"cancelled".equals(mine.status))
		{
			card.add(Ui.vgap(4));
			if (removed)
			{
				card.add(Ui.badge(Strings.STATUS_REMOVED, Palette.NEGATIVE));
				if (mine.removedReason != null)
				{
					card.add(Ui.wrapped(mine.removedReason, Palette.MUTED));
				}
			}
			else if (hasFee)
			{
				card.add(mine.feePaid
					? Ui.badge(Strings.STATUS_PAID, Palette.POSITIVE)
					: Ui.badge(Strings.STATUS_PAYMENT_PENDING, Palette.WARNING));
			}
			else
			{
				card.add(Ui.badge(Strings.STATUS_SIGNED_UP, Palette.POSITIVE));
			}
		}

		if (e.usesSignups && !removed)
		{
			card.add(Ui.vgap(4));
			if (active)
			{
				card.add(row(smallButton(Strings.CANCEL_SIGNUP, () -> actions.cancelSignUp(e.id))));
			}
			else if (e.signupsOpen)
			{
				card.add(row(smallButton(Strings.SIGN_UP, () -> actions.signUp(e.id))));
			}
			else
			{
				card.add(Ui.small(Strings.SIGNUPS_CLOSED, Palette.MUTED));
			}
		}
		return card;
	}

	private static JPanel competitionCard(ClanDtos.Competition c)
	{
		JPanel card = Ui.card();
		card.add(Ui.title(c.title, Palette.TEXT));
		Instant now = Instant.now();
		Instant start = parse(c.startsAt);
		Instant end = parse(c.endsAt);
		if (start != null && start.isAfter(now))
		{
			card.add(Ui.small(Strings.EVENT_STARTS_IN + Strings.duration(Duration.between(now, start)), Palette.MUTED));
		}
		else if (end != null)
		{
			card.add(Ui.small(Strings.EVENT_ENDS_IN + Strings.duration(Duration.between(now, end)), Palette.MUTED));
		}
		if (c.top != null)
		{
			for (ClanDtos.Standing s : c.top)
			{
				card.add(Ui.statRow(s.rank + ". " + s.rsn, Strings.gp(s.gained)));
			}
		}
		if (c.me != null)
		{
			card.add(Ui.statRow("Você: #" + c.me.rank, Strings.gp(c.me.gained), Palette.BRAND_GOLD));
		}
		return card;
	}

	@Nullable
	private static String when(@Nullable String iso)
	{
		Instant at = parse(iso);
		return at == null ? null : at.atZone(Strings.BRASILIA).format(Strings.DATE_TIME) + " (Brasília)";
	}

	@Nullable
	private static Instant parse(@Nullable String iso)
	{
		if (iso == null)
		{
			return null;
		}
		try
		{
			return Instant.parse(iso);
		}
		catch (DateTimeParseException e)
		{
			return null;
		}
	}

	private JButton smallButton(String text, Runnable action)
	{
		JButton b = Ui.button(text, action);
		b.setEnabled(!data.isBusy());
		return b;
	}

	static JPanel row(JButton... buttons)
	{
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		row.setOpaque(false);
		row.setAlignmentX(LEFT_ALIGNMENT);
		for (JButton b : buttons)
		{
			row.add(b);
		}
		return row;
	}
}
