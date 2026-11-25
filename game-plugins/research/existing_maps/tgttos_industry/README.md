# TGTTOS Industry — deep research dossier (A8)

**Map:** `TGTTOS Industry - By Mickey_Kat` (world name `TGTTOS Industry - Practise map`)
**Source archive:** `C:\Users\LeonS\Downloads\EXISTING MCC RECREATIONS\tgttos-industry-by-mickey-kat.zip` (2,214,645 bytes)
**Live review world:** `mcc_tgttos_industry` (Multiverse), Paper 1.21.1
**Datapack:** `Industry` (namespace `tgttos`), 7 functions + 2 function tags + `pack.mcmeta`, `pack_format: 10`
**Status:** research complete; import currently broken on 1.21.1 (A12 finding confirmed below). No edits, ports, builds or server changes were made.

Evidence tags: **[H]** = directly read from the file/world/NBT or seen in a render; **[M]** = inference from two independent observations; **[L]** = estimate or guess. Untagged prose is context.

---

## 0. What was examined

| Source | Read | Result |
|---|---|---|
| Source archive / extracted world | `level.dat`, `region/*.mca`, `entities/*.mca`, `data/scoreboard.dat`, `playerdata/*.dat`, all 10 datapack files | H |
| Live review world `...\scratchpad\review\mcc_tgttos_industry` | `level.dat`, `datapacks/Industry/*`, `region/r.0.0.mca` sign, `entities` chicken NBT | H |
| Review server | `version_history.json`, `server.properties`, `spigot.yml`, `plugins/Multiverse-Core/worlds.yml` | H |
| Current game code | `game-plugins/Tgttos` (`README.md`, `SPEC_v2.md`, `TgMapData.java`, `Kit.java`, `FinishRow.java`, `TgttosGame.java`, `GameListener.java`) | H |
| Sibling Mickey_Kat packs | Badlands `tgttos` pack (2 functions), Pits `Pits` pack (6 functions), read at file-list/tick-tag level only | H |
| Renders | full world dump 122,276 blocks → `render_preview.py`; custom labelled plan/profile | H |

No command blocks, chests, shulkers, item frames, armor stands (at rest), or other containers exist to open. The only block entity is the start sign. H.

---

## 1. Import state — A12 finding confirmed against the source archive

A12 reported that the imported Industry world has the `Industry` datapack disabled. This is **confirmed** and is independent of the 1.21 syntax problems.

### 1.1 Source archive

- The zip root is `TGTTOS Industry - By Mickey_Kat/`; `level.dat` is 1.19.1, DataVersion **3117**, LevelName `TGTTOS Industry - Practise map`. [H]
- Source `level.dat` `DataPacks`: `Enabled: [vanilla, Fabric Mods, file/Industry]`, `Disabled: []`. The Industry pack **is enabled in the archive**. [H]
- Archive datapack tree: `datapacks/Industry/pack.mcmeta` (`pack_format: 10`), `data/tgttos/functions/` (7 `.mcfunction`), `data/minecraft/tags/functions/` (`load.json`, `tick.json`). All plural. [H]
- The extracted copy is byte-identical to the archive's datapack (SHA-256 on all 10 files). [H]

### 1.2 Live world

- `mcc_tgttos_industry` is 1.21.1, DataVersion **3955**, LevelName `mcc_tgttos_industry`, spawn 73,-33,11, GameType 0. [H]
- Live `level.dat` `DataPacks`: `Enabled: [vanilla, file/bukkit, paper]`, `Disabled: [bundle, trade_rebalance]`. **`file/Industry` is in neither list**, so the pack is present on disk but not selected/loaded. [H]
- `mcc_tgttos_industry/datapacks/Industry/` still contains the **same 10 files**, byte-identical to the archive, still with plural `functions/` and `tags/functions/`, still `pack_format: 10`. [H]
- Multiverse registers the world as `minecraft:mcc_tgttos_industry` in `plugins/Multiverse-Core/worlds.yml`. [H]
- The review server is Paper 1.21.1 (`1.21.1-133-3cb8529`), port 25580, `simulation-distance=6`, `view-distance=8`, `enable-command-block=true`, `initial-enabled-packs=vanilla`. [H]

**Exact load path:** zip → extracted source (`%TEMP%\opencode\mcc_maps\...`) → review source copy `review\tgttos_industry` (1.19.1) → Multiverse live world `review\mcc_tgttos_industry` (upgraded to 1.21.1, DataPacks rebuilt without Industry). There is **no paste/build path into `McmTgttos`**: the plugin is a pure-Java generator and does not load imported worlds/datapacks. [H]

**Why the pack does not run:** (1) not enabled in the live `level.dat`; (2) 1.21 expects `function/` and `tags/function/`, not `functions/` and `tags/functions/`; (3) `pack_format` is 10, not 48; (4) 21 `item replace` lines use pre-1.20.5 item NBT, which is a parse error on 1.21.1. Any one of these is enough; all four are present. [H for the files; H for the 1.21 rename/format/component changes]

Full detail, exact line numbers and the fix checklist: **`data/LOAD_AND_1_21.md`**.

### 1.3 What did survive the upgrade

