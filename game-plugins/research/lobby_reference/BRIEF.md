# MCC lobby reference — research brief

**Goal:** collect so many real images of the existing MCC event lobby that the layout can be reconstructed one-to-one from evidence, not memory. No building. No live-server changes. No custom interpretation.

**Target:** the MCC event lobby around the Decision Dome — the island in `seed/hub_festive.png` and the aerials `seed/hub_exterior_day.jpg` / `seed/hub_exterior_modern.jpg`. Not MCC Island.

## Rules

1. **Research only.** No game code, no map generators, no server commands, no world edits, no paste, no build.
2. **Actual image files**, full resolution. No text description as a substitute.
3. Label every image: source URL, timestamp/section, view/direction, what it shows.
4. **No full VODs.** Targeted windows only. Exceptions allowed: the 4:08 `DqEIibDDOtI` lobby showcase and the 6:40 `cfs4qlMmGcw` dome history, because the whole video is relevant. Extract frames at 1 fps and on scene changes.
5. For long event VODs: only the hub/lobby chapter, 2–5 minutes max; probe 6–10 seconds first, verify, then extract.
6. Prefer 1080p. 720p is acceptable only when it is the only source.
7. Write only in `game-plugins/research/lobby_reference/<agent-id>/`.
8. Duplicate images are allowed in your own output; QA dedupes later.

## Known seed sources

- `seed/hub_festive.png` — festive front view. The clearest single target.
- `seed/hub_exterior_day.jpg` and `seed/hub_exterior_modern.jpg` — the real aerial layout: dome on its own island, one bridge, crescent beach, mountain left, portals right.
- `seed/DqEIibDDOtI_contact_01.jpg` / `_02.jpg` — "MCC lobby and decision dome showcase", 4:08. Best full-lobby tour.
- `seed/cfs4qlMmGcw_contact_01.jpg` / `_02.jpg` — "The History of the Decision Dome", 6:40. Hub exteriors and interiors.
- `seed/GR-DecisionDome.png`, `dome_exterior_labelled.png`, `dome_plan_labelled.png`, `dome_elevation_labelled.png`, `dial_plan_labelled.png` — dome geometry.

## Source list

- MCC wiki Main Hub page and its file/gallery pages, both wiki.gg and Fandom mirrors, including old revisions via Wayback.
- YouTube: MCC lobby tours, Decision Dome showcases, MCC Live shows, update videos, event trailers, admin streams, seasonal variants.
- Reddit, Twitter/X, Tumblr, Pinterest, Imgur, DeviantArt, Bilibili, Weibo, Chinese/Russian fan sites.
- Planet Minecraft, MCCreations, Modrinth, GitHub — recreation pages and screenshots, even when no world is downloadable.
- Official Noxcrew static assets, MCC website, press renders.

## Required output

- `images/` — every downloaded image, original resolution.
- `REFERENCE_IMAGES.md` — path → source URL → timestamp → view → what it proves.
- `notes.md` — what the images reveal about the layout, scale, palette, seasonal differences, unresolved angles.
- `STATUS.md` — source count, image count, what is still missing, where you looked.
- For the layout analyst: `layout/` with a top-down diagram, coordinate plan, and a side-by-side comparison board.

## Acceptance

Enough views to determine: dome island size and position, bridge length/width, main island crescent shape, mountain position, globe position, portal-building positions and facings, water channels, beach/plaza shape, terrace levels, and seasonal overlays. If an angle is missing, say exactly which view is missing.
