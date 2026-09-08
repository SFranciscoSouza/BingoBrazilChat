package com.bingobrazilchat.ui;

import com.bingobrazilchat.api.ApiDtos;
import com.bingobrazilchat.event.EventWindow;
import com.bingobrazilchat.submit.QueuedSubmission;
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
public class PanelState
{
	public enum Connection
	{
		/** No token entered: the plugin is intentionally doing nothing. */
		NOT_CONFIGURED("Not set up"),
		/** Token entered, no successful fetch yet. */
		CONNECTING("Connecting"),
		/** Talking to the site. */
		ONLINE("Connected"),
		/** The token was rejected. */
		UNAUTHORIZED("Token rejected"),
		/** Reachable config, but no event is currently active. */
		NO_EVENT("No active event"),
		/** Cannot reach the site. */
		OFFLINE("Offline");

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

	public static PanelState initial()
	{
		return new PanelState(Connection.NOT_CONFIGURED, null, null,
			Collections.emptyList(), Collections.emptyList(), false);
	}
}
