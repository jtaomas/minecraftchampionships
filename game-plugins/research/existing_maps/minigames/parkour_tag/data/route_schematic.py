import csv
from PIL import Image, ImageDraw, ImageFont

rows = list(csv.DictReader(open('data/pt_blocks.csv', encoding='utf-8')))
g = {(int(r['x']), int(r['y']), int(r['z'])): r['block'].split(':')[1] for r in rows}

X0, X1, Z0, Z1 = 1048, 1072, 978, 1011
CELL = 34
PAD = 90
W = (X1 - X0 + 1) * CELL + PAD * 2
H = (Z1 - Z0 + 1) * CELL + PAD * 2
im = Image.new('RGB', (W, H), (250, 250, 248))
d = ImageDraw.Draw(im)
try:
    font = ImageFont.truetype('arial.ttf', 19)
    small = ImageFont.truetype('arial.ttf', 14)
    tiny = ImageFont.truetype('arial.ttf', 12)
except Exception:
    font = ImageFont.load_default(); small = font; tiny = font

def px(x, z):
    return (PAD + (x - X0) * CELL, PAD + (z - Z0) * CELL)

# base plan
for x in range(X0, X1 + 1):
    for z in range(Z0, Z1 + 1):
        a = px(x, z)
        b = (a[0] + CELL, a[1] + CELL)
        if 1050 <= x <= 1071 and 984 <= z <= 1005:
            d.rectangle([a, b], fill=(214, 214, 210), outline=(235, 235, 232))
        elif 1057 <= x <= 1065 and (979 <= z <= 984 or 1005 <= z <= 1010):
            d.rectangle([a, b], fill=(186, 214, 224), outline=(235, 235, 232))

# features
def fill_cells(cells, color):
    for x, z in cells:
        a = px(x, z); b = (a[0] + CELL, a[1] + CELL)
        d.rectangle([a, b], fill=color)

# ice rink
fill_cells([(x, z) for x in range(1051, 1061) for z in (985, 986)] +
           [(x, z) for x in range(1051, 1053) for z in range(987, 992)], (150, 205, 245))
# slime pad
fill_cells([(x, z) for x in range(1059, 1063) for z in range(1002, 1006)], (120, 200, 120))
# central mast footprint
fill_cells([(x, z) for x in range(1057, 1064) for z in range(986, 997)], (160, 130, 120))
# corner rooms
fill_cells([(x, z) for x in range(1051, 1057) for z in range(999, 1005)], (190, 160, 140))  # SW
fill_cells([(x, z) for x in range(1064, 1071) for z in range(999, 1005)], (190, 160, 140))  # SE
fill_cells([(x, z) for x in range(1063, 1066) for z in range(986, 990)], (200, 150, 120))  # NE reactor
fill_cells([(x, z) for x in range(1051, 1057) for z in range(985, 992)], (200, 185, 170))  # NW

# arena wall
d.rectangle([px(1050, 984), (px(1071, 1005)[0] + CELL, px(1071, 1005)[1] + CELL)], outline=(60, 60, 60), width=3)

# doors
for (x0, x1, z) in [(1059, 1062, 984), (1059, 1062, 1005)]:
    a = px(x0, z); b = (px(x1, z)[0] + CELL, a[1] + CELL)
    d.rectangle([a, b], fill=(80, 170, 210), outline=(20, 80, 110), width=2)

# labels
def label(x, z, text, color=(30, 30, 30), f=None, dy=-8):
    a = px(x, z)
    d.text((a[0] + 2, a[1] + dy), text, fill=color, font=f or tiny)

label(1057, 985, 'ICE', (20, 60, 120), small)
label(1059, 1002, 'SLIME', (20, 80, 20), small)
label(1057, 991, 'CENTRAL', (255, 255, 255), small)
label(1058, 992, 'MAST', (255, 255, 255), small)
label(1051, 1000, 'SW', (40, 40, 40), small)
label(1064, 1000, 'SE', (40, 40, 40), small)
label(1063, 986, 'NE', (40, 40, 40), tiny)
label(1051, 986, 'NW', (40, 40, 40), tiny)
label(1059, 984, 'RUNNER', (255, 255, 255), tiny, dy=2)
label(1059, 1005, 'HUNTER', (255, 255, 255), tiny, dy=2)

# route polylines
def draw_route(points, color, width=4, arrows=True):
    pts = [px(x, z) for x, z in points]
    pts = [(p[0] + CELL // 2, p[1] + CELL // 2) for p in pts]
    d.line(pts, fill=color, width=width, joint='curve')
    if arrows:
        for i in range(len(pts) - 1):
            x1, y1 = pts[i]; x2, y2 = pts[i + 1]
            if (i % 2) == 0:
                # small arrowhead midway
                mx, my = (x1 + x2) / 2, (y1 + y2) / 2
                import math
                ang = math.atan2(y2 - y1, x2 - x1)
                for da in (2.6, -2.6):
                    d.line([(mx, my), (mx + 12 * math.cos(ang + da), my + 12 * math.sin(ang + da))], fill=color, width=3)

runner = [(1061, 984), (1060, 987), (1057, 988), (1054, 988), (1052, 990), (1052, 993), (1054, 995), (1057, 994), (1060, 992), (1063, 993), (1065, 995), (1068, 997), (1069, 1001), (1067, 1003), (1064, 1003), (1061, 1001), (1059, 999), (1057, 996), (1056, 993), (1058, 990), (1060, 987), (1061, 984)]
hunter = [(1060, 1005), (1061, 1003), (1063, 1001), (1065, 999), (1067, 997), (1069, 995), (1069, 992), (1067, 990), (1064, 988), (1062, 987), (1061, 985), (1061, 984)]
draw_route(runner, (30, 90, 200), 4)
draw_route(hunter, (200, 40, 40), 4)

# start markers
for (x, z, txt, col) in [(1061, 982, 'R', (30, 90, 200)), (1060, 1007, 'H', (200, 40, 40))]:
    a = px(x, z)
    d.ellipse([a[0] + 4, a[1] + 4, a[0] + CELL - 4, a[1] + CELL - 4], outline=col, width=3)
    d.text((a[0] + 11, a[1] + 7), txt, fill=col, font=font)

# title/legend
d.text((PAD, 18), 'MG-PT-01 Parkour Tag - plan + loop schematic (interpretive, M)', fill=(20, 20, 20), font=font)
d.line([PAD, 48, PAD + 30, 48], fill=(30, 90, 200), width=5)
d.text((PAD + 38, 39), 'runner loop', fill=(30, 90, 200), font=small)
d.line([PAD + 150, 48, PAD + 180, 48], fill=(200, 40, 40), width=5)
d.text((PAD + 188, 39), 'hunter intercept', fill=(200, 40, 40), font=small)
d.text((PAD + 340, 39), 'doors = cyan; rooms = tan; mast = brown; ice = blue; slime = green', fill=(80, 80, 80), font=tiny)

# axes
for x in range(X0, X1 + 1, 2):
    a = px(x, Z0)
    d.text((a[0], PAD - 24), str(x), fill=(60, 60, 60), font=tiny)
for z in range(Z0, Z1 + 1, 2):
    a = px(X0, z)
    d.text((PAD - 38, a[1]), str(z), fill=(60, 60, 60), font=tiny)
d.text((PAD - 62, PAD + 30), 'N', fill=(0, 0, 0), font=font)
im.save('renders/pt_route_schematic.png')
print('wrote renders/pt_route_schematic.png', im.size)
