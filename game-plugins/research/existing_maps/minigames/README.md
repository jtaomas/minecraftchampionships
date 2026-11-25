# A6 — Minigames world survey (`mcc_minigames`)

Status: **survey complete; bounds published**. No game code, no build, no server writes.
Owner: A6. Written folder: `game-plugins/research/existing_maps/minigames/` only.

Source: `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3`
Live world name: `mcc_minigames` (not touched; no RCON used).
Evidence scale: **H** = directly observed (file bytes / decoded blocks / rendered pixels / exact command string); **M** = inference from two sources; **L** = guess.

---

## 1. What this world actually is

`mcc_minigames` is **not one map**. It is a **hub world with ten built clusters** separated by tens of thousands of blocks of empty void. Each cluster is a self-contained arena or service room. Players move between clusters only by **teleport pads / command teleports**; there are no walkways, rails, or physical connections. The four named games are all command-block driven; there are **no datapacks** and **no Nether/End regions**.

| Fact | Value | Conf. |
|---|---|---|
| `level.dat` LevelName | `Minigame Map` | H |
| DataVersion | `2730` = Minecraft **1.17.1** | H |
| World spawn | `-196 24 927`, angle 90° (inside the main lobby) | H |
| Region files | 68 `.mca`, 73.5 MB total | H |
| Chunk slots present | **18,628** | H |
| Chunk slots with real block data | **196** (668 chunk slots carry sections/palettes; the rest are empty stubs) | H |
| Non-air blocks decoded | **133,030** | H |
| Built clusters | **10** (8 playable/service, 2 Rebecca) | H |
| Command blocks | **942** — 548 chain, 212 repeating, 182 impulse; 731 `auto=1`. **A12 independently found 942 in `mcc_minigames`; this scan matches exactly** | H |
| Signs | **187** | H |
| Command-block clusters | **25** spatial clusters (see `data/command_clusters.csv`); largest is CB-14, 445 blocks in the command room | H |
| Dimensions | overworld only: `DIM-1` and `DIM1` contain **no region folders** (only `data/raids.dat`); no custom `dimensions/` folder | H |
| Containers | **34 container block entities** — 11 barrels, 6 campfires, 6 chests, 5 lecterns, 6 shulker boxes; **21 empty**, **13 Sky Battle loot** containers (**93 items**). Full list: `data/containers.csv` | H |
| Block entities | **1,129 command/sign block entities** = 942 command blocks + 187 signs; container block entities are separate (34 total; see Containers row) | H |
| Entities | 8: 4 eye_of_ender at `1631.7 21.1 -3.8`; armor stand, item frame, glow item frame, small fireball at `2000 26 0` | H |
| Datapacks | none (`Enabled: [vanilla]`) | H |
| Game rules | `keepInventory=true`, `doImmediateRespawn=true`, `fallDamage=false`, `doDaylightCycle=false`, `randomTickSpeed=0`, `commandBlockOutput=false` | H |

**M:** the 18,432 empty chunks are pre-generated stubs left by the author's world border/Chunky pass; the built area is 196 chunks, not 18,628. Do not estimate arena size from the world's chunk count.

**Coverage:** all 68 overworld region files and all 69,632 chunk slots were read; 18,628 slots exist, 668 carry sections, 985 sections carry palettes, and 196 chunks contain non-air blocks (133,030 blocks total). `DIM-1`/`DIM1` have **no region folders** and no custom `dimensions/` exists, so there is nothing outside the overworld. All 187 signs and 942 command blocks were exported (25 command clusters), and all 81 entity-region and 27 POI files were scanned: 8 entities, 21 POI chunks with 16 villager-workstation records (11 fisherman, 5 librarian), no command/sign POIs. **H.**

### Why the source looks "empty" to the shared tool

The shared `tools/world_to_mcmdump.py` and `render_preview.py` only understand the **1.18+** chunk layout (`sections` / `block_states`). This world is 1.17.1 (`Level` → `Sections` / `Palette` / `BlockStates`), so the shared dumper silently produces `no blocks in bounds` on it. I did not edit the shared tool. I wrote a 1.17→MCMDUMP adapter (`data/dump_117_mcmdump.py`) and fed its output to the shared renderer unchanged.

