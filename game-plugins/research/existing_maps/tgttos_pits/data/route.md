# Pits — route and anatomy

All coordinates are world coordinates. Source world: 1.19.1, extents x -3..117, y -64..29, z -9..83; the playable shell is x -3..80, z -9..80. Confidence: **[H]** observed, **[M]** inferred from two sources, **[L]** guess.

Primary evidence: `renders/route_schematic.png`, `renders/course_top_labeled.png`, `renders/start_top_labeled.png`, `renders/finish_top_labeled.png`, `renders/pit_section.png`; all also copied into `frames/`.

## 1. One-paragraph route

Spawn in a narrow trench at **15,-2,36** facing east. Click the wall sign at **14,-1,36** (line 1). After a 7 s countdown the barrier shell opens. Exit east onto **Road A** (x18..25, top **y=-3**), cross the 7-wide road, then bridge **12 blocks east** over **Pit 1** (x26..37). Drop 4 blocks onto **Road B** (x38..46, top **y=-7**), cross it, bridge **12 blocks east** over **Pit 2** (x47..58), drop 2 onto the **black-glass lip** (x59..60, y=-9), then drop 2 more into the **chicken pool** (x61..67, z19..52, floor **y=-11**) and punch any of the 20 chickens. A fall below **y=-41** teleports you back to the start, not a checkpoint.

## 2. Start

| Item | Value | Confidence |
|---|---|---|
| Spawn (feet) | **15, -2, 36**, yaw **-90** (east) | H (level.dat spawn 15,-2,36; respawn tp yaw -90) |
| Start sign | **14, -1, 36**, oak wall sign facing east, attached to x13 | H (block entity) |
| Sign click | line 1 (blank) `run_command` → `tag @a add starting`; line 2 is green “Start” with no click | H (NBT) / M (per-line click) |
| Cage shell | barrier at **x13/x17** and **z25/z46**, **y -2..1**; 156 barriers | H |
| Cage interior | x14..16 quartz floor at y=-3, x17 black wool floor edge; 3 wide clear, 22 long | H |
| Cage walls | x13 black wool/gray concrete/black wool; sign on x13 | H |
| Exits | east **x17** (22 long), north **z25** (4 wide), south **z46** (4 wide) | H |
| North exit | 2-high corridor z21..24 (air y -2..-1, green wool ceiling y0..2), opens to park at z19-20 (brown-wool floor y=-2) | H |
| South exit | 2-high corridor z47..50, opens to gray plaza at z51 (floor y=-2) | H |
| East exit | after barrier removal, step onto x18 cobblestone at y=-3 | H |

The east exit is the road. The north/south exits are 2-high side passages; they are not the main course, but they give access to the park / south plaza (see §7).

## 3. Road A (west island)

| Property | Value | Confidence |
|---|---|---|
| Extent | **x13..25, z17..54**, top **y=-3** | H |
| Road lane | x19..25 (7 wide): yellow terracotta x19 and x25, blackstone x20-21 and x23-24, quartz centre x22 | H |
| Centre-line pattern | 2 quartz, 1 blackstone, repeat every 3 blocks along z (e.g. z35-36 quartz, z37 blackstone) | H |
| Shoulder | x18 cobblestone; x17 black-wool edge/barrier line | H |
| Slab thickness | 6 blocks at x20 (cobblestone y -9..-4, blackstone y -3); tapers to 3 at x25 (cobble -5..-4, yellow -3) | H |
| Start trench | embedded at x13..17, z25..46, floor y=-3 | H |
| Ends | z17 north, z54 south; beyond are park/buildings; no walk-around at road level | H |

The white “dashes” are not white concrete; they are the quartz centre line interrupted by blackstone. The white concrete in the palette is entirely in the external banners (§8). **[H]**

## 4. Pit 1

| Property | Value | Confidence |
|---|---|---|
| Extent | **x26..37** (12 wide), **z17..54** at the road line; the void continues north/south beyond the roads | H |
| Depth | no floor until the black-glass layers at **y=-44**; shroomlight floor at **y=-64** | H |
| Kill plane | feet **y ≤ -41** → tp to **15,-2,36** | H |
| Walls | west = Road A tapered slab; east = Road B solid tower | H |
| Crossing | 12 concrete blocks from x25 to x38 at y≈-3; landing is 4 blocks lower (Road B y=-7) | H geometry / M play |
| Fall result | ~2 s fall, then start; no checkpoint; timer keeps running | M |

## 5. Road B (middle island)

| Property | Value | Confidence |
|---|---|---|
| Extent | **x38..46, z17..54**, top **y=-7** | H |
| Lane | 9 wide: yellow x38/x46, blackstone x39-41/x43-45, quartz centre x42 | H |
| Centre-line pattern | 2 quartz, 1 blackstone, phase shifted (z17 blackstone, z18-19 quartz, z20 blackstone) | H |
| Base | solid tower from **y=-64** to **y=-7** (cobblestone/bricks) | H |
| Ends | z17/z54; bounded by buildings/park, no walk-around | H |

## 6. Pit 2 and finish

