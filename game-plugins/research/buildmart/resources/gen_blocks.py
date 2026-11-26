"""Generate data/blocks.csv for the Build Mart resource-island research pack.

Coordinates are PROPOSED map coordinates for our rebuild, not extracted MCC
coordinates.  The MCC map only gives relative ring order, so angles are a
design proposal and every row is marked M/L accordingly.
"""
import csv, os

OUT = os.path.join(os.path.dirname(__file__), "data", "blocks.csv")
os.makedirs(os.path.dirname(OUT), exist_ok=True)

# ---- proposed geometry (our map) -------------------------------------------
HUB_R, HUB_Y = 0, 64
COLOUR_R, COLOUR_Y = 56, 64
UPPER_R, UPPER_Y = 128, 104

# 8 upper islands, clockwise from north.  Order derived from three wide VOD
# shots (see README finding F4); flagged M because GARDEN/NETHER adjacency was
# seen from one angle only.
UPPER = [
    ("ores",   "Ores",   0),
    ("nether", "Nether", 45),
    ("bricks", "Bricks", 90),
    ("logs",   "Lumber", 135),
    ("sand",   "Sand",   180),
    ("stone",  "Stone",  225),
    ("copper", "Copper", 270),
    ("garden", "Garden", 315),
]

# material, form, note, confidence
ISLAND_BLOCKS = {
    "ores": [
        ("REDSTONE_BLOCK", "block", "", "H"),
        ("DIAMOND_BLOCK", "block", "", "H"),
        ("EMERALD_BLOCK", "block", "", "H"),
        ("GOLD_BLOCK", "block", "", "H"),
        ("IRON_BLOCK", "block", "", "H"),
        ("COAL_BLOCK", "block", "", "H"),
        ("QUARTZ_BLOCK", "block", "also on Nether (MCC P21)", "H"),
    ],
    "nether": [
        ("QUARTZ_BLOCK", "block", "also on Ores (MCC P21)", "H"),
        ("WARPED_WART_BLOCK", "block", "", "H"),
        ("NETHER_WART_BLOCK", "block", "", "H"),
        ("NETHER_BRICKS", "block", "also on Bricks (MCC P21)", "H"),
        ("BLACKSTONE", "block", "", "H"),
    ],
    "bricks": [
        ("BRICKS", "block", "", "H"),
        ("PRISMARINE_BRICKS", "block", "texture read as prismarine bricks; plain prismarine possible", "M"),
        ("MOSSY_STONE_BRICKS", "block", "", "H"),
        ("STONE_BRICKS", "block", "", "H"),
        ("NETHER_BRICKS", "block", "also on Nether (MCC P21)", "H"),
    ],
    "logs": [
        ("BIRCH_LOG", "log", "", "H"),
        ("BIRCH_PLANKS", "planks", "", "H"),
        ("SPRUCE_LOG", "log", "", "H"),
        ("SPRUCE_PLANKS", "planks", "", "H"),
        ("JUNGLE_LOG", "log", "3rd wood type; exact species inferred from texture", "M"),
        ("JUNGLE_PLANKS", "planks", "3rd wood type; exact species inferred", "M"),
        ("OAK_LOG", "log", "", "H"),
        ("OAK_PLANKS", "planks", "", "H"),
        ("DARK_OAK_LOG", "log", "", "H"),
        ("DARK_OAK_PLANKS", "planks", "", "H"),
    ],
    "sand": [
        ("SAND", "block", "replaced by Snow in MCC 19 only", "H"),
        ("SANDSTONE", "block", "", "H"),
        ("RED_SAND", "block", "", "H"),
        ("RED_SANDSTONE", "block", "", "H"),
    ],
    "stone": [
        ("STONE", "block", "drops cobblestone unless silk-touch/custom drop", "H"),
        ("DIORITE", "block", "", "H"),
        ("ANDESITE", "block", "", "H"),
        ("GRANITE", "block", "", "H"),
    ],
    "copper": [
        ("COPPER_BLOCK", "block", "replaced Coral in MCC 20", "H"),
        ("CUT_COPPER", "block", "", "H"),
        ("OXIDIZED_COPPER", "block", "", "H"),
        ("OXIDIZED_CUT_COPPER", "block", "", "H"),
    ],
    "garden": [
        ("LILY_OF_THE_VALLEY", "flower", "", "H"),
        ("ALLIUM", "flower", "", "H"),
        ("CORNFLOWER", "flower", "", "H"),
        ("OXEYE_DAISY", "flower", "", "H"),
        ("DANDELION", "flower", "", "H"),
        ("BLUE_ORCHID", "flower", "", "H"),
        ("AZURE_BLUET", "flower", "", "H"),
        ("POPPY", "flower", "", "H"),
        ("PINK_TULIP", "flower", "", "H"),
        ("ORANGE_TULIP", "flower", "", "H"),
        ("OAK_SAPLING", "plant", "shown as small tree on poster", "M"),
        ("MOSS_BLOCK", "block", "MCC 20 added azalea bushes + moss", "H"),
    ],
}

