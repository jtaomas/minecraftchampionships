# Build Mart — cart, Blueprint Gallery, shopping list and plot interactions

**Scope:** the interaction layer, not the map schematic. This dossier answers four questions precisely:

1. What was the pre-MCC 14 **shopping cart**, and what did it actually do?
2. Does current MCC have a cart/container, and what replaced it?
3. How do the **Blueprint Gallery**, **shopping-list book**, **plot/pedestal** and **Gold plot** work?
4. What should our plugin build, given the current code has one plot and a chat list?

**Status:** research only. No source, world or `game-plugins/BuildMart/` file was edited. All new files are under `game-plugins/research/buildmart/cart/`.

---

## 1. Method

- Read the shared brief, `BuildMart/README.md`, `BuildMartGame.java` (`sendShoppingList`, `showTarget`, `complete`, `evaluate`, `buildArea`, `preparePlayer`, `voidCheck`, `onInteract`) and `BuildPreset.java`.
- Read the current MCC wiki (wiki.gg) and the Fandom old-map page.
- Two targeted VOD windows only, 7.5 minutes total:
  - **S3** Philza MCC P25, Build Mart chapter 25:36–40:04; sampled **25:36–30:06** (270 s). Covers the current book, plot reference, sidebar, mart islands and gold timer.
  - **S4** Illumina MCC 13, Build Mart chapter 2:28:10–2:43:00; sampled **2:28:30–2:31:30** (180 s). Covers the old indoor mart and the cart.
- 49 extracted frames (including contact sheets and near-duplicates), with 20 curated reference images under `frames/ref/`. The image pack is the primary handoff: **`REFERENCE_IMAGES.md`**.
- No full VOD downloaded. No third-party map world or build asset was imported into the plugin; the wiki images are reference only.

---

## 2. Sources

| ID | Source | Tier | Use | Confidence |
|---|---|---|---|---|
| **S1** | MC Championship Wiki (wiki.gg), *Big Sales at Build Mart*, retrieved 2026-11-24, sections Goal/Introduction, Build Area, Build Mart, Blueprint Gallery, Coin Rewards, History, Trivia | 1 | Rules, gallery, gold, book, history | **H** for stated rules; **M** for staging/visual details |
| **S2** | MC Championship Wiki (Fandom), *Big Sales at Build Mart (Map)*, retrieved 2026-11-24 | 1, **Tier 3** for the old cart | Old map, cart travel, pitfalls | **H** |
| **S3** | Philza MCC P25 VOD, YouTube `jpMnwvRmHao`, Build Mart 25:36–40:04; sampled 25:36–30:06 | 1 | Current book, plot, sidebar, islands, gold timer | **H** visual |
| **S4** | Illumina MCC 13 VOD, YouTube `b6v_TuEUhkI`, Build Mart 2:28:10–2:43:00; sampled 2:28:30–2:31:30 | **3** historical | Old mart, cart ride | **H** visual; **M** entity details |
| **S5** | Wiki stills: `BSABM-BlueprintGallery.png`, `BSABM-BuildArea.png` | 1 | 7-pedestal gallery, 3+gold build area | **H** composition; **M** staging |
| **S6** | Current code: `BuildMartGame.java`, `BuildPreset.java`, `BuildMart/README.md` | current implementation | What exists / what changes | **H** |

**Not used:** `mcchampionship.com/pages/build-mart` returned HTTP 403; no official Noxcrew page is quoted. No unsourced “design intent” is claimed.

---

## 3. Findings

### 3.1 The old cart was a rideable travel minecart, not a container or a list — **Tier 3 historical**

| Finding | Evidence | Confidence |
|---|---|---|
| The cart was a **plain minecart on rails**, ridden first- or third-person through the old indoor mart. | S4 2:30:42–2:30:45: player seated in a minecart, cart lip in first-person, white aisle and hazard edging passing. | **H** visual |
| It carried **no blocks**. The player’s inventory carried the resources; the cart has no chest model. | S4 2:30:45: empty minecart; hotbar holds the gathered blocks. | **H** visual; **M** that no chest variant existed |
| It was **not** a shopping-list UI. The list/book is a P25 invention. | S1 History: shopping-list book added in MCC P25; S2 describes the cart only as travel. | **H** |
| Players could **run or ride at will**. Carts were faster but had less control. | S2: “players would either run on their feet or use the shopping carts to travel faster but with less control in the mart. The players could change their method of traveling at will.” | **H** |
| The old mart had **pitfalls**; falling in lost all gathered resources. | S2. | **H** |
| The old map was used from **MCC 3 through MCC 13**, plus Jingle Jam and All-Stars/Scuffed; **11 plays** total. | S2 appearances table (“Times Played 11”). | **H** |
| **MCC 14 replaced cart travel with elytra** and introduced the floating-island map, central fan, launch pads/speed boosts, nether/coral resources, Gold Builds, and 10-minute games. | S1 History (MCC 14). | **H** |

