# Cross-map synthesis — Ace Race and the reusable lessons

**Owner:** A13 · **Scope:** read-only synthesis of all completed existing-map dossiers · **Date:** 2026-11-25
**Folder:** `game-plugins/research/existing_maps/comparison/` (the only folder written)
**Rule:** no game/plugin code, no build, no paste, no registry edit, no server change. The source worlds and
shared tools were read-only.

Evidence tags: **H** = directly observed in the cited report's blocks/commands/frames; **M** = inference
from two observations; **L** = design judgement or unresolved. Every ranking below states its tag.

---

## 1. The answer in one page

**What works.** The set contains one world-class *route grammar* (Clouds 2), one world-class *palette and
skyline* (City Marathon), one world-class *vertical set piece* (Space Race), and one world-class *compact lap*
(Yeti Set Go). Around them, TGTTOS Badlands supplies the best *finish-as-a-place*, Sky Battle the best *team
identity and reset discipline*, Parkour Tag the best *start theatre and timer presentation*, and the Decision
Dome a precise lesson in *what not to scale*. None of the four Ace Race maps is a playable 40-player game.

**What does not work.** None of the four has a game loop: (1) world spawn is shared respawn state, so one
racer moves everyone's respawn; (2) there is no timer, lap counter, ordered checkpoint, finish or placement;
(3) a static "You are in 1st place" lies to 39 of 40 players; (4) the start gate is not per-round runtime
state — one-shot fills in City/Space/Yeti, a manual reset in Clouds 2; (5) the effect layers are
multiplayer-hostile in different ways — Clouds 2, City and Yeti pads target `@a`, while Space's
levitation/speed pads use `@p` but its elytra clear and both kill planes target `@a`. Space Race adds a broken
elytra grant; City adds seven no-op `gamemode` commands and an unreachable elytra layer; Yeti adds a single
elytra command that only works within simulation distance; Clouds 2 adds 31 failing commands and six
duplicated sensor banks.

**What is usable as a template.** The *lap*, not the download. Each of the four is a closed circuit (M for
Space/Yeti, whose finish/lap reading is unresolved); MCC-faithful racing is multi-lap. Use Clouds 2's
chevron/one-landmark-per-act grammar, Yeti's compact block budget, Space's lift-to-ring maths, City's
palette/lighting hierarchy, TGTTOS's finish composition, Sky Battle's team identity, and Parkour Tag's gate
theatre — then build **original geometry** and a **server-side runtime**. Do not copy a map block-for-block.
Do not ship a downloaded pack.

**What must be built around it.** A runtime that owns timer, laps, ordered checkpoints, finish, positions,
teams, per-player effects, respawn, HUD, start gate, reset, spectators and simulation-distance-independent
evaluation. A map contract/validator (40 spawns, sections, bounds, budget). An async paster for 120–300k-block
maps. Original art. A real-client elytra/riptide test. Full detail: `data/runtime_gap.md`.

---

## 2. Which map contributes which quality — ranked

Ranking is by **usefulness to a premium MCC-faithful Ace Race**, not by archaeological completeness.

