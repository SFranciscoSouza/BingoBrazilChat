package com.brazilchat.bingo.ui;

import com.brazilchat.bingo.api.ApiDtos;
import com.brazilchat.bingo.event.EventWindow;
import com.brazilchat.bingo.submit.QueuedSubmission;
import com.brazilchat.bingo.submit.SubmissionState;
import com.brazilchat.ui.Palette;
import com.brazilchat.ui.Strings;
import com.brazilchat.ui.Ui;
import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The bingo section of the Eventos tab: connection, countdown, team scoreboard and this client's
 * submission queue. EDT only -- the shell hands it each new {@link BingoState}.
 */
public class BingoTab extends JPanel
{
	private final JPanel statusCard = Ui.accentCard(Palette.MUTED);
	private final JLabel statusLabel = Ui.title(" ", Palette.MUTED);
	private final javax.swing.JTextArea detailLabel = Ui.wrapped(" ", Palette.MUTED);
	private final JLabel eventNameLabel = Ui.small(" ", Palette.TEXT);
	private final JLabel countdownLabel = Ui.small(" ", Palette.MUTED);
	private final JPanel teamsPanel = Ui.column();
	private final JPanel queuePanel = Ui.column();
	private final JButton clearButton;

	private BingoState state = BingoState.initial();

	public BingoTab(Runnable onClearFinished)
	{
		setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
		setBackground(Palette.BACKGROUND);
		setAlignmentX(LEFT_ALIGNMENT);

		add(Ui.sectionHeader(Strings.SECTION_BINGO));
		statusCard.add(statusLabel);
		statusCard.add(detailLabel);
		statusCard.add(Ui.vgap(4));
		statusCard.add(eventNameLabel);
		statusCard.add(countdownLabel);
		add(statusCard);

		add(Ui.vgap(Ui.SECTION_GAP));
		add(Ui.sectionHeader(Strings.SECTION_TEAMS));
		add(teamsPanel);

		add(Ui.vgap(Ui.SECTION_GAP));
		add(Ui.sectionHeader(Strings.SECTION_YOUR_DROPS));
		add(queuePanel);
		add(Ui.vgap(Ui.GAP));
		clearButton = Ui.button(Strings.CLEAR_FINISHED, onClearFinished);
		add(clearButton);

		render();
	}

	/** EDT only. */
	public void setState(BingoState next)
	{
		this.state = next;
		render();
	}

	/** EDT only; called by the shell's one-second timer. */
	public void tick()
	{
		countdownLabel.setText(countdownText(state.getWindow(), Instant.now()));
	}

	// ------------------------------------------------------------------------------------------

	private void render()
	{
		Color colour = colourFor(state.getConnection());
		statusLabel.setText(state.getConnection().getLabel());
		statusLabel.setForeground(colour);
		statusCard.setBorder(Ui.accentCard(colour).getBorder());

		String detail = state.getDetail();
		if (state.getConnection() == BingoState.Connection.NOT_CONFIGURED && detail == null)
		{
			detail = Strings.ENTER_TOKEN;
		}
		detailLabel.setText(detail == null ? " " : detail);

		EventWindow window = state.getWindow();
		eventNameLabel.setText(window == null ? " " : window.getName());
		tick();

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
			teamsPanel.add(Ui.empty(Strings.NO_SCORES));
		}
		for (ApiDtos.TeamRow team : teams)
		{
			teamsPanel.add(Ui.listRow(team.rank + ". " + team.name,
				Palette.parseHex(team.color, Palette.TEXT),
				team.points + Strings.POINTS_SUFFIX, Palette.TEXT));
			teamsPanel.add(Ui.vgap(3));
		}
	}

	private void renderQueue()
	{
		queuePanel.removeAll();
		List<QueuedSubmission> queue = state.getQueue();
		if (queue.isEmpty())
		{
			queuePanel.add(Ui.empty(Strings.NOTHING_SENT));
		}
		// Newest first: the drop someone just got is the one they want to see.
		for (int i = queue.size() - 1; i >= 0; i--)
		{
			queuePanel.add(buildQueueRow(queue.get(i)));
			queuePanel.add(Ui.vgap(3));
		}
		clearButton.setEnabled(queue.stream().anyMatch(s -> s.getState().isTerminal()));
	}

	private static JPanel buildQueueRow(QueuedSubmission submission)
	{
		JPanel row = Ui.card();
		String itemName = submission.getItemDisplayName() == null
			? submission.getItemNameRaw()
			: submission.getItemDisplayName();
		row.add(Ui.small(submission.getRsn() + " - " + itemName, Palette.TEXT));

		String statusText = submission.getState().getLabel();
		if (submission.getStatusDetail() != null && !submission.getStatusDetail().isEmpty())
		{
			statusText = statusText + " - " + submission.getStatusDetail();
		}
		row.add(Ui.wrapped(statusText, colourFor(submission.getState())));
		return row;
	}

	/** Shared with the Home tab so both describe the event the same way. */
	public static String countdownText(@Nullable EventWindow window, Instant now)
	{
		if (window == null)
		{
			return " ";
		}
		if (window.isBeforeStart(now))
		{
			return Strings.EVENT_STARTS_IN + Strings.duration(Duration.between(now, window.getStart()));
		}
		if (window.isAfterEnd(now))
		{
			return Strings.EVENT_FINISHED;
		}
		if (window.getEnd() != null)
		{
			return Strings.EVENT_ENDS_IN + Strings.duration(Duration.between(now, window.getEnd()));
		}
		return Strings.EVENT_RUNNING;
	}

	public static Color colourFor(BingoState.Connection connection)
	{
		switch (connection)
		{
			case ONLINE:
				return Palette.POSITIVE;
			case UNAUTHORIZED:
			case OFFLINE:
				return Palette.NEGATIVE;
			case NO_EVENT:
			case CONNECTING:
				return Palette.WARNING;
			default:
				return Palette.MUTED;
		}
	}

	private static Color colourFor(SubmissionState state)
	{
		switch (state)
		{
			case DONE:
			case DUPLICATE:
				return Palette.POSITIVE;
			case FAILED:
				return Palette.NEGATIVE;
			case REJECTED:
				return Palette.WARNING;
			default:
				return Palette.MUTED;
		}
	}
}
