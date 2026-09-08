package com.bingobrazilchat.submit;

import com.bingobrazilchat.BingoBrazilChatConfig;
import com.bingobrazilchat.capture.ScreenshotService;
import com.bingobrazilchat.catalog.CatalogItem;
import com.bingobrazilchat.chat.ParsedBroadcast;
import com.bingobrazilchat.event.EventState;
import com.bingobrazilchat.event.ServerClock;
import com.bingobrazilchat.util.RsnUtil;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/**
 * Decides what happens to a parsed broadcast: ignore it, or turn it into a queued submission.
 *
 * <p>The ordering of the checks is deliberate -- everything cheap and local runs before anything
 * that could reach the network, so an ordinary clan broadcast for an item nobody is tracking costs
 * a couple of map lookups and nothing else.
 */
@Slf4j
public class DropPipeline
{
	private final ScheduledExecutorService executor;
	private final ScreenshotService screenshots;
	private final SubmissionQueue queue;
	private final QueuePersistence persistence;
	private final BingoBrazilChatConfig config;
	private final ServerClock clock;
	private final Random random = new Random();

	public DropPipeline(
		ScheduledExecutorService executor,
		ScreenshotService screenshots,
		SubmissionQueue queue,
		QueuePersistence persistence,
		BingoBrazilChatConfig config,
		ServerClock clock)
	{
		this.executor = executor;
		this.screenshots = screenshots;
		this.queue = queue;
		this.persistence = persistence;
		this.config = config;
		this.clock = clock;
	}

	/**
	 * Called from the chat handler, on the client thread. Returns quickly; anything slow is handed
	 * to the executor.
	 *
	 * @param localPlayerName the account this client is logged in as, read on the client thread by
	 *                        the caller because {@code getLocalPlayer()} is client-thread-only
	 * @return an outcome, purely so the caller can decide what to log
	 */
	public Outcome handle(ParsedBroadcast broadcast, @Nullable EventState state,
		@Nullable String localPlayerName)
	{
		if (state == null)
		{
			return Outcome.NOT_READY;
		}

		Optional<CatalogItem> match = state.getCatalog().lookup(broadcast.getItemName());
		if (!match.isPresent())
		{
			return state.getCatalog().isKnownIgnored(broadcast.getItemName())
				? Outcome.NOT_SCOREABLE
				: Outcome.UNKNOWN_ITEM;
		}

		if (!state.isOnRoster(broadcast.getRsn()))
		{
			// Someone in the clan who is not in the event. Nothing to do, and no network call.
			return Outcome.NOT_ON_ROSTER;
		}

		Instant now = clock.now();
		if (!state.getWindow().isOpen(now))
		{
			// The server would reject this anyway; refusing here avoids a pointless round trip and
			// keeps the panel from filling with rejections outside the event.
			return state.getWindow().isBeforeStart(now)
				? Outcome.BEFORE_EVENT
				: Outcome.AFTER_EVENT;
		}

		boolean isSelf = localPlayerName != null
			&& RsnUtil.normalizeForCompare(localPlayerName)
			.equals(RsnUtil.normalizeForCompare(broadcast.getRsn()));

		if (!isSelf && !config.enableObserverReports())
		{
			return Outcome.OBSERVER_DISABLED;
		}

		CatalogItem item = match.get();
		QueuedSubmission submission = build(broadcast, state, item, now, isSelf, localPlayerName);

		// The screenshot is taken NOW even for an observer report whose claim is delayed: by the
		// time the delay elapses the chatbox has scrolled and the moment is gone. If the claim then
		// comes back as a duplicate, the held image is simply discarded.
		long delay = isSelf ? 0L : observerDelayMillis(state);

		if (config.captureScreenshot())
		{
			screenshots.capture(executor, config.screenshotScalePercent(),
				state.getPolicy().maxProofBytes, png ->
				{
					if (png != null)
					{
						persistence.writeProof(submission.getDedupeKey(), png);
						submission.setHasProof(true);
					}
					queue.enqueue(submission, delay);
				});
		}
		else
		{
			queue.enqueue(submission, delay);
		}

		return isSelf ? Outcome.QUEUED_SELF : Outcome.QUEUED_OBSERVER;
	}

	private QueuedSubmission build(ParsedBroadcast broadcast, EventState state, CatalogItem item,
		Instant now, boolean isSelf, @Nullable String localPlayerName)
	{
		QueuedSubmission s = new QueuedSubmission();
		s.setDedupeKey(DedupeKey.of(state.getWindow().getEventId(), broadcast.getRsn(),
			item.getDropItemId()));
		s.setEventId(state.getWindow().getEventId());
		s.setRsn(broadcast.getRsn());
		s.setDropItemId(item.getDropItemId());
		s.setItemDisplayName(item.getCatalogName());
		s.setPoints(item.getPoints());
		s.setItemNameRaw(broadcast.getItemName());
		s.setBroadcastType(broadcast.getKind().getWireName());
		s.setRawMessage(broadcast.getRawMessage());
		s.setOccurredAt(now.toString());
		s.setObserver(!isSelf);
		// Falls back to the dropper's own name when we somehow have no local player: the server
		// only uses this to make a retry from the same client idempotent.
		s.setReporterRsn(localPlayerName == null ? broadcast.getRsn() : localPlayerName);
		s.setState(SubmissionState.PENDING_CLAIM);
		return s;
	}

	/**
	 * How long an observer waits before reporting someone else's drop.
	 *
	 * <p>The point is to let the player who actually got the drop claim it first, so the screenshot
	 * that ends up attached is theirs. The jitter then spreads the remaining clients out instead of
	 * having twenty of them fire at the same instant once the delay elapses.
	 */
	private long observerDelayMillis(EventState state)
	{
		long base = Math.max(0L, state.getPolicy().observerDelayMs);
		long jitter = Math.max(0L, state.getPolicy().observerJitterMs);
		return base + (jitter == 0L ? 0L : (long) (random.nextDouble() * jitter));
	}

	/** What the pipeline decided, so the caller can log or announce appropriately. */
	public enum Outcome
	{
		QUEUED_SELF,
		QUEUED_OBSERVER,
		NOT_READY,
		UNKNOWN_ITEM,
		NOT_SCOREABLE,
		NOT_ON_ROSTER,
		BEFORE_EVENT,
		AFTER_EVENT,
		OBSERVER_DISABLED
	}
}
