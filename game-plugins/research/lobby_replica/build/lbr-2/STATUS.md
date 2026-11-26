# LBR-2 — festive Decision Dome

> **BACKUP DOME — not for integration; the live dome is owned by another session.**
> This folder is a visual reference/backup only. Do **not** run or paste these functions into the test hub, do not expect this build to replace the live dome, and do not install the datapack on the review server.

**Status:** refined locally as a reference build. Glass hemisphere, copper ribs, widened/legibility-tested sign, clock and front elevation are complete. Nothing was run on the server and no other folder was touched.

**Owner scope:** the festive Decision Dome at `C = 5000, 120, 5000` in `mcc_lobby_official`. This backup does not include the island, plaza, paths, mountain, globe, portals or water.

**References used**

- `reference/hub_festive.png` — primary source for the drum colours, clock, `MCC CHAMPIONSHIP` sign, gold tiara, glass/copper dome, red bow, white cross, entrance bridge, screen wing and speaker tower.
- `reference/dome_exterior_labelled.png`, `dome_plan_labelled.png`, `dome_elevation_labelled.png` — used only for the drum/pod dimensions: 53-wide drum, 43 layers, ten pods at r16 and 36° spacing. The imported Decision Dome recreation (`existing_maps/decision_dome_recreation`) is the dark Nocturne Orrery; none of its palette, roof, crown or facade was copied.

---

## Bounds

| item | value |
|---|---|
| Full build envelope | `x 4959..5035`, `y 120..199`, `z 4971..5041` |
| Core drum shell | `x 4974..5026` (53), `z 4974..5026` (53), `y 120..161` |
| Drum + terrace | 43 layers from the plaza: drum `120..161`, terrace `162` |
| Terrace / rim | radius **28.85** (`x 4971..5029`, `z 4971..5029`), 1 layer at `y 162` |
| Clock/entrance axis | **south = +z**; the clock, sign, arch and bridge all face `+z` |
| Bridge | `x 4995..5005`, `z 5027..5041`, deck `y 120`, carpet `y 121` |
| West screen annex | `x 4959..4974`, `z 5014..5028`, `y 120..165` |
| East speaker tower | centre `(5030, 4997)`, radius 5, `y 120..154` |
| Sign marquee | widest band `x 4976..5024` (49), flat face `z 5028/5029`, `y 136..161` |

The build stays inside the LBR-2 area. The annex roof overhang reaches `r ≈ 49.6` above ground; all annex legs are within `r ≈ 42`; the bridge end is `r ≈ 41`.

---

## Height stack (after the refinement)

| height | element |
|---:|---|
| 120–121 | dark plinth ring + gold feet |
| 122–161 | drum shell, facade, clock marquee, entrance arch |
| 135–161 | flat black/gold marquee: clock `137–145`, `CHAMPIONSHIP` banner `146–154`, `MCC` crest `154–160`, crown icon `159–161` |
| 162 | terrace deck, arena floor, inlaid dial, snow patches |
| 163–171 | ten team pods (5×5 body, 7×7 canopy, 5×5 stepped top) |
| 162–184 | glass/copper dome: **near-hemisphere, apex 184** |
| 165–175 | gold tiara band, 8 red-jewelled spikes |
| 183–195 | red bow: four 3-D loops, compact knot, two tails down the glass |
| 193–199 | white cross, rising above the bow |
| 120–124 | entrance bridge deck, curbs and lantern posts |
| 120–154 | east speaker tower |
| 120–165 | west screen annex + roof + flag |

The glass dome is a true spherical cap: radius 25 at the rim (`y162`), 22 layers high, apex `y184`. From the crown band (`y168`) the visible dome is ~16 blocks high, so it reads as a shallow hemisphere from the approach while still having a real domed profile.

---

## What changed in this refinement

1. **Dome raised and shaped.** The old `r 25 → r 5.5` cap (`y162..177`) was replaced with a spherical profile `y162..183` and a gold apex collar at `y184`. The dome is now 22 layers high instead of 16, with a wide shoulder and a smooth taper.
2. **Eight copper ribs made structural.** Each rib is now **3 blocks wide** in `copper_block`, runs from the rim to the apex, and is crossed by **three copper hoops** at `y175`, `y180` and `y183`. They read from the front, iso and top views.
3. **Glass changed from mottled to 8 panels.** Four panels are clear `glass`, four are `white_stained_glass`, with `light_blue_stained_glass` accents near the top. The dark-red arena and the team pods remain visible through the shell; the top view is no longer a random colour field.
4. **Sign widened and font rebuilt.** The banner is now **49 wide** (47-wide red field), with a **3-wide, 5-tall** block font, a one-block gap after every letter, and red padding above/below the line. `C/H/A/M/P/I/O/N/S/H/I/P` are all distinct; the S and O glyphs were corrected. The clock is unchanged at 25×9.
5. **Banner no longer cut by pipes.** The gold side pipes and shroomlights stop at `y145` and resume at `y155`; nothing crosses the lettering.
6. **Bow, crown and cross lifted to the new apex.** The bow sits at `183–195`, the cross at `193–199`; the tiara remains at `165–175`.
7. **New straight-on south elevation.** `preview_front.png` is a one-pixel-per-block orthographic projection from `+z`; it verifies the word order and clock digits without perspective compression.
8. **Backup labelling and no integration.** `STATUS.md`, the comparison sheet and this folder now state that the live dome belongs to another session. No datapack was installed and no server command was run.

