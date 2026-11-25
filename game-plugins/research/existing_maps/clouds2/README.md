# Clouds 2 — Electric Boogaloo: forensic dossier (A1)

Status: **complete first pass, research only**. No game code, no build, no server change.
Source world (read-only): `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Clouds_2_-_Electric_Boogaloo__Download_Version_\Clouds 2 - Electric Boogaloo`
Live world: `mcc_clouds2` on the 25580 review server (loaded by A12, read-only here).
Version: Minecraft **1.20.1**, DataVersion **3465**. Date: 2026-11-25.

Confidence tags: **H** = observed directly in the world files, NBT, commands, renders or
pack; **M** = inference from two independent observations; **L** = reasoned guess.
Every table row and claim below carries one.

Companion files:

| File | What it is |
|---|---|
| `data/route.md` | leg-by-leg route with coordinates and sightlines |
| `data/route_legs.csv` | the same route as 14 machine-readable legs |
| `data/checkpoints.csv` | 111 ordered start/finish, checkpoint, pad, elytra and gate rows |
| `data/command_blocks.csv` | **all 322** command blocks: x/y/z, type, auto, exact command, trigger, effect, reset, multiplayer, 1.21 risk |
| `data/command_failures.csv` | the 31 commands that fail under 1.21.1 (A12 cross-check) |
| `data/signs.csv` | **all 285** signs: front/back text, colour, glow, click event |
| `data/containers.csv` | **all 225** shulkers, contents and role |
| `data/blocks.csv` | **all 171** block types, count, share, use |
| `data/palette.md` | palette, gradients, lighting |
| `data/pack_mapping.csv` | map element → pack asset (A10 owns the pack inventory) |
| `data/entities.csv` | the single entity in the world |
| `renders/` | top-down, 2 iso, route overlay, 20 section renders, palette + pack evidence |
| `frames/` | two cited video frames (A11 owns the video analysis) |
| `REFERENCE_IMAGES.md` | image → what it shows → what to take |
| `STATUS.md` | done / blocked / open |

---

## 1. Provenance and world facts

| Fact | Value | Tag |
|---|---|---|
| Level name | `Clouds 2 - Electric Boogaloo` | H |
| Version / DataVersion | 1.20.1 / 3465 | H |
| Loaded chunks | 21,904 | H |
| Block bounds | x −258..66, y −64..319, z −316..291 (325 × 384 × 608) | H |
| Non-air blocks | 150,414 in 171 types | H |
| Generator | `minecraft:the_void`, `generate_features=false`, seed −8093419227102785266 | H |
| Spawn | 5, 4, 23 (angle 0) | H |
| level.dat GameType | 1 = creative | H |
| Enabled data packs | `vanilla`, `fabric` (the world was last saved on a Fabric server) | H |
| Map datapack | **none shipped**; `datapacks/` is empty | H |
| Resource pack | `resources.zip`, 21.23 MB, `pack_format 15` | H |
| Entity count | **1** (`minecraft:marker` at 26,−6,132, tag `refresh_worldspawn`) | H |
| Chests / barrels / hoppers / dispensers | **0** | H |
| Shulkers | 225 (207 lime + 18 orange), all **empty** | H |
| Command blocks | 322 (284 repeating, 23 impulse, 15 chain) | H |
| Signs | 285 (277 birch, 8 bamboo wall), all glowing, none waxed | H |
| Barrier blocks | 3,130 | H |
| Water blocks | 60,002 (39.9 % of the build) | H |

The world was authored on a Fabric client/server (`ServerBrands=[fabric]`,
`WasModded=1`) and still contains a 2 MB `data/DistantHorizons.sqlite` and a
`data/scoreboard.dat` full of objectives from a **“refresh” event datapack that is not
shipped**. The scoreboard has 14 teams (the 16 dye colours minus pink and brown),
objectives such as `refresh_memberID`, `refresh_player_x`, `refresh_settings`,
`refresh_totemtime`, and a marker tagged `refresh_worldspawn`. **H**

**Consequence:** the download is the map *build plus its command blocks*, not a
self-contained event. The orchestration layer (teams, timers, HUD, finish, stats) is
missing. This is the single most important fact for anyone planning to reuse it. **H**

---

## 2. What the map is, in one paragraph

