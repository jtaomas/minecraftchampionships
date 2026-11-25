# Reference images

No video exists for this map, so every image is a **render of the source world** (no timestamps).
Each entry: path → how it was produced → what it shows → what to take from it.

`frames/` holds byte-identical copies of the most important renders (verified by SHA-256); it is a
convenience set, not a second source.

Renderer: `C:\Users\LeonS\Desktop\Mcc\tools\render_preview.py` (read-only, unmodified).
Dumps: `scripts/dump_mcmdump.py` (NumPy reimplementation of the same MCMDUMP format, because the
shared `world_to_mcmdump.py` per-block loop is too slow for 14k chunks).

---

## Overview

### `renders/overview_iso.png`
- Source: full world, `--scale 3`, iso angle 0.
- Shows: the whole course as one diagonal chain: black main building (right/top), orange asteroid
  chain (centre), two ring platforms, the right-hand station chain at bottom-right, and the ten
  decorative 3×3 asteroids.
- Take: the map is **one long line**, not a circuit in plan; the loop is closed by flight, not by
  geometry. The main building is by far the largest object and reads as a landmark from anywhere.

### `renders/overview_iso_a2.png`
- Source: full world, `--scale 3`, iso angle 2 (opposite side).
- Shows: the start pod and its runway on the near side; the west arch and the first slide.
- Take: the pod is the only "front door" and the only part of the building with a human scale.
  The rest of the building is a featureless black volume from outside.

### `renders/route_overlay.png`
- Source: top-down material map (2 px/block, `x -190..240`, `z -150..470`) with the reconstructed
  lap drawn on top (`scripts/route_overlay.py`).
- Shows: 17 numbered stops from START/FINISH through the cavern, Elytra Rings A/B, Pool Islands,
  water slide, bridge, Platform C, Trident Rings, launch pad, Elytra Islands, orange mega pad,
  right-chain turn and the return glide.
- Take: the only single image that explains the whole map. Dotted = launch/glide, solid/dashed =
  measured geometry. It is a **reconstruction**, not a game-provided route (M).

---

## Start / finish

### `frames/01_start_pod.png` = `renders/pod_iso.png`
- Source: `x -2..46, y 100..140, z 10..60`, scale 1, iso angle 0, 10 px/block.
- Shows: checkered start grid, three magenta pads, two gold "engine" props, the glass gate strip,
  the sloping quartz runway, and the green glass floor of the main building behind.
- Take: this is a **start line**, not a lobby. The checklist floor + gate + pads are the ritual.

### `frames/02_start_gate_redstone.png` = `renders/pod_iso_a2.png`
- Source: same crop, iso angle 2.
- Shows: the underside of the pod, the redstone circuit (repeater loop + red block) on the south
  edge, the blue skylight pool in the roof, and the runway arrow pointing away from the gate.
- Take: the countdown clock is physically here; the arrow contradicts the spawn facing (F5).

### `renders/section_x17.png`
- Source: vertical section at `x=17`, `z -130..80`, `y 95..230`.
- Shows: pod floor `y=114`, gate height, hall floor at `y 98..108`, hall roof `y 217`, the
  floating yellow "propeller" at `z -56..-40`, and the water tank at `y 160..192`.
- Take: the pod sits **7 blocks above** the hall floor and the hall is 110 blocks tall — the
  vertical scale is the point of the map.

---

## Luminous Cavern

### `renders/hall_interior_iso.png`
- Source: hall interior `x -60..170, y 100..200, z -120..40`, roof cut off, scale 2, angle 0.
- Shows: the black shell, white sea-lantern "circuit" murals, the big pool with white rim, the
  green glass floor, and the props (barrels/plants) in the west corner.
- Take: the cavern is a **room**, not a corridor. It reads as a lounge until you notice the floor
  is lime glass and the command kills on it.

### `frames/03_cavern_catwalk_plan.png` = `renders/hall_catwalk_plan.png`
- Source: topmost block in `y 118..168`, `x -70..90`, `z -90..10`, 8 px/block, with labels.
- Shows: the two catwalk arches springing from `z≈0`, and all seven red levitation pads
  (`buoy1..5`, `pad40`, `pad60`) sitting on them at increasing heights.
