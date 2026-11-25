# A6c — Other minigames inventory (`mcc_minigames`)

Status: **inventory complete; no build, no game/plugin code, no server writes.**
Owner: A6c. Written folder: `game-plugins/research/existing_maps/minigames/other/` only.
Source: `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3` (1.17.1, DataVersion 2730).
Live world: `mcc_minigames` (not touched; no RCON used).

This folder catalogues **eight arenas only**: `MG-HUB-01`, `MG-SC-01`, `MG-SR-HUB-01`, `MG-SR-RED-01`, `MG-SR-ORG-01`, `MG-CMD-01`, `MG-REB-01`, `MG-REB-02`. Sky Battle (`MG-SB-01`) and Parkour Tag (`MG-PT-01`) are **excluded**; A6a/A6b own those deep dives. World-level facts (18,628 chunk slots, 942 command blocks, dimensions, region layout) are A6's survey in `../README.md`; they are not re-derived here.

## 0. Method and confidence

- Read-only: A6's `README.md`, `STATUS.md`, `data/arenas.csv`, `data/command_clusters.csv`, `data/command_blocks.csv`, `data/signs.csv`, `data/islands.csv`, `data/blocks_by_island.csv`, `data/blocks_positions.csv`, and the world's `data/scoreboard.dat`. The 1.17 adapter `data/dump_117_mcmdump.py` was read but not modified.
- Containers: A14's independent source scan (`../../_qa/data/minigames_container_by_arena.csv`) supplies the exact x/y/z for the 12 empty containers in these eight arenas; no new NBT scan and no live container access. **H** for the source rows.
- Images: re-rendered from A6's existing MCMDUMPs (`renders/*.mcmdump.gz`, copied read-only into this folder) with the shared `tools/render_preview.py`, at A6c's own scales. No new region scan, no block edits.
- Because the renderer hides `barrier` and `light`, the images show **visible geometry**, not the full copy envelope. Bounds and barrier counts come from the decoded block table; every image's limits are stated in `REFERENCE_IMAGES.md`.
- Evidence scale: **H** = directly observed decoded block / exact command / exact scoreboard criterion / rendered pixel; **M** = inference from two observed sources; **L** = guess.
- All coordinates are absolute source-world coordinates. Every teleport, `fill`, `setblock`, `spawnpoint` and selector in this world is hard-coded; moving an arena means rebasing its commands as well as its blocks. **H.**

## 0b. Key findings (numbered)

1. **The hub is an 8-slot selector with 4 slots live.** Eight colour bays and eight matching deck pads exist; only orange/yellow/lime/blue have teleport commands. **H** (blocks + commands).
2. **The hub has no physical crown↔deck path.** `z 951..960` is empty at every y; the only links are teleport commands/items back to `-196 24 927`. **H** blocks/commands; **M** that a plugin/creative teleport is expected.
3. **Super Clash is a complete class-PvP loop.** Five class pads mirrored per team, five kits, 3-2-1, first to 10, fireworks, reset. **H** (commands + signs).
4. **Super Clash’s villain kill objective is suspect.** `kill_sc_vp` uses `teamkill.green`; villains join `Super_Clash_V`. **H** criteria; **M** that villain kills do not count.
5. **Sky Race Red and Orange are not the same layout.** Red climbs y13–82 with 98 chains + grey cloud props; Orange runs y13–54 with 24 cobwebs and no chains. **H** (blocks + images).
6. **Sky Race Red is the cleanest complete race loop.** 49 commands, one cluster, 2,567 visible blocks, clear start/finish, wool+shears, 9-segment reset. **H**; **M** reuse judgement.
7. **The Sky Race hub is partial.** Six pads/signs, only red and orange wired; four levels exist only as labels. **H**.
8. **The command room is Sky Battle only.** 474 commands, 57 signs, 110 wool-clear fills, 93 loot inserts, 180 s timer. A6’s README and the current atlas agree: Sky Battle logic only. **H**.
9. **Rebecca is a demo, not a game.** Kiosk leaves level 201; the only course command requires level 1; no finish/timer/reset. **H** levels; **M** intent.
10. **The 1.21.1 risk is the command layer, not the geometry.** Old item/entity NBT in command strings is the main breakage; block geometry and coordinates should survive. **H** syntax; **M** runtime.
11. **A6c owns 12 empty containers.** The world has 34 container block entities = 21 empty + 13 Sky Battle loot (93 items); A6c’s 12 are the hub 8, Super Clash 3 and Sky Race hub 1. **H** (A14 independent scan; all `items=0`).

## 1. Arena index

`Blocks` is **total / renderable**. “Renderable” excludes `barrier` and `light` (the two blocks the renderer does not draw); it is the visible build size, not the copy size. Full machine table: `data/other_arenas.csv`.

