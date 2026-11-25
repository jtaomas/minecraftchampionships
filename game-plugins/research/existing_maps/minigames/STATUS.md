# STATUS — A6 Minigames Cartographer

Updated: 2026-11-25 (priority bounds first published 2026-11-25; unchanged)
Source: `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3`
Live world: `mcc_minigames` (not touched; no RCON used)
Method: read-only 1.17.1 region decode (`data/scan_chunks.py`, `data/build_inventory.py`, `data/dump_117_mcmdump.py`), tile-entity export, MCMDUMP renders through shared `tools/render_preview.py`.

Detail report pointer: **A6a** (`sky_battle/`), **A6b** (`parkour_tag/`) and **A6c** (`other/`) own layout, loot, routes and bugs. This file is the world/arena index and does not duplicate them.

## A12 correction — command blocks

A12 independently found **942 command blocks** in `mcc_minigames`. The source scan in this folder also finds **942** — 548 chain, 212 repeating, 182 impulse; 731 `auto=1`; plus **187 signs** (1,129 command/sign block entities; containers are separate). The earlier "0 command blocks / 0 block entities" is superseded. The 942 are grouped into **25 spatial clusters** (`data/command_clusters.csv`), and the arena index now carries a `command_clusters` column.

Coverage: overworld only — `DIM-1`/`DIM1` contain **no region folders**, no custom `dimensions/` exists; `poi` (27 files, 21 chunks, 16 villager-workstation records) and `entities` (81 files, 8 entities) were also scanned. Containers: **34 container block entities** — 11 barrels, 6 campfires, 6 chests, 5 lecterns, 6 shulkers; **21 empty**, **13 Sky Battle loot** (93 items). Full list: `data/containers.csv`. **H.**

## PRIORITY — Wave 2 bounds (stable)

### Sky Battle — `MG-SB-01`
- **Full arena envelope (copy box): `x 1948..2052, y 0..80, z -52..52`**
  (105 × 81 × 105 blocks; 47,874 non-air blocks)
- Playfield / barrier shell: `x 1948..2052, y 0..56, z -52..52`
  - outer barrier walls at x=1948, x=2052, z=-52, z=52; solid barrier roof at y=56
  - **entry chain at `2000 56 0`** (single chain in the roof; lobby-to-mid shaft, not the team entry)
  - built terrain: y 11..42; team spawn level: y 23
- Sky lobby above the roof: `x 1993..2007, y 57..80, z -7..7`
- **Lobby arrival / entry point: `2000 78 0`**
- **Main-hub entry pad: `-208 10 970`** (yellow carpet; XP level 8) → `2000 78 0`
- **Team spawns (from commands):**
  - Red `1970 23 0` · Blue `2000 23 30` · Green `2030 23 0` · Yellow `2000 23 -30`
- **Form:** four gift-box spawn pods, four **loot towers built as wool balloons** (orange/cyan/lime/purple), and a **circular covered gallery**; teams **bridge ~9 blocks** from spawn to the gallery rim. The chain at `2000 56 0` is the lobby-to-mid shaft, not the team entry. **H** geometry; **M** intended bridge.
- **Containers: 19** container block entities — **13 loot** (4 shulkers, 5 barrels, 4 chests; 93 items) plus **6 empty** (5 campfires, 1 lobby lectern). Detail: A6a / `data/containers.csv`.
- Palette: barrier shell, white concrete, team concrete + terracotta + wool (red/yellow/green/blue), stone bricks, smooth quartz, oak fences/leaves, gray-wool lobby.
- Confidence: **H** bounds/coords; **H** identity (10 signs say "Sky Battle"; team objectives `Sky_Battle_*`).

### Parkour Tag — `MG-PT-01`
- **Full arena envelope (copy box): `x 1035..1078, y 0..58, z 972..1017`**
  (44 × 59 × 46 blocks; 8,375 non-air blocks)
