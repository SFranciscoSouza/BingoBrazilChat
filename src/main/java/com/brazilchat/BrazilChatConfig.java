package com.brazilchat;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(BrazilChatConfig.GROUP)
public interface BrazilChatConfig extends Config
{
	String GROUP = "brazilchat";

	/** The config group used before the rename, read once by {@link ConfigMigration}. */
	String LEGACY_GROUP = "bingobrazilchat";

	// ---------------------------------------------------------------------------------- sections

	@ConfigSection(
		name = "Clã",
		description = "Eventos, inscrições, anúncios e ferramentas de staff. Usa o site do Brazil "
			+ "Chat.",
		position = 5
	)
	String clanSection = "clanSection";

	@ConfigSection(
		name = "Bingo",
		description = "Envio automático de drops para o placar do evento de bingo.",
		position = 10
	)
	String bingoSection = "bingoSection";

	@ConfigSection(
		name = "Mensagens do clã",
		description = "Ocultar ou destacar anúncios do clã no chat. Só muda o que você vê.",
		position = 20
	)
	String broadcastSection = "broadcastSection";

	@ConfigSection(
		name = "Avisos",
		description = "Avisos mostrados só para você.",
		position = 30
	)
	String noticeSection = "noticeSection";

	@ConfigSection(
		name = "<html>Este plugin envia dados ao site<br>"
			+ "do Brazil Chat quando você ativa<br>"
			+ "as opções. Abra para ler.</html>",
		description = "Tudo vai só para brazilchat.vercel.app. "
			+ "(1) \"Conectar ao Brazil Chat\": envia seu RSN e o identificador da sua conta "
			+ "RuneLite (account hash) para mostrar eventos, inscrições e anúncios, envia suas "
			+ "inscrições, e uma vez por login suas conquistas (nível total, max, diários e tier de "
			+ "Combat Achievements) para a lista de promoções. "
			+ "(2) \"Chave de administrador\" (só staff): o plugin também envia a lista de "
			+ "membros do clã (nome, cargo, data de entrada). "
			+ "(3) \"Token do evento\": quando um drop listado é anunciado no clã, envia o nome "
			+ "do jogador, o item, o texto do anúncio e uma captura de tela. "
			+ "Com as opções desligadas, nada é enviado. Perfil, membros online e filtro de "
			+ "mensagens funcionam só no seu computador.",
		position = 99,
		closedByDefault = true
	)
	String privacySection = "privacySection";

	// -------------------------------------------------------------------------------------- clan

