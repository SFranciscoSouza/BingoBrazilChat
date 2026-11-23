package com.brazilchat.clan.api;

import com.brazilchat.BrazilChatConfig;
import com.brazilchat.bingo.api.ApiCallback;
import com.brazilchat.bingo.api.ApiException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * HTTP for the clan features (calendar, sign-ups, staff tools). Separate from the bingo client
 * because the auth is different: no shared token, the caller is identified by account hash + RSN,
 * and staff calls add the personal staff key.
 *
 * <p>Same RuneLite rules as the bingo client: always {@code enqueue}, never block, and the only host
 * is the fixed site address.
 */
@Slf4j
@Singleton
public class ClanApiClient
{
	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	private static final String BASE_URL = "https://brazilchat.vercel.app";
	private static final String CLIENT_VERSION = "2.0.0";

	/** Who is asking. Read on the client thread, then passed along. */
	@Value
	public static class Identity
	{
		long accountHash;
		String rsn;
	}

	private final OkHttpClient http;
	private final Gson gson;
	private final BrazilChatConfig config;

	@Inject
	public ClanApiClient(OkHttpClient okHttpClient, Gson gson, BrazilChatConfig config)
	{
		this.http = okHttpClient.newBuilder()
			.callTimeout(30, TimeUnit.SECONDS)
			.connectTimeout(10, TimeUnit.SECONDS)
			.readTimeout(20, TimeUnit.SECONDS)
			.build();
		this.gson = gson;
		this.config = config;
	}

	public boolean hasStaffKey()
	{
		return !staffKey().isEmpty();
	}

	// ----------------------------------------------------------------------------------- member

	public void bootstrap(Identity id, ApiCallback<ClanDtos.Bootstrap> cb)
	{
		send(member(id, url("clan", "bootstrap")).get().build(), ClanDtos.Bootstrap.class, cb);
	}

	public void signUp(Identity id, String eventId, ApiCallback<ClanDtos.OkResponse> cb)
	{
		send(member(id, url("clan", "events", eventId, "signup")).post(json(Collections.emptyMap())).build(),
			ClanDtos.OkResponse.class, cb);
	}

	public void cancelSignUp(Identity id, String eventId, ApiCallback<ClanDtos.OkResponse> cb)
	{
		send(member(id, url("clan", "events", eventId, "signup")).delete().build(),
			ClanDtos.OkResponse.class, cb);
	}

	public void reportAchievements(Identity id, ClanDtos.Achievements achievements,
		ApiCallback<ClanDtos.OkResponse> cb)
	{
		send(member(id, url("clan", "achievements")).post(json(achievements)).build(),
			ClanDtos.OkResponse.class, cb);
	}

	/**
	 * The clan's announcements. Sends no name and no account hash: it is called for any confirmed
	 * clan member, even with the clan connection off.
	 */
	public void announcements(ApiCallback<ClanDtos.Announcements> cb)
	{
		Request request = new Request.Builder().url(url("clan", "announcements"))
			.header("Accept", "application/json")
			.header("X-Plugin-Version", CLIENT_VERSION)
			.header("User-Agent", "BrazilChat/" + CLIENT_VERSION)
			.get().build();
		send(request, ClanDtos.Announcements.class, cb);
	}

	// ------------------------------------------------------------------------------------ staff

	public void staffCreateAnnouncement(Identity id, ClanDtos.AnnouncementInput input, ApiCallback<ClanDtos.OkResponse> cb)
	{
		send(staff(id, url("staff", "announcements")).post(json(input)).build(), ClanDtos.OkResponse.class, cb);
	}

	public void staffUpdateAnnouncement(Identity id, String announcementId, ClanDtos.AnnouncementInput input,
		ApiCallback<ClanDtos.OkResponse> cb)
	{
		send(staff(id, url("staff", "announcements", announcementId)).post(json(input)).build(),
			ClanDtos.OkResponse.class, cb);
	}

