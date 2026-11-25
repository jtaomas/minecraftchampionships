# Pits — palette, lighting and build techniques

Counts are exact non-air block counts from `blocks.csv` (164,145 blocks, 63 block states, 51 occupied chunks). Shares are of the 164,145 total. Role grouping is analytical; individual block use is in `blocks.csv`.

## 1. Dominant palette

| Material | Count | Share | Role |
|---|---:|---:|---|
| Black stained glass | 34,374 | 20.94% | ceiling 7,216; 10 pit layers 27,090; finish lip 68 |
| Light blue wool | 32,329 | 19.70% | 1-thick perimeter "sky" wall, ≈344 per layer × 94 layers (some gaps) |
| Cobblestone | 22,567 | 13.75% | road slabs, towers, pool floor, park rocks |
| Bricks | 15,650 | 9.53% | building facades, pool tower/walls |
| Gray terracotta | 10,141 | 6.18% | building walls, road shoulders |
| Quartz block | 7,017 | 4.27% | white road dashes, plaza slab, pool rim, trim |
| Light gray wool | 5,954 | 3.63% | light facade bands |
| Brown wool | 5,953 | 3.63% | warm building walls/roofs |
| Brown terracotta | 4,409 | 2.69% | brown roofs/facades |
| Gray wool | 4,225 | 2.57% | building bands, south plaza |
| Shroomlight | 2,709 | 1.65% | hidden pit-floor light grid |
| Black carpet | 2,709 | 1.65% | cover over the shroomlight |
| Light gray terracotta | 2,572 | 1.57% | neutral facades |
| End stone bricks | 2,137 | 1.30% | park path/rock, pool underlay |
| Green concrete | 1,827 | 1.11% | park terrain/canopy |
| Blackstone | 1,653 | 1.01% | road asphalt |
| Red concrete | 1,306 | 0.80% | external banner red |
| Stone bricks | 990 | 0.60% | building trim |
| Green wool | 966 | 0.59% | park foliage |
| Polished diorite | 792 | 0.48% | roof/trim |
| White concrete | 675 | 0.41% | external banner white (3 × 25 × 9) |
| Yellow terracotta | 624 | 0.38% | road edge lines |
| Black wool | 455 | 0.28% | start trench walls, accents |
| Barrier | 340 | 0.21% | 156 start cage + 184 legacy cage |
| Light blue stained glass | 258 | 0.16% | building windows |
| Light blue concrete | 160 | 0.10% | pool-tower bands |
| Red/white/yellow accents | <300 | <0.2% | park flowers/foliage |
| Glowstone | 8 | 0.005% | building lamp pairs |

**Shell + void = 72,121 blocks (43.9%)** of the map (glass 34,374 + wool wall 32,329 + shroomlight/carpet 5,418). The course surfaces are a small part of a large diorama. **[H]** counts; **[M]** grouping.

## 2. Colour logic

| Zone | Palette | Effect |
|---|---|---|
| Roads | blackstone + quartz + yellow terracotta + cobble shoulder | dark asphalt, white 2:1 dashes, yellow edges; high contrast, readable from across the pits |
| Buildings | gray/brown/light-gray wool + terracotta + bricks + stone bricks | muted, warm/cool alternating blocks; windows in black concrete and light blue stained glass |
| Park | green concrete + green wool + end-stone bricks + red sandstone + netherrack + snow + sandstone/quartz stairs | the only green mass; a bright scenic counterweight to the black roads |
| Finish | black glass lip + quartz rim + brick tower + cobble floor | cyan-black band, pale rim, warm brick base; the pool is the visual endpoint |
| External base | red/white concrete panels + brown/gray building columns | branding/thumbnail only, outside the wall |
| Start | quartz floor + black wool walls + gray concrete patch + oak sign | a neutral, dark slot; the green “Start” text is the only bright UI |

The course has **no white concrete** in the roads; the white dashes are quartz. All white concrete is in the external banners. **[H]** (blocks.csv counts/coords).

## 3. Lighting