| ID | Name | Game / role | Bounds x / y / z | Entry | Blocks total / renderable | Signs | Containers | Cmds | Complete? | Conf. | Deep dive? |
| --- | --- | --- | --- | --- | ---: | ---: | ---: | ---: | --- | --- | --- |
| **MG-HUB-01** | Main lobby — crown hub | Hub / 8-bay selector | -235..-173 / 8..52 / 904..984 | `-196 24 927` (world spawn); pads at y10 | 10,161 / 10,089 | 28 | 8 | 71 | Hub built; **4 of 8** pads wired | H | Yes — flow/nav |
| **MG-SC-01** | Super Clash | Class PvP, 5 classes, first to 10 | 4922..5012 / 17..36 / -30..6 | `5000 21 0`; heroes `4972 29 -15`; villains `4926 20 -15` | 16,492 / 10,040 | 32 | 3 | 165 | Loop complete; villain kill objective suspect | H | Yes — class kits |
| **MG-SR-HUB-01** | Sky Race level select | Level-select kiosk | 19995..20013 / 39..49 / 19992..20008 | `20000 41 20000`; pads at x20010 y42 | 902 / 890 | 6 | 1 | 5 | **Partial: 2 of 6** levels wired | H | No — low value |
| **MG-SR-RED-01** | Sky Race — Red | Wool-bridge / shears race | 20991..21070 / 0..82 / 19979..20021 | `21000 31 20000`; finish `21060 55 20000` | 23,115 / 2,567 | 13 | 0 | 49 | Complete single lap; no checkpoints | H | **Yes — cleanest race loop** |
| **MG-SR-ORG-01** | Sky Race — Orange | Easy wool-bridge / shears race | 20991..21070 / 0..82 / 20979..21021 | `21000 31 21000`; finish `21063 52 21000` | 22,869 / 2,272 | 13 | 0 | 49 | Complete single lap; lower/flatter | H | Yes — difficulty variant |
| **MG-CMD-01** | Global command room | Sky Battle controller | 926..1004 / 9..23 / -18..16 | none (service room) | 1,533 / 1,533 | 57 | 0 | 474 | Complete for Sky Battle | H | Yes — logic reference |
| **MG-REB-01** | “Rebecca’s Minigames” kiosk | Teleport booth (M) | -50005..-49995 / 97..104 / -50005..-49995 | `-50000 100 -50000`; pad `-50003 99 -49998` | 309 / 309 | 2 | 0 | 2 | Partial / demo | M | No |
| **MG-REB-02** | “Rebecca” linear course | Jump course / race (M) | -50005..-49871 / 99..131 / -51005..-50946 | `-50000 100 -51000`; jump command `-49916 130 -51001` | 1,400 / 1,400 | 1 | 0 | 1 | Partial / demo; broken level gate | M | No |

**Read this table with the caveats below, not as a “complete/working” stamp.** The hub’s four unwired pads, Super Clash’s villain objective, and Rebecca’s level mismatch are real findings, not formatting.

### 1b. Container inventory

**34 container block entities exist in the whole minigames world; 21 are empty; 13 are Sky Battle loot containers containing 93 item entries; A6c’s eight arenas own 12 of the empty ones.** **H** (A14 independent source scan, `../../_qa/data/minigames_container_by_arena.csv`; all 12 rows match A14’s per-arena coordinates and `items=0`).

The 12 A6c containers are all empty and all non-loot:

| Arena | Count | Types | State | Conf. |
|---|---:|---|---|---|
| `MG-HUB-01` | 8 | 3 barrels, 2 chests, 2 shulker boxes, 1 campfire | `items=0` all | H |
| `MG-SC-01` | 3 | 2 barrels, 1 lectern | `items=0` all | H |
| `MG-SR-HUB-01` | 1 | 1 lectern | `items=0` | H |
| `MG-SR-RED-01`, `MG-SR-ORG-01`, `MG-CMD-01`, `MG-REB-01`, `MG-REB-02` | 0 | none | — | H |

Exact coordinates are in `data/containers.csv`. The command room creates Sky Battle loot chests/barrels at runtime with `item replace block`, but the source has no container block entities in that room. **H** (commands); **M** (live-world scan after a game). **A6’s current container table matches these totals**: 34 block entities, 21 empty, 13 Sky Battle loot (93 items), with hub 8, Super Clash 3 and Sky Race hub 1. **H** (current A6 README + A14 scan).

## 2. One-page inventories

### 2.1 MG-HUB-01 — Main lobby (“crown” hub)

