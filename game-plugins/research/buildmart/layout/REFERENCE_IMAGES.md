# Reference image pack — Build Mart current map

**This is the primary handoff.** The builder should open these files, not re-watch anything.
All stills are full-resolution (1920×1080) unless marked *wiki image*. Nothing here is a
third-party map download; every image is a frame from official/Noxcrew footage or a wiki PNG.

Source codes used below:

| code | source |
|---|---|
| **S4** | Noxcrew, *MC Championship Pride 25 — Update Video: Big Build Mart Update!* — YouTube `zSvKYe9pl0M`, 6:05, uploaded 2025-06-27. Timestamps are video time. |
| **S5** | Wolfeei, *MCC Pride 2025 Updates & Games — BUILD MART UPDATE!* — YouTube `bjMl2fLvMok`, 20:21. Contains the same 6:05 showcase plus a wide establishing shot. Timestamps are video time. |
| **S6** | MC Championship Wiki image files (official renders/screenshots), retrieved 2026-11-24. |

---

## 1. Map overview / top-down

### `frames/pack/00_map_central_island_aerial.jpg` — **the single most important still**
- **Source:** S5 @ 2:46 (wide aerial over the central island)
- **Shows:** the whole mart in one frame — the blue gallery disc with 7 plinth builds and the `MCC` lettering, the ring of 8 dark colour pavilions with coloured awnings and roof pads, white bridges from disc to pavilions, the giant fan disc at the disc edge, two upper islands (GARDEN top-right, COPPER far left) floating higher, and the yellow dome ring behind everything.
- **Copy structurally:** concentric organisation (disc → 8 bridges → 8 pavilions → 8 higher islands), the 1:1 radial pairing, the fact that *nothing* has a guard rail, and that the disc is the navigational origin you can see from every pavilion.
- **Do not copy:** the exact 8-fold symmetry or the specific colour-to-bearing assignment.

### `frames/pack/01_map_gallery_aerial.jpg` — approaching the disc
- **Source:** S4 @ 3:04
- **Shows:** the disc from above with all plinths readable as a loose 3×3 lattice; `ORANGE`, `YELLOW`, `LIME`, `BLACK` pavilion signs on the ring; launch pads visible as dark squares with coloured rings; the fan disc at bottom.
- **Copy structurally:** plinth spacing (large enough to walk a 4-player team between two plinths), the way signs face *inward* toward the disc, and the two-step approach (bridge → pavilion deck → bins).

### `frames/pack/19_official_revised_map.png` — *wiki image*
- **Source:** S6, `Build-Mart-Revised-Blueprint.png` (current map render)
- **Shows:** eye-level view from the disc: plinths with builds, colour pavilions behind them with big block-letter signs, MCC banners, glass cabinets, upper islands overhead, dome trusses.
- **Copy structurally:** the visual hierarchy — build first, sign second, island third. The player standing on the disc should always know which pavilion is which without opening a map.

---

## 2. Blueprint Gallery

### `frames/pack/18_official_blueprint_gallery.png` — *wiki image*
- **Source:** S6, `BSABM-BlueprintGallery.png`
- **Shows:** the 7+ builds on individual plinths arranged on the blue circuit floor; a grey stone plinth; green/white/black colour pavilions beyond; the giant fan partially visible bottom-right.
- **Copy structurally:** plinth = 7×7 base + 1-block riser, build sits on top, name/stars/materials float above head height, and the 3-block walking gap between plinths.

### `frames/pack/02_gallery_plinth_and_materials.jpg` — plinth + read-out
- **Source:** S4 @ 3:12
- **Shows:** a completed plinth (`Cover`: Blue Concrete 4×, Red Concrete 12×) with the floating material list; `ORANGE`, `RED`, `LIME` signs behind; the circuit-line floor; a red spiral speed pad.
- **Copy structurally:** the material read-out is the *only* text on the plinth, pinned at eye level, and it is legible while walking past at speed.

### `frames/pack/05_gallery_yellow_plinth.jpg` — player at a plinth
- **Source:** S4 @ 4:00
- **Shows:** scale reference — a player standing beside a plinth; the build is roughly 2× the player's width and 1.5× their height. Confirms the builds are *small objects*, not dioramas.
- **Copy structurally:** plinth height ≈ 1 block; build height 3–5 blocks; the player's head is above the build, so the floating label must sit at +3 or higher.

