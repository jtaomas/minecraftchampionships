# MCC festive hub — strict 1:1 build brief v4

**Goal:** reconstruct the MCC event hub in the reference images at **1 block = 1 block**, not a themed interpretation. Leave the Decision Dome to the other bot.

**Test server:** `localhost:25591`, RCON `25592`, password `LobbyBuildRcon2026`, world `lobby_build`, void flat. **Never touch 25580.**

**Build origin:** `C4 = 0, 120, 0`. Build area `x -150..150`, `z -150..150`, `y 90..210`.

## Primary references (open these actual files first)

- `reference/MCCHub-SD.png` — current festive hub, primary composition.
- `reference/MCCHub-S4.png` — same hub without snow, clear layout.
- `reference/MCCHub-S6.png` — current entrance, giant MCC columns and team-coloured columns.
- `reference/MCCHub-13.png` — older festive hub, red bow and snow globe.
- `reference/MCCHubSeason1.png` — original hub, clearest aerial of dome/mountain/biosphere/team towers.
- `reference/MCCNewHub.jpg` — side aerial, rainbow bridge and island shape.
- `reference/MainHub.png` — front axis and team-coloured overhead banners.
- `reference/MainHub-StageArea.png`, `StageAreaBuilding.png`, `StageAreaAnC.png`, `StageAreaPride.png`, `StageAreaTwitch.png`, `StageAreaIntro.png` — the 10 team plots and player skins.
- `reference/HallOfFame.png`, `reference/TheChamps.png` — Hall of Fame and champions stage.
- `reference/MainHub-AR.png`, `MainHub-DB.png`, `MainHub-PKW.png`, `MainHub-RSR.png` — training portals.
- `reference/DecisionDomeS3.png` — dome reference only. **Do not build the dome.**

## What is being built

1. **Island and terraces** — irregular floating/jungle/snow island, water at the edges, snow on the festive version, paths, trees.
2. **Entrance** — bridge from the south, two giant red MCC banner columns with white `MCC` letters and gold crown caps, team-coloured column row at their base, checkered/red-carpet axis to the dome.
3. **Rainbow plinth only** — circular plinth `r=30`, y=120..128, with red carpet stairs and a central opening `r=28` for the other bot’s dome. **No dome drum, roof, crown, clock or sign.**
4. **`THE CHAMPS` mountain** — west at `-95,120,0`, 55×40, height ~50, sign facing east/southeast, snow-capped.
5. **Glass Hall of Fame / biosphere** — west at `-70,120,45`, glass dome ~r20, interior floor/columns, `HALL OF FAME` sign.
6. **Rainbow bridge** — from the mountain/Hall of Fame side to the dome at y≈155–180, 8-wide, actual rainbow bands.
7. **Snow globe** — east at `75,125,55`, glass sphere `r=14`, castle/trees inside, stone base.
8. **Aqua building / water ride** — east at `80,120,0`, white/blue, water channel/boat.
9. **Training portals** — Ace Race, Battle Box, Dodgebolt, Parkour Warrior, Parkour Tag, Rocket Spleef Rush, matching the reference training buildings.
10. **Team section** — exactly **10 team columns**, not 16. One per team, order red, orange, yellow, lime, green, cyan, aqua, blue, purple, pink. Each 3×3 column 7 high with four player-head/skin pedestals at the top. Also 10 Stage Customization plots, one per team, with a small stage and four skin NPCs. Reference: `MainHub-StageArea`, `StageArea*`.
11. **Props** — snow, spruce/palm trees, boats, hot-air balloons, presents, candy canes, lamps, market stalls.

## Ownership

- **V4-A** — island, terraces, water, entrance bridge, red-carpet axis, rainbow plinth, two giant MCC columns. No dome.
- **V4-B** — `THE CHAMPS`, glass Hall of Fame/biosphere, rainbow bridge, snow globe.
- **V4-C** — 10 team columns, 10 Stage Customization plots, player-head pedestals, team labels. No other structures.
- **V4-D** — Aqua building/water ride, six training portals, props (trees/boats/balloons/presents/lamps/stalls). No team section.
- **V4-E** — integration/QA on 25591 after A–D; no redesign.

## Build method

- Write only under `game-plugins/research/lobby_replica/build_v4/<agent-id>/`.
- Generate Python + `blocks.csv` + `.mcfunction` files. Use `setblock` and `fill`.
- Do not touch 25580. Do not paste into 25591 yourself unless you are V4-E.
- Render `preview_top.png` and `preview_iso.png` with `tools/render_preview.py`.
- Open the actual reference images before building; do not work from memory.
- If an angle/detail is not visible, write it in `GAPS.md`; do not invent.
- 1:1 scale. Match proportions and positions, not “looks close”.

## Acceptance

- Side-by-side and overlay against `MCCHub-SD.png` / `MCCHub-S4.png` / `MCCHub-S6.png`.
- Dome footprint empty and exactly reserved.
- 10 team columns, four skins each, ordered red first.
- Same landmark positions and palette as the reference.
- No live-server changes until V4-E and the owner approve.
