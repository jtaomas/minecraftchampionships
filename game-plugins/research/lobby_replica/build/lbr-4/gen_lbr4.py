#!/usr/bin/env python3
"""
LBR-4  --  MCC festive-hub replica: game portal ring + hub dressing.

Build site : mcc_lobby_official, C = 5000,120,5000
Owner      : LBR-4 (see ../BRIEF.md, section "Portal ring" and "Ownership")

What this generates
-------------------
* eight game-portal pavilions on the ring R = 72, one every 45 deg,
  18x18 lots sitting exactly on LBR-1's portal pads, 29 blocks tall
  (plinth -> arcade wall -> frieze -> cornice -> name marquee -> crown)
* the hub dressing in the land ring between the lagoon and the rim:
  market stalls, snowy trees, lamp posts with bunting, presents,
  one flying sleigh with reindeer and three hot-air balloons

Output
------
  blocks.csv                     x,y,z,block   (authoritative block list)
  datapack/.../function/lbr4/*   chunked .mcfunction files (~8k commands each)
  preview.mcmdump                plain MCMDUMP (build + preview-only context)
  preview_top.png / preview_iso.png / preview_detail_*.png
  STATUS.md

Conventions
-----------
* Local planes:  f = inward (toward the plaza), t = tangent.
                 n = dot(p - O, f)     (positive toward the plaza)
                 s = dot(p - O, t)
* Nothing is placed below y = 121.  The base (y <= 120) is LBR-1's island.
* Vanilla Java 1.21.1 block ids only.

Run:  python gen_lbr4.py
"""

from __future__ import annotations

import gzip
import math
import os
import random

HERE = os.path.dirname(os.path.abspath(__file__))
FN_DIR = os.path.join(HERE, "functions")
DP_DIR = os.path.join(HERE, "datapack", "data", "mcchub", "function", "lbr4")
NS = "mcchub"

# --------------------------------------------------------------------------
# Site constants
# --------------------------------------------------------------------------
CX, CZ = 5000, 5000          # plaza centre (matching LBR-1)
RING_R = 72.0                # portal ring radius
GROUND = 120                 # top walkable block of the island
Y0 = 121                     # lowest row this section may touch
CHUNK_CMDS = 8000            # commands per .mcfunction

# Portal pad centres -- must match LBR-1's PAD_C exactly.
PAD_C = [
    (5072, 5000), (5051, 5051), (5000, 5072), (4949, 5051),
    (4928, 5000), (4949, 4949), (5000, 4928), (5051, 4949),
]

# LBR-1 site numbers, used for prop placement only (read-only knowledge).
PLAZA_R = 48.5
LAGOON_OUT = 60.5            # water ring: plaza edge .. ~60.5, causeways cross it
PATH_HALF = 3.0              # 7 wide causeways
BELT_IN = 83.5               # outer pond belt

# Preview / check region
RG = (4860, 4860, 116, 5140, 5140, 200)   # x0, z0, y0, x1, z1, y1

SECTIONS = [
    ("00_plinth", "portal plinths and floors"),
    ("01_ace_race", "Ace Race portal"),
    ("02_battle_box", "Battle Box portal"),
    ("03_tgttos", "TGTTOS portal"),
    ("04_sky_battle", "Sky Battle portal"),
    ("05_build_mart", "Build Mart portal"),
    ("06_sands_of_time", "Sands of Time portal"),
    ("07_hole_in_the_wall", "Hole in the Wall portal"),
    ("08_parkour_tag", "Parkour Tag portal"),
    ("10_market_stalls", "market stalls"),
    ("11_trees", "snowy trees"),
    ("12_lamps_bunting", "lamp posts and bunting"),
    ("13_presents", "presents"),
    ("14_sleigh", "Santa sleigh and reindeer"),
    ("15_balloons", "hot-air balloons"),
]


# --------------------------------------------------------------------------
# Tiny helpers
# --------------------------------------------------------------------------
def snow(n: int) -> str:
    return f"minecraft:snow[layers={n}]"


def ang(deg: float, r: float):
    a = math.radians(deg)
    return CX + r * math.cos(a), CZ + r * math.sin(a)


def path_pd(x: float, z: float) -> float:
    """Perpendicular distance to the nearest of the 8 radial causeways (LBR-1 rule)."""
    best = 1e9
    for k in range(8):
        a = math.radians(k * 45.0)
        dx, dz = math.cos(a), math.sin(a)
        px, pz = x - CX, z - CZ
        t = px * dx + pz * dz
        if t < 38.0:
            continue
        best = min(best, abs(-px * dz + pz * dx))
    return best


def pad_gap(x: float, z: float) -> float:
    """Chebyshev distance from (x,z) to the nearest portal pad (19x19 boxes)."""
    best = 1e9
    for px, pz in PAD_C:
        ddx, ddz = abs(x - px), abs(z - pz)
        best = min(best, max(0.0, max(ddx, ddz) - 9.0))
    return best


# --------------------------------------------------------------------------
# 3x5 block font for the marquees
# --------------------------------------------------------------------------
FONT = {
    "A": ("###", "#.#", "###", "#.#", "#.#"),
    "B": ("##.", "#.#", "##.", "#.#", "##."),
    "C": ("###", "#..", "#..", "#..", "###"),
    "D": ("##.", "#.#", "#.#", "#.#", "##."),
    "E": ("###", "#..", "##.", "#..", "###"),
    "F": ("###", "#..", "##.", "#..", "#.."),
    "G": ("###", "#..", "#.#", "#.#", "###"),
    "H": ("#.#", "#.#", "###", "#.#", "#.#"),
    "I": ("#", "#", "#", "#", "#"),
    "J": ("..#", "..#", "..#", "#.#", "###"),
    "K": ("#.#", "#.#", "##.", "#.#", "#.#"),
    "L": ("#..", "#..", "#..", "#..", "###"),
    "M": ("#.#", "###", "###", "#.#", "#.#"),
    "N": ("##.", "#.#", "#.#", "#.#", "#.#"),
    "O": ("###", "#.#", "#.#", "#.#", "###"),
    "P": ("###", "#.#", "###", "#..", "#.."),
    "Q": ("###", "#.#", "###", "..#", "..#"),
    "R": ("###", "#.#", "##.", "#.#", "#.#"),
    "S": ("###", "#..", "###", "..#", "###"),
    "T": ("###", ".#.", ".#.", ".#.", ".#."),
    "U": ("#.#", "#.#", "#.#", "#.#", "###"),
    "V": ("#.#", "#.#", "#.#", "#.#", ".#."),
    "W": ("#...#", "#...#", "#.#.#", "#.#.#", "#.#.#"),
    "X": ("#.#", ".#.", ".#.", ".#.", "#.#"),
    "Y": ("#.#", "#.#", ".#.", ".#.", ".#."),
    "Z": ("###", "..#", ".#.", "#..", "###"),
    " ": (" ", " ", " ", " ", " "),
}


def text_width(line: str) -> int:
    w = 0
    for ch in line:
        w += len(FONT[ch][0]) + 1
    return w - 1


def text_pixels(line: str):
    """Yield (col, row) in font pixels, row 0 = top.  Origin at column 0."""
    x = 0
    for ch in line:
        g = FONT[ch]
        for r in range(5):
            for c, bit in enumerate(g[r]):
                if bit == "#":
                    yield x + c, r
        x += len(g[0]) + 1


# --------------------------------------------------------------------------
# Voxel document
# --------------------------------------------------------------------------
class Doc:
    def __init__(self):
        self.b = {}          # (x,y,z) -> block id
        self.sec = {}        # (x,y,z) -> section key
        self.skipped = 0

    def set(self, x, y, z, blk, sec, force=True):
        if blk is None:
            return False
        if y < Y0:
            raise SystemExit(f"BUG: block below y={Y0} at {x},{y},{z}")
        k = (x, y, z)
        if not force and k in self.b:
            self.skipped += 1
            return False
        self.b[k] = blk
        self.sec[k] = sec
        return True

    def get(self, x, y, z):
        return self.b.get((x, y, z))

    def occupied(self, x, y, z):
        return (x, y, z) in self.b

    def count(self):
        return len(self.b)


