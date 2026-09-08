package com.bingobrazilchat;

import com.bingobrazilchat.api.ApiCallback;
import com.bingobrazilchat.api.ApiDtos;
import com.bingobrazilchat.api.ApiException;
import com.bingobrazilchat.api.BingoApiClient;
import com.bingobrazilchat.capture.ScreenshotService;
import com.bingobrazilchat.catalog.AliasResource;
import com.bingobrazilchat.catalog.CatalogItem;
import com.bingobrazilchat.catalog.DropCatalog;
import com.bingobrazilchat.chat.ChatBroadcastListener;
import com.bingobrazilchat.event.EventState;
import com.bingobrazilchat.event.ServerClock;
import com.bingobrazilchat.submit.Backoff;
import com.bingobrazilchat.submit.DropPipeline;
import com.bingobrazilchat.submit.QueuePersistence;
import com.bingobrazilchat.submit.QueuedSubmission;
import com.bingobrazilchat.submit.SubmissionQueue;
import com.bingobrazilchat.ui.BingoPanel;
import com.bingobrazilchat.ui.PanelState;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

/**
 * Reports listed raid drops from clan broadcasts to a BrazilChat event.
 *
 * <p><b>Consent gate:</b> this plugin sends data off the machine, which the other plugins in this
 * account do not. It is inert until the player enters an event token, so a fresh install makes no
 * network requests at all.
 *
 * <p><b>Threading:</b> chat handling is on the client thread; every piece of slow or stateful work
 * runs on {@link #executor}, a single thread owned by this plugin. RuneLite's own injected
 * {@code ScheduledExecutorService} is deliberately not used -- it is one thread shared by every
 * plugin in the client, and a PNG encode there would stall everyone else.
 */
@Slf4j
@PluginDescriptor(
	name = "Bingo BrazilChat",
	description = "Reports listed raid drops from clan broadcasts to a BrazilChat event",
	tags = {"bingo", "clan", "broadcast", "drop", "raids", "event", "leaderboard"}
)
public class BingoBrazilChatPlugin extends Plugin
{
	private static final long CONFIG_RETRY_SECONDS = 60L;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private BingoBrazilChatConfig config;

	@Inject
	private EventBus eventBus;

	@Inject
	private Gson gson;

	@Inject
	private BingoApiClient api;

	@Inject
	private ScreenshotService screenshots;

	private ScheduledExecutorService executor;
	private SubmissionQueue queue;
	private QueuePersistence persistence;
	private DropPipeline pipeline;
	private ChatBroadcastListener chatListener;
	private BingoPanel panel;
	private NavigationButton navButton;

	private final ServerClock serverClock = new ServerClock();

	private AliasResource.Loaded bakedAliases;

	/** Replaced wholesale on each refresh so readers always see one consistent view. */
	private volatile EventState eventState;

	private volatile PanelState.Connection connection = PanelState.Connection.NOT_CONFIGURED;
	private volatile String connectionDetail;
	private volatile List<ApiDtos.TeamRow> teams = Collections.emptyList();
	private volatile List<QueuedSubmission> queueSnapshot = Collections.emptyList();
	private volatile String leaderboardEtag;

	@Override
	protected void startUp() throws Exception
	{
		bakedAliases = AliasResource.load(gson);

		executor = Executors.newSingleThreadScheduledExecutor(r ->
		{
			Thread t = new Thread(r, "bingo-brazilchat");
			t.setDaemon(true);
			return t;
		});

		persistence = new QueuePersistence(configManager, gson);
		queue = new SubmissionQueue(executor, api, persistence, new Backoff(),
			snapshot ->
			{
				queueSnapshot = snapshot;
				pushPanelState();
			},
			this::announceRecorded);
		pipeline = new DropPipeline(executor, screenshots, queue, persistence, config, serverClock);
		chatListener = new ChatBroadcastListener(client, config, pipeline, () -> eventState);

		panel = new BingoPanel(() -> queue.clearTerminal());

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/com/bingobrazilchat/icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Bingo BrazilChat")
			.icon(icon)
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		eventBus.register(chatListener);
		queue.start();

		scheduleConfigRefresh(0L);
		scheduleLeaderboardRefresh();
		pushPanelState();
	}