| Field | Value | Conf. |
|---|---|---|
| Identity | World spawn and social lobby; gold “crown” ring with **8 colour game bays**; four working teleport pads, four unwired | H |
| Bounds | `x -235..-173, y 8..52, z 904..984`; 63 × 45 × 81; 22 chunk columns | H |
| Entry | World spawn `-196 24 927`, angle 90°, on the central netherite pedestal | H |
| Pads | Orange `-212 10 975` → Parkour Tag; yellow `-208 10 970` → Sky Battle; lime `-205 10 970` → Super Clash; blue `-201 10 976` → Sky Race hub | H |
| Palette | Gold block 1,690; gray concrete 1,455; smooth quartz 1,275; raw gold 801; white concrete 710; red concrete 286; pink/yellow/magenta/blue 230 each | H |
| Blocks | 10,161 total; 64 barrier; 10,089 renderable | H |
| Signs | 28: four game names, three unbuilt names (Theme Race, Gladiators, Guest’s Minigames), service labels (Lobby spawn, Tp to lobby, Effects, Pvp free lobby, Scoreboard reset, Clear inv, Set gamemode, Xp reset) | H |
| Containers | 8 empty: 3 barrels, 2 chests, 2 shulker boxes, 1 campfire; `items=0` all | H |
| Commands | 71 in 11 clusters: CB-03 (39, pad detection/team join/reset), CB-04 (22, clock/scenery), CB-05..CB-13 (10, one-off fills) | H |
| Completeness | Built as a hub; **4 of 8** bays have teleport commands. Scoreboard has `timer_themerace`, `death_themerace`, `timer_gladiators`, `death_gladiators`, so the other four bays were planned | H objectives; M intent |
| Confidence | H geometry/commands; M meaning of the four unwired bays | H/M |
| Deep dive? | **Yes** — the 8-bay selector and the crown↔deck nav gap are the two open questions | M |

**Form.** A gold crown ring on a gray/team-colour plinth, with eight 2×2 colour bays around the inner wall at y21; a central netherite pedestal at y22–23 with a floating red-glass finial at y31; a white/red hot-air balloon inside; and a separate south deck at y8–10 carrying eight matching colour pads. **H** (blocks + `frames/hub_top.png` + `frames/hub_iso.png`).

**Flow.** Players spawn on the central pedestal (`-196 24 927`) and can step down to the ring floor y21. The four working pads are on the separate south deck (see nav gap below); each is an XP-level gate: orange level 4, yellow 8, lime 15, blue 100; the command teleports to the game’s entry. **H** (commands). The other four pads — red, light-blue, pink, magenta — have no teleport commands. **H** (commands).

**The nav gap.** The crown floor is at y21; the pad deck is at y8–10. `z 951..960` is empty at every y, so there is **no block path** between them. **H** (block table). The only deck→crown links are the “Tp to lobby” pads and the lobby-teleporter item, both → `-196 24 927`; there is no crown→deck command. **H**. **M:** the map expects a plugin/creative teleport, or the deck is meant to be reached by a command not present in this export. This is the single biggest reuse caveat for the hub.

**What it proves.** The hub is a **ready-made 8-slot game selector**: eight bays, eight deck pads, four occupied. **H** (layout). The crown silhouette is strong and instantly readable; the functional part is the south deck. **M** (reuse judgement). Reuse the selector concept and the pad-gate pattern, not the exact crown. **M**.

### 2.2 MG-SC-01 — Super Clash

| Field | Value | Conf. |
|---|---|---|
| Identity | Class-based team PvP: Heroes (`Super_Clash_H`) vs Villains (`Super_Clash_V`); five classes; first to 10 kills | H |
| Bounds | `x 4922..5012, y 17..36, z -30..6`; 91 × 20 × 37; 20 chunk columns | H |
| Entry | `5000 21 0`; heroes `4972 29 -15`; villains `4926 20 -15` | H |
| Class pads | `x 5005` (villains) and `x 5007` (heroes), `y 21`, `z -17, -19, -21, -23, -25`; mirrored order | H |
| Palette | Light 6,416; tinted glass 1,443; light-blue concrete 1,053; deepslate bricks 870; stone bricks 860; grass 796; stone 743; blue concrete 718 | H |
| Blocks | 16,492 total; 36 barrier; 6,416 light; **10,040 renderable** | H |
| Signs | 32: five class labels per team wall, Heroes/Villains teleporters, kill tracking, Lobby teleporter, “Press when everybody is ready” | H |
| Containers | 3 empty: 2 barrels, 1 lectern; `items=0` all | H |
| Commands | 165 in 3 clusters: CB-19 (161), CB-20 (3), CB-21 (1); 18 impulse / 99 chain / 48 repeating; 147 auto | H |
| Completeness | Full loop: class select → kit → 3-2-1 → first to 10 → win/fireworks/reset. One suspect objective (below) | H loop; M bug |
| Confidence | H geometry/commands/signs; M villain-side outcome | H/M |
| Deep dive? | **Yes** — the five-class kit table and the asymmetric spawns are worth lifting | M |

