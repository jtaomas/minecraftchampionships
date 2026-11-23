# Skyblockle: map structure research

Purpose: understand how the arena in the retired MCC game Skyblockle is structured (scale, layout logic, island types, look). We will build our own original map for a private fan server. This file describes principles and approximate numbers only. It is not a copy of an official layout, and official map/island names are deliberately not used.

Researched 2026-11-23. Everything below is view-only research: nothing was downloaded and no account was used.

## Sources viewed

| # | Source | What it showed | Value |
|---|---|---|---|
| S1 | mcchampionship.wiki.gg / fandom: game page (text) | Official description, mechanics, 1 map, 18 min, 10 teams of 4 | Mechanics |
| S2 | wiki.gg game page, website promo image (2500x1406) | Eye-level view of several outer islands: sculpted statue island, tree island, a TNT column, thin player-made bridges, cyan particles | High for look |
| S3 | wiki.gg map page: text + 1 screenshot | Calls the centre a "duel platform"; the screenshot is inside the central arena (grid floor, stone-brick ring wall, ladders, arched openings) | High for the centre |
| S4 | wiki.gg TGTTOS version (MCC MU) screenshot | A high view of the central castle-tower plus many small islands at different heights. This is a race version, so its yellow stair route and coloured guide lines were added for that game | Medium (modified) |
| S5 | YouTube thumbnails (MCC 4 clip, MCC 8 clip, MCC AF clips) | A wide in-game view of scattered islands at mixed heights (10/10 teams alive, ~10 min left); a giant statue prop; a wooden spawn deck with crafting/enchanting blocks; ship-like wooden islands in the distance; the castle interior late in the game | Medium |
| S6 | **mccisland.wiki Skyblockle page: interactive chest map + full top-down render (5424 px square)** | Exact positions of every spawn and chest group, with chest contents, for the 2026 MCC Island remake ("relive the classic"). Read at the assumed 16 px per block (a 6-block L-shaped start island checks out at that scale) | **Very high for layout logic.** It may differ in detail from the 2019-22 event version |

Blocked or not useful: Reddit (blocked in the browser pane), YouTube video playback (ads/embed errors; thumbnails only), the fandom page (same text as wiki.gg).

## 1. Footprint and height

- **Footprint:** a circular field. The outermost objects sit about 165-170 blocks from the centre, so the field is about 330-340 blocks across. The render's square is about 339 blocks per side. *(High, from S6. Block scale is inferred.)*
- **Radial rings (distance from centre, approx.):**
  - ~20-25: the central castle (about 45-50 blocks across).
  - ~48: small crafting/enchanting decks.
  - ~70: small brewing decks.
  - ~103: fort islands.
  - ~119: thin pillars.
  - ~133-136: team start islands, with secondary islands placed between them.
  - ~148-150: the resource-generator and spike islands just outside the starts.
  - ~160-170: scattered decorative or rock islands at the very edge. *(High for order and spacing, medium for exact values.)*
- **Height range:** not measurable from a top-down view. The screenshots show islands deliberately staggered at different heights, roughly ±10-20 blocks of each other. Thin pillars and columns rise well above the islands (an estimated 15-30 blocks). The central castle is the tallest mass: an estimated 30-40 blocks from its base to the tops of its towers, with the arena floor raised inside it. Everything floats over void. *(Low-medium, estimates from S2/S4/S5.)*

## 2. Island count and roles

