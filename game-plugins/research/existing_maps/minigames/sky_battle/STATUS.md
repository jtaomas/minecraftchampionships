# STATUS — A6a Sky Battle deep dive (MG-SB-01)

Updated: 2026-11-25
Owner: A6a. Folder: `game-plugins/research/existing_maps/minigames/sky_battle/` only.
Source: 1.17.1 `daniellaur's minigames version 3` (DataVersion 2730).
Method: A6's `../data/dump_117_mcmdump.py` → shared `tools/render_preview.py` (unchanged); block/command/NBT parsing in this folder. No RCON, no server writes, no build, no plugin code.

## Done

- [x] Read `PLAN.md`, A6 `README.md`, `STATUS.md`, `data/arenas.csv`, `data/command_clusters.csv`, `data/command_blocks.csv`.
- [x] Exact arena envelope and 27-structure layout (`data/layout.csv`).
- [x] Ring/gallery/canopy/lower-deck section analysis (`frames/14_section_east_west.png`, `frames/15_section_north_south.png`).
- [x] Four gift-box spawn pods, four balloons, mid, sky lobby, void edges, border.
- [x] 10 signs (`data/signs.csv`), 4 arena marker entities (`data/entities.csv`).
- [x] **19 container block entities = 13 loot containers (93 items) + 5 campfires + 1 lectern; all non-loot are empty** (`data/containers.csv`, `data/containers.md`). The 13 loot containers are in `data/chests.csv`; all 13 were scanned in the source NBT and their contents match the 93 commands 93/93 (`data/scan_containers.py`, `data/container_nbt.csv`).
- [x] 477 Sky Battle commands classified (`data/commands.csv`); 40 legacy-NBT commands flagged.
- [x] Required-name command export `data/command_blocks.csv` = 477-row alias of `data/commands.csv` (identical columns/rows).
- [x] 110 reset fills, 22 z-strips, 5 block types.
- [x] Palette/blocks (`data/blocks.csv`, `data/blocks_by_region.csv`, `data/palette.md`).
- [x] Gameplay loop from commands: start, team assignment, kit, loot, border, death, win, reset (`data/flow.md`).
- [x] 1.21.1 compatibility risks (40 commands; exact fix patterns).
- [x] Read-only comparison with `game-plugins/SkyBattle` (`SkyBattleGame.java`, `Arena.java`, `LootTable.java`).
- [x] 17 full-resolution frames, opened and described (`REFERENCE_IMAGES.md`); re-checked 2026-11-25 after A14 QA (all paths/dimensions match).
- [x] `README.md` with H/M/L claims.

## Reconciliation with A6 (2026-11-25)

**Date-stamped QA note:** the earlier A6a correction rows “A6 says 0 containers” and “A6 says 161 fill commands” are withdrawn. A6's current `README.md` records Sky Battle as **13 pre-placed containers** (4 white shulkers, 5 barrels, 4 chests) plus a separate Parkour Tag barrel (13+1), and makes no 161-fill claim. A6a defines the Sky Battle 13 as **13 loot containers**, then adds 6 non-loot block entities (5 campfires + 1 lectern) for **19 total**. The substantive A6a findings remain:

1. **19 container block entities = 13 loot containers (93 items) + 5 campfires + 1 lectern; all non-loot are empty.** **H** (`data/containers.csv`).
2. The four “corner chests” are **inside the diagonal balloons**, not on the ring/islands. **H**.
3. The 8 iron blocks are **gift-box ribbon knots**, not border markers. **H**.
4. The ring is a **covered gallery** (floor y25 + canopy y27-37 + lower deck y19), not a solid tiered colosseum. **H**.
5. The `fill` set is **162** commands: **110** wool/cobweb sweep + **52** cages/glass/border/destroy/iron. This is a refinement, not an A6 error. **H**.

## Key numbers

| Item | Value | Conf. |
|---|---:|---|
| Envelope | 105 × 81 × 105 (`1948..2052, 0..80, -52..52`) | H |
| Non-air blocks | 47,874 | H |
| Barrier | 34,751 (72.59%) | H |
| Visible build | 13,123 | H |
| Block types | 59 | H |
| Sky Battle commands | 477 (CB-14 445, CB-15 29, CB-18 3) | H |
| Container block entities | 19 = 13 loot containers + 5 campfires + 1 lectern (non-loot empty) | H |
| Loot containers | 13 (4 white shulker boxes, 5 barrels, 4 chests) | H |
| Loot commands | 93 | H |
| Reset fills | 110 (22 strips × 5 types) | H |
| Legacy-NBT commands | 40 (36 item + 4 firework) | H |
| Signs | 10 | H |
| Arena entities | 4 | H |
| Iron knots | 8 (2 per gift box) | H |

## Blocked / not done

- **No live-server check.** The brief says read-only; no RCON, no in-game light test, no command execution. Whether barrier passes skylight and whether the start button refills containers are **M**, not H.
- **No full redstone trace.** The 474 logic commands are 1,000 blocks from the arena; I read every command and the scoreboard, but did not map every repeater/redstone line. Button→chain order is **M**.
- **No Parkour Tag / Super Clash / Sky Race / hub analysis** — out of scope by brief.
- **No build, no game/plugin code, no registry, no server restart.**
- **No 1.21 test instance** — compatibility is a syntax audit, not a runtime test.

## Open questions

1. Does the barrier roof block skylight in 1.17.1/1.21.1? If yes, the gallery needs hidden light. **M**.
2. Does the Start button fire the 93 container-fill commands every round, or only the normal reset? **M**.
3. Is the “bug reset” meant to be one-way (schematic restore required)? It destroys the 13 loot containers and does not recreate them. **H** commands; **M** intent.
4. Why do Red and Green both have 50 `POINTS` in the snapshot? **M** test residue.
5. Was the lower deck y19 intended as a second route or only a fall-catch? **M/L**.

## Files to read first

1. `README.md` — full dossier.
2. `REFERENCE_IMAGES.md` — 17 images, all opened.
3. `data/layout.csv` — structures and bounds.
4. `data/containers.csv` / `data/containers.md` — all 19 container block entities; `data/chests.csv` / `data/loot.csv` — the 13 loot containers and 93 items.
5. `data/commands.csv` (and required-name alias `data/command_blocks.csv`) — every command and 1.21 risk.
6. `data/flow.md` — gameplay loop narrative.
7. `data/palette.md` — palette/lighting.
8. `data/scoreboard.md` — objectives/teams/XP state.
