package com.bingobrazilchat.submit;

import com.bingobrazilchat.api.ApiCallback;
import com.bingobrazilchat.api.ApiDtos;
import com.bingobrazilchat.api.ApiException;
import com.bingobrazilchat.api.BingoApiClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Drives every submission from "a broadcast happened" to a terminal state.
 *
 * <p><b>Threading:</b> every method here runs on the plugin's own single-threaded executor. That is
 * the entire concurrency design -- because all queue mutation happens on one thread, none of this
 * state needs locks. API callbacks arrive on OkHttp dispatcher threads and immediately hop back via
 * {@code executor.execute(...)}; nothing else may touch the queue.
 *
 * <p><b>The herd:</b> no attempt is made to elect a single reporter. Every client submits and the
 * server collapses them, because leader election would be fragile and would lose the drop entirely
 * whenever the elected client was the one not running the plugin.
 */
@Slf4j
public class SubmissionQueue
{
	/** How often to look for work. Backoff decides when an individual item is actually due. */
	private static final long PUMP_INTERVAL_SECONDS = 5L;

	/** How long one client remembers a drop it already handled, so it cannot submit it twice. */
	private static final long SEEN_TTL_MS = TimeUnit.MINUTES.toMillis(15);

	/** Nothing is retried forever, even if the event is still open. */
	private static final long MAX_LIFETIME_MS = TimeUnit.HOURS.toMillis(24);

	private final ScheduledExecutorService executor;
	private final BingoApiClient api;
	private final QueuePersistence persistence;
	private final Backoff backoff;
	private final Consumer<List<QueuedSubmission>> onChanged;
	private final Consumer<QueuedSubmission> onRecorded;

	private final List<QueuedSubmission> queue = new ArrayList<>();
	private final Map<String, Long> seen = new HashMap<>();

	/** Guards against two pumps overlapping if a call outlives the interval. */
	private boolean pumping = false;

	public SubmissionQueue(
		ScheduledExecutorService executor,
		BingoApiClient api,
		QueuePersistence persistence,
		Backoff backoff,
		Consumer<List<QueuedSubmission>> onChanged,
		Consumer<QueuedSubmission> onRecorded)
	{
		this.executor = executor;
		this.api = api;
		this.persistence = persistence;
		this.backoff = backoff;
		this.onChanged = onChanged;
		this.onRecorded = onRecorded;
	}

	/** Restores anything left over from a previous session and starts the retry pump. */
	public void start()
	{
		executor.execute(() ->
		{
			queue.clear();
			queue.addAll(persistence.load());

			long now = System.currentTimeMillis();
			for (QueuedSubmission s : queue)
			{
				// Anything mid-flight when the client died is due again immediately; there is no
				// value in honouring a backoff that was measured against a previous session.
				if (!s.getState().isTerminal())
				{
					s.setNextAttemptAtMillis(now);
					seen.put(s.getDedupeKey(), now + SEEN_TTL_MS);
				}
			}

			persistence.sweep(queue);
			notifyChanged();
			log.debug("Bingo BrazilChat: restored {} queued submissions", queue.size());
		});

		executor.scheduleWithFixedDelay(this::pump,
			PUMP_INTERVAL_SECONDS, PUMP_INTERVAL_SECONDS, TimeUnit.SECONDS);
	}

	/**
	 * Adds a submission unless this client already handled that exact drop.
	 *
	 * @param delayMillis how long to hold it before the first attempt; non-zero for observer
	 *                    reports, so the person who actually got the drop reports it first
	 */
	public void enqueue(QueuedSubmission submission, long delayMillis)
	{
		executor.execute(() ->
		{
			long now = System.currentTimeMillis();
			expireSeen(now);

			if (seen.containsKey(submission.getDedupeKey()))
			{
				// This client already has this drop in hand. The server would collapse it anyway,
				// but not sending it at all is cheaper and keeps the panel honest.
				log.debug("Bingo BrazilChat: already handling {}", submission.getDedupeKey());
				return;
			}

			submission.setNextAttemptAtMillis(now + Math.max(0L, delayMillis));
			submission.setExpiresAtMillis(now + MAX_LIFETIME_MS);
			seen.put(submission.getDedupeKey(), now + SEEN_TTL_MS);

			queue.add(submission);
			persist();
			notifyChanged();
		});
	}

