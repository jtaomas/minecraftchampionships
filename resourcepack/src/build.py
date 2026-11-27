"""Block Showdown resource pack - full build.

    python src/build.py            (from the resourcepack folder, or anywhere)

Regenerates pack/, dist/BlockShowdownPack.zip, dist/sha1.txt, McmGlyphs.java,
the glyph table inside README.md and preview_sheet.png. (Ogg serials vary, so the zip SHA-1 changes per build.)
"""
import hashlib
import json
import math
import os
import shutil
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
ROOT = os.path.dirname(HERE)
PACK = os.path.join(ROOT, 'pack')
DIST = os.path.join(ROOT, 'dist')
NS = 'mcmshow'
PACK_FORMAT = 34  # Minecraft 1.21 / 1.21.1

from PIL import Image, ImageDraw, ImageFont  # noqa: E402
import numpy as np  # noqa: E402
import icons  # noqa: E402
import gui  # noqa: E402
import items  # noqa: E402
import sounds  # noqa: E402
from pixel import scale, rgb, lerp, styled_text, RAMPS  # noqa: E402


def wjson(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2, ensure_ascii=True)
        f.write('\n')


def wpng(path, img):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)


def inline_ascent(h):
    """Ascent that centres an h-px glyph on the 8px vanilla text line."""
    return min(h, int(4 + h / 2))


# ============================================================== glyph registry
class Glyph:
    def __init__(self, const, cp, img, folder, ascent=None, group='', desc=''):
        self.const, self.cp, self.img, self.folder = const, cp, img, folder
        self.height = img.height
        self.ascent = inline_ascent(img.height) if ascent is None else ascent
        self.group, self.desc = group, desc
        self.file = f'{NS}:font/{folder}/{const.lower()}.png'

    @property
    def char(self):
        return chr(self.cp)

    @property
    def advance(self):
        bbox = self.img.getbbox()
        return (bbox[2] if bbox else 0) + 1


def build_registry():
    reg = []

    def add(const, cp, img, folder, ascent=None, group='', desc=''):
        reg.append(Glyph(const, cp, img, folder, ascent, group, desc))

    for i, (name, img) in enumerate(icons.team_icons().items()):
        add(name, 0xE000 + i, img, 'team', group='Teams', desc=name.split('_')[1].title() + ' team icon')
    hud_order = ['COIN', 'CROWN', 'SKULL', 'SWORD', 'BOW', 'HEART', 'CLOCK', 'FLAG', 'TROPHY',
                 'MEDAL_1', 'MEDAL_2', 'MEDAL_3', 'ARROW_UP', 'ARROW_DOWN', 'STAR', 'LOCK', 'CHECK',
                 'CROSS', 'FLAG_FINISH']
    hud = icons.hud_icons()
    descs = {'COIN': 'coins / points', 'CROWN': 'leader / winner', 'SKULL': 'elimination',
             'FLAG': 'checkpoint flag', 'FLAG_FINISH': 'finish line', 'CLOCK': 'timer',
             'MEDAL_1': '1st place', 'MEDAL_2': '2nd place', 'MEDAL_3': '3rd place',
             'ARROW_UP': 'rank up', 'ARROW_DOWN': 'rank down', 'LOCK': 'locked',
             'CHECK': 'yes / done', 'CROSS': 'no / failed'}
    for i, k in enumerate(hud_order):
        add(k, 0xE010 + i, hud[k], 'hud', group='HUD', desc=descs.get(k, k.lower()))
    for i, (gid, img) in enumerate(icons.game_icons().items()):
        add('GAME_' + gid.upper(), 0xE040 + i, img, 'game', group='Games', desc=gid)
    add('LOGO', 0xE060, icons.logo_banner(), 'brand', group='Brand',
        desc='sidebar title banner (emblem + BLOCK SHOWDOWN)')
    add('LOGO_LARGE', 0xE061, icons.logo_large(), 'brand', group='Brand',
        desc='stacked splash logo for titles / MOTD-style chat headers')
    add('EMBLEM', 0xE062, icons.emblem_small(), 'brand', group='Brand', desc='inline brand mark')
    bar = icons.title_bar()
    add('TITLE_BAR', 0xE063, bar, 'brand', ascent=10, group='Brand',
        desc='translucent lower-third bar for actionbar/subtitle text')
    for i, (const, (img, rows)) in enumerate(gui.all_guis().items()):
        g = Glyph(const, 0xE070 + i, img, 'gui', ascent=13, group='GUI',
                  desc=f'{rows}-row chest overlay ({9 * rows} slots)')
        g.rows = rows
        reg.append(g)
    return reg


SPACES = [1, 2, 4, 8, 16, 32, 64, 128, 256]
NEG_BASE, POS_BASE = 0xF801, 0xF821


