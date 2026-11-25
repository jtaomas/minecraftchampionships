# TGTTOS Pits — forensic dossier (A9)

Source: `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-pits-by-mickey-kat\TGTTOS Pits - By Mickey_Kat`
Source world: 1.19.1 (DataVersion 3117). Live imported world: `mcc_tgttos_pits`, 1.21.1 (DataVersion 3955), 3,254 chunks, 0 command blocks, 1 clickable start sign at `14,-1,36`; datapack `Pits` on disk but **not enabled** (A12; direct read).
Owner: A9. Research only: no game/plugin code, no builds, no pastes, no registry edits, no server restarts. All writes are inside `game-plugins/research/existing_maps/tgttos_pits/`. Shared tools and the source world were read-only.

**Confidence tags:** **[H]** directly observed (file, NBT, command, pixel, or the imported world read here / by A12 and cited); **[M]** inference from two sources; **[L]** guess. Every claim below is tagged; the per-file tables in `data/` carry the same tags where relevant.

**What was opened:** every image cited here and in `REFERENCE_IMAGES.md` was opened at full resolution (`renders/` and `frames/`), not described from memory. The 1.21.1 claims were checked against the local Paper 1.21.1 jar (`versions/1.21.1/paper-1.21.1.jar`) and its `version.json` (world 3955, data pack format 48).

---

## 0. Executive summary

Pits is a compact, single-course 1.19.1 TGTTOS practice map built as a floating night-city diorama. Three long street islands (top y=-3, -7, -9) are separated by two 12-block-wide pits with a 38-block drop to the respawn plane; the start is a barrier cage cut into the west island; the finish is a 7×34 sunken pool holding 20 chickens. The kit is an unbreakable iron pickaxe plus one team-coloured concrete in the offhand, refilled every tick; the only way across the pits is to bridge (or take the park/roof detour). The datapack is six functions (no `block_reset`), no command blocks, no containers: a Start sign click starts a 7 s countdown, opens the cage, runs a per-player stopwatch, detects a chicken punch as `damage_dealt_resisted`, sets the finisher to spectator, cleans up placed concrete around armor-stand anchors, and resets everyone 7 s after the first finish.

The map is strong as a **diorama and a route idea** (three roads over a lit void, a chicken pool finish, a one-click start) but weak as a **multiplayer game**: the first finisher stops everyone's timer and, 7 s later, teleports everyone back; there are no checkpoints; the Start sign's click is on a blank line; and the whole datapack is inert on Paper 1.21.1 because 1.21 renamed the data pack directories and replaced item NBT with components. On 1.21.1 as-is the imported world has the pack **disabled** (A12: `Enabled=[vanilla,file/bukkit,paper]`, `file/Pits` not enabled); even after enabling it, 1.21's folder rename and item components make it inert, so the cage never opens and the map is unplayable.

---

## 1. Identity and world facts

| Fact | Value | Confidence |
|---|---|---|
| Level name | `TGTTOS Pits - Practise map` | H (`level.dat`) |
| DataVersion | **3117** = 1.19.1 | H |
| World GameType | source **1 (creative)**; imported **0 (survival)**, MV `SURVIVAL`; source playerdata GameType 3 (spectator), previous 2 (adventure) | H (source/import `level.dat`, source playerdata) |
| Difficulty | 0 (peaceful) | H |
| Chunks present / occupied | **3,254 / 51** | H (region scan) |
| Non-air blocks / states | **164,145 / 63** | H (`world_scan.json`) |
| Extents | x **-3..117**, y **-64..29**, z **-9..83** | H |
| Playable shell | x -3..80, z -9..80; external base/banners extend past it | H |
| Command blocks | **0** | H (no `command_block` state or block entity) |
| Containers | **0** (no chests/barrels/shulkers) | H |
| Signs | **1** (`oak_wall_sign` at 14,-1,36) | H |
| Entities | **20 chickens** at y=-10, x61.5..67.6, z19.2..52.6; no armor stands/item frames | H (`entities/*.mca`) |
| Datapacks enabled | source: `vanilla`, `Fabric Mods`, **`file/Pits`**; imported live: `vanilla`, `file/bukkit`, `paper` — **`file/Pits` not enabled** (A12) | H (source `level.dat`) / H (import) |
| Player test stats | `damage_dealt_resisted: 200`, `damage_dealt: 0`, `chicken_spawn_egg: 20`, `oak_sign: 1`, play_time 80,716 ticks | H (`stats/*.json`) |
| Player saved inventory | iron pickaxe (Unbreakable, CanDestroy 10 concretes) + cyan concrete (CanPlaceOn list); `selectColor=6` | H (playerdata, scoreboard.dat) |

