# File map — current and proposed

## 1. Current files

| File | Lines | Responsibility | Verdict | Notes |
|---|---:|---|---|---|
| `game-plugins/BuildMart/src/main/java/com/mcm/games/buildmart/BuildMartGame.java` | 1041 | Everything: `McmGame`, `Listener`, world creation, arena geometry, stalls/shops, plot/reference, scoring, HUD, lifecycle | **Split and rewrite** | 1041 lines is too large; map geometry, shops/gold and scoring are all fiction. Keep the lifecycle/listener skeleton, move the rest. |
| `.../BuildPreset.java` | 167 | 25 5x5x5 presets, legend, material counts, star score, support validator | **Rewrite as data** | Keep shapes as seed content; move to `BuildPresetData` + YAML/JSON; add BlockData, gold, sizes. |
| `.../BuildMartPlugin.java` | 46 | Bootstrap, world folder delete at enable, register game | **Keep, small edits** | Use `McmBuild.voidWorld`; move folder delete into `MapWorld`; add config load, command registration, map registry. |
| `.../VoidGenerator.java` | n/a | Empty chunk generator | **Delete** | Use `com.mcm.api.McmBuild.VoidGenerator`. |
| `.../resources/plugin.yml` | n/a | Plugin metadata, command/permission | **Keep, extend** | Add `buildmart` admin command, `buildmart.admin` permission. |
| `pom.xml` | n/a | Build config | **Keep** | Add SnakeYAML/Jackson only if needed; prefer Bukkit `YamlConfiguration` to avoid deps. |
| `README.md` | 33 | Player-facing rules | **Rewrite after code** | Remove sales/shops/gold-shop/one-plot fiction; document maps, gallery, gold, cart toggle, config defaults. |
| `target/` | n/a | Build output | **Ignore** | Do not commit new jars unless the repo does that already. |

### 1.1 `BuildMartGame.java` section map

| Lines | Section | Verdict | Destination |
|---:|---|---|---|
| 42–73 | Class doc, constants, `RAW_STALLS`, `SOLO_COLORS` | Delete stalls; keep `SOLO_COLORS` | `BuildMartConfig`, `TeamRegistry` |
| 75–118 | `Stall`, `Cost`, `Shop`, `Regen`, `Area` models | Delete `Stall/Cost/Shop`; replace `Regen`; rewrite `Area` | `world/ResourceNode`, `team/TeamBay` |
| 120–148 | State fields (`arenaBlocks`, `stallBlocks`, `shopBlocks`, `regen`, `areas`, `sideOf`, `sequence`, `finishers`) | Rewrite | `MapWorld`, `TeamRegistry`, `PlotRegistry`, `GalleryService` |
| 150–159 | `McmGame` identity (`id`, name, icon, description, scoreboard) | Keep | `BuildMartGame` |
| 163–194 | `ensureWorld` + gamerules | Rewrite | `world/MapWorld.ensure()` |
| 196–250 | `start` (stop, world, build sequence/mart, ring areas, prep, timer, task) | Rewrite | `BuildMartRuntime.start()` |
| 252–280 | `stop`, `endGame` | Keep skeleton | `BuildMartRuntime.stop/end` |
| 284–324 | `key`, `forceChunk`, `setType`, `put`, `label` | Keep utilities, move | `world/BlockKey`, `MapWorld`, `hud/Labels` |
| 326–393 | `buildMart` floor, stalls, shops | Delete | `world/gen/*` |
| 395–398 | `stallText`/sale label | Delete | — |
| 400–441 | `buildArea` platform, rings, spawn, path, labels | Delete | `world/gen/*` |
| 443–445 | `label(side)` | Keep | `BuildMartMessages`/`McmFx` |
| 447–452 | `clearRegion` 5x5x5 | Keep but generalize | `plot/PlotRegion`, `MapWorld` |
| 454–476 | `presetAt`, `buildSequence`, `appendShuffle` | Delete shared order | `build/PresetPool` |
| 478–500 | `showTarget` reference placement + hologram | Rewrite | `plot/BuildPlot.bind`, `gallery/ReferencePedestal` |
| 502–534 | `cleanupArena` | Rewrite | `world/MapWorld.wipe()` |
| 536–573 | tools, `isTool`, `preparePlayer` | Keep tools; rewrite travel | `util/ItemKeys`, `travel/TravelService`, `util/PlayerStateService` |
| 575–587 | `resetPlayers` | Rewrite | `util/PlayerStateService` |
| 589–610 | `areaOf`, `members`, `inOurWorld`, `inRegion` | Keep concept | `team/TeamRegistry`, `plot/PlotRegistry` |
| 612–632 | `tickLoop` | Rewrite | `BuildMartRuntime.tick()` |
| 634–648 | `processRegen` | Rewrite | `world/ResourceService` |
| 650–667 | `voidCheck` | Rewrite | `travel/FallService` |
| 669–726 | `second` (pregame, sales, evaluate, HUD, end) | Rewrite | `BuildMartRuntime`, `GalleryService`, `GoldService`, `BuildMartHud` |
| 728–741 | `startSale` | Delete | — |
| 743–760 | `evaluate` | Rewrite | `plot/PlotMatcher` |
| 762–787 | `complete` | Rewrite | `plot/BuildPlot`, `score/BuildMartScoring` |
| 789–800 | `sendShoppingList` | Rewrite | `shop/ShoppingList` |
| 802–815 | `updateSidebar` | Rewrite | `hud/BuildMartHud` |
| 817–831 | `awardFinalBonuses` | Rewrite | `score/EmployeeBonus` |
| 833–995 | Listeners (break/place/interact/damage/food/drop/leaves/fade/spread/falling) | Split | `listener/BuildMartListener`, `listener/PlayerListener` |
| 997–1040 | legacy text, title, fmt, nice, `concreteFor` | Keep | `BuildMartMessages`, `util/Materials` |

