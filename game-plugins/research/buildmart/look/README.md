# Build Mart — map look research

**Agent:** look (visual language). **Status:** first pass, research only. Nothing outside `research/buildmart/look/` was edited.

**Primary handoff is the image pack:** `frames/` + `REFERENCE_IMAGES.md` (54 full-resolution stills — 36 wiki-derived and 18 VOD — with "what to take" per image, plus provenance/timestamps; 45 are catalogued individually, the rest are the source wiki frames). This file is the analysis on top of it.

---

## 1. Scope and method

Goal: document the **visual language** of the current MCC Build Mart map so the rebuild can match its quality without copying its layout.

- Read the shared brief, `game-plugins/BuildMart/README.md`, the current arena generator, `MAP_BRIEF.md`, `MAP_PROFILES.md` (Build Mart) and `LOBBY_DOME_BRIEF.md` (quality bar).
- Read the two relevant wiki pages for structure and terminology, then pulled **12 wiki stills at source resolution** through the MediaWiki API (no screenshots of screenshots).
- Captured **three 2.5-minute VOD windows** (7.5 min total) from one POV: `youtube.com/watch?v=k1LSXEr32KQ` — MCC Rising 2, Ginerous POV, 12:09. Windows: 0:20–2:50 (hub + build hall + portal), 3:30–6:00 (build hall detail + entrance), 6:05–8:35 (colour islands + hub dais).
- Extracted 18 full-resolution frames at chosen seconds; made contact sheets for orientation; cropped wiki stills losslessly per island/area.
- Wrote palettes as approximate sRGB for the builder's reference (`data/palettes.csv`), and three original theme directions (§7).

What this dossier is **not**: a layout spec. Positions, counts and travel timings belong to the map agent. Every image here is a reference for *palette, silhouette, density, light and signage scale* only.

## 2. Sources

| # | Source | What it gives | Confidence |
|---|---|---|---|
| S1 | `mcchampionship.wiki.gg/wiki/Build-Mart_(Map)` + files `Build-Mart-Revised.png`, `-Revised-Blueprint.png`, `-Original.png`, seasonal variants | map structure, island categories, elytra/fan/launch pads, portal, visual appearance | **H** |
| S2 | `mcchampionship.wiki.gg/wiki/Big_Sales_at_Build_Mart` + `BSABM-BlueprintGallery.png`, `BSABM-BuildArea.png` | gallery rules, build area contents (5 chests, stonecutters, 2 smelting areas), gold build, plot appearance | **H** |
| S3 | `BuildMartMap.png` (official "available blocks" graphic) | category list and sizes, graphic identity | **H** |
| S4 | VOD `k1LSXEr32KQ` (MCC Rising 2, 2023), three windows | hub plan, portal, island interiors, build hall at eye level, travel-in-progress | **H** for geometry, **M** for anything seasonal |
| S5 | the VOD's own HUD | confirms the full team roster and the live scoreboard layout; event-specific timer not used for design | **M** |
| S6 | `game-plugins/BuildMart` source (read only) | the current code-built ring, for the critique in §6 | **H** |
| S7 | `MAP_PROFILES.md` line 198–206 | accepted scale/density envelope (200–300 across, 80–100+ walls) | **M** |

## 3. Findings

Each finding: **what** → **evidence** → **confidence**.