| Rank | Source | Quality it contributes | What to take | What not to take | Confidence |
|---:|---|---|---|---|---|
| **1** | **Clouds 2** | **Route legibility + set-piece sequencing** | Animated chevron flow field; five colour-zoned acts; dense checkpoint/pad rows; pad colour grammar; layered containment | Global `@a` targeting; six command banks; 278 legend signs; static "1st"; 21 MB pack | **H** for the mechanics, **M** for the design ranking |
| **2** | **Yeti Set Go** | **Compactness + theme unity** | A complete lap in 28.5k blocks; engine→village→station→pool→snow; two route lines (station/pool); lodestone elytra swap; Riptide/bubble beats | One distant elytra command; global spawn checkpoints; backwards countdown; one global start gate | **H** for geometry, **M** for the lap reading |
| **3** | **City Marathon** | **Palette, lighting, skyline** | Warm masonry vs cool infrastructure; gray/white/yellow road grammar; night light pools on pads; station/train hero; one-block curb | Symmetric paint with no arrows; parallel padded forks; unreachable elytra roofs; empty concourse | **H** for blocks, **M** for readability |
| **4** | **Space Race** | **Vertical choreography** | Tuned 48/71-block lifts matched to ring heights; elytra ring as landmark + grant idea; trident rings; constant-flow water slides; chain-of-islands silhouette | Block-offset elytra trigger; global `kill @a`; material-as-trigger; two unmarked exits; one-pad chain link | **H** for commands, **M** for intent |
| **5** | **TGTTOS Badlands** | **Finish as a place + landmark journey** | MCC wall/towers/chicken ledge composition; one landmark per leg; barrier pen; 1-block bridge economy; map-contract warning | Chicken-punch finish as the primary detector; sign-click start; single-player timer; approximate reset | **H** for the build, **M** for reuse |
| **6** | **Sky Battle** | **Team identity + reset discipline** | Four gift-box pods; fixed loot; 38-block `CanPlaceOn` palette; traffic-light cages; nested border; iron economy | XP-level state machine; `@e` damage; partial reset; `fill destroy`; 85 walk components; invisible wall | **H** for commands, **M** for design |
| **7** | **Parkour Tag** | **Start theatre + timer + spectator deck** | Opposed doors, red→yellow→lime countdown; 60 s `notched_6` bossbar; role pads; barrier roof as invisible spectator floor; compact self-contained arena | XP role state; runtime redstone team split; narrow bossbar selector; broken tellraw; glow-defeats-hiding | **H** for commands, **M** for judgement |
| **8** | **Decision Dome** | **Composition discipline / anti-pattern** | One hero silhouette; team-colour verticals; ceremonial axis; compact detail budget; shallow reveal pit (3–5 blocks) | 42-deep unrailed pit; 0 seats/rooms/stairs; dial invisible from the terrace; 2 command blocks and no runtime | **H** for the model, **M** for the comparison |

**Ace Race ranking, stated plainly:** Clouds 2 is the best *teacher*, Yeti the best *budget*, City the best
*palette*, Space the best *vertical idea*. **None is the best game.** The best game is a new build that uses
all four grammars and none of their command layers.

**Duplicate note:** `zipped-map.zip` is byte-identical to `YetiSetGo.zip` (SHA-256 of `level.dat`,
`resources.zip` and every region file; `../yeti_set_go/README.md` §1). There are **four unique Ace Race
maps**, not five; the matrix has one Yeti row.

---

## 3. What works — top five, with evidence

1. **Chevron flow field (Clouds 2).** The yellow-glazed arrows' block `facing` is the reverse of travel;
   sampling every 32×32 cell gives one consistent flow around the loop (45/45 at z −32, 50/50 at z −64,
   30/30 at z 192). Direction is proven from the map, not the video. **H** (`../clouds2/README.md` §3,
   `../clouds2/renders/route_labeled.png`).
2. **One landmark per act (Clouds 2, Badlands).** Planet waterfall → glass tunnel → crater → rainbow →
   castle is five unmistakable silhouettes in one lap; Badlands does it at smaller scale (billboard → mine →
   rail → cave → mesa → MCC wall). **H/M** (`../clouds2/README.md` §10; `../tgttos_badlands/README.md` §7.2).
3. **Pad colour grammar (Clouds 2, Yeti).** Each map keeps a consistent local mapping — Clouds 2: yellow
   glazed = checkpoint/trident, red-family glazed = jump+speed, lime/orange = jump/mega; Yeti: yellow =
   speed+spawn, pink/red/magenta = jump+speed, lodestone = elytra. The player learns one map's language in
   seconds, but the same block means something else on the next map. **H**
   (`../clouds2/README.md` §4.4; `../yeti_set_go/README.md` §3.1).
4. **Tuned vertical lift (Space).** Red levitation ≈48 blocks, orange ≈71 blocks; four independent
   lift-to-ring pairs match. The author tuned pads to rings; only the grant check is wrong. **M**
   (`../space_race/README.md` F12).
5. **Finish as a destination (TGTTOS Badlands).** A 45-long MCC wall, four towers and a fenced ledge, visible
   from the mesa. A finish should be a place, not an invisible trigger. **H**
   (`../tgttos_badlands/README.md` §7.2).

Honourable mentions: City's night light pools (2,182 invisible light blocks placed on pads/gates; **H**,
`../city_marathon/README.md` §9); Yeti's 0.1 MB animation pack (**H**, `../yeti_set_go/README.md` §7);
Parkour Tag's door colour cycle (**H**, `../minigames/parkour_tag/README.md` §4.4); Sky Battle's gift-box
pods (**H**, `../minigames/sky_battle/README.md` §8.3).

