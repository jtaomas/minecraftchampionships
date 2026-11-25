# Reference images — TGTTOS Badlands

**Source of every image:** the downloaded world `TGTTOS Badlands - By MickeyKat` (1.19.2), rendered read-only with the shared tool `tools/render_preview.py` (unchanged). Crops were made with the local helper `_crop.py`; labels/sections/height maps with the local helpers `_label.py`, `_section.py`, `_heightmap.py`. **No in-game screenshots exist** — the map ships only a 64×64 `icon.png`. Every image cited in the dossier was opened and inspected; the table below says what it shows and what to take from it.

Tags: **H** = the image directly shows the thing; **M** = partly obscured/interpreted; **L** = orientation/scale guess.

---

## 1. Raw evidence (`frames/`)

These are the unannotated full-resolution renders; identical copies of the same-named files exist in `renders/` but are listed once here.

| File | What it shows | How made | What to take |
|---|---|---|---|
| `frames/map_icon.png` | The world's 64×64 thumbnail: a tiny iso of the island. | Copied from the source world's `icon.png`; opened at native size. | Provenance/cover only; too small for detail. [H] |
| `frames/overview_iso.png` | Full island, 2:1 iso, angle 0, 2000×1012. Start pen at left, finish wall at right, mine wings and mesa in between. | `render_preview.py world.mcmdump.gz` (default angle 0, auto-scale). | Overall composition: the long east–west strip, the mine as the only dense built cluster, the mesa as the high point, the finish as a dark wall. [H] |
| `frames/overview_iso_back.png` | Full island, angle 2 (opposite side), 2000×1454. Shows the back of the finish wall, the rear of the mine wings, and the "MCC" signs. | `render_preview.py --angle 2`. | The MCC lettering is readable; the rear faces are as detailed as the front (no one-sided facade). [H] |
| `frames/overview_top.png` | Orthographic top-down, 827×299, height shading + cliff outlines. | `render_preview.py --top`. | Footprint, the start/finish at the two ends, the black rail line, the mine footprint, the mesa. [H] |
| `frames/start_pen_barriers_iso.png` | Start crop x 0–26, y −47…−34, z 14–47 at scale 22 with `--show-barriers`. The barrier volume is drawn translucent red/purple. | `render_preview.py data/start_pen.mcmdump.gz --scale 22 --show-barriers`. | The pen: black-wool frame, quartz floor, 3 barrier walls + x=8 lip. This is the clearest evidence of the gate. [H] |
| `frames/mine_gate_iso.png` | Mine west gate x 50–92, y −35…−4, z 4–59 at scale 14. The big MCC billboard on its dark frame, white/cyan-striped pillars, chains, sea lanterns, the two white-roofed wings with solar panels and roof cacti. | `render_preview.py data/mine_gate.mcmdump.gz --scale 14`. | The map's best single landmark; the billboard/gate composition. [H] |
| `frames/cave_mouth_iso.png` | Crop x 148–182, y −30…0, z 5–58 at scale 18. **This is a cutaway**: the crop plane at x=182 slices the cave, so the right-hand opening is partly the cut face, not a real mouth. It does show the interior/pillars. | `render_preview.py data/cave_mouth.mcmdump.gz --scale 18`. | The cave interior and white pillar walls; do not read the cut face as an entrance. The real mouth is at x≈150 (air slice). [H/M] |
| `frames/finish_close_iso.png` | Finish x 218–256, y −14…16, z 0–61 at scale 14. Dark gray-terracotta wall, red/white MCC sign on top, white corner towers, chain/lantern details. | `render_preview.py data/finish_close.mcmdump.gz --scale 14`. | The finish wall's front; the sign is 19 long and sits above the wall. [H] |
| `frames/finish_close_iso_back.png` | Same crop, angle 2. The rear of the finish wall: gray terracotta with a red/white zigzag band, the MCC sign readable, the four white towers. | `render_preview.py --angle 2`. | The wall is finished on both sides; the rear has a red/white pattern, not a blank face. [H] |
| `frames/section_z31.png` | Vertical section at z=31, x 0–255, y −64…16, 4 px/block. The whole route profile: start, slope, mine, canyon, mesa, cave void, finish canyon, finish wall. | `_section.py` (custom, reads the MCMDUMP). | The single best route-shape image: the 19-block mesa cliff, the cave void under the roof, the 21-block drop, the finish ledge. [H] |

---

## 2. Overview and route (`renders/`)