**Why replaced (inference, not wiki quote):** rail-bound carts cannot serve resources at different heights. The current map is a 3D island course; elytra + fan + launch pads are the traversal. Restoring carts as travel would fight the map. **M.**

**What the cart interaction felt like:** a route commitment. Fast, fixed, less control; running was slower but turnable. The danger was the trip, not the storage. That is the feeling worth preserving, not the rail network.

### 3.2 Current MCC has no cart and no container — **H**

- S1’s current Build Mart description lists elytra, central fan, launch pads/speed boosts, resource islands and falling risk. It never mentions a cart, chest cart or team container.
- S3 27:52–28:10 shows the player flying between `LOGS`, `SAND` and `STONE` islands; no rails or carts in the sampled mart.
- The current “shopping” objects are:
  1. the **shopping-list book** in each player’s inventory;
  2. the **inventory** itself;
  3. the **3D reference** on the plot pedestal;
  4. the **Blueprint Gallery** selection.
- **Verdict:** the old cart’s travel role is gone; its “shopping” name survives only in the book. Any cart we add is **original**, not a recreation. Label it in-game.

### 3.3 Blueprint Gallery — 7 pedestals, 90 s rotation, up to 3 active builds

| Finding | Evidence | Confidence |
|---|---|---|
| Seven blueprint plots: **2×1★, 2×2★, 1×3★, 1×4★, 1×5★**. | S1 §Blueprint Gallery. | **H** |
| Each plot is a physical pedestal/diorama on a blue gallery floor, with a floating name, star rating and material list; a small red/gold **select pad** sits at its foot. | S5 gallery image; S3 26:46 sidebar. | **H** composition; **M** exact click target |
| A team may work **up to three builds at a time**; selecting sends a copy to the team area and creates a reference in front of an empty plot. | S1 §Introduction. | **H** |
| The gallery **restocks every 90 seconds**. | S1 §Blueprint Gallery. | **H** |
| After a build is completed, the team may select another. | S1 §Introduction. | **H** |
| Since **MCC P25**, a team can **repeat the same build** by selecting it again before the rotation happens. | S1 Trivia (“Since the revamp in MCC P25, it is possible to repeat the build by selecting the same build before the build rotation happens”). | **H** |
| The **Gold Build** has its own gallery pedestal; it is not a normal selectable slot. | S1 §Blueprint Gallery. | **H** |
| Selection state is visible in the sidebar: `Empty Plot #1/#2/#3` before selection; `Alternator (x1 ★)`, `Booth (x5 ★)`, `Clouds (x2 ★)` after. | S3 26:46 and 27:58. | **H** visual |

**How selection is shown, in order of certainty:**

1. **Certain:** the team area receives a physical reference; the sidebar lists the three slots with names and star counts; the book gains pages.
2. **Highly likely:** the gallery pedestal itself shows the diorama, name, stars and materials; a select pad/button is the interaction.
3. **Uncertain:** whether the pad is right-clicked, stepped on, or a button. The wiki image shows a pad; the VOD does not show the click. Use a deliberate right-click in our build (see §6).

**How builds are replaced:**

- Gallery slots rotate every 90 s, replacing **unselected** blueprints.
- An **active** build stays on its plot until completed or the game ends; rotation does not delete active work.
- On completion, the plot clears, the reference is removed, and the slot returns to `Empty Plot #N`; the team selects another.
- The **repeat rule** applies only while the same blueprint is still displayed in the gallery. After rotation, it is gone.
- Our design should make this explicit: active builds are pinned; gallery rotation only affects unselected stock.

### 3.4 Shopping-list book — layout, counts, markers, accessibility, updates

**Observed layout (S3 27:45–27:46):**

