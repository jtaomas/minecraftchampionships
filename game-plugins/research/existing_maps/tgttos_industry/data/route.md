# Industry — route, leg by leg

All coordinates are world block coordinates. "Deck" means the top of the conveyor table at y=-35; a player stands at y=-34. "Gap" means the void between tables, with the table foundations ending at y=-42..-61 and the drop continuing to y=-64. Falling below y=-58 teleports the player to the start (73,-33,11) without resetting the timer.

The route is **free-form**: there is no checkpoint, gate, or ordered leg system in the datapack. The line below is a fast reference line, not a required path. H for geometry; M/L for the "fast" judgement.

## Overview

```
z=  8..24   DECK 1   x=26..120   start box at x63..84, z9..13, spawn 73,-33,11
z= 25..51   GAP A    27 deep; 3 glass fins at x=55, 73, 91 (z26..50, y=-61..-23)
z= 52..62   DECK 2   x=26..120
z= 63..87   GAP B    25 deep; 3 paint curtains at z=66 (red), 74 (yellow), 83 (red)
z= 88..98   DECK 3   x=26..57 and x=89..120, finish pen x=58..88, floor y=-40
```

Decks are 95 wide (x26..120) and 17/11/11 deep; the foundations are 1 block smaller in x/z (27..119). Each table is a solid 27-block stack from y=-61 to -35: a 21-block plinth (y=-61..-41) and a 6-block deck body (y=-40..-35). The walkable top is y=-35; the void between tables is open from y=-34 down past -61. H.

## Leg 0 — start box

- Box: x63..84 (22 wide), z9..13 (5 deep, 3-deep interior), y=-33..-29. Walls are barrier blocks (184 total); the north wall at z9 is gray concrete / black wool, not barrier, with the single oak wall sign at 73,-32,10 (facing south).
- Floor: quartz block x65..82, z10..12 at y=-34; black wool border; the box sits 1 block above the deck (deck top -35). Spawn: 73.5,-33.0,11.37, yaw ~0 (south). H.
- On GO (countdown=140) the barrier box is filled with air; the player exits **south** onto DECK 1. The visible "Start" line is Text2; the clickEvent is on the empty Text1. Sign clicks run all four line events (1.21.1 `SignBlockEntity` loops the four messages), so the empty-line event fires when the sign is used. H for data and 1.21.1 code; M for 1.19.1 in-game.
- The box is cleared every tick by `fill 83 -30 10 64 -33 12 air replace #minecraft:mineable/pickaxe`, so no pre-built blocks survive inside it. H.

## Leg 1 — DECK 1 (z8..24)

- Straight run in x, 95 blocks long. Surface: alternating blackstone / gray wool strips (the conveyor), polished andesite side rails, blackstone stair ends. H.
- Two machine heads at x40..42 and x104..106 rise from the deck to the roof: red sandstone/blackstone frames, white concrete control panels, a sea-lantern "button", glass panes, and 1-block-wide brick/magenta towers. They are solid obstacles on the deck edges but do not block the central lane. H.
- Quartz pillars at y=-31..-27 under the deck (x40..106, z8..98) are decorative legs; they are below the walking surface. H.
- The deck edges at z8 and z24 are 1-block lips; the first gap starts at z25. H.
- Sightline: from the start you see the three glass fins and their catwalks across GAP A, the roof/skylight above, and the machine heads left/right. The finish is not visible (the middle deck and the curtains block it). H geometry; M player view.

## Leg 2 — GAP A (z25..51)

- 27-block void; no floor at y=-35. The only structures are three **glass fins**:
  - x=55, z26..50, y=-61..-23 (25 long, 39 high, 1 thick; 975 light-blue stained glass each).
  - x=73, same.
  - x=91, same.
- Each fin carries a 3-wide iron-trapdoor catwalk at y=-20 (z28..48) with orange/red sandstone posts; the catwalks are 15 blocks above the deck. They do not connect to the decks or to each other's ends; they are not a usable route without towering. H for geometry; M for "decorative".
- Lane choice: the fins split the 95-wide deck into four usable lanes: x26..54, x56..72, x74..90, x92..120. The start/spawn x73 is exactly on the middle fin line, so the player must move left or right before bridging. This is the first decision of the run. H.
- Crossing: bridge with the auto-refilled offhand concrete (1 block at a time, refilled every tick) or tower onto a catwalk. The void respawn is below y=-58, not an instant kill. H for kit; M for the time cost.

## Leg 3 — DECK 2 (z52..62)

