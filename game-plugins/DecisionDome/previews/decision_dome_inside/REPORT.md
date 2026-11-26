# Decision Dome interior merge — build report

Status: **geometry complete, built and validated offline. Not deployed, not committed.**
Generator: `HubGenerator.VERSION = 5`. World rebuilds on next start (the build marker invalidates v4).

## 1. What changed

| File | Change |
|---|---|
| `plugin-src/com/mcm/lobby/HubVenue.java` | Interior composition rebuilt: dial, podium, pod towers, concourse, stand accents, inner-wall band, crown, fill ring, lighting, reveal/timer anchors. Exterior code untouched. |
| `plugin-src/com/mcm/lobby/HubGenerator.java` | `VERSION` 4 → 5 and a v5 changelog line. |
| `plugin-src/com/mcm/lobby/HubLayout.java` | **Not modified.** No new anchors were needed. |
| `plugin-src/com/mcm/lobby/HubManager.java` | **Not modified.** Every existing field and method it reads stays valid. |
| `game-plugins/DecisionDome/src/main/java/com/mcm/games/decisiondome/HubDialView.java` | **Not modified.** |

Untouched: foundation, terrace ring, drum, facade, gatehouse (clock/marquee/tunnel), glass dome, armillary topper, pylons, rim flags, stands walk geometry, aisles, grand stair, service door.

## 2. Coordinates (world)

Centre `CX, CZ = 0, -62`; `STAGE = 64` (dial top y65); `POD_FLOOR = 65`; `TERRACE = 76`;
`DIAL_R = 12`; pods `POD_IN 13.4 … POD_OUT 19.4`, angles `{18,54,72,108,126,162,198,234,270,306}`.

### Dial (replaces the black orrery plate)
- Plate at `y=64` (walk surface 65), radius 12: `smooth_quartz`/`quartz_block`/`calcite`/`quartz_bricks`.
- Eight 1–2 block `gray_concrete` radial dividers on the 22.5°+45°k boundaries; a 5-cell `gray_concrete` diamond in each wedge at r 6.5; `light_gray_concrete` rim lip at r 11.15–11.45; `gray_concrete` rim r 11.45–12.
- Repaintable band unchanged: outer team ring r 10.45–11.45 (`dialBand`, `K_CONCRETE`), inner light strip r 8.02–8.34 (`K_LIGHT`). 96 cells, all painted at build.
- Brass kerb: `cut_copper` r 11.55–12.35 at y64, top ring r 11.55–12.10 at y65, gold/quartz studs every 45°.
- **Floating read:** a 1-block-wide shadow moat at r 12.30–13.30, y64 air over `deepslate_tiles` at y63, with eight `ochre_froglight` under-lights. The outer lip (r 13.30–13.45) is dropped one block so a chicken that hops the kerb can hop back out (64 → 65 → 66).
- Hub medallion: flat gold/froglight cross at r ≤ 1.75; reveal plinth 3×3 at y65 (`smooth_quartz`, gold corners, froglight core).
- Totems/labels unchanged: 8 totems at r 10.55, `L.wedge8` r 8.5, `L.wedgeLabels8` r 11.9 y 74.5, 6-game set recorded but not built.

### Cage towers (one per pod, above the existing rooms)
The lower room is untouched (y65 floor, y66 carpet, y66–69 walls/glass, rear gate y66–67, 4 seats, glass/copper roof y70). The tower starts one block above the roof and is **hollow**:

| y | Layer |
|---|---|
| 70 | base collar (`setIfAir`, `white_concrete`/`cut_copper`) closing the slot above the room walls |
| 71–72 | white glass base band, team terracotta/concrete corners |
| 73–79 | hollow cage: continuous team verticals on the angular sides and the concourse face (white-concrete centre stripe), copper lintels, open toward the dial |
| 80 | copper/gold belt (perimeter only) |
| 81–82 | glass collar, team dyed (`K_GLASS` accents, so `setPodState` recolours it) |
| 83–85 | flared crown band (sector widened 1 block): gold / team terracotta / white concrete; the body is decked in team concrete at y85 so the shaft is closed under the cap |
| 86–88 | 3×3 cap: team concrete, white concrete, gold+yellow with an `ochre_froglight` centre |
| 89 | `end_rod` finial |

Top y89 = 13 above the terrace. Each tower is a sector shell (the pods are arcs, not 5×5 boxes); the crown flare gives the 7×7 silhouette and the 3×3 cap the reference tip. A froglight at y71 lights the hollow shaft through the room's glass roof.

