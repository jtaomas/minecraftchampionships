#!/usr/bin/env python3
"""
V4-D  --  MCC festive-hub replica, part D.

Scope (BRIEF.md, section "Ownership", item V4-D):
  * Aqua building + water ride, east at 80,120,0
  * six training portals: Ace Race, Battle Box, Dodgebolt,
    Parkour Warrior, Parkour Tag, Rocket Spleef Rush
  * props: snow, trees, boats, hot-air balloons, presents,
    candy canes, lamps, market stalls

Not built here: dome / plinth, base island + water, THE CHAMPS, Hall of Fame,
rainbow bridge, snow globe, team columns, stage plots.  See STATUS.md.

Site: C4 = 0,120,0.  Nothing is placed below y = 121, so the island surface
(y = 120, V4-A) is never written to.

Output
------
  blocks.csv                      x,y,z,block   (authoritative block list)
  functions/*.mcfunction          flat copy for inspection
  datapack/data/mcchub/function/v4d/*.mcfunction
  preview.mcmdump.gz              build + preview-only ground context
  preview_top.png / preview_iso.png / preview_iso_back.png
  detail_*.png
  STATUS.md / GAPS.md

Run:  python gen_v4d.py
"""

from __future__ import annotations

import gzip
import math
import os
import random

HERE = os.path.dirname(os.path.abspath(__file__))
FN_DIR = os.path.join(HERE, "functions")
DP_DIR = os.path.join(HERE, "datapack", "data", "mcchub", "function", "v4d")
NS = "mcchub"

# --------------------------------------------------------------------------
# Site constants
# --------------------------------------------------------------------------
Y0 = 121                     # lowest row this section may touch (island top = 120)
GROUND = 120
CHUNK_CMDS = 8000
RG = (-150, -150, 90, 150, 150, 210)      # preview region: x0,z0,y0,x1,z1,y1

# Plot rectangles reserved by this agent (world x/z, inclusive).
# Documented in STATUS.md so V4-E can check integration.
PLOTS = {
    "ace_race":        (-118, -100, -32, -56),
    "battle_box":      (-45, -119, -5, -89),
    "dodgebolt":       (52, -100, 100, -60),
    "parkour_warrior": (0, -118, 48, -82),
    "parkour_tag":     (-120, -70, -80, -30),
    "rocket_spleef":   (85, -60, 133, -20),
    "aqua":            (40, -22, 101, 22),
}

SECTIONS = [
    ("00_aprons", "paved aprons and forecourts"),
    ("01_ace_race", "Ace Race sea-serpent portal"),
    ("02_battle_box", "Battle Box portal building"),
    ("03_dodgebolt", "Dodgebolt court"),
    ("04_parkour_warrior", "Parkour Warrior frog and sign"),
    ("05_parkour_tag", "Parkour Tag portal building"),
    ("06_rocket_spleef", "Rocket Spleef Rush gantry"),
    ("07_aqua", "Aqua building and water ride"),
    ("10_market", "market stalls"),
    ("11_trees", "palms, spruces, decorated trees"),
    ("12_lamps_canes", "lamps and candy canes"),
    ("13_presents", "presents, snowmen, snow cover"),
    ("14_boats", "rowboats and steamboat"),
    ("15_balloons", "hot-air balloons"),
]

# --------------------------------------------------------------------------
# Palette (vanilla Java 1.21.1 only)
# --------------------------------------------------------------------------
W = "minecraft:white_concrete"
LG = "minecraft:light_gray_concrete"
GY = "minecraft:gray_concrete"
BK = "minecraft:black_concrete"
RD = "minecraft:red_concrete"
OR = "minecraft:orange_concrete"
YL = "minecraft:yellow_concrete"
GO = "minecraft:gold_block"
CY = "minecraft:cyan_concrete"
LB = "minecraft:light_blue_concrete"
BL = "minecraft:blue_concrete"
NAVY = "minecraft:blue_concrete"
DPB = "minecraft:polished_blackstone"
SB = "minecraft:stone_bricks"
MSB = "minecraft:mossy_stone_bricks"
SS = "minecraft:smooth_stone"
SAND = "minecraft:sand"
SAND_APRON = "minecraft:sandstone"
SST = "minecraft:smooth_sandstone"
CSST = "minecraft:cut_sandstone"
OAK = "minecraft:oak_planks"
SPR = "minecraft:spruce_planks"
DKO = "minecraft:dark_oak_planks"
LOGS = "minecraft:spruce_log"
JUNG = "minecraft:jungle_planks"
TERRA = "minecraft:orange_terracotta"
BROWN = "minecraft:brown_terracotta"
TEAL = "minecraft:cyan_terracotta"
WATER = "minecraft:water[level=0]"
LAVA = "minecraft:lava[level=0]"
SNOW = "minecraft:snow_block"
LEAF_SPRUCE = "minecraft:spruce_leaves[persistent=true]"
LEAF_JUNGLE = "minecraft:jungle_leaves[persistent=true]"
LEAF_OAK = "minecraft:oak_leaves[persistent=true]"
GLASS = "minecraft:glass"
GLASS_CY = "minecraft:cyan_stained_glass"
GLASS_LB = "minecraft:light_blue_stained_glass"
GLASS_WH = "minecraft:white_stained_glass"
PANE_LB = "minecraft:light_blue_stained_glass_pane"
PANE_WH = "minecraft:white_stained_glass_pane"
SEA = "minecraft:sea_lantern"
LANTERN = "minecraft:lantern[hanging=false]"
IRON = "minecraft:iron_block"
IBARS = "minecraft:iron_bars"
CHAIN = "minecraft:chain"


def sl(n: int) -> str:
    return f"minecraft:snow[layers={n}]"


# --------------------------------------------------------------------------
# Deterministic hash noise (fast, stable across runs/platforms)
# --------------------------------------------------------------------------
def h3(x, y, z, salt=0):
    n = (int(x) * 73856093) ^ (int(y) * 19349663) ^ (int(z) * 83492791) ^ (salt * 2654435761)
    n = (n ^ (n >> 13)) * 1274126177
    n &= 0xFFFFFFFF
    return n / 4294967295.0


def pick(x, y, z, salt, choices):
    return choices[int(h3(x, y, z, salt) * len(choices)) % len(choices)]


# --------------------------------------------------------------------------
# Voxel document
# --------------------------------------------------------------------------
class Doc:
    def __init__(self):
        self.b = {}
        self.sec = {}
        self.skipped = 0
        self.placed = 0

    def set(self, x, y, z, blk, sec, force=True):
        if blk is None:
            return False
        x, y, z = int(round(x)), int(round(y)), int(round(z))
        if y < Y0:
            raise SystemExit(f"BUG: block below y={Y0} at {x},{y},{z} ({blk})")
        k = (x, y, z)
        if not force and k in self.b:
            self.skipped += 1
            return False
        self.b[k] = blk
        self.sec[k] = sec
        self.placed += 1
        return True

    def get(self, x, y, z):
        return self.b.get((int(round(x)), int(round(y)), int(round(z))))

    def remove(self, x, y, z):
        k = (int(round(x)), int(round(y)), int(round(z)))
        self.b.pop(k, None)
        self.sec.pop(k, None)

    def box(self, x0, y0, z0, x1, y1, z1, blk, sec, force=True):
        for y in range(int(y0), int(y1) + 1):
            for z in range(int(z0), int(z1) + 1):
                for x in range(int(x0), int(x1) + 1):
                    self.set(x, y, z, blk, sec, force)

    def frame_box(self, x0, y0, z0, x1, y1, z1, blk, sec, force=True):
        """Hollow box (walls, no top/bottom fill beyond the shell)."""
        for y in (y0, y1):
            for z in range(int(z0), int(z1) + 1):
                for x in range(int(x0), int(x1) + 1):
                    self.set(x, y, z, blk, sec, force)
        for z in (z0, z1):
            for y in range(int(y0), int(y1) + 1):
                for x in range(int(x0), int(x1) + 1):
                    self.set(x, y, z, blk, sec, force)
        for x in (x0, x1):
            for y in range(int(y0), int(y1) + 1):
                for z in range(int(z0), int(z1) + 1):
                    self.set(x, y, z, blk, sec, force)

    def count(self):
        return len(self.b)


# --------------------------------------------------------------------------
# Local frame: u = width (to the right when facing the build from outside),
#               v = depth, positive toward the plaza (the outside/front).
# --------------------------------------------------------------------------
class Frame:
    def __init__(self, cx, cz, facing):
        self.cx, self.cz, self.f = int(cx), int(cz), facing

    def p(self, u, v):
        if self.f == "S":
            return self.cx + u, self.cz + v
        if self.f == "N":
            return self.cx - u, self.cz - v
        if self.f == "W":
            return self.cx - v, self.cz + u
        if self.f == "E":
            return self.cx + v, self.cz - u
        raise SystemExit("bad facing")

    def set(self, doc, u, v, y, blk, sec, force=True):
        x, z = self.p(u, v)
        return doc.set(x, y, z, blk, sec, force)

    def fill(self, doc, u0, u1, v0, v1, y0, y1, blk, sec, force=True):
        for u in range(int(u0), int(u1) + 1):
            for v in range(int(v0), int(v1) + 1):
                for y in range(int(y0), int(y1) + 1):
                    self.set(doc, u, v, y, blk, sec, force)


    def shell(self, doc, u0, u1, v0, v1, y0, y1, wall, sec, force=True):
        """One-block-thick ring of walls, no floor or ceiling."""
        for y in range(int(y0), int(y1) + 1):
            for u in range(int(u0), int(u1) + 1):
                for v in range(int(v0), int(v1) + 1):
                    if u in (u0, u1) or v in (v0, v1):
                        self.set(doc, u, v, y, wall, sec, force)

    def fill_fn(self, doc, u0, u1, v0, v1, y0, y1, fn, sec, force=True):
        for u in range(int(u0), int(u1) + 1):
            for v in range(int(v0), int(v1) + 1):
                for y in range(int(y0), int(y1) + 1):
                    b = fn(u, v, y)
                    if b:
                        self.set(doc, u, v, y, b, sec, force)


