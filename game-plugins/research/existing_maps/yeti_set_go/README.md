# Yeti Set Go — forensic dossier

**Map:** Yeti Set Go (`Yeti Set Go!` in `level.dat`)
**Source:** `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\YetiSetGo` (`YetiSetGo.zip`; `zipped-map.zip` is byte-identical)
**Live world:** `mcc_yetisetgo` (review server). The reachable copy `mcc_recreations\YetiSetGo` is **byte-identical** to the source (SHA-256 of `level.dat`, `resources.zip` and all region files match; H), so it was analysed once.
**Version:** 1.19.2, DataVersion 3120 (H)
**Size:** 3,357 chunk records (109 with non-air blocks); content bounds **x −182..77, y −64..46, z −140..77**; **28,533 non-air blocks**; 0.1 MB `resources.zip` (H)
**Counts:** 37 command blocks, 14 signs, 3 shulkers + 1 hopper + 2 empty campfires (6 container block entities; 4 storage containers), 2 paintings (the only entities) (H)
**Owner:** A4. Research only; no builds, no server edits.

Confidence tags: **H** = directly observed (block/NBT/file/frame), **M** = inferred from two independent observations, **L** = guess.

---

## TL;DR — the ten findings that matter

1. **It is a one-lap circuit, not a point-to-point race.** Start and finish are the same checkered gate at **z = 7** on the train-engine platform; the route goes south through the village, west to the station/pool, north by elytra, then back south through the north chain, tunnel and waterfall to the gate (M; evidence in §2).
2. **37 command blocks in two clusters** — start (20, −3, 0) and station (−155, 32, 27). 30 repeating, 2 chain, 5 impulse (H).
3. **The countdown is wired backwards.** `LastExecution` timestamps show gate-fill → `>1<` → `>2<` → `Starting In`/`>3<` → trident; the gate opens 2.4 s early and only `>3<` is ever visible (H/M).
4. **`gamemode adventure` does nothing — all 3 blocks fail.** Coordinates (24, −3, −8), (24, −3, −6) and the chain block (20, −4, 1); all three have `SuccessCount = 0`, and the first stores `LastOutput = permissions.requires.player`. Players stay in whatever gamemode Multiverse/the server applies (survival on this server), not adventure (H; cross-checked with A12).
5. **All pad effects target `@a`, not `@s`.** One player on a pad buffs everyone; one player on the station stone pad strips **everyone’s** elytra; one player on lodestone gives **everyone** an elytra (H).
6. **Every yellow glazed pad is a global spawn checkpoint** because `setworldspawn` runs for any player standing on yellow glazed (H). There are 29 yellow clusters / 377 blocks.
7. **The elytra grant only works locally at the station on a sim-6 server.** The single command at (−155, 35, 28) needs its chunk loaded; the lodge roof is 152 m away and the start 177 m away. At the review server’s `simulation-distance=6` only the station works (H/M).
8. **The saved gate is closed.** 11 barrier blocks + 11 glass panes at z 7 are present; the `fill` block has `SuccessCount=0` in the saved NBT (H). A fresh button press should clear it (M).
9. **Containers are empty.** 6 container block entities = 3 shulkers + 1 hopper + 2 empty campfires; 4 storage containers, all empty (H). 13 of 14 signs are empty; the 14th is `>>>>>>>>>>>>>` at (−135, 29, 42) (H).
10. **The pack is 0.1 MB and mostly pad animation.** `pack_format` 9, animated yellow/pink/red/magenta pad textures, an unused glowstone texture, an unused purple pad, a Photoshop source (`AnimPad.psd`), and a full 1.19.2 `en_us.json` with `item.minecraft.rotten_flesh = "Energy"` (H). Full pack inventory: **A10 → `../resource_packs/`** (link, not duplicated).

---

## 1. Identity and provenance

