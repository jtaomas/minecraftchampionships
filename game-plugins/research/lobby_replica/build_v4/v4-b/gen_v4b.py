#!/usr/bin/env python3
"""
V4-B — THE CHAMPS mountain, Hall of Fame biosphere, rainbow bridge, snow globe.

Build origin C4 = 0,120,0 (world coords, the same origin V4-A uses).

Sites (world coords):
    THE CHAMPS        rock massif   x -116..-74, z -30..+20, peak ~y=170
    Hall of Fame      glass dome    centre (-70, +45), r = 20, drum 123..129
    rainbow bridge    x -74..-31, z +1..+8, deck y 155 -> crown 176 -> 155
    snow globe        centre (+75, +55), glass r = 14, centre y = 140

Everything is vanilla Java 1.21.1.  The script is the single source of truth:
it writes blocks.csv, the chunked .mcfunction files, the datapack and the
preview dumps, then prints the checks.
"""
import gzip
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__))
FN_DIR = os.path.join(HERE, "functions")
DP_DIR = os.path.join(HERE, "datapack", "data", "mcchub", "function", "v4b")
NS = "mcchub"
CHUNK_CMDS = 4000
Y0 = 121                     # nothing at or below y = 120 (island surface)

# ---------------------------------------------------------------- sites ----
MOUNT = dict(x0=-116, x1=-70, z0=-30, z1=20)
SIGN_Z = 12                              # backdrop plane of the marquee
SHOULDER_Y = 155                         # rock shoulder the bridge springs from

HOF_C = (-70.0, 45.0)
HOF_PLINTH_R = 21.5
HOF_R = 20.0
HOF_FLOOR = 123
HOF_DRUM = (123, 129)
HOF_DOME0 = 130                          # dome springing (sphere centre)
HOF_APEX = 150

BR_X = (-75, -31)
BR_Z = (-5, 2)
BR_END_Y = 155
BR_CROWN_Y = 176

GL_C = (75.0, 55.0)
GL_CY = 140.0
GL_R = 14.0
GL_BASE_TOP = 127

# preview / check region  (x0, z0, y0, x1, z1, y1)
RG = (-122, -36, 118, 94, 80, 188)

SECTIONS = [
    ("10_mountain_rock", "THE CHAMPS rock massif"),
    ("11_mountain_lava", "lava veins and glow"),
    ("12_mountain_snow", "snow caps and ledges"),
    ("13_mountain_terrace", "shoulder terrace, stair, parapet, tunnel door"),
    ("14_champs_sign", "THE CHAMPS marquee: backdrop, board, letters, crown, bulbs"),
    ("20_hof_plinth", "Hall of Fame plinth, drum, doorway"),
    ("21_hof_dome", "Hall of Fame glass dome and ribs"),
    ("22_hof_interior", "interior floor, columns, dais, coin, rainbow inlay"),
    ("23_hof_sign", "HALL OF FAME sign on the south face"),
    ("30_bridge", "rainbow bridge"),
    ("31_bridge_fittings", "bridge deck snow, lanterns, landings"),
    ("40_globe_base", "snow globe stone base"),
    ("41_globe_glass", "snow globe glass sphere"),
    ("42_globe_diorama", "castle, trees, presents, snow"),
]


# ------------------------------------------------------------- helpers ----
def snow(n):
    return f"minecraft:snow[layers={n}]"


def hsh(x, y, z):
    """Deterministic 0..999 hash, stable across runs."""
    v = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791)
    v = (v ^ (v >> 13)) * 1274126177
    return (v ^ (v >> 16)) & 1023


def pick(table, x, y, z, k=0):
    return table[(hsh(x, y, z) + k * 271) % len(table)]


def lerp(a, b, t):
    return a + (b - a) * t


def clamp(v, a, b):
    return a if v < a else b if v > b else v


# --------------------------------------------------------------------------
# 3x5 block font
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


def text_width(line):
    return sum(len(FONT[c][0]) + 1 for c in line) - 1


def text_pixels(line):
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
        self.b = {}
        self.sec = {}

    def set(self, x, y, z, blk, sec):
        if blk is None or y < Y0:
            return False
        k = (int(x), int(y), int(z))
        self.b[k] = blk
        self.sec[k] = sec
        return True

    def get(self, x, y, z):
        return self.b.get((int(x), int(y), int(z)))

    def drop(self, x, y, z):
        self.b.pop((int(x), int(y), int(z)), None)
        self.sec.pop((int(x), int(y), int(z)), None)

    def count(self):
        return len(self.b)


CARVES = []          # boxes the rock fill must not enter


def carved(x, y, z):
    for (a, b, c, d, e, f) in CARVES:
        if a <= x <= d and b <= y <= e and c <= z <= f:
            return True
    return False


# ==========================================================================
# 1. THE CHAMPS — rock massif
# ==========================================================================
# Height profiles: the massif is a north-south ridge with a broad east
# shoulder the rainbow bridge springs from and a sheer south face carrying
# the marquee.
X_PROF = [(-118, 6), (-114, 16), (-110, 24), (-106, 30), (-102, 34), (-98, 36),
          (-94, 35), (-90, 33), (-86, 30), (-82, 26), (-78, 21), (-75, 17),
          (-73, 13), (-71, 8)]
Z_PROF = [(-32, 0.45), (-28, 0.62), (-24, 0.78), (-20, 0.90), (-16, 0.97),
          (-12, 1.00), (-8, 1.02), (-4, 1.02), (0, 1.02), (4, 1.00),
          (7, 0.92), (9, 0.80), (11, 0.66)]

SPIRES = [
    # x,     z,   h,   r     (rock spikes standing out of the ridge)
    (-101, -26, 40, 6.0),
    (-97, -21, 48, 4.5),
    (-104, -17, 44, 5.0),
    (-95, -14, 54, 4.5),
    (-100, -10, 51, 4.0),
    (-92, -8, 47, 4.0),
    (-106, -6, 43, 4.5),
    (-98, -3, 56, 4.0),
    (-89, -2, 46, 4.5),
    (-102, 2, 46, 4.0),
    (-95, 3, 50, 3.8),
    (-86, 3, 42, 4.0),
    (-106, 7, 42, 5.0),
    (-92, 8, 46, 4.0),
    (-99, 9, 44, 4.0),
    (-86, 9, 38, 4.5),
    (-109, 9, 36, 5.0),
    (-82, -6, 40, 4.5),
    (-79, 2, 38, 4.0),
    (-111, -12, 35, 5.5),
]


