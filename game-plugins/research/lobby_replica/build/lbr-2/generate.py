#!/usr/bin/env python3
"""
LBR-2 - festive Decision Dome for the MCC event-lobby replica.

Build site:  C = (5000, 120, 5000), world mcc_lobby_official.
Reference:   game-plugins/research/lobby_replica/reference/hub_festive.png
             (festive hub; NOT the dark Nocturne Orrery / imported mcc_dome).

Outputs (all inside this folder):
  blocks.csv                     final block list, absolute world coords
  functions/lbr2/*.mcfunction    chunked commands, one load function
  preview.mcmdump.gz             renderer dump
  preview_top.png, preview_iso.png
  STATUS.md                      written by hand, not this script

Conventions:
  x east, z south (the clock / entrance face is south, +z), y up.
  block coord (x,y,z) is the block's own integer coordinate.
  every block is a vanilla Java 1.21.1 block id with no impossible states.
"""

from __future__ import annotations

import csv
import gzip
import math
import os
import sys
from collections import defaultdict

# ----------------------------------------------------------------------------
# site
# ----------------------------------------------------------------------------
CX, CZ = 5000, 5000
BASE_Y = 120.0

DRUM_TOP = 161          # last drum-shell layer
TERRACE_Y = 162
DOME_APEX = 184
CROWN_TOP = 175
BOW_TOP = 195

# ----------------------------------------------------------------------------
# block registry
# ----------------------------------------------------------------------------
BLOCKS: dict[tuple[int, int, int], tuple[str, str]] = {}


def put(x: int, y: int, z: int, block: str, section: str) -> None:
    BLOCKS[(int(x), int(y), int(z))] = (block, section)


def put_if_air(x: int, y: int, z: int, block: str, section: str) -> None:
    if (int(x), int(y), int(z)) not in BLOCKS:
        put(x, y, z, block, section)


def clear_box(x0, y0, z0, x1, y1, z1) -> None:
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                BLOCKS.pop((x, y, z), None)


def fill_box(x0, y0, z0, x1, y1, z1, block, section) -> None:
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                put(x, y, z, block, section)


# ----------------------------------------------------------------------------
# geometry helpers
# ----------------------------------------------------------------------------
def d2(x: float, z: float) -> float:
    return math.hypot(x - CX, z - CZ)


def angle_of(x: float, z: float) -> float:
    """Clockwise degrees from north (-z), 0..360."""
    return math.degrees(math.atan2(x - CX, -(z - CZ))) % 360.0


def adist(a: float, b: float) -> float:
    d = abs(a - b) % 360.0
    return min(d, 360.0 - d)


def ring(y: int, r0: float, r1: float, block: str, section: str,
         cx: float = CX, cz: float = CZ) -> None:
    R = int(math.ceil(r1)) + 2
    for x in range(int(cx) - R, int(cx) + R + 1):
        for z in range(int(cz) - R, int(cz) + R + 1):
            d = math.hypot(x - cx, z - cz)
            if r0 <= d <= r1:
                put(x, y, z, block, section)


def disc(y: int, r: float, block: str, section: str,
         cx: float = CX, cz: float = CZ) -> None:
    R = int(math.ceil(r)) + 2
    for x in range(int(cx) - R, int(cx) + R + 1):
        for z in range(int(cz) - R, int(cz) + R + 1):
            if math.hypot(x - cx, z - cz) <= r:
                put(x, y, z, block, section)


def cyl_shell(y0: int, y1: int, r0: float, r1: float, block: str, section: str) -> None:
    for y in range(y0, y1 + 1):
        ring(y, r0, r1, block, section)


def line3d(p0, p1, block, section, width=1, thick=1) -> None:
    """Simple 3-D Bresenham-ish ribbon, one block per step (good enough for cables)."""
    x0, y0, z0 = p0
    x1, y1, z1 = p1
    n = max(abs(x1 - x0), abs(y1 - y0), abs(z1 - z0), 1)
    for i in range(n + 1):
        t = i / n
        x = round(x0 + (x1 - x0) * t)
        y = round(y0 + (y1 - y0) * t)
        z = round(z0 + (z1 - z0) * t)
        for dx in range(width):
            for dy in range(thick):
                put(x + dx, y, z + dy, block, section)


def surface_z(x: float, y: int) -> int:
    """Front-most z of the drum shell at this x (south face)."""
    dx = x - CX
    if abs(dx) >= 26.4:
        return CZ
    return int(math.floor(math.sqrt(26.6 ** 2 - dx * dx) + CZ))


# ----------------------------------------------------------------------------
# palette
# ----------------------------------------------------------------------------
TEAM = {
    "lime": "lime_concrete",
    "green": "green_concrete",
    "cyan": "cyan_concrete",
    "light_blue": "light_blue_concrete",
    "blue": "blue_concrete",
    "purple": "purple_concrete",
    "pink": "pink_concrete",
    "red": "red_concrete",
    "orange": "orange_concrete",
    "yellow": "yellow_concrete",
}
TEAM_TERRA = {
    "lime": "lime_terracotta",
    "green": "green_terracotta",
    "cyan": "cyan_terracotta",
    "light_blue": "light_blue_terracotta",
    "blue": "blue_terracotta",
    "purple": "purple_terracotta",
    "pink": "pink_terracotta",
    "red": "red_terracotta",
    "orange": "orange_terracotta",
    "yellow": "yellow_terracotta",
}
TEAM_GLASS = {
    "lime": "lime_stained_glass",
    "green": "green_stained_glass",
    "cyan": "cyan_stained_glass",
    "light_blue": "light_blue_stained_glass",
    "blue": "blue_stained_glass",
    "purple": "purple_stained_glass",
    "pink": "pink_stained_glass",
    "red": "red_stained_glass",
    "orange": "orange_stained_glass",
    "yellow": "yellow_stained_glass",
}

# The ten pods, clockwise from the rotated north-east start. 18 deg offset keeps
# both the south (clock) and north (service) entrances clear.
PODS = [
    ("lime", 18), ("green", 54), ("cyan", 90), ("light_blue", 126), ("blue", 162),
    ("purple", 198), ("pink", 234), ("red", 270), ("orange", 306), ("yellow", 342),
]
POD_R = 15.5

S = {
    "base": "10_base",
    "drum": "20_drum",
    "facade": "30_facade",
    "windows": "40_windows",
    "entrance": "50_entrance",
    "bridge": "55_bridge",
    "clock": "60_clock_sign",
    "terrace": "65_terrace",
    "pods": "70_pods",
    "dome": "80_glass_dome",
    "crown": "90_crown",
    "cross": "92_cross",
    "bow": "94_bow",
    "annex": "96_west_annex",
    "speaker": "98_east_tower",
    "lights": "99_lights_snow",
}
ORDER = list(S.values())


