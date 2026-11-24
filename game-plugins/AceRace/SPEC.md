# Ace Race rebuild — v1 specification (for owner approval)

**Status:** research complete. No source changed, no map built. This is the gate document: build starts only after the owner approves this spec and the schematics in `SCHEMATICS.md`.

**Owner decisions (24 Nov 2026):**
- Build order: **Ace Race first**; Decision Dome second.
- v1 map slate: **Sunset Skyway + Kessler Run**.
- Dome theme (later): **Nocturne Orrery**.
- Dome integration (later): **extend the core API** (`McmVoteRequest`, `McmLobby.Venue` v2).

**Primary sources:** `research/acerace/mechanics_rules`, `mechanics_movement`, `map_layout`, `map_look`, `map_space`, `presentation`, `fan_recreations`, `code_audit`. Every design decision below is MCC-sourced unless marked `[ORIGINAL]` or `[ADAPT]`.

---

## 1. Scope

### In
- A from-scratch rebuild of the shelved Ace Race plugin.
- Two original maps: **Sunset Skyway** (ground/elytra, 3 laps) and **Kessler Run** (space, elytra/riptide, 2 laps).
- One race per event; map chosen by rotation or `/mcm map acerace <id>`.
- Order: contract + world/paster + Test Grid + runtime/HUD first; then Sunset Skyway; then Kessler Run.

### Out
- Tidal Foundry (third map) — deferred.
- Ghost/practice mode, custom resource pack, team scoring, PvP, block editing, loot.
- Copying any MCC map layout, name, logo or asset.

---

## 2. Rules

### Format
- **Per-map laps and time**, not fixed: v1 Sunset Skyway 3 laps / 10:00; Kessler Run 2 laps / 8:00. Config allows 1–4 laps and 5–15 min.
- The clock starts at the countdown while players are sealed in the start pen.
- 9–10 checkpoints per lap (Sunset 9, Kessler 10), depending on the map.
- **Start and finish are the same structure**, crossed from opposite sides; the lap loops, or uses an explicit lap spawn on point-to-point maps.
- One event = one map. No multi-round Ace Race.

### Checkpoints
- Checkpoints are **ordered**; only the next checkpoint advances. Crossing one early shows “Missed <next> — go back” at most every 2 s; it never advances or teleports.
- A checkpoint:
  1. records a split;
  2. sets the respawn point;
  3. atomically replaces the loadout (the previous buff is removed, the new one granted);
  4. fires the MCC-style chat split.
- A checkpoint that swaps gear shows the centre-screen `+ Elytra − Trident` / `+ Trident − Elytra` toast. `[MCC]`
- Checkpoint volumes are **directional gates**, not four-point sampling.

### Movement and buffs
- **Elytra and trident are swapped, never carried together.** `[MCC]`
  - Elytra is granted by a **wing ring** (historical Space Race mechanic) or a checkpoint, and removed on landing.
  - Trident is granted for a riptide section; Riptide only works in water/rain, so trident throwing is cancelled. `[MCC]`
- Pad taxonomy: `[MCC]`
  - **Speed:** yellow/orange chevron strip; forward boost, decays after leaving.
  - **Launch:** red concentric target; aim-dependent forward/up launch.
  - **Jump:** green concentric target; vertical launch.
  - **Slow:** green leafy patch; cancels speed.
  - **Wing ring:** light-blue ring; grants elytra and starts a glide.
  - **Boost ring:** gold/lime ring; adds velocity along its normal.
- No in-race fireworks/rockets in v1. Kessler Run is built to be flyable with zero rockets. `[MCC]` (official kits have no in-race fireworks; rockets are an `[ORIGINAL]` option deferred to config).
- No damage, no PvP, no hunger, no block break/place, no item drop/pickup, no inventory moves, no portals.
- Falling below the section kill plane or outside the play bounds returns the player to the last checkpoint within ~3 s, with a sound and action-bar “Recovered”. No death.
- **Collision:** keep default player collision in v1 (official behaviour is unverified); spread the start grid. If starts jam, switch to a scoreboard team collision rule `NEVER` rather than `setCollidable(false)` (the latter breaks projectiles).
- **Late join/reconnect:** competition-safe policy — finish/DNF as a spectator, no mid-race resume. Solo/fast-test may resume at the last checkpoint via config.
- **End:** all online racers finished, or time limit; DNFs ranked by route progress, no placement points.

