package com.brazilchat.bingo;

import com.brazilchat.BrazilChatConfig;
import com.brazilchat.FeatureModule;
import com.brazilchat.bingo.api.ApiCallback;
import com.brazilchat.bingo.api.ApiDtos;
import com.brazilchat.bingo.api.ApiException;
import com.brazilchat.bingo.api.BingoApiClient;
import com.brazilchat.bingo.capture.ScreenshotService;
import com.brazilchat.bingo.catalog.AliasResource;
import com.brazilchat.bingo.catalog.CatalogItem;
import com.brazilchat.bingo.catalog.DropCatalog;
import com.brazilchat.bingo.chat.ChatBroadcastListener;
import com.brazilchat.bingo.event.EventState;
import com.brazilchat.bingo.event.ServerClock;
import com.brazilchat.bingo.submit.Backoff;
import com.brazilchat.bingo.submit.DropPipeline;
import com.brazilchat.bingo.submit.QueuePersistence;
import com.brazilchat.bingo.submit.QueuedSubmission;
import com.brazilchat.bingo.submit.SubmissionQueue;
import com.brazilchat.bingo.ui.BingoState;
import com.brazilchat.ui.Strings;
import com.google.gson.Gson;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;

/**
 * Reports listed raid drops from clan broadcasts to the BrazilChat event site.
 *
 * <p><b>Consent gate:</b> inert until the player enters an event token, so with the token empty
 * this module makes no network requests at all.
 *
 * <p><b>Threading:</b> chat handling is on the client thread; every piece of slow or stateful work
 * runs on {@link #executor}, a single thread owned by this module. RuneLite's injected
 * {@code ScheduledExecutorService} is deliberately not used -- it is one thread shared by every
 * plugin, and a PNG encode there would stall everyone else.
 */
@Slf4j
@Singleton
public class BingoModule implements FeatureModule
{
	private static final long CONFIG_RETRY_SECONDS = 60L;

	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;
	private final BrazilChatConfig config;
	private final EventBus eventBus;
	private final Gson gson;
	private final BingoApiClient api;
	private final ScreenshotService screenshots;

	private final ServerClock serverClock = new ServerClock();

	private ScheduledExecutorService executor;
	private SubmissionQueue queue;
	private ChatBroadcastListener chatListener;
	private AliasResource.Loaded bakedAliases;
	private Consumer<BingoState> sink = s -> { };

	/** Replaced wholesale on each refresh so readers always see one consistent view. */
	private volatile EventState eventState;

	private volatile BingoState.Connection connection = BingoState.Connection.NOT_CONFIGURED;
	private volatile String connectionDetail;
	private volatile List<ApiDtos.TeamRow> teams = Collections.emptyList();
	private volatile List<QueuedSubmission> queueSnapshot = Collections.emptyList();
	private volatile String leaderboardEtag;

