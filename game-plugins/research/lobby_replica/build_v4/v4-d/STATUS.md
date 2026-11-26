# V4-D — Aqua building, six training portals, props

**Section (BRIEF.md §Ownership):** V4-D = Aqua building / water ride, six
training portals (Ace Race, Battle Box, Dodgebolt, Parkour Warrior, Parkour
Tag, Rocket Spleef Rush) and the surrounding props (snow, trees, boats,
hot-air balloons, presents, candy canes, lamps, market stalls).
**Not built here:** dome/plinth, base island and water, THE CHAMPS, glass Hall
of Fame, rainbow bridge, snow globe, team columns, stage plots.
**Server commands run:** none. Nothing was pasted into 25591 or 25580.

Site: `C4 = 0,120,0`. **Nothing is placed below y = 121** — the island surface
(y = 120, V4-A) is never written to, so this pack can be layered on top of
any island shape V4-A produces.

---

## 1. State at handoff (emergency checkpoint)

| Item | State |
|---|---|
| `gen_v4d.py` | **Complete and running.** Single source of truth, re-runnable. |
| `blocks.csv` | **Complete**, 69,171 rows `x,y,z,block`. |
| `functions/*.mcfunction` | **Complete**, 14 files + `load.mcfunction`. |
| `datapack/` | **Complete** (`pack.mcmeta` format 48, `data/mcchub/function/v4d/`). |
| `preview.mcmdump.gz` | **Complete** (build + preview-only ground). |
| `preview_top.png`, `preview_iso.png`, `preview_iso_nw.png` | **Complete**, rendered from the build **before** the last cosmetic tweak (see §7). |
| `detail_*.png` (7) | **Complete**, same caveat. |
| `build_report.txt` | Captured generator report for this build. |
| `STATUS.md`, `GAPS.md` | This file and its companion — **complete**. |

**Nothing is half-written.** The last change (balloon moved off the Aqua roof,
Rocket Spleef board widened to 49×18) is already inside `gen_v4d.py`,
`blocks.csv` and the `.mcfunction` files. Only the PNGs predate it.

---

## 2. Numbers

| | |
|---|---|
| Blocks placed | **69,171** |
| Commands | **25,351** in 14 chunk-safe files (max 4,552 per file) |
| Bounds | x **-117 .. 140**, y **121 .. 200**, z **-139 .. 73** |
| Distinct block ids | 80, all vanilla Java 1.21.1 |
| Blocks below y = 121 | **0** |
| Blocks inside the rainbow plinth (r < 33) | **0** |
| Blocks inside another agent's named volume | **0** |

| Section (`.mcfunction`) | Commands | Blocks |
|---|---:|---:|
| `00_aprons` | 423 | 3,210 |
| `01_ace_race` | 3,642 | 9,247 |
| `02_battle_box` | 1,825 | 7,993 |
| `03_dodgebolt` | 3,208 | 6,449 |
| `04_parkour_warrior` | 4,552 | 10,326 |
| `05_parkour_tag` | 2,089 | 7,559 |
| `06_rocket_spleef` | 2,487 | 5,204 |
| `07_aqua` | 3,015 | 10,798 |
| `10_market` | 743 | 2,007 |
| `11_trees` | 1,146 | 2,490 |
| `12_lamps_canes` | 320 | 342 |
| `13_presents` | 211 | 252 |
| `14_boats` | 184 | 558 |
| `15_balloons` | 1,506 | 2,736 |

Every file is far below the 8,000-command chunk limit; not one needed a `_02`
part. Install: copy `datapack/` into the world's `datapacks/`, `/reload`, then
`function mcchub:v4d/load`. Or paste `blocks.csv` with any structure tool (the
CSV is authoritative).

---

## 3. Site plan — the rectangles this agent reserves

All plots are axis-aligned. Facades face the plaza; block text needs a cardinal
plane, so every facade is cardinal rather than radial.

| Build | Facade faces | Plot x | Plot z | Reference |
|---|---|---|---|---|
| Ace Race (sea serpent) | south | -118..-32 | -100..-56 | `MainHub-AR.png` |
| Battle Box | south | -45..-5 | -119..-89 | *(none — see GAPS 3.1)* |
| Parkour Warrior (frog) | south | 0..48 | -118..-82 | `MainHub-PKW.png` |
| Dodgebolt (arena) | south | 52..-100 | -100..-60 | `MainHub-DB.png` |
| Parkour Tag | east | -120..-80 | -70..-30 | *(none — see GAPS 3.1)* |
| Rocket Spleef Rush (gantry) | west | 85..133 | -60..-20 | `MainHub-RSR.png` |
| Aqua building + pool | west | 40..101 | -22..22 | `MCCHubSeason1.png` east, `MCCNewHub.jpg` |

