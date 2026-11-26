# Current vs target — Build Mart

Line numbers refer to `game-plugins/BuildMart/src/main/java/com/mcm/games/buildmart/BuildMartGame.java`
unless another file is named. "Target" is the real MCC game as sourced in `README.md` §2.

Verdicts: **KEEP** (reuse as is), **REWRITE** (same job, new implementation), **DELETE** (not in the
real game), **NEW** (no current equivalent).

## 1. One-screen comparison

| Area | Current | Target | Verdict |
|---|---|---|---|
| Map | Code-built 51x27 mart floor + ring of 17x11 platforms (`:208–229`, `:326–441`) | Two areas: team build hall + separate mart with central fan and resource islands; portal between them | DELETE current layout; NEW `BuildMartMap` + paster |
| Teams | One ring platform per side; number of sides = participant sides (up to 40 solos); 12 solo colours (`:208–229`, `:69–73`) | Fixed team bays (up to 10) + gold bay; separate compact solo/test layout | REWRITE `Area` → `TeamArea`; NEW solo layout |
| Plots | 1 plot + 1 reference pedestal per side (`:102–118`, `:478–500`) | 3 regular plots + 1 gold plot per team, each with its own reference (regular only) | REWRITE |
| Builds | One shared sequence, one build at a time (`:454–476`, `:762–787`) | 7-plot gallery, team selects up to 3, 3 simultaneous builds, gold separate | DELETE sequence; NEW gallery + per-plot jobs |
| Gallery | None | 7 plots: 2x1★, 2x2★, 1x3★, 1x4★, 1x5★, plus gold; restock 90 s; claimed builds don't restock until done | NEW |
| Reference | Physical copy on a pedestal beside the plot (`showTarget` `:478–500`) | Copy in the team area in front of the plot; gold has no team copy | REWRITE |
| Materials | 27 flat stalls, 2x2x3 columns, 3 s regen (`:59–67`, `:354–392`, `:634–648`) | Resource islands: upper ores/logs/stones, lower colours; per-block respawn; nether/coral/copper/garden/snow variants | DELETE stalls; NEW islands |
| Shops | 4 right-click converter counters; Glass/Mason/Kiln/Jeweller (`:85–95`, `:346–352`, `:890–940`) | Crafting tables, stonecutters, 2 smelting areas, 5 colour-coded chests; no converter counters | DELETE shops; NEW crafting/smelting stations |
| Gold | A shop product: 1 Iron + 1 Glowstone → 2 Gold Blocks (`:350`) | 7★ gold build, gallery only, first at 4:00, every 2:00, 2:00 to complete from memory, fail clears | DELETE shop gold; NEW gold build state |
| Travel | Creative flight everywhere (`:557–573`) | Elytra + central fan + launch/speed pads; fly only in build bay; falling costs carried materials | REWRITE |
| Cart | None | Old map: hotbar minecart/trolley on rails, faster/less control, pitfalls; travel only | NEW optional old-map variant |
| Shopping list | One chat line (`:789–800`) | Written book in every inventory + plot holograms | REPLACE with `ShoppingList`/book |
| HUD | Sidebar "builds completed" + action bar placed/needed | Sidebar: team coins, build slots with names/stars, gold timer; action bar per plot; boss bar timer | KEEP plumbing, REWRITE content |
| Scoring | Per-member full points, 4+3*stars, speed, order, +10/6/3 (`:762–831`) | Team coins, star value 10/15/20 by elapsed, partial %, gold 7★, Employee of the Month 100/50/25 | REWRITE |
| Completion | Material-only, 5x5x5, polled every second (`:743–760`) | BlockData-aware, per-plot, dirty-driven, partial % | REWRITE |
| World | Void world, code rebuild, partial cleanup (`:163–194`, `:502–534`) | Owned world, full region wipe, autosave off, stale-safe | REWRITE |
| Spectator | None; core default for late join (README) | Spectator overview + zone, no glass gallery | NEW |
| Late join | Default spectator, not handled (README) | Assign spectator overview, or team if configured | NEW `onLateJoin` |
| Map selection | One implicit map | 2–3 original maps, `maps()`/`forceNextMap()` | NEW |
| Preview | None | `/<id>map build|dump <map>` via `McmBuild.dumpRegion` | NEW |

