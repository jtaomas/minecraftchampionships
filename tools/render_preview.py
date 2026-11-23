#!/usr/bin/env python3
"""
render_preview.py - render MCMDUMP block dumps to isometric (or top-down) PNG previews.

Usage:
    python render_preview.py in.mcmdump.gz out.png [--angle 0..3] [--scale N] [--top]
                             [--no-ao] [--no-shadows] [--no-bloom] [--show-barriers]

Input (gzip text):  first line  "MCMDUMP 1 <sx> <sy> <sz>", then one block per line
                    "x y z namespace:id[states]"  (air omitted, coords relative to region min).
"""
import argparse
import colorsys
import gzip
import hashlib
import sys
import time

import numpy as np
from PIL import Image, ImageFilter

# =============================================================================
# Colour table
# =============================================================================
COLOURS = ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
           "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")


def _fam(*vals):
    return dict(zip(COLOURS, vals))


WOOL = _fam((234, 236, 237), (241, 118, 20), (190, 69, 180), (58, 175, 217), (249, 198, 40),
            (112, 185, 26), (238, 141, 172), (63, 68, 72), (142, 142, 135), (21, 138, 145),
            (122, 42, 173), (53, 57, 157), (114, 72, 41), (85, 110, 28), (161, 39, 35), (21, 21, 26))
CONCRETE = _fam((207, 213, 214), (224, 97, 1), (169, 48, 159), (36, 137, 199), (241, 175, 21),
                (94, 169, 25), (214, 101, 143), (55, 58, 62), (125, 125, 115), (21, 119, 136),
                (100, 32, 156), (45, 47, 143), (96, 60, 32), (73, 91, 36), (142, 33, 33), (8, 10, 15))
POWDER = _fam((226, 227, 228), (227, 132, 32), (193, 84, 185), (74, 181, 213), (233, 199, 55),
              (125, 189, 42), (229, 153, 181), (77, 81, 85), (155, 155, 148), (37, 148, 157),
              (132, 56, 178), (70, 73, 167), (126, 85, 54), (97, 119, 45), (168, 54, 51), (25, 27, 32))
TERRACOTTA = _fam((210, 178, 161), (162, 84, 38), (150, 88, 109), (113, 109, 138), (186, 133, 35),
                  (104, 118, 53), (162, 78, 79), (58, 42, 36), (135, 107, 98), (87, 91, 91),
                  (118, 70, 86), (74, 60, 91), (77, 51, 36), (76, 83, 42), (143, 61, 47), (37, 23, 16))
GLAZED = _fam((188, 212, 203), (155, 147, 92), (208, 100, 192), (95, 165, 209), (234, 192, 89),
              (163, 198, 55), (235, 154, 181), (83, 90, 94), (144, 166, 168), (52, 119, 125),
              (110, 48, 152), (47, 65, 139), (120, 106, 86), (117, 142, 67), (182, 60, 53), (68, 30, 32))
GLASS = _fam((250, 250, 250), (216, 127, 51), (178, 76, 216), (102, 153, 216), (229, 229, 51),
             (127, 204, 25), (242, 127, 165), (76, 76, 76), (153, 153, 153), (76, 127, 153),
             (127, 63, 178), (51, 76, 178), (102, 76, 51), (102, 127, 51), (153, 51, 51), (25, 25, 25))

# name -> (top_rgb, side_rgb, alpha, emit, solid, strip_rgb_or_None)
BLOCKS = {}


def _b(name, side, top=None, alpha=1.0, emit=0.0, solid=None, strip=None):
    if solid is None:
        solid = alpha >= 1.0
    BLOCKS[name] = (tuple(top or side), tuple(side), float(alpha), float(emit), bool(solid), strip)


for _c in COLOURS:
    _b(f"{_c}_wool", WOOL[_c])
    _b(f"{_c}_carpet", WOOL[_c], alpha=0.9, solid=False)
    _b(f"{_c}_bed", WOOL[_c], alpha=0.9, solid=False)
    _b(f"{_c}_banner", WOOL[_c], alpha=0.8, solid=False)
    _b(f"{_c}_shulker_box", WOOL[_c])
    _b(f"{_c}_candle", WOOL[_c], alpha=0.8, emit=0.4, solid=False)
    _b(f"{_c}_concrete", CONCRETE[_c])
    _b(f"{_c}_concrete_powder", POWDER[_c])
    _b(f"{_c}_terracotta", TERRACOTTA[_c])
    _b(f"{_c}_glazed_terracotta", GLAZED[_c])
    _b(f"{_c}_stained_glass", GLASS[_c], alpha=0.55)
    _b(f"{_c}_stained_glass_pane", GLASS[_c], alpha=0.45)
_b("terracotta", (152, 94, 68))
_b("glass", (205, 228, 235), alpha=0.28)
_b("glass_pane", (205, 228, 235), alpha=0.25)
_b("tinted_glass", (44, 38, 46), alpha=0.85)
_b("shulker_box", (139, 96, 139))
_b("candle", (230, 210, 160), alpha=0.8, emit=0.4, solid=False)

