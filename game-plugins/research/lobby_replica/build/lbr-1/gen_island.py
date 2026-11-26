#!/usr/bin/env python3
"""
LBR-1 - base island for the festive MCC hub replica.

Build site:  C = 5000, 120, 5000   (world mcc_lobby_official)
Top plane:   y = 120                (the walkable block layer)
Underside:   down to y = 90
Footprint:   irregular floating island, mean radius ~97 (86..108)

Layout (plan view, C at the centre):
    r  0..25   drum footprint (under the Decision Dome, LBR-2)
    r  0..48   paved plaza, eight tan ribbons, medallion ring
    r 48..60   lagoon ring (water), eight 7-wide causeways cross it
    r 60..rim  land ring: 20x20 flattened portal pads on r=72,
               snow / grass / dirt mosaic, three sunken ponds, snow rampart
    r ..rim    1-block snow lip overhanging the underside
    y 90..120  hollow shell: 3-layer crust + 2-layer belly, banded strata

Owned by LBR-1: island mass + underside, plaza, paths, pads, water, snow.
Not owned here: dome, THE CHAMPS, globe, portal facades, props, lamps.

Outputs (all in this folder):
    blocks.csv                    x,y,z,block,section  (one row per placed block)
    island.mcmdump.gz             MCMDUMP v1, region relative, for tools/render_preview.py
    datapack/                     pack.mcmeta + mcchub:lbr1/*.mcfunction (split ~8k cmds)
    preview_top.png / preview_iso.png
"""

from __future__ import annotations

import csv
import gzip
import math
import os

import numpy as np

# ----------------------------------------------------------------------------
# Site constants
# ----------------------------------------------------------------------------
CX, CZ = 5000, 5000           # build centre
TOP = 120                     # walkable top layer
UNDER = 90                    # deepest underside block
PLAZA_R = 48.0                # plaza paving radius (brief: ~48)
LAGOON_OUT = 60.5             # lagoon ring outer edge / land ring start
PORTAL_R = 72.0               # portal ring radius
PAD_HALF = 9                  # 18x18 pads -> half size 9
PATH_HALF = 3.0               # 7 wide
RIDGE_RHO = 0.925             # snow rampart starts here
RIM_RHO = 0.955               # vertical 7-block rim cliff starts here

# Region box (mean radius 97 + lobes + lip -> ~14 blocks of margin)
X0, X1 = 4886, 5114
Z0, Z1 = 4886, 5114
Y0, Y1 = 88, 127

NX, NY, NZ = X1 - X0 + 1, Y1 - Y0 + 1, Z1 - Z0 + 1

# Section ids (used for the .mcfunction split and the CSV column)
S_MASS, S_SURFACE, S_WATER, S_PAVING, S_DETAIL = 1, 2, 3, 4, 5
SECTION_NAME = {
    S_MASS: "mass",
    S_SURFACE: "surface",
    S_WATER: "water",
    S_PAVING: "paving",
    S_DETAIL: "detail",
}

# Eight portal bearings, degrees (0 = east = +x, 90 = south = +z)
PORTALS = [
    (0.0, "Ace Race", "east"),
    (45.0, "Battle Box", "south-east"),
    (90.0, "TGTTOS", "south"),
    (135.0, "Sky Battle", "south-west"),
    (180.0, "Build Mart", "west"),
    (225.0, "Sands of Time", "north-west"),
    (270.0, "Hole in the Wall", "north"),
    (315.0, "Parkour Tag", "north-east"),
]
# Integer pad centres (portal ring snapped to whole blocks)
PAD_C = [
    (5072, 5000), (5051, 5051), (5000, 5072), (4949, 5051),
    (4928, 5000), (4949, 4949), (5000, 4928), (5051, 4949),
]

# Sunken ponds in the land ring: (bearing deg, centre radius, radial half, tangential half)
PONDS = (
    (22.5, 84.0, 12.0, 8.0),
    (112.5, 85.0, 11.5, 7.5),
    (247.5, 83.0, 10.0, 7.0),
)

# ----------------------------------------------------------------------------
# Palettes (vanilla Java 1.21.1 only)
# ----------------------------------------------------------------------------
AIR = "minecraft:air"
WATER = "minecraft:water[level=0]"
SNOW = "minecraft:snow_block"
ICE = "minecraft:ice"
PACKED_ICE = "minecraft:packed_ice"
BLUE_ICE = "minecraft:blue_ice"

# ----------------------------------------------------------------------------
# Deterministic noise
# ----------------------------------------------------------------------------
_M = 0xFFFFFFFF


def hsh(x: int, z: int, seed: int) -> float:
    n = (int(x) * 374761393 + int(z) * 668265263 + int(seed) * 1013904223) & _M
    n = (n ^ (n >> 13)) & _M
    n = (n * 1274126177) & _M
    n = (n ^ (n >> 16)) & _M
    return n / _M


def hsh3(x: int, y: int, z: int, seed: int) -> float:
    return hsh(x * 31 + y * 173 + seed, z * 17 + y * 71, seed * 13 + 7)