- The sign at 73,-32,10 migrated to `front_text.messages` / `front_text.color` / `front_text.has_glowing_text` + `back_text`; the clickEvent JSON is preserved on `front_text.messages[0]` (empty text). [H, live chunk]
- 24 chickens survived at y=-39 with `PersistenceRequired:0` and `ActiveEffects` id 11 (resistance) amplifier 5 duration 19. [H, live entity NBT]
- The world blocks are intact: the live scan is 122,276 solid blocks with the same bbox (x11..141, y-61..11, z-5..99); the live world exposes 140 distinct block states vs 91 in 1.19.1 (1.21 block-state serialisation), but the block count and geometry are unchanged. [H, full live scan]

---

## 2. Executive summary

1. **The map is a compact bridging course in a covered industrial shed, not a long TGTTOS track.** 122,276 solid blocks in a 131×73×105 envelope; 4,464 chunks, but only **54 chunk columns contain any block** (98.8% empty). [H]
2. **Three conveyor tables at y=-35** (DECK 1 z8..24, DECK 2 z52..62, DECK 3 z88..98, x26..120) are separated by **two real voids**: GAP A 27 deep (z25..51) and GAP B 25 deep (z63..87). The only crossing is the auto-refilled 1-block concrete (or a 12–15-block tower). [H]
3. **GAP A is a lane choice:** three 1×25×39 glass fins at x55/73/91 split the 95-wide deck into four lanes; the spawn x73 is on the middle fin, so the player must pick left/right immediately. [H]
4. **GAP B is a forced weave:** three 1-thick concrete curtains at z66 (red), z74 (yellow), z83 (red), each 25–26 wide × 39 high with 6–7-wide x-gaps that alternate. No single x lane passes all three; the minimum is one shift. [H]
5. **The finish is a hidden 31×11 basin** at x58..88, z88..98, floor y=-40, with 24 scattered chickens at y=-39. You step over a 1-block acacia lip and drop 4; there is no finish row, arch, sign, hay, or name tag. [H]
6. **The datapack is a single-player practice timer**, not a game framework: 7-second countdown, per-player action-bar timer (centisecond display), chicken-punch finish via the `damage_dealt_resisted` stat, spectator, 7-second reset. No checkpoints, no teams, no scoring, no round manager. [H]
7. **Multiplayer is broken by design:** one finish removes `playing` from **all** players, broadcasts the finisher's time to all, and the 140-tick reset teleports **all** players; one sign click tags **all** players `starting`. [H from `@a` targets]
8. **The start sign works, but its event is on an invisible line:** the clickEvent is on `Text1` (empty); the visible `Start` is `Text2` with no clickEvent. On signs, a right-click runs the click events of **all four lines** (`SignBlockEntity` loops the four messages), so the empty-line event fires when the sign is clicked. [H for the NBT; H for the 1.21.1 sign code; M for 1.19.1 in-game confirmation]
9. **The colour selector has no in-world trigger:** `color.mcfunction` enables `/trigger color` and prints a clickable menu, but the only sign/command in the world starts the game. The saved player has the `cyan` tag, so the system was used via `/trigger`. [H]
10. **`block_reset` rebuilds 8 of 10 curtain segments, not all 10:** it rebuilds z83 (3), z74 (4, via a 58-wide pattern clone), and z66 only x29..53 (1). z66 x61..85 and x93..117 are never rebuilt. [H commands]
11. **The kit is one concrete block + an Efficiency II unbreakable iron pickaxe**, refilled every tick. The pickaxe can break only concrete; the concrete can be placed on the table/glass/trim surfaces. This is a slow, precise bridging kit, not the 64-block stack used by `McmTgttos`. [H]
12. **On 1.21.1 the map is dead without a repack:** datapack not enabled + plural folders + format 10 + 21 item-NBT lines. [H]

---

## 3. Datapack — unpacked and documented

Copy of the pack: `data/datapack/`. Every non-empty line with a 1.21 risk tag: `data/datapack_functions.csv` (264 rows). Timeline: `data/timeline.csv`.

### 3.1 File tree and tags

```
Industry/
├─ pack.mcmeta                         pack_format 10, "Made by Mickey_Kat"
└─ data/
   ├─ minecraft/tags/functions/
   │  ├─ load.json                     ["tgttos:load"]
   │  └─ tick.json                     ["tgttos:loop","tgttos:color","tgttos:start_timer","tgttos:gamerules","tgttos:respawn"]
   └─ tgttos/functions/
      ├─ load.mcfunction               23 lines / 20 commands: 14 objectives + 6 score initialisers
      ├─ loop.mcfunction               127 lines / 87 commands: finish, cleanup, timer, kit, HUD
      ├─ color.mcfunction              131 lines / 113 commands: /trigger color menu + 10 tag sets
      ├─ start_timer.mcfunction        25 lines / 15 commands: 140-tick countdown
      ├─ gamerules.mcfunction          16 lines / 14 commands
      ├─ respawn.mcfunction            3 lines / 2 commands: y<=-58 -> tp
      └─ block_reset.mcfunction        20 lines / 13 commands: paint-curtain rebuild
```

[H, file tree and line counts]

### 3.2 Scoreboards

