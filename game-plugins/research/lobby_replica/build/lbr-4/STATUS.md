# LBR-4 — game portal ring, market, sleigh, balloons

**Section:** eight game-portal facades + hub dressing around `C = 5000,120,5000`
in `mcc_lobby_official`.
**Owner scope (BRIEF.md §Ownership):** portal facades, market stalls, sleigh /
balloons, trees, lamps, presents. Nothing else was touched.
**Server commands run:** none. Nothing was pasted, executed or saved into the world.

---

## 1. Summary

| | |
|---|---|
| Blocks placed | **45,010** |
| Commands | 16,616 in 15 `.mcfunction` files (+ `load.mcfunction`) |
| Bounds (world) | x **4918..5091**, y **121..181**, z **4909..5082** |
| Lowest block | y = 121 (nothing at or below y = 120) |
| Tallest portal | y = 150 (all eight, roof icon included) |
| Portal heights | 30 blocks — brief allows 20–30 |
| Lots | 8 × 18×18, sitting exactly on LBR-1's portal pads |
| Roof + floor | every portal has a coloured roof, coloured frieze and paved plinth |

Everything is Java 1.21.1 vanilla (`minecraft:` ids only, no custom blocks).

---

## 2. The eight portals

Lot = 18×18 axis-aligned, biased one cell outward so LBR-1's pad rim stays
visible as a forecourt. Each lot carries the same building grammar so the ring
reads as one family:

```
121          paved plinth (18x18, 45 deg cut corners, inlaid threshold)
122-127      arcade: 6-wide / 6-tall arched entrance, jambs, side lights
128-129      frieze band - the game's graphic signature
130          coloured canopy / awning (20-29 wide, lanterns on the front edge)
132-142      name marquee (black board, white 3x5 block letters, 2 lines)
143-144      cornice, 3-course hip roof to 147, gold finial and the
             game's roof icon at 148-150
             (HITW: 3-line sign 132-148, lime crown instead of a hip roof)
```

| Game | Bearing | Pad centre | Lot (x) | Lot (z) | Facade faces | Sign lines |
|---|---:|---|---|---|---|---|
| Ace Race | 0° | 5072,5000 | 5064..5081 | 4991..5008 | west | `ACE` / `RACE` |
| Battle Box | 45° | 5051,5051 | 5043..5060 | 5043..5060 | north | `BATTLE` / `BOX` |
| TGTTOS | 90° | 5000,5072 | 4991..5008 | 5064..5081 | north | `TGTTOS` / `CHICKEN` |
| Sky Battle | 135° | 4949,5051 | 4940..4957 | 5043..5060 | east | `SKY` / `BATTLE` |
| Build Mart | 180° | 4928,5000 | 4919..4936 | 4991..5008 | east | `BUILD` / `MART` |
| Sands of Time | 225° | 4949,4949 | 4940..4957 | 4940..4957 | south | `SANDS` / `OF TIME` |
| Hole in the Wall | 270° | 5000,4928 | 4991..5008 | 4919..4936 | south | `HOLE` / `IN THE` / `WALL` |
| Parkour Tag | 315° | 5051,4949 | 5043..5060 | 4940..4957 | west | `PARKOUR` / `TAG` |

**Why the four diagonal portals have a cardinal facade.** A 3×5 blocky font
needs exactly one block per glyph pixel. On a 45° plane the same pixel lands on
1–2 blocks (the lattice spacing is 1/√2), so letters fuse into bars — tried it,
it is unreadable. The diagonal lots therefore turn their facade 45° to the next
cardinal (a consistent pinwheel: SE→north, SW→east, NW→south, NE→west). The
entrance still faces within 45° of the plaza, the sign is crisp, and the
approach from the causeway reaches the lot corner. All geometry stays
axis-aligned, so it is also robust for any paste tool.

Palettes (`wall` = dominant, plus frieze, canopy, roof pattern, floor, glass,
light):

| Game | Palette |
|---|---|
| Ace Race | cyan/white concrete, black jambs, **checkered** cyan/white roof, checker frieze, black+white floor, sea lanterns |
| Battle Box | white shell with **blue half / red half** walls and roof, iron pilasters, red+blue floor |
| TGTTOS | yellow concrete + black bands, yellow/black striped roof, chevron frieze, shroomlights |
| Sky Battle | light-blue + white, white-wool/light-blue banded roof, cloud frieze, light-blue glass |
| Build Mart | green concrete + gold, gold-striped roof, gold pilasters, gold-banded floor |
| Sands of Time | smooth sandstone + teal, cut-sandstone/teal striped roof, gold pilasters, teal glass |
| Hole in the Wall | lime + white, lime/white striped roof, lime grid frieze |
| Parkour Tag | orange + magenta, striped roof, speed-chevron frieze |

