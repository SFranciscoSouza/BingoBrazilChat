package com.bingobrazilchat.capture;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.DrawManager;

/**
 * Captures one client frame and hands back PNG bytes.
 *
 * <p>Two hazards this exists to handle:
 *
 * <ol>
 *   <li><b>The frame arrives on the client thread.</b> {@code requestNextFrameListener} fires its
 *       consumer at end-of-frame on the client thread, so the consumer here does nothing except
 *       hand the image to our own executor. This mirrors RuneLite's own {@code ImageCapture}.</li>
 *   <li><b>The callback can silently never fire.</b> Inside {@code DrawManager.processDrawComplete},
 *       if the image supplier returns null -- which happens with some renderer states, or when the
 *       client is not drawing at all -- RuneLite clears the pending listeners without notifying
 *       them. Without the timeout below, a submission would sit waiting for proof forever.</li>
 * </ol>
 */
@Slf4j
@Singleton
public class ScreenshotService
{
	/** Generous: a frame normally lands in well under 100ms, so this only fires when none will. */
	private static final long FRAME_TIMEOUT_MS = 5_000L;

	private final DrawManager drawManager;

	@Inject
	public ScreenshotService(DrawManager drawManager)
	{
		this.drawManager = drawManager;
	}

	/**
	 * Requests the next rendered frame.
	 *
	 * <p>Safe to call from the client thread: {@code requestNextFrameListener} is backed by a
	 * concurrent queue.
	 *
	 * @param onResult receives the PNG bytes, or null if no frame arrived in time or it could not
	 *                 be encoded. Always invoked exactly once, on {@code executor}.
	 */
	public void capture(ScheduledExecutorService executor, int scalePercent, long maxBytes,
		Consumer<byte[]> onResult)
	{
		final AtomicBoolean delivered = new AtomicBoolean(false);

		drawManager.requestNextFrameListener(image ->
		{
			// Still the client thread here. Encoding is far too slow to do inline -- it would
			// stutter the game at the exact moment the player got their drop.
			executor.execute(() ->
			{
				if (!delivered.compareAndSet(false, true))
				{
					return;
				}
				byte[] png = null;
				try
				{
					png = ImageEncoder.toPng(image, scalePercent, maxBytes);
				}
				catch (RuntimeException e)
				{
					log.debug("Bingo BrazilChat: screenshot encode failed", e);
				}
				onResult.accept(png);
			});
		});

		executor.schedule(() ->
		{
			if (delivered.compareAndSet(false, true))
			{
				log.debug("Bingo BrazilChat: no frame within {}ms, submitting without proof",
					FRAME_TIMEOUT_MS);
				onResult.accept(null);
			}
		}, FRAME_TIMEOUT_MS, TimeUnit.MILLISECONDS);
	}

}
