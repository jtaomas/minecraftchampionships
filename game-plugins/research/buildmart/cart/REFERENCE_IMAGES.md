# Build Mart — reference image pack (interaction handoff)

**Open these files directly.** This pack is the primary handoff for the cart / shopping-list / gallery / plot interaction builder. The prose in `README.md`, `data/cart.md` and `data/interactions.csv` explains the rules; the images show what the interaction should look like.

All paths are relative to `game-plugins/research/buildmart/cart/`.

## Source IDs

| ID | Source | Tier |
|---|---|---|
| S1 | MC Championship Wiki (wiki.gg), *Big Sales at Build Mart*, retrieved 2026-11-24 | 1 |
| S2 | MC Championship Wiki (Fandom), *Big Sales at Build Mart (Map)*, retrieved 2026-11-24 | 1 / Tier 3 for the old cart |
| S3 | Philza MCC P25 VOD, YouTube `jpMnwvRmHao`, Build Mart chapter 25:36–40:04; sampled 25:36–30:06 | 1 |
| S4 | Illumina MCC 13 VOD, YouTube `b6v_TuEUhkI`, Build Mart chapter 2:28:10–2:43:00; sampled 2:28:30–2:31:30 | 3 (historical cart) |
| S5 | Still images on the S1 page: `BSABM-BlueprintGallery.png`, `BSABM-BuildArea.png` | 1 (staged) |
| S6 | Current code: `BuildMartGame.java`, `BuildPreset.java`, `BuildMart/README.md` | current implementation |

> Times are absolute video times. The downloaded windows start at 25:36 (S3) and 2:28:30 (S4). Frame filenames keep the original local offsets.

---

## Core pack

### 01 — Blueprint Gallery, all seven pedestals
- **Path:** `frames/ref/01_blueprint_gallery_7_pedestals.png`
- **Source:** S5, `BSABM-BlueprintGallery.png` (current / P25-era map)
- **Timestamp:** n/a (still image)
- **Shows:** seven blueprint pedestals on a blue floor, each with a small 5×5-ish diorama, floating name / star / material text, and a small red-and-gold **select pad** at its foot. Team-coloured portal structures are visible at the edge.
- **Emulate:** seven physical gallery slots; one select pad per slot; floating name, stars and materials; selection is a world interaction, not a command.
- **Caveat:** staged wiki image. The exact activation (right-click pad vs step-on vs button) is not proven; treat that as M/L and choose a deliberate right-click in our build.

### 02 — Gallery select pads and floating labels (crop)
- **Path:** `frames/ref/02_blueprint_gallery_select_pads.png`
- **Source:** crop of 01
- **Timestamp:** n/a
- **Shows:** the red/gold floor pads in front of the graveyard, duck pond, house and sofa blueprints; the white grid lines; the floating `Materials:` labels.
- **Emulate:** a distinct, high-contrast select target that cannot be triggered by walking through the display; a short hover label with build name + stars + material count.
- **Caveat:** crop loses the full seven-slot composition; use 01 for layout.

### 03 — Shopping-list book, page 1 of 4: “Alternator”
- **Path:** `frames/ref/03_book_page1_alternator.png`
- **Source:** S3, 27:45 (local 129.0)
- **Timestamp:** 27:45
- **Shows:** vanilla written-book GUI; header `Page 1 of 4`; build name `Alternator`; `Materials:` heading; two lines, each with a 3D block icon, block name and count (`Lime Concrete 2x`, `Moss Block 8x`); back/forward page arrows; orange `Done` button. Hotbar slot 8 is the book.
- **Emulate:** book GUI, not chat; one build starts a page; icon + full name + count; page x of y; Done button; book in a fixed hotbar slot.
- **Caveat:** the book shows no star rating and no source marker. Those are our additions.

