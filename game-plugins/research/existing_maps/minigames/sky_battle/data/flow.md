# Gameplay loop — MG-SB-01 Sky Battle

All command strings are in `data/commands.csv`; the raw export is `data/commands_raw.csv`; the readable non-loot list is `data/cb14_noloot.txt`. Coordinates are absolute. **H** = quoted command/block; **M** = inferred order/intent; **L** = guess.

## 0. One-line loop

Hub pad → sky-lobby lantern → stand on a team pad → press Start → 10 s countdown + kit → teleport to your gift box → bridge ~9 blocks to the circular gallery (2-block climb at the rim) → loot the balloon caches and the mid barrel → fight / bridge / shear → the circular border shrinks from r50 to r5 → last team with an adventure player wins → +50 team points, fireworks, reset.

## 1. Entry and team assignment

1. **Hub pad** (outside this arena, A6): `-208 10 970`, yellow carpet, player at XP level 8 → `tp 2000 78 0`. **H** (A6 `README.md` §3).
2. **Arrival** `2000 78 0` is inside a 13×13 barrier cage at y77-80. The cage has no floor; the player falls onto a 3×3 gray-wool pad at y76, inside a gray/gold hanging lantern. **H** (blocks + barrier layers).
3. **Team join pads**: four `*_concrete_powder` blocks at y75:
   - Red `1997 75 0`; Green `2003 75 0`; Blue `2000 75 3`; Yellow `2000 75 -3`. **H**.
   - Repeating command blocks (CB-15 x1001, y15): `execute as @a[level=8] at @s if block ~ ~-1 ~ red_concrete_powder run team join Sky_Battle_Red @s`, then a second block `... if block ... run tp @s 1998 70 0`. Same pattern for Blue (→ `2000 70 2`), Green (→ `2002 70 0`), Yellow (→ `2000 70 -2`). **H**.
4. **Team bays** at y70: 2×2-ish floor of coloured stained glass at y69, feet y70. The four bays sit around the command-block core. **H**.
5. The same four colours exist as stained glass at y69; `level=8` on glass joins the team again, `level=14` on glass resets a winner to level 8. **H** (CB-15 x1003/1004, y15-16).
6. **Signs** at y76 label the four join pads; four signs at y71 say “Press the button to start the game”; one at `1999 67 0` says “Press when the game is bugged”; one at `2000 67 1` says “at 1000 10 10 are all the command blocks”. **H** (`data/signs.csv`).

## 2. Start

1. A stone button at the lantern control panel (five buttons at y70: `1999,-1`, `2000,-1`, `2000,1`, `2001,-1`, `2000,0`) pulses the command room. The exact button→command-block wiring is **M**; the signs identify Start and Bug-reset.
2. **Pre-start cleanup** (CB-14 x929-933, impulse/chain):
   - `tp @a[level=14] 2000 77 0`
   - `gamemode adventure @a[level=14]`
   - `clear @a[level=14]`
   - `kill @a[level=14]`
   - `effect clear @a[level=14]`
   - `xp set @a[level=14] 8 levels`
   - `scoreboard players set @a death_skybattle 0`
   - `scoreboard players set @a[level=14] timer_skybattle 0`
   With `doImmediateRespawn=true` and the lobby `spawnpoint`, the kill respawns winners back in their bay at level 8. **H** commands; **M** that this is the intended “reset winners” trick.
3. **Start trigger**: `execute as @a[level=8,team=Sky_Battle_Red] run xp set @s 9 levels` (and Blue/Green/Yellow) at CB-14 x986-989. Everyone on a team becomes level 9. **H**.
4. **Countdown**: 10 numbered titles `>1<` … `>10<` at x947-982, y10-12, z2-6, with note-block hat/bell sounds. The titles target `@a[level=9]`. **H**.
5. **Kit handout during the countdown** (all `@a[level=9]`):
   - `>3<`: `item replace entity @s hotbar.0 with stone_sword{Unbreakable:1b,CanDestroy:[cobweb]}`
   - later: `hotbar.1 stone_pickaxe{Unbreakable:1b,CanDestroy:[iron_block]}`
   - later: `hotbar.2 shears{Unbreakable:1b,CanDestroy:[red_wool,blue_wool,green_wool,yellow_wool]}`
   - `hotbar.8 cooked_beef 16`
   - `armor.chest iron_chestplate{Unbreakable:1b}`
   - offhand team wool 64 at level 9 (no CanPlaceOn yet), then replaced at level 10-13 by CB-15 with `team_wool{CanPlaceOn:[38 blocks]} 64`. **H** commands; **M** exact order of the two wool grants.