`load.mcfunction` creates 14 objectives:

| Objective | Criterion | Use |
|---|---|---|
| `hit_chicken` | `minecraft.custom:minecraft.damage_dealt_resisted` | finish detector: a hit fully resisted by Resistance V increments the stat |
| `playing` | dummy | timer runs while present |
| `countdown` | dummy | start countdown |
| `spawntime` | dummy | marker spawn cadence |
| `playtimeMi1`, `playtimeMi2` | dummy | minutes (ones, tens) |
| `playtimeSe1`, `playtimeSe2` | dummy | seconds (ones, tens) |
| `playtimeMs1`, `playtimeMs2` | dummy | 10 ms units (centisecond display) |
| `finished` | dummy | per-player finish counter/reset clock |
| `respawn` | dummy | last stored Y |
| `color` | trigger | opens the colour menu |
| `selectColor` | dummy | chosen colour 1..10 |

The world `data/scoreboard.dat` additionally has `playtimeS` and `playtimeT`, stale objectives not created by the current `load` (older version residue). [H]

`load` also adds 0 to the six playtime scores for every player so the sidebar/actionbar never shows a blank. [H]

### 3.3 Tags / colours

10 player tags: `red`, `orange`, `yellow`, `lime`, `green`, `cyan`, `aqua`, `blue`, `purple`, `pink`. `selectColor` 1..10 maps to them. The `aqua` tag receives **light_blue_concrete**, not cyan; `cyan` receives cyan. [H]

The saved player `Mickey_Kat` has `Tags:[cyan]`, `selectColor:6`, and a cyan concrete in the offhand. [H]

### 3.4 Start and countdown

- The start sign (`oak_wall_sign[facing=south]` at 73,-32,10) has four lines:
  - `Text1`: `{"clickEvent":{"action":"run_command","value":"tag @a add starting"},"text":""}`
  - `Text2`: `{"text":"Start","bold":true,"color":"green"}`
  - `Text3`, `Text4`: empty.
  - `Color: black`, `GlowingText: 0`. [H]
- The clickEvent is on the **empty** first line; the visible "Start" line has no clickEvent of its own. On signs, a right-click executes the click events of all four lines (the 1.21.1 `SignBlockEntity` loops `getMessages()` and runs every `RUN_COMMAND` event), so the empty-line event fires when the sign is used. [H for NBT; H for 1.21.1 code; M for 1.19.1 in-game confirmation] The click command is `tag @a add starting`, i.e. **all players**, not the clicker. [H]
- `start_timer` increments `countdown` for every `starting` player once per tick:
  - tick 40 (2.0 s): title "Starting in" + subtitle "5"
  - 60/80/100/120: "4"/"3"/"2"/"1"
  - tick 140 (7.0 s): "GO!!", `tag @a add playing`, `fill 63 -33 13 84 -29 9 air replace barrier`, `tag @a remove starting`, `countdown=0`. [H]
- There is no 15-second pre-round and no start-strip confinement; the barrier box is the only hold. [H]

### 3.5 Timer

- `loop` adds **5** to `playtimeMs1` per tick for `playing` players. At 20 ticks (1 s) the pair `Ms2 Ms1` rolls 00..95→00 and `Se1` increments. The displayed `MM:SS:CC` is therefore **centiseconds**, although the objectives are named `Ms1/Ms2`. [H]
- Carry chain: `Ms1>=10` → `Ms2++`; `Ms2>=10` → `Se1++`; `Se1>=10` → `Se2++`; `Se2>=6` → `Mi1++`; `Mi1>=10` → `Mi2++`. [H]
- The HUD is a per-player `title @s actionbar` coloured by the player's tag. There is no boss bar and no shared sidebar. [H]

### 3.6 Finish, cleanup, reset

- Finish detection: `hit_chicken>=1` → `tag @s add finished`; `finished` players become spectator, `finished` increments each tick, and for `finished=1..5` a title "Finished" + subtitle "Time: MM:SS:CC" is shown. [H]
- `tag @a remove playing` is executed as the finisher: it removes `playing` from **every** player, stopping all timers. [H]
- Every 20 ticks while `playing`, an invisible armor stand tagged `remove_block` is summoned 10 blocks above the player. [H]
- At `finished>=60` (3.0 s), for **every** `remove_block` marker, 10 `fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace <colour>_concrete` commands remove red/orange/yellow/lime/green/cyan/light_blue/blue/purple/pink concrete in a 31×31×31 box. Then all markers are killed. [H]
- At `finished=65` (3.25 s), `function tgttos:block_reset` runs once. [H]
- At `finished>=140` (7.0 s), four `fill ... barrier` commands rebuild the start-box walls, `tp @a 73 -33 11`, `tag @a remove finished`, `gamemode adventure @a`, `hit_chicken=0`, `clear @a`, all six playtime scores reset, `finished=0`. Again `@a` targets all players. [H]
- The 7-second reset means a finisher watches the pen for 7 s, then is back in the box in adventure. [H]

### 3.7 Respawn

```
execute as @a store result score @s respawn run data get entity @s Pos[1]
execute as @a[scores={respawn=..-58}] at @s run tp @s 73 -33 11
```