def vnoise(x: float, z: float, cell: float, seed: int) -> float:
    fx, fz = x / cell, z / cell
    ix, iz = math.floor(fx), math.floor(fz)
    tx, tz = fx - ix, fz - iz
    sx = tx * tx * (3 - 2 * tx)
    sz = tz * tz * (3 - 2 * tz)
    a = hsh(ix, iz, seed)
    b = hsh(ix + 1, iz, seed)
    c = hsh(ix, iz + 1, seed)
    d = hsh(ix + 1, iz + 1, seed)
    return (a * (1 - sx) + b * sx) * (1 - sz) + (c * (1 - sx) + d * sx) * sz


def fbm(x: float, z: float, cell: float, octaves: int, seed: int) -> float:
    total, amp, norm = 0.0, 1.0, 0.0
    for i in range(octaves):
        total += amp * vnoise(x, z, cell / (2 ** i), seed + i * 97)
        norm += amp
        amp *= 0.5
    return total / norm


def ang_delta(a: float, b: float) -> float:
    return abs((a - b + math.pi) % (2 * math.pi) - math.pi)


# ----------------------------------------------------------------------------
# Island geometry
# ----------------------------------------------------------------------------
def radius_at(th: float) -> float:
    """Irregular silhouette: bold lobes, three bitten notches, portal aprons."""
    r = (98.0
         + 4.2 * math.sin(3.0 * th + 0.70)
         + 2.8 * math.sin(5.0 * th + 2.35)
         + 1.9 * math.sin(8.0 * th + 4.10)
         + 0.6 * math.sin(13.0 * th + 1.20))
    for a, w, d in ((0.30, 0.075, 5.0), (2.15, 0.062, 3.4), (4.55, 0.080, 4.2)):
        r -= d * math.exp(-(ang_delta(th, a) / w) ** 2)
    # one soft outward bulge on the east-south-east shoulder
    r += 3.0 * math.exp(-(ang_delta(th, 0.95) / 0.30) ** 2)
    # gentle aprons at the eight portal bearings so the pads never crowd the rim
    for k in range(8):
        r += 2.5 * math.exp(-(ang_delta(th, k * math.pi / 4) / 0.30) ** 2)
    return max(86.0, min(110.0, r))


# Hanging rock spurs under the belly: (angle, radius, extra depth, angular width)
SPURS = ((0.55, 46.0, 6.5, 0.11), (2.45, 58.0, 5.0, 0.09),
         (4.15, 40.0, 7.5, 0.10), (5.45, 63.0, 4.5, 0.08))


# Vertical profile: (y, radius scale). A 20-block slab edge, then the belly.
PROFILE = ((120.0, 1.000), (102.0, 1.000), (99.0, 0.965), (96.0, 0.915),
           (93.0, 0.840), (91.5, 0.740), (90.0, 0.520))


def wall_offset(th: float) -> float:
    """How much this bearing's slab wall hangs below the nominal y=102."""
    o = 3.0 + 3.2 * math.sin(2.0 * th + 0.90) + 2.0 * math.sin(4.0 * th + 2.20)
    for a, d in ((1.35, 4.5), (3.60, 3.0), (5.05, 5.0)):
        o += d * math.exp(-(ang_delta(th, a) / 0.055) ** 2)
    return max(0.0, min(10.0, o))


def bottom_base(r: float, th: float) -> float:
    """y of the deepest block of the column (slab wall, then the belly bell)."""
    R = radius_at(th)
    q = r / R
    if q >= 1.0:
        base = 102.0
    elif q <= PROFILE[-1][1]:
        base = 90.0
    else:
        base = 90.0
        for (y0, s0), (y1, s1) in zip(PROFILE, PROFILE[1:]):
            if s0 <= s1:
                continue
            if s1 <= q <= s0:
                f = (s0 - q) / (s0 - s1)
                base = y0 + (y1 - y0) * f
                break
    base -= wall_offset(th)
    for a, rad, depth, wid in SPURS:
        if ang_delta(th, a) < wid * 2.2 and abs(r - rad) < 7.0:
            angf = max(0.0, 1.0 - ang_delta(th, a) / (wid * 2.2))
            radf = max(0.0, 1.0 - abs(r - rad) / 7.0)
            base -= depth * angf * radf
    return max(90.0, base)


def islet(x: int, z: int) -> bool:
    for a, rad, rr, h in ((1.18, 90.0, 4.2, 1), (3.95, 89.5, 3.4, 0), (5.72, 91.0, 2.8, 1)):
        px = CX + rad * math.cos(a)
        pz = CZ + rad * math.sin(a)
        if (x - px) ** 2 + (z - pz) ** 2 <= rr * rr:
            return True
    return False


def pond_at(x: int, z: int) -> bool:
    for deg, rad, ax, az in PONDS:
        a = math.radians(deg)
        px, pz = CX + rad * math.cos(a), CZ + rad * math.sin(a)
        dr = (x - px) * math.cos(a) + (z - pz) * math.sin(a)
        dt = -(x - px) * math.sin(a) + (z - pz) * math.cos(a)
        q = math.hypot(dr / ax, dt / az)
        q += 0.09 * (fbm(x, z, 9.0, 2, 511) - 0.5)
        if q <= 1.0:
            return True
    return False


