# Clouds 2 — video / gameplay analysis (A11)

Status: **complete draft, research only** · Folder: `game-plugins/research/existing_maps/video_clouds2/`
Source video: `X4l2WSQv0Wk` — “(Download) MCC: Ace Race - Clouds 2 Recreation”
Local copy: `game-plugins/research/acerace/videos/fan_recreations/X4l2WSQv0Wk.mp4`
World read: `Clouds 2 - Electric Boogaloo` (1.20.1, DataVersion 3465) at
`C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Clouds_2_-_Electric_Boogaloo__Download_Version_\Clouds 2 - Electric Boogaloo`.

No game/plugin code, no build, no server change, no edits outside this folder. The world and the
shared tools were read-only. Confidence: **H** = directly observed, **M** = inference from two
sources, **L** = guess / unresolved.

---

## 1. What this video actually is

1. **A fan recreation showcase, not an MCC event VOD.** The title says “Clouds 2 Recreation”; the
   on-screen card at 0:33 says “1:1 Working Ace Race Remake”; the outro is a download card. **H**
2. **1920×1080, 30 fps, 3:54.97, H.264 + Opus, 109.9 MB.** Read with `ffprobe`. **H**
3. **It is an edited montage, not one continuous lap.** Scene detection finds ~48 hard cuts; the
   countdown alone has two different takes (dark 0:11–0:13.4, bright 0:13.4+); the player’s hand
   changes from a cyan sleeve (0:16 / 2:08) to a black-and-green wing (1:05–1:28 / 2:32), and the
   HUD is switched off for many beauty shots. **H**
4. **There is no race clock, lap counter, checkpoint counter or standings panel anywhere in the
   sampled frames.** The only persistent text is `You are in 1st place`. **H**
5. **Therefore the video can teach look, feel, framing and pad vocabulary; it cannot teach lap
   time, checkpoint cadence, route order, or finish feedback.** **H**
6. **A1’s world dossier (published during this pass) reports a single-lap loop with no timer, lap
   counter or finish detection.** The video is a separate recreation; its lap count is not shown.
   Do not assume a three-lap race. **H** (A1 `README.md` §2, `data/route.md` §15), **H** (video:
   no lap HUD).

> The existing `../acerace/presentation/README.md` S5 row says this recreation has “no countdown,
> no title card” and “HUD: None”. The frames here show `Starting In` at 0:11–0:13 and
> `You are in 1st place` throughout. That row is inaccurate for this file; use the frames. **H**

---

## 2. Method and evidence

- `ffprobe` for duration/codec; `ffmpeg` scene detection at 0.35; 1 fps and 2 fps contact sheets;
  targeted full-resolution frames at every beat; HUD crops at 1:05.
- World: read-only region scan → top-down surface map (`renders/world_topdown.png`), isometric
  crops (`renders/crop_*.png`), and block-entity coordinates for the technical sign groups.
- The world’s full command-block / sign / container inventory is **A1’s** job (`../clouds2/`).
  This dossier only reads enough geometry to check whether the video’s landmarks exist.
- A1 published its dossier during this pass. I cross-checked the route legs, the static actionbar,
  the six sensor banks and the single-lap fact against `../clouds2/README.md` and
  `../clouds2/data/route.md`; A1 remains the authority on commands, signs, containers and exact
  route. My own read-only geometry pass was used only for the landmark renders. **H**

---

## 3. Timestamped gameplay map

Times are **video time**, not race time. `type` is what the viewer sees, not what the player did.
Every row below is **H** for what appears on screen; mechanic interpretations are tagged **M/L** in
the text that follows.