- 11 deep, 95 wide. Same conveyor surface and side rails. The two machine heads continue at x40..42 / x104..106. H.
- The deck is the last safe place before the paint curtains. H.
- Sightline: the three curtain rows are directly ahead; the glass window bands (y=-33..-27) are above eye level (player eye ~-32.4), so you see the wall faces and the window band, but the far side is only visible through the glass. H geometry; M player view.

## Leg 4 — GAP B (z63..87) — the paint curtains

- 25-block void. Three 1-block-thick concrete walls, each 25–26 wide × 39 high (y=-61..-23), with a 9-high window band (border + glass + diagonal paint drips):
  - z=66 red: x29..53, 61..85, 93..117. Windows have **white** concrete borders/drips.
  - z=74 yellow: x12..36, 44..69, 76..101, 109..134. Windows have **black** concrete borders/drips.
  - z=83 red: x29..53, 61..85, 93..117. White borders/drips.
- Walkable x-gaps at deck level (within x26..120):
  - z66: 26..28 (3), 54..60 (7), 86..92 (7), 118..120 (3).
  - z74: 37..43 (7), 70..75 (6).
  - z83: same as z66.
- The slalom: a straight line at x57 passes z66 (gap 54..60), is **blocked** by z74 (segment 44..69), then passes z83. A straight line at x73 is blocked by z66 (61..85), passes z74 (gap 70..75), is blocked by z83. No single x lane passes all three inside the deck; the minimum is one shift. H from the segment spans.
- The intended fast line is: cross z66 at x54..60, shift east to x70..75 along z67..73, cross z74, shift west to x54..60 along z75..82, cross z83. That is the line drawn in `renders/route_top_labelled.png`. M/L (one of several lines; any gap works).
- Alternative: mine a hole through a concrete segment with the iron pickaxe (CanDestroy all concretes, Efficiency II) or tower over the 12-block top at y=-23. Both cost more time than the weave. M.
- The curtain tops are at y=-23; the catwalk grid at y=-20 crosses above them at z65..67, 73..75, 82..84 (segments in x), but it is disconnected from the decks. H.

## Leg 5 — DECK 3 / finish pen

- Deck 3 is split by the pen: x26..57 and x89..120, z88..98 (the block at z98 is the south lip). H.
- Finish pen: x58..88, z88..98 (31 × 11), floor y=-40 (smooth stone), side walls x57/x89 (blackstone, 5 high), end walls z88/z98 (acacia planks, 4 high, top y=-36). The pen is a closed basin; you step over the 1-block lip (deck -35 → wall top -36) and drop 4 to the floor. H.
- 24 chickens stand on the floor at y=-39, x61.84..86.80, z89.50..97.50. They are non-persistent (PersistenceRequired 0) and have Resistance V (amplifier 5, duration 19, refreshed each tick by the datapack). H.
- Finish: punch any chicken. The `hit_chicken` stat (`minecraft.custom:minecraft.damage_dealt_resisted`) increments because the Resistance V hit is fully resisted; `finished` is set, the player becomes spectator, and the time title appears. H for data; M for the exact "fully resisted" threshold (Resistance V is 100% in 1.19.1).
- After 140 ticks (7 s) the player is teleported to 73,-33,11, the barriers are rebuilt, adventure is set, the inventory is cleared, and the timer/`finished`/`hit_chicken` are reset. H.
- The pen is not visible from the start or from DECK 2; from the z83 lip you see the far deck and the top of the basin, but the chickens are 4 below the lip and only become visible at the edge. H geometry; M player view.

## Fail states

- Fall below y=-58 → `tp @s 73 -33 11`; timer keeps running, inventory/kit is not explicitly reset (the kit is refilled every tick anyway), barriers are not rebuilt. H.
- No lava, water, fire, mob, or damage hazard exists in the world; `fallDamage=false`, `difficulty=peaceful`, `doMobSpawning=false`, `keepInventory=true`. H for blocks/gamerules.
- No checkpoints: a fall costs the whole bridge from the last deck. H (no checkpoint code).

## Reference times (L)

No timed run was recorded. Rough model: sprint 5.6 b/s, bridge ~3–4 blocks/s with the 1-block refill. Start→z24 13 blocks, GAP A 27, DECK 2 11, GAP B 25, lip 5, pen run ~15: ~96 blocks of travel plus ~52 blocks of bridging. Expert 25–35 s, median 45–65 s; the curtain weave and the 1-block-at-a-time refill dominate. [L; not measured]