| Field | Value | Evidence |
|---|---|---|
| Level name | `Yeti Set Go!` | `level.dat` (H) |
| Version | 1.19.2, DataVersion 3120 | `level.dat`, chunk NBT (H) |
| Generator | void; every section biome `minecraft:the_void` | chunk NBT (H) |
| `generate_features` | 0 | `level.dat` (H) |
| GameType / allowCommands | 1 (creative) / 1 | `level.dat` (H) |
| Difficulty / hardcore | 0 / 0 | `level.dat` (H) |
| Spawn | 21, −1, 4 | `level.dat` (H) |
| World time | 1,089,421; `DayTime` 1000; no cycle | `level.dat` (H) |
| Weather | clear; no cycle | `level.dat` (H) |
| Player data | 1 player, creative, empty inventory, at 21.5, −1, 4.5 | `playerdata/*.dat` (H) |
| Regions with content | r.-1.-1, r.-1.0, r.0.-1, r.0.0; r.-2.-1 and r.-2.0 hold 49 empty chunk records | region files + dump (H) |
| Chunk records | 3,357; 109 with non-air blocks | scan (H) |
| Resource pack | `resources.zip` 97,599 B, SHA-256 `03AD22…055D` | files (H) |

Gamerules (H, `level.dat`): `doDaylightCycle=false`, `doWeatherCycle=false`, `doMobSpawning=false`, `doFireTick=false`, `fallDamage=false`, `keepInventory=true`, `doImmediateRespawn=true`, `disableElytraMovementCheck=false`, `commandBlockOutput=false`, `maxCommandChainLength=65536`, `randomTickSpeed=3`, `spawnRadius=0`, `doTileDrops=false`, `doEntityDrops=false`, `announceAdvancements=false`, `showDeathMessages=false`, `naturalRegeneration=true`.

The map is a **void build**: 3,357 chunk records are saved, but only 109 contain non-air blocks; the rest are empty void chunks. The void floor is y −64; a fall is fatal even with `fallDamage=false` (H/M).

---

## 2. The course in one page

- **Start/finish:** train-engine platform, x 10..32, z −13..7, floor y −2. Button at (21, −1, 6); checkered gate at z 7 (x 16..26, panes y −1, barrier y 1). Spawn (21, −1, 4) (H).
- **Leg 1 — village U:** gate → dock z 8..9 → boat basin z 11..19 → U path around the basin, east side x 24..32, bottom z 60..72, west side x −31..−14 → south-west plaza x −45..−25, z 30..44 (H).
- **Split:** station line west through the ice cave (x −55..−123, y 1..4) → bubble tower (x −129..−127, z 29..32, y 4..27) → station checkpoint (x −171..−130, y 29..41, z 24..53). Pool line north through the chalet (x −46..−35, z 19..32) → **riptide up the pool waterfall at x −38** (5–6 blocks from the chalet roof) → pool (x −92..−38, y 15) → arch/bridge → lodge roof (x −25..2, y 35, z −39..−3) (H/M).
- **Elytra:** station y 36 or lodge y 35 → north snow chain; target north tip x 7..22, y −20, z −140..−129 (M).
- **Leg 5 — north chain south:** 7 jump pads and 6 yellow strips from z −132 to z −53 → colonnade/tunnel x 17..26, z −45..−36, y −7..−9 (M).
- **Leg 6 — finish:** tunnel → ledge y −13, z −35..−14 → riptide up the 22-block waterfall at z −16..−14 → platform y −2 → south across the platform → finish gate z 7 (H/M).

Full leg-by-leg coordinates, pads, sightlines and failure planes: **`data/route.md`**. Pad clusters: **`data/pad_clusters.csv`**. Route image: **`renders/route_overlay.png`** / `frames/18_route_overlay.png`.

### Why the lap reads as a lap (M)

1. The only start/finish gate, button and countdown are on the engine platform (H).
2. The north chain physically connects the start’s north side to the north tip (H: single component chain C0 → C49 → C66 → C25 → C24 → C13 → C07 → C06 → C37 → C57 → C73 → C27 → C22).
3. The start’s north side has a purpose-built **landing ledge** at y −13 with two jump pads and a waterfall up to the platform (H). That ledge is only useful for an arrival (M).
4. The station has a **setworldspawn checkpoint**, not a finish gate (H).
5. The station and lodge both have lodestone elytra pads and no other exit; the only logical destination is the north (M).
6. No section is left unused in the loop; the river is an optional scenic detour inside the pool section (M).

If the map is actually point-to-point, the only candidates are the station (finish at the caboose) or the north tip (finish at the snow). The station has no finish gate and carries a checkpoint, which argues against it (H); the north tip has a pad and marker posts but no gate (H). The lap reading is the only one that uses the return ledge and waterfall. I keep point-to-point as an alternative (L) in §10.

