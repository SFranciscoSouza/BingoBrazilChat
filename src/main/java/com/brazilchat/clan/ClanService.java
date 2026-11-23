package com.brazilchat.clan;

import com.brazilchat.BrazilChatConfig;
import com.brazilchat.FeatureModule;
import com.brazilchat.bingo.api.ApiCallback;
import com.brazilchat.bingo.api.ApiException;
import com.brazilchat.clan.api.ClanApiClient;
import com.brazilchat.clan.api.ClanDtos;
import com.brazilchat.ui.Strings;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;

/**
 * The networked clan features: calendar and sign-ups, announcements, WOM competitions, event
 * reminders, and -- for staff with a key -- sign-up management and the automatic roster upload.
 *
 * <p><b>Consent gate:</b> does nothing unless "Conectar ao Brazil Chat" is on <em>and</em> the
 * player is logged in to the Brazil Chat clan. Staff calls additionally need the staff key.
 *
 * <p>All state changes happen on {@link #executor}; the panel gets immutable {@link ClanData}.
 */
@Slf4j
@Singleton
public class ClanService implements FeatureModule
{
	private static final long POLL_SECONDS = 300L;
	private static final long ROSTER_UPLOAD_SECONDS = 30L * 60L;
	private static final String SEEN_ANNOUNCEMENTS_KEY = "seenAnnouncements";
	private static final int MAX_SEEN_ANNOUNCEMENTS = 50;

	private final ClanApiClient api;
	private final ClanTracker tracker;
	private final ClientThread clientThread;
	private final ChatMessageManager chatMessageManager;
	private final Notifier notifier;
	private final BrazilChatConfig config;
	private final ConfigManager configManager;

	private ScheduledExecutorService executor;
	private Consumer<ClanData> sink = d -> { };

	// Executor-confined state.
	private ClanData data = ClanData.disabled();
	private volatile ClanSnapshot snapshot = ClanSnapshot.loggedOut();
	private long lastAccountHash = -1L;
	private Instant lastFetch = Instant.EPOCH;
	private Instant lastRosterUpload = Instant.EPOCH;
	private Instant lastAnnouncementsFetch = Instant.EPOCH;
	private boolean loginReminderDone;
	private ClanDtos.Achievements lastSentAchievements;
	private final Set<String> reminded = new HashSet<>();

	@Inject
	ClanService(ClanApiClient api, ClanTracker tracker, ClientThread clientThread,
		ChatMessageManager chatMessageManager, Notifier notifier, BrazilChatConfig config,
		ConfigManager configManager)
	{
		this.configManager = configManager;
		this.api = api;
		this.tracker = tracker;
		this.clientThread = clientThread;
		this.chatMessageManager = chatMessageManager;
		this.notifier = notifier;
		this.config = config;
	}

	public void setSink(Consumer<ClanData> sink)
	{
		this.sink = sink;
	}

	@Override
	public void startUp()
	{
		executor = Executors.newSingleThreadScheduledExecutor(r ->
		{
			Thread t = new Thread(r, "brazilchat-clan");
			t.setDaemon(true);
			return t;
		});
		tracker.addSink(s ->
		{
			snapshot = s;
			run(this::onSnapshot);
		});
		executor.scheduleWithFixedDelay(this::tick, 20L, 30L, TimeUnit.SECONDS);
		run(this::onSnapshot);
	}

