# Build Mart — map layout research (current resource-island map)

**Agent:** map-layout researcher (Wave 1)
**Scope:** reconstruct the *current* Build Mart map topologically enough to build an original map:
overall footprint, central island, resource islands, build area, Blueprint Gallery, flow, dimensions,
sightlines. Research only — no source, plugin or world was edited.
**Deliverables:** this file, `data/dimensions.md`, `data/sections.csv`, `schematics/current.md`
(+ 4 rendered PNGs), `frames.md`, `REFERENCE_IMAGES.md`, and 21 stills in `frames/pack/`.

---

## 1. Method

- Read the shared brief, `game-plugins/BuildMart/README.md` and `BuildMartGame.java` first, to fix the
  baseline that must change.
- MCC-first sources: MC Championship Wiki (game + map pages), official Noxcrew showcase, then stills.
- **10 min 25 s of footage total**, in three windows:
  1. official showcase `zSvKYe9pl0M` (6:05) — used whole, because it is an official 6-minute feature,
     not a VOD; 13 stills;
  2. `bjMl2fLvMok` 02:40–05:40 (3 min) — wide establishing aerial + pavilion interior; 4 stills;
  3. free preview via YouTube storyboards (no video downloaded) — used only to locate timestamps.
- 21 full-resolution frames kept (`frames/pack`), 20–40 required.
- Dimensions derived by player-scale calibration (1.8-block player), cross-checked against two
  independent widths. Nothing is claimed as exact; every number is H/M/L-tagged.
- No full VOD downloaded, no third-party world, no browser session left open.

## 2. Sources

| # | source | type | used for | conf |
|---|---|---|---|---|
| S1 | MC Championship Wiki, *Big Sales at Build Mart* (`mcchampionship.wiki.gg`), rev. 2026-11-08 | wiki (T1) | rules, gallery composition, Gold timing, build-area contents, history | H |
| S2 | MC Championship Wiki, *Build Mart (Map)* | wiki (T1) | map identity, elytra/fan/pads, two areas | H |
| S3 | MC Championship Wiki, *Big Sales at Build Mart (Map)* (original map) | wiki (T3) | only to confirm the cart map is *not* current | H |
| S4 | Noxcrew, *MCC Pride 25 — Update Video: Big Build Mart Update!*, YouTube `zSvKYe9pl0M`, 6:05 | official footage (T1) | gallery, build area, Gold plot, shopping list, islands | H |
| S5 | Wolfeei, *MCC Pride 2025 Updates & Games — BUILD MART UPDATE!*, `bjMl2fLvMok`, 20:21 | coverage containing S4 + aerial | wide aerial, pavilion interior | M–H |
| S6 | Wiki image files: `Build-Mart-Revised-Blueprint.png`, `BSABM-BlueprintGallery.png`, `BSABM-BuildArea.png`, `BuildMartMap.png` | official renders (T1) | proportions, palette, block categories | H |
| S7 | r/MinecraftChampionship, *How to Prepare for Build Mart* (2022) | community guide (T2) | island order/colour clumping — pre-revamp | L |
| S8 | r/MinecraftChampionship, *I made a semi-accurate map of Build Mart* (2021) | community map (T2) | colour-island clumping only | L |
| S9 | `game-plugins/BuildMart/src/.../BuildMartGame.java` | current code (baseline) | what to keep / change | H |

**Map version note.** Two things are being conflated in casual discussion, and the rebuild needs both:
the **geometry** is the resource-island map introduced in MCC 14 (still current through 2026), while the
**gallery/plot loop** is the P25 "Blueprint Update" (June 2025). "Current Build Mart" = MCC 20+ map
(Copper replaces Coral, Garden gets azalea/moss) **plus** the P25 gallery. Pre-P25 there was no
selectable gallery: builds arrived in a fixed order and the reference was sent to the team area.

---

## 3. Findings

Each finding: **claim — source, timestamp/section — confidence.**

### 3.1 Macro structure

1. **The map has exactly two areas, joined by a portal**: the build area (team halls) and the mart
   (gallery + islands). The portal stands *in front of the plots*; players may fly inside the build
   area. — S1 §The Build Area; S2; S4 3:25 — **H**.
2. **The mart is a dome, not a room.** Yellow trusses + tinted glass panels overhead, red ring
   ornaments, open sky between islands, void below. No perimeter wall at player height. — S4 4:30
   (`09`), S6 `Build-Mart-Revised-Blueprint.png` — **H**.
3. **Radial organisation, three shelves:** central gallery disc (0) → ring of 8 colour pavilions
   (+2) → ring of 8 upper category islands (+26…+40). — S5 2:46 (`00`), S4 3:04 (`01`) — **H** for the
   three-shelf structure, **M** for counts of 8.
