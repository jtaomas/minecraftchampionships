# Parkour Tag: plan to match MCC

Status: **plan only.** Nothing has been built, pasted or edited. The owner reads this before any work starts.
Date: 2026-11-25. Area: `game-plugins/ParkourTag`.

Paths below are relative to `game-plugins/` unless they start with `tools/` or `_candidates/` (repo root).

---

## 1. What I read, and what was useful

| Source | Useful finds |
|---|---|
| `ParkourTag/README.md` and all 7 classes (`ParkourTagGame` 1,300 lines, `ArenaBlueprint`, `ArenaBuilder`, `SpectatorGuard`, `FastTest`, `VoidGenerator`, plugin) | Working round-robin game loop. The arena is a seeded random generator (see §2). |
| `research/mcc_archive/parkour_tag/pt-1/` (notes, REFERENCE_IMAGES, STATUS, downloads/README, 32 frames) | Factory, Obelisk, Plaza, Prefecture: 8 official renders, 19 POV/aerial frames, palette per map, landmarks, routes, hiding spots. The Zibly "Factory" world download was checked and **rejected**: it contains no arena. |
| `research/mcc_archive/parkour_tag/pt-2/` (notes, REFERENCE_IMAGES, STATUS, `downloads/wiki_pages.json`, 104 frames) | Urban Jungle, Winter Town, The Dustbowl: 86 VOD stills, 9 HUD crops, 4 wiki renders. A full HUD and chat-message catalogue. The round beat sheet with timings. The raw MCC wiki text (rules, coin history, map trivia). **Dustbowl has a true top-down frame** (`022412_dustbowl_topdown_arena.jpg`), 1080p. |
| `research/mcc_archive/_index/leads/pt-1.md`, `pt-2.md` | Video leads that were not downloaded: Noxcrew update videos (Factory MCC 14, Dustbowl MCC 21, Prefecture HH/Blank Canvas) and Zibly's "How to Build MCC's Urban Jungle" (14:27). |
| `research/existing_maps/README.md`, `AGENTS.md`, `minigames/parkour_tag/` (dossier, renders, 40+ analysis scripts) | The only imported Parkour Tag world is **MG-PT-01**, a fan arena in `mcc_minigames` (daniellaur, 1.17.1, 20×20, 1 hunter vs 1–4 runners). It is **not** an MCC map. Patterns worth copying: the invisible barrier roof used as a spectator floor, and the black→red→yellow→lime glass door countdown. Pattern to avoid: glowing **runners**, which is the reverse of MCC. Its `standable.py` / `connectivity.py` / `paths.py` scripts can be reused for route checks. Research only: no world edits without the owner. |
| `research/existing_maps/comparison/data/*.md` | Confirms the takeaways above (door-glass countdown, notched 60 s bar, barrier-roof deck; never use XP level as state). |
| `research/lobby_replica/build_v4/v4-d/detail_parkour_tag.png` + STATUS §4.5 | The hub pavilion only ("no direct reference"). It is not relevant to the arena. |
| `MAP_PROFILES.md` | Parkour Tag profile: 30–50 across, 3–4 tiers, about 15 tall, loop with no dead ends, 5 arenas in parallel. |
| `_candidates/SURVEY.md`, `GALLERY.md`, `_candidates/maps/*` | **No Parkour Tag or tag-arena world was downloaded.** Only parkour courses (TKoPL, Calamity, PP3), which do not fit. |
| `tools/README.md`, `tools/bot-harness/`, `BattleBox/tools/PreviewMain.java`, `ParkourWarrior/.../JumpSim.java`, `_candidates/tools/montage.py` | Existing tooling for offline renders (`.mcmdump` → iso/top PNG), side-by-side sheets, jump checks, and mineflayer flow bots. |

---

## 2. Our Parkour Tag today, and its gaps against MCC

**What works (keep):**
- Sides from tournament teams (solos are grouped), round-robin pairings with byes, each pairing in its own arena copy. This is the MCC structure.
- Both hunters play at once in a pairing. Tagging is a melee hit. Tagged runners go to spectator, held in by `SpectatorGuard`. No fall damage, no hunger, no block break or place.
- 60 s rounds, early end when no hunted runners are left, survival ticks every 10 s.
- Runner Vision on an Eye of Ender: 3 s effect, 10 s cooldown (the MCC values).
- Admin skip and fast-test hooks.

**Gaps against MCC:**