def interp(prof, v):
    if v <= prof[0][0]:
        return prof[0][1]
    if v >= prof[-1][0]:
        return prof[-1][1]
    for i in range(len(prof) - 1):
        a, b = prof[i], prof[i + 1]
        if a[0] <= v <= b[0]:
            t = (v - a[0]) / float(b[0] - a[0])
            return a[1] + (b[1] - a[1]) * (t * t * (3.0 - 2.0 * t))
    return prof[-1][1]


SIGN_XC = -89


def mount_h(x, z):
    """Rock height above y=120 at column (x,z); 0 = outside the massif."""
    if z >= 12:
        # apron in front of the marquee; kept low so the sign stays visible
        if z > 20:
            return 0
        if -104 <= x <= -73:
            return 2
        return max(1, int(round(6.0 - (z - 12) * 0.7)))

    # the cliff carrying the marquee is not a flat wall: its foot zigzags
    z_edge = 11 - (hsh(x, 37, 5) % 4)
    if z > z_edge:
        return max(2, 4 - (z - z_edge))

    h = interp(X_PROF, x) * interp(Z_PROF, z)
    # folds down the flanks so the massif is never a smooth slab
    h *= 0.85 + 0.15 * math.sin(x * 0.66 + z * 0.21)
    # taper the west and north edges so the massif grows out of the island
    ew = clamp((x + 118) / 9.0, 0.0, 1.0)
    en = clamp((z + 32) / 8.0, 0.0, 1.0)
    h *= (ew * ew * (3 - 2 * ew)) * (en * en * (3 - 2 * en))
    for (sx, sz, sh, sr) in SPIRES:
        d = math.hypot(x - sx + 0.5, (z - sz) * 0.78)
        if d < sr:
            h = max(h, sh * (1.0 - d / sr) ** 0.80)

    # rock that backs the marquee: keep the cliff under the sign tall enough
    if -104 <= x <= -72 and z >= 5:
        h = max(h, 40.0 + 6.0 * math.exp(-((x - SIGN_XC) / 12.0) ** 2))
    # ragged cliff edge + rock spurs poking into the plaza
    if x > -78:
        h *= 0.72 + 0.28 * ((hsh(x, 17, z) % 5) / 4.0)
    if x > -72:
        if hsh(x, 23, z) % 6 == 0 and z > -22:
            h = 6.0 + (hsh(x, 29, z) % 7) * 2.0
        else:
            h = 0.0
    h += ((hsh(x, 7, z) % 5) - 2) * 0.5
    # strata: regular ledges cut into the flanks
    hi = int(round(h))
    if hi >= 12 and (hi % 8) in (0, 1):
        h -= 2
    return max(0, int(round(h / 2.0)) * 2)


def build_ledges(doc, tops):
    """Rock lips under overhangs on the cliff faces (breaks up the flat wall)."""
    sec = "10_mountain_rock"
    for (x, z), top in tops.items():
        for (dx, dz) in ((0, 1), (0, -1), (1, 0), (-1, 0)):
            nb = tops.get((x + dx, z + dz))
            if nb is None or top - nb < 4 or hsh(x, nb, z) % 4:
                continue
            px, pz = x + dx, z + dz
            doc.set(px, nb + 3, pz, pick(ROCK_MID, px, nb + 3, pz), sec)
            if hsh(px, 3, pz) % 3 == 0:
                doc.set(px, nb + 4, pz, pick(ROCK_DARK, px, nb + 4, pz), sec)


ROCK_DARK = ("minecraft:deepslate", "minecraft:cobbled_deepslate",
             "minecraft:deepslate", "minecraft:blackstone", "minecraft:tuff")
ROCK_MID = ("minecraft:andesite", "minecraft:stone", "minecraft:tuff",
            "minecraft:andesite", "minecraft:cobblestone")
ROCK_HI = ("minecraft:tuff", "minecraft:calcite", "minecraft:light_gray_concrete_powder",
           "minecraft:tuff", "minecraft:diorite")
ROCK_CAP = ("minecraft:calcite", "minecraft:light_gray_concrete_powder", "minecraft:tuff")


def rock_block(x, y, z, top):
    rel = (top - Y0) / 52.0
    r = hsh(x, y, z) % 100
    if rel < 0.20:
        return pick(ROCK_DARK, x, y, z) if r < 88 else pick(ROCK_MID, x, y, z)
    if rel < 0.46:
        if r < 62:
            return pick(ROCK_DARK, x, y, z)
        return pick(ROCK_MID, x, y, z, 1)
    if rel < 0.74:
        if r < 26:
            return pick(ROCK_DARK, x, y, z, 2)
        if r < 84:
            return pick(ROCK_MID, x, y, z, 2)
        return pick(ROCK_HI, x, y, z, 3)
    if rel < 0.90:
        if r < 14:
            return pick(ROCK_MID, x, y, z, 3)
        return pick(ROCK_HI, x, y, z, 3)
    return pick(ROCK_HI, x, y, z, 4) if r < 55 else pick(ROCK_CAP, x, y, z, 4)


def build_mountain(doc):
    sec = "10_mountain_rock"
    tops = {}
    for z in range(MOUNT["z0"], MOUNT["z1"] + 1):
        for x in range(MOUNT["x0"], MOUNT["x1"] + 1):
            h = mount_h(x, z)
            if h <= 0:
                continue
            top = Y0 + h - 1
            tops[(x, z)] = top
            for y in range(Y0, top + 1):
                if carved(x, y, z):
                    continue
                doc.set(x, y, z, rock_block(x, y, z, top), sec)
    return tops