| Element | Observed | Notes |
|---|---|---|
| Item | Written book with enchant glint, hotbar slot 8 | Protected item; should survive void |
| Header | `Page N of M`, centred | `Page 1 of 4` … `Page 4 of 4` |
| Build title | Large, only when a new build starts | `Alternator`, `Booth`, `Clouds` |
| Heading | `Materials:` | One per build |
| Line | 3D block icon + block name + count + `x` | e.g. `Block of Quartz 11x`, `Diorite 10x` |
| Continuation | Page 3 has **no title**, only two remaining lines | The book paginates a continuous list; page count ≠ build count |
| Footer | Back/forward page arrows; orange `Done` button | Vanilla book GUI |
| Empty state | `Page 1 of 1 — Select builds in the mart!` | Seen at 26:53 before selection |
| Star rating | **Not** on the book page | Stars are on the plot/sidebar |
| Source markers | **None** visible | No island/shop/craft badge in the sampled pages |
| Colourblind support | Icons + full names + counts | Plus the MCC 7 “block subtitles” (block name under the crosshair; S1 History, **H**) |

**Pagination finding (important):** with three active builds the book showed **four pages**. Page 1 = Alternator (2 materials), page 2 = Booth (9 materials), page 3 = **continuation with no title** (Red Concrete 10x, Spruce Stairs 12x), page 4 = Clouds (2 materials). The page count is driven by material-line capacity, not by one-page-per-build. **H** for the observation; **M** for “page 3 belongs to Booth” (adjacency).

**Readability bug to avoid:** page 2 truncates `Heavy Weighted Pressure Plate` to `Heavy Weighted Pre`. Our book must wrap long names or use a short name + icon tooltip.

**How it updates:** the book is rebuilt when the selection/completion state changes. In MCC it is a written book, so it is effectively reissued, not edited live. Pre-selection it has one prompt page; after three selections it has the full paginated list. The book is **per player**, not a shared team object (S1: “in each player’s inventory”).

**Shop/craft markers:** current MCC does not visibly mark source on the sampled pages. Our plugin **should** add them because our shop counters are explicit and the book is the natural place to teach the recipe. Proposed badges:

- `I` = resource island (raw)
- `S` = shop counter (Glassworks/Mason/Kiln/Jeweller)
- `C` = craft/smelt at the build area
- `G` = gold-only

Use letters **and** a legend, not colour alone. Show the recipe in the item tooltip/lore: `S — Glassworks: 2 Sand → 1 Glass`.

### 3.5 Plot and pedestal — reference, guides, completion, reset

| Finding | Evidence | Confidence |
|---|---|---|
| Each regular plot has a **3D reference** on a pedestal behind/next to it, plus floating name, stars and material list. | S3 27:34; S5 build-area image. | **H** |
| The build is replicated on the **empty plot in front** of the reference. | S1 §Introduction. | **H** |
| The team area has **three regular plots + one Gold plot**, five colour-coded chests, crafting tables, stonecutters, two smelting areas, and a portal to the mart. | S1 §Build Area. | **H** |
| Flight is allowed in the build area. | S1 §Build Area. | **H** |
| The plot floor in the current map is a light grid/border; the reference carries the shape. The current code additionally shades used columns (light grey) as a guide. | S3 26:50 (empty plots); S6 `showTarget` (floor tiles). | **H** for both; **M** for how much guide the real map uses |
| Completion is an **exact block match**; since MCC All-Stars the **correct block rotation** is required (stairs, doors, slabs), while texture rotation is not; double slabs count as a full block. | S1 History (MCC AS). | **H** |
| Extra blocks count as wrong; the build only completes when every required block is correct and nothing extra is present. | S6 `evaluate`; S1 implies exact replication. | **H** for code; **M** for MCC’s exact wrong-block handling |
| Partial completion pays coins by percentage at the end. | S1 §Coin Rewards. | **H** |
| On completion the plot clears and the next build can be selected. | S1; S6 `complete`. | **H** |
| Gold failure removes the blocks placed in the Gold plot. | S1 §Introduction. | **H** |
| There is no documented “cancel build” interaction. | No source. | **L** — our design adds a confirmable cancel. |

**Our plot interaction should:**

- Keep the **reference physical** (a real 3D copy), not only a hologram. It is the fastest way to read a build.
- Put the **name/stars/materials** in a fixed floating block above the pedestal, with a max of ~5 lines and `…` overflow into the book.
- Use **plot numbers and corner markers**, not only team colour, for colourblind players.
- Show a **ghost/floor guide** for used columns, toggled by sneak-right-click on the pedestal (accessibility aid; off by default for experienced players).
- Mark wrong blocks with red particles + action bar `3 wrong`; do not block placement, but do not complete until corrected.
- On completion: clear only the plot volume, keep the reference until the slot is reselected, play a title/particles, free the slot.
- On cancel: only if the plot is empty or the team confirms; return placed blocks to the cart/inventory; clear the reference. Gold cannot be cancelled.

