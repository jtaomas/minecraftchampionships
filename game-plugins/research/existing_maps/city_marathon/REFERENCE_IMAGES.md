# City Marathon — reference images

Every image below was opened by A2. Unless stated otherwise, it is a **tool render** from `tools/render_preview.py` / A2's top-down scripts over `region/*.mca`; it is **not** an in-game screenshot and does not reproduce the map's night lighting. Geometry and block IDs are H; colour is the renderer's approximation — M.

Paths are relative to this folder.

| Image | Source / method | What it shows | What to take | Evidence |
|---|---|---|---|---|
| `frames/01_overview_route.png` | A2 render + route overlay, 3684×1780 | Whole map with main line (cyan), alternates (dashed), pads, CP2/CP3, start/finish, elytra zones | Primary route figure; use for orientation only, not for exact ground height | H geometry; M route intent |
| `frames/02_start_gate_iso.png` | `start_gate_crop.mcmdump.gz`, iso angle 0, scale 10, 1100×631 | Start gate alley: yellow-concrete posts, quartz podium, 3×3 glazed pad, TARDIS ladder | Start pen/gate detail; gate is narrow and flanked by brick walls | H |
| `frames/03_start_topdown.png` | `topdown_crop` x−70..30, z−20..110, 10 px/block, 1010×1394 | Start area from above: spawn, button, barrier line at z10, gate at z49–57, TARDIS | Shows the two-gate pen and the road geometry | H |
| `frames/04_park_paths.png` | `park_floor.png`, 6 px/block, 1446×1170 | Park paths: z73 north path, z88 central path, fountain plaza, south path, NO ENTRY gate | Park fork: north entry vs central exit; fountain is the main landmark | H |
| `frames/05_plaza_detours.png` | `courtyard_floor.png`, 8 px/block, 1088×1252 | Plaza block: blue dome, long pool, north road z37, south road z139, west edge x−311 | The direct z88 line is blocked; north/south are the two detours | H |
| `frames/06_station_floor.png` | `station_floor.png`, 6 px/block, 1446×990 | Station floor and train from above: red train z85–91, yellow roof strip, platform x−351 | Train is the set piece; the floor is huge and grey | H |
| `frames/07_train_iso.png` | `train_crop.mcmdump.gz`, iso angle 2, scale 8, 1178×758 | Train detail: red/blue/white cars, yellow-glazed roof strip, orange pad, hazard-striped platform | Signature set piece; roof route and stairs at x−457 | H |
| `frames/08_elytra_roof_pad.png` | `sw_tower_iso.png`, 1320×532 | A rooftop y28 tower with no exterior access; command block below the pad; roof is andesite, not the checked diorite | Elytra pad unreachable **and** non-matching; no ladder/stairs | H |
| `frames/09_highway.png` | `topdown_highway.png`, 6 px/block, 1686×690 | North Highway west end and T-junction at x−476 | Highway is a wide pad-lined return leg; spur south | H |
| `frames/10_station_iso.png` | `station_interior_iso.png`, y≤32 cutaway, 1262×890 | Station concourse cutaway: glass shed, open grey floor, train in the distance | Station is visually under-detailed; train/glass shed carry identity | H |
| `renders/iso_sw.png` | full dump, iso angle 0, 1562×834 | Whole city from the south-west: station, park, towers, highway | Best single hero shot of the build | H geometry; M colour |
| `renders/iso_ne.png` | full dump, iso angle 2, 1562×864 | Whole city from the north-east: park, station, glass shed, highway | Shows the city block arrangement and the station roof | H geometry; M colour |
| `renders/topdown_raw.png` | top block per column, 4 px/block, 2236×964 | Whole map top-down | Overview; the purple sidewalks are a renderer colour-table artefact, not real blocks | H geometry; M colour |
| `renders/topdown_start.png` | x−130..40, z−80..180, 6 px/block | East side of the city, start road, park east | Start-to-park relationship | H |
| `renders/topdown_start_close.png` | x−70..30, z−20..110, 10 px/block | Start close-up | Gate, button, crosswalk, TARDIS | H |
| `renders/topdown_west.png` | x−560..40, z−80..180, 4 px/block | West side: station, park, plaza, highway | Station/plaza relationship | H |
| `renders/ground_layout.png` | highest block y≤6, 3 px/block, 1677×723 | Ground-level layout with roads and pads (no roofs) | Best base for route tracing | H |
| `renders/route_furniture.png` | ground layout + pad clusters, 6 px/block | Every pad cluster, command block, sign and banner position | Pad-density evidence | H |
| `renders/route_overlay.png` | route overlay v2, 3684×1696 | Same as frame 01 without the header | Route figure for embedding | H geometry; M route intent |
| `renders/paths_overlay.png` | BFS shortest paths on the walkable graph, 4 px/block | Green start→station, blue station→highway, magenta highway→start, yellow start→park, orange park→station | Proves ground connectivity and the station/west-link link | H |
| `renders/station_floor.png` | highest block y≤10, x−540..−300, z15..165, 6 px/block | Station floor, train, platform x−351, green-roof buildings | Train position and platform edges | H |
| `renders/station_interior_iso.png` | station crop y≤32, iso angle 0, scale 4, 1262×806 | Station cutaway at y32: open grey concourse, arched glass train shed, train in the distance | Signature station section; the concourse is largely empty and the route through it is unmarked | H |
| `renders/park_floor.png` | highest block y≤6, x−300..−60, z−10..170, 6 px/block | Park paths, fountain, pond, cherry tree, east gate | Park layout and fork | H |
| `renders/park_interior_iso.png` | park crop y0..30, iso angle 0, scale 4, 1500×950 | Park in 3D: fountain, tree grid, table, cherry tree, buildings | Park character | H |
| `renders/courtyard_floor.png` | highest block y≤6, x−350..−215, z15..160, 8 px/block | Plaza block: dome, pool, north/south roads, west edge | Detour geometry | H |
| `renders/cp3_station_map.png` | highest block y≤10, x−470..−350, z30..105, 8 px/block | CP3/station north approach and train | CP3 relationship to train | H |
| `renders/start_gate_iso.png` | start crop, iso angle 0, scale 10, 780×535 | Start gate/podium close-up | Gate detail | H |
| `renders/train_iso.png` | train crop, iso angle 2, scale 8, 1178×674 | Train close-up | Train detail | H |
| `renders/tower_iso.png` | x−235..−195, z10..65, y18..34, iso angle 0, scale 8, 554×398 | Two rooftop towers with stair bulkheads | Roof access absent | H |
| `renders/sw_tower_iso.png` | x−230..−200, z138..162, iso angle 2, scale 8, 360×428 | South-west rooftop tower | Elytra pad unreachable | H |
| `renders/level_-1.png` | all blocks at y=−1, 4 px/block | Ground roads and pad layer | Road network at ground level | H |
| `renders/level_4.png` | all blocks at y=4, 4 px/block | Station platform/train level and start podium pads | Vertical layer 1 | H |
| `renders/level_8.png` | all blocks at y=8, 4 px/block | Train roof/upper pads | Vertical layer 2 | H |
| `renders/roof_layout.png` | highest block y20..40, 4 px/block | Rooftop modules, AC banks, tower roofs | Elytra pad roofs | H |
| `renders/walkable_component.png` | BFS reachable ground cells, 1 px/block | 65,286 walkable cells reachable from spawn | Proves rooftop pads are unreachable | H |

**Not used / not cited:** `park_iso.png`, `stadium_iso.png`, `track_layers.png`, `tower_top.png`, and the raw `.mcmdump.gz` crops are working files. They are kept for reproducibility but are not claimed as evidence in this report.
