# Reference images — Build Mart build pack

Primary handoff for the build designer. Every file here is a **real image at
native resolution** (no re-drawing, no AI upscale), cropped with ffmpeg where a
detail needed isolating. Open the files directly.

**Source hierarchy:** all files are official MC Championship wiki stills
(`mcchampionship.wiki.gg`), not fan recreations and not VOD frames. Wiki files
carry no timestamp, so the `Timestamp` column records `n/a — still` plus the
file page and page revision date. No video was downloaded for this pack.

Base path: `game-plugins/research/buildmart/builds/frames/`

| # | File | Source | Timestamp | Build | Star | What it shows |
|---|---|---|---|---|---|---|
| 01 | `01_blueprint_gallery_1920x1080.png` | [File:BSABM-BlueprintGallery.png](https://mcchampionship.wiki.gg/wiki/File:BSABM-BlueprintGallery.png), rev. 2026-11-08 | n/a — still | 7 gallery builds + gold pad | mixed | Full 1920×1080 Blueprint Gallery. Seven white-rimmed build pads on a blue floor, holograms at each pad, gold pad with yellow frame at centre. Use for room layout, pad spacing, sightlines. |
| 02 | `02_blueprint_gallery_pads_native.png` | same as 01 | n/a — still | tree, arch, house, snowflake, hat | mixed | Native-res crop of five gallery pads. Shows the 9×9 module: 1-block white/quartz rim, stone-brick corner squares, build centred, lantern posts between pads. |
| 03 | `03_gold_build_pad_gallery_native.png` | same as 01 | n/a — still | gold pad (empty between rotations) | gold (7) | The gold gallery pad: **yellow** frame instead of white, grey interior, red accent block at the near corner. Distinguishes the gold slot from the seven regular slots. |
| 04 | `04_build_area_1920x1080.png` | [File:BSABM-BuildArea.png](https://mcchampionship.wiki.gg/wiki/File:BSABM-BuildArea.png), rev. 2026-11-08 | n/a — still | TKO + two references | TKO 3★ | Full team build area: empty dark build plot with hologram, reference builds on adjacent pads, gold bay under the crown sign, colour-coded chests at right. |
| 05 | `05_tko_reference_pedestal_native.png` | same as 04 | n/a — still | TKO | 3★ | The TKO reference model (green posts, chain ropes, blue ring floor) on its lit white pedestal. This is what "a copy of the build is sent to the team area" looks like. |
| 06 | `06_team_plots_and_references_native.png` | same as 04 | n/a — still | TKO (empty plot + hologram) | 3★ | **Key frame.** The empty build plot — flat dark-grey grid floor, quartz rim, stone-brick corners — with the floating hologram `TKO / ★★★ / Materials: Black Concrete 9×, Blue Concrete 12×, Chain 18×, Oxidized Copper 4×, Oxidized Cut Copper 8×`. Shows the plot is not sunken and the material list is at the plot, not in chat. |
| 07 | `07_material_hologram_tko_native.png` | same as 04 | n/a — still | TKO | 3★ | Native crop (300×180) of the hologram alone: name, stars, five material lines with counts. |
| 08 | `08_material_hologram_tko_zoom3x.png` | same as 07 | n/a — still | TKO | 3★ | 3× nearest-neighbour enlargement of 07 for reading the counts in a slide. Pixel-exact, not reconstructed. |
| 09 | `09_stars_tko_zoom4x.png` | same as 04 | n/a — still | TKO | 3★ | 4× crop of the three gold stars above TKO. Evidence that the in-game hologram shows **3★**, while the wiki's MCC SB table lists TKO at 4★ (recorded as a conflict in the dossier). |
| 10 | `10_render_mighty_piston_1920x1080.png` | [File:BSABM-The_Mighty_Piston.png](https://mcchampionship.wiki.gg/wiki/File:BSABM-The_Mighty_Piston.png) | n/a — still | The Mighty Piston | 5★ / legacy gold | Official flat-lit render on the standard base. 7-wide build, ~8 tall, green glazed body, glass blades. Shows build density and height for a top-tier build. |
| 11 | `11_render_boot_camp_1920x1080.png` | [File:BSABM-Boot_Camp.png](https://mcchampionship.wiki.gg/wiki/File:BSABM-Boot_Camp.png) | n/a — still | Boot Camp | 1★ (P25 pool) | Official render: obstacle wall with ladder, 7 wide × ~6 tall. Shows that a 1★ build is still a full-height structure, not a flat sprite. |
| 12 | `12_render_fountain_1920x1080.png` | [File:BSABM-Fountain.png](https://mcchampionship.wiki.gg/wiki/File:BSABM-Fountain.png) | n/a — still | Fountain | 3★ (P25), 2★ (MU) | Official render: 7×7 grass base, stone-brick basin and four corner columns, ~4 tall. ~80 blocks. Useful 3★ calibration. |
| 13 | `13_render_house_1920x1080.png` | [File:BSABM-House.png](https://mcchampionship.wiki.gg/wiki/File:BSABM-House.png) | n/a — still | House | 5★ (SB) | **Key calibration frame.** A 5-wide × 6-tall façade, one block deep, ~40–50 blocks, rated 5★. Proves stars encode precision/recognisability, not volume. |
| 14 | `14_build_base_7x7_piston_native.png` | same as 10 | n/a — still | The Mighty Piston (base only) | 5★ | Native crop of the base: **7 grey floor tiles across**, 1-wide dark-red centre inlay, 1-block white rim. The direct evidence for the 7×7 authoring footprint. |
| 15 | `15_mart_overview.png` | [File:BuildMartMap.png](https://mcchampionship.wiki.gg/wiki/File:BuildMartMap.png) | n/a — still | — | — | Whole mart from above: central fan, blueprint gallery, upper/lower resource islands. Context for where the gallery sits relative to the islands. |
| 16 | `16_gold_build_sign_and_bay_native.png` | same as 04 | n/a — still | Gold bay | gold (7) | The gold bay: raised orange/yellow podium under a crown sign reading **"Gold Build — Copy this directly from the mart!"**. Note there is **no reference model** in the bay; the TKO model in the foreground belongs to the adjacent regular plot. |

## How to use this pack

- **Room / pad geometry** → 01, 02, 03, 14.
- **Build plot + reference + hologram** → 04, 05, 06, 07, 08.
- **Gold rules visually** → 03, 16 (plus 06 for contrast with a normal plot).
- **Height / density calibration by tier** → 10 (5★), 11 (1★), 12 (3★), 13 (5★ façade).
- **Star conflict evidence** → 09 vs `data/builds.csv` row `tko`.

## Gaps

- No official still exists for a **revamp 2★ or 4★** build; 11 and 12 are
  legacy builds that were re-rated into the revamp pool, so use them as tier
  proxies, not as current-pool members.
- No frame captures the **90 s rotation** or the **gold block-clear on
  failure**; those are text findings only (S2/S3), not image findings.
- The gallery holograms in 01/02 are too small to read at native resolution;
  only the TKO hologram (06–08) is legible. Do not infer material counts from
  the gallery pads.
