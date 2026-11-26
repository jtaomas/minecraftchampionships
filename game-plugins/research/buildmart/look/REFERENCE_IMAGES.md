# Build Mart — reference image pack (primary handoff)

Open these files directly. Everything here is a **design reference only**: palette, density, silhouette and lighting principles. Layouts, logos, names and assets are Noxcrew's and must not be reproduced.

- Folder: `game-plugins/research/buildmart/look/frames/`
  - `wiki/` — full-resolution wiki stills (1920×1080 unless noted) and full-res crops of them.
  - `vod/` — full-resolution 1920×1080 frames from three 2.5-minute windows of one POV VOD.
  - `contact/` — contact sheets for orientation only (do not build from these).
- Source keys:
  - **W1** = `https://mcchampionship.wiki.gg/wiki/Build-Mart_(Map)` file `Build-Mart-Revised.png` (first revision, MCC 14–P24 geometry; island set).
  - **W2** = same page, `Build-Mart-Revised-Blueprint.png` (current revision, P25+; blueprint gallery on the hub floor).
  - **W3** = `https://mcchampionship.wiki.gg/wiki/Big_Sales_at_Build_Mart` files `BSABM-BlueprintGallery.png`, `BSABM-BuildArea.png`.
  - **W4** = same page, `Build-Mart-Original.png` (original 2014-era mall? no — original floating-island map, MCC 14–19 flavour; coral + flowers + sand).
  - **W5** = seasonal variants `Build-Mart-Halloween.png`, `Build-Mart-Festive.png`, `Build-Mart-Pride-2023.png`.
  - **C** = `BuildMartMap.png` (available-block catalogue graphic).
  - **V** = VOD `https://www.youtube.com/watch?v=k1LSXEr32KQ` — "Build Mart — MCC Rising 2 — Aqua Axolotls — Ginerous POV", 12:09. Windows downloaded: 0:20–2:50, 3:30–6:00, 6:05–8:35. Timestamps below are **video time**, not clip time.

Confidence: **H** = clearly visible in ≥2 sources or the source is explicit; **M** = visible once / inferred from texture; **L** = estimate.

---

## 1. Whole-mart composition (start here)

| File | Source / time | Shows | Take |
|---|---|---|---|
| `wiki/ref_hub_wide.png` | W1, full frame | The mart from the team-bay ring: floating islands overhead, dark ring of team piers, huge pale arena wall, blue sky. | The **composition formula**: dark neutral near-field → saturated themed midground islands → pale/blank far wall + sky. Three depth values, not three colours. **H** |
| `wiki/wiki_Build-Mart-Revised.png` | W1, full 1920×1080 | Same, wider, more islands and ceiling visible. Use as the "postcard" benchmark. | Ceiling is a designed layer (yellow beams + red ribbon hoops), not empty sky. Sign words are 3D and bigger than players by 3–5×. **H** |
| `vod/hub_plan_medallion_topdown.png` | V 0:36 | Top-down plan: circular inlaid medallion (green/white/red) at the centre, four brown fan discs, dark floor, team piers radiating. | Floor graphic = the hero. A ~25–30 block medallion reads from any angle; piers are "gate piers" with hazard-striped edges. **H** |
| `vod/hub_plan_team_bays_topdown.png` | V 0:44 | Full radial plan: 10 piers, team names in flat letters on the deck, stairs to the centre. | Radial/star plan; each pier identical in footprint, unique in colour + nameplate. Do **not** copy the radial arrangement; take the *idea* of one floor graphic + identical modular piers. **H** |
| `vod/team_bay_arrival_portal.png` | V 2:04 | Eye level on a pier: portal ring with red carpet ramp, team name signs on the cabinets, islands rising around. | The portal is an architectural object (ring/arch + carpet + steps), not a purple square. Arrival view is deliberately framed. **M** |
| `vod/ceiling_ribbons_island_signs.png` | V 2:08 | Ceiling ribbons + hanging `NETHER` / `BRICKS` / `LOGS` signs, island undersides at eye level. | Best single image for **canopy + island silhouettes**: ribbon arcs (stepped slabs), 3D category words, thick tapered rock undersides with hanging chunks. **H** |
| `wiki/gallery_bays_letters_blue.png` | W2 crop | Blue-painted hub floor with white linework, giant team letters, dark piers. | Typography at 12–20 blocks long on the floor; white 1-block linework on saturated ground. **H** |
| `wiki/hall_ceiling_islands.png` | W2 crop, top band | Yellow beam grid + red garland hoops + islands entering frame. | Ribbons sag; beams are thin (1–2 blocks) and bright yellow — a cheap, strong ceiling. **H** |

## 2. Blueprint gallery