---

## 3. Mechanics

### 3.1 Pads (H)

| Block | Count / clusters | Effect | Duration | Notes |
|---|---:|---|---:|---|
| Yellow glazed terracotta | 377 / 29 | Speed IV (`speed 2 3`) | 2 s | also a **global spawn checkpoint** |
| Yellow carpet | 4 blocks / 1 cluster | Speed IV (`speed 2 3`) | 2 s | same as yellow |
| Pink / red / magenta glazed | 92 / 92 / 23 | Speed VIII or IX + Jump Boost V | 1 s | 3×3 pad: pink corners, red edges, magenta centre |
| Lime wool | 54 / 6 | Jump Boost VII or VIII | 1 s | 3×3 pads at the plaza, pool, lodge, station |
| Lodestone | 312 / 6 clusters | Elytra on (chest slot) | until removed | station 100, start 146, lodge 60, misc 6 |
| Stone | many | Elytra off (chest slot) | until re-equipped | station logic only |

Every effect uses `effect give @a …`, so the buff applies to **all players**, not only the one on the pad (H). The station’s `item replace entity @a armor.chest` also applies to all players (H). In a multiplayer race this is a serious bug; in single-player it is invisible.

### 3.2 Start kit (H)

- `item replace entity @a hotbar.0 with minecraft:trident 1` at (22, −3, 3) — impulse, fired by the button.
- `enchant @a minecraft:unbreaking 3` at (19, −3, −5) — repeating.
- `enchant @a minecraft:riptide` at (19, −3, −4) — repeating.
- The station refreshes the same kit: `unbreaking 3` at (−157, 32, 28), `riptide 2` at (−156, 32, 28) (H).

Because the enchant commands target the **main hand**, the trident must be selected (slot 0). `keepInventory=true` keeps it through deaths (H).

### 3.3 Elytra swap (H/M)

- **On:** step on lodestone; command at (−155, 35, 28) replaces the chest slot with an elytra. Repeating, always on while its chunk is loaded.
- **Off:** step on stone at (−155, 33, 28) or yellow glazed at (−155, 34, 28); the chest slot is set to air.
- The station’s west platform at y 36 is 100 lodestone blocks; the lodge roof is 60; the start side lanes are 146 in three strips. The hollow train-car floor between them is spruce/mangrove (H).
- **Chunk dependency:** at `simulation-distance=6` (review server, H) the command only runs near the station; the lodge roof (152 m) and start (177 m) are outside. At sim ≥10 the lodge works; at ≥12 the start works (M). This is a design that assumes a high simulation distance (MCC-style server) or a plugin.

### 3.4 Riptide / water (H/M)

- The trident is given at the start with Riptide I; the station refreshes Riptide II.
- Water features: the start boat basin (z 11..19, y −4), the pool (y 15), the pool waterfall at x −38, the bubble column (x −129..−127, z 29..32, y 4..27), the south channel (x −31..−8, z 42..62, y −4), the north waterfall (x 55..65, z −80..−61), and the finish waterfall (z −16..−14, x 15..28, y −13..9).
- The finish waterfall is 22 blocks tall; a single Riptide II is roughly 10–14 blocks, so the intended move is likely **two riptides or a riptide plus a swim** (M/L — needs in-game timing).
- The bubble column is a one-way up elevator (H).

### 3.5 Checkpoints / respawn (H/M)

- Global spawn starts at (21, −1, 4).
- Every yellow glazed pad under a player runs `setworldspawn`; the spawn is **global**, so in multiplayer all players share the last pad (H).
- The station yellow pad at (-167, 36, 31) (command at -155, 35, 27) is the only explicit mid-course checkpoint (H).
- Coverage at sim 6: start command ≈ x −73..119, z −96..94; station command ≈ x −251..−59, z −69..123. This leaves the pool east/lodge and the whole north chain without a checkpoint (M).
- No scoreboard, timer or lap counter exists in the 37 blocks (H).

### 3.6 Start gate / countdown (H)

