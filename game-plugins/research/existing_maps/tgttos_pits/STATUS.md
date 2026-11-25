# STATUS — A9 TGTTOS Pits

Updated: 2026-11-25. Owner: A9. Research only.

## Done

- [x] Source world scanned read-only: 3,254 chunks, **51 occupied**, **164,145 non-air blocks**, 63 states, extents x -3..117, y -64..29, z -9..83.
- [x] `Pits` datapack unpacked: `pack.mcmeta`, load/tick tags, all 6 functions, 14 objectives, 1 sign, gamerules, timing, finish, respawn, cleanup, reset. Verbatim export in `data/commands_export.txt`; analysis in `data/datapack.md`.
- [x] Command blocks: **0** confirmed (`data/command_blocks.csv` header only; no `command_block` state or block entity).
- [x] Containers: **0** (no chests/barrels/shulkers). Signs: **1**. Entities: **20 chickens**.
- [x] Route/anatomy documented leg-by-leg (`data/route.md`): start cage, 3 islands, 2 × 12-block pits, glass lip, 7×34 chicken pool, east plaza, kill plane, side passages.
- [x] Blocks/palette/lighting/build techniques (`data/blocks.csv`, `data/palette.md`); SkyLight 15 through the black-glass ceiling verified from region light arrays.
- [x] 1.21.1 compatibility risks documented with exact failure points (`README.md` §5, `data/datapack.md` §11): **imported pack disabled** (A12); folder rename fatal; item NBT fatal for kit; pack_format mismatch; other syntax valid. Exact import state and load/build path: `data/load_and_121.md`.
- [x] A12's imported-world finding confirmed directly against the review server world and the source archive: source `DataPacks.Enabled=[vanilla,Fabric Mods,file/Pits]`, imported `Enabled=[vanilla,file/bukkit,paper]`, `file/Pits` on disk but not enabled; all 9 pack files byte-identical by SHA-1; imported `level.dat` DataVersion 3955, GameType 0 (survival), spawn 15,-2,36. 21 old item-NBT lines extracted with exact commands + 1.21 component forms (`data/item_nbt_lines.csv`).
- [x] Comparison with `McmTgttos` and architecture-level comparison with Badlands/Industry; lineage evidence (shared banner coords + legacy Industry cage).
- [x] 22 render/diagram images + 10 frame copies; every cited image opened and indexed in `REFERENCE_IMAGES.md`.
- [x] `README.md`, `REFERENCE_IMAGES.md`, `STATUS.md`, `data/`, `renders/`, `frames/`.

## Blocked / not done

- **Live world not queried via RCON.** No server command was run. The review server world was read directly, read-only, at A12's path (`...\scratchpad\review\mcc_tgttos_pits`), and matches A12's report. The imported `level.dat` and all 9 pack files were inspected; the pack files are byte-identical to the source (SHA-1), and `file/Pits` is not in `Enabled`. No `mcc_tgttos_pits` folder exists under the dev server root; the review server is a separate folder.
- **A12 says the pack is disabled; confirmed directly.** Import `Enabled=[vanilla,file/bukkit,paper]`, `Disabled=[bundle,trade_rebalance]`; source `Enabled=[vanilla,Fabric Mods,file/Pits]`, `Disabled=[]`. Imported `level.dat` is DataVersion 3955, GameType 0 (survival), spawn 15,-2,36. Recorded in `data/load_and_121.md` §1.
- **1.21 runtime not tested.** We did not start/reload a server. The failure chain in §5 is from jar paths/classes + syntax, not a live log.
- **No port/fix applied.** Research only; no datapack or world edits.

## Open questions