| File | Source / time | Shows | Take |
|---|---|---|---|
| `wiki/gallery_floor_pattern.png` | W3 crop | Blue concrete field, white dashes / plus marks / corner brackets, lighter-blue patches. | Pattern rules: 1-block white lines, dashes 3–5 long, `+` at crossings, bracket corners, 4–6 block clear zones so it never reads as noise. **H** |
| `wiki/gallery_selected_plot.png` | W3 crop | 5×5-ish sample plots on natural ground with a 1-block stone border; the selected plot is a **solid bright-yellow frame** with white interior. Corner beacon props. | Selection = whole-frame colour + emissive rim + brighter light. Also: each diorama sits on its own *material ground* (grass/dirt/stone/snow), which sells the mini-build. **H** |
| `wiki/gallery_diorama_plinth.png` | W2 crop | Yellow selected plinth, tree diorama, stone arch diorama, gold/red corner candles, sparkle particles. | Gold/red corner candle-lantern prop (2 blocks) is the cheapest "this is interactive" cue in the whole map. Copy the *device*, not the shape. **H** |
| `wiki/gallery_team_letters.png` | W3 crop, top | Giant flat team lettering lying on the floor/edge, glass cabinets behind. | Team name as 15–20 block wordmark; letters 2–3 blocks thick. **H** |
| `vod/hub_from_dais_team_shelves.png` | V 7:19 | From the central dais: green ring, red banners, glass shelving towers with team-coloured stacks, `RED/ORANGE/YELLOW/LIME` signs, islands behind. | The hub's midground is a **ring of lit display furniture** — vertical rhythm, not a flat wall. **H** |

## 3. Resource islands (one file per theme)

| File | Source / time | Shows | Palette to take | Landmark / detail |
|---|---|---|---|---|
| `wiki/island_ores.png` | W1 crop | Ore island: flat stone deck, hazard rim, ore stacks on white tables, huge `ORES` sign + giant pickaxe icon. | stone/andesite/tuff greys; white display tops; hazard yellow/black; ore accents (emerald, diamond, gold, redstone, quartz) | Giant tool icon hanging beside the word sign; pale layered underside. **H** |
| `wiki/island_logs.png` | W1 crop | Timber yard: log stacks in tall frames on a grass deck over a grey cliff. | oak/spruce browns, grass green, grey stone, steel grey frames, white sign | Vertical lumber racks give a 6–8 block silhouette on a flat deck. **H** |
| `wiki/island_stone.png` | W5 (Pride) crop, upscaled 3× | Stone island: grey boulders with orange mineral flecks, machined display shelf, `STONE` sign. | stone, andesite, tuff, calcite; orange/copper flecks | Rough boulder silhouette vs. machined flat deck = contrast of organic/industrial. **M** (low-res source) |
| `wiki/island_bricks.png` | W1 crop | Terracotta island with a miniature brick town (towers, cottages), greenery + brick stacks on the deck, `BRICKS` sign. | terracotta/brick reds, mud brown, oak, green shrubs, white sign | A whole **village** as backdrop; functional deck only 40% of the top surface. **H** |
| `wiki/island_nether.png` | W1 crop | Nether island: crimson deck, blackstone castle spires, lava falls, floating netherrack chunks, red `NETHER` sign. | netherrack, blackstone, basalt, crimson, lava orange | Contrast: near-black architecture over red ground; small lava streams as light sources. **H** |
| `wiki/island_coral.png` | W4 crop | Coral island: orange/brown base, giant coral fans, pink coral tree, sculpted fish/dolphins, blue sign. | orange terracotta, pink/magenta coral, cyan, bone white | Theme pushed by **oversized organic props** (3–4× player height), not texture. **H** |
| `wiki/island_garden.png` | W4 crop | Flower island: white quartz ruined greenhouse/temple overgrown with green, giant flowers, azalea, dirt underside with roots. | quartz white, leaf greens, azalea pink, dirt brown | Best silhouette of the set: a single hero building + overgrowth + giant blooms. **H** |
| `wiki/island_garden_glass.png` | W5 (Festive) crop | Same greenhouse under snow; snowflake particles; blue-grey underside. | snow white, ice blue, quartz, spruce | Seasonal overlay is top-surface only (snow caps + particles). **M** |
| `wiki/island_sand.png` | W4 crop | Sand/dune island: layered sandstone mesas, palm silhouettes. | sand, sandstone, smooth sandstone, palm green | Stratified mesa edges = cheap geology. **M** |
| `wiki/island_snow.png` | W5 (Festive) crop | Snow/frost island: snow cap, icy blue glassy blocks, stone sides with copper flecks, floating ice chunks. | snow, ice, packed ice, stone, copper orange | Frost reads through **pale blue + white + grey**, never plain white. **M** |
| `vod/island_copper_harvest.png` | V 2:28 | Inside the copper island: pile of oxidised copper under a yellow/black hazard canopy. | oxidised copper greens, black floor, hazard yellow | Hazard stripes outline the *work zone* under the architecture. **H** |
| `vod/island_colour_cyan_shelves.png` | V 6:55 | Colour island interior: stacked smooth-stone slab frames holding cyan glass tanks with block stacks beneath. | cyan glass, pale stone, charcoal floor, white markings | The colour island = **20 glass tanks**, one per colour, stacked 2–3 levels. **H** |
| `vod/island_colour_orange.png` | V 5:59 | Orange section: orange-tinted glass volume, black concrete floor with white dashes, corrugated orange wall, red banners. | orange glass, black concrete, white markings, red banner | Tint the air, not only the blocks — a glass volume changes the mood instantly. **M** |
| `vod/island_colour_yellow.png` / `_lime.png` / `_green.png` / `_purple.png` | V 6:15 / 6:31 / 6:39 / 6:47 | One frame per colour bay, same module. | — | Same module, one variable. Cheap to build, reads as a system. **H** |