# --- stone family
for _n, _c in {
    "stone": (125, 125, 125), "cobblestone": (127, 127, 127), "mossy_cobblestone": (110, 118, 94),
    "stone_bricks": (122, 121, 122), "mossy_stone_bricks": (115, 121, 105),
    "cracked_stone_bricks": (118, 117, 118), "chiseled_stone_bricks": (119, 118, 119),
    "smooth_stone": (158, 158, 158), "granite": (149, 103, 85), "polished_granite": (154, 106, 89),
    "diorite": (188, 188, 188), "polished_diorite": (192, 193, 194), "andesite": (136, 136, 136),
    "polished_andesite": (132, 134, 133), "bricks": (150, 97, 83), "bedrock": (85, 85, 85),
    "deepslate": (80, 80, 82), "cobbled_deepslate": (77, 77, 80), "polished_deepslate": (72, 72, 73),
    "deepslate_bricks": (70, 70, 71), "cracked_deepslate_bricks": (64, 64, 65),
    "deepslate_tiles": (54, 54, 55), "cracked_deepslate_tiles": (52, 52, 52),
    "chiseled_deepslate": (54, 54, 54), "reinforced_deepslate": (80, 82, 78),
    "blackstone": (42, 36, 41), "polished_blackstone": (53, 48, 56),
    "polished_blackstone_bricks": (48, 42, 49), "cracked_polished_blackstone_bricks": (44, 37, 43),
    "chiseled_polished_blackstone": (53, 48, 56), "gilded_blackstone": (55, 42, 38),
    "quartz_block": (235, 229, 222), "smooth_quartz": (235, 229, 222),
    "chiseled_quartz_block": (231, 226, 218), "quartz_pillar": (235, 230, 224),
    "quartz_bricks": (234, 229, 221),
    "sandstone": (216, 203, 155), "chiseled_sandstone": (216, 202, 155),
    "cut_sandstone": (217, 206, 159), "smooth_sandstone": (223, 214, 170),
    "red_sandstone": (186, 99, 29), "chiseled_red_sandstone": (183, 96, 27),
    "cut_red_sandstone": (189, 101, 31), "smooth_red_sandstone": (181, 98, 31),
    "prismarine": (99, 156, 151), "prismarine_bricks": (99, 171, 158), "dark_prismarine": (51, 91, 75),
    "tuff": (108, 109, 102), "polished_tuff": (97, 104, 99), "tuff_bricks": (98, 102, 95),
    "chiseled_tuff": (89, 94, 87), "chiseled_tuff_bricks": (98, 102, 95),
    "calcite": (223, 224, 220), "dripstone_block": (134, 107, 92), "pointed_dripstone": (129, 102, 89),
    "basalt": (80, 81, 86), "polished_basalt": (99, 98, 100), "smooth_basalt": (72, 72, 78),
    "obsidian": (15, 10, 24), "netherrack": (97, 38, 38), "nether_bricks": (44, 21, 26),
    "cracked_nether_bricks": (40, 20, 23), "chiseled_nether_bricks": (47, 23, 28),
    "red_nether_bricks": (69, 7, 9), "nether_wart_block": (114, 2, 2), "warped_wart_block": (22, 119, 121),
    "soul_sand": (81, 62, 50), "soul_soil": (75, 57, 46), "end_stone": (219, 222, 158),
    "end_stone_bricks": (218, 224, 162), "purpur_block": (169, 125, 169), "purpur_pillar": (171, 129, 171),
    "mud": (60, 57, 60), "packed_mud": (142, 106, 79), "mud_bricks": (137, 103, 79),
    "clay": (160, 166, 179), "sponge": (195, 192, 74), "wet_sponge": (171, 181, 70),
    "hay_block": (166, 136, 38), "bone_block": (229, 225, 207), "dried_kelp_block": (50, 59, 39),
    "slime_block": (111, 192, 91), "honey_block": (251, 185, 52), "honeycomb_block": (229, 148, 29),
    "bookshelf": (117, 94, 59), "crafting_table": (129, 99, 61), "furnace": (110, 110, 110),
    "note_block": (88, 58, 40), "jukebox": (94, 64, 47), "tnt": (182, 60, 40), "target": (229, 176, 168),
    "sculk": (12, 29, 36), "sculk_catalyst": (16, 34, 40),
    "melon": (111, 145, 30), "pumpkin": (198, 118, 24), "carved_pumpkin": (198, 118, 24),
    "cactus": (85, 127, 43), "mushroom_stem": (203, 196, 185), "red_mushroom_block": (200, 46, 45),
    "brown_mushroom_block": (149, 111, 81), "chest": (162, 120, 50), "barrel": (134, 100, 58),
    "scaffolding": (175, 140, 75),
}.items():
    _b(_n, _c)
_b("quartz_pillar", (235, 230, 224), top=(232, 227, 218))
_b("purpur_pillar", (171, 129, 171), top=(172, 128, 172))
_b("basalt", (80, 81, 86), top=(88, 88, 91))
_b("bone_block", (229, 225, 207), top=(210, 206, 185))
_b("hay_block", (166, 136, 38), top=(165, 140, 35))
_b("pumpkin", (198, 118, 24), top=(196, 118, 24))
_b("melon", (111, 145, 30), top=(114, 146, 30))
_b("tnt", (182, 60, 40), top=(143, 62, 54))
_b("cactus", (85, 127, 43), top=(108, 155, 47), alpha=0.95, solid=True)

# --- metals / minerals / ores-as-blocks
for _n, _c in {
    "gold_block": (246, 208, 61), "iron_block": (220, 220, 220), "diamond_block": (98, 237, 228),
    "emerald_block": (42, 203, 87), "lapis_block": (31, 67, 140), "redstone_block": (175, 24, 5),
    "coal_block": (16, 15, 15), "netherite_block": (66, 61, 63), "raw_iron_block": (166, 135, 107),
    "raw_gold_block": (221, 169, 46), "raw_copper_block": (154, 105, 79),
    "amethyst_block": (133, 97, 191), "budding_amethyst": (132, 96, 186),
    "ancient_debris": (96, 64, 56),
}.items():
    _b(_n, _c)
_b("amethyst_cluster", (163, 126, 207), alpha=0.85, emit=0.3, solid=False)

# --- copper (waxed_ variants handled by the resolver)
for _stage, _c in (("", (192, 107, 79)), ("exposed_", (161, 125, 103)),
                   ("weathered_", (108, 153, 110)), ("oxidized_", (82, 162, 132))):
    _b(f"{_stage}copper_block" if _stage == "" else f"{_stage}copper", _c)
    _b(f"{_stage}cut_copper", tuple(max(0, v - 2) for v in _c))
    _b(f"{_stage}chiseled_copper", tuple(max(0, v - 8) for v in _c))
    _b(f"{_stage}copper_grate", _c, alpha=0.75)
    _b(f"{_stage}copper_bulb", tuple(min(255, v + 40) for v in _c), emit=0.7)
    _b(f"{_stage}copper_door", _c, alpha=0.85, solid=False)
    _b(f"{_stage}copper_trapdoor", _c, alpha=0.85, solid=False)
_b("copper_block", (192, 107, 79))