### 1.2 `BuildPreset.java` section map

| Lines | Section | Verdict | Destination |
|---:|---|---|---|
| 7–15 | Doc, `SIZE=5`, `SHOP_MATERIALS` | Rewrite | `build/BuildPresetData` |
| 17–49 | `LEGEND` 30 chars | Rewrite | `build/BlockPalette` (BlockData strings) |
| 51–57 | fields: name, sx/sy/sz, ox/oz, blocks, stars, counts, grid | Rewrite | `BuildPresetData` (add `gold`, `band`, bounds) |
| 59–91 | constructor, ragged checks, counts, star score | Keep logic | `BuildPresetData` |
| 94–99 | `at(x,y,z)` | Keep, return BlockData | `BuildPresetData.at` |
| 102–105 | `columnUsed` | Keep | `BuildPresetData` |
| 107–109 | `starString` | Keep | `BuildMartMessages.stars` |
| 111–130 | `unsupported` flood fill | Keep + extend | `PresetValidator` (support, gravity, block entities, obtainable) |
| 132–166 | 25 build literals | Keep as seed | `resources/builds/*.yml` + loader |

### 1.3 `BuildMartPlugin.java` section map

| Lines | Section | Verdict | Destination |
|---:|---|---|---|
| 14–16 | plugin fields | Keep | add `BuildMartConfig`, `BuildMartGame` |
| 17–27 | `onEnable`: folder delete, register game | Keep + extend | delegate to `MapWorld`, load config/maps/presets, register command |
| 29–34 | `onDisable` stop | Keep | idempotent |
| 36–45 | `deleteFolder` | Keep | `world/MapWorld.deleteFolder` |

## 2. Proposed files

Package root: `com.mcm.games.buildmart`. Sizes are estimates to keep each file reviewable.

### 2.1 Entry points and runtime

| File | ~Lines | Responsibility | Depends on |
|---|---:|---|---|
| `BuildMartPlugin.java` | 80 | Bootstrap: config, map registry, preset registry, command, `McmApi.registerGame` | `McmApi`, `MapWorld` |
| `BuildMartGame.java` | 180 | `McmGame` implementation: identity, `start/stop`, `useCoreScoreboard`, `onLateJoin`, `skipWait`, `maps`, `forceNextMap`, `currentMap` | `BuildMartRuntime`, `McmGameContext` |
| `BuildMartRuntime.java` | 350 | State machine, 20 Hz tick, phase transitions, task ownership, broadcast/title flow | `MapWorld`, `TeamRegistry`, `GalleryService`, `GoldService`, `BuildMartHud`, `BuildMartScoring` |
| `BuildMartConfig.java` | 220 | Typed config (map, timings, gallery, gold, plots, travel, scoring, resources, spectator) + validation + defaults | `YamlConfiguration` |
| `BuildMartMessages.java` | 180 | All legacy/Adventure text, star strings, book text, broadcast builders | `McmFx.text` |
| `BuildMartCommand.java` | 160 | `/buildmart map build|dump|list`, `/buildmart reload`, `/buildmart debug`; permission `buildmart.admin` | `MapRegistry`, `McmBuild.dumpRegion` |

