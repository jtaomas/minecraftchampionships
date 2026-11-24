# McmBingo - Bingo but Fast / Lockout Bingo

Two MinecraftMondayCore games in one plugin (`McmBingo.jar`):

| id | `/mcm play <id>` | name | time limit |
|----|------------------|------|------------|
| `bingo` | `/mcm play bingo` | Bingo but Fast | 10 min |
| `lockout` | `/mcm play lockout` | Lockout Bingo | 12 min, or earlier once all 25 squares are claimed |

Design research (item pools, card weighting, world generation): `../research/bingo_design.md`.

## World
- A new survival world `mcc_bingo` with a random seed is generated when the server starts (the old `mcc_bingo` folder is deleted first, and only that exact folder is ever deleted). Each game is played in a fresh, never-visited area of it, 4096 blocks further east each time, with a 1200-block world border.
- **13 small biomes per area.** The world uses the plugin's own biome provider on top of the vanilla terrain generator. Each area has 13 chosen biomes laid out as cells: 4 inner cells meet at the start point, and 9 outer cells reach the border. Each area takes one biome from each of these families: plains/meadow, oak or birch forest, dark forest, taiga, desert, badlands, snowy, jungle/bamboo, swamp/mangrove, cherry grove or mushroom fields, and savanna. Two extra biomes are added on top.
  - Water stays where the vanilla terrain puts it: oceans, rivers and beaches, recoloured to the climate of the cell they are in (frozen, cold, lukewarm or warm ocean).
  - Underground, one inner cell has lush caves below it and one outer cell has dripstone caves. There is no deep dark, so there are no ancient cities.
  - The area centre is chosen on land, preferably with water within 450 blocks.
- **Structures.** These are placed with the vanilla `/place structure` command while the game is preparing, one per tick:
  - one village, of the type that matches its biome
  - a desert pyramid, if the area has a desert
  - a jungle pyramid, if the area has a jungle
  - a ruined portal 80 to 220 blocks from the start
  - a beached shipwreck and a buried treasure, if a beach exists; otherwise a shipwreck in the ocean
  Vanilla adds mineshafts, and sometimes more structures. The card menu lists each placed structure with its direction, distance and coordinates.
- **More caves.** Extra worm tunnels are dug as chunks generate. About 6 % of chunks start one, and 30 % of those open to the surface as cave entrances. They only replace natural ground blocks, and are never dug next to water or lava.
- Preparing: the chunks around the start and the structure sites load asynchronously while players wait in the lobby. The structures are then placed and the card is drawn. After that everyone is teleported and the countdown starts. The area plan (biome census and sites) is computed in the background before the game, from noise only.

## How it plays
- All sides start together at one shared start point, spread over a few blocks.
- **Kit:**
  - unbreakable iron pickaxe, axe and shovel (Efficiency II)
  - 64 steak
  - permanent Speed I
  - the **Cave Finder** (a compass)
  - the **Bingo Card** (a knowledge book)
- **Card:** every side has the same 5x5 card.
  - Bingo: 8 easy, 10 medium, 5 hard and 2 very hard items.
  - Lockout: 6 easy, 10 medium, 6 hard and 3 very hard items.
  - At most one item per group appears on a card (Fetchr-style categories, for example one village item and one jungle item).
  - At most 2 squares can be made from wood alone.
  - Biome-, structure- and mob-specific items are twice as likely to be picked as generic ones.
- **Mob items:** Slimeball, Sticky Piston (hard) and Lead (very hard) are back on the card. Slimes spawn in slime chunks below y 40 at any time of day. Swamp slimes and other surface night mobs are not counted on, because a game mostly ends before night.
- **Obtainability:** every pool item declares what it needs, such as biomes, a structure or the mobs of a biome. A card only uses items whose needs are met by this area's actual biomes and placed structures. Nether and End items are never used, because portals are disabled. Each card square shows its difficulty, its point value and where to find the item.
- An item counts as soon as it is in a side member's inventory (checked every 0.5 s).
- **Card menu:** right-click the card in your hand, or click it anywhere in your inventory. The card cannot be moved, dropped or swapped. The menu shows the scoring, the structures, the biomes in the area, teleports to the start and to teammates (45 s cooldown), and every square's status and value.
- **Cave Finder:** right-click it to scan the 5x5 loaded chunks around you for a sizeable underground air pocket. The result shows in the action bar (distance, direction and depth), with a short particle trail, and the compass needle points to the cave. Cooldown 20 s. It does not count as a Compass for the card, and it cannot be crafted into a map.
- **`/top`:** teleports you to the highest block above you, but only when something is above your head. Cooldown 30 s. It is blocked for 8 s after you take PvP damage, and teleports from the card are blocked during that time too.
- **Rules:**
  - PvP is on between sides; friendly fire is off; no damage during the countdown.
  - Keep inventory and keep XP; nothing drops on death, so a kill gives no items. Kills give no points (configurable).
  - Immediate respawn at the shared start, or at your own bed or anchor if it is inside the current border.
  - Hunger on. Natural mob spawning is on: animals and hostile mobs, on the surface and in caves. No phantoms.
  - Time runs (day-night cycle) from sunrise (time 0) at the start of each game. A 10-minute Bingo game ends around dusk; Lockout (12 min) gets a short stretch of night.
  - Clear weather; portals disabled.
  - Sleeping is vanilla: the night is only skipped if every player in the world sleeps (`playersSleepingPercentage` 100).
  - Deaths to mobs are handled like PvP deaths: keep inventory and XP, nothing drops, and respawn at the start or at your own bed or anchor inside the border.
  - Why mobs were missing before: the server's `server.properties` has `spawn-animals=false` and `spawn-monsters=false`. Bukkit applies these to every world as spawn flags, so the plugin now sets this world's spawn flags (and the `doMobSpawning` gamerule) itself.

