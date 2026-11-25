# Clouds 2 — route, leg by leg

Companion files: `data/route_legs.csv`, `data/checkpoints.csv`,
`renders/route_labeled.png`, `renders/overview_top.png`,
`renders/overview_iso_a_labeled.png`, `renders/overview_iso_b_labeled.png`.

All coordinates are absolute world coordinates. Y is the block the player stands on
unless stated otherwise. **H** = observed, **M** = inferred from two sources,
**L** = guess.

## 0. The shape of the course

The course is a **single closed loop** around a void world. The start pen is a
two-door box on the east side of the loop at z ≈ 20–29. The player leaves north
through the checkered gate, runs the loop counter-clockwise (north up the east side,
west across the top, south down the west side, east across the bottom, north up the
east side) and re-enters the same pen from the south to finish.

Why this is a loop and not a point-to-point course:

- The pen has a **north gate at z = 20** and a **south opening at z = 29**; the reset
  command closes the south side with red stained glass, and the start command opens
  it again. **H** (commands at x −49..−39, y 1, z 22/26)
- The yellow-glazed arrows in the corridor **south** of the pen face south, i.e. their
  chevrons point **north into the pen** — a finish approach, not a start. **H**
  (`scripts/arrow_field.py`, cell z 32, x 0: 6/6 facing south)- The same arrow field runs continuously around the loop: north on the east side,
  west at the planet, south down the west side, east across the bottom, north through
  the castle and coral. **H** (`scripts/arrow_field.py`)
- No command detects a finish, a lap or a timer, so the “loop” is a design fact, not a
  scripted one. **H**

Total length ≈ 1,500–1,700 blocks along the centreline, with roughly 350 blocks of
elytra flight. **M** (sum of leg lengths in `route_legs.csv`)

## 1. Start / finish box (L0)

| Item | Coordinate | Tag |
|---|---|---|
| Teleport target | 5.5, 4.0, 24.5, facing (5, 5, −20) = north | H |
| Start line (checkered) | z = 20, x −3..13, barrier at y = 5 when closed | H |
| Pen walls | x = −3 and x = 13, z = 20..29; lime concrete + lime glass | H |
| South wall (finish door) | z = 29, x −2..11; red stained glass when reset, air during the race | H |
| Start button | stone button at (5, 4, 22) | H |
| Reset button | stone button at (−3, 4, 25), under the “RESET COURSE” sign | H |
| Countdown | 9 titles `>1<` … `>9<` + “Starting In”, from the repeater line at x −39..−3, z 22–24 | H |
| Start sound | `music_disc.far` at (−35, 1, 21) then `block.brewing_stand.brew` at (3, 1, 22) | H |
| First item | trident into hotbar.0 at (3, 2, 22) | H |
| HUD | static actionbar “You are in 1st place” at (−51, 1, 29) | H |
| World spawn | `setworldspawn` is **not** called here; the initial spawn is 5,4,23 from level.dat | H |

The pen is 17 × 10 blocks internally (x −3..13, z 20..29) with a white floor and a
dark-green arrow painted on it pointing north. **H** (`renders/start_top.png`)

The reset chain (`tp` → `clear` → red glass → barrier → “Course has reset!”) is
triggered by the reset button at (−3,4,25). The start chain (gate opens, trident,
countdown) by the button at (5,4,22). **H**

## 2. L1 — Start corridor and garden (z 20 → −10)

- Exit through the checkered line at **z = 20**, x −3..13.
- Cross a 10-block green apron with three animated yellow chevron strips pointing
  north. **H** (`renders/start_iso.png`, `renders/pack_pads.png`)
- Pass under the **green hedge arch** (lime concrete/terracotta, ~15 wide, ~7 high).
  **H**
- Checkpoint at **(11, 0, 16)** — yellow glazed, gives trident + `setworldspawn`. **H**
- **Mega jump at (11, 1, 11)** — orange shulker: speed 35 for 1 s, jump boost 14,
  clears inventory, candle-extinguish sound. **H**
- **Jump pad at (11, 1, 8)** — lime shulker: jump boost 7. **H**
- Checkpoint band at **z = 4**, x −2..12, y 3 (three 3×5 pads). **H**
- Pad row at **z = −7**: red glazed at (5, 2), (−7, 3), (17, 3). **H**
- Checkpoint band at **z = −17**, x −5..15, y 3 (four pads). **H**

