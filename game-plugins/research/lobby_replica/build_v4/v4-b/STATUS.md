# V4-B — THE CHAMPS / Hall of Fame biosphere / rainbow bridge / snow globe

**Checkpoint written mid-iteration at the owner's request.** Everything listed under
*complete* is generated, written to disk and rendered. Everything under *partial* is
buildable but has a known visual defect. Nothing was pasted, executed or saved into any
world; `25580` and `25591` were never touched.

Build origin `C4 = 0,120,0`. All coordinates below are world coordinates.

---

## 1. Numbers (from the last full run, `build.log`)

| | |
|---|---|
| Blocks | **68,333** |
| Commands | **44,948** in 22 `.mcfunction` files (+ `load.mcfunction`) |
| Bounds | x **-116..91**, y **121..177**, z **-30..71** |
| Lowest block | y = 121 (nothing at or below 120) |
| Block ids | 45, all vanilla 1.21.1 `minecraft:` |
| Largest file | `10_mountain_rock_01` 4,000 commands / 164 KiB |

Sections and their block counts:

| Section | Blocks | State |
|---|---:|---|
| `10_mountain_rock` | 42,303 | complete |
| `11_mountain_lava` | 147 | partial — far too few veins vs reference |
| `12_mountain_snow` | 167 | partial — sparse, reads as patches |
| `13_mountain_terrace` | 1,298 | complete (belvedere + parapet + tunnel door + ledge) |
| `14_champs_sign` | 1,716 | partial — letter ghosting bug (see §5) |
| `20_hof_plinth` | 4,839 | complete |
| `21_hof_dome` | 2,948 | complete |
| `22_hof_interior` | 546 | complete |
| `23_hof_sign` | 686 | complete (curved onto the drum) |
| `30_bridge` | 3,152 | complete |
| `31_bridge_fittings` | 303 | complete |
| `40_globe_base` | 4,824 | complete |
| `41_globe_glass` | 2,884 | complete |
| `42_globe_diorama` | 2,520 | complete |

---

## 2. What is complete

### THE CHAMPS mountain — x -116..-72, z -30..20, peak y = 177
* Height field: N-S ridge (`X_PROF` / `Z_PROF` profiles) tapered to zero on the west and
  north edges, sheer on the south and east where the reference is sheer.
* 17 rock spikes (`SPIRES`) give the jagged crown; 2-block terracing plus strata ledges
  (`hi % 8 in (0,1)`) and mirror-image rock lips under overhangs.
* Palette by relative height: `deepslate / cobbled_deepslate / blackstone` at the base,
  `andesite / stone / tuff` mid, `calcite / light_gray_concrete_powder` near the peaks,
  mottled per block (no horizontal bands).
* Rock spurs poke into the plaza at x = -71..-70; the cliff foot zigzags in plan
  (z 8..11 per column) so the south face is not one flat wall.
* Snow caps on local maxima above y = 158, dusting above y = 150.

### `THE CHAMPS` marquee — backdrop plane z = 12, board z = 13, letters z = 14
* 31-wide stepped red backdrop (x -104..-74, y 130..161) with gold studs and
  `red_terracotta` strata; 25 × 17 black board; white 3×5 letters `THE` (y 143..147) over
  `CHAMPS` (y 136..140); 15 × 8 gold crown above at y 152..159; marquee bulbs every 2 blocks
  around the board; two red banner flags on poles at x = -108 and -70.
* Mounted high on the cliff on purpose: at y 121..152 the biosphere hides it from the
  entrance, at 130..161 the letters clear the dome's silhouette.

### Stone belvedere (bridge head) — x -81..-75, z -6..3, deck y 155
* Solid masonry body down to the rock, `smooth_stone` deck with a red inlay stripe,
  white quartz parapet, gold finials, two lantern posts framing the bridge mouth.
* Tunnel door at the foot of the plaza cliff (x = -73, z 3..7) with a chiselled frame and a
  dark tunnel behind, implying the way up.

### Hall of Fame biosphere — centre (-70, 45)
* Plinth r 21.5, courses at y 121/122, red interior floor at y 123.
* Drum y 123..129, r 20: quartz field, white pilasters every 30°, **gold band at y 129**
  (the yellow ring in the reference), doorway 7 wide (z 41..47) on the east at y 123..128
  with a quartz frame and gold caps.
* Glass dome y 130..150, r 20 hemisphere: 8 meridian ribs every 45°, quartz apex cap.
* Interior: red carpet, 6-arc rainbow floor inlay at r 16..17, 8 quartz-pillar columns with
  gold capitals, stepped white/gold dais, gold championship coin (9-block disc, 2 thick)
  facing the door, 4 lanterns on chains.
* `HALL OF FAME` marquee **curved onto the drum's south face** (each cell placed at
  `hof_surf_z(x,y)`, so the panel hugs the curve instead of hovering): quartz frame,
  red panel, gold `OF` between white `HALL` and `FAME`, letters mounted directly on the
  red (no black board — that is how `HallOfFame.png` does it).

