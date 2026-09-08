package com.bingobrazilchat.submit;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;

/**
 * Keeps the queue across restarts, split across two stores for a reason.
 *
 * <p>Metadata goes into RuneLite config as one JSON blob: small, and it survives a crash. It is
 * written at <em>plugin-group</em> scope rather than RS-profile scope on purpose -- an observer
 * entry belongs to the client, not to the account that happens to be logged in, so switching
 * accounts must not orphan a pending drop.
 *
 * <p>Screenshot bytes go to files under the RuneLite directory. They are far too large to sit in a
 * config value that gets rewritten on every state change.
 */
@Slf4j
public class QueuePersistence
{
	private static final String CONFIG_KEY = "queue";
	private static final String SUBDIR = "bingo-brazilchat/queue";
	private static final long ORPHAN_MAX_AGE_MS = TimeUnit.HOURS.toMillis(24);

	private final ConfigManager configManager;
	private final Gson gson;
	private final File proofDir;

	public QueuePersistence(ConfigManager configManager, Gson gson)
	{
		this(configManager, gson, new File(RuneLite.RUNELITE_DIR, SUBDIR));
	}

	/** Test seam: lets a unit test point the proof directory somewhere disposable. */
	QueuePersistence(ConfigManager configManager, Gson gson, File proofDir)
	{
		this.configManager = configManager;
		this.gson = gson;
		this.proofDir = proofDir;
	}

	public List<QueuedSubmission> load()
	{
		if (configManager == null)
		{
			return new ArrayList<>();
		}

		String raw = configManager.getConfiguration(
			com.bingobrazilchat.BingoBrazilChatConfig.GROUP, CONFIG_KEY);
		if (raw == null || raw.trim().isEmpty())
		{
			return new ArrayList<>();
		}

		try
		{
			Type type = new TypeToken<List<QueuedSubmission>>()
			{
			}.getType();
			List<QueuedSubmission> loaded = gson.fromJson(raw, type);
			return loaded == null ? new ArrayList<>() : loaded;
		}
		catch (JsonSyntaxException e)
		{
			// A corrupt blob must not stop the plugin loading; losing a queued drop is the lesser
			// harm, and the alternative is the plugin refusing to start at all.
			log.warn("Bingo BrazilChat: discarding unreadable queue", e);
			return new ArrayList<>();
		}
	}

	public void save(List<QueuedSubmission> queue)
	{
		if (configManager == null)
		{
			return;
		}
		configManager.setConfiguration(
			com.bingobrazilchat.BingoBrazilChatConfig.GROUP, CONFIG_KEY, gson.toJson(queue));
	}

	public void writeProof(String dedupeKey, byte[] png)
	{
		try
		{
			//noinspection ResultOfMethodCallIgnored
			proofDir.mkdirs();
			Files.write(proofFile(dedupeKey).toPath(), png);
		}
		catch (IOException e)
		{
			// A missing screenshot is a degraded submission, not a failed one -- the drop still
			// scores, it just has no image.
			log.warn("Bingo BrazilChat: could not store screenshot for {}", dedupeKey, e);
		}
	}

	@Nullable
	public byte[] readProof(String dedupeKey)
	{
		File file = proofFile(dedupeKey);
		if (!file.isFile())
		{
			return null;
		}
		try
		{
			return Files.readAllBytes(file.toPath());
		}
		catch (IOException e)
		{
			log.warn("Bingo BrazilChat: could not read screenshot for {}", dedupeKey, e);
			return null;
		}
	}

	public void deleteProof(String dedupeKey)
	{
		File file = proofFile(dedupeKey);
		if (file.isFile() && !file.delete())
		{
			log.debug("Bingo BrazilChat: could not delete screenshot for {}", dedupeKey);
		}
	}

	/**
	 * Removes screenshots with no queue entry left, and anything older than a day. Without this the
	 * directory grows without bound whenever a client is killed between writing an image and
	 * finishing the upload.
	 */
	public void sweep(List<QueuedSubmission> queue)
	{
		File[] files = proofDir.listFiles();
		if (files == null)
		{
			return;
		}

		Set<String> live = new HashSet<>();
		for (QueuedSubmission s : queue)
		{
			if (!s.getState().isTerminal())
			{
				live.add(s.getDedupeKey() + ".png");
			}
		}

		long cutoff = System.currentTimeMillis() - ORPHAN_MAX_AGE_MS;
		for (File file : files)
		{
			if (!live.contains(file.getName()) || file.lastModified() < cutoff)
			{
				//noinspection ResultOfMethodCallIgnored
				file.delete();
			}
		}
	}

	private File proofFile(String dedupeKey)
	{
		return new File(proofDir, dedupeKey + ".png");
	}
}
