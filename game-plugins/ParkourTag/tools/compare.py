"""Side-by-side compare sheet: reference frames (left) beside our renders (right), one row per pair.
Usage: python compare.py out.png title ref1 render1 [ref2 render2 ...]"""
import sys
from PIL import Image, ImageDraw
out, title, files = sys.argv[1], sys.argv[2], sys.argv[3:]
H = 540
rows = []
for i in range(0, len(files), 2):
    ims = []
    for f in files[i:i + 2]:
        im = Image.open(f).convert("RGB")
        ims.append(im.resize((int(im.width * H / im.height), H)))
    rows.append((ims, files[i:i + 2]))
Wd = max(sum(im.width for im in ims) + 10 for ims, _ in rows)
S = Image.new("RGB", (Wd, len(rows) * (H + 24) + 30), (20, 20, 20))
d = ImageDraw.Draw(S)
d.text((8, 8), title, fill=(255, 255, 0))
y = 30
for ims, names in rows:
    x = 0
    for im, n in zip(ims, names):
        S.paste(im, (x, y + 20)); d.text((x + 6, y + 4), n.replace("\\", "/").split("/")[-1], fill=(255, 255, 255)); x += im.width + 10
    y += H + 24
S.save(out)
