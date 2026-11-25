# Runtime gap — what the maps provide vs what a 40-player Ace Race must supply

Gap analysis only. No code, no build, no server change. Evidence tags: **H** observed, **M** inference,
**L** judgement.

**The finding in one sentence:** the four Ace Race downloads are **courses plus ad-hoc command stubs**. None
of them is a game. A premium 40-player race needs a server-side runtime that owns every piece of state the
maps currently approximate with world spawn, `@a` effects, one-shot fills and static text.

Read-only context: `game-plugins/AceRace/README.md` and `SPEC.md` already define a Phase-1 runtime and a
40-slot contract. This document does not replace that spec; it records what the *maps* prove is missing and
what the runtime must therefore own.

---

## 1. What each map actually provides

| Capability | Clouds 2 | City Marathon | Space Race | Yeti Set Go |
|---|---|---|---|---|
| Geometry / route | 14 legs, 5 set pieces | 8 legs, city circuit | 17 beats, vertical loop | 6–7 legs, compact lap |
| Start ritual | button → titles → trident | button → titles → fill gate | button → 3-2-1 → fill gate | button → titles → fill gate |
| Pads | jump / mega / slow / speed | speed / jump / launch | levitation / speed | speed / jump |
| Elytra | grants + clears, working | equip exists, unreachable | grant broken | one grant, chunk-dependent |
| Checkpoints | 111 route-ordered records, no enforcement | setworldspawn only | none | setworldspawn only |
| Lap count | none | none | `LapCheck1` unused | none |
| Timer | none | none | stale objectives, no writer | none |
| Finish | none | none | none | none |
| Positions | static "1st" | static "1st" | static "1st" | static "1st" |
| Teams | none | none | none | none |
| Reset | button, global | button, one-shot | fill, one-shot | finish/manual, global |
| Spectator | vanilla | vanilla | vanilla | vanilla |
| Runtime dependency | 6 command banks + sim distance | command blocks at pads | 1 command room | 2 command clusters + sim distance |

Evidence: `../clouds2/README.md` §3–4; `../city_marathon/README.md` §3–5; `../space_race/README.md` §3–4;
`../yeti_set_go/README.md` §2–4. All H for the commands/blocks, M for intent.

**What the maps do not contain at all:** a race clock, a per-player lap counter, an ordered checkpoint
sequence, a finish event, live placement, team rosters, per-player effect scoping, per-player respawn,
results, or a reset that survives a restart. Those are not "missing polish"; they are the game.

---

## 2. The thirteen capabilities a faithful runtime must supply

