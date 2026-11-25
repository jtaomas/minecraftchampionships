# REFERENCE_IMAGES — MG-SB-01 Sky Battle

All images in `frames/` were opened and checked before this file was written. Re-checked again on 2026-11-25 after A14 QA: all 17 cited paths exist, every file opens, and the dimensions in the table match the files on disk. They are full-resolution PNGs; no cropping after render except the labelled overlay. **H** for what is visible; **M/L** where I interpret intent.

## How the images were made

- **Block source:** `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3` (1.17.1, DataVersion 2730).
- **Dump adapter:** A6's `../data/dump_117_mcmdump.py` (read-only) with bounds `1948 0 -52` → `2052 80 52` and crops listed below.
- **Renderer:** shared `C:\Users\LeonS\Desktop\Mcc\tools\render_preview.py` (read-only), unchanged. Angles 0 and 2 are two 90°-apart isometric views; top is orthographic.
- **Labelled top:** `data/make_top_label.py` (PIL) draws labels on the block-derived top surface at 6 px/block; the underlying block colours are my palette map, not the shared renderer.
- **Sections:** `data/section_plot.py` (PIL) draws 1-block cells of the plane x=2000 or z=0; not a Minecraft screenshot.

## Index

| # | File | Size (px) | Source / bounds | What it shows | What to learn | Conf. |
|---|---|---|---|---:|---:|---|
| 01 | `frames/01_top_labelled.png` | 540×540 | custom top surface, x1955-2045, z-45..45, barriers hidden | Four gift boxes at cardinals, four balloons at diagonals, circular gallery with four coloured canopy quadrants, tiny mid plus, labels RED/GREEN/BLUE/YELLOW | The whole layout in one view; team colours and rotation order (N Yellow → E Green → S Blue → W Red). | H |
| 02 | `frames/02_top_clean.png` | 443×443 | shared renderer top, whole dump | Same plan without labels; the canopy reads as a thick ring, the mid as a tiny yellow/black cross | Confirms the ring is a **canopy**, not a solid mound; the build is only 79×79 in a 105×105 box | H |
| 03 | `frames/03_iso_angle0.png` | 778×622 | shared renderer iso, angle 0 | Full arena from the south-east: gift boxes, balloons, gallery, hanging sky-lobby lantern | Team-coloured canopy roofs; the lobby hangs above the centre on a chain; the gift boxes are hollow boxes | H |
| 04 | `frames/04_iso_angle2.png` | 778×622 | shared renderer iso, angle 2 | Full arena from the north-west; opposite side of the same structures | The ring is symmetric; the lantern reads as a gray/gold bulb; the balloons are at the diagonals | H |
| 05 | `frames/05_ring_iso.png` | 1298×943 | ring crop 1958-2042, 10-45, -42..42 | Close-up of the circular gallery: four coloured canopy segments, white legs/trim, oak colonnade, inner void, mid tower | The canopy is a **floating roof** on white legs; the gallery floor at y25 is open underneath; the four segments are separated at the diagonals | H |
| 06 | `frames/06_ring_top.png` | 781×781 | same ring crop, top | Ring plan: outer stone rim, four coloured quadrants, white quadrant seams, inner void, mid cross, balloons/gift boxes around | The ring is a **donut**, not a disc; quadrant colours are separated by white seams; the outer rim is continuous | H |
| 07 | `frames/07_island_red_iso.png` | 528×416 | Red gift box 1955-1980, 10-32, -10..10, iso | The Red gift box: white stone base, red box, white ribbon cross, quartz bow loops | The gift box is a **13×13×16 hollow box**, not a flat island; the ribbon is a white cross; the top is walkable | H |
| 08 | `frames/08_island_red_top.png` | 368×256 | same, top | Red gift-box plan: red lid with white ribbon, small brown centre (shulker/crafting table) | The spawn area is a 1-block-wide ribbon cross; the shulker/crafting table sit on it; the box is symmetric | H |
| 09 | `frames/09_shulker_iso.png` | 488×348 | 1963-1968, 22-26, -2..2, iso + top | White shulker box and crafting table on the Red ribbon, surrounded by red glazed terracotta | The team loot container is a **white shulker box**, not a chest; the crafting table is adjacent | H |
| 10 | `frames/10_mid_iso.png` | 990×720 | 1988-2012, 18-36, -12..12, iso | The four canopy segments seen from above, with the mid tower in the centre (team-coloured compass + barrel/crafting table) | The mid is **tiny and central**, not a large island; the canopies are separate roofs around it | H |
| 11 | `frames/11_balloon_orange_iso.png` | 228×480 | 1966-1982, 10-34, -34..-18, iso | The orange NW balloon: wool envelope, timber basket, mast, chain, small barrel at the bottom | The balloon is a **vertical loot tower**: barrel at y14, crate/chest at y26, envelope y31-42 | H |
| 12 | `frames/12_chest_crate_cutaway.png` | 216×312 | 1974-1979, 23-31, -28..-24, cutaway, iso angle 3 | The inside of the balloon crate: oak trapdoor cage with the chest in the centre | The chest is **enclosed in a 3×3 trapdoor crate** inside the balloon; you must break in | H |
| 13 | `frames/13_lobby_iso.png` | 272×448 | 1988-2012, 55-80, -12..12, iso | The hanging sky-lobby lantern: gray wool body, gold/raw-gold cap, red/yellow/blue/green accents, small gray pad at the bottom | The lobby is a **small hanging room**, not a large deck; the barrier cage above is invisible | H |
| 14 | `frames/14_section_east_west.png` | 1080×432 | plane z=0, x-45..45, y10..45 | West-east cross-section: Red island, ring at x1981-1988, void, mid tower, ring, Green island | The ring is a **thin 5-8 block structure** with a floor at y25, canopy at y33-37, lower lip y19; the centre is void | H |
| 15 | `frames/15_section_north_south.png` | 1080×432 | plane x=2000, z-45..45, y10..45 | North-south cross-section: Yellow island, ring, void, mid, ring, Blue island | Same ring profile on the other axis; the gift boxes are 16 blocks tall; the mid is a thin vertical stack | H |
| 16 | `frames/16_barrier_envelope_top.png` | 577×577 | whole dump, top, `--show-barriers` | Red translucent barrier shell: walls at x1948/2052, z-52/52, solid roof y56; the build sits inside | The copy envelope is **much larger than the build**; the barrier roof covers everything; the walls are 13+ blocks outside the visible build | H |
| 17 | `frames/17_balloon_basket_iso.png` | 224×521 | 1968-1980, 12-32, -32..-20, iso | The balloon basket from below: barrel, chain, timber basket, wool envelope | The barrel hangs **12 blocks below** the crate; the chain is the only connection; the basket is open | H |