| # | Ours | MCC | Weight |
|---|---|---|---|
| G1 | **One arena, generated from `Random(20201114)`:** a 5×5 grid of random "tower/steps/stilts/vineWall/hill" cells plus **45 random floating stepping stones**, sandstone hills, and a barrier box with an open sky. This is the "random blocks everywhere" layout the owner rule forbids. | 7 hand-built themed maps, each with landmarks, a readable route-colour language, loops and an enclosure (sky-mural walls or a stadium). | **Critical** |
| G2 | Players freeze on floor pads. | Each team starts in a **spawn cage**: a small room with team-coloured glass, a `Select Hunter:` wall sign, and a door that opens after `Starting in 5..1`. | High |
| G3 | The hunter is picked automatically (least-hunted player first). | Players **volunteer** by clicking the sign during an 8 s `Select Hunter` phase. If nobody does, one is picked at random. Each player can hunt **at most 4 times**. Chat: `X is going to hunt the opposing team this round. They can hunt N more times.` | High |
| G4 | The hunter is blinded and frozen for 3 s after `RUN!` | No source shows a hunter delay. Both cages open on the countdown. | Medium (owner call) |
| G5 | Runner Vision draws client particles and a beam. It does not show through walls. | A real outline of the enemy hunter, through blocks, seen only by the runner who used it. A `Recharging:` 10-segment bar sits above the hotbar. | Medium |
| G6 | The sidebar is the same for everyone. Roles are shown in the action bar. | Sidebar with phase label (`Select Hunter` / `Time left` / `Next Round`), `Runners alive:` dots per team, and overlays: `Tagged!`, `Hunted!`, `Survivor!` / `Well played!`, `Round Over!`. | Medium |
| G7 | Only one map. | One map per event, used for all 9 rounds. | High (follows G1) |
| G8 | Placeholder points. | MCC coins (another agent owns this; see §3.8). | Not in scope |

---

## 3. MCC Parkour Tag rules to match

Source keys: **W** = MCC wiki text saved in `research/mcc_archive/parkour_tag/pt-2/downloads/wiki_pages.json`. **N1** = `pt-1/notes.md`. **N2** = `pt-2/notes.md`. **F:** = a frame in `pt-1/frames` or `pt-2/frames`.

### 3.1 Teams and rounds
- 3v1 per side: 4-player teams, **1 hunter + 3 runners** each. Two teams meet per arena. Each hunter chases the other team's runners at the same time. (W; N2 §1)
- **9 rounds × 60 s.** Each team meets a different opponent each round. With 10 teams that is 5 arenas running at once. (W "9 rounds, each against other teams"; MAP_PROFILES "5 parallel"; N2 §1)
- A note on sources: N1 §1 says "three match-ups against each opponent" for 4 teams. That is a 4-team reading; W and N2 match the 10-team event. For our small groups, the current round-robin repeat is correct.
- Measured cadence is about 90 s per round: round card about 2 s → cage and hunter select about 8 s → `Starting in 5..1` → 60 s → results with `Next Round: 00:08`. A full game takes about 13 m 45 s. (N2 §1, from MCC 12 and MCC 21 VOD timestamps)

### 3.2 Hunter selection
- Each team picks its own hunter. Whoever presses the button or sign becomes hunter. If nobody does, one is picked at random. (W)
- **Max 4 hunts per player.** This was 3 before MCC 15. (W History)
- The prop is an oak wall sign `Select Hunter:` inside the cage (F: `022304_dustbowl_cage_starting.jpg`). The sidebar shows `Select Hunter: 00:08` in pink (N2 §1).
- Chat line: `<player> is going to hunt the opposing team this round. They can hunt N more times.` (F: `022308`, `002114`)
- To match, add: a click-to-volunteer sign or item during an 8 s phase, a random fallback, the 4-hunt cap, and the chat counter. When teams are smaller than 4, keep our existing fallbacks (1v1 alternation and so on).

### 3.3 Round timers
- Lobby `Game begins`, then per round: `Select Hunter` (8 s), `Starting in 5`, `Time left` (60 s, red), `Next Round` (8 s, pink). (N2 §1 table; F: `022406`, `001756`)
- A round ends when the timer reaches 0 or all enemy runners are tagged. (N1 §1; W)
- The 3 s hunter blind/freeze is **not sourced** (G4). See open question 3.