# ----------------------------------------------------------------------------
# Field helpers: paths, pads
# ----------------------------------------------------------------------------
_RADIALS = [(math.cos(k * math.pi / 4), math.sin(k * math.pi / 4)) for k in range(8)]


def path_field(x: int, z: int):
    """Nearest radial path: (perpendicular distance, distance along the path)."""
    px, pz = x - CX, z - CZ
    best_pd, best_t = 1e9, 0.0
    for dx, dz in _RADIALS:
        t = px * dx + pz * dz
        if t < 36.0:
            continue
        s = abs(-px * dz + pz * dx)
        if s < best_pd:
            best_pd, best_t = s, t
    return best_pd, best_t


def pad_at(x: int, z: int, half: int = PAD_HALF):
    for i, (px, pz) in enumerate(PAD_C):
        ddx, ddz = x - px, z - pz
        if abs(ddx) <= half and abs(ddz) <= half:
            return i, abs(ddx), abs(ddz)
    return None


# ----------------------------------------------------------------------------
# Voxel store
# ----------------------------------------------------------------------------
PAL: dict[str, int] = {"minecraft:air": 0}
NAMES: list[str] = ["minecraft:air"]


def pid(name: str) -> int:
    i = PAL.get(name)
    if i is None:
        i = len(NAMES)
        PAL[name] = i
        NAMES.append(name)
    return i


GRID = np.zeros((NX, NY, NZ), np.int16)
SEC = np.zeros((NX, NY, NZ), np.int8)


def put(x: int, y: int, z: int, block: str, section: int, force: bool = True) -> None:
    if not (X0 <= x <= X1 and Y0 <= y <= Y1 and Z0 <= z <= Z1):
        return
    ix, iy, iz = x - X0, y - Y0, z - Z0
    if not force and GRID[ix, iy, iz]:
        return
    GRID[ix, iy, iz] = pid(block)
    SEC[ix, iy, iz] = section


def get(x: int, y: int, z: int) -> int:
    if not (X0 <= x <= X1 and Y0 <= y <= Y1 and Z0 <= z <= Z1):
        return 0
    return int(GRID[x - X0, y - Y0, z - Z0])


# ----------------------------------------------------------------------------
# Material maps
# ----------------------------------------------------------------------------
DIRT_MIX = (
    ("minecraft:dirt", 0.50), ("minecraft:coarse_dirt", 0.28),
    ("minecraft:rooted_dirt", 0.12), ("minecraft:podzol", 0.10),
)
STONE_MIX = (
    ("minecraft:stone", 0.40), ("minecraft:andesite", 0.24),
    ("minecraft:tuff", 0.14), ("minecraft:stone_bricks", 0.12),
    ("minecraft:cobblestone", 0.10),
)
MID_MIX = (
    ("minecraft:andesite", 0.36), ("minecraft:tuff", 0.32),
    ("minecraft:stone", 0.18), ("minecraft:stone_bricks", 0.14),
)
DEEP_MIX = (
    ("minecraft:deepslate", 0.40), ("minecraft:cobbled_deepslate", 0.28),
    ("minecraft:tuff", 0.18), ("minecraft:deepslate_tiles", 0.14),
)
TILE_MIX = (
    ("minecraft:deepslate_tiles", 0.46), ("minecraft:cobbled_deepslate", 0.24),
    ("minecraft:deepslate", 0.20), ("minecraft:blackstone", 0.10),
)
BOTTOM_MIX = (
    ("minecraft:blackstone", 0.38), ("minecraft:deepslate_tiles", 0.26),
    ("minecraft:cobbled_deepslate", 0.24), ("minecraft:obsidian", 0.12),
)


