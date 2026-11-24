# To Get To The Other Side — owner playtest feedback #1

Owner played the maps on the review server. Overall: likes the ideas; the point of the game is **people punching each other off things**, not a parkour course. Maps should generally be **a bit bigger/longer**, except where he said a map is fine. Laundry Line and Ticket Gates: no feedback yet.

## Mechanics (all maps)
1. **Bug: punching the goal chickens does nothing on any map.** Nobody can finish by punching. Find the cause and fix; add a test path (admin command or log) that proves a punch finishes a player.
2. **Bridging blocks regenerate infinitely.** "That stack of blocks you give people — make it infinitely regenerating." The concrete stack refills (config: on/off, refill delay; default on, instant top-up to 64). This replaces the spec's "64 blocks must not be enough" resource pressure — owner decision.
3. **Fall damage / lethal falls only on Orchard Drop** (it is the drop map). Other maps stay no-damage. On Orchard Drop a missed landing must matter (see below).
4. **Punching must work on the elytra map** (owner: "you can't punch people" on Kite Festival).
5. **Decoy chickens:** maps may place fake chickens **without name tags**; real goal chickens carry a visible name tag (e.g. "Punch me!"). Punching a decoy does nothing (default; config for a small penalty later). Used on Quarry Steps first; available to any map.

## Per map
- **Dam Face** — "the best one you made", leave the layout alone. Gets the regenerating blocks.
- **Orchard Drop** — loves the idea. Problems: no fall damage, so landing in the water or not makes no difference; no water at the bottom. Wants: missing the water on a target = you fail the drop (respawn at the top); make it **harder and a lot more confusing**, especially the part at the bottom of the drop — "people don't know where to go". Slightly longer.
- **Bookshelf Run** — "cool idea I guess". Needs **much more void**: jumps over the void so that punched players fall off. Less parkour course, more places to knock people off.
- **Quarry Steps** — first found it confusing in a bad way, then decided he likes it. Wants the **top made deliberately confusing/cluttered** ("put a bunch of stuff everywhere") and **decoy chickens** there. A bit bigger.
- **Kite Festival** — biggest problem: finished in ~10 s; can't punch. Elytra map must be **really big and really long**, **lots of fireworks**, **much smaller holes/rings**, a lot more difficult.
- **Frozen Canal** — "such a good idea"; make it **a lot longer**.

## Research instruction for the elytra and boat maps
Owner: don't design these from scratch. There are many publicly available elytra-course and ice-boat-race maps; do **extensive research** on them and take heavy inspiration (course length, ring sizes and spacing, turn types, track width, hazards, how they stay readable at speed). Not MCC's maps specifically; any good public maps. Use them as templates for structure and scale, then build ours with our own generator and names. If a specific map with a clear free-to-use licence would be worth importing as a file, report it (name, source, licence, size) for the owner to approve — do not download map files without that approval.
