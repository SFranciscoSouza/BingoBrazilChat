# Brazil Chat

A RuneLite plugin for the **Brazil Chat** Old School RuneScape clan. It shows your clan profile and
who is online, cleans up clan broadcasts, and records listed raid drops on the clan's bingo
scoreboard automatically.

The interface is in Brazilian Portuguese (PT-BR).

## Features

The side panel (the green chat-bubble icon on the right) has up to four tabs.

- **Início (Home)**
  - **Staff announcements:** shown to every confirmed member of the clan (anyone on the in-game member list, not guests), even with the clan connection off. Each new announcement is also printed once in the chatbox, at the member's next login.
  - **Your clan profile:** name, rank, join date and time in the clan.
  - **The clan at a glance:** members online and in total.
  - **The next event:** with a countdown.
  - **Links:** the clan site (brazilchat.com.br), Discord, the bingo site and the clan's Wise Old Man group.
- **Clã (Clan):** everyone online in the clan channel, with the game's own rank icon (gold key Owner, silver key Deputy Owner, bronze key Administrator, and each rank's icon), the clan's name for the rank, and their world. You can search by name.
- **Eventos (Events)**
  - **The clan calendar:** date in Brasília time, entry fee and free slots. You can **sign up or cancel** from here, and see whether your fee is marked as paid.
  - **The clan's Wise Old Man competitions:** the leaders and your own position.
  - **The bingo:** live team scoreboard and the drops this client has sent.
- **Staff:** only for Administrator, Deputy Owner and Owner, and only with an admin key.
  - **Per event:** sign-ups with totals (paid, pending, gp collected). For each person you can mark the fee as paid, remove them (optionally with a reason they will see) or restore them. You can also open or close sign-ups.
  - **Suggested promotions** from the clan's rank table. *Promover* means every requirement is confirmed. *Possível* means nothing known is missing, but diaries or CA are unverified because the member doesn't use the plugin.
  - **MVP suggestions:** the top monthly EHB (MVP PvM) and EHP (MVP Skiller) from Wise Old Man.
  - **Announcements:** create, edit and delete the announcements shown on Início. The same can be done on the site at `/admin/clan`.
  - **Roster status:** when the member list was last updated, recent promotions, and members with 60+ days without XP.

It also adds these, in the chatbox and on screen:

- **Broadcast filter.** Hide level-ups, quests, diaries, Combat Achievements, personal bests, PvP and deaths, people joining or leaving, and drops below a gp value you choose. Raid loot, pets, collection log items and max total are never hidden; they're highlighted in a colour of your choice, with an optional notification.
- **Rank-up notice.** When your clan rank goes up you get a congratulation in your chatbox, visible only to you. The promotion is also posted to the clan wall on the site.
- **Event reminders.** A message when you log in on a day you have an event, and another *N* minutes before it starts.
- **Event overlay.** During an event that has a password, the event name, the password and the Brasília time are shown on screen, so every proof screenshot carries them.

## Networking and privacy — read this first

The profile, online list, broadcast filter and rank-up notice only read what your game client already
shows you, and they stay local. Four optional features send data, and only to
`https://brazilchat.vercel.app`. That address is fixed in the source rather than configurable.

| Feature | Turned on by | What is sent |
| --- | --- | --- |
| Clan announcements | **Mostrar anúncios do clã**, on by default, and only for confirmed members of the clan | Nothing about you: a plain request for the announcement list (your IP address is visible to the site, as with any request). |
| Clan connection (calendar, sign-ups, competitions, rank-up list) | **Conectar ao Brazil Chat**, off by default | Your RSN and your RuneLite account hash; your own sign-ups and cancellations; and, once per login, your own achievements: total level, max cape, the diary tier done in every area, and your Combat Achievement tier. |
| Staff tools | An **admin key** pasted by a staff member | Also the clan member list from the game (name, rank, title, join date), every 30 minutes, plus sign-up management actions. |
| Bingo drop reporting | The **event token** | For a listed drop in a clan broadcast: the player's name, the item, the broadcast text, the time, and a screenshot (optional). |

