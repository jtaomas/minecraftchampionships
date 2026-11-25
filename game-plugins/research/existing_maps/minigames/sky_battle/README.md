# MG-SB-01 — Sky Battle arena deep dive

Status: **research complete; no build, no game/plugin code, no server writes.**
Owner: A6a. Written folder: `game-plugins/research/existing_maps/minigames/sky_battle/` only.
Source: `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3` (1.17.1, DataVersion 2730).
Live world: `mcc_minigames` (read-only; no RCON used).
Evidence: **H** = directly observed block/command/NBT/render pixel; **M** = inference from two sources; **L** = guess. Every claim below carries one.

---

## 0. Reconciliation with A6 (2026-11-25)

**Date-stamped QA note (2026-11-25):** earlier A6a drafts carried two stale correction rows — “A6 says 0 containers” and “A6 says 161 fill commands”. Both are withdrawn. A6's current `README.md` records Sky Battle as **13 pre-placed containers** (4 white shulkers, 5 barrels, 4 chests) plus a separate Parkour Tag barrel (13+1), and makes no 161-fill claim. A6a defines the Sky Battle 13 as **13 loot containers**, then adds 6 non-loot block entities (5 campfires + 1 lectern) for **19 total**. The substantive A6a findings stand.

A6's bounds and identity are accurate. These refinements matter for a rebuild.

| Finding | Detail | Conf. |
|---|---|---|
| Container total | **19 container block entities = 13 loot containers (93 items) + 5 campfires + 1 lectern; all non-loot are empty.** Full list: `data/containers.csv`, `data/containers.md`. | H |
| Balloon chests | The four chests at `1974/2026, 26, ±26` are **inside the diagonal balloons**, in a 3×3 oak-trapdoor crate. They are not on the ring or on the islands. | H |
| Balloons | They are **loot towers**, not decoration: 290 blocks each, with a chest in a trapdoor crate at y26, a barrel at y14, a chain and a campfire. The colours are orange/cyan/lime/purple. | H |
| Balloon barrels | The four barrels at `1974/2026, 14, ±26` are the **bottom of the balloon loot tower**, 12 blocks below the crate, on a chain. | H |
| Ring | It is a **covered circular gallery** (stone floor y25, team-coloured canopy y27-37, lower deck y19). Teams spawn on the gift boxes and **bridge ~9 blocks** to the rim (the bounding-box gap is 7, but the last 2 blocks are the box wall); the chain drop is the lobby-to-mid entrance, not the team entry. | H geometry; M drop |
| Fill set | The `fill` set is **162** commands: **110** are the wool/cobweb sweep (22 z-strips × 5 types); **52** are cages, glass, border pulses, destroy and iron restore. This is a refinement, not an A6 error. | H |
| Iron | The 8 `iron_block`s are **ribbon knots on the gift boxes**, not border markers. | H |
| Sky lobby | It is a **hanging gray/gold lantern** with an invisible barrier cage above it, not a flat deck. | H |

---

## 1. What the arena is

A four-team, hand-built **Sky Battle carnival**: four giant gift boxes at the cardinal points, four wool hot-air balloons at the diagonals, a circular covered gallery in the middle, and a tiny plus-shaped mid platform in the centre. Teams spawn on the gift boxes, bridge to the gallery, loot the balloons and mid, and fight while a circular damage border shrinks from radius 50 to 5.

| Fact | Value | Conf. |
|---|---|---|
| Copy envelope | `x 1948..2052, y 0..80, z -52..52` = 105 × 81 × 105 | H |
| Non-air blocks | **47,874** | H |
| Barrier blocks | **34,751** (72.59%) | H |
| Visible build | **13,123** blocks | H |
| Block types | **59** | H |
| Build footprint | `x 1961..2039, z -39..39` (79 × 79) | H |
| Playfield shell | walls x=1948/2052, z=-52/52 (y0..56); solid roof y56; **no floor** | H |
| Entry chain | single `chain` at `2000 56 0` in the roof; chain column down to `2000 31 0` | H |
| Lobby arrival | `2000 78 0` inside a 13×13 barrier cage y77..80 | H |
| Team spawns | Red `1970 23 0` · Blue `2000 23 30` · Green `2030 23 0` · Yellow `2000 23 -30` | H |
| Local commands | **3** in the arena (CB-18 at `1999..2001, 66..69, -1..0`) | H |
| Logic commands | **477** total: CB-14 445 + CB-15 29 + CB-18 3, at `x926..1004, y9..23, z-18..16` | H |
| Signs | **10** (4 team join, 4 start, 1 bug reset, 1 command-room pointer) | H |
| Container block entities | **19** = 13 loot containers (4 white shulker boxes, 5 barrels, 4 chests) + 5 campfires + 1 lectern; non-loot empty | H |
| Arena entities | 4 at `2000 26 0` (armor stand, item frame, glow item frame, small fireball) | H |
| Game rules | `keepInventory=true`, `doImmediateRespawn=true`, `fallDamage=false`, `doDaylightCycle=false`, `randomTickSpeed=0`, `commandBlockOutput=false` | H |

The 105×105×81 box is only 5.4% filled. The build is a thin set of floating platforms; the barrier is a separate invisible room around it. **H**.

---

## 2. Exact layout

Machine table: `data/layout.csv`. Plan: `frames/01_top_labelled.png`, `frames/02_top_clean.png`. Isometrics: `frames/03_iso_angle0.png`, `frames/04_iso_angle2.png`. Ring: `frames/05_ring_iso.png`, `frames/06_ring_top.png`. Sections: `frames/14_section_east_west.png`, `frames/15_section_north_south.png`.

### 2.1 Structure index

