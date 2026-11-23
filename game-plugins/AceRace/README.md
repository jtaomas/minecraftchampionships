# Ace Race (McmAceRace)

Individual racing minigame for MinecraftMondayCore. Start it with `/mcm play acerace`.

## How it plays
- Everyone spawns on a start grid (team-coloured pads, teammates together) in the void world `mcc_acerace`.
  A 7-second countdown runs, then the glass gate opens.
- Race **2 laps** of a course built in code. Sections, each starting with a checkpoint:
  1. **Start straight** with speed pads (magenta glazed terracotta: Speed III + a push forward)
  2. **Jump Pads**: emerald pads launch you up two ledges
  3. **Parkour**: small platforms over the void
  4. **Elytra Launch**: purpur pads launch you and give you an elytra. Glide through 3 gold **boost rings** to the landing
  5. **Landing**: the elytra is taken away, then a big **drop**
  6. **Hazard Run**: magma strips. Touching magma sends you back
  7. **Riptide Pool**: you get a Riptide III trident and Dolphin's Grace. Riptide or swim through the pool
  8. **Ring Jump**: a long jump pad over a gap through a boost ring, then the finish arch
- Crossing the finish arch on lap 1 teleports you back to the start straight for lap 2.
- Falling into the void or touching magma teleports you to your last checkpoint. The **red dye** (slot 9, right-click) does it on demand.
- No PvP and no damage of any kind. Finished players become spectators at the grandstand.
- Time limit: 6 minutes. The game ends early once every remaining racer has finished.

## Scoring
| Event | Points |
|---|---|
| Each checkpoint (7 per lap) | 2 |
| Completing lap 1 | 10 |
| Finishing the race | 15 |
| Placement (finishers only) | 1st 50, 2nd 40, 3rd 32, 4th 26, 5th 22, 6th 18, 7th 15, 8th 12, 9th 10, 10th 8, 11-15th 6, 16-25th 4, then 2 |

A winner who completes everything gets about 103 points. Racers who don't finish keep their checkpoint and lap points.

## HUD
- Sidebar: finished count plus the top 5 by progress (lap, checkpoint, distance to the next checkpoint).
- Action bar: lap, checkpoints, next section, your position and current lap time.
- Title at each checkpoint: section name, split time, and the gap to the first player who reached that split (or LEADER).

## Known limitations
- Player collisions are not disabled, because the core owns the scoreboard teams.
- The course is point-to-point. Lap 2 starts with a teleport back to the start straight instead of a looped track.
- The forced elytra glide (`setGliding`) and the riptide pool depend on vanilla client behaviour. They haven't been play-tested on a live server.
- Late joiners get the core's default (spectator).