### `frames/pack/11_gallery_white_and_launch_pads.jpg` — ring edge + pads
- **Source:** S4 @ 4:54
- **Shows:** `WHITE` pavilion, two plinths, the disc edge, and the roof launch pads (dark pad, coloured target ring) on the pavilions.
- **Copy structurally:** the pad sits on the *outer* edge of each pavilion roof, aimed back at the disc; it is the return trip, not the outbound one.

### `frames/pack/13_gallery_overview_colour_ring.jpg` — gallery panorama
- **Source:** S4 @ 5:06
- **Shows:** the ring of colour pavilions as a continuous wall of signs (`RED`, `ORANGE`, `YELLOW`, `LIME`, `BLACK`, `WHITE`), plinths in front, `MCC` floor lettering.
- **Copy structurally:** sign density — one oversized word per pavilion, readable from 40+ blocks; colour-coded background behind the letters.

### `frames/pack/14_gallery_from_ring.jpg` — from a pavilion looking back
- **Source:** S5 @ 2:50
- **Shows:** the disc seen from a colour pavilion: flags, plinths, the white bridge, and the fan. This is the view a returning player has.
- **Copy structurally:** the return sightline must put the disc centre and the player's own team colour in view; the fan should be visible from every bridge mouth.

### `frames/pack/15_mcc_lettering_speed_pads.jpg` — floor detail (crop)
- **Source:** S5 @ 2:50 (2× crop of `14`)
- **Shows:** `MCC` in white block letters on the blue floor, two red/orange spiral speed pads flush with the floor, white flag posts and a low kerb.
- **Copy structurally:** speed pads are *flush* (no trip hazard), 2×2, and placed on the walking line, not in the middle of a plinth aisle.

---

## 3. Team build area

### `frames/pack/04_build_area_overview.jpg` — **best build-area still**
- **Source:** S4 @ 3:36
- **Shows:** 3 regular plots on the left/centre, each a dark floor inside a white curb with black corner blocks; the green/blue team portal (4 emerald columns + chains around a blue portal bed) at centre; the 5 orange colour-coded chests on a counter, labelled (ROYAL BLUE, LIME, …); a purple wall banner; the yellow-backed furnace bank at right; floor text `Select a blueprint in the mart!`.
- **Copy structurally:** plot proportions (curb ≈ 15×15, build zone ≈ 7×7 inset), the black corner blocks as the orientation cue, and the fact that utilities sit on the *wall behind* the plots, not between them.

### `frames/pack/03_build_area_portal.jpg` — portal + plot in use
- **Source:** S4 @ 3:25
- **Shows:** the portal from the side, a plot with a finished grass/path build, a second plot empty (dark floor, blue border), the chest counter and furnace bank in the background, and the tooltip `TWO ★★ Materials: Black Concrete 9×, Blue Concrete 12×, Chain 18×, Oxidized Copper 4×`.
- **Copy structurally:** the portal is a *freestanding prop* in the middle of the hall, not a wall doorway; you can see plots through it. Empty plot = dark floor; filled plot = the build owns the 7×7 core.

### `frames/pack/10_build_area_plots_row.jpg` — two plots side by side
- **Source:** S4 @ 4:36
- **Shows:** the true plot module: outer white curb, grey apron, blue-checker border, central build area; player scale; green diamond speed pads between plots; the gold dais behind.
- **Copy structurally:** the apron. Builds are allowed to be messy at the edge because the apron absorbs overspill; the checker border is the actual build boundary.

### `frames/pack/06_build_area_gold_plot_wide.jpg` — **Gold plot**
- **Source:** S4 @ 4:12
- **Shows:** the Gold plot as a raised yellow dais with a dark inset, sitting in its own alcove under a yellow/black hazard-striped canopy; `Gold Build — Copy this directly from the mart!` hologram; two green spiral speed pads on the floor in front; red-block wall.
- **Copy structurally:** Gold gets a *different room treatment* (hazard stripes, yellow terracotta, raised +2) so it cannot be confused with a normal plot; the pads in front reward the dash from the portal.

### `frames/pack/07_build_area_gold_plot.jpg` — Gold plot close
- **Source:** S4 @ 4:16
- **Shows:** the gold dais surface and the build pedestal, with the hazard canopy overhead.
- **Copy structurally:** the dais is large enough for a 7★ build (≈9×9) plus one player on each side.

