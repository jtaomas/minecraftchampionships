# Parkour Tag (McmParkourTag)

Start it with `/mcm play parkourtag`.

## Rules
- Players are grouped into **sides**: tournament teams stay together. Solo players are grouped into ad-hoc squads about the size of a team, or into squads of up to 4 when there are no teams. If everyone ends up on one side, that side is split in two.
- Sides meet **round-robin**. Each pairing gets its own copy of the same generated arena (`mcc_parkourtag` world, one arena every 120 blocks on X). With an odd number of sides, one side gets a **bye** each round and spectates. There are at least 4 rounds and at most 9; with only 2 sides the pairing repeats.
- Every round, **each side picks a Hunter** by rotation: the teammate who has hunted least goes next, ties broken at random. Everyone else runs. Both hunters play at once: your hunter chases the enemy's runners while theirs chases you.
  - 1v1: one player hunts and the other runs, alternating each round.
  - A side of 1 against a bigger side alternates between hunting and running.
  - A hunter with nobody to chase becomes a runner.
- Round flow:
  - 5 s frozen on your team-coloured start pad.
  - **RUN!** The hunters stay frozen and blinded for another 3 s.
  - 60 s of play.
- A hunter tags a runner by hitting them. The tagged runner is out and spectates above the arena for the rest of the round.
- A round ends when the timer runs out, or earlier once every arena has no hunted runners left.
- Runners get **Runner Vision**, an Eye of Ender. Right-click it to see the enemy hunter for 3 s: a red outline and a pointer beam that only you can see, plus the hunter's distance in the action bar. It has a 10 s cooldown.
- There is no fall damage or hunger, and players cannot break or place blocks. Falling into the void puts you back on your pad.

## Maps
The game picks one finished map at random per game; `/parkourtag map <id>` (or `/mcm map parkourtag <id>`) forces the next one. Ids: `dustbowl`, `urbanjungle`, `factory`, `stadium`.

**Stadium** is daniellaur's Parkour Tag arena (from "daniellaur's minigames", private use only, not an MCC map). Its blocks are a bundled resource (`src/main/resources/maps/stadium.mcmdump.gz`, regenerate with `tools/stadium_extract.py` + `tools/stadium_build.py`; the extractor reads the 1.17 world with the standard library only, run it with `python -I`). All command blocks and redstone are stripped (the game here does the round), the underground lobby is gone and the two start rooms are our cages (runner north, hunter south). The original barrier roof and walls keep everyone inside.

## Scoring
| Event | Points |
|---|---|
| Runner alive at each 10 s mark (while an enemy hunter is after you) | +1 (max 6) |
| Runner alive at the end of the round | +4 |
| Any of your side's runners survive | +12 split among that side's runners |
| Hunter tags a runner | 6, minus 1 per 10 s elapsed (min 1) |
| Hunter tags all enemy runners | 20, minus 3 per 10 s elapsed (min 2) |
| Your hunter clears the enemy before their hunter clears you | +24 split across your side |

Over 9 rounds, a strong player should score roughly 100–150.

## Known limitations
- The sidebar is the same for everyone, because the core API has no per-player lines. It shows the round, the total runners left and up to 4 arena scores. Each player's opponent, role and runners left are in the action bar.
- Runner Vision uses client-side particles, not a real glowing outline, so it does not show through walls. The pointer beam and distance readout make up for that.
- Bye sides and tagged runners spectate in spectator mode, so they can fly around freely.
- Late joiners get the core default (spectator).
- One player alone gets 2 practice rounds with no hunter and earns no points.

## Admin testing
- `/mcm skipwait` ends the intro, the pre-round freeze/countdown or the intermission now.
- `/mcm skip` ends the current round now with no round-end points (survival ticks already scored stay) and starts the next round; after the last round the game ends normally.
- `/mcm fast on`: intro, pre-round countdown and intermission are at most 1 s. Round length and the hunter release delay are not shortened.