	@Override
	protected void shutDown()
	{
		if (chatListener != null)
		{
			eventBus.unregister(chatListener);
			chatListener = null;
		}
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
		if (panel != null)
		{
			panel.shutdown();
			panel = null;
		}
		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}
		eventState = null;
		teams = Collections.emptyList();
	}

	@Provides
	BingoBrazilChatConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BingoBrazilChatConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!BingoBrazilChatConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		// A newly pasted token should take effect immediately rather than on the next poll --
		// otherwise the plugin looks broken for five minutes after setup.
		if ("sharedToken".equals(event.getKey()))
		{
			leaderboardEtag = null;
			scheduleConfigRefresh(0L);
		}
	}

	// ------------------------------------------------------------------------------------------

	private void scheduleConfigRefresh(long delaySeconds)
	{
		if (executor == null)
		{
			return;
		}
		executor.schedule(this::refreshConfig, Math.max(0L, delaySeconds), TimeUnit.SECONDS);
	}

	private void refreshConfig()
	{
		if (!api.isConfigured())
		{
			// The consent gate: no token means no request, ever.
			setConnection(PanelState.Connection.NOT_CONFIGURED, null);
			scheduleConfigRefresh(CONFIG_RETRY_SECONDS);
			return;
		}

		if (eventState == null)
		{
			setConnection(PanelState.Connection.CONNECTING, null);
		}

		final Instant sentAt = Instant.now();
		api.fetchConfig(sentAt, new ApiCallback<ApiDtos.ConfigResponse>()
		{
			@Override
			public void onSuccess(ApiDtos.ConfigResponse result)
			{
				final Instant receivedAt = Instant.now();
				executor.execute(() -> applyConfig(result, sentAt, receivedAt));
			}

			@Override
			public void onFailure(ApiException error)
			{
				executor.execute(() -> onConfigFailed(error));
			}
		});
	}

	private void applyConfig(ApiDtos.ConfigResponse response, Instant sentAt, Instant receivedAt)
	{
		if (response == null || !response.ok || response.event == null)
		{
			setConnection(PanelState.Connection.NO_EVENT, "No event is running right now");
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
				log.debug("Bingo BrazilChat: unreadable serverTime {}", response.serverTime);
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

		setConnection(PanelState.Connection.ONLINE,
			serverClock.isSkewed()
				? "Your PC clock is out by " + (serverClock.getOffsetMillis() / 1000) + "s"
				: null);

		log.debug("Bingo BrazilChat: catalog has {} names, roster has {} players",
			catalog.size(), eventState.getRoster().size());

		long poll = response.policy == null ? 300L : response.policy.configPollSeconds;
		scheduleConfigRefresh(Math.max(30L, poll));
	}

	private void onConfigFailed(ApiException error)
	{
		if (error.isAuthFailure())
		{
			setConnection(PanelState.Connection.UNAUTHORIZED,
				"Check your event token in the plugin settings");
		}
		else if (error.getHttpStatus() == 503)
		{
			setConnection(PanelState.Connection.NO_EVENT, "No event is running right now");
		}
		else
		{
			setConnection(PanelState.Connection.OFFLINE, "Cannot reach the site");
		}
		scheduleConfigRefresh(CONFIG_RETRY_SECONDS);
	}

	private void scheduleLeaderboardRefresh()
	{
		if (executor == null)
		{
			return;
		}
		executor.scheduleWithFixedDelay(this::refreshLeaderboard, 5L, 60L, TimeUnit.SECONDS);
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
					executor.execute(() ->
					{
						leaderboardEtag = etag;
						teams = result.teams == null ? Collections.emptyList() : result.teams;
						pushPanelState();
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
					log.debug("Bingo BrazilChat: leaderboard fetch failed", error);
				}
			});
	}

	private void setConnection(PanelState.Connection next, @Nullable String detail)
	{
		connection = next;
		connectionDetail = detail;
		pushPanelState();
	}

	private void pushPanelState()
	{
		BingoPanel current = panel;
		if (current == null)
		{
			return;
		}
		EventState state = eventState;
		current.setState(new PanelState(
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
		String message = "[Bingo BrazilChat] Recorded " + item
			+ " for " + submission.getRsn()
			+ (submission.getPoints() > 0 ? " (" + submission.getPoints() + " pts)" : "");

		clientThread.invokeLater(() ->
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
			return true;
		});
	}
}
