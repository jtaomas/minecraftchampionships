# City Marathon — status

**Agent:** A2 · **Folder:** `game-plugins/research/existing_maps/city_marathon/`
**State:** research complete; no code, no build, no server changes.
**Source:** `City Marathon` 1.19.2 · 6,374 chunks · 67 command blocks · 140 shulkers · 84 banners · 7 signs · 39 item frames.

## Done

- [x] Read `PLAN.md`; confirmed A2 scope (E2 standard).
- [x] Read-only NBT scan of 72 region files: block counts, block entities, entities, playerdata, `level.dat` — H.
- [x] Extracted all **67 command blocks** with exact text, coords, mode, `auto`, conditional, `LastExecution`, `SuccessCount`, custom name — H.
- [x] Built `data/command_blocks.csv` (67 rows) with trigger/effect/reset/1.21 risk and A12 cross-ref.
- [x] Inventoried all **156 containers** (140 shulkers, 12 barrels, 3 campfires, 1 blast furnace) — all empty — H.
- [x] Inventoried **84 banners**, **7 signs**, **39 item frames** — H.
- [x] Built `data/blocks.csv` (197 types, 207,440 non-air blocks) and `data/palette.md`.
- [x] Built walkability graph (65,286 reachable cells) and proved the six rooftop elytra pads are unreachable — H.
- [x] Reconstructed the main 1,352-block clockwise circuit and eight alternates/skips — M for intent, H for coordinates.
- [x] Rendered 2 full isos, full top-down, ground-level layout, route overlay, station/train/park/plaza/roof/highway crops, and 10 annotated frames — H geometry.
- [x] Opened every cited image (10 frames + 25 renders) — H.
- [x] Wrote `README.md`, `REFERENCE_IMAGES.md`, `data/route.md`, `data/palette.md`, `data/upgrade_1_21.md`, `data/server_verification.md`.
- [x] Cross-referenced A12's 7 no-target `/gamemode adventure` failures with exact coordinates and Multiverse survival implication — H/A12 + M implication.

## Key numbers

| Metric | Value | Evidence |
|---|---:|---|
| Chunks | 6,374 | H |
| Non-air blocks | 207,440 | H |
| Block types | 197 | H |
| Command blocks | 67 (60 repeating, 5 impulse, 2 chain) | H |
| Containers | 156 (all empty) | H |
| Shulkers | 140 (115 roof AC + 21 vertical stack + 4 gate posts) | H |
| Banners | 84 | H |
| Signs | 7 | H |
| Item frames | 39 (25 normal + 14 glow) | H |
| Reachable ground cells | 65,286 | H BFS |
| Main line length | closed loop ≈1,352; gate-to-gate ≈1,400 | H arithmetic |
| A12 command failures | 7 (all no-target gamemode) | H |

## Known defects (observed, not fixed)

1. **7 × no-target `/gamemode adventure`** fail (`permissions.requires.player`): (11,−2,−6), (11,−2,−4), (7,−3,3), (−387,−8,45), (−387,−8,47), (−208,−2,86), (−208,−2,87) — **H**.
2. **Release gate closed at save:** 18 barrier blocks at z10 remain; the `/fill` has SuccessCount=1 but the world was saved with the gate rebuilt — **H**.
3. **Countdown missing “3”:** the `>3<` chain blocks never fired; only `Starting In`, `>2<`, `>1<` ran — **H**.
4. **Dead yellow-carpet logic:** 3 commands check `yellow_carpet`, but 0 yellow_carpet blocks exist — **H**.
5. **Elytra pads unreachable and mostly non-matching:** 6 rooftop pads at y28; only E1 has polished diorite (E2–E6 are polished andesite, so their predicate cannot fire); no access; no fireworks/rain/height gain — **H/M**.
6. **No finish/lap/timer:** crossing the gate does nothing; no scoreboard — **H**.
7. **Debug stick left in playerdata hotbar 9** — **H**.
8. **Hard-coded actionbar “You are in 1st place”** shown to all players every tick — **H**.
9. **Survival-mode risk under Multiverse** if the per-world gamemode is survival — **M**.
10. **Station concourse is largely empty grey floor**; route through it is unmarked — **H**.

## Open questions

1. Intended **direction and lap count** — inferred clockwise single lap; no code enforcement (M/L).
2. Were the **elytra roofs** meant to be reached by removed stairs/elevators, or are they landing-only? (H no access; L intent).
3. Purpose of the **21-shulker vertical stack** at x−12,z49 (H empty; L intent).
4. Purpose of **28 stadium-seat buttons** at x−445..−397,z85/91 and **15 park buttons** at x−30/−24,z82..94 — no nearby command blocks/note blocks (H; L intent).
5. Live **Multiverse per-world gamemode**: `mcc_citymarathon` is not in the visible `worlds.yml`; A12 must confirm (H read; open).
6. Is the **train roof** the main line or a skip? Stairs/roof pads are deliberate, but the direction conflicts with the westbound station approach (H geometry; M/L intent).
7. Why do the **NO ENTRY** signs face east at the park gate, while the drawn main line enters westbound? (H facing; L intent).
8. Was the map ever played beyond the one start-circuit test at tick ~110,295? (H LastExecution; L).

## Blocked

- None for the dossier. Live-server RCON queries were intentionally left to A12; no RCON was used here (plan rule).
- Cross-map comparison is **out of scope** for A2; A13 owns it.

## Handoff notes

- Use `data/route.md` + `frames/01_overview_route.png` for the route; use `data/command_blocks.csv` for exact commands.
- Do not copy from other map reports; this dossier is self-contained.
- Do not edit `tools/` or other agents' folders; all A2 scripts are under `scripts/` and write only in this folder.
- If A12 confirms survival mode, record it in `data/server_verification.md`; do not change the world.
- If the coordinator needs a single sentence: **“A handsome, legible city with a strong station/park set piece, but only a half-finished race: no finish/lap logic, seven broken gamemode commands, empty containers, and an unreachable elytra layer.”** — M.
