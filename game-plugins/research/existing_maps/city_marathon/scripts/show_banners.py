import json
base = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data"
sel = json.load(open(base + r"\selected_blocks.json", encoding="utf-8"))
be = json.load(open(base + r"\block_entities.json", encoding="utf-8"))
print("selected palette of carpet-like:")
for st in sorted(set(x[3] for x in sel if "carpet" in x[3] or "concrete" in x[3])):
    print(st, sum(1 for x in sel if x[3]==st), "bbox", min(x[0] for x in sel if x[3]==st), min(x[2] for x in sel if x[3]==st), max(x[0] for x in sel if x[3]==st), max(x[2] for x in sel if x[3]==st))
print()
print("banners", sum(1 for x in be if x["id"]=="minecraft:banner"))
for x in sorted([b for b in be if b["id"]=="minecraft:banner"], key=lambda a:(a["x"],a["z"],a["y"])):
    print(x["x"],x["y"],x["z"])
print()
print("signs")
for x in be:
    if x["id"]=="minecraft:sign":
        print(x["x"],x["y"],x["z"], x.get("Color"), x.get("GlowingText"), [x.get("Text%d"%i) for i in range(1,5)])
