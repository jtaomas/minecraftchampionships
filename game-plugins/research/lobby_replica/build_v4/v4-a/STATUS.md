# V4-A — island base, terraces, water, entrance, rainbow plinth, giant MCC columns

**Status: PARTIAL / CHECKPOINT.** The generator runs end-to-end and all outputs listed below
are written and current (regenerated from the same source at checkpoint time). The build has
**not** been pasted anywhere, **not** been run through the verifier, and the before/after
overlay QA against the reference photos has **not** been produced.

Origin `0,120,0`. Build box `x -150..150`, `z -150..150`, `y 90..210`. Vanilla Java 1.21.1.
No dome geometry: `r < 28` at the centre is empty above `y = 120` (only the y=120 floor exists).

## What is complete

| item | detail |
|---|---|
| island base | floating island, irregular coast from 7 lobes + 2 coves + noise, top surface y=120, sand/gravel shore lip at y=119, 3-wide rock cliff band down to y=115, hanging rock keel to y=96 |
| water | moat `r 38..52` around the dome island: bed y=115, water y=116..118, stone quays both banks, scattered seagrass + shore ice |
| dome island | `r 38` yellow-concrete terrace (`yellow_concrete`, stone inlay rings), stone rim at y=120, axis red carpet, stone balustrade with post/rail rhythm, gate piers with lanterns, 6 island lamp posts |
| rainbow plinth | ring `r 28..30`, y=120..128, 20 vertical rainbow bands (`*_concrete`), blackstone band, dark cap, 24 dark rim posts at r≈29.4 y=129..131, 9-wide red carpet stair z=31..37 (y 121..127) with quartz cheeks + lantern newels, red carpet on the plinth top |
| plaza + axis | brick plaza `x ±38, z 54..112` with stone border; 11-wide yellow axis band + 7-wide red runner; 25-wide red/yellow MCC floor emblem (crown + M) on a stone ring at `0,120,86`; transverse stone bands; south exit path to the shore |
| terraces | 4 raised planters (2 tall terraces z 98..112 with hedges, 2 low edge planters z 66..80), cream `smooth_sandstone` walls, oak-leaf hedges, flower pits, snow coping |
| entrance bridge | 13-wide stone-brick deck `x -6..6, z 38..52` over the moat, cracked-stone edge, stone balustrades with post/rail/slab, snow, 4 newel lanterns |
| two giant MCC columns | at `(±21, 55)`: 15×5 stone plinth, 9×4 smooth-quartz core, 13×2 red banner with tapered foot, white 7×7 `MCC` letters 1 block proud, 1-wide white side pillars, 17-wide quartz capital, gold crenellated crown, gold side lamps, festive red bow — 42 blocks total (y 120..161) |
| team-colour entrance row | 10 columns 3×3, 10 tall + cap + snow, team order red, orange, yellow, lime, green / cyan, aqua, blue, purple, pink at `x ±18..±46, z 60..62` (decorative entrance row, **not** V4-C's team section) |
| snow pass | festive drifts on grass (`snow[layers=1]` + `snow_block` cores), snow on planter walls/coping, hedges, rail tops, plinth cap, capitals/crowns, column caps, shore ice |
| paths | 5-wide stone-brick connection stubs: plaza→Hall of Fame door (west, ends `-49,46`), plaza→east arm→`96,18` (toward aqua), north-east spur |

## Numbers (checkpoint run)

- **115,092 blocks**, **56 block states**, **26,798 commands**, 13 chunk files.
- Per section: island surface 47,235 · keel 23,692 · moat 17,540 · dome island 5,345 ·
  plinth 4,548 · plaza/axis 4,996 · gardens 2,058 · bridge 564 · giant columns 5,146 ·
  team row 1,080 · paths 1,028 · shore dressing 616 · snow 8,539.
- Largest single function chunk is well under the 16,000-line chunk size; total is under the
  vanilla 65,536 command-chain limit, so `load.mcfunction` can run in one tick.

## Files in this folder

| file | state |
|---|---|
| `gen_v4a.py` | **complete** — the only source of truth; reruns deterministically |
| `blocks.csv` | **complete**, `x,y,z,block,section`, absolute world coords, 115,092 rows |
| `functions/v4a_*.mcfunction` | **complete**, 13 files, `fill`-merged x-runs + `setblock` |
| `load.mcfunction` | **complete**, ordered `function v4a:...` calls |
| `preview.mcmdump.gz` | **complete**, full build box 301×121×301 |
| `preview_iso.png` | **complete** (2000×1148) |
| `preview_top.png` | **complete** (1496×1346) |
| `stats.json` | **complete** (counts, skipped blocks, overwrites) |
| `scratch/` | QA crops + two throwaway renderers (`persp_render.py`, `front_render.py`) — not part of the build |
| `STATUS.md`, `GAPS.md` | this checkpoint |

## What is partial / not done

1. **Verification not run.** `verify_v4a.py` (bounds, dome-empty check, block-id whitelist,
   command syntax, floating-block scan) was planned but not written at checkpoint time.
   `stats.json` exists but nothing has been asserted against it.
2. **No side-by-side / overlay QA** against `MCCHub-SD/S4/S6`. Only visual inspection of the
   iso/top renders and throwaway perspective/projection renders.
3. **No front-view preview committed.** `preview_iso.png` + `preview_top.png` are the required
   deliverables and exist; the perspective QA shot is `scratch/persp.png` only.
4. **Balustrade** is a post/rail abstraction (`stone_brick_wall` posts + `smooth_stone_slab`
   cap); the reference railing's exact profile is not reproduced.
5. **Column lettering** is a 7×7 block font, not a traced 1:1 of the reference glyphs.
6. **Terraces** are the planter terraces + plinth stair; no broad stepped land terraces were
   added, to keep y=120 free for V4-B/C/D footprints.
7. **No team section, no THE CHAMPS, no Hall of Fame, no rainbow bridge, no snow globe, no aqua
   building, no training portals, no props** (trees/boats/balloons/presents/stalls) — by scope,
   those are V4-B/C/D. The island is therefore visually empty away from the entrance.
8. **The dome is absent** (by scope) so the plinth reads as a yellow bowl in the previews.

## Remaining work for V4-A (in priority order)

1. Write and run `verify_v4a.py`: 0 blocks outside the build box; 0 blocks with
   `y > 120 and r < 28`; every id in a vanilla 1.21.1 whitelist; every command parses; report
   the count of unsupported/floating blocks.
2. Produce `overlay_*.png` / side-by-side crops against S4/S6/SD and correct the three
   proportions most at risk: column height vs dome, bridge length, plinth visible height above
   the balustrade.
3. Decide the railing question (continuous in the reference vs the 9-wide gate built here — see
   GAPS) with the owner.
4. Confirm path stubs with V4-B/D (west path ends at the Hall of Fame door; east path ends at
   `96,18`).
5. Re-render `preview_iso.png` / `preview_top.png` after any change, then hand to V4-E.

## Do not

- Do not paste this into 25591 (V4-E owns integration) or touch 25580.
- Do not fill `r < 28` above y=120: it is reserved for the dome.