### 3.4 Tagging
- Melee hit. The tag feed reads `<runner> was tagged by <hunter>`, with a crossed-swords glyph and an `ELIMINATION!` banner. (F: `000004_prefecture_tag-elimination-hud.jpg`; N1 §2)
- The tagged runner becomes a **spectator** (since MCC 13; W History), with a red vignette. Living teammates are outlined in team colour (F: `015941`, `020029`).
- Chat for the tagged runner: `You got tagged! You survived for N seconds.` (F: `022412`)
- Ours already tags on hit. Keep it.

### 3.5 Speed, potions and items
- **No speed or jump potions are shown** in any frame, the wiki or the notes. Do not add any.
- Hotbar (F: `022932_dustbowl_runnervision_hud_crop.png`): slot 1 is Runner Vision (a dark orb), slot 2 is a **white feather whose use is unknown**, slots 3–9 are red team filler. An armour bar shows above the hearts (N2 §3). It is unclear whether the hunter wears armour.
- **Runner Vision:** right-click shows the enemy hunter's outline through blocks for **3 s**, only to that runner. **10 s cooldown** with a recharge bar. Using it too early prints `Runner Vision Unavailable!` (W; N2 §3)
- History: before MCC 14 the hunter flashed after each tag instead (W). We match the current version.
- Map-built movement aids are part of the maps, not effects: **ice rinks** (Winter Town, Plaza, Dustbowl), ice slides (Plaza), slime is not seen. (W map pages; N1 §5; N2 §7)

### 3.6 Map rotation
- **One map per event, used for all 9 rounds.** The map is not rotated inside a game. (N1 §1; W appearance table: each event lists one map)
- There are 7 maps in total: Urban Jungle (MCC 12), Winter Town (13), Factory (14), Plaza (17), The Dustbowl (21), Prefecture (HH), Obelisk (27). Variants: Plaza Winter/Halloween, Obelisk Halloween, Factory Pride, Pridebowl. (W)

### 3.7 Elimination order and survival
- The order of elimination is the order of tags. Each runner's survival time is reported (`You survived for 34 seconds`). (F: chat in N2 §5)
- Every 10 s alive: `You successfully survived for N seconds.` (aqua). (N2 §5)
- Any runner alive at 0:00 → the team "outlasted the hunter" (`Survivor! / Well played!`, green). (F: `022405`, `022406`)
- The hunter clears all three → `Hunted! / Well played!`. Whichever hunter **clears first** wins a bonus for their team. The round continues for the other hunter. (F: `022832`; N2 §5 chat lines)

### 3.8 MCC coin rules (note only; scoring belongs to the other agent)
Current values (W, from MCC 16, with the P24 change):
- Runner: 2 coins per 10 s survived. 60 coins shared by the runners if any runner survives to 0:00. From P24, +2 more for surviving the full 60 s.
- Hunter: 6 per tag, −1 per 10 s elapsed. 42 for tagging all runners, −7 per 10 s elapsed.
- Team: 120 shared if your hunter clears the enemy before their hunter clears you.
- In-game chat shows `[+60.0] Your team outlasted the hunter!` and `[+42.0] You've tagged all enemies!` (N2 §5). An older frame shows +25 for 30 s, which does not fit; trust W.

---

## 4. The maps

None of the seven MCC maps exists as a world anywhere in the repo, in `research/existing_maps`, or in `_candidates`. No public download exists (pt-1 and pt-2 `downloads/README.md`). **Every map must be recreated from images.**

