# Battle Box — owner playtest feedback #1 (must all be fixed)

## Bugs
1. **Gates don't open** when the fight starts. Find why (`openGates` in BattleBoxGame ~line 619 vs the gate geometry in ArenaGeo/ArenaTheme; check world/coordinate offsets per arena copy, block type matched, the build queue overwriting the gate after it opens, timing) and fix it. Add a visible/audible gate-drop moment (blocks vanish with particles + sound) and log a warning if any gate block wasn't found.
2. **Centre must start filled.** The 3x3 centre starts filled with neutral blocks (white concrete, or white wool in wool mode). Players must break a neutral/enemy block before they can place their own there. You can only place onto an empty centre cell. This stops a team just running in and dropping 9 blocks.
3. **Spectators:** no glass gallery. Eliminated players go into spectator mode, teleported to a good overview point above their own arena (looking down at the centre), and may fly around freely but are kept within their arena's bounds (teleport back if they leave the arena box). Remove the side galleries and windows from the build (reuse the space for the arena or make the walls solid/painted).

## Block-state correctness (also important)
4. **Fences, panes, iron bars and walls must connect** to neighbours. Compute connection states (`north/east/south/west=true|false`, walls `low|tall|none`, `up`) in a post-pass in `Grid` from the final grid for every fence, fence gate neighbour, pane, iron bars and wall, using vanilla rules: connect to same-family blocks, to full solid blocks, and (fences) to fence gates facing sideways.
5. **No carpets on slabs, stairs or other non-full blocks** (they float). Carpets only on full solid blocks; validate in the post-pass and drop offenders.
6. **Staircases must be complete and walkable:** every staircase starts with a stair at floor level + 1 on top of the floor (not sunk into the floor) and ends flush with the upper level, with no missing first or last step and no gap the player has to jump. Write a staircase helper that takes the bottom and top positions and a facing and places exactly the right number of steps, and use it everywhere. Add an offline validation that walks each staircase from bottom to top (each step: rise ≤ 1 via a stair/slab, headroom 2) and fails the render if any staircase is broken.

## Map structure (the biggest change — redesign all three layouts)
7. The real arenas are **structured into distinct sections and levels**, not open terrain with props. Right now both teams spawn staring at each other and die instantly. Redesign each theme so:
   - **No line of sight from one spawn exit to the other** (and not to the far half from the spawn exit). Each spawn opens into its own covered **forecourt/building** (porch, room, chalet, greenhouse wing, workshop room) with 2–3 exits.
   - **Lanes:** a centre lane plus two flank routes per side, separated by buildings/walls with doorways and windows, so players choose a route and can flank.
   - **Real buildings** with walls, doors, windows and roofs you can stand on — at least 2 per half, **multi-level**: ground floor + an upper floor/balcony/rooftop reached by proper stairs or ladders, giving height advantage spots that are themselves exposed.
   - **Sightline control:** the centre is visible from mid-range positions and from the high spots, but nowhere on one side can see the enemy spawn exit; long sightlines across the arena are broken by tall cover in the middle third.
   - **Mirrored** for fairness (180° rotation), sized so a sprint from spawn to centre is ~5 s.
   - Keep the themes (Greenhouse, Toy Workshop, Ski Lodge) and the sky-painted walls/frieze/hazard-striped centre; keep detail density high.
   - Verify sightlines offline: write a small ray-cast check over the grid (eye height 1.62) from each spawn exit to every point of the opposite spawn exit and to the opposite half; report blocked percentages in your notes and fix until the spawn-to-spawn lines are fully blocked.
   - Render top-down + cut-away + two angles for each theme and iterate.

## Deliver
Build the jar to `_built\`, update README, report what changed, how each layout is structured (sections/levels/lanes), the sightline-check numbers, and preview paths.
