# A7 — TGTTOS Badlands (MickeyKat) forensic dossier

**Status: research complete, read-only.** No game/plugin code, no builds, no pastes, no registry edits, no server restarts, no RCON. All writes are inside `game-plugins/research/existing_maps/tgttos_badlands/`.

| | |
|---|---|
| Source world | `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-badlands-by-mickeykat\TGTTOS Badlands - By MickeyKat` |
| World name | `TGTTOS Badlands Practise` |
| Version | 1.19.2 (DataVersion 3120) |
| Imported copy | `mcc_tgttos_badlands` (Paper 1.21.1, DataVersion 3955); files read read-only, no RCON; pack on disk but **not enabled** |
| Size | 6,559 chunks, 16 region files (27.0 MB), 411,834 non-air blocks |
| Footprint | x 4–254, y −61…12, z −5…69 (251 × 74 × 75) |
| Blocks | 62 IDs / 97 states; **0 command blocks**; 184 barriers; 181 cacti |
| Tile entities | 49: **1 sign** (start) + 48 daylight detectors |
| Entities | 28: 27 finish chickens + 1 chicken below the world |
| Datapack | `datapacks/tgttos`, enabled as `file/tgttos`; pack_format 10; 2 functions / 2 tags |
| Pack / resource pack | none (vanilla assets); map icon only |
| Gamerules | 36, including `fallDamage=false`, `doDaylightCycle=false`, `doMobSpawning=false`, `randomTickSpeed=0`, `spawnRadius=10` |
| Evidence key | **H** = directly observed file/command/render; **M** = inference from 2 sources; **L** = guess |

---

## 1. Executive summary

The Badlands map is a **single-player practice course** in a void superflat world: a hand-built badlands island with a start pen, a mine/rail yard, a canyon, a mesa with a cave chamber, a finish canyon, and a fenced chicken ledge under a huge "MCC" billboard. The whole game is **two datapack functions** (8 + 130 lines) and **one clickable sign**; there are no command blocks, no resource pack, no checkpoints, no round loop and no results screen. [H]

The run is: click the sign → 6.5 s title countdown → invisible barrier gate opens → run east across the island → punch a chicken through a chain fence at the far end. The finish is detected by `minecraft.custom:minecraft.damage_dealt_resisted` on chickens that are kept at Resistance VI, so a punch deals no damage but still scores. [H/M]

Its strengths are the **place**: a coherent badlands journey with one strong landmark per leg, a 1-block-wide offhand bridge economy (blue concrete in a warm palette), and a finish that reads as a place (fenced chicken ledge + MCC wall). Its weaknesses are everything a live game needs: multiplayer selectors, respawn/checkpoints, mode enforcement, a reset that survives quits, a timer/HUD, scoring, and 1.21.1 compatibility. [H/M]

The build is **reusable**; the datapack is a **reference**, not a dependency. The current `McmTgttos` plugin (v2.0.0, Paper 1.21.1) already implements the multiplayer/round/reset layer the map lacks, but its map contract would reject this build's **544 dyed-concrete decoration blocks** and its single spawn / missing pen / missing play-bounds metadata. [H code + H blocks]

---

## 2. How it plays (short version)

1. **Start pen** `x 8–12, z 20–41`, floor y=−43; player spawn `10,−42,31`; sign `9,−41,31` on the west wall. 184 barriers; 140 are the 3 gate walls, 44 are the static x=8 lip. [H]
2. Click the sign (line 1 has the clickEvent) → `start=1` → countdown. [H]
3. At countdown 120 the gate fill removes the 140 barriers; at 130 `playing=1` and the timer starts. [H]
4. Run **east**: badlands slope (x 12–62) → mine/rail yard (x 62–120) → canyon with a black rail ridge (x 120–150) → 19-block mesa climb (x 150) → mesa top (x 150–195) → 21-block safe drop into the finish canyon (x 194) → canyon floor up to x 230 → bridge/climb 5–6 blocks to the chicken ledge at x=241. [H/M]
5. Punch a chicken through the 3-high chain/iron-bar fence; `hit_chicken` increments; spectator 3 s; title with time; then `tp @a 10 −42 31`, `clear @s`, adventure, barriers restored. [H]
6. No checkpoints, no round timer, no placement order, no team scoring, no auto-restart. [H]

