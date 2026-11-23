# McmBingo - Bingo but Fast / Lockout Bingo

Two MinecraftMondayCore games in one plugin (`McmBingo.jar`):

| id | `/mcm play <id>` | name | time limit |
|----|------------------|------|------------|
| `bingo` | `/mcm play bingo` | Bingo but Fast | 10 min |
| `lockout` | `/mcm play lockout` | Lockout Bingo | 12 min, or earlier once all 25 squares are claimed |

## How it plays
- Each game generates a new survival world `mcc_bingo` with a random seed. The old `mcc_bingo` folder is unloaded and deleted first, and only that exact folder is ever deleted. There is a 1200-block world border around spawn.
- Each side (team or solo player) starts at its own point near spawn. Everyone gets an iron pickaxe, axe and shovel (Efficiency II), 12 steak, permanent Speed I and a **Bingo Card** (knowledge book).
- After a 5 s countdown, everyone plays normal survival. Every side has the same 5x5 card of items, drawn from a pool of about 110 early-game items in three difficulty tiers. A bingo card has 10 easy, 11 medium and 4 hard items; a lockout card has 8, 12 and 5. Near-duplicate items (bucket, water bucket and milk bucket, for example) never appear on the same card. "Any X" squares accept every variant, such as any boat, bed, wool, flower or sign.
- An item counts as soon as it is in a side member's inventory (checked every 0.5 s).
- Right-click the card to open the card menu, which shows each square's status. Its bottom row has teleports to your start point and to teammates (45 s cooldown).
- Rules: no PvP, keep inventory, hunger on, mobs on, no phantoms, time locked to day, clear weather, portals disabled. Respawn is at your side's start point.

## Scoring (given to every online member of the side)
**Bingo but Fast**
- Square, by completion order across sides: 1st +6, 2nd +5, 3rd +4, 4th +3, 5th and later +2
- Line (row, column or diagonal; 12 in total): +12 each for your first 4, then +6
- Full card: +30. The game ends early once every remaining side has a full card.

**Lockout Bingo**
- The first side to obtain an item claims that square: +8. Nobody else can get it after that.
- Owning all 5 squares of a line: +15
- Final placement by squares claimed: 1st +20, 2nd +12, 3rd +6 (ties share the higher place)

## Known limitations
- Items are detected in inventories only, not in chests. Handing an item to another side also counts for that side.
- The world is generated synchronously at game start, which can freeze the server for a few seconds. There is no pre-generation.
- If Windows still has region files locked right after unload, deleting the old world fails. It is retried at the next game start.
- Late joiners spectate inside the bingo world.

## Admin testing
- `/mcm skipwait` ends the start countdown (GO within half a second).
- `/mcm skip` does nothing: Bingo is a single round.
- `/mcm fast on`: the start countdown is at most 1 s. The game length is not shortened.
