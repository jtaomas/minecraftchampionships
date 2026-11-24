# To Get To The Other Side (McmTgttos) - v2

Game id `tgttos` (`/mcm play tgttos`). Paper 1.21.1, MinecraftMondayCore 3.2.x. Spec: `SPEC_v2.md`.
Runtime (mechanics) lives in `com.mcm.games.tgttos`; the 8 maps are pure-Java generators in
`com.mcm.games.tgttos.maps.a` / `.maps.b` behind the contract in `com.mcm.games.tgttos.model` (`TgMap`, `TgMapData`).

## How it plays
- 6 rounds, each on a different random map (no repeats while there are >= 6 playable maps; with fewer, maps repeat and a warning is logged).
- Pre-round 15 s in the start strip with the kit already in hand, held by the map's barrier blocks (plus a strip check that puts anyone outside back). Big title card with the map name, then a "3-2-1 / GO!" title. At GO the barrier (and on elytra maps the gantry floor) disappears.
- Round: 100 s. Punch any named goal chicken (visible name tag, config `goal.chicken-name`, default "Punch me!") in the goal pen to finish. Unnamed decoy chickens (per map, `decoyChickens()`) do nothing when punched. Finish: placement title, red "Whack!" text, chicken sound, firework, chat line with place + time + points. Finishers become spectators, confined to the map's play box.
- Falling below the map, leaving the play box, entering a hazard region or touching a hazard block (lava always) sends you back to a random start spawn instantly with a fresh starting kit, and a chicken pun goes to chat.
- No damage at all. Punches and snowballs only knock back. A map may turn on lethal falls (`lethalFallHeight()`, default off; Orchard Drop will use it): landing on anything but water after a fall of that many blocks sends you back to the start like a void fall, with a chat line - still no health damage. Players never collide (scoreboard team rule on every viewer's board).
- Kits by map: bridging = 64 team-colour concrete (endless: the stack tops back up to 64 after every placement, `bridging.infinite-blocks` / `bridging.refill-delay-ticks`; owner decision, replaces the old 64-block limit) + iron pickaxe + 10 snowballs (SURVIVAL); parkour/doors = 10 snowballs (ADVENTURE); boat = seated in a boat + 10 snowballs; elytra = worn elytra + 2 fireworks; at GO the barrier opens but the gantry floor stays `elytra.floor-drop-delay-seconds` (default 3) so racers can punch each other off; knockback on a gliding player is added to their flight velocity (`knockback.glide-multiplier`). You can place only concrete, only on bridging maps, only into air inside the map's build box; you can break only player-placed concrete (anyone's), with no drops.
- Karma: punching raises your karma (more for the same victim within 5 s, none for punching a red player). Above 3 you take more knockback; at 6 you glow red and get Slowness I; at 9 you cannot punch for 4 s. Decays 0.35/s. Players who get hit a lot take up to 45% less knockback. Karma state is shown in the action bar.
- Round ends when every online racer has finished or at 0:00. When 80% have finished the timer drops to 20 s. Placed blocks, boats, chickens and projectiles are removed between rounds.
- After round 6: top-5 list in chat, each player's own round places, a 3-2-1 side placement reveal, then `finish` (exactly once).

## Scoring (researched shape, scaled /5; config `points`)
- Finishing place: 8 points for places 1-5, 7 for 6-10, ... minimum 1. Top-10 bonus 16, 15, ... 7. Non-finishers 0. So 1st = 24 per round.
- Team bonus: the first 4 sides with 2+ online members who have all finished get 20 / 15 / 10 / 5, paid with `addSidePoints` (split evenly).
- A perfect game is about 144 + team bonus.

## HUD
- Boss bar timer (`ctx.setTimer`): pre-round countdown, then the round timer.
- Sidebar (global, core board): round n/6, map name, finished n/N, phase (starting in / first finisher / time skip), top player.
- Action bar (per player): karma state and your placement per round so far (the core sidebar is shared by everyone, so per-player placements live here).

