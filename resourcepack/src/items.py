"""16x16 custom item textures + 1.21.1 custom_model_data override definitions."""
from PIL import Image
import numpy as np
from pixel import render, RAMPS, rgb, lerp
import icons


def token(rim, emblem_img):
    """Round 16x16 token: metal rim, coloured face, emblem in the middle."""
    r = RAMPS[rim]
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d > 7.6:
                continue
            if d > 6.7:
                px[x, y] = r[0]
            elif d > 5.4:
                # rim, lit from top-left
                t = ((x - cx) + (y - cy)) / 12 + 0.5
                px[x, y] = lerp(RAMPS['gold'][4], RAMPS['gold'][1], max(0, min(1, t)))
            else:
                t = (y - 2) / 11
                px[x, y] = lerp(r[3], r[1], max(0, min(1, t)))
    e = emblem_img.crop(emblem_img.getbbox())
    img.alpha_composite(e, ((16 - e.width) // 2, (16 - e.height) // 2))
    return img


ITEM_ART = {
    'trophy': (['GGGGGGGGGGGG', 'GG+GGGGGGGGG', 'G.GGGGGGGG.G', 'G.GGGGGGGG.G', '.GGGGGGGGGG.',
                '..GGGGGGGG..', '...GGGGGG...', '.....GG.....', '.....GG.....', '....GGGG....',
                '..TTTTTTTT..', '..TTWWWWTT..', '..TTTTTTTT..'], {'W': ('gold', 'lt')}),
    'vote_token': (['WWWWWWWWWWWW', 'WWWWWWWWWWWW', 'WWWWWWWWWLWW', 'WWWWWWWWLLWW', 'WWLWWWWLLWWW',
                    'WWLLWWLLWWWW', 'WWWLLLLWWWWW', 'WWWWLLWWWWWW', 'WWWWWWWWWWWW', 'PPPPPPPPPPPP',
                    'pppppppppppp'], None),
}

TEAM_COLOURS = {'Red': 'red', 'Orange': 'orange', 'Yellow': 'yellow', 'Lime': 'lime', 'Green': 'green',
                'Cyan': 'cyan', 'Aqua': 'aqua', 'Blue': 'blue', 'Purple': 'purple', 'Pink': 'pink'}


def flag(mat):
    rows = ['GG..........', 'EFFFFFFFF...', 'EFFFFFFFFFF.', 'EFF+FFFFFFFF', 'EFFFFFFFFFF.', 'EFFFFFFFF...',
            'EFFFFFF.....', 'E...........', 'E...........', 'E...........', 'E...........', 'E...........',
            'EE..........']
    return render(rows, {'F': (mat, 'auto'), '+': (mat, 'hi')})


def _seg_dist(px, py, ax, ay, bx, by):
    vx, vy = bx - ax, by - ay
    t = max(0.0, min(1.0, ((px - ax) * vx + (py - ay) * vy) / (vx * vx + vy * vy)))
    return ((px - ax - t * vx) ** 2 + (py - ay - t * vy) ** 2) ** 0.5, t


def launcher_rows():
    """Chunky diagonal rocket tube: gold muzzle, red bands, steel body, wood grip."""
    n = 14
    g = [['.'] * n for _ in range(n)]
    for y in range(n):
        for x in range(n):
            d, t = _seg_dist(x, y, 1.5, 11.5, 11.5, 1.5)
            if d < 1.75:
                if t > 0.86:
                    g[y][x] = 'G'
                elif 0.55 < t < 0.64 or 0.22 < t < 0.3:
                    g[y][x] = 'R'
                elif t < 0.08:
                    g[y][x] = 'D'
                else:
                    g[y][x] = 'S'
            d2, _ = _seg_dist(x, y, 5.5, 8.5, 8.0, 11.5)
            if g[y][x] == '.' and d2 < 1.0:
                g[y][x] = 'T'
    g[2][10] = '+'
    return [''.join(r) for r in g], None


def bolt_rows():
    """Energy bolt arrow: glowing yellow head, cyan shaft, blue fletching."""
    n = 14
    g = [['.'] * n for _ in range(n)]
    for y in range(n):
        for x in range(n):
            d, t = _seg_dist(x, y, 1.5, 12.5, 10.5, 3.5)
            if d < 0.75:
                g[y][x] = 'A'
    head = ['....YY', '..YYYY', '...YWY', '..YWYY', '.YY.YY', '.Y....']
    for dy, row in enumerate(head):
        for dx, ch in enumerate(row):
            if ch != '.':
                g[dy][8 + dx] = ch
    for (x, y) in ((0, 11), (1, 11), (0, 12), (2, 13), (2, 12), (1, 13), (0, 13)):
        g[y][x] = 'B'
    return [''.join(r) for r in g], {'W': ('yellow', 'hi')}


def centered16(img):
    img = img.crop(img.getbbox())
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    out.alpha_composite(img, ((16 - img.width) // 2, (16 - img.height) // 2))
    return out


def all_items():
    hud = icons.hud_icons()
    out = {}
    for k, (rows, legend) in ITEM_ART.items():
        out[k] = centered16(render(rows, legend))
    out['rocket_launcher'] = centered16(render(*launcher_rows()))
    out['dodgebolt_bolt'] = centered16(render(*bolt_rows()))
    # Battle Box kit tokens (Fighter / Archer / Marksman / Medic)
    medic = render(['..W..', '..W..', 'WWWWW', '..W..', '..W..'], {'W': ('bone', 'lt')})
    target = render(['..RRR..', '.RWWWR.', 'RWRRRWR', 'RWRWRWR', 'RWRRRWR', '.RWWWR.', '..RRR..'],
                    {'W': ('bone', 'base')})
    out['kit_fighter'] = token('red', hud['SWORD'])
    out['kit_archer'] = token('green', hud['BOW'])
    out['kit_marksman'] = token('orange', target)
    out['kit_medic'] = token('pink', medic)
    out['checkpoint_token'] = token('lime', hud['FLAG'])
    out['team_flag'] = centered16(flag('bone'))
    for team, mat in TEAM_COLOURS.items():
        out['team_flag_' + team.lower()] = centered16(flag(mat))
    return out


# (vanilla item, parent, [(cmd, model_name)])
OVERRIDES = {
    'arrow': ('minecraft:item/generated', [(1001, 'dodgebolt_bolt')]),
    'paper': ('minecraft:item/generated', [
        (1001, 'kit_fighter'), (1002, 'kit_archer'), (1003, 'kit_marksman'), (1004, 'kit_medic'),
        (1010, 'vote_token'), (1020, 'checkpoint_token'), (1030, 'team_flag'),
        (1031, 'team_flag_red'), (1032, 'team_flag_orange'), (1033, 'team_flag_yellow'),
        (1034, 'team_flag_lime'), (1035, 'team_flag_green'), (1036, 'team_flag_cyan'),
        (1037, 'team_flag_aqua'), (1038, 'team_flag_blue'), (1039, 'team_flag_purple'),
        (1040, 'team_flag_pink')]),
    'golden_shovel': ('minecraft:item/handheld', [(1001, 'rocket_launcher')]),
    'gold_ingot': ('minecraft:item/generated', [(1001, 'trophy')]),
}
HANDHELD = {'rocket_launcher'}