Full leg-by-leg: **`data/route.md`**. Datapack exact: **`data/datapack.md`**. Palette: **`data/palette.md`**.

---

## 3. Datapack — brief (full in `data/datapack.md`)

- **`load.mcfunction`**: 8 `scoreboard objectives add` lines: `hit_chicken` (custom `damage_dealt_resisted`), `playing`, `countdown`, `start`, `spawntime`, `playtimeS`, `playtimeT`, `finished`. No display slots, no gamerules, no teams. [H]
- **`loop.mcfunction`**: 130 lines / 113 non-empty, every tick:
  - kill item entities; keep chickens at Resistance VI; clear blue concrete in the start box;
  - force offhand blue concrete (count 1) and `hotbar.0` iron pickaxe with `CanPlaceOn` 44 blocks / `CanDestroy` blue concrete;
  - countdown → titles → gate fill → `playing`;
  - per-player `playtimeS/T`; every 20 ticks summon an invisible `remove_block` armor stand 10 above the player;
  - `hit_chicken` → stop timer, title, spectator, `finished+1`;
  - `finished=60` → barrier walls, `clear @s`, adventure, `tp @a` to start, reset timers, box-clean blue concrete around every marker, kill markers, 64 cactus fills, reset `hit_chicken`/`finished` for `@a`.
- **No checkpoints** (no objective, entity or block); `data/checkpoints.csv` has one `none` row. [H]
- **No respawn handling**; death is vanilla. [H]
- **No reset on load/quit**; only a finish triggers the cleanup. [H]

### Timers

| Timer | Length | Scope |
|---|---|---|
| countdown | 130 ticks (6.5 s) for 1 started player; **scales 1/N with N started players** | global |
| run time | unlimited | per player (`playtimeS/T`); shown only at finish |
| finish hold | 60 ticks (3 s) | per finisher (reset globally) |
| marker interval | 20 ticks | per playing player |
| no round/time limit | — | — |

---

## 4. Route and anatomy — brief

| Leg | x | y | Signature | Images |
|---|---|---|---|---|
| Start pen | 8–12 | −43/−42 | quartz floor, black wool frame, 184 barriers, start sign | `start_pen_barriers_iso.png`, `start_pen_barriers_top.png`, `start_iso.png` |
| 1 Badlands shelf | 12–62 | −43 → −31 | red stakes, cacti, first MCC billboard at x=64 | `leg1_iso.png`, `overview_iso.png` |
| 2 Mine/rail yard | 62–120 | −30 → −28 | two long white wings, solar arrays, MCC gate at x=64 | `mine_gate_iso.png`, `mine_iso.png` |
| 3 Canyon | 120–150 | −30 → −22 | stone-brick platform, black rail/wall x=137 | `canyon_iso.png` |
| 4 Mesa/cave | 150–195 | −22 → 0 | 19-block west cliff, **west-facing dead-end cave**, white pillar walls, mesa top | `cave_iso.png`, `cave_mouth_iso.png`, `plateau_iso.png`, `section_z31.png` |
| 5 Finish canyon | 195–230 | 0 → −5 | 21-block safe drop, open canyon, rising floor | `finish_iso.png`, `finish_top.png` |
| Finish ledge | 240–247 | −2 → 12 | gray MCC wall, red/white sign, 4 white towers, 27 chickens, chain fence | `finish_close_iso.png`, `finish_close_iso_back.png`, `finish_ledge_iso.png` |