| File | What it shows | How made | What to take |
|---|---|---|---|
| `renders/overview_iso.png` | Same as `frames/overview_iso.png`. | — | Composition. [H] |
| `renders/overview_iso_back.png` | Same as `frames/overview_iso_back.png`. | — | Rear faces/MCC. [H] |
| `renders/overview_iso_side.png` | Full island, angle 1, 2000×1037. A side-on view of the strip; the mesa and finish wall overlap. | `render_preview.py --angle 1`. | Height relationship: start low, mine shelf, mesa high, finish mid. [H] |
| `renders/overview_top.png` | Same as `frames/overview_top.png`. | — | Footprint. [H] |
| `renders/route_top_annotated.png` | Top non-air block, 5 px/block, 1580×495, with a cyan schematic centre line and 8 labelled stops (start, mine gate, plaza, black rail, cave, pillars, finish canyon, chicken ledge), 20-block grid, height legend. | `_label.py` (custom; height colour + labels). | The labelled route map. The cyan line is a **schematic centre line**, not a measured speedrun path. [H/M] |
| `renders/route_profile_z31.png` | x–y profile at z=31, 1600×520: brown fill = floor, yellow line = top non-air/roof, shaded leg bands, markers. | `_label.py` (custom). | The vertical story: −43 start, −30 mine, −22 canyon, 0 mesa, −21 canyon, −1 chicken ledge. [H] |

---

## 3. Start

| File | What it shows | How made | What to take |
|---|---|---|---|
| `renders/start_iso.png` | Start crop x 0–45, y −45…0, z 5–60 at scale 8. Black-framed pen, quartz floor, the first slope, red marker stake, small trees. | `render_preview.py data/start.mcmdump.gz --scale 8`. | The pen's visible frame and the slope; barriers invisible here. [H] |
| `renders/start_top.png` | Top-down of the same crop at scale 10. The black rectangle around the white floor; the slope and cacti. | `--top --scale 10`. | The pen is a 5×22 rectangle at the island's west end. [H] |
| `renders/start_pen_barriers_iso.png` | The start pen at scale 22 with the barrier volume drawn. | `--show-barriers`. | The 3 gate walls + the x=8 lip; the quartz floor; the sign block at the west wall. [H] |
| `renders/start_pen_barriers_top.png` | Top-down at scale 20 with barriers. | `--top --show-barriers`. | The gate rectangle: red barrier overlay on x=12, z=20, z=41; the x=8 lip. [H] |

---

## 4. Mine / rail yard

| File | What it shows | How made | What to take |
|---|---|---|---|
| `renders/mine_gate_iso.png` | Same as `frames/mine_gate_iso.png`. | — | The MCC billboard/gate, the white/cyan pillars, the wings, the solar arrays. [H] |
| `renders/mine_iso.png` | Mine crop x 45–115, y −45…5, z 0–65 at scale 10. Both wings, the central plaza, the black "escalator"/rail, the roof cacti, the MCC sign. | `render_preview.py data/mine.mcmdump.gz --scale 10`. | The two-wing plan and the open plaza; the player runs between the wings. [H] |
| `renders/leg1_iso.png` | x 35–105, y −45…0, z 5–60 at scale 6. The first slope, the marker stakes, the first rail strip, the mine edge, the MCC sign. | `render_preview.py data/leg1.mcmdump.gz --scale 6`. | Transition from natural slope to the mine; the MCC sign as the first landmark. [H] |

---

## 5. Canyon / mesa / cave

| File | What it shows | How made | What to take |
|---|---|---|---|
| `renders/canyon_iso.png` | x 105–165, y −40…10, z 0–65 at scale 10. The mine edge, the grey canyon, the black rail/wall, the two white cave pillars in the distance, the mesa. | `render_preview.py data/canyon.mcmdump.gz --scale 10`. | The black rail obstacle and the rise to the mesa. [H] |
| `renders/mesa_iso.png` | x 95–190, y −45…15, z 5–70 at scale 4. The mine, canyon, mesa, and the cave pillars. | `render_preview.py data/mesa.mcmdump.gz --scale 4`. | The mesa as a wall between the canyon and the finish; the cave as an interior. [H] |
| `renders/plateau_iso.png` | x 160–232, y −25…10, z 0–65 at scale 8. The mesa, the natural arch/window, the lower shelf with cacti, the start of the finish canyon. | `render_preview.py data/plateau.mcmdump.gz --scale 8`. | The mesa top and the cave window; the drop to the finish side. [H] |
| `renders/cave_iso.png` | x 145–205, y −30…8, z 0–62 at scale 9. The mesa block with the cave interior/pillars exposed; the roof; the canyon. | `render_preview.py data/cave.mcmdump.gz --scale 9`. | The cave as a west-facing chamber under the mesa roof; the pillars inside. [H/M] |
| `renders/cave_mouth_iso.png` | x 148–182 at scale 18. **Cutaway** through the chamber; shows the pillars and the void; the right-hand opening is the crop face. | `render_preview.py data/cave_mouth.mcmdump.gz --scale 18`. | Interior detail; not a true mouth. [H/M] |
| `renders/cave_top.png` | Top-down of the cave crop. Only the mesa roof is visible; no opening from above. | `--top --scale 10`. | The cave is fully roofed; the route over the mesa is a roof run. [H] |

