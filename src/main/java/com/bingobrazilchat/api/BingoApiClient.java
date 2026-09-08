package com.bingobrazilchat.api;

import com.bingobrazilchat.BingoBrazilChatConfig;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Every HTTP call this plugin makes.
 *
 * <p>Two RuneLite-specific constraints shape this class:
 *
 * <ul>
 *   <li>The injected {@link OkHttpClient} carries an interceptor that <em>throws</em> on a blocking
 *       call made from the client thread or the EDT. So every call here uses {@code enqueue}, never
 *       {@code execute} -- with enqueue the interceptor runs on a dispatcher thread and passes.</li>
 *   <li>The shared instance must not be mutated, so per-call timeouts go through
 *       {@code newBuilder()} once, at construction.</li>
 * </ul>
 *
 * <p>The plugin is inert until a token is configured: {@link #isConfigured()} gates every call, so
 * an unconfigured install genuinely never touches the network.
 */
@Slf4j
@Singleton
public class BingoApiClient
{
	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	private static final MediaType PNG = MediaType.get("image/png");
	private static final String CLIENT_VERSION = "1.0.0";

	/**
	 * The only host this plugin ever contacts.
	 *
	 * <p>Deliberately a constant rather than a config field: a user-editable address would let a
	 * player be talked into pointing their RSN and screenshots at somewhere else, and it makes the
	 * plugin's network behaviour impossible to state precisely. To test against a local server,
	 * change this line and rebuild.
	 */
	private static final String BASE_URL = "https://brazilchat.vercel.app";

	private final OkHttpClient http;
	private final Gson gson;
	private final BingoBrazilChatConfig config;

	@Inject
	public BingoApiClient(OkHttpClient okHttpClient, Gson gson, BingoBrazilChatConfig config)
	{
		this.http = okHttpClient.newBuilder()
			.callTimeout(30, TimeUnit.SECONDS)
			.connectTimeout(10, TimeUnit.SECONDS)
			.readTimeout(20, TimeUnit.SECONDS)
			.build();
		this.gson = gson;
		this.config = config;
	}

	public boolean isConfigured()
	{
		return !token().isEmpty() && baseUrl() != null;
	}

	/**
	 * Fetches the event window, catalog, aliases and roster.
	 *
	 * @param sentAt when the request left, so the caller can measure clock skew against the
	 *               {@code serverTime} in the reply
	 */
	public void fetchConfig(Instant sentAt, ApiCallback<ApiDtos.ConfigResponse> callback)
	{
		HttpUrl url = path("config");
		if (url == null)
		{
			callback.onFailure(ApiException.transport("Invalid site address"));
			return;
		}

		enqueue(authed(new Request.Builder().url(url).get()).build(),
			ApiDtos.ConfigResponse.class, callback);
		log.debug("Bingo BrazilChat: config requested at {}", sentAt);
	}

	/** Phase 1 of a submission: tell the server the drop happened. No image is sent here. */
	public void claimDrop(ApiDtos.ClaimRequest claim, ApiCallback<ApiDtos.ClaimResponse> callback)
	{
		HttpUrl url = path("drops");
		if (url == null)
		{
			callback.onFailure(ApiException.transport("Invalid site address"));
			return;
		}

		claim.clientVersion = CLIENT_VERSION;
		RequestBody body = RequestBody.create(JSON, gson.toJson(claim));

		Request request = authed(new Request.Builder().url(url).post(body))
			// Lets a proxy or a future server-side cache collapse a retry without our help.
			.header("Idempotency-Key", claim.dedupeKey)
			.build();

		enqueue(request, ApiDtos.ClaimResponse.class, callback);
	}

	/**
	 * Phase 2: attach the screenshot. Only the client whose claim returned {@code created} holds a
	 * valid proof token, so only it gets here -- which is the whole reason the screenshot is a
	 * separate request rather than part of the claim.
	 */
	public void uploadProof(String submissionId, String proofToken, byte[] png,
		ApiCallback<Void> callback)
	{
		HttpUrl url = path("drops", submissionId, "proof");
		if (url == null)
		{
			callback.onFailure(ApiException.transport("Invalid site address"));
			return;
		}

		RequestBody form = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("file", submissionId + ".png", RequestBody.create(PNG, png))
			.build();

		Request request = authed(new Request.Builder().url(url).post(form))
			.header("X-Proof-Token", proofToken)
			.build();

		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				callback.onFailure(ApiException.transport(e.getMessage()));
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response r = response)
				{
					// 409 means the proof is already attached. That is the outcome we wanted, so
					// treat it as success rather than retrying forever against a settled row.
					if (r.isSuccessful() || r.code() == 409)
					{
						callback.onSuccess(null);
						return;
					}
					callback.onFailure(ApiException.fromStatus(r.code(), bodySnippet(r.body()),
						retryAfter(r)));
				}
			}
		});
	}

	/** @param etag the last ETag we saw, so an unchanged board costs a 304 instead of a payload */
	public void fetchLeaderboard(@Nullable String eventId, @Nullable String etag,
		LeaderboardCallback callback)
	{
		HttpUrl base = path("leaderboard");
		if (base == null)
		{
			callback.onFailure(ApiException.transport("Invalid site address"));
			return;
		}

		HttpUrl url = eventId == null ? base
			: base.newBuilder().addQueryParameter("eventId", eventId).build();

		Request.Builder builder = authed(new Request.Builder().url(url).get());
		if (etag != null && !etag.isEmpty())
		{
			builder.header("If-None-Match", etag);
		}

		http.newCall(builder.build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				callback.onFailure(ApiException.transport(e.getMessage()));
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response r = response)
				{
					if (r.code() == 304)
					{
						callback.onUnchanged();
						return;
					}
					if (!r.isSuccessful())
					{
						callback.onFailure(ApiException.fromStatus(r.code(),
							bodySnippet(r.body()), retryAfter(r)));
						return;
					}
					ApiDtos.LeaderboardResponse parsed =
						parse(r.body(), ApiDtos.LeaderboardResponse.class);
					if (parsed == null)
					{
						callback.onFailure(ApiException.transport("Unreadable leaderboard"));
						return;
					}
					callback.onSuccess(parsed, r.header("ETag"));
				}
			}
		});
	}

	public interface LeaderboardCallback
	{
		void onSuccess(ApiDtos.LeaderboardResponse result, @Nullable String etag);

		/** The board has not changed since the ETag we sent. */
		void onUnchanged();

		void onFailure(ApiException error);
	}

	// ------------------------------------------------------------------------------------------

	private <T> void enqueue(Request request, Class<T> type, ApiCallback<T> callback)
	{
		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				callback.onFailure(ApiException.transport(e.getMessage()));
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response r = response)
				{
					if (!r.isSuccessful())
					{
						callback.onFailure(ApiException.fromStatus(r.code(),
							bodySnippet(r.body()), retryAfter(r)));
						return;
					}
					T parsed = parse(r.body(), type);
					if (parsed == null)
					{
						callback.onFailure(ApiException.transport("Unreadable response"));
						return;
					}
					callback.onSuccess(parsed);
				}
			}
		});
	}

	@Nullable
	private <T> T parse(@Nullable ResponseBody body, Class<T> type)
	{
		if (body == null)
		{
			return null;
		}
		try
		{
			return gson.fromJson(body.charStream(), type);
		}
		catch (JsonSyntaxException e)
		{
			log.debug("Bingo BrazilChat: unparseable response", e);
			return null;
		}
	}

	private Request.Builder authed(Request.Builder builder)
	{
		return builder
			.header("Authorization", "Bearer " + token())
			.header("Accept", "application/json")
			.header("X-Plugin-Version", CLIENT_VERSION)
			.header("User-Agent", "BingoBrazilChat/" + CLIENT_VERSION);
	}

	@Nullable
	private HttpUrl path(String... segments)
	{
		HttpUrl base = baseUrl();
		if (base == null)
		{
			return null;
		}
		HttpUrl.Builder builder = base.newBuilder().addPathSegment("api").addPathSegment("plugin");
		for (String segment : segments)
		{
			builder.addPathSegment(segment);
		}
		return builder.build();
	}

	@Nullable
	private HttpUrl baseUrl()
	{
		return HttpUrl.parse(BASE_URL);
	}

	private String token()
	{
		String raw = config.sharedToken();
		return raw == null ? "" : raw.trim();
	}

	private static long retryAfter(Response response)
	{
		String header = response.header("Retry-After");
		if (header == null)
		{
			return 0L;
		}
		try
		{
			return Math.max(0L, Long.parseLong(header.trim()));
		}
		catch (NumberFormatException e)
		{
			return 0L;
		}
	}

	/** Body text for the log, capped so a stray HTML error page cannot flood the client log. */
	private static String bodySnippet(@Nullable ResponseBody body)
	{
		if (body == null)
		{
			return "";
		}
		try
		{
			String text = body.string();
			return text.length() > 300 ? text.substring(0, 300) : text;
		}
		catch (IOException e)
		{
			return "";
		}
	}
}