**Form.** A tinted-glass-roofed box painted as a blue sky/cloud diorama, with a grass/dirt/stone floor, trees, and two dark tower props east of the box. The roof is opaque in top-down; `frames/sc_top.png` is a black rectangle, `frames/sc_iso.png` shows the sky mural and roof. **H** (renders + blocks). 6,416 `light` blocks are invisible illumination. **H** (count); **M** that the arena reads darker in render than in play.

**Flow.** Entry `5000 21 0`; players choose a class on the pads at `x5005/x5007`. The colour→class mapping is consistent across both team walls: red = Healer, orange = Knight, yellow = Drunkard, lime = Swordsman, blue = Wizard. **H** (signs + concrete positions). Standing on a pad at level 16 sets XP 17–21; the class chain then grants:

| Level | Class | Kit observed in commands | Conf. |
|---:|---|---|---|
| 17 | Healer | 3 golden apples; regeneration aura | H |
| 18 | Knight | Iron axe + shield (team also gets iron armour) | H |
| 19 | Drunkard | Resistance II (hunger is applied to all classes) | H |
| 20 | Swordsman | Netherite sword | H |
| 21 | Wizard | Flame bow + fire-aspect sword/axe | H |

A 3-2-1 countdown runs from the ready sign at `5002 23 -23` (buttons at `5001 24 -24` and `5003 23 -24`); the first team to 10 kills gets a title/tellraw and fireworks, then the win chain resets teams and returns players to `5000 21 0`. **H** (commands + signs).

**The suspect objective.** `scoreboard.dat` defines `kill_sc_hp` as `playerKillCount` (heroes) but `kill_sc_vp` as **`teamkill.green`** (villains). Villains join `Super_Clash_V`, not `Green`; the `Green` team exists but is unused. **H** (exact criteria + team list). **M:** villain kills therefore do not increment `kill_sc_vp`, so the villain side cannot reach 10 through this chain. This is a real bug to fix before reuse, not a cosmetic quirk.

**What it proves.** A compact, complete **class-PvP loop** with readable class pads and a small kit table. **H** (loop/commands). The arena itself is a painted diorama: reuse the kit/win logic and rebuild the box, or keep the box as a self-contained “sky arena” set piece. **M** (reuse judgement).

### 2.3 MG-SR-HUB-01 — Sky Race level select

| Field | Value | Conf. |
|---|---|---|
| Identity | Brick/stone kiosk with six colour pillars; level select for Sky Race | H |
| Bounds | `x 19995..20013, y 39..49, z 19992..20008`; 19 × 11 × 17; 4 chunks | H |
| Entry | `20000 41 20000`; six powder pads at `x 20010, y 42, z 19995/19997/19999/20001/20003/20005` | H |
| Palette | Stone bricks 201; gray concrete 168; white concrete 161; bricks 148; stone-brick slab 98; brick stairs 30 | H |
| Blocks | 902 total; 12 light; 890 renderable | H |
| Signs | 6: Red, Orange, Yellow, Green, Blue, Purple Level | H |
| Containers | 1 empty lectern; `items=0` | H |
| Commands | 5: 4 pad gates + 1 item cleanup (`kill @e[type=item,distance=0..25]`) | H |
| Completeness | **Partial: red and orange wired; four levels labelled but unbuilt** | H |
| Confidence | H | H |
| Deep dive? | **No** — only two working levels; the kiosk is a pattern, not a map | M |

**Form.** A small square kiosk with six saturated pillars on the east face and six matching concrete-powder pads at their feet. **H** (blocks + `frames/srhub_top.png` + `frames/srhub_iso.png`). The palette is deliberately neutral (brick/stone/white) so the six colours pop. **M** (design intent).

**Flow.** Red pad: level 100 → 101, then level 101 → `tp 21000 31 20000`. Orange pad: level 100 → 103, then level 103 → `tp 21000 31 21000`. **H** (commands). Yellow, green, blue and purple pads exist as blocks but have **no commands**; their levels are not in the world. **H** (commands + no course blocks elsewhere).

**What it proves.** A clean **level-select pattern**: colour pillar + powder pad + XP gate + teleport. **H** (pattern). It is worth copying conceptually, not as a build; the four missing courses make a deep dive low-value. **M** (judgement).

### 2.4 MG-SR-RED-01 — Sky Race — Red