The **air slices** used to prove the cave is a dead end are not images; they are the console tables in the working log and are summarised in `data/route.md` §5. The key facts: open at x=150, solid at x=195, roof at y≈−3…0, floor at y≈−22…−20. [H]

---

## 6. Finish

| File | What it shows | How made | What to take |
|---|---|---|---|
| `renders/finish_iso.png` | x 185–256, y −25…10, z 0–70 at scale 8. The finish canyon, the dark wall, the MCC sign, the white towers, the chickens' ledge (chickens are entities and are not drawn). | `render_preview.py data/finish.mcmdump.gz --scale 8`. | The finish as a wall at the end of the canyon; the sign reads from the canyon. [H] |
| `renders/finish_top.png` | Top-down of the finish crop. The long wall/sign footprint, the white towers, the ledge strip. | `--top --scale 8`. | The gray wall is 45 long in z (z 9–53); the sign is 19 long; the ledge is 1 wide. [H] |
| `renders/finish_close_iso.png` | x 218–256, y −14…16, z 0–61 at scale 14. The wall, sign, towers, chains, the ledge/fence at the base. | `render_preview.py data/finish_close.mcmdump.gz --scale 14`. | The wall/sign composition and the ledge at its foot. [H] |
| `renders/finish_close_iso_back.png` | Same crop, angle 2. The rear of the wall with the red/white zigzag band; the MCC letters readable. | `--angle 2`. | The finish is finished on both faces; the rear pattern is a red/white band. [H] |
| `renders/finish_close_top.png` | Top-down of the finish crop at scale 12. The long dark wall, the white/red sign band, the two end towers, the ledge. | `--top --scale 12`. | The plan: a linear wall with a central sign and a 1-wide ledge on the west face. [H] |
| `renders/finish_ledge_iso.png` | x 231–250, y −12…6, z 4–59 at scale 20. The wall base, the sign, the white towers, the 1-wide ledge and its fence posts. | `render_preview.py data/finish_ledge.mcmdump.gz --scale 20`. | The ledge/fence geometry at the base; chickens not drawn. [H] |

---

## 7. Sections and maps

| File | What it shows | How made | What to take |
|---|---|---|---|
| `renders/section_z31.png` | Vertical section at z=31 (the centre line): start, slope, mine, canyon, mesa + cave void, finish canyon, finish wall. | `_section.py`, 4 px/block. | The route's vertical profile and the cave/roof relationship. [H] |
| `renders/section_z0.png` | Section at z=0 (north edge): the low shoulder and hoodoos, the mine, the finish towers. | `_section.py`. | The north edge is lower but broken; the mesa is not a clean ramp here. [H] |
| `renders/section_z8.png` | Section at z=8: the mine wings, the north shoulder, the finish. | `_section.py`. | The mine's north wing and the shoulder terrain. [H] |
| `renders/section_z15.png` | Section at z=15: through the mine plaza, the canyon, the cave, the finish. | `_section.py`. | The plaza floor and the cave chamber. [H] |
| `renders/section_z45.png` | Section at z=45: through the south side of the mine/cave. | `_section.py`. | The south wing/cave wall. [H] |
| `renders/section_z60.png` | Section at z=60 (south edge): the sparse southern rim, the mesa, the finish. | `_section.py`. | The south edge is not a continuous path; the island narrows. [H] |
| `renders/heightmap_top.png` | Top non-air y, 2 px/block, colour ramp + 10-block grid + legend. | `_heightmap.py`. | The overall height distribution: high mesa at x 150–200, low start/finish. [H] |
| `renders/surface_top.png` | Top block material map, 2 px/block. Orange = terracotta/red sand, grey = stone/concrete, white = built white terracotta/quartz, green = cacti/leaves. | `_heightmap.py`. | The built footprint (white/grey) vs the natural terrain (orange). [H] |

---

## 8. Images that do **not** exist

- No in-game screenshots (the world has no `screenshots/`).
- No video frames (no video belongs to this map; A11 owns the Clouds 2 video).
- No resource-pack textures (the map has no resource pack).
- No entity renders: the renderer draws blocks only, so the 27 chickens are **not** in any image. Their positions/effects are in `data/entities.csv`; the fenced ledge they stand on is visible in the finish images. [H]