| # | Question | Current evidence |
|---|---|---|
| Q1 | Does clicking the visible “Start” line work, or only blank line 1? | NBT click is on Text1 (blank); Text2 “Start” has no click. H NBT / M client. |
| Q2 | Does 1.21.1 keep a `pack_format 10` pack enabled or disable it? | PackCompatibility marks incompatible; enabled list still has `file/Pits`. M. |
| Q3 | Is the park → north-roof route continuous/faster? | Park top y5 and north roofs y5; long detour; no barrier. M/L. |
| Q4 | `store result` truncation vs rounding of `Pos[1]`? | Kill plane -41 either way; exact boundary M. |
| Q5 | Will the 20 non-persistent chickens survive on the live server? | `PersistenceRequired=0`, no chunk tickets; finish is ~50 blocks from start, likely within view distance. H NBT / M server. |
| Q6 | Are the external banners visible from spectator flight? | They are outside the light-blue wall at z83; spectator can pass the wall. M. |
| Q7 | Was the legacy Industry cage intended as a spectator room? | Sealed inside solid brown wool, no entrance, 20 blocks below the finish. M/L. |

## Tool notes (report only; shared tools not edited)

- `tools/world_to_mcmdump.py` is correct but slow on this world: for every palette-1 section it yields all 4,096 cells (mostly air). With ~3,254 chunks × ~24 sections this is ~320M yields; a full dump exceeded the 10-minute shell timeout. A bounded crop (`--min/--max`) produced the 164k-block dump in ~2–3 min. `data/scan_world.py` has a fast path that skips palette-1 air sections; it is an analysis helper in this folder, not a replacement for the shared tool.
- `render_preview.py` was used unmodified. `--show-barriers` works in both top and iso; the barrier overlay is translucent red.
- The source world's `entities/r.0.0.mca` is 131,072 bytes with 20 entities; no armor stands/item frames are saved.
- The source `playerdata`/`stats` were useful ground truth: adventure→spectator, pickaxe + concrete, 200 resisted damage, 20 chicken eggs, 1 sign.

## File map

```text
tgttos_pits/
  README.md                  full dossier, H/M/L
  REFERENCE_IMAGES.md        every image, source, what to take
  STATUS.md                  this file
  data/
    blocks.csv               63 states with role/use
    command_blocks.csv       header only (0 command blocks)
    signs.csv                the one Start sign
    checkpoints.csv          start/finish gates + pits (no checkpoints)
    route.md                 leg-by-leg route
    palette.md               palette/lighting/build techniques
    entities.csv             20 chickens
    functions.csv            6 functions summary
    datapack.md              line-by-line datapack + bugs + 1.21 risks
    commands_export.txt      every function line, numbered
    load_and_121.md          source vs imported level.dat, load/build path, 1.21 requirements
    item_nbt_lines.csv       exact 21 old item-NBT commands + component forms
    import_state.json        direct read: source/import DataPacks, GameType, pack SHA-1s
    export_item_nbt.py      extractor for the above
    check_import.py         read-only source-vs-import comparator
    datapack_manifest.csv     line/byte counts
    scoreboards.csv          objectives
    tags.csv                 load/tick tags
    timeline.csv             event timeline
    world_scan.json          world counts/extents
    levels.csv, platforms.csv, columns.csv, top_surface.txt
    entities_raw.json, entities_full.txt
    scan_world.py, filter_dump.py, light_probe.py, probe.py, slice.py,
    cells.py, walk_probe.py, platforms.py, top_surface.py, export_datapack.py,
    make_data.py, make_route_map.py, make_details.py, make_iso_labels.py
  renders/                   22 PNGs (top, 2 iso, labeled, details, section)
  frames/                    10 full-resolution evidence copies
```

## Acceptance check against PLAN.md §4

- Every command block accounted for: **0** — yes.
- Every container opened: **0** — yes.
- Route followable from the report alone: yes (`data/route.md` + signed schematic).
- At least 6 full-resolution images: **22** renders + 10 frames; overview/start/signature/pit/finish/detail all covered.
- Explicit better/worse section: yes (`README.md` §8).
- No claim without H/M/L: all findings tagged; image table has no claims beyond what is visible.
- No writes outside the folder: yes.
- No duplicate Badlands/Industry deep dive: architecture only; full reports belong to A7/A8.
- A12 confirmation: source enabled vs imported disabled documented; 1.21 fix requirements and all 21 old item-NBT lines listed; no port applied.
