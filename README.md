# Minecraft Monday Championship Server (Paper 1.21.1)

Paper 1.21.1 tournament server featuring the custom **MinecraftMondayCore** plugin (`plugin-src/` and pre-built `plugins/MinecraftMondayCore.jar`), supporting plugins (`Multiverse-Core`, `OldCombatMechanics`, `TAB`, `ViaVersion`, `ViaBackwards`, `ViaRewind`), and all tournament/minigame worlds (`world`, `world_nether`, `world_the_end`, `dropper_world`, `oneshot_world`, `parkour_world`, `pvp_world`, `sg_world`, `sg4_world`, `spleef_world`, `survival_world`, `tntrun_world`).

## Prerequisites

- **Java 21** (e.g., Eclipse Temurin 21 from https://adoptium.net/)

## Setup & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/jtaomas/minecraftchampionships.git
   cd minecraftchampionships
   ```
2. Start the server:
   - **Windows:** run `start-mcm-server.bat`
   - **macOS / Linux:**
     ```bash
     java -Xms2G -Xmx6G -jar paper.jar nogui
     ```

> **Note:** Paper automatically downloads and recreates `cache/`, `libraries/`, `versions/`, and `plugins/.paper-remapped/` on first launch.

## In-Game Commands

- `/mcm` (aliases: `/mcc`, `/tournament`) — Open the Minecraft Monday Tournament Controller (ops only). Subcommands (ops/console, tab-completed, `/mcm help` lists them):
  - `/mcm start [mcc]` — start the MM (or MCC) championship. `/mcm play <game>` — play one game.
  - `/mcm stop` — stop and return to the lobby. `/mcm reset` — reset scores. `/mcm games` — list games. `/mcm teamsize <1-5>`.
  - `/mcm lobby [tp|mm|rebuild|status]`, `/mcm preview <x1> <y1> <z1> <x2> <y2> <z2> [name]`, `/mcm selftest`.
- `/team` (alias: `/teams`) — Open the Team Selector GUI.

### Admin testing tools (ops only)

- `/mcm fast [on|off]` — fast-test mode (no arg = toggle; saved as `fast-test` in the config, survives restarts; the state is shown only to you).
  While on, every core wait is cut to at most 1 second: built-in pre-round freeze, `McmFx` countdowns, breaks between rounds,
  intermissions between championship games, the Decision Dome vote (after 1 s the core asks the vote to end now), placement/results
  reveals, title cards, celebrations, podium return. Gameplay timers (round length) are not shortened. MCC games shorten their own waits
  via `McmApi.isFastTest()`.
- `/mcm skipwait` — finish the wait that is running now (vote, intermission, round break, results reveal, pre-round countdown); otherwise
  asks the running game (`McmGame.skipWait()`), then finishes running `McmFx` countdowns. Tells you what was skipped or that nothing was waiting.
- `/mcm skip` — skip only the current **round/map** (no points for it; `McmGame.skipRound()`; built-in TNT Run/Spleef/Battle Arena supported).
  Single-round games say they have no rounds to skip. In the lobby it acts like `/mcm skipwait`.
- `/mcm next` — skip the **whole current game/event** (the old meaning of `/mcm skip`).
- `/mcm map` — list the running game's maps; `/mcm map <id>` — the next round uses that map; `/mcm map <game> [id]` — list/pick before starting
  (`McmGame.maps()` / `forceNextMap()`; built-in Survival Games: `sg4`, `kalas`).
- `/mcm tools` — the **Admin Tools** menu. Ops also get an **Admin Tools feather** (hotbar slot 9 when empty + top-right inventory slot,
  re-added when those slots are empty; right-click it or click it in your inventory). During MCC games only the inventory copy is kept
  (click it there), and during games in `admin-tools-hidden-in-games` (default `bingo`, `skyblockle`: they scan inventories) it is removed. Buttons: Fast test ON/OFF, Skip wait,
  Skip round/map, Maps (pick a game first when none is running), Skip whole game, Start a game, Stop game, MM lobby, MCC lobby,
  Tournament Controller. The feather can't be dropped, moved, placed or duplicated and is removed from non-ops. Config `admin-tools-item: true`.
  In spectator mode (no inventory) use `/mcm tools`.
- The Tournament Controller also has Fast test / Skip wait / Skip round buttons (bottom row).

## Games

- **Minecraft Monday (MM):** the 7 built-ins (Survival Games, Parkour Spiral, Multi-Stage Dropper, TNT Run, Spleef/Splegg, Battle Arena,
  Speed Survival; plus optional One Shot) **and Find the Button** (`findthebutton`). External games listed in the config
  `minecraft-monday-external-games` (default `[findthebutton]`) belong to MM: they appear in the MM compass and Tournament Controller,
  are part of `/mcm start`, use the MM lobby / podium, and are excluded from the MCC hub list, `/mcm start mcc` and Decision Dome votes.
- **MCC:** every other installed MCC game plugin (run from the MCC hub).

## Two lobbies / two event modes (core 3.2.0)

- **Minecraft Monday (MM) lobby** = the default world `world`. Players still join here. Built-in games, `/mcm start` and
  MM championships work exactly as before and return to `world`.
- **MCC hub** = the world `mcc_lobby` (island hub + "Block Showdown" Decision Dome). It is generated once on first enable
  (marker `mcc_lobby/mcm_hub_built.txt`, spread over ticks). Turn it off with `mcc-hub.enabled: false`.
- **Starting an MCC event:** `/mcm championship mcc` (or `/mcm start mcc`). This runs a championship made only of MCC (external) games
  (`mcc-championship-playlist`, empty = all installed MCC games). Votes run inside the dome, and lobby/standings/podium use `mcc_lobby`.
  `/mcm play <mcc game>` also counts as MCC context.
- **Compass:** in `world` it shows the MM games plus an "MCC Lobby" button. In `mcc_lobby` it shows the MCC games plus a button back to the MM lobby (ops also get championship controls).
- **Hub commands:** `/mcm lobby tp` (go to the hub), `/mcm lobby mm` (back to `world`), `/mcm lobby status`, `/mcm lobby rebuild` (op/console).
- **Decision Dome plugin:** `/dome test [hub|dome]`, `/dome tp [hub|dome]`.