Second trap for later agents: `nbtlib` `LongArray` is **falsy when its first element is 0**; `if not states:` therefore silently turns most sections into air. Use `if states is None or len(states) == 0:` (all scripts here do).

---

## 2. Arena index

Full machine-readable index: `data/arenas.csv`. Bounds are the **copy envelope** (every non-air block, including barrier shells and floating banners). Detailed layout, loot, routes and bugs live in **A6a** (`sky_battle/`), **A6b** (`parkour_tag/`) and **A6c** (`other/`); this table is a summary and does not duplicate them.

| ID | Name / guess | Likely game | Bounds x / y / z | Entry / spawn | Chunks | Blocks | Cmd clusters | Palette (top) | Complete? | Conf. |
|---|---|---|---|---|---|---|---|---|---|---|
| **MG-HUB-01** | Main lobby — crown hub | Hub / game select | -235..-173 / 8..52 / 904..984 | `-196 24 927` (world spawn); pads at y10 | 22 | 10,161 | CB-03..13 | gold, gray concrete, quartz, raw gold, white concrete, team colours | Build complete; known defects (4/8 pads, no path; A6c) | H |
| **MG-SB-01** | **Sky Battle arena** | **Sky Battle** | **1948..2052 / 0..80 / -52..52** | **`2000 78 0`**; pad `-208 10 970` | 64 | 47,874 | CB-18 | barrier 34,751; white concrete; team concrete/terracotta/wool; stone bricks; quartz | Build complete; loop has known defects (A6a) | H |
| **MG-PT-01** | **Parkour Tag arena** | **Parkour Tag** | **1035..1078 / 0..58 / 972..1017** | **`1061 28 995`**; pad `-212 10 975` | 13 | 8,375 | CB-16/17 | gray concrete, cyan terracotta, stone bricks, quartz, dead horn coral, waxed copper | Build complete; loop has known defects (A6b) | H |
| **MG-SC-01** | Super Clash arena | Super Clash | 4922..5012 / 17..36 / -30..6 | `5000 21 0`; pad `-205 10 970`; heroes `4972 29 -15`, villains `4926 20 -15` | 20 | 16,492 | CB-19..21 | light blocks, tinted glass, light-blue concrete, stone bricks, grass, blue concrete | Build complete; loop has known defects (villain objective; A6c) | H |
| **MG-SR-HUB-01** | Sky Race level select | Sky Race hub | 19995..20013 / 39..49 / 19992..20008 | `20000 41 20000`; pad `-201 10 976` | 4 | 902 | CB-22/23 | stone bricks, gray/white concrete, bricks, brick stairs | Build complete; 2/6 levels wired (A6c) | H |
| **MG-SR-RED-01** | Sky Race — Red course | Sky Race | 20991..21070 / 0..82 / 19979..20021 | `21000 31 20000` | 22 | 23,115 | CB-24 | barrier 20,548; black concrete; red concrete/glass/terracotta; white concrete | Build complete; no checkpoints (A6c) | H |
| **MG-SR-ORG-01** | Sky Race — Orange course | Sky Race | 20991..21070 / 0..82 / 20979..21021 | `21000 31 21000` | 16 | 22,869 | CB-25 | barrier 20,597; black concrete; orange concrete/glass/terracotta; cobweb | Build complete; no checkpoints; not a Red reskin (A6c) | H |
| **MG-CMD-01** | Sky Battle command room | Sky Battle logic | 926..1004 / 9..23 / -18..16 | none (service room) | 14 | 1,533 | CB-14/15 | white concrete, command blocks, repeaters, redstone wire | Build complete; Sky Battle only (A6c) | H |
| **MG-REB-01** | "Rebecca's Minigames" kiosk | Unknown / parkour hub | -50005..-49995 / 97..104 / -50005..-49995 | `-50000 100 -50000` | 4 | 309 | CB-02 | white/black concrete, black glass, quartz | Partial / demo | M |
| **MG-REB-02** | "Rebecca" linear course | Parkour / race | -50005..-49871 / 99..131 / -51005..-50946 | `-50000 100 -51000` | 17 | 1,400 | CB-01 | white concrete, lava, waxed copper, yellow carpet, quartz | Partial / demo | M |

