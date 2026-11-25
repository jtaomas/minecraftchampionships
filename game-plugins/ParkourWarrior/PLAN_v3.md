# Parkour Warrior: plan v3 (for owner review)

Status: **plan only.** Nothing in the code, the worlds or the server was changed to write it. The only side effect is a
read-only extraction of the MCC 6 replica zip into the agent's scratch folder, used to list its stages and render it.

Short version:
1. **Change the import source** from The King of Parkour Land to the **MCC 6 Parkour Warrior fan replica**, which is already in our
   research archive. It contains all 24 original MCC Parkour Warrior sections plus the Final Tower. Their names, signs and
   checkpoint coordinates are readable from the world. This is "recreate the reference" in the most literal sense.
2. **Keep the import tool chain** the paused agent wrote (section loader, route finder, route tool, physics fixes, label
   fix). **Drop its TKoPL stage-1 content** as main-line material.
3. **Add moving parts** by porting the TGTTOS mover system into Parkour Warrior. The model and the validator share one set
   of maths, so every moving trap is checked by the simulation before anyone plays it.
4. **Fill the remaining slots** with research-sourced mechanics: MCC SB, Dojo and Survivor obstacle types, plus the old
   obstacles you liked, rebuilt.

---

## 1. What I read (and the useful finds)

| Source | Useful finds |
|---|---|
| `ParkourWarrior/README.md`, `SPEC_v2.md`, `FEEDBACK_1.md` | Game rules stay as approved. Current validator design. Playtest 1 verdicts: main too easy, "impossible" neos, bypasses on Tick-Tock Teeth and Rattling Lift, Runaway Rails ≈10 tries = floor of the hard tier. |
| `research/parkourwarrior_design.md` | Jump limits for 1.21.1 (4b flat max, 3+1 max, neo table: triple neo never), the 48-idea obstacle catalogue, the anti-bypass rules (§2.3), and the difficulty ladder with timing windows (§2.4). |
| `research/parkourwarrior_maps.md` | Real-course structure. Obstacles are built *from objects* (hanging things, rails, façades, machinery). There are "dynamic/rhythm pieces" such as revolving doors and crumbling blocks (§5). Also hub, fork and finish architecture (§7) and readability rules (§8). |
| `mcc_archive/parkour_warrior/pw-1/*` (README, notes, GAP, STATUS, REFERENCE_IMAGES, 62 frames) | MCC: Spellbound 2026 section names: *The Revolving Doors, Pincer Parkour, Shrinking Potion, Magic Rabbits, The Blinking Beams, The Container Drop, Sliding Challenge, The Turning Sphinx, The Farm Gates, Take the L*. The wiki overview (`frames/current_wiki_overview.png`) shows the real layout: a central spine, side islands carrying giant black "monitor" letter signs, a nether tower finish, and hot-air balloons. The official render (`current_official_render_rules_thumb.jpg`) shows stone blocks hanging on ropes as footholds. |
| `pw-1/downloads/parkour_warrior_mcc6_replica.zip` (extracted read-only to scratch) | **Main find.** It is a full 1.15.2 world of the original course: 8 stages × 3 sections plus the Final Tower, about 57k blocks in a 261 × 366 area. Its command blocks give every section's name and respawn point (table in §3.2). A render matches the MCC overview style: monitor-sign islands, balloons and a giant crown. |
| `mcc_archive/parkour_warrior/pw-2/*` (Dojo + Survivor, 73 images) | Obstacle archetypes with frames: *Shifting Plates* (`000032`), hanging barrels on chains "[M1-2] The Right Pane" (`000048`), *Blinking Beams* (`000512`), *Dropping Ledges* (`000036`), *Checkerboard Swap* (`000542`), *Neo Run* (`000424`), *Take the L* (`000357`), *Turning Forks* (`000208`). Black/yellow hazard edges on every lethal edge. The checkpoint ritual. The MEDIUM/HARD gatehouses (`000544`). |
| `research/existing_maps/` README, AGENTS, `minigames/parkour_tag/README.md`, `comparison/data/what_works.md`, `video_clouds2` | Parkour Tag is a tag arena; only its ladder, trapdoor-catwalk, slime and ice vocabulary is relevant. Clouds 2 is the quality bar: forced route, obvious next direction, pads whose meaning shows at a glance. The TGTTOS Industry conveyor tables are a palette for a "belt" mechanic. Space Race shows launch pads tuned to the next target (lift matched to height). |
| `research/lobby_replica/build_v4/v4-d/detail_parkour_warrior.png` | The hub-side PW plot: teal frog statue, the blue "PARKOUR WARRIOR" board, and a hot-air balloon. These are the props for our start and hub islands. |
| `_candidates/SURVEY.md`, `GALLERY.md`, `RACE_FEATURES.md` | TKoPL is rated A for looks but is natural terrain, not PW style. Parkour Calamity has conveyors and levitation. RACE_FEATURES §7 has a trap list (shutter gate, crusher, wind zone, drop bars). |
| `Tgttos/.../FeatureRuntime.java`, `model/MapFeatures.java` | A mover model with no Bukkit dependency: pendulum, rotor (any axis) and slider. It has hazard boxes, a `hits()` sweep test, telegraphs (whoosh, grind, red glow 1 s before a slider closes), pushers (knockback), toggles with warning indicators, and crumble traps. |
| Repo search for "Parkour Warrior" | `HANDOFF.md` (status), `MAP_PROFILES.md`, `VISUAL_REFS.md`, `MAP_BRIEF.md`. The bot harness (`tools/bot-harness/`): its mineflayer bots only move randomly, so they test game flow, not jumps. Nothing else relevant. |

