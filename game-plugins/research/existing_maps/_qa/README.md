# A14 — Post-fix re-audit of the existing-maps dossiers

**Status:** complete. **Date:** 2026-11-25. **Scope:** targeted re-audit of the previously raised P0/P1/P2 items and the changed files only. **Writes:** `game-plugins/research/existing_maps/_qa/` only.

**Verdict:** all **P0 and P1 gaps are closed**. No folder fails. Ten owners pass; six pass with only P2 residuals. No fixes were applied by A14.

---

## 1. What changed in this pass

Checked the fixes made around 07:10–07:31:

1. **A6 top level:** `data/containers.csv` has **34 rows**; every `data/arenas.csv` row has a `containers` value; the 34/21/13 statement is in README and STATUS; the Parkour gallery/waiting-lobby and countdown coordinates are corrected; Orange is lower/flatter and not a Reskin; the wool/shears signs are no longer “leftover”; “Complete” is softened; duplicate-image accounting is explicit.
2. **A6a:** 19 container block entities = 13 loot/93 items + 5 campfires + 1 lectern; `command_blocks.csv` is a 477-row SHA-256-identical alias of `commands.csv`; the old A6 correction notes are withdrawn.
3. **A6b:** 3 container block entities = 1 barrel + 2 lecterns (each holds a guide book, no Items); `command_blocks.csv` is a 123-row SHA-256-identical alias; the old A6 note is corrected.
4. **A6c:** 12 containers in `data/containers.csv`; `containers` column in `other_arenas.csv`; phantom correction rows removed; `palette.md` and `route.md` added; global 34/21/13 statement present.
5. **A5:** 0 of 12 zips active (all disabled + 1 unzipped folder); source spawn `0,4,0` vs live `0,76,0` stated.
6. **A4:** 6 container block entities (3 shulkers + hopper + 2 campfires); pack path base defined as `pack/extracted/assets/minecraft/`.
7. **A2:** missing station image replaced with existing `renders/station_interior_iso.png`; scoreboard wording now cites `data/scoreboard.dat`.
8. **A12:** Space Race pack described as one zip with a wrapper folder, not a nested zip.
9. **A13:** Space `@p` correction; Orange-vs-Red row in §8.2; 18-column matrix with `primary_sources`; route links fixed; creeper marked video-only; Parkour Tag §4.11; checkpoint wording softened; simulation-distance claim scoped; Yeti count 6.
10. **External `acerace/presentation/README.md`:** S5 now records `Starting In` + 13/12/11, the action bar/custom HUD, and a montage caveat.

## 2. Method

- Re-read every changed file and the four Minigames subfolders.
- Re-ran the independent `nbtlib` container scan; the fixed A6 34-row table matches source row-for-row (same x/y/z/block/items).
- Re-ran the command CSV row counts and SHA-256 alias checks.
- Re-ran the image citation audit after the changed REFERENCE/README files; opened the changed atlas, A6a frame 17, A2 station cutaway and A6b route schematic.
- Re-checked the A13 matrix (4×18) and resolved all 12 `primary_sources` paths.
- Re-checked the external S5 row and montage caveat.
- No live server/RCON test was run by A14; A12's read-only baseline remains authoritative for live state.

## 3. Final verdict by agent

| Agent | Folder | Verdict | Basis |
|---|---|---|---|
| A1 | `clouds2` | **PASS** | 322 commands / 285 signs / 225 empty shulkers / 150,414 blocks / 31 failures all reconcile; 41 cited images, 0 missing. P2: `frames/` has 2 JPGs, but 51 renders are the evidence set. |
| A2 | `city_marathon` | **PASS** | 67 commands / 156 containers / 7 signs / 207,440 blocks / 7 failures; station image fixed; scoreboard evidence fixed; 40 cited images, 0 missing. |
| A3 | `space_race` | **PASS** | 29 commands / 0 containers / 22 signs / 296,699 blocks / 0 syntax failures; 37 cited images, 0 missing. |
| A4 | `yeti_set_go` | **PASS** | 37 commands / 14 signs / **6** container BEs (4 storage + 2 campfires) / 28,533 blocks / 3 failures; pack base fixed; 36 cited, 0 unresolved. |
| A5 | `decision_dome_recreation` | **PASS** | 2 commands / 45 TEs / 20,054 blocks; **0 active packs**; source/live spawn distinction explicit; 29 cited, 0 missing. |
| A6 | `minigames` | **PASS WITH FIXES** | 34-row container table, all arena columns, 34/21/13 statement, corrected Parkour/Orange/signs/Complete; residual: atlas panel 07 still has the old Orange label and container provenance cites the QA scan. |
| A6a | `minigames/sky_battle` | **PASS WITH FIXES** | 19 container BEs (13 loot/93 items + 5 campfires + 1 lectern); 477-row alias hash-identical; stale correction notes withdrawn. Residual: container source column points to `_qa`. |
| A6b | `minigames/parkour_tag` | **PASS WITH FIXES** | 3 container BEs (1 barrel + 2 guide-book lecterns); 123-row alias hash-identical; old A6 note corrected. Residual: container source cites `_qa`. |
| A6c | `minigames/other` | **PASS WITH FIXES** | 12 containers, filled column, no phantom rows, `palette.md`/`route.md` added, global 34/21/13 statement. Residual: container rows cite `_qa`; atlas note. |
| A7 | `tgttos_badlands` | **PASS** | 0 commands / 0 containers / 1 sign / 411,834 blocks; 43 cited, 0 missing. |
| A8 | `tgttos_industry` | **PASS WITH FIXES** | 0 commands / 0 containers / 122,276 blocks; 24 cited, 0 missing. Residual P2: `frames/` has 4 images, but 41 renders meet the image rule. |
| A9 | `tgttos_pits` | **PASS** | 0 commands / 0 containers / 1 sign / 164,145 blocks; 32 cited, 0 missing. |
| A10 | `resource_packs` | **PASS** | 3 packs / 71 textures / 455 commands / 387 containers (225+156+6); Yeti count now aligned with A4/A13; 18 cited, 0 missing. |
| A11 | `video_clouds2` | **PASS** | 21 frames / 48 cuts / exact HUD; 27 cited, 0 missing; external S5 corrected. |
| A12 | `server_import` | **PASS** | 455 commands / 41 failures / 9 worlds / 3 packs; wrapper-folder wording fixed; no images by design. |
| A13 | `comparison` | **PASS WITH FIXES** | 4×18 matrix, `primary_sources` 12/12, Space `@p`, Orange row, Yeti 6, creeper video-only, §4.11, checkpoint wording, sim scope. Residual: two broken prefix paths in `contradiction_resolution.md`; §8.2 rows still quote stale A6 wording. |