Other agents' volumes I keep clear of (checked every build): THE CHAMPS
`x -122..-67, z -25..25`; Hall of Fame `x -95..-45, z 20..70`; snow globe
`x 56..94, z 36..74`; the plinth `r < 33`; the red-carpet axis `|x| <= 14,
z > 30`; V4-C's south band `z > 76`.

Deliberate airborne overhangs (no ground footprint, nothing to collide with):

* **Rocket Spleef board** `x 113..114, y 130..147, z -64..-16` — 4 blocks
  wider than its plot at each end. The nearest structure (Dodgebolt) stops at
  `x = 100`, so there is no intersection.
* **Battle Box pennant** 2 blocks past the plot edge at `y 149..151`.
* **Hot-air balloons** sit at `y 160..200`; four of them are above other
  agents' footprints by design (the reference has balloons over the dome).
  Checked separately: **0** of their blocks are below y = 150.

---

## 4. What was built, and how it matches the references

### 4.1 Ace Race — giant sea serpent (`MainHub-AR.png`)
An arching serpentine body swept along a hand-placed path (17 stations, radius
1 → 4.5), `x -115..-48`, crown at `y 154`, span 67 blocks (reference ratio
width:height ≈ 2:1). Warm terracotta hide (four oranges/browns in coarse 3×3
patches), pale plated belly wherever the surface normal points down, dorsal
spines on the ridge. At the east foot: a blue-faced head with a red mouth
carved through, white teeth top and bottom, black eyes on the brow — the
reference's closed-jaw monster. On the beach: a sand cay (irregular ellipse
76×38, stone lip), a spruce shack with barrel and chest, a rowboat pulled up on
the sand, the plaque path, and a timber `ACE RACE` board.

### 4.2 Battle Box — portal pavilion (no direct reference)
White shell, red base band, glazed front (`light_blue_stained_glass`, white
mullions), arched entrance with a gold frame and a portal panel inside, white
door canopy, red crest panel with the gold MCC flame, gold crenellated crown,
snow-dusted cornice, white marquee on two posts, side flagpole with a red/gold
pennant. Grammar taken from the portal building visible in `MCCHub-S4.png`.

### 4.3 Dodgebolt — twin court (`MainHub-DB.png`)
33×27 court at `y 121` with two team halves (white with three thick
blue / red rings, black centre square, navy edge), light-blue glass barriers,
five tiers of plank seating per long side with a cyan terracotta back wall,
eight stone-brick pillars carrying two long white canopy beams plus end beams,
a sandstone end wall with the yellow flame crest, and a wide stair entrance
with white glass rails. The deck is **raised** to `y 124` rather than dug in
(see GAPS 2.1).

### 4.4 Parkour Warrior — teal frog + sign (`MainHub-PKW.png`)
Stone plinth 30×25 (`y 121..125`) with an elliptical stone outline, stepped
front, and a lava-lit dark mouth under the frog. The frog is four ellipsoids
(body, two haunches, head) plus modelled front legs with splayed cream toes,
raised eye sockets with black pupils and a brow ridge. 19 wide × 24 long ×
14 tall, mottled `cyan_concrete` / `prismarine` / `cyan_terracotta` with a
cream belly. Behind it, two stone-brick pillars carry a 47×20 blue board with
`PARKOUR` / `WARRIOR` in beige 5×7 sandstone letters, top at `y 168`.

### 4.5 Parkour Tag — portal pavilion (no direct reference)
Same white/glass/gold grammar as Battle Box, with a magenta base band, magenta
crest strip, stepped ziggurat roof cap, a magenta pennant on a side pole, and a
white board with magenta `PARKOUR` / `TAG`.

### 4.6 Rocket Spleef Rush — launch gantry (`MainHub-RSR.png`)
Two hollow riveted pillars (7×6, `y 121..146`) in grey concrete with black
courses and three yellow/black hazard collars, iron deck with railings and
white cross braces, a dark-oak truss arch under the sign, and the 49×18 navy
marquee with `ROCKET` / `SPLEEF` in yellow. Two red/white/gold rockets stand on
the pillar caps (16 and 20 tall) with magma engines, gold fins and a static
smoke puff trail. A white launch walkway with a red/white striped barrier runs
from the plaza under the arch, flanked by blackstone lamps.