# --------------------------------------------------------------------------
# 5x7 block font (one block per pixel) for the big marquees
# --------------------------------------------------------------------------
FONT = {
    "A": (".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"),
    "B": ("####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."),
    "C": (".####", "#....", "#....", "#....", "#....", "#....", ".####"),
    "D": ("####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."),
    "E": ("#####", "#....", "#....", "####.", "#....", "#....", "#####"),
    "F": ("#####", "#....", "#....", "####.", "#....", "#....", "#...."),
    "G": (".####", "#....", "#....", "#..##", "#...#", "#...#", ".####"),
    "H": ("#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"),
    "I": ("#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"),
    "J": ("..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##.."),
    "K": ("#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"),
    "L": ("#....", "#....", "#....", "#....", "#....", "#....", "#####"),
    "M": ("#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"),
    "N": ("#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#"),
    "O": (".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
    "P": ("####.", "#...#", "#...#", "####.", "#....", "#....", "#...."),
    "Q": (".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#"),
    "R": ("####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"),
    "S": (".####", "#....", "#....", ".###.", "....#", "....#", "####."),
    "T": ("#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."),
    "U": ("#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
    "V": ("#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#.."),
    "W": ("#...#", "#...#", "#...#", "#.#.#", "#.#.#", "##.##", "#...#"),
    "X": ("#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#"),
    "Y": ("#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."),
    "Z": ("#####", "....#", "...#.", "..#..", ".#...", "#....", "#####"),
    "0": (".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."),
    "1": ("..#..", ".##..", "..#..", "..#..", "..#..", "..#..", "#####"),
    "2": (".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"),
    "3": ("####.", "....#", "....#", ".###.", "....#", "....#", "####."),
    "4": ("...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."),
    "5": ("#####", "#....", "####.", "....#", "....#", "#...#", ".###."),
    "6": (".###.", "#....", "#....", "####.", "#...#", "#...#", ".###."),
    "7": ("#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."),
    "8": (".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."),
    "9": (".###.", "#...#", "#...#", ".####", "....#", "....#", ".###."),
    ":": (".....", "..#..", "..#..", ".....", "..#..", "..#..", "....."),
    ".": (".....", ".....", ".....", ".....", ".....", ".##..", ".##.."),
    " ": ("...", "...", "...", "...", "...", "...", "..."),
}


MINI = {
    "A": (".#.", "#.#", "###", "#.#", "#.#"),
    "B": ("##.", "#.#", "##.", "#.#", "##."),
    "C": (".##", "#..", "#..", "#..", ".##"),
    "D": ("##.", "#.#", "#.#", "#.#", "##."),
    "E": ("###", "#..", "##.", "#..", "###"),
    "F": ("###", "#..", "##.", "#..", "#.."),
    "G": (".##", "#..", "#.#", "#.#", ".##"),
    "H": ("#.#", "#.#", "###", "#.#", "#.#"),
    "I": ("###", ".#.", ".#.", ".#.", "###"),
    "J": ("..#", "..#", "..#", "#.#", ".#."),
    "K": ("#.#", "#.#", "##.", "#.#", "#.#"),
    "L": ("#..", "#..", "#..", "#..", "###"),
    "M": ("#.#", "###", "###", "#.#", "#.#"),
    "N": ("##.", "#.#", "#.#", "#.#", "#.#"),
    "O": (".#.", "#.#", "#.#", "#.#", ".#."),
    "P": ("##.", "#.#", "##.", "#..", "#.."),
    "Q": (".#.", "#.#", "#.#", "##.", ".##"),
    "R": ("##.", "#.#", "##.", "#.#", "#.#"),
    "S": (".##", "#..", ".#.", "..#", "##."),
    "T": ("###", ".#.", ".#.", ".#.", ".#."),
    "U": ("#.#", "#.#", "#.#", "#.#", ".##"),
    "V": ("#.#", "#.#", "#.#", "#.#", ".#."),
    "W": ("#.#", "#.#", "###", "###", "#.#"),
    "X": ("#.#", "#.#", ".#.", "#.#", "#.#"),
    "Y": ("#.#", "#.#", ".#.", ".#.", ".#."),
    "Z": ("###", "..#", ".#.", "#..", "###"),
    " ": (" ", " ", " ", " ", " "),
}


def text_width(line, gap=1):
    return sum(len(FONT[c][0]) for c in line) + gap * (len(line) - 1)


def draw_mini(doc, fr, u_left, y_bottom, line, blk, sec, v, gap=1, force=True):
    """3x5 letters, for the smaller boards."""
    u = u_left
    for ch in line:
        pat = MINI.get(ch)
        if pat is None:
            u += 2
            continue
        w = len(pat[0])
        for r, row in enumerate(pat):
            for c, bit in enumerate(row):
                if bit == "#":
                    fr.set(doc, u + c, v, y_bottom + (4 - r), blk, sec, force)
        u += w + gap
    return u - gap - u_left


def mini_width(line, gap=1):
    return sum(len(MINI[c][0]) for c in line) + gap * (len(line) - 1)


def mini_centred(doc, fr, uc, y_bottom, line, blk, sec, v, gap=1, force=True):
    w = mini_width(line, gap)
    return draw_mini(doc, fr, uc - w // 2, y_bottom, line, blk, sec, v, gap, force)


def draw_text(doc, fr, u_left, y_bottom, line, blk, sec, v, gap=1, force=True):
    """Draw a line of 5x7 letters on the plane v, reading toward +u."""
    u = u_left
    for ch in line:
        pat = FONT.get(ch)
        if pat is None:
            u += 4
            continue
        w = len(pat[0])
        for r, row in enumerate(pat):
            for c, bit in enumerate(row):
                if bit == "#":
                    fr.set(doc, u + c, v, y_bottom + (6 - r), blk, sec, force)
        u += w + gap
    return u - gap - u_left


def centred_text(doc, fr, uc, y_bottom, line, blk, sec, v, gap=1, force=True):
    w = text_width(line, gap)
    return draw_text(doc, fr, uc - w // 2, y_bottom, line, blk, sec, v, gap, force)


def marble(fr, doc, u0, u1, v0, v1, y0, y1, base, alt, sec, salt=7, p=0.3, force=True):
    fr.fill_fn(doc, u0, u1, v0, v1, y0, y1,
               lambda u, v, y: alt if h3(u, y, v, salt) < p else base, sec, force)


def stripe(fr, doc, u0, u1, v0, v1, y0, y1, a, b, sec, period=2, force=True):
    fr.fill_fn(doc, u0, u1, v0, v1, y0, y1,
               lambda u, v, y: a if ((u + v + y) // 1) % period == 0 else b, sec, force)


def hazard(fr, doc, u0, u1, v0, v1, y0, y1, sec, force=True):
    """Yellow / black diagonal hazard band (as on the Rocket Spleef gantry)."""
    fr.fill_fn(doc, u0, u1, v0, v1, y0, y1,
               lambda u, v, y: YL if ((u - v) % 4) < 2 else BK, sec, force)


AR_C = (-72, -78)          # Ace Race sand cay centre


# ==========================================================================
# 00 -- paved aprons in front of each plot
# ==========================================================================
def build_aprons(doc):
    sec = "00_aprons"
    # (frame, u0,u1,v0,v1, path half-width, paving, kerb)
    jobs = [
        (Frame(AR_C[0], AR_C[1], "S"), -30, 30, 4, 19, 4, SAND_APRON, LG),     # Ace Race beach
        (Frame(-26, -104, "S"), -16, 16, 5, 15, 4, SS, LG),    # Battle Box
        (Frame(76, -80, "S"), -20, 20, 10, 16, 5, SPR, LG),    # Dodgebolt deck walk
        (Frame(24, -100, "S"), -20, 20, 9, 18, 4, SB, LG),     # Parkour Warrior
        (Frame(-100, -50, "E"), -16, 16, 8, 18, 4, SS, LG),    # Parkour Tag
        (Frame(110, -40, "W"), -17, 17, 8, 22, 5, SS, LG),     # Rocket Spleef
    ]
    for fr, u0, u1, v0, v1, ph, pave, kerb in jobs:
        for u in range(u0, u1 + 1):
            for v in range(v0, v1 + 1):
                # rounded corners so the apron is not a hard rectangle
                if abs(u) > (u1 - 2) and v in (v0, v1):
                    continue
                on_path = abs(u) <= ph
                fr.set(doc, u, v, Y0, pave if on_path else kerb, sec, force=False)
        # candy-striped kerb dots along the front edge
        for u in range(u0 + 2, u1 - 1, 6):
            fr.set(doc, u, v1, Y0, RD, sec, force=False)
        # snow dusting off the walking line
        for u in range(u0, u1 + 1):
            for v in range(v0, v1 + 1):
                if abs(u) <= ph:
                    continue
                if h3(u // 2, 7, v // 2, 6) < 0.5:
                    fr.set(doc, u, v, Y0 + 1, sl(1), sec, force=False)


# ==========================================================================
# 01 -- Ace Race: giant sea serpent arching over a sand cay  (MainHub-AR.png)
# ==========================================================================
AR_PATH = [
    #  u,     y,    z,    r
    (-43.0, 33.0, -1.9, 0.9),
    (-41.5, 31.8, -1.7, 1.3),
    (-40.0, 30.0, -1.5, 1.8),
    (-36.0, 26.0, -1.2, 2.1),
    (-32.0, 20.0, -0.9, 2.5),
    (-29.0, 13.0, -0.6, 3.0),
    (-27.0, 8.0, -0.3, 3.4),
    (-25.5, 5.0, 0.0, 3.8),
    (-22.0, 9.0, 0.0, 4.1),
    (-18.0, 15.0, 0.0, 4.3),
    (-13.0, 22.0, 0.0, 4.4),
    (-8.0, 28.0, 0.0, 4.5),
    (-2.0, 32.0, 0.0, 4.5),
    (4.0, 33.0, 0.0, 4.5),
    (10.0, 31.0, 0.0, 4.4),
    (15.0, 27.0, 0.0, 4.3),
    (19.0, 21.0, 0.0, 4.2),
    (22.0, 15.0, 0.0, 4.0),
    (24.0, 10.5, 0.0, 3.8),
]


def _ar_colour(u, y, z, ny):
    """Body colour: warm terracotta hide above, pale plated belly below."""
    if ny < -0.40:
        return pick(u // 2, y // 2, z // 2, 41, (LG, LG, SS, "minecraft:gray_concrete"))
    u, y, z = u // 3, y // 3, z // 3
    n = h3(u, y, z, 42)
    if n < 0.18:
        return BROWN
    if n < 0.38:
        return TERRA
    if n < 0.55:
        return "minecraft:terracotta"
    if n < 0.70:
        return "minecraft:brown_concrete"
    if n < 0.88:
        return OR
    return "minecraft:orange_terracotta"


def _tube(doc, path, sec):
    """Sweep a chunky tube along `path` = [(u,y,z,r), ...]."""
    for i in range(len(path) - 1):
        a, b = path[i], path[i + 1]
        seg = math.dist(a[:3], b[:3])
        steps = max(1, int(seg * 2.6))
        for st in range(steps + 1):
            t = st / steps
            cu = a[0] + (b[0] - a[0]) * t
            cy = a[1] + (b[1] - a[1]) * t
            cz = a[2] + (b[2] - a[2]) * t
            cr = a[3] + (b[3] - a[3]) * t
            ri = int(math.ceil(cr + 1.0))
            for du in range(-ri, ri + 1):
                for dy in range(-ri, ri + 1):
                    for dz in range(-ri, ri + 1):
                        wx = cu + du
                        wy = cy + dy
                        wz = cz + dz
                        d = math.sqrt(du * du + dy * dy * 1.04 + dz * dz)
                        bump = (h3(round(wx / 2), round(wy / 2), round(wz / 2), 43) - 0.5) * 0.9
                        if d > cr + bump:
                            continue
                        ny = dy / max(0.6, math.sqrt(du * du + dy * dy + dz * dz))
                        blk = _ar_colour(round(wx), round(wy), round(wz), ny)
                        doc.set(AR_C[0] + wx, Y0 + wy, AR_C[1] + wz, blk, sec, force=True)


def _ar_head(doc, sec):
    """Blue-faced monster head with a red open mouth, at the arch's foot."""
    fr = Frame(AR_C[0], AR_C[1], "S")
    cu, cy0 = 31, 9                 # head centre (u, height above the island)
    wu, wv, wy = 7, 6, 6            # half extents

    # ---- skull -----------------------------------------------------------
    for u in range(cu - wu, cu + wu + 1):
        for v in range(-wv, wv + 1):
            for y in range(cy0 - wy, cy0 + wy + 1):
                du = (u - cu) / wu
                dv = v / wv
                dy = (y - cy0) / wy
                if du * du + dv * dv + dy * dy > 1.0:
                    continue
                rind = du * du + dv * dv + dy * dy > 0.62
                # blue mask over the whole front (-u) half, hide behind
                if u <= cu - 1 or (v >= 3 and y <= cy0 + 2):
                    b = pick(u, y, v, 44, (BL, BL, BL, "minecraft:light_blue_concrete"))
                    if rind and h3(u, y, v, 48) < 0.25:
                        b = "minecraft:light_blue_concrete"
                else:
                    b = pick(u, y, v, 46, (TERRA, OR, "minecraft:terracotta", BROWN))
                fr.set(doc, u, v, Y0 + y, b, sec)

    # ---- mouth: carved through the -u face, wrapping the +v corner -------
    m_u0, m_u1 = cu - wu - 1, cu + 1
    m_v0, m_v1 = -4, 6
    m_y0, m_y1 = cy0 - 4, cy0 + 1
    for u in range(m_u0, m_u1 + 1):
        for v in range(m_v0, m_v1 + 1):
            for y in range(m_y0, m_y1 + 1):
                if v > 2 and u > cu - 2:
                    continue                      # keep the cheek
                x, z = fr.p(u, v)
                doc.remove(x, Y0 + y, z)
    # red throat
    for u in range(m_u0, m_u1 + 1):
        for v in range(m_v0, m_v1 + 1):
            for y in range(m_y0, m_y1 + 1):
                if v > 2 and u > cu - 2:
                    continue
                if u == m_u1 or v == m_v1 or y == m_y1:
                    fr.set(doc, u, v, Y0 + y, RD, sec)
    # teeth along the top and bottom lip
    for u in range(m_u0 - 1, m_u1 + 2):
        for v in range(m_v0, m_v1 + 1):
            if v > 2 and u > cu - 2:
                continue
            if (u + v) % 3 != 0:
                fr.set(doc, u, v, Y0 + m_y1 + 1, W, sec)
                fr.set(doc, u, v, Y0 + m_y0 - 1, W, sec)
    # ---- brow ridges + eyes ---------------------------------------------
    for su in (-4, 3):
        for du in range(0, 3):
            for dv in range(0, 2):
                fr.set(doc, cu + su + du, 1 + dv, Y0 + cy0 + 5, "minecraft:blue_concrete", sec)
                fr.set(doc, cu + su + du, 1 + dv, Y0 + cy0 + 6, BK, sec)
    # ---- dorsal spines along the top of the arch ------------------------
    for i, (pu, py, pz, pr) in enumerate(AR_PATH):
        if i % 2:
            continue
        u, y = int(round(pu)), int(round(py + pr))
        for k in range(2):
            fr.set(doc, u, pz, Y0 + y + k, pick(u, y, k, 47, (BK, GY, "minecraft:blackstone")),
                   sec, force=False)


def build_ace_race(doc, rng):
    sec = "01_ace_race"
    fr = Frame(AR_C[0], AR_C[1], "S")

    # ---- sand cay under the arch -----------------------------------------
    for u in range(-38, 39):
        for v in range(-19, 20):
            e = (u / 37.0) ** 2 + (v / 18.4) ** 2
            wob = 0.10 * math.sin(math.atan2(v, u) * 3.0 + 1.1) + 0.06 * math.sin(math.atan2(v, u) * 7.0)
            if e > 1.0 + wob:
                continue
            blk = pick(u // 3, Y0, v // 3, 31, (SAND, SAND, SAND, SAND, "minecraft:sandstone"))
            fr.set(doc, u, v, Y0, blk, sec, force=False)
            if e < 0.62 and h3(u // 2, 1, v // 2, 32) < 0.45:
                fr.set(doc, u, v, Y0 + 1, pick(u // 2, 2, v // 2, 33, (SAND, SAND, SST)), sec, force=False)
            if e > 0.9:
                fr.set(doc, u, v, Y0, pick(u // 2, 3, v // 2, 34, (SS, LG, SS)), sec, force=False)

    # light snow on the dry sand
    for u in range(-36, 37):
        for v in range(-18, 19):
            if (u / 37.0) ** 2 + (v / 18.4) ** 2 > 0.92:
                continue
            if abs(u) <= 5 and v >= -6:
                continue
            if h3(u // 3, 9, v // 3, 7) < 0.20:
                fr.set(doc, u, v, Y0 + 2, sl(1), sec, force=False)

    # ---- path from the plaza to the arch ---------------------------------
    for v in range(-6, 20):
        for u in range(-3, 4):
            fr.set(doc, u, v, Y0, pick(u, 4, v, 35, (SST, SST, SST, CSST)), sec)
        for u in (-4, 4):
            fr.set(doc, u, v, Y0, CSST, sec)

    # ---- the serpent -----------------------------------------------------
    _tube(doc, AR_PATH, sec)
    _ar_head(doc, sec)

    # ---- beach dressing: shack, driftwood, rowboat on the sand -----------
    su, sv = -29, -13
    for u in range(su, su + 6):
        for v in range(sv, sv + 6):
            fr.set(doc, u, v, Y0, SPR, sec)
            fr.set(doc, u, v, Y0 + 1, SPR, sec)
    for u in (su, su + 5):
        for v in (sv, sv + 5):
            for y in (Y0 + 2, Y0 + 3, Y0 + 4):
                fr.set(doc, u, v, y, "minecraft:spruce_log", sec)
    for u in range(su, su + 6):
        for v in range(sv, sv + 6):
            fr.set(doc, u, v, Y0 + 5, DKO, sec)
            if (u + v) % 3 == 0:
                fr.set(doc, u, v, Y0 + 6, DKO, sec)
    fr.fill(doc, su, su + 5, sv + 3, sv + 5, Y0 + 1, Y0 + 1, "minecraft:dark_oak_planks", sec)
    fr.set(doc, su + 2, sv + 5, Y0 + 2, "minecraft:barrel[facing=up]", sec)
    fr.set(doc, su + 4, sv + 5, Y0 + 2, "minecraft:chest[facing=south]", sec)

    # rowboat pulled up on the sand
    bu, bv = 20, -13
    for du in range(-3, 4):
        for dv in range(-1, 2):
            if abs(du) == 3 and dv != 0:
                continue
            fr.set(doc, bu + du, bv + dv, Y0, OAK, sec)
            fr.set(doc, bu + du, bv + dv, Y0 + 1, "minecraft:oak_slab[type=bottom]" if abs(du) < 3 else OAK, sec)
    fr.set(doc, bu - 4, bv, Y0, OAK, sec)
    fr.set(doc, bu - 4, bv, Y0 + 1, OAK, sec)
    fr.set(doc, bu + 2, bv, Y0 + 1, "minecraft:oak_pressure_plate", sec)

    # grey stone terrace lip on the seaward side (as in the reference)
    for u in range(-20, 12):
        for v in (-18, -17):
            if h3(u, 5, v, 36) < 0.75:
                fr.set(doc, u, v, Y0, pick(u, 6, v, 37, (SS, LG, SB)), sec, force=False)
                fr.set(doc, u, v, Y0 + 1, "minecraft:stone_slab[type=bottom]", sec, force=False)

    # ---- name board on the beach ----------------------------------------
    bu = -22
    bv = 15
    for u in (bu, bu + 12):
        for y in range(Y0 + 1, Y0 + 6):
            fr.set(doc, u, bv - 1, y, "minecraft:spruce_fence", sec)
    fr.fill(doc, bu - 1, bu + 13, bv - 1, bv - 1, Y0 + 5, Y0 + 7, DKO, sec)
    fr.fill(doc, bu, bu + 12, bv - 1, bv - 1, Y0 + 6, Y0 + 6, DKO, sec)
    mini_centred(doc, fr, bu + 6, Y0 + 6, "ACE RACE", W, sec, bv, gap=1)


# --------------------------------------------------------------------------
# Shared decoration used by the two "generic" portal pavilions
# --------------------------------------------------------------------------
CREST = (
    "..#...#..",
    ".###.###.",
    "#########",
    "#########",
    ".#######.",
    "..#####..",
)


def crest_panel(doc, fr, sec, uc, y_bottom, v, panel=RD, flame=GO, frame=LG, scale=1):
    """Gold MCC flame on a red panel, 11x10 with a light frame."""
    w, h = 9 * scale, 6 * scale
    fr.fill(doc, uc - w // 2 - 1, uc + w // 2 + 1, v - 0, v, y_bottom - 1, y_bottom + h, panel, sec)
    fr.fill(doc, uc - w // 2 - 1, uc - w // 2 - 1, v, v, y_bottom - 1, y_bottom + h, frame, sec)
    fr.fill(doc, uc + w // 2 + 1, uc + w // 2 + 1, v, v, y_bottom - 1, y_bottom + h, frame, sec)
    fr.fill(doc, uc - w // 2 - 1, uc + w // 2 + 1, v, v, y_bottom - 1, y_bottom - 1, frame, sec)
    fr.fill(doc, uc - w // 2 - 1, uc + w // 2 + 1, v, v, y_bottom + h, y_bottom + h, frame, sec)
    for r, row in enumerate(CREST):
        for c, bit in enumerate(row):
            if bit == "#":
                for sy in range(scale):
                    for sx in range(scale):
                        fr.set(doc, uc - w // 2 + c * scale + sx, v + 1,
                               y_bottom + (5 - r) * scale + sy, flame, sec)


def glass_wall(doc, fr, sec, u0, u1, y0, y1, v, glass, mull=LG, every=4):
    for u in range(u0, u1 + 1):
        for y in range(y0, y1 + 1):
            if (u - u0) % every == 0:
                fr.set(doc, u, v, y, mull, sec)
            else:
                fr.set(doc, u, v, y, glass, sec)
    fr.fill(doc, u0, u1, v, v, y0, y0, mull, sec)
    fr.fill(doc, u0, u1, v, v, y1, y1, mull, sec)


def snow_cap(fr, doc, u0, u1, v0, v1, y, sec, p=0.55):
    """Clustered snow dusting: coarse noise so it reads as drifts, not static."""
    for u in range(u0, u1 + 1):
        for v in range(v0, v1 + 1):
            if h3(u // 3, y, v // 3, 5) < p:
                fr.set(doc, u, v, y, sl(1), sec, force=False)


# ==========================================================================
# 02 -- Battle Box portal building (the white / red / gold / glass grammar
#       visible in MCCHub-S4.png; no dedicated Battle Box photo is supplied)
# ==========================================================================
def build_battle_box(doc, rng):
    sec = "02_battle_box"
    fr = Frame(-26, -104, "S")
    UB, VB = 15, 4          # half width / front plane of the body
    BACK = -14
    H = 24                  # body top

    # body
    fr.fill(doc, -UB, UB, BACK, VB, Y0, Y0 + 2, RD, sec)                 # red base band
    fr.shell(doc, -UB, UB, BACK, VB, Y0 + 3, Y0 + H, W, sec)             # white shell
    # corner pilasters + roof cornice
    for u in (-UB, -UB + 1, UB - 1, UB):
        fr.fill(doc, u, u, BACK, VB, Y0, Y0 + H, LG, sec)
    fr.fill(doc, -UB, UB, BACK, VB, Y0 + H + 1, Y0 + H + 1, LG, sec)
    fr.fill(doc, -UB - 1, UB + 1, BACK - 1, VB + 1, Y0 + H + 2, Y0 + H + 2, W, sec)

    # front: glass curtain wall either side of the entrance
    glass_wall(doc, fr, sec, -13, -6, Y0 + 5, Y0 + 13, VB, GLASS_LB, every=2)
    glass_wall(doc, fr, sec, 6, 13, Y0 + 5, Y0 + 13, VB, GLASS_LB, every=2)
    # red spandrel over the glass
    fr.fill(doc, -13, -6, VB, VB, Y0 + 14, Y0 + 16, RD, sec)
    fr.fill(doc, 6, 13, VB, VB, Y0 + 14, Y0 + 16, RD, sec)
    fr.fill(doc, -13, 13, VB, VB, Y0 + 17, Y0 + 17, GO, sec)

    # entrance: 11 wide, 10 tall, arched, cut 8 deep
    for u in range(-5, 6):
        for v in range(BACK + 1, VB + 1):
            if v > VB - 1 and abs(u) == 5:
                pass
            for y in range(Y0, Y0 + 11):
                if abs(u) == 5 or y >= Y0 + 9:
                    continue
                x, z = fr.p(u, v)
                doc.remove(x, y, z)
    # arch top corners
    for u in (-5, 5):
        for v in range(BACK + 1, VB + 1):
            for y in (Y0 + 8, Y0 + 9):
                x, z = fr.p(u, v)
                doc.remove(x, y, z)
    # door frame
    for v in (VB, VB - 1):
        fr.fill(doc, -6, -6, v, v, Y0, Y0 + 10, GO, sec)
        fr.fill(doc, 6, 6, v, v, Y0, Y0 + 10, GO, sec)
        fr.fill(doc, -6, 6, v, v, Y0 + 10, Y0 + 10, GO, sec)
    fr.fill(doc, -6, 6, VB, VB, Y0 + 11, Y0 + 12, RD, sec)

    # interior: team floor, ceiling, portal panel
    fr.fill(doc, -UB + 1, UB - 1, BACK + 1, VB - 1, Y0 + H, Y0 + H, SS, sec)
    for u in range(-UB + 1, UB):
        for v in range(BACK + 1, VB):
            if not (-5 <= u <= 5 and BACK < v < VB):
                fr.set(doc, u, v, Y0, "minecraft:polished_diorite",
                       sec, force=False)
    for u in range(-5, 6):
        for v in range(BACK + 1, VB):
            fr.set(doc, u, v, Y0, RD if (u + v) % 2 == 0 else BL, sec)
            fr.set(doc, u, v, Y0 + 10, SS, sec)
    fr.fill(doc, -6, 6, BACK + 1, BACK + 1, Y0 + 1, Y0 + 9, SB, sec)
    fr.fill(doc, -4, 4, BACK + 2, BACK + 2, Y0 + 1, Y0 + 8, GLASS_WH, sec)
    for u in (-5, 5):
        fr.fill(doc, u, u, BACK + 2, BACK + 2, Y0, Y0 + 9, GO, sec)
    fr.fill(doc, -5, 5, BACK + 2, BACK + 2, Y0 + 9, Y0 + 9, GO, sec)
    for u in (-3, 3):
        fr.set(doc, u, BACK + 1, Y0 + 9, SEA, sec)

    # crest on the parapet
    crest_panel(doc, fr, sec, 0, Y0 + 19, VB + 1)
    # white canopy over the door
    fr.fill(doc, -7, 7, VB + 1, VB + 3, Y0 + 13, Y0 + 13, W, sec)
    for u in (-7, 7):
        fr.fill(doc, u, u, VB + 3, VB + 3, Y0 + 10, Y0 + 12, W, sec)
    snow_cap(fr, doc, -7, 7, VB + 1, VB + 3, Y0 + 14, sec, 0.5)

    # roof + gold crenellation (the MCC crown edge)
    fr.fill(doc, -UB, UB, BACK, VB, Y0 + H + 3, Y0 + H + 3, LG, sec)
    for u in range(-UB, UB + 1, 3):
        for v in (BACK, VB):
            fr.set(doc, u, v, Y0 + H + 4, GO, sec)
    for v in range(BACK, VB + 1, 3):
        for u in (-UB, UB):
            fr.set(doc, u, v, Y0 + H + 4, GO, sec)
    snow_cap(fr, doc, -UB + 1, UB - 1, BACK + 1, VB - 1, Y0 + H + 4, sec, 0.4)

    # sign board on two posts above the roof
    for u in (-9, 9):
        fr.fill(doc, u, u, 0, 0, Y0 + H + 5, Y0 + H + 6, LG, sec)
    fr.fill(doc, -15, 15, 0, 0, Y0 + H + 7, Y0 + H + 20, DPB, sec)
    fr.fill(doc, -14, 14, 0, 0, Y0 + H + 8, Y0 + H + 19, W, sec)
    mini_centred(doc, fr, 0, Y0 + H + 14, "BATTLE", DPB, sec, 1, gap=2)
    mini_centred(doc, fr, 0, Y0 + H + 9, "BOX", DPB, sec, 1, gap=2)

    # side flagpole with the red / gold pennant
    fr.fill(doc, UB + 3, UB + 3, -2, -2, Y0, Y0 + H + 6, LG, sec)
    fr.fill(doc, UB + 3, UB + 3, -2, -2, Y0 + H + 7, Y0 + H + 7, GO, sec)
    for y, u0, u1 in ((Y0 + H + 4, UB + 4, UB + 7), (Y0 + H + 5, UB + 4, UB + 7),
                      (Y0 + H + 6, UB + 4, UB + 6)):
        fr.fill(doc, u0, u1, -2, -2, y, y, RD, sec)
    for y in (Y0 + H + 4, Y0 + H + 5, Y0 + H + 6):
        fr.set(doc, UB + 4, -2, y, GO, sec)

    # two lamps flanking the door on the apron
    for u in (-9, 9):
        fr.set(doc, u, VB + 2, Y0, SS, sec)
        fr.fill(doc, u, u, VB + 2, VB + 2, Y0 + 1, Y0 + 3, IRON, sec)
        fr.set(doc, u, VB + 2, Y0 + 4, LANTERN, sec)
        fr.set(doc, u, VB + 2, Y0 + 5, sl(1), sec)


# ==========================================================================
# 03 -- Dodgebolt: sunken-looking twin court with tiered wooden seating
#       (MainHub-DB.png).  Court floor is y=121 and the deck is raised to
#       y=124 because this agent may not cut below y=121.
# ==========================================================================
DB_COURT = (-16, 16, -14, 12)          # u0, u1, v0, v1


def build_dodgebolt(doc, rng):
    sec = "03_dodgebolt"
    fr = Frame(76, -80, "S")
    cu0, cu1, cv0, cv1 = DB_COURT

    # ---- court floor: white with three thick team rings per half ---------
    for u in range(cu0, cu1 + 1):
        for v in range(cv0, cv1 + 1):
            uc, team = (-8, BL) if u < 0 else (7, RD)
            d = max(abs(u - uc), abs(v + 1))
            if u == 0:
                blk = BK
            elif u in (cu0, cu1) or v in (cv0, cv1):
                blk = NAVY
            elif d <= 1:
                blk = BK
            elif d <= 4:
                blk = team
            elif 8 <= d <= 10:
                blk = team
            elif 13 <= d <= 14:
                blk = team
            else:
                blk = W
            fr.set(doc, u, v, Y0, blk, sec)

    # ---- raised wooden deck around the court ----------------------------
    for u in range(cu0 - 5, cu1 + 6):
        for v in range(cv0 - 4, cv1 + 5):
            if cu0 <= u <= cu1 and cv0 <= v <= cv1:
                continue
            if -5 <= u <= 5 and v >= cv1 + 1:
                continue                       # entrance stair opening
            for y in range(Y0, Y0 + 3):
                blk = SPR if (v // 2) % 2 else OAK
                fr.set(doc, u, v, y, blk, sec)
            fr.set(doc, u, v, Y0 + 3, OAK if (u + v) % 2 else SPR, sec)

    # ---- tiered seating on both long sides ------------------------------
    for side in (-1, 1):
        for tier in range(5):
            uu = side * (17 + tier)
            for v in range(cv0 - 3, cv1 + 4):
                y = Y0 + 4 + tier
                fr.set(doc, uu, v, y, SPR if (tier + v) % 2 else OAK, sec)
                for yy in range(Y0 + 4, y):
                    fr.set(doc, uu, v, yy, DKO if tier % 2 else SPR, sec)
                # bench back lip
                if tier < 4:
                    fr.set(doc, uu, v, y + 1, "minecraft:oak_slab[type=bottom]", sec)
        uu = side * 22
        for v in range(cv0 - 4, cv1 + 5):
            for y in range(Y0 + 4, Y0 + 11):
                fr.set(doc, uu, v, y, TEAL if (v + y) % 5 else CY, sec)
            fr.set(doc, uu, v, Y0 + 11, LG, sec)

    # ---- glass barriers -------------------------------------------------
    for u in range(cu0, cu1 + 1):
        for v in (cv0 - 1, cv1 + 1):
            fr.set(doc, u, v, Y0 + 1, PANE_LB, sec)
            fr.set(doc, u, v, Y0 + 2, PANE_LB, sec)
            fr.set(doc, u, v, Y0 + 3, LG, sec)
    for v in range(cv0, cv1 + 1):
        for u in (cu0 - 1, cu1 + 1):
            fr.set(doc, u, v, Y0 + 1, PANE_LB, sec)
            fr.set(doc, u, v, Y0 + 2, PANE_LB, sec)
            fr.set(doc, u, v, Y0 + 3, LG, sec)
    for side in (-1, 1):
        for v in range(cv0 - 3, cv1 + 4):
            fr.set(doc, side * 17, v, Y0 + 9, PANE_WH, sec)
            fr.set(doc, side * 17, v, Y0 + 10, PANE_WH, sec)

    # ---- stone pillars and white canopy beams ---------------------------
    for side in (-1, 1):
        for v in (-12, -4, 4, 11):
            u = side * 19
            fr.fill(doc, u - 1, u, v - 1, v, Y0 + 4, Y0 + 14, SB, sec)
            fr.fill(doc, u - 1, u, v - 1, v, Y0 + 15, Y0 + 15, LG, sec)
            fr.fill(doc, u - 1, u, v - 1, v, Y0 + 4, Y0 + 4, LG, sec)
            for k in range(4):
                fr.set(doc, u, v, Y0 + 5 + k * 2, MSB, sec)
        fr.fill(doc, side * 20, side * 18, -13, 12, Y0 + 16, Y0 + 17, W, sec)
        fr.fill(doc, side * 20, side * 20, -13, 12, Y0 + 15, Y0 + 15, LG, sec)
        snow_cap(fr, doc, min(side * 20, side * 18), max(side * 20, side * 18),
                 -13, 12, Y0 + 18, sec, 0.6)
    for v in (-15, 14):
        fr.fill(doc, -20, 20, v, v, Y0 + 16, Y0 + 17, W, sec)
        fr.fill(doc, -20, 20, v, v, Y0 + 15, Y0 + 15, LG, sec)

    # ---- north end wall with the gold flame crest ------------------------
    for u in range(-10, 11):
        for y in range(Y0 + 4, Y0 + 13):
            fr.set(doc, u, cv0 - 4, y, SST if (y % 3) else CSST, sec)
    crest_panel(doc, fr, sec, 0, Y0 + 6, cv0 - 3, panel=SST, flame=YL, frame=CSST)

    # ---- entrance: wide stair up to the deck ----------------------------
    for u in range(-5, 6):
        for v in range(cv1 + 1, cv1 + 6):
            step = Y0 + (v - cv1 - 1)
            fr.set(doc, u, v, step, SS, sec)
            for y in range(Y0, step):
                fr.set(doc, u, v, y, LG, sec)
    for u in (-6, 6):
        for v in range(cv1 + 1, cv1 + 6):
            fr.set(doc, u, v, Y0 + 3, W, sec)
            fr.set(doc, u, v, Y0 + 4, PANE_WH, sec)


# ==========================================================================
# 04 -- Parkour Warrior: stone plinth, teal frog, huge blue sign
#       (MainHub-PKW.png)
# ==========================================================================
FROG_BODY = ("minecraft:cyan_concrete", "minecraft:cyan_concrete",
             "minecraft:prismarine", CY, "minecraft:prismarine")
FROG_DARK = ("minecraft:cyan_terracotta", "minecraft:dark_prismarine", CY)
FROG_BELLY = (SST, "minecraft:bone_block", "minecraft:diorite")


def build_parkour_warrior(doc, rng):
    sec = "04_parkour_warrior"
    fr = Frame(24, -100, "S")

    # ---- stone plinth ----------------------------------------------------
    plat_top = Y0 + 5                     # walkable top of the plinth (126)
    for u in range(-15, 16):
        for v in range(-14, 11):
            e = (u / 15.0) ** 2 + (v / 12.5) ** 2
            if e > 1.0:
                continue
            for y in range(Y0, plat_top):
                if y == plat_top - 1:
                    blk = pick(u, y, v, 51, (SB, SB, MSB, "minecraft:cobbled_deepslate"))
                else:
                    blk = pick(u // 2, y, v // 2, 52,
                               ("minecraft:tuff", SB, "minecraft:cobbled_deepslate"))
                fr.set(doc, u, v, y, blk, sec)
    # steps at the front
    for u in range(-5, 6):
        for v in range(11, 14):
            step = plat_top - (v - 10)
            for y in range(Y0, step):
                fr.set(doc, u, v, y, SB if (u + y) % 2 else LG, sec)

    # ---- lava-lit mouth ------------------------------------------------
    for u in range(-4, 5):
        for v in range(2, 11):
            for y in range(Y0, plat_top - 1):
                x, z = fr.p(u, v)
                doc.remove(x, y, z)
    for u in range(-4, 5):
        for v in range(2, 11):
            fr.set(doc, u, v, Y0,
                   LAVA if (abs(u) <= 2 and 4 <= v <= 8) else
                   pick(u, 0, v, 53, (BK, "minecraft:deepslate_tiles")), sec)
    for u in range(-5, 6):
        for v in range(2, 11):
            if abs(u) == 5 or v == 2:
                for y in range(Y0, plat_top - 1):
                    fr.set(doc, u, v, y, "minecraft:polished_deepslate", sec)
    for u in range(-3, 4):
        fr.set(doc, u, 9, Y0, "minecraft:polished_deepslate", sec)
    for u in (-3, 3):
        for v in range(3, 10):
            fr.set(doc, u, v, Y0, "minecraft:polished_deepslate", sec)
    for u in (-4, 4):
        fr.set(doc, u, 4, plat_top - 2, SEA, sec)

    # ---- the frog --------------------------------------------------------
    BLOBS = (
        (0.0, -4.0, 6.8, 10.2, 8.4, 5.8),     # body
        (-10.0, -7.6, 4.2, 5.0, 5.8, 5.4),    # left haunch
        (10.0, -7.6, 4.2, 5.0, 5.8, 5.4),     # right haunch
        (0.0, 6.2, 9.8, 9.4, 5.8, 5.4),       # head
    )

    def frog_colour(u, v, y, ny):
        cream = (SST, SST, "minecraft:bone_block", "minecraft:diorite")
        if ny < -0.35 or (y <= 1 and h3(u, y, v, 64) < 0.7):
            return pick(u, y, v, 65, cream)
        if v > 5 and y < 6:
            return pick(u, y, v, 66, cream)      # chin / throat
        n = h3(u // 2, y // 2, v // 2, 55)
        if n < 0.16:
            return pick(u, y, v, 56, FROG_DARK)
        return pick(u, y, v, 57, FROG_BODY)

    for u in range(-14, 15):
        for v in range(-13, 14):
            for y in range(0, 16):
                best = 9.9
                for (bc_u, bc_v, bc_y, ru, rv, ry) in BLOBS:
                    d = ((u - bc_u) / ru) ** 2 + ((v - bc_v) / rv) ** 2 + ((y - bc_y) / ry) ** 2
                    if d < best:
                        best = d
                        ny = (y - bc_y) / ry
                if best > 1.0:
                    continue
                fr.set(doc, u, v, plat_top + y, frog_colour(u, v, y, ny), sec)

    # eyes: raised sockets on top of the head, black pupils at the front
    for su in (-5, 5):
        for du in range(-2, 3):
            for dv in range(-2, 3):
                for dy in range(0, 4):
                    if du * du + dv * dv + (dy - 1.5) ** 2 > 7.0:
                        continue
                    b = pick(u, dv, dy, 67, FROG_BODY) if dy < 2 else W
                    fr.set(doc, su + du, 8 + dv, plat_top + 14 + dy, b, sec)
        fr.fill(doc, su - 1, su + 1, 10, 10, plat_top + 15, plat_top + 17, BK, sec)
        fr.fill(doc, su - 2, su + 2, 9, 10, plat_top + 13, plat_top + 13,
                pick(u, 1, su, 68, FROG_DARK), sec)

    # front legs: two bent columns in front of the body, toes splayed
    for su in (-1, 1):
        uu = su * 7
        for y in range(0, 9):
            for du in range(-1, 2):
                for dv in range(0, 3):
                    fr.set(doc, uu + du, 9 + dv - (1 if y > 6 else 0), plat_top + y,
                           pick(uu + du, y, dv, 59, FROG_BODY), sec)
        for k in range(-3, 4):
            for dv in range(0, 4):
                fr.set(doc, uu + k, 11 + dv, plat_top,
                       pick(uu, k, dv, 60, FROG_BELLY), sec)
    # rear feet
    for su in (-1, 1):
        for k in range(-3, 4):
            for dv in range(0, 3):
                fr.set(doc, su * 8 + k, -12 - dv, plat_top,
                       pick(su, k, dv, 62, FROG_BELLY), sec)

    # ---- sign: two stone pillars + blue board ----------------------------
    for u in (-14, 14):
        fr.fill(doc, u - 1, u + 1, -15, -13, plat_top, plat_top + 40, SB, sec)
        fr.fill(doc, u - 1, u + 1, -15, -13, plat_top + 41, plat_top + 41, LG, sec)
    b = -12
    fr.fill(doc, -23, 23, b, b + 1, plat_top + 21, plat_top + 40, "minecraft:blue_concrete", sec)
    fr.fill(doc, -23, 23, b, b + 1, plat_top + 21, plat_top + 21, "minecraft:blue_terracotta", sec)
    fr.fill(doc, -23, 23, b, b + 1, plat_top + 40, plat_top + 40, "minecraft:blue_terracotta", sec)
    for u in (-23, 23):
        fr.fill(doc, u, u, b, b + 1, plat_top + 21, plat_top + 40, "minecraft:blue_terracotta", sec)
    fr.fill(doc, -24, 24, b, b + 1, plat_top + 41, plat_top + 41, LG, sec)
    # beige sandstone letters, one block proud of the board
    draw_text(doc, fr, -20, plat_top + 33, "PARKOUR", SST, sec, b + 2, gap=1)
    draw_text(doc, fr, -20, plat_top + 24, "WARRIOR", SST, sec, b + 2, gap=1)


# ==========================================================================
# 05 -- Parkour Tag portal building
#       No dedicated photo is supplied for this portal; it uses the portal
#       grammar that IS visible in MCCHub-S4.png (white shell, coloured
#       band, cyan glass, gold crown, crest panel, board on posts) with the
#       Parkour Tag accent colours.  Flagged in GAPS.md.
# ==========================================================================
def build_parkour_tag(doc, rng):
    sec = "05_parkour_tag"
    fr = Frame(-100, -50, "E")
    UB, VB = 14, 2
    BACK = -16
    H = 20

    fr.fill(doc, -UB, UB, BACK, VB, Y0, Y0 + 2, "minecraft:magenta_concrete", sec)
    fr.shell(doc, -UB, UB, BACK, VB, Y0 + 3, Y0 + H, W, sec)
    for u in (-UB, -UB + 1, UB - 1, UB):
        fr.fill(doc, u, u, BACK, VB, Y0, Y0 + H, LG, sec)
    fr.fill(doc, -UB, UB, BACK, VB, Y0 + H + 1, Y0 + H + 1, LG, sec)
    fr.fill(doc, -UB - 1, UB + 1, BACK - 1, VB + 1, Y0 + H + 2, Y0 + H + 2, W, sec)
    fr.fill(doc, -UB + 1, UB - 1, BACK + 1, VB - 1, Y0 + H, Y0 + H, SS, sec)

    # glass front
    glass_wall(doc, fr, sec, -12, -5, Y0 + 4, Y0 + 12, VB, GLASS_LB, every=2)
    glass_wall(doc, fr, sec, 5, 12, Y0 + 4, Y0 + 12, VB, GLASS_LB, every=2)
    fr.fill(doc, -12, 12, VB, VB, Y0 + 13, Y0 + 14, "minecraft:magenta_concrete", sec)
    fr.fill(doc, -12, 12, VB, VB, Y0 + 15, Y0 + 15, OR, sec)

    # entrance
    for u in range(-4, 5):
        for v in range(BACK + 1, VB + 1):
            for y in range(Y0, Y0 + 10):
                if abs(u) == 4 or y == Y0 + 9:
                    continue
                x, z = fr.p(u, v)
                doc.remove(x, y, z)
    for v in (VB, VB - 1):
        fr.fill(doc, -5, -5, v, v, Y0, Y0 + 9, OR, sec)
        fr.fill(doc, 5, 5, v, v, Y0, Y0 + 9, OR, sec)
        fr.fill(doc, -5, 5, v, v, Y0 + 9, Y0 + 9, OR, sec)
    for u in range(-4, 5):
        for v in range(BACK + 1, VB):
            fr.set(doc, u, v, Y0, "minecraft:magenta_concrete" if (u + v) % 3 else W, sec)
            fr.set(doc, u, v, Y0 + 8, SS, sec)
    fr.fill(doc, -3, 3, BACK + 2, BACK + 2, Y0 + 1, Y0 + 7, "minecraft:magenta_stained_glass", sec)
    for u in (-4, 4):
        fr.fill(doc, u, u, BACK + 2, BACK + 2, Y0, Y0 + 8, OR, sec)
    fr.fill(doc, -4, 4, BACK + 2, BACK + 2, Y0 + 8, Y0 + 8, OR, sec)

    crest_panel(doc, fr, sec, 0, Y0 + 16, VB + 1)

    # roof: stepped ziggurat cap instead of Battle Box's flat parapet
    for k in range(3):
        fr.fill(doc, -UB + k, UB - k, BACK + k, VB - k, Y0 + H + 3 + k, Y0 + H + 3 + k,
                W if k < 2 else LG, sec)
    fr.set(doc, 0, -6, Y0 + H + 6, GO, sec)
    fr.set(doc, 0, -6, Y0 + H + 7, GO, sec)
    snow_cap(fr, doc, -11, 11, -13, -1, Y0 + H + 4, sec, 0.35)

    # banner pole with a magenta pennant
    fr.fill(doc, UB + 3, UB + 3, -2, -2, Y0, Y0 + H + 9, LG, sec)
    for y in (Y0 + H + 7, Y0 + H + 8):
        fr.fill(doc, UB + 4, UB + 7, -2, -2, y, y, "minecraft:magenta_concrete", sec)
    fr.fill(doc, UB + 4, UB + 4, -2, -2, Y0 + H + 7, Y0 + H + 8, W, sec)

    # board on the roof
    for u in (-8, 8):
        fr.fill(doc, u, u, 1, 1, Y0 + H + 4, Y0 + H + 6, LG, sec)
    fr.fill(doc, -18, 18, 1, 1, Y0 + H + 7, Y0 + H + 18, DPB, sec)
    fr.fill(doc, -17, 17, 1, 1, Y0 + H + 8, Y0 + H + 17, W, sec)
    mini_centred(doc, fr, 0, Y0 + H + 13, "PARKOUR", "minecraft:magenta_concrete", sec, 2, gap=2)
    mini_centred(doc, fr, 0, Y0 + H + 9, "TAG", "minecraft:magenta_concrete", sec, 2, gap=2)


# ==========================================================================
# 06 -- Rocket Spleef Rush launch gantry  (MainHub-RSR.png)
# ==========================================================================
def _rocket(doc, fr, sec, u, v, y0, height, tilt=0):
    """Red / white / gold rocket with a magma engine."""
    for k in range(height):
        y = y0 + k
        off = tilt * k
        if k < height - 4:
            r = 2
        elif k < height - 2:
            r = 1
        else:
            r = 0
        for du in range(-r, r + 1):
            for dv in range(-r, r + 1):
                if r and abs(du) + abs(dv) > r + 1:
                    continue
                band = ((k + du + dv) // 2) % 4
                blk = W if band in (1, 2) else RD
                if k >= height - 3:
                    blk = W
                if k == 0:
                    blk = GO
                fr.set(doc, u + du + off, v + dv, y, blk, sec, force=False)
    for du, dv in ((-3, 0), (3, 0), (0, -3), (0, 3)):
        fr.set(doc, u + du + tilt * 2, v + dv, y0 + 2, GO, sec, force=False)
        fr.set(doc, u + du + tilt * 3, v + dv, y0 + 3, RD, sec, force=False)
    # engine + flame
    fr.set(doc, u, v, y0 - 1, "minecraft:magma_block", sec, force=False)
    fr.set(doc, u, v, y0 - 2, OR, sec, force=False)


def build_rocket_spleef(doc, rng):
    sec = "06_rocket_spleef"
    fr = Frame(110, -40, "W")
    # pillars
    for su in (-1, 1):
        u0 = su * 7
        u1 = su * 13
        ulo, uhi = min(u0, u1), max(u0, u1)
        for u in range(ulo, uhi + 1):
            for v in range(-12, -5):
                for y in range(Y0, Y0 + 25):
                    edge = (u in (ulo, uhi)) or (v in (-12, -6))
                    blk = pick(u // 2, y // 2, v // 2, 71, (GY, LG, "minecraft:gray_concrete"))
                    if y % 6 == 0:
                        blk = BK
                    band = y % 9
                    if edge and band in (0, 1, 2):
                        blk = YL if (((u + v + y) // 1) % 4) < 2 else BK
                    if not edge and y % 6 != 0:
                        continue
                    fr.set(doc, u, v, y, blk, sec)
        # yellow / black hazard collars
        for u in range(ulo, uhi + 1):
            for v in range(-12, -5):
                if (u in (ulo, uhi)) or (v in (-12, -6)):
                    for yy in (3, 4, 12, 13, 21, 22):
                        fr.set(doc, u, v, Y0 + yy,
                               YL if ((u + v + yy) % 4) < 2 else BK, sec)
        # cap
        fr.fill(doc, ulo - 1, uhi + 1, -13, -5, Y0 + 25, Y0 + 25, LG, sec)
        fr.fill(doc, ulo, uhi, -12, -6, Y0 + 26, Y0 + 26, GY, sec)

    # deck / iron truss on top
    fr.fill(doc, -15, 15, -14, -3, Y0 + 26, Y0 + 26, "minecraft:smooth_stone", sec)
    fr.fill(doc, -15, 15, -14, -3, Y0 + 27, Y0 + 27, GY, sec)
    for u in range(-15, 16):
        for v in (-14, -3):
            fr.set(doc, u, v, Y0 + 28, IBARS, sec)
    for u in range(-15, 16, 3):
        fr.set(doc, u, -3, Y0 + 27, W, sec)
        fr.set(doc, u, -3, Y0 + 26, LG, sec)
    # white cross braces on the front fascia (as in the reference)
    for cu in (-11, 0, 11):
        for k in range(-1, 2):
            fr.set(doc, cu + k, -3, Y0 + 25, W, sec)
        fr.set(doc, cu, -3, Y0 + 26, W, sec)

    # wooden truss arch under the sign
    for k in range(0, 7):
        fr.set(doc, -6 + k, -9, Y0 + 1 + k, "minecraft:dark_oak_log", sec)
        fr.set(doc, 6 - k, -9, Y0 + 1 + k, "minecraft:dark_oak_log", sec)
    fr.fill(doc, -1, 1, -10, -8, Y0 + 9, Y0 + 9, "minecraft:dark_oak_log", sec)
    for u in (-6, -3, 0, 3, 6):
        fr.fill(doc, u, u, -10, -8, Y0 + 8, Y0 + 8, "minecraft:dark_oak_log", sec)

    # the big navy board
    board_v = -3
    fr.fill(doc, -24, 24, board_v, board_v + 1, Y0 + 9, Y0 + 26, "minecraft:blue_concrete", sec)
    fr.fill(doc, -24, 24, board_v, board_v + 1, Y0 + 9, Y0 + 9, "minecraft:blue_terracotta", sec)
    fr.fill(doc, -24, 24, board_v, board_v + 1, Y0 + 26, Y0 + 26, "minecraft:blue_terracotta", sec)
    for u in (-24, 24):
        fr.fill(doc, u, u, board_v, board_v + 1, Y0 + 9, Y0 + 26, "minecraft:blue_terracotta", sec)
    # letters, 5x7, 1 proud, centred on the 49-wide board
    draw_text(doc, fr, -17, Y0 + 18, "ROCKET", YL, sec, board_v + 2, gap=1)
    draw_text(doc, fr, -17, Y0 + 10, "SPLEEF", YL, sec, board_v + 2, gap=1)

    # rockets on the pillar caps
    _rocket(doc, fr, sec, -10, -9, Y0 + 27, 16, tilt=0)
    _rocket(doc, fr, sec, 9, -9, Y0 + 27, 20, tilt=0)
    # smoke puffs above the tall rocket (static: reference shows drifting smoke)
    for i, (dy, sc) in enumerate(((2, 1), (5, 2), (8, 1), (11, 1), (14, 1))):
        dx = int(round(math.sin(i * 1.3) * 1.5))
        for a in range(-sc, sc + 1):
            for b in range(-sc, sc + 1):
                if a * a + b * b > sc * sc:
                    continue
                fr.set(doc, 9 + dx + a, -9 + b, Y0 + 48 + dy,
                       pick(a, b, i, 72, (W, "minecraft:light_gray_concrete")),
                       sec, force=False)

    # launch walkway from the plaza, under the arch
    for v in range(-8, 23):
        for u in range(-4, 5):
            fr.set(doc, u, v, Y0, W if (u + v) % 5 else LG, sec, force=False)
    for v in range(-8, 23):
        for u in (-5, 5):
            fr.set(doc, u, v, Y0, RD if v % 2 else W, sec, force=False)
            fr.set(doc, u, v, Y0 + 1, RD if v % 2 else W, sec, force=False)
    # lamp posts at the mouth of the walkway
    for u in (-8, 8):
        fr.fill(doc, u, u, 18, 18, Y0, Y0 + 3, "minecraft:polished_blackstone", sec)
        fr.set(doc, u, 18, Y0 + 4, LANTERN, sec)
        fr.set(doc, u, 18, Y0 + 5, sl(1), sec)


# ==========================================================================
# 07 -- Aqua building and water ride (east at 80,120,0)
#       White / blue pavilion, glazed front, "AQUA" board, raised pool and
#       channel with a boat.  Reference: MCCHubSeason1.png east side
#       ("AQUA JUNGLE" board on stilts) plus the blue pools of MCCNewHub.jpg.
# ==========================================================================
def build_aqua(doc, rng):
    sec = "07_aqua"
    fr = Frame(80, 0, "W")
    UB, VB, BACK = 16, 6, -18
    H = 24

    # ---- shell -----------------------------------------------------------
    fr.fill(doc, -UB, UB, BACK, VB, Y0, Y0 + 2, BL, sec)          # blue base band
    fr.shell(doc, -UB, UB, BACK, VB, Y0 + 3, Y0 + H, W, sec)
    for u in (-UB, -UB + 1, UB - 1, UB):
        fr.fill(doc, u, u, BACK, VB, Y0, Y0 + H, LG, sec)
    for v in range(BACK, VB + 1, 5):
        fr.fill(doc, -UB, -UB, v, v, Y0 + 3, Y0 + H, LG, sec)
        fr.fill(doc, UB, UB, v, v, Y0 + 3, Y0 + H, LG, sec)
    fr.fill(doc, -UB, UB, BACK, VB, Y0 + H + 1, Y0 + H + 1, LG, sec)
    fr.fill(doc, -UB - 1, UB + 1, BACK - 1, VB + 1, Y0 + H + 2, Y0 + H + 2, W, sec)
    fr.fill(doc, -UB + 1, UB - 1, BACK + 1, VB - 1, Y0 + H, Y0 + H, SS, sec)
    for u in range(-UB + 1, UB):
        for v in range(BACK + 1, VB):
            if not (-4 <= u <= 4 and BACK < v < VB):
                fr.set(doc, u, v, Y0, LB if (u + v) % 3 else W, sec, force=False)

    # ---- glazed two-storey front ----------------------------------------
    glass_wall(doc, fr, sec, -15, -5, Y0 + 4, Y0 + 16, VB, GLASS_LB, every=2)
    glass_wall(doc, fr, sec, 5, 15, Y0 + 4, Y0 + 16, VB, GLASS_LB, every=2)
    # side windows so the pavilion is not blank from the plaza flanks
    for v0 in (-15, -10, -5):
        for u in (-UB, UB):
            for y in range(Y0 + 5, Y0 + 14):
                for v in range(v0, v0 + 2):
                    fr.set(doc, u, v, y, GLASS_LB if (len(str(y)) and (y % 3)) else LG, sec)
    fr.fill(doc, -16, 16, VB, VB, Y0 + 3, Y0 + 3, CY, sec)
    fr.fill(doc, -16, 16, VB, VB, Y0 + 17, Y0 + 17, LB, sec)
    fr.fill(doc, -16, 16, VB, VB, Y0 + 18, Y0 + 18, GO, sec)

    # ---- entrance --------------------------------------------------------
    for u in range(-4, 5):
        for v in range(BACK + 2, VB + 1):
            for y in range(Y0, Y0 + 11):
                if abs(u) == 4 or y == Y0 + 10:
                    continue
                x, z = fr.p(u, v)
                doc.remove(x, y, z)
    for v in (VB, VB - 1):
        fr.fill(doc, -5, -5, v, v, Y0, Y0 + 10, LB, sec)
        fr.fill(doc, 5, 5, v, v, Y0, Y0 + 10, LB, sec)
        fr.fill(doc, -5, 5, v, v, Y0 + 10, Y0 + 10, LB, sec)
    for u in range(-4, 5):
        for v in range(BACK + 2, VB):
            fr.set(doc, u, v, Y0, LB if (u + v) % 2 else W, sec)
            fr.set(doc, u, v, Y0 + 9, SS, sec)
    fr.fill(doc, -3, 3, BACK + 3, BACK + 3, Y0 + 1, Y0 + 8, GLASS_LB, sec)
    for u in (-4, 4):
        fr.fill(doc, u, u, BACK + 3, BACK + 3, Y0, Y0 + 9, GO, sec)
    fr.fill(doc, -4, 4, BACK + 3, BACK + 3, Y0 + 9, Y0 + 9, GO, sec)

    # ---- aquarium tank behind the entrance (glass box, water sealed in) --
    for u in range(-14, 15):
        for v in range(BACK + 1, BACK + 4):
            fr.set(doc, u, v, Y0 + 11, GLASS_LB, sec)
            fr.set(doc, u, v, Y0 + 14, GLASS_LB, sec)
            fr.set(doc, u, v, Y0 + 12, WATER, sec)
            fr.set(doc, u, v, Y0 + 13, WATER, sec)
    for y in range(Y0 + 11, Y0 + 15):
        for v in range(BACK + 1, BACK + 4):
            fr.set(doc, -15, v, y, GLASS_LB, sec)
            fr.set(doc, 15, v, y, GLASS_LB, sec)
        for u in range(-15, 16):
            fr.set(doc, u, BACK + 4, y, GLASS_LB, sec)

    # ---- balcony across the front ---------------------------------------
    fr.fill(doc, -12, 12, VB + 1, VB + 3, Y0 + 19, Y0 + 19, W, sec)
    for u in (-11, -4, 4, 11):
        fr.fill(doc, u, u, VB + 3, VB + 3, Y0, Y0 + 18, LG, sec)
    for u in range(-12, 13):
        fr.set(doc, u, VB + 3, Y0 + 20, PANE_WH, sec)
    for u in (-12, 12):
        fr.fill(doc, u, u, VB + 1, VB + 3, Y0 + 20, Y0 + 20, W, sec)
    snow_cap(fr, doc, -12, 12, VB + 1, VB + 3, Y0 + 20, sec, 0.45)

    # ---- side elevations: windows and a water stair ----------------------
    for u in (-UB, UB):
        for v0 in (-15, -10, -5):
            for y in range(Y0 + 6, Y0 + 13):
                for dv in range(0, 2):
                    fr.set(doc, u, v0 + dv, y,
                           GLASS_LB if (y % 3) else LG, sec)
    for v in range(VB + 1, 13):
        for u in (-UB, UB):
            fr.set(doc, u, v, Y0, LG, sec)

    # ---- roof + crest ----------------------------------------------------
    for u in range(-UB, UB + 1, 4):
        fr.set(doc, u, VB, Y0 + H + 3, GO, sec)
    crest_panel(doc, fr, sec, 0, Y0 + H + 3, VB + 2)

    # ---- rooftop plant: skylight, vent stack and rail -------------------
    for u in range(-8, 9, 4):
        for v in range(BACK + 4, -4, 4):
            fr.fill(doc, u, u + 1, v, v + 1, Y0 + H + 3, Y0 + H + 3, GLASS_CY, sec)
    fr.fill(doc, 8, 10, BACK + 3, BACK + 5, Y0 + H + 3, Y0 + H + 5, LG, sec)
    fr.fill(doc, 9, 9, BACK + 4, BACK + 4, Y0 + H + 6, Y0 + H + 7, IRON, sec)
    for u in range(-UB, UB + 1, 2):
        fr.set(doc, u, BACK + 1, Y0 + H + 3, IRON, sec)

    # ---- AQUA board + MCC board above it --------------------------------
    for u in (-6, 6):
        fr.fill(doc, u, u, 2, 2, Y0 + H + 4, Y0 + H + 6, LG, sec)
    fr.fill(doc, -9, 9, 2, 2, Y0 + H + 7, Y0 + H + 14, "minecraft:blue_concrete", sec)
    fr.fill(doc, -9, 9, 2, 2, Y0 + H + 7, Y0 + H + 7, "minecraft:blue_terracotta", sec)
    fr.fill(doc, -9, 9, 2, 2, Y0 + H + 14, Y0 + H + 14, "minecraft:blue_terracotta", sec)
    mini_centred(doc, fr, 0, Y0 + H + 9, "AQUA", W, sec, 3, gap=1)
    for u in (-4, 4):
        fr.fill(doc, u, u, 2, 2, Y0 + H + 15, Y0 + H + 16, LG, sec)
    fr.fill(doc, -7, 7, 2, 2, Y0 + H + 17, Y0 + H + 23, RD, sec)
    mini_centred(doc, fr, 0, Y0 + H + 19, "MCC", W, sec, 3, gap=1)
    for u, y in ((-3, 24), (-2, 24), (-1, 25), (0, 25), (1, 25), (2, 24), (3, 24),
                 (-2, 26), (-1, 26), (0, 26), (1, 26), (2, 26), (0, 27)):
        fr.set(doc, u, 2, Y0 + H + y, GO, sec)

    # ---- water ride: channel then pool -----------------------------------
    for v in range(VB + 1, 13):
        for u in (-3, 3):
            fr.fill(doc, u, u, v, v, Y0, Y0 + 2, W, sec)
        for u in range(-2, 3):
            fr.set(doc, u, v, Y0, WATER, sec)
            fr.set(doc, u, v, Y0 + 1, WATER, sec)
    PU0, PU1, PV0, PV1 = -10, 10, 12, 32
    for u in range(PU0 - 1, PU1 + 2):
        for v in range(PV0, PV1 + 1):
            edge = u in (PU0 - 1, PU1 + 1) or v in (PV0, PV1)
            if edge:
                for y in range(Y0, Y0 + 3):
                    fr.set(doc, u, v, y, W if y < Y0 + 2 else LG, sec)
            else:
                fr.set(doc, u, v, Y0, WATER, sec)
                fr.set(doc, u, v, Y0 + 1, WATER, sec)
    for u in range(PU0, PU1 + 1):
        for v in range(PV0 + 1, PV1):
            if u in (PU0, PU1) or v in (PV0 + 1, PV1 - 1):
                fr.set(doc, u, v, Y0, CY, sec)
    for u in range(-2, 3):
        fr.set(doc, u, 12, Y0 + 2, W, sec)
        fr.set(doc, u, 13, Y0 + 1, WATER, sec)
    for u in range(PU0 + 2, PU1 - 1, 4):
        fr.set(doc, u, PV1, Y0, SEA, sec)
        fr.set(doc, u, PV0, Y0, SEA, sec)
    # diving board
    fr.fill(doc, -7, -4, 12, 14, Y0 + 4, Y0 + 4, W, sec)
    fr.fill(doc, -7, -7, 12, 14, Y0 + 1, Y0 + 3, LG, sec)

    # ---- boats on the pool ----------------------------------------------
    for (bu, bv, rot) in ((3, 20, 0), (-6, 27, 1)):
        for du in range(-3, 4):
            for dv in range(-1, 2):
                if abs(du) == 3 and dv != 0:
                    continue
                uu, vv = (bu + du, bv + dv) if not rot else (bu + dv, bv + du)
                fr.set(doc, uu, vv, Y0 + 2, OAK, sec)
                fr.set(doc, uu, vv, Y0 + 3, "minecraft:oak_slab[type=bottom]", sec)
        ex, ey = (bu - 4, bv) if not rot else (bu, bv - 4)
        fr.set(doc, ex, ey, Y0 + 2, W, sec)
        fr.set(doc, ex, ey, Y0 + 3, OAK, sec)

    # ---- deck chairs and umbrellas on the pool deck ---------------------
    for (cu, cv, cc) in ((-14, 18, RD), (14, 26, CY)):
        for du in range(0, 3):
            fr.set(doc, cu + du, cv, Y0 + 1, W, sec)
        fr.fill(doc, cu, cu + 2, cv - 1, cv - 1, Y0 + 2, Y0 + 3, cc, sec)


# ==========================================================================
# Placement guards for props
# ==========================================================================
RES_OTHER = [
    (-122, -25, -67, 25),      # THE CHAMPS (BRIEF: 55x40 at -95,120,0)
    (-95, 20, -45, 70),        # glass Hall of Fame (r=20 at -70,120,45)
    (56, 36, 94, 74),          # snow globe (r=14 at 75,125,55)
]
SOUTH_BAND_Z = 76              # V4-C team columns / stage plots live south of this
AXIS_HALF = 14                 # V4-A red-carpet axis, kept clear


def blocked(x, z, m=0.0):
    """True if a ground prop may not be placed at (x,z)."""
    r = math.hypot(x, z)
    if r < 36 + m:
        return True
    if r > 128 - m:
        return True
    for (x0, z0, x1, z1) in RES_OTHER:
        if x0 - m <= x <= x1 + m and z0 - m <= z <= z1 + m:
            return True
    for (x0, z0, x1, z1) in PLOTS.values():
        if x0 - m <= x <= x1 + m and z0 - m <= z <= z1 + m:
            return True
    if z > SOUTH_BAND_Z - m:
        return True
    if abs(x) < AXIS_HALF + m and z > 30:
        return True
    return False


def occupied_near(spots, x, z, d):
    return any(math.hypot(x - a, z - b) < d for a, b in spots)


# ==========================================================================
# 10 -- market stalls
# ==========================================================================
def build_stall(doc, cx, cz, face, pal, rng):
    """Market tent: 9 x 7 footprint, striped awning, counter, goods."""
    sec = "10_market"
    fr = Frame(cx, cz, face)
    aw1, aw2, goods = pal

    # deck + four posts
    fr.fill(doc, -4, 4, -3, 3, Y0, Y0, SPR, sec)
    for u in (-4, 4):
        for v in (-3, 3):
            fr.fill(doc, u, u, v, v, Y0 + 1, Y0 + 4, "minecraft:spruce_log", sec)
    # back wall with shelves
    fr.fill(doc, -4, 4, -3, -3, Y0 + 1, Y0 + 4, DKO, sec)
    for u in (-3, -1, 1, 3):
        fr.set(doc, u, -2, Y0 + 2, "minecraft:barrel[facing=south]", sec)
        fr.set(doc, u, -2, Y0 + 3, "minecraft:barrel[facing=south]", sec)
    # counter
    fr.fill(doc, -4, 4, 2, 2, Y0 + 1, Y0 + 2, SPR, sec)
    fr.fill(doc, -4, 4, 2, 2, Y0 + 3, Y0 + 3, "minecraft:spruce_slab[type=bottom]", sec)
    # awning: chequered, dips one at the front
    for u in range(-4, 5):
        for v in range(-3, 4):
            yy = Y0 + 5 if v < 2 else Y0 + 4
            fr.set(doc, u, v, yy, aw1 if (u + v) % 2 == 0 else aw2, sec)
    fr.fill(doc, -5, 5, -4, 4, Y0 + 5, Y0 + 5, aw2, sec)
    fr.set(doc, 0, 0, Y0 + 4, LANTERN, sec)
    # goods on the counter
    for k, u in enumerate((-3, -1, 1, 3)):
        fr.set(doc, u, 2, Y0 + 4, goods[k % len(goods)], sec)
    # sign plank over the counter
    fr.fill(doc, -3, 3, 3, 3, Y0 + 4, Y0 + 4, DKO, sec)
    # snow on the awning
    snow_cap(fr, doc, -5, 5, -4, 4, Y0 + 6, sec, 0.45)
    # crate + candy cane beside the stall
    off = {"S": (6, 4), "N": (-6, -4), "W": (-4, 6), "E": (4, -6)}[face]
    if not blocked(cx + off[0], cz + off[1], 0.0):
        build_cane(doc, cx + off[0], cz + off[1], 5)


def build_market(doc, rng):
    pals = [
        (RD, W, ("minecraft:cake[bites=0]", "minecraft:pumpkin", "minecraft:melon")),
        ("minecraft:green_concrete", W, ("minecraft:hay_block[axis=y]", "minecraft:barrel[facing=up]", "minecraft:sweet_berry_bush[age=3]")),
        (RD, GO, ("minecraft:gold_block", "minecraft:chiseled_bookshelf[facing=south]", "minecraft:lantern[hanging=false]")),
        (CY, W, ("minecraft:ice", "minecraft:barrel[facing=up]", "minecraft:pumpkin")),
        ("minecraft:magenta_concrete", W, ("minecraft:cake[bites=0]", "minecraft:melon", "minecraft:hay_block[axis=y]")),
        ("minecraft:lime_concrete", W, ("minecraft:sweet_berry_bush[age=3]", "minecraft:pumpkin", "minecraft:barrel[facing=up]")),
    ]
    spots = []
    cands = []
    for k in range(72):
        b = k * 5.0
        for r in (60.0, 70.0, 80.0):
            x = round(r * math.cos(math.radians(b)))
            z = round(r * math.sin(math.radians(b)))
            if blocked(x, z, 8.0):
                continue
            if occupied_near(spots, x, z, 16.0):
                continue
            cands.append((x, z, b))
    rng.shuffle(cands)
    for (x, z, b) in cands:
        if len(spots) >= 8:
            break
        if occupied_near(spots, x, z, 16.0):
            continue
        face = "S" if abs(z) > abs(x) else ("E" if x < 0 else "W")
        if z < 0:
            face = "S" if abs(x) < abs(z) * 0.6 else ("W" if x > 0 else "E")
        else:
            face = "N"
        build_stall(doc, x, z, face, pals[len(spots) % len(pals)], rng)
        spots.append((x, z))
    return spots


# ==========================================================================
# 11 -- trees
# ==========================================================================
def build_palm(doc, x, z, h, rng, sec="11_trees"):
    lean = rng.choice((-1, 1))
    for i in range(h):
        px = x + round(lean * (i / h) ** 2 * 2)
        doc.set(px, Y0 + i, z, "minecraft:jungle_log[axis=y]" if i % 3 else "minecraft:stripped_jungle_log[axis=y]", sec)
    tx, ty, tz = x + round(lean * 2), Y0 + h, z
    for d, drops in ((0, 0), (1, 1), (2, 1), (3, 2)):
        for aa in range(-3, 4):
            for bb in range(-3, 4):
                r = math.hypot(aa, bb)
                if r > 3.2 - d * 0.15:
                    continue
                if h3(tx + aa, ty, tz + bb, 81) < 0.12:
                    continue
                doc.set(tx + aa, ty - drops, tz + bb, LEAF_JUNGLE, sec, force=False)
    # drooping fronds
    for ang in range(0, 360, 45):
        ca, sa = math.cos(math.radians(ang)), math.sin(math.radians(ang))
        for step in range(2, 5):
            px = int(round(tx + ca * step))
            pz = int(round(tz + sa * step))
            doc.set(px, ty - 2, pz, LEAF_JUNGLE, sec, force=False)
    return True


def build_spruce(doc, x, z, h, rng, decorated=False, sec="11_trees"):
    for i in range(h):
        doc.set(x, Y0 + i, z, LOGS, sec)
    top = Y0 + h
    for i in range(h - 1):
        y = Y0 + 2 + i
        r = max(0, int((h - 1 - i) * 0.42))
        if r == 0:
            continue
        for aa in range(-r, r + 1):
            for bb in range(-r, r + 1):
                if abs(aa) + abs(bb) > r + 1:
                    continue
                if aa == 0 and bb == 0:
                    continue
                doc.set(x + aa, y, z + bb, LEAF_SPRUCE, sec)
                if abs(aa) + abs(bb) >= r and h3(x + aa, y, z + bb, 82) < 0.5:
                    doc.set(x + aa, y + 1, z + bb, sl(1), sec, force=False)
    doc.set(x, top, z, LEAF_SPRUCE, sec)
    doc.set(x, top + 1, z, LEAF_SPRUCE, sec)
    doc.set(x, top + 2, z, sl(1), sec, force=False)
    if decorated:
        for aa in range(-3, 4):
            for bb in range(-3, 4):
                for y in range(Y0 + 2, top):
                    if (abs(aa) + abs(bb)) % 3 == 1 and h3(x + aa, y, z + bb, 83) < 0.55:
                        doc.set(x + aa, y, z + bb, RD, sec, force=False)
        doc.set(x, top + 2, z, GO, sec)


def build_trees(doc, rng):
    spots = []
    # palms first: the reference beaches are full of them
    palm_ang = [168, 190, 208, 232, 252, 288, 306, 330, 350, 8, 26, 44, 62]
    for b in palm_ang:
        for r in (64.0, 80.0, 96.0, 112.0):
            x = round(r * math.cos(math.radians(b)))
            z = round(r * math.sin(math.radians(b)))
            if blocked(x, z, 9.0):
                continue
            if occupied_near(spots, x, z, 9.0):
                continue
            build_palm(doc, x, z, rng.choice((7, 8, 9, 10)), rng)
            spots.append((x, z))
            break
    # snowy spruces in the gaps
    for k in range(72):
        b = k * 5.0 + 2.0
        for r in (66.0, 80.0, 94.0, 108.0, 120.0):
            x = round(r * math.cos(math.radians(b)))
            z = round(r * math.sin(math.radians(b)))
            if blocked(x, z, 9.0):
                continue
            if occupied_near(spots, x, z, 10.0):
                continue
            build_spruce(doc, x, z, rng.choice((6, 7, 8, 9, 10)), rng,
                         decorated=(h3(x, 0, z, 84) < 0.25))
            spots.append((x, z))
    return spots


# ==========================================================================
# 12 -- lamps and candy canes
# ==========================================================================
def build_lamp(doc, x, z, sec="12_lamps_canes"):
    doc.set(x, Y0, z, SS, sec)
    for y in (Y0 + 1, Y0 + 2, Y0 + 3):
        doc.set(x, y, z, "minecraft:dark_oak_fence", sec)
    doc.set(x, Y0 + 4, z, LANTERN, sec)
    doc.set(x, Y0 + 5, z, sl(1), sec)


def build_cane(doc, x, z, h=5, sec="12_lamps_canes"):
    for i in range(h):
        blk = RD if i % 2 == 0 else W
        doc.set(x, Y0 + i + 1, z, blk, sec, force=False)
    doc.set(x + 1, Y0 + h + 1, z, RD, sec, force=False)
    doc.set(x + 2, Y0 + h + 1, z, RD, sec, force=False)
    doc.set(x + 2, Y0 + h, z, W, sec, force=False)


FACING = {
    "ace_race": "S", "battle_box": "S", "dodgebolt": "S",
    "parkour_warrior": "S", "parkour_tag": "E", "rocket_spleef": "W",
    "aqua": "W",
}
HALF = {
    "ace_race": (40, 22), "battle_box": (17, 15), "dodgebolt": (22, 19),
    "parkour_warrior": (22, 18), "parkour_tag": (18, 18),
    "rocket_spleef": (23, 18), "aqua": (29, 21),
}


def front_pt(name, u, v):
    """World point at local (u,v) of a plot, using its plot-centre frame."""
    x0, z0, x1, z1 = PLOTS[name]
    fr = Frame((x0 + x1) // 2, (z0 + z1) // 2, FACING[name])
    return fr.p(u, v)


def build_lamps_and_canes(doc, rng):
    sec = "12_lamps_canes"
    # two lamps flanking every forecourt, on the front edge of the plot
    for name in PLOTS:
        hu, hv = HALF[name]
        for su in (-1, 1):
            x, z = front_pt(name, su * (hu - 6), hv - 1)
            if not blocked(x, z, -6.0):
                build_lamp(doc, x, z, sec)
    # candy canes in pairs at the portal thresholds
    for name in PLOTS:
        hu, hv = HALF[name]
        for su in (-1, 1):
            x, z = front_pt(name, su * (hu - 9), hv - 5)
            build_cane(doc, x, z, 5, sec)
    # lamps along the plaza rim, in the free arcs
    for b in range(0, 360, 15):
        r = 44.0
        x = round(r * math.cos(math.radians(b)))
        z = round(r * math.sin(math.radians(b)))
        if blocked(x, z, -2.0):
            continue
        build_lamp(doc, x, z, sec)


# ==========================================================================
# 13 -- presents, snowmen, snow cover
# ==========================================================================
def build_present(doc, x, z, size, rng, sec="13_presents"):
    cols = (RD, "minecraft:green_concrete", CY, "minecraft:magenta_concrete",
            BL, GO, "minecraft:purple_concrete")
    body = cols[int(h3(x, 0, z, 91) * len(cols)) % len(cols)]
    rib = W if body != W else GO
    for dx in range(size):
        for dz in range(size):
            for y in range(size):
                doc.set(x + dx, Y0 + y, z + dz,
                        rib if (dx == size // 2 or dz == size // 2) else body, sec, force=False)
    # bow
    doc.set(x + size // 2, Y0 + size, z + size // 2, rib, sec, force=False)
    if size > 1:
        doc.set(x, Y0 + size, z, rib, sec, force=False)
        doc.set(x + size - 1, Y0 + size, z + size - 1, rib, sec, force=False)
    # a little snow on top
    if h3(x, 1, z, 92) < 0.6:
        doc.set(x + size // 2, Y0 + size + 1, z + size // 2, sl(1), sec, force=False)


def build_snowman(doc, x, z, scarf, hat=True, sec="13_presents"):
    doc.set(x, Y0, z, SNOW, sec, force=False)
    doc.set(x, Y0 + 1, z, SNOW, sec, force=False)
    doc.set(x, Y0 + 2, z, SNOW, sec, force=False)
    doc.set(x, Y0 + 1, z, SNOW, sec)
    for k in ((x, Y0 + 1, z + 1), (x, Y0 + 2, z + 1)):
        doc.set(k[0], k[1], k[2], scarf, sec, force=False)
    doc.set(x, Y0 + 3, z, SNOW, sec, force=False)
    doc.set(x, Y0 + 4, z, SNOW, sec, force=False)
    doc.set(x, Y0 + 4, z + 1, OR, sec, force=False)
    doc.set(x, Y0 + 4, z - 1, BK, sec, force=False)
    if hat:
        doc.set(x, Y0 + 5, z, BK, sec, force=False)
        doc.set(x, Y0 + 5, z + 1, BK, sec, force=False)
        doc.set(x, Y0 + 5, z - 1, BK, sec, force=False)
        doc.set(x - 1, Y0 + 5, z, BK, sec, force=False)
        doc.set(x + 1, Y0 + 5, z, BK, sec, force=False)
        doc.set(x, Y0 + 6, z, BK, sec, force=False)


def build_presents(doc, rng):
    sec = "13_presents"
    # clusters around the market ring
    spots = []
    for k in range(60):
        b = k * 6.0 + 3.0
        for r in (55.0, 66.0, 78.0, 90.0, 104.0):
            x = round(r * math.cos(math.radians(b)))
            z = round(r * math.sin(math.radians(b)))
            if blocked(x, z, 3.0):
                continue
            if occupied_near(spots, x, z, 7.0):
                continue
            if h3(x, 2, z, 93) > 0.45:
                continue
            build_present(doc, x, z, 1 + int(h3(x, 3, z, 94) * 2), rng)
            spots.append((x, z))
            break
    # presents on the forecourts of the portals (the reference thresholds)
    for name in PLOTS:
        hu, hv = HALF[name]
        for su in (-1, 1):
            x, z = front_pt(name, su * (hu - 12), hv - 6)
            if blocked(x, z, -4.0):
                continue
            build_present(doc, x, z, 2, rng)
    # snowmen on the snowy arcs (festive reference shows them everywhere)
    for (x, z, sc, hat) in ((-56, -70, CY, True), (-62, -58, BL, True),
                            (-8, -78, "minecraft:purple_concrete", True),
                            (46, -64, "minecraft:teal" if False else CY, True),
                            (96, 26, RD, True), (-100, 8, "minecraft:lime_concrete", False)):
        if blocked(x, z, 1.0):
            continue
        build_snowman(doc, x, z, sc, hat)


# ==========================================================================
# 14 -- boats on the water ring
# ==========================================================================
def build_rowboat(doc, x, z, heading, sec="14_boats"):
    """Small open boat, 7 long, floating with its hull top at y=123."""
    ca, sa = math.cos(math.radians(heading)), math.sin(math.radians(heading))
    def P(a, b):
        return (int(round(x + ca * a - sa * b)), int(round(z + sa * a + ca * b)))
    for a in range(-3, 4):
        for b in (-1, 0, 1):
            px, pz = P(a, b)
            doc.set(px, Y0 + 1, pz, OAK, sec, force=False)
            doc.set(px, Y0 + 2, pz, OAK, sec, force=False)
    for a in range(-3, 4):
        for b in (-2, 2):
            px, pz = P(a, b)
            doc.set(px, Y0 + 2, pz, "minecraft:oak_slab[type=bottom]", sec, force=False)
    for a in (-4, 4):
        px, pz = P(a, 0)
        doc.set(px, Y0 + 2, pz, OAK, sec, force=False)
    px, pz = P(0, 0)
    doc.set(px, Y0 + 2, pz, "minecraft:oak_pressure_plate", sec, force=False)
    for a in (-2, 2):
        for b in (-2, 2):
            px, pz = P(a, b)
            doc.set(px, Y0 + 3, pz, "minecraft:oak_fence", sec, force=False)


def build_steamboat(doc, x, z, heading, sec="14_boats"):
    """White / blue river steamer with a black funnel (as at Rocket Spleef)."""
    ca, sa = math.cos(math.radians(heading)), math.sin(math.radians(heading))
    def P(a, b):
        return (int(round(x + ca * a - sa * b)), int(round(z + sa * a + ca * b)))
    for a in range(-8, 9):
        for b in range(-3, 4):
            px, pz = P(a, b)
            doc.set(px, Y0 + 1, pz, BL, sec, force=False)
            edge = abs(b) == 3 or abs(a) == 8
            doc.set(px, Y0 + 2, pz, W if edge else BL, sec, force=False)
    for a in range(-6, 7):
        for b in range(-2, 3):
            px, pz = P(a, b)
            doc.set(px, Y0 + 3, pz, DKO, sec, force=False)
    for a in range(-3, 4):
        for b in range(-2, 3):
            px, pz = P(a, b)
            doc.set(px, Y0 + 4, pz, W, sec, force=False)
            doc.set(px, Y0 + 5, pz, W, sec, force=False)
            if abs(b) == 2:
                doc.set(px, Y0 + 6, pz, BL, sec, force=False)
    px, pz = P(0, 0)
    for y in (Y0 + 6, Y0 + 7):
        doc.set(px, y, pz, BK, sec, force=False)
    px, pz = P(-4, 0)
    doc.set(px, Y0 + 4, pz, LANTERN, sec, force=False)


def build_boats(doc):
    sec = "14_boats"
    for (x, z, h) in ((-30, -132, 20), (46, -136, 200), (128, 46, 300)):
        build_rowboat(doc, x, z, h, sec)
    build_steamboat(doc, 136, -6, 265, sec)


# ==========================================================================
# 15 -- hot-air balloons (MCCHubSeason1.png)
# ==========================================================================
def build_balloon(doc, cx, cz, cy, cols, sec="15_balloons"):
    """Gored envelope ~9 wide, 11 tall, wicker basket below."""
    R, Hh = 4.2, 6.0
    for dy in range(-6, 7):
        t = dy / Hh
        r = R * math.sqrt(max(0.0, 1.0 - t * t)) * (0.72 + 0.28 * (1.0 - t))
        if dy < -3:
            r *= 0.62
        ri = int(math.ceil(r))
        for dx in range(-ri, ri + 1):
            for dz in range(-ri, ri + 1):
                if dx * dx + dz * dz > r * r + 0.25:
                    continue
                ang = math.atan2(dz, dx)
                seg = int(((ang + math.pi) / (2 * math.pi)) * 14) % len(cols)
                doc.set(cx + dx, cy + dy, cz + dz, cols[seg], sec)
    # skirt + basket + ropes
    by = cy - 9
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            doc.set(cx + dx, by, cz + dz, DKO, sec)
            doc.set(cx + dx, by + 1, cz + dz, DKO, sec)
            doc.set(cx + dx, by + 2, cz + dz, "minecraft:spruce_fence", sec)
    doc.set(cx, by + 1, cz, "minecraft:shroomlight", sec)
    for dx, dz in ((-1, -1), (-1, 1), (1, -1), (1, 1)):
        for y in range(by + 3, cy - 4):
            doc.set(cx + dx, y, cz + dz, CHAIN, sec)


def build_balloons(doc):
    sec = "15_balloons"
    jobs = [
        (34, -124, 172, ("minecraft:green_concrete", YL, "minecraft:lime_concrete", YL)),
        (-24, -122, 186, ("minecraft:magenta_concrete", CY, "minecraft:pink_concrete", CY)),
        (112, -78, 178, (BL, RD, BL, RD)),
        (-96, -92, 184, (RD, RD, "minecraft:red_concrete", W)),
        (114, 30, 180, ("minecraft:purple_concrete", "minecraft:pink_concrete", W,
                        "minecraft:pink_concrete")),
        (18, -34, 194, (CY, W, CY, W)),
    ]
    for (x, z, y, cols) in jobs:
        build_balloon(doc, x, z, y, cols, sec)


# ==========================================================================
# Master build
# ==========================================================================
def build_all():
    doc = Doc()
    rng = random.Random(20251125)
    build_aprons(doc)
    build_ace_race(doc, rng)
    build_battle_box(doc, rng)
    build_dodgebolt(doc, rng)
    build_parkour_warrior(doc, rng)
    build_parkour_tag(doc, rng)
    build_rocket_spleef(doc, rng)
    build_aqua(doc, rng)
    build_market(doc, rng)
    build_trees(doc, rng)
    build_lamps_and_canes(doc, rng)
    build_presents(doc, rng)
    build_boats(doc)
    build_balloons(doc)
    return doc


# ==========================================================================
# Emission
# ==========================================================================
def rle_commands(cells):
    """{(x,y,z): block} -> setblock/fill commands, x-runs merged."""
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
            body = [f"# V4-D {dict(SECTIONS).get(key, key)} - "
                    f"{len(part)} commands (part {chunk})"]
            body.extend(part)
            text = "\n".join(body) + "\n"
            for d in (FN_DIR, DP_DIR):
                with open(os.path.join(d, name + ".mcfunction"), "w", encoding="utf-8") as fh:
                    fh.write(text)
            files.append((name, len(part), len(text)))
            chunk += 1

    load = ["# V4-D  --  Aqua building, six training portals, props",
            "# run:  function mcchub:v4d/load",
            "# nothing here is below y=121: the island surface belongs to V4-A"]
    for name, _, _ in files:
        load.append(f"function {NS}:v4d/{name}")
    for d in (FN_DIR, DP_DIR):
        with open(os.path.join(d, "load.mcfunction"), "w", encoding="utf-8") as fh:
            fh.write("\n".join(load) + "\n")

    with open(os.path.join(HERE, "datapack", "pack.mcmeta"), "w", encoding="utf-8") as fh:
        fh.write('{\n  "pack": {\n    "pack_format": 48,\n'
                 '    "description": "MCC festive hub replica - V4-D portals, Aqua, props"\n'
                 '  }\n}\n')

    with open(os.path.join(HERE, "blocks.csv"), "w", encoding="utf-8", newline="") as fh:
        fh.write("x,y,z,block\n")
        for (x, y, z) in sorted(doc.b, key=lambda k: (k[1], k[2], k[0])):
            fh.write(f"{x},{y},{z},{doc.b[(x, y, z)]}\n")
    return files


# ==========================================================================
# Preview dump (build + preview-only ground, so placement can be judged)
# ==========================================================================
def context_block(x, z):
    """Preview-only stand-in for V4-A's island.  NOT emitted, never pasted."""
    r = math.hypot(x + 0.5, z + 0.5)
    if r < 33.5:
        return "minecraft:smooth_stone"
    if r < 118:
        return "minecraft:grass_block[snowy=false]"
    if r < 128:
        return "minecraft:sand"
    if r < 150:
        return "minecraft:water[level=0]"
    return None


def _dump(blocks, name, x0, y0, z0, x1, y1, z1, gz=False):
    path = os.path.join(HERE, name)
    op = gzip.open if gz else open
    with op(path, "wt", encoding="utf-8") as fh:
        fh.write(f"MCMDUMP 1 {x1 - x0 + 1} {y1 - y0 + 1} {z1 - z0 + 1}\n")
        for (x, y, z) in sorted(blocks, key=lambda k: (k[1], k[2], k[0])):
            fh.write(f"{x - x0} {y - y0} {z - z0} {blocks[(x, y, z)]}\n")


def write_preview(doc):
    x0, z0, y0, x1, z1, y1 = RG
    blocks = dict(doc.b)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b = context_block(x, z)
            if b and (x, GROUND, z) not in blocks:
                blocks[(x, GROUND, z)] = b
    _dump(blocks, "preview.mcmdump.gz", x0, y0, z0, x1, y1, z1, gz=True)


def write_detail(doc, name, cx, cz, rad=34, ylo=118, yhi=190):
    x0, x1 = cx - rad, cx + rad
    z0, z1 = cz - rad, cz + rad
    blocks = {}
    for (x, y, z), b in doc.b.items():
        if x0 <= x <= x1 and z0 <= z <= z1 and ylo <= y <= yhi:
            blocks[(x, y, z)] = b
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b = context_block(x, z)
            if b and (x, GROUND, z) not in blocks:
                blocks[(x, GROUND, z)] = b
    _dump(blocks, name, x0, ylo, z0, x1, yhi, z1, gz=True)


# ==========================================================================
# Checks
# ==========================================================================
def checks(doc):
    rep = {}
    rep["blocks"] = len(doc.b)
    xs = [k[0] for k in doc.b]
    ys = [k[1] for k in doc.b]
    zs = [k[2] for k in doc.b]
    rep["bounds"] = (min(xs), min(ys), min(zs), max(xs), max(ys), max(zs))

    rep["below_y121"] = sum(1 for k in doc.b if k[1] < Y0)
    rep["in_plinth_r33"] = sum(1 for k in doc.b if math.hypot(k[0] + 0.5, k[2] + 0.5) < 33.0)

    # blocks belonging to another agent's named volume
    clash = 0
    clash_air = 0
    for (x, y, z) in doc.b:
        for (a, b, c, d) in RES_OTHER:
            if a <= x <= c and b <= z <= d:
                if y >= 150:
                    clash_air += 1
                else:
                    clash += 1
                break
    rep["in_other_agent_boxes"] = clash
    rep["over_other_boxes_above_y150"] = clash_air

    # blocks outside every plot of this agent (boats and balloons excluded)
    out = 0
    out_sec = {}
    STRUCT = ("00_aprons", "01_ace_race", "02_battle_box", "03_dodgebolt",
              "04_parkour_warrior", "05_parkour_tag", "06_rocket_spleef", "07_aqua")
    for (x, y, z), s in doc.sec.items():
        if s not in STRUCT:
            continue
        inside = False
        for (a, b, c, d) in PLOTS.values():
            if a <= x <= c and b <= z <= d:
                inside = True
                break
        if not inside:
            out += 1
            out_sec[s] = out_sec.get(s, 0) + 1
    rep["outside_plots"] = out
    rep["outside_plots_by_section"] = out_sec

    rep["outside_build_area"] = sum(
        1 for (x, y, z) in doc.b if not (-150 <= x <= 150 and -150 <= z <= 150
                                         and 90 <= y <= 210))

    per = {}
    for s in doc.sec.values():
        per[s] = per.get(s, 0) + 1
    rep["per_section"] = per
    rep["distinct_blocks"] = len(set(doc.b.values()))
    return rep


def main():
    doc = build_all()
    files = emit(doc)
    write_preview(doc)
    write_detail(doc, "detail_ace_race.mcmdump.gz", AR_C[0] + 4, AR_C[1], 40)
    write_detail(doc, "detail_battle_box.mcmdump.gz", -26, -104, 30)
    write_detail(doc, "detail_dodgebolt.mcmdump.gz", 76, -80, 30)
    write_detail(doc, "detail_parkour_warrior.mcmdump.gz", 24, -100, 32, 118, 190)
    write_detail(doc, "detail_parkour_tag.mcmdump.gz", -100, -50, 28)
    write_detail(doc, "detail_rocket_spleef.mcmdump.gz", 108, -40, 30, 118, 195)
    write_detail(doc, "detail_aqua.mcmdump.gz", 76, 0, 32)
    rep = checks(doc)

    print(f"blocks            : {rep['blocks']}")
    print(f"commands (chunks) : {sum(f[1] for f in files)} in {len(files)} files")
    print(f"bounds            : {rep['bounds']}")
    for k in ("below_y121", "in_plinth_r33", "in_other_agent_boxes",
              "outside_plots", "outside_build_area", "distinct_blocks"):
        print(f"{k:18s}: {rep[k]}")
    if rep["outside_plots_by_section"]:
        print("outside plots by section:", rep["outside_plots_by_section"])
    print("per section:")
    for s in sorted(rep["per_section"]):
        print(f"   {s:22s} {rep['per_section'][s]}")
    print("files:")
    for name, n, size in files:
        print(f"   {name:28s} {n:6d} cmds {size/1024:8.1f} KiB")
    return rep


if __name__ == "__main__":
    main()









