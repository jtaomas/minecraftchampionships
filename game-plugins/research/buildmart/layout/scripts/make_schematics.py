"""Generate the Build Mart layout schematics (research only).

Outputs (schematics/):
  current_topdown.png   mart ring + build area, plan view, north-up
  gallery_plan.png      7 normal + 1 gold blueprint plinths, plan view
  build_area_plan.png   3 plots + gold plot + utilities
  elevation.png         section through gallery -> lower pavilion -> upper island

Positions are estimates reconstructed from stills in ../frames/pack.
Each drawing states the confidence of the thing it draws.
"""
from PIL import Image, ImageDraw, ImageFont
import math, os

OUT = os.path.join(os.path.dirname(__file__), "..", "schematics")
os.makedirs(OUT, exist_ok=True)

FONT = r"C:\Windows\Fonts\arial.ttf"
FONTB = r"C:\Windows\Fonts\arialbd.ttf"
def f(sz, bold=False):
    try:
        return ImageFont.truetype(FONTB if bold else FONT, sz)
    except Exception:
        return ImageFont.load_default()

VOID = (18, 22, 38)
BLUE = (58, 74, 190)
BLUE_L = (86, 104, 216)
WHITE = (238, 240, 246)
GREY = (70, 76, 88)
GREY_L = (140, 146, 158)
GOLD = (232, 178, 34)
GREEN = (58, 154, 96)
RED = (198, 62, 54)


def rot_rect(cx, cy, w, h, deg):
    a = math.radians(deg)
    pts = []
    for dx, dy in ((-w/2, -h/2), (w/2, -h/2), (w/2, h/2), (-w/2, h/2)):
        pts.append((cx + dx*math.cos(a) - dy*math.sin(a),
                    cy + dx*math.sin(a) + dy*math.cos(a)))
    return pts


def label(d, xy, text, size=22, fill=WHITE, anchor="mm", bold=False):
    d.text(xy, text, font=f(size, bold), fill=fill, anchor=anchor)


def legend_box(d, x, y, w, rows, title="LEGEND"):
    h = 30 + 26*len(rows)
    d.rounded_rectangle([x, y, x+w, y+h], 8, fill=(26, 30, 44), outline=GREY)
    label(d, (x+14, y+16), title, 20, WHITE, "lm", True)
    yy = y + 44
    for colour, text in rows:
        d.rectangle([x+14, yy-7, x+30, yy+7], fill=colour, outline=GREY_L)
        label(d, (x+38, yy), text, 18, (208, 212, 222), "lm")
        yy += 26
    return y + h


def dashed_circle(d, cx, cy, r, colour, width=3, dash=14, gap=10):
    n = int(2*math.pi*r/(dash+gap))
    for i in range(n):
        a0 = 2*math.pi*i/n
        a1 = 2*math.pi*(i+0.55)/n
        d.arc([cx-r, cy-r, cx+r, cy+r], math.degrees(a0), math.degrees(a1),
              fill=colour, width=width)