A 1,500-block single-lap Ace Race loop through a void world: a garden start, a farm, a
climb over snow-and-waterfall sky islands, a circular waterfall (“the planet”), a
150-block glass water tunnel, a mud-brick crater village, a 250-block elytra ring
flight, a desert village with a rainbow gate, a stepped white terrace descent, a
bottom crossing, a wooden castle, two orange arches, a coral section, and a finish
corridor back into the start pen. The route is held together by jump pads, elytra
grants, checkpoints and a single kill plane. There is no timer, no lap counter and no
finish detection. **H/M**

---

## 3. Route (summary)

Full detail in `data/route.md`; overlay in `renders/route_labeled.png`.

| Leg | From → to | Theme | Key mechanics |
|---|---|---|---|
| L0 | start pen (5,4,24.5) → line z=20 | lime hedge, checkered banner | start button, 9 s countdown, trident, adventure |
| L1 | z 20 → −10 | garden | chevrons, checkpoint (11,0,16), mega jump (11,1,11) |
| L2 | z −10 → −50 | farm terraces | checkpoint bands z −17 / −37 |
| L3 | z −50 → −140 | snow sky islands | pads, slow traps z −62, checkpoints z −96/−100 |
| L4 | z −140 → −215 | high islands | 11 pads, climb y 3 → 18 |
| L5 | z −215 → −256 | planet waterfall | elytra (29,19,−234), mega jump (−38,25,−252) |
| L6 | x −88 → −240 at z −256 | glass water tunnel | swim west, red ribs every 13 blocks |
| L7 | z −283 → −150 | ruin + crater village | 14-pad stepping-stone chain |
| L8 | z −161 → +75 | ring flight | elytra, 5 rings + 3 bolt markers |
| L9 | z 75 → 210 | village, rainbow, terraces | rainbow gate z=161, terrace pads |
| L10 | z 210 → 265 | bottom crossing | vine path, blue pool, 6 pads |
| L11 | x −13 → 60, z 200 → 260 | castle | 5 stacked elytra grants, climb y −40 → −8 |
| L12 | z 200 → 60 | arches + coral | 2 orange arches, coral pads |
| L13 | z 60 → 20 | finish corridor | chevrons north, finish line z=20 |

**Direction is proven from the map itself, not from the video.** The yellow-glazed
floor arrows are animated chevrons; their block `facing` is the reverse of travel.
Sampling every 32 × 32 cell gives one consistent flow field around the loop: north up
the east side (45/45 arrows at z −32, 50/50 at z −64), west at the planet, south down
the west side (30/30 at z 192), east along the bottom (53/55 at z 224), north through
the castle (14/14 at z 192, x 32) and north through the coral into the pen. **H**
(`renders/route_labeled.png`, `scripts/arrow_field.py`)

---

## 4. The command system (322 blocks)

### 4.1 Anatomy

| Type | Count | Auto | Role | Tag |
|---|---|---|---|---|
| repeating | 284 | 283 always active | pad/effect sensors + global race effects | H |
| impulse | 23 | 0 (needs redstone) | start countdown, gate animation, reset `tp` | H |
| chain | 15 | n/a | start sound/title tails, 9-block reset chain | H |

Every block is named `{"text":"@"}` — the author labelled them all “@”. **H**

### 4.2 Six copies of the same sensor bank

The same ~45-command sensor bank exists **six times** along the loop, because a
repeating command block only runs while its chunk is loaded:

| Bank | Location (x, y, z) | Notes | Tag |
|---|---|---|---|
| A | −154..−152, −51, 179..194 | terraces; jump boost 7; has regeneration + saturation | H |
| B | −176..−174, −38, −190..−205 | crater/ruin; jump boost 6; adds elytra-on-spruce | H |
| C | −3..−1, −12, −137..−122 | right chain; jump boost 6 | H |
| D | −2..0, −8, −18..−2 | start; jump boost 6; adds `time set 200` + `gamemode adventure @a` | H |
| E | 27..43, −38, 231..233 | castle; jump boost 5; elytra on andesite; clears trident on andesite | H |
| F | 41..43, 6, −281..−266 | planet rim; jump boost 5 | H |

The banks are **not byte-identical**: A–F differ only in jump-boost level (5/6/7) and
in one or two section-specific commands. Diff in `scripts/diff_banks.py`. **H**