**Container counts (all 34; 21 empty, 13 loot / 93 items):** HUB **8**, SB **19** (13 loot + 6 empty), PT **3**, SC **3**, SR-HUB **1**, SR-RED/SR-ORG/CMD/REB **0**. Full list: `data/containers.csv`.

**Not found as arenas:** `Gladiators` and `Theme Race` appear only as lobby wall signs. No blocks, no teleport pads, no commands point to separate arenas for them. **M** that they are disabled/unbuilt. `Guest's Minigames (closed)` is a sign-only bay.

---

## 3. PRIORITY — Sky Battle (`MG-SB-01`)

### Bounds
- **Copy envelope: `x 1948..2052, y 0..80, z -52..52`** (105 × 81 × 105).
- Barrier shell + playfield: `x 1948..2052, y 0..56, z -52..52`.
  - solid barrier roof at **y 56**, with a single **chain at `2000 56 0`** and a chain column at `2000 31..57 0` — this is the drop/climb entrance from the sky lobby to the ring centre.
  - outer walls at x=1948, x=2052, z=-52, z=52 (y 0..56).
  - visible terrain: y 11..42 (x 1961..2039, z -39..39).
- Sky lobby above the roof: `x 1993..2007, y 57..80, z -7..7`, floor/roof of gray wool and gold.
- **Entry / arrival: `2000 78 0`** (centre of sky lobby).
- **Main-hub pad: `-208 10 970`** — yellow carpet, XP level 8; command `execute as @a[level=8] … tp @s 2000 78 0`.
- **Team spawns** (command room `982..985,10,1`):
  - Red **`1970 23 0`** (west) · Blue **`2000 23 30`** (south) · Green **`2030 23 0`** (east) · Yellow **`2000 23 -30`** (north).
- Secondary team-assembly points (lobby): `1998 70 0`, `2000 70 2`, `2002 70 0`, `2000 70 -2`.

### Form (summary only — A6a `sky_battle/README.md` owns the detail)
A four-team Sky Battle carnival inside a 105×105 barrier box: four **gift-box spawn pods** at the cardinal points, four **loot towers built as wool hot-air balloons** at the diagonals (orange, cyan, lime, purple), a circular **covered gallery** (stone floor y25, team-coloured canopy y27–37, lower deck y19), and a small plus-shaped mid at y28. Teams spawn on the gift boxes and **bridge ~9 blocks** to the gallery rim (7-block bounding gap plus 2 blocks of box wall); the central chain at `2000 56 0` is the lobby-to-mid entrance, **not** the team entry. **H** geometry; **M** intended bridge route.

Loot/containers: **19 container block entities** — **13 loot** (4 white shulkers, 5 barrels, 4 chests; **93 item entries**) plus **6 empty** (5 campfires, 1 lobby lectern). The 93 `item replace block` commands refill the loot on reset. **H.** Full list: `data/containers.csv`; detail in A6a.

Palette: barrier 34,751; white concrete 4,545; stone bricks 1,231; red 984; yellow 725; green 725; blue 570; blue/red/yellow/green terracotta ~360 each; gray wool 287; smooth quartz 256; oak fences 240; orange/cyan/lime/purple wool 223 each; cobblestone wall 164; oak leaves 138. **H**.

Completeness: **H** — shell, teams, loadout, 13 loot containers + 6 empty container block entities, start/win logic, team signs and "press when bugged" reset are present. **Build complete; loop has known defects** — the bug reset destroys containers and the wool sweep is partial. **A6a** (`sky_battle/`) owns the defect list, loot table, walk graph and border detail. The sky lobby sits *inside* the same copy box; do not crop it away.

