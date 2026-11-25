#!/usr/bin/env python3
"""Build data/asset_map.csv with own-map usage separated from other maps.

own_count  = placements in the world that actually ships/uses this pack
other_maps = placements elsewhere (those worlds use vanilla textures, not this pack)
"""
import csv
import os

HERE = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs"
DATA = os.path.join(HERE, "data")
ALL = ["clouds2", "yeti_set_go", "space_race", "city_marathon", "tgttos_badlands", "tgttos_industry", "tgttos_pits"]
SHORT = {"clouds2": "clouds2", "yeti_set_go": "yeti", "space_race": "space", "city_marathon": "city",
         "tgttos_badlands": "badlands", "tgttos_industry": "industry", "tgttos_pits": "pits"}
OWN = {"clouds2": "clouds2", "yeti_set_go": "yeti_set_go", "space_race": "space_race"}

usage = {}
for r in csv.DictReader(open(os.path.join(DATA, "block_usage.csv"), encoding="utf-8")):
    usage[(r["block"], r["map"])] = int(r["count"])


def own(block, pack):
    return usage.get((block, OWN[pack]), 0)


def others(block, pack):
    parts = [(SHORT[m], usage.get((block, m), 0)) for m in ALL if m != OWN[pack]]
    return "; ".join(f"{m}:{c}" for m, c in parts if c) or "none"


ROWS = []


def add(pack, asset, kind, target, effect, trigger, status, conf, notes=""):
    block_key = target
    if target.startswith("minecraft:block/"):
        block_key = "minecraft:" + target.split("/", 1)[1]
    o = own(block_key, pack) if block_key.startswith("minecraft:") else ""
    ROWS.append({"pack": pack, "asset": asset, "asset_type": kind, "target": target, "effect": effect,
                 "own_map_placements": o,
                 "other_maps_same_block": others(block_key, pack) if block_key.startswith("minecraft:") else "",
                 "trigger": trigger, "status": status, "confidence": conf, "notes": notes})


# ---- shared Clouds 2 / Yeti block art ----
shared = [
    ("magenta_glazed_terracotta", "Pulsing concentric-square pad (7 frames, frametime 2)",
     "Pad block; commands test it and grant speed + jump boost, play block.bell.use"),
    ("pink_glazed_terracotta", "Rotating corner-spiral pad (7 frames, frametime 2)",
     "Pad block; speed + jump boost, block.bell.use"),
    ("red_glazed_terracotta", "Scrolling stripe pad (7 frames, frametime 2)",
     "Pad block; speed + jump boost, block.bell.use"),
    ("yellow_glazed_terracotta", "Upward chevron flow (16 frames, frametime 1)",
     "Pad block; speed + trident grant, block.chain.break"),
    ("purple_glazed_terracotta", "Corner-spiral pad variant (7 frames, frametime 2)",
     "No command tests purple; zero placed in Clouds 2 and Yeti"),
    ("glowstone", "Flat white light texture", "Zero glowstone placed in Clouds 2 or Yeti"),
    ("sea_lantern", "White rounded light, 5 frames, frametime 5", "Lighting block in track and hubs"),
    ("diorite", "Grey speckled stone", "Track/building surface"),
    ("oak_planks", "Pale beige planks", "Building wood"),
    ("red_nether_bricks", "Flat red brick courses", "Red accent material"),
    ("red_stained_glass", "Red patterned translucent glass", "Red glass accent"),
    ("soul_sand", "Dark brown noise", "Dark accent / water-section floor"),
]
YETI_HAS = {"magenta_glazed_terracotta", "pink_glazed_terracotta", "red_glazed_terracotta",
            "yellow_glazed_terracotta", "purple_glazed_terracotta", "glowstone"}
for slug, effect, trigger in shared:
    for pack in ("clouds2", "yeti_set_go"):
        if pack == "yeti_set_go" and slug not in YETI_HAS:
            continue
        o = own(f"minecraft:{slug}", pack)
        status = "used" if o else "unused"
        add(pack, f"assets/minecraft/textures/block/{slug}.png", "texture", f"minecraft:{slug}",
            effect, trigger, status, "H",
            "byte-identical in both packs" if pack == "yeti_set_go" else "")

# 16 terracotta colours (Clouds 2 only)
TERRA = ["black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray",
         "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow"]
