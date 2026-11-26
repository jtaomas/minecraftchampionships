#!/usr/bin/env python3
"""
V4-A -- MCC festive hub, base package.

Island base, terraces, water, entrance bridge, checkered / red-carpet axis,
rainbow plinth (no dome) and the two giant MCC entrance columns.

Origin           0,120,0        (y = 120 is the walkable ground surface)
Build box        x -150..150, z -150..150, y 90..210
Reserved         r < 28 at the centre: nothing above y = 120 (dome footprint)

Reference basis (files in build_v4/reference):
  MCCHub-S6.png   entrance, stone bridge + balustrade, giant red MCC columns,
                  white 7x7 MCC letters, white capital, gold crenellated crown,
                  yellow dome-island floor, red carpet stairs.
  MCCHub-SD.png   festive pass: snow, ice ledges, red bows on the columns,
                  team-coloured column row at the column feet.
  MCCHub-S4.png   plaza: red brick paving, cream planter terraces with hedges,
                  red/yellow MCC floor emblem, balustrade above a water channel,
                  three-step transition up to the bridge terrace.
  MCCHubSeason1   rainbow plinth: vertical rainbow bands in a dark stone rim.
  MainHub.png     yellow walkway + red bands + checkered axis.

Outputs (this folder):
  blocks.csv                       x,y,z,block,section (absolute world coords)
  functions/v4a_*.mcfunction       chunked vanilla 1.21.1 commands
  load.mcfunction                  ordered function calls (namespace v4a)
  preview.mcmdump.gz               dump for tools/render_preview.py
  preview_top.png, preview_iso.png renders
  stats.json                       counts used by STATUS.md / verify_v4a.py

Run:  python gen_v4a.py
"""

import gzip
import json
import math
import os
from collections import defaultdict, OrderedDict

HERE = os.path.dirname(os.path.abspath(__file__))
FUNC_DIR = os.path.join(HERE, "functions")

X_MIN, X_MAX = -150, 150
Z_MIN, Z_MAX = -150, 150
Y_MIN, Y_MAX = 90, 210
GROUND = 120

# ---------------------------------------------------------------------------
# geometry constants (all radii measured in the x/z plane around 0,0)
# ---------------------------------------------------------------------------
DOME_R = 28.0          # reserved dome footprint (other bot)
PLINTH_R0, PLINTH_R1 = 28.0, 30.0     # rainbow ring, 2 thick
PLINTH_TOP = 128
ISLE_R = 38.0          # dome island outer edge
MOAT_R1 = 52.0         # moat outer bank
WATER_Y = 118          # water surface
BRIDGE_HALF = 6        # deck x -6..6
BRIDGE_Z0, BRIDGE_Z1 = 38, 52
COL_X = 21             # giant column centre |x|
COL_Z = 55             # giant column centre z
PLAZA_X0, PLAZA_X1 = -38, 38
PLAZA_Z0, PLAZA_Z1 = 54, 112
EMBLEM_Z = 86
EMBLEM_R = 12.5

# island lobes: (cx, cz, r)
LOBES = [(0, 10, 96), (2, 100, 46), (-92, 0, 42), (92, 12, 42),
         (-70, 80, 38), (66, 88, 36), (0, -64, 34)]
COVES = [(-104, -46, 24), (112, 58, 22)]

# ---------------------------------------------------------------------------
# noise
# ---------------------------------------------------------------------------


def _h3(x, y, z, seed=0):
    n = (int(x) * 374761393 + int(y) * 668265263 + int(z) * 2147483647
         + seed * 144665) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    n ^= n >> 16
    return (n & 0xFFFFFF) / 16777215.0


def _smooth(t):
    return t * t * (3.0 - 2.0 * t)


def vnoise2(x, z, seed=0):
    xi, zi = math.floor(x), math.floor(z)
    u, v = _smooth(x - xi), _smooth(z - zi)
    a = _h3(xi, 0, zi, seed) * (1 - u) + _h3(xi + 1, 0, zi, seed) * u
    b = _h3(xi, 0, zi + 1, seed) * (1 - u) + _h3(xi + 1, 0, zi + 1, seed) * u
    return a * (1 - v) + b * v


def fbm2(x, z, seed=0, octaves=3):
    total = norm = 0.0
    amp = 1.0
    freq = 1.0
    for i in range(octaves):
        total += amp * vnoise2(x * freq, z * freq, seed + i * 17)
        norm += amp
        amp *= 0.5
        freq *= 2.07
    return total / norm


# ---------------------------------------------------------------------------
# block store / sections
# ---------------------------------------------------------------------------
_SECTIONS = []
_CUR = [None, None]
SKIPPED = defaultdict(int)
OVERWRITES = defaultdict(int)


def begin(name):
    d = {}
    _SECTIONS.append((name, d))
    _CUR[0], _CUR[1] = name, d


def put(x, y, z, block, force_air=False):
    x, y, z = int(round(x)), int(round(y)), int(round(z))
    if not (X_MIN <= x <= X_MAX and Z_MIN <= z <= Z_MAX and Y_MIN <= y <= Y_MAX):
        SKIPPED["out_of_box"] += 1
        return
    if not force_air and y > GROUND and math.hypot(x, z) < DOME_R:
        SKIPPED["dome_reserve"] += 1
        return
    d = _CUR[1]
    k = (x, y, z)
    if k in d:
        OVERWRITES[_CUR[0]] += 1
    d[k] = block


def has(x, y, z):
    return any((x, y, z) in d for _, d in _SECTIONS)


def block_at(x, y, z):
    for _, d in reversed(_SECTIONS):
        b = d.get((x, y, z))
        if b is not None:
            return b
    return None


def S(name):
    return "minecraft:" + name


