# Route and anatomy — TGTTOS Badlands

All coordinates are world block coordinates in the source world (`mcc_tgttos_badlands` is a copy). The map is a **single continuous run**: start pen → badlands shelf → mine/rail yard → canyon → mesa/cave → finish canyon → fenced chicken ledge. There are **no checkpoints, no lap logic and no round timer**. [H]

Content bounds: **x 4–254, y −61…12, z −5…69** (251 × 74 × 75). The world is a void superflat (`WorldGenSettings` biome `the_void`, air layers), so everything is a hand-built floating island; falling off the edge is a void death. [H]

Images referenced here are in `renders/` and `frames/`; every one is described in `REFERENCE_IMAGES.md`.

---

## 0. Route at a glance

| Leg | x range | Floor y | Length (x) | Signature | Evidence |
|---|---|---|---|---|---|
| Start pen | 8–12 | −43 (floor), player −42 | 5 | quartz floor, black-wool frame, 184 barriers, start sign | `renders/start_pen_barriers_iso.png`, `start_pen_barriers_top.png` [H] |
| 1 — badlands shelf | 12–62 | −43 → −31 | 50 | red marker stakes, cacti, first MCC billboard at x=64 | `renders/start_iso.png`, `overview_iso.png` [H] |
| 2 — mine / rail yard | 62–120 | −30 → −28 | 58 | two long white mine wings, solar arrays, MCC sign at x=64 | `renders/mine_gate_iso.png`, `mine_iso.png` [H] |
| 3 — canyon | 120–150 | −30 → −22 | 30 | stone-brick platform, black rail/wall at x=137 | `renders/canyon_iso.png` [H] |
| 4 — mesa / cave | 150–195 | −22 → 0 | 45 | 19-block west cliff, west-facing cave chamber, white pillar walls, mesa top | `renders/cave_iso.png`, `cave_mouth_iso.png` (cutaway), `section_z31.png` [H] |
| 5 — finish canyon | 195–230 | 0 → −5 | 35 | 21-block drop, open canyon, rising floor | `renders/plateau_iso.png`, `finish_iso.png` [H] |
| Finish — chicken ledge | 240–247 | −2 → 12 | 7 | gray MCC wall, red/white sign, 27 chickens, chain fence | `renders/finish_close_iso.png`, `finish_close_iso_back.png`, `finish_ledge_iso.png` [H] |

The map is **wide** in z (most of z 0–60 is playable). There is no enforced line beyond the start pen and the finish ledge; the table describes the direct line. Multiple alternatives exist and are noted per leg. [M]

---

## 1. Start — the pen (x 8–12, z 20–41)

- Floor: quartz at y=−43, x 9–11, z 21–40. Player spawn: `10, −42, 31`, yaw −89.4° (east). World spawn is the same point. [H `level.dat`, `playerdata`]
- Visible frame: black wool at x 7–12, y −43…−40, z 20–41 (98 blocks). The pen is 5 wide (x 8–12), 22 long (z 20–41). [H]
- Gate: barrier blocks, 5 tall (y −42…−38), on three sides — x=12, z=20, z=41 — plus a static 2-high barrier lip at x=8 (44 blocks) above the black wall. 140 + 44 = 184 barriers. [H]
- Start sign: oak wall sign at `9, −41, 31`, facing east, colour black, not glowing. Line 2 is the visible **Start** (bold green); line 1 is empty but carries the clickEvent `scoreboard players add @s start 1`. [H `data/signs.csv`]
- At countdown 120 (`GO!!!`) the fill `9 −38 41 12 −42 20 air replace barrier` removes the 140 gate barriers; at 130 `playing=1`. The x=8 lip stays. [H]
- The player exits **east** through the removed x=12 wall. There is no other gate. [H]
- The pen is the only "waiting" area; it is also where a finisher is returned after 3 s. There is no separate spectator platform. [H/M]

Top view: `renders/route_top_annotated.png` (start at left); barrier overlay: `renders/start_pen_barriers_top.png`.

---

## 2. Leg 1 — badlands shelf (x 12–62, y −43 → −31)

- The ground is a stepped badlands slope: red sand/terracotta/white terracotta strata, with dirt and brown concrete powder; the player climbs ~12 blocks over 50 blocks of x. [H `section_z31.png`]
- **Red marker stakes**: three small red accents on the slope — `x=25, z 40–43`, `x=39–40, z 18–21`, `x=51–52, z 35–38`, each 2–4 red concrete/red wool blocks 1–3 tall. They alternate sides of the centre line and read as route stakes; they are solid but small. [H blocks; M purpose]
- Hazards: cacti (181 across the map; the densest field is this slope and the finish canyon), 1-block ledges, and the void beyond the irregular island edge. No barriers, no water, no lava. [H]
- Sightline: the **MCC billboard at x=64** (white band + red letters, y −15…−9, z 22–40) is visible from about x 30 and is the first landmark. The mine wings appear behind it. [H]
- The first **3-wide smooth-stone/rail strip** crosses at x 63–65, z 10–52, y −32…−30. It is 1–3 blocks above the local floor; cross it or go around. [H]
- The route is not signposted beyond the stakes; the billboard and the mine silhouette do the wayfinding. [M]

