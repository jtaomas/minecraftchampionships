#!/usr/bin/env python3
"""Write data/blocks.csv (base blocks) and data/block_states.csv (every state)."""
import csv
import json
from collections import Counter

scan = json.load(open("data/scan.json", encoding="utf-8"))
states = scan["block_states"]
base = scan["base_blocks"]
total = sum(base.values())

FAMILY = {
    "water": "water", "bubble_column": "water",
    "snow_block": "snow/ice", "snow": "snow/ice", "packed_ice": "snow/ice", "ice": "snow/ice",
    "stone": "stone", "andesite": "stone", "cobblestone": "stone", "stone_bricks": "stone",
    "smooth_quartz": "quartz", "quartz_bricks": "quartz", "quartz_pillar": "quartz", "quartz_block": "quartz",
    "chiseled_quartz_block": "quartz", "smooth_quartz_stairs": "quartz", "smooth_quartz_slab": "quartz",
    "gray_concrete": "concrete", "black_concrete": "concrete", "white_concrete": "concrete",
    "red_concrete": "concrete", "cyan_concrete": "concrete", "yellow_concrete": "concrete",
    "blue_concrete": "concrete", "lime_concrete": "concrete", "purple_concrete": "concrete",
    "light_gray_concrete": "concrete", "light_blue_concrete": "concrete", "red_concrete_powder": "concrete",
    "mangrove_planks": "wood", "spruce_planks": "wood", "stripped_spruce_wood": "wood",
    "stripped_oak_wood": "wood", "dark_oak_planks": "wood", "spruce_slab": "wood", "mangrove_slab": "wood",
    "spruce_stairs": "wood", "mangrove_stairs": "wood", "spruce_fence": "wood", "spruce_door": "wood",
    "spruce_trapdoor": "wood", "mangrove_trapdoor": "wood", "spruce_leaves": "wood", "oak_log": "wood",
    "acacia_stairs": "wood", "acacia_slab": "wood", "acacia_trapdoor": "wood", "warped_planks": "wood",
    "stripped_warped_hyphae": "wood", "bookshelf": "wood", "ladder": "wood",
    "yellow_glazed_terracotta": "pad", "pink_glazed_terracotta": "pad", "magenta_glazed_terracotta": "pad",
    "red_glazed_terracotta": "pad", "yellow_carpet": "pad", "lime_wool": "pad",
    "light_gray_wool": "wool", "gray_wool": "wool", "white_wool": "wool", "black_wool": "wool",
    "red_wool": "wool", "blue_wool": "wool", "cyan_wool": "wool", "orange_wool": "wool",
    "yellow_wool": "wool", "green_terracotta": "terracotta", "yellow_terracotta": "terracotta",
    "cyan_terracotta": "terracotta", "terracotta": "terracotta", "light_gray_terracotta": "terracotta",
    "lodestone": "gameplay", "barrier": "gameplay", "netherite_block": "accent",
    "command_block": "command", "repeating_command_block": "command", "chain_command_block": "command",
    "redstone_wire": "redstone", "repeater": "redstone", "lever": "redstone",
    "redstone_block": "redstone", "polished_blackstone_button": "redstone",
    "sea_lantern": "light", "lantern": "light", "campfire": "light",
    "iron_trapdoor": "detail", "iron_bars": "detail", "iron_block": "detail", "iron_door": "detail",
    "light_blue_stained_glass": "glass", "cyan_stained_glass": "glass",
    "light_blue_stained_glass_pane": "glass", "gray_stained_glass_pane": "glass",
    "black_stained_glass_pane": "glass", "white_stained_glass_pane": "glass",
    "light_gray_shulker_box": "container", "hopper": "container",
    "dirt": "ground", "grass": "ground", "podzol": "ground", "coarse_dirt": "ground", "dirt_path": "ground",
    "tuff": "stone", "calcite": "stone", "polished_deepslate": "stone", "deepslate_bricks": "stone",
    "polished_blackstone_slab": "stone", "polished_blackstone_bricks": "stone",
    "polished_blackstone_wall": "stone", "stone_slab": "stone", "stone_brick_slab": "stone",
    "stone_brick_stairs": "stone", "andesite_wall": "stone", "andesite_stairs": "stone",
    "diorite_wall": "stone", "granite_slab": "stone", "granite": "stone",
    "mud_bricks": "stone", "nether_bricks": "stone", "soul_sand": "ground", "cut_red_sandstone": "stone",
    "cut_red_sandstone_slab": "stone", "smooth_quartz": "quartz",
}

