# Nocturne Orrery — build report

**Status:** first full pass built in the core hub; core compiles and installs; offline geometry, staircase and sightline checks pass. Runtime v2 methods are implemented but have not yet been exercised on a staging server.

**Files changed**

- `plugin-src/com/mcm/lobby/HubVenue.java` — Nocturne Orrery exterior and interior (dial, podium, 10 pods, concourse, grand stair, stands, dividers, armoury reveal, clock, planetarium, portal, marquee, ribbed dome, armillary topper).
- `plugin-src/com/mcm/lobby/HubLayout.java` — v2 layout data: dial capacity/radius, 8/6 wedge centres, label anchors, repaintable dial band, wedge accents, pod spawns/gates/bars, divider teeth and seams, reveal surfaces, clock.
- `plugin-src/com/mcm/lobby/HubManager.java` — `McmLobby.Venue` v2 implementation (dial/pods/dividers/reveal/runtime) plus v1 aliases; startup restores the venue into its flush, normal state.
- `plugin-src/com/mcm/lobby/HubGenerator.java` — generator version bumped to **4** so an existing world rebuilds.

No `DecisionDome` sources, API signatures, worlds, `plugins/`, or the live review server were touched.

---

## 1. Block budget and key dimensions

| Item | Value |
|---|---:|
| Whole hub (`mcc_lobby`) | **6,773,009 blocks** |
| Venue region (drum + terrace + interior, x/z ±62, y 50–200) | **451,515 blocks** |
| Budget | 275k–485k → within range; first pass near the upper half |
| Drum outer / inner radius | 48.5 / 45.5 |
| Terrace / entrance | y 76 (r 56.5) |
| Dial floor / pod floor | y 64 / y 65 (walking surface 66) |
| Dial radius | 12.0 |
| Wedges | 8 × 45°, boundaries 22.5° + k·45°; 6 × 60°, boundaries 30° + k·60°; wedge 1 centred on 90° |
| Podium | r 7.5 base at y 65, top r 5.8 at y 66, standing y 67 |
| Dividers | 8 sets × 51 above-floor cells; 6 sets × 51; 3 high when raised with crenellated top |
| Pods | 10, r 13.4–19.4, angles 18/54/72/108/126/162/198/234/270/306; 4 seats each (40) |
| Concourse | r 19.6–23.4, y 64, brass rail at r 23.4 |
| Grand stair | 7 wide, y 64 r 26 → y 76 r 39, 12 rises / 13 run |
| Stands | r 24–35, 12 rows, y 67–78, **120 spots** (12 × 10) |
| Inner wall | r 37, y 79–102; soffit ring to the drum at y 103 |
| Reveal | open 7×7×7 copper armillary cage, y 73–79; four surfaces at r 8.5, y 76; rings r 3.2/3.5/3.8; gold planet |
| Interior clock | brass ring y 81–83; text anchor y 84 |
| Glass dome | 12 meridian ribs, 4 latitude rings, oculus; apex y 146 |
| Topper | brass armillary, three rings + gold planet, y 150–170 |
| Marquee | 57 wide × 22 high, x −28..28, y 105–126; cream `BLOCK SHOWDOWN`, copper orrery emblem |
| Facade clock | 25 × 9 block digits, y 92–100, updated by `showCountdown` |
| Entrance | 13-wide portal, 9-wide door frame, red/gold carpet, terrace preserved |

**Palette hierarchy (after the reference pass):** the dial is now a matte black plate with cream chevrons and brass rings; team colour is concentrated in a thin outer jewel rim, the inner light strip, the 8 low totems and the 10 glass pod suites. This matches the reference stills' 60/25/15 read much better than the first pass, where the whole wedge floor was coloured. The room is dark basalt/deepslate/blackstone, the wall/ceiling cream and copper, the accent gold/oxidized copper/teal/team jewel glass.

