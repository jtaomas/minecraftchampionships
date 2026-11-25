# REFERENCE_IMAGES — Clouds 2 video (X4l2WSQv0Wk)

Every image below was opened at full resolution before this file was written. All paths are
relative to `game-plugins/research/existing_maps/video_clouds2/`. Video timestamps are video time;
the file is 3:54.97 at 1920×1080 / 30 fps. Confidence: **H** = directly observed, **M** = inference
from two sources, **L** = guess / unresolved.

| # | Path | Timestamp | What it shows | What to learn | Conf |
|---|---|---|---|---|---|
| 01 | `frames/01_countdown_12s.png` | 0:12.0 | First-person on the checkered start apron; cyan `Starting In`; yellow `12`; green hedge walls; stone/leaf arch; MCC banner; `You are in 1st place`; hearts/hunger | How a start line can read without a HUD panel: the arch frames the pads, the checkered floor is the “start” sign, the banner gives identity. The title is too large and overlaps the number — fix that in a rebuild. | H |
| 02 | `frames/02_start_straight_16s.png` | 0:16.0 | First-person running over yellow chevron pads on grass toward a grey stone arch; more pads ahead; custom hotbar strip | The first speed pad is **on the line**, not beside it; the arch is the destination frame; the pad texture is readable at a glance. This is the strongest single legibility image in the video. | H |
| 03 | `frames/03_fall_creeper_23s.png` | 0:23.3 | Freecam of a tiny elytra player falling past a giant black/green creeper statue beside a floating island; no HUD | Pure spectacle. It sells scale and danger but teaches nothing about the route; the fall has no respawn. Do not treat it as gameplay reference. | H |
| 04 | `frames/04_elytra_ring_63s.png` | 1:03.0 | Giant flat yellow 2D ring in open sky; a tiny player at its centre; grey cliffs left; no HUD | A ring can be a **silhouette**, not a 3D torus. The yellow zigzag reads at extreme distance. Use one dominant colour and a clear hole. | H |
| 05 | `frames/05_water_tunnel_hud_65s.png` | 1:05.0 | First-person blue water tunnel; red vertical beams; 10 blue air bubbles; black wing element bottom-right; `You are in 1st place`; hearts/hunger | The exact minimal HUD: action bar + survival bars only. The red/blue contrast keeps orientation in a monochrome tunnel. The wing element is unexplained. | H |
| 05b | `frames/05b_hud_detail_65s.png` | 1:05.0 | 1300×400 crop of the HUD: `You are in 1st place`, 10 hearts, 10 drumsticks, 10 blue-ring bubbles, thin hotbar strip | This is the **entire** HUD vocabulary. No lap, checkpoint, timer, position panel or chat. Use it as a “what not to omit” checklist, not a target. | H |
| 06 | `frames/06_water_tunnel_67s.png` | 1:07.0 | Red square gate inside the blue water tunnel; blue fog; green floor edge; HUD | A square gate is as readable as a ring; the red frame gives a target in a low-contrast tunnel. The gate sits on the line. | H |
| 07 | `frames/07_animated_cave_80s.png` | 1:20.0 | Prismarine/coral cave; glow lichen; title `and with custom animated textures` across the centre; HUD | The animated-texture palette is good; the full-screen title is bad — it covers the route at the exact moment the player needs it. | H |
| 08 | `frames/08_blue_tube_94s.png` | 1:34.0 | Green platform with blue tiles leading into a blue glass tube with thick red rims; white cloud walls; freecam | Entry readability: the green platform is “safe”, the blue tube is “go”, the red rim is the gate. Three colours, one decision. | H |
| 09 | `frames/09_launch_pads_98s.png` | 1:38.0 | First-person red spiral launch pads on a grass hill; black/green arrow; white structure ahead; yellow pads beyond; HUD | The launch pad, the arrow and the destination are all in one frame. This is the three-beat read at its cleanest. | H |
| 10 | `frames/10_castle_aerial_100s.png` | 1:40.0 | Castle aerial: white sandstone towers, red conical roofs, wooden bridge, yellow chevrons, black/yellow checkerboard, blue blocky item, player; no HUD | A castle can be a landmark cluster, not a single building. A1 places this at L11; the yellow pads are the only route line through the noise. | H |
| 11 | `frames/11_rainbow_bridge_114s.png` | 1:54.0 | Rainbow arch over a blue causeway; green lily pads; red spiral and yellow chevron pads; player with elytra; no HUD | The rainbow is the signature landmark of the village/rainbow leg (A1 L9): unique colour, visible from the causeway, and it marks the transition to the water/terrace section. | H |
| 12 | `frames/12_look_up_climb_118s.png` | 1:58.0 | First-person looking up ~70° at a sandstone arch/bridge, teal beam and white clouds; HUD | Verticality can be framed by a ceiling; the player reads “up” from the geometry, not from an arrow. This is the best non-pad wayfinding shot. | H |
| 13 | `frames/13_water_garden_124s.png` | 2:04.0 | Water garden aerial: blue pool, grass pads, yellow chevrons, red spiral pad on a grass island; white cave walls; no HUD | The surface coding does the work: green = land, blue = water, yellow = speed. A jump line over water is readable without a bridge. | H |
| 14 | `frames/14_pink_launch_152s.png` | 2:32.0 | First-person red spiral pad on a magenta frame; blue/yellow canyon; white/teal steps ahead; black wing element; HUD | The launch pad is framed by a contrasting colour (magenta), and the destination is visible up the canyon. The pad is the loudest object in the frame. | H |
| 15 | `frames/15_finish_arch_176s.png` | 2:56.0 | Green hedge arch with a black/white checkerboard banner and two giant pink flowers; checkered apron beyond; small wooden sign left; HUD | The finish/lap gate is unmistakable: green + checker + flowers, and it frames the apron. This is the one gate to imitate. | H |
| 16 | `frames/16_water_crossing_200s.png` | 3:20.0 | First-person at the edge of a blue pool; green grass pads; blue/white structure; rainbow behind; HUD | A jump line over water uses colour contrast (green on blue) and a landmark behind (rainbow) to keep the player oriented. | H |
| 17 | `frames/17_finish_apron_203s.png` | 3:23.0 | High third-person view of the checkered finish apron (red/white/black), green arrow, player with elytra, wooden barriers; no HUD | The finish is the **same apron** as the start, which is good. But there is no gate, burst, title, chat or result — the weakest beat in the video. | H |
| 18 | `frames/18_world_start_render.png` | n/a (world) | Isometric render of the world’s spawn start/finish: green hedges, checkered apron, red sign/banner, pink flowers, blue pool | Confirms the video’s start/finish design matches the downloaded world’s spawn area. The video’s MCC banner is not confirmed in the world; treat the logo as video-only branding. | H/M |
| 19 | `frames/19_world_rainbow_render.png` | n/a (world) | Isometric render of the world’s rainbow arch, blue causeway, green lily pads, pads, and blue/red tube at bottom-left | Confirms the video’s rainbow/causeway and blue tube exist in the world. The world crop is one piece of the same vocabulary. | H |
| 20 | `frames/20_world_topdown_marks.png` | n/a (world) | Top-down surface map with six command-station markers A–F; layered loop; large void centre; coordinate grid | The world is a long, layered circuit, not a flat loop; the six stations sit at different heights. The video’s clip order is not the world route order. | H |

