# TGTTOS: elytra course and ice boat race research

Research date: 2026-11-24. Target: Paper 1.21.1, one ~100 s "get to the other side" round, up to ~40 players who can punch each other.
Method: web pages only (wiki pages, map listing pages, plugin pages, bug trackers). No map files were downloaded or run.
Several map hosts (Planet Minecraft, minecraftmaps.com) refused automated fetches (HTTP 403), so details from those come from search-engine summaries of the pages and are marked as such.

How to read this file:
- **[R]** = researched, with a source linked inline.
- **[D]** = derived by us from researched numbers or from the vanilla game code as we understand it. Treat it as a calculation to check, not a fact.
- **[U]** = uncertain or unverified; test in game before relying on it.
- Parts 1 to 3 cover elytra, parts 4 to 6 cover boats. Each category ends with "Our recommendation".

---

## Part 1: Elytra flight physics (researched)

| Quantity | Value | Source |
|---|---|---|
| Best glide ratio (pitch 0°, aiming at the horizon) | ~10.06 blocks forward per block of height, not counting the time to build speed | [R] [minecraft.wiki Glide](https://minecraft.wiki/w/Glide) |
| Lowest sink rate | ~1.5 m/s at ~12–15° nose-up (slow flight) | [R] [minecraft.wiki Glide](https://minecraft.wiki/w/Glide) |
| Minimum airspeed / stall | ~7.2 m/s at 30° nose-up; at 60° nose-up you stall hard enough to take fall damage | [R] [minecraft.wiki Glide](https://minecraft.wiki/w/Glide) |
| Firework boost speed | 33.5 blocks/s in the facing direction (another wiki page says 35.5) | [R] [minecraft.wiki Glide](https://minecraft.wiki/w/Glide), [Kinetic energy](https://minecraft.wiki/w/Kinetic_energy) |
| Boost length | "roughly 10 × (2 + flight duration) game ticks", so ~1.5 / 2.0 / 2.5 s for flight 1/2/3 | [R] [switchbladegaming elytra guide](https://www.switchbladegaming.com/minecraft/elytra-guide/); our reading of the code is 10×(flight+1) + rand(0..5) + rand(0..6) ticks, i.e. flight 1 ≈ 1.0–1.55 s [U] |
| Distance per rocket | ~50 blocks (flight 1) to ~84 blocks (flight 3) of boosted flight, plus the glide afterwards | [R] search summary of calculators (formula × 33.5 b/s) |
| Wall collision (kinetic) damage | damage = 10 × (change in horizontal speed in blocks/tick) − 3. Damage starts at a 0.3 b/tick (6 b/s) change; a 2.3 b/tick (46 b/s) head-on stop deals 20 HP | [R] formula from [Kinetic energy](https://minecraft.wiki/w/Kinetic_energy); thresholds [D] |
| Landing damage | landing steeper than ~50° hurts | [R] [minecraft.wiki Glide](https://minecraft.wiki/w/Glide) |
| "Super Sonic" | flying through a 1×1 gap at >40 m/s is possible (advancement) | [R] same |
| 1.21.1-relevant version notes | 1.21.5 made climbable blocks cancel gliding, and 1.21.11 allowed gliding through vines. **Neither applies to 1.21.1.** | [R] [minecraft.wiki Glide](https://minecraft.wiki/w/Glide) |

**Steady glide speed at pitch 0° [D, U].** No source gives this number directly. Working through the vanilla gliding code as we remember it (gravity 0.08 scaled by (−1 + 0.75·cos²pitch), 10% of the downward speed turned into forward speed, drag ×0.99 / ×0.98 per tick) gives a steady state of about **33 b/s horizontal and ~3 b/s sink**. That is a ratio of ~11:1, which agrees with the wiki's 10:1. Speed builds up with a time constant of about 5 s. Two things follow:
- An unboosted expert holding a level line flies at roughly **30 b/s**, about the same as rocket speed. Rockets matter most for regaining speed after climbs, tight turns and crashes, and for climbing.
- Diving trades height for speed above 40 b/s. Climbing through a ring that is 20 blocks higher costs a lot of speed.
- The first version of our map was finished in ~10 s, which at ~30 b/s means only ~300 blocks of flight. We need roughly **4–5× the path length**.

---

## Part 2: How elytra courses are built (researched)

### 2.1 Reference courses and minigames

| # | Name / author | Where | What is structurally notable | Licence / terms (as written on page) |
|---|---|---|---|---|
| 1 | **Glide** (Mojang / 4J, Legacy Console mini game). Tracks: Cavern, Temple, Canyon (free) plus 9 DLC tracks | [minecraft.wiki Glide mini game (mirror)](https://wiki.ronlab.site/content/minecraftwiki_en_all_maxi_2025-11/Glide_mini_game), [Score Ring](https://minecraft.wiki/w/Score_Ring) | The official reference design. Time attack and score attack modes. **Rings: green 9×9, yellow 7×7, blue 5×5** (smaller ring = more points). Checkpoints marked by **two beacon beams**. 3 hearts: a wall hit costs 1 heart, **touching the ground = instant death**, respawn at the last checkpoint after a few seconds. Speed boosts and updrafts scattered on the maps ([fan page](https://sonicblaston62.github.io/Minecraft4J/Mini_Games.html)). 8–16 players. | Official content, not a template |
| 2 | **Glide Mini Game remake** (Bedrock) by "Glide Mini Game Team" | [CurseForge](https://www.curseforge.com/minecraft-bedrock/maps/glide-mini-game) | Recreates Cavern/Temple/Canyon. **Orange boost pads push you in whatever direction you face; yellow pads only push when you face the pad's direction** (directional boosts). Checkpoints "secure" score. | "All Rights Reserved" |
| 3 | **Elytra Rush** by TheblueMan003 | minecraftmaps.com (403 to fetcher; from search summary) | 12 levels of rings, each level timed. **Some rings hand out 1–2 fireworks to reach the next ring**, so rocket supply paces the course. **Hitting something adds 10 s** (time penalty instead of reset). Online leaderboard. | Not retrieved |
| 4 | **Dimensional Rush** by TheblueMan003 | [minecraftmaps.com](https://www.minecraftmaps.com/parkour-maps/dimensional-rush) (search summary), [minecraft-france](https://www.minecraft-france.fr/?p=184220) | Rings across several dimension themes: **gold rings give a firework, diamond rings give a speed boost**. Colour carries the meaning. | Not retrieved |
| 5 | **Elytra Fall** by pi314159265358978forge | [CurseForge](https://www.curseforge.com/minecraft/worlds/elytra-fall) | Hub with 16 tunnels, **each tunnel ~1.7 min**. Ends with a chest hanging from the ceiling that you grab mid-flight. Difficulty via the game difficulty setting. Shows that **enclosed tunnels** give long, readable flights. | "All Rights Reserved" |
| 6 | **ELYTRA RACE** by BakuTheBuilder (Bedrock) | [CurseForge](https://www.curseforge.com/minecraft-bedrock/maps/elytra-race-bakuthebuilder) | Multiplayer race: "multiple racing gates", 1v1 and Red vs Blue modes, solo practice. | "All Rights Reserved" |
| 7 | **Pixel Gliders – Elytra Challenge** by kingzustin (design) and Dragonze (build) | [Planet Minecraft](https://www.planetminecraft.com/project/pixel-glide/) (search summary) | 3 themed obstacle levels (Falling Lava Hallway, Mage's Tower, Mining Operation). Obstacle-dense indoor flying. | Not retrieved (PMC 403) |
| 8 | **Temple Glide** port, original by Cuwwa | [MCPEDL](https://mcpedl.com/temple-glide-map-minecraft-glide/) | Race "to the end of the course using Elytra and Fireworks". An update **added barriers where players could fly off course**, so out-of-bounds control was a real problem. | None stated |
| 9 | **MCC Ace Race** (Noxcrew) elytra section | [MCC wiki](https://mcchampionship.fandom.com/wiki/Ace_Race_Movement_Mechanics) (via search summary) | An elytra checkpoint gives the elytra, and it is **taken away the moment the player lands on something**. One data point only. | n/a |
| 10 | **MCC/Munchy hall-map guide** (fan event rules) | [munchymc forum guide](https://munchymc.com/forums/t/guide-making-a-championship-hall-map/62848) | Elytra race rules: players get **32 fireworks** and the map must be beatable with that "with some to spare". **Hitting any block past the departure point sends you back to the start.** Departure point marked by an iron pressure plate, finish by red stained glass. **Target ~2 min.** Ring stacks: three rings on different layers, **bigger lower down**. | Forum guide |
| — | Plugins (design hints): [Ely-sky](https://hangar.papermc.io/SunflowerAges/Ely-sky), [ElytraRace](https://modrinth.com/plugin/elytrarace) | — | Ely-sky: configurable ring sizes, **boost portals with adjustable strength**, timing from ring 1, **missed checkpoint → short recovery window then restart**. ElytraRace: boundary anti-cheat plus a race timeout. | Plugins, not maps |

None of these is a direct template for us: most say "All Rights Reserved" or nothing, and none fits a 40-player, 100 s point-to-point round. We take only their structural ideas.

### 2.2 What the sources agree on

- **Length/time [R]:** a full race takes ~2 min (Munchy guide), Elytra Fall tunnels ~1.7 min each, and Glide records count only under 5 min. A 60–90 s median for our round is shorter than these, which is right for one TGTTOS round.
- **Ring sizes [R]:** the only hard numbers found are Glide's **9×9 / 7×7 / 5×5** outer sizes. With a 1-block frame, those openings are about 7 / 5 / 3 blocks [D]. The smallest ring is a "hard, high-reward" ring, not the main line.
- **Fireworks [R]:** either a fixed stock (32 in the Munchy rules, generous) or **ring-dispensed rockets (1–2 per ring)** in Elytra Rush and Dimensional Rush. Ring dispensing gives tighter pacing and makes collecting rockets part of the route.
- **Boosts and updrafts [R]:** Glide uses speed boosts and updrafts, with directional and omnidirectional boost pads in the remake. Ely-sky has boost portals. Dimensional Rush uses diamond-ring boosts. Updrafts let a long course regain height without a huge start tower.
- **Failure [R], three models:**
  1. Instant reset to start (Munchy rules). Too harsh for 100 s.
  2. Respawn at the last checkpoint, with hearts for wall hits and instant death on the ground (Glide).
  3. Time penalty, +10 s per hit (Elytra Rush).
  Model 2 fits a race where finishing order matters.
- **Readability [R]:** checkpoint beacons (Glide), colour-coded rings by size or function (Glide, Dimensional Rush), a distinct finish material (red glass, Munchy), barriers to stop off-course shortcuts (Temple Glide).
- **Route choice [R, partial]:** Glide's ring colours sit side by side as risk/reward choices, and Munchy's stacked rings get bigger lower down. We found no source with numbers for a "safe wide vs fast tight" split. The split below is our design.

### 2.3 What we could not find
- Measured course lengths in blocks or expert times for any community elytra map (no video data was available to the fetcher).
- Gap spacing numbers. Our spacing below comes from flight speed: 30 b/s ≈ 1.5 blocks per tick.

---

## Part 3: Elytra course, our recommendation

### 3.1 Scale [D]
- Expert average speed on a ringed course with turns, climbs and a few rockets: **~30 b/s**. Expert target 35–42 s → **path length ~1,300–1,400 blocks**. We pick **1,350 blocks**.
- Median player: flies at ~22–25 b/s (nose-up, over-correcting, missing rings) and has 1–2 crashes at ~8 s each → **~70–85 s**.
- Weak player: 3–5 crashes, or runs out of height on the safe line → sometimes **> 100 s** (DNF).
- **Height budget:** a level glide sinks ~3 b/s, so 40 s of gliding costs ~120 blocks, and turning and diving cost more. Start at **Y 300** and finish at **Y ~90**. That is a 210-block drop over 1,350 blocks (6.4:1 average, well under the 10:1 best), plus **two updrafts (+40 to +50 Y each)** so the safe line never runs out of height without rockets. Rockets are for speed, never required.

### 3.2 Segment plan (12 segments)

| # | Segment | Path length | Y (in → out) | Openings (inner, blocks): safe / fast | Notes |
|---|---|---|---|---|---|
| 1 | **Launch deck** | — | 300 | — | 40 players on a ~20×20 deck with a 3-wide edge funnel. Elytra are equipped during the countdown, players jump off on "GO". **Main punching moment:** shoving at the funnel and knocking people off early or late. |
| 2 | **Drop gate run** | 120 | 300 → 255 | 11 / 11 | 3 big lime rings in a straight dive to teach the line and build speed (the dive lets everyone reach ~30 b/s). First rocket refill ring at the end (+1). |
| 3 | **Canyon slalom** | 180 | 255 → 235 | 9 / 7 | Alternating left/right gates spaced 35–45 blocks with 15–25 blocks of lateral offset (about 25–35° heading change per gate). The fast line cuts the inside on smaller orange rings. |
| 4 | **Checkpoint ledge A** | 60 | 235 → 230 | 11 | Wide ring plus a beacon-marked respawn ledge on the canyon wall. **Respawns land here on foot, so punching happens here.** |
| 5 | **Cave split** | 200 | 230 → 205 | safe tunnel 11×11 (lit, longer by ~25 blocks) / fast tunnel 5×5 with 2 bends | Enclosed flight like Elytra Fall. Bright contrasting walls. A wall touch inside the fast tunnel means a crash. |
| 6 | **Updraft shaft 1** | 40 | 205 → 250 | 9×9 shaft | Vertical column with particle markers. The plugin adds upward velocity to gliding players inside it. Exit through a ring at the top facing the next segment. |
| 7 | **Ring stack** | 100 | 250 → 235 | 3 stacked rings: low 11 / mid 7 / high 5 | Munchy-style stack, bigger lower down. The high small ring keeps your height for segment 8 (fast line). The low one costs height, which the safe line has spare. Refill ring +1. |
| 8 | **Pillar forest** | 180 | 235 → 200 | gaps 7–9 / 5 | Weaving between basalt or deepslate pillars with lit caps. Checkpoint B (beacon) at the exit. |
| 9 | **Dive & pull-up** | 150 | 200 → 165 → 190 | 9 / 7 | Dive under a natural arch, then climb through 3 rising rings (+25 Y). Tests speed management: diving too deep means you can't make the climb without a rocket. |
| 10 | **Updraft 2 + boost ring + needle** | 100 | 190 → 220 → 200 | 9 / 4×4 needle | Short updraft, then a directional boost ring (Glide's yellow-pad idea: it only boosts if you're aligned), then a "needle" of 3 small gates for experts. The safe line detours around them through 9-wide rings (+30 blocks). Checkpoint C. |
| 11 | **Final S-canyon** | 150 | 200 → 120 | 9 / 7 | Two linked 70° turns with the canyon floor rising beneath you, so a low line forces an early landing (a crash). |
| 12 | **Finish gate & landing field** | 70 | 120 → 90 | 15×15 finish plane | Red-glass finish frame (Munchy). Crossing the plane finishes you. Then a wide landing meadow. Finished players can loiter on foot (and punch latecomers who land). |
| | **Total** | **~1,350** | 300 → 90 | | 3 checkpoints, 3 refill rings, 2 updrafts, 1 boost ring |

### 3.3 Rings, spacing, readability
- **Opening sizes:** 11 intro, 9 standard (the safe line), 7 fast, 5 hard, 4×4 needle for experts. Use 1-block-thick frames. Glide's 9/7/5 outer sizes map closely onto our 9/7/5 openings plus a frame.
- **Detection:** at 30–45 b/s a player moves 1.5–2.3 blocks per tick, so a ring is crossed in a single tick. Detect ring passes by **segment-versus-plane intersection** between the previous and current position, not by "player is inside a block region" [D].
- **Spacing:** 30–60 blocks (1–2 s) between consecutive gates. Never put a gate within 15 blocks after a turn of more than 45°. The next ring must be visible through the current one.
- **Colour code (one meaning per colour):** lime = 11/safe, yellow = 9, orange = 7, red = 5/needle, light blue = rocket refill, magenta = boost, beacon beam = checkpoint, red glass = finish. Frames use glowing blocks (sea lantern / shroomlight / glowstone) against dark terrain such as deepslate, blackstone or spruce. Avoid white rings against bright sky. Place particle arrows or a beam at each route split.
- **Out of bounds:** a barrier ceiling at ~Y 315 and barrier walls on canyon rims where a cut would skip rings, as the Temple Glide update did. Rings must be passed in order. A missed ring means "go back"; we don't kill the player for it (Ely-sky recovery-window idea).

### 3.4 Fireworks
- **3 rockets at start (flight duration 1)**, **+1 at each of the 3 refill rings**, and **+1 granted on respawn at a checkpoint**. Cap the inventory at 4.
- A full rocket budget of ~6 × 1.3 s ≈ 8 s of boost is a nice extra for experts, not a carry. Total course time drops ~10–15% with perfect rocket use [D].
- Don't hand out 32 rockets as the Munchy rules do. Spamming rockets turns the course into a straight-line flight and was likely part of why v1 took 10 s.

### 3.5 Failure handling
- **Crash** = the player stops gliding while not over a checkpoint ledge or the landing field (`EntityToggleGlideEvent` to false), touches water or lava, or leaves the bounds. Response: 1 s blackout, then teleport to the last checkpoint ledge on foot, re-equip the elytra and give +1 rocket. The player re-launches by jumping off the ledge.
- Effective penalty ≈ 6–10 s. That is Glide's model with the ground as death and checkpoints as respawn points.
- Cancel all fall and kinetic damage, so the crash itself is the penalty.
- Optional "graze" leniency: a wall touch that doesn't end the glide is free. It costs speed anyway (kinetic physics) [D].

### 3.6 Where punching happens (realistic) [D]
- **Gliding:** punching is possible (3-block reach, knockback applies to gliding players), but riders move at similar speeds along a 1,350-block path and rarely come within reach. Expect occasional hits only at the start dive and in ring stacks. Don't design around mid-air fights.
- **On foot:** the launch deck funnel (main choke), the checkpoint ledges (crashers respawning in groups and shoving each other off the ledge), and the landing field.
- Optional extra: make the launch "dock" a narrow 3-wide pier so the first 5 s is a shoving match.

### 3.7 Target times [D]
| Player | Path | Crashes | Est. time |
|---|---|---|---|
| Expert, fast line, rockets used well | ~1,250 (fast lines cut ~100) | 0 | **35–42 s** |
| Median, safe line | ~1,400 | 1–2 | **70–85 s** |
| Weak | ~1,400 | 3–5 | **95–120 s (some DNF)** |
Calibrate after the first owner or bot run. If the owner beats 30 s, extend segments 3 and 8, or shrink the fast-line openings by 1.

---

## Part 4: Boat physics (researched, Java)

| Quantity | Value | Source |
|---|---|---|
| Top speed: water | 8.0 b/s | [R] [minecraft.wiki Boat](https://minecraft.wiki/w/Boat) |
| Top speed: ice / packed ice / frosted ice | **40 b/s** (2.0 b/tick) | [R] same; confirmed by [Craftdex](https://craftdex.net/articles/boats-with-chests-and-ice-travel) and the [Fandom Blue Ice](https://minecraft.fandom.com/wiki/Blue_Ice) page |
| Top speed: blue ice | **72.7 b/s** (3.63 b/tick). Sportskeeda says 75; minecraft.how says "+50%", which is wrong | [R] same |
| Max turn rate | ice 50°/tick (1000°/s), blue ice ~91°/tick, water 10°/tick | [R] [minecraft.wiki Boat](https://minecraft.wiki/w/Boat) |
| Slipperiness | ice 0.98, blue ice 0.989, other blocks 0.6, **air 0.9**, water 0.9 | [R] same |
| Thrust / drag | forward thrust 16 b/s², reverse 2 b/s²; drag 0.4 s⁻¹ on ice, 0.22 s⁻¹ on blue ice, 8 s⁻¹ on land, 4 s⁻¹ on slime | [R] same |
| Hitbox | 1.375 wide × 0.5625 tall (Java). Two passengers | [R] same |
| Other land | ~2.5 b/s; soul sand and honey ~1 b/s | [R] same |

**Derived behaviour [D]:**
- Terminal speed = thrust / drag. On packed ice: 16 / 0.4 = 40 b/s, with a **time constant of 2.5 s**, so a boat reaches ~90% of top speed in ~6 s and ~150 blocks. On blue ice: 72.7 b/s, time constant 4.5 s, ~90% after ~10 s and ~450 blocks.
- **Blue ice only pays off on long straights.** On short sections it mainly makes the boat slide (0.989 slip) into walls. This matches guides saying blue ice "may lead to loss of control on curves" (search summary of [Sportskeeda](https://www.sportskeeda.com/minecraft/how-make-ice-race-track-minecraft)), and [Hermitcraft's Blue River Raceway](https://hermitcraft.fandom.com/wiki/Blue_River_Raceway), which used ice/packed ice with **stripes of blue ice, fastest on the longest straight** (search summary).
- **In the air, horizontal speed is multiplied by ~0.9 per tick [U]**, so a boat loses ~65% of its speed in a 10-tick (~2-block) fall. That explains player complaints that boats "lose all momentum in the air" ([feedback post](https://feedback.minecraft.net/hc/en-us/community/posts/37706511408909-Boats-losing-all-momentum-in-the-air), version unclear). The [IBR racing plugin/mod](https://modrinth.com/mod/ibr) "reduces air resistance significantly to encourage boat jumps". Conclusion: **big vanilla jumps kill speed. Use 1-block step-downs, not cliffs.** On step-downs, a vanilla video in [Minestom #1880](https://www.github.com/Minestom/Minestom/issues/1880) shows the boat keeping its speed over 1-block ledges.
- **Boats cannot climb.** Vanilla Java boats don't step up even 1 block (a snapshot briefly allowed it and was reverted, per search summary). Racing mods add step-up ([IBR](https://modrinth.com/mod/ibr), [Boat Step Up](https://github.com/Qendolin/boat-step-up)). So a vanilla point-to-point track must **go downhill or stay level**, or use a lift: a water/bubble elevator, ≤20 blocks tall to avoid ejection ([Munchy guide](https://munchymc.com/forums/t/guide-making-a-championship-hall-map/62848)), or an on-foot section.
- **Falls in 1.21.1 [U]:** 1.21.2 (24w37a) removed "the remaining code for boats that caused them to be destroyed like in pre-1.9", where falling from certain heights destroyed the boat and damaged the rider ([minecraft.wiki Boat history](https://minecraft.wiki/w/Boat)). On **1.21.1 that code is still present**. Keep every drop onto non-water **≤3 blocks**, or land in water, and test it.
- **Server-side velocity breaks boats [R].** Boat movement is client-predicted. When a server (Minestom) sent velocity packets to boats, the client's horizontal momentum was cancelled ([Minestom #1880](https://www.github.com/Minestom/Minestom/issues/1880)). For us: **don't `setVelocity` on ridden boats for boosts.** Use surfaces (blue ice) for speed, and eject → teleport → remount for resets.
- **Non-full blocks [R, U]:** [MC-203989](https://bugs.mojang.com/browse/MC-203989) (open since 1.16.4, lists 1.21) says a boat keeps ice speed when carpet or a slab sits on top of ice. Carpet over ice could carry colour markings without slowing boats. Test before relying on it.
- Other known quirks: boats getting stuck on water→ice transition bumps (feedback search summary); MC-298798 boats glitching into ice in 1.21.5 (not our version).

---

## Part 5: How ice boat tracks are built (researched)

### 5.1 Reference tracks, servers, rule sets

| # | Name / author | Where | What is structurally notable | Licence / terms (as written) |
|---|---|---|---|---|
| 1 | **Boat Rage** by pi314159265358978forge | [CurseForge](https://www.curseforge.com/minecraft/worlds/boat-rage), [PMC](https://www.planetminecraft.com/project/boat-rage/) | Underground lobby plus **18 ice track levels**, point-to-point, ~10 min for a skilled player (author PB 6:21). **Optional checkpoints and a "low speed penalty"** (punishes stalling or wall-hugging). | **"MIT License"** (the CurseForge page's licence field). Only candidate with a permissive licence; we still only take ideas. |
| 2 | **Ice Boat Raceway** by Scurrminator808 (Bedrock) | [MCPEDL](https://mcpedl.com/ice-boat-raceway/) | Circuit with **23 corners, 2 lakes (water sections = slow zones), 2 shortcuts**, pit lane, up to 10 players. Blue ice, plus an "Easy" version on packed ice. Lap record **1:16** (author comment). | None stated (only MCPEDL site terms) |
| 3 | **Mountain Drop Raceway** by Scurrminator808 | [MCPEDL](https://mcpedl.com/mountain-drop-raceway-map-1/) | **Point-to-point downhill** through 1.18 cliffs and caves; "first to reach the bottom". The same concept as our TGTTOS boat map. | None stated |
| 4 | **BDC tracks** on MinecraftOnline (community rule set) | [minecraftonline wiki](https://minecraftonline.com/wiki/Ice_Boat_Racing) | Certification rules: **track ≥ 24 s for an above-average racer**; **width ≥ 11 blocks for ≥ 70% of the length**; start/finish and grid made of drivable blocks; ≥4 grid slots; ≥80% lit to light 8; "-rally" (point-to-point) vs "-circuit". BDC Bay Circuit medals: **Diamond 40.25 s, Gold 41.5 s, Iron 46.0 s, Wood 52.0 s** (a 30% spread from top to casual). | Server wiki |
| 5 | **Munchy/MCC-style boat race rules** | [munchymc guide](https://munchymc.com/forums/t/guide-making-a-championship-hall-map/62848) | **Grid of ≥60 3×3 slots** fenced or walled, red wool spawns. Ice, packed or blue ice track. Other blocks used to slow players. **Walls on both sides. "Cannot have inescapable areas where boats get stuck".** Water elevators ≤20 blocks. **~2–4 min** race. Boat hockey: **barrier roof 5 blocks above** to stop launches. | Forum guide |
| 6 | **Competitive IBR scene**: BoatLabs, FrostHex, BRWC/IceDrifters, WolfNetwork | [Ice Boat modpack page](https://modrinth.com/project/YGyccRLk) | F1-calendar replica circuits, rally tracks, weekly time attacks. **TimingSystem** plugin: checkpoints, 1-tick (50 ms) timing, pit stops. Top medals need "blockstops and walltaps" (deliberate wall contact to turn). Uses OpenBoatUtils for non-ice racing surfaces, and BoatCam/Bobby because chunk loading and the camera struggle at 40–70 b/s. | n/a |
| 7 | **IceBoatRacing plugin** (Paper) | [Modrinth](https://modrinth.com/mod/ibr) | Oriented-rectangle checkpoints auto-traced from one lap, **default spacing 3 blocks**, (raise on straights, lower in corners), alternate bypass routes, sector gates, countdown lights. | Plugin |
| 8 | **Horizon Ventures server boat race** | [gitbook](https://horizon-ventures.gitbook.io/horizon-ventures-wiki/events-and-minigames/ice-boat-racing) | Boats frozen during a 3 s countdown, ordered checkpoints, **auto-return for off-track**, power-ups (5 s speed boost, slime trail), **20 s completion window for the rest after the winner**. | Server wiki |
| 9 | **Laguna Seca** and **Trial Mountain** recreations by GodzillaGamer85 | [CurseForge LS](https://www.curseforge.com/minecraft/worlds/laguna-seca-raceway-ice-boat-race-track), [Trial Mountain](https://www.curseforge.com/minecraft/worlds/trial-mountain) | 1:1-scale real circuits (3.6 km / 3.98 km) with real elevation and a tunnel. Shows full-scale tracks are km-long and far too long for us. | "All Rights Reserved" (both) |
| 10 | Hermitcraft **Blue River Raceway** | [Hermitcraft wiki](https://hermitcraft.fandom.com/wiki/Blue_River_Raceway) (search summary) | Ice/packed ice with blue-ice stripes; top speed on the longest straight; redstone lap timer. | n/a |

### 5.2 What the sources agree on
- **Width:** ≥11 blocks for most of the track (BDC). Fenced 3×3 grid slots for a big start (Munchy). With 40 boats (1.375 wide each), an 11-wide track fits ~5–6 abreast [D].
- **Length/time:** circuits ≥24 s per lap (BDC), events 2–4 min (Munchy), community laps 40–76 s. For a 100 s round the expert target of 35–45 s sits inside the normal range.
- **Surfaces:** packed ice is the default (40 b/s), blue ice is for straights, and non-ice blocks and water (8 b/s) are used as slow zones or hazards (Ice Boat Raceway lakes, Munchy "other blocks to slow").
- **Walls:** both sides walled; no pockets where boats get stuck (Munchy); barrier roof over jump or launch areas.
- **Recovery:** checkpoints plus auto-return or a reset (Horizon, Ely-style), and a low-speed penalty (Boat Rage).
- **Fun and overtaking:** several lines through corners, shortcuts with risk (Ice Boat Raceway), split routes (IBR plugin "alternate bypass routes"), power-ups (Horizon, Ice Boat Madness). In a dense pack, wall-tapping and corner braking separate skill levels.
- **Not found:** numeric radii for hairpins, ramp angles, or data on boat-to-boat collisions. Behaviour for those below is our understanding [U].

### 5.3 Boat collisions and punching [U, needs testing on Paper 1.21.1]
- Boats push each other on contact. A pack of 40 in one corner will pile up, which is fun in moderation, but a 9-wide hairpin with 40 boats is a jam.
- Riders can attack other players and boats from inside a boat. Punching a boat damages it and can break it. **Cancel `VehicleDamageEvent` / `VehicleDestroyEvent`** so boats can't be broken. Rider-to-rider punches likely do little: a mounted player's knockback doesn't move the boat much.
- **Realistic punching therefore happens on foot:** at the start (if boats must be boarded) and at any on-foot section.

---

## Part 6: Ice boat race, our recommendation

### 6.1 Concept
A **point-to-point downhill "rally"** in the style of Mountain Drop Raceway / Boat Rage, packed ice by default, with **one on-foot portage** in the middle. Reasons:
1. It solves the "boats can't climb" problem. The portage regains elevation for the second half.
2. It gives TGTTOS a real punching moment mid-race.
3. It reshuffles the pack.

The start is a **"Le Mans" dash**: players start on foot and run to boats laid out in 3×3 slots.

### 6.2 Scale [D]
- Packed ice tops out at 40 b/s. On a technical track an expert averages ~28–32 b/s, a bit more with blue straights.
- About **1,150 blocks of boat track plus a ~30-block portage** → expert ≈ 4 s (dash) + ~34 s (boat) + ~7 s (portage and re-boarding) ≈ **~42–45 s**.
- Median: wall bounces, pile-ups, a water lake detour, slow re-boarding, at ~18–20 b/s → **~75–85 s**.
- Weak: falling into water channels and getting stuck → DNF at 100 s.
- Compare BDC Bay Circuit: Wood 52 s vs Diamond 40.25 s. Our wider median-to-expert spread comes from 40-player traffic.

### 6.3 Segment plan (12 segments)

| # | Segment | Length | Width | Surface | Elevation | Notes |
|---|---|---|---|---|---|---|
| 1 | **Le Mans dash** | 20 (on foot) | 30 | stone/grass | 0 | 40 players spawn behind a line and sprint to **48 boats** (spares) placed in fenced 3×3 slots (Munchy grid). **Punching moment #1:** shoving and boat-stealing. Boats can't be broken (cancel damage). |
| 2 | **Grid straight** | 120 | 15 | packed ice | 0 | Pack sorts out. Full acceleration takes ~150 blocks, so the leaders are at ~38 b/s at the end. |
| 3 | **Funnel + first hairpin** | 60 | 15 → 11 | packed ice | 0 | 180° walled hairpin, inside radius ~6, outside ~17. **First pile-up.** The outside line is safe and slower; inside wall-tapping is fast. |
| 4 | **Chicane / S-bends** | 150 | 11 | packed ice | 0 | 3 linked 60–90° bends. Room for 2 lines. |
| 5 | **Step-down cascade** | 90 | 13 | packed ice | −6 (6 × 1-block steps, ≥12 blocks apart) | Keeps speed (1-block drops), looks dramatic. Never more than 1 block per step. |
| 6 | **Split: canal cut vs ice ring** | 140 | fast 5 / safe 9 | fast: blue ice with an **open edge into a water canal**; safe: packed ice, walled, ~20% longer | 0 | Risk shortcut (Ice Boat Raceway style). Falling into water = 8 b/s until a ramp back (water → ice transition via a 1-block-lower ice lip, which needs testing for stuck boats). |
| 7 | **Blue-ice straight with low bridges** | 200 | 11 | **blue ice** centre stripe 5 wide, packed ice edges | 0 | The only place where blue ice pays off (~450 blocks to reach 90% of 72.7 b/s, so the stripe gives +15–20 b/s over 200 blocks). 2 arch bridges with **≥3 blocks headroom** so they're scenery, not hazards. |
| 8 | **Braking hairpins** | 120 | 11 | packed ice | 0 | Two 180° turns right after the fast straight. Separates skill levels: overcooked boats slam the wall and lose ~all speed. |
| 9 | **Portage (on foot)** | ~30 on foot | 7 | stairs/parkour | **+12** | Track ends at a dock wall. Players exit and run up a 12-block stair or ramp to an upper deck with **48 fresh boats**. **Punching moment #2** (main brawl). Leftover boats despawn after use. |
| 10 | **Mixed-surface sweeper** | 140 | 13 | packed ice with soul sand and snow patches (~2.5 / ~1 b/s) on the inside apex | −4 (steps) | Long right-left sweeper. Patches punish apex-cutting. Clear lines exist around them. |
| 11 | **Final drop** | 60 | 11 | packed ice | −3 (one 3-block drop onto a long downhill run-out, then a flat **water landing pool**) [U: test boat-break and speed loss] | Single "jump". Barrier roof 5 above. Keep ≤3 blocks or land in water. |
| 12 | **Finish straight** | 80 | 15 | packed ice → water run-off | 0 | Finish plane across the full width. A water pool after the line stops boats. |
| | **Total** | **~1,160 boat + 50 on foot** | | | net −1 | 3 checkpoints (after segments 4, 8 and 10) |

### 6.4 Walls, barriers, anti-stuck
- **Walls:** 2 blocks high on all ice edges (boats can't climb, but players dismount and jump, and boats bounce on drops). 3 high plus a barrier roof at the jump. Use straight full-block walls. On diagonals, use **smooth curves of full blocks (2–3-block runs per step)**. Avoid fences, walls, panes and single-block posts in the drivable area, and avoid inside corners narrower than 3 blocks (stuck pockets, per the Munchy rule) [D].
- **Track edges:** flush, the same height as the ice. No 1-block lips: boats can't step up, so a lip stops them dead.
- **Stuck detection:** if boat speed < 1 b/s for 3 s outside the portage, or the player is out of the boat on ice, or in water outside the canal: eject → teleport to the last checkpoint → new boat → remount, about 2 s. Never push boats with server velocity (Minestom #1880).
- **Countdown:** freeze players, not boats. With the Le Mans start they're on foot anyway.

### 6.5 Safe line vs fast line
- Safe: outside lines in hairpins, the walled packed-ice ring at the segment 6 split, around the soul-sand patches, and staying off the blue stripe edges.
- Fast: inside wall-taps, the canal cut (falling in costs ~6–8 s), apex-cutting next to the patches, and the blue stripe.

### 6.6 Target times [D]
| Player | Est. time |
|---|---|
| Expert (fast line, clean portage) | **40–45 s** (shorten segment 4 or 10 by ~50 blocks if it comes out over 45 s) |
| Median (safe lines, 1 pile-up, slow portage) | **75–85 s** |
| Weak (canal fall, stuck reset, brawl losses) | **> 100 s (DNF)** |

### 6.7 1.21.1-specific test list
1. Boat break or damage after a 3-block fall onto ice (pre-1.21.2 code).
2. Speed kept over a 1-block step-down at 40 b/s.
3. Water → ice re-entry lip doesn't trap boats.
4. Carpet on ice keeps speed (MC-203989), for lane markings.
5. Cancelling VehicleDamageEvent stops boat theft and breaking.
6. Chunk loading at 40–70 b/s for 40 clients (keep the track inside ~10 chunks of view distance or pre-load). The IBR community needs Bobby for this.
