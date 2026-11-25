# Build Mart — Rebuild Plan

**Status:** plan only. Nothing has been built, edited, pasted or started. Per the repo's `AGENTS.md`, no
work starts until the owner approves this plan and answers the open questions (section 9).

**Target:** current MCC "Big Sales at Build Mart": the floating-island map from MCC 14 onward, the **P25
"Blueprint Update"** loop (gallery, 3 plots, Gold Build, shopping-list book) and the **MCC Fall Stars
scoring** (flat 20 coins per star). The old indoor mall with shopping trolleys (MCC 3–13) is an optional
second map (Phase 7, Q1).

**How to read this:** sections 1–2 cover what the real game is and where the research packs disagree.
Section 3 covers what we have now. Sections 4–7 describe what we will build. Section 8 is the build order
and the tests. Sections 9–10 are your decisions and the risks.

Research packs used (all paths in this document are relative to `game-plugins/research/`):

| Short name | Folder | What it covers |
|---|---|---|
| **Pack A** (earlier, broad) | `buildmart/` (`rules/`, `layout/`, `look/`, `resources/`, `cart/`, `builds/`, `audit/`) | rules, map layout, block lists, cart history, build format, code audit |
| **BM-1** | `mcc_archive/build_mart/bm-1/` | the physical world: arena, gallery, team rooms, islands, fan, old mall |
| **BM-2** | `mcc_archive/build_mart/bm-2/` | builds and UI: 114 build renders, holograms, book, HUD, results |

---

## 1. What real Build Mart is

Confidence: **H** means two or more independent sources, or plainly visible in footage. **M** means one
source or a strong inference. **L** means weak or unverified.

### 1.1 Format and round flow

| # | Fact | Conf. | Source |
|---|---|---|---|
| F1 | Game lasts **12:00**. Teams of **4**, up to **10 teams**. | H | `buildmart/rules/README.md` F1; `bm-2/frames/ui/09_game_results_overlay.jpg` |
| F2 | Pre-game: everyone stands in their **own team build room** with **empty plots** and no build preview, under a "GAME STARTING IN: 00:xx" countdown. | H | rules F2; `buildmart/rules/frames/02_pregame_empty_plots_hud.png` |
| F3 | Each team room has **3 regular plots + 1 raised Gold plot**, a portal to the mart, **5 colour-coded chests**, crafting tables, stonecutters and **2 fuelled smelter areas**. Flying is allowed in the room. | H | rules F3–F4; `bm-1/images/wiki_build_area.png`; `buildmart/layout/frames/pack/04_build_area_overview.jpg` (chests labelled R O Y L B) |
| F4 | Loop: room → portal → mart (gallery in the centre) → pick builds → fly to islands to gather → portal back → build → repeat. | H | `buildmart/layout/README.md` 3.5 |
| F5 | **Blueprint Gallery**: 7 blueprint pads, **2×1★, 2×2★, 1×3★, 1×4★, 1×5★**, plus 1 Gold pad. | H (count) / M (exact mix) | rules F5; `bm-2/notes.md` §2 |
| F6 | The gallery **restocks every 90 s** (8 restocks in 12 min). Chat: "Blueprints plots now have new builds!" | H | rules F6; `buildmart/rules/frames/09_build_completed_hologram.png` |
| F7 | A pad whose build is on a team's plot **does not restock until that build is completed**. This is what lets a team repeat a build. | M | rules F7/F12; `buildmart/builds/README.md` F7 |
| F8 | Blueprints are **not exclusive**: several teams can take the same blueprint at once. | H | rules F10 (three teams completed "Whale") |
| F9 | Selecting: press the **red button on a gold post** at the front of the pad. The build goes to your first free plot. You can hold at most 3 builds. Finishing one frees its slot, and nothing advances automatically. | H (button, 3 max) / M (exact input) | rules F8–F11; `bm-2/frames/gallery/09_selection_cart_detail.png` |
| F10 | Selecting delivers: a **reference copy** on the back half of the plot pair, the **name/stars/material hologram** on the build plot, and **pages in the shopping-list book**. | H | rules F13; `bm-1/images/wiki_build_area.png` |
| F11 | **Gold Build**: the first appears at **4:00**, then a new one every **2:00** (4:00, 6:00, 8:00, 10:00). It is worth **7★** and is taken from the gold pad in the gallery. The team room has **no reference**, so players build it from memory. If it is unfinished when the next gold arrives, its blocks are removed. Past golds cannot be finished later. | H (timing, 7★, no ref) / M (clear-on-fail) | rules F17–F20; `bm-1/frames/004400_cur_gallery_gold_tendril_plot.jpg` |
| F12 | The gold window runs from when the gold **becomes available**, not from when your team selects it. The "GOLDEN BUILD IN" bar resets to the next gold as soon as the current one appears. | M | rules F18 + open Q2; `buildmart/rules/frames/06_gold_selected_broadcast.png` |
| F13 | A build is complete only on an **exact match**: right block, right place, **right rotation** (stairs, slabs, doors). "Texture rotation" is ignored and a double slab counts as one full block. Any extra block in the plot blocks completion. | H | rules F22–F23; builds F24 |

### 1.2 Mart, travel and kit

| # | Fact | Conf. | Source |
|---|---|---|---|
| F14 | The mart is a **shared radial arena under a glass dome**. In the centre is a blue "blueprint" disc holding the gallery. Around it is a ring of **colour pavilions**, and above that a ring of **8 category islands**. The void is below. | H | `buildmart/layout/frames/pack/00_map_central_island_aerial.jpg`; `bm-1/images/wiki_current_map_blueprint_topdown.png` |
| F15 | Upper islands: **ORES, NETHER, BRICKS, LOGS, SAND, STONE, COPPER, GARDEN**, each with an exact block list (see 4.5). | H | `buildmart/resources/README.md` F2 (official block poster) |
| F16 | Colour pavilions stock **concrete, concrete powder and stained glass** for each colour. There is **no wool** anywhere. | H (forms, no wool) / M (all 16 colours) | resources F3 |
| F17 | Quartz block is on both ORES and NETHER, and nether bricks are on both NETHER and BRICKS. | H | `buildmart/resources/data/islands.md` §4 |
| F18 | Mining a resource block puts it **straight into your inventory**, and the block grows back after a few seconds. | H (to inventory) / M (regrow time unmeasured, ~2–3 s) | resources F5 |
| F19 | Travel uses an **elytra equipped from the start**, a **central fan** (updraft), big **launch pads** and small 2×2 **speed pads**. You cannot fly in the mart. | H (elytra) / M (fan, pads) | rules F31; layout F11–F12 |
| F20 | **Falling off** respawns you in your team room and **destroys every carried material**. Tools and the book are kept. | H | rules F33; layout F28 |
| F21 | Kit: diamond pickaxe, axe and shovel, a 4th tool (looks like a **hoe** in Fall Stars footage; Pack A says shears), **elytra**, **Haste II**, and the **shopping-list book** in hotbar slot 9. | H (set) / M (4th tool, slot) | rules F30; `buildmart/rules/frames/13_inventory_kit_elytra_book.png`; `bm-1/frames/004400_*` |
| F22 | The block under your crosshair is named in **green above the hotbar**, with the held item underneath. This is the colourblind aid. | H | rules F34; `buildmart/rules/frames/02_*` ("Andesite Slab") |