- No timer reset, no inventory reset (the kit is refilled every tick), no barrier rebuild, no gamemode change. [H]
- `data get` returns a double; `store result score` truncates/floors, so the effective threshold is around y<=-57.9 rather than exactly -58. [M]
- The respawn point is the single spawn 73,-33,11, not a random start spawn. [H]

### 3.8 Gamerules

`gamerules.mcfunction` runs every tick and sets: `difficulty peaceful`, `sendCommandFeedback false`, `fallDamage false`, `commandBlockOutput false`, `doEntityDrops false`, `disableRaids true`, `doFireTick false`, `doDaylightCycle false`, `doWeatherCycle false`, `doMobSpawning false`, `fireDamage false`, `showDeathMessages false`, `freezeDamage false`, `keepInventory true`. [H]
Level.dat also has `doTileDrops false`, `randomTickSpeed 0`, `naturalRegeneration true`, `mobGriefing true`, `doImmediateRespawn false`. [H]

### 3.9 Kit

- Every tick: `item replace entity @a hotbar.0 with iron_pickaxe{CanDestroy:[red, blue, lime, pink, green, orange, purple, yellow, light_blue, cyan, black, white concrete],Unbreakable:1b,Enchantments:[{id:"minecraft:efficiency",lvl:2s}]} 1`. The list has 12 of the 16 concrete colours (not magenta, gray, light_gray, brown). [H]
- Every tick: for each colour tag, two `item replace ... weapon.offhand with <colour>_concrete{CanPlaceOn:[...]} 2` then `... 1`; the net result is **1 block** in the offhand, refilled next tick. [H]
- `CanPlaceOn` includes the map surfaces: the player's concrete, light_blue_stained_glass, blackstone, white/red/black/yellow concrete, blackstone_stairs, gray_wool, polished_andesite, smooth_stone, smooth_stone_slab, acacia_planks, cobblestone_wall, yellow_wool, black_wool, red_sandstone_stairs, red_sandstone, red_sandstone_wall, iron_trapdoor, acacia_fence, oak_fence. [H]
- No snowballs, no food, no armour, no ender pearls, no elytra. [H]
- `keepInventory=true` + `clear @a` at reset means the kit is re-created, not carried. [H]

### 3.10 `block_reset` — exact coverage

`block_reset.mcfunction` (13 commands, lines 5–17):

1. Fill red x93..117 at z83, y -61..-35 and -25..-23; fill the window band y -34..-26 with white concrete (`replace #minecraft:mineable/pickaxe`, then `replace air`). [H]
2. Clone that 25×39×1 wall to x61..85 and x29..53 at z83. → all 3 red z83 segments. [H]
3. Fill yellow x109..134 at z74, y -61..-35 and -25..-23; fill the window band with black concrete. [H]
4. Clone x109..134 (26 wide) to x76..101. [H]
5. Clone the **58-wide** source x77..134 (which contains yellow x77..101, the 7-wide gap x102..108, yellow x109..134) to x12..69. This rebuilds yellow x12..36, the gap x37..43, and yellow x44..69 in one pass. [H]
6. Clone x29..53 from z83 to z66. → only the west red z66 segment. [H]

**Coverage: 8 of 10 segments** — z83 red ×3, z74 yellow ×4, z66 red ×1. **Missing: z66 red x61..85 and x93..117.** [H]

Two notes:
- The z83 and z74 rebuilds are self-contained: z83 is filled fresh before its clones; z74 fills x109..134 fresh, clones it to x76..101, then clones the now-fresh x77..134 strip to x12..69 (which also reproduces the x37..43 gap). The cleanup cannot poison those sources. [H]
- The cleanup does **not** remove black/white/gray concrete (the 10-colour list excludes them), so the machine panels and the curtain window borders survive. The red/yellow walls are the only map concrete in the cleanup list, which is why `block_reset` only restores red/yellow. [H]

### 3.11 Datapack bugs and design limits (summary)

| Finding | Grade |
|---|---|
| Start clickEvent on empty `Text1`; visible `Text2` "Start" has none; sign-click runs all four line events, so it works | H (NBT + 1.21.1 code); M (1.19.1 in-game) |
| `tag @a add starting` starts everyone, not the clicker | H |
| Colour menu has no in-world trigger; only `/trigger color` (no sign, no plate, no command block) | H |
| One finish stops every timer (`tag @a remove playing`) | H |
| Finish title/time/reset teleport target all players | H |
| Single spawn; no 40-spawn strip; all players stack at 73,-33,11 | H |
| No checkpoints, no hazard regions, no teams, no scoring, no round manager | H |
| Chicken finish is a 24-chicken basin, not a visible row/landmark | H |
| Chickens `PersistenceRequired:0`; no chunk ticket; 24 only, no respawn if killed | H (NBT); M (despawn risk) |
| `block_reset` misses z66 centre/east (2 of 10 segments) | H (commands) |
| Timer is centiseconds labelled `Ms`; no boss bar/sidebar | H |
| `gamerules` and `color` run every tick; harmless but noisy | H |
| 1.19 pack on 1.21.1 is dead without repack | H |

---

## 4. World anatomy and route

