# City Marathon — forensic dossier

**Agent:** A2 · **Owner folder:** `game-plugins/research/existing_maps/city_marathon/`
**Source world:** `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\city-marathon\City Marathon`
**Live world:** `mcc_citymarathon` · **Version:** 1.19.2 / DataVersion 3120 · **Chunks:** 6,374
**Built evidence:** 67 command blocks · 140 shulkers · 84 banners · 12 barrels · 7 signs · 39 item frames · 3 campfires · 1 blast furnace
**Status:** research complete; see `STATUS.md` for open questions.

All claims carry **H** (directly observed in region/NBT/render), **M** (inference from two or more observed facts) or **L** (guess). This is a read-only forensic pass: no game code, no plugin code, no build/paste, no registry edits, no server restart. The live server was not queried by this agent; A12's server verification is cross-referenced in `data/server_verification.md` and `data/upgrade_1_21.md`.

---

## 1. Executive findings

1. **This is a course build, not a complete game.** There is no scoreboard, datapack, timer, lap counter, ordered checkpoint, finish detection, or `kill` plane. The only state changes are short effects, a world-spawn update, an elytra swap, a one-shot gate fill, and a hard-coded actionbar — **H** (all 67 commands; no `data/scoreboard.dat` in the source; the world `datapacks/` folder is empty; `level.dat` `DataPacks.Enabled` contains only `vanilla` and `Fabric Mods`, with no map datapack).
2. **The start is a two-gate pen.** Spawn is **8,0,5**; a polished-blackstone button at **8,0,8** fires the start circuit; an invisible 18-block barrier gate at **z=10** blocks the road; a one-shot `/fill 3 0 10 13 2 10 air` clears its centre (LastExecution 110345, SuccessCount 1). The saved world still has the barrier gate closed — **H**.
3. **Seven command blocks are already broken.** All are no-target `/gamemode adventure` blocks at (11,−2,−6), (11,−2,−4), (7,−3,3), (−387,−8,45), (−387,−8,47), (−208,−2,86), (−208,−2,87); A12 verified `permissions.requires.player`; their NBT `SuccessCount=0` agrees — **H**. The Multiverse survival-gamemode implication is in §11.
4. **No kits, no team kits, no hidden item caches.** All 140 shulkers, 12 barrels, 3 campfires and 1 blast furnace are empty — **H**. The only starting items are a trident (Riptide I, Unbreaking III) and a debug stick in the playerdata — **H**. The trident is re-issued by the start circuit; elytra comes only from polished-diorite pads.
5. **The elytra layer is not reachable and half of it cannot trigger.** Six auto-equip command blocks sit under rooftop pads at y27–28 at x−127 and x−216; only E1 (x−125..−124, z−69..−63) actually has the polished-diorite surface the command checks — E2–E6 are polished andesite. The walkability graph cannot reach any of those roofs (no ladder/stairs on the towers), and there is no firework, rain, or height-gain source. Only the long ground-level diorite strip at x−351,y4,z23..154 is reachable — **H** positions, **M** conclusion.
6. **The route is a clockwise city circuit of ~1,350 blocks closed-loop (~1,400 gate-to-gate).** The pad-dense line is: start pen → East Road south → City Avenue z≈88 west → park → plaza north loop → station → West Link x−476 north → North Highway z−50 east → East Road south → finish gate. It is a reconstruction, not code-enforced — **M** (H coordinates, H pad positions, M intent).
7. **The track reads as a city, not as a unique race line.** The road hierarchy, yellow double centreline, lit pads and landmarks are strong; arrows, checkpoint numbers, lap/timer and finish detection are absent, and most sections have a parallel pad-lined alternative — **H** (geometry/commands) / **M** (readability judgement).
8. **The palette is a warm masonry city against cool grey infrastructure.** Red/brown facades (bricks, terracotta, white terracotta, mud bricks, granite) dominate; stone/andesite/stone-slab infrastructure carries the roads and station; saturated yellow/orange/green pads are the only “game furniture” colour — **H**.
9. **The station is the signature set piece but also the emptiest space.** A huge open grey concourse, an arched glass train shed, a red/blue/white train on a hazard-striped platform, and 824 sea lanterns — **H**. The concourse floor is largely undetailed and the route through it is not marked — **H** (frames 06/10).
10. **1.21 portability:** 60/67 commands use still-valid syntax; the 7 no-target gamemodes remain broken; DataFixer work is limited to signs, item frames and banner/block-entity formats. The live Multiverse per-world gamemode must be confirmed — **H** for the command failures / **M** for the upgrade path.

