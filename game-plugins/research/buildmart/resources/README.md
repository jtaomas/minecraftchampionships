# Build Mart — resource islands, block locations and shopping/cart (research)

Scope: where every block is in the current MCC Build Mart, how a player gets it
into their inventory, what the old shopping cart actually was, and what the 25
current presets demand from that palette. Research only — no source under
`game-plugins/BuildMart/` was edited.

Deliverables: this file, `data/blocks.csv` (machine-readable island/block/slot
table), `data/islands.md` (human island + travel table), `frames.md` (VOD frame
index), `frames/REFERENCE_IMAGES.md` (image-pack index), `frames/pack/` (29
full-resolution stills/crops the builder can open directly).

---

## 1. Method

1. Read the shared brief, `BuildMart/README.md`, `BuildPreset.java` (25 builds,
   5×5×5, legend) and `BuildMartGame.java` (27 raw stalls, 4 shop counters,
   3 s regen).
2. Pulled the authoritative wiki pages as raw wikitext and the official
   available-blocks poster (`BuildMartMap.png`), plus five official map/area
   photos.
3. Downloaded **two targeted VOD windows only** (7 min total):
   - MCC 19 TommyInnit POV, 0:00–3:00 — the only event with the Snow island.
   - MCC 20 5up POV, 2:18:40–2:22:40 — first event with Copper.
4. Extracted ~200 stills at 1–2 s intervals for triage, then kept **28**
   indexed stills plus contact sheets and curated 29 images into the pack.
5. Parsed `BuildPreset.java` programmatically to cross-check demand against the
   live stall list. Numbers below are from that parse, not hand-counting.

No full VOD downloaded. No map copied. No third-party build/world used.

---

## 2. Sources

| Ref | Source | Use | Tier |
|---|---|---|---|
| W1 | `mcchampionship.fandom.com/wiki/Build_Mart_(Map)` | map description, elytra/fan/pads, appearances | 1 |
| W2 | `mcchampionship.fandom.com/wiki/Big_Sales_at_Build_Mart` (raw wikitext) | 3 plots + gold, gallery, book/list, chests/crafting/smelting, history | 1 |
| W3 | `File:BuildMartMap.png` (upload 2022-04-21), captioned "all available blocks … since MCC 20" | exact block list per category | 1 |
| W4 | `File:Build-Mart-Revised.png`, `File:Build-Mart-Original.png`, `File:Build-Mart-Revised-Blueprint.png` | upper/lower split, island silhouettes, build-area layout | 1 |
| W5 | `File:MCC-buildmart.png` (original map) | shopping carts and mall departments | 1 |
| W6 | `File:BSABM-BuildArea.png`, `File:BSABM-BlueprintGallery.png` | plot material hologram, 7-model gallery | 1 |
| V1 | YouTube `vZTZfMwhyyA` (TommyInnit, MCC 19) 2:42–2:58 | Snow island, subtitles | 1 |
| V2 | YouTube `biXUdZgjqlw` (5up, MCC 20) 2:19:26–2:22:30 | ring order, copper, colour bays, powder/glass | 1 |
| C1 | `game-plugins/BuildMart/src/.../BuildPreset.java` | 25 presets and legend (code, not design source) | context |
| C2 | `game-plugins/BuildMart/src/.../BuildMartGame.java` | 27 stalls, 4 shops, regen/sale values | context |

---

## 3. Findings

### F1 — Three-tier mart: hub, lower colour ring, upper category ring (H)

The current map is a central hub with a giant fan and four launch pads, a
**lower ring of colour bays**, and an **upper ring of eight category islands**
(W1; V2 2:20:00, 2:21:10, 2:21:32). The wiki's "upper islands = ores/logs/stones,
lower islands = coloured blocks" is correct but understates the lower layer: it
is a continuous ring of individually labelled colour bays around the fan, not a
single island (V2 2:20:00).

The brief's category list omits **Bricks** and **Sand** and lists **snow** as if
standard. Correct set for MCC 20+: ores, nether, bricks, logs, sand, stone,
copper, garden (upper) + 16 colour bays (lower). Snow is MCC 19 only.

### F2 — Exact block list per island (H, poster)

From W3, with crops in `frames/pack/01…09`:

| Island | Blocks |
|---|---|
| Ores | redstone block, diamond block, emerald block, gold block, iron block, coal block, quartz block |
| Nether | quartz block, warped wart block, nether wart block, nether bricks, blackstone |
| Bricks | bricks, prismarine bricks, mossy stone bricks, stone bricks, nether bricks |
| Lumber | birch, spruce, jungle, oak, dark oak — **log and planks for each** |
| Sand | sand, sandstone, red sand, red sandstone |
| Stone | stone, diorite, andesite, granite |
| Copper | block of copper, cut copper, oxidized copper, oxidized cut copper |
| Garden | lily of the valley, allium, cornflower, oxeye daisy, dandelion, blue orchid, azure bluet, poppy, pink tulip, orange tulip, oak sapling, moss block (+ azalea from MCC 20) |
| Colour (×16) | concrete, concrete powder, stained glass in that colour |