---

## 2. The paused import work: real state and recommendation

### 2.1 State (checked with `git status`, file times and the outputs)

- **Uncommitted, tracked files changed:** `CourseWorld` (loads `plugins/McmParkourWarrior/imported/` at runtime) and
  `CourseMap` (slot replacement, plus the **label fix**: one name label per obstacle, and difficulty and credit merged into
  one second label. This targets the overlapping checkpoint text). Also changed: `Bs` (rail-shape rotation), `Phys` (door,
  stonecutter, end-portal-frame and cactus shapes), `JumpSim` (topRect bug for local y = −1), `Validate` (imported
  sections: decoys and small objects become warnings, length up to 60), `PreviewMain` (optional import folder) and
  `render.sh` / `side_view.py` (paths).
- **Untracked, new:** `map/ImportedCourse.java` (course.txt format: section, base, origin, end, turn, crop, drop, set, via,
  avoid, killdepth, tag, credit), `map/Router.java` (easiest-route search over a section, re-routing around jumps the
  simulation calls impossible) and `tools/ImportRoute.java`.
- **`_candidates/pk/`** (git-ignored): cuts of TKoPL canyon, sky, saw and Hylur pieces; `stage1/course.txt` with M1-1
  Crumble Canyon, M1-2 Canyon Ledges, M1-3 Canyon Descent and B1-1..B1-3 Skylands.
- **The last full validation of stage 1 FAILED with 28 errors**:
  - "impossible" first jumps in 5 sections;
  - zone-1 windows too tight;
  - a neo in zone 1;
  - gold needing about 95 attempts;
  - bronze B1-1 at about 4100 attempts;
  - the ramp broken;
  - the start/respawn point not standable at y 101.
- That validation predates the last `course.txt` edits (the added pillars, log step and ledge). After those edits only
  M1-3 was re-routed (est. 1.1 attempts, i.e. *too easy* for anything but zone 1). **Stage 1 was never validated in its
  final form.**
- The server has **no** `imported/` folder, so the live game still runs the built-in course.

### 2.2 Why TKoPL is the wrong source for the course itself

