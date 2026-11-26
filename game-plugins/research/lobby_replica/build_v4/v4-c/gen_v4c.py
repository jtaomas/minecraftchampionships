#!/usr/bin/env python3
"""
V4-C  --  MCC festive hub replica: the TEAM SECTION.

Build site : lobby_build (V4 integration), build origin C4 = 0,120,0
Owner      : V4-C  (see ../BRIEF.md, item 10, and "Ownership")
Scope      : exactly 10 team columns (red .. pink, red first) + 10 Stage
             Customization plots (one per team), player-head pedestals,
             team labels.  Nothing else.

What this generates
-------------------
* a "team avenue" running north off the dome plaza.  Two rows of five plots
  face the avenue - red/orange/yellow/lime/green on the west side (south to
  north), cyan/aqua/blue/purple/pink on the east side (north to south), so a
  visitor meets the teams in the canonical order red .. pink.
* 10 team columns, one per team, 3x3 and 7 courses tall on a 3x3 stone
  footing, standing on the avenue edge directly in front of their own plot,
  each carrying four player-head pedestals at descending post heights
  (highest first) and a small wall-sign nameplate reading the colour.
* 10 plots, each a 13 x 12 raised stage: stone-brick retaining wall, team
  concrete deck, black carpet outline, team carpet field, 2-deep back wall
  with a recessed light-grey panel, four player-head statues ("skin NPCs",
  MCC's own pedestal convention) and a team-name wall sign on the front step.

Output
------
  blocks.csv                        x,y,z,block  (authoritative block list)
  functions/v4c/*.mcfunction        chunked commands (setblock / fill)
  datapack/.../function/v4c/*       the same, ready as a datapack
  preview.mcmdump.gz                build + preview-only ground context
  preview_top.png / preview_iso.png / preview_iso_*.png / preview_*_detail
  report.txt                        generated QA numbers (see STATUS.md)

Checks
------
  gen_v4c.py     self-report  -> report.txt
  verify_v4c.py  independent re-derivation of every claim from blocks.csv

Conventions
-----------
* Build origin C4 = 0, 120, 0.  y <= 120 belongs to V4-A's island, so nothing
  here is placed below y = 121.  The avenue paving is V4-A's too: this section
  starts at y = 121 and shows the avenue only in the preview context.
* Vanilla Java 1.21.1 ids only.  Block states/NBT kept minimal on purpose.
* The four player heads per team are the team roster.  Only the Lime Llamas
  roster is legible in the references; every other slot is a bare
  minecraft:player_head placeholder - see GAPS.md.

Run:  python gen_v4c.py
"""

from __future__ import annotations

import gzip
import csv
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
FN_DIR = os.path.join(HERE, "functions", "v4c")
DP_DIR = os.path.join(HERE, "datapack", "data", "mcchub", "function", "v4c")
NS = "mcchub"
CHUNK_CMDS = 6000

# --------------------------------------------------------------------------
# Site constants
# --------------------------------------------------------------------------
Y_MIN = 121            # lowest course this section may touch (island top = 120)
Y_DECK = 123           # plot deck: block at y=123, walkable surface y=124
Y_WALL0 = 124          # plot back wall, first course
WALL_H = 5             # 124..128 included
Y_COL0 = 121           # team column first course
COL_H = 7              # 121..127 included

PATH_HALF = 4          # avenue x -4..4  (V4-A paving; preview context only)
PLOT_W = 13            # plot extent along the avenue (local u)
PLOT_D = 13            # plot depth away from the avenue (local v)
PITCH = 16             # plot pitch along the avenue
ZC0 = -42              # centre of the south-most plot of either row
WEST_BACK_X = -22      # back-wall cell of a west plot; front step is x -10
EAST_BACK_X = 22       # back-wall cell of an east plot; front step is x  10
COL_WEST_X = -7        # west team column occupies x -7..-5
COL_EAST_X = 5         # east team column occupies x  5..7

# preview / QA region  (x0, z0, y0, x1, z1, y1)
RG = (-28, -118, 118, 28, -30, 136)