### 04 — Book page 2 of 4: “Booth”
- **Path:** `frames/ref/04_book_page2_boots.png`
- **Source:** S3, 27:45.2 (local 129.2)
- **Timestamp:** 27:45
- **Shows:** `Page 2 of 4`; `Booth`; nine material lines: Acacia Fence Gate 6x, Block of Quartz 11x, Blue Orchid 1x, Flower Pot 2x, Heavy Weighted Pre… 1x, Oak Fence 9x, Oak Log 4x, Orange Tulip 1x, Quartz Slab 4x.
- **Emulate:** dense lists need wrapping or a wider line; the real book truncates `Heavy Weighted Pressure Plate` to `Heavy Weighted Pre`, which is a readability bug we should not copy.
- **Caveat:** line count is tight; long names are clipped in MCC.

### 05 — Book page 3 of 4: continuation page
- **Path:** `frames/ref/05_book_page3_continuation.png`
- **Source:** S3, 27:45.4 (local 129.4)
- **Timestamp:** 27:45
- **Shows:** `Page 3 of 4` with **no build title**, only `Red Concrete 10x` and `Spruce Stairs 12x`.
- **Emulate:** the book paginates a continuous material list; a build’s list can overflow onto the next page. Page count is **not** equal to the number of builds. Our build should add a `(cont.)` label and repeat the build name in small text, because MCC’s blank continuation page is ambiguous.
- **Caveat:** the exact build that owns page 3 is inferred from adjacency (likely Booth); mark M.

### 06 — Book page 4 of 4: “Clouds”
- **Path:** `frames/ref/06_book_page4_clouds.png`
- **Source:** S3, 27:46 (local 130)
- **Timestamp:** 27:46
- **Shows:** `Page 4 of 4`; `Clouds`; `Block of Quartz 4x`, `Diorite 10x`; page arrows; Done.
- **Emulate:** a short build gets a short page; page 4 starts a new build after the continuation page; build order in the book follows selection order.
- **Caveat:** no source markers; no star rating on the page.

### 07 — Book item in hotbar slot 8
- **Path:** `frames/ref/07_book_hotbar_slot8.png`
- **Source:** crop of S3, 27:58 (local 142)
- **Timestamp:** 27:58
- **Shows:** hotbar with pickaxe, axe, shovel, shears, four empty slots, and a purple/brown **written book with enchant glint** in the last slot.
- **Emulate:** give every player a protected book in slot 8; it must survive the void and must not be consumed by crafting; reissue it on join/respawn if missing.
- **Caveat:** exact slot index is inferred from the HUD; the book is visibly last.

### 08 — Plot reference pedestal and floating material text
- **Path:** `frames/ref/08_plot_reference_pedestal.png`
- **Source:** S3, 27:34 (local 118)
- **Timestamp:** 27:34
- **Shows:** team area; a 3D reference build on a white/glass pedestal; floating `Materials: Block of Gold 1x, Cobblestone 10x, Cobblestone Slab 12x, … 5x`; a chest at the pedestal; purple team carpet; book in slot 8.
- **Emulate:** a physical reference copy on a pedestal behind/next to each plot; floating name + star + per-material list; a chest for the build’s reference kit; the plot itself is the empty area in front.
- **Caveat:** the floating text is large and can overlap; our layout should give it a fixed anchor and a max line count.

### 09 — Current team build area: three regular plots
- **Path:** `frames/ref/09_build_area_3_plots.png`
- **Source:** S3, 26:50 (local 74)
- **Timestamp:** 26:50
- **Shows:** three empty regular plots in a row, each with a light border and a reference/pedestal area behind; the `BUILD PLOTS` / `BUILD` sign; a smelter sign; the player standing on the centre plot.
- **Emulate:** three simultaneous plots, each independently selectable; a wide team area with a clear “build here” border; a central walkway; the portal/mart beyond.
- **Caveat:** the gold plot is not in this frame; use 10.

### 10 — Build area with the Gold Build plot
- **Path:** `frames/ref/10_build_area_gold_plot.png`
- **Source:** S5, `BSABM-BuildArea.png` (current / P25-era map)
- **Timestamp:** n/a (still image)
- **Shows:** regular plots with references; a raised orange/gold booth behind them with a crown and the text `GOLD BUILD — Copy this directly from the mart!`; the gold plot has no reference build.
- **Emulate:** a separate, visually distinct gold plot; no reference in the team area; a crown/banner and a “copy from the mart” instruction; the gold build is viewed in the gallery/mart.
- **Caveat:** staged image; the exact number of regular plots in the frame is ambiguous, but the gold booth is clear.