Full leg-by-leg: **`data/route.md`**. Labelled plan: **`renders/route_top_labelled.png`**. Centre profile: **`renders/profile_x73.png`**.

### 4.1 Envelope and mass

| Property | Value | Grade |
|---|---|---|
| Solid blocks | 122,276 | H |
| Block states / base ids | 91 / 39 | H |
| Bounding box | x11..141, y=-61..11, z=-5..99 | H |
| Content bbox | x12..134, y=-61..11, z=-5..99 | H |
| Chunks in region files | 4,464 | H |
| Chunk columns with any block | 54 (x0..8, z-1..6); 98.8% of chunks empty | H |
| Entity chunks | 6 | H |
| Block entities | 1 (the sign) | H |
| Entities | 24 chickens; 0 armor stands, item frames, etc. | H |
| Command blocks | 0 | H |
| World version (source) | 1.19.1 / DataVersion 3117 | H |
| World version (live) | 1.21.1 / DataVersion 3955 | H |
| Spawn / angle | 73,-33,11 / 0 (south) | H |

### 4.2 Vertical stack

- **Foundation:** each table is a solid 27-block stack from y=-61 to -35: a 21-block plinth (y=-61..-41) and a 6-block deck body (y=-40..-35); the walkable surface is y=-35, stand y=-34. The plinth footprint is x27..119; the deck lip is x26..120. [H]
- **DECK 1:** z8..24 (17 deep). **DECK 2:** z52..62 (11 deep). **DECK 3:** z88..98 (11 deep, split by the pen). All 95 wide. [H]
- **GAP A:** z25..51 (27 deep); glass fins z26..50; catwalks y=-20. [H]
- **GAP B:** z63..87 (25 deep); curtains z66/74/83; catwalks y=-20. [H]
- **Roof:** blackstone at y=8–9, x37..109, z-5..99; glass skylight y=9, x58..88, z-5..99; white concrete roof units y=10. [H]
- **Machine towers:** x40..42 and x104..106, from the deck to the roof; white control panels, sea-lantern buttons, red sandstone frames, glass panes. [H]
- **Start box:** x63..84, z9..13, y=-33..-29, quartz floor at y=-34, barrier walls. [H]
- **Finish pen:** x58..88, z88..98, floor y=-40; blackstone side walls x57/x89, acacia end walls z88/z98 (4 high, top -36). [H]

### 4.3 Start, decks, gaps, finish — exact coordinates

| Feature | Coordinates | Notes | Grade |
|---|---|---|---|
| Spawn | 73.5, -33.0, 11.37, yaw 0 | single spawn | H |
| Start box | x63..84, z9..13, y=-33..-29 | 184 barriers; quartz floor y=-34 | H |
| Sign | 73,-32,10, oak wall sign facing south | clickEvent on empty line 1 | H |
| DECK 1 | x26..120, z8..24, top y=-35 | 95×17 | H |
| Machine heads | x40..42 / x104..106, z8..22 / z88..98 | rise to roof | H |
| GAP A | z25..51 | 27 deep; no floor | H |
| Glass fins | x55, 73, 91; z26..50; y=-61..-23 | 25×39×1; 975 blocks each | H |
| Fin catwalks | x54..56 / 72..74 / 90..92; z28..48; y=-20 | 3 wide; 15 above deck | H |
| DECK 2 | x26..120, z52..62, top y=-35 | 95×11 | H |
| GAP B | z63..87 | 25 deep; no floor | H |
| Curtain z66 (red) | x29..53, 61..85, 93..117; y=-61..-23 | gaps 26..28, 54..60, 86..92, 118..120 | H |
| Curtain z74 (yellow) | x12..36, 44..69, 76..101, 109..134; y=-61..-23 | gaps 37..43, 70..75 | H |
| Curtain z83 (red) | x29..53, 61..85, 93..117; y=-61..-23 | same gaps as z66 | H |
| Curtain catwalks | z65..67, 73..75, 82..84; y=-20 | x segments 31..51/63..83/95..115 and 15..35/47..67/79..99/112..132 | H |
| DECK 3 | x26..57 and x89..120, z88..98, top y=-35 | split by pen | H |
| Finish pen | x58..88, z88..98, floor y=-40 | 31×11; 4–5-high walls | H |
| Chickens | 24 at y=-39, x61.84..86.80, z89.50..97.50 | no name tags, AI on | H |

### 4.4 Route (reference line)

1. **Start** at 73,-33,11; exit south onto DECK 1. [H]
2. **DECK 1:** run to a lane between the fins, e.g. x60 (between 55 and 73). [H for geometry; L for choice]
3. **GAP A:** bridge south 27 blocks at x60 (or x80, or outside x92). The fins block x55/73/91; the catwalks at y=-20 are above and disconnected. [H]
4. **DECK 2:** cross 11 blocks south. [H]
5. **GAP B:** cross z66 at x54..60; shift east to x70..75 along z67..73; cross z74; shift west to x54..60 along z75..82; cross z83. [H for gaps; M/L for the line]
6. **Finish:** continue 5 blocks to z88 at x57..60, step over the acacia lip (top -36), drop 4 into the pen, punch any chicken at y=-39. [H]
7. **Reset:** spectator for 7 s, then tp to the start box. [H]