# --------------------------------------------------------------------------
# Teams.  Order is the MC Championship order required by the brief:
# red, orange, yellow, lime, green, cyan, aqua, blue, purple, pink.
#
# players: four roster slots, in-roster order.  None = unknown -> bare
# minecraft:player_head placeholder.  Only the Lime Llamas roster is legible
# in reference/StageAreaIntro.png (chat line + three name tags beside the lime
# plot in MainHub-StageArea.png); the rest are genuinely not in the references.
# --------------------------------------------------------------------------
TEAMS = [
    dict(key="red", dye="red", name="Red Rabbits", short="RED",
         players=[None, None, None, None]),
    dict(key="orange", dye="orange", name="Orange Ocelots", short="ORANGE",
         players=[None, None, None, None]),
    dict(key="yellow", dye="yellow", name="Yellow Yaks", short="YELLOW",
         players=[None, None, None, None]),
    dict(key="lime", dye="lime", name="Lime Llamas", short="LIME",
         players=["Chocolate_Orange", "Kaos21la_Faniel49", "HeerTimothy", None]),
    dict(key="green", dye="green", name="Green Geckos", short="GREEN",
         players=[None, None, None, None]),
    dict(key="cyan", dye="cyan", name="Cyan Coyotes", short="CYAN",
         players=[None, None, None, None]),
    dict(key="aqua", dye="light_blue", name="Aqua Axolotls", short="AQUA",
         players=[None, None, None, None]),
    dict(key="blue", dye="blue", name="Blue Bats", short="BLUE",
         players=[None, None, None, None]),
    dict(key="purple", dye="purple", name="Purple Pandas", short="PURPLE",
         players=[None, None, None, None]),
    dict(key="pink", dye="pink", name="Pink Parrots", short="PINK",
         players=[None, None, None, None]),
]

SECTIONS = [
    ("00_columns", "the ten team columns, heads and nameplates"),
]
for _i, _t in enumerate(TEAMS):
    SECTIONS.append((f"{10 + _i:02d}_plot_{_t['key']}", f"{_t['name']} stage plot"))


# --------------------------------------------------------------------------
# Layout
# --------------------------------------------------------------------------
def plot_side(i: int) -> str:
    """'W' for teams 1-5, 'E' for teams 6-10."""
    return "W" if i < 5 else "E"


def plot_zc(i: int) -> int:
    """Centre z of team i's plot.

    West row runs south->north (red at the hub, green far north), east row runs
    north->south (cyan far north, pink at the hub), so the walk meets the teams
    in the order red .. pink and the two members of a pair are complementary
    colours 180 deg apart in the roster.
    """
    if i < 5:
        return ZC0 - PITCH * i
    return ZC0 - PITCH * (9 - i)


def plot_anchor(i: int):
    """Back-wall x cell (v=0) for team i's plot."""
    return WEST_BACK_X if plot_side(i) == "W" else EAST_BACK_X


