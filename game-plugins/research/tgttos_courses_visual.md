# TGTTOS: elytra courses and ice-boat tracks, studied from images, video frames and downloaded maps

Research date: 2026-11-24. Follows `tgttos_elytra_boat_courses.md` (text-only pass) and owner feedback `Tgttos/FEEDBACK_2.md`
("too plain, big squares winding around, there's literally nothing"; wants pendulums, traps, 1-block gaps in big walls, paying hard lines).
Owner's later instruction (via coordinator): for **community maps** we may copy obstacles directly, so features from the downloaded maps
below are written down precisely enough to rebuild block for block. Official event maps (MCC, Glide) are studied only for ideas.

Evidence tags used throughout:
- **[DATA]**: measured by us from the downloaded world files (our own Anvil/NBT parser; command-block text read as data).
- **[VIDEO]**: seen by us in video frames (YouTube storyboard sheets and full-size player frames). Times are mm:ss in that video.
- **[IMG]**: seen by us in a map-page gallery image.
- **[INF]**: our inference or design proposal, not seen.

All images are in `research/img/tgttos_courses/` (34 files). File names start `render_` (our renders of downloaded maps),
`yt_` (video frames or storyboard contact sheets, the video id is in the name) or `pmc_` (gallery image).

---

## 0. How this was done (method and limits)

- **Browser:** the built-in browser reached Planet Minecraft (PMC), CurseForge and YouTube. PMC project pages and CurseForge pages were read
  through the browser. PMC's static image host refused `curl` (HTTP 403) and, after a few views, also the browser, so only one PMC gallery image
  was saved (Cosmic Cove). The rest of Part A comes from YouTube.
- **Video frames:** for each video, the YouTube storyboard sprite sheets (real frames every 1–10 s, 160×90 or 320×180) were fetched from
  `i.ytimg.com` and laid out as dated contact sheets (`*_storyboard.jpg`). Key moments were then viewed full-size in the player and saved
  (`*_tMMSS_*.jpg`). 15 videos, about 1,500 frames viewed in total.
- **Downloads (Part B):** 4 Java worlds, each into its own folder under the scratchpad `dl/courses/` (not in the repo). Only `region/*.mca`
  and `level.dat` were extracted. No archive held executables or scripts (checked the file lists: 0 `.exe/.jar/.bat/.ps1/.sh/.py/.js`);
  two held Forge `serverconfig/*.toml` text files, which were not extracted. Nothing was run or loaded into a server.
- **Analysis tools** (written by us, kept in the scratchpad `tools/`, run with `python -I`, paths passed as arguments): `anvil.py`
  (NBT + Anvil reader for 1.13 to 1.21 chunk formats; `scan`, `dump` to our MCMDUMP format, `cmds` = command-block and sign text),
  `topmap.py`, `gates.py` (finds cross-walls in corridor courses and measures every opening), `xray.py`, `find.py`, `ents.py`, `montage.py`.
  Renders use our own `tools/render_preview.py`. No pip installs; numpy and Pillow were already on the machine.
- **Blocked / skipped:** Elytra Racing League (Watson_inc, PMC-hosted) could not be fetched outside the browser (Cloudflare 403) and the
  browser download did not land on disk, so it was not studied. All other boat maps with stated-free terms on PMC are hosted on
  MediaFire / Google Drive / Dropbox mirrors; per the brief they were **not** downloaded (links recorded in section 1.3).

---

## 1. Per map studied

### 1.1 Elytra

#### E1. The Glider's Challenge, by Nordic_Studios [DATA]
- URL: https://www.curseforge.com/minecraft/worlds/the-gliders-challenge (file 5350522, "The Glider's Challenge.zip", 1.20.6)
- Terms as written on the page: **"License: MIT License"**.
- Studied: downloaded and parsed (7,562 chunks, y −64..9). Renders: `render_gliders_challenge_gate_types_grid.jpg` (12 levels with the
  long side walls removed so the gates show), `render_gliders_challenge_band12_plus_3x3_5x5.jpg`, `render_gliders_challenge_gate_walls.png`
  (every gate wall drawn face-on: light = open), `render_gliders_challenge_side_xray.png` (side x-ray of all 19 levels).
- Layout [DATA]: 20 levels side by side, each a straight **corridor 15 wide (z), ~186 long (x), ~70 tall**: floor of lava at y −61/−60, ceiling ~y 9.
  Corridor walls `light_blue_concrete`, 1 thick; corridors on a 17-block pitch (level centres z = −15, −32, −49 … −321).
  You start on a balcony at x ≈ 8..13, y 0, behind a full-width 15×13 window (blue wall with a sign), and dive in −x towards a finish wall at
  x ≈ −178 whose 15×25 opening sits at the bottom (y −64..−40). Between them, **2 to 4 gate walls, 40–50 blocks apart** (typically x = −28, −76, −124),
  each opening **8–10 blocks lower than the previous one**, so a level is one ~186-block dive at about 5:1.
- Mechanics [DATA, command blocks]: finishing a level runs `tp @p 13 0 <next level z>` (command blocks at x −179, y −37 under each finish);
  every second level has a "Checkpoint" sign and `spawnpoint @a 16 0 <z>`; a button at each start runs
  `give @p elytra{Enchantments:[{id:unbreaking,lvl:3}]} 1` ("Get a New Elytra"). Touching lava = death = back to the checkpoint.
- Every obstacle found (level number = order of play; x is the wall position; openings w×h, y = bottom of the opening):

