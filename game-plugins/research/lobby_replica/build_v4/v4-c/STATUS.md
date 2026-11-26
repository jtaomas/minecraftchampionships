# V4-C — Team section: 10 team columns + 10 Stage Customization plots

Status: **built, generated, QA-passing, rendered. Not pasted on any server.**
Owner: V4-C. Scope from `../BRIEF.md` item 10 and the Ownership table.

| | |
|---|---|
| Build origin | `C4 = 0, 120, 0` (blocks placed `y >= 121` only) |
| Footprint | `x -22..22`, `z -112..-36`, `y 121..133` |
| Blocks | 8,110 |
| Commands | 2,230 across 11 `.mcfunction` files + `load.mcfunction` |
| Heads | 80 (`6` named, `74` placeholder) |
| Signs | 20 team labels |
| QA | `report.txt` — **PASS**; `verify_v4c.py` — **144 checks, 0 failed** |

Run `python gen_v4c.py`, `python render_v4c.py`, `python verify_v4c.py`. All
deterministic. `verify_v4c.py` re-derives every claim from `blocks.csv` alone,
so a bug in the generator cannot hide behind a matching bug in its own report.

---

## 1. What was actually built

Two deliverables, one per the brief, sharing one site:

**A. Ten team columns** (section `00_columns`, 870 blocks)

Each is a 3 × 3 column, seven courses tall, carrying four player-head pedestals
at the top:

```
y 121       3x3 smooth_stone footing pad
y 122..128  3x3 shaft, exactly seven courses: six team concrete + 1 powder cap
y 129       4 smooth_stone pedestal plinths, one per top corner
y 130..132  oak_fence posts + player_head, descending
```

The pedestal heights descend in slot order — the roster slot 1 head is the
highest, slot 4 the lowest:

| slot | corner | post courses | head at |
|---|---|---|---|
| 1 | south-west | 3 | y 133 |
| 2 | south-east | 2 | y 132 |
| 3 | north-east | 1 | y 131 |
| 4 | north-west | 0 (head on the plinth) | y 130 |

The brief's "ordered red/highest first and descending" is implemented twice:
the ten columns are ordered red → pink along the walk, and inside each column
the four heads step down from slot 1 to slot 4.

Column centres, in roster order:

| # | team | side of avenue | column centre | plot centre z |
|---|---|---|---|---|
| 1 | red | west | `-6, -42` | -42 |
| 2 | orange | west | `-6, -58` | -58 |
| 3 | yellow | west | `-6, -74` | -74 |
| 4 | lime | west | `-6, -90` | -90 |
| 5 | green | west | `-6, -106` | -106 |
| 6 | cyan | east | `6, -106` | -106 |
| 7 | aqua | east | `6, -90` | -90 |
| 8 | blue | east | `6, -74` | -74 |
| 9 | purple | east | `6, -58` | -58 |
| 10 | pink | east | `6, -42` | -42 |

**B. Ten Stage Customization plots** (sections `10_plot_red` … `19_plot_pink`, 724 blocks each)

Each plot is a 13 × 13 raised stage, its backdrop turned away from the avenue:

```
v = local depth, 0 = back wall, 12 = front step      u = 0..12 along the avenue
y 121..122  stone_bricks retaining wall (2 visible courses)
y 123       deck course, team concrete;  v=12 is the front step (smooth_quartz, 121..122)
y 124       team carpet field, 11 x 10, with a 1-wide darker rim in the team's own
            terracotta, exactly the way the reference plots read
y 124..128  back wall, 13 wide x 2 deep x 5 courses, powder cap on top,
            with a 9 x 3 opening in the front layer and a recessed
            light_gray_concrete panel behind it
y 124..128  four player-head statues at u = 2, 5, 8, 11 on v = 4:
            smooth_stone plinth, three oak_fence courses, player_head at y 128
y 123       oak wall sign on the front step, facing the avenue, team name
```

The wall panel is the same motif the reference plots use for their custom
murals (see `crops/twitch_plot.png`: purple frame, recessed grey panel). Here
it is deliberately **blank** — see GAPS.md §4.

## 2. Site plan

The brief fixes the counts, the order and the column dimensions, but the real
hub's Stage Area plan is not visible in any reference image, so the site had to
be authored. It is a **team avenue** running due north off the dome plaza, so
the section clears everybody else's ground:

```
        z=-112   <- north edge of the section (far corner of the last plots)
        z=-106   green (W) [plot][col] |avenue| [col][plot] cyan (E)
        z=-90    lime                                       aqua
        z=-74    yellow                                     blue
        z=-58    orange                                     purple
        z=-42    red                                        pink
        z=-36    <- south edge of the section
        z=-30    <- dome plinth rim (V4-A)
```

* avenue `x -4..4` is **V4-A's** paving — nothing of mine is placed there
* plots west `x -22..-10`, east `x 10..22`; columns `x -7..-5` and `x 5..7`
* two clear cells (`x -9..-8`) between each column and its plot
* 3-block gaps between neighbouring plots; each plot front has a 2-course
  smooth-quartz step up to the deck
* nearest build to the dome centre: 37.4 blocks (plinth edge is at 30) — no
  overlap, and `report.txt` counts **0** blocks inside `r=30`
