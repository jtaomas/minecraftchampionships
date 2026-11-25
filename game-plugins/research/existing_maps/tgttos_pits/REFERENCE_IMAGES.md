# Pits — reference images

Every image below was opened at full resolution while writing this dossier. Source is the source world dump (`data/_test_start.mcmdump.gz`, world -3..117 / -64..29 / -9..83) rendered with the shared `tools/render_preview.py`; the chicken dots in the labelled images are added by `data/make_details.py` from `data/entities_raw.json`, because the block renderer does not draw entities.

The **blue background** in every render is the renderer's sky gradient, **not water, not void, not a resource pack**. The `--show-barriers` images draw barrier blocks translucent red; without the flag they are invisible.

| Image | Source / view | What it shows | What to take | Opened |
|---|---|---|---|---|
| `renders/world_iso_angle0.png` | full open shell, iso angle 0, scale 4 | The whole floating city: three street islands, park, building masses, black-glass ceiling plane, external red/white base banners | Overall massing and the 3-island/2-pit silhouette; the base is much larger than the course | yes |
| `renders/world_iso_angle2.png` | full open shell, iso angle 2, scale 4 | The opposite side of the same city; park on the right, finish-area buildings in front | Confirms the shell/ceiling and the depth of the building columns | yes |
| `renders/world_top.png` | full shell with ceiling and black glass dropped, top, scale 7 | Top of the city: dark roofs, the 3 roads as slots, park on the left, external banners at bottom | The map is a dense city block; the roads are trenches between roofs | yes |
| `renders/course_top.png` | course band y -16..4, top, scale 8 | The 3 islands, 2 pits, park edge, finish pool; blue = renderer background | Clean top view of the playable route; the pit gaps are the blue bands | yes |
| `renders/course_top_labeled.png` / `frames/02_course_top_labeled.png` | same + PIL labels | Numbered start/Road A/Pit 1/Road B/Pit 2/glass lip/pool/plaza; orange route; dashed bridges | Route in one frame; use with `data/route.md` | yes |
| `renders/course_iso_angle0.png` | course band, iso angle 0, scale 6 | Start strip, Road A, Road B, building canyon; finish partly hidden behind buildings | 3D readability; road paint; how buildings frame the route | yes |
| `renders/course_iso_angle0_labeled.png` | same + approximate markers | 4 markers: start strip, Road A, Road B, finish direction | The iso markers are approximate; use `course_top_labeled.png` for exact pit positions | yes |
| `renders/course_iso_angle2.png` | course band, iso angle 2, scale 6 | Reverse angle of the course; park/road relationship | Compare with angle 0 for the building occlusion | yes |
| `renders/route_schematic.png` / `frames/09_route_schematic.png` | drawn schematic, 8 px/block | Exact plan: road rectangles, 2 pits, glass lip, pool, 20 chicken dots, route arrows, legend, key coordinates, scale bar | The cleanest hand-off for coordinates; not a block render | yes |
| `renders/start_top.png` | start crop x4..25, z12..60, top, scale 12 | Top of the start area: park canopy, trench, road, side passages | The park hides much of the cage; use the labelled/large versions | yes |
| `renders/start_top_labeled.png` / `frames/04_start_top_labeled.png` | same + labels | Start spawn, sign, east gate, north/south corridors, road, park | Start anatomy in one image; sign is on the west wall | yes |
| `renders/start_cage_top.png` / `frames/03_start_cage_top.png` | start crop, top, scale 24, `--show-barriers` | The 22-long barrier cage as red side stripes; quartz floor; road on the right | Barrier shell and the 3-openings layout; the red is the barrier overlay | yes |
| `renders/start_cage_iso.png` | start crop, iso angle 0, scale 14, `--show-barriers` | The trench beside Road A, park canopy above/left, road paint | The cage is a trench cut into the park edge; the near wall hides the interior from this angle | yes |
| `renders/start_iso.png` | start crop x4..30, z12..60, iso angle 1, scale 8, `--show-barriers` | Wider start/park/road view | Context: start is embedded in the park/road edge | yes |
| `renders/park_detail.png` / `frames/10_park_detail.png` | park crop x -5..18, y -8..21, z12..62, iso angle 1, scale 10 | Green concrete/wool canopy, snow patches, stairs/slabs, road below | The only organic mass; the north exit leads here; possible roof access | yes |
| `renders/road_markings_detail.png` / `frames/05_road_markings.png` | road crop x14..48, z20..52, top, scale 16 | Both roads: yellow edges, blackstone asphalt, quartz 2:1 centre dashes, start shoulder | The exact paint pattern; no white concrete in the roads | yes |
| `renders/finish_top.png` | finish crop x59..80, z12..60, top, scale 12 | The long pool, quartz rim, buildings, no chickens (entities not rendered) | Pool shape and rim; chickens are absent by design of the renderer | yes |
| `renders/finish_top_labeled.png` / `frames/08_finish_top_labeled.png` | same + 20 chicken dots + labels | Exact saved chicken X/Z as yellow dots inside the 7×34 pool; lip, plaza, ends | Proof the chickens are spread over the length, not a row | yes |
| `renders/finish_iso.png` | finish crop x50..80, y -18..3, z12..60, iso angle 3, scale 8 | The pool as a long sunken reflecting pool between buildings | Finish landmark/silhouette | yes |
| `renders/finish_pool_iso.png` | tight pool crop x55..72, y -16..-4, z14..58, iso angle 2, scale 16 | Pool interior, quartz rim, brick tower, glass lip | The pool is empty in a block render; chickens are entities | yes |
| `renders/finish_pool_iso_labeled.png` / `frames/07_finish_pool_iso.png` | same + markers/legend | Pool, lip, east plaza, rim | Best close-up of the finish geometry | yes |
| `renders/pit_section.png` / `frames/06_pit_section.png` | drawn vertical section at z=36, 10 px/block | Road A slab, Road B tower, pits, glass lip, pool tower, 10 glass layers, shroomlight floor, kill plane -41 | The vertical logic in one image; not a block render | yes |

## Image-generation commands (reproducible)

```text
python tools/world_to_mcmdump.py "<source world>" data/_test_start.mcmdump.gz
python tools/render_preview.py data/dump_course.mcmdump.gz renders/course_top.png --top --scale 8
python tools/render_preview.py data/dump_course.mcmdump.gz renders/course_iso_angle0.png --angle 0 --scale 6
python tools/render_preview.py data/dump_full_open.mcmdump.gz renders/world_iso_angle0.png --angle 0 --scale 4
python tools/render_preview.py data/dump_start.mcmdump.gz renders/start_top.png --top --scale 12
python tools/render_preview.py data/dump_finish.mcmdump.gz renders/finish_top.png --top --scale 12
python data/make_route_map.py
python data/make_details.py
python data/make_iso_labels.py
```

The shared renderer and `world_to_mcmdump.py` were not edited. `data/filter_dump.py` only creates filtered copies; `data/scan_world.py` and the other scripts are read-only analysis helpers in this folder.

## What not to infer from these images

- The blue background is not water/void; it is the renderer's sky.
- Block renders do not draw entities: the 20 chickens, armor stands (none saved), signs (sign is a block), and item frames (none) are absent. Chicken dots are overlaid from NBT.
- The iso markers in `course_iso_angle0_labeled.png` are approximate screen positions; the top/schematic images are the coordinate truth.
- The renders use a flat-ish light model; in-game the black-glass ceiling tints the view and the hidden shroomlight does not light the course.
- The external red/white banners are outside the playable shell and are not visible from the course; they appear in the open-shell renders because the wall was removed.