| Level (z) | Wall block | Gate sequence in flight order [DATA] | Type |
|---|---|---|---|
| L01 (−15) | light_blue_concrete | x −85: full-width **15×7 slot** at y −18 | letterbox |
| L02 (−32) | light_blue_concrete | x −52: 15×5 slot y −13; x −111: 15×5 slot y −26 | letterbox, stepping down |
| L03 (−49) | light_blue_concrete | 15×5 slots at x −38 (y −13), −93 (y −27), −138 (y −21, i.e. back **up** 6) | letterbox, down then up |
| L04 (−66) | lime_terracotta | x −34 and −87: 4-wide piers on both sides, **9-wide full-height centre slot**; x −115: three 3-wide fins = **four 3-wide full-height slots** | slots, choose one |
| L05 (−83) | lime_terracotta | full-height **fins** leaving a 7-wide gap on alternating sides: x −22 gap right, −49 gap left, −79 **5-wide** gap right, −134 5-wide gap left | slalom fins |
| L06 (−100) | lime_terracotta | fins with **4-wide** gaps alternating: −22 left, −50 right, −73 left; x −108: 4-wide gap only 34 tall at the bottom (y −60..−27) | tighter slalom + low exit |
| L07 (−117) | yellow_concrete | x −28: **5×5** hole y −12; x −76: **3×3** hole y −20; x −124: **3×3 plus/cross** (5 cells) y −30 | shrinking holes |
| L08 (−134) | yellow_concrete | vertical **1-wide slits**: 1×7 (y −11), 1×5 (y −20), 1×3 (y −27) | shrinking slits |
| L09 (−151) | yellow_concrete | three **5×5** holes at y −11, −18, −34 (last one is a 16-block drop) | holes, steep line |
| L10 (−168) | yellow_concrete | x −28: 3×3; x −106: **1×1** at y −23 | pinhole |
| L11 (−185) | gray_concrete | 4 walls (x −28, −73, −117, −159) of **2×2 checkerboard** holes, full height | lattice |
| L12 (−202) | gray_concrete | 3 walls (x −23, −87, −140) of **1×1 checkerboard** (holes offset per row) | fine lattice (Super-Sonic style) |
| L13 (−219) | pink_concrete | x −23 and −99: **1-wide vertical bars, 1-wide gaps** (7 slots); x −61 and −139: two 5-wide full-height slots beside a 5-wide pier | bars / pier |
| L14 (−236) | **barrier** + red_wool + fire | invisible barrier walls with **fire rings**: x −124: wool ring 5 across, fire inside, **1×1 air centre** (y −32); x −76 and −28: wool ring 7 across, fire ring, **3×3 air centre** (y −22..−24 and −10..−12) | fire rings (exact pattern below) |
| L16 (−270) | pink_concrete | x −42: curtain hanging from the ceiling down to y −16 (fly **under**); x −88: wall from the lava up to y −15 (fly **over**); x −149..−88: **horizontal slab at y −36 with 5 round holes ~3 across** (drop **through** a hole), x −149 wall from the slab up (must be under the slab) | over / under / through-the-floor |
| L18 (−304) | pink_concrete | x −82: 1×2 hole y −30 | pinhole |
| L19 (−321) | pink_concrete | x −58: **1×1** y −15; x −103: **1×1** y −24 | two pinholes |
| L15, L17 | — | no blocks between start and finish (practice/free levels) | — |

  Fire-ring cross-section, L14 x −76 (rows top to bottom, 15 wide; b = barrier, W = red_wool, f = fire, . = air) [DATA]:
  ```
  bbbbbbWWWbbbbbb
  bbbbbWfffWbbbbb
  bbbbWf...fWbbbb
  bbbbWf...fWbbbb
  bbbbWf...fWbbbb
  bbbbbWfffWbbbbb
  bbbbbbWWWbbbbbb
  ```
  The 1×1 version (x −124) is the same with a 3×3 fire square around one air cell and a 5-across wool ring.
- Length/scale: 186 blocks per level, ~16 s per level at the slow glide the corridor forces [INF].

#### E2. "Elytra Racing!" (Elytra Race! version 1.5), by kingzonas (Jellyvish404) [DATA + VIDEO]
- URL: https://www.planetminecraft.com/project/elytra-racing-multiplayer-friendly/ (PMC-hosted download "Download Minecraft Map").
- Terms as written: **no project licence stated**; only the site footer ("All creations copyright of the creators"). Treated as acceptable
  for private study per the brief; copy only after the owner accepts that risk.
- Studied: downloaded and parsed; entities and 335 command blocks/signs read as text. Video FLgeTuN-WqY (63 s) storyboard viewed.
  Renders: `render_elytra_race_kingzonas_iso.jpg` (outside), `render_elytra_race_kingzonas_cutaway_iso.jpg` (roof removed above y 30),
  `render_elytra_race_kingzonas_top.png`.
- Layout [DATA]: one **enclosed greenhouse hall**, x −405..−215 (~190 long) × z −335..−262 (interior ~60 wide), half-cylinder oak/glass roof up to
  y ~57, ribs every ~25 blocks. Inside: terrain floor with hills, a central ridge (~10 wide, up to y 25) that the 3-lap loop circles, a
  rock arch/cave at the far end, a mushroom-and-tree "forest" with lantern posts at the near end, a minecart rail along one side.
  Flying height y 3–18, i.e. **always within 15 blocks of the floor**.
- Rings [DATA]: rings are **invisible marker armor stands** with particle circles drawn around them (tags `ringStand` + `SRing1` or `XSRing1`).
  10 per lap at (x, y, z): out leg z ≈ −312: (−385.6,17,−312) XS, (−354.4,16.5,−313.5) S+item, (−322.5,18.5,−319.5) S, (−289.5,10,−313) XS,
  (−253.4,10.5,−311.5) S+item, (−222.1,3.5,−297.1) XS; back leg z ≈ −280: (−265.4,16,−280) XS, (−290.5,10.5,−284.5) S+item, (−327.4,13.5,−276.6) S,
  (−371.4,12,−282) XS. Spacing 30–37 blocks; heights alternate 3.5–18.5, so the line rolls up and down.
