#!/usr/bin/env python3
"""
kite_views.py - Kite Festival extra previews (the course is folded, so the side view is drawn unfolded along s).

  python kite_views.py kite_festival.mcmdump.gz kite_profile.csv out_prefix
writes  <out_prefix>_side.png   unfolded height profile: canyon floor / crest, every opening, both simulated lines
        <out_prefix>_start.png  perspective view from the middle of the start gantry (camera file next to the dump)
"""
import csv
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import views  # noqa: E402  (shared B-map perspective renderer, read-only)

COL = {"SAFE": (90, 200, 60), "FAST": (245, 140, 30), "NEEDLE": (220, 40, 40), "FINISH": (180, 30, 60)}


def font(sz):
    for f in ("arial.ttf", "DejaVuSans.ttf"):
        try:
            return ImageFont.truetype(f, sz)
        except OSError:
            pass
    return ImageFont.load_default()


def side(csvf, out):
    plan, holes, lines, pieces = [], [], {"safe": [], "fast": []}, []
    with open(csvf, encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            k = row["kind"]
            if k == "plan":
                plan.append((float(row["s"]), int(row["a"]), int(row["b"]), float(row["c"]), float(row["label"])))
            elif k == "hole":
                holes.append((float(row["s"]), int(row["a"]), int(row["b"]), row["c"], row["label"]))
            elif k in lines:
                lines[k].append((float(row["s"]), float(row["a"]), float(row["c"])))
            elif k == "piece":
                pieces.append((float(row["s"]), row["label"]))
    S = max(p[0] for p in plan)
    sx, sy, ml, mt, mb = 1.0, 3.0, 60, 70, 60
    Y0, Y1 = -60, 185
    W, H = int(S * sx) + ml + 30, int((Y1 - Y0) * sy) + mt + mb
    im = Image.new("RGB", (W, H), (214, 232, 248))
    d = ImageDraw.Draw(im)
    X = lambda s: ml + s * sx
    Yp = lambda y: mt + (Y1 - y) * sy
    f12, f14, f18 = font(12), font(14), font(18)
    # height grid
    for y in range(-60, 181, 20):
        d.line([(ml, Yp(y)), (W - 30, Yp(y))], fill=(196, 214, 232))
        d.text((8, Yp(y) - 7), f"y{y}", fill=(90, 100, 120), font=f12)
    # canyon air between floor and crest, rock below the floor
    top = [(X(s), Yp(c + 1)) for s, f, c, _, _ in plan]
    bot = [(X(s), Yp(f + 1)) for s, f, c, _, _ in plan]
    d.polygon(top + bot[::-1], fill=(236, 244, 252))
    d.polygon(bot + [(X(S), Yp(Y0)), (X(0), Yp(Y0))], fill=(120, 150, 95))
    d.line(top, fill=(120, 110, 100), width=2)
    # pieces (legs / hairpins)
    for s, nm in pieces:
        d.line([(X(s), mt), (X(s), H - mb)], fill=(150, 160, 175), width=1)
        d.text((X(s) + 3, H - mb + 6), nm, fill=(60, 70, 90), font=f12)
    # screens: wall from floor to crest (grey), openings coloured
    seen = {}
    for s, y0, y1, k, nm in holes:
        p = plan[min(len(plan) - 1, int(round(s)))]
        if nm not in seen:
            d.rectangle([X(s) - 1, Yp(p[2] + 1), X(s) + 1, Yp(p[1] + 1)], fill=(110, 110, 120))
            seen[nm] = s
        d.rectangle([X(s) - 3, Yp(y1 + 1), X(s) + 3, Yp(y0)], fill=COL.get(k, (0, 0, 0)))
    for i, (nm, s) in enumerate(seen.items()):
        d.text((X(s) - 14, mt - 16 - 14 * (i % 2)), nm, fill=(40, 40, 60), font=f12)
    # simulated flights
    for nm, c in (("safe", (30, 120, 30)), ("fast", (210, 90, 0))):
        pts = [(X(s), Yp(y + 0.3)) for s, y, _ in lines[nm]]
        if len(pts) > 1:
            d.line(pts, fill=c, width=2)
    tl = {k: (v[-1][2] if v else 0) for k, v in lines.items()}
    d.text((ml, 8), "Kite Festival - unfolded course profile (x = distance along the centre line, 1 px per block; y x3)", fill=(20, 20, 40), font=f18)
    d.text((ml, 30), f"green: safe line flight (model, rockets in the hairpins) {tl['safe']:.0f} s   orange: fast line flight (expert) {tl['fast']:.0f} s   "
                     "bars: lime = safe opening, orange = fast, red = needle / finish; grey = screen wall", fill=(40, 40, 60), font=f14)
    im.save(out)


def main():
    src, csvf, prefix = sys.argv[1], sys.argv[2], sys.argv[3]
    side(csvf, prefix + "_side.png")
    camf = src.replace(".mcmdump.gz", ".cam")
    if os.path.exists(camf):
        grid, lut = views.load(src)
        cam = [float(v) for v in open(camf).read().split()]
        views.perspective(grid, lut, cam, prefix + "_start.png", far=320.0)
    print("kite views written:", prefix)


if __name__ == "__main__":
    main()