| Map | Dossier | Frames (key ones) | Overview quality | World? |
|---|---|---|---|---|
| **The Dustbowl** | N2 §7.3 | `pt-2/frames/022220`…`022936` (40 stills + 6 crops). Key: `022412_dustbowl_topdown_arena.jpg` (**true top-down**), `022416`/`022420` overhead, `022632`/`022640`/`022648` aerials, `022552` billboard, `022700`/`022704` ice rink, `022304`/`022308` cage, `dustbowl_wiki_render.png`, `_pride.png`, `00_index_dustbowl.jpg` | **Best: 1080p**, top-down + render + 3/4 views | None |
| **Urban Jungle** | N2 §7.1 | `pt-2/frames/015705`…`020333` (24 + 2 crops). Key: `urbanjungle_wiki_render.png`, `015710_arena_overview`, `015717_start_area_aerial`, `020121_wide_arena`, `020033_arena_red_roofs`, `015813_roof_parkour` | Good (720p), square box, simple 4-colour palette | None. Lead: Zibly block-by-block build video (not downloaded) |
| **Factory** | N1 §5 | `pt-1/frames/parkourtag_factory_normal.png`, `_pride.png`, `000044`, `000046`, `000049`, `000212_factory_recreation_aerial-1`, `000218_…aerial-2` (**fan recreation**, labelled) | Medium: no official flyover | None (Zibly download rejected) |
| **Plaza** | N1 §5 | `parkourtag_plaza_normal/halloween/festive.png`, `000050_flyover-mural-street`, `000100_flyover-town-square`, `000059`, `000067` | Medium-good: official flyover | None |
| **Obelisk** | N1 §5 | `parkourtag_obelisk_normal/halloween.png`, `000154_overview-aerial`, `000212_obelisk_start-hud-overlay`, `000202`, `000221`, `000303` | Medium: official aerial + spawn platform | None |
| **Prefecture** | N1 §5 | `parkourtag_prefecture_normal.png`, `000004`, `000015`, `000127`, `000227`, `000333` | **Weak:** POV only, no overview | None (a Bedrock "Japan" fan map lead only) |
| **Winter Town** | N2 §7.2 | `pt-2/frames/001724`…`002114` (22 + crop), `wintertown_wiki_render.png` | Good (720p) | None |
| *(not MCC)* MG-PT-01 | `research/existing_maps/minigames/parkour_tag/README.md` | `minigames/frames/parkour_tag_iso.png`, `_top.png`, `parkour_tag/frames/*` | Exact (imported) | **Yes**, in `mcc_minigames`. A fan arena, so use it for patterns only |

**Build first:**
1. **The Dustbowl.** It has the strongest evidence by far: a 1080p true top-down, a render, three aerials and 40 stills. The wiki describes it as built to forgive a failed jump, which suits a friends server. Its route-colour language is easy to copy: teal = keep going, purple = jump, wood = safe ledge. It was MCC's most-played map from MCC 21 onward.
2. **Urban Jungle.** A square box and a small palette (red slab roofs, white plaster, brick, sky walls). Its one rule, cross the map on rooftops without touching the street, is easy to check. There is a good render plus 24 stills. Winter Town is close to it (N2 says "same chase geometry, snowed over", marked as inference), so a later seasonal version is cheap.
3. **Factory.** The official render plus two fan aerials. It is the first non-box MCC map and adds a new theme (industrial) to the set.

Plaza and Obelisk come later (medium evidence). Prefecture comes last: it needs an official flyover first (lead in `leads/pt-1.md`).

---

## 5. Build approach per map

### 5.1 Common approach (all maps)
- **Recreate from images. Import is not possible** (no worlds exist). Do not reuse any MG-PT-01 geometry.
- **Replace `ArenaBlueprint`'s random generator** with hand-authored map data: one file per map, read by the same `ArenaBuilder` so per-pairing copies still work. Follow the BattleBox `arena/Grid` + `tools/PreviewMain` pattern so a map can be dumped to `.mcmdump` and rendered **without a server**.
- A per-map record holds: footprint, the two cage positions per pairing, the spectator point and box, landmark list, and the void/respawn rule.
- **Cages:** two per arena on opposite sides, each a small room with a team-coloured glass door and a `Select Hunter:` sign. The door fill goes black → red → yellow → lime (the MG-PT-01 pattern) on `Starting in`.
- **Spectator deck:** a barrier roof over the arena (MG-PT-01 pattern), plus the existing `SpectatorGuard`.
- **Build agents get the frames listed in §4 for their map**, attached directly, not a text summary.

### 5.2 How routes, chokepoints and loops will match the reference
1. **Trace before building.** From the top-down or overview, draw a block-scale floorplan (`maps/<map>/plan.png` plus a layer text file). Scale comes from countable 1-block features: chains, panes, slabs and stair steps. Then mark on the plan:
   - the **loops** (closed runner circuits);
   - the **chokepoints** (gates, arches, single bridges);
   - the **landmarks**;
   - the **high lines**;
   - the **cage exits** (the first jump out of each cage, from the "round start" frames).

   The owner approves the plan before blocks are placed.
2. **Dustbowl targets** (from `022412`, `022416`, `dustbowl_wiki_render.png`):
   - a terracotta/dirt street floor;
   - the central blackstone gate building as the main chokepoint, with loops on both sides of it;
   - teal and purple block stacks as step chains to the low roofs;
   - wood ledges;
   - the orange kiosk;
   - the ice rink;
   - green wall arrows;
   - a stadium ring with the red brick pillars;
   - the `PARKOUR TAG` billboard;
   - ramps out of every street (no dead drops).
