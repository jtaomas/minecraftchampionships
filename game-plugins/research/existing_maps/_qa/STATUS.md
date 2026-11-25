# STATUS — A14 post-fix QA

Updated: 2026-11-25. **State: COMPLETE.** No fixes applied by A14; read-only outside `_qa/`.

## Final result

- **0 FAIL.**
- **10 PASS:** A1, A2, A3, A4, A5, A7, A9, A10, A11, A12.
- **6 PASS WITH FIXES** (P2 residuals only): A6, A6a, A6b, A6c, A8, A13.

All previously raised **P0/P1** gaps are closed:
- Minigames containers: 34/21/13; `data/containers.csv` 34 rows; all arena columns filled; A6a 19, A6b 3, A6c 12.
- A5: 0 active packs; 12 zips disabled + 1 folder; source vs live spawn explicit.
- A6 top-level Parkour/Orange/signs/Complete corrected.
- A13 Space `@p`, Orange row, `primary_sources`, Yeti 6, creeper, §4.11, checkpoint wording, sim scope.
- A2 image/scoreboard, A4 6 containers/pack base, A12 wrapper wording, external S5 row.

## Re-run checks

- 455 command blocks: 322 + 67 + 29 + 37; 41 A12 failures reconcile.
- Containers: Clouds 225 empty; City 156 empty; Yeti 6 (4 storage + 2 campfires); Space 0; Decision 0; TGTTOS 0/0/0; Minigames 34 (21 empty, 13 loot/93 items).
- A6 34-row table matches the independent source scan row-for-row.
- A6a 477-row alias and A6b 123-row alias are SHA-256 identical to `commands.csv`.
- A13 matrix: 4×18; 12/12 `primary_sources` paths resolve.
- Images: 417 citation rows, 407 distinct; 414 resolve (411 direct + 3 via documented Yeti base); 2 missing-prefix citations; 1 fix-log quote; 47 distinct images opened (55 rows).

## Residual P2 gaps

1. A6 `renders/arena_atlas.png` panel 07 still has the old Orange “same layout” label.
2. A13 `data/contradiction_resolution.md:76` cites `frames/srred_iso.png`/`srorg_iso.png` without `../../minigames/other/`.
3. Container provenance in A6a/A6b/A6c/A6 cites the `_qa` source scan rather than original NBT/region paths; A6c's `data/containers.csv` path `../../_qa` is off by one level (should be `../../../_qa`).
4. A1 `frames/` has 2 JPGs and A8 has 4 frames; both have enough renders/cited images.
5. A13 §8.2 still quotes stale A6 wording in present tense.
6. A6 parent `frames/` duplicate A6a/A6b frames; add a duplicate note.

## Blocked

None.

## Open questions / limits

1. **Container definition:** A4/A10/A13 now agree on 6 for Yeti (4 storage + 2 campfires); A6/A6a/A6b/A6c use broad block-entity counts. This is consistent but should remain explicit.
2. **Live parity:** no RCON/server run by A14; A12 is the live baseline. Source vs live differences are documented.
3. **Image sampling:** 47 distinct images opened; remaining distinct paths were verified for existence/dimensions/readability, not all visually caption-checked.
4. **Git provenance:** `existing_maps/` is untracked; mtime evidence and scope statements are the available provenance.

## Files

```
_qa/
  README.md
  GAPS.md
  STATUS.md
  data/
    acceptance.csv
    claim_checks.csv
    image_audit.csv
    count_checks.csv
    container_source_scan.csv
    minigames_containers.csv
    minigames_container_by_arena.csv
    image_duplicates.csv
    image_audit_raw.csv
  scripts/
    image_audit.py
    build_image_audit.py
    verify_containers.py
    list_minigame_containers.py
    build_count_checks.py
    build_tables.py
    build_final_tables.py
```

No writes outside `_qa/`.