# ----------------------------------------------------------------- figure 1
def current_topdown():
    W, H = 2300, 1600
    im = Image.new("RGB", (W, H), VOID)
    d = ImageDraw.Draw(im)

    label(d, (60, 44), "BUILD MART — CURRENT MAP (resource-island revision, MCC 20 → P25+)",
          33, WHITE, "lm", True)
    label(d, (60, 82), "Top-down topology. North = up. Ring radii and diameters are estimates from stills; "
                       "bearings of individual islands are L.", 18, (168, 176, 192), "lm")
    label(d, (60, 114), "Disc = Blueprint Gallery + fan.  Inner ring = 8 colour pavilions.  Outer ring = 8 upper category islands.",
          18, (206, 210, 222), "lm")

    cx, cy = 740, 790
    dashed_circle(d, cx, cy, 590, (196, 168, 60), 4)
    d.ellipse([cx-575, cy-575, cx+575, cy+575], outline=(150, 52, 44), width=3)
    label(d, (cx, cy-606), "dome / canopy frame  ⌀ ~150 blocks (M)", 19, (210, 190, 120), "mm")

    # upper islands (higher, further out)
    upper = [("LOGS", -90, (96, 132, 70)), ("SAND / SNOW", -45, (200, 186, 140)),
             ("BRICKS", 0, (150, 86, 70)), ("STONE", 45, (150, 152, 158)),
             ("NETHER", 90, (128, 56, 60)), ("COPPER", 135, (188, 116, 82)),
             ("ORES", 180, (120, 140, 190)), ("GARDEN", 225, (92, 160, 92))]
    for name, ang, col in upper:
        a = math.radians(ang)
        ix, iy = cx + 505*math.cos(a), cy + 505*math.sin(a)
        d.ellipse([ix-96, iy-68, ix+96, iy+68], fill=(30, 36, 46), outline=col, width=3)
        d.ellipse([ix-84, iy-56, ix+84, iy+56], fill=col)
        label(d, (ix, iy), name, 21, (20, 24, 30), "mm", True)

    # lower colour pavilions (walkable showrooms, close in)
    lower = [("RED · ORANGE", -90), ("YELLOW · LIME", -45), ("GREEN", 0), ("AQUA · CYAN", 45),
             ("BLUE", 90), ("PURPLE · PINK", 135), ("WHITE · GREY", 180), ("BLACK · BROWN", 225)]
    for name, ang in lower:
        a = math.radians(ang)
        px, py = cx + 305*math.cos(a), cy + 305*math.sin(a)
        bx, by = cx + 218*math.cos(a), cy + 218*math.sin(a)
        d.line([bx, by, px, py], fill=(214, 216, 224), width=7)
        pts = rot_rect(px, py, 120, 66, ang+90)
        d.polygon(pts, fill=(44, 46, 56), outline=(120, 124, 138))
        label(d, (px, py-4), name, 15, WHITE, "mm", True)
        label(d, (px, py+18), "pavilion + pad", 12, (168, 176, 190), "mm")

    # gallery disc
    d.ellipse([cx-215, cy-215, cx+215, cy+215], fill=BLUE, outline=(228, 230, 238), width=5)
    d.ellipse([cx-200, cy-200, cx+200, cy+200], outline=BLUE_L, width=3)
    label(d, (cx-152, cy+118), "MCC", 42, WHITE, "mm", True)
    for k in range(4):
        off = -120 + k*80
        d.line([cx-190, cy+off, cx+190, cy+off], fill=(226, 228, 236), width=4)
    d.line([cx, cy-190, cx, cy+190], fill=(226, 228, 236), width=4)

    # fan
    d.ellipse([cx-34, cy-34, cx+34, cy+34], fill=(92, 96, 106), outline=(210, 212, 220), width=3)
    for k in range(6):
        aa = math.radians(k*60)
        d.line([cx, cy, cx+26*math.cos(aa), cy+26*math.sin(aa)], fill=(200, 202, 210), width=3)
    label(d, (cx, cy+44), "GIANT FAN", 13, WHITE, "mm", True)
    label(d, (cx, cy+58), "vertical launch", 10, (210, 214, 226), "mm")

    # speed pads on disc edge
    for k in range(8):
        aa = math.radians(k*45+22)
        px, py = cx + 170*math.cos(aa), cy + 170*math.sin(aa)
        d.ellipse([px-7, py-7, px+7, py+7], fill=(220, 90, 50))

    # 7 plinths + gold
    plot_pos = [(-95, -80), (-30, -110), (60, -96), (100, -30), (86, 46), (10, 92), (-72, 34)]
    for i, (dx, dy) in enumerate(plot_pos):
        d.rectangle([cx+dx-17, cy+dy-17, cx+dx+17, cy+dy+17], fill=(48, 52, 62), outline=WHITE, width=2)
        label(d, (cx+dx, cy+dy), str(i+1), 16, WHITE, "mm", True)
    d.rectangle([cx+6, cy+120, cx+54, cy+168], fill=(70, 56, 26), outline=GOLD, width=4)
    label(d, (cx+30, cy+144), "GOLD", 16, GOLD, "mm", True)


    # build area panel
    bx, by, bw, bh = 1470, 420, 700, 510
    d.rounded_rectangle([bx, by, bx+bw, by+bh], 10, fill=(24, 27, 40), outline=(150, 120, 60), width=3)
    label(d, (bx+bw/2, by+28), "TEAM BUILD AREA  (portal-linked hall)", 22, GOLD, "mm", True)
    label(d, (bx+bw/2, by+54), "~ 62 × 44 blocks, ceiling +14 (M)", 16, (188, 192, 204), "mm")
    for i in range(3):
        px = bx + 62 + i*188
        py = by + 108
        d.rectangle([px, py, px+150, py+150], fill=(72, 72, 78), outline=(236, 238, 244), width=3)
        d.rectangle([px+24, py+24, px+126, py+126], fill=(96, 96, 100), outline=(150, 154, 166), width=2)
        label(d, (px+75, py+72), f"PLOT {i+1}", 17, WHITE, "mm", True)
        label(d, (px+75, py+162), "15×15 curb · 7×7 build", 12, (188, 192, 204), "mm")
    # gold dais
    d.rectangle([bx+62, by+300, bx+262, by+398], fill=(92, 70, 22), outline=GOLD, width=4)
    label(d, (bx+162, by+334), "GOLD PLOT", 17, GOLD, "mm", True)
    label(d, (bx+162, by+360), "raised +2 · no reference", 12, (226, 214, 180), "mm")
    # chests
    for i in range(5):
        d.rectangle([bx+300+i*36, by+300, bx+328+i*36, by+320], fill=(190, 96, 40), outline=(240, 220, 200))
    label(d, (bx+390, by+334), "5 colour-coded chests", 13, (222, 226, 236), "mm")
    # portal
    d.rectangle([bx+300, by+366, bx+372, by+404], fill=(28, 60, 120), outline=GREEN, width=4)
    label(d, (bx+336, by+385), "PORTAL", 13, (140, 230, 170), "mm", True)
    label(d, (bx+378, by+388), "→", 16, (140, 230, 170), "lm", True)
    # crafting / smelting
    d.rectangle([bx+480, by+300, bx+640, by+336], fill=(96, 78, 54), outline=GREY_L)
    label(d, (bx+560, by+318), "crafting ×2 · stonecutter ×2", 12, WHITE, "mm")
    d.rectangle([bx+480, by+360, bx+640, by+398], fill=(80, 74, 70), outline=RED)
    label(d, (bx+560, by+379), "2 smelting banks (fuel)", 12, WHITE, "mm")
    label(d, (bx+bw/2, by+448), "spawn / return pad — a fall respawns here minus inventory", 13, (180, 184, 196), "mm")
    d.line([bx+120, by+470, bx+560, by+470], fill=(120, 124, 138), width=3)

    # link
    d.line([1120, 940, 1460, 940], fill=(170, 136, 70), width=4)
    d.polygon([(1460, 940), (1444, 932), (1444, 948)], fill=(170, 136, 70))
    label(d, (1290, 918), "portal (instant, both ways)", 17, GOLD, "mm")

    rows = [
        (BLUE, "Blueprint Gallery disc — walkable, indoor floor"),
        ((44, 46, 56), "Lower colour pavilion × 8 — glass cases, bins, roof pad"),
        ((96, 132, 70), "Upper category island × 8 — block piles, +26…+40"),
        (GOLD, "Gold plot / gold blueprint"),
        ((220, 90, 50), "Speed-boost pad on deck"),
        ((196, 168, 60), "Dome canopy truss — open sky, no wall"),
    ]
    legend_box(d, 1450, 1000, 800, rows, "LEGEND / CONFIDENCE")
    label(d, (1450, 1224), "Void below every platform. There is no rail at the market edge: falling is the failure state.",
          17, (176, 182, 196), "lm")
    label(d, (1450, 1256), "Scale: disc ⌀ ~70, pavilion ring r ~76, upper ring r ~126, dome r ~148 (blocks). 1 px ≈ 4 blocks.",
          17, (176, 182, 196), "lm")
    label(d, (1450, 1288), "Pavilion and island bearings are illustrative — order is a navigation aid, not a measured fact (L).",
          17, (176, 182, 196), "lm")
    label(d, (1450, 1330), "16 islands: 8 lower colour pavilions + 8 upper category islands (M).", 19, WHITE, "lm", True)
    label(d, (1450, 1362), "Flow: build hall → portal → gallery (pick 1-3) → fan/elytra → island → portal → build.", 16, (196, 200, 212), "lm")
    label(d, (1450, 1388), "A fall respawns you and deletes everything in your inventory except tools.", 16, (196, 200, 212), "lm")
    label(d, (1450, 1414), "Gallery: 7 blueprints rotate every 90 s. Gold appears at 4:00, lives 120 s.", 16, (196, 200, 212), "lm")

    im.save(os.path.join(OUT, "current_topdown.png"))


