package com.brazilchat.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

/**
 * The shared building blocks. Every tab is assembled from these and nothing else, so spacing,
 * fonts and colours stay identical across the panel.
 */
public final class Ui
{
	public static final int GAP = 6;
	public static final int SECTION_GAP = 12;
	private static final int PAD = 6;

	private Ui()
	{
	}

	/** A vertical stack: the layout every tab and card body uses. */
	public static JPanel column()
	{
		JPanel panel = new FullWidth();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(Palette.BACKGROUND);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return panel;
	}

	/** Wraps a tab body so it scrolls vertically and never horizontally. */
	public static JScrollPane scroll(JComponent content)
	{
		JPanel holder = new WidthTrackingPanel();
		holder.setBackground(Palette.BACKGROUND);
		holder.add(content, BorderLayout.NORTH);

		JScrollPane scroll = new JScrollPane(holder,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getViewport().setBackground(Palette.BACKGROUND);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		return scroll;
	}

	/**
	 * Always exactly as wide as the scroll viewport. Without this a single long line makes the whole
	 * tab wider than the panel and every right-aligned value is clipped off the edge.
	 */
	private static final class WidthTrackingPanel extends JPanel implements Scrollable
	{
		WidthTrackingPanel()
		{
			super(new BorderLayout());
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return visibleRect.height;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}

	public static JLabel sectionHeader(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Palette.TEXT);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		return label;
	}

	/** A dark rounded-feeling block that groups related rows. */
	public static JPanel card()
	{
		JPanel card = new FullWidth();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(Palette.CARD);
		card.setBorder(BorderFactory.createEmptyBorder(PAD, PAD + 2, PAD, PAD + 2));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		return card;
	}

	/** A card with a coloured left edge, for status and highlights. */
	public static JPanel accentCard(Color accent)
	{
		JPanel card = card();
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
			BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD + 2)));
		return card;
	}

	/** "Label ........ value" on one line. */
	public static JPanel statRow(String label, String value)
	{
		return statRow(label, value, Palette.TEXT);
	}

	public static JPanel statRow(String label, String value, Color valueColour)
	{
		JPanel row = new JPanel(new BorderLayout(GAP, 0));
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel left = small(label, Palette.MUTED);
		JLabel right = small(value, valueColour);
		row.add(left, BorderLayout.WEST);
		row.add(right, BorderLayout.EAST);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		return row;
	}

	/** Left text, right text, inside its own card. Used for list rows (teams, members, drops). */
	public static JPanel listRow(String left, Color leftColour, String right, Color rightColour)
	{
		JPanel row = new JPanel(new BorderLayout(GAP, 0));
		row.setBackground(Palette.CARD);
		row.setBorder(BorderFactory.createEmptyBorder(5, PAD, 5, PAD));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.add(small(left, leftColour), BorderLayout.CENTER);
		row.add(small(right, rightColour), BorderLayout.EAST);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		return row;
	}

	public static JLabel title(String text, Color colour)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(colour);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	public static JLabel small(String text, Color colour)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(colour);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	/**
	 * Small text that wraps inside the panel width. A read-only text area rather than an HTML label:
	 * HTML labels report a one-line preferred size and then get cut off instead of wrapping.
	 */
	public static JTextArea wrapped(String text, Color colour)
	{
		JTextArea area = new JTextArea(text);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setEditable(false);
		area.setFocusable(false);
		area.setOpaque(false);
		area.setBorder(BorderFactory.createEmptyBorder());
		area.setFont(FontManager.getRunescapeSmallFont());
		area.setForeground(colour);
		area.setAlignmentX(Component.LEFT_ALIGNMENT);
		return area;
	}

	/** Placeholder for an empty list, so no section is ever just blank. */
	public static JTextArea empty(String text)
	{
		JTextArea area = wrapped(text, Palette.MUTED);
		area.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		return area;
	}

	/** A small coloured tag, e.g. "Staff". */
	public static JLabel badge(String text, Color colour)
	{
		JLabel label = small(text, colour);
		label.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(colour),
			BorderFactory.createEmptyBorder(0, 3, 0, 3)));
		return label;
	}

	public static JButton button(String text, Runnable action)
	{
		JButton button = new JButton(text);
		button.setFocusPainted(false);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.addActionListener(e -> action.run());
		return button;
	}

	/** A text row that opens a URL in the browser. */
	public static JLabel link(String text, String url)
	{
		JLabel label = small(text, Palette.BRAND_GOLD);
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		label.setToolTipText(url);
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				LinkBrowser.browse(url);
			}
		});
		return label;
	}

	/** Makes a whole card clickable, with hover feedback. */
	public static void clickable(JPanel card, Runnable action)
	{
		card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		card.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				action.run();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				card.setBackground(Palette.CARD_HOVER);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				card.setBackground(Palette.CARD);
			}
		});
	}

	public static Component vgap(int height)
	{
		return Box.createRigidArea(new Dimension(0, height));
	}

	/**
	 * A panel that always fills the panel width but never grows taller than its content. Plain
	 * BoxLayout panels report a maximum width equal to their widest child, which leaves cards with
	 * short text narrower than their neighbours.
	 */
	private static final class FullWidth extends JPanel
	{
		@Override
		public Dimension getMaximumSize()
		{
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}
	}

}
