#!/usr/bin/env python3
"""
LBR-3 -- MCC lobby replica: THE CHAMPS mountain, giant blue globe, outer terraces, water edge.

Site : world mcc_lobby_official, new test hub, C = (5000, 120, 5000).
Rule : this generator only ever writes y >= 121.  The y = 120 surface belongs to LBR-1.
       Portal pads (19x19 at r=72) and LBR-1's paths are never touched.

Layout (LBR-1 correction of 2026-11-25):
    mountain  centre (4880, 120, 5015)  ~55 x 40, height ~45, sign faces EAST
    globe     centre (5000, 132, 5105)  r 12, on its own plinth outside the south rim
    terraces  rim arcs between the portal pads, water pool along the rim

Outputs (all in this folder):
    blocks.csv                   x,y,z,block  (absolute world coordinates)
    functions/lbr3_*.mcfunction  chunked vanilla commands
    lbr3_all.mcfunction           call order for a datapack namespace
    lbr3_preview.mcmdump.gz       dump for tools/render_preview.py
    preview_top.png, preview_iso.png

Run:  python build_lbr3.py
"""

import gzip
import math
import os
import random
from collections import OrderedDict

HERE = os.path.dirname(os.path.abspath(__file__))
FUNC_DIR = os.path.join(HERE, "functions")

BASE_Y = 121                 # lowest y the normal build may write
SKIRT_BOTTOM = 90            # the rock skirt under the headland goes down to here
GROUND_Y = 120               # LBR-1 terrain top
CX, CZ = 5000, 5000          # hub centre


# ============================================================================
# maths helpers
# ============================================================================
def clamp(v, lo, hi):
    return lo if v < lo else hi if v > hi else v


def smooth(t):
    return t * t * (3.0 - 2.0 * t)


def _h3(x, y, z, seed=0):
    n = (int(x) * 374761393 + int(y) * 668265263 + int(z) * 2147483647 + seed * 144665) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    n ^= n >> 16
    return (n & 0xFFFFFF) / 16777215.0


def vnoise3(x, y, z, seed=0):
    xi, yi, zi = math.floor(x), math.floor(y), math.floor(z)
    u, v, w = smooth(x - xi), smooth(y - yi), smooth(z - zi)
    c = {}
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                c[(dx, dy, dz)] = _h3(xi + dx, yi + dy, zi + dz, seed)
    x00 = c[(0, 0, 0)] * (1 - u) + c[(1, 0, 0)] * u
    x10 = c[(0, 1, 0)] * (1 - u) + c[(1, 1, 0)] * u
    x01 = c[(0, 0, 1)] * (1 - u) + c[(1, 0, 1)] * u
    x11 = c[(0, 1, 1)] * (1 - u) + c[(1, 1, 1)] * u
    return (x00 * (1 - v) + x10 * v) * (1 - w) + (x01 * (1 - v) + x11 * v) * w


def vnoise2(x, z, seed=0):
    return vnoise3(x, 0.5, z, seed)


def fbm2(x, z, seed=0, oct=3):
    total, amp, norm, f = 0.0, 1.0, 0.0, 1.0
    for i in range(oct):
        total += amp * vnoise2(x * f, z * f, seed + i * 17)
        norm += amp
        amp *= 0.5
        f *= 2.07
    return total / norm


# ============================================================================
# section / block bookkeeping
# ============================================================================
_SECTIONS = []
_CUR = [None, None]
SKIPPED = {}


def begin(name):
    d = {}
    _SECTIONS.append((name, d))
    _CUR[0], _CUR[1] = name, d
    return d


def put(x, y, z, b):
    x, y, z = int(x), int(y), int(z)
    if y < BASE_Y:
        return
    if in_keepout(x, z):
        SKIPPED[_CUR[0]] = SKIPPED.get(_CUR[0], 0) + 1
        return
    _CUR[1][(x, y, z)] = b


def put_low(x, y, z, b):
    """Underside rock (y 90..120).  Only the rock skirt uses this."""
    x, y, z = int(x), int(y), int(z)
    if y < SKIRT_BOTTOM or y > GROUND_Y:
        return
    if in_keepout(x, z):
        SKIPPED[_CUR[0]] = SKIPPED.get(_CUR[0], 0) + 1
        return
    _CUR[1][(x, y, z)] = b


# ============================================================================
# keep-out guards (other agents' coordinates)
# ============================================================================
PAD_HALF = 10.5              # 19x19 pads at r=72, +1 block clearance
PADS = []
for _k in range(8):
    _a = math.radians(_k * 45.0)
    PADS.append((CX + 72 * math.cos(_a), CZ + 72 * math.sin(_a)))

GX, GY, GZ = 5000, 133, 5105          # globe centre (r=12, bottom at y=121)
GR = 12
GEARTH = 10.2

# --- mountain extents -------------------------------------------------------
MX0, MX1 = 4853, 4907       # 55 wide (the sign runs along x)
MZ0, MZ1 = 4995, 5035       # 40 deep
MSX, MSZ = 4880, 5015       # mountain centre
WALL_X0, WALL_X1 = 4861, 4899   # 39 wide south quarry face
WALL_Z0, WALL_Z1 = 5033, 5035
SIGN_CX = 4880


def in_keepout(x, z):
    for px, pz in PADS:
        if abs(x - px) <= PAD_HALF and abs(z - pz) <= PAD_HALF:
            return True
    return False