### 1.3 Scoring (current, MCC Fall Stars onward)

| # | Fact | Conf. | Source |
|---|---|---|---|
| F23 | **20 coins per star, flat**: 1★ = 20 … 5★ = 100, Gold 7★ = 140. Paid to the team when the build completes. (P25 used 10/15/20 by time band; replaced.) | H | rules F25; official rules page |
| F24 | At the end, each **unfinished** build pays **% complete × its star value**. Since MCC BG, the gold is **excluded** from this. | H (partial) / M (gold excluded) | rules F26–F27 |
| F25 | **Employee of the Month**: Entrepreneur (most stars), Line Cook (most builds), Quality Assurance (highest average stars). The top 3 teams in each get **100 / 50 / 25**. | H | rules F28 |
| F26 | There is **no** speed bonus, finish-order bonus, most-builds bonus, Big Sales, shop counters or fixed build order. | H | rules F38 |
| F27 | Chat shows each player's share: "[+10] Your team completed Coin (★★)". That is 40 team coins split across 4 players. | M (inference) | `buildmart/rules/frames/09_build_completed_hologram.png` |

### 1.4 The "shopping cart": what it really is

There are two different things, and the research packs mix them up:

1. **Current MCC has no cart.** The P25 gallery has a **red button on a gold post** at each pad. BM-2
   calls it the "selection cart". It is a button: the hologram text next to it says "…button"
   (`bm-2/frames/gallery/09_selection_cart_detail.png`). **H**
2. **The old mall map (MCC 3–13) had shopping trolleys.** A trolley was a **rideable vehicle that looked
   like a boat**, with a grey basket hull. It came from a trolley item in the hotbar and was ridden on the
   flat mall floor with **no rails**. It was faster but harder to steer, and players could switch to
   running at any time. It carried the player, not blocks. **H** visual. Evidence:
   `buildmart/cart/frames/ref/11_cart_ride_thirdperson.png` and `13_cart_rail_mart.png` (the sound
   subtitle reads "Rowing", which is the boat sound), `bm-1/frames/zz_detail_orig_shopping_carts_zoom.jpg`,
   and `buildmart/audit/frames/official_cart_item_hotbar.png` (trolley icon in the hotbar). Pitfalls in
   the mall floor cost you your carried blocks. **H**

No version of MCC had a cart that **stores blocks** or a cart-style shopping UI. The shopping list is the
book.

---

## 2. Where the research packs disagree, and what I trust

| Topic | One side says | Other side says | I trust | Why |
|---|---|---|---|---|
| Old trolley | Pack A `cart/` + `audit/`: plain **minecart on rails** | BM-1: boat-like trolley, **no rails**, ridden anywhere | **BM-1** | Frames show a basket hull on flat snow/quartz floor, the subtitle "Rowing" (a boat sound) and no rails anywhere |
| Gallery "cart" | BM-2: "selection cart" (marked as inferred) | Pack A rules: red **button** on a gold pedestal | **Button** | The hologram beside it says "button"; the P25 notes describe a button press |
| Shopping book | BM-2 and rules: "custom GUI, not a vanilla book", one page per build | Pack A `cart/`: **vanilla written book**, pages depend on list length | **`cart/`** | Frames show vanilla page arrows, a vanilla "Page 3 of 4" header and a title-less continuation page (`cart/frames/ref/05_book_page3_continuation.png`). It is a written book **reskinned by the resource pack** (notebook paper, orange Done button) |
| Team portal | Pack A `layout/` F24: a freestanding "4 emerald columns + chains + blue bed" prop | BM-1 and rules: a **purple portal in a quartz frame** under a "MART" sign | **Purple portal** | The "emerald prop" is the **TKO build** (its hologram lists Black/Blue Concrete, Chain, Oxidized Copper): `bm-1/images/wiki_build_area.png` |
| Gold booth | BM-2: the gold booth in the gallery has a gold counter, a crown and "Copy this directly from the mart!" | BM-1: the gallery gold is a **floor pad** with "Tendril / Golden Build / ★×7" | **BM-1** | BM-2's frames `gallery/06`–`07` show the **team-room** gold dais, not the gallery. The real gallery gold is in `bm-1/frames/004400_*` |
| Gold booth (rules) | `rules/frames/05_gallery_gold_booth.png` is captioned "gallery" | — | It is the **team room** | GOLD BUILD sign over the yellow dais, smelter walls |
| Plot size | `layout/`: 15×15 curb, 7×7 inset; `cart/`: 5×5 | `builds/`: **7×7 build surface + 1-block rim = 9×9** | **7×7** | Counted tiles on official renders (`buildmart/builds/frames/14_build_base_7x7_piston_native.png`); also matches the room images |
| Colour islands | `layout/`: 8 pavilions | `resources/`: 16 colour bays; BM-1: one per colour | **8 pavilions × 2 colour towers = 16 colours** | The aerial shows twin rack towers per pavilion (`layout/frames/pack/00_*`) |
| Fan | `layout/`: ⌀20–24, flush with the floor | BM-1: a "tiered quartz ring with glass top" | **A sunken grey bowl with blades at the disc edge, about one pad wide (⌀~13)** | `bm-1/images/wiki_blueprint_gallery.png`. BM-1's quartz/red "ring" is the **portal pavilion**: its purple door is visible in `bm-1/images/wiki_current_map_blueprint_topdown.png` |
| Pavilion on the deck | BM-1: unknown (portal? gold display?) | — | **Portal arrival house** | Purple portal door, red carpet, white columns in the top-down render |
| Star value | `audit/`: 10/15/20 by time | `rules/`: **flat 20** | **Flat 20** | Newest patch (MCC FS) plus the official rules page |
| Gallery per team or shared | `rules/` [ADAPT]: each team has its own gallery state | `cart/`, BM-1, BM-2: **one shared physical gallery** | **Shared** | There is one deck in the aerial footage, and teams visibly share blueprints |
| Gallery mix | Wiki: 2/2/1/1/1 | BM-2: one frame shows two 4★ pads at once | **2/2/1/1/1** (configurable) | Several sources; one frame may catch a restock in progress |
| Book position | `rules/`: offhand | `cart/`, Fall Stars frames: **hotbar slot 9** | **Slot 9** | Seen in several frames |
| "114 builds" | BM-2: 114 builds with renders | `builds/`: those 114 are the **legacy (pre-P25) set with no stars**; P25+ uses ~60 builds per event **with** stars, mostly without renders | **Both are right** | We have pictures of the 114 legacy builds only, so we recreate those and assign stars ourselves (Q4) |