- Saved state: gate **closed** — 11 glass panes at y −1 and 11 barrier blocks at y 1, x 16..26, z 7 (H).
- Button at (21, −1, 6) → 10 repeaters and 13 wire → 5 impulse blocks → gate fill, subtitles, title, trident (H).
- `LastExecution` order is gate → 1 → 2 → Starting In/3 → trident (H). The gate opens 48 ticks before the title; the 1/2 subtitles are set before any title, so only `>3<` shows (M).
- The `fill` block’s saved `SuccessCount=0` (H): on the saved state the last run cleared nothing. A fresh press should clear the panes/barrier (M).

---

## 4. Command blocks

Full export: **`data/command_blocks.csv`** — 37 rows with x/y/z, block type, facing, auto/conditional, exact command, trigger, effect, reset, 1.21 risk, `LastExecution`, `LastOutput`, custom name.

### 4.1 Two clusters (H)

| Cluster | Bounds | Count | Role |
|---|---|---:|---|
| Start | x 11..26, y −5..−3, z −8..7 | 22 | gate/countdown, kit, pad checks |
| Station | x −157..−155, y 30..35, z 27..28 | 15 | station pads, setworldspawn, kit refresh, elytra swap |

Block types: **30 repeating**, **2 chain**, **5 impulse** (H). All are `conditional=false`; `conditionMet=1` is a stale runtime flag, not the conditional setting (H).

### 4.2 Start cluster (22)

| x,y,z | Type | Command | Effect |
|---|---|---|---|
| 24,−3,−8 | repeating | `gamemode adventure` | **no-op** (no player) |
| 24,−3,−6 | repeating | `/gamemode adventure` | **no-op** |
| 20,−4,1 | chain | `gamemode adventure` | **no-op** |
| 19,−3,−5 | repeating | `enchant @a minecraft:unbreaking 3` | kit |
| 19,−3,−4 | repeating | `enchant @a minecraft:riptide` | kit |
| 23,−3,−2 | repeating | `execute as @a at @s if block ~ ~-1 ~ yellow_glazed_terracotta run setworldspawn` | global checkpoint pad |
| 22,−3,0 | repeating | `title @a actionbar "You are in 1st place"` | spam every tick |
| 26,−3,2 | repeating | lime → `jump_boost 1 6` | Jump Boost VII |
| 11,−4,3 | impulse | `fill 16 -1 7 26 1 7 air` | gate open |
| 14,−4,3 | impulse | subtitle `>1<` (green) | countdown |
| 17,−4,3 | impulse | subtitle `>2<` (yellow) | countdown |
| 20,−4,3 | impulse | title `Starting In` (aqua) | countdown |
| 20,−4,2 | chain | subtitle `>3<` (red) | countdown |
| 22,−3,3 | impulse | `item replace entity @a hotbar.0 with minecraft:trident 1` | trident |
| 26,−5/4/3,3 | repeating | magenta/pink/red → `jump_boost 1 4` | Jump Boost V |
| 26,−5/4/3,4 | repeating | magenta/pink/red → `speed 1 8` | Speed IX |
| 26,−3,5 | repeating | yellow glazed → `speed 2 3` | Speed IV |
| 26,−3,6 | repeating | yellow carpet → `speed 2 3` | Speed IV |

### 4.3 Station cluster (15)

| x,y,z | Type | Command | Effect |
|---|---|---|---|
| −157,32,27 | repeating | pink → `speed 1 7` | Speed VIII |
| −157,33,27 | repeating | pink → `jump_boost 1 4` | Jump Boost V |
| −156,32,27 | repeating | magenta → `speed 1 7` | Speed VIII |
| −156,33,27 | repeating | magenta → `jump_boost 1 4` | Jump Boost V |
| −155,30,27 | repeating | yellow carpet → `speed 2 3` | Speed IV |
| −155,31,27 | repeating | yellow glazed → `speed 2 3` | Speed IV |
| −155,32,27 | repeating | red → `speed 1 7` | Speed VIII |
| −155,33,27 | repeating | red → `jump_boost 1 4` | Jump Boost V |
| −155,34,27 | repeating | lime → `jump_boost 1 7` | Jump Boost VIII |
| −155,35,27 | repeating | yellow glazed → `setworldspawn` | **checkpoint** |
| −157,32,28 | repeating | `enchant @a unbreaking 3` | kit refresh |
| −156,32,28 | repeating | `enchant @a riptide 2` | kit refresh |
| −155,33,28 | repeating | stone → `item replace entity @a armor.chest with air` | elytra off (all players) |
| −155,34,28 | repeating | yellow glazed → `armor.chest` air | elytra off |
| −155,35,28 | repeating | lodestone → `armor.chest` elytra | elytra on (all players) |