6. **Spawn teleport and cage** at the end of the countdown:
   - `tp @s 1970 23 0` (Red), `2000 23 30` (Blue), `2030 23 0` (Green), `2000 23 -30` (Yellow). **H**.
   - A 3×1 barrier strip is placed at y25 above each spawn: `fill 1970 25 -1 1970 25 1 barrier`, etc. It is recoloured red → yellow → lime → air over the last seconds: `fill … red_stained_glass`, `… yellow_stained_glass`, `… lime_stained_glass`, `… air`. **H**.
   - Effect: a coloured 3-block “traffic light” floats one block above the player's head (y25; feet y23, head y24), then vanishes. It is not a physical cage. **H** geometry; **M** “cage” intent.
7. **Spawnpoint**: `spawnpoint @a[team=Sky_Battle_Red] 1997 70 -1 -90` etc. is set in the lobby, so a death/respawn returns to the bay, not the arena. **H**.

## 3. Kit and loadout

| Slot | Item | NBT / count | Purpose |
|---|---|---|---|
| hotbar.0 | stone sword | Unbreakable, CanDestroy cobweb | melee + clear cobwebs |
| hotbar.1 | stone pickaxe | Unbreakable, CanDestroy iron_block | mine the 4 iron ribbon knots |
| hotbar.2 | shears | Unbreakable, CanDestroy 4 team wools | cut enemy bridges fast |
| hotbar.8 | cooked beef | 16 | food |
| armor.chest | iron chestplate | Unbreakable | only armour piece |
| weapon.offhand | team wool | 64, CanPlaceOn 38 arena blocks | bridging |

No helmet, leggings, boots, blocks other than wool, or potions in the kit. **H**.

The **CanPlaceOn list** (38 ids, identical for all teams except dye):
`red_wool, yellow_wool, green_wool, blue_wool, white_concrete, red_concrete, yellow_concrete, green_concrete, blue_concrete, red_terracotta, yellow_terracotta, green_terracotta, blue_terracotta, red_glazed_terracotta, yellow_glazed_terracotta, green_glazed_terracotta, blue_glazed_terracotta, smooth_quartz, smooth_quartz_stairs, cyan_wool, orange_wool, purple_wool, lime_wool, chain, gray_shulker_box, white_shulker_box, barrel, crafting_table, stripped_spruce_log, spruce_trapdoor, oak_trapdoor, spruce_fence, oak_fence, stone_bricks, stone_brick_stairs, oak_leaves, cobblestone_wall, polished_blackstone`.

The CB-15 kit version replaces the last id with `polished_blackstone_wall` (still 38). **H**. In adventure mode this list is what makes bridging possible: wool can only be placed on the arena's own palette, so players cannot build off the barrier or on the void with arbitrary blocks. **H** mechanics; **M** design intent.

## 4. Loot

13 loot containers (19 container block entities total: 5 campfires + 1 lectern, all empty), 93 `item replace block` commands. Full tables: `data/chests.csv`, `data/loot.csv`, `data/containers.csv`.

| Container | Type | Position | Contents |
|---|---|---|---|
| Red spawn shulker | white shulker box | `1965 24 0` | crossbow, arrow, bow, arrow, stick×4, arrow, bow, arrow, crossbow |
| Green spawn shulker | white shulker box | `2035 24 0` | same |
| Blue spawn shulker | white shulker box | `2000 24 35` | same |
| Yellow spawn shulker | white shulker box | `2000 24 -35` | same |
| Orange balloon crate | chest | `1974 26 -26` | 4× cobweb (CanPlaceOn), iron axe, 2× fishing rod, regeneration splash potion, spyglass |
| Cyan balloon crate | chest | `1974 26 26` | same |
| Lime balloon crate | chest | `2026 26 -26` | same |
| Purple balloon crate | chest | `2026 26 26` | same |
| Orange balloon bottom | barrel | `1974 14 -26` | 2× ender pearl, strong_leaping splash potion |
| Cyan balloon bottom | barrel | `1974 14 26` | same |
| Lime balloon bottom | barrel | `2026 14 -26` | same |
| Purple balloon bottom | barrel | `2026 14 26` | same |
| Mid | barrel | `2000 29 0` | 4× cobweb ×2 (CanPlaceOn), 2× arrow ×2, 2× ender pearl, trident (Loyalty I) |

**H** all positions/slots/items. The loot is **fixed**, not random. **H**. The 9-slot pattern is slots 3-5, 12-14, 21-23 (a centred 3×3 in the 27-slot container); the balloon barrels use slots 11, 13, 15. **H**.

The **cobwebs are placeable traps**: `CanPlaceOn` the same 38-block palette. **H**. The **trident** is the only enchanted item (Loyalty I). **H**. The **regeneration** potion is the only heal; the **strong_leaping** potion is the only mobility consumable. **H**.