# --- woods
WOODS = {
    #         planks            log side          log top           stripped          leaves
    "oak":      ((162, 130, 78), (109, 85, 50), (151, 122, 72), (177, 144, 86), (72, 125, 38)),
    "spruce":   ((115, 85, 49), (59, 38, 17), (109, 80, 47), (116, 90, 52), (58, 94, 58)),
    "birch":    ((192, 175, 121), (217, 215, 210), (193, 179, 135), (197, 176, 118), (104, 136, 62)),
    "jungle":   ((160, 115, 81), (85, 68, 25), (150, 109, 71), (171, 133, 85), (52, 135, 25)),
    "acacia":   ((168, 90, 50), (103, 97, 87), (150, 92, 51), (175, 93, 60), (88, 128, 30)),
    "dark_oak": ((67, 43, 20), (60, 47, 26), (65, 44, 22), (72, 56, 36), (52, 98, 24)),
    "mangrove": ((117, 54, 48), (84, 67, 41), (110, 48, 42), (120, 54, 48), (86, 126, 36)),
    "cherry":   ((227, 179, 173), (55, 33, 44), (186, 135, 126), (216, 145, 149), (229, 173, 194)),
    "pale_oak": ((228, 218, 216), (88, 78, 75), (215, 205, 203), (246, 238, 237), (164, 168, 155)),
    "crimson":  ((101, 49, 71), (93, 26, 30), (112, 49, 70), (138, 58, 90), None),
    "warped":   ((43, 105, 99), (58, 59, 78), (53, 110, 110), (58, 151, 148), None),
}
for _w, (_pl, _ls, _lt, _st, _lv) in WOODS.items():
    _log = "stem" if _w in ("crimson", "warped") else "log"
    _wood = "hyphae" if _w in ("crimson", "warped") else "wood"
    _b(f"{_w}_planks", _pl)
    _b(f"{_w}_{_log}", _ls, top=_lt)
    _b(f"{_w}_{_wood}", _ls)
    _b(f"stripped_{_w}_{_log}", _st, top=_lt)
    _b(f"stripped_{_w}_{_wood}", _st)
    if _lv:
        _b(f"{_w}_leaves", _lv, alpha=0.85, solid=False)
_b("bamboo_planks", (194, 174, 82))
_b("bamboo_mosaic", (190, 170, 78))
_b("bamboo_block", (127, 144, 58), top=(139, 141, 62))
_b("stripped_bamboo_block", (184, 170, 79), top=(178, 158, 72))
_b("bamboo", (95, 143, 38), alpha=0.8, solid=False)
_b("azalea_leaves", (96, 122, 46), alpha=0.85, solid=False)
_b("flowering_azalea_leaves", (112, 118, 70), alpha=0.85, solid=False)
_b("azalea", (101, 124, 47), alpha=0.8, solid=False)
_b("flowering_azalea", (112, 121, 73), alpha=0.8, solid=False)

# --- terrain
_b("grass_block", (134, 96, 67), top=(95, 159, 53), strip=(95, 159, 53))
_b("dirt", (134, 96, 67))
_b("coarse_dirt", (119, 85, 59))
_b("rooted_dirt", (144, 103, 76))
_b("podzol", (134, 96, 67), top=(91, 63, 24), strip=(91, 63, 24))
_b("mycelium", (134, 96, 67), top=(111, 99, 105), strip=(111, 99, 105))
_b("dirt_path", (134, 96, 67), top=(148, 122, 65))
_b("farmland", (134, 96, 67), top=(110, 75, 46))
_b("sand", (219, 207, 163))
_b("red_sand", (190, 102, 33))
_b("suspicious_sand", (217, 204, 159))
_b("gravel", (131, 127, 126))
_b("suspicious_gravel", (129, 125, 123))
_b("snow_block", (249, 254, 254))
_b("snow", (249, 254, 254), alpha=0.95, solid=False)
_b("powder_snow", (248, 253, 253))
_b("ice", (145, 183, 253), alpha=0.6)
_b("packed_ice", (141, 180, 250))
_b("blue_ice", (116, 167, 253))
_b("frosted_ice", (140, 181, 252), alpha=0.6)
_b("water", (52, 108, 222), alpha=0.55)
_b("bubble_column", (52, 108, 222), alpha=0.55)
_b("lava", (212, 96, 18), emit=1.0)
_b("magma_block", (142, 63, 31), emit=0.5)
_b("moss_block", (89, 109, 45))
_b("moss_carpet", (89, 109, 45), alpha=0.95, solid=False)
_b("pale_moss_block", (107, 112, 104))
_b("pale_moss_carpet", (107, 112, 104), alpha=0.95, solid=False)
_b("crimson_nylium", (97, 38, 38), top=(130, 31, 31), strip=(130, 31, 31))
_b("warped_nylium", (97, 38, 38), top=(43, 114, 101), strip=(43, 114, 101))
_b("lily_pad", (32, 128, 48), alpha=0.9, solid=False)
_b("kelp", (60, 120, 40), alpha=0.7, solid=False)
_b("seagrass", (60, 130, 40), alpha=0.7, solid=False)
_b("vine", (60, 110, 30), alpha=0.7, solid=False)
_b("glow_lichen", (112, 131, 121), alpha=0.7, emit=0.4, solid=False)

# --- light sources (drawn brighter + bloom)
_b("glowstone", (214, 168, 100), emit=1.0)
_b("sea_lantern", (186, 214, 205), emit=1.0)
_b("ochre_froglight", (251, 245, 207), emit=1.0)
_b("verdant_froglight", (229, 244, 228), emit=1.0)
_b("pearlescent_froglight", (245, 240, 239), emit=1.0)
_b("shroomlight", (240, 146, 70), emit=1.0)
_b("end_rod", (230, 222, 212), alpha=0.85, emit=1.0, solid=False)
_b("lantern", (232, 178, 92), alpha=0.9, emit=1.0, solid=False)
_b("soul_lantern", (96, 196, 206), alpha=0.9, emit=1.0, solid=False)
_b("torch", (255, 205, 100), alpha=0.75, emit=1.0, solid=False)
_b("wall_torch", (255, 205, 100), alpha=0.75, emit=1.0, solid=False)
_b("soul_torch", (110, 210, 220), alpha=0.75, emit=1.0, solid=False)
_b("soul_wall_torch", (110, 210, 220), alpha=0.75, emit=1.0, solid=False)
_b("redstone_lamp", (175, 110, 60), emit=0.5)
_b("jack_o_lantern", (214, 150, 40), emit=0.8)
_b("beacon", (118, 221, 215), alpha=0.9, emit=1.0)
_b("sea_pickle", (110, 140, 60), alpha=0.8, emit=0.5, solid=False)
_b("campfire", (200, 130, 60), alpha=0.85, emit=0.8, solid=False)
_b("soul_campfire", (90, 190, 200), alpha=0.85, emit=0.8, solid=False)
_b("fire", (230, 140, 40), alpha=0.7, emit=1.0, solid=False)
_b("soul_fire", (60, 200, 210), alpha=0.7, emit=1.0, solid=False)
_b("crying_obsidian", (32, 10, 60), emit=0.3)
_b("respawn_anchor", (40, 20, 70), emit=0.4)
_b("conduit", (160, 140, 110), alpha=0.85, emit=0.8, solid=False)