| ID | Structure | Bounds x / y / z | Blocks | Palette | Role | Conf. |
|---|---|---|---:|---|---|---|
| SB-ENV | Barrier envelope | 1948..2052 / 0..56 / -52..52 | 34,751 | barrier | invisible walls + roof; open bottom | H |
| SB-RING | Circular gallery | 1980..2020 / 11..37 / -20..20 | 7,344 | white concrete, stone bricks, team concrete/terracotta/glazed, oak | main arena | H |
| SB-RING-UPPER | Upper gallery floor | 1980..2020 / 25 / -20..20 | 856 | stone bricks, white concrete | 6-wide floor (r14..19), covered | H |
| SB-RING-CANOPY | Team canopy | 1981..2019 / 27..37 / -19..19 | 1,837 | team concrete + terracotta + glazed, white trim | four roofs; y37 peaks, y27 dips | H |
| SB-RING-RIM | Outer rim | 1981..2019 / 25 / -19..19 | 116 | stone bricks, quartz stairs | 1-wide continuous circuit | H |
| SB-RING-LOWER | Lower deck | 1982..2018 / 19 / -18..18 | 736 | white concrete, stone bricks | 6-wide deck 6 below floor; inner 2 open | H |
| SB-RING-FENCE | Outer colonnade | 1981..2019 / 12..32 / -19..19 | 378 | oak fence, oak leaves | decorative guard | H |
| SB-MID | Mid plus | 1999..2001 / 20..30 / -1..1 | 43 | stone bricks, team concrete, chain, barrel, crafting table | central loot | H |
| SB-GIFT-RED | Red gift box | 1961..1973 / 11..26 / -6..6 | 1,017 | white concrete, red concrete/glazed, quartz, 2 iron | Red spawn + shulker | H |
| SB-GIFT-GREEN | Green gift box | 2027..2039 / 11..26 / -6..6 | 1,018 | white, green concrete/glazed, quartz, 2 iron | Green spawn + shulker | H |
| SB-GIFT-BLUE | Blue gift box | 1994..2006 / 11..26 / 27..39 | 1,018 | white, blue concrete/glazed, quartz, 2 iron | Blue spawn + shulker | H |
| SB-GIFT-YELLOW | Yellow gift box | 1994..2006 / 11..26 / -39..-27 | 1,017 | white, yellow concrete/glazed, quartz, 2 iron | Yellow spawn + shulker | H |
| SB-BALLOON-NW | Orange balloon | 1970..1978 / 14..42 / -30..-22 | 290 | orange wool, timber, chain, chest, barrel | NW loot tower | H |
| SB-BALLOON-SW | Cyan balloon | 1970..1978 / 14..42 / 22..30 | 290 | cyan wool, timber, chain, chest, barrel | SW loot tower | H |
| SB-BALLOON-NE | Lime balloon | 2022..2030 / 14..42 / -30..-22 | 290 | lime wool, timber, chain, chest, barrel | NE loot tower | H |
| SB-BALLOON-SE | Purple balloon | 2022..2030 / 14..42 / 22..30 | 290 | purple wool, timber, chain, chest, barrel | SE loot tower | H |
| SB-LOBBY | Sky lobby lantern | 1993..2007 / 57..80 / -7..7 | 464 visible | gray wool, gold, raw gold, glass, light | pre-game room | H |
| SB-ARRIVAL | Arrival cage | 1993..2007 / 77..80 / -7..7 | barrier | barrier | catches hub arrivals | H |
| SB-CAGE-* | Team spawn cages | 3×1 at y25 over each spawn | 3 each | barrier → red → yellow → lime → air | countdown traffic light | H |
| SB-IRON-* | Ribbon iron knots | 2 at each gift-box apex | 8 total | iron block | mineable iron cache | H |
| SB-VOID | Void | open bottom | 0 | air | falls are lethal | H |

### 2.2 The ring is a covered gallery, not a mound

A cross-section at `z=0` (west-east) shows the real profile:

- `x1981`: stone-brick lip at y25.
- `x1982..1986`: white-concrete facade y16..19, red-terracotta band y24, stone-brick floor y25, **gap y26..32**, red-concrete roof y33..35, white cap y36..37.
- `x1987`: stone-brick lower lip y19, white-concrete inner lip y25.
- `x1988`: white-concrete lower lip y19.
- `x1989..2011`: void (except the mid tower at x1996..2004).
- The east side mirrors it in green.

So the ring is **a 5-block-wide floating roof on a 6-block-wide stone floor**, not a solid wall. The roof is high at the team centres (underside y33, top y37) and dips to y27 at the diagonals. The gap y26..32 is open on the outer side; the oak colonnade at r19-20 rises to y32 and appears to support the roof edge. **H** section/blocks; **M** support.

The gallery floor at y25 is continuous around the ring, but the low canopy at the diagonals leaves only 1-2 blocks of headroom. A walking analysis (2-block clearance, 1-block step) finds **85 disconnected walkable components**: four gift-box tops (72 each), four canopy arcs (126 each), one lower deck (544), one upper rim/gallery circuit (144), and the mid. **H** (walk graph script `data/walk_graph.py`, `data/walk_comp.py`). The practical consequence: **you cannot walk from one team's gallery to another under the canopy; you must use the 1-wide outer rim or bridge.**

### 2.3 Gift boxes (spawn pods)

Each is a **13×13×16 hollow gift box** on a rounded white-concrete/stone base:

- Base: white concrete y11-12, a rounded 13×13 pad.
- Box body: hollow white concrete walls y13-15, then the team-coloured lid.
- Lid: team concrete + team glazed terracotta, with a **white quartz ribbon cross** across the top at y22-24.
- Ribbon apex: a 2-block **iron block** knot at the island centre, 3 blocks outward from the spawn (away from the arena).
- Top surface: a 1-block-wide quartz cross at y22-24; the spawn is on the smooth-quartz inner edge at y23, just inside the ribbon cross.
- Loot: a white shulker box at y24, 5 blocks outward from the spawn (2 blocks outward of the island centre), next to a crafting table.
- Interior: hollow, floor at y14-16; the box is decorative, not a room.

Exact spawn and loot:

| Team | Spawn | Shulker | Crafting table | Iron knot | Conf. |
|---|---|---|---|---|---|
| Red (west) | `1970 23 0` | `1965 24 0` | `1966 24 0` | `1967 25-26 0` | H |
| Green (east) | `2030 23 0` | `2035 24 0` | `2034 24 0` | `2033 25-26 0` | H |
| Blue (south) | `2000 23 30` | `2000 24 35` | `2000 24 34` | `2000 25-26 33` | H |
| Yellow (north) | `2000 23 -30` | `2000 24 -35` | `2000 24 -34` | `2000 25-26 -33` | H |

The spawn coordinate is **3 blocks inward** from the gift-box centre; the island centre is at radius 33, the spawn at radius 30. **H** (bounds + coordinates).

### 2.4 Balloons (vertical loot towers)

Four 9×9 wool balloons at the diagonals, each 290 blocks:

- Wool envelope y31..42 (hollow shell), 5×5 cap at y41-42.
- Timber basket/mast y24..29: stripped spruce log, oak trapdoors, spruce fences/gates, spruce slab, empty campfire at y29. The campfire is a decorative block entity, not loot.
- **Chest at y26** inside a 3×3 oak-trapdoor crate at the basket centre. Break the trapdoors to reach it.
- **Barrel at y14**, 12 blocks below the crate, connected by a chain y15..24.
- Colours: NW orange, SW cyan, NE lime, SE purple. These are **not** team colours; they mark neutral loot. **H**.

Plan positions: `1974,-26` (orange), `1974,26` (cyan), `2026,-26` (lime), `2026,26` (purple). The nearest edge is ~31 blocks from the centre; the balloons sit between the gift boxes, not on the direct spawn→ring path. **H**.

### 2.5 Mid

A tiny **plus-shaped** platform at y28: five stone-brick blocks at `(2000,-1)`, `(1999,0)`, `(2000,0)`, `(2001,0)`, `(2000,1)`, with a barrel at y29 and a crafting table at y30 on top. Below it, y20-25, is a decorative team-coloured compass: red west, green east, yellow north, blue south, with white/stone rings. A chain runs from y31 up to the lobby. **H**.

The mid is 3 blocks above the gallery floor (y25), 11 blocks from the lower inner lip (r12) and 13 from the upper gallery inner edge (r14). It is small enough that one player can block it, and it has no cover. **H** geometry; **M** tactical reading.

### 2.6 Sky lobby

A hanging **gray/gold lantern** at `x1993..2007, y58..76, z-7..7`:

- Gray wool body, gold/raw-gold cap, red/yellow/blue/green glass accents, gray-wool pads.
- Four team bays at y69-70 around a 3×3 command-block core; four `*_concrete_powder` pads at y75; signs at y76.
- A 3×3 gray-wool pad at y76 under the arrival point; the player at `2000 78 0` falls 1-2 blocks onto it.
- A 13×13 barrier cage y77..80 with no floor; it catches hub arrivals and prevents jumping out.
- An **empty lectern** at `2000 77 -1`; 4 `light` blocks at y74; 5 stone buttons at y70; 2 command blocks + 1 repeating at y66-69. The stem also has an empty campfire at `2000 63 0`. **H** (`data/containers.csv`).

The lantern is suspended from the barrier roof by a chain/column; the only hole in the roof is `2000 56 0`. **H**.

### 2.7 Void edges and border

- **No floor.** The barrier envelope has walls and a roof but no bottom. Falling off the gallery, gift box or balloon drops past y0; death is by void at y<-64. `fallDamage=false` removes fall damage but not the void. **H** (no floor blocks); **M** death height.
- **Walls**: x1948/2052 and z-52/52, 1 block thick, y0..56; the build is 13+ blocks inside. Players hit an invisible wall, not a visible border. **H**.
- **Roof**: solid barrier y56; the only opening is the single chain at `2000 56 0`. No elytra/pearl escape. **H**.
- **Shrinking border**: not a world border. See §5.6. It is a set of **nested circular damage rings** at radii 50 → 40 → 30 → 20 → 15 → 10 → 8 → 6 → 5, drawn with campfire smoke and enforced with a 1-second wither refreshed every tick while the stage is powered. The r50 ring is placed at start (harmless, outside the build); the timer stages shrink it from 60 s. **H** commands; **M** start trigger and accumulation intent.

---

## 3. Map flow and rotations

1. **Spawn** on the smooth-quartz inner edge of your gift box, at radius 30, facing roughly the ring (yaw is not set by the tp; the player keeps their lobby yaw). **H** no yaw; **M** facing.
2. **Bridge ~9 blocks** across the void to the ring's outer rim. The bounding gap is 7 blocks (e.g. Red box edge x1973 → rim x1981), but x1972-1973 are the box wall at y15, not a walkable ledge; the walkable inner ledge is x1971 (top y23), so the real bridge is 9 blocks. The rim top is y25, a **2-block climb**; a wool staircase at the rim is needed. Starting from the higher ribbon top (y24) gives an 11-block bridge and a 1-block climb. **H** geometry; **M** that either is intended.
3. **Choose a route**: the 1-wide outer rim is a continuous circuit; step under your team's canopy into the 6-wide gallery for cover, or stay on the rim for speed. **H** walk graph; **M** tactical.
4. **Loot**: your spawn shulker is behind you on the gift box; the nearest balloon crate is a short 5-13 block bridge from the rim (depending on the launch point); the mid is an 11-block bridge from the lower inner lip. **H** coordinates; **M** the 5-13 range.
5. **Rotate**: adjacent team gallery is 90° away (~30 blocks of rim); the opposite is 180° (~60 blocks). The four canopy dips at the diagonals are the seams between team rooms; the outer rim crosses them freely. **H** geometry.
6. **Vertical play**: the canopy tops are walkable in four arcs (y27-33), reachable by pillaring from the gallery or bridging up from the gift box side at the diagonal dip. The lower deck y19 is a fall-catch under the gallery; it has no stairs back, so falling there costs time and wool. **H** walk graph; **M** intent.
7. **Endgame**: the damage rings force everyone inward: r30 cuts off most of the islands/balloons, r20 the outer ring, r10 the inner void, r5 the mid. The last team on the mid plus wins. **M** (design reading of the radius schedule).

