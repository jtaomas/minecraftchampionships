# Palette, lighting and build techniques — TGTTOS Badlands

Source: block histogram in `data/blocks.csv` (411,834 blocks, 62 block IDs / 97 block states). Every count below is **H** (scanned from the region files). Percentages are of all non-air blocks.

---

## 1. Dominant palette

| # | Block | Count | Share | Role |
|---|---|---|---|---|
| 1 | `stone` | 192,974 | 46.86% | terrain body / underside |
| 2 | `terracotta` | 149,342 | 36.26% | badlands body, orange top |
| 3 | `granite` | 15,897 | 3.86% | strata band / outcrops |
| 4 | `andesite` | 10,515 | 2.55% | strata band / grey canyon |
| 5 | `red_sand` | 8,796 | 2.14% | surface patches |
| 6 | `dirt` | 7,252 | 1.76% | surface patches / trench |
| 7 | `brown_concrete_powder` | 4,989 | 1.21% | soft badlands band |
| 8 | `white_terracotta` | 4,493 | 1.09% | **mine walls, towers, roofs** |
| 9 | `red_terracotta` | 3,555 | 0.86% | strata band |
| 10 | `gray_terracotta` | 3,308 | 0.80% | strata / finish wall |
| 11 | `diorite` | 2,035 | 0.49% | white strata |
| 12 | `polished_granite` | 1,149 | 0.28% | paving / trim |
| 13 | `soul_sand` | 778 | 0.19% | finish trench / texture |
| 14 | `smooth_stone` | 653 | 0.16% | rails / platforms / roads |
| 15 | `light_gray_terracotta` | 599 | 0.14% | pale strata |
| 16 | `oak_leaves` | 572 | 0.14% | tree canopy |
| 17 | `cyan_terracotta` | 531 | 0.13% | mine stripe accent |
| 18 | `quartz_block` | 434 | 0.10% | start floor (60) + finish approach (374) |
| 19 | `stone_bricks` | 320 | 0.08% | paving / ruins |
| 20 | `gravel` | 310 | 0.07% | surface detail |
| 21 | `red_concrete` | 190 | 0.05% | MCC letters / accents |
| 22 | `black_wool` | 190 | 0.05% | start frame / rail bed |
| 23 | `chain` | 185 | 0.04% | hanging detail / fences |
| 24 | `barrier` | 184 | 0.04% | start gate (invisible) |
| 25 | `acacia_leaves` | 182 | 0.04% | tree canopy |
| 26 | `cactus` | 181 | 0.04% | hazard / roof detail |
| 27 | `jungle_planks` | 173 | 0.04% | crates / scaffolding |
| 28 | `nether_brick_wall` | 155 | 0.04% | industrial trim |
| 29 | `blackstone` | 123 | 0.03% | rail / trim |
| 30 | `red_wool` | 110 | 0.03% | sign / stakes |
| 31 | `white_concrete` | 106 | 0.03% | MCC white bands |
| 32 | `nether_brick_slab` | 107 | 0.03% | industrial trim |

Full 97-row histogram with role per state: `data/blocks.csv`.

**Material mix** [H]:
- Terrain and strata: **402,322 blocks, 97.69%** (stone, terracotta, all terracotta variants, granite, andesite, diorite, red sand, dirt, gravel, soul sand, brown concrete powder, smooth red sandstone).
- Built masonry: **5,967 blocks, 1.45%** (white terracotta, quartz, stone bricks, smooth stone).
- Accents / industrial / light / fence: **2,130 blocks, 0.52%** (concrete, wool, nether brick, blackstone, iron, chain, sea lanterns, lanterns, daylight detectors).
- Vegetation and technical blocks: the small remainder (leaves 754, cactus 181, dead bush 15, barrier 184, sign 1, etc.).

This is a **terrain-first** map: the build is ~2% of the blocks. The island reads as a natural badlands formation with a handful of man-made scars (mine, rails, billboards, finish wall). [H/M]

---

## 2. Palette families

### 2.1 Badlands strata (the 97.7%)

The strata are built from horizontal bands of `terracotta` (orange), `white_terracotta` (cream), `red_terracotta`, `gray_terracotta`, `light_gray_terracotta`, `cyan_terracotta` (rare teal band), `granite`, `andesite`, `diorite`, `brown_concrete_powder` and `red_sand`. The body is `stone`; the top is terracotta/red sand/dirt. [H]