def build_lava(doc, tops):
    """Orange veins running down the exposed flanks (reference: lava streaks)."""
    sec = "11_mountain_lava"
    starts = [(-89, 9), (-86, 8), (-94, 10), (-99, 9), (-84, 9), (-103, 9),
              (-91, 1), (-97, -7), (-105, -15), (-82, -9), (-109, -1), (-100, 11),
              (-87, -14), (-95, -22), (-111, 6)]
    for (sx, sz) in starts:
        x, z = sx, sz
        seen = set()
        for step in range(30):
            if (x, z) not in tops or (x, z) in seen:
                break
            seen.add((x, z))
            top = tops[(x, z)]
            blk = "minecraft:magma_block" if hsh(x, step, z) % 3 else "minecraft:orange_concrete"
            if hsh(x, step + 5, z) % 11 == 0:
                blk = "minecraft:glowstone"
            doc.set(x, top, z, blk, sec)
            if hsh(x, step, z) % 4 == 0 and (x, z + 1) in tops:
                doc.set(x, tops[(x, z + 1)], z + 1, "minecraft:magma_block", sec)
            best = None
            for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, z + dz)
                if n in seen or n not in tops:
                    continue
                if tops[n] > top:
                    continue
                if best is None or tops[n] < best[0]:
                    best = (tops[n], n)
            if best is None:
                break
            x, z = best[1]
    # glow pockets where the veins reach the ground
    for (x, z) in ((-88, 11), (-97, 11), (-84, 11), (-104, 10), (-92, 11)):
        if (x, z) in tops:
            doc.set(x, tops[(x, z)], z, "minecraft:glowstone", sec)


def build_snow(doc, tops):
    sec = "12_mountain_snow"
    for (x, z), top in tops.items():
        if top < Y0 + 20:
            continue
        nb = [tops.get((x + 1, z), Y0), tops.get((x - 1, z), Y0),
              tops.get((x, z + 1), Y0), tops.get((x, z - 1), Y0)]
        if max(nb) > top:                     # not an upward face
            continue
        local_max = min(nb) < top
        if top >= Y0 + 38 and local_max:      # peaks: solid cap
            doc.set(x, top, z, "minecraft:snow_block", sec)
            if top >= Y0 + 45 and hsh(x, 1, z) % 3 == 0:
                doc.set(x, top + 1, z, snow(1), sec)
        elif top >= Y0 + 30 and hsh(x, 2, z) % 5 < 2:
            doc.set(x, top, z, "minecraft:snow_block", sec)
        elif hsh(x, 3, z) % 7 == 0:
            doc.set(x, top, z, snow(1), sec)


# --------------------------------------------------------------- terrace --
BELV = dict(x0=-81, x1=-75, z0=-6, z1=3)


def build_terrace(doc, tops):
    """Stone belvedere on the ridge shoulder: the rainbow bridge springs from it."""
    sec = "13_mountain_terrace"
    deck = SHOULDER_Y
    for x in range(BELV["x0"], BELV["x1"] + 1):
        for z in range(BELV["z0"], BELV["z1"] + 1):
            ground = tops.get((x, z), Y0 - 1)
            top = max(ground, Y0 - 1)
            for y in range(top + 1, deck - 1):
                doc.set(x, y, z, "minecraft:stone_bricks" if (y - top) % 5
                        else "minecraft:polished_andesite", sec)
            doc.set(x, deck - 1, z, "minecraft:stone_bricks", sec)
            doc.set(x, deck, z, "minecraft:smooth_stone", sec)
            # red inlay stripe along the middle of the deck
            if z in (2, 3, 4, 5):
                doc.set(x, deck, z, "minecraft:red_concrete", sec)
    # white parapet around the belvedere, open on the east (bridge) and west
    for z in range(BELV["z0"], BELV["z1"] + 1):
        for x in (BELV["x0"], BELV["x1"]):
            if x == BELV["x1"] and BR_Z[0] - 1 <= z <= BR_Z[1] + 1:
                continue
            doc.set(x, deck + 1, z, "minecraft:smooth_quartz", sec)
    for x in range(BELV["x0"], BELV["x1"] + 1):
        for z in (BELV["z0"], BELV["z1"]):
            doc.set(x, deck + 1, z, "minecraft:smooth_quartz", sec)
    # gold finials on the four corners
    for (x, z) in ((BELV["x0"], BELV["z0"]), (BELV["x0"], BELV["z1"]),
                   (BELV["x1"], BELV["z0"]), (BELV["x1"], BELV["z1"])):
        doc.set(x, deck + 2, z, "minecraft:gold_block", sec)
    # lantern posts framing the bridge mouth
    for z in (BR_Z[0] - 1, BR_Z[1] + 1):
        for y in range(deck + 1, deck + 4):
            doc.set(BELV["x1"], y, z, "minecraft:white_concrete", sec)
        doc.set(BELV["x1"], deck + 4, z, "minecraft:gold_block", sec)
        doc.set(BELV["x1"], deck + 5, z, "minecraft:lantern", sec)

    # tunnel door at the foot of the plaza cliff (the way up to the belvedere)
    for z in range(3, 8):
        for y in range(Y0, Y0 + 5):
            doc.drop(-73, y, z)
    for z in range(3, 8):
        doc.set(-73, Y0 + 5, z, "minecraft:stone_bricks", sec)
    for z in (2, 8):
        for y in range(Y0, Y0 + 6):
            doc.set(-73, y, z, "minecraft:chiseled_stone_bricks", sec)
    for d in range(1, 5):
        for z in range(3, 8):
            for y in range(Y0, Y0 + 5):
                doc.set(-73 - d, y, z, "minecraft:polished_deepslate", sec)
    doc.set(-75, Y0 + 4, 5, "minecraft:lantern", sec)
    doc.set(-73, Y0 + 6, 2, "minecraft:gold_block", sec)
    doc.set(-73, Y0 + 6, 8, "minecraft:gold_block", sec)

    # narrow viewing ledge along the foot of the marquee
    for x in range(-103, -72):
        doc.set(x, Y0, 13, "minecraft:stone_bricks", sec)
        if x % 8 == 0:
            doc.set(x, Y0 + 1, 14, "minecraft:smooth_quartz", sec)
            doc.set(x, Y0 + 2, 14, "minecraft:lantern", sec)


# ==========================================================================
# 2. THE CHAMPS marquee
# ==========================================================================
SIGN_Z = 12                        # backdrop plane (the cliff behind is z = 11)
CROWN = (    ".X.....X.....X.",
    ".X.....X.....X.",
    "XXX...XXX...XXX",
    "XXX...XXX...XXX",
    "XXXXXXXXXXXXXXX",
    "XX.XXXXXXXXX.XX",
    "XXXXXXXXXXXXXXX",
    "XXXXXXXXXXXXXXX",
)


