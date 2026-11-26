# Build Mart — current-code audit and fan-recreation survey

Audit of `game-plugins/BuildMart/` against the real MCC game, plus a technical survey of fan
recreations, plugins and map attempts. Research only: no source, world or plugin file was edited.
Visual evidence is in `frames/` and indexed in `REFERENCE_IMAGES.md`.

Scope: the three Build Mart Java files, its README, the core API, and the map/paste patterns in
`game-plugins/Tgttos` and `game-plugins/SkyBlockle`. Out of scope: building the map, writing presets,
downloading third-party worlds.

## 1. Method

1. Read every Build Mart source file line by line and mapped behaviour to line numbers (`data/file_map.md`).
2. Read the core API the game must use (`McmGame`, `McmGameContext`, `McmBuild`, `McmFx`,
   `McmSpectator`, `Waits`) and the two local map/paste implementations (TGTTOS, Sky Blockle).
3. Read the MCC wiki rules/map/history pages and the official rules page for the target behaviour.
4. Watched only short, targeted fan clips (2 trailers, 1 tutorial, 1 MCC 7 POV window) and captured
   stills; no full VODs. Read plugin source where it was public (CopyBuild, SpeedBuilders, datapack).
5. Compared current behaviour with target behaviour, then proposed file-level architecture and phases.

## 2. Sources

