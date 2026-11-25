# Yeti Set Go — route

World: `YetiSetGo` / live `mcc_yetisetgo` · 1.19.2 (DataVersion 3120) · content bounds **x −182..77, y −64..46, z −140..77** · 28,533 non-air blocks.

Coordinates are world block coordinates. `y` is the **block the player stands on** unless stated. Confidence tags: **H** = directly observed in blocks/NBT, **M** = two independent observations agree, **L** = inference.

## 0. Shape of the course

The build is one connected ribbon of floating islands in a void world (`minecraft:the_void`, level.dat). The start/finish is the train-engine platform at **x 10..32, z −13..7, floor y −2**. The start line is the checkered gate plane **z = 7** (glass panes at y −1, barrier at y 1, x 16..26). The button at **(21, −1, 6)** starts the countdown.

The route is a **single lap that returns to the same gate** (M; all sections form one closed chain — see README §2). Direction from the gate is **south** (through the village), then west, then north, then back east to the gate. On the top-down this is anticlockwise.

Two lines split at the south-west plaza (x ≈ −35, z ≈ 35) and rejoin at the north landing:

| Line | Ground route | Launch | Evidence |
|---|---|---|---|
| **Primary — station** | plaza → ice cave → bubble tower → station | station lodestone, y 36 | H for geometry; M for intent |
| **Alternate — pool/lodge** | plaza → chalet stair → pool → bridge → lodge roof | lodge lodestone, y 35 | H for geometry; M for intent |

Both lines end with an elytra flight to the **north snow chain**, then a southbound run through the tunnel and a riptide up the waterfall to the finish gate. The station line is called primary here because it carries the **only explicit checkpoint pad** (setworldspawn at −155, 35, 27) and the elytra command sits in its chunk (H). The pool/lodge line is fully built but has no checkpoint (H) and its elytra pad only works at simulation distance ≥10 (M).

## 1. Start — train engine platform (H)

| What | Where | Notes |
|---|---|---|
| Spawn | 21, −1, 4 | `level.dat` SpawnX/Y/Z (H) |
| Platform floor | y −2, x 10..32, z −13..7 | gray concrete; white concrete stripes at z 6 (H) |
| Side lanes | y −1, x 11..14 and x 28..31 | lodestone strips; one block above the floor (H) |
| Start button | 21, −1, 6 | polished blackstone floor button, facing south (H) |
| Gate | z 7, x 16..26, y −1..1 | y −1 = black/white stained-glass panes; y 1 = 11 barrier blocks (H) |
| Gate posts | x 15 and x 27 at z 7 | lime concrete (H) |
| Countdown circuit | x 11..22, y −4..−3, z 3..6 | 10 repeaters, 13 redstone wire, 5 impulse command blocks, 2 chain blocks (H) |
| Kit | hotbar 0 | trident + Riptide I + Unbreaking III (H) |

The saved world has the gate **closed** (11 barrier blocks + 11 panes at z 7; H). The `fill 16 −1 7 26 1 7 air` command block has `SuccessCount = 0` in the saved NBT (H), so the last recorded run cleared nothing; the saved state is a pre-race state. On a fresh load the button should clear the gate (M — the fill is syntactically valid, 11×3×1 = 33 blocks).

Countdown order from `LastExecution` ticks (H):

1. 1083056 — `fill … air` (gate opens)
2. 1083040 — subtitle `>1<`
3. 1083024 — subtitle `>2<`
4. 1083008 — title `Starting In` + chain `>3<` + chain `gamemode adventure`
5. 1083006 — trident

This is **reversed** from a normal countdown: the gate opens 2.4 s before “Starting In”, and the 1/2 subtitles are set before any title, so only `>3<` is visible (M). The `gamemode adventure` commands fail (`LastOutput` = `permissions.requires.player`, H), so players stay in the world’s creative mode unless the server sets adventure.

## 2. Leg 1 — village U (H)

Exit the gate south. The dock is spruce planks at z 8..9 (y −2), then a boat basin of water at z 11..19, y −4..−3 (x 11..28). A decorative boat build sits at x 6..12, z 12..16. The race path is the **U-shaped gray/ice path** around the basin:

