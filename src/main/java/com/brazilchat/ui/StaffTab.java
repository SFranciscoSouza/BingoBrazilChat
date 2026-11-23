package com.brazilchat.ui;

import com.brazilchat.clan.ClanData;
import com.brazilchat.clan.api.ClanDtos;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.runelite.client.ui.FontManager;

/**
 * Staff: sign-up management (fee paid, remove, open/close), roster freshness, inactive members and
 * recent promotions. Only shown to Administrator+ with a staff key; the server re-checks both on
 * every call. EDT only.
 */
public class StaffTab extends JPanel
{
	private final ClanActions actions;
	private final JPanel signupsSection = Ui.column();
	private final JPanel overviewSection = Ui.column();
	private final JComboBox<String> eventPicker = new JComboBox<>();

	private ClanData data = ClanData.disabled();
	private boolean hasKey;
	private List<ClanDtos.StaffEvent> events = List.of();
	private String selectedEventId;
	private boolean updatingPicker;

	public StaffTab(ClanActions actions)
	{
		this.actions = actions;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(Palette.BACKGROUND);

		eventPicker.setAlignmentX(LEFT_ALIGNMENT);
		eventPicker.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		eventPicker.addActionListener(e ->
		{
			if (updatingPicker)
			{
				return;
			}
			int i = eventPicker.getSelectedIndex();
			selectedEventId = i >= 0 && i < events.size() ? events.get(i).id : null;
			renderSignups();
		});

		add(Ui.sectionHeader(Strings.SECTION_STAFF_SIGNUPS));
		add(signupsSection);
		add(Ui.vgap(Ui.SECTION_GAP));
		add(overviewSection);

		render();
	}

	public void setClanData(ClanData next, boolean hasStaffKey)
	{
		this.data = next;
		this.hasKey = hasStaffKey;
		render();
	}

	private void render()
	{
		ClanDtos.StaffEvents se = data.getStaffEvents();
		events = se == null || se.events == null ? List.of() : se.events;

		updatingPicker = true;
		eventPicker.removeAllItems();
		int selectedIndex = -1;
		for (int i = 0; i < events.size(); i++)
		{
			eventPicker.addItem(events.get(i).name);
			if (events.get(i).id.equals(selectedEventId))
			{
				selectedIndex = i;
			}
		}
		if (selectedIndex < 0 && !events.isEmpty())
		{
			selectedIndex = 0;
			selectedEventId = events.get(0).id;
		}
		eventPicker.setSelectedIndex(selectedIndex);
		updatingPicker = false;

		renderSignups();
		renderOverview();
	}

	private void renderSignups()
	{
		signupsSection.removeAll();

		String gate = EventsTab.gateMessage(data);
		if (!hasKey)
		{
			signupsSection.add(Ui.empty(Strings.STAFF_NEEDS_KEY));
		}
		else if (gate != null)
		{
			signupsSection.add(Ui.empty(gate));
		}
		else
		{
			if (data.getStaffMessage() != null)
			{
				signupsSection.add(Ui.wrapped(data.getStaffMessage(), Palette.NEGATIVE));
				signupsSection.add(Ui.vgap(4));
			}
			if (data.getMessage() != null && data.getStatus() == ClanData.Status.ERROR)
			{
				signupsSection.add(Ui.wrapped(data.getMessage(), Palette.NEGATIVE));
				signupsSection.add(Ui.vgap(4));
			}
			if (events.isEmpty())
			{
				signupsSection.add(Ui.empty(Strings.NO_EVENTS));
			}
			else
			{
				signupsSection.add(eventPicker);
				signupsSection.add(Ui.vgap(6));
				ClanDtos.StaffEvent event = events.stream()
					.filter(e -> e.id.equals(selectedEventId)).findFirst().orElse(events.get(0));
				renderEvent(event);
			}
		}

		signupsSection.revalidate();
		signupsSection.repaint();
	}

