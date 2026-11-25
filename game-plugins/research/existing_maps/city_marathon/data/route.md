# City Marathon — route

World: `mcc_citymarathon` / source `City Marathon` · 1.19.2 · 6,374 chunks · 67 command blocks · 140 shulkers.
All coordinates are **world X/Y/Z**. Evidence tags: **H** = directly observed in region/NBT/render; **M** = inference from two or more observed facts; **L** = guess.

> Scope note: the build has **no scripted route order, no finish detection, no timer and no lap counter** (H: no datapack/functions, no `data/scoreboard.dat`, all 67 commands are local pad/effect/respawn commands). The line below is the **most pad-dense, ground-connected circuit**; it is a reconstruction, not a code-enforced course. Alternatives are listed separately so the reader can see where the build forks.

## 0. Route summary

| Leg | From → to | Approx. length | Route furniture | Confidence |
|---|---|---|---|---|
| 0 | Start pen (8,0,5) → release gate (8,10) → start/finish gate (8,53) | 48 | button (8,0,8), 18-barrier gate at z10, 4 yellow-concrete posts x1/x15,z49/z57, 3×3 pad (8,0,53) | H positions; M intended start |
| 1 | East Road x≈8, z53 → z88 | 35 | speed strips at z20, 33, 43, 69; gate at z53; TARDIS ladder/pad at x4,z63-71 | H |
| 2 | City Avenue z≈88, x8 → x−225 | 233 | 7 speed strips, 65-block pad at x−27, effect-clear green carpet at x−98, NO ENTRY signs at x−91 | H |
| 3 | Park west → Plaza north loop → CP3 | 234 | park fountain, plaza dome/pool, pads at x−243/−294,z37, x−311,z54, x−337,z52, x−388,z51 | H geometry; M intended detour |
| 4 | CP3 → Station floor / train → West Link | 127 | train stairs x−457, train roof pads x−443..−441, station floor pads, long diorite platform x−351 | H geometry; M train variant |
| 5 | West Link x−476, z86 → z−50 | 136 | speed strips at z5 and z−51 | H |
| 6 | North Highway z−50, x−476 → x8 | 484 | 3 orange + 3 yellow + 1 glazed pad on the highway | H |
| 7 | East Road x8, z−50 → finish gate z53 | 103 | speed strips z20/33/43; finish gate z53 | H positions; M finish |

Drawn closed loop ≈ **1,352 blocks** (Manhattan sum of the polyline; H arithmetic). A **gate-to-gate lap** is ≈ **1,400 blocks** (start pen→gate 48 + gate→z88 35 + z88→highway 1,214 + highway→gate 103). The **z5–z53 East Road segment is traversed twice** (start and finish), so the race is 48 blocks longer than the closed-loop drawing — **M**. A clean run is likely 1,350–1,500 blocks depending on the plaza/park fork.

## 1. Leg 0 — start pen and release

- Spawn is **8,0,5** (`level.dat` SpawnX/Y/Z; playerdata `9ed6d016-…` at 8.43,0,5.53) — **H**.
- A **polished blackstone button at 8,0,8** sits on the road, 3 blocks north of the spawn — **H** block state.
- An invisible **barrier gate** spans z=10, x≈0..18, y1–2 (18 barrier blocks) — **H** block dump. It blocks the road south of spawn until opened.
- The start circuit under the road (x−2..13, y−3..−1) includes 5 impulse command blocks, 2 chain blocks, 10 repeaters and 13 redstone-wire blocks — **H**.
- One impulse command is `/fill 3 0 10 13 2 10 air` at (−2,−3,5); its `LastExecution=110345`, `SuccessCount=1` — **H**. It clears the **centre** of the barrier gate (x3..13) and leaves the side posts at x0 and x16–18. In the saved world the centre barriers are present again, so the gate is **closed at save** unless the start circuit is fired — **H** (block dump). The most likely explanation is that the gate was rebuilt after the single test at tick 110345 — **M**.
- The same circuit fires `item replace entity @a hotbar.0 with minecraft:trident 1` at (9,−2,5), then the title sequence (`Starting In`, `>2<`, `>1<`) — **H** LastExecution 110295–110345. The `>3<` chain block at (7,−3,3) never fired (no LastExecution) — **H**.
- The yellow-concrete gate at z49–57 is the **start/finish line**, not the release gate. Four posts: (1,4,49), (1,4,57), (15,4,49), (15,4,57) — **H**. Ground pad at (8,0,53) is a 3×3 red/pink/magenta boost pad — **H**.

