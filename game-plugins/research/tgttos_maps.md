# TGTTOS: map structure research

Purpose: understand how the maps of the MCC race game "To Get To The Other Side" are built (scale, start, goal, crossing types, look), so we can design our own original maps with the same structure and quality for a private fan server. This file covers principles and approximate numbers only. Official map names are not used in the analysis. Sources are listed by URL, and some of those URLs contain the names. No official layout is reproduced.

Researched 2026-11-24. All of it was view-only: nothing was downloaded and no account was used. I used my own browser tab, which I closed afterwards.

## Sources viewed

**Wiki images (full-size, 1920x1080 or 3840x2160): 34 images from 33 different maps.** About half are high free-cam overviews of the map, and the rest are crowd shots taken during a round. Each entry gives the base URL, the file name, and what the image shows.

From `mcchampionship.wiki.gg/images/` (event versions, 2019-2025):

| # | File | Shows |
|---|---|---|
| W1 | TGTTOS-Walls.png | City construction site. The start is on a road strip with grass verges. Hazard-striped wall sections with a "keep out" sign. Brick and grey tower facades as a backdrop. Desert-coloured void sky |
| W2 | TGTTOS-Pits.png | Crowd on a grey start floor facing a grey trench wall. Players' rainbow bridges and pillars go over and up. Painted sky-box walls (blue with white pixel clouds), big signs |
| W3 | TGTTOS-Cliff.png | 40 players packed on a ~3-4 wide yellow-edged ledge running along a grey cliff face. Wooden stilt walkways are ahead. Void below |
| W4 | TGTTOS-Boats.png | Flat sheet of packed ice and ice. A red/white striped finish arch, with hot-air-balloon props in the sky |
| W5 | TGTTOS-Doors.png | Arena with a sand floor. A gate wall with ~10 doors under a grandstand, castle towers with red roofs, banners. Trapdoor holes in the floor |
| W6 | TGTTOS-Glide.png | Elytra map. A black starry box wall, white cloud platforms, coloured pillars, and a red plane prop |
| W7 | TGTTOS-Siege.png | Castle front on a hill: a grass start with a black-and-white pad, then a moat with lily pads, gates, a terrace of stairs, and roofs with banners on top |
| W8 | TGTTOS-SkydiveOriginal.png | Start on the edge of a tall stone tower. Below are dozens of floating cubes painted as concentric coloured targets |
| W9 | TGTTOS-Basins.png | Floating stone islands, each with a water basin on top. A tall back wall with stone lumps as holds. Players are in the air between basins |
| W10 | TGTTOS-Industry.png | Factory hall: conveyor/glass lanes, hanging cranes, hazard-striped beams. Rainbow player bridges |
| W11 | TGTTOS-Breakdown.png | Indoor station hall with long blue lanes in the floor, a yellow ceiling, and player-built pillars |
| W12 | TGTTOS-Launcher.png | **Side view of a whole map**: one compact red/flame-painted block about 60-70 long, with a start deck on the right, launch pads in the middle, and a giant chicken statue on a pillar at the goal end |
| W13 | TGTTOS-ShallowLava.png | A volcano backdrop. Grass/podzol islands in a sea of lava, with trees |
| W14 | TGTTOS-ToTheDome.png | A finish gate in festival style: huge sign, rainbow banners, mascot statue |
| W15 | Terraswoopforce.jpg | A cave tunnel start: players with elytra stand on a stone stair dais in front of a floor hatch. A sign says to drop once the hatch opens |
| W16 | TGTTOS-Skyscraper.png | A tall hotel tower facade (10+ floors) with furnished rooms behind the windows |
| W17 | TGTTOS-Spiral.png | An adobe desert house standing in a water pool with stepping blocks, surrounded by terracotta mesa scenery |
| W18 | TGTTOS-Airtrain.png | A flying steam train (red/gold), about 6 carriages long, in open sky |
| W19 | TGTTOS-TrainPassing.png | A train at the bottom, then a grass island, then a ~30-tall terracotta cliff. Players tower straight up in team colours |
| W20 | TGTTOS-Water_Park.png | A wipeout-style pool course: stepping posts, padded red/white obstacles, palm huts, bunting |
| W21 | TGTTOS-Zoo.png | Grey rock enclosures, wooden fences, sand. Players are moving fast through the air (a movement item) |
| W22 | TGTTOS-Las_Vegas.png | A neon casino street with a big lit sign. Colourful block clutter on the start |
| W23 | TGTTOS-Chilli_Con_Carnage.png | A beach island parkour run: ice-block jumps, orange/white striped barriers, a bunting finish line between palms |
| W24 | TGTTOS-Beehive.png | A floor of hex honeycomb cells with raised rims (honey-coloured), a giant hive in a tree |
| W25 | TGTTOS-Badlands.png | Terracotta mesa terraces, cacti, red flags, a big sign on white pillars |
| W26 | TGTTOS-Golf.png | A checkered fairway strip rising in terraces, with flags, towards a tall stacked island |
| W27 | TGTTOS-Domino.png | A long wooden deck about 15 wide, packed with rows of standing domino slabs in black, white and colours |
| W28 | TGTTOS-IceMountain.png | An icy peak with icicle overhangs, wooden scaffold decks and ladders |
| W29 | TGTTOS-SlipperySlope.png | Boats on an ice river between snowy cliffs and industrial buildings, with a penguin statue |
| W30 | TGTTOS-FrozenSprint.png | A narrow snow ravine (~8 wide) with blue-ice trees. The crowd is funnelled through it |
| W31 | TGTTOS-Lunchtime.png | A "tiny players in a giant kitchen" scale gag: checkered walls, a giant cake, a giant figure, grilles |

