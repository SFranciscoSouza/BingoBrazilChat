package com.bingobrazilchat.submit;

import lombok.Data;

/**
 * One drop on its way to the site.
 *
 * <p>Mutable and Gson-friendly on purpose: instances are serialised straight into RuneLite config
 * so a client restart mid-event does not lose a drop. The screenshot bytes are deliberately
 * <em>not</em> a field -- they live in a file next to the queue, keyed by {@link #dedupeKey},
 * because a base64 image inside a config value would be both huge and slow to rewrite on every
 * state change.
 */
@Data
public class QueuedSubmission
{
	/** Stable across retries and identical on every client that saw this broadcast. */
	private String dedupeKey;

	private String eventId;

	/** The dropper. */
	private String rsn;

	private String dropItemId;

	/** For the panel, so a queued row can name the item without a catalog lookup. */
	private String itemDisplayName;

	private int points;

	private String itemNameRaw;
	private String broadcastType;
	private String rawMessage;

	/** ISO-8601 UTC, server-clock corrected. */
	private String occurredAt;

	/** True when we are reporting someone else's drop rather than our own. */
	private boolean observer;

	/** Whichever account this client is logged in as; the server's retry guard keys on it. */
	private String reporterRsn;

	private SubmissionState state = SubmissionState.PENDING_CLAIM;

	/** Failed attempts so far, driving the backoff ceiling. */
	private int attempt;

	/** Epoch millis before which this should not be tried again. */
	private long nextAttemptAtMillis;

	/** Epoch millis after which we give up entirely. */
	private long expiresAtMillis;

	/** Set once the claim succeeds. */
	private String submissionId;

	private String proofToken;

	/** True while a screenshot for this key is still on disk waiting to be uploaded. */
	private boolean hasProof;

	/** Why a terminal state was reached, shown in the panel. */
	private String statusDetail;

	public boolean isDue(long nowMillis)
	{
		return !state.isTerminal() && nowMillis >= nextAttemptAtMillis;
	}

	public boolean isExpired(long nowMillis)
	{
		return expiresAtMillis > 0 && nowMillis > expiresAtMillis;
	}
}
