# Hole in the Wall: the wall set

Everything here comes from the MCC reference pack in
`game-plugins/research/mcc_archive/hole_in_the_wall/` (hitw-1 and hitw-2). No hole shape is made up.

## Where the shapes come from

- **Hole patterns:** the MCC Island wiki wall diagrams for Classic, Fishbowl and Highrise
  (`hitw-1/frames/<map>_wall_<E|M|D|X><n>.png`, sheets `*_wall_patterns_sheet.jpg`). Each diagram is
  1120 x 400 px = 14 x 5 blocks (80 px per slime block). They were read cell by cell into
  `src/main/java/com/mcm/games/hitw/WallPatterns.java`. MCC sorts them into four sets: **E**asy, **M**edium,
  **D**ifficult, e**X**treme; we keep MCC's own set for each wall.
- **Look:** event frames `020330_dehef_wall_closeup.jpg`, `020340_dehef_observer_walls_three_sides.jpg`,
  `011137_frozenpit_wall_slot_closeup.jpg`, `011616_frozenpit_arena_topdown_walls_round2.jpg`,
  `fs_021811_magiccouncil_wall-closeup-piston-heads.jpg`: lime **slime block** walls, 5 high, with grey
  **piston caps** on top. Ours are the same. A special wall type colours its caps (MCC uses a banner
  instead; the colour keeps the type readable here).
- **Ladders:** the dark-green ladder strips in the diagrams (Classic M1, D6, X2, X3, X4; Fishbowl M1, M2,
  M5, D1, D2, D3, X1, X3, X7; Highrise D2, X4, X6) are placed as real ladders in the hole, on the same side.
- **Wall types:** MCC wiki event list (11 types, `hitw-1/notes.md` section 3, `hitw-2/notes.md` section 2)
  and four MCC Island types (`hitw-2/notes.md`, icons `hitw-2/frames/wall_HITW-Wall_*.png`).

## How a diagram becomes our wall

- Bottom diagram row = floor level, top row = the piston-cap row (5 blocks high, as before).
- A lane can be passed **walking** (floor and the block above it open) or **jumping** (floor block solid,
  the two above open: jump into the slot as the wall arrives; ladders help you hang there).
- 9 diagrams have an empty bottom row and no lane a player could use with that reading (Classic D2, D3, D5,
  X6; Highrise M2, M5, D2, D3, X5). For those the empty bottom row is taken as below the floor (shifted one
  row down), which turns their raised holes into jump slots. Highrise X2 has no usable lane either way and is
  left out. Duplicates on the wiki (Fishbowl D5 = Classic X3, Fishbowl D6 = Classic M3) are kept once.
- Our platform is 17 wide, the diagrams 14: the pattern is centred with a random 1 or 2 lane shift, the edge
  column repeated outward, and it is mirrored half of the time.
- Every wall is checked against the current (possibly shrunken) platform: if no lane on it can be passed,
  another wall of the same set is rolled (a 2-high doorway is cut only if 16 tries fail).
- Highrise's 1x1 peep holes are half-block details in the diagrams and are not reproduced (they cannot be
  passed anyway).
- Crouching does not help in MCC (full-block walls; a crouching player is 1.5 high), so there are no
  crouch-only holes: the low and raised slots are walk or jump.

Preview sheets (MCC diagram left, ours right, passable lanes marked): `previews/hitw_walls_0_easy.png`,
`previews/hitw_walls_1_medium.png`, `previews/hitw_walls_2_difficult.png`, `previews/hitw_walls_3_extreme.png`.
Known differences visible there: the Highrise diagrams use half-block steps, which are rounded to whole
blocks; 1x1 peep holes are dropped.

## Ramp over a round

Wall level goes up every 30 s of the difficulty clock (unchanged, with the 60 s head start). The level picks
both the speed (unchanged) and now the **set** each wall is drawn from:

| Level (difficulty name, MCC Island table) | Easy | Medium | Difficult | Extreme |
|---|---|---|---|---|
| 1-2 Very Easy, Easy | 100% | | | |
| 3 Normal (game start with the head start) | 70% | 30% | | |
| 4 Moderate | 40% | 50% | 10% | |
| 5 Challenging | 20% | 50% | 30% | |
| 6 Difficult | | 40% | 50% | 10% |
| 7 Very Difficult | | 25% | 50% | 25% |
| 8 Extreme | | 10% | 50% | 40% |
| 9-10 Crazy Extreme | | | 40% | 60% |
| OVERDRIVE (5:00) | | | 30% | 70% |