### What A6a should not miss
1. The roof/chain at `2000 56 0` is the **lobby-to-mid** shaft, not the team entry; teams bridge ~9 blocks from the gift boxes.
2. Team spawn `y=23` vs. terrain top `y=42` — the ring is taller than the team islands.
3. `Sky_Battle_Yellw` is misspelt in the commands (not `Yellow`); any re-implementation must match or fix it.
4. The 47,874-block count includes 34,751 barrier blocks; the visible build is 13,123 blocks.

---

## 4. PRIORITY — Parkour Tag (`MG-PT-01`)

### Bounds
- **Copy envelope: `x 1035..1078, y 0..58, z 972..1017`** (44 × 59 × 46).
- Playable course: `x 1048..1071, y 10..26, z 978..1010`.
- Gallery / barrier roof: `x 1043..1078, y 27..39, z 977..1012` (the flat gallery sits on the invisible barrier roof; it is **not** the waiting lobby).
- Command machine + underground waiting lobby: `x 1043..1071, y 0..9, z 981..1006`; waiting lobby room at `1064 1 985` / `1058 1 985`.
- Giant "PARKOUR TAG" banner at the west edge: `x 1035..1036, y 48..58, z 972..1017` (the only blocks in that band; include it or crop it deliberately).
- **Entry / arrival: `1061 28 995`** (gallery, on the barrier roof).
- **Main-hub pad: `-212 10 975`** — orange carpet, XP level 4; command `tp 1061 28 995`.
- **Runner start: `1061 17 982`** (`tp @a[level=6]`); **Hunter start: `1060 17 1007`** (`tp @a[level=7]`).
- Runner teleporter sign `1050 29 991`; Hunter teleporter sign `1050 29 1000`.
- Respawn: `spawnpoint @s 1061 28 995` for runners (gallery respawn; not the waiting lobby).

### Form (summary only — A6b `parkour_tag/README.md` owns the detail)
A circular **stadium bowl** (tiered cyan-terracotta/stone seating) with the parkour course in the pit, a floating red "PARKOUR TAG" banner outside the west rim, and a gallery/spectator bowl around the invisible barrier roof at y27..39. The waiting lobby is **underground** at `1064 1 985` / `1058 1 985`, not the y27 deck. The logic is fully local: 123 command blocks inside the same envelope, a 60-second bossbar (`Parkour Tag - Timer`), hunter/runner team assignment, on-screen 3-2-1 countdown, runner-count settings (1–4), hunter settings, win/lose `tellraw`, fireworks and sounds. **H.** Containers: **3 container block entities** — 1 empty barrel at `1067 12 992` plus 2 empty lecterns at `1049 28 993` / `1049 28 996`; no loot. **H.**

Palette: gray concrete 1,849; cyan terracotta 1,073; stone bricks 708; smooth quartz 609; white concrete 509; dead horn coral block 441; barrier 401; brick slab 379; black concrete 328; deepslate bricks 298; red concrete 251; waxed copper family 151+137+106+57. **H.**

Completeness: **H** — the loop is fully local (no commands outside this envelope except the hub pad). **Build complete; loop has known defects** — broken hunter-win `tellraw`, timer-stop commands target the player score instead of `fakeplayer`, and the bossbar selector misses top perches. **A6b** (`parkour_tag/`) owns the defect list, routes and climbables.

### What A6b should not miss
1. The course floor is at y≈10–26; the gallery/barrier roof is y27–39; the waiting lobby is underground at `1064 1 985` / `1058 1 985`; the banner is y48–58.
2. Barrier gate at `1049 28 999` toggles with the hunter's on/off switch (`fill … barrier/air`).
3. The 3-2-1 countdown is in `title` commands at `x1044/1048/1052, y5..8, z995`.
4. `fakeplayer timer_parkourtag` is the timer; bossbar id `minecraft:1`.

---

## 5. Other arenas (short)

