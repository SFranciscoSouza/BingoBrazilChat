package com.brazilchat.ui;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Every piece of player-facing text, in one place. The plugin is PT-BR only; keeping the strings
 * together means a wording change never needs a hunt through the Swing code.
 */
public final class Strings
{
	public static final String PLUGIN_NAME = "Brazil Chat";
	public static final String CLAN_NAME = "Brazil Chat";
	public static final String CHAT_PREFIX = "[Brazil Chat] ";

	/** Event times are shown in Brasília time, not UTC and not the PC's zone. */
	public static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
	public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	// Tabs
	public static final String TAB_HOME = "Início";
	public static final String TAB_CLAN = "Clã";
	public static final String TAB_EVENTS = "Eventos";

	// Header
	public static final String NOT_LOGGED_IN = "Faça login para ver seu perfil";
	public static final String NOT_IN_CLAN = "Você não está em um clã";

	// Home
	public static final String SECTION_YOU = "Você";
	public static final String SECTION_CLAN = "Clã";
	public static final String SECTION_CURRENT_EVENT = "Evento atual";
	public static final String SECTION_LINKS = "Links";
	public static final String LABEL_NAME = "Nome";
	public static final String LABEL_RANK = "Cargo";
	public static final String LABEL_MEMBER_SINCE = "Membro desde";
	public static final String LABEL_TIME_IN_CLAN = "Tempo no clã";
	public static final String LABEL_ONLINE = "Online agora";
	public static final String LABEL_MEMBERS = "Membros";
	public static final String OTHER_CLAN_WARNING =
		"Este plugin é feito para o clã Brazil Chat. Algumas funções só funcionam nele.";
	public static final String NO_EVENT = "Nenhum evento em andamento";
	public static final String OPEN_EVENTS = "Ver detalhes do evento";
	public static final String LINK_SITE = "Site do Brazil Chat";
	public static final String LINK_WOM = "Grupo no Wise Old Man";

	// Clan
	public static final String SECTION_ONLINE_MEMBERS = "Membros online";
	public static final String SEARCH_PLACEHOLDER = "Buscar membro";
	public static final String NO_MEMBERS_ONLINE = "Ninguém online no chat do clã";
	public static final String NO_MEMBERS_MATCH = "Nenhum membro encontrado";
	public static final String STAFF_BADGE = "Staff";

	// Bingo
	public static final String SECTION_BINGO = "Bingo";
	public static final String SECTION_TEAMS = "Times";
	public static final String SECTION_YOUR_DROPS = "Seus drops";
	public static final String NO_SCORES = "Nenhuma pontuação ainda";
	public static final String NOTHING_SENT = "Nenhum drop enviado ainda";
	public static final String CLEAR_FINISHED = "Limpar concluídos";
	public static final String ENTER_TOKEN = "Insira o token do evento nas configurações do plugin";
	public static final String POINTS_SUFFIX = " pts";
	public static final String EVENT_STARTS_IN = "Começa em ";
	public static final String EVENT_ENDS_IN = "Termina em ";
	public static final String EVENT_FINISHED = "Evento encerrado";
	public static final String EVENT_RUNNING = "Em andamento";
	public static final String NO_EVENT_RUNNING = "Nenhum evento em andamento no momento";
	public static final String CHECK_TOKEN = "Verifique o token do evento nas configurações";
	public static final String SITE_UNREACHABLE = "Não foi possível acessar o site";

	// Clan connection
	public static final String TAB_STAFF = "Staff";
	public static final String SECTION_ANNOUNCEMENTS = "Anúncios";
	public static final String SECTION_CALENDAR = "Calendário";
	public static final String SECTION_COMPETITIONS = "Competições do clã";
	public static final String CONNECT_HINT =
		"Ative \"Conectar ao Brazil Chat\" nas configurações do plugin para ver eventos, "
			+ "inscrições e anúncios.";
	public static final String NOT_IN_BRAZIL_CHAT =
		"Entre no chat do clã Brazil Chat para ver eventos e anúncios.";
	public static final String CONNECTING = "Conectando ao site...";
	public static final String NO_EVENTS = "Nenhum evento marcado";
	public static final String NO_COMPETITIONS = "Nenhuma competição em andamento";
	public static final String SIGN_UP = "Inscrever-se";
	public static final String CANCEL_SIGNUP = "Cancelar inscrição";
	public static final String SIGNED_UP = "Inscrição feita!";
	public static final String SIGNUP_CANCELLED = "Inscrição cancelada.";
	public static final String STATUS_SIGNED_UP = "Inscrito";
	public static final String STATUS_PAYMENT_PENDING = "Pagamento pendente";
	public static final String STATUS_PAID = "Pago";
	public static final String STATUS_REMOVED = "Removido";
	public static final String SIGNUPS_CLOSED = "Inscrições fechadas";
	public static final String LABEL_FEE = "Taxa";
	public static final String LABEL_SLOTS = "Vagas";
	public static final String LABEL_SIGNED_UP = "Inscritos";
	public static final String LABEL_PASSWORD = "Senha";
	public static final String LABEL_DEADLINE = "Inscrições até";
	public static final String FREE = "Grátis";
	public static final String REFRESH = "Atualizar";
	public static final String LINK_DISCORD = "Discord do clã";
	public static final String LINK_BINGO = "Site do bingo";
	public static final String CLAN_SITE_URL = "https://brazilchat.com.br";
	public static final String BINGO_SITE_URL = "https://brazilchat.vercel.app";
	public static final String DISCORD_URL = "https://discord.com/invite/b9WnCuF";
	public static final String WOM_URL = "https://wiseoldman.net/groups/3088";