| # | Time | Beat | Type / camera | HUD | What to notice |
|---|---|---|---|---|---|
| 1 | 0:00–0:11 | Intro cards | title / n/a | none | Discord screenshots, “Okay, I’ll finish it!” |
| 2 | **0:11–0:13.4** | **Countdown** | gameplay / first-person | `Starting In`, yellow `13/12/11`, `You are in 1st place` | Green hedges, checkered apron, MCC banner; title overlaps the number; cut before release |
| 3 | **0:13.4–0:19.8** | **Start straight (post-countdown take)** | gameplay / first-person | `You are in 1st place` | Bright take; stone arch; **first yellow speed pads at 0:16**; no `GO!` title seen |
| 4 | **0:19.8–0:24.5** | **Fall / creeper spectacle** | cinematic / freecam | none | Player elytra-falls past a giant black/green creeper; **no respawn**; cut |
| 5 | 0:24.5–0:58.9 | Trailer montage + cards | cinematic | none | “Nearly a year in the making”, “1:1 Working Ace Race Remake”, “With checkpoints…” |
| 6 | **0:58.9–1:03.4** | **Elytra ring** | cinematic / freecam | none | Giant flat yellow 2D ring in the sky; tiny player at its centre |
| 7 | **1:03.4–1:29.2** | **Water / riptide act** | gameplay / first-person | `You are in 1st place`, 10 air bubbles | Blue tunnel, red beams/gates, prismarine/coral cave; black-green wing at bottom-right; title overlay covers the centre at 1:20–1:29 |
| 8 | 1:29.2–1:33.1 | Red launch pads | cinematic / freecam | none | Red spiral pads at a tunnel mouth; player stands between them |
| 9 | **1:33.1–1:35.0** | **Blue tube jump** | cinematic / freecam | none | Green platform + blue glass tube with thick red rims |
| 10 | 1:35.0–1:40.0 | Castle aerial → launch-pad POV | freecam → first-person | none → `You are in 1st place` | Red conical roofs, white walls, brown bridge, yellow chevrons; then red pads + black/green arrow |
| 11 | 1:40.0–1:47.3 | Castle aerial | freecam | none | Route threads the castle; yellow pads are the line |
| 12 | 1:47.3–1:50.0 | Sandstone corridor | first-person | `You are in 1st place` | Low overhang; blue causeway ahead |
| 13 | **1:50.0–1:57.8** | **Rainbow bridge** | cinematic / freecam | none | Rainbow over blue causeway; green lily pads; red/yellow pads |
| 14 | **1:57.8–1:59.0** | **Vertical climb** | first-person | `You are in 1st place` | Camera tilts up ~60–70°; sandstone arch, teal beam, white clouds |
| 15 | 1:59.0–2:02.0 | Water garden | freecam | none | Blue pool, grass pads, yellow chevrons, red spiral |
| 16 | 2:02.0–2:07.0 | Alley / arch | first-person | `You are in 1st place` | Narrow white wall; green arch; red pad |
| 17 | 2:07.0–2:12.0 | Arch / void | freecam | none | Orange/yellow arch over void; player looking down |
| 18 | 2:12.0–2:16.0 | Ice / water | freecam | none | Blue/white ice, red/blue rings, rainbow |
| 19 | 2:16.0–2:20.1 | Slot climb | first-person | `You are in 1st place` | Squeeze between white wall and blue glass |
| 20 | 2:20.1–2:24.0 | Island underside | freecam | none | Floating island, red pad, elytra player |
| 21 | **2:24.0–2:28.2** | **Pink launch** | first-person | `You are in 1st place` | Red spiral pad on a magenta frame; blue/yellow canyon; black wing |
| 22 | 2:28.2–2:33.3 | Island hop | freecam | none | Yellow pads on separate islands; elytra |
| 23 | 2:33.3–2:46.5 | Cave/island POV | first-person | `You are in 1st place` | White cave, blue ledge, green top |
| 24 | 2:46.5–2:49.5 | Red pad / arch sculpture | first-person | `You are in 1st place` | Red pad, black/green ring sculpture |
| 25 | 2:49.5–2:52.9 | Grass climb | first-person | `You are in 1st place` | Yellow pads up a grass slope |
| 26 | **2:52.9–2:56.3** | **Finish arch** | first-person | `You are in 1st place` | **Green hedge arch + checkerboard + two pink flowers**; checkered apron beyond |
| 27 | 2:56.3–3:04.9 | Water garden / castle aerial | freecam | none | Pool, pink/green pads, red spiral; castle |
| 28 | 3:04.9–3:10.0 | Final approach | first-person | `You are in 1st place` | White alley, wood fence, green arch |
| 29 | 3:10.0–3:23.0 | Final alley / water crossing | first-person | `You are in 1st place` | Moss alley, white slope, yellow pads, grass pads over blue pool, rainbow |
| 30 | **3:23.0–3:23.4** | **Finish apron** | cinematic / high third-person | none | Checkered red/white/black floor, green arrow, player; **no gate, burst, chat or title**; cut to logo |
| 31 | 3:23.4–3:55.0 | Outro | title | none | ACE RACE logo, “Download now!” |

