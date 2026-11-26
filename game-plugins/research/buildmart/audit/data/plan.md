# Build Mart — phased rebuild plan

Order requested by the owner: **map + gallery + 3 plots → cart/list → Gold → polish**.
A short Phase 0 comes first because the current code has economy and cleanup bugs that would poison
every later phase.

Each phase ends with a build, a start/stop cycle, a preview dump, and the listed acceptance tests.
No phase may leave two systems writing the same blocks.

---

## Phase 0 — Stop the bleeding (0.5–1 day)

**Goal:** make the current game safe to run while the real map is built. No new content.

### Tasks

1. **Fix team scoring.** Replace the per-member `ctx.addPoints` loop in `complete()`
   (`BuildMartGame.java:770–771`) with `ctx.addSidePoints(side, pts, ...)`. Add a debug line
   `team total = pts` and a test with a 4-player team vs a solo.
2. **Fix solo/many-side layout.** Cap the team ring at 10 sides; if `keys.size() > 10`, use a compact
   solo/test grid (or split into heats). Do not let 40 solos grow the ring to ~147 blocks and reuse 12
   colours (`:208–229`).
3. **Material → BlockData compare.** Change `BuildPreset.at()` to return `BlockData` (or add
   `BuildPreset.dataAt()`), compare canonical data in `evaluate()`, and add a rotated-stair test that
   must fail. Keep a config flag `strict-block-states: true`.
4. **World wipe + autosave off.** Move cleanup into a small `MapWorld` helper: track every chunk the
   map or players touched, set `setAutoSave(false)`, clear chunks on stop, remove only tagged entities,
   release tickets. Keep the folder delete at enable.
5. **Quit/rejoin.** Add `PlayerQuitEvent` handling: clear Build Mart items, cursor, flight, vehicle;
   remove from `sideOf`; do not let a ghost team complete builds (skip areas with no online members).
6. **Inventory safety.** In the shop loop, check `addItem` leftovers and roll back or drop at the
   player's feet; clear the cursor on void; do not consume costs if the product cannot fit.
7. **Config skeleton.** Add `BuildMartConfig` with timings, plot count, strictness, debug; load in
   `onEnable`; log defaults as "defaults, not researched".
8. **Feature-flag the fiction safely.** Flag only the "Big Sales" timer (`startSale`/`SALE_*`) first.
   Keep the four shop counters until Phase 2's crafting/smelting loop exists, otherwise presets that need
   glass/stone bricks/bricks/gold become uncompletable. Delete the shops in Phase 2, not before.

### Acceptance tests

- [ ] A 4-player team that completes one 3★ build has the same team total as a solo completing it
      (not 4x).
- [ ] A stair placed facing the wrong way does **not** complete the build; the right way does.
- [ ] 20 solo players start on a compact solo layout (not a ~300-block ring) with unique colours, or the
      game refuses and logs a clear error.
- [ ] Start → stop → start leaves no stale blocks, items, labels or entities.
- [ ] A player who quits mid-build and rejoins has no Build Mart items and cannot build as a ghost.
- [ ] A full inventory at a shop does not delete the costs or drop the product silently.

**Do not proceed until these pass.**

---

## Phase 1 — Real map + gallery + 3 plots (3–5 days)

**Goal:** one original map that looks and plays like Build Mart: a team build hall with 3 regular plots
and a gold bay, a 7-plot Blueprint Gallery, and a mart footprint with placeholder resource nodes.
Gold logic can be a placeholder (locked bay) in this phase.

### 1.1 Map layer (new files)

- `world/BuildMartMapData` — sparse block-state store, `Region`, `Pos`, `Spawn`, `PlotSlot`,
  `GallerySlot`, `ResourceNode`, `ChestSpot`, `TravelNode`.
- `world/BuildMartMap` — pure contract: id, name, theme, bounds, bays, spawns, plots, gallery, gold bay,
  resource islands, fan/pads, portal, chests, crafting/smelting, spectator home, `voidY`, `playBounds`.
- `world/gen/HarbourMarket` — first original map. Pure Java, `MapSink` only; no Bukkit. Build:
  - team build hall: 10 bays, each 3 plots + gold bay + 5 chests + crafting/stonecutter/smelting;
  - gallery hall: 7 pedestals + gold booth, selection pads, sightlines from the entrance;
  - mart: central fan, 8–10 resource islands (ores/logs/stones upper, colours lower), portal, pads;
  - spectator overview + backdrop; no branded text/logos.