# --- misc thin things
_b("iron_bars", (160, 162, 160), alpha=0.6, solid=False)
_b("chain", (60, 65, 75), alpha=0.6, solid=False)
_b("ladder", (125, 97, 55), alpha=0.75, solid=False)
_b("rail", (125, 110, 90), alpha=0.8, solid=False)
_b("cobweb", (228, 233, 234), alpha=0.5, solid=False)

# --- plants / flowers (non-occluding, a bit translucent)
for _n, _c in {
    "short_grass": (98, 152, 58), "grass": (98, 152, 58), "tall_grass": (98, 152, 58),
    "fern": (88, 138, 60), "large_fern": (88, 138, 60), "dead_bush": (124, 96, 52),
    "dandelion": (255, 230, 60), "poppy": (220, 40, 30), "blue_orchid": (47, 162, 210),
    "allium": (186, 125, 228), "azure_bluet": (222, 232, 232), "red_tulip": (214, 50, 30),
    "orange_tulip": (236, 124, 40), "white_tulip": (232, 242, 232), "pink_tulip": (236, 176, 210),
    "oxeye_daisy": (236, 236, 205), "cornflower": (70, 105, 228), "lily_of_the_valley": (240, 240, 240),
    "wither_rose": (40, 40, 30), "sunflower": (248, 205, 40), "lilac": (205, 150, 210),
    "rose_bush": (190, 40, 40), "peony": (232, 182, 222), "torchflower": (232, 132, 60),
    "pitcher_plant": (100, 140, 200), "pink_petals": (242, 172, 204), "spore_blossom": (210, 100, 170),
    "sweet_berry_bush": (60, 100, 50), "wheat": (190, 170, 70), "carrots": (90, 150, 40),
    "potatoes": (90, 150, 40), "beetroots": (90, 140, 40), "sugar_cane": (140, 190, 90),
    "red_mushroom": (210, 50, 45), "brown_mushroom": (160, 120, 90), "crimson_roots": (130, 20, 40),
    "warped_roots": (20, 150, 130), "nether_sprouts": (20, 150, 130), "oak_sapling": (80, 120, 40),
    "spruce_sapling": (50, 80, 50), "birch_sapling": (110, 140, 60), "cherry_sapling": (220, 160, 190),
    "hanging_roots": (160, 110, 90), "big_dripleaf": (90, 150, 40), "small_dripleaf": (90, 150, 40),
}.items():
    _b(_n, _c, alpha=0.75, solid=False)

INVISIBLE = {"air", "cave_air", "void_air", "barrier", "structure_void", "light", "moving_piston"}

PLANT_WORDS = ("flower", "tulip", "sapling", "bush", "orchid", "petals", "roots", "sprouts",
               "vines", "_grass", "fern", "seagrass", "kelp", "coral", "propagule", "lichen")
THIN_SUFFIXES = {  # suffix -> (alpha multiplier, solid)
    "_stairs": (1.0, True), "_slab": (1.0, True), "_wall": (1.0, False),
    "_fence_gate": (0.85, False), "_fence": (0.85, False), "_pane": (0.9, False),
    "_door": (0.9, False), "_trapdoor": (0.9, False), "_button": (0.8, False),
    "_pressure_plate": (0.8, False), "_wall_hanging_sign": (0.85, False), "_hanging_sign": (0.85, False),
    "_wall_sign": (0.85, False), "_sign": (0.85, False), "_carpet": (0.9, False), "_bars": (0.6, False),
    "_wall_banner": (0.8, False), "_banner": (0.8, False), "_rail": (0.8, False),
}

MATERIAL_WORDS = [  # substring -> rgb, checked in order
    ("dark_oak", WOODS["dark_oak"][0]), ("pale_oak", WOODS["pale_oak"][0]), ("oak", WOODS["oak"][0]),
    ("spruce", WOODS["spruce"][0]), ("birch", WOODS["birch"][0]), ("jungle", WOODS["jungle"][0]),
    ("acacia", WOODS["acacia"][0]), ("mangrove", WOODS["mangrove"][0]), ("cherry", WOODS["cherry"][0]),
    ("bamboo", (194, 174, 82)), ("crimson", WOODS["crimson"][0]), ("warped", WOODS["warped"][0]),
    ("deepslate", (70, 70, 72)), ("blackstone", (45, 40, 46)), ("cobblestone", (127, 127, 127)),
    ("sandstone", (216, 203, 155)), ("redstone", (175, 24, 5)), ("netherite", (66, 61, 63)),
    ("nether", (60, 22, 26)), ("end_stone", (219, 222, 158)), ("purpur", (169, 125, 169)),
    ("prismarine", (99, 156, 151)), ("quartz", (235, 229, 222)), ("copper", (192, 107, 79)),
    ("gold", (246, 208, 61)), ("iron", (200, 200, 200)), ("diamond", (98, 237, 228)),
    ("emerald", (42, 203, 87)), ("lapis", (31, 67, 140)), ("amethyst", (133, 97, 191)),
    ("obsidian", (15, 10, 24)), ("granite", (149, 103, 85)), ("diorite", (188, 188, 188)),
    ("andesite", (136, 136, 136)), ("tuff", (108, 109, 102)), ("basalt", (80, 81, 86)),
    ("brick", (150, 97, 83)), ("stone", (125, 125, 125)), ("glass", (205, 228, 235)),
    ("ice", (145, 183, 253)), ("snow", (249, 254, 254)), ("sand", (219, 207, 163)),
    ("gravel", (131, 127, 126)), ("mud", (60, 57, 60)), ("dirt", (134, 96, 67)),
    ("moss", (89, 109, 45)), ("leaves", (72, 125, 38)), ("grass", (95, 159, 53)),
    ("log", (109, 85, 50)), ("wood", (162, 130, 78)), ("planks", (162, 130, 78)),
    ("wool", (234, 236, 237)), ("concrete", (125, 125, 115)), ("terracotta", (152, 94, 68)),
    ("coral", (200, 90, 140)), ("mushroom", (160, 120, 90)), ("clay", (160, 166, 179)),
    ("sculk", (12, 29, 36)), ("lamp", (230, 190, 110)), ("lantern", (232, 178, 92)),
    ("glow", (230, 200, 120)), ("ore", (130, 128, 125)), ("coal", (30, 30, 30)),
]
LIGHT_WORDS = ("lamp", "lantern", "glow", "light", "torch", "fire", "beacon")


