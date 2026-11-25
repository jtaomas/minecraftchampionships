# A6b — Parkour Tag arena `MG-PT-01`

Status: **research complete**. No game code, no build, no paste, no registry edit, no server write.
Owner: A6b. Written folder: `game-plugins/research/existing_maps/minigames/parkour_tag/` only.
Source world: `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3` (1.17.1, DataVersion 2730).
Live world: `mcc_minigames` (not touched; no RCON used).

Evidence scale: **H** = directly observed block, command, sign NBT, scoreboard NBT or rendered pixel; **M** = inference from two independent observations; **L** = guess. Every substantive claim is tagged.

---

## 1. What this arena is

`MG-PT-01` is a **fan-made Parkour Tag party arena** (**M**: source name “daniellaur’s minigames” + simplified 1-hunter format), not an MCC event map (**L**: no MCC branding or geometry match). It is a 20 × 20 × 16 play box sunk into a circular stadium bowl, with four raised tiers, two opposed start rooms, an underground waiting lobby, a 60-second timer, and **1 hunter vs 1–4 runners**. **H** (geometry + commands + signs)

It is fully self-contained: all 123 command blocks, all 25 signs and all game state live inside the envelope. There is no dependency on the global command room except the hub entry pad. **H**

| Fact | Value | Conf. |
|---|---|---|
| Full copy envelope | `x 1035..1078`, `y 0..58`, `z 972..1017` (44 × 59 × 46) | H |
| Non-air blocks in envelope | **8,375** | H |
| Playable arena box | `x 1050..1071`, `y 10..26`, `z 984..1005` (22 × 17 × 22) | H |
| Arena interior (air) | `x 1051..1070`, `y 11..26`, `z 985..1004` (20 × 16 × 20) | H |
| Standable cells in the arena box | **666** (242 at ground `y 11`) | H |
| Start rooms | north `x 1057..1065, y 15..21, z 979..984`; south `x 1057..1065, y 15..21, z 1005..1010` | H |
| Gallery / spectator deck | `x 1043..1078`, `y 27..39`, `z 977..1012`; floor plain at `y 27` | H |
| Underground waiting lobby / command machine | `x 1043..1071`, `y 0..9`, `z 981..1006`; lobby room `x 1050..1066, z 982..988` | H |
| Banner | `x 1035..1036`, `y 48..58`, `z 972..1017` (432 blocks) | H |
| Entry from hub | `1061 28 995` (gallery, on the barrier roof) | H |
| Runner start | `1061 17 982` | H |
| Hunter start | `1060 17 1007` | H |
| Command blocks | **123** = 36 impulse + 52 chain + 35 repeating; 78 `auto=1`, 45 `auto=0` | H |
| Command clusters | **CB-16 (121)** at `x 1044..1071, y 1..9, z 983..1003`; **CB-17 (2)** at `x 1050..1052, y 12, z 979` | H |
| Signs | **25**, all front text, 0 click events | H |
| Entities | **0** inside the envelope | H |
| Containers | **1 storage barrel + 2 decorative lecterns = 3 container block entities; all empty** (`Items: []`). Barrel at `1067 12 992`; lecterns at `1049 28 993` / `1049 28 996` (each holds a guide Book, not loot). A14 source scan: 34 container block entities world-wide, 21 empty, 13 Sky Battle loot. | H |
| Datapack | none; world uses vanilla command blocks | H |