# ----------------------------------------------------------------- figure 2
def gallery_plan():
    W, H = 1400, 1100
    im = Image.new("RGB", (W, H), VOID)
    d = ImageDraw.Draw(im)
    label(d, (50, 42), "BLUEPRINT GALLERY — PLINTH PLAN (top-down)", 32, WHITE, "lm", True)
    label(d, (50, 78), "7 selectable blueprints + 1 Gold. The seven rotate every 90 s; Gold appears at 4:00 and rotates every 120 s.",
          17, (168, 176, 192), "lm")

    cx, cy = 560, 600
    d.ellipse([cx-330, cy-330, cx+330, cy+330], fill=BLUE, outline=WHITE, width=5)
    for k in (-1, 0, 1):
        d.line([cx-300, cy+k*120, cx+300, cy+k*120], fill=(220, 224, 234), width=5)
        d.line([cx+k*120, cy-300, cx+k*120, cy+300], fill=(220, 224, 234), width=5)

    cells = {
        (-1, -1): ("1 STAR", 1), (0, -1): ("1 STAR", 1), (1, -1): ("2 STAR", 2),
        (-1, 0): ("2 STAR", 2), (0, 0): ("3 STAR", 3), (1, 0): ("4 STAR", 4),
        (-1, 1): ("5 STAR", 5), (1, 1): ("GOLD 7", 7),
    }
    for (gx, gy), (txt, stars) in cells.items():
        px, py = cx + gx*150, cy + gy*150
        gold = stars == 7
        d.rounded_rectangle([px-52, py-52, px+52, py+52], 8,
                            fill=(74, 58, 24) if gold else (46, 50, 60),
                            outline=GOLD if gold else WHITE, width=4 if gold else 3)
        d.rectangle([px-34, py-34, px+34, py+34], fill=(96, 74, 30) if gold else (62, 66, 78),
                    outline=GREY_L)
        label(d, (px, py-6), txt, 15 if gold else 18, GOLD if gold else WHITE, "mm", True)
        label(d, (px, py+22), "plinth", 13, (190, 194, 206), "mm")
    d.rectangle([cx-52, cy+98, cx+52, cy+202], fill=(30, 34, 48), outline=(140, 146, 160), width=2)
    label(d, (cx, cy+150), "arrival / aisle\n(from portal)", 14, (170, 176, 190), "mm")

    x0, y0 = 960, 300
    d.rounded_rectangle([x0, y0, x0+390, y0+300], 10, fill=(26, 30, 44), outline=GREY)
    label(d, (x0+195, y0+26), "ONE PLINTH (detail)", 20, WHITE, "mm", True)
    d.rectangle([x0+120, y0+70, x0+270, y0+220], fill=(58, 62, 74), outline=WHITE, width=3)
    d.rectangle([x0+150, y0+100, x0+240, y0+190], fill=(84, 88, 102), outline=GREY_L, width=2)
    label(d, (x0+195, y0+145), "build\n(≤ 9×9 footprint)", 15, WHITE, "mm")
    d.rectangle([x0+150, y0+232, x0+240, y0+258], fill=(150, 110, 40), outline=GOLD)
    label(d, (x0+195, y0+245), "selection plate", 13, GOLD, "mm")
    label(d, (x0+195, y0+274), "name · star rating · material list float above", 12, (188, 192, 204), "mm")
    label(d, (x0+195, y0+292), "interaction: right-click plate / walk-on (L)", 12, (188, 192, 204), "mm")

    rows = [
        (WHITE, "1-star / 2-star: cheap, 1–2 min, teach the loop"),
        (GOLD, "Gold: 7-star, no reference in the build area"),
        ((30, 34, 48), "aisle: ~3-block gap between plinths"),
        ((220, 90, 50), "speed pad on the disc edge"),
    ]
    legend_box(d, 960, 640, 390, rows, "READING THE PLAN")

    label(d, (960, 940), "9 pads are visible in the stills (frames 18/19).", 16, (200, 204, 216), "lm")
    label(d, (960, 966), "Only 7 are selectable + Gold; one pad reads as a", 16, (200, 204, 216), "lm")
    label(d, (960, 992), "spare / rotation buffer — verify in a live POV (M/L).", 16, (200, 204, 216), "lm")

    im.save(os.path.join(OUT, "gallery_plan.png"))


