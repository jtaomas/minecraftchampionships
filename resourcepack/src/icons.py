"""All HUD / font glyph artwork for the Block Showdown pack (original pixel art)."""
from PIL import Image
import numpy as np
from pixel import render, RAMPS, rgb, lerp, styled_text, outline_alpha, drop_shadow, paste

# ------------------------------------------------------------------ teams
# Each team has its own silhouette, so teams stay distinguishable for
# colour-blind players and on any background.
TEAM_SHAPES = {
    'Red': ['.RRRRR.', 'RRRRRRR', 'RRRRRRR', 'RRRRRRR', 'RRRRRRR', 'RRRRRRR', '.RRRRR.'],      # badge
    'Orange': ['..OOO..', '.OOOOO.', 'OOOOOOO', 'OOOOOOO', 'OOOOOOO', '.OOOOO.', '..OOO..'],  # disc
    'Yellow': ['...Y...', '..YYY..', '..YYY..', '.YYYYY.', '.YYYYY.', 'YYYYYYY', 'YYYYYYY'],  # triangle
    'Lime': ['...L...', '..LLL..', '.LLLLL.', 'LLLLLLL', '.LLLLL.', '..LLL..', '...L...'],    # diamond
    'Green': ['..NNN..', '..NNN..', 'NNNNNNN', 'NNNNNNN', 'NNNNNNN', '..NNN..', '..NNN..'],   # plus
    'Cyan': ['...C...', '..CCC..', '.CCCCC.', 'CCCCCCC', 'CCCCCCC', '.CCCCC.', '..CCC..'],    # droplet
    'Aqua': ['AAAAAAA', 'AAAAAAA', 'AAAAAAA', 'AAAAAAA', '.AAAAA.', '..AAA..', '...A...'],    # shield
    'Blue': ['..BBBB.', '.BBB...', 'BBB....', 'BBB....', 'BBB....', '.BBB...', '..BBBB.'],    # crescent
    'Purple': ['...P...', '...P...', '..PPP..', 'PPPPPPP', '..PPP..', '...P...', '...P...'],  # sparkle
    'Pink': ['..KKK..', '.KKKKK.', 'KKK.KKK', 'KK...KK', 'KKK.KKK', '.KKKKK.', '..KKK..'],    # ring
}
TEAMS = list(TEAM_SHAPES)


def _team_shape(name, rows):
    # add a tiny specular dot top-left on the filled shapes
    rows = [list(r) for r in rows]
    for y in range(len(rows)):
        for x in range(len(rows[0])):
            if rows[y][x] != '.':
                ny, nx = y + 1, x + 1
                if ny < len(rows) and nx < len(rows[0]) and rows[ny][nx] != '.' and y >= 1:
                    if rows[ny - 1][nx - 1] != '.' and name not in ('Purple', 'Blue', 'Pink'):
                        rows[ny][nx] = '+'
                        return [''.join(r) for r in rows]
    return [''.join(r) for r in rows]


def team_icons():
    return {f'TEAM_{n.upper()}': render(_team_shape(n, s)) for n, s in TEAM_SHAPES.items()}


