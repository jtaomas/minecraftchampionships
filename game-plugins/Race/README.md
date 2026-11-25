# Race (McmRace) - elytra and boat races

Split out of TGTTOS (owner, feedback 3: "To Get To The Other Side is supposed to be jumping around, building blocks,
punching each other with snowballs ... Move all the elytra races, boat races and dropper maps out into a different
game mode - call it Race for now"). Separate from Ace Race. Own world `mcc_race`, MCM game id `race`, display name
"Race", admin command `/race`. The code started as a copy of McmTgttos (package `com.mcm.games.race`); the two
plugins do not depend on each other.

## Rules (defaults, not researched - all in `config.yml`)
- `rounds: 4` rounds, each a different map (Kite Festival = elytra, Frozen Canal = boat, plus any imported elytra / boat maps; dropper maps belong to the core Multi-Stage Dropper), random order, no repeats while there are enough maps. Round length 300 s (`round-seconds`); time skip to 20 s once half the racers have finished (`time-skip.finished-fraction: 0.5`).
- Pre-round 15 s in the start strip with the kit in hand, 3-2-1 / GO; at GO the barrier (and on elytra maps the gantry floor, after `elytra.floor-drop-delay-seconds`) goes.
- **Finish = crossing the finish line / gate**: the map's goal region (`TgMapData.pen`, grown by one block each way) finishes a racer the moment their feet enter it (`race.finish: line`; `chickens` = the old TGTTOS chicken goal, code kept). Kite Festival and Frozen Canal still build the TGTTOS finish arch (chequered board, FINISH sign, hay strip): running into the hay strip under the arch finishes you; no chickens spawn. Imported maps that declare a chicken row / pen: the same region becomes the finish volume, no conversion step needed.
- Coins (OURS - Race is not an MCC game; it uses the MCC TGTTOSAWAF coins, `coins.*`, see `../COINS.md`): 40 for 1st, 1 less per place, top-10 bonus 80 ... 35, team pools 100 / 75 / 50 / 25; line bonus (bonus gates, best tier per section, medium 5 / hard 15, cap 75), checkpoints, refills, boat depots - see the map features below.
- No snowballs (`race.snowballs: false`), no punching (`race.punching: false`). Elytra crash rule on (`elytra.crash-kills`: a wall slam or nose-dive sends you back to the last checkpoint; floor touch = death is `elytra.floor-kills`, off).
- No damage; void / out of the play box / hazard = back to the last checkpoint (or the start) with a fresh kit.
- McmGame hooks as in TGTTOS: `currentMap()` (chat feedback log), late join (an unfinished participant rejoins from a start spawn; finished or intermission = spectator), spectators confined to the play box.

## Admin
`/race list | map <id> | info [id] | chickens | tp <id> | build <id> | reload` (permission `mcm.race.admin`), same behaviour as `/tg`.

## Imported maps
`plugins/McmRace/imported/<id>/` (map.yml + packed blocks), format unchanged from the TGTTOS loader; Race accepts types elytra and boat only (drop / run are skipped with a log line). The review server's folders are still under `plugins/McmTgttos/imported/` - move them at deploy time. Runtime feature requests from the Hylur import work go in `_candidates/RACE_FEATURES.md` (not there yet when the split was made).

## Offline tools
- `sh tools/b/kite_render.sh` - Kite Festival validator (`KiteV`, `KiteGlide` flight model) + previews.
- `sh tools/b/canal_quarry_render.sh` - Frozen Canal validator (`CanalV`) + previews (the file name is historical; Quarry Steps stayed in TGTTOS).
- Both default to each map's in-game seed (`model.Seeds.fixed`, same formula as `MapRegistry`). `NORENDER=1` skips PNGs.

## Not verified in game
Nothing in this plugin has run on a server yet: the line-finish trigger at elytra / boat speeds (a racer moving more than ~3 blocks per tick could in principle skip a 3-deep volume between two position checks), the HUD texts that still say "whack" in places, imported maps from the new folder.

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
- Median estimate: about 85 s. Weak players: about 115 s; with 300 s rounds they DNF only when the time skip (half finished -> 20 s left) catches them.
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

### Interactive map features (`model.MapFeatures`, `TgMap.features()`, runtime `FeatureRuntime`)
- **Movers**: pendulum / rotor / slider made of BlockDisplays (2-tick interpolation); hit test = sphere vs the hazard boxes in the mover frame, sampled along the racer's path each tick; a hit sends the racer back to the last checkpoint ("was flattened by the ..."). Solid movers also place barrier blocks in their current cells. Telegraphs: pendulum whoosh at the bottom of the swing, rotor sound every quarter turn, slider glows red 1 s before it closes and plays a door sound (sounds only within 28 blocks of a racer).
- **Toggles**: timed block frames (placed without physics; solid blocks wait for an empty cell); indicator blocks turn to a warn block before the dangerous frame.
- **Traps**: a block that gives way under a racer (creak, delay, drop, restored once nobody stands there).
- **Checkpoints** (crossing the trigger saves it; void / hazard / mover respawns go there with a fresh kit), **refills** (elytra: +rockets, once per opening per attempt), **depots** (boat maps: a racer on foot gets a fresh boat).
- **Bonus regions**: tier 1 orange / 2 red / 3 brutal; the best tier per group pays once, only on finishing, capped (`bonus.enabled`, `bonus.tier-points [1,2,3]`, `bonus.cap 8`; 1st place is 24, so a perfect hard line is worth a third of a win). Shown in the title, results and chat ("+N line").
- All features stop and restore their blocks at round end. `/tg info` shows feature status.

### Kite Festival features
Glider's Challenge (MIT) letterbox sequence on drops 1-2, red / brutal holes added to the existing screens, fire-ring frames on the ring stack, six movers (wrecking ball, turn 1 / turn 2 pendulums, lift fan, windmill, needle shutter; all clear of the lime line at every phase, open >= 50% of the time for the orange / red lines - `tools/b/KiteV`), 5 ledge checkpoints, 8 gold refill openings (+2 rockets), 6 rockets up front, 33 bonus openings in 5 groups.

### Frozen Canal features
Blue Mile pendulum (edges always clear), causeway lock gate (iron bars, 3 s open / 1.5 s shut, lintel goes red 1 s before), Blue Mile slush strip (blue ice -> snow 2 s every 7 s, wall lamps warn), 5 checkpoints, 2 boat depots, bonus regions on the hairpin inside kerbs, the lock gate and the blue stripe (`tools/b/CanalV`).

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

