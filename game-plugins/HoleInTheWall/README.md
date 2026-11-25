# Hole in the Wall (`hitw`)

Plugin `McmHoleInTheWall` for MinecraftMondayCore. Start it with `/mcm play hitw`.

## Rules
- Everyone spawns on a floating 17x17 platform in the void world `mcc_hitw`. Each team gets a coloured pad; solo players get sea-lantern pads.
- Slime walls with piston caps (5 tall, as wide as the platform) slide across the platform from the North/East/South/West, one block per step.
  Their holes are MCC's own wall set: 70 walls read from the MCC Island wall diagrams (Classic, Fishbowl, Highrise), in MCC's Easy / Medium / Difficult / Extreme sets. Some holes you walk through, some are raised slots you jump into (some with ladders). The set gets harder as the level rises. Full list and sources: `WALLS.md`.
- If a wall reaches you and you are not inside a hole, it pushes you along with it until you fall off. Falling below the platform knocks you out, and you become a spectator (spectator mode, above the platform, kept within about 40 blocks of it; the spectator teleport menu cannot leave the arena). Late joiners and rejoiners spectate the same way.
- Living players are see-through "ghosts" to each other (invisibility + a shared scoreboard team that shows friendly invisibles) and never collide (team collision rule NEVER; TAB's collision is also off server-wide). The walls push players by their bounding box, not by entity collision, so this does not change the walls. Eliminated players lose the effect and the team; everything is removed at game end / stop.
- The difficulty ramp has a **head start** (`difficulty-head-start-seconds`, default 60): walls start at the level they used to reach after 1:00 and keep ramping from there. Real times with the default:
  - Walls get faster every 30 s of ramp time (10 levels, 3.3 to 10 blocks/s): the round starts at level 3 and reaches level 10 at 3:30. Two or three walls can come at once from the start.
  - Wall types work like MCC: Standard and Sike (a fake that vanishes right before the platform) are always on, and the other types join one at a time ("New Wall Type appearing") at 0, 30, 100, 140, 190, 240 and 290 s of ramp time, in a random order each game. With the default head start, two types are in from the start and new ones join at 0:40, 1:20, 2:10, 3:00 and 3:50.
  - Types: Frozen, Waltz, Sprint, Morph, Rush, Sandwich (a pair from both sides that meet in the middle) first; Ender, Returning, Warp, Trick (rushes in, breaks over the edge), Sideswipe (swings round to a neighbouring side) from the third; Dance (steps back over the platform) and Vengeful (narrow, follows a player) from the fifth. Details: `WALLS.md`.
  - The level also picks the wall set: Easy at first, Medium and Difficult in the middle, Extreme at the end and in OVERDRIVE. Chat says "Wall speed increased!" / "Wall difficulty increased!" like MCC, and the sidebar shows MCC's difficulty name.
  - With `difficulty-head-start-seconds: 0` the ramp starts at level 1 (Very Easy).
  - A special type colours the wall's piston caps, and incoming walls are announced on the action bar.
- The platform shrinks by one ring at 2:00 and again at 3:30. The ring turns red 5 s before it goes.
- At 5:00 **OVERDRIVE** starts: 3–4 walls at a time, from every side, at 40 blocks/s. The game ends 60 s later at the latest.
- Players cannot hit or push each other (owner decision after playtest: the walls are hard enough). No Boop Stick.
- The game ends when one player is left (or nobody, in a solo test), or when time runs out.

## Scoring
- Outliving a player: each player still alive gets `round(40 / (players - 1))` points, clamped to 1–6, every time someone is knocked off or falls.
- Surviving a wall wave: +1 to everyone alive, once all of that wave's walls are gone. Sike-only waves don't count.
- Placement bonus: 1st 20, 2nd 12, 3rd 6.
  - Everyone still standing at the end ties for 1st.
  - Players knocked out in the same tick tie with each other.
  - Tied players split the bonus for the places they cover.
- A top player typically ends with about 50–150 points.

## Config (defaults, not researched)
- `config.yml` → `difficulty-head-start-seconds: 60` — offsets only the wall difficulty ramp (level / speed / wave spacing / hole count / wall-type unlocks). It does not change the game clock, game length, platform shrinks (2:00 and 3:30), OVERDRIVE (still at 5:00), scoring or the start countdown. Read at each game start; 0–300.

## Known limitations
- Walls are real blocks moved one cell per step, not smooth display entities. The push is a small teleport plus velocity, so it can look slightly jittery at high speed.
- Collision is only checked when a wall steps. Between steps, the wall blocks are ordinary solid blocks.
- Sideswipe does not rotate smoothly: it jumps a quarter turn (with a sweep effect) just before the platform.
- Highrise's 1x1 peep holes are not reproduced; the 9 diagrams with an empty bottom row are read one row lower (see `WALLS.md`).
- `debug-walls: true` in `config.yml` logs every wall's pattern and types to the console (off by default; not in the shipped config).
- The game runs as one continuous round, not MCC's 3 rounds.
- Players who disconnect count as eliminated for placement, but nobody gets "outlived" points for them.

## Admin testing
- `/mcm skipwait` ends the start countdown (walls start now) or the results screen.
- `/mcm skip` does nothing: Hole in the Wall is a single round.
- `/mcm fast on`: the start countdown and the results screen are at most 1 s. The game length is not shortened.
