# Build Mart — visual reference pack

All files are in `frames/`. Open them directly; do not re-host them or ship them in the plugin.
These are research stills, not assets. Nothing here is copied into `game-plugins/BuildMart/`.

## How to read this pack

- **Official** = MCC broadcast/recreation of the real game (Tier 1 design target).
- **Fan** = a fan recreation (Tier 2 implementation reference; do not copy its layout or assets).
- **Contact** = a contact sheet; use it to find a moment, then open the single frame.

Confidence is for *what the image shows*, not for a rule claim. Rule claims belong in
`README.md` / `data/current_vs_target.md` with their own sources.

Timestamps are approximate (±1 s) because frames were sampled.

## Official reference frames

| File | Source | What it shows | Emulate | Avoid / do not copy | Conf. |
|---|---|---|---|---|---|
| `official_gallery_plots.png` | repo still: `research/buildmart/layout/img/buildarea.png`; MCC broadcast (Tier 1), exact timestamp not recorded | Blueprint Gallery floor: several 5x5 plots with builds on them, floating name + `★★★` + `Materials:` hologram on the nearest plot; green "select" pad; gold-build bay at the back; "Select a blueprint in the..." prompt on the right | Plot footprint + floating material/star hologram; a distinct green selection pad; readable material list at the plot; gold bay framed differently from the seven normal plots | The exact layout, block palette, hologram font and "MCC" branding | H (visual) |
| `official_gallery_overview.png` | repo still: `research/buildmart/builds/img/gallery.png`; MCC broadcast | High view of a blue-floored gallery/hall: 3x2 arrangement of build plots, raised reference/pedestal builds, team-colour name walls around the edge (`ORANGE`, `YELLOW`, `LIME`, `BLACK`), red/gold road markings | Wide sightlines; team names readable from the air; a coloured route/road system; raised references so they cannot be confused with the plot | Blue-floor pattern, exact plot spacing, team wall design | H (visual) |
| `official_plot_hologram.png` | repo still: `research/buildmart/builds/img/crop_plot_grid.png`; MCC broadcast | Close-up of an empty plot with a floating `TKO ★★★ Materials: Black Concrete 9x ...` hologram, white/cream plot border and black floor | One hologram per plot, always facing the player, showing name/stars/materials; empty plot is visually distinct from a reference | The specific hologram text style/background; the exact border trim | H |
| `official_team_build_area_3plots.png` | repo still: `research/buildmart/rules/frames/01_team_build_area_3plots_gold.png`; MCC P25 broadcast, timestamp not recorded | Team build area from a player's eyes: three empty plots side by side, `GOLD BUILD` sign at the back, sidebar `BUILD SLOTS: Empty Plot #1/#2/#3`, hotbar tools + book, orange plot-selector item | Three regular plots in a row facing the same way; a separate gold bay behind/above; sidebar lists the three slots; book is always in the hotbar | Exact plot size, exact gold sign, the streamer overlay | H |
| `official_gold_booth_sidebar.png` | repo still: `research/buildmart/rules/frames/05_gallery_gold_booth.png`; MCC P25 broadcast | Gold booth: `GOLDEN BUILD IN: 00:06`, `GOLD BUILD` sign, gold plot; sidebar `BUILD SLOTS: Whale (★★★), Game Set Match (★★), Run (★★)`; chat shows teams completing builds; book in hotbar | A gold countdown that is global and unmistakable; selected-build names + stars in the sidebar; chat completion lines with team colours | Exact gold booth geometry; overlays | H |
| `official_gold_selected_broadcast.png` | repo still: `research/buildmart/rules/frames/06_gold_selected_broadcast.png`; MCC broadcast | `Golden Build selected!` centre-screen title over a team-coloured build hall; red selection pad on the floor; team name walls (`YELLOW`, `BLACK`, ...); gold countdown at top | Big centre title + sound when gold is selected; the gold bay is a physical, findable place; team-coloured floor zones | Team name walls and the exact hall | H |
| `official_build_area_gold_plot.png` | repo still: `research/buildmart/cart/frames/ref/10_build_area_gold_plot.png`; MCC broadcast | Gold plot in a team build area, chained frame around the plot, green pad, white/cream trim | A gold plot that is visibly "special" (chains/frame/colour) and cannot be mistaken for a normal plot | Exact chain/frame design | H |
| `official_gold_countdown_hud.png` | repo still: `research/buildmart/cart/frames/ref/17_gold_countdown_hud.png`; MCC broadcast | Gold countdown HUD at the top of the screen while players build | Put the gold timer in the boss bar / top centre, not only in chat | Exact HUD art | M |
| `official_available_blocks.png` | repo still: `research/buildmart/builds/img/martmap.png`; provenance not fully recorded — a fan-made "available blocks" sheet, not an official Noxcrew asset | Block taxonomy: colours (glass + concrete), garden, ores, stone, copper, lumber, bricks, sand, nether | Use the *categories* as the resource-island grouping; cover every colour and the full block families | Do not treat the exact list as current; do not copy the sheet art or "Big Sales at Build Mart" logo | M |
| `official_old_buildarea_tutorial.png` | `https://www.youtube.com/watch?v=LknsKs8ctvk` (MajK, MCC 7 POV), t≈0:16 | Old build area; tutorial text: "You are given all the tools ... going out through the portal ... Use your trolley to make traversing Build Mart faster - but be careful you don't fall in any holes, as you'll lose all the resources you're carrying." | Teach the portal + trolley + fall penalty in the pre-game, in the player's face; carts are a *travel* tool with a risk | Exact text/UI; the old mall layout | H |
| `official_old_mall_rails.png` | same video, t≈0:06 | Old mall: central corridor with a minecart rail line, department signs (`LUMBER`, `STONE`, `COLOURS`, `ORES`, `OUTDOOR`), red/yellow/white palette, glass roof, hazard-striped edges | A mall with a legible "department" wayfinding system and rails as the fast route; hazard edges that read instantly | The exact mall, signs, palette, branding | H |
| `official_cart_item_hotbar.png` | same video, t≈0:44 | Player in the old build area; **minecart item** in hotbar slot 5 (the "Shopping Cart"/trolley), pickaxe/axe/shovel/shears, empty slots | Cart as a *hotbar item* you place/use, not a GUI; clear tool row | Exact item order/name | H |
| `official_cart_portal.png` | same video, t≈0:56 | Player standing at the black portal doorway holding the minecart item; "Game begins: 00:11" | A physical portal from the build area to the mart; cart item visible before the portal | Exact portal frame | H |
| `official_cart_pov.png` | same video, t≈2:10 | Fast first-person movement through the old mall on the cart route; hazard-striped edges, coloured departments | Speed and risk: the cart is faster but less controllable; keep the route readable at speed | Do not copy the mall | M (cart not visible in-frame; POV is consistent with riding) |