### Crown truss (replaces `orrery()`; centre 0.5, −61.5)
| Part | Coordinates |
|---|---|
| 8 gold pillars | 3×3 black/gold plinth at y82–83; 1×1 `gold_block`/`cut_copper` shaft y84–90 at r 7.4, every 45° |
| yellow top ring | y91–92, r 6.7–8.5, `yellow_concrete`/`gold_block`/`yellow_terracotta`; 17×17 plan |
| gold rail | y93, r ≤ 8.0 |
| red jewels | `red_concrete` y94 + `red_stained_glass` y95 on the 8 pillar lines |
| white cross/truss | 4 axis beams + 4 diagonal braces at y86; axis beams also y87; `smooth_quartz`/`calcite`, gold at the ends |
| gold hub | y85, 3×3 with an `ochre_froglight` core; four `sea_lantern`s under the truss |
| hanging lights | 8 `end_rod[facing=down]` at y90, r 6.9 |

Crown extent: y82–95 (14 tall), inside the glass dome (drum top 102, dome 103+). It is deliberately unsupported (a floating orrery crown); no chains were added, so nothing hangs into the entrance sightline.

The old 6×6×6 armillary cage, planet, three rings, central/4-corner chains, the interior clock ring and the 18 hanging star clusters are **removed**. `armillaryRing()` is kept (still used by the exterior topper).

### Anchor contract (all values printed by the checker)
- `L.timer` / `L.clock` = `0.5, 88.8, −61.5` — inside the crown's open ring, under the yellow ring and above the white truss; the 6× countdown reads from every stand.
- `L.revealSurfaces[4]` = `(0.5,86,−52.2)`, `(9.8,86,−61.5)`, `(0.5,86,−70.8)`, `(−8.8,86,−61.5)` (r 9.3, four crown faces). Reveal icon lands at y88.5, title at y91.8, both outside the gold ring; fireworks spawn at y90.
- `L.podium` = `0.5, 66, −61.5`, `podiumBaseY = 65` (3×3 plinth; the old 7.5-radius dais is gone so the wedges read).
- `L.pods[10]` = y66, nominal r 14.2; the cardinal pods are nudged 0.35 outward so the spawn cell is never inside the 0.85-thick front sill (all ten verified air/carpet at feet, air at head).
- `L.dialCapacity = 8`, `L.dialRadius = 12`, `L.dialGeneration = HubGenerator.VERSION = 5`, `L.dialCentre = 0.5,65,−61.5`.
- `L.podGates`, `L.podBars`, `L.podAccents` semantics unchanged; the only new accent cells are the y81–82 tower collars (K_GLASS).
- `L.divider8/6`, `L.dividerSeam8/6`, `L.wedgeAccents8/6` semantic unchanged. The divider teeth are recorded but **not raised at build**; `setDividers(progress)` raises them as before, so the venue opens on the flat seam grid.

### Concourse / fill ring / lighting
- Concourse y64 repaved in `smooth_quartz`/`calcite`/`quartz_block` with `gray_concrete` seam rings at r 21.4/23.1 and copper rays every 18°; ten white medallions with team-concrete cores at r 21.7; front parapet and outer rail restyled white/copper/gold.
- Eight white quartz pilasters at r 20.1 on the pod boundaries `{36,63,117,144,216,252,288,342}` (the grand stair at 90 and the opposite aisle at 180 stay clear): 3×3 base y65, 1×1 shaft y66–76 with calcite/team bands, 3×3 gold/sea-lantern cap y77–78, gold finial y79.
- The same eight angles carry a quartz/copper/team rib on the drum wall at r 36.2, y79–87, so the ring continues above the top walk.
- Inner wall: y79 `cut_copper`, y80 `smooth_quartz`, y81 `calcite` + gold studs; the team banner band (82–87), cream flutes (89–95), starfield (97+) stay.
- Stands: continuous `calcite` landings on k=27/31/35 and warm `ochre_froglight` markers every 72° on the three tier nosings. Seats, aisles, stair unchanged.
- Two white/copper nebula boards at r 29, y86–90 on the east and west flanks (0°/180°) with a team-colour strip row and chains to the drum top; the old 90°/270° boards are gone so they do not block the crown from the entrance.

## 3. Validation

- `mvn -B clean package` in `plugin-build`: **BUILD SUCCESS** (`target/MinecraftMondayCore.jar`).
- `HubValidate` (offline walk of every registered stair/aisle/path): **VALIDATION PASSED** — 89 stair columns and 10 paths, 0 failures, 0 connection mismatches, 0 floating carpets.
- `CheckDome` (temporary assertion tool, not committed):
  - dialGeneration 5, capacity 8, radius 12;
  - all 10 pod spawns clear at feet/head, floor present, rear gate fully open (6/6 cells), ≥4 seats, tower shell above the roof;
  - podium standable, 4 reveal surfaces air, timer/clock anchors air;
  - all 96 `dialBand` cells painted, 152 seam cells painted;
  - crown inside its 19×19×14 box and below y97; tower ring present.
- Exterior diff: every changed block is inside the bowl (`r < 38.5`, `y 63–95`). The drum wall (`r 45.5+`), terrace ring (`r 56.5`), facade, gatehouse/clock/marquee, glass dome, armillary topper, pylons and rim flags were not touched; no exterior code path in `HubVenue` was edited.