	private void renderEvent(ClanDtos.StaffEvent event)
	{
		JPanel totals = Ui.card();
		ClanDtos.Totals t = event.totals;
		String signed = event.maxSlots != null ? t.signedUp + "/" + event.maxSlots : String.valueOf(t.signedUp);
		totals.add(Ui.statRow(Strings.LABEL_SIGNED_UP, signed));
		boolean hasFee = event.entryFee != null && event.entryFee > 0;
		if (hasFee)
		{
			totals.add(Ui.statRow(Strings.LABEL_FEE, Strings.gp(event.entryFee)));
			totals.add(Ui.statRow("Pagos", String.valueOf(t.paid), Palette.POSITIVE));
			totals.add(Ui.statRow("Pendentes", String.valueOf(t.pending), t.pending > 0 ? Palette.WARNING : Palette.TEXT));
			totals.add(Ui.statRow("Arrecadado", Strings.gp(t.collected)));
		}
		totals.add(Ui.vgap(4));
		totals.add(EventsTab.row(button(event.signupsOpen ? Strings.CLOSE_SIGNUPS : Strings.OPEN_SIGNUPS,
			() -> actions.staffSetSignupsOpen(event.id, !event.signupsOpen))));
		signupsSection.add(totals);
		signupsSection.add(Ui.vgap(6));

		if (event.signups == null || event.signups.isEmpty())
		{
			signupsSection.add(Ui.empty(Strings.NO_SIGNUPS));
			return;
		}
		for (ClanDtos.StaffSignup s : event.signups)
		{
			signupsSection.add(signupCard(s, hasFee));
			signupsSection.add(Ui.vgap(3));
		}
	}

	private JPanel signupCard(ClanDtos.StaffSignup s, boolean hasFee)
	{
		boolean removed = "removed".equals(s.status);
		JPanel card = Ui.accentCard(removed ? Palette.NEGATIVE : s.feePaid || !hasFee ? Palette.POSITIVE : Palette.WARNING);
		card.add(Ui.title(s.rsn, removed ? Palette.MUTED : Palette.TEXT));
		if (s.note != null)
		{
			card.add(Ui.wrapped(s.note, Palette.MUTED));
		}
		if (removed)
		{
			card.add(Ui.small(Strings.STATUS_REMOVED + (s.removedReason == null ? "" : ": " + s.removedReason), Palette.NEGATIVE));
			card.add(EventsTab.row(button(Strings.RESTORE, () -> actions.staffSignupAction(s.id, "restore", null))));
			return card;
		}
		if (hasFee)
		{
			card.add(Ui.small(s.feePaid
				? Strings.STATUS_PAID + (s.feeMarkedBy == null ? "" : " (" + s.feeMarkedBy + ")")
				: Strings.STATUS_PAYMENT_PENDING, s.feePaid ? Palette.POSITIVE : Palette.WARNING));
		}
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		buttons.setOpaque(false);
		buttons.setAlignmentX(LEFT_ALIGNMENT);
		if (hasFee)
		{
			buttons.add(button(s.feePaid ? Strings.MARK_UNPAID : Strings.MARK_PAID,
				() -> actions.staffSignupAction(s.id, s.feePaid ? "mark_unpaid" : "mark_paid", null)));
		}
		buttons.add(button(Strings.REMOVE, () ->
		{
			String reason = JOptionPane.showInputDialog(this, Strings.REMOVE_REASON_PROMPT + "\n" + s.rsn,
				Strings.REMOVE, JOptionPane.QUESTION_MESSAGE);
			if (reason != null)
			{
				actions.staffSignupAction(s.id, "remove", reason.trim().isEmpty() ? null : reason.trim());
			}
		}));
		card.add(Ui.vgap(2));
		card.add(buttons);
		return card;
	}