---

## Drum and facade (unchanged in this pass)

- **Shell:** 53-wide hollow cylinder, `r 23.4..26.6`, 43 layers `120..162` including the terrace.
- **Lower bays `122–131`:** red field, cream courses, ten team-colour panels, gold pilasters, four candy-cane stripes, snowflakes and a green/red wreath on the SE face.
- **Red belt `132–138`:** gold trim, gold studs at the pod angles, white band at the front.
- **Upper bays `139–158`:** red field, gold ribs, four cream/cyan window bays, a black/gold speaker disc at 135°, a second wreath at 225°, snowflake motifs.
- **Cornice `159–161`:** black concrete, gold line, `shroomlight` garland.
- **Entrances:** south arch 13 wide at the base, 15 tall; north service arch 9 wide, 12 tall.
- **Bridge:** 9-wide smooth-stone deck, red carpet, deepslate curbs, gold lantern posts.

---

## Clock and sign (verified)

- **Clock face:** **25 × 9** cells, `x 4988..5012`, `y 137..145`, one block proud at `z 5029`.
  - Gold frame; redstone corner studs.
  - Bespoke 5×7 block digits `0 0 : 4 2` = **`00:42`**.
  - Face read: **225 cells = 25×9**.
- **Banner:** outer **49 × 9** (`x 4976..5024`, `y 146..154`), red field 47 × 7 (`x 4977..5023`, `y 147..153`).
  - White **`CHAMPIONSHIP`**, 5-tall, 3-wide letters with a 1-block gap after every letter.
  - Red padding row above and below the word.
- **Crest:** 17 × 7 (`x 4992..5008`, `y 154..160`), white **`MCC`** in a 3-wide, 4-tall font.
- **Crown icon:** 9 × 3 gold crown at `y159..161`, red jewels at the points.
- **Straight-on check:** `preview_front.png` shows, left-to-right in the world, **`C H A M P I O N S H I P`** and **`00:42`**. It is **not mirrored** and has no horizontal compression; the wide banner is the legibility trade-off.
- No block states are used; the clock is static.

---

## Ten team pods

All at `r = 15.5`, 36° apart, on the arena floor `y 163`. Body 5×5 `163..169`; 7×7 canopy `168..169`; stepped 5×5 top `170..171` with a sea-lantern centre.

| # | team | angle (CW from north) | centre x,z | team blocks |
|---:|---|---:|---|---:|
| 1 | lime | 18° | 5005, 4985 | 271 |
| 2 | green | 54° | 5013, 4991 | 271 |
| 3 | cyan | 90° | 5016, 5000 | 272 |
| 4 | light blue | 126° | 5013, 5009 | 271 |
| 5 | blue | 162° | 5005, 5015 | 271 |
| 6 | purple | 198° | 4995, 5015 | 271 |
| 7 | pink | 234° | 4987, 5009 | 271 |
| 8 | red | 270° | 4984, 5000 | 272 |
| 9 | orange | 306° | 4987, 4991 | 271 |
| 10 | yellow | 342° | 4995, 4985 | 271 |

The 18° rotation keeps both the south clock entrance and the north service entrance clear.

---

## Dome, tiara, bow and cross

- **Glass dome:** `162..184`; radius 25 at the rim, 22 layers high, apex 184. Four clear and four white glass panels, light-blue accents at the top. No random colour mottling.
- **Copper structure:** 8 ribs, each 3 blocks wide at 45° spacing, plus hoops at `175`, `180` and `183`. Gold base ring and gold apex collar.
- **Gold tiara:** flared lip at 165, gold band rows `166..168`, eight 2×2 spikes `168..173`, red jewels `174`, gold caps `175`.
- **Red bow:** four 3-D torus loops (upper pair ±6.8, lower ±6.2), compact knot, two tails following the glass from 183 down to 175.
- **White cross:** smooth quartz vertical `193..199`, 5-wide crossbar at 197, white-concrete tip at 199.
- **Arena floor:** dark red terracotta, black outer ring, gold rings at r8, cream/red dial, sea-lantern hub.

---

## Side wings (interpretive, optional)

- **West screen annex:** 13×13 body on four teal/gold legs, 13×18 gold-framed screen facing south (cyan sky, white mountains, snowfield, gold ring, red pennant), stepped red/gold roof, snow shoulders, red flag, copper pipe.
- **East speaker tower:** 5-radius yellow cabinet, black bands, black/grey speaker cone, gold cap, gold/red finial, cable and copper connector.
- They remain in the default reference build. They are not part of the live dome and are not to be pasted anywhere.

---

## Files