| Property | Value | Confidence |
|---|---|---|
| Pit 2 | **x47..58** (12 wide), z17..54; same depth/kill plane | H |
| Glass lip | **x59..60, z19..52**, top **y=-9**, black stained glass on a solid tower | H |
| Chicken pool | **x61..67** (7 wide), **z19..52** (34 long), floor **y=-11** cobblestone; brick walls | H |
| Chickens | 20 at **y=-10**, spread x61.5..67.6, z19.2..52.6 (not a row); Resistance V, `PersistenceRequired=0` | H |
| East rim/plaza | **x68..79, z17..54**, top **y=-9**, 1-block quartz slab | H |
| Finish trigger | punch any chicken → `hit_chicken>=1` → `finished` tag, spectator, time title | H |
| Reset | 7 s after first finish: all tp to start, barriers rebuilt, adventure, clear | H |

The pool is a long sunken reflecting-pool shape. The approach from the west sees the black-glass lip as a dark band; the chickens are 2 blocks below the lip and are not visible until you are at the edge. **[M]** from geometry.

## 7. Side routes and possible skips

These are geometry observations, not intended-route claims.

1. **North side passage → park.** The 2-high corridor at z21..24 opens onto the park floor at y=-2. The park (x -2..14, z15..56) rises to y=5 and has stairs/slabs; its top may connect to the north building roofs at y5. A roof route could go north, east along the north buildings, then drop to the finish; it is a long detour, and no barrier stops it. **[H]** geometry; **[M]** viability. The park detail render (`frames/10_park_detail.png`) shows the canopy/stairs.
2. **South side passage → south plaza.** z47..50 opens to a gray-wool plaza at z51, then a short corridor to z54; it appears to dead-end at the south building row. **[H]** geometry; **[M]** dead end.
3. **Direct bridge A→finish.** With infinite concrete, a player can bridge straight from Road A x25 to the finish lip x59 (33 blocks) at y≈-3, bypassing Road B. It is a longer, more exposed bridge but not blocked. **[H]** geometry / **[M]** viability.
4. **Pillar/tower.** `CanPlaceOn` includes the road/plaza materials and the placed concrete itself, so a player can pillar up to the y5 roofs. The pickaxe can only break concrete, so the map is otherwise safe. **[M]** mechanics.
5. **No anti-skip barrier or world border inside the shell.** The only enclosure is the light-blue wool perimeter and black glass ceiling; in adventure the pickaxe cannot break them. **[H]**.

## 8. External shell / decoration

| Item | Value | Confidence |
|---|---|---|
| Perimeter wall | light blue wool, 1 thick, x -3..80, z -9..80, y -64..29 (344/layer) | H |
| Ceiling | black stained glass, 1 layer, y=29, 7,216 blocks | H |
| Sky light | SkyLight 15 at road surfaces and up to y=28 below the glass; the glass does not reduce skylight | H (region light arrays) |
| Fake pit bottom | 10 black-glass layers at y=-62,-60,…, -44, 27,090 blocks; shroomlight y=-64 + black carpet y=-63 (2,709 each) | H |
| External banners | red/white 25-wide panels at z=83, x29..53 / 61..85 / 93..117; y -61..-23; outside the wall, not reachable | H |
| Legacy cage | 184 barriers at x63..84, y -33..-29, z9..13, inside a solid brown-wool building; inert | H |
| Glowstone | 8 blocks: x30/39,y8,z5-6; x32/41,y12,z65-66 | H |

## 9. Sightlines / readability

- **Start:** player faces east (respawn tp yaw -90; world SpawnAngle is 0/south, so a fresh join may face south — [H] level.dat, [M] join facing). The sign is west/behind; the east exit is the obvious opening. The road's yellow edges and the dark pit beyond read as “cross east”. **[M]**
- **Road A → Road B:** across Pit 1, Road B is 4 blocks lower and 8 blocks wide; its yellow edge and white centre line are visible across the gap. The finish plaza is farther east, partly hidden by the finish buildings. **[M]**
- **Road B → finish:** the black-glass lip (2 wide) is a dark band; the quartz rim and the long pool are visible; the chickens are below the lip and only appear at the edge. **[M]**
- **Finish:** the long pool is a strong landmark; 20 chickens spread over 34 blocks mean no single choke point. **[H]** entities + **[M]** feel.

## 10. Timing / difficulty

No in-map timing data exists (no checkpoints, no timer hooks besides the per-player stopwatch). Estimates are **[L]**:

| Leg | Distance | Expert | Median |
|---|---:|---:|---:|
| Start → Road A edge | ~10 blocks | 1.5 s | 2 s |
| Bridge Pit 1 (12 blocks) | 12 blocks | 2.5–3.5 s | 4–5 s |
| Road B crossing | 8 blocks | 1.2 s | 1.8 s |
| Bridge Pit 2 (12 blocks) | 12 blocks | 2.5–3.5 s | 4–5 s |
| Lip → chicken | 3 blocks + drop | 1 s | 1.5 s |
| **Total** | ~45–50 blocks | **~9–12 s** | **~14–18 s** |

The map is short; the challenge is the two 12-block bridges over a 38-block drop, not route length. A single fall costs the full fall + run-back (~2–3 s) plus lost bridge progress. **[M/L]**

## 11. Checkpoints

There are **none** in the datapack or the build. `checkpoints.csv` lists the start/finish gates and pits as anatomy rows, not checkpoints. `respawn.mcfunction` always teleports to 15,-2,36. **[H]**