# ============================================================================
# 1. THE CHAMPS MOUNTAIN  (headland west of the rim, sign faces SOUTH)
# ============================================================================
LOBES = [
    (4888, 5012, 43.0),   # summit, right behind the sign
    (4880, 5002, 40.0),
    (4896, 5008, 40.0),
    (4872, 5012, 38.0),
    (4876, 4998, 34.0),
    (4894, 5024, 34.0),
    (4861, 5013, 31.0),
    (4902, 5002, 35.0),
    (4870, 5026, 30.0),
]
LOBE_SLOPE = 1.05
LOBE_TOP = 45.0


def wall_h(x):
    """Quarry-face profile of the south wall (32 at the ends, 38.5 in the middle)."""
    t = clamp((x - SIGN_CX) / 19.0, -1.0, 1.0)
    return 32.0 + 6.5 * (1.0 - t * t)


def inside_mass(x, z):
    """Ragged rounded rectangle: 55 x 40, corners eaten by noise."""
    dx = abs(x - MSX)
    dz = abs(z - MSZ)
    qx = max(0.0, dx - 19.0)
    qz = max(0.0, dz - 12.0)
    d = math.hypot(qx, qz)
    edge = 8.0 + (fbm2(x * 0.22, z * 0.22, 881, 2) - 0.5) * 7.0
    return d <= edge


def edge_dist(x, z):
    """Approximate distance to the headland outline (negative outside)."""
    dx = abs(x - MSX)
    dz = abs(z - MSZ)
    qx = max(0.0, dx - 19.0)
    qz = max(0.0, dz - 12.0)
    d = math.hypot(qx, qz)
    return min(27.0 - dx, 20.0 - dz, 8.0 - d)


def mtn_height(x, z):
    if not inside_mass(x, z):
        return 0
    h = 0.0
    for lx, lz, lh in LOBES:
        h = max(h, lh - math.hypot(x - lx, z - lz) * LOBE_SLOPE)

    # the mass leans up from the quarry wall behind the sign
    if z < WALL_Z0 and WALL_X0 - 2 <= x <= WALL_X1 + 2:
        h = max(h, wall_h(x) - (WALL_Z0 - z) * 0.30)

    rg = 1.0 - abs(2.0 * fbm2(x * 0.115, z * 0.115, 77, 3) - 1.0)
    h += (rg ** 2.4) * 4.0 * clamp(h / 24.0, 0.0, 1.0)
    crag = max(0.0, vnoise2(x * 0.50, z * 0.50, 555) - 0.48) * 7.0
    h += crag * clamp((h - 24.0) / 10.0, 0.0, 1.0)
    h += (fbm2(x * 0.36, z * 0.36, 101, 3) - 0.5) * 4.0 * clamp(h / 16.0, 0.0, 1.0)
    h += (vnoise2(x * 1.05, z * 1.05, 111) - 0.5) * 2.2

    # rounded shoulders instead of a sheer box
    dd = edge_dist(x, z)
    if not (WALL_X0 <= x <= WALL_X1 and WALL_Z0 <= z <= WALL_Z1):
        h *= 0.42 + 0.58 * smooth(clamp(dd / 7.0, 0.0, 1.0))

    if WALL_X0 <= x <= WALL_X1 and WALL_Z0 <= z <= WALL_Z1:
        h = wall_h(x)
    return int(round(clamp(h, 0, 45)))


ROCK_BANDS = [
    (0.00, "minecraft:blackstone"),
    (0.28, "minecraft:polished_blackstone"),
    (0.38, "minecraft:deepslate"),
    (0.50, "minecraft:cobbled_deepslate"),
    (0.58, "minecraft:deepslate_tiles"),
    (0.66, "minecraft:tuff"),
    (0.72, "minecraft:basalt"),
    (0.77, "minecraft:smooth_basalt"),
    (0.83, "minecraft:gray_concrete"),
    (0.87, "minecraft:deepslate_bricks"),
    (0.90, "minecraft:calcite"),
    (0.93, "minecraft:dripstone_block"),
    (1.01, "minecraft:blackstone"),
]


def rock(x, y, z):
    m = vnoise3(x * 0.15, y * 0.075, z * 0.15, 201)
    s = vnoise3(x * 0.33, y * 0.70, z * 0.33, 202)
    t = clamp(0.5 + (m * 0.60 + s * 0.40 - 0.5) * 1.8, 0.0, 1.0)
    if y <= 123 and t < 0.60:
        return "minecraft:deepslate"
    for thr, blk in ROCK_BANDS:
        if t < thr:
            return blk
    return "minecraft:blackstone"


def heightfield():
    H = {}
    for x in range(MX0, MX1 + 1):
        for z in range(MZ0, MZ1 + 1):
            H[(x, z)] = mtn_height(x, z)
    return H