Rotation order around the compass: **Yellow (N) → Green (E) → Blue (S) → Red (W) → Yellow**. The gift boxes and canopy quadrants follow the same order. **H** coordinates/render.

---

## 4. Loot and loadout

### 4.1 Loot containers (13; 19 total block entities)

Full loot table: `data/chests.csv`; 93 individual entries: `data/loot.csv`; complete 19-row block-entity list: `data/containers.csv` and `data/containers.md`.

| ID | Type | Pos | Role | Access | Conf. |
|---|---|---|---|---|---|
| SB-C01 | white shulker box | `1965 24 0` | Red spawn loot | island top | H |
| SB-C02 | white shulker box | `2035 24 0` | Green spawn loot | island top | H |
| SB-C03 | white shulker box | `2000 24 35` | Blue spawn loot | island top | H |
| SB-C04 | white shulker box | `2000 24 -35` | Yellow spawn loot | island top | H |
| SB-C05 | chest | `1974 26 -26` | Orange balloon crate | break trapdoor crate | H |
| SB-C06 | chest | `1974 26 26` | Cyan balloon crate | break trapdoor crate | H |
| SB-C07 | chest | `2026 26 -26` | Lime balloon crate | break trapdoor crate | H |
| SB-C08 | chest | `2026 26 26` | Purple balloon crate | break trapdoor crate | H |
| SB-C09 | barrel | `1974 14 -26` | Orange balloon bottom | bridge/drop below | H |
| SB-C10 | barrel | `1974 14 26` | Cyan balloon bottom | bridge/drop below | H |
| SB-C11 | barrel | `2026 14 -26` | Lime balloon bottom | bridge/drop below | H |
| SB-C12 | barrel | `2026 14 26` | Purple balloon bottom | bridge/drop below | H |
| SB-C13 | barrel | `2000 29 0` | Mid loot | bridge to mid plus | H |

There are **no loot containers on the gallery floor** and none in the sky lobby. **H**. The arena has **19 container block entities = 13 loot containers (93 items) + 5 campfires + 1 lectern; all non-loot are empty.** The 13 loot containers were scanned as `TileEntities`; each contains the items listed below (93/93 entries match the command table). **H** (`data/containers.csv`, `data/scan_containers.py`).

### 4.2 Loot table

All 13 loot containers use the same 3×3 slot pattern: slots 3,4,5 / 12,13,14 / 21,22,23. The balloon barrels use slots 11,13,15. **H**.

| Container type | Slots | Items (per container) | Conf. |
|---|---:|---|---|
| Team spawn shulker | 9 | crossbow, arrow, bow, arrow, stick ×4, arrow, bow, arrow, crossbow | H |
| Balloon crate chest | 9 | cobweb (CanPlaceOn 38) ×4, iron axe, fishing rod ×2, regeneration splash potion, spyglass | H |
| Balloon bottom barrel | 3 | ender pearl ×2, strong_leaping splash potion | H |
| Mid barrel | 9 | cobweb ×2 (CanPlaceOn 38) ×4, arrow ×2 ×2, ender pearl ×2, trident (Loyalty I) | H |

Notable: arguably the best weapon is the **Loyalty I trident** (mid); the only heal is **regeneration** (balloon crate); the only mobility potion is **strong_leaping** (balloon barrel), with ender pearls as the other mobility item; the spawn shulker supplies **bow/crossbow**. There is **no armour in any container** — the iron chestplate from the kit is the only armour piece. **H** items; **M** “best”.

### 4.3 Team kit

| Slot | Item | Count / NBT | Conf. |
|---|---|---|---|
| hotbar.0 | stone sword | 1; Unbreakable, CanDestroy cobweb | H |
| hotbar.1 | stone pickaxe | 1; Unbreakable, CanDestroy iron_block | H |
| hotbar.2 | shears | 1; Unbreakable, CanDestroy 4 team wools | H |
| hotbar.8 | cooked beef | 16 | H |
| armor.chest | iron chestplate | 1; Unbreakable | H |
| weapon.offhand | team wool | 64; CanPlaceOn 38 arena blocks | H |

No helmet, leggings, boots, extra food, potions, or blocks other than the wool. **H**.

### 4.4 Wool CanPlaceOn

The 38-block allow-list (identical for all four teams except the dye):
`red_wool, yellow_wool, green_wool, blue_wool, white_concrete, red_concrete, yellow_concrete, green_concrete, blue_concrete, red_terracotta, yellow_terracotta, green_terracotta, blue_terracotta, red_glazed_terracotta, yellow_glazed_terracotta, green_glazed_terracotta, blue_glazed_terracotta, smooth_quartz, smooth_quartz_stairs, cyan_wool, orange_wool, purple_wool, lime_wool, chain, gray_shulker_box, white_shulker_box, barrel, crafting_table, stripped_spruce_log, spruce_trapdoor, oak_trapdoor, spruce_fence, oak_fence, stone_bricks, stone_brick_stairs, oak_leaves, cobblestone_wall, polished_blackstone`.

The CB-15 kit version ends with `polished_blackstone_wall` instead of `polished_blackstone`; still 38. **H**. This list is the arena's own palette, so bridging is possible on every intended surface, and adventure-mode placement is blocked everywhere else. **H** mechanics; **M** design intent.

### 4.5 Iron / crafting

- 2 iron blocks at each gift-box ribbon apex (8 total). **H**.
- Stone pickaxe can mine them (`CanDestroy:[iron_block]`); `ironblock` is a `mined:iron_block` objective; CB-15 gives 1 iron_block item and resets the score. **H**.
- At start, the impulse at `x971,10,-6` places the outermost r50 ring and restores all four knots (same chain). **H** command; **M** start trigger.
- Crafting tables on every gift box and at mid; wood from balloons/gallery. **H**.
- **M:** the intended economy is iron block → 9 ingots → iron sword/armour at the crafting table, with wood for sticks. There is no furnace, so no smelting; iron gear is craftable directly from ingots. This is a small, optional upgrade path, not a shop.

### 4.6 Respawn, kill, win