### Rainbow bridge — x -75..-31, z -5..2, deck y 155, crown y 176
* 8 wide, 9 courses deep: red deck, orange, yellow, lime, cyan, light_blue, blue, purple,
  red underside fascia. Sine-eased arch (`sin(pi t)^0.72`).
* Snow crust on the two deck edge rows; red underside arch ribs every 6 blocks.
* East landing: 6 × 10 gold-trimmed balcony at y 154..155 with a white parapet, gold
  corner caps and a gold keystone, stopping 1 block clear of the reserved plinth (r 30).

### Snow globe — centre (75, 55), glass centre y 140, r 14
* Stepped stone base r 16.6..13.9, y 121..127, white/quartz rim, snow + packed ice inside.
* Glass sphere y 126..154, light blue stained glass with darker blue accents low down.
* Diorama clipped to r ≤ 13 so nothing pierces the shell: a concave snow bowl
  (`globe_ground`), 9 snowy conifers 7..13 tall, a stepped snow castle (7×7 → 5×5 → 3×3,
  y 128..145) with battlements, windows, side wings and a gold crown at y 146..148, and
  5 wrapped presents.

### Checks that ran and passed
```
below_y121           : 0
outside_region       : 0
inside_dome_plinth   : 0     (nothing of mine inside r30, y<=128 — the reserved dome)
globe_in_dome_plinth : 0
```

---

## 3. What is partial

| Item | Defect |
|---|---|
| `14_champs_sign` | The two letter rows render with a vertical ghost/echo in `detail_mountain.png`; the ASCII dump confirms the glyphs are correct, so the cause is in the placed geometry (most likely the `by0-1` board lip row or the bulbs overlapping a letter row). **Not fixed.** |
| Mountain silhouette | Still reads slightly block-like from the south-east: the south cliff is only ~28 deep in relief. Needs deeper gullies and more spike variety. |
| Lava veins | 147 blocks. The steepest-descent walk dies early on the 2-block terraces. The reference has long, obvious orange streaks; this does not. |
| Snow | 167 blocks — reads as patches, not a snow-capped massif. |
| HoF dome | Reads as a white lattice at the top (12→8 ribs converge). Glass panels are `minecraft:glass`, which the renderer draws at alpha 0.28, so the dome is nearly invisible except its ribs. |
| Globe glass | `light_blue_stained_glass` at alpha 0.55 turns muddy/dark in the iso preview; the interior reads as noise at preview scale (fine at detail scale). |

---

## 4. What remains

1. Fix the marquee letter ghosting (§3), then re-render `detail_mountain.png`.
2. Add a **front elevation renderer** (`render_front.py`, like `build/lbr-2/`) so this pack
   can be compared side-by-side with `MCCHub-SD.png` / `MCCHub-S4.png` — the brief's
   acceptance test currently cannot be run, because `tools/render_preview.py` only does
   iso and top.
3. Re-run the overlap check properly: `mountain_in_hof_plinth` reports 3,633, but the check
   is **mis-scoped** — it filters `x <= -74, z >= 22` inside r 21.6 of the HoF centre, which
   is exactly where the HoF's *own* plinth sits. It is counting my own plinth, not rock.
   The real test is "`10_mountain_rock` cells inside the plinth disc", which should be 0
   (the apron stops at z = 20, the plinth starts at z 23.5) but has not been re-run.
4. More lava veins and a denser snow cap.
5. Adjust the mountain's south face so the marquee's edges are backed by rock, not sky.

---

## 5. Files

```
v4-b/
  gen_v4b.py                  generator (single source of truth, re-runnable)
  blocks.csv                  68,333 rows, x,y,z,block
  functions/                  flat copies of all 23 .mcfunction files
  datapack/pack.mcmeta        pack_format 48 (1.21.1)
  datapack/data/mcchub/function/v4b/*.mcfunction
  preview.mcmdump.gz          build + preview-only ground disc (not emitted)
  preview_iso.png             1468x912 isometric
  preview_top.png             2147x1247 orthographic top map
  detail_mountain.png         close-up of the massif and marquee
  detail_hof.png              close-up of the biosphere and sign
  detail_hof_cutaway.mcmdump.gz   glass removed above y 132 (preview only)
  detail_bridge.png           close-up of the rainbow and its landings
  detail_globe.png            close-up with the shell cut away
  build.log                   last run's numbers
  crop.py / gridcrop.py / measure.py / sample_strip.py   reference study tools
  crops/                      the reference crops these decisions were made from
  STATUS.md, GAPS.md
```

Install: **merge** `datapack/data/mcchub/function/v4b/` into the shared `mcchub` datapack
(one `pack.mcmeta`, several function trees), `/reload`, then `function mcchub:v4b/load`.
Or paste `blocks.csv` with any structure tool — the CSV is authoritative.

The preview dump adds a **preview-only** snow disc at y = 120 for scale. It is not part of
the emitted functions and must never be pasted.
