# Rocket Spleef Rush (McmRocketSpleef)

Game id: `rocketspleef` - run it with `/mcm play rocketspleef`.

## Rules
- Arena: three stacked circular floors of concrete (top/middle/bottom, 12 blocks apart, each wider than the one above) floating over the void in world `mcc_rocketspleef`. Floor size scales with player count (radius 14-32). Each side starts on a team-coloured wool pad on the top floor.
- Every player gets a **Rocket Launcher** (blaze rod, right-click). It fires a straight-flying rocket that blasts a ~2.7-block sphere out of the floor where it hits (no drops) and knocks back everyone within 4.5 blocks, the shooter too, so rocket jumps work. There is no damage. The cooldown (0.9 s) shows as the item cooldown.
- Every player gets **2 Updrafts** per round (feather). Double-jump (press space twice) or right-click the feather to launch upward. Use them to save yourself from a fall.
- Falling below y=68 (under the lowest floor) eliminates you: you become a spectator until the next round.
- 3 rounds, and the arena regenerates every round. A round has a 5 s countdown (rockets locked), 120 s of play, then 45 s of **overtime**. In overtime the floors crumble from the outer edge inward; blocks turn red one second before they vanish. The round ends early when only one side is left. If all players are on one side (or there is only one player), it ends when everyone has fallen or the time runs out.

## Scoring (per round)
- **Survival:** each time someone falls, everyone still standing shares in an 18-point round pool (each fall gives 18/(players-1) points, with fractions carried over).
- **Knock-off:** +5 to the last enemy whose rocket blast hit the victim within 10 s before they fell. There is no credit for teammates or yourself.
- **Last side standing:** +12 to each survivor. If several sides survive the time limit, each survivor gets +6 instead.
- **Late elimination:** +5 / +3 / +2 for the last three players to fall.
- Rough top-player total over 3 rounds: about 100-140.

## Known limitations
- No elytra or durability system and no TNT phase. The double-jump updraft replaces fireworks/elytra.
- Rockets are snowball entities drawn as a firework item. Very fast movement can occasionally clip through a 1-block floor.
- Players who disconnect are removed silently: survivors get no survival points for them.
- The arena is only cleared on the floor Y levels (100/88/76), which are the only places it builds.

## Admin testing
- `/mcm skipwait` ends the round countdown (rockets unlock now) or the intermission.
- `/mcm skip` ends the current round now with no round-end points (no last-side, survivor or placement bonus; knock-out points already scored stay) and starts the next round; after round 3 the game ends normally.
- `/mcm fast on`: the round countdown and the intermission are at most 1 s. Round length and overtime are not shortened.