---

## 3. Scoring (all configurable)

Progress points:
- Checkpoint: **1** each, in order.
- Non-final lap: **3**.
- Finish: **8** once.
- No fastest-lap points; best lap is cosmetic.

Placement points (finishers only), scaled official shape:

| Place | Pts | Place | Pts | Place | Pts | Place | Pts |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 100 | 6 | 60 | 11–15 | 41,38,35,32,29 | 26–30 | 11,10,9,8,7 |
| 2 | 90 | 7 | 55 | 16–20 | 27,25,23,21,19 | 31–35 | 6,6,5,5,4 |
| 3 | 81 | 8 | 51 | 21–25 | 17,15,14,13,12 | 36–40 | 4,3,3,2,2 |
| 4 | 73 | 9 | 47 | | | | |
| 5 | 66 | 10 | 44 | | | | |

Worked totals:
- Sunset Skyway (9 CP × 3 laps): 27 + 6 + 8 + 100 = **141** for 1st.
- Kessler Run (10 CP × 2 laps): 20 + 3 + 8 + 100 = **131** for 1st.
- A DNF keeps progress only and never outranks a finisher.

Rationale: MCC’s 400/−10 + 300/240/180/120/60/15 shape, scaled to the project’s 50–150 top-end. The observed MCC 35 +1050 total remains an open question and is **not** encoded.

---

## 4. Map contract

Adopt the `RaceMap` / `RaceMapData` contract from `code_audit/README.md` §3, modelled on `TgMap`/`CourseModel` and FootRace’s plane gate.

`RaceMap`:
- `id`, `displayName`, `themeId`, `laps`, `timeLimitSeconds`, `countdownSeconds`, `build(long seed)`, `blockBudget()`, `notes()`.

`RaceMapData`:
- sparse `Blocks` + bounds;
- `startGrid` (>= 40 unique feet positions, team rows), `startStrip`, `startBarrier`;
- ordered `sections` with `gate`, `respawn`, `onEnter`, `onRespawn`, `killY`, pad ids, ring ids;
- `finishGate`, `loop`, `lapSpawn`;
- `pads`, `rings`, `hazards`;
- `playBounds`, `overviews`, `spectatorBox`, `voidY`;
- optional `routeHints` for validators/previews;
- `check()` returning contract errors; a map with errors is refused at registry load.

Contract checks: 40 spawns with solid ground and headroom; start strip contains all spawns; barrier is real blocks; each respawn is solid, in bounds, outside hazards; kill planes ordered; directional finish; pads/rings unique and in bounds; block budget; no air-only map.

---

## 5. World and paste

- **One `mcc_acerace` void world**, created at plugin enable via `McmBuild.voidWorld`.
- Base Y **80**; two map slots at `(0, 80, 0)` and `(1800, 80, 0)`; slot spacing configurable (default 1800 × 1400).
- Each map is pasted into its own slot **once per session**; static maps do not need a per-game wipe.
- Paste pipeline adapted from TGTTOS: worker prepares blocks grouped by chunk; main thread loads chunks with `getChunkAtAsync` (8–12 in flight), adds chunk tickets, writes 10–15 ms/tick, no physics; log blocks/chunks/ticks.
- Prebuild the next/forced map during the current race or countdown; at game start, only load chunks if the slot is already built.
- Stale-key cleanup per slot; cancel-safe union of old/new keys; tagged entity cleanup per session.
- Test the one-time-paste persistence on Paper 1.21.1 before relying on it; if unloaded chunks do not persist, fall back to repasting the selected slot at game start.
- World rules: no mobs, no weather/daylight cycle, no fire tick, no mob griefing, PvP off, immediate respawn, spectators do not generate chunks, difficulty peaceful, spawn chunks not kept.
- Spectators are confined to the current map’s `spectatorBox`; teleports outside are cancelled; other slots are out of bounds.