### 4.4 Bugs / sharp edges

1. **Backwards countdown** (H/M): see TL;DR §3.
2. **`gamemode adventure` no-op — 3 blocks, all fail** (H; A12 confirms): (24, −3, −8) repeating `gamemode adventure`; (24, −3, −6) repeating `/gamemode adventure`; (20, −4, 1) chain `gamemode adventure` (runs after the `Starting In` impulse at 20, −4, 3). All three have `SuccessCount = 0`; the first stores `LastOutput` = `permissions.requires.player`. The commands have no player target, so a command block executes them as the server and the game refuses. **Effect:** none — the map never forces adventure mode. **Multiverse implication:** on import, the world is played in the per-world/server gamemode (the review server default is `gamemode=survival`, `server.properties` H; the world’s `level.dat` GameType is creative, H). In survival players can break/place blocks, use items and take environmental damage, so the build is unprotected and the “race” is not enforced; in creative they can also fly. A plugin or Multiverse per-world `gamemode=adventure` is the only thing that would set it. This is a pre-existing 1.19.2 bug, not a 1.21 regression (H/M).
3. **Actionbar spam** (H): `title @a actionbar "You are in 1st place"` runs every tick, unconditionally.
4. **`@a` kit/pad application** (H): multiplayer-wide buffs/elytra toggles.
5. **Simulation-distance dependency** (H/M): station-only elytra at sim 6; lodge needs ≥10; start needs ≥12.
6. **Saved gate closed + fill SuccessCount 0** (H).
7. **Chain order**: the chain block at (20,−4,2) runs before the title at (20,−4,3), so its subtitle exists before the title; only `>3<` ever displays (M).
8. **Button placement**: the button is a floor button at (21,−1,6); a player must stand on the platform, not the dock, to press it (H).

---

## 5. Containers, signs, entities

### 5.1 Containers (H — all empty)

**Definition (shared with A10/A13):** 6 container block entities = 3 shulkers + 1 hopper + 2 empty campfires; 4 storage containers, all empty. The two campfires have 4 cooking slots each, all empty.

| Block | Coordinates | Kind | Items |
|---|---|---|---|
| Light gray shulker box | 15, 4, 7 | storage | 0 |
| Light gray shulker box | 27, 4, 7 | storage | 0 |
| Light gray shulker box | 27, −1, 7 | storage | 0 |
| Hopper | −149, 33, 27 | storage | 0 |
| Campfire | −39, 4, 32 | cooking | 0 (4 slots empty) |
| Campfire | −101, 2, 35 | cooking | 0 (4 slots empty) |

The shulkers sit on/above the start gate structure (y −1 and y 4); they are empty start-kit boxes (M). The hopper is part of the station control wall, empty (H). The two campfires are lit (`lit=true`, `facing=west`) and are the cave/chalet accent lights; their cooking slots are empty (H). Full export: **`data/containers.csv`** (6 rows, with a `kind` column).

### 5.2 Signs (H)

14 signs total: 7 warped wall signs on the station interior wall (x −153..−149, y 30..31, z 27), 3 on the station lower balcony (x −136..−134, y 29, z 42), and 4 dark oak wall signs on the lodge wall (x −2, y 36/38, z −19/−20). 13 are empty. The one with text:

- **(−135, 29, 42)** — white, glowing, `>>>>>>>>>>>>>` on lines 2 and 3 (H). It is a direction marker on the station’s lower balcony, pointing east/out toward the bubble-column side.

No sign has a click event (`clickEvent` absent from all messages; H). Full export: **`data/signs.csv`**.

### 5.3 Entities (H)

| Entity | Coordinates | Variant |
|---|---|---|
| Painting | −143.0, 36.5, 27.03 | `minecraft:pool` |
| Painting | −140.5, 36.5, 27.03 | `minecraft:bomb` |

