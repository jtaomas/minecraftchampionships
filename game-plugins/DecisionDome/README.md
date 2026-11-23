# McmDecisionDome

Vote provider for MinecraftMondayCore (not an McmGame, so there is no `/mcm play` id). On enable it calls
`McmApi.setVoteProvider(...)`. During a championship the core then calls `runVote(remainingIds, onChosen)` between events.

## Two stages (needs MinecraftMondayCore 3.2.0)
- **MCC hub dome (MCC events):** when the core's MCC hub is enabled and built (`McmLobby.isAvailable()`) and the vote belongs to an
  MCC event (`McmLobby.isMccEvent()`, i.e. `/mcm championship mcc`), voters are teleported into the Decision Dome venue inside
  `mcc_lobby`. One stage pad per remaining game lights up in a game colour, with a floating name, item icon and live vote count.
  The facade block-digit clock and a large interior timer count down. After voting, the losing pads are knocked out one by one
  (fewest votes first; smoke, sounds, struck-through label, pad goes dark), then the winner is revealed on the central podium
  (floating icon and name, firework show, titles). Then `onChosen` is called as before.
- **Fallback dome (`mcc_dome` world):** used for Minecraft Monday championships, and whenever the hub venue is unavailable.
  It plays exactly as before (the spin flow below).

## How it plays (fallback dome)
- All online players are teleported to the hub of a glass dome in the void world `mcc_dome`.
- Around the hub is a ring of coloured wedges, one per remaining game (up to 12). Each wedge has a floating
  name + live vote count (TextDisplay) and a spinning item icon (ItemDisplay).
- Players have **20 s** to stand on a wedge (boss bar, countdown titles for the last 5 s, action bar shows your current vote).
- Votes = players standing on each wedge at the end. The pick is **weighted random** (more players = more likely);
  with zero votes the pick is uniformly random.
- A ~4 s decelerating highlight spins across the wedges and lands on the winner, then the dome announces it (gold wedge, title, fireworks)
  and calls `onChosen.accept(id)` exactly once.
- Players are invulnerable, have no hunger, are in adventure mode, and can't break/place/drop items in the dome. Falling out puts you back on the hub.
- Joiners during a vote are pulled into the dome (5 ticks after join, after the core's lobby teleport). Quitters just stop counting.

## Commands (op, `mcm.dome.admin`)
- `/dome test [hub|dome]` - demo vote; only announces the result, then sends players back. `hub` (default when the hub venue is available)
  uses the MCC games in the hub dome; `dome` uses every installed game in the fallback dome.
- `/dome end` - end the voting phase now.  `/dome cancel` - stop a test (players returned) or a real vote (core continues in playlist order via `onChosen(null)`).
- `/dome tp [hub|dome]`, `/dome status` (shows the active stage).

## Known limitations
- More than 12 remaining games: only the first 12 (playlist order) are offered. The test vote picks 12 at random.
- No power-ups, eggs or chickens from the original; voting is by standing on a platform.
- If the plugin is disabled mid-vote, the callback is not called; the core's 1-hour placeholder or `/mcm skip` takes over.

## Admin testing
- `DomeVote.skipWait()` (public, for the core's `/mcm skipwait`) and `/dome skipwait`: while voting, the option with the most votes right now wins (ties or no votes: random among the leaders) with no spin; during the spin/elimination it jumps to the result; during the announcement it hands the result to the core now.
- `/mcm fast on`: voting lasts 1 s, the spin/elimination show is skipped and the announcement is at most 1 s.
