#!/usr/bin/env python3
"""Generate signs.csv, blocks.csv, containers.csv, entities.csv, pack_mapping.csv, command_failures.csv."""
import csv, json, re, zipfile
from collections import Counter, defaultdict

D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
Z = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Clouds_2_-_Electric_Boogaloo__Download_Version_\Clouds 2 - Electric Boogaloo\resources.zip"

def decode(js):
    out = []
    for m in js:
        try:
            o = json.loads(m)
            out.append(o.get("text", "") if isinstance(o, dict) else str(o))
        except Exception:
            out.append(str(m))
    return out

# ---------- signs ----------
sig = json.load(open(D + r"\signs.json", encoding="utf-8"))
rows = []
for s in sorted(sig, key=lambda r: (r["z"], r["x"], r["y"])):
    f = decode(s["front"]); b = decode(s["back"])
    rows.append({
        "x": s["x"], "y": s["y"], "z": s["z"],
        "block": "", "front_1": f[0] if len(f) > 0 else "", "front_2": f[1] if len(f) > 1 else "",
        "front_3": f[2] if len(f) > 2 else "", "front_4": f[3] if len(f) > 3 else "",
        "back_1": b[0] if len(b) > 0 else "", "back_2": b[1] if len(b) > 1 else "",
        "back_3": b[2] if len(b) > 2 else "", "back_4": b[3] if len(b) > 3 else "",
        "color": s["color"], "glowing": int(s["glow"]), "waxed": int(s["waxed"]),
        "click_event": "none",
    })
with open(D + r"\signs.csv", "w", newline="", encoding="utf-8-sig") as fh:
    w = csv.DictWriter(fh, fieldnames=list(rows[0].keys()))
    w.writeheader(); w.writerows(rows)
print("signs.csv", len(rows))