Images: `renders/start_iso.png`, `renders/leg1_iso.png`, `renders/overview_iso.png`.

---

## 3. Leg 2 — mine / rail yard (x 62–120, y −30 → −28)

- **MCC west gate**: the billboard at x=64 is a 1-block-thick, 19-long, 7-tall red-on-white "MCC" panel on a dark frame, held above the course by white/cyan-striped pillars and chains. The player passes under/near it. [H `mine_gate_iso.png`]
- **Two long mine wings**: white terracotta walls with cyan terracotta/cyan concrete bands at x 82–118; the north wing z 2–14 and the south wing z 47–60; the central plaza z 15–46 is open ground. The wings have white/quartz roofs, iron-trapdoor roof details, nether-brick trim, sea lanterns, chains and cacti on the roofs. [H `mine_iso.png`, block bounds]
- **Solar arrays**: four 3×2 arrays of daylight detectors at y −18/−17, x 94–96 and 102–104, z 7–8/11–12 and 50–51/54–55 (48 detectors total). They are set into the roof/ground of the wings. [H]
- The route runs **through the plaza** between the wings, on natural ground at y −30…−28; there is no built floor in the plaza. [H]
- Hazards: cacti on the plaza and roofs; the two 3-wide smooth-stone strips at x 63–65 and 82–84 (y −32…−16; the latter is a ramp); the mine walls themselves. No checkpoint, no gate, no door. [H]
- A second white/red accent group at x 88–93 and 103–104 (small red concrete patches) sits on the wings; it is decorative, not a sign. [H]
- Sightline: the wings frame the route like a street; the far mesa is visible above them. [H/M]

Images: `renders/mine_gate_iso.png`, `renders/mine_iso.png`, `renders/overview_iso.png`.

---

## 4. Leg 3 — canyon (x 120–150, y −30 → −22)

- The mine ends at a **stone-brick platform** x 118–123, y −31…−28, z 12–44; beyond it the ground drops into a canyon of grey stone/andesite walls and a red-sand floor. [H `canyon_iso.png`, `section_z31.png`]
- **Black rail / wall**: `x=137–139`, y −29…−26, z 9–53 — a 2–3-block-high black concrete/black wool ridge 45 blocks long, with a black-wool bed at y=−29. It cuts the canyon across z. The direct line is blocked; go around the **z ends** (z < 9 or z > 53) or bridge over. [H]
- The canyon floor rises from about −30 at x 120 to −22 at x 150; the black ridge is the only built obstacle. [H]
- Hazards: cacti, the black ridge, the steep canyon walls, and the drop from the mine platform. No water/lava. [H]
- Sightline: the mesa/cave mouth appears at x 150; the black ridge hides the floor behind it. [H]

Images: `renders/canyon_iso.png`, `renders/section_z31.png`.

---

## 5. Leg 4 — mesa and cave (x 150–195, y −22 → 0)

- The mesa is a solid badlands block from x 150 to 195, top y −5…+2. Its **west face is a 19-block cliff** from the canyon floor (−22) to the top (−3…0). There is no staircase. The player either bridges up the face or takes a north/south shoulder. [H `section_z31.png`, `section_z0.png`, `section_z60.png`]
- **Cave**: a large **west-facing chamber** in the mesa, mouth at x≈150–160, z 14–50, from y −22 up to −5. The chamber runs east to a solid back wall at x≈195; it is a **dead end**, not a through-tunnel. Inside are two long white/cyan-striped pillar walls at x 182–184 and 187–189 (y −19…−8, z 14–48), with sea lanterns, chains, lanterns and smooth-stone bases. The cave floor is y −22…−20. [H `cave_iso.png`, `cave_mouth_iso.png`, air slices at y=−20/−14/−8]
- The cave is a **scenic detour**: entering it costs time unless the player is looking for the pillars/landmark. The race line goes over the mesa top, not into the cave. [M]
- The mesa top is a thin roof over the cave (y −3…0) and is continuous enough to run across. The **north/south shoulders** taper down to −10…−5 at z≈0–8 and z≈55–62, giving a natural 1-block-step approach from the canyon floor (longer than bridging). [H `section_z8.png`, `section_z60.png`; M route]
- At the **east edge** (x≈193–194) the top drops ~21 blocks straight into the finish canyon. `fallDamage=false` makes the drop safe. [H]
- Hazards: the 19-block climb, the dead-end cave, cacti on top, the drop. No barriers, no checkpoint. [H]

Images: `renders/cave_iso.png` (chamber + pillars), `renders/cave_mouth_iso.png` (**cutaway** at x=182; the right-hand opening is the crop face, not a real mouth), `renders/plateau_iso.png`, `renders/section_z31.png`, `renders/section_z0.png`, `renders/section_z60.png`. The mouth itself faces west at x≈150; the air slices at y=−20/−14/−8 are the definitive evidence, not the render. [H/M]

### 5.1 The block question