Top-down with labels: **`renders/route_top_annotated.png`**; profile at z=31: **`renders/route_profile_z31.png`**; sections: `renders/section_z0.png`, `section_z8.png`, `section_z15.png`, `section_z31.png`, `section_z45.png`, `section_z60.png`; iso overviews: `renders/overview_iso.png`, `overview_iso_back.png`, `overview_iso_side.png`, `overview_top.png`.

**Checkpoints: none.** **Spectator area: none** — the finish wall/towers are decorative; finishers are vanilla spectators for 3 s, then teleported to the start pen. [H/M]

**Hazards**: cacti (181), void edges, the black rail ridge, the 19-block mesa climb, the dead-end cave (time trap), the 21-block drop (safe only because `fallDamage=false`). No lava/water/fire. [H]

**Alternative lines** (M/L, not walked end-to-end): north low bypass around the mesa at z≈0–8; south shoulder at z≈55–62; direct 19-block west-face bridge; the cave is a dead end.

---

## 5. Build / palette / lighting — brief

- **98% terrain, 2% built**: 402,322 terrain blocks (97.69%), 5,967 built masonry (1.45%), 2,130 accents/trim/light (0.52%). [H]
- Dominant: `stone` 46.86%, `terracotta` 36.26%, `granite` 3.86%, `andesite` 2.55%, `red_sand` 2.14%, `dirt` 1.76%, `brown_concrete_powder` 1.21%, `white_terracotta` 1.09%. [H]
- Built language: white terracotta mine wings/towers, cyan terracotta/concrete stripes, quartz start floor + finish approach, smooth-stone/stone-brick rails, nether-brick/blackstone trim, iron trapdoors/chains, MCC red/white concrete lettering. [H]
- **Lighting**: `DayTime=0`, no daylight/weather cycle; 94 sea lanterns + 12 lanterns; **no** glowstone/lava/torches. Daylight detectors are decorative, not light. [H]
- **Techniques**: voxel strata, hoodoos, two long flanking mine halls, 19-block billboards, rail motif, west-facing cave chamber, fenced chicken ledge + wall, invisible barrier pen. [H/M]
- Full: **`data/palette.md`**; histogram: **`data/blocks.csv`**.

---

## 6. 1.21.1 risks — exact likely failures

The live server is Paper **1.21.1** (data pack format **48**); the source world is **1.19.2** (DataVersion 3120). The imported world is **1.21.1** (DataVersion **3955**). A12's server path is `...\scratchpad\review\mcc_tgttos_badlands`. [H]

**Imported registration state (confirmed 2026-11-25):** the imported `level.dat` has `DataPacks.Enabled = [vanilla, file/bukkit, paper]` and `Disabled = [bundle, trade_rebalance]`; **`file/tgttos` is in neither list**, so the pack is on disk but not loaded. The source archive had `Enabled = [vanilla, file/tgttos]` and `Disabled = []`. The imported pack is **byte-identical** to the source (5/5 files, SHA-256). The imported world's `GameType` is **0 (Survival)** and Multiverse sets `gamemode: survival`; the source default was Creative and the saved player was Adventure. Full evidence and the exact fix list: **`data/import_and_121_verification.md`** and **`data/import_state.json`**. [H]