# ----------------------------------------------------------------- figure 3
def build_area_plan():
    W, H = 1500, 1100
    im = Image.new("RGB", (W, H), VOID)
    d = ImageDraw.Draw(im)
    label(d, (50, 42), "TEAM BUILD AREA — PLAN (one team of four)", 32, WHITE, "lm", True)
    label(d, (50, 78), "Indoor hall. Three regular plots in a row, raised Gold dais at the head, utilities on the long wall.",
          17, (168, 176, 192), "lm")

    x0, y0, w, h = 120, 150, 900, 700
    d.rectangle([x0, y0, x0+w, y0+h], fill=(46, 48, 56), outline=(200, 202, 210), width=4)
    label(d, (x0+w/2, y0-26), "hall ~62 × 44 × 14 (M/L)", 18, (200, 204, 216), "mm")

    for i in range(3):
        px = x0 + 70 + i*270
        py = y0 + 250
        d.rectangle([px, py, px+230, py+230], fill=(88, 88, 92), outline=(238, 240, 246), width=4)
        d.rectangle([px+50, py+50, px+180, py+180], fill=(104, 104, 108), outline=(160, 164, 176), width=3)
        for cxp, cyp in ((px+8, py+8), (px+222, py+8), (px+8, py+222), (px+222, py+222)):
            d.rectangle([cxp-10, cyp-10, cxp+10, cyp+10], fill=(28, 30, 36))
        label(d, (px+115, py+115), f"PLOT {i+1}", 24, WHITE, "mm", True)
        label(d, (px+115, py+246), "~15×15 curb · 7×7 build zone", 15, (200, 204, 216), "mm")
        label(d, (px+115, py+272), "floor hologram: name/stars/materials", 14, (176, 182, 196), "mm")

    gx, gy = x0+80, y0+60
    d.rectangle([gx, gy, gx+300, gy+150], fill=(96, 74, 26), outline=GOLD, width=5)
    d.rectangle([gx+60, gy+36, gx+240, gy+114], fill=(70, 70, 74), outline=(180, 150, 70), width=3)
    label(d, (gx+150, gy+54), "GOLD PLOT", 22, GOLD, "mm", True)
    label(d, (gx+150, gy+78), "raised +2 · hazard canopy", 14, (238, 224, 186), "mm")
    label(d, (gx+150, gy+100), "no reference — view it in the gallery", 14, (238, 224, 186), "mm")

    for i in range(5):
        d.rectangle([x0+420+i*46, y0+62, x0+462+i*46, y0+94], fill=(186, 96, 42),
                    outline=(244, 228, 206), width=2)
    label(d, (x0+530, y0+36), "5 colour-coded chests", 16, WHITE, "mm", True)

    d.rectangle([x0+700, y0+52, x0+870, y0+96], fill=(104, 84, 58), outline=GREY_L, width=2)
    label(d, (x0+785, y0+74), "crafting ×2 · stonecutters ×2", 13, WHITE, "mm")
    d.rectangle([x0+700, y0+124, x0+870, y0+176], fill=(86, 80, 74), outline=RED, width=3)
    label(d, (x0+785, y0+150), "SMELTING BANK A (8 furnaces)", 13, WHITE, "mm")
    d.rectangle([x0+40, y0+600, x0+250, y0+650], fill=(86, 80, 74), outline=RED, width=3)
    label(d, (x0+145, y0+625), "SMELTING BANK B", 13, WHITE, "mm")

    d.rectangle([x0+390, y0+640, x0+500, y0+690], fill=(26, 62, 120), outline=GREEN, width=4)
    label(d, (x0+445, y0+665), "PORTAL → MART", 14, (150, 235, 180), "mm", True)
    label(d, (x0+445, y0+712), "in front of the plots (wiki)", 13, (180, 186, 198), "mm")

    d.ellipse([x0+640, y0+600, x0+800, y0+700], outline=(150, 154, 166), width=3)
    label(d, (x0+720, y0+650), "return / respawn", 14, (208, 212, 224), "mm")

    rows = [
        ((238, 240, 246), "Plot curb: quartz-white + black corner blocks"),
        (GOLD, "Gold zone: yellow terracotta + hazard stripe"),
        (GREEN, "Speed pad / portal frame (emerald green)"),
        (RED, "Smelting: lava-fed furnace bank"),
    ]
    legend_box(d, 1080, 250, 360, rows, "MATERIAL KEY")
    label(d, (1080, 470), "Sightlines:", 18, WHITE, "lm", True)
    for i, t in enumerate(["• every plot visible from the hall centre",
                            "• gold dais reads across the whole hall",
                            "• portal is the only way in/out",
                            "• flight allowed inside the hall"]):
        label(d, (1080, 500+i*26), t, 15, (196, 200, 212), "lm")
    label(d, (1080, 660), "Chest colour order and furnace count are M/L.", 15, (176, 182, 196), "lm")

    im.save(os.path.join(OUT, "build_area_plan.png"))