## 2. KEEP — reuse, with small changes

| # | What | Where | Why | Change |
|---|---|---|---|---|
| K1 | Lifecycle skeleton: `start` → `stop` first; `stop` idempotent; `endGame` before `finish`; cleanup after finish | `:196–280` | Matches `McmGame` contract | Keep; move cleanup into a `MapWorld`/wiper |
| K2 | Core HUD plumbing: `useCoreScoreboard`, `setTimer`, `setSidebarLines`, action bars | `:159`, `:241–242`, `:696–724`, `:802–815` | Correct integration | Keep calls; replace content via `BuildMartHud` |
| K3 | Side/team plumbing: `ctx.sides`, `sideOf`, `members`, `sideLabel`, team colour | `:208–229`, `:589–601`, `:443–445` | Handles teams + solos | Move into `TeamArea`/`TeamRegistry`; use `McmFx.sideKeyOf` |
| K4 | Player prep: clear inventory/armour, tools, food, fire, fall, haste | `:557–573` | Right baseline | Keep; add elytra/firework rules, flight zone |
| K5 | Void fall penalty: lose carried items, keep tools, respawn with message/sound | `:650–667` | Matches MCC "fall, lose materials" | Keep concept; per-area respawn, clear cursor, no loss in build bay |
| K6 | Protection listeners: damage, food, drops, leaf decay, fade, spread, falling block | `:957–995` | Prevents arena damage | Keep; add explosions, pistons, fluids, item frames, portal creation |
| K7 | World gamerules (mob spawning, daylight, weather, fall damage, keep inventory, random tick, fire tick, tile drops, mob griefing, immediate respawn) | `:163–194` | Mostly correct | Replace body with `McmBuild.voidWorld` + missing rules (`SPECTATORS_GENERATE_CHUNKS`, `SHOW_DEATH_MESSAGES`, autosave off) |
| K8 | Preset parser/validator ideas: legend, layers, `at`, `columnUsed`, `unsupported`, star scoring | `BuildPreset.java:59–130` | Good base | Rewrite as `BuildPresetData` with BlockData, bigger sizes, gravity/orientation checks, gold flag |
| K9 | 25 original build shapes | `BuildPreset.java:136–166` | Original content, useful | Keep as seed content; add more; no MCC build copies |
| K10 | Tool PDC + unbreakable + efficiency | `:538–555` | Prevents tool loss/dupe | Keep; add cart item and book PDC keys |
| K11 | Scoreboard sorting by builds completed | `:802–815` | Simple standings | Keep as fallback; replace with coins/stars/slots |
| K12 | Solo path | `:69–73`, `:208–229` | Needed for testing | Keep as a separate `solo` layout; never put 40 solos on the team ring |
| K13 | Per-side broadcast on completion | `:774–775` | Good feedback | Keep; use `McmFx` + team colour; add partial/gold variants |

## 3. REWRITE — same job, new implementation