- Mechanics [DATA, command text]:
  - Ring pass = player within **3 blocks of an S stand or 2 blocks of an XS stand** → `ringTimer 3`, chime, **+1 firework rocket**. Rockets come only from rings and lap finish.
  - **Landing grace**: while gliding `inAirTimer` is held at 25; on touching ground it counts down with warning bass notes at 21/14/7/1 ticks; at 0 you are out of the race. Smoke particles follow airborne players, flame particles grounded ones.
  - Lap checks: two check volumes (x −357..−352 and x −332..−327, full height) must both be crossed before the finish plane (x −240..−225, z −298) counts a lap; 3 laps.
  - **Pickups** on `itemStand` rings: item entities (NoGravity, respawned by a spawner block) of 1 or 3 snowballs.
  - **Snowball hit** within 2 blocks: `slowness 1 100` (1 s freeze) + `blindness 2 1`.
  - **Mines**: thrown splash potions are frozen in mid-air (`NoGravity`), shown with lava particles; a player within 2 blocks (or the potion touching a block) triggers `summon creeper … {ExplosionRadius:3b,Fuse:0,ignited:1b}` = an explosion knock-back trap that players lay for each other. A second "mine" type: tagged markers that, while armed, explode a radius-2 creeper when touched and puff squid ink.
- Scale [DATA + page]: lap ≈ 2 × 180 + 2 × 35 ≈ 430 blocks; author's 3-lap record 46.8 s → ~27.5 b/s average.

#### E3. Glide mini game (Mojang / 4J, Legacy Console Edition), 12 tracks [VIDEO]
- Source: "Minecraft Glide Minigame - Showcase of all 12 Maps", Rozz, https://www.youtube.com/watch?v=k5UNPIy57h0 (22:02), plus
  Cavern WR https://www.youtube.com/watch?v=FjDzrZiC95w and stampylonghead https://www.youtube.com/watch?v=3xWduLJ6iAE (sheets downloaded).
- Terms: official content, study only, no copying.
- Seen (storyboards `yt_glide_showcase_k5UNPIy57h0_storyboard_*.jpg`, frames `_t0100_`, `_t0202_`, `_t0431_`, `_t1331_`, `_t1732_`):
  - Every track is **enclosed or semi-enclosed**: caves, canyons with overhangs, temple naves, nether caverns, an ice cave, a ship in a sea cave. Scenery is within 3–15 blocks of the line almost all the time; open sky appears only in short canyon stretches (0:10–0:20, 4:30, 20:10).
  - **Boost/updraft volumes drawn as translucent yellow** "chevron" tunnels: yellow-and-grey zig-zag stripes lining a tunnel (1:00, 2:30, 7:40, 9:20, 14:50). [VIDEO]
  - **Checkpoint rings**: red-and-white ring at the top of a vertical shaft you fly up through (13:31); "Checkpoint!" banner, progress bar with section ticks at the bottom of the HUD, 3 hearts, timer, speed in m/s (e.g. 16.57, 25.80) and a ghost/rival time at the left.
  - Wall **fire niches**: braziers set into rows of 2×2 window niches in cave walls (2:02); flaming braziers in rows along a nave every ~6 blocks (17:32).
  - **Hanging chandeliers** / cages on chains over the line (3:00, 17:32), hanging lanterns (0:30).
  - Waterfalls and lava falls across or beside the line (0:30, 7:20, 14:00, 19:50); lava rivers under the line (7:10, 14:00 nether).
  - Giant set-piece statues the line passes (creeper head 13:20, dragon 6:10, chinese temple 5:10, library shelves 8:10/10:40).
  - Narrow slots between pillars, grates and portcullis-like bars (11:10–11:50: iron-bar/cobweb lattices in windows).
- Course length: a Cavern run is 33–60 s (WR 33.4 s, video FjDzrZiC95w).

#### E4. ElytraMC "Academy" (server map) [VIDEO]
- Source: https://www.youtube.com/watch?v=eOyQ1ZmP7kQ (69 s, WR 29.7 s). `yt_elytramc_academy_eOyQ1ZmP7kQ_storyboard.jpg`.
- Terms: server map, not downloadable; ideas only.
- Seen: indoor flight through a castle: corridors ~7–11 wide and 2–3 storeys tall, doorways and archways about 3–5 wide, a **round vertical shaft with concentric ring galleries** (0:03), library rooms with bookshelf walls, chandeliers, staircases you fly up. **A carpet stripe on the floor shows the route** and changes colour per section (red → purple → green, 0:10–0:34). Finish through a purple portal room.

#### E5. "The HARDEST Elytra Course in Minecraft" (Webble) [VIDEO]
- Source: https://www.youtube.com/watch?v=iY6IdbZqOJw (4:11). `yt_hardest_elytra_course_iY6IdbZqOJw_storyboard.jpg`.
- Seen: open-terrain course; **prismarine/diamond block rectangular frames** as gates; **flying through houses** (in through a door-sized hole, out the other side, 0:30–0:48); **bullseye target walls** (red/white concentric squares) you must pass through or hit (0:58–1:06); narrow cave entries (2:36, 3:18).

#### E6. "The Ultimate Minecraft Elytra Map" (Cosmo's Universe) [VIDEO]
- Source: https://www.youtube.com/watch?v=bKQL6kMWYlg (9:32). `yt_ultimate_elytra_map_bKQL6kMWYlg_storyboard.jpg`.
- Seen: floating-island terrain; **checkpoint pads = diamond-block platforms with a beacon beam**; strings of lanterns/blocks **dangling on chains** in open air (0:10–1:40); a dark ravine section lit by beacons; dying = respawn on the last pad.

(Also opened but not used beyond the storyboard: MCC Ace Race "Colorado" route ngiDlc7uz1s, official, ideas only.)

### 1.2 Ice boats

#### B1. Boat Rage, by pi314159265358978forge [DATA + VIDEO]
- URL: https://www.curseforge.com/minecraft/worlds/boat-rage (file 2821080, v73, 1.13.2).
- Terms as written: **"License: MIT License"**.
- Studied: downloaded and parsed (9,234 chunks); 1,131 command blocks/signs read. Video "Boat Rage, the ice boating map" FptDAGPoeUw (6:22) storyboard viewed.
  Renders: `render_boat_rage_iso.jpg` (everything above y 74), `render_boat_rage_level_y80_top.png` (one level), `render_boat_rage_bubble_elevator_tower.png`.