for c in TERRA:
    block = f"minecraft:{c}_terracotta"
    o = own(block, "clouds2")
    add("clouds2", f"assets/minecraft/textures/block/{c}_terracotta.png", "texture", block,
        f"Flat team-colour tint ({c}) with faint horizontal banding",
        "Placed building/team-colour block", "used" if o else "unused", "H",
        "only Clouds 2 ships the 16-colour flat set")

# models
for c in ["magenta", "pink", "purple", "red", "yellow"]:
    o = own(f"minecraft:{c}_glazed_terracotta", "clouds2")
    add("clouds2", f"assets/minecraft/models/block/{c}_glazed_terracotta.json", "model",
        f"minecraft:block/{c}_glazed_terracotta",
        "Replaces vanilla template_glazed_terracotta with cube_all; animation shows on all six faces",
        "Model override; 1.20.1/1.21.1 blockstate still points at this model name",
        "used" if o else "unused", "H", "removes vanilla pattern-face rotation")
for pack in ("clouds2", "yeti_set_go"):
    add(pack, "assets/minecraft/models/item/rotten_flesh.json", "model", "minecraft:item/rotten_flesh",
        "Vanilla-style item/generated model (byte-identical to 1.16.5 vanilla)",
        "No map trigger found", "unused", "H", "only Mojang-identical JSON in either pack")

# ---- Clouds 2 item + lang ----
add("clouds2", "assets/minecraft/textures/item/rotten_flesh.png", "texture",
    "minecraft:rotten_flesh (display name 'Energy')",
    "2-frame red/blue strobe icon (32x16, frames [0,1], frametime 1)",
    "No give/item replace/container/entity/playerdata reference in the nine staged maps",
    "unused", "H", "renamed by en_us.json item.minecraft.rotten_flesh='Energy'")
add("yeti_set_go", "assets/minecraft/textures/item/rotten_flesh.png", "texture",
    "minecraft:rotten_flesh (display name 'Energy')",
    "2-frame red/blue strobe icon (byte-identical to Clouds 2)",
    "No map trigger found", "unused", "H", "same bytes as Clouds 2 item texture")
add("clouds2", "assets/minecraft/lang/en_us.json", "language", "item.minecraft.rotten_flesh",
    "'Rotten Flesh' -> 'Energy' (only gameplay-relevant custom line)", "Language override only",
    "unused", "H", "5084 strings identical to 1.19.2 vanilla; 44 changed; 27 extra; 305 missing")
add("yeti_set_go", "assets/minecraft/lang/en_us.json", "language", "item.minecraft.rotten_flesh",
    "'Rotten Flesh' -> 'Energy'", "Language override only", "unused", "H",
    "byte-identical to Clouds 2 lang")