The live world is `mcc_minigames`; A6 independently counted 942 command blocks world-wide and 123 in this arena, and this scan matches. **H** (A6 `data/commands.csv` + this folder's `data/pt_commands.csv`)

---

## 2. Envelope breakdown

The copy box is a stadium, not just the play box. From bottom to top:

1. **Command machine + waiting lobby** (`y 0..9`, 683 blocks). A technical basement under the west half of the arena: white-concrete floor, dead-coral pattern, 121 command blocks, repeaters, redstone, 4 birch buttons, 23 signs. **H**
2. **Arena box** (`y 10..26`, 3,130 blocks). The 20 × 20 play volume with the dead-coral floor (`y 10`), four raised tiers of platforms, and a 400-block barrier roof at `y 27`. **H**
3. **North / south start rooms** (`y 15..21`, outside the arena wall at `z 979..984` and `z 1005..1010`). 9 × 6 outer, 7 × 5 interior, cyan-terracotta walls, dead-coral floor, spruce-stair bench, iron door frame, 4 × 2 glass door into the arena. **H**
4. **Gallery + seating bowl** (`y 27..39`, 1,796 blocks). The `y 27` floor is a flat ring of white concrete / smooth quartz around the invisible barrier roof; the seating rises in gray-concrete tiers to `y 39`, trimmed with smooth-quartz stairs/slabs and brick slabs. **H**
5. **Banner** (`y 48..58`, 432 blocks). A 2-thick, 46-long red/black/quartz "PARKOUR TAG" sign mounted on the west rim by a red/black arm at `y 50` (`x 1037..1047`). **H**

The envelope is **13 chunk columns** (`x 64..67`, `z 60..63`), all in region `r.2.1`. **H** (A6 `data/regions.csv`)

---

## 3. Exact course layout

### 3.1 The box and the roof

- The arena is a 22 × 22 outer box (`x 1050..1071`, `z 984..1005`) with 1-block side walls (cyan terracotta at the base, stone/copper trim above) and thicker north/south door walls; the interior is 20 × 20 (`x 1051..1070`, `z 985..1004`). **H**
- The floor is `y 10` dead-horn-coral block (`360` of the 441 coral blocks; the rest are in the start rooms and the machine), with blue ice and obsidian/slime insets. Players stand at `y 11`. **H**
- The roof is a **400-block barrier plane at `y 27`**, exactly covering `x 1051..1070, z 985..1004`. It is invisible in game and flush with the `y 27` gallery floor, so spectators walk over the arena on an invisible “glass floor”. **H**
- Playable air is `y 11..26` (16 blocks); the highest standable surface is `y 25`, because `y 26` slabs leave only one block of headroom below the barrier. **H**
- The arena reads as a single connected volume: there is **no void, no kill plane and no mid-air barrier partition** inside the box. **H**

### 3.2 Vertical tiers

Counts are standable cells in the arena box, from the block export. **H**

| Tier | Surface y | Main surfaces | Cells |
|---|---|---|---:|
| Ground | 11 | 20 × 20 dead-coral floor, NW ice rink, S slime pad, four corner structures | 242 |
| Lower catwalks | 12–15 | deepslate-tile island, east iron-trapdoor catwalk, centre-west trapdoor pad, NE copper shelf | 93 |
| Mid | 16–19 | start-door landings, west/centre trapdoor pads, east pad, copper north ledge, SE room door ledge | 126 |
| Upper-mid | 20–21 | west/centre/south trapdoor and copper pads (the start-tower roofs are outside the arena box and not counted) | 69 |
| Top | 23–25 | SW/NE brick balconies, centre brick plus, SE spruce grandstand; the NW brick roof is decorative | 136 |
| **Total** | | | **666** |

The tiers are not floors of a building; they are a **climbing frame**: platforms hang off the four walls and a central mast, with ladders, stairs and trapdoors connecting them. **M** (block pattern + render)

### 3.3 Ground floor (`y 10`, stand `y 11`)

- **Open area:** 242 of 400 plan cells are standable at `y 11`; the remaining 158 are occupied by the central mast, the four corner structures, and low walls/props. **H**
- **NW blue-ice rink:** `x 1051..1060, z 985..986` (10 × 2) plus `x 1051..1052, z 987..991` (2 × 5) — an L-shaped 30-block rink along the north and west walls. Slippery; the fastest ground line around the NW corner. **H**
- **South slime/obsidian pad:** obsidian ring `x 1059..1062, z 1002..1005` around a 2 × 2 slime core at `x 1060..1061, z 1003..1004`. A bounce pad on the floor. **H**
- **Second slime pad:** 2 × 2 slime at `x 1056..1057, z 998..999`, `y 13` (a raised bounce pad, 2 blocks above the floor). **H**
- **Red concrete pads:** `x 1069..1070, z 994..996` at `y 11` (5 blocks), `x 1065..1066, z 996..997` at `y 12` (4), `x 1060..1061, z 996..997` at `y 13` (4). Small warm accents/step markers. **H**
- **Central mast footprint:** a roughly 7 × 11 stone-brick/deepslate/copper mass at `x 1057..1063, z 986..996`, rising to the top plus. It is not solid: it contains the deepslate-tile island, ladder shafts, trapdoor pads and nooks. **H**
- **Corner structures:**
  - **NW compartment** `x 1052..1057, z 984..990`: stone-brick walls with oak-slab gaps; ladder at `1052 987` (y 14–15). **H**
  - **NE reactor** `x 1063..1066, z 986..989`: cyan/copper box with a sealed 2 × 2 air pocket at `1064..1065, z 987..988, y 11..12`; decorative dead end. **H**
  - **SW closet** `x 1051..1056, z 999..1004`: stone-brick/deepslate walls, iron door at `1054 1003` (y 12–13), copper/stone details. **H**
  - **SE pit room** `x 1064..1069, z 1000..1004`: stone-brick/copper walls, iron door at `1067/1068 999` (y 18–19), floor 7 blocks below the door, pressure plate at `1070 14 1003`. **H**

### 3.4 Lower catwalks (y 12–15)

- **Deepslate-tile island:** `x 1063..1065, z 992..994`, `y 12`, 9 cells. A step up from the floor in the centre-east. **H**
- **East grated scaffold:** a cluster of iron trapdoors along the east wall at `x 1064..1070, z 991..998` and `z 1002..1004`, `y 11..13`; the largest single run is `x 1064..1067, z 995..998` at `y 13`. It is a scaffold, not one continuous catwalk. **H**
- **Centre-west trapdoor pad:** `x 1055..1058, z 997..1000` at `y 14`, 12 cells, above the second slime pad. **H**
- **NE copper shelf:** `x 1063..1065, z 985..989` at `y 14`, 9 cells, outside the NE reactor. **H**
- **West stone-stair ramp:** `x 1051..1053, z 989..992` at `y 14`, 5 cells. **H**

### 3.5 Mid platforms (y 16–19)

- **Runner door landing:** stone-brick step `x 1057..1060, z 985..986` (`y 16`), copper parapet `x 1057..1062, z 984..987` (`y 17`), stone stairs `x 1056..1060, z 985..987` (`y 14`). The door at `x 1059..1062, y 17..18, z 984` opens onto this ramp. **H**
- **Hunter door landing:** stone-brick platform `x 1062..1067, z 1003..1004` (`y 16`), stone stairs `x 1059..1062, z 1003..1004` (`y 15`), copper shelf `x 1058..1063, z 1002..1004` (`y 20`). The door at `x 1059..1062, y 17..18, z 1005` opens onto this ramp. **H**
- **West ladder shaft:** ladders at `1053 994` (y 16–19) and `1056 994..996` (y 16–17); iron trapdoor pad `x 1059..1062, z 995..998` (`y 20`, 12 cells) and `x 1051..1053, z 995..999` (`y 20`, 13 cells). **H**
- **East catwalk:** ladder `1070 992` (y 13–15), iron trapdoor pad `x 1068..1070, z 993..997` (`y 19`, 13 cells), deepslate ledge `1070, z 986..989` (`y 19`, 4 cells). **H**
- **Central nooks:** iron trapdoors at `x 1057..1059, z 994` (`y 18`), `x 1056, z 991..993` (`y 18`), `x 1060..1062, z 989..991` (`y 16`), `x 1063..1065, z 992..994` (`y 16–17`). **H**

### 3.6 Top balconies and grandstand (y 23–25)

- **NW brick roof (decorative):** `x 1051..1056, z 985..992`, 33 brick slabs at `y 26`. The barrier roof at `y 27` leaves only one block of headroom, so this is a **roof, not a usable balcony**; only 2 standable cells exist at `y 25` on its edge. The stone-stair approach at `x 1051..1055, z 986..989` (`y 24`) reaches the roof edge but not a playable deck. **H**
- **SW brick balcony:** `x 1051..1055, z 999..1004`, brick slabs at `y 24..25`, L-shaped. **H**
- **NE brick balcony:** `x 1068..1070, z 985..992`, brick slabs at `y 22..23`; 24 standable cells at `y 23` (3 × 8). **H**
- **Centre brick plus:** `x 1056..1060, z 990..994`, brick slabs at `y 24..25` (20 cells), supported by the central mast. **H**
- **SE spruce grandstand:** `x 1064..1070, z 998..1004` — stone-brick edge at `z 998` (`y 22`), spruce stairs at `z 999/1004` (`y 23..24`), spruce stairs/slabs at `z 1000..1003` (`y 25`, 28 cells). It reads as a 3-tier staircase rising from the east catwalk to the roof of the SE room. **H**
- **Start-tower roofs:** cyan-terracotta caps at `x 1058..1063, z 980..983` and `z 1006..1009`, `y 21`, 24 cells each. They are standable but isolated from the arena by a 4-block vertical gap; **M** that they are decorative/out-of-bounds rather than intended route.

### 3.7 Climbables and gaps

- **Ladders (15):** `1052 987` (y14–15), `1053 986` (y14–15), `1070 992` (y13–15), `1053 994` (y16–19), `1056 994..996` (y16–17), `1051 988` (y19); these 15 blocks are all the ladders in the arena. **H**
- **Stone-brick stairs (66):** the arena sets are `x 1051..1053, z 989..992` (y14), `x 1056..1060, z 985..987` (y14), `x 1059..1062, z 1003..1004` (y15–16) and the SE edge `x 1064..1070, z 998` (y22). The rest are gallery/banner. **H**
- **Spruce stairs (38):** start-room benches (y16) and the SE grandstand (y23–25). **H**
- **Iron trapdoors (93):** one-block-thick grated catwalk surfaces at y11–20; the main runs are `x 1064..1070, z 993..998` (y13) and `x 1068..1070, z 993..997` (y19). **H**
- **Slime (8):** south floor 2 × 2 at `1060..1061, 1003..1004`; centre 2 × 2 at `1056..1057, 998..999` (y13). **H**
- **Blue ice (34):** 30 in the NW rink; 4 decorative at `1064..1065, z 987..988, y 17` inside the NE reactor. **H**
- **Gaps:** there is **no void and no kill plane** inside the arena. A failed jump lands on the ground floor `y 11`; fall damage is off. The “gaps” are vertical drops of 2–14 blocks and horizontal jumps of 1–4 blocks. The usable top perches are separated by 4–11 blocks of air, so there is no top-level lap. The start-tower roofs are 4 blocks above the door ledges with no ladder/stairs. **H** geometry / **M** jumpability.

### 3.8 Start rooms

| | Runner (north) | Hunter (south) |
|---|---|---|
| Outer box | `x 1057..1065, y 15..21, z 979..984` | `x 1057..1065, y 15..21, z 1005..1010` |
| Interior | `x 1058..1064, z 979..983` | `x 1058..1064, z 1006..1009` |
| Floor | dead coral `y 15`, spruce bench `y 16` | dead coral `y 15`, spruce bench `y 16` |
| Spawn | `1061 17 982` | `1060 17 1007` |
| Door | `x 1059..1062, y 17..18, z 984` | `x 1059..1062, y 17..18, z 1005` |
| Door fill | black → red → yellow → lime glass → air | same |
| Roof | cyan cap `y 20..21` | cyan cap `y 20..21` |

All **H**. The two rooms are mirror images across the arena centre. The door is a 4 × 2 opening; the `fill` commands change the glass colour during the countdown and then clear it. **H**

### 3.9 Loops and routes

This is a **non-linear arena**, not a checkpoint course. The geometry supports several loops; the ones below are the natural lines, not an authored script. **M**

**Runner loop (example):**
`1061 984` door → `1060 987` landing → `1057 988` → `1054 988` → `1052 990` → `1052 993` (west ladder) → `1054 995` → `1057 994` (central ladder) → `1060 992` → `1063 993` (deepslate island) → `1065 995` → `1068 997` (east catwalk) → `1069 1001` (SE stairs) → `1067 1003` (SE room door) → `1064 1003` → `1061 1001` → `1059 999` → `1057 996` (central mast) → `1056 993` → `1058 990` → `1060 987` → back to `1061 984`.

This loop uses the ice rink (west), the central ladder (middle), the east trapdoor catwalk and the SE stair. **M** (traced on the block plan)

**Hunter intercept (example):**
`1060 1005` door → `1061 1003` → `1063 1001` → `1065 999` → `1067 997` (east catwalk) → `1069 995` → `1069 992` → `1067 990` → `1064 988` (NE reactor) → `1062 987` → `1061 985` → `1061 984` (runner door).

The shortest graph path from hunter spawn to runner spawn is **11 moves**, using 4-block jumps and 1-block rises; it runs over the mid/upper pads rather than the floor. **M** (graph result, not playtested)

**Why the hunter can cut:** the arena is only 20 × 20; the central mast and the east catwalk provide vertical shortcuts, and the hunter has Strength 100 + glowing runners. The runner's advantage is the ice rink, the slime bounce and the separate top perches (NE, SW, centre plus, SE grandstand). **H** effects/geometry, **M** tactic.

### 3.10 Hiding spots

| Spot | Bounds | Access | Assessment |
|---|---|---|---|
| NW compartment | `x 1052..1057, z 984..990` | open floor side, oak-slab gaps, ladder `1052 987` | candidate nook; low headroom; **M** intent |
| SW closet | `x 1051..1056, z 999..1004` | iron door `1054 1003` (1 block above floor) | small closet; **H** geometry, **M** intent |
| SE pit room | `x 1064..1069, z 1000..1004` | iron door `1067/1068 999` (7 blocks above floor) | trap/pit; pressure plate `1070 14 1003` may open the door from inside; **M** |
| Central mast nooks | `x 1057..1063, z 986..996` | ladders/trapdoors at several levels | route junctions, not sealed rooms; **H** |
| NE reactor | `x 1063..1066, z 986..989` | none — sealed 2 × 2 air pocket | decorative dead end; **H** |

**Glow defeats hiding:** the command `effect give @a[level=6,scores={death_parkourtag=0}] glowing 1 1 true` runs every tick. Every living runner is outlined through walls. The rooms are therefore **escape routes/traps**, not stealth spots. **H** command, **M** design intent.

### 3.11 Spectator / lobby

- **Hub entry:** the main lobby’s orange carpet pad at `-212 10 975` sets XP level 4 and teleports to `1061 28 995`. **H** (A6 arena index + hub command)
- **Arrival:** `1061 28 995` is the **centre of the invisible barrier roof** over the arena, not the outer ring. The player stands on “glass” 16 blocks above the course, with the seating bowl around them. **H** (barrier plane + entry y)
- **Role pads:** runner `1049 27 990` (chiseled stone bricks), hunter `1049 27 999` (chiseled polished blackstone). Both are flush with the `y 27` deck. **H**
- **Instruction lecterns:** two lecterns at `1049 28 993` and `1049 28 996`, on the west gallery walkway between the pads. Each holds a written book titled “Parkour Tag” by daniellaur: *“Welcome to Parkour Tag. In this game the Runners try to stay out of the hands from the hunter. When every runner got tagged within a minute, the game ends. The hunter can start the game by pressing the right button. Stay high!”* **H** NBT. This confirms the 60-second/all-runners win reading; “the right button” is **M** one of the four count buttons.
- **Waiting lobby:** the pads teleport to `1064 1 985` (runner) / `1058 1 985` (hunter), an underground room at `x 1050..1066, z 982..988` with signs and four count buttons. **H**
- **Seating:** the bowl rises from `y 28` to `y 39`; the top row is brick slabs, the lower rows smooth quartz/white concrete, the outer shell gray/black concrete with red trim. **H**
- **Spectator view:** from the gallery, the barrier roof is invisible; the whole course is visible from above. The bossbar selector, however, only covers `y 15..20`, so gallery spectators are not added to the timer. **H** selector, **M** consequence.

### 3.12 Checkpoints

There are **no mid-course checkpoints**. `data/checkpoints.csv` lists the state transitions instead: hub pad → gallery → role pad → waiting lobby → start door → timer → win trigger → reset. **H**

---

## 4. Game loop from commands

Full machine export: `data/commands.csv` (123 rows, exact command, trigger, role, 1.21 risk, notes); the canonical-name alias is `data/command_blocks.csv` (same 123 rows, same schema). Summary below.

### 4.1 State machine: XP level = role

The map uses the player’s **XP level** as the state variable. There is no scoreboard role objective. **H**

| Level | State | Set by | Evidence |
|---:|---|---|---|
| 4 | lobby / unselected / post-round | hub pad, end reset | `xp set @a[level=6] 4 levels`, `xp set @a[level=7] 4 levels` |
| 5 | runner selected, in waiting lobby | runner pad | `xp set @s 5 levels` |
| 6 | runner in game | start chain | `xp set @s 6 levels` |
| 7 | hunter (waiting or in game) | hunter pad | `xp set @s 7 levels` |

Consequences:
- XP is a **fragile state variable**: any XP gain/loss (or another system setting levels) changes the player’s role. The hub also uses XP levels for its pads. **M**
- **Team model:** lobby players are on `pve` (`AllowFriendlyFire=0`). At round start, `PT-C060` places a redstone block at `1057 6 1001`; that powers the repeater at `1056 6 1001`, the `team empty Parkour_Tag` block at `1055 6 1001`, and the `team join Parkour_Tag @a[level=6]` chain at `1054 6 1001`. Runners move to `Parkour_Tag` (gold) while the hunter stays on `pve`, so pve-vs-Parkour_Tag damage is legal. At reset, `PT-C072/073` remove the redstone block and `PT-C092/093` rejoin level-6 players to `pve`. This is a **runtime redstone dependency**, not a static team assignment; if the block is missing, everyone stays on `pve` and tags fail. **H** commands, **M** exact timing/facing.

### 4.2 Lobby flow

1. Hub pad `-212 10 975` → `xp set 4` → `tp 1061 28 995`. **H**
2. Walk to a role pad on the `y 27` deck. **H**
3. Runner pad `1049 27 990`: level 4 → 5; next tick, level 5 on the same block → `tp 1064 1 985` (waiting lobby). **H**
4. Hunter pad `1049 27 999`: level 4 → 7; next tick, level 7 on the same block → `tp 1058 1 985`. A repeating block also places a 2-high barrier at `1049 28..29 999`, locking the pad so only one hunter can be selected. **H** commands, **M** tick order.
5. A glowstone pad at `1058 0 982` in the waiting lobby removes the barrier, unlocking the hunter slot. **H**
6. In the waiting lobby, `clear @a[level=5]` and `clear @a[level=7]` run every tick (empty inventory; removes the hub carrot-on-a-stick). **H**
7. Saturation is refreshed for levels 4, 5, 6 and 7; no hunger. **H**

### 4.3 Runner-count selection (1–4)

Four birch buttons on the waiting-lobby wall at `1057 1 984`, `1057 1 986`, `1057 3 984`, `1057 3 986`. **H**

The signs map them (with the sign facing west, left = south):

| Button | Sign | Runner count | Armed threshold column |
|---|---|---:|---|
| `1057 1 984` | “Four Runners ------>” | 4 | x 1064 |
| `1057 1 986` | “Three Runners <------” | 3 | x 1066 |
| `1057 3 984` | “Two Runners ------>” | 2 | x 1068 |
| `1057 3 986` | “One Runner <------” | 1 | x 1070 |

Each button pulse:
1. clears the redstone row at `y 5, z 1005, x 1064..1070`;
2. places one redstone block in that row at the chosen x (1064/1066/1068/1070), arming exactly one kill-threshold column. **H** (commands + signs at `y 4, z 1006` labelling the columns)

The guide book on the gallery lecterns says “the hunter can start the game by pressing the right button”; the four count buttons are the only start inputs. **M** which button “right” means.

### 4.4 Countdown and doors

- At the start of the countdown, both doors are filled with **black stained glass** (`fill 1059 18 1005 1062 17 1005 black_stained_glass` and the north equivalent). **H** commands, **M** exact trigger tick.
- Three countdown stages then replace the glass with **red → yellow → lime** and play note-block hat sounds; the last stage plays a bell and clears the glass. **H** (commands at `x 1044/1048/1052, y 1..8`; titles `>3<`, `>2<`, `>1<`)
- The countdown is a redstone/command chain; the exact 1-second spacing is set by repeaters in the machine, not by a scoreboard. **M** (repeater layout; no explicit `schedule` command)
- At the end: level 5 → 6, runners teleport to `1061 17 982`, hunter teleports to `1060 17 1007`, timer = 60, bossbar created/max 60/style notched_6/players added. **H**

### 4.5 Timer and bossbar

- `fakeplayer timer_parkourtag` starts at **60**. **H**
- One command displays the value (`scoreboard players remove … 0` stored into the bossbar); another decrements it by 1 (`… 1`) on the 1-second clock. **H**
- Bossbar id `minecraft:1`, name `{"text":"Parkour Tag - Timer"}`, max 60, style `notched_6`. **H**
- Bossbar players: `@a[x=1057,y=15,z=979,dx=8,dy=5,dz=32]` → box `x 1057..1065, y 15..20, z 979..1011`. The list is set once by an impulse block in the start chain. If that block runs after the start-room teleports (likely, but the exact chain order is not stored in the export), the players are in the box and are added; if it runs before the teleports, the box contains nobody and the bossbar gets no players. A player outside the box at that moment (late join, gallery spectator) is missed; the box does not cover the upper perches (y 21–25) or the E/W edges, which would only matter if the list were refreshed. **H** selector, **M** order/effect.
- At timer 0: a repeating block places redstone at `1068 5 987`, which fires the **Runners win** chain. **H**

### 4.6 Tag / kill

- The hunter gets **Strength 100**, 1 s, refreshed every tick. With an empty inventory and no armor on runners, a bare fist one-shots. **H** command; **M** one-shot in vanilla (Strength 100 is far above the 20 HP of an unarmored player).
- Living runners (`level=6`, `death_parkourtag=0`) get **Glowing 1 s**, refreshed every tick; the hunter sees them through walls. **H**
- The map does not run `scoreboard players add` for kills/deaths. Instead it relies on the objectives’ **criteria**:
  - `death_parkourtag` = `deathCount` (auto-increments on death);
  - `kill_parkourtag` = `playerKillCount` (auto-increments on player kills);
  - `kill_parkourtag2` = `playerKillCount` (a duplicate used for fireworks). **H** (`data/scoreboard.dat` objectives)
- On death: `keepInventory=true`, `doImmediateRespawn=true`, `fallDamage=false`, `fireDamage=false`, `drowningDamage=false`. The runner’s spawnpoint is set to `1061 28 995` (gallery) at round start. **H** gamerules + command
- A dead runner respawns on the gallery at level 6; the role pads only react at level 4, so they cannot re-enter the arena. At round end, `xp set … 4` returns them to the lobby. **H** commands, **M** re-entry in practice.

### 4.7 Win conditions

**Hunter wins** when `kill_parkourtag` equals the selected runner count (1–4). The armed column places redstone at `1070 5 991`, which runs the hunter-win chain: tellraw to the hunter, firework over the hunter, bossbar removal, score resets, teleports, XP reset. **H**

**Runners win** when:
1. the timer reaches 0 → redstone at `1068 5 987` → tellraw to runners/hunter, fireworks over survivors, reset; **H**
2. no level-7 player exists (`execute as @a[level=6] … unless entity @a[level=7]`) → same redstone trigger; **H**
3. the hunter disconnects → same, plus the hunter pad is unlocked. **H** command, **M** disconnect behaviour.

**No other win condition** exists. There is no score, no round count, no side-vs-side; it is one 60-second round. **H**

### 4.8 Reset

The end chain (roughly `x 1064..1071, y 5..9`) does:
- remove bossbar; reset death/kill scores for levels 6/7;
- clear redstone triggers at `1070 5 991` and `1068 5 987`;
- teleport all players to `1061 28 995`;
- set XP 4 for levels 6/7;
- set fakeplayer timer to -1;
- clear the armed-threshold row. **H**

### 4.9 Loadout / effects

| Effect | Target | Duration | Purpose |
|---|---|---|---|
| Saturation | levels 4/5/6/7 | 2 s, refreshed | no hunger |
| Glowing | level 6, `death=0` | 1 s, refreshed | hunter sees living runners |
| Strength 100 | level 7 | 1 s, refreshed | one-hit tag |
| Clear inventory | levels 5/7 | every tick | no items/armor; removes hub carrot |
| Fireworks | winners / survivors | one-shot | celebration |
| `spawnpoint` | level 6 | round start | respawn on gallery |

All **H**. There are **no items, no armor, no checkpoints, no kit chests**. The “Items free lobby” sign is not backed by any stocked container or `give` command in this envelope (the barrel is empty and the lecterns hold only a guide book); **M** that it is leftover/aspirational.

### 4.10 The command machine

- **121 commands in CB-16** under the arena (`y 1..9`), plus **2 in CB-17** at `y 12` (the NW redstone clock). **H**
- The machine is a set of **vertical command columns** (mostly at `x 1044/1048/1052/1056/1060/1064/1066/1068/1070/1071` and `z 984..1003`), wired with repeaters and redstone. The chain order is vertical; a chain block executes after the block facing into it. **H** positions, **M** order.
- The four start buttons feed the four threshold columns; the countdown columns (`x 1044/1048/1052, y 5..8`) hold the 3-2-1 titles and sounds; the `x 1064..1070, y 5, z 1001/1003` grid holds the four kill-threshold checks; the `x 1068, y 5..7, z 986/995` columns hold the runners-win chain; the `x 1066, y 5..7, z 997` column holds the hunter-win chain. **H** positions, **M** role mapping.

### 4.11 Defects and ambiguities (important)

These are direct observations, not fixes.

1. **Team split is a runtime redstone dependency.** `pve` has `AllowFriendlyFire=0`. The runner team split works only while the command-placed redstone block at `1057 6 1001` exists: `PT-C060` places it at round start, the repeater at `1056 6 1001` reads it, `team empty Parkour_Tag` at `1055 6 1001` runs, and the chain at `1054 6 1001` joins level-6 runners to `Parkour_Tag`. `PT-C072/073` remove the block at reset. If the block is not placed (start chain skipped, redstone broken), all players stay on `pve` and **cannot damage each other**; the round ends on the 60 s timer. **H** commands, **M** runtime. This is the biggest single functional dependency in the map.
2. **Broken hunter-win message.** `PT-C100` ends with an extra `}`: `tellraw @s {'text':'[Parkour Tag] The Hunter wins!','color':'gold'}}`. The command fails to parse in 1.17 and 1.21. Runners never see the hunter-win text (the hunter’s own message, `PT-C082`, is fine). **H** exact string.
3. **Timer-stop commands target the wrong score.** `PT-C088..C091` do `scoreboard players set @s timer_parkourtag -1` as the hunter; `@s` is the hunter’s personal score, not `fakeplayer`. The fakeplayer clock is only stopped by the end chain (`PT-C120`). The bossbar may keep counting after the win until the reset chain runs. **H** selector, **M** visible effect.
4. **Bossbar box and trigger order.** The `players` selector covers `y 15..20`; the top perches are `y 23..25` and the E/W edges are outside `x 1057..1065`. As a static list, only players in the box when the command runs are added. The command is an impulse in the start chain; if it runs before the start-room teleports, it adds nobody. **H** selector, **M** order/who is missed.
5. **Fireworks use pre-1.20.5 item NBT.** `FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{…}}}` is rejected by 1.21.1. Six commands are affected (`PT-C026`, `C050`, `C064`, `C074`, `C080`, `C112`). **H** syntax change.
6. **Empty chain block.** `PT-C081` at `1049 5 997` has an empty command. Harmless but evidence of unfinished wiring. **H**
7. **The four threshold columns share one output.** All four place redstone at the same `1070 5 991`. Only the armed column should be powered; if two are powered (leftover redstone, reset failure), the first kill ends the round. **H** commands, **M** failure mode.
8. **Friendly-fire / tag model.** The map detects a tag as a **kill**. If the hunter dies (possible only by player damage or `/kill`), there is no hunter respawn point in the arena; the `unless entity @a[level=7]` check still sees the hunter online at the hub, so the round may hang until the timer. **M**
9. **Glowing runners.** The anti-camp glow is applied to runners, not the hunter. It makes the four “hiding” rooms tactically irrelevant. **H** command, **M** design.
10. **`Items free lobby`** sign has no stocked container or `give` in this envelope (the barrel is empty; the lecterns hold only a guide book). **H** absence, **M** leftover.

---

## 5. Blocks, palette, lighting

Full table: `data/blocks.csv` (68 rows). Palette notes: `data/palette.md`.

- **8,375 blocks, 68 types.** Top shares: gray concrete 22.08%, cyan terracotta 12.81%, stone bricks 8.45%, smooth quartz 7.27%, white concrete 6.08%, dead horn coral 5.27%, barrier 4.79%, brick slab 4.53%, black concrete 3.92%, deepslate bricks 3.56%. **H**
- **Copper family:** 451 blocks (copper 151, exposed 137, weathered 106, oxidised 57) — the route colour. **H**
- **Lighting:** 29 invisible `light` blocks in the arena/start rooms (y 13–23), 1 glowstone + 1 sea lantern in the waiting lobby, 4 soul lanterns in the command room. No visible arena lamps. **H**
- **Barrier:** 401 blocks total: the 400-block roof at y 27 and one banner support at y 50. **H**
- **Containers/entities:** **1 storage barrel + 2 decorative lecterns = 3 container block entities; all empty** (`Items: []`). Barrel at `1067 12 992`; lecterns at `1049 28 993` / `1049 28 996` (each holds a guide Book, not loot). No loot containers, no entities. A14 source scan (`_qa/data/minigames_container_by_arena.csv`): 34 container block entities world-wide (21 empty, 13 Sky Battle loot). **H**

---

## 6. Images

See `REFERENCE_IMAGES.md` for the full index, source bounds and what each image proves. The main evidence set:

- `frames/mg_pt_top.png` — full-envelope top-down (stadium + banner + course).
- `frames/mg_pt_iso_south.png` — full-envelope iso from the south (banner + bowl + course).
- `frames/mg_pt_iso_north.png` — full-envelope iso from the north (back of the bowl).
- `frames/mg_pt_course_top.png` — course-only top-down (20 × 20 plan).
- `frames/mg_pt_course_iso.png` — course-only iso from the south.
- `frames/mg_pt_runner_spawn.png` — runner start room + north door + landing.
- `frames/mg_pt_hunter_spawn.png` — hunter start room + south door + landing.
- `frames/mg_pt_cutaway_south.png` — south/east walls removed, interior from the south.
- `frames/mg_pt_cutaway_north.png` — cutaway from the north.
- `frames/mg_pt_surface_low.png` / `frames/mg_pt_surface_high.png` — lowest/highest standable surface per column.
- `frames/mg_pt_route_schematic.png` — labelled plan with example runner loop and hunter intercept (interpretive).

All were opened and checked, and re-opened in the 2026-11-25 A14 QA pass. **H** for pixel content; route schematic is **M**.

---

## 7. Why the course is readable — and where it is ambiguous

### 7.1 What works for runners

- **One box, one floor.** The 20 × 20 ground floor is continuous and has no void; a missed jump lands on the floor, not in a pit. `fallDamage=false` removes the punishment. **H**
- **Two opposed doors.** The runner and hunter enter from north and south; the door glass flashes red/yellow/lime during the countdown, so both sides see the start. **H**
- **Four corner landmarks + central mast.** The NW compartment, NE reactor, SW closet and SE pit room are distinct silhouettes; the central mast is visible from every floor. A runner can orient by “corner + mast” without signage. **H** geometry, **M** legibility.
- **Three fast ground features.** The blue-ice rink (NW), the south slime pad and the second slime pad (centre) give clear, learnable movement options. **H**
- **Vertical escape routes.** Ladders at `1053 994`, `1056 994..996`, `1070 992` and the SE grandstand give multiple ways to break line of sight and change level. **H**
- **A safe top.** The NE/SW balconies, the centre plus and the SE grandstand are perches with a view; a runner who reaches one can see the hunter coming. The NW brick roof is decorative, not a perch. **H** geometry, **M** tactical.

### 7.2 What works for hunters

- **Glowing runners.** The hunter sees every living runner through walls. There is no search phase; the game is a chase. **H**
- **One-hit tag.** Strength 100 means one clean hit ends a runner; no trade or combo. **H** command, **M** one-shot.
- **Vertical shortcuts.** The east catwalk, central mast and SE grandstand let the hunter cut up/down faster than a runner who commits to the ground loop. **M** (graph/geometry)
- **Small arena.** 20 × 20 is only ~4 seconds of sprint across; a hunter who predicts the loop can intercept at a corner. **M**
- **Start symmetry.** The hunter’s door and the runner’s door are 21 blocks apart across the box; the hunter can beeline to the runner door before the runner reaches the first corner if the runner hesitates. **M**

### 7.3 Where it is ambiguous

- **Value mud.** Copper, dead coral, deepslate and gray concrete sit in a narrow value range; from the gallery the course reads as a brown-pink mass. The cyan wall and white/quartz gallery are the only strong separators. **M** (render observation)
- **Upper perches are isolated.** The gaps between the usable top perches (NE, SW, centre plus, SE grandstand) are 4–11 blocks of air; there is no top-level lap. A runner who goes up must come down the same way, which is a trap against a hunter below. **H** geometry, **M** tactic.
- **NE reactor is a dead end.** The cyan/copper box at `x 1063..1066, z 986..989` is sealed; it looks like a route but is not. **H**
- **SE pit room is a trap.** The iron door at `y 18..19` opens 7 blocks above the floor; entering drops the player in. The pressure plate at `1070 14 1003` is on the wall, not the floor. **H** geometry, **M** intent.
- **Iron-trapdoor catwalks are thin and dark.** The east catwalk is one block wide in places and visually merges with the copper/stone behind it. **M** (render)
- **No route markers.** The only signage is in the lobby/machine, not the arena. The course relies on geometry and the glow. **H** signs.
- **Glow vs hiding.** The map has geometric hiding spots but the glow makes them useless; a runner who expects stealth will be disappointed. **H** command, **M** expectation.
- **Bossbar coverage.** A player who is not in the start-room box when the list is set (late join, wrong teleport order) may not be on the timer bossbar; the box itself does not cover the top perches. **H** selector, **M** effect.

---

## 8. 1.21.1 compatibility risks

The source is **1.17.1** (DataVersion 2730). A 1.21.1 client/server will upgrade the world and run the command blocks; the following need attention.

| Area | Risk | Why | Conf. |
|---|---|---|---|
| Firework NBT | **HIGH** | `FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{…}}}` is the pre-1.20.5 item format; 1.21.1 expects `components` (`minecraft:fireworks`). Six commands fail; no celebration fireworks. | H |
| Broken tellraw | **HIGH** | `PT-C100` has an extra `}`; parse error in any version. Runners never see the hunter-win text. | H |
| Team split | **MED** | Works only if `PT-C060` places the redstone block at `1057 6 1001` and the repeater at `1056 6 1001` reads it. If not, all players stay on `pve` (friendly fire off) and tags fail. | H data / M runtime |
| Sign NBT | **LOW–MED** | 1.17 `Text1..4` is upgraded to `front_text.messages` by DataFixerUpper; colours/glow survive, raw JSON may be re-serialised. | M |
| Bossbar/title/tellraw JSON | **LOW–MED** | 1.21.1 still accepts JSON text components in commands; SNBT is also accepted. The `bossbar add 1 {"text":…}` form should parse. | M |
| `summon firework_rocket` entity id | **LOW** | Entity id unchanged; only the item NBT is the problem. | H |
| `effect give … strength 100` | **LOW** | Amplifier 100 is within the 0–255 range; `glowing`/`saturation` ids unchanged. | H |
| `spawnpoint @s x y z` | **LOW** | Syntax unchanged in 1.21.1. | H |
| `team join/empty` | **LOW** | Syntax and friendly-fire flags unchanged. | H |
| `scoreboard` criteria `deathCount` / `playerKillCount` | **LOW** | Criteria names unchanged; objectives persist. | H |
| `execute store result bossbar …` | **LOW** | Syntax unchanged. | H |
| `bossbar set players @a[x=…,dx=…]` | **LOW** | Box selectors unchanged. | H |
| Redstone / chain timing | **LOW** | Command blocks run in the same tick order; repeaters are unchanged. The 1.21 command-block tick behaviour is the same. | M |
| World height / blocks | **LOW** | All blocks are ≤ y 58 and exist in 1.21.1 (`light`, `barrier`, `blue_ice`, waxed copper, dead coral). | H |
| `maxCommandChainLength` | **LOW** | World gamerule is 65,536; the chains are short. | H |
| `keepInventory` / `fallDamage` / `doImmediateRespawn` | **MED** | The map depends on these; a server that overrides them changes the tag/respawn model. | H |

Minimum 1.21.1 fix list (research only, not implemented): rewrite the six firework commands with `components`; fix `PT-C100`; verify the runtime team-split redstone at `1057 6 1001` and the repeater facing; widen the bossbar selector to the whole arena; consider replacing the XP-level state with scoreboard tags.

---

## 9. Comparison with the current `McmParkourTag` (read-only)

Current implementation: `game-plugins/ParkourTag/src/main/java/com/mcm/games/parkourtag/` (read-only for this report). It is a full side-vs-side tournament game with a procedurally built 41 × 41 arena, 60 s rounds, 5 s prep + 3 s hunter release, scoring, byes, multiple arenas, runner vision, and event-based tags. **H** source read. In the table, facts about either implementation are **H**; the “Verdict” column is **M** judgement.

| Dimension | `MG-PT-01` (this map) | Current `McmParkourTag` | Verdict |
|---|---|---|---|
| Format | 1 hunter vs 1–4 runners, one 60 s round | 2 sides, 1 hunter per side, 4–9 rounds, byes, scoring | Current is a full game; source is a party mode |
| Play area | 20 × 20 × 16, 666 standable cells, 4 raised tiers + ground | 41 × 41 × 22, 5 × 5 feature grid, ledge ring, centre tower | Source is denser/vertical; current is larger and easier to read |
| Presentation | stadium bowl, seating, banner, invisible barrier roof, underground lobby | void world, glass waiting platform, spectators above the arena box | Source is more presentational; current is more functional |
| Role selection | player chooses runner/hunter on pads; 1–4 runner buttons | auto-balanced sides; hunter chosen by least-hunts rotation | Source gives agency; current is fairer |
| Tag model | kill (Strength 100; `deathCount`/`playerKillCount`) | any player hit (damage cancelled; `EntityDamageByEntityEvent`) | Current is cleaner and death-free; source's team split is a runtime redstone chain |
| Runner vision | **hunter** sees glowing runners | **runners** get an ender-eye Runner Vision to see the hunter | Current is MCC-faithful; source is inverted |
| Loadout | no items/armor; saturation + glow + strength | leather armor, blaze-rod tagger, ender eye | Current has clearer roles; source is purer parkour |
| Countdown | glass doors red→yellow→lime + note/bell | 5 s prep + 3 s hunter release + blindness + titles | Source is theatrical; current is controlled |
| Timer/HUD | 60 s bossbar `notched_6`, box-limited players | core timer + sidebar lines + action bars | Source is compact; current is more informative |
| Respawn | runner spawnpoint on gallery; dead runner stays level 6, cannot re-enter | tagged runner → spectator above the arena | Current is cleaner |
| Portability | absolute coordinates; command-block machine | procedural builder; `SPACING=120`; no command blocks | Current is far more portable |
| Failure modes | team split, broken tellraw, old firework NBT, bossbar box, wrong timer selector | code-level, testable, no redstone | Current wins |

**What this map does better:**
- **Density per block.** 666 standable cells in a 20 × 20 footprint; four raised tiers plus a central mast. The current 41 × 41 arena is wider but flatter; per block, the source has more parkour decisions. **H** counts, **M** design judgement.
- **Stadium presentation.** The bowl, banner, barrier roof and underground lobby make the arena feel like an event venue. The current glass waiting platform is functional but visually plain. **H** builds, **M** judgement.
- **Player agency.** Choosing runner/hunter and 1–4 runners is a good casual-party flow; the current auto-balance is better for tournaments but less social. **H** commands, **M** judgement.
- **Countdown theatre.** The glass-door colour cycle and note/bell cues are simple and readable. **H** commands.
- **Pure movement.** No items or armor; the only “loadout” is speed/position. This is closer to the parkour fantasy than the current blaze rod/armor kit. **M** design judgement.

**What it does worse:**
- **Logic robustness.** The team split depends on a command-placed redstone block and a repeater; one tellraw is broken; the timer-stop commands target the wrong score; the firework NBT is obsolete. The current plugin’s event-based tag and code-level state are far more reliable. **H** commands, **M** consequence.
- **Readability of the top level.** The usable top perches are isolated and similar; the NW brick roof is decorative; the NE reactor is a dead end; the SE pit is a trap. The current arena's 5 × 5 feature grid is more legible. **H** geometry, **M** judgement.
- **Stealth.** Glowing runners remove the hiding/spatial-search game; the current runner-vision item preserves it. **H** command, **M** design.
- **HUD coverage.** The bossbar selector is smaller than the arena; a player outside it when the list is set is missed. The current core timer/sidebar covers everyone. **H** selector, **M** who is missed.
- **Scalability.** One hand-built arena, absolute coordinates, one round. The current builder produces N identical arenas and a tournament schedule. **H** source.

**What is reusable (read-only findings, no code written):**
1. **The 20 × 20 four-raised-tier micro-arena template** — four corner features + central mast + two opposed doors. It fits inside a 128³ structure and is cheap to build. **H** bounds.
2. **The stadium envelope** — bowl seating, 2-thick banner, invisible barrier roof flush with the `y 27` gallery. The barrier-as-glass-floor trick is the strongest presentation idea in the map. **H** blocks.
3. **The role-pad + underground waiting room + 1–4 runner buttons** — a complete casual pre-game flow; the four threshold columns are a clever way to arm a kill target. **H** commands.
4. **The countdown glass doors** — red/yellow/lime fills + note-block hat + bell. **H** commands.
5. **The palette** — cyan terracotta + copper oxidation + dead coral + quartz/white; no resource pack. **H** blocks.
6. **The 60 s bossbar styling** (`notched_6`, max 60). **H** commands.
7. **The “no items, pure movement” philosophy** — worth keeping as a mode option even if the current plugin keeps armor/vision items. **M** design.

---

## 10. Open questions

1. **Does the runtime team-split redstone work?** `PT-C060` places a redstone block at `1057 6 1001` and `PT-C072/073` remove it. The static block export does not show the runtime block, so the chain can only be verified in-game (read-only observation). If the block or repeater facing is wrong, tags fail. **H** commands, **M** runtime.
2. **Does the bossbar get players?** The selector is `y 15..20`; it is set once by an impulse in the start chain. If it runs before the start-room teleports, it adds nobody; if after, it adds the start rooms. The top perches are outside the box, but the static list means a player added at start keeps the bossbar when they climb. **H** selector, **M** order/effect.
3. **Is the SE pit room a hiding spot or a trap?** The pressure plate at `1070 14 1003` is on the wall, not the floor; no visible stairs. **H** geometry, **M** intent.
4. **Are the start-tower roofs intended to be reachable?** They are standable (`y 21`) but isolated by a 4-block vertical gap; no ladder/stairs connect them. **H** geometry, **M** intent.
5. **Is the map a one-round party game or a best-of?** The commands implement one 60 s round and reset; there is no round counter or score. **H** commands.
6. **Does the live world still have all 25 signs and 123 command blocks?** A6/A12 confirm 942 command blocks world-wide and A6 counts 123 in this cluster; the sign count was not independently confirmed live. **H** source, **M** live.

---

## 11. Files in this folder

```
README.md                     this dossier
REFERENCE_IMAGES.md           every cited image, bounds, what it shows, what to take
STATUS.md                     done / blocked / open questions
frames/                       full-resolution evidence images (copies of the renders cited in REFERENCE_IMAGES.md)
renders/                      MCMDUMPs + all render/plan outputs
data/commands.csv             123 commands: id, cluster, kind, auto, conditional, role, effect, trigger, 1.21 risk, notes
data/command_blocks.csv       canonical-name alias of commands.csv (same 123 rows, same schema)
data/signs.csv                25 signs: x,y,z, colour, glow, 4 front lines, click event, role, notes
data/checkpoints.csv          state transitions (no mid-course checkpoints)
data/blocks.csv               68 block types: count, share, category, palette role, use, light source
data/entities.csv             empty (no entities in envelope)
data/containers.csv           1 storage barrel + 2 decorative lecterns = 3 container block entities; all empty (lecterns hold a guide Book)
data/route.md                 leg-by-leg route and level notes
data/palette.md               palette, accents, gradients, lighting
data/pt_blocks.csv            the 8,375-block subset used for analysis
data/pt_commands.csv          the 123-command subset
data/pt_signs.csv             the 25-sign subset (raw text)
data/signs_full.csv           full sign NBT (colour, glow, four lines, click)
data/*.py                     one-purpose analysis/export scripts (read-only source)
```

## 12. Explicitly not done

- No Sky Battle, Super Clash, Sky Race or hub analysis. The hub is mentioned only as the entry pad.
- No build, paste, registry edit, command-block edit, or server restart.
- No plugin code.
- No 1.21.1 fix applied; risks are listed only.
- No playtest; route/graph results are geometry, not observed play.