def mix_of(px: int, pz: int, table, cell: int, seed: int) -> str:
    n = hsh(px // cell, pz // cell, seed) * 0.85 + hsh(px, pz, seed + 5) * 0.15
    acc = 0.0
    for name, w in table:
        acc += w
        if n <= acc:
            return name
    return table[-1][0]


def mass_mat(x: int, y: int, z: int) -> str:
    """Banded strata for every block at y <= 119: pale rock, dark base."""
    if y >= 119:
        m = mix_of(x, z, DIRT_MIX, 5, 301)
    elif y >= 118:
        m = mix_of(x, z, STONE_MIX, 6, 302)
    elif y >= 115:
        m = mix_of(x, z, MID_MIX, 6, 303)
    elif y >= 111:
        m = mix_of(x, z, (("minecraft:tuff", 0.38), ("minecraft:andesite", 0.28),
                          ("minecraft:cobbled_deepslate", 0.20),
                          ("minecraft:stone", 0.14)), 6, 304)
    elif y >= 107:
        m = mix_of(x, z, (("minecraft:cobbled_deepslate", 0.34), ("minecraft:tuff", 0.30),
                          ("minecraft:andesite", 0.20), ("minecraft:stone", 0.16)), 6, 305)
    elif y >= 102:
        m = mix_of(x, z, (("minecraft:deepslate", 0.34), ("minecraft:cobbled_deepslate", 0.32),
                          ("minecraft:tuff", 0.22), ("minecraft:stone", 0.12)), 7, 306)
    elif y >= 96:
        m = mix_of(x, z, DEEP_MIX, 7, 307)
    elif y >= 92:
        m = mix_of(x, z, TILE_MIX, 5, 308)
    else:
        m = mix_of(x, z, BOTTOM_MIX, 6, 309)
    # sparse copper ore sparkle in the cliff bands
    if 104 <= y <= 117 and hsh3(x // 2, y, z // 2, 811) > 0.992:
        m = "minecraft:copper_ore" if hsh3(x, y, z, 812) > 0.4 else "minecraft:raw_copper_block"
    return m


def sand_or_gravel(x: int, z: int) -> str:
    return "minecraft:sand" if hsh(x, z, 401) > 0.35 else "minecraft:gravel"


def lagoon_out(th: float) -> float:
    """Organic outer shore of the lagoon ring (60.5 mean)."""
    return LAGOON_OUT + 2.4 * math.sin(2.0 * th + 0.40) + 1.6 * math.sin(5.0 * th + 1.90)


# ----------------------------------------------------------------------------
# Top surface classification
# ----------------------------------------------------------------------------
class Col:
    __slots__ = ("x", "z", "r", "th", "R", "rho", "pd", "t", "pad", "padm", "kind", "h", "lo")

    def __init__(self, x, z):
        self.x, self.z = x, z
        dx, dz = x - CX, z - CZ
        self.r = math.hypot(dx, dz)
        self.th = math.atan2(dz, dx)
        self.R = radius_at(self.th)
        self.rho = self.r / self.R
        self.pd, self.t = path_field(x, z)
        self.pad = pad_at(x, z)
        self.padm = pad_at(x, z, PAD_HALF + 2)
        self.kind = "void"
        self.h = TOP
        self.lo = lagoon_out(self.th)


def classify(c: Col) -> str:
    r, rho, R, pd = c.r, c.rho, c.R, c.pd
    if rho > 1.02 and not islet(c.x, c.z):
        return "void"
    if c.pad is not None:
        return "pad"
    if r <= PLAZA_R:
        return "plaza"
    if r <= c.lo and pd > PATH_HALF:
        return "water_lagoon"
    if pd <= PATH_HALF and r <= R - 1.4:
        return "path"
    if rho > 1.0:
        return "lip"
    if islet(c.x, c.z):
        return "land"
    if rho < RIDGE_RHO and pd > PATH_HALF + 1.6 and pond_at(c.x, c.z):
        return "water_pond"
    if rho >= RIDGE_RHO and c.padm is None:
        return "ridge"
    return "land"


# ----------------------------------------------------------------------------
# Top-surface materials
# ----------------------------------------------------------------------------
def bank_mat(x: int, z: int) -> str:
    """Frosty pond shore: snow, gravel, ice."""
    n = hsh(x, z, 411)
    if n > 0.62:
        return "minecraft:gravel"
    if n > 0.30:
        return SNOW
    return "minecraft:packed_ice" if n > 0.14 else "minecraft:ice"


def surface_land(x: int, z: int, near_water: bool, ridge: bool, rho: float) -> str:
    if ridge:
        n = fbm(x, z, 15.0, 2, 601)
        if n < 0.22 and hsh(x, z, 603) > 0.62:
            return "minecraft:packed_ice"
        return SNOW
    if near_water:
        return bank_mat(x, z)
    n = fbm(x, z, 32.0, 2, 604)     # grass fields
    d = fbm(x, z, 26.0, 2, 607)     # bare dirt / gravel flats
    i = fbm(x, z, 11.0, 2, 608)     # frozen puddles
    if n > 0.600:
        return "minecraft:grass_block"
    if d > 0.745:
        return "minecraft:coarse_dirt" if hsh(x // 3, z // 3, 609) > 0.45 else "minecraft:dirt_path"
    if i > 0.862:
        return "minecraft:ice"
    return SNOW


def sub_mat(x: int, y: int, z: int, top_block: str) -> str:
    if top_block in ("minecraft:sand", "minecraft:gravel"):
        return bank_mat(x, z)
    if top_block == "minecraft:ice":
        return "minecraft:packed_ice" if y >= 119 else "minecraft:gravel"
    if top_block == SNOW or top_block == "minecraft:packed_ice":
        return "minecraft:stone" if hsh(x, z, 606) > 0.72 else "minecraft:dirt"
    if top_block == "minecraft:grass_block":
        return "minecraft:dirt"
    return mix_of(x, z, DIRT_MIX, 5, 301)


def path_mat(pd: float, t: float, built: bool) -> str:
    """7 wide: 5-block tan body + 1 stone-brick kerb each side."""
    if abs(pd) > 2.05:
        return "minecraft:stone_bricks"
    band = int(t) % 11
    if band == 0:
        return "minecraft:polished_andesite"
    if built:
        return "minecraft:dirt_path" if band != 6 else "minecraft:coarse_dirt"
    if band in (1, 2):
        return "minecraft:coarse_dirt"
    return "minecraft:dirt_path" if band != 8 else "minecraft:gravel"


def plaza_mat(c: Col) -> str:
    r, pd, t = c.r, c.pd, c.t
    # the eight ribbons continue the causeways into the plaza
    if pd <= PATH_HALF:
        return path_mat(pd, t, True)
    if pd <= 4.0 and r >= 31.0:
        return "minecraft:deepslate_tiles"       # dark outline around each ribbon
    if r >= 46.8:
        return "minecraft:deepslate_tiles"       # plaza rim ring
    if r >= 45.2:
        return "minecraft:stone_bricks"
    if r >= 43.2:
        return "minecraft:white_concrete"
    if r >= 41.4:
        return "minecraft:smooth_stone"
    if 33.8 <= r <= 35.6:
        return "minecraft:light_gray_concrete"
    if r >= 28.6:
        # eight medallions on the bisectors
        for k in range(8):
            a = (k + 0.5) * math.pi / 4
            mx, mz = CX + 38.0 * math.cos(a), CZ + 38.0 * math.sin(a)
            d2 = (c.x - mx) ** 2 + (c.z - mz) ** 2
            if d2 <= 1.2 ** 2:
                return "minecraft:sea_lantern"
            if d2 <= 2.4 ** 2:
                return "minecraft:gold_block"
            if d2 <= 3.0 ** 2:
                return "minecraft:polished_andesite"
        return "minecraft:white_concrete"
    if r >= 26.6:
        return "minecraft:chiseled_stone_bricks"
    if r >= 25.4:
        return "minecraft:stone_bricks"
    # under the drum: a quiet compass so the bare site still reads
    if r >= 22.0:
        return "minecraft:polished_andesite"
    if r >= 20.6:
        return "minecraft:stone_bricks"
    if r >= 10.4:
        return "minecraft:smooth_stone"
    return "minecraft:polished_andesite"


def pad_mat(c: Col) -> str:
    i, ax, az = c.pad
    cheb = max(ax, az)
    if cheb == PAD_HALF:
        return "minecraft:stone_bricks"
    if c.pd <= PATH_HALF:
        return path_mat(c.pd, c.t, False)
    if cheb >= PAD_HALF - 2:
        return "minecraft:light_gray_concrete"
    return "minecraft:smooth_stone"


# ----------------------------------------------------------------------------
# Build
# ----------------------------------------------------------------------------
def build() -> None:
    # ---- pass 1: classify every column, find the water mask -----------------
    cols: dict[tuple[int, int], Col] = {}
    water = np.zeros((NX, NZ), bool)
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            c = Col(x, z)
            if c.rho > 1.06 and not islet(x, z):
                continue
            c.kind = classify(c)
            if c.kind == "void":
                continue
            cols[(x, z)] = c
            if c.kind in ("water_lagoon", "water_pond"):
                water[x - X0, z - Z0] = True

    # distance-to-water (chebyshev, 2 rounds of dilation) for sandy banks
    near = water.copy()
    for _ in range(2):
        n2 = near.copy()
        n2[1:, :] |= near[:-1, :]
        n2[:-1, :] |= near[1:, :]
        n2[:, 1:] |= near[:, :-1]
        n2[:, :-1] |= near[:, 1:]
        near = n2

    # ---- pass 2: columns ----------------------------------------------------
    for (x, z), c in cols.items():
        kind = c.kind
        r, R, rho, pd, t = c.r, c.R, c.rho, c.pd, c.t

        if kind == "lip":
            # one-block snow overhang around the whole rim
            put(x, TOP, z, SNOW, S_SURFACE)
            continue

        if kind in ("water_lagoon", "water_pond"):
            floor = "minecraft:gravel" if hsh(x, z, 421) > 0.945 else "minecraft:sand"
            put(x, 116, z, floor, S_MASS)
            put(x, 115, z, floor, S_MASS)
            put(x, 117, z, WATER, S_WATER)
            put(x, 118, z, WATER, S_WATER)
            continue

        if kind == "path" and PLAZA_R <= r <= c.lo + 0.5:
            # causeway deck over the lagoon (water continues underneath)
            put(x, TOP, z, path_mat(pd, t, True), S_PAVING)
            put(x, 118, z, WATER, S_WATER)
            put(x, 117, z, WATER, S_WATER)
            pier = (int(t) % 7) < 2 or abs(pd) <= 0.6
            if pier:
                for y in range(117, TOP):
                    put(x, y, z, "minecraft:stone_bricks", S_PAVING)
            continue

        bottom = max(UNDER, bottom_base(r, c.th))
        bottom = int(round(max(UNDER, bottom + (fbm(x, z, 12.0, 2, 701) - 0.5) * 2.6)))

        # ---- top height
        h = TOP
        if kind == "ridge":
            taper = min(1.0, max(0.0, (0.992 - rho) / 0.045))
            bump = fbm(x, z, 11.0, 2, 702)
            h = TOP if pd <= PATH_HALF + 1.2 else TOP + int(round(2.6 * bump * taper))
        elif kind == "land" and not near[x - X0, z - Z0]:
            # soft snow drifts away from paths, pads and water
            if (c.padm is None and pd > 6.0 and rho < 0.90
                    and fbm(x, z, 27.0, 2, 720) > 0.615):
                h = TOP + (1 if fbm(x, z, 13.0, 2, 721) > 0.58 else 0)
        c.h = h

        # ---- top layer material
        if kind == "plaza":
            top = plaza_mat(c)
        elif kind == "pad":
            top = pad_mat(c)
        elif kind == "path":
            top = path_mat(pd, t, False)
        elif kind == "ridge":
            top = surface_land(x, z, False, True, rho)
        else:
            top = surface_land(x, z, bool(near[x - X0, z - Z0]), False, rho)

        # shoreline: a stone quay around the lagoon, frosty banks elsewhere
        if kind == "land" and near[x - X0, z - Z0]:
            if r < c.lo + 3.5:
                top = "minecraft:stone_bricks"
            else:
                top = bank_mat(x, z)

        sec_top = S_PAVING if kind in ("plaza", "pad", "path") else S_SURFACE
        put(x, h, z, top, sec_top)

        # ---- surface sub-layer + stone crust
        for y in range(h - 1, max(116, h - 3) - 1, -1):
            put(x, y, z, sub_mat(x, y, z, top), S_MASS)
        for y in range(117, h - 2):
            put(x, y, z, mass_mat(x, y, z), S_MASS)

        # ---- solid outer wall, hollow belly with a 2-block shell
        if r >= R - 5.0:
            for y in range(int(bottom), 117):
                blk = mass_mat(x, y, z)
                if y in (113, 106):          # pale strata bands in the cliff
                    blk = "minecraft:stone_bricks"
                put(x, y, z, blk, S_MASS)
        else:
            for y in (int(bottom), int(bottom) + 1):
                put(x, y, z, mass_mat(x, y, z), S_MASS)

        # ---- snow dusting on the grass patches
        if (top == "minecraft:grass_block" and pd > 4.0
                and not near[x - X0, z - Z0] and fbm(x, z, 9.0, 2, 731) > 0.62):
            put(x, h + 1, z, "minecraft:snow[layers=1]", S_SURFACE)

    # ---- underside dressing: dripstone clusters ----------------------------
    rng = np.random.default_rng(97)
    for _ in range(30):
        a = rng.uniform(0, 2 * math.pi)
        rr = rng.uniform(8.0, 62.0)
        x = int(round(CX + rr * math.cos(a)))
        z = int(round(CZ + rr * math.sin(a)))
        by = int(round(max(UNDER, bottom_base(rr, a))))
        if get(x, by, z) == 0:
            continue
        for k, (ox, oz) in enumerate(((0, 0), (1, 0), (0, 1), (-1, 0))):
            gy = by - 1 - k
            if gy < UNDER or get(x + ox, gy, z + oz):
                continue
            put(x + ox, gy, z + oz,
                "minecraft:pointed_dripstone[vertical_direction=down,thickness=tip]"
                if k == 0 else "minecraft:pointed_dripstone[vertical_direction=down,thickness=frustum]",
                S_DETAIL)

    # ---- rim rock outcrops (a few so the snow rampart has teeth) -----------
    for a, rr, w in ((0.95, 88.0, 2.6), (2.75, 87.0, 2.2), (4.40, 89.0, 2.8)):
        cx, cz = CX + rr * math.cos(a), CZ + rr * math.sin(a)
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                if dx * dx + dz * dz > w * w:
                    continue
                x, z = int(round(cx + dx)), int(round(cz + dz))
                cc = Col(x, z)
                if cc.rho < 0.90 or cc.pd <= PATH_HALF + 1.0:
                    continue
                if get(x, TOP, z) == 0:
                    continue
                rock = "minecraft:andesite" if hsh(x, z, 901) > 0.35 else "minecraft:tuff"
                put(x, TOP + 1, z, rock, S_DETAIL)
                if hsh(x, z, 902) > 0.45:
                    put(x, TOP + 2, z, rock if hsh(x, z, 903) > 0.4 else SNOW, S_DETAIL)

    # ---- frozen cascades where a pond reaches the rim ---------------------
    for deg in (22.5, 112.5):
        a = math.radians(deg)
        R = radius_at(a)
        for dy in range(0, 17):
            y = TOP - 1 - dy
            for off in (-1, 0, 1):
                aa = a + off * (1.05 / R)
                rr = R + (0.45 if dy % 4 != 3 else 1.05)
                x = int(round(CX + rr * math.cos(aa)))
                z = int(round(CZ + rr * math.sin(aa)))
                if get(x, y, z) == 0:
                    continue
                blk = BLUE_ICE if off == 0 else (PACKED_ICE if (dy + off) % 2 else ICE)
                put(x, y, z, blk, S_DETAIL)

    # ---- frozen pond spill at the rim + rim ice shelves -------------------
    for deg, rr, s in ((22.5, 96.0, 3), (112.5, 96.0, 3), (247.5, 92.0, 2)):
        a = math.radians(deg)
        cx, cz = CX + rr * math.cos(a), CZ + rr * math.sin(a)
        for dx in range(-s, s + 1):
            for dz in range(-s, s + 1):
                x, z = int(round(cx + dx)), int(round(cz + dz))
                if get(x, TOP, z) == 0:
                    continue
                if dx * dx + dz * dz <= s * s and hsh(x, z, 951) > 0.30:
                    put(x, TOP, z, ICE if hsh(x, z, 952) > 0.45 else PACKED_ICE, S_DETAIL)

    # ---- underwater pickles in the lagoon --------------------------------
    for k in range(8):
        a = (k + 0.5) * math.pi / 4
        for rr in (54.0, 55.5):
            x, z = int(round(CX + rr * math.cos(a))), int(round(CZ + rr * math.sin(a)))
            if get(x, 118, z) == pid(WATER):
                put(x, 117, z, "minecraft:sea_pickle[pickles=3,waterlogged=true]", S_DETAIL)


# ----------------------------------------------------------------------------
# Meshing -> commands
# ----------------------------------------------------------------------------
def boxes_for_section(sect: int):
    """Greedy x/z rectangles per layer, then merge identical ones upward."""
    out: list[list[int]] = []          # [x0,x1,y0,y1,z0,z1,pid]
    open_boxes: dict[tuple[int, int, int, int, int], int] = {}
    for iy in range(NY):
        y = Y0 + iy
        sl = (SEC[:, iy, :] == sect) & (GRID[:, iy, :] != 0)
        if not sl.any():
            open_boxes.clear()
            continue
        ids = GRID[:, iy, :]
        visited = np.zeros((NX, NZ), bool)
        layer: list[tuple[int, int, int, int, int]] = []
        for gz in range(NZ):
            gx = 0
            while gx < NX:
                if not sl[gx, gz] or visited[gx, gz]:
                    gx += 1
                    continue
                id0 = ids[gx, gz]
                x2 = gx
                while (x2 + 1 < NX and sl[x2 + 1, gz] and ids[x2 + 1, gz] == id0
                       and not visited[x2 + 1, gz]):
                    x2 += 1
                z2 = gz
                while z2 + 1 < NZ:
                    row = slice(gx, x2 + 1)
                    if not (sl[row, z2 + 1].all() and (ids[row, z2 + 1] == id0).all()
                            and not visited[row, z2 + 1].any()):
                        break
                    z2 += 1
                visited[gx:x2 + 1, gz:z2 + 1] = True
                layer.append((gx, x2, gz, z2, int(id0)))
                gx = x2 + 1
        new_open: dict[tuple[int, int, int, int, int], int] = {}
        for (a0, a1, c0, c1, bid) in layer:
            key = (a0, a1, c0, c1, bid)
            j = open_boxes.get(key)
            if j is not None:
                out[j][3] = y           # extend upward
            else:
                out.append([a0, a1, y, y, c0, c1, bid])
                j = len(out) - 1
            new_open[key] = j
        open_boxes = new_open
    return out


def split_fill(x0: int, y0: int, z0: int, x1: int, y1: int, z1: int, blk: str,
               limit: int = 30000) -> list[str]:
    """Emit /fill commands for a box, split so no command exceeds `limit` blocks."""
    out: list[str] = []
    stack = [(x0, y0, z0, x1, y1, z1)]
    while stack:
        a0, b0, c0, a1, b1, c1 = stack.pop()
        vol = (a1 - a0 + 1) * (b1 - b0 + 1) * (c1 - c0 + 1)
        if vol <= limit:
            out.append(f"fill {a0} {b0} {c0} {a1} {b1} {c1} {blk}")
            continue
        dx, dy, dz = a1 - a0 + 1, b1 - b0 + 1, c1 - c0 + 1
        if dz >= dx and dz >= dy:
            mid = c0 + dz // 2 - 1
            stack.append((a0, b0, c0, a1, b1, mid))
            stack.append((a0, b0, mid + 1, a1, b1, c1))
        elif dy >= dx:
            mid = b0 + dy // 2 - 1
            stack.append((a0, b0, c0, a1, mid, c1))
            stack.append((a0, mid + 1, c0, a1, b1, c1))
        else:
            mid = a0 + dx // 2 - 1
            stack.append((a0, b0, c0, mid, b1, c1))
            stack.append((mid + 1, b0, c0, a1, b1, c1))
    out.sort()
    return out


def box_commands(boxes) -> list[str]:
    cmds: list[str] = []
    for (a0, a1, b0, b1, c0, c1, bid) in boxes:
        x0, x1 = a0 + X0, a1 + X0
        y0, y1 = b0, b1
        z0, z1 = c0 + Z0, c1 + Z0
        blk = NAMES[bid]
        if x0 == x1 and y0 == y1 and z0 == z1:
            cmds.append(f"setblock {x0} {y0} {z0} {blk}")
        else:
            cmds.extend(split_fill(x0, y0, z0, x1, y1, z1, blk))
    return cmds


# ----------------------------------------------------------------------------
# Output writers
# ----------------------------------------------------------------------------
def write_csv(path: str) -> int:
    rows = 0
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["x", "y", "z", "block", "section"])
        for sect in (S_MASS, S_SURFACE, S_WATER, S_PAVING, S_DETAIL):
            xs, ys, zs = np.nonzero(SEC == sect)
            order = np.lexsort((xs, zs, ys))
            for k in order:
                gx, gy, gz = xs[k], ys[k], zs[k]
                rows += 1
                w.writerow([gx + X0, gy + Y0, gz + Z0, NAMES[GRID[gx, gy, gz]], SECTION_NAME[sect]])
    return rows


def write_dump(path: str) -> int:
    n = 0
    with gzip.open(path, "wt", encoding="utf-8", compresslevel=6) as f:
        f.write(f"MCMDUMP 1 {NX} {NY} {NZ}\n")
        xs, ys, zs = np.nonzero(GRID)
        order = np.lexsort((xs, zs, ys))
        buf = []
        for k in order:
            gx, gy, gz = xs[k], ys[k], zs[k]
            buf.append(f"{gx} {gy} {gz} {NAMES[GRID[gx, gy, gz]]}\n")
            n += 1
            if len(buf) >= 20000:
                f.write("".join(buf))
                buf.clear()
        f.write("".join(buf))
    return n


def write_functions(root: str, chunk: int = 8000) -> list[tuple[str, int]]:
    fn_dir = os.path.join(root, "datapack", "data", "mcchub", "function", "lbr1")
    os.makedirs(fn_dir, exist_ok=True)
    written: list[tuple[str, int]] = []
    calls: list[str] = []

    # 00 clear: guarantee the site is empty before the island lands
    clear: list[str] = []
    for (ya, yb) in ((88, 100), (101, 112), (113, 127)):
        clear.extend(split_fill(X0, ya, Z0, X1, yb, Z1, "minecraft:air"))
    name = "00_clear"
    with open(os.path.join(fn_dir, name + ".mcfunction"), "w", encoding="utf-8") as f:
        f.write("\n".join(clear) + "\n")
    written.append((name, len(clear)))
    calls.append(name)

    idx = 1
    for sect in (S_MASS, S_SURFACE, S_WATER, S_PAVING, S_DETAIL):
        cmds = box_commands(boxes_for_section(sect))
        label = SECTION_NAME[sect]
        parts = [cmds[i:i + chunk] for i in range(0, len(cmds), chunk)] or [[]]
        for pi, part in enumerate(parts, 1):
            name = f"{idx:02d}_{label}_{pi:02d}"
            with open(os.path.join(fn_dir, name + ".mcfunction"), "w", encoding="utf-8") as f:
                f.write("\n".join(part) + ("\n" if part else ""))
            written.append((name, len(part)))
            calls.append(name)
        idx += 1

    with open(os.path.join(fn_dir, "load.mcfunction"), "w", encoding="utf-8") as f:
        f.write("# LBR-1 base island  C=5000,120,5000\n")
        for c in calls:
            f.write(f"function mcchub:lbr1/{c}\n")
    return written


def write_pack(root: str) -> None:
    os.makedirs(os.path.join(root, "datapack"), exist_ok=True)
    with open(os.path.join(root, "datapack", "pack.mcmeta"), "w", encoding="utf-8") as f:
        f.write('{\n  "pack": {\n    "pack_format": 48,\n'
                '    "description": "MCC festive hub replica - LBR-1 base island"\n  }\n}\n')


# ----------------------------------------------------------------------------
# Stats
# ----------------------------------------------------------------------------
def stats() -> dict:
    total = int(np.count_nonzero(GRID))
    per = {SECTION_NAME[s]: int(np.count_nonzero(SEC == s)) for s in
           (S_MASS, S_SURFACE, S_WATER, S_PAVING, S_DETAIL)}
    ids, counts = np.unique(GRID[GRID > 0], return_counts=True)
    kinds = sorted(((NAMES[int(i)], int(c)) for i, c in zip(ids, counts)), key=lambda t: -t[1])
    return dict(total=total, per=per, kinds=kinds)


def main() -> None:
    here = os.path.dirname(os.path.abspath(__file__))
    print("building island ...")
    build()
    st = stats()
    print(f"  blocks: {st['total']:,}")
    for k, v in st["per"].items():
        print(f"    {k:8s} {v:,}")
    print("  top block types:")
    for name, c in st["kinds"][:12]:
        print(f"    {c:7d}  {name}")
    print("writing blocks.csv ...")
    n = write_csv(os.path.join(here, "blocks.csv"))
    print(f"  rows: {n:,}")
    print("writing island.mcmdump.gz ...")
    write_dump(os.path.join(here, "island.mcmdump.gz"))
    print("writing functions ...")
    write_pack(here)
    for name, count in write_functions(here):
        print(f"    {name:22s} {count:6d} commands")
    print("done")


if __name__ == "__main__":
    main()