# ============================================================================
# 1. DRUM SHELL
# ============================================================================
def build_drum() -> None:
    # dark two-block plinth band, then a hollow 3-block wall
    for y in range(120, 122):
        ring(y, 23.6, 26.6, "polished_deepslate", S["base"])
    # gold feet on the eight principal axes
    for a in range(0, 360, 45):
        ang = math.radians(a)
        x = CX + 26.0 * math.sin(ang)
        z = CZ - 26.0 * math.cos(ang)
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                put(round(x) + dx, 120, round(z) + dz, "gold_block", S["base"])

    for y in range(122, DRUM_TOP + 1):
        r_in = 23.4 if y <= 137 else 24.4
        for x in range(4973, 5028):
            for z in range(4973, 5028):
                d = math.hypot(x - CX, z - CZ)
                if r_in <= d <= 26.6:
                    put(x, y, z, "white_concrete", S["drum"])

    # interior skin in warm deep red, seen through the entrance / above the rim
    for y in range(122, DRUM_TOP + 1):
        for x in range(4973, 5028):
            for z in range(4973, 5028):
                d = math.hypot(x - CX, z - CZ)
                if 22.4 <= d < 23.4:
                    put(x, y, z, "red_concrete", S["drum"])


# ============================================================================
# 2. FACADE
# ============================================================================
def build_facade() -> None:
    # ---- lower drum: red field, team panels, gold pilasters, candy canes ----
    for y in range(122, 139):
        for x in range(4973, 5028):
            for z in range(4973, 5028):
                d = math.hypot(x - CX, z - CZ)
                if not (24.8 <= d <= 26.6):
                    continue
                ang = angle_of(x, z)
                base = "red_concrete"
                if y <= 131:
                    # cream horizontal courses
                    if y in (123, 128):
                        base = "white_concrete"
                    # team panel
                    hit = None
                    for name, pa in PODS:
                        if adist(ang, pa) <= 4.1:
                            hit = name
                            break
                    if hit:
                        base = TEAM[hit]
                        if y in (125, 130):
                            base = TEAM_TERRA[hit]
                        if y == 124 and adist(ang, dict(PODS)[hit]) <= 0.8:
                            base = "white_concrete"
                    # gold pilaster at the midpoints
                    for pa in range(0, 360, 36):
                        if adist(ang, pa) <= 1.6:
                            base = "gold_block"
                    # four candy cane stripes only, at the diagonals
                    for ca in (45, 135, 225, 315):
                        if adist(ang, ca) <= 1.9 and y not in (123, 128):
                            base = "red_concrete" if y % 2 == 0 else "white_concrete"
                    if y == 131:
                        base = "gold_block"
                else:
                    # red belt y132..138
                    base = "red_concrete"
                    if y in (132, 138):
                        base = "gold_block"
                    if y in (134, 136):
                        for pa in range(0, 360, 36):
                            if adist(ang, pa) <= 1.2:
                                base = "gold_block"
                    if y == 137 and adist(ang, 180) < 30:
                        base = "white_concrete"
                put(x, y, z, base, S["facade"])

    # ---- upper drum: red field, gold ribs, cream window bays, snowflakes ----
    for y in range(139, 159):
        for x in range(4973, 5028):
            for z in range(4973, 5028):
                d = math.hypot(x - CX, z - CZ)
                if not (24.8 <= d <= 26.6):
                    continue
                ang = angle_of(x, z)
                base = "red_concrete"
                # horizontal gold trim
                if y in (139, 148, 157):
                    base = "gold_block"
                # gold pilasters on the bay boundaries
                for pa in range(0, 360, 36):
                    if adist(ang, pa) <= 1.9:
                        base = "gold_block"
                # cream window bays at the four diagonals
                for wa in (45, 135, 225, 315):
                    if adist(ang, wa) <= 8.5:
                        base = "white_concrete"
                        if 141 <= y <= 151:
                            if 3.0 <= adist(ang, wa) * math.pi / 180 * 25.5 <= 4.0 or y in (141, 151):
                                base = "black_concrete"
                            else:
                                base = "cyan_stained_glass" if y % 3 else "light_blue_stained_glass"
                # snowflake motifs high on the red field, between windows
                if 151 <= y <= 155:
                    for sa in range(22, 360, 45):
                        s = adist(ang, sa)
                        # a 5x5 plus / star in angular + height space
                        ds = s * math.pi / 180 * 26.0
                        dy = y - 153
                        if abs(ds) < 0.7 and abs(dy) <= 2:
                            base = "white_concrete"
                        elif abs(dy) < 0.7 and abs(ds) <= 2.4:
                            base = "white_concrete"
                        elif abs(abs(ds) - abs(dy)) < 0.7 and abs(ds) <= 2.2:
                            base = "light_blue_concrete"
                put(x, y, z, base, S["windows"])

    # ---- black cornice under the terrace ----
    for y in range(159, 162):
        for x in range(4973, 5028):
            for z in range(4973, 5028):
                d = math.hypot(x - CX, z - CZ)
                if 24.6 <= d <= 27.0:
                    b = "black_concrete"
                    if y == 159:
                        b = "gold_block"
                    elif y == 160:
                        ang = angle_of(x, z)
                        if any(adist(ang, a) < 1.8 for a in range(0, 360, 20)):
                            b = "gold_block"
                    put(x, y, z, b, S["windows"])

    # ---- big speaker / porthole on the SE face ----
    for x in range(4973, 5028):
        for z in range(4973, 5028):
            d = math.hypot(x - CX, z - CZ)
            if not (25.0 <= d <= 26.6):
                continue
            ang = angle_of(x, z)
            ds = adist(ang, 135) * math.pi / 180.0 * 26.0
            for y in range(144, 153):
                rr = math.hypot(ds, y - 148.0)
                if rr <= 3.8:
                    b = "black_concrete"
                    if rr <= 2.9:
                        b = "gray_concrete"
                    if rr <= 1.8:
                        b = "black_concrete"
                    if rr <= 0.7:
                        b = "light_gray_concrete"
                    if 3.4 < rr <= 4.2:
                        b = "gold_block"
                    put(x, y, z, b, S["windows"])

    # ---- wreaths on the SW and SE faces ----
    for wa, wy in ((225, 148), (120, 130)):
        for x in range(4973, 5028):
            for z in range(4973, 5028):
                d = math.hypot(x - CX, z - CZ)
                if not (25.0 <= d <= 26.6):
                    continue
                ang = angle_of(x, z)
                ds = adist(ang, wa) * math.pi / 180.0 * 26.0
                for y in range(wy - 5, wy + 6):
                    rr = math.hypot(ds, y - wy)
                    if 2.0 <= rr <= 3.4:
                        b = "green_concrete"
                        if rr > 2.7 and (y + round(ds)) % 3 == 0:
                            b = "red_concrete"
                        put(x, y, z, b, S["windows"])
                # bow on top of the wreath
                if abs(ds) <= 1.2 and y in (wy + 3, wy + 4):
                    put(x, y, z, "red_concrete", S["windows"])

    # ---- garland / light string under the cornice ----
    for x in range(4973, 5028):
        for z in range(4973, 5028):
            d = math.hypot(x - CX, z - CZ)
            if 26.0 <= d <= 26.8:
                ang = angle_of(x, z)
                if (round(ang) % 12) < 2:
                    put(x, 158, z, "shroomlight", S["lights"])