	public void staffDeleteAnnouncement(Identity id, String announcementId, ApiCallback<ClanDtos.OkResponse> cb)
	{
		send(staff(id, url("staff", "announcements", announcementId)).delete().build(), ClanDtos.OkResponse.class, cb);
	}

	public void staffEvents(Identity id, ApiCallback<ClanDtos.StaffEvents> cb)
	{
		send(staff(id, url("staff", "events")).get().build(), ClanDtos.StaffEvents.class, cb);
	}

	public void staffOverview(Identity id, ApiCallback<ClanDtos.Overview> cb)
	{
		send(staff(id, url("staff", "overview")).get().build(), ClanDtos.Overview.class, cb);
	}

	/** action: mark_paid | mark_unpaid | remove | restore */
	public void staffSignupAction(Identity id, String signupId, String action, @Nullable String reason,
		ApiCallback<ClanDtos.OkResponse> cb)
	{
		Map<String, String> body = reason == null
			? Collections.singletonMap("action", action)
			: Map.of("action", action, "reason", reason);
		send(staff(id, url("staff", "signups", signupId)).post(json(body)).build(),
			ClanDtos.OkResponse.class, cb);
	}

	public void staffSetSignupsOpen(Identity id, String eventId, boolean open,
		ApiCallback<ClanDtos.OkResponse> cb)
	{
		send(staff(id, url("staff", "events", eventId)).post(json(Collections.singletonMap("signupsOpen", open))).build(),
			ClanDtos.OkResponse.class, cb);
	}

	public void staffRoster(Identity id, ClanDtos.RosterUpload upload, ApiCallback<ClanDtos.OkResponse> cb)
	{
		send(staff(id, url("staff", "roster")).post(json(upload)).build(), ClanDtos.OkResponse.class, cb);
	}

	// ---------------------------------------------------------------------------------- plumbing

	private Request.Builder member(Identity id, HttpUrl url)
	{
		return new Request.Builder().url(url)
			.header("Accept", "application/json")
			.header("X-Plugin-Version", CLIENT_VERSION)
			.header("User-Agent", "BrazilChat/" + CLIENT_VERSION)
			.header("X-Account-Hash", Long.toString(id.getAccountHash()))
			.header("X-RSN", id.getRsn());
	}

	private Request.Builder staff(Identity id, HttpUrl url)
	{
		return member(id, url).header("X-Staff-Key", staffKey());
	}

	private RequestBody json(Object body)
	{
		return RequestBody.create(JSON, gson.toJson(body));
	}

	private static HttpUrl url(String... segments)
	{
		HttpUrl.Builder builder = HttpUrl.get(BASE_URL).newBuilder()
			.addPathSegment("api").addPathSegment("plugin");
		for (String s : segments)
		{
			builder.addPathSegment(s);
		}
		return builder.build();
	}

	private String staffKey()
	{
		String raw = config.staffKey();
		return raw == null ? "" : raw.trim();
	}

	private <T extends ClanDtos.OkResponse> void send(Request request, Class<T> type, ApiCallback<T> cb)
	{
		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				cb.onFailure(ApiException.transport(e.getMessage() == null ? "network error" : e.getMessage()));
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					T parsed = null;
					if (body != null)
					{
						try
						{
							parsed = gson.fromJson(body.charStream(), type);
						}
						catch (JsonSyntaxException e)
						{
							log.debug("Brazil Chat: unparseable clan response", e);
						}
					}
					if (response.isSuccessful() && parsed != null)
					{
						cb.onSuccess(parsed);
					}
					else
					{
						// The server's error code (e.g. "event_full") is the message, so the UI can
						// translate it.
						String code = parsed != null && parsed.error != null ? parsed.error : "http_" + response.code();
						cb.onFailure(new ApiException(code, response.code(), response.code() >= 500, 0L));
					}
				}
			}
		});
	}
}
