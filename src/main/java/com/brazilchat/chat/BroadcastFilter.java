package com.brazilchat.chat;

import com.brazilchat.BrazilChatConfig;
import com.brazilchat.FeatureModule;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

/**
 * Hides noisy clan broadcasts and highlights rare ones. Purely cosmetic and local: hidden lines
 * are still delivered to every other listener (the bingo reporter included), they are just not
 * drawn in the chatbox.
 *
 * <p>Only system broadcasts are touched -- never text a player typed.
 */
@Singleton
public class BroadcastFilter implements FeatureModule
{
	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final Notifier notifier;
	private final BrazilChatConfig config;

	@Inject
	BroadcastFilter(Client client, ClientThread clientThread, EventBus eventBus, Notifier notifier,
		BrazilChatConfig config)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.notifier = notifier;
		this.config = config;
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);
		refreshChat();
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
		refreshChat();
	}

	@Override
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getKey().startsWith("hide") || "minDropValue".equals(event.getKey()))
		{
			refreshChat();
		}
	}

	/** Runs for each line the chatbox is about to draw; setting the flag to 0 hides it. */
	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!"chatFilterCheck".equals(event.getEventName()))
		{
			return;
		}

		int[] intStack = client.getIntStack();
		int intStackSize = client.getIntStackSize();
		Object[] objectStack = client.getObjectStack();
		int objectStackSize = client.getObjectStackSize();

		ChatMessageType type = ChatMessageType.of(intStack[intStackSize - 2]);
		if (!isBroadcast(type))
		{
			return;
		}

		String message = (String) objectStack[objectStackSize - 1];
		if (shouldHide(message))
		{
			intStack[intStackSize - 3] = 0;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!isBroadcast(event.getType()))
		{
			return;
		}

		BroadcastCategory category = BroadcastCategory.classify(event.getMessage());
		if (!category.isRare())
		{
			return;
		}

		if (config.highlightRare())
		{
			MessageNode node = event.getMessageNode();
			node.setValue(ColorUtil.wrapWithColorTag(Text.removeTags(node.getValue()),
				config.highlightColour()));
		}
		if (config.notifyRare())
		{
			notifier.notify(Text.removeTags(event.getMessage()));
		}
	}

	boolean shouldHide(String message)
	{
		BroadcastCategory category = BroadcastCategory.classify(message);
		switch (category)
		{
			case DROP:
				return isBelowDropThreshold(message);
			case LEVEL:
				return config.hideLevels();
			case QUEST:
				return config.hideQuests();
			case DIARY:
				return config.hideDiaries();
			case COMBAT_ACHIEVEMENT:
				return config.hideCombatAchievements();
			case PERSONAL_BEST:
				return config.hidePersonalBests();
			case PVP:
				return config.hidePvp();
			case MEMBERSHIP:
				return config.hideMembership();
			default:
				// Rare broadcasts and anything we do not recognise are always shown.
				return false;
		}
	}

	private boolean isBelowDropThreshold(String message)
	{
		int threshold = config.minDropValue();
		if (threshold <= 0)
		{
			return false;
		}
		Optional<ParsedBroadcast> parsed = BroadcastParser.parse(message);
		// A drop line without a value is shown: we cannot prove it is below the threshold.
		return parsed.isPresent() && parsed.get().getValueGp() >= 0
			&& parsed.get().getValueGp() < threshold;
	}

	private static boolean isBroadcast(ChatMessageType type)
	{
		return type == ChatMessageType.CLAN_MESSAGE
			|| type == ChatMessageType.CLAN_GIM_MESSAGE
			|| type == ChatMessageType.CLAN_GUEST_MESSAGE;
	}

	private void refreshChat()
	{
		clientThread.invokeLater(client::refreshChat);
	}
}
