package com.brazilchat.clan;

import com.brazilchat.BrazilChatConfig;
import com.brazilchat.FeatureModule;
import com.brazilchat.ui.Strings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.ClanMemberJoined;
import net.runelite.api.events.ClanMemberLeft;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/**
 * Reads the player's clan straight from the game client: their rank and join date, the clan size,
 * and who is online in the clan channel. Also notices when the player's own rank goes up and
 * congratulates them.
 *
 * <p>Everything here is local -- no network request is made. All game reads happen on the client
 * thread (event handlers), and the result is published as an immutable {@link ClanSnapshot}.
 */
@Singleton
public class ClanTracker implements FeatureModule
{
	/** Re-read about every 30 seconds; rank changes have no dedicated event. */
	private static final int POLL_TICKS = 50;
	private static final String RANK_KEY_PREFIX = "lastRank.";
	/** Which next rank the player was last told they qualify for, so it is said once. */
	private static final String READY_KEY = "readyNotified";

	private final Client client;
	private final EventBus eventBus;
	private final ConfigManager configManager;
	private final ChatMessageManager chatMessageManager;
	private final Notifier notifier;
	private final BrazilChatConfig config;
	private final AchievementReader achievementReader;

	private final List<Consumer<ClanSnapshot>> sinks = new java.util.concurrent.CopyOnWriteArrayList<>();
	private int ticksUntilPoll;
	private boolean dirty = true;
	private ClanSnapshot last = ClanSnapshot.loggedOut();

	@Inject
	ClanTracker(Client client, EventBus eventBus, ConfigManager configManager,
		ChatMessageManager chatMessageManager, Notifier notifier, BrazilChatConfig config,
		AchievementReader achievementReader)
	{
		this.achievementReader = achievementReader;
		this.client = client;
		this.eventBus = eventBus;
		this.configManager = configManager;
		this.chatMessageManager = chatMessageManager;
		this.notifier = notifier;
		this.config = config;
	}

