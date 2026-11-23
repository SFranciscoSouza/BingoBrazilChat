package com.brazilchat.ui;

import java.awt.Color;
import net.runelite.client.ui.ColorScheme;

/**
 * The one colour palette every tab draws from. Nothing in the UI should construct a {@link Color}
 * directly -- that is what keeps the tabs looking like one product.
 */
public final class Palette
{
	/** Page background behind cards. */
	public static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
	/** Card / row background. */
	public static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
	/** Card background on hover, for rows that are clickable. */
	public static final Color CARD_HOVER = ColorScheme.DARKER_GRAY_HOVER_COLOR;

	public static final Color TEXT = Color.WHITE;
	public static final Color MUTED = new Color(150, 150, 150);

	/** Brand: the green and gold of the flag. */
	public static final Color BRAND_GREEN = new Color(0, 168, 89);
	public static final Color BRAND_GOLD = new Color(255, 204, 41);

	public static final Color POSITIVE = new Color(76, 175, 80);
	public static final Color WARNING = new Color(255, 167, 38);
	public static final Color NEGATIVE = new Color(229, 115, 115);

	/** Staff ranks (Administrator and above). */
	public static final Color STAFF = BRAND_GOLD;

	private Palette()
	{
	}

	public static Color parseHex(String hex, Color fallback)
	{
		if (hex == null || hex.isEmpty())
		{
			return fallback;
		}
		try
		{
			return Color.decode(hex.startsWith("#") ? hex : "#" + hex);
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}
}