| # | Current | Where | Target | Notes |
|---|---|---|---|---|
| R1 | `Area` = 1 plot + 1 pedestal + counters | `:102–118` | `TeamArea` = bay origin, 3 `BuildPlot`, 1 `GoldPlot`, spawn, portal link, 5 chests, crafting/smelting stations | Coordinates from `BuildMartMap`, not computed from a ring |
| R2 | `buildArea()` draws a 17x11 platform, rings, spawn pad, path | `:400–441` | Map generator builds the bay; runtime only binds plot regions and spawn | Delete geometric drawing from the game |
| R3 | `showTarget()` clears target, writes reference, builds hologram text | `:478–500` | `BuildPlot.bind(preset)` places a reference in the pedestal region, updates a per-plot hologram, sets `buildStartTick`, resets dirty/partial state | Reference placement is map-defined; no world writes outside the plot/pedestal regions |
| R4 | `evaluate()` 5x5x5 Material compare | `:743–760` | `PlotMatcher.evaluate(plot)` compares canonical `BlockData` over the preset bounds; returns correct/needed/wrong/percent; called on dirty + 1 Hz reconcile | Ignore only air; compare orientation; skip `waterlogged` only if the preset does not set it |
| R5 | `complete()` scoring + clear + advance | `:762–787` | `BuildPlot.complete()`: compute stars/coins/partial, award via `addSidePoints`, clear plot, free slot, notify gallery, update book/HUD | Order bonus per build instance or drop |
| R6 | `sendShoppingList()` chat line | `:789–800` | `ShoppingList.update(player)` writes a `WRITTEN_BOOK` with one page per selected build (name/stars/counts, gold note), plus a short "where to find" hint | Book is per-player; update on selection/completion/restock |
| R7 | `updateSidebar()` builds a list from `presetAt(seqIndex)` | `:802–815` | `BuildMartHud`: team coins, 3 slots with name/stars/progress, gold timer, next gallery restock; throttle to 4–10 Hz | Do not rebuild a scoreboard per tick |
| R8 | `awardFinalBonuses()` ranks by builds only | `:817–831` | `BuildMartScoring.awardEmployeeOfTheMonth`: Entrepreneur (most stars), Line Cook (most builds), Quality Assurance (highest average stars), 100/50/25, top 3, ties share | Broadcast each category with `McmFx` |
| R9 | `cleanupArena()` resets only recorded blocks, removes all entities, unforces chunks | `:502–534` | `MapWorld.wipe()` clears every dirty chunk (map footprint + player edits), removes non-player entities, releases tickets, clears state | Sky Blockle pattern; stale-safe after crash |
| R10 | `startSale()` every 75 s, `SALE_*` constants, sale labels | `:51–54`, `:395–398`, `:704–711`, `:728–741` | Delete. Replace with `GalleryService.rotate()` every 90 s and `GoldService` at 4:00/2:00 | "Big Sales" is the game name, not a mechanic |
| R11 | `Shop`/`Cost`/`shopBlocks`/`shopCooldown` and `onInteract` conversion | `:85–95`, `:346–352`, `:890–940`, `:942–950` | `CraftingService` (vanilla crafting table/stonecutter/furnace/smoker), `ChestService` (5 colour-coded chests), `PortalService` (build area ↔ mart) | No right-click "2 sand → 1 glass"; use real recipes or a documented custom recipe set |
| R12 | `onBreak`/`onPlace` single-plot checks | `:835–888` | `PlotRegistry.plotAt(block)` returns the owning plot; allow break/place only inside that plot; return blocks on break; mark dirty | Also handle falling blocks, block entities, item frames |
| R13 | `voidCheck()` teleports to `a.spawn` and clears all non-tools | `:650–667` | `FallService`: if in mart, respawn at mart entrance and lose carried resources; if in bay, respawn at bay spawn and keep; clear cursor; message per case | Cart falls: dismount, lose carried resources |
| R14 | `preparePlayer()` creative flight everywhere | `:557–573` | `TravelService`: elytra + firework in mart, flight only in bay, fan/pad impulses | Use `McmFx` sounds; no permanent `allowFlight` in mart |
| R15 | `resetPlayers()` | `:575–587` | `PlayerStateService.reset` on stop/quit: clear inventory/cursor/armour, effects, flight, vehicle, PDC items | Handle `PlayerQuitEvent` and `onLateJoin` |
| R16 | `ensureWorld()` custom body | `:163–194` | `MapWorld.ensure()` using `McmBuild.voidWorld`, set missing rules, autosave off, save-none | Avoid duplicate world code |
| R17 | `start()` ring math | `:208–229` | `MapRegistry`/`BuildMartMap` provides fixed team bays and spawns; runtime assigns sides to bays in `ctx.sides()` order | Max 10 team bays; solo layout separate |
| R18 | `buildSequence()` shared order | `:459–476` | `PresetPool` per star band; gallery draws without replacement per restock; gold pool separate | Seeded, replayable, no repeats within a restock |
| R19 | `BuildPreset` 5x5x5 Material grid | `BuildPreset.java:11–130` | `BuildPresetData` with `BlockData[][][]`, size up to 7x7x7 (config), star band, gold flag, counts, `supported()`, `gravityBlocks()`, `blockEntities()` | Data in YAML/JSON, not Java literals |
| R20 | `isTool()` / tool PDC | `:551–555` | `ItemKeys`: tool, cart, book, plot-selector; all PDC-tagged, unbreakable, non-droppable | One key per item type |
| R21 | `hasRoom()` approximate | `:942–950` | `InventoryUtil.canFit` + check `addItem` leftovers; if leftover, do not consume costs / roll back | Prevents item loss |
| R22 | `world` field + `inOurWorld` | `:127`, `:603–605` | `MapWorld` owns world; listeners ask `mapWorld.isActive()` | Null world on stop |

