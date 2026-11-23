# Map & presentation brief (quality pass)

Read this together with `BRIEF.md`. The games already work. This pass is about making them **look and feel like a big, polished, televised Minecraft event tournament** — high-energy, colourful, readable, with lots of feedback — while every build and layout is our own original design.

## What "feels like MCC" means here (do this)
- **Same game structure and utility as the event format**: e.g. Sky Battle = ring of team spawn islands around a rich central island, bridging gaps, closing border + rising hazard; Battle Box = small mirrored arena, kit rooms behind gates, wool centre; Parkour Warrior = staged course with checkpoints and optional harder routes. Keep the sizes, pacing and sightlines that make those formats play well.
- **Bold, saturated, colour-coded visuals.** Team areas use the team's concrete/wool/glass/terracotta colour. Clean shapes, strong silhouettes, readable from far away. Concrete, terracotta, glazed terracotta, quartz, copper, prismarine, amethyst, sea lanterns, froglights, glass panes, banners, candles, end rods, chains, lanterns.
- **Every map has a theme** (e.g. floating ruins, candy factory, volcanic forge, frozen harbour, neon city rooftop, jungle temple, cloud kingdom, desert bazaar, clockwork tower). Theme is carried through palette, props and backdrop.
- **Detail density**: no flat slabs of one block. Use 3–5 block palettes with weighted randomness, edge trims, stairs/slabs/walls for depth, overhangs, undersides of floating islands tapered with stalactite-style noise, foliage (leaves of several types, vines, azalea, flowers, dripleaf), light sources every few blocks so nothing is dark.
- **Backdrop/skyline**: distant decorative structures, clouds (white concrete/wool blobs or snow), floating set pieces outside the play area so the world never looks empty void. Spectator area with stands overlooking the action.
- **Multiple maps per game** (aim 3, at least 2), chosen at random each game (avoid repeating the last one). Each map is a class implementing a small `ArenaMap` interface in your plugin: `name()`, `theme()`, `build(World, origin, Consumer<Runnable> queue)` / spawn points / hazard regions etc. — whatever your game needs.
- **Presentation & effects** — use `com.mcm.api.McmFx` from the core (see below) for consistency:
  - Pre-game: map name + theme title card, a short spectator flyover or overview teleport, then a 3-2-1 countdown with note-block sounds and a big "GO!".
  - In-game: titles/action bars for kills, eliminations, checkpoints, team eliminated (with team colour), round results; firework bursts in team colour; particles on pickups, power-ups, captures; sounds for everything (use vanilla `Sound`s — experience orb, note block pling/bell/chime, totem, levelup, wither spawn for big moments, etc.).
  - End: placements announced 3rd → 2nd → 1st with sounds, winners get a firework show.
  - Glowing for important targets where it suits; team-coloured leather armour; nicely named and lored items.
- Vanilla only, no resource pack. Use only Paper 1.21.1 API.

## Do not
- Do not copy Noxcrew's specific map layouts, builds, map names, logos, branded text, characters or music, even approximately. Same *game format*, our *own* designs.
- Do not download any maps, schematics or assets.
- Do not edit the core or any other game's folder (only your own `game-plugins\<YourGame>\`).

## Core helpers you can use (`com.mcm.api`, available once the core agent ships v3.2.0 — check `C:\Users\USER\Desktop\Mcc\plugin-src\com\mcm\api\`)
- `McmFx`: countdown, title cards, team-colour fireworks, celebration, placement reveal, sounds.
- `McmBuild`: tick-spread block placement queue, fill/hollow/sphere/cylinder/ellipsoid, weighted palettes, noise (value/simplex), floating island generator, clouds, dye/colour helpers for team materials, and `/mcm preview` dump support.
- If the version isn't installed yet, read the source and wait; build against `com.mcm:MinecraftMondayCore:3.2.0` once it exists in `~/.m2` (check `C:\Users\USER\.m2\repository\com\mcm\MinecraftMondayCore\`).

## Previews
- Each map must be dumpable for preview: after building a map in its world, `McmBuild.dumpRegion(world, min, max, file)` writes the blocks; the renderer script `C:\Users\USER\Desktop\Mcc\tools\render_preview.py` turns it into an isometric PNG. Add an admin command or subcommand to your plugin: `/<yourid>map build <mapName>` (builds it without starting a game) and `/<yourid>map dump <mapName>` (dumps it). Document them in your README.