	// Staff
	public static final String SECTION_STAFF_SIGNUPS = "Inscrições";
	public static final String SECTION_INACTIVE = "Inativos (60+ dias sem XP)";
	public static final String SECTION_PROMOTIONS = "Promoções recentes";
	public static final String SECTION_ROSTER = "Lista de membros";
	public static final String STAFF_NEEDS_KEY =
		"Cole sua chave de administrador nas configurações do plugin (Clã → Chave de "
			+ "administrador). Peça a chave ao Owner.";
	public static final String MARK_PAID = "Pago";
	public static final String MARK_UNPAID = "Desfazer pago";
	public static final String REMOVE = "Remover";
	public static final String RESTORE = "Restaurar";
	public static final String OPEN_SIGNUPS = "Abrir inscrições";
	public static final String CLOSE_SIGNUPS = "Fechar inscrições";
	public static final String REMOVE_REASON_PROMPT = "Motivo da remoção (opcional, o membro vê):";
	public static final String NO_SIGNUPS = "Ninguém inscrito ainda";
	public static final String NOBODY_INACTIVE = "Ninguém inativo";
	public static final String NO_PROMOTIONS = "Nenhuma promoção recente";
	public static final String OVERLAY_TIME = "Horário (Brasília)";
	public static final String SECTION_STAFF_ANNOUNCEMENTS = "Anúncios do clã";
	public static final String NEW_ANNOUNCEMENT = "Novo anúncio";
	public static final String EDIT = "Editar";
	public static final String DELETE = "Apagar";
	public static final String ANNOUNCEMENT_TITLE = "Título";
	public static final String ANNOUNCEMENT_BODY = "Mensagem (opcional)";
	public static final String ANNOUNCEMENT_PINNED = "Fixar no topo";
	public static final String ANNOUNCEMENT_SAVED = "Anúncio salvo.";
	public static final String ANNOUNCEMENT_DELETED = "Anúncio apagado.";
	public static final String NO_ANNOUNCEMENTS = "Nenhum anúncio";
	public static final String ANNOUNCEMENT_TITLE_REQUIRED = "O título é obrigatório.";
	public static final String SECTION_WORKLIST = "Promoções sugeridas";
	public static final String SECTION_NEXT_RANK = "Próximo cargo";
	public static final String NEXT_RANK_READY =
		"Você cumpre os requisitos! Avise a staff no Discord para ser promovido.";
	public static final String NEXT_RANK_MANUAL = "Peça na staff pelo Discord.";
	public static final String SECTION_MVP = "MVPs do mês";
	public static final String PROMOTE = "Promover";
	public static final String POSSIBLE = "Possível";
	public static final String NO_WORKLIST = "Ninguém para promover agora";
	public static final String NO_DATA = "sem dados";
	public static final String MVP_PVM = "MVP PvM (Infantry)";
	public static final String MVP_SKILLER = "MVP Skiller (Skiller)";

	public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
	public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

	private Strings()
	{
	}

	/**
	 * The clan's own names for the in-game rank titles (from the rank table on Discord). Titles not
	 * listed are shown as they are in game.
	 */
	public static String rankLabel(String title)
	{
		return com.brazilchat.clan.RankLadder.label(title);
	}

