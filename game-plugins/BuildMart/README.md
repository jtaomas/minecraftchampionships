# Build Mart (McmBuildMart)

Play with `/mcm play buildmart`. Display name: **Build Mart**. World: `mcc_buildmart` (void world, rebuilt in code; the folder is wiped on every server start).

## Rules
- Each side (tournament team or solo player) gets its own platform on a ring around the central mart:
  a 5x5 **build plot** (left, team-coloured border) and a **pedestal** (right, quartz border) showing the current target build,
  with its name, star rating and material list floating above it.
- The **mart** in the middle has 27 stalls, one material each (2x2x3 columns, sand is 1 layer high). Mined stall blocks go straight
  into your inventory and regrow after 3 seconds.
- Four **shop counters** turn raw materials into crafted blocks (right-click, sneak-right-click for up to 16 at once):
  Glassworks 2 Sand → 1 Glass, Mason 2 Cobblestone → 1 Stone Bricks, Kiln 2 Clay → 1 Bricks, Jeweller 1 Iron Block + 1 Glowstone → 2 Gold Blocks.
- Copy the target on your plot. Every block has to be the same material in the same spot (rotation/block states are ignored, extra blocks count as wrong).
  The light grey floor tiles show which columns the build uses. The action bar shows `placed/needed` and how many wrong blocks you have.
- When the plot matches, the side scores, the plot is cleared and the next build appears. Every side gets the same order of builds
  (easier ones first, then random).
- **Big Sales:** every 75 s three random stalls go on sale for 40 s and restock in 0.5 s.
- Everyone gets unbreakable Efficiency V iron pickaxe/axe/shovel and shears, plus Haste II, and can fly. No PvP, no fall damage, no hunger.
  You can only place blocks on your own plot and only break blocks on your own plot (you get them back) or at mart stalls.
- Falling into the void sends you back to your platform and you lose the blocks you were carrying.
- 10 s preview, then 8 minutes of play.

## Scoring (MCC coins, MCC 3-13 rules; config `coins.*`, see `../COINS.md`)
- Completed build: **30 coins to each player** of the team, **2 less** every time any team has completed that build (floor 10, ours).
- End of game: each player gets the % of each unfinished build the team filled in, times that build's current value.
- The core multiplies every award by the event multiplier (x1.0 ... x3.0).

## Known limitations
- No build selection gallery or Gold Builds from the original; one build at a time per side.
- No dedicated spectator stand (spectators fly around in spectator mode).
- Block states/orientation are ignored when checking a build.
- Late joiners are left to the core's default (spectator).

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