The maker's own stats are the best proof of the finish mechanic: 200 resisted damage, 0 normal damage, exactly 20 chicken spawn eggs used, one sign placed. **[H]**

The old Industry start cage survives at **x63..84, y -33..-29, z9..13** (184 barrier blocks) inside a solid brown-wool building; it is not connected to the Pits start (15,-2,36) and is inert. **[H]** coordinates; see §8 for the lineage evidence.

---

## 2. Datapack — what it does

Full line-by-line documentation: `data/datapack.md`; verbatim export: `data/commands_export.txt`. Summary:

| Function | Lines | Tag | Role |
|---|---:|---|---|
| `load` | 23 | load | 14 objectives + initial time scores |
| `loop` | 129 | tick | finish, spectator, cleanup, reset, timer, HUD, kit, block reset |
| `color` | 111 | tick | `/trigger color` menu and team tags (default red) |
| `start_timer` | 25 | tick | 7 s countdown, GO, open cage, add `playing` |
| `gamerules` | 16 | tick | 13 gamerules + `difficulty peaceful` every tick |
| `respawn` | 3 | tick | store feet Y; y ≤ -41 → tp to start |
| `pack.mcmeta` | 6 | — | `pack_format: 10`, “Made by Mickey_Kat” |

### 2.1 Start

- The sign at **14,-1,36** is an oak wall sign facing east, attached to x13. Its **line 1 is blank but carries** `clickEvent: run_command → tag @a add starting`; **line 2 is green “Start” with no click event**. The visible word is not the clickable line. **[H]** NBT; **[M]** per-line click behaviour.
- `tag @a` starts **all** players, not just the clicker. **[H]**
- The cage is a barrier shell at **x13/x17, z25/z46, y -2..1** (156 barriers); interior floor is quartz at y=-3, x14..16, with x17 black wool. It has three exits: east (x17) to Road A, north (z25) to a 2-high corridor z21..24, south (z46) to a 2-high corridor z47..50. **[H]**
- At countdown 140 the cage is cleared with `fill 14 1 46 17 -2 25 air replace barrier`; the three walls are rebuilt only at reset. **[H]**

### 2.2 Countdown and timer

- Countdown ticks only for `starting` players; 1→39 is a 2 s silent lead-in, then 5/4/3/2/1 at 1 s each, GO at 140 = **7.0 s**. **[H]**
- `playing` is added at GO; the stopwatch adds **5 points/tick** = 100/s = 0.01 s per point, displayed as `MM:SS:hh` (0.05 s resolution). The actionbar is coloured by team tag; the default is red. **[H]**
- The finish title shows the finisher's time for 5 s (`title times 0 100 20`). **[H]**
- **Bug:** `tag @a remove playing` on the first finish removes `playing` from everyone; all timers freeze. **[H]**

### 2.3 Respawn

```mcfunction
execute as @a store result score @s respawn run data get entity @s Pos[1]
execute as @a[scores={respawn=..-41}] at @s run tp @s 15 -2 36 -90 0
```

Always the start; **no checkpoint**; the timer keeps running. The kill plane is feet y ≤ -41, 38 blocks below Road A, 3 blocks above the top glass layer. **[H]** command; **[M]** truncation.

### 2.4 Finish

