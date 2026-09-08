package com.bingobrazilchat;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(BingoBrazilChatConfig.GROUP)
public interface BingoBrazilChatConfig extends Config
{
	String GROUP = "bingobrazilchat";

	@ConfigItem(
		keyName = "sharedToken",
		name = "Event token",
		description = "The token your event organiser gave you. The plugin does nothing at all "
			+ "until this is filled in - with no token it never contacts the network.",
		position = 1,
		secret = true
	)
	default String sharedToken()
	{
		return "";
	}

	@ConfigItem(
		keyName = "apiBaseUrl",
		name = "Site address",
		description = "Where to send drops. Leave this alone unless you are testing against a "
			+ "local copy of the site.",
		position = 2
	)
	default String apiBaseUrl()
	{
		return "https://brazilchat.vercel.app";
	}

	@ConfigItem(
		keyName = "captureScreenshot",
		name = "Attach screenshots",
		description = "Take a screenshot when a listed drop is broadcast and send it as proof. "
			+ "Turning this off still records the drop, just without an image.",
		position = 3
	)
	default boolean captureScreenshot()
	{
		return true;
	}

	@ConfigItem(
		keyName = "screenshotScale",
		name = "Screenshot size",
		description = "Scale applied before upload, as a percentage of the client size. Lower "
			+ "means a smaller upload.",
		position = 4
	)
	@net.runelite.client.config.Range(min = 25, max = 100)
	default int screenshotScalePercent()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "enableObserverReports",
		name = "Report other players' drops",
		description = "Report listed drops broadcast by anyone on the event roster, not just your "
			+ "own. Leaving this on means a drop still gets recorded when the person who got it "
			+ "isn't running the plugin.",
		position = 5
	)
	default boolean enableObserverReports()
	{
		return true;
	}

	@ConfigItem(
		keyName = "announceInChat",
		name = "Confirm in chat",
		description = "Print a local game message when a drop has been recorded on the site.",
		position = 6
	)
	default boolean announceInChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "debugLogUnmatched",
		name = "Log unrecognised broadcasts",
		description = "Write clan broadcasts that look like drops but could not be parsed to the "
			+ "client log. Useful for reporting a missed item to the event organiser.",
		position = 7
	)
	default boolean debugLogUnmatched()
	{
		return false;
	}

	@ConfigSection(
		name = "<html>This plugin sends data to the event site<br>"
			+ "When a listed raid drop is broadcast in your clan,<br>"
			+ "it sends the player name, the item, and a screenshot<br>"
			+ "to your event organiser's BrazilChat site.<br>"
			+ "Nothing is sent until you enter an event token.</html>",
		description = "Bingo BrazilChat reports listed clan drop broadcasts to the BrazilChat "
			+ "event site so they score on the team leaderboard automatically. It sends the "
			+ "dropper's name, the item name, the broadcast text and a screenshot. It sends "
			+ "nothing at all while the event token is empty.",
		position = 98,
		closedByDefault = true
	)
	String privacySection = "privacySection";

	@ConfigSection(
		name = "<html>Open the side panel for the live scoreboard<br>"
			+ "Click the Bingo BrazilChat icon on the right<br>"
			+ "to see team standings and your pending drops</html>",
		description = "The side panel shows the live team leaderboard, a countdown to the event "
			+ "start or end, and anything this client has queued to send.",
		position = 99,
		closedByDefault = true
	)
	String panelSection = "panelSection";
}
