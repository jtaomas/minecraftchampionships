# Meltdown (McmMeltdown)

Start it with `/mcm play meltdown`. The plugin name is `McmMeltdown`, it needs `MinecraftMondayCore`, and it builds its arena in the world `mcc_meltdown` (a void world created on first start).

## How it plays
- The facility is a 7x7 grid of rooms with a glass roof. The walls are packed ice, snow and blue ice, and the floors are quartz and concrete. Doorways connect the rooms as a random maze plus extra openings, so every room can be reached on foot. The internal walls can also be mined. The quartz pillars, the outer wall, the floor and the roof cannot.
- Each side starts on its own coloured pad in a room on the outer ring. There is a 10-second countdown and players can't move during it.
- The facility freezes from the outside in. A room first gets a **warning**: a title, a siren, a frosted ceiling and snow creeping in. After 20 s it **freezes**: the ceiling turns blue, the floor slowly turns to ice and powder snow, and its coin crates are sealed in ice. Anyone in a frozen room gets Slowness and the frost overlay, and loses ½ heart/s, then 1 heart/s after 15 s, then 2 hearts/s after 35 s. Health does not regenerate naturally.
  - Outer ring: rooms freeze one by one between 80 s and 170 s.
  - Middle ring: between 190 s and 270 s.
  - Inner ring: between 290 s and 360 s.
  - The warm core freezes at 395 s.
  - Once a room has been frozen for 60 s it reaches "absolute zero" and heaters and warmers stop working there. The game lasts 8:00 at most.
- Kit:
  - **Ice Pick** (Efficiency III): mines walls and coin crates.
  - **Frost Bow** (Infinity, 1 s cooldown): one hit freezes an enemy in place. A frozen player shatters (is eliminated) after 40 s unless thawed. If every living member of a side is frozen, the whole side is eliminated.
  - **Heater** (a campfire you place): warms a 4.5-block radius for 25 s, for your own side only.
    - It protects teammates from the cold and heals ½ heart every 2 s.
    - It thaws frozen teammates. Thawing takes 5 s with one heater, and more heaters thaw faster.
    - One punch from an enemy breaks it, and you can punch your own to pick it up.
    - After a heater ends or breaks, there is a 10 s cooldown before you get the item back.
  - **2 Hand Warmers** (coal): right-click to heal 2 hearts and get 8 s of cold immunity. Crates have a 35% chance to drop another one.
  - **Facility Map** (a compass pointing to the core): right-click to see a coloured grid of the room states in chat.
  - Team-coloured leather chestplate and leggings.
- The action bar shows your room, its state, your distance to the core and your heater status. The sidebar shows players alive, sides alive, which ring is freezing, rooms frozen, the time to the next freeze and the coin crates left.

## Scoring
| Event | Points |
|---|---|
| Break a coin crate (about 130 per game: 2 per outer room, 3 per other room, 12 in the core) | +2 to the breaker |
| Freeze an enemy | +3 to the shooter |
| An enemy you froze is eliminated without being thawed (shatter, team wipe or cold) | +5 to the shooter |
| Survival: another player is eliminated | +1 to every player still alive |
| Placement of a side (1st, 2nd, 3rd, ...) | 25 / 18 / 14 / 10 / 7 / 5 / 4 / 3 / 2 per member, through `addSidePoints` |

- Placement is decided when a side is wiped out.
- If several sides are still alive at the time limit, they are ranked by unfrozen survivors, then by total survivors, then by total health. Tied sides share a place.
- With several sides, the game ends as soon as one side is left. With one side (solo testing), it ends when that side dies or time runs out.

## Known limitations
- There is a single round, not MCC's three. There is no team-shared telepickaxe (everyone gets a pickaxe), and there are no locked rooms or capture points.
- Rooms do not seal their doors when they freeze. The escalating cold damage forces players out instead.
- Thawing does not take points back from the shooter. The freeze bonus is paid in two parts instead: +3 on the freeze and +5 only if the victim is eliminated.
- Players who disconnect are eliminated, with no survival points for the others. If they come back during the game, they become spectators (the core's default).
- The arena is rebuilt from scratch at every start, which takes about 1 second. Between games only heaters and entities are cleared.
