# Frame index and capture log

**Method.** Wiki pages were read first (`Build Mart (Map)`, `Big Sales at Build Mart`) to locate the current map and its sections. All wiki stills were downloaded at source resolution through the MediaWiki API and cropped losslessly with ffmpeg (`crop=W:H:X:Y`) — no upscaling except `island_stone.png` (source is only 911×513). Then **three 2.5-minute windows** of one POV VOD were downloaded with `yt-dlp --download-sections`; frames were pulled with ffmpeg at chosen seconds and saved as 1920×1080 PNG. No full VOD was downloaded.

Total: **54 full-resolution stills** (36 wiki-derived — 10 source frames plus 26 lossless crops — and 18 VOD frames) plus 3 contact sheets. Descriptive manifest with "what to take" is `REFERENCE_IMAGES.md`; this file is the provenance record.

## Sources

| Key | Source | Used for | Confidence |
|---|---|---|---|
| W1 | `mcchampionship.wiki.gg`, file `Build-Mart-Revised.png` (current map, first revision) | whole-mart composition, 4 island themes, team-bay ring | H |
| W2 | same, `Build-Mart-Revised-Blueprint.png` (current revision, P25+) | blueprint-gallery hub floor, giant letters, ceiling, island overview | H |
| W3 | `mcchampionship.wiki.gg`, `BSABM-BlueprintGallery.png`, `BSABM-BuildArea.png` | gallery plots and pattern; build hall, plots, gold station | H |
| W4 | same, `Build-Mart-Original.png` (original island map) | coral, garden/flowers, sand islands; central pavilion; floor emblem | H |
| W5 | same, `Build-Mart-Halloween.png`, `Build-Mart-Festive.png`, `Build-Mart-Pride-2023.png` | seasonal skin layer, snow island, ceiling hero prop | M |
| C | same, `BuildMartMap.png` | official available-block catalogue and category sizes | H |
| V | YouTube `k1LSXEr32KQ` — "Build Mart — MCC Rising 2 — Aqua Axolotls — Ginerous POV" (12:09) | hub plan, portal arrival, island interiors, build hall at eye level, copper/colour islands | H |

Textual facts (island categories, elytra + fan + launch pads, portal, five chests, stonecutters, two smelting areas, gold build) come from the wiki pages' prose, not from guesswork:
- `Build Mart (Map)`: "resources are now in several categorized floating islands… use elytra… giant fan in the center area… Speed Pads and Jump Pads from Ace Race… players who fall off respawn but lose all materials."
- `Big Sales at Build Mart`: "five colour-coded chests, crafting tables, stonecutters, and two smelting areas… enter via a portal in front of the Build Area… may also fly in the Build Area."

## VOD windows

| Window | Video time | Why | Files |
|---|---|---|---|
| 1 | 0:20 – 2:50 | intro flyover of the whole mart, then the build hall, then the portal exit | `vod/hub_plan_*.png`, `vod/team_bay_arrival_portal.png`, `vod/buildarea_*.png`, `vod/ceiling_ribbons_island_signs.png`, `vod/island_copper_harvest.png` |
| 2 | 3:30 – 6:00 | build hall first-person detail, then the walk to the mart entrance | `vod/buildarea_*.png`, `vod/island_colour_orange.png`, `vod/fail_case_flat_corrugated_wall.png` |
| 3 | 6:05 – 8:35 | colour-island interiors, hub from the dais, multi-plot hall | `vod/island_colour_*.png`, `vod/hub_from_dais_team_shelves.png`, `vod/buildarea_rainbow_*.png` |

## Frame map (VOD)

| File | Video time | Window |
|---|---|---|
| `vod/hub_plan_medallion_topdown.png` | 0:36 | 1 |
| `vod/hub_plan_team_bays_topdown.png` | 0:44 | 1 |
| `vod/buildarea_plot_eyelevel.png` | 1:48 | 1 |
| `vod/buildarea_carpet_banners.png` | 1:52 | 1 |
| `vod/buildarea_reference_chests.png` | 2:00 | 1 |
| `vod/team_bay_arrival_portal.png` | 2:04 | 1 |
| `vod/ceiling_ribbons_island_signs.png` | 2:08 | 1 |
| `vod/island_copper_harvest.png` | 2:28 | 1 |
| `vod/fail_case_flat_corrugated_wall.png` | 5:59 | 2 |
| `vod/island_colour_orange.png` | 5:59 | 2 |
| `vod/island_colour_yellow.png` | 6:15 | 3 |
| `vod/island_colour_lime.png` | 6:31 | 3 |
| `vod/island_colour_green.png` | 6:39 | 3 |
| `vod/island_colour_purple.png` | 6:47 | 3 |
| `vod/island_colour_cyan_shelves.png` | 6:55 | 3 |
| `vod/hub_from_dais_team_shelves.png` | 7:19 | 3 |
| `vod/buildarea_rainbow_plots.png` | 8:05 | 3 |
| `vod/buildarea_rainbow_closeup.png` | 8:29 | 3 |

Contact sheets (orientation only, 4 s steps):
`contact/vod_contact_w1_0m20-2m50.jpg`, `contact/vod_contact_w3_6m05-8m35.jpg`, `contact/vod_contact_all_stills.jpg`.

## Known gaps

- No clean close-up of the giant central fan from the side; only its top-down disc (V 0:36) and the fact of its updraft (wiki prose). Confidence **M** for its form.
- No full view of a copper island or of the snow island in the current revision. Copper is only seen from inside (V 2:28); snow only in the Festive skin (W5).
- Island count and exact heights are not visible in any single frame; counts below are estimates (**L**).
- Team build halls are seen only for one team (red); the per-team recolour is inferred from the hall's red carpet/banners vs. neutral shell (**M**).
- The hub in W2 (blueprint floor) and in V (first revision, dark floor) differ; both are treated as "current" variants. The **blueprint floor** is the later one and is the one worth studying for the gallery.