| Segment | Centreline (x, z) | Surface y | Pads / detail |
|---|---|---|---|
| East descent | 27..30, 8 → 40 | −2 | no yellow pad; blue concrete markers at x 22..23, z 37..38 |
| Basin west edge | 11..17, 20 → 33 | −2 | yellow Y23 at x 11..17, z 32..33 |
| South turn | 26, 40 → 20, 60 | −2 to −3 | yellow Y26 at x 12..13, z 54..55; jump pad at x 15, z 49 |
| Bottom | 10, 64 → −3, 69 | −2 | yellow Y28 at x 7..9, z 65..67; Y29 at x −1..1, z 70..72 |
| West return | −13, 62 → −16, 50 | −2 | yellow Y27 at x −14..−6, z 58..66; Y25 at x −31..−21, z 36..39 |
| Plaza entry | −19, 44 → −25, 39 | 0 | lime jump pad at x −31..−29, z 30..32 |

The water channel inside the U is at x −31..−8, z 42..62, y −4 (a 2-deep decorative/boat channel, H). A yellow speed pad here is also a **global spawn checkpoint** because the start command `if block ~ ~−1 ~ yellow_glazed_terracotta run setworldspawn` fires for every yellow pad under any player (H).

Sightline: the U is open to the sky; the train engine is the only tall object behind, the pool cliff and its waterfall are ahead/left, the chalet is the next landmark.

## 3. Leg 2 — south-west plaza and chalet (H/M)

From the west return the path opens into a gray plaza at **x −45..−25, z 30..44, y −2..4**. It contains:

- the chalet at **x −46..−35, z 19..32, y −2..10** — mangrove/spruce house with a quartz spiral stair, lanterns, campfire, red/white detail (H);
- the “cannon” platform at **x −60..−50, z −2..15, y 0** — a black barrel on a stone mount, aimed at the pool; decorative side branch (H);
- lime jump pad at **x −31..−29, z 30..32, y −2** (H);
- dirt/coarse-dirt plaza floor with spruce-slab edging (H).

This is the **split**. The station line continues west into the ice cave; the pool line climbs the chalet stair to the pool.

## 4. Leg 3A — station line (H)

### 4.1 Ice cave

A covered hall at **x −55..−123, z 14..44**, floor **y 1..4**, walls of stone/andesite/ice with a few lanterns and campfires (H). The floor is a speed-pad slalom:

| Pad | x | y | z |
|---|---|---|---|
| magenta jump pad | −65 | 3 | 30 |
| yellow strip | −76 | 3 | 19 |
| magenta jump pad | −76 | 3 | 41 |
| magenta jump pad | −94 | 3 | 30 |
| magenta jump pad | −107 | 3 | 43 |
| magenta jump pad | −108 | 4 | 19 |
| magenta jump pad | −122 | 21 | 2 |

The pads are 3×3 (4 pink corners, 4 red edges, 1 magenta centre). The cave ends at the **bubble-column tower** at **x −129..−127, z 29..32** (H).

### 4.2 Bubble tower

A 3×4 vertical water shaft, **y 4..27**, inside a wood/glass frame; `bubble_column` blocks at y 4..27, water at the top y 28..30 (H). Bubble columns lift the player. The tower top opens onto a short stair/bridge at **y 27..29** that leads west to the station’s lower balcony.

### 4.3 Station checkpoint

The station is a hollow mangrove/spruce train car on a mangrove-plank pillar at **x −171..−130, z 24..53** (H).