### 3.1 The required beats, stated plainly

- **Start / countdown.** One custom centred title, `Starting In`, in cyan pixel type, with a yellow
  number below it counting `13 → 12 → 11` at 1 s intervals. The number is partially overlapped by
  the title. The camera is first-person on the apron; the player is frozen; the action bar already
  says `You are in 1st place`. The video cuts at 0:13.4 before a release or `GO!`. **H** for the
  text/counts; **M** for the freeze; **L** for the total countdown length (only 3 s shown).
  A1’s world countdown is nine titles `>1<`…`>9<` plus `Starting In`; the video’s 13/12/11 is a
  different countdown version. **H** video/world; **M** same map version.
- **First speed pad.** At 0:16 the player runs over a **yellow/orange chevron pad** on the start
  straight, with more pads ahead, toward a grey stone arch with green foliage. The pad is on the
  racing line, not off to the side. **H** (frame `02`).
- **First jump pad.** The first clear **red spiral launch pad** read is at 1:29 (cinematic) and
  1:38 (POV): red square-spiral pads on a grass hill, a black/green arrow pointing up the hill,
  and a white structure as the destination. The video does **not** show the launch arc itself;
  the pads are read, not demonstrated. **H** for the visual; **M** for the mechanic.
- **Elytra section.** Two distinct reads: (a) the giant flat **yellow 2D ring** at 1:03, with a
  tiny player threading it; (b) the **blue/red ring gates** in the later water/ice sections at
  1:34 and 2:12. The camera is freecam for (a); the player’s elytra is only clear from the third-
  person/underside shots. **H** for the rings; **M** for which clips are true elytra vs riptide.
- **Checkpoint.** No HUD checkpoint counter and no checkpoint title appears. The only physical
  gate that reads as progress is the **green hedge arch with the black/white checkerboard banner
  and two giant pink flowers** at 2:56; beyond it is the checkered apron. It reads as a lap/finish
  gate, not a mid-lap checkpoint. A1’s L13 finish corridor enters the pen from z = 29 and crosses
  the checkered line at z = 20; the video’s arch is the pen gate. **H** for no HUD counter; **M**
  for the gate’s role; **M** for the exact instance.
- **Failure / respawn.** The only fall is 0:21–0:24: the player drops off a floating island past
  the giant creeper, tracked by a freecam, then the video hard-cuts to a trailer shot. There is
  **no death screen, no respawn, no chat line, no “back to checkpoint” toast**. Whether it was a
  death is unresolved. **H** for no respawn UI; **L** for the death.
- **Finish.** At 3:23 a high third-person shot shows the checkered apron, green arrow, and the
  player with elytra wings. There is **no finish gate in frame, no burst, no placement title, no
  chat, no results, no lap time**. A1 confirms the world has **no finish detection**; the race
  ends visually at the line. The video cuts to the logo at 3:23.4. **H**

### 3.2 HUD — exact text and layout

```
                    (nothing at top)
                    (nothing on the right)

                 You are in 1st place          <- action bar, above the hotbar
        [10 hearts]              [10 drumsticks]
                    [10 blue air bubbles]      <- only while submerged
        [thin empty hotbar strip / often hidden]
                                   [black wing graphic with green chevron]  <- flight/water clips
```

- Exact action-bar string: **`You are in 1st place`** — capital `Y`, lower-case rest, `1st`. **H**
- Exact countdown string: **`Starting In`** — capital `S` and `I`, no ellipsis. **H**
- Countdown number: **`13`, `12`, `11`** in yellow, one per second. **H**
- There is **no** `LAP`, `CHECKPOINT`, `TIME`, `PLACE` field, `PLAYERS REMAINING`, sidebar, boss
  bar, tab list, chat, or subtitle. The action bar is the only position text. **H**
- Hearts/hunger/air are vanilla layouts with custom textures (bright hearts, drumstick hunger,
  blue-ring bubbles). **H**
- The hotbar is empty or hidden; no item icons are visible. The black wing graphic sits at the
  bottom-right only in the water/elytra clips and shifts with the camera. It is either a custom
  held glider item or a flight HUD widget; **M**.