### 4.3 Start / reset circuit

- **Start button** (5,4,22) → 39 repeaters / 45 redstone wires in two rows at z 22–25,
  x −51..5 → 22 impulse blocks → countdown titles, gate animation, trident. The 23rd
  impulse block is the reset `tp`. **H**
- **Reset button** (−3,4,25) → impulse `tp @a 5.5 4 24.5 facing 5 5 −20` at (−5,4,25)
  → 9-block chain at (−14..−6, 4, 25): clear actionbar, stop sound, barrier, red glass,
  “Course has reset!”, `clear @a`. **H**
- The countdown is a **sequential command sequencer**, not a scoreboard timer: the
  repeater line powers one command block at a time as the signal walks west. **M**
- One **malformed** chain block at (−49,1,27) runs `setblock 5 4 22` with no block
  argument — it was meant to restore the start button. **H**

### 4.4 The mechanic language (one row per pad family)

| Block under the player | Command | Effect | Reset | Tag |
|---|---|---|---|---|
| calcite | `clear @a` | wipe every inventory | none | H |
| yellow glazed | `setworldspawn` + `item replace hotbar.0 trident` | checkpoint + trident | next checkpoint | H |
| lime shulker | `effect give @a jump_boost 1 5/6/7` + cave-vine sound | jump pad | 1 s | H |
| orange shulker | `speed 1 35` + `jump_boost 1 14` + `clear` + candle sound | mega jump | 1 s | H |
| magenta glazed | `jump_boost 1 3` + `speed 1 15` + bell | jump + speed | 1 s | H |
| red glazed | `jump_boost 1 3` + `speed 1 15` + bell | jump + speed | 1 s | H |
| pink glazed | `jump_boost 1 3` + `speed 1 15` + bell | jump + speed | 1 s | H |
| green glazed | `slowness 1 0` + `effect clear speed`/`jump_boost` | slow trap | 1 s | H |
| red terracotta (1–5 below) | leather boots `depth_strider 255` | water boots | until feet on air | H |
| spruce planks | `item replace armor.chest elytra` | elytra grant | `cave_air` or air | H |
| andesite slab | same, plus `clear @a trident` | elytra grant (castle variant) | same | H |
| cave_air at the player | `item replace armor.chest air` | elytra clear | next grant | H |
| air below feet | remove leather boots | boots clear | next red terracotta | H |
| player box at y = −60 | `kill @p` | kill plane | respawn at world spawn | H |

Global race state (applied by every loaded bank, every tick): water_breathing 255,
regeneration 255, saturation 255, conduit_power 255, dolphins_grace, weakness 255,
glowing, `enchant unbreaking 3`, `enchant riptide 1`, `kill @e[type=item]`,
`xp set @a 0`. **H**

Small sign/command drift worth recording: the legend signs say “kill at y-50 / DONT
TOUCH DY”, but the command actually tests `y=-60,dy=0`. The command wins; the sign is
stale. **H**

### 4.5 Targeting model — the multiplayer problem

This is the map's biggest technical flaw. Every pad command has the form

```
execute as @a at @s if block ~ ~-1 ~ <pad> run <action> @a
```

- the **condition** is evaluated per player (`as @a at @s`);
- the **action** targets `@a` — every player.

So in multiplayer **one player standing on a pad gives the effect to everyone**:
one player's mega jump boosts the whole field; one player's slow pad slows everyone;
one player's calcite pad clears everyone's inventory; one player's checkpoint moves
everyone's world spawn. **H**

The kill plane is worse: `execute as @a at @a if entity @a[y=-60,dy=0] run kill @p`.
`as` and `at` both fork over `@a`, so the command runs N² times, and `@p` resolves to
the player at the execution position. If **any** player is at y=−60, every player is
killed. In single-player it works. **M** (command semantics)

The start and reset are also global: one button press starts or resets the whole
server. **H**

### 4.6 A12 cross-check: 31 failing commands

A12 reported 30 pre-1.20.5 item-NBT failures plus one malformed `setblock`. My
independent count matches exactly:

| Failure | Count | Location | Why | Tag |
|---|---|---|---|---|
| `item replace … leather_boots{display:{…},Enchantments:[…],HideFlags:1}` | **30** = 5 variants × 6 banks | A (−152,−51,180..184), B (−174,−38,−200..−204), C (−1,−12,−132..−136), D (0,−8,−13..−17), E (37..41,−38,233), F (43,6,−280..−276) | item NBT was replaced by item components in 1.20.5; `leather_boots{…}` is a parse error in 1.21.1 | H |
| `setblock 5 4 22` | **1** at (−49,1,27) | start prepare chain | missing block argument; fails in 1.20.1 too | H |

Exact lines: `data/command_failures.csv`. The 30 boots commands are the **depth-strider
water-boot grant**, used in every water section. On 1.21.1 the boots never appear; the
rest of the map still runs. **H**

The `setblock` failure means the start button is never restored by the chain. In
practice the button is a stone button and pops back by itself, so the visible symptom
is only a failed command in the log. **M**

### 4.7 Server environment interaction (A12 findings)

A12 reports the live world loads under **Multiverse with survival gamemode and
`allow-flight=false`**. That interacts with the map in five ways:

1. **Elytra kick.** Vanilla survival + `allow-flight=false` disconnects a player who
   stays airborne for ~4 s. Four legs are elytra-only (L5, L8, L9, L11). The map sets
   `disableElytraMovementCheck=false` and does **not** grant `mayfly`, so on the review
   server the flight legs will kick players. Needs `allow-flight=true` (or creative/
   spectator) to test. **H/M** (setting from A12; vanilla behaviour M)
2. **Survival vs adventure.** The map's only `gamemode adventure @a` lives in bank D,
   which is loaded only near the start (z −18..−2). A player who joins or respawns
   elsewhere stays in survival until they pass the start. In survival they can break
   blocks (the map is not protected by barriers everywhere) and can take melee damage
   (weakness 255 makes that harmless). **H**
3. **Fall damage is off** in level.dat (`fallDamage=false`), so survival is not lethal;
   the only death is the y=−60 kill plane, which is only active when a bank is loaded
   (see 4.8). **H**
4. **`setworldspawn` checkpoints are world-level, not player-level.** Under Multiverse,
   respawn handling could use a `respawnWorld`/`respawnLocation` instead of the world
   spawn, in which case checkpoints would not work. **Answered (H, server config):
   `mcc_clouds2` has no Multiverse respawn-world, and the world spawn is 5,4,23.** So
   `setworldspawn` is the respawn path and checkpoints work as designed — but they stay
   shared world state, not per-player. **H**
5. **One reset for everyone.** The reset button teleports and clears all players; it
   cannot reset one racer. **H**

### 4.8 Bank coverage vs simulation distance

The banks are 68–119 blocks apart along the east side, but the ring flight sits about
**160 blocks from both B and A**, the largest uncovered span on the loop. Rough
coverage requirement:

| Course section | Nearest bank | Distance | Sim distance needed | Tag |
|---|---|---|---|---|
| start / garden | D | 0–30 | 2 | H |
| farm → right chain | D / C | ≤ 60 | 5 | H |
| high islands | C / B | ≤ 40 | 3 | H |
| planet | F | ≤ 20 | 2 | H |
| tunnel west end | B | ~88 | 6 | H |
| crater | B | ≤ 30 | 3 | H |
| **ring flight (middle)** | B / A | **~160** | **10** | H |
| village | A | ~110 | 7 | H |
| terraces | A | 0–40 | 3 | H |
| bottom | A | ~90 | 6 | H |
| castle | E | ≤ 10 | 2 | H |
| coral | D / E | ~90 | 6 | H |
| finish | D | ~30 | 2 | H |

The map's own sign recommends “render and simulation distance 4+”, but 4 chunks (64
blocks) is **not enough** for the village, the tunnel west end, the bottom or the ring
flight. At 10–12 chunks most of the loop is covered, including the ring flight at its
northern end; at 4–5 chunks the village, tunnel and ring flight are uncovered. During
an uncovered stretch the pads, the global effects and the **kill plane** do not run, so
a fall there is a soft-lock (fall to y=−64 and sit). **M** (arithmetic from measured
bank positions)

**Actual review-server setting (H, server config via A12/coordinator):**
`simulation-distance=6`, `view-distance=8`. Six chunks = 96 blocks. Against the table
above that covers the start, farm, high islands, planet, crater, terraces, castle and
finish, and just covers the tunnel west end (~88), the bottom (~90) and the coral
(~90). It does **not** cover:

- the **village** (~111 blocks from bank A) → the L9 elytra grant at (−148,−57,70..84)
  will not fire while the player is in the village;
- the **ring-flight middle** (~160 blocks from B or A) → the kill plane is inactive for
  most of L8.

This confirms the design conclusion at the real setting rather than changing it: the
map needs either a higher simulation distance or a seventh bank between z −40 and
z +90. **H** (setting) + **M** (coverage)

---

## 5. Signs (285)

- 277 `birch_sign` + 8 `bamboo_wall_sign`; all `has_glowing_text=1`; none waxed; **no
  click/hover events**. **H**
- 57 distinct texts, repeated 5–6 times: they are the author's **pad-legend panels**,
  one per sensor bank. **H**
- 7 black signs at (−3, 4..5, 21..25) are the only player-facing text: render distance
  14, simulation distance 4+, Minecraft 1.20+, disable water-drip particles, check the
  pack is working (“this block should be animated”), “RESET COURSE”. **H**
- The legend panels are colour-coded to match the pad colour (purple = effect/enchant,
  red = jump pad, lime = jump, orange = mega jump, white = utility, green = slow,
  yellow = speed/checkpoint). **H**
- Full text and colours: `data/signs.csv`.

---

## 6. Containers

- **225 shulkers, all empty.** In 1.20.1 an empty shulker block entity has no `Items`
  tag; all 225 have no `Items` tag at all. **H**
- 207 lime + 18 orange. The lime ones are the **jump-pad surface**; the orange ones are
  the **mega-jump surface**. They are functional blocks, not storage. **H**
- **Zero chests, barrels, hoppers, dispensers, droppers, furnaces.** **H**
- There are therefore **no start kits, no team kits and no hidden skips in container
  form**. Kits are granted by command: trident (checkpoint/start), elytra (spruce/
  andesite), leather depth-strider boots (red terracotta). **H**
- The only entity is a `refresh_worldspawn` marker at (26,−6,132) — evidence of the
  missing datapack, not a container. **H**

Full list: `data/containers.csv`.

---

## 7. Block palette

Summary (full table `data/blocks.csv`, analysis `data/palette.md`,
chart `renders/palette_bars.png`):

- Water 39.9 %, snow_block 18.1 %, stone 4.3 %, calcite 3.7 %, cobweb 2.9 %,
  sandstone 2.3 %, smooth sandstone 2.3 %, barrier 2.1 %, blue stained glass 1.6 %,
  lime terracotta 1.4 %.
- Five palettes: cloud white, garden green, desert warm, water/ice blue, accent set.
- One true gradient: the 7-band terracotta **rainbow at z = 161**.
- Lighting is 6 sea lanterns and skylight; fixed morning (`time 200`,
  `doDaylightCycle=false`), all players glowing.
- The pack flattens every terracotta texture to a flat colour, which is what makes
  terracotta usable as paint.

---

## 8. Map → resource-pack mapping

I own the **map side** of the mapping; A10 owns the pack inventory. Do not duplicate
A10's tables — use:

- `../resource_packs/README.md` — pack verdict, counts, hashes
- `../resource_packs/data/inventory.csv` — every file with size/SHA-256/dimensions
- `../resource_packs/data/animation_manifest.csv` — frame counts
- `../resource_packs/data/audio.csv` — every OGG
- `../resource_packs/data/lang_diff.csv` — the `Energy` rename and vanilla drift
- `../resource_packs/data/compatibility.md` — 1.21.1 pack compatibility
- `../resource_packs/data/missing_refs.csv` — missing `acerace.start/checkpoint`

My `data/pack_mapping.csv` lists, for each asset, **which map element uses it**:

| Map element | Pack asset | Evidence |
|---|---|---|
| yellow glazed chevron/checkpoint | 16-frame animated `yellow_glazed_terracotta.png`, `frametime 1` + `cube_all` model | H |
| red/magenta/pink/purple glazed pads | 7-frame animated textures, frametime 2 | H |
| every terracotta terrain block + rainbow | flat 152-byte colour textures | H |
| sea lanterns (6) | 5-frame animated texture | H |
| red reset wall | custom `red_stained_glass.png` | H |
| bell-sound pads | `block/bell/bell_use01-03.ogg` | H |
| cave-vine-sound pads | `block/cave_vines/break1-4.ogg` | H |
| candle-sound mega jump | `block/candle/extinguish1-3.ogg` | H |
| chain-sound checkpoints | `block/chain/break1-4.ogg`, `step1-6.ogg` | H |
| start brew sound | `block/brewing_stand/brew1-2.ogg` | H |
| countdown music | `records/far.ogg` (5 MB) | H |
| “Energy” item | `item/rotten_flesh.png` 2-frame + lang rename | H |

Three defects worth repeating:

- **`sounds.json` is invalid JSON** (object syntax instead of array), so none of the
  six `acerace.*` events register. A10 verified the parse failure; I verified the
  object syntax and that **no command plays any `acerace.*` event** — all audible
  feedback uses vanilla event names. **H**
- `acerace/start.ogg` and `acerace/checkpoint.ogg` are absent even if the JSON were
  fixed. **H**
- 21.22 of 21.59 MB uncompressed is OGG; three 5.28 MB cherry-trapdoor files are
  unused and Opus-encoded (A10: decode failure). Dropping the unused cherry-trapdoor,
  azalea and `acerace/*` audio leaves ~6.4 MB, dominated by the 4.96 MB music track;
  dropping all audio leaves a ~0.4 MB pack. **H/M**

---

## 9. 1.21 upgrade / compatibility notes

### 9.1 Commands

| Issue | Count | Effect | Tag |
|---|---|---|---|
| `leather_boots{…}` item NBT | 30 | water-boot grant fails; no boots in every water leg | H |
| `setblock 5 4 22` missing block | 1 | start-button restore fails | H |
| leading `/` on 35 commands | 35 | accepted by command blocks in 1.20.1 and still accepted in 1.21.1 (M) | M |
| `title … {"text":…}` JSON | 16 | still accepted in 1.21.1; SNBT preferred from 1.21.5 | M |
| `effect give … infinite 255 true` | 40+ | valid in 1.21.1 | H |
| `execute … if block`, `fill … replace`, `item replace`, `enchant`, `xp set`, `setworldspawn`, `tp … facing` | rest | valid in 1.21.1 | H |
| world upgrade 1.20.1 → 1.21.1 | — | DataVersion 3465 → 3955; `minecraft:grass` → `short_grass` (45 blocks auto-renamed); no command references it | H/M |

The map **loads and runs** on 1.21.1 except for the water-boot grant and the button
restore. The global effects, pads, checkpoints, elytra, kill plane and start/reset
circuit are syntactically fine. **H**

### 9.2 Resource pack

- `pack_format 15` vs 34 → “made for an older version” warning, still loadable. **H**
- Block models and animated `.mcmeta` still load. **H** (A10)
- `sounds.json` invalid → `acerace.*` never registers. **H**
- `block.cherrywood_trapdoor.toggle` path was renamed in 1.20.5; those three OGGs are
  likely dead in 1.21.1 (and unused by commands anyway). **M**
- `block/cave_vines/break5` is missing (pack ships 1–4) → 1-in-5 silent crack. **M** (A10)
- The cherry-trapdoor OGGs are Opus; vanilla expects Vorbis → decode failure. **M** (A10)

### 9.3 World / server

- `disableElytraMovementCheck=false` + `allow-flight=false` + survival = elytra kick
  on four legs. **H/M**
- `setworldspawn` checkpoints are the respawn path: the Multiverse respawn-world is
  empty and the world spawn is 5,4,23 (H, server config); they remain shared world
  state, not per-player. **H**
- The missing “refresh” datapack means no teams, timer, HUD or finish; the static
  “You are in 1st place” is the only HUD. **H**
- The `DistantHorizons.sqlite` and Fabric `ServerBrands` are save residue; they do not
  affect a vanilla/Paper 1.21.1 load. **M**

---

## 10. What this map does better / worse than the others

