# Build Mart rebuild — shared research brief (Wave 1)

Read this before starting. It applies to every Build Mart research agent.

## Why this wave exists

`game-plugins/BuildMart/` already has a functioning loop: 5x5 plots, 27 regenerating stalls, four shop counters, Big Sales, presets and scoring. The owner wants the **map, the shopping-cart/list interactions, and the real block locations** to catch up with the rest of the rebuild. The current map is a code-built ring of team platforms around a 27-stall mart; the real MCC Build Mart is a large central hall with a Blueprint Gallery, dispersed resource islands, elytra/fan travel and a team build area with three plots plus a Gold Build plot.

The failure to avoid is the old one: shallow screenshots → immediate build. This wave is research only. No source is edited. The coordinator writes the map schematic and spec after the dossiers, and the owner approves before building.

## Non-negotiable rules

1. **Research only.** Do not edit `game-plugins/BuildMart/`, `plugin-src/`, worlds, `plugins/` or any source. Write only inside `game-plugins/research/buildmart/<your-agent>/`.
2. **Original build.** MCC mechanics and design principles may be recreated; layouts, map names, logos, assets and build presets must be original. Fan recreations are technical references only.
3. **MCC-first source hierarchy.**
   - **Tier 1 — design target:** MCC event footage/wiki, official Noxcrew pages, MCC admin streams. This is Build Mart.
   - **Tier 2 — technical reference:** fan recreations, open-source plugins, tutorials. Implementation only.
   - **Tier 3 — historical context:** the pre-MCC 14 shopping-cart map, if relevant. Use only to understand what the cart was; current MCC uses elytra/fan travel.
   - **Generic technique:** building/rendering/Paper performance can come from anywhere, labelled.
4. **Every factual claim cites source + timestamp/section + confidence H/M/L.** Never guess. Put unknowns under Open questions.
5. **Targeted clips only.** No full 1–2 hour VODs. Read the description/chapters/transcript first, then download only 2–5 minute windows with `yt-dlp --download-sections`. Max ~10–15 minutes of footage per agent. Images/wiki pages are fine. No browser pop-ups; write straight to `game-plugins/research/buildmart/videos/<agent>/`.
6. **Time-box:** deliver a useful first-pass dossier within ~15–20 minutes. Mark gaps instead of chasing them.
7. **Do not touch the live review server** at port 25580 or the Claude scratchpad.

## Current code (context, not a design source)

`game-plugins/BuildMart/`:
- `BuildMartGame`: `mcc_buildmart` void world; 8:00 game; team platforms around a central mart; 27 raw-material stalls; four shop counters (Glassworks/Mason/Kiln/Jeweller); 5x5 plots; a pedestal per side; Big Sales every 75 s; 27 presets in `BuildPreset`.
- `BuildPreset`: 5x5x5 max, stars from block count + shop-material weighting, block legend.
- Missing: Blueprint Gallery, three simultaneous plots + Gold plot, Gold Builds, resource islands, elytra/fan/launch pads, shopping-list book/cart, real map, spectator stand.
- The owner explicitly wants the **shopping cart interactions** and **where the blocks are** researched.

## What the real game looks like (verify everything)

From the MCC wiki (`Big_Sales_at_Build_Mart`):
- Team-based; 12 minutes current (was 10/15 in earlier versions). Teams replicate builds from a Blueprint Gallery; up to **three builds at a time**; each has a reference and a shopping list in the plot/book.
- **Blueprint Gallery:** seven plots — 2×1-star, 2×2-star, 1×3-star, 1×4-star, 1×5-star; rotates every **90 s**. A Gold Build plot appears; first Gold at **4 minutes**, rotates every 2 minutes, 7 stars, no reference in the team area, two minutes to complete from memory; failure removes the placed blocks.
- **Build area:** three regular plots + one Gold plot; five colour-coded chests, crafting tables, stonecutters, two smelting areas; portal to the mart; flight allowed.
- **Mart:** central island with a giant fan; resource islands around it (upper islands: ores/logs/stones; lower: coloured blocks; nether/coral/copper/garden/snow variants by event). Elytra + launch pads/speed boosts for travel. Falling off loses gathered materials and respawns you.
- **Scoring:** stars × 20 coins (current); partial completion pays by percentage; Employee of the Month bonuses (Entrepreneur most stars, Line Cook most builds, Quality Assurance highest average stars) pay 100/50/25.
- **History:** MCC 14 replaced **shopping-cart travel** with elytra travel and added the central fan, launch pads, nether/coral resources, Gold Builds. The current map is the resource-island map; the old map used carts. The owner wants cart interactions researched — determine exactly what the cart did, and whether it is a travel system, a block container, or a shopping list UI.

Do not treat this as final; correct it from footage and the wiki.

## Tooling

`yt-dlp` is a Python module, not on PATH:

```powershell
python -m yt_dlp --no-playlist --no-warnings --js-runtimes node `
  -f "bv*[height<=1080]+ba/b[height<=1080]" --merge-output-format mp4 `
  --download-sections "*MM:SS-MM:SS" `
  -o "game-plugins/research/buildmart/videos/<agent>/%(id)s_%(section_start)s.%(ext)s" `
  "<VIDEO URL>"
```

`ffmpeg`/`ffprobe` are on PATH. Use contact sheets (`fps=1/10,tile=5x4`) then 20–40 full-res frames. Reference images can be downloaded; do not download third-party map worlds.

## Image handoff (mandatory)

The builder must receive **actual image files**, not a text description of them.

- Save every useful reference as a full-resolution JPG/PNG under `frames/<topic>/` (not just a contact sheet).
- Write `REFERENCE_IMAGES.md`: image path → source URL → timestamp/section → what it shows → what the builder should learn from it.
- Prioritise 10–20 strong images over 100 near-duplicates: map overview/top-down, Blueprint Gallery, Gold Build plot, cart/shopping list, a team pod/plot, each resource island, travel/fan/launch pads, build-area chests/crafting.
- The build agent's first step is to open those image files with the read tool, not to read a summary. The coordinator will pass the paths into the build brief.
- Acceptance compares the built renders side-by-side with the reference images; "matches the text" is not enough.

## Dossier format

`game-plugins/research/buildmart/<agent>/README.md`:
1. Scope/method.
2. Sources table.
3. Findings (numbered, source + timestamp + confidence).
4. Frame index (`frames.md`).
5. Design implications.
6. Open questions.
7. Premium/implementation checklist (10–15 testable bullets).
Optional `data/` CSV/JSON.

Final message: 12–18 lines with the top findings, paths, blockers.

## Definition of done

- Three independent sources per major claim, or explicit `L` confidence.
- The dossier is specific enough to build the map/interaction without asking what a stall, island, plot or cart looks like.
- No source edited. No third-party asset copied. No full VOD downloaded.