With the clan connection, the admin key and the event token off (the default), the only request is the announcement list for confirmed clan members; turn off **Mostrar anúncios do clã** and the plugin makes no network requests at all.

**Why the account hash?** It stops someone else from signing up in your name. The site
remembers which game account first used an RSN and refuses that RSN from any other account. A
staff member can release an RSN on the site if it legitimately moves to another account.

**Admin keys:** the Owner creates them on the site. The site stores only a hash of each key,
not the key itself. A key works only from the game account that first used it, and only
while that account's clan rank is Administrator or above.

## Setup

1. Open **RuneLite → Configuration → Brazil Chat**.
2. Turn on **Conectar ao Brazil Chat** to see events, sign-ups and announcements.
3. *(Staff only)* Paste the **Chave de administrador** (admin key) the Owner gave you.
4. *(Bingo only)* Paste the **Token do evento** (event token) from the event organiser.

### Upgrading from "Bingo BrazilChat"

This plugin used to be called **Bingo BrazilChat**. The first time Brazil Chat starts, it copies your event
token, your bingo settings and any drops still queued for upload from the old plugin, so you don't
have to set anything up again.

## Settings

| Section | Setting | Default | What it does |
| --- | --- | --- | --- |
| Clã | Mostrar anúncios do clã | On | Announcements on Início for clan members. Sends no name or account. |
| Clã | Avisar novos anúncios no chat | On | Shows each new announcement once in your chatbox, at your next login (or within 5 minutes if online). |
| Clã | Conectar ao Brazil Chat | Off | Calendar, sign-ups, announcements and competitions. Sends your RSN and account hash. |
| Clã | Lembrete de eventos / Avisar quantos minutos antes | On / 15 | Chat reminders for events you signed up for. |
| Clã | Mostrar senha do evento na tela | On | Event overlay with password and Brasília time. |
| Clã | Chave de administrador | *(empty)* | Staff only: unlocks the Staff tab and the roster upload. |
| Bingo | Token do evento | *(empty)* | The event token. The bingo is inert until this is set. |
| Bingo | Anexar captura de tela / Tamanho da captura | On / 60% | Screenshot proof and how much it is scaled down. |
| Bingo | Reportar drops de outros | On | Also report drops by other people on the roster. |
| Bingo | Confirmar no chat | On | Print a local message when a drop is recorded. |
| Bingo | Registrar anúncios não reconhecidos | Off | Log drop-like broadcasts the plugin couldn't read. |
| Mensagens do clã | Ocultar drops abaixo de (gp) | 0 | Hide drop broadcasts worth less than this. |
| Mensagens do clã | Ocultar … | Off | Hide that category of clan broadcast. |
| Mensagens do clã | Destacar raros / Cor / Notificar raros | On / green / Off | Highlight and notify for rare broadcasts. |
| Avisos | Avisar promoção | On | Tells you when you meet every requirement for the next rank (so you can ask staff), and congratulates you when your rank goes up. |

## How bingo duplicates are handled

Everyone in the clan sees the same broadcast, so everyone running the plugin reports the same drop.
That is deliberate: it means a drop is still recorded when the person who got it isn't running the
plugin. The site merges those reports into one scored drop, so a drop never counts twice.

The person who got the drop reports immediately, and everyone else waits about twenty seconds first.
That way the screenshot that ends up attached is usually the looter's own.

If the site is unreachable, drops are queued to disk and retried with backoff. The queue survives closing
RuneLite.

## Building locally

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-11.0.31.11-hotspot'
.\gradlew build   # compile + unit tests
.\gradlew run     # boots a RuneLite client with the plugin loaded
```

Targets Java 11. The server side lives in the companion `BrazilChat` repository, which provides the
`/api/plugin/*` endpoints the bingo talks to.

## License

[BSD 2-Clause](LICENSE) © SFranciscoSouza
