# LBR-1 — base island (status)

Festive MCC hub replica, base layer only. World `mcc_lobby_official`, build centre
`C = 5000, 120, 5000`. Nothing outside this folder was touched, and nothing was
run on the server.

## Bounds

| | x | z | y |
|---|---|---|---|
| island occupies | 4902 … 5106 (205 wide) | 4895 … 5097 (203 deep) | 90 … 122 |
| declared site (BRIEF) | 4860 … 5140 | 4860 … 5140 | 100 … 200 |

* Mean radius 98, silhouette range **86 … 110** (brief: "irregular, about 100").
* Walkable plane **y = 120**; deepest underside block **y = 90** exactly.
* The island reaches 10 blocks below the BRIEF's nominal `y 100` floor, because the
  island line ("underside down to y = 90") overrides the box. `X/Z` are inside the box.
* Site box cleared: `00_clear` fills `x 4886..5114, z 4886..5114, y 88..127` with air
  in 96 commands so the island cannot intersect existing terrain at the site.

## Block count

**238,735 blocks**, 32,524 top columns, 33 block types, all vanilla Java 1.21.1.

| section | blocks | what |
|---|---:|---|
| `mass` | 199,685 | island body: 3-layer crust, 2-block belly shell, solid outer wall, banded strata |
| `surface` | 17,118 | top layer (snow / grass / dirt / ridge / lip) + snow layers |
| `water` | 9,532 | lagoon ring, three ponds, water under the causeways |
| `paving` | 12,327 | plaza, 8 ribbons, 8 paths, causeway decks + piers, 8 portal pads |
| `detail` | 73 | frozen cascades, rim boulders, dripstone, sea pickles |

Main palette: `dirt, cobbled_deepslate, tuff, andesite, deepslate_tiles, stone, blackstone,
snow_block, deepslate, stone_bricks, water, sand, coarse_dirt, grass_block, white_concrete,
smooth_stone, light_gray_concrete, polished_andesite, dirt_path, gold_block, sea_lantern,
packed_ice, ice, blue_ice, copper_ore, raw_copper_block, obsidian, podzol, rooted_dirt`.

## Geometry

| feature | spec |
|---|---|
| plaza | disc radius 48.0 at y = 120: white/smooth-stone rings, deepslate rim ring, 8 tan ribbons, 8 gold+sea-lantern medallions at r = 38 |
| lagoon | ring r 48 … ~60.5 (outer shore wanders ±4 with a 2-harmonic wobble), water at y = 117/118, sand bed at y = 115/116, stone quay on the land side |
| causeways | 7 wide (5 tan body + 1 stone-brick kerb each side) across the lagoon, stone piers, water continues underneath |
| land ring | r ~60 … rim, snow / grass / coarse-dirt mosaic, soft 1–2 block drifts, 3 sunken ponds (22.5°, 112.5°, 247.5°) |
| paths | 7 wide, eight bearings, plaza → portal pad → rim promenade; every bearing verified continuous to within 2.5 blocks of the rim |
| portal pads | 19×19 flat apron at y = 120 centred on each portal (18×18 footprint + 1 margin), stone-brick border, light-grey band, smooth-stone field; path crosses the middle |
| rim | 1-block snow lip overhanging the edge, 5-block-thick solid wall, two-tier wall bottom that varies 94…107 by bearing, strata bands at y = 113 and 106 |
| underside | profile (radius scale by height): 1.00 down to y = 102, then 0.965 @ 99, 0.915 @ 96, 0.84 @ 93, 0.74 @ 91.5, 0.52 @ 90; four hanging rock spurs; hollow inside behind a 5-block solid wall |
| water edge | frozen cascades at 22.5° and 202.5° (blue/packed ice down the wall face), ice shelves at the rim, no flowing water sources (see gaps) |

## Functions

Datapack root: `datapack/` (`pack_format 48`, namespace `mcchub`, folder `function/lbr1`).
Run once, bottom-up, at the site:

```
/function mcchub:lbr1/load
```

| file | commands | contents |
|---|---:|---|
| `00_clear` | 96 | air over the site box |
| `01_mass_01..04` | 8,000 / 8,000 / 8,000 / 5,249 | island body, y-ascending |
| `02_surface_01` | 1,814 | top layer |
| `03_water_01` | 330 | water volumes |
| `04_paving_01` | 2,269 | plaza, paths, pads, causeways |
| `05_detail_01` | 50 | dressings |
| `load` | 9 calls | ordered entry point |

**33,817 commands total**, split at ~8,000 commands per file. Largest single `/fill`
is 25,230 blocks (engine limit 32,768). Detail blocks use `setblock`, masses use `fill`
via greedy x/z rectangle merge with vertical coalescing.

## Preview

* `preview_top.png` — `--top --scale 4` (orthographic plan)
* `preview_iso.png` — `--scale 4` (isometric, the same camera as the official hub render)
* `island.mcmdump.gz` — region-relative MCMDUMP used for both renders