# ------------------------------------------------------------------ HUD icons
HUD = {
    'COIN': ['..GGG..', '.G+GGG.', 'GGGgGGG', 'GGGgGGG', 'GGGgGGG', '.GGGGG.', '..GGG..'],
    'CROWN': ['G..G..G', 'GG.G.GG', 'GGGGGGG', 'GRGBGRG', 'GGGGGGG', 'ggggggg'],
    'SKULL': ['.WWWWW.', 'WWWWWWW', 'WXXWXXW', 'WXXWXXW', 'WWWXWWW', '.WWWWW.', '.W.W.W.'],
    'SWORD': ['.....SS', '....S+S', '...SSS.', 'Y.SSS..', '.YSS...', '.TYY...', 'T..Y...'],
    'BOW': ['..TTE..', '.T..E..', 'T...E.S', 'SSSSSSS', 'T...E.S', '.T..E..', '..TTE..'],
    'HEART': ['.RR.RR.', 'R+RRRRR', 'RRRRRRR', 'RRRRRRR', '.RRRRR.', '..RRR..', '...R...'],
    'CLOCK': ['..SSS..', '.SWXWS.', 'SWWXWWS', 'SWWXXWS', 'SWWWWWS', '.SWWWS.', '..SSS..'],
    'FLAG': ['ELLLL..', 'ELLLLL.', 'ELLLL..', 'E......', 'E......', 'E......', 'E......'],
    'FLAG_FINISH': ['EWXWXW', 'EXWXWX', 'EWXWXW', 'E.....', 'E.....', 'E.....', 'E.....'],
    'TROPHY': ['GGGGGGGGG', 'G.G+GGG.G', '.GGGGGGG.', '..GGGGG..', '....G....', '...GGG...', '..TTTTT..'],
    'ARROW_UP': ['...L...', '..LLL..', '.LLLLL.', 'LLLLLLL', '..LLL..', '..LLL..', '..LLL..'],
    'ARROW_DOWN': ['..RRR..', '..RRR..', '..RRR..', 'RRRRRRR', '.RRRRR.', '..RRR..', '...R...'],
    'STAR': ['...Y...', '..YYY..', 'YYY+YYY', '.YYYYY.', '..YYY..', '.YY.YY.', '.Y...Y.'],
    'LOCK': ['..SSS..', '.S...S.', '.S...S.', 'GGGGGGG', 'GGGxGGG', 'GGGxGGG', 'GGGGGGG'],
    'CHECK': ['......L', '.....LL', 'L...LL.', 'LL.LL..', '.LLL...', '..L....'],
    'CROSS': ['RR...RR', '.RR.RR.', '..RRR..', '.RR.RR.', 'RR...RR'],
}
_DIGITS = {
    1: ['.d.', 'dd.', '.d.', 'ddd'],
    2: ['dd.', '..d', '.d.', 'ddd'],
    3: ['dd.', '.dd', '..d', 'dd.'],
}


def medal(place):
    metal = {1: 'gold', 2: 'silver', 3: 'bronze'}[place]
    ribbon = {1: 'red', 2: 'blue', 3: 'green'}[place]
    rows = ['.rr.rr.', '..rrr..',
            '.MMMMM.', 'MMMMMMM', 'MMMMMMM', 'MMMMMMM', 'MMMMMMM', '.MMMMM.']
    rows = [list(r) for r in rows]
    for dy, line in enumerate(_DIGITS[place]):
        for dx, ch in enumerate(line):
            if ch == 'd':
                rows[3 + dy][2 + dx] = 'd'
    rows[3][1] = '+'
    return render([''.join(r) for r in rows],
                  legend={'M': (metal, 'auto'), 'd': (metal, 'ol'), 'r': (ribbon, 'auto'),
                          '+': (metal, 'hi')})


def hud_icons():
    out = {k: render(v) for k, v in HUD.items()}
    out['MEDAL_1'] = medal(1)
    out['MEDAL_2'] = medal(2)
    out['MEDAL_3'] = medal(3)
    return out


# ------------------------------------------------------------------ game icons (16x16 tiles)
GAME_IDS = ['skybattle', 'battlebox', 'dodgebolt', 'parkourwarrior', 'rocketspleef', 'hitw',
            'bingo', 'lockout', 'gridrunners', 'railroadrush', 'sandsoftime', 'acerace',
            'tgttos', 'buildmart', 'meltdown', 'parkourtag', 'footrace', 'dome']

CUBE = ['....aaaa....', '..aaaaaaaa..', 'aaaaaaaaaaaa', 'bbaaaaaaaacc', 'bbbbaaaacccc',
        'bbbbbbcccccc', 'bbbbbbcccccc', 'bbbbbbcccccc', 'bbbbbbcccccc', 'bbbbbbcccccc',
        '..bbbbcccc..', '....bbcc....']

