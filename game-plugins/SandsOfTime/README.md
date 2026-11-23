# Sands of Time (McmSandsOfTime)

Play with `/mcm play sandsoftime`. Requires MinecraftMondayCore 3.1.0, Paper 1.21.1.

## How it plays
- Each side (team or solo player) gets its own copy of the same generated sandstone dungeon in the world `mcc_sandsoftime`. The dungeon is a 5x5 grid of rooms joined by a randomized maze with a few loops. It is rebuilt from code every game.
- You start in the **hub**. It has the **hourglass** (the physical sand level shows your remaining time), the **Sphinx** (bank), the **Revive Shrine**, and the **EXIT** alcove.
- The hourglass starts at 2:00 and holds up to 2:30. It is paused for the first 20 s, then drains 1 s per second. Each **Sand of Time** you find adds 10 s: right-click the hourglass while holding it. It ticks every 10 s and every second for the last 10.
- Rooms:
  - **Treasure**: loose gold, sometimes gear (iron sword, helmet, boots, chainmail, shield, golden apple).
  - **Sand**: lots of sand.
  - **Husk rooms**: husks spawn the first time you enter.
  - **Dart traps**: pressure plates fire arrows from dispensers in the walls.
  - **Crumbling floor pits**: the gold sits on tiles that collapse into lava shortly after you step on them, then reform after 6 s.
  - **Red key room**: kill the husk guardians to open the iron cage and get the Red Key.
  - **Gold key room**: lava parkour to a pedestal.
  - **Green key**: lying somewhere in the dungeon.
  - **Vaults** (Red, Green, Gold): dead-end rooms behind coloured concrete doors with coloured wall markings. Right-click the door holding the matching key. Vaults hold gold blocks.
- Gold is worth more the deeper the room: base value × (1 + 0.25 × rooms from the hub). Nugget = 1, ingot = 5, block = 25 base.
- **Banking**:
  - Right-click the Sphinx to bank everything you carry, minus a 20% fee.
  - Or walk onto the EXIT pad to bank 100% and get the escape bonus. You become a spectator and can't go back in.
- **Dying** (husks, darts, lava, void) loses all unbanked gold and the sand you carry. Keys go back to where they were found. You wait in a glass cage in the hub until a teammate offers 1 sand at the Revive Shrine, or for 20 s.
- If your side's hourglass runs dry, everyone still inside is **trapped**: they lose their unbanked gold and become spectators.
- The whole game is capped at 8 minutes, after which all dungeons seal (everyone still inside is trapped). It ends early once every side is out.
- Each player can place up to 24 team-coloured carpets to mark a trail. Players can't damage each other.

## Scoring
- Banked coins × 0.12 → points for that player, given as they bank.
- Escaping through the exit: +15 points.
- Team placement by total banked coins (only with 2+ sides): 1st +30, 2nd +20, 3rd +10, split across the side.
- A strong player in a 4-player team ends up around 60–110 points. A solo player who clears vaults can reach about 100.

## Sidebar / HUD
- Sidebar: each side's hourglass time (`*` = paused) and banked coins, sorted by coins (top 5–6 sides).
- Action bar: your own timer, carried and banked coins, and sand count.
- Boss bar: the 8-minute hard cap.

## Known limitations
- Dungeons are spaced 100 blocks apart (8 per row). 40 solo sides means 40 dungeons, built one per tick (about 2 s).
- The world folder is deleted and recreated on each server start. Between games, dungeons are cleared over a few ticks.
- MCC's 2-player blue puzzle key and the "sand sacrifice" mechanic are not implemented.
- The hourglass sand is a 3x3x2 bulb, so the visual is coarse. The exact time is on the sidebar and action bar.
- Husks can wander out of their room.
- Late joiners are left to the core's default (spectator).

## Admin testing
- `/mcm skipwait` ends the start countdown (the hub barriers drop now) or the results wait.
- `/mcm skip` does nothing: Sands of Time is a single round.
- `/mcm fast on`: the start countdown and the results wait are at most 1 s. The hourglass grace and the game length are not shortened.