1. **The mart is a radial hub, not a hall.** A circular floor plaza with a giant inlaid medallion, four large circular grilles set into the floor (fan intakes or pads — form not confirmed), and ten identical team piers radiating outward. Floating resource islands sit above and around the ring; the whole thing is open to a bright sky but visually enclosed by a very tall pale corrugated wall. (S1, S4 0:36 / 0:44 / 2:08) **H** for composition, **M** for the grilles' function.
2. **Floor graphics do the wayfinding.** The hub has no direction signs. It has a 30–40-block medallion and, on the current revision, a blueprint field of white lines covering the whole plaza. Team identity is a 12–20-block wordmark laid flat on the deck edge. (S4 0:36, 0:44; S1 W2) **H**
3. **Utility language is consistent and repeated.** Yellow/black hazard striping marks every edge, drop and work zone: pier rims, island rims, the copper harvest canopy, the departure pad. Grey steel frames, red buttons, glass display cases and red banners repeat everywhere. This repetition is what makes a 200-block map read as one product. (S1 W1/W2; S4 2:04, 2:28) **H**
4. **Every material is displayed as itself, in quantity.** Team piers hold glass-fronted shelving towers stacked with team-coloured blocks, several layers deep. Islands hold 1-block-high stacks on white tables, 3–5 wide. Nothing is represented by an icon at close range; the block *is* the label. (S1 W2; S4 7:19) **H**
5. **Signage is architecture.** Category words are 3D block letters, roughly 4–6 blocks tall and 18–28 long, mounted on the island landmark or hung from the ceiling ring. White letters with a black shadow side; one word per island, plus one oversized theme icon (pickaxe, crown, brick). (S1 W1/W2; S4 2:08) **H** for style, **M** for size.
6. **The ceiling is a designed layer.** A thin yellow steel grid plus red-and-cream ribbon hoops spans the whole mart; category signs hang below it. In seasonal versions a giant themed prop (pumpkin/spider ring) replaces the ribbon centre. No solid roof is needed. (S1 W1/W2, W5; S4 2:08) **H**
7. **Islands are dioramas, not platforms.** Each has: a flat working deck with a hazard rim; one themed landmark building or oversized prop; a word sign; and a thick, tapered rock underside with two or three strata and 3–8 dangling chunks. (S1 island crops; S4 2:08) **H**
8. **Island palettes are monochrome-dominant with two accents.** Bricks = terracotta + brick + oak, one green accent. Nether = netherrack + blackstone + crimson, lava as the only light. Logs = brown + green + grey steel. Frost = white + pale blue + grey. Only the colour island is polychrome, and it is deliberately a system of identical glass tanks. (S1 crops; S4 6:15–6:55) **H**
9. **Undersides carry the "floating" illusion.** Estimated 8–14 blocks deep, tapering to a point, with strata changes, moss/roots/lava drips and small detached chunks falling away. Pale calcite/diorite for ore, packed mud for bricks, dirt for garden, dark prismarine for coral. (S1 crops; S4 2:08) **M**
10. **Lighting is warm, practical and directional.** Lanterns and chandeliers in the build hall; sea-lantern inlays in plot frames; glowing glass volumes on the colour island; lava on the nether island. Sky light is bright, but every interior has a warm source at 6–10 block spacing so nothing reads flat. (S2 crops, S4 1:48–2:00, 6:55) **M**
11. **The Blueprint Gallery is a drawing, not a showroom.** Blue concrete field, 1-block white dashes / plus signs / corner brackets, occasional pale-blue patches. Sample builds sit on 5×5 plinths with their own natural ground and a 1-block stone border; a gold/red corner beacon marks each. The selected plot turns its **entire frame** bright yellow with a pale interior and a sparkle cue. (S2 crop `gallery_*`; S1 W2) **H**
12. **The build hall is a neutral expensive shell with team-coloured soft goods.** Pale stone-brick columns, white trim, black trusses, red fabric banners with gold bands, red carpet, pale sandstone floor, white-framed raised plots with inset light strips. Team colour appears in carpet/banners/nameplate, not in the architecture. (S2 `BSABM-BuildArea.png`; S4 1:48–2:00) **H**
13. **References are staged.** The reference build sits on a stepped white plinth behind the plot with a big coloured name banner, floating stars and a material list whose bullets are coloured like the item. (S2 crop; S4 2:00) **H**
14. **The rare build gets a new object, not a recolour.** The gold station is a gold box console on yellow legs with an orange inlay, a giant flat pixel crown above and a fenced, hazard-marked pit. Completely different silhouette from everything else in the hall. (S2 crop `buildarea_gold_console.png`) **M**
15. **Travel is architectural.** Central fans/turbines in the floor, launch and speed pads with chevrons, no railings anywhere, elytra always equipped. Falling is a designed risk that costs materials. (S1 prose; S4 2:04) **H** for existence, **M** for form.
16. **The portal is a framed object.** A ring/arch with a carpet ramp and steps leading into the build hall, set into the team pier. Arrival is staged with a framed view of the mart. (S4 2:04) **M**
17. **Seasonal skins are cheap because the layers are separable.** Pride changes the hub floor only. Halloween changes sky, ceiling props, particles and island tops. Festive changes snow caps and particles. Geometry, signage and team piers never change. (S1 W5) **H**
18. **Contrast strategy:** saturated team colours and raw material blocks sit on near-black or neutral grey; signage is white on saturated ground; gold is reserved for one or two objects. Nothing important is grey-on-grey. (S1 W1/W2; S4) **H**
19. **Scale is deliberately non-human.** Words are 4–6× player height; islands 30–45 wide; trusses 40+ above the floor. Small props (candles, buttons, chains) exist, but the readable layer is huge. This is what makes screenshots look like an arena rather than a build. (S1/S4, pixel-scaled; ±30%) **M**
20. **Cheap tells are consistent too:** unbroken 20+ block single-material walls, corrugated panel used as the only detail, flat floors with nothing but stripes, and text-only labels. The real map has these in places (see `vod/fail_case_flat_corrugated_wall.png`) — they are the parts that look dated. (S4 5:59, S1 W5) **H**