## Quality bar (the owner wants genuinely high-quality, showcase-level maps)
- Think like a professional minigame build team: every map should look good in a screenshot from any angle, not just play well.
- Build in layers: base shape → terrain noise → material palette & gradients (e.g. darker at bottom, lighter at top) → trims/details (stairs, slabs, walls, fences, trapdoors, buttons as bolts) → props (barrels, crates, lamps, banners, signs-free decorations, market stalls, pipes, gears, chains) → foliage → lighting → backdrop.
- Use gradients and noise, never uniform fills. Avoid big flat unbroken surfaces; break them up with insets, pillars, arches, beams.
- Render every map with the preview tool (`tools\render_preview.py`, once it exists) from at least two angles, look at the PNGs with the Read tool, and **iterate at least twice** on anything that looks plain, blocky, dark or empty. Keep the final PNGs in `game-plugins\<YourGame>\previews\`.
- **Offline rendering (required — there is no free test server for you).** Write each map generator against a tiny local interface in your plugin, e.g. `interface MapSink { void set(int x, int y, int z, String blockData); }` where `blockData` is a vanilla block-state string like `minecraft:oak_stairs[facing=north,half=top]` (or `null`/`"minecraft:air"` for air). Keep map generators free of Bukkit/Paper classes (pure Java: your own noise/palette/shape helpers, or copy `McmBuild.Noise`'s approach). At runtime, a `BuildQueue`-backed sink converts strings with a cached `Bukkit.createBlockData(String)` and places them over ticks. Offline, a plain `main` (in `src/test/java` or a separate `tools` source dir, compiled with `javac` — no Paper on the classpath) runs the generator into a sink that writes the dump format (gzipped: header `MCMDUMP 1 sx sy sz`, then `x y z blockstate` lines relative to the region min, non-air only), then run `python C:\Users\USER\Desktop\Mcc\tools\render_preview.py dump.mcmdump.gz out.png [--angle N] [--top]`. The renderer strips `[...]` states itself.
- The renderer draws every block as a full cube, so judge shapes, colours, composition and detail density; stairs/slabs/fences will look chunkier than in-game.
- (Old note, superseded by the above:) you may also write your map generator so it can run against an in-memory block grid (a tiny `BlockSink` interface implemented by both the real `BuildQueue` and a test sink that writes the dump format) and run it from a plain `main` with the Paper API on the classpath — choose whatever works.

## Quality benchmark: professional event-server maps (added — read carefully)
The owner wants the maps to match the *quality level* of professionally built event-server maps (the kind built by dedicated build teams for big televised Minecraft events). Use that level as the benchmark, not their designs:
- You may read public wiki pages or articles about the original event's games and maps (if reachable) to understand *why* they work: scale, readability, how they guide players, how much detail they have, how themes are carried through. Take away principles only. Do not reproduce their layouts, landmarks, named themes or distinctive set pieces.
- Concrete bar every map must clear before you're done:
  0. **The game's own profile comes first.** Different games have completely different map scales and densities (e.g. a short obstacle-crossing round map is compact and simple; a giant store/warehouse building game is enormous, 100-block-tall walls packed with stuff; a race course is long and fast). Follow your game's entry in `C:\Users\USER\Desktop\Mcc\game-plugins\MAP_PROFILES.md` (scale, density, mood) when it exists; it overrides any generic rule below that doesn't fit.
  1. **An identity** — a recognisable theme and, *where it suits the game*, a landmark. Not every map needs a centrepiece.
  2. **Three depth layers**: playable foreground with props; midground structures framing the arena; background skyline/scenery at 60–200 blocks out so screenshots never show empty void at the edges.
  3. **Material richness**: ≥ 25 distinct block types per map, every large surface built from a weighted palette of 3–6 blocks with gradients (darker/mossier low, lighter high), and trims using stairs/slabs/walls/fences/trapdoors.
  4. **Detail density**: no flat run of more than ~6 blocks of a single block without a break (inset, pillar, beam, prop, pattern) in non-gameplay areas; gameplay floors stay clean and readable but get patterned borders.
  5. **Lighting**: light sources integrated into the build (lanterns on chains, glowing blocks behind panes/trapdoors, froglights in canopies), nothing dark, colour-coordinated with the theme.
  6. **Organic shapes**: noise-shaped terrain, curved paths, tapered island undersides, varied tree sizes; avoid perfect boxes except where the theme is architectural.
  7. **Gameplay readability**: team colours and interactive blocks pop against the palette; hazards look dangerous; the objective is obvious from spawn.
  8. **Spectator & arrival moment**: the first view when players spawn should look like a postcard; build that composition deliberately.
- Self-critique loop: after each render, write down (in your notes, briefly) what a professional builder would criticise — then fix it. Repeat until you'd honestly call it showcase quality. At least 2 full iterations, more if needed. Quality matters more than speed; there is no time limit.

## Done means
- Everything in `BRIEF.md` "Done means", plus: 2–3 themed maps, random rotation, the effects above, README updated with map list and themes, and a short report listing each map with a one-line description and its approximate block count.