> Start confidence: **M**. The button + fill + gate are real, but the map never states “press to start”; no sign says so.

## 2. Leg 1 — East Road start straight (z5 → z88)

Follow the East Road south from the pen, through the release gate (z10) and the yellow-concrete gate (z53).

- Road: gray wool asphalt, double yellow-wool centreline at x7 and x9, white-wool dashes in each lane, stone-slab sidewalks — **H**.
- Speed strips (15 yellow-glazed blocks each): **(8,−1,20)**, **(4,−1,33)**, **(12,−1,43)**, **(8,−1,69)** — **H**.
- Boost pad at **(8,0,53)** on the start/finish line; it is the red/pink/magenta 3×3 “jump/speed” flower — **H**.
- The **TARDIS** prop is at x3–5, z63–71, with two birch wall signs reading `D0CT WH0` (4,1,63 facing north; 4,1,71 facing south) and a 3-wide ladder up to the y4 quartz podium — **H**.
- Podium pad: 3×3 red/pink/magenta at **(4,4,66)**, reached by the ladder; stairs down at z69–70 — **H**.
- The road bends west at z≈80–95 into the City Avenue. The next landmark is the large 65-block yellow pad at (−27,−1,88) — **H**.

Sightline: the road is a straight, walled canyon between brick buildings; the yellow double line is continuous and the pads are visible 20–30 blocks ahead — **H**. At night the embedded `light` blocks illuminate the road surface from below/inside the pads — **H** (2,182 light blocks, mostly level 15).

## 3. Leg 2 — City Avenue and the park (x8 → x−225, z≈88)

Turn west at z≈88 and follow the avenue through the park.

- Speed strips: **(−3,−1,88)**, **(−27,−1,88)** (65-block special), **(−52,−1,84)** and **(−52,−1,92)**, **(−85,−1,88)**, **(−150,−1,88)**, **(−172,−1,88)** — **H**.
- 3×3 pads: **(−112,−1,82)** (red/pink), plus the large start-side pad at (−27,88) — **H**.
- **Green carpet** effect-clear patches at **(−98,0,82)** and **(−97,0,90)**; the command clears all effects — **H**.
- **NO ENTRY** signs at (−91,0,88) and (−91,0,89), standing `acacia_sign[rotation=12]` → text faces **east**. Two more are at (−56,1,49) facing west and (−55,2,47) rotation 3 (faces west-southwest); a fifth is at (−2,1,47) rotation 13 (faces east-southeast) — **H**.
- Park centre: fountain at **(−145,85)**, circular plaza, tree grid, cherry-blossom tree in the south-east, pond at x−120..−83, z94–142 — **H**.
- Park side pads: **(−140,−1,120)** (south path), **(−159,−1,96)** (south-west diagonal) — **H**.
- Park west edge: the z88 path meets the walled plaza at x−225; the route turns **north** here — **H** walkability.

> Direction ambiguity: the two east-facing NO ENTRY signs at x−91 imply the z88 park gate is an **exit**, not an entry. A player entering the park westbound sees their backs only if the signs are on the south side of the gate; the exact sign face is H, the intended one-way meaning is **M**. A clean alternative is to enter the park on the **north path z≈73** (walkable, no sign) and exit at z88.

## 4. Leg 3 — Park west → Plaza north loop → CP3

The plaza block is x≈−315..−225, z≈25..150. Its interior is a sunken pool and a blue half-dome; the direct z88 line is **not walkable** (H: (−245,88) and (−268,88) are inside the dome/pool; BFS cannot reach them).

Take the **north loop**:

1. From (−225,88) go north along the plaza east edge to **z≈44**.
2. West along the north road **z≈37–44**: pads at **(−213,−1,37)** (orange), **(−243,−1,37)** (yellow), **(−294,−1,37)** (yellow) — **H**.
3. South along the west edge **x≈−311**: pad at **(−311,−1,54)** (yellow) — **H**.
4. Rejoin the z88 line at (−311,88) where a 3×3 red/pink pad sits — **H**.
5. Continue west: **(−337,4,52)** yellow, **(−361,4,52)** orange, **(−388,−8,51)** CP3 command cluster, **(−396,4,52)** yellow, **(−425,4,52)** red/pink, **(−468,4,57)** yellow — **H**.

