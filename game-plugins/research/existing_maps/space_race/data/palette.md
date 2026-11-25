# Palette, materials and lighting

All counts are exact block counts from the full 296,699-block dump (`data/blocks.csv`).
Percentages are share of non-air blocks. **H** unless marked.

## 1. Headline numbers

| Metric | Value |
|---|---|
| Non-air blocks | 296,699 |
| Distinct block states | 179 |
| Distinct block ids (no states) | ~150 (M) |
| Chunks | 14,198 |
| Footprint | `x -381..261`, `y 0..255`, `z -116..614` |
| Build volume actually used | `x -160..163`, `y 83..255`, `z -116..439` (M: 99% of blocks) |

The map is a **void world**: `WorldGenSettings` uses `minecraft:the_void`, `generate_features=false`
(H `level.dat`). Every visible block is placed by hand. Ten 3×3×12 red-sandstone "asteroids"
float at `y=0..5` as set dressing (`x -381..261`, `z -43..614`; 1,078 blocks) (H).

## 2. Dominant palette

| Rank | Block | Count | Share | Role |
|---|---|---|---|---|
| 1 | gray concrete | 80,932 | 27.28% | main building shell, station, truss/tech |
| 2 | lime stained glass | 65,821 | 22.18% | **lethal cavern floor** (20-layer slab, `y=89..107`) |
| 3 | red sandstone | 52,027 | 17.54% | every asteroid / island / slide bank |
| 4 | water | 27,073 | 9.12% | slides, pools, trident tubes |
| 5 | andesite | 13,740 | 4.63% | basin B, cave rock |
| 6 | stone bricks | 9,177 | 3.09% | main-building detail |
| 7 | sea lantern | 6,831 | 2.30% | light strips, "circuit" murals |
| 8 | light block (level 15) | 5,756 | 1.94% | invisible cavern fill light |
| 9 | white concrete | 4,042 | 1.36% | pod, trim, runway |
| 10 | polished andesite | 2,748 | 0.93% | floors/walkways |
| 11 | yellow concrete | 2,497 | 0.84% | hazard rings, buoy shafts |
| 12 | bedrock | 1,900 | 0.64% | kill hazard / "reactor" |
| 13 | light-blue stained glass | 1,690 | 0.57% | slide walls, sign window |
| 14 | glass | 1,678 | 0.57% | trident tubes, windows |
| 15 | light-gray wool | 1,508 | 0.51% | pod upholstery/accent |
| 16 | smooth stone | 1,351 | 0.46% | trims |
| 17 | lime concrete | 1,249 | 0.42% | start line stripes, accents |
| 18 | gray wool | 873 | 0.29% | accents |
| 19 | orange wool | 821 | 0.28% | accents |
| 20 | black concrete | 814 | 0.27% | portal frames, station |

### Family totals

| Family | Count | Share |
|---|---|---|
| concrete + terracotta (all colours) | 92,690 | 31.24% |
| glass (all colours + panes) | 70,690 | 23.83% |
| red sandstone | 52,027 | 17.54% |
| water | 27,073 | 9.12% |
| andesite family | 13,740 | 4.63% |
| light-emitting blocks | 12,679 | 4.27% |
| stone bricks | 9,177 | 3.09% |
| slabs/stairs/walls/panes | 4,866 | 1.64% |
| wool + carpet | 4,182 | 1.41% |
| bedrock | 1,900 | 0.64% |

## 3. Accents and functional colours

The map uses **material as interface**. This is the most important palette fact:

| Material | Count | Gameplay meaning | Command |
|---|---|---|---|
| diamond block | 130 | elytra grant (4 rings) | `14..20,112,12` + `14,112,14` |
| red concrete | 477 | levitation 23 + speed 30 | `17,112,16` + `18,112,16` |
| orange concrete | 153 | levitation 70 + speed 255 | `19,112,16` + `20,112,16` |
| magenta glazed terracotta | 723 | speed II + `setworldspawn` | `16,112,14` + `11,112,16` |
| bedrock | 1,900 | kill | `21,112,16` |
| lime stained glass | 65,821 | kill (the whole cavern floor) | `23,112,16` |
| red sandstone | 52,027 | clear chestplate | `12,112,12` |
| water | 27,073 | clear chestplate | `12,112,14` |
| gray concrete | 80,932 | clear chestplate | `22,112,14` |

Because the trigger materials are also the build's decorative palette, the effects fire almost
everywhere. A player crossing Platform C (red sandstone) loses elytra; a player brushing any
magenta trim gets Speed II and moves the world spawn (H commands; M consequence).

## 4. Gradients and composition

- **Islands** are layered red sandstone with a 1–3 block irregular rim; no gradient — the
  silhouette does the work. Tops are flat enough to run on but never gridded (H renders).
- **Main building**: near-black shell (gray concrete reads dark in the custom pack), white
  sea-lantern strips as "circuit" murals, a lime-glass floor that glows green from beneath
  (H renders `start_iso_a0.png`, `hall_interior_iso.png`).
- **Tech set pieces** (station, pod, portals): gray concrete + blackstone/black concrete +
  orange/yellow accents + sea lantern edge lighting. High contrast against the warm islands.
- **Hazard language**: yellow/black (yellow concrete + blackstone) is used consistently on the
  trident-ring tops and the elytra-ring surrounds — the one coherent "this is interactive"
  signal in the map (H).
- **Water** is always light-blue glass-edged; you can read a slide from a long way off (H).

## 5. Lighting

| Source | Count | Notes |
|---|---|---|
| sea lantern | 6,831 | strips, murals, island edges, station |
| light block (15) | 5,756 | invisible, `y=108..130`, cavern fill |
| shroomlight | 92 | island F accents |
| glowstone / lava / campfire | 0 | none |

- `doDaylightCycle=false`, `DayTime=18000` (midnight) (H `level.dat`). The map is lit entirely
  by placed light and the skybox.
- `raining=1` at save time; `doWeatherCycle=true` (H). Rain matters because **Riptide** needs
  rain or water (M game rule).
- Fog/void: no barrier walls, no world border beyond the default 60M (H). Falling = void death.
- The resource pack retunes `sea_lantern` (animated, frametime 5) and `magenta_glazed_terracotta`,
  `diamond_block`, `red_concrete`, etc. (H pack listing). Palette reads differently with the pack
  on; see A10's resource-pack dossier for the textures themselves.

## 6. Palette verdict

Coherent and cheap: **three materials carry 67% of the map** (gray concrete, lime glass, red
sandstone). The warm/cool split (orange asteroid vs gray tech) makes the two halves of the course
instantly distinguishable. The weakness is that the same three materials are also the kill/clear
triggers, so the visual language and the mechanical language collide (see `README.md` §6–7).
