"""Cross-check the 25 BuildPreset builds against the real MCC block palette
and the current 27 raw stalls.  Writes:
  data/preset_demand.csv      one row per (preset, material)
  data/current_vs_real.csv    one row per current raw stall
  data/demand_summary.csv     one row per material used by any preset
"""
import collections, csv, os, re, pathlib

ROOT = pathlib.Path(__file__).resolve().parent
SRC = ROOT.parents[2] / "BuildMart" / "src" / "main" / "java" / "com" / "mcm" / "games" / "buildmart" / "BuildPreset.java"
DATA = ROOT / "data"
DATA.mkdir(exist_ok=True)
src = SRC.read_text(encoding="utf-8")

legend = dict(re.findall(r"LEGEND\.put\('(.)', Material\.([A-Z_]+)\);", src))
body = src.split("static final List<BuildPreset> ALL", 1)[1]
entries = []
for m in re.finditer(r'p\("([^"]+)",\s*(.*?)\)\s*,?\s*(?=p\("|\)\s*;)', body, re.S):
    name, blob = m.group(1), m.group(2)
    layers = re.findall(r'L\(([^)]*)\)', blob)
    if not layers:
        continue
    rows = []
    for lay in layers:
        rows.append(re.findall(r'"([^"]*)"', lay))
    counts = collections.Counter()
    for lay in rows:
        for row in lay:
            for ch in row:
                if ch != ".":
                    counts[legend[ch]] += 1
    if counts:
        entries.append((name, counts))

SHOP = {"GLASS", "STONE_BRICKS", "BRICKS", "GOLD_BLOCK"}
def stars(mats, total):
    score = total + 2 * sum(v for k, v in mats.items() if k in SHOP)
    return 1 if score < 12 else 2 if score < 20 else 3 if score < 30 else 4 if score < 42 else 5

with open(DATA / "preset_demand.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["preset", "blocks", "stars", "shop_blocks", "material", "count"])
    for name, c in entries:
        blk = sum(c.values())
        shop = sum(v for k, v in c.items() if k in SHOP)
        for mat, n in c.most_common():
            w.writerow([name, blk, stars(c, blk), shop, mat, n])

total = collections.Counter()
per = collections.defaultdict(list)
for name, c in entries:
    total.update(c)
    for k, v in c.items():
        per[k].append((name, v))

with open(DATA / "demand_summary.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["material", "total_used", "max_in_one_build", "builds_using", "kind"])
    for mat, n in total.most_common():
        kind = "shop" if mat in SHOP else "raw"
        w.writerow([mat, n, max(v for _, v in per[mat]),
                    ";".join(f"{a}:{b}" for a, b in per[mat]), kind])

# current stalls -> real island
STALLS = ["OAK_PLANKS", "OAK_LOG", "SPRUCE_PLANKS", "BIRCH_PLANKS",
          "COBBLESTONE", "SAND", "CLAY", "IRON_BLOCK", "GLOWSTONE",
          "QUARTZ_BLOCK", "SANDSTONE", "SNOW_BLOCK",
          "WHITE_WOOL", "RED_WOOL", "ORANGE_WOOL", "YELLOW_WOOL", "LIME_WOOL",
          "GREEN_WOOL", "LIGHT_BLUE_WOOL", "BLUE_WOOL", "BLACK_WOOL",
          "PINK_WOOL", "BROWN_WOOL", "OAK_LEAVES", "PUMPKIN", "HAY_BLOCK", "MELON"]
MAP = {
    "OAK_PLANKS": ("logs", "exact", "mined as planks on Lumber"),
    "OAK_LOG": ("logs", "exact", ""),
    "SPRUCE_PLANKS": ("logs", "exact", ""),
    "BIRCH_PLANKS": ("logs", "exact", "no preset uses birch; dead stall"),
    "COBBLESTONE": ("stone", "derived", "Stone island stocks STONE; cobble only via mining/drop"),
    "SAND": ("sand", "exact", "used only as a shop input, never placed in a build"),
    "CLAY": ("-", "absent", "CLAY is not on the MCC 20 poster; Bricks island stocks BRICKS"),
    "IRON_BLOCK": ("ores", "exact", "also the Jeweller gold recipe input"),
    "GLOWSTONE": ("-", "absent", "not on the MCC 20 poster; only Lighthouse uses 1"),
    "QUARTZ_BLOCK": ("ores+nether", "exact", "duplicate across two islands"),
    "SANDSTONE": ("sand", "exact", ""),
    "SNOW_BLOCK": ("snow", "event-only", "Snow island replaced Sand in MCC 19 only"),
    "WHITE_WOOL": ("colour", "wrong-form", "colour bays stock concrete/powder/glass, not wool"),
    "OAK_LEAVES": ("garden", "unconfirmed", "poster shows sapling/moss, no leaf block"),
    "PUMPKIN": ("garden", "unconfirmed", "not on poster"),
    "HAY_BLOCK": ("garden", "unconfirmed", "not on poster"),
    "MELON": ("garden", "unconfirmed", "not on poster"),
}
for wool in ["RED_WOOL", "ORANGE_WOOL", "YELLOW_WOOL", "LIME_WOOL", "GREEN_WOOL",
             "LIGHT_BLUE_WOOL", "BLUE_WOOL", "BLACK_WOOL", "PINK_WOOL", "BROWN_WOOL"]:
    MAP[wool] = ("colour", "wrong-form", "colour bays stock concrete/powder/glass, not wool")

with open(DATA / "current_vs_real.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["stall_material", "used_by_presets", "total_used", "real_island",
                "match", "note"])
    for s in STALLS:
        isl, match, note = MAP.get(s, ("-", "unknown", ""))
        w.writerow([s, "yes" if total[s] else "no", total[s], isl, match, note])

print(f"{len(entries)} presets, {len(total)} materials")
print("shop-only:", sorted(SHOP & set(total)))
print("dead stalls:", [s for s in STALLS if not total[s]])