def _hash_colour(name):
    h = hashlib.md5(name.encode()).digest()
    hue = h[0] / 255.0
    sat = 0.40 + 0.25 * h[1] / 255.0
    val = 0.60 + 0.25 * h[2] / 255.0
    return tuple(int(v * 255) for v in colorsys.hsv_to_rgb(hue, sat, val))


def _colour_token(name):
    toks = name.split("_")
    for i, t in enumerate(toks):
        if t == "light" and i + 1 < len(toks) and toks[i + 1] in ("blue", "gray", "grey"):
            return "light_blue" if toks[i + 1] == "blue" else "light_gray"
        if t == "grey":
            return "gray"
        if t in WOOL:
            return t
    return None


def guess_block(name):
    """Stable fallback for ids not in the table: keyword colour, else hash."""
    alpha, solid, emit = 1.0, True, 0.0
    if any(w in name for w in PLANT_WORDS):
        alpha, solid = 0.75, False
    if any(w in name for w in LIGHT_WORDS):
        emit = 0.7
    if "glass" in name:
        alpha, solid = 0.45, False
    col = _colour_token(name)
    if col is not None:
        rgb = GLASS[col] if "glass" in name else CONCRETE[col] if "concrete" in name else WOOL[col]
    else:
        rgb = next((c for w, c in MATERIAL_WORDS if w in name), None) or _hash_colour(name)
    return (rgb, rgb, alpha, emit, solid, None)


def resolve_block(raw):
    """namespaced id (+ optional [states]) -> block spec, or None for invisible blocks."""
    name = raw.split("[", 1)[0].strip().lower()
    name = name.split(":", 1)[1] if ":" in name else name
    if name in INVISIBLE:
        return None
    if name in BLOCKS:
        return BLOCKS[name]
    for pre in ("waxed_", "infested_"):
        if name.startswith(pre) and name[len(pre):] in BLOCKS:
            return BLOCKS[name[len(pre):]]
    base = name[len("waxed_"):] if name.startswith("waxed_") else name
    for suf, (amul, solid) in THIN_SUFFIXES.items():
        if base.endswith(suf):
            b = base[: -len(suf)]
            cands = [b, b + "s", b + "_block", b + "_planks", b.replace("_brick", "_bricks"),
                     b.replace("_tile", "_tiles"), b.replace("smooth_quartz", "quartz_block"),
                     b + "_stained_glass" if suf == "_pane" else b]
            if suf == "_pane" and b.endswith("_stained_glass"):
                cands.insert(0, b)
            for c in cands:
                spec = BLOCKS.get(c)
                if spec:
                    t, s, a, e, so, _strip = spec
                    return (t, s, a * amul, e, solid and so, None)
            g = guess_block(b)
            return (g[0], g[1], g[2] * amul, g[3], solid and g[4], None)
    if name.startswith("potted_"):
        g = BLOCKS.get(name[7:]) or guess_block(name[7:])
        return ((150, 80, 60), (150, 80, 60), 0.85, g[3], False, None)
    if name.endswith("_ore"):
        host = (80, 80, 82) if "deepslate" in name else (97, 38, 38) if "nether" in name else (125, 125, 125)
        return (host, host, 1.0, 0.0, True, None)
    return guess_block(name)


