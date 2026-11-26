# Build Mart (McmBuildMart)

Play with `/mcm play buildmart`. Display name: **Build Mart**. World: `mcc_buildmart` (void world, generated in code; it is
kept between games and regenerated when the map version changes).

## Rules
- Each side (tournament team or solo player) gets a private **build zone**: three plot pairs side by side, each pair a
  **reference plot** (back) and a **build plot** (front) — a 7x7 surface with the build's own themed floor on a white quartz
  plinth. The reference copy of the target build stands on the back plot.
- The zone also holds a portal to the mall, three crafting tables, a stonecutter, three furnaces and a chest per plot.
- **Blueprint Gallery** (in the mall plaza): seven pads, each showing one build with a floating name/star/material hologram
  and a gold post with a red button in front of it. Right-click the button to send that build to your first free plot; three
  builds at a time, and a completed build frees its plot. Unselected pads restock every 90 s; a pad whose build is on a
  team's plot stays until that build is completed.
- **Gold Build**: from 4:00 into the game, then every 2:00, the gold pad carries a **7-star** build. It has **no reference
  copy** — memorise it in the gallery — and you have **2 minutes**; if it is unfinished when its time is up, the blocks on
  that plot are removed and it cannot be completed later.
- The **mart**: five department halls (LUMBER, OUTDOOR, STONE, ORES, COLOURS) packed with mineable shelves, plus four
  service counters (Water Bucket, Shears, Flint and Steel, Coal). Mined blocks go straight into your inventory and the shelf
  block regrows after 3 s.
- Copy the target on your build plot: same block, same spot, same orientation (stairs, slabs, doors, log axis). Connection
  shapes (stair shape, fence/rail links) and door hinges are ignored; extra blocks count as wrong. The action bar shows
  `placed/needed` and how many wrong blocks there are.
- Everyone gets unbreakable diamond pickaxe/axe/shovel and shears (Efficiency IV) and a **Shopping Cart** in hotbar slot 5:
  use it on the mall floor (frosted ice) to ride the trolley. No PvP, no fall damage, no hunger.
- You can only place blocks on your own team's build plots and only break blocks on your own plot (you get them back) or at
  the mart shelves.
- Falling through a floor pit sends you back to your build zone and **loses every resource you are carrying**.
- 20 s pre-game (admins can `/mcm skipwait`), then **12 minutes**.

## Scoring (MCC coins, MCC 3-13 rules plus the MCC 14 gold; config `coins.*`, see `../COINS.md`)
- Completed build: **30 coins to each player** of the team, **2 less** every time any team has completed that build (floor 10, ours).
- **Gold Build:** its value **plus 30** (`coins.gold-bonus`), i.e. double a regular build at the base value, which is how
  MCC 14 paid golden builds.
- End of game: each player gets the % of each unfinished build the team filled in, times that build's current value. Gold
  builds are excluded from that, as in MCC.
- The core multiplies every award by the event multiplier (x1.0 ... x3.0).

## Known limitations
- The gold is taken into the first free normal plot: the MCC 13 build zone has no fourth 7x7 plot. It keeps "no reference
  copy", 7 stars, the 2-minute window and the wipe on failure.
- The gallery is in the mall plaza; the MCC 3-13 map never had one.
- Gallery holograms are text only (item icons need a gallery UI in the resource pack).
- No dedicated spectator stand (spectators fly around in spectator mode).
- Star ratings of the classic builds are ours (MCC published none for them), and only two builds are 1-star and two 5-star.
- Late joiners are put on their team if the game is running, otherwise the core's default (spectator).

## Classic Mall v12 (owner fix list, Nov 2026)
- Departments are packed with **server-rack shelving** (MCC 13's warehouse racks, 021810, dressed as a data centre on the
  owner's request): rows 2 deep, back to back, 4-wide aisles (hazard stripe, 2 white tiles, hazard stripe), cabinets of
  4 shelves x 2 blocks (3 columns wide, 2 in COLOURS), ladders on the posts, black caps with status lights (redstone lamps
  blink), wire cable trays on top and across the aisles, black end-of-row panels with green lights and a `RACK L1` label.
  ~8,950 mineable blocks (was ~1,300): e.g. redstone block 192 (was 4), oak log 144 (was 12), every colour 32-64.
- Department doors get quartz jambs and a lintel in the department colour; service counters moved beside the doors.
- The team build zone has a roof (y 80): glazed like the mall (white beams, glass, sea lanterns) with a quartz/red cornice,
  so builds are lit by daylight.
- Admin: `/bm stock` prints the stock per material; `/bm fdump x1,y1,z1,x2,y2,z2 <name>` writes a full-state dump.
- Pack (`_candidates/pack/build_pack.py`, Build Mart section): floor ages retextured, name "Mall Floor", trolley texture.
  Previews: `previews/compare_departments.png`, `compare_room_roof.png`, `compare_mall_after.png`, `pack_tex.png`.