CP3 is at **(−388,−8,51)**: 15 command blocks under the road, including `enchant @a unbreaking 3`, `enchant @a riptide`, `setworldspawn`, `title @a actionbar "You are in 1st place"` and seven failed `gamemode adventure` attempts — **H**.

Sightline: the plaza is a walled rectangular block; the north road is a clear 9–11 block corridor with a yellow centreline. The blue dome is a strong landmark to the south, but it is also the reason the direct line is blocked — **H**.

## 5. Leg 4 — Station approach, floor and train

From CP3 (x−388,z51) continue west to **x−457,z52**; the station occupies x≈−536..−350, z≈20..155.

- Station north approach pads: **(−425,4,52)**, **(−468,4,57)** — **H**.
- Descend south along x−457 to the station floor at **z≈80–88** (walkable; H).
- The **train** is x≈−460..−380, z≈85–91; its body is at y4–8, its roof at y8–9. The train is solid and blocks the direct z88 line; the floor passes north (z≈80) or south (z≈95) — **H**.
- Boarding point: yellow-concrete stair at **x−457, y5–7, z86–90**, leading to the roof — **H**. Roof pad: 15 yellow-glazed blocks at **(−445..−441, 8, 87..89)** — **H**.
- If you run the roof, you travel **east** from x−457 to x−380 and drop at the east end. This is a **detour/skip**, not the main westbound line.
- Station floor pads: **(−362,4,81)** and **(−362,4,95)** yellow; **(−395,8,88)** yellow on the train roof; **(−417,8,88)** orange on the train roof; **(−443,8,88)** yellow on the train roof — **H**.
- Long **checkered platform**: x−355..−347, y4, z23..154 (9×132; 594 polished-diorite tiles in a 1,188-tile checker (remainder andesite)). It is the only large ground-reachable elytra-equip surface; its south end at z154 is **not** reachable on the ground graph, its north end z23 is — **H**.
- The platform intersects the station at z88 and is bounded by green-roofed buildings on the east (x−345..−320) — **H**.

The station concourse is visually enormous and mostly empty grey floor; the train is the only strong set piece — **H** (see `frames/10_station_iso.png`).

## 6. Leg 5 — West Link (x−476, z86 → z−50)

From the station floor go west to **x−476**, then north.

- The corridor at x−476 is continuous from z≈86 to z−50 (BFS, H), even though the top-down view shows it as a narrow slot between the station and the map edge.
- Speed strips: **(−476,−1,5)** and **(−476,−1,−51)** — **H**.
- The corridor is 9–11 blocks wide, with the same gray/yellow/white road language — **H**.
- It passes the **west waterfall**: x−296..−294, z49–70, water from y−1 down to y−64 (255 water blocks) — **H**. It is scenery/trap, not a route.

## 7. Leg 6 — North Highway (z−50, x−476 → x8)

Turn east on the broad highway.

- Highway is z≈−60..−40, x−536..8, with a T-junction at x−476 — **H**.
- Pads: **(−450,−1,−55)** orange, **(−420,−1,−47)** orange, **(−390,−1,−51)** orange, **(−360,−1,−55)** yellow, **(−335,−1,−47)** yellow, **(−310,−1,−51)** yellow, **(−280,−1,−47)** red/pink — **H**.
- The highway passes a row of six grey office towers on the south side (x−260..−60, z−70..−40), each with a shulker-box AC bank on its roof — **H**.
- At x≈8 the highway bends south into the East Road; a raised ramp/guardrail runs along the outer edge — **H**.

## 8. Leg 7 — East Road finish (z−50 → z53)

- South on x≈8 from z−50 to the finish gate at z53.
- Speed strips: **z20**, **z33**, **z43**, **z69** (the z69 strip is south of the finish, used only if you overshoot) — **H**.
- Finish gate: same four yellow-concrete posts at x1/x15,z49/z57, y4; the ground 3×3 pad at (8,0,53) — **H**.
- Crossing the gate does **nothing**: no command checks for it, no scoreboard, no title, no sound — **H**.