- Every tick all chickens get Resistance 1 s, amplifier 5, no particles. A punch deals 1 damage, all resisted, so the custom stat `minecraft.custom:minecraft.damage_dealt_resisted` increments. At ≥1 the player gets `finished`, is set to spectator, and the `finished` score starts counting. **[H]** commands; **[M]** stat/effect interaction; **[H]** maker's 200 resisted / 0 normal damage.
- **Any** of the 20 chickens works; no row, no arch, no per-chicken check. **[H]**
- The first finisher's 60-tick (3 s) cleanup and 140-tick (7 s) reset are global, not per-player. **[H]**

### 2.5 Block reset

- Every tick: `fill 14 1 46 17 -2 25 air replace #minecraft:mineable/pickaxe` clears player blocks in the start box. **[H]**
- Every second: an invisible `remove_block` armor stand is spawned 10 blocks above each playing player. **[H]**
- 3 s after a finish: for every stand, a 31×31×31 `fill … air replace <colour>` removes the 8 player concrete colours; then all stands are killed. The first finisher's cleanup kills every stand, so other players' earlier concrete can survive. **[H]**
- 7 s after a finish: three barrier planes are rebuilt, **all** players are teleported to start, set adventure, cleared, and timers reset. **[H]**
- There is **no** global block reset and **no** `block_reset.mcfunction` (Industry has one). **[H]**

### 2.6 Gamerules

`difficulty peaceful`; `sendCommandFeedback false`; `fallDamage false`; `commandBlockOutput false`; `doEntityDrops false`; `disableRaids true`; `doFireTick false`; `doDaylightCycle false`; `doWeatherCycle false`; `doMobSpawning false`; `fireDamage false`; `showDeathMessages false`; `freezeDamage false`; `keepInventory true`. All re-applied every tick. **[H]**

### 2.7 Kit and HUD

- Hotbar 0: unbreakable Efficiency II iron pickaxe, `CanDestroy` only 10 team concretes.
- Offhand: team-coloured concrete, `CanPlaceOn` a 40-material whitelist (39 for yellow/green); set to 2 then 1 in the same tick, so the final count is **always 1**. The 2 is redundant. **[H]**
- Actionbar: 8 team colours; default red. No sidebar/boss bar. **[H]**

### 2.8 Colour menu

- `color` is a **trigger** objective; the player must run `/trigger color`. No sign/book/command tells them. **[H]**
- Menu: Red 1, Orange 2, Lime 4, Cyan 6, Aqua 7, Blue 8, Purple 9, Pink 10. Yellow/green are struck through (“used for building this map”). **[H]**
- `selectColor=0` maps to red; the menu resets `color` to 0 after showing. **[H]**

---

## 3. Route and anatomy

Full leg-by-leg: `data/route.md`. Images: `renders/route_schematic.png`, `renders/course_top_labeled.png`, `renders/start_top_labeled.png`, `renders/finish_top_labeled.png`, `renders/pit_section.png`.

| Leg | Geometry | Top y | Confidence |
|---|---|---:|---|
| Start cage | x13..17, z25..46; 3-wide clear floor; sign at x14,-1,36 | -3 | H |
| Road A | x18..25, z17..54; 7-wide lane x19..25, yellow edges, quartz 2:1 dashes | -3 | H |
| Pit 1 | x26..37 (12 wide), z17..54; void to glass at -44; kill plane -41 | — | H |
| Road B | x38..46, z17..54; 9-wide lane, yellow edges, quartz dashes; solid tower to -64 | -7 | H |
| Pit 2 | x47..58 (12 wide), z17..54 | — | H |
| Glass lip | x59..60, z19..52, black stained glass on a solid tower | -9 | H |
| Chicken pool | x61..67, z19..52; cobble floor, brick walls; 20 chickens | -11 | H |
| East plaza | x68..79, z17..54; 1-block quartz slab; backstop | -9 | H |