| Field | Value | Conf. |
|---|---|---|
| Identity | Single-lap wool-bridge / shears race; team `Sky_Race`; red palette | H commands; M “race” label |
| Bounds | `x 20991..21070, y 0..82, z 19979..20021`; 80 × 83 × 43; 22 chunks | H |
| Entry | `21000 31 20000` (inside black start tower); finish on raw gold at `21060 55 20000` | H |
| Form | Black start tower (x20991–21003, y13–35) → a sequence of red-glass gate/beam/obstacle structures climbing east to y82 → red/white/gold finish basket (x21055–21065, y53–57, z19995–20005) | H |
| Palette | Barrier 20,548; black concrete 870; red concrete 374; red stained glass 325; red terracotta 196; white concrete 177; light-gray concrete 116; chain 98 | H |
| Blocks | 23,115 total; 20,548 barrier; **2,567 renderable** | H |
| Signs | 13: finish, wool free lobby, give wool, give shears, reset & start, reset only, lobby teleporter, safe spectator, fast respawn | H |
| Containers | 0 | H |
| Commands | 49 in CB-24: 13 impulse / 23 chain / 13 repeating; 35 auto | H |
| Completeness | Complete single lap; no checkpoint/gate detection; no per-player reset | H |
| Confidence | H | H |
| Deep dive? | **Yes** — the cleanest complete race loop in the set | M |

**Form.** The start tower is a black box with red panels; the course is a climbing run of red-glass arches/platforms with hanging chains and grey “cloud/branch” props in the upper half; the finish is a red/white basket with a raw-gold floor. **H** (`frames/srred_top.png` + `frames/srred_iso.png` + positions). The visible course spans y13–82; the surrounding 20,548 barrier blocks form the copy envelope, not the build. **H**.

**Flow.** Level 101 is the lobby state: adventure, team `Sky_Race`, spawn `21000 31 20000`, 3-2-1 countdown, then `xp set 101 → 102`. Racers get **64 white wool in the offhand** with a long `CanPlaceOn` allow-list and **shears** with `CanDestroy:[white_wool]`; they also get glowing, invisibility and saturation. **H** (commands). Finish on raw gold → `say finished the red course!`, XP back to 101, spectator, firework; when no one is still at level 102/adventure, a redstone pulse resets the wool. **H**.

**Caveats.** The red-glass gates/beams are **visual obstacles, not checkpoints**: there is no `if block` at the gates, only at the finish. **H**. The reset is **nine `fill … air replace white_wool` segments** spanning x21004–21069 plus the start area, not a structure reset. **H** (exact commands). The “Pvp free lobby” sign is in the pre-start lobby; team `Sky_Race` has `AllowFriendlyFire=1`, so PvP is enabled during the race. **H** (scoreboard team) + **M** (sign intent). A6 now agrees the wool/shears signs match the loadout (“Wool free lobby”, “Give wool”, “Give shears”); this folder’s commands confirm it. **H** signs + commands.

**What it proves.** The cleanest **complete race loop**: 49 commands, 2,567 visible blocks, a clear start/finish, one resource mechanic (wool + shears), and a palette that can be swapped. **H** (loop/counts). It is the best candidate for a self-contained lift. **M** (judgement).

### 2.5 MG-SR-ORG-01 — Sky Race — Orange

| Field | Value | Conf. |
|---|---|---|
| Identity | Single-lap wool-bridge / shears race; orange palette; lower/easier than Red | H commands; M difficulty intent |
| Bounds | `x 20991..21070, y 0..82, z 20979..21021`; 80 × 83 × 43; 16 chunks | H |
| Entry | `21000 31 21000`; finish on raw gold at `21063 52 21000` | H |
| Form | Same black start tower and finish-basket type; a sequence of orange gate/platform structures on a **low flat run y13–54**; 24 cobwebs; no chains/grey cloud props | H (blocks + images) |
| Palette | Barrier 20,597; black concrete 871; orange concrete 409; orange stained glass 280; orange terracotta 215; white concrete 177; black stained glass 88; raw gold 69 | H |
| Blocks | 22,869 total; 20,597 barrier; **2,272 renderable** | H |
| Signs | 13: same set as Red, orange wording | H |
| Containers | 0 | H |
| Commands | 49 in CB-25: 13 impulse / 23 chain / 13 repeating; 35 auto | H |
| Completeness | Complete single lap; lower/flatter; no checkpoint detection | H |
| Confidence | H | H |
| Deep dive? | **Yes** — as the easy half of a difficulty pair | M |

**Form.** The start tower and finish basket are the same construction as Red, but the middle is a **low, flat platform run** (visible y13–54 versus Red’s y13–82). Orange uses 24 cobwebs and no chains; Red uses 98 chains and grey cloud/branch props. **H** (block positions + `frames/srorg_top.png` + `frames/srorg_iso.png`). A6 now agrees Orange is lower/flatter and not a reskin; only the start/finish and repeated gate rhythm are shared. **H**.

**Flow.** Same 49-command skeleton: level 103 lobby → level 104 race; wool + shears; `say finished the orange course!`; spectator; nine-segment wool reset; requeue. **H** (commands). The lower height makes it the natural **easy** variant if Red is the hard one.

