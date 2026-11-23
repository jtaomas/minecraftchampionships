# Dodgebolt (McmDodgebolt)

The MCC-style finale for MinecraftMondayCore. Start it with **`/mcm play dodgebolt`**.

## Who plays
- The two sides with the most tournament points so far, from `ctx.topSides(2)`. Everyone else watches from the stands.
- If only one side is online, its players are split at random into "West" and "East" halves.
- If there's only one player, the game runs in **test mode**: the other half is empty and rounds end on the timer or when you fall.
- If the two sides have the same colour (for example two solo players, both white), the second one is recoloured light blue or orange.

## Rules
- The arena is generated in the void world `mcc_dodgebolt`. It has two team-coloured halves, separated by a glowing 3-block river that players can't cross. There are glass walls and an invisible ceiling, and spectator stands sit outside the walls.
- Every finalist gets one unbreakable bow. One arrow hit from the other side eliminates you, and you go to the stands.
- Arrows are scarce:
  - Each round starts with 1 arrow per side. If a side is behind on score, it gets all the starting arrows.
  - Every 30 s, one more arrow spawns on each side, up to 4 in play.
  - A shot arrow drops as an item where it lands, so arrows landing on your half are yours to pick up.
  - An arrow that falls into the river or the void goes back to the side that didn't shoot it.
  - Arrows held by an eliminated player return to their own side.
- The arena shrinks:
  - From the 4th elimination in a round, each elimination drops the outer floor ring, up to 3 rings.
  - Overtime starts at 60 s. The remaining rings flash red and drop every 4 s, then the last floor crumbles block by block. Falling into the void eliminates you.
- A round ends when one side is wiped out. At the 120 s limit, the side with more players standing wins the round. Equal numbers, or both sides wiped out together, make a draw.
- The match is first to 3 round wins. Win-by-two is available but off by default. Play stops after at most 7 rounds; if it's still tied then, the tiebreakers are total eliminations, then tournament points.
- The champion side is announced in chat and with a title on every player's screen, then fireworks go off for 10 s before `finish`.

## Scoring (modest, as it's a finale; set in `config.yml`)
| Event | Points |
|---|---|
| Elimination | 4 to the shooter |
| Round win | 8 to each member of the winning side (`addSidePoints` with 8 x side size) |
| Match win | 25 to each champion |

A champion with a few eliminations ends up with about 60–75 points.

If one side was split into two groups, points go to each player with `addPoints`, because `addSidePoints` would pay the whole side.

## HUD
- Sidebar: round number and target, round score, players left per side, arrows in play, and an OVERTIME flag.
- Boss-bar timer: the countdown, the round clock and the intermission.

## Known limitations
- Players who disconnect count as eliminated. If they rejoin, they watch from the stands until the next game.
- A side with no online players at the start of a round forfeits the match.
- There's no "foul line" that blocks shooting near the river. The only limit is that players can't cross it.
- The arena is rebuilt on every start and wiped about 2 s after the game ends.

## Credits
The round, arrow and ring-collapse mechanics were partly adapted from DevvyDont's Dodgebolt recreation, which is licensed as follows:

```
MIT License

Copyright (c) 2020 DevvyDont

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

No assets, schematics, text or music from MCC or that project are used. The arena is generated in code.

## Admin testing
- `/mcm skipwait` ends the round countdown, the intermission, the pause before the champions celebration, or the celebration itself.
- `/mcm skip` ends the current round now: no winner, no score change, no points, not counted towards the max rounds; the next round starts. (First to N wins, so there is no "last round" to end on.)
- `/mcm fast on`: round countdown, intermission, the pause before the celebration and the celebration are at most 1 s. Round length and overtime are not shortened.
