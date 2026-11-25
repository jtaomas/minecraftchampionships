# MG-PT-01 — palette and lighting

Total: **8,375 non-air blocks**, **68 block types** in the envelope `x 1035..1078, y 0..58, z 972..1017`. **H**

## 1. Dominant palette

| Rank | Block | Count | Share | Role |
|---:|---|---:|---:|---|
| 1 | gray concrete | 1,849 | 22.08% | bowl shell, gallery deck, command room |
| 2 | cyan terracotta | 1,073 | 12.81% | arena walls, start towers, trim |
| 3 | stone bricks | 708 | 8.45% | arena walls, start-room trim, stairs |
| 4 | smooth quartz | 609 | 7.27% | gallery trim, start-room floor, seating |
| 5 | white concrete | 509 | 6.08% | gallery rim, command floor, pads |
| 6 | dead horn coral block | 441 | 5.27% | main arena floor (pink) |
| 7 | barrier | 401 | 4.79% | invisible roof at y27 (400) + banner support (1) |
| 8 | brick slab | 379 | 4.53% | SW/NE/centre upper perches + NW decorative roof |
| 9 | black concrete | 328 | 3.92% | bowl rim, banner frame |
| 10 | deepslate bricks | 298 | 3.56% | SW/SE room walls, lower ledges |
| 11 | red concrete | 251 | 3.00% | banner letters, small pads, trim |
| 12 | waxed copper family | 451 | 5.39% | copper ledges, pipes, NE reactor |
| 13 | iron trapdoor | 93 | 1.11% | grated catwalk surfaces |
| 14 | stone-brick stairs | 66 | 0.79% | arena climbing steps |
| 15 | chain/impulse/repeating command blocks | 123 | 1.47% | the command machine |

Full machine table: `data/blocks.csv` (68 rows, count, share, category, use, lighting).

## 2. Accents and materials

- **Cyan terracotta** is the identity colour: the arena wall is cyan, the start towers are cyan, and the upper tower caps are cyan. It reads as a cool "aquarium/factory" box against the warm copper. **H**
- **Waxed copper family** (copper 151, exposed 137, weathered 106, oxidised 57) is the route colour: ledges, pipes, the NE reactor ring and the copper north/south door shelves. The four oxidation states give a subtle green→brown gradient without any dyed blocks. **H**
- **Dead horn coral block** is the floor: a muted pink, not a bright coral. It makes the arena floor warm and low-contrast so the cyan walls and copper routes pop. **H**
- **Stone bricks / deepslate bricks** are the structural neutrals: the central mast, the SW/SE room walls, the start-room trim and the stair edges. **H**
- **Red concrete** is used sparingly: the banner letters/frame, the small red pads at `1069..1070, 994..996` (y11), `1065..1066, 996..997` (y12), `1060..1061, 996..997` (y13), and the red glass countdown. **H**
- **Blue ice** (34) and **slime** (8) are the only two "mechanic" colours: a blue NW rink and a green south/centre bounce pad. **H**
- **Obsidian** (15) frames the south slime pad and marks the blast-resistant centre of the floor. **H**

## 3. Gradients

There is no dyed-gradient build. The apparent gradient comes from:
1. copper oxidation (copper → exposed → weathered → oxidised) on the vertical pipes and ledges; **H**
2. the bowl shell's gray concrete base → black concrete rim → red concrete/brick trim → white/quartz seating; **H**
3. the floor's pink dead coral → red concrete pads → blue ice/slime accents. **H**

## 4. Lighting

- **29 invisible `light` blocks** inside the arena and start rooms, at `y 13..23`; they are the only arena lighting. **H**
- **1 glowstone** at `1058 0 982` and **1 sea lantern** at `1064 0 982` in the waiting lobby. **H**
- **4 soul lanterns** at `1058 4 983`, `1060 4 986`, `1065 4 986`, `1063 5 984` in the command room. **H**
- No visible lamps, torches or lanterns in the arena itself; the light blocks make the arena evenly lit but leave the copper/stone undersides in shadow. The bowl interior is open to the sky (no roof above the seating), so daylight also enters. **H**
- `doDaylightCycle=false`; the world time is fixed by the hub, not by this arena. **H** gamerule; **M** time of day at play.

## 5. Palette verdict

The palette is **cheap and coherent**: gray/cyan/white/quartz neutrals + copper + dead coral + red/black accents. It needs no resource pack and no custom models. The risk is **muddy mid-tones**: the copper, dead coral, deepslate and gray concrete are close in value, so the arena reads as a brown-pink mass from the gallery; the cyan wall and the white/quartz gallery are the only strong separators. **M** (render observation).