- The bands are 1–4 blocks thick and follow the terrain contour; the cliff faces show 3–6 alternating bands. [H renders]
- The tops are noisy: patches of red sand, dirt, gravel, brown concrete powder and exposed stone. Cacti and dead bushes are scattered on top. [H]
- The underside is a solid stone mass down to y≈−61, with a rough voxel bottom; the island floats in void. [H section]

### 2.2 Mine / industrial (the 1.5%)

- **White terracotta** is the main built material: the two long mine wings (x 82–118, z 2–14 and 47–60), the four finish corner towers, and the finish wall. It is used as a warm off-white, not quartz. [H]
- **Cyan terracotta** (531) and **cyan concrete** (28) / **cyan concrete powder** (75) are the mine's accent stripes and small details; **light blue concrete** (2) / **light blue concrete powder** (1) are tiny highlights on the finish towers. [H]
- **Quartz** is surprisingly rare (434): 60 blocks are the start-pen floor (x 9–11, z 21–40, y −43); 374 are a long 8-wide floor at x 222–229, y −9, z 7–54 (the finish approach). [H]
- **Smooth stone** (653) and **stone bricks** (320) form rails, platforms, roads and ruins: the 3-wide strips at x 63–65, 82–84, 182–189, 233–235; the x 118–123 platform; the x 74–79 and x 164–174 ruins. [H]
- **Nether brick** (walls 155, slabs 107, fences 34) and **blackstone** (123) are the industrial trim: fence posts, roof edges, sign frames, rail details. [H]
- **Iron**: iron block 60, iron bars 48, iron trapdoor 229, chain 185/191 (states), used for roof shutters, fences, hanging signs and the chicken ledge fence. [H]
- **Jungle planks/fences** (173/15) and **spruce planks** (31) / **stripped spruce logs** (22) are crates, scaffolds, posts and the start-pen trim. [H]
- **Daylight detectors** (48) are solar-panel details on the mine roofs, in four 3×2 arrays; they are **not** light sources. [H]

### 2.3 Branding / accents (the 0.5%)

- **MCC billboards**: white concrete band + red concrete letters + nether-brick fence posts + gray-wool cap + black/dark frame. Two of them: x=64 (y −15…−9, z 22–40) and x=241–242 (y 4…12, z 22–40). [H]
- **Start frame**: black wool (98) with a quartz floor; the barrier gate is invisible. [H]
- **Rail/wall**: black concrete (41) at x=137, y −28…−26, z 9–53, on a black-wool bed (88) at x 138–139, y=−29. [H]
- **Marker stakes**: red concrete (190) / red wool (110) small accents at x 25, 39–40, 51–52 on the first slope. [H]
- **Wool accents**: white wool 62 (finish towers / sign), gray wool 33 (sign cap), black wool 190 (start frame/rail bed), red wool 110 (sign/stakes). [H]

### 2.4 Vegetation

- **Oak leaves** 572 and **acacia leaves** 182; trunks/posts are `stripped_spruce_log` (22) and a single `stripped_dark_oak_log`. The trees are small acacia/oak clumps on the badlands top and around the finish. [H]
- **Cactus** 181: scattered as hazards on the course and as roof decoration on the mine wings; the datapack refills 179 positions (4 are missing from the reset). [H]
- **Dead bush** 15. [H]

---

## 3. Lighting

- **Time**: `DayTime=0`, `doDaylightCycle=false` → permanent sunrise/golden hour, no weather (`doWeatherCycle=false`, `raining=0`). [H `level.dat`]
- **Sky light** does the work on the open island; the map is fully day-lit. [H]
- **Baked lights**: 94 `sea_lantern` + 12 `lantern` (11 hanging, 1 standing) = 106 light blocks. They are concentrated in the mine wings, the cave chamber and the finish towers. [H]
- **No** glowstone, shroomlight, froglights, lava, fire, campfires, torches, candles, redstone lamps or beacon. [H]
- The cave is lit by lanterns/sea lanterns despite being under the mesa; the render shows the interior clearly. [H `cave_iso.png`]
- The renderer's bloom makes the sea lanterns read as bright cyan-white points; in-game they cast a cool light against the warm terracotta. [H render / M look]
- **Daylight detectors** do not emit light; they are a roof pattern, not lighting. [H]