* farthest point `(22, -112)` → 114 blocks from the centre, inside V4-A's island

Colour pairing across the avenue is deliberate: red/pink, orange/purple,
yellow/blue, lime/aqua, green/cyan — i.e. the pair you meet at the same z is
always the colour 180° round the roster, which is how the rainbow ring of the
plinth orders the teams. Walking in from the plaza you meet red first and pink
last, in the roster order the brief requires.

## 3. How to place it

```
blocks.csv                              authoritative list, x,y,z,block
functions/v4c/*.mcfunction              setblock/fill, 11 parts + load
datapack/                             pack_format 48, data/mcchub/function/v4c
```

Either `function mcchub:v4c/load` from a datapack, or run the files in
`functions/v4c/` in filename order. Every part is independent, so V4-E can
place or drop a single team's plot.

* Nothing is placed at or below `y = 120` — V4-A owns the island surface.
* The avenue floor is not built (V4-A's paving/axis). Previews draw a
  `dirt_path` strip there as **context only**; it is not in `blocks.csv`.
* `fill` is used for runs; blocks with NBT (signs, named heads) are emitted as
  one `setblock` each, so no NBT is ever lost to a fill.

## 4. Previews

| file | what it shows |
|---|---|
| `preview_top.png` | plan: 10 plots, both rows, roster order, avenue, columns |
| `preview_iso.png` | the whole section from the south (approach view) |
| `preview_iso_north.png` | the same from the north |
| `preview_columns.png` | the four southern teams close up: plot + column pairs |
| `preview_plot_red.png` | one plot, close: deck, wall, four statues, nameplate |
| `preview_plot_pink.png` | the opposite row, from the north |

Rendered with `tools/render_preview.py` through `render_v4c.py`, which only
registers a colour for `player_head` (the shared table has no entry for it, so
untouched it would fall back to a hash colour) and then calls the tool's own
`main()`. Nothing else is changed.

## 5. Comparison with the references

Read from these crops (in `crops/`, made by `crop.py` / `crop2.py` / `crop3.py`):

* `sd_columns_left.png`, `s6_columns_left.png` — the team-coloured columns at
  the entrance: solid team colour, flat sides, stepped/capped top, snowmen on
  top in the festive build.
* `s1_cols_zoom.png` — a Season-1 team column: a solid colour shaft with a
  **single** player head on top; also a giant open book and a chunky stepped
  orange column, i.e. each team's column was individually custom-built.
* `twitch_plot.png` — a whole plot: coloured floor close to the ground on a
  stone-brick base, thin dark line inset in the floor, 2-deep back wall with a
  recessed grey panel behind a coloured frame, one head-on-post statue, a
  gold-tipped lamp post.
* `sa_blue_heads.png`, `anc_heads_purple.png`, `sa_plots_right.png` — four
  head-on-post statues per plot, evenly spaced along a plot edge; plots cut
  into a terraced slope behind tall stone-brick retaining walls.
* `pride_heads.png` — the same statue convention on a quartz-stepped plot.

Faithful: the plot vocabulary (stone-brick skirt, bright team floor, thin dark
inset line, 2-deep backdrop with a recessed panel, four head statues per plot,
team nameplate), the column vocabulary (solid team colour, capped top, heads on
pedestals), the roster order.

Deliberately different, and why:

* **uniform columns** — the references show ten individually customised
  structures (a 2×2 purple shaft ~16 high, a 4×4 stepped orange tower, a giant
  book). The brief mandates one uniform 3 × 3 × 7 column, so this build is the
  brief's type, not any one team's custom build.
* **straight avenue** — the reference layout is an organic terraced garden.
  Authored, because it also has to coexist with V4-A/B/D ground.
* **no trees, lamps, coins, presents, snowmen** — props are V4-D's scope, and
  the festive dressing is the snow variant.

## 6. Deliberate non-scope

Not built, per the brief: the dome, the island and terraces, the mountain, the
Hall of Fame, the rainbow bridge, the snow globe, the aqua building, the six
training portals, the entrance columns, and every prop. Nothing outside
`game-plugins/research/lobby_replica/build_v4/v4-c/` was touched, and no server
was contacted.

## 7. Files

```
gen_v4c.py            generator (layout, palette, emit, checks, preview dumps)
verify_v4c.py         independent check of blocks.csv (144 assertions)
render_v4c.py         preview wrapper around tools/render_preview.py
crop.py crop2.py crop3.py   reference-crop helpers (study only, not build input)
crops/                13 reference crops cited by STATUS.md / GAPS.md
blocks.csv            8,110 blocks
functions/v4c/        .mcfunction parts + load.mcfunction
datapack/             the same as a datapack (pack_format 48)
preview*.png          previews
preview*.mcmdump.gz   preview dumps (context ground is preview-only)
report.txt            generated QA numbers
STATUS.md GAPS.md
```

One cosmetic detail worth knowing before any later edit: the four statue plinths
sit at plot-local `u = 2, 5, 8, 11`, which is even spacing centred on the plot,
and that puts the right-hand plinth on the floor's dark rim — 37 of the rim's 38
cells stay visible. That is deliberate (the reference plots stand their heads on
the field edge) but it is a one-cell overlap, and `verify_v4c.py` asserts the
exact expected block for every one of those cells.