	/** Listeners get every snapshot, on the client thread. */
	public void addSink(Consumer<ClanSnapshot> sink)
	{
		sinks.add(sink);
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);
		dirty = true;
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
		// Singleton across plugin restarts: listeners are re-added on the next start.
		sinks.clear();
		last = ClanSnapshot.loggedOut();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			dirty = true;
		}
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			publish(ClanSnapshot.loggedOut());
		}
	}

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		dirty = true;
	}

	@Subscribe
	public void onClanMemberJoined(ClanMemberJoined event)
	{
		dirty = true;
	}

	@Subscribe
	public void onClanMemberLeft(ClanMemberLeft event)
	{
		dirty = true;
	}

	/** A diary or CA tier just completed: re-check right away instead of on the next poll. */
	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (AchievementReader.isTracked(event.getVarbitId()))
		{
			dirty = true;
		}
	}

	/** A level-up can complete a total-level or max requirement. */
	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		dirty = true;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Membership events can arrive in bursts (logging in to a busy clan); coalescing them into
		// at most one read per tick keeps the panel from rebuilding hundreds of times.
		if (--ticksUntilPoll <= 0)
		{
			ticksUntilPoll = POLL_TICKS;
			dirty = true;
		}
		if (dirty)
		{
			dirty = false;
			ClanSnapshot snapshot = read();
			checkRankUp(snapshot);
			checkReadyForPromotion(snapshot);
			publish(snapshot);
		}
	}

	// ------------------------------------------------------------------------------------------

	private void publish(ClanSnapshot snapshot)
	{
		last = snapshot;
		for (Consumer<ClanSnapshot> sink : sinks)
		{
			sink.accept(snapshot);
		}
	}

	/**
	 * The full in-game member list (name, rank, title, join date), for the staff roster upload.
	 * Client thread only. Empty when the clan data is not loaded.
	 */
	public List<com.brazilchat.clan.api.ClanDtos.RosterMember> readRoster()
	{
		List<com.brazilchat.clan.api.ClanDtos.RosterMember> out = new ArrayList<>();
		ClanSettings settings = client.getClanSettings();
		if (settings == null)
		{
			return out;
		}
		for (ClanMember member : settings.getMembers())
		{
			if (member == null || member.getName() == null)
			{
				continue;
			}
			ClanRank rank = member.getRank();
			out.add(new com.brazilchat.clan.api.ClanDtos.RosterMember(
				Text.removeTags(member.getName()).replace('\u00A0', ' '),
				rank == null ? 0 : rank.getRank(),
				titleFor(settings, rank),
				member.getJoinDate() == null ? null : member.getJoinDate().toString()));
		}
		return out;
	}

	public ClanSnapshot getLast()
	{
		return last;
	}

	/** Client thread only. */
	private ClanSnapshot read()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return ClanSnapshot.loggedOut();
		}

		Player local = client.getLocalPlayer();
		String rsn = local == null ? null : local.getName();
		ClanSettings settings = client.getClanSettings();
		if (rsn == null || settings == null)
		{
			return new ClanSnapshot(true, client.getAccountHash(), rsn, null, null, 0, null, 0, new ArrayList<>(),
				achievementReader.read());
		}

		ClanMember me = settings.findMember(rsn);
		int rank = me == null || me.getRank() == null ? 0 : me.getRank().getRank();
		String title = me == null ? null : titleFor(settings, me.getRank());

		List<ClanSnapshot.OnlineMember> online = new ArrayList<>();
		ClanChannel channel = client.getClanChannel();
		if (channel != null)
		{
			for (ClanChannelMember member : channel.getMembers())
			{
				ClanRank memberRank = member.getRank();
				ClanTitle memberTitle = memberRank == null ? null : settings.titleForRank(memberRank);
				online.add(new ClanSnapshot.OnlineMember(
					Text.removeTags(member.getName()),
					memberTitle == null ? null : memberTitle.getName(),
					memberTitle == null ? -1 : memberTitle.getId(),
					memberRank == null ? 0 : memberRank.getRank(),
					member.getWorld()));
			}
		}
		// Staff first, then by rank, then alphabetically: the order clan leaders expect.
		online.sort(Comparator.comparingInt(ClanSnapshot.OnlineMember::getRank).reversed()
			.thenComparing(m -> m.getName().toLowerCase(Locale.ROOT)));

		return new ClanSnapshot(true, client.getAccountHash(), rsn, settings.getName(), title, rank,
			me == null ? null : me.getJoinDate(), settings.getMembers().size(), online,
			achievementReader.read());
	}

	@Nullable
	private static String titleFor(ClanSettings settings, @Nullable ClanRank rank)
	{
		if (rank == null)
		{
			return null;
		}
		ClanTitle title = settings.titleForRank(rank);
		return title == null ? null : title.getName();
	}

	/**
	 * Compares the player's rank with the one remembered for this account and clan. A higher rank
	 * gets a congratulation in the chatbox (only ever shown to this player, never typed into clan
	 * chat). The first sighting is stored silently so installing the plugin does not "promote"
	 * anyone.
	 */
	private void checkRankUp(ClanSnapshot snapshot)
	{
		if (!snapshot.isLoggedIn() || !snapshot.inClan() || snapshot.getRank() <= 0)
		{
			return;
		}

		// Stored as "<rank number>|<title>". Whether a change is a promotion is decided by the
		// clan's ladder (RankLadder), not by the number: the number is just the slot the title
		// was given in the clan settings.
		String key = RANK_KEY_PREFIX + snapshot.getClanName().toLowerCase(Locale.ROOT);
		String stored = configManager.getRSProfileConfiguration(BrazilChatConfig.GROUP, key);
		String current = snapshot.getRank() + "|" + (snapshot.getRankTitle() == null ? "" : snapshot.getRankTitle());

		if (!current.equals(stored))
		{
			configManager.setRSProfileConfiguration(BrazilChatConfig.GROUP, key, current);
		}

		if (stored == null || current.equals(stored) || !config.announceRankUp())
		{
			return;
		}

		int bar = stored.indexOf('|');
		int previousRank = parseInt(bar < 0 ? stored : stored.substring(0, bar));
		String previousTitle = bar < 0 ? null : stored.substring(bar + 1);
		if (!RankLadder.isPromotion(previousTitle, previousRank, snapshot.getRankTitle(), snapshot.getRank()))
		{
			return;
		}

		String title = RankLadder.label(snapshot.getRankTitle());
		String message = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append(Strings.rankUp(title))
			.build();
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(message)
			.build());
		notifier.notify(Strings.rankUp(title));
	}

	/**
	 * Tells the player, once, when they meet every requirement for the next rank on the clan
	 * ladder -- from their own client's data, so it shows within seconds of finishing the last
	 * diary task, CA tier or level. Only in the Brazil Chat clan, only to the player themselves.
	 * Promotions are still given by staff in game; this just tells them to ask.
	 */
	private void checkReadyForPromotion(ClanSnapshot snapshot)
	{
		if (!snapshot.isLoggedIn() || !snapshot.inClan() || !config.announceRankUp()
			|| !Strings.CLAN_NAME.equalsIgnoreCase(snapshot.getClanName().trim()))
		{
			return;
		}
		RankLadder.Rank next = RankLadder.next(snapshot.getRankTitle());
		RankLadder.Facts facts = RankLadder.facts(snapshot.getAchievements(), snapshot.getJoinDate(),
			java.time.LocalDate.now(Strings.BRASILIA));
		if (next == null || facts == null || !RankLadder.meetsAll(next, facts))
		{
			return;
		}
		if (next.getKey().equals(configManager.getRSProfileConfiguration(BrazilChatConfig.GROUP, READY_KEY)))
		{
			return;
		}
		configManager.setRSProfileConfiguration(BrazilChatConfig.GROUP, READY_KEY, next.getKey());

		String text = Strings.readyForRank(next.getLabel());
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append(text).build())
			.build());
		notifier.notify(text);
	}

	private static int parseInt(@Nullable String value)
	{
		if (value == null)
		{
			return Integer.MIN_VALUE;
		}
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			return Integer.MIN_VALUE;
		}
	}
}
