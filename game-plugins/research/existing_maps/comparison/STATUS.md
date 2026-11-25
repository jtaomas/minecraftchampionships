# STATUS — A13 cross-map synthesis

Updated: 2026-11-25, A14 fixes applied. **Research complete.** No game/plugin code, no build, no paste, no
registry edit, no server restart, no writes outside `game-plugins/research/existing_maps/comparison/`.

## Done

- [x] Read `PLAN.md` and all 15 completed report folders: `clouds2/`, `city_marathon/`, `space_race/`,
      `yeti_set_go/`, `decision_dome_recreation/`, `minigames/` + `sky_battle/` + `parkour_tag/` + `other/`,
      `tgttos_badlands/`, `tgttos_industry/`, `tgttos_pits/`, `resource_packs/`, `video_clouds2/`,
      `server_import/`.
- [x] Read the source `AceRace` README/SPEC read-only to ground the runtime gap; no code changed.
- [x] Built the four-row Ace Race matrix (18 columns) and validated it parses as CSV with a constant
      18-field width (`python csv.reader`: 5 rows × 18 fields). `primary_sources` cites paths + sections for
      every row.
- [x] Wrote the adopt / adapt / reject synthesis by quality: route legibility, arrows/signage, landmark
      sequencing, pad placement, elytra/trident, start/finish, palette/lighting, compactness, skyline,
      containment.
- [x] Wrote the 13-capability runtime gap, including the 40-player contract and the simulation-distance
      consequence.
- [x] Wrote the non-Ace-Race takeaways: TGTTOS portability, Sky Battle loot/teams/reset, Parkour Tag role
      pads/timer, Decision Dome playable-vs-display comparison.
- [x] Resolved the Super Clash contradiction: **`kill_sc_vp`**, criterion `teamkill.green`; `kill_sc_vip` has
      zero occurrences in the source reports (it appears in this comparison only as the quoted error). Full
      chain in `data/contradiction_resolution.md`.
- [x] Resolved the Sky Race Red/Orange contradiction: A6c is correct; A6's "same layout" is superseded. Red
      y13–82 with 98 chains/cloud props; Orange y13–54 with 24 cobwebs/no chains. Added to
      `data/contradiction_resolution.md` §2 and README §8.2.
- [x] Built `renders/comparison_sheet.png` from eight existing report renders; opened every source image
      before citing it. No map was re-rendered.
- [x] Recorded the duplicate archive: `zipped-map.zip` is byte-identical to `YetiSetGo.zip`; one row only.
- [x] Recorded other report-vs-report disagreements checked (Sky Race signs, allow-flight layers, stale
      PLAN counts, Yeti lap reading, Space finish, video vs world countdown).

## A14 fixes applied (2026-11-25)

1. **Space Race pad targeting.** Corrected the blanket "all four maps use `@a` actions": Space's levitation
   and speed pads use `effect give @p`; its elytra clear and two kill planes use `@a`. Clouds 2, City and
   Yeti pads use `@a`. Fixed in README §1/§4/§7, the matrix Space cell, `what_works.md` and `runtime_gap.md`.
2. **Orange vs Red.** Added `data/contradiction_resolution.md` §2: Red y13–82 with 98 chains + grey cloud
   props; Orange y13–54 with 24 cobwebs and no chains. A6c is correct; A6's "same layout" is superseded.
   Also added a row to README §8.2.
3. **Primary sources.** Added an 18th `primary_sources` column to the matrix, with dossier paths and section
   references for all four rows.
4. **Route path shorthand.** Fixed `renders/route_labeled.png` to `../clouds2/renders/route_labeled.png` in
   README §3 and `what_works.md` §0.
5. **Giant creeper.** Marked as video-only/unconfirmed in the downloaded world (`what_works.md` §3), along
   with the other video-observed decoration noise.
6. **Parkour Tag cross-reference.** Corrected `§4.8` to `§4.11` in `non_ace_takeaways.md` §3.
7. **Checkpoint wording.** Changed "111 ordered" to "111 route-ordered pad/checkpoint records" and "10
   checkpoint/gate" to "10 start/gate/soft-respawn records"; kept the runtime warning that neither map
   enforces order.