# ============================================================================
# 3. ENTRANCES
# ============================================================================
def arch_open_halfwidth(y: int) -> int:
    """Half-width of the south entrance opening at height y (120..134)."""
    if y <= 126:
        return 6
    dy = y - 126
    if dy > 8:
        return 0
    return int(round(6.5 * math.sqrt(max(0.0, 1.0 - (dy / 8.0) ** 2))))


def build_south_entrance() -> None:
    """Gold/orange arch on the south face, tunnel, threshold. Carves the shell."""
    z_front = 5027
    # clear the opening through the shell and a shallow vestibule
    for y in range(120, 135):
        hw = arch_open_halfwidth(y)
        if hw <= 0:
            continue
        for x in range(CX - hw, CX + hw + 1):
            clear_box(x, y, 5016, x, y, z_front)
    # tunnel lining
    for z in range(5017, 5027):
        for x in range(CX - 7, CX + 8):
            # floor / threshold
            put(x, 120, z, "polished_andesite", S["entrance"])
            put(x, 121, z, "red_carpet", S["entrance"])
        for y in range(121, 135):
            put(CX - 7, y, z, "cyan_concrete", S["entrance"])
            put(CX + 7, y, z, "cyan_concrete", S["entrance"])
            if y in (126, 131):
                put(CX - 6, y, z, "gold_block", S["entrance"])
                put(CX + 6, y, z, "gold_block", S["entrance"])
        for x in range(CX - 6, CX + 7):
            put(x, 135, z, "gold_block", S["entrance"])
    for x in (CX - 4, CX + 4):
        for z in (5020, 5024):
            put(x, 134, z, "sea_lantern", S["entrance"])
    # wall sconces inside the vestibule
    for x in (CX - 7, CX + 7):
        for z in (5020, 5024):
            put(x, 127, z, "sea_lantern", S["entrance"])
            put(x, 130, z, "gold_block", S["entrance"])

    # raised arch frame, 2 thick, one block proud of the curved facade
    for y in range(120, 136):
        hw = arch_open_halfwidth(y)
        for x in range(CX - hw - 2, CX + hw + 3):
            d = abs(x - CX)
            z = surface_z(x, y) + 1
            if hw < d <= hw + 2:
                b = "gold_block"
                if (y + x) % 5 == 0:
                    b = "orange_concrete"
                if y == 120:
                    b = "polished_deepslate"
                put(x, y, z, b, S["entrance"])
                put(x, y, z + 1, b, S["entrance"])
    # cap the crown of the arch so there is no gap at the apex
    for x in range(CX - 4, CX + 5):
        z = surface_z(x, 134) + 1
        put(x, 134, z, "gold_block", S["entrance"])
        put(x, 134, z + 1, "gold_block", S["entrance"])
    for x in range(CX - 3, CX + 4):
        z = surface_z(x, 135) + 1
        put(x, 135, z, "gold_block", S["entrance"])
        put(x, 135, z + 1, "gold_block", S["entrance"])

    # outer crest above the arch
    for x in range(CX - 8, CX + 9):
        z = surface_z(x, 136) + 1
        put(x, 136, z, "red_concrete", S["entrance"])
        put(x, 137, z, "gold_block", S["entrance"])
    # small crown emblem on the crest
    emblem = [
        "100010001",
        "111111111",
        "011111110",
        "001111100",
    ]
    for r, row in enumerate(emblem):
        for c, ch in enumerate(row):
            if ch == "1":
                x = CX - 4 + c
                z = surface_z(x, 137) + 1
                put(x, 137 - r, z, "gold_block" if r < 3 else "red_concrete", S["entrance"])

    # teal rocket / candle columns flanking the arch
    for side in (-1, 1):
        xc = CX + side * 11
        zc = surface_z(xc, 120)
        for dx in (-1, 0, 1):
            for dz in (0, 1, 2):
                x, z = xc + dx, zc + dz
                for y in range(120, 138):
                    b = "cyan_concrete"
                    if y in (120, 121):
                        b = "gold_block"
                    if y in (125, 129, 133):
                        b = "white_concrete"
                    if y in (136, 137):
                        b = "gold_block"
                    if y == 137 and dx == 0 and dz == 1:
                        b = "red_concrete"
                    put(x, y, z, b, S["entrance"])
        # fins
        for y in (121, 122):
            for dx in (-2, 2):
                put(xc + dx, y, zc + 1, "gold_block", S["entrance"])

    # north service entrance, simpler
    build_north_entrance()


def build_north_entrance() -> None:
    z_back = 4973
    for y in range(120, 131):
        hw = 4 if y <= 126 else max(0, 4 - (y - 126))
        for x in range(CX - hw, CX + hw + 1):
            clear_box(x, y, 4973, x, y, 4983)
    for z in range(4974, 4983):
        for x in range(CX - 5, CX + 6):
            put(x, 120, z, "polished_andesite", S["entrance"])
            put(x, 121, z, "red_carpet", S["entrance"])
        for y in range(121, 130):
            put(CX - 5, y, z, "red_concrete", S["entrance"])
            put(CX + 5, y, z, "red_concrete", S["entrance"])
    for y in range(120, 132):
        hw = 4 if y <= 126 else max(0, 4 - (y - 126))
        for x in range(CX - hw - 1, CX + hw + 2):
            if hw < abs(x - CX) <= hw + 1:
                z = 4972
                put(x, y, z, "gold_block" if y not in (124, 128) else "red_concrete", S["entrance"])
    # north canopy
    for x in range(CX - 6, CX + 7):
        put(x, 131, 4971, "gold_block", S["entrance"])
        put(x, 132, 4971, "red_concrete", S["entrance"])
    for x in (CX - 3, CX + 3):
        put(x, 133, 4971, "sea_lantern", S["entrance"])


# ============================================================================
# 4. CLOCK AND SIGN
# ============================================================================
DIGITS = {
    "0": ["11111", "10001", "10001", "10001", "10001", "10001", "11111"],
    "2": ["11110", "00001", "00010", "00100", "01000", "10000", "11111"],
    "4": ["10001", "10001", "10001", "11111", "00001", "00001", "00001"],
    "colon": ["0", "1", "0", "1", "0"],
}

# 5-tall mixed-width font for CHAMPIONSHIP. H/P/S are 3 wide so the word keeps
# solid letter shapes instead of reading as a row of thin bars.
GLYPHS = {
    "C": ["110", "100", "100", "100", "110"],
    "H": ["101", "101", "111", "101", "101"],
    "A": ["010", "101", "111", "101", "101"],
    "M": ["101", "111", "111", "101", "101"],
    "P": ["110", "110", "100", "100", "100"],
    "I": ["111", "010", "010", "010", "111"],
    "O": ["111", "101", "101", "101", "111"],
    "N": ["101", "111", "111", "101", "101"],
    "S": ["111", "100", "111", "001", "111"],
    "E": ["111", "100", "110", "100", "111"],
}
WORD = "CHAMPIONSHIP"


