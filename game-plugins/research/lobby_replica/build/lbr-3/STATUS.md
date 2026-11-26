# LBR-3 — THE CHAMPS mountain, giant globe, outer terraces, water edge

Section owner: **LBR-3**. Site: `mcc_lobby_official`, new test hub at `C = 5000, 120, 5000`.
No dome, no portal facades, no paths, no portal pads touched. Nothing sits on the island's
top surface; the only blocks below `y = 121` are the mountain's rock skirt (y 90…120).

Layout follows LBR-1's 2026-11-25 correction (mountain at `4880, 5015`; globe at `5000, 5105`
on its own plinth outside the south rim) plus the combined-preview review of 2026-11-25:

* the `THE CHAMPS` panel and the gate moved from the **east** face to the **south** face
  (the Build Mart pad sits between the plaza and the east face — see "Why south" below);
* a deep rock skirt was added under the headland so it reads as a cliff, not a floating slab.

## Bounds

| | x | y | z |
|---|---:|---:|---:|
| mountain body | 4853 … 4907 | 121 … 165 | 4995 … 5035 |
| sign panel / letters | 4861 … 4899 | 130 … 158 | 5036 … 5037 |
| gate porch + shelf | 4863 … 4897 | 121 … 130 | 5036 … 5043 |
| **rock skirt (below ground)** | **4853 … 4907** | **90 … 120** | **4995 … 5044** |
| globe | 4988 … 5012 | 121 … 145 | 5093 … 5117 |
| globe plinth | 4980 … 5020 | 121 … 124 | 5085 … 5125 |
| rim terraces | 4948 … 5052 | 121 … 125 | 5062 … 5092 |
| globe causeway | 4962 … 4996 | 121 … 123 | 5085 … 5098 |
| **whole section** | **4853 … 5051** | **90 … 165** | **4995 … 5124** |

Block total: **28,828 unique placements** (29,571 written, 743 overwritten by later passes).
49 distinct block states, all vanilla Java 1.21.1. 15,939 commands.

## Why the sign is now on the south face

The Build Mart pad is at `4928, 5000` with a 19×19 footprint and a 30-block roofline
(top ≈ `y = 150`). From the central plaza the pad's shadow lands on the east face across
`z ≈ 4987…5013`, i.e. the first two-and-a-half letters of `CHAMPS`.

