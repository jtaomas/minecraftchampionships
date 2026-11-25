# Route / access — A6c arenas

One paragraph per arena. These are **access and circulation summaries**, not deep-dive routes; Sky Battle (`MG-SB-01`) and Parkour Tag (`MG-PT-01`) are A6a/A6b and are excluded. Coordinates are absolute source-world coordinates. **H** = exact block/command; **M** = inference.

## MG-HUB-01 — crown hub

World spawn is `-196 24 927` on the central netherite pedestal; the player steps down to the ring floor at y21 and can walk the eight colour bays. The four working game pads are on a **separate south deck** at y8–10 (orange `-212 10 975` → Parkour Tag, yellow `-208 10 970` → Sky Battle, lime `-205 10 970` → Super Clash, blue `-201 10 976` → Sky Race hub). There is **no block path** between the crown and that deck: `z 951..960` is empty at every y, and the only deck→crown links are teleports/items back to `-196 24 927`. **H** blocks/commands; **M** that the intended access is a plugin/creative teleport. This is the main circulation risk.

## MG-SC-01 — Super Clash

Entry `5000 21 0`; heroes spawn `4972 29 -15` (east, high) and villains `4926 20 -15` (west, low). Both team walls have a mirrored five-class pad row at `x5005` (villains) and `x5007` (heroes), `y21`, `z -17,-19,-21,-23,-25`; standing on a pad at level 16 sets XP 17–21, and the class chain grants the kit. The ready sign is at `5002 23 -23` (buttons at `5001 24 -24` and `5003 23 -24`); the 3-2-1 countdown, kill counters, win tellraw/fireworks and reset follow. The arena is an **asymmetric** glass box, not a mirrored two-side map. **H** commands/signs.

## MG-SR-HUB-01 — Sky Race level select

Entry `20000 41 20000` inside the brick kiosk. Six powder pads sit at `x20010, y42, z19995/19997/19999/20001/20003/20005`; red (`level 100 → 101`, then `101 → 21000 31 20000`) and orange (`100 → 103`, then `103 → 21000 31 21000`) are wired. Yellow/green/blue/purple pads exist but have no commands, so there is no route to those four levels. **H** commands.

## MG-SR-RED-01 — Sky Race Red

Start inside the black tower at `21000 31 20000` (level 101 lobby → level 102 race). The course is a **linear climb east** from the start tower (x20991–21003, y13–35) through red-glass gate/beam structures to y82, then down to the red/white/gold finish basket at `x21055–21065, y53–57, z19995–20005`; finish on the raw-gold floor at `21060 55 20000`. There are **no checkpoint gates**: the red-glass structures are visual obstacles, and the only `if block` checks are at the finish. **H** commands/blocks. Moving the course requires rebasing every coordinate.

## MG-SR-ORG-01 — Sky Race Orange

Start inside the same black tower type at `21000 31 21000` (level 103 lobby → level 104 race). The route is the **low, flat variant**: orange gate/platform structures run y13–54, with 24 cobwebs and no chains, then the finish basket at `21063 52 21000` (raw gold at y51). It shares the command skeleton and start/finish language with Red but **not** the upper layout. No checkpoint gates. **H** commands/blocks.

## MG-CMD-01 — command room

No player route: the room has no spawn, pad or game entry. It is a **service platform** at `x926..1004, y9..23, z-18..16`; the player would enter only by teleport/creative. The rows are organised as a circuit board: team/level logic, loadout, 93 loot inserts, 110 wool-clear fills, timer/border, win checks, then a separate NE test rig and SE ladder rail. Access for maintenance is the ladder rail; gameplay coordinates point at the Sky Battle arena ~1,000 blocks away. **H** blocks/commands; **M** maintenance access.

## MG-REB-01 — Rebecca kiosk

Entry `-50000 100 -50000` in the 11×11 kiosk; the red-concrete-powder pad is at `-50003 99 -49998`. Standing on it at level 200 sets level 201; standing on it at 201 teleports to `-50000 100 -51000` (the Rebecca course). This is a **one-way** entrance: there is no return command in the kiosk, despite the “Lobby Teleporter” sign. **H** commands/signs.

## MG-REB-02 — Rebecca course

Start at `-50000 100 -51000`; a white concrete beam leads east to stepping stones, a 25×9 lava field, and a staircase of 2×2 waxed-copper pads rising y101→127, then a yellow-carpet finish at `-49874 124 -50985`. The only command is a jump-boost check at `-49916 130 -51001` for `level=1` on waxed copper; the kiosk leaves the player at level 201, so the pad never triggers as shipped. There is no finish detection, timer, respawn or reset. **H** commands; **M** intended parkour/race.
