# City Marathon — palette, gradients, lighting

Source: block palette decoded from `region/*.mca` (H); counts exclude air. Total non-air blocks **207,440** (H). Shares are of that total.

## 1. Dominant palette (top 32)

| Block | Count | Share | Role | Use |
|---|---:|---:|---|---|
| stone_slab | 21,453 | 10.342% | sidewalk | Main sidewalk/curb and roof-edge slab; dominant grey edge material. |
| mud_bricks | 20,587 | 9.924% | facade | Warm brown building facade. |
| stone_bricks | 17,900 | 8.629% | facade | Grey masonry facade and station walls. |
| stone | 16,447 | 7.929% | structure | Hidden fill/foundations and station mass. |
| bricks | 15,532 | 7.487% | facade | Red-brick residential/commercial facades. |
| gray_wool | 14,040 | 6.768% | road | Asphalt; also clears elytra via CB04. |
| white_terracotta | 12,055 | 5.811% | facade | Pale upper-storey band. |
| terracotta | 11,004 | 5.305% | facade | Orange-brown mid-band. |
| polished_andesite | 7,181 | 3.462% | plaza | Plaza paving, roof pads, station trim. |
| grass_block | 6,739 | 3.249% | park | Park lawn. |
| deepslate_tile_slab | 5,010 | 2.415% | roof | Dark modern roof/edge slab. |
| granite | 4,799 | 2.313% | facade | Warm trim/base course. |
| smooth_stone_slab | 4,582 | 2.209% | sidewalk | Smooth platform/sidewalk slab. |
| moss_block | 4,531 | 2.184% | park | Hedge/roof-garden mass. |
| glass | 3,520 | 1.697% | station | Station roof glazing and windows. |
| mushroom_stem | 3,012 | 1.452% | decor | Flagpole/light-pole shafts. |
| yellow_wool | 2,840 | 1.369% | road | Double-yellow centreline; strongest route cue. |
| light | 2,182 | 1.052% | lighting | Invisible level-15 light blocks. |
| stone_brick_stairs | 2,162 | 1.042% | structure | Stairs. |
| white_wool | 1,910 | 0.921% | road | White lane dashes/awnings. |
| glass_pane | 1,813 | 0.874% | station | Window glazing. |
| smooth_stone | 1,549 | 0.747% | station | Station floor/plaza. |
| barrier | 1,053 | 0.508% | route | Train chassis + start-release gate (invisible). |
| water | 1,037 | 0.500% | water | Ponds, fountain, waterfall. |
| blackstone_slab | 1,035 | 0.499% | roof | Dark station/roof trim. |
| sandstone | 1,005 | 0.484% | plaza | Warm plaza paving. |
| andesite | 956 | 0.461% | facade | Grey wall/trim. |
| iron_bars | 954 | 0.460% | station | Station grilles/fences. |
| sea_lantern | 824 | 0.397% | lighting | Station roof/edge lights. |
| cobblestone_slab | 765 | 0.369% | station | Platform edge/floor. |
| polished_diorite | 681 | 0.328% | route | Elytra auto-equip pad. |
| rooted_dirt | 652 | 0.314% | park | Park soil. |

The full 197-row list is `data/blocks.csv`.

## 2. Colour story

**City:** warm red/brown masonry (bricks, terracotta, white terracotta, mud bricks, granite) against cool grey infrastructure (stone, stone bricks, andesite, stone slab, deepslate tile). This is a deliberately “European railway city” palette, not a bright MCC primary palette — **M** from the material mix and the station/train set piece.

**Roads:** gray wool is the asphalt; white wool the dashed lane paint; yellow wool the double centreline. All three are wool, so the road reads as a flat graphic plane rather than a textured asphalt — **H**.

**Park:** grass block + moss block + dirt path/coarse dirt/rooted dirt/podzol, with spruce/oak/azalea canopies and a pond (water, sand, seagrass, kelp, horn/fire coral). The green is a saturated, cool green that separates strongly from the warm city — **H**.

**Station:** stone/smooth stone floor, polished andesite trim, calcite columns, iron bars and glass; the train is red/blue/white/black concrete with tinted glass and yellow/black hazard stripes on the platform edges — **H**.

**Route furniture:** yellow glazed terracotta (speed), red/pink/magenta glazed (jump/speed flower), orange wool (Jump Boost X + Speed XII), lime wool (Jump Boost VII), green carpet/yellow concrete (effect clear), polished diorite (elytra). These are the only saturated yellow/orange/green surfaces in the city, so they read as “game furniture” — **H**.

## 3. Gradients

1. **Facade gradient (most buildings):** granite plinth (y0–2) → bricks or terracotta body (y0–8) → white_terracotta upper band (y9–15) → stone_slab cornice (y15–20) → deepslate_tile_slab roof. This is visible on the start block and the north office row — **H** (column samples).
2. **Road → sidewalk:** gray wool asphalt → white wool dash → yellow wool double line → stone_slab curb (one block higher) → building wall. The curb is a consistent 1-block step, which is also the visual boundary of the runnable surface — **H**.
3. **Park:** grass_block lawn → moss_block hedge mass → dirt_path/coarse_dirt/rooted_dirt path → water. The path is a darker brown ribbon through the green, with stone-slab edging — **H**.
4. **Station:** smooth_stone floor → polished_andesite edge → calcite column → iron_bars/glass → sea_lantern/light. The train adds a red/blue/white horizontal band at y4–8 — **H**.

## 4. Lighting

- **Time:** `DayTime=13000` (night), `doDaylightCycle=false`; clear weather (`raining=0`, `thunder=0`, `clearWeatherTime=6000`, `doWeatherCycle=false`) — **H** `level.dat`.
- **Invisible light:** 2,182 `minecraft:light` blocks, mostly `level=15`; they are embedded in the road/pad surfaces and under the start gate. They are the main reason the route is legible at night — **H**.
- **Visible fixtures:** 824 sea lanterns (station roof/edges), 146 lanterns, 17 wall torches, 3 campfires, 39 lightning rods, plus glow item frames used as train windows/traffic lights — **H**.
- **Contrast:** the road is mid-grey; the pads are saturated yellow/orange; the park is dark green; the station is pale grey. At night the light blocks create pools of light exactly on the pads and the start gate — **H** from block placement, **M** for perceived effect (no in-game screenshot).
- **Renderer caveat:** `renders/` and `frames/` are generated by `tools/render_preview.py` with a daylight studio sky, not in-game screenshots. They preserve geometry and palette but **not** the night lighting or bloom. Do not use them to judge in-game brightness — **H** (tool README) / **M** (visual).

## 5. Palette verdict

- Cohesive and legible: warm city vs cool grey infrastructure vs saturated route furniture — **H**.
- The station is the weakest palette area: a very large, uniform grey floor with little mid-scale detail; the train and glass shed carry the identity — **H** (frames 06/10).
- The park is the strongest: green lawn, brown paths, blue water, pink cherry tree, and a clear fountain plaza — **H** (frame 04).
- The start is the most cluttered: brick canyon, TARDIS, quartz podium, glazed pads, gate posts and command circuit all within ~50 blocks — **H** (frame 02/03).