Chat cues as in MCC: `New Wall Type appearing: X`, `Wall speed increased!`, `Wall difficulty increased!`,
and the existing `The platform is shrinking!`. The sidebar shows MCC's difficulty name.

## Wall types (behaviours)

MCC rule (wiki): Standard + Sike are always on, one more type at the start, and new types join during the
round. Ours: at difficulty seconds 0, 30, 100, 140, 190, 240, 290 one new type joins, in a random order per
game (stage 0 types first, stage 1 from the third, stage 2 from the fifth). Types can stack (Frozen, Waltz,
Sprint, Morph combine).

| Type | Source | Behaviour | Stage |
|---|---|---|---|
| Standard | MCC event | glides straight across | always |
| Sike | MCC event | looks Standard, vanishes just before the platform | always |
| Frozen | MCC event | stops at random points on its way, then goes on | 0 |
| Waltz | MCC event | two blocks at a time in a 3/4 rhythm (two beats, rest) | 0 |
| Sprint | MCC event | alternates strolling and sprinting | 0 |
| Morph | MCC event | swaps to a different wall of the same set just before the platform | 0 |
| Rush | MCC Island | races in, then crawls over the platform | 0 |
| Sandwich | MCC Island | **walls from both sides**: a pair from opposite sides that meet in the middle | 0 |
| Ender | MCC event | teleports to the opposite side on arrival and comes from there | 1 |
| Returning | MCC event | turns round at the far end and comes back | 1 |
| Warp | MCC event | warps forward to the platform edge | 1 |
| Trick | MCC event | rushes the platform, then breaks apart over the first two rows (changed: it used to creep, pause and rush across) | 1 |
| Sideswipe | MCC event | swings a quarter turn just before the platform and comes in from a neighbouring side (new; was missing) | 1 |
| Dance | MCC Island | over the platform: two steps forward, one step back, 2-3 times (the back step pushes early runners) | 2 |
| Vengeful | MCC Island | a narrower (9-lane) wall that slides sideways to follow one player until just before it arrives | 2 |

Not built: Hidden, Ghost, Bulldoze, Possessed, Jumpscare, Party, Weeping, Dash (MCC Island specials or
near-duplicates of the above), and MCC Island traps (not in the event).

## The walls

### Easy (17 walls)