---

## 3. Gap analysis: current plugin vs real Build Mart

The current plugin works, but it is a different game: a ring of team platforms around a 27-stall
market, one 5×5 plot, a fixed build order, chat shopping lists, invented shop counters and timed "sales".

| File | What it does now | Verdict | Details |
|---|---|---|---|
| `pom.xml` | Paper 1.21.1, core 3.1.0 | **Keep** | Add JUnit (test scope) for the build-catalogue and matcher tests. Bump the core dependency if Q12's core change is approved |
| `src/main/resources/plugin.yml` | No commands | **Change** | Add `/bm` admin command + `mcm.buildmart.admin` permission (TGTTOS pattern) |
| `README.md` | Rules of the old game | **Replace** at the end | Write it in the Dodgebolt README style: MCC rules, `[default]` list for non-researched values, resource-pack section |
| `BuildMartPlugin.java` | Deletes the world folder on every boot, registers the game | **Change** | Stop deleting the world. Load the saved arena world. Load config and catalogue. Register `/bm`. Keep the `registerGame` and `onDisable → stop()` pattern |
| `VoidGenerator.java` | Empty chunks | **Keep** (or use `McmBuild.voidWorld`) | — |
| `BuildPreset.java` | 25 hard-coded 5×5×5 pictures, Material only, stars from block count | **Replace** with a JSON build catalogue | Keep the ideas: layered text format, `at()`, `unsupported()` (floating-block lint). Delete all 25 presets: none are MCC builds, 47% of their blocks are wool (not stocked in MCC) and they cannot hold stairs or slabs |
| `BuildMartGame.java` (1,042 lines) | Everything | **Replace** (split into ~25 classes, section 7) | **Keep:** the lifecycle skeleton (start calls stop, idempotent stop, bonuses before `finish`, cleanup after; lines 196–280), the protection listeners (957–995), mine → inventory with no drops (835–872), the regrow queue concept (634–648), fall = lose carried blocks (650–667), the side/team mapping, and the text/title helpers. **Delete:** the ring layout (208–229, 400–441), the 27-stall grid + 4 shop counters (326–393, 890–940), Big Sales (704–741), the shared build sequence (454–476), the chat shopping list (789–800), the 10 s preview, unlimited flight (570) and the iron tools |
| Bugs to fix on the way | — | — | Points are given **per member** (770–771), so a team of 4 earns 4×; the new code uses `addSidePoints`. The check compares **Material only** (743–760), so a wrong-facing stair passes. Quitting players are not handled. No world wipe after a crash |

Missing entirely: Blueprint Gallery, 3 plots, Gold Build, team rooms, portal, islands, elytra/fan/pads,
shopping book, HUD slots, EOTM bonuses, partial pay, block subtitles, results holograms, trolley.

---

## 4. World design

**Build agents must open the listed images before building each part** (owner rule: recreate from
images, never from text). Each part is accepted by a side-by-side render vs. those images.

### 4.1 Overall layout

One saved world, **`mcc_buildmart`** (void generator). It is **generated once** by code, previewed and
saved. Each game then only resets the parts that change: plots, gallery pads, resource stock and gold.
This replaces today's "delete the world and rebuild it at every server start".

| Area | Where | Size (blocks) |
|---|---|---|
| Mart centre (gallery disc) | (0, 100, 0) | disc ⌀ 67 |
| Colour pavilion ring | radius ≈ 56 | 8 floating decks, ~16 × 11 |
| Upper category islands | radius ≈ 118, tops at y 132–140 | 8 islands, 36–44 across, 16–22 thick |
| Dome + outer wall | radius ≈ 150 | apex ≈ y 165 |
| Fall line (mart) | y < 85 | — |
| Team rooms | 10 sealed halls in a row at z = +400 (x = −450 … +450, every 100) | interior ~47 × 44, ceiling +14 |
| Spectator overview | above the gallery disc, looking at the pavilions | `McmSpectator.Zone` around the mart |

Scale follows the research estimates (`buildmart/layout/data/dimensions.md`, `buildmart/resources/data/islands.md`),
then gets **tuned by timing**, not by blocks: a gather round trip must take **≤ 30 s**, because top teams
finish a build every ~45 s (layout F29). See Q5 for scale.

**Reference images (whole mart):** `bm-1/images/wiki_current_map_blueprint_topdown.png`,
`buildmart/layout/frames/pack/00_map_central_island_aerial.jpg`, `bm-1/images/wiki_current_map_topdown.png`,
`buildmart/layout/frames/pack/19_official_revised_map.png`, `bm-1/images/wiki_elytra_flying_mart.png`.

### 4.2 Gallery disc (the centre)

| Element | Spec | Conf. |
|---|---|---|
| Floor | Saturated **blue concrete** with white concrete "blueprint" line-work (dashes, crosses, guide arcs), a white/blue rim | H |
| Pads | **3 × 3 lattice** of 9×9 pads (7×7 build area + 1-block light-grey/stone-brick rim), 3-block aisles. 7 blueprint pads + 1 **Gold pad** + 1 decorative checkered pad. Empty pads are pale blue squares | M (exact lattice) |
| Pad floor | Each build has its own **floor theme** (grass, sand, dirt, stone…), shown under the model | H |
| Select button | 1×2 post at the front edge of each pad: gold frame, red body, big **red button** cap | H |
| Pad hologram | Floats ~2.5 above the pad front: name, star row, "Materials:", one line per material (icon, name, right-aligned "N×") | H |
| Empty/restock state | Model removed, pad greyed, single line "This blueprint will appear soon..." | H (look) / L (timing) |
| Gold pad | A larger grey pad with a floor hologram "<Name> / Golden Build / ★×7", 1:1 model, and a red/white striped arch behind it. Before 4:00 it is closed ("???", purple curtain) | M |
| Portal pavilion | North edge: white quartz temple, red columns/roof, red carpet, **purple portal door**. This is the arrival point from every team room | H (look) |
| Launch pads | Two big round pads (⌀ ~11, black/white rim, brown mesh centre) flanking the pavilion | H |
| Fan | South edge: sunken grey bowl (⌀ ~13) with blades. The updraft lifts players to elytra height | H (exists) / L (lift height) |
| Speed pads | 8 orange spiral 2×2 pads near the rim | H |
| Team flags | 10 posts with team-colour flags along the rim | M |