Important detail: the "ores" are **mineral blocks**, not ore textures. The
"lumber" island gives planks as well as logs, so no log→plank crafting is needed.

### F3 — Colour bays: glass + concrete + powder; no wool (H)

The poster title is "COLOURS (GLASS AND CONCRETE)" and shows 10 columns × two
rows (W3) — that is the build palette, not necessarily the bay count. In game,
a single bay also stocks **concrete powder**: the subtitle reads
`Gray Concrete Powder (Black Stained Glass)` (V2 2:22:18), and white concrete +
white glass share the white bay (2:21:40). So each bay = three forms.

The physical ring has **at least 11 labelled bays** (we read WHITE, GRAY, BLACK,
RED, ORANGE, YELLOW, LIME, GREEN, BLUE, PURPLE, PINK) and the top-down framing
suggests 16; we model 16 and mark the five unseen colours M. No wool appears in
any bay, any poster crop, or any frame. This matters because the current 25
presets are **47% wool by block count** (252 of 536 placed blocks); see F8.

### F4 — Ring order, duplicates and event changes (M/H)

Upper ring order reconstructed from three shots: `STONE–COPPER–GARDEN`
(V2 2:21:10), `NETHER–BRICKS` with a pickaxe/ORES sign on the other side
(2:21:32 + first-revision still W4), and `SAND–STONE–LOGS` (2:22:12). Those
edges close into one ring with no contradictions:
**ores → nether → bricks → logs → sand → stone → copper → garden → ores** (M,
because no single frame pans the whole ring; every edge is seen at least once).

Duplicates (W2 history, H):
- **Quartz block** on Ores **and** Nether (since MCC P21).
- **Nether bricks** on Nether **and** Bricks (since MCC P21).

Changes (W2 history):
- **MCC 17** — scoring only; no block/map change.
- **MCC 19** — **Snow replaces Sand for that event only** (packed ice, snow
  block, snow layer; V1 2:54–2:58).
- **MCC 20** — **Coral → Copper**, all copper + oxidized variants; azalea and
  moss added to Garden.
- **MCC 21** — Build Mart **not chosen**; there is no MCC 21 map change.
- (MCC P21 — separate Pride event — introduced the duplicates and retextured
  logs.)

### F5 — Stall interaction: mine → inventory, no drops, fast regen (H/M)

- Mining a resource block puts it **straight into the inventory**; no item
  entities, no pickup step (C2 behaviour matches footage; V2 mining copper).
- Blocks regrow a few seconds later (looks 2–3 s; current code 60 ticks = 3 s)
  (V2 2:21:14–2:21:32; M on exact ticks).
- Falling off an island respawns you and **loses everything gathered** (W1;
  current code implements this at Y<45).
- There are **no shop counters in the mart**. Crafting/smelting happens in the
  build area: crafting tables, stonecutters, two fuelled smelting areas, five
  colour-coded chests (W2, H). Current code's four mart shop counters are an
  invention; the real conversion is raw→crafted at the build area.
- Colourblind support: looking at a block prints its name as a subtitle, and the
  held item is shown in parentheses when it differs (V2 2:22:18, H). Bay names
  are also written in huge letters (BLUE, RED, YELLOW…), a second colour cue.

### F6 — The old "shopping cart" was travel, not a container (H)

Pre-MCC 14 the mart was a **mall with departments** (LUMBER, STONE, COLOURS,
ORES signs) and players could "run on their feet or use the shopping carts to
travel faster but with less control… change their method at will"; pitfalls made
you lose all resources (W1 old-map page). The cart is a rideable grey minecart;
it carries the **player only**. Your inventory is the container; falling loses
it. Carts did not hold blocks and were not a shopping-list UI.

MCC 14 replaced carts with elytra + the central fan + launch/speed pads (W1).
So a cart rebuild should be a **movement option on the mall map**, not an
inventory or shop UI. *Scope note: cart handling/travel tuning belongs to the
cart agent; this dossier only settles what the cart is and that it carried no
blocks — inventory and block locations are the parts needed here.*

### F7 — Shopping list: book + plot hologram + gallery model (H/M)

Current MCC gives each player a **shopping-list book** in the inventory and also
prints the material list **on the build plot** (W2). The plot hologram shows the
build name, star rating and counts, e.g. `TWO ★★☆☆☆ / Materials: Black Concrete
9x, Blue Concrete 12x, Chain 18x, Weathered Cut Copper 4x, Oxidized Cut Copper 8x`
(W6, H). The blueprint gallery shows seven mini models on pedestals and
teleports the chosen blueprint to a free plot (W2, W6).

