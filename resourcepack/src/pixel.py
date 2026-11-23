"""Block Showdown pack - tiny pixel-art engine.

Sprites are authored as ASCII masks. Each letter is a material; the engine
auto-shades it (light on top/left edges, dark on bottom/right edges) and adds a
1px coloured outline around everything, so every icon in the pack shares one
consistent lighting model and palette.

Mask syntax
  .  or space   transparent
  A-Z           material, auto-shaded
  a-z           same material, forced dark tone
  +             specular highlight of the neighbouring material
  legend chars  per-sprite override: char -> (material, tone)
Tones: ol (outline), dk, base, lt, hi
"""
from PIL import Image
import numpy as np


def rgb(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def _ramp(*hexes):
    return [rgb(h) for h in hexes]


# outline, dark, base, light, highlight  -- one shared palette for the whole pack
RAMPS = {
    'gold':   _ramp('#5a330a', '#c27612', '#f2b31f', '#ffd84a', '#fff4b8'),
    'silver': _ramp('#2a303c', '#7f889c', '#b4bdcf', '#dfe5ef', '#ffffff'),
    'bronze': _ramp('#45200e', '#94501f', '#c47638', '#e6a065', '#f8d2a6'),
    'red':    _ramp('#470b13', '#b01e2c', '#e6363f', '#ff6a6a', '#ffb7b2'),
    'orange': _ramp('#4a1e06', '#c75418', '#f5892a', '#ffb156', '#ffdcab'),
    'yellow': _ramp('#4a3806', '#c79f16', '#f4d43a', '#fff07c', '#fffcd6'),
    'lime':   _ramp('#1d3d06', '#4d9e1b', '#7fd63a', '#b3f26b', '#e6ffc8'),
    'green':  _ramp('#0b2e13', '#1d7731', '#2ea548', '#5bc96a', '#bdf1c2'),
    'cyan':   _ramp('#05343c', '#118894', '#1eb6c2', '#5bd7df', '#c8f7f9'),
    'aqua':   _ramp('#09334e', '#2a8dcf', '#4dbff5', '#8bd9ff', '#d9f4ff'),
    'blue':   _ramp('#0e184a', '#283dae', '#3c5be6', '#7a92ff', '#ccd5ff'),
    'purple': _ramp('#280e4a', '#682aae', '#9346df', '#ba83f5', '#e8d3ff'),
    'pink':   _ramp('#4a0e31', '#c03b88', '#ef6ab3', '#ff9fd0', '#ffdaed'),
    'bone':   _ramp('#35313d', '#b3adbb', '#e6e2ea', '#faf9ff', '#ffffff'),
    'stone':  _ramp('#20232b', '#585e6e', '#7f8696', '#a6adbb', '#d2d7e0'),
    'dgray':  _ramp('#101116', '#2b2e37', '#3f434e', '#5a5f6c', '#828899'),
    'wood':   _ramp('#2c180b', '#6a3e1e', '#8f5a2e', '#b47d48', '#dbac78'),
    'ink':    _ramp('#0b0b10', '#15151c', '#22222c', '#363644', '#55556a'),
    'sand':   _ramp('#47371c', '#c09d5e', '#e2c383', '#f5dfa9', '#fff5d8'),
    'fire':   _ramp('#4a1306', '#d1390f', '#ff6a1f', '#ff9b3b', '#ffd463'),
    'ice':    _ramp('#183a4d', '#79b6d7', '#a8dcf0', '#d1f1fb', '#ffffff'),
    'indigo': _ramp('#0f0f2a', '#23255c', '#33367f', '#4a4fa8', '#7c82d8'),
    'coral':  _ramp('#4a1012', '#c93a40', '#ff5a5f', '#ff8a8a', '#ffc6c2'),
}

LETTER_MAT = {
    'G': 'gold', 'S': 'silver', 'Z': 'bronze', 'R': 'red', 'O': 'orange',
    'Y': 'yellow', 'L': 'lime', 'N': 'green', 'C': 'cyan', 'A': 'aqua',
    'B': 'blue', 'P': 'purple', 'K': 'pink', 'W': 'bone', 'E': 'stone',
    'D': 'dgray', 'T': 'wood', 'X': 'ink', 'Q': 'sand', 'F': 'fire',
    'I': 'ice', 'M': 'indigo', 'H': 'coral',
}
TONE = {'ol': 0, 'dk': 1, 'base': 2, 'lt': 3, 'hi': 4}


def parse(rows, legend=None):
    legend = legend or {}
    h = len(rows)
    w = max(len(r) for r in rows)
    cells = [[None] * w for _ in range(h)]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in legend:
                cells[y][x] = legend[ch]
            elif ch in '. ':
                continue
            elif ch == '+':
                cells[y][x] = ('?', 'hi')
            elif ch.upper() in LETTER_MAT:
                mat = LETTER_MAT[ch.upper()]
                cells[y][x] = (mat, 'auto' if ch.isupper() else 'dk')
            else:
                raise ValueError('bad char %r' % ch)
    # resolve '+' to neighbouring material
    for y in range(h):
        for x in range(w):
            c = cells[y][x]
            if c and c[0] == '?':
                for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (1, 1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and cells[ny][nx] and cells[ny][nx][0] != '?':
                        cells[y][x] = (cells[ny][nx][0], 'hi')
                        break
                else:
                    cells[y][x] = ('bone', 'hi')
    return cells


def render(rows, legend=None, outline=True, light_left=True):
    cells = parse(rows, legend)
    h, w = len(cells), len(cells[0])
    pad = 1 if outline else 0
    img = Image.new('RGBA', (w + 2 * pad, h + 2 * pad), (0, 0, 0, 0))
    px = img.load()

    def mat(x, y):
        if 0 <= x < w and 0 <= y < h and cells[y][x]:
            return cells[y][x][0]
        return None

    for y in range(h):
        for x in range(w):
            c = cells[y][x]
            if not c:
                continue
            m, tone = c
            if tone == 'auto':
                up, dn = mat(x, y - 1) == m, mat(x, y + 1) == m
                lf, rt = mat(x - 1, y) == m, mat(x + 1, y) == m
                if not up or (light_left and not lf and dn):
                    tone = 'lt'
                elif not dn or not rt:
                    tone = 'dk'
                else:
                    tone = 'base'
            px[x + pad, y + pad] = RAMPS[m][TONE[tone]]
    if outline:
        for y in range(h + 2):
            for x in range(w + 2):
                if px[x, y][3]:
                    continue
                cx, cy = x - 1, y - 1
                for dx, dy in ((0, 1), (1, 0), (-1, 0), (0, -1)):
                    m = mat(cx + dx, cy + dy)
                    if m:
                        px[x, y] = RAMPS[m][0]
                        break
    return img


def outline_alpha(img, color, diag=False):
    """Add a 1px outline (grows canvas by 1px each side)."""
    a = np.array(img)
    h, w = a.shape[:2]
    out = np.zeros((h + 2, w + 2, 4), np.uint8)
    out[1:-1, 1:-1] = a
    mask = out[:, :, 3] > 0
    grow = np.zeros_like(mask)
    shifts = [(0, 1), (1, 0), (0, -1), (-1, 0)]
    if diag:
        shifts += [(1, 1), (1, -1), (-1, 1), (-1, -1)]
    for dy, dx in shifts:
        grow |= np.roll(np.roll(mask, dy, 0), dx, 1)
    ring = grow & ~mask
    out[ring] = color
    return Image.fromarray(out)


def drop_shadow(img, color, dy=1, dx=0):
    a = np.array(img)
    h, w = a.shape[:2]
    out = np.zeros((h + dy, w + dx, 4), np.uint8)
    m = a[:, :, 3] > 0
    sh = np.zeros_like(out)
    sh[dy:dy + h, dx:dx + w][m] = color
    out[:] = sh
    top = out[0:h, 0:w]
    top[m] = a[m]
    return Image.fromarray(out)


def paste(dst, src, x, y):
    dst.alpha_composite(src, (x, y))


def scale(img, k):
    return img.resize((img.width * k, img.height * k), Image.NEAREST)


def lerp(c1, c2, t):
    return tuple(int(round(c1[i] + (c2[i] - c1[i]) * t)) for i in range(4))


# ---------------------------------------------------------------- logo font
# Original chunky 7px display face (2px stems) drawn for the Block Showdown logo.
FONT7 = {
    'B': ['XXXX.', 'XX.XX', 'XX.XX', 'XXXX.', 'XX.XX', 'XX.XX', 'XXXX.'],
    'L': ['XX...', 'XX...', 'XX...', 'XX...', 'XX...', 'XX...', 'XXXXX'],
    'O': ['.XXX.', 'XX.XX', 'XX.XX', 'XX.XX', 'XX.XX', 'XX.XX', '.XXX.'],
    'C': ['.XXXX', 'XX...', 'XX...', 'XX...', 'XX...', 'XX...', '.XXXX'],
    'K': ['XX.XX', 'XX.XX', 'XXXX.', 'XXX..', 'XXXX.', 'XX.XX', 'XX.XX'],
    'S': ['.XXXX', 'XX...', 'XX...', '.XXX.', '...XX', '...XX', 'XXXX.'],
    'H': ['XX.XX', 'XX.XX', 'XX.XX', 'XXXXX', 'XX.XX', 'XX.XX', 'XX.XX'],
    'W': ['XX...XX', 'XX...XX', 'XX...XX', 'XX.X.XX', 'XXXXXXX', 'XXX.XXX', 'XX...XX'],
    'D': ['XXXX.', 'XX.XX', 'XX.XX', 'XX.XX', 'XX.XX', 'XX.XX', 'XXXX.'],
    'N': ['XX..XX', 'XXX.XX', 'XXXXXX', 'XX.XXX', 'XX..XX', 'XX..XX', 'XX..XX'],
    ' ': ['...', '...', '...', '...', '...', '...', '...'],
}


def text_mask(text, k=1, spacing=1):
    cols = []
    for i, ch in enumerate(text):
        g = FONT7[ch]
        cols.append(g)
    w = sum(len(g[0]) for g in cols) + spacing * (len(cols) - 1)
    m = np.zeros((7, w), bool)
    x = 0
    for g in cols:
        gw = len(g[0])
        for y in range(7):
            for gx in range(gw):
                if g[y][gx] == 'X':
                    m[y, x + gx] = True
        x += gw + spacing
    if k > 1:
        m = np.kron(m, np.ones((k, k), bool))
    return m


def styled_text(text, k, top, bottom, outline, shadow, spacing=1, shine=None):
    """Gradient-filled logo text with outline + drop shadow."""
    m = text_mask(text, k, spacing * k)
    h, w = m.shape
    a = np.zeros((h, w, 4), np.uint8)
    for y in range(h):
        c = lerp(top, bottom, y / max(1, h - 1))
        a[y][m[y]] = c
    if shine is not None:
        # 1px highlight on the top edge of every stroke
        edge = m & ~np.roll(m, 1, 0)
        edge[0] |= m[0]
        a[edge] = shine
    img = Image.fromarray(a)
    img = outline_alpha(img, outline, diag=True)
    if shadow is not None:
        img = drop_shadow(img, shadow, dy=1)
    return img