| # | Failure | Why | Exact effect | Tag |
|---|---|---|---|---|
| 0 | **Pack not enabled in the imported world** | imported `level.dat` has `DataPacks.Enabled=[vanilla, file/bukkit, paper]`; `file/tgttos` is absent (it is *not* in `Disabled=[bundle, trade_rebalance]` either). The source had `Enabled=[vanilla, file/tgttos]`. | the pack on disk is never loaded; no functions/tags, no countdown/gate/kit/finish. This is the first gate; enable `file/tgttos` before anything else. | **H** |
| 1 | **Functions/tags do not load** | 1.21 renamed `data/<ns>/functions/` → `function/` and `tags/functions/` → `tags/function/` | even after enabling, the pack registers no `load`/`tick`; no countdown, gate, kit, finish or reset. The map is inert; players are sealed in the pen (Survival/Adventure). | **H** |
| 2 | **Item commands fail** | 1.20.5 replaced item NBT with components | Lines 5–7 (`CanPlaceOn`, `CanDestroy`, `Enchantments`) error every tick; no offhand blue concrete, no pickaxe. Renaming folders alone is not enough. New syntax: `blue_concrete[minecraft:can_place_on={blocks:[…]}]`; `iron_pickaxe[minecraft:can_break={blocks:"minecraft:blue_concrete"},minecraft:enchantments={levels:{"minecraft:efficiency":2}}]`. | **H** |
| 3 | **Pack format 10 vs 48** | 1.19.2 pack | "Incompatible"/old warning; can usually still be enabled. Not the functional blocker (row 1). | **H/M** |
| 4 | **Sign click hotspot** | `clickEvent` is on **empty line 1**; the visible "Start" is line 2 with no event; 1.20+ signs use `front_text.messages` and per-line hit testing | The top line may still be clickable (line-index hit), but the visible word may not be. Verify in-game before relying on it. | **M/L** |
| 5 | **No gamemode enforcement** | imported `level.dat GameType=0` (Survival); Multiverse `gamemode: survival`; saved player was Adventure; source default Creative; server `force-gamemode=false` | New players enter in Survival (imported) and can break the build; `CanPlaceOn`/`CanDestroy` are ignored outside Adventure. In Creative they could fly out of the pen. Only the finish reset sets Adventure. | **H/M** |
| 6 | **No respawn** | no handler; `spawnRadius=10`, `doImmediateRespawn=false`, `keepInventory=false` | Void death → vanilla respawn near start, possibly outside the pen; timer continues; no run reset. | **H/M** |
| 7 | **Chickens unprotected if loop dead** | 27 normal chickens; Resistance is per-tick | One hit kills a chicken; no respawn. One chicken is already below the world (y=−114). | **H/M** |
| 8 | **Scoreboard persistence** | objectives saved in `scoreboard.dat` | The sign's `start` command works, but nothing reads it; `hit_chicken` still increments; no finish. | **H/M** |
| 9 | **Text/title/particle/playsound** | 1.21.1 still accepts JSON text components | No failure on 1.21.1 (the `click_event`/SNBT rewrite is 1.21.5). | **H/M** |
| 10 | **Objective criterion** | `minecraft.custom:minecraft.damage_dealt_resisted` still valid | No failure; objective already saved. | **M** |

**Bottom line:** the world loads, but the imported pack is not enabled and the game does not run. A port needs (0) enable `file/tgttos`, (a) `function/` + `tags/function/` + format 48, (b) item components, (c) Adventure mode + sign-click verification + a respawn/reset wrapper. The current plugin already provides the wrapper for its own maps. [H/M]

---

## 7. Comparison with `McmTgttos` (read-only)

Reference: `game-plugins/Tgttos/` — `TgttosGame.java` (99,719 bytes), `GameListener.java`, `model/TgMapData.java`, `config.yml`, `README.md`. Version v2.0.0, Paper 1.21.1. **This section reads the implementation; it does not change it.** [H]

### 7.1 Like-for-like

