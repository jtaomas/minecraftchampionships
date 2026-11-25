# Visitor route — Decision Dome Recreation

**There is no gameplay route and no checkpoint chain.** The world has 0 signs, 0 chests, 0 scoreboards, 0 gates and 0 map spawn/finish logic (the live spawn is the Multiverse placement described below). The only scripted interaction is the north-dais egg/glow station (2 command blocks). This file records the **visitor path** that the geometry supports, for scene dressing and for anyone re-using the model.

All coordinates are world coordinates. Centre = `(-3, 16)`. North = `-Z`.

## World arrival (H)

- **Two spawns:** the **source** `level.dat` spawn is `(0, 4, 0)` — 22 blocks below the base disk (y26), inside the pit footprint; it drops below the base and is unsafe. The **live Multiverse** spawn is `(0, 76, 0)` on lime concrete (the top of the lime pod crown), safe per A12. Use the live spawn or an explicit teleport; do not rely on the source value.
- Game mode `creative`, `allowCommands=1`, difficulty peaceful, fixed noon (`DayTime 6000`, `doDaylightCycle=false`), no damage, no mob spawning, keepInventory. This is a showcase configuration, not an arena configuration.

## Path (H geometry / M intent)

1. **Teleport to the terrace ring** at y68. The walkable ring is 4 wide, r25–28; the inner barrier rail is at r24–25, y71. There is **no outer rail**.
2. **North dais** at `x -9..3, z -21..-8`. Three 1-block steps lead up from the terrace: y68 at z≈-13, y69 at z≈-15, y70 at z≈-17. The red carpet head is 11 wide × 5 deep at y70.
3. **Egg plate** at `(-3, 71, -19)`: acacia pressure plate on the red carpet. The impulse command block is at `(-3, 69, -19)` and the chain block at `(-3, 68, -19)`, both facing down. The NBT records `LastExecution=286717`, `SuccessCount=1` for both, so the station has fired. Effect: nearest player gets **1 egg** + **Glowing 999999 s, amplifier 255**. It is a one-player prop, not a tally.
4. **Terrace circuit**: step back down to y68 and walk the ring. The 10 pod towers are at r16 and are **solid decorative shafts** — no doors, no rooms. The 8-wedge dial is 10 below the terrace (y58) and is **not accessible**; there are no stairs.
5. **West gatehouse** `x -45..-36, z 11..22, y64–73`: 10×12×10 black/white frame with a red panel; 5-long, 9-wide quartz-slab approach at y67 and a 1-block step up to the terrace at `x=-32`. Mirror **east gatehouse** at `x 30..39`.
6. **NW annex/lounge** `x -30..-20, z -15..-1, y64–75`: 11×15 glass-roofed room at y73; light-gray carpet floor at y69; 17 trophy heads, 24 banners, 2 beehives, oak leaves, 2 armor stands. Entered by 4 smooth-quartz steps at y69.
7. **SE service door** `x 19–20, z 37–38, y69–70`: 2×2 iron doors in an 8×8 frame on the outer rim; the drum behind is solid, so it is decorative.

## What a driver would have to add (no code in this dossier)

A plugin or operator must supply: a safe spawn/teleport (the live Multiverse spawn `0,76,0` exists, but it is on a pod top, not the terrace), team assignment, pod access (the build has none), a vote tally (the build has none), dial repainting (the dial is uniform quartz/gray), divider raise/lower (there are no dividers), egg clearing/refill (the station gives one egg per press), and a reset. The build supplies geometry and one egg/glow prop only.

## Not a route

The 8 wedges cannot be visited; the pit has no stair or ladder. Do not treat the 10 pod towers as team rooms: each is a 5×5 hollow shaft with 3×3 caps at y64 and y77, no door, no floor at the terrace, 0 seats.