def build_sign(doc):
    sec = "14_champs_sign"
    zb = SIGN_Z                 # backdrop plane
    zm = SIGN_Z + 1             # board plane
    zl = SIGN_Z + 2             # letter plane

    # ---- red backdrop: a stepped, organic outline ----------------------
    x0, x1 = -104, -74
    y0, y1 = 130, 161
    cx = SIGN_XC
    for y in range(y0, y1 + 1):
        t = (y - y0) / float(y1 - y0)
        half = 15.0 - 4.6 * (t ** 3.4) + 1.1 * math.sin(t * 8.0)
        for x in range(x0, x1 + 1):
            if abs(x - cx) > half + (0.9 if hsh(x, y, 1) % 3 else 0):
                continue
            blk = "minecraft:red_concrete"
            if (y - y0) % 6 == 0 or hsh(x, y, zb) % 13 == 0:
                blk = "minecraft:red_terracotta"
            doc.set(x, y, zb, blk, sec)

    # gold studs scattered over the backdrop (reference: gold squares in the red)
    for y in range(y0 + 1, y1):
        for x in range(x0 + 1, x1):
            if hsh(x, y, 3) % 15 == 0 and doc.get(x, y, zb):
                doc.set(x, y, zb, "minecraft:gold_block", sec)

    # ---- black board ---------------------------------------------------
    bx0, bx1 = -101, -77                # 25 wide
    by0, by1 = 134, 150                 # 17 tall
    for y in range(by0, by1 + 1):
        for x in range(bx0, bx1 + 1):
            doc.set(x, y, zm, "minecraft:black_concrete", sec)
    for x in range(bx0, bx1 + 1):
        doc.set(x, by0 - 1, zm, "minecraft:black_concrete", sec)

    # ---- letters -------------------------------------------------------
    def put(line, ytop, xc, colour):
        w = text_width(line)
        xs = int(round(xc - w / 2.0))
        for (c, r) in text_pixels(line):
            doc.set(xs + c, ytop + r, zl, colour, sec)

    put("CHAMPS", 136, cx, "minecraft:white_concrete")
    put("THE", 143, cx, "minecraft:white_concrete")

    # ---- gold crown above the board ------------------------------------
    cr_x0 = -96
    cr_y0 = 152
    for r, row in enumerate(CROWN):
        y = cr_y0 + (len(CROWN) - 1 - r)
        for c, ch in enumerate(row):
            if ch == "X":
                blk = "minecraft:gold_block"
                if (r * 3 + c) % 11 == 3:
                    blk = "minecraft:yellow_concrete"
                doc.set(cr_x0 + c, y, zm, blk, sec)

    # ---- marquee bulbs around the board --------------------------------
    for x in range(bx0 - 1, bx1 + 2, 2):
        doc.set(x, by0 - 2, zm, "minecraft:gold_block", sec)
        doc.set(x, by1 + 1, zm, "minecraft:gold_block", sec)
    for y in range(by0, by1 + 1, 2):
        doc.set(bx0 - 1, y, zm, "minecraft:gold_block", sec)
        doc.set(bx1 + 1, y, zm, "minecraft:gold_block", sec)

    # ---- red banner flags on poles flanking the marquee ----------------
    for fx in (-108, -70):
        for y in range(Y0, 158):
            doc.set(fx, y, SIGN_Z + 1, "minecraft:dark_oak_fence", sec)
        for dy in range(0, 6):
            for dx in range(1, 5):
                doc.set(fx + dx, 157 - dy, SIGN_Z + 1, "minecraft:red_concrete", sec)
        doc.set(fx + 2, 158, SIGN_Z + 1, "minecraft:gold_block", sec)
        doc.set(fx, 158, SIGN_Z + 1, "minecraft:gold_block", sec)
        doc.set(fx + 4, 158, SIGN_Z + 1, "minecraft:gold_block", sec)