### 4.5 Sightlines / legibility

- **From the start:** the three fins and their catwalks are directly ahead; the machine heads frame left/right; the glass skylight is overhead. The finish is **not visible** (middle deck + curtains + the 4-deep pen). [H geometry; M view]
- **From DECK 2:** the three curtain faces fill the view; the window bands are above eye level, so the far side is visible only through glass. [H geometry; M view]
- **From the z83 lip:** the far DECK 3 and the top of the pen are visible; the chickens are 4 below the lip and only appear at the edge. [H geometry; M view]
- The intended "where do I go" signal is the **fin lines / curtain gaps**, not a landmark or a finish arch. There is no finish sign, no arrow, no beacon, no coloured finish line. [H]

### 4.6 Hazards / fail states

- Only the void: fall below y=-58 → tp to 73,-33,11; timer keeps running. [H]
- No lava/water/fire/mob hazard; `fallDamage=false`, `difficulty=peaceful`, `doMobSpawning=false`, `keepInventory=true`. [H]
- No checkpoints; a fall costs the bridge from the last deck. [H]

---

## 5. Blocks, palette, lighting, build techniques

Full table: **`data/blocks.csv`** (39 base blocks, count/share/use). Palette/lighting/build notes: **`data/palette.md`**.

### 5.1 Dominant materials

| Material | Count | Share | Role |
|---|---:|---:|---|
| smooth_stone | 34,812 | 28.47% | plinth mass, walls |
| smooth_stone_slab | 34,675 | 28.36% | double-slab plinth, trim, roof units |
| blackstone | 13,918 | 11.38% | conveyor strips, deck edge, roof |
| polished_andesite | 8,833 | 7.22% | conveyor frames |
| light_blue_stained_glass | 7,286 | 5.96% | fins, skylight, windows |
| red_concrete | 4,500 | 3.68% | red curtains |
| yellow_concrete | 3,090 | 2.53% | yellow curtain |

[H, `data/blocks.csv`]

### 5.2 Lighting

- 9 sea lanterns only: 3 at y=-25, 6 at y=4 (roof units). No torches/lanterns/glowstone. [H]
- 31×105 glass skylight at y=9 over a dark roof; `doDaylightCycle=false`, DayTime 0. The hall is sky-lit in the centre, shaded at the machine heads and side decks. [H geometry/gamerules; M light levels]
- The glass fins transmit light and read as bright planes; the blackstone deck and machine undersides are the dark contrast. [M from renders]

### 5.3 Build techniques

- **Conveyor table:** 95-wide slab, 2-wide blackstone + 1-wide gray wool stripes, andesite rails, blackstone stair ends; solid plinth, no interior. [H]
- **Machine head:** white concrete panel + sea-lantern button + red sandstone frame + brick/magenta tower + glass panes, 1–3 wide, deck-to-roof. [H]
- **Glass fin:** 1-block-thick, 25×39, through the void into the foundation; 3-wide iron-trapdoor catwalk on top. [H]
- **Paint curtain:** 1-block-thick concrete, 25–26×39, 9-high window band with border + glass + diagonal drip wedges; red rows have white frames, the yellow row black. [H]
- **Start tray:** quartz floor 1 above the deck, black wool border, barrier cage, single oak sign. [H]
- **Roof:** 73×105 blackstone slab with a 31-wide glass strip and white concrete roof units; 13-block north overhang. [H]
- **Catwalk grid:** 702 iron trapdoors + red sandstone supports at y=-20, in 21-wide segments with 6–7-block gaps; disconnected from the decks. [H for blocks/segments; M intent]
- **No command blocks, redstone, containers, or item frames.** [H]

---

## 6. 1.19 → 1.21.1 — exact failures

Full detail and fix checklist: **`data/LOAD_AND_1_21.md`**.

| # | Failure | Exact effect | Grade |
|---|---|---|---|
| 1 | `file/Industry` absent from live `DataPacks.Enabled` (and Disabled) | pack present but not loaded | H |
| 2 | `data/tgttos/functions/` instead of `function/`; `data/minecraft/tags/functions/` instead of `tags/function/` | no functions, no load/tick tags | H |
| 3 | `pack_format: 10` instead of 48 | old-pack warning/incompatibility | H |
| 4 | 21 `item replace` lines with pre-1.20.5 item NBT | parse error; no pickaxe, no offhand concrete | H |
| 5 | `tellraw` click values start with `/` | 1.21.1 OK; 1.21.5+ risk | M |
| 6 | sign `Text1..4` NBT | migrated to `front_text` in the live world; click JSON preserved | H |
| 7 | chicken NBT | survived; resistance id 11 amp 5 dur 19; persistence 0 | H |
| 8 | `damage_dealt_resisted` criterion | stat exists in 1.21.1 | H |
| 9 | `data get Pos[1]`, `summon`, `effect`, `fill`, `clone`, `gamerule`, `title` | valid in 1.21.1 | H/M |

