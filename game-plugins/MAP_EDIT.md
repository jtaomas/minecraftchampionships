# Map Edit mode (`/mcm mapedit`)

Ops open a game's map exactly as the game builds it, in creative with flight and no game logic, edit it, and on
leaving / disconnecting the difference from the original is saved as an **overlay file** that the game applies on top
of its own blocks every time it builds that map.

## Commands (ops only; console: list, status, reset, backups, restore)

| Command | What it does |
|---|---|
| `/mcm mapedit` | GUI: games, then maps (also the Admin Tools feather -> Map Edit) |
| `/mcm mapedit list [game]` | clickable chat list; `*` = map has saved edits |
| `/mcm mapedit <game> <map> [place]` | enter a map (optionally at a place, see `tp`) |
| `/mcm mapedit save` | save, keep editing (green emerald item, also autosave every `mapedit.autosave-minutes`, default 10) |
| `/mcm mapedit leave` | save and leave (red barrier item); disconnecting, leaving the world, a game starting and server stop do the same |
| `/mcm mapedit discard` | leave without saving |
| `/mcm mapedit tp <place>` / `anchors` | jump around a big map: Parkour Warrior `start`, `fork`, `hub1..5`, `view1..`, any obstacle id (`M2-1`, `B3-2`, `E-HARD-1`); TGTTOS/Race `start`, `goal`, `overview1..`; Parkour Tag `overview`, `cage1`, `cage2` |
| `/mcm mapedit reset <game> <map>` | drop the overlay (backup kept) |
| `/mcm mapedit backups <game> <map>` / `restore <game> <map> [n]` | list / restore backups (25 kept per map) |
| `/mcm mapedit status` | open sessions |

## How it works

* **Core** (`com.mcm.mapedit.MapEditManager`, `RegionScan`): one editor per map, any number of maps at once; only while
  no game is running (a starting game saves and evicts every editor). The player's inventory/mode/location is restored
  on leave and persisted in `plugins/MinecraftMondayCore/mapedit-state/<uuid>.yml` so a crash cannot lose it.
  The edit area's chunks are ticketed; a palette-compressed **baseline snapshot** is taken right after the game built the
  original map, the existing overlay is applied on top for editing, and on save every cell of the edit area is compared with
  the baseline (BlockData equality, so stairs/doors/fences states are exact). Snapshots are read a few chunks per tick, the
  comparison runs on a worker thread. Damage, hunger and void death are off for editors.
* **Overlay** (`com.mcm.api.McmMapOverlay`): `plugins/<GamePlugin>/mapedits/<map>.overlay.gz`, gzip, magic + format version 1,
  game/map/editor/time, state palette, entries delta/varint coded in **local coordinates** (world - the origin the game pastes at),
  so it survives plugin updates, map regeneration, slot changes and arena copies. It is always the full difference from the
  original (never stacked). Written atomically, previous file copied to `mapedits/backups/<map>.<timestamp>.overlay.gz`.
  Hard cap `mapedit.max-overlay-blocks` (default 750000; the save is refused and the op keeps editing), edit area cap
  `mapedit.max-region-chunks` (4096). Every save logs who / what / how many blocks. Block states only: no tile-entity data (sign
  text, chest contents), no entities.
* **Warnings on save** (chat + log): a game marker (spawn, chicken spawn, hub respawn, sign...) is blocked or has no floor,
  plus game-specific `editWarnings`. Nothing is blocked; the save still happens.
* **API** (`com.mcm.api`): `McmMapEditable` (maps, `buildForEdit`, `overlayChanged`, `editClosed`, `editWarnings`),
  `McmEditSpace` (world, origin, tracked box, spawn, anchors, markers, notes), `McmMapOverlay` (read/apply). A game registers
  with `McmApi.registerMapEditable(plugin, gameId, impl)` (or its `McmGame` implements `McmMapEditable`). Put the
  implementation in its own class and wrap the registration in `try/catch (Throwable)` so the game jar still loads on an older core.
  Nothing existing changed (coin API etc. untouched).

## Per game

| Game | Edit region | Overlay applied |
|---|---|---|
| TGTTOS (`tgttos`) | fresh slot 1000+n in `mcc_tgttos`, first option of each variant group | layered over `TgMapData` (`Overlays.Layered`), so the paster's stale clearing, round-end restores and finish-row checks all see the edited map; imported maps included |
| Race (`race`) | same code as TGTTOS, `mcc_race` | same |
| Parkour Tag (`parkourtag`) | fresh x offset 100000+ in `mcc_parkourtag` | appended to the placements of every arena copy; clear boxes include the overlay |
| Parkour Warrior (`parkourwarrior`) | generated course (same seed/settings, no overlay) at x offset +100000*n; maps = `classic` + `maps/<id>/course.txt` | queued after the course and timed blocks in `CourseWorld.place`; course is rebuilt after a save of the built map |
| Hole in the Wall (`hitw`) | the platform, in place at the world origin (it is the only arena) | applied after `Arena.build()` |