- Take: the intended ascent is **arch → pad → arch**, not floor-walking. The pads are spaced 6–10
  blocks apart vertically, which matches the 48-block levitation budget (F12).

### `frames/04_elytra_ring_A.png` = `renders/archA_iso.png`
- Source: `x -90..-10, y 140..210, z -80..-10`, scale 1, 8 px/block, angle 0.
- Shows: the 7×11 diamond ring set into the west wall, with the two yellow/black "eye" rings of
  the wall mural beside it and a small gray platform outside.
- Take: the exit is a **1-block-thick hoop 50 blocks above the floor**; from inside it is
  invisible until you are on the catwalk. The wall mural deliberately camouflages it.

### `renders/eastdeck_iso.png`
- Source: `x 95..170, y 175..225, z -90..30`, angle 2.
- Shows: the east catwalk/deck, the white lit panel, the black/white walkway, and the orange trim.
- Take: there is a **second exit** (Ring C at `x=88, y 205..215`) with no sign. A first-time
  player has no way to know which exit is the route (F10).

---

## Elytra Ring B / west platform

### `renders/west_start_iso.png`
- Source: `x -200..-40, y 130..230, z -130..50`, scale 2, 5 px/block.
- Shows: the floating west platform (black, magenta pads, white stairs) and, at left, the diamond
  Ring B floating free in the air, plus the hall's west wall at right.
- Take: Ring B is a **free-floating hoop** with no walkable approach — you must arrive by
  levitation from the platform's red pad. This is the clearest "tuned jump" in the map (M).

---

## Pool Islands / water

### `frames/05_pool_island_slide.png` = `renders/islA_iso.png`
- Source: `x -175..-95, y 140..215, z 75..130`, scale 1, 8 px/block, angle 0.
- Shows: Pool Island A — red sandstone asteroid, recessed pool, and the black/green **portal ring**
  where the water slide leaves.
- Take: the portal is the only "gate" that is not diamond; it frames the slide mouth and reads as
  a destination from the north.

### `renders/islA_iso_a2.png`
- Source: same, angle 2.
- Shows: the pool from the other side, the rim at `y≈176`, and the slide rising above the pool.
- Take: the pool is 16 deep; the rim is the safe landing, the pool is a trap (M).

### `renders/section_x-141.png`
- Source: vertical section at `x=-141`, `z 80..200`, `y 140..200`.
- Shows: pool surface `y=160`, the slide at `y=177`, and the 17-block step between them.
- Take: the slide is **above** the pool — the pool is a catch basin, not the route.

### `renders/islB_iso.png`
- Source: `x -175..-85, y 140..215, z 150..255`, scale 1, 8 px/block, angle 0.
- Shows: the andesite basin, its big pool, the black portal on the south rim, the incoming slide
  (top right) and the outgoing bridge (bottom right).
- Take: B is a **junction**: two water inputs and one output. Colour and level alone tell you
  which way to go (M).

### `frames/06_blank_sign_wall.png` = `renders/signwall_clean_iso.png`
- Source: `x -150..-132, y 168..184, z 168..184`, water and light-blue glass removed from the dump,
  scale 1, 22 px/block.
- Shows: the 21 acacia wall signs in a cross-shaped array set into the glass window, at the slide
  mouth above Pool Island B.
- Take: this is the map's **only scoreboard surface** and it is completely blank. It proves the
  map expected a leaderboard system that is not in the world (F8).

### `renders/water_map.png`
- Source: plan of water blocks `y 140..200` plus diamond/bedrock/pad overlays, `x -180..180`,
  `z -130..450`.
- Shows: every water body, slide, tube and pool in one image, with pad/diamond/kill overlays.
- Take: the water network is a single connected southbound chain from A to F; nothing flows north.

---

## Trident Rings / Platform C

### `frames/07_trident_rings.png` = `renders/rings_iso.png`
- Source: `x -70..10, y 160..260, z 190..350`, scale 1, 8 px/block, angle 0.
- Shows: Platform C with its water channel and gray landing strip, the walkway south, the two
  circular platforms with **glass water tubes** and yellow/black hazard crowns, and the red pad at
  the exit.