## 4. Palette hierarchy (what to copy)

The map never uses more than three values at once on a large surface:

- **60% base** — a dark or pale neutral that everything else sits on: near-black concrete/deepslate (hub), pale stone/quartz (build hall), or the island's own material family (terracotta, netherrack, stone).
- **30% structure** — one mid-tone used for frames, tables, racks, trusses: steel grey, stripped wood, prismarine, brass. Repeated identically across the map.
- **10% signal** — saturated colour with a job: team colour (identity), hazard yellow (edge/danger), gold (rare/VIP), red (banner/carpet/circulation), white (signage and linework). Never decorative-only.

Additional rules that hold across every image:

- **Saturation budget:** at most two saturated hues per island, and the hub floor owns the third. The build hall gets **zero** saturated architecture so placed builds read.
- **Value ladder:** dark near-field → mid-value islands → pale far wall/sky. Never dark-on-dark at distance.
- **Textures are not detail.** Every "premium" surface has a silhouette break every ≤6 blocks (inset, rib, frame, lamp, prop). The cheap surfaces are the ones where the only change is texture.
- **Light is a material.** One warm source per 8–12 blocks at eye level; glow blocks embedded in rims, not scattered.
- **Signage colour:** white letters on the island's saturated ground, with a black shadow side and a 1-block standoff from the wall so it reads in silhouette.

## 5. Premium vs cheap

| Looks premium | Why | Looks cheap | Fix |
|---|---|---|---|
| Stepped/tapered silhouette (roof rings, island undersides, stepped plinths) | catches light, reads at distance | a box with a flat top | add a 2–4 step fascia and a tapered underside |
| One hero object per island (village, castle, greenhouse, giant tool) | gives each area a name | an even spread of small props | delete half the props, scale one up 3× |
| Materials shown as stacks 3+ deep | proves the material, reads as stock | single blocks on pedestals | stacks of 3–12, arranged in a grid |
| Glass cases, steel frames, brass trim | "expensive retail" language | bare shelves / floating text | frame every display with 1 block of trim |
| Warm light inside the object | depth, invitation | uniform sky light | lamps in canopies, rims and undersides |
| Repeated utility language (hazard, steel, red) | one product, not ten sets | every area invents its own trim | define a 4-material "kit" and reuse it |
| Floor as graphic (medallion, blueprint lines) | free identity, zero collision | blank floor with stripes | 1-block inlay graphic at 30+ block scale |
| Team colour as soft goods | architecture stays coherent | hall painted team colour | carpet, banner, nameplate, light tint only |
| Oversized typography | legible while flying | floating name tags | 3D letters, 4–6 blocks tall |

## 6. The current code-built ring: what fails

