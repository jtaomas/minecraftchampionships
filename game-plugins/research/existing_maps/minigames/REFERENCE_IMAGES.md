# REFERENCE_IMAGES — A6 minigames survey

All images are renders of the **source** world, produced from `data/dump_117_mcmdump.py` + shared `tools/render_preview.py`. Nothing here is a screenshot of the live server. Every image below was opened and checked before writing this file. Evidence class: **H** (directly observed pixel content).

North is up in every top-down view (`-z` up), matching the renderer.

**Image accounting:** `frames/` contains **12 byte-identical convenience copies** of 12 distinct render views in `renders/`. The `*_top.png` / `*_top5.png` / `*_top_big.png` and `*_iso` / `*_iso_big` variants are the same view at different resolutions, not independent evidence. **Distinct views = 12; 14 including `renders/arena_atlas.png` and `renders/world_map.png`.** A14's duplicate audit is correct; do not count the copies twice.

| # | Image | Made from / bounds | What it shows | What to take |
|---|---|---|---|---|
| 1 | `frames/sky_battle_top.png` | `renders/sb.mcmdump.gz`; x1948..2052, z-52..52 | 105×105 barrier box; central ring with red/yellow/green/blue quadrants; four gift-box spawn pods at N/E/S/W; four wool **loot towers** styled as balloons (orange/cyan/lime/purple); plus-shaped mid | Sky Battle envelope and the 4 team-spawn symmetry. Team pods match Red (west), Yellow (north), Green (east), Blue (south) tp coords |
| 2 | `frames/sky_battle_iso.png` | same | Covered circular gallery, gift boxes with white ribbons, loot-tower baskets, void floor inside the box | Palette (team concrete/terracotta/wool, white concrete, stone bricks, quartz) and the fact that the barrier roof seals the arena at y56 |
| 3 | `frames/parkour_tag_top.png` | `renders/pt.mcmdump.gz`; x1035..1078, z972..1017 | Circular stadium bowl; course fills the interior; invisible barrier roof at y27; long red banner at the west edge | Parkour Tag envelope; course is inside the bowl; the y48..58 red strip is the banner, not an arena |
| 4 | `frames/parkour_tag_iso.png` | same | Tiered seating wall, interior parkour course, floating "PARKOUR TAG" banner | Build complete; loop has known defects (broken hunter tellraw, timer-stop/bossbar selectors) — A6b; banner is part of the copy box |
| 5 | `frames/lobby_iso.png` | `renders/lobby.mcmdump.gz`; x-235..-173, z904..984 | Yellow/gold crown-shaped ring with team-coloured outer wall, balloon inside, seating, and a small entry kiosk to the south | Hub identity and shape; the tiny south platform is the game-pad corridor |
| 6 | `frames/super_clash_iso.png` | `renders/sc.mcmdump.gz`; x4922..5012, z-30..6 | Large tinted-glass-roofed box with blue sky/cloud mural walls, two dark tower props east of the box, trees | Super Clash is an enclosed arena; top-down views are unreadable through the tinted roof, use iso for any future work |
| 7 | `frames/sky_race_red_iso.png` | `renders/woolred.mcmdump.gz`; x20991..21070, z19979..20021 | Black start tower, run of red glass arch/gate obstacles, red-and-gold finish basket, all floating over void | Sky Race is an aerial obstacle race; Red course copy box includes start tower + gates + finish |
| 8 | `frames/sky_race_orange_iso.png` | `renders/woolorg.mcmdump.gz`; x20991..21070, z20979..21021 | Lower/flatter orange course: visible y13..54, 24 cobwebs, no chains; same start tower and finish basket only | Orange is **not** a Red reskin: Red climbs y13..82 with 98 chains and grey cloud props; only the start/finish/gate rhythm matches (A6c) |
| 9 | `frames/sky_race_hub_iso.png` | `renders/woolhub.mcmdump.gz`; x19995..20013, z19992..20008 | Small square kiosk with six coloured pillars (red, orange, yellow, green, blue, purple) | Level-select hub; only red/orange are wired by commands |
| 10 | `frames/command_room_top.png` | `renders/cmd.mcmdump.gz`; x926..1004, z-18..16 | Flat white platform packed with command blocks, repeaters, redstone wire; separate test rig NE; ladder rail south-east | The 474-command Sky Battle logic room; top-down is the only useful view |
| 11 | `frames/rebecca_course_iso.png` | `renders/sky.mcmdump.gz`; x-50005..-49871, z-51005..-50946 | Long white concrete bridge, yellow/orange start platform, stepping stones, finish pad, lava below | A separate linear course, visually unlike the four named games; identity stays **M** |
| 12 | `frames/rebecca_kiosk_iso.png` | `renders/rebecca.mcmdump.gz`; x-50005..-49995, z-50005..-49995 | Small 11×11 kiosk with dark roof and a red-concrete-powder pad inside | Start point for `MG-REB-02`; "Rebecca's Minigames" sign |
| 13 | `renders/arena_atlas.png` | composition of the above top views | 10 labelled arena cells with ID, bounds, entry, and one-line note | The quick index; use with `data/arenas.csv` |
| 14 | `renders/world_map.png` | `data/make_world_map.py` | Panel A: true-scale main cluster (lobby, command room, Parkour Tag, Sky Battle). Panel B: schematic of all clusters with teleport links and exact coordinates | Spatial relationships and the fact that only panel A is to scale |

## Not used / rejected images

- `renders/sb_top5.png`, `pt_top5.png`, `lobby_top5.png`, `sc_top5.png`, `woolred_top5.png`, `woolhub_top5.png`, `sky_top5.png`, `rebecca_top5.png` — early low-resolution drafts, superseded by the `_top_big` and `_iso_big` files. Kept only as working files.
- `renders/*_top.png` at scale 3 — superseded.
- `icon.png` (world icon) — a low-resolution screenshot of the lobby ring; not used as evidence because its coordinates cannot be tied to blocks. **M** that it depicts the lobby.