---

## 2. Identity and world facts

| Fact | Value | Evidence |
|---|---|---|
| Level name | `City Marathon` | H `level.dat` |
| Version / DataVersion | 1.19.2 / 3120 | H |
| World spawn | 8, 0, 5 (angle 0) | H |
| Playerdata | one UUID `9ed6d016-4235-458f-b5c9-59b56ef4900e`, pos 8.43,0,5.53; adventure; 20 HP/food; trident + debug stick | H |
| GameType / difficulty | adventure / peaceful (0) | H |
| Time / weather | DayTime 13000 (night); `doDaylightCycle=false`; clear; no weather cycle | H |
| Key gamerules | `keepInventory=true`, `doImmediateRespawn=true`, `fallDamage=false`, `drowningDamage=false`, `doMobSpawning=false`, `doFireTick=false`, `randomTickSpeed=0`, `commandBlockOutput=false` | H |
| Build bounds | x −536..22, y −64..55, z −74..166 (559×120×241) | H block dump |
| Non-air blocks / types | 207,440 / 197 | H |
| Region files | 72 `.mca`, 26.2 MB | H |
| Datapacks / functions | none (empty `datapacks/`) | H |
| Scoreboard / bossbar | no `data/scoreboard.dat`; `level.dat` `CustomBossEvents` empty | H |
| Structure/jigsaw blocks | none found | H |

---

## 3. Route

**Full leg-by-leg detail:** [`data/route.md`](data/route.md). **Labelled overlay:** `frames/01_overview_route.png` (also `renders/route_overlay.png`).

### 3.1 Main line (M; coordinates H)

| Leg | Line | Length | Key furniture |
|---|---|---:|---|
| 0 | Start pen (8,0,5) → button (8,0,8) → release gate (8,1..2,10) → start/finish gate (8,4,53) | 48 | 18 barriers at z10; four yellow-concrete posts; 3×3 pad at (8,0,53) |
| 1 | East Road x≈8, z53 → z88 | 35 | speed strips z20/33/43/69; TARDIS and y4 podium at x4,z63–71 |
| 2 | City Avenue z≈88, x8 → x−225 | 233 | 65-block yellow pad at x−27; pads x−52/−85/−150/−172; green-carpet effect clear at x−98 |
| 3 | Park west → Plaza north (z≈37–44) → CP3 | 234 | pads x−243/−294,z37; x−311,z54; x−337,z52; x−388,z51 CP3 |
| 4 | CP3 → station floor/train → West Link | 127 | train stairs x−457; train-roof pads x−445..−441; long diorite platform x−351 |
| 5 | West Link x−476, z86 → z−50 | 136 | speed strips at z5 and z−51 |
| 6 | North Highway z−50, x−476 → x8 | 484 | orange pads x−450/−420/−390; yellow x−360/−335/−310; glazed pad x−280 |
| 7 | East Road x8, z−50 → finish gate z53 | 103 | speed strips z20/33/43; finish gate z53 |

Drawn closed loop ≈ **1,352 blocks** (Manhattan sum; H arithmetic). A **gate-to-gate lap** is ≈ **1,400 blocks**: 48 (pen→gate) + 35 (gate→z88) + 1,214 (z88→highway) + 103 (highway→gate). The **z5–z53 East Road segment is traversed twice** (start and finish); the finish return passes the pen at z5 before the gate at z53, which is why the race is 48 blocks longer than the closed-loop drawing — **M**. **Direction:** clockwise on the map (east side south, bottom west, west side north, top east, east side south to finish) — **M** from the start gate and pad order; not code-enforced.

