# Reference images — Clouds 2

Every image below was opened with the read tool before being cited. All renders were
produced from the read-only source world with `tools/render_preview.py` (or my own
crop/section scripts in `scripts/` using the same colour table). Coordinates in the
file names / notes are absolute world coordinates.

## Overviews

| Image | Source | What it shows | What to take |
|---|---|---|---|
| `renders/overview_top.png` | render, full dump, top, 2 px/block | The whole 325 × 608 build from above: an oval loop of themed islands around empty sky. | The loop shape; the five set pieces; the distance between the start (centre-right) and the planet (top-right). |
| `renders/overview_top_grid.png` | same + 50-block grid | Same with world coordinates. | Any coordinate can be read off this image. |
| `renders/overview_iso_a.png` | render, iso angle 0, 2 px/block | 3D read of the loop; planet + tunnel upper right, rainbow left, castle lower centre. | How islands sit at different heights; the waterfalls; the rainbow as a gate. |
| `renders/overview_iso_a_labeled.png` | same + title bar | Same with a title/description strip. | Presentation copy for the dossier. |
| `renders/overview_iso_b.png` | render, iso angle 2, 2 px/block | Reverse 3D angle; ruin + tunnel lower left, planet left, ring hoops bottom centre, rainbow/terraces right. | The closed-loop silhouette and the ring-flight line. |
| `renders/overview_iso_b_labeled.png` | same + title bar | Same with a title strip. | Presentation copy. |
| `renders/route_labeled.png` | `overview_top.png` + my route polyline | 14 legs L0–L13, numbered badges, landmark labels, start/finish marker. | The route at a glance; use with `data/route.md`. |
| `renders/heightmap.png` | my heightmap script | Top-block Y per column on a blue→white scale. | Elevation flow; note how little height variation there is in the east chain. |
| `renders/route_pads.png` | `overview_top.png` + `pads.json` | Every pad/checkpoint/elytra component as a coloured dot. | Proof that the pads form a continuous chain around the loop. |

## Sections

