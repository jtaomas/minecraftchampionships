# Resource islands — where every block is

**Geometry is proposed, not extracted.** MCC's map is not published as a
schematic. The ring order is reconstructed from three wide VOD shots; the
numbers below are a clean layout for *our* rebuild in a 320×320 area:

| Layer | Radius | Y | Contents |
|---|---|---|---|
| Hub | 0–24 | 64 | giant fan, 4 launch pads, 4 brown pads / drop holes, portal in, blueprint gallery in modern versions |
| Lower colour ring | 48–68 | 64 | 16 colour bays, each with two shelf walls (glass + concrete + powder) |
| Upper resource ring | 118–138 | 104 | 8 category islands, each on a 20×20 pad, big neon sign on the outer edge |
| Build area | separate island / instance | 64 | 3 plots + gold plot, 5 colour-coded chests, crafting tables, stonecutters, 2 smelting areas |

Upper-ring order (clockwise from north), **M**: `ores → nether → bricks → logs →
sand → stone → copper → garden`. Evidence: `STONE–COPPER–GARDEN` are consecutive
in VOD-B 2:21:10; `NETHER–BRICKS` (with an ORES pickaxe sign beside NETHER) in
VOD-B 2:21:32 and the first-revision still; `SAND–STONE–LOGS` in VOD-B 2:22:12.
All eight edges close into one ring, but no single frame pans the whole ring, so
the exact angles below are a design choice.

## 1. Upper islands (raw resources)

| # | Island id | Sign | Category | Blocks on the island (MCC 20 poster) | Count | Angle | Centre (x, z, y) | Travel from hub |
|---|---|---|---|---|---|---|---|---|
| 1 | `ores` | ORES | Ores / mineral blocks | redstone block, diamond block, emerald block, gold block, iron block, coal block, quartz block | 7 | 0° | (0, −128, 104) | 7–10 s |
| 2 | `nether` | NETHER | Nether | quartz block, warped wart block, nether wart block, nether bricks, blackstone | 5 | 45° | (91, −91, 104) | 7–10 s |
| 3 | `bricks` | BRICKS | Bricks | bricks, prismarine bricks, mossy stone bricks, stone bricks, nether bricks | 5 | 90° | (128, 0, 104) | 8–11 s |
| 4 | `logs` | LOGS / LUMBER | Lumber | birch log+planks, spruce log+planks, jungle log+planks, oak log+planks, dark oak log+planks | 10 | 135° | (91, 91, 104) | 8–11 s |
| 5 | `sand` | SAND | Sand | sand, sandstone, red sand, red sandstone | 4 | 180° | (0, 128, 104) | 8–11 s |
| 6 | `stone` | STONE | Stone | stone, diorite, andesite, granite | 4 | 225° | (−91, 91, 104) | 8–11 s |
| 7 | `copper` | COPPER | Copper (was Coral ≤ MCC 19) | block of copper, cut copper, oxidized copper, oxidized cut copper | 4 | 270° | (−128, 0, 104) | 7–10 s |
| 8 | `garden` | GARDEN | Garden (was FLOWERS ≤ MCC 19) | lily of the valley, allium, cornflower, oxeye daisy, dandelion, blue orchid, azure bluet, poppy, pink tulip, orange tulip, oak sapling, moss block (+ azalea from MCC 20) | 12–13 | 315° | (−91, −91, 104) | 7–10 s |

Travel band **L**: elytra cruise ≈ 20–30 blocks/s; the fan gives a vertical
launch of ≈ 40 blocks; a 50-block hop between adjacent upper islands is 3–5 s
of flight plus 3–5 s climb/land, so 8–11 s from the hub is realistic. Marked L
because no coordinate-extracted distances exist.

## 2. Lower colour ring (16 bays)

Each bay is one colour and stocks **three forms**: concrete, concrete powder and
stained glass. The in-game subtitle `Gray Concrete Powder (Black Stained Glass)`
(VOD-B 2:22:18) proves powder and glass share a bay. The poster only calls out
"glass and concrete", so powder is the extra form.