# ---------------------------------------------------------------------------
# coastline
# ---------------------------------------------------------------------------
def coast_f(x, z):
    """Positive inside the main island, negative outside."""
    f = max(r - math.hypot(x - cx, z - cz) for cx, cz, r in LOBES)
    f += (fbm2(x * 0.042, z * 0.042, 701, 3) - 0.5) * 10.0
    for cx, cz, r in COVES:
        f = min(f, math.hypot(x - cx, z - cz) - r)
    return f


def is_land(x, z):
    r = math.hypot(x, z)
    if r < ISLE_R:
        return True
    if r < MOAT_R1:
        return False
    return coast_f(x, z) > 0.0


def dome_island_floor(x, z):
    return math.hypot(x, z) <= 36.0


# ===========================================================================
# 1. ISLAND SURFACE + COAST
# ===========================================================================
ROCK = ["minecraft:stone", "minecraft:stone", "minecraft:andesite",
        "minecraft:cobblestone", "minecraft:tuff", "minecraft:stone"]
ROCK_W = [0.34, 0.24, 0.18, 0.12, 0.07, 0.05]
MOSSY = ["minecraft:mossy_cobblestone", "minecraft:mossy_stone_bricks"]


def rock_at(x, y, z):
    n = vnoise2(x * 0.23 + y * 0.07, z * 0.23 - y * 0.05, 313)
    if n > 0.80:
        return MOSSY[int(_h3(x, y, z, 7) * 2) % 2]
    acc = 0.0
    t = (n + vnoise2(x * 0.07, z * 0.07 + y * 0.11, 91) * 0.6) / 1.6
    for b, w in zip(ROCK, ROCK_W):
        acc += w
        if t <= acc:
            return b
    return ROCK[0]


def build_island_surface():
    begin("01_island_surface")
    for x in range(X_MIN, X_MAX + 1):
        for z in range(Z_MIN, Z_MAX + 1):
            if math.hypot(x, z) < MOAT_R1:
                continue
            f = coast_f(x, z)
            if f <= 0.0:
                continue
            shore = f < 2.2
            top = GROUND - 1 if f < 0.55 else GROUND
            if shore:
                b = "minecraft:sand" if _h3(x, 1, z, 11) < 0.78 else "minecraft:gravel"
            elif f < 4.5:
                b = "minecraft:coarse_dirt" if _h3(x, 2, z, 12) < 0.25 else "minecraft:grass_block"
            else:
                b = "minecraft:grass_block"
            put(x, top, z, b)
            # 5-block cliff band under the lip; only the outermost cells show
            if f < 3.2:
                for y in range(GROUND - 5, top):
                    put(x, y, z, rock_at(x, y, z))
            # occasional boulders at the water / cliff lip
            if 2.0 < f < 3.4 and _h3(x, 5, z, 13) < 0.06:
                put(x, GROUND, z, "minecraft:mossy_cobblestone")
                if _h3(x, 6, z, 14) < 0.4:
                    put(x, GROUND + 1, z, "minecraft:mossy_cobblestone")


def build_keel():
    """Hanging rock keel below the island plate (visible from the side)."""
    begin("02_island_keel")
    for x in range(X_MIN, X_MAX + 1):
        for z in range(Z_MIN, Z_MAX + 1):
            r = math.hypot(x, z)
            if r > 64:
                continue
            if not is_land(x, z):
                # under the moat the basin closes the keel
                if not (r < MOAT_R1):
                    continue
            depth = 119.0 - max(0.0, 64.0 - r) * 0.62
            y = max(96, int(round(depth)))
            put(x, y, z, rock_at(x, y, z))
            if r < 46:
                put(x, y - 1, z, rock_at(x, y - 1, z))
            if r < 26:
                put(x, y - 2, z, rock_at(x, y - 2, z))
                put(x, y - 3, z, rock_at(x, y - 3, z))


# ===========================================================================
# 2. MOAT
# ===========================================================================
def build_moat():
    begin("03_moat")
    for x in range(-int(MOAT_R1) - 2, int(MOAT_R1) + 3):
        for z in range(-int(MOAT_R1) - 2, int(MOAT_R1) + 3):
            r = math.hypot(x, z)
            if not (ISLE_R - 1 <= r <= MOAT_R1 + 1):
                continue
            if r < ISLE_R:      # under the dome island rim
                continue
            if r <= MOAT_R1:
                # basin
                put(x, 115, z, "minecraft:sand" if _h3(x, 3, z, 21) < 0.6
                    else "minecraft:gravel")
                for y in (116, 117, WATER_Y):
                    put(x, y, z, "minecraft:water")
                if _h3(x, 4, z, 22) < 0.05:
                    put(x, 116, z, "minecraft:seagrass")
            # outer quay wall r 52..53
            if MOAT_R1 <= r <= MOAT_R1 + 1.2:
                for y in range(116, GROUND + 1):
                    b = ("minecraft:stone_bricks" if y % 5 else
                         "minecraft:chiseled_stone_bricks")
                    put(x, y, z, b)
            # inner quay wall under the dome island rim r 37..38
            if ISLE_R - 1.4 <= r <= ISLE_R:
                for y in range(116, GROUND):
                    put(x, y, z, "minecraft:stone_bricks" if y % 4 else
                        "minecraft:chiseled_stone_bricks")