# =============================================================================
# Loading
# =============================================================================
def load_dump(path):
    """Return (grid uint16 [X,Y,Z], names list where index 0 is air). Grid is trimmed to content."""
    opener = gzip.open if path.lower().endswith(".gz") else open
    with opener(path, "rb") as f:
        header = f.readline().split()
        if len(header) < 5 or header[0] != b"MCMDUMP":
            raise SystemExit(f"{path}: not an MCMDUMP file (header {header[:5]!r})")
        sx, sy, sz = (int(v) for v in header[2:5])
        raw_ids = {}
        xs_l, ys_l, zs_l, ids_l = [], [], [], []
        rest = b""
        while True:
            chunk = f.read(1 << 24)
            if not chunk and not rest:
                break
            data = rest + chunk
            if chunk:
                cut = data.rfind(b"\n")
                if cut < 0:
                    rest = data
                    continue
                data, rest = data[:cut], data[cut + 1:]
            else:
                rest = b""
            toks = data.split()
            if len(toks) % 4:   # unusual whitespace inside ids: fall back to per-line parse
                toks = []
                for ln in data.splitlines():
                    p = ln.split(None, 3)
                    if len(p) == 4:
                        toks.extend(p)
            if not toks:
                continue
            xs_l.append(np.array(toks[0::4], dtype=np.int32))
            ys_l.append(np.array(toks[1::4], dtype=np.int32))
            zs_l.append(np.array(toks[2::4], dtype=np.int32))
            sd = raw_ids.setdefault
            ids_l.append(np.fromiter((sd(n, len(raw_ids)) for n in toks[3::4]), dtype=np.int32,
                                     count=len(toks) // 4))
            if not chunk:
                break
    if not xs_l:
        return np.zeros((0, 0, 0), np.uint16), ["air"], (sx, sy, sz)
    xs, ys, zs, rid = (np.concatenate(a) for a in (xs_l, ys_l, zs_l, ids_l))

    # map raw ids (with states) -> canonical block names; invisible -> 0
    names = ["air"]
    canon = {}
    remap = np.zeros(len(raw_ids), np.uint16)
    for raw, i in raw_ids.items():
        r = raw.decode("utf-8", "replace")
        n = r.split("[", 1)[0].strip().lower()
        n = n if ":" in n else "minecraft:" + n
        if resolve_block(n) is None:
            continue
        if n not in canon:
            canon[n] = len(names)
            names.append(n)
        remap[i] = canon[n]
    bid = remap[rid]
    keep = (bid > 0) & (xs >= 0) & (ys >= 0) & (zs >= 0) & (xs < sx) & (ys < sy) & (zs < sz)
    xs, ys, zs, bid = xs[keep], ys[keep], zs[keep], bid[keep]
    if len(bid) == 0:
        return np.zeros((0, 0, 0), np.uint16), names, (sx, sy, sz)
    x0, y0, z0 = xs.min(), ys.min(), zs.min()
    grid = np.zeros((xs.max() - x0 + 1, ys.max() - y0 + 1, zs.max() - z0 + 1), np.uint16)
    grid[xs - x0, ys - y0, zs - z0] = bid
    return grid, names, (sx, sy, sz)


def build_luts(names):
    n = len(names)
    top = np.zeros((n, 3), np.float32)
    side = np.zeros((n, 3), np.float32)
    strip = np.zeros((n, 3), np.float32)
    has_strip = np.zeros(n, bool)
    alpha = np.zeros(n, np.float32)
    emit = np.zeros(n, np.float32)
    solid = np.zeros(n, bool)
    for i, nm in enumerate(names[1:], 1):
        t, s, a, e, so, st = resolve_block(nm)
        top[i], side[i], alpha[i], emit[i], solid[i] = t, s, a, e, so
        if st is not None:
            strip[i], has_strip[i] = st, True
    return dict(top=top / 255.0, side=side / 255.0, strip=strip / 255.0, has_strip=has_strip,
                alpha=alpha, emit=emit, solid=solid)


# =============================================================================
# Isometric renderer
# =============================================================================
SHADE = {"top": 1.0, "left": 0.80, "right": 0.62}
# face: normal, tangent for param a, tangent for param b (a/b = 1 at +tangent side)
FACE_AXES = {
    "top": ((0, 1, 0), (1, 0, 0), (0, 0, 1)),
    "left": ((0, 0, 1), (1, 0, 0), (0, -1, 0)),    # +z face, drawn on the left
    "right": ((1, 0, 0), (0, 0, -1), (0, -1, 0)),  # +x face, drawn on the right
}


def face_sprites(s):
    """Pixel masks of the three faces inside the 2s x 2s hexagon box, with face-local (a, b)."""
    c = np.arange(2 * s) + 0.5
    U, V = np.meshgrid(c, c)  # V = rows
    out = {}
    a = ((U - s) / s + 2 * V / s) / 2
    b = (2 * V / s - (U - s) / s) / 2
    out["top"] = (a, b)
    a = U / s
    out["left"] = (a, (V - s / 2 - a * s / 2) / s)
    a = (U - s) / s
    out["right"] = (a, (V - s + a * s / 2) / s)
    sprites = {}
    for f, (a, b) in out.items():
        m = (a >= 0) & (a <= 1) & (b >= 0) & (b <= 1)
        if f == "left":
            m &= U < s
        if f == "right":
            m &= U > s
        dv, du = np.nonzero(m)
        sprites[f] = (du, dv, a[m], b[m])
    return sprites


def _shift_or(S, C, y):
    """S[x, y, z] = C[x, y, z] | S[x-1, y+1, z+1]  (light coming from -x, +y, +z).
    The face-adjacent cells around each diagonal step are tested too, so 1-block
    walls do not leak light through their diagonal gaps."""
    S[:, y, :] = C[:, y, :]
    S[1:, y, :-1] |= C[:-1, y, 1:]
    S[1:, y, :] |= C[:-1, y + 1, :]
    S[:, y, :-1] |= C[:, y + 1, 1:]
    S[1:, y, :-1] |= S[:-1, y + 1, 1:]


def render_iso(grid, lut, s, ao=True, shadows=True, log=print):
    t0 = time.time()
    X, Y, Z = grid.shape
    P = np.zeros((X + 2, Y + 2, Z + 2), np.uint16)
    P[1:-1, 1:-1, 1:-1] = grid
    Yp, Zp = Y + 2, Z + 2
    strides = np.array([Yp * Zp, Zp, 1], np.int64)
    solid_g = lut["solid"][P]
    flat_solid = solid_g.ravel()

    if shadows:
        caster = (lut["alpha"] >= 0.6)[P]
        S = np.zeros_like(caster)
        S[:, Yp - 1, :] = caster[:, Yp - 1, :]
        for y in range(Yp - 2, -1, -1):
            _shift_or(S, caster, y)
        del caster
        flat_S = S.ravel()

    h = s // 2
    W = (X + Z) * s
    H = (X + Z - 2) * h + (Y - 1) * s + 2 * s
    off_u, off_v = (Z - 1) * s, (Y - 1) * s
    sprites = face_sprites(s)
    have_emit = bool(lut["emit"].max() > 0)   # names only contains block types present

    C = np.zeros((H * W, 3), np.float32)
    A = np.zeros(H * W, np.float32)
    E = np.zeros((H * W, 3), np.float32) if have_emit else None

    rng = np.random.default_rng(12345)
    tex = (1 + 0.075 * (rng.random((32, 64)) * 2 - 1)).astype(np.float32)
    use_tex = s >= 8
    edge_on = s >= 6

    inner = P[1:-1, 1:-1, 1:-1]
    faces = {}
    nb_slices = {"top": (slice(1, -1), slice(2, None), slice(1, -1)),
                 "left": (slice(1, -1), slice(1, -1), slice(2, None)),
                 "right": (slice(2, None), slice(1, -1), slice(1, -1))}
    nface = 0
    for f in ("top", "left", "right"):
        nb = P[nb_slices[f]]
        draw = (inner > 0) & ~solid_g[nb_slices[f]] & (nb != inner)
        x, y, z = (v.astype(np.int64) for v in np.nonzero(draw))
        del draw
        bid = inner[x, y, z]
        d = x + y + z
        order = np.argsort(d, kind="stable")
        x, y, z, bid, d = x[order], y[order], z[order], bid[order], d[order]
        nface += len(x)

        n_, t1, t2 = (np.array(v, np.int64) for v in FACE_AXES[f])
        pflat = (x + 1) * strides[0] + (y + 1) * strides[1] + (z + 1)
        nflat = pflat + n_ @ strides

        emit = lut["emit"][bid]
        alpha = lut["alpha"][bid]
        fac = np.full(len(x), SHADE[f], np.float32)
        if shadows and f != "right":
            lit = ~flat_S[nflat]
            fac *= np.where(lit, 1.0, 0.70).astype(np.float32)
        hs = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791)
        fac *= (1 + 0.035 * ((hs % 1001) / 500.0 - 1)).astype(np.float32)
        # emissive: ignore shading/shadow, glow a little
        fac = fac * (1 - emit) + emit * 1.18
        base_lut = lut["top"] if f == "top" else lut["side"]
        col = base_lut[bid] * fac[:, None]
        strip_col = has_strip = None
        if f != "top":
            has_strip = lut["has_strip"][bid]
            if has_strip.any():
                strip_col = lut["strip"][bid] * fac[:, None]

        # per-vertex AO
        if ao:
            aov = np.empty((len(x), 4), np.float32)
            for k, (i, j) in enumerate(((0, 0), (1, 0), (0, 1), (1, 1))):
                o1 = (2 * i - 1) * (t1 @ strides)
                o2 = (2 * j - 1) * (t2 @ strides)
                s1 = flat_solid[nflat + o1]
                s2 = flat_solid[nflat + o2]
                cc = flat_solid[nflat + o1 + o2]
                occ = np.where(s1 & s2, 3, s1.astype(np.int8) + s2 + cc)
                aov[:, k] = 1 - 0.11 * occ
            aov = aov * (1 - emit[:, None]) + emit[:, None]
        else:
            aov = None

        u0 = (x - z) * s + off_u
        v0 = (x + z) * h - y * s + off_v
        du, dv, pa, pb = sprites[f]
        wts = np.stack([(1 - pa) * (1 - pb), pa * (1 - pb), (1 - pa) * pb, pa * pb], 1).astype(np.float32)
        pix = np.ones(len(du), np.float32)
        if edge_on:
            e = np.minimum(np.minimum(pa, 1 - pa), np.minimum(pb, 1 - pb)) * s
            pix = np.where(e < 0.75, 0.88, 1.0).astype(np.float32)
        texk = (np.clip((pa * 8).astype(int), 0, 7) * 8 + np.clip((pb * 8).astype(int), 0, 7))
        strip_pix = (pb < 0.2) if f != "top" else None
        faces[f] = dict(d=d, base=v0 * W + u0, col=col, alpha=alpha, emit=emit, aov=aov,
                        pid=(hs % 32), strip_col=strip_col, has_strip=has_strip,
                        offs=dv * W + du, wts=wts, pix=pix, texk=texk, strip_pix=strip_pix)
    del solid_g, flat_solid
    if shadows:
        del S, flat_S
    t1_ = time.time()

    dmin = min((fd["d"][0] for fd in faces.values() if len(fd["d"])), default=0)
    dmax = max((fd["d"][-1] for fd in faces.values() if len(fd["d"])), default=-1)
    bounds = {f: np.searchsorted(fd["d"], np.arange(dmin, dmax + 2)) for f, fd in faces.items()}
    for k in range(dmax - dmin + 1):
        for f, fd in faces.items():
            lo, hi = bounds[f][k], bounds[f][k + 1]
            if lo == hi:
                continue
            m = np.broadcast_to(fd["pix"], (hi - lo, len(fd["pix"])))
            if fd["aov"] is not None:
                m = (fd["aov"][lo:hi] @ fd["wts"].T) * fd["pix"]
            if use_tex:
                m = m * tex[fd["pid"][lo:hi, None], fd["texk"][None, :]]
            col = fd["col"][lo:hi, None, :] * m[..., None]
            if fd["strip_col"] is not None:
                hs_ = fd["has_strip"][lo:hi]
                if hs_.any():
                    sel = hs_[:, None] & fd["strip_pix"][None, :]
                    col = np.where(sel[..., None], fd["strip_col"][lo:hi, None, :] * m[..., None], col)
            idx = (fd["base"][lo:hi, None] + fd["offs"][None, :]).ravel()
            col = col.reshape(-1, 3)
            a = fd["alpha"][lo:hi]
            em = fd["emit"][lo:hi]
            if a.min() >= 1.0:
                C[idx] = col
                A[idx] = 1.0
                if E is not None:
                    E[idx] = col * np.repeat(em, len(fd["offs"]))[:, None]
            else:
                ap = np.repeat(a, len(fd["offs"]))
                C[idx] = col * ap[:, None] + C[idx] * (1 - ap[:, None])
                A[idx] = ap + A[idx] * (1 - ap)
                if E is not None:
                    E[idx] = (col * (np.repeat(em, len(fd["offs"])) * ap)[:, None]
                              + E[idx] * (1 - ap[:, None]))
    log(f"  faces: {nface:,}  canvas {W}x{H}  (prep {t1_ - t0:.2f}s, draw {time.time() - t1_:.2f}s)")
    C = C.reshape(H, W, 3)
    A = A.reshape(H, W)
    E = E.reshape(H, W, 3) if E is not None else None
    return C, A, E


# =============================================================================
# Top-down renderer
# =============================================================================
def render_top(grid, lut, s):
    X, Y, Z = grid.shape
    C = np.zeros((X, Z, 3), np.float32)
    A = np.zeros((X, Z), np.float32)
    E = np.zeros((X, Z, 3), np.float32)
    Hm = np.full((X, Z), -1, np.int32)
    for y in range(Y):
        ids = grid[:, y, :]
        pres = ids > 0
        if not pres.any():
            continue
        a = lut["alpha"][ids]
        em = lut["emit"][ids]
        f = (0.74 + 0.36 * y / max(Y - 1, 1)) * (1 - em) + 1.15 * em
        col = lut["top"][ids] * f[..., None]
        op = pres & (a >= 1.0)
        tr = pres & ~op
        C[op] = col[op]
        A[op] = 1.0
        E[op] = col[op] * em[op, None]
        if tr.any():
            at = a[tr][:, None]
            C[tr] = col[tr] * at + C[tr] * (1 - at)
            E[tr] = col[tr] * em[tr, None] * at + E[tr] * (1 - at)
            A[tr] = a[tr] + A[tr] * (1 - a[tr])
        Hm[pres & (a >= 0.5)] = y
    # hillshade (light from north-west)
    nw = np.full_like(Hm, -1)
    nw[1:, 1:] = Hm[:-1, :-1]
    dh = np.where((Hm >= 0) & (nw >= 0), Hm - nw, 0)
    shade = (1 + 0.07 * np.clip(dh, -3, 3)).astype(np.float32)
    C *= shade[..., None]
    # to image orientation: rows = z, cols = x
    C, A, E, Hm = (np.swapaxes(v, 0, 1) for v in (C, A, E, Hm))
    if s > 1:
        rep = lambda v: np.repeat(np.repeat(v, s, 0), s, 1)
        C, A, E = rep(C), rep(A), rep(E)
        if s >= 4:
            lw = max(1, s // 8)
            Hs = rep(Hm)
            dark = np.zeros(Hs.shape, bool)
            dz = Hm[1:, :] != Hm[:-1, :]
            dx = Hm[:, 1:] != Hm[:, :-1]
            for k in range(lw):
                rows = np.nonzero(dz)
                dark[(rows[0] + 1) * s + k - lw // 2, rows[1] * s + np.arange(s)[:, None]] = True
                cols = np.nonzero(dx)
                dark[cols[0] * s + np.arange(s)[:, None], (cols[1] + 1) * s + k - lw // 2] = True
            C[dark] *= 0.72
    return C, A, (E if E.any() else None)


# =============================================================================
# Compositing
# =============================================================================
def compose(C, A, E, blur_px, auto_width=False):
    ys, xs = np.nonzero(A > 0.004)
    if len(ys) == 0:
        img = np.zeros((600, 800, 3), np.float32)
        A = np.zeros((600, 800), np.float32)
        C = img
    else:
        y0, y1, x0, x1 = ys.min(), ys.max() + 1, xs.min(), xs.max() + 1
        C, A = C[y0:y1, x0:x1], A[y0:y1, x0:x1]
        E = E[y0:y1, x0:x1] if E is not None else None
        m = int(max(24, 0.05 * max(C.shape[:2])))
        C = np.pad(C, ((m, m), (m, m), (0, 0)))
        A = np.pad(A, ((m, m), (m, m)))
        E = np.pad(E, ((m, m), (m, m), (0, 0))) if E is not None else None
    H, W = A.shape
    # sky gradient
    t = np.linspace(0, 1, H, dtype=np.float32)[:, None, None]
    top_c = np.array([104, 160, 226], np.float32) / 255
    bot_c = np.array([222, 236, 248], np.float32) / 255
    bg = np.broadcast_to(top_c * (1 - t) + bot_c * t, (H, W, 3)).copy()
    # soft drop shadow
    if A.any():
        am = Image.fromarray((A * 255).astype(np.uint8))
        r = max(4, int(0.012 * max(H, W)))
        sh = np.asarray(am.filter(ImageFilter.GaussianBlur(r * 2)), np.float32) / 255
        off = int(r * 1.5)
        sh = np.pad(sh, ((off, 0), (0, 0)))[:H]
        bg *= (1 - 0.22 * sh)[..., None]
    out = C + bg * (1 - A[..., None])
    if E is not None and E.any():
        ei = Image.fromarray((np.clip(E, 0, 1) * 255).astype(np.uint8))
        glow = np.asarray(ei.filter(ImageFilter.GaussianBlur(max(2, blur_px))), np.float32) / 255
        glow2 = np.asarray(ei.filter(ImageFilter.GaussianBlur(max(4, blur_px * 3))), np.float32) / 255
        out = out + 0.55 * glow + 0.45 * glow2
    img = Image.fromarray((np.clip(out, 0, 1) * 255 + 0.5).astype(np.uint8), "RGB")
    if auto_width and not 1600 <= W <= 2400:   # automatic scale: aim for a ~2000 px wide image
        img = img.resize((2000, max(1, round(H * 2000.0 / W))), Image.LANCZOS)
    return img


# =============================================================================
# CLI
# =============================================================================
def main(argv=None):
    ap = argparse.ArgumentParser(description="Render an MCMDUMP block dump to a PNG preview.")
    ap.add_argument("input", help="input .mcmdump.gz (or plain text)")
    ap.add_argument("output", help="output .png")
    ap.add_argument("--angle", type=int, default=0, choices=range(4),
                    help="rotate the view by 90 degree steps (0..3)")
    ap.add_argument("--scale", type=int, default=None,
                    help="pixels per block: iso = half tile width (even, >=2); top = block size")
    ap.add_argument("--top", action="store_true", help="orthographic top-down map instead of isometric")
    ap.add_argument("--no-ao", action="store_true", help="disable ambient occlusion")
    ap.add_argument("--no-shadows", action="store_true", help="disable cast shadows")
    ap.add_argument("--no-bloom", action="store_true", help="disable glow around light blocks")
    ap.add_argument("--show-barriers", action="store_true", help="draw barrier blocks (light red, translucent)")
    args = ap.parse_args(argv)

    if args.show_barriers:
        INVISIBLE.discard("barrier")
        _b("barrier", (230, 80, 80), alpha=0.35)

    t0 = time.time()
    grid, names, region = load_dump(args.input)
    t_load = time.time() - t0
    nblocks = int(np.count_nonzero(grid)) if grid.size else 0
    print(f"loaded {args.input}: region {region[0]}x{region[1]}x{region[2]}, {nblocks:,} visible blocks, "
          f"{len(names) - 1} block types, content {'x'.join(map(str, grid.shape))} ({t_load:.2f}s)")
    if args.angle and grid.size:
        grid = np.ascontiguousarray(np.rot90(grid, args.angle, axes=(0, 2)))
    lut = build_luts(names)

    t1 = time.time()
    if grid.size == 0:
        img = compose(np.zeros((10, 10, 3), np.float32), np.zeros((10, 10), np.float32), None, 2)
    elif args.top:
        X, _, Z = grid.shape
        s = args.scale or max(1, round(2000 / max(X, Z)))
        C, A, E = render_top(grid, lut, s)
        img = compose(C, A, None if args.no_bloom else E, max(2, s // 2), auto_width=not args.scale)
    else:
        X, Y, Z = grid.shape
        if args.scale:
            s = max(2, args.scale + (args.scale & 1))
        else:
            target = 2000.0 / (X + Z)                 # ideal half tile width
            s = max(2, 2 * int(np.ceil(target)))      # ~2x supersampling, always even
            while s > 2 and (X + Z) * s * ((X + Z) / 2 + Y) * s > 16e6:
                s -= 2
        C, A, E = render_iso(grid, lut, s, ao=not args.no_ao, shadows=not args.no_shadows)
        img = compose(C, A, None if args.no_bloom else E, max(2, s // 2), auto_width=not args.scale)
    img.save(args.output, optimize=False)
    print(f"wrote {args.output} {img.size[0]}x{img.size[1]}  render {time.time() - t1:.2f}s, "
          f"total {time.time() - t0:.2f}s")


if __name__ == "__main__":
    main()