Raising the panel does not fix that: from a plaza viewer at eye level (`y ≈ 122`, 63 blocks
from the pad's near face) the pad's roofline subtends ≈ 24°, so a sign 92 blocks away would
have to reach `y ≈ 162` — above the 45-block mountain — to clear it. Shifting the panel down
the east face is impossible too: the 39-wide panel already fills the 40-wide face.

On the south face (`z = 5036`) the pad is 27 blocks north of the panel plane, so it is not in
the line of sight from **any** plaza position, and the whole panel clears the pad's silhouette
in the combined iso. The cost is that the panel is seen obliquely from the plaza centre and
front-on from the south (globe platform, terraces, south path) — the direction the reference
photo is taken from.

## Files

| file | what |
|---|---|
| `build_lbr3.py` | the generator — every number in this build comes from this file |
| `blocks.csv` | `x,y,z,block`, absolute world coordinates, sorted |
| `functions/lbr3_*.mcfunction` | vanilla commands, run in the order of `lbr3_all.mcfunction` |
| `lbr3_all.mcfunction` | `function mcc_lobby:<name>` call order |
| `lbr3_preview.mcmdump.gz` | render dump (includes a **preview-only** ground skirt, not in `blocks.csv`) |
| `preview_top.png` / `preview_iso.png` | renders (iso = 2:1, angle 0, scale 5 / top scale 6) |
| `scratch/qa_check.py` | bounds / pad-clearance / command-syntax checks |
| `scratch/heights.py` | ASCII heightfield dump of the mountain |

## Function list

| order | function | blocks | what |
|---:|---|---:|---|
| 1 | `lbr3_01_mountain_mass_a` + `_b` | 11,675 | rock body, quarry wall behind the sign, rounded shoulders |
| 2 | `lbr3_02_mountain_detail` | 711 | snow drifts, summit pinnacles, rock slabs, rim scree, 4 pines |
| 3 | `lbr3_03_gate` | 1,392 | copper/terracotta arch, lit niche, snow roof, rock shelf |
| 4 | `lbr3_04_sign` | 1,436 | maroon panel, `THE CHAMPS` letters, gold crown, buttresses |
| 5 | `lbr3_05_globe_earth` | 1,680 | inner earth: ocean, continents, ice caps |
| 6 | `lbr3_06_globe_shell` | 2,637 | 24-wide glass shell, 8 meridians, latitudes, cloud belt |
| 7 | `lbr3_07_globe_base` | 2,022 | plinth, lantern ring, rocky rim, north pool |
| 8 | `lbr3_08_terraces` | 1,588 | two rim terrace arcs, 3 levels, stairs, pool coping |
| 9 | `lbr3_09_water` | 185 | cascade, pool dressing, causeway to the globe plinth |
| 10 | `lbr3_10_snow_trim` | 8 | snow caps on the terrace lanterns |
| 11 | `lbr3_11_rock_skirt` | 6,237 | headland underside: sheer cliff y 120→104, keel →90, keel floor |

Run order matters: gate (3) before sign (4) so the panel overwrites the porch's roof snow,
and the mass (1) before everything else. Install the files under
`data/mcc_lobby/functions/`.

## Checks run

* Normal build `y >= 121` (0 violations). Underside `y >= 90`: 6,237 blocks, all at
  `y 90…120`, all at horizontal radius **≥ 96 from C** (outside the island rim), so no block
  lands in the island's top surface, its underside, a path, a pad or the causeway.
* 0 blocks inside any of the eight 19×19 portal pads (centres at r = 72).
* All commands are `setblock`/`fill`, all block ids namespaced, no malformed lines.
* Signed text verified to read left-to-right from the south (+z view).

## Palette

Rock: `blackstone`, `polished_blackstone`, `deepslate`, `cobbled_deepslate`, `deepslate_tiles`,
`deepslate_bricks`, `tuff`, `basalt`, `smooth_basalt`, `gray_concrete`, `calcite`, `dripstone_block`.
Snow: `snow_block`, `snow[layers=1..2]`. Sign: `red_concrete`, `nether_bricks`,
`polished_blackstone_bricks`, `white_concrete`, `black_concrete`, `gold_block`.
Gate: `orange_terracotta`, `copper_block`, `exposed_copper`, `cut_copper`, `sea_lantern`, `lantern`.
Globe: `blue/cyan/light_blue/green/lime/white_stained_glass`, `blue_concrete`, `cyan_concrete`,
`green_concrete`, `moss_block`, `lime_concrete`, `sand`, `white_concrete`.
Terraces: `stone_bricks`, `smooth_stone`, `polished_deepslate`, `dark_oak_planks`,
`dark_prismarine`, `sand`, `water`, `lily_pad`, `seagrass`.

## Previews

```
python tools/render_preview.py game-plugins/research/lobby_replica/build/lbr-3/lbr3_preview.mcmdump.gz \
    game-plugins/research/lobby_replica/build/lbr-3/preview_iso.png --scale 5
python tools/render_preview.py game-plugins/research/lobby_replica/build/lbr-3/lbr3_preview.mcmdump.gz \
    game-plugins/research/lobby_replica/build/lbr-3/preview_top.png --top --scale 6
```

## Deviations and gaps

1. **Headland over the rim.** LBR-1 puts the west rim near `x = 4906`; 53 of the mountain's
   55 columns hang west of it, exactly as the correction allows. The new rock skirt turns that
   into a cliff, but the skirt's columns stop at radius 96 so they never intersect the island's
   underside — expect a hairline seam where the two masses meet, around `x 4906`.
2. **South-facing sign.** See "Why the sign is now on the south face". It is front-on from the
   south (globe platform / terraces / south path) and oblique from the central plaza.
3. **Globe centre y = 133, not 132.** With r = 12 a centre of 132 puts the lowest block at 120;
   133 keeps everything at `y >= 121`. Horizontal centre is exactly `5000, 5105`.
4. **Globe plinth is a built platform.** Its south half is over the void, so it is a stone/rock
   quay platform with a rocky rim, lantern ring and a north pool; a flat underside is deliberate
   (the review did not ask for a skirt here). Say the word and it gets the same treatment.
5. **Terraces only occupy the free rim arcs** `56°…82°` and `98°…124°` — the 19×19 pads swallow
   the rest of the ring (their far corners reach r ≈ 85).
6. **Water is one block deep** (raised basin: stone bed `y = 121`, water `y = 122`).
7. **Not built here:** the Decision Dome, the eight portal facades, the hub paths, the island
   terrain/underside, the plaza floor. The 4 spruces on the mountain slopes are integral to the
   silhouette — LBR-4 can replace them.
8. **No server commands were run.**

## Open questions for LBR-5

* Is the island rim really at `r ≈ 94…96` on the west/south-west? The skirt is clipped to
  `r ≥ 96` on that assumption; if the rim is further out, the skirt should be extended inward
  (or LBR-1's underside will overlap it).
* Should the globe platform get a matching rock skirt below `y = 120`?
* The mountain's north side at `z = 4995` is a sheer 30-block cliff; extra boulders at its foot
  would soften it.