## 4. DELETE — not in the real game

| # | What | Where | Why |
|---|---|---|---|
| D1 | 75 s "Big Sale" events + SALE regen + sale labels | `:51–54`, `:395–398`, `:704–711`, `:728–741` | "Big Sales at Build Mart" is the title; the live timers are gallery restock (90 s) and gold (4:00/2:00) |
| D2 | Four shop counters (Glassworks/Mason/Kiln/Jeweller) | `:85–95`, `:346–352`, `:890–940` | Real game uses crafting tables/stonecutters/smelters + chests; no right-click converters |
| D3 | Jeweller → Gold Block | `:350` | Gold is a build, not a shop material; current preset `$` is only one gold trophy |
| D4 | Flat 8x4 stall grid + 27 `RAW_STALLS` | `:59–67`, `:354–392` | Replaced by resource islands with categories and respawn |
| D5 | One shared `sequence` and `presetAt(seqIndex)` | `:147`, `:454–476`, `:719`, `:745`, `:763`, `:790` | Replaced by gallery selection + per-plot jobs |
| D6 | `finishersPerIndex` order bonus | `:148`, `:764` | Meaningless with 3 simultaneous, team-chosen builds |
| D7 | Per-member `addPoints` for a team build | `:770–771` | Use `addSidePoints` for team coins |
| D8 | Chat shopping list | `:789–800` | Replace with book + holograms |
| D9 | Hardcoded mart floor constants `MART_X/MART_Z` and floor loop | `:57`, `:326–343` | Map data owns geometry |
| D10 | Physical pedestal target next to every plot | `:105`, `:116`, `:440`, `:478–500` | Replace with map-defined reference pedestals for regular plots; gold has none |
| D11 | `VoidGenerator.java` | whole file | Use `McmBuild.VoidGenerator` from the core |
| D12 | "Fly everywhere" | `:570` | Trivialises the mart; elytra/fan only outside the bay |
| D13 | `README.md` claims about sales/shops/gold shop | README lines 9–20 | Update after rewrite; do not ship fiction |

## 5. Risk register (line-level)

Severity: **S1** breaks the game/economy, **S2** wrong behaviour/exploit, **S3** polish/performance.