	@ConfigItem(
		keyName = "connectClan",
		name = "Conectar ao Brazil Chat",
		description = "Mostra eventos, inscrições, anúncios e competições do clã. Envia seu RSN, "
			+ "o identificador da sua conta RuneLite e suas conquistas (nível total, diários, "
			+ "Combat Achievements) ao site do Brazil Chat. Desligado, nada é enviado.",
		position = 2,
		section = clanSection,
		warning = "Isto envia seu RSN, o identificador da sua conta RuneLite (account hash) e suas "
			+ "conquistas (nível total, diários, Combat Achievements) para brazilchat.vercel.app, "
			+ "um site que não é controlado nem verificado pelos desenvolvedores do RuneLite. "
			+ "Continuar?"
	)
	default boolean connectClan()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showAnnouncements",
		name = "Mostrar anúncios do clã",
		description = "Mostra os anúncios da staff na aba Início, para membros do clã Brazil Chat. "
			+ "Busca os anúncios no site do Brazil Chat sem enviar seu nome nem sua conta.",
		position = 0,
		section = clanSection
	)
	default boolean showAnnouncements()
	{
		return true;
	}

	@ConfigItem(
		keyName = "announceNewAnnouncements",
		name = "Avisar novos anúncios no chat",
		description = "Quando a staff publica um anúncio, mostra-o uma vez no chat (no próximo "
			+ "login, ou em até 5 minutos se você estiver online). Só para você.",
		position = 1,
		section = clanSection
	)
	default boolean announceNewAnnouncements()
	{
		return true;
	}

	@ConfigItem(
		keyName = "eventReminders",
		name = "Lembrete de eventos",
		description = "Avisa no chat quando um evento em que você se inscreveu está para começar.",
		position = 3,
		section = clanSection
	)
	default boolean eventReminders()
	{
		return true;
	}

	@ConfigItem(
		keyName = "reminderMinutes",
		name = "Avisar quantos minutos antes",
		description = "Quanto tempo antes do início do evento o lembrete aparece.",
		position = 4,
		section = clanSection
	)
	@Range(min = 1, max = 240)
	default int reminderMinutes()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "eventOverlay",
		name = "Mostrar senha do evento na tela",
		description = "Durante um evento com senha, mostra o nome, a senha e o horário de "
			+ "Brasília na tela, para as capturas de prova.",
		position = 5,
		section = clanSection
	)
	default boolean eventOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "staffKey",
		name = "Chave de administrador",
		description = "Só para Administrator, Deputy Owner e Owner. A chave é gerada pelo Owner no "
			+ "site. Com ela o plugin mostra a aba Staff e envia a lista de membros do clã.",
		position = 6,
		section = clanSection,
		secret = true
	)
	default String staffKey()
	{
		return "";
	}

	// ------------------------------------------------------------------------------------- bingo

	@ConfigItem(
		keyName = "sharedToken",
		name = "Token do evento",
		description = "O token que a organização do evento te passou. O bingo não faz nada - e "
			+ "não se conecta à internet - enquanto este campo estiver vazio.",
		position = 1,
		secret = true,
		section = bingoSection
	)
	default String sharedToken()
	{
		return "";
	}

	@ConfigItem(
		keyName = "captureScreenshot",
		name = "Anexar captura de tela",
		description = "Tira uma captura de tela quando um drop listado é anunciado e envia como "
			+ "prova. Desligado, o drop ainda é registrado, só que sem imagem.",
		position = 2,
		section = bingoSection
	)
	default boolean captureScreenshot()
	{
		return true;
	}

	@ConfigItem(
		keyName = "screenshotScale",
		name = "Tamanho da captura",
		description = "Escala aplicada antes do envio, em % do tamanho do cliente. Menor = envio "
			+ "mais leve.",
		position = 3,
		section = bingoSection
	)
	@Range(min = 25, max = 100)
	default int screenshotScalePercent()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "enableObserverReports",
		name = "Reportar drops de outros",
		description = "Também reporta drops de outros jogadores inscritos no evento, para que o "
			+ "drop conte mesmo se quem pegou não usa o plugin.",
		position = 4,
		section = bingoSection
	)
	default boolean enableObserverReports()
	{
		return true;
	}

	@ConfigItem(
		keyName = "announceInChat",
		name = "Confirmar no chat",
		description = "Mostra uma mensagem local quando um drop é registrado no site.",
		position = 5,
		section = bingoSection
	)
	default boolean announceInChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "debugLogUnmatched",
		name = "Registrar anúncios não reconhecidos",
		description = "Escreve no log do cliente anúncios que parecem drops mas não foram "
			+ "reconhecidos. Útil para reportar um item que faltou.",
		position = 6,
		section = bingoSection
	)
	default boolean debugLogUnmatched()
	{
		return false;
	}

	// -------------------------------------------------------------------------------- broadcasts

	@ConfigItem(
		keyName = "minDropValue",
		name = "Ocultar drops abaixo de (gp)",
		description = "Esconde anúncios de drop com valor menor que este. 0 mostra todos. "
			+ "Loot de raid, pets e collection log nunca são ocultados.",
		position = 1,
		section = broadcastSection
	)
	default int minDropValue()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "hideLevels",
		name = "Ocultar níveis",
		description = "Esconde anúncios de nível, nível total e marcos de XP.",
		position = 2,
		section = broadcastSection
	)
	default boolean hideLevels()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideQuests",
		name = "Ocultar quests",
		description = "Esconde anúncios de quests concluídas.",
		position = 3,
		section = broadcastSection
	)
	default boolean hideQuests()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideDiaries",
		name = "Ocultar diários",
		description = "Esconde anúncios de Achievement Diaries.",
		position = 4,
		section = broadcastSection
	)
	default boolean hideDiaries()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideCombatAchievements",
		name = "Ocultar Combat Achievements",
		description = "Esconde anúncios de Combat Achievements.",
		position = 5,
		section = broadcastSection
	)
	default boolean hideCombatAchievements()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hidePersonalBests",
		name = "Ocultar recordes pessoais",
		description = "Esconde anúncios de novos recordes pessoais (PB).",
		position = 6,
		section = broadcastSection
	)
	default boolean hidePersonalBests()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hidePvp",
		name = "Ocultar PvP e mortes",
		description = "Esconde anúncios de PvP e de mortes de Hardcore.",
		position = 7,
		section = broadcastSection
	)
	default boolean hidePvp()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideMembership",
		name = "Ocultar entradas e saídas",
		description = "Esconde anúncios de membros entrando ou saindo do clã.",
		position = 8,
		section = broadcastSection
	)
	default boolean hideMembership()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highlightRare",
		name = "Destacar raros",
		description = "Pinta de outra cor os anúncios de loot de raid, pets, collection log e "
			+ "total máximo.",
		position = 9,
		section = broadcastSection
	)
	default boolean highlightRare()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "highlightColour",
		name = "Cor do destaque",
		description = "Cor usada para os anúncios raros.",
		position = 10,
		section = broadcastSection
	)
	default Color highlightColour()
	{
		return new Color(0, 168, 89);
	}

	@ConfigItem(
		keyName = "notifyRare",
		name = "Notificar raros",
		description = "Envia uma notificação do RuneLite quando sai um anúncio raro no clã.",
		position = 11,
		section = broadcastSection
	)
	default boolean notifyRare()
	{
		return false;
	}

	// ----------------------------------------------------------------------------------- notices

	@ConfigItem(
		keyName = "announceRankUp",
		name = "Avisar promoção",
		description = "Só para você: avisa quando você já cumpre os requisitos do próximo cargo "
			+ "(para pedir a promoção) e dá parabéns quando seu cargo no clã sobe.",
		position = 1,
		section = noticeSection
	)
	default boolean announceRankUp()
	{
		return true;
	}
}