	private void renderOverview()
	{
		overviewSection.removeAll();
		ClanDtos.Overview o = data.getOverview();
		if (o != null && hasKey)
		{
			renderAnnouncements(o.announcements);
			overviewSection.add(Ui.vgap(Ui.SECTION_GAP));

			overviewSection.add(Ui.sectionHeader(Strings.SECTION_ROSTER));
			JPanel roster = Ui.card();
			roster.add(Ui.statRow(Strings.LABEL_MEMBERS, String.valueOf(o.memberCount)));
			if (o.syncs != null)
			{
				for (ClanDtos.Sync s : o.syncs)
				{
					roster.add(Ui.statRow("game".equals(s.source) ? "Lista do jogo" : "Wise Old Man", ago(s.synced_at)));
				}
			}
			overviewSection.add(roster);
			overviewSection.add(Ui.vgap(Ui.SECTION_GAP));

			overviewSection.add(Ui.sectionHeader(Strings.SECTION_WORKLIST));
			if (o.worklist == null || o.worklist.isEmpty())
			{
				overviewSection.add(Ui.empty(Strings.NO_WORKLIST));
			}
			for (ClanDtos.WorkItem w : o.worklist == null ? List.<ClanDtos.WorkItem>of() : o.worklist)
			{
				boolean promote = "promote".equals(w.kind);
				JPanel card = Ui.accentCard(promote ? Palette.POSITIVE : Palette.WARNING);
				card.add(Ui.title(w.rsn, Palette.TEXT));
				card.add(Ui.statRow(promote ? Strings.PROMOTE : Strings.POSSIBLE, w.from + " > " + w.to,
					promote ? Palette.POSITIVE : Palette.WARNING));
				if (w.maybeHigher != null)
				{
					card.add(Ui.small("Talvez " + w.maybeHigher, Palette.MUTED));
				}
				if (w.unknown != null && !w.unknown.isEmpty())
				{
					card.add(Ui.wrapped("Verificar: " + String.join(", ", w.unknown), Palette.MUTED));
				}
				overviewSection.add(card);
				overviewSection.add(Ui.vgap(3));
			}
			overviewSection.add(Ui.vgap(Ui.SECTION_GAP));

			overviewSection.add(Ui.sectionHeader(Strings.SECTION_MVP));
			JPanel mvp = Ui.card();
			mvp.add(Ui.statRow(Strings.MVP_PVM, top(o.mvp == null ? null : o.mvp.pvm, "EHB"), Palette.BRAND_GOLD));
			mvp.add(Ui.statRow(Strings.MVP_SKILLER, top(o.mvp == null ? null : o.mvp.skiller, "EHP"), Palette.BRAND_GOLD));
			overviewSection.add(mvp);
			overviewSection.add(Ui.vgap(Ui.SECTION_GAP));

			overviewSection.add(Ui.sectionHeader(Strings.SECTION_PROMOTIONS));
			if (o.recentPromotions == null || o.recentPromotions.isEmpty())
			{
				overviewSection.add(Ui.empty(Strings.NO_PROMOTIONS));
			}
			else
			{
				JPanel card = Ui.card();
				for (ClanDtos.Promotion p : o.recentPromotions)
				{
					card.add(Ui.statRow(p.rsn, Strings.rankLabel(p.title), Palette.BRAND_GOLD));
				}
				overviewSection.add(card);
			}
			overviewSection.add(Ui.vgap(Ui.SECTION_GAP));

			overviewSection.add(Ui.sectionHeader(Strings.SECTION_INACTIVE));
			if (o.inactive == null || o.inactive.isEmpty())
			{
				overviewSection.add(Ui.empty(Strings.NOBODY_INACTIVE));
			}
			else
			{
				JPanel card = Ui.card();
				o.inactive.stream().limit(25).forEach(m ->
					card.add(Ui.statRow(m.rsn, m.daysInactive + " dias", Palette.WARNING)));
				overviewSection.add(card);
			}
		}
		overviewSection.revalidate();
		overviewSection.repaint();
	}

