# Parkour Warrior — spec v2 (owner-approved)

Replaces the current plugin's design (three difficulty lanes per stage), which was wrong. Game id `parkourwarrior`, plugin `McmParkourWarrior`, world `mcc_parkourwarrior` (created at plugin enable). Research: `..\research\parkourwarrior_rules.md` (rules, Version B = the current redesign) and `..\research\parkourwarrior_maps.md` (course structure, 18 design requirements, obstacle categories). Follow `..\LESSONS.md`.

## Rules (researched)
- **One shared course.** Everyone starts on one start island and runs the same line individually. Players don't collide; nearby players are shown translucent/hidden so they never block a jump.
- **Main line:** 5 stages × 3 obstacles = 15 main obstacles. Each one completed = 1 stone medal and a new checkpoint.
- **Bonus paths:** at the hub at the start of each stage, one optional side path of 3 harder obstacles: 1st gives bronze, 2nd silver, 3rd gold. Each medal once per player.
- **Teleport scroll** (hotbar item): returns a player from a bonus path to the main line, keeping medals. Removed with a chat notice when 30 s remain.
- **Endings:** after stage 5 a pavilion with three gates, Easy / Medium / Hard (lime / orange / red), each 2 obstacles, each with its own finish. A player may switch ending until the 30 s mark. Finishing broadcasts "<player> finished the entire course!".
- **Time:** 10:00 plus 30 s overtime. Normal time: a fall returns you to your last checkpoint, nothing lost. Overtime: a fall ends your run.
- **No damage, hunger, item dropping.** Daylight runs morning → sunset over the game.
- **Team multiplier:** starts ×1.0; each player finishing an ending adds +0.15 / +0.35 / +0.80 (Easy / Medium / Hard).
- **HUD:** boss bar timer ("TIME LEFT", then "OVERTIME"); sidebar: medals obtained (top 3 + you), team multiplier, main challenges left n/15; hotbar medal counters + scroll; chat line per medal ("You completed <name> and earned a <medal> medal!"), action bar "Spawn point updated", red fall line, "You completed the <Zone> zone!" after a stage's 3rd obstacle.

## Scoring (researched shape, scaled ÷5 to our points scale)
- Stone medal: 1 each (15 max).
- Metal medals by how many of that type you hold (1/2/3/4/5): bronze 1/3/5/8/12, silver 2/5/9/14/20, gold 3/6/11/18/28.
- Perfect run before multiplier: 15 + 12 + 20 + 28 = 75.
- Team total = sum of members' medal points × team multiplier; the extra from the multiplier is shared by ending weight 0.40 / 0.45 / 0.65 / 1.10 (none / Easy / Medium / Hard). Award via `ctx.addPoints` per player at the end (medal points live during play, multiplier share at the end).

## Defaults, not researched (config; listed in the README)
- Scroll destination: the hub of the stage whose bonus path you are on. Finishing a bonus path's 3rd obstacle returns you to that hub automatically.
- Finished players: spectator mode with overview teleports (spine top-down, the fork).
- Obstacle lengths/jump distances, moving-block speed, fall kill-height (lowest foothold of the obstacle − 6).
- Overtime knock-out: spectator mode.

## Course (our own design)
Follow all 18 design requirements in `parkourwarrior_maps.md`. Summary: one straight spine ~450–550 blocks of **33 individually designed, named obstacles** (15 main + 15 bonus + 6 ending halves counted as 3 endings × 2), each 15–35 blocks, one movement idea each, no two neighbours with the same idea; **footholds are parts of recognisable themed objects**, not floating cubes; a checkpoint pad (one material used only for pads) after every obstacle; floating label per obstacle; hubs, fork pavilion and finishes in one consistent architectural style of our own; no ground plane, void under everything; backdrop islands 20–80 blocks off the line.

Zones (original; easy → hard):
1. **Toy Box** — building bricks, domino posts, toy train track on stilts, alphabet blocks, jack-in-the-box bounce.
2. **Market Rooftops** — awnings, hanging shop signs on chains, fruit-crate stacks, chimney posts, drainpipe climbs.
3. **Shipwreck in the Sky** — broken mast beam, cargo-net ladders, barrel scatter, cannon posts, rope bridge with missing planks.
4. **Clockwork Workshop** — gear platforms, pendulum weights, slime springs, cuckoo doors on a beat (timed obstacles live here).
5. **Crystal Mine** — amethyst footholds, rail beams on trestles, lantern chains, lift cage with timed platforms, dripstone descent.
Endings: Easy **Meadow & Beehive** (honeycomb columns, flower posts); Medium **Frozen Harbour** (ice floes, lighthouse climb); Hard **Library Tower** (tall vertical climb: book spines, rolling ladder, chandeliers, spiral stair with missing treads) — clearly the hardest thing on the map.

Movement vocabulary across the course: gaps 1–4, up-jumps, drops, neos, ladder/vine catches, 1×1 posts, head-hitters, slime, ice, and at least 3 timed/rhythm obstacles with a visible indicator of where blocks will appear.

Validators (offline, per `..\MAP_BRIEF.md`): every jump on the main line and Easy ending within an "easy" jump table, every jump anywhere within vanilla-possible limits (sprint jump 4 gap flat, less with height gain; ladder/neo rules), a checkpoint pad after every obstacle, kill-height below each obstacle, no foothold look-alike decoration at foothold height, connection post-pass, staircase helper. Renders: whole course top-down + side elevation, one close-up per obstacle (33), hub and fork close-ups, in `ParkourWarrior\previews\`.

## Acceptance checklist
1. One continuous course from one start island; no parallel lanes within stages.
2. 15 main obstacles in 5 themed zones; each gives 1 stone medal once and sets a checkpoint with the action-bar and chat feedback.
3. Each stage hub has one 3-obstacle bonus path giving bronze, silver, gold in order, once each.
4. Normal time: falling returns the player to their last checkpoint within ~1 s, red chat line, nothing lost; a respawn never puts a player ahead of their last earned checkpoint.
5. The scroll returns a player from a bonus path to its hub, medals kept; it is removed with a notice at 30 s left.
6. Three ending gates after stage 5; 2 obstacles each; switching allowed until 30 s left.
7. Finishing an ending adds +0.15/+0.35/+0.80 to the team multiplier once per player and broadcasts the finish; the finisher becomes a spectator.
8. 10:00 then 30 s overtime; in overtime a fall ends that player's run.
9. Game ends at the end of overtime or when everyone has finished or is out; `finish` once; players back in the MCC lobby.
10. Scoring matches the table above, incl. the multiplier split; solo (1 player) works.
11. Boss bar, sidebar, hotbar counters and zone-complete messages update live.
12. Players can't collide, take damage, get hungry, drop or move items, or break/place blocks.
13. Timed obstacles show where blocks will be; at least 3 exist.
14. Bonus obstacles are harder than the main ones of the same stage; the Hard ending is the hardest.
15. Every obstacle passes the jump validators; the main line + Easy ending pass the easy table.
16. Daylight goes morning → sunset.
17. `/mcm stop` cleans up; quitters don't block the end; a rejoining player resumes at their checkpoint.
18. Timing target (owner playtest): strong player everything in ~8.5–9.5 min; average player main + Easy in 4–6 min.

## Owner notes at approval
- **Endings must not be long:** each ending is 2 obstacles and at most ~40 blocks of path in total (Hard is short and steep, difficulty from precision not length). A strong player clears an ending in 30–60 s.
- Emulate the current real game's structure throughout (main line, hub bonus paths, three-gate ending); original builds and names.