- The course direction is **west → east**; the spawn/respawn yaw is -90 (east). The roads run north-south, so the player crosses them perpendicular to their markings. **[H]** tp yaw + road coords.
- The two pits are continuous voids beyond the road ends; you cannot walk around them at road level. The roads are islands. **[H]** (no surface in x26..37 or x47..58 for z8..61 at y -16..5).
- The park (x -2..14, z15..56, up to y17) and the south plaza (z51) are reachable through the 2-high side exits; the park's y5 top may reach the north roofs, a long detour. **[H]** geometry; **[M]** viability.
- **No checkpoints.** `checkpoints.csv` lists start/finish gates and pits as anatomy, not checkpoints. **[H]**

---

## 4. Build, blocks, palette, lighting

Counts: `data/blocks.csv`; analysis: `data/palette.md`.

- 164,145 blocks, 63 states; the **shell + void is 72,121 blocks (43.9%)**: 34,374 black glass (ceiling 7,216 + 10 pit layers 27,090 + 68 finish lip), 32,329 light-blue wool (≈344 per layer × 94 layers), 2,709 shroomlight + 2,709 black carpet. **[H]**
- Roads: blackstone 1,653, quartz 7,017 (paint/trim), yellow terracotta 624, cobblestone 22,567 (slabs/towers/pool floor). **[H]**
- Buildings: bricks 15,650, gray terracotta 10,141, light-gray/brown/gray wool ≈16,100, brown terracotta 4,409, stone bricks 990, polished diorite 792. **[H]**
- Park: green concrete 1,827 + green wool 966 + end stone bricks 2,137 + snow 174 + red sandstone 188 + sandstone/stairs/slabs ~150 + netherrack 34 + granite 8 + yellow concrete 8 + red/white wool 160. **[H]**
- External banners: red concrete 1,306 + white concrete 675 at z=83; outside the wall. **[H]**
- Barriers: 340 (156 start + 184 legacy Industry cage). **[H]**
- Lighting: **SkyLight 15** at the start, both roads, the finish lip, the chicken height and y=28 below the ceiling; the black glass is transparent to skylight. 8 glowstone lamps. 2,709 shroomlight under 10 glass layers at y=-64; the saved `BlockLight` at the sampled pit cells is 0. No torches/lanterns. **[H]** light arrays; **[M]** visual intent.
- Build techniques: floating city on a lit void; thin slabs vs solid towers; 10-layer black-glass void with kill plane 3 above; block “paint” (yellow edges + quartz dashes); 1-thick wool sky wall + black-glass ceiling; scenic park; external banner panels; invisible barrier cage; armor-stand cleanup; chicken pool. Details in `data/palette.md`.

---

## 5. 1.21.1 compatibility — exact likely failures

Target: Paper **1.21.1**, world version **3955**, data pack format **48** (verified in `versions/1.21.1/paper-1.21.1.jar` `version.json`). Source world DataVersion 3117 (1.19.1).

**Imported state ([H]):** the live `mcc_tgttos_pits` `level.dat` has `DataPacks.Enabled=[vanilla, file/bukkit, paper]`; `file/Pits` is **not enabled** (not in Enabled, not in Disabled). The pack is on disk (`datapacks/Pits`). The source archive had `Enabled=[vanilla, Fabric Mods, file/Pits]`. So the **first** blocker is that the pack is not loaded; the folder rename and item NBT are the next two. Exact evidence: `../server_import/README.md` §5, `../server_import/data/datapacks.csv`, `../server_import/data/worlds.csv`; confirmed against the source `level.dat` here. Full detail: `data/load_and_121.md`.

### 5.0 Pack disabled in the import (first blocker, A12)