**Old item-NBT lines:** `loop.mcfunction` 84 (pickaxe) and 86–87, 89–90, 92–93, 95–96, 98–99, 101–102, 104–105, 107–108, 110–111, 113–114 (10 colours ×2). 21 total. The replacement shapes are in `data/LOAD_AND_1_21.md` §6 and `data/datapack_functions.csv`.

---

## 7. Comparison

### 7.1 With `McmTgttos` (current game code)

`McmTgttos` is a Paper 1.21.1, pure-Java TGTTOS generator: 5 rounds × 300 s, 15 s pre-round, 40+ spawns, kit in hand, 64 team concrete + iron pickaxe + 25 snowballs, only player-placed concrete breakable, finish row of ≥8 named chickens, time skip at 50%, placement/points, spectators, no damage, no collision. [H, `game-plugins/Tgttos/README.md`, `TgMapData.java`, `Kit.java`, `FinishRow.java`]

| Axis | Industry (this map) | `McmTgttos` | Grade |
|---|---|---|---|
| Form | imported 1.19.1 world + 7-function datapack | pure-Java generator, pasted per round | H |
| Runtime | Paper 1.21.1 review world; pack **not enabled** | Paper 1.21.1 plugin, native | H |
| Start | 1 spawn in a 20×3 barrier box; 7 s countdown | 40+ spawns in a start strip; 15 s pre-round | H |
| Kit | 1 concrete (refilled every tick) + Efficiency II unbreakable pickaxe | 64 concrete (refill after 1 tick) + iron pickaxe + 25 snowballs | H |
| Place/break rules | CanPlaceOn surface whitelist; CanDestroy all 12 concretes; **no build-box** | only concrete, only inside `buildBounds`; only player-placed concrete breakable | H |
| Bridging rate | 1 block/tick max (slow, precise) | 64-block stack (fast, spammy) | H |
| Finish | 24 chickens scattered in a 31×11×4-deep basin; no row, no tags, no landmark | a single row of ≥8 named chickens on hay under a FINISH arch, ≥75% in sight from 8–16 blocks | H |
| Finish detector | `damage_dealt_resisted` stat on Resistance V chickens | `PrePlayerAttackEntityEvent` (invulnerable chickens never fire a damage event) | H |
| Checkpoints | none | `MapFeatures` checkpoint/mover/toggle/trap/refill/depot/bonus | H |
| Hazards | void only (y<=-58) | void, hazard blocks/regions, lethal-fall option | H |
| Timer/HUD | per-player actionbar MM:SS:CC; no boss bar/sidebar | boss bar + sidebar + time skip | H |
| Reset | 7 s spectator, tp all players, local concrete cleanup, `block_reset` 8/10 curtains | full re-paste of the map at round end; placed blocks cleared | H |
| Teams/scoring | colour tags only; no placement/points | placement points, top-10 bonus, team bonus | H |
| Map validation | none | 40+ spawns, 8+ chickens, pen/strip/bounds, no dyed concrete | H |
| Dyed concrete in map | red/yellow curtains (also two player colours) | **forbidden** ("reserved for players") | H |
| 1.21 readiness | dead without repack (folder/format/item NBT) | native 1.21.1 | H |

**What this means:** Industry is a **single-player practice map** whose mechanics rhyme with `McmTgttos` (chicken punch, concrete bridge, timer) but whose implementation is the opposite: fixed world + datapack vs generated map + plugin; 1-block refill vs 64-block stack; hidden basin vs finish row; no validation vs strict validation. It is not a drop-in map for `McmTgttos`; the plugin has no world loader, and Industry would fail `TgMapData.check` on spawn count, chicken count/pen shape, `playBounds`, and dyed concrete. [H for the contract; M for "would fail" because the check is applied to generated `TgMapData`, not to an imported world, but the criteria are explicit]

### 7.2 With Badlands and Pits (architecture only)