Both are inside the station car (tile −144/36/27 and −141/36/27). No armor stands, item frames, boats, or displays exist in the saved world (H). The “boat” at the start is a block build, not an entity (H).

---

## 6. Blocks and palette

Full export: **`data/blocks.csv`** (133 base blocks, count/share/family/use) and **`data/block_states.csv`** (238 states). Summary: **`data/palette.md`**.

Top-line numbers (H): water 7,781 (27.3%), stone 5,917 (20.7%), snow_block 3,083 (10.8%), packed_ice 1,094 (3.8%), mangrove 906 (3.2%), andesite 856 (3.0%), spruce 836 (2.9%), gray concrete 672 (2.4%). Family shares: stone 28.6%, water 28.2%, snow/ice 15.6%, wood 9.7%, concrete 5.8%, wool 3.4%, pads 2.3%.

Lighting (H): daylight only (DayTime 1000, no cycle), 6 sea lanterns at x 57..64, y −13, z −72, 5 lanterns, 2 campfires; **no glowstone** despite the pack texture. The map is a daylight build; the cave/tunnel/station interiors are dim.

Compactness (H/M): 28.5k blocks in a 260×218 footprint; two command clusters; one elytra command for three lodestone pads; every yellow pad is a checkpoint; 3×3 jump pads use 4 colours for 2 effects; a 3×4 bubble shaft for a 23-block climb; the void is the boundary.

---

## 7. Resource pack mapping (0.1 MB)

The world ships `resources.zip` (97,599 B). It is a **small Ace Race animation pack**, not a full texture set. Mapping table: **`data/pack_mapping.csv`**. Full inventory and licensing: **A10 → `../resource_packs/`** (link only; not duplicated here).

**Path base:** all pack paths in this section and in §8.3 are relative to **`pack/extracted/assets/minecraft/`** (the unzipped `resources.zip`). For example, `textures/block/yellow_glazed_terracotta.png` is `pack/extracted/assets/minecraft/textures/block/yellow_glazed_terracotta.png`. In `data/pack_mapping.csv`, paths are relative to **`pack/extracted/`** (so they start with `assets/minecraft/...`).

| Pack asset | What it does | Used by the map |
|---|---|---|
| `pack.mcmeta` | `pack_format` 9, description `§c Ace Race Animation Example` | whole map (H) |
| `models/block/{yellow,pink,red,magenta,purple}_glazed_terracotta.json` | `cube_all` override → pad texture on all faces | yellow/pink/red/magenta used; **purple unused** (H) |
| `textures/block/yellow_glazed_terracotta.png` | 16×256, 16-frame animated chevron | yellow speed pads (H) |
| `textures/block/{pink,red,magenta,purple}_glazed_terracotta.png` | 16×112, 7-frame ripple | jump pads; purple unused (H) |
| `textures/block/glowstone.png` | 16×16 custom texture | **unused** — no glowstone blocks (H) |
| `textures/block/AnimPad.psd` | 29,647 B Photoshop source | not loaded by the game (H) |
| `models/item/rotten_flesh.json` + `textures/item/rotten_flesh.png` | 2-frame red/blue icon | “Energy” — **not given by any command/container** (H) |
| `lang/en_us.json` | 5,155 keys; `item.minecraft.rotten_flesh = "Energy"` | tooltip only (H rename / M rest vanilla) |

The pad model is `cube_all`, so the animated pattern appears on **all six faces**; the pads read as solid glowing tiles from above and as patterned blocks from the side (H, frame 15/16). The pack is generic (“Ace Race Animation Example”), not bespoke Yeti art (H).

**1.21 pack risk:** `pack_format` 9 must become **34** for 1.21.1; otherwise clients mark it incompatible and may not apply it (H). The `en_us.json` is a full 1.19.2 snapshot and will be missing/incorrect for many 1.21 keys (M). The models, animations and `.mcmeta` files are forward-compatible (H/M). `AnimPad.psd` and the unused glowstone/purple assets should be removed for distribution (H).

---

## 8. 1.21 upgrade notes (1.19.2 → 1.21.1)

The review server runs Paper 1.21.1 (`versions/1.21.1/paper-1.21.1.jar`, H). The world is 1.19.2 / DataVersion 3120. A12 owns the full compatibility matrix; these are the map-specific items.