### 11 — Old-map cart: third-person ride
- **Path:** `frames/ref/11_cart_ride_thirdperson.png`
- **Source:** S4, 2:30:45 (local 135)
- **Timestamp:** 2:30:45
- **Shows:** a player seated in a **plain minecart** (no chest) on a rail in a white aisle of the old indoor mart; yellow/black hazard edging; wool/terracotta departments on both sides; first-person-style hotbar with blocks.
- **Emulate (historical):** the cart is a **rideable travel vehicle**, not a container. It follows rails; the player can exit; it does not carry the team’s blocks.
- **Caveat:** Tier 3 historical. This is the single best frame for “what the cart was.”

### 12 — Old-map cart: first-person ride
- **Path:** `frames/ref/12_cart_ride_firstperson.png`
- **Source:** S4, 2:30:42 (local 132)
- **Timestamp:** 2:30:42
- **Shows:** first-person view from a moving cart; the cart’s front lip at the bottom of the screen; red terracotta / yellow-black hazard blocks passing on both sides.
- **Emulate (historical):** camera locks to the cart; no special HUD; the player’s inventory remains the only storage.
- **Caveat:** the cart model is vanilla; a custom “shopping cart” entity is possible but not proven.

### 13 — Old-map cart: rail line inside the mart
- **Path:** `frames/ref/13_cart_rail_mart.png`
- **Source:** S4, 2:30:06 (local 96)
- **Timestamp:** 2:30:06
- **Shows:** a rail line running through the mart floor, a cart in the distance, a player running on foot beside it, department signs (`ORES`) and the old map’s white floor.
- **Emulate (historical):** carts share the mart floor with players on foot; rails are the fixed route; players choose cart (faster, less control) or feet (slower, more control).
- **Caveat:** the old map had pitfalls; the current map has void/fall risk instead.

### 14 — Current mart: resource islands and elytra traversal
- **Path:** `frames/ref/14_mart_islands_elytra.png`
- **Source:** S3, 27:52 (local 136)
- **Timestamp:** 27:52
- **Shows:** the current floating-island mart: `LOGS`, `SAND`, `STONE` islands; team-coloured portal pads below (`ORANGE`, `YELLOW`, `LIME`, `BLACK`); the player flying with elytra; no rails or carts.
- **Emulate (current):** travel is elytra + central fan + launch pads; do **not** reintroduce rideable carts as the main travel system.
- **Caveat:** this is the clearest “current equivalent” image: the cart’s travel role is gone.

### 15 — Selected build slots in the sidebar
- **Path:** `frames/ref/15_selected_builds_sidebar.png`
- **Source:** crop of S3, 27:58 (local 142)
- **Timestamp:** 27:58
- **Shows:** `BUILD SLOTS:` with `Alternator (x1 ★)`, `Booth (x5 ★)`, `Clouds (x2 ★)`.
- **Emulate:** the sidebar is the always-visible selection summary: three slots, build name, star count; empty slots read `Empty Plot #1/#2/#3`. The book is the detail view; the sidebar is the glance view.
- **Caveat:** `x1` here means one star, not a multiplier; use a clearer `1★` in our UI.

### 16 — Empty build slots before selection
- **Path:** `frames/ref/16_empty_plots_sidebar.png`
- **Source:** crop of S3, 26:46 (local 70)
- **Timestamp:** 26:46
- **Shows:** `BUILD SLOTS:` with `Empty Plot #1`, `Empty Plot #2`, `Empty Plot #3`; `GAME STARTING IN 00:23`; book already in slot 8.
- **Emulate:** all three slots exist from the start, visibly empty; the player can open the book before selecting and sees `Select builds in the Mart`.
- **Caveat:** the empty book page itself is in `frames/p25_sheet_1556_1656.jpg` (low-res contact sheet), not in the curated pack.

