# Yeti Set Go — palette, lighting, composition

Measured from `data/blocks.csv` / `data/block_states.csv` (28,533 non-air blocks, 238 block states, 133 base blocks). Confidence tags: **H** observed, **M** two sources, **L** inference.

## 1. Family shares (H)

| Family | Blocks | Share | Role |
|---|---:|---:|---|
| Stone (stone, andesite, cobble, stone bricks, deepslate, blackstone) | 8,160 | 28.6% | island bodies, track base, plaza, arches, undersides |
| Water (water + bubble column) | 8,053 | 28.2% | pool, river, waterfalls, boat basin, bubble elevator |
| Snow / ice (snow_block, snow, packed_ice, ice) | 4,440 | 15.6% | north chain, ice speed lanes, roofs, pool islands |
| Wood (mangrove, spruce, stripped spruce, slabs/stairs/leaves) | 2,763 | 9.7% | village, train cars, station, roofs, tunnel |
| Concrete (gray/black/white/red/blue/yellow) | 1,659 | 5.8% | start platform, train engine, lodge beams, markers |
| Wool (incl. lime, yellow, red, gray, white) | 970 | 3.4% | pad bases, train stripes, flag cross |
| Pads (glazed terracotta + carpet + lime wool) | 642 | 2.3% | speed/jump pads |
| Quartz (smooth quartz, quartz bricks, pillar, stairs) | 567 | 2.0% | chalet/station trim and stair |
| Gameplay (lodestone + barrier) | 342 | 1.2% | elytra pads, start gate |
| Ground (dirt, grass, podzol, coarse dirt, path) | 259 | 0.9% | village floor patches |
| Detail (iron, ladders, fences, doors, bookshelf) | 204 | 0.7% | train/cafe detail |
| Terracotta (green/yellow/cyan/plain) | 197 | 0.7% | village roofs |
| Glass (stained + panes) | 157 | 0.6% | train windows, start-gate panes, station glazing |
| Accent (netherite block) | 41 | 0.1% | station/lodge accents |
| Command blocks | 37 | 0.1% | logic |
| Redstone (wire, repeater, lever, button, block) | 25 | 0.1% | countdown |
| Light (sea lantern, lantern, campfire) | 13 | 0.05% | local accents |
| Storage containers (shulker ×3, hopper) | 4 | 0.01% | empty |

The two lit campfires at (−101, 2, 35) and (−39, 4, 32) are also container block entities (4 cooking slots each, empty). They are counted under **Light** above, giving **6 container block entities = 3 shulkers + 1 hopper + 2 campfires; 4 storage containers, all empty** (H).

## 2. Top blocks (H)

| Block | Count | Share | Where |
|---|---:|---:|---|
| water | 7,781 | 27.27% | pool, river, waterfalls, basin |
| stone | 5,917 | 20.74% | island bodies, track base, undersides |
| snow_block | 3,083 | 10.81% | north chain, roofs, snow islands |
| packed_ice | 1,094 | 3.83% | speed lanes, river decks |
| mangrove_planks | 906 | 3.18% | village houses, train car body |
| andesite | 856 | 3.00% | track edging, cave |
| spruce_planks | 836 | 2.93% | station platform, roofs, tunnel |
| gray_concrete | 672 | 2.36% | start platform, village path, plaza |
| light_gray_wool | 472 | 1.65% | pale track edging in the ice cave / lodge / pool rim |
| yellow_glazed_terracotta | 377 | 1.32% | speed pads (29 clusters) |
| black_concrete | 366 | 1.28% | train engine, lodge roof beams |
| white_concrete | 347 | 1.22% | train stripe, platform edges |
| stone_bricks | 346 | 1.21% | arches, station base |
| stripped_spruce_wood | 318 | 1.11% | train/station trim |
| lodestone | 312 | 1.09% | elytra pads (station 100, start 146, lodge 60, misc 6) |
| gray_wool | 304 | 1.07% | train/station detail |
| spruce_slab | 291 | 1.02% | roofs |
| smooth_quartz | 283 | 0.99% | chalet, stair |
| cobblestone | 278 | 0.97% | island rims |
| bubble_column | 272 | 0.95% | tower elevator |
| snow | 226 | 0.79% | snow layers |
| calcite | 199 | 0.70% | cave/ice detail |
| red_concrete | 150 | 0.53% | flags, accents |
| lime_wool | 54 | 0.19% | jump-boost pads (6) |

## 3. Palette logic (M)

- **Track vs scenery.** The playable ribbon is gray concrete + andesite + packed ice; the scenery is snow, mangrove/spruce, and water. The eye can separate the path from the decoration at a glance (H).
- **Biome gradient.** The course moves from a temperate village (mangrove, spruce, grass, terracotta roofs) at y −5..10, through an ice cave and a blue pool at y 15, to a high station/lodge at y 35, then to a snow chain at y −20. The material palette follows the elevation: wood → stone/ice → snow (H).
- **Train theme.** Three train elements: the black/white engine with a yellow concrete trident at the start (H), the hollow mangrove “caboose” station at the west (H), and the black-beam “track” gantry on the lodge roof (H).
- **Pads are the only saturated colour.** Yellow chevrons = speed; pink/red/magenta ripples = jump; lime = jump boost; lodestone = elytra. Everything else is desaturated (H). This makes the gameplay readable at speed.
- **Flags.** Red concrete + white wool crosses at x −182 (and near the station/pool) are decorative Swiss/winter markers, not banner blocks (H — no banner block exists in the palette).

## 4. Lighting (H)

| Source | Count | Coordinates |
|---|---:|---|
| Daylight | — | `doDaylightCycle=false`, `DayTime=1000` (morning), clear weather (level.dat) |
| Sea lantern | 6 | x 57..64, y −13, z −72 (north ice bridge) |
| Lantern | 5 | (−83, 8, 24), (−79, 8, 24), (−39, 9, 25), (−39, 9, 29), (−38, 19, −17) |
| Campfire | 2 | (−101, 2, 35), (−39, 4, 32) |
| Glowstone | 0 | **the pack ships a glowstone texture but the map uses none** (H) |

The map is a **daylight build**: the sky lights the open track; the ice cave, station interior and tunnel rely on the few lanterns/campfires and ambient light. There is no night cycle and no weather cycle (H). The pack’s custom glowstone texture is unused (H).

## 5. Compactness tricks (H/M)

1. **Two command-block clusters cover the map.** Start at (20, −3, 0) and station at (−155, 32, 27); the pad checks use `execute as @a at @s`, so a single block can test pads anywhere in the loaded radius. At the review server’s `simulation-distance=6` this leaves gaps (M); at 10–12 it covers everything (M).
2. **One elytra command serves three lodestone pads** (station 100, lodge 60, start 146 blocks) — the same block equips at whichever pad is loaded (H/M).
3. **One setworldspawn command turns all 29 yellow speed pads into checkpoints** (H).
4. **3×3 jump pads use four block colours but only two effects** (speed + jump boost): pink corners, red edges, magenta centre — a cheap way to make a large, readable pad from 9 blocks (H).
5. **The bubble column is a 3×4 shaft** (12 water columns) for a 23-block vertical transition (H).
6. **The pool is a 54×38 water sheet with a 1-block stone rim and a waterfall edge**; the waterfall is both scenery and a riptide elevator (H/M).
7. **The whole map is 28.5k blocks in a 260×218 footprint** and a 0.1 MB pack — an extremely small download for a full race (H).
8. **The void is the boundary.** No walls except the start gate; the failure plane is the world floor (H).