Read from `BuildMartGame.java` (line numbers current at time of writing):

1. **One plane.** `FLOOR_Y = 64` (L48); the mart floor, every team platform, every path and both plots sit on it. No islands, no undersides, no verticality — the single biggest reason it reads as a diagram instead of a place.
2. **Too small.** The mart is `51 × 27` (`MART_X 25`, `MART_Z 13`, L57) against an accepted envelope of ~200–300 across (`MAP_PROFILES` L201). Team platforms sit only 48+ blocks out, so the whole map is one glance.
3. **Two-tone floor.** A checker of `SMOOTH_STONE` / `POLISHED_ANDESITE` with a `POLISHED_BLACKSTONE` edge and four `SEA_LANTERN` corners (L326–342). No graphic, no inlay, no light design.
4. **~13 structural block types** for the whole arena (floor, stalls, shops, platforms, paths). The brief's bar is ≥25 per map with 3–6-block weighted palettes. There are no gradients and no trims beyond one `STONE_BRICK_SLAB`.
5. **Copy-paste stalls.** A 6-block grid of 27 identical 4×4 plinths in `STRIPPED_SPRUCE_WOOD` / `SPRUCE_PLANKS` (L354–390), each labelled by a floating `TextDisplay`. No categories, no island themes, no canopy, no racks, no glass, no stock depth.
6. **Signage is text, not architecture.** One floating "BUILD MART" label 11 blocks up (L343). No 3D letters, no icons, no hanging signs. At flying distance it is unreadable and invisible in a screenshot.
7. **No backdrop.** Void world: nothing outside the ring, no wall, no skyline, no clouds, no ceiling. Every wide shot ends in empty sky.
8. **Team areas are coloured rings.** A 17×11 `SMOOTH_STONE` platform with a team-concrete edge, a quartz ring for the pedestal, a `SEA_LANTERN` spawn pad (L400–416) and a text label (L439–440). No team identity, no seating, no portal, no reference staging, no lighting.
9. **Travel is a footpath.** 3-wide `POLISHED_ANDESITE` with `STONE_BRICK_SLAB` edges (L427–432). No fan, launch pad, speed boost, beacon or landmark, so movement has no drama and no risk.
10. **No gallery.** The target build is a single 5×5 copy on a pedestal beside one plot; there is no sample diorama, no star display object, no selection state, no rotation. The gallery is currently the game's biggest missing *visual* system, not just a missing feature.

Implication: the rebuild is not a recolour. It needs a new vertical composition (hub + two-tier islands + enclosed shell), a map-wide trim kit, and code generators for: island (taper + strata + rim + deck), sign (block letters + icon), display furniture (table/rack/glass tank/case), and plot (frame + light strip + reference stage).

## 7. Three original directions

All three are new identities, not recolours of the mall/warehouse or the open-sky islands. Each is chosen to make the *mechanics* readable: a central landmark you can see from any island, islands that differ in silhouette as well as colour, and a build hall that stays neutral so player builds are the colour.

### Direction A — **The Gilded Orrery** (night archive)

