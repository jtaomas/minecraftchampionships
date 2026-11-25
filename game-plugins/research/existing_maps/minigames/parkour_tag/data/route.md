# MG-PT-01 — route and level notes

All coordinates are absolute in the source world. **H** = direct block/command observation; **M** = inference from two sources; **L** = guess. This is a non-linear arena, not a checkpoint course: there is no single authored route. The legs below are the natural lines the geometry supports.

## 0. The box

- Arena interior: `x 1051..1070`, `z 985..1004`, air `y 11..26`; floor top `y 10`, barrier roof `y 27`. **H**
- Walls: `x 1050` / `x 1071`, `z 984` / `z 1005`; cyan terracotta with stone-brick/copper trim. **H**
- Start rooms: north `x 1057..1065, z 979..984`, south `x 1057..1065, z 1005..1010`; interior floors `y 15`, doors `x 1059..1062, y 17..18`. **H**
- Standable cells: **666** in the arena box, 242 of them at ground level `y 11` (of 400 plan cells). **H**

## 1. Runner line (north start)

1. Spawn `1061 17 982`; face south. The room floor is `y 15` (stand `y 16`); the door sill is `y 17`, one step above the standable floor. **H**
2. Exit the door at `1059..1062, y 17..18, z 984` onto the stone-brick landing `x 1056..1061, z 985..987`. **H**
3. Descend the stone-brick stair ramp (`x 1056..1060, z 985..987`, `y 14..16`) to the ground `y 11`. **H**
4. Choose a loop:
   - **NW ice loop:** west to the blue-ice rink (`x 1051..1060, z 985..986` + `x 1051..1052, z 987..991`), slide north/west, then south along the wall. **H**
   - **Central mast loop:** south-east to the deepslate-tile island `x 1063..1065, z 992..994` (`y 12`), then the ladder cluster `x 1055..1057, z 994..996` (`y 16..17`) to the mid pads. **H**
   - **East catwalk loop:** south-east to the SE stair landing `x 1059..1062, z 1003..1004` (`y 15..16`), then the east grated scaffold `x 1064..1070, z 991..998` (`y 11..13`), ladder `1070 992` (`y 13..15`), east pad `x 1068..1070, z 993..997` (`y 19`). **H**
5. Top exits: SE spruce grandstand `x 1064..1070, z 998..1004` (`y 23..25`); SW brick balcony `x 1051..1055, z 999..1004` (`y 24..25`); centre brick plus `x 1056..1060, z 990..994` (`y 24..25`); NE brick balcony `x 1068..1070, z 985..992` (`y 22..23`). The NW brick structure at `x 1051..1056, z 985..992` is a **decorative roof** at `y 26` with only 2 standable edge cells at `y 25`. **H**
6. The top level is a set of perches, not one continuous ring: the gaps between the usable perches are 4–11 blocks of air, so each perch is reached from below, not by a top-level lap. **M**

## 2. Hunter line (south start)

1. Spawn `1060 17 1007`; face north. Door `1059..1062, y 17..18, z 1005`. **H**
2. Exit onto the stone-brick landing `x 1059..1062, z 1003..1004` (`y 15..16`), descend to the ground `y 11`. **H**
3. Direct intercept: north across the floor to the central mast, or east up the SE stair/grandstand to the top. **M**
4. Shortest computed parkour line from hunter spawn to runner spawn (11 moves, 4-block jumps allowed): `1060 16 1007 → 1059 17 1005 → 1058 20 1002 → 1055 20 1001 → 1053 20 998 → 1053 19 994 → 1056 18 992 → 1056 18 991 → 1057 17 990 → 1059 17 987 → 1059 16 983 → 1061 16 982`. This is a graph result, not a playtested route. **M**
5. The hunter has Strength 100 (one-hit tag) and sees glowing runners, so the optimal line is a beeline; the upper pads let the hunter cut vertically rather than chase on the floor. **H** for effects, **M** for tactic.

## 3. Levels