| Wall | Holes (rows top to bottom, `#` slime, `P` piston cap, `<`/`>` ladder) | Passable lanes | MCC reference frame |
|---|---|---|---|
| Classic E1 | `.P#.#P..P#.#P.` / `.##.##..##.##.` / `###.##..##.###` / `###.##..##.###` / `.##.##..##.##.` | 4 walk-through lanes | `hitw-1/frames/classic_wall_E1.png` |
| Classic E2 | `P###.P..P.###P` / `...#.##.#.#...` / `####.#..#.####` / `###..#..#..###` / `....##.###....` | 3 walk-through lanes, 3 jump-through lanes | `hitw-1/frames/classic_wall_E2.png` |
| Classic E3 | `P.......P....P` / `#.......#....#` / `###.#####...##` / `...##....####.` / `..............` | 8 walk-through lanes | `hitw-1/frames/classic_wall_E3.png` |
| Classic E4 | `P....P..P....P` / `#....####....#` / `#....#..#....#` / `######..######` / `#....#..#....#` | 2 walk-through lanes | `hitw-1/frames/classic_wall_E4.png` |
| Classic E5 | `P....P..P.###P` / `#....#..#.#...` / `####.#..#.####` / `#.#..#..#....#` / `#.#.##..##...#` | 7 walk-through lanes, 2 jump-through lanes | `hitw-1/frames/classic_wall_E5.png` |
| Fishbowl E1 | `#P##P#..#P##P#` / `#....#..#....#` / `#....####....#` / `#....####....#` / `######..######` | 8 jump-through lanes, **jump only** | `hitw-1/frames/fishbowl_wall_E1.png` |
| Fishbowl E2 | `#P#..#P#..P##P` / `#....#....#..#` / `###..###..####` / `..#..#....#..#` / `###..###..#..#` | 6 walk-through lanes | `hitw-1/frames/fishbowl_wall_E2.png` |
| Fishbowl E3 | `P..P.P..P.P..P` / `#..#.#..#.#..#` / `#..#.#..#.#..#` / `#..#.#..#.#..#` / `####.####.####` | 2 walk-through lanes, 6 jump-through lanes | `hitw-1/frames/fishbowl_wall_E3.png` |
| Fishbowl E4 | `#P#.##P#.P###P` / `#.#.#....#...#` / `###.#....#...#` / `.#..#....#...#` / `.#..####.#####` | 4 walk-through lanes, 6 jump-through lanes | `hitw-1/frames/fishbowl_wall_E4.png` |
| Fishbowl E5 | `#P#.P##P.#P#.P` / `#...#..#.#.#.#` / `#...#..#.#.#.#` / `#...#..#.#.#.#` / `###.####.#.#.#` | 4 walk-through lanes, 4 jump-through lanes | `hitw-1/frames/fishbowl_wall_E5.png` |
| Fishbowl E6 | `#P.#P.#P#.#P##` / `.#.##.#.#.#..#` / `.#.##.#.#.#..#` / `.#.##.#.#.#..#` / `.#.##.#.#.#..#` | 7 walk-through lanes | `hitw-1/frames/fishbowl_wall_E6.png` |
| Highrise E1 | `.P..P..P..P..P` / `.#..#..#..#..#` / `.#..#..#..#..#` / `.####..####..#` / `.#..####..####` | 1 walk-through lane, 4 jump-through lanes | `hitw-1/frames/highrise_wall_E1.png` |
| Highrise E2 | `.P..P.PP.P..P.` / `.####.##.####.` / `.####.##.####.` / `.####.##.####.` / `.####....####.` | 4 walk-through lanes | `hitw-1/frames/highrise_wall_E2.png` |
| Highrise E3 | `P......P.....P` / `#......#.....#` / `#......#.....#` / `####...#..####` / `.############.` | 5 jump-through lanes, **jump only** | `hitw-1/frames/highrise_wall_E3.png` |
| Highrise E4 | `....P...P...P.` / `...##..##..##.` / `..###.###.###.` / `.###.###.###..` / `.##..##..##...` | 5 walk-through lanes | `hitw-1/frames/highrise_wall_E4.png` |
| Highrise E5 | `P......P.....P` / `#......#.....#` / `###..#####..##` / `###..#####..##` / `..............` | 4 walk-through lanes | `hitw-1/frames/highrise_wall_E5.png` |
| Highrise E6 | `..............` / `#P..P###P#..P#` / `##..##..##..##` / `.####....####.` / `.####....####.` | 6 walk-through lanes | `hitw-1/frames/highrise_wall_E6.png` |

### Medium (14 walls)