# Roof-top icons: 3 rows tall, drawn 2 cells deep on the ridge so every portal
# has its own silhouette on the skyline.  Legend is per game.
ROOF_ICONS = {
    "01_ace_race": (
        ("k|CCCC",
         "k|CWCW",
         "k|CCCC"),
        {"k": "minecraft:dark_oak_fence", "C": "minecraft:cyan_concrete",
         "W": "minecraft:white_concrete"},
    ),
    "02_battle_box": (
        ("I.....I",
         ".I.G.I.",
         "..IGI.."),
        {"I": "minecraft:iron_block", "G": "minecraft:gold_block"},
    ),
    "03_tgttos": (
        ("..RR...",
         "WWWWW..",
         ".WWW.O."),
        {"R": "minecraft:red_concrete", "W": "minecraft:white_concrete",
         "O": "minecraft:orange_concrete"},
    ),
    "04_sky_battle": (
        (".WWWWW.",
         "WWWWWWW",
         "..SSS.."),
        {"W": "minecraft:white_wool", "S": "minecraft:light_blue_concrete"},
    ),
    "05_build_mart": (
        ("GG.GG..",
         "GGGGGG.",
         "K....K."),
        {"G": "minecraft:gold_block", "K": "minecraft:black_concrete"},
    ),
    "06_sands_of_time": (
        ("GGGGG",
         ".SSS.",
         "GGGGG"),
        {"G": "minecraft:gold_block", "S": "minecraft:smooth_sandstone"},
    ),
    "08_parkour_tag": (
        ("....M..",
         "MMMMMM.",
         "..O.O.."),
        {"M": "minecraft:magenta_concrete", "O": "minecraft:orange_concrete"},
    ),
}


def build_roof_icon(doc, port, y0):
    spec = ROOF_ICONS.get(port.g["key"])
    if not spec:
        return
    rows, legend = spec
    w = len(rows[0])
    nf = 0.0                                   # centred on the lot
    for r, row in enumerate(rows):
        y = y0 + (len(rows) - 1 - r)
        for c, ch in enumerate(row):
            blk = legend.get(ch)
            if not blk:
                continue
            s0 = -w / 2.0 + c
            port.rect(doc, -1.0, 1.0, s0, s0 + 1.0, y, y, blk)
# --------------------------------------------------------------------------
# Game portal specification
# --------------------------------------------------------------------------
# frieze / wall patterns are small functions of (s, y) so both the flat and the
# 45 deg facades get the same graphic.
GAMES = [
    dict(
        key="01_ace_race", title="ACE RACE", bearing=0.0, lines=("ACE", "RACE"),
        board_w=25, roof_kind="checker",
        wall="minecraft:cyan_concrete", wall2="minecraft:white_concrete",
        pilaster="minecraft:black_concrete", accent="minecraft:cyan_concrete",
        cornice="minecraft:white_concrete", crown="minecraft:cyan_concrete",
        roof=("minecraft:cyan_concrete", "minecraft:white_concrete"),
        letter="minecraft:white_concrete", board="minecraft:black_concrete",
        floor=("minecraft:black_concrete", "minecraft:white_concrete"),
        glass="minecraft:light_blue_stained_glass", light="minecraft:sea_lantern",
        carpet="minecraft:cyan_carpet", frieze="checker",
    ),
    dict(
        key="02_battle_box", title="BATTLE BOX", bearing=45.0, lines=("BATTLE", "BOX"),
        board_w=25, roof_kind="split", wsplit=True,
        wall="minecraft:white_concrete", wall2="minecraft:light_gray_concrete",
        pilaster="minecraft:iron_block", accent="minecraft:red_concrete",
        cornice="minecraft:white_concrete", crown="minecraft:red_concrete",
        roof=("minecraft:red_wool", "minecraft:blue_wool"),
        letter="minecraft:white_concrete", board="minecraft:black_concrete",
        floor=("minecraft:red_concrete", "minecraft:blue_concrete"),
        glass="minecraft:white_stained_glass", light="minecraft:lantern",
        carpet="minecraft:red_carpet", frieze="duel",
    ),
    dict(
        key="03_tgttos", roof_kind="stripe", title="TGTTOS", bearing=90.0, lines=("TGTTOS", "CHICKEN"),
        board_w=29,
        wall="minecraft:yellow_concrete", wall2="minecraft:black_concrete",
        pilaster="minecraft:black_concrete", accent="minecraft:yellow_concrete",
        cornice="minecraft:white_concrete", crown="minecraft:yellow_concrete",
        roof=("minecraft:yellow_concrete", "minecraft:black_concrete"),
        letter="minecraft:white_concrete", board="minecraft:black_concrete",
        floor=("minecraft:yellow_concrete", "minecraft:black_concrete"),
        glass="minecraft:yellow_stained_glass", light="minecraft:shroomlight",
        carpet="minecraft:yellow_carpet", frieze="chevron",
    ),
    dict(
        key="04_sky_battle", roof_kind="band", title="SKY BATTLE", bearing=135.0, lines=("SKY", "BATTLE"),
        board_w=25,
        wall="minecraft:light_blue_concrete", wall2="minecraft:white_concrete",
        pilaster="minecraft:white_concrete", accent="minecraft:light_blue_concrete",
        cornice="minecraft:white_concrete", crown="minecraft:light_blue_concrete",
        roof=("minecraft:white_wool", "minecraft:light_blue_concrete"),
        letter="minecraft:white_concrete", board="minecraft:black_concrete",
        floor=("minecraft:light_blue_concrete", "minecraft:white_concrete"),
        glass="minecraft:light_blue_stained_glass", light="minecraft:sea_lantern",
        carpet="minecraft:light_blue_carpet", frieze="cloud",
    ),
    dict(
        key="05_build_mart", roof_kind="stripe", title="BUILD MART", bearing=180.0, lines=("BUILD", "MART"),
        board_w=25,
        wall="minecraft:green_concrete", wall2="minecraft:gold_block",
        pilaster="minecraft:gold_block", accent="minecraft:green_concrete",
        cornice="minecraft:white_concrete", crown="minecraft:gold_block",
        roof=("minecraft:green_concrete", "minecraft:gold_block"),
        letter="minecraft:white_concrete", board="minecraft:black_concrete",
        floor=("minecraft:smooth_stone", "minecraft:gold_block"),
        glass="minecraft:lime_stained_glass", light="minecraft:lantern",
        carpet="minecraft:green_carpet", frieze="mart",
    ),
    dict(
        key="06_sands_of_time", roof_kind="stripe", title="SANDS OF TIME", bearing=225.0, lines=("SANDS", "OF TIME"),
        board_w=25,
        wall="minecraft:smooth_sandstone", wall2="minecraft:cyan_concrete",
        pilaster="minecraft:gold_block", accent="minecraft:cyan_concrete",
        cornice="minecraft:smooth_sandstone", crown="minecraft:cyan_concrete",
        roof=("minecraft:cut_sandstone", "minecraft:cyan_concrete"),
        letter="minecraft:white_concrete", board="minecraft:black_concrete",
        floor=("minecraft:smooth_sandstone", "minecraft:cyan_terracotta"),
        glass="minecraft:cyan_stained_glass", light="minecraft:shroomlight",
        carpet="minecraft:cyan_carpet", frieze="sand",
    ),
    dict(
        key="07_hole_in_the_wall", roof_kind="stripe", title="HOLE IN THE WALL", bearing=270.0,
        lines=("HOLE", "IN THE", "WALL"), board_w=21,
        wall="minecraft:lime_concrete", wall2="minecraft:white_concrete",
        pilaster="minecraft:white_concrete", accent="minecraft:lime_concrete",
        cornice="minecraft:white_concrete", crown="minecraft:lime_concrete",
        roof=("minecraft:lime_concrete", "minecraft:white_concrete"),
        letter="minecraft:white_concrete", board="minecraft:black_concrete",
        floor=("minecraft:lime_concrete", "minecraft:white_concrete"),
        glass="minecraft:lime_stained_glass", light="minecraft:sea_lantern",
        carpet="minecraft:lime_carpet", frieze="grid", three=True,
    ),
    dict(
        key="08_parkour_tag", roof_kind="stripe", title="PARKOUR TAG", bearing=315.0, lines=("PARKOUR", "TAG"),
        board_w=29,
        wall="minecraft:orange_concrete", wall2="minecraft:magenta_concrete",
        pilaster="minecraft:magenta_concrete", accent="minecraft:orange_concrete",
        cornice="minecraft:white_concrete", crown="minecraft:orange_concrete",
        roof=("minecraft:orange_concrete", "minecraft:magenta_concrete"),
        letter="minecraft:white_concrete", board="minecraft:black_concrete",
        floor=("minecraft:orange_concrete", "minecraft:magenta_concrete"),
        glass="minecraft:orange_stained_glass", light="minecraft:shroomlight",
        carpet="minecraft:orange_carpet", frieze="speed",
    ),
]

