# MCC lobby layout v2 — from the research aerials

Reference images, in order of authority:

1. `reference/hub_festive.png` — festive front view, dome/crown/clock, globe, mountain, right-side portals.
2. `reference/hub_exterior_day.jpg` — clear aerial of the real layout.
3. `reference/hub_exterior_modern.jpg` — same layout, modern render.
4. `reference/dome_exterior_labelled.png` / `dome_plan_labelled.png` — dome dimensions and drum.
5. `reference/dome_interior.png` — interior pods/dial reference.

## What the aerials actually show

- The **Decision Dome sits on its own round island**, surrounded by water.
- **One bridge** runs from the dome island south to the main beach.
- The main land is a **crescent/irregular beach island**, not a full ring:
  - wide sand/plaza in front (south),
  - `THE CHAMPS` mountain on the left/west,
  - a white/red/gold portal building left of the dome,
  - white/blue and colourful portal buildings on the right/east,
  - water and boats on the far right,
  - trees, terraces and paths around the beach.
- The **giant globe is in the foreground** on the main beach, in front of the bridge.
- Hot-air balloons and floating islands sit above/behind.
- The festive version adds snow, the red bow, candy canes and presents.

## Target site

`C2 = 6000, 120, 6000` in `mcc_lobby_official`. New chunks, far from `mcc_lobby`, `mcc_official_hub` and the discarded 5000 ring.

## Layout

- **Dome island:** centre `6000,120,6000`, radius 30, top y=120, rock skirt to y=90. Dome drum 53 wide; leave the centre empty for the live dome.
- **Water ring:** radius 30…40 around the dome island, y=119…120.
- **Bridge:** one 9-wide bridge at `x 5995…6004`, `z 6030…6042`, from dome island to beach, with lantern posts.
- **Main crescent island:**
  - South beach: `x 5915…6085`, `z 6040…6115`, sand/grass, y=120, irregular edge.
  - West arm: `x 5890…5965`, `z 5965…6055`, grass/snow terraces.
  - East arm: `x 6035…6115`, `z 5965…6070`, grass/terraces and waterfront.
  - Back/north is mostly water; the dome island is the only central land.
- **`THE CHAMPS` mountain:** centre `5925,120,6005`, footprint about 55×40, height 45, sign facing south-east toward the beach/bridge.
- **Giant globe:** centre `6000,132,6090`, radius 12, on the beach in front of the bridge, not on a separate plinth.
- **Portal buildings:** clustered, not a ring:
  - White/red/gold left portal: `5935,120,6045`, facing east.
  - White/blue right portal: `6060,120,6025`, facing west.
  - Rainbow/colourful right portal: `6090,120,6055`, facing west/south-west.
  - Green/gold right portal: `6085,120,6000`, facing west.
  - Optional additional portals along the east arm at `6070,120,5985` and `6040,120,5970`.
- **Beach props:** palms, umbrellas, market stalls, boats on the east water, balloons, presents, candy canes.
- **Snow layer:** festive version adds snow on roofs/terrain and the red bow/candy canes.

## Build rules

- Keep the dome island clear in the centre; the live dome is owned by another bot.
- Do not build a full ring of paths or eight evenly spaced portals.
- Do not touch `mcc_lobby` or `mcc_official_hub`.
- Generate datapack functions; no server commands from builders.
- Base builder owns terrain/water/bridge/beach only. Landmarks/portals are separate passes.