### 2.2 World / map (pure data + paste + wipe)

| File | ~Lines | Responsibility | Notes |
|---|---:|---|---|
| `world/BuildMartMap.java` | 220 | Pure-data map contract: id, display name, theme, bounds, team bays, spawns, plot slots, gallery pedestals, resource islands, fan/pads, portal, chests, crafting/smelting, gold bay, spectator home, void Y, play bounds | No Bukkit types. Like `TgMapData`/`MapModel`. |
| `world/BuildMartMapData.java` | 180 | Sparse `BlockData` string store + `Region`, `Pos`, `Spawn`, `ResourceNode`, `ChestSpot`, `PlotSlot`, `GallerySlot`, `TravelNode` records; bounds/validation | `BlockSink` style; `forEach` visitor |
| `world/BuildMartMapRegistry.java` | 160 | Load generators by class list/jar scan; build once per fixed seed; validate; `playable()`, `byId()`, `ids()` | Copy `Tgttos/MapRegistry` shape |
| `world/MapPaster.java` | 300 | Worker groups writes per chunk; main-thread `getChunkAtAsync` with a few in flight; plugin chunk tickets; 15 ms/tick; clear stale positions from the previous paste; progress callback | Copy `Tgttos/Paster`; do not re-invent |
| `world/MapWorld.java` | 300 | Owns world, `McmBuild.voidWorld`, gamerules, baseY, dirty chunk set, paster, `wipe(progress, done)`, chunk tickets, entity tracking | Copy `SkyBlockle/MapWorld` cleanup parts |
| `world/MapGenerator.java` | interface | `BuildMartMap build(long seed)`; pure Java; offline-runnable | `MAP_BRIEF` contract |
| `world/gen/HarbourMarket.java` | 600–1200 | First original map: build hall + mart + islands + fan + pads + portal + spectator | Keep pure; use `MapSink`, own noise/palette helpers |
| `world/gen/FrostFair.java` | 600–1200 | Second original map, later | Same |
| `world/gen/MapSink.java` | 30 | `set(int x,int y,int z,String blockData)` / `fill` / `box` helpers | Shared by runtime and offline dump |
| `world/MapDump.java` | 120 | Offline `main` that runs a generator into a dump (`MCMDUMP 1 ...`) for `tools/render_preview.py` | Test/tools source dir |

### 2.3 Teams, plots, gallery, gold

| File | ~Lines | Responsibility |
|---|---:|---|
| `team/TeamBay.java` | 140 | Bay origin, spawn, portal, 3 `BuildPlot` refs, gold plot, 5 chests, crafting/smelting stations, label anchors |
| `team/TeamRegistry.java` | 160 | Assign `ctx.sides()` to bays, membership, online check, team coins/stars, disconnect/rejoin |
| `plot/BuildPlot.java` | 240 | Slot index, region, owner bay, `BuildJob`, state (`EMPTY/BUILDING/COMPLETE/FAILED`), bind/clear/complete, dirty flag, reference anchor |
| `plot/PlotMatcher.java` | 140 | Canonical `BlockData` compare over preset bounds; correct/needed/wrong/percent; air handling; state normalization |
| `plot/PlotRegistry.java` | 120 | `plotAt(block)`, all plots, per-bay free slot, per-team completion counts |
| `plot/BuildJob.java` | 80 | Preset, start tick, attempt, gold flag, completed tick, partial percent |
| `gallery/Blueprint.java` | 90 | Preset + pedestal + star band + claimed-by-team set + `availableFor(team)` |
| `gallery/GalleryService.java` | 260 | Build 7 pedestals; rotate every 90 s; keep claimed builds; select(team, blueprint) → free plot; broadcast; sound/title; gold booth |
| `gallery/GalleryPedestal.java` | 120 | Physical pedestal, display entity blocks, name/stars/materials hologram, selection pad, claim beam |
| `gold/GoldService.java` | 180 | First at 240 s, rotate every 120 s, 120 s window, 7★ pool, no team reference, fail clear, global boss bar/title |
| `gold/GoldAttempt.java` | 60 | Team, preset, deadline, plot, state |