### 3.2 Start, finish and laps

- **Start pen:** spawn 8,0,5; the player faces south (SpawnAngle 0); the button at 8,0,8 is on the road 3 blocks south of the spawn — **H**.
- **Release gate:** 18 barrier blocks at z10, x0..18, y1–2; the fill clears only x3..13, leaving the side posts at x0 and x16–18 — **H**. The centre barriers are present at save despite the fill's SuccessCount=1; likely rebuilt after the tick-110345 test — **M**.
- **Start/finish gate:** four yellow-concrete posts (1,4,49), (1,4,57), (15,4,49), (15,4,57), 15 blocks apart; ground pad (8,0,53) — **H**. The drawn line crosses it **southbound** at the start (pen → z88) and again **southbound** at the finish (highway z−50 → z53); this is why the East Road pads at z20/33/43 serve both passes — **M**.
- **Countdown:** title `Starting In` + subtitles `>2<` then `>1<`; the `>3<` chain block never fired — **H** LastExecution. The `item replace … trident` and `/fill` both fired at tick 110295–110345 — **H**.
- **Laps:** none. No `scoreboard`, no `execute if score`, no finish command, no timer — **H**. `setworldspawn` on every yellow pad is the only checkpoint-like state — **H**. A second lap must be run manually; the gate is not reset — **H**.
- **Failure / kill planes:** no `kill` command, no lava; the void below y−64 is the only true kill plane. The release gate (18 barriers) and the train chassis (1,035 barriers) are solid invisible walls. `keepInventory=true` and `fallDamage=false` reduce death loss — **H**. Full detail in `data/route.md` §11.

---

## 4. Pads and effects

All effects are 1–2 seconds and re-applied every tick while standing on the pad — **H** command text. Effect levels are amplifier+1.

| Visual pad | Block | Command effect | Count / locations | Evidence |
|---|---|---|---|---|
| Yellow speed strip | yellow glazed terracotta, 3×5 | Speed IV (2 s) + `setworldspawn` | 40 strips; 15 blocks each; e.g. (8,−1,20), (−3,−1,88), (−337,4,52) | H |
| Red/pink/magenta “flower” | red + pink + magenta glazed, 3×3 | red: Speed IX or Jump Boost V; pink: Jump Boost V or Speed IX; magenta: Jump Boost V or Speed IX | 16 pads; e.g. (8,0,53), (−311,−1,88), (−425,4,52) | H |
| Orange launch pad | orange wool, 3×3 | Jump Boost X (1 s) **and** Speed XII (1 s) | 10 pads; e.g. (−361,4,52), (−417,8,88), (−450,−1,−55) | H |
| Lime pad | lime wool, 3×3 | Jump Boost VII (1 s) | 1 pad at (−248,0,−42) | H |
| Green carpet | green carpet | `effect clear` (all effects) | 35 blocks in 2 patches near (−98,82) and (−97,90) | H |
| Yellow concrete | yellow concrete | `effect clear @a` | 19 blocks; gate posts + station stair + rooftop gate | H |
| Gray wool | gray wool (asphalt) | chestplate → air (removes elytra) | 14,040 blocks; every road | H |
| Polished diorite | polished diorite | chestplate → elytra (auto-equip) | 681 blocks; E1 2×7 pad, the 9×132 checkered strip, and plaza/gate trim | H |
| Yellow carpet | (none in world) | Speed IV (2 s) | 3 dead command blocks; **0 yellow_carpet blocks exist** | H |

**Dead/duplicate logic:** the three `yellow_carpet` commands can never fire (H). The `gamemode adventure` blocks fail (H). The actionbar `"You are in 1st place"` is hard-coded and re-applied every tick by three repeating blocks — **H**.

---