def put_curved(x: int, y: int, z_off: int, block: str, section: str) -> None:
    """Place relative to the south drum face, z_off=1 -> panel, 2 -> raised detail."""
    put(x, y, surface_z(x, y) + z_off, block, section)


FRONT_Z = 5028          # flat marquee face, 2 blocks proud of the drum centre


def build_clock() -> None:
    """Flat black marquee box carrying the 25x9 clock and the MCC sign.

    The reference mounts both on a flat plate, not on the curve of the drum.
    The box is filled back to the drum shell at every column so it reads as a
    solid steel/gold sign cabinet, and the clock/sign face is perfectly flat.
    """
    # ---- stepped billboard backing ----
    for y in range(136, 162):
        if 146 <= y <= 154:
            x0, x1 = 4976, 5024      # wide banner band
        else:
            x0, x1 = 4982, 5018      # clock + crest band
        for x in range(x0, x1 + 1):
            zs = surface_z(x, y) + 1
            for z in range(zs, FRONT_Z + 1):
                b = "black_concrete"
                if z == FRONT_Z:
                    if y in (136, 161):
                        b = "deepslate_tiles"
                    if y in (147, 154):
                        b = "gold_block"
                put(x, y, z, b, S["clock"])

    # gold outline of the whole marquee
    for y in range(135, 162):
        x0, x1 = (4976, 5024) if 146 <= y <= 154 else (4982, 5018)
        for x in (x0 - 1, x1 + 1):
            put(x, y, FRONT_Z, "gold_block", S["clock"])
    for x in range(4979, 5022):
        put(x, 135, FRONT_Z, "gold_block", S["clock"])
        put(x, 161, FRONT_Z, "gold_block", S["clock"])

    face = FRONT_Z + 1

    # ---- clock: 25 x 9 face, x 4988..5012, y 137..145 ----
    x0, y0 = 4988, 137
    for col in range(25):
        for row in range(9):
            b = "black_concrete"
            if row in (0, 8):
                b = "deepslate_tiles"
            put(x0 + col, y0 + row, FRONT_Z + 1, b, S["clock"])
    # gold frame
    for col in range(-1, 26):
        for row in (-1, 9):
            put(x0 + col, y0 + row, FRONT_Z + 1, "gold_block", S["clock"])
    for row in range(-1, 10):
        for col in (-1, 25):
            put(x0 + col, y0 + row, FRONT_Z + 1, "gold_block", S["clock"])
    # corner jewels
    for col, row in ((0, 0), (24, 0), (0, 8), (24, 8)):
        put(x0 + col, y0 + row, FRONT_Z + 1, "redstone_block", S["clock"])

    # digits, 5x7, at x 0-4, 6-10, colon 12, 14-18, 20-24
    seq = [("0", 0), ("0", 6), ("colon", 12), ("4", 14), ("2", 20)]
    digit_top = y0 + 7          # glyph row 0 is the top row of the 7-row cell
    for glyph, ox in seq:
        if glyph == "colon":
            for r in (2, 4):
                put(x0 + ox, digit_top - r, FRONT_Z + 1, "white_concrete", S["clock"])
        else:
            for r, rowstr in enumerate(DIGITS[glyph]):
                for c, ch in enumerate(rowstr):
                    if ch == "1":
                        put(x0 + ox + c, digit_top - r, FRONT_Z + 1, "white_concrete", S["clock"])

    # ---- CHAMPIONSHIP banner, 49 outer / 47 field, x 4976..5024, y 147..153 ----
    bx0, by0 = 4977, 148
    widths = [len(GLYPHS[c][0]) for c in WORD]
    cursor = 0
    letter_cells = []
    for i, ch in enumerate(WORD):
        if i:
            cursor += 1
        letter_cells.append((ch, cursor))
        cursor += widths[i]
    assert cursor == 47, cursor
    for y in range(146, 155):
        for x in range(4976, 5025):
            b = "red_concrete"
            if y in (146, 154) or x in (4976, 5024):
                b = "gold_block"
            put(x, y, FRONT_Z + 1, b, S["clock"])
    for ch, ox in letter_cells:
        for r, rowstr in enumerate(GLYPHS[ch]):
            for c, bit in enumerate(rowstr):
                if bit == "1":
                    put(bx0 + ox + c, by0 + (4 - r), FRONT_Z + 1, "white_concrete", S["clock"])

    # ---- MCC crest, x 4992..5008, y 154..160 ----
    for y in range(154, 161):
        for x in range(4992, 5009):
            b = "red_concrete"
            if y in (154, 160) or x in (4992, 5008):
                b = "gold_block"
            put(x, y, FRONT_Z + 1, b, S["clock"])
    mcc = [
        "101  111  111",
        "111  100  100",
        "111  100  100",
        "101  111  111",
    ]
    for r, rowstr in enumerate(mcc):
        for c, ch in enumerate(rowstr):
            if ch == "1":
                put(4994 + c, 158 - r, FRONT_Z + 1, "white_concrete", S["clock"])

    # gold crown icon on the crest
    crown = [
        "100010001",
        "111111111",
        "011111110",
    ]
    for r, rowstr in enumerate(crown):
        for c, ch in enumerate(rowstr):
            if ch == "1":
                b = "redstone_block" if r == 0 and c in (0, 4, 8) else "gold_block"
                put(4996 + c, 159 + r, FRONT_Z + 1, b, S["clock"])

    # red/gold side wings turn the black marquee into a festive sign cabinet
    for y in range(137, 147):
        for x in list(range(4982, 4987)) + list(range(5014, 5019)):
            put(x, y, face, "red_concrete", S["clock"])
    for x in (4982, 4986, 5014, 5018):
        for y in range(137, 147):
            put(x, y, face, "gold_block", S["clock"])
    for x in list(range(4982, 4992)) + list(range(5009, 5019)):
        for y in range(154, 161):
            put(x, y, face, "red_concrete", S["clock"])
    for x in (4982, 4991, 5009, 5018):
        for y in range(154, 161):
            put(x, y, face, "gold_block", S["clock"])
    for x in range(4982, 5019):
        put(x, 154, face, "gold_block", S["clock"])
        put(x, 160, face, "gold_block", S["clock"])

    # industrial side pipes and lamps in the black margins; the banner rows
    # 146..154 are deliberately left clear so nothing crosses the lettering
    for x in (4984, 5016):
        for y in list(range(136, 146)) + list(range(155, 162)):
            b = "gold_block"
            if y % 6 in (0, 1):
                b = "deepslate_tiles"
            if y in (140, 158):
                b = "shroomlight"
            put(x, y, face, b, S["clock"])
    for y in (140, 158):
        put(4983, y, face, "shroomlight", S["clock"])
        put(5017, y, face, "shroomlight", S["clock"])
    # small gold studs on the bottom rail
    for x in range(4983, 5018, 3):
        put(x, 136, face, "gold_block", S["clock"])

    # warm lamps under the marquee lip
    for x in range(4982, 5019, 4):
        put(x, 134, FRONT_Z, "shroomlight", S["lights"])