| Part | Coordinates | Notes |
|---|---|---|
| Lower balcony | y 29, x −147..−135, z 36..42 | yellow Y24 at x −147..−143, z 36..38; arrows sign at −135, 29, 42 |
| West lodestone platform | y 36, x −171..−155, z 28..34 | **100 lodestone blocks** (elytra equip); yellow pad x −168..−166, z 29..33; red/white cross marker at x −182 (H) |
| Train-car floor | y 36, x −155..−135, z 28..34 | spruce/mangrove runway through the hollow car; roof y 41 (H) |
| Command wall | x −155, y 30..35, z 27 | 15 repeating blocks stacked 6 high (H) |
| Control signs | x −153..−149, y 30..31, z 27 | 7 empty warped wall signs (H) |
| Hopper | −149, 33, 27 | empty (H) |
| Lever | −143, 31, 27 | powered=true (H) |
| Roof | y 41, x −141..−130, z 27..42 | spruce/mangrove, black beams (H) |
| East entry | x −135..−126, y 29..36, z 28..34 | bubble-column side / lower-balcony access; east car wall at x −135 (H) |

**Checkpoint**: step on the station’s yellow pad at (-167, 36, 31) (x -168..-166, z 29..33; command block at -155, 35, 27) → the station command `setworldspawn` moves the global spawn to the station (H). **Elytra**: step on any lodestone of the y 36 floor → the command at (-155, 35, 28) equips an elytra in the chest slot (H). **Elytra off**: step on stone or yellow glazed → the two commands at (-155, 33..34, 28) clear the chest slot (H). **Riptide refresh**: `enchant @a riptide 2` + `unbreaking 3` run continuously at (-157/-156, 32, 28) (H).

Every one of these uses `@a`, not `@s`: one player on a pad changes the kit for **all** players (H). This is a multiplayer bug, invisible in single-player testing.

### 4.4 Exit

The west lodestone platform is the **equip pad**: the player steps on the lodestone (elytra on) and launches from the platform’s open **north edge at z 28**, gliding north-east to the north snow. The car body to the east (x −155..−135) and the lower balcony to the south (z 36..42) enclose the other sides; the west edge faces the red/white cross and the void (H). The cross at x −182 is decoration (H). The launch target is the north snow chain (§5): station y 36 → target y −20, ~240 m, 56-block drop (ratio 4.3:1), an easy glide (M).

## 5. Leg 3B — pool / lodge line (M)

### 5.1 Chalet to pool

Climb the chalet spiral stair from y −2 to y 10 (H). The pool’s south rim is at **x −40, y 14..15, z 18..19**, directly above the chalet roof at y 9..10 — a 5–6 block step (H: component gap). There is no continuous stair between them (H). The pool’s east edge at **x −38** is a waterfall into the void; the intended move is a **riptide up that waterfall** from the chalet roof to the pool rim (M). The trident is in the kit and Riptide is level 1 at the start / level 2 at the station, so this is the same mechanic as the finish (M). The lime pad at the plaza base is not a 5-block launcher on its own (H); it is a jump-boost accent.

The pool is a 54×38 water/ice sheet at **x −92..−38, z −19..19, y 15**, with snow islands and five yellow speed pads (H). Its east edge at x −38 is the waterfall — a hard failure plane (H). The north/east exit is the stone arch and the white/red bridge.

### 5.2 Pool crossings and the arch

| Feature | Coordinates | Notes |
|---|---|---|
| West river (optional loop) | x −179..−92, z −32..−11, y 16 | packed-ice lower deck; yellow Y09/Y10; hairpin at x −176; upper deck y 20 (Y15) |
| Stone arch | x −50..−35, z −30..−20, y 17..20 | gray stone arch with a pink/red jump pad on top (H) |
| East bridge | x −38..−24, z −16..−10, y 17..19 | white/red/ice bridge, yellow Y11 at x −44..−43, magenta at −28, 19, −13, yellow Y16 at x −34..−31 (H) |
| Ridge to lodge | x −24..−14, z −20..−30, y 20..25 | lime pad at x −14..−12, y 22, z −4..−2 is on the ridge (H) |

The river loop is a **scenic out-and-back** (lower deck west → hairpin → upper deck east → arch → pool). It is fully padded but adds ~250 m and has no checkpoint (H). It is not needed for the primary line.

### 5.3 Lodge roof

The lodge is a dark-wood/stone building at **x −25..2, z −39..−3, y 26..46** (H). Its roof at **y 35** carries:

- 60 lodestone blocks at x −24..−10, z −39..−34 (elytra pad, H);
- yellow Y07 at x −22..−11, z −30..−29 (24 blocks) and Y08 at x −9..−8, z −24..−22 (6 blocks);
- yellow Y12 at x −3..−2, y 32, z −11..−9 (the descent/approach from the pool ridge);
- black-concrete beams along the roof edges (a train-track gantry; these also act as railings on the south/east sides) (H).

The open side is north — the elytra launch direction (M). The lodge roof → north tip is only 100 m with a 55-block drop (ratio 1.8:1), the easiest launch in the map (M).

## 6. Leg 4 — elytra to the north landing (H/M)

Both lines launch from a lodestone pad:

| Launch | Pad (equip) | Target | Horizontal | Drop | Ratio |
|---|---|---|---|---|---|
| Station | −163, 36, 31 (west platform; launch north edge z 28) | north tip 15, −20, −132 | ~240 m | 56 | 4.3 |
| Lodge | −17, 35, −36 (roof) | north tip 15, −20, −132 | ~100 m | 55 | 1.8 |

Landing candidates (H for geometry):

- **North tip platform**: x 7..22, y −20, z −140..−129, snow, 3×3 jump pad at (15, −20, −132), two blue-concrete posts with gold bases (x 7/8, z −135/−131). This is the end of the walkable chain and the natural landing (M).
- **Lone island**: x −29..−13, y −19, z −124..−114, snow, 3×3 jump pad at (−18, −19, −119); **unreachable on foot** (separate component) and directly under the lodge→north flight path (M). It may be an alternate landing/bonus (L).

There is **no checkpoint in the north**: a missed landing is a void death; respawn is the last yellow pad (station or start) (H).

## 7. Leg 5 — north chain south (M)

From the north tip run **south** along the zig-zag snow chain. The chain is a single 3–6-wide ribbon at y −24..−7, with seven 3×3 jump pads and six yellow speed strips:

| Order (southbound) | Feature | x | y | z |
|---|---|---|---|---|
| 1 | yellow Y01 | 26..28 | −22 | −119..−117 |
| 2 | jump pad | 36 | −23 | −110 |
| 3 | yellow Y02 | 50..55 | −23 | −105..−102 |
| 4 | yellow Y03 | 33..35 | −21 | −93..−91 |
| 5 | jump pad | 48 | −19 | −88 |
| 6 | yellow Y04 | 58..63 | −22 | −82..−81 |
| 7 | jump pad | 40 | −12 | −59 |
| 8 | yellow Y05 | 52..53 | −13 | −62..−57 |
| 9 | yellow Y06 | 19..22 | −9 | −58..−53 |
| 10 | tunnel mouth | 21 | −9..−7 | −45..−36 |

Sea lanterns at x 57..64, z −72 light the ice bridge (H). The path descends from y −20 at the tip to y −9 at the tunnel, then the tunnel/colonnade at **x 17..26, z −45..−36, y −7..−9** (three 1-wide stone pillars at x 17/21/25, z −38) (H).

## 8. Leg 6 — tunnel, waterfall, finish (H/M)

- From the tunnel the ledge at **y −13, x 14..28, z −35..−14** runs south (H). Two jump pads sit on it at **(16, −13, −25)** and **(26, −13, −25)** (H) — the only pads on this final ledge, spaced like two landing lanes.
- The ledge ends at the **waterfall sheet** at **z −16..−14, x 15..28, y −13..9** (22 blocks tall; H). The water is a solid sheet from the platform down to the ledge; the wall behind it is the train engine/platform (H).
- The intended move is a **riptide up the waterfall** onto the start platform (M): the trident is given at the start, riptide is refreshed at the station to level 2, and the waterfall is exactly the height a riptide can clear.
- Alternatively swim up the flowing water (slow) or use the platform’s north edge (L).
- Finish: run south across the platform from z −13 to the gate at **z 7** (H). The gate is open after the start fill; the checkered panes are the finish line (M).

## 9. Checkpoints and respawn (H/M)