## Defaults, not researched (all in `config.yml`)
- Start barrier line that drops at GO; strip check during pre-round.
- Respawn refills the starting kit; lava (and any map hazard block/region) = respawn; respawn point = random start spawn.
- Finished players: spectator mode confined to the map (instead of the real finish pen with a spectate item).
- Time skip 80% -> 20 s (MCC Island rule).
- Karma values: +1 per punch, +0.5 same victim within 5 s, decay 0.35/s, more knockback from 3 (+8%/point, max x1.8), red glow + Slowness I from 6, punch lock 4 s from 9; hit protection 7% per hit up to 45%, decays.
- Knockback: punch 0.42 horizontal / 0.36 up, +0.35 when sprinting with a charged hit, weaker for uncharged hits; snowball 0.55 / 0.30 in the snowball's travel direction (`knockback.snowball` / `knockback.snowball-vertical`, no per-victim cooldown, same gliding handling as punches; a racer in a boat gets the boat pushed); 10-tick per-victim cooldown for punches. Snowball hits give no karma (`karma.snowball-hits-count: false`).
- Iron pickaxe, no enchantments, unbreakable; 10 snowballs per attempt (owner, playtest 1; `kit.snowballs`), they don't regenerate but every respawn refills the kit; 2 fireworks on elytra maps.
- 10 chickens per pen, AI on but teleported back if they leave the pen; invulnerable, no collision, no eggs. Decoys: no AI, no gravity, put back every second if moved.
- Endless concrete refill after 1 tick; elytra floor held 3 s after GO; gliding knockback x1.6.
- 6 s intermission between rounds; maps use fixed seeds (`random-seeds: false`), turnstile/variant outcomes are re-rolled every round.
- Punches, snowballs and fireworks are disabled before GO.