Each portal interior (reachable through the arch) has the game's floor pattern,
a carpet runner, a gold-framed glowing back panel, hanging lanterns, a barrel
and a chest.

On the roof ridge stands a 3-block silhouette icon (148–150) so the skyline is
readable even when the sign is seen edge-on: checkered flag (Ace Race), crossed
swords (Battle Box), chicken head (TGTTOS), cloud (Sky Battle), gold-laden cart
(Build Mart), hourglass (Sands of Time), speed chevron (Parkour Tag).
Hole in the Wall has no room for one — its three-line sign fills the upper
storey.

---

## 3. Hub dressing

| Item | Where | Notes |
|---|---|---|
| Market stalls | 16 stalls (2 per mid-bearing) at r ≈ 70 | 6×6, striped wool awnings, spruce counter, goods (barrels, cakes, pumpkins, melons, hay), hanging lanterns, candy-cane post. Facing snapped to the nearest cardinal. |
| Trees | 26 snowy spruces in the wedges between pads, r 63–81 | 6–9 tall, snow-dusted outer rings, ~4 decorated with red baubles and a gold star |
| Lamp posts | 16 on the causeway shoulders (r ≈ 51.5) + 16 gate posts framing each portal door | candy-cane red/white posts, gold collar, lantern, snow cap |
| Bunting | 8 strings across the causeways | sagging red/white/green wool flags, sag kept at y 124–125 so it clears heads |
| Presents | 16 at portal thresholds + 24 scattered by stalls/trees | 1×1×1 to 2×2×2 concrete boxes with white/gold ribbon and bow |
| Sleigh | flying at (5045, 156, 5069), heading west | red sleigh with gold rail, Santa (hat, beard, sack), two reindeer with antlers, harness |
| Balloons | 3 at r 88–92, y 158–176 | striped envelopes (9×11), gold band, chains and wicker basket, shroomlight burner |

No prop stands on the lagoon: a footprint test keeps presents/stalls/trees off
LBR-1's water ring (verified: 0 ground blocks over water).

---

## 4. Files

```
lbr-4/
  gen_lbr4.py                  generator (single source of truth, re-runnable)
  blocks.csv                   x,y,z,block for all 45,010 blocks
  functions/                   same 15 files, flat copy for inspection
  datapack/pack.mcmeta         pack_format 48 (1.21.1)
  datapack/data/mcchub/function/lbr4/*.mcfunction
  preview.mcmdump.gz           MCMDUMP of the build + preview-only context
  preview_top.png              632x632 orthographic top map
  preview_iso.png              1196x758 isometric
  detail_ace_race.png          close-up, cardinal portal (Ace Race)
  detail_battle_box.png        close-up, diagonal portal (Battle Box)
  detail_market.png            close-up, stalls + two portals
```

| Function | Commands | Blocks |
|---|---:|---:|
| `00_plinth` | 1,539 | 2,337 |
| `01_ace_race` | 1,307 | 4,584 |
| `02_battle_box` | 1,510 | 4,566 |
| `03_tgttos` | 1,549 | 4,709 |
| `04_sky_battle` | 1,308 | 4,594 |
| `05_build_mart` | 1,288 | 4,566 |
| `06_sands_of_time` | 1,586 | 4,581 |
| `07_hole_in_the_wall` | 1,545 | 4,553 |
| `08_parkour_tag` | 1,384 | 4,678 |
| `10_market_stalls` | 1,270 | 2,087 |
| `11_trees` | 1,418 | 2,662 |
| `12_lamps_bunting` | 219 | 219 |
| `13_presents` | 164 | 191 |
| `14_sleigh` | 70 | 92 |
| `15_balloons` | 459 | 591 |
| `load` | 15 | — (calls the above in order) |

Run order matters only for the plinth (first). Every file is far below the
8,000-command chunk limit, so no section needed splitting.

Install: copy `datapack/` into the world's `datapacks/` folder, then
`/reload` and `function mcchub:lbr4/load`. Or paste `blocks.csv` with any
structure tool (the CSV is the authoritative list).