- **Global spawn** starts at (21, −1, 4) (H).
- **Every yellow glazed pad** under a player runs `setworldspawn` (H: command at 23, −3, −2; 29 yellow clusters). Because the command block is in the start chunk, only pads within the loaded radius are live: at `simulation-distance=6` (the review server, H) that is roughly x −73..119, z −96..94; the station command at (−155, 35, 27) covers roughly x −251..−59, z −69..123. The two together cover the village, south-west, ice cave and station, but **not the pool east/lodge or the north chain** (M). At sim ≥10 the regions overlap; at sim ≥12 they cover the whole map.
- **Station pad** is the only explicit mid-course checkpoint (H).
- **No checkpoint** on the pool/lodge line or the north chain (H).

## 10. Failure planes (H unless noted)

| Failure | Where | Result |
|---|---|---|
| Void | y −64 everywhere outside the islands | death; respawn at last yellow pad |
| Pool waterfall edge | x −38, z −19..19 | fall to void |
| Start/return waterfall | z −16..−14, x 15..28 | fall to the y −13 ledge (safe) or miss it (void) |
| Elytra miss | anywhere over the middle void | death; no north checkpoint |
| North chain sides | y −24..−7, narrow ribbon | fall to void |
| Jump-pad overshoot | 3×3 pads at Speed VIII/IX | overshoot into void (M) |
| Gate | z 7 if the fill fails | blocked start (the saved state is closed, H) |
| Bubble column | x −129..−127, z 29..32 | safe (water) but one-way up |

## 11. Lap behaviour and timing

- **One lap**, start = finish at the gate plane z 7 (M). The gate opens once at the start and stays open (fill air); the button restarts the countdown (H).
- No lap counter, timer or scoreboard exists in the 37 command blocks (H). The only place text is the always-on actionbar “You are in 1st place” at (22, −3, 0) (H), which is a placeholder for a plugin (M).
- `keepInventory=true`, `doImmediateRespawn=true`, `fallDamage=false`, `doDaylightCycle=false`, `DayTime=1000` (H: level.dat). Deaths keep the trident/elytra and respawn instantly.
- Estimated lap: ~700 m of path, 60–100 s at Speed IV–IX (L — no timing data in the world).

## 12. What to verify in-game

1. Press the button: does the gate fill run and clear the panes/barrier? (Saved SuccessCount was 0.)
2. Does the countdown show 1 → 2 → 3 in order? (Timestamps say no.)
3. Does one player’s station pad strip everyone’s elytra? (`@a` says yes.)
4. Does the lodge roof equip an elytra at `simulation-distance=6`? (Geometry says no.)
5. Does the riptide up the start waterfall reach the platform? (Height 22 blocks; Riptide II is ~11–13 blocks per use, so likely needs two riptides or a swim assist — M/L.)

## 13. Overlay waypoint key (`renders/route_overlay.png`)

| # | x,z | What |
|---:|---|---|
| 1 | 21, 6 | start button / gate |
| 2 | 21, 14 | boat basin |
| 3 | 28, 32 | village east/south turn |
| 4 | 12, 63 | village bottom |
| 5 | −16, 50 | village west return |
| 6 | −30, 37 | south-west plaza / split |
| 7 | −60, 28 | ice-cave east |
| 8 | −95, 30 | ice-cave mid |
| 9 | −120, 37 | ice-cave west |
| 10 | −127, 29 | bubble-tower base/top |
| 11 | −150, 31 | station car |
| 12 | −165, 31 | station west lodestone platform (elytra equip) |
| 13 | 15, −132 | north tip (elytra landing) |
| 14 | 48, −104 | north chain mid |
| 15 | 21, −48 | tunnel/colonnade |
| 16 | 21, −25 | landing ledge pads |
| 17 | 21, −12 | finish waterfall |
| 18 | 21, 6 | finish gate (same as 1) |

The orange pool/lodge line uses the same 6 → pool → river → lodge branch; the cyan dashed lines are the two elytra legs; the magenta line is the north-chain return.
