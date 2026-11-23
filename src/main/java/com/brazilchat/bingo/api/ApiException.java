package com.brazilchat.bingo.api;

import lombok.Getter;

/**
 * A failed API call, carrying the one thing the queue actually needs to decide: whether trying
 * again could ever help.
 *
 * <p>Getting this classification wrong in the retryable direction is expensive -- a bad shared
 * token would otherwise have every client in the clan retrying a 401 against Vercel forever.
 */
@Getter
public class ApiException extends Exception
{
	/** HTTP status, or -1 for a transport-level failure (timeout, DNS, connection reset). */
	private final int httpStatus;

	private final boolean retryable;

	/** Seconds the server asked us to wait, from Retry-After. Zero when it did not say. */
	private final long retryAfterSeconds;

	public ApiException(String message, int httpStatus, boolean retryable, long retryAfterSeconds)
	{
		super(message);
		this.httpStatus = httpStatus;
		this.retryable = retryable;
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public static ApiException transport(String message)
	{
		// No response at all: the network or the server is down, and both recover on their own.
		return new ApiException(message, -1, true, 0L);
	}

	public static ApiException fromStatus(int status, String body, long retryAfterSeconds)
	{
		return new ApiException("HTTP " + status + (body == null || body.isEmpty() ? "" : ": " + body),
			status, isRetryableStatus(status), retryAfterSeconds);
	}

	private static boolean isRetryableStatus(int status)
	{
		// 429 and 5xx are the server telling us to come back. Everything else in the 4xx range is
		// a statement about the request itself, which will not improve by repeating it.
		return status == 429 || status >= 500;
	}

	/** True when the failure means the token is wrong, so the panel can say so plainly. */
	public boolean isAuthFailure()
	{
		return httpStatus == 401 || httpStatus == 403;
	}
}