From `mccisland.wiki/images/` (MCC Island versions, 2022-2025):

| # | File | Shows |
|---|---|---|
| I1 | 9/9b/TGTTOS_Treetop.png | A rope course in the tree canopy: trunk platforms, swings/chains, slime pads, colour-coded cyan/yellow steps |
| I2 | 6/63/TGTTOS_Canyon.png | A dark deepslate cave with lava falls and creeper statues. A narrow path |
| I3 | 2/27/TGTTOS_Fun_House.png | A modern house on a grass yard with giant toys and a black/white start pad |
| I4 | e/e3/TGTTOS_Bricks.png | Parkour over a pile of giant toy bricks between two giant tables, with a purple sky |
| I5 | 8/86/TGTTOS_Drop_Zone.png | A descending stack of ~4 glass-fronted chambers. A lava basin sits at the bottom |
| I6 | 0/09/TGTTOS_Scramble.png | **The whole map as a stepped diorama**: ~5 terraces (water, grass, forest...) rising ~30 blocks over ~70. The start is on hazard-striped pads at the bottom, two NPC statues stand on the top |
| I7 | c/c2/TGTTOS_Construction_Obstruction.png | Three open building frames joined by two tower cranes |
| I8 | 9/92/TGTTOS_Sea_Monsters.png | A floating reef rock with pools, giant fish/jellyfish statues and a shipwreck |
| I9 | 2/2e/TGTTOS_Carnival_of_Chaos.png | A Halloween fair: pumpkin path, Ferris wheel, tents, purple sky |
| I10 | f/ff/TGTTOS_Racetrack.png | A straight race strip ~30 wide and 100+ long, with grandstands on both sides, a pit garage at the end, and tyre/barrier obstacles |

Text sources (wiki.gg and mccisland.wiki game and map pages, patch notes 0.8.0-0.28.0) gave the round timer (1:30 by default), "punch the NPC/chicken at the end", the map pool sizes (~48 event maps and 28 standard + 6 seasonal Island maps), the one-line map descriptions ("speed bridging", "parkour while being punched", "boats on ice", "elytra and building verticality", "climb a hill past piston traps and skeletons", "random doors, some impassable", "drop down onto a platform by bridging/towering") and the record times (10-35 s).

