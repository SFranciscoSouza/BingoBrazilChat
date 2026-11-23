package com.brazilchat.bingo.capture;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns a captured frame into PNG bytes small enough to upload.
 *
 * <p>Everything here is slow enough (tens to low hundreds of milliseconds) that it must never run
 * on the client thread -- it would show up as a visible stutter at the exact moment the player got
 * their drop.
 */
@Slf4j
public final class ImageEncoder
{
	private ImageEncoder()
	{
	}

	/**
	 * @param scalePercent 25-100; downscaling is what keeps the upload comfortably under both our
	 *                     own cap and Vercel's 4.5 MB request-body limit
	 * @return PNG bytes, or null if the frame could not be encoded
	 */
	@Nullable
	public static byte[] toPng(Image source, int scalePercent, long maxBytes)
	{
		BufferedImage image = toBuffered(source);
		if (image == null)
		{
			return null;
		}

		int clamped = Math.max(25, Math.min(100, scalePercent));
		if (clamped < 100)
		{
			image = scale(image, clamped / 100.0);
		}

		byte[] encoded = encode(image);
		if (encoded == null)
		{
			return null;
		}

		// One halving pass rather than a loop: if a 50%-of-requested-size render still will not
		// fit, the frame is pathological and sending no image beats sending a useless one.
		if (encoded.length > maxBytes)
		{
			log.debug("Brazil Chat: screenshot {} bytes exceeds {}, halving", encoded.length, maxBytes);
			byte[] smaller = encode(scale(image, 0.5));
			return smaller != null && smaller.length <= maxBytes ? smaller : null;
		}

		return encoded;
	}

	@Nullable
	private static BufferedImage toBuffered(Image source)
	{
		if (source == null)
		{
			return null;
		}
		if (source instanceof BufferedImage)
		{
			return (BufferedImage) source;
		}

		int width = source.getWidth(null);
		int height = source.getHeight(null);
		if (width <= 0 || height <= 0)
		{
			return null;
		}

		BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = copy.createGraphics();
		try
		{
			g.drawImage(source, 0, 0, null);
		}
		finally
		{
			g.dispose();
		}
		return copy;
	}

	private static BufferedImage scale(BufferedImage source, double factor)
	{
		int width = Math.max(1, (int) Math.round(source.getWidth() * factor));
		int height = Math.max(1, (int) Math.round(source.getHeight() * factor));

		BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = scaled.createGraphics();
		try
		{
			// Bilinear, not nearest-neighbour: the proof has to stay legible enough to read the
			// broadcast line in the chatbox.
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(source, 0, 0, width, height, null);
		}
		finally
		{
			g.dispose();
		}
		return scaled;
	}

	@Nullable
	private static byte[] encode(BufferedImage image)
	{
		try
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			// ImageIO.write returns false rather than throwing when no writer is available.
			if (!ImageIO.write(image, "png", out))
			{
				log.debug("Brazil Chat: no PNG writer available");
				return null;
			}
			return out.toByteArray();
		}
		catch (IOException e)
		{
			log.debug("Brazil Chat: failed to encode screenshot", e);
			return null;
		}
	}
}