| Dimension | Badlands (this map) | `McmTgttos` v2.0.0 | Who is better for a live game |
|---|---|---|---|
| Format | One hand-built 1.19.2 practice world + 138-line datapack | 5 procedural Java maps, pasted into `mcc_tgttos`, native 1.21.1 | Badlands for place/art; plugin for repeatability |
| Start | Click sign → `start` → 6.5 s titles → barrier fill | Automatic pre-round (15 s), title card, barrier + strip check, GO at 0 | Plugin |
| Countdown | Title only; multiplayer scales 1/N | `McmFx.countdown`; per-player HUD; no scaling bug | Plugin |
| Finish | Any chicken with Resistance VI + `damage_dealt_resisted`; no tag/pen/order | Tagged invulnerable goal chickens, `PrePlayerAttackEntityEvent`, pen validation, decoys off, placement order | Plugin for integrity; Badlands' trick is portable |
| Checkpoints | None | `MapFeatures.Checkpoints` (maps opt in) + respawn at last checkpoint | Plugin |
| Respawn | Vanilla; timer continues; no kit reset | Explicit respawn at checkpoint/start, fresh kit, pun, sound | Plugin |
| Block reset | Marker armor stands + 31×31×21 box `air replace blue_concrete` + 64 cactus fills; only on finish; global | Tracks player-placed concrete in a set; `canPlace` bounds; `cleanupRound`; map re-paste/reset each round | Plugin |
| Anti-grief | `CanPlaceOn` 44-block allow-list (client-side, Adventure only); pickaxe `CanDestroy` blue concrete; **dirt is missing** | Server-side `canPlace` (only `*_CONCRETE`, only air, only inside build bounds, only racing) + `canBreak` (only tracked player concrete) | Plugin (server-side, complete) |
| Bridge economy | 1 blue concrete in the offhand, refilled every tick; no stack | 64 team concrete, `infinite-blocks: true` refill; hotbar | Badlands is a tighter skill test; plugin is faster/cleaner for 5 rounds |
| HUD | None; titles only | Sidebar, boss-bar timer, action bar, placements, time skip | Plugin |
| Multiplayer | Several global `@a` selectors; countdown scales; first finisher resets everyone | Participants, sides, points, team bonus, spectator confinement, quitters | Plugin |
| Spectator | Vanilla spectator 3 s, then `tp` to pen; no confinement | Confined spectator, results, placement reveal | Plugin |
| Round loop | None; one continuous run | 5 rounds, 300 s, 15 s pre, 6 s intermission, time skip | Plugin |
| Build | Bespoke badlands, mine, cave, finish; 411k blocks | Procedural generators; less place-specific, more repeatable | Badlands visually; plugin for volume |
| 1.21.1 | Dead (folder rename + item NBT) | Native | Plugin |

### 7.2 What this map does better [M unless noted]

1. **A coherent journey with one landmark per leg**: MCC billboard → mine gate → black rail → cave mouth → mesa drop → MCC finish wall. The player always has a next silhouette. [H renders]
2. **The finish is a place, not a row**: a 45-long gray wall (z 9–53), four white towers, a fenced 1-wide chicken ledge, a 19-long red/white MCC sign. It reads from the mesa and is unmistakable. [H]
3. **The 1-block offhand bridge is a readable economy**: one blue concrete at a time in a warm orange/cream palette; the blue is the only cool saturated player colour. It prevents block spam and makes every placement count. [H/M]
4. **Cheap, compact build**: 251×75×74, 411k blocks, 0 command blocks, 0 containers; a single map slot and a fast paste. [H/M]
5. **The barrier pen**: invisible gate + visible black-wool/quartz frame; clean start without doors. [H]
6. **The chicken-resistance finish**: a no-event, no-plugin finish that works for any chicken; a useful fallback for imported maps. [H/M]
7. **The cave/mine set piece**: a west-facing chamber with white pillar walls and lanterns; it gives the mesa an interior, not just a cliff. [H]

### 7.3 What it does worse [M unless noted]

1. **Multiplayer is broken**: countdown ×N, `@a` titles, `tp @a` on one finish, `clear @s` only the finisher, global `hit_chicken`/`finished` reset, global block cleanup, late joiners with 0 timer. [H]
2. **No respawn/checkpoint/round/timer/results**: one mistake costs the run; a void death soft-respawns with the clock running; no placement order or points. [H]
3. **No mode enforcement**: the world default is Creative; the saved player is Adventure; the pack never sets Adventure at start. Survival/Creative players can break the build or fly out. [H/M]
4. **No reset on load/quit**: placed blocks and broken cacti persist until a finish; 4 cacti are not in the refill list. [H]
5. **The start sign is fragile**: the clickEvent is on the empty first line, not the visible "Start"; one global latch; a Creative player can open the editor instead. [H/M]
6. **1.21.1 incompatibility**: folder rename + item NBT are fatal; the map is inert on the live server. [H]
7. **The cave is a time trap**: a west-facing dead end; a first-time player may enter and lose time. [H/M]
8. **Open edges/void only**: no play bounds or edge treatment; a 21-block drop is safe only because `fallDamage=false`. [H]
9. **No spectator confinement**: finishers can fly anywhere; no results screen. [H/M]
10. **No scoring/teams**: 27 chickens, first hit wins nothing; no order, no points, no team bonus. [H]

