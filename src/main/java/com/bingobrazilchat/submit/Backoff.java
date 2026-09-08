package com.bingobrazilchat.submit;

import com.bingobrazilchat.api.ApiException;
import java.util.Random;

/**
 * How long to wait before trying a failed submission again.
 *
 * <p>Full jitter rather than plain exponential: a clan-wide outage would otherwise have every
 * client retry in lockstep, and the site would be hit by the whole herd at once each time.
 * Picking uniformly from {@code [0, cap]} spreads them out.
 */
public final class Backoff
{
	static final long BASE_MS = 5_000L;
	static final long CAP_MS = 15 * 60 * 1000L;

	private final Random random;

	public Backoff()
	{
		this(new Random());
	}

	/** Seeded constructor, for tests that need a deterministic sequence. */
	public Backoff(Random random)
	{
		this.random = random;
	}

	/**
	 * @param attempt      how many attempts have already failed (0 for the first failure)
	 * @param retryAfterSeconds what the server asked for, or 0 if it did not say
	 */
	public long delayMillis(int attempt, long retryAfterSeconds)
	{
		if (retryAfterSeconds > 0)
		{
			// An explicit Retry-After is the server telling us exactly when it will be ready.
			return Math.min(CAP_MS, retryAfterSeconds * 1000L);
		}

		long ceiling = ceilingFor(attempt);
		return (long) (random.nextDouble() * ceiling);
	}

	static long ceilingFor(int attempt)
	{
		int clamped = Math.max(0, Math.min(attempt, 20));
		long scaled = BASE_MS << clamped;
		// The shift overflows well past the cap for large attempt counts; guard both ways.
		return scaled <= 0 || scaled > CAP_MS ? CAP_MS : scaled;
	}

	/**
	 * Whether a failure is worth another attempt at all.
	 *
	 * <p>Getting this wrong in the permissive direction is what turns one misconfigured token into
	 * a whole clan hammering the site indefinitely.
	 */
	public static boolean shouldRetry(ApiException error)
	{
		return error.isRetryable();
	}
}