- **Mood:** after-hours museum of a clockwork observatory. Quiet, warm lamps against deep indigo; the only bright things are brass and the material displays.
- **Palette:** deepslate/blackstone navy-black base (60%) · brass gold + waxed copper + dark oak (30%) · parchment (calcite/smooth sandstone) + amethyst + cyan glow (10%). Full rows in `palettes.csv` (`dir_a`).
- **Silhouette:** a 60-block glass-and-brass dome over a circular reading room, with a giant armillary sphere at the centre and eight "moons" (islands) hung on visible brass arcs at two heights. From any island you see the sphere through the dome ribs.
- **Hero landmark:** a **nested armillary sphere** — three gimbal rings (r≈8/12/16) of gold and waxed copper around a glowing sea-lantern core, on a stepped blackstone plinth. Slowly rotating ring segments give the map its only motion at rest; a beacon column rises from the core.
- **Islands (8):** *Geode* (amethyst/calcite/deepslate, giant cut geode), *Lumber* (dark oak/mangrove, a dead tree through a collapsed shelf), *Masonry* (tuff/stone brick, a broken arch and a scale model of the dome), *Pigment* (concrete/terracotta, a paint-pot wall), *Ember* (blackstone/lava, a brass-cased furnace), *Verdant* (moss/azalea, a glass bell jar), *Verdigris* (oxidised copper, a giant telescope/clock face), *Frost* (snow/ice, a silver observatory).
- **Gallery:** seven "reading desks" on a star-chart floor — gold constellation lines between them, each desk a 5×5 diorama under a brass lamp; the selected desk raises and lights its rim.
- **Build hall:** archive workshop — calcite plaster, polished deepslate floor, dark-oak casework, brass lamp rails, team carpet + banner, gold rare-build case.
- **Travel:** updraft through the sphere (fan disguised as the orrery's core), brass launch plates on the arcs, constellation light lines on the floor to each pad.
- **Why it is not MCC:** MCC is a bright retail carnival in open sky. This is a dark, enclosed, warm-lit instrument — different value structure, different silhouette, different light story.

### Direction B — **Tideglass Bazaar** (sunken conservatory market)

- **Mood:** a Victorian glasshouse sunk in a bright lagoon. Airy, clean, shafts of light; the only loud colours are coral and team cloth.
- **Palette:** prismarine/verdigris + cream limestone/calcite (60%) · oxidised copper + jungle teak (30%) · coral pink/red, sea-glass cyan, team wool (10%). Rows in `palettes.csv` (`dir_b`).
- **Silhouette:** a low glass dome ringed by looping aqueduct bridges; a 40-block **tideglass column** (water in a glass cylinder with rising bubbles and a prismarine cap) rises through the dome's oculus and is visible from every island. Islands are coral terraces at three heights, undersides of dark prismarine.
- **Hero landmark:** the **tideglass column** — 5×5 glass shaft, water source at the base, bubble particles, glowing prismarine crown, brass rings every 8 blocks. It is simultaneously the updraft (travel) and the compass.
- **Islands (9):** *Reef* (coral/tube/brain + sea pickles), *Shell Sand* (sand/sandstone, giant shell), *Mangrove Garden* (mud/mangrove roots/lily pads), *Pigment* (20 glass floats in a rack), *Deep Ore* (deepslate/amethyst), *Verdigris* (copper pipes and a diving bell), *Brine Frost* (packed ice/snow, a frozen lighthouse), *Vent* (blackstone/magma, a subsea chimney), *Kelp Walk* (dried kelp blocks, a drying rack).
- **Gallery:** a mosaic floor of sea-glass tiles with shell inlays; each diorama sits in a shallow "tide pool" with a 1-block stone lip; selection floods the pool with light.
- **Build hall:** dry harbour office — quartz/white plaster, teak joinery, brass portholes, barrels and rope, rain on the windows (particles), team awning + carpet, brass rare-build cabinet.
- **Travel:** bubble columns, slime-block shell pads, water-slide chutes between terraces; elytra still allowed.
- **Why it is not MCC:** no open sky, no black industrial ring, no hazard stripes as the main motif; the signature is water-and-glass volume and a vertical column of light, not floating rock islands.

### Direction C — **Cinderworks** (volcanic foundry)

- **Mood:** night shift at a mountain foundry. Embers, steam, heavy steel; the darkest of the three, so lighting carries it.
- **Palette:** blackstone/basalt/deepslate (60%) · steel grey (iron/polished andesite) + furnace brick (30%) · molten orange + one verdigris accent + warm lamp (10%). Rows in `palettes.csv` (`dir_c`).
- **Silhouette:** a broken smelter shell around a round casting floor, a 30-block blast-furnace chimney with a glowing throat, and gantry bridges running out to slag-ledge islands over a lava ravine. Chimney + gantries read at 200 blocks.
- **Hero landmark:** the **blast furnace** — a stepped brick chimney with an iron-banded throat, a magma/glowstone core visible through grillwork, steam particles and a redstone-lamp "heat gauge" up its side. The updraft comes from its vent; the whole map is oriented around the glow.
- **Islands (8):** *Quarry* (tuff/andesite/stone, a cut terrace with crane), *Charred Timber* (charred spruce/dark oak, a saw yard), *Kiln Yard* (bricks/mud bricks, bottle kilns), *Pigment* (glazed terracotta, a dye vat row), *Ember* (magma/blackstone, a lava pour), *Verdigris* (copper pipes, a cooling tower), *Ash Garden* (moss/azalea under grey ash, a greenhouse), *Ash Frost* (snow + white concrete, a frozen slag heap).
- **Gallery:** a chalked casting-floor pattern (white 1-block lines, circles, hatching) with dioramas on iron stands; the selected stand swings a lamp over the plot.
- **Build hall:** pattern shop — light grey concrete, steel frame, chalk lines on the floor, timber pattern tables, team carpet + hanging banner, brass cage for the rare build.
- **Travel:** steam vents (piston/launch pads), conveyor rails, gantry walkways, and the furnace updraft; path lighting in glowing floor channels.
- **Why it is not MCC:** MCC's palette is bright retail plastic + sky; this is a dark, hot, industrial interior where the light sources are part of the architecture and hazard striping is thematically earned rather than decorative.

## 8. Premium bar (testable, 18 bullets)

A map passes only if every bullet can be checked from a render or a 10-second flyover.

1. The spawn view shows three depth layers (foreground props / midground islands / far wall or skyline) within one screen.
2. Every island has a unique silhouette readable at 100 blocks, from at least two angles.
3. Every large surface uses a weighted palette of ≥3 blocks; no surface is one material for more than 6 blocks without a break.
4. The map uses ≥25 distinct block types, with ≥5 reserved for trims/edges only.
5. No non-gameplay flat run exceeds 6 blocks without an inset, rib, frame, lamp or prop.
6. Team colour appears only as soft goods, nameplate, light tint or pads — never as the architecture.
7. Every island underside tapers ≥6 blocks with ≥2 strata and ≥3 detached/dangling chunks.
8. Every edge and drop carries the same hazard/utility trim language.
9. Each island has exactly one hero landmark (3× player height or more) and one 3D word sign 4–6 blocks tall.
10. Signage is legible at 60 blocks; no area is identified only by floating text.
11. Every display surface shows the material as a stack ≥3 deep, framed by trim.
12. The ceiling is designed (trusses/ribbons/hanging props), not empty sky directly above the hub.
13. A warm light source exists every 8–12 blocks at eye level; no interior pocket is unlit.
14. The rare/gold station differs in silhouette, colour and light from every other object.
15. Plot floor is neutral mid-value, frame is light, and a placed build is readable at 30 blocks against it.
16. Gallery selection changes the whole frame colour plus an emissive rim and a particle cue, not a small icon.
17. Travel elements (fan/launch/boost) are ≥3-block silhouettes with a particle and sound cue, and are visible from two islands away.
18. The world has no visible void from any playable viewpoint: sky + clouds + distant silhouettes, or an enclosing shell.

## 9. Open questions

- **Island count and layout** in the current map: 8–10 is the working assumption from screenshots; no single frame shows all. Needs the map agent's call. **L**
- **Pier function:** the ring of team bays with glass cabinets looks like a per-team staging/showcase area, but the footage never shows whether teams gather there or it is decorative. **L**
- **Build-area access:** the portal is visible, but whether it teleports or is a walk-through is not observable in a 2.5-minute window. **L**
- **Exact fan form:** only the top-down disc is visible; the blade construction, housing and particle column are unseen. **M**
- **How per-team halls differ:** only a red-team hall was captured. Assume neutral shell + team-colour soft goods until the map agent confirms. **M**
- **Time of day / skybox:** all current footage is bright day. Night/storm variants are untested; our directions A and C depend on controlled interior light.
- **Performance at scale:** the real map is far beyond the current block budget; our build must cap the static shell and reset only gameplay blocks (see `MAP_BRIEF.md` tick rules). Block-count estimates per area are still needed from the map agent.