- Layout [DATA]: inside a snow dome, a disc ~360 across (x, z −180..180). **18 levels stacked between y 64 and y 154**, each one long
  **open ribbon of ice with no walls**: falling off = fail. Ribbon width measured by run length: mostly **5–11, peak 9**; the start of a level
  narrows to 1–2 wide wiggly chutes (top right of the y 80 render). Surfaces: `blue_ice` (127k) and `packed_ice` (54k) mixed; **lapis_block
  1-block kerb strips on the outside of bends** (dark lines in the y 80 render) as the only edge marking. A `sea_lantern` layer under the
  ribbons lights them from below.
- **Bubble-elevator towers** between levels [DATA], exact build (centre at (2, 64, −57)): shaft walls `stone_bricks` / `mossy_stone_bricks` /
  `cracked_stone_bricks`, interior per side: 1×1 column of `bubble_column` over a `soul_sand` block set into the ice, a `ladder` next to it,
  1-block centre wall, mirrored on the other side (section `#^L#L^#`, 7 wide), 14 high (y 64..77), soul sand capping both columns at the
  top (y 78). Players leave the boat, ride the bubble column (or climb the ladder) and get a new boat at the top. 19 towers (soul-sand pairs
  at y 49, 64, 72, 80, 88, 92, 99/100, 105/106, 110/111, 115, 119, 122, 130/131, 133, 141).
