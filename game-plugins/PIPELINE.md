# Per-game pipeline (every game goes through this)

All subagents run on **Opus**. The coordinator writes specs and briefs. Read `LESSONS.md` at every stage.

## 1. Research (two agents in parallel, outputs in `research\<game>_rules.md` and `research\<game>_maps.md`)

### Rules agent — "how the game runs"
Order of sources (use several; cross-check; cite URL per fact; mark confidence; list open questions instead of guessing):
1. Wikis: mcchampionship.fandom.com (only via built-in browser: `navigate` + `get_page_text`), mcchampionship.wiki.gg, mccisland.wiki (search/fetch). Read the game page, every map sub-page, the history/changes section, and item pages.
2. Other text: news articles, Reddit/forum threads (via search snippets if the site blocks), official announcements/patch notes, video descriptions.
3. Optional: YouTube transcripts (text) of a commentary/admin stream or a player's round, if reachable — for rules said out loud (timings, items, what happens on death). Skip if blocked; videos don't play in the browser pane.
Output: full rules (teams, rounds, timings, items/kits, objectives, win/draw conditions, scoring values, death/respawn, hazards, HUD/feedback, version changes) + a draft acceptance checklist of 15–25 testable statements.

### Maps agent — "the general idea of a map"
Collect as many images as possible, then describe structure:
1. Wiki galleries: on every game and map page, `find` "image", open each full-size image URL, screenshot.
2. **Image search**: in the built-in browser run image searches (Google Images `https://www.google.com/search?tbm=isch&q=...`, Bing Images `https://www.bing.com/images/search?q=...`, DuckDuckGo images) for queries like "MCC <game> map", "MC Championship <game> arena", "<game> MCC overview", "<game> MCC Island map", "<game> top down"; screenshot result grids, open the most informative results full-size and screenshot those. View only; no downloads; no sign-in; most-private cookie choice.
3. Top-down/overview renders and chest/loot maps are the most valuable; then spawn-view shots; then close-ups.
4. Optional: YouTube thumbnails/search result pages for extra angles.
Aim for 15+ distinct images per game across at least 3 different maps of that game (when the game has several). Record each image's URL and what it shows.
Output: structure notes — footprint, heights, sections/buildings/levels/lanes, spawn positions, sightlines, objective placement, hazards, palette/materials, detail density, backdrop, how team colours and interactive things are made readable — with estimates marked and confidence per point; a "design requirements for our own map" list (10–15 bullets). Structure and principles only: no official map names, no reproducible copy of an official layout.

### Video step (both agents; verified working in the built-in browser)
- **Own tab first:** call `mcp__Claude_Browser__tabs_create` and pass that `tabId` to every browser call. Several agents share the browser; never use the default tab.
- Find videos: `https://www.youtube.com/results?search_query=...` then read links with `javascript_tool`: `[...document.querySelectorAll('a#video-title')].map(a=>a.title+' | '+a.href)`. Prefer: admin/spectator free-cam streams (whole-map views), a single player's full round (spawn view, kit menu, HUD), map showcase / "all maps" videos, highlight compilations.
- Open `https://www.youtube.com/watch?v=<id>`; an ad usually plays first: poll `!!document.querySelector('.ad-showing')` with 10 s waits until false (don't click ads).
- Frame capture: `const v=document.querySelector('video'); v.currentTime=<sec>; v.pause();` wait 2 s, then `computer` screenshot (full scale for detail). Sample every 10–15 s through the relevant segment (40–60 frames per game); `v.duration` gives the length. Auto-captions show in the frame; for the full transcript open the description's "Show transcript" panel and `get_page_text`.
- From gameplay frames note: HUD (timer, round counter, scores, coin popups), kit/menu screens, what players hold at round start, what happens at round start/end and on death, plus map structure from different positions. Tag every fact with video id + timestamp.
- View only: no downloads, no sign-in, no comments/likes.

## 2. Spec (coordinator writes `<Game>\SPEC.md`; owner approves before any build)
Rules, map structure, acceptance checklist.

## 3. Build (two agents in parallel: mechanics, maps)
Shared helpers: staircase helper, connection post-pass, carpet check, sightline ray-cast, reachability flood-fill, offline renders.

## 4. Verify (before the owner sees it)
Bot test of the acceptance checklist on a test server + an independent reviewer agent comparing the build with the spec and research images.

## 5. Owner playtest on the review server → feedback file `<Game>\FEEDBACK_n.md` → fixes → new lessons appended to `LESSONS.md`.