## 4. Team build area

| File | Source / time | Shows | Take |
|---|---|---|---|
| `wiki/buildarea_hall_architecture.png` | W3 crop, top | Hall shell: pale stone-brick columns, white trim, red fabric banners with gold bands, black ceiling trusses, gold/yellow wall bay. | Neutral expensive shell + **team colour in soft goods only** (carpet, banners). Gold as the VIP accent. **H** |
| `wiki/buildarea_plots.png` | W3 crop | Raised plots with pale stone frames, white corner blocks, inset light strips, dark plot floor with faint grid; floating name/stars/material list. | Plot = dark neutral floor + light frame + light strips. Reference on its own plinth behind. **H** |
| `vod/buildarea_plot_eyelevel.png` | V 1:48 | Eye level: pale yellow sandstone floor, white frame, red carpet, white stepped plinth, grey stone columns, black trusses. | Floor is warm pale sandstone, not grey — makes placed blocks read. **H** |
| `vod/buildarea_carpet_banners.png` | V 1:52 | Red carpet run, white stepped plinth with a live reference build, `MCC` wall letters, dark window frames, hanging lanterns. | Carpet as a circulation line; letters as a 10-block wall graphic. **H** |
| `vod/buildarea_reference_chests.png` | V 2:00 | Build name on a big yellow banner, reference build on a stepped plinth, chests along the wall, `MCC` letters. | Reference build gets a **stage**: banner + steps + light. Chests sit in a run, not scattered. **H** |
| `wiki/buildarea_gold_console.png` | W3 crop | Gold build station: gold box console on yellow legs, orange inlay, giant flat pixel crown above, chains, fenced pit below. | Rare station = new shape + new colour + new icon; never just a recolour. **M** |
| `vod/buildarea_rainbow_plots.png` | V 8:05 | Multiple plots in one hall, each with a different in-progress build, coloured concrete. | Saturation belongs to the builds; the shell stays quiet. **H** |
| `vod/buildarea_rainbow_closeup.png` | V 8:29 | Close-up of in-progress builds and plot frames. | Readability check: frames never compete with build colours. **H** |

## 5. Travel, lighting, backdrop, seasonal

| File | Source / time | Shows | Take |
|---|---|---|---|
| `wiki/central_carousel_fan.png` | W4 crop | Original central pavilion: stepped white roof rings, red/orange band, gold filigree, glass drum. | Concentric stepped rings = premium roof at low cost; gold filigree = trapdoors/grates. **M** |
| `wiki/floor_emblem_inlay.png` | W4 crop | Giant grey pixel emblems inlaid in a glossy black floor. | Inlays are 1-block-deep, matte grey on gloss black; zero lighting cost, huge identity. **M** |
| `wiki/ceiling_ribbons_signs.png` | W4 crop | Ribbon hoops + hanging category signs + coral/garden islands. | Garland hoops + sign words carry the ceiling; no solid roof needed. **H** |
| `wiki/ceiling_props_halloween.png` | W5, top band | Giant jack-o'-lantern / spider ring suspended above the hub; purple sky; hanging cobwebs. | Seasonal hero prop hangs **above** play, never in it; sky recolour does half the work. **M** |
| `wiki/hub_rainbow_floor.png` | W5 (Pride) | Hub floor reskinned to a rainbow patchwork; everything else unchanged. | Proves the hub floor is a swappable "skin" layer. **H** |
| `wiki/catalogue_all_blocks.png` | C | Official block catalogue: 9 categories, 20 colour swatches, cream background, red header, monospace caps. | Category sizes for our stall counts; graphic language for in-world signage (caps, wide tracking, flat colour blocks). **H** |
| `vod/fail_case_flat_corrugated_wall.png` | V 5:59, left-half crop | A 20+ block run of flat corrugated wall with one banner, flat floor, white dashes. | The cheap tell: detail by texture only, no silhouette change, no lighting. Fix with ribs/insets/lamps. **H** |

---

## How to use this pack (for the builder)

1. Start with `ref_hub_wide.png` + `ceiling_ribbons_island_signs.png` to set scale and the three depth values.
2. Pick **one** of the three directions in `README.md`; take its palette from `data/palettes.csv` (`set` column = `dir_a|dir_b|dir_c`).
3. For each island, copy the *device* from the island row: flat display deck + hazard rim + one hero landmark + tapered underside + word sign.
4. For the gallery, copy the *pattern rules* from `gallery_floor_pattern.png`, not the layout.
5. For the build hall, copy the *shell/soft-goods split* from `buildarea_hall_architecture.png`.
6. Reject the layout of every image in this pack. Only principles travel.