## 5. Iron / crafting economy

- Each gift box has 2 `iron_block` at its ribbon apex: `1967 25-26 0` (Red), `2033 25-26 0` (Green), `2000 25-26 33` (Blue), `2000 25-26 -33` (Yellow). **H**.
- The stone pickaxe has `CanDestroy:[iron_block]`, so a player can mine them in adventure mode. **H**.
- `ironblock` is `minecraft.mined:minecraft.iron_block`; CB-15 x1001: `execute as @a at @s if score @s ironblock matches 1 run give @s iron_block 1`, then resets the score to 0. **H**.
- At start, the impulse at `x971,10,-6` places the outermost r50 ring and restores all four knots: `fill 1967 26 0 1967 25 0 iron_block` etc. **H** command; **M** start trigger.
- Crafting tables are on every gift box (`1966 24 0` etc.) and at mid (`2000 30 0`). Wood is available from the balloons (stripped spruce logs, fences, trapdoors) and the gallery (oak fences/leaves). **H** blocks; **M** that the intended loop is iron blocks → ingots → iron gear. There is no furnace and no coal; iron tools/armour can be crafted from ingots at a crafting table, so the loop is technically complete. **M**.

## 6. Border (the “shrinking” ring)

There is **no `worldborder` command** in the arena. The border is a set of **nested damage rings** driven by four spinning marker entities at `2000 26 0`:

- The four entities (armor stand, item frame, glow item frame, small fireball) are spun by repeating `tp @s ~ ~ ~ ~10 …` commands. **H**.
- At **start**, an impulse at `x971,10,-6` fills the outermost r50 redstone at `975 14 -3`; the same chain also restores the iron knots and kills dropped items. **H** command; **M** start trigger.
- At each timer threshold, a redstone block is placed at `x… 14 -3`, powering a **repeating loop**: a repeating command block at z-4 plus chain blocks at z-8..-5. While powered, it:
  - draws a smoke ring at radius N from the spinning entity: `execute as @e[type=…] at @s run particle minecraft:campfire_cosy_smoke ^ ^ ^N`; **H**.
  - withers every entity farther than N from the marker: `execute as @e[type=armor_stand] at @s run effect give @e[distance=N..50] minecraft:wither 1 1 true`; **H**.
  - The 1-second wither is refreshed every tick, so the border is continuous, not one-shot. **H** loop; **M** damage feel.
- Stages (timer → radius N):

| Stage | start | 60 | 90 | 110 | 140 | 160 | 170 | 175 | 180 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Safe radius | 50 | 40 | 30 | 20 | 15 | 10 | 8 | 6 | 5 |
| Trigger block x | 975 | 973 | 971 | 969 | 967 | 965 | 963 | 961 | 959 |

- The redstone blocks **accumulate**; the harshest active radius dominates. The `fill 959 14 -3 975 14 -3 air` at game end clears them all. **H** commands; **M** accumulation intent.
- The radii line up with the architecture: r50 = whole box, r40 = islands/balloons, r30 = outer ring, r20 = ring, r10 = inner void, r5 = mid. **M** (design reading).
- `tellraw` warnings at timer 150, 130, 110, 80, 50 (“The border will keep shrinking!”, “5 blocks in 8 seconds!”, “10 blocks in 10 seconds!”). **H** text; the numbers are approximate, not a literal block count. **M**.
- The damage selector is `@e`, not `@a[gamemode=adventure]`; it can hit lobby players at y70 (distance ~44) and non-player entities. **H** selector; **M/L** impact.

## 7. Death, respawn, win

- Game rules: `keepInventory=true`, `doImmediateRespawn=true`, `fallDamage=false`, `doDaylightCycle=false`, `randomTickSpeed=0`, `commandBlockOutput=false`. **H** (`level.dat`).
- `death_skybattle` is a `deathCount` objective. On death it becomes 1; repeating CB-14 x949:
  - `gamemode spectator @s`
  - `xp set @s 14 levels`
  - `scoreboard players set @s death_skybattle 0`
  So death = instant spectator, no respawn, no item drop (keepInventory). **H**.
- The win check is per team: if a level-10/11/12/13 player exists and **no** enemy team member is in `gamemode=adventure` inside `x1800..2200, y-400..57, z-200..200`, then:
  - `xp set @s 14 levels`
  - `gamemode spectator @s`
  - `scoreboard players add <Team> POINTS 50`
  - `fill 953 18 -3 redstone_block` (game-end sound)
  - `summon firework_rocket` at the winner
  - repeating `tellraw @a {'text':'[Sky Battle] Team X wins!','color':…}`. **H** commands.