### 2.4 Building, book, crafting, travel

| File | ~Lines | Responsibility |
|---|---:|---|
| `build/BuildPresetData.java` | 260 | BlockData grid, size, name, stars/band, counts, gold flag, `at`, `columnUsed`, `bounds` |
| `build/PresetPool.java` | 180 | Load YAML/JSON presets; validate; buckets by star band; draw without replacement per restock; seeded |
| `build/PresetValidator.java` | 160 | Support flood fill, gravity blocks, block entities, unobtainable materials, size, air-only, gold 7★ |
| `shop/ShoppingList.java` | 220 | Per-player book state; build pages from selected plots; update on selection/completion/restock; "where to find" hints |
| `shop/BookItem.java` | 80 | `WRITTEN_BOOK` creation, PDC tag, title/author, unbreakable/non-droppable |
| `craft/CraftingService.java` | 120 | Place/register crafting tables/stonecutters; allow vanilla recipes; no custom converter |
| `craft/SmeltingService.java` | 100 | Furnaces/smokers, fuel provided, hopper-free; protect from break |
| `craft/ChestService.java` | 100 | 5 colour-coded chests per bay; fill with starter fuel/tools? (no free build blocks) |
| `craft/ResourceService.java` | 220 | Resource island nodes, per-block respawn, hit/break handling, respawn particles, counts |
| `travel/TravelService.java` | 160 | Elytra equip/unequip, firework supply, flight zone, fall handling |
| `travel/FanService.java` | 120 | Central fan: radius check, levitation/velocity impulse, sound/particles, cooldown |
| `travel/LaunchPadService.java` | 120 | Pads: pressure plate/trigger, velocity + sound; speed pads; cooldown |
| `travel/CartService.java` | 260 | Optional old-map variant: cart item, spawn/enter/exit minecart, rail clamp, speed, gap fall, cleanup |
| `travel/FallService.java` | 120 | Void check, lose carried resources, respawn per zone, cursor clear, message/sound |

### 2.5 HUD, scoring, listeners, utils

| File | ~Lines | Responsibility |
|---|---:|---|
| `hud/BuildMartHud.java` | 220 | Sidebar (coins, slots, stars, gold/restock timers), action bar per plot, boss bar, throttled updates |
| `hud/Labels.java` | 100 | TextDisplay pool/spawn/update/remove; chunk force; respawn on load |
| `score/BuildMartScoring.java` | 180 | Star value by elapsed, partial %, gold, per-team totals, completion order |
| `score/EmployeeBonus.java` | 140 | Entrepreneur/Line Cook/Quality Assurance; top 3; ties; broadcast + payout via `addSidePoints` |
| `listener/BuildMartListener.java` | 280 | Break/place/interact/portal/gallery/pad/fan/chest/craft; cancel outside allowed regions |
| `listener/PlayerListener.java` | 160 | Join/quit/respawn/teleport/vehicle/inventory click; late join; state reset |
| `listener/WorldListener.java` | 80 | Explosions, pistons, fluids, block entities, item frames, portal creation |
| `util/ItemKeys.java` | 60 | NamespacedKeys: tool, cart, book, selector; helpers |
| `util/PlayerStateService.java` | 140 | Prepare/reset inventory, armour, cursor, effects, flight, vehicle, PDC |
| `util/InventoryUtil.java` | 100 | `canFit`, `addOrDrop`, rollback helpers |
| `util/BlockKey.java` | 40 | Packed long key + chunk key |
| `util/Materials.java` | 80 | `nice`, concrete/wool helpers, canonical BlockData string |

## 3. Dependency direction