- **Skylight:** the black-glass ceiling at y=29 is transparent to skylight. Region `SkyLight` is **15** at the start head, both road heads, the finish lip, the chicken height and y=28 just below the ceiling. **[H]** (`light_probe.py`).
- **Daylight:** `doDaylightCycle false`, `DayTime 4328` (noon); the map is permanently day-lit through the ceiling. The black glass tints the view dark/blue but does not darken the light level. **[H]** gamerule + light arrays; **[M]** visual tint.
- **Glowstone:** exactly 8 blocks in two pairs (x30/39, y8, z5-6; x32/41, y12, z65-66), building lamps. **[H]**
- **Shroomlight:** 2,709 blocks at y=-64 under 10 black-glass layers; this is a hidden warm bottom for the pit, not a practical light for the course (the `BlockLight` array sampled at the pit cells is 0, and the glass layers block the view). **[H]** count/coords, **[M]** light contribution.
- **No torches/lanterns/sea lanterns** in the palette. The map relies on skylight + 8 glowstone + the hidden grid. **[H]**
- The renderer ignores in-game light/tint; the blue background in every render is the renderer's sky, **not water or void**. **[H]** tool behaviour.

## 4. Build techniques

1. **Floating city on a lit void.** Three long street islands, building masses, and a black pit below. Some platforms are thin slabs (Road A 6 blocks at x20, tapering to 3 at x25; east plaza 1 block), others are solid towers from y=-64 (Road B, glass lip, pool tower). The mix gives a skyline of solids and plates. **[H]** column probes.
2. **Layered black-glass void.** 10 sheets at y=-62,-60,…, -44, each ~52×52, over a shroomlight+carpet floor at y=-64. The kill plane y=-41 is 3 blocks above the top sheet, so the player never lands on it; the layers read as a deep, faintly lit bottom. **[H]** block counts/coords; **[M]** visual intent.
3. **Road paint as blocks.** Yellow terracotta edges + blackstone asphalt + quartz 2:1 dashes; no decals or custom textures. The 3-block period is exact and repeats for the full 38-block road. **[H]**
4. **Skybox shell.** A 1-thick light-blue wool perimeter + a 1-layer black-glass ceiling. The wool hides the external base and reads as a blue horizon; the glass keeps skylight. ≈344 blocks per layer × 94 layers = 32,329 wool (a few layers have openings). **[H]**
5. **Scenic park.** Green concrete/wool canopy with snow patches, red sandstone/netherrack/polished granite boulders, sandstone/quartz stairs/slabs; 21×30×51 bounding box, y -8..21. It is the only organic mass and the only route to the roofs (via its y5 top). **[H]** geometry; **[M]** route.
6. **External banners.** Three 25-wide red/white panels at z=83 (x29..53, 61..85, 93..117), y -61..-23, outside the play shell. Industry's `block_reset.mcfunction` rebuilds/clones the same coordinates, so Pits inherited the base template. **[H]** coords; **[M]** lineage.
7. **Invisible barrier cage.** 156 barriers form the start gate; a single `/fill … air replace barrier` opens all three exits at GO; three `/fill … barrier` planes rebuild it at reset. No pistons/command blocks. **[H]**
8. **Armor-stand cleanup anchors.** An invisible marker armor stand is spawned 10 blocks above each playing player every second; after a finish, 31³ fills around each marker remove the 8 player concrete colours. No global map reset. **[H]**
9. **Chicken-pool finish.** 20 chickens in a 7×34 sunken pool, Resistance V refreshed each tick; a punch increments `damage_dealt_resisted` and finishes. No arch, no chicken row, no sign. **[H]**
10. **No command blocks, no containers, no redstone.** 6 functions and 164k blocks; the only interactive block is the oak sign. **[H]**

## 5. What to copy / avoid (palette level)

Copy:
- The **road paint** (yellow edge + blackstone + 2:1 quartz dashes) — cheap, readable, no pack.
- The **thin-slab vs solid-tower** mix — gives depth without filling the world.
- The **layered glass void + kill plane above it** — strong depth cue, no fall-to-bottom cost.
- The **light-blue wool + black-glass skybox** — daylight through glass, dark ceiling, no torches.
- The **green park counterweight** — one organic mass makes the grey city readable.

Avoid:
- 43.9% of the map is shell/void; for a gameplay-first map this is a lot of non-playable geometry.
- External banners/base are invisible from the course; they only serve thumbnails.
- The black glass ceiling + light blue walls make the map feel enclosed; spectators can fly out and see the seams.
- The finish pool relies on entities; the block build alone reads as an empty reflecting pool. The chickens are essential.