### 7.4 What is reusable [M]

| Idea | Reuse in `McmTgttos`? | Caveat |
|---|---|---|
| The **build** (island, mine, cave, finish) | **Yes** — as an imported map/build | Must adapt: 544 dyed concrete blocks vs `TgMapData.check`; 1 world spawn vs >=40; no pen/play-bounds metadata; y −61 vs local −60; strip saved chickens/sign/datapack |
| **Chicken-resistance finish** | As a fallback for imported maps with no event hooks | No pen/tag/order/decoy; depends on stat semantics and chicken survival |
| **One-block offhand bridge** | As an optional map kit/variant (`kit.bridging` + a 1-count refill) | The plugin's `canPlace` is server-side and complete; the allow-list should include dirt |
| **Sign-click start** | As a no-plugin start trigger | One global latch; the clickEvent is on an empty line; not a registration system |
| **Marker + box cleanup** | Concept only; the plugin's tracked set/re-paste is better | Global, multiplayer-hostile, not persistent-safe |
| **Cactus refill list** | As a per-block hazard reset list for imported maps | Generate it from the map diff; this one is 4 short |
| **Barrier pen** | Yes — the plugin already has `startBarrier` + `startStrip` | Use 140 gate positions, keep the static lip |
| **MCC wall/finish composition** | As a visual reference for `FinishRow` | The plugin's finish row is a different geometry |
| **Golden-hour lighting** | Set `DayTime=0` + `doDaylightCycle=false` on a map | The plugin's world may share time across slots; per-world gamerules |

### 7.5 Integration notes (findings, not a plan)

- **`TgMapData.check` rejects the build**: it counts any block ID ending `_concrete` and fails the map ("map uses dyed concrete - reserved for players"). Badlands has **544** dyed concrete blocks: red 190, white 106, gray 97, light gray 80, black 41, cyan 28, light blue 2. These are the MCC lettering and trim. To import, either allow decoration concrete (and keep the player-concrete rule by tracking placements, which the plugin already does) or substitute terracotta/wool. [H code + H blocks]
- **Start spawns**: the pen is 3×22 interior cells (x 9–11, z 21–40); the plugin requires >=40 spawns. Two rows of 20 fit; the saved world has only 1 world spawn. [H/M]
- **Chickens**: 27 saved normal chickens; the plugin spawns its own tagged/invulnerable chickens and requires >=8 spawn points + a `pen`. The saved chickens should be removed; the ledge is a ready-made pen region. [H/M]
- **Play bounds / start strip / pen**: not stored anywhere in the world; an adapter must define them from the geometry (island bounds, pen x 8–12/z 20–41, ledge x 240–242/z 11–51). [M]
- **Barriers**: 184 total; the 140 gate walls are the `startBarrier` list; the 44 x=8 lip is static and need not be removed. [H]
- **y offset**: build y −61…12; plugin local y −60…180 with `base-y: 64`; a +1 or a rebase is needed. [M]
- **No command blocks/containers/armor stands/item frames**: clean import; the only entities are chickens; the only tile entities are the sign + daylight detectors. [H]
- **The saved datapack and start sign should be removed** if the plugin drives the game; otherwise the sign can start a parallel countdown and the datapack can fight the plugin for items/gamemode. [M]

---

## 8. Findings (numbered, with evidence tags)