**Counts:** 10 PASS, 6 PASS WITH FIXES, **0 FAIL**.

## 4. Residual P2 list

1. **A6 atlas panel 07** still says “same layout as Red, orange palette”; A6 README/A6c text is corrected. Regenerate or annotate the atlas.
2. **A13 `data/contradiction_resolution.md:76`** cites `frames/srred_iso.png` / `srorg_iso.png` without the `../../minigames/other/` prefix; the files exist there.
3. **Container provenance:** A6a `containers.csv`, A6b STATUS and A6c README/containers cite `_qa/data/minigames_container_by_arena.csv`; A6 `data/containers.csv` has no source path. A6c's data-folder evidence path `../../_qa` is off by one level (should be `../../../_qa`). Counts match the source scan, but add a source-region/NBT path if strict provenance is required.
4. **A1/A8 `frames/` remain thin:** 2 and 4 files respectively; both have 41/51 renders and enough cited full-resolution images, so this is packaging only.
5. **A13 §8.2** still quotes “A6 says same layout/leftover” in present tense although A6 text is corrected; mark the rows historical or point at the stale atlas label.
6. **A6 parent `frames/` duplicate A6a/A6b frames:** A6 declares its own frame copies but not the parent/subfolder duplication; add one line to `REFERENCE_IMAGES.md`.

No residual is P0 or P1. No report claims loot that is not in source NBT: the 13 Sky Battle loot containers still match **93/93** entries; all other 21 Minigames containers are empty; Clouds/City/Yeti remain empty.

## 5. Re-run counts

| Check | Result |
|---|---|
| Clouds 2 commands / shulkers / signs | 322 / 225 empty / 285 |
| City Marathon commands / containers / signs | 67 / 156 empty / 7 |
| Space Race commands / containers | 29 / 0 |
| Yeti commands / container BEs | 37 / 6 (4 storage + 2 campfires) |
| Minigames commands / signs / blocks | 942 / 187 / 133,030 |
| Minigames containers | 34 (21 empty, 13 loot/93 items) |
| A6a loot | 13 containers, 93/93 NBT match |
| A6b containers | 3 (1 barrel + 2 lecterns) |
| A6c containers | 12 (hub 8, SC 3, SR hub 1) |
| A12 failures | 41 = 30 + 1 + 7 + 3 |
| A13 matrix | 4 data rows × 18 columns; 12/12 source paths resolve |

## 6. Image audit summary

- **417** citation rows after excluding rejected drafts; **407** distinct paths.
- **414** resolve: 411 directly, 3 through A4's documented `pack/extracted/assets/minecraft/` base.
- **2** do not resolve as written: A13 `contradiction_resolution.md` uses `frames/srred_iso.png`/`srorg_iso.png` without the `minigames/other/` prefix (files exist).
- **1** is a fix-log quote, not a live citation: `comparison/STATUS.md:45` quotes the old `renders/route_labeled.png` while describing the fix.
- **47 distinct images opened** (55 citation rows), including the regenerated atlas, A6a frame 17, A2 station cutaway and A6b route schematic.
- **56 duplicate groups**, all within the same world/folder family; the large sets are declared frame↔render copies. A6 now marks its 12 frame copies and 14 distinct views; A6c/A3/A9 also declare copies.

## 7. Process / integrity

- No writes outside `_qa/`; no code, world, server, tool, jar, plugin or config edit.
- No evidence of shared-tool or game-code modification in this pass; prior mtime evidence still stands. `existing_maps/` remains untracked, so per-file git provenance is unavailable.
- No live RCON/server run; A12's live observations remain the baseline.

## 8. Deliverables

| File | Contents |
|---|---|
| `README.md` | this re-audit, fix verification, final verdicts, residual P2 list |
| `GAPS.md` | closed P0/P1/P2 table + six residual P2 gaps |
| `data/acceptance.csv` | 16 agents, required files, counts, image checks, H/M/L, final verdict |
| `data/claim_checks.csv` | 41 claims, source, status, evidence |
| `data/image_audit.csv` | 417 citations, existence, dimensions, opened, caption match |
| `data/count_checks.csv` | 59 numeric checks, all PASS after fixes |
| `data/container_source_scan.csv` | independent NBT scan |
| `data/minigames_containers.csv` | all 34 containers, coords/items |
| `data/minigames_container_by_arena.csv` | 34 containers assigned to arenas |
| `data/image_duplicates.csv` | hash-level duplicate groups |
| `STATUS.md` | done / residual / open questions |
