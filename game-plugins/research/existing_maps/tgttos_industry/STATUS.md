# A8 — TGTTOS Industry — status

**State:** done (research). No game code, plugin code, world edits, server restarts, pastes or registry changes were made. All writes are inside `game-plugins/research/existing_maps/tgttos_industry/`.

## Done

- [x] Source archive opened and confirmed: 1.19.1 / DataVersion 3117, `datapacks/Industry/`, `pack_format: 10`, plural `functions/` + `tags/functions/`.
- [x] Extracted world scanned: 4,464 chunks, 122,276 solid blocks, 91 block states / 39 base ids, 1 block entity (start sign), 24 entities (chickens), 0 command blocks, 0 containers.
- [x] Datapack unpacked and copied to `data/datapack/`; every non-empty line classified in `data/datapack_functions.csv` (264 rows; 21 old item-NBT lines, 1 tellraw 1.21.5+ risk, rest 1.21.1-valid).
- [x] Load/tick tags, 14 objectives, 10 colour tags, 140-tick countdown, centisecond timer, finish via `damage_dealt_resisted`, spectator/reset, gamerules, respawn, `block_reset` documented.
- [x] Route/anatomy measured: DECK 1 z8..24, GAP A z25..51, DECK 2 z52..62, GAP B z63..87, DECK 3 z88..98, finish basin x58..88 / z88..98 / floor y=-40; 3 fins x55/73/91; 3 curtains z66/74/83 with exact x-gaps.
- [x] Blocks/palette/lighting/build techniques documented (`data/blocks.csv`, `data/palette.md`).
- [x] 1.21.1 risks documented with exact failures and fix requirements (`data/LOAD_AND_1_21.md`).
- [x] A12 finding confirmed against the source archive: source `Enabled: [vanilla, Fabric Mods, file/Industry]`; live `mcc_tgttos_industry` (1.21.1, DataVersion 3955) `Enabled: [vanilla, file/bukkit, paper]`, `file/Industry` in neither list; folder present and byte-identical; plural folders and `pack_format: 10` unchanged.
- [x] Comparison with `McmTgttos` at architecture level, and with Badlands/Pits at file/tick/mechanic level only (links, no duplication).
- [x] Renders: 2 full iso, roof-off iso + top, labelled plan, profile, start, fins, curtains, finish, layer plan, catwalk layer; 4 full-resolution close-up frames.
- [x] Every cited image opened and indexed in `REFERENCE_IMAGES.md`.

## Blocked (not a research blocker; blocks a live run)

- **The Industry datapack does not run on the live 1.21.1 world.** Four independent causes: not enabled in `level.dat`; `functions/`/`tags/functions/` instead of `function/`/`tags/function/`; `pack_format 10` instead of 48; 21 pre-1.20.5 item-NBT `item replace` lines. Fix requirements are listed, not applied (research only). [H]
- The live world itself upgraded correctly: blocks, the sign (`front_text`) and 24 chickens survived. [H]
- No live in-game test was performed (no server restart/edits; no RCON writes). The 1.21.1 `SignBlockEntity` code confirms that sign clicks run all four line click events, so the empty-line start event should fire; the 1.19.1 in-game confirmation, the finish-stat and the `block_reset` visual question remain open.

## Open questions

1. **Start sign in-game confirmation.** The clickEvent is on empty `Text1`; the 1.21.1 `SignBlockEntity` loops all four messages and runs every `RUN_COMMAND`, so the sign should fire when right-clicked anywhere. Confirm once in 1.19.1 and 1.21.1 (permission/behavior). [H for 1.21.1 code; M for 1.19.1]
2. **Finish detection.** Confirm a fist hit on a Resistance V chicken increments `damage_dealt_resisted` in 1.19.1 (amplifier 5, duration 19). [M]
3. **Chicken survival.** `PersistenceRequired:0`; sim-distance 6 on this server puts the pen 81 blocks from the start, so they should tick, but verify after a fresh load and after a long idle. [M]
4. **`block_reset` visual.** z66 x61..85 and x93..117 are never rebuilt; confirm a hole persists after an east-lane run. [H commands; M visual]
5. **Catwalk grid intent.** It is disconnected (6–7-block gaps, no deck access); was it meant as a high route, or is it scenery? [M]
6. **Light levels.** Inferred from geometry; not measured in-game. [M]

## Key numbers

| Quantity | Value |
|---|---|
| Solid blocks | 122,276 |
| Block states / base ids | 91 / 39 |
| Bounding box | x11..141, y -61..11, z -5..99 |
| Chunks / with blocks | 4,464 / 54 (98.8% empty) |
| Entities / chickens | 24 / 24 |
| Block entities / signs | 1 / 1 |
| Command blocks | 0 |
| Datapack files / non-empty lines | 10 / 264 |
| Objectives | 14 created (16 in scoreboard.dat, 2 stale) |
| Countdown | 140 ticks = 7.0 s |
| Timer resolution | 5 units/tick = 100/s (centiseconds) |
| Respawn | y <= -58 -> 73,-33,11 |
| Finish reset | 140 ticks = 7.0 s |
| Curtains | 10 segments (6 red + 4 yellow); 8 rebuilt by `block_reset` |
| Fins | 3 at x55/73/91, 25×39×1 |

## Files

`README.md`, `REFERENCE_IMAGES.md`, `STATUS.md`, `data/` (blocks, command_blocks, signs, checkpoints, entities, datapack_functions, datapack copy, LOAD_AND_1_21, route, palette, timeline, raw scans), `frames/` (4), `renders/` (overviews, plan, profile, closeups), `_tools/` (read-only analysis scripts).
