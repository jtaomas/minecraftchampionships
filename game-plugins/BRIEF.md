# Shared brief for MCC game plugins

Each game is a standalone Paper plugin that plugs into the server's tournament plugin, **MinecraftMondayCore**, through its game API. The tournament runs the event and handles teams and points. Your plugin just plays one game and reports points.

## Environment
- Server: Paper **1.21.1**, Java 21. The server folder is `C:\Users\USER\Desktop\Mcc`. **Don't edit anything there except your own `game-plugins\<YourGame>\` folder.** Don't start the server; a separate test pass does that.
- JDK 21: `JAVA_HOME=C:\Users\USER\Desktop\java`. Maven: `C:\Users\USER\Desktop\java-tools\maven\bin\mvn.cmd`. Set JAVA_HOME explicitly in each command.
- The core API is installed in the local Maven repo as `com.mcm:MinecraftMondayCore:3.1.0`. Its source is `C:\Users\USER\Desktop\Mcc\plugin-src\com\mcm\api\*.java`. **Read all four files first**: `McmGame`, `McmGameContext`, `McmApi`, `McmVoteProvider`.

## Project layout (Maven)
- `game-plugins\<YourGame>\pom.xml`
  - `io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT`, scope provided, from repo `https://repo.papermc.io/repository/maven-public/`
  - `com.mcm:MinecraftMondayCore:3.1.0`, scope provided
  - `maven.compiler.release` 21, UTF-8
  - `finalName` = your plugin name
- `src/main/java/com/mcm/games/<yourgame>/...`
- `src/main/resources/plugin.yml`
  - `api-version: '1.21'`
  - `depend: [MinecraftMondayCore]`
  - main class in onEnable: `McmApi.registerGame(this, new YourGame(this))`
  - onDisable: stop if running
- Build: `mvn -B -q package`, then copy the jar to `C:\Users\USER\Desktop\Mcc\game-plugins\_built\`. Create that folder if needed.
- No shaded dependencies unless really needed. Use only Paper API.

## How a game must behave (API contract)
- **`start(ctx)`**, called on the main thread. Teleport `ctx.participants()` into your arena, run the game, and call `ctx.finish(reason)` **exactly once** at the end.
  - When finish is called, the core returns everyone to the lobby and prints standings. Don't teleport players to the lobby yourself.
- **`stop()`** is an admin skip, stop or shutdown. Cancel every task, restore or clear the arena, and remove your entities/items. **Don't call finish.** It must be safe to call at any time, including twice.
- **Teams:** use `ctx.sides()`. Keys are `TEAM:<name>` for tournament teams (Red, Orange, Yellow, Lime, Green, Cyan, Aqua, Blue, Purple, Pink) or `SOLO:<uuid>` for solo players.
  - Use `ctx.teamColor` / `ctx.sideLabel` for display.
  - Support 1 to 40 players and any mix of teams and solos.
  - With 1 player the game must still run, so an admin can test alone.
  - For 2-team games, pair or merge sides sensibly.
- **Points:** use `ctx.addPoints(uuid, pts, reason)` for individual points and `ctx.addSidePoints(sideKey, pts, reason)` for team bonuses (split across members).
  - Rough scale: a whole game should hand out about 50–150 points to a top player.
  - Award as things happen (kills, finishes, placements), like MCC.
- **HUD:** call `ctx.setTimer(secondsLeft, total)` every second, and `ctx.setSidebarLines(List<String>)` for up to 6 short status lines.
  - Keep `useCoreScoreboard()` returning true.
  - Use titles and action bars for moment-to-moment feedback.
- **Rules:** the core turns off ALL its own rules during your game: damage, block break/place, hunger, item drops, pickups, portals. Enforce your own with your own listeners, and only while your game is running and only in your world.
  - Set the game modes yourself. Players arrive in survival with an empty inventory.
- **Late joiners:** the default (spectator) is fine. Override `onLateJoin` only if it's easy.
- **Disconnects:** `participants()` drops anyone who quits. Handle someone leaving mid-game, including the last player on a side.
- **Time limit:** every game must end on its own via a timer, even if nobody completes it.

## Arena / world
- **No downloaded maps.** Generate your arena in code inside your own world named `mcc_<id>` (for example `mcc_buildmart`).
  - Create it with `WorldCreator`, using a void `ChunkGenerator` (return no blocks) and a fixed spawn.
  - Set gamerules for that world: no mob spawning, no daylight or weather cycle, keep inventory if it suits the game, no fall damage if it suits the game.
- Build the arena with block placement at game start, or once at enable. Spread big builds over a few ticks if needed; keep it under about 50k blocks per tick.
- Reset it fully between games.
- Make it look decent: themed colours and materials, borders, a spectator area, team-coloured starting pads, and so on.
- It must be playable without any resource pack or client mod.
- Original builds and code only. Recreate the *mechanics* of the MCC game; don't copy Noxcrew assets, maps, text or music.

## Done means
- `mvn package` succeeds with no errors.
- The jar is in `game-plugins\_built\`.
- You've re-read your code for obvious runtime bugs:
  - NPEs when a world isn't loaded
  - tasks not cancelled in `stop()`
  - listeners acting outside your game or world
  - `finish` called twice
  - ConcurrentModification on participant lists
- Short `README.md` in your folder: rules, how scoring works, the `/mcm play <id>` id, and known limitations.
- **Final report**, kept short:
  - game id and display name
  - how it plays
  - scoring
  - files
  - anything you couldn't finish
  - any assumptions about the API that might be wrong

## Admin testing hooks (owner request; every game implements these)
The owner (op) tests alone and wants no waiting. Contract (core 3.2.x adds these to the API; they are `default` methods, so old jars keep working):
- `McmApi.isFastTest()` — true while the admin's fast-test mode is on (`/mcm fast on|off`). While true, **every wait a game controls is cut to ≤ 1 s**: pre-game/pre-round countdowns, kit-selection timers, intermissions, results screens, celebrations, title cards. `McmFx.countdown` already shortens itself in fast-test mode; games shorten their own timers. Actual gameplay timers (round length) are NOT shortened.
- `McmGame.skipWait()` — admin ran `/mcm skipwait`: end whatever waiting phase is running right now (countdown, kit select, intermission, results) and go straight to the next phase. Return true if something was skipped.
- `McmGame.skipRound()` — admin ran `/mcm skip`: end the current round/map immediately with no points for it and move to the next round/map (if it was the last one, end the game normally via `finish`). Return true if handled; games with a single round return false.
- `McmGame.maps()` → `List<String>` of map/arena ids and `McmGame.forceNextMap(String id)` → boolean — admin ran `/mcm map <id>`: the next round uses that map. Optional; default empty/false.
To stay compilable against an older installed core, games may declare these methods WITHOUT `@Override` and read fast-test mode through a tiny reflective helper (call `com.mcm.api.McmApi.isFastTest()` via reflection, false if absent).