## Fan reference frames

| File | Source | What it shows | Emulate | Avoid | Conf. |
|---|---|---|---|---|---|
| `fan_plottings_map_overview.png` | `https://www.youtube.com/watch?v=sbwR5ehOId4` (Plottings, 2024-08-15), t≈0:02 | Opening flyover of a custom Build Mart: floating build islands with red/white/purple trim over a light-blue void, hazard-striped edges | Readable silhouettes from the air; a coloured rim on every playable island; void as a design element | The exact floating-island layout/palette; it is the fan's own map | H |
| `fan_plottings_mart_dept.png` | same, t≈0:44 | Mart department: yellow/black hazard-striped block piles, red hazard holes in the floor, a player mining a grey block pile, blue void beyond | Hazard-striped edges around every resource pile; open holes that are visually obvious; piles are approachable from all sides | Exact block piles / hole pattern | H |
| `fan_plottings_plots_references.png` | same, t≈0:52 | Build hall: two rows of 5x5 plots; top row has reference builds on raised plinths, bottom row empty plots; player building; white/black floor | References raised behind the plot, not inside it; a clear "copy here" empty plot; multiple plots in one hall | The exact 3x2 hall; the all-black plot floor | H |
| `fan_person_plots.png` | `https://www.youtube.com/watch?v=gM6ewTfFofM` (ThePerson, 2024-08-06), t≈0:07 | Three build plots in a row on a white tiled floor; a reference build (green/black/blue) on a stepped plinth behind; red trim; `Coins` sidebar | Three plots + raised reference behind each; stepped plinth so the reference is visible over a builder's head | The flat white tile floor and red trim are generic; do not copy the MCC logo wall | H |
| `fan_person_plot_floor.png` | same, t≈0:08 | Close-up of a 5x5 plot floor with a thin dark outline and a single grey concrete block placed; `Gray Concrete` item name | A thin, high-contrast plot outline; item name shown on the hotbar; plot floor is neutral so block colours pop | A pure black/white checker floor is hard on the eyes for 12 min | H |
| `fan_person_crafting_kit.png` | same, t≈0:04 | Crafting table UI; inventory already holds 60x of many concrete colours; `Night Vision` + `Saturation` status; `Coins` sidebar | Give night vision/saturation as QoL; keep a crafting grid accessible in the build area | **Do not** hand the player the build materials: it removes the mart loop and trivialises the game | H |
| `fan_person_glass_walkway.png` | same, t≈0:38 | Glass-floored walkway between plots, red floor, grey pillars, blue portal; `Coins 128` | A portal/route back to the mart that is visible from the plots; glass as a safe walkway over a void | The exact corridor | H |
| `fan_person_hanging_build.png` | same, t≈1:02 | Large build hanging from chains in a tall hall; `MCC` lettering on the wall; `Iron Bars` item name | Verticality and a landmark; use chains/scaffolding to make a tall hall feel full | Hanging a build over the play area (collision/visibility risk) | H |
| `contact_fan_plottings.png` | same video, 1 frame / 2 s, 6x11 tile | Whole trailer at a glance: build islands, mart, plots, gallery, "50+ builds", "Freestyle", "Replayable" | Use to choose moments; not a design source by itself | — | H |
| `contact_fan_person.png` | same video, 1 frame / 2 s, 6x10 tile | Whole Showcase 2 at a glance: build hall, plots, item-frame wall, portal, HUD | Same | — | H |
| `contact_fan_person_part1.png` | `https://www.youtube.com/watch?v=3xFljOsJoXQ` (ThePerson, Part 1), 1 fps, 6x8 tile | Flat grass test world: one plot, a reference, block placement/detection; no map, no gallery, no mart | Shows the "mechanic first, map later" iteration path — useful as a process warning, not as a visual target | Do not ship a flat test world as the arena | H |
| `contact_official_cart_clip.png` | `https://www.youtube.com/watch?v=LknsKs8ctvk`, 0:00–1:10, 1 frame / 2 s | Old build area + mall entrance + rail line + cart item + portal | Use to find the cart moments; the single frames above are the citable ones | — | H |