## Supporting renders (not counted as video stills)

| Path | What it shows | What to learn | Conf |
|---|---|---|---|
| `renders/crop_start_iso.png` | World castle: red conical roofs, white sandstone towers, wooden bridge, yellow pads | The video's 1:40 castle is this cluster (A1 L11; design match). | H |
| `renders/crop_rainbow_iso.png` | World rainbow, blue causeway, green pads, blue/red tube | The video’s 1:54 rainbow and 1:34 tube are these structures (design match). | H |
| `renders/crop_north2_iso.png` | World circular glass-shaft tower with blue/yellow top and brown bridge | A world-only landmark absent from the sampled video frames. | H |
| `renders/crop_spawn_iso.png` | World spawn start/finish close-up | The video’s start/finish is this place (design match). | H |
| `renders/world_iso.png` | Whole-map isometric view | Quick orientation of the loop and void. | M |
| `renders/world_topdown.png` | Whole-map top-down | Surface/route orientation; coordinates. | M |

## What is deliberately not in this folder

- No full VOD copy (the shared `.mp4` already exists under `game-plugins/research/acerace/videos/`;
  I did not duplicate it).
- No extracted audio, no full-frame dump, no contact sheets in `frames/` (the working contact
  sheets were deleted after the stills were chosen).
- No screenshots from other maps.
- No resource-pack assets; A10 owns those.
