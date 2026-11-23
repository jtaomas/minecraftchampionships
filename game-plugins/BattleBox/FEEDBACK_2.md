# Battle Box — owner playtest feedback #2

Owner verdict: much better, "getting somewhere". Fix these.

## 1. Centre blocks break instantly — they should mine like normal blocks
Remove the forced instant-break. **Don't guess the right speed (LESSONS.md "No guessing"):** run the video step in `..\PIPELINE.md` on real Battle Box footage (post-concrete era and the wool era) and time how long a centre block takes to break and what tool/enchant the player holds (frame-step around a break: `v.currentTime` in 0.1 s steps). Then match it with a vanilla tool/enchant combination (e.g. plain pickaxe vs concrete, or shears vs wool) and make it a config value. Record the evidence (video id + timestamps) in `..\research\battlebox_video.md`.

## 2. Bug: Ski Lodge — the left and right exits from the spawn building are blocked
You can only leave through the middle. Fix, and add an offline **route check** for every theme: flood-fill walkability (walk, 1-block step-ups via stairs/slabs, jumps of 1) from each spawn through every intended exit to the centre and into each lane; fail the render if any intended exit or lane is blocked.

## 3. The three maps are the same layout reskinned — make each one structurally unique
Owner: "every single one is a centrepiece with ~9 buildings scattered around it … same map, just reskins … there should be more experimental elements — maybe a tower people can shoot from here, maybe nothing there, something else over there."
- **Research first (video + image step, PIPELINE.md):** look at 6–8 different real Battle Box arenas (wiki map pages, image search, gameplay videos) and write, in `..\research\battlebox_layouts.md`, how they **differ from each other structurally**: e.g. open middle vs. blocked middle, one dominant tower/high ground, water or lava dividing the map, a sunken or raised centre, a central building you fight inside, long flank corridors, bridges, asymmetric left/right flanks, how spawn rooms/exits vary. Archetypes and principles only; no reproduction of any one official layout, no official names.
- **Then redesign so each theme has its own layout concept** — three clearly different archetypes, for example (final choice should follow the research):
  - *Greenhouse*: the centre is inside a big glass central hall you enter through doors on four sides; outside it, hedge-maze flanks; one tall palm-house tower on each side as a risky shooting perch.
  - *Toy Workshop*: an open middle crossed by an elevated toy-train viaduct (a bridge level over the centre) with the centre underneath; one flank is a tight shelf corridor, the other an open play-mat with low cover — left and right flanks are deliberately different.
  - *Ski Lodge*: a frozen river/ravine splits the map with two bridges and a lower ice level; the centre sits on a mid-river island; a ski-lift tower gives one exposed high spot; a big lodge on one diagonal, open slope on the other.
- **Vary the spawn area too** ("experiment a bit more with the starting"): different spawn-building shapes and exit arrangements per map (e.g. upstairs exit onto a balcony + ground exits; a tunnel exit; a wide porch), keeping the no-spawn-to-spawn-sightline rule.
- Fairness: the two **teams' halves** stay a 180° rotation of each other. Within one half, left and right should differ (not mirrored), and no two themes may share a skeleton. Empty space is allowed where it serves the design ("maybe there's nothing here").
- Keep everything from feedback #1 (gates, centre starts filled, spectators, connections, staircase helper, sightline check). Re-run all validators + the new route check. Render top-down, floorplan, cut-away and 2 angles per theme; look at them side by side and confirm the three floorplans are visibly different; iterate.

## Deliver
Jar to `_built\`, README updated, short report: evidence for the break speed, the layout research summary, each map's concept and how it differs, validator/route/sightline numbers, preview paths.