Sightlines: the garden is a shallow bowl of grass, hedges and flower beds. The hedge
arch frames the exit; beyond it the white pen and the chevrons are the only bright
things, so the eye is pulled north. **H** (`renders/mid_top.png`)

## 3. L2 — Farm terraces (z −10 → −50)

- A stepped farm of packed mud, coarse crop rows (yellow/orange blocks) and
  terracotta edges. **H** (`renders/mid_iso.png`)
- Checkpoint band at **z = −37**, x −8..18, y 3 (five pads). **H**
- The farm terraces are the first place the floor changes from grass to packed mud;
  the player can read “still on course” from the path material. **M**

## 4. L3 — Sky islands, first climb (z −50 → −140)

- Pads at **(0, 0, −56)** and **(10, 0, −56)** (red glazed).
- **Slow traps** at **(0, 0, −62)** and **(10, 0, −62)** — green glazed: slowness 1 s,
  clears speed and jump boost. **H**
- Pad at **(5, 0, −77)**.
- Checkpoints at **(9, −2, −96)** and **(12, −3, −100)**.
- **Lime jump at (−2, −2, −116)**.
- Pads at **(8, 0, −123)** and **(−1, 5, −127)**.
- Pad at **(11, 3, −141)**, checkpoint at **(24, 5, −142)**.

This is where the theme changes from soil to **snow and cloud**. The islands are
snow_block tops with calcite shoulders and long blue waterfalls pouring off them.
**H** (`renders/snow_iso.png`)

## 5. L4 — High islands (z −140 → −215)

- Pad (26, 8, −151); checkpoint (−7, 9, −151).
- Pad (13, 8, −159).
- Pad (12, 14, −177); checkpoints (−10, 11, −176) and (22, 18, −177).
- Pad (−1, 14, −184).
- Pad (30, 16, −189); checkpoint band (30, 16, −198).
- Pad (35, 16, −210).

The pads climb from y ≈ 3 to y ≈ 18 over 150 blocks. Each island is 3–6 blocks
higher than the last, so the course reads as a staircase. **H** (pad y values)

A **black/white checkered gate** sits at z ≈ −145 (x 10..14, y 3..9) — a section
marker, not a finish. **H** (`scripts/find_block.py black_concrete`)

## 6. L5 — Planet and waterfall (z −215 → −256)

- **Elytra grant at (29, 19, −234)** on spruce planks. **H**
- Pad (35, 17, −238).
- Checkpoint (14, 18, −254) and lime jump (11, 18, −254).
- Rim pads (4, 23, −256) and (−16, 23, −259).
- **Mega jump at (−38, 25, −252)** on the rim of the circular pool.

The “planet” is a 40-block circular pool at y ≈ 25 with a huge waterfall pouring out
of it and a brown/gold spiral hanging above. The player arrives on the eastern white
island (with a checkpoint flag), crosses the rim, and drops into the pool. The
waterfall carries them down ~30 blocks to the water tunnel at y ≈ −5. **H**
(`renders/planet3_iso.png`, cross-section `renders/xsec_z-255.png`)

Landmark value: the planet is the single tallest, most contrasted object on the
course and is visible from most of the east side. **H** (`overview_iso_a.png`)

## 7. L6 — Water tunnel (x −88 → −240 at z = −256, y ≈ −5)

- A 6-block-high glass tube: blue stained glass at y = −8 and y = −2, water y = −7..−3.
- Red terracotta ribs every 13 blocks (x −206, −196, −183, −170, −157, −144, −133,
  −121, −108, −95).
- The tube is **interrupted inside the ruin** at x ≈ −224..−211 (structure_void cuts
  and an open channel), which is the exit. **H** (`scripts/slice_x.py --y -5`)
- Length ≈ 150 blocks. The player swims west; `dolphins_grace` is global from the
  command banks, and `drowningDamage=false` in level.dat. **H**

The tunnel is the only fully enclosed corridor on the course. It is also the only
place where the player cannot see the next island, so the red ribs act as a **speed
gauge**: one rib every 13 blocks. **M**

## 8. L7 — Ruin and crater village (z −283 → −150)

- Exit the tunnel into the **stone ruin** at (−240, −5, −256). The ruin is a large
  C-shaped masonry amphitheatre spanning x −258..−190, z −316..−230, with snow
  terraces. **H** (`renders/topleft_top.png`, `renders/topleft_iso.png`)