- **F1 [H]** The map is a 1.19.2 void-superflat practice world: 6,559 chunks, 411,834 non-air blocks, 62 IDs / 97 states, bounds x 4–254, y −61…12, z −5…69. It is a single floating island.
- **F2 [H]** There are **0 command blocks**; the only tile entities are 1 start sign + 48 daylight detectors. All logic is in `tgttos:load` (8 lines) and `tgttos:loop` (130 lines / 113 non-empty).
- **F3 [H]** The datapack is enabled in `level.dat` (`file/tgttos`), pack_format 10. The world's `scoreboard.dat` has 9 objectives, including a dead `playtime`.
- **F4 [H/M]** Finish detection is the `damage_dealt_resisted` statistic on chickens kept at Resistance VI; any of the 27 chickens counts. No tag, pen, order or decoy.
- **F5 [H]** There are **no checkpoints**; the only gate is the start barrier. The run is continuous; no round timer/time limit/results.
- **F6 [H/M]** Start is a sign click (`start+1`) → 130-tick countdown → `fill … air replace barrier` over x 9–12, y −42…−38, z 20–41 → `playing=1`. The clickEvent is on **empty line 1**; the visible "Start" is line 2.
- **F7 [H]** The countdown advances `@a countdown` once per started player; N players shorten 6.5 s to 6.5/N s. Titles are `@a` inside `as @a`, so they are broadcast N× per tick.
- **F8 [H]** At `finished=60` one finisher runs `tp @a 10 −42 31`, clears `hit_chicken`/`finished` for `@a`, and cleans blue concrete around **every** `remove_block` stand. A second finisher within 3 s loses their `finished` timer and stays a spectator.
- **F9 [H]** The block reset is finish-only and approximate: 31×31×21 boxes around markers summoned every 20 ticks, ±15 x/z, −25…+5 y. No reset on load/quit/death. The 64 cactus fills cover 179 positions; 4 cacti at x=154, z=33 are never restored, 2 stale positions at x=22, z=25 remain.
- **F10 [H/M]** There is no respawn handler: `fallDamage=false`, `spawnRadius=10`, `doImmediateRespawn=false`, `keepInventory=false`; void death is a vanilla respawn near the start with the timer still running.
- **F11 [H/M]** The world default is Creative (`GameType=1`), the saved player is Adventure, the server default is Survival with `force-gamemode=false`; the pack sets Adventure only in the finish reset. Mode is not enforced.
- **F12 [H]** On Paper 1.21.1 the pack is inert: `data/tgttos/functions/` and `data/minecraft/tags/functions/` are not read (1.21 needs `function/` / `tags/function/`), and lines 5–7 use pre-1.20.5 item NBT (`CanPlaceOn`, `CanDestroy`, `Enchantments`). Exact fixes in §6.
- **F13 [H]** The build is 98% terrain / 2% built: stone 46.86%, terracotta 36.26%, white terracotta 1.09%; 544 dyed concrete (signs/trim), 106 baked lights (94 sea lanterns + 12 lanterns), permanent time 0.
- **F14 [H]** The cave at x 150–195 is a **west-facing dead-end chamber** (mouth x≈150–160, z 14–50; solid back wall x≈195), not a through-tunnel. It contains two white/cyan pillar walls at x 182–189 and is a scenic detour/time trap.
- **F15 [H/M]** The finish is a 45-long gray-terracotta wall (x 242–247, y −10…2, z 9–53) with four white towers, a 19-long red/white MCC sign (x 241–242, y 4–12, z 22–40), a 1-wide chicken ledge at x=241/y=−2/z 11–51, and a 3-high chain/iron-bar fence at x=240. The player bridges up 5–6 blocks from the soul-sand trench and punches through the fence.
- **F16 [H code + H blocks]** `McmTgttos` v2.0.0 is a full multiplayer 5-round game; its `TgMapData.check` rejects 544 dyed concrete, requires >=40 start spawns and a pen/play-bounds; the Badlands build has 1 spawn, no metadata, and decoration concrete. The build is reusable only with an adapter/allowance; the datapack is not.