	/** Caps how long anything is retried, so a queue cannot outlive the event by more than an hour. */
	public void setEventEnd(long eventEndMillis)
	{
		executor.execute(() ->
		{
			long deadline = eventEndMillis + TimeUnit.HOURS.toMillis(1);
			for (QueuedSubmission s : queue)
			{
				if (!s.getState().isTerminal())
				{
					s.setExpiresAtMillis(Math.min(s.getExpiresAtMillis(), deadline));
				}
			}
		});
	}

	public void clearTerminal()
	{
		executor.execute(() ->
		{
			queue.removeIf(s -> s.getState().isTerminal());
			persist();
			notifyChanged();
		});
	}

	public List<QueuedSubmission> snapshot()
	{
		return Collections.unmodifiableList(new ArrayList<>(queue));
	}

	// ------------------------------------------------------------------------------------------

	private void pump()
	{
		if (pumping || !api.isConfigured())
		{
			return;
		}

		long now = System.currentTimeMillis();
		expireSeen(now);

		QueuedSubmission due = null;
		for (QueuedSubmission s : queue)
		{
			if (s.isExpired(now) && !s.getState().isTerminal())
			{
				fail(s, "Gave up after " + s.getAttempt() + " attempts");
				continue;
			}
			if (s.isDue(now))
			{
				due = s;
				break;
			}
		}

		if (due == null)
		{
			return;
		}

		pumping = true;
		if (due.getState() == SubmissionState.PENDING_CLAIM)
		{
			sendClaim(due);
		}
		else
		{
			sendProof(due);
		}
	}

	private void sendClaim(QueuedSubmission s)
	{
		ApiDtos.ClaimRequest req = new ApiDtos.ClaimRequest();
		req.eventId = s.getEventId();
		req.rsn = s.getRsn();
		req.dropItemId = s.getDropItemId();
		req.itemNameRaw = s.getItemNameRaw();
		req.broadcastType = s.getBroadcastType();
		req.rawMessage = s.getRawMessage();
		req.occurredAt = s.getOccurredAt();
		req.observer = s.isObserver();
		req.reporterRsn = s.getReporterRsn();
		req.dedupeKey = s.getDedupeKey();
		req.hasProof = s.isHasProof();

		api.claimDrop(req, new ApiCallback<ApiDtos.ClaimResponse>()
		{
			@Override
			public void onSuccess(ApiDtos.ClaimResponse result)
			{
				executor.execute(() -> onClaimResult(s, result));
			}

			@Override
			public void onFailure(ApiException error)
			{
				executor.execute(() -> onAttemptFailed(s, error));
			}
		});
	}

	private void onClaimResult(QueuedSubmission s, ApiDtos.ClaimResponse result)
	{
		pumping = false;

		if (result.isRejected())
		{
			s.setState(SubmissionState.REJECTED);
			s.setStatusDetail(describeRejection(result.reason));
			finish(s);
			return;
		}

		if (result.isDuplicate())
		{
			// Someone else's client reported it first. That is a success: the drop is on the board.
			s.setState(SubmissionState.DUPLICATE);
			s.setSubmissionId(result.submissionId);
			finish(s);
			return;
		}

		if (!result.isCreated() || result.submissionId == null)
		{
			onAttemptFailed(s, ApiException.transport("Unexpected claim status: " + result.status));
			return;
		}

		s.setSubmissionId(result.submissionId);
		s.setProofToken(result.proofUploadToken);
		if (result.itemName != null)
		{
			s.setItemDisplayName(result.itemName);
			s.setPoints(result.points);
		}

		byte[] proof = s.isHasProof() ? persistence.readProof(s.getDedupeKey()) : null;
		if (proof == null || s.getProofToken() == null)
		{
			// No screenshot to attach: the drop is recorded, which is what matters.
			s.setState(SubmissionState.DONE);
			finish(s);
			return;
		}

		s.setState(SubmissionState.AWAITING_PROOF);
		s.setAttempt(0);
		s.setNextAttemptAtMillis(System.currentTimeMillis());
		persist();
		notifyChanged();
	}

