package com.bingobrazilchat.catalog;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;

/**
 * Loads the shipped {@code aliases.json} fallback.
 *
 * <p>The {@link Gson} instance is passed in rather than constructed. The Plugin Hub packager fails
 * the build outright if a plugin builds its own Gson under {@code src/main} -- RuneLite provides an
 * injectable one, and that is the only instance a plugin should use.
 */
public final class AliasResource
{
	private static final String RESOURCE = "/com/bingobrazilchat/aliases.json";

	private AliasResource()
	{
	}

	public static Loaded load(Gson gson) throws IOException
	{
		try (InputStream in = AliasResource.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				throw new IOException("Missing bundled resource " + RESOURCE);
			}
			Type type = new TypeToken<RawFile>()
			{
			}.getType();
			RawFile raw = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), type);
			if (raw == null)
			{
				throw new IOException("Unparseable " + RESOURCE);
			}

			Map<String, DropCatalog.AliasTarget> aliases = new HashMap<>();
			if (raw.aliases != null)
			{
				for (Map.Entry<String, RawTarget> e : raw.aliases.entrySet())
				{
					RawTarget t = e.getValue();
					if (t != null && t.name != null && t.raid != null)
					{
						aliases.put(e.getKey(), new DropCatalog.AliasTarget(t.name, t.raid));
					}
				}
			}

			List<String> ignored = raw.ignored == null ? new ArrayList<>() : raw.ignored;
			return new Loaded(Collections.unmodifiableMap(aliases),
				Collections.unmodifiableList(ignored));
		}
	}

	@Value
	public static class Loaded
	{
		Map<String, DropCatalog.AliasTarget> aliases;
		List<String> ignored;
	}

	private static final class RawFile
	{
		Map<String, RawTarget> aliases;
		List<String> ignored;
	}

	private static final class RawTarget
	{
		String name;
		String raid;
	}
}