| # | Capability | What the maps provide (best case) | What the runtime must supply | If missing | Evidence |
|---:|---|---|---|---|---|
| 1 | **Timer** | Nothing in the four Ace Race maps. Space has `Minutes`/`Seconds`/`milliseconds`/`LapCheck1` objectives but no writer; TGTTOS Industry/Pits have a per-player centisecond stopwatch, but for a single-player practice run. | Race clock from GO, per-player stopwatch, lap splits, freeze on finish, DNF time, pause/resume on server lag; authoritative server tick, not command-block repeaters. | No results, no splits, no fair ranking. | `../space_race/README.md` §1/§4 F2; `../tgttos_industry/README.md` §3.5; H |
| 2 | **Laps** | None counts a lap. Space's `LapCheck1` is never written; the other three are geometrically closed loops with no lap state. | Lap count per racer, lap transition (increment, reset section index, apply lap loadout, optional lap spawn), lap points, best-lap tracking, final-lap flag. | A closed loop is indistinguishable from a one-way course; no 2/3-lap format. | `../space_race/README.md` F2; `../yeti_set_go/README.md` §3.5; `game-plugins/AceRace/SPEC.md` §7; H/M |
| 3 | **Ordered checkpoints** | Clouds 2 has 111 route-ordered pad/checkpoint records and a directional route; City and Yeti only move `setworldspawn`; Space has no checkpoint. None enforces order. | Ordered, directional gate volumes (`sections[nextCp].gate`), previous-position segment test, reject reverse/out-of-order crossings, per-section respawn, checkpoint toast, split time. | players skip sections, cut the loop, or get credit for backing through a gate. | `../clouds2/README.md` §3/§4; `../city_marathon/README.md` §3.2; `game-plugins/AceRace/SPEC.md` §7; H |
| 4 | **Finish** | Nothing detects a finish in any of the four. Clouds 2's finish is a visual line; City's gate does nothing; Space's pod has no event; Yeti's gate is crossed southbound but not measured. TGTTOS's chicken punch is a fallback detector, not a race finish. | Directional finish gate (same segment rule as checkpoints), placement lock, finish time, finish title/chat/firework, DNF classification, results order, team points. | The race never ends; the first player to stop running is not the winner. | `../clouds2/README.md` §10; `../city_marathon/README.md` §3.2; `../space_race/README.md` F2; `../yeti_set_go/README.md` §3.6; H |
| 5 | **Positions** | A static `You are in 1st place` actionbar in all four. City/Space/Yeti re-apply it every tick; Clouds 2 has the same static text. No standings, no gaps. | Live progress metric (section + distance-to-next-gate), live place 1..40, signed gaps to the racer ahead/behind, players-remaining count, final placement. | 39 players are lied to; no race narrative. | `../space_race/README.md` F3; `../city_marathon/README.md` §4; `../yeti_set_go/README.md` §4.4; H |
| 6 | **Teams** | None of the four maps has a team. MCC's Ace Race is an individual race inside a 10-team × 4-player roster; the maps only prove colour identity (Clouds 2's 14 scoreboard teams are residue from a missing datapack). | 10 teams × 4 roster, team colours/tab, team coin totals from individual placements, team-level results; **no** team gates, team collision or team-only routes. | No event scoring, no broadcast story, no roster control. | `../clouds2/README.md` §1 (14 teams, no datapack); `../minigames/sky_battle/README.md` §8.1 (team identity is a build quality); H/M |
| 7 | **Per-player effects** | Clouds 2, City and Yeti use `execute as @a at @s if block … run effect give @a …`. Space's levitation/speed pads correctly use `@p`, but its elytra clear and both kill planes use `@a`. | Effect service scoped to the triggering racer (or explicitly to a team/area), with amplifier/duration/cooldown, refresh only while on the pad, clear on respawn/finish/leave, no effects on spectators, and a whitelist of allowed effects. | One player's pad buffs the field (Clouds/City/Yeti), or one player's clear/kill wipes it (Space). | `../clouds2/README.md` §4.5; `../city_marathon/README.md` §4; `../yeti_set_go/README.md` §3.1; `../space_race/README.md` §3/F13; H |
| 8 | **Respawn** | Clouds 2/City/Space/Yeti use `setworldspawn` as a global checkpoint, or have no handler. TGTTOS teleports to the start on y≤−41/−58; the timer keeps running. | Per-player last-checkpoint respawn, velocity/fire/glide reset, 10-tick grace, kit/effect restoration, checkpoint respawn selection, void/fall/hazard detection per section, death counter, DNF policy. | A fall sends everyone to the same place or soft-locks a player at y=−64. | `../clouds2/README.md` §1/§4.7; `../yeti_set_go/README.md` §3.5; `../tgttos_pits/README.md` §2.3; `game-plugins/AceRace/SPEC.md` §7; H/M |
| 9 | **HUD** | Static actionbar only; no timer, lap, checkpoint, position, standings or finish panel. The recreation video also has none — the only persistent text is `You are in 1st place`. | Sidebar (map, lap n/N, players remaining, checkpoint n/N, standings with gaps), action bar (position/split), bossbar for race time or final countdown, finish/results card, team totals. Must be per-player and update at a bounded rate. | Players cannot confirm progress or pace; spectators cannot follow. | `../video_clouds2/README.md` §1/§3.2; `../video_clouds2/data/route_readability.csv` row 13; H |
| 10 | **Start gate** | A button, a title sequencer, a one-shot `/fill` and (in City) an 18-block barrier wall. Space/Yeti never restore the gate; City's saved gate is closed. None supports 40 slots or a re-run. | 40 unique spawn positions with solid ground/headroom, start strip/pen, per-round barrier state, freeze until GO, shared release, false-start handling, countdown, gate reset for the next round, team rows. | Players stack, clip, escape early, or start lap 2 through an open gate. | `../city_marathon/README.md` §3.2; `../space_race/README.md` F4; `../yeti_set_go/README.md` §3.6; `game-plugins/AceRace/SPEC.md` §6/§8; H |
| 11 | **Reset** | Manual reset buttons (Clouds 2), finish-triggered global resets (TGTTOS), or nothing. Space's fill is one-shot; Yeti's gate is saved closed with `SuccessCount=0`. | Idempotent round reset: map blocks/containers/entities, pad cooldowns, scores, respawn points, spectator state, placed blocks, HUD, gate, and a restart-safe "is this slot clean?" check. Must reset one racer or the whole round without corrupting the other. | Round 2 inherits round 1's blocks, scores and spawn; a crash leaves a dirty map. | `../space_race/README.md` F4; `../yeti_set_go/README.md` §3.6; `../tgttos_pits/README.md` §2.5; `game-plugins/AceRace/README.md` known limitations; H |
| 12 | **Spectator handling** | Vanilla spectator + a teleport. Badlands holds finishers for 3 s then teleports all players; Industry/Pits hold 7 s; Sky Battle leaves dead players at the death spot; Parkour Tag respawns on a gallery. No confinement. | Confined spectator box per map, no chunk generation, no interaction, no teleports outside, results view, follow-camera, rejoin policy (finish/DNF as spectator; no mid-race resume), quitter handling. | Spectators fly through the course, load chunks, or leak the route; finishers interfere. | `../tgttos_badlands/README.md` §3; `../minigames/sky_battle/README.md` §8.2 item 14; `game-plugins/AceRace/SPEC.md` §7; H |
| 13 | **Simulation-distance coverage** | Command-block logic only runs while its chunk is loaded. Clouds 2 duplicates the same 45-command bank six times; its ring-flight middle is ~160 blocks from the nearest bank and the village ~111. Yeti's single elytra command is 152 m from the lodge and 177 m from the start. The review server runs simulation-distance 6 / view-distance 8. | No command-block logic. All gates, pads, falls and effects evaluated server-side from region data, independent of chunk tickets; or force-load the whole course. The runtime must also keep the active slot's chunks loaded and spectators out of unloaded chunks. | In Clouds 2 and Yeti, pads, checkpoints, effects and kill planes silently stop mid-course; falls become soft-locks. | `../clouds2/README.md` §4.8; `../yeti_set_go/README.md` §3.3; `../server_import/README.md` §2.1; H/M |