| path | contents |
|---|---|
| `generate.py` | reproducible generator: palette, dome profile, font, every section, CSV/dump/function/datapack writers |
| `render_front.py` | straight-on south elevation renderer (front-to-back glass compositing) |
| `blocks.csv` | **45,145** blocks, columns `x,y,z,block,section` |
| `preview.mcmdump.gz` | renderer dump, region **77 × 80 × 71** |
| `preview_iso.png` | 2000×1918 isometric, south-east view |
| `preview_top.png` | 2202×2046 orthographic top view |
| `preview_front.png` | 818×876 straight-on south elevation, 10 px/block |
| `compare_reference.png` | 1820×940 reference crop vs the actual iso render |
| `functions/lbr2/00_load.mcfunction` | reference function list; **do not run on the review server** |
| `functions/lbr2/part_*.mcfunction` | chunked commands; largest **6,350** |
| `datapack/` | local reference datapack only; do not install on the test hub |

**Block count:** 45,145. **Block types:** 49. **Commands:** 19,741 across 16 part functions plus the load list.

### Function list

| function | commands | blocks placed |
|---|---:|---:|
| `part_10_base` | 222 | 790 |
| `part_20_drum` | 6,350 | 9,738 |
| `part_30_facade` | 1,919 | 4,232 |
| `part_40_windows` | 3,103 | 6,843 |
| `part_50_entrance` | 723 | 1,541 |
| `part_55_bridge` | 80 | 274 |
| `part_60_clock_sign` | 1,061 | 6,613 |
| `part_65_terrace` | 439 | 2,223 |
| `part_70_pods` | 940 | 2,740 |
| `part_80_glass_dome` | 2,087 | 3,792 |
| `part_90_crown` | 608 | 1,165 |
| `part_92_cross` | 7 | 11 |
| `part_94_bow` | 527 | 917 |
| `part_96_west_annex` | 1,012 | 2,630 |
| `part_98_east_tower` | 561 | 1,544 |
| `part_99_lights_snow` | 86 | 92 |
| `00_load` | 16 | — |

**Local reference only.** The datapack and functions exist so the backup can be replayed in a private scratch world if someone wants to inspect it. They are **not** to be installed, run or pasted into the test hub or review server.

---

## Verification

- All 45,145 blocks lie inside `x 4959..5035`, `y 120..199`, `z 4971..5041`; **zero blocks below y 120**.
- Drum shell is **53 × 53** at `y150`; terrace outer radius **28.85**; 43 layers from plaza to terrace.
- Glass dome springs from `y162` (r25), peaks at `y184`; 8 three-wide copper ribs and 3 copper hoops.
- Clock face is exactly **225 cells = 25 × 9**; the five glyphs are `0 0 : 4 2`.
- All ten team colours are present, **271–272 blocks each**; pods form a regular 36° ring at r15.5.
- Banner is 49 outer / 47 field; the front elevation shows `CHAMPIONSHIP` left-to-right and `00:42`, not mirrored.
- 19,741 commands; largest function 6,350; every command is `setblock`, `fill` or `function`.
- **Function replay check:** a local parser applied all 17 functions to an empty map and compared with `blocks.csv`: **45,145 blocks, 0 missing, 0 extra, 0 wrong**.
- Block IDs are vanilla Java 1.21.1; no Bedrock/Noxcrew/custom blocks, no barriers, command blocks, entities or items.
- No server command was run; no other folder was edited.

---

## Gaps / known deviations (backup context)

1. **Backup only.** The live Decision Dome is another session's build. This folder is a reference/backup and must not be integrated or pasted.
2. **Wide flat marquee.** The banner is 49 wide on a 53-wide drum and sits on a flat plate at `z5028/5029`; the reference sign is narrower and slightly curved. The width buys legibility and makes the letters 3 blocks wide with full gaps.
3. **Dome height.** The glass shell rises to `y184` (22 layers); the visible dome above the tiara is ~16 blocks, matching a shallow hemisphere. The bow reaches 195 and the cross 199, so the total ornament height is 80 blocks above the plaza. If a strict 57-block/177 roof line is required, this backup overshoots it by design.
4. **Pod visibility.** The tiara hides the pods' midsections; their stepped tops at 171 show through the upper glass. All ten pods exist and are correctly coloured.
5. **Arena is decorative.** No seats, stairs, barriers, gameplay logic or working clock. The interior below the arena floor is an empty shell.
6. **MCC crest is shadowed** by the terrace overhang at some angles; the gold crown icon is small.
7. **Screen art is abstract** (gold ring, snow mountains, pennant), not a copy of the reference's game screenshot.
8. **Snow** is limited to terrace patches, annex roof shoulders and bridge-post caps.
9. **No integration.** The bridge ends at `z5041`; the south path would start at `r48` (`z5048`) in the live hub. This backup is not to be connected.
10. **Camera.** `preview_iso.png` is the default south-east isometric; `preview_front.png` is a true orthographic south elevation. The reference is a perspective shot, so the comparison is compositional, not pixel-matched.