### 3.6 Gold Build — timer, memory, failure

| Finding | Evidence | Confidence |
|---|---|---|
| First Gold appears **4 minutes** into the game; then rotates **every 2 minutes**. | S1 §Blueprint Gallery. | **H** |
| Difficulty **7 stars**; separate Gold plot in the team area; **no reference** in the team area; viewed only in the mart/gallery. | S1 §Introduction, §Blueprint Gallery. | **H** |
| **Two minutes** to complete it from memory; failure removes the blocks placed in the Gold plot; cannot retroactively complete past Golds. | S1 §Introduction. | **H** |
| The HUD shows `GOLDEN BUILD IN: mm:ss` under the game timer. At 11:11 remaining, it read 03:11 → first gold at 4:00 elapsed. | S3 27:58. | **H** visual |
| Gold is worth 7 stars; star value is 20 coins since Fall Stars (P25 used an escalating 10/15/20 schedule). | S1 §Coin Rewards, History (FS, P25). | **H** |

**Gold interaction spec:**

- Gold pedestal in the gallery, visually distinct (crown, gold frame), with a 3D build and a “memorise” prompt.
- Right-click the gold pedestal → a 5-second locked camera/preview (or simply a clear view); no schematic is placed in the team area.
- Gold plot in the team area: a 7×7 platform with a gold border, a crown banner, and **no reference**; floor shows only the footprint, not block types.
- Book: a `Gold` page with the **material list only** (no layout) while active; at expiry, remove the page and mark leftover cart materials surplus.
- At 0:00: clear placed blocks, broadcast `Gold Build expired`, play a low sound, free the plot; the next gold spawns at the next 2-minute mark.
- Completion within the window: 7 stars, title/particles, clear the plot, mark complete; late placement after expiry cannot score.

### 3.7 Current code — what exists

| Area | Current implementation | File / line |
|---|---|---|
| Team layout | One 5×5 **plot** + one **pedestal** per side, on a 17×11 platform | `BuildMartGame.buildArea` (~400–441) |
| Build sequence | Fixed `sequence`: ramp pass then shuffled passes; one `seqIndex` per side | `buildSequence`, `appendShuffle`, `presetAt` (~454–476) |
| Target display | `showTarget`: clears pedestal, builds 3D copy, floats name/stars/`counts`; floor tiles light grey for used columns | `showTarget` (~478–500) |
| Shopping list | `sendShoppingList`: **chat message** at GO and after each completion; counts + `(shop)` markers | `sendShoppingList` (~789–800) |
| Completion | `evaluate` every second/dirty tick; exact match; `complete` clears plot, `seqIndex++`, `showTarget`, `sendShoppingList` | `evaluate`/`complete` (~743–787) |
| Scoring | `4 + 3×stars` + speed + order; final most-builds bonus | `complete` (~762–787), `awardFinalBonuses` |
| Players | Tools + Haste + flight; **no book**; void strips all non-tools | `preparePlayer` (~557–573), `voidCheck` (~650–666) |
| Interaction | Shop counters only; no gallery, pedestal, cart | `onInteract` (~890–940) |
| Presets | Max 5×5×5; stars 1–5 from block count + shop weighting | `BuildPreset` (~11–110) |
| Known gaps | No gallery, Gold, three plots, book, cart; late joiners spectator | `BuildMart/README.md` §Known limitations |

**Exact current behaviour of the list:** at `GO` and after each completion, each player gets one chat line: `Build #N: Name ★★★ / Needs: 4 Glass (shop), 10 Cobblestone…`. It is not persistent, not per-player state, not a UI, and it does not update as blocks are placed. It is the single biggest interaction gap.

---

## 4. Frame index

See **`frames.md`** for the frame index and **`REFERENCE_IMAGES.md`** for the curated 20-image handoff. The machine-readable interaction matrix is **`data/interactions.csv`** (34 rows). Key images:

| Image | Shows |
|---|---|
| `frames/ref/01_blueprint_gallery_7_pedestals.png` | 7 gallery pedestals + select pads |
| `frames/ref/03_book_page1_alternator.png` | Book page 1: Alternator |
| `frames/ref/04_book_page2_boots.png` | Book page 2: Booth (long list, truncated name) |
| `frames/ref/05_book_page3_continuation.png` | Book page 3: continuation, no title |
| `frames/ref/06_book_page4_clouds.png` | Book page 4: Clouds |
| `frames/ref/08_plot_reference_pedestal.png` | Reference pedestal + floating materials |
| `frames/ref/09_build_area_3_plots.png` | Three current regular plots |
| `frames/ref/10_build_area_gold_plot.png` | Gold plot / crown / no reference |
| `frames/ref/11_cart_ride_thirdperson.png` | Old cart: third-person ride |
| `frames/ref/12_cart_ride_firstperson.png` | Old cart: first-person ride |
| `frames/ref/14_mart_islands_elytra.png` | Current elytra islands (no cart) |
| `frames/ref/15_selected_builds_sidebar.png` | 3 selected slots + stars |
| `frames/ref/16_empty_plots_sidebar.png` | Empty slots before selection |
| `frames/ref/17_gold_countdown_hud.png` | Gold countdown HUD |

---

## 5. Design implications

1. **Do not restore cart travel.** The current map’s traversal is elytra + fan + launch pads. A rail network would flatten the islands or create a worse second movement system.
2. **The book is the primary UI.** Current chat is insufficient; the book must be persistent, per-player, paginated, and protected.
3. **Three plots change the whole loop.** Selection, sidebar, book, cart manifest and scoring all become per-plot; the current single `seqIndex` model must be replaced.
4. **The gallery must be a physical place.** Selection is a world interaction with a visible slot state, not a command.
5. **Gold needs its own plot, timer and memory rules.** It cannot be bolted onto the regular plot as a fourth build.
6. **Accessibility is a layout constraint.** Icons + names + counts + source letters + plot numbers; no colour-only cues; block subtitles on look.
7. **Pagination is content-driven.** Do not force one page per build; allocate lines and repeat the build name on continuation pages.
8. **The book must survive the void.** Current `voidCheck` strips every non-tool item; a book would be lost.
9. **Late join/disconnect are first-class.** Books, carts and plots are team state; reissue on join, never duplicate.
10. **The cart is optional and original.** If enabled, keep it stationary at the plot; if disabled, the loop still works exactly like MCC.
11. **Source badges depend on the blocks agent.** The material→island/shop/craft map belongs in that dossier; the book should consume a single `SourceIndex`, not duplicate the map.

---

## 6. Proposed interaction specification

### 6.1 World layout (interaction-relevant)

**Blueprint Gallery (in the mart):**

- 7 pedestals in a loose ring/rows on a blue floor. Each pedestal:
  - 5×5 diorama of the build at 1:1 block scale;
  - floating text: name, `★` rating, `Materials: …` (max 5 lines);
  - a **select pad** at the foot: a distinct block/button with a gold frame, not walk-triggered;
  - a slot id (0–6) and a rotation timestamp.
- Gold pedestal at the end: crown, gold frame, 3D build, `GOLD BUILD — memorise` text; not a normal slot.

**Team build area:**

- 3 regular plots in a row, each with:
  - a 7×7 base / 5×5 build volume (keep current 5×5 for scope; the base can grow later);
  - a reference pedestal behind it with a 3D copy and floating name/stars/materials;
  - a chest at the pedestal for the reference kit;
  - a plot number and white corner markers; team-colour border.
- 1 Gold plot at the end: orange/gold border, crown banner, no reference, a 7×7 footprint.
- 5 colour-coded chests, 2 crafting tables, stonecutter, 2 smelting areas, portal to the mart.
- **Supply Cart** (optional, original): chest minecart on a 5-block rail spur at the back, team banner, floating manifest, not rideable.

### 6.2 Click flow

| Where | Input | Precondition | Result | Failure |
|---|---|---|---|---|
| Gallery pedestal | **Right-click select pad** | Team has a free plot; blueprint is in stock | Fill the next free plot; spawn reference; add book page; sidebar slot; sound | 3 active → `All plots busy — finish a build`; rotated → `Blueprint restocked` |
| Gallery pedestal | Right-click diorama | Any | Show a 3D preview / name + stars + materials (no selection) | — |
| Gallery pedestal | Shift-right-click select pad | Blueprint already active | No-op; action bar `Already building this` | — |
| Plot reference | Right-click pedestal | Active build | Open the book at that build’s page | — |
| Plot reference | Shift-right-click pedestal | Active build | Toggle ghost/floor guide | — |
| Plot border | Shift-right-click with empty hand | Active build, plot empty or confirm | Cancel build; clear reference; return blocks; free slot | Gold → `Gold cannot be cancelled` |
| Book | Right-click | Has book | Open book at last page | Missing → reissue |
| Book | Click arrows / Done | Open | Page / close | — |
| Cart | Right-click | Team member | Open 27-slot cart | Non-team → `This cart belongs to <team>` |
| Cart | Shift-click item | Item on manifest | Move stack to cart; update hologram | Invalid → `Not on the list`; full → `Cart full` |
| Gold pedestal | Right-click | Gold active | 5 s preview / locked view | Expired → `Gold has rotated` |
| Gold plot | Walk/place | Gold active | Place blocks from memory | After expiry → blocks cleared |
| Sidebar | Look | Any | 3 slots: empty or name + stars | — |

