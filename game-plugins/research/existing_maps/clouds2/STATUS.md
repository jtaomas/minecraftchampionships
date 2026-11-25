# STATUS — A1 Clouds 2

Updated: 2026-11-25. Research only; nothing outside `clouds2/` was written.
Shared tools and map sources untouched.

## Done

- [x] World loaded and scanned read-only: 21,904 chunks, 150,414 non-air blocks,
      171 block types, bounds x −258..66 / y −64..319 / z −316..291.
- [x] **322 / 322 command blocks** extracted with exact command, block variant
      (284 repeating / 23 impulse / 15 chain), auto, powered, last-execution and position.
      Full table: `data/command_blocks.csv`.
- [x] Command system decoded: 6 redundant sensor banks, start/reset circuit,
      39-repeater countdown line, 31 failing commands, global targeting model.
- [x] **285 / 285 signs** extracted (front/back, colour, glow, wax, click events):
      `data/signs.csv`. 7 are player-facing; the rest are 6 copies of a pad legend.
- [x] **225 / 225 shulkers** inventoried: all empty, all pad surfaces.
      **0** chests/barrels/hoppers/dispensers in the world: `data/containers.csv`.
- [x] Entities: exactly 1 (`marker` with tag `refresh_worldspawn`): `data/entities.csv`.
- [x] Route rebuilt leg by leg (L0–L13) with coordinates, gates, checkpoints, pads,
      elytra grants/clears, kill plane, start/finish and lap behaviour:
      `data/route.md`, `data/route_legs.csv`, `data/checkpoints.csv` (111 rows).
- [x] Route direction proven from the map's own yellow-glazed arrow `facing` field
      (`scripts/arrow_field.py`), not from the video.
- [x] Wayfinding analysis: 8 devices, with the chevron statistics.
- [x] Block palette + gradients + lighting: `data/blocks.csv` (171), `data/palette.md`,
      `renders/palette_bars.png`.
- [x] Map-side resource-pack mapping: `data/pack_mapping.csv` (33 rows). Pack inventory
      is linked to A10, not duplicated.
- [x] 1.21.1 compatibility: 30 item-NBT boot commands + 1 malformed `setblock` fail;
      everything else is syntactically valid. `data/command_failures.csv`.
- [x] A12 cross-check recorded in README §4.6–4.8, including the Multiverse survival /
      `allow-flight=false` interaction and bank-coverage vs simulation distance.
- [x] 51 rendered images, all opened before citation; `REFERENCE_IMAGES.md` maps each
      to what it shows and what to take.
- [x] Two video frames copied and cited only for the start-gate language and the static
      actionbar (`frames/README.md`).

## Blocked

Nothing blocked. No server access was needed; A12 supplied the live-environment facts.

## Tool note (read-only, no fork shipped)

`tools/world_to_mcmdump.py` is too slow for this world: it timed out after 15 minutes
having processed one region (80,603 blocks). I did **not** modify it. I wrote an
equivalent MCMDUMP writer with numpy-vectorised palette decode
(`scripts/dump_mcmdump.py`, ~3 min for the full 150k-block world) and used the
unmodified `tools/render_preview.py` for every render. If the team wants one dumper,
the numpy path in `scripts/dump_mcmdump.py` is the drop-in optimisation.

## Open questions (also in README §11)

**Answered (H, server config via A12/coordinator):**

1. **Simulation distance is 6 chunks, view distance 8.** Six chunks = 96 blocks. That
   covers most of the loop but **not** the village (~111 from bank A) or the ring-flight
   middle (~160). On the review server the L9 village elytra grant will not fire and the
   kill plane is inactive for most of L8. This confirms the 4.8 finding at the real
   setting; see README §4.8.
2. **Multiverse respawn-world is empty; world spawn is 5,4,23.** `setworldspawn` is the
   respawn path, so checkpoints work as designed (shared by all players).
3. **No other imported map audited so far ships the “refresh” datapack.** It remains
   missing here; the scoreboard and `refresh_worldspawn` marker are the only traces.

**Still open:**

4. **Start-button restore.** `setblock 5 4 22` is missing its block argument. Is the
   button meant to be re-placed, or is the failure harmless because stone buttons pop
   back? No evidence either way.
5. **Lap count.** The build is a closed loop but no command counts laps. Assume 1 lap.
6. **“Currency: 9.3k?” from the brief.** There is no currency item, score, entity or
   container in the shipped world. Actual totals: 150,414 non-air blocks, 832 block
   entities (322 command blocks + 285 signs + 225 shulkers), 1 entity. Any 9.3k figure
   must come from the missing datapack or the video, not from this download.
7. **Fan-recreation vs original.** The two cited frames come from a fan recreation; the
   yellow pad art in the frame does not match the download's chevron texture exactly.
   Do not use the video for palette claims.

## Notes for downstream agents

- **A13 synthesis:** the reusable ideas are the chevron floor, the pad colour language,
  the 6-bank chunk-loading redundancy, and the theme-per-leg palette. The things to
  reject are global `@a` targeting, the static “1st place” HUD, and the missing finish
  logic.
- **A12:** the simulation-distance (6) and Multiverse respawn (empty) questions are now
  answered from your config; no further check needed there. If you re-test, the two
  expected gaps at 6 chunks are the village elytra grant and the L8 kill plane.
- **A10:** my `data/pack_mapping.csv` is the map side only; your `data/inventory.csv`
  is the authority for pack contents. The one overlap is the invalid `sounds.json`,
  which we both found; my extra observation is that **no command plays any
  `acerace.*` event**, so the broken index is currently harmless.
- **A14 QA:** the 111-row `checkpoints.csv` is curated (key route points), not every one
  of the 396 raw pad components; `data/pads.json` has the raw components.

## File inventory

```
README.md
REFERENCE_IMAGES.md
STATUS.md
data/blocks.csv
data/checkpoints.csv
data/command_blocks.csv
data/command_failures.csv
data/containers.csv
data/entities.csv
data/pack_mapping.csv
data/palette.md
data/route.md
data/route_legs.csv
data/signs.csv
data/pads.json
data/block_entities.json
data/block_counts.json
data/scan_summary.json
data/clouds2_full.mcmdump.gz
data/cave_air.txt.gz
data/cb_plain.txt
data/signs_plain.txt
data/crops/*.mcmdump.gz
frames/README.md
frames/f_0034_start_line.jpg
frames/f_0130_finish_approach.jpg
renders/*.png  (51)
scripts/*.py   (analysis only)
```