- `keepInventory=true`; death does not drop items. **H**.
- `death_skybattle` = `deathCount`; on death the player becomes **spectator**, gets XP level 14, and the score resets. No respawn into the game. **H**.
- `doImmediateRespawn=true`; the lobby `spawnpoint` is used only for the post-game kill/reset trick. **H**.
- Win = last team with an `gamemode=adventure` player inside `x1800..2200, y-400..57, z-200..200`, and no enemy team member in that box. **H** selector.
- Winner: XP 14, spectator, +50 team `POINTS`, firework, `tellraw`. **H**.
- No time limit; no kill credit; no per-player score. **H**.

---

## 5. Gameplay loop from the commands

Full command table: `data/commands.csv` (477 rows); `data/command_blocks.csv` is the required-name alias with identical rows. Readable list: `data/cb14_noloot.txt`. Narrative: `data/flow.md`. Below is the loop in order.

### 5.1 Start / reset of the previous round

The Start button (four signs at y71) pulses the command room. The pre-start chain at CB-14 `x929..933, y18..20, z-7..-1`:

```
tp @a[level=14] 2000 77 0
gamemode adventure @a[level=14]
clear @a[level=14]
kill @a[level=14]
effect clear @a[level=14]
xp set @a[level=14] 8 levels
scoreboard players set @a death_skybattle 0
scoreboard players set @a[level=14] timer_skybattle 0
```

With `doImmediateRespawn` and the lobby spawnpoint, this kills the previous winners, respawns them in their bay, and resets them to level 8. **H** commands; **M** the exact button→chain wiring (the command blocks are 1,000 blocks away and the redstone line is not traced).

### 5.2 Team assignment

Team is a scoreboard team, but the **XP level is the real state**:

- Level 8 = lobby/ready.
- Stand on `red_concrete_powder` at `1997 75 0` (or blue/green/yellow) → `team join Sky_Battle_<colour>` and `tp` to the team bay at y70. **H**.
- The same works on the coloured glass at y69; `level=14` on glass resets to level 8. **H**.
- The four bays: Red `1998 70 0`, Blue `2000 70 2`, Green `2002 70 0`, Yellow `2000 70 -2`. **H**.

### 5.3 Start trigger and countdown

`execute as @a[level=8,team=Sky_Battle_Red] run xp set @s 9 levels` (and Blue/Green/Yellow) turns everyone on a team to level 9. **H**. Then 10 numbered titles `>1<` … `>10<` at x947-982, y10-12, z2-6, with note-block sounds. **H**.

During the countdown, the kit is handed out piece by piece (sword at >3<, pickaxe, shears, beef, chestplate). **H**. At the end:

- `tp @s 1970 23 0` / `2000 23 30` / `2030 23 0` / `2000 23 -30`. **H**.
- 3×1 barrier cages at y25 over each spawn; recoloured red → yellow → lime → air. **H**.
- Team wool 64 with CanPlaceOn is placed in the offhand (level 10-13 CB-15 impulses). **H** commands; **M** order.

### 5.4 Loot

The 93 `item replace block` commands write fixed items into the 13 loot containers (part of the 19 container block entities; the 5 campfires and 1 lectern are empty). The 9-slot pattern is centred 3×3. **H**. They are **13 parallel chains, one per loot container**: 13 impulse heads at y10, z-10, x949..973, each writing slot 3 (or slot 11 for the barrels), then the chain runs along z-11..-18 for the other slots. The Start button is **M** the trigger. The loot containers are pre-placed in the map; the commands only fill them. **H** blocks + commands.

### 5.5 Death

`death_skybattle` is `deathCount`; on death it becomes 1. Repeating CB-14 `x949, y14, z1..3`:

```
gamemode spectator @s
xp set @s 14 levels
scoreboard players set @s death_skybattle 0
```

No respawn; no item drop (`keepInventory=true`). **H**.

### 5.6 Border

No `worldborder` command exists. The border is **nested circular damage rings**:

- Four marker entities at `2000 26 0` spin continuously (`tp @s ~ ~ ~ ~10 …`). **H**.
- **At start**, an impulse at `x971,10,-6` fills the r50 redstone at `975 14 -3`; the same chain restores the iron knots and kills dropped items. **H** command; **M** start trigger.
- At timer **60/90/110/140/160/170/175/180**, a redstone block at `x973/971/969/967/965/963/961/959, y14, z-3` fires a chain. **H**.
- Each stage is a **repeating loop**: a repeating command block at z-4 (auto=0, powered by the stage redstone) plus chain blocks at z-8..-5. While powered, it draws a smoke ring at radius N (`particle … ^ ^ ^N`) and withers entities farther than N (`effect give @e[distance=N..50] wither 1 1 true`) every tick. **H** block kinds/commands.
- Radii: **r50 at start**, then 40, 30, 20, 15, 10, 8, 6, 5 at the timer thresholds. **H** (x/N mapping).
- The redstone blocks accumulate; the smallest active radius dominates. **H** commands; **M** intent. The end-of-game `fill 959 14 -3 975 14 -3 air` clears them. **H**.
- Warnings at timer 150/130/110/80/50 are pre-warnings for the next stage. **H** text; the "5/10 blocks" numbers are approximate. **M**.

### 5.7 Win

Per-team check at CB-14 `x943..951, y14, z-8..-3`:

```
execute as @a[level=10] at @s
  if entity @a[level=10,team=Sky_Battle_Red]
  unless entity @a[x=1800,y=-400,z=-200,dx=400,dy=457,dz=400,gamemode=adventure,team=!Sky_Battle_Red]
  run xp set @s 14 levels
...
run scoreboard players add Red POINTS 50
run fill 953 18 -3 redstone_block
run summon firework_rocket ...
```

and a repeating `tellraw` with the correct colour. **H**. The box excludes the sky lobby (y57+). **H**. No time limit. **H** (no such command).

### 5.8 Reset

Three resets:

1. **Normal**: next Start kills/clears level-14 players, resets to level 8, refills containers via the 93 item commands. The refill writes only the scripted slots (3×3 or 3 slots); an item a player stashes in any other slot of a 27-slot container **persists** across rounds. **M** trigger; **H** commands + slot pattern; **H** persistence implication.
2. **Wool/cobweb sweep**: 110 `air replace` fills at x926-947, y10/13, z-13..-6. 22 z-strips from -51..51 × 5 block types (red/yellow/green/blue wool + cobweb). It clears **only those five**; concrete, terracotta, glazed terracotta, quartz, wood, etc. placed via CanPlaceOn **persist**. **H** commands; **H** consequence.
3. **Bug reset**: button at `1999 67 0` → command block `2000 69 0` → `setblock 978 10 3 redstone` → 13 `fill <loot container> destroy` + `kill @e[type=item]`. It **does not recreate the 13 loot containers**, so the next round's 93 item commands target air and fail. Manual restore is required. **H** commands; **H** consequence.

---

## 6. Blocks, palette, lighting

Full counts: `data/blocks.csv`; per-region matrix: `data/blocks_by_region.csv`; notes: `data/palette.md`.

- **Barrier 34,751 (72.59%)**; visible build 13,123.
- **Neutral stone/quartz 6,330 (13.22%)**: white concrete 4,545, stone bricks 1,231, smooth quartz 256 + stairs 95.
- **Team concrete 3,004 (6.27%)**: red 984, yellow 725, green 725, blue 570.
- **Team terracotta 1,437 (3.00%)**; **team glazed 313 (0.65%)**.
- **Balloon wool 892 (1.86%)**: orange/cyan/lime/purple 223 each.
- **Wood/containers 662**, **gray wool 287**, **gold/raw gold 68**, **chain 61**, **iron 8**, **join pads 4**, **utility/lighting 28**.
- **59 block types**, all vanilla 1.17.1. **H**.

Lighting: 4 `light` blocks at y74 in the lobby; 4 campfires at y29 on the balloon crates; 1 campfire at y63 on the lobby stem. No torches, lanterns, glowstone, or sea lanterns in the arena. The gallery is covered by a canopy and the whole arena by the barrier roof; expect dark pockets under the low canopy unless barrier passes skylight. **H** fixtures; **M** in-game light.

Palette logic: white/stone neutral shell + one saturated triple (concrete / terracotta / glazed) per team; wool balloons as the only non-team colour; no custom pack. **H** counts; **M** design reading.

---

## 7. 1.21.1 compatibility risks

The source is 1.17.1. The world upgrades automatically, but **command strings are not rewritten by DataFixer**. The map's item syntax is the main breakage.

| Risk | Commands | What fails in 1.21.1 | Fix pattern | Conf. |
|---|---:|---|---|---|
| Legacy item NBT in `item replace` / `give` | **36** | `cobweb{CanPlaceOn:[…]}`, `red_wool{CanPlaceOn:[…]}`, `stone_sword{Unbreakable:1b,CanDestroy:[…]}`, `shears{…}`, `iron_chestplate{Unbreakable:1b}`, `trident{Enchantments:[…]}`, `splash_potion{Potion:'…'}` — braces are no longer valid item syntax | `cobweb[can_place_on={predicates:[…]}]`, `stone_sword[unbreakable={},can_destroy={predicates:["minecraft:cobweb"]}]`, `trident[enchantments={levels:{"minecraft:loyalty":1}}]`, `splash_potion[potion_contents={potion:"minecraft:regeneration"}]` | H |
| Legacy firework NBT | **4** | `summon firework_rocket … {LifeTime:…,FireworksItem:{id:…,Count:1,tag:{Fireworks:{…}}}}` — old item stack NBT; `Colors`/`FadeColors`/`Type`/`Flicker`/`Trail` names changed under components | Use `FireworksItem:{id:"minecraft:firework_rocket",count:1,components:{"minecraft:fireworks":{flight_duration:0,explosions:[{shape:"large_ball",has_twinkle:true,has_trail:true,colors:[…],fade_colors:[…]}]}}}`; `Type:1` = large_ball, not star. Verify the entity field name on 1.21.1 | H command; M exact field |
| `spawnpoint @s x y z` | 5 | **OK** — still valid | — | H |
| `execute … if entity … unless entity …` | 124 | **OK** | — | H |
| `team join` / `team empty` | ~20 | **OK** | — | H |
| `scoreboard players …` | 9 | **OK** (objectives must exist; they are in `scoreboard.dat`, not created by commands) | — | H |
| `fill … air replace` / `destroy` | 123 | **OK** | — | H |
| `title` JSON / `tellraw` JSON | 26 | **OK** | — | H |
| `effect give … true` | 2 | **OK** | — | H |
| `particle … ^ ^ ^N` | 36 | **OK** | — | H |
| `stopsound` / `playsound` | ~30 | **OK** | — | H |
| `light` block id | 4 | **OK** (1.17+ block, still in 1.21) | — | H |
| `@a[level=8]`, `@a[team=!…]`, `@e[distance=5..50]` | many | **OK** | — | H |
| `summon` / `tp` / `kill` / `clear` / `gamemode` | many | **OK** | — | H |

**Totals:** 40 of 477 commands (8.4%) contain legacy item/entity NBT that will not run as written on 1.21.1. The other 437 are syntactically compatible. **H** (count from `data/commands.csv`).

World-upgrade notes:

- 1.17.1 `Level`/`Sections`/`Palette`/`BlockStates` → 1.18+ `sections`/`block_states` is handled by DataFixer; the blocks survive. **H** (format), **M** (upgrade behaviour).
- Signs: 1.17 `Text1..4` → 1.20 `front_text.messages`; DataFixer handles it. **H** format; **M** live.
- Command blocks: `Command`, `auto`, `conditionMet` survive; the command **string** is not fixed. **H**.
- `TeamColor=yellow` for all four Sky Battle teams is preserved; fix manually. **H**.
- `Sky_Battle_Yellw` is preserved; rename both team and all 11+ selectors or keep the typo. **H**.
- `scoreboard.dat` criteria survive; do not delete the objectives. **H**.
- The 1.17.1 source has no datapacks; no pack-format issue. **H**.

