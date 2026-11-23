# Foot Race (McmFootRace)

Game id: `footrace` (`/mcm play footrace`). Display name: **Foot Race**.

## How it plays
- A 9-wide closed-loop track is generated in the void world `mcc_footrace` at game start, with four themed straights:
  - **Stadium** (start straight): starting grid, start/finish line, hurdles, a soul-sand bog with a blue-ice fast lane on the inside, and three 2-block gaps.
  - **Jungle**: slime pads that launch you over a void, then three 1-wide bridges (the middle one is broken).
  - **Nether**: a 7-high ladder wall, magma stripes (touching magma is a hazard), and slalom walls.
  - **Frost**: a packed-ice straight with hurdles.
  - **Shortcuts**: gold stepping stones across the SE corner and packed-ice stepping stones across the NW corner. Both are shorter but you have to jump over the void.
- Players line up on team-coloured pads inside a glass start box. After a 10 s countdown the gate opens.
- Run **3 laps**. Each lap has 6 checkpoints (5 on the track plus the finish line) that must be crossed **in order**. Crossing one out of order does nothing and shows a "missed a checkpoint" warning.
- Falling into the void or touching magma teleports you back to your last checkpoint.
- Punching another runner does no damage but knocks them back.
- Finished runners become spectators above the middle of the track.
- Time limit: **5:00**. The game ends early once everyone still in the game has finished.

## Scoring
- **15 points** for each lap completed, awarded straight away (DNF runners keep their lap points).
- **Finish bonus** by placement: 60 for 1st, then 3 fewer per place, with a minimum of 10.
- A winning run is worth 45 + 60 = 105 points.

## HUD
- Sidebar: the top 5 by progress (lap and % of the race), plus the number finished.
- Action bar: your lap, position, checkpoint count and current lap time.
- Boss bar timer from the core.

## Known limitations
- Progress for ranking comes from projecting your position onto the track centre line, clamped between your last and next checkpoints. Positions can flicker slightly on the shortcuts.
- The slime launch is a scripted velocity (forward along the track), not vanilla slime physics.
- The grid has 56 slots. With more than 56 players, slots are shared.
- Players' game modes are set back to survival before `finish`. The core is assumed to handle lobby state from there.

## Admin testing
- `/mcm skipwait` ends the start countdown (the race starts now) or the short wait after every runner finished.
- `/mcm skip` does nothing: Foot Race is a single race.
- `/mcm fast on`: the start countdown and the end wait are at most 1 s. The race time limit is not shortened.