No flat single-material run >6 blocks on the architectural surfaces: the drum has 10° seams and copper ribs every 15°, the interior wall has 3° flutes and team banners every 36°, the stands use tier nosings/landing coins, and the marquee is the only large flat field (a graphic sign).

---

## 2. Core API v2

`HubManager` still implements `McmLobby.Venue`; `HubVenue` builds the geometry and `HubManager` owns the runtime blocks.

| v2 method | Implementation |
|---|---|
| `apiVersion()` | `2` |
| `dialCapacity()` | live 8 or 6 |
| `setDial(int)` | clamps to 6/8; swaps the active 8/6 totem set, repaints the jewel rim into 45°/60° sectors, raises the matching divider set, paints the matching seam |
| `wedgeCenter(i)` / `wedgeRadius()` | r 8.5 at the live wedge angle / dial r 12.0 |
| `setWedge(i, color, lit)` | repaints the active totem and the wedge's rim/light cells; `null` makes the wedge dark/blank |
| `podCount()` / `podSpawn(i)` | 10 / pod spawn at r 14.2 on the window side, facing the dial |
| `setPodGate(i, open)` | clears or closes the 3-wide rear gate |
| `setPodState(i, state)` | normal / caged / dunked / frozen / invaded: glass, carpet, crest, lamp, bars and gate |
| `setDividers(0..1)` | raises/lowers the active 8 or 6 comb in three layers; inactive set is lowered |
| `clearRuntime()` | lowers dividers, opens gates, restores pods, repaints the dial, removes vote text |
| `revealWedge(i, gameId)` | raises dividers, blanks every other wedge, gilds the winner, paints four reveal titles at `revealSurface(0..3)` |
| `dialGeneration()` | generator version 4 |
| `revealSurface(face)` | four face anchors at r 8.5, y 76 |
| `interiorClock()` | y 84 orrery-clock anchor |

**v1 aliases:** `padCount()` → `dialCapacity()`, `padCenter(i)` → `wedgeCenter(i)`, `padRadius()` → 2.0, `setPad()` → `setWedge()`, `resetPads()` → restore the active wedge set. `revealPodium`, `interiorTimer`, `countdownOrigin`, `showCountdown`, `standSpots` keep their old contracts. `finishReady()` calls `setDial(8)`, normalises pods and lowers the showcase dividers, so a world built with the dividers raised in the preview starts flush.

**Label anchors:** the core exposes `wedgeLabels8[8]` / `wedgeLabels6[6]` in `HubLayout` (x, y 74.5, z, yaw), but the public `McmLobby.Venue` API has no wedge-label method. The plugin should place its game-name `TextDisplay`s at `wedgeCenter(i)` + ~9 blocks, or the core can expose the anchors in a later API addition.

---

## 3. Reference comparison (opened directly)

The references are official MCC wiki stills and extracted frames, not reconstructions. I did not trace any of them block-for-block; the comparison is proportions, flow, palette hierarchy and staging beats.

All `...` paths below are relative to `game-plugins/research/dome/build_look/`.

