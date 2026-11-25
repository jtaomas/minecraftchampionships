# To Get To The Other Side (McmTgttos) - v2

Game id `tgttos` (`/mcm play tgttos`). Paper 1.21.1, MinecraftMondayCore 3.2.x. Spec: `SPEC_v2.md`.
Runtime (mechanics) lives in `com.mcm.games.tgttos`; the 8 maps are pure-Java generators in
`com.mcm.games.tgttos.maps.a` / `.maps.b` behind the contract in `com.mcm.games.tgttos.model` (`TgMap`, `TgMapData`).

## How it plays
- 5 rounds (`rounds`), each on a different random map: Laundry Line, Dam Face, Bookshelf Run, Ticket Gates, Quarry Steps (no repeats while there are enough playable maps; with fewer, maps repeat and a warning is logged). Elytra and boat maps (Kite Festival, Frozen Canal, imported ones) moved to the Race plugin; Orchard Drop was deleted (owner, feedback 3).
- Pre-round 15 s in the start strip with the kit already in hand, held by the map's barrier blocks (plus a strip check that puts anyone outside back). Big title card with the map name, then a "3-2-1 / GO!" title. At GO the barrier disappears.
- Round: 300 s (config `round-seconds`, owner change after playtest 2; was 100 s), cut short by the time skip once half the racers are in. Punch any goal chicken (visible name tag, config `goal.chicken-name`, default "Punch me!") in the finish row to finish. Every map ends in a row of goal chickens across the full width of the finish line, one every 2 blocks, on hay in front of a black / white chequered board under a striped arch with a FINISH sign (`model.FinishRow`). No decoys (`goal.decoys: false`, code kept). Finish: placement title, red "Whack!" text, chicken sound, firework, chat line with place + time + points. Finishers become spectators, confined to the map's play box.
- Falling below the map, leaving the play box, entering a hazard region or touching a hazard block (lava always) sends you back to a random start spawn instantly with a fresh starting kit, and a chicken pun goes to chat.
- No damage at all. Punches and snowballs only knock back. A map may turn on lethal falls (`lethalFallHeight()`, default off; no TGTTOS map uses it since Orchard Drop was deleted): landing on anything but water after a fall of that many blocks sends you back to the start like a void fall, with a chat line - still no health damage. Players never collide (scoreboard team rule on every viewer's board).
- Kits by map: bridging = 64 team-colour concrete (endless: the stack tops back up to 64 after every placement, `bridging.infinite-blocks` / `bridging.refill-delay-ticks`; owner decision, replaces the old 64-block limit) + iron pickaxe + 25 snowballs (SURVIVAL); parkour/doors/push = 25 snowballs (ADVENTURE). (The boat and elytra kits are still in the code but no TGTTOS map uses them.) You can place only concrete, only on bridging maps, only into air inside the map's build box; you can break only player-placed concrete (anyone's), with no drops.
- Karma: **off by default (owner decision after playtest 2: karma off; `karma.enabled: false`)**. Off means no karma gain, no extra knockback, no glow or Slowness, no punch lock and no hit protection; the HUD hides it. The code is kept; with `karma.enabled: true`: punching raises your karma (more for the same victim within 5 s, none for punching a red player). Above 3 you take more knockback; at 6 you glow red and get Slowness I; at 9 you cannot punch for 4 s. Decays 0.35/s. Players who get hit a lot take up to 45% less knockback. Karma state is shown in the action bar.
- Round ends when every online racer has finished or at 0:00. When half have finished (`time-skip.finished-fraction: 0.5`) the timer drops to 20 s. Placed blocks, boats, chickens and projectiles are removed between rounds.
- After the last round: top-5 list in chat, each player's own round places, a 3-2-1 side placement reveal, then `finish` (exactly once).

## Scoring (real MCC coins, TGTTOSAWAF; config `coins.*`, see `../COINS.md`)
- Finishing place: 40 coins for 1st, 1 less per place (40, 39, 38, ...). Top-10 bonus 80, 75, ... 35. Non-finishers 0. So 1st = 120 per round.
- Team bonus: the first 4 sides with 2+ online members who have all finished get the team pools 100 / 75 / 50 / 25; each member gets the MCC share (pool / 4).
- Line bonus (ours, gate maps): medium 5, hard 15 per section, cap 75.
- The core applies the event multiplier to every award.

## HUD
- Boss bar timer (`ctx.setTimer`): pre-round countdown, then the round timer.
- Sidebar (global, core board): round n/6, map name, finished n/N, phase (starting in / first finisher / time skip), top player.
- Action bar (per player): karma state and your placement per round so far (the core sidebar is shared by everyone, so per-player placements live here).

