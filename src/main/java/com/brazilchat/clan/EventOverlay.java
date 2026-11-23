package com.brazilchat.clan;

import com.brazilchat.BrazilChatConfig;
import com.brazilchat.clan.api.ClanDtos;
import com.brazilchat.ui.Palette;
import com.brazilchat.ui.Strings;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * During a clan event that has a password, shows the event name, the password and the current
 * Brasília time on screen -- so every screenshot taken as proof carries both. Only events whose
 * password the site released to this player (signed up, or no sign-ups needed) qualify.
 */
public class EventOverlay extends OverlayPanel
{
	/** Shown from a little before the start, so people can check their setup. */
	private static final Duration LEAD = Duration.ofMinutes(30);
	/** Events without an end time are assumed to last this long. */
	private static final Duration DEFAULT_LENGTH = Duration.ofHours(6);

	private final ClanService service;
	private final BrazilChatConfig config;

	@Inject
	EventOverlay(ClanService service, BrazilChatConfig config)
	{
		this.service = service;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.eventOverlay())
		{
			return null;
		}
		ClanDtos.EventDto event = current(service.current(), Instant.now());
		if (event == null)
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text(event.name)
			.color(Palette.BRAND_GOLD)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left(Strings.LABEL_PASSWORD)
			.right(event.password)
			.rightColor(Palette.BRAND_GOLD)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left(Strings.OVERLAY_TIME)
			.right(Instant.now().atZone(Strings.BRASILIA).format(Strings.TIME))
			.build());
		return super.render(graphics);
	}

	@Nullable
	static ClanDtos.EventDto current(ClanData data, Instant now)
	{
		ClanDtos.Bootstrap b = data.getBootstrap();
		if (data.getStatus() != ClanData.Status.ONLINE || b == null || b.events == null)
		{
			return null;
		}
		for (ClanDtos.EventDto e : b.events)
		{
			Instant start = ClanService.parse(e.startDate);
			if (e.password == null || start == null)
			{
				continue;
			}
			Instant end = ClanService.parse(e.endDate);
			if (end == null)
			{
				end = start.plus(DEFAULT_LENGTH);
			}
			if (!now.isBefore(start.minus(LEAD)) && now.isBefore(end))
			{
				return e;
			}
		}
		return null;
	}
}