**What it proves.** A ready-made **easy/hard pair** with one command skeleton. The palette swap is not the only difference; a reuse plan must carry both layouts, not just recolour Red. **H** (geometry) + **M** (difficulty intent).

### 2.6 MG-CMD-01 — Global command room

| Field | Value | Conf. |
|---|---|---|
| Identity | Sky Battle game controller on a white test platform; **service room, not a playable arena** | H |
| Bounds | `x 926..1004, y 9..23, z -18..16`; 79 × 15 × 35; 14 chunks | H |
| Entry | None; no player spawn or pad | H |
| Palette | White concrete 752; chain command block 335; repeater 154; redstone wire 92; command block 80; repeating command block 59; birch wall sign 57 | H |
| Blocks | 1,533; no barrier/light | H |
| Signs | 57: settings labels (timer 5–180 s, border shrink, team wins, starting island/mid/top balloon, particle rigs, “at 1000 10 10 are all the command blocks”) | H |
| Containers | 0 in the source; Sky Battle chests/barrels are created at runtime by `item replace block` | H source; M live |
| Commands | 474: CB-14 (445) + CB-15 (29); 80 impulse / 335 chain / 59 repeating; 378 auto; 473 condition-met | H |
| Completeness | Complete for Sky Battle; **not “every game”** | H |
| Confidence | H | H |
| Deep dive? | **Yes — as a logic reference**, not as geometry | M |

**Form.** A flat white platform packed with long rows of chain command blocks, repeaters and redstone wire; a separate test rig at the north-east; a ladder rail at the south-east. The top view reads like a circuit board; the iso shows the rows are layered/stepped. **H** (`frames/cmd_top.png` + `frames/cmd_iso.png`).

**Flow.** Teams are assigned by XP 8 on coloured powder (`Sky_Battle_Red/Blue/Green/Yellw`); team levels rise 8→14; loadout commands give wool with a long `CanPlaceOn` allow-list and iron blocks. 93 `item replace block … container.N` commands seed chests/barrels at arena coordinates; 110 fills clear placed wool across 5-block strips; a 180 s timer drives border-shrink messages at 150/130/110/80/50 s; win checks are per team; spawnpoints and particle rigs are here. **H** (commands + signs).

**Caveats.** The gameplay coordinates point at the Sky Battle arena ~1,000 blocks away; nothing here is playable. **H**. A6’s README and the current atlas agree: Sky Battle logic only. **H**. One repeating block at `993 15 -14` has `condition_met=0` (disabled border-damage effect). **H**.

**What it proves.** A full, real example of a **command-block game controller**. **H** (commands). It is the best checklist in this world for what a plugin must own: team assignment, levels, loot, reset, timer, border, win detection, spectator handling. **M** (reuse judgement). It is not copy-paste geometry. **H**.

### 2.7 MG-REB-01 — “Rebecca’s Minigames” kiosk

| Field | Value | Conf. |
|---|---|---|
| Identity | 11×11 teleport kiosk; sign reads “Rebecca’s Minigames”; likely a booth to the linear course | H sign/commands; M game identity |
| Bounds | `x -50005..-49995, y 97..104, z -50005..-49995`; 11 × 8 × 11; 4 chunks | H |
| Entry | `-50000 100 -50000`; pad `-50003 99 -49998` (red concrete powder) | H |
| Palette | White concrete 151; black stained glass 121; black concrete 24; smooth quartz 4; quartz stairs 3; 2 command blocks | H |
| Blocks | 309; 2 signs; 2 commands; 0 containers | H |
| Containers | 0 | H |
| Completeness | Partial / demo: two-stage XP gate into the course; no game loop | H |
| Confidence | H geometry/commands; M identity | H/M |
| Deep dive? | **No** — a teleport booth, not an arena | M |

**Form.** A small white kiosk with a dark glass roof and a single red-concrete-powder pad; the top view is almost entirely roof. **H** (`frames/reb1_top.png` + `frames/reb1_iso.png`).

**Flow.** Level 200 on the pad → XP 201; level 201 on the pad → `tp -50000 100 -51000`. **H** (commands). The sign says “Lobby Teleporter”, but the commands are **one-way into the course**; there is no return command in this kiosk. **H**.

**What it proves.** A teleport booth / demo entrance, not a game. It is useful only as evidence that the Rebecca area was a separate side project. **M**.

### 2.8 MG-REB-02 — “Rebecca” linear course

| Field | Value | Conf. |
|---|---|---|
| Identity | 135-block linear jump course over lava; parkour/race (M); demo | H geometry/command; M game identity |
| Bounds | `x -50005..-49871, y 99..131, z -51005..-50946`; 135 × 33 × 60; 17 chunks | H |
| Entry | `-50000 100 -51000` (start platform); jump command block `-49916 130 -51001`; finish `-49874 124 -50985` (yellow carpet) | H |
| Palette | White concrete 1,030; lava 218; stone 66; waxed copper 52; yellow carpet 24; smooth quartz 4 | H |
| Blocks | 1,400; 1 sign; 1 command; 0 containers | H |
| Containers | 0 | H |
| Completeness | Partial / demo: no finish, timer, respawn or reset; **jump pad requires level 1, kiosk leaves 201** | H command; M non-functional |
| Confidence | H geometry/command; M identity | H/M |
| Deep dive? | **No** — curiosity only | M |

