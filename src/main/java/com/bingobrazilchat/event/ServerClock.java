package com.bingobrazilchat.event;

import java.time.Duration;
import java.time.Instant;

/**
 * Corrects the local clock against the server's.
 *
 * <p>Players' machine clocks are routinely minutes -- occasionally hours -- out. Since the event
 * window is a hard gate, checking it against an uncorrected local clock would silently exclude
 * someone whose PC is set wrong, and they would have no way of telling why their drops stopped
 * counting. Every config fetch carries {@code serverTime}, so the offset is free to maintain.
 *
 * <p>The offset is only ever applied to window checks and to the {@code occurredAt} we report. It
 * is deliberately <em>not</em> applied to the dedupe key, which contains no time at all.
 */
public class ServerClock
{
	/** Ignore offsets below this: they are network latency, not a wrong clock. */
	private static final long NOISE_FLOOR_MS = 2_000L;

	private volatile long offsetMillis = 0L;

	/**
	 * @param serverTime  the instant the server reported
	 * @param requestSent when we sent the request, used to discount round-trip latency
	 */
	public void sync(Instant serverTime, Instant requestSent, Instant responseReceived)
	{
		if (serverTime == null || requestSent == null || responseReceived == null)
		{
			return;
		}

		// Assume the server generated its timestamp halfway through the round trip.
		long roundTripMs = Duration.between(requestSent, responseReceived).toMillis();
		Instant localAtServerTime = requestSent.plusMillis(Math.max(0L, roundTripMs) / 2);
		long candidate = Duration.between(localAtServerTime, serverTime).toMillis();

		offsetMillis = Math.abs(candidate) < NOISE_FLOOR_MS ? 0L : candidate;
	}

	/** Local wall clock corrected by the measured offset. */
	public Instant now()
	{
		return Instant.now().plusMillis(offsetMillis);
	}

	public long getOffsetMillis()
	{
		return offsetMillis;
	}

	public boolean isSkewed()
	{
		return offsetMillis != 0L;
	}
}