- The action bar is **static and wrong during the countdown** — everyone cannot be 1st before the
  race starts. A1’s world dossier identifies it as a **static actionbar string at (−51, 1, 29)**,
  fired once by the start chain and cleared only by reset. It is not a live position. **H** (A1
  `data/route.md`; frame `01`).
- The trailer titles (`Nearly a year in the making`, `1:1 Working Ace Race Remake`, `With
  checkpoints, easy reset, music and SFX`, `and with custom animated textures`) are full-screen
  overlays, not HUD; the last one covers the route for ~9 s. **H**

### 3.3 Camera, FOV, motion

- **POV shots:** first-person, eye height ~1.6, wide FOV (roughly 90–100° horizontal; exact value
  not measurable), no view-bob, no motion blur, no speed lines, no FOV kick, no camera shake.
  Speed is sold by geometry and pad launches, not post-effects. **H** for the absence of effects;
  **M/L** for the FOV number.
- **Cinematic shots:** freecam, third-person, low-angle, high-angle, and slow orbit. Many are
  HUD-off. These are the video’s best-looking shots and the least representative of play. **H**
- **Pitch:** the camera tilts up ~60–70° at 1:58 (vertical climb) and down at 2:07 (arch/void).
  These are player camera moves, not cuts. **H**
- **Motion:** yellow chevrons give a horizontal boost; red spirals launch vertically; water tunnel
  is a straight fast swim; elytra is a smooth glide. The video never shows a hard landing, a
  bounce, or a failed pad. **M**
- **Transitions:** hard cuts; no fades except the intro/outro. **H**

---

## 4. Why the route reads clearly on video

The video is good at **one thing**: a player can name the next target from a single frame.

1. **Landmark hierarchy.** Each act has one saturated silhouette: the green start arch, the giant
   yellow ring, the red-roof castle, the rainbow, the pink-flower finish arch. They are unique in
   colour and shape and dominate their frame. **H** for the frames; **M** for the “visible from
   100+ blocks” claim, which the video implies but does not measure (frames `01`, `04`, `10`,
   `11`, `15`).
2. **Colour zoning.** White cloud = safe ground; green = village/garden; blue = water/ice; brown =
   wood/dirt; magenta/pink = launch; red spiral = jump; yellow chevron = speed; black/green =
   direction. The surface type is readable before landing. **H** (frames `02`, `09`, `11`, `14`).
3. **Pad language.** Two pad textures do most of the wayfinding: yellow chevrons (speed/forward)
   and red spirals (launch/up). They sit **on the racing line**, not beside it. The first 10 s
   teach the vocabulary; the rest of the video reuses it. **H** for the pads; **M** for the
   learning claim (frames `02`, `09`, `14`).
4. **Arch/tunnel framing.** The start arch frames the first pads; the blue tube frames the water
   entry; the green finish arch frames the checkered apron; the narrow alley frames the exit. The
   next beat is seen through a hole, not across the whole map. **H** (frames `02`, `08`, `15`).
5. **Painted arrows.** Black/green arrows on the floor and as floating panels add a second channel
   when the geometry is ambiguous (1:38, 2:56, 3:10, 3:23). They point at the pad or the gate. **H**
   (frames `09`, `15`, `17`).
6. **Vertical layering.** The map is a stack: sky islands, a water tunnel below, caves, a water
   garden, a high climb. The look-up shot at 1:58 and the underside shots at 2:20/2:41 show the
   next level from below, so height itself reads as progress. **H** (frames `12`, `03`, `20`).
7. **Sightline breaks.** Arches and cave mouths hide the next section until close range; the map
   never shows its whole self from the track. **M** (frames `07`, `15`).
8. **Repetition.** The same pad/arch/checker vocabulary recurs in every act, so no new UI is
   needed. **M**

### What undermines it

- **White-on-white.** Cloud, quartz, calcite and sky share a value range; at speed the next
  platform can disappear (frame `12`). **H**
- **Dark-on-dark.** The water tunnel is deep blue walls + blue fog + blue floor; only the red beams
  and the HUD bubbles keep orientation (frame `06`). **H**
- **Decoration noise.** The giant creeper, floating text signs, flowers, cacti and checkerboard
  walls compete with the route (frames `03`, `10`, `14`). **H**
- **Cinematic cheating.** Freecam/underside shots show the route from angles the player never
  gets, with the HUD off. The video therefore **overstates** first-person readability. **H**