| Reference image | What it actually shows | What I changed / kept |
|---|---|---|
| `game-plugins/research/dome/build_look/images/DecisionDomeS3.png` | Current 8-game interior from above: ten large tinted-glass team suites around a black/white/colour chevron dial; chunky game totems at the wedge ends; black crenellated dividers; a huge gold/white truss/chandelier over the centre; dark carved wall with red sponsor boxes and cream verticals. | **Changed:** the dial became black/cream chevrons with a thin team rim and low totems instead of a solid coloured pie. **Kept/adapted:** ten glass suites, black comb dividers, a central hero object (our brass armillary instead of the crown chandelier), dark bowl + cream/copper wall. |
| `.../images/DecisionDomeS2.png` | Close 3/4 view: stepped tinted-glass pods (2–3 rows), solid red wedge floor, black radial dividers, hanging `?` banners and game cubes. | Confirms the pod is a **room with stepped rows**, not a pad. Our pods now have a low rear step and four seats; the `?` banners are replaced by the orrery holographic game names. |
| `.../images/DecisionDomeS1.png` | Season 1: hanging game banners with icons over a dark dial with white wedges; blue/cyan glass booths at the rim. | Confirms the **hanging-label / readable-icon** staging and the dark-bowl/white-wedge contrast. We use a physical low totem plus a floating TextDisplay, not literal hanging banners. |
| `.../images/DecisionDomeTR.png` | The 6-game variant in the same room: fewer, wider wedges and fewer game stations; the same ten-team pod ring. | Our `setDial(6)` keeps the room and pods, switches to six 60° wedges/totems/dividers/seams and repaints the rim. |
| `.../images/GR-DecisionDome.png` | Close-up of the dial and pods: **black floor with white chevron ribbons**, 3-high black comb dividers with crenellated tops, stepped glass pod rows, gold-topped game totems, a side elevator door. | This drove the final pass: black/cream chevron dial, 3-high crenellated dividers, stepped pod rows, low gold-capped totems. Our elevator/lore door is at the 270° service wall. |
| `.../images/Hub-Festive-S1.png` | Clean exterior: red drum → pale-blue glass dome with gold ribs/crown → bow topper; black countdown panel; gold/white portal; bridge/carpet approach; surrounding festival island. | **Massing and facade composition match** (drum → ribbed glass dome → distinctive topper; countdown + logo + portal + carpet). The palette is deliberately the Nocturne Orrery basalt/copper/teal, not the red/gold MCC livery. Our topper is an armillary, not a crown/bow. |
| `.../frames/cfs4qlMmGcw/f_0024_s3_dial_from_pod.jpg` | Pod POV through tinted glass: black/white chevron dial, game cubes/totems, central coin-modifier truss, dark wall and sponsor boxes. | Confirms the **pod POV silhouette**: team glass foreground, dark bowl, lit dial, hero object above. Our pod spawn is at the window (r 14.2) so the throw/view line is clear. |
| `.../frames/cfs4qlMmGcw/f_0200_s3_interior_wide.jpg` | Wide bowl: ten large stepped glass suites, black/white/colour dial, central copper ring/chandelier, dark carved wall with white/red panels, upper audience tiers. | Confirms the **scale of the pod ring and the bowl**. Our pods are tighter (r 13.4–19.4) by schematic; the bowl/wall depth and tiered stands are kept. |
| `.../frames/cfs4qlMmGcw/f_0344_s3_interior_hero.jpg` | High view over the pods and dial: stepped glass pods, totems, gold chandelier, stands with team-coloured audience blocks. | Confirms the **three-tier stands**, the pod-as-suite read and the hero ceiling object. Our stands are darker and deliberately less confetti; team colour stays in pods/banners. |
| `.../frames/cfs4qlMmGcw/f_0356_s3_interior_floor.jpg` | Floor/high-pod view: stepped glass tiers, black/white chevron floor, black comb dividers, players/chickens at floor level. | Confirms divider height/pitch and the chevron grain. Our divider is three cells high with a one-in-three tooth gap; the floor has cream chevrons on black. |
| `.../frames/cfs4qlMmGcw/f_0264_mcc6_dividers.jpg` | 6-game close-up: white/black chevron floor, green glass-topped comb dividers, chickens/players, the `Counting chickens...` tally beat. | Confirms the **6-game** geometry uses wider wedges in the same room. Our `setDial(6)` builds a 60° divider/seam/totem set and repaints the rim; the tally HUD is the plugin's job. |
| `.../frames/cfs4qlMmGcw/f_0272_hub_night_exterior.jpg` | Night exterior: lit drum with big `50:00` countdown, gold-ribbed dome/topper, balloons, dark sky and surrounding attractions. | Confirms the **night show and countdown-as-appointment**. Our facade clock and black marquee reproduce that composition; the exterior is basalt/copper/teal with an armillary topper. |
| `.../frames/obQz8VyqfTU/f_0045_dome_update_exterior.jpg`, `f_0080_dome_update_approach.jpg`, `f_0120_dome_update_interior.jpg` | The official dome-update video is a hub/market/backstage tour rather than a clean dome fly-through; it shows the event as a **place** (market street, giant logo wall, production monitors), not a dome section. | Used as context for the hub-as-place and event-brand scale, not as dome geometry. Our plaza/approach/terrace are preserved. |