## Defaults, not researched (all in `config.yml`)
- Start barrier line that drops at GO; strip check during pre-round.
- Respawn refills the starting kit; lava (and any map hazard block/region) = respawn; respawn point = random start spawn.
- Finished players: spectator mode confined to the map (instead of the real finish pen with a spectate item).
- Time skip: once half of the racers (config `time-skip.finished-fraction: 0.5`, was 0.8; rounded up, 2+ players) have finished, the clock drops to 20 s (`seconds-left`) if more than that is left; broadcast + sound + sidebar "Time skip!". With 300 s rounds this, not the clock, normally ends a round.
- Karma values: +1 per punch, +0.5 same victim within 5 s, decay 0.35/s, more knockback from 3 (+8%/point, max x1.8), red glow + Slowness I from 6, punch lock 4 s from 9; hit protection 7% per hit up to 45%, decays.
- Knockback: punch 0.42 horizontal / 0.36 up, +0.35 when sprinting with a charged hit, weaker for uncharged hits; snowball **1.375 / 0.45** (owner-set after playtest 2: 2.5x the old 0.55 horizontal; not researched, no clamp) in the snowball's travel direction (`knockback.snowball` / `knockback.snowball-vertical`, no per-victim cooldown, same gliding handling as punches; a racer in a boat gets the boat pushed); 10-tick per-victim cooldown for punches. Snowball hits give no karma (`karma.snowball-hits-count: false`).
- Iron pickaxe, no enchantments, unbreakable; **25 snowballs per attempt (owner-set after playtest 2; `kit.snowballs`)**, they don't regenerate but every respawn refills the kit.
- One goal chicken on every spot of the finish row (at least `chickens.count`), AI off (`chickens.wander: false`), sent back to their own spot if pushed; invulnerable, no collision, no eggs. Goal chickens and decoys are persistent and their chunks are held by plugin tickets for the round (see the chicken bug below). Decoys: AI and gravity on, leashed 4 blocks round their spot (`goal.decoy-leash-radius`).
- Endless concrete refill after 1 tick.
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
- Maps are loaded from `config.yml -> maps:` (fully-qualified class names; missing classes are skipped with a warning) plus auto-discovery of any `TgMap` class under `com.mcm.games.tgttos.maps.*` in the jar. `stub.StubBridgeMap` is a dev stub used only when fewer real maps than rounds load (`stub-map: auto`). Imported community maps are no longer loaded by TGTTOS (Race takes elytra / boat imports; droppers go to the core Multi-Stage Dropper).
- Each map is built once, off the main thread (one worker thread, pure Java), when the plugin enables or `/tg reload` runs, and checked (>= 40 spawns, >= 8 chickens, pen/strip/bounds present, no dyed concrete). A game started before that finishes waits for it. Pasting: see "Map loading and pasting" below.
- Map lines, time estimates and route checks are documented by the map builders (see the map packages / previews).

## Acceptance checklist mapping
1. 6 rounds, different maps, sidebar round n/6 + map - `TgttosGame.start/prepareRound/sidebar`.
2. 300 s round (time skip at half finished), 15 s pre-round, held in strip, kit in hand - `beginPre`, `onTick` strip check, barrier blocks, `go`.
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
- `java.util.List<int[]> decoyChickens()` - default `List.of()`. Each entry `{x, y, z}` is the local feet block of one decoy (same local integer convention as `TgMapData.pen`); it stands at `(x + 0.5, y, z + 0.5)`. Since playtest 2 an empty list (now the norm) means the runtime scatters 6 wandering decoys with `model.DecoySpots.pick` (walkable, never ice / glass / roofs, outside hazards, >= 4 from the pen, on the later part of the map); validators use `DecoySpots.effective` so they check the same spots. Decoys wear the goal name tag, wander (leashed), are unkillable; punching one says `goal.decoy-message` ("haha lol"). Must be inside `playBounds` (validated). Real goal chickens (from `TgMapData.chickenSpawns`) always carry the visible name tag.
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
## Changes after playtest 2
### Missing goal chickens on Kite Festival (bug)
Cause: chickens were spawned non-persistent (`setPersistent(false)`, HEAD `TgttosGame.java:727` goal, `:749` decoys) and the map was pasted with the core `McmBuild.BuildQueue` (HEAD `TgttosGame.java:266`), which holds no chunk tickets. The Kite Festival pen (local 160..171, -44, 72..83) is ~350 blocks from the gantry, beyond view distance 8, so its chunks unloaded about 10 s after the paste (`delay-chunk-unloads-by`) and the server discarded the non-persistent chickens. The other maps' pens are inside view distance of the start (Frozen Canal's ~115 blocks, borderline). Fix: `spawnChickens` holds the 3x3 chunks round every chicken spot with a plugin ticket for the round (`Paster.hold`), chickens are persistent and never removed when far away, `checkChickens()` re-spawns any invalid chicken every second with a log warning, leftovers from an unload are removed on `EntitiesLoadEvent`, and `/tg info` lists every chicken (valid, ticking, chunk loaded / load level / ticket, position, in pen). Tickets are released after cleanup at round end / stop.