A12's imported-world audit: `Enabled=[vanilla,file/bukkit,paper]`, map packs not in the list; `datapacks_on_disk=Pits`; `datapacks_enabled_source="vanilla,Fabric Mods,file/Pits"`. The pack is present but not loaded. No load/tick function runs; the Start sign's `tag @a add starting` still executes, but nothing reads it. **[H]** / **[H]** direct read. Direct read of the review world: all **9 pack files are byte-identical to the source by SHA-1**, imported `level.dat` DataVersion **3955**, `GameType 0` (survival), `allowCommands 1`, spawn 15,-2,36; source `GameType 1` (creative). Enabling is a prerequisite for every later check (`/datapack enable "file/Pits"` + `/reload`, or add to `level.dat`; not done here).

### 5.1 Fatal: data pack directory rename

1.21 renamed data pack directories to singular. The 1.21.1 jar uses `data/minecraft/advancement`, `loot_table`, `recipe`, `structure`, `tags/block`, `tags/entity_type`; `ServerFunctionLibrary` looks for **`function`** (literal in the class) and `.mcfunction`. Pits has:
- `data/tgttos/functions/*.mcfunction` → 1.21 looks in `data/tgttos/function/` → **no functions exist**.
- `data/minecraft/tags/functions/load.json` / `tick.json` → 1.21 looks in `data/minecraft/tags/function/` → **no load/tick tags**.

Result: even if the pack is enabled, it is **inert** — no objectives, gamerules, kit, countdown, respawn, finish or reset. The Start sign still runs `tag @a add starting`, but no function reads it. The barrier cage never opens except in creative. **[H]** paths; **[H/M]** inert result.

### 5.2 Fatal for the kit: item NBT → components

1.20.5+ removed item NBT in commands. `item replace … with iron_pickaxe{CanDestroy:…,Unbreakable:1b,Enchantments:…}` is a parse error in 1.21.1; the item parser expects `[` components (`DataComponentPatch`, `SYNTAX_START_COMPONENTS`). All **21** `item replace` commands (1 pickaxe + 2 × 10 colours) fail → **no pickaxe, no concrete** → no bridging. The rest of the loop still runs (start/finish/timer), but the map cannot be completed without creative. **[H]**

1.21 forms (documentation only; not applied): `iron_pickaxe[unbreakable={},enchantments={levels:{"minecraft:efficiency":2}},can_break={blocks:[…]}]`; `red_concrete[can_place_on={blocks:[…]}]`; the fully explicit predicate form is `can_break={predicates:[…]}` / `can_place_on={predicates:[…]}`. Saved playerdata items are DataFixed to components on world upgrade; command strings are not. **[H]** DataFixer classes; **[M]** exact predicate shape.

### 5.3 Warning: `pack_format` mismatch

`pack_format: 10` vs 48 → the pack is marked incompatible. In the imported world the pack is not enabled at all (§5.0), so this is the next check after enabling; if it were enabled, 1.21 would normally still load it with a warning, but §5.1 makes it contribute nothing. **[H]** mismatch; **[M]** “still loaded”.

### 5.4 Other syntax checks

Valid in 1.21.1: `scoreboard objectives add … minecraft.custom:minecraft.damage_dealt_resisted` (stat exists), `trigger`, `effect give … resistance`, `fill … replace #minecraft:mineable/pickaxe`, `summon armor_stand {…}`, `data get entity @s Pos[1]`, `tp … -90 0`, `title`/`tellraw` JSON, `setworldspawn`, `spawnpoint`, `clear`, `gamemode`, `difficulty`, `gamerule freezeDamage`. Sign `Text1..4` is DataFixed to `front_text`; the click event should survive. `clickEvent`/leading-slash `run_command` are still accepted in 1.21.1 but changed in 1.21.5+. **[H/M]** per row in `data/datapack.md` §11.4.

### 5.5 Expected failure chain

0. World opens at 3955; `file/Pits` is **not enabled** (A12) → no pack at all.
1. If enabled, `pack_format 10` is flagged incompatible (warning).
2. No load/tick functions run (old `functions` / `tags/functions` paths) → static build, cage closed.
3. Even with folders renamed, 21 item commands fail → no kit.
4. Even with the kit fixed: blank-line sign click, first-finish timer freeze, 7 s global reset, no checkpoints.
5. The map is salvageable only with a port; **none of that is done here** (research only).