### 8.1 Commands (H unless noted)

| Command | 1.21.1 status | Risk |
|---|---|---|
| `gamemode adventure` (no target) | syntax valid, **runtime no-op** | pre-existing bug; fix to `gamemode adventure @a` |
| `enchant @a minecraft:unbreaking 3` | valid | item must be in main hand |
| `enchant @a minecraft:riptide` / `riptide 2` | valid | level 2 legal |
| `execute as @a at @s if block ~ ~-1 ~ <block>` | valid | block predicate syntax unchanged |
| `effect give @a <effect> <s> <amp> true` | valid | effect ids unchanged |
| `title @a title/subtitle/actionbar <json>` | valid in 1.21.1 | 1.21.5+ prefers SNBT components; JSON still accepted here (M) |
| `item replace entity @a hotbar.0 with minecraft:trident 1` | valid | slot names unchanged |
| `item replace entity @a armor.chest with minecraft:elytra` / `air` | valid | slot names unchanged |
| `fill 16 -1 7 26 1 7 air` | valid | 33 blocks, under the 32,768 limit |
| `setworldspawn` | valid | global spawn semantics unchanged |
| Command-block NBT (`Command`, `auto`, `conditional`, `powered`, `LastOutput`) | compatible | DataFixer upgrades the world on load (H/M) |

### 8.2 World upgrade (H/M)

- 1.19.2 → 1.21.1 DataFixer runs on first load; 3,357 chunk records are mostly empty, so the upgrade cost is small (M).
- **Signs:** 1.20 moved sign text from `Text1..4` to `front_text`/`back_text`. The 14 signs will be converted; 13 are empty, so the only content at risk is the arrows sign (H/M). No click events exist (H).
- **Command block `LastOutput`/`CustomName`** JSON components convert; the stored `permissions.requires.player` output is historical, not functional (H/M).
- **Player data / stats / advancements** are 1.19.2 and upgrade normally (M).
- `DataPacks.Enabled` contains `Fabric Mods` from the original Fabric server; on Paper it is an unknown pack and should be removed (H/M).

### 8.3 Resource pack (H/M)

- `pack_format` 9 → **34**; otherwise 1.21.1 clients see “incompatible” and may not apply the pad textures (H).
- The full `en_us.json` (5,155 keys) is a 1.19.2 snapshot; 1.21 renamed/added keys. Trim it to the custom `item.minecraft.rotten_flesh` entry (M).
- Remove `textures/block/AnimPad.psd`, `textures/block/glowstone.png`, and `textures/block/purple_glazed_terracotta.*` (paths relative to `pack/extracted/assets/minecraft/`) if they are not wanted (H).
- Models/animations are forward-compatible; the `cube_all` pad model and `.mcmeta` frametimes still work (H/M).
- The world’s `resources.zip` is auto-served; test with a 1.21.1 client, not only the server console (M).

### 8.4 Runtime / server (H/M)

- **Gamemode: 3 failed adventure commands** (H; A12 cross-check). (24, −3, −8), (24, −3, −6) and (20, −4, 1) all run `gamemode adventure` with no target and all have `SuccessCount = 0`; the first stores `permissions.requires.player`. The map therefore runs in the **Multiverse/server gamemode**. The review server’s `server.properties` says `gamemode=survival` (H) while the world’s `level.dat` says GameType 1/creative (H), so the actual mode depends on Multiverse’s per-world setting or an enforcing plugin. In survival the track is breakable and players can place blocks; in creative they can fly. This is the map’s biggest behavioural gap after the elytra/sim-distance issue, and it is not a 1.21 syntax failure — it fails identically in 1.19.2 (H).
- **`simulation-distance=6`** on the review server (H: `server.properties`). The map’s one-elytra-command design needs ≥10 for the lodge, ≥12 for the start. This is the single biggest functional risk after the pack format (M).
- `enable-command-block=false` in the root `server.properties` (H). The PLAN says command blocks were enabled on the review server; A12 should confirm the live value before judging the map (H/M).
- The map expects `keepInventory`, `fallDamage=false`, `doImmediateRespawn`, `doDaylightCycle=false`; these are world gamerules and survive the upgrade (H).
- The map has no datapack; no pack-format issue beyond the resource pack (H).

---