---

## 4. What does not work — ranked by damage

1. **`@a` actions from per-player conditions.** Clouds 2, City and Yeti: one player on a pad gives the effect
   to everyone. In Clouds 2 one player's calcite pad clears every inventory; in all three a checkpoint pad
   moves everyone's spawn. Space's levitation/speed pads correctly target `@p`, but its elytra clear is `@a`
   and both kill planes are `@a` (item 2). **H** (`../clouds2/README.md` §4.5; `../city_marathon/README.md` §4;
   `../space_race/data/command_blocks.csv`; `../yeti_set_go/README.md` §3.1).
2. **Global kill.** Space's `execute at @a … run kill @a` kills the whole lobby from one player's foot; Clouds
   2's N² selector can do the same. **H** (`../space_race/README.md` F13; `../clouds2/README.md` §4.5).
3. **No game loop.** No timer, lap, ordered checkpoint, finish or placement in any of the four. Space's
   `LapCheck1` is never written; City's gate does nothing; Yeti's gate is not measured; Clouds 2's finish is
   a visual line. **H** (`../space_race/README.md` F2; `../city_marathon/README.md` §3.2;
   `../yeti_set_go/README.md` §3.6; `../clouds2/README.md` §10).
4. **Broken movement mechanics.** Space's elytra grant tests a horizontal offset, so flight is impassable
   (**H**, F1); City's rooftop pads are unreachable and five have the wrong block underneath (**H**, §5);
   Yeti's single elytra command only works near the station at sim 6 (**H/M**, §3.3); Clouds 2's water boots
   fail on 1.21.1 (30 commands, **H**, §9.1).
5. **Start/finish theatre without state.** One-shot fills, gates saved closed, shared buttons, and a static
   "You are in 1st place" in all four. **H** (`../space_race/README.md` F3/F4; `../yeti_set_go/README.md`
   §3.6; `../city_marathon/README.md` §3.2; `../clouds2/README.md` §4.3).
6. **Chunk-dependent rules.** Six duplicated banks in Clouds 2; one distant command in Yeti. At the review
   server's simulation distance 6, parts of **those two** courses are dead. Space's command room sits under
   the start and A12 confirms spawn chunks are kept loaded, so its global checks run there — an accident of
   placement, not a portable design. **H/M** (`../clouds2/README.md` §4.8; `../yeti_set_go/README.md` §3.3;
   `../server_import/README.md` §2.1).
7. **Packs that cannot ship.** No licence, 21 MB dominated by a copyrighted MCC track, invalid
   `sounds.json`, missing events, and an invalid Space Race archive. **H** (`../resource_packs/README.md` §7).

---

## 5. The template — a spec, not a build

This is the synthesis recommendation. It is a design target for an **original** map; it does not override
the owner-approved `game-plugins/AceRace/SPEC.md` (which plans Sunset Skyway at 3 laps and Kessler Run at
2). Where they differ, the SPEC is the build authority.