### Decoy chickens
The fake coops are gone (Quarry Steps: plain sandstone tiles; Orchard Drop: D1 is a solid apple-crate heap, D2 a barrel-store ring, which keeps its validator's route checks). Decoys now wander on the later part of the map (see the map contract), wear "Punch me!", and answer a punch with "haha lol" plus a sound (rate-limited).

### Interactive map features (`model.MapFeatures`, `TgMap.features()`, runtime `FeatureRuntime`)
- **Movers**: pendulum / rotor / slider made of BlockDisplays (2-tick interpolation); hit test = sphere vs the hazard boxes in the mover frame, sampled along the racer's path each tick; a hit sends the racer back to the last checkpoint ("was flattened by the ..."). Solid movers also place barrier blocks in their current cells. Telegraphs: pendulum whoosh at the bottom of the swing, rotor sound every quarter turn, slider glows red 1 s before it closes and plays a door sound (sounds only within 28 blocks of a racer).
- **Toggles**: timed block frames (placed without physics; solid blocks wait for an empty cell); indicator blocks turn to a warn block before the dangerous frame.
- **Traps**: a block that gives way under a racer (creak, delay, drop, restored once nobody stands there).
- **Checkpoints** (crossing the trigger saves it; void / hazard / mover respawns go there with a fresh kit), **refills** (elytra: +rockets, once per opening per attempt), **depots** (boat maps: a racer on foot gets a fresh boat).
- **Bonus regions**: tier 1 orange / 2 red / 3 brutal; the best tier per group pays once, only on finishing, capped (`bonus.enabled`, `bonus.tier-points [1,2,3]`, `bonus.cap 8`; 1st place is 24, so a perfect hard line is worth a third of a win). Shown in the title, results and chat ("+N line").
- All features stop and restore their blocks at round end. `/tg info` shows feature status.

### Ticket Gates
Lengthened (owner: expert 25-35 s): a third live track (T3 = 70), a queue hall of single-file ticket barriers, plaza and clock tower moved 56 along; expert 25.1 s, safe 33.6 s (`TicketV` checks the window). The three coupling anvils are traps: 0.4 s after a racer lands on one it creaks, then drops with its iron-bar stand into the live pit (hazard = respawn); it is back 3 s later once the cell is empty. The fast line over the couplings now needs a quick hop.

### Dam Face hole
Void holes in the grating, at the toe walkway ends, the terrace-end slits, the crest edges and the reservoir sides were sealed (`sealSides`, grating fix); a new `ExtraChecks.holes` validator finds unintended void columns. The apron back edge behind the start is void by design.

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


## Changes after feedback 3 (owner)
- **Split:** Kite Festival, Frozen Canal, the imported-map loader, the elytra crash rule and the boat handling now live in the Race plugin (`game-plugins/Race`, see its README). Orchard Drop is deleted. TGTTOS is jumping, building and snowball fights only.
- **Round timing:** `round-seconds: 300` (was 100), `time-skip.finished-fraction: 0.5` (was 0.8). The time skip (half finished -> 20 s left) is what normally ends a round now; `TgttosGame.onTick` checks it every second (2+ racers, rounded up, only if more than 20 s are left).
- **Finish rows:** every map ends at a full-width row of goal chickens (`model.FinishRow.build`): Laundry Line roof C z 78 (16 chickens), Dam Face crest z 55 (16), Bookshelf Run back of the desk z 159 (14; the teacup stays as a landmark in the way), Ticket Gates plaza z 138 wall to wall (20), Quarry Steps far yard z 105 (18) with an open apron in front. Validators (`FinishRow.check`, every map): >= 8 chickens in one straight row, spacing 1.5-2.5, a standing cell in front of every chicken, >= 75% in sight from the approach (8-16 blocks out, eye height).
- **Decoys gone:** `goal.decoys: false`; no fake pens (Quarry Steps' former pens are plain tiles).
- **Dam Face holes (playtester, world 582,65,15 and 603,87,35):** (1) the play box ended at x -19 / 18 while the training walls, apron and side lids run out to x -21 / 20, so stepping onto the outer columns was an instant respawn ("killbox") - play box widened; (2) the dam was a hollow 2-thick shell, open to the void underneath, with a 1-high slot under every tier's face - the core is now solid (`fillCore`); (3) gaps between the playfield and the noisy canyon rock at every height were filled (`sideFill`, `sealSides` down to solid ground), the start strip's side walls are as wide as the apron; (4) the canyon rim is kept out of reach (y >= 46 from z 8), a rock bank closes the reservoir's far shore, rail gaps let a swimmer climb back onto the footbridge, and an invisible barrier curtain (x -22 / 21, z -10 / 108) encloses the map. **Why the old validator missed them:** it ran on seed 20261124 while the server builds each map with `id.hashCode() * 2654435761L` (the canyon noise differs), it only looked beside cells reachable on foot inside the design search box, and it ignored the play box edge. The validators now use the in-game seed (`model.Seeds`), and the new `EscapeScan` floods walking / jumping / falling anywhere, every holdable cell of the build box (bridging kit), and 6-block snowball knockback from every standing cell, failing on any reachable void column, any exit from the play box, any dead-end pocket and any hazard block.
- **Known regressions, reported:** Bookshelf Run's fast line no longer beats the safe line (the drop-into-the-cup goal was what made the safe line long; `minFastSave` 0); Orchard is gone; Ticket Gates' fast line saves 19.6% (check lowered to 18%).