| Tier | Surface y | Main surfaces | Count (standable cells) |
|---|---|---|---|
| Ground | 11 | 20×20 dead-coral floor; ice rink NW; slime pad S | 242 |
| Lower catwalks | 12–15 | deepslate tile island; east iron-trapdoor catwalk; centre-west trapdoor pad; copper NE shelf | 93 |
| Mid | 16–19 | start-door landings; west/centre trapdoor pads; east pad; copper north ledge | 126 |
| Upper-mid | 20–21 | west/centre/south trapdoor and copper pads (start-tower roofs are outside the box) | 69 |
| Top | 23-25 | SW/NE brick balconies; centre brick plus; SE spruce grandstand; NW brick roof decorative | 136 |

Counts are from the block export, arena box only. **H**

## 4. Climbables and gaps

- **Ladders (15):** `1052 987` (y14–15), `1053 986` (y14–15), `1070 992` (y13–15), `1053 994` (y16–19), `1056 994..996` (y16–17), `1051 988` (y19). Others are wall decoration. **H**
- **Stone-brick stairs (66):** the functional arena sets are at `x 1051..1053, z 989..992` (y14), `x 1056..1060, z 985..987` (y14), `x 1059..1062, z 1003..1004` (y15–16), and the SE edge `x 1064..1070, z 998` (y22). The rest are in the gallery/banner. **H**
- **Spruce stairs (38):** start-room benches (y16) and the SE grandstand (y23–25). **H**
- **Iron trapdoors (93):** grated catwalk surfaces at y11–20; the largest runs are `x 1064..1070, z 993..998` (y13) and `x 1068..1070, z 993..997` (y19). **H**
- **Slime blocks (8):** south floor 2×2 at `1060..1061, 1003..1004`; centre 2×2 at `1056..1057, 998..999` (y13). **H**
- **Blue ice (34):** 30 in the NW rink; 4 decorative at `1064..1065, z 987..988, y 17` inside the NE reactor. **H**
- **Obsidian (15):** frame around the south slime pad, plus a small cluster at `1060..1061, 1002..1005`. **H**
- **Gaps:** there is no void and no kill plane inside the arena; a missed jump lands on the ground floor `y 11`. Fall damage is off. The "gaps" are vertical drops of 2–14 blocks and horizontal jumps of 1–4 blocks. **H**

## 5. Hiding spots (geometric) and the glow caveat

| Spot | Bounds | Access | Reality |
|---|---|---|---|
| NW walled compartment | `x 1052..1057, z 984..990` | open from the floor / oak-slab gaps; ladder `1052 987` | candidate hiding nook; low headroom, **M** intent |
| SW closet | `x 1051..1056, z 999..1004` | iron door `1054 1003` (y12–13), 1 block above the floor | small closet, **H** geometry / **M** intent |
| SE pit room | `x 1064..1069, z 1000..1004` | iron door `1067/1068 999` (y18–19); floor is 7 blocks below the door | trap/pit; pressure plate `1070 14 1003` may open the door from inside, **M** |
| Central mast nooks | `x 1057..1063, z 986..996` | ladders/trapdoors at several levels | route junctions, not sealed rooms, **H** |
| NE reactor | `x 1063..1066, z 986..989` | none — sealed 2×2 air pocket at `1064..1065, 987..988` | decorative dead end, **H** |

**The glow effect defeats hiding:** every level-6 runner with `death_parkourtag=0` is given Glowing 1 s, refreshed every tick. The hunter sees runner outlines through walls. The rooms are therefore escape routes/traps, not stealth spots. **H** command, **M** design intent.

## 6. Sightlines

- From the centre plus (`1056..1060, 990..994`, y25) you see the whole 20×20 floor and all four corner rooms; it is the best observation perch. **H** geometry, **M** tactical value.
- From the ground floor the centre mast blocks the north–south sightline; the four corner rooms and the copper/stone clutter create many local occluders. **H**
- The barrier roof at y27 is invisible; from the gallery the whole arena is visible from above, but from inside the roof reads as open sky. **H**
- The start doors face each other across the 20×20 box; a player at one door can see the other door's glass colour during the countdown. **H**