# ---------- containers ----------
be = json.load(open(D + r"\block_entities.json", encoding="utf-8"))
sh = [b for b in be if b["id"] == "minecraft:shulker_box"]
# variant from dump
import gzip
O = (-258, -64, -316)
want = {(b["x"], b["y"], b["z"]) for b in sh}
var = {}
with gzip.open(D + r"\clouds2_full.mcmdump.gz", "rt", encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        p = line.split(" ", 3)
        if len(p) < 4: continue
        x, y, z = int(p[0]) + O[0], int(p[1]) + O[1], int(p[2]) + O[2]
        if (x, y, z) in want:
            var[(x, y, z)] = p[3].strip().split("[")[0]
crows = []
for b in sorted(sh, key=lambda r: (r["z"], r["x"], r["y"])):
    crows.append({
        "x": b["x"], "y": b["y"], "z": b["z"],
        "block": var.get((b["x"], b["y"], b["z"]), "?"),
        "nbt_items_tag": "absent",
        "contents": "empty",
        "role": "jump-pad surface (command-detected), not storage",
    })
with open(D + r"\containers.csv", "w", newline="", encoding="utf-8-sig") as fh:
    w = csv.DictWriter(fh, fieldnames=list(crows[0].keys()))
    w.writeheader(); w.writerows(crows)
print("containers.csv", len(crows), Counter(r["block"] for r in crows))

# ---------- entities ----------
ents = json.load(open(D + r"\entities.json"))
erows = [{"type": e["type"], "x": e["x"], "y": e["y"], "z": e["z"],
          "tags": ";".join(e["tags"]), "items": e["items"], "armor": e["armor"],
          "note": "Refresh datapack spawn marker (datapack not shipped)"} for e in ents]
with open(D + r"\entities.csv", "w", newline="", encoding="utf-8-sig") as fh:
    w = csv.DictWriter(fh, fieldnames=list(erows[0].keys()))
    w.writeheader(); w.writerows(erows)
print("entities.csv", len(erows))

# ---------- command failures ----------
cbf = list(csv.DictReader(open(D + r"\command_blocks.csv", encoding="utf-8-sig")))
frows = []
for r in cbf:
    if r["syntax_1_21"].startswith("FAIL"):
        frows.append({"x": r["x"], "y": r["y"], "z": r["z"], "bank": r["bank"],
                      "command": r["command"], "failure": r["syntax_1_21"],
                      "native_1_20_1": "fails" if r["command"].strip() == "setblock 5 4 22" else "works",
                      "fix_owner": "none (research only)"})
with open(D + r"\command_failures.csv", "w", newline="", encoding="utf-8-sig") as fh:
    w = csv.DictWriter(fh, fieldnames=list(frows[0].keys()))
    w.writeheader(); w.writerows(frows)
print("command_failures.csv", len(frows))

# ---------- pack mapping ----------
z = zipfile.ZipFile(Z)
pack_rows = [
    ("pack.mcmeta", "pack_format 15 (1.20.1), description 'Ace Race Resource Pack'", "whole pack", "loads in 1.20.1; 1.21.1 flags format 15 as old"),
    ("assets/minecraft/textures/block/yellow_glazed_terracotta.png", "16-frame animated yellow/orange chevron arrow (frametime 1)", "all yellow glazed pads: wayfinding arrows + checkpoints", "arrows point against block facing; verified by facing field vs route direction"),
    ("assets/minecraft/textures/block/red_glazed_terracotta.png", "7-frame animated red stripes (frametime 2)", "red glazed jump+speed pads", "pad readability"),
    ("assets/minecraft/textures/block/magenta_glazed_terracotta.png", "7-frame animated concentric square (frametime 2)", "magenta glazed jump+speed pads", "pad readability"),
    ("assets/minecraft/textures/block/pink_glazed_terracotta.png", "7-frame animated L-corner stripes (frametime 2)", "pink glazed jump+speed pads", "pad readability"),
    ("assets/minecraft/textures/block/purple_glazed_terracotta.png", "7-frame animated L-corner stripes (frametime 2)", "purple glazed blocks (decor in crater)", "unused by commands"),
    ("assets/minecraft/models/block/*_glazed_terracotta.json", "cube_all parent: removes vanilla oriented pattern", "all glazed terracotta", "makes every pad read as a flat colour tile, independent of placement rotation"),
    ("assets/minecraft/textures/block/*_terracotta.png", "152-byte flat-colour 16x16 textures (16 colours)", "all terracotta terrain, rainbow arch at z=161", "the rainbow is terracotta bands; the pack flattens them to clean colour"),
    ("assets/minecraft/textures/block/sea_lantern.png", "5-frame animated white lamp (frametime 5)", "sea lanterns (6 blocks)", "light source"),
    ("assets/minecraft/textures/block/glowstone.png", "custom glowstone texture", "glowstone (not present in map block list)", "unused"),
    ("assets/minecraft/textures/block/diorite.png", "flat diorite", "diorite terrain (222 blocks)", "palette flattening"),
    ("assets/minecraft/textures/block/oak_planks.png", "flat oak planks", "oak planks (1189 blocks)", "palette flattening"),
    ("assets/minecraft/textures/block/red_nether_bricks.png", "flat red nether bricks", "red nether brick decor (131+118)", "palette flattening"),
    ("assets/minecraft/textures/block/red_stained_glass.png", "custom red glass", "start-box reset wall at z=29", "reset visual"),
    ("assets/minecraft/textures/block/soul_sand.png", "flat soul sand", "bubble elevator bases (448)", "water elevators"),
    ("assets/minecraft/textures/item/rotten_flesh.png + .mcmeta", "2-frame animated 'Energy' item", "item.minecraft.rotten_flesh renamed 'Energy' in lang", "no command uses rotten flesh; dead asset in this download"),
    ("assets/minecraft/models/item/rotten_flesh.json", "generated item model", "'Energy' item", "dead asset in this download"),
    ("assets/minecraft/sounds.json", "declares acerace.start / checkpoint / jumppad / megajumppad / speed / waterboost", "custom sound events", "start + checkpoint OGGs are MISSING; no command plays any acerace.* event"),
    ("assets/minecraft/sounds/acerace/jumppad.ogg", "custom jump-pad sound", "declared, never played by any command", "unused"),
    ("assets/minecraft/sounds/acerace/megajumppad.ogg", "custom mega-jump sound", "declared, never played", "unused"),
    ("assets/minecraft/sounds/acerace/speed.ogg", "custom speed sound", "declared, never played", "unused"),
    ("assets/minecraft/sounds/acerace/waterboost.ogg", "custom water boost sound", "declared, never played", "unused"),
    ("assets/minecraft/sounds/block/bell/bell_use01-03.ogg", "replaces vanilla bell", "red/magenta/pink glazed pads: playsound block.bell.use", "jump-pad sound"),
    ("assets/minecraft/sounds/block/cave_vines/break1-4.ogg", "replaces cave vines break", "lime shulker pads: playsound block.cave_vines.break", "jump-pad sound"),
    ("assets/minecraft/sounds/block/candle/extinguish1-3.ogg", "replaces candle extinguish", "orange shulker mega jump: playsound block.candle.extinguish", "mega-jump sound"),
    ("assets/minecraft/sounds/block/chain/break1-4.ogg + step1-6.ogg", "replaces chain sounds", "checkpoint (cave_air) and green pads", "checkpoint/trap sounds"),
    ("assets/minecraft/sounds/block/brewing_stand/brew1-2.ogg", "replaces brewing stand", "start gate: playsound block.brewing_stand.brew", "start sound"),
    ("assets/minecraft/sounds/block/azalea/break1-6.ogg", "replaces azalea break", "not referenced by any command", "unused"),
    ("assets/minecraft/sounds/block/cherrywood_trapdoor/toggle1-3.ogg", "replaces cherry trapdoor", "not referenced by any command", "path renamed in 1.20.5+; likely unused in 1.21"),
    ("assets/minecraft/sounds/records/far.ogg", "replaces music disc far", "start countdown: playsound music_disc.far", "5 MB music; countdown theme"),
    ("assets/minecraft/lang/en_us.json", "full 1.20.1 en_us copy (5155 keys); only deliberate override found: item.minecraft.rotten_flesh = 'Energy'", "item naming", "safe to drop except the 'Energy' rename"),
    ("assets/minecraft/textures/block/AnimPad.psd", "author's layered source file for the animated pad", "source art", "licensing/source leak risk; do not ship"),
    ("pack.png", "pack icon", "pack list", "cosmetic"),
]
with open(D + r"\pack_mapping.csv", "w", newline="", encoding="utf-8-sig") as fh:
    w = csv.writer(fh)
    w.writerow(["asset", "what_it_is", "used_by_in_map", "notes"])
    w.writerows(pack_rows)
print("pack_mapping.csv", len(pack_rows))

# ---------- blocks ----------
bc = json.load(open(D + r"\block_counts.json", encoding="utf-8"))
total = sum(v for k, v in bc)
USE = {
 "water": "waterfalls, pools, glass water tunnel, bubble elevators",
 "snow_block": "primary white walkable surface of sky islands",
 "stone": "ruin/terrace structure",
 "calcite": "white cloud/terrace surface; 'clear inventory' pad material",
 "cobweb": "cave/spider decor and soft-landing traps",
 "sandstone": "desert village walls/floors",
 "smooth_sandstone": "desert village trim and terrace edges",
 "barrier": "invisible course bounding walls, gate block, pool rim",
 "blue_stained_glass": "water-tunnel shell and portal rings",
 "lime_terracotta": "start hedge/arch and green terrain accents",
 "packed_mud": "garden/farm paths and castle bridge",
 "grass_block": "garden and farm surface",
 "lime_concrete": "start gate/hedge, animated chevron ground",
 "podzol": "crater village ground",
 "oak_planks": "village decks and bridges",
 "dirt": "sub-surface",
 "gray_terracotta": "rainbow/village accents",
 "green_terracotta": "rainbow/village accents",
 "light_gray_terracotta": "gate trim and terracotta accents",
 "mud_bricks": "crater village walls",
 "light_blue_wool": "ice/water decorative accents",
 "light_blue_concrete": "portal rings and ice accents",
 "structure_void": "technical block: cuts the tunnel at the ruin, masks structures",
 "yellow_terracotta": "rainbow band and desert trim",
 "bubble_column": "upward water elevators (3 shafts)",
 "light_blue_terracotta": "rainbow band",
 "prismarine": "water/reef decor",
 "yellow_glazed_terracotta": "checkpoint + animated chevron arrow pads",
 "oak_slab": "village steps",
 "red_terracotta": "rainbow band + depth-strider pad triggers",
 "stone_bricks": "ruin masonry",
 "ice": "frozen pools",
 "soul_sand": "bubble elevator bases",
 "yellow_concrete": "direction arrows / lightning-bolt markers",
 "packed_ice": "frozen pools",
 "orange_concrete": "crater rim and final-leg arches",
 "warped_planks": "nether-ish accents in the castle",
 "seagrass": "water decor",
 "moss_block": "garden/ruin greenery",
 "gray_concrete": "start floor and control-room casing",
 "smooth_stone": "village trim",
 "spruce_planks": "elytra launch platforms",
 "repeating_command_block": "logic (283 always active)",
 "oak_leaves": "garden hedges",
 "birch_sign": "instruction panels",
 "stripped_oak_wood": "village beams",
 "andesite": "ruin/village stone",
 "diorite": "cloud island accents",
 "black_concrete": "checkered gates and start line",
 "lime_shulker_box": "jump-pad surface (+5/6/7 jump boost)",
 "purple_terracotta": "rainbow band / crater decor",
 "magenta_terracotta": "rainbow/village accents",
 "brown_mushroom_block": "giant mushroom decor",
 "mud_brick_slab": "crater village steps",
 "pink_glazed_terracotta": "jump+speed pad",
 "red_glazed_terracotta": "jump+speed pad",
 "green_concrete": "wayfinding arrow heads",
 "slime_block": "bounce decor/landing",
 "white_concrete": "checkered gates, cloud accents",
 "stone_slab": "steps",
 "sandstone_wall": "village walls",
 "cobblestone": "ruin rubble",
 "spruce_leaves": "snow-section trees",
 "red_nether_bricks": "castle/nether accents",
 "red_concrete": "castel/bottom accents",
 "iron_trapdoor": "village/ruin detail",
 "red_nether_brick_stairs": "castle stairs",
 "tuff": "ruin stone",
 "blue_terracotta": "rainbow band",
 "black_terracotta": "rainbow/village accents",
 "warped_slab": "castle trim",
 "oak_wood": "village beams",
 "pink_terracotta": "rainbow/desert accents",
 "sandstone_stairs": "village stairs",
 "vine": "hanging greenery on the water tunnel and islands",
 "black_wool": "checker pattern",
 "white_wool": "checker pattern",
 "spruce_trapdoor": "village detail",
 "white_terracotta": "rainbow/village accents",
 "oak_stairs": "village stairs",
 "brown_concrete": "crater/desert accents",
 "smooth_quartz_slab": "village trim",
 "stripped_birch_log": "village beams",
 "mud_brick_wall": "crater village walls",
 "stripped_spruce_wood": "village beams",
 "diorite_slab": "cloud terrace steps",
 "stone_stairs": "ruin stairs",
 "prismarine_slab": "reef decor",
 "spruce_stairs": "village stairs",
 "magenta_glazed_terracotta": "jump+speed pad",
 "grass": "garden decor (becomes short_grass on upgrade)",
 "redstone_wire": "start countdown circuit",
 "mud_brick_stairs": "crater village stairs",
 "red_stained_glass": "reset wall at z=29",
 "pink_concrete": "pad accents",
 "repeater": "start countdown delay line",
 "terracotta": "generic terracotta",
 "red_nether_brick_slab": "castle trim",
 "green_glazed_terracotta": "slowness trap pad",
 "tall_grass": "garden decor",
 "purple_concrete": "flower/castle accents",
 "sweet_berry_bush": "garden decor/trap",
 "iron_bars": "village detail",
 "sandstone_slab": "village steps",
 "spruce_slab": "village steps",
 "command_block": "impulse logic (23)",
 "smooth_quartz": "village trim",
 "spruce_fence": "village rails",
 "fire_coral_block": "reef decor",
 "prismarine_brick_slab": "reef decor",
 "soul_soil": "bubble elevator bases",
 "fire_coral": "reef decor",
 "tube_coral": "reef decor",
 "brain_coral_block": "reef decor",
 "orange_shulker_box": "mega-jump pad surface",
 "smooth_sandstone_stairs": "village stairs",
 "ladder": "village/ruin access",
 "horn_coral_fan": "reef decor",
 "pink_tulip": "garden decor",
 "orange_tulip": "garden decor",
 "brain_coral": "reef decor",
 "azalea_leaves": "garden hedges",
 "birch_slab": "village steps",
 "tube_coral_block": "reef decor",
 "chain_command_block": "sequential start/reset logic (15)",
 "andesite_slab": "elytra launch platform (castle variant)",
 "dark_oak_leaves": "snow-section trees",
 "spruce_door": "village doors",
 "oak_log": "village beams",
 "cobblestone_slab": "ruin steps",
 "stone_brick_stairs": "ruin stairs",
 "birch_planks": "village decks",
 "warped_stairs": "castle stairs",
 "bamboo_wall_sign": "instruction panels",
 "dark_oak_wood": "castle beams",
 "granite": "ruin accents",
 "quartz_stairs": "village stairs",
 "stone_brick_wall": "ruin walls",
 "andesite_wall": "ruin walls",
 "iron_block": "village detail",
 "sea_lantern": "light source",
 "light_blue_stained_glass": "ice/portal accent",
 "jungle_fence": "garden fence",
 "lily_pad": "pond decor",
 "jungle_leaves": "garden canopy",
 "polished_andesite": "gate trim",
 "red_sandstone_slab": "village steps",
 "mushroom_stem": "giant mushroom",
 "granite_stairs": "ruin stairs",
 "granite_slab": "ruin steps",
 "brown_mushroom": "giant mushroom",
 "snow": "snow layer",
 "smooth_red_sandstone_slab": "village steps",
 "fire_coral_wall_fan": "reef decor",
 "deepslate_tile_slab": "ruin steps",
 "smooth_sandstone_slab": "village steps",
 "tube_coral_wall_fan": "reef decor",
 "chain": "hanging chain decor",
 "stone_button": "START (5,4,22) and RESET (-3,4,25) buttons",
 "red_sandstone": "village walls",
 "polished_deepslate_slab": "ruin steps",
 "stone_brick_slab": "ruin steps",
 "horn_coral_block": "reef decor",
 "fire_coral_fan": "reef decor",
 "tube_coral_fan": "reef decor",
 "sticky_piston": "start circuit latch",
 "redstone_block": "start circuit power source",
 "birch_fence": "garden fence",
 "flowering_azalea_leaves": "garden hedges",
}
brows = []
for k, v in bc:
    name = k.replace("minecraft:", "")
    use = USE.get(name)
    if not use:
        if "coral" in name or "prismarine" in name or "sea" in name:
            use = "reef/water decor"
        elif "sign" in name:
            use = "instruction panel"
        elif "leaves" in name or "grass" in name or "flower" in name or "tulip" in name:
            use = "garden decor"
        elif "stairs" in name or "slab" in name:
            use = "step/trim"
        else:
            use = "decor/detail"
    brows.append({"block": name, "count": v, "share_pct": round(v / total * 100, 3), "use": use})
with open(D + r"\blocks.csv", "w", newline="", encoding="utf-8-sig") as fh:
    w = csv.DictWriter(fh, fieldnames=["block", "count", "share_pct", "use"])
    w.writeheader(); w.writerows(brows)
print("blocks.csv", len(brows), "total", total)