**Design rule:** selection is **deliberate** (right-click a pad). Do not use a pressure plate or walk-through trigger; accidental selection with 3 slots is frustrating.

### 6.3 Book layout (our version)

**Item:** `WRITTEN_BOOK`, PDC `bm_book=1`, team id, revision; slot 8; glint; `Lore: Shopping list — right-click`. Protected from void, crafting, dropping? Allow dropping but reissue on respawn. Never consume.

**Pages:**

1. **Cover:** `BUILD MART`, team name, `2/3 plots active`, gold status (`next in 2:14`), legend:
   - `I` island / `S` shop / `C` craft / `G` gold.
2. **One or more pages per active build**, in plot order:
   - `1. Alternator  ★☆☆☆☆` (or `1★` for readability);
   - `Materials:`;
   - lines: `[icon] Lime Concrete   2x   [I]`, `[icon] Moss Block   8x   [I]`;
   - footer: `Plot 1 • 0/10 placed • 0 wrong`.
   - Long lists **continue** on the next page with `Alternator (cont.)`; never a blank-title page.
3. **Cart page:** `CART 34/58`, per-material `have/need`, `SURPLUS` section in gray.
4. **Gold page (only while active):** `GOLD — 7★ — 01:42 left`, materials only, no layout, `Memorise it in the gallery`.

**Line rules:**

- Max ~9 material lines per page; wrap long names to a second line rather than truncate.
- Icons are the block’s item form; counts are right-aligned tabular numbers.
- Source badge is a letter + tooltip recipe; never colour-only.
- Page arrows + Done; page turn sound; no live per-block updates inside the book (avoid GUI churn). The action bar/HUD shows live `placed/needed`; the book updates on selection/completion/rotation.
- On update: remove old book by PDC, give new book to slot 8, preserve the open page if the book is closed; if open, defer refresh until close and show `Shopping list updated` on the action bar.
- If inventory is full: drop the book at the player’s feet with a glow and a message; it is never lost.

### 6.4 Cart (summary; full spec in `data/cart.md`)

- Chest minecart at the plot, **not rideable**; 27 slots; team-only.
- Accepts only manifest materials; hologram `CART delivered/required` + top 3 missing.
- Deposits survive void/death; cleared at game end.
- Surplus after completion/rotation is gray, retrievable, not counted.
- Optional config `cart.enabled`; default **true** for our server because the owner asked for it, but the MCC-faithful loop must work with it false.

### 6.5 Data model

```java
class TeamState {
    String side;
    List<Plot> plots = new ArrayList<>();      // exactly 3
    GoldPlot gold;
    SupplyCart cart;                            // nullable if disabled
    Set<UUID> members;
    int starsEarned;
}

class Plot {
    int index;                                  // 0..2
    PlotPhase phase;                            // EMPTY, ACTIVE, COMPLETE
    String presetId;
    int stars;
    long startedTick;
    Map<Material,Integer> required;
    int correct, wrong, needed;
    UUID selectedBy;
    int gallerySlot;                            // for repeat tracking
}

class GallerySlot {
    int index;                                  // 0..6
    String presetId;
    long rotateAtTick;
    boolean selected;                           // an active copy exists
}

class GoldState {
    GoldPhase phase;                            // IDLE, ACTIVE, COMPLETE, EXPIRED
    String presetId;
    long spawnTick, expireTick;
    boolean completed;
}

class BookView {
    UUID player;
    int page;
    long revision;
}
```

**State ownership:** the game object owns all state. The book item is a view keyed by PDC; the cart inventory is the source of truth for delivered counts. Do not store gameplay state in item NBT beyond `team` + `revision`.

### 6.6 Edge cases