**Better**
1. **Route legibility.** The animated chevron floor + colour-coded pads + per-leg
   palette is the strongest wayfinding in the map set I examined. The player never
   needs a HUD arrow. (Cross-map ranking is A13's job.) **H**
2. **Set-piece identity.** The planet waterfall, glass tunnel, crater bowl, rainbow and
   castle are five unmistakable landmarks in one lap. **H**
3. **Boundedness.** 3,130 barriers plus a kill plane keep a 325 × 608 build playable. **H**
4. **Checkpoint density.** 111 ordered checkpoint/pad rows; a fall costs at most one
   section. **H**
5. **Mechanic variety without new items.** Jump, mega jump, slow, elytra and
   depth-strider boots all reuse vanilla blocks and effects. **H**

**Worse**
1. **No orchestration.** No timer, lap, finish, team or position. The map is a course,
   not a game. **H**
2. **Global targeting.** Every pad affects every player; the kill plane can kill the
   whole field. Unusable for a real multi-player race without a rewrite. **H**
3. **Server fragility.** Six banks are needed because of chunk loading; the ring flight
   is 160 blocks from the nearest bank, so the loop needs ~10–12 chunks of simulation
   distance and `allow-flight=true`. **M**
4. **31 broken commands** (30 boots + 1 setblock), including the entire water-boot
   mechanic on 1.21.1. **H**
5. **Dead pack weight.** 21 MB of audio, most of it unused; the custom sound index
   never registers. **H**
6. **No anti-cheat / no protection.** Survival players can break the build; there is
   no adventure lock outside the start. **H**

---

## 11. Open questions

**Answered by A12/coordinator — server config, H:**

1. **Simulation distance.** `simulation-distance=6`, `view-distance=8`. Six chunks =
   96 blocks. The village (~111 from bank A) and the ring-flight middle (~160) are
   outside coverage; see 4.8. At the start only bank D is inside 96 blocks (26–42 away);
   bank C is ~146 away and does not load.
2. **Multiverse respawn.** `mcc_clouds2` Multiverse respawn-world is **empty**; the world
   spawn is 5,4,23. `setworldspawn` is therefore the respawn path, so checkpoints work as
   designed (but remain shared world state).
3. **The “refresh” datapack.** No other imported map audited so far ships it either. It
   remains missing from this download; the scoreboard objectives and the
   `refresh_worldspawn` marker are the only traces.

**Still open:**

4. Is the `setblock 5 4 22` failure intentional (a disabled button restore) or an
   editing mistake? No evidence either way.
5. The `en_us.json` has 44 value changes vs 1.19.2 per A10; only `Energy` is
   gameplay-relevant. Confirm no other rename matters.
6. **Lap count.** The build is a closed loop but no command counts laps. Assume 1 lap.
7. **“Currency: 9.3k?” from the brief.** There is no currency item, score, entity or
   container in the shipped world. Actual totals: 150,414 non-air blocks, 832 block
   entities, 1 entity. Any 9.3k figure must come from the missing datapack or the video.
8. **Fan-recreation vs original.** The two cited frames come from a fan recreation; the
   yellow pad art in the frame does not match the download's chevron texture exactly.
   Do not use the video for palette claims.

---

## 12. Evidence index

- Commands: `data/command_blocks.csv` (322 rows), `data/command_failures.csv` (31).
- Signs: `data/signs.csv` (285).
- Containers: `data/containers.csv` (225).
- Blocks: `data/blocks.csv` (171).
- Route: `data/route.md`, `data/route_legs.csv` (14), `data/checkpoints.csv` (111).
- Pack: `data/pack_mapping.csv` (33).
- Renders: `renders/route_labeled.png`, `overview_top.png`, `overview_iso_a_labeled.png`,
  `overview_iso_b_labeled.png`, `heightmap.png`, `start_top.png`, `start_iso.png`,
  `mid_top.png`, `mid_iso.png`, `snow_top.png`, `snow_iso.png`, `planet3_iso.png`,
  `topleft_top.png`, `topleft_iso.png`, `leftcol_top.png`, `leftcol_iso.png`,
  `botleft_iso.png`, `botright_top.png`, `botright_iso.png`, `gap_iso.png`,
  `arrows_iso.png`, `tile_coral.png`, `tile_bottom.png`, `tile_w.png`, `tile_e.png`,
  `palette_bars.png`, `pack_pads.png`, `xsec_z-255.png`.
- Video (cited only, A11 owns it): `frames/f_0034_start_line.jpg`,
  `frames/f_0130_finish_approach.jpg`.