---

## 6. Runtime

State machine: `IDLE → LOADING → PRE → RACING → ENDING → FINISHED`.

- `LOADING`: resolve map, ensure world/slot, title card “Preparing <map>”.
- `PRE`: teleport grid, apply start loadout, close barrier, start-strip leash, countdown (fast-test aware), skipWait ends it.
- `RACING`: remove barrier, timer, per-tick rules, second-tick HUD.
- `ENDING`: stop rules, results, spectator all, top-3 reveal/fireworks, wait.
- `FINISHED`: cleanup, `ctx.finish()` exactly once.

Per-racer model: `lap`, `nextCp`, `lastCp`, `finished`, `dnf`, `online`, `place`, `lapStartTick`, best splits, pad/ring/respawn cooldowns, `prev`, progress. Per-lap transient state resets; scoring state persists.

Tick budget:
- every tick: fall/hazard/bounds, current section pads/rings, next gate crossing, `prev`;
- every 4 ticks: action bar;
- every 10 ticks: ranking, nearby pad particles;
- every 20 ticks: timer, sidebar, time warnings.
- Do not scan all rings for all racers; index by section.
- Do not call `world.getBlockAt` per racer per tick unless the section has block hazards.

Checkpoints/laps:
- Crossing `sections[nextCp].gate` advances; apply `onEnter`, award CP, record split.
- `nextCp == sections.size()` → finish gate. If `lap < laps`: increment lap, award lap points, apply lap loadout, teleport to `lapSpawn` only if `loop == false`; else let the circuit continue. If final lap: finish.
- Gate crossing requires a real segment/plane crossing in the gate’s forward direction inside the gate box, and the player inside `playBounds`.

Pads/rings:
- Pad trigger by region + cooldown; speed capped; launch direction from data, not a hardcoded switch.
- Ring trigger by plane crossing in the normal direction within the radius band; boost along the normal, capped; never use client look direction.
- Wing ring grants elytra and starts a glide; landing removes it.
- Riptide pad/section requires water/rain and a trident; cancel trident throws.

Falls/respawns:
- Fail on section killY, map voidY, outside playBounds, hazard region, or hazardous feet/below block; no `isOnGround()` requirement.
- Respawn: teleport to `section.respawn`, zero velocity, clear fire/fall/glide, apply `onRespawn`, 10-tick grace, sound + action bar.
- Hard void plane as last resort.

Spectators/late join/quit:
- Finish: `McmSpectator.makeSpectator` at `overviews[0]`, confine to `spectatorBox`, cancel outside teleports.
- Late join: participant running → spectate (v1 competitive policy); finished/DNF → spectate; non-participant → spectate.
- Quit: mark offline + DNF if racing; never process offline racers; results list DNFs by progress.
- Rejoin: spectator by default; solo/fast-test resume at last checkpoint via config.

---

## 7. Presentation

- **Pre-game:** 4-page “HOW TO PLAY” deck (letterboxed broadcast frame, live map shot per page, `PAGE n/4`, logo watermark, copy such as “no coins if you do not finish”). `[MCC]` This is the biggest premium upgrade over a logo card.
- **Start:** teleport → deck/card → pre-race warm-up → **10 s frozen countdown** → release with **no “GO” word**. `[MCC]`
- **HUD:** map, `LAP [n/N]` with one slot per lap time, `PLAYERS REMAINING`, `CHECKPOINT n/N`, standings with signed gaps, action bar position. `[MCC]`
- **Checkpoints:** chat split only from lap 2 onward; no big title on every checkpoint. `[MCC]`
- **Titles:** reserved for lap completion (`Lap 3/3`) and `Game Over!`. `[MCC]`
- **Finish:** placement-coloured medal icons in chat, `made 1st Place!`, gate flare. `[MCC]`
- **Results:** top-5, personal splits and podium are `[ORIGINAL]` additions; label them as such and do not present them as MCC.
- **Sounds:** vanilla only; use `McmFx` for countdown/titles/fireworks; no copied music.
- **Spectator:** overview at the map’s postcard point; clickable player teleports; no flying out of bounds.