**Form.** A white concrete start platform and beam, stepping stones, a 25×9 lava field (218 lava blocks), a staircase of 2×2 waxed-copper jump pads rising from y101 to y127, and a yellow-carpet finish. **H** (`frames/reb2_top.png` + `frames/reb2_iso.png` + positions).

**Flow.** The only command is `execute as @a[level=1] … if block ~ ~-1 ~ waxed_copper_block run effect give @s jump_boost 1 5 true`. The kiosk leaves the player at **level 201**, and no command resets to 1; the jump pad therefore never triggers as shipped. **H** (exact levels) + **M** (non-functional). There is no finish detection, timer, respawn or reset. **H**.

**What it proves.** A hand-built jump demo, not a game. It is the weakest of the eight and should not consume a deep dive. **M**.

## 3. Strongest candidates for future reuse

Ranked by “how much of a working game can be lifted with the least reinterpretation”. This is an assessment, not an implementation plan.

| Rank | Arena | Reuse as | Why it is strong | Caveat |
|---:|---|---|---|---|
| **1** | **MG-SR-RED-01** | Self-contained **wool-bridge race** | 49 commands, one cluster, 2,567 visible blocks, clear start/finish, one resource mechanic (wool + shears), palette-swappable, full reset/requeue loop. **H** | No checkpoint detection; 20,548 barrier blocks; all coords absolute; old item NBT |
| **2** | **MG-SC-01** | **Class-based team PvP** | 165 commands, five class pads mirrored per team, five kits, 3-2-1, first-to-10, win/fireworks/reset; compact 91×20×37 box. **H** | Villain objective `teamkill.green` is suspect (**H** criteria, **M** effect); asymmetric spawn heights; painted diorama box |
| **3** | **MG-SR-ORG-01** | **Easy variant** of the race | Same 49-command skeleton, lower/flatter y13–54, 2,272 visible blocks, cobwebs instead of chains. **H** | Not a pure reskin; carry the different layout, not just the palette; no checkpoints |
| **4** | **MG-HUB-01** | **8-slot game selector / front-end** | Eight bays + eight deck pads already built; four wired; strong crown identity; 71 commands. **H** | Only 4/8 games; no physical crown↔deck path (**H**); the four unbuilt bays need arenas |
| **5** | **MG-CMD-01** | **Logic checklist / controller reference** | 474 commands covering teams, levels, loot, 110-fill wool reset, 180 s timer, border shrink, win checks, spectator. **H** | Service room only; hard-coded to Sky Battle; 1.17 NBT; not copy-paste |

**Bottom line.** If one arena is lifted as a **playable unit**, take **Sky Race Red** (with Orange as the easy variant). If the goal is a **game-design reference**, take **Super Clash** (class kits) and the **command room** (controller scope). If the goal is a **lobby**, take the hub’s **8-bay selector concept**, but plan a physical route between the crown and the pad deck. **M** (judgement from the evidence above).

## 4. Corrections and resolved findings

**No active contradictions against current A6.** The two A6c findings that were originally corrections are now reflected in A6; the table keeps the evidence trail rather than re-opening them. A6’s files were not edited. **H** unless marked.

| # | Finding | Current A6 text | A6c evidence |
|---:|---|---|---|
| 1 | **Orange vs Red layout** | A6 §5 now says Red climbs y13–82 with 98 chains + grey cloud props; Orange runs y13–54 with 24 cobwebs and no chains; Orange is **not** a reskin. | `blocks_positions.csv`; `frames/srred_iso.png` vs `frames/srorg_iso.png` |
| 2 | **Sky Race sign intent** | A6 §5 now says the wool/shears signs **match** the loadout (64 white wool offhand + shears with `CanDestroy:[white_wool]`) and are not leftover template text. | H signs/commands; M “Pvp free lobby” intent. `signs.csv`; `command_blocks.csv`; `scoreboard.dat` teams |

**Removed phantom rows.** Earlier A6c rows that duplicated or contradicted current A6 — CMD scope, hub completeness, Super Clash completeness, Rebecca identity/teleport, and source-container count — were removed. Current A6 already says CMD is Sky Battle-only, the hub is 4/8 with no path, Super Clash has the villain-objective bug, Rebecca is **M**, and the world has **34 containers / 21 empty / 13 loot (93 items)**. A6 also lists hub 8, Super Clash 3 and Sky Race hub 1; §1b matches those counts. **H** (current A6 README + A14 scan).

