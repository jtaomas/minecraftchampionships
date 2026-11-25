#!/usr/bin/env python3
"""Labelled top-down route map and z-y profile for TGTTOS Industry."""
import csv
import os

from PIL import Image, ImageDraw, ImageFont

DATA = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\tgttos_industry\data\blocks_xyz.csv"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\tgttos_industry\renders"

X0, X1, Z0, Z1 = 8, 138, 0, 102
S = 7
ML, MT = 40, 56
W = (X1 - X0 + 1) * S
H = (Z1 - Z0 + 1) * S


def font(sz, bold=False):
    for name in ("consolab.ttf" if bold else "consola.ttf", "arialbd.ttf" if bold else "arial.ttf"):
        try:
            return ImageFont.truetype(name, sz)
        except Exception:
            pass
    return ImageFont.load_default()


def px(x):
    return ML + (x - X0) * S


def pz(z):
    return MT + (z - Z0) * S


# load columns: top block in y -42..-18
cols = {}
with open(DATA, encoding="utf-8") as f:
    for d in csv.DictReader(f):
        y = int(d["y"])
        if y < -42 or y > -18:
            continue
        k = (int(d["x"]), int(d["z"]))
        if k not in cols or y > cols[k][0]:
            cols[k] = (y, d["block"])


def colour(y, block):
    n = block.split("[")[0]
    if "red_concrete" in n:
        return (178, 34, 34)
    if "yellow_concrete" in n:
        return (219, 168, 20)
    if "white_concrete" in n:
        return (228, 230, 232)
    if "black_concrete" in n:
        return (30, 32, 34)
    if "light_blue_stained_glass" in n:
        return (150, 205, 236)
    if "blackstone" in n:
        return (34, 31, 34)
    if "gray_wool" in n:
        return (95, 98, 101)
    if "black_wool" in n:
        return (22, 22, 24)
    if "iron_trapdoor" in n:
        return (205, 207, 208)
    if "red_sandstone" in n:
        return (190, 100, 55)
    if "bricks" in n:
        return (150, 97, 83)
    if "magenta_terracotta" in n:
        return (150, 88, 109)
    if "quartz" in n:
        return (235, 229, 222)
    if "smooth_stone" in n:
        return (158, 158, 158)
    if "polished_andesite" in n:
        return (120, 120, 120)
    if "sea_lantern" in n:
        return (210, 240, 235)
    return (140, 140, 140)


im = Image.new("RGB", (W + ML + 250, H + MT + 46), (238, 243, 248))
d = ImageDraw.Draw(im, "RGBA")

for (x, z), (y, b) in cols.items():
    if x < X0 or x > X1 or z < Z0 or z > Z1:
        continue
    c = colour(y, b)
    d.rectangle([px(x), pz(z), px(x) + S - 1, pz(z) + S - 1], fill=c)

# overlay the three glass fins and the ten curtain segments so material reads at a glance
for fx in (55, 73, 91):
    d.rectangle([px(fx) - 1, pz(26), px(fx) + S, pz(50) + S - 1], fill=(80, 175, 235, 235))
curtains = [
    (66, (200, 35, 35), [(29, 53), (61, 85), (93, 117)]),
    (74, (225, 175, 25), [(12, 36), (44, 69), (76, 101), (109, 134)]),
    (83, (200, 35, 35), [(29, 53), (61, 85), (93, 117)]),
]
for cz, col, segs in curtains:
    for a, b in segs:
        d.rectangle([px(a), pz(cz) - 2, px(b) + S, pz(cz) + 3], fill=col + (245,))
    # mark walkable x-gaps inside the deck width x26..120
    covered = [0] * 200
    for a, b in segs:
        for x in range(a, b + 1):
            covered[x] = 1
    run = None
    for x in range(26, 121):
        if not covered[x] and run is None:
            run = x
        if (covered[x] or x == 120) and run is not None:
            end = x - 1 if covered[x] else x
            if end - run >= 3:
                cx = (run + end) // 2
                d.ellipse([px(cx) - 3, pz(cz) - 3, px(cx) + 3, pz(cz) + 3], fill=(40, 180, 90))
            run = None

# axis grid
f13 = font(13)
for gx in range(10, 140, 10):
    d.line([(px(gx), MT), (px(gx), MT + H)], fill=(255, 255, 255, 45))
    d.text((px(gx) + 2, MT + H + 4), str(gx), fill=(40, 50, 65), font=f13)
