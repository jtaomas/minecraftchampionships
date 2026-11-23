# To Get To The Other Side (McmTgttos) - v2

Game id `tgttos` (`/mcm play tgttos`). Paper 1.21.1, MinecraftMondayCore 3.2.x. Spec: `SPEC_v2.md`.
Runtime (mechanics) lives in `com.mcm.games.tgttos`; the 8 maps are pure-Java generators in
`com.mcm.games.tgttos.maps.a` / `.maps.b` behind the contract in `com.mcm.games.tgttos.model` (`TgMap`, `TgMapData`).

## How it plays
- 6 rounds, each on a different random map (no repeats while there are >= 6 playable maps; with fewer, maps repeat and a warning is logged).
- Pre-round 15 s in the start strip with the kit already in hand, held by the map's barrier blocks (plus a strip check that puts anyone outside back). Big title card with the map name, then a "3-2-1 / GO!" title. At GO the barrier (and on elytra maps the gantry floor) disappears.
- Round: 100 s. Punch any chicken in the goal pen to finish: placement title, red "Whack!" text, chicken sound, firework, chat line with place + time + points. Finishers become spectators, confined to the map's play box.
- Falling below the map, leaving the play box, entering a hazard region or touching a hazard block (lava always) sends you back to a random start spawn instantly with a fresh starting kit, and a chicken pun goes to chat.
- No damage at all. Punches and snowballs only knock back. Players never collide (scoreboard team rule on every viewer's board).
- Kits by map: bridging = 64 team-colour concrete + iron pickaxe + 5 snowballs (SURVIVAL); parkour/doors = 5 snowballs (ADVENTURE); boat = seated in a boat + 5 snowballs; elytra = worn elytra + 2 fireworks. You can place only concrete, only on bridging maps, only into air inside the map's build box; you can break only player-placed concrete (anyone's), with no drops.
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
- Knockback: punch 0.42 horizontal / 0.36 up, +0.35 when sprinting with a charged hit, weaker for uncharged hits; snowball 0.38 / 0.28; 10-tick per-victim cooldown.
- Iron pickaxe, no enchantments, unbreakable; snowballs don't regenerate; 2 fireworks on elytra maps.
- 10 chickens per pen, AI on but teleported back if they leave the pen; invulnerable, no collision, no eggs.
- 6 s intermission between rounds; maps use fixed seeds (`random-seeds: false`), turnstile/variant outcomes are re-rolled every round.
- Punches, snowballs and fireworks are disabled before GO.

## Admin
- `/tg list` - maps with type, kit, slot and validation state. `/tg info <id>` - counts, hazards, a sample variant roll, notes.
- `/tg map <id>` - force that map as the next round (or the first round of the next game).
- `/tg tp <id>` / `/tg build <id>` - paste a map (no game running) and teleport there in creative. `/tg reload` - reload config and maps.
- Admin testing hooks (core 3.2.x, declared without `@Override`, fast-test read reflectively so older cores still work):
  `skipRound()` (`/mcm skip`: ends the round now, takes back that round's points, next map or final results),
  `skipWait()` (`/mcm skipwait`: ends the pre-round, intermission or results now), `maps()` / `forceNextMap(id)` (`/mcm map`, same as `/tg map`).
  Fast-test mode (`/mcm fast on`): pre-round 1 s, no slow title card, 0.5 s intermission, no placement reveal. Round length is not shortened.

## Maps and world
- World `mcc_tgttos` (void) is deleted and recreated at every plugin enable. Map slot i is pasted at x = i * 600, local y 0 = world y 64.
- Maps are loaded from `config.yml -> maps:` (fully-qualified class names; missing classes are skipped with a warning) plus auto-discovery of any `TgMap` class under `com.mcm.games.tgttos.maps.*` in the jar. `stub.StubBridgeMap` is a dev stub used only when fewer than 6 real maps load (`stub-map: auto`).
- Each map is built once at enable and checked (>= 40 spawns, >= 8 chickens, pen/strip/bounds present, no dyed concrete). The map is re-pasted with a BuildQueue before its round, clearing anything left from a previous paste at that slot.
- Map lines, time estimates and route checks are documented by the map builders (see the map packages / previews).

## Acceptance checklist mapping
1. 6 rounds, different maps, sidebar round n/6 + map - `TgttosGame.start/prepareRound/sidebar`.
2. 100 s round, 15 s pre-round, held in strip, kit in hand - `beginPre`, `onTick` strip check, barrier blocks, `go`.
3. Kits by type - `giveKit` (+ boat seat in `placeAtStart`).
4. Only player-placed concrete breakable; no placing on non-bridging maps - `canPlace/canBreak` + `GameListener.onPlace/onBreak`.
5. Knockback-only, no collision, no fall/fire/lava death - `GameListener.onDamage`, `punch/snowballHit/knock`, `ScoreTeams`, world gamerules.
6. Void return within 1 s with fresh kit + pun - `onTick` (every tick) -> `respawn`, `Puns`.
7. Chicken finish once, effects, spectator; chickens invulnerable, kept in pen - `whack`, `spawnChickens`, `leashChickens`.
8. Scoring table - `Settings.placementPoints/top10Bonus`, `checkTeamBonus`.
9. Karma - `punch`, `karmaSecond`, `takenMult`.
10. Round end, 80% skip, blocks cleared - `checkRoundEnd`, `onSecond`, `cleanupRound`.
11. `finish` once, `/mcm stop` cleanup - `endGame` guard, `stop()` (tasks, queue, boats, chickens, concrete, glow, elytra).
12. Solo play, quitters - `onQuit` re-checks round end/team bonus; time skip needs 2+ players.
13. Route checks / crossing times - map builders.
14. Turnstiles re-rolled each round, >= 4 of 12 open - `Variants.pick` (map rule, plus a built-in fallback of 4 OPEN for category `turnstile`).

## Known limitations
- Not tested on a live server yet (built only). Boat knockback is applied to the boat and may be ignored client-side while a player drives it. Boats still collide with each other (vanilla). Per-player round placements are in the action bar, not the shared sidebar.