The overall look is **warm orange badlands + cream mine + teal/cyan accents + red/white/black branding**, with cool lantern points. [H/M]

---

## 4. Build techniques

1. **Voxel badlands sculpting**: no WorldEdit smoothing; the terrain is built from stacked 1–4-block strata with noisy edges, overhangs and 1–3-block steps. The silhouette is a long ridge with hoodoos, not a smooth island. [H]
2. **Strata banding**: horizontal colour bands exposed on cliffs; the top is a separate noisy skin (red sand/dirt/gravel). The bands are consistent across the map, which sells the badlands read. [H]
3. **Terrain-first composition**: the mine, rails and finish are inserted into the landform rather than sitting on a flat platform. The course follows the canyon floor; the buildings flank it. [H]
4. **Two long mine wings**: 36×12×12 halls with white walls, cyan stripes, flat roofs, roof cacti, solar-panel arrays, chains and lanterns; the player runs through the open plaza between them. This is a cheap way to frame a route without a roof. [H]
5. **Billboard lettering**: 1-block-thick red concrete letters on a white band, held by a dark frame/posts; 19 long × 7–9 tall. The letters are large enough to read from 30+ blocks. [H]
6. **Rail motif**: 3-wide smooth-stone/stone-brick strips along z, a black concrete/wool ridge at x=137, and sloped ramps. The rails are decorative but give the map a "train yard" identity. [H/M]
7. **Cave chamber**: a west-facing void carved into the mesa, with a thin roof (the mesa top), two long white pillar walls, a smooth-stone base, lanterns/chains and a solid back wall. It is a **dead end**, used as a landmark/detour, not a tunnel. [H]
8. **Finish as a wall**: a 45-long gray-terracotta wall (z 9–53), four white corner towers, a fenced 1-wide chicken ledge at its base, and a 19-long MCC sign on top. The finish is visible from the mesa; the chickens are hidden behind the fence until the player is close. [H]
9. **Barrier gate**: invisible barrier walls with a visible black-wool/quartz frame; the datapack removes only the barrier volume at GO. This is a clean way to build a start pen without visible doors. [H]
10. **Command-free detail**: 0 command blocks; all logic is in two functions. The build has no redstone, no command blocks, no item frames, no armor stands, no containers. [H]

---

## 5. What to copy / what to avoid

**Copy** [M]:
- The **terrain-first ratio** (~98% terrain / ~2% build) and the strata banding; it makes a small block budget read as a place.
- The **flanking mine wings** and the **open plaza**; they frame the route without blocking sightlines.
- The **single strong landmark per leg**: MCC billboard → mine gate → black rail → cave mouth → mesa drop → MCC finish wall. The player always has a next silhouette.
- The **fenced chicken ledge** and the **big sign**; the finish is legible and thematic.
- The **invisible barrier pen** with a visible frame; it keeps players in without ugly walls.

**Avoid / adapt** [M]:
- **Dirt is not in the bridge allow-list**; either add it or avoid exposed dirt on the critical line.
- **Cave as a dead end** is a time trap; if copying the cave, give it a reward, a shortcut or a clear "no exit" read.
- **Open edges**: the island's irregular rim is beautiful but the void is the only bound; a race needs either a visible edge treatment or a play-bounds rule.
- **No spectator/return area**: the finish wall is decorative; finishers are teleported back to the pen. The current plugin's confined spectator + results screen is better for a live game.
- **No checkpoint/reset**: a single mistake costs the run; a 5-round game needs the plugin's respawn/checkpoint system.

---

## 6. Dimensions and budget

- Footprint: **251 × 75** blocks (x 4–254, z −5…69); height **74** (y −61…12).
- **411,834** non-air blocks; **62** block IDs; **97** block states; **16** region files; **27.0 MB** region data; **6,559** chunks (most of them empty void).
- The build is small enough to paste as a single map slot in the current `McmTgttos` world (500×500 slot, 600-block spacing) if the y offset and the dyed-concrete rule are handled. [M]
- Player kit adds `blue_concrete` (not in the build); the build has **544 dyed concrete** blocks (red 190, white 106, gray 97, light gray 80, black 41, cyan 28, light blue 2) — the current plugin's `TgMapData.check` rejects any dyed concrete because it reserves it for players. That is the main conversion conflict. [H code + H blocks]