- It reads as natural terrain and a timber village. The MCC frames show floating, object-built set pieces.
- Its sections are long walks with jumps in between. The validator had to stretch the length limit to 60.
- It needed hand easing (`set` pillars) just to be possible in zone 1. That is the opposite of "recreate the reference".
- The 5-block "impossible" first jumps came from the route finder picking lines through open terrain. Open terrain is also
  where bypasses come from.

### 2.3 Recommendation

| Keep | Drop or park |
|---|---|
| `ImportedCourse`, `Router`, `ImportRoute` (generic, work on any cut) | TKoPL stage-1 `course.txt` as course content (park it in `_candidates/pk/`, no deletion) |
| The label fix in `CourseMap`/`CourseWorld` (verify in game) | Hylur and saw cuts (park) |
| `Bs`, `Phys`, `JumpSim` fixes (they correct real bugs) | |
| `Validate` import allowances. Bypass stays an **error** for imported sections | |
| Runtime loading from `plugins/McmParkourWarrior/imported/` (third-party blocks never go into the jar or git) | |
| TKoPL sky islands as **backdrop only**, if wanted (Q2) | |

Fix before reuse: the "spawn not standable" error the import run produced at the start island.

---

## 3. Proposed course

The structure is unchanged from SPEC_v2 (approved): start, then 5 zones × 3 main obstacles, a hub with a 3-obstacle bonus
spur (bronze, silver, gold) at each zone, then the fork, then Easy / Medium / Hard endings of 2 obstacles each. What
changes is **where each obstacle comes from** and **how hard it is**.

### 3.1 Difficulty ramp (validator targets)

The attempt estimates use the existing model, calibrated on Runaway Rails = 10.1.

| Slot | Target est. attempts | Minimum window (worst/best) | Feel |
|---|---|---|---|
| Zone 1 main | 1.2 – 1.6 | 3/4 | Real sprint jumps from the first obstacle; no 1b gaps |
| Zone 2 main | 1.5 – 2.5 | 2/3 | Precision starts; first moving part |
| Zone 3 main | 2 – 3.5 | 2/3 | First real parkour: single neo, ladder catch, head-hitters |
| Zone 4 main | 3 – 5 | 1/2 | Medium-hard; one 3+1 at most per obstacle |
| Zone 5 main | 4 – 8 | 1/2 | Hardest main; always **below** Runaway Rails |
| Bronze n | above zone n main max | 1/2 | |
| Silver | 5 – 10 | 0/1 | |
| Gold | **10 – 25** (Runaway Rails = floor) | 0/1, ±1° yaw robust | |
| Easy ending | ≈1 | 3/4 | Victory lap (owner: "fine") |
| Medium ending | 3 – 6 | 1/2 | Like zone 4; not the old ice slide |
| Hard ending | above every other obstacle | 0/1 | Short and steep, ≤ 40 blocks |

The validator already enforces the ramp, bronze < silver < gold and "hard is hardest". The new rule is a **lower** bound
per zone, so the course can no longer be "too easy" and still pass.

### 3.2 Where every slot comes from

The replica stage list below was read from the world's signs and command blocks. The start is the respawn armour-stand
point, in replica world coordinates. Each section runs from its start to the next row's start.

| Replica section | Start (x y z) | Replica section | Start (x y z) |
|---|---|---|---|
| 1-1 The Lofty Leap | 7 15 2 | 5-1 The Helpful Dolphins | -34 28 306 |
| 1-2 The Hanging Hoops | 19 15 29 | 5-2 The Anchors | -65 32 296 |
| 1-3 The Sidesteps | 47 15 30 | 5-3 The Wall Clinger | -64 33 256 |
| 2-1 The Barrel Hop | 97 10 39 | 6-1 The Six Rings | -95 27 223 |
| 2-2 The Narrow Ledge | 85 13 84 | 6-2 The Lateral Leaps | -132 27 222 |
| 2-3 The Switchback | 56 13 83 | 6-3 The Tuning Forks | -130 27 179 |
| 3-1 The Double Weaver | 20 21 123 | 7-1 The Sneaky Jumper | -73 24 180 |
| 3-2 The Holey Moley | 9 18 146 | 7-2 The Frozen Path | -38 24 174 |
| 3-3 The Wide Hoops | 9 23 182 | 7-3 The Broken Bars | -38 21 130 |
| 4-1 The Ladder Hop | 23 24 226 | 8-1 The Trouble U's | -38 23 90 |
| 4-2 The Triple Bounce | 23 27 259 | 8-2 The Chain Pipes | -37 23 53 |
| 4-3 The Ladder Climb | 5 25 301 | 8-3 The Final Tower | -33 24 -32 |

