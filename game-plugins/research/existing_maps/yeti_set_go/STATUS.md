# Yeti Set Go — status

**Agent:** A4 (forensic owner)
**Folder:** `game-plugins/research/existing_maps/yeti_set_go/`
**Source:** `...\mcc_maps\YetiSetGo` (1.19.2, 3,357 chunk records, 28,533 non-air blocks)
**Live copy:** the reachable copy `mcc_recreations\YetiSetGo` is byte-identical to the source (SHA-256 `level.dat`, `resources.zip`, all `.mca`; H). Analysed once.
**Rule:** research only — no builds, no plugin/game code, no registry edits, no server restarts, no edits outside this folder.

## Done

- [x] World scanned: bounds, chunks, regions, palette, block entities, entities.
- [x] All **37 command blocks** decoded and exported (`data/command_blocks.csv`) with trigger/effect/reset/1.21 risk.
- [x] All **14 signs** decoded (`data/signs.csv`).
- [x] **6 container block entities** opened: 3 shulkers + 1 hopper + 2 empty campfires; **4 storage containers, all empty** (`data/containers.csv`, 6 rows with a `kind` column). This is the shared A10/A13 definition.
- [x] **2 paintings** (`data/entities.csv`).
- [x] **133 base blocks / 238 states** exported (`data/blocks.csv`, `data/block_states.csv`); palette/lighting/compactness (`data/palette.md`).
- [x] **249 pad/elytra clusters** (`data/pad_clusters.csv`).
- [x] **Route leg-by-leg** with coordinates, pads, elytra/trident swaps, checkpoints, failure planes, start/finish, lap behaviour (`data/route.md`).
- [x] **Top-down + 2 iso + labelled route overlay + height map + 20 evidence images** (`renders/`, `frames/`: 17 map/diagram views + 3 pack texture strips); every cited image opened.
- [x] **Pack mapping** (`data/pack_mapping.csv`) with link to A10 (`../resource_packs/`), no duplicate inventory.
- [x] **1.21 upgrade notes** (commands, world, pack, server).
- [x] `README.md`, `REFERENCE_IMAGES.md`, this `STATUS.md`.
- [x] A12 cross-check recorded: 3 `gamemode adventure` failures, pack format 9→34.

## Cross-checks received

| From | Claim | My verification |
|---|---|---|
| A12 | 3 command-block failures, all no-target `/gamemode adventure` (`permissions.requires.player`) | Confirmed: (24, −3, −8), (24, −3, −6), (20, −4, 1); all `SuccessCount=0`; first has the explicit error (H). Effects: none; Multiverse/server gamemode applies. |
| A12 | Resource pack is format 9, needs 34 for 1.21.1 | Confirmed: `pack.mcmeta` `pack_format: 9`; 1.21.1 = 34 (H). |
| A14 | Container definition (P2) | Fixed: 6 container block entities = 3 shulkers + 1 hopper + 2 empty campfires; 4 storage containers, all empty. A13 aligns to 6. |
| A14 | Pack texture paths (P2) | Fixed: README §7/§8.3 now state the base `pack/extracted/assets/minecraft/`; §8.3 uses `textures/block/...` paths. |

## Open questions (not blockers for the dossier)

1. **Lap vs point-to-point** — the dossier uses the lap reading (M) because it is the only one that uses the return ledge/waterfall and the start-side pads. If in-game testing shows the finish at the station or the north tip, the last leg changes; the sections and coordinates do not.
2. **Primary line** — station line is called primary (M) because it carries the checkpoint and the elytra command; the pool/lodge line is fully built and could be the intended main line (L).
3. **Start waterfall riptide** — 22 blocks tall; needs two riptides or a swim assist (M/L).
4. **“Energy” rotten flesh** — custom item, no command/container uses it (H); purpose unknown (L).
5. **Two gray “grabber” props** north of the start (x 30..40, z −30..−25) — no mechanics attached (L).
6. **Gate `SuccessCount=0`** — saved gate is closed; a fresh button press should clear it (M).

## Caveats

- **Tool note:** the shared `tools/world_to_mcmdump.py` did not finish within 600 s on this 3,357-chunk sparse map (it expands every air cell). I used a local read-only `tools/dump_blocks.py` (same MCMDUMP format) for the analysis dump and the shared `tools/render_preview.py` for every render. The shared tool was not modified.
- All images are **offline renders** of the block dump, not in-game screenshots; the source is a world folder (H).
- The route overlay is **schematic** (straight lines between waypoints), not a physics path (M).
- The “better/worse” comparison uses only `PLAN.md` counts; no independent analysis of Clouds 2, City Marathon, Space Race or TGTTOS (scope rule).
- The live server `simulation-distance=6` changes which far pads/elytra pads work; the original map likely assumed 10–12 (M). No fix applied.
- The root `server.properties` says `enable-command-block=false` (H), but the PLAN says command blocks were enabled on the review server; A12 owns the live check.

## Missing / not done (by scope)

- No in-game run or timing (A12 owns start instructions; A11 owns the video).
- No independent analysis of the other four maps (A1/A2/A3).
- No pack re-authoring or format bump (A10 owns the pack; no fixes).
- No Multiverse/server config change (A12).
- The route’s exact finish (lap vs station vs north tip) is not proven by a gameplay recording; it is a geometry/NBT inference (M).