- Take: the strongest single set piece. High contrast, readable from distance, and the only place
  where the "yellow/black = interact" language is unambiguous.

### `renders/course_plan.png`
- Source: topmost block `y 140..270`, `x -180..170`, `z 60..450`, 5 px/block, labelled A–I.
- Shows: the whole left chain in plan with its water blue against red sandstone, plus the right
  chain at the right edge.
- Take: the two chains are visibly **separate**; only the orange pad bridges them (F11).

---

## Elytra Islands / finish

### `renders/islF_iso.png`
- Source: `x -75..55, y 160..215, z 345..445`, scale 1, 7 px/block, angle 0.
- Shows: the Elytra Islands — two pools, a red pad on a small platform, a green accent, the white
  ring at `x≈40 z≈410`, and the orange 3×3 pad at the east tip.
- Take: the island is an **on-foot section** (red sandstone clears elytra). The orange pad is the
  only way out and points east, not north.

### `frames/08_orange_mega_pad.png` = `renders/finish_left_iso.png`
- Source: `x -10..70, y 160..230, z 380..440`, scale 1, 8 px/block.
- Shows: the east tail of the Elytra Islands and the orange pad with white corner markers.
- Take: the pad is deliberately framed by white corners — the only pad in the map with a
  built frame. It is the launch to the right chain.

### `frames/09_space_station_chain.png` = `renders/right_chain_iso.png`
- Source: `x 85..165, y 175..265, z 235..450`, scale 1, 7 px/block, angle 0.
- Shows: the space station with dish/antenna, orange and magenta trim, the three descending
  islands with white vertical panels, and the floating diamond Ring D at top right.
- Take: the right chain is a **climb northbound** (islands rise toward the station). The ring is
  separate from the station and reads as a hoop to fall through — the return-glide gate.

### `renders/signwall_iso.png`
- Source: `x -160..-125, y 160..190, z 160..190`, scale 1, 12 px/block, angle 1.
- Shows: the outside of the sign window and the slide's red ribs.
- Take: from outside, the sign wall reads as a plain glass panel; the signs only appear once the
  water/glass is stripped (see `06`).

---

## Grids / material maps

### `renders/heightmap_grid.png`
- Source: topmost block per column, 2 px/block, 64-block grid, cyan = command blocks, yellow =
  signs, coloured dots = pads/diamond/orange/red.
- Shows: the whole world footprint and the position of the only control room and the only sign.
- Take: every interactive object is in a 30-block radius of spawn except the four rings and the
  pads; the rest of the map is inert geometry.

### `renders/start_floorplan_floor.png`
- Source: topmost block `y 108..118`, `x -60..170`, `z -125..70`, 6 px/block.
- Shows: the hall's south wall, the start room, the gate line, the three pillars, and the pod.
- Take: the start is a **single-file door** into a 140-block-wide room; the scale jump is extreme.

### `renders/hall_catwalk_plan.png`
- Source: as `frames/03`.
- (listed above)

### `renders/signwall_front.png`
- Source: elevation of the `z=176` plane, `x -150..-132`, `y 170..182`, 24 px/block.
- Shows: the sign/glass window as a coloured blob; the block renderer does not resolve sign faces.
- Take: use `06` for the readable view; this one is only useful for the exact plane.

### `renders/start_pod_plan_y100_130.png`
- Source: topmost block `y 100..130`, `x -5..45`, `z -10..75`, 10 px/block.
- Shows: the pod layout — gate at `z=24`, spawn at `z=27`, blue arrow on the runway `z 32..60`.
- Take: the arrow points **south**, away from the gate and away from the building (F5).

---

## External reference (not shipped in this folder)

- `https://www.planetminecraft.com/project/space-race-mcc-ace-race-recreation/` — the author's
  listing (Agnor). Gallery captions used as corroboration: "StartFinish Line", "Trident Rings",
  "Pool Island", "Water Features", "Luminous Cavern", "Elytra Islands", "Main Building Entrance".
  Page was HTTP-403 to direct fetch on 2026-11-24; captions came from the search index. Treat as
  **M**, not H. No third-party images were copied into this dossier.