```
BuildMartPlugin
  -> BuildMartGame (McmGame)
       -> BuildMartRuntime
            -> MapWorld -> MapPaster, BuildMartMapRegistry -> BuildMartMapData
            -> TeamRegistry -> TeamBay -> BuildPlot -> PlotMatcher, BuildJob
            -> GalleryService -> Blueprint, GalleryPedestal
            -> GoldService -> GoldAttempt
            -> ResourceService, CraftingService, ChestService
            -> TravelService -> FanService, LaunchPadService, CartService, FallService
            -> ShoppingList -> BookItem
            -> BuildMartHud, Labels
            -> BuildMartScoring, EmployeeBonus
       -> BuildMartListener, PlayerListener, WorldListener (registered by plugin/game)
```

Rules:
- `world/*` and `build/BuildPresetData` must stay free of `McmGameContext` and `Player`.
- `BuildMartGame` is the only class that calls `ctx.finish()`.
- `BuildMartRuntime` owns every scheduled task and cancels them in `stop()`.
- `MapWorld` is the only class that touches chunks/blocks for map load/wipe.
- `BuildPlot` is the only class that writes inside a plot region.
- `BuildMartScoring` is the only class that calls `addSidePoints`.

## 4. Core API mapping

| Need | Core API | Where |
|---|---|---|
| Register game | `McmApi.registerGame(plugin, game)` | `BuildMartPlugin.onEnable` |
| Sides/teams/solo | `ctx.sides()`, `ctx.teamOf`, `ctx.sideLabel`, `ctx.teamColor` | `TeamRegistry` |
| Points | `ctx.addSidePoints(side, points, reason)` | `BuildMartScoring` |
| Timer | `ctx.setTimer(secondsLeft, total)` | `BuildMartHud`/runtime |
| Sidebar | `ctx.setSidebarLines(lines)` | `BuildMartHud` |
| Finish once | `ctx.finish(reason)` | `BuildMartGame.end` |
| Late join | `McmGame.onLateJoin` | `BuildMartGame` |
| Map selection | `maps()`, `forceNextMap(id)`, `currentMap()` | `BuildMartGame` |
| Skip pregame | `skipWait()` | `BuildMartGame` → runtime |
| Void world | `McmBuild.voidWorld(name)` | `MapWorld` |
| Tick-spread build | `McmBuild.BuildQueue` | `MapWorld` (small maps) |
| Shapes/palettes/noise | `McmBuild.fill/hollowBox/sphere/...`, `Palette`, `Noise`, `floatingIsland` | `world/gen/*` |
| Preview dump | `McmBuild.dumpRegion` | `BuildMartCommand` |
| Titles/sounds/fireworks | `McmFx.title`, `actionBar`, `sound`, `teamFirework`, `celebrate`, `placementReveal` | HUD/runtime |
| Spectator | `McmSpectator.Zone`, `makeSpectator` | `PlayerListener`/runtime |
| Fast-test waits | `Waits.ticks/seconds/fast` | runtime/gold/gallery |

## 5. Migration order (file-level)

1. **Freeze the old game**: leave `BuildMartGame` compiling; add `BuildMartConfig` and delete the sale/shop
   paths behind a feature flag if a quick build is needed.
2. **Land the world layer first**: `BuildMartMapData`, `BuildMartMap`, `MapRegistry`, `MapPaster`,
   `MapWorld`, one generator; no gameplay change yet (build/dump only).
3. **Land plots and teams**: `TeamBay`, `TeamRegistry`, `BuildPlot`, `PlotMatcher`, `PlotRegistry`;
   move `evaluate/complete` into them; keep a single plot per bay until the map has 3.
4. **Land gallery + book**: `Blueprint`, `GalleryService`, `GalleryPedestal`, `ShoppingList`, `BookItem`;
   remove `sequence`/`showTarget`.
5. **Land travel + resources**: `ResourceService`, `TravelService`, `FanService`, `LaunchPadService`,
   `FallService`, `CraftingService`, `ChestService`; delete stalls/shops.
6. **Land gold**: `GoldService`, `GoldAttempt`; add gold pool and sidebar/boss bar.
7. **Land scoring + HUD**: `BuildMartScoring`, `EmployeeBonus`, `BuildMartHud`; remove `addPoints` loops.
8. **Land cart** as an optional map variant: `CartService`; do not put carts on the current map.
9. **Land polish**: labels, spectator, late join, previews, README, config docs, tests.

At every step the plugin must still start and stop cleanly; do not leave two systems writing the same
blocks.
