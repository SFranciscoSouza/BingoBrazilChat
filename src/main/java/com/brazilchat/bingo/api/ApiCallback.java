package com.brazilchat.bingo.api;

/**
 * Result of an API call. Both methods are invoked on an OkHttp dispatcher thread, so an
 * implementation that touches queue state must hop back onto the plugin executor first.
 */
public interface ApiCallback<T>
{
	void onSuccess(T result);

	void onFailure(ApiException error);
}
