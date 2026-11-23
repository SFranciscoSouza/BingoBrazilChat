package com.brazilchat.ui;

import com.brazilchat.clan.ClanSnapshot;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.components.IconTextField;

/**
 * Clã: who is online in the clan channel right now, with rank and world, and a search box.
 * EDT only.
 */
public class ClanTab extends JPanel
{
	private final IconTextField search = new IconTextField();
	private final JLabel countLabel = Ui.small(" ", Palette.MUTED);
	private final JPanel list = Ui.column();

	private final java.util.function.Function<net.runelite.api.clan.ClanTitle, java.awt.image.BufferedImage> rankIcon;
	private ClanSnapshot clan = ClanSnapshot.loggedOut();

	public ClanTab(java.util.function.Function<net.runelite.api.clan.ClanTitle, java.awt.image.BufferedImage> rankIcon)
	{
		this.rankIcon = rankIcon;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(Palette.BACKGROUND);

		add(Ui.sectionHeader(Strings.SECTION_ONLINE_MEMBERS));

		search.setIcon(IconTextField.Icon.SEARCH);
		search.setPreferredSize(new Dimension(0, 30));
		search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		search.setBackground(Palette.CARD);
		search.setHoverBackgroundColor(Palette.CARD_HOVER);
		search.setToolTipText(Strings.SEARCH_PLACEHOLDER);
		search.setAlignmentX(LEFT_ALIGNMENT);
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				render();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				render();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				render();
			}
		});
		add(search);
		add(Ui.vgap(4));
		add(countLabel);
		add(Ui.vgap(4));
		add(list);

		render();
	}

	public void setClan(ClanSnapshot snapshot)
	{
		this.clan = snapshot;
		render();
	}

	private void render()
	{
		list.removeAll();

		if (!clan.isLoggedIn())
		{
			countLabel.setText(" ");
			list.add(Ui.empty(Strings.NOT_LOGGED_IN));
		}
		else if (!clan.inClan())
		{
			countLabel.setText(" ");
			list.add(Ui.empty(Strings.NOT_IN_CLAN));
		}
		else
		{
			List<ClanSnapshot.OnlineMember> online = clan.getOnline();
			countLabel.setText(Strings.onlineCount(online.size()));

			String query = search.getText() == null
				? ""
				: search.getText().trim().toLowerCase(Locale.ROOT);
			int shown = 0;
			for (ClanSnapshot.OnlineMember member : online)
			{
				if (!query.isEmpty() && !member.getName().toLowerCase(Locale.ROOT).contains(query))
				{
					continue;
				}
				list.add(memberRow(member));
				list.add(Ui.vgap(2));
				shown++;
			}
			if (shown == 0)
			{
				list.add(Ui.empty(online.isEmpty() ? Strings.NO_MEMBERS_ONLINE : Strings.NO_MEMBERS_MATCH));
			}
		}

		revalidate();
		repaint();
	}

	private JPanel memberRow(ClanSnapshot.OnlineMember member)
	{
		JPanel row = new JPanel(new BorderLayout(Ui.GAP, 0));
		row.setBackground(Palette.CARD);
		row.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		row.setAlignmentX(LEFT_ALIGNMENT);

		// The game's rank icon: gold key (Owner), silver key (Deputy Owner), bronze key
		// (Administrator), and the clan's chosen icon for every other title.
		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(13, 13));
		java.awt.image.BufferedImage image = member.getTitleId() < 0 || member.getTitle() == null ? null
			: rankIcon.apply(new net.runelite.api.clan.ClanTitle(member.getTitleId(), member.getTitle()));
		if (image != null)
		{
			icon.setIcon(new javax.swing.ImageIcon(image));
		}
		icon.setVerticalAlignment(JLabel.TOP);
		row.add(icon, BorderLayout.WEST);

		JPanel left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
		left.setOpaque(false);
		left.add(Ui.small(member.getName(), Palette.TEXT));
		left.add(Ui.small(member.getTitle() == null ? " " : Strings.rankLabel(member.getTitle()),
			member.isStaff() ? Palette.STAFF : Palette.MUTED));
		row.add(left, BorderLayout.CENTER);

		JPanel right = new JPanel();
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
		right.setOpaque(false);
		right.add(Ui.small(Strings.world(member.getWorld()), Palette.MUTED));
		row.add(right, BorderLayout.EAST);

		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		return row;
	}
}