| Dimension | Recommendation | Why (source) |
|---|---|---|
| **Race format** | 3 laps for a ground/elytra circuit; 2 for a long vertical/riptide circuit. Each lap 60–90 s; total 3–4.5 min. | The downloads are single closed laps; MCC-faithful racing repeats a lap (L). `../clouds2/README.md` §3; `../yeti_set_go/README.md` §2 |
| **Players / teams** | 40 racers, 10 teams × 4. Individual race; teams only for roster, colour and coin totals. | Runtime contract; no map supplies teams. `game-plugins/AceRace/SPEC.md` §6 |
| **Footprint** | 250–350 blocks across; 30k–120k visible blocks; one paste volume. | Yeti 28.5k / 260×218 is the compact proof; Clouds 2 325×608 is the sprawl warning (`../yeti_set_go/README.md` §6; `../clouds2/README.md` §1) |
| **Sections** | 3 ordered sections + start/finish per lap, each 20–35 s. Directional gates, per-section respawn. | Clouds 2 has 111 route-ordered pad/checkpoint records but no enforced order; runtime must impose 3–4 (`../clouds2/README.md` §3) |
| **Landmarks** | 3–4 per lap, 100–150 blocks apart, each visible from the previous. One hero at the finish. | Clouds 2/Badlands sequencing; Space chain spacing (`../space_race/README.md` §6) |
| **Pads** | One effect family per block/colour; on the racing line; every 60–90 blocks; per-player, cooldown, data-driven. | Clouds 2 grammar; City rhythm; runtime scoping (`../clouds2/README.md` §4.4; `../city_marathon/README.md` §4) |
| **Vehicle beats** | One elytra beat (20–30 s) and one trident/water beat (10–15 s) per lap; never two swaps in a row. | Space rings + Yeti Riptide; City's vestigial elytra is the warning (`../space_race/README.md` §6; `../yeti_set_go/README.md` §3.4) |
| **Start** | 40-slot pen in team rows; visible gate; 10 s countdown; gate colour/opening as the clock; release all at GO; reset every round. | Parkour Tag doors; Sky Battle cages; City/Clouds start framing (`../minigames/parkour_tag/README.md` §4.4) |
| **Finish** | Directional gate + arch/checkerboard + results card; finishers become confined spectators. | TGTTOS finish-as-place; runtime finish rule (`../tgttos_badlands/README.md` §7.2) |
| **Containment** | Visible edge + invisible rail + per-player void/respawn; explicit `playBounds`, `voidY`, `spectatorBox`. | Clouds 2 barriers/kill plane; Sky Battle envelope; Decision Dome's unrailed pit is the warning (`../clouds2/README.md` §10; `../decision_dome_recreation/README.md` §2.2) |
| **Palette/light** | 3–5 base materials per act, one saturated game-furniture family, light on the racing line, fixed time. | City/City night; Clouds 2 five palettes; Yeti coherence (`../city_marathon/README.md` §9; `../clouds2/README.md` §7) |
| **Art** | Original textures/models or no pack. No downloaded pack, no MCC track. | Licensing and invalid packs (`../resource_packs/README.md` §7) |

**Pacing target.** 3 laps × ~75 s = ~3:45, with a 10 s start, 3 s per checkpoint toast, and a 15 s results
hold. The downloads cannot confirm this: no Ace Race map has a timer, and the recreation video has no race
HUD (`../video_clouds2/README.md` §1). Treat the number as **L** until a real-client run.

---

## 6. What must be built around it

1. **Runtime (13 capabilities).** Timer, laps, ordered checkpoints, finish, positions, teams, per-player
   effects, respawn, HUD, start gate, reset, spectator handling, simulation-distance coverage. The maps
   provide none of these as game state. Full table: `data/runtime_gap.md`.
2. **Map contract + validator.** 40 unique spawns with solid ground/headroom; `startStrip`, `startBarrier`;
   ordered sections with `gate`/`respawn`/`onEnter`/`onRespawn`/`killY`; `playBounds`, `voidY`,
   `spectatorBox`; pads/rings with ids; loadouts; overviews. The contract must exist before art
   (`game-plugins/AceRace/SPEC.md` §6; read-only).
3. **Simulation-distance independence.** Evaluate gates/pads/falls server-side from data, not from command
   blocks in loaded chunks. If a command layer is kept for authoring, it is a preview only. Evidence:
   Clouds 2 six banks, Yeti single distant command, Space command room (loaded only because it sits under the
   start) (`../clouds2/README.md` §4.8; `../yeti_set_go/README.md` §3.3; `../server_import/README.md` §2.1).
4. **Async paster.** 120–300k-block maps need the TGTTOS-style chunked paster; Phase 1's `BuildQueue` at
   20k blocks/tick is a known limitation (`game-plugins/AceRace/README.md`; H).
5. **Original art.** A small original pack (or none), correct `pack_format`, no MCC music, no Mojang language
   dump, no author PSD (`../resource_packs/README.md` §7).
6. **Real-client movement test.** A bot cannot fly elytra or judge a riptide. Test 40 concurrent glides,
   ring grants, water boosts, and fall/respawn under load (`game-plugins/AceRace/SPEC.md` §10; H).
7. **Acceptance matrix.** 40 players, solo, late join, quit/rejoin, DNF, time-up, skip-wait, forced map,
   restart mid-race, and a full reset between rounds.

---

## 7. Behaviours that must never be copied

A blacklist, ranked; detail and evidence in `data/what_works.md` §11.