**Reference images:** `bm-1/images/wiki_blueprint_gallery.png`, `bm-1/frames/004455_cur_gallery_deck_topdown_dioramas.jpg`,
`bm-2/frames/gallery/01_gallery_overview.png`, `02_gallery_ground_level.png`,
`buildmart/rules/frames/04_gallery_blueprint_podium.png`, `bm-2/frames/gallery/09_selection_cart_detail.png`,
`bm-2/frames/gallery/11_blueprint_name_stars_materials_detail.png`, `bm-2/frames/gallery/10_restock_hologram_detail.png`,
`bm-2/frames/gallery/05_gallery_empty_pads_restock.jpg`, `bm-1/frames/004400_cur_gallery_gold_tendril_plot.jpg`,
`bm-1/frames/zz_detail_cur_gold_plot_hologram.jpg`, `bm-1/frames/003910_cur_gallery_pavilion_aerial.jpg`,
`buildmart/layout/frames/pack/15_mcc_lettering_speed_pads.jpg`, `bm-1/frames/zz_detail_cur_central_fan_housing.jpg`.

### 4.3 Colour pavilions (lower ring)

- **8 floating decks** around the disc (dark grey floor, white edge stripes, red button pads), separated
  from the disc and from each other by short gaps that you cross with a hop or a glide. Each deck carries
  **2 rack towers, one per colour**: a steel scaffold frame with ladder posts and 4 shelves. Each shelf
  holds a big cube of stock (concrete ×2 shelves, powder ×1, stained glass ×1). The roof has orange/black
  hazard trim and a **launch pad**.
- A giant **colour-name sign** in block letters on each tower's inward face (e.g. "BLACK", "LIME").
- Colour pairs (order is a design choice, **L**): white·light gray, gray·black, brown·red, orange·yellow,
  lime·green, cyan·light blue, blue·purple, magenta·pink.

**Reference images:** `buildmart/layout/frames/pack/16_colour_pavilion_glass_cases.jpg`, `bm-1/frames/004000_cur_colour_island_yellow_interior.jpg`,
`004020_*`, `004030_*`, `004050_cur_lower_ring_walkway_colour_islands.jpg`, `004250_*`, `bm-1/images/wiki_elytra_flying_mart.png`,
`buildmart/resources/frames/pack/17_colour_bay_white.png` … `20_colour_bays_blue_purple_white_red.png`.

### 4.4 Upper category islands

Thick floating chunks: grass/dirt lip, grey stone underside with stalactites. Stock is shown as **block
stacks on scaffold shelves and piles** with hazard-striped kerbs. Decorative props sit on top (trees on
LOGS, cacti/camels on SAND, flowers on GARDEN). Each has a **giant sign hung from the dome truss** in its
own style (LOGS in wood with log-end letters, SAND in sandstone, STONE in grey, NETHER in dark red, and so
on). Clockwise from north (**M**): ORES → NETHER → BRICKS → LOGS → SAND → STONE → COPPER → GARDEN. There
are no bridges: you reach them by fan + elytra + pads only.

**Reference images:** `buildmart/layout/frames/pack/09_upper_islands_logs_sand.jpg`, `bm-1/frames/004330_cur_nether_island_interior.jpg`,
`buildmart/resources/frames/pack/11_ring_garden_nether_bricks.png`, `12_ring_stone_copper_garden_sand.png`,
`13_island_copper_from_air.png`, `14_island_copper_block_close.png`, `28_ring_sand_stone_logs.png`,
`bm-1/images/wiki_elytra_flying_mart.png`.

### 4.5 Stock (what each island sells)

From the official block poster (`buildmart/resources/frames/pack/00_wiki_block_poster_all.png` to `09_*`),
machine-readable in `buildmart/resources/data/blocks.csv`:

| Island | Blocks |
|---|---|
| ORES | redstone, diamond, emerald, gold, iron, coal and quartz **blocks** |
| NETHER | quartz block, warped wart block, nether wart block, nether bricks, blackstone |
| BRICKS | bricks, prismarine bricks, mossy stone bricks, stone bricks, nether bricks |
| LOGS | oak, spruce, birch, jungle and dark oak: **log + planks** each |
| SAND | sand, sandstone, red sand, red sandstone |
| STONE | stone, diorite, andesite, granite |
| COPPER | copper block, cut copper, oxidized copper, oxidized cut copper |
| GARDEN | lily of the valley, allium, cornflower, oxeye daisy, dandelion, blue orchid, azure bluet, poppy, pink tulip, orange tulip, oak sapling, moss block, azalea |
| Colour pavilions | 16 colours × (concrete, concrete powder, stained glass) |

Everything else a build needs (stairs, slabs, walls, fences, doors, trapdoors, chains, glass, cut copper
stairs and so on) is **crafted, stonecut or smelted in the team room**, as in MCC. A build-catalogue check
(5.4) refuses any build that needs a block players cannot get.

Mining: break → the item goes straight to your inventory (no drops). The block regrows after
`regrow-seconds` (default **3 s**, `[default]`) **only if the spot is still empty**.

### 4.6 Dome, wall, lighting and palette

- Dome: light-blue tinted glass panels on **yellow truss** meridians, with a ring of **red chain-link
  ornaments** high above the disc. Players fly up through the rings from the fan. An outer **corrugated
  grey metal wall** runs around the base with hanging **red banners with yellow fringe**. MCC's banners say
  "MCC"; ours carry our brand (Q2).
- Lighting: bright daylight (time fixed at noon). Glowing accents under the islands so they read as
  floating. Recessed lamp strips along pavilion and plot edges.
- Palette: blue/white disc; red/white/yellow trim; dark grey decks; saturated colour cubes; natural
  palettes on the islands.

**Reference images:** `bm-1/images/wiki_elytra_flying_mart.png`, `bm-1/frames/004230_cur_fan_ring_ceiling_islands.jpg`,
`bm-1/frames/004220_cur_outer_ring_mcc_banners_arrows.jpg`, `buildmart/look/README.md` (palette notes),
`buildmart/look/data/palettes.csv`.

### 4.7 Team build room (×10)

Each team gets an identical sealed hall (~47 × 44, ceiling +14) and arrives in it at the start, through
the portal, and after a fall.

| Element | Spec | Conf. |
|---|---|---|
| Plot pairs | 3 pairs side by side. Each pair = **build plot** (front) + **reference plot** (back). Each plot is a 7×7 surface on the floor, 1-block white rim with marked corner blocks, outer step of light-grey stone bricks (11×11 footprint) | H (pairs) / M (exact trim) |
| Build volume | 7×7, up to **12 high** above each plot (config cap 16) | M |
| Plot hologram | On the build plot: name, star row, "Materials:", lines with icon/name/count. Empty plot: "Select a blueprint in the mart!" | H |
| Gold dais | Back wall, centre: raised **+2 yellow terracotta dais** with an orange front panel, 7×7 gold build surface, a pixel-art **crown** on the wall, hologram "Gold Build / Copy this directly from the mart!", yellow/black hazard canopy, and 2 green spiral jump pads at its foot. **Never a reference** | H |
| Chests | Counter with **5 chests labelled R O Y L B** (coloured letter blocks above them), team-only | H |
| Crafting | 2 crafting tables, 2 stonecutters | H (exist) / M (counts) |
| Smelters | 2 alcoves with hand-painted "SMELTER" boards, 8 furnaces each, always fuelled | H |
| Mart portal | Purple portal in a quartz frame on the front wall, "MART" hologram + down arrow | H |
| Walls/ceiling | Pale stone-brick base, teal upper band, red panels, team-colour glass strips; ceiling of orange terracotta beams, yellow/black hazard beams and white skylights | H |
| Flight | Allowed in the room | H |