## 5. Elytra zones and reachability

| ID | Command block | Pad | Ground-reachable? |
|---|---|---|---|
| E1 | CB01 (−127,27,−66) | 2×7 polished-diorite strip in a 13×11 polished-andesite roof at y28 | **No** — roof y28, no ladder/stairs |
| E2 | CB02 (−127,27,−36) | polished-andesite roof only; **no polished diorite** — the predicate cannot match | **No** |
| E3 | CB29 (−216,27,25) | polished-andesite roof only; **no polished diorite** — predicate cannot match | **No** |
| E4 | CB34 (−216,27,49) | polished-andesite roof only; **no polished diorite** — predicate cannot match | **No** |
| E5 | CB66 (−216,27,127) | polished-andesite roof only; **no polished diorite** — predicate cannot match | **No** |
| E6 | CB67 (−216,27,151) | polished-andesite roof only; **no polished diorite** — predicate cannot match | **No** |
| E7 | global CB01/02/29/34/66/67 | 9×132 checkered platform at x−351,y4,z23..154: 594 polished-diorite tiles in a 1,188-tile checker (remainder andesite) | **Yes at north end; south end blocked** |
| E8 | none | 3×3 glazed pad on the y4 start podium (x4,z66) | Yes via ladder, but it is not polished diorite, so it does **not** equip elytra |

- Among the rooftop pads, only **E1** has the polished-diorite surface the command checks. E2–E6 are **polished andesite**, so their command blocks can never equip elytra even if a player reaches the roof — **H** block IDs.
- All six rooftop pads are also unreachable in the ground graph (BFS over 65,286 walkable cells; **H**). No ladder, scaffolding, vine, water elevator or stair connects to them; the only nearby ladder at (−243,−38) tops out at y17 — **H**.
- No firework rockets exist in any container, player inventory or item frame; weather is clear and `doWeatherCycle=false`. The park pond/fountain are the only water, off the elytra pads; there is no riptide source at the pads — **H**.
- Conclusion: elytra is **vestigial/unreachable** as a course mechanic. The long strip can equip it, and E1 is the only rooftop pad with the right surface, but there is no reliable way to reach the roofs or gain height — **M**.

---

## 6. Command blocks

**Every block:** [`data/command_blocks.csv`](data/command_blocks.csv) (67 rows; exact text, coords, mode, auto/conditional, trigger, effect, reset, 1.21 risk, LastExecution, SuccessCount, A12 cross-ref).

| Mode | Count | Notes |
|---|---:|---|
| repeating | 60 | all `auto=1`; run every tick while their chunk is loaded; no redstone needed |
| impulse | 5 | start circuit: trident, title, two subtitles, `/fill` gate |
| chain | 2 | (7,−3,4) and (7,−3,3); never fired (no LastExecution) |

**Command families:** 45 `execute as @a at @s if block …` pad commands (33 effect-give, 6 elytra equip, 1 elytra clear, 3 `setworldspawn`, 2 effect clear); 6 `enchant`; 7 `title` (3 actionbar + 4 title/subtitle); 7 no-target `gamemode` (all fail); 1 `fill`; 1 trident. See CSV for exact rows.

**A12 failure list (verified):** the 7 no-target gamemode blocks listed in §1.3; exact coordinates/effects and the Multiverse implication are in [`data/server_verification.md`](data/server_verification.md).

---

## 7. Containers

**Every container:** [`data/containers.csv`](data/containers.csv) (156 rows: 140 shulkers + 12 barrels + 3 campfires + 1 blast furnace). **All are empty** — **H**.

- **140 light-gray shulker boxes**, 28 clusters:
  - 23 rooftop “AC banks” of 5 boxes each on the north office row and the x−216/−262 tower roofs, y29–30 — **H**.
  - one **vertical stack of 21** at x−12, y0..20, z49 — **H**. Likely a decorative column/elevator marker; no items — **M**.
  - four **start-gate posts** at (2,0,10), (2,6,10), (14,0,10), (14,6,10) — **H**.
