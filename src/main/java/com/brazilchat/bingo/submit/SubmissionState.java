package com.brazilchat.bingo.submit;

/**
 * Where a queued submission has got to. Only {@link #PENDING_CLAIM} and {@link #UPLOADING_PROOF}
 * are retried; everything else is terminal and stays visible in the panel so a player can see what
 * happened rather than watching a drop disappear.
 */
public enum SubmissionState
{
	/** Waiting to tell the server this drop happened. */
	PENDING_CLAIM("Enviando"),

	/** The claim succeeded and this client owns the proof upload. */
	AWAITING_PROOF("Anexando prova"),

	/** Uploading the screenshot. */
	UPLOADING_PROOF("Anexando prova"),

	/** Recorded, with proof attached. */
	DONE("Registrado"),

	/** Someone else's client got there first. Still a success -- the drop is on the board. */
	DUPLICATE("Já registrado"),

	/** The server understood us and declined: outside the window, not on the roster, not scoreable. */
	REJECTED("Não contabilizado"),

	/** Gave up. The reason is kept alongside so the panel can explain it. */
	FAILED("Falhou");

	private final String label;

	SubmissionState(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}

	public boolean isTerminal()
	{
		return this == DONE || this == DUPLICATE || this == REJECTED || this == FAILED;
	}
}