	private void sendProof(QueuedSubmission s)
	{
		byte[] proof = persistence.readProof(s.getDedupeKey());
		if (proof == null || s.getSubmissionId() == null || s.getProofToken() == null)
		{
			s.setState(SubmissionState.DONE);
			finish(s);
			pumping = false;
			return;
		}

		s.setState(SubmissionState.UPLOADING_PROOF);
		api.uploadProof(s.getSubmissionId(), s.getProofToken(), proof, new ApiCallback<Void>()
		{
			@Override
			public void onSuccess(Void result)
			{
				executor.execute(() ->
				{
					pumping = false;
					s.setState(SubmissionState.DONE);
					finish(s);
				});
			}

			@Override
			public void onFailure(ApiException error)
			{
				executor.execute(() ->
				{
					if (!Backoff.shouldRetry(error))
					{
						// The row exists and scores; only the image failed. Not worth failing over.
						log.debug("Bingo BrazilChat: giving up on proof for {}", s.getDedupeKey());
						pumping = false;
						s.setState(SubmissionState.DONE);
						s.setStatusDetail("Recorded without a screenshot");
						finish(s);
						return;
					}
					s.setState(SubmissionState.AWAITING_PROOF);
					onAttemptFailed(s, error);
				});
			}
		});
	}

	private void onAttemptFailed(QueuedSubmission s, ApiException error)
	{
		pumping = false;

		if (!Backoff.shouldRetry(error))
		{
			fail(s, error.isAuthFailure()
				? "Check your event token in the plugin settings"
				: error.getMessage());
			return;
		}

		s.setAttempt(s.getAttempt() + 1);
		long delay = backoff.delayMillis(s.getAttempt(), error.getRetryAfterSeconds());
		s.setNextAttemptAtMillis(System.currentTimeMillis() + delay);
		s.setStatusDetail("Retrying");

		log.debug("Bingo BrazilChat: attempt {} failed for {}, retrying in {}ms",
			s.getAttempt(), s.getDedupeKey(), delay);

		persist();
		notifyChanged();
	}

	private void fail(QueuedSubmission s, String detail)
	{
		s.setState(SubmissionState.FAILED);
		s.setStatusDetail(detail);
		finish(s);
	}

	/** Terminal states release the screenshot, which is the only large thing we hold. */
	private void finish(QueuedSubmission s)
	{
		persistence.deleteProof(s.getDedupeKey());
		s.setHasProof(false);
		persist();
		notifyChanged();

		// Only announce a drop this client actually put on the board. A DUPLICATE means someone
		// else's client got there first, and every one of them announcing it would mean the whole
		// clan seeing the same line for one drop.
		if (s.getState() == SubmissionState.DONE && onRecorded != null)
		{
			onRecorded.accept(s);
		}
	}

	private void expireSeen(long now)
	{
		Iterator<Map.Entry<String, Long>> it = seen.entrySet().iterator();
		while (it.hasNext())
		{
			if (it.next().getValue() < now)
			{
				it.remove();
			}
		}
	}

	private void persist()
	{
		persistence.save(queue);
	}

	private void notifyChanged()
	{
		if (onChanged != null)
		{
			onChanged.accept(snapshot());
		}
	}

	private static String describeRejection(String reason)
	{
		if (reason == null)
		{
			return "Not counted";
		}
		switch (reason)
		{
			case "event_not_started":
				return "The event has not started yet";
			case "event_ended":
				return "The event has already ended";
			case "player_not_registered":
				return "That player is not on the event roster";
			case "item_not_in_catalog":
				return "That item does not score in this event";
			default:
				return "Not counted (" + reason + ")";
		}
	}
}