**Reference images:** `bm-1/images/wiki_build_area.png`, `buildmart/layout/frames/pack/04_build_area_overview.jpg`,
`bm-1/frames/003710_cur_buildroom_wide_three_plots_smelter.jpg`, `003740_*`, `003550_*`, `003830_*`, `003850_*`,
`bm-2/frames/build_area/06_team_build_room_pregame.jpg`, `buildmart/rules/frames/01_team_build_area_3plots_gold.png`,
`buildmart/rules/frames/12_portal_to_mart.png`. Gold dais: `bm-2/frames/gallery/06_gold_build_podium.jpg`
(it shows the team room, despite the folder), `bm-1/frames/003630_*`, `003530_*`, `004410_*`,
`buildmart/layout/frames/pack/06_*`, `07_*`. Plot hologram: `bm-2/frames/build_area/02_plot_hologram_stars_materials.png`,
`buildmart/builds/frames/07_material_hologram_tko_native.png`. Chests: `buildmart/rules/frames/08_team_chest_gui.png`,
`buildmart/look/frames/vod/buildarea_reference_chests.png`.

### 4.8 Travel and falling (how it works in Paper)

| Mechanic | Implementation |
|---|---|
| Elytra | Unbreakable elytra in the chest slot for the whole game. `allowFlight` is **on** in team rooms and **off** in the mart (zone check every 5 ticks) |
| Fan | A player inside the bowl's column gets an upward push each tick (velocity, tuned until it reaches ~+45). Particles + wind sound. Players glide off at the top |
| Launch pads | Reuse the Ace Race launch-pad code and look (`game-plugins/AceRace/.../RaceGame.java`, `map/RaceGeo.java`): step on → angled boost, cooldown, sound |
| Speed pads | 2×2 spiral pads → short forward boost |
| Portals | Purple portal blocks with a region trigger (the vanilla portal event is cancelled). Room portal → mart pavilion; pavilion → **your own team's room** |
| Falling | Below y 85 in the mart: teleport to your room spawn, remove every non-kit item **including the cursor item**, keep tools/elytra/book, title "You fell! Your materials were lost", sound |

### 4.9 The shopping trolley (only if Q1 says so)

In MCC the trolley belongs to the **old mall map**, not the island map. If you want it, it comes as a
second map, **"Classic Mall"** (Phase 7): an indoor mall on a white grid floor with red carpet runners,
department shopfronts (LUMBER, STONE, COLOURS, ORES, OUTDOOR), **pitfalls** that cost your carried
blocks, a central plaza, and a build zone entered through a passage. Gallery, plots, gold and scoring
stay the same.

How the trolley would work in Paper 1.21.1:
- A "Shopping Trolley" item in the hotbar. Right-click the floor to spawn your trolley and sit in it.
  Sneak to get out; the trolley disappears. One trolley per player.
- **Recommended technique:** a server-driven vehicle. The player sits on an invisible carrier entity with
  a trolley model (resource-pack item on an `ItemDisplay`). Each tick the server pushes it forward in the
  direction the player looks, with momentum and drift. That is "fast but less control", and it needs no
  global retexture. Paper 1.21.1 cannot read WASD from a rider (that event arrives in 1.21.3+), which is
  why steering is by looking.
- Alternative: a real boat with the trolley model, which steers smoothly with WASD. Boats only move fast
  on ice, though, so the mall floor would have to be packed ice retextured to look like tiles. That
  retexture would apply on every map. Not recommended.
- Pitfalls: same fall rule (lose carried blocks, keep kit, respawn at the build zone).
- Every trolley is removed on dismount, death, quit and game end.

**Reference images:** `buildmart/cart/frames/ref/11_cart_ride_thirdperson.png`, `12_cart_ride_firstperson.png`,
`13_cart_rail_mart.png`, `18_old_mart_departments.png`, `bm-1/frames/zz_detail_orig_shopping_carts_zoom.jpg`,
`bm-1/frames/021550_*` … `022140_*` (mall corridors, pitfalls, departments, build zone),
`bm-1/images/wiki_original_map_topdown_1920.png`, `wiki_original_map_overview.png`,
`buildmart/audit/frames/official_cart_item_hotbar.png`.

---

## 5. Builds

### 5.1 The catalogue

- **Source:** the **114 build renders** in `bm-2/frames/builds/` (one front-angle render each, listed in
  `bm-2/builds_manifest.json` with name, debut event and gold flag). **14 are flagged Gold**, 100 are
  regular. Official 7×7 base renders for scale: `buildmart/builds/frames/10_render_mighty_piston_1920x1080.png` … `14_*`.
- **Authoring:** a build agent opens the render and writes the build as data, then a preview render is
  made at the same camera angle for side-by-side review. The white stepped plinth, nether-brick strip and
  red carpet in the renders are **the wiki's photo stand**, not part of the build. The pad floor theme is
  part of the build data.
- **Hidden sides:** each render shows one angle. The parts you cannot see (back faces) are filled by
  symmetry and common sense, and every such guess is marked in the build file (`guessed: true` + a note).
- **Blocks:** MCC has swapped build materials since MCC 8, and some legacy builds use blocks that are no
  longer stocked (wool, terracotta). Recommendation (Q9): **keep the shape and swap to the nearest
  stocked block** (wool → concrete of the same colour).

### 5.2 Stars and gold

- The legacy 114 have **no published stars**. Only 5 names overlap with the rated P25+ list
  (`buildmart/builds/data/builds.csv`: Fountain, House, Penguin, Rocket Spleef, Trampampoline), and those
  keep their MCC rating.
- For the rest we assign stars with a written rubric: how readable the silhouette is, number of distinct
  materials, number of rotation-sensitive blocks (stairs/slabs/doors), crafted/smelted materials, and
  block count as a time budget only. MCC confirms block count does **not** decide stars
  (`buildmart/builds/README.md` F12–F14). The owner reviews the full star list as one table (Q4).
- Target split of the 100 regular builds, matching the gallery's 2:2:1:1:1 ratio: **~29 × 1★, ~29 × 2★,
  ~14 × 3★, ~14 × 4★, ~14 × 5★**. The 14 gold-flagged builds become the **Gold pool (7★)**.

### 5.3 How builds are dealt (rotation)

MCC ships about **60 regular builds per event, split 16/16/8/8/8, plus 4 golds**
(`buildmart/builds/README.md` F2). That is exactly what one 12-minute game uses: 8 restocks × 7 pads =
56, and 4 gold windows.