**Full 1.21.1 requirement list, exact import state, and the 21 old item-NBT lines:** `data/load_and_121.md` and `data/item_nbt_lines.csv`. The requirements are: enable `file/Pits`; set `pack_format` 48; rename `functions`→`function`; rename `tags/functions`→`tags/function`; convert the 21 item-NBT lines to components; verify the sign after DataFix; reconcile existing objectives; runtime-test one player. No edit or port was applied.

---

## 6. Comparison with McmTgttos (read-only, architecture level)

`McmTgttos` (Paper 1.21.1 plugin) is a Java round game: 8 generated maps, 5 rounds, 300 s rounds, 15 s pre-round, barrier start strip, 3-2-1/GO, 300 s clock with half-finished time skip, goal chickens in a row, spectator confinement, void/hazard respawn with fresh kit, infinite 64-block concrete refill, 25 snowballs, scoring/team bonus/karma, map features (movers, toggles, traps, checkpoints, refills, depots, bonus regions), pre-built pastes with chunk tickets. Source: `game-plugins/Tgttos/README.md`.

| Dimension | Pits (datapack) | McmTgttos |
|---|---|---|
| Runtime | 6 tick functions, no command blocks | Java plugin + event/tick system |
| Start | click sign line 1 → 7 s countdown; barrier cage with 3 exits | 15 s pre-round in a start strip; barrier line; 3-2-1/GO |
| Course | 3 islands, 2 × 12-block pits, chicken pool; no checkpoints | 5 maps, generators, checkpoints/features on some |
| Kit | unbreakable pickaxe + 1 concrete/tick (offhand) | 64 concrete (infinite refill) + pickaxe + 25 snowballs |
| Finish | any of 20 chickens in a 7×34 pool; `damage_dealt_resisted` | row of goal chickens; `PrePlayerAttackEntityEvent`; placement + “Whack!” |
| Timer | per-player stopwatch, starts at GO, stops for all at first finish | global 300 s clock + time skip; boss bar/sidebar |
| Respawn | start only; timer continues | last checkpoint/start; fresh kit; pun; timer continues |
| Reset | 7 s after first finish, all players | per-round cleanup, next map, pre-build |
| Multiplayer | first finisher freezes all timers and resets all | per-player finish state, spectators, team scoring |
| 1.21.1 | inert (folder rename + item NBT) | native |
| Best idea to borrow | chicken-pool finish; 3-exit cage; road paint; layered glass void | full round/scoring/checkpoint architecture |

**Pits does better:** a memorable finish (long pool of chickens, enter anywhere); a very readable 3-island/2-pit silhouette; a simple 1-concrete/tick bridge loop that never runs out; a scenic park counterweight; a cheap armor-stand cleanup that avoids a plugin; a one-click start for a group.
**Pits does worse:** no checkpoints; first-finish global timer/reset; late joiners get 0:00; no scoring/rounds/teams; no spectator box; no chunk tickets for chickens; join gamemode is not adventure (source creative, import survival); no anti-skip; 1.21-inert.

---

## 7. Comparison with Badlands / Industry (architecture only)

Source-only comparison; full route/build reports are owned by A7/A8, so this is deliberately shallow.

| | Badlands | Industry | **Pits** |
|---|---|---|---|
| Version / pack_format | 1.19.2 / 10 | 1.19.1 / 10 | **1.19.1 / 10** |
| Spawn | 10,-42,31 | 73,-33,11 | **15,-2,36** |
| Functions | 2 (`load`, `loop`) | 7 (`load`, `loop`, `block_reset`, `color`, `gamerules`, `respawn`, `start_timer`) | **7, but no `block_reset`** |
| Start cage | in `loop` | barrier box x63..84, y-33..-29, z9..13 | barrier box x13..17, z25..46 |
| Tick tag | `tgttos:loop` only | loop, color, start_timer, gamerules, respawn | same as Industry |
| Finish | `hit_chicken` custom stat | same | same |
| Block reset | none separate | `block_reset.mcfunction` rebuilds/clones external banners | armor-stand cleanup, no separate function |
| 1.21 risk | same folder/NBT failures | same | same |