4. **The mart is ~300 blocks across** including the dome (disc ⌀ ~66, pavilion ring r ~76, upper ring
   r ~126, dome r ~148). — estimated from `00`/`01`, see `data/dimensions.md` — **M**.
5. **Nothing at the market edge has a rail or lip tall enough to stop a walk.** Falling is the
   designed failure state; you respawn at the build area and lose everything you carried except
   tools. — S1 §The Build Mart; S4 3:36 (`04` shows the open edge) — **H**.

### 3.2 Central island / Blueprint Gallery

6. **The Blueprint Gallery is on the central island, not a separate building.** Wiki: added "in the
   central hall"; footage shows the plinths standing on the blue disc, surrounded by the pavilion
   signs. — S1 §History (MCC P25); S4 3:04/5:06 — **H**.
7. **The disc floor is blue with white "circuit" lane lines and giant `MCC` block lettering.** —
   S5 2:50 (`15`), S4 5:06 (`13`) — **H**.
8. **Seven selectable blueprints + one Gold**: 2×1★, 2×2★, 1×3★, 1×4★, 1×5★, plus the Gold (7★).
   Rotation is 90 s; Gold first appears at 4:00, lives 120 s, and has **no reference in the team
   area**. — S1 §The Blueprint Gallery, §History — **H**.
9. **Plinths are small object-displays, not dioramas**: ~7×7 stone base, 1-block riser, build on top
   (typically 3–6 wide, 2–5 tall), name/stars/material list floating above. — S4 3:04, 3:12, 4:00
   (`01`,`02`,`05`), S6 `BSABM-BlueprintGallery.png` — **H** (size **M**).
10. **Nine plinth-sized pads are visible on the disc** but only 7 are selectable plus Gold; one pad
    reads as spare/rotation buffer. Do not build a 9th interactive plinth. — S4 3:04/5:06 — **M/L**.
11. **The giant fan sits on the disc, flush with the floor**, ⌀ ~20–24, wooden blades behind a grille.
    It is the vertical launch that makes elytra travel possible from a standing start. — S5 2:46
    (`00`), S1 §The Build Mart — **M** (existence **H**).
12. **Speed pads on the disc are red/orange spirals, 2×2, flush**, on the walking lines near the rim.
    — S5 2:50 (`15`) — **H**.
13. **Pavilion signs face inward and are readable from the opposite rim** (~60–70 blocks). One
    oversized word per pavilion, colour-on-colour. — S4 5:06 (`13`), 3:12 (`02`) — **H** for the
    system, **M** for the distance.
14. **Selection is at the plinth**, by stepping on / interacting with a plate at the plinth foot
    (≤2 blocks). The exact verb (right-click vs stand) is not resolvable from footage. — S4 3:04–4:00
    — **L**.

### 3.3 Resource islands

15. **Lower islands are colour pavilions — walk-in showrooms, not piles.** Each has a dark floor, a
    full-height charcoal back wall with MCC banners, glass display cabinets on steel frames, open bins,
    a hanging colour sign, and a launch pad on the roof. — S5 2:52 (`16`), S4 5:06 (`13`) — **H**.
16. **Eight pavilions, two colour families each**: RED·ORANGE, YELLOW·LIME, GREEN, AQUA·CYAN, BLUE,
    PURPLE·PINK, WHITE·GREY, BLACK·BROWN. Sign words are singular (`ORANGE`, `LIME`, `BLACK`), so the
    bins inside are per-colour. — S4 5:06, 3:12, 5:02; S7 (clumping) — **M**.
17. **Upper islands are thick chunks, not platforms**: grass/dirt lip, stone underside with
    stalactites, 18–26 blocks thick, 40–52 across, with 1–3-high block piles and decorative animals
    (sheep on logs, camels on sand). — S4 4:30 (`09`), S6 — **H**.
18. **The upper set is eight categories:** LOGS, SAND (→SNOW in MCC 19), STONE, ORES, NETHER, BRICKS,
    COPPER (replaced CORAL in MCC 20), GARDEN (azalea/moss added in MCC 20). — S1 §History, S6
    `BuildMartMap.png` — **H**.
19. **Upper island signs hang from the dome trusses**, not from the island, so they read against the
    sky from the disc. — S4 4:30 (`09`) — **H**.
20. **Access to upper islands is elytra-only**; there are no bridges or stairs. Elytra are equipped
    automatically; launch pads exist "throughout the mart to ease traversal". — S1, S2 — **H**.