| Image | Source | What it shows | What to take |
|---|---|---|---|
| `renders/start_top.png` | crop x −60..28, z −10..45, top, 10 px/block | Start pen, checkered line, floor arrow, the pad-legend row west of the pen, the hedge arch. | Exact pen geometry; arrow direction; the legend row is a build, not signage. |
| `renders/start_iso.png` | same crop, iso 6 px/block | The hedge arch, checkered banner, white pen, garden and the colourful legend row. | The start frames the first corner; the legend row is separate from the course. |
| `renders/mid_top.png` | crop x −16..26, z −120..20, top, 8 px/block | Garden → farm → snow transition in one image. | The three checkpoints bands and the first slow pads. |
| `renders/mid_iso.png` | same crop, iso 5 px/block | 3D of the garden, farm terraces and first snow island. | How the farm steps down and the snow island floats above it. |
| `renders/snow_top.png` | crop x −20..45, z −245..−85, top, 5 px/block | The snow sky-island chain with pads and ice pools. | Pad spacing and the waterfall columns. |
| `renders/snow_iso.png` | same crop, iso 4 px/block | The islands as floating platforms with long waterfalls. | The vertical drop under every island (why barriers and the kill plane matter). |
| `renders/planet_top.png` | crop x −195..−15, z −305..−225, top, 4 px/block | The planet pool, the glass tunnel running west, the ruin and the pad islands. | Tunnel alignment and the planet's position relative to the tunnel. |
| `renders/planet_iso.png` | same crop, iso 3 px/block | The tunnel, the planet's brown spiral and the huge waterfall. | The tunnel enters the base of the waterfall. |
| `renders/planet2_iso.png` | crop x −85..−20, z −290..−225, iso 5 px/block | Close-up of the planet pool, gold hangings, waterfall and the checkpoint island with flag. | The gold “lightning” decoration and the pool rim. |
| `renders/planet3_iso.png` | crop x −115..−15, z −290..−225, iso 6 px/block | The planet with the glass tunnel emerging west and the checkpoint island east. | The exact tunnel-to-waterfall junction. |
| `renders/topleft_top.png` | crop x −270..−115, z −320..−140, top, 4 px/block | The C-shaped ruin, the tunnel with red ribs, the white pad islands and the crater from above. | The ruin's C shape and the tunnel's west end. |
| `renders/topleft_iso.png` | same crop, iso 3 px/block | The ruin as a stone amphitheatre, the tunnel, the pad islands and the crater village with its orange arch. | The ruin → pads → crater descent. |
| `renders/leftcol_top.png` | crop x −205..−110, z −300..−130, top, 5 px/block | The tunnel, five-pad arc, narrow bridge and crater bowl. | The five-pad arc at z −245..−230 and the bridge at z ≈ −213. |
| `renders/leftcol_iso.png` | same crop, iso 4 px/block | The crater as a mud-brick bowl village with towers, orange arch and blue portal ring. | The crater's interior layout. |
| `renders/tile_nw.png` | scale-4 overview crop, 1.3× | The north-west quadrant with a 50-block grid. | Read exact coordinates for the ruin/tunnel/crater. |
| `renders/tile_w.png` | scale-4 crop, x −220..−60, z 40..290, 1.2× | The left column: village, rainbow, terraces, bottom terraces. | The 150-block-long terrace descent after the rainbow. |
| `renders/tile_coral.png` | scale-4 crop, x −30..35, z 0..100, 2.2× | The start pen, south corridor and coral pools. | The finish corridor and the coral section's pad pattern. |
| `renders/tile_e.png` | scale-4 crop, x −20..75, z 70..230, 2× | The coral → white platforms → orange arches → castle gap. | The final approach and the orange arch gates. |
| `renders/tile_bottom.png` | scale-4 crop, x −210..80, z 180..300, 1.2× | The whole bottom crossing: terraces → vine path → blue pool → flower bridge → castle. | The bottom leg's eastbound direction. |
| `renders/botleft_iso.png` | crop x −181..−107, z 62..210, iso 4 px/block | The desert village with parrot, the rainbow arch and the white temple/terraces. | The rainbow as a gate and the village as the L8 landing zone. |
| `renders/botleft_grid.png` | same crop, top, 6 px/block + grid | Same from above with coordinates. | Terrace levels and pad positions. |
| `renders/botright_top.png` | crop x −60..80, z 170..315, top, 6 px/block | The blue pool, flower bridge, round platform and castle. | The castle courtyard and the checkered gate at z = 201. |
| `renders/botright_iso.png` | same crop, iso 4 px/block | The castle with three sandstone towers, blue “diving boards” and flower bridge. | The vertical climb inside the castle. |
| `renders/gap_iso.png` | crop x −210..−60, z −170..90, iso 3 px/block | The ring flight: blue hoops and yellow lightning-bolt markers between the crater and the village. | Ring/bolt positions and spacing. |
| `renders/arrows_iso.png` | crop x −10..50, z 130..190, iso 8 px/block | The final-leg orange/white arches and a lime-concrete/black direction arrow. | The arch construction and the arrow marker. |

## Technical / pack evidence

| Image | Source | What it shows | What to take |
|---|---|---|---|
| `renders/xsec_z-255.png` | my cross-section at z = −255 | The glass tunnel in section: 6 water blocks, glass shell, red ribs, planet waterfall rising at x ≈ −91. | The tunnel's exact cross-section and the waterfall junction. |
| `renders/xsec_z-248.png` | cross-section at z = −248 | The planet is a thin ring, not a solid disc. | Do not model it as a solid sphere. |
| `renders/xsec_x-40.png` | cross-section at x = −40 | The planet rim at y ≈ 25–27 and the void below. | The rim is ~2 blocks thick. |
| `renders/pack_pads.png` | extracted pack PNGs | The actual pad textures: yellow 16-frame chevron, red/magenta/pink/purple 7-frame pads, sea lantern 5-frame. | The chevron is the wayfinding device; the pad colours are the mechanic language. |
| `renders/palette_bars.png` | my block table | Top-24 blocks by count with shares. | Water + snow = 58 % of the build. |

## Video frames (A11 owns the video; these are citations only)

| Image | Source | What it shows | What to take |
|---|---|---|---|
| `frames/f_0034_start_line.jpg` | X4l2WSQv0Wk 00:34, copied from `../acerace/fan_recreations/frames/` | Hedge arch + checkered banner + white pen + green floor arrow + yellow chevrons. | Confirms the start/finish gate language (M; fan recreation). |
| `frames/f_0130_finish_approach.jpg` | X4l2WSQv0Wk 02:10, same source | “You are in 1st place” actionbar; survival hearts/hunger; brown path toward the rainbow. | Confirms the static actionbar and survival HUD (M). |