**Provisional slotting.** It is re-ordered by *measured* difficulty after the Phase 1 survey: the original stage order is
a good first guess, but the replica is a fan recreation, so every jump is simulated before it is trusted.

| Zone / hub (theme from the replica) | Main line (stone) | Bonus spur: bronze / silver / gold |
|---|---|---|
| 1 Mesa | 1-1 Lofty Leap, 1-2 Hanging Hoops, 1-3 Sidesteps (replica) | **Shifting Plates** (new: stepping toggle) / **Jump-Rope Sweeper** (new: rotor) / **Runaway Rails** (kept, the gold floor) |
| 2 Garden | 2-1 Barrel Hop, 2-2 Narrow Ledge, 2-3 Switchback (replica) | **Dropping Ledges** (new: crumble) / **Revolving Doors** (new: turnstile) / **Swing-Catch Barrels** (new: pendulum + dwell) |
| 3 Jungle | 3-1 Double Weaver, 3-2 Holey Moley, 3-3 Wide Hoops (replica) | Space spur: 4-1 Ladder Hop / 4-2 Triple Bounce / 4-3 Ladder Climb (replica) |
| 4 Ocean | 5-1 Helpful Dolphins, 5-2 Anchors, 5-3 Wall Clinger (replica) | City spur: 6-1 Six Rings / 6-2 Lateral Leaps / 6-3 Tuning Forks (replica) |
| 5 Ice | 7-1 Sneaky Jumper, 7-2 Frozen Path, 7-3 Broken Bars (replica) | **Pincer Press** (new: crushers) / **Blinking Beams** (new: two-phase toggle) / **Checkerboard Swap** (new) |
| Endings | Easy: **Honeycomb Hop + Bloom Stepping** (kept: owner said fine; reskinned to the fork's palette) · Medium: **8-1 Trouble U's** (replica) + **Rattling Lift v2** (kept idea, bypass fixed) · Hard: **8-2 Chain Pipes + 8-3 The Final Tower** (replica, chains restored) | |

That is 24 replica sections, 8 new-mechanic obstacles, 1 kept (Runaway Rails), 1 kept idea (Rattling Lift) and the 2-part
Easy ending. The total is 36 slots.

- **Main line = replica only.** This is the strongest "recreate, don't invent" position. Moving parts reach the main line
  through the replica-themed versions in the spares pool (below), only where the zone target needs them.
- **Spares pool.** The real course "randomises and changes for every event" (pw-1 `notes.md` §1; `parkourwarrior_maps.md`
  §3). A seed-selected spare can replace a slot of its tier, and the whole set is validated together. Spares:
  - **Rigging Ropes**, with your fixes: first jump shorter (gap 3 → 2, or +1 → 0), second rope 2 vines wide instead of 1;
  - **Tick-Tock Teeth** (sides walled, 2.5 bc ceiling, kill volume beside it);
  - **Pendulum Hall v2** (new mover, safe-window check);
  - **Gear Train** (gears turn on a toggle, one notch harder);
  - **Cargo Net Catch**, **Plankless Rope Bridge**, **Geode Steps** (+1 step harder).

  The replaced built-in obstacles stay in code as further spares.
- **Hubs and frame are not imported pieces.** Start island, hubs, fork and finishes are built to the references (§5).

### 3.3 Layout

- One straight spine in +z. Each hub's spur goes out at +x (as now). The next hub is visible from the current one
  (`parkourwarrior_maps.md` §8; Clouds 2 standard).
- Replica sections are pasted **as cut**: obstacle plus its own scenery props, turned to +z, start on the previous pad and
  a 3×3 completion pad at the end (the existing `ImportedCourse` contract).
- Kill height is lowest foothold − 6. A kill column is applied around each section so nobody walks along scenery.
- Hub islands carry the replica's giant monitor sign showing the zone number (the replica has 5 of them; overview shows
  the same). There is a "BONUS PATH" banner toward the spur and a frog statue at the junction.