- **No race feedback.** With no lap/checkpoint/timer, the viewer cannot tell whether the clip is
  lap 1 or lap 3, or whether a gate was a checkpoint or the finish. **H**
- **Static “1st place”.** The action bar says 1st during the countdown, so it is not a live
  position readout. **M**

### Does the world support the same read?

Yes, for the landmarks I could render:

| Landmark | Video | World | Verdict |
|---|---|---|---|
| Green-hedge start + checkered apron + pink flowers | 0:12 | `renders/crop_spawn_iso.png` | **H** same vocabulary; **M** same layout/instance |
| MCC banner | 0:12 | world start render shows a red sign/banner in a similar spot, letters not legible | **H** video; **M** world; treat the MCC logo as video-only branding |
| Checkerboard + pink-flower arch | 2:56 | same crop | **H** same structure |
| Red-roof castle towers + wooden bridge + yellow pads | 1:40 | `renders/crop_start_iso.png` (castle, A1 L11) | **H** same design; **M** same instance |
| Rainbow + blue causeway + green lily pads | 1:54 | `renders/crop_rainbow_iso.png` (village/rainbow, A1 L9) | **H** same structure; **M** same instance |
| Blue glass tube with red rims | 1:34 | bottom-left of the rainbow crop | **M** same design; one tube shown |
| Water garden + pink/red pads | 2:04 | rainbow/water crop | **M** same design language |
| Giant yellow 2D elytra ring | 1:03 | not in the sampled renders; the world has a circular blue/yellow tower top | **H** different structures |
| Circular glass-shaft tower | not in video | `renders/crop_north2_iso.png` | **H** world-only landmark |
| Giant black/green creeper | 0:23 | not in the sampled renders | **M** video-only or unsampled |

The world is **not flat**: A1’s six sensor banks sit at **y = +6, −8, −12, −38, −38, −51** (the
−38 level appears twice), and my sign scan sees the same stations at sign height y ≈ +7…−50. That
matches the video’s sky → water → cave → garden → climb structure in spirit. **H** for the world
heights (A1 bank table); **M** for the video mapping. Exact route order remains A1’s.

**The video’s clip order is not the world’s route order.** A1 places the rainbow at L9 and the
red-roof castle at L11, but the video shows the castle at 1:40 **before** the rainbow at 1:54. That
is direct proof of the montage’s non-chronological edit, independent of the cuts. **H**

---

## 5. Timing

**There is no race clock and no lap counter in the video.** Every duration below is **edit time**
(screen time), not traversal time. Do not put these in a race design as lap splits.

| Item | Measured | What it means | Conf |
|---|---|---|---|
| Video runtime | 3:54.97 | file length | H |
| In-world footage | 0:11–3:23.4 = 192.4 s | includes title cards, freecam, POV | H |
| Countdown shown | 2.4 s (13?11) | video shows only 13?11; A1 world uses nine titles >1<..>9<; versions differ | H/M |
| Countdown cut → first pads | 2.6 s (0:13.4–0:16.0) | one take; no `GO!` | H |
| Water/riptide act | 25.8 s (1:03.4–1:29.2) | screen time; includes cuts | H |
| Castle + rainbow act | 24.0 s (1:35.0-1:59.0) | screen time; freecam-heavy | H |
| Flight/water screen time | ~54 s total (0:20–0:24, 0:59–1:03, 1:33–1:47, 2:16–2:47) | some may be riptide, not elytra; “elytra uptime” is not measurable | M |
| Speed-pad visible | six sampled beats (0:16, 1:40, 2:04, 2:28, 3:10, 3:20) | ~6 s of sampled screen time; not uptime | M |
| Launch-pad visible | ~10 s of sampled clips (1:29–1:33, 1:38–1:40, 2:24–2:28) | no launch arc shown | M |
| Checkpoint cadence | **no data** | no checkpoint HUD or crossing shown | H |
| Full lap | **not shown** | estimate 2:20–2:50 from MCC reference + edit density | L |
| Full race (this world) | **not shown** | A1: **single-lap** loop, no timer/lap/finish; the race is the lap | H (A1) |

**Pace read:** the video cuts on motion, not on a beat. Most cuts are 2–6 s; the longest sustained
act (the water tunnel, 1:03.4–1:29.2) is ~26 s of screen time stitched from several takes. That is
a useful editorial rule for a trailer, but it is not evidence of how long the section takes to
play. **H** for the cut density; **M** for the pacing lesson.