## 5. 1.21.1 compatibility risks — high level only

A12 owns the exact syntax matrix. These are the risks that affect **these eight arenas**, at the level a reuse decision needs.

| Risk | Where | Severity | Why |
|---|---|---|---|
| **Item NBT → item components** (`CanPlaceOn`, `CanDestroy`, `Unbreakable`, `Enchantments`, `display.Name`) | Sky Race Red/Orange, Super Clash kits, Sky Battle loadout, hub carrot-on-a-stick | **High** | 1.17 commands embed NBT in the command string. A world upgrade does not rewrite command strings; these commands will fail or lose behaviour on 1.21.1. **H** syntax; **M** runtime. |
| **Sign NBT migration** (`Text1..4` → `front_text`/`back_text`) | All 187 signs, especially 57 command-room labels | Medium | 1.20 changed sign block-entity format; a world upgrade normally migrates it, but a schematic/structure export can lose it. Verify after any copy. **M** |
| **Firework entity NBT** (`FireworksItem` with nested item tag) | Super Clash win, Sky Race finish, Sky Battle win | Medium | Firework item data moved to components; the summon NBT is 1.17-style. **H** syntax; **M** runtime. |
| **`spawnpoint <targets> <pos> <angle>`** | Sky Race, Super Clash | Low | Still valid in 1.21.1; verify selector order. **M** |
| **`teamkill.green` criterion** | Super Clash villains | Low (syntax) / High (logic) | The criterion still exists, but the team is wrong for the villains; this is a logic bug, not a 1.21 syntax issue. **H** criteria; **M** effect. |
| **`scoreboard objectives setdisplay sidebar.team.green`** | Super Clash | Low | Team-colour sidebar slots still exist; verify the team name. **M** |
| **`effect give … <amplifier> <seconds> true`** | All arenas | Low | Boolean “hide particles” argument is still accepted. **M** |
| **`title … {'text':…}` JSON** | All arenas | Low | Raw JSON text is still accepted; later 1.21.x text-component changes are beyond 1.21.1. **M** |
| **`fill … replace <block>`** | Sky Race wool reset, hub glass recolour, command room wool clear | Low | Still valid; the 1.17 syntax is stable. **M** |
| **`light` block** | Super Clash (6,416), hub (8), Sky Race hub (12) | Low | `light` exists in 1.17+ and 1.21.1; it is invisible, so keep it out of block-count comparisons. **H** |
| **Command-block `auto` / `conditionMet` NBT** | All 942 command blocks | Low/Medium | World upgrade should preserve block entities; a structure export may not. Re-check after copy. **M** |
| **World height and coordinates** | All | Low | 1.17 y0–255 → 1.21 -64..320; existing absolute coordinates remain valid. **M** |

**High-level verdict:** the **geometry** is likely to survive a 1.21.1 world upgrade; the **command strings** are the risk. Any reuse should treat the command layer as 1.17-only and re-author item/entity NBT, not assume a version bump fixes it. **M.**

## 6. Files in this folder

```
README.md                     this dossier
STATUS.md                     done / blocked / open questions
REFERENCE_IMAGES.md           16 evidence images + 2 A6 context images, what each proves
frames/                       16 full-resolution PNGs (top + iso per arena)
renders/                      8 MCMDUMPs (A6 source, copied read-only) + the same 16 PNGs as working renders
data/other_arenas.csv         all 8 arenas, all fields (bounds, entry, palette, counts, containers, confidence, deep-dive)
data/containers.csv           12 empty container block entities in A6c arenas, exact x/y/z
data/commands.csv             per-arena command summary (counts, kinds, top verbs, key commands, purpose)
data/scoreboard_objectives.csv 26 objectives with exact criteria (proves the Super Clash kill-objective issue)
data/teams.csv                15 teams with friendly-fire flags
data/palette.md               per-arena palette table + read
data/route.md                 one-paragraph route/access summary per arena
renders/README.md             source dumps + exact render scales
```

## 7. Explicitly not done / boundaries

- No deep dive of **Sky Battle** (`MG-SB-01`) or **Parkour Tag** (`MG-PT-01`) — A6a/A6b own those.
- No analysis of the TGTTOS maps, Ace Race maps, Decision Dome or resource packs — other agents own those.
- No build, no paste, no schematic, no plugin/game code, no registry edit, no server restart, no RCON.
- No 1.21 fix: compatibility is described at risk level only; A12 owns the exact matrix.
- No re-scan of the 18,628-chunk world: counts are A6’s decoded data, spot-checked against `blocks_positions.csv`; world-level totals remain A6’s.
- No claim that the live `mcc_minigames` world still has all 187 signs or the exact same block entities; the source was read, the live world was not. **M** for live parity.
