# Find the Button — spec (approved by owner)

Game id `findthebutton`, display name "Find the Button", plugin `McmFindTheButton`, world `mcc_findthebutton` (create at plugin enable, void world via `McmBuild.voidWorld`). Simple game; maps should be tidy and correct, not showcase builds.

## Rules
- 5 rounds (config), each on a different map picked without repeats; 90 s per round (config).
- One stone button is hidden on the map, at a spot chosen at random each round from that map's ~10 hand-placed hiding spots (each spot = block position + face; spots vary in difficulty: behind furniture, under stairs, on a ceiling beam, inside a cupboard nook, etc. Every spot must be reachable on foot and pressable).
- Round flow: 5 s countdown in a start area (players frozen or behind a barrier wall that is removed — follow LESSONS.md on gates) → search → first press scores → pressers become spectators (spectator mode, confined to the map bounds) until the round ends → round ends when everyone has pressed or time runs out → 4 s results → next round.
- Scoring per round: 1st presser 20, each later one 2 fewer, minimum 6. Team bonus: +5 to each member of a side when every online member of that side found it (sides with 2+ players only). Non-finders get 0.
- Hint: from 45 s in, a particle pulse + soft sound every 3 s at the button, visible/audible within ~12 blocks; from 70 s within ~24 blocks.
- No PvP, no damage, no hunger, no block breaking/placing, no item drops. Players in ADVENTURE (they only need to press a button; make sure button pressing works in adventure — it does for interact; verify listeners don't cancel it). Only the real button counts; no decoy buttons in the maps.
- Players can't push each other off anything dangerous: no void exposure inside rooms; falling out of the map teleports back to the start.
- Works with 1 player. Handles quitters. Ends itself. Follows `BRIEF.md` API contract (finish once; stop() idempotent).
- HUD: boss-bar timer via `ctx.setTimer`; sidebar: round x/5, map name, found n/m, top 3 finders this round; action bar: "Find the button!" / your placement. Use `McmFx` for countdown, title card with the map name, a find sound + firework for each finder, results.

## Maps (6, simple, enclosed rooms ~20x20x8, pure-Java generators per `MAP_BRIEF.md` offline approach)
Library, Kitchen, Garden Shed & yard, Mineshaft, Toy Room, Ship Cabin. Each: one or two connected rooms with furniture built from blocks, well lit, a clear start spot, and ~10 hiding spots defined in code next to the build. Use the lessons: staircase helper, connection post-pass for fences/panes/walls, no carpets on non-full blocks. Offline-render each map once (cut-away) to check it's sane; previews in `FindTheButton\previews\`.

## Acceptance checklist (the bot test and reviewer will check these)
1. `/mcm play findthebutton` starts; all players are teleported into the round-1 map.
2. Players cannot move out of the start area before GO, and can after.
3. Exactly one button exists in the map each round, at one of the map's defined spots.
4. Every defined spot on every map is reachable by walking/jumping and the button is pressable (offline validation: flood-fill reachability from the start to a standing position within reach of each spot).
5. Pressing the button awards 20/18/16… by order and makes the presser a spectator.
6. Pressing anything else, or pressing twice, awards nothing.
7. Team bonus is awarded only when the whole side found it.
8. Hints start at 45 s and widen at 70 s.
9. Round ends early when everyone has found it; otherwise at 90 s.
10. 5 rounds use 5 different maps; the game then calls `finish` once and players are back in the lobby.
11. `/mcm stop` mid-round cleans up (tasks cancelled, button removed, players restored).
12. No damage, no block changes, no item drops possible during the game.
13. A player who quits mid-round doesn't block the round from ending.