**Video frames: 18 frames from 3 videos.**
- `NRYOGgyuAdA`, a compilation of 1st-place runs (6 maps), frames at 3/14/24/34/46/92/128 s. These show the HUD and items, the bridging POV, the view back down to a start pad, and the finish punch.
- `_ssKl3NOVsE`, the MCC 26 admin stream, TGTTOS chapter at 8056 s. Frames at 8180-8816 s cover 6 rounds: the start line with countdown, the mid-course, three goal pits, and finish counts.
- `yoME302evpk`, the fastest MCC Island run on each map, with chapter list and record cards. Frames at 5/22/58 s.

**Total: 34 images + 18 frames, from 33+ distinct maps.**

---

## 1. Footprint and timing

- **Length, start to goal:** usually **70-140 blocks** in a straight line. Compact maps (a launch-pad map seen from the side, a beach parkour island) are about 50-70. Long flat ones (a race strip, a domino deck, an ice sheet) are about 120-160. *(Medium. Scaled from player sizes in W12, I6, I10, W27, W4, and checked against run times below.)*
- **Width:** **20-40 blocks** where the 40 players start. Many maps then narrow to lanes of 8-15 wide (a ravine, a cliff ledge 3-4 wide, a domino deck ~15). Bridging maps stay 25-40 wide, so 40 people can each build their own line. *(Medium.)*
- **Height range:** flat maps vary by only 3-8 blocks. Climb maps rise **25-45** from start to goal (a terraced diorama about 30, a tower-up cliff about 30, castle terraces about 25). Drop maps start **40-80** above their landing targets. *(Low-medium.)*
- **Crossing time:**
  - The round timer is 1:30 (some events 1:40). Coins drop by 1 per place, and there is a top-10 bonus.
  - Records are 10-35 s on most maps. The fastest bridging map took about 10-13 s, typical maps take 20-25 s, and harder ones (choice of doors, long races) take 33-45 s.
  - In one stream round, **39 of 40 had finished by about 1:13**. On another map, **27 of 40 had finished by 0:41**.
  - So design for: **best player about 20-30 s, median player about 40-60 s, slowest about 75-90 s**. *(High: HUD frames and wiki records.)*

## 2. Start area

- A **flat, open start strip across the full width of the map** (about 30-40 wide, 4-8 deep). Players stand 2-3 rows deep. A dark (black) floor with a straight start line is common. On one map it was a team-colour rainbow stripe, and black-and-white checker pads are also used. *(High: `_ssKl3NOVsE` 8180 s, W7, I3, I6.)*
- Players are held at the start for a 10 s countdown with a large title ("Chaos begins in 9"). Then everyone is released at once. This crowded first 3 seconds is part of the comedy. *(High.)*
- Fitting 40 players takes about **1.5-2 m² of floor per player**, so 35x6 to 40x8 is enough. Some maps have a raised start (the edge of a tower, a dais in front of a floor hatch, launch pads), but the deck is still wide.
- **Items at the start** (the same on most maps):
  - 64 blocks in the player's **team colour** (concrete)
  - a pickaxe (on MCC Island: shears/pickaxe)
  - 5 snowballs for knocking people back
  - map-specific items replace or add to these: a boat, elytra, a launcher, nothing at all for parkour maps

  *(High, from HUD frames.)*

## 3. Goal area

- **What marks it:** a small enclosed **"fan pit"**. It is a low-walled square or round enclosure, about **10-14 across**, where NPC "fans" stand (player-skin NPCs at events; chickens on MCC Island).
  - You finish by **punching an NPC**. Chat then reads "X whacked Y, finishing in 7th place!"
  - Examples: a red-floored round pit with a white rim, a fenced cobble square, a sunken grey box with a black rim, a red-carpet runway lined with fans.
  - *(High: `_ssKl3NOVsE` 8345/8460/8690/8815 s, `NRYOGgyuAdA` 92 s.)*
- **How it is seen:**
  - The pit itself is low, so the **landmark above it** is what you read from the start: a giant sign on pillars, a striped finish arch, a giant chicken statue on a column, banners on the castle top, two statues on the top terrace.
  - On straight outdoor maps the goal landmark is **visible from the start line** (W4, W12, I6, I10, W25).
  - On indoor, maze or choice maps it is hidden until the last section (doors, factory).
  - *(Medium-high.)*