| # | Sev | Location | Risk | Fix |
|---|---|---|---|---|
| A1 | S1 | `:770–771` | Every member gets full build points → 4x team coins; standings wrong | `ctx.addSidePoints(side, pts, ...)`; test a 4-player vs solo team |
| A2 | S1 | `:743–760` | Material-only compare: wrong stair/slab/log orientation completes a build | Compare canonical `BlockData`; add a test with a rotated stair |
| A3 | S1 | `:502–534` | Partial cleanup: crash/reload leaves stale blocks, chests, item drops | Own the world; dirty-chunk wipe; autosave off; delete folder at enable |
| A4 | S2 | `:208–229` | 40 solos → radius ~147, ~300-block arena, long paths, 12 colours repeat, heavy synchronous build | Fixed team bays (max 10) + compact solo grid; cap sides |
| A5 | S1 | `:713` | `evaluate()` runs for areas with no online members; ghost team completes and takes order bonus | Skip/disable area when `members(side).isEmpty()`; end team if empty for N seconds |
| A6 | S1 | `:890–940` | Shop x16 loop + `hasRoom` can leave `addItem` leftovers on the floor; costs already removed | Check `addItem` return; roll back or drop at player feet with a message |
| A7 | S1 | `:650–667` | Void clears slots but not cursor; item on cursor survives (duplication/loss) | `p.setItemOnCursor(null)`; clear armour; close inventory |
| A8 | S2 | `:846`, `:885` | `setCancelled(false)` at HIGH un-cancels another plugin's protection | Do not force false; ignore foreign worlds; document exclusive world ownership |
| A9 | S2 | `:634–648` | Regen removes the entry if the block is non-air; a block placed/fallen on a stall permanently deletes the resource | If non-air, leave the entry pending or restore only if the block is still the stall material; log |
| A10 | S2 | `:835–888` | With 3 plots, `areaOf(p)` returns one `Area` and `inRegion` checks only its single plot; a player cannot break/place on their other two plots, and a plot-owned check can be bypassed if regions overlap | `PlotRegistry.plotAt(block)`; allow only if `plot.ownerSide == side`; never rely on one `Area` |
| A11 | S2 | `:557–573` | Creative flight everywhere trivialises elytra/fan travel; players can bypass islands | Flight zone check per tick; elytra in mart |
| A12 | S2 | `:589–601` | `sideOf` only at start; a player who changes team/rejoins is stale | Rebuild membership on join/quit/team change |
| A13 | S2 | `:575–587` | Reset misses cursor, vehicle, PDC book/cart, and players outside the world | Central `PlayerStateService.reset`; handle quit |
| A14 | S2 | `:909` | Shop cooldown uses wall clock, not ticks | Use `tick`/`System.nanoTime` with a tick budget |
| A15 | S2 | `:307–324` | Text displays can become invalid after chunk unload; labels vanish silently | Force-load label chunks; respawn labels on `ChunkLoadEvent` or at plot bind |
| A16 | S3 | `:300–305` | Synchronous `put()` for every map block at start; a 100k-block map will hitch | `McmBuild.BuildQueue` or a `Paster` with 15 ms/tick |
| A17 | S3 | `:614–632` | `processRegen` scans every tick; `voidCheck` every 5; fine now, scales poorly with islands | Bucket regen by due tick; check falls every 2–5 ticks per player |
| A18 | S3 | `:835–888` | `getBlockAt` in listeners can load chunks synchronously | Only query within known plot regions; keep chunks ticketed |
| A19 | S3 | `:515` | `w.getEntities()` removes every non-player entity in the whole world, including other systems' | Track spawned entities; remove by tag/region |
| A20 | S3 | `:307–324` | One `TextDisplay` per stall + 2 per area (47 at 10 teams), each with a chunk force | Pool labels; one per gallery/plot, not per block |
| A21 | S2 | `:502–534`, `BuildMartPlugin.java:20–22` | A crash/reload while the world is loaded leaves old chests, item drops and blocks; a new game can loot them | Full dirty-region wipe; autosave off; no containers on the old map; delete the folder only when safe |
| A22 | S3 | `:307–324` | Labels are spawned once and never re-spawned after a chunk unload/reload; the HUD loses its prompts | Pool/refresh labels on chunk load or plot bind; keep chunks ticketed |