## 4. Renders

All in `game-plugins/DecisionDome/previews/decision_dome_inside/` (opened and checked):

| File | View |
|---|---|
| `interior_cutaway.png` | SE quadrant removed (x≥0, z≥−62), bowl y58–112 — ten towers, crown, dial, stands |
| `top.png` | top-down bowl plan, y58–96 — ten tower caps + crown ring |
| `dial_top.png` | top-down dial plan, y58–80 — eight wedges, dividers, diamonds, team bands, totems |
| `iso_a.png` | full venue, angle 0 — exterior proof |
| `iso_b.png` | full venue, angle 2 — exterior proof from the rear |
| `pod_closeup.png` | pod 54 tower: base band, team verticals, belt, glass collar, crown flare, cap |
| `crown_dial_closeup.png` | crown + dial + surrounding towers |
| `compare_downloaded.png` | downloaded `iso_interior_labelled.png` beside the new cutaway |
| `pod_room_section.png` | optional z-section through pod 198's room (carpet, seats, rear gate) |

Reproduce (from the repo root, with the JDK/Maven paths used for the build):

```
javac -d <out> plugin-src/com/mcm/lobby/*.java tools/lobby_preview/HubPreview.java
java -cp <out> HubPreview <out> "bowl:-48:58:-106:48:112:-18" "bowltop:-42:58:-104:42:96:-20" \
     "venue:-78:52:-138:78:190:80" "pod54:5:62:-52:23:94:-38" "crown:-17:62:-79:17:108:-44" \
     "dialtop:-16:58:-78:16:80:-46"
python tools/render_preview.py <out>/bowl_cut.mcmdump.gz  <previews>/interior_cutaway.png
python tools/render_preview.py <out>/bowltop.mcmdump.gz   <previews>/top.png --top
python tools/render_preview.py <out>/venue.mcmdump.gz    <previews>/iso_a.png --scale 6
python tools/render_preview.py <out>/venue.mcmdump.gz    <previews>/iso_b.png --scale 6 --angle 2
python tools/render_preview.py <out>/pod54.mcmdump.gz    <previews>/pod_closeup.png
python tools/render_preview.py <out>/crown.mcmdump.gz    <previews>/crown_dial_closeup.png
python tools/render_preview.py <out>/dialtop.mcmdump.gz  <previews>/dial_top.png --top
```

The cutaway is the full bowl dump with blocks in `x ≥ 0 && z ≥ −62` removed (a legal air cut: the MCMDUMP header only stores the region size).

## 5. Runtime risks / notes

1. **Interior visible through the clerestory and dome.** The glass at y88–95 and the dome now show ten team towers and a gold crown instead of the old orrery. No exterior *block* changed, but the exterior preview is slightly more colourful because the renderer draws interior blocks through translucent glass. In game the effect is a lit venue behind blue glass, not a block change.
2. **Floating crown has no chains.** It is a deliberate magical centrepiece. If the owner wants suspension, 4 chains from the y93 rail to the oculus would be the natural addition; none were added to avoid a 50-block chain column in the entrance view.
3. **Chicken moat.** The dial's shadow moat is 1 block deep and its outer lip is dropped one block, so a chicken that hops the 1-block kerb can hop out (64 → 65 → 66). The kerb height is unchanged from v4. A chicken that stays in the moat still has a valid x/z inside `outerRadius + 4`; the vote tally is unaffected.
4. **Tower collars recolour with pod state.** The y81–82 glass band is a `K_GLASS` accent, so `caged`/`frozen`/`dunked`/`invaded` tint the tower collar as well as the room. Everything else in the tower is permanent architecture and is *not* touched by `setPodState`.
5. **Divider teeth start down.** `setDividers(0)` is called by `DomeSession.prepare`, so the venue opens flat; the 8 teeth rise only during a live vote. This is a visual choice, not a gameplay change.
6. **Pods are still rated for entry.** The only change at y ≤ 70 is the `setIfAir` base collar, which cannot overwrite the gate, glass roof or team crest. Automated checks confirm feet/head/gate/seat validity for all ten.
7. **`L.podium` moved from y67 to y66.** It is only a fallback now (`HubDialView.revealLocation()` uses `revealSurface(0)` first); `L.podiumBaseY` is still valid and the plinth is standable.
8. **No new `HubLayout` anchors.** `revealSurfaces`, `timer`, `clock`, `podium`, `pods`, band/divider/pod lists all keep their shapes; `dialGeneration` is 5.

## 6. Not done

- No deploy to `:25580`, no restart, no RCON, no world paste.
- No git commit (and none of the excluded Nocturne Orrery geometry was added to a commit).
- `HubLayout.java` / `HubManager.java` show as modified in `git status` from the pre-existing working tree; this task did not edit them.