Current code has neither book nor gallery: it uses floating text over the
pedestal and a one-shot chat message (`sendShoppingList`), and marks
shop-made materials with `(shop)` (C2). For parity, add a written book and keep
the plot hologram.

### F8 — Demand cross-check: no material is missing, but the palette is wrong (H)

Parsed all presets (`data/preset_demand.csv`, `data/demand_summary.csv`):

- **25 presets**, not 27. There are 27 **raw stalls**; the brief conflated them.
- Total placed blocks across one pass of all builds: **536**.
- 25 distinct materials: 22 raw + 3 shop (`GLASS`, `BRICKS`, `GOLD_BLOCK`);
  `STONE_BRICKS` is defined as a shop product but no preset places it.
  **No preset material is unattainable** — every one is either a current raw
  stall or a shop output.
- **Wool is 252/536 = 47%** of all placed blocks (red 74, black 44, white 31,
  yellow 26, lime 23, pink 22, green 16, orange 7, light blue 5, blue 4). The
  real map stocks **no wool** (F3).
- Biggest single-build pulls: **red wool 25** (Cottage), **snow 36** (Igloo),
  **sandstone 34** (Desert Pyramid), **red wool 21 + glass 8** (Lighthouse).
  A current 2×2×3 stall holds 12, so Cottage red wool alone needs 3 regen trips.
- **Dead stalls** (0 presets use them directly): `BIRCH_PLANKS`, `BROWN_WOOL`.
  `SAND`, `CLAY`, `IRON_BLOCK` are used only as shop inputs.
- **Unused shop product**: `STONE_BRICKS` is produced by the Mason but no preset
  places it. The Jeweller's `GLOWSTONE` input is not on the MCC 20 poster, and
  `GOLD_BLOCK` is mined directly on Ores, so the Jeweller recipe is
  non-canonical.
- Materials the presets assume that are **not on the real islands**:
  the 10 wools used by presets (plus the unused brown-wool stall), `SNOW_BLOCK`
  (event-only), `CLAY`, `GLOWSTONE`, and the four garden guesses `PUMPKIN`,
  `HAY_BLOCK`, `MELON`, `OAK_LEAVES` (poster garden has sapling/moss, not
  pumpkin/melon/hay/leaf blocks).
- Only **7 exact matches** between the 27 stalls and the real palette: oak
  planks, oak log, spruce planks, sand, sandstone, quartz and iron block, plus
  cobblestone as a stone derivative. `data/current_vs_real.csv` lists every
  stall with a match verdict.

Bottom line: **no stall is "missing a material used by the presets"** — the
opposite. The stalls are a bespoke palette; the presets must be re-authored
against the 51-material upper-island palette plus 16 colour bays (or the map
must deliberately deviate and keep wool/pumpkin/melon/hay).

### F9 — Data model for our map (proposal)

```
Island { id, name, tier: HUB|LOWER|UPPER|BUILD, category,
         centre: (x,y,z), radius, angleDeg, travelSec,
         stalls: [Stall] }

Stall  { id, islandId, material, form (block|log|planks|flower|concrete|powder|glass),
         local: (x,y,z), stock, regenTicks, recipe?, duplicateOf? }

Regen  { pos, material, dueTick }          // mine: give directly, then schedule
Lookup Map<Material, List<StallRef>>       // duplicates → 2 entries
```

Runtime rules that follow from the research:

1. On break at a stall: cancel drops, `inventory.addItem(material)`, enqueue
   `dueTick = tick + (saleActive ? 10 : 60)`. Refill only if the block is still
   air.
2. `lookup(material)` returns all stalls; choose the nearest by 3-D distance,
   with a tie-break preferring the same tier (lower ring is faster).
3. Duplicates are features: quartz and nether bricks intentionally resolve to
   two islands so teams spread out.
4. Crafted items are not stalls: route them to `build_area` recipes
   (sand→glass, cobblestone→stone→stone bricks, clay→bricks, log→planks).
5. `blocks.csv` is the source of truth; the island centre + `local_x/local_z`
   give the world slot. Angles/radii are proposed for our 320×320 layout.

`data/blocks.csv` columns:
`island_id, island_name, tier, category, angle_deg, radius, y, slot,
local_x, local_z, material, form, stock, regen_ticks, recipe, duplicate_of,
confidence, source` — 104 rows.

### F10 — Travel budget (L)

With hub radius 24, colour ring 56, upper ring 128 and +40 Y:

| Trip | Distance | Time (elytra 20–30 b/s + climb) |
|---|---|---|
| Adjacent colour bay (on foot) | ~22 blocks | 2–3 s |
| Opposite colour bay / over fan | ~112 blocks | 6–8 s |
| Hub → any upper island | ~128 + 40 up | 8–12 s |
| Adjacent upper islands | ~50 blocks | 4–6 s |
| Full loop of upper ring | ~800 blocks | 35–45 s |