### 5.1 Duplication audit (explicit)

- **No classic double-add path found** in the stall, shop or plot flow. Bukkit events are sequential, the
  stall block is air before the next break, `give()` for a single item is all-or-nothing, and the shop
  checks `containsAtLeast` before removing costs.
- **Infinite stall regen is intended farming**, not a dupe: a player mines one block, gets one item, and
  the stall restores one block. If a player places the item back on the stall before regen,
  `processRegen` sees non-air and drops the entry (A9), which is a *loss*, not a dupe.
- **Real risks are loss and score inflation**, not dupe: shop `addItem` leftovers (A6), cursor on void
  (A7), stale chests after a crash (A21), and 4x team points (A1).
- **Do not add a "sell back" or "cart storage" feature** without a transaction log; those are the usual
  places a Build Mart clone develops a dupe.

## 6. Config surface (proposed keys)

All values below are **defaults, not researched** unless a source is named. Keep them in
`config.yml` and list them in the README.

```yaml
map:
  id: harbour-market          # original name, not an MCC map name
  rotation: [harbour-market, frost-fair]   # 2-3 original maps
  paste-ms-per-tick: 15
  clear-ms-per-tick: 20
  base-y: 64
game:
  duration-seconds: 720       # 12 min, S1
  pregame-seconds: 15
  gallery:
    restock-seconds: 90       # S1
    composition: {1: 2, 2: 2, 3: 1, 4: 1, 5: 1}
  gold:
    first-at-seconds: 240     # S1
    rotate-seconds: 120       # S1
    window-seconds: 120       # S1
    stars: 7                  # S1
    clear-on-fail: true
  plots:
    regular-per-team: 3       # S1
    size: 5                   # 5x5x5 default, Q1
  travel:
    elytra-in-mart: true
    flight-in-bay: true
    fan-radius: 3.0
    fan-levitation-ticks: 20
    fan-amplifier: 49
    cart-enabled: false       # true only for the old-map variant
  scoring:
    star-coins: [[0, 10], [240, 15], [480, 20]]   # S3
    partial-completion: true
    employee-bonuses: {entrepreneur: 100, line-cook: 100, quality-assurance: 100}
    employee-second: 50
    employee-third: 25
  resource:
    respawn-ticks: 60
    islands: [ores, logs, stones, colours, nether, coral, copper, garden, snow]
  spectator:
    overview: [0, 96, 0]
    zone-radius: 80
```

## 7. Runtime state machine (proposed)

```
IDLE
  -> LOADING        (paste map, bind regions, spawn labels/entities)
  -> PREGAME        (teleport, title card, book, tutorial, Waits.countdown)
  -> PLAYING        (gallery rotation, gold windows, regen, plot dirty eval, HUD)
       -> GOLD_ACTIVE (overlay; does not pause PLAYING)
  -> ENDING         (award Employee bonuses, placement reveal, fireworks)
  -> CLEANUP        (wipe dirty chunks, remove entities, clear state)
  -> IDLE
```

- `stop()` can jump to `CLEANUP` from any state and must be idempotent.
- `skipWait()` jumps `PREGAME -> PLAYING` (admin fast test).
- `skipRound()` ends the round early and goes to `ENDING` (single-round game: return false or end).
- `onLateJoin()` puts the player in `SPECTATOR` at `spectator.overview`; if `late-join-team` is enabled
  and a bay has room, assign and prepare instead.

## 8. Acceptance tests for the audit itself

1. Every "current" row cites a line range that exists in the current file.
2. Every "target" row cites a source in `README.md` §2.
3. No target claims a value as researched when it is a default (see Q1–Q10).
4. No current file is edited by this audit; `git status` under `game-plugins/BuildMart` is clean.
5. The visual pack contains at least one image each for gallery, 3 plots + gold, shop/crafting, cart,
   resource island/mart, and a fan recreation; each has a source line.