| Case | Required behaviour |
|---|---|
| **Solo** | One team, 3 plots, 1 cart, 7 gallery slots; no order bonus; all selection rules unchanged. |
| **Late join** | If the game is running and the team has space: survival, tools, book, teleport to team spawn, action bar `Joined <team>`. If no space: spectator. Never leave a late joiner in the void. |
| **Disconnect** | Plot/reference/cart stay; the build is not cancelled. On rejoin: reissue book, teleport to spawn, restore sidebar. No duplicate books. |
| **Three simultaneous builds** | Each plot evaluates independently; one completion frees only that plot; book has pages for all three; cart manifest is the union. |
| **Gallery rotation** | Every 90 s, replace only **unselected** slots; active builds are pinned. A completed build can be repeated only while its blueprint is still displayed. |
| **Selection race** | Synchronize on `TeamState`; first click wins; second gets `Slot taken` / `All plots busy`. |
| **Fourth selection** | Deny with `All 3 plots busy — finish a build first`; do not queue silently. |
| **Gold spawn** | At elapsed 240 s, then every 120 s; duration 120 s; add gold page + plot; broadcast. |
| **Gold expiry** | Clear gold plot blocks, mark expired, remove page, surplus cart materials, broadcast; no retroactive score. |
| **Gold completion at 0:00** | Resolve completion first if the final block is placed on the expiry tick; otherwise expire. Test the boundary explicitly. |
| **Wrong blocks** | Do not complete; action bar `N wrong`; red particles on wrong positions; allow correction. |
| **Extra blocks** | Count as wrong; never complete until removed. |
| **Cancel build** | Confirm; clear plot/reference; return placed blocks to cart/inventory; free slot. Gold cannot be cancelled. |
| **Void** | Lose carried blocks, **not** book/tools/cart; respawn at team spawn; action bar `Lost carried blocks`. |
| **Inventory full** | Books drop at feet; cart deposits denied; shop counter denies; no silent deletion. |
| **Game end** | Clear plots, references, gold, cart, books, holograms, entities; pay partial %; show results. |
| **Restart mid-game** | Prefer a clean abort + cleanup over restoring a half state; if restoring, rebuild from `TeamState`, never from item NBT. |

---

## 7. What must change in the current code

| File / area | Current | Required change | Risk |
|---|---|---|---|
| `BuildMartGame.Area` | `plotX/plotZ`, `tgtX/tgtZ`, one `seqIndex` | `List<Plot>` (3), `GoldPlot`, `SupplyCart`; per-plot `seqIndex`/preset | **High** — touches most methods |
| `buildArea` | One 17×11 platform, one plot + pedestal | 3 plots + gold + references + cart rail + 5 chests + smithing | **High** — map/layout dependency |
| `buildSequence` / `presetAt` | Fixed sequence, one pointer | `Gallery` of 7 slots, 90 s rotation, per-team pinning, repeat | **High** |
| `showTarget` | One target; floor tiles | `showReference(plot, preset)`; per-plot floor guide; keep reference after completion until reselect | **Medium** |
| `sendShoppingList` | Chat line | `ShoppingBook.rebuild(team)`; written book; pagination; source badges; cart page | **Medium** |
| `evaluate` | One plot, exact match | Per-plot; partial tracking; wrong-block particles; no auto-next | **Medium** |
| `complete` | Clears, `seqIndex++`, shows next | Free one plot; no auto-next; score stars; update book/sidebar/cart; leave other plots | **Medium** |
| `second` | Sales, timer, action bar | Add gallery rotation, gold spawn/expiry, book refresh, 3-plot HUD | **Medium** |
| `preparePlayer` | Tools only | Add book in slot 8; keep tools; protect book | **Low** |
| `voidCheck` | Strips all non-tools | Protect `bm_book`; keep cart contents | **Low** |
| `onInteract` | Shop counters only | Add gallery select pad, plot pedestal, cart, gold pedestal | **Medium** |
| `cleanupArena` | Clears plots/entities | Clear 3 plots + gold + cart + books + holograms + gallery | **Medium** |
| `BuildPreset` | 5×5×5, 1–5 stars | Add stable id/category; optional 7×7; keep star formula | **Low** |
| `plugin.yml` / config | Hard-coded | `gallery.rotation=90`, `gold.first=240`, `gold.every=120`, `cart.enabled` | **Low** |
| `BuildMart/README.md` | One plot, no gallery | Update rules after implementation | **Low** |
| Late join | Spectator | Join if team has space; book/tools/spawn | **Medium** |

**Minimum shippable order:**