- Follow the pad chain south along x ≈ −155:
  checkpoints (−159, −24, −283) and (−150, −24, −283);
  pads (−162, −24, −274), (−150, −24, −274);
  lime jump (−155, −26, −270);
  checkpoint (−155, −30, −264);
  pads (−163, −26, −257), (−143, −27, −257);
  checkpoint (−150, −29, −250);
  pads (−167, −27, −239), (−148, −28, −239);
  checkpoints (−157, −30, −232), (−158, −30, −228);
  pad (−158, −30, −223);
  checkpoint (−158, −29, −206);
  lime jumps (−171, −29, −196), (−145, −29, −196);
  checkpoints (−172, −25, −191), (−143, −25, −191).
- The visible mid-section is a **five-pad arc on the white cloud islands**, x −185..−140,
  z −245..−230, followed by a narrow one-block bridge at z ≈ −213 into the crater rim.
  **H** (`renders/tile_nw.png`)
- The chain descends only 5 blocks over 90 blocks; it is a **stepping-stone path**,
  not a drop. **H** (pad y values)

The crater is a mud-brick **bowl village**: a circular terraced bowl ~50 blocks
across with four red pads inside, towers, and an orange rim. The route enters at the
north rim and exits at the south “handle”. **H** (`renders/leftcol_iso.png`)

## 9. L8 — Ring flight (z −161 → z +75)

- **Elytra grant at (−158, −19, −161)** on spruce planks, after checkpoint
  (−158, −18, −182). **H**
- Fly south through a slalom of **light-blue concrete rings** and **yellow concrete
  lightning-bolt markers**:
  ring (−161, −18, −147); bolt (−153, −19, −124);
  rings (−196, −40, −80) and (−139, −31, −76); bolt (−224, −25, −61);
  ring (−198, −45, −27); ring (−161, −50, 33); bolt (−165, −35, 39).
- Land at the bottom-left village around x −150, z 70–90. **H**
  (`renders/gap_iso.png`, `scripts/find_block.py light_blue_concrete` / `yellow_concrete`)

This is the longest flight and the only leg with no floor. The rings are spaced
40–50 blocks apart and alternate left/right of the straight line, so the player
must actively steer. **M**

## 10. L9 — Village, rainbow and terraces (z 75 → 210)

- **Elytra grant in the village at (−148, −57, 70..84)** on spruce planks. **H**
- Checkpoints (−147, −38, 82), (−149, −57, 88), (−148, −57, 96), pad (−148, −57, 103).
- The village is a desert-style cluster of sandstone towers, a wooden deck and a
  **parrot** on a perch; it is the landing zone for L8. **H** (`renders/botleft_iso.png`)
- **Rainbow gate at z = 161**: 7 terracotta bands, x ≈ −172..−130, apex y ≈ 2, legs to
  y ≈ −32. The player runs through it heading south. **H** (`scripts/probe.py`)
- Terrace checkpoints (−151, −39, 151), (−142, −39, 151), pad (−147, −32, 169),
  pad (−140, −39, 176), checkpoint (−165, −38, 211).
- Lime jumps at (−157, −47, 139), (−137, −51, 137), (−153, −57, 129), (−143, −57, 129).

The terraces are a **stepped white staircase** of calcite/snow with blue pools and
calcite edges. Each terrace is 3–5 blocks lower than the last, so the player always
sees the next one below. **H** (`renders/tile_w.png`)

## 11. L10 — Bottom crossing (z 210 → 265, x −165 → −13)

- Checkpoint (−124, −34, 260).
- Pads (−115, −29, 260), (−100, −29, 260), (−60, −39, 255), (−47, −39, 255),
  (−41, −42, 260), (−21, −40, 255).
- The leg crosses a **vine path**, a **blue pool with two blue diving boards**, and a
  small island with a red pad. **H** (`renders/tile_bottom.png`)

## 12. L11 — Castle (x −13 → 60, z 200 → 260)

- **Five stacked elytra grants**: spruce planks at (3, −40, 250), (9, −39, 232),
  (49, −27, 219), and one **andesite slab** variant at (48, −8, 195). **H**
- Checkpoints at (13, −39, 232), (29, −32, 232), (40, −32, 232), (50, −32, 232),
  (39, −23, 220), (48, −10, 199).
- Lime jumps at (49, −19, 205) and (46, −16, 203).
- **Checkered gate at z = 201**, x 42..50, y −16..−10. **H**
  (`scripts/find_block.py black_concrete`)