### 17 — Gold Build countdown HUD
- **Path:** `frames/ref/17_gold_countdown_hud.png`
- **Source:** crop of S3, 27:58 (local 142)
- **Timestamp:** 27:58
- **Shows:** `GOLDEN BUILD IN: 03:11` under the game timer `11:11`.
- **Emulate:** a persistent top-centre countdown to the next gold; at 11:11 remaining, 3:11 to gold = first gold at 4:00 elapsed. Use the same elapsed-time anchor, not “4 minutes remaining.”
- **Caveat:** this is the gold timer, not the 90-second gallery rotation. No clean gallery-rotation frame was captured; that rule is text-only (S1).

### 18 — Old mart departments (context)
- **Path:** `frames/ref/18_old_mart_departments.png`
- **Source:** S4, 2:29:29 (local 59)
- **Timestamp:** 2:29:29
- **Shows:** the old indoor mart’s wide white aisle, red carpet, `LUMBER`, `STONE`, `COLOURS`, `ORES` signs, and the pre-game view from the team side.
- **Emulate (historical):** the old mart was a dense indoor department store; the cart ran through these aisles. Do not copy the layout; copy only the “one cart, one route, departments” interaction idea if needed.
- **Caveat:** Tier 3 historical; the current map is islands, not aisles.

### 19 — Selection state, before and after
- **Path:** `frames/ref/19_selection_before_after.png`
- **Source:** composite of `16_empty_plots_sidebar.png` (S3 26:46) and `15_selected_builds_sidebar.png` (S3 27:58)
- **Timestamp:** 26:46 → 27:58
- **Shows:** left: `Empty Plot #1/#2/#3`; right: `Alternator (x1 ★)`, `Booth (x5 ★)`, `Clouds (x2 ★)`.
- **Emulate:** the sidebar is the before/after proof of selection: three persistent slots, empty labels, then build names + star counts. Do not replace it with a chat message.
- **Caveat:** the two crops come from different moments; team/score lines differ. Use it for the slot state, not for a continuous shot.

### 20 — Book empty state: “Select builds in the mart!”
- **Path:** `frames/ref/20_book_empty_state.png`
- **Source:** S3, 26:53 (local 77)
- **Timestamp:** 26:53
- **Shows:** `Page 1 of 1`; `Select builds in the mart!`; sidebar `Empty Plot #1/#2/#3`; book in slot 8; Done button.
- **Emulate:** the pre-selection book is a single prompt page, not an empty list; the three slots are visible but empty. This is the “before” state of the selection loop.
- **Caveat:** it appears during the pre-game countdown, so the player is in the team area rather than the gallery.

---

## What is missing / weak in this pack

| Interaction | Image status | Notes |
|---|---|---|
| Exact gallery click | **Missing** | The select pad is visible, but the activation is not proven. Build a deliberate right-click; do not infer a step-on trigger. |
| 90 s gallery rotation | **Missing** | Text-only (S1). Capture later if a clean gallery VOD window is allowed. |
| Gold build viewing from the mart | **Missing** | S1 says no team-area reference and view from the mart; no clean frame. Build a 5 s “look” interaction at the gold pedestal. |
| Book update animation | **Missing** | Pages 1–4 prove the layout and pagination; the update moment is not captured. Reissue the book on selection/completion. |
| Cart storage | **Not applicable** | The old cart has no chest; no storage interaction existed. Our supply cart is original. |
| Plot completion / wrong blocks | **Missing** | Current code and wiki text define it; no clean P25 completion frame in the sampled window. |

## How to use the pack

1. Build the gallery from **01 + 02**: seven slots, one select pad each, floating name/stars/material count.
2. Build the team area from **09 + 10**: three regular plots, one gold plot, references behind the plots, gold booth.
3. Build the book from **03–06**: page x of y, build title only when a new build starts, icon + name + count, continuation pages, Done.
4. Build the plot reference from **08**: physical copy on a pedestal + floating material text + chest.
5. Build the sidebar from **15 + 16**: three slots, empty vs selected, star count.
6. Build the gold timer from **17**: elapsed-time anchor, top-centre countdown.
7. Do **not** build rideable carts as travel. Use **11–13** only as historical context; build the original **supply cart** described in `data/cart.md`.
8. Use **14** to keep the elytra/island travel intact.