## 9. Alternates, shortcuts and skips

| ID | Name | Coordinates | What it does | Evidence |
|---|---|---|---|---|
| S1 | Plaza south loop | (−225,88) → (−225,139) → (−294,139) → (−311,126) → (−311,88) | Mirror of the north loop; pads at −244/−294,z139 and −311,z126, −337,z124. Same length ±10 blocks. | H geometry; M intended alternate |
| S2 | Park north entry | East Road z73 → x−67 → x−145 → south to z88 | Avoids the z88 NO ENTRY gate; no pads on the z73 path itself. | H walkable; M intent |
| S3 | Train roof | x−457 stairs → roof y8 → east to x−380 | 5-block-high shortcut over the train; pads at −445/−443; drops at the east end. | H |
| S4 | Long diorite platform | x−351, y4, z23..154 | 9×132 elytra-equip strip; north end connects to the station/plaza, south end blocked. | H |
| S5 | West Link bypass | x−476, z86 → z−50 | Skips the station/train entirely; 2 speed strips. | H |
| S6 | North ladder | (−243,−38) ladder to y17; orange pad (−240,17,−37) | Vertical shortcut to the highway-side roof; not on the ground main line. | H |
| S7 | TARDIS ladder | x3–5, z63 → y4; pad (4,4,66) | Start-side vertical shortcut to a 3×3 boost pad. | H |
| S8 | Park south path | (−145,85) → (−145,120) → (−140,120) pad | Dead-end loop around the south plaza; joins no west exit. | H |

**Not a skip:** the west waterfall at x−295 is a 63-block drop; the east pool at x−295,z106–127 is only 1 block deep. Neither is a viable shortcut — **M**.

## 10. Lap behavior

- **No lap logic.** There is no scoreboard objective, no datapack function, no `scoreboard`/`execute if score` command, no timer, no finish detection — **H** (67-command table, empty `datapacks/`, no `data/scoreboard.dat`).
- `setworldspawn` on every yellow-glazed strip is the only checkpoint-like state; it moves the respawn point but does not record order or lap — **H**.
- The release gate opens once via `/fill`; there is no command to close it again — **H**.
- A second lap can be run manually, but the map does not reset the gate, the elytra state, the actionbar or the spawn — **H**.
- The three command clusters (start, −208, −388) are duplicated mechanic banks, not three laps — **H** (each contains the same pad-effect commands).

## 11. Failure / kill planes

- **No `kill` command, no lava, no explicit kill plane** — **H** (all 67 commands).
- Void: the world bottom is y−64; the only blocks at y−64 are 3 water blocks at x−296..−294,z49. Falling below y−64 kills — **H**.
- `fallDamage=false`, `drowningDamage=false`, `doImmediateRespawn=true`, `keepInventory=true` — **H** gamerules. Death is therefore mostly void-only.
- `difficulty=0` (peaceful), `doMobSpawning=false` — **H**.
- The **release gate** can trap a player in the pen if the start circuit is not fired — **H** (18 barriers at z10 at save).
- The **train chassis** is 1,035 barrier blocks at y4, z85–91, x−536..−367; it is a solid invisible wall through the station — **H**.
- Map edges are open to the void; there is no barrier ring around the 559×241 build — **H** (barrier bbox is only the train + gate).

## 12. Readability verdict

- **As a city:** strong. Continuous asphalt, stone-slab sidewalks, double-yellow centreline, white lane dashes, lit pads, and a clear landmark sequence (park fountain → blue dome → train → station → highway) — **H**.
- **As a race line:** weak. There are no direction arrows, no “1/2/3” checkpoint boards, no lap/timer display, no finish detection, and nearly every block has a parallel pad-lined alternative (park z73/z88, plaza north/south, train roof/floor, West Link/platform) — **H**.
- **Vertical layer:** broken. Six elytra command blocks sit on rooflines at y27–28; the ground graph cannot reach any of them (no ladder/stairs on the towers; the one ladder at −243,−38 tops out at y17). The long diorite strip is the only ground-reachable elytra pad, and there is no fireworks/rain/height-gain source — **H** positions; **M** conclusion.