- The castle is a wooden-and-sandstone courtyard with a flower bridge, two blue
  “diving boards”, a round table platform and three towers. **H**
  (`renders/botright_iso.png`)

The castle climbs y = −40 → −8 in 60 blocks, using elytra to cross each gap. **H**

## 13. L12 — Arches and coral (z 200 → 60)

- North through the checkered gate at z = 201.
- Two **orange/white arch gates** at z ≈ 169 and z ≈ 145 (white concrete + orange
  terracotta, ~10 wide). **H** (`renders/arrows_iso.png`)
- Coral section: checkpoint (9, −13, 93), pads (26, −5, 82), (15, −6, 72),
  (28, −4, 68), **elytra grant (17, 0, 61)**, pads (9, 0, 56), (22, −2, 55).
- The coral section is a chain of blue pools with red/green pads and coral decor.
  **H** (`renders/tile_coral.png`)

## 14. L13 — Finish (z 60 → 20)

- From the coral, run north up the **finish corridor** at z 50..30. The floor is
  yellow glazed chevrons whose texture points north. **H**
- The corridor is flanked by white cloud walls and calcite; the pen is visible from
  z ≈ 45. **M**
- Enter the pen through the south opening at **z = 29**, cross the checkered line at
  **z = 20**. **H**
- There is **no finish detection, no lap counter, no timer**. The race ends visually
  at the line. **H** (no scoreboard/tag/advancement commands exist in the 322)

## 15. Lap behaviour

- **Single lap.** The pen is both start and finish; the loop is closed. **M**
- No command counts laps or detects crossing. The “1st place” actionbar is a **static**
  string fired once by the start chain and cleared only by reset. **H**
- The reset cycle is global: one reset button teleports and clears **all** players. **H**
- If the map were run as multiple laps, nothing would stop a player from finishing
  lap 1 and continuing; but the course design (one-way pads, one-way drops) assumes
  one lap. **L**

## 16. Why the player always knows where to go

This is the strongest part of the map. Eight independent devices, each verified:

1. **Animated chevron floor.** Yellow glazed terracotta uses a 16-frame animated
   yellow/orange chevron texture. The block `facing` is set opposite to travel, so the
   chevron points the way. Verified statistically around the whole loop: start
   corridor 45/45 at z −32 (travel north); bottom-left 25/25 at z 64 (travel south);
   bottom crossing 53/55 at z 224 (travel east); castle 14/14 at z 192 (travel north).
   **H** (`scripts/arrow_field.py`, `renders/pack_pads.png`)
2. **Colour-coded pads.** Yellow = checkpoint/trident; lime/orange shulker = jump;
   red/magenta/pink glazed = jump + speed; green glazed = slow trap; red terracotta =
   depth-strider boots; spruce/andesite = elytra. The sign panels spell this out. **H**
3. **Theme changes per leg.** Garden → farm → snow → planet → tunnel → crater →
   sky rings → rainbow village → terraces → castle → coral. A player can name the leg
   from the floor material alone. **H**
4. **Gate framing.** The start hedge arch, the rainbow at z = 161, the two orange
   arches at z 145/169, and the checkered gates at z = 201 and z ≈ −145 all frame the
   next section. **H**
5. **Bounded corridors.** 3,130 barrier blocks form invisible walls and tall no-fly
   columns (e.g. x −171..−169 / z 168..170 up to y = 319). The pen, tunnel and crater
   are fully enclosed. **H**
6. **Checkpoint strips.** Wide rows of yellow glazed (3–5 pads across the path) at
   z = 4, −17, −37, −198, etc. reset world spawn and re-grant the trident, so a fall
   never sends the player far back. **H**
7. **Long-range landmarks.** The planet waterfall, the rainbow, the three castle
   towers and the giant water columns are visible across the void. **H**
8. **HUD.** A static “You are in 1st place” actionbar and the countdown titles. **H**

**Camera framing.** The start `tp` sets the player's rotation to look north at
(5, 5, −20), and each section exit is a gate centred on the sightline from the previous
landing: the hedge arch frames the pen and the garden arrow; the crater rim frames the
first ring; the rainbow frames the terraces; the castle's checkered gate frames the two
orange arches. The player is never asked to turn around to find the next leg. **M**

What it does **not** do: no dynamic position, no distance-to-next, no arrow HUD, no
per-player checkpoint. **H**
