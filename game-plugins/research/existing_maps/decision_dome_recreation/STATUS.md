# STATUS — Decision Dome Recreation (A5)

**State:** **DONE** (forensic pass complete). No blockers. Research only; no game/plugin code, no build, no paste, no registry edit, no server restart, no edits outside `game-plugins/research/existing_maps/decision_dome_recreation/`.

## Done

- [x] Read `game-plugins/research/existing_maps/PLAN.md` and executed the A5 section.
- [x] Parsed the 1.17.1 world directly: 1,344 chunks, 4 region files (2 zero-byte), **20,054 blocks**, 85 base types, 186 block states.
- [x] All-dimension scan: overworld only; DIM-1/DIM1 have no region folder. **2 command blocks, 45 tile entities, 2 armor stands**; 0 signs/chests.
- [x] Exact anatomy: drum r26/53 wide/43 tall, terrace y68 4 wide, pit 42 deep, dial y58 8 zones, crown 17×17 y69–82, 10 pods r16 36° 5×5/7×7 y27–77, 0 seats, 0 stands, 0 stairs into the pit.
- [x] Palette: 74.4% concrete, 9.4% terracotta, 9.2% glass, 2.2% quartz, 0.7% gold, **0 copper**; 18 sea lanterns; fixed noon.
- [x] Top-down, two iso, interior cutaway, section, dial plan, pod/crown/entrance details; labelled plans/elevations. Every cited image opened.
- [x] `data/blocks.csv`, `block_states.csv`, `layers.csv`, `dimensions.csv`, `palette.md`, `command_blocks.csv`, `entities.csv`, `signs.csv`, `checkpoints.csv`, `dimension_scan.csv`, `sightlines.csv`, `route.md`.
- [x] Direct comparison table vs `game-plugins/DecisionDome/SCHEMATIC.md` and `previews/nocturne_orrery/REPORT.md`; adopt/do-not-adopt lists.
- [x] How the build expects to be driven without command blocks (it has 2; one egg/glow prop; no vote loop).
- [x] README, REFERENCE_IMAGES, STATUS, frames/, data/, renders/.

## Corrections to the Wave-1 brief (important)

| Brief said | Actual | Evidence |
|---|---|---|
| 0 command blocks | **2**: impulse `(-3,69,-19)` `give @p egg`; chain `(-3,68,-19)` `effect give @p minecraft:glowing 999999 255 true` | H (block + NBT + `LastExecution=286717`, `SuccessCount=1`) |
| 0 block entities | **45**: 24 banners, 17 skulls, 2 command blocks, 2 beehives | H (NBT) |
| (not mentioned) | **2 armor stands** at `(-19.5,69,-7.5)`, gold + green leather chestplates | H (entity NBT) |
| "pure build" | **0 of the 12 zips active**: source `Enabled=[vanilla, mod:forge, mod:worldedit]`; all **12 zips** in `Disabled` (11 feature packs + `VanillaTweaks_c772085.zip`) + 1 unzipped bundle folder; no dome logic | H (`level.dat` DataPacks + folder scan) |
| Spawn (not in brief) | **source** `0,4,0` (22 below the base, unsafe); **live Multiverse** `0,76,0` on lime concrete (safe, A12) | H (A12 worlds.csv + source level.dat) |
| "0 command blocks" possibly from an overworld-only or block-palette scan | DIM-1/DIM1 have no region folder; the overworld scan is complete; the 2 command blocks are real | H (all-dimension scan) |

A12 independently found 2 command blocks; this dossier confirms the coordinates and adds the exact NBT/commands/1.21 risk. **A14 QA pass:** pack state (0 of 12 zips active; all 12 Disabled) and the source-vs-live spawn distinction have been corrected; A12 is authoritative for both. Geometry, sightline and comparison conclusions are unchanged.

## Blocked

None.

## Open questions

1. **Dial intent:** 8 zones with 4 continuous cardinal + 4 dashed diagonal dividers; could be read as 4 quadrants with chevrons. I document both and use "8 zones" for the count. (M)
2. **Command-block coupling:** NBT proves the station has fired, but the plate→carpet→impulse block-power path is not visually proven; no dust/repeater/lever. (M)
3. **Heads/banners lore:** 9 chicken / 5 zombie / 3 pale heads; 16 patterned + 8 coloured banners. Likely a trophy/apiary lounge; the author/lore is not recorded in the world. (M/L)
4. **"Dome" naming:** no dome/roof exists. If this was meant as a dome, the roof is missing. (H)
5. **Author/source page:** no signs/credits; level name + 2021-10-09 timestamp only. (L)

## Tooling findings (report, do not fork/fix)

1. **`tools/world_to_mcmdump.py` does not read this 1.17.1 world.** It expects the 1.18+ `sections` / `block_states` layout; 1.17.1 stores `Level.Sections` with `Palette` / `BlockStates`. It would find 0 blocks. I used a local 1.17-compatible dumper in `_scratch/dump117.py` and did **not** edit the shared tool. (H)
2. **`tools/render_preview.py` ignores block states and entities.** Stairs/slabs are drawn as full blocks; banners/heads are block colors; armor stands are absent. Heights in this dossier come from block data, not the renderer. (H — tool limitation)
3. **The world icon is a generic sky crop**, not a build screenshot; no use for identification. (H)

## Deliverable checklist (PLAN §4 adapted)

| Requirement | Status |
|---|---|
| `README.md` full dossier with numbered H/M/L claims | done |
| `REFERENCE_IMAGES.md` path → source → shows → take | done |
| `frames/` full-resolution evidence | done (14 crops + head textures) |
| `data/blocks.csv` every type + count + share + use | done (85 rows) |
| `data/command_blocks.csv` both blocks, exact command, trigger, effect, 1.21 risk | done (2 rows) |
| `data/signs.csv` | done (0 signs record) |
| `data/checkpoints.csv` | done (none record) |
| `data/route.md` | done (visitor path, explicitly no gameplay route) |
| `data/palette.md` | done |
| `data/entities.csv` | done (45 TEs + 2 stands) |
| `renders/` top-down + 2 iso + interior + exterior + labelled | done (plus section, dial, contact sheet) |
| Comparison table vs SCHEMATIC/REPORT | done |
| No code / no build / no server edits | done |

## Handoff notes for A13 / A14

- **Use this as a "what not to copy" case** for pod/room and dial readability: the reference's solid pods and buried dial are the failure modes; its team-colour read, single crown and red-carpet arrival are the transferable ideas.
- **Do not use the 20k block count as a budget target for the playable core.** It is a 1:2 model; our venue is 22.5× larger. The transferable budget lesson is detail discipline on non-playable surfaces, not shrinking the arena.
- **If A13 wants a "crown/coin" alternative reveal**, this build is the best reference for that silhouette: 17×17, 14 tall, gold ring + white cross + yellow top ring + 8 red jewels.
- **If A14 checks the brief's "0 command blocks / 0 block entities" claim**, this dossier and `data/dimension_scan.csv` are the correction; the world has 2 command blocks in the overworld and none elsewhere.