- **`MG-HUB-01` main lobby** — a 63×45×81 gold "crown" ring on a dark plinth, with a hot-air balloon inside and a separate south deck at y8–10 carrying eight matching pads. It is the world spawn. Only **4 of the 8 deck pads are wired**: orange `-212 10 975` (Parkour Tag), yellow `-208 10 970` (Sky Battle), lime `-205 10 970` (Super Clash), blue `-201 10 976` (Sky Race hub); the red, light-blue, pink and magenta pads have no teleport commands. The crown floor (y21) and the pad deck (y10) have **no block path** — `z 951..960` is empty at every y; the only links are teleports back to `-196 24 927`. **H.** Containers: **8 empty** (3 barrels, 2 chests, 2 shulkers, 1 campfire). See `other/README.md` (A6c) for the 8-bay flow; not duplicated here.
- **`MG-SC-01` Super Clash** — 91×20×37 glass-roofed arena with a painted blue sky/cloud mural, two dark tower props east of the glass box, and trees. Teams `Super_Clash_H` (heroes) / `Super_Clash_V` (villains); 5 classes (Healer, Knight, Drunkard, Swordsman, Wizard); first to 10 kills; full kit commands. Entry `5000 21 0`, heroes `4972 29 -15`, villains `4926 20 -15`. **Bug:** the villain kill objective `kill_sc_vp` uses criterion **`teamkill.green`** while villains join **`Super_Clash_V`** (heroes use `playerKillCount`), so the villain side likely cannot reach 10 through that chain — **H** criteria, **M** effect. Containers: **3 empty** (2 barrels, 1 lectern). See A6c `other/README.md` for the class/kit table.
- **`MG-SR-HUB-01` Sky Race select** — 19×11×17 brick kiosk with six colour pillars. Only red (`level 101 → 21000 31 20000`) and orange (`103 → 21000 31 21000`) are wired; yellow/green/blue/purple signs exist but have no pads. **H** for 2/6, **M** that the other four were planned. Container: 1 empty lectern at `20005 41 20000`.
- **`MG-SR-RED-01` / `MG-SR-ORG-01`** — 80×83×43 aerial wool-course envelopes: black start tower, glass gates, finish basket, all over void. 49 commands each (countdown, wool give, shears, `finished the … course!`, white-wool reset fills). **Red climbs y13..82 with 98 chains and grey cloud props; Orange runs y13..54 with 24 cobwebs and no chains** — only the start tower, finish basket and repeated gate rhythm match; Orange is **not** a reskin. **H** (A6c). The wool/shears wall signs **match** the loadout: 64 white wool in the offhand + shears with `CanDestroy:[white_wool]`; they are not leftover template text. **H** signs/commands.
- **`MG-CMD-01` Sky Battle command room** — 79×15×35 white platform with 474 command blocks and 57 signs. This is the **Sky Battle brain**: team assignment, loadout, chest/barrel looting, wool reset and win checks. The commands reference `Sky_Battle_*` teams or the arena's coordinates; no Parkour Tag / Super Clash / Sky Race logic was found here. Sign: "at 1000 10 10 are all the command blocks". **H.**
- **`MG-REB-01/02`** — an isolated 11×11 kiosk at `-50000 -50000` and a 135×60 linear white-concrete course at `-50000 -51000` over lava, connected by an XP-level 200/201 teleport. Only 3 commands. **M** that it is a parkour/race demo; its name is "Rebecca's Minigames".

---

## 5b. Command-block clusters (game-logic index)

All 942 command blocks were grouped by 3D proximity (union-find, 8-block radius), producing **25 spatial clusters**. Cluster bounds are *logic* bounds, not arena bounds. The cluster's parent arena is the island that contains it. Full machine table: `data/command_clusters.csv`; `data/arenas.csv` now carries a `command_clusters` column per arena.