1. Book service + protect item + reissue on join (fixes the worst gap without changing the loop).
2. 3 plots + per-plot evaluate/complete + sidebar.
3. Gallery + selection + 90 s rotation + repeat.
4. Gold plot + timer + failure.
5. Supply cart.
6. Partial scoring + Employee bonuses + polish.

This order lets the owner see the book and three plots before the map is finished.

---

## 8. 15-bullet interaction checklist

1. **Gallery stock:** exactly 7 pedestals spawn with the 2/2/1/1/1 star distribution; each shows a 3D diorama, name, stars and material count.
2. **Select:** right-clicking a select pad fills the next free plot, spawns the reference, adds a sidebar slot and a book page; a fourth selection is denied with `All 3 plots busy`.
3. **Preview:** right-clicking a diorama without selecting shows the build info and does not consume a slot.
4. **Rotation:** every 90 s only unselected gallery slots restock; active builds are untouched; a completed build can be repeated only while its blueprint is still displayed.
5. **Sidebar:** empty state reads `Empty Plot #1/#2/#3`; selected state shows name + star count for all three; it updates on select/complete/cancel.
6. **Book layout:** cover + paginated material pages; `Page N of M`; build title only on the first page of that build; continuation pages say `(cont.)`; long names wrap, never truncate.
7. **Book contents:** every line has a block icon, full name, count and source badge (`I/S/C/G`); the badge tooltip shows the recipe/source; no colour-only cue.
8. **Book lifecycle:** book is in slot 8, survives void, is reissued on join/respawn, is never duplicated, and refreshes on selection/completion/gold without closing an open book.
9. **Plot reference:** each active plot has a physical 3D reference + floating name/stars/materials; the plot is empty in front; the reference is removed when the slot is freed.
10. **Completion:** exact block + rotation match completes; extra/wrong blocks prevent completion and show red particles + `N wrong`; double slabs count as a full block.
11. **Partial:** incomplete builds track best percentage and pay at game end; the results line names the build and percentage.
12. **Gold:** first gold at 4:00 elapsed, then every 2:00; 7 stars; separate plot; no team-area reference; 2:00 to complete from memory; failure clears the gold plot; no retroactive score.
13. **Gold HUD:** top-centre countdown uses elapsed time (`GOLDEN BUILD IN: 03:11` at 11:11 remaining) and switches to `GOLD — 01:42 left` when active.
14. **Cart:** team-only 27-slot chest minecart at the plot, not rideable; accepts only manifest materials; hologram `delivered/required`; contents survive void; surplus is gray and retrievable; cleared at game end.
15. **Edge paths:** solo, late join, disconnect/rejoin, three simultaneous builds, build rotation, gold expiry, full inventory and game end all have explicit, tested behaviour with no orphan books/entities.

---

## 9. Open questions

1. Exact gallery activation: right-click pad vs step-on vs button. The wiki image shows a pad; the VOD does not show the click. **M/L.**
2. Does a selected build survive the 90 s rotation? The wiki implies yes (“copy sent to team area”); not explicitly stated. **M.**
3. Does the current book list Gold materials? The wiki says no reference; the book may still list them. **L.**
4. Are all teams offered the same Gold Build at the same time? Not stated. **L.**
5. Can two copies of the same blueprint be active simultaneously? The repeat rule proves repeat after completion; simultaneous copies are unstated. Our design denies duplicates while active. **M.**
6. Are placed blocks returned when a build is cancelled/replaced? No source. Our design returns them. **L.**
7. Are plot dimensions in current MCC 5×5, 7×7 or larger? Not measured; the current code is 5×5. The interaction works either way. **L.**
8. Does the old cart have a chest/custom entity variant? Visual evidence says plain minecart; no storage. **M/L.**
9. How much of the real plot floor is a guide vs decoration? S3 shows a grid; the current code shades used columns. Our design keeps the guide as a toggle. **M.**

---

## 10. Confidence and caveats

- **H** claims are directly stated by S1/S2 or directly visible in S3/S4.
- **M** claims are inferences from layout/adjacency or staged images.
- **L** claims are unresolved; do not build them as if certain.
- The old cart is **Tier 3 historical**. It is not current MCC. Any cart in our plugin is **original** and must be labelled in the rules/UI.
- The current MCC book is a **written book**, so it is reissued rather than live-edited. Our “refresh on change” is an implementation detail, not a claim about MCC internals.
- The wiki gallery/build-area images are **staged**. Use them for composition and interaction affordances, not for exact block counts or map scale.