---

## 4. New mechanics and traps

### 4.1 Engine: reuse the TGTTOS mover system

- **Port the model, do not rewrite it.** Copy `MapFeatures.Mover / Toggle / Trap` into Parkour Warrior's `CourseModel`.
  They are pure Java, with the same pose maths (`angle`, `extension`, `toLocal`, `hits`, `frameAt`, `warning`).
- **Port `FeatureRuntime`** with Bukkit types as they are. Swap only TGTTOS's `MapPlacer` / `TgMapData` for `CourseWorld`.
  Kept as is:
  - block-display movers updated every 2 ticks (sliders every tick);
  - telegraphs: whoosh, grind, red glow 1 s before closing;
  - toggles that never place a solid block into an occupied cell;
  - crumble traps with creak, then drop, then restore;
  - pusher knockback (`pushSpeed` / `pushLift`, 10-tick cool-down);
  - `stop()` restoring every cell.
- **One global clock** (ticks since GO). Players are ghosts and never collide, so every runner sees the same phase.
- **What a hit does.** A hazard hit is the same as a fall: back to the checkpoint, red line "Hit by the <name>". A pusher
  knocks you away; whether that is a fall depends on where you land.
- **Three extensions are needed:**
  1. **Dwell platform:** a real block (Toggle) that exists only while a display mover sits at a set pose. Used for the
     swing-catch: the barrel stops at the end of its swing and becomes solid. Standing on display entities is not possible.
  2. **Belt zone:** while the feet are on given cells, add a fixed velocity per tick. This gives conveyor-like pushes with
     stripe blocks from the TGTTOS Industry palette.
  3. **Phase lock:** an optional per-obstacle offset, so the 3 movers of one obstacle stay in rhythm.
- **Not used:** TGTTOS's "solid" barrier trick for moving arms. A barrier placed into a moving player's cell glitches.
  Arms that touch you are pushers instead.

### 4.2 How a moving obstacle stays fair and checkable

Every check runs in the same tick model as the runtime, because it is the same code.

1. **Platforms that move are discrete.** Stepping toggles hold every state for at least 10 ticks on the main line and
   bronze, and at least 6 on silver, gold and Hard. JumpSim treats each state as a static world. The validator needs one
   working jump inside each dwell window (this extends the existing timed-block phase check).
2. **Hazard sweep.** For each step, take JumpSim's successful trajectory (feet box per tick) and test it against every
   mover's hazard boxes for **every start phase** of the period. A 0.1 margin is added and the runtime's "pose the player
   saw" lag (t − 1 + u) is matched. Required:
   - at least 15 consecutive safe start ticks per period on the main line, 10 for bronze and silver, 6 for gold and Hard;
   - a **safe waiting foothold** before every hazard, i.e. one no hazard touches during the whole period.
3. **Crumble.** Each crumble foothold has a deadline: the next jump must work from the landing state within the
   delay. Delay is at least 20 ticks on the main line, 12 on gold. The check uses JumpSim's chained window.
4. **Belts.** The belt velocity goes into `Phys` per tick, so the push is simulated, not guessed.
5. **Bypass search** runs with every toggle state present at once, the conservative union. This would have caught
   Tick-Tock Teeth and Rattling Lift. Pushers and hazards count as walls for the bypass search.
