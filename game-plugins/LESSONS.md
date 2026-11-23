# Lessons learned (every agent reads this before building; add to it when a new mistake is found)

## Gameplay code
- **Adventure mode can't break blocks.** If players must break/place anything, keep them in SURVIVAL and cancel everything else in listeners. Never `setUseInteractedBlock(DENY)` on left-clicks.
- **Gates/doors that must open:** verify the open code uses the same coordinates (incl. per-arena-copy offset and world) and block type as the builder; make sure no queued build task re-places them afterwards; log a warning if a gate block isn't found. Opening must be visible and audible.
- **Objectives must not be trivially claimable.** Check the real rule (e.g. Battle Box centre starts filled; you break before you place).
- **Give players the real tools/items** the original game gives (check research; e.g. pickaxe + concrete, not shears + wool).
- **Spectators** = spectator game mode with a good overview teleport, confined to the arena. No glass galleries.
- **Combat:** MCC worlds use modern combat (OldCombatMechanics is disabled per world name `mcc_<id>`; create your world at plugin enable, not lazily, so it exists at startup).
- `ctx.finish()` is synchronous and `stop()` is not called after it. `stop()` must be idempotent.

## Map building
- **Structure before decoration.** Real maps have sections, buildings, levels, lanes and controlled sightlines. Open terrain with props is wrong for PvP games. Verify sightlines with a ray-cast check (no spawn-to-spawn line of sight).
- **Blocks are placed without physics**, so connection states are never auto-computed: run a post-pass that sets fence/pane/iron-bar/wall connections, and validate.
- **Staircases:** use a staircase helper (first step sits ON the floor at floor+1, last step flush with the upper level); validate by walking each one (rise <= 1 per step, 2 headroom). No missing first/last steps.
- **No carpets (or other needs-support blocks) on slabs/stairs/air.**
- Each game's maps must match that game's own scale/density profile (`MAP_PROFILES.md`, `VISUAL_REFS.md`); no generic checklist.
- Original layouts and names only; never copy official layouts, names, logos.

## Process
- Research from several sources, not just one wiki page; mark uncertain facts.
- The builder never grades its own work: a bot test and an independent reviewer check the acceptance list before the owner sees it.

## Guess less (owner rule)
- Rules, items, kits, objectives, scoring and map structure come from research (wiki, images, video step), not from assumptions. Check before you build.
- Small details research doesn't cover (exact timings, break speeds, counts) get a sensible vanilla-style default: don't spend research time on them. Put them in config and list them in the README as "defaults, not researched" so the owner can adjust after playtesting.
- Never present a default as a researched fact.
- **Keep furniture out of doorways**; register every intended exit and lane and run the route check (flood-fill from each spawn through each exit alone) before delivering.