3. **Urban Jungle targets:**
   - a continuous red-roof line around the box;
   - about 10 chimneys as pivots;
   - striped awnings;
   - the black/red central spire as the one landmark;
   - streets as the slow, risky option;
   - the strong "top-left corner" (W trivia) kept, but checked so it is not unbeatable.
4. **Factory targets:**
   - a courtyard with at least 3 ground exits;
   - the crate → awning → sill → pipe → roof chains;
   - a rooftop catwalk ring;
   - the hazard arch;
   - clock faces;
   - loading bays that have visible exits.
5. **Automatic checks** (reuse `research/existing_maps/minigames/parkour_tag/data/standable.py`, `connectivity.py` and `paths.py` on the dump; `ParkourWarrior` `JumpSim` for jump lengths):
   - every standable area connects to both cages;
   - no dead end: every junction has 2 or more exits;
   - at least 3 independent loops;
   - every roof reachable from 2 directions (N1 §3);
   - no jump harder than the reference style allows (Dustbowl: forgiving);
   - list the chokepoints and compare them with the traced plan;
   - hunter-to-runner cage path length is similar for both sides (fairness).

### 5.3 How each map is checked
- **Renders side by side with the references:**
  - `PreviewMain` → `.mcmdump` → `tools/render_preview.py` (iso at each `--angle`, plus `--top`);
  - put each render next to the matching frame with `_candidates/tools/montage.py`;
  - top-down vs `022412`; iso vs the wiki render; cage view vs `022308`.

  The owner signs off the sheet.
- **Bot test** on the staging server (`tools/bot-harness`, port 25570): 6–8 mineflayer bots play a full 9-round game. Check:
  - cage → select → countdown → release;
  - tags register;
  - tagged runners spectate inside the box;
  - the round ends early on a full clear;
  - the hunt cap is enforced;
  - nobody escapes the arena or falls into the void;
  - no errors in the log.

  The bots move randomly, so they test flow, not jumps. The jumps are covered by the checks in §5.2.
- **Owner playtest** with friends, then a short `FEEDBACK.md`.

---

## 6. Open questions for the owner

1. Is the map order **Dustbowl → Urban Jungle → Factory** OK?
2. One map per game, as MCC does. Who picks it: random, admin command, or a vote?
3. MCC shows no hunter delay. Should we drop our 3 s hunter blind/freeze, or keep it for friends?
4. May an agent download two reference videos?
   - Zibly's Urban Jungle build video (14:27): close to a block-by-block plan.
   - The Noxcrew MCC 21 update video, for a Dustbowl flyover.
5. A true through-wall outline for Runner Vision needs packet-level code (NMS or a ProtocolLib dependency; neither is used now). Is that OK, or keep particles?
6. On the maps, copy the `PARKOUR TAG` / MCC signage as is, or rebrand it to Block Showdown?
7. Retire the random arena, or keep it only for solo practice?

---

## 7. Phases

Sizes: **S** is under one session. **M** is 1–2 sessions. **L** is 3 or more sessions.

| Phase | Work | Size | Done when |
|---|---|---|---|
| 0 | Owner answers §6 | S | Answers recorded here |
| 1 | **Runtime to MCC rules** (no new map yet):<br>• map registry and one-map-per-game;<br>• cages with door countdown;<br>• `Select Hunter` sign phase, random fallback, 4-hunt cap and chat counter;<br>• phase labels and overlays (`Tagged!`, `Hunted!`, `Survivor!`, `Round Over!`);<br>• Runner Vision recharge bar;<br>• hunter delay per answer 3.<br>Scoring hooks stay with the coin agent. | M | Bot flow test passes on the current arena |
| 2 | **Dustbowl trace:** floorplan from `022412` plus overviews, scale, palette, loops, chokepoints. Owner approves the plan | M | Approved `plan.png` and layer file |
| 3 | **Dustbowl build:** map data, cages, stadium and billboard, spectator deck; renders vs references; route checks | L (about 30–50k blocks per copy, estimated) | Owner signs off the render sheet; checks pass |
| 4 | **Dustbowl test:** bot run, owner playtest, fixes | M | Playtest notes closed |
| 5 | **Urban Jungle:** trace, build, check, test (faster with the Zibly video if allowed) | L | Same gates as phases 2–4 |
| 6 | **Factory:** trace from the render and fan aerials, build, check, test | L | Same gates |
| 7 | Later: Plaza, Obelisk, Winter Town (an Urban Jungle snow version), Prefecture once a flyover is found | L each | Same gates |
