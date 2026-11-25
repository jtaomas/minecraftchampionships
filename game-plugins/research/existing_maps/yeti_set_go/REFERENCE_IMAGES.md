# Yeti Set Go — reference images

All images are full-resolution PNGs generated from the source world `YetiSetGo` (or extracted from `resources.zip`), not from a video. “Source” is the dump/render or pack file. Confidence tags: **H** observed, **M** two sources, **L** inference.

| # | Image | Source | What it shows | What to take |
|---|---|---|---|---|
| 01 | `frames/01_overview_top.png` | `data/yeti_full.mcmdump.gz` → `tools/render_preview.py --top` | Whole map top-down, 2,288×1,952. Start engine at right, village U bottom, pool centre, station/lodge left/centre, north snow top-right. | Overall loop shape; where the sections sit. H. |
| 02 | `frames/02_iso_angle0.png` | same dump → iso angle 0 | 2,000×1,019 isometric from the south-east. Train engine, waterfall, pool, station, north chain all visible. | The three vertical layers: village y −5..10, pool y 15, station/lodge y 35, north y −20. H. |
| 03 | `frames/03_iso_angle1.png` | same dump → iso angle 1 | 2,000×1,887 isometric from the south-west. Best view of the west station on its pillar and the river loop. | Station height and bubble column; river loop shape. H. |
| 04 | `frames/04_start_engine.png` | crop x 0..45, y −32..14, z −62..16 | The black/white train engine with yellow trident emblem, the boat basin, the platform. | Start identity; the engine is the start marker, not a finish. H. |
| 05 | `frames/05_start_gate.png` | crop x 6..36, y −8..8, z −4..18 | Close-up of the start platform, boat, basin and engine rear. | Gate is at the platform’s south edge; boat basin is immediately south. H. |
| 06 | `frames/06_village_u.png` | crop x −62..36, y −12..8, z 6..80 | The U-shaped village path around the boat basin, houses, pads. | The U is the first leg; it returns north on the west side. H. |
| 07 | `frames/07_southwest_chalet.png` | crop x −135..−48, y −10..22, z 0..50 | The south-west plaza, chalet with spiral stair, cannon platform, raised pool. | The split point: ice cave west vs chalet/pool north. H/M. |
| 08 | `frames/08_pool_arch.png` | crop x −72..−4, y −2..32, z −32..26 | The pool, stone arch, white/red bridge, lodge roof at top-right. | The pool’s east exit is the arch/bridge to the lodge. H. |
| 09 | `frames/09_west_river_loop.png` | crop x −186..−88, y 6..26, z −42..22 | The two-level west river, pads, hairpin, flagged platform. | The river is an optional loop; it returns to the pool. M. |
| 10 | `frames/10_station_checkpoint.png` | crop x −186..−112, y −2..46, z 14..56 | The mangrove train car on its pillar, west platform with yellow pads/lodestone, bubble column below. | Station is high (y 36); west platform is the elytra equip pad; the open launch edge is north (z 28). H/M. |
| 11 | `frames/11_station_interior.png` | crop x −174..−132, y 26..42, z 22..40 | Hollow train car, black/white buffer, red side, roof hole, bubble shaft. | The station is a hollow shell with a walkable lodestone floor; command wall is inside. H. |
| 12 | `frames/12_lodge_launch.png` | crop x −32..12, y 18..50, z −48..2 | Lodge roof “track” with black beams and yellow strips, stone arch with a jump pad, white/red bridge. | The lodge roof is a purpose-built elytra launch with railings on the south/east. H. |
| 13 | `frames/13_north_chain.png` | crop x −4..82, y −30..−6, z −146..−72 | The north snow chain, 7 jump pads, yellow strips, blue posts, return waterfall. | North landing and the southbound return chain. H. |
| 14 | `frames/14_finish_waterfall.png` | crop x 8..34, y −20..6, z −34..2 | Start platform north side, waterfall sheet, two “grabber” props, ledge below. | The finish approach: tunnel → ledge → waterfall → platform. H/M. |
| 15 | `frames/15_pack_yellow_pad.png` | `resources.zip` → `pack/extracted/assets/minecraft/textures/block/yellow_glazed_terracotta.png` | 16 frames of the yellow chevron speed-pad animation. | Yellow = speed; 16-frame, frametime 1. H. |
| 16 | `frames/16_pack_pink_pad.png` | pack | 7 frames of the pink ripple jump-pad animation. | Pink/red/magenta = jump; 7-frame ripple. H. |
| 17 | `frames/17_pack_energy.png` | pack | 2 frames of the red/blue “Energy” icon (rotten flesh rename). | The custom item is an icon, not a map mechanic. H. |
| 18 | `frames/18_route_overlay.png` | `tools/route_overlay.py` on the dump | Labelled route: white = station line, orange = pool/lodge line, cyan dashed = elytra, magenta = north return. | The one image to follow the route by. M (schematic lines, not a physics path). |
| 19 | `frames/19_heightmap.png` | `tools/height_map.py` | Top-down coloured by top-block world y, 8-block contours. | Elevation profile: village low, pool 15, station/lodge 35, north −20. H. |
| 20 | `frames/20_station_top.png` | `tools/station_map.py` | Station top-down with command blocks/signs/hopper/lever marked. | Command wall and lodestone floor layout. H. |

## Extra renders (opened but not framed)

| Image | Use |
|---|---|
| `renders/yeti_iso_b.png` (angle 2) | Opposite iso; useful for the village/pool from the north. Opened. |
| `renders/z_start.png`, `renders/z_checkpoint.png` | 12 px/block top-down crops with world grids. Opened. |
| `renders/pads_map.png` | All 249 pad clusters numbered; used to build `data/pad_clusters.csv`. Opened. |
| `renders/flood_dock.png` | Walkable flood from the dock; shows the village U is disconnected from the west on foot (jump required). Opened. |
| `renders/crop_start.png`, `renders/crop_west.png`, `renders/crop_pool.png` | Earlier detail crops. Opened. |

Other generated files (`renders/crop_tunnel.png`, `z_mid.png`, `z_north.png`, `yeti_iso_d.png`, `flood_*.png`, `pack_*.png`) exist for completeness but are not cited in this dossier.

## Images that do **not** exist / were not used

- No in-game screenshots: the source is a world folder, not a running client; all images are offline renders of the block dump (H).
- No video frames: the video is out of scope for A4 (A11 owns it).
- No images of Clouds 2, City Marathon, Space Race or TGTTOS: out of scope (A1/A2/A3).