- The goal is usually at the **same height as the start or higher**. On drop maps it is at the bottom. A finished player is a spectator who watches the stragglers come in.

## 4. Crossing categories

Every map has **one dominant gimmick**, sometimes with a short secondary section. These are the categories I saw, with how many of the 33 maps fall into each (some maps belong to two categories):

| Category | Maps seen | What the structure looks like | What makes it fun |
|---|---|---|---|
| **A. Bridge over void/hazard** | ~9 (trench, factory, station hall, casino, mesa corridor, city walls, lava sea, two others) | A wide start deck, then a **void or hazard gap of 10-30 blocks**. Sometimes there is a mid-wall to go over or through (hazard-striped barrier, cobweb, water), then the goal deck. Obstacles stick out of the gap: pillars, crane hooks, beams that block the straight line. The floor of the gap is void or a pit far below. | **Speed-bridging** in team colours. 40 rainbow bridges appear at once, and slow players **reuse other players' bridges** or snowball people off them. There is a choice between building your own line and stealing someone else's. |
| **B. Tower up / climb** | ~8 (cliff tower-up, hotel tower, castle front, icy peak, adobe spiral, terraced diorama, building frames with cranes, golf terraces) | A **wall or slope of 25-45 blocks** facing the start. It is broken into terraces/floors with ladders, stairs, scaffold decks, windows and balconies. The goal is on top. Players can take the built route or **pillar straight up** with blocks. | It reads at a glance ("get to the top"). The built route and the self-built shortcut compete. Players who fall land back on a lower terrace, not in the void. |
| **C. Parkour course** | ~9 (cliff ledge, tree canopy, beach run, lava cave, honeycomb, toy bricks, giant kitchen, domino deck, ravine) | A **narrow (3-15 wide) path** of jumps, often with no blocks given. Themed objects are the jump blocks: domino slabs, honeycomb rims, toy bricks, ice cubes, trunk platforms. The route is mostly straight, with 1-2 bends. | Traffic: 40 people on a 4-wide ledge, **punching each other off**. Big objects make the scale silly (giant kitchen, toy bricks). |
| **D. Drop / descent** | 3 (target-cube skydive, glass chamber stack, downward bridging) | Start on a **high tower edge**. Below is a field of landing targets (painted cubes, water pools, chambers) at various heights. The goal is a specific platform or the bottom chamber. | One brave jump. Aiming for the target, with water-bucket/landing tension. Very short and spectacular. |
| **E. Elytra / glide** | 2 (sky box with clouds, cave tunnel) | A high start, or a dais in front of a **floor hatch that opens at the go**. The route is a long open box or a tunnel with pillars, clouds and props to steer around. The goal pit is at the far end, lower. | Steering. The hatch/launch is a shared dramatic start. Players crash into props. |
| **F. Vehicles: boats on ice** | 2-3 (ice sheet, ice river, race strip) | A **flat ice sheet or a 15-25 wide ice river**, sometimes with bends between snowy cliffs, and a striped finish arch. | Boat-on-ice speed, bumping, wide lines. Very easy to read. |
| **G. Choice gate / doors / maze** | 2-3 (door wall arena, maze, house) | An arena with a **wall of ~10 identical doors**. Some lead through and some are dead ends or traps. Behind it is a second stage (corridors, wooden barriers), then a sand finish floor with trapdoor holes. | Gambling and crowd-following: whoever guesses right first shows the way. The longest map (records ~33 s). |
| **H. Hazard floor / movement gadget** | ~5 (lava with islands, wipeout pool, launch pads, water basins with movement items, skeleton/piston castle) | A shallow lava or water floor, or pads that fling you. Islands and posts are the safe steps. Traps fire along the route. | Chaos and luck, and "everyone gets launched". |
| **I. Moving set-piece** | 2 (flying train, train passing under the start) | A long object (a train ~6 carriages long) as the route or as a moving obstacle. | Novelty: running along a moving train. |