### 5.1 Forced estimates (all **L** — do not treat as measurements)

These exist only so the rebuild has a starting point. They are derived from the video’s pace, the
MCC Clouds 2 reference lap (2:17–2:25) and the world’s layered scale; the video itself proves none
of them.

| Leg | Estimate | Basis |
|---|---|---|
| Start → first elytra/water entry | 15–25 s | start straight is short; the first act is a drop |
| Water/elytra act | 45–60 s | 26 s of screen time, but cuts remove travel |
| Castle / rainbow / water act | 40–55 s | three distinct sub-areas, all compact; edit order, not route order |
| Final climb → finish | 30–40 s | vertical climb + water crossing |
| Full lap | 2:20–2:50 | MCC reference + edit density; single lap (A1) |
| Full race | 2:20–2:50 | **single-lap world** (A1); no race total exists in the build |
| Elytra uptime per lap | 30–40 s | two long glide sections |
| Pad uptime per lap | 10–15 s | six speed beats + three launch beats |
| Checkpoint cadence | **not measurable** | no checkpoint HUD or crossing in the video; A1’s world has checkpoint bands but no counter |

---

## 6. Video vs the downloaded world

**Framing:** the video and the downloaded world are **two different fan recreations of the same MCC
map**, not two captures of one file. They share the landmark vocabulary and several exact-looking
structures, but the countdown numbers, the ring colour and the HUD details differ. Treat every
“match” below as a **design match**, not proof of the same world file. **H** for the differences;
**M** for shared authorship/asset lineage.

### 6.1 What matches

- **Start/finish:** green hedge walls, checkered red/white/black apron, green arrow, pink flowers.
  The world render and the video’s 0:12 / 2:56 / 3:23 shots share the same vocabulary, but the
  layouts are not identical. The video adds a large red **MCC banner** on the right; the world
  render has a red sign/banner in a similar spot but the letters are not legible, so do not claim
  the world ships the MCC logo. **H** vocabulary; **M** layout/instance and banner.
- **Start pen / first chevrons:** A1’s L0 pen at (5, 4, 24.5), checkered line at z = 20, green
  arrow pointing north, three yellow chevron strips in the garden. The video’s 0:12 / 0:16 shots
  match. **H** design; **M** instance.
- **Finish corridor:** A1’s L13 runs north from z = 60 to the pen at z = 20, flanked by white
  cloud/calcite walls, with yellow chevrons pointing north. The video’s 3:10–3:23 approach matches.
  **H** design; **M** instance.
- **Castle:** red conical roofs, white sandstone towers, wooden bridge, blue diving boards,
  yellow chevrons. A1’s L11 castle is at x −13..60, z 200..260; the video’s 1:40 aerial is this
  cluster. **H** design; **M** instance.
- **Village / rainbow:** A1’s L9 village is a desert-style sandstone cluster with a wooden deck
  and a parrot at x ≈ −148, z 70–84; the rainbow gate is at z = 161. The video’s 1:54 rainbow
  matches. **H** design; **M** instance.
- **Water garden:** blue pool, grass pads, pink/red launch pads. **M**.
- **Blue glass tube with red rims:** A1’s L6 water tunnel is a 6-block glass tube with red
  terracotta ribs every 13 blocks; the video’s 1:03–1:29 tunnel shows the same red ribs. **H**
  design; **M** instance.
- **Layered vertical route:** video changes from sky to water to cave to garden to climb; A1’s six
  sensor banks span y = +6…−51. **H** world; **M** video mapping.

### 6.2 What does not match

- **The north-strip circular structure.** The world has a circular blue/yellow top on a ribbed
  blue glass shaft, with a brown bridge entering from the left, at roughly x = −80,
  z = −260..−280, y = 7. A1’s L5 planet/waterfall and L6 water tunnel are in this part of the
  course, so this is probably part of that transition rather than a separate landmark. It is not
  obviously in the sampled video frames. **H** world; **M** its role; **M** whether it appears
  off-screen in the video.
- **The video’s yellow ring vs A1’s ring flight.** The 1:03 ring is a flat, yellow, pixel-zigzag
  ring in open sky. A1’s L8 ring flight uses **light-blue concrete rings** plus **yellow
  lightning-bolt markers**; the colour and shape do not match. This is either a pack recolour, a
  different version, or a different asset. **H** video/world; **M** explanation.