**Lineage evidence [H/M]:** Industry and Pits share exact coordinates for the external red/white banner panels at z=83 (x29..53, 61..85, 93..117, y -61..-23) and Industry's `block_reset.mcfunction` rebuilds/clones those exact coords. Industry's spawn is 73,-33,11, inside the barrier box x63..84, y -33..-29, z9..13 — the same box that survives, sealed and inert, inside a Pits building. Pits moved the start to 15,-2,36 and built a new course above; the legacy cage and banners are inherited. This is architecture-level derivation, not proof of intent. **[H]** coordinates; **[M]** derivation. Badlands has a different spawn and only two functions; it is a separate/older base.

---

## 8. What this map does better / worse than the others

### Better

1. **Finish landmark.** The 7×34 sunken pool of 20 chickens is more memorable than a flat row; players can enter anywhere along 34 blocks, so a crowd does not fight over one gate. **[H]** entities/coords; **[M]** feel.
2. **Silhouette.** Three long islands, two black pits, a bright park and a dark city: readable from any angle. The `course_iso_angle0.png` shows the whole route in one frame. **[H]** render.
3. **Bridge loop.** One concrete per tick, refilled forever, with a `CanPlaceOn` whitelist. It is impossible to run out, but placement is constrained to existing surfaces/own concrete; the bridge is the skill. **[H]** commands; **[M]** play.
4. **Cheap cleanup.** Armor-stand anchors + local 31³ fills remove placed concrete without a plugin or a full map reset. **[H]**
5. **One-click group start.** The sign tags all players; no per-player lobby. **[H]**
6. **Lighting trick.** Black-glass ceiling + light-blue wool wall + skylight 15 = a night look that is actually day-lit; no torches. **[H]** light arrays.

### Worse

1. **No checkpoints.** A fall from either bridge returns to the start; the timer keeps running. On a 45–50-block course this is harsh. **[H]**
2. **First finisher controls everyone.** `tag @a remove playing` freezes all timers; `finished=140` teleports/resets all players. Not a multiplayer race. **[H]**
3. **Late joiners.** No `starting`/`playing`; they can finish with 00:00:00. **[H/M]**
4. **Start sign usability.** Click is on blank line 1, not visible “Start” line 2. **[H/M]**
5. **Gamemode on join is not adventure.** Source `level.dat` GameType=1 (creative); imported `level.dat` GameType=0 (survival), MV `SURVIVAL`; the pack only sets adventure at reset. In the source a new player can fly/break. In the import they play survival: the pickaxe is restricted to concrete, but **bare hands can still break wool/glass/terrain**, the light-blue wool wall is 1 thick, and MV `pvp=true`; the datapack has no damage guard. **[H]**
6. **Chicken persistence.** `PersistenceRequired=0`, no chunk tickets; 20 can despawn if the finish chunks unload. **[H/M]**
7. **1.21-inert.** Folder rename + item NBT; no functions/kit on the live 1.21.1 server. **[H]**
8. **Shell-heavy.** 43.9% of blocks are shell/void; the gameplay area is small relative to the build. **[H]**
9. **No anti-skip.** Direct 33-block bridge from Road A to the finish, or pillar to the roofs. **[H]** geometry; **[M]** viability.
10. **No spectator box.** Finishers become spectators in place and can fly out through the shell. **[H/M]**

---

## 9. Open questions

