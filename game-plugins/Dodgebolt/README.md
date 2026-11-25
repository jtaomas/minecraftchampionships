# Dodgebolt (McmDodgebolt)

The MCC-style finale for MinecraftMondayCore. Start it with **`/mcm play dodgebolt`**.

Rules, arena and look follow real MCC Dodgebolt (Season 6 "pool" arena). Research: `game-plugins/research/mcc_archive/dodgebolt/db-1/`
(README, notes.md, REFERENCE_IMAGES.md, frames). Settings marked `[default]` in `config.yml` are not researched.

## Who plays
- The two sides with the most tournament points so far (`ctx.topSides(2)`). Everyone else watches from the stands.
- If only one side is online, its players are split at random into "West" and "East".
- If there's only one player, the game runs in **test mode**: rounds end on the timer or when you fall.
- White/black/grey sides are shown as orange or light blue (they would vanish on the white deck); two same-coloured sides are split orange / light blue.

## Rules (MCC)
- 4v4 style, **first to 3 round wins** (best of 5). **One hit eliminates.** Players can't cross the black centre line.
- **Only 2 arrows ("bolts") are ever in play.** At round start one drops on each side's black/white target pad; if a side is behind on score, **both drop on its side**.
- A shot bolt that hits the floor or a wall becomes an item where it lands; whoever is on that half picks it up. A bolt that hits a player drops at their feet.
  Bolts lost in the lava come back on the target pad of the half they fell on. Bolts held by an eliminated player drop where they were hit.
- **The arena shrinks** one ring at a time: 2 blocks for each of the first two eliminations, 1 for the third, 1 for each later one, and 1 for every 6 shots fired; never more than 8 blocks. Each one-block shrink sends "⚠ The arena is about to shrink!", turns the outer ring red for 2 s, then drops it into the lava; consecutive shrinks are 1 s apart.
- **Falling into the lava eliminates you.**
- Spectators (and only spectators) see a purple arrow badge above every player carrying a bolt.
- Eliminated players watch from the rim behind their half; non-finalists and late joiners watch from the stands (adventure mode, kept out of the pool by invisible walls). Set `crowd-in-stands: false` to use spectator mode instead.
- Round end: "Round Over!" title, the physical scoreboard updates, then "NEXT ROUND IN" for 15 s. Match end: "♛ <team> ♛ / TAKE THE CROWN" over the live arena with fireworks.

### Additions that are not MCC (labelled `[default]` in config)
- `round-seconds: 180` safety cap (MCC has no round timer): more players standing wins, equal = draw.
- `shrink.idle-seconds: 30`: if nothing shrank the arena for 30 s, it shrinks one block anyway, so a round where nobody shoots still closes in.
- `glowing-bolts: true`: dropped bolts glow so they are easy to see on the white deck.
- Coins: MCC gives no coins in Dodgebolt ("this one's for the win"). In a championship the Dodgebolt winner is declared the event champion (`ctx.setEventWinner`), whatever the coin totals.

## Arena (generated in the void world `mcc_dodgebolt`, rebuilt in the match colours at each start)
- Deck at y 64: x -12..12, side A z -17..-1, side B z 1..17, black centre line at z 0. White deck with bold team-colour pixel art and a 7x7 black/white target pad per side (|z| 3..9). After the maximum 8-block shrink each side is 9x9 around its pad.
- Lava one block under the whole deck. Pool walls: grey stone below deck level, team colour, white band on top, quartz pillar at the centre line.
- Spectator ring: stone-brick walkway at rim level (4 blocks above the deck), four tiers of spruce seats, a high stone outer wall with two hanging white scoreboards (red pixel digits, updated each round), a team-coloured crown panel behind each half and two big team banners.
- Invisible barrier walls and ceiling over the pool keep the arrows in and the crowd out.

## Resource pack (section "Dodgebolt" in `_candidates/pack/build_pack.py`)
- Bow: `custom_model_data` 7301 on the Dodgebolt bow only, charcoal bow (from the Witchbolt frame; MCC's only published event bow texture is identical to vanilla).
- Bolt: Dodgebolt's arrows are `SPECTRAL_ARROW` items with `custom_model_data` 7302 (white outlined bolt). Only Dodgebolt fires spectral arrows, so the bolt look in flight comes from a replaced `entity/projectiles/spectral_arrow.png`; normal arrows and bows keep the vanilla look.
- Sounds (with the pack): `mcc.countdown`, `mcc.go`, `mcc.death`, `mcc.crowd.cheer`, `mcc.victory.win`; vanilla sounds otherwise.

## Scoring (config `coins.*`, see `../COINS.md`)
| Event | Coins |
|---|---|
| Elimination | 0 (MCC) |
| Round win | 0 (MCC) |
| Match win | 0 (MCC); the winner wins the event (`coins.winner-wins-event: true`) |

Values above 0 are house rules (ours) and pay normal multiplied coins.

## Known limitations
- Players who disconnect count as eliminated. If they rejoin, they watch until the next game.
- A side with no online players at the start of a round forfeits the match.
- MCC's older "curved line that disables your bow" is not in the S6 arena and is not implemented.
- No mascot statues or merch booths (decoration only).

## Admin testing
- `/mcm skipwait` ends the round countdown, the intermission, the pause before the celebration, or the celebration itself.
- `/mcm skip` ends the current round now: no winner, no score change, no points; the next round starts.
- `/mcm fast on`: countdown, intermission and celebration waits are at most 1 s. Round length and shrink timing are not shortened.

## Credits
Earlier versions adapted the round and ring-collapse idea from DevvyDont's MIT-licensed Dodgebolt recreation (MIT License, Copyright (c) 2020 DevvyDont).
No assets, schematics, text or music from that project are used. The arena is generated in code from MCC reference images.
