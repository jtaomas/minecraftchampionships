# STATUS — video_clouds2 (A11)

Updated: 2026-11-25
Scope: one video (`X4l2WSQv0Wk`), Clouds 2 fan-recreation showcase. Research only.
Confidence: **H** = directly observed, **M** = inference from two sources, **L** = guess.

## Done

- [x] Read the PLAN and stayed inside `video_clouds2/`; shared source/tools read-only.
- [x] Confirmed the local video is the full source: 1920×1080, 30 fps, 3:54.97, H.264/Opus. **H**
- [x] Built a 1 fps/2 fps contact-sheet pass plus full-resolution frames at every required beat;
  opened every cited still. **H**
- [x] Timestamped clip map (`data/timeline.csv`) with 39 rows: intro, countdown, start, pads,
  elytra/water, castle, rainbow, climb, finish, outro.
- [x] Exact HUD inventory (`data/hud_text.csv`): `Starting In`, `13/12/11`,
  `You are in 1st place`, hearts/hunger/air; **no** lap/checkpoint/timer/standings text. **H**
- [x] Key-beats sheet (`data/beats.csv`): start, first speed pad, jump pad, elytra, checkpoint,
  failure/respawn, finish.
- [x] 17 full-resolution video stills + 3 world comparison renders + 1 HUD crop in `frames/`;
  `REFERENCE_IMAGES.md` indexes every one
  with timestamp, content, lesson and confidence.
- [x] Read-only world check: top-down surface map, isometric crops of start, castle, rainbow and
  the north-strip circular structure. A1’s six sensor banks sit at y = +6, −8, −12, −38, −38,
  −51; my sign scan sees the same stations at y ≈ +7…−50. **H**
- [x] Landmark comparison (`data/world_comparison.csv`): start/finish, castle, rainbow and water
  tunnel match the world by design; the yellow ring vs A1’s blue rings, the countdown numbers and
  the creeper are flagged as differences. **H/M**
- [x] A1 cross-check: **single-lap** loop; **no timer/lap/finish detection**; `You are in 1st place`
  is a static command-block string at (−51, 1, 29). **H** (A1)
- [x] Timing section separates **edit time** from **race time**; no lap time is claimed. **H/L**
- [x] Premium rebuild section: what to reproduce (legibility principles) and what to reject
  (spectacle, static “1st”, missing race HUD, 1:1 art). **H/M**
- [x] Flagged the contradiction with `../acerace/presentation/README.md` S5 (“no countdown, no
  HUD”): the frames show both. **H**

## Blocked / not possible from this source

- **No continuous lap.** The video is a montage (~48 cuts, multiple skins/hands, HUD toggled);
  route order cannot be read from clip order. **H**
- **No race clock, lap counter or checkpoint counter.** Total lap/race time and checkpoint cadence
  are not measurable; any number would be an estimate. **H**
- **No finish feedback.** The finish is a 0.8 s high shot of the apron; no gate, burst, title,
  chat or results. **H**
- **No clean failure/respawn.** The 0:21 fall is unresolved; no death screen or respawn shown. **H/L**
- **No exact FOV.** Wide, but not measurable from the HUD; quoted only as a range. **L**
- ~~No A1 dossier to cross-check.~~ A1 published during this pass. I cross-checked the route legs,
  the static actionbar, the six sensor banks and the single-lap fact against `../clouds2/README.md`
  and `../clouds2/data/route.md`; A1 remains the authority on commands, signs and containers. **H**
- **No pack inventory.** A10 owns `../resource_packs/`; this dossier only names visible textures
  (chevrons, spirals, bubbles, hearts, wing). **H/M**

## Open questions

1. Was the 0:21 fall a death? No death screen, no respawn, no chat; next shot is elsewhere. **L**
2. How long is the real countdown? The video shows 13→11; A1’s world uses nine titles `>1<`…`>9<`.
   Different versions; the video’s total is unresolved. **H** video/world; **L** length.
3. Is the giant yellow 2D ring an elytra ring or a jump gate? The player threads it; A1’s L8 rings
   are light-blue, so this is a pack/version difference or a different asset. **H** video/world;
   **M** mechanic.
4. What is the black-green bottom-right object? Held glider item or flight HUD widget. **M**
5. Does the world contain the giant creeper? Not in the sampled renders. **M**
6. What is the world’s actual HUD/route? A1: single-lap, no timer/lap/finish, static
   `You are in 1st place`. A10 owns the pack. **H**
7. North-strip circular structure: approximate x≈−80, z≈−280, y≈7. A1’s L5/L6 planet/water tunnel
   is nearby; confirm whether it is part of that transition. **M**

## Contradiction to flag for QA

`game-plugins/research/acerace/presentation/README.md` S5 row says the Clouds 2 recreation has
“no countdown, no title card, no freeze” and “HUD: None. Vanilla hearts/hunger/XP clutter”. The
video shows `Starting In` at 0:11–0:13 and a persistent `You are in 1st place` action bar plus
custom hearts/hunger/air at 1:05. The row should be corrected or annotated; it does not affect the
MCC-only design rules in that dossier, but it is wrong about this file. **H**

## Files

```
README.md                  dossier (findings, timing, comparison, rebuild rules)
REFERENCE_IMAGES.md        17 video stills + 3 world renders + HUD crop; supporting renders
STATUS.md                  this file
frames/                    01–17 video stills, 18–20 world comparison stills, 05b HUD crop
data/timeline.csv          clip/beat map (39 rows)
data/scene_cuts.csv        48 detected cuts for montage/continuity checks
data/route.md              clip-order route + “not a lap” warning
data/beats.csv             required beats only
data/hud_text.csv          exact strings, positions, absences
data/world_comparison.csv  landmark-by-landmark video/world check
data/route_readability.csv principles + what hurts
data/palette_video.md      semantic colour + contrast failures
data/timing.csv            measured edit times vs forced estimates
data/frame_index.csv       file → time → content
data/world_top_blocks.csv  world top-surface block counts (orientation only)
renders/                   top-down, iso, start/castle/rainbow/north-strip crops
scripts/topdown.py         read-only world surface renderer (writes only here)
```

## Next

- A13: use `data/route_readability.csv` and §7 as the video/feel input to the cross-map template.
- A14: check that every image in `REFERENCE_IMAGES.md` exists and that no claim here is used as a
  route-time fact; the timing section is edit time by design.
- A1: confirm the north-strip structure’s role, and whether the creeper/yellow ring exist outside
  the sampled crops.
- A10: map the visible textures (chevron, spiral, bubble, heart, wing) to the pack; do not infer
  the world pack from the video pack.
