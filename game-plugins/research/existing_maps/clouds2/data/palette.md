# Clouds 2 — block palette, gradients and lighting

Evidence: `data/blocks.csv` (full 171-type table), `renders/palette_bars.png`,
`renders/overview_top.png`, `renders/overview_iso_a.png`, `renders/overview_iso_b.png`,
`renders/tile_*.png`, `renders/pack_pads.png`.

Confidence tags: **H** observed directly in the dump/pack; **M** inference from two
independent observations; **L** reasoned guess.

## 1. Scale

| Quantity | Value | Tag |
|---|---|---|
| Non-air blocks | 150,414 | H |
| Distinct block types | 171 | H |
| Columns with a solid top block | 33,203 | H |
| Loaded chunks | 21,904 | H |
| Block bounds | x −258..66, y −64..319, z −316..291 (325 × 384 × 608) | H |
| Biome | `minecraft:the_void`, `generate_features=false` | H |

The world is a void world: every surface is a built island. There is no natural
terrain horizon, so the sky gradient is the backdrop for the whole course. **H**

## 2. Dominant materials

| Rank | Block | Count | Share | Role | Tag |
|---|---|---|---|---|---|
| 1 | water | 60,002 | 39.89 % | waterfalls, pools, glass tunnel, bubble elevators | H |
| 2 | snow_block | 27,192 | 18.08 % | primary white walkable surface | H |
| 3 | stone | 6,517 | 4.33 % | ruin and terrace structure | H |
| 4 | calcite | 5,565 | 3.70 % | white cloud/terrace surface; also the “clear inventory” pad material | H |
| 5 | cobweb | 4,369 | 2.90 % | cave decor and soft-landing traps | H |
| 6 | sandstone | 3,529 | 2.35 % | desert village walls/floors | H |
| 7 | smooth_sandstone | 3,447 | 2.29 % | village trim, terrace edges | H |
| 8 | barrier | 3,130 | 2.08 % | invisible course bounds, gate, pool rim | H |
| 9 | blue_stained_glass | 2,416 | 1.61 % | water-tunnel shell, portal rings | H |
| 10 | lime_terracotta | 2,152 | 1.43 % | start hedge, garden accents, rainbow band | H |
| 11 | packed_mud | 2,053 | 1.37 % | garden/farm paths, castle bridge | H |
| 12 | grass_block | 1,896 | 1.26 % | garden and farm surface | H |

Top-surface shares (the block a player actually stands on) are different:
snow_block 35.2 %, stone 7.8 %, calcite 5.7 %, packed_mud 5.1 %, grass_block 4.6 %,
smooth_sandstone 4.6 %. **H** (`scripts/top_surface.py`)

## 3. The five palettes

The map is not one palette; it changes palette at every leg. That is the main
wayfinding device after the arrows. **H**

1. **Cloud white** — snow_block, calcite, diorite, white concrete, smooth quartz,
   light gray terracotta. Used for the sky islands, terraces and cloud platforms.
   Stepped gradient snow_block → calcite → diorite gives the islands a lit top and a
   shadowed underside. **H**
2. **Garden green** — grass_block, podzol, moss_block, oak/spruce/birch leaves,
   lime terracotta, lime concrete, green concrete, tall grass, tulips. Used for the
   start garden and farm. **H**
3. **Desert warm** — sandstone, smooth sandstone, sandstone stairs/slabs/walls,
   packed_mud, mud bricks, terracotta (yellow, orange, red, white, light gray).
   Used for the crater village and the bottom-left village. **H**
4. **Water/ice blue** — water, ice, packed ice, blue stained glass, light blue
   concrete, light blue wool, prismarine, sea lanterns. Used for the tunnel, pools,
   the planet and the reef. **H**
5. **Accent set** — lime concrete (start hedge), yellow concrete (direction
   arrows/bolts), orange concrete (crater rim, final arches), black + white concrete
   (checkered gates), red stained glass (reset wall). These are deliberately outside
   the terrain palette so they read as “game furniture”. **H**

## 4. Gradients

- **Rainbow gate at z = 161** — a 7-band vertical arch of terracotta:
  purple → blue → light blue → lime → yellow → orange → red, x ≈ −172..−130,
  apex y ≈ 2, legs down to y ≈ −32. It is the only true multi-hue gradient in the
  map. **H** (`scripts/probe.py -160,161 -170,161 -150,161`)
- **Cloud islands** — snow_block top, calcite shoulder, diorite/stone underside.
  A three-step value gradient that makes the islands legible from below. **H**
- **Desert village** — smooth_sandstone trim over sandstone over packed_mud, with
  terracotta roof accents. A warm three-step gradient. **H**
- **Water tunnel ribs** — red_terracotta rings every 13 blocks around blue stained
  glass. A repeating accent rhythm that measures speed while swimming. **H**

The resource pack flattens every terracotta texture to a 152-byte single colour
(`renders/pack_pads.png`, `data/pack_mapping.csv`). That is why the map can use
terracotta as paint: in vanilla the terracotta patterns would fight the rainbow. **H**

## 5. Lighting

| Light source | Count | Tag |
|---|---|---|
| sea_lantern | 6 | H |
| glowstone / torch / lantern / shroomlight / froglight | 0 | H |

Lighting is therefore **almost entirely skylight**. The map sets `doDaylightCycle=false`
and `time set 200` (morning), and the command banks give every player `glowing`
infinite so racers are visible through geometry. **H**

Consequences:
- No dark sections; the course is readable at all times.
- The only “light” the pack adds is the animated sea lantern texture and the
  emissive look of the animated pads. **M**
- Night vision is not needed, and the map never grants it. **H**

## 6. What the palette does well / badly

**Better than a typical race map**
- Five clearly separated palettes mean the player can name the leg from a screenshot.
- The white cloud base makes the coloured pads and arrows pop at distance.
- Terracotta-as-paint (with the pack) gives clean flat colour at 16×16 with no
  texture noise, which is ideal for a fast race.
- The rainbow gate is a single unmistakable landmark.

**Worse**
- Water is 40 % of all blocks; the many waterfalls dominate silhouettes and can hide
  the next island. **M** (visible in `overview_iso_b.png`)
- The white islands have low internal contrast; without the pads, a snow island and a
  calcite terrace look alike. **M**
- Cobweb is 2.9 % of the map and is used as a soft trap; in 1.21 cobweb slows
  elytra badly if clipped. **L**
- Six sea lanterns is effectively zero artificial light; any indoor/covered section
  (the tunnel, the crater interior) relies on the pack and on skylight holes. **M**
