package com.bingobrazilchat.chat;

import com.bingobrazilchat.BingoBrazilChatConfig;
import com.bingobrazilchat.event.EventState;
import com.bingobrazilchat.submit.DropPipeline;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;

/**
 * Watches clan chat for drop broadcasts.
 *
 * <p>Registered manually on the event bus (see the plugin's {@code startUp}) because its
 * constructor takes collaborators that are not Guice bindings.
 *
 * <p>Runs entirely on the client thread and does only cheap, local work: a regex, a map lookup, a
 * set membership test. Anything slower is handed to the plugin executor by {@link DropPipeline}.
 */
@Slf4j
public class ChatBroadcastListener
{
	private final Client client;
	private final BingoBrazilChatConfig config;
	private final DropPipeline pipeline;
	private final Supplier<EventState> stateSupplier;

	public ChatBroadcastListener(Client client, BingoBrazilChatConfig config,
		DropPipeline pipeline, Supplier<EventState> stateSupplier)
	{
		this.client = client;
		this.config = config;
		this.pipeline = pipeline;
		this.stateSupplier = stateSupplier;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!isBroadcast(event.getType()))
		{
			return;
		}

		final String message = event.getMessage();
		Optional<ParsedBroadcast> parsed = BroadcastParser.parse(message);

		if (!parsed.isPresent())
		{
			if (config.debugLogUnmatched() && BroadcastParser.looksLikeUnparsedDrop(message))
			{
				// The one thing that turns a wording mismatch into a fixable report instead of a
				// drop that silently never scored.
				log.info("Bingo BrazilChat: unrecognised drop broadcast: {}", message);
			}
			return;
		}

		// getLocalPlayer() is client-thread-only, so it is read here and passed down rather than
		// looked up later on the executor.
		String localName = localPlayerName();

		DropPipeline.Outcome outcome = pipeline.handle(parsed.get(), stateSupplier.get(), localName);

		if (outcome == DropPipeline.Outcome.UNKNOWN_ITEM && config.debugLogUnmatched())
		{
			log.info("Bingo BrazilChat: broadcast item not in catalog: {}",
				parsed.get().getItemName());
		}
	}

	/**
	 * Only system broadcasts count.
	 *
	 * <p>{@code CLAN_CHAT} and {@code FRIENDSCHAT} are deliberately excluded: those carry text a
	 * player typed, so accepting them would let anyone score by typing a fake drop line.
	 */
	private static boolean isBroadcast(ChatMessageType type)
	{
		return type == ChatMessageType.CLAN_MESSAGE
			|| type == ChatMessageType.CLAN_GIM_MESSAGE
			|| type == ChatMessageType.CLAN_GUEST_MESSAGE;
	}

	@Nullable
	private String localPlayerName()
	{
		return client.getLocalPlayer() == null ? null : client.getLocalPlayer().getName();
	}
}