- Playable course: `x 1048..1071, y 10..26, z 978..1010`
- Gallery / barrier roof: `x 1043..1078, y 27..39, z 977..1012` (flat gallery on the invisible barrier roof; **not** the waiting lobby)
- Command machine + underground waiting lobby: `x 1043..1071, y 0..9, z 981..1006`; waiting lobby at `1064 1 985` / `1058 1 985`
- "PARKOUR TAG" banner (west edge): `x 1035..1036, y 48..58, z 972..1017`
- **Gallery entry: `1061 28 995`** (not the waiting lobby)
- **Main-hub entry pad: `-212 10 975`** (orange carpet; XP level 4) → `1061 28 995`
- **Runner start: `1061 17 982`** (`tp @a[level=6]`) — Runner Teleporter sign `1050 29 991`
- **Hunter start: `1060 17 1007`** (`tp @a[level=7]`) — Hunter Teleporter sign `1050 29 1000`
- **Containers: 3** — 1 empty barrel at `1067 12 992` plus 2 empty lecterns at `1049 28 993` / `1049 28 996`; no loot. Detail: A6b / `data/containers.csv`.
- Palette: gray concrete, cyan terracotta, stone bricks, smooth quartz, dead horn coral block, waxed copper oxidation family, brick slabs, barrier.
- Confidence: **H** bounds/coords; **H** identity (signs "Parkour Tag", bossbar commands).

## All arenas (final IDs)

| ID | Guess | Envelope (x / y / z) | Entry | Conf. |
|---|---|---|---|---|
| MG-HUB-01 | Main lobby ("crown" hub) | -235..-173 / 8..52 / 904..984 | `-196 24 927` (world spawn) | H |
| MG-SB-01 | Sky Battle | 1948..2052 / 0..80 / -52..52 | `2000 78 0` | H |
| MG-PT-01 | Parkour Tag | 1035..1078 / 0..58 / 972..1017 | `1061 28 995` | H |
| MG-SC-01 | Super Clash | 4922..5012 / 17..36 / -30..6 | `5000 21 0` | H |
| MG-SR-HUB-01 | Sky Race level select | 19995..20013 / 39..49 / 19992..20008 | `20000 41 20000` | H |
| MG-SR-RED-01 | Sky Race — Red course | 20991..21070 / 0..82 / 19979..20021 | `21000 31 20000` | H |
| MG-SR-ORG-01 | Sky Race — Orange course | 20991..21070 / 0..82 / 20979..21021 | `21000 31 21000` | H |
| MG-CMD-01 | Sky Battle command room | 926..1004 / 9..23 / -18..16 | none (service room) | H |
| MG-REB-01 | "Rebecca's Minigames" kiosk | -50005..-49995 / 97..104 / -50005..-49995 | `-50000 100 -50000` | M |
| MG-REB-02 | "Rebecca" linear course | -50005..-49871 / 99..131 / -51005..-50946 | `-50000 100 -51000` | M |

**Index notes (detail in A6c):** `MG-HUB-01` has **4 of 8 deck pads wired** (orange/yellow/lime/blue) and **no block path** between the crown floor (y21) and pad deck (y10): `z 951..960` is empty at every y. `MG-SC-01`'s villain kill objective `kill_sc_vp` uses **`teamkill.green`** while villains join **`Super_Clash_V`** (heroes use `playerKillCount`); likely villain-side bug — **H** criteria, **M** effect.

**Containers (34 total, 21 empty, 13 loot/93 items):** HUB **8**, SB **19** (13 loot + 6 empty), PT **3**, SC **3**, SR-HUB **1**; SR-RED, SR-ORG, CMD and both Rebecca clusters have **0**. Full coordinate list: `data/containers.csv`; per-arena column: `data/arenas.csv`.

**Completeness wording:** every “complete” label in this folder means **build complete**, not bug-free. The loops have known defects; **A6a**, **A6b** and **A6c** own the defect lists and routes.

### Command-block clusters (game logic)

`data/command_clusters.csv` has all 25 clusters. Grouped by owning arena:

| Arena | Clusters | Cmds | What the cluster proves |
|---|---|---:|---|
| MG-CMD-01 | CB-14 (445), CB-15 (29) | 474 | Sky Battle teams, loadout, loot, wool reset, win checks — **not inside the arena** |
| MG-SB-01 | CB-18 (3) | 3 | only the start/reset/kill buttons are local |
| MG-PT-01 | CB-16 (121), CB-17 (2) | 123 | `timer_parkourtag`, `death_parkourtag`, `kill_parkourtag`, runner/hunter settings |
| MG-SC-01 | CB-19 (161), CB-20 (3), CB-21 (1) | 165 | `Super_Clash_H/V`, `kill_sc_h/v`, class XP levels |
| MG-SR-RED-01 | CB-24 (49) | 49 | course countdown, wool/shears, finish, reset |
| MG-SR-ORG-01 | CB-25 (49) | 49 | orange course; different upper layout from Red (A6c) |
| MG-SR-HUB-01 | CB-22 (4), CB-23 (1) | 5 | level pads + item cleanup |
| MG-HUB-01 | CB-03 (39), CB-04 (22), CB-05..13 (9) | 71 | pad detection, clock, one-off fills |
| MG-REB-01 | CB-02 (2) | 2 | XP 200/201 → course |
| MG-REB-02 | CB-01 (1) | 1 | jump-boost pad |

**Why this matters:** the Sky Battle arena contains only 3 command blocks; the game is identified by 474 commands 1,000 blocks away. A block-only or in-arena-only scan would miss the game type, team spawns and win conditions. **H.**

## Critical caveats for Wave 2

1. **Source is 1.17.1 (DataVersion 2730).** Shared `tools/world_to_mcmdump.py` and `render_preview.py` assume 1.18+ `sections`/`block_states`; they return nothing on this world. I wrote a 1.17 adapter (`data/dump_117_mcmdump.py`) and used the shared renderer unchanged. Reported to coordinator; not a shared-tool edit.
2. **Command blocks and signs:** **942 command blocks** (548 chain, 212 repeating, 182 impulse; 731 auto) — **A12 confirms the same 942 in the live world** — plus **187 signs** in the source (1,129 command/sign block entities; containers separate). Counts agree; no live discrepancy on the command-block count. The signs' live count is not confirmed. All are exported to `data/tile_entities.csv` and grouped into 25 clusters in `data/command_clusters.csv`.
3. **Arenas are separated by void, not walls.** Each cluster sits thousands of blocks from the next; chunk clusters and region-file sets are disjoint. See `README.md` §6.
4. **Region-file copying is not safe by itself** — each arena spans 1–4 region files and shares them with nothing else, but `.mca` also carries hundreds of empty stub chunks. Copy by block/chunk bounds (structure/schematic), not by raw region file, unless the stubs are acceptable.
5. **All command coordinates are absolute.** Copying geometry without rebasing commands breaks teleports, fills and selectors. **H.**
6. **Sky Battle's game logic is not in the arena** — its start/teams/loadout/loot commands live in `MG-CMD-01` (CB-14/15, 474 commands), ~1,000 blocks away. PT, SC and SR are self-contained.
7. **Dimensions:** source `DIM-1`/`DIM1` contain no region folders; all source arenas are overworld. If the live world has Nether/End regions, they were not in the source download and are out of scope for this survey. **H** source, **L** live.

## Open questions
- Whether the live world still has all 187 signs: A12 confirms 942 command blocks, but the sign count was not reported. **M** that the source sign set is intact.
- Whether `Gladiators` and `Theme Race` were ever built: signs only, no arena blocks/pads/commands. **M** disabled/unbuilt.
- Whether the far-west "Rebecca" course is a real game or an author easter egg/demo: only 3 commands. **M.**
- Yellow/green/blue/purple Sky Race levels are labelled but unwired. **H** (no pads); **M** planned.

## Done
- Full chunk occupancy scan (196 non-empty chunk columns of 18,628 present), all dimensions checked.
- Exact bounds and block inventories for all 10 built clusters.
- Sky Battle + Parkour Tag priority bounds published.
- Arena index CSV, tile-entity CSVs, block-position CSV, palette notes.
- 25 command-block clusters mapped to arenas (`data/command_clusters.csv`; `command_clusters` column in `arenas.csv`).
- `data/containers.csv`: 34 container block entities (21 empty, 13 Sky Battle loot / 93 items).
- 12 convenience frame copies (byte-identical to 12 distinct renders); 14 distinct image views including atlas and world map.
- Separation and copyability analysis (regions, chunks, barrier shells, absolute commands).
