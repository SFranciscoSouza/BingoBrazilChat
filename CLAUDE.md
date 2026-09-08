# Bingo BrazilChat — working instructions

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

Unlike the other plugins in this account, this one **does** make network requests: it sends a
player RSN, an item name, and a screenshot to the BrazilChat event site. That must stay disclosed
in README.md and in the Plugin Hub description. The plugin is inert until the user pastes a shared
token into config — that empty default is the consent gate.

## Build

JDK 11 (Temurin). Set `JAVA_HOME` before invoking Gradle:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-11.0.31.11-hotspot'
.\gradlew build
```

Version lives in `runelite-plugin.properties` (the Hub reads this) and must stay in sync with
`build.gradle`.