| # | Colour | Materials (all three forms) | Angle | Centre (r=56) |
|---|---|---|---|---|
| 1 | White | white concrete / powder / stained glass | 0° | (0, −56, 64) |
| 2 | Light Gray | light_gray … | 22.5° | (21, −52, 64) |
| 3 | Gray | gray … | 45° | (40, −40, 64) |
| 4 | Black | black … | 67.5° | (52, −21, 64) |
| 5 | Brown | brown … | 90° | (56, 0, 64) |
| 6 | Red | red … | 112.5° | (52, 21, 64) |
| 7 | Orange | orange … | 135° | (40, 40, 64) |
| 8 | Yellow | yellow … | 157.5° | (21, 52, 64) |
| 9 | Lime | lime … | 180° | (0, 56, 64) |
| 10 | Green | green … | 202.5° | (−21, 52, 64) |
| 11 | Cyan | cyan … | 225° | (−40, 40, 64) |
| 12 | Light Blue | light_blue … | 247.5° | (−52, 21, 64) |
| 13 | Blue | blue … | 270° | (−56, 0, 64) |
| 14 | Purple | purple … | 292.5° | (−52, −21, 64) |
| 15 | Magenta | magenta … | 315° | (−40, −40, 64) |
| 16 | Pink | pink … | 337.5° | (−21, −52, 64) |

**Seen on camera (H):** White, Gray, Black, Red, Orange, Yellow, Lime, Green,
Blue, Purple, Pink. **Inferred (M):** Light Gray, Brown, Cyan, Light Blue,
Magenta. The angular order is **L/M** — use the material→island lookup, not the
angle, when wiring the data.

Travel: adjacent bay 2–3 s on foot (about 22 blocks), opposite bay 6–8 s around
the ring, or fly over the fan. **L.**

## 3. Hub and build area

| Area | Contents | Role |
|---|---|---|
| Hub | giant fan, 4 launch pads, MCC disc floor, drop holes | vertical launch + landmark; portal from build area lands here |
| Blueprint gallery (P25+) | 7 mini build models on pedestals, teleport pads | build selection; not present in MCC 20 but in current MCC |
| Build area | 3 regular plots + 1 gold plot, 5 colour-coded chests, crafting tables, stonecutters, 2 smelting areas with fuel, portal | spend/craft/smelt and place |

## 4. Duplicates across islands

| Material | Islands | Since | Confidence |
|---|---|---|---|
| Quartz block | `ores` + `nether` | MCC P21 | H (wiki history) |
| Nether bricks | `nether` + `bricks` | MCC P21 | H (wiki history) |
| Stone bricks | `bricks` (mined) + craftable from stone | — | M |
| Bricks | `bricks` (mined) + smelted from clay | — | M |
| Planks | `logs` (both log and planks shelves) | MCC P21 retexture | H (poster shows both) |

No other material appears on two islands. Wool appears on **no** island.

## 5. What changed, event by event

| Event | Change to blocks/islands |
|---|---|
| MCC 14 | Island map debuts: carts → elytra, giant fan, launch/speed pads, Nether + Coral islands added, terracotta removed |
| MCC 17 | Scoring only; no block change |
| MCC P21 | Duplicates allowed: Quartz on Ores + Nether, Nether Bricks on Nether + Bricks; logs retextured |
| MCC 19 | **Snow replaces Sand for this event only** (packed ice, snow block, snow layer) |
| MCC 20 | **Coral → Copper** (copper block, cut copper, oxidized variants); azalea bushes + moss added to Garden |
| MCC 21 | Build Mart **not chosen** — no map change exists for MCC 21 |
| MCC P25 | Blueprint gallery + 3 simultaneous plots + gold plot; block palette unchanged |

## 6. Material → island lookup

Full machine-readable list: `data/blocks.csv` (104 rows). Quick lookup:

| If the build needs… | Go to |
|---|---|
| redstone/diamond/emerald/gold/iron/coal/quartz **blocks** | `ores` |
| quartz, warped/nether wart, nether bricks, blackstone | `nether` |
| bricks, prismarine bricks, mossy/stone bricks, nether bricks | `bricks` |
| any log or plank (5 wood types) | `logs` |
| sand, sandstone, red sand, red sandstone | `sand` |
| stone, diorite, andesite, granite | `stone` |
| copper block, cut copper, oxidized copper | `copper` |
| flowers, tulips, sapling, moss, azalea | `garden` |
| white…black concrete / powder / stained glass | matching `colour_<name>` bay |
| glass, stone, stone bricks, bricks, planks (crafted) | `build_area` crafting/smelting |

## 7. Open questions

- Exact angular order of the 16 colour bays (only 11 labels read; ring wider
  than one frame). **M/L.**
- Whether the standard palette also stocks wool anywhere (poster and all VOD
  frames say no; current presets assume yes). **H that it is absent from the
  poster; M that it is absent in game.**
- Precise regen delay per block (looks like ~2–3 s; current code uses 60 ticks).
  **M/L.**
- Whether prismarine tile is plain prismarine or prismarine bricks. **M.**