GAME_ART = {
    'skybattle': (['.....NN.....', '....NNNN....', '...NN+NNN...', '....NNNN....', '.....TT.....',
                   '.LLLLTTLLLL.', 'LLLLLLLLLLLL', '.TTTTTTTTTT.', '..TTEETTTT..', '...TEEEET...',
                   '....EEEE....', '.....EE.....'], None, 'aqua'),
    'battlebox': (['SS........SS', 'S+S......S+S', '.SSS....SSS.', '..SSS..SSS..', '...SSSSSS...',
                   '....SSSS....', '....SSSS....', '...SSSSSS...', '.YYSS..SSYY.', '..YY....YY..',
                   '.TY......YT.', 'TT........TT'], None, 'red'),
    'dodgebolt': (['.......YYYYY', '........YYYY', '.......Y.YYY', '......Y...YY', '.....Y.....Y',
                   '....Y.......', '...Y........', '..Y.........', 'WY..........', 'WWW.........',
                   '.W..........'], None, 'blue'),
    'parkourwarrior': (['.........ELL', '.........ELL', '.........E..', '........QQQQ', '........QQQQ',
                        '........QQQQ', '.....QQQQQQQ', '.....QQQQQQQ', '.....QQQQQQQ', '..QQQQQQQQQQ',
                        '..QQQQQQQQQQ', '..QQQQQQQQQQ'], None, 'orange'),
    'rocketspleef': (['.....RR.....', '....RWWR....', '....WWWW....', '....WAAW....', '....WAAW....',
                      '....WWWW....', '....WWWW....', '...RWWWWR...', '..RRWWWWRR..', '..RR.EE.RR..',
                      '.....FF.....', '.....YY.....'], None, 'purple'),
    'hitw': (['EEEEEEEEEEEE', 'EEEEE..EEEEE', 'EEEEE..EEEEE', 'EEE......EEE', 'EEE.E..E.EEE',
              'EEEEE..EEEEE', 'EEEEE..EEEEE', 'EEEE.EE.EEEE', 'EEEE.EE.EEEE', 'EEEE.EE.EEEE',
              'EEEEEEEEEEEE', 'eeeeeeeeeeee'], None, 'lime'),
    'bingo': (['LLL.WWW.WWW', 'LLL.WWW.WWW', 'LLL.WWW.WWW', '...........', 'WWW.LLL.WWW',
               'WWW.LLL.WWW', 'WWW.LLL.WWW', '...........', 'WWW.WWW.LLL', 'WWW.WWW.LLL',
               'WWW.WWW.LLL'], None, 'yellow'),
    'lockout': (['RRR.WWW.BBB', 'RRR.WWW.BBB', 'RRR.WWW.BBB', '...........', 'BBB.RRR.WWW',
                 'BBB.RRR.WWW', 'BBB.RRR.WWW', '...........', 'WWW.BBB.RRR', 'WWW.BBB.RRR',
                 'WWW.BBB.RRR'], None, 'dgray'),
    'gridrunners': (['CCCCCCCCCCC', 'C...C.....C', 'C.C.C.CCC.C', 'C.C...C...C', 'C.CCCCC.CCC',
                     'C.....C...C', 'CCC.C.CCC.C', 'C...C.....C', 'C.CCCCC.C.C', 'CY....C.C.C',
                     'CCCCC.CCCCC'], {'Y': ('yellow', 'lt')}, 'indigo'),
    'railroadrush': (['............', 'SSSSSSSSSSSS', 'SOOOOOOOOOOS', 'SSSSSSSSSSSS', '.SSSSSSSSSS.',
                      '.SSSSSSSSSS.', '..DD....DD..', '..DD....DD..', 'EEEEEEEEEEEE', '.TT..TT..TT.'],
                     None, 'green'),
    'sandsoftime': (['TTTTTTTTTT', '.IQQQQQQI.', '.IIQQQQII.', '..IIQQII..', '...IQQI...',
                     '....QQ....', '...IQQI...', '..IIQQII..', '.IIIQQIII.', '.IIQQQQII.',
                     '.IQQQQQQI.', 'TTTTTTTTTT'], None, 'orange'),
    'acerace': (['EWWXXWWXX', 'EWWXXWWXX', 'EXXWWXXWW', 'EXXWWXXWW', 'EWWXXWWXX', 'EWWXXWWXX',
                 'E........', 'E........', 'E........', 'E........'],
                {'W': ('bone', 'base'), 'X': ('ink', 'lt')}, 'cyan'),
    'tgttos': (['.....RR......', '....WWWW.....', '....WWXWOO...', '....WWWWO....', '....WWWWR....',
                '.W..WWWW.....', '.WWWWWWWW....', '.WWWWWWWWW...', '..WWWWWWWW...', '...WWWWWW....',
                '....O..O.....', '...OO.OO.....'], None, 'lime'),
    'buildmart': (CUBE, {'a': ('lime', 'lt'), 'b': ('wood', 'base'), 'c': ('wood', 'dk')}, 'aqua'),
    'meltdown': (['.....F......', '....FF......', '....FFF..F..', '...FFFF.FF..', '..FFFFFFFF..',
                  '..FFFYFFFFF.', '.FFFYYYFFFF.', '.FFYYYYYFFF.', '.FFYYWYYYFF.', '.FFYYWWYYFF.',
                  '..FFYYYYFF..', '...FFFFFF...'], {'W': ('yellow', 'hi')}, 'ice'),
    'parkourtag': (['...OOOOOOOO.', '..OOOOOOOOOO', '.OOOOOOOOOOO', 'OO.OOOOOOOOO', 'OO.OOOOOOOOO',
                    '.OOOOOOOOOOO', '..OOOOOOOOOO', '...OOOOOOOO.'], None, 'pink'),
    'footrace': (['....BBBB....', '....BBBB....', 'EE..BBBBB...', '....BBBBBBB.', 'EEE.BWBBBBBB',
                  '...BBBBBBBBB', '...WWWWWWWWW'], None, 'yellow'),
    'dome': (['....PPPP....', '..PPPPPPPP..', '.PPPPPPPPPP.', '.PPPPPPPPWP.', 'PPPPPPPPWWPP',
              'PPPWPPPWWPPP', 'PPPWWPWWPPPP', 'PPPPWWWPPPPP', 'PPPPPWPPPPPP', 'GGGGGGGGGGGG'],
             {'W': ('bone', 'hi')}, 'gold'),
}


