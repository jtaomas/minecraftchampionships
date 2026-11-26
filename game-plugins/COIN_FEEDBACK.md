# Coin feedback: what MCC does, and what we will build

Status: **research done, nothing built yet.** Owner review pending.

Evidence lives in the private research folder `game-plugins/research/mcc_archive/coin_feedback/`:
- `R1_ARCHIVE.md`: the on-disk archive (pack assets, sounds, per-game notes), our code host points, and every coin award call in every game.
- `R2_VODS.md` + `sources.md`: MCC Season 6 player-POV VODs (62 cited frames in `frames/`, 9 audio clips in `audio/`) and the MCC wiki.

Labels: H = seen directly in a frame or file, M = inferred from two sources, L = guess. The details and frame paths are in the two files above.

## 1. What MCC does

| Element | MCC behaviour | Evidence |
|---|---|---|
| Coin amount | **Chat only**: `[+N🪙] <text>`. White digits plus a gold coin glyph. N already includes the game multiplier, with one decimal only when fractional (e.g. `[+22.5]`). Only the earner sees `[+N]`; a spectator sees a skull in its place. | H (R1, R2) |
| Kill graphic | Crossed swords under the crosshair, with a dark box under it: `ELIMINATION!` in red, then `[team icon][head] VICTIM` in white caps. No coin number and no killer name. | H (R2: Sky Battle, Battle Box, Survival Games, Meltdown) |
| Kill tiers | Single kill grey, `DOUBLE ELIMINATION!` bronze, `TRIPLE ELIMINATION!` silver. Meltdown's first tier is `FREEZE!` in cyan. | H (R2) |
| Kill timing | Pops in about 0.3 s (white dot, star, ring burst, icon). Holds about 4 s in total. The text disappears about 0.13 s before the box and icon fade. One slot: a new kill replaces the banner and restarts the timer. | H (R2, frame-measured) |
| Objective graphics | Ace Race `LAP COMPLETED!` flag box (about 4.1 s) plus a `Lap N/3` title. Grid Runners `ROOM COMPLETED!` box. Parkour Warrior `+1 [medal]` pop (about 1.45 s). | H (R2) |
| No graphic | TGTTOS finish: only a gold coin burst and a red hit flash. Hole in the Wall: chat `[+6]` per survival tick, then a round-end summary. Ace Race checkpoints: chat plus a red or green time delta. | H (R2) |
| Sands of Time | `+42 [coin]` about 20 GUI px below the crosshair, with a second simultaneous gain in brackets. | H (R1 frames) |
| Coin animation asset | `coin_receive`: 11 frames at 128×128 in font `mcc:callouts` (U+E00B–E015). A coin fades in, falls about 53 px onto the baseline and lands with a white halo. Siblings: `champion_score_receive`, `gem_receive`, `experience_receive`. The pack has no `.mcmeta`, so the server drives the frame timing. | H (R1) |
| Sound | One bright ding of about 0.15–0.2 s for all awards. Closest vanilla sound is `entity.experience_orb.pickup` at pitch 1.0. Archive candidates are `ui.coin_receive` (0.53 s) and `games.global.score.small_coins` / `big_coins`. No pitch ramp on kill streaks was found. | M (R2 audio, R1 sounds.json) |

## 2. Our code (R1 §C)

- One central place: `TeamScoring.addCoins` (`plugin-src/com/mcm/TeamScoring.java`). It already sends the chat line and a sound for every `ctx.addCoins` / `addTeamCoins` / `addPoints` / `addSidePoints`. The built-in games go through `addIndividualPoints`, which needs the same hook.
- It only gets a free-text reason. Planned: a backward-compatible default method on `McmGameContext` that carries a small cue (kind = kill / objective / tick, the victim name, the label). Old game jars keep working and fall back to chat only.
- In every game the victim is available where kill coins are paid, but only Meltdown and Parkour Tag pass it on.
- Several games already print their own coin text. A central hook would double it, so those lines go (list in R1 §C/§D).

## 3. Still unsettled

- Length of the multi-kill window, and whether a fourth tier exists.
- Whether the Parkour Warrior medal pop stacks.
- Full length of the Grid Runners graphic (the clip was cut by a teleport).
- Whether Rocket Spleef Rush has a centre banner. Dodgebolt was not sampled.
- The exact title fade timings at frame level for the objective boxes (only approximate).
- The elimination banner art itself (box, crossed swords, tier colours) was seen in VODs but not found as a file in our archive. It would be redrawn from the frames.