| Wall | Holes (rows top to bottom, `#` slime, `P` piston cap, `<`/`>` ladder) | Passable lanes | MCC reference frame |
|---|---|---|---|
| Classic M1 | `P##..P..P..##P` / `..#..#..#..#..` / `..####..####..` / `..####..####..` / `###..#<>#..###` | 2 walk-through lanes, 4 jump-through lanes, ladders in the holes | `hitw-1/frames/classic_wall_M1.png` |
| Classic M2 | `.#P.#P.#P..P..` / `##..#..#.###.#` / `#..##.###.#..#` / `###.##.#.#####` / `.#...#.#......` | 3 walk-through lanes | `hitw-1/frames/classic_wall_M2.png` |
| Classic M3 | `P..#.P..P.P..P` / `##.#.#..#.#..#` / `.#.###..#.#.##` / `.########.##.#` / `##....##.#####` | 2 jump-through lanes, **jump only** | `hitw-1/frames/classic_wall_M3.png` = Fishbowl D6 |
| Fishbowl M1 | `#P..P..P..P.#P` / `.#..#..#..#.#.` / `.#<.#######.#.` / `.#<.#######.#.` / `.####..#..###.` | 2 walk-through lanes, 3 jump-through lanes, ladders in the holes | `hitw-1/frames/fishbowl_wall_M1.png` |
| Fishbowl M2 | `P..P..#P..P..P` / `#<.#<.##.>#.>#` / `#<.#<.##.>#.>#` / `#.....##.....#` / `##############` | 8 jump-through lanes, ladders in the holes, **jump only** | `hitw-1/frames/fishbowl_wall_M2.png` |
| Fishbowl M3 | `P##P#.P#.#P##P` / `#...#.##.#...#` / `#.#.#.##.#.#.#` / `#.#.#.####.#.#` / `#.###.##.###.#` | 3 walk-through lanes, 2 jump-through lanes | `hitw-1/frames/fishbowl_wall_M3.png` |
| Fishbowl M4 | `#P##P.P#.P##P#` / `.#..#.##.#..#.` / `.#..#.##.#..#.` / `.#..######..#.` / `####..##..####` | 6 jump-through lanes, **jump only** | `hitw-1/frames/fishbowl_wall_M4.png` |
| Fishbowl M5 | `.#P#.P##P.#P#.` / `.#...#..#.#...` / `.###.#..#.###.` / `...#.#..#...#.` / `.###.####.###<` | 4 walk-through lanes, 2 jump-through lanes, ladders in the holes | `hitw-1/frames/fishbowl_wall_M5.png` |
| Fishbowl M6 | `..P..P..P..P..` / `###.##..##.###` / `..#..####..#..` / `####.####.####` / `..#........#..` | 2 walk-through lanes | `hitw-1/frames/fishbowl_wall_M6.png` |
| Highrise M1 | `P..P......P..P` / `#..#......#..#` / `#..########..#` / `###.######.###` / `###.######.###` | 2 walk-through lanes | `hitw-1/frames/highrise_wall_M1.png` |
| Highrise M2 | `P......P.....P` / `#......#.....#` / `#......#.....#` / `###..#####..##` / `##############` | 4 jump-through lanes, **jump only** | `hitw-1/frames/highrise_wall_M2.png` (shifted: empty bottom row is below the floor) |
| Highrise M3 | `P.....P......P` / `#.....#......#` / `##############` / `...########...` / `..............` | 6 walk-through lanes | `hitw-1/frames/highrise_wall_M3.png` |
| Highrise M4 | `P.....P......P` / `#.....#......#` / `##############` / `#..########..#` / `..............` | 4 walk-through lanes | `hitw-1/frames/highrise_wall_M4.png` |
| Highrise M5 | `P....P..P....P` / `#....#..#....#` / `#....#..#....#` / `######..######` / `##############` | 2 jump-through lanes, **jump only** | `hitw-1/frames/highrise_wall_M5.png` (shifted: empty bottom row is below the floor) |

### Difficult (19 walls)

