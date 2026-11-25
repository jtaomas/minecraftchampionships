# GAPS — post-fix re-audit

Updated: 2026-11-25 after the targeted fixes. Scope: read-only; **no fixes applied** by A14.

**Status: no active P0 or P1 gaps. All previously reported P0/P1 findings are closed.** Six P2 residuals remain; none blocks acceptance.

---

## Closed in this pass

| ID | Original gap | Verified fix |
|---|---|---|
| P0-1 | A6/A6c claimed 14 containers where the source has 34 | `minigames/data/containers.csv` has **34 rows** (11 barrels, 6 campfires, 6 chests, 5 lecterns, 6 shulkers; 21 empty, 13 loot/93 items); all 10 `arenas.csv` rows have a `containers` value; A6 reports 34/21/13 and 34 per arena. |
| P1-1 | A5 claimed 11 active VanillaTweaks packs | A5 now says **0 of 12 zips active** (12 disabled + 1 unzipped folder) and distinguishes source `0,4,0` from live `0,76,0`. |
| P1-2 | A6 top-level Parkour/Sky Race text was stale | Waiting lobby/countdown/gallery corrected; Orange is lower/flatter and not a reskin; signs match the loadout. See residual P2-R1 for the atlas image label. |
| P1-3 | A13 said all four Ace Race maps use `@a` pad actions | A13 now says Clouds/City/Yeti pads use `@a`; Space levitation/speed pads use `@p`, with `@a` elytra clear and kill planes. |
| P1-4 | Old `acerace/presentation/README.md` S5 row said no countdown/HUD | S5 now records `Starting In` + 13/12/11, the persistent action bar and custom HUD; a montage caveat was added. |
| P1-5 | A13 did not resolve the A6/A6c Orange contradiction | A13 §8.2 now has an Orange-vs-Red row; A6 top-level text is corrected. |
| P2-1 | Yeti container count 4 vs 6 | A4 `containers.csv` now has **6** rows (3 shulkers + hopper + 2 campfires); A13 matrix says 6; A10 already said 6. |
| P2-2 | A6c phantom corrections | A6c removed the CMD/hub/Super Clash/no-container rows; §4 now has only the two real resolved findings. |
| P2-3 | A6a/A6b stale A6 correction notes | Both mark the old “0 containers”/“161 fill” rows as **withdrawn**; active text uses 19/3 and the current A6 counts. |
| P2-4 | A13 matrix lacked per-row sources | Added `primary_sources` as column 18; **12/12 paths resolve** from `comparison/data/`. |
| P2-5 | A13 said parts of every course are dead at sim 6 | Scoped to Clouds 2 and Yeti; Space's command room under the start + `keep-spawn-in-memory` is now stated. |
| P2-6 | A5 presented source spawn as live spawn | A5 now states source `0,4,0` vs live `0,76,0` explicitly. |
| P2-7 | A12 called the Space Race pack a “double zip” | A12 now says **one zip with a wrapper folder**, not a zip-inside-zip. |
| P2-8 | City cited missing `renders/station_iso.png` | A2 now cites existing `renders/station_interior_iso.png`; the old path is gone. |
| P2-9 | City scoreboard evidence cited `level.dat` | A2 now cites the absent `data/scoreboard.dat`; conclusion unchanged. |
| P2-10 | A13 comparison route path shorthand | Live links now use `../clouds2/renders/route_labeled.png`; STATUS line 45 is only a fix-log quote. |
| P2-11 | Yeti pack texture paths omitted prefix | A4 `README.md:254` defines the base as `pack/extracted/assets/minecraft/`; the three files resolve there. |
| P2-12 | A6a/A6b lacked `command_blocks.csv` | Both now have the required-name file; 477-row and 123-row aliases are SHA-256 identical to `commands.csv`. |
| P2-14 | A13 attributed the giant creeper to Clouds 2 | Marked **video-only, unconfirmed** in the downloaded world. |
| P2-15 | A13 cited Parkour Tag §4.8 | Corrected to §4.11. |
| P2-16 | A13 “111 ordered”/“10 checkpoint” wording | Changed to `route-ordered` and `start/gate/soft-respawn records`. |
| P2-17 | A6 “Complete” labels overclaimed | Changed to “build complete; loop has known defects” with A6a/A6b/A6c pointers. |
| P2-18 | Duplicate image totals | A6 marks 12 frame copies / 14 distinct views; A6c and A3/A9 also declare copies. See residual P2-R6. |
| P2-19 | A6c lacked `palette.md` / `route.md` | Both added; `route.md` covers all eight arenas, `palette.md` has per-arena block counts and roles. |

---

## Residual P2 gaps

### P2-R1 — A6 atlas panel 07 still says Orange is the same layout as Red