- Mechanics [DATA, command text]: **checkpoints** (`execute positioned … run scoreboard players set @a[distance=..N] Checkpoint N`; on a fall the
  nearest boat is teleported to the checkpoint and the player re-seated); "You have reached another level: #N" messages; **low-speed
  penalty**: a `Speed` score; if too slow while `Damage` is enabled the player gets damage and taunts ("Too slow!", "Hold W!", "Here is some
  motivational damage!"); lobby options for checkpoints ON/OFF and speed damage ON/OFF; boats with mob passengers as achievement rewards.
- Scale: the y 80 level alone is roughly 900 blocks of ribbon [INF from the render, 1 px = 1 block]; whole map ~10 min for a skilled player (page).
- Seen in video: the ribbons, the towers (2:05, 3:05 etc.: climbing shaft frames), constant open drops on both sides.

#### B2. Boats Race, by omemas12 [DATA]
- URL: https://www.curseforge.com/minecraft/worlds/boats-race (project 1148597, file 5941117).
- Terms as written: **"License: Academic Free License v3.0"**.
- Studied: downloaded and parsed. Render `render_boats_race_omemas12_iso.jpg`.
- Layout [DATA]: a small **water** (not ice) boat course on an ocean at x 225..300, z 240..300, sea level y 62: lanes **4–5 wide** walled by
  `jungle_wood` + `jungle_fence` (1 high above water), a second walled section in `polished_blackstone_brick_wall`, **stub walls jutting
  into the lane** (short spurs, dead-end side pockets) and **92 `magma_block` on the lane bed** under the blackstone section, i.e. downward
  bubble columns (whirlpools) that drag boats down [DATA for the blocks; effect on a boat in 1.21.1 is [INF], test]. Total lane ~250 blocks.
- No command blocks. A small, plain map; its useful ideas are the magma whirlpool strip and the spur walls.

#### B3. "I Built Minecraft's Most Dangerous Ice Boat Course" (Midmysticx, OUTCASTS SMP) [VIDEO]
- Source: https://www.youtube.com/watch?v=tbbKmPPQEy8 (23:31). `yt_dangerous_iceboat_tbbKmPPQEy8_storyboard.jpg` + 5 full frames.
- Terms: SMP build shown in a video, not downloadable; ideas only.
- Seen: **red-and-white striped kerbs**: 2-high walls of alternating `red_concrete`/`white_concrete` in 2-block diagonal stripes, and single-block
  striped kerbs on elevated ribbons (`_t0914_`, `_t1531_`); a **neon tunnel**: 9-wide ice under a black-and-white chequer ceiling with
  coloured light panels and fire on posts along both edges (`_t0731_`); a **cobweb field over lava** between striped walls (`_t0811_`,
  cobwebs slow everything to a crawl and lava is death); **elevated ice ribbons over a drop** looping round **giant planet spheres**
  (`_t1411_`, `_t1531_`); lava lakes beside and under the track, a nether section with lava falls (17:20–17:50), a stair section in a
  ruined castle (16:30–17:00), mountain tunnels (11:00–12:30). Race of ~6 boats at the end (19:00–22:30): position HUD "1ST/2ND", crashes into kerbs.

#### B4. "Minecraft ICE BOAT OBSTACLE COURSE! (Hardest map ever?)" (Preston) [VIDEO]
- Source: https://www.youtube.com/watch?v=wvuOHZKC6G4 (19:48). `yt_preston_iceboat_obstacle_wvuOHZKC6G4_storyboard.jpg`.
- Seen: packed-ice **ribbons 1–3 wide floating over a void** (red ground far below), zig-zags with 90° and 135° corners, ribbons narrowing to
  **1 wide**, stair-step drops, **glowing 3×3 checkpoint pads** (sea-lantern squares) every 30–60 s, "You died" on every fall.

#### B5. Mario Kart ice-boat racetrack (TheGalaxyJumper) [VIDEO]
- Source: https://www.youtube.com/watch?v=Ymt03N6X8xQ (4:05). `yt_mariokart_iceboat_Ymt03N6X8xQ_storyboard.jpg`.
- Seen: track cut through canyon walls; dark caves and tunnels with **lava pools along the edge** (1:12–1:40); **black-and-white chevron
  signs** on corner walls (0:12–0:18); a **booster strip** (yellow/orange stripe) in a tunnel (1:28); **blue-ice patches set into snow**
  (2:08–2:12, the off-line surface slows you); a white-framed **gate/portal** (3:10).

#### B6. Loop Lake and Cosmic Cove, by Bluleader64 [VIDEO + IMG]
- Loop Lake: https://www.planetminecraft.com/project/loop-lake-ice-boat-race-track/, video https://www.youtube.com/watch?v=QBpwJj8ADg4
  (`yt_loop_lake_bluleader64_QBpwJj8ADg4_storyboard.jpg`). Cosmic Cove: https://www.planetminecraft.com/project/cosmic-cove-ice-boat-race-track/
  (`pmc_cosmic_cove_bluleader64_crystal_cave.jpg`).
- Terms (Bluleader64 pages, paraphrased; exact wording not captured): public use requires crediting the author. Downloads are Google Drive mirrors, not taken.
- Seen: **no walls at all**: ice is flush with grass, coarse dirt and podzol verges; packed ice racing surface with **blue-ice inner lines**;
  **log fences and a long wooden bridge over a lake**; dense forest within 1–3 blocks of the edge; Cosmic Cove: a dark crystal cave with
  glowing purple/blue amethyst clusters on pillars and ceiling, reflective ice floor.

#### B7. PMC Ice Boat Racing Map, by The Lasersloth (Appleseed community) [VIDEO]
- https://www.planetminecraft.com/project/pmc-ice-boat-racing-map-1-17-1/, video https://www.youtube.com/watch?v=Qk1JTOyQaoU
  (`yt_pmc_iceboat_lasersloth_Qk1JTOyQaoU_storyboard.jpg`). Terms: none stated on page. Download is a MediaFire mirror, not taken.
- Seen: **cliff-ledge track** clinging to a sheer cliff with no wall on the drop side (1:18–1:30), **bridges** over gorges, a track threading
  **through a building** (0:52–1:00), a red-flower field, a dark cave with light dots (1:12), giant timber trestles (2:04–2:10); page mentions
  "YEET" jumps and soul-sand tricks.

### 1.3 Recorded but not downloaded (third-party hosts)
Glide Canyon Race (Lewmas) → Dropbox; Elytra Parkour Race (BigLittleWolf) → MediaFire; Boat Race Track (strawberrana) → MediaFire;
Cosmic Cove / Loop Lake / Racers Rainforest / Haliwood (Bluleader64) → Google Drive; Ardere Forest (LudoFax) → MediaFire; PMC Ice Boat Map,
Mount Akina, Ice Boat World (AdamsApple) → MediaFire; Rally Monza → MediaFire (.rar). Excluded as "All Rights Reserved": Elytra Fall, Pixel
Gliders, Sky Scapers, Minecraft Elytra Challenge, MineBoatRace, Ice Boat Madness, Ice Boat Racing (aturtle233), Darwin Raceway, Laguna Seca.

---

## 2. Obstacle and feature catalogue

Columns: what / how built (blocks, sizes) / moves or timed, and how / what it asks / difficulty (1 easy … 5 brutal) / source.

### 2.1 Elytra (34 items)

| # | Feature | Build (measured where possible) | Moving? How | Asks | Diff | Source |
|---|---|---|---|---|---|---|
| E-1 | Letterbox slot | full-width 15×5 or 15×7 slot in a 1-thick wall, ~70 tall wall | static | hold a height band | 2 | E1 L01–L03 [DATA] |
| E-2 | Stepping letterboxes | 2–3 letterboxes 45–55 apart, 13–14 lower each; last one 6 **higher** | static | dive, then pull up | 3 | E1 L02–L03 |
| E-3 | Choice of slots | 3-wide fins making four 3-wide full-height slots | static | pick a lane | 2 | E1 L04 x −115 |
| E-4 | Centre slot | 4-wide piers both sides, 9-wide full-height slot | static | centre line | 1 | E1 L04 |
| E-5 | Slalom fins | full-height fins leaving 7 → 5 → 4-wide gaps on alternating sides, 25–35 apart | static | weave | 3–4 | E1 L05–L06 |
| E-6 | Low exit | 4-wide gap only 34 high at the floor after a slalom | static | dive late | 4 | E1 L06 x −108 |
| E-7 | Shrinking square holes | 5×5 → 3×3 → 3×3 cross, each 8–10 lower, 48 apart | static | aim + glide angle | 4 | E1 L07 |
| E-8 | Shrinking vertical slits | 1×7 → 1×5 → 1×3 | static | roll-free precision | 5 | E1 L08 |
| E-9 | Hole with big drop | 5×5 holes at −11, −18, −34 | static | steep dive, speed control | 3 | E1 L09 |
| E-10 | **Pinhole 1×1** in a huge wall | 1×1 opening in a 15×70 wall | static | exact aim (Super-Sonic) | 5 | E1 L10, L19 |
| E-11 | 1×2 pinhole | 1 wide × 2 tall | static | exact aim | 5 | E1 L18 |
| E-12 | Lattice wall 2×2 | 2×2 checkerboard holes over the whole wall | static | any hole, keep level | 3 | E1 L11 |
| E-13 | Lattice wall 1×1 | 1×1 checkerboard, rows offset | static | exact | 5 | E1 L12 |
| E-14 | Bar curtain | 1-wide bars with 1-wide gaps, full height | static | exact lane | 5 | E1 L13 |
| E-15 | **Fire ring** | invisible `barrier` wall, `red_wool` ring 5 or 7 across, `fire` ring inside, 1×1 or 3×3 air centre (exact pattern in 1.1) | static (fire) | precise, burn if off | 4–5 | E1 L14 |
| E-16 | Curtain from the ceiling | wall from ceiling down to 44 above floor | static | fly under | 2 | E1 L16 |
| E-17 | Wall from the floor | wall up to mid height | static | fly over | 2 | E1 L16 |
| E-18 | **Drop-through floor** | 62-long slab across the corridor with 5 round ~3-wide holes; must end up beneath it | static | dive through a hole | 4 | E1 L16 |
| E-19 | Lava floor | lava 1 deep over the whole corridor floor | static | don't sink | 2 | E1 |
| E-20 | Enclosed hall course | ~190×60 greenhouse, roof ~55, flying 3–18 above terrain round a central ridge | static | low, close-quarter flying | 3 | E2 [DATA] |
| E-21 | Invisible proximity rings | marker armor stand; pass within 3 (S) or 2 (XS) blocks; particle circle | no blocks | aim at a point | 2–3 | E2 |
| E-22 | Rocket-on-ring | each ring gives +1 rocket; no other supply | — | ring collection = fuel | 3 | E2 |
| E-23 | Landing grace beeps | 25-tick grace after touching ground, beeps at 21/14/7/1 | timed | relaunch fast | 2 | E2 |
| E-24 | Snowball stun pickups | NoGravity item on a stand, 1 or 3 snowballs; hit = slowness 100 for 1 s + blindness 2 s | respawning pickup | PvP in the air | 3 | E2 |
| E-25 | **Aerial mines** | frozen splash potion or marker; touch = radius-3 creeper explosion (knock-back) | player-placed trap | dodge / lay traps | 3 | E2 |
| E-26 | Anti-shortcut lap checks | 2 invisible volumes (5 × full height × 28) that must both be crossed | — | no cutting | — | E2 |
| E-27 | Yellow boost/updraft tunnel | translucent yellow volume in a tunnel lined with yellow/grey zig-zag stripes | constant force | line up, get flung | 2 | E3 [VIDEO] |
| E-28 | Vertical shaft with checkpoint ring | narrow vertical shaft, red/white ring at the top | static | climb with boost | 3 | E3 13:31 |
| E-29 | Fire niches / brazier rows | 2×2 windows with fire in cave walls; braziers every ~6 along a nave | static scenery | framing | 1 | E3 2:02, 17:32 |
| E-30 | Hanging chandelier / cage | chains from the ceiling with a lantern cluster in the line | static (could swing [INF]) | avoid | 2 | E3, E6 |
| E-31 | Lava/water falls across the line | falling liquid columns in front of openings | static | pass between | 3 | E3 |
| E-32 | Fly-through house | building with door-sized entry and exit openings | static | precise entry | 3 | E5 |
| E-33 | Bullseye target walls | red/white concentric square target, hole in the middle | static | aim | 3 | E5 |
| E-34 | Route carpet stripe | carpet line on the floor, colour per section | static | wayfinding | — | E4 |
| E-35 | Beacon checkpoint pad | diamond/pad + beacon beam | static | respawn marker | — | E6 |
| E-36 | Swinging pendulum, rotating blades, sliding crusher, closing shutter | **not seen in any elytra map or video we opened** | — | — | — | needed for the owner's request [INF], see 4.3 |

### 2.2 Ice boats (30 items)

| # | Feature | Build | Moving? How | Asks | Diff | Source |
|---|---|---|---|---|---|---|
| B-1 | Open ribbon over a drop | ice 5–11 wide (peak 9), no walls, void or 15-block drop below | static | stay on line | 3 | B1 [DATA], B4 |
| B-2 | Narrow start chute | 1–2 wide wiggly ribbon for the first ~40 blocks of a level | static | low-speed precision | 4 | B1 [DATA] |
| B-3 | Lapis outside-bend kerb | 1-block `lapis_block` strip on the outer edge of bends, flush | static | read the bend | 1 | B1 [DATA] |
| B-4 | Blue/packed mix | blue-ice racing line inside packed-ice track | static | speed line | 2 | B1, B6 |
| B-5 | Underglow | sea-lantern layer 1 below the ice | static | readability | — | B1 [DATA] |
| B-6 | **Bubble-elevator tower** | 7-wide shaft: soul sand + 1×1 bubble column + ladder, ×2, 14 high, stone-brick mix (exact in 1.2) | constant | leave boat, ride up | 2 | B1 [DATA] |
| B-7 | New boat at the top | boat teleported/summoned to the level start | scripted | re-board | 1 | B1 [DATA] |
| B-8 | Checkpoint per level | score + teleport boat to checkpoint on fall | scripted | — | — | B1 [DATA] |
| B-9 | Low-speed penalty | damage + taunt when Speed score low | scripted | keep moving | 2 | B1 [DATA] |
| B-10 | Ribbon narrowing to 1 | 3 → 2 → 1 wide over the void | static | precision | 5 | B4 [VIDEO] |
| B-11 | Zig-zag 90°/135° corners | sharp corners on narrow ribbon | static | brake + turn | 4 | B4 |
| B-12 | Glowing 3×3 checkpoint pad | sea-lantern square in the ribbon | static | marker | — | B4 |
| B-13 | Striped 2-high kerb wall | red/white concrete, 2-block diagonal stripes | static | contain, read speed | 1 | B3 [VIDEO] |
| B-14 | Striped 1-block kerb on elevated ribbon | alternating red/white single blocks | static | edge read | 2 | B3 |
| B-15 | **Cobweb field over lava** | cobwebs filling a section between striped walls, lava under | static | crawl through or lose | 4 | B3 08:11 |
| B-16 | Neon tunnel | 9-wide ice, black/white chequer ceiling, coloured light panels, fire on edge posts | static | scenery | 1 | B3 07:31 |
| B-17 | Lava pools at the edge | lava beside the ice, flush | static | don't slide off | 3 | B3, B5 |
| B-18 | Planet set-pieces | giant spheres the ribbon loops round | static | scenery | — | B3 14:11 |
| B-19 | Ruin stair section | castle stairs (on foot?) | static | — | 2 | B3 16:30 |
| B-20 | Chevron corner signs | black/white diagonal stripes on corner walls | static | read corner | — | B5 |
| B-21 | Booster strip | yellow stripe in a tunnel (likely blue ice under a marker) | static | line up | 2 | B5 [VIDEO], [INF] for build |
| B-22 | Blue-ice patches on snow | off-line patches; snow slows | static | stay on line | 2 | B5 |
| B-23 | Flush verges, no walls | ice flush with grass/podzol/coarse dirt | static | punish cutting | 2 | B6 |
| B-24 | Lake bridge | long wooden deck across water, log fences | static | narrow straight | 2 | B6 |
| B-25 | Crystal cave | dark cave, amethyst clusters on pillars | static | scenery | — | B6 Cosmic Cove |
| B-26 | Cliff ledge | track on a cliff face, drop side open | static | fear + precision | 3 | B7 |
| B-27 | Through-building | track passes through a building | static | scenery | 1 | B7 |
| B-28 | Magma whirlpool strip | `magma_block` bed under water lane (downward bubble columns) | constant | avoid strip | 3 | B2 [DATA]; effect [INF] |
| B-29 | Spur walls / pockets | short walls jutting into a 4–5 wide water lane | static | dodge | 2 | B2 [DATA] |
| B-30 | Moving barrier, piston puncher, sliding barge, rotating arm | **not seen in any boat map or video we opened** | — | — | — | needed for the owner's request [INF], see 4.4 |

---

## 3. How route choice and scoring were rewarded in what we saw
- **Rockets as the reward** [DATA E2]: the only way to get fireworks is to pass rings; the small XS rings (2-block radius) pay the same
  rocket as S rings but sit on the faster line. Glide (E3, from the earlier text research) scores smaller rings higher.
- **Pickups on the ring line** [DATA E2]: snowballs at three of the rings, so the line that collects them gives you weapons.
- **Penalties instead of points** [DATA B1, E2]: Boat Rage damages slow drivers; Elytra Race gives 1.25 s to relaunch after a landing, then removes you.
- **Checkpoints per level** [DATA B1, E1]: progress is banked every level or every second level.
- **Nothing we saw gives a points bonus for a harder line** in a race. The bonus scheme in 4.1 is our design [INF].

---

## 4. Upgrade plan for Kite Festival and Frozen Canal (course lines unchanged)

### 4.1 Points for colour-coded lines (both maps)
Our scoring (`Tgttos/SPEC_v2.md`): finishing 8 for 1st, −1 per 5 places, minimum 1; top-10 bonus 16..7. So 1st = 24, 10th = 14, 11th = 7,
20th = 5, 40th = 1; one place is worth ~1–2 points in the top 10 and ~0.2 below.
Proposal [INF]:
- **Line gates** (passing detected by segment-vs-plane, as before): lime 0, **orange +1**, **red +2**, **pinhole / fire ring / 1-wide slit +3**.
- **Cap +10 per round**, paid **only if you finish** (otherwise hard-line farming beats finishing). +10 is roughly the gap between 10th and 3rd.
- Show "+2 HARD" in the action bar and a sidebar total; keep a per-round "line points" column.
- Frozen Canal uses the same scale: causeway +2, 3-wide barge gap +2, inside-kerb chicane gate +1, piston gauntlet clean pass +3.
- Elytra rocket economy from E2 as an extra reward: **red and pinhole gates also give +1 rocket**.

### 4.2 New mechanics needed (flags)
| Mechanic | Used by | Notes |
|---|---|---|
| **M1 Gate-bonus regions** | both | plane per gate with value; reuse the ring plane test |
| **M2 Checkpoints / respawn** | both | per-segment respawn (Kite on ledges, Canal on a re-boarding pad); Gliders/Boat Rage pattern |
| **M3 Moving hazard entities** | Kite | block_display groups animated by transformation interpolation; plugin does the collision test (oriented box vs player box each tick) and applies knock-back + `setGliding(false)` |
| **M4 Timed block toggles** | both | fill/clear block sets on a cycle (shutters, gates, stepped arms); real blocks, so boats and gliders collide naturally |
| **M5 Piston/redstone pushers** | Canal | vanilla pistons in walls; push boats sideways [U: test on Paper 1.21.1] |
| **M6 Refill rings** | Kite | +1 rocket on pass (E2's ringTimer pattern) |
| **M7 Boat depots** | Canal | after a fall/elevator, give the racer a fresh boat (Boat Rage tower + new boat) |
| **M8 Pickups** | both | snowball stands (E2), aerial mines (E2) as optional chaos items |
| **M9 Updraft / boost volumes** | Kite | plugin velocity on gliding players only (never on boats) |

### 4.3 Kite Festival, segment by segment (14 segments; canyon interior ~37 wide, banner walls)
Moving/timed hazards in **bold**: 6 (requirement ≥4).

| # | Segment | Add (catalogue items; "rebuild" = copy exactly) | Mech |
|---|---|---|---|
| 1 | Launch Gantry | Fire niches (E-29) in the gantry back wall; snowball stand on the deck (E-24) for the start brawl | M8 |
| 2 | Drop Gate Run | Replace the two plain 11s with **rebuild E1 L02–L03 letterboxes** at canyon width: 37-wide × 7-tall slots, stepping 13 down then 6 up; keep the third gate's fast 7 | M1 |
| 3 | Canyon Slalom | **Rebuild E1 L05–L06 slalom fins** scaled to the canyon: full-height fins from the alternating walls, gaps 9 (lime) / 7 (orange) / 4 (red) every 30. **Swinging pendulum #1**: 5×5×5 wrecking ball (polished blackstone + chain displays) hung from a bridge over the 3rd gap, ±60°, 4 s period | M3 |
| 4 | Turn 1 | Hairpin ceiling of hanging chandeliers/kites on chains (E-30) every 8 blocks at 8–20 below the rim; one ledge gets a fire-ring (E-15) shortcut through the inside wall, worth +3 | M1 |
| 5 | Tunnel Split | Safe 11×11 tube gets **rebuild E1 L11 2×2 lattice** at the exit; fast 5×5 tube gets **sliding crusher**: two wall slabs closing from the sides on a 3 s cycle (open 2 s) | M4 |
| 6 | Lift Hill | Yellow boost/updraft tunnel (E-27) look: zig-zag yellow/grey wall stripes + yellow translucent glass panes as the updraft column; **rotating fan blades** (4 blades, 2×12, 8 s/rev) in front of the fast 6s | M9, M3 |
| 7 | Ring Stack | Low 11 / mid 7 / high **rebuild E1 L14 fire ring (3×3 air centre) and 1×1 fire ring** as the red option; refill ring on the mid | M6 |
| 8 | Tether Forest | Masts become **rebuild E1 L13 bar curtains** (1-wide bars, 1-wide gaps) for the red line between two mast rows; dangling lanterns on chains (E-30) every 6 blocks | M1 |
| 9 | Turn 2 | **Swinging pendulum #2** across the inside line (timed so the inside is open 50%); aerial mines (E-25) as optional items | M3, M8 |
| 10 | Dive & Pull-up | **Rebuild E1 L16 drop-through floor**: a 60-long deck across the canyon at mid height with 5 round 3-wide holes; under it, the pull-up through **rebuild E1 L07 shrinking holes** (5×5 → 3×3 → cross) | M1 |
| 11 | Needle | The three 4×4s become **one 37×70 wall with a 1×1 pinhole** (owner's "one-block gap in a massive wall", E-10), then a **1×3 slit** (E-8), then a **closing shutter**: 4×4 portcullis open 2 s / closed 1 s. Safe detour unchanged | M4, M1 |
| 12 | S-Canyon | Lava falls (E-31) from the rim at both bends, 3 wide, leaving 5–7 gaps; a fly-through house (E-32) on the inside of the second bend for the fast line | — |
| 13 | Turn 3 | **Rotating windmill gate**: 4-arm kite wheel (arms 2×15) turning round the orange 7-ring, 10 s/rev | M3 |
| 14 | Finish & Field | Bullseye target-wall finish frame (E-33); snowball stands on the hay bales | M8 |

Scenery fill for the "plain" banner walls: every 25 blocks a rib/arch across the canyon (E2 greenhouse ribs), brazier rows (E-29), route
carpet colour on the canyon floor per line (E-34), hanging kites on chains. Aim: something within 10 blocks of the line at all times (E3).

### 4.4 Frozen Canal, segment by segment (12 segments; lanes 11–15, brick walls)
Moving/timed hazards in **bold**: 5 (requirement ≥4). Boats only meet **real blocks** (M4/M5); no server velocity on boats.

| # | Segment | Add | Mech |
|---|---|---|---|
| 1 | Grid Straight | Striped red/white kerb tops on the brick walls (B-13); a sea-lantern 3×3 pad at the line (B-12) | — |
| 2 | Mill Hairpin | **Mill wheel sweeper**: block arm (oak planks + stripped log, 1 high, 6 long) rotated in 8 steps by block toggles, sweeping the inside half of the hairpin every 4 s | M4 |
| 3 | Weavers' Chicane | **Piston gauntlet**: 6 sticky pistons per wall at boat height, firing in a ripple 1 s apart; clean pass +3; chevron signs (B-20) on the walls | M5, M1 |
| 4 | Cellar Hairpin | Cobweb pocket (B-15) on the inside apex (3×5 cobweb on snow), so the inside line is a gamble; lava-light fire niches in the cellar walls | — |
| 5 | Cellar Steps | Lapis outside-bend kerbs (B-3) at each step; underglow (B-5) under the step lips | — |
| 6 | Fish Market split | The 5-wide causeway (fast) gets a **lock gate**: iron-bar portcullis across the causeway, open 3 s / closed 1.5 s; walled ring (safe) unchanged; causeway pass +2 | M4, M1 |
| 7 | Brewery Bend | Narrowing like B-10 on the inside kerb: an open-edged 3-wide inside ribbon over the canal (fall = water, slow), +1 | M1 |
| 8 | Blue Mile | Under the three arch bridges: **swinging lantern pendulum** (block_display, visual) and a **timed ice/water toggle** in the outer lane: a 5×20 frosted-ice strip that turns to water for 2 s every 5 s (8 b/s when water); neon-tunnel treatment (B-16) under the middle bridge | M4 |
| 9 | Bell Switchbacks | **Swinging bell** under the footbridge as scenery; flush-verge section (B-23) on one hairpin: replace one wall with a 1-block lapis kerb and a drop to a lower canal (fall = re-board) | M2, M7 |
| 10 | Tannery Sweeper | Soul-sand and cobweb patches on the apex (B-15); magma whirlpool strip (B-28) where the lane crosses water [U] | — |
| 11 | Lantern Bend | Lantern chandeliers on chains over the lane (scenery); striped single-block kerb (B-14) | — |
| 12 | Barge Basin + finish | **Sliding barges**: two barge hulls (spruce + dark oak, 3×9) shifted 1 block per 10 ticks by block toggles, opening/closing the 3-wide fast gaps; gap pass +2; finish through a chequer gantry | M4, M1 |

Boat depots (M7): one at the Cellar Steps and one after the Bell Switchbacks (rebuild the B1 bubble-elevator tower if a fall drops racers to
a lower channel: soul sand + 1×1 bubble column + ladder, 7-wide stone-brick shaft, new boat at the top).

### 4.5 What to copy exactly, and from where
| Feature | Map, coordinates | Terms |
|---|---|---|
| Letterbox, slalom fins, shrinking holes, slits, pinholes, lattices, bars, fire rings, drop-through floor | The Glider's Challenge, levels at z −15 … −321, gate walls at the x listed in 1.1 (corridor x −178..8, y −61..9) | MIT |
| Ring/rocket/landing-grace/snowball/mine command logic | Elytra Race v1.5 (kingzonas), command blocks at x −214..−198, y 1–7, z −328..−290 | none stated |
| Bubble-elevator tower, lapis bend kerbs, open ribbons, low-speed penalty | Boat Rage, tower at (0..4, 64..78, −57), level at y 80 | MIT |
| Magma whirlpool strip, spur walls | Boats Race, x 225..300, z 240..300, y 50..63 | AFL 3.0 |

Credit lines for MIT/AFL content go in the map's credits file [INF].