---

## 8. Admin and testing

- `/acerace list|info <id>|build <id>|tp <id>|validate <id>|reload|respawn|debug`.
- `/mcm play acerace`; `/mcm map acerace <id>`; `McmGame.maps()`, `forceNextMap`, `currentMap`.
- `skipWait()` ends countdown/results. `skipRound()` returns false (single race) or an explicit admin “end with no points”.
- Fast-test: countdown and results ≤ 1 s; lap/race timers unchanged.
- Solo test must work.
- Offline validators: contract check, route flood-fill, `Walk`/`JumpSim`/`Glide` simulations, block-budget check, preview renders from at least two angles.
- Bot harness can verify lifecycle/HUD/checkpoint timing but **cannot fly elytra**; the elytra and riptide sections require a real-client playtest.
- Acceptance matrix: 40 players, solo, late join, quit/rejoin, DNF, time-up, skipWait, fast-test, map force, restart mid-race.

---

## 9. Build plan

1. **Contract + foundation:** `RaceMap`/`RaceMapData`, `RaceWorld`, `RacePaster`, `RaceRegistry`, `RaceGame`, `RaceRuntime`, `RaceHud`, `RaceListener`, `RaceScoring`; a small **Test Grid** map to prove start/checkpoint/pads/finish; `/acerace` commands.
2. **Sunset Skyway:** generator + palette + landmarks + start/finish + spectator; offline renders; route/timing validator.
3. **Kessler Run:** generator + wing rings + riptide + ring pass + freighter + dish farm; offline renders; glide validator.
4. **Verify:** contract checks, route proof, block budget, HUD/lifecycle bot run, elytra/riptide real-client playtest.
5. **Owner playtest:** feedback file, fixes, then v1 sign-off.

---

## 10. Acceptance checklist

- [ ] Two maps load from the registry; `/acerace list` shows ids, laps, time, block count, validation state.
- [ ] One map per event; `/mcm map acerace <id>` forces the next race.
- [ ] 40 unique start slots; team rows; barrier drops at GO; start strip holds players.
- [ ] Laps and time come from the map, not constants.
- [ ] Checkpoints are ordered; skipping shows feedback and does not advance.
- [ ] Checkpoint swaps elytra/trident atomically and shows the toast.
- [ ] Checkpoint splits appear in chat from lap 2; no big title on every checkpoint.
- [ ] Speed/launch/jump/slow/wing rings behave per taxonomy; no client-look boost.
- [ ] Fall/void returns to the last checkpoint in ≤3 s, no damage.
- [ ] Finish gate is directional; finishers become confined spectators.
- [ ] Late join/quit/rejoin follow the competition-safe policy.
- [ ] HUD shows map, lap boxes, players remaining, checkpoint, standings, position.
- [ ] 4-page intro deck and 10 s frozen countdown; no GO word.
- [ ] Results: top-5, DNFs by progress, personal splits; podium labelled original.
- [ ] Fast-test and skipWait work; lap/race timers are not shortened.
- [ ] Solo race completes.
- [ ] Block budget ≤350k per map; paste ≤15 ms/tick; no main-thread freeze.
- [ ] Chunk tickets hold the current slot; spectators cannot generate chunks.
- [ ] Restart mid-race cleans up or recovers; no stale entities/blocks.
- [ ] Both maps render from two angles and pass route/jump/glide validation.
- [ ] Real-client elytra and riptide test passes.

---

## 11. Open questions (do not guess)

- Exact official pad velocities, riptide level, checkpoint volumes, and player collision.
- Whether a 1-frame “GO” exists; v1 follows the observed no-GO release.
- The four lap-box semantics.
- In-race fireworks: v1 excludes them; Kessler is zero-rocket.
- One-time-paste persistence on Paper 1.21.1; test before relying on it.
- Exact spectator/grandstand convention; v1 uses a per-map overview + box.

---

## 12. Approval

Owner approval of this file and `SCHEMATICS.md` is the build gate. After approval: build Phase 1 only (contract + Test Grid + runtime), then show the first playable before decorating either map.