- **The countdown length.** The video shows yellow **13 / 12 / 11** under `Starting In`. A1’s world
  countdown is **nine titles `>1<` … `>9<`** plus `Starting In` (route.md §1). Different lengths
  and number styles; the video is not a clean read of the world’s countdown. **H** video/world;
  **M** same map version.
- **The giant black/green creeper.** Visible at 0:23 in the video; not found in the sampled world
  renders. **M** (may be unsampled).
- **Race HUD.** A1 confirms the world has **no timer, no lap counter and no finish detection**, and
  the `You are in 1st place` actionbar is a **static string** at (−51, 1, 29). The video shows a
  minimal HUD consistent with A1’s build; neither build has the race layer. **H** (A1).
- **Route order.** The video is an edited montage with hard cuts; its clip order is **not** a lap.
  A1’s L9 rainbow and L11 castle are shown in reverse order in the video (castle 1:40 before
  rainbow 1:54). **H**.

### 6.3 Version / pack

- World: **1.20.1**, DataVersion 3465, 322 command blocks, 285 signs, 225 shulkers, 21.23 MB
  pack (A1 dossier). **H**
- Video: 1080p30, custom pad textures, custom hearts/hunger/air, custom `Starting In` font. The
  visual language is 1.20-era; exact version not stated. **M**
- Do not infer the world’s pack from the video’s pack. A10 owns the asset inventory; this dossier
  only notes the visible vocabulary (chevrons, spirals, bubbles, hearts, wing). **H/M**

---

## 7. What a premium rebuild must reproduce

### 7.1 Reproduce (the transferable part)

These are design recommendations derived from the frames; the tag is for the observation behind
the rule, not a guarantee of the result.

1. **The three-beat read on every leg:** a unique landmark → a pad on the racing line → a framed
   exit (arch/tunnel/gate). If a leg has only two of the three, it will feel blind at speed. **M**
2. **Semantic colour zoning:** one colour = one meaning, used everywhere. White = safe cloud;
   green = village/garden; blue = water/ice; brown = wood; magenta/red = launch; yellow = speed;
   black/green = direction. **H**
3. **A shared start/finish place.** The apron, the green arch and the checkered line are the same
   structure; the lap is a loop, not a teleport. **H**
4. **One signature landmark visible from two legs.** The rainbow and the red-roof castle do this;
   the player can orient without a map. **H**
5. **A layered route with a clear descent and ascent.** Sky → water → cave → garden → climb gives
   the race an act structure and makes the finish feel earned. **H/M**
6. **Physical checkpoint/lap gates.** A gate the player passes through, with a readable label and
   a colour change, beats a pop-up. **H** (the video’s green arch is the best example).
7. **A calm HUD.** Race state at the edges (lap, checkpoint, time, position), the centre clear;
   one big title at lap/finish only. The video’s minimal HUD is calm but under-informs. **H/M**
8. **Pad readability at 30+ blocks.** The chevron/spiral textures are large, high-contrast and on
   the line. Do not shrink them to fit decoration. **H**
9. **A short, player-facing tutorial.** The world’s technical signs are for the builder; a player
   needs one line per mechanic (`Pads launch — rings boost — fall = back to checkpoint`). **M**
10. **A finish sequence.** The video’s finish is the weakest beat: no gate, no burst, no title, no
    chat, no results. A premium rebuild must add the feedback the video lacks. **H**

### 7.2 Do not copy (video-only spectacle or bad practice)

- **The giant black/green creeper** and the floating decorative text signs: they are memorable but
  they are set dressing, not route. Copying them would add noise and IP risk. **H/M**
- **The 1:1 MCC art and the `MCC` / `ACE RACE` logo lock-ups.** Build original geometry and an
  original event mark. **H**
- **The static `You are in 1st place` during the countdown.** A1 confirms it is a static command
  block at (−51, 1, 29), cleared only by reset. Hide the position line until the race starts. **H**
- **The missing lap/checkpoint/timer HUD.** A1 confirms the **world itself** has no timer, lap
  counter or finish detection; the video is showing the map’s real HUD, not a stripped showcase
  HUD. A rebuild must add the race layer. **H** (A1)
- **The freecam/underside shots as gameplay expectations.** They are trailer language; the player
  never sees those angles. **H**