1. `@a` as the action target from a per-player condition (Clouds 2/City/Yeti pads; Space elytra clear and kill planes).
2. Global `kill @a` (Space) / N² kill selector (Clouds 2).
3. `setworldspawn` as a per-player checkpoint.
4. Static "You are in 1st place".
5. One-shot start gate that never recloses.
6. Elytra grant by block offset, unreachable roof pad, or a single distant command.
7. Material-as-trigger (Space's bedrock/lime glass/red sandstone).
8. Command-block banks as the live runtime.
9. `gamemode adventure` with no target.
10. Shipping a downloaded resource pack.
11. Sign-click start and sign walls.
12. XP level as the state machine.
13. Reset by `fill … destroy` or a global teleport.
14. A 42-deep unrailed pit or a dial buried under its own crown.

---

## 8. Contradictions resolved

### 8.1 Super Clash villain objective — `kill_sc_vp`, not `kill_sc_vip`

**Ruling: `kill_sc_vp`, criterion `teamkill.green`.** Before this synthesis, `kill_sc_vip` appeared in no
command, scoreboard file or source report; a literal search of the source dossiers returned zero matches. It
occurs here only as the quoted transcription error. The real bug is the criterion: villains
join `Super_Clash_V`, not `Green`, so the objective is unlikely to increment. The win check actually reads
`kill_sc_v` (dummy), fed by `kill_sc_vp`; the feeder also adds to `@a`, not just the killer. Full chain and
sources: `data/contradiction_resolution.md`.

Sources: `../minigames/other/data/scoreboard_objectives.csv` line 19; `../minigames/other/README.md` §2.2;
`../minigames/README.md` §5; `../minigames/STATUS.md` line 65; command blocks at `(4987,24,-2..-4)` in
`../minigames/data/command_blocks.csv`. **H** exact strings, **M** effect.

### 8.2 Other disagreements checked

| Disagreement | Resolution | Evidence |
|---|---|---|
| A6 says Sky Race wall signs are "leftover template text"; A6c says the wool/shears signs match the kit | **A6c is correct on the kit.** "Wool free lobby / Give wool / Give shears" match the 64 wool + shears loadout; only the finish/team state is command-driven. | `../minigames/other/README.md` §4 row 3; `../minigames/other/README.md` §2.4; **H** signs + commands |
| A6 index says Sky Race Red and Orange share "the same layout"; A6c says they do not | **A6c is correct: only the start/finish and repeated gate rhythm match.** Red climbs y13–82 with 98 chains and grey cloud/branch props; Orange runs y13–54 with 24 cobwebs and no chains. | `../minigames/other/README.md` §4 row 2 and §2.5; `blocks_positions.csv`; **H** |
| A1 says `allow-flight=false`; A12's `server.properties` says `allow-flight=true` | **Both are true at different layers.** The server global is true; Multiverse enforces per-world `allow-flight: false` with `enforce-flight: true`. The runtime must set its own world policy, not rely on either. | `../server_import/README.md` §2.1; **H** config |
| PLAN says Decision Dome 0 command blocks; A5 finds 2 | **A5 and A12 are correct: 2** (egg + glowing). PLAN is stale. | `../decision_dome_recreation/README.md` §0; `../server_import/README.md` §1; **H** |
| PLAN says minigames 0 command blocks; A6/A12 find 942 | **942 is correct** (548 chain, 212 repeating, 182 impulse). | `../minigames/README.md` §1; `../server_import/README.md` §1; **H** |
| Yeti is a lap or point-to-point? | **Lap is the better reading (M).** Start/finish share the z=7 gate; the north landing ledge and finish waterfall exist only for an arrival; the station carries a checkpoint, not a finish. Point-to-point remains possible (L). | `../yeti_set_go/README.md` §2/§10; **M/L** |
| Space Race finish location | **Unresolved.** The pod is captioned "StartFinish Line" and has `LapCheck1`, but nothing detects a crossing; the right-chain bottom island is the alternative. | `../space_race/README.md` §10; **L** |
| Video vs world countdown (13/12/11 vs 9 titles) | **Different recreations, not the same build.** The video is a fan montage; use it for look/feel, not command parity. | `../video_clouds2/README.md` §1/§3.1; **H/M** |

No other report-vs-report contradiction changed a conclusion. Where a report disagreed with the PLAN's
stale counts, the report plus A12 won.

---

## 9. Evidence map

Clickable entry points for every claim above:

| Source | Dossier | Key evidence opened for this synthesis |
|---|---|---|
| Clouds 2 | [`../clouds2/README.md`](../clouds2/README.md) | [`route_labeled.png`](../clouds2/renders/route_labeled.png), `data/command_blocks.csv`, `data/checkpoints.csv` |
| City Marathon | [`../city_marathon/README.md`](../city_marathon/README.md) | [`01_overview_route.png`](../city_marathon/frames/01_overview_route.png), `data/checkpoints.csv` |
| Space Race | [`../space_race/README.md`](../space_race/README.md) | [`route_overlay.png`](../space_race/renders/route_overlay.png), `data/command_blocks.csv` |
| Yeti Set Go | [`../yeti_set_go/README.md`](../yeti_set_go/README.md) | [`route_overlay.png`](../yeti_set_go/renders/route_overlay.png), `data/checkpoints.csv` |
| TGTTOS Badlands | [`../tgttos_badlands/README.md`](../tgttos_badlands/README.md) | [`route_top_annotated.png`](../tgttos_badlands/renders/route_top_annotated.png), `data/datapack.md` |
| Sky Battle | [`../minigames/sky_battle/README.md`](../minigames/sky_battle/README.md) | [`01_top_labelled.png`](../minigames/sky_battle/frames/01_top_labelled.png), `data/loot.csv` |
| Parkour Tag | [`../minigames/parkour_tag/README.md`](../minigames/parkour_tag/README.md) | [`mg_pt_route_schematic.png`](../minigames/parkour_tag/frames/mg_pt_route_schematic.png), `data/commands.csv` |
| Decision Dome | [`../decision_dome_recreation/README.md`](../decision_dome_recreation/README.md) | [`plan_labelled.png`](../decision_dome_recreation/renders/plan_labelled.png) |
| Minigames index | [`../minigames/README.md`](../minigames/README.md) | `data/command_blocks.csv`, `data/arenas.csv` |
| TGTTOS Industry / Pits | [`../tgttos_industry/README.md`](../tgttos_industry/README.md), [`../tgttos_pits/README.md`](../tgttos_pits/README.md) | `data/LOAD_AND_1_21.md`, `data/datapack.md` |
| Resource packs | [`../resource_packs/README.md`](../resource_packs/README.md) | `data/licensing.md`, `data/compatibility.md` |
| Video | [`../video_clouds2/README.md`](../video_clouds2/README.md) | `data/route_readability.csv`, `data/timing.csv` |
| Server import | [`../server_import/README.md`](../server_import/README.md) | §2.1 settings, §3 per-world load |

---

## 10. Deliverables and limits

| File | What it is |
|---|---|
| `README.md` | this executive answer and ranked synthesis |
| `data/ace_race_matrix.csv` | 4 Ace Race rows × 18 columns (version, dimensions, blocks, commands, containers, laps/checkpoints, clarity, boundedness, mechanics, pacing, multiplayer, pack, failures, best, worst, confidence, primary sources) |
| `data/what_works.md` | adopt / adapt / reject by quality, with a ranked shortlist and a never-copy list |
| `data/runtime_gap.md` | maps vs a 40-player runtime, 13 capabilities, contract, simulation-distance consequence |
| `data/non_ace_takeaways.md` | TGTTOS portability, Sky Battle loot/teams/reset, Parkour Tag role pads/timer, Decision Dome comparison |
| `data/contradiction_resolution.md` | the `kill_sc_vp` ruling and command chain, plus the Sky Race Red/Orange layout ruling |
| `renders/comparison_sheet.png` | eight-panel visual index compiled from existing report renders (no map re-rendered) |
| `make_comparison_sheet.py` | the read-only compositor used to build the sheet |
| `STATUS.md` | done / not done / open questions |

Paths in the matrix's `primary_sources` column are relative to `data/`.

**Limits.** No live run, no RCON, no playtest. All four Ace Race maps were read from their dossiers, not
re-scanned here. Pacing is the weakest claim: no Ace Race map has a timer, and the only video is a
single-lap fan montage with no race HUD. The lap/timing numbers in §5 are **L** until a real-client test.
The pad-scope, global-kill and checkpoint findings are **H** from exact command strings (Space levitation/speed
pads `@p`; Space elytra clear and kill planes `@a`; Clouds 2/City/Yeti pads `@a`). The design rankings are **M**
judgements, stated as such.
