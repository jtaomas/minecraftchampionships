"""The Dustbowl: block-scale floorplan traced from the MCC 21 references.

Arena-local coordinates: ax 0..39 west->east, az 0..47 north->south (north = commentary booth / lounge end,
west = PARKOUR TAG billboard side, east = MCC logo side). h = standing height above the street (blocks).
Sources per feature are given in `src`. DustbowlMap.java builds exactly these rectangles.

Run: python dustbowl_plan.py out.png [ref1.jpg ref2.jpg]   (draws the plan, optionally beside the references)
"""
import sys
from PIL import Image, ImageDraw, ImageFont

W, D = 40, 48
# (name, ax0, az0, ax1, az1, h, colour, src)
FEATURES = [
    ("rim ledge (loop)",        0, 0, 39, 47, 3, (24, 120, 120), "022640 022648 teal stepped wall ring"),
    ("N cage + ramp",          18, 0, 22, 3, 3, (60, 90, 200), "022440 blue ramp out of cage, 022304"),
    ("S cage + ramp",          18, 44, 22, 47, 3, (60, 90, 200), "022440 (other end, mirrored)"),
    ("NW plank stair tower",    1, 1, 5, 7, 4, (170, 120, 70), "022412 top-left wood + white"),
    ("chevron scaffold",        7, 1, 14, 6, 5, (230, 200, 40), "022412/022640/022648 yellow chevrons"),
    ("orange kiosk",           25, 1, 31, 7, 7, (230, 110, 30), "022412 022640 d010 orange kiosk"),
    ("NE purple lounge",       33, 1, 39, 10, 5, (120, 60, 160), "022456 022828 lounge, 022640 NE"),
    ("face building",          14, 8, 24, 12, 6, (70, 70, 80), "dustbowl_wiki_render centre-right, d010"),
    ("louver building",         1, 11, 5, 32, 4, (235, 235, 235), "022412 left white/black stripes"),
    ("west toy blocks",         7, 13, 12, 23, 6, (150, 70, 200), "022412 left-centre purple/teal"),
    ("chain pillars",           9, 25, 13, 29, 6, (190, 190, 180), "d010 d026 light brick pillars + chains"),
    ("central gate",           15, 15, 24, 28, 8, (35, 35, 40), "wiki render centre, d018 gate close-up"),
    ("east platform",          31, 13, 39, 33, 4, (55, 55, 65), "022412 right dark platform, purple stripe"),
    ("ice tunnel",             34, 15, 36, 31, 0, (150, 200, 255), "u21_0339 ice corridor"),
    ("dice planters",           6, 31, 14, 40, 3, (245, 245, 245), "022412 lower-left, d026 white dot boxes"),
    ("toy-block stair",        16, 31, 29, 41, 7, (110, 80, 220), "022412 lower centre purple/pink/blue"),
    ("ice rink",               32, 36, 38, 43, 0, (120, 180, 255), "022700 022704 ice between black buildings"),
    ("SE dark block",          32, 44, 39, 47, 3, (45, 45, 50), "022412 bottom-right"),
    ("red carpet",              1, 41, 12, 43, 0, (200, 30, 30), "022412 lower-left red band"),
    ("teal step ramp",          2, 44, 13, 47, 3, (40, 200, 210), "022412 bottom teal steps"),
]
CRATES = [(6, 9), (15, 7), (23, 13), (13, 24), (27, 22), (28, 14), (8, 29), (26, 30), (30, 9), (14, 42), (30, 44)]