All three are Mickey_Kat datapack maps with the same template: `pack_format: 10`, plural folders, `namespace tgttos` (Pits pack folder is `Pits` but the data namespace is `tgttos`), a 140-tick countdown, per-player centisecond timer, `hit_chicken` finish, armor-stand cleanup, 140-tick reset. [H from the three packs' file trees/tick tags]

| Map | Pack files | Tick list | Map-specific mechanic | Owner report |
|---|---|---|---|---|
| Industry | 7 (load, loop, color, start_timer, gamerules, respawn, block_reset) | 5 functions | red/yellow paint curtains + 1-block concrete bridge | this dossier |
| Badlands | 2 (load, loop) | 1 function (`loop`) | cactus regrowth inside `loop` | A7 `../tgttos_badlands/` |
| Pits | 6 (load, loop, color, start_timer, gamerules, respawn) | 5 functions | pit/void course; no `block_reset` | A9 `../tgttos_pits/` |

Industry is closest to Pits in structure (same 6-function core + a map-specific reset); Badlands compresses everything into `loop`. Industry's unique pieces are `block_reset.mcfunction`, the 58-wide yellow pattern clone, and the paint-curtain geometry. Details of Badlands/Pits routes, blocks and commands are **not** repeated here; use the A7/A9 reports. [H for the file/tick comparison; links for the rest]

---

## 8. What Industry does better / worse

### Better

- **Strong, coherent theme on a tiny palette:** 39 base blocks, six materials >85%, no organic clutter. The conveyor stripe + glass fin + paint curtain reads instantly as "factory". [H]
- **A real first decision:** the three fins put the spawn on a divider; the player must pick a lane in the first seconds. This matches the "decision in the first 3 s" requirement better than a straight corridor. [H geometry; M design intent]
- **A forced weave, not a straight bridge:** the alternating x-gaps in the three curtains cannot be passed on one x line. This is a genuine skill gate and a good use of 1-thick walls. [H]
- **The paint-curtain window detail is excellent:** 9-high glass band, white vs black frames, diagonal paint drips; it gives a flat wall a readable "painted window" story. [H]
- **Cheap to run:** 122k blocks, no command blocks, no redstone, no containers; the entire logic is 7 functions. [H]
- **A compact, readable plan:** 95×91 play area, three tables, two gaps; easy to learn the layout. [H]

### Worse

- **The start sign's trigger is on an invisible line** (event on empty line 1; the visible "Start" line has no event). It works because sign clicks run all line events, but it is not self-documenting. [H/M]
- **No colour-selector trigger**; the only way is `/trigger color`. [H]
- **The finish is hidden and unmarked:** a 4-deep basin with 24 scattered chickens, no row, no tags, no arch, no "FINISH" sign. It fails the current `FinishRow` rule (single row, spacing 1.5–2.5, ≥75% in sight). [H geometry; H contract]
- **Multiplayer is broken:** one finish stops everyone, teleports everyone, and one sign click starts everyone. [H]
- **One spawn:** all players stack in a 20×3 box; no 40-spawn strip. [H]
- **No checkpoints or hazard regions:** a fall costs the entire bridge; no mid-course recovery. [H]
- **The reset is incomplete:** z66 centre/east curtains are never rebuilt; those holes persist after a run. [H commands; M visual]
- **Chickens are non-persistent and only 24:** no respawn if killed; despawn risk on smaller simulation distances. [H NBT; M risk]
- **Team-colour concrete overlaps the map:** red/yellow curtains vs red/yellow player blocks; the pickaxe can mine both. [H]
- **The catwalk grid is disconnected:** 6–7-block gaps and no deck access; if intended as a high route, the access is missing. [M]
- **1.19.1 pack is dead on 1.21.1** without a repack (four blockers). [H]
- **World bloat:** 4,464 chunks but only 54 with blocks; 98.8% empty. [H]

---

## 9. Open questions / things to verify in-game

1. **Confirm the start sign in-game.** The clickEvent is on the empty `Text1`; the 1.21.1 `SignBlockEntity` loops all four messages and runs every `RUN_COMMAND`, so the sign should start the game when right-clicked anywhere. Test once in 1.19.1 and 1.21.1 to confirm the 1.19.1 behavior and the permission level. [H for 1.21.1 code; M for 1.19.1]
2. **Does Resistance V produce `damage_dealt_resisted` on a fist hit in 1.19.1?** The stat is the finish detector; the chickens have amplifier 5, duration 19. A single hit should fully resist. [M]
3. **Does the live 1.21.1 world still have the chickens after a fresh load?** The entity NBT survived the upgrade; persistence is 0. On this server sim-distance 6, the pen is 81 blocks from the start, so they should tick; verify with `/tg chickens`-style entity counts or a manual visit. [M]
4. **Does `block_reset` visibly restore the z66 centre/east curtains after an east-lane run?** The commands say no; confirm with a before/after screenshot. [H commands; M visual]
5. **Is the catwalk grid intended as a route?** It has no deck access and 6–7-block gaps; if the author intended a high road, the access is the missing piece. [M]
6. **Exact light levels** in the hall (skylight vs sea lanterns) were inferred from geometry, not measured in-game. [M]

---

## 10. Deliverables in this folder

| Path | Contents |
|---|---|
| `README.md` | this dossier |
| `REFERENCE_IMAGES.md` | image → source → what it shows → what to take |
| `frames/` | full-resolution close-up evidence (start box, gap A lanes, curtain window, finish pen) |
| `renders/` | overviews, roof-off iso/top, labelled route plan, profile, layer plans |
| `data/blocks.csv` | 39 base blocks, counts, shares, group, use |
| `data/command_blocks.csv` | header only — world has 0 command blocks |
| `data/signs.csv` | the one start sign, all four lines, click event |
| `data/checkpoints.csv` | start/respawn + finish; no mid-course checkpoints |
| `data/entities.csv` | 24 chickens, positions, persistence, effects |
| `data/datapack_functions.csv` | every non-empty datapack line, 1.21 risk, notes |
| `data/datapack/` | unpacked `Industry` pack (read-only copy) |
| `data/LOAD_AND_1_21.md` | exact source/live load path, four blockers, fix checklist, 21 old item-NBT lines |
| `data/route.md` | leg-by-leg route, coordinates, sightlines, fail states |
| `data/palette.md` | palette, lighting, build techniques |
| `data/timeline.csv` | tick/second event timeline |
| `data/scan_summary.json`, `block_histogram.json`, `blocks_xyz.csv` | raw scan evidence |
| `STATUS.md` | done / blocked / open questions |