USE = {
    "water": "waterslides, waterfalls, boat basin, bubble elevator",
    "bubble_column": "vertical elevator x-129..-127 z29..32 (y4-27)",
    "snow_block": "snow platforms, north chain, roofs",
    "snow": "snow layer detail",
    "packed_ice": "main ice track/runway surface",
    "ice": "ice patches on the north chain",
    "stone": "track base, island undersides, plaza",
    "andesite": "track edging and cave path",
    "cobblestone": "island rims and cave",
    "stone_bricks": "arch/tunnel and station base",
    "smooth_quartz": "chalet/station trim and stair",
    "quartz_bricks": "chalet detail",
    "quartz_pillar": "chalet stair columns",
    "gray_concrete": "start platform, village path, plaza",
    "black_concrete": "train engine body, lodge roof beams",
    "white_concrete": "train stripe, platform edges",
    "red_concrete": "train cab body/accent",
    "yellow_glazed_terracotta": "Speed IV (2 s) pad + global spawn checkpoint",
    "pink_glazed_terracotta": "jump pad corner (Speed VIII/IX + Jump Boost V)",
    "magenta_glazed_terracotta": "jump pad centre (Speed VIII/IX + Jump Boost V)",
    "red_glazed_terracotta": "jump pad edge (Speed VIII/IX + Jump Boost V)",
    "yellow_carpet": "Speed IV (2 s) pad",
    "lime_wool": "Jump Boost VII/VIII pad",
    "lodestone": "elytra equip pad (station, lodge, start lanes)",
    "barrier": "start gate wall (removed by fill at GO)",
    "netherite_block": "station/lodge accent blocks",
    "command_block": "countdown impulse blocks",
    "repeating_command_block": "always-on pad/kit logic",
    "chain_command_block": "countdown chain (title/gamemode)",
    "redstone_wire": "countdown circuit",
    "repeater": "countdown delay chain",
    "lever": "station control lever (powered=true)",
    "sea_lantern": "north ice bridge lights",
    "lantern": "cave/chalet/bridge accent lights",
    "campfire": "cave/chalet accent light and smoke",
    "light_gray_shulker_box": "empty start kit boxes",
    "hopper": "empty station control hopper",
    "spruce_planks": "station platform, roofs, tunnel",
    "mangrove_planks": "village houses and roofs",
    "stripped_spruce_wood": "train/station trim",
    "spruce_slab": "roof and platform slabs",
    "mangrove_slab": "village roof slabs",
    "spruce_stairs": "village/station stairs",
    "mangrove_stairs": "village stairs",
    "spruce_fence": "village/station fences",
    "spruce_door": "village/station doors",
    "spruce_trapdoor": "village/station shutters",
    "mangrove_trapdoor": "village shutters",
    "acacia_stairs": "village detail",
    "acacia_slab": "village detail",
    "acacia_trapdoor": "village detail",
    "warped_planks": "station interior",
    "stripped_warped_hyphae": "station interior",
    "bookshelf": "village interior",
    "ladder": "chalet/station access",
    "spruce_leaves": "village trees",
    "smooth_quartz_stairs": "chalet/station stair",
    "stone_brick_slab": "arch/station trim",
    "stone_brick_stairs": "arch/station stairs",
    "polished_blackstone_slab": "arch/station detail",
    "polished_blackstone_bricks": "arch/station detail",
    "polished_blackstone_wall": "arch/station detail",
    "deepslate_bricks": "station base",
    "light_gray_wool": "pale track edging in the ice cave / lodge / pool rim",
    "cyan_concrete": "station/ice-cave cyan accents",
    "blue_concrete": "north-tip marker posts; village markers",
    "lime_concrete": "start-gate posts",
    "yellow_wool": "train/start accent",
    "black_wool": "train/start accent",
    "blue_wool": "train/start accent",
    "cyan_wool": "pool/ice accent",
    "purple_concrete": "village marker",
    "light_blue_concrete": "train window",
    "red_concrete_powder": "village path accent",
    "iron_bars": "train windows/rails",
    "iron_door": "station/village doors",
    "glass": "train windows",
    "iron_trapdoor": "station/start grates and engine detail",
    "grass": "village ground patches",
    "dirt": "village ground",
    "podzol": "village ground",
    "cut_red_sandstone": "chalet/roof accent",
    "mud_bricks": "village house walls",
    "nether_bricks": "chalet fireplace/chimney",
    "soul_sand": "campfire base",
    "terracotta": "village roofs",
    "green_terracotta": "village roofs",
    "yellow_terracotta": "village roofs",
}

rows = []
for b, n in sorted(base.items(), key=lambda kv: -kv[1]):
    key = b.replace("minecraft:", "")
    short = key
    fam = FAMILY.get(short, "detail")
    use = USE.get(short, "detail / decorative")
    rows.append({
        "block": key, "count": n, "share_pct": round(n / total * 100, 3),
        "family": fam, "use_in_map": use,
    })
with open("data/blocks.csv", "w", newline="", encoding="utf-8") as fh:
    w = csv.DictWriter(fh, fieldnames=list(rows[0].keys()))
    w.writeheader(); w.writerows(rows)

srows = []
for s, n in sorted(states.items(), key=lambda kv: -kv[1]):
    srows.append({"state": s, "count": n, "share_pct": round(n / total * 100, 3)})
with open("data/block_states.csv", "w", newline="", encoding="utf-8") as fh:
    w = csv.DictWriter(fh, fieldnames=list(srows[0].keys()))
    w.writeheader(); w.writerows(srows)

print(f"base blocks {len(rows)}, states {len(srows)}, total {total}")