---

## 3. Simulation distance — the architectural consequence

This is the single largest reason not to ship the downloaded command layers.

- **Clouds 2:** six copies of a ~45-command sensor bank exist because a repeating command block only runs in
  a loaded chunk. At sim 6, the village elytra grant (~111 blocks from bank A) and the ring-flight kill plane
  (~160 blocks from B/A) are outside coverage. The map's own sign recommends "simulation distance 4+", which
  is arithmetically insufficient (`../clouds2/README.md` §4.8; H/M).
- **Yeti Set Go:** one elytra command at the station serves three lodestone pads. At sim 6 only the station
  works; the lodge needs ≥10 and the start ≥12 (`../yeti_set_go/README.md` §3.3; H/M).
- **City Marathon:** the command blocks sit near their pads, but the course is 559 blocks wide; any
  server-side check that relies on a command block at the start will not run at the far side.
- **Space Race:** the command room sits under the start, and A12 reports `keep-spawn-in-memory: true`, so the
  room is loaded and its global `execute at @a` checks run at sim 6. That is an accident of start placement,
  not a portable design, and it gives no per-section coverage
  (`../space_race/README.md` §3; `../server_import/README.md` §2.1; H).
- **TGTTOS:** datapack `tick` functions are global and do not depend on chunk loading, but the packs are
  disabled on import and the folder/item syntax is dead on 1.21.1 (`../tgttos_badlands/README.md` §6; H).

**Conclusion:** the runtime must treat the course as **data** (regions, gates, pads, rings, spawns) and
evaluate it on the server tick, not as a set of blocks that must be near a player. If any command-block
layer is retained for authoring, it is a build-time preview only, never the live rules.

---

## 4. The 40-player contract the maps do not meet

The existing Phase-1 spec already names the fields; the map evidence shows why each is non-negotiable
(`game-plugins/AceRace/SPEC.md` §6, read-only):

| Contract field | Why the maps fail it | Runtime requirement |
|---|---|---|
| `startGrid` ≥ 40 unique feet positions, team rows | Every map has 1 spawn (City/Yeti/Badlands/Industry/Pits) or a single pen; Sky Battle has 4 team spawns | 40 solid, headroom-checked slots; spread to avoid collision; team rows for roster, not separate routes |
| `startStrip` + `startBarrier` | One-shot fills, gates saved closed, no 40-player strip | Real barrier blocks, per-round state, release at GO, re-close on reset |
| Ordered `sections` with `gate`, `respawn`, `onEnter`, `onRespawn`, `killY` | 111 route-ordered records with no enforcement (Clouds), setworldspawn (City/Yeti), none (Space) | Directional gates, per-section respawn, ordered state machine |
| `playBounds`, `voidY`, `spectatorBox` | No map declares bounds; Sky Battle's barrier is 13 blocks outside the build; Decision Dome has no outer rail | Explicit bounds + visible edges + confined spectator box |
| `pads` / `rings` with unique ids | Pads are block predicates; rings are decorative; effect scope is inconsistent — `@a` on Clouds 2/City/Yeti pads, `@p` on Space pads but `@a` on its elytra clear and kill planes | Data-driven pad/ring definitions, per-player trigger, cooldown, effect whitelist |
| `laps` / `loop` | No lap state; `LapCheck1` unused | Lap count, lap transition, lap loadout, final-lap flag |
| `loadouts` | Kits are command grants (trident, elytra, boots); some fail on 1.21.1 | Atomic per-checkpoint kit/effect swap, validated, no stale items |
| `overviews` / results | None | Start overview, finish overview, results card, spectator camera |

Acceptance must include: 40 players, solo, late join, quit/rejoin, DNF, time-up, skip-wait, forced map,
restart mid-race, and a **real-client elytra/riptide test** (a bot cannot fly;
`game-plugins/AceRace/SPEC.md` §10; H).

---

## 5. What the maps are still good for

They are excellent **authoring references** for geometry, palette, landmark rhythm, pad vocabulary and
start/finish composition. They are not runtime dependencies, not pack sources, and not a map contract.
The correct division is:

- **Map (original build):** surfaces, silhouettes, pads/rings as blocks, spawn pen, spectator deck.
- **Map data:** gate volumes, pad ids, effect payloads, respawn points, bounds, overviews.
- **Runtime:** timer, laps, ordered checkpoints, finish, positions, teams, effects, respawn, HUD, gate,
  reset, spectators, chunk coverage.

That division is the whole gap.