## What the images do **not** show

- **Lighting.** The renderer uses a flat sky background and no in-game light. Do not infer brightness from these PNGs. **H**.
- **Barrier collision.** Images 01-15 hide barriers; image 16 shows them as translucent red. The invisible walls/roof are not a visual feature in game. **H**.
- **Command blocks.** No image shows the 474 logic blocks in `MG-CMD-01`; the command evidence is `data/commands.csv` / `data/cb14_noloot.txt`. **H**.
- **Chest contents.** All **13 loot containers** contain the loot in the source snapshot (NBT scan); the 93 commands refill the same items. The arena has **19 container block entities** in total: the 13 loot containers plus 5 empty campfires and 1 empty lectern. Image 12 shows the chest container, not the loot; no image is needed for the empty campfires/lectern. **H** (`data/containers.csv`).
- **Spawn cages.** The 3×1 barrier/glass cages exist only during a game; they are not in the source snapshot. **H** commands; **M** live appearance.

## Reading order

1. **01** for the plan and team order.
2. **03/04** for the 3D silhouette.
3. **05/06** to see that the ring is a covered gallery, not a solid mound.
4. **07/08/09** for the spawn pod and team loot.
5. **10** for the mid and the canopy separation.
6. **11/12/17** for the vertical balloon loot.
7. **13** for the pre-game lobby.
8. **14/15** for the exact heights.
9. **16** for the copy envelope.