6. **Telegraph rule.** Every mover has at least 1 s of warning: glow, indicator blocks or sound
   (MCC R3: "moving blocks show indicators", pw-1 `notes.md` §1).

### 4.3 Catalogue

| # | Mechanic | What the player does | Ramp position | Fairness / sim check | Source |
|---|---|---|---|---|---|
| 1 | **Shifting Plates** (stepping toggle) | Plates step sideways one cell at a time on a beat; jump when the next plate is in line | Bronze 1 (zone-2 main as spare) | Rule 1; 20-tick dwell; indicator ghost blocks | Dojo `[1-1] The Shifting Plates`, pw-2 `000032`; design #42 |
| 2 | **Jump-Rope Sweeper** (rotor, axis y or x, 1 high) | A bar sweeps across a 2b chain at foot height; jump it while chaining | Silver 1 | Rule 2; the bar is a pusher (knock-off = fall) | design #43 Sweepers; TGTTOS rotor |
| 3 | **Dropping Ledges** (crumble trap) | Ledges creak and drop 20 ticks after landing; keep moving | Bronze 2; zone 3 spare | Rule 3; restores after 60 ticks | Survivor `[1-4] The Dropping Ledges`, pw-2 `000036`; TGTTOS Trap; design #44 |
| 4 | **Revolving Doors** (4-arm y-rotor turnstile on a platform) | Step into the turning gap, ride it a quarter turn, jump out | Silver 2 | Rule 2 with the arms as pushers; safe hub inside the gap | MCC SB silver "The Revolving Doors" (pw-1 `current_000105_void_bonus.jpg`); maps.md §5 #12 |
| 5 | **Swing-Catch Barrels** (pendulum display + dwell platform) | Barrels on chains swing; each is solid for about 12 ticks at the end of its swing; chain 4 catches | Gold 2 | Extension 1 + rule 1 (6-tick minimum); chain catch simulated | Dojo hanging barrels `000048`; official render hanging stones (`current_official_render_rules_thumb.jpg`); maps.md §4 "Hanging chains" |
| 6 | **Pincer Press** (paired sliders, never fully shut) | Two crusher jaws close over a 1-wide beam; pass in the open phase; jaws leave a slot | Bronze 5; zone 4 spare | Rule 2; jaws are hazards (respawn); 1 s red glow | MCC SB "Pincer Parkour" (pw-1 `notes.md` 2:36); RACE_FEATURES §7 crusher |
| 7 | **Blinking Beams** (two-phase toggle) | Two beam sets alternate on and off; move diagonally between sets | Silver 5; zone 3 spare | Rule 1; 30/30 ticks main, 16/16 silver | MCC SB silver (`current_000267_blinking_beams.jpg`); Dojo `[3-3]` `000512` |
| 8 | **Checkerboard Swap** | A black/white grid swaps; stand on the colour about to stay, read the swap telegraph | Gold 5 | Rule 1 plus rule 6 indicators; every route cell has a legal dwell | Survivor final duel `000542` |
| 9 | **Rattling Lift v2** (vertical stepping toggle) | A cage of slabs rises in steps; climb as it rises; walls on 3 sides | Medium ending (part 2) | Rule 1 + rule 5 (bypass with all states) | Owner liked it (FEEDBACK_1); design §2.3 fixes |
| 10 | **Pendulum Hall v2** (pendulum hazards across a jump line) | Time 3b jumps between swinging weights | Spare (zone 4 or bronze) | Rule 2 + a safe waiting foothold (the v1 bug: no safe window) | Owner liked it; TGTTOS pendulum |
| 11 | **Shutter Doors** (slider closing a doorway) | Doors on a beat over a head-hitter frame; go when open | Spare (zone 3–4) | Rule 2; open ≥ 40 ticks main | MCC SB "The Farm Gates" (name); RACE_FEATURES §7 shutter gate |
| 12 | **Conveyor Deck** (belt zone) | Striped belts push sideways while you sprint-jump across them | Spare (zone 4 / bronze) | Rule 4 (simulated); in-game feel test (server-side push and ping) | maps.md §5 #7 "conveyor-like stripes"; Parkour Calamity conveyors (GALLERY P4); TGTTOS Industry conveyor tables |
| 13 | **Shrinking Potion** (scale attribute 0.5 inside a box) | Shrink to pass 1-high tunnels, then grow back before the next jump | Optional novelty (bronze) | Needs the scale in `Phys` (box, step height); untested in 1.21.1 → Q4 | MCC SB section name "Shrinking Potion" (`current_000213_magic_stage.jpg`); mechanic inferred from the name only |

