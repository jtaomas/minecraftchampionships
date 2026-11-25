# REFERENCE_IMAGES — MG-PT-01 Parkour Tag

All images are renders of the **source** world (1.17.1) made with A6’s `data/dump_117_mcmdump.py` + the shared `tools/render_preview.py`, or custom plans drawn from the block export. Nothing is a live-server screenshot. **H** for pixel content of renders; the route schematic is **M** (interpretive).

North is up in every top-down view (`-z` up), matching the renderer. Coordinates are absolute.

| # | Image | Made from / bounds | What it shows | What to take | Conf. |
|---:|---|---|---|---|---|
| 1 | `frames/mg_pt_top.png` | `renders/pt_full_top_big.png`; envelope `x 1035..1078, y 0..58, z 972..1017` | Full top-down: the circular bowl, the white/quartz ring, the invisible barrier roof (barrier not drawn), the course inside, the red banner strip at the far west | Envelope, bowl footprint, banner position, course-inside-bowl relationship; **not** detailed enough for tier reading | H |
| 2 | `frames/mg_pt_iso_south.png` | `renders/pt_full_iso_big.png`; same envelope, angle 0 (south) | Stadium from the south: black/gray outer wall, red-trimmed rim, tiered seating, the “PARKOUR TAG” banner floating at the west, the command machine hanging under the bowl | Presentation/venue quality; the banner reads from this side; the bowl hides most of the course from outside | H |
| 3 | `frames/mg_pt_iso_north.png` | `renders/pt_full_iso2_big.png`; same envelope, angle 2 (north) | Back of the bowl: the banner crosses the rim and shows its black back; the interior top (SE spruce grandstand, red/white pads) is visible over the rim | Banner mounting/back face; the bowl is a closed ring, not an open stand | H |
| 4 | `frames/mg_pt_course_top.png` | `renders/pt_course_top_big.png`; `x 1047..1072, y 8..28, z 977..1011` | Course-only top-down at 20 × 20: NW copper L, NE copper ring, centre copper plus, SE brown grandstand, red/white/green pads, NW blue ice, south slime | The plan and the four corner landmarks; use with the height maps for verticality | H |
| 5 | `frames/mg_pt_course_iso.png` | `renders/pt_course_iso_big.png`; same bounds | Course-only iso from the south; the south wall hides the lower interior, but the hanging copper platforms and central mast are clear | The “climbing frame” character: platforms hang off the walls; the floor is deep below | H |
| 6 | `frames/mg_pt_runner_spawn.png` | `renders/pt_runner_spawn_iso.png`; `x 1051..1069, y 13..24, z 976..990` | Runner start room (cyan box) + 4 × 2 door + north landing/stairs, with the NE copper reactor at right | The runner exit: door at `1059..1062, y 17..18, z 984`; the landing/stairs drop to the floor; the room is a dead-end vestibule | H |
| 7 | `frames/mg_pt_hunter_spawn.png` | `renders/pt_hunter_spawn_iso.png`; `x 1051..1069, y 13..24, z 999..1013` | Hunter start room (gray/cyan box) + 4 × 2 door + south landing/stairs, with the SE grandstand at left | The hunter exit: door at `1059..1062, y 17..18, z 1005`; the spruce bench/roof and the SE stairs are adjacent | H |
| 8 | `frames/mg_pt_cutaway_south.png` | `renders/pt_cutaway_iso.png`; arena box `x 1050..1070, y 9..27, z 984..1004`, south (`z 1005`) and east (`x 1071`) walls removed | Interior from the south: copper top structures, central mast, brown spruce grandstand, ground pads, slime/obsidian, blue ice | The four tiers and the hanging platforms; use this to read verticality | H |
| 9 | `frames/mg_pt_cutaway_north.png` | `renders/pt_cutaway_iso2.png`; same cutaway, angle 2 | Interior from the north: the back of the SE grandstand, the central plus, the west/east wall platforms | Top-level perches and the grandstand’s stair profile | H |
| 10 | `frames/mg_pt_surface_low.png` | custom `data/surface_maps.py`; arena `x 1050..1071, z 984..1005` | Lowest standable surface per column, numbered by y (11–25) | The ground floor is continuous at `y 11`; the start rooms are `y 16/17`; the high structures are the only columns without a ground-level surface | H |
| 11 | `frames/mg_pt_surface_high.png` | custom `data/surface_maps.py`; same bounds | Highest standable surface per column, numbered by y | The top level: NW/SW/NE balconies, centre plus, SE grandstand; the start-tower roofs are isolated | H |
| 12 | `frames/mg_pt_route_schematic.png` | custom `data/route_schematic.py`; `x 1048..1072, z 978..1011` | Labelled plan: arena wall, doors, four corner rooms, central mast, ice/slime, blue runner loop, red hunter intercept | The route narrative; **not** an authored route — the loop is traced from the geometry | M |
| 13 | `frames/mg_pt_band_a.png` | `renders/pt_band_a.png`; `y 10..13`, `x 1048..1071, z 978..1005` | Ground/lower band: dead-coral floor, blue ice, slime/obsidian, red/white pads, low walls, first trapdoors | Ground-floor feature map | H |
| 14 | `frames/mg_pt_band_b.png` | `renders/pt_band_b.png`; `y 14..17`, `x 1050..1071, z 979..1010` | Lower/mid band: start rooms, copper ledges, first trapdoor catwalk, stone stairs | Start rooms and the first climb | H |
| 15 | `frames/mg_pt_band_c.png` | `renders/pt_band_c.png`; `y 18..21`, `x 1050..1071, z 979..1010` | Mid/upper band: iron-trapdoor pads, NE copper ring, tower roofs, central stone platform | The mid-level “floor” is a set of pads, not a continuous deck | H |
| 16 | `frames/mg_pt_band_d.png` | `renders/pt_band_d.png`; `y 22..26`, `x 1050..1071, z 984..1005` | Top band: NW/SW/NE brick balconies, centre plus, SE spruce grandstand | The top level is four perches + one stair, not a ring | H |