# ==========================================================================
# 3. Hall of Fame biosphere
# ==========================================================================
def build_hof(doc):
    sec = "20_hof_plinth"
    cx, cz = HOF_C

    def d2(x, z):
        return math.hypot(x + 0.5 - cx, z + 0.5 - cz)

    # plinth: two courses, the lower one wider
    for z in range(int(cz) - 24, int(cz) + 25):
        for x in range(int(cx) - 24, int(cx) + 25):
            d = d2(x, z)
            if d <= HOF_PLINTH_R:
                doc.set(x, 121, z, "minecraft:stone_bricks", sec)
            if d <= HOF_PLINTH_R - 0.5:
                doc.set(x, 122, z, "minecraft:polished_andesite", sec)
            if d <= HOF_R - 0.7:
                doc.set(x, HOF_FLOOR, z, "minecraft:red_concrete", sec)

    # drum wall 123..129: gold cornice on top, quartz field, pilasters
    for z in range(int(cz) - 22, int(cz) + 23):
        for x in range(int(cx) - 22, int(cx) + 23):
            d = d2(x, z)
            if not (HOF_R - 1.1 <= d <= HOF_R + 0.35):
                continue
            ang = (math.degrees(math.atan2(z + 0.5 - cz, x + 0.5 - cx)) + 360) % 360
            pilaster = (ang % 30.0) < 4.0
            for y in range(HOF_DRUM[0], HOF_DRUM[1] + 1):
                # doorway: 7 wide (z 41..47) at x = -50, y 123..128
                if x >= -52 and 41 <= z <= 47 and y <= 128:
                    continue
                if y == HOF_DRUM[1]:
                    blk = "minecraft:yellow_concrete"          # gold band under the glass
                elif pilaster:
                    blk = "minecraft:white_concrete"
                else:
                    blk = "minecraft:smooth_quartz"
                doc.set(x, y, z, blk, sec)

    # doorway frame
    for z in range(40, 49):
        for y in range(121, 130):
            if not (z in (40, 48) or y == 129):
                continue
            for x in (-52, -51, -50):
                doc.set(x, y, z, "minecraft:smooth_quartz", sec)
    for z in (40, 48):
        doc.set(-49, 121, z, "minecraft:chiseled_stone_bricks", sec)
        doc.set(-49, 129, z, "minecraft:gold_block", sec)
    doc.set(-51, 130, 44, "minecraft:gold_block", sec)

    # red carpet through the door to the dais
    for x in range(-52, int(cx) + 1):
        for z in range(43, 47):
            if d2(x, z) <= HOF_PLINTH_R - 1.1:
                doc.set(x, HOF_FLOOR, z, "minecraft:red_carpet", "22_hof_interior")

    # ---- glass dome ----------------------------------------------------
    sec = "21_hof_dome"
    R = HOF_R
    cy = HOF_DOME0
    for y in range(cy, HOF_APEX + 2):
        dy = y - cy
        for z in range(int(cz) - 22, int(cz) + 23):
            for x in range(int(cx) - 22, int(cx) + 23):
                dx, dz = x + 0.5 - cx, z + 0.5 - cz
                d3 = math.sqrt(dx * dx + dz * dz + dy * dy)
                if not (R - 0.85 <= d3 <= R + 0.35):
                    continue
                # 8 meridian ribs, sill ring, quartz apex cap
                ang = (math.degrees(math.atan2(dz, dx)) + 360) % 360
                rib = min(abs(((ang + 22.5) % 45.0) - 22.5), 99) < 2.1
                ring = (y == cy)
                if y >= HOF_APEX - 1 and dx * dx + dz * dz < 7:
                    blk = "minecraft:smooth_quartz"
                elif rib or ring:
                    blk = "minecraft:white_concrete"
                else:
                    blk = "minecraft:glass"
                doc.set(x, y, z, blk, sec)

    # ---- interior ------------------------------------------------------
    sec = "22_hof_interior"
    # rainbow inlay ring r 16..17, six 60 deg arcs
    arcs = ["minecraft:red_concrete", "minecraft:orange_concrete", "minecraft:yellow_concrete",
            "minecraft:lime_concrete", "minecraft:light_blue_concrete", "minecraft:purple_concrete"]
    for z in range(int(cz) - 20, int(cz) + 21):
        for x in range(int(cx) - 20, int(cx) + 21):
            d = d2(x, z)
            if 16.0 <= d <= 17.2:
                a = (math.degrees(math.atan2(z + 0.5 - cz, x + 0.5 - cx)) + 360) % 360
                doc.set(x, HOF_FLOOR, z, arcs[int(a // 60) % 6], sec)

    # 8 slim columns r = 13.5
    for k in range(8):
        a = math.radians(22.5 + k * 45.0)
        px = int(round(cx + 13.5 * math.cos(a)))
        pz = int(round(cz + 13.5 * math.sin(a)))
        for y in range(HOF_FLOOR + 1, HOF_DRUM[1]):
            doc.set(px, y, pz, "minecraft:quartz_pillar", sec)
        doc.set(px, HOF_DRUM[1], pz, "minecraft:gold_block", sec)

    # central dais
    for z in range(int(cz) - 7, int(cz) + 8):
        for x in range(int(cx) - 7, int(cx) + 8):
            d = d2(x, z)
            if d <= 6.6:
                doc.set(x, HOF_FLOOR, z, "minecraft:white_concrete", sec)
            if d <= 5.4:
                doc.set(x, HOF_FLOOR + 1, z, "minecraft:smooth_quartz", sec)
            if 4.2 <= d <= 5.4:
                doc.set(x, HOF_FLOOR + 1, z, "minecraft:gold_block", sec)
            if d <= 3.2:
                doc.set(x, HOF_FLOOR + 2, z, "minecraft:smooth_quartz", sec)
                doc.set(x, HOF_FLOOR + 3, z, "minecraft:gold_block", sec)

    # gold championship coin on the dais, facing the door (+x)
    coin_x = int(cx)
    coin_y = HOF_FLOOR + 5
    for y in range(coin_y - 6, coin_y + 7):
        for dz in range(-6, 7):
            dd = math.hypot(dz, y - coin_y)
            if dd <= 5.0:
                blk = "minecraft:gold_block"
                if dd > 4.0:
                    blk = "minecraft:yellow_concrete"
                if abs(dz) <= 1 and -2 <= y - coin_y <= 2:
                    blk = "minecraft:orange_terracotta"
                doc.set(coin_x, y, int(cz) + dz, blk, sec)
    for y in range(coin_y - 6, coin_y + 7):
        doc.set(coin_x + 1, y, int(cz), "minecraft:gold_block", sec)

    # hanging lanterns in the dome
    for k in range(4):
        a = math.radians(45 + k * 90)
        px, pz = int(cx + 9 * math.cos(a)), int(cz + 9 * math.sin(a))
        for y in range(138, 142):
            doc.set(px, y, pz, "minecraft:chain", sec)
        doc.set(px, 137, pz, "minecraft:lantern", sec)


def hof_surf_z(x, y):
    """z of the drum/dome outer surface at (x, y) - the marquee hugs this."""
    dx = x + 0.5 - HOF_C[0]
    dy = max(0.0, y - HOF_DOME0)
    return HOF_C[1] + math.sqrt(max(0.0, HOF_R * HOF_R - dx * dx - dy * dy))


def build_hof_sign(doc):
    """HALL OF FAME marquee, curved onto the drum's south face (HallOfFame.png)."""
    sec = "23_hof_sign"
    cx, cz = HOF_C
    cx_i = int(cx)

    px0, py0, pw, ph = cx_i - 13, 122, 27, 21      # quartz frame + red panel
    for y in range(py0, py0 + ph):
        for x in range(px0, px0 + pw):
            zz = int(round(hof_surf_z(x, y))) + 1
            border = (x in (px0, px0 + pw - 1) or y in (py0, py0 + ph - 1))
            doc.set(x, y, zz, "minecraft:smooth_quartz" if border
                    else "minecraft:red_concrete", sec)
    # stepped crown on top of the frame
    for x in range(cx_i - 5, cx_i + 6):
        zz = int(round(hof_surf_z(x, py0 + ph - 1))) + 1
        doc.set(x, py0 + ph, zz, "minecraft:smooth_quartz", sec)
    for x in range(cx_i - 2, cx_i + 3):
        zz = int(round(hof_surf_z(x, py0 + ph - 1))) + 1
        doc.set(x, py0 + ph + 1, zz, "minecraft:smooth_quartz", sec)
    for x in (cx_i - 11, cx_i - 8, cx_i + 8, cx_i + 11):
        zz = int(round(hof_surf_z(x, py0 + ph - 1))) + 1
        doc.set(x, py0 + ph, zz, "minecraft:gold_block", sec)

    # HALL / OF / FAME stacked directly on the red panel (no black board:
    # the reference mounts the letters straight onto the red facade)
    def put_at(line, ytop, x0, colour):
        for (c, r) in text_pixels(line):
            x = x0 + c
            y = ytop + r
            zz = int(round(hof_surf_z(x, y))) + 2
            doc.set(x, y, zz, colour, sec)

    put_at("FAME", 124, int(round(cx - text_width("FAME") / 2.0)), "minecraft:white_concrete")
    put_at("OF", 130, int(round(cx - text_width("OF") / 2.0)), "minecraft:gold_block")
    put_at("HALL", 136, int(round(cx - text_width("HALL") / 2.0)), "minecraft:white_concrete")


# ==========================================================================
# 4. Rainbow bridge
# ==========================================================================
BANDS = [
    ("minecraft:red_concrete", 0),        # deck
    ("minecraft:orange_concrete", 1),
    ("minecraft:yellow_concrete", 2),
    ("minecraft:lime_concrete", 3),
    ("minecraft:cyan_concrete", 4),
    ("minecraft:light_blue_concrete", 5),
    ("minecraft:blue_concrete", 6),
    ("minecraft:purple_concrete", 7),
    ("minecraft:red_concrete", 8),        # underside fascia
]


def bridge_y(x):
    t = (x - BR_X[0]) / float(BR_X[1] - BR_X[0])
    return BR_END_Y + (BR_CROWN_Y - BR_END_Y) * (math.sin(math.pi * t) ** 0.72)


def build_bridge(doc):
    sec = "30_bridge"
    for x in range(BR_X[0], BR_X[1] + 1):
        y = int(round(bridge_y(x)))
        for z in range(BR_Z[0], BR_Z[1] + 1):
            for blk, off in BANDS:
                doc.set(x, y - off, z, blk, sec)

    # east landing: a small gold-trimmed balcony reaching toward the reserved
    # dome footprint, so the bridge reads as terminating on the dome's terrace
    sec = "31_bridge_fittings"
    zl0, zl1 = BR_Z[0] - 1, BR_Z[1] + 1
    for x in range(-37, BR_X[1] + 1):
        for z in range(zl0, zl1 + 1):
            doc.set(x, BR_END_Y, z, "minecraft:smooth_stone", sec)
            doc.set(x, BR_END_Y - 1, z, "minecraft:stone_bricks", sec)
            if (x + z) % 3 == 0:
                doc.set(x, BR_END_Y - 2, z, "minecraft:stone_bricks", sec)
    # parapet: white with gold caps, open to the west (bridge)
    for z in range(zl0, zl1 + 1):
        doc.set(-32, BR_END_Y + 1, z, "minecraft:smooth_quartz", sec)
    for x in range(-37, -31):
        for z in (zl0, zl1):
            doc.set(x, BR_END_Y + 1, z, "minecraft:smooth_quartz", sec)
    for (x, z) in ((-32, zl0), (-32, zl1), (-37, zl0), (-37, zl1)):
        doc.set(x, BR_END_Y + 2, z, "minecraft:gold_block", sec)
    # gold keystone under the balcony
    for z in range(zl0 + 1, zl1):
        doc.set(-34, BR_END_Y - 3, z, "minecraft:gold_block", sec)
    # lamp posts at both ends of the deck
    for (x, z) in ((-73, BR_Z[0] - 1), (-73, BR_Z[1] + 1),
                   (-38, BR_Z[0] - 1), (-38, BR_Z[1] + 1)):
        yy = BR_END_Y
        for k in range(1, 4):
            doc.set(x, yy + k, z, "minecraft:white_concrete" if k < 3 else "minecraft:gold_block", sec)
        doc.set(x, yy + 4, z, "minecraft:lantern", sec)
    # snow crust along the deck edges (festive reference)
    for x in range(BR_X[0], BR_X[1] + 1):
        y = int(round(bridge_y(x)))
        for z in (BR_Z[0], BR_Z[1]):
            if hsh(x, 9, z) % 3:
                doc.set(x, y + 1, z, snow(1), sec)
    # red underside arch ribs
    for x in range(BR_X[0] + 4, BR_X[1] - 2, 6):
        y = int(round(bridge_y(x))) - 9
        for z in range(BR_Z[0] + 1, BR_Z[1]):
            doc.set(x, y, z, "minecraft:red_concrete", sec)


# ==========================================================================
# 5. Snow globe
# ==========================================================================
def globe_ground(dx, dz):
    d = math.hypot(dx, dz)
    if d >= GL_R - 1.0:
        return None
    return int(math.floor(GL_CY - math.sqrt(max(0.0, (GL_R - 1.2) ** 2 - d * d)) + 1.2))


def build_globe(doc):
    cx, cz = int(GL_C[0]), int(GL_C[1])

    # ---- base ----------------------------------------------------------
    sec = "40_globe_base"
    for z in range(cz - 18, cz + 19):
        for x in range(cx - 18, cx + 19):
            d = math.hypot(x + 0.5 - GL_C[0], z + 0.5 - GL_C[1])
            if d <= 16.6:
                doc.set(x, 121, z, "minecraft:light_gray_concrete", sec)
            if d <= 16.2:
                doc.set(x, 122, z, "minecraft:smooth_stone", sec)
            if d <= 15.6:
                doc.set(x, 123, z, "minecraft:stone_bricks", sec)
                doc.set(x, 124, z, "minecraft:light_gray_concrete", sec)
            if d <= 15.2:
                doc.set(x, 125, z, "minecraft:smooth_stone", sec)
            if 13.9 <= d <= 15.4:
                doc.set(x, 126, z, "minecraft:white_concrete", sec)
                doc.set(x, 127, z, "minecraft:smooth_quartz", sec)
            if d < 13.9:
                doc.set(x, 125, z, "minecraft:snow_block", sec)
                doc.set(x, 126, z, "minecraft:packed_ice", sec)

    # ---- glass sphere --------------------------------------------------
    sec = "41_globe_glass"
    for y in range(126, 156):
        for z in range(cz - 16, cz + 17):
            for x in range(cx - 16, cx + 17):
                dx, dz, dy = x + 0.5 - GL_C[0], z + 0.5 - GL_C[1], y + 0.5 - GL_CY
                d3 = math.sqrt(dx * dx + dz * dz + dy * dy)
                if not (GL_R - 0.95 <= d3 <= GL_R + 0.35):
                    continue
                if y <= GL_BASE_TOP:
                    continue
                blk = "minecraft:light_blue_stained_glass"
                if dy < -8 and hsh(x, y, z) % 5 == 0:
                    blk = "minecraft:blue_stained_glass"
                doc.set(x, y, z, blk, sec)

    # ---- diorama -------------------------------------------------------
    sec = "42_globe_diorama"

    def gset(x, y, z, blk):
        """Only place inside the glass (keeps trees and towers off the shell)."""
        if math.dist((x + 0.5, y + 0.5, z + 0.5), (GL_C[0], GL_CY, GL_C[1])) > GL_R - 1.0:
            return
        doc.set(x, y, z, blk, sec)

    for z in range(cz - 14, cz + 15):
        for x in range(cx - 14, cx + 15):
            gy = globe_ground(x + 0.5 - GL_C[0], z + 0.5 - GL_C[1])
            if gy is None:
                continue
            for y in range(gy - 1, gy + 1):
                if y > GL_BASE_TOP:
                    gset(x, y, z, "minecraft:snow_block" if hsh(x, y, z) % 5
                         else "minecraft:packed_ice")
            if hsh(x, 5, z) % 6 == 0:
                gset(x, gy + 1, z, snow(1))

    # ---- snow castle on the summit of the mound ------------------------
    bx, bz = cx - 3, cz - 3
    base_y = 128

    def box(x0, z0, w, y0, h, blk="minecraft:white_concrete"):
        for y in range(y0, y0 + h):
            for dx in range(w):
                for dz in range(w):
                    gset(x0 + dx, y, z0 + dz, blk)

    box(bx, bz, 7, base_y, 4)                       # 128..131  skirt
    box(bx + 1, bz + 1, 5, base_y + 4, 7, "minecraft:snow_block")   # 132..138
    box(bx + 2, bz + 2, 3, base_y + 11, 7, "minecraft:white_concrete")  # 139..145
    # battlements on the skirt
    for dx in range(7):
        for dz in range(7):
            if (dx in (0, 6) or dz in (0, 6)) and (dx + dz) % 2 == 0:
                gset(bx + dx, base_y + 4, bz + dz, "minecraft:white_concrete")
    # windows
    for (wx, wz, wy) in ((bx + 1, bz + 3, 134), (bx + 5, bz + 3, 135),
                         (bx + 3, bz + 1, 136), (bx + 3, bz + 2, 142),
                         (bx + 2, bz + 3, 141), (bx + 4, bz + 3, 141)):
        gset(wx, wy, wz, "minecraft:light_gray_concrete")
    # side wings with dark undersides
    for (sx, sy, sz, ln) in ((bx - 3, 133, bz + 2, 3), (bx + 7, 134, bz + 2, 3)):
        for i in range(ln):
            for dz in range(3):
                gset(sx + i, sy, sz + dz, "minecraft:white_concrete")
                gset(sx + i, sy - 1, sz + dz, "minecraft:gray_concrete")
    # gold crown on the tower top
    cxr, cyr = bx + 2, 146
    crown = ("X.X.X",
             "X.X.X",
             "XXXXX")
    for r, row in enumerate(crown):
        for c, ch in enumerate(row):
            if ch == "X":
                for dz in (0, 1, 2):
                    gset(cxr + c, cyr + (2 - r), bz + 1 + dz, "minecraft:gold_block")

    # ---- snowy conifers around the diorama -----------------------------
    trees = [(-9, -3, 13), (-6, 7, 11), (6, 8, 12), (10, 1, 9), (7, -6, 10),
             (-10, 4, 9), (2, -9, 11), (-4, -8, 8), (11, 6, 7)]
    for (tx, tz, th) in trees:
        px, pz = cx + tx, cz + tz
        gy = globe_ground(px + 0.5 - GL_C[0], pz + 0.5 - GL_C[1])
        if gy is None:
            continue
        for k in range(th):
            y = gy + 1 + k
            rad = 0.0 if k == th - 1 else (2.4 * (1 - k / float(th)) + 0.5)
            r = int(round(rad))
            for dz in range(-r, r + 1):
                for dx in range(-r, r + 1):
                    if dx * dx + dz * dz > rad * rad + 0.6:
                        continue
                    gset(px + dx, y, pz + dz,
                         "minecraft:snow_block" if (k % 3 != 1) else "minecraft:spruce_leaves")
        gset(px, gy + th, pz, "minecraft:snow_block")

    # ---- presents on the snow ------------------------------------------
    pres = [(-8, 4, "minecraft:red_concrete"), (6, -3, "minecraft:green_concrete"),
            (9, 5, "minecraft:lime_concrete"), (-6, -6, "minecraft:blue_concrete"),
            (4, 6, "minecraft:red_concrete")]
    for (px, pz, col) in pres:
        gx, gz = cx + px, cz + pz
        gy = globe_ground(gx + 0.5 - GL_C[0], gz + 0.5 - GL_C[1])
        if gy is None:
            continue
        for dz in range(2):
            for dx in range(2):
                gset(gx + dx, gy + 1, gz + dz, col)
        gset(gx, gy + 2, gz, "minecraft:white_concrete")
        gset(gx + 1, gy + 2, gz + 1, "minecraft:white_concrete")
        gset(gx + 1, gy + 2, gz, "minecraft:gold_block")


# ==========================================================================
# build all
# ==========================================================================
def build_all():
    doc = Doc()
    tops = build_mountain(doc)
    build_ledges(doc, tops)
    build_lava(doc, tops)
    build_snow(doc, tops)
    build_terrace(doc, tops)
    build_sign(doc)
    build_hof(doc)
    build_hof_sign(doc)
    build_bridge(doc)
    build_globe(doc)
    return doc


# ==========================================================================
# emit
# ==========================================================================
def rle_commands(cells):
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
        part = 1
        for start in range(0, len(cmds), CHUNK_CMDS):
            chunk = cmds[start:start + CHUNK_CMDS]
            name = f"{key}_{part:02d}" if len(cmds) > CHUNK_CMDS else key
            body = [f"# V4-B {dict(SECTIONS).get(key, key)} - part {part} ({len(chunk)} cmds)"]
            body.extend(chunk)
            text = "\n".join(body) + "\n"
            for d in (FN_DIR, DP_DIR):
                with open(os.path.join(d, name + ".mcfunction"), "w", encoding="utf-8") as fh:
                    fh.write(text)
            files.append((name, len(chunk), len(text)))
            part += 1

    load = ["# V4-B  THE CHAMPS + Hall of Fame + rainbow bridge + snow globe",
            "# run:  function mcchub:v4b/load"]
    for name, _, _ in files:
        load.append(f"function {NS}:v4b/{name}")
    for d in (FN_DIR, DP_DIR):
        with open(os.path.join(d, "load.mcfunction"), "w", encoding="utf-8") as fh:
            fh.write("\n".join(load) + "\n")

    with open(os.path.join(HERE, "datapack", "pack.mcmeta"), "w", encoding="utf-8") as fh:
        fh.write('{\n  "pack": {\n    "pack_format": 48,\n'
                 '    "description": "MCC hub replica V4-B - Champs / Hall of Fame / bridge / globe"\n'
                 '  }\n}\n')

    with open(os.path.join(HERE, "blocks.csv"), "w", encoding="utf-8", newline="") as fh:
        fh.write("x,y,z,block\n")
        for (x, y, z) in sorted(doc.b, key=lambda k: (k[1], k[2], k[0])):
            fh.write(f"{x},{y},{z},{doc.b[(x, y, z)]}\n")
    return files


# ==========================================================================
# preview dumps
# ==========================================================================
def context_block(x, z):
    """Preview-only stand-in for the island surface (not emitted)."""
    r = math.hypot(x + 0.5, z + 0.5)
    if r < 150:
        return "minecraft:snow_block" if r < 146 else "minecraft:dirt"
    return None


def write_dump(path, blocks, x0, z0, y0, x1, z1, y1):
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "wt", encoding="utf-8") as fh:
        fh.write(f"MCMDUMP 1 {x1 - x0 + 1} {y1 - y0 + 1} {z1 - z0 + 1}\n")
        for (x, y, z) in sorted(blocks, key=lambda k: (k[1], k[2], k[0])):
            if x0 <= x <= x1 and y0 <= y <= y1 and z0 <= z <= z1:
                fh.write(f"{x - x0} {y - y0} {z - z0} {blocks[(x, y, z)]}\n")


def write_preview(doc):
    x0, z0, y0, x1, z1, y1 = RG
    blocks = dict(doc.b)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b = context_block(x, z)
            if b:
                blocks.setdefault((x, 120, z), b)
    write_dump(os.path.join(HERE, "preview.mcmdump.gz"), blocks, *RG)


def write_detail(doc, name, bx0, bz0, bx1, bz1, by0=118, by1=190, hide=None):
    blocks = dict(doc.b)
    if hide:
        for k in list(blocks):
            if hide(k):
                del blocks[k]
    for x in range(bx0, bx1 + 1):
        for z in range(bz0, bz1 + 1):
            blocks.setdefault((x, 120, z), "minecraft:snow_block")
    write_dump(os.path.join(HERE, name), blocks, bx0, bz0, by0, bx1, bz1, by1)


# ==========================================================================
# checks
# ==========================================================================
def checks(doc):
    rep = {}
    x0, z0, y0, x1, z1, y1 = RG
    rep["blocks"] = doc.count()
    rep["below_y121"] = sum(1 for k in doc.b if k[1] < Y0)
    rep["outside_region"] = sum(1 for k in doc.b
                                if not (x0 <= k[0] <= x1 and z0 <= k[2] <= z1
                                        and y0 <= k[1] <= y1))

    # reserved dome footprint (plinth r 30 at the origin, y 120..128) must stay empty
    bad = [k for k in doc.b
           if math.hypot(k[0] + 0.5, k[2] + 0.5) < 30.0 and k[1] <= 128]
    rep["inside_dome_plinth"] = len(bad)

    # biosphere plinth must not touch the mountain apron
    clash = [k for k in doc.b
             if k[0] <= -74 and k[2] >= 22 and math.hypot(k[0] + 0.5 + 70, k[2] + 0.5 - 45) < 21.6]
    rep["mountain_in_hof_plinth"] = len(clash)

    # globe outside the plinth radius
    rep["globe_in_dome_plinth"] = sum(
        1 for k in doc.b if math.hypot(k[0] + 0.5, k[2] + 0.5) < 31.0
        and math.hypot(k[0] + 0.5 - GL_C[0], k[2] + 0.5 - GL_C[1]) < 17)

    xs = [k[0] for k in doc.b]
    ys = [k[1] for k in doc.b]
    zs = [k[2] for k in doc.b]
    rep["bounds"] = (min(xs), min(ys), min(zs), max(xs), max(ys), max(zs))
    per = {}
    for s in doc.sec.values():
        per[s] = per.get(s, 0) + 1
    rep["per_section"] = per
    ids = sorted({v.split("[")[0] for v in doc.b.values()})
    rep["block_ids"] = len(ids)
    rep["ids"] = ids
    return rep


def main():
    doc = build_all()
    files = emit(doc)
    write_preview(doc)
    write_detail(doc, "detail_mountain.mcmdump.gz", -125, -30, -60, 30, 118, 190)
    write_detail(doc, "detail_hof.mcmdump.gz", -95, 20, -40, 72, 118, 165)
    write_detail(doc, "detail_hof_cutaway.mcmdump.gz", -95, 20, -40, 72, 118, 165,
                 hide=lambda k: (k[1] > 131 and doc.b[k] in
                                 ("minecraft:glass", "minecraft:light_blue_stained_glass")))
    write_detail(doc, "detail_bridge.mcmdump.gz", -80, -6, -24, 16, 140, 186)
    write_detail(doc, "detail_globe.mcmdump.gz", 55, 35, 95, 76, 118, 160,
                 hide=lambda k: (k[1] > 132 and doc.b[k] == "minecraft:light_blue_stained_glass"))

    rep = checks(doc)
    print(f"blocks        : {rep['blocks']:,}")
    print(f"commands      : {sum(f[1] for f in files):,} in {len(files)} files")
    print(f"bounds        : {rep['bounds']}")
    for k in ("below_y121", "outside_region", "inside_dome_plinth",
              "mountain_in_hof_plinth", "globe_in_dome_plinth", "block_ids"):
        print(f"{k:22s}: {rep[k]}")
    print("per section:")
    for s in sorted(rep["per_section"]):
        print(f"   {s:24s} {rep['per_section'][s]:6d}")
    print("files:")
    for name, n, size in files:
        print(f"   {name:30s} {n:6d} cmds {size/1024:8.1f} KiB")
    return rep


if __name__ == "__main__":
    main()
