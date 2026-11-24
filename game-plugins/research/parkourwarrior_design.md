# Parkour Warrior: jump mechanics and obstacle design research

Scope: what vanilla Java **1.21.1** movement allows with no effects; how well-regarded public parkour maps and servers build and grade obstacles; how an offline validator should prove that every jump is makeable. This file complements `parkourwarrior_maps.md`, which covers the real event game's course structure, and responds to `ParkourWarrior/FEEDBACK_1.md`.

**Conventions**
- **Gap** = number of air blocks between the takeoff block's front edge and the landing block's near edge. So "4b" is a 4-air-block gap. This matches the community notation: mcpk.wiki says a "4b" jump "is actually 3.4 m", which is gap − 0.6 m of player width ([Parkour Nomenclature](https://www.mcpk.wiki/wiki/Parkour_Nomenclature)).
- **"+1 / −1"** = landing block top is 1 higher or lower than the takeoff top.
- **bm** = blocks of momentum (run-up behind the takeoff block). **bc** = ceiling height above the takeoff surface.

**Source and confidence tags**
- **[2src]** confirmed by two independent sources.
- **[1src]** one source only.
- **[sim]** computed by our own tick simulator (described in Part 3), which matches mcpk.wiki's published limits to within about 0.01 block.
- **[unverified]** from memory of game source or community lore. These must be checked in-game or by the validator before you rely on them.

---

## Part 1: What is physically possible (Java 1.21.1, no effects)

### 1.1 Core constants [2src]
Sources: mcpk.wiki [Horizontal](https://www.mcpk.wiki/wiki/Horizontal_Movement_Formulas) and [Vertical Movement Formulas](https://www.mcpk.wiki/wiki/Vertical_Movement_Formulas), and Minecraft Wiki [Attribute](https://minecraft.wiki/w/Attribute). Attributes added in 1.20.5 give jump_strength 0.42, gravity 0.08, movement_speed 0.1 and step_height 0.6.

| Quantity | Value |
|---|---|
| Jump vertical velocity | 0.42 b/t |
| Gravity / vertical drag | −0.08 per tick, then ×0.98 |
| Peak jump height | 1.2522 b (1.9+; 1.8 was 1.2492) |
| Flat airtime | 12 ticks |
| Airtime for +1 | 9 ticks ([Tiers](https://www.mcpk.wiki/wiki/Tiers)) |
| Sprint-jump boost | +0.2 b/t horizontally along facing |
| Ground acceleration | 0.1 × 1.3 (sprint) × input × (0.6/slip)³ |
| Air acceleration | 0.02 × 1.3 = 0.026 × input |
| Input magnitude | 0.98 for W only; 1.0 for W+A/D at the "45° strafe" |
| Horizontal drag | ×(slip × 0.91) when on ground at the start of the tick, ×0.91 in air |
| Slipperiness | default 0.6, slime 0.8, ice and packed ice 0.98, blue ice 0.989 ([Slipperiness](https://www.mcpk.wiki/wiki/Slipperiness)) |
| Momentum cut-off | \|v\| < 0.003 is zeroed (1.9+; 0.005 in 1.8) |
| Player box | 0.6 × 1.8 × 0.6; 1.5 high when sneaking (1.14+) |
| Step-up height | 0.6 |
| Collision order | Y is always moved first. Then X/Z, with the faster axis last (1.14+) ([Collisions](https://www.mcpk.wiki/wiki/Collisions)) |
| Speed reference | sprint 5.612 m/s; sprint-jumping averages 7.127 m/s ([Sprinting](https://minecraft.wiki/w/Sprinting)) |

### 1.2 Maximum straight gaps (the key table)

Each cell gives two numbers: the maximum gap from a ground run-up of any length ≥1 block with perfect timing [sim], and in brackets the maximum with ideal "flat momentum" (chained sprint-jumps), taken from mcpk.wiki [Longest Jumps](https://www.mcpk.wiki/wiki/Longest_Jumps) as max metres + 0.6. Numbers use the W+A/D strafe; W-only is about 0.03–0.05 shorter.

| Landing height | Max gap, ground run (chain mm) | What is buildable | Typical timing window from a run-up* |
|---|---|---|---|
| **+1** | 3.40 (3.95) | **3+1 max. 4+1 impossible**, even with momentum [2src: mcpk tier 3 = 3.35 m; sim] | 2+1: about 5 ticks (easy). 3+1: about 1.3 ticks (hard precision) |
| +0.5 (slab) | 3.69 (4.28) | 3+0.5 from a run. 4+0.5 needs chain momentum | 3+0.5: about 2.3 ticks |
| **0 (flat)** | 4.27 (4.92) | **4b max. 5b impossible** without ice or head-hitter momentum [2src: MC Wiki "up to four blocks"; mcpk 4.32 m] | 3b: about 4.4 ticks (easy). 3.5 (e.g. onto a slab edge): about 2.6. **4b: about 0.8 ticks (precise)** |
| −0.5 | 4.56 (5.24) | 4−0.5 | — |
| **−1** | 4.85 (5.55) | 4−1 from a run. **5−1 only with chain momentum** | 4−1: about 2.9 ticks |
| −2 | 5.14 (5.87) | 5−2 from a run (tight) | 4−2: about 3.9 ticks |
| −3 | 5.72 (6.49) | 5−3 from a run. 6−3 only with momentum | — |
| −4 | 6.01 (not tabulated) | 6−4 from a run (tight) | — |

\* The timing window is how many distinct jump ticks still land, averaged over 24 start alignments, W-only, 1-block-long landing [sim]. A window of 1 tick or less means a 50 ms timing target, which is "practise it" territory. Main-line jumps should have a window of 3 ticks or more.

**How run-up length matters [sim]**
- Run-up barely matters for distance. Ground sprint speed reaches about 95% of its terminal value (0.2806 b/t) within about 5 ticks, roughly 1 block. Results for 1 to 30 blocks of run-up differ by under 0.1.
- What changes with run-up is **timing alignment**. With 0–1 bm the player's feet arrive at the edge on a fixed tick phase, so a 4b may need a setup: shift-taps or force-momentum (FMM), as described in [Bahnarli's Minr guide](https://forums.minr.org/threads/bahnarlis-guide-to-parkour-on-minr.6236/).
- Rule: **4b with 0–1 bm is an advanced jump.** 4b with 3+ bm is intermediate.
- The extra distance in the momentum column comes only from chaining jumps (or head-hitter or ice), not from a longer flat run. This explains why mcpk numbers look about 0.65 b longer than what a single run-up jump gives.

**Effects of ceilings, ice and slime**
- **Head-hitter momentum** (spam-jumping under a 2bc ceiling, then jumping out at the edge) reaches about 5.0 b flat [sim], which is borderline. The Minecraft Wiki says a 2-high ceiling "increases jump reach to five blocks" ([Sprinting](https://minecraft.wiki/w/Sprinting)).
  - Treat 5b flat with hh momentum as **expert / possibly impossible**. Never use it.
- **Ice run-up** [sim; consistent with community "ice 5b" jumps, 1src]:
  - from ice or packed ice: 5.25 b flat, 4.17 at +1, 5.94 at −1
  - from blue ice: 5.39 b flat, 4.29 at +1, 6.09 at −1
  - So **4+1 and 5b become possible only from ice.** A short ice run of 3 blocks gives about 5.0 b flat, which is tight.
- **Slime as a run-up surface** gives nothing. Slime also slows horizontal motion when you walk on it (the Minecraft Wiki quotes about 70% slower walking).

### 1.3 Head-hitters (low ceilings) [sim, partly 1src]

| Ceiling over the takeoff **and** ≥1 block out over the gap | Max flat gap | Max at +1 | Notes |
|---|---|---|---|
| 2.0 bc (hh) | about 1.6 b | impossible (cannot rise) | Jump rises only 0.2. Airtime about 3 ticks ([Vertical formulas](https://www.mcpk.wiki/wiki/Vertical_Movement_Formulas): "2bc: 3") |
| 2.25 bc | about 2.2 | impossible | |
| 2.5 bc | about 2.5 | +0.5 only, about 1.9 | 2.5bc airtime 6 ticks |
| 2.75 bc | about 3.4 (position-dependent) | +1 at about 3.3 | |
| ≥3.0 bc | unaffected (head peak is 1.80 + 1.25 = 3.05) | | 3bc airtime 11 ticks, so almost no effect |
| 1.8125 bc (trapdoor hh) | forces a crouch (1.8–1.5 bc forces sneak, 1.14+) | — | 1.14+ change, [Version Differences](https://www.mcpk.wiki/wiki/Version_Differences) [1src] |

- A ceiling that covers **only the takeoff block** has almost no effect if the player jumps on the tick they leave the block. Y is resolved first, so the player can jump one tick after the feet leave the edge ([Collisions](https://www.mcpk.wiki/wiki/Collisions)). For casual players it still shortens the jump. **Do not count on a ceiling over the takeoff block alone to make a jump harder.**
- Design use:
  - A ceiling 2–2.5 bc over the gap turns a "trivial" 2b into a reading-and-timing jump.
  - Chains of 1–2b jumps under a 2bc ceiling (the "hh chain") are a classic intermediate element.
  - Put a 2bc ceiling over the approach and run-up when you want to stop players from jumping over an obstacle. This is also the standard anti-skip tool.

### 1.4 Neos (jumping around a wall)

**Definition:** the wall is directly between the takeoff and landing blocks, and the player must curve around its end in the air ([prinston neo guide](https://prinston.neocities.org/guides/neos.html); [Nomenclature](https://www.mcpk.wiki/wiki/Parkour_Nomenclature): "#b neo: a jump around a wall, where # is the wall's length").
- The wall must be at least 2 high, so the player cannot jump onto it or over it.
- The player needs displacement of at least wall length + 0.6, because the 0.6 box must be fully before the wall at takeoff and fully past it before swinging back over the landing block.
- mcpk gives the equivalence: "add 1.2 to its distance and raise its tier by one" ([Collisions](https://www.mcpk.wiki/wiki/Collisions)).

| Neo | Linear equivalent | Possible? | Tier (community) | Design rule |
|---|---|---|---|---|
| Single (1-wall) | 2.2 b at about +0.25 | Yes, comfortably from a run. Our sim finds plenty of slack | Intermediate | Use from main zone 3 onward. Give at least 2 bm and a ≥2-block landing |
| Double (2-wall) | 3.2 b at about +0.25 | Yes, but tight. Our crude neo sim reaches about 2.25 walls flat (2.85 with a −1 landing), so the margin is only about 0.25 b | Advanced. Minr guide calls a "double neo / ne-up" intermediate with a precise setup (3 shift-taps back, aim 2–5° out) | Gold/hard only. Need ≥2 bm, a landing at −1 or a ≥2-block landing strip, and validator proof |
| **Triple (3-wall)** | 4.2 b at about +0.25 | **Only with optimal momentum or collision tricks.** mcpk: "a 2bm triple neo … only possible X-facing" | Expert. ManaCube treats it as the ceiling: "The most challenging jump that will be accepted is about the level of a triple neo" ([ManaCube forum](https://manacube.com/goto/post?id=182247), 1src snippet). A Hive player needed about 30 min on one ([Hive forum](https://forum.playhive.com/t/new-hub-parkour-to-the-hive-sign/16922?page=3)) | **Never build.** This is the playtest's "impossible" Billboard Bend, Dripstone Descent and similar |
| Quad (4-wall) | 5.2 b at about +0.25 | **Impossible.** The maximum at that tier is 4.60 | — | Never |

**What makes a neo impossible when misbuilt**
- The wall is 3 or more blocks long.
- The landing is only one block long and level or higher.
- There is no run-up, or the run-up is blocked.
- The wall has a cap or overhang that blocks the curve (a ceiling over the swing path at 2–2.5 bc).
- A second wall or pane sits on the outside of the swing path. That turns it into a "butterfly neo", which is much harder.
- A Z/X-facing quirk: mcpk notes that some neos are possible only on one axis. **The validator must test the actual facing.**

**Wall and corner jumps (non-neo)**
- **Jumping around a corner** (takeoff and landing on adjacent faces of a pillar) is intermediate.
- **Wall-hugging** (sliding along a wall in the air) cancels momentum on that axis and stops sprint. mcpk: "If the player hits an X-facing or Z-facing wall, momentum on that axis is cancelled, and the player stops sprinting". This is the main hidden cause of "impossible" jumps beside walls. **Keep at least 0.3 b of clearance** between the jump line and any wall.

### 1.5 Thin and odd hitboxes (footholds)

Heights and widths are in blocks. They come from the mcpk [Blocks](https://www.mcpk.wiki/wiki/Blocks) table (1.8). Rows marked [unverified] are from 1.21 block shapes (pixels/16) and must be checked.

| Block | Top height | Foothold width | Notes for design |
|---|---|---|---|
| Fence (post) | 1.5 | 0.25 × 0.25 | Cannot be jumped onto from level ground (peak 1.25). Reach it from ≥0.5 higher, or use it as a "1.5 wall" obstacle. Fence-post landing is a precision jump |
| Wall (post / 4-sided) | 1.5 | 0.5 × 0.5 up to 1 × 1 | Same 1.5 height. A post is a wider foothold than a fence |
| Glass pane / iron bars (single post) | 1.0 | 0.125 × 0.125 [unverified for 1.21 post size] | **Jumpable onto from level** (1.0 < 1.25). ManaCube bans "single iron fences, single glass panes" as jump blocks (they are too precise). Use connected panes (a 1 × 0.125 rail) |
| Pane line (2-opposite) | 1.0 | 1 × 0.125 | "Pane walk" or pane landings: the landing is 0.125 + 0.6 box = 0.725 wide sideways tolerance |
| Trapdoor (bottom/top, closed) | 0.1875 / 1.0 | 1 × 1 | An open trapdoor is a 0.1875 vertical plate. It is used as ceiling (tdhh 1.8125 bc) or as a thin wall |
| Ladder | 1.0 (climbable) | 0.1875 deep (1.9+; 0.125 in 1.8) | Climbable. See 1.6 |
| Slab | 0.5 | 1 × 1 | +0.5 landings change the tier (see 1.2) |
| Stairs | 0.5 / 1.0 | half-block step | Players step 0.5 automatically. Stairs are *not* a +1 obstacle |
| Chest / ender chest | 0.875 | 0.875 × 0.875 | "+0.875" behaves like +1 (tier 3) but is slightly easier |
| Enchanting table | 0.75 | 1 × 1 | +0.75 is tier 2 |
| Anvil | 1.0 | 1 × 0.75 (1.8); 1 × 0.625 [unverified 1.21] | Narrow top on one axis |
| Flower pot | 0.375 | 0.375 × 0.375 | Below step height 0.6, so it can be walked onto. A tiny precision landing |
| Mob head | 0.5 | 0.5 × 0.5 | Precision landing |
| Lantern (standing) | about 0.5625 [unverified] | about 0.25–0.375 [unverified] | Precision landing |
| Chain (vertical) | 1.0 | 0.1875 [unverified] | Like a pane post |
| End rod (vertical) | 1.0 | 0.25 [unverified] | Like a fence post at height 1.0 |
| Carpet | 0.0625 | 1 × 1 | Removes slipperiness? No: slabs and carpets inherit the block below (1.15+ checks 0.5 below) ([Slipperiness](https://www.mcpk.wiki/wiki/Slipperiness)) |
| Snow layers | (n−1) × 0.125 in 1.13+ (a single layer has no collision) | 1 × 1 | [1src: Version Differences] |
| Bed | 0.5625 | 1 × 1 | Bounces about 66% (1.12+) |
| Bamboo | full height | about 0.1875, **randomly offset per position** [1src: Version Diff "narrow box"] | Avoid. The offset makes the validator geometry position-dependent |
| Cauldron / hopper | 1.0 rim | Interior floor lower | Rim walking |

Landing tolerance rule: the player lands if the 0.6 box overlaps the top face. So the effective landing width is **block width + 0.6**: a fence post gives 0.85, a pane rail gives 0.725 sideways.

### 1.6 Ladders, vines, scaffolding [1src unless noted]
Source: mcpk [Ladders and Vines](https://www.mcpk.wiki/wiki/Ladders_and_Vines).
- **Climb speed:** 0.1176 b/t up when pushing into the ladder or, since 1.14, holding jump.
- **Speed caps:** horizontal speed is capped at 0.15 b/t on a ladder, and fall speed is capped at −0.15 b/t.
- **Grabbing:** touching the 0.1875-deep box (1.9+) grabs the ladder, so a "ladder catch" is forgiving horizontally.
- **Sneaking on a ladder** holds the player in place.
- **Vines** behave like ladders but have **no collision**, so you can pass through them. Since 1.14 they need no supporting block.
- **Ladder jumps:** leaving a ladder sideways or backwards starts at no more than 0.15 b/t, with no sprint-jump boost (you are not on the ground).
  - Ladder-to-ladder gaps of 1–2 b, or ladder to a block 1–2 b away, are typical in community maps.
  - We have no sourced maximum. **Validate each one with the sim** [unverified].
- **Scaffolding:** solid top when you approach from above and are not sneaking. You climb by jumping and descend by sneaking ([MC Wiki](https://minecraft.wiki/w/Scaffolding)). It slows movement inside it; we have no number [unverified].

### 1.7 Special blocks and moving elements

**Ice / packed ice / blue ice** [2src for slip]
- They give low ground acceleration but keep speed. Use a ≥3-block straight ice run-up to make 5b and 4+1 possible (see 1.2).
- Ice landings make players slide off narrow platforms. That is good "control" difficulty: land on ice, then a short jump.
- Ice melts near light in warm biomes. Microsoft's sample says "Ice blocks may melt in some biomes", so use packed ice or blue ice.

**Soul sand**
- 0.875 high. It multiplies horizontal speed by 0.4 on its inner surface; since 1.15 there is a 0.3 m normal-ground perimeter ([Soulsand](https://www.mcpk.wiki/wiki/Soulsand)) [1src].
- Jumps *from* soul sand are short: you take off at about 40% speed.
- Our sim estimate: about 2–2.5 b flat [unverified]. Validate.

**Slime block**
- It reflects downward velocity (no fall damage). Successive bounce apexes [sim]:

| Drop height | 1st bounce | 2nd | 3rd |
|---|---|---|---|
| 1.25 (a normal jump onto slime) | 0.96 | 0.57 | 0.36 |
| 3 | 2.10 | 1.45 | 1.15 |
| 5 | 3.53 | 2.25 | 1.73 |
| 10 | 6.33 | 4.25 | 2.78 |
| 20 | 10.7 | 6.9 | 4.5 |

- These results are roughly consistent with the Minecraft Wiki's "22 blocks for a 50-block fall" and its maximum of 57.625 ([Slime Block](https://minecraft.wiki/w/Slime_Block)).
- Bounce apexes vary ±0.2 with tick phase.
- **1.21.1 specific:** pressing jump on landing *overrides* the bounce with a normal jump. The Minecraft Wiki says that from 24w33a (1.21.2) the bounce "can no longer be overwritten by jumping". So on our version a held jump key kills big bounces. Tell players, or design bounces that work either way.
- Sneaking cancels the bounce.

**Honey block**
- Jump height about 0.38 b (1.15+; [Version Differences](https://www.mcpk.wiki/wiki/Version_Differences) "only jump about 0.383m"). The Minecraft Wiki quotes "about 3/16 of a block", which disagrees, so **flag it**. Either way, you cannot jump up a +0.5 step from honey.
- Walking speed about 60% slower.
- **Wall slide:** pressing against a honey wall's side while falling limits the fall to about 0.05 b/t [unverified, from source memory]. The Minecraft Wiki says holding a wall allows jumps "about 2 blocks farther". This is the classic "honey wall slide across a gap".

**Cobweb**
- Horizontal speed ÷4 and vertical ÷20, reset every tick. Jump height inside is 0.021 ([Cobweb](https://www.mcpk.wiki/wiki/Cobweb)) [1src].
- Use it as a "brake" or trap tile, never on a required line.

**Powder snow**
- Slows like cobweb. The stuck multiplier is 0.9 horizontal and 1.5 vertical ([Version Differences](https://www.mcpk.wiki/wiki/Version_Differences)) [1src].
- Without leather boots you sink through it.
- It causes freezing damage after 140 ticks. We take no damage, but the freeze vignette is a nuisance.
- Avoid it except as a short "kill zone you sink into".

**Water and bubble columns** [unverified numbers]
- Water: since 1.13 sprinting in water swims.
- A soul-sand bubble column pushes up. A magma column pulls down.
- They are useful as elevators between zones, not as jumps.
- Hypixel's lobby speedrun splits 1.8–1.12 from 1.13+ because of water strats, so **water changes routes**. Keep it out of race paths unless intended.

**Pistons and moving blocks**
- A piston pushes an entity standing on a moved block.
- A slime block on a piston launches players about 6.4 b up, or about 3.5 b sideways ([Slime Block](https://minecraft.wiki/w/Slime_Block)) [1src].
- Moving-block collision is server-side and lag-sensitive. ManaCube bans x-facing and down-facing piston heads as jump blocks.
- For timed obstacles **prefer plugin-placed blocks that toggle on a fixed tick cycle** (appear/disappear platforms) over real pistons. Then the validator can model "block present between ticks a and b".

**Boats as platforms**
- Standable since 1.17 (no teleport), but they are entities: they drift and can be broken or ridden.
- Use them as decoration only, or as plugin-locked display entities [unverified].

### 1.8 1.21 versus old 1.8 guides (what changed)

| Topic | 1.8 guides | 1.21.1 | Source |
|---|---|---|---|
| Jump height | 1.2492 | 1.2522; some jumps got easier tiers | mcpk Tiers, Version Diff [2src] |
| Momentum cut-off | 0.005 | 0.003 | mcpk [2src] |
| Collision order | Y, X, Z fixed (X/Z-facing differences) | Velocity-dependent (Y first, then the faster axis last); "most collisions now resemble X-facing" | mcpk Collisions [1src] |
| Sneak | 1.8 tall, cannot sprint | 1.5 tall, can sprint, 0.6 ledge stop (1.11); ceilings below 1.8 force a crouch | mcpk Version Diff [1src] |
| Ladder depth | 0.125 | 0.1875 | mcpk Ladders [1src] |
| Slipperiness lookup | 1 block below | 0.5 below (1.15) | mcpk Slipperiness [1src] |
| Soul sand | slip-affected, 0.4 everywhere | inner-surface slowdown only | mcpk Soulsand [1src] |
| Water | old | swimming (1.13), bubble columns | mcpk [1src] |
| Slime jump override | — | still overrides in 1.21.1; fixed in 1.21.2 | MC Wiki [1src] |
| Attributes | hard-coded | `jump_strength`, `gravity`, `step_height`, `scale` exist (1.20.5+). **Make sure no plugin changes them** | MC Wiki Attribute [1src] |

Where the numbers come from:
- mcpk "Longest Jumps" and "Tiers" give 1.8 and 1.9+ columns. The 1.9+ physics is unchanged for straight jumps through 1.21.1, apart from the collision order.
- Our sim uses 1.21 rules.
- ManaCube and Minr serve 1.8 to 1.21 clients. Their difficulty lore is mostly 1.8-era.

---

## Part 2: What good courses do

### 2.1 Maps and servers studied
No map we found grants reuse rights. Every one is either "All Rights Reserved" or carries no stated licence. **Treat them all as inspiration only: reuse mechanics, never builds.**

| # | Name / author | URL | Obstacles | Difficulty ramp | Memorable | Licence (as written) |
|---|---|---|---|---|---|---|
| 1 | Parkour Spiral 1–3, Hielke | [hielkemaps.com/maps](https://hielkemaps.com/maps), [9minecraft](https://www.9minecraft.net/?p=164287) | One giant themed tower; "lots of different parkour themes"; PS2 adds dropper and elytra sections | Climb bottom to top; "quick and moderately tricky" | One continuous vertical race, many checkpoints, racing friends | none stated on the map page; site footer "Copyright © Hielke Maps 2026." |
| 2 | Parkour Paradise 1–3, Hielke | [hielkemaps.com/maps](https://hielkemaps.com/maps) | 100 small 10×10 (PP1) or 15×15 (PP2) rooms; PP3 adds rising-lava and "snake" levels | Rising over 100 levels, beginner-friendly start | Compact one-idea rooms, the same model as our "one idea per obstacle" | none stated (copyright footer only) |
| 3 | Whirlpool Parkour, Zombie1111 | [mc-mod.net](https://www.mc-mod.net/?p=318959) | Moving blocks, player launchers, climbing out of a whirlpool | "medium-difficult", built for speedrun | Fail and time tracking, grades, practice mode | none stated |
| 4 | No Time To Stop 2, TheblueMan003 | [mc-mod.net](https://www.mc-mod.net/?p=315272) | 30 levels where stopping kills you; some elytra | Speedrun levels | A forced-flow mechanic | none stated |
| 5 | Parkour Helix 2, Jerrah | [minecraftmaps](https://www.minecraftmaps.com/47866-parkour-helix-2) (snippet) | 60 stages down or up a helix, hidden shortcuts | Stage by stage | Shortcuts as a reward (the opposite of our rule) | unverified |
| 6 | Ascend, "Lol" (1.21.3) | [minecraftmaps](https://www.minecraftmaps.com/47924-ascend) (snippet) | "hard ones like triple Neos and piston jumps" | Rising | Shows that triple neos are an expert-map element | unverified |
| 7 | The Ultimate Parkour Challenge (Sphere Castle and Cave), staycoolis | [CurseForge](https://www.curseforge.com/minecraft/worlds/the-sphere-castle-and-cave) | Block jumps, "soul-crushing neo jumps", ladder leaps, slime launches, ice slides, soul sand | "Progressive (Easy to Insane)", 10+ themed stages, bonus stage for elite players | Themed stages plus an elite bonus, close to our bronze/silver/gold idea | "All Rights Reserved" |
| 8 | Learn Parkour: Training Map, ClipClipper (1.21.x) | [CurseForge](https://www.curseforge.com/minecraft/worlds/learn-parkour-minecraft-parkour-training-map) | Tutorials for neos, classic jumps, "advanced movement tricks" | 4 courses, beginner to expert | Teaches each element before testing it | "All Rights Reserved" |
| 9 | Microsoft Sample Parkour World | [Microsoft Learn](https://learn.microsoft.com/en-us/minecraft/creator/documents/parkourworldwalkthrough) | 1–4 gaps ("4 blocks, or a quad, requires players to have a running start"), 2+1, ladder-to-ladder round a corner, head-hitters, soul sand, packed or blue ice, slime, honey wall slide, dripleaf, piston footholds | Sections in order: player mechanics, then block mechanics, then redstone | Uses "walls and a ceiling to constrain the player"; spawn-set buttons | none stated on the page (repo licence not checked) |
| 10 | Minr server | [wiki.minr.org/Map](https://wiki.minr.org/Map) | Community maps | 7 colours: White 1, Blue 2, Green 4, Yellow 7, Orange 12, Red 18, Black 25 points; "Ratings are decided by the Green+ community." | Points ladder | n/a (server) |
| 11 | ManaCube parkour | [submission rules](https://www.manacube.com/threads/must-read-map-submission-information-format.649/post-88233) | Medium: "normal 3 and 4-Blocker, no ladder and fence jumps". Hard: "headhitter jumps", "a neo is needed". Expert: "many basic 4 blocker… only 3 neos" (search snippet) | Easy 20–40 jumps, Medium 40–60, Hard 60–80, Expert 80–100, Insane 100+ | Bans single panes and iron fences; triple-neo cap | n/a |
| 12 | Hypixel main-lobby parkour | [speedrun.com](https://www.speedrun.com/ru-RU/mcm_hsp) (snippet) | 4 checkpoints | — | Checkpoint skipping created "All Checkpoints" and "Any%" categories, so **checkpoints must be ordered and enforced** | n/a |

Not covered (sites blocked fetching or no source found): Planet Minecraft pages, Hypixel Housing, the Hive and the prinston MCPK course archive.

### 2.2 Obstacle catalogue (48 ideas)

Recipes use gap in air blocks and height relative to takeoff.

**Tiers**
- **B** = beginner (main zones 1–2)
- **I** = intermediate (main zones 3–5, bronze)
- **A** = advanced (silver, hard ending)
- **E** = expert (gold only, validator window 1 tick)

Every recipe must still pass the validator; the numbers are inside the Part 1 limits.

| # | Name | Mechanic / blocks | Build recipe | Tier |
|---|---|---|---|---|
| 1 | Sprint threes | flat 3b chain | 4 jumps of 3b on 1×1 pillars, 1-block landings, staggered 1 sideways | B |
| 2 | Up-twos | 2+1 staircase | 2b gaps, each +1, 1×1 tops, 5 steps | B |
| 3 | Drop fours | 4−1 | 4b gap down 1 onto a 1×2 landing, 3 bm | B/I |
| 4 | The quad | 4b flat | 4b flat, ≥3 bm, 2-long landing | I (4b with 0–1 bm is A) |
| 5 | Three-plus-one | 3+1 | 3b gap to +1, ≥2 bm, 1×1 landing | A (1.3-tick window) |
| 6 | Slab hop | +0.5 | 3b onto a bottom slab +0.5, then 3b off it | I |
| 7 | Diagonal pairs | diagonal | 2×1 and 3×1 offsets (e.g. 3b forward, 1 sideways). Check the Euclidean edge distance √(dx²+dz²) against the table | I |
| 8 | Corner cut | jump past a pillar corner | 2b around a 1×1 pillar to a block offset 1 sideways | I |
| 9 | Single neo | 1-wall | 2-high wall of 1 block; 2 bm; landing 1×2 behind | I |
| 10 | Neo drop | 1-wall, −1 | as #9 but landing −1 | I |
| 11 | Double neo | 2-wall | 2 bm, landing −1 or 2 long, validated | A/E |
| 12 | Butterfly neo | neo with panes on the outer side | 1-wall plus pane line 1.5 out | E |
| 13 | Head-hitter chain | 2bc ceiling | 4× 1b gaps under 2bc continuous ceiling, flat | I |
| 14 | Low-roof two | 2.5bc over the gap | 2b flat under a 2.5bc ceiling covering takeoff and gap | I |
| 15 | Trapdoor lid | tdhh 1.8125 | Ceiling of top-trapdoors forces a crouch; walk in, then jump out of the end | I |
| 16 | Fence step-up | 1.5 fence/wall | Block +0.5, then a fence post +1.5 relative to the ground (that is +1 from the slab), 1b apart | I |
| 17 | Fence line | posts | 4 fence posts 2b apart, flat (0.85 landing tolerance) | A |
| 18 | Wall-post hops | cobble wall posts | 2b gaps between single wall posts (0.5 wide) | I |
| 19 | Pane rail | connected panes | 1×0.125 pane runs; jump 2b along the rail onto the next rail | A |
| 20 | Pane neo | glass pane wall | Neo around a pane end: the wall is only 0.125 thick, so it is easier than a block neo | I |
| 21 | Iron-bar ladder | bars plus ladders | Climb a ladder, side-jump 1b to bars (land on top, 1.0 high) | I |
| 22 | Ladder catch | ladder | Run-jump 2b across a gap onto a ladder face; climb 3 | B/I |
| 23 | Ladder-to-ladder | ladders on pillar faces | Ladders on two faces of a corner; leave one and catch the next 1b around the corner | I |
| 24 | Ladder top-out jump | ladder to block | Climb a ladder whose top is level with a block 2b behind you; turn and jump | A |
| 25 | Vine wall | vines | Vine climb, then a sideways leap 1–2b to the next vine strip | I |
| 26 | Scaffold tower | scaffolding | Climb inside a scaffold column; exit at the top onto a 2b jump | B |
| 27 | Chest hops | chest 0.875 | 2b gaps onto chests (feels like +1, slightly easier) | B/I |
| 28 | Table tops | enchanting table 0.75 | Alternating +0.75 and −0.75 3b jumps | I |
| 29 | Anvil beam | anvils | Row of anvils oriented so the 0.625 top is across the jump; 2b gaps | A |
| 30 | Pot precision | flower pots | 2b flat onto a flower pot (0.375 square, 0.975 tolerance) | A |
| 31 | Lantern line | standing lanterns | 2b onto lantern tops (verify the hitbox first) | A [unverified hitbox] |
| 32 | End-rod spikes | end rods (vertical) | 2b between rods, height 1.0, 0.85 tolerance | A |
| 33 | Chain posts | vertical chains | Similar to pane posts | E |
| 34 | Ice runway | packed ice | 5-block packed-ice run-up, then 5b flat to a 2-long landing | A |
| 35 | Ice landing | blue ice | 3b onto a 1×3 blue-ice strip, then a 2b off the end without sliding off the side | I |
| 36 | Soul-sand slog | soul sand | 2b from soul sand (short takeoff), validated | I |
| 37 | Slime trampoline | slime | Drop 3–5 onto slime and bounce to a ledge +1 to +2 above the drop point. **No jump key on landing (1.21.1)** | I |
| 38 | Slime ricochet | slime chain | Fall 5 onto slime, bounce onto a 2nd slime 2 lower, and so on | A |
| 39 | Honey wall slide | honey | 4 tall honey wall beside a 5b gap that drops 2; slide down while drifting across | A |
| 40 | Honey brake | honey floor | Honey run-up before a 2b, which kills the momentum and timing | I |
| 41 | Cobweb gate | cobweb | Cobweb hanging in the jump arc forces a lower route or a head-hitter | I |
| 42 | Timed platforms | plugin toggle | 3 platforms that appear for 30 ticks out of every 60 in sequence; 2b gaps | I |
| 43 | Sweepers | plugin-moved block line | A 1-high barrier line sweeping across a 2b chain; jump over it (1.0 obstacle) | I |
| 44 | Crumbling path | plugin decay | Blocks vanish 15 ticks after stepping on them; a 6-block winding path of 1–2b jumps | I |
| 45 | Piston stair | plugin-extended blocks | Footholds extend every 40 ticks; 2+1 chain | I |
| 46 | Dripleaf hop | big dripleaf | Leaf tilts after a short time on it (Microsoft sample: "Jump before it collapses"); 2b chain | I [timing unverified] |
| 47 | Route-read maze | visual | 3 paths; 2 dead-end into a 2bc ceiling or a 5b impossible gap (visibly too far); 1 real. Reveal only at the hub | I |
| 48 | Momentum-chain finale | chained sprint jumps | 3b, 3b, 3b, then 4b+0.5 using chain momentum (validated) | E |

### 2.3 Stopping bypasses (fixes for Tick-Tock Teeth and Rattling Lift)
- **No walkable side ledges.** Every jump surface must be exactly the intended width, with void or a kill plane beside it. Decoration must sit at least 1 block lower than the path, or be non-collidable (barrier-free decorative layers), so it cannot be used as a footpath.
- **Side walls and ceilings.** Microsoft's sample "uses walls and a ceiling to constrain the player to a particular path". A 2–2.5 bc ceiling stops jump-overs. Keep walls 0.3 b or more from the jump line (see 1.4).
- **Barriers, used sparingly.** ManaCube: "avoid annoying barrier block placement"; barriers must not interfere with legitimate jumps or sit right beside the player without a warning. **Prefer visible blocks** (glass, chains, iron bars) to block skips.
- **Kill planes and teleport volumes.** Put a teleport volume a fixed depth below each obstacle, and around its sides if needed, so a player who leaves the line is sent to the checkpoint. Our "fall to checkpoint" system is already this. Extend the volume horizontally so players cannot walk along the decoration.
- **Ordered checkpoints.** The Hypixel speedrun split shows that skipping checkpoints happens. Each obstacle's finish checkpoint should accept only players who passed its start (sequence numbers).
- **Geometry.** Make the hard route the *only* geometric route. If an easier variant exists, it must be a separate path that branches at a hub, with different points (the owner's rule).
- **Validator check.** For each obstacle, run reachability from start to end over all surfaces except the intended ones. Any path that avoids the intended jump means "bypass found".

### 2.4 Difficulty ladder for our course (competent Minecraft players)

**Calibration (owner, corrected):** Runaway Rails, at about 10 attempts for a good player, is the **easy end (floor) of the hard/gold tier**. A typical hard or gold obstacle must be clearly harder than that and still be validator-proven. Sim timing windows give an objective scale:

| Window (consecutive jump ticks that work) | Feel |
|---|---|
| ≥4 ticks | almost never fails |
| 3 | fails about 1 time in 5 |
| 2 | fails about 1 time in 2 |
| 1 | needs practice |
| <1 with alignment dependence | needs a setup; expert |

These "feel" estimates are our own judgement [unverified].

| Section | Should feel like | Allowed elements (minimum to maximum) | Validator margin |
|---|---|---|---|
| Main, zone 1 | Real sprint-jumping from the start: no 1b gaps, no walking | 3b flat, 2+1, 3−1, chest hops, ladder catch, scaffold. At least 2 different elements per obstacle | Window ≥4 ticks with W only; landing overlap ≥0.25 b |
| Main, zone 2 | Precision starts | Plus 3+0.5, 4−1, head-hitter chain, wall-post hops, ice landing, slime trampoline | Window ≥3 |
| Main, zone 3 | First "real parkour" | Plus 4b with ≥3 bm, single neo, fence step-up, pane neo, timed platforms, diagonal 3×1 | Window ≥3 |
| Main, zone 4 | Medium-hard; occasional fails expected | Plus 3+1 (one per obstacle at most), 2.5bc low-roof 2b, ladder-to-ladder corners, soul sand, honey brake, sweepers | Window ≥2 |
| Main, zone 5 | As hard as Runaway Rails at most (main line must stay finishable) | Plus combos: neo drop, pane rail, crumbling path, ice runway 5b with a long landing | Window ≥2; no single-tick jumps |
| Bronze (per zone) | One step harder than its zone's main line | Next zone's elements, longer chains (5–6 jumps without a checkpoint) | Window ≥2 |
| Silver | Advanced: about 10–20 attempts for a good player (the Runaway Rails level) | 4b with 1 bm, 3+1 chains, fence lines, anvil beams, flower pots, slime ricochet, honey slide | Window ≥1.5 (averaged over alignments) |
| **Gold** | Clearly above Runaway Rails: about 20–50 attempts for a good player | Double neo (validated), butterfly-free neo chains, 4+0.5 with chain momentum, pane posts, end-rod spikes, 3+1 after a neo, timed chains | Window ≥1 tick at the best alignment; landing overlap ≥0.03 b; **never use a jump our sim only makes with 45° strafe AND the late-tick trick AND chain momentum combined** |
| Easy ending (~40 blocks) | Fast victory lap | 6–8 jumps of 3b and 2+1, one ladder | Window ≥4 |
| Medium ending | Like main zone 4 | 4b with bm, single neo, 3+0.5, head-hitter chain, 2-step slime bounce (not the "way too easy" ice slide) | Window ≥2 |
| Hard ending | Gold-level and climactic, vertical | Ladder top-out jump, fence line, double neo or 3+1 into pane rail, ice 5b, finishing with a slime bounce to the finish | Window ≥1, validated, and no instant-kill blocks: fix the Spine Scramble red-block bug and check kill-volume overlap |

What to stop doing (from the playtest): 1b gaps, plain chains of identical 3b jumps, a "medium" that is a single idea done once, ice slides with no jump, and any neo with a wall 3 or more long.

---

## Part 3: Proving a jump is makeable offline

### 3.1 The movement model (per tick, Java 1.21.1)

Sources: mcpk formulas (above), Minecraft Wiki attributes, and decompiled `LivingEntity.travel`/`aiStep` and `Player` code [unverified line-level, but our implementation reproduces mcpk's published tier maxima].

```
inputs per tick: forward/strafe keys (W, W+A, W+D), yaw, jump, sprint=true, sneak
input vector = (strafe, forward) * 0.98; if |v|>1 normalise (W+A/D -> magnitude 1.0)
1. if jump && onGround: vy = 0.42 * blockJumpFactor (honey 0.5)
                        if sprinting: vxz += 0.2 * (-sin yaw, cos yaw)
2. slip = friction of the block 0.5 below the feet (0.6 default; slime 0.8; ice 0.98; blue 0.989)
   accel = onGround ? 0.13 * 0.21600002 / slip^3 : 0.026   (0.1 and 0.02 when not sprinting)
   vxz += rotate(input) * accel
3. move: resolve Y first against all boxes, then X and Z (larger |v| axis last);
   onGround := (Y collided while moving down); horizontal collision -> zero that axis, sprint stops
   block speedFactor (soul sand / honey 0.4) multiplies vxz; stuck blocks (cobweb 0.25/0.05, powder 0.9/1.5) scale motion
   slime: on landing with vy<0 and not sneaking, vy = -vy
   climbable (ladder/vine/scaffold): clamp vxz to ±0.15, vy >= -0.15 (or 0 when sneaking); if pushing into a wall or jump held: vy = 0.2
4. vxz *= (onGround at start of tick ? slip*0.91 : 0.91); vy = (vy - 0.08) * 0.98
5. |component| < 0.003 -> 0
box 0.6 x 1.8 (1.5 when sneaking); step-up 0.6 on the ground
```

Validation of our reference implementation (`pksim.py`, kept in the research scratchpad; port it to Java):
- Chained sprint-jump momentum reproduces mcpk's flat maximum of 4.3227 m and the +1 maximum of 3.3518 m to within about 0.01 m.
- The flat 4b result matches the Minecraft Wiki's "up to four blocks".

### 3.2 How the validator should decide

**Search, not a single replay.** For each jump, enumerate:
- start alignment (16 sub-block offsets of the approach)
- jump tick (every tick from 3 ticks before the edge to 1 tick after leaving it)
- input set (W only; and W+A/D for silver and gold)
- for neos and diagonals, yaw sequences: initial yaw ±0–25° in 1° steps, plus one or two in-air yaw switches at any tick. This is a coarse grid search, refined by local search.

A jump is "makeable" if at least one input sequence lands with the player's box overlapping the target top face and no collision with any other box. Collision with other boxes is what catches the wall-graze failures behind Pendulum Hall and Gun Deck Duck.

**Robustness margins.** Accept a jump only if all of these hold:

| Tier | Timing window | Landing overlap | Other |
|---|---|---|---|
| Main zones 1–3, easy ending | ≥3–4 consecutive successful jump ticks at the *worst* alignment | ≥0.25 b | W-only input |
| Main zones 4–5, bronze, medium ending | ≥2 ticks at the worst alignment | ≥0.15 b | |
| Silver | ≥2 ticks at the best alignment and ≥1 at the worst | ≥0.08 b | |
| Gold, hard ending | ≥1 tick at the best alignment | ≥0.03 b | Success must survive a ±1° yaw perturbation and a ±0.02 b start offset |

- Do not credit the "jump one tick after leaving the edge" trick for main-line jumps. Allow it for gold only.
- Reject anything that needs chain momentum, head-hitter momentum or 45° strafe **and** sits at a window below 1.

**Whole-obstacle checks**
- **Reachability with bypass detection** (see 2.3).
- **Kill-volume overlap check:** no landing box may intersect a kill or teleport volume. This catches the Spine Scramble "instant death on the red block" bug.
- **Timed elements:** simulate with block presence as a function of tick and require success for every phase offset, or for a ≥10-tick phase window.

### 3.3 Existing open-source tools

| Tool | URL | Licence | Use |
|---|---|---|---|
| ParkourCalculator (Leg0shii) | [github.com/Leg0shii/ParkourCalculator](https://www.github.com/Leg0shii/ParkourCalculator) | **Apache-2.0** (GitHub API) | Standalone simulator for 1.8.9 and 1.12 physics, blocks and inputs. The best reference to port from: the licence allows reuse. Adjust for the 1.14+ collision order and 1.15+ slipperiness lookup |
| ParkourCalculatorMod (Leg0shii) | [github.com/Leg0shii/ParkourCalculatorMod](https://github.com/Leg0shii/ParkourCalculatorMod) | GPL-3.0 | In-game TAS planner with a Fabric 1.21.10 build ([Modrinth](https://modrinth.com/project/Lxy4H4lS)). Use it to verify jumps by hand in-game. Do not copy its code into non-GPL work |
| Grim anticheat | [github.com/GrimAnticheat/Grim](https://github.com/GrimAnticheat/Grim) | GPL-3.0 | Claims "1:1 replication of the player's possible movements" for 1.8–1.21. An excellent cross-check for edge cases (ladders, slime, honey, powder snow, collision order). GPL, so treat it as a reference only |
| MpkMod (kurrycat2004) | [github.com/kurrycat2004/MpkMod](https://github.com/kurrycat2004/MpkMod) | none stated | Parkour HUD mod (landing offsets). Useful for manual testing only |

---

## Open questions / unverified (check before you rely on them)
- Neo limits come from mcpk's equivalence rule and a crude sim. Our neo sim used a single in-air yaw switch and found about 2.25 walls flat. The real optimum is somewhat higher, but well below 3 for normal play. Port the validator first, then re-derive.
- 1.21 hitboxes for lantern, chain, end rod, pane posts and anvil (Part 1.5 rows marked [unverified]).
- Exact numbers for honey wall-slide speed, bubble columns, scaffolding climb speed and soul-sand jump distance.
- The honey jump height disagrees between sources (0.383 vs 3/16).
- Ladder jump maxima.
- The ManaCube tier jump descriptions and triple-neo cap come from search snippets, not full threads.
- Map licences: Hielke's ToS text and the Microsoft sample repo licence were not read.