**Deliberate differences from the reference:** the current MCC dome's crown/chandelier, red sponsor boxes, `?` banners and giant gold columns are not copied. The Nocturne Orrery uses a brass armillary reveal, a planetarium wall, basalt/copper/cream, and team jewel glass. The reference totems are taller/chunkier than ours; I chose low totems + floating game names so every pod keeps a clear label line, and recorded that as an open issue below.

---

## 4. Validation

### Build

```
cd plugin-build
$env:JAVA_HOME='C:\Users\LeonS\Desktop\java'
$env:Path='C:\Users\LeonS\Desktop\java\bin;' + $env:Path
C:\Users\LeonS\Desktop\java-tools\maven\bin\mvn.cmd -B install
```

**BUILD SUCCESS** — Java 21, 34 core sources, `plugin-build/target/MinecraftMondayCore.jar` installed.

### Offline hub validator (`tools/lobby_preview/HubValidate`)

- 89 staircase columns walked
- dome entrance 31/31, grand stair 7/7, all six dome aisles 1/1
- connection mismatches 0, floating carpets 0, staircase failures 0
- **VALIDATION PASSED**
- 72 informational loose bottom stairs are the harbour's dark-oak stairs, not the dome.

### Offline Orrery check (`OrreryCheck`, temporary harness)

- 10 pods; 10 spawns; 10 gates; 40 pod seats (4 per pod)
- exactly 120 stand spots
- 8 wedge accent sets (20 cells each) + 6 wedge accent sets (20 cells each)
- 8 + 6 divider tooth sets (51 cells each); seams tracked per mode
- 96 repaintable dial cells (thin rim + light strip)
- **40/40 sightline checks pass** with dividers ignored (they are lowered while voting):
  - each pod sees ≥2/5 sampled points in its own wedge (the low totem itself occupies the exact wedge centre),
  - 8/8 label holograms from every pod,
  - the nearest reveal face,
  - its own label.
- entrance/carpet/door/stair endpoints preserved.

Block budget: venue **451,515**; whole hub **6,773,009**.

**Caveat:** the sightline check is geometric and uses the block dump; it treats glass/panes as transparent and ignores divider teeth. It does not prove the in-game camera/culling result.

---

## 5. Renders

All under `game-plugins/DecisionDome/previews/nocturne_orrery/`:

| File | View | Notes |
|---|---|---|
| `top.png` | top-down interior/dial | 105×35×105 dump, orthographic top; dividers shown raised (showcase state) |
| `dial_plan.png` | close plan of dial/pods | 33×14×33 dump, scale 12 |
| `six_dial_plan.png` | 6-game dial proof | offline swap of the same layout: 8 totems hidden, 6 placed, 6 seams/dividers raised; validates the 60° set |
| `interior.png` | cutaway half-bowl | angle 1; shows dial, pods, grand stair, stands, wall, orrery |
| `closeup.png` | dial + pods + reveal | angle 1; best read of the orrery and totems |
| `exterior.png` | exterior 3/4 | angle 0, step 2; drum/glass dome/armillary topper, marquee, plaza |
| `facade.png` | facade close-up | marquee/clock/portal detail |
| `reference_comparison.png` | direct side-by-side sheet | reference S3 vs top plan, S3 wide vs interior cutaway, GR close-up vs dial/pods close-up, 6-game dividers vs 6-game proof, Hub-Festive S1 vs exterior |

