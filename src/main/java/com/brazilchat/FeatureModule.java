package com.brazilchat;

import net.runelite.client.events.ConfigChanged;

/**
 * One self-contained feature of the plugin (bingo, clan tracking, broadcast filter, ...).
 *
 * <p>The plugin class only wires modules together: it starts them in order, stops them in reverse,
 * and forwards config changes for its own group. A module that needs game events registers its own
 * subscribers on the event bus in {@link #startUp()} and removes them in {@link #shutDown()}.
 */
public interface FeatureModule
{
	void startUp() throws Exception;

	void shutDown();

	default void onConfigChanged(ConfigChanged event)
	{
	}
}
