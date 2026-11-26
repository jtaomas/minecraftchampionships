# Build data format (proposal v2)

A build is **authored data**, not code. One build = one JSON object; the whole
catalogue = one JSON array (or one file per build, loaded by id).

The format is designed around four facts from the MCC research:

1. MCC's standard build volume is **7×7 in plan**, not 5×5, and builds routinely
   reach 7–10 blocks high (source: `img/piston.png`, `img/bootcamp.png`,
   `img/gallery.png`).
2. Stars are **hand-assigned per build**, not derived from block count. TKO is
   51 blocks at 3★ while a 1★ letter build can be 40 blocks. So `stars` is a
   stored field.
3. Completion has to compare **block state subsets** (stairs/slabs/doors), not
   just `Material` (MCC AS rule).
4. `block_slots` (completion units) and `item_cost` (what players must gather)
   are **different numbers**. A double slab is 1 slot but 2 items; a door is
   2 slots but 1 item.

---

## 1. File layout

```
data/
  builds/                      one JSON per build (preferred)
    bm_camp_stool.json
    bm_windmill.json
  builds.index.json            { "schema": 2, "ids": ["bm_camp_stool", ...] }
  palette.json                 optional: shared char -> block mappings
  recipes.json                 craft/smelt chains used by "source"
```

Runtime loads `builds.index.json`, then each build lazily. A build never
references another build.

---

## 2. Build object

```json
{
  "schema": 2,
  "id": "bm_camp_stool",
  "name": "Camp Stool",
  "stars": 1,
  "gold": false,
  "dims": { "x": 3, "y": 2, "z": 3 },
  "anchor": { "corner": "min", "y": "plot_floor" },
  "palette": {
    ".": "minecraft:air",
    "a": "minecraft:oak_planks",
    "f": "minecraft:oak_fence"
  },
  "layers": [
    ["f.f", "...", "f.f"],
    ["aaa", "aaa", "aaa"]
  ],
  "states": {},
  "materials": [
    { "id": "minecraft:oak_fence",  "count": 4, "source": "craft",
      "recipe": "6 planks + 2 sticks -> 3 fences" },
    { "id": "minecraft:oak_planks", "count": 9, "source": "craft",
      "recipe": "1 log -> 4 planks" }
  ],
  "completion": { "block_slots": 13, "item_cost": 13 },
  "rotation": { "mode": "state_exact_texture_agnostic", "sensitive": [] },
  "reference": {
    "mode": "pedestal",
    "hologram": ["name", "stars", "materials"],
    "hide_in_team_area": false
  },
  "tags": ["furniture", "wood", "tier1"]
}
```

### Field rules

| field | type | rule |
|---|---|---|
| `id` | string | `bm_` + snake_case, stable forever, never reused |
| `name` | string | display only; 1–24 chars; may change |
| `stars` | int | **1–5 or 7 only**. 6 is illegal. 7 requires `gold: true` |
| `gold` | bool | `true` ⇒ 7★, no team-area reference, 2-minute timer |
| `dims` | int3 | must equal the parsed layer extents; ≤ 9×16×9 |
| `anchor` | enum | `min` corner of the plot floor; y=0 is the first layer |
| `palette` | map | 1 char → block id or block-state string; ≤ 62 keys |
| `layers` | array | index 0 = bottom. Each layer = `dims.z` strings of `dims.x` chars |
| `states` | map | optional per-char state check policy (see §4) |
| `materials` | array | derived from layers, then validated; never hand-edited |
| `completion.block_slots` | int | non-air cells inside `dims` |
| `completion.item_cost` | int | sum of `materials[].count` |
| `rotation.mode` | enum | `ignore` \| `state_exact_texture_agnostic` \| `state_exact_full` |
| `rotation.sensitive` | array | block ids whose placement state is checked |
| `reference.mode` | enum | `pedestal` \| `wall_panel` \| `none` |
| `reference.hide_in_team_area` | bool | must be `true` when `gold` |
| `tags` | array | free-form; used by the selector, not by scoring |

### Why layers are still strings

The current `BuildPreset` layer-string format is genuinely good: diffable,
hand-editable, readable in a PR, and 7 chars per row stays inside a phone
width. Keep it. The two changes are (a) `dims` up to 9×16×9 and (b) a `states`
map so a char can mean `oak_stairs[facing=east,half=bottom,shape=straight]`
instead of only a material.

For rows wider than 9, authoring is done in a spreadsheet and exported; the
runtime never sees CSV.

---

## 3. Multi-state characters

A char maps to a state string. Two chars may map to the same block with
different states:

```json
"palette": {
  "s": "minecraft:oak_stairs[facing=north,half=bottom,shape=straight]",
  "t": "minecraft:oak_stairs[facing=east,half=bottom,shape=straight]"
}
```

This is required: MCC AS onward, a build only completes when stairs, slabs and
open doors are **rotated correctly** (source: wiki History, MCC AS entry).

---

## 4. Completion matching

For every cell in `dims`:

```
want = resolved block + state subset
have = block at (origin + x, floor + 1 + y, origin + z)
```

State subsets by mode:

| mode | compares | use |
|---|---|---|
| `ignore` | block id only | 1★ builds, wool/concrete shapes |
| `state_exact_texture_agnostic` | id + `facing`, `half`, `type` (slab), `shape`, `open`, `axis` **excluded**, door `hinge` excluded | default for 2★+ |
| `state_exact_full` | every property | debug/validation only |

`texture_agnostic` is the MCC rule: "Texture rotation is not needed to complete
the build (e.g. Door hinges in the wrong position still counts)". Double slabs
count as one full block.

Scoring (matches MCC current):

```
coins = floor(stars * 20 * correctSlots / block_slots)
```

No speed bonus, no placement-order bonus. The only extras are the three
Employee of the Month bonuses (100/50/25 for 1st/2nd/3rd).

---

## 5. Materials and sources

`materials[].source` is one of:

- `island_ore`, `island_log`, `island_stone`, `island_colour`, `island_nether`,
  `island_coral`, `island_copper`, `island_garden`, `island_snow`
- `craft` (recipe required at a crafting table)
- `smelt` (furnace, fuel pre-placed in the team area)
- `stonecut` (stonecutter; 1 input → 1 output, no waste)
- `shop` (a counter in the mart buys it for raw blocks)

The build only stores `source` for the shopping-list hint and the "shop"
badge. The authoritative recipe list lives in `recipes.json` so one recipe fix
updates every build.

Example chains the current plugin does not model:

```
sand        -> smelt -> glass
cobblestone -> smelt -> stone -> craft -> stone bricks
clay        -> smelt -> brick -> craft(4) -> bricks
log         -> craft -> planks -> craft -> sticks / stairs / slabs / fences / doors
stone       -> stonecut -> stairs / slabs / walls
copper ingot-> craft(9) -> copper block -> stonecut -> cut copper
```

`raw_copper -> smelt -> copper_ingot` and oxidised copper are separate
island materials (MCC 20 replaced the coral island with a copper island).

---

## 6. Reference display contract

| `reference.mode` | Where | What the player sees |
|---|---|---|
| `pedestal` | Blueprint Gallery | the build itself on a 7×7 pad, 1-block white rim, floating hologram (`name`, `★`, `materials`) |
| `wall_panel` | team build area | a 1:1 copy on a lit pedestal in front of the empty plot |
| `none` | Gold plot only | empty 7×7 plot + sign "copy this from the mart" |

Gold (`hide_in_team_area: true`) must never render a copy in the team area. The
only legal view is the gallery pedestal, which is why the gold build has to be
memorised.

---

## 7. Validation (run at load; fail loudly)

1. `layers.length == dims.y`
2. every layer has `dims.z` rows, every row `dims.x` chars
3. every char exists in `palette`; `.` must be air
4. no non-air cell outside `dims`; no floating island of blocks with no
   face-adjacent path to the floor (reuse `BuildPreset.unsupported()`)
5. `completion.block_slots == count(non-air)`
6. `completion.item_cost == sum(materials.count)`
7. `stars == 7` ⇔ `gold == true`; `gold` ⇒ `reference.hide_in_team_area`
8. `stars <= 2` ⇒ `rotation.mode != state_exact_full`
9. every material resolves through `recipes.json` to a gatherable base block
10. `dims.x <= 9`, `dims.z <= 9`, `dims.y <= 16`

Checks 5 and 6 are why `materials` is generated: a hand-typed count is the
single most likely data bug, and a wrong shopping list is worse than a wrong
star rating.

---

## 8. Loader shape (Java, drop-in for `BuildPreset`)

```java
record BuildDef(
    String id, String name, int stars, boolean gold,
    int sx, int sy, int sz,
    Material[][][] grid,        // [y][z][x], null = air
    Map<Material,Integer> counts,
    int slots, int itemCost,
    RotationMode rotation, Set<Material> sensitive,
    ReferenceMode reference, boolean hideInTeamArea,
    List<String> tags) {}
```

`at(x,y,z)`, `columnUsed(x,z)` and `unsupported()` keep their current
signatures, so `BuildMartGame.evaluate()` changes in one place only: compare
`BlockData` against the stored state instead of `Material`.