def slope_of(H, x, z):
    h = H[(x, z)]
    return max(abs(h - H.get((x + dx, z + dz), h)) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def build_mountain_mass():
    begin("lbr3_01_mountain_mass")
    H = heightfield()
    for x in range(MX0, MX1 + 1):
        for z in range(MZ0, MZ1 + 1):
            h = H[(x, z)]
            if h <= 0:
                continue
            nbh = [H.get((x + dx, z + dz), 0) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            lo = max(BASE_Y, 121 + max(0, min(h - 4, min(nbh) - 1)))
            for y in range(lo, GROUND_Y + h + 1):
                put(x, y, z, rock(x, y, z))


def build_mountain_detail():
    begin("lbr3_02_mountain_detail")
    H = heightfield()
    on_wall = lambda x, z: (z >= WALL_Z0 - 1 and WALL_X0 - 1 <= x <= WALL_X1 + 1)

    # --- snow: coherent drifts on ledges, nothing on the sheer faces --------
    for (x, z), h in H.items():
        if h < 10 or on_wall(x, z):
            continue
        if slope_of(H, x, z) > 1:
            continue
        top = GROUND_Y + h
        patch = fbm2(x * 0.13, z * 0.13, 301, 3) + h / 150.0
        if patch > 0.605:
            put(x, top, z, "minecraft:snow_block")
        elif patch > 0.56:
            put(x, top, z, "minecraft:snow[layers=1]")

    # --- pinnacles bursting out of the summit bench -------------------------
    PINNACLES = [
        (4894, 5010, 5), (4888, 5004, 7), (4882, 5018, 6), (4892, 5022, 5),
        (4874, 5007, 4), (4870, 5024, 4), (4866, 5014, 5), (4880, 5028, 4),
    ]
    for (px, pz, ph) in PINNACLES:
        base = H.get((px, pz), 0)
        if base < 30:
            continue
        top = GROUND_Y + min(base + ph, 45)
        for i, y in enumerate(range(GROUND_Y + base + 1, top + 1)):
            r = 1 if i < 2 else 0
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if abs(dx) + abs(dz) > r:
                        continue
                    put(px + dx, y, pz + dz, rock(px + dx, y, pz + dz))

    # --- rock slabs breaking up the smooth flanks ---------------------------
    for i in range(22):
        a = _h3(i, 17, 3, 351) * math.pi * 2
        rad = 8 + _h3(i, 19, 5, 352) * 15
        x = int(MSX + math.cos(a) * rad * 1.3)
        z = int(MSZ + math.sin(a) * rad)
        if not (MX0 + 2 <= x <= MX1 - 2 and MZ0 + 2 <= z <= MZ1 - 2):
            continue
        hh = H.get((x, z), 0)
        if hh < 8:
            continue
        top = GROUND_Y + hh
        for dx in range(2):
            for dz in range(2):
                if _h3(x + dx, 21, z + dz, 353) > 0.45:
                    continue
                put(x + dx, top, z + dz, rock(x + dx, top, z + dz))
                if _h3(x + dx, 23, z + dz, 354) > 0.72:
                    put(x + dx, top + 1, z + dz, rock(x + dx, top + 1, z + dz))

    # --- scree sitting on the rim ledge east of the headland ---------------
    for i in range(18):
        x = int(4896 + _h3(i, 7, 3, 41) * 9)
        z = int(4999 + _h3(i, 9, 5, 42) * 32)
        hh = H.get((x, z), 0)
        if hh < 2:
            continue
        y = GROUND_Y + hh
        put(x, y, z, rock(x, y, z))
        if _h3(i, 12, 4, 44) > 0.6:
            put(x, y + 1, z, "minecraft:snow_block")

    # --- wind-bent spruces on the slopes ------------------------------------
    for (px, pz) in [(4877, 5009), (4886, 5000), (4870, 5020), (4892, 5027)]:
        h = H.get((px, pz), 0)
        if h < 3 or h > 38:
            continue
        pine(px, GROUND_Y + h + 1, pz, 4 + int(_h3(px, pz, 1, 51) * 2))

    # --- snow drift against the wall ---------------------------------------
    for x in range(WALL_X0, WALL_X1 + 2):
        if _h3(x, 3, 9, 55) > 0.55:
            put(x, GROUND_Y + 1, WALL_Z1 + 1, "minecraft:snow[layers=2]")


def pine(x, y, z, height):
    for i in range(height):
        put(x, y + i, z, "minecraft:spruce_log[axis=y]")
        if i >= 1 and i % 2 == 0:
            r = 2 if i < height - 2 else 1
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if abs(dx) + abs(dz) > r + 1 or (dx == 0 and dz == 0):
                        continue
                    put(x + dx, y + i, z + dz,
                        "minecraft:spruce_leaves[distance=3,persistent=true]")
    put(x, y + height, z, "minecraft:spruce_leaves[distance=1,persistent=true]")


# ============================================================================
# 1b. ROCK SKIRT  (headland underside, y 90..120, outside the island rim)
# ============================================================================
SKIRT_RECEDE = 0.34          # blocks inward per block down


def skirt_edge(x, z):
    """Distance inside the skirt footprint; -1 = not part of the skirt."""
    if inside_mass(x, z):
        return edge_dist(x, z)
    # rock shelf that carries the gate porch
    if 4862 <= x <= 4898 and MZ1 < z <= 5044:
        return min(x - 4862, 4898 - x, z - MZ1 - 1, 5044 - z) + 0.6
    return -1.0


def build_rock_skirt():
    begin("lbr3_11_rock_skirt")
    for y in range(SKIRT_BOTTOM, GROUND_Y + 1):
        # sheer cliff down to y=104, then a keel that draws in
        d = max(0.0, (104 - y) * 0.45)
        for x in range(MX0 - 3, MX1 + 6):
            for z in range(MZ0 - 3, 5048):
                if math.hypot(x - CX, z - CZ) < 96.0:
                    continue          # LBR-1 owns everything inside the rim
                e = skirt_edge(x, z)
                if e < 0:
                    continue
                # ragged, noise-broken face instead of a machined cylinder
                jitter = (vnoise2(x * 0.30, z * 0.30, 950) - 0.5) * 2.4 \
                    + (vnoise2(x * 0.75, z * 0.75, 951) - 0.5) * 1.2
                if abs(e - d - jitter * 0.5) <= 0.6:
                    put_low(x, y, z, rock(x, y, z))
    # keel floor so it does not read hollow from underneath
    e_min = max(0.0, (104 - SKIRT_BOTTOM) * 0.45) - 0.6
    for x in range(MX0 - 3, MX1 + 6):
        for z in range(MZ0 - 3, 5048):
            if math.hypot(x - CX, z - CZ) < 96.0:
                continue
            if skirt_edge(x, z) >= e_min:
                put_low(x, SKIRT_BOTTOM, z, rock(x, SKIRT_BOTTOM, z))


# ============================================================================
# 2. THE SIGN  "THE CHAMPS"  (east face, reads toward the plaza)
# ============================================================================
FONT = {
    "T": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."],
    "H": ["#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "E": ["#####", "#....", "#....", "####.", "#....", "#....", "#####"],
    "C": [".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."],
    "A": [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "M": ["#...#", "##.##", "#.#.#", "#...#", "#...#", "#...#", "#...#"],
    "P": ["####.", "#...#", "#...#", "####.", "#....", "#....", "#...."],
    "S": [".####", "#....", "#....", ".###.", "....#", "....#", "####."],
}

BOARD_Y0, BOARD_Y1 = 130, 151
BOARD_Z = WALL_Z1 + 1        # 5036
LETTER_Z = BOARD_Z + 1       # 5037
BOARD_X0, BOARD_X1 = 4861, 4899


def text_cells(text, x0, y0):
    """Letter bitmap in (x, y).  Increasing x reads left-to-right on the +z face."""
    cells = set()
    px = 0
    for ch in text:
        for r, line in enumerate(FONT[ch]):
            for c, v in enumerate(line):
                if v == "#":
                    cells.add((x0 + px + c, y0 + 6 - r))
        px += 6
    return cells


def build_sign():
    begin("lbr3_04_sign")
    for x in range(BOARD_X0, BOARD_X1 + 1):
        for y in range(BOARD_Y0, BOARD_Y1 + 1):
            edge = (x in (BOARD_X0, BOARD_X0 + 1, BOARD_X1 - 1, BOARD_X1)
                    or y in (BOARD_Y0, BOARD_Y0 + 1, BOARD_Y1 - 1, BOARD_Y1))
            if edge:
                put(x, y, BOARD_Z, "minecraft:polished_blackstone_bricks")
            else:
                n = vnoise2(x * 0.55, y * 0.55, 401)
                put(x, y, BOARD_Z,
                    "minecraft:red_concrete" if n < 0.60 else "minecraft:nether_bricks")
    for x in (BOARD_X0 + 2, BOARD_X1 - 2):
        for y in (BOARD_Y0 + 2, BOARD_Y1 - 2):
            put(x, y, BOARD_Z, "minecraft:gold_block")
    for x in range(BOARD_X0 + 2, BOARD_X1 - 1):
        put(x, BOARD_Y0 + 1, BOARD_Z, "minecraft:gold_block")

    letters = text_cells("CHAMPS", 4863, 134) | text_cells("THE", 4872, 143)
    outline = set()
    for (x, y) in letters:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                outline.add((x + dx, y + dy))
    outline -= letters
    for (x, y) in outline:
        put(x, y, LETTER_Z, "minecraft:black_concrete")
    for (x, y) in letters:
        put(x, y, LETTER_Z, "minecraft:white_concrete")

    # --- gold crown above the board -----------------------------------------
    crown = [
        "..#.....#.....#..",
        ".###...###...###.",
        ".###...###...###.",
        "#################",
        "#################",
        "##.###.###.###.##",
        "#################",
    ]
    for r, line in enumerate(crown):
        y = 152 + 6 - r
        for c, v in enumerate(line):
            if v != "#":
                continue
            blk = "minecraft:gold_block"
            if r in (1, 2) and c in (8, 16):
                blk = "minecraft:red_concrete"
            put(4872 + c, y, LETTER_Z, blk)
    for gx in (4875, 4880, 4885):
        put(gx, 158, LETTER_Z, "minecraft:red_concrete")

    # --- rock buttresses framing the panel ----------------------------------
    for x in (BOARD_X0 - 1, BOARD_X1 + 1):
        for y in range(BOARD_Y0, BOARD_Y1 + 2):
            put(x, y, BOARD_Z, rock(x, y, BOARD_Z))
        put(x, BOARD_Y1 + 2, BOARD_Z, "minecraft:snow[layers=1]")


# ============================================================================
# 3. THE GATE  (copper / terracotta arch + lit niche, faces south)
# ============================================================================
PORCH_X0, PORCH_X1 = 4865, 4895      # 31 wide
PORCH_Z0, PORCH_Z1 = 5036, 5040      # 5 deep, protruding south
PORCH_Y0, PORCH_Y1 = 121, 128
ARCH_CX, ARCH_SPRING, ARCH_R = 4880, 123, 5.6


def arch_top(x):
    k = abs(x - ARCH_CX)
    if k > ARCH_R:
        return ARCH_SPRING - 1
    return int(ARCH_SPRING + math.sqrt(max(0.0, ARCH_R * ARCH_R - k * k)))


def build_gate():
    begin("lbr3_03_gate")
    # --- rock shelf / apron in front of the gate ----------------------------
    for x in range(PORCH_X0 - 2, PORCH_X1 + 3):
        for z in range(PORCH_Z1 + 1, PORCH_Z1 + 4):
            put(x, GROUND_Y + 1, z,
                "minecraft:stone_bricks" if (x + z) % 5 else "minecraft:mossy_stone_bricks")
    for x in range(PORCH_X0 + 3, PORCH_X1 - 2):
        put(x, GROUND_Y + 1, PORCH_Z1 + 2, "minecraft:smooth_stone")
        put(x, GROUND_Y + 1, PORCH_Z1 + 3, "minecraft:smooth_stone")

    # --- portico body --------------------------------------------------------
    for x in range(PORCH_X0, PORCH_X1 + 1):
        top_open = arch_top(x)
        for z in range(PORCH_Z0, PORCH_Z1 + 1):
            open_cell = (PORCH_X0 + 6 <= x <= PORCH_X1 - 6)
            for y in range(PORCH_Y0, PORCH_Y1 + 1):
                inside = open_cell and y <= top_open and z > PORCH_Z0
                if inside:
                    if y == PORCH_Y0:
                        put(x, y, z, "minecraft:red_concrete")
                    continue
                if z == PORCH_Z0 and open_cell and y <= top_open:
                    n = (x + y) % 7
                    put(x, y, z, "minecraft:gold_block" if n == 0 else
                        "minecraft:red_concrete" if n == 3 else
                        "minecraft:polished_blackstone")
                    continue
                if z == PORCH_Z1:
                    put(x, y, z, "minecraft:orange_terracotta" if y % 5 else
                        "minecraft:copper_block")
                elif y == PORCH_Y1:
                    put(x, y, z, "minecraft:orange_terracotta")
                else:
                    put(x, y, z, "minecraft:copper_block" if (x + y) % 4 == 0
                        else "minecraft:exposed_copper")

    for x in range(PORCH_X0 + 5, PORCH_X1 - 4):
        put(x, arch_top(x) + 1, PORCH_Z1, "minecraft:copper_block")
    for x in (ARCH_CX - 1, ARCH_CX, ARCH_CX + 1):
        put(x, PORCH_Y1, PORCH_Z1, "minecraft:gold_block")
    for x in (PORCH_X0 + 2, PORCH_X1 - 2):
        put(x, PORCH_Y1, PORCH_Z1, "minecraft:cut_copper")
        put(x, ARCH_SPRING + 3, PORCH_Z1, "minecraft:cut_copper")

    # --- roof + snow ---------------------------------------------------------
    for x in range(PORCH_X0 - 1, PORCH_X1 + 2):
        for z in range(PORCH_Z0, PORCH_Z1 + 1):
            put(x, PORCH_Y1 + 1, z, "minecraft:orange_terracotta")
            if _h3(x, 7, z, 421) > 0.30:
                put(x, PORCH_Y1 + 2, z, "minecraft:snow_block")
            else:
                put(x, PORCH_Y1 + 2, z, "minecraft:snow[layers=2]")
    for x in (PORCH_X0, PORCH_X1):
        put(x, PORCH_Y1 + 3, PORCH_Z1, "minecraft:gold_block")

    # --- lanterns ------------------------------------------------------------
    for x in (ARCH_CX - 3, ARCH_CX + 3):
        put(x, 124, PORCH_Z1 - 1, "minecraft:lantern[hanging=true]")
    for x in (ARCH_CX - 5, ARCH_CX + 5):
        put(x, 122, PORCH_Z1 - 1, "minecraft:sea_lantern")

    # --- shelf lip ----------------------------------------------------------
    for x in range(PORCH_X0 - 1, PORCH_X1 + 2):
        put(x, GROUND_Y + 1, PORCH_Z1 + 2, "minecraft:smooth_stone")
        put(x, GROUND_Y + 1, PORCH_Z1 + 3, "minecraft:smooth_stone_slab[type=bottom]")


# ============================================================================
# 4. THE GLOBE  (5000, 133, 5105)
# ============================================================================
CONTINENTS = [
    (42, 25, 27, 1.00),    # Eurasia
    (56, 65, 20, 0.65),
    (22, 78, 15, 0.75),    # India / SE Asia
    (44, -100, 26, 1.00),  # North America
    (-13, -60, 21, 0.95),  # South America
    (5, 20, 21, 0.85),     # Africa
    (-25, 134, 17, 0.75),  # Australia
    (-5, 115, 11, 0.55),   # Indonesia
    (73, -42, 11, 0.55),   # Greenland
    (76, 105, 14, 0.45),   # north Siberia
]


def continent(lat, lon):
    la, lo = math.radians(lat), math.radians(lon)
    best = 0.0
    for (blat, blon, rad, w) in CONTINENTS:
        b = math.radians(blat)
        dd = math.acos(clamp(math.sin(la) * math.sin(b) +
                             math.cos(la) * math.cos(b) * math.cos(lo - math.radians(blon)),
                             -1.0, 1.0))
        best += w * math.exp(-((math.degrees(dd) / rad) ** 2) * 1.5)
    return best


def build_globe_earth():
    begin("lbr3_05_globe_earth")
    for dx in range(-11, 12):
        for dy in range(-11, 12):
            for dz in range(-11, 12):
                d = math.sqrt(dx * dx + dy * dy + dz * dz)
                if d > GEARTH or d < GEARTH - 1.5:
                    continue
                lat = math.degrees(math.asin(clamp(dy / GEARTH, -1, 1)))
                lon = math.degrees(math.atan2(dz, dx))
                sc = continent(lat, lon)
                if sc > 0.44:
                    v = vnoise3(dx * 1.5, dy * 1.5, dz * 1.5, 604)
                    blk = ("minecraft:green_concrete" if v < 0.72 else
                           "minecraft:lime_concrete" if v < 0.86 else
                           "minecraft:moss_block")
                    if sc < 0.49:
                        blk = "minecraft:sand"
                else:
                    v = vnoise3(dx * 1.2, dy * 1.2, dz * 1.2, 605)
                    blk = ("minecraft:blue_concrete" if v < 0.74 else
                           "minecraft:cyan_concrete")
                if abs(lat) > 70:
                    blk = "minecraft:white_concrete"
                put(GX + dx, GY + dy, GZ + dz, blk)


def build_globe_shell():
    begin("lbr3_06_globe_shell")
    for dx in range(-GR, GR + 1):
        for dy in range(-GR, GR + 1):
            for dz in range(-GR, GR + 1):
                d = math.sqrt(dx * dx + dy * dy + dz * dz)
                if d > GR or d < GR - 1.15:
                    continue
                lat = math.degrees(math.asin(clamp(dy / GR, -1, 1)))
                lon = math.degrees(math.atan2(dz, dx))
                rib = False
                for k in range(8):
                    dd = abs((lon - k * 45.0 + 180) % 360 - 180)
                    if dd < 3.0 and abs(lat) < 80:
                        rib = True
                if abs(abs(lat) - 32) < 2.2 or abs(lat) > 84:
                    rib = True
                if rib:
                    blk = "minecraft:cyan_concrete"
                else:
                    sc = continent(lat, lon)
                    if abs(lat) > 72:
                        blk = "minecraft:white_stained_glass"
                    elif sc > 0.44:
                        blk = ("minecraft:green_stained_glass" if sc > 0.52 else
                               "minecraft:lime_stained_glass")
                    else:
                        v = vnoise3(dx * 0.85, dy * 0.85, dz * 0.85, 611)
                        blk = ("minecraft:light_blue_stained_glass" if v < 0.66 else
                               "minecraft:cyan_stained_glass")
                put(GX + dx, GY + dy, GZ + dz, blk)

    # --- white cloud belt, tilted ~9 degrees --------------------------------
    tilt = math.radians(9.0)
    for dx in range(-GR - 3, GR + 4):
        for dy in range(-GR - 3, GR + 4):
            for dz in range(-GR - 3, GR + 4):
                d = math.sqrt(dx * dx + dy * dy + dz * dz)
                if d < GR - 0.4 or d > GR + 2.6:
                    continue
                ny = dy * math.cos(tilt) - dz * math.sin(tilt)
                band = abs(ny) / max(d, 0.001)
                if band > 0.14:
                    continue
                n = vnoise3(dx * 0.5, dy * 0.5, dz * 0.5, 621)
                if band > 0.09 and n < 0.28:
                    continue
                put(GX + dx, GY + dy, GZ + dz, "minecraft:white_concrete")

    # --- detached puffs -----------------------------------------------------
    for i in range(26):
        a = _h3(i, 1, 2, 631) * math.pi * 2
        rr = GR + 2.5 + _h3(i, 3, 4, 632) * 1.1
        t = _h3(i, 5, 6, 633) * 1.1 - 0.55
        x = int(GX + math.cos(a) * rr)
        z = int(GZ + math.sin(a) * rr)
        y = int(GY + math.sin(math.radians(9.0)) * rr * math.cos(a) * 0.45 + t)
        put(x, y, z, "minecraft:white_concrete")
        if _h3(i, 7, 8, 634) > 0.5:
            put(x + (1 if x > GX else -1), y, z, "minecraft:white_concrete")


# ============================================================================
# 5. GLOBE PLINTH  (separate rock/stone platform outside the south rim)
# ============================================================================
PLAT_R = 17.5


def build_globe_base():
    begin("lbr3_07_globe_base")
    for dx in range(-19, 20):
        for dz in range(-19, 20):
            r = math.hypot(dx, dz)
            x, z = GX + dx, GZ + dz
            ang = int(round(math.degrees(math.atan2(dz, dx)))) % 360
            if r <= 12.2:
                put(x, BASE_Y, z, "minecraft:polished_deepslate")
                put(x, BASE_Y + 1, z, "minecraft:sea_lantern" if (ang // 9) % 3 == 0
                    else "minecraft:polished_deepslate")
            elif r <= 15.0:
                put(x, BASE_Y, z, "minecraft:smooth_stone")
                put(x, BASE_Y + 1, z, "minecraft:stone_bricks" if (ang // 4) % 3 else
                    "minecraft:smooth_stone")
            elif r <= PLAT_R:
                put(x, BASE_Y, z, "minecraft:stone_bricks")
                if r > PLAT_R - 1.2:
                    # craggy rock rim
                    hh = 1 + int(2 * vnoise2(x * 0.5, z * 0.5, 720))
                    for y in range(BASE_Y, BASE_Y + hh):
                        put(x, y, z, rock(x, y, z))
            elif r <= PLAT_R + 1.6:
                hh = 1 + int(2 * vnoise2(x * 0.5, z * 0.5, 721))
                for y in range(BASE_Y, BASE_Y + hh):
                    put(x, y, z, rock(x, y, z))

    # lanterns around the platform rim (open, no balustrade wall)
    for ang in range(16, 170, 8):
        ar = math.radians(ang)
        x = int(round(GX + (PLAT_R - 1.6) * math.cos(ar)))
        z = int(round(GZ + (PLAT_R - 1.6) * math.sin(ar)))
        put(x, BASE_Y + 2, z, "minecraft:lantern")

    # a shallow pool on the north side of the platform
    for dx in range(-6, 7):
        for dz in range(-15, -9):
            x, z = GX + dx, GZ + dz
            if abs(dx) + abs(dz) < 2:
                continue
            if math.hypot(dx, dz) > 12.5:
                continue
            put(x, BASE_Y, z, "minecraft:polished_deepslate")
            if abs(dx) <= 4 and -13 <= dz <= -11:
                put(x, BASE_Y + 1, z, "minecraft:water[level=0]")
    for i in range(14):
        x = GX - 4 + int(_h3(i, 2, 3, 731) * 9)
        z = GZ - 13 + int(_h3(i, 4, 5, 732) * 3)
        put(x, BASE_Y + 2, z, "minecraft:lily_pad")


# ============================================================================
# 6. OUTER TERRACES  (rim arcs clear of the portal pads)
# ============================================================================
ARCS = [(56.0, 82.0), (98.0, 124.0)]

LEVELS = [
    (85.4, 88.2, 123),
    (88.2, 90.2, 122),
    (90.2, 91.2, 121),
]


def polar(x, z):
    dx, dz = x - CX, z - CZ
    r = math.hypot(dx, dz)
    if r < 85.0 or r > 93.4:
        return None
    a = math.degrees(math.atan2(dz, dx))
    hit = False
    for (a0, a1) in ARCS:
        if a0 <= a <= a1:
            hit = True
    if not hit:
        return None
    if math.hypot(x - GX, z - GZ) < PLAT_R + 2.0:
        return None
    if MX0 - 2 <= x <= MX1 + 6 and MZ0 - 2 <= z <= MZ1 + 2:
        return None
    return r


def build_terraces():
    begin("lbr3_08_terraces")
    for x in range(CX - 100, CX + 100):
        for z in range(CZ - 100, CZ + 100):
            r = polar(x, z)
            if r is None:
                continue
            if r < 91.2:
                top = 121
                for a0, a1, ty in LEVELS:
                    if a0 <= r < a1:
                        top = ty
                for y in range(BASE_Y, top):
                    put(x, y, z, "minecraft:stone_bricks" if y < top - 1 else "minecraft:smooth_stone")
                n = vnoise2(x * 0.5, z * 0.5, 701)
                if top >= 123:
                    put(x, top, z, "minecraft:snow_block" if n > 0.52 else "minecraft:smooth_stone")
                elif top == 122:
                    put(x, top, z, "minecraft:snow[layers=1]" if n > 0.55 else "minecraft:smooth_stone")
                else:
                    put(x, top, z, "minecraft:dark_oak_planks" if n > 0.66 else "minecraft:smooth_stone")
            elif r < 91.9:
                for y in range(BASE_Y, BASE_Y + 2):
                    put(x, y, z, "minecraft:polished_deepslate")
                put(x, BASE_Y + 2, z, "minecraft:smooth_stone_slab[type=bottom]")
            elif r < 92.9:
                put(x, BASE_Y, z, "minecraft:sand")
                put(x, BASE_Y + 1, z, "minecraft:water[level=0]")
            else:
                for y in range(BASE_Y, BASE_Y + 2):
                    put(x, y, z, "minecraft:stone_bricks")
                put(x, BASE_Y + 2, z, "minecraft:snow_block" if _h3(x, 3, z, 705) > 0.45
                    else "minecraft:smooth_stone")

    # a landing stair up to the top terrace from the plaza side
    for a in (64, 110):
        ar = math.radians(a)
        for i in range(3):
            rr = 84.4 - 3 + i
            top = BASE_Y + i
            for w in (-2.0, -1.0, 0.0, 1.0, 2.0):
                x = int(round(CX + rr * math.cos(ar) - w * math.sin(ar)))
                z = int(round(CZ + rr * math.sin(ar) + w * math.cos(ar)))
                if polar(x, z) is None and math.hypot(x - CX, z - CZ) < 85.0:
                    continue
                for y in range(BASE_Y, top):
                    put(x, y, z, "minecraft:stone_bricks")
                put(x, top, z, "minecraft:smooth_stone")

    # balustrade along the pool
    for a in list(range(57, 82, 6)) + list(range(99, 124, 6)):
        ar = math.radians(a)
        x = int(round(CX + 91.5 * math.cos(ar)))
        z = int(round(CZ + 91.5 * math.sin(ar)))
        if polar(x, z) is None:
            continue
        put(x, BASE_Y + 2, z, "minecraft:polished_deepslate")
        put(x, BASE_Y + 3, z, "minecraft:polished_deepslate")
        put(x, BASE_Y + 4, z, "minecraft:lantern")


# ============================================================================
# 7. WATER  (rim pool cascade + causeway to the globe platform)
# ============================================================================
CAUSEWAY = [(4966, 5087), (4974, 5090), (4982, 5094)]


def build_water():
    begin("lbr3_09_water")
    rng = random.Random(4321)

    # cascade from the top terrace into the rim pool
    for a in (104.0,):
        ar = math.radians(a)
        for rr, top in ((86.0, 123), (88.6, 123), (90.0, 122), (91.4, 122)):
            for w in (-1.0, 0.0, 1.0):
                x = int(round(CX + rr * math.cos(ar) - w * math.sin(ar)))
                z = int(round(CZ + rr * math.sin(ar) + w * math.cos(ar)))
                if polar(x, z) is None:
                    continue
                put(x, top, z, "minecraft:water[level=0]")
                for y in range(BASE_Y, top):
                    put(x, y, z, "minecraft:stone_bricks")

    # pool dressing
    for i in range(70):
        a = rng.uniform(98.0, 124.0)
        rr = rng.uniform(92.0, 92.8)
        x = int(round(CX + rr * math.cos(math.radians(a))))
        z = int(round(CZ + rr * math.sin(math.radians(a))))
        if polar(x, z) is None:
            continue
        q = rng.random()
        if q < 0.45:
            put(x, BASE_Y + 1, z, "minecraft:water[level=0]")
        elif q < 0.72:
            put(x, BASE_Y + 1, z, "minecraft:lily_pad")
        elif q < 0.9:
            put(x, BASE_Y, z, "minecraft:seagrass")
        else:
            put(x, BASE_Y + 1, z, "minecraft:water[level=0]")

    # --- causeway from the island rim to the globe platform -----------------
    for i in range(len(CAUSEWAY) - 1):
        ax, az = CAUSEWAY[i]
        bx, bz = CAUSEWAY[i + 1]
        steps = max(abs(bx - ax), abs(bz - az)) * 2
        for s in range(steps + 1):
            t = s / steps
            cx = ax + (bx - ax) * t
            cz = az + (bz - az) * t
            for w in (-2, -1, 0, 1, 2):
                x = int(round(cx))
                z = int(round(cz + w))
                put(x, BASE_Y, z, "minecraft:stone_bricks" if (x + z) % 3 else "minecraft:smooth_stone")
                if abs(w) == 2 and s % 4 == 0:
                    put(x, BASE_Y + 1, z, "minecraft:stone_brick_wall")
                    put(x, BASE_Y + 2, z, "minecraft:lantern")


# ============================================================================
# 8. FINAL SNOW TRIM
# ============================================================================
def build_snow_trim():
    begin("lbr3_10_snow_trim")
    # snow caps on the terrace lanterns
    for a in list(range(58, 82, 6)) + list(range(100, 124, 6)):
        ar = math.radians(a)
        x = int(round(CX + 91.5 * math.cos(ar)))
        z = int(round(CZ + 91.5 * math.sin(ar)))
        if polar(x, z) is None:
            continue
        put(x, BASE_Y + 5, z, "minecraft:snow[layers=1]")


# ============================================================================
# output
# ============================================================================
CHUNK_CMDS = 7000


def section_commands(blocks):
    rows = {}
    for (x, y, z), b in blocks.items():
        rows.setdefault((y, z), []).append((x, b))
    cmds = []
    for (y, z) in sorted(rows):
        row = sorted(rows[(y, z)])
        i = 0
        while i < len(row):
            x, b = row[i]
            j = i + 1
            while j < len(row) and row[j][1] == b and row[j][0] == row[j - 1][0] + 1:
                j += 1
            if j - i >= 2:
                cmds.append("fill %d %d %d %d %d %d %s" % (row[i][0], y, z, row[j - 1][0], y, z, b))
            else:
                cmds.append("setblock %d %d %d %s" % (x, y, z, b))
            i = j
    return cmds


def main():
    build_mountain_mass()
    build_mountain_detail()
    build_gate()
    build_sign()
    build_globe_earth()
    build_globe_shell()
    build_globe_base()
    build_terraces()
    build_water()
    build_snow_trim()
    build_rock_skirt()

    os.makedirs(FUNC_DIR, exist_ok=True)
    for old in os.listdir(FUNC_DIR):
        if old.endswith(".mcfunction"):
            os.remove(os.path.join(FUNC_DIR, old))
    merged = OrderedDict()
    call_order = []
    total = 0
    for name, blocks in _SECTIONS:
        for k, v in blocks.items():
            merged[k] = v
        total += len(blocks)
        cmds = section_commands(blocks)
        chunks, cur = [], []
        for c in cmds:
            cur.append(c)
            if len(cur) >= CHUNK_CMDS:
                chunks.append(cur)
                cur = []
        if cur:
            chunks.append(cur)
        for i, chunk in enumerate(chunks):
            fname = name if len(chunks) == 1 else "%s_%s" % (name, "abcdefgh"[i])
            with open(os.path.join(FUNC_DIR, fname + ".mcfunction"), "w", newline="\n") as f:
                f.write("\n".join(chunk) + "\n")
            call_order.append(fname)

    with open(os.path.join(HERE, "lbr3_all.mcfunction"), "w", newline="\n") as f:
        for n in call_order:
            f.write("function mcc_lobby:%s\n" % n)

    with open(os.path.join(HERE, "blocks.csv"), "w", newline="\n") as f:
        f.write("x,y,z,block\n")
        for (x, y, z), b in sorted(merged.items()):
            f.write("%d,%d,%d,%s\n" % (x, y, z, b))

    xs = [k[0] for k in merged]
    ys = [k[1] for k in merged]
    zs = [k[2] for k in merged]
    x0, x1 = min(xs), max(xs)
    y0, y1 = min(min(ys), GROUND_Y), max(ys)
    z0, z1 = min(zs), max(zs)

    # preview-only ground: a 6 block skirt around the built columns
    cols = set((k[0], k[2]) for k in merged)
    skirt = set()
    for (cx, cz) in cols:
        for dx in range(-6, 7):
            for dz in range(-6, 7):
                skirt.add((cx + dx, cz + dz))
    body = ["%d %d %d %s" % (x - x0, y - y0, z - z0, b) for (x, y, z), b in merged.items()]
    for (x, z) in sorted(skirt):
        if x0 <= x <= x1 and z0 <= z <= z1:
            if 4835 <= x <= 4926 and 4978 <= z <= 5056:
                continue            # the mountain has its own rock skirt
            n = vnoise2(x * 0.35, z * 0.35, 901)
            b = "minecraft:stone" if n < 0.74 else "minecraft:snow_block"
            body.append("%d %d %d %s" % (x - x0, GROUND_Y - y0, z - z0, b))
    with gzip.open(os.path.join(HERE, "lbr3_preview.mcmdump.gz"), "wt", newline="\n") as f:
        f.write("MCMDUMP 1 %d %d %d\n" % (x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1))
        f.write("\n".join(body) + "\n")

    print("sections:")
    for name, blocks in _SECTIONS:
        print("  %-28s %6d blocks" % (name, len(blocks)))
    print("  %-28s %6d" % ("TOTAL (with overlaps)", total))
    print("  %-28s %6d" % ("merged unique", len(merged)))
    print("functions: %d" % len(call_order))
    print("bounds x %d..%d  y %d..%d  z %d..%d" % (x0, x1, y0, y1, z0, z1))
    if SKIPPED:
        print("pad-guard skips:", SKIPPED)


if __name__ == "__main__":
    main()
