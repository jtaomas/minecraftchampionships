# Grid Runners (McmGridRunners)

Co-op room race for MinecraftMondayCore. Start it with `/mcm play gridrunners`.

## Rules
- Each side (team, or solo player) gets its own copy of a 3x3 grid of rooms in the void world `mcc_gridrunners`. Every lane has the same room layout, chosen at random per game.
- Players start in an entry hall. After a 5 second countdown the door to Room 1 (top-left) opens.
- Each of the 8 challenge rooms has one objective. Clearing a room opens the doors to every room next to it, so the team can split up across several open rooms.
- When all 8 rooms are cleared (or skipped), the gold doors of the final room (bottom-right) open. The first player to reach the gold checkered corner finishes the course for the whole side. Finished players become spectators.
- The game lasts 10 minutes, or ends earlier once every side has finished.
- Anti-softlock: a room gives a hint after 60 s (chat text plus particles or glowing). After 3 minutes it is skipped automatically and gives no points. Lost mobs, sheep, villagers and crystals respawn. Escaped mobs are teleported back. Players can't die: lethal hits heal them and give a moment of Resistance.
- Players are in Adventure mode with an unbreakable stone sword and shears. No PvP, no hunger, no fall damage, no block breaking or placing.

### Room pool (8 of these 10 per game)
| Room | Objective |
|---|---|
| Monster Pit | Kill 6-10 zombies, husks and slimes (count scales with team size; at most 4 alive at once) |
| Pressure Plates | Have all 6 plates lit at once (each stays lit for 14 s); some sit on small towers |
| Sky Steps | Parkour up a spiral of blocks and press the button |
| Library Search | Right-click barrels to find 5 keys hidden in 18 barrels |
| Lamp Grid | 3x3 "lights out" puzzle; each button flips its lamp and the lamps next to it |
| Colour Shear | Shear a sheep of the colour shown on the pedestal, 4 times in a row |
| Hopper Feed | Take crystals from the corner pedestals and drop (Q) 10 of them onto the centre hoppers |
| Hedge Maze | Reach the gold tile in the middle of a randomly generated maze |
| Lever Lock | Set 6 levers to match the code board, 3 codes in a row |
| Villager Guard | Protect a villager through 3 zombie waves. If it falls, the wave restarts |

## Scoring (individual points to every online member of the side)
- Room cleared: 12 points for the first side to clear that room slot, then 11, 10 and so on, with a minimum of 4. Skipped rooms give 0.
- Course finish: 40 / 32 / 26 / 21 / 17 / 14 / 12 / 10 / 8 / 6 for 1st to 10th, then 5.
- A side that clears every room first and finishes first earns 8 x 12 + 40 = 136 points per player.

## Known limitations
- At most 12 lanes. Extra solo players are merged into the smallest lane, which keeps its first side's label.
- Difficulty scales slightly with team size (mob counts), so lanes are equal in layout but not perfectly equal in effort.
- Timers, hints and skips only run while a side has at least one online player.
- The arena is cleared about 1 second after the game ends, and again before the next game starts.

## Admin testing
- `/mcm skipwait` ends the start countdown (the doors open now) or the results wait.
- `/mcm skip`: Grid Runners is one round, so this skips every challenge room that is active right now, exactly like the 3-minute auto-skip (no points, doors open).
- `/mcm fast on`: the start countdown and the results wait are at most 1 s. Game length, hint and auto-skip timers are not shortened.
