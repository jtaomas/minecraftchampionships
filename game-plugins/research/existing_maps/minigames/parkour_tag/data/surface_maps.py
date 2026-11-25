import csv
from PIL import Image, ImageDraw, ImageFont

rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
X0, X1, Z0, Z1 = 1048, 1071, 978, 1010
block = {(int(r['x']), int(r['y']), int(r['z'])): r['block'].split(':')[1] for r in rows}

def solid(x, y, z):
    return (x, y, z) in block

def stand(x, y, z):
    return (not solid(x, y, z)) and (not solid(x, y + 1, z)) and solid(x, y - 1, z)

hi = {}
lo = {}
for x in range(1050, 1072):
    for z in range(984, 1006):
        ys = [y for y in range(11, 27) if stand(x, y, z)]
        if ys:
            hi[(x, z)] = max(ys)
            lo[(x, z)] = min(ys)

# palette by height
colors = {
    11: (240, 210, 210), 12: (230, 185, 185), 13: (220, 160, 160), 14: (210, 140, 140),
    15: (200, 120, 120), 16: (190, 100, 100), 17: (180, 85, 85), 18: (170, 70, 70),
    19: (160, 60, 60), 20: (150, 50, 50), 21: (140, 40, 40), 22: (130, 30, 30),
    23: (120, 20, 20), 24: (110, 10, 10), 25: (100, 0, 0), 26: (80, 0, 0),
}

def draw(mapping, name, title):
    cell = 32
    pad = 70
    w = (X1 - X0 + 1) * cell + pad * 2
    h = (Z1 - Z0 + 1) * cell + pad * 2 + 30
    im = Image.new('RGB', (w, h), (255, 255, 255))
    d = ImageDraw.Draw(im)
    try:
        font = ImageFont.truetype('arial.ttf', 18)
        small = ImageFont.truetype('arial.ttf', 13)
    except Exception:
        font = ImageFont.load_default(); small = font
    d.text((pad, 12), title, fill=(0, 0, 0), font=font)
    for z in range(Z0, Z1 + 1):
        for x in range(X0, X1 + 1):
            px = pad + (x - X0) * cell
            py = pad + (z - Z0) * cell
            if 1050 <= x <= 1071 and 984 <= z <= 1005:
                if (x, z) in mapping:
                    v = mapping[(x, z)]
                    c = colors.get(v, (255, 0, 255))
                    d.rectangle([px, py, px + cell - 1, py + cell - 1], fill=c)
                    if cell >= 28:
                        d.text((px + 8, py + 5), str(v), fill=(255, 255, 255), font=small)
                else:
                    d.rectangle([px, py, px + cell - 1, py + cell - 1], fill=(235, 235, 235))
            else:
                d.rectangle([px, py, px + cell - 1, py + cell - 1], fill=(250, 250, 250))
    # grid
    for i, x in enumerate(range(X0, X1 + 2)):
        px = pad + i * cell
        d.line([px, pad, px, pad + (Z1 - Z0 + 1) * cell], fill=(200, 200, 200))
        if x % 2 == 0:
            d.text((px - 12, pad - 22), str(x), fill=(0, 0, 0), font=small)
    for i, z in enumerate(range(Z0, Z1 + 2)):
        py = pad + i * cell
        d.line([pad, py, pad + (X1 - X0 + 1) * cell, py], fill=(200, 200, 200))
        if z % 2 == 0:
            d.text((pad - 40, py - 7), str(z), fill=(0, 0, 0), font=small)
    # labels
    d.text((pad - 55, pad + (995 - Z0) * cell), 'N', fill=(0, 0, 0), font=font)
    # spawn markers
    for (x, z, txt) in [(1061, 982, 'R'), (1060, 1007, 'H')]:
        px = pad + (x - X0) * cell
        py = pad + (z - Z0) * cell
        d.ellipse([px, py, px + cell, py + cell], outline=(0, 80, 255), width=3)
        d.text((px + 10, py + 8), txt, fill=(0, 80, 255), font=font)
    d.rectangle([pad, pad + (Z1 - Z0 + 1) * cell + 10, pad + 20, pad + (Z1 - Z0 + 1) * cell + 30], fill=(0, 0, 0))
    d.text((pad + 26, pad + (Z1 - Z0 + 1) * cell + 12), 'colour = standable surface y; R=runner tower, H=hunter tower (outside arena box)', fill=(0, 0, 0), font=small)
    im.save(name)
    print('wrote', name, im.size)

draw(lo, 'renders/pt_surface_low.png', 'MG-PT-01 arena - lowest standable surface per column')
draw(hi, 'renders/pt_surface_high.png', 'MG-PT-01 arena - highest standable surface per column')