| Arena | Clusters | Cmds | Bounds (x / y / z) | Role evidence |
|---|---|---:|---|---|
| MG-HUB-01 | CB-03 (39) | 39 | -215..-205 / 11..19 / 961..978 | pad detection, team join, scoreboard reset, gamemode/XP/inventory |
| MG-HUB-01 | CB-04 (22) | 22 | -233..-212 / 19..22 / 907..915 | timed `setblock` scenery/clock |
| MG-HUB-01 | CB-05..CB-13 (9 clusters) | 10 | spread -206..-186 / 19 / 921..941 | one-off `fill` effects |
| **MG-CMD-01** | **CB-14 (445)** | 445 | 926..989 / 10..23 / -18..11 | Sky Battle teams, loadout, loot, wool reset, win checks |
| **MG-CMD-01** | **CB-15 (29)** | 29 | 993..1004 / 15..16 / -16..-8 | Sky Battle team join/effects/buttons |
| MG-PT-01 | CB-16 (121) | 121 | 1044..1071 / 1..9 / 983..1003 | Parkour Tag timer, countdown, settings, win/lose |
| MG-PT-01 | CB-17 (2) | 2 | 1050..1052 / 12 / 979 | two `setblock` triggers |
| MG-SB-01 | CB-18 (3) | 3 | 1999..2001 / 66..69 / -1..0 | start/reset/kill buttons in sky lobby |
| MG-SC-01 | CB-19 (161) | 161 | 4976..5000 / 20..32 / -28..0 | Super Clash classes, kits, kill counters, win |
| MG-SC-01 | CB-20 (3) + CB-21 (1) | 4 | 5006..5008 / 20..23 / -21..0 | class join / saturation edge cases |
| MG-SR-HUB-01 | CB-22 (4) + CB-23 (1) | 5 | 20004..20013 / 39..42 / 19995..20000 | level pads, item cleanup |
| MG-SR-RED-01 | CB-24 (49) | 49 | 20998..21002 / 15..30 / 19995..20005 | Red course countdown/wool/shears/finish |
| MG-SR-ORG-01 | CB-25 (49) | 49 | 20998..21002 / 15..30 / 20995..21005 | Orange course (lower/flatter than Red; chains vs cobwebs) |
| MG-REB-01 | CB-02 (2) | 2 | -50003 / 97..98 / -49998 | XP 200/201 → course teleport |
| MG-REB-02 | CB-01 (1) | 1 | -49916 / 130 / -51001 | jump-boost pad |

**Cluster → game identification (H):**
- The **Sky Battle** logic splits into **CB-14/15 in the command room (474)** plus **CB-18 in the arena (3)**. A scan that only looked inside the arena would find 3 commands and might misclassify the game as a simple wool arena.
- **Parkour Tag** is fully self-contained in **CB-16/17 (123)**; the cluster's commands name `timer_parkourtag`, `death_parkourtag`, `kill_parkourtag`.
- **Super Clash** is fully self-contained in **CB-19/20/21 (165)**; commands name `Super_Clash_H/V`, `kill_sc_h/v`, class XP levels 17–21.
- **Sky Race** is local to each course (**CB-24/25, 49 each**) plus the selection kiosk (**CB-22/23, 5**).
- The **hub** clusters (CB-03..13) contain no game rules, only pad detection and resets; they point to the four games.
- The **Rebecca** clusters (CB-01/02, 3 commands) are too small to identify a game with confidence.
- **No cluster lies outside the 10 built islands**; there is no hidden command-only arena. **H.**

---

## 6. How the arenas are separated — and whether they can be copied out

### Separation
- **Void gaps, not walls.** Centre-to-centre distances: lobby→Parkour Tag ≈ 1,261 blocks; Parkour Tag→Sky Battle ≈ 1,371; Sky Battle→Super Clash ≈ 2,967; Super Clash→Sky Race hub ≈ 25,032; Sky Race hub→Rebecca ≈ 99,654. **H** (coordinates).
- **No physical connection**: no rails, roads, bridges, or shared terrain. The only links are command teleports and the hub's four carpet pads. **H.**
- **Chunk clusters are disjoint.** Each arena occupies its own set of chunk columns; no two arenas share a chunk column. **H** (chunk sets).
- **Region files are disjoint too** (computed from block positions):