**Hard, non-moving parkour** (the "real hard parkour" ask) is used in silver, gold and Hard. It comes from
`parkourwarrior_design.md` §2.2 and §2.4:
- 3+1 chains; 4b with 1 bm;
- fence and pane posts; anvil beams;
- head-hitter chains; ladder top-out jumps;
- ice 5b runways; slime ricochet;
- neo into a 3+1.

Never: a triple neo, or a wall 3 or more long. Many replica sections (Wall Clinger, Lateral Leaps, Tuning Forks, Final
Tower) already use these in their original form.

---

## 5. Theming and decoration

**Rule: build agents get the image paths and the replica cut, and recreate them.** No text-only briefs.

| Area | Recreate from |
|---|---|
| Replica sections | The replica cut itself, plus the matching original POV frames (`pw-1/frames/original_*`, e.g. `original_000076_jungle_slime.jpg`, `original_000156_space_ladder.jpg`, `original_000212_ocean_complete.jpg`, `original_000356_nether_take_the_l.jpg`, `original_000644_final_tower.jpg`). 1.15 stand-ins (iron bars or fences for chains) become chains where the frames show chains. |
| Start island | `current_000000_start_gate.jpg` (red-banner gate, stone path); `current_000060_mesa_stage.jpg` (PARKOUR WARRIOR arch); lobby `detail_parkour_warrior.png` (frog, board, balloon) |
| Hubs | Replica monitor-sign islands; `current_wiki_overview.png`; `maps.md` §7 (torii, lanterns, frog statue, BONUS PATH banner); Dojo `000132`, `000224` |
| Fork pavilion | `maps.md` §7 (three gates, EASY/MEDIUM/HARD hanging signs, lime/orange/red pillars); Dojo `000544` |
| Finishes | `current_000528_course_completed.jpg` (gold frog trophy, torii); Hard = replica Final Tower checked against `original_000644_final_tower.jpg` |
| Movers | Themed to their zone: anchors and barrels on chains (Ocean / Garden), pistons with hazard stripes (Ice / industrial), using the hazard-stripe language from pw-2 `notes.md` §1.7 |
| Backdrop | Replica balloons, crown and monitor islands; the overview's floating islands 20–80 blocks off the line; TKoPL sky islands only if Q2 = yes |

**No "cooked" decoration.**
- Every prop is a complete object with a closed silhouette: no half boats, no floating roof or house fragments.
- The cut tool drops partial props at section edges instead of leaving them.
- New validator **warning**: any detached cluster of fewer than about 30 blocks within 25 blocks of the route that is not
  a foothold, a mover or an intentional hanging prop.
- Render review: each obstacle close-up is placed beside its reference frame in one sheet (`_candidates/tools/montage.py`
  already exists). The sheet goes to you before any in-game build.

**Readability (the Clouds 2 standard).**
- Footholds are warm or light against dark structure.
- One label per obstacle, plus one difficulty/credit label below it (the fix already in the working tree).
- Lime / orange / red difficulty colours on signs, pillars and finish portals.
- The next hub is visible from each pad.

---

## 6. Testing