	@Inject
	BingoModule(Client client, ClientThread clientThread, ConfigManager configManager,
		BrazilChatConfig config, EventBus eventBus, Gson gson, BingoApiClient api,
		ScreenshotService screenshots)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.config = config;
		this.eventBus = eventBus;
		this.gson = gson;
		this.api = api;
		this.screenshots = screenshots;
	}

	/** Where state snapshots go. Set before {@link #startUp()}. */
	public void setSink(Consumer<BingoState> sink)
	{
		this.sink = sink;
	}

	@Override
	public void startUp() throws IOException
	{
		bakedAliases = AliasResource.load(gson);

		executor = Executors.newSingleThreadScheduledExecutor(r ->
		{
			Thread t = new Thread(r, "brazilchat-bingo");
			t.setDaemon(true);
			return t;
		});

		QueuePersistence persistence = new QueuePersistence(configManager, gson);
		queue = new SubmissionQueue(executor, api, persistence, new Backoff(),
			snapshot ->
			{
				queueSnapshot = snapshot;
				pushState();
			},
			this::announceRecorded);
		DropPipeline pipeline = new DropPipeline(executor, screenshots, queue, persistence, config,
			serverClock);
		chatListener = new ChatBroadcastListener(client, config, pipeline, () -> eventState);

		eventBus.register(chatListener);
		queue.start();

		scheduleConfigRefresh(0L);
		executor.scheduleWithFixedDelay(this::refreshLeaderboard, 5L, 60L, TimeUnit.SECONDS);
		pushState();
	}

	@Override
	public void shutDown()
	{
		if (chatListener != null)
		{
			eventBus.unregister(chatListener);
			chatListener = null;
		}
		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}
		eventState = null;
		teams = Collections.emptyList();
	}

	@Override
	public void onConfigChanged(ConfigChanged event)
	{
		// A newly pasted token should take effect immediately rather than on the next poll --
		// otherwise the plugin looks broken for five minutes after setup.
		if ("sharedToken".equals(event.getKey()))
		{
			leaderboardEtag = null;
			scheduleConfigRefresh(0L);
		}
	}

	public void clearFinished()
	{
		if (queue != null)
		{
			queue.clearTerminal();
		}
	}

	// ------------------------------------------------------------------------------------------

	private void scheduleConfigRefresh(long delaySeconds)
	{
		ScheduledExecutorService ex = executor;
		if (ex == null || ex.isShutdown())
		{
			return;
		}
		ex.schedule(this::refreshConfig, Math.max(0L, delaySeconds), TimeUnit.SECONDS);
	}

	private void runOnExecutor(Runnable task)
	{
		ScheduledExecutorService ex = executor;
		if (ex != null && !ex.isShutdown())
		{
			ex.execute(task);
		}
	}

	private void refreshConfig()
	{
		if (!api.isConfigured())
		{
			// The consent gate: no token means no request, ever.
			setConnection(BingoState.Connection.NOT_CONFIGURED, null);
			scheduleConfigRefresh(CONFIG_RETRY_SECONDS);
			return;
		}

		if (eventState == null)
		{
			setConnection(BingoState.Connection.CONNECTING, null);
		}

		final Instant sentAt = Instant.now();
		api.fetchConfig(sentAt, new ApiCallback<ApiDtos.ConfigResponse>()
		{
			@Override
			public void onSuccess(ApiDtos.ConfigResponse result)
			{
				final Instant receivedAt = Instant.now();
				runOnExecutor(() -> applyConfig(result, sentAt, receivedAt));
			}

			@Override
			public void onFailure(ApiException error)
			{
				runOnExecutor(() -> onConfigFailed(error));
			}
		});
	}

	private void applyConfig(ApiDtos.ConfigResponse response, Instant sentAt, Instant receivedAt)
	{
		if (response == null || !response.ok || response.event == null)
		{
			eventState = null;
			setConnection(BingoState.Connection.NO_EVENT, Strings.NO_EVENT_RUNNING);
			scheduleConfigRefresh(CONFIG_RETRY_SECONDS);
			return;
		}

		if (response.serverTime != null)
		{
			try
			{
				serverClock.sync(Instant.parse(response.serverTime), sentAt, receivedAt);
			}
			catch (java.time.format.DateTimeParseException e)
			{
				log.debug("Brazil Chat: unreadable serverTime {}", response.serverTime);
			}
		}

		List<CatalogItem> items = new ArrayList<>();
		if (response.items != null)
		{
			for (ApiDtos.ItemDto dto : response.items)
			{
				items.add(new CatalogItem(dto.id, dto.name, dto.raid, dto.points));
			}
		}

		List<String> ignored = response.ignored != null
			? response.ignored
			: bakedAliases.getIgnored();

		DropCatalog catalog = DropCatalog.build(items, response.aliases,
			bakedAliases.getAliases(), ignored);

		eventState = EventState.from(response, catalog);

		if (eventState.getWindow().getEnd() != null)
		{
			queue.setEventEnd(eventState.getWindow().getEnd().toEpochMilli());
		}

		setConnection(BingoState.Connection.ONLINE,
			serverClock.isSkewed()
				? Strings.clockSkew(serverClock.getOffsetMillis() / 1000)
				: null);

		log.debug("Brazil Chat: catalog has {} names, roster has {} players",
			catalog.size(), eventState.getRoster().size());

		long poll = response.policy == null ? 300L : response.policy.configPollSeconds;
		scheduleConfigRefresh(Math.max(30L, poll));
	}

	private void onConfigFailed(ApiException error)
	{
		if (error.isAuthFailure())
		{
			setConnection(BingoState.Connection.UNAUTHORIZED, Strings.CHECK_TOKEN);
		}
		else if (error.getHttpStatus() == 503)
		{
			eventState = null;
			setConnection(BingoState.Connection.NO_EVENT, Strings.NO_EVENT_RUNNING);
		}
		else
		{
			setConnection(BingoState.Connection.OFFLINE, Strings.SITE_UNREACHABLE);
		}
		scheduleConfigRefresh(CONFIG_RETRY_SECONDS);
	}

	private void refreshLeaderboard()
	{
		EventState state = eventState;
		if (!api.isConfigured() || state == null)
		{
			return;
		}

		api.fetchLeaderboard(state.getWindow().getEventId(), leaderboardEtag,
			new BingoApiClient.LeaderboardCallback()
			{
				@Override
				public void onSuccess(ApiDtos.LeaderboardResponse result, @Nullable String etag)
				{
					runOnExecutor(() ->
					{
						leaderboardEtag = etag;
						teams = result.teams == null ? Collections.emptyList() : result.teams;
						pushState();
					});
				}

				@Override
				public void onUnchanged()
				{
					// Nothing moved since our last fetch; the panel is already correct.
				}

				@Override
				public void onFailure(ApiException error)
				{
					log.debug("Brazil Chat: leaderboard fetch failed", error);
				}
			});
	}

	private void setConnection(BingoState.Connection next, @Nullable String detail)
	{
		connection = next;
		connectionDetail = detail;
		pushState();
	}

	private void pushState()
	{
		EventState state = eventState;
		sink.accept(new BingoState(
			connection,
			connectionDetail,
			state == null ? null : state.getWindow(),
			teams,
			queueSnapshot,
			serverClock.isSkewed()));
	}

	/**
	 * Local-only confirmation that a drop reached the site, printed on the client thread.
	 *
	 * <p>Only fires for a submission this client actually recorded, never for one that came back as
	 * a duplicate -- otherwise every member of the clan would print the same line for one drop.
	 */
	private void announceRecorded(QueuedSubmission submission)
	{
		if (!config.announceInChat())
		{
			return;
		}

		String item = submission.getItemDisplayName() == null
			? submission.getItemNameRaw()
			: submission.getItemDisplayName();
		String message = Strings.recorded(item, submission.getRsn(), submission.getPoints());

		clientThread.invokeLater(() ->
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
			return true;
		});
	}
}
