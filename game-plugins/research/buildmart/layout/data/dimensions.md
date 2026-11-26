# Dimensions — current Build Mart (resource-island map, MCC 20 → P25+)

Two units:

- **Measured** = a count visible in a still (blocks in a stack, number of chests, number of signs), error ±1.
- **Estimated** = derived from player-scale calibration (`frames.md`), error ±15 %.
- **Assumed** = needed to finish the plan but not observable; must be re-tested on the build.

Confidence: **H / M / L** as in the brief.

---

## 1. Overall footprint

| quantity | value | kind | conf | source / method |
|---|---|---|---|---|
| Map areas | 2 (build area, mart) joined by a portal | fact | H | S1 §Build Area; S4 3:25 |
| Mart plan shape | circular, radial, 8-fold | observed | H | S5 2:46; S4 3:04 |
| Mart diameter incl. dome | **~300 blocks** (r ≈ 148) | estimated | M | 00: dome ring is ~4.5 disc diameters |
| Dome height above gallery floor | **+55 to +65** at apex | estimated | L | 09 + 19; truss curvature |
| Gallery disc diameter | **~66 blocks** (r ≈ 33) | estimated | M | 01; ~4.4 plot widths |
| Lower pavilion ring radius | **~76 blocks** to deck centre | estimated | M | 00; bridges ~10–14 long |
| Lower pavilion deck | **~14 × 9**, top **+2**, roof **+7**, pad **+8** | estimated | M | 00, 11, 16 |
| Upper island ring radius | **~126 blocks** to island centre | estimated | M | 00; islands sit outside the pavilion ring |
| Upper island footprint | **~40–52 across**, **~18–26 thick** | estimated | M | 09; ~3 plot widths |
| Upper island top surface | **+26 to +40** above disc floor | estimated | M | 09, 19; sign height + eye line |
| Bridge disc→pavilion | **3 wide × 10–14 long**, no rail | estimated | M | 00, 14 |
| Void below | every platform; kill plane effectively y < island base | fact | H | S1; 00 |
| Skybox | open sky inside a glass/truss dome, bright daylight, no horizon terrain | observed | H | 09, 19 |

### Why these numbers

- The **disc** is the one object whose width can be cross-checked two ways: plinth count across it (≈9 plinth widths of 7 = 63) and plot-width comparison (4.4 × 15 = 66). Both land at **62–70**.
- The **pavilion ring** is set by the bridge length (10–14) plus the disc radius (33) plus half a pavilion (7): **~76**.
- The **upper ring** begins where the pavilions end (~83) and the islands are large (r ≈ 20–26), so centres sit at **~120–130**.
- **Heights** come from the `09` shot: standing on a pavilion deck (+2), the LOGS island lip is roughly 7–9 player heights above the camera → +15 to +18, and its top is another 8–10 blocks → **+25 to +30**. The +40 upper bound covers the higher rear islands.

---

## 2. Central island (Blueprint Gallery)

| quantity | value | kind | conf |
|---|---|---|---|
| Floor | blue concrete/terracotta with white "circuit" lane lines, `MCC` block lettering | observed | H |
| Plinths shown | **9 pads visible**; **7 selectable + 1 Gold** confirmed by wiki | observed + fact | M / H |
| Plinth base | **7 × 7**, 1 block high, grey stone/blackstone | estimated | M |
| Build footprint on plinth | **≤ 9 × 9**, typically 3–6 wide, 2–5 tall | estimated | M |
| Plinth gap (walking aisle) | **3 blocks** clear | estimated | M |
| Gallery layout | loose 3 × 3 lattice, not a perfect ring; entry aisle from the south | observed | M |
| Giant fan | **⌀ 20–24**, flush with the floor, wooden blades behind a metal grille | estimated | M |
| Fan position | on the disc, offset from the plinth lattice, adjacent to the south/east edge | observed | M |
| Fan column | vertical, carries a player ~+30 to +45 before elytra deploy | assumed | L |
| Speed pads on disc | **8** red/orange spirals, 2 × 2, flush, on the walking lines near the rim | observed | H |
| Flag posts | 6–8 white posts with team-colour flags along the south kerb | observed | M |
| Sign height | pavilion signs centred at **+10 to +14**, letters 3–4 blocks tall | estimated | M |
| Readable distance | sign legible from the opposite rim, **~60–70 blocks** | observed | M |
| Interaction distance | plate is stepped on / right-clicked at **≤ 2 blocks** | assumed | L |

---

## 3. Lower colour pavilions

| quantity | value | kind | conf |
|---|---|---|---|
| Count | **8** | estimated | M |
| Grouping | two colour families per pavilion (e.g. RED·ORANGE, YELLOW·LIME, PURPLE·PINK, WHITE·GREY, BLACK·BROWN) plus singles (GREEN, AQUA·CYAN, BLUE) | observed signs | M |
| Deck | **~14 × 9**, dark grey/black floor, +2 above disc | estimated | M |
| Back wall | full-height, dark charcoal with MCC banners | observed | H |
| Storage | glass display cabinets on steel frames (2 tiers), plus open block bins/piles | observed | H |
| Collision | cabinets are solid; the walking lane is ~4 wide between cabinets and edge | estimated | M |
| Hanging sign | one oversized word, on the *inward* face | observed | H |
| Roof | flat, with a **launch pad** (dark pad + coloured target ring) on the outer edge | observed | H |
| Decals | 2 × 2 spiral pads on the deck (magenta/coloured) | observed | M |
| Fall consequence | respawn at the build area, whole inventory except tools deleted | fact | H |