### 4.7 Aqua building and water ride (east at `80,120,0`)
34×25 pavilion, `y 121..147`: blue base band, white shell with corner ribs,
two-storey glazed front, balcony across the front on stilts with a white
railing, flat roof with skylights, vent stack and rail, gold crest, and the
sign stack — blue `AQUA` board with a red `MCC` board and gold crown above it
(the `MCCHubSeason1.png` east façade). Water: a raised channel from the front
door, a lip, then a 21×21 pool with a tiled edge, sea-lantern underwater
lights, a diving board, two moored boats, deck chairs and snow.

### 4.8 Props
* **Market stalls** — 8 tents, 9×7, striped chequered awnings, spruce-log
  posts, barrel shelving at the back, counter with goods (cake, pumpkin,
  melon, hay, gold, chiselled bookshelf, sweet berries), lantern, snow, candy
  cane at the corner. Placed on the free ring `r 60..80` with an 8-block
  clearance from every reserved volume.
* **Trees** — palms (curved trunk, drooping fronds) on the beach arcs, snowy
  spruces with layered snow rims and a few red-baubled ones with a gold star.
  30 in total, all `r 66..122`, none inside a plot or another agent's volume.
* **Lamps** — 16 dark-oak lantern posts: two flanking each portal forecourt
  plus the free arcs of the plaza rim at `r = 44`.
* **Candy canes** — red/white, 5 tall, in pairs at every portal threshold plus
  one per market tent.
* **Presents** — 1–3 block boxes with ribbon and bow in clusters on the market
  ring and at each portal threshold; a few snowmen with top hats and coloured
  scarves on the snowy arcs.
* **Boats** — three rowboats and one white/blue river steamer with a black
  funnel, at `r 135..145` on the water ring.
* **Balloons** — six gored envelopes (9 wide, 12 tall, 14 colour segments,
  wicker basket, chain rigging, shroomlight burner) at `y 172..194`.
* **Snow** — clustered dusting on every roof, canopy, awning, tree crown and
  the aprons/sand off the walking lines. Coarse noise so it reads as drifts.

---

## 5. Verification run by the generator on every build

| Check | Result |
|---|---|
| Blocks below y = 121 | 0 |
| Blocks inside the reserved plinth (r < 33) | 0 |
| Blocks inside THE CHAMPS / Hall of Fame / snow globe volumes below y 150 | 0 |
| Blocks over those volumes above y 150 (balloons only) | 1,104, by design |
| Blocks outside the briefed build area `x,z ±150, y 90..210` | 0 |
| Blocks outside this agent's own plots (structures) | 2 (Battle Box pennant) |
| Commands per file | max 4,552 |
| Block ids | 80 distinct, all vanilla 1.21.1 |

---

## 6. Files

```
v4-d/
  gen_v4d.py                  generator (re-runnable, ~1,600 lines)
  blocks.csv                  69,171 rows, x,y,z,block
  build_report.txt            last generator report
  functions/                  14 .mcfunction + load.mcfunction (flat copy)
  datapack/pack.mcmeta        pack_format 48 (1.21.1)
  datapack/data/mcchub/function/v4d/*.mcfunction
  preview.mcmdump.gz          build + preview-only ground disc
  preview_top.png             1320x1320 orthographic top
  preview_iso.png             1874x1256 isometric from the south-east
  preview_iso_nw.png          1874x1032 isometric from the north-west
  detail_ace_race.png         detail_battle_box.png    detail_dodgebolt.png
  detail_parkour_warrior.png  detail_parkour_tag.png   detail_rocket_spleef.png
  detail_aqua.png
```

The preview dumps contain a **preview-only** stand-in for V4-A's island
(grass disc to r = 118, sand to 128, water beyond) at `y = 120`. It is not in
`blocks.csv` and must never be pasted.

---

## 7. Known deltas at handoff

1. The PNGs were rendered **before** the final cosmetic patch: the pink/purple
   balloon was moved from above the Aqua roof to `(114, 180, 30)` and the
   Rocket Spleef board was widened from 39 to 49 blocks. Both are in the
   generator/CSV/functions. Re-render with
   `python tools/render_preview.py preview.mcmdump.gz preview_iso.png --scale 3`
   to bring the images level.
2. The Rocket Spleef board is 4 blocks wider than its plot at each end
   (airborne, no intersection — see §3).
3. No in-game test: nothing was pasted, so lighting, ladder/rail connections
   and the readability of the 5×7 marquees at walking distance are judged from
   renders only.