- The box excludes the sky lobby (y57+), so lobby players do not block the win. **H** selector.
- There is **no time limit** and no “time up” message. The game ends only when one team is left (or everyone is dead). **H** (no such command).
- Dead players stay as spectators at their death position; no teleport to a spectator point. **H** (no tp in the death handler).
- Winners are cleared and reset at the next Start: `team join pve @a[level=14]`, `clear @a[level=14]`, `effect clear @a[level=14]`, `kill @a[level=14]`, `xp set … 8`. **H**.

## 8. Reset

Three different resets exist:

1. **Normal start reset** (every round):
   - Kill/clear level-14 players, reset to level 8, `death/timer` scores 0.
   - Refill the 13 loot containers with the 93 `item replace` commands. **M** that this chain is fired by Start; the commands are 13 parallel chains at y10, z-10..-18, x949..973 (13 impulse heads at z-10), and the Start button pulses the command room. **H** commands, **M** wiring.
   - The refill writes only the scripted slots (3×3 or 3 slots). Items stashed in the other slots of a 27-slot container persist across rounds. **H** slot pattern; **H** implication.
   - No block sweep at start; the previous round's wool/cobweb is removed by the **end-of-round** sweep below. **M**.
2. **Wool/cobweb sweep** (110 fills at CB-14 x926-947):
   - 22 overlapping z-strips from -51 to 51, each `fill 1949 55 <z0> 2051 0 <z1> air replace <block>` for red_wool, yellow_wool, green_wool, blue_wool, cobweb.
   - It clears **only** those 5 blocks. Concrete, terracotta, glazed terracotta, quartz, planks, fences, etc. placed via the CanPlaceOn list are **not** cleared. **H** commands; **H** implication.
   - It also clears the four team wools from the balloons? The balloons are orange/cyan/lime/purple, not the four team colours, so they survive. **H**.
3. **Bug reset** (`1999 67 0` sign, command block at `2000 69 0`):
   - Pulses `978 10 3`, which runs a vertical chain at x978, y10-23, z-8: 13 `fill <container> destroy` commands (drop contents), then `kill @e[type=item]` in the arena.
   - **It does not recreate the 13 loot containers.** After a bug reset, the 93 loot commands target air and fail; the map has no loot until manually restored. **H** commands; **H** consequence.
   - It also restores the iron knots? No — those are in the border chain, not the bug chain. **H**.

## 9. Rotations and flow

- **Spawn ring**: four gift boxes at cardinal points, centres at radius 33, spawns at radius 30. Adjacent spawns are 90° / ~42 blocks apart; opposite spawns 180° / 60 blocks apart. **H** coordinates.
- **Bridge gap**: the bounding-box gap is 7 blocks (Red x1973 → rim x1981), but x1972-1973 are the box wall at y15; the walkable inner ledge is x1971 (top y23), so the real bridge is **9 blocks**. The rim top is y25, a **2-block climb** from the inner ledge (feet y24 → rim feet y26). Starting from the ribbon top y24 gives an 11-block bridge and a 1-block climb. **H** geometry; **M** intended launch point.
- **Outer rim**: a 1-wide continuous circuit at y25, 116 blocks; the only through-route between the four covered galleries. **H** walk graph.
- **Galleries**: four covered rooms under the team canopies; the canopy dips to y27 at the diagonals, leaving too little headroom to pass, so each team's gallery is a separate room. **H** geometry; **M** separation intent.
- **Lower deck**: a 6-wide floor at y19, 6 blocks below the gallery; the inner 2 blocks are open to the void. Falling off the upper gallery's inner edge lands here; there are no stairs back. **H** blocks/walk graph; **M** fall-catch intent.
- **Mid**: 5-block plus at y28, 11 blocks from the lower inner lip, 13 from the upper gallery; the barrel/crafting table tower is the contested centre. **H**.
- **Balloons**: at the four diagonals, centres radius ~37; a crate chest at y26 (1 above the rim) and a barrel at y14 (11 below the rim). They are neutral loot between teams, not on the direct rotation. **H**.
- **Rotations**: clockwise or counterclockwise around the rim; the shortest hostile rotation is 90° (~30 blocks of rim) to the neighbouring gallery; the mid is equidistant from all four. The canopy tops are a second, optional level (walkable in four arcs, y27-33). **H** geometry; **M** tactical reading.

## 10. What the loop does not do

- No kill credit, no per-player score, no survival points, no placement points. Only +50 team `POINTS` for the win. **H**.
- No shop, currency, or gold/diamond. The only economy is iron + wood + crafting table. **H**.
- No time limit. **H**.
- No chest randomisation; every round has the same 13 loot containers and 93 items. **H**.
- No block reset for non-wool blocks. **H**.
- No world border; the barrier box is fixed. **H**.
- No team colour in the sidebar (all teams yellow). **H**.