### 3.4 Team build area

21. **Three regular plots in a row + one raised Gold plot.** Plot module ≈15×15 outer curb (1 high,
    white quartz with **black corner blocks**), a 1-block apron, and a 7×7 inset build zone marked by a
    blue/grey checker. — S4 3:36/4:36 (`04`,`10`), S6 `BSABM-BuildArea.png` — **M/H**.
22. **The plot is the UI**: name, stars and material list float on the floor at the plot; a second
    hologram reports slot state (`Empty Plot #1/#2/#3`) and the hall carries both a game clock and
    `GOLDEN BUILD IN: mm:ss`. — S4 4:26 (`08`), 3:36 — **H**.
23. **Utilities are on the wall behind the plots**: five colour-coded chests on a counter (labelled
    with colour names), crafting tables, stonecutters, and **two smelting banks** of ~8 lava-fed
    furnaces with fuel provided. — S1 §The Build Area; S4 3:36 (`04`) — **H**, counts **M**.
24. **The team portal is a freestanding prop** (four emerald columns + chains around a blue portal
    bed), in front of the plots, not a doorway in a wall. — S4 3:25 (`03`) — **H**.
25. **The Gold plot is architecturally different**: raised +2 on a yellow terracotta dais (~9×9), in
    its own alcove under a yellow/black hazard-striped canopy, with green spiral speed pads at its
    foot. No reference build; the only view is in the gallery. — S4 4:12/4:16 (`06`,`07`), S1 — **H**.
26. **Flight is allowed in the build area**; there is no fall risk there. — S1 — **H**.

### 3.5 Flow, pacing, scoring

27. **Loop:** spawn in the hall → portal → disc → pick 1–3 blueprints (plinth) → fan + elytra →
   pavilions/islands → return through portal → build → repeat. One reference copy arrives per
   selected build; a completed plot frees a slot. — S1 §Introduction; S4 3:25–4:26 — **H**.
28. **Falling**: respawn at the build area, *inventory wiped except tools* — the material loss is the
   punishment, not the respawn. — S1; confirmed by the current code's `voidCheck` behaviour (S9) —
   **H**.
29. **Timing:** 12-minute game; gallery restock 90 s; Gold from 4:00, 120 s life; 3 builds in flight;
   ~14–17 builds finished by the best teams per event ⇒ **≈45 s per build average**. — S1
   §Appearances/History — **H**.
30. **Scoring (current):** 20 coins per star (flattened at MCC FS); partial builds pay by percentage;
    Employee of the Month bonuses 100/50/25 for most stars, most builds, highest average stars. — S1
   §Coin Rewards, §History — **H**.
31. **Shopping list book:** 3 pages, item icon + count per row, orange `Done` button; the same list is
   on the plot floor. — S4 5:02 (`12`) — **H**.

### 3.6 What is *not* verified

32. **Spectator positions**: no spectator stand appears in any of the 10 minutes used. The current
   code lets spectators fly. Treat the real stand as unverified (see Open questions).
33. **Exact island bearings / clockwise order**: the aerial is oblique and partly occluded; community
   maps disagree with each other. Order is a navigation aid, not a fact.
34. **Interaction radius at a plinth**, **fan exit height**, **elytra launch geometry**, **exact
   furnace counts**: all **L**.

---

## 4. Current code vs the real map — keep / change

Baseline: `BuildMartGame` builds a void world, a 51×27 mart floor with 27 raw stalls and 4 shop
counters, and 8 team platforms on a ring of radius 48, each 17×11 with a 5×5 plot and a pedestal.

### Keep (good bones)

| keep | why |
|---|---|
| One void world, code-built, wiped each start | fast iteration, no world files |
| Team→area mapping and per-area state (`Area`, `seqIndex`, `completed`) | clean model for 3 plots |
| Stall abstraction (`Stall`, `Regen`) | becomes island bins; same regrow idea |
| Shop counters (`Shop`, `Cost`) | the real game has crafting/furnaces instead, but the *conversion* concept is right |
| Plot evaluation by exact block match + wrong count | matches MCC strictness (MCC AS requires rotation; we can start looser) |
| Void check that deletes inventory | exactly the real failure state |
| Sidebar / action bar / titles | cheap feedback layer |
| 10 s preview then timed play | MCC-style |

### Change (must)