The contact sheet is the direct visual check: it pairs the official stills with the build renders at equal panel size, so the dial hierarchy, pod scale, divider pitch and exterior massing can be judged without relying on the written notes.

Render notes: these are block dumps from the same generator the server uses, rendered with `tools/render_preview.py`; no hub world was copied into Git. The previews show the **built showcase state**, so the eight black comb dividers are up and the 6-game set is hidden. `finishReady()` lowers the dividers and re-applies the normal pod state at runtime. Game names and the `NOCTURNE ORRERY` sign are `TextDisplay` entities, so they do not appear in the block renders; the physical totems, label anchors and per-wedge colour are present. The facade clock is drawn idle (`--:--`); `showCountdown` fills it at runtime.

---

## 6. Open issues / next pass

1. **Runtime smoke test.** `setDial`, `setWedge`, `setDividers`, `setPodState`, `setPodGate` and `revealWedge` compile and are logically checked, but block writes need one staging-server run (`/mcm lobby rebuild`, then `/dome test hub` fast-test). Do not use the live review server on port 25580 for this.
2. **Pod roofs vs the schematic's `≤66`.** The pod floor is y 65 and the room is four blocks high; the roof is a glass/copper canopy at y 70, with the opaque front fascia at y 66–67. A literal solid roof at y 66 would make the room one block high. The glass roof is transparent and the sightline check passes through it. If the team wants a hard roof line, the pod floor must drop below the dial floor or the room must become 2-high.
3. **Label height.** The physical totem top is y 67; the game-name holograms are anchored at y 74.5. The schematic suggests label tops y 70–71. Lowering them to 70–71 re-introduced totem occlusion of the far labels in the first pass; the current anchor keeps 8/8 labels clear from every pod. This is the main deliberate visual deviation from the reference's taller, chunkier totems.
4. **Public label API.** `McmLobby.Venue` has no `wedgeLabel()` method. The core has anchors in `HubLayout`; the plugin needs either a fixed offset from `wedgeCenter(i)` (roughly +9 y) or a small API addition later.
5. **6-game visual pass.** The 6-game data/runtime is implemented and `six_dial_plan.png` proves the 60° totem/divider/seam swap offline, but no live 6-game interior render was produced. Run `/dome test hub` with six options and capture a top/interior pass to check the 60° totems, dividers and rim in-game.
6. **Pod size.** The reference suites are deeper/larger; our ring is 6 blocks deep (schematised r 13–19) and the hero pods at 72/108 are narrow. The rear step improves the tiered read, but 3–4 rows would need a deeper ring and a wider concourse.
7. **Exterior occlusion.** From some north/east angles the existing ridge and trees crowd the drum; terrain edits were out of scope. From the plaza approach the drum → dome → topper silhouette and countdown read correctly.
8. **Block budget.** 451.5k is inside 275–485k but near the top; if the architecture needs to shrink, the biggest safe reductions are the foundation fill and the hollow-wall backing, not the dial/pods.
9. **Marquee clock preview.** The facade render shows the idle `--:--`; a second facade render with a live `MM:SS` would better demonstrate the 25×9 digits.

---

## 7. Source map

- `plugin-src/com/mcm/lobby/HubVenue.java` — `build()`, `drumAndFacade()`, `interior()` (dial, `podiumIsland`, `pods`, `concourse`, `stands`, `grandStair`, `aisles`, `innerWallShell`, `dividers`, `orrery`, `interiorClockRing`, `planetarium`, `tunnel`), `glassDome()`, `topper()`, `gatehouse()`.
- `plugin-src/com/mcm/lobby/HubLayout.java` — v2 data.
- `plugin-src/com/mcm/lobby/HubManager.java` — v2 runtime + v1 aliases.
- `plugin-src/com/mcm/lobby/HubGenerator.java` — version 4 build marker.
