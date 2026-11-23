package com.brazilchat;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;

/**
 * One-time carry-over from the "Bingo BrazilChat" plugin to "Brazil Chat".
 *
 * <p>The config group changed with the rename, so without this an existing player would lose their
 * event token and any drop still queued for upload. Values already present in the new group win,
 * and a marker key makes sure this only ever runs once.
 */
@Slf4j
final class ConfigMigration
{
	private static final String MARKER = "migratedFromBingo";
	private static final String[] KEYS = {
		"sharedToken", "captureScreenshot", "screenshotScale", "enableObserverReports",
		"announceInChat", "debugLogUnmatched", "queue",
	};
	private static final String LEGACY_PROOF_DIR = "bingo-brazilchat";
	static final String PROOF_DIR = "brazilchat";

	private ConfigMigration()
	{
	}

	static void run(ConfigManager configManager)
	{
		if (configManager.getConfiguration(BrazilChatConfig.GROUP, MARKER) != null)
		{
			return;
		}

		int copied = 0;
		for (String key : KEYS)
		{
			String legacy = configManager.getConfiguration(BrazilChatConfig.LEGACY_GROUP, key);
			String current = configManager.getConfiguration(BrazilChatConfig.GROUP, key);
			if (legacy != null && current == null)
			{
				configManager.setConfiguration(BrazilChatConfig.GROUP, key, legacy);
				copied++;
			}
		}

		File oldDir = new File(RuneLite.RUNELITE_DIR, LEGACY_PROOF_DIR);
		File newDir = new File(RuneLite.RUNELITE_DIR, PROOF_DIR);
		if (oldDir.isDirectory() && !newDir.exists() && !oldDir.renameTo(newDir))
		{
			log.warn("Brazil Chat: could not move {} to {}", oldDir, newDir);
		}

		configManager.setConfiguration(BrazilChatConfig.GROUP, MARKER, "1");
		if (copied > 0)
		{
			log.info("Brazil Chat: carried over {} settings from Bingo BrazilChat", copied);
		}
	}
}