- **The 32 s logo/download outro.** It is a YouTube CTA, not a game state. **H**
- **Full-screen title overlays that cover the route** (the animated-textures card at 1:20–1:29).
  If a tutorial card is needed, put it at the edge, not over the centre. **H**

### 7.3 Minimum acceptance for the rebuild’s video/feel pass

- A first-time viewer can name the next target in every 5 s clip. **Test**
- Every pad is visible from 30 blocks in first person, with no HUD. **Test**
- A lap has exactly one big title (lap/finish); checkpoints use chat + a gate particle. **Test**
- The finish gate is visible from 60 blocks and reads differently from a checkpoint. **Test**
- The action bar never shows a position before GO, and never goes stale for >0.5 s. **Test**
- No cinematic camera is required to understand the route. **Test**

---

## 8. Open questions, contradictions, and links

1. **Is the video a single run?** No. Different hands/skins (cyan sleeve, black-green wing), two
   countdown takes, HUD toggled off for many shots, ~48 cuts. **H**
2. **Was the 0:21 fall a death?** No death screen, no respawn, no chat; the next shot is a
   different location. **L**
3. **How long is the real countdown?** The video shows 13→11; A1’s world uses nine titles
   `>1<`…`>9<` plus `Starting In`. These are not the same countdown; the video’s version is
   unresolved. **H** video/world; **L** the total length.
4. **What is the black-green bottom-right object?** A held custom glider item or a flight HUD
   widget; it shifts with the camera and appears only in water/elytra clips. **M**
5. **Is the giant yellow ring an elytra ring or a jump gate?** It is a flat 2D target in the sky;
   the player threads it. A1’s L8 rings are light-blue, not yellow, so this is a pack/version
   difference or a different asset. **H** video/world; **M** the mechanic.
6. **What is the world’s actual HUD/route?** A1 published during this pass: single-lap, no timer,
   no lap counter, no finish detection, static `You are in 1st place`. A10 owns the pack. **H**
7. **Contradiction with `../acerace/presentation/README.md`:** its S5 row says this video has no
   countdown and no HUD. The frames show both. Use the frames; flag the row for correction. **H**
8. **The north-strip circular structure** is approximate from the isometric crop and sign group
   (x ≈ −80, z ≈ −280, y ≈ 7). A1’s L5/L6 planet/water-tunnel area is nearby; A1 should confirm
   whether it is part of the planet transition. **M**
9. **Does the world contain the creeper?** Not in the sampled crops; it may be outside them. **M**
10. **FOV exact value:** visually wide, no measurable HUD; do not quote a number as fact. **L**

Links: map forensics `../clouds2/` (A1) · pack `../resource_packs/` (A10) · cross-map synthesis
`../comparison/` (A13) · MCC presentation reference `../../acerace/presentation/`.

---

## 9. Evidence index

- 17 full-resolution video stills + 3 world comparison renders + 1 HUD crop in `frames/`; every
  cited image is listed in `REFERENCE_IMAGES.md`.
- `data/timeline.csv` — every clip/beat with camera, HUD, type and confidence.
- `data/scene_cuts.csv` — the 48 detected cuts (times exact; notes approximate).
- `data/route.md` — the clip-order route, with the “not a lap” warning and world anchors.
- `data/beats.csv` — the required beats only (start, first speed pad, jump pad, elytra, checkpoint,
  fail, finish).
- `data/hud_text.csv` — exact strings and what is absent.
- `data/world_comparison.csv` — landmark-by-landmark video/world check.
- `data/route_readability.csv` — principles, evidence, what hurts.
- `data/palette_video.md` — semantic colour and contrast failures.
- `data/timing.csv` — measured edit times vs forced estimates, all tagged.
- `data/frame_index.csv` — file → source time → what it shows.
- `data/world_top_blocks.csv` — world top-surface block counts (orientation only; A1 owns the full
  block inventory).
- `renders/` — top-down, isometric, start/castle/rainbow/north-strip crops, and the marked top-down.
- `scripts/topdown.py` — read-only world surface renderer used for the comparison; writes only
  into this folder.

### One-line verdict

**The video proves the map’s visual language works — landmark, colour, pad, arch — and proves the
recreation’s race presentation does not: no lap, no checkpoint, no timer, no finish feedback, and a
static “1st place” even before the race starts. Copy the legibility, not the HUD or the spectacle.**