# ----------------------------------------------------------------- figure 4
def elevation():
    W, H = 1800, 900
    im = Image.new("RGB", (W, H), VOID)
    d = ImageDraw.Draw(im)
    label(d, (50, 42), "SECTION — GALLERY → LOWER PAVILION → UPPER ISLAND", 30, WHITE, "lm", True)
    label(d, (50, 76), "Relative heights above the gallery floor (0). Estimates; the dome is open sky, not a wall.",
          17, (168, 176, 192), "lm")

    ground = 760
    d.line([80, ground, 1720, ground], fill=(90, 94, 106), width=3)
    label(d, (88, ground+24), "void / y=-∞", 15, (150, 154, 166), "lm")

    gx0, gx1 = 180, 470
    d.rectangle([gx0, ground-60, gx1, ground], fill=BLUE)
    label(d, (gx0, ground-30), "GALLERY DISC", 17, WHITE, "lm", True)
    label(d, (gx0, ground+70), "floor 0 · plinths +1…+4", 14, (190, 194, 206), "lm")
    for k in range(9):
        d.line([325, ground-40-k*26, 325, ground-58-k*26], fill=(214, 216, 224), width=4)
    d.polygon([(300, ground-40), (350, ground-40), (340, ground-10), (310, ground-10)],
              fill=(120, 124, 136))
    label(d, (360, ground-250), "fan column: vertical launch", 15, (220, 222, 230), "lm")

    px0, px1 = 700, 940
    d.rectangle([px0, ground-160, px1, ground-110], fill=(52, 54, 64), outline=WHITE, width=2)
    d.rectangle([px0+20, ground-206, px1-20, ground-160], fill=(66, 70, 82), outline=GREY_L)
    label(d, (px0, ground-236), "LOWER PAVILION  +2 deck", 15, WHITE, "lm", True)
    label(d, (px0, ground-120), "glass cases · bins · roof pad", 13, (190, 194, 206), "lm")
    d.rectangle([px0+150, ground-108, px0+186, ground-96], fill=(220, 90, 50))
    label(d, (px0+168, ground-84), "speed pad", 12, (232, 170, 140), "mm")
    d.line([500, ground-80, 680, ground-120], fill=(130, 134, 148), width=3)
    label(d, (590, ground-130), "bridge (no rail)", 13, (180, 184, 196), "mm")

    ux0, ux1 = 1080, 1430
    d.polygon([(ux0, ground-330), (ux1, ground-330), (ux1-40, ground-290), (ux0+40, ground-290)],
              fill=(92, 132, 78))
    d.polygon([(ux0+40, ground-290), (ux1-40, ground-290), (ux1-110, ground-380), (ux0+110, ground-380)],
              fill=(74, 60, 44))
    label(d, (ux0+10, ground-410), "UPPER ISLAND  +26…+40", 15, WHITE, "lm", True)
    label(d, (ux0+10, ground-300), "block piles on top, stalactite underside", 13, (200, 204, 214), "lm")

    pts = [(470, ground-90), (640, ground-260), (860, ground-330), (1050, ground-330)]
    d.line(pts, fill=(120, 200, 230), width=3, joint="curve")
    for p in pts:
        d.ellipse([p[0]-4, p[1]-4, p[0]+4, p[1]+4], fill=(160, 220, 240))
    label(d, (700, ground-300), "elytra line (fan → island)", 14, (150, 215, 235), "lm")

    d.arc([300, ground-780, 1500, ground-180], 200, 340, fill=(200, 172, 60), width=5)
    label(d, (1010, 232), "dome canopy: yellow trusses + tinted glass (no walls)", 16, (214, 190, 110), "mm")

    for y, t in [(ground, "0"), (ground-160, "+8"), (ground-330, "+16"), (ground-500, "+24"), (ground-660, "+32")]:
        d.line([100, y, 120, y], fill=(120, 124, 140), width=2)
        label(d, (90, y), t, 13, (170, 174, 186), "rm")
    label(d, (60, 420), "y (blocks)", 14, (170, 174, 186), "mm")

    im.save(os.path.join(OUT, "elevation.png"))


current_topdown()
gallery_plan()
build_area_plan()
elevation()
print("written to", os.path.abspath(OUT))
