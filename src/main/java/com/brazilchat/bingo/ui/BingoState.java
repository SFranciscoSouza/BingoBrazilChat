package com.brazilchat.bingo.ui;

import com.brazilchat.bingo.api.ApiDtos;
import com.brazilchat.bingo.event.EventWindow;
import com.brazilchat.bingo.submit.QueuedSubmission;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * An immutable snapshot handed to the panel.
 *
 * <p>The panel runs on the EDT and everything that produces this runs on the plugin executor, so
 * passing a whole snapshot rather than letting the panel read live state is what keeps the two
 * threads from tripping over each other.
 */
@Value
public class BingoState
{
	public enum Connection
	{
		/** No token entered: the plugin is intentionally doing nothing. */
		NOT_CONFIGURED("Não configurado"),
		/** Token entered, no successful fetch yet. */
		CONNECTING("Conectando"),
		/** Talking to the site. */
		ONLINE("Conectado"),
		/** The token was rejected. */
		UNAUTHORIZED("Token recusado"),
		/** Reachable config, but no event is currently active. */
		NO_EVENT("Sem evento ativo"),
		/** Cannot reach the site. */
		OFFLINE("Sem conexão");

		private final String label;

		Connection(String label)
		{
			this.label = label;
		}

		public String getLabel()
		{
			return label;
		}
	}

	Connection connection;

	@Nullable
	String detail;

	@Nullable
	EventWindow window;

	List<ApiDtos.TeamRow> teams;

	List<QueuedSubmission> queue;

	/** True when the local clock is far enough out that we are correcting for it. */
	boolean clockSkewed;

	public static BingoState initial()
	{
		return new BingoState(Connection.NOT_CONFIGURED, null, null,
			Collections.emptyList(), Collections.emptyList(), false);
	}
}
