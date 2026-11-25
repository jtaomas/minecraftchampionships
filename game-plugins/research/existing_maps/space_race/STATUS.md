# A3 — Space Race — status

**State: COMPLETE for the E2 brief.** No game/plugin code touched, no builds, no server commands,
no writes outside `game-plugins/research/existing_maps/space_race/`.

## Done

| Deliverable | File(s) | Confidence |
|---|---|---|
| World facts, version, gamerules, kit | `README.md` §1–2 | H |
| All 29 command blocks, coordinates, type, facing, exact text | `data/command_blocks.csv` | H |
| Trigger / effect / target scope / reset / 1.21 risk per command | `data/command_blocks.csv` columns 11–15 | H |
| Global `kill @a` planes verified (2), standable areas counted | `README.md` §4 F13 | H |
| Multiverse survival + `allow-flight=false` effects documented | `README.md` §9 | H/M/L |
| Countdown and start ritual decoded | `data/route.md` Leg 0 | H |
| Leg-by-leg route with coordinates and elevations | `data/route.md` | H/M |
| Elytra grant/clear zones | `data/route.md` §Zone summary | H/M |
| Trident / water sections | `data/route.md` Legs 4, 6 | H |
| Rings, pads, checkpoints | `data/checkpoints.csv` | H |
| Finish and lap behaviour | `README.md` §4 F2, `data/route.md` §Finish | H |
| All 22 signs (state, colour, glow, text) | `data/signs.csv` | H |
| Containers and entities — none exist | `data/entities.csv` | H |
| Blocks, counts, shares, palette, lighting | `data/blocks.csv`, `data/palette.md` | H |
| Top-down + 2 iso + labelled route overlay | `renders/` (see index) | H |
| 9 evidence stills | `frames/` | H |
| Image index with what to take | `REFERENCE_IMAGES.md` | H |
| Direction/readability analysis | `README.md` §5 | H/M |
| What is reusable in a premium rebuild | `README.md` §6 | M |
| 1.18.1 → 1.21 upgrade notes | `README.md` §7 | H/M |
| Raw scans / scripts (reproducible) | `scripts/`, `data_raw/` | H |

## Not blocked, but deliberately out of scope

- No RCON queries were made (rule 4). Live-world parity (command blocks enabled, Multiverse name)
  is A12's.
- The resource pack was only listed, not analysed: A10 owns `resource_packs/`. This dossier records
  the pack format (6) and the block list that matters to the map.
- Cross-map ranking is A13's. `README.md` §6 gives only the on-its-own-terms position.
- No 1.21 test load was performed; the 1.21 notes are static analysis of syntax + NBT (H/M) and
  must be confirmed by A12.

## Cross-checks

| Source | Finding | Status |
|---|---|---|
| A12 server verification (2026-11-25) | 29/29 commands parse on the live 1.21 server — **no syntax failures** | Matches my static check (§7) |
| A12 server verification | **2 global `kill @a` planes** can kill the lobby from one player's trigger | Verified independently here: F13, `data/command_blocks.csv` rows `21,112,16` and `23,112,16` |
| A12 server verification | Live world runs **survival** with **`allow-flight=false`** | Effects documented in `README.md` §9 |

A12's kill-plane finding is the same pair I had logged as "kill" commands; it is now documented as
a **global** hazard, with standable-area counts (6,090 lime-glass columns, 1,852 bedrock columns)
and the respawn interaction. No fixes applied.

## Open questions

1. **Elytra ring intent vs implementation.** The grant tests a horizontal neighbour
   (`~ ~ ~-1…-7`), not the block below. As saved, it cannot fire in flight. Did the author's build
   differ, or was it only ever tested in creative? (H command text; L on history.)
2. **Exact finish line.** The pod is captioned "StartFinish Line" and the scoreboard has
   `LapCheck1`, but nothing detects a crossing. The right-chain bottom island is the alternative
   podium reading (M).
3. **Missing timing datapack.** `Minutes`, `Seconds`, `milliseconds`, `LapCheck1`, `Restart` exist
   with stale scores (4:44, 5:59) but no writer and no datapack folder. Probably stripped before
   download (L).
4. **Rain survival on 1.21.** Riptide needs rain or water; `doWeatherCycle=true`. If the import
   clears weather, some sections change difficulty (L, A12).
5. **The 3 empty-looking chain blocks** at `11,111,*` do hold the subtitles, but some have no
   `LastOutput`. Consistent with a WorldEdit paste (L).
6. **`allow-flight=false` vs levitation.** A12 reports the live server has flight disabled. Creative
   players are exempt; survival players on levitation pads / long falls may trip the anti-flight
   kick. Needs an in-game test on the orange pad and the Ring D return (L; A12).

## Acceptance checklist (PLAN §4)

- [x] Every command block accounted for (29/29; 22 repeating, 4 impulse, 3 chain)
- [x] Every container opened/inventoried — there are none (0), proven by full chunk scan
- [x] Route can be followed from the report alone (`data/route.md` + overlay)
- [x] At least 6 full-resolution images (19 renders, 9 frames)
- [x] Explicit better/worse section (on its own terms; cross-map deferred to A13 per brief)
- [x] No claim without H/M/L
- [x] `data/command_blocks.csv`, `signs.csv`, `blocks.csv`, `checkpoints.csv`, `entities.csv`,
      `palette.md`, `route.md`
- [x] `README.md`, `REFERENCE_IMAGES.md`, `STATUS.md`, `frames/`, `data/`, `renders/`

## Reproduce

```
python scripts/scan_te.py            # chunk list, tile entities, entities  (~3 min)
python scripts/count_blocks.py       # numpy block counter                  (~2 min)
python scripts/dump_blocks.py        # every block with coords -> data_raw  (~8 min)
python scripts/export_data.py        # writes data/*.csv
python scripts/route_overlay.py      # labelled route PNG
python scripts/dump_mcmdump.py ...   # render inputs (same MCMDUMP format)
python C:\Users\LeonS\Desktop\Mcc\tools\render_preview.py ...   # read-only shared renderer
```

Shared tools were not modified. `scripts/dump_mcmdump.py` is a NumPy reimplementation of
`tools/world_to_mcmdump.py`'s output format because the shared per-block loop exceeds 10 minutes on
this 14,198-chunk world; the shared tool is left untouched and the format is identical
(`MCMDUMP 1 sx sy sz` + `x y z id`).