| Wall | Holes (rows top to bottom, `#` slime, `P` piston cap, `<`/`>` ladder) | Passable lanes | MCC reference frame |
|---|---|---|---|
| Classic D1 | `P###.P..P.###P` / `####.####.####` / `#....#..#....#` / `#....#..#....#` / `##############` | 10 jump-through lanes, **jump only** | `hitw-1/frames/classic_wall_D1.png` |
| Classic D2 | `..P....P#.P..P` / `..#....##.#..#` / `#.#...###.##.#` / `#.##########.#` / `##############` | 2 jump-through lanes, **jump only** | `hitw-1/frames/classic_wall_D2.png` (shifted: empty bottom row is below the floor) |
| Classic D3 | `P...#P..P#...P` / `#...##..##...#` / `###..#..#..###` / `..####..####..` / `##############` | 2 jump-through lanes, **jump only** | `hitw-1/frames/classic_wall_D3.png` (shifted: empty bottom row is below the floor) |
| Classic D4 | `P....P..P....P` / `#....#..#....#` / `####.#..#.####` / `#....#..#....#` / `##############` | 4 jump-through lanes, **jump only** | `hitw-1/frames/classic_wall_D4.png` |
| Classic D5 | `P...#P..P#...P` / `#...##..##...#` / `###..#..#..###` / `..#######..#..` / `##############` | 2 jump-through lanes, **jump only** | `hitw-1/frames/classic_wall_D5.png` (shifted: empty bottom row is below the floor) |
| Classic D6 | `P....P..P....P` / `#....#..#....#` / `#<>########<>#` / `#....#..#....#` / `##############` | 4 jump-through lanes, ladders in the holes, **jump only** | `hitw-1/frames/classic_wall_D6.png` |
| Classic D7 | `P#...P..P...#P` / `.#...#..#...#.` / `.############.` / `.#...#..#...#.` / `##############` | 2 jump-through lanes, **jump only** | `hitw-1/frames/classic_wall_D7.png` |
| Fishbowl D1 | `#P.P..#P.#P..P` / `#<>#...#..#<>#` / `#<>########<>#` / `#<>########<>#` / `#..#...#..####` | 2 walk-through lanes, 2 jump-through lanes, ladders in the holes | `hitw-1/frames/fishbowl_wall_D1.png` |
| Fishbowl D2 | `P..P...P..P##P` / `#<>#...#<>#..#` / `#<>###.#<>#..#` / `#<>#.###<>####` / `#<>#...#<>####` | 5 walk-through lanes, ladders in the holes | `hitw-1/frames/fishbowl_wall_D2.png` |
| Fishbowl D3 | `P##P..#P..P##P` / `#<>#..##..#<>#` / `#<>########<>#` / `#<>########<>#` / `####......#..#` | 2 walk-through lanes, 2 jump-through lanes, ladders in the holes | `hitw-1/frames/fishbowl_wall_D3.png` |
| Fishbowl D4 | `#P#.P..P..P..P` / `#..##..#..#..#` / `#..##..####..#` / `##..#..#..#.##` / `#####..#######` | 2 walk-through lanes, 2 jump-through lanes | `hitw-1/frames/fishbowl_wall_D4.png` |
| Fishbowl D7 | `P##.P....P.##P` / `##.###..###.##` / `##.#.####.#.##` / `##.#.####.#.##` / `###........###` | 2 walk-through lanes, 2 jump-through lanes | `hitw-1/frames/fishbowl_wall_D7.png` |
| Highrise D1 | `..P..P..P..P..` / `..#..#..#..#..` / `###..#..#..###` / `######..######` / `..#..####..#..` | 2 jump-through lanes, **jump only** | `hitw-1/frames/highrise_wall_D1.png` |
| Highrise D2 | `P..P..P...P..P` / `#..#..#...#..#` / `#<>#..#...#<>#` / `#<>########<>#` / `##############` | 4 jump-through lanes, ladders in the holes, **jump only** | `hitw-1/frames/highrise_wall_D2.png` (shifted: empty bottom row is below the floor) |
| Highrise D3 | `.P..P..P..P..P` / `.#..#..#..#..#` / `.#..#..#..#..#` / `.#..#..#..#..#` / `##############` | 9 jump-through lanes, **jump only** | `hitw-1/frames/highrise_wall_D3.png` (shifted: empty bottom row is below the floor) |
| Highrise D4 | `..............` / `P.P..P..P..P.P` / `#.##########.#` / `#.##########.#` / `#.##########.#` | 2 walk-through lanes | `hitw-1/frames/highrise_wall_D4.png` |
| Highrise D5 | `P##P##P##P##P#` / `#..#..#..#..#.` / `#.##..#..#..#.` / `#..#..#..##.#.` / `#..#..#..#..#.` | 8 walk-through lanes | `hitw-1/frames/highrise_wall_D5.png` |
| Highrise D6 | `..............` / `P.#P..P.#P#.#P` / `#.#####.###.##` / `#.#####.###.#.` / `#.##.##.###.#.` | 4 walk-through lanes | `hitw-1/frames/highrise_wall_D6.png` |
| Highrise D7 | `....P..P..P..P` / `....#..#..#..#` / `....##########` / `....##########` / `....##########` | 4 walk-through lanes | `hitw-1/frames/highrise_wall_D7.png` |

### Extreme (20 walls)

