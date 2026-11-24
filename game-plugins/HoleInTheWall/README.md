# Hole in the Wall (`hitw`)

Plugin `McmHoleInTheWall` for MinecraftMondayCore. Start it with `/mcm play hitw`.

## Rules
- Everyone spawns on a floating 17x17 platform in the void world `mcc_hitw`. Each team gets a coloured pad; solo players get sea-lantern pads.
- Walls of blocks (5 tall, as wide as the platform) slide across the platform from the North/East/South/West, one block per step.
  They have holes: doorways you walk through and raised holes you have to jump through.
- If a wall reaches you and you are not inside a hole, it pushes you along with it until you fall off. Falling below the platform knocks you out, and you become a spectator.
- Walls get faster every 30 s (10 levels, 3.3 to 10 blocks/s). From level 3 on, two or three walls can come from different sides at once.
- New wall types unlock during the round:
  - Start: Standard, plus Sike (a fake that vanishes right before it reaches the platform).
  - 0:30: Frozen (stops for a moment), Waltz (moves 2 blocks at a time).
  - 1:40: Sprint (alternates slow and fast), Morph (changes its holes just before it arrives), Ender (teleports to the opposite side).
  - 2:20: Returning (turns around and comes back), Trick (creeps up, pauses, then rushes across), Warp (warps forward).
  - Each type has its own colour, and incoming walls are announced on the action bar.
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

## Known limitations
- Walls are real blocks moved one cell per step, not smooth display entities. The push is a small teleport plus velocity, so it can look slightly jittery at high speed.
- Collision is only checked when a wall steps. Between steps, the wall blocks are ordinary solid blocks.
- There is no Sideswipe (rotating) wall type.
- The game runs as one continuous round, not MCC's 3 rounds.
- Players who disconnect count as eliminated for placement, but nobody gets "outlived" points for them.

## Admin testing
- `/mcm skipwait` ends the start countdown (walls start now) or the results screen.
- `/mcm skip` does nothing: Hole in the Wall is a single round.
- `/mcm fast on`: the start countdown and the results screen are at most 1 s. The game length is not shortened.
