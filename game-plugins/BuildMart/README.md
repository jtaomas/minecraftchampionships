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

## Scoring (per member of the side, via `addPoints`)
- Completed build: `4 + 3 x stars` (7–19), stars 1–5 from block count (shop blocks count double).
- Speed bonus: `+5` minus 1 per 30 s spent on that build (min 0).
- First / second / third side to finish that build number: `+4 / +2 / +1` (only with 2+ sides).
- End of game, most builds completed: `+10 / +6 / +3` for 1st/2nd/3rd (ties share the rank; needs 2+ sides and at least one build).

## Known limitations
- No build selection gallery or Gold Builds from the original; one build at a time per side.
- No dedicated spectator stand (spectators fly around in spectator mode).
- Block states/orientation are ignored when checking a build.
- Late joiners are left to the core's default (spectator).