| change | from → to |
|---|---|
| **Topology** | ring of 8 identical team platforms around a 27-stall mart → **one shared mart** (disc + 8 pavilions + 8 islands) and **separate team halls** reached by portal. Teams must *compete for the same blocks*, not farm their own copy. |
| **Build loop** | 1 build at a time, fixed order → **3 concurrent plots + Gold plot, player-selected** from a gallery. |
| **Reference** | pedestal beside the plot, 5×5 → **reference copy on/next to the plot**, build up to 9×9, Gold has none. |
| **Gallery** | none → **7 rotating plinths (2/2/1/1/1 stars) + Gold**, 90 s / 120 s.
| **Travel** | walk/fly on a flat floor → **fan vertical launch + elytra + roof launch pads + deck speed pads**, with real fall risk. |
| **Materials** | 27 flat stalls, 4 shop counters → **8 colour pavilions + 8 category islands** with the MCC block families, plus crafting tables / stonecutters / 2 furnace banks in the hall. |
| **Plots** | 5×5 → **15×15 curb / 7×7–9×9 build zone**; three in a row; black corner blocks. |
| **Chests** | none → **5 colour-coded chests**. |
| **Scoring** | 4+3×stars, speed + order bonus → **stars × 20**, partial %, Employee-of-the-Month bonuses. |
| **Map identity** | flat checkered floor, lamp posts, text labels floating over stalls → **dome, blue circuit disc, block-letter signs, MCC banners, tinted glass**. |
| **Spectators** | fly-around → dedicated stand/box (see open question). |

### Do **not** copy

- The exact 8-fold colour order, the `MCC` lettering, the exact build list, the dome ornament design,
  the pavilion sign typeface, or the plot naming (`Two`, `Cover`, `Alternator`). These are Noxcrew's
  identity. Rebuild the *system* with our own names, palette and ornaments.

---

## 5. Design implications for our original map

1. **The map is a clock, not a floor plan.** The player's mental model is "which shelf, which
   direction". If our map is a rectangle with islands at the corners, travel becomes pathing; a radial
   plan makes it navigation. Keep the radial idea even at reduced scale.
2. **Two shelves are enough.** A disc + one ring of combined colour/category islands (8 total) gives
   the same loop at ~45 % of the block count: walk out, gather, elytra/launch back. The second
   (upper) shelf is what makes the real map feel huge, but it doubles the build.
3. **The gallery must be visible from the gather space.** In MCC the plinths are on the same disc you
   land on. If our gallery is a separate room, players will not re-check it and the 90 s rotation is
   invisible.
4. **Gold needs a different silhouette, not a different sign.** The hazard canopy + raised yellow dais
   is readable from the portal. A gold-tinted plot alone is not.
5. **The plot IS the interface.** Floor holograms (name/stars/materials/slot state) do more for
   legibility than any GUI, and they survive at a distance.
6. **Never rail the market.** The fall is the tension. But do rail the *approach* to the portal and
   the plot curbs, so the only lethal edge is the one the player chose to cross.
7. **Pacing math drives scale.** At 45 s/build, a gather trip must be ≤30 s. If our islands are too
   far, the game becomes travel. Measure the round trip in seconds, not blocks, and tune.
8. **Colour is a wayfinding system, not decoration.** Every pavilion should be identifiable by a
   single saturated colour at 60 blocks, before the sign is readable.

---

## 6. Open questions

| # | question | why it matters | how to close |
|---|---|---|---|
| Q1 | Is the spectator area a stand above the build hall, or do spectators fly? | affects hall height and camera | one 2-min admin-POV window at the start of a Build Mart |
| Q2 | Exact selection verb and radius at a plinth | determines interaction code | one POV clip of a player selecting |
| Q3 | Are there 8 or 9 lower pavilions / upper islands? | map symmetry and block budget | one clean top-down from an admin stream |
| Q4 | Fan exit height and elytra boost vector | feel of the launch | one unedited POV of a fan launch |
| Q5 | Do builds exceed 7×7? | plot size and preset limit | check a 5★ build in the gallery |
| Q6 | Are the 5 chests fixed colours or team-chosen? | UI and inventory rules | close-up of the chest labels |
| Q7 | Partial-credit formula and per-star value in the current event | scoring fidelity | official rules page / one results screen |
| Q8 | Does a failed Gold Build delete the blocks at timeout, and when? | Gold implementation | one Gold fail clip |

---

## 7. Implementation checklist (testable)