1. **At game start**, draw the game's **deck** from the catalogue (seeded, logged): 16×1★, 16×2★, 8×3★,
   8×4★, 8×5★ and 4 golds. Builds used in the last game are avoided where possible.
2. **Every 90 s**, each **unpinned** pad takes the next build of its tier from the deck. The new model
   appears after a 2 s "This blueprint will appear soon..." pause (`[default]`).
3. A pad is **pinned** while any team holds its build and has not finished it (F7). To stop a stale
   gallery, a pad stays pinned for at most `max-pin-restocks` restocks (default 1, `[default]`, Q7). The
   teams' own copies are never affected by restocks.
4. **Gold:** 4:00, 6:00, 8:00, 10:00 → next gold from the deck. Before 4:00 the gold pad shows "???".

### 5.4 Storage

One JSON file per build in `src/main/resources/builds/` plus `builds/index.json`. This follows the
format proposed in `buildmart/builds/data/format.md`, simplified:

```json
{
  "id": "coin_casting", "name": "Coin Casting", "stars": 7, "gold": true,
  "size": [7, 7, 5], "floor": "minecraft:gold_block",
  "palette": { "g": "minecraft:gold_block", "o": "minecraft:orange_concrete",
               "s": "minecraft:spruce_slab[type=top]" },
  "layers": [ ["ggggggg", "..."], ["..."] ],
  "guessed": [ "back row of layer 2 mirrored from the front" ],
  "reference": "mcc_archive/build_mart/bm-2/frames/builds/BSABM-Coin_Casting.png"
}
```

- `materials` (the shopping list) is **computed** from the layers, never typed by hand. A double slab
  counts as 2 slabs, a door (2 blocks) as 1 door, a bed as 1 bed.
- **Catalogue check** (runs in `mvn test` and at server start, and fails loudly): every file parses, fits
  7×7×12, has stars 1–5 or 7 (gold), uses only real block states, has no floating blocks (the old
  `unsupported()` lint), and **every material is obtainable** from an island, a craft, a stonecut or a
  smelt.

### 5.5 Build check (verification algorithm)

For each active plot, the plugin holds the target as a 7×7×H grid of **canonical block states**.

1. **Canonicalise** each block state by keeping only what the player controls and what MCC checks:
   - **Checked:** block type, `facing`, `half`, slab `type` (bottom/top/double), `axis` (logs/pillars),
     `open` (doors/trapdoors), `face` (buttons/levers), `rotation` (signs/banners/heads), `attachment`,
     `hanging`.
   - **Ignored:** door `hinge` (MCC's "texture rotation"), `powered`, `waterlogged` (unless the target is
     waterlogged), leaf `distance`/`persistent`, `snowy`, the connection sides of fences/walls/panes/bars,
     and stair `shape`. These are either auto-calculated from neighbours or invisible.
2. **Compare** every cell: required cell = correct if the canonical states are equal; required cell
   holding something else = wrong; empty target cell holding a block = wrong (extra).
3. **Complete** when correct = total and wrong = 0. Pay immediately, then clear the plot and the
   reference, set the slot to "Empty Plot #N", and refresh the book, sidebar and hologram.
4. **When:** event-driven. Place, break, physics, falling blocks and liquids inside a plot mark that plot
   "dirty", and it is checked on the next tick (at most 588 cells: cheap). A slow safety re-check runs
   once per second.
5. **Feedback:** the action bar on your plot shows "placed/needed • N wrong". Sneak shows red particles on
   wrong cells (`[default]` helper, not MCC).
6. **Partial pay at 0:00:** snapshot each regular plot once: `floor(correct/total × stars × value)`. Gold is
   excluded. Wrong blocks do not reduce it (Q8). The snapshot is taken once, so a build cannot be paid
   twice.

Unit tests (no server): rotated stair fails, correct stair passes, hinge flip passes, double slab
passes, an extra block fails, a log with the wrong axis fails.

---

## 6. UI

### 6.1 Shopping-list book

- A **written book** in hotbar slot 9, protected: it cannot be dropped, moved into chests or lost on a
  fall. It is rebuilt whenever your team's builds change (select, complete, gold, fall).
- Content, as in MCC: "Page N of M" (vanilla), build name at the top of its first page, "Materials:", then
  one line per material: **icon, name, count "N×"**, right-aligned. Long lists continue on the next page.
  We add "(cont.)" on continuation pages, a small `[ADAPT]` (MCC leaves them untitled). Long names wrap
  and are never cut off. Empty state: "Select builds in the mart!". Gold page: name, "Golden Build ★×7",
  materials only, no layout (Q6).
- **With the resource pack:** notebook-paper skin, orange Done button, item icons (font glyphs that point
  at vanilla block/item textures, so nothing is copied). **Without the pack:** the same text without
  icons.
- **Reference images:** `bm-2/frames/ui/01_shopping_list_book.jpg`, `buildmart/cart/frames/ref/03_book_page1_alternator.png` …
  `06_book_page4_clouds.png`, `20_book_empty_state.png`, `07_book_hotbar_slot8.png`.

### 6.2 HUD

| Place | Content (MCC) | How |
|---|---|---|
| Top centre, bar 1 | Game timer `MM:SS` | Core boss bar (`ctx.setTimer`) |
| Top centre, bar 2 | **"GOLDEN BUILD IN: 01:04"** with progress, counting to the next gold | Plugin boss bar |
| Top right | Team **stars ★** and **builds ⛏** counters | First sidebar line |
| Sidebar | GAME COINS standings (core), then **BUILD SLOTS:** `↳ Whale (x3 ★)` / `↳ Empty Plot #2` | **Needs a small core change** (Q12): game lines are currently global, but slots are per team |
| Above hotbar | Target block name (green) + held item (white) | Action bar, every 4 ticks (ray trace 6 blocks) |
| Plot | "BUILD COMPLETED! <NAME>" floating text, particle burst, falling gold | TextDisplay + particles |
| Chat | `[+10] Your team completed Coin (★★)`, `<Team> completed Whale (★★★)`, "Blueprints plots now have new builds!", "A new golden build is now available!" | Broadcast + per-team messages |
| Titles | Pre-game rules card, "GO!", "Golden Build selected!" | `McmFx.titleCard` / `title` |
| End | Each plot's hologram becomes a result card: "<Build> / +N coins / Completions: k". EOTM in chat, then the core's placement reveal | TextDisplay + `McmFx.placementReveal` |

**Reference images:** `bm-1/frames/004400_*` (timer + gold bar + stars/builds + slots),
`buildmart/rules/frames/02_*`, `03_build_slots_three_selected.png`, `buildmart/cart/frames/ref/15_*`, `16_*`, `17_gold_countdown_hud.png`,
`bm-2/frames/ui/02_build_slots_sidebar.png`, `04_hud_and_team_coins.jpg`, `05_build_completed_feedback.jpg`, `06_completion_chat_and_coins.png`,
`09_game_results_overlay.jpg`, `bm-2/frames/build_area/07_build_stats_hologram.png`, `buildmart/rules/frames/09_build_completed_hologram.png`.