## Scoring (given to every online member of the side)
Values per tier: easy, medium, hard, very hard.

**Bingo but Fast**
- Square: 3 / 5 / 8 / 12 points for the first side to get it, then 80 %, 60 % and 40 % of that for the 2nd, 3rd and 4th side onwards.
- Line (row, column or diagonal; 12 in total): +12 each for your first 4 lines, then +6.
- Full card: +30. The game ends early once every remaining side has a full card. A side with a full card becomes spectators (above the start, kept within 320 blocks) until the game ends.
- A typical card is worth 138 points if a side finds every square first. The old flat scale (6 for first) gave 150. A realistic top run is about 15 squares plus 1 or 2 lines, roughly 70 to 100 points.

**Lockout Bingo**
- Claiming a square (the first side to obtain it): 5 / 8 / 12 / 16 points. Nobody else can get it after that.
- Owning all 5 squares of a line: +15.
- Final placement by squares claimed: 1st +20, 2nd +12, 3rd +6 (ties share the higher place).
- All 25 claims on a typical card add up to 230 points, split between the sides. The old flat 8 per claim gave 200.

## Config (`plugins/McmBingo/config.yml`): defaults, not researched
These are sensible defaults chosen by us, not values taken from research. Adjust them after playtesting.
- card mixes, the wood-only cap and the card teleport cooldown
- tier points, placement percentages, line, full-card and placement bonuses, and kill points (0)
- `/top` cooldown (30 s), the combat block on or off, and the combat time (8 s)
- Cave Finder cooldown (20 s) and scan radius (2 chunks)
- steak count (64)
- mob spawning on/off, day-night cycle on/off, and the start time of day (default 0 = sunrise)
- whether to place structures, the extra-cave chance (0.06 per chunk) and the entrance share (0.3); these two take effect at the next server start
- `debug-timing`: logs the preload, card, teleport and cave-scan timings
- Also chosen by us: the biome families, the 13-cell layout, the cave-biome placement, structure distances, 4x4x4 cave cells with ≥36 air and ≥6 cells for the Cave Finder, and all item tier assignments (checked against Extremelyd1's tier list).

## Offline check
`com.mcm.games.bingo.CardCheck` (in the jar) plans 500 random areas on synthetic terrain and generates 4 cards for each. It verifies that every card has 25 distinct squares, that every requirement is met by that area's biomes and structures, that no group repeats, that the wood cap holds, that the tier mix is right and that every site is inside the border. Last run: 500 areas, 2000 cards, 0 failures; all 251 pool items appeared; 9 to 13 surface biomes present per area (13 in 68 % of areas).
Run it with `target/classes`, the paper-api jar, guava 32, adventure and jetbrains-annotations on the classpath.

## Known limitations / not verified without a live server
- The world generation with the custom biome layout, `/place structure` from the plugin (including its success detection from the command feedback) and the extra-cave populator were compiled against the API but have not run on a server yet. Check the log line `Bingo area N at x,z: ...` and the `placed ...` lines at the first game.
- Placing a village stalls the main thread for one tick, measured at 1-2 s in the bot test, while players are still in the lobby. It is a single vanilla `/place` call and cannot be split cheaply, so it was left as is.
- The preparing phase loads about 250 chunks (start plus structure sites), which takes longer than before. There is a 60 s timeout; structures whose chunks are not loaded in time are skipped, and their items are left off the card.
- Items are detected in inventories only, not in chests. Handing an item to another side also counts for that side.
- If Windows still has region files locked after an unload, deleting the old world fails. It is retried at the next server start.
- Late joiners and rejoiners spectate inside the bingo world, kept within 320 blocks of the start area.

## Admin testing
- `/mcm skipwait` ends the start countdown (GO within half a second).
- `/mcm skip` does nothing: Bingo is a single round.
- `/mcm fast on`: the start countdown is at most 1 s. The game length is not shortened.