# vertical slots
Y_PLINTH = 121
Y_WALL0 = 122
Y_ARCH1 = 127          # 6 tall opening (122..127), narrowed at the top
Y_FRZ0, Y_FRZ1 = 128, 129
Y_CANOPY = 130
Y_SGN0 = 132
Y_MAST1 = 150


# --------------------------------------------------------------------------
# Facade graphic patterns
# --------------------------------------------------------------------------
def frieze(g, s: float, y: int) -> str:
    """Colour band above the door: the game's graphic signature."""
    kind = g["frieze"]
    si = int(math.floor(s))
    yy = y - Y_FRZ0
    if kind == "checker":
        return g["wall2"] if ((si + y) & 1) else "minecraft:white_concrete"
    if kind == "duel":
        if yy == 1:
            return "minecraft:white_concrete"
        return "minecraft:blue_concrete" if s < 0 else "minecraft:red_concrete"
    if kind == "chevron":
        return g["wall2"] if ((si - yy) % 4 in (0, 1)) else g["wall"]
    if kind == "cloud":
        return "minecraft:white_wool" if (abs(si) % 5 < 3) else g["wall"]
    if kind == "mart":
        return g["wall2"] if (si % 4 == 0 or yy == 1) else g["wall"]
    if kind == "sand":
        return g["wall2"] if ((si // 2 + yy) % 2 == 0) else g["wall"]
    if kind == "grid":
        return g["wall2"] if (si % 3 == 0 or yy == 1) else g["wall"]
    if kind == "speed":
        return g["wall2"] if ((abs(si) % 6) in (0, 1) or yy == 2) else g["wall"]
    return g["wall"]


def facade_block(g, s: float, y: int):
    """Block on the front face at tangent coordinate s, height y.
    Returns None for the door opening."""
    a = abs(s)
    if y <= Y_ARCH1:
        if y == Y_ARCH1:
            return None if a < 2.0 else g["accent"]
        if a < 3.0:
            return None                      # the arch
        if a < 3.9:
            return g["pilaster"]             # door jambs
        if a < 6.0 and 124 <= y <= 126:
            return g["glass"]                # side lights
        if a < 6.0:
            return g["wall2"]
        if a < 7.0:
            return g["pilaster"]
        return g["wall"]
    if Y_FRZ0 <= y <= Y_FRZ1:
        return frieze(g, s, y)
    return g["wall"]


def roof_block(g, s: float, k: int) -> str:
    """Roof colour: each game gets its own signature pattern."""
    kind = g.get("roof_kind", "band")
    si = int(math.floor(s))
    a, b = g["roof"]
    if kind == "checker":
        return a if ((si + k) & 1) else b
    if kind == "split":
        return a if s < 0 else b
    if kind == "stripe":
        return a if ((si // 2 + k) % 4 < 2) else b
    return a if (k % 2 == 0) else b


# --------------------------------------------------------------------------
# Portal builder
# --------------------------------------------------------------------------
class Portal:
    def __init__(self, g, pad):
        self.g = g
        self.sec = g["key"]
        cx, cz = pad
        b = math.radians(g["bearing"])
        self.ox, self.oz = math.cos(b), math.sin(b)      # outward radial (lot bias)

        # Facade normal: the cardinal direction nearest the inward radial.
        # A 45 deg plane cannot carry 3x5 block text without aliasing, so the
        # four diagonal portals use the next cardinal face round (pinwheel),
        # which keeps every sign axis-aligned and crisp.
        fb = g["bearing"] + 180.0
        if abs(self.ox) > 0.5 and abs(self.oz) > 0.5:
            fb += 45.0
        fb = math.radians(fb % 360.0)
        self.fx = round(math.cos(fb))
        self.fz = round(math.sin(fb))
        # tangent: +s is the viewer's screen-right when facing the facade
        self.tx, self.tz = self.fz, -self.fx

        # 18x18 lot, biased one cell outward so LBR-1's pad rim stays visible
        self.x0 = cx - 9 + (1 if self.ox > 0.5 else 0)
        self.x1 = cx + 8 + (1 if self.ox > 0.5 else 0)
        self.z0 = cz - 9 + (1 if self.oz > 0.5 else 0)
        self.z1 = cz + 8 + (1 if self.oz > 0.5 else 0)
        self.cxm = (self.x0 + self.x1 + 1) / 2.0
        self.czm = (self.z0 + self.z1 + 1) / 2.0

        self.diag = False
        self.n_front = 8.0            # 1 cell of forecourt inside the lot
        self.face_w = 18.0

        # vertical levels (2-line signs are 28 tall, the 3-line HITW sign 30)
        self.three = bool(g.get("three"))
        self.sgn1 = 148 if self.three else 142            # last sign row
        self.cor0 = self.sgn1 + 1
        self.cor1 = self.cor0 + 1
        self.roof0 = self.cor1 + 1
        self.roof1 = self.roof0 + 2
        self.top = self.cor1 if self.three else self.roof1 + 1

    # --- local coordinates -------------------------------------------------
    def cs(self, x, z):
        px = x + 0.5 - self.cxm
        pz = z + 0.5 - self.czm
        return (px * self.fx + pz * self.fz, px * self.tx + pz * self.tz)

    def in_plan(self, x, z, inset=0.0):
        if x < self.x0 + inset or x > self.x1 - inset:
            return False
        if z < self.z0 + inset or z > self.z1 - inset:
            return False
        n, _ = self.cs(x, z)
        return n <= self.n_front - inset

    def plan(self, inset=0.0):
        out = []
        for x in range(self.x0 - 2, self.x1 + 3):
            for z in range(self.z0 - 2, self.z1 + 3):
                if self.in_plan(x, z, inset):
                    out.append((x, z))
        return out

    # --- plane primitives --------------------------------------------------
    def rect(self, doc, n0, n1, s0, s1, y0, y1, blk, mask=None):
        """Fill the plane rectangle [n0,n1) x [s0,s1) x [y0,y1].

        Strict half-open comparisons: every facade plane here is axis aligned,
        so cell centres sit on half-integers and no cell lands on a bound.
        (An epsilon on the s bound would make each glyph pixel two cells wide.)
        """
        pts = []
        for n in (n0, n1):
            for s in (s0, s1):
                pts.append((self.cxm + n * self.fx + s * self.tx,
                            self.czm + n * self.fz + s * self.tz))
        xs = [p[0] for p in pts]
        zs = [p[1] for p in pts]
        for x in range(int(math.floor(min(xs))) - 1, int(math.ceil(max(xs))) + 2):
            for z in range(int(math.floor(min(zs))) - 1, int(math.ceil(max(zs))) + 2):
                n, s = self.cs(x, z)
                if not (n0 <= n < n1 and s0 <= s < s1):
                    continue
                for y in range(y0, y1 + 1):
                    b = blk if mask is None else mask(n, s, y)
                    if b is not None:
                        doc.set(x, y, z, b, self.sec)

    # --- the building ------------------------------------------------------
    def build(self, doc):
        self.plinth(doc)
        self.walls(doc)
        self.canopy(doc)
        self.interior(doc)
        self.roof(doc)
        self.marquee(doc)
        if not self.three:
            build_roof_icon(doc, self, self.roof1 + 1)

    def plinth(self, doc):
        """Paved podium, full 18x18 with cut corners; the walkable floor."""
        g = self.g
        cx, cz = self.cxm, self.czm
        for x in range(self.x0, self.x1 + 1):
            for z in range(self.z0, self.z1 + 1):
                if abs(x + 0.5 - cx) + abs(z + 0.5 - cz) > 15.6:
                    continue                       # 45 deg corner cut
                n, s = self.cs(x, z)
                edge = (n > self.n_front - 1.2 or x in (self.x0, self.x1)
                        or z in (self.z0, self.z1)
                        or abs(x + 0.5 - cx) + abs(z + 0.5 - cz) > 14.4)
                doc.set(x, Y_PLINTH, z, "minecraft:chiseled_stone_bricks" if edge
                        else "minecraft:stone_bricks", "00_plinth")
        # inlaid threshold under the door
        self.rect(doc, -1.0, self.n_front + 1.6, -3.2, 3.2, Y_PLINTH, Y_PLINTH,
                  g["accent"])
        # inner floor pattern (kept under the room)
        for (x, z) in self.plan(2.6):
            n, s = self.cs(x, z)
            b = g["floor"][(int(math.floor(s)) + int(math.floor(n))) & 1]
            doc.set(x, Y_PLINTH, z, b, "00_plinth")
        # snow drift at the back corners
        for x in range(self.x0, self.x1 + 1):
            for z in range(self.z0, self.z1 + 1):
                n, s = self.cs(x, z)
                if n < -6.5 and abs(s) > 6.0 and (x + z) % 3 != 0:
                    doc.set(x, Y_PLINTH, z, "minecraft:snow_block", "00_plinth")

    def walls(self, doc):
        """Ground-floor room shell (2 cells thick) plus the sign-bearing upper wall."""
        g = self.g
        shell = set(self.plan(0.0)) - set(self.plan(2.0))
        for (x, z) in sorted(shell):
            n, s = self.cs(x, z)
            on_facade = n > self.n_front - 2.35
            for y in range(Y_WALL0, self.sgn1 + 1):
                if y <= Y_FRZ1 and on_facade:
                    b = facade_block(g, s, y)
                else:
                    b = g["wall"] if (y % 5) else g["wall2"]
                    if g.get("wsplit"):
                        b = ("minecraft:blue_concrete" if s < 0
                             else "minecraft:red_concrete") if (y % 5) else "minecraft:white_concrete"
                    # side windows on the ground floor
                    if 124 <= y <= 127 and abs(s) > 7.2 and (
                            -5.5 < n < -3.0 or 1.0 < n < 3.5):
                        b = g["glass"]
                    # upper storey: banded sides, so the back reads as a building
                    if y >= Y_SGN0 and abs(s) > 8.0:
                        b = g["wall2"] if ((y - Y_SGN0) % 4 < 2) else g["wall"]
                        if g.get("wsplit"):
                            b = ("minecraft:blue_concrete" if s < 0
                                 else "minecraft:red_concrete") if ((y - Y_SGN0) % 4 < 2) else "minecraft:white_concrete"
                if b is not None:
                    doc.set(x, y, z, b, self.sec)
        # door jambs continue up through the frieze
        for sgn in (-1, 1):
            self.rect(doc, self.n_front - 2.35, self.n_front,
                      sgn * 2.9, sgn * 3.9, Y_WALL0, Y_FRZ1, g["pilaster"])
        # keystone over the arch
        self.rect(doc, self.n_front - 2.35, self.n_front, -1.4, 1.4,
                  Y_ARCH1 + 1, Y_FRZ1, g["accent"])
        # ground floor ceiling slab = the sign's floor
        for (x, z) in self.plan(0.0):
            doc.set(x, Y_SGN0, z, g["wall2"], self.sec)
        # cornice: proud of the wall and wide enough to cap the sign
        for (x, z) in self.plan(-1.0):
            n, s = self.cs(x, z)
            doc.set(x, self.cor0, z, g["crown"], self.sec)
            doc.set(x, self.cor1, z,
                    g["crown"] if (int(math.floor(s)) % 3) else "minecraft:white_concrete",
                    self.sec)

    def canopy(self, doc):
        """Coloured awning over the door: the roof colour, seen from the plaza."""
        g = self.g
        half = g["board_w"] / 2.0 - 1.4
        nf = self.n_front
        # deck, sloping out over the forecourt
        self.rect(doc, nf - 1.0, nf + 1.9, -half, half, Y_CANOPY, Y_CANOPY, g["crown"])
        # fascia edge and its snow
        self.rect(doc, nf + 1.2, nf + 1.9, -half, half, Y_CANOPY, Y_CANOPY,
                  g["cornice"])
        self.rect(doc, nf + 0.9, nf + 1.9, -half, half, Y_CANOPY + 1, Y_CANOPY + 1,
                  snow(2))
        # lanterns hanging under the front edge
        for sc in (-half + 1.5, -half / 2.0, half / 2.0, half - 1.5):
            self.rect(doc, nf + 1.2, nf + 1.9, sc - 0.5, sc + 0.5,
                      Y_CANOPY - 1, Y_CANOPY - 1, "minecraft:lantern")

    def interior(self, doc):
        g = self.g
        inner = self.plan(2.0)
        if not inner:
            return
        # carpet runner from the door to the back wall
        self.rect(doc, -7.0, self.n_front - 2.2, -1.6, 1.6, Y_WALL0, Y_WALL0, g["carpet"])
        # back-wall portal: glowing panel with a gold frame
        self.rect(doc, -7.2, -6.2, -4.5, 4.5, 123, 129, "minecraft:gold_block")
        self.rect(doc, -6.2, -5.6, -3.4, 3.4, 124, 128, g["glass"])
        self.rect(doc, -5.6, -5.4, -2.4, 2.4, 125, 127, g["light"])
        # ceiling lamps
        for ss, nn in ((-4.5, -1.0), (4.5, -1.0), (0.0, -4.6)):
            self.rect(doc, nn - 0.5, nn + 0.5, ss - 0.5, ss + 0.5, Y_SGN0 - 1,
                      Y_SGN0 - 1, "minecraft:chain")
            self.rect(doc, nn - 0.5, nn + 0.5, ss - 0.5, ss + 0.5, Y_SGN0 - 2,
                      Y_SGN0 - 2, "minecraft:lantern")
        # two counters for scale
        for (nn, ss, blk) in ((3.0, -5.0, "minecraft:barrel"), (3.0, 5.0, "minecraft:chest")):
            self.rect(doc, nn - 0.5, nn + 0.5, ss - 0.5, ss + 0.5, 122, 122, blk)

    def roof(self, doc):
        """Shallow snow-dusted hip roof, kept short so the sign stays dominant."""
        g = self.g
        if self.three:
            # three-line marquee fills the whole upper wall: a low crown instead
            for k, ins in enumerate((0.0, 3.0)):
                y = self.cor0 + k
                edge = set(self.plan(ins + 1.0))
                for (x, z) in self.plan(ins):
                    doc.set(x, y, z, g["crown"] if k == 0 else g["wall"], self.sec)
                    if k == 0 and (x, z) not in edge:
                        doc.set(x, y + 1, z, snow(2), self.sec)
            for (x, z) in self.plan(4.2):
                doc.set(x, self.cor1, z, "minecraft:gold_block", self.sec)
            return
        insets = (0.0, 2.6, 5.2)
        for k, ins in enumerate(insets):
            y = self.roof0 + k
            nxt = set(self.plan(insets[k + 1] if k + 1 < len(insets) else ins + 2.6))
            edge = set(self.plan(ins + 1.0))
            for (x, z) in self.plan(ins):
                n, s = self.cs(x, z)
                doc.set(x, y, z, roof_block(g, s, k), self.sec)
                if (x, z) not in edge and (k == 0 or k == len(insets) - 1):
                    # snow rakes the eave line and the cap, colour in between
                    doc.set(x, y + 1, z, snow(2), self.sec)
        # ridge cap + gold finial
        for (x, z) in self.plan(insets[-1]):
            doc.set(x, self.roof1, z, roof_block(g, self.cs(x, z)[1], 2), self.sec)
        for (x, z) in self.plan(insets[-1] + 2.6):
            doc.set(x, self.roof1 + 1, z, "minecraft:gold_block", self.sec)
        # snow lip along the cornice where the roof does not cover it
        r0 = set(self.plan(0.0))
        for (x, z) in self.plan(-1.0):
            if (x, z) not in r0:
                doc.set(x, self.roof0, z, snow(2), self.sec)

    def marquee(self, doc):
        """Name board: 1 cell plate plus raised letters, framed by pilasters."""
        g = self.g
        bw = g["board_w"]
        half = bw / 2.0
        sgn_top = self.sgn1
        nf = self.n_front
        # plate is two cells thick: a 1-cell band cannot rasterise a 45 deg
        # plane without pinholes, two layers always union into a solid face
        self.rect(doc, nf - 2.0, nf, -half, half, Y_SGN0, sgn_top, g["board"])
        self.rect(doc, nf - 2.0, nf, -half, -half + 1.0, Y_SGN0, sgn_top, g["pilaster"])
        self.rect(doc, nf - 2.0, nf, half - 1.0, half, Y_SGN0, sgn_top, g["pilaster"])
        # text in the outermost cell layer only: keeps strokes one cell thin
        n0, n1 = nf - 0.5, nf
        lines = g["lines"]
        # first line on top: the board is read top-down
        tops = [self.sgn1, self.sgn1 - 6, self.sgn1 - 12][:len(lines)]
        for line, top in zip(lines, tops):
            w = text_width(line)
            left = -w / 2.0
            for c, r in text_pixels(line):
                s0 = left + c
                y = top - r
                self.rect(doc, n0, n1, s0, s0 + 1.0, y, y, g["letter"])
        # cap the board: cornice over its full width
        if not self.three:
            self.rect(doc, nf - 1.5, nf + 0.6, -half, half, self.cor0, self.cor1,
                      g["cornice"])
            self.rect(doc, nf - 1.5, nf + 0.6, -half + 1.0, half - 1.0,
                      self.cor1 + 1, self.cor1 + 1, snow(2))
            # pennant masts on the corners of the cornice
            for sgn in (-1, 1):
                sc = sgn * (half - 1.5)
                for y in range(self.cor1 + 1, self.top + 1):
                    self.rect(doc, nf - 0.5, nf + 0.6, sc - 0.5, sc + 0.5, y, y,
                              "minecraft:dark_oak_fence")
                for i, y in enumerate(range(self.top - 1, self.top + 1)):
                    self.rect(doc, nf + 0.6, nf + 1.6, sc - 3.0, sc - 3.0 + 1.2 + i * 1.4,
                              y, y, g["accent"])
        else:
            self.rect(doc, nf - 1.5, nf + 0.6, -half, half, self.cor0, self.cor1,
                      g["crown"])


# --------------------------------------------------------------------------
# Rotated pixel grid (sleigh, stalls, small props)
# --------------------------------------------------------------------------
class Grid:
    """Integer local grid -> world, rotated by `bearing` (any angle)."""

    def __init__(self, bearing):
        b = math.radians(bearing)
        self.c, self.s = math.cos(b), math.sin(b)
        self.g = {}

    def set(self, u, v, y, blk):
        if blk is not None:
            self.g[(u, v, y)] = blk

    def rows(self, y, u0, u1, v, blk):
        """Horizontal run along u."""
        for u in range(u0, u1 + 1):
            self.set(u, v, y, blk)

    def bake(self, doc, ox, oz, sec):
        if not self.g:
            return
        us = [k[0] for k in self.g]
        vs = [k[1] for k in self.g]
        R = max(abs(min(us)), abs(max(us)), abs(min(vs)), abs(max(vs))) + 2
        for x in range(int(ox) - R, int(ox) + R + 1):
            for z in range(int(oz) - R, int(oz) + R + 1):
                px, pz = x + 0.5 - ox, z + 0.5 - oz
                u = math.floor(px * self.c + pz * self.s)
                v = math.floor(-px * self.s + pz * self.c)
                for y in range(Y0, 200):
                    blk = self.g.get((u, v, y))
                    if blk:
                        doc.set(x, y, z, blk, sec)


# --------------------------------------------------------------------------
# Market stalls
# --------------------------------------------------------------------------
STALL_PALETTES = [
    ("minecraft:red_wool", "minecraft:white_wool", "minecraft:spruce_planks",
     "minecraft:red_concrete"),
    ("minecraft:green_wool", "minecraft:white_wool", "minecraft:dark_oak_planks",
     "minecraft:lime_concrete"),
    ("minecraft:blue_wool", "minecraft:white_wool", "minecraft:spruce_planks",
     "minecraft:light_blue_concrete"),
    ("minecraft:yellow_wool", "minecraft:red_wool", "minecraft:dark_oak_planks",
     "minecraft:orange_concrete"),
]

GOODS = ["minecraft:barrel", "minecraft:cake", "minecraft:pumpkin", "minecraft:melon",
         "minecraft:hay_block", "minecraft:snow_block", "minecraft:bookshelf"]


def build_stall(doc, cx, cz, facing_deg, pal, rng, sec="10_market_stalls"):
    """6 wide x 6 deep market stall: striped awning, counter, goods, lantern."""
    aw1, aw2, wood, accent = pal
    gr = Grid(facing_deg)
    # legs
    for (u, v) in ((-2, -3), (-2, 2), (2, -3), (2, 2)):
        for y in range(Y0, 126):
            gr.set(u, v, y, "minecraft:spruce_log")
    # deck
    for u in range(-2, 3):
        for v in range(-3, 3):
            gr.set(u, v, Y0, wood)
    # counter across the front (u = -2)
    for v in range(-3, 3):
        gr.set(-2, v, 122, wood)
        gr.set(-2, v, 123, accent if (v % 2) else wood)
    # back wall / shelf
    for v in range(-3, 3):
        for y in range(122, 126):
            gr.set(2, v, y, "minecraft:dark_oak_planks")
    for v in range(-3, 3):
        gr.set(1, v, 124, "minecraft:dark_oak_planks" if v % 2 else wood)
    # awning: stripes along v, front edge one lower
    for u in range(-2, 3):
        for v in range(-3, 3):
            y = 126 if u > -2 else 125
            gr.set(u, v, y, aw1 if (v % 2 == 0) else aw2)
    for v in range(-3, 3):
        gr.set(-3, v, 125, aw1 if (v % 2 == 0) else aw2)
        gr.set(-3, v, 124, aw1 if (v % 2 == 1) else aw2)
    # snow dusting on the back half of the awning
    for u in range(0, 3):
        for v in range(-3, 3):
            if (u + v) % 2:
                gr.set(u, v, 127, snow(2))
    # goods on the counter
    for v in (-2, -1, 0, 1):
        if rng.random() < 0.85:
            gr.set(-2, v, 124, rng.choice(GOODS))
    gr.set(0, -2, 125, "minecraft:lantern")
    gr.set(1, 1, 125, "minecraft:lantern")
    # candy cane at the front corner
    for y in range(121, 125):
        gr.set(-2, 2, y, "minecraft:red_concrete" if (y % 2) else "minecraft:white_concrete")
    gr.bake(doc, cx, cz, sec)


# --------------------------------------------------------------------------
# Trees
# --------------------------------------------------------------------------
def build_tree(doc, x, z, h, rng, decorated=False, sec="11_trees"):
    """Snow-dusted spruce, 6..9 blocks tall, conical."""
    if doc.occupied(x, Y0, z) or doc.occupied(x, Y0, z - 1):
        return False
    top = Y0 + h
    for y in range(Y0, top + 1):
        doc.set(x, y, z, "minecraft:spruce_log", sec)
    for i, y in enumerate(range(top, Y0 + 2, -1)):
        r = min(3, 1 + i // 2)
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if abs(dx) + abs(dz) > r + 1:
                    continue
                if dx == 0 and dz == 0 and y > Y0 + 3:
                    continue                     # keep the trunk visible
                doc.set(x + dx, y, z + dz, "minecraft:spruce_leaves", sec)
                edge = abs(dx) + abs(dz) >= r
                if edge and rng.random() < 0.38:
                    doc.set(x + dx, y + 1, z + dz, snow(1), sec)
    doc.set(x, top + 1, z, "minecraft:spruce_leaves", sec)
    doc.set(x, top + 2, z, snow(1), sec)
    if decorated:
        doc.set(x, top + 3, z, "minecraft:gold_block", sec)
        for dx, dz, y in ((1, 0, top - 1), (-1, 1, top - 2), (0, -1, top - 3),
                          (1, 1, top - 4)):
            doc.set(x + dx, y, z + dz, "minecraft:red_concrete", sec)
    return True


# --------------------------------------------------------------------------
# Lamp posts + bunting
# --------------------------------------------------------------------------
def build_lamp(doc, x, z, sec="12_lamps_bunting"):
    if doc.occupied(x, Y0, z):
        return False
    doc.set(x, Y0, z, "minecraft:stone_bricks", sec)
    for y in range(Y0 + 1, Y0 + 5):
        doc.set(x, y, z, "minecraft:red_concrete" if (y % 2) else "minecraft:white_concrete", sec)
    doc.set(x, Y0 + 5, z, "minecraft:gold_block", sec)
    doc.set(x, Y0 + 6, z, "minecraft:lantern", sec)
    doc.set(x, Y0 + 7, z, snow(1), sec)
    return True


def bunting(doc, a, b, base_y, sec="12_lamps_bunting"):
    """A sagging string of flags between two posts, staying above head height."""
    (x0, z0), (x1, z1) = a, b
    n = max(abs(x1 - x0), abs(z1 - z0))
    for i in range(n + 1):
        t = i / max(1, n)
        x = round(x0 + (x1 - x0) * t)
        z = round(z0 + (z1 - z0) * t)
        y = base_y - int(round(1.6 * math.sin(math.pi * t)))
        blk = "minecraft:red_wool" if i % 3 else "minecraft:white_wool"
        if not doc.occupied(x, y, z):
            doc.set(x, y, z, blk, sec)
        if i % 2 == 0 and not doc.occupied(x, y - 1, z):
            doc.set(x, y - 1, z, "minecraft:green_wool" if i % 4 else "minecraft:red_wool", sec)


# --------------------------------------------------------------------------
# Presents
# --------------------------------------------------------------------------
def build_present(doc, x, z, size, rng, sec="13_presents"):
    body = rng.choice(["minecraft:red_concrete", "minecraft:lime_concrete",
                       "minecraft:light_blue_concrete", "minecraft:magenta_concrete",
                       "minecraft:yellow_concrete", "minecraft:green_concrete"])
    rib = rng.choice(["minecraft:white_concrete", "minecraft:gold_block"])
    for dx in range(size):
        for dz in range(size):
            for dy in range(size):
                if doc.occupied(x + dx, Y0 + dy, z + dz):
                    continue
                blk = body
                if dx == size // 2 or dz == size // 2:
                    blk = rib
                doc.set(x + dx, Y0 + dy, z + dz, blk, sec)
    top = Y0 + size
    if not doc.occupied(x, top, z):
        doc.set(x, top, z, rib if size == 1 else "minecraft:gold_block", sec)


def safe_ground(x, z, size=1):
    """True when a size x size footprint stands on LBR-1's dry ground."""
    for dx in (0, size - 1):
        for dz in (0, size - 1):
            px, pz = x + dx, z + dz
            r = math.hypot(px + 0.5 - CX, pz + 0.5 - CZ)
            if PLAZA_R < r < 62.0 and path_pd(px, pz) > PATH_HALF:
                return False
            if r > 84:
                return False
    return True


# --------------------------------------------------------------------------
# Sleigh and reindeer
# --------------------------------------------------------------------------
def build_sleigh(doc, cx, cz, y, heading_deg, sec="14_sleigh"):
    """Santa's sleigh with a two-reindeer team. Axis-aligned heading keeps the
    silhouette crisp at a distance (14 long x 4 wide x 7 tall)."""
    gr = Grid(heading_deg)
    B = y
    RED, WHT, DOK, GLD = ("minecraft:red_concrete", "minecraft:white_concrete",
                          "minecraft:dark_oak_planks", "minecraft:gold_block")
    IRON, BROWN, DARK = ("minecraft:iron_block", "minecraft:brown_concrete",
                         "minecraft:dark_oak_log")
    SKIN, SACK = "minecraft:birch_planks", "minecraft:brown_wool"

    # --- runners ----------------------------------------------------------
    for v in (-1, 1):
        for u in range(1, 6):
            gr.set(u, v, B, IRON)
        gr.set(6, v, B + 1, IRON)          # upturned front of the runner

    # --- hull -------------------------------------------------------------
    for u in range(0, 6):
        for v in (-1, 0, 1):
            gr.set(u, v, B + 1, RED)
    for u in range(1, 6):
        for v in (-1, 1):
            gr.set(u, v, B + 2, RED if (u % 2) else WHT)
        gr.set(u, 0, B + 2, DOK)           # deck
    # back panel and gold rail
    for v in (-1, 0, 1):
        gr.set(0, v, B + 2, RED)
        gr.set(0, v, B + 3, RED)
        gr.set(0, v, B + 4, GLD)
    # curled prow
    gr.set(6, 0, B + 2, GLD)
    gr.set(6, 0, B + 3, GLD)

    # --- Santa ------------------------------------------------------------
    gr.set(2, 0, B + 3, RED)               # body
    gr.set(2, 0, B + 4, WHT)               # beard / belt line
    gr.set(3, 0, B + 4, SKIN)              # face
    gr.set(3, 0, B + 5, RED)               # hat
    gr.set(3, 0, B + 6, WHT)               # bobble
    gr.set(1, 0, B + 3, SACK)              # sack of presents

    # --- reindeer team ----------------------------------------------------
    for v in (-2, 1):
        for u in range(9, 12):
            gr.set(u, v, B + 2, BROWN)     # body
        gr.set(12, v, B + 3, BROWN)        # neck
        gr.set(12, v, B + 4, BROWN)        # head
        gr.set(13, v, B + 4, RED if v > 0 else BROWN)   # nose
        gr.set(12, v, B + 5, DARK)         # antler
        gr.set(11, v + (1 if v > 0 else -1), B + 4, DARK)
        gr.set(12, v + (1 if v > 0 else -1), B + 5, DARK)
        for u in (9, 11):
            gr.set(u, v, B + 1, BROWN)     # legs
        gr.set(10, v, B + 3, WHT)          # blanket
    # harness
    for u in (7, 8):
        gr.set(u, 0, B + 2, RED)
        gr.set(u, -2, B + 2, RED)
        gr.set(u, 1, B + 2, RED)
    gr.bake(doc, cx, cz, sec)


# --------------------------------------------------------------------------
# Hot-air balloons
# --------------------------------------------------------------------------
def build_balloon(doc, cx, cz, cy, cols, sec="15_balloons"):
    rx, ry, rz = 4.6, 5.6, 4.6
    n = 0
    for dx in range(-5, 6):
        for dy in range(-6, 7):
            for dz in range(-5, 6):
                f = (dx / rx) ** 2 + (dy / ry) ** 2 + (dz / rz) ** 2
                if f > 1.0:
                    continue
                # shell only
                g = ((dx * 1.1) / rx) ** 2 + ((dy * 1.1) / ry) ** 2 + ((dz * 1.1) / rz) ** 2
                if g < 1.0 and dy < 5:
                    continue
                a = math.atan2(dz, dx)
                stripe = int((a + math.pi) / (math.pi / 4)) % len(cols)
                blk = cols[stripe]
                if dy <= -5:
                    blk = "minecraft:gold_block"
                doc.set(cx + dx, cy + dy, cz + dz, blk, sec)
                n += 1
    # basket + ropes
    by = cy - 10
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            doc.set(cx + dx, by, cz + dz, "minecraft:dark_oak_planks", sec)
            doc.set(cx + dx, by + 1, cz + dz, "minecraft:dark_oak_planks", sec)
            doc.set(cx + dx, by + 2, cz + dz, "minecraft:spruce_fence", sec)
            doc.set(cx + dx, by + 3, cz + dz, "minecraft:dark_oak_planks", sec)
    doc.set(cx, by + 1, cz, "minecraft:shroomlight", sec)
    for dx, dz in ((-1, -1), (-1, 1), (1, -1), (1, 1)):
        for y in range(by + 3, cy - 5):
            doc.set(cx + dx, y, cz + dz, "minecraft:chain", sec)
    return n


# ==========================================================================
# Master build
# ==========================================================================
def build_all():
    doc = Doc()
    rng = random.Random(20251125)

    # ---- the eight portals ------------------------------------------------
    portals = []
    for g, pad in zip(GAMES, PAD_C):
        p = Portal(g, pad)
        p.build(doc)
        portals.append(p)

    # ---- market stalls: two per mid-bearing, in the land ring ------------
    stall_pts = []
    for k in range(8):
        b = 22.5 + 45.0 * k
        pal = STALL_PALETTES[k % len(STALL_PALETTES)]
        face = round((b + 180.0) / 90.0) * 90.0     # keep stalls axis aligned
        base = ang(b, 70.0)
        for side in (-1, 1):
            px = base[0] + side * 4.6 * math.cos(math.radians(b + 90))
            pz = base[1] + side * 4.6 * math.sin(math.radians(b + 90))
            px, pz = round(px), round(pz)
            if path_pd(px, pz) < 4.0:
                continue
            build_stall(doc, px, pz, face, pal, rng)
            stall_pts.append((px, pz))
        # crates beside each pair
        for dx, dz, sz in ((7, 6, 2), (-8, 5, 1), (8, -5, 1)):
            x, z = int(base[0]) + dx, int(base[1]) + dz
            if safe_ground(x, z, sz) and pad_gap(x, z) >= 1.0:
                build_present(doc, x, z, sz, rng)

    # ---- trees: fill the wedges between the pads ------------------------
    spots = []
    for k in range(160):
        b = k * 2.25
        for r in (65.0, 69.0, 73.0, 77.0):
            x, z = ang(b, r)
            x, z = round(x), round(z)
            rr = math.hypot(x - CX, z - CZ)
            if rr < 63.0 or rr > 81.0:
                continue
            if pad_gap(x, z) < 1.2 or path_pd(x, z) < 5.0:
                continue
            if any(math.hypot(x - a, z - c) < 8.0 for a, c in spots):
                continue
            if any(math.hypot(x - a, z - c) < 11.0 for a, c in stall_pts):
                continue
            spots.append((x, z))
    rng.shuffle(spots)
    trees = 0
    for (x, z) in spots[:26]:
        h = rng.choice((6, 6, 7, 7, 8, 9))
        if build_tree(doc, x, z, h, rng, decorated=(rng.random() < 0.15)):
            trees += 1
    print(f"   trees placed: {trees}")

    # ---- lamp posts + bunting on the causeway edges ---------------------
    for k in range(8):
        b = 45.0 * k
        for r, bun in ((51.5, True),):
            pts = []
            for side in (-1, 1):
                lat = 2.6 * side
                px = CX + r * math.cos(math.radians(b)) - lat * math.sin(math.radians(b))
                pz = CZ + r * math.sin(math.radians(b)) + lat * math.cos(math.radians(b))
                px, pz = round(px), round(pz)
                if path_pd(px, pz) > PATH_HALF:
                    continue
                if build_lamp(doc, px, pz):
                    pts.append((px, pz))
            if bun and len(pts) == 2:
                bunting(doc, pts[0], pts[1], Y0 + 4)

    # ---- gate posts framing each portal door ----------------------------
    for p in portals:
        for sc in (-7.4, 7.4):
            px = p.cxm + (p.n_front + 1.0) * p.fx + sc * p.tx
            pz = p.czm + (p.n_front + 1.0) * p.fz + sc * p.tz
            build_lamp(doc, round(px), round(pz))

    # ---- presents at the portal thresholds and around the market --------
    for p in portals:
        n = p.n_front
        for s0, sz in ((-5.0, 2), (5.0, 1)):
            px = p.cxm + (n + 1.6) * p.fx + s0 * p.tx
            pz = p.czm + (n + 1.6) * p.fz + s0 * p.tz
            build_present(doc, round(px), round(pz), sz, rng)
    for k in range(10):
        b = 30.0 + 36.0 * k
        x, z = ang(b, 64.0 + (k * 3) % 9)
        x, z = round(x), round(z)
        sz = 1 + k % 2
        if pad_gap(x, z) < 4.0 or path_pd(x, z) < 5.0:
            continue
        if not safe_ground(x, z, sz):
            continue
        build_present(doc, x, z, sz, rng)

    # ---- sleigh (flying over the SE quadrant, heading for the dome) -----
    sx, sz = ang(57.0, 82.0)
    build_sleigh(doc, sx, sz, 156, 270.0)

    # ---- balloons --------------------------------------------------------
    for b, r, y, cols in (
        (18.0, 92.0, 166.0, ("minecraft:magenta_concrete", "minecraft:light_blue_concrete",
                             "minecraft:white_concrete", "minecraft:light_blue_concrete")),
        (132.0, 90.0, 176.0, ("minecraft:blue_concrete", "minecraft:red_concrete",
                              "minecraft:white_concrete", "minecraft:blue_concrete")),
        (262.0, 88.0, 158.0, ("minecraft:yellow_concrete", "minecraft:red_concrete",
                              "minecraft:green_concrete", "minecraft:gold_block")),
    ):
        bx, bz = ang(b, r)
        build_balloon(doc, round(bx), round(bz), int(y), cols)
    return doc


# ==========================================================================
# Emission
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
            while (j + 1 < len(row) and row[j + 1][0] == row[j][0] + 1
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

    # group by section
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
            if len(cmds) > CHUNK_CMDS:
                name = f"{key}_{chunk:02d}"
            else:
                name = key
            body = [f"# LBR-4 {dict(SECTIONS).get(key, key)} - part {chunk} "
                    f"({len(part)} commands)"]
            body.extend(part)
            text = "\n".join(body) + "\n"
            for d in (FN_DIR, DP_DIR):
                with open(os.path.join(d, name + ".mcfunction"), "w", encoding="utf-8") as fh:
                    fh.write(text)
            files.append((name, len(part), len(text)))
            chunk += 1

    # load.mcfunction
    load = ["# LBR-4 hub dressing - portals, market, sleigh, balloons, trees, lamps, presents",
            "# run:  function mcchub:lbr4/load"]
    for name, _, _ in files:
        load.append(f"function {NS}:lbr4/{name}")
    for d in (FN_DIR, DP_DIR):
        with open(os.path.join(d, "load.mcfunction"), "w", encoding="utf-8") as fh:
            fh.write("\n".join(load) + "\n")

    # pack.mcmeta
    with open(os.path.join(HERE, "datapack", "pack.mcmeta"), "w", encoding="utf-8") as fh:
        fh.write('{\n  "pack": {\n    "pack_format": 48,\n'
                 '    "description": "MCC festive hub replica - LBR-4 portals and dressing"\n'
                 '  }\n}\n')

    # blocks.csv
    with open(os.path.join(HERE, "blocks.csv"), "w", encoding="utf-8", newline="") as fh:
        fh.write("x,y,z,block\n")
        for (x, y, z) in sorted(doc.b, key=lambda k: (k[1], k[2], k[0])):
            fh.write(f"{x},{y},{z},{doc.b[(x, y, z)]}\n")

    return files


# ==========================================================================
# Preview dump (build + preview-only context so placement can be judged)
# ==========================================================================
def context_block(x, z):
    """Preview-only stand-in for LBR-1's surface (so placement can be judged)."""
    r = math.hypot(x + 0.5 - CX, z + 0.5 - CZ)
    pd = path_pd(x, z)
    if r <= PLAZA_R:
        return "minecraft:smooth_stone" if r <= PLAZA_R - 1.2 else "minecraft:deepslate_tiles"
    if r < LAGOON_OUT:
        return "minecraft:dirt_path" if pd <= PATH_HALF else "minecraft:water[level=0]"
    if r < 96:
        if pad_gap(x, z) <= 0.1:
            return "minecraft:smooth_stone"
        return "minecraft:dirt_path" if pd <= PATH_HALF else "minecraft:snow_block"
    return None


def write_detail(doc, name, cx, cz, rad=34, ctx=True):
    """Close-up dump of one portal (with optional preview context)."""
    x0, x1 = cx - rad, cx + rad
    z0, z1 = cz - rad, cz + rad
    y0, y1 = 116, 200
    blocks = {}
    for (x, y, z), b in doc.b.items():
        if x0 <= x <= x1 and z0 <= z <= z1 and y0 <= y <= y1:
            blocks[(x, y, z)] = b
    if ctx:
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b = context_block(x, z)
                if b:
                    blocks.setdefault((x, 120, z), b)
    with open(os.path.join(HERE, name), "w", encoding="utf-8") as fh:
        fh.write(f"MCMDUMP 1 {x1 - x0 + 1} {y1 - y0 + 1} {z1 - z0 + 1}\n")
        for (x, y, z) in sorted(blocks, key=lambda k: (k[1], k[2], k[0])):
            fh.write(f"{x - x0} {y - y0} {z - z0} {blocks[(x, y, z)]}\n")


def write_preview(doc):
    x0, z0, y0, x1, z1, y1 = RG
    blocks = dict(doc.b)

    def ctx(x, y, z, blk):
        if (x, y, z) not in blocks:
            blocks[(x, y, z)] = blk

    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b = context_block(x, z)
            if b:
                ctx(x, GROUND, z, b)
    with gzip.open(os.path.join(HERE, "preview.mcmdump.gz"), "wt", encoding="utf-8") as fh:
        fh.write(f"MCMDUMP 1 {x1 - x0 + 1} {y1 - y0 + 1} {z1 - z0 + 1}\n")
        for (x, y, z) in sorted(blocks, key=lambda k: (k[1], k[2], k[0])):
            fh.write(f"{x - x0} {y - y0} {z - z0} {blocks[(x, y, z)]}\n")


# ==========================================================================
# Checks
# ==========================================================================
def checks(doc):
    rep = {}
    x0, z0, y0, x1, z1, y1 = RG
    low = [k for k in doc.b if k[1] < Y0]
    rep["below_y121"] = len(low)

    dome = 0
    for (x, y, z) in doc.b:
        if math.hypot(x + 0.5 - CX, z + 0.5 - CZ) < 29.0:
            dome += 1
    rep["inside_dome_radius29"] = dome

    mountain = 0
    globe = 0
    for (x, y, z) in doc.b:
        if 4870 <= x <= 4932 and 4968 <= z <= 5032:
            mountain += 1
        if math.dist((x + 0.5, y + 0.5, z + 0.5), (5000.5, 132.5, 5080.5)) < 13.0:
            globe += 1
    rep["in_mountain_box"] = mountain
    rep["in_globe_sphere"] = globe

    over_water = 0
    for (x, y, z) in doc.b:
        if y != Y0:
            continue
        r = math.hypot(x + 0.5 - CX, z + 0.5 - CZ)
        if PLAZA_R < r < LAGOON_OUT and path_pd(x, z) > PATH_HALF:
            over_water += 1
    rep["ground_over_lagoon"] = over_water

    # ---- explicit clash reports against LBR-3's briefed volumes ----------
    globe_pts = [k for k in doc.b
                 if math.dist((k[0] + 0.5, k[1] + 0.5, k[2] + 0.5),
                              (5000.0, 132.0, 5080.0)) < 12.0]
    rep["globe_clash"] = len(globe_pts)
    if globe_pts:
        rep["globe_bbox"] = (min(p[0] for p in globe_pts), min(p[1] for p in globe_pts),
                             min(p[2] for p in globe_pts), max(p[0] for p in globe_pts),
                             max(p[1] for p in globe_pts), max(p[2] for p in globe_pts))
    mount_pts = [k for k in doc.b if 4870 <= k[0] <= 4930 and 4980 <= k[2] <= 5020]
    rep["mountain_clash"] = len(mount_pts)
    if mount_pts:
        rep["mountain_bbox"] = (min(p[0] for p in mount_pts), min(p[1] for p in mount_pts),
                                min(p[2] for p in mount_pts), max(p[0] for p in mount_pts),
                                max(p[1] for p in mount_pts), max(p[2] for p in mount_pts))

    out = 0
    for (x, y, z) in doc.b:
        if not (x0 <= x <= x1 and z0 <= z <= z1 and y0 <= y <= y1):
            out += 1
    rep["outside_region"] = out

    # per-section totals
    per = {}
    for k, s in doc.sec.items():
        per[s] = per.get(s, 0) + 1
    rep["per_section"] = per

    xs = [k[0] for k in doc.b]
    ys = [k[1] for k in doc.b]
    zs = [k[2] for k in doc.b]
    rep["bounds"] = (min(xs), min(ys), min(zs), max(xs), max(ys), max(zs))
    return rep


# ==========================================================================
def main():
    doc = build_all()
    files = emit(doc)
    write_preview(doc)
    write_detail(doc, "detail_ace_race.mcmdump", 5072, 5000, 30)
    write_detail(doc, "detail_battle_box.mcmdump", 5051, 5051, 30)
    write_detail(doc, "detail_market.mcmdump", *[round(v) for v in ang(22.5, 70.0)], 26)
    rep = checks(doc)

    print(f"blocks           : {doc.count()}")
    print(f"commands (chunks): {sum(f[1] for f in files)} in {len(files)} files")
    print(f"bounds           : {rep['bounds']}")
    for k in ("below_y121", "inside_dome_radius29", "in_mountain_box",
              "in_globe_sphere", "ground_over_lagoon", "outside_region",
              "globe_clash", "mountain_clash"):
        print(f"{k:18s}: {rep.get(k)}")
    for k in ("globe_bbox", "mountain_bbox"):
        if k in rep:
            print(f"{k:18s}: {rep[k]}")
    print("per section:")
    for s in sorted(rep["per_section"]):
        print(f"   {s:24s} {rep['per_section'][s]}")
    print("files:")
    for name, n, size in files:
        print(f"   {name:28s} {n:6d} cmds {size/1024:8.1f} KiB")
    return rep


if __name__ == "__main__":
    main()