def game_tile(bg):
    """16x16 rounded tile: dark rim, vertical gradient, bevel highlight."""
    r = RAMPS[bg]
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            corner = (x in (0, 15) and y in (0, 15))
            if corner:
                continue
            edge = x in (0, 15) or y in (0, 15)
            if edge:
                px[x, y] = r[0]
            else:
                c = lerp(r[3], r[1], (y - 1) / 13)
                px[x, y] = c
    for x in range(2, 14):
        px[x, 1] = lerp(r[4], r[3], 0.35)
    for y in range(2, 14):
        px[1, y] = lerp(px[1, y], r[4], 0.25)
    for x in range(1, 15):
        px[x, 14] = lerp(r[1], r[0], 0.4)
    return img


def game_icon(gid):
    rows, legend, bg = GAME_ART[gid]
    tile = game_tile(bg)
    sym = render(rows, legend)
    sym = sym.crop(sym.getbbox())
    x = (16 - sym.width) // 2
    y = (16 - sym.height + 1) // 2
    # soft contact shadow under the symbol
    sh = Image.new('RGBA', sym.size, (0, 0, 0, 0))
    a = np.array(sym)
    s = np.zeros_like(a)
    s[a[:, :, 3] > 0] = (0, 0, 0, 70)
    sh = Image.fromarray(s)
    tile.alpha_composite(sh, (min(x + 1, 16 - sym.width), min(y + 1, 16 - sym.height)))
    tile.alpha_composite(sym, (x, y))
    return tile