### 6.3 Holograms

TextDisplay entities (billboard centre, shadow on) for every pad, plot and island label. Icons come from
pack glyphs, and the star row uses a pack sprite with "★" as the fallback. All of them are spawned
non-persistent and tracked, and removed on stop.

### 6.4 Sounds

MCC's sounds were not captured, so all of these are `[default]`: select = bell, restock = chime, build
complete = challenge-complete toast, gold available = level-up, gold failed = low anvil, fall =
villager "no". With the pack, they are replaced by generated `bm.*` stings through `McmFx.packSound`.

### 6.5 Resource-pack additions

These go in the existing original pack generator (`resourcepack/`, "Block Showdown", or
`_candidates/pack/build_pack.py` like Dodgebolt). Nothing is copied from the MCC pack in
`mcc_archive/resource_pack/` (downloaded, untrusted, Noxcrew's).

1. Book GUI skin (notebook paper, orange Done). This replaces the vanilla book texture on every map, so
   the owner confirms it (Q11).
2. Item-icon glyphs (font providers pointing at vanilla textures) and the star sprite.
3. Red-button-on-gold-post model (fallback: blocks).
4. Trolley model (only with Phase 7).
5. `bm.*` sounds.

---

## 7. Code architecture

### 7.1 Classes (package `com.mcm.games.buildmart`)

| Package | Classes | Job |
|---|---|---|
| (root) | `BuildMartPlugin`, `BuildMartGame`, `BmConfig` | Enable/disable, `McmGame` implementation, state machine **LOADING → PREGAME → PLAYING → RESULTS → CLEANUP**, config |
| `arena` | `ArenaLayout` (all anchor points, loaded from `arena.json`), `ArenaGenerator` + one generator per part (`GalleryDisc`, `PortalPavilion`, `Fan`, `ColourPavilion`, `UpperIsland`, `Dome`, `TeamRoom`, later `ClassicMall`), `ArenaPaster` (TGTTOS `Paster` pattern: chunk-grouped, ≤15 ms per tick), `ArenaReset` | Build the world once, then reset only the dynamic parts per game |
| `team` | `TeamState`, `RoomAssigner` | Team → room, members, stars, builds, coins |
| `plot` | `Plot`, `PlotEvaluator` (dirty queue), `BuildMatcher`, `Canonical` | The 3+1 plots per team, the check from 5.5 |
| `builds` | `BuildDef`, `BuildCatalog` (load + check), `GameDeck`, `MaterialList`, `SourceIndex` (material → island/craft/smelt) | Catalogue and dealing |
| `gallery` | `Gallery`, `GalleryPad`, `SelectButton` (block post + `Interaction` entity as the click target) | Pads, restocks, pinning, selection |
| `gold` | `GoldCycle`, `GoldAttempt` | Gold windows, success, clear-on-fail |
| `mart` | `ResourceStock` (regrow queue), `TravelService` (elytra/flight zones), `FanService`, `PadService`, `PortalService`, `FallService` | Gathering and movement |
| `ui` | `ShoppingBook`, `Hud`, `Holograms`, `BlockSubtitle`, `Sounds` | Everything the player sees |
| `score` | `Scoring` | Completion pay, partial pay, EOTM |
| `listen` | `BuildListener`, `MartListener`, `ProtectionListener`, `PlayerListener` (join/quit/late join/respawn) | Bukkit events, ignoring other worlds, no un-cancelling |
| `cmd` | `BmCommand` | Admin commands |
| `trolley` (Phase 7) | `TrolleyService` | Rideable trolleys |

### 7.2 Data files

| File | Contents |
|---|---|
| `config.yml` | All timings and values, every non-researched one marked `[default]` (house style) |
| `builds/*.json` + `builds/index.json` | Build catalogue (5.4) |
| `stock.json` | Island → materials (from `buildmart/resources/data/blocks.csv`) |
| `arena.json` | Written by the generator: pad, plot, room, portal, pad, fan and stock coordinates. The game reads this instead of hard-coding numbers |

### 7.3 Config (defaults)

```yaml
game-seconds: 720            # MCC
pregame-seconds: 30          # [default] MCC shows ~45-60 s with a tutorial card
gallery:
  restock-seconds: 90        # MCC
  tiers: [1, 1, 2, 2, 3, 4, 5]   # MCC
  appear-delay-seconds: 2    # [default]
  max-pin-restocks: 1        # [default]
gold:
  first-at: 240              # MCC
  every: 120                 # MCC
  stars: 7                   # MCC
max-active-builds: 3         # MCC
points:                      # house convention: MCC coins scaled /5, team pool split by addSidePoints
  per-star: 4                # MCC 20
  eotm: [20, 10, 5]          # MCC 100/50/25
  partial-gold: false        # MCC BG
regrow-seconds: 3            # [default]
fall-y: 85
kit: { tool4: DIAMOND_HOE, haste: 1 }   # [default] for the 4th tool
trolley: { enabled: false }  # Phase 7
```

### 7.4 Admin commands (`/bm`, op only)

`arena build|reset|tp <part>|dump <part>`, `builds list|check|preview <id>|place <id> <team> <plot>`,
`gallery restock|set <pad> <id>`, `gold now|set <id>`, `plot fill <team> <plot> [percent]` (test helper),
`plot clear <team> <plot>`, `fast` (short timings for testing), `debug`, `reload`.

Core hooks: `skipWait()` (skips pre-game/results), `maps()` / `forceNextMap()` / `currentMap()` for
"islands" and later "classic", `onLateJoin()` (join your team if it is playing, otherwise spectate the
mart).

### 7.5 Core integration

- `McmApi.registerGame`. Uses `ctx.sides()`, `sideLabel()` and `teamColor()`. Points go through
  `addSidePoints` (team pool). `setTimer` for the main bar. `McmSpectator.Zone` for spectators.
  `McmFx` for titles, sounds, fireworks and the placement reveal. `Waits` for every non-gameplay delay.
- **One small core addition** (Q12): `ctx.setSideSidebarLines(sideKey, lines)`, so each team sees its own
  BUILD SLOTS. Fallback without it: slots shown in the action bar while you hold the book.
- Solo players: each solo player is a "team" with their own room (10 rooms max; above 10 sides, solos
  share rooms by pairs, or the game refuses to start, logged clearly).

---

## 8. Build order, milestones and tests

Every phase ends with something **you can test in-game**. Nothing starts before you approve.

| Phase | What gets built | You test in-game | Rough effort* |
|---|---|---|---|
| **0. Decisions** | Answers to section 9; this plan updated | — | — |
| **1. Engine on a grey box** | Grey-box mart (flat disc, 9 pads with buttons, simple racks for a few materials) + **1 grey-box team room** with 3 plot pairs, gold dais, chests, crafting, smelters, portal. Gallery deal/restock/pinning, selection, references, plot holograms, book (text), build check, scoring, fall rule, kit. **20 test builds** from the catalogue | Pick 3 builds, gather, build them, see "BUILD COMPLETED!" and coins. A wrong-facing stair does not count. A 4th pick is refused. Restock every 90 s | 4–5 days |
| **2. First real builds** | **34 builds** from the renders (10/10/5/5/4 + 4 gold), each with a side-by-side preview sheet. Star rubric + star list | Review the preview sheets and the star table; play with them | 3–4 days |
| **3. Gold + HUD + scoring** | Gold cycle (4:00/6:00/8:00/10:00, clear on fail), gold boss bar, BUILD SLOTS sidebar (core change), stars/builds line, block subtitles, completion chat, partial pay, EOTM, result holograms | Play a full 12-minute game in `fast` mode and normal mode; check gold timings and the end screen | 2–3 days |
| **4. The real arena** | Generated mart (disc, pavilion, fan, pads, 8 colour pavilions, 8 upper islands, dome, wall) + 10 team rooms, each part previewed against its images, plus elytra/fan/pad tuning | Fly the mart. Time a gather trip (target ≤ 30 s). Check that each part "looks like MCC" against the images | 7–10 days |
| **5. Full catalogue + pack** | The remaining ~80 builds, resource-pack additions (book skin, icons, stars, button, sounds) | Review previews in batches of 20; play with the pack on and off | 6–8 days |
| **6. Polish + soak** | Performance, late join/quit, crash recovery, README, config docs | Full event run with 4+ teams | 2–3 days |
| **7. Classic Mall + trolleys** (optional, Q1) | Second map with departments, pitfalls and trolleys | Ride, fall in a pitfall, play a full game on it | 5–8 days |

\*Agent working days, rough. Phases 1–6: **~4–6 weeks**. Phase 7 adds 1–1.5 weeks.

### Test plan

- **Automatic (no server, `mvn test`):** catalogue check on every build, the canonical matcher cases
  (5.5), material-list maths, deck dealing (right tier counts, no duplicates), scoring maths (partial,
  EOTM ties, gold excluded).
- **Bot harness (`tools/bot-harness`):** 8 bots in 2–4 teams join, get kits, take the portal, fall
  (materials lost, book kept), quit and rejoin (no duplicate book), late-join. Building is driven with
  `/bm plot fill` so completion, partial pay and gold failure can be tested without precise bot building.
- **Manual checklist (owner + agent), every phase:** start → stop → start leaves no stale blocks, entities
  or items; 3 builds at once; restock pins work; repeat a build; gold appears at 4:00, has no reference,
  and is cleared on failure; book updates; the fall rule; 10 teams + spectators hold 20 TPS (spark).
- **Visual acceptance:** every arena part and every build has a preview render (`McmBuild.dumpRegion` →
  `tools/render_preview.py`) placed next to its reference images. The owner approves before the next
  batch.

---

## 9. Open questions (owner decides)

| # | Question | My recommendation |
|---|---|---|
| Q1 | **Which "shopping cart" do you want?** (a) current MCC only: the gallery **button** pads, no trolley; (b) also a rideable trolley on the island map (not MCC); (c) also a second **Classic Mall** map with trolleys and pitfalls, as MCC 3–13 had | **(a) now, (c) as Phase 7.** The trolley is real MCC only on the old mall map. On the island map it would have almost no floor to ride on |
| Q2 | MCC's map has "MCC" lettering on the disc and "MCC" banners. Copy them, or use our server brand? | Recreate everything 1:1, but **swap the literal MCC logo and lettering for our brand** |
| Q3 | Use MCC's real build names and shapes (from the 114 renders)? | **Yes**: you asked for MCC, not inventions. Private server only |
| Q4 | Stars for the 114 legacy builds are not published. OK for us to rate them with a written rubric and send you the list? | **Yes**, with one review of the whole table |
| Q5 | Map scale: full research size (~300 across, 10 team rooms) or smaller? | **Full layout**, then shrink distances only if a gather trip takes over 30 s |
| Q6 | Does the gold build appear in the book (materials only)? Not captured in footage | **Yes, materials only**, no layout |
| Q7 | Pinned gallery pads: may a pad stay pinned forever if some team never finishes it? | **No**: pinned for at most 1 extra restock |
| Q8 | Partial pay at the end: do wrong blocks reduce the %? | **No**: % = correct blocks / total |
| Q9 | Legacy builds that use unstocked blocks (wool, terracotta): swap the block or add stock? | **Swap to the nearest stocked block**, keeping the shape (MCC has swapped blocks since MCC 8) |
| Q10 | Points: MCC coins scaled /5 like our other games (4 points per star team pool, gold 28, EOTM 20/10/5)? | **Yes** |
| Q11 | Resource pack: the book skin replaces the vanilla book look on **all** maps. OK? | **Yes** (books are rare in other games). Otherwise we keep the vanilla book look |
| Q12 | Approve a small core change so each team sees its own BUILD SLOTS in the sidebar? | **Yes**. Fallback: action bar |
| Q13 | Pre-game length (MCC ~45–60 s with a how-to-play card) | **30 s** with a rules card, `skipWait` for admins |
| Q14 | Late joiners: put them on their team if it is playing? | **Yes**, otherwise spectate |

---

## 10. Risks and effort

| Risk | Impact | Mitigation |
|---|---|---|
| Builds come from **one camera angle**; the back sides are guessed | Some builds differ from MCC | Symmetry rules, every guess marked in the file, owner review of the previews |
| **114 builds is the biggest single job** | Long tail of authoring | Batches of 20 with review; the game is playable from 34 builds (Phase 2) |
| Arena is large (~25k+ blocks of detail plus 10 rooms) and needs to look right | Weeks of iteration | Generate once and save the world; per-part previews against images; grey-box first so gameplay never waits for art |
| Travel feel (fan lift, glide distances, pads) | Too slow or too easy | Timing targets (≤ 30 s gather trip), tunable config, playtest in Phase 4 |
| Paper 1.21.1 limits: no rider WASD input, no sprite text components | Trolley steers by looking; icons are flat glyphs | Accepted; noted in 4.9 and 6.1 |
| Per-team sidebar needs a core change | Touches the shared core | Small additive API; fallback ready |
| Unmeasured MCC details (regrow time, gold book page, pin rule, exact plot trim) | Small differences from MCC | All in config as `[default]`; easy to change after playtests |
| Branding/IP (MCC builds, look) | Only an issue if the server goes public | Private server; logo swapped (Q2) |
| Old code assumptions (world wiped each boot, per-member points) | Hidden bugs | Phase 1 replaces them; tests cover team-pool scoring and restart cleanup |

**Overall effort:** about **4–6 weeks** of agent work for Phases 1–6, plus **1–1.5 weeks** for the
optional Classic Mall with trolleys. The playable game exists after Phase 1 (grey box) and becomes
recognisably MCC after Phase 4.