for gz in range(0, 101, 10):
    d.line([(ML, pz(gz)), (ML + W, pz(gz))], fill=(255, 255, 255, 45))
    d.text((4, pz(gz) - 7), str(gz), fill=(40, 50, 65), font=f13)
d.text((ML + W // 2, MT + H + 26), "x (block)", fill=(25, 35, 50), font=font(15))
d.text((4, 4), "z (block, down)", fill=(25, 35, 50), font=font(15))
d.text((ML, 22), "TGTTOS Industry - lower course plan (y=-42..-18), labelled route", fill=(15, 25, 40), font=font(19, True))

# route line: suggested weave
route = [(73, 11), (73, 24), (60, 25), (60, 51), (60, 62), (57, 63), (57, 66),
         (72, 67), (72, 74), (57, 75), (57, 83), (57, 88), (73, 89), (73, 93)]
pts = [(px(x) + S // 2, pz(z) + S // 2) for x, z in route]
d.line(pts, fill=(255, 235, 80, 235), width=5, joint="curve")
for i, (x, z) in enumerate(route):
    pass
# arrow heads
for a, b in zip(pts[:-1], pts[1:]):
    d.line([a, b], fill=(255, 235, 80, 235), width=5)

# callouts
def box(x, z, text, tx, tz, col=(20, 30, 45)):
    ax, az = px(x) + S // 2, pz(z) + S // 2
    bx, bz = px(tx), pz(tz)
    d.line([(ax, az), (bx, bz)], fill=col, width=2)
    d.ellipse([ax - 4, az - 4, ax + 4, az + 4], fill=col)
    tw = d.textlength(text, font=font(14, True))
    d.rectangle([bx - 4, bz - 3, bx + tw + 6, bz + 18], fill=(255, 255, 255, 225), outline=col)
    d.text((bx, bz), text, fill=col, font=font(14, True))


box(73, 11, "START (73,-33,11)", 92, 5)
box(60, 32, "GAP A: 3 glass fins x=55/73/91", 72, 30)
box(60, 58, "DECK 2 (z52-62)", 92, 58)
box(57, 66, "curtain z66 (red)", 20, 64)
box(72, 74, "curtain z74 (yellow)", 20, 74)
box(57, 83, "curtain z83 (red)", 20, 84)
box(73, 93, "FINISH PEN x58-88, 24 chickens", 88, 94)
box(40, 16, "machine head x40", 14, 20)
box(105, 16, "machine head x104", 92, 20)
box(73, 20, "conveyor deck 1 (z8-24)", 86, 16)

# legend on the right
lx = W + ML + 16
ly = MT
d.text((lx, ly), "LEGEND", fill=(15, 25, 40), font=font(16, True))
items = [
    ((255, 235, 80), "suggested route"),
    ((178, 34, 34), "red paint curtain"),
    ((219, 168, 20), "yellow paint curtain"),
    ((150, 205, 236), "glass fin / skylight"),
    ((34, 31, 34), "conveyor deck"),
    ((190, 100, 55), "red sandstone trim"),
    ((228, 230, 232), "white concrete"),
]
for i, (c, t) in enumerate(items):
    yy = ly + 28 + i * 26
    d.rectangle([lx, yy, lx + 18, yy + 14], fill=c, outline=(40, 40, 40))
    d.text((lx + 26, yy - 1), t, fill=(30, 40, 55), font=font(13))
d.text((lx, ly + 240), "Route is free-form:", fill=(30, 40, 55), font=font(13, True))
for i, t in enumerate(["bridge the two voids with the", "auto-refilled offhand concrete;", "the drawn line is one fast weave."]):
    d.text((lx, ly + 260 + i * 18), t, fill=(30, 40, 55), font=font(12))
d.text((lx, ly + 330), "No checkpoints.", fill=(160, 30, 30), font=font(13, True))
d.text((lx, ly + 350), "Void respawn: y <= -58", fill=(160, 30, 30), font=font(12))
im.save(os.path.join(OUT, "route_top_labelled.png"))
print("wrote route_top_labelled.png", im.size)

# ---------------------------------------------------------------- profile
PW, PH = 1400, 760
pm = 80
im2 = Image.new("RGB", (PW, PH), (244, 247, 250))
d2 = ImageDraw.Draw(im2, "RGBA")
# coordinate maps: z 0..102 -> x ; y -63..11 -> y
def qx(z):
    return pm + int(z / 102 * (PW - pm - 260))

def qy(y):
    return PH - 70 - int((y + 63) / 74 * (PH - 140))


def bar(z0, z1, y0, y1, fill, outline=None):
    a, b = qx(z0), qx(z1)
    c, d = qy(y0), qy(y1)
    d2.rectangle([min(a, b), min(c, d), max(a, b), max(c, d)], fill=fill, outline=outline)

# sky
d2.text((pm, 18), "TGTTOS Industry - z/y profile at x=73 (centre lane)", fill=(15, 25, 40), font=font(19, True))
# decks
for z0, z1, label in [(8, 24, "DECK 1"), (52, 62, "DECK 2")]:
    bar(z0, z1, -42, -35, (45, 42, 46))
    bar(z0, z1, -35, -34, (20, 20, 22))
    d2.text((qx((z0 + z1) / 2) - 26, qy(-33) - 4), label, fill=(20, 30, 45), font=font(14, True))
# foundations
for z0, z1 in [(8, 24), (52, 62), (88, 98)]:
    bar(z0, z1, -61, -42, (150, 150, 150))
# fins at z26..50
bar(26, 50, -61, -23, (150, 205, 236, 210))
# curtains
for z, c in [(66, (178, 34, 34)), (74, (219, 168, 20)), (83, (178, 34, 34))]:
    bar(z, z + 1, -61, -23, c)
# catwalk lines present at x=73: z28-48, z65-67 and z82-84 (z73-75 has an x-gap 68-78)
bar(28, 48, -20, -19, (225, 227, 228))
for z0, z1 in [(65, 67), (82, 84)]:
    bar(z0, z1, -20, -19, (225, 227, 228))
# start room
bar(9, 13, -34, -29, (235, 229, 222), (20, 20, 20))
d2.text((qx(9) - 40, qy(-27)), "start box", fill=(20, 30, 45), font=font(13, True))
# finish pen at x=73: deck absent, closed basin floor y=-40, acacia end walls at z88/z98
bar(88, 98, -61, -41, (150, 150, 150))
bar(88, 98, -41, -40, (120, 120, 120))
bar(88, 88, -40, -36, (190, 130, 70))
bar(98, 98, -40, -36, (190, 130, 70))
lx2, ly2 = qx(86), qy(-47)
tw2 = d2.textlength("FINISH PEN x58-88, floor -40, drop in over lip", font=font(11))
d2.rectangle([lx2 - 3, ly2 - 2, lx2 + tw2 + 4, ly2 + 15], fill=(255, 255, 255, 235), outline=(160, 30, 30))
d2.text((lx2, ly2), "FINISH PEN x58-88, floor -40, drop in over lip", fill=(20, 30, 45), font=font(11))
d2.text((qx(89), qy(-34) - 2), "DECK 3 only at x26-57 / x89-120", fill=(90, 60, 30), font=font(11))
# roof
bar(0, 102, 8, 9, (30, 30, 32))
bar(0, 102, 9, 10, (150, 205, 236))
d2.text((qx(1), qy(11) - 16), "blackstone roof frame + glass skylight (y=8..10)", fill=(20, 30, 45), font=font(12))
# void labels
d2.text((qx(34), qy(-50)), "GAP A  27 deep", fill=(150, 30, 30), font=font(16, True))
d2.text((qx(70), qy(-50)), "GAP B  25 deep", fill=(150, 30, 30), font=font(16, True))
# heights
d2.text((pm - 66, qy(-35) - 8), "y -35", fill=(20, 30, 45), font=font(12))
d2.text((pm - 66, qy(-58) - 8), "y -58", fill=(160, 30, 30), font=font(12))
d2.line([(pm - 10, qy(-58)), (PW - 200, qy(-58))], fill=(160, 30, 30, 160), width=2)
d2.text((PW - 196, qy(-58) - 8), "respawn threshold", fill=(160, 30, 30), font=font(12))
# z axis
for z in range(0, 101, 10):
    d2.line([(qx(z), qy(-62)), (qx(z), qy(-61))], fill=(20, 20, 20))
    d2.text((qx(z) - 8, qy(-63) + 6), str(z), fill=(30, 40, 55), font=font(12))
d2.text((PW // 2, PH - 26), "z (block)", fill=(25, 35, 50), font=font(15))
# y axis labels
for y in (-60, -50, -40, -30, -20, -10, 0, 10):
    d2.text((pm - 34, qy(y) - 7), str(y), fill=(30, 40, 55), font=font(12))
im2.save(os.path.join(OUT, "profile_x73.png"))
print("wrote profile_x73.png", im2.size)