Other 1.21.1 risks:

- **Command length:** longest legacy command ~705 chars, under 32,500. **H**.
- **Item count:** 64 is under the 99 cap. **H**.
- **`container.N` slots:** valid; `weapon.offhand`, `armor.chest`, `hotbar.N` valid. **H**.
- **Barrier collision/light:** barrier is still invisible/solid in 1.21; light pass-through needs an in-game check. **H** block; **M** light.
- **`keepInventory` / `doImmediateRespawn` / `fallDamage`:** all still exist. **H**.

---

## 8. Comparison with the current `McmSkyBattle` (read-only)

Current implementation: `game-plugins/SkyBattle/src/main/java/com/mcm/games/skybattle/` (`SkyBattleGame.java`, `Arena.java`, `LootTable.java`), read but not modified. It generates a void world at `BASE_Y=100`, `START_R=5`, `MID_R=4`, `CENTER_R=10`, ring radius 36 (minimum), 4+ sides (10 named team colours + 16 dye colours for solos), 480 s, square world border, height squeeze, kill/survival/placement scoring.

### 8.1 What the reference map does better

| # | Reference | Why it is better | Conf. |
|---|---|---|---|
| 1 | **Gift-box spawn pods + team canopies + balloons** | Instant silhouette and team identity; a player always knows their side and the enemy sides from the shape/colour. The current map is generic grass discs with a flag. | H render; M design |
| 2 | **Circular gallery around a central void** | The map shape and the border shape agree. The four team rooms are distinct; the 1-wide outer rim is a natural rotation route. The current square border around a circular map is a mismatch. | H geometry; M design |
| 3 | **Nested circular damage rings** | Cheap, no world-border API, matches the radii of the architecture (r30 islands, r20 ring, r5 mid). Visible smoke rings. The current square world border cannot express this. | H commands; M design |
| 4 | **Utility loot** (trident, pearls, cobwebs, fishing rod, leaping/regen potions, spyglass) | Creates tricks and emergent play; cobweb traps + shears + fishing rod are a skill layer. The current loot is mostly stat gear. | H loot |
| 5 | **Iron/crafting economy** | Mineable ribbon knots + crafting tables + balloon wood = a small upgrade path beyond chest RNG. The current map has no crafting loop. | H blocks/commands; M intent |
| 6 | **CanPlaceOn palette restriction** | Keeps player builds on the arena palette and inside the intended surfaces; shears/pickaxe are the only ways to remove key blocks. The current map allows free placement. | H commands |
| 7 | **Traffic-light spawn cages** | Barrier → red → yellow → lime → air is a clear, wordless start signal. The current map uses titles only. | H commands; M feel |
| 8 | **Fixed loot / fixed teams** | Competitive fairness and replayable routing; no highroll. The current random tiers can decide a game. | H loot |
| 9 | **Compact visible build** | 13,123 visible blocks; cheap to copy/render. The current procedural map builds a larger, generic arena. | H count |
| 10 | **Balloon vertical loot** | A chest 1 above the rim and a barrel 12 below creates risk/reward and a reason to look up/down. The current map is mostly horizontal. | H geometry |

### 8.2 What the reference map does worse

| # | Reference | Why it is worse | Conf. |
|---|---|---|---|
| 1 | **No scoring beyond the win** | Only +50 team `POINTS`; no kills, survival, placement, or per-player HUD. The current system is much richer. | H commands |
| 2 | **Low-canopy seams** | The canopy dips to y27 at the diagonals; the walk graph splits into 85 components; players cannot walk between galleries under cover and may not understand the route. The current open discs are clearer. | H walk graph; M readability |
| 3 | **Tiny mid** | 5 blocks, no cover, one player can block it; the current centre island is 10-radius with 4 chests. | H geometry |
| 4 | **`@e` border damage** | The wither selector is not filtered to alive adventure players; it can hit lobby players at y70 and non-player entities. The current world border only affects players. | H selector; M impact |
| 5 | **Partial block reset** | Only wool/cobweb are swept; concrete/terracotta/quartz/wood placed via CanPlaceOn persist. The current tracker clears every placed block. | H commands |
| 6 | **Bug reset destroys loot containers** | 13 `fill <loot container> destroy` and no recreate; the next round has no loot. The current map regenerates. | H commands |
| 7 | **XP-level state machine** | Levels 8/9/10-13/14 encode team and phase; any XP change breaks the game. The current explicit state is robust. | H commands |
| 8 | **All teams yellow** | `TeamColor=yellow` on all four Sky Battle teams; the tab/name colour is wrong. The current team colours are correct. | H NBT |
| 9 | **No time limit** | A stalemate at the mid can run forever; the current 480 s + timeout ranking avoids it. | H no command |
| 10 | **No kill credit / no drops** | `keepInventory=true` and no last-damager tracking; kills are not rewarded and gear cannot be looted. The current system has both. | H rules/commands |
| 11 | **Invisible boundary** | Players hit a barrier wall 13 blocks outside the build with no visual cue. The current world border is visible. | H blocks; M feel |
| 12 | **1.17 item NBT** | 40 commands fail on 1.21.1 until rewritten. The current code is native 1.21 API. | H count |
| 13 | **No HUD** | No sidebar/action bar; only titles and chat. The current sidebar shows alive/border/height/kills. | H commands |
| 14 | **No spectator handling** | Dead players stay at the death spot; the current teleports them above the centre. | H commands |
| 15 | **Spawn-to-rim height offset** | Spawn feet y23, inner ledge y24, rim top y25; the first bridge needs a 2-block wool climb at the rim. The current spawns and ring are at the same level. | H geometry; M feel |

### 8.3 What is reusable (read-only recommendation)

