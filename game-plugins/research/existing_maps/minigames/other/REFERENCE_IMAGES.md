# REFERENCE_IMAGES — A6c other-arena inventory

All images are renders of the **source** world, made from A6’s existing MCMDUMPs with the shared `tools/render_preview.py`. Nothing here is a live-server screenshot. Every image below was opened and checked before this file was written; the 18 rows were re-checked after the A14 fix (all paths resolve; dimensions match). **H** (directly observed pixels).

- The 16 PNGs in `frames/` are byte-identical copies of the 16 PNGs in `renders/`; both paths point at the same render.
- The renderer hides `barrier` and `light`, so these images show **visible geometry**, not the full copy envelope. Bounds and barrier counts are in `data/other_arenas.csv`.
- North is up in top-down views (`-z` up), matching the renderer. Isometric views use the renderer’s default angle.
- “Source dump” is A6’s `.mcmdump.gz`; it already carries an 8-block margin around the arena bounds. The rendered image is trimmed to visible content.

| # | Image | Source dump / bounds | Size | What it shows | What it proves | Conf. |
|---:|---|---|---|---|---|---|
| 1 | `frames/hub_top.png` | `renders/lobby.mcmdump.gz`; x -235..-173, y 8..52, z 904..984 | 1278×1602 | Crown ring plan: gold outer wall, eight colour bays, central pedestal, **separate south pad deck**, and the empty band between them | The hub has 8 bays + 8 deck pads; the crown and deck are not connected by blocks (`z 951..960` empty). Top-down is the best view of the selector layout | H |
| 2 | `frames/hub_iso.png` | same | 1198×968 | Crown silhouette: gold/quartz banding, team-colour outer wall, hot-air balloon inside, small south pad deck | The hub reads as a decorative social centre, not a game arena; the gold crown is the identity | H |
| 3 | `frames/sc_top.png` | `renders/sc.mcmdump.gz`; x 4922..5012, y 17..36, z -30..6 | 2002×922 | Almost-black rectangle: tinted-glass roof; a few coloured fragments east (props/command rig) | The roof is opaque; top-down cannot show the Super Clash interior. The envelope and east props are visible | H |
| 4 | `frames/sc_iso.png` | same | 1254×864 | Blue sky/cloud mural box, dark glass roof, two dark tower props and trees east; interior only a brown silhouette | Super Clash is a **painted sky diorama** inside a glass box; the exterior is the readable part, the interior needs a roof-off render | H |
| 5 | `frames/srhub_top.png` | `renders/woolhub.mcmdump.gz`; x 19995..20013, y 39..49, z 19992..20008 | 918×830 | Six colour bars on the east face, six matching pads, mostly empty interior | The kiosk has **six level slots**; the image does not show which are wired (commands do: only red/orange) | H |
| 6 | `frames/srhub_iso.png` | same | 594×477 | Small brick/stone kiosk, six saturated pillars on the east face, neutral palette | Level-select kiosk form; the colour pillars are the only saturated elements | H |
| 7 | `frames/srred_top.png` | `renders/woolred.mcmdump.gz`; x 20991..21070, y 0..82, z 19979..20021 | 1650×450 | Start tower, a line of gate/beam/obstacle structures, finish basket; the course is a narrow strip in plan | Sky Race Red is a **linear multi-stage course**, not an open arena; the footprint is mostly empty barrier | H |
| 8 | `frames/srred_iso.png` | same | 868×673 | Black start tower, red-glass gates climbing to y82, hanging chains and grey cloud/branch props, red/white/gold finish basket over void | Red is a **vertical climb** with chains/cloud props; the whole course is self-contained in one copy box | H |
| 9 | `frames/srorg_top.png` | `renders/woolorg.mcmdump.gz`; x 20991..21070, y 0..82, z 20979..21021 | 1716×416 | Lower/flatter orange structures, start tower, finish basket | Orange has the same rhythm but a **different, flatter silhouette** than Red | H |
| 10 | `frames/srorg_iso.png` | same | 902×407 | Low orange platform run y13–54, cobwebs, no chains, same start/finish types | Orange is an **easy/lower variant**, not a pure palette reskin; do not reuse Red’s geometry unchanged | H |
| 11 | `frames/cmd_top.png` | `renders/cmd.mcmdump.gz`; x 926..1004, y 9..23, z -18..16 | 1738×858 | Dense rows of chain command blocks/repeaters/wire on a white platform; a separate NE test rig; SE ladder rail | The command room is a **circuit board**: 474 commands in two spatial clusters, organised in rows | H |
| 12 | `frames/cmd_iso.png` | same | 1240×724 | Layered rows of command blocks, repeaters, signs; white platform; separate test rig | It is a **service room**, not playable geometry; the logic is spatially organised but hard-coded | H |
| 13 | `frames/reb1_top.png` | `renders/rebecca.mcmdump.gz`; x -50005..-49995, y 97..104, z -50005..-49995 | 676×676 | Almost entirely dark roof; tiny footprint | Rebecca’s kiosk is an **11×11 booth**; top-down is just the roof | H |
| 14 | `frames/reb1_iso.png` | same | 532×378 | White-walled kiosk/tub, black glass rim, single red-concrete-powder pad inside | It is a **teleport booth**, not an arena | H |
| 15 | `frames/reb2_top.png` | `renders/sky.mcmdump.gz`; x -50005..-49871, y 99..131, z -51005..-50946 | 1782×882 | 135-block line: start platform/beam, stepping stones, lava pool, red platforms, yellow finish | The Rebecca course is **linear**, with a long lava hazard and a dog-leg finish | H |
| 16 | `frames/reb2_iso.png` | same | 1556×604 | White concrete bridge, lava pool, staircase of waxed-copper jump pads, yellow-carpet finish | Hand-built jump demo; copper pads are the jump-boost pads; no game loop visible | H |
| 17 | `../renders/arena_atlas.png` | A6 atlas (context) | 1400×2300 | Ten labelled arena cells with IDs, bounds, entry and one-line notes | Quick index of all ten clusters. The CMD cell says “Sky Battle logic only” (agrees with A6c). The SR Orange cell still uses the old same-layout wording even though the current A6 README says lower/flatter; `README.md` §4 treats the README as current and keeps the A6c evidence row | H |
| 18 | `../renders/world_map.png` | A6 world map (context) | 1460×1180 | Panel A true-scale main cluster; Panel B compressed schematic with teleport links | Shows why the arenas are separate islands: void gaps and teleport links, no walkways | H |

## Notes on what the images do **not** prove

1. **Barrier shells are invisible.** Sky Race Red/Orange look like a thin course; the 20,548/20,597 barrier blocks are not drawn. Use `data/other_arenas.csv` for the copy envelope, not the image.
2. **`light` blocks are invisible.** Super Clash’s 6,416 light blocks do not appear; the box reads darker than it plays.
3. **Top-down is not a floor plan for SC.** The tinted roof hides the interior; a roof-off render is needed before any layout claim.
4. **Colour in the image is the renderer’s block-colour table, not a resource pack.** No pack is loaded; `waxed_copper` and `raw_gold` are approximations.
5. **The top-down images are trimmed to visible content.** The blank margins are not part of the arena; the full bounds are in the CSV.

## Rejected / not used

- A6’s early low-resolution drafts and scale-3 top views: superseded by A6’s `_big` set; not used here.
- A6’s individual Sky Battle / Parkour Tag frames: out of scope (A6a/A6b).
- The world icon: a low-resolution lobby screenshot with no coordinate tie; not used.