## QA re-check (2026-11-25, A14)

All **16 cited `frames/` images** were re-opened and compared to their captions; all exist, are readable, and match. A14 resolved **29 citations, 0 missing** for this folder. The `renders/` aliases are byte-identical to the `frames/` copies (hash-checked). No image-based finding changed.

The two guide lecterns at `1049 28 993` and `1049 28 996` are too small to resolve at these render scales; they are confirmed by block-entity NBT, not by an image. They are decorative props on the west gallery walkway, not part of the course.

## What each image does **not** prove

- Images 1–5 are renders, not in-game screenshots; lighting/AO is the renderer’s, not the game’s. **H** render, **M** in-game look.
- The barrier roof is invisible in the renderer; images 1–5 do not show it. The roof is proven by the block export (400 barrier blocks at `y 27`). **H** blocks.
- Image 12 is an interpretive diagram; the arrows are not command output, not a playtest, and not a claim that the map has an authored route. **M**
- The surface maps count **standable** cells (air with solid below and one air above); they do not model stair/slab half-blocks, jump arcs, or player skill. **H** method, **M** playability.
- No image proves the live server state; all are source-world renders. **H**

## Superseded / working images

- A6’s parent-folder `..\renders/pt_top.png`, `pt_top5.png`, `pt_top_big.png`, `pt_iso.png`, `pt_iso_big.png` — first-pass Parkour Tag renders. Superseded by this folder’s `pt_full_*` and `pt_course_*`; kept there as working files.
- `renders/pt_band_*.mcmdump.gz`, `pt_full.mcmdump.gz`, `pt_course.mcmdump.gz`, `pt_cutaway.mcmdump.gz`, `pt_runner_spawn.mcmdump.gz`, `pt_hunter_spawn.mcmdump.gz`, `pt_under.mcmdump.gz`, `pt_machine.mcmdump.gz` — the MCMDUMP inputs. Kept for reproducibility.