- **12 barrels**, all empty, in a market stall at x−117..−107, z59..66; oak fence gates, campfires and a blast furnace nearby — **H**.
- **3 campfires**, all empty, at (−41,1,47), (−116,3,62), (−116,3,63) — **H**.
- **1 blast furnace**, empty, at (−232,4,88) — **H**.
- **No chests, trapped chests, hoppers, dispensers, droppers, furnaces, brewing stands, or item-bearing entities** — **H** block-entity/entity scan.
- **No start kits / team kits / secret item skips.** The only item grant is the start-circuit trident — **H**.

---

## 8. Signs, banners, item frames

### Signs (7) — [`data/signs.csv`](data/signs.csv)

| ID | Coords | Block / rotation | Text |
|---|---|---|---|
| S1 | −91,0,88 | acacia standing, rotation 12 (faces east) | `NO` / `ENTRY` |
| S2 | −91,0,89 | acacia standing, rotation 12 | `NO` / `ENTRY` |
| S3 | −56,1,49 | acacia wall, facing west | `NO` / `ENTRY` |
| S4 | −55,2,47 | acacia standing, rotation 3 | `NO` / `ENTRY` |
| S5 | −2,1,47 | acacia standing, rotation 13 | `NO` / `ENTRY` |
| S6 | 4,1,63 | birch wall, facing north | `D0CT WH0` |
| S7 | 4,1,71 | birch wall, facing south | `D0CT WH0` |

All are black text, glowing, no click event — **H**. The two `D0CT WH0` signs flank the TARDIS prop at x3–5,z63–71 — **M**.

### Banners (84) — [`data/banners.csv`](data/banners.csv)

Colour counts: light gray 24, black 22, red 21, yellow 5, orange 3, lime 2, magenta 2, light blue 2, blue 1, green 1, pink 1 — **H**. No `Base`/`Patterns` NBT; plain colour flags on poles at building fronts and the station — **H**. Likely team/city dressing, not a checkpoint system — **M**.

### Item frames (39) — [`data/entities.csv`](data/entities.csv)

25 normal + 14 glow item frames; 17 hold invisible anvils, 4 light-gray panes (train windows), 4 lime + 4 red + 4 orange panes (traffic-light clusters at x−31/−22, z81/95), 2 red concrete + 4 stained-glass panes (train/TARDIS lights) — **H**. All are `Invisible:1`; no armour stands, paintings or displays — **H**.

---

## 9. Build, palette and lighting

**Full palette:** [`data/palette.md`](data/palette.md) · **197-row counts:** [`data/blocks.csv`](data/blocks.csv).

- **Dominant materials:** stone slab 21,453 (10.34%), mud bricks 20,587 (9.92%), stone bricks 17,900 (8.63%), stone 16,447 (7.93%), bricks 15,532 (7.49%), gray wool 14,040 (6.77%), white terracotta 12,055 (5.81%), terracotta 11,004 (5.30%), polished andesite 7,181 (3.46%), grass block 6,739 (3.25%) — **H**.
- **Road language:** gray wool asphalt + white wool dashed lanes + yellow wool double centreline + stone-slab curb — **H**.
- **Facade gradient:** granite plinth → bricks/terracotta body → white terracotta upper band → stone-slab cornice → deepslate-tile roof — **H** column samples.
- **Park:** grass/moss/dirt-path/water with spruce/oak/azalea canopies, a fountain, a pond and a cherry tree — **H**.
- **Station:** stone/smooth stone floor, polished andesite trim, calcite columns, iron bars/glass, 824 sea lanterns; red/blue/white train with yellow/black hazard stripes — **H**.
- **Lighting:** night (DayTime 13000, no cycle); 2,182 invisible `light` blocks (mostly level 15), 824 sea lanterns, 146 lanterns, 17 wall torches, 3 campfires, 39 lightning rods, plus glow item frames — **H**. `renders/` and `frames/` are tool renders with a daylight studio sky, not in-game screenshots; do not use them to judge night brightness — **H** tool README / **M** implication.