## Gaps in this pack

1. **No clean frame of a player riding a cart in motion.** The cart is confirmed as a hotbar minecart item
   (`official_cart_item_hotbar.png`) and the rail line is visible (`official_old_mall_rails.png`), but the
   sampled clip did not show the seated pose clearly. If a build agent needs the pose, capture a 10 s
   window where the POV is low and the cart is visible, or build the cart from the item/rail evidence and
   playtest it.
2. **No official frame of the current resource-island map from above.** The layout agent has
   `research/buildmart/layout/img/buildmartmap.png` (a map diagram), but it is not in this pack. The
   fan `fan_plottings_map_overview.png` is *not* the official layout.
3. **No frame of the shopping-list book open.** The book is visible in the hotbar
   (`official_team_build_area_3plots.png`, `official_gold_booth_sidebar.png`) but the page layout is not
   captured. Do not invent a pixel-perfect book; design a readable book and playtest it.
4. **No frame of the five colour-coded chests / smelting areas.** See the rules agent's build-area
   frames; add them here when available.

## Use rules

- These are **reference only**. Do not copy layouts, map names, logos, textures, UI art or builds.
- Do not ship any of these PNGs in the plugin jar or resource pack.
- If a build agent needs a new frame, capture a 2–5 minute window with `yt-dlp --download-sections`,
  keep it in the temp folder, and add only the still + source line here.