These are design targets, not measured MCC values (no coordinates published).

---

## 4. Design implications for the rebuild

1. **Build the ring, not 27 stalls.** Eight upper category pads + 16 lower
   colour bays around a fan hub is the recognisable silhouette and fixes the
   "flat 27-stall grid" problem.
2. **Drop wool or add a deliberate wool bay.** About half of all placed blocks
   in the current presets are wool. Either re-author to concrete/glass/powder, or keep
   one extra "Textiles" bay and mark it as a deviation.
3. **Move crafting out of the mart.** Remove the four shop counters from the
   stall grid; put crafting tables, stonecutters and two furnaces in the build
   area, and make the recipes raw→crafted (glass, stone, stone bricks, bricks).
4. **Make the plot list the primary UI.** A hologram with name, stars and
   per-material counts, plus a written book in slot 8. Highlight materials that
   are on sale, and mark crafted ones.
5. **Keep the colourblind subtitle.** Ray-trace the block under the crosshair
   and show `Material Name` in the action bar; include the held item in
   parentheses when it differs. Label every bay in giant letters.
6. **Price the ring in seconds.** A build should need at most 2–3 island stops;
   otherwise teams spend the game flying. Cluster complementary islands
   (ores next to nether; logs next to sand/stone).
7. **Fall = lose inventory.** Keep it; it is the map's main risk and makes the
   fan route meaningful.

---

## 5. Open questions

1. Exact 16-bay angular order (only 11 labels read; ring wider than one frame).
2. Whether any MCC variant stocks wool (all sources say no for MCC 20+).
3. Exact regen ticks (looks 2–3 s; 60 ticks is a safe rebuild value).
4. Whether the teal Bricks tile is plain prismarine or prismarine bricks.
5. Where the build-area portal lands relative to the rings (frames suggest the
   portal side is opposite GARDEN/COPPER, but this is L).
6. How the modern P25+ gallery co-exists with the ring (gallery is in the
   central hall; the stall map appears unchanged).

---

## 6. Implementation checklist (testable)

- [ ] Eight upper islands exist with the exact block sets in F2; a `/bm blocks`
      debug command lists them from `data/blocks.csv`.
- [ ] 16 colour bays each dispense concrete, concrete powder and stained glass;
      no bay dispenses wool.
- [ ] Quartz resolves to Ores **and** Nether; nether bricks to Nether **and**
      Bricks (lookup returns two entries).
- [ ] Mining any resource block gives the item directly (no drop entity) and
      refills in ≤3 s; refill does not overwrite a placed block.
- [ ] Falling below Y=45 respawns the player and clears non-tool inventory.
- [ ] No shop counters in the mart; glass/stone/stone bricks/bricks are made at
      build-area stations with the F5 recipes.
- [ ] Plot hologram shows build name, stars, and `count x material` for every
      material, with crafted items marked.
- [ ] Each player receives a written shopping-list book on spawn and after each
      completion.
- [ ] Looking at a block shows its name in the action bar for ≥1 s; looking at a
      different held item appends `(Held Item)`.
- [ ] Each colour bay has its colour name in letters ≥3 blocks tall on the
      outer wall.
- [ ] Adjacent colour bay ≤3 s on foot; hub→upper island ≤12 s with elytra+fan.
- [ ] A single build never needs more than 3 island stops at 32 stock per stall.
- [ ] 25 presets re-authored against the real palette (or a documented
      wool/textile deviation); no preset uses an unobtainable material.
- [ ] `BIRCH_PLANKS`, `BROWN_WOOL` and the unused Mason product are either used
      by a preset or removed.
- [ ] `blocks.csv` loads at startup and fails loudly on an unknown material.

---

## 7. Files

| Path | Contents |
|---|---|
| `README.md` | this dossier |
| `data/blocks.csv` | 104 rows: island, tier, angle/radius/y, slot, local x/z, material, form, stock, regen, recipe, duplicate, confidence, source |
| `data/islands.md` | island/ring/travel tables + material→island lookup |
| `data/preset_demand.csv` | every preset × material with counts, stars, shop count |
| `data/demand_summary.csv` | material totals, max per build, which builds use it |
| `data/current_vs_real.csv` | all 27 live stalls vs the real palette, with verdicts |
| `frames.md` | 28 indexed VOD frames with timestamps and confidence |
| `frames/REFERENCE_IMAGES.md` | annotated image-pack index |
| `frames/pack/` | 29 full-resolution reference stills/crops |
| `gen_blocks.py`, `gen_presets.py`, `parse_presets.py` | generators/parsers used above |