	/** Server error codes from /api/plugin/clan|staff, in Portuguese. */
	public static String clanError(String code)
	{
		if (code == null)
		{
			return "Erro desconhecido";
		}
		switch (code)
		{
			case "not_in_clan":
				return "Seu nome não está na lista do clã ainda. Tente de novo mais tarde.";
			case "identity_mismatch":
				return "Este nome já está ligado a outra conta. Peça a um admin para liberar.";
			case "rate_limited":
				return "Muitas tentativas. Aguarde um minuto.";
			case "already_signed_up":
				return "Você já está inscrito.";
			case "removed_by_staff":
				return "Você foi removido deste evento pela staff.";
			case "signups_closed":
				return "As inscrições estão fechadas.";
			case "deadline_passed":
				return "O prazo de inscrição acabou.";
			case "event_full":
				return "O evento está lotado.";
			case "event_ended":
				return "O evento já terminou.";
			case "not_signed_up":
				return "Você não está inscrito.";
			case "staff_key_required":
			case "staff_key_invalid":
				return "Chave de administrador inválida ou revogada.";
			case "staff_key_other_account":
				return "Esta chave está ligada a outra conta do jogo.";
			case "not_staff":
				return "Seu cargo no clã não é de staff segundo o site.";
			case "roster_too_small":
				return "A lista de membros parecia incompleta e foi ignorada.";
			default:
				return code.startsWith("http_") || code.contains(" ")
					? "Não foi possível acessar o site"
					: "Erro: " + code;
		}
	}

	/** "[Brazil Chat] Novo anúncio: Bingo de dezembro! - Inscrições abertas até sexta." */
	public static String newAnnouncement(String title, String body)
	{
		String text = CHAT_PREFIX + "Novo anúncio: " + title;
		if (body != null && !body.trim().isEmpty())
		{
			String oneLine = body.trim().replaceAll("\\s+", " ");
			text += " - " + (oneLine.length() > 160 ? oneLine.substring(0, 157) + "..." : oneLine);
		}
		return text;
	}

	public static String readyForRank(String rank)
	{
		return CHAT_PREFIX + "Você já cumpre os requisitos para " + rank
			+ "! Peça sua promoção à staff no Discord.";
	}

	public static String reminderSoon(String event, String in)
	{
		return CHAT_PREFIX + "Lembrete: " + event + " começa em " + in + "!";
	}

	public static String reminderToday(String event, String in)
	{
		return CHAT_PREFIX + "Você está inscrito em " + event + ", que começa em " + in + ".";
	}

	/** 12500000 -> "12.5M", 250000 -> "250K". */
	public static String gp(long value)
	{
		if (value >= 1_000_000_000L)
		{
			return trim(value / 1_000_000_000d) + "B";
		}
		if (value >= 1_000_000L)
		{
			return trim(value / 1_000_000d) + "M";
		}
		if (value >= 1_000L)
		{
			return trim(value / 1_000d) + "K";
		}
		return Long.toString(value);
	}

	private static String trim(double d)
	{
		String s = String.format(java.util.Locale.ROOT, "%.1f", d);
		return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
	}

	public static String clockSkew(long seconds)
	{
		return "O relógio do seu PC está " + seconds + "s fora do horário";
	}

	public static String recorded(String item, String rsn, int points)
	{
		return CHAT_PREFIX + "Registrado: " + item + " para " + rsn
			+ (points > 0 ? " (" + points + POINTS_SUFFIX + ")" : "");
	}

	public static String rankUp(String title)
	{
		return CHAT_PREFIX + "Parabéns! Seu novo cargo no clã é " + title + "!";
	}

	public static String onlineCount(int online)
	{
		return online == 1 ? "1 membro online" : online + " membros online";
	}

	public static String world(int world)
	{
		return world > 0 ? "Mundo " + world : "";
	}

	/** "2d 4h", "3h 12m", "4m 9s" -- compact enough for a 225px panel. */
	public static String duration(Duration d)
	{
		long seconds = Math.max(0L, d.getSeconds());
		long days = seconds / 86_400L;
		long hours = (seconds % 86_400L) / 3_600L;
		long minutes = (seconds % 3_600L) / 60L;

		if (days > 0)
		{
			return days + "d " + hours + "h";
		}
		if (hours > 0)
		{
			return hours + "h " + minutes + "m";
		}
		return minutes + "m " + (seconds % 60L) + "s";
	}

	/** "2 anos e 3 meses", "5 meses", "12 dias". */
	public static String timeInClan(long days)
	{
		if (days < 31)
		{
			return days == 1 ? "1 dia" : days + " dias";
		}
		long months = days / 30;
		long years = months / 12;
		months = months % 12;
		if (years == 0)
		{
			return months == 1 ? "1 mês" : months + " meses";
		}
		String y = years == 1 ? "1 ano" : years + " anos";
		if (months == 0)
		{
			return y;
		}
		return y + " e " + (months == 1 ? "1 mês" : months + " meses");
	}
}