*(Category counts are medium confidence. Some maps were judged from one image plus a one-line description.)*

**Takeaways:**
- About half of all maps are **bridge** or **climb**: the player's own blocks are the core verb of TGTTOS.
- Parkour is the next biggest group.
- Vehicles, elytra, drops and doors are special maps, one of each per event at most.
- Every crossing allows **more than one line**: built route vs own bridge, left vs right of an obstacle, a door choice, a pillar shortcut.

## 5. Scenery vs gameplay; plain vs decorated

- **The route is straight and simple. The set around it is not.** The owner remembers "fairly straight and simple maps", and that is **true for the route**: one axis, one gimmick, a goal you can point at.
- But the maps are **dressed sets**, not bare courses. Big themed buildings, props and signs frame the lane on both sides and behind the goal: tower facades, cranes, grandstands, volcanoes, giant statues, balloons. *(High.)*
- **Estimated split:**
  - On outdoor maps, about **40-60% of the placed blocks are scenery** the player never needs to touch (backdrop facades, trees, statues, a volcano).
  - On sets that are the course themselves (honeycomb, domino deck, castle front, tower), the scenery *is* the route, and only about 15-25% is pure backdrop.
  - *(Low-medium.)*
- **By age:**
  - The earliest maps (2019-20) were the plainest: grey floors inside a **painted sky box** (blue walls with pixel clouds), coloured pillars, big signs.
  - Later event maps and all MCC Island maps are much more detailed: furnished rooms in a hotel, glass chambers with interiors, a full fairground.
  - The route stayed just as simple.
- **Recurring dressing:**
  - huge **pixel-letter signs** with the event logo
  - **red/white or orange/white striped** barriers, arches and bunting
  - **black/yellow hazard stripes** on edges and obstacles
  - giant mascot statues
  - props in the sky (balloons, planes, fish)

## 6. Themes, palette, materials

- **Themes:** one strong real-world or toy-like theme per map. Examples: city construction site, factory, station, castle, desert mesa, adobe house, beach, volcano, ice/snow, cave, treetops, beehive, reef, casino, fairground, zoo, kitchen/toys, train, racetrack, sky box. Seasonal reskins (Halloween, festive, Pride) recolour the same layout. *(High.)*
- **Palette:** **saturated and high-contrast**. Each map has 2-3 dominant colours (honey orange + cream; red + gold; ice blue + white; terracotta + green; grey stone + red banners), with white or black trim. **Team-colour concrete is kept for the players' own blocks.** Map surfaces avoid large areas of plain concrete in team colours, so the bridges stay readable. *(Medium-high.)*
- **Materials:**
  - concrete and wool for clean colour blocking
  - terracotta, sandstone and smooth stone for terrain sets
  - ice, packed ice and blue ice
  - honey and honeycomb
  - polished blackstone or black concrete for start lines and rims
  - glass for chamber fronts and conveyor lanes
  - slabs and stairs for rims
  - big custom-shaped "terrain" in stepped organic forms
  - *(Medium.)*

## 7. Walls, borders and void