- [ ] Mart is one shared space; two teams can mine the same island bin and race for the last block.
- [ ] Build area is reached only by the portal; the portal is visible from every plot.
- [ ] A player can leave spawn and be standing on the disc in ≤5 s.
- [ ] Fan launches a standing player to ≥+30 and an elytra deploy at apex reaches the farthest island in ≤8 s.
- [ ] Every pavilion is identifiable by colour alone at 60 blocks, and by sign at 40.
- [ ] Falling anywhere in the mart respawns at the hall and deletes all non-tool items, every time.
- [ ] Gallery shows 7 blueprints with the 2/2/1/1/1 star spread and rotates on a 90 s clock that is visible in-world.
- [ ] Gold appears at 4:00, is 7★, has no reference in the hall, and expires at 120 s with block removal on failure.
- [ ] A team can hold 3 selected builds and sees which plot each belongs to without opening a menu.
- [ ] Plot is 15×15 with black corner blocks; a 9×9 build fits with a walkable apron.
- [ ] Gold plot is raised, hazard-marked, and cannot be confused with a normal plot in a screenshot.
- [ ] 5 colour-coded chests, 2 crafting tables, ≥1 stonecutter, 2 furnace banks with fuel, all reachable in ≤4 s from any plot.
- [ ] Shopping list book paginates, counts match the plot hologram, and updates when a build completes.
- [ ] Spectators have a fixed viewpoint that sees all 4 plots and the portal.
- [ ] A full gather→build cycle measures 25–45 s in playtest, and 12 minutes yields 10–16 builds.

---

## 8. Layout principles for our original map

1. **One shared mart, many private halls.** The competition is over blocks; give every team its own
   safe place to build, but exactly one place to shop.
2. **Make the centre a decision, not a corridor.** The middle of the map should be the gallery — the
   thing players must return to — not a spawn junction they cross once.
3. **Build the map as three altitudes:** safe decision floor (0), near showrooms (+2), far quarries
   (+26 and up). Each altitude has one job.
4. **Put the smallest object at the centre and the biggest at the rim.** Plinths at the middle,
   islands at the edge: it gives a readable silhouette from every angle and a natural difficulty ramp.
5. **One colour = one place.** Never split a colour family across two islands; never put two families
   under one sign unless the sign says both.
6. **Signs are architecture.** Oversized block letters on colour fields, hung from the structure, sized
   to be read at 60 blocks. No floating name tags for the map's main landmarks.
7. **The gallery is a shop window.** Each blueprint gets a plinth, a riser, a light, and a floating
   name/star/material card at head height — no chests, no GUI walls.
8. **Rotate on a visible clock.** 90 s for the seven, 120 s for Gold, with an in-world countdown on the
   disc; if the player cannot see the clock, the rotation does not exist.
9. **Gold is a place, not a build.** Give it a raised dais, hazard markings, and its own alcove so it
   reads at a glance and from the portal.
10. **Build plots are stages.** 15×15 curb, 7×7–9×9 stage, black corners, an apron for overspill, and
    the build's identity written on the floor.
11. **The return trip must be the easy one.** Outbound is the challenge (fan, aim, glide); inbound gets
    roof pads and a portal mouth you can hit from the air.
12. **No rails at the market edge; rails everywhere else.** The only fall the player can take is the
    one they chose; plots, portal, and stairs are safe.
13. **A fall costs cargo, never time.** Respawn close, keep tools, delete the load. Punish greed, not
    the player.
14. **Colour-code the storage, not the players.** Five chests labelled by colour family, aligned with
    the island signs, so sorting is spatial memory rather than reading.
15. **Utilities live on the wall, plots live in the open.** Furnaces, tables and chests behind; plots
    with clear sightlines between them so four players can talk without moving.
16. **Design for the 45-second build.** Every leg — portal, plinth, fan, island, return — must be
    measurable in seconds; if a leg exceeds 10 s, shorten the map, not the timer.
17. **Two shelves beat three at our scale.** Disc + combined colour/category ring (8 islands) keeps the
    loop intact at roughly half the blocks; add the upper shelf only if the first one playtests fast.
18. **The dome is the skybox.** A truss ring, tinted glass and a coloured ornament give depth without
    terrain; the void does the rest. Do not build an enclosing wall.
19. **Every landmark must survive a screenshot.** If a player cannot point at "the gold plot", "the
    black pavilion" and "the fan" in a single frame, the silhouette is too busy.
20. **Original surface, MCC skeleton.** Copy the *relationships* (centre ↔ ring ↔ altitude ↔ portal),
    never the ornaments, names, lettering or exact order.

---

## 9. Files

```
layout/
  README.md                  this dossier
  REFERENCE_IMAGES.md        image pack guide (start here to build)
  frames.md                  frame index + calibration
  frames/pack/               21 full-res stills
  data/dimensions.md         every number, with kind + confidence
  data/sections.csv          machine-readable section table
  schematics/current.md      plan + elevation write-up
  schematics/*.png           top-down, gallery plan, build-area plan, section
  scripts/make_schematics.py reproducible drawings
```