# ============================================================== writers
def write_font(reg):
    providers = []
    for g in reg:
        assert g.ascent <= g.height, g.const
        wpng(os.path.join(PACK, 'assets', NS, 'textures', 'font', g.folder, g.const.lower() + '.png'), g.img)
        providers.append({'type': 'bitmap', 'file': g.file, 'ascent': g.ascent, 'height': g.height,
                          'chars': [g.char]})
    adv = {}
    for i, n in enumerate(SPACES):
        adv[chr(NEG_BASE + i)] = -n
        adv[chr(POS_BASE + i)] = n
    providers.append({'type': 'space', 'advances': adv})
    wjson(os.path.join(PACK, 'assets', 'minecraft', 'font', 'default.json'), {'providers': providers})


def write_meta():
    wjson(os.path.join(PACK, 'pack.mcmeta'), {
        'pack': {
            'pack_format': PACK_FORMAT,
            'supported_formats': [34, 99],
            'min_format': 34,
            'max_format': 99,
            'description': [
                {'text': 'Block Showdown', 'color': 'gold', 'bold': True},
                {'text': '\nTournament HUD, menus & sounds', 'color': 'gray'},
            ],
        }
    })
    # pack.png 128x128
    W = 128
    img = Image.new('RGBA', (W, W))
    px = img.load()
    c1, c2 = rgb('#3a3e9a'), rgb('#12122e')
    for y in range(W):
        for x in range(W):
            d = math.hypot(x - 64, y - 52) / 90
            c = lerp(c1, c2, min(1, d))
            if (x + y) % 16 < 2:
                c = lerp(c, rgb('#ffffff'), 0.04)
            px[x, y] = c
    e = scale(icons.emblem(), 3)
    img.alpha_composite(e, ((W - e.width) // 2, 8))
    t1 = styled_text('BLOCK', 3, icons.GOLD_TOP, icons.GOLD_BOT, icons.INK, icons.SHADOW,
                     shine=rgb('#fffbe0'))
    t2 = styled_text('SHOWDOWN', 2, icons.WHITE_TOP, icons.WHITE_BOT, icons.INK, icons.SHADOW,
                     spacing=1, shine=rgb('#ffffff'))
    img.alpha_composite(t1, ((W - t1.width) // 2, 56))
    img.alpha_composite(t2, ((W - t2.width) // 2, 56 + t1.height + 4))
    # gold frame
    for i in range(W):
        for (x, y) in ((i, 0), (i, W - 1), (0, i), (W - 1, i)):
            px[x, y] = RAMPS['gold'][1]
        for (x, y) in ((i, 1), (i, W - 2), (1, i), (W - 2, i)):
            if 1 <= i <= W - 2:
                px[x, y] = RAMPS['gold'][3]
    wpng(os.path.join(PACK, 'pack.png'), img.convert('RGB'))
    return img


def write_items():
    tex = items.all_items()
    for name, img in tex.items():
        wpng(os.path.join(PACK, 'assets', NS, 'textures', 'item', name + '.png'), img)
        parent = 'minecraft:item/handheld' if name in items.HANDHELD else 'minecraft:item/generated'
        wjson(os.path.join(PACK, 'assets', NS, 'models', 'item', name + '.json'),
              {'parent': parent, 'textures': {'layer0': f'{NS}:item/{name}'}})
    for item, (parent, ovs) in items.OVERRIDES.items():
        ovs = sorted(ovs)
        wjson(os.path.join(PACK, 'assets', 'minecraft', 'models', 'item', item + '.json'), {
            'parent': parent,
            'textures': {'layer0': f'minecraft:item/{item}'},
            'overrides': [{'predicate': {'custom_model_data': cmd}, 'model': f'{NS}:item/{m}'}
                          for cmd, m in ovs],
        })
        # Minecraft 1.21.4+ (including 1.21.11) item model definitions
        wjson(os.path.join(PACK, 'assets', 'minecraft', 'items', item + '.json'), {
            'model': {
                'type': 'minecraft:range_dispatch',
                'property': 'minecraft:custom_model_data',
                'fallback': {'type': 'minecraft:model', 'model': f'minecraft:item/{item}'},
                'entries': [{'threshold': float(cmd), 'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{m}'}}
                            for cmd, m in ovs],
            }
        })
    return tex


def write_mcc_compat(reg):
    """Add mcc:* fonts (icon, hud, callout, callout_t1, callout_t2, card) and minecraft:mcc.* sound mappings
    so MinecraftMondayCore (McmHud, McmFx, Callout, CoinFeedback) and McmBingo work on 1.21.1 through 1.21.11+."""
    by_const = {g.const: g for g in reg}

    # 1. mcc:icon (matches McmHud.java codepoints: COIN..STAR, SPECTATOR, PLAYER, TEAM_BASE+0..9)
    icon_map = [
        (0xE000, 'COIN'), (0xE001, 'CROWN'), (0xE002, 'TROPHY'), (0xE003, 'SKULL'),
        (0xE004, 'CHECK'), (0xE005, 'CROSS'), (0xE006, 'STAR'),
        (0xE00A, 'CLOCK'), (0xE00B, 'EMBLEM'),
    ]
    team_consts = ['TEAM_RED', 'TEAM_ORANGE', 'TEAM_YELLOW', 'TEAM_LIME', 'TEAM_GREEN',
                   'TEAM_CYAN', 'TEAM_AQUA', 'TEAM_BLUE', 'TEAM_PURPLE', 'TEAM_PINK']
    for i, tc in enumerate(team_consts):
        icon_map.append((0xE010 + i, tc))
    icon_providers = []
    for cp, const in icon_map:
        g = by_const[const]
        icon_providers.append({'type': 'bitmap', 'file': g.file, 'ascent': min(g.ascent, g.height),
                               'height': g.height, 'chars': [chr(cp)]})
    wjson(os.path.join(PACK, 'assets', 'mcc', 'font', 'icon.json'), {'providers': icon_providers})

    # 2. mcc:hud (clean reference to default font so titles/actionbars never show missing-glyph boxes)
    wjson(os.path.join(PACK, 'assets', 'mcc', 'font', 'hud.json'), {
        'providers': [{'type': 'reference', 'id': 'minecraft:default'}]
    })

    # 3. mcc:callout, mcc:callout_t1, mcc:callout_t2 (matches Callout.java)
    callout_adv = {}
    for k in range(11):
        v = 0.5 * (1 << k)
        callout_adv[chr(0xE800 + k)] = v
        callout_adv[chr(0xE820 + k)] = -v
    # Dark translucent callout box (57x22 -> 114x44 GUI px at 2x subtitle scale)
    box = Image.new('RGBA', (57, 22), (16, 18, 34, 215))
    bp = box.load()
    for x in range(57):
        bp[x, 0] = (255, 216, 74, 240)
        bp[x, 21] = (255, 216, 74, 240)
    for y in range(22):
        bp[0, y] = (255, 216, 74, 240)
        bp[56, y] = (255, 216, 74, 240)
    wpng(os.path.join(PACK, 'assets', 'mcc', 'textures', 'font', 'callout_box.png'), box)

    def make_badge(src_img, white_only=False):
        canvas = Image.new('RGBA', (36, 18), (0, 0, 0, 0))
        scaled = src_img.resize((min(16, src_img.width * 2), min(16, src_img.height * 2)), Image.NEAREST)
        if white_only:
            wp = scaled.copy()
            px = wp.load()
            for yy in range(wp.height):
                for xx in range(wp.width):
                    if px[xx, yy][3] > 0:
                        px[xx, yy] = (255, 255, 255, px[xx, yy][3])
            scaled = wp
        canvas.alpha_composite(scaled, ((36 - scaled.width) // 2, (18 - scaled.height) // 2))
        return canvas

    badge_defs = [
        (0xE000, 'single', 'SWORD'), (0xE001, 'double', 'SWORD'), (0xE002, 'triple', 'CROWN'),
        (0xE003, 'freeze', 'LOCK'), (0xE004, 'lap', 'FLAG'), (0xE005, 'room', 'CHECK'),
        (0xE006, 'objective', 'STAR'),
        (0xE010, 'dot', 'COIN'), (0xE011, 'star', 'STAR'), (0xE012, 'ring', 'EMBLEM'),
    ]
    callout_providers = [{'type': 'space', 'advances': callout_adv}]
    callout_providers.append({'type': 'bitmap', 'file': 'mcc:font/callout_box.png', 'ascent': 16, 'height': 22, 'chars': [chr(0xE030)]})
    for cp, name, const in badge_defs:
        wpng(os.path.join(PACK, 'assets', 'mcc', 'textures', 'font', f'callout_{name}.png'), make_badge(by_const[const].img))
        callout_providers.append({'type': 'bitmap', 'file': f'mcc:font/callout_{name}.png', 'ascent': 18, 'height': 18, 'chars': [chr(cp)]})
        if 0xE000 <= cp <= 0xE006:
            wpng(os.path.join(PACK, 'assets', 'mcc', 'textures', 'font', f'callout_{name}_sil.png'), make_badge(by_const[const].img, white_only=True))
            callout_providers.append({'type': 'bitmap', 'file': f'mcc:font/callout_{name}_sil.png', 'ascent': 18, 'height': 18, 'chars': [chr(0xE020 + (cp - 0xE000))]})
    wjson(os.path.join(PACK, 'assets', 'mcc', 'font', 'callout.json'), {'providers': callout_providers})
    wjson(os.path.join(PACK, 'assets', 'mcc', 'font', 'callout_t1.json'), {
        'providers': [{'type': 'reference', 'id': 'minecraft:default'}]
    })
    t2_providers = [{'type': 'reference', 'id': 'minecraft:default'}]
    for i, tc in enumerate(team_consts):
        g = by_const[tc]
        t2_providers.append({'type': 'bitmap', 'file': g.file, 'ascent': min(g.ascent, g.height),
                             'height': g.height, 'chars': [chr(0xE900 + i)]})
    eg = by_const['EMBLEM']
    t2_providers.append({'type': 'bitmap', 'file': eg.file, 'ascent': min(eg.ascent, eg.height),
                         'height': eg.height, 'chars': [chr(0xE90A)]})
    wjson(os.path.join(PACK, 'assets', 'mcc', 'font', 'callout_t2.json'), {'providers': t2_providers})

    # 4. mcc:card (Bingo card difficulty cell tints matching BingoGame.java)
    tier_colors = [
        (85, 220, 105, 95),   # tier 1: easy green
        (245, 215, 70, 95),   # tier 2: medium yellow
        (245, 140, 50, 105),  # tier 3: hard orange
        (230, 65, 75, 115),   # tier 4: very hard red
    ]
    card_providers = [{'type': 'space', 'advances': {chr(0xE1F0): 36, chr(0xE1F1): 1, chr(0xE1F2): -126}}]
    for t_idx, rgba in enumerate(tier_colors):
        sq = Image.new('RGBA', (16, 16), rgba)
        wpng(os.path.join(PACK, 'assets', 'mcc', 'textures', 'font', f'card_t{t_idx + 1}.png'), sq)
        for r in range(5):
            cp = 0xE100 + t_idx * 5 + r
            card_providers.append({'type': 'bitmap', 'file': f'mcc:font/card_t{t_idx + 1}.png',
                                   'ascent': -11 - r * 18, 'height': 16, 'chars': [chr(cp)]})
    wjson(os.path.join(PACK, 'assets', 'mcc', 'font', 'card.json'), {'providers': card_providers})

    # 5. minecraft:mcc.* sound aliases (matches McmFx, CoinFeedback, DodgeboltGame, WorldRace)
    mcc_sounds = {
        'mcc.ui.coins_small': 'point_gained',
        'mcc.ui.score': 'point_gained',
        'mcc.countdown': 'countdown_tick',
        'mcc.go': 'countdown_go',
        'mcc.victory.win': 'round_win',
        'mcc.victory.big_success': 'podium_fanfare',
        'mcc.death': 'elimination',
        'mcc.crowd.cheer': 'podium_fanfare',
        'mcc.ar.checkpoint_pass': 'checkpoint',
        'mcc.ar.launch1': 'countdown_go',
        'mcc.ar.jump1': 'vote_cast',
        'mcc.ar.speed1': 'checkpoint',
        'mcc.ar.elytra_zone': 'vote_locked',
        'mcc.ar.elytra_remove': 'countdown_tick',
    }
    mc_sj = {k: {'sounds': [{'name': f'{NS}:{v}'}]} for k, v in mcc_sounds.items()}
    wjson(os.path.join(PACK, 'assets', 'minecraft', 'sounds.json'), mc_sj)


def write_sounds():
    info = sounds.write_all(os.path.join(PACK, 'assets', NS, 'sounds'))
    sj, lang = {}, {}
    for name, (_, sub) in sounds.SOUNDS.items():
        sj[name] = {'sounds': [{'name': f'{NS}:{name}'}], 'subtitle': f'subtitles.{NS}.{name}'}
        lang[f'subtitles.{NS}.{name}'] = sub
    wjson(os.path.join(PACK, 'assets', NS, 'sounds.json'), sj)
    wjson(os.path.join(PACK, 'assets', NS, 'lang', 'en_us.json'), lang)
    return info


def write_bossbar():
    """Restyles only the WHITE boss bar (used for the event timer)."""
    W, H = 182, 5
    bg = Image.new('RGBA', (W, H))
    fg = Image.new('RGBA', (W, H))
    b, f = bg.load(), fg.load()
    for x in range(W):
        for y in range(H):
            edge = y in (0, H - 1) or x in (0, W - 1)
            b[x, y] = rgb('#0f0f2a') if edge else lerp(rgb('#23255c'), rgb('#15163a'), (y - 1) / 2)
            t = x / (W - 1)
            base = lerp(rgb('#ffd84a'), rgb('#ff5a5f'), t)
            if edge:
                c = lerp(base, rgb('#3a1a10'), 0.55)
            elif y == 1:
                c = lerp(base, rgb('#ffffff'), 0.45)
            elif y == 3:
                c = lerp(base, rgb('#7a2a10'), 0.25)
            else:
                c = base
            f[x, y] = c
    d = os.path.join(PACK, 'assets', 'minecraft', 'textures', 'gui', 'sprites', 'boss_bar')
    wpng(os.path.join(d, 'white_background.png'), bg)
    wpng(os.path.join(d, 'white_progress.png'), fg)
    return bg, fg


# ============================================================== java
def jesc(s):
    return ''.join(c if 32 <= ord(c) < 127 and c not in '"\\' else '\\u%04X' % ord(c) for c in s)


def write_java(reg, sound_info):
    guis = [g for g in reg if g.group == 'GUI']
    lines = []
    A = lines.append
    A('package com.mcm.api;')
    A('')
    A('import java.util.Locale;')
    A('import java.util.Map;')
    A('import java.util.Set;')
    A('import java.util.UUID;')
    A('import java.util.concurrent.ConcurrentHashMap;')
    A('')
    A('/**')
    A(' * Glyph, model-data and sound constants for the Block Showdown resource pack.')
    A(' * GENERATED by resourcepack/src/build.py - do not edit by hand, re-run the build.')
    A(' *')
    A(' * Glyphs live in the default font (minecraft:default) at private-use codepoints, so')
    A(' * they work in any legacy String or Adventure text. Only send them to players who have')
    A(' * the pack loaded (see {@link #hasPack(UUID)}); everyone else should get plain text.')
    A(' */')
    A('public final class McmGlyphs {')
    A('    private McmGlyphs() {}')
    A('')
    cur = None
    for g in reg:
        if g.group != cur:
            cur = g.group
            A(f'    // ---- {cur} ' + '-' * (60 - len(cur)))
        A(f'    /** U+{g.cp:04X} {g.img.width}x{g.img.height} - {g.desc} */')
        A(f'    public static final String {g.const} = "{jesc(g.char)}";')
    A('')
    A('    // ---- Spacing (font "space" provider) ----------------------------------')
    for i, n in enumerate(SPACES):
        A(f'    public static final String NEG_{n} = "{jesc(chr(NEG_BASE + i))}";')
    for i, n in enumerate(SPACES):
        A(f'    public static final String POS_{n} = "{jesc(chr(POS_BASE + i))}";')
    A('')
    A('    /** Pixel-precise horizontal shift (negative = move left). Any value, built from powers of two. */')
    A('    public static String space(int px) {')
    A('        if (px == 0) return "";')
    A('        StringBuilder sb = new StringBuilder();')
    A('        boolean neg = px < 0;')
    A('        int left = Math.abs(px);')
    A(f'        int[] steps = {{{", ".join(str(n) for n in reversed(SPACES))}}};')
    A(f'        String[] negs = {{{", ".join("NEG_%d" % n for n in reversed(SPACES))}}};')
    A(f'        String[] poss = {{{", ".join("POS_%d" % n for n in reversed(SPACES))}}};')
    A('        for (int i = 0; i < steps.length; i++) {')
    A('            while (left >= steps[i]) { sb.append(neg ? negs[i] : poss[i]); left -= steps[i]; }')
    A('        }')
    A('        return sb.toString();')
    A('    }')
    A('')
    A('    // ---- Chest-menu overlays ---------------------------------------------')
    adv = guis[0].advance
    A(f'    /** Horizontal advance of every GUI_* glyph (image width {guis[0].img.width} + 1). */')
    A(f'    public static final int GUI_ADVANCE = {adv};')
    for g in guis:
        A(f'    /** Required inventory size for {g.const}. */')
        A(f'    public static final int {g.const}_SIZE = {9 * g.rows};')
    A('')
    A('    /**')
    A('     * Container title that paints a custom background behind a chest menu.')
    A('     * Result: "\\u00a7f" + space(-8) + background + space(-(GUI_ADVANCE - 8)) + "\\u00a7f" + label.')
    A('     * The \\u00a7f keeps the background from being tinted by the default dark-grey title colour.')
    A('     * Use with Bukkit.createInventory(holder, GUI_x_SIZE, guiTitle(GUI_x, "Label")).')
    A('     */')
    A('    public static String guiTitle(String background, String label) {')
    A('        return "\\u00a7f" + space(-8) + background + space(-(GUI_ADVANCE - 8)) + "\\u00a7f" + label;')
    A('    }')
    A('')
    A('    // ---- Lookups ----------------------------------------------------------')
    A('    private static final Map<String, String> TEAMS = Map.of(')
    teams = [g for g in reg if g.group == 'Teams']
    A(',\n'.join(f'            "{g.const[5:].lower()}", {g.const}' for g in teams) + ');')
    A('')
    A('    /** Team icon by team name ("Red", "red", "RED"...); empty string if unknown. */')
    A('    public static String teamIcon(String teamName) {')
    A('        if (teamName == null) return "";')
    A('        return TEAMS.getOrDefault(teamName.trim().toLowerCase(Locale.ROOT), "");')
    A('    }')
    A('')
    games = [g for g in reg if g.group == 'Games']
    A('    private static final Map<String, String> GAMES = Map.ofEntries(')
    A(',\n'.join(f'            Map.entry("{g.desc}", {g.const})' for g in games) + ');')
    A('')
    A('    /** 16x16 game tile by game id (skybattle, battlebox, ..., dome); empty string if unknown. */')
    A('    public static String gameIcon(String gameId) {')
    A('        if (gameId == null) return "";')
    A('        return GAMES.getOrDefault(gameId.trim().toLowerCase(Locale.ROOT), "");')
    A('    }')
    A('')
    A('    /** Medal for a 1-based placement (1..3), otherwise empty. */')
    A('    public static String medal(int place) {')
    A('        return switch (place) { case 1 -> MEDAL_1; case 2 -> MEDAL_2; case 3 -> MEDAL_3; default -> ""; };')
    A('    }')
    A('')
    A('    // ---- Title bar helper -------------------------------------------------')
    tb = [g for g in reg if g.const == 'TITLE_BAR'][0]
    A(f'    public static final int TITLE_BAR_WIDTH = {tb.advance - 1};  // drawn width (advance - 1)')
    A('')
    A('    /** Approximate vanilla default-font width of a string (ignores \\u00a7 codes, honours \\u00a7l). */')
    A('    public static int textWidth(String s) {')
    A('        int w = 0; boolean bold = false;')
    A('        for (int i = 0; i < s.length(); i++) {')
    A('            char c = s.charAt(i);')
    A("            if (c == '\\u00a7' && i + 1 < s.length()) {")
    A('                char k = Character.toLowerCase(s.charAt(++i));')
    A("                if (k == 'l') bold = true; else if (k == 'r' || \"0123456789abcdef\".indexOf(k) >= 0) bold = false;")
    A('                continue;')
    A('            }')
    A('            int a = switch (c) {')
    A("                case '!', '\\'', ',', '.', ':', ';', 'i', '|' -> 2;")
    A("                case '`', 'l' -> 3;")
    A("                case ' ', '\"', '(', ')', '*', 'I', '[', ']', 't', '{', '}' -> 4;")
    A("                case '<', '>', 'f', 'k' -> 5;")
    A("                case '@', '~' -> 7;")
    A('                default -> c >= 0xE000 && c < 0xF900 ? 0 : 6;')
    A('            };')
    A('            w += a + (bold && a > 0 ? 1 : 0);')
    A('        }')
    A('        return w;')
    A('    }')
    A('')
    A('    /** Text centred on the TITLE_BAR glyph; total advance equals the bar so vanilla centring stays exact. */')
    A('    public static String onTitleBar(String text) {')
    A('        int w = textWidth(text);')
    A('        int barAdv = TITLE_BAR_WIDTH + 1;')
    A('        int back = barAdv - (TITLE_BAR_WIDTH - w) / 2;')
    A('        int tail = barAdv - ((TITLE_BAR_WIDTH - w) / 2 + w);')
    A('        return "\\u00a7f" + TITLE_BAR + space(-back) + text + space(tail);')
    A('    }')
    A('')
    A('    // ---- Custom model data (1.21.1 item model overrides) --------------------')
    for item, (_, ovs) in items.OVERRIDES.items():
        A(f'    // on minecraft:{item}')
        for cmd, m in sorted(ovs):
            A(f'    public static final int CMD_{m.upper()} = {cmd};')
    A('')
    A('    // ---- Sounds (play with Player#playSound(loc, KEY, SoundCategory.MASTER, 1f, 1f)) ----')
    fallback = {
        'countdown_tick': 'minecraft:block.note_block.hat', 'countdown_go': 'minecraft:block.note_block.bell',
        'point_gained': 'minecraft:entity.experience_orb.pickup', 'elimination': 'minecraft:entity.player.hurt',
        'team_eliminated': 'minecraft:entity.wither.death', 'round_win': 'minecraft:ui.toast.challenge_complete',
        'checkpoint': 'minecraft:block.note_block.chime', 'vote_cast': 'minecraft:ui.button.click',
        'vote_locked': 'minecraft:block.anvil.land', 'podium_fanfare': 'minecraft:ui.toast.challenge_complete'}
    for name in sounds.SOUNDS:
        A(f'    /** {sound_info.get(name, "?")} s */')
        A(f'    public static final String SOUND_{name.upper()} = "{NS}:{name}";')
    A('')
    A('    /** Vanilla sound to play instead when the player has not loaded the pack. */')
    A('    public static final Map<String, String> SOUND_FALLBACK = Map.ofEntries(')
    A(',\n'.join(f'            Map.entry(SOUND_{n.upper()}, "{fallback[n]}")' for n in sounds.SOUNDS) + ');')
    A('')
    A('    // ---- Pack state -------------------------------------------------------')
    A('    private static final Set<UUID> PACK_LOADED = ConcurrentHashMap.newKeySet();')
    A('')
    A('    /** Call from PlayerResourcePackStatusEvent (SUCCESSFULLY_LOADED -> true, DECLINED/FAILED -> false) and on quit (false). */')
    A('    public static void setHasPack(UUID player, boolean loaded) {')
    A('        if (loaded) PACK_LOADED.add(player); else PACK_LOADED.remove(player);')
    A('    }')
    A('')
    A('    public static boolean hasPack(UUID player) {')
    A('        return player != null && PACK_LOADED.contains(player);')
    A('    }')
    A('')
    A('    /** glyph if the player has the pack, otherwise the plain-text fallback. */')
    A('    public static String or(UUID player, String glyph, String fallback) {')
    A('        return hasPack(player) ? glyph : fallback;')
    A('    }')
    A('')
    A('    /** Sound key to use for this player (custom if pack loaded, vanilla fallback otherwise). */')
    A('    public static String sound(UUID player, String key) {')
    A('        return hasPack(player) ? key : SOUND_FALLBACK.getOrDefault(key, key);')
    A('    }')
    A('}')
    with open(os.path.join(ROOT, 'McmGlyphs.java'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(lines) + '\n')


# ============================================================== README table
def update_readme(reg, sound_info):
    path = os.path.join(ROOT, 'README.md')
    if not os.path.exists(path):
        return
    rows = ['| Constant | Codepoint | Size (px) | Ascent | Purpose |', '|---|---|---|---|---|']
    for g in reg:
        rows.append(f'| `{g.const}` | U+{g.cp:04X} | {g.img.width}x{g.img.height} | {g.ascent} | {g.desc} |')
    rows.append('| `NEG_1..NEG_256` | U+F801..U+F809 | space | - | -1,-2,-4 ... -256 px advance |')
    rows.append('| `POS_1..POS_256` | U+F821..U+F829 | space | - | +1,+2,+4 ... +256 px advance |')
    table = '\n'.join(rows)
    with open(path, encoding='utf-8') as f:
        txt = f.read()
    a, b = '<!-- GLYPHS:BEGIN -->', '<!-- GLYPHS:END -->'
    if a in txt and b in txt:
        txt = txt[:txt.index(a) + len(a)] + '\n' + table + '\n' + txt[txt.index(b):]
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(txt)


# ============================================================== preview
def _font(size):
    for name in ('consola.ttf', 'arial.ttf', 'DejaVuSans.ttf'):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            pass
    return ImageFont.load_default()


def preview(reg, item_tex, bossbar, packpng):
    W = 1500
    BG = (28, 29, 44, 255)
    sheet = Image.new('RGBA', (W, 4000), BG)
    d = ImageDraw.Draw(sheet)
    f_h, f_s = _font(22), _font(12)
    y = 16

    def checker(w, h, cell=8):
        c = Image.new('RGBA', (w, h))
        p = c.load()
        for yy in range(h):
            for xx in range(w):
                p[xx, yy] = (52, 54, 74, 255) if ((xx // cell) + (yy // cell)) % 2 else (44, 46, 64, 255)
        return c

    def header(text):
        nonlocal y
        d.text((16, y), text, font=f_h, fill=(255, 216, 74, 255))
        y += 32

    def grid(entries, k, cellw):
        nonlocal y
        x, rowh = 16, 0
        for label, img in entries:
            s = scale(img, k)
            w = max(cellw, s.width + 8)
            if x + w > W - 16:
                x, y = 16, y + rowh + 8
                rowh = 0
            bgc = checker(s.width + 8, s.height + 8)
            sheet.alpha_composite(bgc, (x, y))
            sheet.alpha_composite(s, (x + 4, y + 4))
            for li, part in enumerate(label.split(chr(10))):
                d.text((x, y + s.height + 12 + 14 * li), part, font=f_s, fill=(200, 205, 230, 255))
            rowh = max(rowh, s.height + 30 + 14 * label.count(chr(10)))
            x += w + 10
        y += rowh + 14

    d.text((16, y), 'BLOCK SHOWDOWN - resource pack contact sheet (all art generated by src/*.py)',
           font=f_h, fill=(255, 255, 255, 255))
    y += 40
    header('Brand')
    grid([(g.const, g.img) for g in reg if g.group == 'Brand'] + [('pack.png (1x)', packpng)], 4, 120)
    header('Team icons  (9px, x6)')
    grid([(g.const[5:], g.img) for g in reg if g.group == 'Teams'], 6, 80)
    header('HUD icons  (x6)')
    grid([(g.const, g.img) for g in reg if g.group == 'HUD'], 6, 80)
    header('Game tiles  (16px, x5)')
    grid([(g.desc, g.img) for g in reg if g.group == 'Games'], 5, 92)
    header('Items  (16px, x5)  - custom_model_data overrides')
    cmd = {m: (it, c) for it, (_, ovs) in items.OVERRIDES.items() for c, m in ovs}
    grid([(n + chr(10) + f'{cmd[n][0]} #{cmd[n][1]}', img) for n, img in item_tex.items()], 5, 120)
    header('Chest menu overlays (x2) and boss bar timer (x3)')
    grid([(f'{g.const} ({g.rows} rows)', g.img) for g in reg if g.group == 'GUI'], 2, 200)
    bb = Image.new('RGBA', (182, 5))
    bb.alpha_composite(bossbar[0])
    half = bossbar[1].crop((0, 0, 120, 5))
    bb.alpha_composite(half)
    grid([('boss_bar/white (timer at 66%)', bb)], 3, 200)

    # in-context mock: inline icons beside text-height bars at true pixel scale x4
    header('Inline alignment check (x4): grey bars = vanilla 7px cap height / baseline')
    line = Image.new('RGBA', (360, 20), (0, 0, 0, 0))
    lp = line.load()
    x = 2
    top = 6  # text line top
    for g in [g for g in reg if g.group in ('Teams', 'HUD')][:20] + \
             [g for g in reg if g.group == 'Games'][:3]:
        gy = top + 7 - g.ascent
        line.alpha_composite(g.img, (x, max(0, gy)))
        x += g.advance
        for yy in range(top, top + 7):
            for xx in range(x, x + 3):
                lp[xx, yy] = (170, 170, 190, 255)
        x += 4
    grid([('icons on a text line', line.crop((0, 0, x + 2, 20)))], 4, 200)

    sheet = sheet.crop((0, 0, W, y + 10))
    sheet.convert('RGB').save(os.path.join(ROOT, 'preview_sheet.png'), optimize=True)


# ============================================================== validate / zip
def validate(reg):
    problems = []
    for dp, _, fs in os.walk(PACK):
        for fn in fs:
            p = os.path.join(dp, fn)
            if fn.endswith('.json') or fn.endswith('.mcmeta'):
                try:
                    with open(p, encoding='utf-8') as f:
                        json.load(f)
                except Exception as e:  # noqa: BLE001
                    problems.append(f'{p}: {e}')

    def res(rl, kind, ext):
        ns, path = rl.split(':', 1)
        return os.path.join(PACK, 'assets', ns, kind, path + ext)

    with open(os.path.join(PACK, 'assets', 'minecraft', 'font', 'default.json')) as f:
        font = json.load(f)
    seen = set()
    for pr in font['providers']:
        if pr['type'] == 'bitmap':
            if pr['ascent'] > pr['height']:
                problems.append(f'ascent>height {pr["file"]}')
            fp = res(pr['file'], 'textures', '')
            if not os.path.exists(fp):
                problems.append('missing ' + fp)
            elif Image.open(fp).height != pr['height']:
                problems.append('height mismatch ' + fp)
            chars = pr['chars']
        else:
            chars = list(pr['advances'])
        for c in ''.join(chars):
            if c in seen:
                problems.append(f'duplicate char U+{ord(c):04X}')
            seen.add(c)
    mdir = os.path.join(PACK, 'assets', 'minecraft', 'models', 'item')
    for fn in os.listdir(mdir):
        with open(os.path.join(mdir, fn)) as f:
            m = json.load(f)
        vals = [o['predicate']['custom_model_data'] for o in m['overrides']]
        if vals != sorted(vals):
            problems.append('overrides unsorted ' + fn)
        for o in m['overrides']:
            mp = res(o['model'], 'models', '.json')
            if not os.path.exists(mp):
                problems.append('missing model ' + mp)
            else:
                with open(mp) as f:
                    tex = json.load(f)['textures']['layer0']
                if not os.path.exists(res(tex, 'textures', '.png')):
                    problems.append('missing texture ' + tex)
    with open(os.path.join(PACK, 'assets', NS, 'sounds.json')) as f:
        sj = json.load(f)
    for ev, v in sj.items():
        for s in v['sounds']:
            if not os.path.exists(res(s['name'], 'sounds', '.ogg')):
                problems.append('missing sound ' + s['name'])
    return problems


def make_zip():
    os.makedirs(DIST, exist_ok=True)
    zp = os.path.join(DIST, 'BlockShowdownPack.zip')
    files = []
    for dp, _, fs in os.walk(PACK):
        for fn in fs:
            full = os.path.join(dp, fn)
            files.append((os.path.relpath(full, PACK).replace(os.sep, '/'), full))
    files.sort()
    with zipfile.ZipFile(zp, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for arc, full in files:
            zi = zipfile.ZipInfo(arc, date_time=(2026, 1, 1, 0, 0, 0))
            zi.compress_type = zipfile.ZIP_DEFLATED
            zi.external_attr = 0o644 << 16
            with open(full, 'rb') as f:
                z.writestr(zi, f.read())
    h = hashlib.sha1(open(zp, 'rb').read()).hexdigest()
    with open(os.path.join(DIST, 'sha1.txt'), 'w', newline='\n') as f:
        f.write(h + '\n')
    return zp, os.path.getsize(zp), h, len(files)


def main():
    if os.path.exists(PACK):
        shutil.rmtree(PACK)
    os.makedirs(PACK)
    reg = build_registry()
    write_font(reg)
    write_mcc_compat(reg)
    packpng = write_meta()
    item_tex = write_items()
    bossbar = write_bossbar()
    try:
        sound_info = write_sounds()
    except Exception as e:  # noqa: BLE001
        print('!! sounds skipped:', e)
        sound_info = {}
    write_java(reg, sound_info)
    update_readme(reg, sound_info)
    preview(reg, item_tex, bossbar, packpng)
    problems = validate(reg)
    zp, size, sha, n = make_zip()
    print(f'glyphs: {len(reg)} bitmap + {2 * len(SPACES)} spaces')
    print(f'items: {len(item_tex)}  sounds: {sound_info}')
    print(f'zip: {zp}  {size} bytes  {n} files')
    print(f'sha1: {sha}')
    print('validation:', 'OK' if not problems else problems)
    return 0 if not problems else 1


if __name__ == '__main__':
    sys.exit(main())
