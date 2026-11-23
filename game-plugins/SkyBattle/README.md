# Sky Battle (McmSkyBattle)

Game id: `skybattle`. Start it with `/mcm play skybattle`.

## Rules
- The map is generated in the void world `mcc_skybattle`. It has a ring of team-coloured starting islands (one per side, at least 4), a ring of mid islands and a large central island with a raised plateau.
- Each player starts with a wooden sword, a stone pickaxe, 48 team wool, steak, and a team-dyed leather chestplate and boots. Team wool is topped back up to 32 every 20 s.
- Chests use three loot tiers. Starting islands have 2 chests each with basic gear. Mid islands and the edge of the centre have mid-tier chests (iron gear, bows, pearls, golden apples, healing potions). The centre plateau has 4 top-tier chests (diamond gear, Sharpness/Power/Protection items, Healing II).
- There is a 10 s frozen countdown, then 480 s of play:
  - From 0:45 the border shrinks towards the mid islands. From 4:00 it shrinks to the centre island, and from 6:30 down to 6 blocks.
  - From 2:00 the safe height range closes in. The ceiling drops from Y140 to Y111 and the floor rises from Y65 to Y95.
  - Being outside the border or the safe height deals 1 heart per second (1.5 hearts in the final phase). This damage ignores armour.
- PvP is on. Friendly fire between members of the same side is off. Fall damage is on. Falling below Y55 is instant elimination.
- When you die you drop your items, become a spectator and are moved above the centre. There are no respawns.
- You can break and place blocks only in the arena, only while the game is live, and only as an alive player. The map is wiped and regenerated every game.
- The game ends when one side is left (or when nobody is left), or when time runs out.

## Scoring
- Kill: +10 to the killer. The last player to hit the victim within 12 s gets the credit, including for void and border deaths.
- Survival: whenever any player is eliminated, every player still alive gets +2 (+1 if there are more than 20 participants).
- Placement (side points, split across the side): 1st 60, 2nd 48, 3rd 38, 4th 30, 5th 23, 6th 17, 7th 12, 8th 8, 9th 5, 10th 3, lower places 1.
  - A side gets its placement when its last member dies.
  - Sides still alive at the end are ranked by players alive, then by total health.

## Sidebar
Players alive, sides alive, border size, safe Y range, top killer and phase. The action bar shows your own kills.

## Known limitations
- The border is the vanilla square world border, not a circle.
- There is no Skymart shop and no gold/diamond currency. Gear comes from chests only.
- The arena is reset by tracking generated and player-placed blocks. Blocks that appear some other way, such as liquids or falling blocks, are not tracked. No loot item can create them.
- The world folder is deleted and recreated once per server start.