8. **Simulation-distance scope.** Removed Space Race from the "parts of every course are dead" claim;
   Space's command room sits under the start and `keep-spawn-in-memory: true` keeps it loaded. Clouds 2 and
   Yeti remain the failing cases.
9. **Yeti containers.** Aligned with A4/A10 broad definition: 6 block entities (3 shulkers + 1 hopper + 2
   campfires), 4 storage containers, all empty.

## Not done (deliberately)

- No live server run, no RCON, no playtest. All map facts are read from the dossiers; this agent did not
  re-scan region files.
- No build/paste of any map or template; no plugin code; no resource-pack creation.
- No independent re-analysis of a whole map. The only direct checks were: report text, report CSVs,
  contradiction grep, CSV parse, and the eight images used in the sheet.
- No QA of other agents' folders. A14 owns that.

## Open questions / weakest claims

1. **Pacing is L.** No Ace Race map has a timer; the only video is a single-lap fan montage with no race
   HUD. The 3-lap / 60–90 s per lap target is a design judgement, not measured.
2. **Yeti lap reading is M/L.** Start=finish and the return ledge argue for a lap; point-to-point at the
   station or north tip remains possible (`../yeti_set_go/README.md` §10).
3. **Space finish location is L.** The pod is captioned "StartFinish Line" and has `LapCheck1`, but nothing
   detects a crossing; the right-chain bottom island is the alternative.
4. **Clouds 2 lap count is assumed 1.** No command counts laps; the build is a closed loop.
5. **`kill_sc_vp` effect is M.** The criterion/team mismatch is H; whether the villain side truly cannot
   reach 10 needs an in-game scoreboard check.
6. **Simulation-distance coverage is arithmetic, not a live test.** The 6/8 setting and bank distances are
   H; the per-section consequence is M.
7. **The comparison sheet is a communication aid, not evidence.** The panels have different scales and
   render styles; use the linked reports for measurements.

## Verification log

| Check | Result | Evidence |
|---|---|---|
| All 15 report READMEs present | yes | `Get-ChildItem -Recurse -Include README.md,STATUS.md` |
| Matrix rows / columns | 4 data rows × 18 fields | `python -c "csv.reader(...)"` |
| `primary_sources` resolution | 12/12 paths resolve from `data/` | Python path check |
| `kill_sc_vip` in source reports | **0** (excludes this comparison's quoted error) | literal `Select-String` over the source dossiers before writing |
| `kill_sc_vp` sources | 6+ | scoreboard CSV, A6c README, A6 index, A6 STATUS, arenas.csv, command blocks |
| Comparison sheet | 2000×2460 PNG, 910,385 B | opened and checked |
| Source images opened | 8/8 | `read` on each render before composing |
| Writes outside `comparison/` | none | only `comparison/` created; no source edits |

## Notes for A14 (QA)

- The matrix's `failure_count` mixes syntax failures (Clouds 2) and runtime no-ops (City/Yeti/Space). The
  column header says "failure count"; the cell text states the kind. If A14 wants a strict syntax-only
  column, split it into `syntax_failures` and `semantic_failures`.
- `renders/comparison_sheet.png` is compiled, not captured. Its footer names the source folders; do not
  treat it as a fresh render.
- The design rankings in `README.md` §2 are explicitly **M** judgements; only the mechanics/commands are H.
- Check that no downstream doc cites `kill_sc_vip`; the correct string is `kill_sc_vp`.

## File inventory

```
README.md                        executive answer, ranking, template, blacklist, contradictions
STATUS.md                        this file
make_comparison_sheet.py         read-only PIL compositor (no world access)
data/ace_race_matrix.csv         4 × 18 comparison matrix (includes primary_sources)
data/what_works.md               adopt / adapt / reject by quality
data/runtime_gap.md              maps vs 40-player runtime, 13 capabilities
data/non_ace_takeaways.md        TGTTOS / Sky Battle / Parkour Tag / Decision Dome
data/contradiction_resolution.md kill_sc_vp ruling + Sky Race Red/Orange ruling
renders/comparison_sheet.png     eight-panel visual index
```
