# Parkour Warrior (McmParkourWarrior) — spec v2

Game id: `parkourwarrior` — start with `/mcm play parkourwarrior`. World: `mcc_parkourwarrior` (void, created at plugin enable, old folder deleted on first load).
Design: `SPEC_v2.md` (owner-approved). The old three-lane design is gone.

## How it plays
- Everyone spawns on one start island and runs the same course individually. Players never collide (scoreboard team with collision `NEVER` on every runner's scoreboard, re-applied each tick because the core replaces scoreboards every second) and are shown as translucent ghosts (invisibility + "see friendly invisibles"; `/pw ghost off` shows them fully).
- Main line: 5 zones x 3 obstacles. Stepping on an obstacle's completion pad gives a stone medal (once), sets the checkpoint ("Spawn point updated" action bar + chat line "You completed <name> and earned a stone medal!"). The 3rd obstacle of a stage prints "You completed the <Zone> zone!".
- Each hub has a bonus path of 3 harder obstacles: bronze, silver, gold (each once). After the 3rd the player is returned to the main line automatically. The **Teleportation Scroll** (paper, slot 8) returns from a bonus path to the main-line checkpoint, medals kept; it is taken away with a chat notice at 30 s left.
- After stage 5: fork pavilion with Easy / Medium / Hard gates (2 obstacles each). The **Return to the Fork** compass (slot 9) lets a player switch ending until 30 s left (walking back to another gate also works until then). Finishing broadcasts "<player> finished the entire course!", adds +0.15 / +0.35 / +0.80 to the team multiplier and turns the player into a spectator (clickable overview links, `/pwview <n>`).
- 10:00 then 30 s overtime. Normal time: a fall teleports you to your last checkpoint (red chat line), nothing lost. Overtime: a fall ends your run (spectator).
- Game ends after overtime, or when no online runner is still active. `finish` is called once, after a short results screen.
- No damage, hunger, drops, item moves, block edits, door/trapdoor/button use in the world. Daylight goes morning -> sunset.
- Rejoining players resume at their checkpoint with their medals; new late joiners spectate.

## Scoring (real MCC coins, Parkour Warrior Dojo; config `coins.*`, see `../COINS.md`)
- Stone medal 2 each. Metal medals by count held (1..5): bronze 5/15/25/40/60, silver 10/25/45/70/100, gold 15/30/55/90/140 (totals; each new medal adds the difference). Medal coins are awarded live with `ctx.addPoints`.
- At the end, per side: extra = sum(members' medal coins) x (team multiplier - 1), split by ending weight 0.40 / 0.45 / 0.65 / 1.10 (none / Easy / Medium / Hard), largest-remainder rounding, awarded per player. Quitters still count. Solo players are their own side.
- The core then applies the event multiplier (x1.0 ... x3.0) to every award.

## HUD
- Own per-player boss bar "TIME LEFT m:ss" (red "OVERTIME" in overtime) + medal row; the core boss bar also gets `setTimer`.
- Sidebar (core, same for everyone): MEDALS OBTAINED top 3, top team multiplier, finished count.
- Personal info the shared sidebar cannot show (main challenges left n/15, your team multiplier) is in the idle action bar; your medals are in the boss bar and hotbar counters (slots 1-4).

## Map contract
`CourseModel` (pure Java) is the contract with the map package; `map.CourseMap.generate(long seed)` is found by reflection (`map.generator: auto|coursemap|stub`); `stub.StubCourse` is a plain test course. The runtime only toggles `timed` blocks (particles + optional ghost block during `warnTicks` before they appear). Missing ids/hubs/finishes are logged at build.

## Maps: classic + two private replica maps (v3)
Each game picks one map at random from `map.pool` (`classic`, `mcc6_replica`, `mcc10_replica`); `/pw map <id>` forces one for the following games, `/pw map random` undoes it, `/pw map` lists them.
- `classic`: the built-in course, unchanged.
- `mcc6_replica`, `mcc10_replica`: the fan replicas of the MCC 6 and MCC 10 Parkour Warrior courses. Same 36-slot layout (5 stages x 3, a 3-obstacle bonus branch at every hub, three endings). Replica sections that the jump simulation can route are pasted in (gaps the replica cannot be jumped across get a few listed stepping stones); the rest of the slots hold the new moving-trap sections (swinging barrels, revolving doors, conveyor belts, monkey bars, crushers, blinking beams, dropping ledges, checkerboard swap, see `map/Mechanics.java`) or classic obstacles. The slot list of each map is in its `course.txt`.
- **Private data, never in git or the jar.** The block data lives only on the server in `plugins/McmParkourWarrior/maps/<id>/` (`course.txt`, `routes.txt`, `*.mcmi.gz`). Without that folder the game falls back to the classic course.
- **Credit:** both replica worlds carry the in-world sign "Map made by: TryHardOfDoom" (the MCC 6 world is also listed on Planet Minecraft as Throw_Away_Account). The credit is on a sign at the start island, in the game-start chat line and on each pasted section.
- Moving traps: every one has a safe waiting spot, at least 1 s of warning (glow, creak, grind, indicator blocks) and is checked by `Validate` for every start timing (mover sweep, crumble deadlines, belts in `Phys`, timed-block phase windows). Maths copied from TGTTOS (`FeatureRuntime`, `MapFeatures`); Tgttos is untouched.
- Offline tools: `tools/ImportRoute.java` (`-Dpw.ease=1` adds stepping stones), `PreviewMain outDir seed mapDir` (full validation of a map), `-Dpw.only=B1-1,B2-2` limits validation to those obstacles.

## Map `mcc6_full`: the MCC 6 replica imported 1:1 (format `linear`)
The MCC 6 replica world ("Championship Parkour", 1.15.2, Throw_Away_Account / TryHardOfDoom) as it is: every block except the command machinery, one single line of 24 sections (8 stages x 3), no hubs, bonus paths, endings or difficulty branches, no stepping stones, nothing added or removed for playability. Its command blocks are replaced by plugin code:
- **Checkpoints:** `cp N box ... respawn ...` in `course.txt` are the exact volumes the replica's `@p[x=,y=,z=,dx=,dy=,dz=]` command blocks tested (the player's hitbox must overlap the box, like the selector) and the respawn the replica teleported its "Test" armor stand to. Touching one (any time, forward only) pays `coins.course.mcc6_full.checkpoints[N-1]` through `ctx.addObjectiveCoins(.., "Checkpoint N completed")`, prints "You completed Stage a-b (name)!" and sets the spawn point. The spawn is the replica's (7.5, 15, 2.5).
- **Fall:** feet below y -30 (the replica's `y=-100,dy=70` command) -> last checkpoint ("You fell! Returning to last checkpoint..."); in overtime a fall ends the run as usual. **Finish:** the tower volume (x -54..-52, y 47..53, z -33..-31): "<player> finished the entire course!", `coins.course.mcc6_full.finish`, spectator. Timer, overtime, ghosts, spectators, HUD are the plugin's.
- **Coins** (`config.yml`, `coins.course.mcc6_full`): checkpoints 4 coins (stage 1) ... 8 (stage 8), 139 for all 23, finish 40, no multipliers; a map without that block gets `coins.main` x stage scale and `coins.ending.hard`.
- **Data** (private, never in git or the jar): `plugins/McmParkourWarrior/maps/mcc6_full/{course.txt, mcc6_full.mcmi.gz}` (~166 KB) written by `tools/mcc6_import.py <world>/region <out dir>` (stdlib python, run with `python -I`; the world is only parsed, never executed). Needs `mcc6_full` in `map.pool`. The importer removes only: command / chain / repeating command blocks, structure/jigsaw blocks, the 15 barrier markers, the repeaters / comparators / wire / redstone blocks / pistons / buttons / levers / torches / pressure plates that sit in clusters with command blocks, instruction / teleporter / credit signs (the 24 "Stage a-b" signs and blank signs stay and get their text from `sign` lines), entities (19 paintings, the armor stand) and block-entity data (banner patterns, chest contents); it converts 1.15 -> 1.21.1 block states (grass -> short_grass, grass_path -> dirt_path, cauldron[level] -> water_cauldron, wall booleans -> none/low/tall).
- **Checks:** `tools/mcc6_verify.py <world>/region <server>/mcc_parkourwarrior/region` diffs the source against the world the server built (block by block, no extras); `tools/mcc6_reach.py <map folder>` is a heuristic jump-chain audit per section (it cannot judge ladders / slime / vines).
- Map Edit (`/mcm mapedit parkourwarrior mcc6_full`) works: the course is one long map with an anchor per section (`M1-1` .. `M8-3`, `hub1` .. `hub8`).
- Known quirks of the source, kept: section 1-1 ("The Lofty Leap") has an ~11-block gap from the stair top to the Mesa island; checkpoint 23's respawn (-32.5, 24, -31.5) is 47 blocks past its pad; checkpoints can be touched out of order in the replica, here they only move forward.

## Admin
- `/pw map [id|random]`: choose the map of the following games.
- `/pw list`, `/pw tp <id|start|fork|hub N|view N>`, `/pw complete <id> [player]`, `/pw time <seconds-left>` (negative = into overtime), `/pw info`, `/pw ghost [on|off]`, `/pw rebuild [seed]` (op). `/pw ghost` (no argument = toggle) switches whether runners see each other as translucent ghosts (on, default) or fully visible (off); it applies at once to a running game, is saved as `ghost-players` in config.yml and shown in `/pw info`. Player collision stays off in both modes (team collision rule `NEVER`; the plugin does not use `setCollidable(false)`, which on Paper would also make players untargetable by projectiles). Mid-game, `/pw tp <id>` sets the runner's checkpoint to that obstacle's start (no medals); `/pw complete` awards it as if the pad was touched.
- Testing hooks (BRIEF "Admin testing hooks"): `skipWait()` ends the countdown or the results screen; `skipRound()` returns false (single course); fast-test mode (`McmApi.isFastTest()`, read by reflection) cuts the countdown, title card, bonus auto-return delay and end celebration to <= 1 s, not the 10:00 timer. Methods are declared without `@Override` for older cores.

## Defaults, not researched (config.yml)
Countdown 10 s; scroll/return destination = main-line checkpoint (= the hub of that bonus path in normal play); auto-return delay 30 ticks; finished / knocked-out players -> spectator with overview teleports; results screen 6 s; start leash 4 blocks; daylight 0 -> 12500; translucent ghosts on, full hiding of nearby players off (`players.hide-within`, it also hides tab entries); global fall line 8 below the lowest block; spectator bounds margin 40; obstacle lengths/jumps, moving-block timing and kill heights come from the map (kill-Y default lowest foothold - 6). Researched but configurable: 10:00 / 30 s, 30 s cut-off, scoring tables, multipliers, weights.

## Acceptance checklist mapping (SPEC_v2)
1 one course: map + start island, Progress graph has no lanes. 2 stone medal + checkpoint + feedback: `complete/award`. 3 bonus bronze/silver/gold once: `award`, `Run.completed`. 4 falls -> checkpoint within 1 tick, never ahead (only candidate obstacles of the current checkpoint count): `checkPlayer/fell/onFall`. 5 scroll + removal at 30 s: `useScroll/removeScrolls`. 6 three gates, switch until 30 s: fork item + `padCandidates`. 7 multiplier once per player, broadcast, spectator: `finishRun`. 8 overtime knock-out: `onFall/knockOut`. 9 end + `finish` once: `endGame/doFinish`. 10 scoring: `Scoring` (offline-checked: perfect 75, shares sum). 11 HUD: `updateHud`, `giveItems`. 12 rules: `GameListener`, ghost team. 13 timed blocks + indicator: `CourseWorld.updateTimed` (count is the map's job, warned if < 3). 14/15 difficulty + validators: map agent. 16 daylight: `secondTick`. 17 stop/quit/rejoin: `stop/cleanup`, `onQuit`, `onLateJoin`. 18 timing: owner playtest.

## Course (rebuilt after owner playtest 1)
Generator: `map.CourseMap` (pure Java). Offline loop: `sh tools/render.sh` compiles the map package standalone, runs the validator (`tools/out/validation.txt`, build FAILS on any error) and renders `previews/` (course top/side/iso, zone and hub views, fork, endings, one close-up per obstacle). The old course's report under the new model is in `tools/out_old/validation_old_course.txt`.

Validator (`map.Validate`, `map.JumpSim`, `map.Phys`, `map.Bypass`): per consecutive foothold pair it simulates 1.21.1 movement (sprint/air acceleration, jump 0.42 + sprint boost, Y-first collision, the one-tick-late "coyote" jump, ladders/vines, slime with jump released, ice friction, run-up lines incl. edge and side-shifted lines, single/double neo search) and reports the timing window (good jump ticks, worst/best alignment, from rest and chained). Window 0 fails. Tier minimums (worst/best): Z1 3/4, Z2-Z3 2/3, Z4-Z5 and bronze 1/2, silver/gold 0/1, easy 3/4, medium 1/2, hard 0/1. Also: bypass flood-fill (generous easy-jump model, timed blocks present) with all / each foothold removed, decoy standables at foothold height near jump lines, kill-volume overlap with other obstacles, timed phase windows (>= 10 ticks main/bronze, >= 6 silver/gold/hard), footholds on real objects, lengths, neighbour ideas, >= 3 timed obstacles, and difficulty ordering from an attempts estimate calibrated on the owner playtest (Runaway Rails ~10 tries = gold floor): main ramp, bronze > zone main > ..., bronze < silver < gold, gold >= Runaway Rails and <= ~25, endings easy < medium < hard, hard above everything else. Current result: PASS, 0 errors, 0 warnings.

| Id | Obstacle | Tier | Technique | Hardest jump (sim) | Window worst/best (ticks) | Est. attempts |
|---|---|---|---|---|---|---|
| M1-1 | Brick by Brick | Z1 | UP | jump gap 3.0 dh +0.00 | 4/5 | 1.1 |
| M1-2 | Domino Line-Up | Z1 | THIN | jump gap 3.0 dh +0.00 | 3/4 | 1.2 |
| M1-3 | Toy Rocket Launch | Z1 | LADDER | ladder exit gap 2.0 dh +1.00 | 4/4 | 1.1 |
| M2-1 | Shop Window Panes | Z2 | THIN | jump gap 2.0 dh +1.00 curved 30deg/sw0 | 2/3 | 1.4 |
| M2-2 | Chimney Pots | Z2 | POST | jump gap 3.0 dh +0.00 | 3/4 | 1.2 |
| M2-3 | Railing Run | Z2 | WALL | jump gap 3.0 dh +0.00 | 2/3 | 1.2 |
| M3-1 | Rigging Ropes | Z3 | VINE | jump gap 3.0 dh +1.00 | 2/3 | 1.2 |
| M3-2 | Plankless Rope Bridge | Z3 | GAP | jump gap 3.0 dh +0.50 | 2/3 | 1.4 |
| M3-3 | Cargo Net Catch | Z3 | LADDER | ladder exit gap 2.0 dh +1.00 | 4/4 | 1.1 |
| M4-1 | Gear Train | Z4 | ROUND | jump gap 3.0 dh +1.00 | 1/2 | 1.7 |
| M4-2 | Tick-Tock Teeth | Z4 | TIMED | jump gap 3.0 dh +0.00 | 4/5 | 1.1 |
| M4-3 | Spring Loaded | Z4 | SLIME | slime bounce gap 2.0 dh -4.00 curved -60deg/sw14 | 3/4 | 1.1 |
| M5-1 | Geode Steps | Z5 | POST | jump gap 3.0 dh +1.00 | 1/2 | 2.0 |
| M5-2 | Lantern Line | Z5 | HANG | jump gap 3.0 dh +1.00 | 1/2 | 1.6 |
| M5-3 | Rattling Lift | Z5 | TIMED | jump gap 3.0 dh +1.00 | 1/2 | 1.7 |
| B1-1 | Alphabet Avalanche | BRONZE | DROP | jump gap 4.0 dh -1.00 | 2/3 | 1.4 |
| B1-2 | Peg Pole Spiral | SILVER | SPIRAL | jump gap 3.0 dh +1.00 | 1/2 | 2.8 |
| B1-3 | Pop Goes the Jester | GOLD | SLIME | jump gap 4.0 dh +0.00 | 1/1 | 12.4 |
| B2-1 | Fruit Crate Tumble | BRONZE | GAP | jump gap 3.0 dh +1.00 | 1/2 | 3.2 |
| B2-2 | Swinging Shop Signs | SILVER | HANG | jump gap 3.0 dh +1.00 | 1/2 | 4.3 |
| B2-3 | Billboard Bend | GOLD | NEO | jump gap 2.0 dh +0.00 curved -25deg/sw0 | 0/1 | 10.1 |
| B3-1 | Barrel Scatter | BRONZE | POST | jump gap 3.0 dh +1.00 | 1/2 | 4.0 |
| B3-2 | Gun Deck Duck | SILVER | HEAD | jump gap 2.0 dh +0.00 | 0/2 | 5.3 |
| B3-3 | Hull Rib Run | GOLD | NEO | jump gap 2.0 dh +0.00 curved 25deg/sw0 | 0/1 | 10.6 |
| B4-1 | Pendulum Hall | BRONZE | TIMED | jump gap 3.0 dh +1.00 | 1/2 | 2.8 |
| B4-2 | Axle Hop | SILVER | POST | jump gap 3.0 dh +1.00 | 1/2 | 3.9 |
| B4-3 | Cuckoo Chorus | GOLD | TIMED | jump gap 3.0 dh +1.00 | 1/2 | 9.7 |
| B5-1 | Dripstone Descent | BRONZE | DROP | jump gap 5.4 dh -3.00 | 1/2 | 3.0 |
| B5-2 | Crystal Curtain | SILVER | NEO | jump gap 3.0 dh +1.00 | 1/2 | 3.8 |
| B5-3 | Runaway Rails | GOLD | BEAM | jump gap 4.0 dh +0.00 | 1/1 | 10.1 |
| E-EASY-1 | Honeycomb Hop | EASY | UP | jump gap 1.0 dh +1.00 | 9/9 | 1.0 |
| E-EASY-2 | Bloom Stepping | EASY | GAP | jump gap 2.0 dh +0.00 | 7/9 | 1.0 |
| E-HARD-1 | Spine Scramble | HARD | UP | jump gap 4.0 dh +0.00 | 1/1 | 13.0 |
| E-HARD-2 | Missing Treads | HARD | SPIRAL | jump gap 4.0 dh +0.00 | 0/1 | 22.6 |
| E-MEDIUM-1 | Ice Floe Slide | MEDIUM | ICE | ice jump gap 4.1 dh +0.00 curved -10deg/sw8 | 3/3 | 1.2 |
| E-MEDIUM-2 | Lighthouse Ladder | MEDIUM | LADDER | jump gap 3.0 dh +1.00 | 1/2 | 2.5 |

Hub 5 order is now Dripstone Descent (bronze), Crystal Curtain (silver), Runaway Rails (gold, unchanged geometry).

### Needs an owner test (the simulation cannot settle these)
- Vines (Rigging Ropes): the sim gives very wide windows for letting go of one rope and drifting onto the next; real vine grab/release feel and the ropes' attachment faces need a check in game.
- Slime (Spring Loaded, Pop Goes the Jester): bounce heights assume jump is released on landing (holding jump cancels the bounce in 1.21.1).
- Four-block flat jumps (Runaway Rails, Pop Goes the Jester, Spine Scramble, Missing Treads) rely on exact chained-momentum timing; Missing Treads' first 4b is from rest (window 0/1).
- Timed blocks: wave direction fixed (Pendulum Hall, Cuckoo Chorus, Rattling Lift, Missing Treads were inverted, phase window 0); the windows assume the player waits on each block for the right beat. Tick-Tock Teeth, Pendulum Hall, Cuckoo Chorus timing should be felt in game.
- Neos (Billboard Bend, Hull Rib Run, Crystal Curtain): modelled as curved sprint jumps around 1-2 thick boards; real yaw control differs per player.
- Ice Floe Slide: the sim keeps full ice momentum, so it scores it only slightly harder than zone 1 (est. 1.2); it is one-wide, shifted, single-block floes with 4-block leads and may feel harder than the number says.
- Lighthouse Ladder: ladder catch on the run, exit onto the balcony, two 3+1s on the broken gallery.
- Gun Deck Duck: head-hitter jumps under a 2.5-block ceiling.

## Known limitations / untested
- Not run on a server yet; only compiled and the pure logic (graph walk, scoring) checked offline against the stub course.
- Sidebar is shared, so the "+ you" line lives in the action bar / boss bar.
- Pads are detected by feet position each tick; very fast movement through a 1-block-thick pad region is not an issue at normal speeds but the map should make pad boxes at least 2 high.