| Arena | Chunk columns | Chunk range (x, z) | Region files |
|---|---|---|---|
| MG-HUB-01 | 22 | -15..-11, 56..61 | `r.-1.1` |
| MG-SB-01 | 64 | 121..128, -4..3 | `r.3.-1`, `r.3.0`, `r.4.-1`, `r.4.0` |
| MG-PT-01 | 13 | 64..67, 60..63 | `r.2.1` |
| MG-SC-01 | 20 | 307..313, -2..0 | `r.9.-1`, `r.9.0` |
| MG-SR-HUB-01 | 4 | 1249..1250, 1249..1250 | `r.39.39` |
| MG-SR-RED-01 | 22 | 1311..1316, 1248..1251 | `r.40.39`, `r.41.39` |
| MG-SR-ORG-01 | 16 | 1311..1316, 1311..1313 | `r.40.41`, `r.41.40`, `r.41.41` |
| MG-CMD-01 | 14 | 57..62, -2..1 | `r.1.-1`, `r.1.0` |
| MG-REB-01 | 4 | -3126..-3125, -3126..-3125 | `r.-98.-98` |
| MG-REB-02 | 17 | -3126..-3117, -3188..-3185 | `r.-98.-100` |

### Can each be copied independently?
**Yes, geometrically — with four caveats.**

1. **Copy by chunk range, not raw `.mca`.** Each arena's region set is disjoint, but the region files also contain hundreds of empty stub chunks (18,432 world-wide). A raw region copy works but carries megabytes of stubs. Every **named game** arena fits inside a **128³ structure volume** (largest: Sky Battle 105×81×105; Sky Race 80×83×43); only the 135-block-long Rebecca course exceeds one axis. A structure-block/schematic export is therefore feasible for the games. **H** for sizes, **M** for structure-block behaviour on 1.17 TEs.
2. **Include the block entities.** All logic and all signage lives in **942 command blocks + 187 signs**. A block-only export (e.g. a renderer that ignores TEs) loses the games. **H.**
3. **Keep the barrier shells.** Sky Battle (34,751 barriers), Sky Race Red (20,548) and Orange (20,597) are mostly barrier volume; strip barriers and the arena loses its ceiling/walls/boundaries. **H.**
4. **All coordinates are absolute.** Every teleport, `fill`, `setblock`, `spawnpoint`, bossbar selector and `distance`/`x..dx` selector is hard-coded to the current world coordinates (e.g. `tp @s 2000 78 0`, `fill 21004 0 19980 21012 81 20020 air replace white_wool`, `execute … x=21002,y=-80,z=19979,dx=67,dy=20,dz=58`). Moving an arena requires rebasing its commands as well as its blocks. **H.**
5. **Cross-arena dependencies exist.** Sky Battle's start, team assignment and loot are in `MG-CMD-01` (≈1,000 blocks away), not in the arena. Parkour Tag, Super Clash and Sky Race are self-contained (their command blocks are inside their own envelopes). If you lift Sky Battle alone, also lift or rewrite the command room. **H.**

---

## 7. Wiring map (for whoever integrates this)

```
MAIN LOBBY (MG-HUB-01)  spawn -196 24 927
  pad  -212 10 975  orange  XP 4   ->  1061 28 995   Parkour Tag
  pad  -208 10 970  yellow  XP 8   ->  2000 78 0     Sky Battle sky lobby
  pad  -205 10 970  lime    XP 15  ->  5000 21 0     Super Clash
  pad  -201 10 976  blue    XP 100 ->  20000 41 20000 Sky Race hub

SKY RACE HUB (MG-SR-HUB-01)
  red    101 -> 21000 31 20000
  orange 103 -> 21000 31 21000
  (yellow/green/blue/purple signs only)

SKY BATTLE COMMAND ROOM (MG-CMD-01)  ~474 commands
  Sky Battle teams, loadout, chest/barrel spawns, start/stop, wool reset
  team detection, scoreboard, bossbars
```

The hub pads work by **XP level** state, not buttons: stepping on the carpet while at that level triggers a repeating command block. Resetting XP levels is part of the lobby's "Xp reset" sign. **H.**

The map targets **1.17.1** command syntax. It contains old-style NBT item syntax (`item replace … with red_wool{CanPlaceOn:[…]}`) and `spawnpoint @s x y z`; both need checking under 1.21 (A12 owns compatibility). **H** for syntax, **M** for behaviour.