def draw(out, refs):
    S = 18
    pad = 30
    img = Image.new("RGB", (W * S + 2 * pad + 330, D * S + 2 * pad), (30, 22, 18))
    g = ImageDraw.Draw(img)
    # street
    g.rectangle([pad, pad, pad + W * S - 1, pad + D * S - 1], fill=(176, 96, 58))
    for (name, x0, z0, x1, z1, h, col, src) in FEATURES:
        if name.startswith("rim"):
            for x in range(W):
                for z in range(D):
                    if x in (0, W - 1) or z in (0, D - 1):
                        g.rectangle([pad + x * S, pad + z * S, pad + x * S + S - 1, pad + z * S + S - 1], fill=col)
            continue
        g.rectangle([pad + x0 * S, pad + z0 * S, pad + (x1 + 1) * S - 1, pad + (z1 + 1) * S - 1], fill=col, outline=(0, 0, 0))
        g.text((pad + x0 * S + 2, pad + z0 * S + 2), "%s h%d" % (name, h), fill=(255, 255, 255) if sum(col) < 450 else (0, 0, 0))
    for (x, z) in CRATES:
        g.rectangle([pad + x * S, pad + z * S, pad + x * S + S - 1, pad + z * S + S - 1], fill=(184, 140, 80), outline=(90, 60, 30))
    for x in range(W + 1):
        g.line([pad + x * S, pad, pad + x * S, pad + D * S], fill=(0, 0, 0) if x % 8 == 0 else (120, 70, 40))
    for z in range(D + 1):
        g.line([pad, pad + z * S, pad + W * S, pad + z * S], fill=(0, 0, 0) if z % 8 == 0 else (120, 70, 40))
    # routes: loops (white), chokepoints (red)
    def P(x, z): return (pad + x * S + S // 2, pad + z * S + S // 2)
    loops = [
        [(14, 14), (14, 29), (25, 29), (25, 14), (14, 14)],                # around the gate
        [(30, 12), (30, 34), (35, 34), (35, 14), (30, 12)],                # east platform + ice tunnel
        [(6, 9), (6, 33), (15, 33), (15, 9), (6, 9)],                      # west street + toy blocks
    ]
    for L in loops:
        g.line([P(*p) for p in L], fill=(255, 255, 255), width=3)
    for (x, z, t) in [(19, 13, "choke: face/gate"), (14, 21, "choke: W arch"), (25, 21, "choke: E arch"), (20, 3, "N exit"), (20, 44, "S exit")]:
        g.ellipse([P(x, z)[0] - 7, P(x, z)[1] - 7, P(x, z)[0] + 7, P(x, z)[1] + 7], outline=(255, 0, 0), width=3)
        g.text((P(x, z)[0] + 9, P(x, z)[1] - 6), t, fill=(255, 220, 220))
    lx = pad + W * S + 20
    g.text((lx, pad), "THE DUSTBOWL - traced plan (block scale)", fill=(255, 255, 0))
    g.text((lx, pad + 16), "40 x 48 pit, north = booth, west = billboard", fill=(255, 255, 255))
    y = pad + 40
    for (name, x0, z0, x1, z1, h, col, src) in FEATURES:
        g.rectangle([lx, y, lx + 12, y + 12], fill=col)
        g.text((lx + 18, y), "%s (h%d)" % (name, h), fill=(255, 255, 255))
        g.text((lx + 18, y + 12), src[:48], fill=(170, 170, 170))
        y += 28
    g.rectangle([lx, y, lx + 12, y + 12], fill=(184, 140, 80)); g.text((lx + 18, y), "oak crates = safe ledges", fill=(255, 255, 255)); y += 20
    g.text((lx, y), "white lines = runner loops", fill=(255, 255, 255)); y += 14
    g.text((lx, y), "red rings = chokepoints / cage exits", fill=(255, 150, 150))
    if refs:
        rh = img.height
        ims = [Image.open(r).convert("RGB") for r in refs]
        ims = [im.resize((int(im.width * rh / 2 / im.height), rh // 2)) for im in ims]
        rw = max(im.width for im in ims)
        out_img = Image.new("RGB", (img.width + rw, rh), (0, 0, 0))
        out_img.paste(img, (0, 0))
        for i, im in enumerate(ims[:2]):
            out_img.paste(im, (img.width, i * rh // 2))
        img = out_img
    img.save(out)

if __name__ == "__main__":
    draw(sys.argv[1], sys.argv[2:])
