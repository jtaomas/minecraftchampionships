"""Trace sheet for the table-driven maps (Urban Jungle, Factory): reads the PLAN text block straight from the map's
Java source so the sheet and the build cannot drift apart.
Run: python plan_sheet.py urbanjungle|factory out.png [ref1.jpg ref2.jpg]"""
import re, sys, os
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "src", "main", "java", "com", "mcm", "games", "parkourtag", "map")
MAPS = {
    "urbanjungle": dict(java="UrbanJungleMap.java", W=28, D=36, title="URBAN JUNGLE - traced plan (block scale)",
                        sub="28 x 36 pit; N + S = cage / purple-pink window rows", cages=[(14, 1, "N cage exit"), (14, 34, "S cage exit"), (13, 17, "spire (landmark)")]),
    "factory": dict(java="FactoryMap.java", W=34, D=32, title="FACTORY - traced plan (block scale)",
                    sub="34 x 32 yard from the official MCC 14 top-down; cages ASSUMED at N + S bays", cages=[(17, 1, "N bay cage (assumed)"), (17, 30, "S bay cage (assumed)")]),
}
COL = {"house": (170, 30, 30), "orange": (240, 120, 20), "grey": (140, 140, 135), "dark": (60, 60, 66), "slate": (35, 30, 40),
       "stripe": (230, 230, 230), "white": (240, 240, 240), "step": (184, 140, 80), "cyan": (30, 160, 175), "green": (40, 110, 40),
       "wood": (150, 60, 50), "yellow": (235, 200, 40), "red": (200, 30, 30), "pad": (210, 40, 40), "rampS": (225, 210, 160),
       "leaf": (80, 170, 60), "floor": (20, 20, 20), "hazard": (240, 190, 0)}

def features(java):
    txt = open(os.path.join(SRC, java), encoding="utf-8").read()
    block = re.search(r'PLAN = """(.*?)"""', txt, re.S).group(1)
    out = []
    for line in block.strip().splitlines():
        head, _, src = line.partition("|")
        p = head.split()
        if len(p) >= 7: out.append((p[0], p[1], *map(int, p[2:7]), src.strip()))
    return out

def draw(key, out, refs):
    cfg = MAPS[key]; W, D = cfg["W"], cfg["D"]
    S, pad = 20, 30
    F = features(cfg["java"])
    img = Image.new("RGB", (W * S + 2 * pad + 330, D * S + 2 * pad), (30, 24, 22))
    g = ImageDraw.Draw(img)
    g.rectangle([pad, pad, pad + W * S - 1, pad + D * S - 1], fill=(150, 150, 145))
    for (k, n, x0, z0, x1, z1, h, src) in F:
        col = COL.get(k, (255, 0, 255))
        g.rectangle([pad + x0 * S, pad + z0 * S, pad + (x1 + 1) * S - 1, pad + (z1 + 1) * S - 1], fill=col, outline=(0, 0, 0))
        if x1 - x0 >= 2 or z1 - z0 >= 2:
            g.text((pad + x0 * S + 2, pad + z0 * S + 2), "%s h%d" % (n, h), fill=(255, 255, 255) if sum(col) < 450 else (0, 0, 0))
    for x in range(W + 1): g.line([pad + x * S, pad, pad + x * S, pad + D * S], fill=(0, 0, 0) if x % 8 == 0 else (120, 120, 115))
    for z in range(D + 1): g.line([pad, pad + z * S, pad + W * S, pad + z * S], fill=(0, 0, 0) if z % 8 == 0 else (120, 120, 115))
    def P(x, z): return (pad + x * S + S // 2, pad + z * S + S // 2)
    for (x, z, t) in cfg["cages"]:
        c = (255, 0, 0) if "cage" in t else (0, 255, 255)
        g.ellipse([P(x, z)[0] - 7, P(x, z)[1] - 7, P(x, z)[0] + 7, P(x, z)[1] + 7], outline=c, width=3)
        g.text((P(x, z)[0] + 9, P(x, z)[1] - 6), t, fill=(255, 230, 230))
    lx = pad + W * S + 20
    g.text((lx, pad), cfg["title"], fill=(255, 255, 0)); g.text((lx, pad + 16), cfg["sub"][:60], fill=(255, 255, 255))
    y = pad + 40
    for (k, n, x0, z0, x1, z1, h, src) in F:
        if y > D * S + pad - 24: g.text((lx, y), "... (%d more rows in the Java PLAN table)" % (len(F) - F.index((k, n, x0, z0, x1, z1, h, src))), fill=(200, 200, 200)); break
        g.rectangle([lx, y, lx + 10, y + 10], fill=COL.get(k, (255, 0, 255)))
        g.text((lx + 16, y - 2), "%s h%d" % (n, h), fill=(255, 255, 255)); g.text((lx + 16, y + 8), src[:52], fill=(160, 160, 160)); y += 20
    g.text((lx, D * S + pad - 12), "red rings = cage exits. Heights h are blocks above the street.", fill=(255, 255, 255))
    if refs:
        rh = img.height
        ims = [Image.open(r).convert("RGB") for r in refs]
        ims = [im.resize((int(im.width * rh / 2 / im.height), rh // 2)) for im in ims]
        o = Image.new("RGB", (img.width + max(i.width for i in ims), rh), (0, 0, 0)); o.paste(img, (0, 0))
        for i, im in enumerate(ims[:2]): o.paste(im, (img.width, i * rh // 2))
        img = o
    img.save(out)

if __name__ == "__main__":
    draw(sys.argv[1], sys.argv[2], sys.argv[3:])
