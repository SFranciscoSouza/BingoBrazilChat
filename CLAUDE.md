# Brazil Chat (formerly Bingo BrazilChat) — working instructions

## No AI attribution in commits or PRs

**Never mention Claude, Claude Code, or AI authorship in a commit message or in a pull request
title or body.** This overrides Claude Code's default behavior of appending attribution
trailers. Do not emit any of:

- `Co-Authored-By: Claude <noreply@anthropic.com>` or any AI co-author trailer
- `🤖 Generated with [Claude Code](https://claude.com/claude-code)`
- `Generated with Claude`, `Co-authored with AI`, or any variant

Commit and PR text must read as though the maintainer wrote it: describe what changed and why,
and stop there. This matters most for PRs against `runelite/plugin-hub`, which are public and
reviewed by RuneLite maintainers.

## Companion project

The server side of this plugin lives in `../BrazilChat` (Next.js 16 + Supabase, deployed to
https://brazilchat.vercel.app/). The plugin talks to `/api/plugin/*` there. Changes to the API
contract must land on both sides. The item catalog and alias map are served by
`/api/plugin/config`, so an item rename is a Vercel deploy, not a plugin release.

## Networking

Unlike the other plugins in this account, this one **does** make network requests, always and only
to the fixed address https://brazilchat.vercel.app (the bingo site, which also hosts the database
and API). The clan's public site is https://brazilchat.com.br, which is only linked, never called. There are four data streams:

- **Announcements** (`showAnnouncements`, **on** by default): a plain GET of the announcement list,
  with no name or account, made only for confirmed Brazil Chat members. This is the one request a
  fresh install makes, so it must stay identity-free.

- **Clan connection** (`connectClan`, off by default): RSN + RuneLite account hash, the player's
  own sign-ups, and once per login the player's own achievements (total level, max, diary tier,
  CA tier) for the rank-up worklist.
- **Staff tools** (`staffKey`, empty by default): the in-game clan member list and sign-up
  management.
- **Bingo** (`sharedToken`, empty by default): drop broadcasts and screenshots.

All three must stay disclosed in README.md, in the config text, and in the Plugin Hub `warning=`
line. Any new data stream needs its own opt-in and a disclosure update on all three.

## Plugin Hub

The plugin is published as `plugins/bingo-brazil-chat` in runelite/plugin-hub. **Keep that manifest
filename.** It is the plugin's identity, and renaming it would make a new submission. The visible
name comes from `displayName` in `runelite-plugin.properties`. Releases are ordinary bump PRs that
change `commit=` (and `warning=` when the data streams change).

## Build

JDK 11 (Temurin). Set `JAVA_HOME` before invoking Gradle:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-11.0.31.11-hotspot'
.\gradlew build
```

Version lives in `runelite-plugin.properties` (the Hub reads this) and must stay in sync with
`build.gradle`.