- `world/MapPaster` — copy the TGTTOS `Paster` pattern: worker groups by chunk, async chunk load,
  plugin tickets, 15 ms/tick, stale-set clearing, progress callback.
- `world/MapWorld` — `McmBuild.voidWorld`, gamerules, baseY, dirty chunks, `wipe()`, entity tracking.
- `world/BuildMartMapRegistry` — build once per fixed seed, validate, list ids.
- `BuildMartCommand` — `/buildmart map build <id>` and `/buildmart map dump <id>` using
  `McmBuild.dumpRegion`; previews in `previews/`.

### 1.2 Plots and teams

- `team/TeamBay`, `team/TeamRegistry` — fixed bay assignment, membership, online check.
- `plot/BuildPlot`, `plot/BuildJob`, `plot/PlotRegistry`, `plot/PlotMatcher` — 3 slots per bay, region,
  reference anchor, state, dirty flag, BlockData compare, partial %.
- Move `onBreak`/`onPlace` to `listener/BuildMartListener`; resolve the owning plot with
  `PlotRegistry.plotAt(block)`; no global `inRegion` with one plot.
- Per-plot holograms: name/stars/materials above the plot; a raised reference behind regular plots;
  gold bay has no reference (placeholder "locked" until Phase 3).

### 1.3 Gallery

- `gallery/Blueprint`, `gallery/GalleryPedestal`, `gallery/GalleryService`:
  - 7 pedestals: 2x1★, 2x2★, 1x3★, 1x4★, 1x5★, plus a gold booth;
  - restock every 90 s; a blueprint claimed by a team does not restock for that team until the build
    completes;
  - selection: right-click the pedestal or its pad; if the team has a free plot, bind the preset and
    place the reference; otherwise action-bar "all plots busy";
  - selection feedback: title/action bar, sound, pedestal beam; no global removal unless researched
    (Q3).
- `build/PresetPool` — YAML/JSON presets; buckets by star band; draw without replacement per restock;
  seeded. Keep the 25 current shapes as seed content; expand to 40–60 original builds across 1–5★.

### 1.4 Runtime + HUD (minimum viable)

- `BuildMartRuntime` — `IDLE → LOADING → PREGAME → PLAYING → ENDING → CLEANUP`; `Waits` for pregame;
  `ctx.setTimer`; `McmFx.titleCard`; start/stop idempotent.
- `BuildMartHud` — sidebar: team coins, 3 slots with name/stars/progress, gallery restock timer;
  action bar per plot: name, placed/needed, wrong; gold placeholder line.
- `score/BuildMartScoring` — team coins via `addSidePoints`; star value by elapsed (10/15/20); partial
  completion; no Employee bonuses yet.
- `BuildMartGame` — implements `McmGame`, `onLateJoin` → spectator overview via
  `McmSpectator.Zone`, `maps()`, `forceNextMap`, `currentMap`, `skipWait`.

### 1.5 Keep temporarily

The current 27-material mart can stay as **placeholder resource nodes** on the new islands until Phase 2.
Do not keep the 8x4 stall grid or the shop counters; map them to island nodes.

### Acceptance tests

- [ ] `/mcm play buildmart` loads the new map, teleports teams to bays, and shows 3 empty plots.
- [ ] The gallery shows 7 blueprints with the right star composition; restock happens at 90 s.
- [ ] Selecting a blueprint fills a free plot and places a reference behind it; all 3 plots can run at
      once; a 4th selection is refused with a clear message.
- [ ] A completed build clears only that plot, frees the slot, updates coins and the sidebar.
- [ ] `/<id>map dump` produces a preview that a human can read; no branded MCC assets.
- [ ] Start → stop → start three times in a row leaves no stale blocks or entities.
- [ ] 10 teams + 20 spectators run at 20 TPS (spark/timings; no tick > 50 ms during paste after the
      first 2 s).
- [ ] A late joiner is a spectator at the overview and cannot build.

**Phase 1 is the demo milestone.** Do not add Gold or carts until it is stable.

---

## Phase 2 — Cart/list + travel + real resources (3–4 days)

**Goal:** the shopping-list book, the full resource-island loop, elytra/fan/pads, and carts as an
old-map variant (not on the current map).

### 2.1 Shopping list book

- `shop/BookItem` — `WRITTEN_BOOK`, PDC tag, title "Shopping List", author team; unbreakable and
  non-droppable; one per player on join/respawn; replace on selection changes.
- `shop/ShoppingList` — one page per selected build: name, stars, counts, "shop/craft/smelt" hint,
  "from memory" for gold. Add a "where to find" page by category (ores/logs/stones/colours/nether/
  coral/copper/garden/snow). Update on selection, completion, restock, and team change.
