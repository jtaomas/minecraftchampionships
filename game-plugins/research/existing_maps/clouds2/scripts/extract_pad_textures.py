#!/usr/bin/env python3
"""Extract animated glazed-terracotta textures and build a contact sheet."""
import io
import zipfile
from PIL import Image, ImageDraw, ImageFont

Z = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Clouds_2_-_Electric_Boogaloo__Download_Version_\Clouds 2 - Electric Boogaloo\resources.zip"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\renders\pack_pads.png"
names = ["yellow_glazed_terracotta", "red_glazed_terracotta", "magenta_glazed_terracotta",
         "pink_glazed_terracotta", "purple_glazed_terracotta", "sea_lantern"]
z = zipfile.ZipFile(Z)
cells = []
for n in names:
    try:
        im = Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/block/{n}.png"))).convert("RGBA")
    except KeyError:
        continue
    w, h = im.size
    frames = h // w
    cells.append((n, im, frames))
    print(n, im.size, "frames", frames)

scale = 6
cell_w = max(c[1].width for c in cells) * scale
rows = []
for n, im, frames in cells:
    f0 = im.crop((0, 0, im.width, im.width))
    f1 = im.crop((0, im.width, im.width, im.width * 2)) if frames > 1 else None
    rows.append((n, f0, f1, frames))
W = 2 * cell_w + 30 + 220
H = sum(max(1, im.width) * scale for _, im, _, _ in rows) + 20 * len(rows) + 40
sheet = Image.new("RGB", (W, H), (28, 30, 38))
d = ImageDraw.Draw(sheet)
try:
    font = ImageFont.truetype("arial.ttf", 16)
    small = ImageFont.truetype("arial.ttf", 13)
except Exception:
    font = small = ImageFont.load_default()
y = 20
for n, f0, f1, frames in rows:
    s = cell_w
    a = f0.resize((s, s), Image.NEAREST)
    sheet.paste(a, (20, y), a)
    if f1 is not None:
        b = f1.resize((s, s), Image.NEAREST)
        sheet.paste(b, (30 + s, y), b)
    d.text((40 + 2 * s, y + 4), n, fill=(240, 240, 240), font=font)
    d.text((40 + 2 * s, y + 26), f"{frames} frame(s), {f0.width}x{f0.height} px", fill=(170, 170, 180), font=small)
    y += s + 20
sheet.save(OUT)
print("wrote", OUT, sheet.size)
