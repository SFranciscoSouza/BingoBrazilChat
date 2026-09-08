package com.bingobrazilchat;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BingoBrazilChatPluginTest
{
	public static void main(String[] args) throws Exception
	{
		// Isolate the dev client to its own RuneLite home BEFORE any RuneLite class loads
		// (RuneLite.RUNELITE_DIR is derived from user.home at class-init time). Without this,
		// gradlew run would also load any Plugin Hub copy of this plugin from ~/.runelite.
		java.io.File devHome = new java.io.File(System.getProperty("user.dir"), "build/runelite-dev");
		//noinspection ResultOfMethodCallIgnored
		devHome.mkdirs();
		System.setProperty("user.home", devHome.getAbsolutePath());
		System.out.println("[dev] isolated RuneLite home: " + devHome.getAbsolutePath());

		ExternalPluginManager.loadBuiltin(BingoBrazilChatPlugin.class);
		RuneLite.main(args);
	}
}