def pxy(anchor_x, zc, side, u, v):
    """Plot-local (u, v) -> world (x, z).

    u 0..PLOT_W-1 runs along the avenue, v 0..PLOT_D-1 runs from the back wall
    (v=0) out to the front step (v=11).
    """
    x = anchor_x + v if side == "W" else anchor_x - v
    z = zc - (PLOT_W // 2) + u
    return x, z


# --------------------------------------------------------------------------
# Block palette
# --------------------------------------------------------------------------
def dye_block(dye: str, kind: str) -> str:
    return f"minecraft:{dye}_{kind}"


def concrete(t):
    return dye_block(t["dye"], "concrete")


def powder(t):
    return dye_block(t["dye"], "concrete_powder")


def carpet(t):
    return dye_block(t["dye"], "carpet")


SKIRT = "minecraft:stone_bricks"
STEP = "minecraft:smooth_quartz"
PANEL = "minecraft:light_gray_concrete"
OUTLINE = None                             # per-team: the team's terracotta tone
PLINTH = "minecraft:smooth_stone"          # under a plot statue / column pedestal
POST = "minecraft:oak_fence"               # pedestal post (thin, as in the refs)


def head(player) -> str:
    """A player head.  Unknown roster slot -> bare default head (placeholder)."""
    if player is None:
        return "minecraft:player_head"
    return f'minecraft:player_head{{profile:{{name:"{player}"}}}}'


def sign(facing: str, lines) -> str:
    """Wall sign, text on line 2 (the visual centre of the four lines)."""
    body = [""] + list(lines)
    pad = body + [""] * (4 - len(body))
    msgs = ",".join(
        "'{\"text\":\"" + str(t).replace('"', "\\\"") + "\"}'" for t in pad
    )
    return (
        f'minecraft:oak_wall_sign[facing={facing},waterlogged=false]'
        f'{{front_text:{{color:"black",messages:[{msgs}]}},is_waxed:1b}}'
    )


# --------------------------------------------------------------------------
# Document: (x,y,z) -> block id, plus the section each block belongs to
# --------------------------------------------------------------------------
class Doc:
    def __init__(self):
        self.b = {}
        self.sec = {}

    def put(self, x, y, z, block, sec):
        if y < Y_MIN:
            raise ValueError(f"block below y{Y_MIN}: {(x, y, z)}")
        self.b[(x, y, z)] = block
        self.sec[(x, y, z)] = sec

    def box(self, x0, y0, z0, x1, y1, z1, block, sec):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.put(x, y, z, block, sec)

    def count(self, sec=None):
        if sec is None:
            return len(self.b)
        return sum(1 for k in self.b if self.sec[k] == sec)


# ==========================================================================
# 1. Team column  (3 x 3, seven courses, four head pedestals on top)
# ==========================================================================
def build_column(doc, t, cx, cz, sec):
    """cx, cz = centre of the 3x3 column footprint."""
    x0, x1 = cx - 1, cx + 1
    z0, z1 = cz - 1, cz + 1

    # footing: a 3x3 smooth-stone pad, so the column reads as a monument of its
    # own against the team's plot rather than a fragment of the same colour
    doc.box(x0, Y_COL0, z0, x1, Y_COL0, z1, PLINTH, sec)

    # shaft: seven courses, bottom six in concrete, top course in concrete
    # powder so the crown reads as a lighter cap (as the reference columns do)
    y0 = Y_COL0 + 1
    doc.box(x0, y0, z0, x1, y0 + COL_H - 2, z1, concrete(t), sec)
    doc.box(x0, y0 + COL_H - 1, z0, x1, y0 + COL_H - 1, z1, powder(t), sec)

    # four pedestals, one per top corner, ordered highest first and descending:
    # SW (tallest) -> SE -> NE -> NW (head sits straight on the plinth).
    # Reading the front pair left to right you get 4-3-2-1 head heights.
    y_top = y0 + COL_H                # first free course above the shaft (129)
    corners = [  # (dx, dz, post length)
        (-1, +1, 3),   # slot 1 - south-west, highest
        (+1, +1, 2),   # slot 2 - south-east
        (+1, -1, 1),   # slot 3 - north-east
        (-1, -1, 0),   # slot 4 - north-west, lowest
    ]
    for slot, (dx, dz, posts) in enumerate(corners):
        bx, bz = cx + dx, cz + dz
        doc.put(bx, y_top, bz, PLINTH, sec)
        for k in range(posts):
            doc.put(bx, y_top + 1 + k, bz, POST, sec)
        doc.put(bx, y_top + 1 + posts, bz,
                head(t["players"][slot]), sec)

    # nameplate on the south face, at eye height, facing the approach
    doc.put(cx, 125, z1 + 1, sign("south", [t["short"]]), sec)


# ==========================================================================
# 2. Stage Customization plot
# ==========================================================================
def build_plot(doc, t, i, sec):
    """13 wide (along the avenue) x 12 deep (away from it)."""
    side = plot_side(i)
    zc = plot_zc(i)
    anchor = plot_anchor(i)

    def P(u, v):
        return pxy(anchor, zc, side, u, v)

    facing = "east" if side == "W" else "west"

    for u in range(PLOT_W):
        for v in range(PLOT_D):
            x, z = P(u, v)
            if v == PLOT_D - 1:
                # front step: two courses of quartz up to the deck
                doc.put(x, 121, z, STEP, sec)
                doc.put(x, 122, z, STEP, sec)
                continue
            # mass: two courses of stone bricks, then the deck course
            doc.put(x, 121, z, SKIRT, sec)
            doc.put(x, 122, z, SKIRT, sec)
            doc.put(x, Y_DECK, z, concrete(t), sec)

    # carpet inlay: a team-colour field with a thin dark outline at its rim, as
    # the reference plots show (colour right up to the edge, one dark line).
    # The line is the team's own terracotta - a darker shade of the same hue -
    # rather than black, which is what the reference plots use.
    line_block = dye_block(t["dye"], "terracotta")
    for u in range(1, PLOT_W - 1):
        for v in range(2, PLOT_D - 1):
            x, z = P(u, v)
            rim = (u in (1, PLOT_W - 2) or v in (2, PLOT_D - 2))
            doc.put(x, Y_DECK + 1, z, line_block if rim else carpet(t), sec)

    # ---- back wall, v = 0..1, six courses, recessed grey panel ----------
    for v in (0, 1):
        for u in range(PLOT_W):
            x, z = P(u, v)
            for y in range(Y_WALL0, Y_WALL0 + WALL_H):
                in_panel = 2 <= u <= PLOT_W - 3 and Y_WALL0 + 1 <= y <= Y_WALL0 + WALL_H - 2
                if v == 1 and in_panel:
                    continue                     # opening in the front layer
                if v == 0 and in_panel:
                    doc.put(x, y, z, PANEL, sec)      # recessed panel
                    continue
                top = y == Y_WALL0 + WALL_H - 1
                doc.put(x, y, z, powder(t) if top else concrete(t), sec)

    # ---- four player-head statues on the deck, in front of the panel ----
    # local u values 2, 5, 8, 11 -> evenly spaced, centred on the plot;
    # head at y=128, i.e. level with the wall cap, so the roster reads against
    # the sky like the pedestal heads in the reference screenshots.
    for slot, u in enumerate((2, 5, 8, 11)):
        x, z = P(u, 4)
        doc.put(x, Y_DECK + 1, z, PLINTH, sec)
        for k in range(3):
            doc.put(x, Y_DECK + 2 + k, z, POST, sec)
        doc.put(x, Y_DECK + 5, z, head(t["players"][slot]), sec)

    # ---- team name plate on the front step face -------------------------
    x, z = P(PLOT_W // 2, PLOT_D - 1)
    doc.put(x, Y_DECK, z, sign(facing, [t["name"]]), sec)


# ==========================================================================
# 3. Build
# ==========================================================================
def build():
    doc = Doc()
    for i, t in enumerate(TEAMS):
        side = plot_side(i)
        cz = plot_zc(i)
        cx = (COL_WEST_X + 1) if side == "W" else (COL_EAST_X + 1)   # centre
        build_column(doc, t, cx, cz, "00_columns")
        build_plot(doc, t, i, f"{10 + i:02d}_plot_{t['key']}")
    return doc


# ==========================================================================
# 4. Emit commands / csv / datapack
# ==========================================================================
def rle_commands(cells):
    """cells: {(x,y,z): block} -> setblock/fill commands, x-runs merged."""
    by_row = {}
    for (x, y, z), b in cells.items():
        by_row.setdefault((y, z), []).append((x, b))
    out = []
    for (y, z), row in sorted(by_row.items()):
        row.sort()
        i = 0
        while i < len(row):
            x0, b = row[i]
            j = i
            # block entities (signs, named heads) must go one at a time
            nbt = "{" in b
            while (not nbt and j + 1 < len(row) and row[j + 1][0] == row[j][0] + 1
                   and row[j + 1][1] == b):
                j += 1
            x1 = row[j][0]
            if x1 > x0:
                out.append(f"fill {x0} {y} {z} {x1} {y} {z} {b}")
            else:
                out.append(f"setblock {x0} {y} {z} {b}")
            i = j + 1
    return out


def emit(doc):
    os.makedirs(FN_DIR, exist_ok=True)
    os.makedirs(DP_DIR, exist_ok=True)

    groups = {}
    for k, blk in doc.b.items():
        groups.setdefault(doc.sec[k], {})[k] = blk

    order = [s for s, _ in SECTIONS]
    for key in sorted(groups):
        if key not in order:
            order.append(key)

    files = []
    for key in order:
        cells = groups.get(key)
        if not cells:
            continue
        cmds = rle_commands(cells)
        chunk = 1
        for start in range(0, len(cmds), CHUNK_CMDS):
            part = cmds[start:start + CHUNK_CMDS]
            name = f"{key}_{chunk:02d}" if len(cmds) > CHUNK_CMDS else key
            body = [f"# V4-C {dict(SECTIONS).get(key, key)} - part {chunk} "
                    f"({len(part)} commands, {len(cells):,} blocks)"]
            body.extend(part)
            text = "\n".join(body) + "\n"
            for d in (FN_DIR, DP_DIR):
                with open(os.path.join(d, name + ".mcfunction"), "w", encoding="utf-8") as fh:
                    fh.write(text)
            files.append((name, len(part), len(cells)))
            chunk += 1

    load = ["# V4-C team section - 10 team columns + 10 Stage Customization plots",
            "# run:  function mcchub:v4c/load",
            "# y >= 121 only; nothing inside the dome radius 30."]
    for name, _, _ in files:
        load.append(f"function {NS}:v4c/{name}")
    for d in (FN_DIR, DP_DIR):
        with open(os.path.join(d, "load.mcfunction"), "w", encoding="utf-8") as fh:
            fh.write("\n".join(load) + "\n")

    with open(os.path.join(HERE, "datapack", "pack.mcmeta"), "w", encoding="utf-8") as fh:
        fh.write('{\n  "pack": {\n    "pack_format": 48,\n'
                 '    "description": "MCC festive hub replica - V4-C team columns and stage plots"\n'
                 '  }\n}\n')

    with open(os.path.join(HERE, "blocks.csv"), "w", encoding="utf-8", newline="") as fh:
        wr = csv.writer(fh, lineterminator="\n")
        wr.writerow(["x", "y", "z", "block"])
        for (x, y, z) in sorted(doc.b, key=lambda k: (k[1], k[2], k[0])):
            wr.writerow([x, y, z, doc.b[(x, y, z)]])

    return files


# ==========================================================================
# 5. Preview dump (build + preview-only ground context)
# ==========================================================================
def context_block(x, z):
    """Preview-only stand-in for V4-A's island surface + avenue paving."""
    if abs(x) <= PATH_HALF and -114 <= z <= -33:
        return "minecraft:dirt_path"
    return "minecraft:grass_block"


def preview_id(b: str) -> str:
    """Block id only for the dump: the MCMDUMP line is space separated, so NBT
    (and therefore team names inside sign text) must not go into the preview."""
    return b.split("{", 1)[0]


def write_preview(doc, name="preview.mcmdump.gz", region=None):
    x0, z0, y0, x1, z1, y1 = region or RG
    blocks = {}
    for (x, y, z), b in doc.b.items():
        if x0 <= x <= x1 and y0 <= y <= y1 and z0 <= z <= z1:
            blocks[(x, y, z)] = preview_id(b)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            blocks.setdefault((x, 120, z), context_block(x, z))
    path = os.path.join(HERE, name)
    opener = gzip.open if name.endswith(".gz") else open
    with opener(path, "wt", encoding="utf-8") as fh:
        fh.write(f"MCMDUMP 1 {x1 - x0 + 1} {y1 - y0 + 1} {z1 - z0 + 1}\n")
        for (x, y, z) in sorted(blocks, key=lambda k: (k[1], k[2], k[0])):
            fh.write(f"{x - x0} {y - y0} {z - z0} {blocks[(x, y, z)]}\n")
    return path


# ==========================================================================
# 6. Checks
# ==========================================================================
def checks(doc):
    rep = {}
    rep["total_blocks"] = len(doc.b)

    # --- sections
    for sec, _ in SECTIONS:
        rep[f"blocks_{sec}"] = doc.count(sec)

    # --- y bounds / footprint
    rep["below_y121"] = sum(1 for (x, y, z) in doc.b if y < Y_MIN)
    rep["max_y"] = max(y for (_, y, _) in doc.b)
    xs = [x for (x, _, _) in doc.b]
    zs = [z for (_, _, z) in doc.b]
    rep["bbox_x"] = (min(xs), max(xs))
    rep["bbox_z"] = (min(zs), max(zs))
    rep["outside_build_area"] = sum(
        1 for (x, y, z) in doc.b if not (-150 <= x <= 150 and -150 <= z <= 150)
    )
    rep["inside_dome_r30"] = sum(
        1 for (x, y, z) in doc.b if (x * x + z * z) ** 0.5 < 30
    )

    # --- ten columns, three by three, seven courses, four heads
    cols = []
    for i, t in enumerate(TEAMS):
        side = plot_side(i)
        cz = plot_zc(i)
        cx = (COL_WEST_X + 1) if side == "W" else (COL_EAST_X + 1)
        shaft = sum(
            1 for dx in (-1, 0, 1) for dz in (-1, 0, 1)
            for y in range(Y_COL0 + 1, Y_COL0 + 1 + COL_H)
            if doc.b.get((cx + dx, y, cz + dz), "") in (concrete(t), powder(t))
        )
        pad = sum(
            1 for dx in (-1, 0, 1) for dz in (-1, 0, 1)
            if doc.b.get((cx + dx, Y_COL0, cz + dz)) == PLINTH
        )
        heads = sum(
            1 for dx in (-1, 0, 1) for dz in (-1, 0, 1)
            for y in range(Y_COL0 + 1 + COL_H, Y_COL0 + 1 + COL_H + 6)
            if doc.b.get((cx + dx, y, cz + dz), "").startswith("minecraft:player_head")
        )
        cols.append(dict(team=t["key"], cx=cx, cz=cz, side=side,
                         shaft_blocks=shaft, pad_blocks=pad, heads=heads))
    rep["columns"] = cols
    rep["columns_with_full_shaft"] = sum(
        1 for c in cols if c["shaft_blocks"] == 3 * 3 * COL_H and c["pad_blocks"] == 9)
    rep["columns_with_4_heads"] = sum(1 for c in cols if c["heads"] == 4)

    # --- ten plots, four statues each
    plots = []
    for i, t in enumerate(TEAMS):
        side = plot_side(i)
        zc = plot_zc(i)
        anchor = plot_anchor(i)
        heads = 0
        for u in (2, 5, 8, 11):
            x, z = pxy(anchor, zc, side, u, 4)
            for y in range(Y_DECK, Y_DECK + 6):
                if doc.b.get((x, y, z), "").startswith("minecraft:player_head"):
                    heads += 1
        deck = sum(
            1 for u in range(PLOT_W) for v in range(PLOT_D - 1)
            if (lambda p: doc.b.get((p[0], Y_DECK, p[1]), "") == concrete(t))(
                pxy(anchor, zc, side, u, v))
        )
        plots.append(dict(team=t["key"], side=side, zc=zc, deck_cells=deck, heads=heads))
    rep["plots"] = plots
    rep["plots_with_4_heads"] = sum(1 for p in plots if p["heads"] == 4)
    rep["plots_with_deck"] = sum(1 for p in plots if p["deck_cells"] == PLOT_W * (PLOT_D - 1))

    # --- heads: named vs placeholder
    named = sum(1 for b in doc.b.values()
                if b.startswith("minecraft:player_head{profile"))
    bare = sum(1 for b in doc.b.values() if b == "minecraft:player_head")
    rep["heads_named"] = named
    rep["heads_placeholder"] = bare
    rep["signs"] = sum(1 for b in doc.b.values() if "oak_wall_sign" in b)

    # --- roster order (red first, descending pedestal heights)
    rep["team_order"] = [t["key"] for t in TEAMS]
    rep["walk_order"] = [
        f"{t['key']}@{plot_side(i)}{plot_zc(i)}" for i, t in enumerate(TEAMS)
    ]

    ok = (
        rep["below_y121"] == 0
        and rep["inside_dome_r30"] == 0
        and rep["outside_build_area"] == 0
        and rep["columns_with_full_shaft"] == 10
        and rep["columns_with_4_heads"] == 10
        and rep["plots_with_4_heads"] == 10
        and rep["plots_with_deck"] == 10
        and rep["signs"] == 20
    )
    rep["PASS"] = ok
    return rep


# ==========================================================================
# 7. Main
# ==========================================================================
def main():
    doc = build()
    files = emit(doc)

    write_preview(doc)
    # detail dumps: the four southern columns, and the red + pink plots
    write_preview(doc, "detail_columns.mcmdump.gz", (-28, -66, 118, 28, -34, 134))
    write_preview(doc, "detail_plot_red.mcmdump.gz", (-26, -52, 118, -2, -30, 132))
    write_preview(doc, "detail_plot_pink.mcmdump.gz", (2, -52, 118, 26, -30, 132))

    rep = checks(doc)

    lines = ["V4-C  team section - generated report", ""]
    lines.append(f"total blocks                {rep['total_blocks']:,}")
    lines.append(f"bbox x {rep['bbox_x']}  z {rep['bbox_z']}  max y {rep['max_y']}")
    lines.append(f"below y121                  {rep['below_y121']}")
    lines.append(f"inside dome radius 30       {rep['inside_dome_r30']}")
    lines.append(f"outside x/z +-150           {rep['outside_build_area']}")
    lines.append(f"columns 3x3x7               {rep['columns_with_full_shaft']} / 10")
    lines.append(f"columns with 4 heads        {rep['columns_with_4_heads']} / 10")
    lines.append(f"plots with deck             {rep['plots_with_deck']} / 10")
    lines.append(f"plots with 4 statues        {rep['plots_with_4_heads']} / 10")
    lines.append(f"named heads                 {rep['heads_named']}")
    lines.append(f"placeholder heads           {rep['heads_placeholder']}")
    lines.append(f"wall signs                  {rep['signs']}")
    lines.append(f"team order                  {', '.join(rep['team_order'])}")
    lines.append("")
    lines.append("per column:")
    for c in rep["columns"]:
        lines.append(f"  {c['team']:<7} at ({c['cx']},{c['cz']}) side {c['side']} "
                     f"shaft {c['shaft_blocks']:>2}/{3 * 3 * COL_H}  heads {c['heads']}")
    lines.append("")
    lines.append("per plot:")
    for p in rep["plots"]:
        lines.append(f"  {p['team']:<7} zc {p['zc']:>4} side {p['side']} "
                     f"deck {p['deck_cells']:>3}/{PLOT_W * (PLOT_D - 1)}  heads {p['heads']}")
    lines.append("")
    lines.append("function files:")
    for name, ncmd, nblocks in files:
        lines.append(f"  {name:<22} {ncmd:>6} commands  {nblocks:>6} blocks")
    lines.append("")
    lines.append(f"RESULT: {'PASS' if rep['PASS'] else 'FAIL'}")
    text = "\n".join(lines)
    with open(os.path.join(HERE, "report.txt"), "w", encoding="utf-8") as fh:
        fh.write(text + "\n")
    print(text)
    return 0 if rep["PASS"] else 1


if __name__ == "__main__":
    sys.exit(main())
