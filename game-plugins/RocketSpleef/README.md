# Rocket Spleef Rush (McmRocketSpleef)

Game id: `rocketspleef` — run with `/mcm play rocketspleef`.

## Rules & Mechanics
- **Kit:**
  - **Unbreakable Elytra** equipped in the chestplate slot (`allow-double-jump: false` by default so Spacebar always deploys Elytra cleanly).
  - **Rocket Launcher** (`BLAZE_ROD`, right-click): fires straight-flying rockets with swept raycast hit detection (never clips through 1-block floors). Blasts a 2.7-block sphere out of the arena (no drops) and knocks back players within 4.5 blocks (including the shooter for rocket-jumping).
  - **Updrafts** (`FEATHER`, 2 charges per round): right-click to launch upward with a forward glide boost.
- **4 Floating Void Maps** in `mcc_rocketspleef` (created at `onEnable` with void generator), rotating across 3 rounds:
  - `journey_isles` — Floating archipelago of rocky-underside sky-islands, custom trees, bridges, high sky-ring, and mid-round spawning sky-pads.
  - `sky_temple` — Sandstone & prismarine temple complex with central ziggurat shrine, 4 corner domed pavilions, and stepped aqueduct bridges.
  - `santorini_cliffs` — Whitewashed quartz & calcite Mediterranean cliffs topped with iconic blue domes and colonnade arches.
  - `overlode_core` — Dark deepslate sky-foundry with glowing neon conduits, octagonal catwalks, and a central energy core.
- **Outer-to-Inner Crumbling & Overtime:** In the final 40s of active play and rapidly during overtime, outer arena blocks turn red (`RED_CONCRETE`) for 1 second before vanishing inward.
- **Elimination:** Falling below `elim-y` (default `64`) eliminates you into spectator mode until the next round.

## Scoring (per round)
- **Survival:** 18-point round pool shared among survivors each time a player falls.
- **Knock-off:** +5 to the last enemy whose rocket blast hit the victim within 10 s.
- **Last side standing:** +12 to each survivor (+6 if multiple sides survive time limit).
- **Late elimination:** +5 / +3 / +2 for the last three players to fall.

## Admin Testing (Core 3.2.0)
- `/mcm fast on|off`: cuts countdown & intermission waits to <= 1 s.
- `/mcm skipwait`: skips the current countdown or intermission.
- `/mcm skip`: skips the current round and advances to the next (or finishes after round 3).
- `/mcm map <id>`: forces the next round to use `journey_isles`, `sky_temple`, `santorini_cliffs`, or `overlode_core`.