---

## 10. Why the track is / is not readable

### What works (H/M)

- **Continuous road grammar.** The same three-band language (grey asphalt → white dash → yellow double line) is used on the East Road, City Avenue, park avenue, plaza perimeter, station approach and North Highway. A player can always tell “road” from “sidewalk” from “building” — **H**.
- **One-block curb.** Stone-slab sidewalks are a consistent one-block step above the road; they are a physical and visual boundary — **H**.
- **Pad breadcrumbs.** Saturated yellow/orange/green pads are placed every ~20–30 blocks on the main line. They are the only saturated surfaces in an otherwise warm-masonry city, so they read as “go here” — **H**.
- **Landmark sequence.** Start gate/TARDIS → park fountain → blue dome/pool → green-roof buildings → train/station → highway towers. Each leg has a distinct silhouette — **H**.
- **Night lighting.** The embedded `light` blocks put pools of light exactly on pads and the start gate, making the route legible at night — **H** placement / **M** perceived.
- **Bounding devices.** Building walls, park hedges/fences, station walls, the train body, the 18-block release gate, the 1,035-block train chassis and the void all bound the playable surface — **H**.

### What fails (H/M)

- **No direction arrows.** The road paint is symmetric; there is no arrow, chevron or “this way” board — **H**.
- **No checkpoint/timer/lap HUD.** The only HUD text is the hard-coded `"You are in 1st place"` actionbar, which is false for everyone — **H**.
- **No finish detection.** Crossing the yellow-concrete gate does nothing — **H**.
- **Parallel pad-lined forks.** Park z73 vs z88; plaza north z37/44 vs south z139; train roof vs station floor; West Link vs long platform. All are walkable and all have pads, so the “fastest line” is not communicated — **H**.
- **The direct plaza z88 line is blocked** by the blue dome and pool; no sign says “detour north/south”. A first-time player hits a dead end — **H** walkability.
- **The station is a large empty grey floor.** The train is a strong landmark, but the route across the concourse is unmarked and the space is visually under-detailed — **H** (frames 06/10).
- **The vertical/elytra layer does not read.** Six rooftop pads are unreachable; the long strip is the only usable equip pad, and it does not lead to a reachable flight — **H** positions / **M** conclusion.
- **Start clutter.** Two gates (release z10, start/finish z53), a TARDIS, a podium, a barrier wall, a dense command circuit and a 3×3 pad all sit within ~60 blocks; the intended first move is not signposted — **H**.

**Verdict:** the build is **readable as a city** and **partially readable as a course**; it is **not readable as an unambiguous race line** — **M**. It needs arrows/checkpoint boards and a finish/lap system before it can be played competitively.

---

## 11. 1.21 upgrade notes

**Full notes:** [`data/upgrade_1_21.md`](data/upgrade_1_21.md). **A12 cross-ref:** [`data/server_verification.md`](data/server_verification.md).

- **7 command failures now and in 1.21:** no-target `/gamemode adventure`. A12 verified `permissions.requires.player`; NBT `SuccessCount=0` agrees — **H**.
- **Multiverse implication:** Multiverse applies a per-world gamemode. If `mcc_citymarathon` is `survival`, the seven adventure blocks do nothing and players can break/place blocks on the course. `level.dat` GameType=1 is not a guarantee — **M**. The visible `plugins/Multiverse-Core/worlds.yml` does not contain a `mcc_citymarathon` entry (H read); A12/coordinator must confirm the live setting. Do not change it in this pass.
- **60/67 commands** use 1.19-era syntax still valid in 1.21 (`execute if block`, `effect give`, `item replace`, `enchant`, `title`, `fill`, `setworldspawn`) — **M**.
- **DataFixer:** signs `Text1..4` → `front_text`; item-frame `Item` → 1.20.5 item components; banners have no patterns; shulkers empty; command blocks preserve `Command`/`auto` — **M**.
- **Dead logic**: `yellow_carpet` predicates have no matching blocks; the `>3<` chain never fired; the actionbar is hard-coded — **H**.