**Integration note:** the pack uses namespace `mcchub` like LBR-1's pack. Do not
drop this `pack.mcmeta` next to LBR-1's — merge this folder's
`data/mcchub/function/lbr4/` into the existing `mcchub` datapack (one
`pack.mcmeta`, two function trees). `load.mcfunction` here is self-contained and
is *not* called from LBR-1's load.

---

## 5. Verification

Checks the generator runs on every build:

| Check | Result |
|---|---|
| Blocks below y = 121 | **0** |
| Blocks inside the dome radius (r < 29 from C) | **0** |
| Blocks outside the briefed build area 4860..5140 | **0** |
| Ground blocks floating over LBR-1's lagoon | **0** |
| Portal-pad overlap between my lots | **0** (lots are 55 apart, 18 wide) |
| Commands per file | max 1,586 (limit ~8,000) |
| Block ids | all vanilla 1.21.1 (64 distinct) |

---

## 6. Gaps, risks and things for LBR-5

### 6.1 The briefed globe sits inside the south portal (needs a decision)

`BRIEF.md` puts the globe at `5000,132,5080` r = 12 and the portal ring at
r = 72 with 18×18 lots. Those two cannot both hold: the TGTTOS lot occupies
`x 4991..5008, z 5064..5081`, and the globe's front cap reaches z = 5068.

* **1,591 of my blocks** lie inside the briefed sphere
  (bbox `4991,121,5068 .. 5008,143,5082`).
* I built to the brief (ring r = 72) and did **not** move the portal.
* Cheapest fixes, in order:
  1. move the globe centre to `5000,132,5096` (r = 12) — 1 block clear of my
     cornice, and the globe then overhangs the island lip like the reference;
  2. or keep z = 5080 and use r = 9 with its centre raised to y = 138
     (front at z = 5071 ⇒ still 10 blocks inside my lot, so this only reduces
     the damage);
  3. or delete the rear 14 cells of this one lot (documented here so the cut is
     intentional rather than a surprise).

### 6.2 The briefed mountain overlaps the west portal

The mountain footprint "about 60×40" centred at `4900,5000` reads as
`x 4870..4930, z 4980..5020` if 60 is the x extent. The Build Mart lot starts at
x = 4919, so **3,229 of my blocks** fall inside that box
(bbox `4918,121,4985 .. 4930,148,5013`) — the portal's west wall and roof.

* Read the footprint as 40 (x) × 60 (z) instead, i.e. `x 4880..4920`, and the
  clash drops to the single column x = 4918..4920 at the mountain's toe.
* Recommendation: keep the mountain's east toe at **x ≤ 4916**. In the
  reference the massif is a north-south ridge, so a 40×60 reading is the
  natural one.

### 6.3 Sign frames overhang the 18×18 lot

The marquees are 25 (or 29 / 21) blocks wide so 6- and 7-letter names fit at
one block per pixel. They overhang the lot by up to 5.5 blocks on each side at
y ≥ 132. That is an airborne cantilever over LBR-1's pad — no ground footprint,
nothing to collide with (nearest neighbouring lot is 55 away), but it is worth
knowing before anyone "fixes" the footprint measurement.

### 6.4 Deliberate departures from a literal reading of the brief

* Diagonal portals use a cardinal facade (pinwheel, see §2) because block text
  aliases on a 45° plane.
* Names are split over two lines, and Hole in the Wall needs three lines
  (its board is 21 wide, 3 lines, height 30). TGTTOS gets a `CHICKEN` second
  line instead of the unwieldy full expansion.
* The plinth is a paved podium on top of LBR-1's pad rather than new terrain;
  the doorway sits one block above the pad, which reads as a threshold step.

### 6.5 Not done / not verified

* No in-game screenshot: nothing was run on the 25580 server, so lighting,
  collision and sign-height read are judged from the renders only.
* Interiors are furnished but never rendered — the detail previews are all
  exterior.
* The preview dumps include a **preview-only** stand-in for LBR-1's surface
  (plaza disc, causeways, lagoon, pads). It is not part of the emitted
  functions and must not be pasted.
* Bunting, lanterns and awnings use fixed block states (no stairs, doors,
  trapdoors or fence connections), so a WorldEdit/FAWE paste cannot rotate or
  break them; fences may re-connect to neighbours, which is cosmetic.
* The sleigh reads as a sleigh + two reindeer at 20+ blocks, but at 8 blocks it
  is still chunky — it is a silhouette prop, not a detailed model.
* Balloon envelopes are static; no animation or beacon beams.
* Snow cover on the roofs is a 1-layer rake along the eaves plus the cap, not a
  full blanket, so the game colours still read from the plaza.