1. **Gift-box spawn pod** — 13×13×16, hollow, white base, team concrete/glazed, quartz ribbon, 2 iron at the apex, shulker + crafting table on top. It is cheap, iconic, and gives each team a private start. **H geometry; M reuse.**
2. **Four team canopies around a circular void** — a stone gallery at y25, team-coloured stepped roof, 1-wide outer rim, lower deck at y19. It creates a readable “four rooms around a pit” and a natural rotation circuit. **H geometry; M reuse.**
3. **Balloon loot towers** — wool envelope, trapdoor crate chest at +1, barrel at -12, campfire smoke. A cheap vertical POI that is not on the direct route. **H geometry; M reuse.**
4. **Wool CanPlaceOn list** — the arena's own palette as the placement allow-list. Reuse the idea (not necessarily the exact 38 ids) to keep builds clean. **H commands.**
5. **Kit** — stone sword + stone pickaxe + shears + 16 beef + iron chestplate + 64 wool. The shears/pickaxe permissions are the interesting part. **H commands.**
6. **Iron/crafting economy** — 2 mineable iron blocks per spawn + crafting tables + wood. Small, optional, no shop. **H; M intent.**
7. **Traffic-light cage** — 3×1 barrier/glass strip above each spawn, red→yellow→lime→air. **H commands.**
8. **Nested circular damage rings** — reimplement with a player-only selector and a proper damage tick; the concept is a good fit for a circular arena. **H commands; M recommendation.**
9. **Fixed 4-team symmetry** — 90° spacing, ~9-block bridge, 30-radius spawns. The spawn-to-rim height offset (2-3 blocks) is a caution, not a feature. **H coordinates; M reuse.**

---

## 9. Findings, confidence, open questions

### 9.1 Strong findings (H)

1. The arena is a **four-team circular gallery** with gift-box spawns, balloon loot towers, and a tiny mid — not a solid colosseum. **H** blocks + renders.
2. **47,874** non-air blocks; **34,751** barrier; **13,123** visible; **59** types. **H**.
3. **477** Sky Battle commands: CB-14 445, CB-15 29, CB-18 3. **H**.
4. **19 container block entities = 13 loot containers (93 items) + 5 campfires + 1 lectern; all non-loot are empty.** Plus **93** loot commands, **110** reset fills, **40** legacy-NBT commands. **H**.
5. Teams are encoded by **XP levels 10/11/12/13**; level 14 = dead/winner; 9 = countdown; 8 = lobby. **H**.
6. The border is **nested circular wither rings** at r50→5, not a world border. **H**.
7. The win check is **last team with an adventure player in the arena box**, +50 team points. **H**.
8. The reset clears **only wool/cobweb**; the bug reset **destroys containers without recreating them**. **H**.
9. The walk graph has **85 components**; the map is a set of platforms, not a continuous floor. **H**.
10. All four Sky Battle teams have **TeamColor=yellow**; `Sky_Battle_Yellw` is misspelt. **H** NBT.

### 9.2 Inferences (M)

1. The redstone button→command-chain wiring is not fully traced; the signs and the command coordinates identify Start and Bug-reset, but the exact pulse order is inferred. **M**.
2. The low canopy dips are intended to separate the four team galleries; the geometry does separate them, but the author's intent is not documented. **M**.
3. The iron blocks + crafting tables + wood are intended as an iron-gear economy; the blocks/commands support it, but there is no sign or book explaining it. **M**.
4. The lower deck y19 is a fall-catch / lower route; it is walkable and open at the inner lip, but there are no stairs. **M**.
5. The balloon crate chest is meant to be hidden; it is enclosed by trapdoors and inside the wool envelope. **M**.
6. The `tellraw` “5/10 blocks” warnings are approximate; the actual radii are 50→5. **M**.
7. The sky-lobby chain drop is the lobby→mid entrance; the team start uses teleports. **M**.

### 9.3 Guesses (L)

1. The map may be inspired by MCC’s “Sky Battle” carnival/balloon aesthetic; no external source is cited in the world. **L**.
2. The lower deck may have been intended as a second combat level; the current geometry makes it a trap. **L**.
3. The five buttons in the lobby may correspond to Start / Bug-reset / four team colours; the signs only label Start and Bug-reset. **L**.

### 9.4 Open questions

- Does the barrier roof pass skylight in 1.17.1/1.21.1? If not, the gallery is dark and the map needs hidden light. **M**.
- Does the Start button refill the containers every round, or only the normal reset? The 93 item commands exist; the trigger is not traced. **M**.
- Was the map ever played with more than 4 teams? The team set and spawns are fixed at 4. **H** fixed 4; **L** never more.
- Why do Red and Green both have 50 `POINTS` in the snapshot? **M** test residue.
- Is the “bug reset” intended to be a one-way teardown (requiring a schematic restore) rather than a live reset? **M**.

---

## 10. Files

```
README.md                     this dossier
REFERENCE_IMAGES.md           image index, what each shows, what to learn
STATUS.md                     done / blocked / open
frames/                       17 opened evidence PNGs
renders/                      all MCMDUMPs + top/iso/close-up/section PNGs
data/blocks.csv               every block type, count, share, group, use
data/blocks_by_region.csv     block x region matrix
data/blocks_mg_sb_01.csv      every non-air block in the arena (47,874 rows)
data/chests.csv               13 loot containers: type, coords, role, slots, contents
data/containers.csv           19 container block entities: 13 loot + 5 campfires + 1 lectern
data/containers.md            complete 19-row container block-entity table + reconciliation
data/loot.csv                 93 loot entries: container, slot, item, count, NBT
data/container_nbt.csv        93 items read from the source containers; 93/93 match loot.csv
data/commands.csv             477 commands: cluster, coords, kind, auto, condition, category, effect, 1.21 risk
data/command_blocks.csv       required-name alias/export of commands.csv (477 rows, identical)
data/commands_raw.csv         raw 477-row export
data/cb14_noloot.txt          readable non-loot command list
data/layout.csv               27 structures: bounds, blocks, palette, role
data/signs.csv                10 signs
data/entities.csv             4 arena entities
data/scoreboard.md            objectives, teams, XP-level state machine
data/palette.md               palette + lighting notes
data/flow.md                  narrative gameplay loop
data/*.py                     read-only analysis scripts (extract, parse, walk graph, sections, labels)
```

No game/plugin code, no build, no paste, no registry edit, no server restart. All source/tool access was read-only.