1. **Simulation (gate for everything).** `Validate` on the assembled course: every step's window per tier, the zone lower
   and upper bounds (§3.1), bypass (all toggle states present), kill overlap, mover sweep (§4.2), crumble deadlines,
   belts in `Phys`, the cluster warning, standable spawns. The build fails on any error. `ImportRoute` per section during
   the survey.
2. **Model unit tests.** Pose maths, `hits`, `frameAt` / `warning` and dwell sync are shared with the runtime. Add tests
   that the validator and the runtime return the same answer for recorded ticks (TGTTOS already pairs these).
3. **Replay bots (new, recommended).** Export JumpSim's winning input sequence per obstacle (yaw, W/A/D, sprint, jump per
   tick). A mineflayer bot replays it on the staging server via `tools/bot-harness`. mineflayer has its own physics, so
   this is an **independent cross-check** of "possible", run against the live movers. Report pass/fail per obstacle; any
   fail blocks the owner test.
4. **Fall telemetry.** The plugin logs falls per obstacle per player during playtests. After each playtest, compare with
   the estimates and recalibrate. Runaway Rails = 10 is our only calibration point now.
5. **Renders.** Whole course (top, side, iso), each hub, the fork, each obstacle close-up next to its reference, mover
   poses at 4 phases.
6. **Owner playtest checklist.** Ramp feel per zone; mover telegraphs; pusher strength; slime with jump released
   (1.21.1); vines; label overlap fixed; scroll/fork flow unchanged.

---

## 7. Open questions for the owner

1. **Switch the source to the MCC 6 fan replica** (private, loaded at runtime from the server folder, never in git or the
   jar, as with TGTTOS imports)? This replaces the TKoPL plan.
2. **Themes:** keep the replica's original zones (Mesa / Garden / Jungle / Ocean / Ice, with Space and City spurs and a
   Nether finish), or reskin toward the current MCC zones (industrial, candy, city, village)? Recreating as-is is cheaper
   and truer.
3. **Moving traps:** may hazards (hit = back to checkpoint) and pushers appear on the **main line** from zone 3, or only
   on bonus spurs, endings and spares?
4. **Shrinking Potion** (scale attribute): in or out?
5. **Credits:** show "Section from <map> by <author>" on imported obstacles (the working tree already supports this), or
   keep the MCC-style `[M1-1] Name` label only?

---

## 8. Phases and size

Sizes are rough agent time: **S** ≈ under half a day, **M** ≈ 1 day, **L** ≈ 2–3 days.

| Phase | Work | Size | Exit condition |
|---|---|---|---|
| 0 Decide | Owner answers §7. Park the TKoPL content. Review and keep the tool-chain diff (no commit until asked) | S | Answers in hand |
| 1 Replica survey | Cut all 24 sections plus the tower with `mca2tg`/`anvil` (1.15 palette path). Write `course.txt`. Run `ImportRoute` and JumpSim on each. Produce a difficulty table and close-ups beside the original frames. Fix the import start-island spawn error | M | Every section has a route, an estimate and a render; impossible or too-long ones listed with proposed minimal edits |
| 2 Mover engine | Port Mover/Toggle/Trap and FeatureRuntime. Add dwell, belt and phase-lock. Validator sweep, crumble and belt checks. Unit tests | M–L | Validator and runtime agree on recorded ticks; a test obstacle per mover type passes |
| 3 Assembly | Slot the replica sections by measured difficulty. Build the 8 new-mechanic obstacles and Rattling Lift v2 from their reference frames. Rework the spares (Rigging Ropes fixes etc.) | L | `Validate` PASS, 0 errors, ramp within §3.1 |
| 4 Frame and theming | Start island, hubs, fork and finishes from the references. Backdrop. Cluster check. Reference-sheet renders | M | Owner approves the render sheet |
| 5 Test | Replay bots on staging, flow bots, fall telemetry on, then the owner playtest | M | Bots pass every obstacle; playtest notes captured in `FEEDBACK_2.md` |

Nothing in phases 1–5 starts without your go-ahead. Third-party map data stays in `_candidates/` and the server data
folder only.
