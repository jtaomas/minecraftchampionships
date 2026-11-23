"""Chest-menu overlay backgrounds (drawn into the container title via the font).

Geometry follows the vanilla generic container screen (176 px wide,
height = 114 + 18 * rows). Slot frames sit exactly where vanilla slots are so
items line up: container slot (r, c) frame at (7 + 18c, 17 + 18r), player
inventory frames at y = 30 + 18*rows (+18 per row), hotbar at y = 88 + 18*rows.
The 'Inventory' label (drawn by vanilla in dark grey at y = height - 94)
stays readable because the lower panel is light.
"""
from PIL import Image
from pixel import rgb, lerp, RAMPS, render
import icons

W = 176

THEMES = {
    'selector': dict(head=('#3c5be6', '#23255c'), accent='gold', icon='STAR',
                     tint='#3c5be6'),
    'admin':    dict(head=('#e6363f', '#5a1420'), accent='gold', icon='CROWN',
                     tint='#e6363f'),
    'dome':     dict(head=('#9346df', '#2a0f4d'), accent='gold', icon='CHECK',
                     tint='#9346df'),
    'kit':      dict(head=('#c75418', '#45140a'), accent='gold', icon='SWORD',
                     tint='#f5892a'),
    'team':     dict(head=('#1eb6c2', '#0b2e3c'), accent='gold', icon='TEAM_PURPLE',
                     tint='#1eb6c2'),
}

BODY_TOP, BODY_BOT = rgb('#d9dcea'), rgb('#bfc3d6')
RIM = rgb('#14152e')
SLOT_IN, SLOT_DK, SLOT_LT = rgb('#8b8fa6'), rgb('#3b3e57'), rgb('#f4f5fb')


def _rect(px, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = c


def _slot(px, x, y, style='normal', tint=None):
    """18x18 recessed slot frame."""
    if style == 'feature':
        dk, lt = RAMPS['gold'][1], RAMPS['gold'][4]
        inner = lerp(rgb(tint), rgb('#2a2c50'), 0.35)
        inner2 = lerp(rgb(tint), rgb('#2a2c50'), 0.6)
    elif style == 'dim':
        dk, lt = rgb('#4a4d62'), rgb('#d6d8e4')
        inner = inner2 = rgb('#a3a6b8')
    elif style == 'tint':
        dk, lt = SLOT_DK, SLOT_LT
        inner = lerp(SLOT_IN, rgb(tint), 0.35)
        inner2 = lerp(SLOT_IN, rgb(tint), 0.25)
    else:
        dk, lt = SLOT_DK, SLOT_LT
        inner, inner2 = SLOT_IN, lerp(SLOT_IN, rgb('#6f7390'), 0.6)
    for j in range(18):
        for i in range(18):
            X, Y = x + i, y + j
            if i == 0 or j == 0:
                c = dk
            elif i == 17 or j == 17:
                c = lt
            else:
                c = lerp(inner, inner2, (j - 1) / 15)
            px[X, Y] = c
    px[x + 17, y] = lerp(dk, lt, 0.5)
    px[x, y + 17] = lerp(dk, lt, 0.5)
    if style == 'feature':
        # gold corner ticks
        g = RAMPS['gold'][3]
        for (cx, cy) in ((1, 1), (16, 1), (1, 16), (16, 16)):
            px[x + cx, y + cy] = g


def make_gui(rows, theme_name, slot_styles=None):
    t = THEMES[theme_name]
    H = 114 + 18 * rows
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    px = img.load()
    head_hi, head_lo = rgb(t['head'][0]), rgb(t['head'][1])
    acc = RAMPS[t['accent']]
    # body
    for y in range(H):
        for x in range(W):
            px[x, y] = lerp(BODY_TOP, BODY_BOT, y / H)
    # header band (rows 0..15)
    for y in range(16):
        for x in range(W):
            c = lerp(head_hi, head_lo, y / 15)
            # subtle diagonal broadcast stripes
            if (x + y) % 8 in (0, 1) and y < 15:
                c = lerp(c, rgb('#ffffff'), 0.06)
            px[x, y] = c
    for x in range(W):
        px[x, 1] = lerp(px[x, 1], rgb('#ffffff'), 0.25)
        px[x, 15] = acc[2]
        px[x, 16] = acc[0]
    # gold chevrons on header right
    for k in range(3):
        bx = W - 33 + k * 5
        for d in range(4):
            for yy in (4 + d, 11 - d):
                px[bx + d, yy] = lerp(acc[3], head_lo, (2 - k) * 0.3)
    for x in range(1, W - 1):
        px[x, 17] = lerp(px[x, 17], rgb('#2a2c50'), 0.35)
    # header icon at right end
    ico_src = (icons.team_icons() if t['icon'].startswith('TEAM_') else icons.hud_icons())[t['icon']]
    ico = ico_src.crop(ico_src.getbbox())
    img.alpha_composite(ico, (W - 7 - ico.width, (16 - ico.height) // 2))

    # container slots
    slot_styles = slot_styles or {}
    for r in range(rows):
        for c in range(9):
            st = slot_styles.get(r * 9 + c, 'normal')
            _slot(px, 7 + 18 * c, 17 + 18 * r, st, t['tint'])
    # rule to the right of the vanilla 'Inventory' label (label spans x 8..~52)
    dy = H - 94 + 3
    for x in range(60, W - 8):
        f = min(1.0, (x - 60) / 24)
        px[x, dy] = lerp(px[x, dy], acc[2], 0.85 * f)
        px[x, dy + 1] = lerp(px[x, dy + 1], acc[0], 0.45 * f)
    # player inventory + hotbar
    for r in range(3):
        for c in range(9):
            _slot(px, 7 + 18 * c, 30 + 18 * rows + 18 * r)
    for c in range(9):
        _slot(px, 7 + 18 * c, 88 + 18 * rows)

    # outer rim with rounded corners + bevel
    for x in range(W):
        px[x, 0] = RIM
        px[x, H - 1] = RIM
    for y in range(H):
        px[0, y] = RIM
        px[W - 1, y] = RIM
    for y in range(17, H - 1):
        px[1, y] = lerp(px[1, y], rgb('#ffffff'), 0.6)
        px[W - 2, y] = lerp(px[W - 2, y], rgb('#5a5e78'), 0.5)
    for x in range(1, W - 1):
        px[x, H - 2] = lerp(px[x, H - 2], rgb('#5a5e78'), 0.5)
    for (cx, cy) in ((0, 0), (W - 1, 0), (0, H - 1), (W - 1, H - 1)):
        px[cx, cy] = (0, 0, 0, 0)
    for (cx, cy) in ((1, 1), (W - 2, 1), (1, H - 2), (W - 2, H - 2)):
        px[cx, cy] = RIM
    return img


def all_guis():
    kit = {i: ('feature' if i in (1, 3, 5, 7) else 'dim') for i in range(9)}
    admin = {4: 'feature'}
    admin.update({i: 'tint' for i in range(10, 18)})
    dome = {i: 'tint' for i in range(9, 18)}
    return {
        'GUI_SELECTOR': (make_gui(6, 'selector'), 6),
        'GUI_ADMIN': (make_gui(3, 'admin', admin), 3),
        'GUI_DOME': (make_gui(3, 'dome', dome), 3),
        'GUI_KIT': (make_gui(1, 'kit', kit), 1),
        'GUI_TEAM': (make_gui(3, 'team'), 3),
    }