---

## 12. Strengths and weaknesses (self-contained)

No cross-map ranking here; A13 owns the comparison matrix.

**Strengths**

- Coherent, mature city palette and a clear warm/cool split — **H**.
- Strong landmark sequence and a genuinely distinctive train-station set piece — **H**.
- Dense, rhythmic pad placement; the road language is consistent and legible — **H**.
- Night lighting is purposeful; the invisible light blocks are placed on the route furniture — **H**.
- The park is a well-composed green room with fountain, paths, pond and cherry tree — **H**.
- The start circuit is a complete little mechanism (button → repeaters → titles → trident → fill gate) — **H**.

**Weaknesses**

- No game loop: no timer, lap, checkpoint order, finish detection, or results — **H**.
- Seven broken gamemode commands; survival-mode risk under Multiverse — **H/M**.
- Elytra is unreachable/vestigial; the vertical layer is decorative — **H/M**.
- Most legs have a parallel pad-lined fork; the “race line” is ambiguous — **H**.
- The station concourse is vast and empty; the train carries the whole space — **H**.
- Containers are all empty; there is no kit/team/secret item design — **H**.
- The start gate is closed at save and the first move is not signposted — **H**.
- A debug stick is left in the playerdata hotbar — **H**.

---

## 13. Open questions

1. **Direction / lap count** — the route is inferred from pads, gate positions and one-way signs; no command enforces it (M/L). Was the intended race one lap, three laps, or a free-run?
2. **Elytra access** — were stairs/elevators removed, or were the rooftop pads always landing-only? No access exists now (H); intent unknown (L).
3. **21-shulker column at x−12,z49** — decorative, elevator, or leftover? Empty (H); purpose unknown (L).
4. **28 stadium-seat buttons** at x−445..−397,y5/7,z85/91 and 15 park buttons at x−30/−24,z82..94 — what did they control? No nearby command blocks or note blocks (H); purpose unknown (L).
5. **Live Multiverse gamemode** — the world is not in the visible `worlds.yml`; A12 must confirm whether the live world is adventure or survival (H read, open).
6. **Yellow-carpet predicates** — why do three commands check a block that does not exist? Likely a palette change during the build (M).
7. **Train roof** — main line or optional skip? The stairs and roof pads are deliberate (H); the direction conflicts with the westbound station approach (M).
8. **`>3<` chain** — never fired; was the countdown supposed to be 3-2-1? (H state, L intent).

---

## 14. File index

```
README.md                     this dossier
STATUS.md                     done / blocked / open questions
REFERENCE_IMAGES.md           image -> source -> what it shows -> what to take
data/command_blocks.csv       67 commands, exact text, coords, trigger, effect, reset, 1.21 risk, A12
data/containers.csv           156 containers (all empty)
data/signs.csv                7 signs, exact text/rotation/colour
data/banners.csv              84 banners, colour/coords
data/entities.csv             39 item frames
data/blocks.csv               197 block types, counts, shares, use
data/checkpoints.csv          10 start/gate/checkpoint rows
data/elytra.csv               8 elytra pads/commands
data/route.md                 leg-by-leg route, alternates, failure planes
data/palette.md               palette, gradients, lighting
data/upgrade_1_21.md          1.21 upgrade notes
data/server_verification.md   A12 cross-ref + Multiverse implication
renders/                      top-down, 2 iso, route overlay, station/train/park/plaza/roof/highway crops
frames/                       10 annotated evidence frames (opened)
scripts/                      read-only scan/render/data-build scripts used for this dossier
```

**Acceptance check:** all 67 commands accounted for (60 repeating + 5 impulse + 2 chain); all 156 containers opened and inventoried (empty); route can be followed from `data/route.md` + overlay; 10 full-resolution frames; explicit readability and 1.21 sections; every claim tagged H/M/L.