- Plot holograms repeat the same counts; the book is the portable copy.

### 2.2 Real resource islands

- `craft/ResourceService` — resource nodes from the map data, per-block respawn (default 60 ticks,
  config), break → give item → schedule respawn; if the block is non-air at respawn, retry instead of
  deleting the entry.
- `craft/ChestService` — 5 colour-coded chests per bay; starter fuel only, **no free build blocks**.
- `craft/CraftingService` / `craft/SmeltingService` — crafting tables, stonecutters, furnaces/smokers;
  vanilla recipes; fuel provided; protect the stations from break.
- Delete the four converter counters and `Jeweller`; delete the 8x4 stall grid.

### 2.3 Travel

- `travel/TravelService` — elytra in the mart, firework supply, no permanent flight in the mart, flight
  in the bay; equip/unequip on zone change.
- `travel/FanService` — central fan: radius check, tuned levitation/velocity impulse, cooldown, sound
  and particles. Do not copy the tutorial's `levitation 50 for 1 s`; tune it.
- `travel/LaunchPadService` — speed/jump pads: trigger, direction, cooldown, sound.
- `travel/FallService` — if below `voidY` in the mart: lose carried resources (not tools/book/cart),
  respawn at the mart entrance; if in the bay: respawn at bay spawn and keep items; clear cursor.
- `travel/CartService` — **old-map variant only** (`cart-enabled: true`):
  - cart item in the hotbar (PDC), place/enter a minecart on rails;
  - clamp to the rail network, set max speed, dismount at the end or on shift;
  - a gap dismounts and drops the player; falling loses carried resources;
  - cleanup every cart on stop; never leave carts in the world;
  - do not put carts on the current island map (owner target is the current map; carts are historical).

### 2.4 Acceptance tests

- [ ] The book is in every inventory, updates when a team selects/completes a build, and survives death/
      respawn (or is reissued).
- [ ] Every material in a preset is obtainable from a resource island, a craft, or a smelt; a preset
      validator refuses an unobtainable build.
- [ ] A resource block respawns after the configured delay; a block placed on the node does not delete
      the node forever.
- [ ] Elytra + fan can reach every island; flight is disabled in the mart; a player cannot fly from the
      bay to the mart.
- [ ] Falling in the mart loses carried resources but keeps tools/book; falling in the bay keeps items.
- [ ] Cart: enter, ride, dismount, fall in a gap, respawn, and no orphan carts after stop.
- [ ] No shop counters remain; crafting/smelting works with vanilla recipes.
- [ ] Tick time stays under 50 ms with 10 players mining/building; regen is bucketed, not a full scan.

---

## Phase 3 — Gold Build (1–2 days)

**Goal:** the 7★ gold build loop, gallery-only, memory-based, with fail-clear.

### Tasks

- `gold/GoldService` + `gold/GoldAttempt`:
  - first gold at 240 s; rotate every 120 s; 120 s window; config;
  - gold pool separate from 1–5★; 7★; no reference in the team bay; visible only at the gallery booth;
  - the gold plot is per-bay, separate from the 3 regular plots; it can be attempted while regular
    builds run;
  - on window end/fail: clear the gold plot, mark failed, no retroactive completion; play a fail sound/
    title; on success: award 7★ value, clear/free the bay, celebrate.
- HUD: gold countdown in the boss bar/top centre + sidebar line; `McmFx.title` on spawn/selection;
  gallery booth beam and `Golden Build selected!` broadcast.
- Book: gold page says "from memory — gallery only", lists materials, no layout.
- Scoring: gold 7★ at the current star value; no partial for gold (or config); no double award.

### Acceptance tests

- [ ] Gold first appears at 4:00 (or the fast-test equivalent), then every 2:00, with a 2:00 window.
- [ ] The gold build is visible in the gallery but has no reference in the team bay.
- [ ] A team can work on gold and regular builds at the same time.
- [ ] Failure clears the gold plot at the deadline; a late completion does not award.
- [ ] Success awards 7★ once, clears the plot, and frees the bay for the next gold.
- [ ] Two teams can attempt the same gold independently without sharing a plot or score.
- [ ] The boss bar/sidebar countdown matches the actual deadline (test with `/mcm skipwait`/debug).

---

## Phase 4 — Polish, balance, second map (2–3 days)

**Goal:** showcase quality and a playable second map.

### Tasks