# ---- Clouds 2 sounds ----
sounds = [
    ("assets/minecraft/sounds/acerace/jumppad.ogg", "acerace.jumppad (declared, unregistered)", "Jump-pad effect (0.97 s Vorbis)",
     "0", "sounds.json is invalid JSON, so this event never registers; audible copy exists at block/bell/bell_use01-03", "orphan", "H"),
    ("assets/minecraft/sounds/acerace/megajumppad.ogg", "acerace.megajumppad (declared, unregistered)", "Mega-jump effect (2.51 s)",
     "0", "unregistered; audible copy at block/candle/extinguish1-3", "orphan", "H"),
    ("assets/minecraft/sounds/acerace/speed.ogg", "acerace.speed (declared, unregistered)", "Speed effect (2.32 s)",
     "0", "unregistered; not copied to any vanilla path", "orphan", "H"),
    ("assets/minecraft/sounds/acerace/waterboost.ogg", "acerace.waterboost (declared, unregistered)", "Water-boost effect (1.44 s)",
     "0", "unregistered; not copied to any vanilla path", "orphan", "H"),
    ("assets/minecraft/sounds/block/bell/bell_use01.ogg", "block.bell.use", "Custom chime 0.97 s replacing vanilla 2.49 s bell",
     "18", "playsound at red/magenta/pink/yellow glazed pads", "used", "H"),
    ("assets/minecraft/sounds/block/candle/extinguish1.ogg", "block.candle.extinguish", "Mega-jump 2.51 s replacing 0.86 s candle puff",
     "6", "playsound at orange shulker mega-jump pads", "used", "H"),
    ("assets/minecraft/sounds/block/cave_vines/break1.ogg", "block.cave_vines.break", "Short pop 0.35 s",
     "6", "playsound at lime shulker jump pads", "used", "H"),
    ("assets/minecraft/sounds/block/chain/break1.ogg", "block.chain.break", "Metallic 2.12 s effect",
     "6", "playsound at yellow glazed speed pads", "used", "H"),
    ("assets/minecraft/sounds/block/chain/step1.ogg", "block.chain.step", "Falling/whoosh 2.11 s",
     "6", "playsound when cave_air is below the player", "used", "H"),
    ("assets/minecraft/sounds/block/brewing_stand/brew1.ogg", "block.brewing_stand.brew", "Start ambience 12.91 s",
     "1", "playsound master @a at start area (3,1,22)", "used", "H"),
    ("assets/minecraft/sounds/block/azalea/break1.ogg", "block.azalea.break", "Short chime 1.14 s",
     "0", "no command plays block.azalea.break", "unused", "H"),
    ("assets/minecraft/sounds/block/cherrywood_trapdoor/toggle1.ogg", "block.cherry_wood_trapdoor.open / .close",
     "5:10 music track, OggOpus", "0",
     "Replaces all cherry-trapdoor open/close sounds; zero cherry trapdoors placed in the staged maps, so unused in-map; Opus is not a supported Java sound codec",
     "unused", "M"),
    ("assets/minecraft/sounds/records/far.ogg", "music_disc.far", "5:10 music track, OggVorbis",
     "1", "playsound minecraft:music_disc.far master @a at start area (-35,1,21)", "used", "H"),
]
for path, target, effect, count, trigger, status, conf in sounds:
    note = ""
    if "bell" in path:
        note = "3 copies; vanilla event references only 01/02, so 03 is orphaned"
    elif "cave_vines" in path:
        note = "break5.ogg missing: 1 in 5 vanilla picks stays vanilla"
    elif "cherrywood" in path:
        note = "3 copies, 15.8 MB; tagged 'Ace Race (From \"MC Championship Season 2\")'"
    elif "records/far" in path:
        note = "tagged 'Ace Race (From \"MC Championship Season 2\")'; copyrighted"
    add("clouds2", path, "sound", target, effect, trigger, status, conf,
        f"trigger count {count}. {note}".strip())

add("clouds2", "assets/minecraft/sounds.json", "sound_index",
    "acerace.start / checkpoint / jumppad / megajumppad / speed / waterboost",
    "6 custom events declared; file is INVALID JSON (uses { \"path\" } instead of [ \"path\" ])",
    "No command uses acerace.*; map uses vanilla event IDs", "broken", "H",
    "whole file rejected by parser; start.ogg and checkpoint.ogg also absent")

# ---- Space Race blocks ----
space_blocks = [
    ("acacia_planks", "Grey checker panel", "building palette"),
    ("birch_planks", "Dark grey solid", "building palette"),
    ("birch_trapdoor", "White panel with four dark squares", "decorative trapdoor"),
    ("crimson_planks", "Orange solid", "building palette"),
    ("dark_oak_planks", "Light grey solid", "building palette"),
    ("jungle_planks", "Tan brick pattern", "building palette"),
    ("blackstone", "Flat red (vanilla blackstone recoloured)", "building palette"),
    ("blackstone_top", "Flat red top", "building palette"),
    ("stone_bricks", "Grey solid brick", "building palette"),
    ("smooth_stone", "Light grey panel", "building palette"),
    ("diamond_block", "Cyan bordered panel", "elytra trigger block: commands test diamond_block under player"),
    ("end_stone", "Yellow solid", "building palette"),
    ("glass", "Near-transparent panel (alpha 6/255)", "invisible walls/windows"),
    ("iron_bars", "Grey lattice", "fence/deco"),
    ("sea_lantern", "White glow, 5 frames", "lighting"),
    ("black_wool", "Near-black flat", "team colour"),
    ("gray_wool", "Dark grey flat", "team colour"),
    ("light_gray_wool", "Light grey flat", "team colour"),
    ("orange_wool", "Orange flat", "team colour"),
    ("yellow_wool", "Yellow flat", "team colour"),
    ("red_concrete", "Red grid panel", "levitation/speed trigger block"),
    ("light_blue_stained_glass", "Blue transparent grid", "glass accent"),
    ("red_stained_glass", "Red transparent grid", "glass accent"),
    ("white_stained_glass", "White transparent grid", "glass accent"),
    ("magenta_glazed_terracotta", "Orange/yellow chevron (not magenta)", "speed pad / world spawn trigger block"),
]
for slug, effect, trigger in space_blocks:
    block = f"minecraft:{slug}"
    o = own(block, "space_race")
    add("space_race", f"Space Race Resource Pack/assets/minecraft/textures/block/{slug}.png", "texture",
        block, effect, trigger, "used" if o else "unused", "H", f"{o} placed in space_race")