- **Most maps float in an open void** (sky blue or warm desert-haze sky). Falling into the void or a hazard sends you back. The admin frames show players reappearing at the start/checkpoint, but the exact rule is a rules-agent question. *(Medium.)*
- **Side borders:** the lane is defined by **the set itself**: cliffs, building facades, grandstands, fences, ravine walls. Early maps used **a painted box** (sky-blue or starry-black walls) that encloses the whole course. There are no glass galleries. *(Medium-high.)*
- **Height:** a ceiling appears only on indoor maps (hall, cave, kitchen). Outdoor climb and bridge maps are open to the sky. Pillaring around the set is limited by the set width and the side void, not by visible barriers. *(Low-medium: barrier blocks can't be seen in screenshots.)*

## 8. How maps avoid feeling like reskins

- **A different verb per map**: bridge, tower, parkour, drop, glide, boat, choose, dodge. In one 6-round event, the admin stream rotated through doors, a lava-bridging map, a parkour ledge, a castle climb, elytra and launch pads. No two rounds shared a verb.
- **A different silhouette per map**: a flat sheet, a wall facing you, a staircase of terraces, a tower to drop from, a narrow ledge, a tunnel, an arena with a gate wall. You can tell the map from the first frame.
- **A different scale trick**: tiny players in a giant kitchen or on toy bricks, against huge buildings in the city sets.
- **A different direction of travel**: forward, up, down, through a gate.
- **A different crowd shape**: a wide spread (bridging), a single file (ledge), a funnel (ravine), a lottery (doors).
- **A different palette and backdrop**, even when two maps share a verb (two bridging maps: grey trench vs a factory with cranes).

---

## What we could not see

- The exact respawn/checkpoint rule when a player falls (likely the last checkpoint or the start). This is for the rules agent.
- Barrier/kill-plane heights and whether pillaring over the set is blocked.
- Exact block dimensions. All sizes are estimated from player height in screenshots.

## Design requirements for our own maps

1. **Straight run of 70-140 blocks** from the start line to the goal pit (compact gimmick maps 50-70, flat speed maps up to ~160). Design for the best player crossing in about 20-30 s and the median in 40-60 s, within a 90 s round.
2. **Start strip 32-40 wide × 6-8 deep**, flat, dark floor with a clear start line and a release barrier for the 10 s countdown. Nothing inside it to snag on.
3. **Goal = one low-walled fan pit, 10-14 across**, with NPCs/chickens to punch, at the far end. Put a **tall landmark above it** (sign on pillars, arch, statue, banners) that is visible from the start on every outdoor map.
4. **One dominant crossing verb per map**, optionally a short second stage. Across our pool, no two maps in the same 6-round set share a verb.
5. **Always at least two lines**: built route vs own blocks, left vs right of an obstacle, or a risky shortcut vs a safe detour. Never one forced single-block path for the whole map.
6. **Bridging maps:** gaps of 10-30 blocks, at least 25 wide, so 40 players can bridge side by side. Add 2-4 obstacles in the gap (pillars, beams, hanging hooks, a mid-wall) that break the straight line.
7. **Climb maps:** a face 25-45 high, split into terraces every 6-10 blocks. A fall should drop you to the terrace below, not into the void.
8. **Parkour maps:** lanes 4-12 wide, jumps of 1-3 blocks built from themed objects, and room for pushing. Wider lanes at the start, so the crowd spreads before the first narrow part.
9. **Drop/elytra maps:** start 40-80 above the targets, with a shared dramatic release (a hatch or a ledge) and a target field that is easy to read (concentric colour targets, water pools).
10. **Team-colour concrete is reserved for player blocks.** Map surfaces use other materials (terracotta, wool tones, stone, ice, honey), so the rainbow bridges always stand out.
11. **2-3 dominant colours per map** plus white or black trim. Use red/white or orange/white striping for finish arches and barriers, and black/yellow stripes for edges and obstacles.
12. **Scenery frames the lane:** facades, terrain, grandstands or a painted box on both sides and behind the goal. It is about 40-60% of the blocks on outdoor maps, but it never sits on the route.
13. **Void below and to the sides** on most maps, with a respawn back to the start or checkpoint. Indoor maps get a ceiling at least 8-12 above the route, so pillaring stays possible but limited.
14. **Each map has its own silhouette** (sheet, wall, staircase, tower, ledge, tunnel, arena), so it can be recognised from the first frame.
15. **Use a scale gag in at least one map** (giant objects or a tiny course) and a moving or interactive set-piece in at most one.
16. **Big pixel-letter signage with our own server name/logo** at the start and goal. No official logos or names.
17. **Original layouts only.** The categories and principles above are generic. Do not copy any specific official map's arrangement.

## Our own map concepts (original)

Each concept is a different crossing category. All have the standard start strip (~36×7) and a fan pit at the end.

1. **Laundry Line (bridge over void).** A rooftop start on a terraced-house block. The route is a 24-block void gap to a second rooftop, with washing lines and hanging sheets (wool, white and pastels) strung across the gap at head height. The sheets block sightlines and bridges must thread between them. A chimney row mid-gap gives an optional stepping-stone line. The goal pit is a rooftop garden ringed by flowerpots, under a big weather-vane landmark. Palette: slate grey, brick red, pastel linen. ~90 long, 34 wide.

2. **Dam Face (tower up / climb).** A curved concrete dam wall, 36 high, faces the start across a short spillway. Built route: maintenance ladders and catwalk terraces every 8 blocks, zig-zagging. Shortcut: pillar straight up the face from the spillway pools (water breaks falls). The goal pit is on the crest road, under a lit control-tower sign. Palette: pale concrete, teal water, yellow catwalk rails. ~70 long, 36 wide, +36 rise.

3. **Bookshelf Run (parkour, scale gag).** Players are mouse-sized on a giant library shelf. They run along a 10-wide shelf of standing and toppled giant books (each book a 3-6 block jump), cross a gap on a pencil bridge, and climb a stack of dictionaries. A desk lamp is the landmark, shining onto the goal pit, which is a teacup on the desk. Palette: walnut wood, cream pages, red/green/blue book spines. ~110 long, 10-14 wide lane, set 30 wide.

4. **Orchard Drop (drop / descent).** Start on the edge of a windmill gallery 60 up. Below is a hillside orchard of floating "apple" target platforms (red/green rings, water centres) at three heights. Landing on a target is safe and gives a short final run downhill to the goal pit in a cider-barrel yard. Missing means a long fall to the void and a respawn. Palette: green, apple red, timber. ~60 long, 30 wide, −50 drop.

5. **Kite Festival (elytra / glide).** A shared launch: a wooden gantry whose floor flaps drop open at "go". Players glide down a 130-block valley with giant kites tethered on chains, wind-sock pillars and festival arches to steer through. A low route through the arches is faster but cramped. The goal pit is a picnic blanket ring on a hill, under a giant kite landmark. Palette: sky blue, multicolour kites, grass green.

6. **Frozen Canal (boats on ice).** A town canal frozen into a 20-wide ice lane with two gentle bends, three low footbridges to duck under, and moored barges as bumpers. A wider lane near the start lets 40 boats spread out. The goal pit is a skating rink lit with lanterns, with a striped finish arch over the last 10 blocks. Palette: ice blue, red-brick quays, lantern gold. ~140 long.

7. **Ticket Gates (choice / doors).** A station concourse. Stage 1 is a row of 12 turnstile gates: some open, some jam (blocked after 1 block), some drop you into a short lower corridor that rejoins later. Stage 2 is a platform crossing between two parked train carriages, with open doors to pass through. The goal pit is the station clock plaza. Indoor, ceiling 12. Palette: cream tile, green ironwork, red signage. ~100 long, 32 wide.

8. **Hot Springs (hazard floor / movement gadget).** A volcanic terrace of steaming pools. Hot water is the hazard (respawn), and the safe route is stepping stones and bamboo rafts. Geyser vents along the way launch players up and forward (fixed launch pads) to skip sections, but a mistimed launch drops you back into a pool. The goal pit is a bathhouse courtyard under a pagoda-style gate. Palette: basalt dark grey, turquoise water, red lacquer wood. ~100 long, 30 wide.

9. **Conveyor Bakery (moving set-piece, parkour).** Players cross a giant bakery production line: three parallel conveyor belts (moving entity platforms, or belt blocks that push the player) carry giant loaves and cakes left to right across the route, with chutes dumping flour clouds as visual cover. Players hop from belt to belt and from loaf to loaf. The goal pit is a giant cake box on the far side. Palette: steel grey, warm bread browns, pink icing. ~80 long, 36 wide.

10. **Quarry Steps (pushing crossing + bridge).** An open quarry with a 4-wide haul road of ledges spiralling down one side, where players get punched into the lower pits. An alternative is to bridge straight across the 22-block quarry bowl from the rim, past a hanging excavator bucket. Both lines meet at the far rim, where the goal pit is the site office yard under a tall crane with a flag. Palette: sandstone, gravel, yellow machinery. ~90 long, 34 wide, −15/+15 relief.