**Evidence:** opened `minigames/renders/arena_atlas.png`; panel 07 caption still reads “same layout as Red, orange palette”. A6 `README.md:137` and `REFERENCE_IMAGES.md:18` correctly say Orange is lower/flatter and **not** a reskin; A6c `REFERENCE_IMAGES.md:28` acknowledges the atlas label is stale.

**Fix:** regenerate `arena_atlas.png` with the corrected caption, or add an explicit note in A6 `REFERENCE_IMAGES.md` row 13 that the panel 07 caption is superseded. No other claim needs changing.

### P2-R2 — A13 `contradiction_resolution.md` has two broken relative image paths

**Evidence:** `comparison/data/contradiction_resolution.md:76` cites `frames/srred_iso.png` and `frames/srorg_iso.png`; from `comparison/data/` these do not exist. The actual files are `minigames/other/frames/srred_iso.png` / `srorg_iso.png`.

**Fix:** use `../../minigames/other/frames/srred_iso.png` / `srorg_iso.png`.

### P2-R3 — Container provenance now cites the QA artifact, not original NBT

**Evidence:**
- `minigames/sky_battle/data/containers.csv` has `source=../../../_qa/data/minigames_container_by_arena.csv` on all 19 rows (correct from that folder).
- `minigames/other/data/containers.csv` has `evidence=A14 ../../_qa/data/...` on all 12 rows; from `minigames/other/data/`, that resolves to `minigames/_qa`, not `existing_maps/_qa` (off by one level; should be `../../../_qa`).
- `minigames/README.md:30` / `data/containers.csv` give counts but no source-region column.
- A6b `STATUS.md:22` and A6c `README.md:13`/`:52` cite A14's scan.

**Impact:** numbers match the independent source scan, so this is a traceability issue, not a count error. **Fix:** add the source region/mca or NBT-scan path, or label the rows “A14 source scan; see `_qa/data/container_source_scan.csv`” and keep the original scan script.

### P2-R4 — A1 and A8 `frames/` are still thin

**Evidence:** `clouds2/frames/` has 2 JPGs; `tgttos_industry/frames/` has 4 PNGs. A1 has 51 renders and 41 cited images; A8 has 41 renders and 24 cited images, so the ≥6-image acceptance rule is met outside `frames/`.

**Fix:** add ≥6 actual frames per map or state explicitly that `renders/` is the full-resolution evidence set and `frames/` is a convenience subset. No factual content is missing.

### P2-R5 — A13 §8.2 still quotes stale A6 wording as present tense

**Evidence:** `comparison/README.md:212-217` says “A6 says Sky Race wall signs are ‘leftover template text’” and “A6 index says … ‘the same layout’”. A6's current README says the opposite; only the atlas panel 07 image still carries the old Orange wording.

**Fix:** mark the two rows “historical — A6 text corrected; see P2-R1 for the stale atlas label”, or point the evidence at the actual current text.

### P2-R6 — A6 parent `frames/` still duplicate A6a/A6b frames

**Evidence:** `image_duplicates.csv` has 19 `minigames` groups; e.g. `minigames/frames/sky_battle_iso.png` is byte-identical to `minigames/sky_battle/frames/03_iso_angle0.png`. A6 `REFERENCE_IMAGES.md:7` declares its 12 frames as copies of its own renders, but not the parent/subfolder duplication. A6a/A6b/A6c also declare frame/render copies.

**Fix:** add one line to A6 `REFERENCE_IMAGES.md` that parent `frames/` are convenience copies shared with the sub-arena folders; do not count them as independent evidence.

---

## Checks that still pass

- **Command counts:** 322 + 67 + 29 + 37 = **455**; A12's **41** failures reconcile (30 item-NBT + 1 malformed setblock + 7 City + 3 Yeti).
- **Containers:** Clouds 225 empty; City 156 empty; Yeti 6 (4 storage + 2 campfires); Space 0; Decision 0; TGTTOS 0/0/0; Minigames 34 (21 empty, 13 loot/93 items).
- **Images:** 417 citation rows, 407 distinct; 414 resolve (411 direct + 3 via A4's documented pack base); 2 missing-prefix paths (files exist), 1 fix-log quote. 47 distinct images opened (55 citation rows).
- **A13 runtime:** all 13 capabilities still covered; matrix is 4×18 with `primary_sources`; 12/12 source paths resolve.
- **External:** `acerace/presentation/README.md` S5 now has countdown/HUD + montage caveat.
- **Process:** no writes outside `existing_maps/_qa`; no shared tool, game code, jar, server or world changes detected.

---

## Final agent verdicts

| Verdict | Agents |
|---|---|
| **PASS** | A1, A2, A3, A4, A5, A7, A9, A10, A11, A12 |
| **PASS WITH FIXES** (P2 residuals only) | A6, A6a, A6b, A6c, A8, A13 |
| **FAIL** | none |