---

## 9. Comparison summary — one line each

- **Better than `McmTgttos`**: place identity, landmark progression, finish composition, one-block bridge tension, compact cheap build.
- **Worse than `McmTgttos`**: multiplayer correctness, respawn/checkpoints, mode enforcement, reset persistence, HUD/results, 1.21.1 compatibility.
- **Reusable**: the build, the chicken-resistance finish, the one-block kit, the barrier pen, the cactus-reset idea, the MCC visual language. **Not reusable**: the countdown arithmetic, the global `@a` resets, the finish-only cleanup, the sign-only registration.

---

## 10. Limitations and open questions

- **No in-game run**: I did not start the world or click the sign. The sign's clickable hotspot (empty line 1) and the 1.21.1 sign-click behavior are **M/L**, not verified.
- **No RCON**: the live world was not queried through RCON (A12 owns that). Its files were read read-only at A12's server path: DataVersion **3955**, `LevelName=mcc_tgttos_badlands`, `GameType=0` (Survival), Multiverse `gamemode: survival`, pack on disk but not enabled, 9 objectives and the upgraded sign still present. The import changed the name/mode and dropped the `file/tgttos` enable entry; the source world is otherwise the same content. [H]
- **No speedrun measurement**: leg times and the fastest line are **L**; the route is described as terrain anatomy, not a verified speedrun.
- **The north low bypass** around the mesa (z≈0–8) is a terrain observation, not walked end-to-end (**M/L**).
- **The renderer** is a block-colour isometric tool, not the game; it ignores block states/biome tint and shows barriers only with `--show-barriers`. Renders are evidence of geometry/palette, not of in-game lighting.
- **The 1.21.1 item-component rewrite** is described generically; exact new syntax should be checked by A12's compatibility matrix. The failure of the old syntax is certain; the exact replacement string may vary.
- **The map icon** (64×64) is the only source image; the map has no video and no screenshots.

---

## 11. Artifacts

| Path | Contents |
|---|---|
| `README.md` | this dossier |
| `REFERENCE_IMAGES.md` | every image → what it shows → what to take |
| `STATUS.md` | done/blocked/open, verification log |
| `data/datapack.md` | full datapack unpack, exact commands, 1.21.1 table |
| `data/datapack_commands.csv` | 113 non-empty loop lines: group, trigger, command, effect, 1.21.1 risk |
| `data/blocks.csv` | 97 block states, counts, share, role |
| `data/signs.csv` | the one start sign, exact JSON, click risk |
| `data/command_blocks.csv` | 0 command blocks (accounted) |
| `data/checkpoints.csv` | none (accounted) |
| `data/entities.csv` | 28 chickens (27 pen + 1 void), effects |
| `data/gamerules.csv` | 36 gamerules + effect |
| `data/objectives.csv` | 9 objectives, created/used/dead |
| `data/can_place_on.csv` | 44 allow-list entries vs 62 palette IDs; 19 not placeable-on |
| `data/cactus_reset.csv` | 64 reset commands, 179 positions; missing/stale |
| `data/route_waypoints.csv` | 16 waypoints with leg/what/notes |
| `data/route.md` | leg-by-leg route |
| `data/palette.md` | palette, lighting, techniques |
| `data/raw/` | exact copies of pack.mcmeta, tags, load/loop functions |
| `data/source_hashes.csv` | SHA-256 of source files |
| `data/import_and_121_verification.md` | A12 import finding confirmed + exact 1.21 fix requirements + the 3 old item-NBT lines |
| `data/import_state.json` | source vs imported `level.dat` / `DataPacks` / pack / sign / scoreboard, machine-readable |
| `frames/` | full-resolution raw evidence (overview/close-up renders + map icon) |
| `renders/` | labelled top-down/profile + all iso/top/section renders |
