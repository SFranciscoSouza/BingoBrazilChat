# Bingo BrazilChat

A RuneLite plugin that records listed raid drops on a BrazilChat event scoreboard automatically.

When someone on your event roster gets a tracked raid unique, OSRS broadcasts it to the clan
channel. This plugin reads that broadcast, screenshots the moment, and posts it to your event site,
where it scores on the team leaderboard straight away — no screenshots to upload by hand, no forms
to fill in, nothing to forget.

## Networking and privacy — read this first

Unlike most plugins, **this one sends data off your machine.** When it recognises a tracked drop it
sends, to the BrazilChat site your event organiser runs:

- the name of the player who got the drop
- the item name and the raw clan broadcast text
- when it happened
- a screenshot of your client at that moment (unless you turn screenshots off)

**Nothing is sent until you enter your event token.** With the token field empty the plugin makes no
network requests at all — that empty default is the consent gate. It only ever talks to
`https://brazilchat.vercel.app`, which is fixed in the source rather than configurable, and it only
reports players who are on the event roster.

The screenshot exists because a manual submission needs one too; it is the proof an organiser looks
at. If you would rather not send images, turn off **Attach screenshots** — drops are still recorded,
just without a picture.

## Setup

1. Ask your event organiser for the **event token**.
2. Open **RuneLite → Configuration → Bingo BrazilChat** and paste it into **Event token**.
3. Open the side panel (the Bingo BrazilChat icon on the right) to check it says *Connected*.

That is all. There is nothing to configure per event — the item list, the point values, the roster
and the event start and end times all come from the site.

## Settings

| Setting | Default | What it does |
| --- | --- | --- |
| **Event token** | *(empty)* | Your event token. The plugin is inert until this is set. |
| **Attach screenshots** | On | Send a screenshot as proof alongside each drop. |
| **Screenshot size** | 60% | Scale applied before upload. Lower means a smaller upload. |
| **Report other players' drops** | On | Also report roster members' drops, so a drop still counts when the person who got it isn't running the plugin. |
| **Confirm in chat** | On | Print a local message when a drop has been recorded. |
| **Log unrecognised broadcasts** | Off | Write drop-like broadcasts the plugin couldn't read to the client log. Useful for reporting a missed item. |

## The side panel

Shows whether you are connected, a countdown to the event start or end, the live team scoreboard,
and every drop this client has sent — including anything still queued because the site was
unreachable.

## How duplicates are handled

Everyone in the clan sees the same broadcast, so everyone running the plugin reports the same drop.
That is deliberate: it means a drop is still recorded when the person who actually got it isn't
running the plugin. The site collapses those reports into one scored drop, so a drop is never
counted twice no matter how many people report it.

To keep that cheap, the person who got the drop reports immediately and everyone else waits about
twenty seconds first — so the screenshot that ends up attached is usually the looter's own.

## Offline behaviour

If the site is unreachable, drops are queued to disk and retried with backoff. The queue survives
closing RuneLite, so a drop is not lost because the site was down or your connection dropped. The
panel shows anything still waiting.

## Building locally

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-11.0.31.11-hotspot'
.\gradlew build   # compile + unit tests
.\gradlew run     # boots a RuneLite client with the plugin loaded
```

Targets Java 11. Pulls the latest released RuneLite client (`net.runelite:client:latest.release`).

The server side lives in the companion `BrazilChat` repository, which provides the `/api/plugin/*`
endpoints this plugin talks to.

## License

[BSD 2-Clause](LICENSE) © SFranciscoSouza