---

## 8. Data quality, contradictions, open questions

1. **Brief vs source vs A12: command blocks.** The original brief said 0 command blocks / 0 block entities. **A12 independently found 942 command blocks in `mcc_minigames`**, and this source scan also finds **942** (548 chain, 212 repeating, 182 impulse). The counts now agree; the "0" was wrong. The source also has **187 signs** (1,129 command/sign block entities; containers are separate); live signs were not counted by A12, so 1,129 is a source figure. **H** for 942; **M** that the live world still carries all 187 signs.
2. **"18,628 chunks" is not built area.** Only 196 chunk columns contain blocks; the rest are empty stubs. **H.**
3. **`Gladiators` and `Theme Race` have no arena.** Signs only; no pads/commands/blocks point anywhere. **M** disabled/unbuilt.
4. **"Rebecca's Minigames" identity is unproven.** Visual + jump-boost command suggest parkour/race; only 3 commands exist. It could be the event-only `Theme Race` course, but the only signs at both ends name "Rebecca's Minigames"; **M** for parkour/race, **L** for any link to Theme Race.
5. **The icon shows the lobby ring**, not a game arena. **M.**
6. **1.17.1 world format**: shared tools fail; use `data/dump_117_mcmdump.py`. **H.**
7. **Containers:** **34 container block entities** in the source — 11 barrels, 6 campfires, 6 chests, 5 lecterns, 6 shulker boxes; **21 empty** and **13 Sky Battle loot** containers with **93 item entries**. Full source list: `data/containers.csv`; loot detail in A6a/A6b. Loot is also re-written by command at reset. **H.**
8. **Dimensions:** the source has no region folders under `DIM-1`/`DIM1`, so all source arenas are overworld. If the live `mcc_minigames` has non-empty Nether/End region folders, those were not present in the source download and are not covered here. **H** source, **L** live-only dimensions.

---

## 9. Files in this folder

```
README.md                     this dossier
STATUS.md                     early-published Sky Battle / Parkour Tag bounds + open questions
REFERENCE_IMAGES.md           every cited image, what it shows, what to take
frames/                       12 convenience copies (12 distinct views; +2 composites = 14 distinct images)
renders/                      all MCMDUMPs + overview/iso/atlas/world map
data/arenas.csv               arena index (ID, bounds, spawn, palette, containers, evidence, detail-report pointer, confidence)
data/islands.csv              per-island bounds/counts (machine summary)
data/regions.csv              chunk/region-file ranges for each arena (copy planning)
data/entries.csv              game -> entry point -> how reached
data/blocks_positions.csv     every non-air block: island,x,y,z,block
data/blocks_by_island.csv     palette counts per island
data/signs.csv                187 signs with raw JSON text
data/command_blocks.csv       942 command blocks: kind, auto, condition_met, exact command
data/command_clusters.csv     25 spatial command-block clusters with arena mapping and role evidence
data/tile_entities.csv        signs + command blocks combined
data/containers.csv           34 container block entities (arena, x/y/z, items, empty, loot, contents; A14 source scan)
data/entities.csv             8 non-block entities (eyes of ender, item frames, armor stand)
data/poi_types.csv            16 POI records (villager workstations; no command/sign POIs)
data/chunk_scan.csv           per-chunk occupancy scan (196 rows)
data/dimension_scan.csv       overworld vs DIM-1/DIM1 coverage (no dimension regions)
data/palette.md               palette and material notes per arena
data/scan_chunks.py           fast occupancy scanner
data/build_inventory.py       authoritative island/block/TE export
data/dump_117_mcmdump.py      1.17 → MCMDUMP adapter used with the shared renderer
data/*.py                     other one-purpose analysis scripts (read-only)
```

## 10. Explicitly not done

- No Sky Battle layout deep dive (islands, chest routes, border) — **A6a**.
- No Parkour Tag route/readability deep dive — **A6b**.
- No other-arena catalogue beyond the table above — **A6c**.
- No Ace Race or TGTTOS analysis (excluded by brief).
- No build, no plugin code, no registry edits, no server commands.
