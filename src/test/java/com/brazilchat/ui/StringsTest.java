package com.brazilchat.ui;

import java.time.Duration;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class StringsTest
{
	@Test
	public void timeInClan()
	{
		assertEquals("1 dia", Strings.timeInClan(1));
		assertEquals("12 dias", Strings.timeInClan(12));
		assertEquals("1 mês", Strings.timeInClan(31));
		assertEquals("5 meses", Strings.timeInClan(155));
		assertEquals("1 ano", Strings.timeInClan(365));
		assertEquals("2 anos e 3 meses", Strings.timeInClan(30 * 27));
	}

	@Test
	public void rankLabelUsesTheClanNames()
	{
		assertEquals("Soldado", Strings.rankLabel("Corporal"));
		assertEquals("GM", Strings.rankLabel("marshal"));
		assertEquals("Owner", Strings.rankLabel("Owner"));
		assertEquals("-", Strings.rankLabel(null));
	}

	@Test
	public void newAnnouncementIsOneLineWithTheBodyShortened()
	{
		assertEquals("[Brazil Chat] Novo anúncio: Bingo de dezembro!", Strings.newAnnouncement("Bingo de dezembro!", null));
		assertEquals("[Brazil Chat] Novo anúncio: Bingo - Taxa 5M. Inscrições até sexta.",
			Strings.newAnnouncement("Bingo", "Taxa 5M.\n\nInscrições   até sexta."));
		String longBody = new String(new char[300]).replace('\0', 'a');
		assertEquals(160, Strings.newAnnouncement("T", longBody).length() - "[Brazil Chat] Novo anúncio: T - ".length());
	}

	@Test
	public void gp()
	{
		assertEquals("950", Strings.gp(950));
		assertEquals("250K", Strings.gp(250_000));
		assertEquals("12.5M", Strings.gp(12_500_000));
		assertEquals("5M", Strings.gp(5_000_000));
		assertEquals("1.2B", Strings.gp(1_200_000_000L));
	}

	@Test
	public void clanErrorsAreTranslatedAndUnknownTransportErrorsAreGeneric()
	{
		assertEquals("O evento está lotado.", Strings.clanError("event_full"));
		assertEquals("Não foi possível acessar o site", Strings.clanError("http_502"));
		assertEquals("Não foi possível acessar o site", Strings.clanError("connect timed out"));
	}

	@Test
	public void duration()
	{
		assertEquals("2d 4h", Strings.duration(Duration.ofHours(52)));
		assertEquals("3h 12m", Strings.duration(Duration.ofMinutes(192)));
		assertEquals("4m 9s", Strings.duration(Duration.ofSeconds(249)));
	}
}