The intended bridge material is blue concrete (one block at a time, offhand, 44-block `CanPlaceOn`). The 19-block mesa face is bridgeable because it is terracotta/stone (in the allow-list). **Dirt is not in the allow-list**, so exposed dirt cannot be used as an anchor. [H]

---

## 6. Leg 5 — finish canyon (x 195–230, y 0 → −5)

- After the mesa drop, the player is on an open canyon floor that rises in steps: roughly −21 at x 195, −19 at x 200, −17 at x 203, −15 at x 205, −13 at x 207, −11 at x 210, −9 at x 213, −7 at x 217, −5 at x 223–230. [H `section_z31.png`]
- The canyon is 30–40 blocks wide in z and open to the sky; cacti and badlands strata. [H]
- At x 231–239 the floor is a **soul-sand/dirt trench** (top about −8…−4), with a 3-wide smooth-stone bed at x 233–235, y −6…−4. This is the foot of the finish wall. [H]
- **No checkpoint**; the finish is a single hit on a chicken. [H]
- Sightline: the MCC finish wall/sign is visible ahead and above the trench; the wall reads as the end. [H]

Images: `renders/plateau_iso.png`, `renders/finish_iso.png`, `renders/finish_top.png`, `renders/section_z31.png`.

---

## 7. Finish — the chicken ledge and MCC wall (x 240–247, z 8–55)

- **Wall**: gray terracotta mass x 242–247, y −10…+2, z 9–53 (black-wool and red/white accents); the broader finish structure, including the white towers, runs z 8–55. The large "MCC" sign is at x 241–242, y 4…12, z 22–40: white concrete band, red concrete letters, nether-brick fence posts, gray-wool cap. It is 19 blocks long and about 9 tall, centred on z=31. [H `finish_close_iso.png`, `finish_close_iso_back.png`]
- **Corner towers**: four white-terracotta towers with lanterns/chains frame the wall at x 233–235 and 240–242, z ≈8–10 and 52–54. [H]
- **Chicken ledge**: 1 block wide at x=241, floor y=−2 (red terracotta/iron trapdoor), z 11–51; 27 chickens at y=−1, z 11.2–51.8. [H]
- **Fence**: at x=240, y −2…1 — iron trapdoor (floor edge), chain at y=−1, iron bars at y=0, trapdoor at y=1 — a 3-high barrier between the player and the chickens. The player punches **through** it. [H]
- **Access**: the ledge is ~5–6 blocks above the soul-sand trench (top −7…−4). The player bridges up (blue concrete) to feet y≈−2 at x=239 and punches the chicken at x≈241.5, y=−1 (horizontal ≈2.5, within 3-block reach). Alternatively, drop from the mesa top / canyon rim onto the ledge. [H/M]
- The finish is **any chicken**: 27 are lined up; one hit increments `hit_chicken`; no pen check, no tag, no decoy. [H]
- After the hit: spectator 3 s, title with the time, then `tp` to the start pen. There is **no spectator platform**; the finish wall/towers are decorative. [H]

Images: `renders/finish_iso.png`, `renders/finish_top.png`, `renders/finish_close_iso.png`, `renders/finish_close_iso_back.png`, `renders/finish_ledge_iso.png`, `renders/finish_close_top.png`.

---

## 8. Alternative lines and skips

These are terrain observations, not verified speedrun routes; tag **M/L**.

1. **Mesa top vs cave**: the cave is a dead end; the fast line is over the mesa. The cave costs time unless the player is checking the landmark.
2. **North low bypass**: at z≈0–8 the mesa tapers to −10…−5 and there are gaps around x 185–205 at y −22; it may be possible to stay at canyon level and go around the mesa's north edge into the finish canyon. I did not walk it end-to-end at 1:1, so this is a suspected skip. [M/L]
3. **South shoulder**: z≈55–62 tapers from −14 at x 140 to −3 at x 160; a longer natural climb onto the mesa. [H/M]
4. **Direct west-face bridge**: 19 blocks up the mesa face; expensive in blocks/time but avoids the shoulder detour. With one-block-at-a-time offhand concrete this is ~19 placements. [M]
5. **Finish drop**: the 21-block drop from the mesa east edge is safe (`fallDamage=false`) and is the fastest way into the finish canyon. [H/M]
6. **No checkpoint or reset**: a mistake costs the whole run; the only recovery is a void death (soft respawn, timer continues) or finishing.

---

## 9. Spectator and waiting areas

- **Start pen** is the waiting area and the post-finish return point. It is sealed by barriers when empty and opened only by the countdown. [H]
- **No dedicated spectator platform, box or camera.** After a finish the player is in vanilla spectator mode wherever they finished, then teleported to the start pen in Adventure after 3 s. There is no `spectate` command, no teleport anchor and no confinement; a spectator can fly anywhere in the world (and into the void). [H/M]
- The finish wall/towers and the mine wings are **decorative**, not seating; the map is a practice map, not an event set. [M]

This is a deliberate contrast with `McmTgttos`, which confines finishers to the map's play box and shows a results sidebar (`TgttosGame.whack` + `GameListener.onTeleport`).
