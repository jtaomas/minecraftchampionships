#!/usr/bin/env python3
"""Draw the leg-by-leg route on the top-down overview and title the iso renders."""
from PIL import Image, ImageDraw, ImageFont

D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\renders"
OX, OZ, SCALE, M = -258, -316, 2, 60

def px(x): return M + (x - OX) * SCALE
def py(z): return M + (z - OZ) * SCALE

im = Image.open(D + r"\overview_top.png").convert("RGB")
d = ImageDraw.Draw(im, "RGBA")
try:
    font = ImageFont.truetype("arialbd.ttf", 15)
    small = ImageFont.truetype("arialbd.ttf", 12)
    tiny = ImageFont.truetype("arial.ttf", 11)
except Exception:
    font = small = tiny = ImageFont.load_default()

legs = [
 ("L0", [(5,24),(5,20)]),
 ("L1", [(5,20),(5,4),(5,-10)]),
 ("L2", [(5,-10),(5,-40)]),
 ("L3", [(5,-40),(5,-60),(0,-80),(9,-100),(8,-125)]),
 ("L4", [(8,-125),(12,-145),(26,-155),(13,-165),(12,-180),(22,-190),(30,-198),(35,-212)]),
 ("L5", [(35,-212),(29,-234),(14,-252),(0,-256),(-20,-257),(-38,-252)]),
 ("L6", [(-38,-252),(-60,-256),(-90,-256),(-150,-256),(-215,-256)]),
 ("L7", [(-215,-256),(-230,-270),(-200,-278),(-165,-280),(-158,-255),(-158,-220),(-160,-195),(-155,-180)]),
 ("L8", [(-158,-165),(-160,-145),(-175,-110),(-190,-80),(-198,-50),(-185,-20),(-170,10),(-162,40)]),
 ("L9", [(-160,60),(-152,80),(-150,100),(-150,130),(-150,161),(-148,185),(-150,210)]),
 ("L10", [(-150,225),(-145,245),(-125,258),(-100,260),(-70,258),(-45,258),(-25,262)]),
 ("L11", [(-25,262),(-5,252),(15,242),(32,236),(46,228),(48,212),(48,200)]),
 ("L12", [(48,200),(45,180),(40,160),(30,145),(22,125),(15,105),(12,85),(10,65)]),
 ("L13", [(10,55),(8,40),(5,26),(5,20)]),
]
pts = []
for name, path in legs:
    pts.extend(path)
# draw a dark halo then the bright line
for w, col in ((7, (0, 0, 0, 200)), (4, (0, 240, 255, 255))):
    for name, path in legs:
        for a, b in zip(path, path[1:]):
            d.line([(px(a[0]), py(a[1])), (px(b[0]), py(b[1]))], fill=col, width=w)
# leg number badges at the midpoint of the first segment
for name, path in legs:
    a = path[0]; b = path[min(1, len(path) - 1)]
    mx, mz = (a[0] + b[0]) / 2, (a[1] + b[1]) / 2
    X, Y = px(mx), py(mz)
    r = 11
    d.ellipse([X - r, Y - r, X + r, Y + r], fill=(10, 20, 30, 235), outline=(0, 240, 255, 255), width=2)
    t = name
    bb = d.textbbox((0, 0), t, font=tiny)
    d.text((X - (bb[2] - bb[0]) / 2, Y - (bb[3] - bb[1]) / 2 - 1), t, fill=(255, 255, 255, 255), font=tiny)

# landmarks
landmarks = [
 ("START / FINISH", 5, 30, (0, 255, 180)),
 ("PLANET", -55, -248, (255, 220, 80)),
 ("WATER TUNNEL", -150, -268, (120, 200, 255)),
 ("RUIN", -232, -300, (230, 230, 230)),
 ("CRATER VILLAGE", -150, -170, (255, 160, 120)),
 ("RING FLIGHT", -195, -60, (200, 150, 255)),
 ("RAINBOW", -152, 155, (255, 120, 200)),
 ("CASTLE", 35, 215, (255, 200, 120)),
 ("CORAL", 12, 90, (120, 255, 220)),
]
for label, x, z, col in landmarks:
    X, Y = px(x), py(z)
    d.text((X + 6, Y - 6), label, fill=(0, 0, 0, 255), font=small)
    d.text((X + 5, Y - 7), label, fill=col + (255,), font=small)

# title bar
head = Image.new("RGB", (im.width, 44), (12, 18, 26))
im.paste(head, (0, 0))
d.text((12, 10), "Clouds 2 - Electric Boogaloo : route legs L0-L13 (top-down, 2 px/block, grid 50 blocks)", fill=(240, 245, 255, 255), font=font)
im.save(D + r"\route_labeled.png")
print("wrote route_labeled.png", im.size)

def title(src, dst, text, sub):
    im = Image.open(src).convert("RGB")
    bar = Image.new("RGB", (im.width, 62), (12, 18, 26))
    out = Image.new("RGB", (im.width, im.height + 62), (12, 18, 26))
    out.paste(im, (0, 62)); out.paste(bar, (0, 0))
    dd = ImageDraw.Draw(out)
    dd.text((12, 8), text, fill=(240, 245, 255), font=font)
    dd.text((12, 30), sub, fill=(150, 200, 230), font=tiny)
    out.save(dst)
    print("wrote", dst, out.size)

title(D + r"\overview_iso_a.png", D + r"\overview_iso_a_labeled.png",
      "Clouds 2 : isometric overview A (angle 0, scale 2)",
      "Planet + glass tunnel upper right; crater village centre; rainbow + terraces left; ring flight centre-left; castle lower centre.")
title(D + r"\overview_iso_b.png", D + r"\overview_iso_b_labeled.png",
      "Clouds 2 : isometric overview B (angle 2, scale 2)",
      "Reverse angle: water-tunnel ruin lower left, planet + waterfall left, terraces + rainbow right, castle far right.")