## 9. Comparison (limited to PLAN counts; no independent analysis of the other maps)

The brief forbids analysing Clouds 2, City Marathon, Space Race or TGTTOS. The comparison below uses only the counts in `PLAN.md` plus this dossier’s measurements; it is **not** a substitute for A1/A2/A3. Links: `../clouds2/`, `../city_marathon/`, `../space_race/`.

| Map | Command blocks | Signs | Shulkers | Pack | Chunks | Character |
|---|---:|---:|---:|---:|---:|---|
| Yeti Set Go | 37 | 14 | 3 (empty) | 0.1 MB | 3,357 | compact lap; two clusters; elytra + riptide; void |
| Clouds 2 (PLAN) | 322 | 285 | 225 | 21.2 MB | — | huge, many containers, bespoke pack |
| City Marathon (PLAN) | 67 | — | 140 | — | — | elytra city route, many containers |
| Space Race (PLAN) | 29 | 22 | — | — | — | 1.18.1, elytra zones |

What Yeti does **better** (M/L, design opinion):
- Readable at speed: pads are the only saturated colours; the track material changes with elevation.
- Tiny download and load: 28.5k blocks, 0.1 MB pack, 3.4 MB region files.
- Strong theme: engine → caboose → roof track, all in one lap.
- One elytra command + three lodestone pads is an elegant reuse (but fragile at low sim distance).

What Yeti does **worse** (M/L):
- Multiplayer correctness: `@a` on pad/kit/elytra commands, global spawn on every yellow pad.
- Robustness: the elytra grant depends on chunk loading; the countdown order is wrong; the gamemode commands are no-ops.
- No checkpoint in the north or on the pool line; a missed elytra is a long respawn.
- 0.1 MB pack has unused assets and a PSD; the pack format is stale.

---

## 10. Open questions

1. **Is the lap reading correct?** The alternative is a point-to-point race finishing at the station or the north tip. The return ledge, waterfall and start-side pads argue for the lap (M); an in-game run would settle it (L).
2. **Which line is intended as primary?** The station carries the checkpoint and the elytra command; the pool/lodge is fully built but checkpoint-free. I call the station primary (M); a speed test could reverse it (L).
3. **Does the start waterfall riptide reach the platform in one use?** 22 blocks is tall; likely two riptides or a swim assist (M/L).
4. **What is the “Energy” rotten flesh for?** No command, container or entity references it. It may be a plugin HUD icon or a leftover (L).
5. **What are the two gray “grabber” builds north of the start?** They look like claw/crane props at x 30..40, z −30..−25; no mechanics attached (L).
6. **Why is the gate `SuccessCount=0`?** The saved state is closed; the fill may have run before the gate was rebuilt. A fresh press should clear it (M).

---

## 11. File index

| Path | Contents |
|---|---|
| `README.md` | this dossier |
| `REFERENCE_IMAGES.md` | image → source → what it shows → what to take |
| `STATUS.md` | done / blocked / open questions |
| `data/command_blocks.csv` | 37 rows, full command/trigger/effect/reset/1.21 risk |
| `data/signs.csv` | 14 signs, text/colour/glow |
| `data/containers.csv` | 6 container block entities (3 shulkers + 1 hopper + 2 campfires), all empty; 4 storage containers |
| `data/entities.csv` | 2 paintings |
| `data/checkpoints.csv` | start, station, 29 yellow pads |
| `data/blocks.csv` | 133 base blocks, count/share/family/use |
| `data/block_states.csv` | 238 states |
| `data/palette.md` | palette, lighting, compactness |
| `data/pack_mapping.csv` | pack asset → map use |
| `data/pad_clusters.csv` | 249 pad/elytra clusters |
| `data/route.md` | leg-by-leg route, failure planes, lap |
| `data/yeti_full.mcmdump.gz` | full block dump (28,533 blocks) |
| `data/*.gz` | per-area MCMDUMP crops used for the detail renders (`crop_*`, `v_*`, `f_*`) |
| `renders/` | top, iso angles, route overlay, height map, detail views |
| `frames/` | 20 evidence images (17 map/diagram views + 3 pack texture strips) |
| `pack/extracted/` | unzipped `resources.zip` (read-only copy) |
| `tools/` | read-only analysis scripts used for this dossier |