Regenerate everything:

```
python gen_island.py                 # blocks.csv, island.mcmdump.gz, datapack functions
python verify.py                     # contract checks, exits non-zero on failure
python lint_functions.py             # command syntax / fill size / bounds
python ../../../tools/render_preview.py island.mcmdump.gz preview_top.png --top --scale 4
python ../../../tools/render_preview.py island.mcmdump.gz preview_iso.png --scale 4
```

## QA (last run)

```
blocks            238,735        columns 32,524
bounds            x 4902..5106  y 90..122  z 4895..5097
plaza             clean at y=120/121, 0 holes
pads              8/8 flat 19x19
paths             8/8 continuous plaza -> rim (r 40 .. 86..104 per bearing)
water             no surface block without water beneath
lint              10 files, 33,817 commands, largest fill 25,230 -> PASS
```

## Not built here (other owners)

No dome, drum, crown, bow, clock or sign (LBR-2). No mountain, globe, outer terraces or
shoreline dressing (LBR-3). No portal facades, stalls, sleigh, balloons, trees, lamps or
presents (LBR-4). The eight pads are terrain only: flat stone aprons, no buildings.

Reserved and left clear:

| owner | volume | state |
|---|---|---|
| LBR-2 dome | disc r ≤ 27.5 at y = 120, up | smooth/polished stone floor, nothing above y = 120 |
| LBR-2 entrance | the 4°–356° plaza apron | paving only |
| LBR-4 portals | each 19×19 pad + 2 block margin | flat y = 120, no mounds, no ridge, path crosses |
| LBR-3 mountain | west land ring, x ≲ 4930 | flat snow at y = 120, one 19×19 pad and promenade inside it |
| LBR-3 globe | south, centre 5000,5080 | flat pad surface at y = 120 |

## Gaps and things the coordinator must decide

1. **Layout conflict — west portal vs THE CHAMPS.** The BRIEF puts the mountain at
   `4900, 120, 5000` with a ~60×40 footprint (x 4870..4930) and the Build Mart portal pad
   at `4928, 5000` (x 4919..4937, z 4991..5009). They occupy the same ground; the mountain
   as specced also reaches x 4870, while the island rim on the 180° bearing is x ≈ 4906,
   so ~35 blocks of the footprint would hang off the edge. Either the mountain moves in
   (centre ≈ 4965) or it is built as a rim massif with its own cliff (as in
   `official_hub_iso.png`). Nothing on my side blocks either option.
2. **Layout conflict — south portal vs the globe.** Globe centre `5000, 132, 5080`, r 12
   (footprint z 5068..5092) overlaps the TGTTOS pad `5000, 5072` (z 5063..5081). The globe
   will stand on the pad's back half. Fine visually if intended, but it is not two separate
   sites.
3. **Water is static.** Every water block is `water[level=0]` with no neighbouring air at
   the same level except at the causeway piers, so nothing flows. The two "cascades" are
   `blue_ice`/`packed_ice` sheets on the rim face, not flowing water — a real source there
   would fall into the void forever. If LBR-3/LBR-4 want flowing water, add it knowingly.
4. **Hollow interior.** The island is a shell: 4 strata of crust, a 2-block belly shell and
   a 5-block solid outer wall. It is sealed; nothing inside is visible. If anyone needs to
   cut into the rim (stairs, a tunnel, the mountain), expect air behind the wall.
5. **19×19 pads, not exactly 18×18.** INTEGER alignment on `r = 72` gives odd-width pads; I
   used 19×19 (footprint + 1 margin) so the path axis stays centred. Say the word and I
   re-cut them to 18×18 with a half-block offset.
6. **The plaza apron is deliberately quiet.** Cream/white rings, tan ribbons, dark rim and
   eight gold medallions. No banners, lanterns or signage — LBR-4 territory. If the plaza
   reads too empty once the dome lands, that is where the dressing should go.
7. **`y` headroom.** Nothing except the rim ridge (y 121–122), boulders (y 121–122) and the
   snow lip (y 120) exists above the walk plane. The dome has open sky from y = 121 up.
8. **Server cost.** 238,735 blocks in ~33.8k commands. On Paper this is one heavy burst;
   run the numbered files individually with a small pause if the tick budget is tight.

## File map

```
build/lbr-1/
  gen_island.py             single deterministic generator (no external data)
  blocks.csv                x,y,z,block,section — 238,735 rows + header
  island.mcmdump.gz         render dump (region relative)
  preview_top.png           plan render
  preview_iso.png           isometric render
  verify.py                 contract checker (plaza, pads, paths, bounds, underside)
  lint_functions.py         command syntax / fill-size / bounds lint
  datapack/pack.mcmeta
  datapack/data/mcchub/function/lbr1/*.mcfunction
  STATUS.md                 this file
```