	/** Create, edit and delete the announcements every member sees on Início. */
	private void renderAnnouncements(@Nullable List<ClanDtos.Announcement> announcements)
	{
		overviewSection.add(Ui.sectionHeader(Strings.SECTION_STAFF_ANNOUNCEMENTS));
		overviewSection.add(EventsTab.row(button(Strings.NEW_ANNOUNCEMENT, () -> editAnnouncement(null))));
		overviewSection.add(Ui.vgap(4));
		if (announcements == null || announcements.isEmpty())
		{
			overviewSection.add(Ui.empty(Strings.NO_ANNOUNCEMENTS));
			return;
		}
		for (ClanDtos.Announcement a : announcements)
		{
			JPanel card = Ui.accentCard(a.pinned ? Palette.BRAND_GOLD : Palette.BRAND_GREEN);
			card.add(Ui.title(a.title, Palette.TEXT));
			if (a.body != null && !a.body.isEmpty())
			{
				card.add(Ui.wrapped(a.body, Palette.MUTED));
			}
			String meta = (a.createdBy == null ? "" : a.createdBy + " - ") + ago(a.createdAt)
				+ (a.expiresAt == null ? "" : " - expira " + ago(a.expiresAt));
			card.add(Ui.small(meta, Palette.MUTED));
			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
			buttons.setOpaque(false);
			buttons.setAlignmentX(LEFT_ALIGNMENT);
			buttons.add(button(Strings.EDIT, () -> editAnnouncement(a)));
			buttons.add(button(Strings.DELETE, () ->
			{
				int ok = JOptionPane.showConfirmDialog(this, Strings.DELETE + " \"" + a.title + "\"?",
					Strings.DELETE, JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				if (ok == JOptionPane.OK_OPTION)
				{
					actions.deleteAnnouncement(a.id);
				}
			}));
			card.add(Ui.vgap(2));
			card.add(buttons);
			overviewSection.add(card);
			overviewSection.add(Ui.vgap(3));
		}
	}

	/** A small form for a new announcement (existing == null) or for editing one. */
	private void editAnnouncement(@Nullable ClanDtos.Announcement existing)
	{
		javax.swing.JTextField title = new javax.swing.JTextField(existing == null ? "" : existing.title, 24);
		javax.swing.JTextArea body = new javax.swing.JTextArea(existing == null || existing.body == null ? "" : existing.body, 5, 24);
		body.setLineWrap(true);
		body.setWrapStyleWord(true);
		javax.swing.JCheckBox pinned = new javax.swing.JCheckBox(Strings.ANNOUNCEMENT_PINNED, existing != null && existing.pinned);

		JPanel form = new JPanel();
		form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
		form.add(new javax.swing.JLabel(Strings.ANNOUNCEMENT_TITLE));
		form.add(title);
		form.add(javax.swing.Box.createVerticalStrut(6));
		form.add(new javax.swing.JLabel(Strings.ANNOUNCEMENT_BODY));
		form.add(new javax.swing.JScrollPane(body));
		form.add(javax.swing.Box.createVerticalStrut(6));
		form.add(pinned);

		while (true)
		{
			int choice = JOptionPane.showConfirmDialog(this, form,
				existing == null ? Strings.NEW_ANNOUNCEMENT : Strings.EDIT, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
			if (choice != JOptionPane.OK_OPTION)
			{
				return;
			}
			String t = title.getText().trim();
			if (t.isEmpty())
			{
				JOptionPane.showMessageDialog(this, Strings.ANNOUNCEMENT_TITLE_REQUIRED);
				continue;
			}
			String b = body.getText().trim();
			if (existing == null)
			{
				actions.createAnnouncement(t, b.isEmpty() ? null : b, pinned.isSelected());
			}
			else
			{
				actions.updateAnnouncement(existing.id, t, b.isEmpty() ? null : b, pinned.isSelected());
			}
			return;
		}
	}

	private JButton button(String text, Runnable action)
	{
		JButton b = Ui.button(text, action);
		b.setFont(FontManager.getRunescapeSmallFont());
		b.setMargin(new Insets(1, 4, 1, 4));
		b.setEnabled(!data.isBusy());
		return b;
	}

	private static String top(@Nullable ClanDtos.Top t, String metric)
	{
		if (t == null)
		{
			return Strings.NO_DATA;
		}
		return t.rsn + " (+" + Math.round(t.gained) + " " + metric + ")";
	}

	private static String ago(@Nullable String iso)
	{
		if (iso == null)
		{
			return "nunca";
		}
		try
		{
			Instant at = java.time.OffsetDateTime.parse(iso).toInstant();
			return at.atZone(Strings.BRASILIA).format(Strings.DATE_TIME);
		}
		catch (DateTimeParseException e)
		{
			return iso;
		}
	}
}
