# V4-D — gaps, assumptions and unseen angles

Rule followed: **if a face, dimension or prop is not visible in the supplied
references, it is not invented as fact.** It is either built from a documented
grammar that *is* visible, or it is listed here.

The supplied references are all *detail shots* or *wide aerials*. None is a
site plan, so **the position of each portal relative to the island exists
nowhere in the images** (see GAPS 2.1). Everything below is scoped to that.

---

## 1. Not visible in any reference — and what I did instead

### 1.1 Battle Box portal building — no photo at all
`MainHub-*.png` covers Ace Race, Dodgebolt, Parkour Warrior and Rocket Spleef
only. There is no Battle Box, no Parkour Tag and no arena exterior anywhere in
the pack. I therefore **reused the portal-building grammar that is visible in
`MCCHub-S4.png`** (white shell, coloured base band, cyan glass curtain wall
with white mullions, red crest panel with the gold MCC flame, gold crenellated
crown, white board on two posts, side flagpole). The footprint (40×30), the
2-line board and the interior portal panel are **my composition**, not a
reproduction.

### 1.2 Parkour Tag portal building — no photo at all
Same as 1.1, with a different accent (magenta base and crest strip, stepped
ziggurat roof cap, magenta pennant). This is a **family variant**, not a
verified building.

### 1.3 The backs and side elevations of every build
Every photographic reference is a front/three-quarter view. Rear walls,
side windows, roof plant and interiors are extrapolated from the front
grammar. Specifically unverified:
* Ace Race: the seaward side of the sand cay, and what holds the serpent up
  (the reference cuts off below the waterline).
* Dodgebolt: the arena's north end wall, the back of the seating, the
  exterior of the wooden podium.
* Parkour Warrior: everything behind the sign; the stone outcrop that appears
  behind the frog in `MainHub-PKW.png` is **V4-A terrain, not modelled here**.
* Rocket Spleef: the deck's far side and the gantry's seaward legs. The
  reference shows the gantry standing **over water**; my legs stand on the
  island at `y 121` (see 2.3).
* Aqua: the east (back) face. Reference resolution is ~40 px per block at
  best; the "AQUA JUNGLE" lettering of the Season-1 building is unreadable,
  so I used `AQUA` over `MCC` on the same two-board stack.

### 1.4 Text on the reference boards
The `PARKOUR WARRIOR` and `ROCKET SPLEEF` boards are legible and reproduced.
The `AQUA`/`MCC` boards are legible. The small boards on the S4 portal building
are **not** legible (`BATTLE`, `PARKOUR TAG` are inferred from the game the
plot serves). All lettering is a hand-made 5×7 (large) and 3×5 (small) block
font, one block per pixel — not the game's actual typeface.

### 1.5 Boat, balloon and stall placement
No reference gives positions, only density and style. Boats are at
`r 135..145`, balloons at `y 172..194`, stalls on the `r 60..80` ring. The
reference *does* show: gored balloon envelopes in 2–4 colours with wicker
baskets; striped market awnings; rowboats and one white/blue steamer; candy
canes; lantern posts; snowmen with top hats. Those forms are matched.

---

## 2. Decisions forced by constraints (not by the reference)

### 2.1 Dodgebolt is a podium, not a sunken arena
**The reference shows the court cut ~3 blocks below the surrounding deck.**
This agent may not write below `y = 121` (the island belongs to V4-A), so the
court floor sits at `y 121` and the **deck is raised to `y 124`** instead. From
the deck the sightline is identical; from outside, the arena reads 4 blocks
taller than the reference. If V4-A's island already dips here, V4-E can drop
the whole court by 3 in one pass (`01_teams_dodgebolt*` → shift y −3) without
any other change.

### 2.2 Portal positions are a ring, not a surveyed plan
The six portals + Aqua are distributed on an arc at `r 74..133` around `C4`,
all facades facing the plaza, all plots axis-aligned (a 5×7 pixel font cannot
survive a 45° plane — the same conclusion LBR-4 reached). The *set* and the
*relative sizes* come from the references; the *bearings* are my site plan.
Documented rectangles are in `STATUS.md` §3 so V4-E can move them without
re-deriving anything.

