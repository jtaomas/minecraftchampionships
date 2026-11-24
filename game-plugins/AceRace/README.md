# Ace Race v2 (McmAceRace)

Map-driven Ace Race for MinecraftMondayCore. This is the **Phase 1 foundation** of the rebuild: the map contract, one world with map slots, a tick-budgeted paster, the race runtime/HUD, admin commands and a **Test Grid** course. Sunset Skyway and Kessler Run are the next generators; the old single-track prototype (`AceRaceGame`/`Track`) has been removed.

## Status

- **Phase 1 — done:** map contract, `mcc_acerace` void world, slot paster, start pen, countdown, directional checkpoints, pads, boost rings, falls/respawn, laps/scoring, spectator handling, HUD, `/acerace` commands, Test Grid.
- **Phase 2 — next:** Sunset Skyway generator + palette + validation.
- **Phase 3:** Kessler Run generator (wing rings, riptide, ring pass).
- **Not yet:** live player playtest, elytra/riptide real-client test, large-map async paster (Phase 1 uses `McmBuild.BuildQueue`), stale-slot recovery after a restart for changed maps.

## Commands

- `/mcm play acerace` — run the next map.
- `/mcm map acerace <id>` — force the next map.
- `/acerace list|info <id>|validate <id>|build <id>|tp <id>|reload|stop` — admin tools (`acerace.admin`).
- `/acerace build testgrid` — paste the Test Grid and teleport to its overview.

## Test Grid

A flat rectangular circuit used to prove the runtime:

- 40 start slots, 4 sections (start + 3 checkpoints), 1 lap, 120 s.
- Speed pad, jump pad, one gold boost ring, start barrier, finish gate.
- `/acerace validate testgrid` should print `OK` and `5236` blocks.
- It is deliberately plain; it is not a showcase map.

## Architecture

```
com.mcm.games.acerace
  AceRacePlugin          enable/disable, register game + command
  AceRaceCommand         /acerace admin
  RaceGame               McmGame: lifecycle, state machine, runtime, scoring
  RaceHud                sidebar, timer, action bar, checkpoint/finish feedback
  RaceListener           world-only guards, start freeze, spectator containment
  RaceScoring            progress + placement points
  RaceRegistry           ids, slots, cached map data, rotation
  RaceWorld              mcc_acerace void world, slots, BuildQueue paster
map/
  RaceMap                pure map interface
  RaceMapData            pure data + check()
  RaceGeo                regions, gates, pads, rings, loadouts, sections
  RaceBlocks             sparse block-state store
  RaceBuilder            shape helpers
  maps/TestGridMap       Phase-1 course
```

The owner-approved design documents are `../research/acerace/SPEC.md` and `../research/acerace/SCHEMATICS.md`.

## Build

Java 21 and core `com.mcm:MinecraftMondayCore:3.2.0` installed:

```bash
cd plugin-build && mvn install
cd ../game-plugins/AceRace && mvn package
# copy target/McmAceRace.jar to game-plugins/_built/ and plugins/
```

## Known limitations

- The Test Grid is not the final map; real maps still need generators, palette and offline renders.
- `RaceWorld` uses the core `BuildQueue` (20k blocks/tick). The 120–300k-block maps need the TGTTOS-style async chunk paster before Phase 2 ships.
- A changed map pasted into a slot after a server restart can leave stale blocks because the old-key set is in memory only. Fix before the real maps are iterated.
- Live playtest still required; the boot test only proves plugin load and registration.