# ===========================================================================
# 3. DOME ISLAND (floor, rim, balustrade, gateway)
# ===========================================================================
def build_dome_island():
    begin("04_dome_island")
    for x in range(-int(ISLE_R) - 1, int(ISLE_R) + 2):
        for z in range(-int(ISLE_R) - 1, int(ISLE_R) + 2):
            r = math.hypot(x, z)
            if r > ISLE_R:
                continue
            if r <= 36.4:
                put(x, GROUND, z, "minecraft:yellow_concrete")
                # smooth-stone inlay ring at the plinth foot
                if 31.0 <= r <= 32.0 and _h3(x, 9, z, 31) < 0.55:
                    put(x, GROUND, z, "minecraft:smooth_stone")
            else:
                put(x, GROUND, z, "minecraft:stone_bricks")
            # yellow floor inlay: a smooth-stone ring just inside the rim
            if 34.6 <= r <= 36.4:
                put(x, GROUND, z, "minecraft:stone_bricks")

    # red carpet axis on the island floor, from the plinth stairs to the rail
    for z in range(31, 38):
        for x in range(-4, 5):
            put(x, GROUND, z, "minecraft:red_concrete")
    for x in range(-9, 10):
        for z in range(29, 32):
            if math.hypot(x, z) > 36:
                continue
            put(x, GROUND, z, "minecraft:red_concrete")

    build_balustrade_ring()
    build_island_lamps()


def build_balustrade_ring():
    """Stone balustrade on the dome island rim, gap at the axis gateway."""
    seen = set()
    steps = 2000
    for i in range(steps):
        a = 2 * math.pi * i / steps
        x = int(round(math.cos(a) * 37.3))
        z = int(round(math.sin(a) * 37.3))
        if (x, z) in seen:
            continue
        seen.add((x, z))
        if abs(x) <= 4:
            continue                      # axis gateway
        put(x, GROUND + 1, z, "minecraft:stone_bricks")
        if (x + z) % 2 == 0:
            put(x, GROUND + 2, z, "minecraft:stone_brick_wall")
            put(x, GROUND + 3, z, "minecraft:stone_brick_wall")
        put(x, GROUND + 4, z, "minecraft:smooth_stone_slab[type=bottom]")

    # gateway piers with lanterns
    for sx in (-5, 5):
        for dz in (0, 1):
            z = 37 + dz
            for y in range(GROUND + 1, GROUND + 5):
                put(sx, y, z, "minecraft:chiseled_stone_bricks")
        put(sx, GROUND + 5, 37, "minecraft:smooth_stone_slab[type=bottom]")
        put(sx, GROUND + 6, 37, "minecraft:lantern[hanging=false]")


def build_island_lamps():
    for i in range(8):
        a = math.radians(22.5 + i * 45.0)
        x = int(round(math.cos(a) * 34.4))
        z = int(round(math.sin(a) * 34.4))
        if abs(x) <= 6 and z > 0:
            continue
        for y in range(GROUND + 1, GROUND + 4):
            put(x, y, z, "minecraft:stone_bricks")
        put(x, GROUND + 4, z, "minecraft:lantern[hanging=false]")


# ===========================================================================
# 4. RAINBOW PLINTH + CARPET STAIRS
# ===========================================================================
RAINBOW = ["red", "orange", "yellow", "lime", "green", "cyan",
           "light_blue", "blue", "purple", "magenta"]


