package com.bingobrazilchat.submit;

import com.bingobrazilchat.api.ApiException;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Retry classification is the guard against one misconfigured token turning a whole clan into a
 * retry loop against the site. A false "retryable" here is a self-inflicted outage.
 */
public class BackoffTest
{
	@Test
	public void transportFailuresRetry()
	{
		assertTrue(Backoff.shouldRetry(ApiException.transport("connection reset")));
	}

	@Test
	public void serverErrorsRetry()
	{
		assertTrue(Backoff.shouldRetry(ApiException.fromStatus(500, "", 0)));
		assertTrue(Backoff.shouldRetry(ApiException.fromStatus(502, "", 0)));
		assertTrue(Backoff.shouldRetry(ApiException.fromStatus(503, "", 0)));
		assertTrue(Backoff.shouldRetry(ApiException.fromStatus(504, "", 0)));
	}

	@Test
	public void rateLimitingRetries()
	{
		assertTrue(Backoff.shouldRetry(ApiException.fromStatus(429, "", 30)));
	}

	@Test
	public void clientErrorsAreTerminal()
	{
		// These will never improve by asking again, and a bad shared token is a 401.
		assertFalse(Backoff.shouldRetry(ApiException.fromStatus(400, "", 0)));
		assertFalse(Backoff.shouldRetry(ApiException.fromStatus(401, "", 0)));
		assertFalse(Backoff.shouldRetry(ApiException.fromStatus(403, "", 0)));
		assertFalse(Backoff.shouldRetry(ApiException.fromStatus(404, "", 0)));
		assertFalse(Backoff.shouldRetry(ApiException.fromStatus(413, "", 0)));
	}

	@Test
	public void authFailuresAreIdentifiable()
	{
		// The panel says "check your token" only for these.
		assertTrue(ApiException.fromStatus(401, "", 0).isAuthFailure());
		assertTrue(ApiException.fromStatus(403, "", 0).isAuthFailure());
		assertFalse(ApiException.fromStatus(500, "", 0).isAuthFailure());
	}

	@Test
	public void retryAfterWins()
	{
		Backoff backoff = new Backoff(new Random(1));
		assertEquals(30_000L, backoff.delayMillis(0, 30));
	}

	@Test
	public void retryAfterIsStillCapped()
	{
		Backoff backoff = new Backoff(new Random(1));
		assertEquals(Backoff.CAP_MS, backoff.delayMillis(0, 60 * 60));
	}

	@Test
	public void delayStaysWithinTheCeilingForTheAttempt()
	{
		Backoff backoff = new Backoff(new Random(42));
		for (int attempt = 0; attempt < 12; attempt++)
		{
			long delay = backoff.delayMillis(attempt, 0);
			assertTrue("attempt " + attempt, delay >= 0);
			assertTrue("attempt " + attempt, delay <= Backoff.ceilingFor(attempt));
			assertTrue("attempt " + attempt, delay <= Backoff.CAP_MS);
		}
	}

	@Test
	public void ceilingGrowsThenSaturates()
	{
		assertEquals(Backoff.BASE_MS, Backoff.ceilingFor(0));
		assertEquals(Backoff.BASE_MS * 2, Backoff.ceilingFor(1));
		assertEquals(Backoff.BASE_MS * 4, Backoff.ceilingFor(2));
		assertEquals(Backoff.CAP_MS, Backoff.ceilingFor(30));
		// Guards the left-shift overflow: a large attempt count must not wrap to a tiny delay.
		assertEquals(Backoff.CAP_MS, Backoff.ceilingFor(Integer.MAX_VALUE));
	}

	@Test
	public void jitterSpreadsRetriesOut()
	{
		// Without jitter every client in the clan would retry in lockstep after an outage and hit
		// the site as one wave.
		Backoff backoff = new Backoff(new Random(7));
		long first = backoff.delayMillis(5, 0);
		boolean sawDifferent = false;
		for (int i = 0; i < 20 && !sawDifferent; i++)
		{
			sawDifferent = backoff.delayMillis(5, 0) != first;
		}
		assertTrue("expected jittered delays to differ", sawDifferent);
	}
}
