# Frame index — Build Mart layout research

21 stills, all full-resolution. Source codes match `REFERENCE_IMAGES.md`.
Extraction: `yt-dlp` section downloads → `ffmpeg -ss <t> -frames:v 1`. No full VOD was kept.

| # | file (`frames/pack/`) | source | t | subject | conf | use for |
|---|---|---|---|---|---|---|
| 00 | `00_map_central_island_aerial.jpg` | S5 | 2:46 | whole mart aerial: disc, 8 pavilions, bridges, fan, upper islands, dome | **H** structure / M count | master plan reference |
| 01 | `01_map_gallery_aerial.jpg` | S4 | 3:04 | disc + plinth lattice + ring signs + pads | H | gallery spacing |
| 02 | `02_gallery_plinth_and_materials.jpg` | S4 | 3:12 | plinth with floating material list, signs, speed pad | H | plinth read-out |
| 03 | `03_build_area_portal.jpg` | S4 | 3:25 | portal, two plots, chest counter, tooltip | H | plot + portal module |
| 04 | `04_build_area_overview.jpg` | S4 | 3:36 | 3 plots, portal, 5 labelled chests, furnace bank, banner | H | build-area module |
| 05 | `05_gallery_yellow_plinth.jpg` | S4 | 4:00 | player-to-plinth scale, yellow build | H | scale calibration |
| 06 | `06_build_area_gold_plot_wide.jpg` | S4 | 4:12 | Gold dais + hazard canopy + green pads + label | H | Gold plot |
| 07 | `07_build_area_gold_plot.jpg` | S4 | 4:16 | Gold dais close, canopy | H | Gold plot surface |
| 08 | `08_build_area_slots_hologram.jpg` | S4 | 4:26 | plot-edge hologram (slots), 10:02 clock, `GOLDEN BUILD IN: 02:02`, ORANGE sign, fan | H | in-world UI + clocks |
| 09 | `09_upper_islands_logs_sand.jpg` | S4 | 4:30 | LOGS + SAND signs, island thickness, dome trusses, STONE | **H** | upper-island construction |
| 10 | `10_build_area_plots_row.jpg` | S4 | 4:36 | two plots, checker border, green pads, gold behind | H | plot detail |
| 11 | `11_gallery_white_and_launch_pads.jpg` | S4 | 4:54 | WHITE pavilion, roof launch pads, disc edge | M | return pads |
| 12 | `12_shopping_list_book.jpg` | S4 | 5:02 | 3-page shopping list book, icon grid, Done button | H | list UI |
| 13 | `13_gallery_overview_colour_ring.jpg` | S4 | 5:06 | continuous wall of colour signs around the disc | H | sign system |
| 14 | `14_gallery_from_ring.jpg` | S5 | 2:50 | disc seen from a pavilion: flags, plinths, bridge, fan | M | return sightline |
| 15 | `15_mcc_lettering_speed_pads.jpg` | S5 | 2:50 | MCC floor lettering, flush red spiral pads, kerb | H | floor graphics |
| 16 | `16_colour_pavilion_glass_cases.jpg` | S5 | 2:52 | colour pavilion interior: glass cases, banners, decals | H | pavilion construction |
| 17 | `17_official_build_area.png` | S6 | — | official build-area render, hall overview | H | proportions |
| 18 | `18_official_blueprint_gallery.png` | S6 | — | official gallery render, plinths from above | H | gallery layout |
| 19 | `19_official_revised_map.png` | S6 | — | official current-map render, disc eye level | H | material/colour language |
| 20 | `20_official_block_categories.png` | S6 | — | canonical block categories since MCC 20 | H | block palette scope |

## Confidence notes

- **H** = the thing is directly visible and unambiguous in the frame (an object, a sign, a colour, a count of ≤5).
- **M** = visible but partly occluded, or a count/measure inferred from parallel lines and player scale.
- **L** = not directly resolvable (island bearings, exact radii, interaction radius, spectator position).

## Scale calibration used across stills

1. Player height = 1.8 blocks. In `05`, the build is ~2.1 players wide → ~3.8 blocks; the plinth is ~1.4 players wide → ~2.5 blocks of *build*, on a 7×7 base.
2. Plot curb in `10` is ~8.3 player-widths → ~15 blocks.
3. Disc in `01` spans ~4.4 plot widths → ~66 blocks.
4. Pavilion deck in `00` spans ~0.9 plot widths → ~13.5 × 9 blocks.
5. Upper island in `09` spans ~3 plot widths → ~45 blocks across.

Error bar on all of these: ±15 % (perspective + occlusion). They are for *proportion*, not for a 1:1 copy.