| # | Source | Used for | Tier | Conf. |
|---|---|---|---|---|
| S1 | [Big Sales at Build Mart — MCC Wiki](https://mcchampionship.wiki.gg/wiki/Big_Sales_at_Build_Mart) (fetched 2026-11-24) | 12 min; 3 builds at once; 7-plot gallery composition; 90 s restock; gold first at 4 min, every 2 min, 7★, no team reference, 2 min, fail clears; 3 regular + gold plot; 5 chests, crafting tables, stonecutters, 2 smelting areas; portal; elytra/fan/launch pads; islands; star coin summary (see S3 for the dated 10/15/20 curve); partial pay; Employee of the Month 100/50/25 | 1 | H |
| S2 | [Build Mart (Map) — MCC Wiki](https://mcchampionship.wiki.gg/wiki/Build_Mart_(Map)) | Current map: build area + mart; resource islands; elytra auto-equipped; giant central fan; speed/jump pads; debut MCC 14 | 1 | H |
| S3 | [Games Updates (Season 5) — MCC Wiki](https://mcchampionship.fandom.com/wiki/Games_Updates_(Season_5)) | Blueprint update: choose up to 3, gallery refresh 90 s, claimed builds don't refresh until done, stars, gold only in gallery, employee bonuses, colour-coded chests | 1 | H |
| S4 | [Build Mart Rules — mcchampionship.com](https://mcchampionship.com/pages/build-mart/) | Official one-paragraph description; confirms resource management + gold from memory | 1 | H |
| S5 | [Big Sales at Build Mart — MCC Fandom](https://mcchampionship.fandom.com/wiki/Big_Sales_at_Build_Mart) | Old map: shopping carts act like controllable minecarts, faster but less control, pitfalls, lose items except tools; 5 categories; 3 builds + 3 empty spaces | 1 | H |
| S6 | [MCC 7 POV clip — MajK](https://www.youtube.com/watch?v=LknsKs8ctvk), 0:00–3:00 only | In-game tutorial text: "Use your trolley ... don't fall in any holes ... lose all the resources"; "templates of **four** different builds"; cart item in hotbar; rail line; portal | 1 | H |
| S7 | Local code: `BuildMartGame.java`, `BuildPreset.java`, `BuildMartPlugin.java`, `README.md` | Current behaviour and line-level risks | — | H |
| S8 | Local core: `plugin-src/com/mcm/api/*` | Contracts, helpers, lifecycle rules | — | H |
| S9 | Local map patterns: `Tgttos/Paster.java`, `MapRegistry.java`, `model/TgMapData.java`; `SkyBlockle/MapWorld.java`, `MapModel.java` | Tick-budgeted paste, chunk tickets, stale-block clearing, dirty-chunk wipe, pure-data maps | — | H |
| S10 | [Plottings — Build Mart Recreated trailer](https://www.youtube.com/watch?v=sbwR5ehOId4) (2024-08-15) | Fan recreation of the first map; 400+ h / 3 years; 3x2 plot hall with raised references; 50+ builds; mall departments + hazard holes; no download | 2 | H (visual) |
| S11 | [ThePerson — Build Mart Recreation Showcase 2](https://www.youtube.com/watch?v=gM6ewTfFofM) and [Part 1](https://www.youtube.com/watch?v=3xFljOsJoXQ) (2024-08-06) | Fan plugin recreation: 3 plots + raised refs, custom `Coins` sidebar, crafting, night vision/saturation, concrete kit; Part 1 is a flat mechanic test | 2 | H (visual) |
| S12 | [Zibly — How to Re-Create MCC's Build Mart](https://www.youtube.com/watch?v=r-wY0YqNJek) (2022-03-13) | Command-block build detector, `/fill` clear, particles/sound, fan = `levitation 50` for 1 s on the nearest player in a radius | 2 | H (tutorial) |
| S13 | [YOBittu — command-block Build Mart](https://www.reddit.com/r/MinecraftChampionship/comments/i6hs4q/) (2020-08-09) | 4v4 only, 2 builds per slot, working portal, no hallway pits; video only | 2 | M |
| S14 | [MCC forums — Community Creations](https://mcchampionship.com/forums/community-creations.92/) (page 403; search snippet) | A 2024 command-block recreation with only the building mechanic and 3 builds; plans the mart later; no download | 2 | L (author/date not verified) |
| S15 | [bur/copybuild](https://git.gay/bur/copybuild) (2025) | 9x9x9 model/player areas; templates.yml stores full block data + relative coords; material-only match; red dust on wrong blocks; one player | 2 | H (source) |
| S16 | [ElhananBrisk/SpeedBuilders](https://github.com/ElhananBrisk/SpeedBuilders) (2020, GPL-2.0) | Unfinished: `Material[7][7][7]`, no schematic IO, no completion check | 2 | H (source) |
| S17 | [Speed Builders — SpigotMC, MejkrCZ](https://www.spigotmc.org/resources/%E2%9C%AA-speed-builders-%E2%9C%AA-minigame-holograms-animations-challenges-multiarena-bungee-mysql.136635/) (v18, 2026) | FAWE schematics; bulk creator splits a platform into plots; auto-placed protected building floor; block-by-block progress; multi-arena | 2 | H (description) |
| S18 | [zageev/speedbuilders-practice](https://github.com/zageev/speedbuilders-practice) | Datapack: structure blocks + loot-table `structure_data`; `execute if blocks` volume compare every tick; clone reference to hidden area | 2 | H (source) |
| S19 | [SuperNeon4ik — MCC Recreation](https://www.youtube.com/watch?v=7wXBv59ZiAw) (2021-09-22) | Fan event server; description explicitly says Build Mart was **not** made (with Sky Battle, Grid Runners, Battle Box) | 2 | H |
| S20 | [MCC wiki — List of Build Mart Builds](https://mcchampionship.wiki.gg/wiki/List_of_Build_Mart_Builds) | 114 unique builds historically; block randomisation since MCC 8; scale reference for a preset library | 1 | H |

Rule of thumb used below: **H** = seen in two independent sources or directly in code/footage;
**M** = one source or a reasonable inference; **L** = single weak/anecdotal source.

## 3. Findings

### 3.1 The current game is a prototype of a different game

1. **It is a ring of team platforms around a 27-stall mart, not the MCC map.** `start()` places every
   side on a ring (`BuildMartGame.java:208–229`), `buildArea()` builds a 17x11 platform with one 5x5 plot
   and one 5x5 reference pedestal (`:400–441`), and `buildMart()` builds a 51x27 tiled floor with an
   8x4 stall grid (`:326–393`). There is no build hall, no gallery, no gold bay, no resource island, no
   portal and no spectator area. [S7, S1, S2] **H**
2. **One build at a time, shared order.** `Area` holds one plot, one pedestal, one `seqIndex`
   (`:102–118`); `presetAt()` walks a single shared sequence (`:454–476`); `complete()` clears the plot
   and advances the same area (`:762–787`). MCC has **three regular plots plus a gold plot**, with
   team-chosen blueprints. [S7, S1] **H**
3. **"Big Sales" is the game's name, not a sale event.** The code runs `startSale()` every 75 s
   (`:704–711`, `:728–741`) and labels stalls `SALE!` (`:395–398`). The real "Big Sales at Build Mart"
   has no timed sale mechanic; its timers are the 90 s gallery restock and the gold build window.
   [S1, S3] **H** — delete this whole subsystem.
4. **The four "shop counters" are invented.** `Shop`/`Cost` (`:85–95`) turn 2 Sand→1 Glass,
   2 Cobblestone→1 Stone Bricks, 2 Clay→1 Bricks, Iron+Glowstone→2 Gold Blocks on right-click
   (`:346–352`, `:890–940`). MCC gives crafting tables, stonecutters and two smelting areas plus five
   colour-coded chests; it does not have right-click converter counters, and "Jeweller → Gold Block"
   confuses a material with the Gold **Build**. [S1, S7] **H/M**
5. **There is no Blueprint Gallery and no team choice.** The reference is a physical copy on a pedestal
   next to the plot (`showTarget()` `:478–500`). MCC's gallery has 7 selectable plots
   (2x1★, 2x2★, 1x3★, 1x4★, 1x5★, plus gold), restocks every 90 s, and sends a copy to a chosen team
   plot. [S1, S3, S10, S11] **H**
6. **There is no shopping list book.** `sendShoppingList()` sends one chat line (`:789–800`). MCC puts a
   written book in every player's inventory; the plot hologram also lists materials. [S1, S10, S11] **H**
7. **There is no Gold Build.** The only "gold" is the shop product (`:350`). MCC's gold build is a 7★
   build, first at 4:00, rotating every 2:00, visible only in the gallery, no reference in the team
   area, 2:00 to complete from memory; failure clears the placed blocks. [S1, S3, REFERENCE_IMAGES] **H**
8. **There is no resource-island travel.** All blocks are in one flat grid; the only movement aid is
   creative flight (`preparePlayer()` `:557–573`). MCC uses elytra + a central fan + launch/speed pads,
   with resource islands above/below; falling off loses carried materials. [S1, S2] **H**
9. **There is no cart.** The old map used a "trolley"/shopping cart: a hotbar minecart item used as a
   controllable vehicle on rails, faster but less controllable, with pitfalls that cost carried
   resources. It was **travel**, not a container and not the shopping-list UI. The MCC 7 tutorial also
   says each team had **four** templates at once on the old map (the Fandom summary says three; the
   in-game tutorial is more specific for that era, so treat "three or four" as a researched
   disagreement). [S5, S6] **H**
10. **The map is rebuilt in code every start, not pasted from data.** `buildMart()`/`buildArea()` write
    blocks synchronously through `put()` (`:300–305`) and there is no map data class, no map registry,
    no preview dump, and no `maps()`/`forceNextMap()` implementation. [S7, S9] **H**

### 3.2 Scoring is structurally wrong for a team game

11. **Points are awarded per member, not to the team pool.** `complete()` loops members and calls
    `ctx.addPoints(m.getUniqueId(), pts, ...)` (`:770–771`). The sidebar/standings sum members, so a
    4-player team earns 4x the intended team coins. `McmGameContext.addSidePoints(side, points, ...)`
    exists precisely for a team award and splits it across online members. [S7, S8] **H**
12. **The formula does not match MCC.** Current: `4 + 3*stars`, speed `5 - secs/30`, order bonus
    4/2/1 (`:766–769`), then +10/6/3 for most builds (`:817–831`). MCC's dated Season 5 history: stars
    are worth 10 coins in minutes 0–4, 15 in 4–8, 20 in 8–12 (some older summary pages say a flat 20;
    prefer the dated history). Partial builds pay by completion %; three Employee of the Month
    bonuses (most stars, most builds, highest average stars) pay 100/50/25 to the top three.
    [S1, S3] **H/M**
13. **Order bonus is keyed to a shared build index.** `finishersPerIndex` (`:148`, `:764`) only makes
    sense when every team is on the same build number. With a gallery and 3 simultaneous plots it is
    meaningless; use per-build-instance completion order or drop it. [S7, S1] **H**

### 3.3 Completion checking is material-only and too coarse

14. **`evaluate()` compares `Material` only.** `:743–760` loops 5x5x5 and compares `have == want`;
    `BuildPreset.at()` returns a `Material` (`:94–99`). Stairs, slabs, logs, trapdoors, fences and
    waterlogged states are ignored, so a wrong-facing stair "completes" a build. The README admits
    this. MCC builds use oriented blocks; block randomisation changes materials, not orientation.
    [S7, S20] **H**
15. **Evaluation is polled, not event-driven at plot level.** `second()` evaluates every area every
    second (`:713`) and `dirty` triggers an extra pass (`:619–626`). With 3 plots x 10 teams that is
    ~3,750 block reads/s; fine now, wasteful later, and a plot can be "complete" one tick after the
    player expects it. Use per-plot dirty flags from place/break plus a slow reconciliation pass. [S7] **M**
16. **Presets are tiny and cannot express the real build set.** `BuildPreset` is max 5x5x5, 30 legend
    chars, 25 builds (`BuildPreset.java:12`, `:17–49`, `:136–166`); no stairs/slabs, no gold builds,
    no per-build shop/craft metadata beyond four hardcoded shop materials (`:15`). The real game has
    114+ historical builds and five star bands. [S7, S20] **H**

### 3.4 Map, world and player lifecycle risks

17. **Solo/many-side layout is unbounded.** Ring radius is `max(48, 11.5/sin(pi/n))` (`:214`). The
    formula keeps the chord between platforms around 23 blocks, so it does **not** overlap, but at 40
    solo players the radius is ~147 blocks: a ~300-block-wide arena, ~120-block paths to a 51-wide mart,
    40 platforms and labels, and only 12 `SOLO_COLORS` before colours repeat (`:69–73`, `:223`). It is
    expensive and ugly rather than broken. A real map needs fixed team bays (max 10) plus a compact
    solo/test layout, not a giant ring. [S7] **H**
18. **No full region wipe.** `cleanupArena()` only resets blocks it recorded in `arenaBlocks`
    (`:502–534`); after a crash or a plugin reload while the world is loaded, stale blocks remain. The
    world folder is only deleted at enable **if the world is not loaded** (`BuildMartPlugin.java:20–22`).
    Sky Blockle's dirty-chunk wipe and TGTTOS's stale-set paste are the patterns to copy. [S7, S9] **H**
19. **Late join and disconnect are unhandled.** `onLateJoin()` is not overridden, so the core makes late
    joiners spectators (README says so); `PlayerQuitEvent` is not handled, and `sideOf`/area membership
    only changes at start. A player who disconnects keeps Build Mart items and flight state until
    `stop()`; a team whose members all leave can still complete builds and take order bonuses because
    `evaluate()` runs for every area regardless of online members. [S7, S8] **H**
20. **Flight is unrestricted.** `preparePlayer()` sets `setAllowFlight(true)` (`:570`) and there is no
    zone check. On a resource-island map this removes the elytra/fan traversal game; MCC allows flight
    in the build area but travel in the mart is elytra/fan/pads. [S7, S1] **H**
21. **World rules are almost right but set in the wrong place.** `ensureWorld()` (`:163–194`) sets the
    right gamerules, but duplicates `McmBuild.voidWorld()` and misses `SPECTATORS_GENERATE_CHUNKS`,
    `SHOW_DEATH_MESSAGES`, `setAutoSave(false)`. Use the core helper and add the missing rules. [S8] **M**
22. **Block-break/place listeners can fight other plugins.** Both use `EventPriority.HIGH` and call
    `e.setCancelled(false)` (`:846`, `:885`), which un-cancels protections set at HIGH/NORMAL. Prefer
    ignoring foreign worlds and not forcing `false`; use `MONITOR` only for observation. [S7] **M**
23. **Shop cooldown uses wall-clock time.** `System.currentTimeMillis()` (`:909`) is not tick-based and
    is affected by clock changes; use ticks. Minor. [S7] **L**
24. **Inventory room check is approximate.** `hasRoom()` (`:942–950`) counts empty slots and matching
    stacks but `addItem()` can still leave leftovers on the ground; the loop does not inspect the
    result. Not duplication, but item loss and a bad "full" message. [S7] **M**
25. **Void loss is all-or-nothing and misses the cursor.** `voidCheck()` clears non-tool slots
    (`:650–667`) but not the item on the cursor, and teleports to `a.spawn` even if the player fell
    while in the gallery/mart. With carts and islands, the respawn point and the "lost materials"
    message need per-mode rules. [S7, S6] **M**

### 3.5 What actually works and should be kept

26. **The lifecycle skeleton is sound.** `start()` calls `stop()` first, `stop()` is idempotent,
    `endGame()` awards bonuses before `ctx.finish()`, and cleanup runs after finish (which is
    synchronous). This matches the core contract (`McmGame`: call `finish` once; core does not call
    `stop` after `finish`). [S7, S8] **H**
27. **The protection listener set is a good base.** Damage/food/drop/leaf-decay/fade/spread/falling-block
    handling (`:957–995`) covers the obvious ways to break a build arena. Add explosions, pistons,
    fluids, item frames and portal creation. [S7] **H**
28. **The side/team plumbing is correct.** `ctx.sides()`, `ctx.sideLabel()`, `ctx.teamColor()`,
    `members()` and `sideOf` already handle teams and solos; `McmFx.sideKeyOf/playersOfSide` can replace
    some of it. Keep the concept, move it into a `TeamArea` model. [S7, S8] **H**
29. **The HUD plumbing is correct.** `useCoreScoreboard() == true`, `ctx.setTimer`, `ctx.setSidebarLines`
    and action bars (`:241–242`, `:696–724`, `:802–815`) are the right core integration; only the
    content needs rewriting. [S7, S8] **H**
30. **`BuildPreset`'s parser and validator are reusable ideas.** The legend/layer format, `at()`,
    `columnUsed()`, `unsupported()` and star scoring (`BuildPreset.java:59–130`) are a good base for a
    data-driven preset format; they need BlockData, bigger sizes, gravity/orientation checks and gold
    metadata. [S7] **H**
31. **The solo path exists and is useful for testing.** `SOLO_COLORS` and per-side areas mean a single
    player can play; keep a `solo` layout/config, but do not use the ring for 40 solos. [S7] **H**

## 4. Design implications (what the rewrite must do)

1. **Data-first map, not code-first ring.** One `BuildMartMap` pure-data object per map, built by a
   generator, pasted by a tick-budgeted paster, wiped by a dirty-region wiper. This is the TGTTOS /
   Sky Blockle pattern already proven in this repo.
2. **Three regular plots + one gold plot per team bay.** Each plot owns a `BuildJob` (preset, start
   tick, completion %, wrong blocks); the bay owns 3 slots and a gold slot.
3. **Gallery is the only source of builds.** Seven pedestals (2/2/1/1/1 + gold), 90 s restock, selection
   sends a copy to a free team slot, claimed blueprints do not restock until completed. Gold appears at
   4:00, rotates every 2:00, has no team reference, fails clear.
4. **Shopping list is a book, not chat.** Written book per player, updated when the team's selected
   builds change; pages per build with star rating and counts; plot holograms repeat the list.
5. **Resource islands replace stalls.** Upper islands: ores/logs/stones; lower: colours; variants for
   nether/coral/copper/garden/snow. Blocks respawn on a timer; falling off costs carried materials.
6. **Travel is elytra + fan + pads on the current map.** Carts are a second-map/old-map option, not the
   default. Flight is allowed in the build bay only.
7. **Scoring is team-pool and MCC-shaped.** Star value by elapsed time, partial completion, gold 7★,
   Employee of the Month; award with `addSidePoints`.
8. **Completion is BlockData-aware and per-plot.** Ignore only states that genuinely do not matter;
   otherwise compare canonical block data. Use dirty flags; reconcile slowly.
9. **World ownership is explicit.** Own the world, track every chunk the map or players touched, wipe
   on stop, delete the folder at enable, disable autosave, and keep spectators out of chunk generation.
10. **Spectator is a zone, not a glass box.** Use `McmSpectator.Zone` around an overview point; late
    joiners go there by default; a player who finishes/leaves a team can be moved there.

## 5. Open questions (do not guess in code)

| # | Question | Why it matters | Current default to use until researched |
|---|---|---|---|
| Q1 | Exact gold plot size — same 5x5 as a normal plot, or larger? | Plot model, reference placement, gold build max size | 5x5x5, config `gold.plot-size`; frames suggest same footprint |
| Q2 | Do resource islands regenerate individual blocks, whole piles, or on a timer? | Regen service design, duplication risk | Per-block respawn 3–5 s after break, config |
| Q3 | Can two teams select the same blueprint at once? Does it disappear from the gallery? | Gallery claim model | Per-team copy; blueprint stays until restock; claimed-by-team only blocks that team's refresh |
| Q4 | Partial completion formula: % of blocks, % of stars, or weighted by material? | Coins, HUD | `placedCorrect / totalBlocks`, rounded down; config weight |
| Q5 | Gold failure: clear only the gold plot, or also consume the attempt? | Gold state machine | Clear gold plot, mark attempt failed, no retroactive completion |
| Q6 | Exact cart physics: max speed, rail gaps, collision, item pickup while riding, fall out of cart | Cart implementation and map | Minecart on rails, `minecartMaxSpeed` config, dismount on gap, lose carried items on void |
| Q7 | Employee of the Month ties and eligibility (min builds? min stars?) | Bonus fairness | Top 3, ties share, require >=1 completed build |
| Q8 | Does the current map allow flight in the mart? | Elytra/fan loop | No flight in the mart; elytra + fan only; flight in build bay |
| Q9 | Late-join policy: spectator always, or fill a team? | `onLateJoin` implementation | Spectator at overview; admin can `/mcm` move |
| Q10 | What exactly does the shopping-list book show for a gold build (no reference)? | Book/HUD | Name, 7★, materials, "from memory — gallery only"; no layout |

## 6. Deliverables

| File | Contents |
|---|---|
| `README.md` | This dossier: method, sources, findings, implications, open questions, checklist |
| `data/current_vs_target.md` | Current behaviour vs target, keep/rewrite/delete, line refs, priority |
| `data/file_map.md` | Every current file/line range, responsibility, verdict; proposed new files |
| `data/recreations.csv` | 15 fan recreations / plugins / maps / videos with tech + licensing |
| `data/plan.md` | Phased plan: map+gallery+3 plots, cart/list, gold, polish; acceptance tests |
| `REFERENCE_IMAGES.md` | Visual index: path, source, what it shows, emulate vs avoid |
| `frames/` | 26 images: 22 stills + 4 contact sheets (research only; never ship) |

## 7. Implementation checklist (testable)

- [ ] `start()` loads exactly one map from `BuildMartMap` data and no block is placed by ad-hoc loops in
      `BuildMartGame`.
- [ ] A team bay has 3 regular plots and 1 gold plot, all with stable world coordinates and labels.
- [ ] The gallery shows 7 blueprints with the 2/2/1/1/1 star composition and restocks every 90 s.
- [ ] Selecting a blueprint fills the next free team plot; the reference appears behind that plot only.
- [ ] The shopping-list book opens, is per-player, lists every selected build with counts, and updates on
      selection/completion.
- [ ] A build completes only when every block matches including orientation; a wrong stair does not pass.
- [ ] Completing a build clears that plot, frees the slot, and awards team coins via `addSidePoints`.
- [ ] Partial builds pay a percentage at the end; the HUD shows placed/needed and wrong blocks.
- [ ] Gold appears at 4:00, is visible only in the gallery, has no team reference, and clears on failure.
- [ ] Resource islands have per-block respawn, and falling off the mart costs carried materials but not
      tools.
- [ ] Elytra/fan/pads move players between islands; flight is disabled in the mart and enabled in the bay.
- [ ] The world is fully wiped on stop; a crash leaves no stale blocks after the next start.
- [ ] A late joiner becomes a spectator at the overview and cannot build; a disconnect is cleaned up.
- [ ] A solo player can play a dedicated, compact solo layout (not the team ring).
- [ ] All timings/values are in config and listed in the README as "defaults, not researched".