def build_plinth():
    begin("05_rainbow_plinth")
    # ring body, y 120..126 rainbow, y 127 blackstone band, y 128 dark cap
    for x in range(-int(PLINTH_R1) - 1, int(PLINTH_R1) + 2):
        for z in range(-int(PLINTH_R1) - 1, int(PLINTH_R1) + 2):
            r = math.hypot(x, z)
            if not (PLINTH_R0 - 0.4 <= r <= PLINTH_R1 + 0.4):
                continue
            a = math.atan2(z, x)
            band = int((((a - math.pi / 2) % (2 * math.pi)) / (2 * math.pi)) * 20) % 20
            col = RAINBOW[(band // 2) % 10]
            for y in range(GROUND, PLINTH_TOP + 1):
                if y == PLINTH_TOP:
                    b = ("minecraft:%s_concrete" % col) if r >= 29.0 else "minecraft:polished_blackstone"
                elif y == PLINTH_TOP - 1:
                    b = "minecraft:blackstone" if (x + z) % 3 else "minecraft:polished_blackstone"
                elif y == GROUND:
                    b = "minecraft:blackstone"
                else:
                    b = "minecraft:%s_concrete" % col
                put(x, y, z, b)

    # dark rim posts (season 1 plinth silhouette)
    for i in range(24):
        a = math.radians(i * 15.0)
        px = int(round(math.cos(a) * 29.4))
        pz = int(round(math.sin(a) * 29.4))
        if abs(px) <= 4 and pz > 20:
            continue
        for y in (PLINTH_TOP + 1, PLINTH_TOP + 2, PLINTH_TOP + 3):
            put(px, y, pz, "minecraft:polished_blackstone")

    # red carpet on the plinth top, from the stairs into the opening
    for z in range(27, 31):
        for x in range(-4, 5):
            if math.hypot(x, z) < PLINTH_R0 - 0.2:
                continue
            put(x, PLINTH_TOP, z, "minecraft:red_carpet")

    build_plinth_stairs()


def build_plinth_stairs():
    """9-wide red carpet stair, z 31..37, surface y 121..127, landing 128.
    White quartz cheeks step with it; the stair body is solid down to the floor."""
    for i, z in enumerate(range(37, 30, -1)):
        y = GROUND + i                      # block under the carpet
        for x in range(-4, 5):
            put(x, y, z, "minecraft:red_terracotta")
            put(x, y + 1, z, "minecraft:red_carpet")
        for yy in range(GROUND, y):
            for x in range(-4, 5):
                put(x, yy, z, "minecraft:stone_bricks")
        # quartz cheeks
        for sx in (-5, 5):
            for yy in range(GROUND, y + 2):
                put(sx, yy, z, "minecraft:smooth_quartz")
            put(sx, y + 2, z, "minecraft:smooth_quartz_slab[type=bottom]")

    # bottom newel posts with lanterns
    for sx in (-7, 7):
        for y in range(GROUND, GROUND + 5):
            put(sx, y, 38, "minecraft:smooth_quartz")
        put(sx, GROUND + 5, 38, "minecraft:lantern[hanging=false]")
        put(sx, GROUND + 1, 39, "minecraft:smooth_quartz_slab[type=bottom]")
    # top posts
    for sx in (-6, 6):
        for y in range(GROUND + 2, GROUND + 8):
            put(sx, y, 31, "minecraft:smooth_quartz")
        put(sx, GROUND + 8, 31, "minecraft:lantern[hanging=false]")


# ===========================================================================
# 5. PLAZA, AXIS, EMBLEM
# ===========================================================================
def in_plaza(x, z):
    if not (PLAZA_X0 <= x <= PLAZA_X1 and PLAZA_Z0 <= z <= PLAZA_Z1):
        return False
    # rounded corners
    for cx, cz in ((PLAZA_X0, PLAZA_Z0), (PLAZA_X1, PLAZA_Z0),
                   (PLAZA_X0, PLAZA_Z1), (PLAZA_X1, PLAZA_Z1)):
        if abs(x - cx) < 7 and abs(z - cz) < 7:
            if math.hypot(x - cx, z - cz) > 9.5:
                return False
    return True


def build_plaza():
    begin("06_plaza_axis")
    for x in range(PLAZA_X0 - 1, PLAZA_X1 + 2):
        for z in range(PLAZA_Z0 - 1, PLAZA_Z1 + 2):
            if not in_plaza(x, z):
                continue
            b = "minecraft:bricks"
            # stone border, 2 wide
            if (x - PLAZA_X0 < 2 or PLAZA_X1 - x < 2 or
                    z - PLAZA_Z0 < 2 or PLAZA_Z1 - z < 2):
                b = "minecraft:stone_bricks"
            put(x, GROUND, z, b)

    # apron from the bridge to the plaza
    for x in range(-9, 10):
        for z in range(BRIDGE_Z1, PLAZA_Z0 + 1):
            put(x, GROUND, z, "minecraft:stone_bricks" if abs(x) > 7
                else "minecraft:deepslate_tiles")

    build_axis()
    build_emblem()
    build_plaza_inlays()


def build_axis():
    """Yellow band + red runner on the plaza axis (MainHub / S4 read)."""
    for z in range(BRIDGE_Z1, PLAZA_Z1 + 1):
        skip = (math.hypot(0, z - EMBLEM_Z) <= EMBLEM_R + 2.2)
        for x in range(-5, 6):
            if skip and math.hypot(x, z - EMBLEM_Z) <= EMBLEM_R + 2.2:
                continue
            put(x, GROUND, z, "minecraft:yellow_concrete")
        for x in range(-3, 4):
            if skip and math.hypot(x, z - EMBLEM_Z) <= EMBLEM_R + 2.2:
                continue
            put(x, GROUND, z, "minecraft:red_concrete")

    # south exit path, continues to the shore
    for z in range(PLAZA_Z1, 149):
        for x in range(-6, 7):
            if not is_land(x, z):
                continue
            put(x, GROUND, z, "minecraft:stone_bricks" if abs(x) > 4 else "minecraft:red_concrete")


EMBLEM = [
    ".......SSSSSSSS.......",
    ".....SSYYYYYYYYSS.....",
    "...SSYYYYYYYYYYYYSS...",
    "..SYYYYYYYYYYYYYYYYS..",
    ".SYYYYYRRRRRRRRYYYYYS.",
    ".SYYYYRRRRRRRRRRYYYYS.",
    "SYYYRRRRRRRRRRRRRRYYYS",
    "SYYRRRRRRRRRRRRRRRRYYS",
    "SYYRRRRRRRRRRRRRRRRYYS",
    "SYRRRRRRRRRRRRRRRRRRYS",
    "SYRRRRRRRRRRRRRRRRRRYS",
    "SYRRRRRRRRRRRRRRRRRRYS",
    "SYRRRRRRRRRRRRRRRRRRYS",
    "SYRRRRRRRRRRRRRRRRRRYS",
    "SYYRRRRRRRRRRRRRRRRYYS",
    "SYYRRRRRRRRRRRRRRRRYYS",
    "SYYYRRRRRRRRRRRRRRYYYS",
    ".SYYYYRRRRRRRRRRYYYYS.",
    ".SYYYYYRRRRRRRRYYYYYS.",
    "..SYYYYYYYYYYYYYYYYS..",
    "...SSYYYYYYYYYYYYSS...",
    ".....SSYYYYYYYYSS.....",
    ".......SSSSSSSS.......",
]

# crown + M motif in yellow, drawn onto the red field (23x23 local grid)
MOTIF_W, MOTIF_H = 23, 23
MOTIF = []
for _r in range(MOTIF_H):
    MOTIF.append(["."] * MOTIF_W)


def _m(px, pz):
    if 0 <= px < MOTIF_W and 0 <= pz < MOTIF_H:
        MOTIF[pz][px] = "Y"


# crown: three points over a heavy band
for _x in (7, 11, 15):
    _m(_x, 4)
for _x in range(6, 17):
    for _z in (5, 6):
        _m(_x, _z)
for _x in range(8, 15):
    _m(_x, 7)
# M, 11 wide, 2-block strokes
_M_ROWS = [
    "YY.......YY",
    "YYY.....YYY",
    "YYYY...YYYY",
    "YY.YY.YY.YY",
    "YY..YYY..YY",
    "YY.......YY",
    "YY.......YY",
]
for _i, _row in enumerate(_M_ROWS):
    for _j, _ch in enumerate(_row):
        if _ch == "Y":
            _m(6 + _j, 9 + _i)


def build_emblem():
    for x in range(-13, 14):
        for z in range(EMBLEM_Z - 13, EMBLEM_Z + 14):
            d = math.hypot(x, z - EMBLEM_Z)
            if d > EMBLEM_R + 0.6:
                continue
            if d > EMBLEM_R - 0.9:
                b = "minecraft:smooth_stone"
            elif d > EMBLEM_R - 2.0:
                b = "minecraft:yellow_concrete"
            else:
                b = "minecraft:red_concrete"
            put(x, GROUND, z, b)

    # motif
    for px in range(MOTIF_W):
        for pz in range(MOTIF_H):
            if MOTIF[pz][px] != "Y":
                continue
            x = px - MOTIF_W // 2
            z = EMBLEM_Z + pz - MOTIF_H // 2
            if math.hypot(x, z - EMBLEM_Z) <= EMBLEM_R - 2.0:
                put(x, GROUND, z, "minecraft:yellow_concrete")


def build_plaza_inlays():
    # stone ring around the emblem
    for x in range(-18, 19):
        for z in range(EMBLEM_Z - 18, EMBLEM_Z + 19):
            d = math.hypot(x, z - EMBLEM_Z)
            if EMBLEM_R + 2.0 < d <= EMBLEM_R + 3.2:
                if in_plaza(x, z):
                    put(x, GROUND, z, "minecraft:stone_bricks")
    # transverse stone bands at the plaza quarters
    for z in (70, 102):
        for x in range(PLAZA_X0 + 2, PLAZA_X1 - 1):
            d = abs(z - EMBLEM_Z)
            if math.hypot(x, d) < EMBLEM_R + 4:
                continue
            if in_plaza(x, z) or in_plaza(x, z + 1):
                put(x, GROUND, z, "minecraft:smooth_stone")


# ===========================================================================
# 6. TERRACES / GARDENS
# ===========================================================================
BEDS = [
    (-38, -34, 66, 80, 1, 2),   # W, low planter
    (34, 38, 66, 80, 1, 2),     # E, low planter
    (-38, -22, 98, 112, 2, 3),  # SW, tall terrace
    (22, 38, 98, 112, 2, 3),    # SE, tall terrace
]


def build_gardens():
    begin("07_gardens")
    for (x0, x1, z0, z1, wall_h, hedge_h) in BEDS:
        top = GROUND + wall_h
        # retaining wall
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge = (x in (x0, x1) or z in (z0, z1))
                if edge:
                    for y in range(GROUND + 1, top + 1):
                        put(x, y, z, "minecraft:smooth_sandstone")
                else:
                    put(x, GROUND + 1, z, "minecraft:dirt")
                    put(x, top, z, "minecraft:grass_block")
        # coping on the wall top
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                put(x, top + 1, z, "minecraft:smooth_sandstone_slab[type=bottom]")
        for z in range(z0 + 1, z1):
            for x in (x0, x1):
                put(x, top + 1, z, "minecraft:smooth_sandstone_slab[type=bottom]")
        # hedge mass with a flower pit
        for x in range(x0 + 2, x1 - 1):
            for z in range(z0 + 2, z1 - 1):
                if not (x0 + 5 <= x <= x1 - 5 and z0 + 5 <= z <= z1 - 5):
                    for y in range(top + 1, top + 1 + hedge_h):
                        if _h3(x, y, z, 40) < 0.9:
                            put(x, y, z, "minecraft:oak_leaves[distance=2,persistent=true]")
                else:
                    put(x, top, z, "minecraft:grass_block")
                    if _h3(x, 8, z, 41) < 0.55:
                        b = ["minecraft:poppy", "minecraft:oxeye_daisy",
                             "minecraft:cornflower", "minecraft:azure_bluet",
                             "minecraft:white_tulip"][int(_h3(x, 9, z, 42) * 5)]
                        put(x, top + 1, z, b)
        # snow on the wall top
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                if _h3(x, 10, z, 43) < 0.7:
                    put(x, top + 1, z, "minecraft:snow[layers=2]")
        for z in range(z0 + 1, z1):
            for x in (x0, x1):
                if _h3(x, 10, z, 43) < 0.7:
                    put(x, top + 1, z, "minecraft:snow[layers=2]")

    # low hedge parapet flanking the plaza edge (beam to the beds)
    for x0, x1 in ((-40, -38), (38, 40)):
        for z in range(96, 113):
            if not is_land(x0, z):
                continue
            put(x0, GROUND + 1, z, "minecraft:smooth_sandstone")
        for z in range(100, 112):
            put(x0, GROUND + 2, z, "minecraft:smooth_sandstone")
            put(x0, GROUND + 3, z, "minecraft:smooth_sandstone_slab[type=bottom]")


# ===========================================================================
# 7. BRIDGE
# ===========================================================================
def build_bridge():
    begin("08_bridge")
    for x in range(-BRIDGE_HALF, BRIDGE_HALF + 1):
        for z in range(BRIDGE_Z0, BRIDGE_Z1 + 1):
            if abs(x) == BRIDGE_HALF:
                b = "minecraft:cracked_stone_bricks"
            elif abs(x) == BRIDGE_HALF - 1:
                b = "minecraft:smooth_stone"
            elif (x + z) % 17 == 0:
                b = "minecraft:cracked_stone_bricks"
            else:
                b = "minecraft:stone_bricks"
            put(x, GROUND, z, b)
    # support arches under the deck (moat crossing)
    for z in range(BRIDGE_Z0 + 1, BRIDGE_Z1):
        for x in range(-BRIDGE_HALF, BRIDGE_HALF + 1):
            put(x, GROUND - 1, z, "minecraft:stone_bricks")
    for z in (BRIDGE_Z0 + 1, BRIDGE_Z1 - 1):
        for x in range(-BRIDGE_HALF, BRIDGE_HALF + 1):
            for y in range(115, GROUND - 1):
                put(x, y, z, "minecraft:stone_bricks")

    build_bridge_rails()


def build_bridge_rails():
    for sx in (-7, 7):
        for z in range(BRIDGE_Z0, BRIDGE_Z1 + 1):
            put(sx, GROUND, z, "minecraft:stone_bricks")
            if z % 2 == 0:
                put(sx, GROUND + 1, z, "minecraft:stone_brick_wall")
                put(sx, GROUND + 2, z, "minecraft:stone_brick_wall")
            put(sx, GROUND + 3, z, "minecraft:smooth_stone_slab[type=bottom]")
        # newels
        for z in (BRIDGE_Z0, BRIDGE_Z1):
            for y in range(GROUND + 1, GROUND + 4):
                put(sx, y, z, "minecraft:chiseled_stone_bricks")
            put(sx, GROUND + 4, z, "minecraft:lantern[hanging=false]")


# ===========================================================================
# 8. GIANT MCC COLUMNS
# ===========================================================================
M_ROWS = [
    "X.....X",
    "XX...XX",
    "X.X.X.X",
    "X..X..X",
    "X.....X",
    "X.....X",
    "X.....X",
]
C_ROWS = [
    ".XXXXX.",
    "X.....X",
    "X......",
    "X......",
    "X......",
    "X.....X",
    ".XXXXX.",
]


def letter(cx, y0, rows, z):
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch == "X":
                put(cx - 3 + i, y0 - j, z, "minecraft:white_concrete")


def build_columns():
    begin("09_giant_columns")
    for side in (-1, 1):
        cx = side * COL_X
        build_one_column(cx, COL_Z)


def build_one_column(cx, cz):
    # 15 x 5 stone plinth
    for x in range(cx - 7, cx + 8):
        for z in range(cz - 2, cz + 3):
            put(x, GROUND, z, "minecraft:stone_bricks")
            put(x, GROUND + 1, z, "minecraft:stone_bricks")
    for x in (cx - 7, cx + 7):
        for z in (cz - 2, cz + 2):
            put(x, GROUND + 1, z, "minecraft:chiseled_stone_bricks")

    # white quartz core
    for x in range(cx - 4, cx + 5):
        for z in range(cz - 2, cz + 1):
            for y in range(GROUND + 2, GROUND + 34):
                put(x, y, z, "minecraft:smooth_quartz")

    # red banner: 13 wide, 2 deep, tapered foot
    for y in range(GROUND + 1, GROUND + 34):
        if y <= GROUND + 4:
            half = 3 + (y - GROUND - 1)
        else:
            half = 6
        for x in range(cx - half, cx + half + 1):
            for z in (cz + 1, cz + 2):
                put(x, y, z, "minecraft:red_concrete")

    # letters, 7x7, 1 block proud of the banner face
    zface = cz + 3
    letter(cx, GROUND + 28, M_ROWS, zface)
    letter(cx, GROUND + 19, C_ROWS, zface)
    letter(cx, GROUND + 10, C_ROWS, zface)

    # white side pillars framing the banner (visible in S6 either side of the red)
    for sx in (-7, 7):
        for z in (cz + 1, cz + 2):
            for y in range(GROUND + 1, GROUND + 34):
                put(cx + sx, y, z, "minecraft:smooth_quartz")
        for y in (GROUND + 1, GROUND + 2):
            put(cx + sx, y, cz + 3, "minecraft:chiseled_quartz_block")
        for y in (GROUND + 32, GROUND + 33):
            put(cx + sx, y, cz + 3, "minecraft:chiseled_quartz_block")

    # capitals
    for x in range(cx - 8, cx + 9):
        for z in range(cz - 3, cz + 4):
            put(x, GROUND + 34, z, "minecraft:quartz_block")
            put(x, GROUND + 35, z, "minecraft:quartz_block")
    for x in range(cx - 7, cx + 8):
        for z in range(cz - 2, cz + 3):
            put(x, GROUND + 36, z, "minecraft:smooth_quartz")
    for x in range(cx - 8, cx + 9):
        for z in (cz - 3, cz + 3):
            put(x, GROUND + 36, z, "minecraft:quartz_slab[type=bottom]")

    # gold crown: solid sill, full band, 2-on 1-off crenellation
    for x in range(cx - 6, cx + 7):
        for z in range(cz - 2, cz + 3):
            put(x, GROUND + 37, z, "minecraft:gold_block")
            ring = (x in (cx - 6, cx + 6) or z in (cz - 2, cz + 2))
            if ring:
                put(x, GROUND + 38, z, "minecraft:gold_block")
                put(x, GROUND + 39, z, "minecraft:gold_block")
                if (x - (cx - 6)) % 3 != 2:
                    put(x, GROUND + 40, z, "minecraft:gold_block")

    # gold side lamps on stone arms
    for sx in (-1, 1):
        x0 = cx + sx * 7
        for x in range(min(cx + sx * 6, x0 + sx * 2), max(cx + sx * 6, x0 + sx * 2) + 1):
            put(x, GROUND + 30, cz, "minecraft:stone_bricks")
            put(x, GROUND + 31, cz, "minecraft:stone_bricks")
        put(x0 + sx, GROUND + 30, cz, "minecraft:gold_block")
        put(x0 + sx, GROUND + 31, cz, "minecraft:gold_block")
        put(x0 + sx, GROUND + 30, cz + 1, "minecraft:gold_block")
        put(x0 + sx, GROUND + 31, cz + 1, "minecraft:gold_block")

    # festive red bow on the banner face
    bow = [
        "..RRR.RRR..",
        ".RRRRRRRRR.",
        ".RRR.G.RRR.",
        "..R.RRR.R..",
        "...R.R.R...",
    ]
    for j, row in enumerate(bow):
        for i, ch in enumerate(row):
            if ch in "RG":
                b = "minecraft:gold_block" if ch == "G" else "minecraft:red_concrete"
                put(cx - 5 + i, GROUND + 33 - j, zface, b)


# ===========================================================================
# 9. TEAM-COLOURED COLUMN ROW AT THE COLUMN FEET
# ===========================================================================
TEAM = ["red", "orange", "yellow", "lime", "green",
        "cyan", "light_blue", "blue", "purple", "pink"]


def build_team_row():
    begin("10_team_row")
    xs = [-46, -39, -32, -25, -18, 18, 25, 32, 39, 46]
    for xc, col in zip(xs, TEAM):
        for x in range(xc - 1, xc + 2):
            for z in range(60, 63):
                put(x, GROUND, z, "minecraft:smooth_stone")
                for y in range(GROUND + 1, GROUND + 10):
                    put(x, y, z, "minecraft:%s_concrete" % col)
                put(x, GROUND + 10, z, "minecraft:smooth_stone")
                put(x, GROUND + 11, z, "minecraft:snow[layers=1]")


# ===========================================================================
# 10. PATHS OFF THE PLAZA
# ===========================================================================
def path_line(x0, z0, x1, z1, width=5):
    steps = max(abs(x1 - x0), abs(z1 - z0)) * 2 + 1
    for i in range(steps + 1):
        t = i / steps
        x = x0 + (x1 - x0) * t
        z = z0 + (z1 - z0) * t
        r = width / 2.0
        for dx in range(-int(r), int(r) + 1):
            for dz in range(-int(r), int(r) + 1):
                xx, zz = int(round(x)) + dx, int(round(z)) + dz
                if math.hypot(xx - x, zz - z) > r + 0.35:
                    continue
                if not is_land(xx, zz):
                    continue
                if math.hypot(xx, zz) < MOAT_R1 + 1:
                    continue
                if in_plaza(xx, zz):
                    continue
                put(xx, GROUND, zz, "minecraft:stone_bricks" if (xx + zz) % 3 else
                    "minecraft:cracked_stone_bricks")
                if _h3(xx, 12, zz, 61) < 0.10:
                    put(xx, GROUND, zz, "minecraft:gravel")


def build_paths():
    begin("11_paths")
    # west: plaza -> Hall of Fame door
    path_line(-38, 90, -49, 90, 5)
    path_line(-49, 90, -49, 46, 5)
    # east: plaza -> around the snow globe -> aqua
    path_line(38, 90, 55, 90, 5)
    path_line(55, 90, 55, 40, 5)
    path_line(55, 40, 96, 40, 5)
    path_line(96, 40, 96, 18, 5)
    # north-east spur along the moat
    path_line(38, 46, 52, 46, 5)


# ===========================================================================
# 11. SNOW + LAMPS + SHORE DRESSING
# ===========================================================================
SNOW_BASE = {
    "minecraft:grass_block": (0.56, 0.86),
    "minecraft:coarse_dirt": (0.60, None),
    "minecraft:oak_leaves[distance=2,persistent=true]": (0.70, None),
    "minecraft:smooth_sandstone": (0.26, None),
    "minecraft:smooth_sandstone_slab[type=bottom]": (0.44, None),
    "minecraft:smooth_stone_slab[type=bottom]": (0.62, None),
    "minecraft:polished_blackstone": (0.56, None),
    "minecraft:quartz_block": (0.30, None),
    "minecraft:smooth_quartz": (0.44, None),
    "minecraft:stone_bricks": (0.66, None),
}


def build_snow():
    begin("12_snow")
    snap = all_blocks()
    todo = []
    for (x, y, z), b in snap.items():
        spec = SNOW_BASE.get(b)
        if spec is None:
            continue
        if (x, y + 1, z) in snap:
            continue
        thr, block_thr = spec
        if b in ("minecraft:grass_block", "minecraft:coarse_dirt"):
            # big drifts with ragged edges
            n = (fbm2(x * 0.045, z * 0.045, 811, 3)
                 + (fbm2(x * 0.19, z * 0.19, 812, 2) - 0.5) * 0.16)
            bias = 0.05 if z < 10 else (-0.02 if z > 100 else 0.0)
            if block_thr is not None and n + bias > block_thr:
                todo.append((x, y, z, "minecraft:snow_block"))
            elif n + bias > thr:
                todo.append((x, y, z, "minecraft:snow[layers=1]"))
        else:
            n = vnoise2(x * 0.9, z * 0.9, 813)
            if n > thr:
                todo.append((x, y, z, "minecraft:snow[layers=1]"))
    for x, y, z, b in todo:
        put(x, y + 1, z, b)

    # shore ice patches on the moat
    for x in range(-int(MOAT_R1), int(MOAT_R1) + 1):
        for z in range(-int(MOAT_R1), int(MOAT_R1) + 1):
            r = math.hypot(x, z)
            if ISLE_R < r < MOAT_R1 and _h3(x, 20, z, 71) < 0.022:
                put(x, WATER_Y + 1, z, "minecraft:ice")

    # shore ice patches on the moat
    for x in range(-int(MOAT_R1), int(MOAT_R1) + 1):
        for z in range(-int(MOAT_R1), int(MOAT_R1) + 1):
            r = math.hypot(x, z)
            if ISLE_R < r < MOAT_R1 and _h3(x, 20, z, 71) < 0.022:
                put(x, WATER_Y + 1, z, "minecraft:ice")


def build_shore_dressing():
    begin("13_shore_dressing")
    # little snow drifts and grass tufts on the coast
    for x in range(X_MIN, X_MAX + 1, 1):
        for z in range(Z_MIN, Z_MAX + 1, 1):
            f = coast_f(x, z)
            if not (0.8 < f < 3.0):
                continue
            top = block_at(x, GROUND, z)
            if top is None or has(x, GROUND + 1, z):
                continue
            n = _h3(x, 31, z, 81)
            if n < 0.20:
                put(x, GROUND + 1, z, "minecraft:snow[layers=2]")
            elif n < 0.33:
                put(x, GROUND + 1, z, ["minecraft:short_grass", "minecraft:fern",
                                       "minecraft:azure_bluet", "minecraft:oxeye_daisy"][
                    int(_h3(x, 32, z, 82) * 4)])


# ===========================================================================
# export
# ===========================================================================
def all_blocks():
    out = OrderedDict()
    for _, d in _SECTIONS:
        out.update(d)
    return out


def chunk_commands(blocks, max_cmds=16000):
    """Group (y,z,block) -> x runs, emit setblock/fill. Returns chunks of lines."""
    groups = defaultdict(list)
    for (x, y, z), b in blocks.items():
        groups[(y, z, b)].append(x)
    lines = []
    for (y, z, b), xs in sorted(groups.items()):
        xs.sort()
        start = prev = xs[0]
        for x in xs[1:]:
            if x == prev + 1:
                prev = x
                continue
            lines.append(_cmd(start, prev, y, z, b))
            start = prev = x
        lines.append(_cmd(start, prev, y, z, b))
    return [lines[i:i + max_cmds] for i in range(0, len(lines), max_cmds)]


def _cmd(x0, x1, y, z, b):
    if x0 == x1:
        return "setblock %d %d %d %s" % (x0, y, z, b)
    return "fill %d %d %d %d %d %d %s" % (x0, y, z, x1, y, z, b)


def export():
    os.makedirs(FUNC_DIR, exist_ok=True)
    blocks = all_blocks()

    # snowy grass pass
    for (x, y, z), b in list(blocks.items()):
        if b == "minecraft:grass_block" and blocks.get((x, y + 1, z)) in (
                "minecraft:snow[layers=1]", "minecraft:snow_block"):
            blocks[(x, y, z)] = "minecraft:grass_block[snowy=true]"
        if b in ("minecraft:podzol", "minecraft:mycelium") and blocks.get((x, y + 1, z)):
            pass

    # CSV
    with open(os.path.join(HERE, "blocks.csv"), "w", encoding="utf-8", newline="\n") as f:
        f.write("x,y,z,block,section\n")
        owner = {}
        for name, d in _SECTIONS:
            for k in d:
                owner[k] = name
        for (x, y, z), b in sorted(blocks.items(), key=lambda kv: (kv[0][1] >> 4, kv[0][1], kv[0][0], kv[0][2])):
            f.write("%d,%d,%d,%s,%s\n" % (x, y, z, b, owner.get((x, y, z), "?")))

    # mcfunction chunks
    for old in os.listdir(FUNC_DIR):
        if old.startswith("v4a_") and old.endswith(".mcfunction"):
            os.remove(os.path.join(FUNC_DIR, old))
    calls = []
    total_cmds = 0
    for name, d in _SECTIONS:
        if not d:
            continue
        chunks = chunk_commands(d)
        for i, chunk in enumerate(chunks, 1):
            fn = "v4a_%s_%02d.mcfunction" % (name, i)
            with open(os.path.join(FUNC_DIR, fn), "w", encoding="utf-8", newline="\n") as f:
                f.write("\n".join(chunk) + "\n")
            calls.append(fn[:-len(".mcfunction")])
            total_cmds += len(chunk)
    with open(os.path.join(HERE, "load.mcfunction"), "w", encoding="utf-8", newline="\n") as f:
        f.write("# V4-A base package -- run once. Origin 0,120,0. No dome.\n")
        for c in calls:
            f.write("function v4a:%s\n" % c)

    # mcmdump for the renderer
    dump = os.path.join(HERE, "preview.mcmdump.gz")
    with gzip.open(dump, "wt", encoding="utf-8", newline="\n") as f:
        f.write("MCMDUMP 1 %d %d %d\n" % (X_MAX - X_MIN + 1, Y_MAX - Y_MIN + 1, Z_MAX - Z_MIN + 1))
        for (x, y, z), b in sorted(blocks.items(), key=lambda kv: (kv[0][1], kv[0][0], kv[0][2])):
            f.write("%d %d %d %s\n" % (x - X_MIN, y - Y_MIN, z - Z_MIN, b))

    # stats
    counts = {}
    for b in blocks.values():
        counts[b] = counts.get(b, 0) + 1
    sec_counts = {name: len(d) for name, d in _SECTIONS}
    stats = {
        "blocks": len(blocks),
        "commands": total_cmds,
        "files": calls,
        "by_section": sec_counts,
        "by_block": dict(sorted(counts.items(), key=lambda kv: -kv[1])),
        "skipped": dict(SKIPPED),
        "overwrites": dict(OVERWRITES),
    }
    with open(os.path.join(HERE, "stats.json"), "w", encoding="utf-8") as f:
        json.dump(stats, f, indent=2)
    print("blocks: %d  commands: %d  files: %d" % (len(blocks), total_cmds, len(calls)))
    for name, n in sec_counts.items():
        print("  %-22s %6d" % (name, n))


# ===========================================================================
def main():
    build_island_surface()
    build_keel()
    build_moat()
    build_dome_island()
    build_plinth()
    build_plaza()
    build_gardens()
    build_bridge()
    build_columns()
    build_team_row()
    build_paths()
    build_shore_dressing()
    build_snow()
    export()


if __name__ == "__main__":
    main()
