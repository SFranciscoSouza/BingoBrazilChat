package com.brazilchat.ui;

import com.brazilchat.bingo.ui.BingoState;
import com.brazilchat.bingo.ui.BingoTab;
import com.brazilchat.clan.ClanSnapshot;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

/**
 * The side panel shell: a fixed header (plugin name, who you are) above a row of tabs. Each tab is
 * its own class built from {@link Ui}; this class only owns layout, the shared one-second timer,
 * and marshalling state onto the EDT.
 */
public class BrazilChatPanel extends PluginPanel
{
	private final JLabel headerName = Ui.title(Strings.PLUGIN_NAME, Palette.BRAND_GOLD);
	private final JLabel headerWho = Ui.small(" ", Palette.MUTED);

	private final HomeTab homeTab;
	private final ClanTab clanTab;
	private final BingoTab bingoTab;
	private final EventsTab eventsTab;
	private final StaffTab staffTab;
	private final MaterialTab eventsMaterialTab;
	private final MaterialTab staffMaterialTab;
	private final MaterialTab homeMaterialTab;
	private final java.util.function.BooleanSupplier hasStaffKey;
	private ClanSnapshot lastClan = ClanSnapshot.loggedOut();
	private com.brazilchat.clan.ClanData lastClanData = com.brazilchat.clan.ClanData.disabled();
	private final MaterialTabGroup tabGroup;
	private final Timer timer;

	public BrazilChatPanel(Runnable onClearFinished, ClanActions actions,
		java.util.function.BooleanSupplier hasStaffKey,
		java.util.function.Function<net.runelite.api.clan.ClanTitle, java.awt.image.BufferedImage> rankIcon)
	{
		super(false);
		this.hasStaffKey = hasStaffKey;
		setLayout(new BorderLayout());
		setBackground(Palette.BACKGROUND);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

		JPanel display = new JPanel();
		display.setBackground(Palette.BACKGROUND);
		tabGroup = new MaterialTabGroup(display);

		homeTab = new HomeTab(this::openEvents);
		clanTab = new ClanTab(rankIcon);
		bingoTab = new BingoTab(onClearFinished);
		eventsTab = new EventsTab(actions, bingoTab);
		staffTab = new StaffTab(actions);

		homeMaterialTab = new MaterialTab(Strings.TAB_HOME, tabGroup, Ui.scroll(homeTab));
		MaterialTab clanMaterialTab = new MaterialTab(Strings.TAB_CLAN, tabGroup, Ui.scroll(clanTab));
		eventsMaterialTab = new MaterialTab(Strings.TAB_EVENTS, tabGroup, Ui.scroll(eventsTab));
		staffMaterialTab = new MaterialTab(Strings.TAB_STAFF, tabGroup, Ui.scroll(staffTab));
		tabGroup.addTab(homeMaterialTab);
		tabGroup.addTab(clanMaterialTab);
		tabGroup.addTab(eventsMaterialTab);
		tabGroup.addTab(staffMaterialTab);
		// Tighter than MaterialTabGroup's default 8px gap, which wraps the fourth tab out of sight.
		tabGroup.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 2, 0));
		// Only Administrator and above ever see the Staff tab (the server re-checks every call).
		tabGroup.remove(staffMaterialTab);
		tabGroup.select(homeMaterialTab);

		JPanel north = new JPanel();
		north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
		north.setBackground(Palette.BACKGROUND);
		north.add(headerName);
		north.add(headerWho);
		north.add(Ui.vgap(8));
		tabGroup.setAlignmentX(LEFT_ALIGNMENT);
		north.add(tabGroup);
		north.add(Ui.vgap(8));

		add(north, BorderLayout.NORTH);
		add(display, BorderLayout.CENTER);

		// Countdowns are the one thing that changes without any new data arriving.
		timer = new Timer(1000, e ->
		{
			homeTab.tick();
			bingoTab.tick();
		});
		timer.start();

		renderHeader(ClanSnapshot.loggedOut());
	}

	/** Safe to call from any thread. */
	public void setClan(ClanSnapshot snapshot)
	{
		SwingUtilities.invokeLater(() ->
		{
			lastClan = snapshot;
			renderHeader(snapshot);
			homeTab.setClan(snapshot);
			clanTab.setClan(snapshot);
			updateStaffVisibility();
		});
	}

	/** Safe to call from any thread. */
	public void setBingo(BingoState state)
	{
		SwingUtilities.invokeLater(() ->
		{
			homeTab.setBingo(state);
			bingoTab.setState(state);
		});
	}

	/** Safe to call from any thread. */
	public void setClanData(com.brazilchat.clan.ClanData data)
	{
		SwingUtilities.invokeLater(() ->
		{
			lastClanData = data;
			homeTab.setClanData(data);
			eventsTab.setClanData(data);
			updateStaffVisibility();
		});
	}

	private void updateStaffVisibility()
	{
		boolean show = lastClan.isStaff() && lastClan.inBrazilChat();
		boolean shown = staffMaterialTab.getParent() == tabGroup;
		if (shown != show)
		{
			if (show)
			{
				tabGroup.add(staffMaterialTab);
			}
			else
			{
				tabGroup.remove(staffMaterialTab);
			}
			if (!show && staffMaterialTab.isSelected())
			{
				tabGroup.select(homeMaterialTab);
			}
			tabGroup.revalidate();
			tabGroup.repaint();
		}
		staffTab.setClanData(lastClanData, hasStaffKey.getAsBoolean());
	}

	public void shutdown()
	{
		timer.stop();
	}

	private void openEvents()
	{
		tabGroup.select(eventsMaterialTab);
	}

	private void renderHeader(ClanSnapshot snapshot)
	{
		String who;
		if (!snapshot.isLoggedIn() || snapshot.getRsn() == null)
		{
			who = Strings.NOT_LOGGED_IN;
		}
		else if (snapshot.getRankTitle() != null)
		{
			who = snapshot.getRsn() + " · " + Strings.rankLabel(snapshot.getRankTitle());
		}
		else
		{
			who = snapshot.getRsn();
		}
		headerWho.setText(who);
		headerWho.setForeground(snapshot.isStaff() ? Palette.STAFF : Palette.MUTED);
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(PluginPanel.PANEL_WIDTH, super.getPreferredSize().height);
	}
}