- **Scoring:** star value 10/15/20 by elapsed; partial completion %; Employee of the Month
  (Entrepreneur most stars, Line Cook most builds, Quality Assurance highest average stars; 100/50/25;
  top 3; ties share); placement reveal via `McmFx.placementReveal`; fireworks for winners.
- **Map 2:** a second original theme (e.g., frost fair / night bazaar) using the same contract; random
  rotation; avoid repeating the last map; `maps()`/`forceNextMap`.
- **Visuals:** 3 depth layers, 25+ block types, weighted palettes, lighting, backdrop, no flat runs;
  `previews/` from two angles; iterate twice.
- **Spectator:** overview point, `McmSpectator.Zone`, confinement, no glass gallery; late join and
  finished players go there.
- **QoL:** night vision/saturation in the bay, recipe hints, "where to find" signs, portal labels,
  action-bar hints, sound pass, `McmFx.titleCard`, `Waits` in every non-gameplay delay.
- **Performance:** profile paste, wipe, regen, plot eval, HUD; target < 50 ms tick, < 5 ms HUD, no
  synchronous chunk loads in listeners; cap entities/labels.
- **Docs:** rewrite `README.md` with maps, rules, scoring, config defaults ("not researched"), commands;
  update `data/file_map.md` and this plan if the shape changed.
- **Tests:** bot harness for 4 teams + 20 spectators; start/stop/crash/rejoin; 12-minute soak; a full
  game with gold and a second map.

### Acceptance tests

- [ ] A full 12-minute game with 4 teams, 3 plots each, gold, a cart-less current map, and a second map
      runs without a tick > 50 ms or a stuck plot.
- [ ] Employee bonuses are correct, including ties and a team with zero builds.
- [ ] Partial builds pay a sensible percentage; a 90%-complete 5★ build is worth less than a completed
      5★ build but more than a 1★.
- [ ] The spectator zone keeps spectators in the arena; late joiners land there.
- [ ] A crash/restart leaves no stale blocks, chests, carts, labels or items.
- [ ] Two preview renders per map exist and have been critiqued/iterated twice.
- [ ] README/config list every default that is not researched.

---

## Cross-phase engineering rules

1. **One writer per block region.** Map load/wipe = `MapWorld`; plot = `BuildPlot`; resource node =
   `ResourceService`; nothing else writes.
2. **No synchronous chunk loads in listeners.** Keep map chunks ticketed while the game runs; plot
   regions are inside ticketed chunks.
3. **No per-tick full-volume scans.** Dirty flags for plots; bucketed regen; 1 Hz reconciliation.
4. **No `setCancelled(false)` at HIGH.** Own the world and ignore foreign worlds; use `MONITOR` only to
   observe.
5. **No `ctx.finish()` outside `BuildMartGame`.** Runtime returns a result; the game finishes.
6. **Every timer in config.** Durations, restock, gold, respawn, cooldowns, star values.
7. **Every non-researched value marked.** README "defaults, not researched" table; open questions in
   `README.md` §5.
8. **No third-party assets.** No MCC logos, map names, builds, textures, sounds or world downloads.
9. **Preview every map.** `McmBuild.dumpRegion` → `tools/render_preview.py`; keep PNGs in `previews/`.
10. **Stop is idempotent.** Cancel tasks, wipe, remove entities, release tickets, clear state; safe to
    call twice and after a crash.

## Suggested PR sequence

| PR | Contents | Gate |
|---|---|---|
| P0 | scoring, solo cap, BlockData, wipe, quit/rejoin, inventory | Phase 0 tests |
| P1a | map data + registry + paster + world + one generator + dump | map loads/dumps, no gameplay |
| P1b | 3 plots + team bays + matcher + HUD + spectator | 3 simultaneous builds |
| P1c | gallery + pedestals + selection + preset pool | 7 blueprints, restock, references |
| P2a | book + resource islands + chests/crafting/smelting | material loop closed |
| P2b | elytra/fan/pads + fall + flight zones | traversal loop |
| P2c | cart variant + old-map rails | cart tests, off by default |
| P3 | gold service + HUD + scoring | gold timeline/fail/success |
| P4 | employee bonuses + second map + polish + docs | full soak + previews |

## Definition of done

- The game is recognisably Build Mart: gallery choice, 3 builds at once, gold from memory, a mart you
  travel through, a book that tells you what to get, and team coins that match the event.
- Nothing in the game is a renamed version of the old ring/stalls/shops/sales.
- The map is original, previewed, and does not use MCC names, logos or layouts.
- A new developer can read `data/file_map.md` and know which file owns which behaviour.
- A full game survives start/stop, crash, rejoin, late join, 10 teams and 20 spectators.