## Admin
- `/tg list` - maps with type, kit, slot and validation state. `/tg info <id>` - counts, hazards, lethal fall, decoys, a sample variant roll, notes (plus the live block below if it is the current map).
- `/tg info` (no id) or `/tg chickens` - live check: current map and phase, goal chickens alive / in pen with positions, decoys, and this round's accepted / rejected chicken punches. Every chicken punch is also logged (`goal.log-punches`) with the reason it was accepted or rejected (decoy, phase, already finished, spectator, ...). Solo test: start a game alone, punch a goal chicken after GO -> log says ACCEPTED, you finish 1st and the round ends.
- `/tg map <id>` - force that map as the next round (or the first round of the next game).
- `/tg tp <id>` / `/tg build <id>` - paste a map (no game running) and teleport there in creative. `/tg reload` - reload config and maps.
- Admin testing hooks (core 3.2.x, declared without `@Override`, fast-test read reflectively so older cores still work):
  `skipRound()` (`/mcm skip`: ends the round now, takes back that round's points, next map or final results),
  `skipWait()` (`/mcm skipwait`: ends the pre-round, intermission or results now), `maps()` / `forceNextMap(id)` (`/mcm map`, same as `/tg map`).
  Fast-test mode (`/mcm fast on`): pre-round 1 s, no slow title card, 0.5 s intermission, no placement reveal. Round length is not shortened.

## Maps and world
- World `mcc_tgttos` (void) is deleted and recreated at every plugin enable. Map slot i is pasted at x = i * 600, local y 0 = world y 64.
- Maps are loaded from `config.yml -> maps:` (fully-qualified class names; missing classes are skipped with a warning) plus auto-discovery of any `TgMap` class under `com.mcm.games.tgttos.maps.*` in the jar. `stub.StubBridgeMap` is a dev stub used only when fewer than 6 real maps load (`stub-map: auto`).
- Each map is built once, off the main thread (one worker thread, pure Java), when the plugin enables or `/tg reload` runs, and checked (>= 40 spawns, >= 8 chickens, pen/strip/bounds present, no dyed concrete). A game started before that finishes waits for it. Pasting: see "Map loading and pasting" below.
- Map lines, time estimates and route checks are documented by the map builders (see the map packages / previews).

## Acceptance checklist mapping
1. 6 rounds, different maps, sidebar round n/6 + map - `TgttosGame.start/prepareRound/sidebar`.
2. 100 s round, 15 s pre-round, held in strip, kit in hand - `beginPre`, `onTick` strip check, barrier blocks, `go`.
3. Kits by type - `giveKit` (+ boat seat in `placeAtStart`).
4. Only player-placed concrete breakable; no placing on non-bridging maps - `canPlace/canBreak` + `GameListener.onPlace/onBreak`.
5. Knockback-only, no collision, no fall/fire/lava death - `GameListener.onDamage`, `punch/snowballHit/knock`, `ScoreTeams`, world gamerules.
6. Void return within 1 s with fresh kit + pun - `onTick` (every tick) -> `respawn`, `Puns`.
7. Chicken finish once, effects, spectator; chickens invulnerable, kept in pen - `GameListener.onAttack` (PrePlayerAttackEntityEvent) -> `chickenPunched` -> `whack`, `spawnChickens`, `leashChickens`.
8. Scoring table - `Settings.placementPoints/top10Bonus`, `checkTeamBonus`.
9. Karma - `punch`, `karmaSecond`, `takenMult`.
10. Round end, 80% skip, blocks cleared - `checkRoundEnd`, `onSecond`, `cleanupRound`.
11. `finish` once, `/mcm stop` cleanup - `endGame` guard, `stop()` (tasks, queue, boats, chickens, concrete, glow, elytra).
12. Solo play, quitters - `onQuit` re-checks round end/team bonus; time skip needs 2+ players.
13. Route checks / crossing times - map builders.
14. Turnstiles re-rolled each round, >= 4 of 12 open - `Variants.pick` (map rule, plus a built-in fallback of 4 OPEN for category `turnstile`).

## Known limitations
- Not tested on a live server yet (built only). Boat knockback is applied to the boat and may be ignored client-side while a player drives it. Boats still collide with each other (vanilla). Per-player round placements are in the action bar, not the shared sidebar.

## Fixes after playtest 1 (mechanics)
- Goal chickens could never be punched: they are spawned invulnerable, and the server discards an attack on an invulnerable entity before any `EntityDamageEvent` is fired, so the finish handler in `onDamage` never ran. The finish is now detected on Paper's `PrePlayerAttackEntityEvent` (fired before the invulnerability check), cancelled so the chicken takes no hit.

- Snowballs never moved anyone: players were set `setCollidable(false)` for no-collision, which on CraftBukkit/Paper also makes a living entity un-pickable, so the server let snowballs fly straight through players (no hit, no damage event, `knock()` never reached). Racers now stay collidable (collision is still off through the scoreboard team rule). Snowball hits are handled on `ProjectileHitEvent`, on the damage event, and by the game's own per-tick sweep of racer snowballs against racer hitboxes (whichever sees it first, once per snowball), and every hit is logged as ACCEPTED or REJECTED with the reason (`knockback.log-snowball-hits`). Rejected: before GO, own snowball, thrower or victim finished / spectator / not a participant.

## Map contract additions (for map generators)
Both are `default` methods on `com.mcm.games.tgttos.model.TgMap`; override them in the map class. Existing maps compile unchanged.
- `int lethalFallHeight()` - default `0` = off. `> 0`: a racer who lands on a non-water block after falling at least this many blocks (highest point since last standing / swimming / climbing / gliding / riding, minus landing height) is sent back to a start spawn with a fresh kit and a chat line. No damage.
- `java.util.List<int[]> decoyChickens()` - default `List.of()`. Each entry `{x, y, z}` is the local feet block of one decoy (same local integer convention as `TgMapData.pen`); it stands at `(x + 0.5, y, z + 0.5)`. Decoys have no name tag, no AI, no gravity, are unkillable and put back if moved; punching one does nothing (`goal.decoy-penalty-seconds` reserved). Must be inside `playBounds` (validated). Real goal chickens (from `TgMapData.chickenSpawns`) always carry the visible name tag.
## Orchard Drop (after playtest 1)
Drop map, snowballs only, **lethal falls: `lethalFallHeight()` = 8** (only map with it). You start on a windmill gallery (feet y 60) and drop in three stages through a giant apple tree. Every landing is a pool of water 2 deep in a giant cut apple, or a pond in the meadow at the bottom. A fall of 8+ blocks onto anything else (apple rim, branch, canopy, meadow) sends you back to the gallery. Intended drops are 15-26 blocks; ordinary hops stay under 2. The limit of 8 also catches near misses onto the rim, which is 1 block above the water.
- **SAFE (left):** big apples with 5-wide pools (tops y 40 and 22), each a 7-8 block jump-out. Then the big pond, the left hedge gaps, a 5-wide plank bridge far left, up the west side of the cider yard and round to the pen. Estimate: expert 32.4 s, median 42.2 s.
- **FAST (right):** small apples with 3-wide pools, lower and further out (tops y 34 and 14). Each drop must be steered diagonally (gaps 8-10). Then the small pond, the right/middle hedge gaps, a 3-wide fallen branch over the void (choke, with a knot and twigs), and a 4-wide ledge along the yard's void edge with staggered barrels (2-wide squeezes). Estimate: expert 25.7 s, median 33.1 s (21% faster).
- **Confusion at the bottom:** two hedge rows with three offset gaps each, a dead-end pocket and a loop. Three ways across the void: the branch, the plank bridge, and a broken bridge (dead end, 6 blocks of void). A barrel wall blocks the direct way into the yard. Two decoy pens hold 9 decoy chickens with no name tag: one in the inner yard, one in a dead-end pocket. The real pen sits under the cider tower with the giant apple (landmark). Lantern posts and bunting mark the real way. Two dry apples (no water) sit where careless jumpers aim. The middle canopy hides the lower stage from the gallery.
- Validators (`tools/a`, extra checks in `maps/a/util/ExtraChecks.java`):
  - every lethal-height drop on both lines lands in water at least 2 deep
  - every dry surface within 5 blocks of a landing is a lethal fall
  - all water is contained
  - a "dry descent" search finds no way down from the gallery or any pool without a water landing
  - decoys stand on solid ground outside the pen

## Bookshelf Run (after playtest 1)
Parkour over the void, snowballs only, no lethal falls. Mouse-sized racers cross a giant library wall: short wall-shelf boards with gaps between them, chained books hanging from brass arms out over the room, and two wide brawl spots next to the drop. The goal is the teacup on the desk, under the desk lamp (landmark). The map is about 50% longer than before (start z 0, cup z 137).
- **FAST (outer):**
  - three chained books (gaps 3 / 3+1 / 4-1, the last only 3 wide)
  - the open atlas, then a 3-wide pencil (choke) and the dictionary stack
  - two chained notebooks (gaps 4), then the dictionary
  - two coin stacks down, then a 2-wide letter opener onto the cup rim

  Void crossings: 10, widest 4. 48% of walked cells have a void drop beside them. Estimate: expert 26.6 s, median 37.0 s.
- **SAFE (against the wall):**
  - boards with 2-gaps, a 3-wide ruler bridge, standing books that make you weave front/back
  - the open atlas, then a 3-wide ruler stair down
  - low boards, a 2-gap, the 2-wide bookmark ribbon, a 2-gap, a stair up to the dictionary
  - a stair down to a pulled-out drawer (or drop 12 onto it), round the saucer, up the spoon

  Void crossings: 6, gaps 2, plus the 12-block drop to the drawer. 36% of walked cells have a void drop beside them. Estimate: expert 33.7 s, median 44.2 s (fast 21% faster).
- **Brawl spots:** the open atlas (26 x 13, 320 cells, 50 on a void edge) before the pencil, and the dictionary on a book tower (24 x 12, 288 cells, 53 on a void edge) before the final descent.
- Jump limits are well inside the game's movement: safe flat gaps <= 3 (actually 2), fast <= 4 with run-up (1.21, no effects). The validator reports every void crossing per line.

## Kite Festival (rebuild after playtest 1)
Elytra map (`maps/b/KiteFestival.java` + `KiteCourse.java`, same class name / slot / start-strip / pen conventions). The old map took about 10 s and had no contact. Now it is a long folded canyon: 4 straight legs joined by 3 hairpins (radius 55, interior 37 wide), about **1,430 blocks of centre line**. The safe line flies about 1,540 blocks and the fast line about 1,330. You start on a gantry at y 173 and descend to a landing meadow at y -45. Kite-banner walls (striped canvas, bunting, lights) carry the gates. Colour code: **lime = safe 9-11**, **orange = fast 5-7**, **red = needle 4x4 / 5x5**. Every opening has a black frame and a froglight (or sea-lantern) ring.

**Segments:**
1. Launch Gantry (the floor drops 3 s after GO)
2. Drop Gate Run (three 11s; the third adds a fast 7)
3. Canyon Slalom
4. Turn 1
5. Tunnel Split (11x11 tube vs 5x5 tube with bends)
6. Lift Hill (the fast 6s climb, the safe 9s weave below)
7. Ring Stack (low 11 / mid 7 / high 5)
8. Tether Forest (mast rows with full-height slots)
9. Turn 2
10. Dive & Pull-up
11. Needle (three 4x4 in a straight line; safe detours through 9s)
12. S-Canyon
13. Turn 3
14. Finish & Field (15x15 finish frame, then a 2-high hedge choke with a 3-wide gap, then the pen)

**Punching spots:**
- the gantry
- 5 wall ledges (ladder towers from the meadow with open 5x5 platforms; three are mid-hairpin)
- the landing field, with the choke and 4 decoy chickens on hay bales

**Rockets:** 2 from the kit plus 14 from the map (`TgMapData.extraItems`) = 16. There are no refills, because the runtime has no ring, updraft or checkpoint hook. The chat kit line still says 2. Landing on the meadow is not a fail: relaunch with a rocket or climb a ledge. Leaving the canyon (76 hazard boxes outside the walls) sends you back to the start.

**Validation:** `sh tools/b/kite_render.sh` writes `tools/b/out/kite_validation.txt` and the previews `previews/kite_festival_{top,iso,iso2,side,start}.png`.

The glide model is the vanilla elytra tick. Steady glide: 0° gives 1:10.1 at 30 b/s; 20° gives 1:8.8 at 40 b/s; 40° gives 1:5.7 at 61 b/s. A rocket holds about 34 b/s. Turn scrub per tick is sqrt(0.82+0.18cos(lead)), so a hairpin taken at speed costs 25-40% of it. A pilot controller flies each line through the real blocks.

The validator checks:
- the gates and the 3D flood fill seal the canyon
- both tubes are clear and closed
- the landmark can be seen from the start
- every ledge relaunches onto the safe line (at most 3 rockets)
- the hazards cover everything outside the walls

Results:

| Line | Gates passed | Time | Rockets used |
|---|---|---|---|
| Safe line, 2 boosts per hairpin + rescue rockets | 27/27 | about 61 s | 6 |
| Fast line, expert (a rocket whenever under 28 b/s) | 29/29 | about 43 s | 7 |

- The fast line is 30% faster than the safe line.
- Median estimate: about 85 s. Weak players: about 115 s, so they DNF at 100 s.
- Without rockets, the safe line runs short of height in the S-canyon, and the fast line is not flyable. The course is built for rockets.
- Block count: about 341k blocks. Generation takes about 0.2-0.5 s, and pasting is spread over ticks by the runtime paster (15 ms of block writes per tick, about 2-4 s of wall time, normally done during the previous round).

**Defaults, not researched:**
- rocket count (16)
- ledge heights
- choke width
- glide-pilot parameters (gain 40, step 4, leak 0.1)
- the median and weak multipliers (x1.2 / x1.35 plus landings)

## Frozen Canal (lengthened after playtest 1)
Boat map (`maps/b/FrozenCanal.java` + `CanalCourse.java`, same class name / slot / start lock / pen conventions). One continuous point-to-point boat run through a snowy canal town, folded inside the slot, then an on-foot scramble from the dock to the pen on the island in the frozen mere (Christmas-tree landmark). Track centre line **1,117 blocks** (safe), lanes 11-15 wide (93% of the main line >= 11), ice falls y 0 -> -6 in 1-block step-downs only, never up. Brick walls with stone copings contain the boats; nothing non-full at boat height.

**Segments:** 1 Grid Straight (88) - 2 Mill Hairpin - 3 Weavers' Chicane - 4 Cellar Hairpin - 5 Cellar Steps (four 1-block step-downs) - 6 Fish Market split (walled ring vs the narrow raised blue-ice causeway, 5 wide, open edges) - 7 Brewery Bend - 8 Blue Mile (168 straight, blue-ice stripe, three arch bridges) - 9 Bell Switchbacks (two hairpins under the bell footbridge) - 10 Tannery Sweeper - 11 Lantern Bend - 12 Barge Basin (moored barges, 3-wide fast gaps) + finish straight to the dock. Low footbridges at the grid (headroom 2) and the cellar (4); dolphins (mooring posts) on the chicane.

**Lines** (boat model from the vanilla ice speeds: packed ice ~40 b/s top, blue ice ~73, snow/land ~2; turn caps by radius):

| Line | Boat | On foot | Expert total |
|---|---|---|---|
| SAFE: middle of the lane, Fish Market ring, wide barge gaps, quay side flight + east bridge | 1,103 blocks, 47.0 s | 61 blocks, 10.3 s | 58 s |
| FAST: inside kerbs, causeway, blue stripe, 3-wide barge gaps, Dock Stairs + 3-wide jetty (brawl) | 1,061 blocks, 40.8 s | 41 blocks, 7.1 s | 49 s |

At the researched expert pace (~30 b/s average on packed ice) the fast line is ~36 s boat + 8 s = **~44 s**; the conservative model above says 49 s. Median: ~80-85 s by safe length / 19 b/s + crowd; the validator's formula (x1.5 boat + 8 s) says ~90 s. Knocked off the jetty onto the mere: back to the pen in 5.5 s.

**Mechanics support missing** (so no mid-race boat change): map-declared boat depots that hand an on-foot racer a fresh boat of their own, a Le Mans start with boats to claim, and checkpoints / per-segment respawn (a fall means a full restart, so the map has no lethal holes). Racers can leave their boat at any time but can only re-enter their own.

**Validation:** `sh tools/b/canal_quarry_render.sh frozen_canal` (report `tools/b/out/canal_quarry_validation.txt`, previews `previews/frozen_canal_{top,iso,iso2,side,start}.png`). Checks: boat continuity start lock -> dock (downhill only), no trap pockets, closed walls, no snags, drops <= 1, lane width, bridge headroom, no on-foot escape into the town before the finish, both foot routes, the start lock holds 40 boats, plus the common contract / start / post-pass / staircase / landmark checks. About 134k blocks, generated in ~0.4-0.6 s.

**Defaults, not researched:** turn-cap model and the x1.5 median factor, barge and dolphin spacing, 2-block headroom under the grid footbridges.
**Unverified (needs a live test):** seated riders under the 2-headroom footbridges, 1-block step-downs at full speed, boats at the causeway's open edges, 40-boat pile-ups at the hairpins, chunk loading at blue-ice speed.

## Quarry Steps (rebuilt after playtest 1)
Push + bridge map (`maps/b/QuarrySteps.java`, same class name / slot / start strip / pen conventions). Bigger: the bowl is 55 x 52 (was 39 x 38), and the yard on top runs to z 130.
- **SAFE (haul road):** down the left wall over three benches (a collapsed 3-block gap, a boulder squeeze), along the far wall 15 below the rim (a 3-block gap, fallen blocks to weave round, a 2-wide ledge with two 1-wide pinches), then up the 3-wide **cut** staircase (choke) or the wide double **ramp**. Cut: 222 blocks, expert 37.0 s; ramp: 230 blocks, 38.7 s.
- **FAST (bridge):** ~53 blocks straight across the open bowl and 5 up the far face, bending round five hanging sheet piles and a rock spire. Blocks regenerate, so the bridge is kept risky by exposure instead: 45 of the 53 blocks hang over open water 21 below, with no cover, in reach of snowballs from the apron, the haul road and the rim, and any rival can break it. Expert (4 b/s) 23.6 s (36% faster than the road); an average bridger (2.2 b/s) needs 34.5 s, about the same as the road. A mixed line (bridge to the ramp pad, then the ramp) takes 30.6 s.
- **Cluttered top:** block stacks, crate piles and skips everywhere (no straight free run > 13), ten portacabins with open doors, two scaffolds with a ladder to a dead-end deck, a raised loading dock (dead end), a parked excavator, a dump truck, a generator and a drill rig. There are **four fake pens built exactly like the real one** (striped ring, hay, floodlights), and the tower-crane hook hangs over the first fake, right at the top of the bridge. The real pen is behind the cabin row at the back (x -7..6, z 100..113).
- **Decoys:** `decoyChickens()` returns 9 unnamed decoys, two in each fake pen and one on a scaffold deck. All are reachable, inside playBounds and at least 17 blocks from the real pen. The real goal chickens carry the runtime's "Punch me!" name tag (`goal.chicken-name`).
- Median estimate: road ~67 s, bridge ~53 s (x1.6 / x1.35 plus crowd and pen search). About 159k blocks.
- **Validation:** `sh tools/b/canal_quarry_render.sh quarry_steps` (`tools/b/QuarryV.java`) checks:
  - both road exits
  - bridge expert and average times, the least-blocks need (>= 50), and exposure
  - the mixed line
  - the cut is 3 wide over its whole length, and the ledge width
  - decoys are reachable and away from the pen
  - the expert bridge saves >= 20%
  - the common checks

  Previews: `previews/quarry_steps_{top,iso,iso2,side,start}.png` (the top view marks the decoys in white).
- **Defaults, not researched:** bridging rates (4 / 2.2 b/s), sheet-pile spacing, the median multipliers, and the decoy count.
## Map loading and pasting (performance)
- Nothing heavy runs inside one tick. Map generation (enable, `/tg reload`, `random-seeds: true` rounds) runs on a worker thread. A paste is also prepared on the worker: stale blocks from the last paste at that slot are cleared, then the map blocks are set, then the chosen variant options, all grouped per chunk.
- On the main thread (`Paster`), chunks are loaded with `getChunkAtAsync` (12 in flight). Each chunk gets a plugin chunk ticket and is written only once it is loaded, so there is never a synchronous chunk load or generation. Block writes stop after 15 ms per tick and use no physics. Tickets stay on the slot being played and are dropped when the next round starts.
- Pre-build: at GO the next round's map is pasted into its own slot while the current round is played, so the next round normally starts with no wait. If the next map uses the same slot (maps repeat), or an admin forces another map, it is pasted during the intermission instead. The round waits until every block is set and never starts on a half-built map. Players stay where they are and see "Preparing the next map... NN%" in the action bar.
- `stop()` or `/mcm skip` in the middle of a paste cancels it cleanly. The next paste of that slot clears anything a cancelled paste left behind.
- `debug-timing: true` logs ms, blocks and chunks per paste tick, plus a summary per paste (blocks, chunks, ticks, wall ms, worst tick).

## Spectators and late joiners
- Finishers become spectators where they finish (at the goal pen) and are kept inside the map's play box. Teleports from the spectator menu are cancelled if they would leave the current map's play box (other slots included).
- `onLateJoin`:
  - A participant who has not finished this round rejoins the race from a start spawn with a fresh kit, during the pre-round or the round.
  - A participant who has already finished this round, or who joins during the intermission, spectates the current map. While the next map is still loading, the core default applies; the next round then places every participant at the start.
  - Non-participants become spectators at the map's overview point and are confined to its play box. They are also kept out of other slots.
- Round announcement: the kit text is counted from a racer's real inventory after the kit is given (base kit plus the map's `extraItems`), e.g. "Elytra, 18 fireworks" on Kite Festival.