def game_icons():
    return {gid: game_icon(gid) for gid in GAME_IDS}


# ------------------------------------------------------------------ brand
GOLD_TOP, GOLD_BOT = rgb('#fff3a8'), rgb('#f29a1f')
WHITE_TOP, WHITE_BOT = rgb('#ffffff'), rgb('#b9c6ff')
INK = rgb('#140f2e')
SHADOW = rgb('#0a0818', 200)


def emblem(big=False):
    """Original brand mark: tri-colour isometric block."""
    legend = {'a': ('gold', 'lt'), 'b': ('coral', 'base'), 'c': ('indigo', 'lt'), '+': ('gold', 'hi')}
    rows = [r for r in CUBE]
    rows = [list(r) for r in rows]
    rows[1][4] = '+'; rows[1][5] = '+'; rows[2][3] = '+'
    img = render([''.join(r) for r in rows], legend)
    return img


def logo_banner():
    """Sidebar-title banner: emblem + BLOCK SHOWDOWN, 11px tall."""
    e = render(['..aa..', 'aaaaaa', 'baaaac', 'bbbccc', 'bbbccc', '.bbcc.'],
               {'a': ('gold', 'lt'), 'b': ('coral', 'base'), 'c': ('indigo', 'lt')})
    t1 = styled_text('BLOCK', 1, GOLD_TOP, GOLD_BOT, INK, SHADOW, shine=rgb('#fffbe0'))
    t2 = styled_text('SHOWDOWN', 1, WHITE_TOP, WHITE_BOT, INK, SHADOW, shine=rgb('#ffffff'))
    h = max(t1.height, t2.height, e.height)
    w = e.width + 2 + t1.width + 2 + t2.width
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    img.alpha_composite(e, (0, (h - e.height) // 2))
    img.alpha_composite(t1, (e.width + 2, 0))
    img.alpha_composite(t2, (e.width + 2 + t1.width + 2, 0))
    return img


def logo_large():
    """Stacked logo for splash titles / pack icon."""
    t1 = styled_text('BLOCK', 2, GOLD_TOP, GOLD_BOT, INK, SHADOW, spacing=1, shine=rgb('#fffbe0'))
    t2 = styled_text('SHOWDOWN', 1, WHITE_TOP, WHITE_BOT, INK, SHADOW, spacing=2, shine=rgb('#ffffff'))
    e = emblem()
    top_w = e.width + 2 + t1.width
    w = max(top_w, t2.width)
    h = t1.height + 1 + t2.height
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    ox = (w - top_w) // 2
    img.alpha_composite(e, (ox, (t1.height - e.height) // 2))
    img.alpha_composite(t1, (ox + e.width + 2, 0))
    img.alpha_composite(t2, ((w - t2.width) // 2, t1.height + 1))
    return img


def emblem_small():
    """Inline 9px brand mark for chat prefixes."""
    return render(['..aa...', 'aaaaaa.', 'baaaacc', 'bbbaccc', 'bbbbccc', '.bbbcc.', '...bc..'],
                  {'a': ('gold', 'lt'), 'b': ('coral', 'base'), 'c': ('indigo', 'lt')})


def title_bar(w=120, h=14):
    """Translucent broadcast lower-third bar with gold rules and faded ends."""
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = img.load()
    top, bot = rgb('#2a2c6e'), rgb('#15163a')
    gold = rgb('#f2b31f')
    fade = 18
    for x in range(w):
        t = min(1.0, x / fade, (w - 1 - x) / fade)
        t = t * t * (3 - 2 * t)
        for y in range(h):
            if y == 0 or y == h - 1:
                c = gold if y == 0 else rgb('#c27612')
                a = 255
            elif y == 1:
                c = rgb('#4a4fa8'); a = 220
            else:
                c = lerp(top, bot, (y - 2) / (h - 3)); a = 205
            px[x, y] = (c[0], c[1], c[2], int(a * t))
    return img