**Important:** the colour pavilions are *rooms*, not open piles. Anyone rebuilding from `00` alone will make floating shelves; `16` is the corrective frame.

---

## 4. Upper category islands

| island | sign | contents (from stills / block sheet) | conf |
|---|---|---|---|
| LUMBER | `LOGS` | log stacks by wood type, grass lip, small trees | H |
| SAND / SNOW | `SAND` | sand, sandstone, red sand; in MCC 19 replaced by snow/ice | H |
| STONE | `STONE` | stone, diorite, andesite, granite, cobble | H |
| ORES | `ORES` | redstone, diamond, emerald, gold, iron, coal, quartz blocks | H |
| NETHER | `NETHER` | quartz, warped/crimson wart blocks, nether bricks, blackstone | H |
| BRICKS | `BRICKS` | bricks, prismarine, stone bricks, deepslate tiles, nether brick | H |
| COPPER | `COPPER` | copper block, cut copper, all oxidation stages | H (MCC 20+) |
| GARDEN | `GARDEN` | flowers, leaves, moss, azalea, grass, vines | H (MCC 20+) |

| quantity | value | kind | conf |
|---|---|---|---|
| Count | **8** | estimated | M |
| Footprint | 40–52 across | estimated | M |
| Thickness | 18–26, grass/dirt lip + stone underside with stalactites | observed | H |
| Access | elytra from the fan; no stairs/bridges to upper islands | fact | H |
| Pile style | block stacks 1–3 high on show, walled by 1-block kerbs; decor animals on sand/logs | observed | M |
| Sign mounting | hung from the dome trusses above the island, not on the island | observed | H |

Older maps had Coral (pre-MCC 20) and no Copper; the current set is the table above.

---

## 5. Team build area

| quantity | value | kind | conf |
|---|---|---|---|
| Hall | **~62 × 44**, ceiling **+14** | estimated | M |
| Walls | white/quartz pilasters, red wall panels, purple banners, dark grey floor | observed | H |
| Regular plots | **3** in a row | fact | H |
| Plot module | **15 × 15** outer curb, 1 block high; **7 × 7** inset build zone; **1-block apron** between | estimated | M |
| Build zone marking | blue/grey checker border, dark grey felt floor | observed | H |
| Orientation cue | **black blocks at the four outer corners** of the curb | observed | H |
| Build height | up to ~5 above the plot; references are small objects | estimated | M |
| Reference | a copy of the selected build arrives in the team area, displayed in front of an empty plot | fact | H |
| Gold plot | **1**, raised **+2** on a yellow terracotta dais **~9 × 9**, in its own alcove | observed | M |
| Gold canopy | yellow/black hazard stripes, ~+6 above the dais | observed | H |
| Gold reference | none — must be viewed in the gallery | fact | H |
| Colour-coded chests | **5**, orange, on a counter, labelled with colour names | observed | H |
| Crafting | **2** crafting tables (+ recipe book) | fact/observed | H |
| Stonecutters | **1–2** | fact | M |
| Smelting | **2 banks** of ~8 furnaces each, lava-fed, fuel provided | fact + observed | H |
| Portal | freestanding 4-column emerald frame + blue bed + chains, in front of the plots | observed | H |
| Speed pads | green 2×2 diamond pads, flush, between plots and at the Gold dais | observed | H |
| Flight | allowed inside the hall | fact | H |
| Spawn / return | a pad in the hall; falls from the mart respawn here | fact | H |

---

## 6. Travel times (estimates, for pacing only)

| leg | distance | mode | time | conf |
|---|---|---|---|---|
| spawn → portal | ~15 b | walk | 2–3 s | L |
| portal → gallery disc | 0 (instant) + 5 b | walk | 2 s | L |
| disc → plinth | 5–25 b | walk | 2–5 s | M |
| disc → fan | ~20 b | walk | 3 s | M |
| fan → apex | +35 b | fan | 2–3 s | L |
| apex → upper island | 60–110 b | elytra glide | 5–8 s | L |
| pavilion → disc | 10–14 b bridge | walk | 3–4 s | M |
| island → portal | 60–110 b + hall | elytra + walk | 8–12 s | L |
| **full gather round trip** | — | — | **25–45 s** | L |
| 1★ build placement | — | — | 20–40 s | L |
| 5★ build placement | — | — | 2–4 min | L |

Time budget check: 12 min, 3 builds at a time, 90 s gallery rotation, Gold every 120 s from 4:00. A team that completes ~14–17 builds per event (S1 appearances table) is doing **~45 s per build**, which only works if a gather trip is ≤ 30 s. That is the single most important pacing constraint for our rebuild.

---

## 7. Block-count estimates for a rebuild budget

| element | count | note |
|---|---|---|
| Gallery disc floor | ~3,400 | ⌀ 66 circle, 1 layer |
| Plinth bases | 8 × 49 = 392 | 7×7, plus riser |
| Lower pavilions | 8 × ~350 = 2,800 | deck + wall + roof + cases |
| Bridges | 8 × ~40 = 320 | 3 × 13 |
| Upper islands | 8 × ~1,200 = 9,600 | thick chunk + piles |
| Dome frame | ~2,500 | trusses + glass |
| Build hall shell | ~3,000 | 62 × 44 × 14 |
| 4 plots + dais | ~1,200 | curbs, aprons |
| Utilities | ~250 | chests, furnaces, tables |
| **total, rough** | **~23,000 blocks** | ±30 %; decorative detail dominates |

This is a *big* map. For our server, the honest options are (a) build it at 0.6 scale, or (b) build the topology and simplify the decoration. See README §Design implications.