| # | Question | Why it matters | Confidence |
|---|---|---|---|
| Q1 | Does a click on the visible “Start” line do anything, or only the blank line 1? | Start usability; NBT says only line 1 has the click event | H NBT / M client |
| Q2 | Does 1.21.1 auto-load a `pack_format 10` pack from an existing world, or disable it? | Failure mode (inert vs disabled) | M |
| Q3 | Is the park → north-roof route actually continuous and faster? | Possible skip/alternate route | M/L |
| Q4 | Does `store result` truncate or round `Pos[1]`? | Exact kill plane (-41 vs -41.9) | H command / M cast |
| Q5 | Will the 20 non-persistent chickens survive on the live server's view distance? | Finish targets | H NBT / M server |
| Q6 | Are the external banners visible from spectator flight outside the shell? | Presentation only | M |
| Q7 | Did the maker intend the legacy Industry cage as a spectator room? | It is sealed inside solid wool; no entrance | M/L |

---

## 10. Evidence index

| Path | What it shows | Opened |
|---|---|---|
| `data/world_scan.json` | extents, 3,254 chunks, 51 occupied, 164,145 blocks, 63 states, 1 sign, 0 containers, 20 entities | — |
| `data/blocks.csv` | every block state, count, share, role/use, first coord | — |
| `data/entities.csv` | 20 chickens, positions, effects, persistence | — |
| `data/signs.csv` | the one sign, its lines and click event | — |
| `data/checkpoints.csv` | start/finish gates and pits; **no checkpoints** | — |
| `data/command_blocks.csv` | header only; **0 command blocks** | — |
| `data/functions.csv`, `data/datapack.md`, `data/commands_export.txt` | all 6 functions line-by-line, objectives, tags, bugs, 1.21 risks | — |
| `data/load_and_121.md` | source vs imported `level.dat`, exact load/build path, 1.21.1 requirement list, 21-line item-NBT table | — |
| `data/item_nbt_lines.csv` | exact 21 old `item replace` commands + compact 1.21 component forms | — |
| `data/import_state.json` | direct read of source + review-server `level.dat`, DataPacks lists, GameType, and full SHA-1 of all 9 pack files (byte-identical) | — |
| `data/scoreboards.csv`, `data/tags.csv`, `data/timeline.csv` | objectives, load/tick tags, event timeline | — |
| `data/route.md`, `data/palette.md` | leg-by-leg route; palette/lighting/build techniques | — |
| `renders/route_schematic.png`, `frames/09_route_schematic.png` | exact top-down route diagram | yes |
| `renders/course_top_labeled.png`, `frames/02_course_top_labeled.png` | labelled top view of the 3 islands/2 pits | yes |
| `renders/course_iso_angle0.png`, `course_iso_angle2.png`, `frames/01_world_overview.png` | 2 iso views | yes |
| `renders/world_top.png`, `world_iso_angle0/2.png` | full open shell / city massing | yes |
| `renders/start_top_labeled.png`, `start_cage_top.png`, `start_iso.png`, `frames/03/04` | start cage, sign, exits, park | yes |
| `renders/finish_top_labeled.png`, `finish_iso.png`, `finish_pool_iso_labeled.png`, `frames/07/08` | pool, lip, 20 chicken positions | yes |
| `renders/pit_section.png`, `frames/06` | vertical section: slabs, towers, glass layers, kill plane | yes |
| `renders/road_markings_detail.png`, `frames/05` | yellow edges, quartz 2:1 dashes | yes |
| `renders/park_detail.png`, `frames/10` | park/tree, snow, stairs | yes |

See `REFERENCE_IMAGES.md` for the full image → source → what it shows → what to take table.

---

## 11. Scope and non-claims

- This is a **read-only forensic dossier**. No game/plugin code, no build/paste, no registry edits, no server restarts, no writes outside this folder.
- The live `mcc_tgttos_pits` world was not queried via RCON (server-import owns live 1.21 behaviour); all findings are from the source world and the local Paper 1.21.1 jar. **[H]** source; **[M]** live.
- Badlands and Industry were read only at the architecture level (file/function inventory, spawn, shared coordinates); their full route/build reports belong to A7/A8. No Ace Race map or video was analyzed.
- The 1.21 port is **not** implemented; §5 documents what would fail, not a fix plan.