# 16 colour bays.  Exact angular order not fully verified (M/L); the
# material->bay lookup is what implementation needs.
COLOURS = [
    "WHITE", "LIGHT_GRAY", "GRAY", "BLACK", "BROWN", "RED", "ORANGE", "YELLOW",
    "LIME", "GREEN", "CYAN", "LIGHT_BLUE", "BLUE", "PURPLE", "MAGENTA", "PINK",
]

# actual names seen on the ring in VOD frames (subset, H)
SEEN_COLOUR_BAYS = {"BLUE", "PURPLE", "PINK", "WHITE", "RED", "ORANGE",
                    "YELLOW", "LIME", "GREEN", "BLACK", "GRAY"}

rows = []

def add(**kw):
    base = dict(island_id="", island_name="", tier="", category="", angle_deg="",
                radius="", y="", slot="", local_x="", local_z="", material="",
                form="", stock="", regen_ticks="", recipe="", duplicate_of="",
                confidence="", source="")
    base.update(kw)
    rows.append(base)

for iid, name, ang in UPPER:
    blocks = ISLAND_BLOCKS[iid]
    # 3-wide shelf grid, 3 rows high inside a 20x20 island pad
    for i, (mat, form, note, conf) in enumerate(blocks):
        lx = (i % 4) * 3 - 4
        lz = (i // 4) * 3 - 3
        dup = ""
        if mat == "QUARTZ_BLOCK":
            dup = "ores" if iid == "nether" else "nether"
        if mat == "NETHER_BRICKS":
            dup = "nether" if iid == "bricks" else "bricks"
        add(island_id=iid, island_name=name, tier="upper", category=name,
            angle_deg=ang, radius=UPPER_R, y=UPPER_Y, slot=f"{i:02d}",
            local_x=lx, local_z=lz, material=mat, form=form, stock=16,
            regen_ticks=60, recipe="", duplicate_of=dup, confidence=conf,
            source=("wiki:BuildMartMap.png" if not note else
                    "wiki:BuildMartMap.png; " + note))

for i, colour in enumerate(COLOURS):
    ang = round(i * 22.5, 1)
    for j, (form, suffix) in enumerate([
            ("concrete", "_CONCRETE"),
            ("concrete_powder", "_CONCRETE_POWDER"),
            ("stained_glass", "_STAINED_GLASS")]):
        mat = colour + suffix
        conf = "H" if colour in SEEN_COLOUR_BAYS else "M"
        add(island_id=f"colour_{colour.lower()}", island_name=f"Colour: {colour.title()}",
            tier="lower", category="Colour", angle_deg=ang, radius=COLOUR_R,
            y=COLOUR_Y, slot=f"{j}", local_x=j * 3 - 3, local_z=0,
            material=mat, form=form, stock=32, regen_ticks=60, recipe="",
            duplicate_of="", confidence=conf,
            source="wiki:BuildMartMap.png (glass+concrete); VOD 2:21:40/2:22:18 (powder, glass)")

# build-area crafting/smelting (NOT mart stalls)
CRAFT = [
    ("GLASS", "smelt 1 SAND -> 1 GLASS", "M"),
    ("STONE", "smelt 1 COBBLESTONE -> 1 STONE", "M"),
    ("STONE_BRICKS", "craft/stonecut 4 STONE -> 4 STONE_BRICKS", "M"),
    ("BRICKS", "smelt 1 CLAY_BALL/CLAY -> 1 BRICK; craft 4 BRICKS -> 1 BRICKS", "M"),
    ("OAK_PLANKS", "craft 1 LOG -> 4 PLANKS (lumber already stocks planks)", "M"),
]
for i, (mat, recipe, conf) in enumerate(CRAFT):
    add(island_id="build_area", island_name="Build area (craft/smelt)",
        tier="build", category="Crafting", angle_deg="", radius="", y=64,
        slot=f"{i:02d}", local_x="", local_z="", material=mat, form="crafted",
        stock="", regen_ticks="", recipe=recipe, duplicate_of="",
        confidence=conf, source="wiki:Big_Sales_at_Build_Mart (5 chests, crafting tables, stonecutters, 2 smelting areas)")

cols = ["island_id", "island_name", "tier", "category", "angle_deg", "radius",
        "y", "slot", "local_x", "local_z", "material", "form", "stock",
        "regen_ticks", "recipe", "duplicate_of", "confidence", "source"]
with open(OUT, "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=cols)
    w.writeheader()
    for r in rows:
        w.writerow(r)
print("wrote", OUT, len(rows), "rows")
