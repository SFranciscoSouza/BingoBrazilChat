package com.brazilchat;

import com.brazilchat.bingo.BingoModule;
import com.brazilchat.chat.BroadcastFilter;
import com.brazilchat.clan.ClanService;
import com.brazilchat.clan.ClanTracker;
import com.brazilchat.clan.EventOverlay;
import com.brazilchat.clan.api.ClanApiClient;
import com.brazilchat.ui.ClanActions;
import com.brazilchat.ui.BrazilChatPanel;
import com.brazilchat.ui.Strings;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

/**
 * Brazil Chat: the clan helper for the Brazil Chat OSRS clan.
 *
 * <p>This class only wires {@link FeatureModule}s to the side panel. Each feature lives in its own
 * package:
 * <ul>
 *   <li>{@code clan} -- your rank, join date and who is online, read from the game; rank-up notice</li>
 *   <li>{@code chat} -- hide noisy clan broadcasts, highlight rare ones</li>
 *   <li>{@code clan} (ClanService) -- calendar, sign-ups, announcements, competitions, reminders
 *       and staff tools; networked, off until "Conectar ao Brazil Chat" is turned on</li>
 *   <li>{@code bingo} -- automatic drop reporting to the event site; inert until an event token
 *       is entered</li>
 * </ul>
 */
@Slf4j
@PluginDescriptor(
	name = "Brazil Chat",
	description = "Ajudante do clã Brazil Chat: perfil, membros, eventos e inscrições, filtro de anúncios e bingo",
	tags = {"clan", "clã", "brazil", "brasil", "bingo", "broadcast", "event", "evento", "rank"}
)
public class BrazilChatPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClanTracker clanTracker;

	@Inject
	private BroadcastFilter broadcastFilter;

	@Inject
	private BingoModule bingoModule;

	@Inject
	private ClanService clanService;

	@Inject
	private ClanApiClient clanApi;

	@Inject
	private EventOverlay eventOverlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private net.runelite.client.game.ChatIconManager chatIconManager;

	private BrazilChatPanel panel;
	private NavigationButton navButton;
	private final List<FeatureModule> started = new ArrayList<>();

	@Override
	protected void startUp() throws Exception
	{
		// Before any module reads config: carries the token and queued drops over from the
		// plugin's previous name.
		ConfigMigration.run(configManager);

		panel = new BrazilChatPanel(() -> bingoModule.clearFinished(), new ClanActions()
		{
			@Override
			public void refresh()
			{
				clanService.refreshNow();
			}

			@Override
			public void signUp(String eventId)
			{
				clanService.signUp(eventId);
			}

			@Override
			public void cancelSignUp(String eventId)
			{
				clanService.cancelSignUp(eventId);
			}

			@Override
			public void staffSignupAction(String signupId, String action, String reason)
			{
				clanService.staffSignupAction(signupId, action, reason);
			}

			@Override
			public void staffSetSignupsOpen(String eventId, boolean open)
			{
				clanService.staffSetSignupsOpen(eventId, open);
			}

			@Override
			public void createAnnouncement(String title, String body, boolean pinned)
			{
				clanService.createAnnouncement(title, body, pinned);
			}

			@Override
			public void updateAnnouncement(String announcementId, String title, String body, boolean pinned)
			{
				clanService.updateAnnouncement(announcementId, title, body, pinned);
			}

			@Override
			public void deleteAnnouncement(String announcementId)
			{
				clanService.deleteAnnouncement(announcementId);
			}
		}, clanApi::hasStaffKey, this::rankIcon);
		clanTracker.addSink(panel::setClan);
		clanService.setSink(panel::setClanData);
		bingoModule.setSink(panel::setBingo);
		overlayManager.add(eventOverlay);

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/com/brazilchat/icon.png");
		navButton = NavigationButton.builder()
			.tooltip(Strings.PLUGIN_NAME)
			.icon(icon)
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		for (FeatureModule module : Arrays.asList(clanTracker, broadcastFilter, clanService, bingoModule))
		{
			// One broken feature must not take the others down with it.
			try
			{
				module.startUp();
				started.add(module);
			}
			catch (Exception e)
			{
				log.error("Brazil Chat: could not start {}", module.getClass().getSimpleName(), e);
				module.shutDown();
			}
		}
	}

	@Override
	protected void shutDown()
	{
		for (int i = started.size() - 1; i >= 0; i--)
		{
			try
			{
				started.get(i).shutDown();
			}
			catch (RuntimeException e)
			{
				log.warn("Brazil Chat: error stopping {}", started.get(i).getClass().getSimpleName(), e);
			}
		}
		started.clear();
		overlayManager.remove(eventOverlay);

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
	}

	/**
	 * The game's own icon for a clan rank title (gold key for Owner, silver for Deputy Owner,
	 * bronze for Administrator, and so on), as RuneLite's chat-channel plugin draws them. Null
	 * until the icons have loaded after login.
	 */
	private BufferedImage rankIcon(net.runelite.api.clan.ClanTitle title)
	{
		try
		{
			return title == null ? null : chatIconManager.getRankImage(title);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	@Provides
	BrazilChatConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BrazilChatConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!BrazilChatConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		for (FeatureModule module : started)
		{
			module.onConfigChanged(event);
		}
	}
}