### 2.3 Rocket Spleef stands on land, not over water
`MainHub-RSR.png` has the gantry over open water with the launch bridge
crossing it. Because the water is V4-A's and its edge is unknown, the legs stop
at `y 121`. If V4-A's shoreline lands under the plot, V4-E can extend the legs
down to the water surface and the build will read correctly (the pillars are
hollow, 7×6, and can simply be lengthened below).

### 2.4 Water is always enclosed
Every water block is inside a walled basin (the Aqua pool, the channel, the
interior aquarium, which is a sealed glass box). No water is placed as a
free-floating surface, so nothing will flow when the chunk loads. The
Parkour Warrior lava pool has a deepslate lip on all four sides, so it cannot
run out of the mouth.

### 2.5 Known small overhangs (airborne, verified clear)
* Rocket Spleef marquee: 4 blocks past its plot at each end
  (`x 113..114, y 130..147, z -64..-16`). Nearest other structure is at `x 100`.
* Battle Box pennant: 2 blocks past the plot edge at `y 149..151`.

---

## 3. Angles and details that remain unverified

| # | Unseen | Risk |
|---|---|---|
| 3.1 | The serpent's tail **as it enters the sand** — the reference crops at the waterline | The tail taper and ground contact are my reading of the silhouette |
| 3.2 | The serpent's **left flank** (camera side is always the south-east) | Scale pattern and belly-plate width on that side are symmetric guesses |
| 3.3 | The frog's **rear/haunches from behind** — never photographed | Haunch size and rear feet are extrapolated |
| 3.4 | Dodgebolt's **court markings at the far end** — the reference is cropped | Ring count (3) and centre square are read from the near half |
| 3.5 | The Rocket Spleef **rockets' far side** and the deck's underside | Fin layout and deck truss are inferred from the south face |
| 3.6 | The Aqua building's **roof** and its relationship to the water ride | Roof plant, skylights and the balcony depth are invented |
| 3.7 | Every **interior** (Battle Box, Parkour Tag, Aqua hall) | Furnished plausibly; never rendered or photographed |
| 3.8 | **Snow on the reference portal buildings** | The portal photos are from the snow-less hub, the festive shots show no portals: I applied a light dusting only (roofs, canopies, awnings, tree crowns, apron edges) |

---

## 4. Integration risks for V4-E

1. **Island shape and water level.** I assume a walkable surface at `y 120`
   and water at `y 120`. If V4-A's surface is not flat at 120 inside my plots,
   the aprons (all at `y 121`) will float or be buried. The aprons are the only
   pieces that assume a flat 120.
2. **South band.** I keep `z > 76` clear for V4-C. If V4-C needs more depth
   than 74 blocks, two market tents (near `z 70`) and a handful of presents are
   the first things to remove — they are all in `10_market` / `13_presents`
   and can be dropped file-by-file.
3. **Balloons.** Four of the six hang above other agents' footprints (above
   `y 160`, nothing below `y 150`). If the dome's crown reaches higher than
   `y 160`, lower those balloons rather than moving them inward.
4. **Sign heights.** Parkour Warrior's board tops out at `y 168`, Rocket
   Spleef's rockets at `y 172`, balloons at `200`. The brief's ceiling is 210.
5. **Block-state strings.** 80 distinct ids, all vanilla 1.21.1. Stairs are
   avoided entirely (slabs, fences, panes and full blocks only) so a
   WorldEdit/FAWE paste cannot rotate or break them; fences and panes may
   re-connect to neighbours, which is cosmetic.
6. **No in-game test.** Nothing was pasted, so collision, light levels and the
  5×7 marquees at walking distance are judged from renders only.

---

## 5. What is deliberately NOT in this pack

* Any block at `y <= 120` (island, terraces, water, beach, red-carpet axis).
* The dome/plinth, mountain, Hall of Fame, rainbow bridge, snow globe.
* Team columns, player-head pedestals, stage plots, team labels.
* Sky dome, beacons, particles, holograms, NPCs or any entity.
* Any command other than `setblock` / `fill`.
