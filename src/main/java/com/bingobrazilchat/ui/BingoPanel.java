package com.bingobrazilchat.ui;

import com.bingobrazilchat.api.ApiDtos;
import com.bingobrazilchat.event.EventWindow;
import com.bingobrazilchat.submit.QueuedSubmission;
import com.bingobrazilchat.submit.SubmissionState;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * The side panel: connection state, a countdown, the team scoreboard, and this client's queue.
 *
 * <p>Everything here runs on the EDT. {@link #setState(PanelState)} is the only entry point from
 * the plugin executor and marshals itself across.
 */
public class BingoPanel extends PluginPanel
{
	private static final Color POSITIVE = new Color(76, 175, 80);
	private static final Color WARNING = new Color(255, 167, 38);
	private static final Color NEGATIVE = new Color(229, 115, 115);
	private static final Color MUTED = new Color(150, 150, 150);

	private final JLabel statusLabel = new JLabel();
	private final JLabel detailLabel = new JLabel();
	private final JLabel eventNameLabel = new JLabel();
	private final JLabel countdownLabel = new JLabel();
	private final JPanel teamsPanel = new JPanel();
	private final JPanel queuePanel = new JPanel();
	private final JButton clearButton = new JButton("Clear finished");

	private final Timer countdownTimer;

	private PanelState state = PanelState.initial();

	public BingoPanel(Runnable onClearFinished)
	{
		super(false);

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		content.add(buildHeader());
		content.add(Box.createRigidArea(new Dimension(0, 12)));
		content.add(buildSectionLabel("Teams"));
		content.add(Box.createRigidArea(new Dimension(0, 4)));

		teamsPanel.setLayout(new BoxLayout(teamsPanel, BoxLayout.Y_AXIS));
		teamsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		teamsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(teamsPanel);

		content.add(Box.createRigidArea(new Dimension(0, 12)));
		content.add(buildSectionLabel("Your drops"));
		content.add(Box.createRigidArea(new Dimension(0, 4)));

		queuePanel.setLayout(new BoxLayout(queuePanel, BoxLayout.Y_AXIS));
		queuePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		queuePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(queuePanel);

		content.add(Box.createRigidArea(new Dimension(0, 8)));
		clearButton.setFocusPainted(false);
		clearButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		clearButton.addActionListener(e -> onClearFinished.run());
		content.add(clearButton);

		add(new javax.swing.JScrollPane(content,
			javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

		// The countdown is the one thing that has to change without any new data arriving.
		countdownTimer = new Timer(1000, e -> refreshCountdown());
		countdownTimer.start();

		render();
	}

	/** Safe to call from any thread. */
	public void setState(PanelState next)
	{
		SwingUtilities.invokeLater(() ->
		{
			this.state = next;
			render();
		});
	}

	public void shutdown()
	{
		countdownTimer.stop();
	}

	// ------------------------------------------------------------------------------------------

	private JPanel buildHeader()
	{
		JPanel header = new JPanel();
		header.setLayout(new GridLayout(0, 1, 0, 2));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		header.setAlignmentX(Component.LEFT_ALIGNMENT);

		statusLabel.setFont(FontManager.getRunescapeBoldFont());
		detailLabel.setFont(FontManager.getRunescapeSmallFont());
		detailLabel.setForeground(MUTED);
		eventNameLabel.setFont(FontManager.getRunescapeSmallFont());
		eventNameLabel.setForeground(Color.WHITE);
		countdownLabel.setFont(FontManager.getRunescapeSmallFont());
		countdownLabel.setForeground(MUTED);

		header.add(statusLabel);
		header.add(detailLabel);
		header.add(eventNameLabel);
		header.add(countdownLabel);
		return header;
	}

	private static JLabel buildSectionLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Color.WHITE);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private void render()
	{
		statusLabel.setText(state.getConnection().getLabel());
		statusLabel.setForeground(colourFor(state.getConnection()));

		String detail = state.getDetail();
		if (state.getConnection() == PanelState.Connection.NOT_CONFIGURED && detail == null)
		{
			detail = "Enter your event token in the plugin settings";
		}
		detailLabel.setText(detail == null ? " " : detail);

		EventWindow window = state.getWindow();
		eventNameLabel.setText(window == null ? " " : window.getName());

		refreshCountdown();
		renderTeams();
		renderQueue();

		revalidate();
		repaint();
	}

	private void renderTeams()
	{
		teamsPanel.removeAll();

		List<ApiDtos.TeamRow> teams = state.getTeams();
		if (teams.isEmpty())
		{
			teamsPanel.add(mutedRow("No scores yet"));
		}
		else
		{
			for (ApiDtos.TeamRow team : teams)
			{
				teamsPanel.add(buildTeamRow(team));
				teamsPanel.add(Box.createRigidArea(new Dimension(0, 3)));
			}
		}

		teamsPanel.revalidate();
		teamsPanel.repaint();
	}

	private JPanel buildTeamRow(ApiDtos.TeamRow team)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

		JLabel name = new JLabel(team.rank + ". " + team.name);
		name.setFont(FontManager.getRunescapeSmallFont());
		name.setForeground(parseColour(team.color));

		JLabel points = new JLabel(team.points + " pts");
		points.setFont(FontManager.getRunescapeSmallFont());
		points.setForeground(Color.WHITE);

		row.add(name, BorderLayout.CENTER);
		row.add(points, BorderLayout.EAST);
		return row;
	}

	private void renderQueue()
	{
		queuePanel.removeAll();

		List<QueuedSubmission> queue = state.getQueue();
		if (queue.isEmpty())
		{
			queuePanel.add(mutedRow("Nothing sent yet"));
		}
		else
		{
			// Newest first: the drop someone just got is the one they want to see.
			for (int i = queue.size() - 1; i >= 0; i--)
			{
				queuePanel.add(buildQueueRow(queue.get(i)));
				queuePanel.add(Box.createRigidArea(new Dimension(0, 3)));
			}
		}

		clearButton.setEnabled(queue.stream().anyMatch(s -> s.getState().isTerminal()));

		queuePanel.revalidate();
		queuePanel.repaint();
	}

	private JPanel buildQueueRow(QueuedSubmission submission)
	{
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		String itemName = submission.getItemDisplayName() == null
			? submission.getItemNameRaw()
			: submission.getItemDisplayName();

		JLabel title = new JLabel(submission.getRsn() + " - " + itemName);
		title.setFont(FontManager.getRunescapeSmallFont());
		title.setForeground(Color.WHITE);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);

		String statusText = submission.getState().getLabel();
		if (submission.getStatusDetail() != null && !submission.getStatusDetail().isEmpty())
		{
			statusText = statusText + " - " + submission.getStatusDetail();
		}

		JLabel status = new JLabel(statusText);
		status.setFont(FontManager.getRunescapeSmallFont());
		status.setForeground(colourFor(submission.getState()));
		status.setAlignmentX(Component.LEFT_ALIGNMENT);

		row.add(title);
		row.add(status);
		return row;
	}

	private void refreshCountdown()
	{
		EventWindow window = state.getWindow();
		if (window == null)
		{
			countdownLabel.setText(" ");
			return;
		}

		Instant now = Instant.now();
		if (window.isBeforeStart(now))
		{
			countdownLabel.setText("Starts in " + humanise(Duration.between(now, window.getStart())));
		}
		else if (window.isAfterEnd(now))
		{
			countdownLabel.setText("Event finished");
		}
		else if (window.getEnd() != null)
		{
			countdownLabel.setText("Ends in " + humanise(Duration.between(now, window.getEnd())));
		}
		else
		{
			countdownLabel.setText("Running");
		}
	}

	private static String humanise(Duration d)
	{
		long seconds = Math.max(0L, d.getSeconds());
		long days = seconds / 86_400L;
		long hours = (seconds % 86_400L) / 3_600L;
		long minutes = (seconds % 3_600L) / 60L;

		if (days > 0)
		{
			return days + "d " + hours + "h";
		}
		if (hours > 0)
		{
			return hours + "h " + minutes + "m";
		}
		return minutes + "m " + (seconds % 60L) + "s";
	}

	private static JLabel mutedRow(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(MUTED);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private static Color colourFor(PanelState.Connection connection)
	{
		switch (connection)
		{
			case ONLINE:
				return POSITIVE;
			case UNAUTHORIZED:
			case OFFLINE:
				return NEGATIVE;
			case NO_EVENT:
			case CONNECTING:
				return WARNING;
			default:
				return MUTED;
		}
	}

	private static Color colourFor(SubmissionState state)
	{
		switch (state)
		{
			case DONE:
			case DUPLICATE:
				return POSITIVE;
			case FAILED:
				return NEGATIVE;
			case REJECTED:
				return WARNING;
			default:
				return MUTED;
		}
	}

	private static Color parseColour(String hex)
	{
		if (hex == null || hex.isEmpty())
		{
			return Color.WHITE;
		}
		try
		{
			return Color.decode(hex.startsWith("#") ? hex : "#" + hex);
		}
		catch (NumberFormatException e)
		{
			return Color.WHITE;
		}
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(PluginPanel.PANEL_WIDTH, super.getPreferredSize().height);
	}
}