	@Override
	public void shutDown()
	{
		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}
	}

	@Override
	public void onConfigChanged(ConfigChanged event)
	{
		if ("connectClan".equals(event.getKey()) || "staffKey".equals(event.getKey())
			|| "showAnnouncements".equals(event.getKey()))
		{
			run(() ->
			{
				lastFetch = Instant.EPOCH;
				lastAnnouncementsFetch = Instant.EPOCH;
				lastRosterUpload = Instant.EPOCH;
				onSnapshot();
			});
		}
	}

	/** Latest snapshot for overlays; safe from any thread. */
	public ClanData current()
	{
		return data;
	}

	// ------------------------------------------------------------------------- member actions

	public void refreshNow()
	{
		run(() ->
		{
			lastFetch = Instant.EPOCH;
			onSnapshot();
		});
	}

	public void signUp(String eventId)
	{
		withIdentity(id -> api.signUp(id, eventId, actionCallback(Strings.SIGNED_UP)));
	}

	public void cancelSignUp(String eventId)
	{
		withIdentity(id -> api.cancelSignUp(id, eventId, actionCallback(Strings.SIGNUP_CANCELLED)));
	}

	// -------------------------------------------------------------------------- staff actions

	public void staffSignupAction(String signupId, String action, @Nullable String reason)
	{
		withIdentity(id -> api.staffSignupAction(id, signupId, action, reason, actionCallback(null)));
	}

	public void staffSetSignupsOpen(String eventId, boolean open)
	{
		withIdentity(id -> api.staffSetSignupsOpen(id, eventId, open, actionCallback(null)));
	}

	public void createAnnouncement(String title, String body, boolean pinned)
	{
		ClanDtos.AnnouncementInput input = new ClanDtos.AnnouncementInput(title, body, pinned);
		withIdentity(id -> api.staffCreateAnnouncement(id, input, actionCallback(Strings.ANNOUNCEMENT_SAVED)));
	}

	public void updateAnnouncement(String announcementId, String title, String body, boolean pinned)
	{
		ClanDtos.AnnouncementInput input = new ClanDtos.AnnouncementInput(title, body, pinned);
		withIdentity(id -> api.staffUpdateAnnouncement(id, announcementId, input,
			actionCallback(Strings.ANNOUNCEMENT_SAVED)));
	}

	public void deleteAnnouncement(String announcementId)
	{
		withIdentity(id -> api.staffDeleteAnnouncement(id, announcementId,
			actionCallback(Strings.ANNOUNCEMENT_DELETED)));
	}

	// ---------------------------------------------------------------------------------- polling

	/**
	 * Announcements for any confirmed clan member. With the clan connection on they arrive with the
	 * bootstrap; with it off they come from a separate call that sends no name or account.
	 */
	private void refreshAnnouncements(ClanSnapshot s)
	{
		if (!config.showAnnouncements() || !s.isConfirmedMember())
		{
			lastAnnouncementsFetch = Instant.EPOCH;
			if (!data.getAnnouncements().isEmpty())
			{
				publish(data.withAnnouncements(java.util.Collections.emptyList()));
			}
			return;
		}
		if (config.connectClan()
			|| Duration.between(lastAnnouncementsFetch, Instant.now()).getSeconds() < POLL_SECONDS)
		{
			return;
		}
		lastAnnouncementsFetch = Instant.now();
		api.announcements(new ApiCallback<ClanDtos.Announcements>()
		{
			@Override
			public void onSuccess(ClanDtos.Announcements result)
			{
				run(() ->
				{
					publish(data.withAnnouncements(result.announcements == null
						? java.util.Collections.emptyList() : result.announcements));
					announceNew(data.getAnnouncements());
				});
			}

			@Override
			public void onFailure(ApiException error)
			{
				log.debug("Brazil Chat: announcements fetch failed: {}", error.getMessage());
			}
		});
	}

	private boolean enabled(ClanSnapshot s)
	{
		return config.connectClan() && s.inBrazilChat();
	}

	private boolean staffEnabled(ClanSnapshot s)
	{
		return enabled(s) && s.isStaff() && api.hasStaffKey();
	}

	private void onSnapshot()
	{
		ClanSnapshot s = snapshot;
		refreshAnnouncements(s);
		if (!config.connectClan())
		{
			publish(ClanData.disabled().withAnnouncements(data.getAnnouncements()));
			return;
		}
		if (!s.inBrazilChat())
		{
			publish(new ClanData(ClanData.Status.NOT_IN_CLAN, null, null, null, null, null, false,
				data.getAnnouncements()));
			return;
		}
		boolean wasOff = data.getStatus() == ClanData.Status.DISABLED
			|| data.getStatus() == ClanData.Status.NOT_IN_CLAN;
		if (s.getAccountHash() != lastAccountHash || wasOff)
		{
			// New login (or account switch): fresh data, fresh reminders.
			lastAccountHash = s.getAccountHash();
			lastFetch = Instant.EPOCH;
			lastRosterUpload = Instant.EPOCH;
			loginReminderDone = false;
			lastSentAchievements = null;
			reminded.clear();
			publish(new ClanData(ClanData.Status.CONNECTING, null, null, null, null, null, false,
				data.getAnnouncements()));
		}
		if (Duration.between(lastFetch, Instant.now()).getSeconds() >= POLL_SECONDS)
		{
			fetch();
		}
		else if (data.getStatus() == ClanData.Status.ONLINE)
		{
			reportAchievementsIfChanged();
		}
		if (staffEnabled(s) && Duration.between(lastRosterUpload, Instant.now()).getSeconds() >= ROSTER_UPLOAD_SECONDS)
		{
			uploadRoster();
		}
	}

	private void tick()
	{
		onSnapshot();
		checkReminders();
	}

	private void fetch()
	{
		ClanSnapshot s = snapshot;
		if (!enabled(s))
		{
			return;
		}
		lastFetch = Instant.now();
		ClanApiClient.Identity id = identity(s);
		api.bootstrap(id, new ApiCallback<ClanDtos.Bootstrap>()
		{
			@Override
			public void onSuccess(ClanDtos.Bootstrap result)
			{
				run(() ->
				{
					publish(data.withStatus(ClanData.Status.ONLINE).withMessage(null).withBootstrap(result)
						.withAnnouncements(config.showAnnouncements() && result.announcements != null
							? result.announcements : java.util.Collections.emptyList()));
					announceNew(data.getAnnouncements());
					checkReminders();
					reportAchievementsIfChanged();
				});
			}

			@Override
			public void onFailure(ApiException error)
			{
				run(() -> publish(data.withStatus(ClanData.Status.ERROR)
					.withMessage(Strings.clanError(error.getMessage()))));
			}
		});

		if (staffEnabled(s))
		{
			fetchStaff(id);
		}
		else if (data.getStaffEvents() != null || data.getOverview() != null)
		{
			publish(data.withStaffEvents(null).withOverview(null).withStaffMessage(null));
		}
	}

	private void fetchStaff(ClanApiClient.Identity id)
	{
		api.staffEvents(id, new ApiCallback<ClanDtos.StaffEvents>()
		{
			@Override
			public void onSuccess(ClanDtos.StaffEvents result)
			{
				run(() -> publish(data.withStaffEvents(result).withStaffMessage(null)));
			}

			@Override
			public void onFailure(ApiException error)
			{
				run(() -> publish(data.withStaffMessage(Strings.clanError(error.getMessage()))));
			}
		});
		api.staffOverview(id, new ApiCallback<ClanDtos.Overview>()
		{
			@Override
			public void onSuccess(ClanDtos.Overview result)
			{
				run(() -> publish(data.withOverview(result)));
			}

			@Override
			public void onFailure(ApiException error)
			{
				log.debug("Brazil Chat: staff overview failed: {}", error.getMessage());
			}
		});
	}

	/** Reads the member list on the client thread, then uploads it signed with the staff key. */
	private void uploadRoster()
	{
		lastRosterUpload = Instant.now();
		clientThread.invokeLater(() ->
		{
			List<ClanDtos.RosterMember> members = tracker.readRoster();
			ClanSnapshot s = snapshot;
			if (members.isEmpty() || !staffEnabled(s))
			{
				return;
			}
			ClanDtos.RosterUpload upload = new ClanDtos.RosterUpload(s.getClanName(), members);
			api.staffRoster(identity(s), upload, new ApiCallback<ClanDtos.OkResponse>()
			{
				@Override
				public void onSuccess(ClanDtos.OkResponse result)
				{
					log.debug("Brazil Chat: roster uploaded ({} members)", members.size());
				}

				@Override
				public void onFailure(ApiException error)
				{
					run(() -> publish(data.withStaffMessage(Strings.clanError(error.getMessage()))));
				}
			});
		});
	}

	/**
	 * Sends the logged-in player's own total level, max status, diary tier and Combat Achievement
	 * tier -- read from their own client by {@link AchievementReader} on every clan snapshot --
	 * whenever it differs from what was last sent. So finishing a diary or a CA tier reaches the
	 * worklist within a minute, and an empty read right after login is corrected on the next one.
	 */
	private void reportAchievementsIfChanged()
	{
		ClanSnapshot s = snapshot;
		ClanDtos.Achievements a = s.getAchievements();
		if (!enabled(s) || a == null || a.equals(lastSentAchievements))
		{
			return;
		}
		lastSentAchievements = a;
		api.reportAchievements(identity(s), a, new ApiCallback<ClanDtos.OkResponse>()
		{
			@Override
			public void onSuccess(ClanDtos.OkResponse result)
			{
			}

			@Override
			public void onFailure(ApiException error)
			{
				// Try again on the next snapshot.
				run(() -> lastSentAchievements = null);
				log.debug("Brazil Chat: achievements report failed: {}", error.getMessage());
			}
		});
	}

	/**
	 * Prints each announcement this account has not seen yet, once, in the chatbox -- so a new
	 * announcement greets everyone at their next login (or within one poll if they are online).
	 * Seen ids are kept per RuneScape account, so an announcement is never shown twice, not even
	 * after a staff member edits it.
	 */
	private void announceNew(List<ClanDtos.Announcement> announcements)
	{
		ClanSnapshot s = snapshot;
		if (!config.announceNewAnnouncements() || !config.showAnnouncements() || !s.isConfirmedMember()
			|| announcements == null || announcements.isEmpty())
		{
			return;
		}

		String stored = configManager.getRSProfileConfiguration(BrazilChatConfig.GROUP, SEEN_ANNOUNCEMENTS_KEY);
		java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
		if (stored != null && !stored.isEmpty())
		{
			seen.addAll(java.util.Arrays.asList(stored.split(",")));
		}

		List<ClanDtos.Announcement> fresh = new java.util.ArrayList<>();
		for (ClanDtos.Announcement a : announcements)
		{
			if (a.id != null && !seen.contains(a.id))
			{
				fresh.add(a);
			}
		}
		if (fresh.isEmpty())
		{
			return;
		}

		// Oldest first, so they read in the order they were posted.
		for (int i = fresh.size() - 1; i >= 0; i--)
		{
			ClanDtos.Announcement a = fresh.get(i);
			say(Strings.newAnnouncement(a.title, a.body));
			seen.add(a.id);
		}
		notifier.notify(Strings.newAnnouncement(fresh.get(0).title, null));

		// Keep the list bounded; old ids are long gone from the site anyway.
		List<String> ids = new java.util.ArrayList<>(seen);
		if (ids.size() > MAX_SEEN_ANNOUNCEMENTS)
		{
			ids = ids.subList(ids.size() - MAX_SEEN_ANNOUNCEMENTS, ids.size());
		}
		configManager.setRSProfileConfiguration(BrazilChatConfig.GROUP, SEEN_ANNOUNCEMENTS_KEY, String.join(",", ids));
	}

	// -------------------------------------------------------------------------------- reminders

	private void checkReminders()
	{
		ClanDtos.Bootstrap b = data.getBootstrap();
		if (!config.eventReminders() || b == null || b.events == null || !enabled(snapshot))
		{
			return;
		}
		Instant now = Instant.now();
		long leadSeconds = config.reminderMinutes() * 60L;

		for (ClanDtos.EventDto e : b.events)
		{
			if (e.mySignup == null || !e.mySignup.isActive())
			{
				continue;
			}
			Instant start = parse(e.startDate);
			if (start == null || start.isBefore(now))
			{
				continue;
			}
			long until = Duration.between(now, start).getSeconds();
			if (until <= leadSeconds && reminded.add(e.id))
			{
				say(Strings.reminderSoon(e.name, Strings.duration(Duration.ofSeconds(until))));
				notifier.notify(Strings.reminderSoon(e.name, Strings.duration(Duration.ofSeconds(until))));
			}
			else if (!loginReminderDone && until <= 24L * 3600L)
			{
				say(Strings.reminderToday(e.name, Strings.duration(Duration.ofSeconds(until))));
			}
		}
		loginReminderDone = true;
	}

	private void say(String text)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append(text).build())
			.build());
	}

	// ---------------------------------------------------------------------------------- helpers

	private ApiCallback<ClanDtos.OkResponse> actionCallback(@Nullable String successMessage)
	{
		run(() -> publish(data.withBusy(true)));
		return new ApiCallback<ClanDtos.OkResponse>()
		{
			@Override
			public void onSuccess(ClanDtos.OkResponse result)
			{
				run(() ->
				{
					publish(data.withBusy(false).withMessage(successMessage));
					lastFetch = Instant.EPOCH;
					fetch();
				});
			}

			@Override
			public void onFailure(ApiException error)
			{
				run(() -> publish(data.withBusy(false).withMessage(Strings.clanError(error.getMessage()))));
			}
		};
	}

	private void withIdentity(Consumer<ClanApiClient.Identity> action)
	{
		run(() ->
		{
			ClanSnapshot s = snapshot;
			if (enabled(s))
			{
				action.accept(identity(s));
			}
		});
	}

	private static ClanApiClient.Identity identity(ClanSnapshot s)
	{
		return new ClanApiClient.Identity(s.getAccountHash(), s.getRsn());
	}

	private void publish(ClanData next)
	{
		data = next;
		sink.accept(next);
	}

	private void run(Runnable task)
	{
		ScheduledExecutorService ex = executor;
		if (ex != null && !ex.isShutdown())
		{
			ex.execute(task);
		}
	}

	@Nullable
	static Instant parse(@Nullable String iso)
	{
		if (iso == null)
		{
			return null;
		}
		try
		{
			return Instant.parse(iso);
		}
		catch (DateTimeParseException e)
		{
			return null;
		}
	}
}