The layout has **10-fold radial symmetry**: one wedge per team, 36° wide, repeated 10 times. Every island type appears either 10 times (one per team, on the team's axis) or 20 times (two per team: on the axis and on the boundary between two teams), plus one centre. *(Very high, S6 marker angles.)*

| Ring | Count | Size (est.) | Role | Contents (remake data, paraphrased) |
|---|---|---|---|---|
| Start island | 10 | ~6x6 L-shape, 3-wide arms, classic Skyblock shape with one tree | Team spawn | Starter chest: food, a few logs, a little cobble |
| Generator island (just behind the start, ~13-15 blocks further out) | 10 | ~8x8 rounded stone blob with water + lava | Infinite cobblestone (cobble generator) | Chest: iron, gold, stone pickaxes, food |
| Between-team grass islands (same radius as starts, offset half a wedge) | 10 | ~6-7 block blob | Contested pickup between two neighbours | Bow, arrows, a healing splash |
| Spike islands (outer ring, both on-axis and between) | 20 | ~3x3 footprint, tall sand / red-stone spikes | Small loot spires | Cobble, an iron sword, a golden apple |
| Thin coloured pillars (inner of the start ring) | 20 | 1x1 column of coloured concrete, tall | Vertical loot points that are risky to climb or bridge to | Diamonds + a mobility item |
| Fort islands (one per team axis, ~30 blocks in from the start) | 10 | ~11x11 dark brick square with crenellations | First "real" structure on the way in | Weapons, snowballs, food, a water bucket, blocks |
| Brewing decks (offset half a wedge) | 10 | ~5x5 deck | Potions, cobwebs | Brewing stand + ingredients |
| Enchanting decks (on-axis, near the centre) | 10 | ~5x5 wooden deck | Gear upgrades | Low enchanted books, food, blocks |
| Central castle | 1 | ~45-50 across, round | Final duel arena, best loot | 4 chests (one per cardinal tower): armour piece, mobility, food |
| Edge rock islands | ~10-20 | ~8-10 rough grey blobs | Filler/scenery and extra stone at the edge | none listed |

Total: roughly **110-130 separate pieces**, but only about **11 kinds**. Each kind is repeated identically per team, so it is fair. *(High, S6.)*

The design idea is a **loot gradient**: basics at the edge (logs, cobble, apples), then iron/bow tier, then diamonds/enchants/potions further in, and the best gear in the centre. The richer an island, the closer it sits to the middle. *(High.)*

## 3. What is on each kind of island (visual)

- **Start island:** dirt/grass with a single oak-style tree and one chest. In the event version, the start area also seems to include a small wooden deck with a crafting table / enchanting table / bookshelves (S5 thumbnail). It may have been expanded per event. *(Medium.)*
- **Generator island:** a stone/cobble rock with a water source and a lava source placed so they create cobblestone. This is the classic Skyblock mechanic and the main renewable block source. *(High.)*
- **Spikes / pillars:** narrow vertical landmarks (sand, red stone, and bright red concrete columns). In the screenshots they read as tall red sticks across the whole map, so they double as orientation markers. *(High.)*
- **Forts:** dark nether-brick style squares with battlements, a small enclosed room with the chest inside. *(Medium-high.)*
- **Statues / props (event version):** big sculpted props on some islands: a giant pale stone animal/figure, a sculpted stone head/bust, a giant ender-eye on a wooden plinth, a TNT column, and ship-like wooden islands with masts. These look like themed "showpiece" islands that the 2026 top-down render may simplify or omit. *(Medium. They are seen in S2/S5 but cannot be mapped to rings.)*
- **Central castle:** a round stone-brick keep. Its outer wall is mossy/cracked stone bricks with patterned (meander/chiselled) bands. There are 4 square towers at the cardinal points, each with a chest and gate. A ring of **red and yellow striped** roof or seating segments sits between the towers, with arched openings to the sky. Inside is a **flat round arena floor about 25 blocks across**, a white/light-blue grid pattern (it looks like glass or snow with a dark grid on top), and a small feature at the exact centre. There are ladders on the inner walls and gates or ladders on all four sides outside. *(High, S3/S4/S6.)*

## 4. Connections

- **No pre-built bridges between team islands.** Players bridge with cobble from generators, logs and dirt. All the thin lines in the screenshots are player-made 1-wide bridges. *(High.)*
- The **typical bridging gap** between neighbouring pieces on one team's path is about **10-25 blocks**: start → generator ~13-15, start → pillar ~15-20, pillar → fort ~16, fort → brewing ~30, brewing → enchanting ~22, enchanting → castle wall ~25. Between two neighbouring teams' starts the gap is about **80 blocks** along the ring (chord at r≈135 over 36°). The between-team grass island sits halfway, so it is ~40 blocks from each. *(Medium-high, derived from S6.)*
- Ender pearls, snowballs/knockback, cobwebs and lava/water buckets are part of the toolset. Bridges are therefore meant to be interruptible (pearl clutches and lava accidents are recurring wiki anecdotes). *(High, S1.)*

## 5. Starts, sightlines, flow

- All 10 starts are on one ring, evenly spaced, all facing the centre. Each team's straight line inward ("spoke") passes generator → start → pillar → fort → enchanting deck → castle. The **between-team spokes** carry grass island → brewing deck and are naturally shared by the two neighbours. *(High.)*
- **Sightlines:** open sky in every direction. From a start you can see both neighbouring starts (~80 blocks), the tall pillars and the castle silhouette. Height staggering and the bulky forts break some lines of fire. Bows and snowballs matter because you can see people while they bridge. *(Medium.)*
- **Pace:** an 18-minute game with a shrinking border that pushes everyone into the castle. Early on it is a farm/loot race with skirmishes between neighbours over the shared islands. Late game is a duel in the walled arena. *(High.)*

## 6. Hazards and borders

- Void under everything: falling is death. *(High.)*
- A **shrinking world border** closes from the outside toward the centre. Players got caught "suffocating in the border" (wiki). The outer rings become unsafe in order: generators/spikes first, then starts, forts, decks. *(High.)* Whether the border also had a ceiling or rising floor is unknown.
- Lava (from the generators and buckets) is a player-made hazard. *(High.)*
- Cyan particles drifting all over the sky in several screenshots, and red particles in the arena: likely border/ambience particles. *(Low.)*

## 7. Materials, palette, theme, detail

- **Palette:** natural Skyblock materials at the edge (grass, dirt, oak, cobble, stone, sand, red stone). Darker nether brick for the forts, warm wood for the decks, grey stone brick for the castle. The only "loud" colours are **red + yellow** on the castle and the **red pillars**, so the centre and the vertical markers pop against a pale sky. *(High.)*
- **Theme:** a "classic Skyblock world, but tournament-dressed". It is not a heavy single theme like Sky Battle's later maps. It is a collection of mini-diorama islands (statue, ship, tower) around a stone arena. *(Medium-high.)*
- **Detail density:** low on the resource islands (deliberately tiny and readable), medium on forts and decks, high on the castle. Showpiece statue islands are high-detail sculptures. *(Medium.)*

## 8. Backdrop

Bright default daytime sky, very light/hazy blue toward the horizon, with nothing below (void) and no terrain at the edges. The edge rock islands and showpiece props are the only scenery. *(High.)*

## 9. Team colours

Not visible in the map geometry. All 10 start islands are identical and uncoloured in the top-down render. Team identity came from players (coloured name tags, team-coloured leather/jersey skins in MCC 1) and the sidebar. **We could not confirm** any team-coloured blocks on the start islands in the original. *(Low-medium.)*

## What we could not see

- True heights / vertical offsets of each ring, and whether islands step up or down toward the centre.
- Close-ups of a start island and generator island in the event version (only partial thumbnails).
- Whether the showpiece statues/ships were on specific rings or were extra filler.
- Whether the castle floor was breakable, and what sits at its exact centre.
- Border speed/shape (circle or square, vertical component).
- Any team-colour markings on the map itself.
- Differences between the 2019-22 event map and the 2026 remake (the S6 numbers come from the remake).

## Design requirements for our own map

1. Use **N-fold radial symmetry with one wedge per team** (10 for 10 teams). Every island type is either on a team's axis or on the boundary between two teams, so there is no positional advantage.
2. Keep the field **~280-340 blocks across**. Put starts on a ring **~120-140 blocks** from the centre and the final arena **~40-50 blocks** across in the middle.
3. Build a **loot gradient of 4-5 tiers** from edge to centre (basics → iron/bow → diamonds/potions → enchants → best gear in the centre). The richer the island, the closer to the middle.
4. Keep start islands **tiny and identical** (~6-8 blocks, one tree, one chest) in our own shape, not a copied L. Give each one a renewable block source next to it (a generator-type island ~12-15 blocks behind it).
5. Place **shared "between-team" islands** halfway between neighbours (~40 blocks from each start) so early PvP happens between neighbours, not across the map.
6. Keep **bridge gaps of 12-30 blocks** between consecutive pieces on a spoke. No pre-built bridges: players must build every crossing.
7. Add **thin tall vertical markers** (1-2 wide columns in one loud accent colour) as high-risk loot spots and orientation landmarks visible from anywhere.
8. Put a **chunky defensible structure about 25-35 blocks in from each start** (our own design, not a nether-fort square). It is the first cover and the first fights over loot.
9. Make the centre a **walled round arena** with 4 cardinal entrances/towers holding the best chests, a flat open fight floor ~20-25 blocks across, and climbable walls (ladders/steps). It must be the tallest and most colourful object.
10. **Stagger island heights** (±10-20 blocks) so bridging sometimes means building up or down, and sightlines are partly broken.
11. Keep the **palette mostly natural** (grass, stone, wood) and reserve **one or two loud accent colours** for the centre and the markers. Choose our own accent pair, not red/yellow.
12. Add **2-4 showpiece sculpture islands** per map (our own props/mascots) as scenery between rings. They should not block the main spokes.
13. Use a **shrinking circular border** that consumes the rings in order (outer generators → starts → forts → decks) and ends at the arena walls. Add a visible particle edge.
14. Use **void below and a bright hazy sky** as the backdrop. Add a few rough rock islands at the very edge so the rim doesn't feel empty.
15. Show **team identity on the start island** (a coloured banner, wool or concrete trim in team colour). This improves on the original, where we found no evidence of it.

## Image-search pass (2026-11-23)

Method: built-in browser, view only (no downloads, no sign-in). Searched Bing Images ("MCC Skyblockle", "\"Skyblockle\" MC Championship", "skyblockle mcc island map", "minecraft championship skyblockle ...", "skyblockle minecraft mcc gameplay"), Google Images ("MCC Skyblockle", "MC Championship Skyblockle map islands"), DuckDuckGo Images ("skyblockle mcc island start island") and Bing Videos (for YouTube thumbnail IDs). The result pool for this game is thin: most "Skyblockle" queries get auto-corrected to generic "Skyblock" server art, and the wikis supply most real hits. Reddit image hosts (i.redd.it, external-preview.redd.it) are blocked in the browser pane. The YouTube search page did not render. A 1920 px overview was inspected by zooming into regions of the original file.

### Images viewed

| # | URL | What it shows | Value |
|---|---|---|---|
| I1 | https://mccisland.wiki/images/5/50/Skyblockle_Map.png (1920x1177, also listed by Bing/Google) | **High-angle perspective overview of the whole remake arena**, looking from just outside one team's generator toward the centre. Every island type is visible in 3D: start, generator, spikes, pillars, forts, decks, castle and edge rock. This is the most useful image found in either pass | **Very high** (closes most visual gaps) |
| I2 | https://i.ytimg.com/vi/4G29yLvHHT4/maxresdefault.jpg (top-10 plays compilation, MCC 4 clip) | Eye-level view across the arena in the 2020 event, with the **full sidebar HUD** visible. Also shows a giant ender-eye showpiece on a wooden deck and ship-like islands to either side | High for HUD, medium for props |
| I3 | https://i.ytimg.com/vi/e9demM-SrPQ/maxresdefault.jpg (MCC 8 clip) | A close-up of the event-version enchanting/showpiece deck: oak plank deck with log posts, cobwebs, a bookshelf, and the big ender-eye sculpture next to it. Shows the hotbar and armour HUD | Medium |
| I4 | https://i.ytimg.com/vi/TFwpB_os9Xg/maxresdefault.jpg (MCC Scuffed clip) | The **castle interior from above, late in the game**: stepped stone-brick tiers, orange accents, a grass ring at the floor edge, a white floor with a dark grid, player lava. HUD shows the final-game sidebar ("Back to Hub" timer, 1/40 players) and a "top 5 players this game" panel at the bottom left | High for interior |
| I5 | https://static.wikia.nocookie.net/mcchampionship/images/8/8c/Skyblockle-Map.png/revision/latest (1920x1080) | Re-examined at full size: inside the arena. The floor is a light ice-blue surface under a **dark-brown grid** (thin lines every ~2 blocks). Two **full-height ladders** on the inner wall, an open roof ring with sky between the towers, cyan and red particles, a chest and player-placed stone in the middle | High |
| I6 | https://static.wikia.nocookie.net/mcchampionship/images/5/56/TGTTOS-Skyblockle.png/revision/latest (1920x1080) | Re-examined: the castle seen from close by, next to its top. Cyan glass arch windows in grey stone, red vertical banners with lettering, **yellow crown-shaped tower caps**, and a red/yellow seat ring inside. Wooden decks and small islands are clearly **20-30+ blocks below the castle rim**. (The yellow stair is from the race version only) | Medium-high for heights |
| I7 | https://i.ytimg.com/vi/fkUL53Ry498/maxresdefault.jpg (MCCI remake clip) | The remake castle interior at eye level: dark stone-brick walls with a **meander / Greek-key band**, tiered seating with orange and teal blocks, and a red banner on a tower | Medium |
| I8 | https://i.ytimg.com/vi/xr-Vd117eXc/maxresdefault.jpg | Event lobby/voting room, not the arena. No map value | None |
| I9 | https://i.ytimg.com/vi/vofMnVnwNXs/maxresdefault.jpg | MCCI hub + logo thumbnail. No map value | None |
| I10 | https://img.wattpad.com/story_parts/1154870523/images/16b7f70176d9f183757597780068.png | Fan guide image. It shows **Sky Battle** (white/orange islands), not Skyblockle. Discarded | None (pitfall) |
| I11 | i.ytimg.com/vi/{FMxwuNEp4bI, h-DmDxmUSLo, Lh4gpWsY_8I, Uyp_B9yQ56w, txv3szwxpJg, k5xlXa_32DA, N36KS2TkiLQ, lOtsyz366IU}/hqdefault.jpg | Logo/face thumbnails or generic Skyblock videos. One shows a plain cobble bridge over void. No map structure | None |
| I12 | https://i.ytimg.com/vi/wDgdTPxILe8/hqdefault.jpg | Podium/winner scene. No map value | None |

Useful new images: 7 (I1-I7; I5/I6 were seen in the first pass but re-examined at full size), plus about 12 checked and discarded. Together with the first pass, that makes about 11 distinct useful images of the one map, in two versions (2019-22 event and the 2026 remake). There is only one Skyblockle map, so the "3 maps" target does not apply.

### Updated / corrected structure notes (from I1 unless stated)

- **Start island: confirmed and detailed.** A small **L-shaped grass/dirt island**, about 3-4 blocks thick, with **one tree** in the corner of one arm and **one chest** on the other arm. Nothing else: no deck, no crafting table, no banner. All start islands are identical. The grass and leaves have the same muted teal tint on every island, so this is foliage colour, **not a team colour**. *(High.)* Correction: the wooden deck with crafting/enchanting blocks seen in the earlier S5 thumbnail is the **inner enchanting/showpiece deck**, not part of the start island (I3).
- **Generator island: confirmed and detailed.** It is not a bare rock. It is a **small stone-brick "shrine"** about 8-9 blocks across and 6-8 blocks tall. A rounded base of stone brick and cobble with mossy and cracked patches carries a **sunken basin with lava on one side and water on the other**, so cobblestone forms between them. Above the basin is a **canopy roof of stone-brick slabs and stairs on four posts**, with cobwebs and a hanging chain/light. It sits **directly behind the start, slightly overlapping it in the view, almost touching** (≈1-3 block gap, possibly a slight height step). That is closer than the "13-15 blocks" estimated from the chest map: the 13-15 figure is centre-to-centre distance, not the gap. *(High for look, medium for gap.)*
- **Spike islands: corrected.** They are not tall spikes. They are **small blobs of sand/sandstone or red terracotta, about 3-5 blocks across, with a short totem on top**: a 1x1 stack of about 4 blocks (dark stone, gold, pale quartz/iron, dark cap) next to a chest. The two materials alternate between on-axis and between-team positions. *(High.)*
- **Pillars: confirmed.** They are **2x2 red concrete/wool columns, an estimated 15-20 blocks tall, with the chest on top**. They are the only tall thin objects and read as red sticks across the whole field. They stand at various heights, so their tops are not level. *(High for look, medium for size.)*
- **Fort islands: corrected.** Not nether-brick squares with an enclosed room. Each is a **roughly cubic hut about 10-11 wide and 8-10 tall**: a **dark blackstone/deepslate base with a ragged underside**, **dark-oak plank walls with large window/door openings on several sides**, and a **flat dark-brick roof with crenels and cobwebs**. A **ladder on an outside wall** reaches the roof. *(High.)*
- **Brewing decks:** small **white/pale stone (calcite/quartz-like) islets with a green-topped brewing setup and a light-blue underside**, at about half the height of the forts. Enchanting decks: **small oak plank decks / boat-like platforms with posts** closest to the castle (I1, I3). *(Medium-high.)*
- **Between-team grass islands:** a **round grass blob (not an L)** with a tree, a few flowers/props and a utility block (anvil/cauldron-like). It is clearly a different silhouette from the start island, so players can tell their own start apart from the shared island. *(Medium-high.)*
- **Edge scenery:** a **huge grey deepslate/stone asteroid with ore patches** (copper/gold coloured) at the very rim. There are also **grey rock "boulder-on-a-stalk"** shapes and pale stone arches near the far side. These are the "edge rock islands" from the earlier pass, and they are much bigger (20+ blocks) than the gameplay islands. *(Medium.)*
- **Showpieces (event version):** the ender-eye sculpture sits **on an inner oak deck** (I2/I3), the ship-like wooden islands are at the deck ring around the castle (I1 shows boat-shaped wooden decks orbiting the castle), and statues are scattered. So showpieces sit **on or next to the inner loot decks**, not out at the start ring. *(Medium.)*
- **Central castle (exterior):** a round drum about 45-50 across. It has **2-3 tiers of tall arched windows** filled with red/orange/blue glass, and the base tapers into a **stalactite-like hanging underside** that reaches well below the island plane. Four square towers have **yellow crown-shaped caps** and **red vertical banners**. The top of the castle is the highest point of the map: an estimated **20-30 blocks above the start/fort plane**, with the underside hanging another ~15-20 below it. *(Medium-high, I1/I6.)*
- **Central castle (interior):** a **bowl/amphitheatre**. Stepped stone-brick tiers (bleacher-like) descend from the rim to a **flat round floor about 20-25 across**. The floor is light ice-blue/white with a **thin dark grid** and has a **grass ring around its edge**. The tiers carry orange/terracotta "seats" with a meander band. **Two or more full-height ladders** on the inner wall lead back up to the rim. The roof is **open**, with sky visible between the towers. No fixed object was seen at the exact centre: the stone and chest in I5 are player-placed. *(High, I4/I5/I7.)*
- **Heights / staggering: corrected downward.** In the overview, starts, generators, pillars' bases, forts and decks all float **on roughly one plane, with small offsets (about ±3-8 blocks)**. Pillars rise 15-20 above it, and the castle rises 20-30 above it. The earlier "±10-20" was too high. Bridging between neighbouring pieces is therefore mostly flat or a short climb. *(Medium. This comes from a single perspective image.)*
- **Border look:** not visible as a wall in any image. Cyan "puff" particles hang in the air across the field and inside the arena late in the game (I5, S2), and red dust appears inside the arena. They still look like ambience/border particles, and there is no solid border wall texture. *(Low.)*
- **Team colours:** **confirmed absent** from the map geometry (I1: every start island is identical, with no wool/banner/concrete trim). Team identity comes only from players and the HUD. *(High.)*
- **HUD (event version, I2/I4):** a right-side sidebar with the event title, "Game n/8: Skyblockle", **Time Left**, a **next border shrink countdown** (seconds), the **coin/score multiplier**, a **top-teams list with scores**, **Players Alive x/40**, **Teams Alive x/10** and **Your Kills**. At the end, a "Back to Hub" timer and a bottom-left "Top 5 players this game" panel with kills/score appear. *(High.)*

### Gaps: status

| Gap from first pass | Status |
|---|---|
| Close-up of start island | **Closed**: L-shape, one tree, one chest, nothing else, no team colour |
| Close-up of generator island | **Closed**: roofed stone-brick shrine with a lava + water basin, almost touching the start |
| Island heights / vertical staggering | **Partly closed**: one common plane with small offsets; pillars and castle rise above it. Exact numbers remain estimates |
| Castle interior and towers | **Closed** (look and layout): bowl with tiers, gridded floor, grass ring, ladders, open roof, crown-capped towers with banners. The **contents of the tower rooms** are still unseen |
| Whether the centre has a fixed feature | **Mostly closed**: none visible. Objects seen there are player-placed |
| Team-colour markings | **Closed**: none on the map |
| HUD | **Closed** (event version) |
| Border look/shape/speed | **Open**: no clear image of the border edge; only particles |
| Placement of showpieces | **Partly closed**: on or near the inner decks, not at the start ring |
| Floor breakability; event vs remake differences | **Open** (the event version had a lighter, brighter castle with cyan glass, the remake a darker one; the rest of the layout looks the same) |

### Design-requirement adjustments (supersede the matching items above)

- Item 4 (start islands): keep them **bare**: our own small shape, one tree, one chest, and put the **generator directly against the start (1-3 block gap)** as a separate, visibly built "machine" structure with a roof, not a plain rock.
- Item 7 (markers): use **2x2 columns about 15-20 tall** with the loot on top.
- Item 8 (forts): an **open-sided hut with a climbable roof** (outside ladder), dark base and our own material mix. Avoid copying the dark-brick-plus-dark-oak combination.
- Item 9 (centre): make the centre a **sunken bowl**: tiers stepping down to a flat 20-25 floor, ladders back up, open roof, a hanging underside, and the tallest silhouette on the map. Use a floor pattern of our own, not the gridded light floor.
- Item 10 (heights): use **one main plane with ±3-8 block offsets** instead of ±10-20. Get the vertical drama from the pillars and the centre, not from big island height jumps.
- Item 15 (team identity): still recommended for us, since the original confirmed has none. Add a small team-coloured detail on each start, plus a sidebar HUD of the same kind (time left, next shrink countdown, players/teams alive, kills).
- New: give the **shared between-team island a clearly different silhouette** from the start island, so a player can tell at a glance which island is theirs.