### `frames/pack/08_build_area_slots_hologram.jpg` — slot-state hologram + Gold clock
- **Source:** S4 @ 4:26
- **Shows:** a floating hologram at the plot edge: `MCC [1/8]: BUILD MART / GAME COINS (×1.0): 0 / BUILD SLOTS: → Empty Plot #1, → Empty Plot #2, → Empty Plot #3 / LIVE SCORES: MCC.LIVE`. Above the hall: `10:02` timer and `GOLDEN BUILD IN: 02:02`. At right, the ORANGE pavilion sign and the giant fan; foreground, the plot's white curb and a red spiral pad.
- **Copy structurally:** two clocks the player can read without a HUD — game time and **time-to-Gold** — plus slot state anchored to the plot, not the sidebar. This single frame is the best evidence for the whole information design.

### `frames/pack/17_official_build_area.png` — *wiki image*
- **Source:** S6, `BSABM-BuildArea.png`
- **Shows:** the hall from above: gold dais with crown, three plots, portal, chests, furnace bank, red wall panels.
- **Copy structurally:** hall proportions and the one-axis layout (plots in a row, utilities on the far wall).

---

## 4. Resource islands

### `frames/pack/09_upper_islands_logs_sand.jpg` — **upper islands + dome**
- **Source:** S4 @ 4:30
- **Shows:** `LOGS` and `SAND` signs hanging from the yellow dome trusses; the logs island as a walled platform of log stacks with a grass lip; the sand island as a desert mound with llamas; `STONE` at right; blue tinted glass roof panels and red ring ornaments.
- **Copy structurally:** upper islands are *thick* (grass/dirt lip, stone underside with stalactites), 15–25 blocks above the walkway, with their sign hanging from the dome rather than mounted on the island — the sign is readable from the disc.

### `frames/pack/16_colour_pavilion_glass_cases.jpg` — colour pavilion interior
- **Source:** S5 @ 2:52
- **Shows:** a lower colour pavilion from inside: glass display cabinets on steel frames, shelves of pink glass/concrete, black wall, MCC banners, blue floor, magenta spiral decals, roof structure.
- **Copy structurally:** the colour islands are *walk-in showrooms*, not piles in the open. Glass cases double as both storage and legibility (you see the colour through glass), and the dark wall makes the colour pop.

### `frames/pack/20_official_block_categories.png` — *wiki image*
- **Source:** S6, `BuildMartMap.png` (available blocks since MCC 20)
- **Shows:** the canonical category list: **Colours (glass + concrete)**, **Garden**, **Ores**, **Stone**, **Copper**, **Lumber**, **Bricks**, **Sand**, **Nether**.
- **Copy structurally:** this is the *minimum* block set a rebuild must expose. Note Snow replaces Sand in MCC 19, Copper replaces Coral from MCC 20.

---

## 5. Fan / launch pads / portal

- **Fan:** `00_map_central_island_aerial.jpg` (disc edge, large grey disc with brown wooden blades), `01_map_gallery_aerial.jpg`, `19_official_revised_map.png`. It is a floor-mounted fan, flush with the gallery disc, roughly 20–24 blocks across; the vertical air column is the outbound launch.
- **Launch pads (roofs):** `11_gallery_white_and_launch_pads.jpg`, `01_map_gallery_aerial.jpg` — dark pad + coloured target ring on each pavilion roof, on the outer edge.
- **Speed pads (deck):** `15_mcc_lettering_speed_pads.jpg` (red spiral, flush), `06_build_area_gold_plot_wide.jpg` / `10_build_area_plots_row.jpg` (green diamond, flush).
- **Portal:** `03_build_area_portal.jpg`, `04_build_area_overview.jpg` (green frame, blue bed, chains), `17_official_build_area.png`.

---

## What is *not* in the pack (deliberate gaps)

| missing | why | impact on build |
|---|---|---|
| true orthographic top-down | no official ortho render found; the best still is an oblique aerial (`00`) | use `schematics/current_topdown.png` as the measured abstraction; do not trace the still |
| spectator stand | never in frame in the 10 min of footage used | design one from first principles (see README open questions) |
| exact island bearings | the aerial is oblique and partially occluded | ring order is a *navigation aid*, not a fact; rebuild may re-order |
| block-level counts per island | would need a world download (not allowed) | estimate from stacks in `09` and set our own quantities |