| Wall | Holes (rows top to bottom, `#` slime, `P` piston cap, `<`/`>` ladder) | Passable lanes | MCC reference frame |
|---|---|---|---|
| Classic X1 | `P....P..P....P` / `#.##########.#` / `#.#..#..#..#.#` / `#.#..#..#..#.#` / `##############` | 8 jump-through lanes, **jump only** | `hitw-1/frames/classic_wall_X1.png` |
| Classic X2 | `P##..P..P..##P` / `..#..#..#..#..` / `..##########..` / `.>#..#..#..#<.` / `.############.` | 2 walk-through lanes, 2 jump-through lanes, ladders in the holes | `hitw-1/frames/classic_wall_X2.png` |
| Classic X3 | `P<>P...P..P<>P` / `#<>#...#..#<>#` / `#<>########<>#` / `#<>########<>#` / `####......####` | 4 jump-through lanes, ladders in the holes, **jump only** | `hitw-1/frames/classic_wall_X3.png` = Fishbowl D5 |
| Classic X4 | `P....P..P....P` / `###<.####.>###` / `..#<.#..#.>#..` / `..#..#..#..#..` / `##############` | 10 jump-through lanes, ladders in the holes, **jump only** | `hitw-1/frames/classic_wall_X4.png` |
| Classic X5 | `P..P..P..P..P.` / `#..####..#..#.` / `####..########` / `####..########` / `#..####..#..#.` | 2 jump-through lanes, **jump only** | `hitw-1/frames/classic_wall_X5.png` |
| Classic X6 | `P.P#.P..P.#P.P` / `#.##.#..#.##.#` / `#.##.#..#.##.#` / `#....####....#` / `##############` | 4 jump-through lanes, **jump only** | `hitw-1/frames/classic_wall_X6.png` (shifted: empty bottom row is below the floor) |
| Classic X7 | `P..P...P..P..P` / `#..#...#..#..#` / `######.#######` / `######.#######` / `#..#......#..#` | 1 walk-through lane | `hitw-1/frames/classic_wall_X7.png` |
| Fishbowl X1 | `..P..P..P..P..` / `..##########..` / `###..#..#..###` / `###<>#<>#<>###` / `..##########..` | 6 jump-through lanes, ladders in the holes, **jump only** | `hitw-1/frames/fishbowl_wall_X1.png` |
| Fishbowl X2 | `..............` / `#P..P##P##P..P` / `##..#######..#` / `##..#######..#` / `##############` | 4 jump-through lanes, **jump only** | `hitw-1/frames/fishbowl_wall_X2.png` |
| Fishbowl X3 | `P....P..P....P` / `###<.#..#.>###` / `..#<.####.>#..` / `###..#..#..###` / `..##########..` | 4 jump-through lanes, ladders in the holes, **jump only** | `hitw-1/frames/fishbowl_wall_X3.png` |
| Fishbowl X4 | `..#P##P##P##P#` / `..############` / `..############` / `..############` / `..............` | 2 walk-through lanes | `hitw-1/frames/fishbowl_wall_X4.png` |
| Fishbowl X5 | `#P##P##P##P#..` / `############..` / `############..` / `############..` / `..............` | 2 walk-through lanes | `hitw-1/frames/fishbowl_wall_X5.png` |
| Fishbowl X6 | `..P..P..P..P..` / `..#.######.#..` / `####.#.##.####` / `..#.##..##.#..` / `##############` | 1 jump-through lane, **jump only** | `hitw-1/frames/fishbowl_wall_X6.png` |
| Fishbowl X7 | `P....P..P....P` / `#....#..#....#` / `###<>####<>###` / `#....#..#....#` / `##############` | 4 jump-through lanes, ladders in the holes, **jump only** | `hitw-1/frames/fishbowl_wall_X7.png` |
| Highrise X1 | `P..P..P...P..P` / `#..#..#...#..#` / `#.##########.#` / `#.##########.#` / `#..#..#...#..#` | 2 walk-through lanes | `hitw-1/frames/highrise_wall_X1.png` |
| Highrise X3 | `P.....P......P` / `#.....#......#` / `##############` / `..#########...` / `..............` | 5 walk-through lanes | `hitw-1/frames/highrise_wall_X3.png` |
| Highrise X4 | `..P..P..P..P..` / `.###>####<###.` / `.###>####<###.` / `.###>####<###.` / `..#..#..#..#..` | 4 walk-through lanes, ladders in the holes | `hitw-1/frames/highrise_wall_X4.png` |
| Highrise X5 | `...P..P..P..P.` / `...#..#..#..#.` / `...#..#..#..#.` / `...###########` / `##############` | 3 jump-through lanes, **jump only** | `hitw-1/frames/highrise_wall_X5.png` (shifted: empty bottom row is below the floor) |
| Highrise X6 | `P#<>P##P##P##P` / `##<>##########` / `##<>##########` / `##<>##########` / `..............` | 2 walk-through lanes, ladders in the holes | `hitw-1/frames/highrise_wall_X6.png` |
| Highrise X7 | `..P..P..P..P..` / `..#..#..#..#..` / `######..######` / `######..######` / `..#..#..#..#..` | 2 walk-through lanes | `hitw-1/frames/highrise_wall_X7.png` |
