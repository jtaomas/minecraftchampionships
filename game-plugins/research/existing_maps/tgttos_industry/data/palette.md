# Industry — palette, lighting, build techniques

## Palette (H: counts from `data/blocks.csv`, 122,276 solid blocks, 39 base ids)

The map is a two-material industrial shell with a paint accent. Six materials are 85% of the mass:

| Material | Count | Share | Role |
|---|---:|---:|---|
| smooth_stone | 34,812 | 28.47% | plinth mass, deck underlay, walls |
| smooth_stone_slab | 34,675 | 28.36% | double-slab plinth fill, top/bottom slab trim, roof units |
| blackstone | 13,918 | 11.38% | conveyor "rubber" strips, deck edge, roof |
| polished_andesite | 8,833 | 7.22% | conveyor side rails / frames |
| light_blue_stained_glass | 7,286 | 5.96% | 3 glass fins, 31×105 skylight, curtain windows |
| red_concrete | 4,500 | 3.68% | red paint curtains (z66, z83) |
| yellow_concrete | 3,090 | 2.53% | yellow paint curtain (z74) |

Accents (each < 2.1%):

- **Machine body:** bricks 2,511 (2.05%), magenta_terracotta 1,811 (1.48%), black_concrete 1,115, white_concrete 690, iron_block 28.
- **Deck:** black_wool 1,299, gray_wool 1,266, blackstone_stairs 148.
- **Glass:** light_blue_stained_glass_pane 1,026 (machine windows, catwalk glazing).
- **Catwalk / rail:** iron_trapdoor 834 (y=-20 grating), cobblestone_wall 534, iron_bars 472, red_sandstone_wall 253, red_sandstone 117, red_sandstone_stairs 78, diorite_wall 192.
- **Machine trim:** quartz_pillar 780, oak_fence 351, acacia_planks 340, polished_diorite 326, yellow_wool 258, acacia_trapdoor 127, acacia_slab 115, stone_bricks 108.
- **Functional:** barrier 184 (start box only), quartz_block 60 (start floor), gray_concrete 40 (start sign wall), oak_wall_sign 1.
- **Light:** sea_lantern 9.
- **Misc:** ladder 42, tripwire_hook 8.

No water, lava, leaves, logs, plants, dirt, sand, gravel, copper, or deepslate. The palette is deliberately non-organic. H.

### Colour logic

- **Dark deck / light trim:** blackstone + black/gray wool read as rubber belt; smooth stone + andesite + quartz read as concrete/steel; the white concrete panels are the only high-value surfaces. H from the renders.
- **Paint accents:** red and yellow concrete are the only saturated large surfaces. They are used exclusively for the three curtains; the rest of the map is neutral. This makes the curtains the visual and gameplay focus. H.
- **Two accent temperatures:** red curtains have white window borders/drips; yellow curtains have black ones. The alternating frames distinguish the rows even for colour-blind players. H (block data + `frames/curtain_window_detail.png`).
- **Team concrete overlap:** red and yellow are also two of the ten selectable player colours (red tag → red_concrete; yellow tag → yellow_concrete). The map's own red/yellow curtains are therefore indistinguishable from a red/yellow player's placed blocks. This is a design conflict, not a palette accident. H.

## Lighting (M unless noted)

- **Daylight:** `doDaylightCycle=false`, `DayTime=0` (dawn), weather off. The 31-wide × 105-long glass skylight at y=9 lets sky light into the central hall; the blackstone roof sides shade the machine heads and the side decks. H for gamerules/geometry; M for the resulting light levels.
- **Artificial:** 9 sea lanterns: 3 at y=-25 (x44..102, z11..50 region) and 6 at y=4 (roof units). They are embedded in machine panels and roof boxes, not exposed as lamps. H for positions/count; M for their contribution.
- **No torches, lanterns, glowstone, froglight, campfires, or shroomlights.** The map relies on sky light + 9 sea lanterns. H.
- **Contrast:** the glass fins and the skylight are the brightest planes; the blackstone deck and the machine undersides are the darkest. The curtains are lit from the front by the skylight and read as flat billboards; their window bands glow pale blue. H from the renders; M for exact in-game light values.
- **Fog/void:** the void has no floor/ceiling; the sky is visible around the roof edges. The map is an open-sided shed under a roof, not a sealed interior. H from the iso overviews.

## Build techniques

1. **Conveyor tables (H).** Each deck is a 95-wide × 11–17-deep slab on a solid 27-block stack (y=-61..-35: 21-block plinth + 6-block deck body). The surface is striped in x: 2-wide blackstone + 1-wide gray wool, repeated, with polished andesite side rails and blackstone stair ends. The stripes are cosmetic; the whole top is walkable. The plinth is solid (no interior rooms), and the void between tables is real.
2. **Machine heads (H).** At x40..42 and x104..106, 1–3-block-wide towers rise from the deck to the roof. They combine a white concrete control panel, a sea-lantern button, red sandstone frames/stairs, blackstone bases, glass panes, and brick/magenta terracotta shafts. They are the only "buildings" on the deck and act as visual gateposts.
3. **Glass fins (H).** 1×25×39 light-blue stained-glass planes at x55/73/91, z26..50. They are exactly one block thick and hang through the void into the foundation, so they read as structural fins, not windows. The 3-wide iron-trapdoor catwalk on top (y=-20) is a separate walkway, 15 blocks above the deck; it is not connected to the decks.
4. **Paint curtains (H).** 1-block-thick concrete planes at z66/74/83, 25–26 wide × 39 high, with a 9-high window band (border + glass + diagonal "drip" wedges). The segments are offset in x to force a weave. The walls are mineable (adventure + CanDestroy) and are rebuilt by `block_reset`. Above each wall, 3 blocks higher at y=-20, runs the 3-wide iron-trapdoor catwalk; its white grating and red sandstone stair trim are separate from the red/yellow wall top at y=-23.
5. **Start tray (H).** An 18×3 quartz floor inside a 20×3 tray 1 block above the deck, black wool border, white/black concrete walls, a barrier cage (184 blocks), and one oak wall sign. It is a "starting gate" inserted into the conveyor, not a separate room.
6. **Roof (H).** A 73×105 blackstone slab at y=8–9 over the hall, with a 31-wide glass strip at y=9 and white concrete roof units at y=10. The roof overhangs the building to z=-5 (13 blocks north of the deck), giving the map its long shed silhouette. The skylight grid is visible from above.
7. **Catwalk grid (M).** 702 iron trapdoors + red sandstone supports at y=-20 form three long N-S bridges over GAP A (x54–56/72–74/90–92, z28–48) and three E-W bridge rows in GAP B (z65–67, 73–75, 82–84) in 21-wide segments. The segments have 6–7-block gaps and no connection to the decks; they read as factory walkways, not a playable route. If they were intended as a high road, the access is missing.
8. **No command blocks (H).** There are no command blocks, redstone, pistons, hoppers, chests, shulkers, or item frames. All logic is in the datapack; all containers are absent. The only block entity is the start sign.

## What to copy / avoid (M)

**Copy:** the one-thick glass fin as a lane divider; the alternating x-gaps in a 1-thick wall as a forced weave; the two-temperature window frames; the "conveyor stripe" deck (2 dark + 1 light) for a cheap industrial read; the skylight strip over a dark roof.

**Avoid:** using the same concrete colours for the map and the player kit; hiding the finish in a 4-deep basin; a 1-block-at-a-time refill as the only bridging rate; an unmarked start sign; 98.8% empty chunks.