space_gui = [
    ("widgets", "Buttons/sliders/hearts sheet, orange-red theme", "dead", "1.21.1 moved this to gui/sprites"),
    ("icons", "Hearts/food/armour icons, orange-purple theme", "dead", "1.21.1 moved icons to gui/sprites"),
    ("container/inventory", "Inventory frame with crown/shield, red slots", "live", "still present in 1.21.1"),
    ("recipe_book", "Recipe book panels, red/orange", "live", "still present in 1.21.1"),
    ("recipe_button", "Two book icons", "dead", "removed in 1.21.1"),
    ("book", "Plain book frame, grey/red", "live", "still present in 1.21.1"),
    ("options_background", "Orange noise tile", "dead", "removed in 1.21.1"),
    ("accessibility", "Fully transparent 32x64 sheet", "dead", "removed in 1.21.1"),
]
for slug, effect, status, note in space_gui:
    add("space_race", f"Space Race Resource Pack/assets/minecraft/textures/gui/{slug}.png", "texture",
        f"GUI: {slug}", effect, "GUI texture override (client-side)", status, "H", note)

add("space_race", "Space Race Resource Pack/assets/minecraft/textures/item/carrot_on_a_stick.png", "texture",
    "minecraft:carrot_on_a_stick", "Glowing red gem/button, not a carrot",
    "Item named 'Restart Attempt' in UUID 00000000... playerdata; no command/datapack trigger",
    "authored", "H", "custom name in NBT, not lang")
add("space_race", "Space Race Resource Pack/assets/minecraft/textures/item/acacia_sign.png", "texture",
    "minecraft:acacia_sign", "Fully transparent item icon (invisible item)",
    "Saved builder hotbar contains acacia_sign; no command/datapack trigger",
    "authored", "H", "alpha=0; makes the item icon invisible")

# item names / NBT
add("space_race", "(NBT) playerdata/00000000-0000-0000-0000-000000000000.dat", "item_nbt",
    "minecraft:carrot_on_a_stick", "display.Name = 'Restart Attempt' (red, italic false)",
    "Saved player inventory slot 1; not granted by any command block", "authored", "H", "no CustomModelData")
add("space_race", "(NBT) playerdata/00000000-0000-0000-0000-000000000000.dat", "item_nbt",
    "minecraft:trident", "display.Name = 'Trident' (yellow, italic false)",
    "Saved player inventory slot 0", "authored", "H", "no CustomModelData")
for i in range(5):
    add("clouds2", f"(NBT) command block red_terracotta layer ~-{i+1}", "item_nbt",
        "minecraft:leather_boots", "display.color=16777215 (white), depth_strider 255, HideFlags:1",
        "item replace entity @a armor.feet when player is over red_terracotta", "authored", "H",
        "vanilla item, no custom name or model")

# meta / icons / source
for pack, fmt, desc in (("clouds2", 15, "Ace Race Resource Pack"), ("yeti_set_go", 9, "Ace Race Animation Example"),
                        ("space_race", 6, "Space Race Textures.")):
    folder = "" if pack != "space_race" else "Space Race Resource Pack/"
    add(pack, f"{folder}pack.mcmeta", "metadata", "pack", f"pack_format {fmt}; {desc}",
        "Pack root descriptor", "valid", "H", "1.21.1 expects 34")
    add(pack, f"{folder}pack.png", "pack_icon", "pack icon",
        "Yellow chevrons" if pack != "space_race" else "Green crowned character on orange",
        "Pack list icon", "ok", "H", "")
add("clouds2", "assets/minecraft/textures/block/AnimPad.psd", "source_file", "(none)",
    "Photoshop CS6 source, 32x32, 6 frames", "Client ignores .psd", "unused", "H",
    "author source file leaked into shipped pack; also in Yeti")

with open(os.path.join(DATA, "asset_map.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(ROWS[0].keys()))
    w.writeheader()
    w.writerows(ROWS)
print("rows", len(ROWS))
for r in ROWS:
    if r["asset_type"] in ("texture", "model"):
        print(f"{r['pack']:12} {r['target'][:42]:42} own={str(r['own_map_placements']):>5} {r['status']:8} other={r['other_maps_same_block'][:64]}")