# ============================================================================
# 5. TERRACE + ARENA FLOOR
# ============================================================================
def build_terrace() -> None:
    # floor disc inside the glass - dark red so the pods read through the glass
    disc(TERRACE_Y, 24.6, "red_terracotta", S["terrace"])
    ring(TERRACE_Y, 22.8, 24.6, "black_concrete", S["terrace"])
    # outer ring
    ring(TERRACE_Y, 24.6, 28.6, "white_concrete", S["terrace"])
    ring(TERRACE_Y, 24.6, 25.9, "polished_deepslate", S["terrace"])
    ring(TERRACE_Y, 28.0, 28.55, "white_concrete", S["terrace"])
    ring(TERRACE_Y, 28.55, 28.85, "gold_block", S["terrace"])
    # inlaid dial
    disc(TERRACE_Y, 3.4, "smooth_quartz", S["terrace"])
    ring(TERRACE_Y, 3.4, 4.3, "gold_block", S["terrace"])
    ring(TERRACE_Y, 8.0, 8.6, "gold_block", S["terrace"])
    disc(TERRACE_Y, 8.0, "red_terracotta", S["terrace"])
    disc(TERRACE_Y, 4.3, "white_concrete", S["terrace"])
    disc(TERRACE_Y, 3.4, "smooth_quartz", S["terrace"])
    # eight wedges, alternating cream and red, gold dividers
    for x in range(CX - 9, CX + 10):
        for z in range(CZ - 9, CZ + 10):
            d = math.hypot(x - CX, z - CZ)
            if not (4.3 < d <= 8.0):
                continue
            ang = angle_of(x, z)
            sector = int((ang + 22.5) // 45) % 8
            b = "white_concrete" if sector % 2 == 0 else "red_concrete"
            if adist(ang % 45, 0) < 1.0 or adist(ang % 45, 45) < 1.0:
                b = "gold_block"
            put(x, TERRACE_Y, z, b, S["terrace"])
    # centre light
    put(CX, TERRACE_Y, CZ, "sea_lantern", S["terrace"])

    # snow patches on the outer walkway
    for a in range(0, 360, 30):
        ang = math.radians(a)
        for rr in (26.8, 27.4):
            x = round(CX + rr * math.sin(ang))
            z = round(CZ - rr * math.cos(ang))
            put(x, TERRACE_Y + 1, z, "snow_block", S["lights"])

    # no railing: a clean rim keeps the glass dome and pod colours readable

    # pod-support ring lights
    for name, pa in PODS:
        ang = math.radians(pa)
        x = round(CX + 22.0 * math.sin(ang))
        z = round(CZ - 22.0 * math.cos(ang))
        put(x, TERRACE_Y, z, "sea_lantern", S["lights"])


# ============================================================================
# 6. TEN TEAM PODS
# ============================================================================
def build_pods() -> None:
    for name, pa in PODS:
        ang = math.radians(pa)
        pcx = round(CX + POD_R * math.sin(ang))
        pcz = round(CZ - POD_R * math.cos(ang))
        # 5x5 body, 7 tall (163..169)
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                x, z = pcx + dx, pcz + dz
                edge = max(abs(dx), abs(dz)) == 2
                for y in range(163, 170):
                    b = TEAM[name]
                    if y == 165:
                        b = TEAM_TERRA[name]
                    if y == 166 and abs(dx) <= 1 and abs(dz) <= 1:
                        b = TEAM_GLASS[name]
                    if y == 167 and edge:
                        b = TEAM_GLASS[name]
                    if y == 168 and edge:
                        b = "white_concrete"
                    put(x, y, z, b, S["pods"])
        # 7x7 canopy
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                x, z = pcx + dx, pcz + dz
                if max(abs(dx), abs(dz)) <= 1:
                    put(x, 168, z, TEAM_GLASS[name], S["pods"])
                    put(x, 169, z, "sea_lantern" if (dx, dz) == (0, 0) else TEAM[name], S["pods"])
                else:
                    put(x, 168, z, TEAM[name], S["pods"])
                    put(x, 169, z, TEAM_TERRA[name] if max(abs(dx), abs(dz)) == 3 else TEAM[name],
                        S["pods"])
        # stepped 5x5 crown so the pod tops read above the tiara
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                x, z = pcx + dx, pcz + dz
                edge = max(abs(dx), abs(dz)) == 2
                put(x, 170, z, TEAM[name] if edge else TEAM_GLASS[name], S["pods"])
                if max(abs(dx), abs(dz)) <= 1:
                    put(x, 171, z, "sea_lantern" if (dx, dz) == (0, 0) else TEAM[name], S["pods"])
                else:
                    put(x, 171, z, TEAM_TERRA[name], S["pods"])
        # team banner colour marks on the terrace edge
        ang2 = math.radians(pa)
        bx = round(CX + 23.4 * math.sin(ang2))
        bz = round(CZ - 23.4 * math.cos(ang2))
        put(bx, TERRACE_Y + 1, bz, TEAM[name], S["pods"])


# ============================================================================
# 7. GLASS DOME
# ============================================================================
DOME_R = {
    # near-hemisphere: radius 25 at the rim, 22 layers high, apex 184.
    162: 25.00, 163: 24.97, 164: 24.90, 165: 24.77,
    166: 24.58, 167: 24.35, 168: 24.05, 169: 23.70,
    170: 23.29, 171: 22.81, 172: 22.27, 173: 21.65,
    174: 20.95, 175: 20.17, 176: 19.28, 177: 18.29,
    178: 17.15, 179: 15.87, 180: 14.37, 181: 12.60,
    182: 10.41, 183: 7.45,
}


def build_dome() -> None:
    # Eight big glazed panels: four frosted white, four clear, so the pod
    # colours and the dark-red arena read through the shell.
    for y, r in DOME_R.items():
        for x in range(4973, 5028):
            for z in range(4973, 5028):
                d = math.hypot(x - CX, z - CZ)
                if r - 0.95 <= d <= r + 0.35:
                    ang = angle_of(x, z)
                    panel = int(ang // 45.0) % 8
                    b = "white_stained_glass" if panel % 2 == 0 else "glass"
                    if y >= 179 and panel in (1, 5):
                        b = "light_blue_stained_glass"
                    if y >= 182:
                        b = "white_stained_glass"
                    put(x, y, z, b, S["dome"])

    # eight bright copper ribs, deliberately 2 blocks wide so they read from
    # the top and from the approach
    for a in range(0, 360, 45):
        for da in (0.0, 2.4, 4.8):
            aa = math.radians(a + da)
            for y, r in DOME_R.items():
                x = round(CX + (r + 0.15) * math.sin(aa))
                z = round(CZ - (r + 0.15) * math.cos(aa))
                put(x, y, z, "copper_block", S["dome"])

    # three horizontal copper hoops make the dome read as a dome from above
    for hy in (175, 180, 183):
        hr = DOME_R[hy]
        ring(hy, hr - 1.1, hr + 0.5, "copper_block", S["dome"])

    # gold base ring and apex collar
    ring(162, 24.9, 26.3, "gold_block", S["dome"])
    ring(163, 25.0, 26.1, "gold_block", S["dome"])
    for x in range(CX - 2, CX + 3):
        for z in range(CZ - 2, CZ + 3):
            put(x, DOME_APEX, z, "gold_block", S["dome"])
    put(CX, DOME_APEX + 1, CZ, "gold_block", S["dome"])


# ============================================================================
# 8. CROWN
# ============================================================================
def build_crown() -> None:
    # slim flared lip just above the glass cylinder
    ring(165, 25.1, 26.7, "yellow_concrete", S["crown"])
    ring(165, 25.1, 26.0, "gold_block", S["crown"])
    # main band, following the dome's taper
    ring(166, 24.8, 26.2, "gold_block", S["crown"])
    ring(167, 24.1, 25.5, "gold_block", S["crown"])
    ring(168, 23.3, 24.7, "gold_block", S["crown"])
    # band jewels on the eight spike axes
    for a in range(22, 360, 45):
        ang = math.radians(a)
        x = round(CX + 25.2 * math.sin(ang))
        z = round(CZ - 25.2 * math.cos(ang))
        put(x, 166, z, "redstone_block", S["crown"])
    # eight tall spikes with red jewels, hugging the dome
    for a in range(22, 360, 45):
        ang = math.radians(a)
        bx = round(CX + 24.3 * math.sin(ang))
        bz = round(CZ - 24.3 * math.cos(ang))
        for dx in (0, 1):
            for dz in (0, 1):
                x, z = bx + dx, bz + dz
                for y in range(168, 174):
                    put(x, y, z, "gold_block", S["crown"])
                put(x, 174, z, "redstone_block", S["crown"])
                put(x, 175, z, "gold_block", S["crown"])
        # little gold prongs at the base
        for dx in (-1, 2):
            put(bx + dx, 168, bz + (0 if dx == -1 else 1), "gold_block", S["crown"])


# ============================================================================
# 9. WHITE CROSS
# ============================================================================
def build_cross() -> None:
    # white cross rising through the bow knot and clear above it
    for y in range(193, 200):
        put(CX, y, CZ, "smooth_quartz", S["cross"])
    for x in range(CX - 2, CX + 3):
        put(x, 197, CZ, "smooth_quartz", S["cross"])
    put(CX, 199, CZ, "white_concrete", S["cross"])


# ============================================================================
# 10. RED BOW
# ============================================================================
def torus(cx, cy, cz, R, tube, u, v, section, block="red_concrete") -> None:
    """3-D doughnut loop: |  (sqrt((p.u)^2+(p.v)^2)-R, p.n) | <= tube."""
    ux, uy, uz = u
    vx, vy, vz = v
    nx = uy * vz - uz * vy
    ny = uz * vx - ux * vz
    nz = ux * vy - uy * vx
    rx, ry, rz = int(R + tube + 2), int(R + tube + 2), int(R + tube + 2)
    for x in range(int(cx) - rx, int(cx) + rx + 1):
        for y in range(int(cy) - ry, int(cy) + ry + 1):
            for z in range(int(cz) - rz, int(cz) + rz + 1):
                px, py, pz = x - cx, y - cy, z - cz
                uu = px * ux + py * uy + pz * uz
                vv = px * vx + py * vy + pz * vz
                ww = px * nx + py * ny + pz * nz
                dist = math.hypot(math.hypot(uu, vv) - R, ww)
                if dist <= tube:
                    b = block if dist < tube * 0.68 else "red_terracotta"
                    put(x, y, z, b, section)


def build_bow() -> None:
    """Four 3-D loops tilted toward the viewer, a knot, and two draped tails."""
    # planes: u = horizontal long axis, v = short axis receding up/north.
    # The normal (0, +0.835, +0.55) faces the viewer from above, so the holes
    # read both in the front view and in the top view.
    v_plane = (0.0, 0.55, -0.835)

    torus(4993.2, 190.5, 5000.0, 5.2, 1.55, (-1.0, 0.0, 0.0), v_plane, S["bow"])
    torus(5006.8, 190.5, 5000.0, 5.2, 1.55, (1.0, 0.0, 0.0), v_plane, S["bow"])
    torus(4993.8, 186.5, 5000.0, 4.2, 1.30, (-1.0, 0.0, 0.0), v_plane, S["bow"])
    torus(5006.2, 186.5, 5000.0, 4.2, 1.30, (1.0, 0.0, 0.0), v_plane, S["bow"])

    # compact knot
    for dx in range(-1, 2):
        for dy in range(-2, 3):
            for dz in range(-1, 5):
                b = "red_concrete"
                if dz in (1, 2):
                    b = "red_terracotta"
                if dx == 0 and dz == 4:
                    b = "red_terracotta"
                put(CX + dx, 187 + dy, CZ + dz, b, S["bow"])
    # a little top fold
    put(CX, 189, CZ + 3, "red_terracotta", S["bow"])
    put(CX, 190, CZ + 2, "red_concrete", S["bow"])

    # two narrow tails following the glass down the front
    for side in (-1, 1):
        for y in range(183, 175, -1):
            r = DOME_R[y]
            t = (183 - y) / 7.0
            xoff = side * (2.0 + 4.4 * t)
            x = round(CX + xoff)
            z = round(CZ + math.sqrt(max(r * r - xoff * xoff, 0)) + 1.0)
            for dx in (-1, 0, 1):
                b = "red_concrete" if dx == 0 else "red_terracotta"
                put(x + dx, y, z, b, S["bow"])
        # swallow-tail end
        for dx in (-1, 0, 1):
            x = round(CX + side * 6.4) + dx
            z = round(CZ + math.sqrt(max(DOME_R[176] ** 2 - 6.4 ** 2, 0)) + 1.0)
            put(x, 175, z, "red_concrete" if dx == 0 else "red_terracotta", S["bow"])


# ============================================================================
# 11. BRIDGE
# ============================================================================
def build_bridge() -> None:
    # narrow deck from the threshold out to the plaza
    for z in range(5027, 5041):
        for x in range(4996, 5005):
            put(x, 120, z, "smooth_stone", S["bridge"])
            if 4998 <= x <= 5002:
                put(x, 121, z, "red_carpet", S["bridge"])
    # gold lip at the arch
    for x in range(4996, 5005):
        put(x, 120, 5027, "gold_block", S["bridge"])
    # low side curbs and lantern posts
    for side in (4995, 5005):
        for z in range(5027, 5041):
            put(side, 121, z, "polished_deepslate", S["bridge"])
        for z in (5028, 5032, 5036, 5040):
            put(side, 122, z, "gold_block", S["bridge"])
            put(side, 123, z, "gold_block", S["bridge"])
            put(side, 124, z, "lantern", S["bridge"])
    # end posts and low step onto the plaza
    for x in range(4995, 5006):
        put(x, 121, 5040, "gold_block", S["bridge"])
        put(x, 120, 5041, "polished_andesite", S["bridge"])
        put(x, 121, 5041, "red_carpet", S["bridge"])


# ============================================================================
# 12. WEST SCREEN ANNEX
# ============================================================================
def build_west_annex() -> None:
    x0, x1 = 4960, 4973
    z0, z1 = 5014, 5027
    # legs
    for lx in (x0 + 1, x1 - 1):
        for lz in (z0 + 1, z1 - 1):
            for y in range(120, 130):
                for dx in (0, 1):
                    for dz in (0, 1):
                        b = "cyan_concrete" if y % 4 else "white_concrete"
                        put(lx + dx, y, lz + dz, b, S["annex"])
            put(lx, 120, lz, "gold_block", S["annex"])
            put(lx, 121, lz, "gold_block", S["annex"])
    # body
    for y in range(130, 155):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge = x in (x0, x1) or z in (z0, z1)
                if not edge:
                    continue
                b = "light_gray_concrete"
                if y in (130, 140, 152, 154):
                    b = "gold_block"
                if x in (x0, x1) and z in (z0, z1):
                    b = "cyan_concrete"
                put(x, y, z, b, S["annex"])
    # stepped red/gold roof
    for y, inset, block in ((154, 1, "gold_block"), (155, 1, "red_concrete"),
                            (156, 2, "red_concrete"), (157, 3, "red_concrete"),
                            (158, 4, "gold_block"), (159, 5, "gold_block")):
        for x in range(x0 - 1 + inset, x1 + 2 - inset):
            for z in range(z0 - 1 + inset, z1 + 2 - inset):
                put(x, y, z, block, S["annex"])
    # snow on the roof shoulders
    for x in range(x0 - 1, x1 + 2):
        put(x, 155, z0, "snow_block", S["annex"])
        put(x, 155, z1, "snow_block", S["annex"])

    # flag pole and pennant
    for y in range(156, 166):
        put(x1 - 4, y, z0 + 4, "gold_block", S["annex"])
    for y in range(162, 166):
        for x in range(x1 - 3, x1 + 1):
            put(x, y, z0 + 4, "red_concrete", S["annex"])

    # large screen on the south face
    sx0, sy0 = 4961, 133
    W, H = 13, 18
    for r in range(H):
        for c in range(W):
            b = "light_blue_concrete" if r < 4 else "cyan_concrete"
            # mountain range on the horizon
            if 7 <= r <= 15:
                peak = 0.9 * (14 - r) + 1.7 * math.cos((c - 6.5) / 2.2)
                if (14 - r) <= peak:
                    b = "white_concrete"
            # snow field
            if r >= 15:
                b = "white_concrete" if r < 17 else "light_gray_concrete"
            # gold ring, the dome motif
            rr = math.hypot(c - 6.5, r - 9.0)
            if 3.2 <= rr <= 4.3:
                b = "gold_block"
            elif rr <= 2.6:
                b = "cyan_concrete"
            # red pennant at the top left
            if 2 <= r <= 5 and 1 <= c <= 2:
                b = "red_concrete"
            # snowflake sparkles
            if (c, r) in ((2, 1), (10, 2), (11, 6), (3, 12)):
                b = "white_concrete"
            put(sx0 + c, sy0 + (H - 1 - r), z1 + 1, b, S["annex"])
    # gold screen frame
    for c in range(-1, W + 1):
        put(sx0 + c, sy0 - 1, z1 + 1, "gold_block", S["annex"])
        put(sx0 + c, sy0 + H, z1 + 1, "gold_block", S["annex"])
    for r in range(-1, H + 1):
        put(sx0 - 1, sy0 + r, z1 + 1, "gold_block", S["annex"])
        put(sx0 + W, sy0 + r, z1 + 1, "gold_block", S["annex"])

    # pipe into the drum
    for x in range(4974, 4982):
        for y in range(140, 144):
            for z in range(5016, 5021):
                b = "gold_block" if y in (140, 143) else "copper_block"
                put(x, y, z, b, S["annex"])


# ============================================================================
# 13. EAST SPEAKER TOWER
# ============================================================================
def build_east_tower() -> None:
    ecx, ecz = 5030, 4997
    # legs
    for lx in (ecx - 4, ecx + 3):
        for lz in (ecz - 4, ecz + 3):
            for y in range(120, 128):
                for dx in (0, 1):
                    for dz in (0, 1):
                        put(lx + dx, y, lz + dz, "black_concrete" if y % 3 else "gold_block",
                            S["speaker"])
    # cylindrical cabinet
    for y in range(128, 151):
        for x in range(ecx - 6, ecx + 7):
            for z in range(ecz - 6, ecz + 7):
                d = math.hypot(x - ecx, z - ecz)
                if 3.6 <= d <= 5.2:
                    b = "yellow_concrete"
                    if y in (143, 144, 146, 147):
                        b = "black_concrete"
                    if y in (128, 129, 149, 150):
                        b = "gold_block"
                    put(x, y, z, b, S["speaker"])
    # cap
    disc(150, 5.4, "gold_block", S["speaker"], ecx, ecz)
    disc(151, 3.0, "red_concrete", S["speaker"], ecx, ecz)
    # speaker cone on the south face
    for x in range(ecx - 4, ecx + 5):
        for y in range(135, 144):
            rr = math.hypot(x - ecx, y - 139)
            if rr <= 4.2:
                b = "black_concrete"
                if rr <= 3.2:
                    b = "gray_concrete"
                if rr <= 2.0:
                    b = "black_concrete"
                if rr <= 1.0:
                    b = "light_gray_concrete"
                put(x, y, ecz + 5, b, S["speaker"])
                put(x, y, ecz + 6, b, S["speaker"])
    # cable down the front
    for i in range(0, 13):
        t = i / 12.0
        x = round(ecx - 5 + 4 * t)
        y = round(128 + 11 * t)
        put(x, y, ecz + 5, "black_concrete", S["speaker"])
    # small gold/red finial on the cap
    disc(151, 3.0, "red_concrete", S["speaker"], ecx, ecz)
    disc(152, 2.0, "gold_block", S["speaker"], ecx, ecz)
    put(ecx, 153, ecz, "gold_block", S["speaker"])
    put(ecx, 154, ecz, "redstone_block", S["speaker"])
    # connector
    for x in range(5023, 5028):
        for y in range(138, 141):
            for z in range(4996, 5000):
                put(x, y, z, "copper_block" if y != 139 else "black_concrete", S["speaker"])


# ============================================================================
# 14. FINAL LIGHTS / SNOW
# ============================================================================
def build_finishing() -> None:
    # warm lights under the crown lip
    for a in range(0, 360, 30):
        ang = math.radians(a)
        x = round(CX + 27.6 * math.sin(ang))
        z = round(CZ - 27.6 * math.cos(ang))
        put(x, 162, z, "sea_lantern", S["lights"])
    # snow on the bridge rails / posts
    for z in (5028, 5032, 5036):
        for side in (4995, 5005):
            put(side, 125, z, "snow_block", S["lights"])
    # north canopy snow (leave the two lamps clear)
    for x in range(CX - 5, CX + 6):
        if x in (CX - 3, CX + 3):
            continue
        put(x, 133, 4971, "snow_block", S["lights"])
    # snow caps on the annex roof corners
    for x, z in ((4965, 4992), (4976, 4992), (4965, 5003), (4976, 5003)):
        put(x, 155, z, "snow_block", S["lights"])


# ============================================================================
# MAIN BUILD
# ============================================================================
def build() -> None:
    build_drum()
    build_facade()
    build_south_entrance()
    build_clock()
    build_terrace()
    build_pods()
    build_dome()
    build_crown()
    build_bow()
    build_cross()
    build_bridge()
    build_west_annex()
    build_east_tower()
    build_finishing()


# ============================================================================
# OUTPUT
# ============================================================================
def write_blocks_csv(path: str) -> int:
    items = sorted(BLOCKS.items(), key=lambda kv: (kv[0][1], kv[0][0], kv[0][2]))
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["x", "y", "z", "block", "section"])
        for (x, y, z), (b, sec) in items:
            w.writerow([x, y, z, b, sec])
    return len(items)


def write_dump(path: str) -> None:
    xs = [p[0] for p in BLOCKS]
    ys = [p[1] for p in BLOCKS]
    zs = [p[2] for p in BLOCKS]
    x0, x1 = min(xs), max(xs)
    y0, y1 = min(ys), max(ys)
    z0, z1 = min(zs), max(zs)
    with gzip.open(path, "wt", encoding="utf-8", compresslevel=6) as f:
        f.write(f"MCMDUMP 1 {x1 - x0 + 1} {y1 - y0 + 1} {z1 - z0 + 1}\n")
        for (x, y, z), (b, _) in sorted(BLOCKS.items(), key=lambda kv: (kv[0][2], kv[0][1], kv[0][0])):
            f.write(f"{x - x0} {y - y0} {z - z0} minecraft:{b}\n")


def commands_for_section(sec: str) -> list[str]:
    cells = [(x, y, z, b) for (x, y, z), (b, s) in BLOCKS.items() if s == sec]
    cells.sort(key=lambda c: (c[1], c[2], c[0]))
    cmds = []
    i = 0
    while i < len(cells):
        x, y, z, b = cells[i]
        # run along +x with the same block and same section
        j = i + 1
        while (j < len(cells) and cells[j][1] == y and cells[j][2] == z
               and cells[j][3] == b and cells[j][0] == cells[j - 1][0] + 1):
            j += 1
        if j - i >= 2:
            cmds.append(f"fill {x} {y} {z} {cells[j-1][0]} {y} {z} minecraft:{b}")
        else:
            cmds.append(f"setblock {x} {y} {z} minecraft:{b}")
        i = j
    return cmds


def write_functions(base: str) -> list[tuple[str, int]]:
    outdir = os.path.join(base, "functions", "lbr2")
    os.makedirs(outdir, exist_ok=True)
    for name in os.listdir(outdir):
        if name.endswith(".mcfunction"):
            os.remove(os.path.join(outdir, name))
    summary = []
    load_lines = ["# LBR-2 festive Decision Dome - run in order.", ""]
    chunk_no = 0
    for sec in ORDER:
        cmds = commands_for_section(sec)
        if not cmds:
            continue
        chunks = [cmds[i:i + 8000] for i in range(0, len(cmds), 8000)]
        for k, chunk in enumerate(chunks):
            name = f"part_{sec}" + (f"_{k+1}" if len(chunks) > 1 else "")
            fn = os.path.join(outdir, name + ".mcfunction")
            with open(fn, "w", encoding="utf-8", newline="\n") as f:
                f.write(f"# LBR-2 {sec} chunk {k+1}/{len(chunks)} ({len(chunk)} commands)\n")
                for c in chunk:
                    f.write(c + "\n")
            load_lines.append(f"function lbr2:{name}")
            summary.append((name, len(chunk)))
            chunk_no += 1
    with open(os.path.join(outdir, "00_load.mcfunction"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(load_lines) + "\n")

    # ready-to-drop datapack (1.21.1 data pack format 48)
    dp = os.path.join(base, "datapack")
    fdir = os.path.join(dp, "data", "lbr2", "function")
    os.makedirs(fdir, exist_ok=True)
    for name in os.listdir(fdir):
        if name.endswith(".mcfunction"):
            os.remove(os.path.join(fdir, name))
    with open(os.path.join(dp, "pack.mcmeta"), "w", encoding="utf-8", newline="\n") as f:
        f.write('{\n  "pack": {\n    "pack_format": 48,\n'
                '    "description": "LBR-2 festive Decision Dome (MCC lobby replica)"\n  }\n}\n')
    for name in os.listdir(outdir):
        if name.endswith(".mcfunction"):
            with open(os.path.join(outdir, name), "rb") as src, open(os.path.join(fdir, name), "wb") as dst:
                dst.write(src.read())
    return summary


def main() -> None:
    base = os.path.dirname(os.path.abspath(__file__))
    build()
    n = write_blocks_csv(os.path.join(base, "blocks.csv"))
    write_dump(os.path.join(base, "preview.mcmdump.gz"))
    funcs = write_functions(base)

    xs = [p[0] for p in BLOCKS]
    ys = [p[1] for p in BLOCKS]
    zs = [p[2] for p in BLOCKS]
    print(f"blocks: {n:,}")
    print(f"bounds: x {min(xs)}..{max(xs)}  y {min(ys)}..{max(ys)}  z {min(zs)}..{max(zs)}")
    print(f"functions: {len(funcs)} + 00_load")
    types = defaultdict(int)
    for (b, sec) in BLOCKS.values():
        types[b] += 1
    print(f"block types: {len(types)}")
    for b, c in sorted(types.items(), key=lambda kv: -kv[1])[:18]:
        print(f"  {b:34s} {c:6,}")
    print("section counts:")
    sc = defaultdict(int)
    for (b, sec) in BLOCKS.values():
        sc[sec] += 1
    for sec in ORDER:
        if sc[sec]:
            print(f"  {sec:18s} {sc[sec]:6,}")


if __name__ == "__main__":
    main()
